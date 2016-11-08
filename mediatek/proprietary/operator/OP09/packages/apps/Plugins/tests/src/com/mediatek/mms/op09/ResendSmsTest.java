package com.mediatek.mms.op09;

import android.content.ContentValues;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.os.Handler;
import android.provider.Telephony.Sms;

import com.mediatek.common.MPlugin;
import com.mediatek.mms.ext.IResendSmsExt;
import com.mediatek.mms.op09.Op09MmsPluginTestRunner.BasicCase; 

public class ResendSmsTest extends BasicCase {
    private static IResendSmsExt sResendSms;
    private boolean mSmsSent;
    private static final String TEST_CONTENT = "Test resend message!";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Clear the sms table.
        SqliteWrapper.delete(mContext, mContext.getContentResolver(), Sms.CONTENT_URI, null, null);

        sResendSms = MPlugin.createInstance("com.mediatek.mms.ext.IResendSms" , mContext);
    }

    public void test001ResendMessage() throws Exception {
        // Insert a record in database first.
        ContentValues values = new ContentValues();
        values.put(Sms.THREAD_ID, NEW_THREAD_ID);
        values.put(Sms.ADDRESS, TEST_ADDRESS);
        values.put(Sms.TYPE, Sms.MESSAGE_TYPE_FAILED);
        values.put(Sms.SIM_ID, mSimIdCdma);
        values.put(Sms.BODY, TEST_CONTENT);
        Uri failedSmsUri = SqliteWrapper.insert(mContext, mContext.getContentResolver(), Sms.CONTENT_URI, values);

        // Register the database listener.
        mContext.getContentResolver().registerContentObserver(Sms.CONTENT_URI, true, smsReceivedObserver);

        sResendSms.resendMessage(mContext, failedSmsUri, mSimIdCdma);

        /* assert, wait a moment */
        long timestamp = System.currentTimeMillis();
        while (!mSmsSent) {
            if (System.currentTimeMillis() - timestamp > SEND_DURATION_LIMIT) {
                /* too long time */
                throw new Exception("Too long time for sending");
            }
            delay(10);
        }

        delay(DELAY_TIME * 2);
    }

    private final ContentObserver smsReceivedObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfUpdate) {
            checkIfSent();
        }
    };

    private void checkIfSent() {
        String where = Sms.TYPE + " = ? and " + Sms.BODY + " = ?";
        Cursor cursor = mContext.getContentResolver().query(
                Sms.CONTENT_URI, null, where,
                new String[] {String.valueOf(Sms.MESSAGE_TYPE_INBOX), TEST_CONTENT},
                null);
        if (cursor != null) {
            try {
                if (cursor.getCount() == 1) {
                mSmsSent = true;
                }
            } finally {
                cursor.close();
            }
        }
    }
}
