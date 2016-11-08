package com.mediatek.deviceregister;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.mediatek.deviceregister.utils.AgentProxy;
import com.mediatek.deviceregister.utils.PlatformManager;

public class RegisterReceiver extends BroadcastReceiver {

    private static final String TAG = Const.TAG_PREFIX + "RegisterReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(TAG, "onReceive " + intent);

        if (!isSwitchOpen()) {
            Log.i(TAG, "Feature is not enabled, do nothing");
            android.os.Process.killProcess(android.os.Process.myPid());
            return;
        }

        String action = intent.getAction();
        if (action.equalsIgnoreCase(Const.ACTION_BOOTCOMPLETED)) {
            PlatformManager.clearPreferences(context);
        }   else if (action.equalsIgnoreCase(Const.ACTION_PRE_BOOT_COMPLETED) ||
                     action.equalsIgnoreCase(Const.ACTION_CT_CONFIRMED_MESSAGE)) {
            goToService(context, intent);

        } else if (Const.ACTION_REGISTER_FEASIBLE.equalsIgnoreCase(action)) {

            SharedPreferences sharedPrf = PlatformManager.getUniquePreferences(context);
            boolean notFirst = sharedPrf.getBoolean(Const.PRE_KEY_NOT_FIRST_SUBINFO, false);
            if (!notFirst) {
                sharedPrf.edit().putBoolean(Const.PRE_KEY_NOT_FIRST_SUBINFO, true).commit();
                goToService(context, intent);
            }
        }
    }

    private void goToService(Context context, Intent intent) {
        intent.setClass(context, RegisterService.class);
        context.startService(intent);
    }

    private boolean isSwitchOpen() {
        return AgentProxy.getInstance().isFeatureEnabled();
    }

}
