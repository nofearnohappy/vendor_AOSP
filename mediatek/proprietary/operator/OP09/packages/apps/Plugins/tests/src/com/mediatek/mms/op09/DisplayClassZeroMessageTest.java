package com.mediatek.mms.op09;

import android.content.Intent;

import com.mediatek.common.MPlugin;
import com.mediatek.mms.op09.Op09MmsPluginTestRunner.BasicCase;
import com.mediatek.mms.ext.IDisplayClassZeroMessageExt;

public class DisplayClassZeroMessageTest extends BasicCase {
    private static IDisplayClassZeroMessageExt sDisplayClass0Message;
    private static int EXPECT_FLAG = Intent.FLAG_ACTIVITY_NEW_TASK
            | Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            | Intent.FLAG_ACTIVITY_SINGLE_TOP;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sDisplayClass0Message = MPlugin.createInstance("com.mediatek.mms.ext.IDisplayClassZeroMessage",mContext);
    }

    public void test001SetLaunchMode() {
        Intent intent = new Intent();
        int flagValue = sDisplayClass0Message.setLaunchMode(intent).getFlags();
        assertEquals(EXPECT_FLAG, flagValue);
    }
}
