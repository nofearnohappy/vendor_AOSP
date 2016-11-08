package com.hesine.nmsg.observer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.hesine.nmsg.business.bo.Activation;
import com.hesine.nmsg.business.bo.Deactivation;
import com.hesine.nmsg.business.dao.Config;
import com.hesine.nmsg.common.CommonUtils;
import com.hesine.nmsg.common.MLog;
import com.hesine.nmsg.thirdparty.Location;
import com.hesine.nmsg.thirdparty.PNControler;
import com.hesine.nmsg.thirdparty.PNMessageHandler;

public class NetworkChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetInfo = cm.getActiveNetworkInfo();
            if (activeNetInfo != null && activeNetInfo.isConnected()) {
                if (!Config.getIsActivated()) {
                    Activation.instance().start();
                } else {
                    String deactivationIp = Config.getDeactivatationIp();
                    if (deactivationIp != null && !deactivationIp.equals("")) {
                        Deactivation.instance().start();
                    }
                    if (!Config.getUploadPNTokenFlag()) {
                        PNControler.startPN(context);
                    }
                    Location.getInstance().checkNeedRequestLocation();
                }
                if (ConnectivityManager.TYPE_WIFI == activeNetInfo.getType()) {
                    Config.saveWifiConnected(true);
                    MLog.info("NetworkChangeReceiver,wifiConnected,get msg from server");
                    CommonUtils.procRequestLatestWifiMsg();
                } else {
                    Config.saveWifiConnected(false);
                    if (!Config.getIsWifiChecked()) {
                        MLog.info("NetworkChangeReceiver,gprsConncted,get msg from server");
                        CommonUtils.procRequestLatestWifiMsg();
                    }
                }
                PNMessageHandler.procRequestFailedMsgs();
            } else {
                MLog.info("NetworkChangeReceiver,disconnect from server");
                Config.saveWifiConnected(false);
            }
        }
    }
}
