package com.mediatek.op.wfc;

import android.content.Intent;
import android.util.Log;

import com.mediatek.common.PluginImpl;

/** Interface that defineds all methos which are implemented in IMSN.
 */

@PluginImpl(interfaceName = "com.mediatek.common.wfc.IImsNotificationControllerExt")
public class ImsNotificationControllerExtOP16 extends DefaultImsNotificationControllerExt {
    private static final String TAG = "ImsNotificationControllerExtOP16";
    private static final String ACTION_WIRELESS_SETTINGS_LAUNCH
            = "android.settings.WIRELESS_SETTINGS";

    @Override
    /**
     * Get customized intent.
     * @param event IMSN event
     * @param defaultIntent defaultIntent
     * @ return intent
     */
    public Intent getIntent(int event, Intent defaultIntent) {
        Intent intent;
        Log.i(TAG, "event:" + event);
        switch(event) {
            case DefaultImsNotificationControllerExt.REGISTRATION:
            case DefaultImsNotificationControllerExt.ERROR:
                intent = new Intent(ACTION_WIRELESS_SETTINGS_LAUNCH);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                break;

            default:
                intent = defaultIntent;
        }
        return intent;
    }
}

