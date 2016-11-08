package com.mediatek.sensorhub.settings;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.mediatek.sensorhub.settings.R;

public class Utils {

    private static final String TAG = "SensorHubUtils";
    private Context mContext;
    private SensorManager mSensorManager = null;
    private StorageManager mStorageManager;
    private String mExternalStoragePath0;
    private String mExternalStoragePath1;

    private final SensorEventListener mSensorEventListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // this method is called very rarely, so we don't have to
            // limit our updates as we do in onSensorChanged(...)
        }

        public void onSensorChanged(final SensorEvent event) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    String sensorData = "SensorEventListener.onSensorChanged time is " + getTimeString()
                            + " Sensor is " + event.sensor.getName() + "  accuracy is " + event.accuracy
                            + " timestamp is " + event.timestamp + getValules(event.values) + "\n";
                    recordToSdcard(sensorData.getBytes(), 0);
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                }
            }.execute();
        }

        private String getValules(float[] values) {
            String valueString = "";
            for (int i = 0; i < values.length; i++) {
                valueString += " value[" + i + "] is " + values[i];
            }
            return valueString;
        }
    };

    public Utils(Context context, SensorManager sensorManager) {
        mContext = context;
        mSensorManager = sensorManager;
        mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
    }

    public boolean registerSensor(Sensor sensor) {
        boolean result = mSensorManager.registerListener(mSensorEventListener, 
                sensor, SensorManager.SENSOR_DELAY_UI, 0);

        int resId = result ? R.string.register_done : R.string.register_error;
        String msg = String.format(mContext.getResources().getString(resId), 
                sensor.getName());
        Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();

        return result;
    }

    public void unRegisterSensor(Sensor sensor) {
        mSensorManager.unregisterListener(mSensorEventListener, sensor);
    }

    public void unRegisterAllSensors() {
        mSensorManager.unregisterListener(mSensorEventListener);
    }

    public void initRecordFileName(int recordType) {
        String sensorTestDirStr = mStorageManager.getPrimaryVolume().getPath() + "/MtkSensorTest/";
        File sensorTestDir = new File(sensorTestDirStr);
        boolean makeDir = false;
        if (!sensorTestDir.isDirectory()) {
            makeDir = sensorTestDir.mkdirs();
        }
        if (recordType == 0) {
            mExternalStoragePath0 = sensorTestDirStr + "register_sensor_test_" + getTimeString()
                    + ".txt";
            Log.d(TAG, "mExternalStoragePath0: " + mExternalStoragePath0 + " makeDir: " + makeDir);
        } else {
            mExternalStoragePath1 = sensorTestDirStr + "sensor_test_" + getTimeString() + ".txt";
            Log.d(TAG, "mExternalStoragePath1: " + mExternalStoragePath1 + " makeDir: " + makeDir);
        }
    }

    public void recordToSdcard(byte[] data, int recordType) {
        FileOutputStream fos = null;
        try {
            if (recordType == 0) {
                fos = new FileOutputStream(mExternalStoragePath0, true);
            } else {
                fos = new FileOutputStream(mExternalStoragePath1, true);
            }
            fos.write(data);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.w(TAG, "FileNotFoundException " + e.getMessage());
            return;
        } catch (IOException e) {
            Log.w(TAG, "IOException " + e.getMessage());
            return;
        } finally {
            try {
                if (fos != null) {
                    fos.flush();
                    fos.close();
                }
            } catch (IOException e2) {
                Log.w(TAG, "IOException " + e2.getMessage());
                return;
            }
        }
    }

    public String getTimeString() {
        return DateFormat.format("yyyy-MM-dd-kk-mm-ss", System.currentTimeMillis()).toString();
    }
}
