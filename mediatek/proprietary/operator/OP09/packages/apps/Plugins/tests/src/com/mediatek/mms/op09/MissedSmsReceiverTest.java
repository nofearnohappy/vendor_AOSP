package com.mediatek.mms.op09;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SqliteWrapper;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Inbox;
import android.provider.Telephony.Sms.Intents;
import android.telephony.SmsMessage;

import com.android.internal.telephony.IccUtils;
import com.mediatek.common.MPlugin;
import com.mediatek.mms.ext.IMissedSmsReceiverExt;
import com.mediatek.mms.op09.Op09MmsPluginTestRunner.BasicCase;

public class MissedSmsReceiverTest extends BasicCase {
    private static IMissedSmsReceiverExt sMissedSmsReceiver;
    private SmsMessage[] mMessages = new SmsMessage[2];
    private Intent mIntent = new Intent(Intents.SMS_RECEIVED_ACTION);
    private static final byte[] DATA_PART_1 = IccUtils.hexStringToBytes(
            "000000000000100200000000010000000b3135333133373036333732000000000000000000009d0003100078018e150028001cc810" +
            "098b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c" +
            "58b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c5" +
            "8b162c58b162c58b162c58b162c58b162c58b162c58b162c58b162c5880306130117115115");
    private static final byte[] DATA_PART_2 = IccUtils.hexStringToBytes(
            "000000000000100200000000010000000b3135333133373036333732000000000000000000005c0003100088014d12b028001cc810" +
            "118b162c58b162c993264c993264c993264c993264c993264c993264c993264c993264c993264c993264c993264c993264c993264c" +
            "993264c993264c993264c993264c9932640306130117115245");

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sMissedSmsReceiver = MPlugin.createInstance("com.mediatek.mms.ext.IMissedSmsReceiver", mContext);

        mIntent.putExtra("format", android.telephony.SmsMessage.FORMAT_3GPP2);
        mIntent.putExtra("rTime", System.currentTimeMillis());
        mIntent.putExtra("simId", 0);
    }

    public void test001StoreSmsPart1() {
        // Clear the sms table.
        SqliteWrapper.delete(mContext, mContext.getContentResolver(), Sms.CONTENT_URI, null, null);

        // Insert a record in database first.
        ContentValues values = new ContentValues();
        values.put(Sms.REFERENCE_ID, 153);
        values.put(Sms.TOTAL_LENGTH, 2);
        values.put(Sms.RECEIVED_LENGTH, 0);
        values.put(Sms.THREAD_ID, NEW_THREAD_ID);
        values.put(Sms.ADDRESS, TEST_ADDRESS);
        SqliteWrapper.insert(mContext, mContext.getContentResolver(), Inbox.CONTENT_URI, values);

        byte[][] pdus = new byte[1][];
        pdus[0] = DATA_PART_1;
        mIntent.putExtra("pdus", pdus);

        mMessages = Intents.getMessagesFromIntent(mIntent);
        assertNotNull(sMissedSmsReceiver.updateMissedSms(mContext, mMessages, 0));
    }

    public void test002UpdateSmsPart2() {
        byte[][] pdus = new byte[2][];
        pdus[0] = DATA_PART_1;
        pdus[1] = DATA_PART_2;
        mIntent.putExtra("pdus", pdus);

        mMessages = Intents.getMessagesFromIntent(mIntent);
        assertNotNull(sMissedSmsReceiver.updateMissedSms(mContext, mMessages, 0));
    }
}
