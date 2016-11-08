package com.mediatek.settings.plugin;

import android.net.wifi.WifiConfiguration;

import com.mediatek.common.PluginImpl;
import com.mediatek.settingslib.ext.DefaultWifiLibExt;

/**
 * Default plugin implementation.
 */
@PluginImpl(interfaceName = "com.mediatek.settingslib.ext.IWifiLibExt")
public class Op01WifiLibExt extends DefaultWifiLibExt {

    @Override
    public boolean shouldCheckNetworkCapabilities() {
        return false;
    }

    @Override
    public void appendApSummary(StringBuilder summary, int autoJoinStatus,
        String connectFail, String disabled) {
        if (autoJoinStatus == WifiConfiguration.AUTO_JOIN_DISABLED_USER_ACTION) {
            summary.append(disabled);
        } else {
            summary.append(connectFail);
        }
    }
}
