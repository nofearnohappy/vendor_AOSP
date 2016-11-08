package com.mediatek.nlpservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootUpReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        log("[receiver] onReceive BOOT_COMPLETED");
        Intent in = new Intent(context, NlpService.class);
        context.startService(in);
    }

    public void log(Object msg) {
        Log.d("nlp_service", "" + msg);
    }
}
