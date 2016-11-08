package com.mediatek.aaltool;

import android.os.Bundle;
import android.provider.Settings;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Button;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.Context;
import android.content.Intent;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Map;

public class AALTuning extends Activity implements OnSeekBarChangeListener {
    private static final String TAG = "AALTool";
    private static final String PREFS_NAME = "aal";
    private static final String FILE_NAME = "aal.cfg";

    private static final int MAX_BRIGHTNESS_VALUE = 255;

    private static final int[][] ALI2BLI = {
        { // Level = 0
        0, 128, 256, 384, 512, 640, 768, 896, 1024, 2048, 4096, 6144, 8192, 10240, 12288, 14336, 16384, 18432,  // ALI
        8, 167, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255 // BLI
        },
        { // Level = 16
        0, 128, 256, 384, 512, 640, 768, 896, 1024, 2048, 4096, 6144, 8192, 10240, 12288, 14336, 16384, 18432,  // ALI
        8, 139, 218, 232, 246, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255 // BLI
        },
        { // Level = 32
        0, 128, 256, 384, 512, 640, 768, 896, 1024, 2048, 4096, 6144, 8192, 10240, 12288, 14336, 16384, 18432,  // ALI
        8, 118, 185, 197, 208, 220, 233, 246, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255 // BLI
        },
        { // Level = 48
        0, 128, 256, 384, 512, 640, 768, 896, 1024, 2048, 4096, 6144, 8192, 10240, 12288, 14336, 16384, 18432,  // ALI
        8, 103, 161, 171, 181, 191, 202, 212, 222, 255, 255, 255, 255, 255, 255, 255, 255, 255 // BLI
        },
        { // Level = 64
        0, 128, 256, 384, 512, 640, 768, 896, 1024, 2048, 4096, 6144, 8192, 10240, 12288, 14336, 16384, 18432,  // ALI
        8, 92, 143, 152, 160, 169, 178, 187, 196, 255, 255, 255, 255, 255, 255, 255, 255, 255 // BLI
        },
        { // Level = 80
        0, 128, 256, 384, 512, 640, 768, 896, 1024, 2048, 4096, 6144, 8192, 10240, 12288, 14336, 16384, 18432,  // ALI
        8, 83, 129, 136, 144, 151, 160, 168, 176, 239, 255, 255, 255, 255, 255, 255, 255, 255 // BLI
        },
        { // Level = 96
        0, 128, 256, 384, 512, 640, 768, 896, 1024, 2048, 4096, 6144, 8192, 10240, 12288, 14336, 16384, 18432,  // ALI
        8, 76, 117, 124, 131, 138, 145, 153, 160, 217, 255, 255, 255, 255, 255, 255, 255, 255 // BLI
        },
        { // Level = 112
        0, 128, 256, 384, 512, 640, 768, 896, 1024, 2048, 4096, 6144, 8192, 10240, 12288, 14336, 16384, 18432,  // ALI
        8, 70, 107, 114, 120, 126, 133, 139, 146, 198, 255, 255, 255, 255, 255, 255, 255, 255 // BLI
        },
        { // Level = 128 
        0, 128, 256, 384, 512, 640, 768, 896, 1024, 2048, 4096, 6144, 8192, 10240, 12288, 14336, 16384, 18432,  // ALI
        8, 64, 99, 105, 111, 116, 122, 128, 135, 183, 255, 255, 255, 255, 255, 255, 255, 255 // BLI
        },
        { // Level = 144
        0, 128, 256, 384, 512, 640, 768, 896, 1024, 2048, 4096, 6144, 8192, 10240, 12288, 14336, 16384, 18432,  // ALI
        8, 60, 92, 97, 102, 108, 113, 119, 124, 169, 255, 255, 255, 255, 255, 255, 255, 255 // BLI
        },
        { // Level = 160
        0, 128, 256, 384, 512, 640, 768, 896, 1024, 2048, 4096, 6144, 8192, 10240, 12288, 14336, 16384, 18432,  // ALI
        8, 56, 86, 91, 96, 101, 106, 111, 116, 158, 243, 255, 255, 255, 255, 255, 255, 255 // BLI
        },
        { // Level = 176 
        0, 128, 256, 384, 512, 640, 768, 896, 1024, 2048, 4096, 6144, 8192, 10240, 12288, 14336, 16384, 18432,  // ALI
        8, 53, 80, 85, 90, 94, 99, 104, 109, 148, 227, 255, 255, 255, 255, 255, 255, 255 // BLI
        },
        { // Level = 192
        0, 128, 256, 384, 512, 640, 768, 896, 1024, 2048, 4096, 6144, 8192, 10240, 12288, 14336, 16384, 18432,  // ALI
        8, 50, 76, 80, 85, 89, 94, 98, 102, 139, 214, 255, 255, 255, 255, 255, 255, 255 // BLI
        },
        { // Level = 208
        0, 128, 256, 384, 512, 640, 768, 896, 1024, 2048, 4096, 6144, 8192, 10240, 12288, 14336, 16384, 18432,  // ALI
        8, 48, 72, 76, 80, 84, 88, 92, 97, 131, 202, 255, 255, 255, 255, 255, 255, 255 // BLI
        },
        { // Level = 224
        0, 128, 256, 384, 512, 640, 768, 896, 1024, 2048, 4096, 6144, 8192, 10240, 12288, 14336, 16384, 18432,  // ALI
        8, 45, 68, 72, 76, 80, 84, 88, 92, 124, 191, 255, 255, 255, 255, 255, 255, 255 // BLI
        },
        { // Level = 240
        0, 128, 256, 384, 512, 640, 768, 896, 1024, 2048, 4096, 6144, 8192, 10240, 12288, 14336, 16384, 18432,  // ALI
        8, 43, 65, 68, 72, 76, 79, 83, 87, 117, 181, 245, 255, 255, 255, 255, 255, 255 // BLI
        },
        { // Level = 255
        0, 128, 256, 384, 512, 640, 768, 896, 1024, 2048, 4096, 6144, 8192, 10240, 12288, 14336, 16384, 18432,  // ALI
        8, 42, 62, 65, 69, 72, 76, 79, 83, 112, 172, 233, 255, 255, 255, 255, 255, 255 // BLI
        }
    };

