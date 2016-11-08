package com.mediatek.mms.op09;

import android.widget.TextView;

import com.mediatek.common.MPlugin;
import com.mediatek.mms.ext.IMmsDialogModeExt;
import com.mediatek.mms.op09.Op09MmsPluginTestRunner.BasicCase; 

public class MmsDialogModeTest extends BasicCase {
    private static IMmsDialogModeExt sMmsDialogMode;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sMmsDialogMode = MPlugin.createInstance("com.mediatek.mms.ext.IMmsDialogMode", mContext);
    }

    public void test001SetSimTypeDrawable() {
        if (checkSims()) {
            TextView textView = new TextView(mContext);
            textView.setText("China Telecom");
            sMmsDialogMode.setSimTypeDrawable(mContext, mSimIdCdma, textView);
            sMmsDialogMode.setSimTypeDrawable(mContext, mSimIdGsm, textView);
        }

        assertNotNull(sMmsDialogMode.getNotificationContentString(
                TEST_ADDRESS, "Test Suject", "100kb", "2013-12-31"));
    }
}
