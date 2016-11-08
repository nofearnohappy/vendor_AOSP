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

/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.mms.folder.ui;


import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ActivityNotFoundException;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SqliteWrapper;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.Telephony;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnKeyListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.SearchView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.mms.data.Contact;
import com.android.mms.data.Conversation;
import com.android.mms.data.FolderView;
import com.android.mms.LogTag;
import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.MmsPluginManager;
import com.android.mms.R;
import com.android.mms.transaction.MessagingNotification;
import com.android.mms.transaction.Transaction;
import com.android.mms.transaction.TransactionBundle;
import com.android.mms.transaction.TransactionService;
import com.android.mms.ui.ComposeMessageActivity;
import com.android.mms.ui.ConversationList;
import com.android.mms.ui.CustomMenu;
import com.android.mms.ui.CustomMenu.DropDownMenu;
import com.android.mms.ui.MessageUtils;
import com.android.mms.ui.MmsPlayerActivity;
import com.android.mms.ui.SearchActivity;
import com.android.mms.util.DownloadManager;
import com.android.mms.util.DraftCache;
import com.android.mms.util.StatusBarSelectorCreator;
import com.android.mms.widget.MmsWidgetProvider;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.util.PduCache;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.mms.util.FeatureOption;
import com.android.mms.util.MmsLog;
import com.mediatek.cb.cbmsg.CBMessagingNotification;
import com.mediatek.mms.folder.util.FolderModeUtils;
import com.mediatek.mms.ui.SubinfoSelectedActivity;
import com.mediatek.mms.util.MmsDialogNotifyUtils;
import com.mediatek.mms.util.PermissionCheckUtil;
import com.mediatek.setting.SettingListActivity;
import com.mediatek.setting.SmsPreferenceActivity;
import com.mediatek.setting.SubSelectActivity;
import com.mediatek.simmessage.ManageSimMessages;
import com.mediatek.wappush.WapPushMessagingNotification;
import com.mediatek.wappush.ui.WPMessageActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


/** M:
 * This activity provides a list view of existing conversations.
 */
public class FolderViewList extends ListActivity implements DraftCache.OnDraftChangedListener {
    private static final String TAG = "FolderViewList";
    private static final String CONV_TAG = "Mms/FolderViewList";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = DEBUG;

    public static final int OPTION_INBOX    = 0;
    public static final int OPTION_OUTBOX   = 1;
    public static final int OPTION_DRAFTBOX = 2;
    public static final int OPTION_SENTBOX  = 3;

    public static final int DRAFTFOLDER_LIST_QUERY_TOKEN      = 1009;
    public static final int INBOXFOLDER_LIST_QUERY_TOKEN      = 1111;
    public static final int OUTBOXFOLDER_LIST_QUERY_TOKEN     = 1121;
    public static final int SENTFOLDER_LIST_QUERY_TOKEN       = 1131;
    public static final int FOLDERVIEW_DELETE_TOKEN           = 1001;
    public static final int FOLDERVIEW_HAVE_LOCKED_MESSAGES_TOKEN     = 1002;
    private static final int FOLDERVIEW_DELETE_OBSOLETE_THREADS_TOKEN = 1003;


    private static final Uri SMS_URI = Uri.parse("content://sms/");
    private static final Uri MMS_URI = Uri.parse("content://mms/");
    private static final Uri WAPPUSH_URI = Uri.parse("content://wappush/");
    private static final Uri CB_URI = Uri.parse("content://cb/messages/");
    private static final Uri SIM_SMS_URI = Uri.parse("content://mms-sms/sim_sms/");
    // IDs of the context menu items for the list of conversations.
    public static final int MENU_DELETE               = 0;
    public static final int MENU_VIEW                 = 1;
    public static final int MENU_VIEW_CONTACT         = 2;
    public static final int MENU_ADD_TO_CONTACTS      = 3;
    public static final int MENU_SIM_SMS              = 4;
    public static final int MENU_FORWORD              = 5;
    public static final int MENU_REPLY                = 6;

    // IDs of the option menu items for the list of conversations.
    public static final int MENU_MULTIDELETE          = 0;
    public static final int MENU_CHANGEVIEW           = 1;

    public static final String FOLDERVIEW_KEY         = "floderview_key";
    private View mFolderSpinner;
    private MenuItem mSearchItem;
    private SearchView mSearchView;
    private ThreadListQueryHandler mQueryHandler;
/// M: @{
    private FolderViewListAdapter mListAdapter = null;
/// @}
    private Handler mHandler;
    private boolean mNeedToMarkAsSeen;
    private Contact mContact = null;
    //private SearchView mSearchView;
   // private StatusBarManager mStatusBarManager;
    //wappush: indicates the type of thread, this exits already, but has not been used before
    private int mType;
    public static int mgViewID;
    private Context context = null;
    private AccountDropdownPopup mAccountDropdown;
    private TextView mSpinnerTextView;
    private TextView mCountTextView;
    private View mSmsPromoBannerView;
    public static final int REQUEST_CODE_SELECT_SIMINFO = 160;
    private Uri mSIM_SMS_URI_NEW = Uri.parse("content://mms-sms/sim_sms/#");

    private SimpleAdapter mAdapter;
    private static final String VIEW_ITEM_KEY_BOXNAME   = "spinner_line_2";
    private String where = null;


    private static final String FOR_MULTIDELETE = "ForMultiDelete";

    private boolean mDisableSearchFlag = false;

    private boolean mNeedUpdateListView = false;
/// M: @{
    private boolean mIsQuerying = false; //is in querying
    private boolean mNeedQuery = false; //whether receive oncontentchanged info
    private boolean mIsInActivity = false; //whether in activity
/// @}
    // Whether or not we are currently enabled for SMS. This field is updated in onResume to make
    // sure we notice if the user has changed the default SMS app.
    private boolean mIsSmsEnabled;
    private Toast mComposeDisabledToast;

    public static final int REQUEST_CODE_DELETE_RESULT = 180;

    private static int mDeleteCounter = 0;


    public ModeCallback mModeCallBack = new ModeCallback();
    public ActionMode mActionMode = null;
    private static String ACTIONMODE = "actionMode";
    private static String BOXTYPE = "boxType";
    private static String NEED_RESTORE_ADAPTER_STATE = "needRestore";
    private static String mSELECT_ITEM_IDS = "selectItemIds";
    private boolean mIsNeedRestoreAdapterState = false;
    private long[] mListSelectedItem;

    private boolean mHasLockedMsg = false;
    private Map<Long, Boolean> mListItemLockInfo;
    ///M: add for update draft cache after deleting message in draft box
    private HashSet<Long> mDeletedThreadIds;
    private HashSet<Long> mCacheMmsList;
    /** M: this is used to record the fontScale, if it is > 1.1[1.1 is big style]
     *  we need make the content view of FolderViewListItem to be one line
     *  or it will overlapping with the above from view.
     */
    private float mFontScale;
    public static final float MAX_FONT_SCALE = 1.1f;
    private AlertDialog mAlertDialog;
    // add for mutli user
    private boolean isUserHasPerUsingMms = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (PermissionCheckUtil.requestRequiredPermissions(this)) {
            return;
        }

        if (!FolderModeUtils.getMmsDirMode()) {
            Log.d(TAG, "enter ConversationList");
            Intent intent = new Intent(this, ConversationList.class);
            startActivity(intent);
            finish();
        }
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.folderview_list_screen);
        mSmsPromoBannerView = findViewById(R.id.banner_sms_promo);
        mQueryHandler = new ThreadListQueryHandler(getContentResolver());

        ListView listView = getListView();
        //listView.setOnCreateContextMenuListener(mConvListOnCreateContextMenuListener);
        listView.setOnKeyListener(mThreadListKeyListener);

//        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
//        listView.setMultiChoiceModeListener(new ModeCallback());

        listView.setOnItemLongClickListener(new MultiSelectOnLongClickListener());
        if (savedInstanceState != null) {
            mgViewID = savedInstanceState.getInt(BOXTYPE, OPTION_INBOX);
            mIsNeedRestoreAdapterState = savedInstanceState.getBoolean(
                    NEED_RESTORE_ADAPTER_STATE, false);
        } else {
            mgViewID = getIntent().getIntExtra(FOLDERVIEW_KEY, OPTION_INBOX);
            mIsNeedRestoreAdapterState = false;
        }

        View emptyView = findViewById(R.id.empty);
        listView.setEmptyView(emptyView);

        context = FolderViewList.this;
        initListAdapter();
        mHandler = new Handler();

        initSpinnerListAdapter();
        setTitle("");
