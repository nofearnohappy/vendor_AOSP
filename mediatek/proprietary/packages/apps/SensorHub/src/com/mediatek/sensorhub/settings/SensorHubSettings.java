package com.mediatek.sensorhub.settings;

import java.util.List;
import java.util.Iterator;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;
import android.widget.Toast;

import com.mediatek.sensor.SensorPlus;
import com.mediatek.sensorhub.Action;
import com.mediatek.sensorhub.ActionDataResult;
import com.mediatek.sensorhub.Condition;
import com.mediatek.sensorhub.ContextInfo;
import com.mediatek.sensorhub.DataCell;
import com.mediatek.sensorhub.SensorHubManager;

import com.mediatek.sensorhub.settings.R;

public class SensorHubSettings extends PreferenceActivity {
    private static final String TAG = "SensorHubSettings";
    private static final String KEY_GESTURE_WAKE_UP = "gesture_wake_up";
    private static final String PROPERTY_SENSORHUB_SUPPORT = "ro.mtk_sensorhub_support";
    public  static final String KEY_SENSOR_PREFERENCE_KEY = "sensor_preference_key";

    private SensorManager mSensorManager;
    private SensorHubManager mSensorHubManager;
    private int mRequestId;
    private Utils mUtils;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean isSensorHubSupport = SystemProperties.getBoolean(PROPERTY_SENSORHUB_SUPPORT, false);
        if (!isSensorHubSupport) {
           Log.d(TAG, "SensorHub not support, exit");
           Toast.makeText(this, R.string.sensor_hub_error, Toast.LENGTH_LONG).show();
           return;
        }
        addPreferencesFromResource(R.xml.sensorhub_settings);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorHubManager = (SensorHubManager) getSystemService(SensorHubManager.SENSORHUB_SERVICE);
        mUtils = new Utils(this, mSensorManager);
        mUtils.initRecordFileName(0);

        addSensorsList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SensorActivity.acquireScreenWakeLock(this);

        Intent intent = getIntent();
        if (ActionDataResult.hasResult(intent)) {
            ActionDataResult result = ActionDataResult.extractResult(intent);
            mRequestId = result.getRequestId();

            List<DataCell> datalist = result.getData();
            int dataSize = datalist.size();
            Log.d(TAG, "onResume: dataSize=" + dataSize);
            for (int i = 0; i < dataSize; i++) {
                DataCell data = datalist.get(i);
            }
        }
    }

    @Override
    protected void onPause() {
        SensorActivity.releaseScreenWakeLock();
        super.onPause();
    }

    private void addSensorsList() {
        ///Add GestureWake when support.
        if (mSensorHubManager.isContextSupported(ContextInfo.Type.GESTURE)) {
            CheckBoxPreference gestureWakePref = new CheckBoxPreference(this);
            gestureWakePref.setKey(KEY_GESTURE_WAKE_UP);
            gestureWakePref.setTitle(R.string.gesture_wake_up);
            getPreferenceScreen().addPreference(gestureWakePref); 
        }

        List<Sensor> sensorList = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        if (sensorList != null && sensorList.size() != 0) {
            for (Sensor sensor : sensorList) {
                if (sensor != null) {
                    int sensorType = sensor.getType();
                    if (sensorType == Sensor.TYPE_ACCELEROMETER
                            || sensorType == Sensor.TYPE_LIGHT
                            || sensorType == Sensor.TYPE_PROXIMITY) {
                        CheckBoxPreference sensorCheckPref = new CheckBoxPreference(this);
                        sensorCheckPref.setKey(String.valueOf(sensorType));
                        sensorCheckPref.setTitle(sensor.getName());
                        getPreferenceScreen().addPreference(sensorCheckPref);
                        sensorCheckPref.setChecked(false);
                        Log.d(TAG, "[TYPE1]Add Sensors : " + sensor.getName());
                    } else if (sensorType == SensorPlus.TYPE_CARRY
                            || sensorType == SensorPlus.TYPE_FACING
                            || sensorType == SensorPlus.TYPE_PEDOMETER
                            || sensorType == SensorPlus.TYPE_PICK_UP
                            || sensorType == SensorPlus.TYPE_SHAKE
                            || sensorType == SensorPlus.TYPE_USER_ACTIVITY
                            || sensorType == Sensor.TYPE_SIGNIFICANT_MOTION) {
                        Preference sensorPref = new Preference(this);
                        sensorPref.setKey(String.valueOf(sensorType));
                        sensorPref.setTitle(sensor.getName());
                        getPreferenceScreen().addPreference(sensorPref);
                        Log.d(TAG, "[TYPE2]Add Sensors : " + sensor.getName());
                    }
                }
            }
        }
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference instanceof CheckBoxPreference) {
            CheckBoxPreference pref = (CheckBoxPreference) preference;
            if (KEY_GESTURE_WAKE_UP.equals(preference.getKey())) {
                if (pref.isChecked()) {
                    mSensorHubManager.enableGestureWakeup(true);
                    setupAction();
                } else {
                    cancelAction(mRequestId);
                    mSensorHubManager.enableGestureWakeup(false);
                }
            } else {
                Sensor sensor = mSensorManager.getDefaultSensor(Integer
                        .valueOf(preference.getKey()));
                if (pref.isChecked()) {
                    mUtils.registerSensor(sensor);
                } else {
                    mUtils.unRegisterSensor(sensor);
                }
            }
        } else if (preference instanceof Preference) {
            Intent intent = new Intent();
            intent.setClass(this, SensorActivity.class);
            final Bundle bundle = new Bundle();
            bundle.putString(KEY_SENSOR_PREFERENCE_KEY, preference.getKey());
            intent.putExtras(bundle);
            startActivity(intent);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    protected void onDestroy() {
        mUtils.unRegisterAllSensors();
        super.onDestroy();
    }

    private void setupAction() {
        Intent intent = new Intent(this, SensorHubSettings.class);
        PendingIntent callbackIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Action action = new Action(callbackIntent, true, false);
        Condition.Builder builder = new Condition.Builder();
        Condition condition = builder.createCondition(ContextInfo.Gesture.VALUE,
                Condition.OP_EQUALS, ContextInfo.Gesture.Type.GES_SLIDE_RIGHT);
        mRequestId = mSensorHubManager.requestAction(condition, action);
        Log.d(TAG, "requestGesture<<< requestid=" + mRequestId);
    }

    private void cancelAction(int requestId) {
        if (mRequestId != 0) {
            if (mSensorHubManager.cancelAction(requestId)) {
                Log.e(TAG, "Cancel request success with id " + requestId);
            } else {
                Log.e(TAG, "Cancel request fail  with id " + requestId);
            }
        }
    }
}
