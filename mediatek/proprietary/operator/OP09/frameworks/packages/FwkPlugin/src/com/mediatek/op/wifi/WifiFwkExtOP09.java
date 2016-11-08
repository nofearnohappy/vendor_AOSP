package com.mediatek.op.wifi;

import android.content.Context;
import android.util.Log;

import com.mediatek.common.PluginImpl;
import com.mediatek.common.wifi.IWifiFwkExt;

@PluginImpl(interfaceName="com.mediatek.common.wifi.IWifiFwkExt")
public class WifiFwkExtOP09 extends DefaultWifiFwkExt {
    private static final String TAG = "WifiFwkExtOP09";

    public WifiFwkExtOP09(Context context) {
        super(context);
    }

    public boolean isPppoeSupported() {
        Log.d(TAG, "isPppoeSupported is true");
        return true;
    }
}
