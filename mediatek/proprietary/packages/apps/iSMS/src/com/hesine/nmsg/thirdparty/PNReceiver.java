package com.hesine.nmsg.thirdparty;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import com.hesine.nmsg.Application;
import com.hesine.nmsg.business.dao.Config;
import com.hesine.nmsg.common.DeviceInfo;
import com.hesine.nmsg.common.GlobalData;
import com.hesine.nmsg.common.MLog;

public class PNReceiver extends BroadcastReceiver {
    public static final String ACTION_REGISTRATION = "com.hpns.android.intent.REGISTRATION";
    public static final String ACTION_RECEIVE = "com.hpns.android.intent.RECEIVE";
    public static final String ACTION_UNREGISTER = "com.hpns.android.intent.UNREGISTER";
    public static final String ACTION_RECONNECT = "com.hpns.android.intent.RECONNECT";
    public static final String ACTION_REG_CHANGE = "com.hpns.android.intent.REGIDCHANGED";

    public static final int HPNS_CODE_SUCCESS = 0;
    private static int nmsgRegisterTimes = 1;

    private Handler mHandler = new Handler();

    private Runnable runnable = new Runnable() {

        @Override
        public void run() {
            PNControler.startPN(Application.getInstance().getApplicationContext());
        }
    };

    public void onReceive(Context context, Intent intent) {
        String receiveAction = intent.getAction();
        MLog.info("onReceive action: " + receiveAction);
        if (receiveAction.equals(ACTION_REGISTRATION)) {
            handleRegistration(context, intent);
        } else if (receiveAction.equals(ACTION_UNREGISTER)) {
            handleUnRegistration(context, intent);
        } else if (receiveAction.equals(ACTION_RECEIVE)) {
            handleNewMessage(context, intent);
        } else if (receiveAction.equals(ACTION_RECONNECT)) {
            handleReconnect(context, intent);
        } else if (receiveAction.equals(ACTION_REG_CHANGE)) {
            handleRegistration(context, intent);
        }
    }

    private void handleRegistration(Context context, Intent intent) {
        String regId = intent.getStringExtra("registration_id");
        int code = intent.getIntExtra("code", 0);
        MLog.info("handleRegistration regId: " + regId + "code: " + code);
        if (HPNS_CODE_SUCCESS == code && regId != null && regId.length() > 0) {
            nmsgRegisterTimes = 1;
            String oldRegId = Config.getPnToken();
            if (oldRegId == null || (oldRegId != null && (!oldRegId.equals(regId)))
                    || !Config.getUploadPNTokenFlag()) {
                Config.savePnToken(regId);
                Config.saveUploadPNTokenFlag(false);
                GlobalData.instance().getSystemInfo().setPnToken(regId);
                PNControler.postPNToken();
            }
        } else {
            MLog.error("fatal error in HPNS for handleRegitration is failed, code:" + code
                    + " times:" + nmsgRegisterTimes);
            if (DeviceInfo.isNetworkReady(context)) {
                mHandler.postDelayed(runnable, 60 * 1000 * nmsgRegisterTimes);
                nmsgRegisterTimes = nmsgRegisterTimes * 2;
            } else {
                MLog.error("Network Not Available");
            }
        }
    }

    private void handleUnRegistration(Context context, Intent intent) {
        Config.savePnToken("");
        Config.saveUploadPNTokenFlag(false);
        GlobalData.instance().getSystemInfo().setPnToken("");
    }

    private void handleNewMessage(Context context, Intent intent) {
        String message = intent.getStringExtra("message");
        MLog.info("handleNewMessage message: " + message);
        PNControler.handlePNCommand(message);
    }

    private void handleReconnect(Context context, Intent intent) {
        PNControler.startPN(context);
    }

}