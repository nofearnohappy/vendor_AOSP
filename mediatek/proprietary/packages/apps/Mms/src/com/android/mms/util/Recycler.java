/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.mms.util;

import com.android.mms.MmsConfig;
import com.android.mms.ui.MessageUtils;
import android.database.sqlite.SqliteWrapper;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.Telephony;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Conversations;
import android.util.Log;

/// M:
import android.provider.Telephony.Threads;
import android.provider.Telephony.WapPush;
import com.google.android.mms.pdu.PduHeaders;
import com.mediatek.setting.GeneralPreferenceActivity;
/**
 * The recycler is responsible for deleting old messages.
 */
public abstract class Recycler {
    private static final boolean LOCAL_DEBUG = false;
    private static final String TAG = "Recycler";

    // Default preference values
    private static final boolean DEFAULT_AUTO_DELETE  = false;

    private static SmsRecycler sSmsRecycler;
    private static MmsRecycler sMmsRecycler;

    /// M:
    private static boolean sAutoDeleteRun = false;

    //Recycler for wap push message.
    private static WapPushRecycler sWapPushRecycler;

    public static SmsRecycler getSmsRecycler() {
        if (sSmsRecycler == null) {
            sSmsRecycler = new SmsRecycler();
        }
        return sSmsRecycler;
    }

    public static MmsRecycler getMmsRecycler() {
        if (sMmsRecycler == null) {
            sMmsRecycler = new MmsRecycler();
        }
        return sMmsRecycler;
    }

    public static boolean checkForThreadsOverLimit(Context context) {
        Recycler smsRecycler = getSmsRecycler();
        Recycler mmsRecycler = getMmsRecycler();
        /// M:
        Recycler wappushRecycler = getWapPushRecycler();
        /// M: add wappush
        return smsRecycler.anyThreadOverLimit(context) ||
               mmsRecycler.anyThreadOverLimit(context) ||
               wappushRecycler.anyThreadOverLimit(context);
    }

    public void deleteOldMessages(Context context) {
        if (LOCAL_DEBUG) {
            Log.v(TAG, "Recycler.deleteOldMessages this: " + this);
        }
        if (!isAutoDeleteEnabled(context)) {
            return;
        }
        /// M: @{
        Log.d(TAG, "Recycler.deleteOldMessages this: ");
        //don't enter this when it already running
        if (!sAutoDeleteRun) {
            Log.d(TAG, "Recycler.deleteOldMessages this 1");
            /// @}
            Cursor cursor = getAllThreads(context);
            try {
                int limit = getMessageLimit(context);
                MmsLog.d(TAG, "limit is:" + limit);
                while (cursor.moveToNext()) {
                    long threadId = getThreadId(cursor);
                    /// M: @{
                    sAutoDeleteRun = true;
                    /// @}
                    deleteMessagesForThread(context, threadId, limit);
                }
            } finally {
                /// M: @{
                sAutoDeleteRun = false;
                /// @}
                cursor.close();
            }
        }

    }

    public void deleteOldMessagesByThreadId(Context context, long threadId) {
        if (LOCAL_DEBUG) {
            Log.v(TAG, "Recycler.deleteOldMessagesByThreadId this: " + this +
                    " threadId: " + threadId);
        }
        if (!isAutoDeleteEnabled(context)) {
            return;
        }

        deleteMessagesForThread(context, threadId, getMessageLimit(context));
    }

