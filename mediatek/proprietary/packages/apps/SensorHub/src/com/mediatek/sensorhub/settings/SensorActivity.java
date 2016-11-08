package com.mediatek.sensorhub.settings;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEventListener;
import android.hardware.TriggerEvent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.mediatek.sensorhub.settings.R;
import com.mediatek.sensor.SensorPlus;

public class SensorActivity extends Activity implements SensorEventListener,
        OnCheckedChangeListener, OnClickListener {
    private static final String TAG = "SensorActivity";

    private static PowerManager.WakeLock sScreenOnWakeLock;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private TriggerListener mListener = new TriggerListener();
    private int mSensorType;

    private TextView[] mTextInfos = new TextView[6];
    private CheckBox mEnableNotifiBox;
    private CheckBox mEnableLog;

    // Pedometer Step clear
    private Button mClearStepBtn;
    private TextView mInitalSteps;
    private TextView mTotalSteps;
    private float mTotalStepCount = 0;
    private float mInitalStepCount = 0;

    // Pedometer Distance clear
    private TextView mInitalDistance;
    private TextView mTotalDistance;
    private float mTotalDistanceValue = 0;
    private float mInitalDistanceValue = 0;


    private String[] mPedometerStrs = { "Step length in meter: ", "Step frequency in Hz: ",
            "Total step count: ", "Total distance in meter: " };
    private String[] mActivityStrs = { "In vehicle: ", "On bike: ", "On foot: ", "Still: ",
            "Unknown: ", "Tilt: " };
    private String[] mActivityTypes = { " 4 ", " 3 ", " 2 ", " 0 ", " -1 ", " 1 " };
    private String[] mInPocketStrs = { "In pocket confidence: " };
    private String[] mPickUpStrs = { "Sensor Pick Up triggers: " };
    private String[] mFacingStrs = { "Sensor Facing triggers: " };
    private String[] mShakeStrs = { "Sensor Shake triggers: " };
    private String[] mSmdStrs = { "Singnificant motion detection: " };
    private String mRecordStr;

    private SoundPool mSounds;
    private int mSoundIdF;
    private int mSoundIdT;
    private int mSoundStreamId;
    private AudioManager mAudioManager;
    private int mUiSoundsStreamType;
    private Utils mUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sensor_activity_view);

        mSensorType = Integer.valueOf(getIntent().getStringExtra(
                SensorHubSettings.KEY_SENSOR_PREFERENCE_KEY));
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(mSensorType);

        mUtils = new Utils(this, mSensorManager);
        updateViewBySensor();

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mSounds = new SoundPool(1, AudioManager.STREAM_NOTIFICATION, 0);
        mSoundIdF = mSounds.load(this, R.raw.in_pocket, 0);
        mSoundIdT = mSounds.load(this, R.raw.non_in_pocket, 0);

        if (mSensorType == Sensor.TYPE_SIGNIFICANT_MOTION
                  || mSensorType == SensorPlus.TYPE_PICK_UP
                  || mSensorType == SensorPlus.TYPE_FACING
                  || mSensorType == SensorPlus.TYPE_SHAKE) {
            // Trigger sensors
            mSensorManager.requestTriggerSensor(mListener, mSensor);
        } else {
            mSensorManager.registerListener((SensorEventListener) this, mSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onDestroy() {
        if (mSensorType == Sensor.TYPE_SIGNIFICANT_MOTION
                || mSensorType == SensorPlus.TYPE_PICK_UP
                || mSensorType == SensorPlus.TYPE_FACING
                || mSensorType == SensorPlus.TYPE_SHAKE) {
            mSensorManager.cancelTriggerSensor(mListener, mSensor);
        } else {
            mSensorManager.unregisterListener(this);
        }
        if (mRecordStr != null && mRecordStr != "") {
            mUtils.initRecordFileName(1);
            mUtils.recordToSdcard(mRecordStr.getBytes(), 1);
        }
        super.onDestroy();
    }

    private void updateViewBySensor() {
        mTextInfos[0] = (TextView) findViewById(R.id.info0);
        mTextInfos[1] = (TextView) findViewById(R.id.info1);
        mTextInfos[2] = (TextView) findViewById(R.id.info2);
        mTextInfos[3] = (TextView) findViewById(R.id.info3);
        mTextInfos[4] = (TextView) findViewById(R.id.info4);
        mTextInfos[5] = (TextView) findViewById(R.id.info5);

        mEnableNotifiBox = (CheckBox) findViewById(R.id.enable_notify);
        if (!(mSensorType == SensorPlus.TYPE_USER_ACTIVITY
               || mSensorType == SensorPlus.TYPE_PEDOMETER)) {
            mEnableNotifiBox.setVisibility(View.VISIBLE);
        }

        mEnableLog = (CheckBox) findViewById(R.id.enable_log);
        mEnableLog.setChecked(false);
        mEnableLog.setOnCheckedChangeListener(this);

        switch (mSensorType) {
        case SensorPlus.TYPE_PEDOMETER:
            for (int i = 0; i < mPedometerStrs.length; i++) {
                mTextInfos[i].setVisibility(View.VISIBLE);
                mTextInfos[i].setText(mPedometerStrs[i]);
            }
            mClearStepBtn = (Button) findViewById(R.id.clear_step);
            mClearStepBtn.setOnClickListener(this);
            mInitalSteps = (TextView) findViewById(R.id.inital_steps);
            mTotalSteps = (TextView) findViewById(R.id.total_steps);
            mInitalDistance = (TextView) findViewById(R.id.inital_distance);
            mTotalDistance = (TextView) findViewById(R.id.total_distance);
            mClearStepBtn.setVisibility(View.VISIBLE);
            mInitalSteps.setVisibility(View.VISIBLE);
            mTotalSteps.setVisibility(View.VISIBLE);
            mInitalDistance.setVisibility(View.VISIBLE);
            mTotalDistance.setVisibility(View.VISIBLE);
            break;
        case SensorPlus.TYPE_USER_ACTIVITY:
            for (int i = 0; i < mActivityStrs.length; i++) {
                mTextInfos[i].setVisibility(View.VISIBLE);
                mTextInfos[i].setText(mActivityStrs[i]);
            }
            break;
        case SensorPlus.TYPE_CARRY:
            for (int i = 0; i < mInPocketStrs.length; i++) {
                mTextInfos[i].setVisibility(View.VISIBLE);
                mTextInfos[i].setText(mInPocketStrs[i]);
            }
            break;
        case SensorPlus.TYPE_PICK_UP:
            for (int i = 0; i < mPickUpStrs.length; i++) {
                mTextInfos[i].setVisibility(View.VISIBLE);
                mTextInfos[i].setText(mPickUpStrs[i]);
            }
            break;
        case SensorPlus.TYPE_FACING:
            for (int i = 0; i < mFacingStrs.length; i++) {
                mTextInfos[i].setVisibility(View.VISIBLE);
                mTextInfos[i].setText(mFacingStrs[i]);
            }
            break;
        case SensorPlus.TYPE_SHAKE:
            for (int i = 0; i < mShakeStrs.length; i++) {
                mTextInfos[i].setVisibility(View.VISIBLE);
                mTextInfos[i].setText(mShakeStrs[i]);
            }
            break;
        case Sensor.TYPE_SIGNIFICANT_MOTION:
            for (int i = 0; i < mSmdStrs.length; i++) {
                mTextInfos[i].setVisibility(View.VISIBLE);
                mTextInfos[i].setText(mSmdStrs[i]);
            }
            break;
        default:
            break;
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void onSensorChanged(SensorEvent event) {
        switch (mSensorType) {
        case SensorPlus.TYPE_PEDOMETER:
            for (int i = 0; i < mPedometerStrs.length; i++) {
                String xString = mPedometerStrs[i] + event.values[i];
                mRecordStr = mRecordStr + "\n" + xString;
                mTextInfos[i].setText(xString);
                if (i == 2) {
                    mTotalStepCount = event.values[i];
                    Log.d(TAG, "onSensorChanged mTotalStepCount = " + mTotalStepCount);
                    float testStep = mTotalStepCount - mInitalStepCount;
                    String totalStep = getString(R.string.total_step_string) + testStep;
                    Log.d(TAG, "onSensorChanged totalStep = " + totalStep);
                    mTotalSteps.setText(totalStep);
                }
                if (i == 3) {
                    mTotalDistanceValue = event.values[i];
                    Log.d(TAG, "onSensorChanged mTotalDistanceValue = " + mTotalDistanceValue);
                    float testDistance = mTotalDistanceValue - mInitalDistanceValue;
                    String totalDistance = getString(R.string.total_distance_string) + testDistance;
                    Log.d(TAG, "onSensorChanged totalDistance = " + totalDistance);
                    mTotalDistance.setText(totalDistance);
                }
            }
            break;
        case SensorPlus.TYPE_USER_ACTIVITY:
            for (int i = 0; i < mActivityStrs.length; i++) {
                String xString = mActivityStrs[i] + event.values[i];
                mTextInfos[i].setText(xString);
            }
            // Time Activity Confidence
            int maxValueIndex = getMaxValueIndex(event.values);
            String recordStr = mUtils.getTimeString() + mActivityTypes[maxValueIndex]
                    + event.values[maxValueIndex];
            mRecordStr = mRecordStr + "\n" + recordStr;
            break;
        case SensorPlus.TYPE_CARRY:
            for (int i = 0; i < mInPocketStrs.length; i++) {
                String xString = mInPocketStrs[i] + event.values[i];
                mRecordStr = mRecordStr + "\n" + xString;
                mTextInfos[i].setText(xString);
            }
            break;

        default:
            break;
        }
        if (mSensorType == SensorPlus.TYPE_CARRY) {
            if (mEnableNotifiBox.isChecked()) {
                Log.d(TAG, "mEnableNotifiBox.isChecked()");
                if (event.values[0] == 1) {
                    playSound(mSoundIdF);
                } else if (event.values[0] == 0) {
                    playSound(mSoundIdT);
                }
            }
        }
        mSensorManager.registerListener((SensorEventListener) this, mSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    private int getMaxValueIndex(float[] values) {
        float tempValue = values[0];
        int index = 0;
        for (int i = 1; i < values.length; i++) {
            if (tempValue < values[i]) {
                tempValue = values[i];
                index = i;
            }
        }
        return index;
    }

    private void playSound(int soundId) {
        mSounds.stop(mSoundStreamId);

        if (mAudioManager != null) {
            mUiSoundsStreamType = mAudioManager.getUiSoundsStreamType();
        }
        // If the stream is muted, don't play the sound
        if (mAudioManager.isStreamMute(mUiSoundsStreamType))
            return;

        mSoundStreamId = mSounds.play(soundId, 1, 1, 1/* priortiy */, 0/* loop */, 1.0f/* rate */);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        CheckBox checkBox = (CheckBox) buttonView;
        if (checkBox.equals(mEnableLog)) {
            if (isChecked) {
                Log.d(TAG, "sensor algorithm test start");
                mUtils.initRecordFileName(1);
                mRecordStr = "";
            } else {
                Log.d(TAG, "sensor algorithm test end");
                if (mRecordStr != null && mRecordStr != "") {
                    mUtils.recordToSdcard(mRecordStr.getBytes(), 1);
                }
            }
        }
    }

    static void acquireScreenWakeLock(Context context) {
        Log.d(TAG, "Acquiring screen on and cpu wake lock");
        if (sScreenOnWakeLock != null) {
            return;
        }

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        sScreenOnWakeLock = pm
                .newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP
                        | PowerManager.ON_AFTER_RELEASE, "SchPwrOnOff");
        sScreenOnWakeLock.acquire();
    }

    static void releaseScreenWakeLock() {
        if (sScreenOnWakeLock != null) {
            sScreenOnWakeLock.release();
            sScreenOnWakeLock = null;
        }
    }

    class TriggerListener extends TriggerEventListener {
        public void onTrigger(TriggerEvent event) {
            Log.d(TAG, "onTrigger mSensorType: " + mSensorType + "onTrigger value: "
                    + event.values[0]);
            switch (mSensorType) {
            case Sensor.TYPE_SIGNIFICANT_MOTION:
                for (int i = 0; i < mSmdStrs.length; i++) {
                    String xString = mSmdStrs[i] + event.values[i];
                    mRecordStr = mRecordStr + "\n" + xString;
                    mTextInfos[i].setText(xString);
                }
                break;
            case SensorPlus.TYPE_PICK_UP:
                for (int i = 0; i < mPickUpStrs.length; i++) {
                    String xString = mPickUpStrs[i] + event.values[i];
                    mRecordStr = mRecordStr + "\n" + xString;
                    mTextInfos[i].setText(xString);
                }
                break;
            case SensorPlus.TYPE_FACING:
                for (int i = 0; i < mFacingStrs.length; i++) {
                    String xString = mFacingStrs[i] + event.values[i];
                    mRecordStr = mRecordStr + "\n" + xString;
                    mTextInfos[i].setText(xString);
                }
                break;
            case SensorPlus.TYPE_SHAKE:
                for (int i = 0; i < mShakeStrs.length; i++) {
                    String xString = mShakeStrs[i] + event.values[i];
                    mRecordStr = mRecordStr + "\n" + xString;
                    mTextInfos[i].setText(xString);
                }
                break;
            default:
                break;
            }
            if (mEnableNotifiBox.isChecked()) {
                Log.d(TAG, "mEnableNotifiBox.isChecked()");
                if (event.values[0] == 1) {
                    playSound(mSoundIdF);
                } else if (event.values[0] == 0) {
                    playSound(mSoundIdT);
                }
            }
            mSensorManager.requestTriggerSensor(mListener, mSensor);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mClearStepBtn) {
            String initalStep = getString(R.string.inital_step_string) + mTotalStepCount;
            Log.d(TAG, "initalStep = " + initalStep);
            mInitalSteps.setText(initalStep);
            mInitalStepCount = mTotalStepCount;
            String initalDistance = getString(R.string.inital_distance_string)
                    + mTotalDistanceValue;
            Log.d(TAG, "initalDistance = " + initalDistance);
            mInitalDistance.setText(initalDistance);
            mInitalDistanceValue = mTotalDistanceValue;
        }
    }
}
