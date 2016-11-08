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
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ProgressBar;

import com.mediatek.rcs.contacts.ContactsReceiver;
import com.mediatek.rcs.contacts.R;
import com.mediatek.rcs.contacts.list.GroupChatLoader;
import com.mediatek.rcs.contacts.list.GroupChatItem;
import com.mediatek.rcs.contacts.list.AutoScrollListView;
import com.mediatek.rcs.contacts.util.WaitCursorView;

import java.util.ArrayList;

public class GroupChatFragment extends Fragment {

    private static final String TAG = GroupChatFragment.class.getSimpleName();    

    private static final String GROUP_CHAT_UPDATE = "com.mediatek.rcs.groupchat.STATE_CHANGED";
    private static final String INTENT_RCS_OFF = "com.mediatek.intent.rcs.stack.StopService";
    private static final int LOADER_GROUPS = 1;
    private static final long GROUP_UPDATE_DELAY_MILLIS = 100;

    private Context mContext;
    private View mRootView;
    private AutoScrollListView mListView;
    private TextView mEmptyView;
    private GroupChatAdapter mAdapter;
    private View mLoadingContainer;
    private TextView mLoadingContact;
    private ProgressBar mProgress;
    private WaitCursorView mWaitCursorView;
    private GroupStateReceiver mReceiver;
    private int mSubId;

    class GroupStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (GROUP_CHAT_UPDATE.equals(intent.getAction())) {
                Log.i(TAG, "onReceive: group state changed");
                mHandler.sendEmptyMessageDelayed(LOADER_GROUPS, GROUP_UPDATE_DELAY_MILLIS);
            } else if (INTENT_RCS_OFF.equals(intent.getAction())) {
                Log.i(TAG, "onReceive: rcs off");
                getActivity().finish();
            } else {
                Log.i(TAG, "onReceive: sim data changed");
                getActivity().finish();
            }
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == LOADER_GROUPS) {
                Log.i(TAG, "handleMessage");
                Activity activity = (Activity) mContext;
                if (activity.isFinishing()) {
                    Log.w(TAG, "handleMessage: activity finish");
                    return;
                }
                if (!isAdded()) {
                    Log.w(TAG, "handleMessage: Fragment is not add to the Activity");
                    return;
                }
                getLoaderManager().restartLoader(LOADER_GROUPS, null, mGroupLoaderListener);
            }
        }
    };

    public GroupChatFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
            
        mRootView = inflater.inflate(R.layout.rcs_group_chat_fragment, null);
        mEmptyView = (TextView) mRootView.findViewById(R.id.empty);
        mLoadingContainer = mRootView.findViewById(R.id.loading_container);
        mLoadingContact = (TextView) mRootView.findViewById(R.id.loading_contact);
        mLoadingContact.setVisibility(View.GONE);
        mProgress = (ProgressBar) mRootView.findViewById(R.id.progress_loading_contact);
        mProgress.setVisibility(View.GONE);
        mWaitCursorView = new WaitCursorView(mContext, mLoadingContainer, mProgress, mLoadingContact);

        mAdapter = new GroupChatAdapter(mContext);
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
        Log.i(TAG, "onAttach");
        mContext = activity;
    }

    @Override
    public void onDetach() {
        Log.i(TAG, "onDetach");
        if (mReceiver != null) {
            mContext.unregisterReceiver(mReceiver);
            mReceiver = null;
            Log.i(TAG, "unregisterReceiver");
        }      
        mContext = null;
        super.onDetach();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
        mSubId = SubscriptionManager.getDefaultDataSubId();
        Log.i(TAG, "getDefaultDataSubId: " + mSubId);
        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            SharedPreferences sh =
                    mContext.getSharedPreferences("rcs_icc_id", mContext.MODE_WORLD_READABLE);
            String oldIccId = sh.getString(ContactsReceiver.RCS_ICC_ID, "");
            Log.i(TAG, "oldIccId: " + oldIccId);
            if (oldIccId != null && !oldIccId.equals("")) {
                SubscriptionInfo subInfo =
                        SubscriptionManager.from(mContext).getSubscriptionInfoForIccId(oldIccId);
                if (subInfo != null) {
                    mSubId = subInfo.getSubscriptionId();
                    Log.i(TAG, "oldSubId: " + mSubId);
                }
            }
        }
        getLoaderManager().restartLoader(LOADER_GROUPS, null, mGroupLoaderListener);
        if (mReceiver == null) {
            mReceiver = new GroupStateReceiver();
            IntentFilter intent = new IntentFilter();
            intent.addAction(GROUP_CHAT_UPDATE);
            intent.addAction(INTENT_RCS_OFF);
            intent.addAction(ContactsReceiver.INTENT_RCS_LOGIN);
            mContext.registerReceiver(mReceiver, intent);
            Log.i(TAG, "registerReceiver");
        }
    }

    private final LoaderManager.LoaderCallbacks<Cursor> mGroupLoaderListener =
            new LoaderCallbacks<Cursor>() {

        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            Log.i(TAG, "onCreateLoader");
            mWaitCursorView.startWaitCursor();
            mEmptyView.setText(null);
            return new GroupChatLoader(mContext, mSubId);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    private void bindData(Cursor data) {
        mEmptyView.setText(R.string.no_group_chat);

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
                GroupChatItem item = mAdapter.getItem(position);
                String chat = item.getId();
                Log.i(TAG, "configOnItemClickListener id: " + chat);
                try {
                    Intent intent = new Intent("com.mediatek.rcs.groupchat.START");
                    intent.setPackage("com.mediatek.rcs.message");
                    intent.putExtra("chat_id", chat);
                    startActivity(intent);
                } catch (ActivityNotFoundException ex) {
                    Log.i(TAG, "configOnItemClickListener not found");
                }
            }
        };
    }

}
