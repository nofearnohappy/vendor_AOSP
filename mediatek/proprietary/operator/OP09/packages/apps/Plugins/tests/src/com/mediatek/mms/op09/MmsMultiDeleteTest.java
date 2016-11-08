package com.mediatek.mms.op09;

import android.content.ContentValues;
import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.provider.Telephony.Sms;

import com.mediatek.common.MPlugin;
import com.mediatek.mms.ext.IMmsMultiDeleteExt;
import com.mediatek.mms.op09.Op09MmsPluginTestRunner.BasicCase; 

public class MmsMultiDeleteTest extends BasicCase {
    private static IMmsMultiDeleteExt sMmsMultiDelete;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sMmsMultiDelete = MPlugin.createInstance("com.mediatek.mms.ext.IMmsMultiDelete", mContext);
    }

    public void test001LockMassTextMsgs() {
        // Clear the sms table.
        SqliteWrapper.delete(mContext, mContext.getContentResolver(), Sms.CONTENT_URI, null, null);

        // Insert a record in database.
        ContentValues values = new ContentValues();
        values.put(Sms.TYPE, Sms.MESSAGE_TYPE_SENT);
        values.put(Sms.BODY, SMS_CONTENT);
        values.put(Sms.IPMSG_ID, -1);
        Uri smsUri = SqliteWrapper.insert(mContext, mContext.getContentResolver(), Sms.CONTENT_URI, values);

        assertFalse(sMmsMultiDelete.lockMassTextMsgs(mContext, null, true));

        long[] messageId = new long[1];
        messageId[0] = Long.parseLong(smsUri.getLastPathSegment());
        assertTrue(sMmsMultiDelete.lockMassTextMsgs(mContext, messageId, true));
    }
}
