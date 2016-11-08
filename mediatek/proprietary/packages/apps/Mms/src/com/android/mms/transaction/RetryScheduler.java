/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
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

package com.android.mms.transaction;

import com.android.mms.R;
import com.android.mms.LogTag;
import com.android.mms.util.DownloadManager;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduPersister;
import com.mediatek.mms.ext.IOpRetrySchedulerExt;
import com.mediatek.opmsg.util.OpMessageUtils;

import android.database.sqlite.SqliteWrapper;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.MmsSms.PendingMessages;
import android.util.Log;
/// M:
import android.os.SystemClock;

import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.util.MmsLog;


public class RetryScheduler implements Observer {
    private static final String TAG = "RetryScheduler";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = false;

    private final Context mContext;
    private final ContentResolver mContentResolver;

    private IOpRetrySchedulerExt mOpRetrySchedulerExt;

    private RetryScheduler(Context context) {
        mContext = context;
        mContentResolver = context.getContentResolver();
        mOpRetrySchedulerExt = OpMessageUtils.getOpMessagePlugin()
                .getOpRetrySchedulerExt();
    }

    private static RetryScheduler sInstance;
    public static RetryScheduler getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new RetryScheduler(context);
        }
        return sInstance;
    }

    private boolean isConnected() {
        ConnectivityManager mConnMgr = (ConnectivityManager)
                mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = mConnMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_MMS);
        return (ni == null ? false : ni.isConnected());
    }

    public void update(Observable observable) {
        try {
            Transaction t = (Transaction) observable;

            if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                Log.v(TAG, "[RetryScheduler] update " + observable);
            }

            // We are only supposed to handle M-Notification.ind, M-Send.req
            // and M-ReadRec.ind.
            if ((t instanceof NotificationTransaction)
                    || (t instanceof RetrieveTransaction)
                    || (t instanceof ReadRecTransaction)
                    || (t instanceof SendTransaction)) {
                try {
                    TransactionState state = t.getState();
                    if (state.getState() == TransactionState.FAILED) {
                        Uri uri = state.getContentUri();
                        if (uri != null) {
                            scheduleRetry(uri);
                        }
                    }
                } finally {
                    t.detach(this);
                }
            }
        } finally {
            /// M:Code analyze 001, remove if,no matter if network is available or not,
            /// set a retry alarm to process the scanning pending message operation @{
            setRetryAlarm(mContext);
            /// @}
        }
    }

    private void scheduleRetry(Uri uri) {
        long msgId = ContentUris.parseId(uri);

        Uri.Builder uriBuilder = PendingMessages.CONTENT_URI.buildUpon();
        uriBuilder.appendQueryParameter("protocol", "mms");
        uriBuilder.appendQueryParameter("message", String.valueOf(msgId));

        Cursor cursor = SqliteWrapper.query(mContext, mContentResolver,
                uriBuilder.build(), null, null, null, null);

        if (cursor != null) {
            try {
                if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                    int msgType = cursor.getInt(cursor.getColumnIndexOrThrow(
                            PendingMessages.MSG_TYPE));

                    int retryIndex = cursor.getInt(cursor.getColumnIndexOrThrow(
                            PendingMessages.RETRY_INDEX)) + 1; // Count this time.

                    // TODO Should exactly understand what was happened.
                    int errorType = MmsSms.ERR_TYPE_GENERIC;

                    DefaultRetryScheme scheme = new DefaultRetryScheme(mContext, retryIndex);
                    mOpRetrySchedulerExt.scheduleRetry(scheme, msgType);

                    ContentValues values = new ContentValues(4);
                    /// M:Code analyze 002,using elapsedRealtime instead of currentTimeMillis,
                    /// because elapsedRealtime include interval time,such as sleep time @{
                    long current = SystemClock.elapsedRealtime();
                    /// @}
                    boolean isRetryDownloading =
                            (msgType == PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND);
                    boolean retry = true;
                    int respStatus = getResponseStatus(msgId);
                    /// M: check the response status value in sending mms case
                    /// M:Code analyze 003,a new method instead of a few lines of code below @{
                    retry = checkSendMmsResponseStatus(isRetryDownloading, uri, respStatus);
                    /// @}

                    if ((retryIndex < scheme.getRetryLimit()) && retry) {
                        long retryAt = current + scheme.getWaitingInterval();

                        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                            Log.v(TAG, "scheduleRetry: retry for " + uri + " is scheduled at "
                                    + (retryAt - System.currentTimeMillis()) + "ms from now");
                        }
                        /// M:
                        MmsLog.d(MmsApp.TXN_TAG,
                                uri + " is scheduled at " + scheme.getWaitingInterval()
                                        + "retryIndex" + retryIndex);

                        values.put(PendingMessages.DUE_TIME, retryAt);

                        if (isRetryDownloading) {
                            // Downloading process is transiently failed.
                            DownloadManager.getInstance().markState(
                                    uri, DownloadManager.STATE_TRANSIENT_FAILURE);
                        }

                        /// M:Code analyze 004, notify transaction transiently failed
                        /// do not notify user for the first failure @{
                        /// M: Operator may require different time to show first popup
                        int notifyIndex = MmsConfig.getMmsRetryPromptIndex();

                        if (retryIndex > notifyIndex
                                && !mOpRetrySchedulerExt.noNeedSendMsgToToastHandler()) {
                            MmsLog.d(MmsApp.TXN_TAG, "to show MSG_SHOW_TRANSIENTLY_FAILED_NOTIFICATION at " + retryIndex);
                            MmsApp.getToastHandler().sendEmptyMessage(MmsApp.MSG_SHOW_TRANSIENTLY_FAILED_NOTIFICATION);
                        }
                        /// @}
                        /// @}
                    } else {
                        errorType = MmsSms.ERR_TYPE_GENERIC_PERMANENT;
                        if (isRetryDownloading) {
                            Cursor c = SqliteWrapper.query(mContext, mContext.getContentResolver(), uri,
                                    new String[] { Mms.THREAD_ID }, null, null, null);

                            long threadId = -1;
                            if (c != null) {
                                try {
                                    if (c.moveToFirst()) {
                                        threadId = c.getLong(0);
                                    }
                                } finally {
                                    c.close();
                                }
                            }

                            if (threadId != -1) {
                                // Downloading process is permanently failed.
                                MessagingNotification.notifyDownloadFailed(mContext, threadId);
                            }

                            DownloadManager.getInstance().markState(
                                    uri, DownloadManager.STATE_PERMANENT_FAILURE);
                        } else {
                            // Mark the failed message as unread.
                            /// M:Code analyze 005, add a new item,mms status @{
                            ContentValues stateValues = new ContentValues(2);
                            stateValues.put(Mms.READ, 0);
                            stateValues.put(Mms.STATUS, PduHeaders.STATUS_UNREACHABLE);
                            SqliteWrapper.update(mContext, mContext.getContentResolver(),
                                    uri, stateValues, null, null);
                            /// @}
                            MessagingNotification.notifySendFailed(mContext, true);
                        }
                    }

                    values.put(PendingMessages.ERROR_TYPE,  errorType);
                    values.put(PendingMessages.RETRY_INDEX, retryIndex);
                    values.put(PendingMessages.LAST_TRY,    current);

                    int columnIndex = cursor.getColumnIndexOrThrow(
                            PendingMessages._ID);
                    long id = cursor.getLong(columnIndex);
                    SqliteWrapper.update(mContext, mContentResolver,
                            PendingMessages.CONTENT_URI,
                            values, PendingMessages._ID + "=" + id, null);
                } else if (LOCAL_LOGV) {
                    Log.v(TAG, "Cannot found correct pending status for: " + msgId);
                }
            } finally {
                cursor.close();
            }
        }
    }

    private int getResponseStatus(long msgID) {
        int respStatus = 0;
        Cursor cursor = SqliteWrapper.query(mContext, mContentResolver,
                Mms.Outbox.CONTENT_URI, null, Mms._ID + "=" + msgID, null, null);
        try {
            if (cursor.moveToFirst()) {
                respStatus = cursor.getInt(cursor.getColumnIndexOrThrow(Mms.RESPONSE_STATUS));
            }
        } finally {
            cursor.close();
        }
        if (respStatus != 0) {
            Log.e(TAG, "Response status is: " + respStatus);
        }
        return respStatus;
    }

    // apply R880 IOT issue (Conformance 11.6 Retrieve Invalid Message)
    private int getRetrieveStatus(long msgID) {
        int retrieveStatus = 0;
        Cursor cursor = SqliteWrapper.query(mContext, mContentResolver,
                Mms.Inbox.CONTENT_URI, null, Mms._ID + "=" + msgID, null, null);
        try {
            if (cursor.moveToFirst()) {
                retrieveStatus = cursor.getInt(cursor.getColumnIndexOrThrow(
                            Mms.RESPONSE_STATUS));
            }
        } finally {
            cursor.close();
        }
        if (retrieveStatus != 0) {
            if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                Log.v(TAG, "Retrieve status is: " + retrieveStatus);
            }
        }
        return retrieveStatus;
    }

    public static void setRetryAlarm(Context context) {
        Cursor cursor = PduPersister.getPduPersister(context).getPendingMessages(
                Long.MAX_VALUE);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    // The result of getPendingMessages() is order by due time.
                    long retryAt = cursor.getLong(cursor.getColumnIndexOrThrow(
                            PendingMessages.DUE_TIME));
                    /// M:Code analyze 006, skip index 0,means if retryAt is 0,
                    /// should not set a alarm,because 0 interval time don't make sense @{
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(
                            PendingMessages.MSG_ID));
                    if (retryAt == 0) {
                        MmsLog.d(MmsApp.TXN_TAG, "pendingid=" + id + "; ra=" + retryAt);
                        while (cursor.moveToNext()) {
                            retryAt = cursor.getLong(cursor.getColumnIndexOrThrow(
                                    PendingMessages.DUE_TIME));
                            id = cursor.getLong(cursor.getColumnIndexOrThrow(
                                    PendingMessages.MSG_ID));
                            if (retryAt == 0) {
                                continue;
                            }
                            break;
                        }
                    }

                    if (retryAt == 0) {
                        return;
                    }
                    /// @}

                    Intent service = new Intent(TransactionService.ACTION_ONALARM,
                                        null, context, TransactionService.class);
                    PendingIntent operation = PendingIntent.getService(
                            context, 0, service, PendingIntent.FLAG_ONE_SHOT);
                    AlarmManager am = (AlarmManager) context.getSystemService(
                            Context.ALARM_SERVICE);
                    /// M:Code analyze 007,using ELAPSED_REALTIME instead of RTC,
                    /// ELAPSED_REALTIME means really elapsed time,including sleep time,
                    /// it can not be waked up by changing time manually@{
                    am.set(AlarmManager.ELAPSED_REALTIME, retryAt, operation);
                    /// @}

                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "Next retry is scheduled at"
                                + (retryAt - System.currentTimeMillis()) + "ms from now");
                    }
                    /// M:
                    MmsLog.d(MmsApp.TXN_TAG, "Next is scheduled at" + (retryAt - SystemClock.elapsedRealtime())
                            + "pendingid=" + id + "; ra=" + retryAt);
                }
            } finally {
                cursor.close();
            }
        }
    }

    ///M:Code analyze 003,a new method @{
    /**
     * check the response status returned by server in sending mms case.
     * the status value may indicate temporary fail or permanent fail.
     * return true if it's a temporary fail and retry can be triggered.
     * return false if it's a permanent fail.
     */
    private boolean checkSendMmsResponseStatus(boolean isRetryDownloading, Uri uri, int respStatus) {
        boolean retry = false;
        MmsLog.d(MmsApp.TXN_TAG, "isRetryDownloading:" + isRetryDownloading + "; response status is " + respStatus);
        //current transaction is not a SendTransaction.
        if (respStatus < PduHeaders.RESPONSE_STATUS_OK) {
            return true;
        }
        if (respStatus >= PduHeaders.RESPONSE_STATUS_ERROR_PERMANENT_FAILURE) {
            retry = false;
        } else if (respStatus >= PduHeaders.RESPONSE_STATUS_ERROR_TRANSIENT_FAILURE) {
            retry = true;
        }
        if (!isRetryDownloading) {
        switch(respStatus) {
            //ok status, normally never get here.
            case PduHeaders.RESPONSE_STATUS_OK:
                return true;
            //the status that should retry.
            //none.
            // the status that should permanent fail.
            case PduHeaders.RESPONSE_STATUS_ERROR_SENDING_ADDRESS_UNRESOLVED:
            case PduHeaders.RESPONSE_STATUS_ERROR_TRANSIENT_SENDNG_ADDRESS_UNRESOLVED:
                //show toast for this case.
                DownloadManager.getInstance().showErrorCodeToast(R.string.invalid_destination);
                return false;
            case PduHeaders.RESPONSE_STATUS_ERROR_SERVICE_DENIED:
            case PduHeaders.RESPONSE_STATUS_ERROR_PERMANENT_SERVICE_DENIED:
                DownloadManager.getInstance().showErrorCodeToast(R.string.service_not_activated);
                return false;
            case PduHeaders.RESPONSE_STATUS_ERROR_NETWORK_PROBLEM:
                DownloadManager.getInstance().showErrorCodeToast(R.string.service_network_problem);
                return false;
            case PduHeaders.RESPONSE_STATUS_ERROR_TRANSIENT_MESSAGE_NOT_FOUND:
            case PduHeaders.RESPONSE_STATUS_ERROR_PERMANENT_MESSAGE_NOT_FOUND:
                DownloadManager.getInstance().showErrorCodeToast(R.string.service_message_not_found);
                return false;
            case PduHeaders.RESPONSE_STATUS_ERROR_UNSPECIFIED:
            //case PduHeaders.RESPONSE_STATUS_ERROR_SERVICE_DENIED:
            case PduHeaders.RESPONSE_STATUS_ERROR_MESSAGE_FORMAT_CORRUPT:
            case PduHeaders.RESPONSE_STATUS_ERROR_MESSAGE_NOT_FOUND:
            //case PduHeaders.RESPONSE_STATUS_ERROR_NETWORK_PROBLEM:
            case PduHeaders.RESPONSE_STATUS_ERROR_CONTENT_NOT_ACCEPTED:
            case PduHeaders.RESPONSE_STATUS_ERROR_UNSUPPORTED_MESSAGE:
                return false;
            default:
                break;
        }
        } else {
            // apply R880 IOT issue (Conformance 11.6 Retrieve Invalid Message)
            // Notification Transaction case
            respStatus = getRetrieveStatus(ContentUris.parseId(uri));
            if (respStatus ==
                    PduHeaders.RESPONSE_STATUS_ERROR_PERMANENT_MESSAGE_NOT_FOUND) {
                DownloadManager.getInstance().showErrorCodeToast(R.string.service_message_not_found);
                SqliteWrapper.delete(mContext, mContext.getContentResolver(), uri,
                    null, null);
                retry = false;
            }
        }
        return retry;
    }
    /// @}
}
