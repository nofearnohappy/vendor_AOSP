package com.mediatek.smartmotion.enabler;

import android.content.Context;
import android.util.Log;
import android.widget.Switch;

import com.mediatek.sensorhub.ContextInfo;
import com.mediatek.smartmotion.sensor.SensorHubClient;

public class QuickAnswerEnabler extends SmartMotionEnabler {
    private static QuickAnswerEnabler sQuickAnswerEnabler;

    public QuickAnswerEnabler(Context context) {
        super(context);
        mIsChecked = mPreferences.getQuickAnswer();
    }

    public synchronized static void registerSwitch(Context context, Switch switch_) {
        if (context == null || switch_ == null) {
            return;
        }

        if (sQuickAnswerEnabler == null) {
            sQuickAnswerEnabler = new QuickAnswerEnabler(context);
            IncomingCallListener.getInstance().registerListener(sQuickAnswerEnabler);
        }
        sQuickAnswerEnabler.addSwitch(switch_);
    }

    public synchronized static void unregisterSwitch(Switch switch_) {
        if (sQuickAnswerEnabler == null) {
            return;
        }
        sQuickAnswerEnabler.removeSwitch(switch_);
    }

    public synchronized static void unregisterAllSwitches() {
        if (sQuickAnswerEnabler == null) {
            return;
        }
        sQuickAnswerEnabler.removeAllSwitches();
    }

    @Override
    protected void setPreference() {
        mPreferences.setQuickAnswer(mIsChecked);
    }

    @Override
    protected void enableSensor() {
        mRequestId = sSensorHubClient.addRequest(ContextInfo.Type.PICK_UP);
        Log.w(TAG, "add request for QuickAnswer: " + mRequestId);
    }
}
