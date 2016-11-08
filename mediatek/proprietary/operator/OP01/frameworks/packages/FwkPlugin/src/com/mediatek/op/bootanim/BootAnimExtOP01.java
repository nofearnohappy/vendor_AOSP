package com.mediatek.op.bootanim;

import android.os.SystemProperties;
import android.util.Log;
import com.mediatek.common.PluginImpl;

/**
 * Interface that defines all methos which are implemented in ConnectivityService
 * {@hide}
 */
@PluginImpl(interfaceName="com.mediatek.common.bootanim.IBootAnimExt")
public class BootAnimExtOP01 extends DefaultBootAnimExt {

    private static final String TAG = "BootAnimExt";

    public BootAnimExtOP01() {
        Log.d(TAG, "BootAnimExtOP01 Contrustor !");
    }

    @Override
    public int getScreenTurnOffTime() {
        // op01 7*1000
        // 0p02 5*1000
        return 5 * 1000;
    }


    @Override
    public boolean isCustBootAnim() {
        if (1 == SystemProperties.getInt("ro.mtk_lte_support", 0)) {
            return true;
        } else {
            return false;
        }
    }
}
