package com.mediatek.dialer.plugin.dialpad;

import android.util.Log;

import com.mediatek.common.PluginImpl;
import com.mediatek.dialer.ext.DefaultSelectAccountExtension;

/**
 * SelectAccount extension plugin for op09.
*/
@PluginImpl(interfaceName = "com.mediatek.dialer.ext.ISelectAccountExtension")
public class OP09SelectAccountExtension extends DefaultSelectAccountExtension {
    private static final String TAG = "OP09SelectAccountExtension";

    /**
     * for select default account
     *
     * @param iconId: The id of the always_ask_account icon.
     */
    public int getAlwaysAskAccountIcon(int iconId) {
        log("getAlwaysAskAccountIcon");
        return 0;
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
