package com.mediatek.smartmotion.enabler;

import android.content.Context;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.mediatek.sensorhub.ContextInfo;
import com.mediatek.sensorhub.SensorHubManager;
import com.mediatek.smartmotion.sensor.SensorHubClient;

public class UserActivityEnabler extends SmartMotionEnabler {
    private static UserActivityEnabler sUserActivityEnabler;

    private UserActivityEnabler(Context context) {
        super(context);
        mIsChecked = mPreferences.getUserActivity();
    }

    public synchronized static void registerSwitch(Context context, Switch switch_) {
        if (context == null || switch_ == null) {
            return;
        }

        if (sUserActivityEnabler == null) {
            sUserActivityEnabler = new UserActivityEnabler(context);
        }
        sUserActivityEnabler.addSwitch(switch_);
    }

    public synchronized static void unregisterSwitch(Switch switch_) {
        if (sUserActivityEnabler == null) {
            return;
        }
        sUserActivityEnabler.removeSwitch(switch_);
    }

    public synchronized static void unregisterAllSwitches() {
        if (sUserActivityEnabler == null) {
            return;
        }
        sUserActivityEnabler.removeAllSwitches();
    }

    @Override
    protected void setPreference() {
        mPreferences.setUserActivity(mIsChecked);
    }

    @Override
    protected void enableSensor() {
        mRequestId = sSensorHubClient.addRequest(ContextInfo.Type.USER_ACTIVITY);
        Log.w(TAG, "add request for UserActivity: " + mRequestId);
    }
}
