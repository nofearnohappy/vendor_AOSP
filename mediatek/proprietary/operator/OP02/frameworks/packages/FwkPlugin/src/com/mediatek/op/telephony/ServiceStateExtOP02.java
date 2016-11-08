package com.mediatek.op.telephony;

import android.content.Context;
import android.os.SystemProperties;

import android.telephony.SignalStrength;
import android.util.Log;

import com.mediatek.common.PluginImpl;

/// M: The OP02 implementation of ServiceState.

@PluginImpl(interfaceName="com.mediatek.common.telephony.IServiceStateExt")
public class ServiceStateExtOP02 extends DefaultServiceStateExt {
    public ServiceStateExtOP02() {
    }

    public ServiceStateExtOP02(Context context) {
    }

    //[ALPS01558804] MTK-START: send notification for using some spcial icc card
    @Override
    public boolean needIccCardTypeNotification(String iccCardType) {
        if ((SystemProperties.getInt("ro.mtk_lte_support", 0) == 1)
                    && (iccCardType.equals("SIM"))) {
            return true;
        }
        return false;
    }
    //[ALPS01558804] MTK-END: send notification for using some spcial icc card

    //[ALPS01706187]MTK-START: Modify signal level's mapping rule
    @Override
    public int mapLteSignalLevel(int mLteRsrp, int mLteRssnr, int mLteSignalStrength) {
        int rsrpIconLevel;

        if (mLteRsrp < -140 || mLteRsrp > -44) {
            rsrpIconLevel = SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
        } else if (mLteRsrp >= -97) {
            rsrpIconLevel = SignalStrength.SIGNAL_STRENGTH_GREAT;
        } else if (mLteRsrp >= -105) {
            rsrpIconLevel = SignalStrength.SIGNAL_STRENGTH_GOOD;
        } else if (mLteRsrp >= -113) {
            rsrpIconLevel = SignalStrength.SIGNAL_STRENGTH_MODERATE;
        } else if (mLteRsrp >= -120) {
            rsrpIconLevel = SignalStrength.SIGNAL_STRENGTH_POOR;
        } else {
            rsrpIconLevel = SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
        }
        Log.i(TAG, "op02_mapLteSignalLevel=" + rsrpIconLevel);
        return rsrpIconLevel;
    }
    //[ALPS01706187]MTK-START: Modify signal level's mapping rule
}
