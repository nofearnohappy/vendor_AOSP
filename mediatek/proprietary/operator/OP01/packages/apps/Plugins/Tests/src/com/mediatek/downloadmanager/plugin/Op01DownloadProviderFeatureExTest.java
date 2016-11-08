package com.mediatek.op01.tests;

import android.content.Context;
import android.test.InstrumentationTestCase;

import com.mediatek.downloadmanager.plugin.Op01DownloadProviderFeatureExt;
import com.mediatek.common.MPlugin;

public class Op01DownloadProviderFeatureExTest extends InstrumentationTestCase
{
    private final String TAG = "Op01DownloadProviderFeatureExTest";
    private static Op01DownloadProviderFeatureExt mPlugin = null;
    private Context mContext;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = this.getInstrumentation().getContext();
        Object plugin = MPlugin.createInstance(
            "com.mediatek.downloadmanager.ext.IDownloadProviderFeatureEx", mContext);
        if (plugin instanceof Op01DownloadProviderFeatureExt) {
            mPlugin = (Op01DownloadProviderFeatureExt) plugin;
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mPlugin = null;
    }


    public void test01_shouldSetDownloadPath() {
        if (mPlugin != null) {
            boolean mode = mPlugin.shouldSetDownloadPath();
            assertTrue(mode);
        }
    }


}
