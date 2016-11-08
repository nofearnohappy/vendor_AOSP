package com.mediatek.smartmotion.enabler;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEventListener;
import android.hardware.TriggerEvent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.util.Log;
import android.widget.Switch;

import com.mediatek.sensorhub.ContextInfo;
import com.mediatek.sensorhub.SensorHubManager;
import com.mediatek.smartmotion.sensor.SensorHubClient;
import com.mediatek.smartmotion.R;

public class SignificantMotionEnabler extends SmartMotionEnabler {
    private static SignificantMotionEnabler sSignificantMotionEnabler;
    private Context mContext;	
    private SensorManager mSensorManager;
	private Sensor mSensor;
    private SoundPool mSounds;
    private int mSoundId;
    private int mSoundStreamId;

    private final TriggerEventListener mSensorEventListener = new TriggerEventListener() {
        public void onTrigger(TriggerEvent event) {
            Log.d(TAG, "SMD_onTrigger: sensor=" + event.sensor + ",values=" + getValules(event.values));
            playSound();
            if (mIsChecked) {
                boolean result = mSensorManager.requestTriggerSensor(mSensorEventListener, mSensor);
                Log.d(TAG, "SMD_onTrigger: sensor=" + mSensor + ", result=" + result);	
            }
        }

        private String getValules(float[] values) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                sb.append("value[" + i + "]=" + values[i]);
				if (i != values.length - 1) sb.append(" ");
            }
            return sb.toString();
        }
    };

    private final OnLoadCompleteListener mLoadCompleteCallback = new OnLoadCompleteListener() {
        public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
            synchronized(mSounds) {
                Log.d(TAG, "SMD_onLoadComplete: sample=" + sampleId + ",loadStatus=" + status);
                mSounds.notify();
            }
        }
    };

    private SignificantMotionEnabler(Context context) {
        super(context);
        mContext = context;
        mIsChecked = mPreferences.getSignificantMotion();
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);		
    }

    public synchronized static void registerSwitch(Context context, Switch switch_) {
        if (context == null || switch_ == null) {
            return;
        }
        
        if (sSignificantMotionEnabler == null) {
            sSignificantMotionEnabler = new SignificantMotionEnabler(context);
        }
        sSignificantMotionEnabler.addSwitch(switch_);
    }
    
    public synchronized static void unregisterSwitch(Switch switch_) {
        if (sSignificantMotionEnabler == null) {
            return;
        }
        sSignificantMotionEnabler.removeSwitch(switch_);
    }
    
    public synchronized static void unregisterAllSwitches() {
        if (sSignificantMotionEnabler == null) {
            return;
        }
        sSignificantMotionEnabler.removeAllSwitches();
    }
    
    @Override
    protected void setPreference() {
        mPreferences.setSignificantMotion(mIsChecked);
    }

    @Override
    protected void enableSensor() {
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);
        boolean result = mSensorManager.requestTriggerSensor(mSensorEventListener, mSensor);
        Log.d(TAG, "SMD_enableSensor: sensor=" + mSensor + ", result=" + result);
    }

    @Override
    protected void disableSensor() {
        boolean result = mSensorManager.cancelTriggerSensor(mSensorEventListener, mSensor);
        Log.d(TAG, "SMD_disableSensor: sensor=" + mSensor + ", result=" + result);	
    }

    private void initSoundPool() {
        mSounds = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        //mSounds.setOnLoadCompleteListener(mLoadCompleteCallback);
        mSoundId = mSounds.load(mContext, R.raw.in_pocket, 0/*priority*/);
        try {
            //synchronized(mSounds) {
            //    mSounds.wait();
            //}
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            Log.e(TAG, "SMD_initSoundPool: InterruptedException", ex);
        }
    }

    private void playSound() {
        Log.i(TAG, "SMD_playSound>>>streamId=" + mSoundStreamId);
        if (mSounds == null) {
            initSoundPool();
        }
        mSounds.stop(mSoundStreamId);
        mSoundStreamId = mSounds.play(mSoundId, 1/*leftVolume*/, 1/*rightVolume*/, 1/* priortiy */, 0/* loop */, 1.0f/* rate */);
        Log.i(TAG, "SMD_playSound<<<streamId=" + mSoundStreamId);
    }
}
