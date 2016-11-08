package com.mediatek.op.notification;

import android.util.Log;

import com.mediatek.common.PluginImpl;

/**
 * Customize the zen mode helper, op01 implementation.
 *
 */
@PluginImpl(interfaceName = "com.mediatek.common.notification.IZenModeHelperExt")
public class Op01ZenModeHelperExt extends DefaultZenModeHelperExt {
    private static final String TAG = "Op01ZenModeHelperExt";

    @Override
    public boolean customizeMuteAlarm(boolean muteAlarm) {
        Log.d(TAG, "customizeMuteAlarm, return false");
        return false;
    }
}
