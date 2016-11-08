package com.mediatek.usbchecker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class UsbCheckerReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        intent.setClass(context, UsbCheckerService.class);
        context.startService(intent);
    }
}