//        mgViewID = getIntent().getIntExtra(FOLDERVIEW_KEY, 0);
        Log.d(TAG, "onCreate, mgViewID:" + mgViewID);
        setBoxTitle(mgViewID);
        mListItemLockInfo = new HashMap<Long, Boolean>();
        mDeletedThreadIds = new HashSet<Long>();
        mCacheMmsList = new HashSet<Long>();
        /** M: get fontscale
         *  we only need to set it to true if needed
         *  font scale change will make this activity create again
         */
        mFontScale = getResources().getConfiguration().fontScale;
        MmsLog.d(TAG, "system fontscale is:" + mFontScale);
        if (mFontScale >= MAX_FONT_SCALE) {
            mListAdapter.setSubjectSingleLineMode(true);
        }
        /// M: add for update sim state dynamically. @{
        IntentFilter intentFilter
                = new IntentFilter(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);
        this.registerReceiver(mSubReceiver, intentFilter);
    }

    private void initSpinnerListAdapter() {

        mAdapter = new SimpleAdapter(this, getData(),
              R.layout.folder_mode_item,
              new String[] {"spinner_line_2"},
              new int[] {R.id.spinner_line_2});
        setupActionBar();

        mAccountDropdown = new AccountDropdownPopup(context);
        mAccountDropdown.setAdapter(mAdapter);

   }

    private void setupActionBar() {
        ActionBar actionBar = getActionBar();
        ViewGroup v = (ViewGroup) LayoutInflater.from(this)
                .inflate(R.layout.folder_mode_actionbar, null);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(v,
                new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT,
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER_VERTICAL | Gravity.LEFT));
        mCountTextView = (TextView) v.findViewById(R.id.message_count);

        mFolderSpinner = (View) v.findViewById(R.id.account_spinner);
        mSpinnerTextView = (TextView) v.findViewById(R.id.boxname);
        mSpinnerTextView.setText(R.string.inbox);

        mFolderSpinner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAdapter.getCount() > 0) {
                    mAccountDropdown.show();
                }
            }
        });
    }

    // Based on Spinner.DropdownPopup
    private class AccountDropdownPopup extends ListPopupWindow {
        public AccountDropdownPopup(Context mcontext) {
            super(mcontext);
            setAnchorView(mFolderSpinner);
            setModal(true);
            setPromptPosition(POSITION_PROMPT_ABOVE);
            setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                    onAccountSpinnerItemClicked(position);
                    dismiss();
                }
            });
        }

        @Override
        public void show() {
            WindowManager windowM = (WindowManager) context
                    .getSystemService(Context.WINDOW_SERVICE);
            Configuration config = context.getResources().getConfiguration();
            Display defDisplay = windowM.getDefaultDisplay();
            int w = 0;
            if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
                w = defDisplay.getWidth();
            } else {
                w = defDisplay.getHeight();
            }
            setWidth(w / 3);
            setInputMethodMode(ListPopupWindow.INPUT_METHOD_NOT_NEEDED);
            super.show();
            // List view is instantiated in super.show(), so we need to do this after...
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }
    }

    private void onAccountSpinnerItemClicked(int position) {
        switch (position) {
            case OPTION_INBOX:
                mgViewID = OPTION_INBOX;
                mSpinnerTextView.setText(R.string.inbox);
                mNeedToMarkAsSeen = true;
                startAsyncQuery();
                break;
            case OPTION_OUTBOX:
                mgViewID = OPTION_OUTBOX;
                mSpinnerTextView.setText(R.string.outbox);
                startAsyncQuery();
                break;
            case OPTION_DRAFTBOX:
                mgViewID = OPTION_DRAFTBOX;
                mSpinnerTextView.setText(R.string.draftbox);
                startAsyncQuery();
                break;
            case OPTION_SENTBOX:
                mgViewID = OPTION_SENTBOX;
                mSpinnerTextView.setText(R.string.sentbox);
                startAsyncQuery();
                break;
            default:
                break;
        }
        MmsLog.d(TAG, "onAccountSpinnerItemClicked mgViewID = " + mgViewID);
        invalidateOptionsMenu();
    }

    private List<Map<String, Object>> getData() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Map<String, Object> map = new HashMap<String, Object>();
        Resources res = getResources();
        map.put(VIEW_ITEM_KEY_BOXNAME, res.getText(R.string.inbox));
        list.add(map);

        map = new HashMap<String, Object>();
        map.put(VIEW_ITEM_KEY_BOXNAME, res.getText(R.string.outbox));
        list.add(map);

        map = new HashMap<String, Object>();
        map.put(VIEW_ITEM_KEY_BOXNAME, res.getText(R.string.draftbox));
        list.add(map);

        map = new HashMap<String, Object>();
        map.put(VIEW_ITEM_KEY_BOXNAME, res.getText(R.string.sentbox));
        list.add(map);

