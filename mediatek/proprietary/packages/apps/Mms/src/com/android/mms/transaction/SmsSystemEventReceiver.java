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

import com.android.mms.util.DraftCache;
import com.android.mms.util.MmsLog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.android.mms.LogTag;
import com.android.mms.MmsApp;
import com.mediatek.mms.util.PermissionCheckUtil;
import android.content.ContentValues;
import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Outbox;
import android.telephony.SmsManager;
/**
 * SmsSystemEventReceiver receives the
 * {@link android.content.intent.ACTION_BOOT_COMPLETED},
 */
public class SmsSystemEventReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsSystemEventReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        MmsLog.d(MmsApp.TXN_TAG, "onReceive(): intent=" + intent.toString());
        String action = intent.getAction();

        /// Add for runtime permission @{
        if (!PermissionCheckUtil.checkRequiredPermissions(context)) {
            MmsLog.d(MmsApp.LOG_TAG, "SmsSystemEventReceiver no permissions return !!");
            return;
        } else {
            MmsLog.d(MmsApp.LOG_TAG, "SmsSystemEventReceiver onReceive() has permisson DraftCache: "
                    + DraftCache.getInstance());
            if (DraftCache.getInstance() == null) {
                MmsApp.getApplication().onRequestPermissionsResult();
            }
        }
        /// @}

        if (action != null && action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            handleBootCompleted(context);
        }
    }

    private void handleBootCompleted(Context context) {
        MmsLog.i(MmsApp.TXN_TAG,"handleBootCompleted sms");
        // fix first launch performance issue ALPS01760483, start a service to avoid mms killed.
        Intent intent = new Intent();
        intent.setClass(context, NoneService.class);
        context.startService(intent);

        // Some messages may get stuck in the outbox or queued. At this point, they're probably irrelevant
        // to the user, so mark them as failed and notify the user, who can then decide whether to
        // resend them manually.
        int numMoved = moveOutboxMessagesToFailedBox(context);
        numMoved = numMoved + moveQueuedMessagesToFailedBox(context);
        if (numMoved > 0) {
            MessagingNotification.notifySendFailed(context.getApplicationContext(), true);
        }
        // Called off of the UI thread so ok to block.
        MessagingNotification.blockingUpdateNewMessageIndicator(
                context, MessagingNotification.THREAD_ALL, false, null);
    }

    /**
     * Move all messages that are in the outbox to the failed state and set them to unread.
     * @return The number of messages that were actually moved
     */
    private int moveOutboxMessagesToFailedBox(Context context) {
        ContentValues values = new ContentValues(3);

        values.put(Sms.TYPE, Sms.MESSAGE_TYPE_FAILED);
        values.put(Sms.ERROR_CODE, SmsManager.RESULT_ERROR_GENERIC_FAILURE);
        values.put(Sms.READ, Integer.valueOf(0));

        int messageCount = SqliteWrapper.update(
                context.getApplicationContext(), context.getContentResolver(), Outbox.CONTENT_URI,
                values, "type = " + Sms.MESSAGE_TYPE_OUTBOX, null);
        MmsLog.v(TAG, "moveOutboxMessagesToFailedBox messageCount: " + messageCount);
        return messageCount;
    }

    /**
     * Move all messages that are in the queued to the failed state and set them to unread.
     * @return The number of messages that were actually moved
     */
    private int moveQueuedMessagesToFailedBox(Context context) {
        ContentValues values = new ContentValues(3);

        values.put(Sms.TYPE, Sms.MESSAGE_TYPE_FAILED);
        values.put(Sms.ERROR_CODE, SmsManager.RESULT_ERROR_GENERIC_FAILURE);
        values.put(Sms.READ, Integer.valueOf(0));

        final Uri uri = Uri.parse("content://sms/queued");
        int messageCount = SqliteWrapper.update(
                context.getApplicationContext(), context.getContentResolver(), uri,
                values, "type = " + Sms.MESSAGE_TYPE_QUEUED , null);

        MmsLog.v(TAG, "moveQueuedMessagesToFailedBox messageCount: " + messageCount);
        return messageCount;
    }
}
