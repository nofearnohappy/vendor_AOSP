/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.app.IntentService;
import android.app.Service;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Inbox;
import android.telephony.SmsMessage;
import android.util.Log;

import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.util.MmsLog;

/**
 * Service that gets started by the MessageStatusReceiver when a message status report is
 * received.
 */
public class MessageStatusService extends IntentService {
    /// M:Code analyze 001, add a column
    private static final String[] ID_PROJECTION = new String[] { Sms._ID, Sms.STATUS };
    private static final String LOG_TAG = "MessageStatusReceiver";
    private static final Uri STATUS_URI = Uri.parse("content://sms/status");

    /// M:Code analyze 002, new members @{
    public static final String MESSAGE_STATUS_RECEIVED_ACTION =
            "com.android.mms.transaction.MessageStatusReceiver.MESSAGE_STATUS_RECEIVED";
    public static final String MMS_READ_STATE_CHANGE = "MMS_READ_STATE_CHANGE";
    /// @}

    public MessageStatusService() {
        // Class name will be the thread name.
        super(MessageStatusService.class.getName());

        // Intent should be redelivered if the process gets killed before completing the job.
        setIntentRedelivery(true);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (!MmsConfig.isSmsEnabled(this)) {
            Log.d(LOG_TAG, "MessageStatusService: is not the default sms app");
            return;
        }
        // This method is called on a worker thread.
        /// M:Code analyze 004,add for unknown @{
        if (MESSAGE_STATUS_RECEIVED_ACTION.equals(intent.getAction())) {
            Uri messageUri = intent.getData();
            byte[] pdu = intent.getByteArrayExtra("pdu");
            String format = intent.getStringExtra("format");
            boolean sendNextMsg = intent.getBooleanExtra(
                    SmsReceiverService.EXTRA_MESSAGE_SENT_SEND_NEXT, false);
            SmsMessage message = updateMessageStatus(this, messageUri, pdu, format, sendNextMsg);
            // Called on a background thread, so it's OK to block.
            if (message != null && message.getStatus() <= Sms.STATUS_PENDING) {
                MessagingNotification.blockingUpdateNewMessageIndicator(this,
                        MessagingNotification.THREAD_NONE, message.isStatusReportMessage(), messageUri);
            }
        } else if (MMS_READ_STATE_CHANGE.equals(intent.getAction())) {
            // update notification when read state change
            MessagingNotification.blockingUpdateNewMessageIndicator(this,
                    MessagingNotification.THREAD_NONE, false, null);
        }
        /// @}
    }

    private SmsMessage updateMessageStatus(Context context, Uri messageUri, byte[] pdu,
            String format, boolean sendNextMsg) {
        /** M: @{ */

        /** @} */
        SmsMessage message = SmsMessage.createFromPdu(pdu, format);
        if (message == null) {
            return null;
        }
        /// M:
        if (messageUri == null) {
            return null;
        }
        // Create a "status/#" URL and use it to update the
        // message's status in the database.
        Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(),
                            messageUri, ID_PROJECTION, null, null, null);

        try {
            if (cursor.moveToFirst()) {
                int messageId = cursor.getInt(0);
                /// M:
                int oldStatus = cursor.getInt(1);
                Uri updateUri = ContentUris.withAppendedId(STATUS_URI, messageId);
                int status = message.getStatus();
                boolean isStatusReport = message.isStatusReportMessage();
                ContentValues contentValues = new ContentValues(2);

                /** M: @{ */
                MmsLog.d(MmsApp.TXN_TAG, "updateMessageStatus: msgUrl=" + messageUri
                    + ", status=" + status + ", isStatusReport=" + isStatusReport);

                if (oldStatus == Sms.STATUS_FAILED) {
                    //if the status is failed already, this means this is a long sms, and
                    //at least one part of it is sent failed. so the status report of this long sms is failed overall.
                    //don't record a part's status.
                    // but this part success status is will toasted.
                    MmsLog.d(MmsApp.TXN_TAG, "the original status is:" + oldStatus);
                } else {
                    MmsLog.d(MmsApp.TXN_TAG, "the original status is:" + oldStatus
                            + "the last part:" + sendNextMsg);
                    //update only if this is the last part of a sms.
                    ///M: add for alps00446904,'0' means transaction completed,'1','3','5','7',which are all one of
                    ///transaction completed status,,but they also mean sim message,such as SmsManager.STATUS_ON_ICC_READ == 1,
                    ///therefore,set them all to '0',please refer to spec 23040,chapter 9.2.3.15,TP-status to get more information @{
                    if (status == 1 || status == 3 || status == 5 || status == 7) {
                        status = 0;
                    }
                    /// @
                    contentValues.put(Sms.STATUS, status);
                    contentValues.put(Inbox.DATE_SENT, System.currentTimeMillis());
                    SqliteWrapper.update(context, context.getContentResolver(), updateUri,
                            contentValues, null, null);
                }
                /** @} */
            } else {
                error("Can't find message for status update: " + messageUri);
                message = null;
                /// M:
                MmsLog.w(MmsApp.TXN_TAG, "Can't find message for status update: " + messageUri);

            }
        } finally {
            cursor.close();
        }
        return message;
    }
    /// @}

    private void error(String message) {
        Log.e(LOG_TAG, "[MessageStatusReceiver] " + message);
    }

    private void log(String message) {
        Log.d(LOG_TAG, "[MessageStatusReceiver] " + message);
    }
}
