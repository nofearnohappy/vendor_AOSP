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

import android.content.ActivityNotFoundException;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;


import android.database.Cursor;
import android.database.sqlite.SqliteWrapper;

/// M: ALPS00619099, highlight matched search string in sms viewer @ {
import android.graphics.Typeface;
import android.text.style.StyleSpan;

import android.graphics.drawable.Drawable;
import android.net.MailTo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
//import android.os.RemoteException;

import android.provider.Browser;
import android.provider.Telephony.Sms;


import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.ClipboardManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.view.WindowManager;


import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.TelephonyIntents;

import com.mediatek.cb.cbmsg.CBMessagingNotification;
import com.mediatek.internal.telephony.CellConnMgr;

import com.mediatek.mms.callback.ITextSizeAdjustHost;
import com.mediatek.mms.ext.IOpFolderModeSmsViewerExt;
import com.mediatek.mms.util.MmsDialogNotifyUtils;
import com.mediatek.mms.util.PermissionCheckUtil;
import com.mediatek.opmsg.util.OpMessageUtils;
import com.mediatek.setting.GeneralPreferenceActivity;
import com.mediatek.telephony.TelephonyManagerEx;
import com.mediatek.wappush.WapPushMessagingNotification;

import com.android.mms.R;
import com.android.mms.data.Contact;
import com.android.mms.data.ContactList;
import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.MmsPluginManager;
import com.android.mms.transaction.MessageSender;
import com.android.mms.transaction.MessagingNotification;
import com.android.mms.transaction.SmsSingleRecipientSender;
import com.android.mms.ui.ComposeMessageActivity;
import com.android.mms.ui.ConversationList;
import com.android.mms.ui.DeliveryReportActivity;
import com.android.mms.ui.MessageListItem;
import com.android.mms.ui.MessageUtils;
import com.android.mms.ui.SearchActivity;
import com.android.mms.ui.SubSelectDialog;
import com.android.mms.ui.SubSelectDialog.SubClickAndDismissListener;
import com.android.mms.util.FeatureOption;
import com.android.mms.util.MessageResource;
import com.android.mms.util.MmsLog;
import com.android.mms.util.Recycler;
import com.android.mms.util.SendingProgressTokenManager;
import com.android.mms.widget.MmsWidgetProvider;
import com.google.android.mms.MmsException;

import java.util.ArrayList;
import java.util.List;



import java.util.regex.Matcher;
import java.util.regex.Pattern;
/// @}



/** M:
 * FolderModeSmsViewer
 */
