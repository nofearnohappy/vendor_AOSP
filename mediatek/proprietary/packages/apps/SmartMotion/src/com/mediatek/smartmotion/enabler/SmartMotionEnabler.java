package com.mediatek.smartmotion.enabler;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.StaticLayout;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.mediatek.smartmotion.MainActivity;
import com.mediatek.smartmotion.sensor.SensorHubClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class SmartMotionEnabler implements CompoundButton.OnCheckedChangeListener,
        IncomingCallListener.Callback {
    protected final static String TAG = "SmartMotionEnabler";
    protected Preferences mPreferences;
    protected List<Switch> mSwitches;
    protected Context mContext;

    protected boolean mIsChecked;
    protected boolean mIsRing;

    protected static SensorHubClient sSensorHubClient;
    protected int mRequestId = -1;

    private boolean mIsSensorEnabled;

    private static final Set<SmartMotionEnabler> sEnablerSet = new HashSet<SmartMotionEnabler>();

    protected SmartMotionEnabler(Context context) {
        mContext = context;
        mPreferences = Preferences.getPreferences(mContext);
        mSwitches = new ArrayList<Switch>();
        if (sSensorHubClient == null) {
            sSensorHubClient = new SensorHubClient(mContext);
        }
        sEnablerSet.add(this);
    }

    public static void resetAllEnabler() {
        Log.i(TAG, "resetAllEnabler");
        for (SmartMotionEnabler enabler : sEnablerSet) {
            enabler.changeSwitchesState(false);
            enabler.mIsChecked = false;
            enabler.disableSensor();
            enabler.mIsSensorEnabled = false;
        }
    }

    public static void disableAllSensors() {
        Log.i(TAG, "disable all sensors");
        for (SmartMotionEnabler enabler : sEnablerSet) {
            enabler.disableSensor();
            enabler.mIsSensorEnabled = false;
        }
    }

    protected void addSwitch(Switch switch_) {
        switch_.setChecked(mIsChecked);
        if (!MainActivity.sDemoMode) {
            setSensor();
        }
        mSwitches.add(switch_);
        switch_.setOnCheckedChangeListener(this);
    }

    protected void removeSwitch(Switch switch_) {
        switch_.setOnCheckedChangeListener(null);
        mSwitches.remove(switch_);
//        disableSensor();
    }

    protected void changeSwitchesState(boolean isChecked) {
        Log.i(TAG, "changeSwitchesState, mSwitches:" + mSwitches.size());
        for (Switch switch_ : mSwitches) {
            switch_.setChecked(isChecked);
        }
    }

    protected void removeAllSwitches() {
        for (Switch switch_ : mSwitches) {
            switch_.setOnCheckedChangeListener(null);
        }
        mSwitches.clear();
    }

    @Override
    public void onCallStateChanged(int state) {
        Log.i(TAG, "onCallStateChanged:" + state);
        switch (state) {
        case TelephonyManager.CALL_STATE_IDLE:
        case TelephonyManager.CALL_STATE_OFFHOOK:
            mIsRing = false;
            break;
        case TelephonyManager.CALL_STATE_RINGING:
            mIsRing = true;
            break;
        }

        if (MainActivity.sDemoMode) {
            setSensor();
        }
    }

    private void setSensor() {
        if (MainActivity.sDemoMode) {
            if (mIsChecked) {
                if (mIsRing) {
                    Log.i(TAG, "enableSensor...");
                    mIsSensorEnabled = true;
                    enableSensor();
                } else if (mIsSensorEnabled) {
                    Log.i(TAG, "disableSensor...");
                    mIsSensorEnabled = false;
                    disableSensor();
                }
            }
        } else {
            if (mIsChecked && !mIsSensorEnabled) {
                Log.i(TAG, "enableSensor...");
                mIsSensorEnabled = true;
                enableSensor();
            } else if (mIsSensorEnabled && !mIsChecked) {
                Log.i(TAG, "disableSensor...");
                mIsSensorEnabled = false;
                disableSensor();
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mIsChecked == isChecked) {
            return;
        }

        mIsChecked = isChecked;
        setPreference();
        changeSwitchesState(isChecked);

        setSensor();
    }

    protected abstract void setPreference();
    protected abstract void enableSensor();

    protected void disableSensor() {
        sSensorHubClient.cancelRequest(mRequestId);
    }
}
