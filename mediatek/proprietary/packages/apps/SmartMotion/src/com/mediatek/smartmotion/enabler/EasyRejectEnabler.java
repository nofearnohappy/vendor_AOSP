package com.mediatek.smartmotion.enabler;

import android.content.Context;
import android.util.Log;
import android.widget.Switch;

import com.mediatek.sensorhub.ContextInfo;
import com.mediatek.sensorhub.SensorHubManager;
import com.mediatek.smartmotion.sensor.SensorHubClient;

public class EasyRejectEnabler extends SmartMotionEnabler {
    private static EasyRejectEnabler sEasyRejectEnabler;

    private EasyRejectEnabler(Context context) {
        super(context);
        mIsChecked = mPreferences.getEasyReject();
    }

    public synchronized static void registerSwitch(Context context, Switch switch_) {
        if (context == null || switch_ == null) {
            return;
        }

        if (sEasyRejectEnabler == null) {
            sEasyRejectEnabler = new EasyRejectEnabler(context);
            IncomingCallListener.getInstance().registerListener(sEasyRejectEnabler);
        }
        sEasyRejectEnabler.addSwitch(switch_);
    }

    public synchronized static void unregisterSwitch(Switch switch_) {
        if (sEasyRejectEnabler == null) {
            return;
        }
        sEasyRejectEnabler.removeSwitch(switch_);
    }

    public synchronized static void unregisterAllSwitches() {
        if (sEasyRejectEnabler == null) {
            return;
        }
        sEasyRejectEnabler.removeAllSwitches();
    }

    @Override
    protected void setPreference() {
        mPreferences.setEasyReject(mIsChecked);
    }

    @Override
    protected void enableSensor() {
     //   mRequestId = mSensorHubClient.addRequest(SensorHubClient.TYPE_EASYREJECT);
        mRequestId = sSensorHubClient.addRequest(ContextInfo.Type.SHAKE);
        Log.w(TAG, "add request for EasyReject: " + mRequestId);
    }
}
