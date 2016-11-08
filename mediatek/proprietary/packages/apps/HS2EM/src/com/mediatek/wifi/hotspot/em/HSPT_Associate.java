package com.mediatek.wifi.hotspot.em;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.net.wifi.WifiManager;
import android.os.ServiceManager;
import android.os.RemoteException;
import android.content.Context;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;

public class HSPT_Associate extends Activity {

    private WifiManager mWifiMgr = null;
    private EditText        mEditTextIndex;
    private EditText        mEditTextName;
    private EditText        mEditTextValue;
    private EditText        mEditTextPn;

    private Button          mButtonSetHsNetwork;
    private Button          mButtonSetHsPreferredNetwork;
    private Button          mButtonSetHsCred;
    private Button          mButtonEnableHs;

    private TextView        mTextViewDump;
    private RadioButton     mRadioButtonOn;
    private RadioButton     mRadioButtonOff;
    private RadioButton     mRadioButtonPreferred;
    private SharedPreferences mSharedPref;
    public static final String ALL_SETTINGS = "settings";
    public static final String ENABLE_HS_SETTING = "enable_hs_setting";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hspt_associate);

        mWifiMgr = (WifiManager)getSystemService(WIFI_SERVICE);
        if(mWifiMgr == null) {
            MtkLog.log("ERR: getSystemService MtkWifiManager failed");
            return;
        }

        initWidget();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void initWidget() {

        mEditTextIndex = (EditText)findViewById(R.id.EditText_Index);
        mEditTextName = (EditText)findViewById(R.id.EditText_Name);
        mEditTextValue = (EditText)findViewById(R.id.EditText_Value);
        mEditTextPn = (EditText)findViewById(R.id.EditText_PreferredNetwork);

        mButtonSetHsNetwork = (Button)findViewById(R.id.Button_SetHsNetwork);
        mButtonSetHsCred = (Button)findViewById(R.id.Button_SetHsCred);
        mButtonEnableHs = (Button)findViewById(R.id.Button_EnableHS);
        mButtonSetHsPreferredNetwork = (Button)findViewById(R.id.Button_setPreferredNetwork);

        mTextViewDump = (TextView)findViewById(R.id.TextView_Dump);
        mRadioButtonOn = (RadioButton)findViewById(R.id.RadioButton_On);
        mRadioButtonOff = (RadioButton)findViewById(R.id.RadioButton_Off);
        mRadioButtonPreferred = (RadioButton)findViewById(R.id.RadioButton_Preferred);
        mButtonEnableHs.setEnabled(false);
        mRadioButtonOn.setEnabled(false);
        mRadioButtonOff.setEnabled(false);

        mSharedPref = this.getSharedPreferences(ALL_SETTINGS, 0);
        boolean isEnabled = mSharedPref.getBoolean(ENABLE_HS_SETTING, false);
        if (isEnabled) {
            mRadioButtonOn.setChecked(true);
        } else {
            mRadioButtonOff.setChecked(true);
        }

        mButtonSetHsNetwork.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                handleSetHsNetwork();
            }
        });

        mButtonSetHsPreferredNetwork.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String index        = mEditTextPn.getText().toString();
                boolean isPreferred = mRadioButtonPreferred.isChecked();
                handleSetPreferredNetwork(Integer.parseInt(index), isPreferred);
            }
        });

        mButtonEnableHs.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean isEnabled = mRadioButtonOn.isChecked();
                SharedPreferences.Editor editor = mSharedPref.edit();
                editor.putBoolean(ENABLE_HS_SETTING, isEnabled);
                editor.commit();
                handleEnableHs(isEnabled);
            }
        });

        mButtonSetHsCred.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                handleSetHsCredential();
            }
        });
    }

    private void handleSetHsCredential() {
        String index        = mEditTextIndex.getText().toString();
        String name        = mEditTextName.getText().toString();
        String value        = mEditTextValue.getText().toString();
        MtkLog.log("handleSetHsCredential:"+ index +","+name+","+value);
        boolean ret = false;
        if (index.isEmpty() || name.isEmpty() ||value.isEmpty()) {
            mTextViewDump.setText("handleSetHsCredential failed due to empty values");
            return;
        }
        
        if (mWifiMgr != null ) {
            ret = mWifiMgr.setHsCredential(Integer.parseInt(index),name,value);
        }
        mTextViewDump.setText("handleSetHsCredential:" + (ret? "success":"failure"));
    }

    private void handleSetHsNetwork() {
        String index        = mEditTextIndex.getText().toString();
        String name        = mEditTextName.getText().toString();
        String value        = mEditTextValue.getText().toString();
        MtkLog.log("handleSetHsNetwork:"+ index +","+name+","+value);
        boolean ret = false;
        if (index.isEmpty() || name.isEmpty() ||value.isEmpty()) {
            mTextViewDump.setText("handleSetHsNetwork failed due to empty values");
            return;
        }
        
        if (mWifiMgr != null ) {
            ret = mWifiMgr.setHsNetwork(Integer.parseInt(index),name,value);
        }
        mTextViewDump.setText("handleSetHsNetwork:" + (ret? "success":"failure"));
    }

    private void handleEnableHs(boolean enable) {
        MtkLog.log("handleEnableHs:" + enable);
        boolean ret = false;
        if (mWifiMgr != null ) {
            ret = mWifiMgr.enableHS(enable);
        }
        mTextViewDump.setText("handleEnableHs("+ enable +"):" + (ret? "success":"failure"));
    }

    private void handleSetPreferredNetwork(int index, boolean isPreferred) {
        MtkLog.log("handleSetPreferredNetwork, index:" + index + " isPreferred:" + isPreferred);
        boolean ret = false;
        int value = isPreferred? 1 : 0;
        if (mWifiMgr != null ) {
            ret = mWifiMgr.setHsPreferredNetwork(index, value);
        }
        mTextViewDump.setText("handleSetPreferredNetwork:" + (ret? "success":"failure"));

    }
}