    public static boolean isAutoDeleteEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(GeneralPreferenceActivity.AUTO_DELETE,
                DEFAULT_AUTO_DELETE);
    }

    abstract public int getMessageLimit(Context context);

    abstract public void setMessageLimit(Context context, int limit);

    public int getMessageMinLimit() {
        return MmsConfig.getMinMessageCountPerThread();
    }

    public int getMessageMaxLimit() {
        return MmsConfig.getMaxMessageCountPerThread();
    }

    abstract protected long getThreadId(Cursor cursor);

    abstract protected Cursor getAllThreads(Context context);

    abstract protected void deleteMessagesForThread(Context context, long threadId, int keep);

    abstract protected void dumpMessage(Cursor cursor, Context context);

    abstract protected boolean anyThreadOverLimit(Context context);

    public static class SmsRecycler extends Recycler {
        private static final String[] ALL_SMS_THREADS_PROJECTION = {
            Telephony.Sms.Conversations.THREAD_ID,
            Telephony.Sms.Conversations.MESSAGE_COUNT
        };

        private static final int ID             = 0;
        private static final int MESSAGE_COUNT  = 1;

        static private final String[] SMS_MESSAGE_PROJECTION = new String[] {
            BaseColumns._ID,
            Conversations.THREAD_ID,
            Sms.ADDRESS,
            Sms.BODY,
            Sms.DATE,
            Sms.READ,
            Sms.TYPE,
            Sms.STATUS,
            Telephony.Sms.IPMSG_ID
        };

        // The indexes of the default columns which must be consistent
        // with above PROJECTION.
        static private final int COLUMN_ID                  = 0;
        static private final int COLUMN_THREAD_ID           = 1;
        static private final int COLUMN_SMS_ADDRESS         = 2;
        static private final int COLUMN_SMS_BODY            = 3;
        static private final int COLUMN_SMS_DATE            = 4;
        static private final int COLUMN_SMS_READ            = 5;
        static private final int COLUMN_SMS_TYPE            = 6;
        static private final int COLUMN_SMS_STATUS          = 7;

        private final String MAX_SMS_MESSAGES_PER_THREAD = "MaxSmsMessagesPerThread";

        public int getMessageLimit(Context context) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            return prefs.getInt(MAX_SMS_MESSAGES_PER_THREAD,
                    MmsConfig.getDefaultSMSMessagesPerThread());
        }

        public void setMessageLimit(Context context, int limit) {
            SharedPreferences.Editor editPrefs =
                PreferenceManager.getDefaultSharedPreferences(context).edit();
            editPrefs.putInt(MAX_SMS_MESSAGES_PER_THREAD, limit);
            editPrefs.apply();
        }

        protected long getThreadId(Cursor cursor) {
            return cursor.getLong(ID);
        }

        protected Cursor getAllThreads(Context context) {
            ContentResolver resolver = context.getContentResolver();
            /// M: change google default
            Cursor cursor = SqliteWrapper.query(context, resolver,
                    Uri.parse("content://sms/all_threadid"),
                    null, null, null, Conversations.DEFAULT_SORT_ORDER);

            return cursor;
        }

        protected void deleteMessagesForThread(Context context, long threadId, int keep) {
            if (LOCAL_DEBUG) {
                Log.v(TAG, "SMS: deleteMessagesForThread");
            }
            ContentResolver resolver = context.getContentResolver();
            Cursor cursor = null;
            /// M: @{
            String notDraft = Sms.TYPE + "<>" + Sms.MESSAGE_TYPE_DRAFT;
            String notIpMessage = Telephony.Sms.IPMSG_ID + "<=0";
            /// @}
            try {
                /// M: change google default.
                cursor = SqliteWrapper.query(context, resolver,
                        ContentUris.withAppendedId(Sms.Conversations.CONTENT_URI, threadId),
                        SMS_MESSAGE_PROJECTION,
                        "locked=0 AND " + notDraft + " AND " + notIpMessage,
                        null, "date ASC");     // get in oldest to newest order
                if (cursor == null || cursor.getCount() == 0) {
                    Log.e(TAG, "SMS: deleteMessagesForThread got back null cursor");
                    return;
                }
                int count = cursor.getCount();
                int numberToDelete = count - keep;
                if (LOCAL_DEBUG) {
                    Log.v(TAG, "SMS: deleteMessagesForThread keep: " + keep +
                            " count: " + count +
                            " numberToDelete: " + numberToDelete);
                }
                if (numberToDelete <= 0) {
                    return;
                }
                /// M: @{
                // Move to the keep limit and then delete everything older than that one.
                cursor.moveToPosition(numberToDelete);
                long latestDate = cursor.getLong(COLUMN_SMS_DATE);

                cursor.close();
                cursor = SqliteWrapper.query(context, resolver,
                        ContentUris.withAppendedId(Sms.Conversations.CONTENT_URI, threadId),
                        SMS_MESSAGE_PROJECTION,
                        "locked=0 AND " + notDraft + " AND " + notIpMessage + " AND date<" + latestDate,
                        null, "date ASC");     // get in oldest to newest order
                //get all the sms id which will be deleted.
                String[] argsSms = new String[cursor.getCount()];
                int i = 0;
                cursor.moveToFirst();
                while (cursor.moveToNext()) {
                    argsSms[i] = Long.toString(cursor.getLong(COLUMN_ID));
                    i++;
                }
                long cntDeleted = SqliteWrapper.delete(context, resolver,
                        ContentUris.withAppendedId(Uri.parse("content://sms/auto_delete"), threadId),
                        "locked=0 AND " + notDraft + " AND date<" + latestDate,
                        argsSms);
                if (LOCAL_DEBUG) {
                    Log.v(TAG, "SMS: deleteMessagesForThread cntDeleted: " + cntDeleted + "date " + latestDate);
                }
                Log.v(TAG, "SMS: deleteMessagesForThread 2 ");
                if (cntDeleted != numberToDelete) {
                    cursor.close();
                    cursor = SqliteWrapper.query(context, resolver,
                            ContentUris.withAppendedId(Sms.Conversations.CONTENT_URI, threadId),
                            SMS_MESSAGE_PROJECTION,
                            "locked=0 AND " + notDraft + " AND " + notIpMessage,
                            null, "_id ASC");     // get in oldest to newest order
                    numberToDelete = numberToDelete - (int) cntDeleted;
                    if (LOCAL_DEBUG) {
                        Log.v(TAG, "SMS: numberToDelete: " + numberToDelete);
                    }
                    if (cursor.getCount() == 0 || numberToDelete <= 0) {
                        return;
                    }
                    long delId = 0;
                    if (cursor.moveToPosition(numberToDelete)) {
                        delId = cursor.getLong(COLUMN_ID);
                    }
                    cntDeleted = SqliteWrapper.delete(context, resolver, ContentUris
                            .withAppendedId(Uri.parse("content://sms/auto_delete"), threadId),
                            "locked=0 AND " + notDraft + " AND _id<" + delId, null);
                    if (LOCAL_DEBUG) {
                        Log.v(TAG, "SMS: deleteMessagesForThread cntDeleted: " + cntDeleted);
                    }
                    /// @}
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        protected void dumpMessage(Cursor cursor, Context context) {
            long date = cursor.getLong(COLUMN_SMS_DATE);
            String dateStr = MessageUtils.formatTimeStampString(context, date, true);
            if (LOCAL_DEBUG) {
                Log.v(TAG, "Recycler message " +
                        "\n    address: " + cursor.getString(COLUMN_SMS_ADDRESS) +
                        "\n    body: " + cursor.getString(COLUMN_SMS_BODY) +
                        "\n    date: " + dateStr +
                        "\n    date: " + date +
                        "\n    read: " + cursor.getInt(COLUMN_SMS_READ));
            }
        }

        @Override
        protected boolean anyThreadOverLimit(Context context) {
            Cursor cursor = getAllThreads(context);
            if (cursor == null) {
                return false;
            }
            int limit = getMessageLimit(context);
            try {
                while (cursor.moveToNext()) {
                    long threadId = getThreadId(cursor);
                    ContentResolver resolver = context.getContentResolver();
                    Cursor msgs = SqliteWrapper.query(context, resolver,
                            ContentUris.withAppendedId(Sms.Conversations.CONTENT_URI, threadId),
                            SMS_MESSAGE_PROJECTION,
                            "locked=0 AND " + Telephony.Sms.IPMSG_ID
                            + "<=0 AND " + Sms.TYPE + "<>" + Sms.MESSAGE_TYPE_DRAFT,
                            null, "date DESC");     // get in newest to oldest order
                    if (msgs == null) {
                        return false;
                    }
                    try {
                        if (msgs.getCount() >= limit) {
                            return true;
                        }
                    } finally {
                        msgs.close();
                    }
                }
            } finally {
                cursor.close();
            }
            return false;
        }
    }

    public static class MmsRecycler extends Recycler {
        private static final String[] ALL_MMS_THREADS_PROJECTION = {
            "thread_id", "count(*) as msg_count"
        };

        private static final int ID             = 0;
        private static final int MESSAGE_COUNT  = 1;

        static private final String[] MMS_MESSAGE_PROJECTION = new String[] {
            BaseColumns._ID,
            Conversations.THREAD_ID,
            Mms.DATE,
        };

        // The indexes of the default columns which must be consistent
        // with above PROJECTION.
        static private final int COLUMN_ID                  = 0;
        static private final int COLUMN_THREAD_ID           = 1;
        static private final int COLUMN_MMS_DATE            = 2;
        static private final int COLUMN_MMS_READ            = 3;

        private final String MAX_MMS_MESSAGES_PER_THREAD = "MaxMmsMessagesPerThread";

        private static final String MMS_CONVERSATION_CONSTRAINT = "(" +
                Mms.MESSAGE_BOX + " != " + Mms.MESSAGE_BOX_DRAFTS + " AND (" +
                Mms.MESSAGE_TYPE + " = " + PduHeaders.MESSAGE_TYPE_SEND_REQ + " OR " +
                Mms.MESSAGE_TYPE + " = " + PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF + " OR " +
                Mms.MESSAGE_TYPE + " = " + PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND + "))";

        public int getMessageLimit(Context context) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            return prefs.getInt(MAX_MMS_MESSAGES_PER_THREAD,
                    MmsConfig.getDefaultMMSMessagesPerThread());
        }

        public void setMessageLimit(Context context, int limit) {
            SharedPreferences.Editor editPrefs =
                PreferenceManager.getDefaultSharedPreferences(context).edit();
            editPrefs.putInt(MAX_MMS_MESSAGES_PER_THREAD, limit);
            editPrefs.apply();
        }

        protected long getThreadId(Cursor cursor) {
            return cursor.getLong(ID);
        }

        protected Cursor getAllThreads(Context context) {
            ContentResolver resolver = context.getContentResolver();
            Cursor cursor = SqliteWrapper.query(context, resolver,
                    Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, "threads"),
                    ALL_MMS_THREADS_PROJECTION, null, null, Conversations.DEFAULT_SORT_ORDER);

            return cursor;
        }

        public void deleteOldMessagesInSameThreadAsMessage(Context context, Uri uri) {
            if (LOCAL_DEBUG) {
                Log.v(TAG, "MMS: deleteOldMessagesByUri");
            }
            if (!isAutoDeleteEnabled(context)) {
                return;
            }
            Cursor cursor = null;
            long latestDate = 0;
            long threadId = 0;
            /// M: @{
            int numberToDelete = 0;
            /// @}
            try {
                String msgId = uri.getLastPathSegment();
                ContentResolver resolver = context.getContentResolver();
                cursor = SqliteWrapper.query(context, resolver,
                        Telephony.Mms.CONTENT_URI,
                        MMS_MESSAGE_PROJECTION,
                        "thread_id in (select thread_id from pdu where _id=" + msgId +
                            ") AND locked=0" + " AND " + MMS_CONVERSATION_CONSTRAINT, ///M: add for alps00454642
                        null, "date DESC");     // get in newest to oldest order
                if (cursor == null) {
                    Log.e(TAG, "MMS: deleteOldMessagesInSameThreadAsMessage got back null cursor");
                    return;
                }

                int count = cursor.getCount();
                int keep = getMessageLimit(context);
                numberToDelete = count - keep;
                if (LOCAL_DEBUG) {
                    Log.v(TAG, "MMS: deleteOldMessagesByUri keep: " + keep +
                            " count: " + count +
                            " numberToDelete: " + numberToDelete);
                }
                if (numberToDelete <= 0) {
                    return;
                }
                // Move to the keep limit and then delete everything older than that one.
                cursor.move(keep);
                latestDate = cursor.getLong(COLUMN_MMS_DATE);
                threadId = cursor.getLong(COLUMN_THREAD_ID);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            if (threadId != 0) {
                /// M: change google default.
                deleteMessagesOlderThanDate(context, threadId, latestDate, numberToDelete);
            }
        }

        protected void deleteMessagesForThread(Context context, long threadId, int keep) {
            if (LOCAL_DEBUG) {
                Log.v(TAG, "MMS: deleteMessagesForThread");
            }
            if (threadId == 0) {
                return;
            }
            Cursor cursor = null;
            long latestDate = 0;
            /// M: @{
            int numberToDelete = 0;
            /// @}
            try {
                ContentResolver resolver = context.getContentResolver();
                cursor = SqliteWrapper.query(context, resolver,
                        Telephony.Mms.CONTENT_URI,
                        MMS_MESSAGE_PROJECTION,
                        "thread_id=" + threadId + " AND locked=0" + " AND " + MMS_CONVERSATION_CONSTRAINT,
                        null, "date ASC");     // get in newest to oldest order
                /// M: change google default.
                if (cursor == null || cursor.getCount() == 0) {
                    Log.e(TAG, "MMS: deleteMessagesForThread got back null cursor");
                    return;
                }

                int count = cursor.getCount();
                numberToDelete = count - keep;
                if (LOCAL_DEBUG) {
                    Log.v(TAG, "MMS: deleteMessagesForThread keep: " + keep +
                            " count: " + count +
                            " numberToDelete: " + numberToDelete);
                }
                if (numberToDelete <= 0) {
                    return;
                }
                // Move to the keep limit and then delete everything older than that one.
                cursor.moveToPosition(numberToDelete);
                latestDate = cursor.getLong(COLUMN_MMS_DATE);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            /// M: change google default.
            deleteMessagesOlderThanDate(context, threadId, latestDate, numberToDelete);
        }

        private void deleteMessagesOlderThanDate(Context context, long threadId,
                long latestDate, int numberToDelete) {
            long cntDeleted = SqliteWrapper.delete(context, context.getContentResolver(),
                    Telephony.Mms.CONTENT_URI,
                    "thread_id=" + threadId + " AND locked=0 AND date<" + latestDate,
                    null);
            /// M: @{
            if (cntDeleted != numberToDelete) {
                Cursor cursor = null;
                try {
                    cursor = SqliteWrapper.query(context, context.getContentResolver(),
                            Telephony.Mms.CONTENT_URI,
                            MMS_MESSAGE_PROJECTION,
                            "thread_id=" + threadId + " AND locked=0",
                            null, "_id ASC");     // get in newest to oldest order
                    numberToDelete = numberToDelete - (int) cntDeleted;
                    if (LOCAL_DEBUG) {
                        Log.v(TAG, "MMS: numberToDelete: " + numberToDelete + "cursor count " + cursor.getCount());
                    }
                    if (cursor.getCount() == 0 || numberToDelete <= 0) {
                        return;
                    }

                    long delId = 0;
                    if (cursor.moveToPosition(numberToDelete)) {
                        delId = cursor.getLong(COLUMN_ID);
                    }
                    cntDeleted = SqliteWrapper.delete(context, context.getContentResolver(),
                            Telephony.Mms.CONTENT_URI,
                            "locked=0 AND _id<" + delId, null);
                    if (LOCAL_DEBUG) {
                        Log.v(TAG, "MMS: deleteMessagesOlderThanDate cntDeleted: " + cntDeleted);
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
            /// @}
        }

        protected void dumpMessage(Cursor cursor, Context context) {
            long id = cursor.getLong(COLUMN_ID);
            if (LOCAL_DEBUG) {
                Log.v(TAG, "Recycler message " +
                        "\n    id: " + id
                );
            }
        }

        @Override
        protected boolean anyThreadOverLimit(Context context) {
            Cursor cursor = getAllThreads(context);
            if (cursor == null) {
                return false;
            }
            int limit = getMessageLimit(context);
            try {
                while (cursor.moveToNext()) {
                    long threadId = getThreadId(cursor);
                    ContentResolver resolver = context.getContentResolver();
                    Cursor msgs = SqliteWrapper.query(context, resolver,
                            Telephony.Mms.CONTENT_URI,
                            MMS_MESSAGE_PROJECTION,
                            "thread_id=" + threadId + " AND locked=0",
                            null, "date DESC");     // get in newest to oldest order

                    if (msgs == null) {
                        return false;
                    }
                    try {
                        if (msgs.getCount() >= limit) {
                            return true;
                        }
                    } finally {
                        msgs.close();
                    }
                }
            } finally {
                cursor.close();
            }
            return false;
        }
    }

    /// M:
    public static class WapPushRecycler extends Recycler {
        private static final String[] ALL_WAPPUSH_THREADS_PROJECTION = {
            Threads._ID,
            Threads.MESSAGE_COUNT
        };

        private static final int ID             = 0;
        private static final int MESSAGE_COUNT  = 1;

        static private final String[] WAPPUSH_MESSAGE_PROJECTION = new String[] {
            WapPush._ID,
            WapPush.THREAD_ID,
            WapPush.ADDR,
            WapPush.URL,
            WapPush.DATE,
            WapPush.READ,
            WapPush.TYPE,
        };

        // The indexes of the default columns which must be consistent
        // with above PROJECTION.
        private static final int COLUMN_ID                  = 0;
        private static final int COLUMN_THREAD_ID           = 1;
        private static final int COLUMN_WAPPUSH_ADDRESS     = 2;
        private static final int COLUMN_WAPPUSH_URL         = 3;
        private static final int COLUMN_WAPPUSH_DATE        = 4;
        private static final int COLUMN_WAPPUSH_READ        = 5;
        private static final int COLUMN_WAPPUSH_TYPE        = 6;

        //private final String mMaxWapPushMessagesPerThread = "MaxWapPushMessagesPerThread";
        /// M: wappush limit use the same settings as sms.
        public int getMessageLimit(Context context) {
            return getSmsRecycler().getMessageLimit(context);
        }

        public void setMessageLimit(Context context, int limit) {
            getSmsRecycler().setMessageLimit(context, limit);
        }

        protected long getThreadId(Cursor cursor) {
            return cursor.getLong(ID);
        }

        protected Cursor getAllThreads(Context context) {
            Uri.Builder uriBuilder = Threads.CONTENT_URI.buildUpon();
            uriBuilder.appendQueryParameter("simple", "true");
            uriBuilder.appendQueryParameter("thread_type", String.valueOf(Telephony.Threads.WAPPUSH_THREAD));

            ContentResolver resolver = context.getContentResolver();
            Cursor cursor = SqliteWrapper.query(context, resolver,
                    uriBuilder.build(),
                    ALL_WAPPUSH_THREADS_PROJECTION,
                    null,
                    null, WapPush.DEFAULT_SORT_ORDER);
            return cursor;
        }

        protected void deleteMessagesForThread(Context context, long threadId, int keep) {
            if (LOCAL_DEBUG) {
                Log.v(TAG, "WAPPUSH: deleteMessagesForThread");
            }
            ContentResolver resolver = context.getContentResolver();
            Cursor cursor = null;
            try {
                cursor = SqliteWrapper.query(context, resolver,
                        ContentUris.withAppendedId(WapPush.CONTENT_URI_THREAD, threadId),
                        WAPPUSH_MESSAGE_PROJECTION,
                        "locked=0",
                        null, "date ASC");     // get in oldest to newest order
                if (cursor == null || cursor.getCount() == 0) {
                    Log.e(TAG, "WAPPUSH: deleteMessagesForThread got back null cursor");
                    return;
                }
                int count = cursor.getCount();
                int numberToDelete = count - keep;
                if (LOCAL_DEBUG) {
                    Log.v(TAG, "WAPPUSH: deleteMessagesForThread keep: " + keep +
                            " count: " + count +
                            " numberToDelete: " + numberToDelete);
                }
                MmsLog.d(TAG, "threadId:" + threadId + ",keep:" + keep + ",delete:" + numberToDelete);
                if (numberToDelete <= 0) {
                    return;
                }
                // Move to the keep limit and then delete everything older than that one.
                cursor.moveToPosition(numberToDelete);
                long latestDate = cursor.getLong(COLUMN_WAPPUSH_DATE);
                long delId = cursor.getLong(COLUMN_ID);

                long cntDeleted = SqliteWrapper.delete(context, resolver,
                        ContentUris.withAppendedId(WapPush.CONTENT_URI_THREAD, threadId),
                        "locked=0 AND date<" + latestDate,
                        null);
                if (LOCAL_DEBUG) {
                    Log.v(TAG, "WAPPUSH: deleteMessagesForThread cntDeleted: " + cntDeleted);
                }

                if (cntDeleted != numberToDelete) {
                    cntDeleted = SqliteWrapper.delete(context, resolver,
                            ContentUris.withAppendedId(WapPush.CONTENT_URI_THREAD, threadId),
                            "locked=0 AND _id<" + delId,
                            null);
                }
                if (LOCAL_DEBUG) {
                    Log.v(TAG, "WAPPUSH: deleteMessagesForThread cntDeleted: " + cntDeleted);
                }


            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        protected void dumpMessage(Cursor cursor, Context context) {
            long date = cursor.getLong(COLUMN_WAPPUSH_DATE);
            String dateStr = MessageUtils.formatTimeStampString(context, date, true);
            if (LOCAL_DEBUG) {
                Log.v(TAG, "Recycler message " +
                        "\n    address: " + cursor.getString(COLUMN_WAPPUSH_ADDRESS) +
                        "\n    url: " + cursor.getString(COLUMN_WAPPUSH_URL) +
                        "\n    date: " + dateStr +
                        "\n    read: " + cursor.getInt(COLUMN_WAPPUSH_READ));
            }
        }

        @Override
        protected boolean anyThreadOverLimit(Context context) {
            Cursor cursor = getAllThreads(context);
            Cursor msgs = null;
            int limit = getMessageLimit(context);
            if (cursor != null) {
                try {
                    long threadId = 0L;
                    while (cursor.moveToNext()) {
                        threadId = getThreadId(cursor);
                        ContentResolver resolver = context.getContentResolver();
                        msgs = SqliteWrapper.query(context, resolver,
                                ContentUris.withAppendedId(WapPush.CONTENT_URI_THREAD, threadId),
                                WAPPUSH_MESSAGE_PROJECTION,
                                "locked=0",
                                null, "date DESC");     // get in newest to oldest order
                        if (msgs != null && msgs.getCount() >= limit) {
                            return true;
                        }
                    }
                } finally {
                    cursor.close();
                    if (msgs != null) {
                        msgs.close();
                    }
                }
            }
            return false;
        }
    }

    public static WapPushRecycler getWapPushRecycler() {
        if (sWapPushRecycler == null) {
            sWapPushRecycler = new WapPushRecycler();
        }
        return sWapPushRecycler;
    }
}
