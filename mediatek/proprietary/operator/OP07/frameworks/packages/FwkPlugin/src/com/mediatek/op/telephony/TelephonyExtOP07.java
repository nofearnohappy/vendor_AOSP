package com.mediatek.op.telephony;

import com.mediatek.common.PluginImpl;

@PluginImpl(interfaceName="com.mediatek.common.telephony.ITelephonyExt")
public class TelephonyExtOP07 extends TelephonyExt {
    public boolean isRatMenuControlledBySIM() {
        return true;
    }
}
