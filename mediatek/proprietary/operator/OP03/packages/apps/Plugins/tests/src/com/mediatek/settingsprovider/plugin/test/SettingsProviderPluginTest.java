package com.mediatek.settingsprovider.plugin.test;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.test.InstrumentationTestCase;


import com.mediatek.op03.plugin.R;
import com.mediatek.common.MPlugin;
import com.mediatek.providers.settings.ext.IDatabaseHelperExt;
import com.mediatek.settingsprovider.plugin.Op03DatabaseHelperExt;

public class SettingsProviderPluginTest extends InstrumentationTestCase {

    private static Op03DatabaseHelperExt sDb03Ext = null;
    private Context mContext;
    private ContentResolver mCr;
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mContext = this.getInstrumentation().getContext();
        mCr = mContext.getContentResolver();
        //Object plugin = PluginManager.createPluginObject(mContext, IDatabaseHelperExt.class.getName());
         Object plugin = (IDatabaseHelperExt) MPlugin.createInstance(
                     		IDatabaseHelperExt.class.getName(), mContext);
        if (plugin instanceof Op03DatabaseHelperExt) {
            sDb03Ext = (Op03DatabaseHelperExt) plugin;
        }
    }

    // test the function of getResStr(Context context, String name, String defaultValue)
    public void test01_getStringValue() {
        String locationName = Settings.Secure.LOCATION_PROVIDERS_ALLOWED;
        int locationResId = R.string.def_location_providers_allowed_op03;
        String locationValue = sDb03Ext.getResStr(mContext, locationName, null);
        assertTrue(locationValue != null);
    }

    // test the function of getResBoolean(Context context, String name, String defaultValue)
    public void test02_getBooleanValue() {
        // test the install_non_market_apps
        String nonMarket = Settings.Secure.INSTALL_NON_MARKET_APPS;
        int nonMarketResId = R.bool.def_install_non_market_apps_op03;
        String nonMarketValue = sDb03Ext.getResBoolean(mContext, nonMarket, "1");
        assertEquals("0", nonMarketValue);
        // test the others
        String testName = "testBool";
        String value = sDb03Ext.getResBoolean(mContext, testName, "1");
        assertEquals("1", value);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        sDb03Ext = null;
    }

}
