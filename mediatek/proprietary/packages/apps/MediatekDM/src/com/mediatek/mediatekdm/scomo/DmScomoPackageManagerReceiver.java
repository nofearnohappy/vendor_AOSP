package com.mediatek.mediatekdm.scomo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mediatek.mediatekdm.DmService;

public class DmScomoPackageManagerReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        // When there are movements in package repository (package added or removed), tell service
        // to rescan.
        Intent serviceIntent = new Intent();
        serviceIntent.setClass(context, DmService.class);
        serviceIntent.setAction(ScomoComponent.SCOMO_SCAN_PACKAGE);
        context.startService(serviceIntent);
    }
}
