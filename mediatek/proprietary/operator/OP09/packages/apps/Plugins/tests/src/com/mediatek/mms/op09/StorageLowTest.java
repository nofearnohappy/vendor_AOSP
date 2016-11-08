package com.mediatek.mms.op09;

import com.mediatek.common.MPlugin;
import com.mediatek.mms.ext.IStorageLowExt;
import com.mediatek.mms.op09.Op09MmsPluginTestRunner.BasicCase; 

public class StorageLowTest extends BasicCase {
    private IStorageLowExt mStorageLow;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mStorageLow = MPlugin.createInstance("com.mediatek.mms.ext.IStorageLow", mContext);
    }

    public void test001ShowNotification() {
        assertEquals("Message memory low", mStorageLow.getNotificationTitle());
        assertEquals("Message memory is nearly full. Please delete old messages.",
                mStorageLow.getNotificationBody());
    }

}
