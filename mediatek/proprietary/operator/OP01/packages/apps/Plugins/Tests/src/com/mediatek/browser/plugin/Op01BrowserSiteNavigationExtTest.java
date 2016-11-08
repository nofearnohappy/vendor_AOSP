package com.mediatek.op01.tests;

import android.content.Context;
import android.test.InstrumentationTestCase;

import com.mediatek.browser.plugin.Op01BrowserSiteNavigationExt;
import com.mediatek.common.MPlugin;

public class Op01BrowserSiteNavigationExtTest extends InstrumentationTestCase {

    private final String TAG = "Op01BrowserSiteNavigationExtTest";
    private static Op01BrowserSiteNavigationExt mPlugin = null;
    private Context mContext;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = this.getInstrumentation().getContext();
        Object plugin = MPlugin.createInstance("com.mediatek.browser.ext.IBrowserSiteNavigationExt", mContext);
        if (plugin instanceof Op01BrowserSiteNavigationExt) {
            mPlugin = (Op01BrowserSiteNavigationExt) plugin;
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mPlugin = null;
    }

    public void test01_getPredefinedWebsites() {
        if (mPlugin != null) {
            CharSequence[] websites = mPlugin.getPredefinedWebsites();
            assertNotNull(websites);
        }
    }

}