/*
* This software/firmware and related documentation ("MediaTek Software") are
* protected under relevant copyright laws. The information contained herein
* is confidential and proprietary to MediaTek Inc. and/or its licensors.
* Without the prior written permission of MediaTek inc. and/or its licensors,
* any reproduction, modification, use or disclosure of MediaTek Software,
* and information contained herein, in whole or in part, shall be strictly prohibited.
*/
/* MediaTek Inc. (C) 2014. All rights reserved.
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

package com.mediatek.rcs.contacts.list;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.Data;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ProgressBar;

import com.mediatek.rcs.contacts.R;
import com.mediatek.rcs.contacts.list.GroupListLoader;
import com.mediatek.rcs.contacts.list.GroupListItem;
import com.mediatek.rcs.contacts.list.AutoScrollListView;
import com.mediatek.rcs.contacts.util.WeakAsyncTask;
import com.mediatek.rcs.contacts.util.WaitCursorView;

import java.util.ArrayList;

public class GroupListFragment extends Fragment {

    private static final String TAG = GroupListFragment.class.getSimpleName();   
    private static final int LOADER_GROUPS = 1;

    protected static final String RESULT = "com.mediatek.contacts.list.pickcontactsresult";
    protected GroupListAdapter mAdapter;

    private Context mContext;
    private View mRootView;
    private AutoScrollListView mListView;
    private TextView mEmptyView;    
    private View mLoadingContainer;
    private TextView mLoadingContact;
    private ProgressBar mProgress;
    private WaitCursorView mWaitCursorView;

    public GroupListFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.rcs_group_list_fragment, null);
        mEmptyView = (TextView) mRootView.findViewById(R.id.empty);
        mLoadingContainer = mRootView.findViewById(R.id.loading_container);
        mLoadingContact = (TextView) mRootView.findViewById(R.id.loading_contact);
        mLoadingContact.setVisibility(View.GONE);
        mProgress = (ProgressBar) mRootView.findViewById(R.id.progress_loading_contact);
        mProgress.setVisibility(View.GONE);
        mWaitCursorView = new WaitCursorView(mContext, mLoadingContainer, mProgress, mLoadingContact);

        mAdapter = new GroupListAdapter(mContext);

        mListView = (AutoScrollListView) mRootView.findViewById(R.id.list);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(configOnItemClickListener());
        mListView.setEmptyView(mEmptyView);
        configureVerticalScrollbar();

        return mRootView;
    }

    private void configureVerticalScrollbar() {
        mListView.setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_RIGHT);
        mListView.setScrollBarStyle(ListView.SCROLLBARS_OUTSIDE_OVERLAY);
        int leftPadding = 0;
        int rightPadding = 0;

        rightPadding = mContext.getResources().getDimensionPixelOffset(
            R.dimen.list_visible_scrollbar_padding);

        mListView.setPadding(leftPadding, mListView.getPaddingTop(),
                rightPadding, mListView.getPaddingBottom());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
    }

    @Override
    public void onStart() {
        getLoaderManager().restartLoader(LOADER_GROUPS, null, mGroupLoaderListener);
        super.onStart();
    }

    private final LoaderManager.LoaderCallbacks<Cursor> mGroupLoaderListener =
            new LoaderCallbacks<Cursor>() {

        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            Log.i(TAG, "onCreateLoader");
            mWaitCursorView.startWaitCursor();
            mEmptyView.setText(null);
            return new GroupListLoader(mContext);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (!isAdded()) {
                Log.w(TAG, "This Fragment is not add to the Activity now.data:" + data);
                if (data != null) {
                    data.close();
                }
                return;
            }

            Log.i(TAG, "onLoadFinished");
            mWaitCursorView.stopWaitCursor();
            bindData(data);
        }

        public void onLoaderReset(Loader<Cursor> loader) {
        }
    };

    private void bindData(Cursor data) {
        mEmptyView.setText(R.string.no_group);

        if (data == null) {
            Log.i(TAG, "bindData null");
            return;
        }
        mAdapter.setCursor(data);
    }

    protected OnItemClickListener configOnItemClickListener() {
        return new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                GroupListItem item = mAdapter.getItem(position);
                int count = item.getMemberCount();
                if (count > 0) {
                    new GroupMemberQueryTask(getActivity()).execute(item.getGroupId());
                } else {
                    Log.i(TAG, "onItemClick count: " + count);
                    final Activity activity = getActivity();
                    activity.setResult(Activity.RESULT_CANCELED, new Intent());
                    activity.finish();
                }
            }
        };
    }

    public class GroupMemberQueryTask extends WeakAsyncTask<Long, Void, long[], Activity> {
        private ProgressDialog mProgress;

        public GroupMemberQueryTask(Activity target) {
            super(target);
        }

        @Override
        protected void onPreExecute(final Activity target) {
            Log.d(TAG, "onPreExecute");
            mProgress = new ProgressDialog(target);
            mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgress.show(target, null, target.getText(R.string.please_wait), true);
            super.onPreExecute(target);
        }

        @Override
        protected long[] doInBackground(Activity target, Long... param) {
            long groupId = param[0];

            if (groupId <= 0) {
                Log.e(TAG, "doInBackground groupId error: " + groupId);
                return null;
            }
            Log.d(TAG, "doInBackground groupId: " + groupId);

            ArrayList<Long> contactIdsList = new ArrayList<Long>();
            Cursor cursor = null;
            try {                
                cursor = mContext.getContentResolver().query(Data.CONTENT_URI,
                        new String[] { Data.CONTACT_ID }, Data.MIMETYPE + "=? AND "
                                + GroupMembership.GROUP_ROW_ID + "=?", new String[] {
                                GroupMembership.CONTENT_ITEM_TYPE,
                                String.valueOf(groupId) }, null);
                if (cursor != null) {
                    cursor.moveToPosition(-1);
                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(0);
                        Log.i(TAG, "doInBackground dataId is " + String.valueOf(id));
                        contactIdsList.add(Long.valueOf(id));
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                    cursor = null;
                }
            }

            Log.d(TAG, "doInBackground size is " + contactIdsList.size());
            long[] contactIds = new long[contactIdsList.size()];
            int index = 0;
            for (Long id : contactIdsList) {
                contactIds[index++] = id.longValue();
            }

            return contactIds;
        }

        @Override
        protected void onPostExecute(final Activity target, long[] ids) {
            Log.d(TAG, "onPostExecute");
            if (!target.isFinishing() && mProgress != null && mProgress.isShowing()) {
                dismissDialogSafely(mProgress);
                mProgress = null;
            }
            super.onPostExecute(target, ids);
            if (ids == null || ids.length == 0) {
                Log.d(TAG, "onPostExecute no member");
                getActivity().setResult(Activity.RESULT_CANCELED, new Intent());
            } else {
                getActivity().setResult(Activity.RESULT_OK, new Intent().putExtra(RESULT, ids));
            }
            target.finish();
        }

        private void dismissDialogSafely(ProgressDialog dialog) {
            try {
                dialog.dismiss();
            } catch (IllegalArgumentException e) {
                Log.d(TAG, "IllegalArgumentException");
            }
        }
    }

}
