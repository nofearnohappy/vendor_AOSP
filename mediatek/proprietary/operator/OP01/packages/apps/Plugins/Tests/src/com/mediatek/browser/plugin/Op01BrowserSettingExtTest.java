package com.mediatek.op01.tests;

import android.content.Context;
import android.test.InstrumentationTestCase;

import com.mediatek.browser.plugin.Op01BrowserSettingExt;
import com.mediatek.common.MPlugin;

public class Op01BrowserSettingExtTest extends InstrumentationTestCase {

    private final String TAG = "Op01BrowserSettingExtTest";
    private static Op01BrowserSettingExt mPlugin = null;
    private Context mContext;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = this.getInstrumentation().getContext();
        Object plugin = MPlugin.createInstance("com.mediatek.browser.ext.IBrowserSettingExt", mContext);
        if (plugin instanceof Op01BrowserSettingExt) {
            mPlugin = (Op01BrowserSettingExt) plugin;
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mPlugin = null;
    }

    public void test01_getCustomerHomepage() {
        if (mPlugin != null) {
            String homepage = mPlugin.getCustomerHomepage();
            assertNotNull(homepage);
        }
    }

    public void test02_getOperatorUA() {
        if (mPlugin != null) {
            String ret = mPlugin.getOperatorUA("testString");
            assertNotNull(ret);
        }
    }

}