package com.mediatek.op.telephony;

import com.mediatek.common.PluginImpl;

@PluginImpl(interfaceName="com.mediatek.common.telephony.ITelephonyExt")
public class TelephonyExtOP02 extends TelephonyExt {
    public boolean isDefaultDataOn() {
        return true;
    }

    public boolean isAutoSwitchDataToEnabledSim() {
        return true;
    }

    public boolean isDefaultEnable3GSIMDataWhenNewSIMInserted() {
        return true;
    }
}
