package com.mediatek.mms.op09;

import com.mediatek.common.MPlugin;
import com.mediatek.mms.ext.IMmsConfigExt;
import com.mediatek.mms.op09.Op09MmsPluginTestRunner.BasicCase;
import com.mediatek.pluginmanager.PluginManager;

public class MmsConfigTest extends BasicCase {
    private static IMmsConfigExt sMmsConfig;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sMmsConfig = MPlugin.createInstance("com.mediatek.mms.ext.IMmsConfig", mContext);
    }

    public void test001APIs() {
        assertEquals(11, sMmsConfig.getSmsToMmsTextThreshold());

        int[] retryScheme = sMmsConfig.getMmsRetryScheme();
        assertEquals(5 * 60 * 1000, retryScheme[retryScheme.length - 1]);

        assertFalse(sMmsConfig.isSupportCBMessage(mContext, 0));
        assertTrue(sMmsConfig.isSupportCBMessage(mContext, 1));

        assertTrue(sMmsConfig.isAllowRetryForPermanentFail());

        /// M: If the test sim card is under international roaming status,
        ///    the API will return false. And then you should change the assert statement.
        assertTrue(sMmsConfig.isAllowDRWhenRoaming(mContext, 0));
    }
}
