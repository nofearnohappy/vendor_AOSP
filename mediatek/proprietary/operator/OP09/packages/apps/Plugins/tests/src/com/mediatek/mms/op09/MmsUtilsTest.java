package com.mediatek.mms.op09;

import android.text.format.DateUtils;
import android.widget.TextView;

import com.mediatek.common.MPlugin;
import com.mediatek.mms.ext.IMmsUtilsExt;
import com.mediatek.mms.op09.Op09MmsPluginTestRunner.BasicCase; 

public class MmsUtilsTest extends BasicCase {
    private static final long DATE = 1359436497;
    private static final String TEST_STRING = "String for test.";
    private static IMmsUtilsExt sMmsUtils;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sMmsUtils = MPlugin.createInstance("com.mediatek.mms.ext.IMmsUtils", mContext);
    }

    // This case need to set time format of the phone to be 24 hour type.
    public void test001FormatDateAndTimeStampString() {
        sMmsUtils.formatDateAndTimeStampString(mContext, DATE , System.currentTimeMillis(), false, TEST_STRING);
        sMmsUtils.formatDateAndTimeStampString(mContext, DATE ,
                System.currentTimeMillis() - 90000000, false, TEST_STRING);
        sMmsUtils.formatDateAndTimeStampString(mContext, DATE ,
            System.currentTimeMillis() - 180000000, false, TEST_STRING);
        sMmsUtils.formatDateAndTimeStampString(mContext, DATE ,
                System.currentTimeMillis() - 60000, false, TEST_STRING);
        sMmsUtils.formatDateAndTimeStampString(mContext, DATE , DATE, false, TEST_STRING);
        sMmsUtils.formatDateAndTimeStampString(mContext, DATE, 0, true, TEST_STRING);
        sMmsUtils.formatDateAndTimeStampString(mContext, 0, 0, false, TEST_STRING);
    }

    public void test003ShowSimTypeBySimId() {
        if (checkSims()) {
            TextView textView = new TextView(mContext);
            textView.setText("China Telecom");
            sMmsUtils.showSimTypeBySimId(mContext, mSimIdCdma, textView);
            sMmsUtils.showSimTypeBySimId(mContext, mSimIdGsm, textView);
        }
    }

    // This case need to set time format of the phone to be 24 hour type and time zone set to GMT+8.
    public void test004FormatDateTime() {
        assertEquals("15:39", sMmsUtils.formatDateTime(mContext, DATE * 1000, DateUtils.FORMAT_SHOW_TIME));
    }

    public void test006IsWellFormedSmsAddress() {
        assertFalse(sMmsUtils.isWellFormedSmsAddress(""));
        assertFalse(sMmsUtils.isWellFormedSmsAddress("(86)15313706372"));
        assertFalse(sMmsUtils.isWellFormedSmsAddress("+(86)15313706372"));
        assertTrue(sMmsUtils.isWellFormedSmsAddress("+8615313706372"));
    }

}
