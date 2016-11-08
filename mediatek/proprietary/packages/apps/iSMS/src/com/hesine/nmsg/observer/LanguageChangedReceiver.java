package com.hesine.nmsg.observer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.hesine.nmsg.business.bo.SendSystemInfo;
import com.hesine.nmsg.business.dao.Config;
import com.hesine.nmsg.common.DeviceInfo;
import com.hesine.nmsg.common.GlobalData;

public class LanguageChangedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_LOCALE_CHANGED)) {
            GlobalData.instance().getSystemInfo().setPnToken(Config.getPnToken());
            GlobalData.instance().getSystemInfo().setLanguage(DeviceInfo.getLanuage(context));
            SendSystemInfo.updateSystemInfo(null);
        }
    }

}