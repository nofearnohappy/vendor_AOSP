package com.mediatek.op.telephony;
import com.mediatek.common.PluginImpl;

@PluginImpl(interfaceName="com.mediatek.common.telephony.IGsmConnectionExt")

public class GsmConnectionExtOP07 extends GsmConnectionExt {

    public int getFirstPauseDelayMSeconds(int defaultValue) {
       return 3000;  //Return 3 seconds.
    }
}
