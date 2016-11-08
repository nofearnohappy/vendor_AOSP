package com.mediatek.mms.op09;

import android.content.ContentValues;
import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.widget.TextView;

import com.google.android.mms.pdu.PduHeaders;
import com.mediatek.common.MPlugin;
import com.mediatek.mms.ext.IMmsMessageListItemExt;
import com.mediatek.mms.ext.IMmsMessageListItemHost;
import com.mediatek.mms.op09.Op09MmsPluginTestRunner.BasicCase; 

public class MmsMessageListItemTest extends BasicCase implements IMmsMessageListItemHost {
    private static final String CONTENT_TYPE = "application/vnd.wap.multipart.related";
    private static final long DATE_SENT = 1371109185;
    private static IMmsMessageListItemExt sMessageListItem;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sMessageListItem = MPlugin.createInstance("com.mediatek.mms.ext.IMmsMessageListItem", mContext);
        sMessageListItem.init(this);
    }

    // This case need to set time format of the phone to be 24 hour type.
    public void test001GetSentDateStr() {
        // Clear the pdu table.
        SqliteWrapper.delete(mContext, mContext.getContentResolver(), Mms.CONTENT_URI, null, null);

        // Insert a record in database.
        ContentValues values = new ContentValues();
        values.put(Mms.THREAD_ID, NEW_THREAD_ID);
        values.put(Mms.DATE_SENT, DATE_SENT);
        values.put(Mms.MESSAGE_TYPE, PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF);
        values.put(Mms.MESSAGE_BOX, Mms.MESSAGE_BOX_INBOX);
        Uri mmsUri = SqliteWrapper.insert(mContext, mContext.getContentResolver(), Mms.CONTENT_URI, values);

        String expectStr = "15:39";
        String actualStr = sMessageListItem.getSentDateStr(mContext, expectStr, 1, PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND,
                DATE_SENT, Mms.MESSAGE_BOX_INBOX);
        assertEquals(expectStr, actualStr);

        actualStr = sMessageListItem.getSentDateStr(mContext, expectStr, 1, PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF,
                DATE_SENT * 1000, Mms.MESSAGE_BOX_INBOX);
        assertEquals(expectStr, actualStr);

        long messageId = Long.parseLong(mmsUri.getLastPathSegment());
        actualStr = sMessageListItem.getSentDateStr(mContext, expectStr, messageId,
                PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF, -1, Mms.MESSAGE_BOX_INBOX);
        assertEquals(expectStr, actualStr);
    }

    public void test002DrawMassTextMsgStatus() {
        // Clear the sms table.
        SqliteWrapper.delete(mContext, mContext.getContentResolver(), Sms.CONTENT_URI, null, null);

        // Insert a record in database.
        ContentValues values = new ContentValues();
        values.put(Sms.DATE, DATE_SENT);
        values.put(Sms.TYPE, Sms.MESSAGE_TYPE_QUEUED);
        values.put(Sms.BODY, SMS_CONTENT);
        values.put(Sms.IPMSG_ID, -1);
        SqliteWrapper.insert(mContext, mContext.getContentResolver(), Sms.CONTENT_URI, values);

        sMessageListItem.drawMassTextMsgStatus(mContext, true, -1);

        // Insert more records.
        values.put(Sms.TYPE, Sms.MESSAGE_TYPE_SENT);
        SqliteWrapper.insert(mContext, mContext.getContentResolver(), Sms.CONTENT_URI, values);
        values.put(Sms.TYPE, Sms.STATUS_FAILED);
        SqliteWrapper.insert(mContext, mContext.getContentResolver(), Sms.CONTENT_URI, values);

        sMessageListItem.drawMassTextMsgStatus(mContext, true, -1);
    }

    public void test003NeedEditFailedMessge() {
        assertTrue(sMessageListItem.needEditFailedMessge(mContext, 1, 0));
        assertFalse(sMessageListItem.needEditFailedMessge(mContext, 1, -1));
    }

    public void test004ShowSimType() {
        if (checkSims()) {
            TextView textView = new TextView(mContext);
            textView.setText("China Telecom");
            sMessageListItem.showSimType(mContext, mSimIdCdma, textView);
            sMessageListItem.showSimType(mContext, mSimIdGsm, textView);
        }
    }

    public void test005SetNotifyContent() {
        TextView textView = new TextView(mContext);
        sMessageListItem.setNotifyContent(TEST_ADDRESS, "Test Suject", "100kb", "2013-12-31", textView);
    }

    // Methods below are for implementing IMmsMessageListItemHost.
    public void showDownloadButton() {
        return;
    }

    public void hideDownloadButton() {
        return;
    }

    public void hideBothButton() {
        return;
    }

    public void drawMassTextMsgStatus(int sendingMsgCount, int sendMsgSuccessCount,
            int sendMsgFailedCount, String smsDate) {
        return;
    }
}
