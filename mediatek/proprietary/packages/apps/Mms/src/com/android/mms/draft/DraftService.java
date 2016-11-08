package com.android.mms.draft;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class DraftService extends Service {

    private final static String TAG = "[Mms][Draft][DraftService]";

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        Log.d(TAG, "DraftService onCreate");
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        Log.d(TAG, "DraftService onDestroy stopForeground");
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        // TODO Auto-generated method stub
        Log.d(TAG, "DraftService onStart");
        super.onStart(intent, startId);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        Log.d(TAG, "DraftService onStartCommand");
        Notification notification = new Notification();
        notification.flags |= Notification.FLAG_HIDE_NOTIFICATION;
        startForeground(1, notification);
        return super.onStartCommand(intent, flags, startId);
    }

}
