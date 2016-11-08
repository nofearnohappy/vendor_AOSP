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
import android.app.Fragment;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.BaseColumns;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.mediatek.rcs.blacklist.BlacklistData.BlacklistTable;

/**
 * BlacklistFragment.
 */
public class BlacklistFragment extends ListFragment
    implements LoaderManager.LoaderCallbacks<Cursor>,
    BlacklistUtils.SyncWithContactsCallback, BlacklistInputDialog.InputAction {

    private static final String TAG = "Blacklist";

    public static final String FRAGMENT_TAG = "BlacklistFragment";

    private static final Uri mUri = BlacklistData.AUTHORITY_URI;
    private static final String[] BLACKLIST_PROJECTION = {BaseColumns._ID,
                                                        BlacklistTable.DISPLAY_NAME,
                                                        BlacklistTable.PHONE_NUMBER};
    private static final String CONTACTS_ADD_ACTION =
                                    "android.intent.action.contacts.list.PICKMULTIPHONES";
    private static final String CONTACTS_ADD_ACTION_DATA_RESULT =
                                    "com.mediatek.contacts.list.pickdataresult";

    // for ims call number
    private static final String CONTACTS_ADD_ACTION_ALL =
                        "android.intent.action.contacts.list.PICKMULTIPHONEANDIMSANDSIPCONTACTS";
    private static final String CONFERENCE_CALL_LIMIT_NUMBER_EXTRA = "CONFERENCE_CALL_LIMIT_NUMBER";
    private static final int CALL_NUMBER_MAX = 3500;

    private static final int REQUEST_CODE = 100;

    private static final int MENU_ID_ADD = Menu.FIRST;
    private static final int MENU_ID_DELETE = Menu.FIRST + 1;

    private static final int MSG_ID_IMPORT_CONTACTS = 100;

    private static final int TOTAL_MEMBER_MAX = BlacklistTable.RECORDS_NUMBER_MAX;

    private BlacklistAdapter mAdapter;
    private Cursor mCursor;
    private boolean mSkipRequery = true;
    private boolean mCursorInused = false;

    private View mInputDlgView;
    private MenuItem mAddMenu;
    private MenuItem mDeleteMenu;
    private ImageButton mImportBtn;

    private AddMembersTask mAddTask;
    private Intent mResultIntent;

    private BlacklistInputDialog mInputDlg = new BlacklistInputDialog();
    private BlacklistDeleteFragment mDeleteFragment = new BlacklistDeleteFragment();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        log("onCreate " + this);

        setHasOptionsMenu(true);

        mAdapter = new BlacklistAdapter(getActivity(), mCursor);
        mAdapter.setListViewMode(BlacklistAdapter.LIST_VIEW_NORMAL);

        getLoaderManager().initLoader(0, null, this);
        mSkipRequery = true;

        log("mInputDlg " + mInputDlg);
        mInputDlg.setInputAction(this);

        Fragment f = getFragmentManager().findFragmentByTag(BlacklistInputDialog.INPUT_DIALOG_TAG);
        if (f != null) {
            getFragmentManager().beginTransaction().remove(f);
            log("showInputDialog remove already fragment, because of state missed");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                Bundle savedInstanceState) {
        log("onCreateView " + this);

        //View view = inflater.inflate(R.layout.blacklist_fragment, container, false);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        log("onViewCreated " + this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        log("onActivityCreated " + this);

        getListView().setAdapter(mAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        log("onResume " + this);

        ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP |
                                        ActionBar.DISPLAY_SHOW_TITLE);
            actionBar.setTitle(R.string.blacklist);
        }

        setEmptyText(getString(R.string.list_empty));

        if (!mSkipRequery) {
            //getLoaderManager().restartLoader(0, null, this);
            restartQuery();
        }
        mSkipRequery = false;
        mCursorInused = false;

        BlacklistUtils.startSyncWithContacts(getActivity().getContentResolver(), this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mAddMenu = menu.add(Menu.NONE, MENU_ID_ADD, MENU_ID_ADD, R.string.add);
        mAddMenu.setIcon(R.drawable.ic_menu_add_holo_light);
        mAddMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        mDeleteMenu = menu.add(Menu.NONE, MENU_ID_DELETE, MENU_ID_DELETE, R.string.delete);
        mDeleteMenu.setIcon(R.drawable.ic_menu_trash_holo_light);
        mDeleteMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (mAdapter.getCount() > 0) {
            mDeleteMenu.setVisible(true);
        } else {
            mDeleteMenu.setVisible(false);
        }

        if (mAdapter.getCount() < TOTAL_MEMBER_MAX) {
            mAddMenu.setVisible(true);
        } else {
            mAddMenu.setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean ret = true;
        switch (item.getItemId()) {
        case MENU_ID_ADD:
            showInputDialog();
            break;

        case MENU_ID_DELETE:
            showDeleteFragment();
            break;

        case android.R.id.home:
            getActivity().finish();
            break;

        default:
            ret = false;
        }

        return ret;
    }

    @Override
    public void onPause() {
        super.onPause();
        log("onPause " + this);
    }

    @Override
    public void onStop() {
        super.onStop();
        log("onStop " + this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log("onDestroy " + this);

        mInputDlg.setInputAction(null);

        if (mAddTask != null) {
            mAddTask.cancel(true);
        }

        BlacklistUtils.cancelSyncWithContacts(this);
        if (mAdapter != null) {
            log("clear cursor");
            mAdapter.changeCursor(null);
        }

        mAdapter = null;
        mCursor = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        log("onTrimMemory: " + level);
    }

    private void showInputDialog() {
        log("showInputDialog");
        Fragment f = getFragmentManager().findFragmentByTag(BlacklistInputDialog.INPUT_DIALOG_TAG);
        if (f != null) {
            log("showInputDialog, press multi times, skip it");
            return;
        }
        mInputDlg.show(getFragmentManager());
    }

    private void showDeleteFragment() {
        log("showDeleteFragment");
        Fragment f = getFragmentManager().findFragmentByTag(BlacklistDeleteFragment.FRAGMENT_TAG);
        if (f != null) {
            log("showDeleteFragment, press multi times, skip it");
            return;
        }

        mCursorInused = true;

        mDeleteFragment.setCursor(mCursor);

        getFragmentManager().beginTransaction()
                .replace(R.id.blacklistActivity, mDeleteFragment,
                    BlacklistDeleteFragment.FRAGMENT_TAG)
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != getActivity().RESULT_OK) {
            return;
    }

        mResultIntent = data;

            mAddTask = new AddMembersTask();
        log("import from contacts start");

        if (mAddTask.getStatus() != AsyncTask.Status.RUNNING) {
            mAddTask.execute(requestCode, resultCode);
        }
    }

    /**
     * For Input dialog and self.
     */
    public void restartQuery() {
        log("restart query");
        mAdapter.notifyDataSetInvalidated();
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity().getApplicationContext(),
                                mUri, BLACKLIST_PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor arg1) {
        log("onLoadFinished: " + this + "mCursor is inused " + mCursorInused);
        if (mCursorInused) {
            return;
        }

        mCursor = arg1;
        mAdapter.changeCursor(arg1);
        mAdapter.notifyDataSetChanged();
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
    }

    @Override
    public void onUpdatedWithContacts(boolean result) {
        log("Sync with contacts done: " + result);
        if (result) {
            restartQuery();
        }
    }

    /**
     * AddMembersTask.
     */
    private class AddMembersTask extends AsyncTask<Integer, String, Integer> {

        @Override
        protected Integer doInBackground(Integer... params) {
            final long[] contactsId = mResultIntent
                    .getLongArrayExtra(CONTACTS_ADD_ACTION_DATA_RESULT);
            BlacklistUtils.importFromContacts(getActivity().getContentResolver(), contactsId);
            return null;
        }

        @Override
        protected void onPreExecute() {
            getActivity().showDialog(BlacklistManagerActivity.PLEASEWAIT_DIALOG);
            log("onPreExecute showDialog");
        }

        @Override
        protected void onProgressUpdate(String... id) {
        }

        @Override
        protected void onPostExecute(Integer size) {
            log("import from contacts end");
            if (!this.isCancelled()) {
                restartQuery();
            }
            ((BlacklistManagerActivity) getActivity())
                    .dismissDialogSafely(BlacklistManagerActivity.PLEASEWAIT_DIALOG);
            getActivity().invalidateOptionsMenu();
        }
    }

    private Handler mMsgHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            log("handleMessage " + msg.what);
            switch(msg.what) {
                case MSG_ID_IMPORT_CONTACTS:
                    Intent intent = new Intent(CONTACTS_ADD_ACTION_ALL);
                    intent.setType(Phone.CONTENT_TYPE);
                    intent.putExtra(CONFERENCE_CALL_LIMIT_NUMBER_EXTRA, CALL_NUMBER_MAX);
                    try {
                        startActivityForResult(intent, REQUEST_CODE);
                    } catch (ActivityNotFoundException e) {
                        log("start activity exception");
                        e.printStackTrace();
                    }
                    break;
                default:
                    log("handleMessage default");
                    break;
            }
        }
    };

    @Override
    public void onClickPositiveBtn() {
        restartQuery();
    }

    @Override
    public void onClickImportBtn() {
        mMsgHandler.sendEmptyMessage(MSG_ID_IMPORT_CONTACTS);
    }

    private void log(String message) {
        Log.d(TAG, "[" + getClass().getSimpleName() + "] " + message);
    }
}