    private Resources mResources;
    private SeekBar mBrightnessBar;
    private SeekBar mDarkeningSpeedBar;
    private SeekBar mBrighteningSpeedBar;
    private SeekBar mSmartBLStrengthBar;
    private SeekBar mSmartBLRangeBar;
    private SeekBar mReadabilityBar;
    private SeekBar mLowBLReadabilityBar;
    private TextView mBrightnessText;
    private TextView mDarkeningSpeedText;
    private TextView mBrighteningSpeedText;
    private TextView mSmartBLStrengthText;
    private TextView mSmartBLRangeText;
    private TextView mReadabilityText;
    private TextView mLowBLReadabilityText;
    private Button mSaveButton;
    private SharedPreferences mPreferences;
    private int mBrightness = 255;
    private final int mBrightnessMode = Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
    private int mPreBrightnessMode = Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
    
    //the content resolver used as a handle to the system's settings  
    private ContentResolver mContentResolver;  
    //a window object, that will store a reference to the current window  
    private Window mWindow;

    private static class AALParameters {
        public int brightnessLevel;
        public int darkeningSpeedLevel;
        public int brighteningSpeedLevel;
        public int smartBacklightStrength;
        public int smartBacklightRange;
        public int readabilityLevel;
        public int lowBLReadabilityLevel;

        public AALParameters(Resources resource) {
            brightnessLevel = resource.getInteger(R.integer.MAX_BRIGHTNESS_LEVEL) / 2;
            darkeningSpeedLevel = resource.getInteger(R.integer.MAX_DARKENING_SPEED_LEVEL) / 2;
            brighteningSpeedLevel = resource.getInteger(R.integer.MAX_BRIGHTENING_SPEED_LEVEL) / 2;
            smartBacklightStrength = resource.getInteger(R.integer.MAX_SMARTBACKLIGHT_STRENGTH) / 2;
            smartBacklightRange = resource.getInteger(R.integer.MAX_SMARTBACKLIGHT_RANGE) / 2;
            readabilityLevel = resource.getInteger(R.integer.MAX_READABILITY_LEVEL) / 2;
            lowBLReadabilityLevel = resource.getInteger(R.integer.MAX_LOW_BL_READABILITY_LEVEL) / 2;
        }
    }
    
