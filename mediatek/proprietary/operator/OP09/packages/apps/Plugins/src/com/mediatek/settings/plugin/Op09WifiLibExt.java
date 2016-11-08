package com.mediatek.settings.plugin;


import android.util.Log;
import com.mediatek.common.PluginImpl;
import com.mediatek.settingslib.ext.DefaultWifiLibExt;

/**
 * Default plugin implementation.
 */
@PluginImpl(interfaceName = "com.mediatek.settingslib.ext.IWifiLibExt")
public class Op09WifiLibExt extends DefaultWifiLibExt {

    @Override
    public boolean shouldCheckNetworkCapabilities() {
        Log.d("Op09WifiLibExt", "shouldCheckNetworkCapabilities()");
        return false;
    }

}
