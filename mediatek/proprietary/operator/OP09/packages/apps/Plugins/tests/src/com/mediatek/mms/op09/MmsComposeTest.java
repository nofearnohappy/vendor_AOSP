package com.mediatek.mms.op09;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.Intent;
import android.database.sqlite.SqliteWrapper;
import android.preference.PreferenceManager;
import android.provider.Telephony.Sms;
import android.widget.LinearLayout;

import com.mediatek.common.MPlugin;
import com.mediatek.mms.op09.Op09MmsPluginTestRunner.BasicCase;
import com.mediatek.mms.ext.IMmsComposeExt;

public class MmsComposeTest extends BasicCase {
    private static final String CMCC = "10086";
    private static final String TEST_CONTACTS = "15210343721:153137,06371:10086;6103";
    private static final String EXPECT_CONTACTS = "15210343721;153137;10086";
    private static IMmsComposeExt sMmsCompose;
    ContentValues mContentValues;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sMmsCompose = MPlugin.createInstance("com.mediatek.mms.ext.IMmsCompose", mContext);

        mContentValues = new ContentValues();
        mContentValues.put(Sms.IPMSG_ID, -1);
        mContentValues.put(Sms.THREAD_ID, NEW_THREAD_ID);
        mContentValues.put(Sms.ADDRESS, TEST_ADDRESS);
        mContentValues.put(Sms.TYPE, Sms.MESSAGE_TYPE_SENT);
        mContentValues.put(Sms.SIM_ID, mSimIdCdma);
        mContentValues.put(Sms.BODY, SMS_CONTENT);
    }

    public void test001GetNumberLocation() {
        assertEquals(CMCC, sMmsCompose.getNumberLocation(mContext, CMCC));
        assertTrue(sMmsCompose.getNumberLocation(mContext, TEST_ADDRESS).endsWith(")"));
    }

    public void test002GetConverationUri() {
        String expString = "content://mms-sms/conversations_distinct/0";
        assertEquals(expString, sMmsCompose.getConverationUri(null, NEW_THREAD_ID).toString());
    }

    public void test003LockMassTextMsg() {
        // Clear the sms table.
        SqliteWrapper.delete(mContext, mContext.getContentResolver(), Sms.CONTENT_URI, null, null);

        // Insert a record in database first.
        mContentValues.put("_id", 1);
        SqliteWrapper.insert(mContext, mContext.getContentResolver(), Sms.CONTENT_URI, mContentValues);

        sMmsCompose.lockMassTextMsg(mContext, 1, -1, true);
        sMmsCompose.lockMassTextMsg(mContext, 1, 1, false);
    }

    public void test004ShowMassTextMsgDetail() {
        // Insert more records.
        mContentValues.put(Sms.TYPE, Sms.MESSAGE_TYPE_OUTBOX);
        SqliteWrapper.insert(mContext, mContext.getContentResolver(), Sms.CONTENT_URI, mContentValues);
        mContentValues.put(Sms.TYPE, Sms.MESSAGE_TYPE_DRAFT);
        SqliteWrapper.insert(mContext, mContext.getContentResolver(), Sms.CONTENT_URI, mContentValues);
        mContentValues.put(Sms.TYPE, Sms.MESSAGE_TYPE_FAILED);
        SqliteWrapper.insert(mContext, mContext.getContentResolver(), Sms.CONTENT_URI, mContentValues);

        sMmsCompose.showMassTextMsgDetail(mContext, -1);
    }

    public void test005DeleteMassTextMsg() {
        /// M: need modify this test method ,because the method has changed.
//        sMmsCompose.deleteMassTextMsg(1, -1);
    }

    public void test006GetNumbersFromIntent() {
        Intent intent = new Intent();
        intent.putExtra(IMmsComposeExt.USING_COLON, true);
        intent.putExtra(IMmsComposeExt.SELECTION_CONTACT_RESULT, TEST_CONTACTS);

        assertEquals(EXPECT_CONTACTS, sMmsCompose.getNumbersFromIntent(intent));
    }

    public void test007AddLayout() {
        LinearLayout panel = new LinearLayout(mContext);
    }

    public void test008EnableDRWarningDialog() {
        final String PREF_KEY_SHOW_DIALOG = "pref_key_show_dialog";

        sMmsCompose.enableDRWarningDialog(mContext, true, mSimIdCdma);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        sp.getBoolean(PREF_KEY_SHOW_DIALOG + "_" + mSimIdCdma, false);

        sMmsCompose.enableDRWarningDialog(mContext, false, mSimIdCdma);
        sp.getBoolean(PREF_KEY_SHOW_DIALOG + "_" + mSimIdCdma, true);
    }
}