//        map = new HashMap<String, Object>();
//        map.put(VIEW_ITEM_KEY_BOXNAME, res.getText(R.string.simbox));
//        list.add(map);

        return list;
    }


    private final FolderViewListAdapter.OnContentChangedListener mContentChangedListener =
        new FolderViewListAdapter.OnContentChangedListener() {
        public void onContentChanged(FolderViewListAdapter adapter) {
            Log.d(TAG, "onContentChanged : mIsInActivity =" + mIsInActivity
                    + "mIsQuerying =" + mIsQuerying +
                    "mNeedQuery =" + mNeedQuery);
            if (mIsInActivity) {
                mNeedQuery = true;
                if (!mIsQuerying) {
                    startAsyncQuery();
                }
            }
        }
    };

    private void initListAdapter() {
        MmsLog.d(TAG, "initListAdapter");
        if (mListAdapter == null) {
            MmsLog.d(TAG, "create it");
            mListAdapter = new FolderViewListAdapter(this, null);
            mListAdapter.setOnContentChangedListener(mContentChangedListener);
            setListAdapter(mListAdapter);
            getListView().setRecyclerListener(mListAdapter);
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        // Handle intents that occur after the activity has already been created.
        setIntent(intent);
        mgViewID = intent.getIntExtra(FOLDERVIEW_KEY, 0);
        Log.d(TAG, "onNewIntent, mgViewID:" + mgViewID);
        setBoxTitle(mgViewID);
        if (mgViewID == OPTION_OUTBOX) {
            FolderView.markFailedSmsMmsSeen(this); //mark as seen
        }
        startAsyncQuery();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // add for multi user
        isUserHasPerUsingMms = !UserManager.get(getApplicationContext()).hasUserRestriction(
                UserManager.DISALLOW_SMS);
        boolean isSmsEnabled = MmsConfig.isSmsEnabled(this);
        if (isSmsEnabled != mIsSmsEnabled) {
            mIsSmsEnabled = isSmsEnabled;
            invalidateOptionsMenu();
        }
        // Multi-select is used to delete message. It is disabled if we are not the sms app.
        ListView listView = getListView();
        if (mIsSmsEnabled) {
            listView.setOnItemLongClickListener(new MultiSelectOnLongClickListener());
        } else {
            listView.setOnItemLongClickListener(null);
            if (mActionMode != null) {
                mActionMode.finish();
                if (mAlertDialog != null && mAlertDialog.isShowing()) {
                    mAlertDialog.dismiss();
                    mAlertDialog = null;
                }
            }
        }

        // Show or hide the SMS promo banner
        if (mIsSmsEnabled || MmsConfig.isSmsPromoDismissed(this)) {
            mSmsPromoBannerView.setVisibility(View.GONE);
        } else {
            initSmsPromoBanner();
            mSmsPromoBannerView.setVisibility(View.VISIBLE);
        }
    }

    private void setBoxTitle(int id) {
        switch (id) {
            case OPTION_INBOX:
                mSpinnerTextView.setText(R.string.inbox);
                break;
            case OPTION_OUTBOX:
                mSpinnerTextView.setText(R.string.outbox);
                break;
            case OPTION_DRAFTBOX:
                mSpinnerTextView.setText(R.string.draftbox);
                break;
            case OPTION_SENTBOX:
                mSpinnerTextView.setText(R.string.sentbox);
                break;
            default:
                Log.d(TAG, "mgViewID = " + id);
                break;
        }
    }

    @Override
    protected void onPause() {
        //mStatusBarManager.hideSIMIndicator(getComponentName());
        StatusBarSelectorCreator.getInstance(this).hideStatusBar();
        super.onPause();
    }
    @Override
    protected void onStart() {
        super.onStart();

        if (!PermissionCheckUtil.checkRequiredPermissions(this)) {
            return;
        }

        if (mListAdapter != null) {
            MmsLog.d(TAG, "set OnContentChangedListener");
            mListAdapter.setOnContentChangedListener(mContentChangedListener);
        }

        if (mNeedUpdateListView) {
            Log.d(TAG, "onStart mNeedUpdateListView");
            //mListAdapter.notifyDataSetChanged();
            mListAdapter.changeCursor(null);
            mNeedUpdateListView = false;
        }
        FolderModeUtils.setMmsDirMode(true);
        //Notify to close dialog mode screen
        new MmsDialogNotifyUtils(this).closeMsgDialog();

        DraftCache.getInstance().addOnDraftChangedListener(this);
      ///M: fix bug ALPS01078057 to disble refresh draftcache. case by case to update the draftcache
//        DraftCache.getInstance().refresh();
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            int mindex = FolderModeUtils.getSimCardInfo();
            if (mindex == 0) {
                where = null;
            } else if (mindex > 0){
                List<SubscriptionInfo> subinfoList = SubscriptionManager
                        .from(MmsApp.getApplication()).getActiveSubscriptionInfoList();
                SubscriptionInfo simInfo = null;
                if (subinfoList != null && subinfoList.size() > 0) {
                    if (subinfoList.size() > 1) {
                        simInfo = subinfoList.get(mindex - 1);
                    } else {
                        simInfo = subinfoList.get(0);
                    }
                }

                // / FIXME: should add SubscriptionInfo
                if (simInfo != null) {
                    where = "sub_id = " + simInfo.getSubscriptionId();
                } else {
                    where = null;
                }
            } else {
                return;
            }
        }
        mNeedToMarkAsSeen = true;
        startAsyncQuery();
        mIsInActivity = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsInActivity = false;
        Contact.invalidateCache();
        DraftCache.getInstance().removeOnDraftChangedListener(this);
        if (mListAdapter != null) {
            MmsLog.d(TAG, "clear OnContentChangedListener");
            mListAdapter.setOnContentChangedListener(null);
        }
        MmsWidgetProvider.notifyDatasetChanged(getApplicationContext());
    }

    @Override
    protected void onDestroy() {
        MmsLog.d(TAG, "onDestroy");

        if (!PermissionCheckUtil.checkRequiredPermissions(this)) {
            super.onDestroy();
            return;
        }

        if (mQueryHandler != null) {
            mQueryHandler.removeCallbacksAndMessages(null);
            mQueryHandler.cancelOperation(DRAFTFOLDER_LIST_QUERY_TOKEN);
            mQueryHandler.cancelOperation(INBOXFOLDER_LIST_QUERY_TOKEN);
            mQueryHandler.cancelOperation(OUTBOXFOLDER_LIST_QUERY_TOKEN);
            mQueryHandler.cancelOperation(SENTFOLDER_LIST_QUERY_TOKEN);
        }
        if (mListAdapter != null) {
            MmsLog.d(TAG, "clear mListAdapter");
            mListAdapter.changeCursor(null);
        }
        unregisterReceiver(mSubReceiver);
        super.onDestroy();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (isTaskRoot()) {
                    moveTaskToBack(false);
                } else {
                    finish();
                }
            return true;
            case KeyEvent.KEYCODE_SEARCH:
                if (mDisableSearchFlag) {
                    return true;
                } else {
                    break;
                }
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void startAsyncQuery() {
        try {
            mNeedQuery = false;
            mIsQuerying = true;
            setProgressBarIndeterminateVisibility(true);
            MmsLog.d(TAG, "startAsyncQuery mgViewID = " + mgViewID);
            switch (mgViewID) {
                case OPTION_INBOX:
                    FolderView.startQueryForInboxView(mQueryHandler,
                            INBOXFOLDER_LIST_QUERY_TOKEN, where);
                    MessagingNotification.cancelNotification(this,
                            MessagingNotification.DOWNLOAD_FAILED_NOTIFICATION_ID);
                    break;
                case OPTION_OUTBOX:
                    FolderView.startQueryForOutBoxView(mQueryHandler,
                            OUTBOXFOLDER_LIST_QUERY_TOKEN, where);
                    MessagingNotification.cancelNotification(this,
                            MessagingNotification.MESSAGE_FAILED_NOTIFICATION_ID);
                    break;
                case OPTION_DRAFTBOX:
                    FolderView.startQueryForDraftboxView(mQueryHandler,
                            DRAFTFOLDER_LIST_QUERY_TOKEN);
                    break;
                case OPTION_SENTBOX:
                    FolderView.startQueryForSentboxView(mQueryHandler,
                            SENTFOLDER_LIST_QUERY_TOKEN, where);
                    break;
                default:
                    break;
            }
        } catch (SQLiteException e) {
            SqliteWrapper.checkSQLiteException(this, e);
        }
    }

    private void startAsyncQuery(int iPostTime) {
        try {
//            setTitle(getString(R.string.refreshing));
//            setProgressBarIndeterminateVisibility(true);
            MmsLog.d(TAG, "startAsyncQuery(int iPostTime) mgViewID = " + mgViewID);
            switch (mgViewID) {
                case OPTION_INBOX:
                    FolderView.startQueryForInboxView(mQueryHandler,
                            INBOXFOLDER_LIST_QUERY_TOKEN, where , iPostTime);
                    MessagingNotification.cancelNotification(this,
                            MessagingNotification.DOWNLOAD_FAILED_NOTIFICATION_ID);
                    break;
                case OPTION_OUTBOX:
                    FolderView.startQueryForOutBoxView(mQueryHandler,
                            OUTBOXFOLDER_LIST_QUERY_TOKEN , where, iPostTime);
                    MessagingNotification.cancelNotification(this,
                            MessagingNotification.MESSAGE_FAILED_NOTIFICATION_ID);
                    break;
                case OPTION_DRAFTBOX:
                    FolderView.startQueryForDraftboxView(mQueryHandler,
                            DRAFTFOLDER_LIST_QUERY_TOKEN , iPostTime);
                    break;
                case OPTION_SENTBOX:
                    FolderView.startQueryForSentboxView(mQueryHandler,
                            SENTFOLDER_LIST_QUERY_TOKEN, where , iPostTime);
                    break;
                default:
                    break;
            }
        } catch (SQLiteException e) {
            SqliteWrapper.checkSQLiteException(this, e);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Cursor cursor = (Cursor) getListView().getItemAtPosition(position);
        if (cursor == null) {
            Log.d(TAG, "cursor == null");
             return;
        }
        int type = cursor.getInt(6);
        int messageid   = cursor.getInt(0);

        if (mActionMode != null && mListAdapter != null) {
            long itemId = mListAdapter.getKey(type, messageid);
            mModeCallBack.setItemChecked(position, !mListAdapter.isContainItemId(itemId));
            mModeCallBack.updateActionMode();
            mListAdapter.notifyDataSetChanged();
            return;
        }

        MmsLog.d(TAG, "messageid =" + messageid + "  mgViewID = " + mgViewID);
        if (mgViewID == OPTION_DRAFTBOX) {  //in draftbox
            long threadId = cursor.getLong(1);
            Intent it = ComposeMessageActivity.createIntent(this, threadId);
            it.putExtra("folderbox", mgViewID);
            it.putExtra("hiderecipient", false); //all draft can show editor
            it.putExtra("showinput", true);
            startActivity(it);
            ///M: fix bug ALPS01078057. when edit draft, set the thread draft state to false.
            /// when edit finish or back, composer will still save, then will reset
            /// the state to true.
            DraftCache.getInstance().setDraftState(threadId, false);
        } else if (type == 1) {  //sms
            Intent intent = new Intent();
            intent.setClass(context, FolderModeSmsViewer.class);
            intent.setData(ContentUris.withAppendedId(SMS_URI, messageid));
            intent.putExtra("msg_type", 1);
            intent.putExtra("folderbox", mgViewID);
            startActivityForResult(intent,REQUEST_CODE_DELETE_RESULT);
        } else if (type == 3) {  //wappush
           //messageid = cursor.getInt(1);
            Intent intent = new Intent();
            intent.setClass(context, FolderModeSmsViewer.class);
            intent.setData(ContentUris.withAppendedId(WAPPUSH_URI, messageid));
            intent.putExtra("msg_type", 3);
            intent.putExtra("folderbox", mgViewID);
            startActivity(intent);
        } else if (type == 4) {  //cb
          //  messageid = cursor.getInt(1);
            Intent intent = new Intent();
            intent.setClass(context, FolderModeSmsViewer.class);
            intent.setData(ContentUris.withAppendedId(CB_URI, messageid));
            intent.putExtra("msg_type", 4);
            intent.putExtra("folderbox", mgViewID);
            startActivity(intent);
        } else if (type == 2) { //mms
            Log.d(TAG, "TYPE1 = " + cursor.getInt(9) + "   mgViewID=" + mgViewID);
            if (cursor.getInt(9) == PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND) {
                if (!mIsSmsEnabled) {
                    Toast.makeText(context, R.string.download_disabled_toast,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                DownloadManager dManager = DownloadManager.getInstance();
                int loadstate = dManager.getState(ContentUris.withAppendedId(MMS_URI, messageid));

                Log.d(TAG, "loadstate = " + loadstate);
                boolean autoDownload = false;
                final int PDU_COLUMN_STATUS = 2;
                final String[] PDU_PROJECTION = new String[] {
                    Mms.MESSAGE_BOX,
                    Mms.MESSAGE_ID,
                    Mms.STATUS,
                };
                Cursor c = getContentResolver().query(
                        ContentUris.withAppendedId(MMS_URI, messageid),
                        PDU_PROJECTION, null, null, null);
                int status = 0;

                try {
                    if (c.moveToFirst()) {
                        status = c.getInt(PDU_COLUMN_STATUS);
                    }
                } finally {
                    c.close();
                }
                Log.v(TAG, "status" + status);

                /* DEFERRED_MASK is not set, it is auto download*/
                if ((status & 0x04) == 0) {
                    autoDownload = true;
                }

                if (loadstate != DownloadManager.STATE_DOWNLOADING && autoDownload == false) {
                    confirmDownloadDialog(new DownloadMessageListener(
                        ContentUris.withAppendedId(MMS_URI, messageid),
                        cursor.getInt(10), messageid));
                } else {
                    Toast.makeText(context, R.string.folder_download, Toast.LENGTH_SHORT).show();
                }
            } else {
                Intent intent = new Intent();
                intent.setClass(context, MmsPlayerActivity.class);
                intent.setData(ContentUris.withAppendedId(MMS_URI, messageid));
                intent.putExtra("dirmode", true);
                intent.putExtra("folderbox", mgViewID);
                startActivityForResult(intent, REQUEST_CODE_DELETE_RESULT);
            }

        }

    }

    private class DownloadMessageListener implements OnClickListener {
        private final Uri mDownloadUri;
        private final int iSubid;
        private final int iMessageid;

        public DownloadMessageListener(Uri sDownloadUri, int mSubid, int msgid) {
            mDownloadUri = sDownloadUri;
            Log.d(TAG, "mDownloadUri =" + mDownloadUri);
            iSubid       = mSubid;
            Log.d(TAG, "iSubid =" + iSubid);
            iMessageid    = msgid;
            Log.d(TAG, "iMessageid =" + iMessageid);
        }

        public void onClick(DialogInterface dialog, int whichButton) {
            markMmsIndReaded(ContentUris.withAppendedId(MMS_URI, iMessageid));
            MessagingNotification.nonBlockingUpdateNewMessageIndicator(FolderViewList.this,
                MessagingNotification.THREAD_NONE, false);
            DownloadManager sManager = DownloadManager.getInstance();
            sManager.setState(ContentUris.withAppendedId(MMS_URI, iMessageid),
                    DownloadManager.STATE_DOWNLOADING);
            Intent intent = new Intent(context, TransactionService.class);
            intent.putExtra(TransactionBundle.URI, mDownloadUri.toString());
            intent.putExtra(TransactionBundle.TRANSACTION_TYPE,
                    Transaction.RETRIEVE_TRANSACTION);
            // add for gemini
            intent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, iSubid);
            context.startService(intent);
        }
    }


//    private final OnCreateContextMenuListener mConvListOnCreateContextMenuListener =
//        new OnCreateContextMenuListener() {
//        public void onCreateContextMenu(ContextMenu menu, View v,
//                ContextMenuInfo menuInfo) {
//            Cursor cursor = mListAdapter.getCursor();
//            if (cursor == null || cursor.getPosition() < 0) {
//                return;
//            }
//
//            int type = cursor.getInt(6);
//            int boxtype = cursor.getInt(11);
//            String recipientIds = cursor.getString(2);
//            ContactList recipients;
//            if(type == 2 || (type == 1 && boxtype ==3) || type == 4){
//                recipients = ContactList.getByIds(recipientIds, false);
//            }else{
//                recipients = ContactList.getByNumbers(recipientIds, false, true);
//            }
//            menu.setHeaderTitle(recipients.formatNames(","));
//
//            if (recipients.size() == 1) {
//                // do we have this recipient in contacts?
//                if (recipients.get(0).existsInDatabase()) {
//                    menu.add(0, MENU_VIEW_CONTACT, 0, R.string.menu_view_contact);
//                } else {
//                    menu.add(0, MENU_ADD_TO_CONTACTS, 0, R.string.menu_add_to_contacts);
//                }
//            }
//        }
//        return super.onContextItemSelected(item);
//    }

    public void onAddContactButtonClickInt(final String number) {
        if (!TextUtils.isEmpty(number)) {
            String message = this.getResources().getString(R.string.add_contact_dialog_message,
                    number);
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                                                         .setTitle(number)
                                                         .setMessage(message);

            AlertDialog dialog = builder.create();

            dialog.setButton(AlertDialog.BUTTON_POSITIVE,
                this.getResources().getString(R.string.add_contact_dialog_existing),
                new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
                    intent.setType(Contacts.CONTENT_ITEM_TYPE);
                    intent.putExtra(ContactsContract.Intents.Insert.PHONE, number);
                        startActivity(intent);
                }
            });

            dialog.setButton(AlertDialog.BUTTON_NEGATIVE, this.getResources().getString(
                    R.string.add_contact_dialog_new),
                        new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    final Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
                    intent.putExtra(ContactsContract.Intents.Insert.PHONE, number);
                        startActivity(intent);
                }

            });
            dialog.show();
        }
    }
    private void forwardMessage(String body) {
        Intent intent = new Intent();
        intent.setClassName(this, "com.android.mms.ui.ForwardMessageActivity");
        intent.putExtra("forwarded_message", true);
        if (body != null) {
            intent.putExtra("sms_body", body);
        }
        startActivity(intent);
    }