    private AALParameters mParam;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.aal_tuning);        

        mResources = getResources();

        //get the content resolver  
        mContentResolver = getContentResolver();
        //get the current window  
        mWindow = getWindow();
        mPreBrightnessMode = Settings.System.getInt(mContentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        Settings.System.putInt(mContentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, mBrightnessMode);
		
        
        mSaveButton = (Button)findViewById(R.id.buttonSave);
        mSaveButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                saveToFile();
            }
        });
        
        mPreferences = getSharedPreferences(PREFS_NAME, 0);
        loadPreference();

        mBrightnessText = (TextView) this.findViewById(R.id.textBrightnessLevel); 
        mBrightnessText.setText(Integer.toString(mParam.brightnessLevel));
        mBrightnessBar = (SeekBar)findViewById(R.id.seekBarBrightness); // make seekbar object
        mBrightnessBar.setOnSeekBarChangeListener(this); // set seekbar listener.
        
        mDarkeningSpeedText = (TextView) this.findViewById(R.id.textDarkeningSpeedLevel); 
        mDarkeningSpeedText.setText(Integer.toString(mParam.darkeningSpeedLevel));
        mDarkeningSpeedBar = (SeekBar)findViewById(R.id.seekBarDarkeningSpeed); // make seekbar object
        mDarkeningSpeedBar.setOnSeekBarChangeListener(this); // set seekbar listener. 
        
        mBrighteningSpeedText = (TextView) this.findViewById(R.id.textBrighteningSpeedLevel); 
        mBrighteningSpeedText.setText(Integer.toString(mParam.brighteningSpeedLevel));
        mBrighteningSpeedBar = (SeekBar)findViewById(R.id.seekBarBrighteningSpeed); // make seekbar object
        mBrighteningSpeedBar.setOnSeekBarChangeListener(this); // set seekbar listener.       

        mSmartBLStrengthText = (TextView) this.findViewById(R.id.textSmartBLStrengthLevel); 
        mSmartBLStrengthText.setText(Integer.toString(mParam.smartBacklightStrength));
        mSmartBLStrengthBar = (SeekBar)findViewById(R.id.seekBarSmartBLStrength); // make seekbar object
        mSmartBLStrengthBar.setOnSeekBarChangeListener(this); // set seekbar listener.

        mSmartBLRangeText = (TextView) this.findViewById(R.id.textSmartBLRangeLevel); 
        mSmartBLRangeText.setText(Integer.toString(mParam.smartBacklightRange));
        mSmartBLRangeBar = (SeekBar)findViewById(R.id.seekBarSmartBLRange); // make seekbar object
        mSmartBLRangeBar.setOnSeekBarChangeListener(this); // set seekbar listener.

        mReadabilityText = (TextView) this.findViewById(R.id.textReadabilityLevel); 
        mReadabilityText.setText(Integer.toString(mParam.readabilityLevel));
        mReadabilityBar = (SeekBar)findViewById(R.id.seekBarReadability); // make seekbar object
        mReadabilityBar.setOnSeekBarChangeListener(this); // set seekbar listener.

        mLowBLReadabilityText = (TextView) this.findViewById(R.id.textLowBLReadabilityLevel); 
        mLowBLReadabilityText.setText(Integer.toString(mParam.lowBLReadabilityLevel));
        mLowBLReadabilityBar = (SeekBar)findViewById(R.id.seekBarLowBLReadability); // make seekbar object
        mLowBLReadabilityBar.setOnSeekBarChangeListener(this); // set seekbar listener.

        mBrightnessBar.setProgress(mParam.brightnessLevel);
        mDarkeningSpeedBar.setProgress(speedLevelToProgress(mParam.darkeningSpeedLevel));
        mBrighteningSpeedBar.setProgress(speedLevelToProgress(mParam.brighteningSpeedLevel));
        mSmartBLStrengthBar.setProgress(mParam.smartBacklightStrength);
        mSmartBLRangeBar.setProgress(mParam.smartBacklightRange);
        mReadabilityBar.setProgress(mParam.readabilityLevel);
        mLowBLReadabilityBar.setProgress(mParam.lowBLReadabilityLevel);
    }

    private static final double SPEED_MIDDLE = 40.0;
    private static final double SPEED_MAX = 255.0;
    private static final double SPEED_GAMMA = Math.log(SPEED_MIDDLE / SPEED_MAX) / Math.log(0.5);
    
    private static int speedLevelToProgress(int level) {
        double progress = Math.exp(Math.log((double)level / SPEED_MAX) / SPEED_GAMMA) * 255.0;
        return Math.max(Math.min((int)(progress + 0.5), 255), 0);
    }

    private static int speedProgressToLevel(int progress) {
        double level = Math.pow((double)progress / 255.0, SPEED_GAMMA) * SPEED_MAX;
        return Math.max(Math.min((int)(level + 0.5), 255), 1);
    }

    private void loadPreference() {
        mParam = new AALParameters(mResources);
    
        Map<String,?> keys = mPreferences.getAll();
        for(Map.Entry<String,?> entry : keys.entrySet()) {
            Log.d(TAG, "map values " + entry.getKey() + ": " + entry.getValue().toString());
            int value = Integer.parseInt(entry.getValue().toString());
            if (entry.getKey().equals("Brightness"))
                mParam.brightnessLevel = value;
            if (entry.getKey().equals("DarkeningSpeed")) 
                mParam.darkeningSpeedLevel = value;
            if (entry.getKey().equals("BrighteningSpeed")) 
                mParam.brighteningSpeedLevel = value;
            if (entry.getKey().equals("SmartBacklightStrength")) 
                mParam.smartBacklightStrength = value;
            if (entry.getKey().equals("SmartBacklightRange")) 
                mParam.smartBacklightRange = value;
            if (entry.getKey().equals("Readability"))
                mParam.readabilityLevel = value;
            if (entry.getKey().equals("LowBLReadability"))
                mParam.lowBLReadabilityLevel = value;
        }
        
        // Get latest parameters from AALService
        nGetParameters(mParam);
    }
    
    private void saveToFile() {
        try {
            FileOutputStream fos = openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
            PrintWriter pw = new PrintWriter(fos); 

            int[] curve = getALI2BLICurve(mParam.brightnessLevel);
            pw.println("frameworks/base/core/res/res/values/config.xml:");
            pw.println("    <integer-array name=\"config_autoBrightnessLevels\">");
            for (int i = 1; i < curve.length / 2; i++)
                pw.println("        <item>" + curve[i] + "</item>");
            pw.println("    </integer-array>");
            pw.println("");

            pw.println("    <integer-array name=\"config_autoBrightnessLcdBacklightValues\">");
            for (int i = curve.length / 2; i < curve.length; i++)
                pw.println("        <item>" + curve[i] + "</item>");
            pw.println("    </integer-array>");
            pw.println("");

            pw.println("frameworks/base/services/core/java/com/android/server/display/DisplayPowerController.java:");
            pw.println("BRIGHTNESS_RAMP_RATE_DARKEN = " + mParam.darkeningSpeedLevel);
            pw.println("BRIGHTNESS_RAMP_RATE_BRIGHTEN = " + mParam.brighteningSpeedLevel);
            pw.println("");

            pw.println("vendor/mediatek/proprietary/custom/<proj>/hal/aal/cust_aal.cpp:");
            pw.println("SmartBacklightStrengthLevel = " + mParam.smartBacklightStrength);
            pw.println("SmartBacklightRangeLevel = " + mParam.smartBacklightRange);
            pw.println("ReadabilityLevel = " + mParam.readabilityLevel);
            pw.println("LowBLReadabilityLevel = " + mParam.lowBLReadabilityLevel);
            pw.close();
            fos.close();
        } catch (java.io.IOException e){
            // Unable to save, do nothing
        }
    }
    	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.aal_tuning, menu);
        return true;
    }

    private int[] getALI2BLICurve(int level) {
        float adjValue = (float)level / (float)mResources.getInteger(R.integer.MAX_BRIGHTNESS_LEVEL);
        float indexF = adjValue * (float)(ALI2BLI.length - 1);

        float floorF = Math.max((float)Math.floor(indexF), 0.0f);
        int floor = (int)floorF;
        int ceil = Math.min((int)Math.ceil(indexF), (ALI2BLI.length - 1));

        if (ALI2BLI[floor].length != ALI2BLI[ceil].length) {
            return ALI2BLI[floor];
        }

        float alpha = 1.0f - (indexF - floorF);
        int length = ALI2BLI[floor].length;
        int[] curve = new int[length];
        for (int i = 0; i < length; i++) {
            float interpolated =
                    (float)ALI2BLI[floor][i] * alpha + (float)ALI2BLI[ceil][i] * (1.0f - alpha);
            curve[i] = (int)(interpolated + 0.5f);
        }

        for (int i = length / 2; i < length; i++) {
            if (curve[i] > 255)
                curve[i] = 255;
        }

        Log.d(TAG, "getALI2BLICurve(" + level + ") : index = " + indexF);
        StringBuffer buffer = new StringBuffer();
        buffer.append("ali:");
        for (int i = 0; i < length / 2; i++) {
            buffer.append(" ");
            buffer.append(curve[i]);
        }
        buffer.append(", bli:");
        for (int i = length / 2; i < length; i++) {
            buffer.append(" ");
            buffer.append(curve[i]);
        }
        Log.d(TAG, buffer.toString());

        return curve;
    }

    @Override
    public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {        
        String key = "";
        if (arg0 == mBrightnessBar) {
            Log.d(TAG, "Brightness level = " + arg1);

            int[] curve = getALI2BLICurve(arg1);
            if (nSetALI2BLICurve(curve)) {
                key = "Brightness";
                mParam.brightnessLevel = arg1;
                mBrightnessText.setText(Integer.toString(mParam.brightnessLevel));
                
                sendUpdateConfigIntent();
            }
        }
        if (arg0 == mDarkeningSpeedBar) {
            int rate = speedProgressToLevel(arg1);
            Log.d(TAG, "set Darkening Speed level = " + arg1 + " -> " + rate);
            if (nSetRampRateDarken(rate)) {
                key = "DarkeningSpeed";
                mParam.darkeningSpeedLevel = rate;
                mDarkeningSpeedText.setText(Integer.toString(mParam.darkeningSpeedLevel));

                sendUpdateConfigIntent();
            }
        }
        if (arg0 == mBrighteningSpeedBar) {
            int rate = speedProgressToLevel(arg1);
            Log.d(TAG, "set Brightening Speed level = " + arg1 + " -> " + rate);
            if (nSetRampRateBrighten(rate)) {
                key = "BrighteningSpeed";
                mParam.brighteningSpeedLevel = rate;
                mBrighteningSpeedText.setText(Integer.toString(mParam.brighteningSpeedLevel));

                sendUpdateConfigIntent();
            }
        }
        if (arg0 == mSmartBLStrengthBar) {
            Log.d(TAG, "set SmartBacklight strength = " + arg1);
            if (nSetSmartBacklightStrength(arg1)) {
                key = "SmartBacklightStrength";
                mParam.smartBacklightStrength = arg1;
                mSmartBLStrengthText.setText(Integer.toString(mParam.smartBacklightStrength));
            }
        }
        if (arg0 == mSmartBLRangeBar) {
            Log.d(TAG, "set SmartBacklight range = " + arg1);
            if (nSetSmartBacklightRange(arg1)) {
                key = "SmartBacklightRange";
                mParam.smartBacklightRange = arg1;
                mSmartBLRangeText.setText(Integer.toString(mParam.smartBacklightRange));
            }
        }
        if (arg0 == mReadabilityBar) {
            Log.d(TAG, "set Readability level = " + arg1);
            if (nSetReadabilityLevel(arg1)) {
                key = "Readability";
                mParam.readabilityLevel = arg1;
                mReadabilityText.setText(Integer.toString(mParam.readabilityLevel));
            }
        }
        if (arg0 == mLowBLReadabilityBar) {
            Log.d(TAG, "set LowBLReadability level = " + arg1);
            if (nSetLowBLReadabilityLevel(arg1)) {
                key = "LowBLReadability";
                mParam.lowBLReadabilityLevel = arg1;
                mLowBLReadabilityText.setText(Integer.toString(mParam.lowBLReadabilityLevel));
            }
        }
		
        if (key.length() > 0) {
            SharedPreferences.Editor editor = mPreferences.edit();
            editor.putInt(key, arg1);
            editor.commit();
        }

    }

    private static final String MTK_AAL_UPDATE_CONFIG_ACTION = "com.mediatek.aal.update_config";

    private void sendUpdateConfigIntent() {
        Intent intent = new Intent(MTK_AAL_UPDATE_CONFIG_ACTION);
        sendBroadcast(intent);
    }
    

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    protected void onResume() {
        Log.d(TAG, "onResume..., restore brightness mode to " + mBrightnessMode);
        mPreBrightnessMode = Settings.System.getInt(mContentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        Settings.System.putInt(mContentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, mBrightnessMode);

        nEnableFunctions();
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause..., restore brightness mode to " + mPreBrightnessMode);
        Settings.System.putInt(mContentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, mPreBrightnessMode);
        super.onPause();
    }
	
    static {
        System.loadLibrary("aaltool_jni");
    }

    private native boolean nSetALI2BLICurve(int[] curve);
    private native boolean nSetRampRateDarken(int rate);
    private native boolean nSetRampRateBrighten(int rate);
    private native boolean nSetSmartBacklightStrength(int level);
    private native boolean nSetSmartBacklightRange(int level);
    private native boolean nSetReadabilityLevel(int level);
    private native boolean nSetLowBLReadabilityLevel(int level);
    private native boolean nEnableFunctions();
    private native void nGetParameters(AALParameters param);
}
