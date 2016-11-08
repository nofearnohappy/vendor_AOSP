/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.rcs.blacklist;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;

import java.util.HashSet;
import java.util.Set;

/**
 * BlacklistDeleteFragment.
 */
public class BlacklistDeleteFragment extends ListFragment {

    private static final String TAG = "Blacklist";

    public static final String FRAGMENT_TAG = "BlacklistDeleteFragment";

    private static final String ALERT_DIALOG_TAG = "blacklist_delete";
    private static final String KEY_CHECKEDIDS = "checkedids";

    private BlacklistAdapter mAdapter;
    private Cursor mCursor;

    private PopupMenu mSelectMenu;
    private Button mSelectBtn;
    private Button mDoneBtn;
    // item position cache for delete
    private Set<Integer> mCheckedIds = new HashSet<Integer>();

    private View mProgress;
    private DeleteMembersTask mDeleteTask;
    private boolean mPopStackFailed;

    private DeleteAlertDialog mDialog = new DeleteAlertDialog();

    /**
     * Constructor.
     */
    public BlacklistDeleteFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        log("onCreate");

        if (savedInstanceState != null) {
            int[] ids = savedInstanceState.getIntArray(KEY_CHECKEDIDS);
            if (ids != null) {
                log("onCreate savedInstance ids: " + ids.length);
                for (int i = 0; i < ids.length; i++) {
                    mCheckedIds.add(ids[i]);
                }
            }
        }

        setHasOptionsMenu(true);
        customizeActionBar();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        log("onViewCreated");

