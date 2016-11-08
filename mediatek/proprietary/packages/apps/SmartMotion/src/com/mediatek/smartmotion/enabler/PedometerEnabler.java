package com.mediatek.smartmotion.enabler;

import android.content.Context;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.mediatek.sensorhub.ContextInfo;
import com.mediatek.sensorhub.SensorHubManager;
import com.mediatek.smartmotion.sensor.SensorHubClient;

public class PedometerEnabler extends SmartMotionEnabler {
    private static PedometerEnabler sPedometerEnabler;

    private PedometerEnabler(Context context) {
        super(context);
        mIsChecked = mPreferences.getPedometer();
    }

    public synchronized static void registerSwitch(Context context, Switch switch_) {
        if (context == null || switch_ == null) {
            return;
        }

        if (sPedometerEnabler == null) {
            sPedometerEnabler = new PedometerEnabler(context);
        }
        sPedometerEnabler.addSwitch(switch_);
    }

    public synchronized static void unregisterSwitch(Switch switch_) {
        if (sPedometerEnabler == null) {
            return;
        }
        sPedometerEnabler.removeSwitch(switch_);
    }

    public synchronized static void unregisterAllSwitches() {
        if (sPedometerEnabler == null) {
            return;
        }
        sPedometerEnabler.removeAllSwitches();
    }

    @Override
    protected void setPreference() {
        mPreferences.setPedometer(mIsChecked);
    }

    @Override
    protected void enableSensor() {
        mRequestId = sSensorHubClient.addRequest(ContextInfo.Type.PEDOMETER);
        Log.w(TAG, "add request for Pedometer: " + mRequestId);
    }
}
