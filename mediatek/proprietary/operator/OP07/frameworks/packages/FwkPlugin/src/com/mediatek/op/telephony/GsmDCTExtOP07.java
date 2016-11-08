package com.mediatek.op.telephony;

import android.content.Context;

import com.android.internal.telephony.dataconnection.DcFailCause;

import com.mediatek.common.PluginImpl;

@PluginImpl(interfaceName="com.mediatek.common.telephony.IGsmDCTExt")
public class GsmDCTExtOP07 extends GsmDCTExt {
    private Context mContext;

    public GsmDCTExtOP07(Context context) {
    }

    public boolean isDomesticRoamingEnabled() {
        return true;
    }

    public boolean isIgnoredCause(Object cause) {
        DcFailCause tmpCause = (DcFailCause) cause;
        log("[OP07] Check sm cause:" +  tmpCause);
        if (tmpCause == DcFailCause.DUE_TO_REACH_RETRY_COUNTER) {
            log("[OP07] Sm cause: DUE_TO_REACH_RETRY_COUNTER(3599)");
            return true;
        }
        return false;
    }
}