public class FolderModeSmsViewer extends Activity implements Contact.UpdateListener,
              ITextSizeAdjustHost, SubClickAndDismissListener {
    private static final String TAG = "Mms/FolderModeSmsViewer";
    private Uri searchUri;
    private TextView date;
    private TextView recipent;
    private TextView textContent;
    private TextView mThrough;
    private TextView subName;
    private int mSubId;
    private String reciBody; // message text content
    private String reciNumber; // message number, this may be the name
    private String mNumber; //use this save the number
    private String mContactNumber; //use to update this number
    private String reciDate; // message reciDate
    private String reciTime; // message reciTime
    private Long reciDateLong;
    private ContactList mContactList;
    private int threadId;
    private int mMsgBox; //which box the msg in
    private int status;
    private int msgType; //sms wappush+
    private boolean mLocked;
    private long msgid;
    private String mServiceCenter;
    private ImageView mLockedInd;
    private long mSmsSentDate = 0; //received message sentTime
    // Whether or not we are currently enabled for SMS. This field is updated in onStart to make
    // sure we notice if the user has changed the default SMS app.
    private boolean mIsSmsEnabled;

    // This must match the column IDs below.
    private static final String[] SMS_PROJECTION = new String[] {
        "address",     //0
        "date",        //1
        "body",        //2
        "type",        //3
        "thread_id",   //4
        "status",      //5
        "locked",      //6
        "_id",          //7
        "service_center", //8
        "sub_id",    //9
        Sms.DATE_SENT  //10
    };
    private static final String[] WAPPUSH_PROJECTION = new String[] {
        "address",     //0
        "date",        //1
        "text",        //2
        "type",        //3
        "thread_id",   //4
        "error",       //5
        "url",         //6
        "_id",          //7
        "service_center", //8
        "sub_id",         //9
        "locked"            //10
    };
    private static final String[] CB_PROJECTION = new String[] {
        "channel_id",        //0
        "date",         //1
        "body",        //2
        "seen",        //3
        "thread_id",   //4
        "locked",       //5
        "read",       //6
        "_id",       //7
        "sub_id"     //8
    };
    private static final Uri SMS_URI = Uri.parse("content://sms/");
    private static final Uri WAPPUSH_URI = Uri.parse("content://wappush/");
    private static final Uri CB_URI = Uri.parse("content://cb/messages/");
    //menu
    private static final int MENU_REPLY            = Menu.FIRST + 0;
    private static final int MENU_FORWORD          = Menu.FIRST + 1;
    private static final int MENU_RESEND           = Menu.FIRST + 2;
    private static final int MENU_DELETE           = Menu.FIRST + 3;
    private static final int MENU_ADD_CONTACT      = Menu.FIRST + 4;
    private static final int MENU_VIEW_REPORT      = Menu.FIRST + 5;
    private static final int MENU_CALL_RECIPIENT   = Menu.FIRST + 6;
    private static final int MENU_CALL_RECIPIENT_BY_VT  = Menu.FIRST + 7;
    private static final int MENU_LOCK             = Menu.FIRST + 8;
    private static final int MENU_UNLOCK           = Menu.FIRST + 9;

    // Context menu ID
    private static final int MENU_VIEW_MESSAGE_DETAILS      = 17;
    private static final int MENU_DELETE_MESSAGE            = 18;
    private static final int MENU_CALL_BACK                 = 22;
    private static final int MENU_SEND_EMAIL                = 23;
    private static final int MENU_COPY_MESSAGE_TEXT         = 24;
    private static final int MENU_ADD_ADDRESS_TO_CONTACTS   = 27;
    private static final int MENU_LOCK_MESSAGE              = 28;
    private static final int MENU_UNLOCK_MESSAGE            = 29;
    private static final int MENU_SAVE_MESSAGE_TO_SIM       = 31;
    private static final int MENU_SEND_SMS                  = 33;
    private static final int MENU_SELECT_TEXT               = 34;

    //extract telephony number ...
    private ArrayList<String> mURLs = new ArrayList<String>();
    private static final int MENU_ADD_TO_BOOKMARK       = 35;
    //extract telephony number end

    // for save message to sim card
    private Handler mSaveMsgHandler = null;
    private Thread mSaveMsgThread = null;
    private static final int SUB_SELECT_FOR_SEND_MSG                    = 1;
    private static final int SUB_SELECT_FOR_SAVE_MSG_TO_SUB             = 2;
    private static final int MSG_QUIT_SAVE_MESSAGE_THREAD               = 100;
    private static final int MSG_SAVE_MESSAGE_TO_SIM                    = 102;
    private static final int MSG_SAVE_MESSAGE_TO_SUB_AFTER_SELECT_SUB   = 104;
    private static final int MSG_SAVE_MESSAGE_TO_SIM_SUCCEED            = 106;
    private static final int MSG_SAVE_MESSAGE_TO_SIM_FAILED_GENERIC     = 108;
    private static final int MSG_SAVE_MESSAGE_TO_SIM_FAILED_SIM_FULL    = 110;
    private List<SubscriptionInfo> mSubInfoList;
    private int mSubCount = 0;
    //add for gemini
    private int mSelectedSubId;
    private static final String SELECT_TYPE                             = "Select_type";
    private AlertDialog mSubSelectDialog;

    private int mHomeBox = 0;

    private boolean isDlgShow = false;
    private IOpFolderModeSmsViewerExt mOpFolderModeSmsViewerExt = null;

    /// M: ALPS00619099, highlight matched search string in sms viewer @ {
    private Pattern mHighlight = null;
//    private static CellConnMgr mCellMgr = null;
    /// @}

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Log.d(TAG, "onCreate");

        if (PermissionCheckUtil.requestAllPermissions(this)) {
            return;
        }

        //define the activity title
        //requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.foldermode_sms_viewer);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        // getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
        //R.layout.foldermode_smsviewer_title);
        recipent = (TextView) findViewById(R.id.msg_recipent);
        //mTitle = (TextView) findViewById(R.id.sms_viewer_title);
        date = (TextView) findViewById(R.id.msg_date);
        textContent = (TextView) findViewById(R.id.msg_text);
        textContent.setOnCreateContextMenuListener(mContextMenuCreateListener);
        textContent.setOnClickListener(mClickListener);

        /// M: ALPS00619099, highlight matched search string in sms viewer @ {
        String highlightString = getIntent().getStringExtra("highlight");
        mHighlight = highlightString == null
            ? null
            : Pattern.compile(Pattern.quote(highlightString), Pattern.CASE_INSENSITIVE);
        /// @}

        mThrough = (TextView) findViewById(R.id.through_id);
        subName = (TextView) findViewById(R.id.subname_ind);
        Intent intent = getIntent();
        searchUri = intent.getData();
        msgType = intent.getIntExtra("msg_type", 1);
        mHomeBox = intent.getIntExtra("folderbox", 0);
        Log.d(TAG, "the sms intent uri is " + searchUri.getPath());
        initPlugin(this);
        mLockedInd = (ImageView) findViewById(R.id.locked_indicator);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!PermissionCheckUtil.checkAllPermissions(this)) {
            return;
        }

        float textSize = MessageUtils.getPreferenceValueFloat(this,
                        GeneralPreferenceActivity.TEXT_SIZE, 18);
        setTextSize(textSize);
        mOpFolderModeSmsViewerExt.onStart(this, this);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        Contact.addListener(this);
        new MmsDialogNotifyUtils(this).closeMsgDialog();
        /// M: add for update sim state dynamically. @{
        IntentFilter intentFilter
                = new IntentFilter(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);
        this.registerReceiver(mSubReceiver, intentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume,msgType:" + msgType);
        if (searchUri == null) {
            Log.e(TAG, "smsId is wrong");
            return;
        }
        // get all SIM info
        mGetSimInfoRunnable.run();
        String[] projection = SMS_PROJECTION;
        Cursor cursor = null;
        if (msgType == 1) {
            projection = SMS_PROJECTION;
            cursor = getContentResolver().query(
                    searchUri, // URI
                    projection, // projection
                    null, // selection
                    null, // selection args
                    null); // sortOrder
        } else if (msgType == 3) {
            //mTitle.setText(R.string.viewer_title_wappush);
            ActionBar actionBar = getActionBar();
            actionBar.setTitle(R.string.viewer_title_wappush);
            projection = WAPPUSH_PROJECTION;
            cursor = getContentResolver().query(
                searchUri, // URI
                projection, // projection
                null, // selection
                null, // selection args
                null); // sortOrder
        } else if (msgType == 4) {
            //mTitle.setText(R.string.viewer_title_cb);
            ActionBar actionBar = getActionBar();
            actionBar.setTitle(R.string.viewer_title_cb);
            projection = CB_PROJECTION;
            String selection = "_id=" + searchUri.getPathSegments().get(1);
            Log.d(TAG, "query cb selection = " + selection);
            cursor = getContentResolver().query(
                    CB_URI, // URI
                    projection, // projection
                    selection, // selection
                    null, // selection args
                    null); // sortOrder
        }

        try {
            if (cursor == null || cursor.getCount() == 0) {
                Log.w(TAG, "cursor is null");
                return;
            }
            Log.d(TAG, "cursor count = " + cursor.getCount());
            cursor.moveToFirst();
            reciBody = cursor.getString(2);
            mMsgBox = cursor.getInt(3);
            //record sim card
            if (msgType == 4) {
                mSubId = cursor.getInt(8);
            } else {
                mSubId = cursor.getInt(9);
            }
            if (mMsgBox == 3 || msgType == 4) {  //draft
                //come here should be impossible.
                String recipientIds = getRecipientIds(cursor.getInt(4));
                reciNumber = getContactNumber(recipientIds);
            } else {
                reciNumber = getContactNumberByNumber(cursor.getString(0));
            }
            Log.d(TAG, "reciNumber = " + reciNumber);
            if (msgType == 1) {
                mSmsSentDate = cursor.getLong(10);
            }
            String showNumber = "";
            String reDate = "";
            reciDateLong = cursor.getLong(1);
            reciDate = MessageUtils.formatTimeStampString(this, cursor.getLong(1));
            reciTime = MessageUtils.formatTimeStampString(FolderModeSmsViewer.this,
                                                   cursor.getLong(1),true);
            if (mMsgBox == 1 || msgType == 3 || msgType == 4) {
                showNumber = getString(R.string.via_without_time_for_send) + ": " + reciNumber;
                reDate = String.format(getString(R.string.received_on), reciDate);
            } else {
                showNumber = getString(R.string.via_without_time_for_recieve) + ": " + reciNumber;
                reDate = String.format(getString(R.string.sent_on), reciDate);
            }
            threadId = cursor.getInt(4);
            status = cursor.getInt(5);
            Log.d(TAG, "reciNumber = " + showNumber + "\n reciDate = " +
                                reciDate + "\n reciBody = " + reciBody);
            if (msgType == 1) {
                mLocked = cursor.getInt(6) > 0;
                mServiceCenter = cursor.getString(8);
            } else if (msgType == 3) {  //wappush
                String url = cursor.getString(6);
                reciBody = reciBody + "\n" + url;
                mServiceCenter = cursor.getString(8);
                mLocked = cursor.getInt(10) > 0;
            } else if (msgType == 4) {
                mLocked = cursor.getInt(5) > 0;
            }
            msgid = cursor.getLong(7);
            recipent.setText(showNumber);
            date.setText(reDate);
//            SmileyParser parser = SmileyParser.getInstance();
//            textContent.setText(parser.addSmileySpans(reciBody));
            SpannableStringBuilder buf = new SpannableStringBuilder();
            if (!TextUtils.isEmpty(reciBody)) {
                buf.append(reciBody);
            }
            /// M: ALPS00619099, highlight matched search string in sms viewer @ {
            setHighlightText(buf);
            /// @}
            textContent.setText(buf);

            setSubIconAndLabel(mSubId);
            //show card indicator
//            if (FeatureOption.MTK_GEMINI_SUPPORT) {
//                formatSimStatus();
//            }
//            else {
//                mByCard.setVisibility(View.GONE);
//            }
            mLockedInd.setVisibility(mLocked ? View.VISIBLE : View.GONE);

            //update it to has read status
            markSmsRead(msgType);
            Log.d(TAG, " markSmsRead(msgType)");
            boolean isSmsEnabled = MmsConfig.isSmsEnabled(this);
            if (isSmsEnabled != mIsSmsEnabled) {
                mIsSmsEnabled = isSmsEnabled;
            }
            invalidateOptionsMenu();
        } finally {
             if (cursor != null) {
                 cursor.close();
             }
        }
    }

    /// M: ALPS00619099, highlight matched search string in sms viewer @ {
    private void setHighlightText(SpannableStringBuilder buf) {
        MmsLog.d(TAG, " setHighlightText");

        if (mHighlight != null) {
            MmsLog.d(TAG, " highlight ok");
            Matcher m = mHighlight.matcher(buf.toString());
            while (m.find()) {
                buf.setSpan(new StyleSpan(Typeface.BOLD), m.start(), m.end(), 0);
            }
        } else {
            MmsLog.d(TAG, " no highlightt");
        }
    }
    /// @}

    private String getRecipientIds(int mthreadId) {
        Uri uri = Uri.parse("content://mms-sms/thread_id");
        final Uri reUri = ContentUris.withAppendedId(uri, mthreadId);
        Log.d(TAG, "getRecipientIds uri = " + reUri.getPath());
        Cursor c = null;
        String res = "";
        try {
            c = getContentResolver().query(reUri, null, null, null, null);
            if (c == null) {
                Log.e(TAG, "getRecipientIds cursor is null");
                return null;
            }
            Log.e(TAG, "count is " + c.getCount());
            c.moveToFirst();
            res = c.getString(0);
            Log.d(TAG, "getRecipientIds = " + res);
            return res;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private String getContactNumberByNumber(String recNum) {
         if (recNum == null) {
            Log.d(TAG, "getContactNumber recNum is null");
            return getString(android.R.string.unknownName);
         }
         mNumber = recNum;
         ContactList recipients = ContactList.getByNumbers(recNum, true, false);
         String res = "";
         if (recipients != null && !recipients.isEmpty()) {
             //for (Contact contact:recipients) {
             //    contact.reload(true);
             //}
             res = recipients.formatNames(", ");
         } else {
             res = getString(android.R.string.unknownName);
         }
         mContactNumber = res;
         Log.d(TAG, "getContactNumber recNum res IS " + res);
         return res;
    }

    private String getContactNumber(String recipientIds) {
         if (recipientIds == null) {
            Log.d(TAG, "getContactNumber recipientIds is null");
            return getString(android.R.string.unknownName);
         }
         ContactList recipients = ContactList.getByIds(recipientIds, true);
         String res = "";
         if (recipients != null && !recipients.isEmpty()) {
             for (Contact contact:recipients) {
                 contact.reload(true);
             }
             res = recipients.formatNames(", ");
         } else {
             res = getString(android.R.string.unknownName);
         }
         Log.d(TAG, "getContactNumber recipientIds res IS " + res);
         return res;
    }

    private void markSmsRead(int type) {
        Uri readUri = null;
        final ContentValues values = new ContentValues(1);
        values.put("read", 1);
        values.put("seen", 1);
        if (type == 1) {
            readUri = ContentUris.withAppendedId(SMS_URI, msgid);
            SqliteWrapper.update(getApplicationContext(), getContentResolver(),
                                readUri, values, null, null);
        } else if (type == 3) {
            readUri = ContentUris.withAppendedId(WAPPUSH_URI, msgid);
            SqliteWrapper.update(getApplicationContext(), getContentResolver(),
                                readUri, values, null, null);
        } else if (type == 4) {
            String selection = "_id=" + searchUri.getPathSegments().get(1);
            SqliteWrapper.update(getApplicationContext(), getContentResolver(),
                                CB_URI, values, selection, null);
        }

        //cancel the notification
        updateNotification(this, type);
        MmsWidgetProvider.notifyDatasetChanged(getApplicationContext());
    }

    public static void updateNotification(final Context context, final int type) {
        new Thread(new Runnable() {
            public void run() {
                 if (type == 1) {
                     //update sms notification
                     MessagingNotification.blockingUpdateNewMessageIndicator(context,
                         MessagingNotification.THREAD_NONE, false, null);
                     MessagingNotification.nonBlockingUpdateSendFailedNotification(context);
                 } else if (type == 3) {
                     WapPushMessagingNotification.blockingUpdateNewMessageIndicator(context,
                         WapPushMessagingNotification.THREAD_NONE);
                 } else if (type == 4) {
                     CBMessagingNotification.updateNewMessageIndicator(context);
                 }
            }
        }).start();
    }

    private final OnCreateContextMenuListener mContextMenuCreateListener =
                                      new OnCreateContextMenuListener() {

        @Override
        public void onCreateContextMenu(ContextMenu arg0, View arg1, ContextMenuInfo arg2) {
            // TODO Auto-generated method stub
            //addPositionBasedMenuItems(arg0, arg1, arg2);
            arg0.setHeaderTitle(R.string.message_options);
            MsgListMenuClickListener l = new MsgListMenuClickListener();
            addCallAndContactMenuItems(arg0, l);
            if (msgType == 1) {
                arg0.add(0, MENU_COPY_MESSAGE_TEXT, 0,
                        R.string.copy_message_text).setOnMenuItemClickListener(l);
                Log.d(TAG, "mSubCount =" + mSubCount);
                if (mSubCount > 0 && mIsSmsEnabled) {
                    arg0.add(0, MENU_SAVE_MESSAGE_TO_SIM, 0,
                        R.string.save_message_to_sim).setOnMenuItemClickListener(l);
                }
            }
            arg0.add(0, MENU_SELECT_TEXT, 0, R.string.select_text)
            .setOnMenuItemClickListener(l);
            arg0.add(0, MENU_VIEW_MESSAGE_DETAILS, 0, R.string.view_message_details)
            .setOnMenuItemClickListener(l);
            if (mIsSmsEnabled) {
                arg0.add(0, MENU_DELETE_MESSAGE, 0, R.string.delete_message)
                .setOnMenuItemClickListener(l);
                if (msgType != 4) {
                    if (mLocked) {
                        arg0.add(0, MENU_UNLOCK_MESSAGE, 0, R.string.menu_unlock)
                        .setOnMenuItemClickListener(l);
                    } else {
                        arg0.add(0, MENU_LOCK_MESSAGE, 0, R.string.menu_lock)
                        .setOnMenuItemClickListener(l);
                    }
                }
            }
        }
    };

    /**
     * Context menu handlers for the message list view.
     */
    private final class MsgListMenuClickListener implements MenuItem.OnMenuItemClickListener {

        @Override
        public boolean onMenuItemClick(MenuItem arg0) {
            // TODO Auto-generated method stub
            switch(arg0.getItemId()) {
              case MENU_LOCK_MESSAGE:
                  lockMessage(true);
                  return true;
              case MENU_UNLOCK_MESSAGE:
                  lockMessage(false);
                  return true;
              case MENU_SAVE_MESSAGE_TO_SIM:
                  mSaveMsgThread = new SaveMsgThread(msgid);
                  mSaveMsgThread.start();
                  return true;
              case MENU_REPLY:
                  int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
                  MmsLog.d(TAG, "subId:" + mSubId);
                  if (SubscriptionManager.isValidSubscriptionId(mSubId)) {
                      subId = mSubId;
                  }
                  MessageUtils.replyMessage(subId, getBaseContext(), mNumber);
                  return true;
              case MENU_RESEND:
                  resendMsg();
                  return true;
              case MENU_DELETE_MESSAGE:
//                  deleteMsg();
                  confirmToDeleteMessage(searchUri);
                  return true;
              case MENU_COPY_MESSAGE_TEXT:
                 copyToClipboard(reciBody);
                  return true;
              case MENU_VIEW_MESSAGE_DETAILS:
                  String messageDetails = getMessageDetails();
                  new AlertDialog.Builder(FolderModeSmsViewer.this)
                          .setTitle(R.string.message_details_title)
                          .setMessage(messageDetails)
                          .setPositiveButton(android.R.string.ok, null)
                          .setCancelable(true)
                          .show();
                  return true;
              case MENU_ADD_TO_BOOKMARK:
                  if (mURLs.size() == 1) {
                      Browser.saveBookmark(FolderModeSmsViewer.this, null, mURLs.get(0));
                  } else if (mURLs.size() > 1) {
                      CharSequence[] items = new CharSequence[mURLs.size()];
                      for (int i = 0; i < mURLs.size(); i++) {
                          items[i] = mURLs.get(i);
                      }
                      new AlertDialog.Builder(FolderModeSmsViewer.this)
                          .setTitle(R.string.menu_add_to_bookmark)
                          .setIcon(R.drawable.ic_dialog_menu_generic)
                          .setItems(items, new DialogInterface.OnClickListener() {
                              public void onClick(DialogInterface dialog, int which) {
                                  Browser.saveBookmark(FolderModeSmsViewer.this,
                                          null, mURLs.get(which));
                                  }
                              })
                          .show();
                  }
                  return true;
               case MENU_SELECT_TEXT:
                   AlertDialog.Builder dialog = new AlertDialog.Builder(FolderModeSmsViewer.this);
                   LayoutInflater factory = LayoutInflater.from(dialog.getContext());
                   final View textEntryView
                           = factory.inflate(R.layout.alert_dialog_text_entry, null);
                   EditText contentSelector
                           = (EditText) textEntryView.findViewById(R.id.content_selector);
                   contentSelector.setText(reciBody);
                   dialog.setTitle(R.string.select_text)
                         .setView(textEntryView)
                         .setPositiveButton(R.string.yes, null)
                         .show();
                   return true;
              default:
                  return false;
            }
        }
    }

    private String getMessageDetails() {
        StringBuilder details = new StringBuilder();
        Resources res = getResources();

        // Message Type: Text message.
        details.append(res.getString(R.string.message_type_label));
        if (msgType == 1) {
            details.append(res.getString(R.string.text_message));
        } else if (msgType == 3) {
            details.append(res.getString(R.string.wp_msg_type));
        } else if (msgType == 4) {
            details.append(res.getString(R.string.cb_message));
        }

        // Address: ***
        details.append('\n');
        if (mMsgBox == Sms.MESSAGE_TYPE_INBOX) {
            details.append(res.getString(R.string.from_label));
        } else {
            details.append(res.getString(R.string.to_address_label));
        }
        details.append(mNumber);

        //Sent Date for received Message:***
        if (mSmsSentDate > 0 && msgType == 1 && mMsgBox == Sms.MESSAGE_TYPE_INBOX) {
            details.append('\n');
            details.append(res.getString(R.string.sent_label));
            details.append(MessageUtils.formatTimeStampString(FolderModeSmsViewer.this,
                                                 mSmsSentDate, true));
        }
        // Date: ***
        details.append('\n');
        if (mMsgBox == Sms.MESSAGE_TYPE_INBOX) {
            details.append(res.getString(R.string.received_label));
        } else {
            details.append(res.getString(R.string.sent_label));
        }
        details.append(reciTime);

        // Message Center: ***
        if (mMsgBox == 1 || msgType == 3) {
            details.append('\n');
            details.append(res.getString(R.string.service_center_label));
            details.append(mServiceCenter);
        }
        return details.toString();
     }
    private void copyToClipboard(String str) {
        ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clip.setText(str);
    }

    private void deleteMsg() {
        MessageUtils.confirmDeleteMessage(this, searchUri);
    }
    private void lockMessage(final boolean lock) {
//        final Uri lockUri = ContentUris.withAppendedId(SMS_URI, id);

        final ContentValues values = new ContentValues(1);
        values.put("locked", lock ? 1 : 0);

        mLocked = lock;
        new Thread(new Runnable() {
            public void run() {
                getContentResolver().update(searchUri, values, null, null);
                runOnUiThread(new Runnable() {

                    public void run() {
                        mLockedInd.setVisibility(lock ? View.VISIBLE : View.GONE);
                        invalidateOptionsMenu();
                    }
                });
            }
        }, "lockMessage").start();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.slideshow_menu, menu);
        // add extra menu option by condition
        if(mIsSmsEnabled) {
            if (mMsgBox == 1) {
                menu.add(0, MENU_REPLY, 1, R.string.menu_reply);
            } else if (mMsgBox == 5) {
                menu.add(0, MENU_RESEND, 1, R.string.menu_retry_sending);
            }
        }
        //show report
        if (((mMsgBox == Sms.MESSAGE_TYPE_SENT)
           || (mMsgBox == Sms.MESSAGE_TYPE_OUTBOX)
           || (mMsgBox == Sms.MESSAGE_TYPE_QUEUED))
           && (isSms())
           && (status != Sms.STATUS_NONE)) {
            menu.add(0, MENU_VIEW_REPORT, 0, R.string.view_delivery_report);
        }

        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!mIsSmsEnabled){
            if (menu.findItem(R.id.message_forward) != null) {
                menu.removeItem(R.id.message_forward);
            }
            if (menu.findItem(R.id.message_delete) != null) {
                menu.removeItem(R.id.message_delete);
            }
        }
        if (mIsSmsEnabled && isSms()
            && ((mMsgBox == Sms.MESSAGE_TYPE_OUTBOX) || (mMsgBox == Sms.MESSAGE_TYPE_QUEUED))
            && (menu.findItem(MENU_RESEND) == null)) {
            // update sms msgbox, it may be send fail and moved to fail box, need show resend item.
            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(searchUri, SMS_PROJECTION, null, null, null);
                if (cursor != null) {
                    cursor.moveToFirst();
                    mMsgBox = cursor.getInt(3);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            if (mMsgBox == Sms.MESSAGE_TYPE_FAILED) {
                menu.add(0, MENU_RESEND, 1, R.string.menu_retry_sending);
            }
        }
        boolean showAddContact = false;
        // if there is at least one number not exist in contact db, should show add.
        mContactList = ContactList.getByNumbers(mNumber, false, true);
        for (Contact contact : mContactList) {
            if (!contact.existsInDatabase() && MessageUtils.canAddToContacts(contact)) {
                showAddContact = true;
                Log.d(TAG, "not in contact[number:" + contact.getNumber()
                        + ",name:" + contact.getName());
                break;
            }
        }
        boolean menuAddExist = (menu.findItem(MENU_ADD_CONTACT) != null);
        if (showAddContact) {
            if (!menuAddExist) {
                menu.add(0, MENU_ADD_CONTACT, 1, R.string.menu_add_to_contacts);
            }
        } else {
            menu.removeItem(MENU_ADD_CONTACT);
        }
        if (isSms() && menu.findItem(MENU_CALL_RECIPIENT) == null && isRecipientCallable()) {
            MenuItem item = menu.add(0, MENU_CALL_RECIPIENT, 0, R.string.menu_call)
                .setIcon(R.drawable.ic_menu_call)
                .setTitle(R.string.menu_call);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        if(mIsSmsEnabled) {
            if (msgType != 4) {
                if (mLocked) {
                    if (menu.findItem(MENU_LOCK) != null) {
                        menu.removeItem(MENU_LOCK);
                    }
                    if (menu.findItem(MENU_UNLOCK) == null) {
                        menu.add(0, MENU_UNLOCK, 0, R.string.menu_unlock);
                    }
                } else {
                    if (menu.findItem(MENU_UNLOCK) != null) {
                        menu.removeItem(MENU_UNLOCK);
                    }
                    if (menu.findItem(MENU_LOCK) == null) {
                        menu.add(0, MENU_LOCK, 0, R.string.menu_lock);
                    }
                }
            }
        }
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
             case R.id.message_forward:
                // this is a little slow if mms is big.
                forwardMessage(reciBody);
                break;
            case R.id.message_delete:
                confirmToDeleteMessage(searchUri);
                break;
            case MENU_ADD_CONTACT:
                addToContact();
                break;
            case MENU_REPLY:
                int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
                MmsLog.d(TAG, "subId:" + mSubId);
                if (SubscriptionManager.isValidSubscriptionId(mSubId)) {
                    subId = mSubId;
                }
                MessageUtils.replyMessage(subId, this, mNumber);
                break;
            case MENU_RESEND:
                resendMsg();
                break;
            case MENU_VIEW_REPORT:
                showDeliveryReport();
                break;
            case android.R.id.home:
                Intent it = new Intent(this, FolderViewList.class);
                it.putExtra("floderview_key", mHomeBox);
                finish();
                it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(it);
                break;
            case MENU_CALL_RECIPIENT:
                dialRecipient(false);
                break;
            case MENU_CALL_RECIPIENT_BY_VT:
                dialRecipient(true);
                break;
            case MENU_LOCK:
                lockMessage(true);
                break;
            case MENU_UNLOCK:
                lockMessage(false);
                break;

            default:
                return false;
        }
        return true;
    }

    private void confirmToDeleteMessage(final Uri msgUri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.confirm_dialog_title);
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
        builder.setCancelable(true);
        builder.setMessage(mLocked ? R.string.confirm_delete_locked_message :
            R.string.confirm_delete_message);
        builder.setPositiveButton(R.string.delete,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        /// M: fix bug ALPS00351620; for requery searchactivity.
                                        SearchActivity.setNeedRequery();
                                        SqliteWrapper.delete(FolderModeSmsViewer.this,
                                            getContentResolver(), msgUri, null, null);
                                        dialog.dismiss();
                                        Intent mIntent = new Intent();
                                        mIntent.putExtra("delete_flag",true);
                                        setResult(RESULT_OK, mIntent);
                                        finish();
                                        MmsWidgetProvider.notifyDatasetChanged(
                                                getApplicationContext());
                                    }
        });
        builder.setNegativeButton(R.string.no, null);
        builder.show();
    }

    private void forwardMessage(String body) {
        Intent intent = new Intent();
        intent.setClassName(this, "com.android.mms.ui.ForwardMessageActivity");
        intent.putExtra("forwarded_message", true);
        if (body != null) {
            // add for SMS forward with sender
            String smsBody = body;

            Contact contact = Contact.get(mNumber, false);
            String nameAndNumber
                    = Contact.formatNameAndNumber(contact.getName(), contact.getNumber(), "");
            smsBody = mOpFolderModeSmsViewerExt.forwardMessage(FolderModeSmsViewer.this,
                    smsBody, nameAndNumber, mMsgBox);

            intent.putExtra("sms_body", smsBody);
        }
        startActivity(intent);
    }

    private void addToContact() {
        int count = mContactList.size();
        switch(count) {
        case 0:
            Log.e(TAG, "add contact, mCount == 0!");
            break;
        case 1:
            Intent intent = ConversationList.createAddContactIntent(mContactNumber);
            startActivity(intent);
            //MessageUtils.addNumberOrEmailtoContact(reciNumber, 0, this);
            break;
        default:
            //MultiRecipientsActivity.setContactList(mContactList);
            //final Intent i = new Intent(getApplicationContext(), MultiRecipientsActivity.class);
            //startActivity(i);
            break;
        }
    }
    private void resendMsg() {
        try {
            final MessageSender sender = new SmsSingleRecipientSender(this,
                mNumber, reciBody, threadId, status == Sms.STATUS_PENDING,
                searchUri,mSubId);
            final Context ct = this;
            final ContentResolver cr = this.getContentResolver();
            final Uri mUri = this.searchUri;
            MmsLog.d(MmsApp.TXN_TAG, "check pin and...: subId=" + mSubId);
            // add CellConnMgr feature
            final CellConnMgr cellConnMgr = new CellConnMgr(getApplicationContext());
            final int state = cellConnMgr.getCurrentState(mSubId, CellConnMgr.STATE_FLIGHT_MODE
                | CellConnMgr.STATE_SIM_LOCKED | CellConnMgr.STATE_RADIO_OFF);
            MmsLog.d(TAG, "CellConnMgr, state is " + state);
            if (((state & CellConnMgr.STATE_FLIGHT_MODE) == CellConnMgr.STATE_FLIGHT_MODE ) ||
                    ((state & CellConnMgr.STATE_SIM_LOCKED) == CellConnMgr.STATE_SIM_LOCKED ) ||
                    ((state & CellConnMgr.STATE_RADIO_OFF) == CellConnMgr.STATE_RADIO_OFF ) ||
                    ((state & (CellConnMgr.STATE_FLIGHT_MODE | CellConnMgr.STATE_RADIO_OFF))
                                == (CellConnMgr.STATE_FLIGHT_MODE | CellConnMgr.STATE_RADIO_OFF))) {
                final ArrayList<String> stringArray = cellConnMgr.getStringUsingState(mSubId,
                        state);
                MmsLog.d(TAG, "CellConnMgr, stringArray length is " + stringArray.size());
                if (stringArray.size() == 4) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(stringArray.get(0));
                    builder.setMessage(stringArray.get(1));
                    builder.setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    builder.show();
                }
            } else {
                try {
                    if (status == Sms.STATUS_FAILED) {
                        ContentValues cv = new ContentValues();
                        cv.put(Sms.STATUS, Sms.STATUS_NONE);
                        SqliteWrapper.update(ct, cr, mUri, cv, null, null);
                    }
                    sender.sendMessage(SendingProgressTokenManager.NO_TOKEN);
                } catch (MmsException e) {
                    Log.e(TAG, "Can't resend mms.");
                }
                Recycler.getSmsRecycler().deleteOldMessagesByThreadId(
                        getApplicationContext(), threadId);
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to send message: " + searchUri + ", threadId=" + threadId, e);
        }
    }

    private boolean isNumberInContacts(String phoneNumber) {
        return Contact.get(phoneNumber, false).existsInDatabase();
    }

    private void addCallAndContactMenuItems(ContextMenu menu, MsgListMenuClickListener l) {
        // Add all possible links in the address & message
        StringBuilder textToSpannify = new StringBuilder();
        if (isInbox()) {
            textToSpannify.append(reciNumber + ": ");
        }
        textToSpannify.append(reciBody);

        SpannableString msg = new SpannableString(textToSpannify.toString());
        Linkify.addLinks(msg, Linkify.ALL);
        ArrayList<String> uris =
            MessageUtils.extractUris(msg.getSpans(0, msg.length(), URLSpan.class));
        mURLs.clear();
        while (uris.size() > 0) {
            String uriString = uris.remove(0);
            // Remove any duplicates so they don't get added to the menu multiple times
            while (uris.contains(uriString)) {
                uris.remove(uriString);
            }
            String prefix = null;
            int sep = uriString.indexOf(":");
            if (sep >= 0) {
                prefix = uriString.substring(0, sep);
                if ("mailto".equalsIgnoreCase(prefix) || "tel".equalsIgnoreCase(prefix)) {
                    uriString = uriString.substring(sep + 1);
                }
            }
            boolean addToContacts = false;
            if ("mailto".equalsIgnoreCase(prefix)) {
//                String sendEmailString = getString(R.string.menu_send_email)
//                .replace("%s", uriString);
//                Intent intent = new Intent(Intent.ACTION_VIEW,
//                        Uri.parse("mailto:" + uriString));
//                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
//                menu.add(0, MENU_SEND_EMAIL, 0, sendEmailString)
//                .setOnMenuItemClickListener(l).setIntent(intent);
                addToContacts = !MessageUtils.haveEmailContact(uriString, this);
            } else if ("tel".equalsIgnoreCase(prefix)) {
                String callBackString = getString(R.string.menu_call_back).replace("%s", uriString);
                Intent intent = new Intent(Intent.ACTION_DIAL,Uri.parse("tel:" + uriString));
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                menu.add(0, MENU_CALL_BACK, 0, callBackString)
                        .setOnMenuItemClickListener(l).setIntent(intent);

                if (reciBody != null && reciBody.replaceAll("\\-", "").contains(uriString)) {
                    String sendSmsString = getString(
                        R.string.menu_send_sms).replace("%s", uriString);
                    Intent intentSms = new Intent(Intent.ACTION_SENDTO,
                        Uri.parse("smsto:" + uriString));
                    intentSms.setClassName(FolderModeSmsViewer.this,
                            "com.android.mms.ui.SendMessageToActivity");
                    intentSms.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (mIsSmsEnabled) {
                        menu.add(0, MENU_SEND_SMS, 0, sendSmsString)
                        .setOnMenuItemClickListener(l)
                        .setIntent(intentSms);
                    }
                }
                addToContacts = !isNumberInContacts(uriString);
            } else {
                //add URL to book mark
                if (mURLs.size() <= 0) {
                    menu.add(0, MENU_ADD_TO_BOOKMARK, 0, R.string.menu_add_to_bookmark)
                    .setOnMenuItemClickListener(l);
                }
                mURLs.add(uriString);
            }
            if (addToContacts) {
                Intent intent = ConversationList.createAddContactIntent(uriString);
                //Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
                //intent.putExtra(ContactsContract.Intents.Insert.PHONE, uriString);
                String addContactString = getString(
                        R.string.menu_add_address_to_contacts).replace("%s", uriString);
                menu.add(0, MENU_ADD_ADDRESS_TO_CONTACTS, 0, addContactString)
                    .setOnMenuItemClickListener(l)
                    .setIntent(intent);
            }
        }
    }

    private void addPositionBasedMenuItems(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;

        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo");
            return;
        }
        final int position = info.position;

        addUriSpecificMenuItems(menu, v, position);
    }

    private void addUriSpecificMenuItems(ContextMenu menu, View v, int position) {
        Uri uri = getSelectedUriFromMessageList((ListView) v, position);

        if (uri != null) {
            Intent intent = new Intent(null, uri);
            intent.addCategory(Intent.CATEGORY_SELECTED_ALTERNATIVE);
            menu.addIntentOptions(0, 0, 0,
                    new android.content.ComponentName(this, ComposeMessageActivity.class),
                    null, intent, 0, null);
        }
    }

    private Uri getSelectedUriFromMessageList(ListView listView, int position) {
        // If the context menu was opened over a uri, get that uri.
        MessageListItem msglistItem = (MessageListItem) listView.getChildAt(position);
        if (msglistItem == null) {
            // FIXME: Should get the correct view. No such interface in ListView currently
            // to get the view by position. The ListView.getChildAt(position) cannot
            // get correct view since the list doesn't create one child for each item.
            // And if setSelection(position) then getSelectedView(),
            // cannot get corrent view when in touch mode.
            return null;
        }

        int selStart = -1;
        int selEnd = -1;
        TextView textView;
        CharSequence text = null;

        //check if message sender is selected
        textView = (TextView) msglistItem.findViewById(R.id.text_view);
        if (textView != null) {
            selStart = textView.getSelectionStart();
            selEnd = textView.getSelectionEnd();
            text = textView.getText();
        }

        if (selStart == -1) {
            //sender is not being selected, it may be within the message body
            textView = (TextView) msglistItem.findViewById(R.id.body_text_view);
            if (textView != null) {
                selStart = textView.getSelectionStart();
                selEnd = textView.getSelectionEnd();
                text = textView.getText();
            }
        }

        // Check that some text is actually selected, rather than the cursor
        // just being placed within the TextView.
        if (selStart != selEnd) {
            int max = Math.max(selStart, selEnd);
            int min = Math.min(selStart, selEnd);

            URLSpan[] urls = ((Spanned) text).getSpans(min, max,
                                                        URLSpan.class);

            if (urls.length == 1) {
                return Uri.parse(urls[0].getURL());
            }
        }

        //no uri was selected
        return null;
    }

    private OnClickListener mClickListener = new OnClickListener() {
        @Override
        public void onClick(View arg0) {
            // TODO Auto-generated method stub
             onMessageItemClick();
        }
    };

    private void onMessageItemClick() {
         boolean mIsTel = false;
         URLSpan[] spans = textContent.getUrls();
         final java.util.ArrayList<String> urls = MessageUtils.extractUris(spans);
         final String telPrefix = "tel:";
         String url = "";
         for (int i = 0;i < urls.size();i++) {
             url = urls.get(i);
             if (url.startsWith(telPrefix)) {
                 mIsTel = true;
                 if (mIsSmsEnabled) {
                     urls.add("smsto:" + url.substring(telPrefix.length()));
                 }
             }
         }

         if (spans.length == 0) {
             Log.i(TAG, "spans.length == 0");
         } else if (spans.length == 1 && !mIsTel) {
             /*Uri uri = Uri.parse(spans[0].getURL());
             Intent intent = new Intent(Intent.ACTION_VIEW, uri);
             intent.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());
             intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
             startActivity(intent);*/
             final String mUriTemp = spans[0].getURL();
             if (!mUriTemp.startsWith("mailto:")) {  //a url
                 if (!isDlgShow) {
                     isDlgShow = true;
                     AlertDialog.Builder b = new AlertDialog.Builder(FolderModeSmsViewer.this);
                     b.setTitle(MessageResource.string.url_dialog_choice_title);
                     b.setMessage(MessageResource.string.url_dialog_choice_message);
                     b.setCancelable(true);
                     b.setNegativeButton(android.R.string.cancel,
                             new DialogInterface.OnClickListener() {
                         public final void onClick(DialogInterface dialog, int which) {
                             dialog.dismiss();
                         }
                     });
                     b.setPositiveButton(android.R.string.ok,
                             new DialogInterface.OnClickListener() {
                         @Override
                         public void onClick(DialogInterface dialog, int which) {
                             Uri uri = Uri.parse(mUriTemp);
                             Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                             intent.putExtra(Browser.EXTRA_APPLICATION_ID,
                                     FolderModeSmsViewer.this.getPackageName());
                             intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                             FolderModeSmsViewer.this.startActivity(intent);
                         }
                     });
                     AlertDialog aDlg = b.create();
                     aDlg.setOnDismissListener(new DialogInterface.OnDismissListener() {

                         public void onDismiss(DialogInterface dialog) {
                             isDlgShow = false;
                         }
                     });
                     aDlg.show();
                 }
            } else {  //open mail directly
                Uri uri = Uri.parse(mUriTemp);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.putExtra(Browser.EXTRA_APPLICATION_ID,
                        FolderModeSmsViewer.this.getPackageName());
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                FolderModeSmsViewer.this.startActivity(intent);
            }
         } else {
             if (!isDlgShow) {
                 isDlgShow = true;
                 ArrayAdapter<String> adapter =
                     new ArrayAdapter<String>(this, android.R.layout.select_dialog_item, urls) {
                     public View getView(int position, View convertView, ViewGroup parent) {
                         View v = super.getView(position, convertView, parent);
                             String url = getItem(position).toString();
                             TextView tv = (TextView) v;
//                             Drawable d = getPackageManager().getActivityIcon(
//                             new Intent(Intent.ACTION_VIEW,
//                                              Uri.parse(url)));
                             /// M: use default icon to display
                             Drawable d = parseAppIcon(FolderModeSmsViewer.this, url);
                             if (d != null) {
                                 d.setBounds(0, 0, d.getIntrinsicHeight(), d.getIntrinsicHeight());
                                 tv.setCompoundDrawablePadding(10);
                                 tv.setCompoundDrawables(d, null, null, null);
                             }
                             final String telPrefix = "tel:";
                             final String smsPrefix = "smsto:";
                             final String mailPrefix = "mailto";
                             if (url.startsWith(telPrefix)) {
                                 url = PhoneNumberUtils.formatNumber(
                                         url.substring(telPrefix.length()));
                             } else if (url.startsWith(smsPrefix)) {
                                 url = PhoneNumberUtils.formatNumber(
                                         url.substring(smsPrefix.length()));
                             } else if (url.startsWith(mailPrefix)) {
                                 MailTo mt = MailTo.parse(url);
                                 url = mt.getTo();
                             }
                             tv.setText(url);

                         return v;
                     }
                 };

                 AlertDialog.Builder b = new AlertDialog.Builder(this);

                 DialogInterface.OnClickListener click = new DialogInterface.OnClickListener() {
                     public final void onClick(DialogInterface dialog, int which) {
                         if (which >= 0) {
                             Uri uri = Uri.parse(urls.get(which));
                             Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                             intent.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());
                             if (urls.get(which).startsWith("smsto:")) {
                                 intent.setClassName(FolderModeSmsViewer.this,
                                         "com.android.mms.ui.SendMessageToActivity");
                             }
                             intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                             startActivity(intent);
                         }
                         dialog.dismiss();
                     }
                 };

                 b.setTitle(R.string.select_link_title);
                 b.setCancelable(true);
                 b.setAdapter(adapter, click);

                 b.setNegativeButton(android.R.string.cancel,
                         new DialogInterface.OnClickListener() {
                     public final void onClick(DialogInterface dialog, int which) {
                         dialog.dismiss();
                     }
                 });
                 AlertDialog aDlg = b.create();
                 aDlg.setOnDismissListener(new DialogInterface.OnDismissListener() {

                     public void onDismiss(DialogInterface dialog) {
                         isDlgShow = false;
                     }
                 });
                 aDlg.show();
             }
         }
    }

 // save sim message
 private Handler mUiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_SAVE_MESSAGE_TO_SIM_SUCCEED:
                Toast.makeText(FolderModeSmsViewer.this, R.string.save_message_to_sim_successful,
                    Toast.LENGTH_SHORT).show();
                break;

            case MSG_SAVE_MESSAGE_TO_SIM_FAILED_GENERIC:
                Toast.makeText(FolderModeSmsViewer.this, R.string.save_message_to_sim_unsuccessful,
                    Toast.LENGTH_SHORT).show();
                break;

            case MSG_SAVE_MESSAGE_TO_SIM_FAILED_SIM_FULL:
                Toast.makeText(FolderModeSmsViewer.this,
                        getString(R.string.save_message_to_sim_unsuccessful) + ". " +
                        getString(R.string.sim_full_title),
                        Toast.LENGTH_SHORT).show();
                break;

            case MSG_SAVE_MESSAGE_TO_SIM:
                String type = (String)msg.obj;
                long msgId = msg.arg1;
                saveMessageToSim(type, msgId);
                break;

            default:
                Log.d(TAG, "inUIHandler msg unhandled.");
                break;
            }
        }
    };

    private final class SaveMsgThread extends Thread {
        private long msgId = 0;
        public SaveMsgThread(long id) {
            msgId = id;
        }
        public void run() {
            Looper.prepare();
            if (null != Looper.myLooper()) {
                mSaveMsgHandler = new SaveMsgHandler(Looper.myLooper());
            }
            Message msg = mSaveMsgHandler.obtainMessage(MSG_SAVE_MESSAGE_TO_SIM);
            msg.arg1 = (int)msgId;
            if (mSubCount > 1) {
                mUiHandler.sendMessage(msg);
            } else {
                mSaveMsgHandler.sendMessage(msg);
            }
            Looper.loop();
        }
    }

    private final class SaveMsgHandler extends Handler {
        public SaveMsgHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_QUIT_SAVE_MESSAGE_THREAD:
                    Log.d(TAG, "exit save message thread");
                    getLooper().quit();
                    break;
                case MSG_SAVE_MESSAGE_TO_SIM:
                    long msgId = msg.arg1;
                    getMessageAndSaveToSim(msgId);
                    break;
                case MSG_SAVE_MESSAGE_TO_SUB_AFTER_SELECT_SUB:
                    Intent it = (Intent)msg.obj;
                    getMessageAndSaveToSim(it);
                    break;
                default:
                    break;
            }
        }
    }

    private void getMessageAndSaveToSim(Intent intent) {
        Log.d(TAG, "get message and save to sim, selected sim id = " + mSelectedSubId);
        long msgId = intent.getLongExtra("message_id", 0);
        getMessageAndSaveToSim(msgId);
    }

    private void getMessageAndSaveToSim(long msgId) {
        int result = 0;
        String scAddress = null;

        ArrayList<String> messages = null;
        messages = SmsManager.getDefault().divideMessage(reciBody);

        int smsStatus = 0;
        long timeStamp = 0;
        if (isInbox()) {
            smsStatus = SmsManager.STATUS_ON_ICC_READ;
            timeStamp = reciDateLong;
            scAddress = getServiceCenter();
        } else if (isSentbox()) {
            smsStatus = SmsManager.STATUS_ON_ICC_SENT;
        } else if (isFailedbox()) {
            smsStatus = SmsManager.STATUS_ON_ICC_UNSENT;
        } else {
            Log.e(TAG, "Unknown sms status");
        }

        int subId = -1;
        if (mSubCount == 1) {
            mSelectedSubId = mSubInfoList.get(0).getSubscriptionId();
        }
        subId = mSelectedSubId;
        if (scAddress == null) {
            scAddress = TelephonyManagerEx.getDefault().getScAddress(subId);
        }

        Log.d(TAG, "\t subId\t= " + subId);
        Log.d(TAG, "\t scAddress\t= " + scAddress);
        Log.d(TAG, "\t Address\t= " + mNumber);
        Log.d(TAG, "\t msgBody\t= " + reciBody);
        Log.d(TAG, "\t smsStatus\t= " + smsStatus);
        Log.d(TAG, "\t timeStamp\t= " + timeStamp);

        /// FIXME: set default subid to be 0
        result = SmsManager.getSmsManagerForSubscriptionId(subId).copyTextMessageToIccCard(
                scAddress, mNumber,
            messages, smsStatus, timeStamp);
        MmsLog.d(TAG, "\t result\t= " + result);
        /// FIXME: set RESULT_ERROR_SUCCESS to be 0, and RESULT_ERROR_SIM_MEM_FULL to be 7
        if (result == 0) {
            //mSaveMsgHandler.sendEmptyMessage(MSG_SAVE_MESSAGE_TO_SIM_SUCCEED);
            mUiHandler.sendEmptyMessage(MSG_SAVE_MESSAGE_TO_SIM_SUCCEED);
        } else if (result == 7) {
            //mSaveMsgHandler.sendEmptyMessage(MSG_SAVE_MESSAGE_TO_SIM_FAILED_SIM_FULL);
            mUiHandler.sendEmptyMessage(MSG_SAVE_MESSAGE_TO_SIM_FAILED_SIM_FULL);
        } else {
            //mSaveMsgHandler.sendEmptyMessage(MSG_SAVE_MESSAGE_TO_SIM_FAILED_GENERIC);
            mUiHandler.sendEmptyMessage(MSG_SAVE_MESSAGE_TO_SIM_FAILED_GENERIC);
        }
        mSaveMsgHandler.sendEmptyMessageDelayed(MSG_QUIT_SAVE_MESSAGE_THREAD, 5000);
    }

    public void onDialogClick(int subId, Intent intent) {
        mSelectedSubId = (int) subId;
        if (intent.getIntExtra(SELECT_TYPE, -1) == SUB_SELECT_FOR_SEND_MSG) {
            Log.i(TAG, "  SUB_SELECT_FOR_SEND_MSG");
        } else if (intent.getIntExtra(SELECT_TYPE, -1) == SUB_SELECT_FOR_SAVE_MSG_TO_SUB) {
            // getMessageAndSaveToSim(it);
            Message msg = mSaveMsgHandler.obtainMessage(MSG_SAVE_MESSAGE_TO_SUB_AFTER_SELECT_SUB);
            msg.obj = intent;
            // mSaveMsgHandler.sendMessageDelayed(msg, 60);
            mSaveMsgHandler.sendMessage(msg);
        }
    }

    public void onCancelClick() {

    }

    public void onDialogDismiss() {

    }

    private void showSimSelectedDialog(Intent intent) {
        SubSelectDialog subSelectDialog = new SubSelectDialog(FolderModeSmsViewer.this, this);
        mSubSelectDialog = subSelectDialog.showSubSelectedDialog(true, null, intent);
    }

