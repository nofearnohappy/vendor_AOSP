package com.mediatek.op.bootanim;

import android.util.Log;
import com.mediatek.common.PluginImpl;

/**
 * Interface that defines all methos which are implemented in ConnectivityService
 * {@hide}
 */
@PluginImpl(interfaceName="com.mediatek.common.bootanim.IBootAnimExt")
public class BootAnimExtOP09 extends DefaultBootAnimExt {

    private static final String TAG = "BootAnimExt";

    public BootAnimExtOP09() {
        Log.d(TAG, "BootAnimExtOP09 Contrustor !");
    }

    @Override
    public int getScreenTurnOffTime() {
        // op01 7*1000
        // 0p02 5*1000
        // 0p09 3*1000
        return 3 * 1000;
    }


    @Override
    public boolean isCustBootAnim() {
        return true;
    }
}
