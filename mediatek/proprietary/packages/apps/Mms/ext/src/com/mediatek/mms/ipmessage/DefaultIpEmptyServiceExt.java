package com.mediatek.mms.ipmessage;

import android.content.Context;
import android.content.Intent;
import android.app.Service;
import android.os.IBinder;


public class DefaultIpEmptyServiceExt implements IIpEmptyServiceExt {
    public void onCreate(Service service) {

    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        return 0;
    }
    public void onDestroy() {

    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onLowMemory() {

    }
    public void onRebind(Intent intent) {

    }
    public boolean onUnbind(Intent intent) {
        return false;
    }
}
