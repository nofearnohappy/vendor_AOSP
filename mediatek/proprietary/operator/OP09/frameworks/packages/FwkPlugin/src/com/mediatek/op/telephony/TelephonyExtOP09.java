package com.mediatek.op.telephony;

import android.provider.Settings;
import android.telephony.Rlog;

import com.mediatek.common.PluginImpl;
import com.mediatek.internal.telephony.ltedc.svlte.SvltePhoneProxy;

/**
 * TelephonyExt OP09 plugin.
 *
 */
@PluginImpl(interfaceName="com.mediatek.common.telephony.ITelephonyExt")
public class TelephonyExtOP09 extends TelephonyExt {
    private static final String TAG = "TelephonyExtOP09";

    @Override
    public boolean ignoreDataRoaming() {
        Rlog.d(TAG, "ignoreDataRoaming, return true");
        return true;
    }

}
