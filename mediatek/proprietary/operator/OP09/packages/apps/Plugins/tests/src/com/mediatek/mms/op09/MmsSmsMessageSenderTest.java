package com.mediatek.mms.op09;

import android.net.Uri;

import com.mediatek.common.MPlugin;
import com.mediatek.mms.ext.IMmsSmsMessageSenderExt;
import com.mediatek.mms.op09.Op09MmsPluginTestRunner.BasicCase; 

public class MmsSmsMessageSenderTest extends BasicCase {
    private static final long DATE_SENT = 1359092173;
    private static IMmsSmsMessageSenderExt sMessageSender;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sMessageSender = MPlugin.createInstance("com.mediatek.mms.ext.IMmsSmsMessageSender", mContext);
    }

    public void test001AddMessageToUri() {
        long dateSent = System.currentTimeMillis();

        assertNotNull(sMessageSender.addMessageToUri(mContext.getContentResolver(),
                Uri.parse("content://sms/queued"), TEST_ADDRESS, SMS_CONTENT, null,
                dateSent, true, false, 12345, mSimIdCdma, -dateSent));
    }
}
