package com.mediatek.systemui.plugin;

import com.mediatek.common.PluginImpl;
import com.mediatek.systemui.ext.DefaultMobileIconExt;

/**
 * M: OP02 IMobileIconExt implements for Plug-in definition of customize mobile icons.
 */
@PluginImpl(interfaceName = "com.mediatek.systemui.ext.IMobileIconExt")
public class OP02MobileIconExt extends DefaultMobileIconExt {
    public static final String TAG = "OP02MobileIconExt";
    public static final int NET_ON = 1;

    @Override
    public int customizeWifiNetCondition(int netCondition) {
        return NET_ON;
    }
    @Override
    public int customizeMobileNetCondition(int netCondition) {
        return NET_ON;
    }
}
