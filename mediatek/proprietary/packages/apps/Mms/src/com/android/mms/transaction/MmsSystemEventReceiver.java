/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
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

package com.android.mms.transaction;

import static android.provider.BaseColumns._ID;

import java.io.File;
import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Telephony.Mms;
import android.util.Log;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.mms.LogTag;
import com.android.mms.MmsApp;
import com.android.mms.TempFileProvider;

/// M:
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SqliteWrapper;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.MmsSms.PendingMessages;
import android.telephony.SubscriptionManager;

import com.android.mms.MmsConfig;
import com.android.mms.MmsPluginManager;
import com.android.mms.ui.ComposeMessageActivity;
import com.android.mms.ui.ConversationList;
import com.android.mms.ui.MessageUtils;
import com.android.mms.ui.SmsTemplateEditActivity;
import com.android.mms.util.DownloadManager;
import com.android.mms.util.DraftCache;
import com.android.mms.util.MuteCache;
import com.android.mms.R;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduPersister;
import com.android.mms.util.MmsLog;
import com.android.mms.widget.MmsWidgetProvider;

import android.provider.Telephony;
import com.mediatek.ipmsg.util.IpMessageUtils;
import com.mediatek.mms.ext.IOpMmsSystemEventReceiverExt;
import com.mediatek.mms.util.PermissionCheckUtil;
import com.mediatek.opmsg.util.OpMessageUtils;
import com.mediatek.storage.StorageManagerEx;

/**
 * MmsSystemEventReceiver receives the
 * {@link android.content.intent.ACTION_BOOT_COMPLETED},
 * {@link com.android.internal.telephony.TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED}
 * and performs a series of operations which may include:
 * <ul>
 * <li>Show/hide the icon in notification area which is used to indicate
 * whether there is new incoming message.</li>
 * <li>Resend the MM's in the outbox.</li>
 * </ul>
 */
public class MmsSystemEventReceiver extends BroadcastReceiver {
    private static final String TAG = "MmsSystemEventReceiver";
    private static MmsSystemEventReceiver sMmsSystemEventReceiver;
    private static Handler sHandler = null;
    private static Runnable sRunnable = null;

    private static IOpMmsSystemEventReceiverExt sOpMmsSystemEventReceiverExt = null;

    private static void wakeUpService(Context context) {
        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            Log.v(TAG, "wakeUpService: start transaction service ...");
        }

