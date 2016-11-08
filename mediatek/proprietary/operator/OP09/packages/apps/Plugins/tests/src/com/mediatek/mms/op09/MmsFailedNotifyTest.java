package com.mediatek.mms.op09;

import com.mediatek.common.MPlugin;
import com.mediatek.mms.op09.Op09MmsPluginTestRunner.BasicCase;
import com.mediatek.mms.ext.IMmsFailedNotifyExt; 

public class MmsFailedNotifyTest extends BasicCase {
    private static IMmsFailedNotifyExt sMmsFailedNotify;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sMmsFailedNotify = MPlugin.createInstance("com.mediatek.mms.ext.IMmsFailedNotify" , mContext);
    }

    public void test001PopUpToast() throws InterruptedException {
        sMmsFailedNotify.popupToast(IMmsFailedNotifyExt.REQUEST_RESPONSE_TEXT, null);
        sMmsFailedNotify.popupToast(IMmsFailedNotifyExt.REQUEST_RESPONSE_TEXT, "Some reason from server.");
        delay(DELAY_TIME);
        sMmsFailedNotify.popupToast(IMmsFailedNotifyExt.DATA_OCCUPIED, null);
        delay(DELAY_TIME);
        sMmsFailedNotify.popupToast(IMmsFailedNotifyExt.CONNECTION_FAILED, null);
        delay(DELAY_TIME);
        sMmsFailedNotify.popupToast(IMmsFailedNotifyExt.GATEWAY_NO_RESPONSE, null);
        delay(DELAY_TIME);
        sMmsFailedNotify.popupToast(IMmsFailedNotifyExt.CANCEL_DOWNLOAD, null);
        delay(DELAY_TIME);
        sMmsFailedNotify.popupToast(IMmsFailedNotifyExt.DISABLE_DELIVERY_REPORT, null);
    }

    public void test002FailedNotificationEnabled() {
        sMmsFailedNotify.setFailedNotificationEnabled(false);
        assertFalse(sMmsFailedNotify.getFailedNotificationEnabled());
    }
}
