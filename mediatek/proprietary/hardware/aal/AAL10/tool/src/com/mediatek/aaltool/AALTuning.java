package com.mediatek.aaltool;

import android.os.Bundle;
import android.provider.Settings;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.AbsoluteLayout;
import android.widget.Button;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.content.Context;
import android.graphics.Color;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Map;

public class AALTuning extends Activity implements OnSeekBarChangeListener {
    private static final String TAG = "AALTool";
    private static final String PREFS_NAME = "aal";
    private static final String FILE_NAME = "aal.cfg";
    private SeekBar mSmartBacklightBar;
    private SeekBar mToleranceRatioBar;
    private SeekBar mReadabilityBar;
    private TextView mSmartBacklightText;
    private TextView mToleranceRatioText;
    private TextView mReadabilityText;
    private TextView mToleranceRatioTitle;
    private TextView mReadabilityTitle;
    private int mSmartBacklightLevel = 5;
    private int mToleranceRatioLevel = 0;
    private int mReadabilityLevel = 5;
    private Button mSaveButton;
    private SharedPreferences mPreferences;
    
    //the content resolver used as a handle to the system's settings  
    private ContentResolver mContentResolver;  
    //a window object, that will store a reference to the current window  
    private Window mWindow;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.aal_tuning);        
        
        Log.d(TAG, "onCreate...");
        
        
        //get the content resolver  
        mContentResolver = getContentResolver();
        //get the current window  
        mWindow = getWindow();
        
        mSaveButton = (Button)findViewById(R.id.buttonSave);
        mSaveButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                saveToFile();
            }
        });
        
        mPreferences = getSharedPreferences(PREFS_NAME, 0);
        loadPreference();

        mSmartBacklightText = (TextView) this.findViewById(R.id.textSmartBacklightLevel); 
        mSmartBacklightText.setText("level: " + Integer.toString(mSmartBacklightLevel));
        mSmartBacklightBar = (SeekBar)findViewById(R.id.seekBarSmartBacklight); // make seekbar object
        mSmartBacklightBar.setOnSeekBarChangeListener(this); // set seekbar listener.

        mToleranceRatioText = (TextView) this.findViewById(R.id.textToleranceRatioLevel); 
        mToleranceRatioText.setText("level: " + Integer.toString(mToleranceRatioLevel));
        mToleranceRatioBar = (SeekBar)findViewById(R.id.seekBarToleranceRatio); // make seekbar object
        mToleranceRatioBar.setOnSeekBarChangeListener(this); // set seekbar listener.       
        
        mReadabilityText = (TextView) this.findViewById(R.id.textReadabilityLevel); 
        mReadabilityText.setText("level: " + Integer.toString(mReadabilityLevel));
        mReadabilityBar = (SeekBar)findViewById(R.id.seekBarReadability); // make seekbar object
        mReadabilityBar.setOnSeekBarChangeListener(this); // set seekbar listener.

        mToleranceRatioTitle = (TextView) this.findViewById(R.id.textToleranceRatio); 
        mReadabilityTitle = (TextView) this.findViewById(R.id.textReadability); 

        enableSeekBar((mSmartBacklightLevel > 0), mToleranceRatioBar, mToleranceRatioTitle);

        mSmartBacklightBar.setProgress(mSmartBacklightLevel);
        mToleranceRatioBar.setProgress(mToleranceRatioLevel);
        mReadabilityBar.setProgress(mReadabilityLevel);
    }

    private void loadPreference() {
        Map<String,?> keys = mPreferences.getAll();
        for(Map.Entry<String,?> entry : keys.entrySet()) {
            Log.d(TAG, "map values " + entry.getKey() + ": " + entry.getValue().toString());
            int value = Integer.parseInt(entry.getValue().toString());
            if (entry.getKey().equals("SmartBacklight")) 
                mSmartBacklightLevel = value;
            if (entry.getKey().equals("ToleranceRatio")) 
                mToleranceRatioLevel = value;
            if (entry.getKey().equals("Readability"))
                mReadabilityLevel = value;
        }
    }
    private void saveToFile() {
        try {
            
            FileOutputStream fos = openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
            PrintWriter pw = new PrintWriter(fos); 
            pw.println("SmartBacklight=" + mSmartBacklightLevel);
            pw.println("ToleranceRatio=" + mToleranceRatioLevel);
            pw.println("Readability=" + mReadabilityLevel);
            pw.close();
            fos.close();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    private void enableSeekBar(boolean enable, SeekBar seekbar, TextView titletext) {
        if (enable) {
            seekbar.setEnabled(true);
            titletext.setTextColor(Color.rgb(0, 0, 0));
        }
        else {
            seekbar.setEnabled(false);
            titletext.setTextColor(Color.rgb(156, 156, 156));
        }	
    }
    	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.aal_tuning, menu);
        return true;
    }

    @Override
    public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {        
        String key = "";
        if (arg0 == mSmartBacklightBar) {
            Log.d(TAG, "set SmartBacklight level = " + arg1);
            if (nSetSmartBacklightLevel(arg1)) {
                key = "SmartBacklight";
                mSmartBacklightLevel = arg1;
                mSmartBacklightText.setText("level: " + Integer.toString(mSmartBacklightLevel));

                enableSeekBar((mSmartBacklightLevel > 0), mToleranceRatioBar, mToleranceRatioTitle);
            }
        }
        if (arg0 == mToleranceRatioBar) {
            Log.d(TAG, "set Tolerance Ratio level = " + arg1);
            if (nSetToleranceRatioLevel(arg1)) {
                key = "ToleranceRatio";
                mToleranceRatioLevel = arg1;
                mToleranceRatioText.setText("level: " + Integer.toString(mToleranceRatioLevel));
            }
        }
        if (arg0 == mReadabilityBar) {
            Log.d(TAG, "set Readability level = " + arg1);
            if (nSetReadabilityLevel(arg1)) {
                key = "Readability";
                mReadabilityLevel = arg1;
                mReadabilityText.setText("level: " + Integer.toString(mReadabilityLevel));
            }
        }
		
        if (key.length() > 0) {
            SharedPreferences.Editor editor = mPreferences.edit();
            editor.putInt(key, arg1);
            editor.commit();
        }

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
        super.onResume();

        // Enable all AAL functions
        nEnableFunctions();
    }

    static {
        System.loadLibrary("aaltool_jni");
    }

    private native boolean nSetSmartBacklightLevel(int level);
    private native boolean nSetToleranceRatioLevel(int level);
    private native boolean nSetReadabilityLevel(int level);
    private native boolean nEnableFunctions();
}