        context.startService(new Intent(context, TransactionService.class));
    }

    public static void delayWakeupService(final Context context) {
        sHandler = new Handler();
        sRunnable = new Runnable() {
            @Override
            public void run() {
                wakeUpService(context);
            }
        };
        sHandler.postDelayed(sRunnable, 20000L);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            Log.v(TAG, "Intent received: " + intent);
        }
        /// M:
        MmsLog.d(MmsApp.LOG_TAG, "onReceive(): intent=" + intent.toString());
        String action = intent.getAction();

        /// Add for runtime permission @{
        if (!PermissionCheckUtil.checkRequiredPermissions(context)) {
            MmsLog.d(MmsApp.LOG_TAG, "MmsSystemEventReceiver no permissions return !!");
            return;
        } else {
            MmsLog.d(MmsApp.LOG_TAG, "MmsSystemEventReceiver onReceive() has permisson DraftCache: "
                    + DraftCache.getInstance());
            if (DraftCache.getInstance() == null) {
                MmsApp.getApplication().onRequestPermissionsResult();
            }
        }
        /// @}

        sOpMmsSystemEventReceiverExt = OpMessageUtils.getOpMessagePlugin()
                .getOpMmsSystemEventReceiverExt();
        sOpMmsSystemEventReceiverExt.onReceive(
                context, intent, TempFileProvider.TEMP_FILENAME_LENGTH);

        if (action.equals(Mms.Intents.CONTENT_CHANGED_ACTION)) {
        /// M:Code analyze 003, put in a new thread @{
            final Intent mIntent = intent;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Uri changed = (Uri) mIntent.getParcelableExtra(Mms.Intents.DELETED_CONTENTS);
                    if (changed != null) {
                        MmsApp.getApplication().getPduLoaderManager().removePdu(changed);
                    }
                    MmsLog.d(MmsApp.TXN_TAG, "Mms.Intents.CONTENT_CHANGED_ACTION: " + changed);
                }
            }).start();
        /// @}
        } else if (action.equals(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED)) {
            String state = intent.getStringExtra(PhoneConstants.STATE_KEY);

            if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                Log.v(TAG, "ANY_DATA_STATE event received: " + state);
            }
            /// M:Code analyze 004, modify the logic,if data connection change,unRegister the listener of
            /// connection changed and wake up transaction service,and add msim logic @{
            String apnType = intent.getStringExtra(PhoneConstants.DATA_APN_TYPE_KEY);

            //if (state.equals("CONNECTED")) {
            if (PhoneConstants.APN_TYPE_MMS.equals(apnType)) {
                MmsLog.d(MmsApp.TXN_TAG, "TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED, type is mms.");
                // if the network is not available for mms, keep listening
                ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo ni = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_MMS);
                if (ni != null && !ni.isAvailable()) {
                    MmsLog.d(MmsApp.TXN_TAG, "network is not available for mms, keep listening.");
                    return;
                }

                unRegisterForConnectionStateChanges(context);

                wakeUpService(context);

            }
            /// @}
        } else if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            if (sHandler != null && sRunnable != null) {
                MmsLog.d(MmsApp.TXN_TAG, "Intent.ACTION_BOOT_COMPLETED, remove startService");
                sHandler.removeCallbacks(sRunnable);
                sHandler = null;
                sRunnable = null;
            }

            /// M:Code analyze 005, mark pending msg failed after boot complete @{
            MmsLog.d(MmsApp.TXN_TAG, "Intent.ACTION_BOOT_COMPLETED");
            final Context contxt = context;
            new Thread(new Runnable() {
                public void run() {
                    // add for ipmessage
                    IpMessageUtils.onIpBootCompleted(contxt.getApplicationContext());
                    setPendingMmsFailed(contxt);
                    setNotificationIndUnstarted(contxt);
                    MmsWidgetProvider.notifyDatasetChanged(contxt.getApplicationContext());
                }
            }).start();
            /// @}
            // We should check whether there are unread incoming
            // messages in the Inbox and then update the notification icon.
            // Called on the UI thread so don't block.
            MessagingNotification.nonBlockingUpdateNewMessageIndicator(
                    context, MessagingNotification.THREAD_NONE, false);
        /// M:Code analyze 001,add for save draft mms when received event of shutdown @{
        } else if (action.equals(Intent.ACTION_SHUTDOWN)) {
            mSaveDraft = (OnShutDownListener) ComposeMessageActivity.getComposeContext();
            if (mSaveDraft != null) {
                mSaveDraft.onShutDown();
            }
        /// @}
        /// M:Code analyze 002,add for listening the changing of sim info @{
        } else if (action.equals(Intent.ACTION_SMS_DEFAULT_SIM_CHANGED)) {
            MmsLog.d(MmsApp.LOG_TAG, "SMS default SIM changed.");
            mSubInforChangedListener = (OnSubInforChangedListener) ComposeMessageActivity
                    .getComposeContext();
            if (mSubInforChangedListener != null) {
                mSubInforChangedListener.onSubInforChanged();
            }
            mSubInforChangedListener = (OnSubInforChangedListener) ConversationList.getContext();
            if (mSubInforChangedListener != null) {
                mSubInforChangedListener.onSubInforChanged();
            }
        /// @}
        /// M:Code analyze 007,add for listening device storage full or not @{
        } else if (action.equals(Intent.ACTION_DEVICE_STORAGE_FULL)) {
            MmsConfig.setDeviceStorageFullStatus(true);
        } else if (action.equals(Intent.ACTION_DEVICE_STORAGE_NOT_FULL)) {
            MmsConfig.setDeviceStorageFullStatus(false);
            MessagingNotification.cancelNotification(context,
                    SmsRejectedReceiver.SMS_REJECTED_NOTIFICATION_ID);
        /// @}
        /// M: new feature, add default quick_text @{
        } else if (action.equals(Intent.ACTION_LOCALE_CHANGED)) {
            if (MmsConfig.getInitQuickText()) {
                return;
            }
            MmsConfig.getQuicktexts().clear();
            MmsConfig.getQuicktextsId().clear();
        }
        /// @}
    }

    public static void registerForConnectionStateChanges(Context context) {
        /// M:
        MmsLog.d(MmsApp.TXN_TAG, "registerForConnectionStateChanges");
        unRegisterForConnectionStateChanges(context);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            Log.v(TAG, "registerForConnectionStateChanges");
        }
        if (sMmsSystemEventReceiver == null) {
            sMmsSystemEventReceiver = new MmsSystemEventReceiver();
        }

        context.registerReceiver(sMmsSystemEventReceiver, intentFilter);
    }

    public static void unRegisterForConnectionStateChanges(Context context) {
        /// M:
        MmsLog.d(MmsApp.TXN_TAG, "unRegisterForConnectionStateChanges");
        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            Log.v(TAG, "unRegisterForConnectionStateChanges");
        }
        if (sMmsSystemEventReceiver != null) {
            try {
                context.unregisterReceiver(sMmsSystemEventReceiver);
            } catch (IllegalArgumentException e) {
                // Allow un-matched register-unregister calls
            }
        }
    }

    /// M: new members and new methods
    /// M:Code analyze 001,add a new variable for save draft mms when received event of shutdown @{
    private OnShutDownListener mSaveDraft;
    /// @}
    /// M:Code analyze 002,add for listening the changing of sim info @{
    private OnSubInforChangedListener mSubInforChangedListener;
    /// @}

    /// M:Code analyze 009,add for setting the pending mms failed,mainly using after boot complete @{
    public static void setPendingMmsFailed(final Context context) {
        MmsLog.d(MmsApp.TXN_TAG, "setPendingMmsFailed");
        Cursor cursor = PduPersister.getPduPersister(context).getPendingMessages(
                Long.MAX_VALUE/*System.currentTimeMillis()*/);
        if (cursor != null) {
            try {
                int count = cursor.getCount();
                MmsLog.d(MmsApp.TXN_TAG, "setPendingMmsFailed: Pending Message Size=" + count);

                if (count == 0) {
                    return;
                }
                DefaultRetryScheme scheme = new DefaultRetryScheme(context, 100);
                ContentValues values = null;
                int columnIndex = 0;
                int columnType = 0;
                int id = 0;
                int type = 0;
                while (cursor.moveToNext()) {
                    columnIndex = cursor.getColumnIndexOrThrow(PendingMessages._ID);
                    id = cursor.getInt(columnIndex);

                    columnType = cursor.getColumnIndexOrThrow(PendingMessages.MSG_TYPE);
                    type = cursor.getInt(columnType);

                    MmsLog.d(MmsApp.TXN_TAG, "setPendingMmsFailed: type=" + type + "; MsgId=" + id);

                    if (type == PduHeaders.MESSAGE_TYPE_SEND_REQ) {
                        values = new ContentValues(2);
                        values.put(PendingMessages.ERROR_TYPE,  MmsSms.ERR_TYPE_GENERIC_PERMANENT);
                        values.put(PendingMessages.RETRY_INDEX, scheme.getRetryLimit());
                        SqliteWrapper.update(context,
                                context.getContentResolver(),
                                PendingMessages.CONTENT_URI,
                                values, PendingMessages._ID + "=" + id, null);
                        columnIndex = cursor.getColumnIndexOrThrow(PendingMessages.MSG_ID);
                        int msgId = cursor.getInt(columnIndex);
                        Uri msgUri = Uri.withAppendedPath(Mms.CONTENT_URI, Integer.toString(msgId));
                        ContentValues readValues = new ContentValues(1);
                        readValues.put(Mms.READ, 0);
                        SqliteWrapper.update(context, context.getContentResolver(), msgUri,
                                readValues, null, null);
                    }
                }
                MessagingNotification.notifySendFailed(context, true);
            } catch (SQLiteDiskIOException e) {
                // Ignore
                MmsLog.e(MmsApp.TXN_TAG, "SQLiteDiskIOException caught while set pending message failed", e);
            } finally {
                cursor.close();
            }
        } else {
            MmsLog.d(MmsApp.TXN_TAG, "setPendingMmsFailed: no pending MMS.");
        }
    }
    /// @}

    /// M:Code analyze 011,add for setting the mms being downloading when shutdown to unrecognized
    /// after boot complete again,have to manual download @{
    public static void setNotificationIndUnstarted(final Context context) {
        MmsLog.d(MmsApp.TXN_TAG, "setNotificationIndUnstarted");
        Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(), Mms.CONTENT_URI,
                new String[] {Mms._ID, Mms.STATUS, Mms.THREAD_ID},
                Mms.MESSAGE_TYPE + "=" + PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND, null, null);
        if (cursor != null) {
            try {
                int count = cursor.getCount();
                MmsLog.d(MmsApp.TXN_TAG, "setNotificationIndUnstarted: Message Size=" + count);

                if (count == 0) {
                    return;
                }

                ContentValues values = null;
                int id = 0;
                int status = 0;
                while (cursor.moveToNext()) {
                    id = cursor.getInt(0);
                    status = cursor.getInt(1);
                    MmsLog.d(MmsApp.TXN_TAG, "setNotificationIndUnstarted: MsgId=" + id + "; status=" + status);

                    if (DownloadManager.STATE_DOWNLOADING == (status & ~DownloadManager.DEFERRED_MASK)) {
                        values = new ContentValues(1);
                        values.put(Mms.STATUS, DownloadManager.STATE_PERMANENT_FAILURE);
                        values.put(Mms.READ, 0);
                        sOpMmsSystemEventReceiverExt.setNotificationIndUnstarted(values, status);
                        SqliteWrapper.update(context,
                                context.getContentResolver(),
                                Mms.CONTENT_URI,
                                values, Mms._ID + "=" + id, null);
                        MessagingNotification.notifyDownloadFailed(context, cursor.getInt(2));
                    }
                }
            } catch (SQLiteDiskIOException e) {
                // Ignore
                MmsLog.e(MmsApp.TXN_TAG, "SQLiteDiskIOException caught while set notification ind unstart", e);
            } finally {
                cursor.close();
            }
        } else {
            MmsLog.d(MmsApp.TXN_TAG, "setNotificationIndUnstarted: no pending messages.");
        }
    }
    /// @}

    /// M:Code analyze 002,new interface,add for listening the changing of sub info @{
    public interface OnSubInforChangedListener {
        void onSubInforChanged();
    }
    /// @}

    /// M:Code analyze 001,a new interface for listening the event of shut down @{
    public interface OnShutDownListener {
        void onShutDown();
    }
    /// @}
}
