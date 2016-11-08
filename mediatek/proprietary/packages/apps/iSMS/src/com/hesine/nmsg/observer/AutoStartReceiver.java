package com.hesine.nmsg.observer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.hesine.nmsg.business.dao.Config;
import com.hesine.nmsg.common.MLog;
import com.hesine.nmsg.thirdparty.PNControler;

public class AutoStartReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!NmsgService.getInstance().isServiceStart()) {
            MLog.info("receive action:" + intent.getAction());
            Intent i = new Intent(context, NmsgService.class);
            context.startService(i);
        }
        if (Config.getIsActivated()) {
            if (Config.getUploadPNTokenFlag()) {
                PNControler.startPNService(context);
            } else {
                PNControler.startPN(context);
            }
        }
    }
}
