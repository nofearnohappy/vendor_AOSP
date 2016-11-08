package com.mediatek.phone.plugin;

import android.preference.PreferenceActivity;
import android.text.TextUtils;
import android.util.Log;

import com.android.ims.ImsManager;

import com.mediatek.common.PluginImpl;
import com.mediatek.phone.ext.DefaultCallFeaturesSettingExt;
import com.mediatek.wfc.plugin.WfcSettings;

/**
 * Plugin implementation for WFC Settings plugin
 */

@PluginImpl(interfaceName = "com.mediatek.phone.ext.ICallFeaturesSettingExt")
public class OP16CallFeaturesSettingExt extends DefaultCallFeaturesSettingExt {
    private static final String TAG = "OP16CallFeaturesSettingExt";
    private WfcSettings mWfcSettings = null;

    @Override
    public void initOtherCallFeaturesSetting(PreferenceActivity activity) {

        Log.d(TAG, "initOtherCallFeaturesSetting" + activity.getClass().getSimpleName());
        if (TextUtils.equals(activity.getClass().getSimpleName(), "CallFeaturesSetting")) {
            if (ImsManager.isWfcEnabledByPlatform(activity)) {
                mWfcSettings = WfcSettings.getInstance(activity);
                mWfcSettings.customizedWfcPreference(activity, activity.getPreferenceScreen());
            }
        }
    }
}
