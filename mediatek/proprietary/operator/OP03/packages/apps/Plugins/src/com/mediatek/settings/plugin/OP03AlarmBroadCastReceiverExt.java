package com.mediatek.settings.plugin;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;


import com.mediatek.op03.plugin.R;

public class OP03AlarmBroadCastReceiverExt extends BroadcastReceiver {

    private static final String TAG = "OP03AlarmBroadCastReceiverExt";

    private Notification.Builder nBuilder;
    static int NOTIFICATION_ID = 13565400;
    @Override
    public void onReceive(Context context, Intent intent) {

        Log.i(TAG, "onReceive\n");
        Intent i = new Intent(
                "android.provider.Telephony.ACTION_CHANGE_DEFAULT");
        i.setPackage("com.android.settings");
        Bundle bundle = new Bundle();
        bundle.putString("package", "com.android.mms");
        bundle.putBoolean("from_notification", true);
        i.putExtras(bundle);
        PendingIntent pIntent = PendingIntent.getActivity(context, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT);

        String info = context.getString(R.string.click_to_change_to_xms_app)
                + "\n"
                + context.getString(R.string.click_to_change_to_xms_app_line2);

        Log.i(TAG, "onReceive1\n");

        Notification bigtextNoti = new Notification.Builder(context)
                .setAutoCancel(true).setContentIntent(pIntent)
                .setContentTitle(context.getString(R.string.smsmms_app_name))
                .setContentText(context.getString(R.string.click_to_change_to_xms_app))
                .setDefaults(Notification.DEFAULT_ALL)
                .setSmallIcon(R.drawable.smsmms)
                .setTicker(context.getString(R.string.smsmms_app_name))
                .setStyle(new Notification.BigTextStyle().bigText(info))
                .build();
        Log.i(TAG, "onReceive2\n");

        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, bigtextNoti);

        Log.i(TAG, "onReceive3\n");
    }


}
