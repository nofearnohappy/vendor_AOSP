package com.hesine.nmsg.observer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import com.hesine.nmsg.business.bo.Activation;
import com.hesine.nmsg.business.bo.Deactivation;
import com.hesine.nmsg.business.bo.SendSystemInfo;
import com.hesine.nmsg.business.dao.Config;
import com.hesine.nmsg.common.DeviceInfo;
import com.hesine.nmsg.common.GlobalData;

public class SimStateChangedReceiver extends BroadcastReceiver {
    public static final String ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";
    private static final int SIM_VALID = 0;
    private static final int SIM_INVALID = 1;
    private int simState = SIM_INVALID;
    public static SimStateChangedReceiver mInstance = null;

    public static SimStateChangedReceiver getInstance() {
        if (mInstance == null) {
            mInstance = new SimStateChangedReceiver();
        }
        return mInstance;
    }

    public int getSimState() {
        return simState;
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_SIM_STATE_CHANGED)) {
            TelephonyManager tm = (TelephonyManager) context
                    .getSystemService(Service.TELEPHONY_SERVICE);
            int state = tm.getSimState();
            switch (state) {
                case TelephonyManager.SIM_STATE_READY:
                    simState = SIM_VALID;
                    String imsi = DeviceInfo.getIMSI(context);
                    GlobalData.instance().getSystemInfo().setImsi(imsi);
                    Config.saveImsi(GlobalData.instance().getSystemInfo().getImsi());
                    if (!Config.getIsActivated()) {
                        Activation.instance().start();
                    } else {
                        String deactivationIp = Config.getDeactivatationIp();
                        if (deactivationIp != null && !deactivationIp.equals("")) {
                            Deactivation.instance().start();
                        }
                        SendSystemInfo.updateSystemInfo(null);
                    }
                    break;
                case TelephonyManager.SIM_STATE_UNKNOWN:
                case TelephonyManager.SIM_STATE_ABSENT:
                case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                default:
                    simState = SIM_INVALID;
                    break;
            }
        }
    }

}