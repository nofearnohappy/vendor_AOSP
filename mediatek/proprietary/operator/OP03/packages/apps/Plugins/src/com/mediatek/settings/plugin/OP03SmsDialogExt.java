package com.mediatek.settings.plugin;

import java.util.Calendar;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.view.KeyEvent;
import android.content.Intent;
import android.util.Log;
import android.content.ComponentName;
import android.os.Bundle;

import com.android.internal.app.AlertController;
import com.android.internal.telephony.SmsApplication;
import com.android.internal.telephony.SmsApplication.SmsApplicationData;
import com.android.internal.app.AlertActivity;

import com.mediatek.common.PluginImpl;
import com.mediatek.settings.ext.DefaultSmsDialogExt;

import com.mediatek.op03.plugin.R;

@PluginImpl(interfaceName = "com.mediatek.settings.ext.ISmsDialogExt")
public class OP03SmsDialogExt extends DefaultSmsDialogExt {
    private static final String TAG = "OP03SmsDialogExt";
    private static final String ALARM_ACTION = "com.android.notification.alarm";

    /**
     * If user set 3rd party xMS as default, set a notification to notify user it is not
     * the manufacturer one.
     * And give out the dialog again to switch back.
     * */
        Context mContext;

    public OP03SmsDialogExt(Context context)    {
              super(context);
              mContext = context;
              Log.i(TAG, "constructor\n");
        }
    private void setAlarm(Context context) {

        Log.i(TAG, "setAlarm");
//      mContext.registerReceiver(broadCastReceiverExt, new IntentFilter(ALARM_ACTION));
        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.SECOND, 3600*24*2);  //set start alarm 2 days later
        //calendar.add(Calendar.SECOND,2);
        Intent intent = new Intent(mContext, OP03AlarmBroadCastReceiverExt.class);
        intent.setAction(ALARM_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
        am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        Log.i(TAG, "setAlarm " + am  + " \n " + pendingIntent);
//        mContext.unregisterReceiver(broadCastReceiverExt);
    }

    /**
    * If user send Messaging as default sms, cancel alarm
    */
    private void cancelAlarm(Context context) {
    Log.i(TAG, "cancelAlarm\n");
        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(mContext, OP03AlarmBroadCastReceiverExt.class);
        intent.setAction(ALARM_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
        am.cancel(pendingIntent);
    }

    public boolean onClick(String newPackageName, AlertActivity activity, Context context,
                           int which) {
        Log.i(TAG, "onClick" + which);
        Intent intent;
        switch (which) {
            case AlertActivity.BUTTON_POSITIVE:
                intent = new Intent(OP03SmsPreferenceExt.SMS_UPDATE_RECEIVED);
                activity.sendBroadcast(intent);
                if (!newPackageName.equals("com.android.mms")) {
                    setAlarm(context);
                } else {
                    cancelAlarm(context);
                }
                break;
            case AlertActivity.BUTTON_NEGATIVE:
                SmsApplicationData oldSmsApplicationData = null;
                ComponentName oldSmsComponent = SmsApplication.getDefaultSmsApplication(activity,
                                                                                        true);
                if (oldSmsComponent != null) {
                    SmsApplication.setDefaultApplication(oldSmsComponent.getPackageName(),
                                                         activity);
                    intent = new Intent(OP03SmsPreferenceExt.SMS_UPDATE_CANCELED);
                    intent.putExtra("old_sms_app", oldSmsComponent.getPackageName());
                    activity.sendBroadcast(intent);
                    activity.setResult(AlertActivity.RESULT_OK);
                }
                break;
           default:
                  Log.i(TAG, "onClick new Dialog\n");
                  intent = new Intent("android.provider.Telephony.ACTION_CHANGE_DEFAULT");
                  intent.setPackage("com.android.settings");
                  Bundle bundle = new Bundle();
                  bundle.putString("package", newPackageName);
                  intent.putExtras(bundle);
                  //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                  //activity.finish();
                  activity.startActivity(intent);
                  return false;
        }
        return true;
    }

    public void buildMessage(AlertController.AlertParams  param, String packageName, Intent intent,
                             String newName, String oldName) {
        boolean isNotification = intent.getBooleanExtra("from_notification", false);
        Log.i(TAG, "buildMessage :" + isNotification + "packageName :" + packageName +
             "isNotification" + isNotification);
        if (packageName.equals("com.android.mms")) {
            if (isNotification) {
                param.mMessage = getResources().getString(R.string.sms_change_default_dialog_text1,
                                                          newName, oldName);
            }
        } else {
            param.mMessage = getResources().getString(R.string.sms_change_default_dialog_text1,
                                                      newName, oldName);
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event, AlertActivity context) {
        Log.i(TAG, "onKeyDown :" + keyCode);
  /*      switch (keyCode){
        case KeyEvent.KEYCODE_BACK:
            SmsApplicationData oldSmsApplicationData = null;
            ComponentName oldSmsComponent = SmsApplication.getDefaultSmsApplication(context, true);
            if (oldSmsComponent != null) {

                SmsApplication.setDefaultApplication(oldSmsComponent.getPackageName(), context);
                Intent intent = new Intent(OP03WirelessSettingExt.SMS_UPDATE_CANCELED);
                intent.putExtra("old_sms_app", oldSmsComponent.getPackageName());
                context.sendBroadcast(intent);
                context.setResult(AlertActivity.RESULT_OK);
            }
            break;
        }
        */
        return true;
        //return super.onKeyDown(keyCode, event);
    }
}
