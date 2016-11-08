package com.mediatek.smartmotion.enabler;

import android.content.Context;
import android.util.Log;
import android.widget.Switch;

import com.mediatek.sensorhub.ContextInfo;
import com.mediatek.smartmotion.sensor.SensorHubClient;

public class SmartSilentEnabler extends SmartMotionEnabler {
    private static SmartSilentEnabler sSmartSilentEnabler;

    public SmartSilentEnabler(Context context) {
        super(context);
        mIsChecked = mPreferences.getSmartSilent();
    }

    public synchronized static void registerSwitch(Context context, Switch switch_) {
        if (context == null || switch_ == null) {
            return;
        }

        if (sSmartSilentEnabler == null) {
            sSmartSilentEnabler = new SmartSilentEnabler(context);
            IncomingCallListener.getInstance().registerListener(sSmartSilentEnabler);
        }
        sSmartSilentEnabler.addSwitch(switch_);
    }

    public synchronized static void unregisterSwitch(Switch switch_) {
        if (sSmartSilentEnabler == null) {
            return;
        }
        sSmartSilentEnabler.removeSwitch(switch_);
    }

    public synchronized static void unregisterAllSwitches() {
        if (sSmartSilentEnabler == null) {
            return;
        }
        sSmartSilentEnabler.removeAllSwitches();
    }

    @Override
    protected void setPreference() {
        mPreferences.setSmartSilent(mIsChecked);
    }

    @Override
    protected void enableSensor() {
        mRequestId = sSensorHubClient.addRequest(ContextInfo.Type.FACING);
        Log.w(TAG, "add request for SmartSilent: " + mRequestId);
    }
}