        mAdapter = new BlacklistAdapter(getActivity(), mCursor);
        mAdapter.setListViewMode(BlacklistAdapter.LIST_VIEW_PICKER);
        getListView().setAdapter(mAdapter);
        setAdapterCheckStatusCallback();
    }

    @Override
    public void onResume() {
        super.onResume();
        log("onResume");

        if (mPopStackFailed) {
            if (getFragmentManager().getBackStackEntryCount() > 0) {
                getFragmentManager().popBackStack();
                log("onResume, go back because pop stack failed");
            }
            mPopStackFailed = false;
        }
        upateActionBar();
        setListShown(true);
    }

    public void setCursor(Cursor c) {
        mCursor = c;
    }

    private void customizeActionBar() {
        LayoutInflater inflater = (LayoutInflater) getActivity()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View customView  = inflater.inflate(R.layout.blacklist_actionbar_picker, null);
        ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP
                                        | ActionBar.DISPLAY_SHOW_TITLE
                                        | ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setCustomView(customView);
        }

        mSelectBtn = (Button) customView.findViewById(R.id.select);
        mDoneBtn = (Button) customView.findViewById(R.id.done);

        mSelectMenu = new PopupMenu(getActivity(), mSelectBtn);
        mSelectMenu.inflate(R.menu.select_popup_menu);

        setActionBarListener();
    }

    private void setActionBarListener() {
        mSelectBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mSelectMenu != null) {
                    updateSelectMenu();
                    mSelectMenu.show();
                }
            }
        });

        mSelectMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.select_item) {
                    if (mCheckedIds.size() == mAdapter.getCount()) {
                        // Menu should show "Deselect all"
                        mCheckedIds.clear();
                    } else {
                        // Menu should show "Select all"
                        selectAllItems();
                    }
                    mAdapter.notifyDataSetChanged();
                    updateSelectItemBtnDescription();
                    return true;
                }

                return false;
            }
        });

        mDoneBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                showDeleteAlertDlg();
            }
        });
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        CheckBox checkView = (CheckBox) v.findViewById(R.id.checked);

        if (checkView != null) {
            checkView.setChecked(!checkView.isChecked());
            if (checkView.isChecked()) {
                mCheckedIds.add(position);
            } else {
                mCheckedIds.remove(position);
            }
            updateSelectItemBtnDescription();
        }
    }

    private void setAdapterCheckStatusCallback() {
        mAdapter.setCheckStatusCallBack(new BlacklistAdapter.CheckStatusCallBack() {

            @Override
            public boolean getItemChecked(int id) {
                if (mCheckedIds.contains(id)) {
                    return true;
                }
                return false;
            }
        });
    }

    private void updateSelectMenu() {
        final Menu menu = mSelectMenu.getMenu();

        if (mAdapter.getCount() == 0) {
            menu.findItem(R.id.select_item).setEnabled(false);
        } else {
            menu.findItem(R.id.select_item).setEnabled(true);
        }

        if (mCheckedIds.size() == 0 || mAdapter.getCount() != mCheckedIds.size()) {
            menu.findItem(R.id.select_item).setTitle(R.string.select_all);
        } else {
            menu.findItem(R.id.select_item).setTitle(R.string.deselect_all);
        }
    }

    private void upateActionBar() {
        if (mDoneBtn != null) {
            mDoneBtn.setText(android.R.string.ok);
        }

        if (mSelectMenu != null) {
            updateSelectMenu();
        }

        updateSelectItemBtnDescription();
    }

    private void updateSelectItemBtnDescription() {
        // update select description
        Button selectItemBtn = (Button) getActivity()
                    .getActionBar().getCustomView().findViewById(R.id.select);
        if (selectItemBtn != null) {
            selectItemBtn.setText(getString(R.string.select_count, mCheckedIds.size()));
        }

        //update done button status
        if (mCheckedIds.size() > 0) {
            mDoneBtn.setEnabled(true);
            mDoneBtn.setTextColor(Color.WHITE);
        } else {
            mDoneBtn.setEnabled(false);
            mDoneBtn.setTextColor(Color.LTGRAY);
        }
    }

    private void selectAllItems() {
        for (int i = 0; i < mAdapter.getCount(); i++) {
            if (!mCheckedIds.contains(i)) {
                mCheckedIds.add(i);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean ret = false;
        switch (item.getItemId()) {
            case android.R.id.home:
                if (getFragmentManager().getBackStackEntryCount() > 0) {
                    getFragmentManager().popBackStack();
                }
                ret = true;
                break;

            default:
                ret = false;
        }

        return ret;
    }

    private void startToDelete() {
        mDeleteTask = new DeleteMembersTask();

        if (mDeleteTask.getStatus() != AsyncTask.Status.RUNNING) {
            log("strat to delete");
            mDeleteTask.execute(0, mCheckedIds.size());
        }
    }

    /**
     * DeleteMembersTask.
     */
    private class DeleteMembersTask extends AsyncTask<Integer, String, Integer> {

        @Override
        protected Integer doInBackground(Integer... params) {
            try {
                StringBuilder where = new StringBuilder("_id in (");
                for (int pos : mCheckedIds) {
                    mCursor.moveToPosition(pos);
                    String rowId = mCursor.getString(mCursor.getColumnIndexOrThrow("_id"));
                    where.append(rowId);
                    where.append(',');
                }
                where.deleteCharAt(where.length() - 1);
                where.append(")");

                BlacklistUtils.deleteMembers(getActivity().getContentResolver(), where.toString());
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            getActivity().showDialog(BlacklistManagerActivity.PLEASEWAIT_DIALOG);
        }

        @Override
        protected void onProgressUpdate(String... id) {
        }

        @Override
        protected void onPostExecute(Integer size) {
            if (!this.isCancelled()) {
                try {
                    if (getFragmentManager().getBackStackEntryCount() > 0) {
                        getFragmentManager().popBackStack();
                        log("delete done, go back");
                    }
                } catch (IllegalStateException e) {
                    log("delete done, go back exception");
                    mPopStackFailed = true;
                    e.printStackTrace();
                }
            }
            ((BlacklistManagerActivity) getActivity())
                .dismissDialogSafely(BlacklistManagerActivity.PLEASEWAIT_DIALOG);
        }
    }

    private void showProgressing(boolean shown) {
        int animStyle = android.R.anim.fade_out;
        int visibility = View.GONE;
        if (shown) {
            animStyle = android.R.anim.fade_in;
            visibility = View.VISIBLE;
        }

        mProgress.startAnimation(AnimationUtils.loadAnimation(getActivity(), animStyle));
        mProgress.setVisibility(visibility);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log("onDestroy");

        mAdapter.swapCursor(null);
        mCheckedIds.clear();

        if (mDeleteTask != null) {
            mDeleteTask.cancel(true);
        }

        mCursor = null;
        mAdapter = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        final int checkedItemsCount = mCheckedIds.size();
        log("[onSaveInstanceState] mCheckedIds size: " + checkedItemsCount);
        if (checkedItemsCount > 0) {
            int[] ids = new int[checkedItemsCount];
            int i = 0;
            for (Integer id : mCheckedIds) {
                ids[i++] = id.intValue();
            }
            outState.putIntArray(KEY_CHECKEDIDS, ids);
        }
    }

    private void showDeleteAlertDlg() {
        Fragment f = getFragmentManager().findFragmentByTag(ALERT_DIALOG_TAG);
        log("showDeleteAlertDlg");
        if (f != null) {
            log("showDeleteAlertDlg, press multi times, skip it");
            return;
        }

        mDialog.show(getFragmentManager());
    }

    /**
     * Delete Alter dialog.
     */
    public class DeleteAlertDialog extends DialogFragment {

        private AlertDialog mDialog;
        private boolean mShowing = false;

        /**
         *  To show this dialog.
         * @param manager FragmentManager
         */
        public void show(FragmentManager manager) {
            log("[DeleteAlertDialog]show, mShowing: " + mShowing);

            if (!mShowing) {
                mShowing = true;
                super.show(manager, ALERT_DIALOG_TAG);
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            log("[DeleteAlertDialog]onResume");
            mDialog.setTitle(getResources().getString(R.string.delete));
            mDialog.setMessage(getResources().getString(R.string.delete_ask));

            Button posBtn = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            posBtn.setText(android.R.string.ok);
            Button negBtn = mDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
            negBtn.setText(android.R.string.cancel);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            log("[DeleteAlertDialog]onCreateDialog");
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.delete)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setMessage(R.string.delete_ask)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    startToDelete();
                                }
                            });
            mDialog = builder.create();
            return mDialog;
        }

        @Override
        public void onDestroyView() {
            log("[DeleteAlertDialog]onDestroyView");
            super.onDestroyView();
            setTargetFragment(null, 0);
            mShowing = false;
        }
    }

    private void log(String message) {
        Log.d(TAG, "[" + getClass().getSimpleName() + "] " + message);
    }
}
