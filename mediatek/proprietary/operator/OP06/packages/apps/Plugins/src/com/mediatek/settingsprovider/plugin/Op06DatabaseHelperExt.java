package com.mediatek.settingsprovider.plugin;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.android.ims.ImsConfig;

import com.mediatek.common.PluginImpl;
import com.mediatek.providers.settings.ext.DefaultDatabaseHelperExt;

@PluginImpl(interfaceName = "com.mediatek.providers.settings.ext.IDatabaseHelperExt")

public class Op06DatabaseHelperExt extends DefaultDatabaseHelperExt {

    private static final String TAG = "Op06DatabaseHelperExt";
    /**
     * @param context Context
     * constructor
     */
    public Op06DatabaseHelperExt(Context context) {
         super(context);
    }

    /**
     * @param context Context
     * @param name String
     * @param defaultValue String
     * @return the value
     * Used in settings provider for WFC feature
     */
    public String getResInteger(Context context, String name, String defaultValue) {
        String res = defaultValue;
        if (Settings.Global.WFC_IMS_MODE.equals(name)) {
            res = Integer.toString(ImsConfig.WfcModeFeatureValueConstants.CELLULAR_PREFERRED);
        }else {
            // nothing to do
        }
        Log.d(TAG, "get name = " + name + " int value = " + res);
        return res;
    }
}
