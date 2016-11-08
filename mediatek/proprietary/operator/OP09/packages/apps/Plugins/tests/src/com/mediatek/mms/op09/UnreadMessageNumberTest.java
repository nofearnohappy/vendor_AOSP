package com.mediatek.mms.op09;

import com.mediatek.common.MPlugin;
import com.mediatek.mms.ext.IUnreadMessageNumberExt;
import com.mediatek.mms.op09.Op09MmsPluginTestRunner.BasicCase; 

public class UnreadMessageNumberTest extends BasicCase {
    private IUnreadMessageNumberExt mUnreadMessageNumber;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mUnreadMessageNumber = MPlugin.createInstance("com.mediatek.mms.ext.IUnreadMessageNumber", mContext);
    }

    public void test001UpdateUnreadMsgNumber() {
        mUnreadMessageNumber.updateUnreadMessageNumber(mContext);
    }

}
