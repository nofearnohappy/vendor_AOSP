package com.mediatek.op.notification;

import android.util.Log;

import com.mediatek.common.PluginImpl;
import com.mediatek.common.notification.IZenModeHelperExt;

/**
 * Customize the zen mode helper, default implementation.
 *
 */
@PluginImpl(interfaceName = "com.mediatek.common.notification.IZenModeHelperExt")
public class DefaultZenModeHelperExt implements IZenModeHelperExt {
    private static final String TAG = "DefaultZenModeHelperExt";
    @Override
    public boolean customizeMuteAlarm(boolean muteAlarm) {
        Log.d(TAG, "customizeMuteAlarm, muteAlarm = " + muteAlarm);
        return muteAlarm;
    }
}
