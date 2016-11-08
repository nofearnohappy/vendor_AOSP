package com.mediatek.rcs.genericui;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import org.gsma.joyn.JoynServiceConfiguration;

public class RcsNotify extends BroadcastReceiver {

    private final static String TAG = "RcsNotify";
    private final static int NOTIFICATION_ID = 1000;
    private final static int ILLEGAL_STATE = -1;
    private final static int RCS_CORE_LOADED = 0;
    private final static int RCS_CORE_FAILED = 1;
    private final static int RCS_CORE_STARTED = 2;
    private final static int RCS_CORE_STOPPED = 3;
    private final static int RCS_CORE_IMS_CONNECTED = 4;
    private final static int RCS_CORE_IMS_TRY_CONNECTION = 5;
    private final static int RCS_CORE_IMS_CONNECTION_FAILED = 6;
    private final static int RCS_CORE_IMS_TRY_DISCONNECT = 7;
    private final static int RCS_CORE_IMS_BATTERY_DISCONNECTED = 8;
    private final static int RCS_CORE_IMS_DISCONNECTED = 9;
    private final static int RCS_CORE_NOT_LOADED = 10;

    private static final String RCS_SET_MODE = "rcs_set_mode";
    private static final String RCS_CORE_STATE = "rcs_core_state";
    private static final String RCS_SMS_MODE = "rcs_sms_mode";
    private static final String MODE_ENABLED = "enable";
    private static final String RCS_SWITCH_ENABLE = "com.mediatek.rcs.genericui.RCS_SWITCH_ENABLE";

    private Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        Log.d(TAG, "onReceive " + intent.getAction());
        mContext = context;
        if (intent.getAction().equals("org.gsma.joyn.action.VIEW_SETTINGS")) {

            int state = intent.getIntExtra("label_enum", ILLEGAL_STATE);
            Log.d(TAG, "onReceive state:" + state);

            dealRcsSetting(context, state);

            if (JoynServiceConfiguration.isServiceActivated(mContext) == false) {
                Log.d(TAG, "RCS is off, so return");
                return;
            }

            int strId = setStringByState(state);
            int iconId = R.drawable.rcs_core_notif_off;
            if (state == RCS_CORE_IMS_CONNECTED) {
                iconId = R.drawable.rcs_core_notif_on;
            }

            showNotification(strId, iconId);

        } else if (intent.getAction().equals("com.orangelabs.rcs.SHOW_403_NOTIFICATION")) {
            showPsDialogActivity();
        } else if (intent.getAction().equals("com.mediatek.intent.rcs.stack.StopService")) {
            // remove notification
            NotificationManager notificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(NOTIFICATION_ID);
        } else if (intent.getAction().equals("com.mediatek.rcs.genericui.SMS_MODE_CHANGE")) {
            SharedPreferences sh = context.getSharedPreferences(
                    RCS_SMS_MODE,
                    Context.MODE_WORLD_READABLE);
            SharedPreferences.Editor editor = sh.edit();
            boolean value = intent.getBooleanExtra(MODE_ENABLED, true);
            editor.putBoolean(MODE_ENABLED, value);
            editor.commit();
        } else if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            SharedPreferences sh = mContext.getSharedPreferences(
                RCS_SET_MODE,
                Context.MODE_WORLD_READABLE);
            SharedPreferences.Editor editor = sh.edit();
            editor.putBoolean(MODE_ENABLED, true);
            editor.putInt(RCS_CORE_STATE, RCS_CORE_NOT_LOADED);
            editor.commit();
        }
    }

    private void dealRcsSetting(Context context, int state) {
        SharedPreferences rcsSh = mContext.getSharedPreferences(
            RCS_SET_MODE,
            Context.MODE_WORLD_READABLE);
        SharedPreferences.Editor editor = rcsSh.edit();
        boolean rcsState = JoynServiceConfiguration.isServiceActivated(mContext);
        Log.d(TAG, "RcsSettingsState corestate:" + state + ", on:" + rcsState);

        //if (rcsState && state == RCS_CORE_LOADED) {
        //    //start one timer
        //} else
        if (state == RCS_CORE_IMS_TRY_CONNECTION) {
            editor.putBoolean(MODE_ENABLED, false);
            editor.putInt(RCS_CORE_STATE, state);
            editor.commit();
            Intent rcsIntent = new Intent(RCS_SWITCH_ENABLE);
            rcsIntent.putExtra(MODE_ENABLED, false);
            context.sendBroadcast(rcsIntent);
        } else if ((rcsState == false && (state == RCS_CORE_STOPPED))
            || (rcsState &&
                (state == RCS_CORE_IMS_CONNECTION_FAILED
                || state == RCS_CORE_IMS_CONNECTED
                || state == RCS_CORE_LOADED))) {
            editor.putBoolean(MODE_ENABLED, true);
            editor.putInt(RCS_CORE_STATE, state);
            editor.commit();
            Intent rcsIntent = new Intent(RCS_SWITCH_ENABLE);
            rcsIntent.putExtra(MODE_ENABLED, true);
            context.sendBroadcast(rcsIntent);
        } else {
            editor.putInt(RCS_CORE_STATE, state);
            editor.commit();
        }
    }

    private void showNotification(int strId, int iconId) {
        Intent intent = new Intent("com.mediatek.rcs.genericui.RcsSettingsActivity");
        //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Notification notif = new Notification(mContext, iconId, "",
            System.currentTimeMillis(),
            mContext.getString(R.string.rcs_core_rcs_notification_title),
            mContext.getString(strId), intent);
        notif.flags = Notification.FLAG_NO_CLEAR;
        NotificationManager notificationManager =
            (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notif);
    }

    private void showPsDialogActivity() {
        Intent activityIntent = new Intent();
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        activityIntent.setClass(mContext, RcsPsAlertDialog.class);
        mContext.startActivity(activityIntent);
    }
    
    private int setStringByState(int state) {
        int strId;
        switch (state) {
        case RCS_CORE_LOADED:
            strId = R.string.rcs_core_loaded;
            break;
        case RCS_CORE_FAILED:
            strId = R.string.rcs_core_failed;
            break;
        case RCS_CORE_STARTED:
            strId = R.string.rcs_core_started;
            break;
        case RCS_CORE_STOPPED:
            strId = R.string.rcs_core_stopped;
            break;
        case RCS_CORE_IMS_CONNECTED:
            strId = R.string.rcs_core_ims_connected;
            break;
        case RCS_CORE_IMS_TRY_CONNECTION:
            strId = R.string.rcs_core_ims_try_connection;
            break;
        case RCS_CORE_IMS_CONNECTION_FAILED:
            strId = R.string.rcs_core_ims_connection_failed;
            break;
        case RCS_CORE_IMS_TRY_DISCONNECT:
            strId = R.string.rcs_core_ims_try_disconnect;
            break;
        case RCS_CORE_IMS_BATTERY_DISCONNECTED:
            strId = R.string.rcs_core_ims_battery_disconnected;
            break;
        case RCS_CORE_IMS_DISCONNECTED:
            strId = R.string.rcs_core_ims_disconnected;
            break; 
        case ILLEGAL_STATE:
        default :
            strId = R.string.app_name; // Example
            break;
        }
        
        return strId;
    }

}
