package com.hesine.nmsg.business.dao;

import java.util.HashSet;
import java.util.Set;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.hesine.nmsg.Application;
import com.hesine.nmsg.common.CommonUtils;

/**
 * The Class NmsSMSMMSManager, all of sms/mms process in this class, retrieve
 * sms/mms data or update/insert/delte etc.
 */
public class NmsSMSMMSManager {

    public static void updateSmsUnreadStatus(final Long threadId) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                ContentValues values = new ContentValues();
                values.put(NmsSMSMMS.READ, 1);
                NmsMtkBinderApi.getInstance().update(NmsSMSMMS.SMS_CONTENT_URI, values,
                        "thread_id = ?", new String[] { String.valueOf(threadId) });
                DBUtils.updateMessageStatusViaThread(threadId);
            }
        }).start();
    }

    /**
     * Gets the sms thread id in threads table via sms id in sms table.
     * 
     * @param sysMsgId
     *            the sms id
     * @return the thread id
     */
    public long getThreadViaSysMsgId(long sysMsgId) {

        final String[] projection = new String[] { NmsSMSMMS.ID, NmsSMSMMS.THREAD_ID };

        final String selection = NmsSMSMMS.ID + " = " + sysMsgId;
        long threadId = 0;
        Cursor cursor = null;
        cursor = NmsMtkBinderApi.getInstance().query(NmsSMSMMS.SMS_CONTENT_URI, projection,
                selection, null, null);
        if ((cursor != null) && (cursor.moveToFirst())) {
            threadId = cursor.getLong(cursor.getColumnIndex(NmsSMSMMS.THREAD_ID));
        }
        return threadId;
    }

    public static String saveSmsToDb(String msg, String fromOrTo, String threadNumber, long date,
            long dateSent) {
        long lThreadId = -1;
        Set<String> setThreadAddr = new HashSet<String>();

        if (!NmsSendMessage.getInstance().isAddressLegal(threadNumber, setThreadAddr)) {
            return null;
        }

        lThreadId = NmsCreateSmsThread.getOrCreateThreadId(Application.getInstance()
                .getApplicationContext(), setThreadAddr);

        ContentValues values = new ContentValues();
        values.put(NmsSMSMMS.THREAD_ID, lThreadId);
        values.put(NmsSMSMMS.ADDRESS, fromOrTo);
        values.put(NmsSMSMMS.TYPE, NmsSMSMMS.SMS_TYPE_INBOX);
        values.put(NmsSMSMMS.BODY, msg);
        values.put(NmsSMSMMS.DATE, date);
        values.put(NmsSMSMMS.DATE_SENT, dateSent);
        values.put(NmsSMSMMS.LOCKED, 0);
        values.put(NmsSMSMMS.SEEN, 1);
        if (CommonUtils.currentActivityIsNmsg(fromOrTo)) {
            values.put(NmsSMSMMS.READ, 1);
        } else {
            values.put(NmsSMSMMS.READ, 0);
        }

        Uri uRet = NmsMtkBinderApi.getInstance().insert(NmsSMSMMS.SMS_CONTENT_URI, values);
        if (uRet != null) {
            return lThreadId + "," + ContentUris.parseId(uRet);
        } else {
            return null;
        }
    }

}
