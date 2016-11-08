package com.mediatek.op.telephony;

import android.content.Context;

import com.android.internal.telephony.RetryManager;
import com.android.internal.telephony.dataconnection.DcFailCause;

import com.mediatek.common.PluginImpl;

@PluginImpl(interfaceName="com.mediatek.common.telephony.IGsmDCTExt")
public class GsmDCTExtOP03 extends GsmDCTExt {
    private Context mContext;

    public GsmDCTExtOP03(Context context) {
    }

    public boolean needSmRetry(Object cause) {

        DcFailCause tmpcause = (DcFailCause) cause;
        log("[OP] Check sm cause:" +  tmpcause);
        if (tmpcause == DcFailCause.MISSING_UNKNOWN_APN) {
            return true;
        }

        return false;
    }

    public boolean doSmRetry(Object cause, Object obj1, Object obj2) {
        log("[OP] doSmRetry: setSmRetryConfig");
        setSmRetryConfig(obj1);
        return true;
    }

    public boolean setSmRetryConfig(Object retryManager) {
        RetryManager rm = (RetryManager) retryManager;
        int retryCount = 4;
        log("[OP] RetryManager:" + rm);
        log("[OP] setCurMaxRetryCount:" + retryCount);
        rm.setCurMaxRetryCount(retryCount);
        return true;
    }

    public boolean needRacUpdate() {
        return false;
    }
}

