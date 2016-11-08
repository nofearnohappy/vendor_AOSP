package com.mediatek.incallui.plugin;

import android.util.Log;

import com.mediatek.common.PluginImpl;
import com.mediatek.incallui.ext.DefaultEmergencyCallCardExt;

/**
 * callcard extension plugin for op09.
*/
@PluginImpl(interfaceName = "com.mediatek.incallui.ext.IEmergencyCallCardExt")
public class OP09CallerInfoAsyncQueryExt extends DefaultEmergencyCallCardExt {

    private static final String TAG = "OP09CallerInfoAsyncQueryExt";

    public String getEmergencyCallAddress(String address, boolean isEmergency) {
        log("isEmergency " + isEmergency + " address " + address);
        if (isEmergency) {
            return address;	
        }
        return null;
    }

    /**
     * simple log info.
     *
     * @param msg need print out string.
     * @return void.
     */
    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