//    private class DeleteMessageListener implements OnClickListener {

//        public void onClick(DialogInterface dialog, int whichButton) {
//            mHandler.startDelete(FOLDERVIEW_DELETE_TOKEN,
//                    null, mDeleteUri, null, null);
//            DraftCache.getInstance().updateDraftStateInCache(threadid);
//            dialog.dismiss();
//        }
//    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // We override this method to avoid restarting the entire
        // activity when the keyboard is opened (declared in
        // AndroidManifest.xml).  Because the only translatable text
        // in this activity is "New Message", which has the full width
        // of phone to work with, localization shouldn't be a problem:
        // no abbreviated alternate words should be needed even in
        // 'wide' languages like German or Russian.

        super.onConfigurationChanged(newConfig);
        if (DEBUG) {
            Log.v(TAG, "onConfigurationChanged: " + newConfig);
        }
    }

    ///M: delete all message from menu-->delete all
    private void confirmDeleteMessageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.confirm_dialog_title);
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
        builder.setCancelable(true);
//        builder.setMessage(R.string.confirm_delete_allmessage);

        View contents = View.inflate(context, R.layout.delete_thread_dialog_view, null);
        TextView msg = (TextView) contents.findViewById(R.id.message);
        msg.setText(getString(R.string.confirm_delete_allmessage));

        final CheckBox checkbox = (CheckBox) contents.findViewById(R.id.delete_locked);
        checkbox.setChecked(false);
        checkbox.setVisibility(mHasLockedMsg ? View.VISIBLE : View.GONE);
        builder.setView(contents);

        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface mDialog, int arg1) {
                // TODO Auto-generated method stub
                mDialog.dismiss();
                mQueryHandler.setProgressDialog(DeleteProgressDialogUtil.getProgressDialog(
                    FolderViewList.this));
                mQueryHandler.showProgressDialog();
                Uri mDeleteUri = null;
                if (mgViewID == OPTION_INBOX) {
                    mDeleteUri = ContentUris.withAppendedId(
                            Uri.parse("content://mms-sms/folder_delete/"), 1);
                } else if (mgViewID == OPTION_OUTBOX) {
                    mDeleteUri = ContentUris.withAppendedId(
                            Uri.parse("content://mms-sms/folder_delete/"), 4);
                } else if (mgViewID == OPTION_DRAFTBOX) {
                    mDeleteUri = ContentUris.withAppendedId(
                            Uri.parse("content://mms-sms/folder_delete/"), 3);
                    Cursor cursor = mListAdapter.getCursor();
                    cursor.moveToPosition(-1);
                    mDeletedThreadIds.clear();
                    while (cursor.moveToNext()) {
                        mDeletedThreadIds.add(cursor.getLong(1));
                    }
                } else if (mgViewID == OPTION_SENTBOX) {
                    mDeleteUri = ContentUris.withAppendedId(
                            Uri.parse("content://mms-sms/folder_delete/"), 2);
                }
                String whereClause = null;
                if (mgViewID != OPTION_DRAFTBOX) {
                    whereClause = where;
                    if (!checkbox.isChecked()) {
                        whereClause = where == null ? " locked=0 " : where + " AND locked=0 ";
                    }
                }
                clearMmsCache();
                FolderView.startDeleteBoxMessage(mQueryHandler,
                        FOLDERVIEW_DELETE_TOKEN, mDeleteUri, whereClause);
            }
        });
        builder.setNegativeButton(R.string.no, null);
        mAlertDialog = builder.create();
        mAlertDialog.show();
    }

    private void confirmDownloadDialog(OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.download);
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
        builder.setCancelable(true);
        builder.setMessage(R.string.confirm_download_message);
        builder.setPositiveButton(R.string.download, listener);
        builder.setNegativeButton(R.string.no, null);
        mAlertDialog = builder.create();
        mAlertDialog.show();
    }

    private void showSimInfoSelectDialog() {
        Intent intent = new Intent();
        intent.setClass(context, SubinfoSelectedActivity.class);
        Log.d(TAG, "showSimInfoSelectDialog");
        startActivityForResult(intent, REQUEST_CODE_SELECT_SIMINFO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SELECT_SIMINFO && resultCode == RESULT_OK) {
              where = data.getStringExtra("sub_id");
              Log.d(TAG, "onActivityResult where=" + where);
              startAsyncQuery();
        } else if (requestCode == REQUEST_CODE_DELETE_RESULT && resultCode == RESULT_OK) {
              mNeedUpdateListView = data.getBooleanExtra("delete_flag", false);
              Log.d(TAG, "onActivityResult mNeedUpdateListView =" + mNeedUpdateListView);
        }
    }



    private final OnKeyListener mThreadListKeyListener = new OnKeyListener() {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DEL:
                        long id = getListView().getSelectedItemId();
                        //if (id > 0) {
                            //confirmDeleteThread(id, mQueryHandler);
                        //}
                        return true;
                    default:
                        return false;
                }
            }
            return false;
        }
    };



    /**
     * The base class about the handler with progress dialog function.
     */
    public static abstract class BaseProgressQueryHandler extends AsyncQueryHandler {
        private NewProgressDialog dialog;
        private int progress;

        public BaseProgressQueryHandler(ContentResolver resolver) {
            super(resolver);
        }

        /**
         * Sets the progress dialog.
         * @param dialog the progress dialog.
         */
        public void setProgressDialog(NewProgressDialog cdialog) {
            this.dialog = cdialog;
        }

        /**
         * Sets the max progress.
         * @param max the max progress.
         */
        public void setMax(int max) {
            if (dialog != null) {
                dialog.setMax(max);
            }
        }

        /**
         * Shows the progress dialog. Must be in UI thread.
         */
        public void showProgressDialog() {
            if (dialog != null) {
                dialog.show();
            } else {
                Log.d(TAG, "dialog = null");
            }
        }

        /**
         * Rolls the progress as + 1.
         * @return if progress >= max.
         */
        protected boolean progress() {
            if (dialog != null) {
                Log.d(TAG, "progress =" + progress + ";   dialog.getMax() =" + dialog.getMax());
                return ++progress >= dialog.getMax();
            } else {
                return false;
            }
        }

        /**
         * Dismisses the progress dialog.
         */
        protected void dismissProgressDialog() {
            try {
                if (dialog != null) {
                    dialog.setDismiss(true);
                    dialog.dismiss();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                dialog = null;
            }
        }

      /// M: add for fix bug ALPS01105172.
        protected boolean isHasProgressDialog() {
            return dialog != null;
        }
    }

    private final Runnable mDeleteObsoleteThreadsRunnable = new Runnable() {
        public void run() {
            if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                LogTag.debug("mDeleteObsoleteThreadsRunnable getSavingDraft(): "
                        + DraftCache.getInstance().getSavingDraft());
            }
            if (DraftCache.getInstance().getSavingDraft()) {
                // We're still saving a draft. Try again in a second. We don't
                // want to delete
                // any threads out from under the draft.
                mHandler.postDelayed(mDeleteObsoleteThreadsRunnable, 1000);
            } else {
                MessageUtils.asyncDeleteOldMms();
                Conversation.asyncDeleteObsoleteThreads(mQueryHandler,
                        FOLDERVIEW_DELETE_OBSOLETE_THREADS_TOKEN);
            }
        }
    };

    private final class ThreadListQueryHandler extends BaseProgressQueryHandler {
        public ThreadListQueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            mIsQuerying = false;
            if (cursor == null || cursor.getCount() == 0) {
                Log.d(TAG, "cursor == null||count==0.");
                mCountTextView.setVisibility(View.INVISIBLE);
                if (cursor != null) {
                    mListAdapter.changeCursor(cursor);
                    ///M: add for fix ALPS01448613, when corsor number is 0,
                    /// clear selectedState and update ActionMode @{
                    mListAdapter.clearstate();
                    if (mActionMode != null && mModeCallBack != null) {
                        mModeCallBack.updateActionMode();
                    }
                    /// @}
                }
                setProgressBarIndeterminateVisibility(false);
                if (mNeedQuery && mIsInActivity) {
                    startAsyncQuery();
                }
                if (mNeedToMarkAsSeen) {
                    MessagingNotification.nonBlockingUpdateSendFailedNotification(context);
                }
//                invalidateOptionsMenu();
                return;
            }
            if (mListAdapter == null || mListAdapter.getOnContentChangedListener() == null) {
                MmsLog.d(TAG, "onQueryComplete, no OnContentChangedListener");
                setProgressBarIndeterminateVisibility(false);
                cursor.close();
                return;
            }
            //in this case the adpter should be notifychanged.
            if (mSearchView != null) {
                String searchString = mSearchView.getQuery().toString();
                if (searchString != null && searchString.length() > 0) {
                    Log.d(TAG, "onQueryComplete mSearchView != null");
                    mSearchView.getSuggestionsAdapter().notifyDataSetChanged();
                }
            }
            mCountTextView.setVisibility(View.VISIBLE);
            switch (token) {
            case DRAFTFOLDER_LIST_QUERY_TOKEN:
                mCountTextView.setText("" + cursor.getCount());
                Log.d(TAG, "onQueryComplete DRAFTFOLDER_LIST_QUERY_TOKEN");
                mListAdapter.changeCursor(cursor);

                if (mNeedToMarkAsSeen) {
                    mNeedToMarkAsSeen = false;
                    Conversation.markAllConversationsAsSeen(getApplicationContext(),
                            Conversation.MARK_ALL_MESSAGE_AS_SEEN);
                    // Delete any obsolete threads. Obsolete threads are threads that aren't
                    // referenced by at least one message in the pdu or sms tables. We only call
                    // this on the first query (because of mNeedToMarkAsSeen).
                    mHandler.post(mDeleteObsoleteThreadsRunnable);
                }
                break;
            case INBOXFOLDER_LIST_QUERY_TOKEN:
                if (mNeedToMarkAsSeen) {
                    mNeedToMarkAsSeen = false;
                    Conversation.markAllConversationsAsSeen(getApplicationContext(),
                            Conversation.MARK_ALL_MESSAGE_AS_SEEN);
                    // Delete any obsolete threads. Obsolete threads are threads that aren't
                    // referenced by at least one message in the pdu or sms tables. We only call
                    // this on the first query (because of mNeedToMarkAsSeen).
                    mHandler.post(mDeleteObsoleteThreadsRunnable);
                }
                int count = 0;
                while (cursor.moveToNext()) {
                    if (cursor.getInt(5) == 0) {
                        count++;
                    }
                    if (mActionMode != null /*&& cursor.getInt(6) == 2*/) {
                        if (cursor.getInt(6) == 2) {
                            mCacheMmsList.add(-(long) cursor.getInt(0));
                        } else {
                            mCacheMmsList.add((long) cursor.getInt(0));
                        }
                    }
                }
                if (mActionMode != null) {
                    mListAdapter.removeDownloadItem(mCacheMmsList);
                }
                mCountTextView.setText("" + count + "/" + cursor.getCount());
                Log.d(TAG, "onQueryComplete INBOXFOLDER_LIST_QUERY_TOKEN count " + count);
                mListAdapter.changeCursor(cursor);
                if (mNeedToMarkAsSeen) {
                    MessagingNotification.nonBlockingUpdateSendFailedNotification(context);
                }
                break;
            case OUTBOXFOLDER_LIST_QUERY_TOKEN:
                mCountTextView.setText("" + cursor.getCount());
                Log.d(TAG, "onQueryComplete OUTBOXFOLDER_LIST_QUERY_TOKEN");
                mListAdapter.changeCursor(cursor);
                break;
            case SENTFOLDER_LIST_QUERY_TOKEN:
                mCountTextView.setText("" + cursor.getCount());
                Log.d(TAG, "onQueryComplete SENTFOLDER_LIST_QUERY_TOKEN");
                mListAdapter.changeCursor(cursor);
                break;
            case FOLDERVIEW_HAVE_LOCKED_MESSAGES_TOKEN:
//                Collection<Long> threadIds = (Collection<Long>)cookie;
//                confirmDeleteThreadDialog(new DeleteThreadListener(threadIds, mQueryHandler,
//                    FolderViewList.this), threadIds,
//                        cursor != null && cursor.getCount() > 0,
//                        FolderViewList.this);
                break;

            default:
                Log.e(TAG, "onQueryComplete called with unknown token " + token);
            }

            if (mActionMode != null) {
                mModeCallBack.updateActionMode();
            }
            if (mCacheMmsList != null) {
                mCacheMmsList.clear();
            }
            /** m: add code @{ */
//            invalidateOptionsMenu();
            setDeleteMenuVisible(mOptionsMenu);
            Log.d(TAG, "onQueryComplete invalidateOptionsMenu");
            /** @} */
            setProgressBarIndeterminateVisibility(false);
            Log.d(TAG, "onQueryComplete : mNeedQuery =" + mNeedQuery);
            if (mNeedQuery && mIsInActivity) {
                startAsyncQuery();
            }
            mHasLockedMsg = false;
            if (mListItemLockInfo != null) {
                mListItemLockInfo.clear();
            }
            if (cursor != null) {
                cursor.moveToPosition(-1);
                boolean isLocked = false;
                while (cursor.moveToNext()) {
                    isLocked = cursor.getInt(13) > 0;
                    if (isLocked) {
                        mHasLockedMsg = true;
                    }
                    mListItemLockInfo.put(
                            FolderViewListAdapter.getKey(cursor.getInt(6), cursor.getInt(0)),
                            isLocked);
                }
            }
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
            // When this callback is called after deleting, token is
            // 1803(DELETE_OBSOLETE_THREADS_TOKEN) not 1801(DELETE_CONVERSATION_TOKEN)
            MmsLog.d(TAG, "onDeleteComplete");
            switch (token) {
            case FOLDERVIEW_DELETE_TOKEN:
                if (mDeleteCounter > 1) {
                    mDeleteCounter--;
                    MmsLog.d(TAG, "igonre a onDeleteComplete,mDeleteCounter:" + mDeleteCounter);
                    return;
                }
                mDeleteCounter = 0;
                if (mgViewID == OPTION_DRAFTBOX && !mDeletedThreadIds.isEmpty()) {
                    Iterator iter = mDeletedThreadIds.iterator();
                    while (iter.hasNext()) {
                        DraftCache.getInstance().setDraftState((Long) (iter.next()), false);
                    }
                }
                if (mActionMode != null) {
                    mActionMode.finish();
                }
                // Update the notification for new messages since they
                // may be deleted.
                MessagingNotification.nonBlockingUpdateNewMessageIndicator(FolderViewList.this,
                        MessagingNotification.THREAD_NONE, false);
                // Update the notification for failed messages since they
                // may be deleted.
                //MessagingNotification.updateSendFailedNotification(FolderViewList.this);
                //MessagingNotification.updateDownloadFailedNotification(FolderViewList.this);

                //Update the notification for new WAP Push messages
                if (FeatureOption.MTK_WAPPUSH_SUPPORT) {
                    WapPushMessagingNotification.nonBlockingUpdateNewMessageIndicator(
                            FolderViewList.this, WapPushMessagingNotification.THREAD_NONE);
                }
                CBMessagingNotification.updateAllNotifications(FolderViewList.this);
                // Make sure the list reflects the delete
                //startAsyncQuery();
                mListAdapter.clearbackupstate();
                if (progress()) {
                    dismissProgressDialog();
                }
                break;

            case FOLDERVIEW_DELETE_OBSOLETE_THREADS_TOKEN:
                // Nothing to do here.
                break;
            default:
                break;
            }
        }
    }

    private void markMmsIndReaded(final Uri uri) {
        new Thread(new Runnable() {
            public void run() {
                final ContentValues values = new ContentValues(2);
                values.put("read", 1);
                values.put("seen", 1);
                SqliteWrapper.update(getApplicationContext(),
                        getContentResolver(), uri, values, null, null);
            }
        }).start();
        MessagingNotification.nonBlockingUpdateNewMessageIndicator(
                this, MessagingNotification.THREAD_NONE, false);
    }

    @Override
    public void onDraftChanged(long threadId, boolean hasDraft) {
        // TODO Auto-generated method stub
        Log.d(TAG, "Override onDraftChanged");
        if (mgViewID == OPTION_DRAFTBOX) {
            FolderView.startQueryForDraftboxView(mQueryHandler, DRAFTFOLDER_LIST_QUERY_TOKEN);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // TODO Auto-generated method stub
        if (!isUserHasPerUsingMms) {
            Log.d(TAG, "onCreateOptionsMenu user has no permission");
            return false;
        }
        menu.add(0, MENU_MULTIDELETE, 0, R.string.menu_delete_all_messages);
        getMenuInflater().inflate(R.menu.conversation_list_menu, menu);
        menu.removeItem(R.id.action_delete_all);
        menu.removeItem(R.id.action_debug_dump);
        mSearchItem = menu.findItem(R.id.search);
        mSearchView = (SearchView) mSearchItem.getActionView();

        mSearchView.setOnQueryTextListener(mQueryTextListener);
        mSearchView.setQueryHint(getString(R.string.search_hint));
        mSearchView.setIconifiedByDefault(true);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        if (searchManager != null) {
            SearchableInfo info = searchManager.getSearchableInfo(this.getComponentName());
            mSearchView.setSearchableInfo(info);
        }

        MenuItem cellBroadcastItem = menu.findItem(R.id.action_cell_broadcasts);
        if (cellBroadcastItem != null) {
            // Enable link to Cell broadcast activity depending on the value in config.xml.
            boolean isCellBroadcastAppLinkEnabled = this.getResources().getBoolean(
                    com.android.internal.R.bool.config_cellBroadcastAppLinks);
            try {
                if (isCellBroadcastAppLinkEnabled) {
                    PackageManager pm = getPackageManager();
                    if (pm.getApplicationEnabledSetting("com.android.cellbroadcastreceiver")
                            == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                        isCellBroadcastAppLinkEnabled = false;  // CMAS app disabled
                    }
                }
            } catch (IllegalArgumentException ignored) {
                isCellBroadcastAppLinkEnabled = false;  // CMAS app not installed
            }
            if (!isCellBroadcastAppLinkEnabled) {
                cellBroadcastItem.setVisible(false);
            }
        }
        menu.add(0, MENU_CHANGEVIEW, 0, R.string.changeview);
        menu.add(0, MENU_SIM_SMS, 0, R.string.menu_sim_sms).setIcon(
                R.drawable.ic_menu_sim_sms);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!isUserHasPerUsingMms) {
            Log.d(TAG, "onPrepareOptionsMenu user has no permission");
            return false;
        }
        mOptionsMenu = menu ;
        setDeleteMenuVisible(menu);
        MenuItem item;
        item = menu.findItem(R.id.action_compose_new);
        if (item != null) {
            // Dim compose if SMS is disabled because it will not work (will show a toast)
            item.getIcon().setAlpha(mIsSmsEnabled ? 255 : 127);
        }

        item = menu.findItem(MENU_SIM_SMS);
        List<SubscriptionInfo> listSimInfo = SubscriptionManager.from(MmsApp.getApplication())
                .getActiveSubscriptionInfoList();
        if (MessageUtils.isSimMessageAccessable(this)) {
            item.setVisible(true);
        } else {
            item.setVisible(false);
        }
        if (FeatureOption.MTK_GEMINI_SUPPORT && listSimInfo != null &&
                !listSimInfo.isEmpty() && mgViewID != OPTION_DRAFTBOX) {
            item = menu.findItem(R.id.action_siminfo);
            item.setVisible(true);
        }

        // omacp menu
        item = menu.findItem(R.id.action_omacp);
        item.setVisible(false);
        Context otherAppContext = null;
        try {
            otherAppContext = this.createPackageContext("com.mediatek.omacp",
                    Context.CONTEXT_IGNORE_SECURITY);
        } catch (Exception e) {
            MmsLog.e(CONV_TAG, "ConversationList NotFoundContext");
        }
        if (null != otherAppContext) {
            SharedPreferences sp = otherAppContext.getSharedPreferences("omacp",
                    MODE_WORLD_READABLE | MODE_MULTI_PROCESS);
            boolean omaCpShow = sp.getBoolean("configuration_msg_exist", false);
            if (omaCpShow) {
                item.setVisible(true);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case MENU_CHANGEVIEW:
                FolderModeUtils.setMmsDirMode(false);
                MessageUtils.updateNotification(this);
                startActivity(new Intent(this, ConversationList.class));
                ///M: Modify for ALPS01645990
                DraftCache.getInstance().refresh();
                finish();
                break;
            case R.id.action_compose_new:
                if (mIsSmsEnabled) {
                    Intent intent = new Intent(context, ComposeMessageActivity.class);
                    intent.putExtra("folderbox", mgViewID);
                    startActivity(intent);
                } else {
                    // Display a toast letting the user know they can not compose.
                    if (mComposeDisabledToast == null) {
                        mComposeDisabledToast = Toast.makeText(this,
                                R.string.compose_disabled_toast, Toast.LENGTH_SHORT);
                    }
                    mComposeDisabledToast.show();
                }
                break;
            case R.id.action_settings:
                    Intent sintent = new Intent(this, SettingListActivity.class);
                    startActivityIfNeeded(sintent, -1);
                break;
            case R.id.action_siminfo:
                showSimInfoSelectDialog();
                break;
            case R.id.action_omacp:
                Intent omacpintent = new Intent();
                omacpintent.setClassName("com.mediatek.omacp",
                        "com.mediatek.omacp.message.OmacpMessageList");
                omacpintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityIfNeeded(omacpintent, -1);
                break;
            case R.id.action_wappush:
                Intent wpIntent = new Intent(this, WPMessageActivity.class);
                startActivity(wpIntent);
                break;
            case MENU_MULTIDELETE:
                confirmDeleteMessageDialog();
                break;
            case MENU_SIM_SMS:
                List<SubscriptionInfo> listSubInfo = SubscriptionManager.from(
                        MmsApp.getApplication()).getActiveSubscriptionInfoList();
                if (listSubInfo.size() > 1) {
                        Intent simSmsIntent = new Intent();
                    simSmsIntent.setClass(this, SubSelectActivity.class);
                        simSmsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    simSmsIntent.putExtra(SmsPreferenceActivity.PREFERENCE_KEY,
                                SmsPreferenceActivity.SMS_MANAGE_SIM_MESSAGES);
                    simSmsIntent.putExtra(SmsPreferenceActivity.PREFERENCE_TITLE_ID,
                                R.string.pref_title_manage_sim_messages);
                        startActivity(simSmsIntent);
                } else if (listSubInfo.size() == 1) {
                        Intent simSmsIntent = new Intent();
                        simSmsIntent.setClass(this, ManageSimMessages.class);
                        simSmsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        simSmsIntent.putExtra(PhoneConstants.SUBSCRIPTION_KEY,
                                listSubInfo.get(0).getSubscriptionId());
                        startActivity(simSmsIntent);
                    } else {
                        Toast.makeText(FolderViewList.this,
                                R.string.no_sim_1, Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.action_cell_broadcasts:
                Intent cellBroadcastIntent = new Intent(Intent.ACTION_MAIN);
                cellBroadcastIntent.setComponent(new ComponentName(
                        "com.android.cellbroadcastreceiver",
                        "com.android.cellbroadcastreceiver.CellBroadcastListActivity"));
                cellBroadcastIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(cellBroadcastIntent);
                } catch (ActivityNotFoundException ignored) {
                    Log.e(TAG, "ActivityNotFoundException for CellBroadcastListActivity");
                }
                return true;
            default:
                return true;
        }
        return true;
    }

    SearchView.OnQueryTextListener mQueryTextListener = new SearchView.OnQueryTextListener() {
        public boolean onQueryTextSubmit(String query) {
            Intent intent = new Intent();
            intent.setClass(FolderViewList.this, SearchActivity.class);
            intent.putExtra(SearchManager.QUERY, query);
            startActivity(intent);
            mSearchItem.collapseActionView();
            return true;
        }

        public boolean onQueryTextChange(String newText) {
            return false;
        }
    };

    @Override
    public boolean onSearchRequested() {
        mSearchItem.expandActionView();
        return true;
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        if (state.getBoolean(ACTIONMODE, false)) {
            mListSelectedItem = state.getLongArray(mSELECT_ITEM_IDS);
            mActionMode = this.startActionMode(mModeCallBack);
            Log.d(TAG, "onRestoreInstanceState: start actionMode");
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActionMode != null) {
            Log.d(TAG, "onSaveInstanceState: mActionMode not null");
            outState.putBoolean(ACTIONMODE, true);
            outState.putInt(BOXTYPE, mgViewID);
            MmsLog.d(TAG, "onSaveInstanceState    mgViewID = " + mgViewID);
            outState.putBoolean(NEED_RESTORE_ADAPTER_STATE, true);
            Set<Long> selectItemId = mListAdapter.getBackUpItemList().keySet();
            Long[] selectList = (Long[]) selectItemId.toArray(new Long[selectItemId.size()]);
            long[] selectedList = new long[selectList.length];
            for (int i = 0; i < selectList.length; i++) {
                selectedList[i] = selectList[i].longValue();
            }
            outState.putLongArray(mSELECT_ITEM_IDS, selectedList);
            Log.d(TAG, "onSaveInstanceState--selectItemIds:" + selectedList.toString());
        }
    }

    //    private class ModeCallback implements ListView.MultiChoiceModeListener {
    private class ModeCallback implements ActionMode.Callback {
        private View mMultiSelectActionBarView;
     //   private TextView mSelectedConvCount;
        private MenuItem mDeleteitem;
        private Button mSelectionTitle;
        private boolean mIsSelectAll = false;

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = getMenuInflater();

            mDisableSearchFlag = true;
            mListAdapter.clearstate();
            mListAdapter.clearbackupstate();

            if (mIsNeedRestoreAdapterState) {
                for (int i = 0; i < mListSelectedItem.length; i++) {
                    mListAdapter.setSelectedState(mListSelectedItem[i]);
                }
                Log.d(TAG, "onCreateActionMode: saved selected number "
                        + mListAdapter.getSelectedNumber());
                mIsNeedRestoreAdapterState = false;
            } else {
                Log.d(TAG, "onCreateActionMode: no need to restore adapter state");
            }
            inflater.inflate(R.menu.conversation_multi_select_menu_with_selectall, menu);

            mDeleteitem = menu.findItem(R.id.delete);

            if (mMultiSelectActionBarView == null) {
                mMultiSelectActionBarView = LayoutInflater.from(FolderViewList.this)
                    .inflate(R.layout.conversation_list_multi_select_actionbar2, null);
                /// M: change select tips style
                mSelectionTitle = (Button) mMultiSelectActionBarView
                        .findViewById(R.id.selection_menu);
                //mSelectedConvCount =
                //    (TextView)mMultiSelectActionBarView.findViewById(R.id.selected_conv_count);
            }
            mode.setCustomView(mMultiSelectActionBarView);
            ((Button) mMultiSelectActionBarView.findViewById(R.id.selection_menu))
                .setText(R.string.select_conversations);

            /// M: Code analyze 005, For new feature ALPS00247476, set long clickable . @{
            getListView().setLongClickable(false);
            return true;
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            if (mMultiSelectActionBarView == null) {
                ViewGroup v = (ViewGroup) LayoutInflater.from(FolderViewList.this)
                    .inflate(R.layout.conversation_list_multi_select_actionbar2, null);
                mode.setCustomView(v);
                /// M: change select tips style
                mSelectionTitle = (Button) mMultiSelectActionBarView
                        .findViewById(R.id.selection_menu);
                //mSelectedConvCount = (TextView)v.findViewById(R.id.selected_conv_count);
            }
            /// M: redesign selection action bar and add shortcut in common version. @{
            CustomMenu customMenu = new CustomMenu(FolderViewList.this);
            mSelectionMenu = customMenu.addDropDownMenu(mSelectionTitle, R.menu.selection);
            mSelectionMenuItem = mSelectionMenu.findItem(R.id.action_select_all);
            updateSelectionTitle();
            customMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                //    if (mListAdapter.isAllSelected()) {
                 if (mListAdapter.getCount() == mListAdapter.getItemList().size()) {
                        isSelectAll(false);
                    //    setAllItemChecked(mActionMode, false);
                    } else {
                        isSelectAll(true);
                      //  setAllItemChecked(mActionMode, true);
                    }
                    updateActionMode();
                    return false;
                }
            });
            return true;
        }

        public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.delete:
                    if (mListAdapter.getItemList().size() > 0) {
                        confirmMultiDelete();
                    } else {
                        item.setEnabled(false);
                    }
                    break;
            default:
                break;
            }
            return true;
        }

        public void onDestroyActionMode(ActionMode mode) {
            mListAdapter.clearstate();
            mDisableSearchFlag = false;
            mDeletedThreadIds.clear();

            getListView().setLongClickable(true);
            mActionMode = null;
            if (mListAdapter != null) {
                mListAdapter.notifyDataSetChanged();
            }
        }

        public void setItemChecked(int position, boolean checked) {
            long itemId = mListAdapter.getItemId(position);
            Log.d(TAG, "itemId =" + itemId + " checked =" + checked);
            Cursor cursor = (Cursor) getListView().getItemAtPosition(position);
            long threadId = cursor.getLong(1);
            if (checked) {
                 mDeletedThreadIds.add(threadId);
                 mListAdapter.setSelectedState(itemId);
            } else {
                 mDeletedThreadIds.remove(threadId);
                 mListAdapter.removeSelectedState(itemId);
            }
            int num = mListAdapter.getSelectedNumber();
            if (num > 0) {
        //        mSelectedConvCount.setText(Integer.toString(num));
                if (mDeleteitem != null) {
                    if (num > 0) {
                        mDeleteitem.setEnabled(true);
                    } else {
                        mDeleteitem.setEnabled(false);
                    }
                }
            } else if (mActionMode != null) {
                mActionMode.finish();
            }
            Log.d(TAG, "setItemChecked:checked count = " + num);
        }

        private void isSelectAll(boolean check) {
            cancelSelect();
            if (check) {
                Log.d(TAG, "select all messages, count is : " + mListAdapter.getCount());
                long itemId = -1;
                int selectCount = mListAdapter.getCount();
                for (int i = 0; i < selectCount; i++) {
                    itemId = mListAdapter.getItemId(i);
                    mListAdapter.setSelectedState(itemId);
                }
                mDeleteitem.setEnabled(true);
            } else {
                mDeleteitem.setEnabled(false);
            }
       //     mSelectedConvCount.setText(Integer.toString(mListAdapter.getSelectedNumber()));
            if (mListAdapter != null) {
                mListAdapter.notifyDataSetChanged();
            }
        }
        private void cancelSelect() {
            Log.d(TAG, "cancel select messages.");
            mListAdapter.clearbackupstate();
            mListAdapter.clearstate();
        }

        private DropDownMenu mSelectionMenu;
        private MenuItem mSelectionMenuItem;

        private void updateActionMode() {

            /// M: exit select mode if no item select
            if (mListAdapter.getItemList().size() == 0 && mActionMode != null) {
                mActionMode.finish();
                ///M: add for fix ALPS01448613, when checkedNum == 0,
                /// dismiss the deleteAlertDialog. @{
                if (mAlertDialog != null && mAlertDialog.isShowing()) {
                    mAlertDialog.dismiss();
                    mAlertDialog = null;
                }
                /// @}
            }
            if (mActionMode != null) {
                mActionMode.invalidate();
            }
            updateSelectionTitle();
        }
        private void updateSelectionTitle() {
            if (mListAdapter.getItemList().size() > 0) {
                mSelectionTitle.setText(FolderViewList.this.getResources().getQuantityString(
                    R.plurals.message_view_selected_message_count,
                    mListAdapter.getItemList().size(),
                    mListAdapter.getItemList().size()));
            } else {
                mSelectionTitle.setText(R.string.select_conversations);
            }
            if (mSelectionMenuItem != null) {
                if (mListAdapter.getCount() == mListAdapter.getItemList().size()) {
                    mSelectionMenuItem.setTitle(R.string.unselect_all);
                } else {
                    mSelectionMenuItem.setTitle(R.string.select_all);
                }
            }
        }
    }

    ///M: multi delete message for long press
    private void confirmMultiDelete() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.confirm_dialog_title);
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
        builder.setCancelable(true);
