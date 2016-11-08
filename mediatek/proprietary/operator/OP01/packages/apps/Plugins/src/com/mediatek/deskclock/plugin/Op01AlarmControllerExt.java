package com.mediatek.deskclock.plugin;

import android.content.Context;
import android.util.Log;

import com.mediatek.common.PluginImpl;
import com.mediatek.deskclock.ext.DefaultAlarmControllerExt;

/**
 * OP implementation of Plug-in definition of Desk Clock.
 */
@PluginImpl(interfaceName="com.mediatek.deskclock.ext.IAlarmControllerExt")
public class Op01AlarmControllerExt extends DefaultAlarmControllerExt {

    private static final String TAG = "Op01AlarmControllerExt";

    @Override
    public void vibrate(Context context) {
        Log.v(TAG, "Do not vibrate when in call state");
    }
}