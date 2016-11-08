package com.mediatek.op.telephony;

import android.content.Context;
import android.text.TextUtils;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;

import com.mediatek.common.PluginImpl;

/**
 * Interface that defines methos which are implemented in IGsmDCTExt
 */

 /** {@hide} */
@PluginImpl(interfaceName="com.mediatek.common.telephony.IGsmDCTExt")
public class GsmDCTExtOP09 extends GsmDCTExt {
    private Context mContext;

    public GsmDCTExtOP09(Context context) {
    }

    /** getDisconnectDoneRetryTimer. */
    public int getDisconnectDoneRetryTimer(String reason, int defaultTimer) {
        int timer = defaultTimer;
        if (Phone.REASON_RA_FAILED.equals(reason)) {
            // RA failed, retry after 90s
            timer = 90000;
        }
        return timer;
    }

}

