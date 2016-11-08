package com.mediatek.mms.op09;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SqliteWrapper;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Intents;
import android.telephony.SmsMessage;

import com.android.internal.telephony.IccUtils;
import com.mediatek.common.MPlugin;
import com.mediatek.mms.ext.ISmsReceiverExt;
import com.mediatek.mms.op09.Op09MmsPluginTestRunner.BasicCase; 

public class SmsReceiverTest extends BasicCase {
    private static final byte[] DATA_PART_1 = IccUtils.hexStringToBytes(
            "000000000000100200000000010000000b3135333133373036333732000000000000000000009d0003100078018e150028001cc810" +
            "098b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c" +
            "58b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c5" +
            "8b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c5880306130117115115");
    private static final byte[] DATA_PART_2 = IccUtils.hexStringToBytes(
            "000000000000100200000000010000000b3135333133373036333732000000000000000000005c0003100088014d12b028001cc810" +
            "118b162c58b162c993264c993264c993264c993264c993264c993264c993264c993264c993264c993264c993264c993264c993264c" +
            "993264c993264c993264c993264c9932640306130117115245");
    private static ISmsReceiverExt sSmsReceiver;
    private SmsMessage[] mMessages = new SmsMessage[2];
    private Intent mIntent = new Intent(Intents.SMS_RECEIVED_ACTION);
    ContentValues mContentValues;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Clear the sms table.
        SqliteWrapper.delete(mContext, mContext.getContentResolver(), Sms.CONTENT_URI, null, null);

        sSmsReceiver = MPlugin.createInstance("com.mediatek.mms.ext.ISmsReceiver", mContext);

        mIntent.putExtra("format", android.telephony.SmsMessage.FORMAT_3GPP2);
        mIntent.putExtra("rTime", System.currentTimeMillis());
        mIntent.putExtra("simId", 0);

        mContentValues = new ContentValues();
        mContentValues.put(Sms.ERROR_CODE, 0);
    }

    public void test001MissedSegments() {
        byte[][] pdus = new byte[1][];
        pdus[0] = DATA_PART_1;
        mIntent.putExtra("pdus", pdus);

        mMessages = Intents.getMessagesFromIntent(mIntent);
        sSmsReceiver.extractSmsBody(mMessages, mMessages[0], mContentValues);
    }

    public void test002TotalSegments() {
        byte[][] pdus = new byte[2][];
        pdus[0] = DATA_PART_1;
        pdus[1] = DATA_PART_2;
        mIntent.putExtra("pdus", pdus);

        mMessages = Intents.getMessagesFromIntent(mIntent);
        sSmsReceiver.extractSmsBody(mMessages, mMessages[0], mContentValues);
    }
}
