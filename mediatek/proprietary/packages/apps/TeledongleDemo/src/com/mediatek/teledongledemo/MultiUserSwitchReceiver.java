package com.mediatek.teledongledemo;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.util.Log;

public class MultiUserSwitchReceiver extends BroadcastReceiver {
    private static final String TAG = "MultiUserSwitchReceiver"; 
    private static Context sContext = null;

    public void onReceive(Context context, Intent intent) {
        sContext = context;
        Log.d(TAG, "In onReceive ");

        /** M: Bug Fix for CR ALPS01633785: when switch to normal owner, do not show TE dongle @{ */  
        if (UserHandle.myUserId() != UserHandle.USER_OWNER) {
            sContext.getPackageManager().setComponentEnabledSetting(
                    new ComponentName("com.mediatek.teledongledemo","com.mediatek.teledongledemo.TeledongleDemoActivity"),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);           
        }
        /** @} */
    } 
}
