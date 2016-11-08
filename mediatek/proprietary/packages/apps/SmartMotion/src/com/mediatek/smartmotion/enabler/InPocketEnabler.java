package com.mediatek.smartmotion.enabler;

import android.content.Context;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.mediatek.sensorhub.ContextInfo;
import com.mediatek.sensorhub.SensorHubManager;
import com.mediatek.smartmotion.sensor.SensorHubClient;

public class InPocketEnabler extends SmartMotionEnabler {
    private static InPocketEnabler sInPocketEnabler;

    private InPocketEnabler(Context context) {
        super(context);
        mIsChecked = mPreferences.getInPocket();
    }

    public synchronized static void registerSwitch(Context context, Switch switch_) {
        if (context == null || switch_ == null) {
            return;
        }

        if (sInPocketEnabler == null) {
            sInPocketEnabler = new InPocketEnabler(context);
            //IncomingCallListener.getInstance().registerListener(sInPocketEnabler);
        }
        sInPocketEnabler.addSwitch(switch_);
    }

    public synchronized static void unregisterSwitch(Switch switch_) {
        if (sInPocketEnabler == null) {
            return;
        }
        sInPocketEnabler.removeSwitch(switch_);
    }

    public synchronized static void unregisterAllSwitches() {
        if (sInPocketEnabler == null) {
            return;
        }
        sInPocketEnabler.removeAllSwitches();
    }

    @Override
    protected void setPreference() {
        mPreferences.setInPocket(mIsChecked);
    }

    @Override
    protected void enableSensor() {
        mRequestId = sSensorHubClient.addRequest(ContextInfo.Type.CARRY);
        Log.w(TAG, "add request for InPocket: " + mRequestId);
    }
}