//        builder.setMessage(R.string.confirm_delete_selected_messages);

        View contents = View.inflate(context, R.layout.delete_thread_dialog_view, null);
        TextView msg = (TextView) contents.findViewById(R.id.message);
        msg.setText(getString(R.string.confirm_delete_selected_messages));

        final CheckBox checkbox = (CheckBox) contents.findViewById(R.id.delete_locked);
        checkbox.setChecked(false);
        checkbox.setVisibility(selectedMsgHasLocked() ? View.VISIBLE : View.GONE);
        builder.setView(contents);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                MmsLog.d(TAG, "start to delete message");
                /// M: modify for fix bug ALPS01105172. if user click quickly,
                /// maybe run twice onClick and then set twice new progressdialog;
                ///  after the second time, the first dialog can not be dismissed by and way. @{
                if (!mQueryHandler.isHasProgressDialog()) {
                    mQueryHandler.setProgressDialog(DeleteProgressDialogUtil.getProgressDialog(
                            FolderViewList.this));
                }
                /// @}
                mQueryHandler.showProgressDialog();
                new Thread(new Runnable() {
                    public void run() {
                        Log.d(TAG, "mListAdapter.getBackUpItemList() ="
                                + mListAdapter.getBackUpItemList());
                        boolean clearMmsPduCache = false;
                        mDeleteCounter = 0;
                        Iterator iter = mListAdapter.getBackUpItemList().entrySet().iterator();
                        Uri deleteSmsUri = null;
                        Uri deleteMmsUri = null;
                        Uri deleteCbUri  = null;
                        Uri deleteWpUri = null;
                        Log.d(TAG, "mListAdapter.getSelectedNumber() ="
                                + mListAdapter.getSelectedNumber());
                        String[] argsSms = new String[mListAdapter.getSelectedNumber()];
                        String[] argsMms = new String[mListAdapter.getSelectedNumber()];
                        String[] argsCb = new String[mListAdapter.getSelectedNumber()];
                        // String[] argsWp = new String[mListAdapter.getSelectedNumber()];
                        int i = 0;
                        int j = 0;
                        int k = 0;
                        int m = 0;
                        while (iter.hasNext()) {
                            @SuppressWarnings("unchecked")
                            Map.Entry<Long, Boolean> entry = (Entry<Long, Boolean>) iter.next();
                            if (entry.getValue()) {
                                if (!checkbox.isChecked()) {
                                    if (isMsgLocked(entry.getKey())) {
                                        continue;
                                    }
                                }
                                if (entry.getKey() > 100000) {
                                    deleteWpUri = ContentUris.withAppendedId(WAPPUSH_URI,
                                            entry.getKey() - 100000);
                                    Log.i(TAG, "wappush :entry.getKey()-100000 = "
                                            + (entry.getKey() - 100000));
                                    mDeleteCounter++;
                                    MmsLog.d(TAG, "wappush mDeleteCounter = " + mDeleteCounter);
                                    mQueryHandler.startDelete(FOLDERVIEW_DELETE_TOKEN,
                                            null, deleteWpUri, null, null);
                                    m++;
                                } else if (entry.getKey() < -100000) {
                                    argsCb[k] = Long.toString(-(entry.getKey() + 100000));
                                    Log.i(TAG, "CB :-entry.getKey() +100000= "
                                            + (-(entry.getKey() + 100000)));
                                    Log.i(TAG, "argsSms[i]" + argsCb[k]);
                                    deleteCbUri = CB_URI;
                                    k++;
                                } else if (entry.getKey() < 0) {
                                    argsMms[j] = Long.toString(-entry.getKey());
                                    Log.i(TAG, "mms :-entry.getKey() = " + (-entry.getKey()));
                                    Log.i(TAG, "argsMms[j]" + argsMms[j]);
                                    deleteMmsUri = Mms.CONTENT_URI;
                                    clearMmsPduCache = true;
                                    j++;
                                } else if (entry.getKey() > 0) {
                                    Log.i(TAG, "sms");
                                    argsSms[i] = Long.toString(entry.getKey());
                                    Log.i(TAG, "argsSms[i]" + argsSms[i]);
                                    deleteSmsUri = Sms.CONTENT_URI;
                                    i++;
                                }
                            }
                        }
                        if (clearMmsPduCache) {
                            Log.i(TAG, "confirmMultiDelete : clearMmsPduCache");
                            clearMmsCache();
                        }
                        if (deleteSmsUri != null) {
                            mDeleteCounter++;
                        }
                        if (deleteMmsUri != null) {
                            mDeleteCounter++;
                        }
                        if (deleteCbUri != null) {
                            mDeleteCounter++;
                        }
                        if (mDeleteCounter <= 0) {
                            FolderViewList.this.runOnUiThread(new Runnable() {

                                public void run() {
                                    if (mActionMode != null) {
                                        mActionMode.finish();
                                    }
                                    if (mQueryHandler.progress()) {
                                        mQueryHandler.dismissProgressDialog();
                                    }
                                }
                            });
                        }
                        MmsLog.d(TAG, "mDeleteCounter = " + mDeleteCounter);
                        if (deleteSmsUri != null) {
                            mQueryHandler.startDelete(FOLDERVIEW_DELETE_TOKEN,
                                null, deleteSmsUri, FOR_MULTIDELETE, argsSms);
                        }
                        if (deleteMmsUri != null) {
                            mQueryHandler.startDelete(FOLDERVIEW_DELETE_TOKEN,
                                    null, deleteMmsUri, FOR_MULTIDELETE, argsMms);
                        }
                        if (deleteCbUri != null) {
                            mQueryHandler.startDelete(FOLDERVIEW_DELETE_TOKEN,
                                    null, deleteCbUri, FOR_MULTIDELETE, argsCb);
                        }
                    }
                }).start();
            }
        });
        builder.setNegativeButton(R.string.no, null);
        mAlertDialog = builder.create();
        mAlertDialog.show();
    }

    private class MultiSelectOnLongClickListener implements OnItemLongClickListener {

        public boolean onItemLongClick(AdapterView<?> parent, View view,
                int position, long id) {
            Log.d(TAG, "folder view: MultiSelectOnLongClickListener");
            getListView().setLongClickable(false);
//            mModeCallBack = new ModeCallback();
            mActionMode = startActionMode(mModeCallBack);
            mModeCallBack.setItemChecked(position, true);
            mModeCallBack.updateActionMode();
            if (mListAdapter != null) {
                mListAdapter.notifyDataSetChanged();
            }
            return true;
        }
    }

    private boolean isMsgLocked(long id) {
        if (mListItemLockInfo != null && mListItemLockInfo.containsKey(id)) {
            return mListItemLockInfo.get(id);
        }
        return false;
    }

    private boolean selectedMsgHasLocked() {
        Iterator iter = mListAdapter.getBackUpItemList().entrySet().iterator();
        while (iter.hasNext()) {
            @SuppressWarnings("unchecked")
            Map.Entry<Long, Boolean> entry = (Entry<Long, Boolean>) iter.next();
            if (entry.getValue()) {
                if (isMsgLocked(entry.getKey())) {
                    return true;
                }
            }
        }
        return false;
    }

    /// M: Fix bug ALPS01089027 @{
    private Menu mOptionsMenu;

    private void setDeleteMenuVisible(Menu menu) {
        if (menu != null) {
            MenuItem item = menu.findItem(MENU_MULTIDELETE);
            if (item != null) {
                item.setVisible(mListAdapter.getCount() > 0 && mIsSmsEnabled);
            }
        }
    }
    /// @}

    private void initSmsPromoBanner() {
        /// M: add for Mutli-user, show 'user is not allowed to use SMS' alert if user has no
        // permission to use SMS. @{
        ImageView defaultSmsAppIconImageView =
            (ImageView)mSmsPromoBannerView.findViewById(R.id.banner_sms_default_app_icon);
        TextView permissionAlertView = (TextView) mSmsPromoBannerView
                .findViewById(R.id.sms_permission_alert);
        LinearLayout disabledAlertView = (LinearLayout) mSmsPromoBannerView
                .findViewById(R.id.sms_disabled_alert);
        if (!isUserHasPerUsingMms) {
            mSmsPromoBannerView.setClickable(false);
            permissionAlertView.setVisibility(View.VISIBLE);
            disabledAlertView.setVisibility(View.GONE);
            defaultSmsAppIconImageView.setImageDrawable(getResources().getDrawable(
                    R.drawable.ic_launcher_smsmms));
            return;
        } else {
            mSmsPromoBannerView.setClickable(true);
            permissionAlertView.setVisibility(View.GONE);
            disabledAlertView.setVisibility(View.VISIBLE);
        }
        /// @}
        Log.d("this", "initSmsPromoBanner 1");
        final PackageManager packageManager = getPackageManager();
        final String smsAppPackage = Telephony.Sms.getDefaultSmsPackage(this);

        // Get all the data we need about the default app to properly render the promo banner. We
        // try to show the icon and name of the user's selected SMS app and have the banner link
        // to that app. If we can't read that information for any reason we leave the fallback
        // text that links to Messaging settings where the user can change the default.
        Drawable smsAppIcon = null;
        ApplicationInfo smsAppInfo = null;
        try {
            smsAppIcon = packageManager.getApplicationIcon(smsAppPackage);
            smsAppInfo = packageManager.getApplicationInfo(smsAppPackage, 0);
        } catch (NameNotFoundException e) {
        }
        final Intent smsAppIntent = packageManager.getLaunchIntentForPackage(smsAppPackage);
        Log.d("this", "initSmsPromoBanner 2");
        // If we got all the info we needed
        if (smsAppIcon != null && smsAppInfo != null && smsAppIntent != null) {
            defaultSmsAppIconImageView.setImageDrawable(smsAppIcon);
            TextView smsPromoBannerTitle =
                    (TextView) mSmsPromoBannerView.findViewById(R.id.banner_sms_promo_title);
            String message = getResources().getString(R.string.banner_sms_promo_title_application,
                    smsAppInfo.loadLabel(packageManager));
            smsPromoBannerTitle.setText(message);

            mSmsPromoBannerView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(smsAppIntent);
                }
            });
        } else {
            // Otherwise the banner will be left alone and will launch settings
            mSmsPromoBannerView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Launch settings
                    Intent settingsIntent = new Intent(FolderViewList.this,
                            SettingListActivity.class);
                    startActivityIfNeeded(settingsIntent, -1);
                }
            });
        }
    }

    /// M: update sim state dynamically. @{
    private BroadcastReceiver mSubReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED)) {
                if (mListAdapter != null) {
//                    FolderModeUtils.setSimCardInfo(0);
                    mListAdapter.notifyDataSetChanged();
                }
            }
        }
    };

    private void clearMmsCache() {
        Uri clearCacheUri = null;
        if (mgViewID == OPTION_INBOX) {
            clearCacheUri = Uri.parse("content://mms/inbox");
        } else if (mgViewID == OPTION_OUTBOX) {
            clearCacheUri = Uri.parse("content://mms/outbox");
        } else if (mgViewID == OPTION_DRAFTBOX) {
            clearCacheUri = null;
        } else if (mgViewID == OPTION_SENTBOX) {
            clearCacheUri = Uri.parse("content://mms/sent");
        }
        Log.d(TAG, "clearMmsCache : clearCacheUri = " + clearCacheUri);
        if (clearCacheUri != null) {
            PduCache.getInstance().purge(clearCacheUri);
        }
    }
}