//    public int getSimStatus(int id) {
//        EncapsulatedTelephonyService teleService = EncapsulatedTelephonyService.getInstance();
//        //int slotId = SIMInfo.getSlotById(this,listSimInfo.get(id).mSubId);
//        int slotId = mSubInfoList.get(id).getSlot();
//        if (slotId != -1 && teleService != null) {
//            try {
//                return teleService.getSimIndicatorStateGemini(slotId);
//            } catch (RemoteException e) {
//                Log.e(TAG, "getSimIndicatorStateGemini is failed.\n" + e.toString());
//                return -1;
//            }
//        }
//        return -1;
//    }

    public boolean is3G(int id) {
        int slotId = mSubInfoList.get(id).getSimSlotIndex();
        Log.i(TAG, "is3G SIMInfo.getSlotById id: " + id + " slotId: " + slotId);
        if (slotId == 0) {
            return true;
        }
        return false;
    }

    private void saveMessageToSim(String smsgType, long msgId) {
        Intent intent = new Intent();
        intent.putExtra("message_type", smsgType);
        intent.putExtra("message_id", msgId);
        intent.putExtra(SELECT_TYPE, SUB_SELECT_FOR_SAVE_MSG_TO_SUB);
        showSimSelectedDialog(intent);
    }

    private void getSimInfoList() {
//        if (FeatureOption.MTK_GEMINI_SUPPORT) {
        mSubInfoList =  SubscriptionManager
                .from(MmsApp.getApplication()).getActiveSubscriptionInfoList();
        mSubCount = mSubInfoList == null ? 0 : mSubInfoList.size();
        Log.v(TAG, "getSimInfoList(): mSubCount = " + mSubCount);
//        } else { // single SIM
//            /** M: MTK Encapsulation ITelephony */
//            // ITelephony phone = ITelephony.Stub.asInterface(
//        ServiceManager.checkService("phone"));
//            try {
//                mSubCount = TelephonyManagerEx.getDefault().isSimInsert(0) ? 1 : 0;
//            } catch (RemoteException e) {
//                Log.e(TAG, "check sim insert status failed");
//                mSubCount = 0;
//            }
//        }
    }

    Runnable mGetSimInfoRunnable = new Runnable() {
        public void run() {
            getSimInfoList();
        }
    };

  /// M: update sim state dynamically. @{
    private BroadcastReceiver mSubReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED)) {
                    new Thread(mGetSimInfoRunnable).start();
                    setSubIconAndLabel(mSubId);
            }
        }
    };

    private boolean isInbox() {
        return mMsgBox == 1;
    }

    private boolean isSms() {
        return msgType == 1;
    }

    private boolean isSentbox() {
        return mMsgBox == 2;
    }

    private boolean isFailedbox() {
        return mMsgBox == 5;
    }

    private String getServiceCenter() {
        return mServiceCenter;
    }

    private void showDeliveryReport() {
        Intent intent = new Intent(this, DeliveryReportActivity.class);
        intent.putExtra("message_id", msgid);
        intent.putExtra("message_type", "sms");
        startActivity(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Contact.removeListener(this);
        unregisterReceiver(mSubReceiver);
    }

    // We don't want to show the "call" option unless there is only one
    // recipient and it's a phone number.
    private boolean isRecipientCallable() {
        return (mContactList.size() == 1 && !mContactList.containsEmail());
    }

    private void dialRecipient(boolean isVideoCall) {
        if (isRecipientCallable()) {
            String number = mContactList.get(0).getNumber();
            Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number));
            if (isVideoCall) {
                dialIntent.putExtra("com.android.phone.extra.video", true);
            }
            startActivity(dialIntent);
        }
    }

    public void onUpdate(Contact updated) {
        MmsLog.d(TAG, "onUpdate,update number and name:" + updated.getNumber()
                + "," + updated.getName());
        if (updated.getNumber().equals(mContactNumber)) {
            if (!updated.getName().equals(reciNumber)) {
                reciNumber = updated.getName();
                String showNumber = "";
                if (mMsgBox == 1 || msgType == 3 || msgType == 4) {
                    showNumber = getString(R.string.via_without_time_for_send) + ": " + reciNumber;
                } else {
                    showNumber = getString(R.string.via_without_time_for_recieve)
                            + ": " + reciNumber;
                }
                final String showString = showNumber;
                runOnUiThread(new Runnable() {
                    public void run() {
                        recipent.setText(showString);
                    }
                });
            }
        }
    }

    @Override
    public void startActivity(Intent intent) {
        try {
            super.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Intent mChooserIntent = Intent.createChooser(intent, null);
            super.startActivity(mChooserIntent);
        }
    }

    public void setTextSize(float size) {
        if (textContent != null) {
            textContent.setTextSize(size);
        }
    }

    private void initPlugin(Context context) {
        mOpFolderModeSmsViewerExt = OpMessageUtils.getOpMessagePlugin()
                .getOpFolderModeSmsViewerExt();
    }

    public boolean  dispatchTouchEvent(MotionEvent ev) {
        boolean ret = false;
        if (mOpFolderModeSmsViewerExt != null) {
            ret = mOpFolderModeSmsViewerExt.dispatchTouchEvent(ev);
        }
        if (!ret) {
            ret = super.dispatchTouchEvent(ev);
        }
        return ret;
    }

    /**
     * M: Use default icon to display
     */
    private Drawable parseAppIcon(Context context, String url) {
        final String telPrefix = "tel:";
        final String smsPrefix = "smsto:";
        final String mailPrefix = "mailto";
        int drawableId;

        if (url.startsWith(telPrefix)) {
            drawableId = R.drawable.common_phone;
        } else if (url.startsWith(smsPrefix)) {
            drawableId = R.drawable.common_message;
        } else if (url.startsWith(mailPrefix)) {
            drawableId = R.drawable.common_email;
        } else {
            drawableId = R.drawable.common_browser;
        }
        return context.getResources().getDrawable(drawableId);
    }

    private void setSubIconAndLabel(int subId) {
        Log.i(TAG, "setSubIconAndLabel subId=" +  subId);
        SubscriptionInfo subInfo = SubscriptionManager.from(MmsApp.getApplication())
                .getActiveSubscriptionInfo(subId);
        Log.i(TAG, "subInfo=" + subInfo);
        if (null != subInfo) {
            if (subInfo.getSimSlotIndex() == SubscriptionManager.SIM_NOT_INSERTED ||
                subInfo.getSimSlotIndex() == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                Log.i(TAG, "current not insert sim card");
                subName.setVisibility(View.GONE);
            } else {
                subName.setVisibility(View.VISIBLE);
                subName.setTextColor(subInfo.getIconTint());
                subName.setText(subInfo.getDisplayName().toString());
            }
        }
   }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (PermissionCheckUtil.requestAllPermissions(this)) {
            MmsLog.d(TAG, "onRestart() requestAllPermissions return !!");
            return;
        }
    }
}