
package com.mediatek.nfc.dta;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;   //Vibtator
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Switch;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.util.Log;
import android.os.Environment;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.IOException;
//import java.lang.Process;
//import java.io.DataOutputStream;
//import java.io.InputStream;

import android.content.SharedPreferences;
import android.provider.Settings;

import com.mediatek.nfc.dta.NativeDtaManager;
import java.io.File;

public class DeviceTestApp extends Activity implements OnClickListener {

    private static final String TAG = "DTA";
    private static final boolean DBG = true;
    private boolean mNfcEnabled = false;

    public static Context sContext;

    private ProgressDialog mProgressDialog;
    private IntentFilter mIntentFilter;
    private TextView mDtaVesrion;
    private TextView mResult;
    private Spinner mTestEquipmentSpinner;
    private Button mSetButton;

    private static final int MSG_UPDATE_MESSAGE = 1001;
    private static final int MSG_SWITCH_TEST_STATE = 1002;

    private Vibrator mVibrator;
    static final long[] VIBRATION_PATTERN = {0, 100, 10000};
    static final long[] VIBRATION_PATTERN_2 = {0, 200, 500};

    private NfcAdapter mAdapter;
    private int mCurrentTestInstrument;
    private String mCurrentInstrumentName;
    private String mCurrentDestFolderPath;
    private boolean mCopyConfigSuccess;

    //DTA mode switch
    private TextView mRebootNotifyText;
    private Switch mDtaModeSwitch;
    private SharedPreferences mPrefs;
    private Context mContext;
    private int mDtaMode;
    private boolean mMustReboot;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (NfcAdapter.ACTION_ADAPTER_STATE_CHANGED.equals(action)) {
                handleNfcStateChanged(intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE,
                        NfcAdapter.STATE_OFF));
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_test_app);

        sContext = this; // d-load

        mAdapter = NfcAdapter.getDefaultAdapter(this);

        if (mAdapter != null ) {
            initUI();

            getDtaVersion();

            new InitTestTask().execute(0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.set_config:
                if (DBG) Log.d(TAG, "Set Config");
                runSetConfig();
                break;
            default:
                Log.d(TAG, "ghost button.");
                break;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
/**
 * 2015/05/08 add quick enable Flag on DTA init
 * 2016/06/08  doDownload() bug fix
 */
    private void disableNfc(){
        //read

        if (DBG) Log.d(TAG, "[QE]disable Nfc");

        mNfcEnabled = mAdapter.isEnabled();

        if (mNfcEnabled) {
            // clear DTA Config
            new NativeDtaManager().setDtaQuickMode(0);
            if (DBG) Log.d(TAG, "[QE]Nfc is on,Now to disable NFC");
            if (DBG) Log.d(TAG, "[QE]change Nfc Adapter to state_changed");
            mIntentFilter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
            if (DBG) Log.d(TAG, "[QE]Start  DTA-JNI de-init");
            registerReceiver(mReceiver, mIntentFilter);
            if (DBG) Log.d(TAG, "[QE]setDtaQuickMode =1 ");
            new NativeDtaManager().setDtaQuickMode(1);
            if (mAdapter.disable()) {
                mProgressDialog = ProgressDialog.show(DeviceTestApp.this, "Disable Nfc", "Please wait ...", true);
                mProgressDialog.show();
            }
        } else {
            // init nfc state
            // clear DTA Config
            new NativeDtaManager().setDtaQuickMode(0);
            //set DTA Config
            new NativeDtaManager().setDtaQuickMode(1);
            if (DBG) Log.d(TAG, "[QE]Nfc is off");
            if (DBG) Log.d(TAG, "[QE]Enter DTA-JNI force clear all()!");
            new NativeDtaManager().setDtaQuickMode(-1);
        }
    }

    private void enableNfc() {
        if (DBG) Log.d(TAG, "[QE]enable Nfc");
        if (mNfcEnabled) {
            mAdapter.enable();
        }
    }

    private void initUI() {
        //vibrator
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        mSetButton = (Button) findViewById(R.id.set_config);
        mSetButton.setOnClickListener(this);

        //test instrument list
        mTestEquipmentSpinner = (Spinner)findViewById(R.id.test_instrument_list);
        ArrayAdapter adapter = ArrayAdapter.createFromResource(this, R.array.test_instrument_table,
                      android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mTestEquipmentSpinner.setAdapter(adapter);

        mTestEquipmentSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                mCurrentTestInstrument = arg2;
                if (DBG) Log.d(TAG, "mCurrentTestInstrument : " + mCurrentTestInstrument + ", " + arg2);
            }

            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        //DTA mode switch
        mContext = this;
        //text
        mRebootNotifyText = (TextView) findViewById(R.id.dta_must_reboot_text);

        //switch
        mDtaModeSwitch = (Switch) findViewById(R.id.dta_mode_switch);

        //set the switch to ON

        //mDtaModeSwitch.setChecked(true);
        mDtaMode = Settings.Global.getInt( this.getContentResolver(),
            DeviceTestAppConstants.PREF_P2P_MANAGER_MODE,
            DeviceTestAppConstants.MANAGER_MODE_DEFAULT);
        //init prefence
        mPrefs = mContext.getSharedPreferences(DeviceTestAppConstants.DTA_PREF,
                                               Context.MODE_PRIVATE);

        //get mode
        mDtaMode = getMode();

        if (mDtaMode == -1) {
            mMustReboot = true;
        } else {
            mMustReboot = false;
        }
        setRebootNotify(mMustReboot);

        if (mDtaMode == DeviceTestAppConstants.MANAGER_MODE_DTA) {
            mDtaModeSwitch.setChecked(true);
        } else {
            mDtaModeSwitch.setChecked(false);
        }

        //attach a listener to check for changes in state
        mDtaModeSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    setMode( 1 );
                }else{
                    setMode( 0 );
                }
            }

        });
        //version
        mDtaVesrion = (TextView) findViewById(R.id.dta_version);
        //result
        mResult = (TextView) findViewById(R.id.result);

    }

    private void getDtaVersion() {
        if (DBG) Log.d(TAG, "getDtaVersion");
        NativeDtaManager mDtaManager = new NativeDtaManager();
        String dtaVersion ;

        dtaVersion = "JAVA_" + DeviceTestAppConstants.DTA_JAVA_VERSION +
                     ",JNI_" + mDtaManager.getDtaVersion();

        //display version
        mDtaVesrion.setText(dtaVersion);

        mDtaManager = null;
    }

    private void runSetConfig() {
        if (DBG) Log.d(TAG, "runSetConfig");
        new CopyConfigTask().execute(0);
    }

    private void runTestMain(){
        if (DBG) Log.d(TAG, "runTestMain");

        if (mCopyConfigSuccess) {

            Intent intent = new Intent();
            Bundle bundle = new Bundle();
            intent.setClass(DeviceTestApp.this, TestMainMenu.class);
            intent.setAction(DeviceTestAppConstants.ACTION_DTA_MAIN_START);

            bundle.putString(DeviceTestAppConstants.DTA_INSTRUMENT_NAME, mCurrentInstrumentName);
            bundle.putString(DeviceTestAppConstants.DTA_DEST_CONFIG_PATH, mCurrentDestFolderPath);
            intent.putExtras(bundle);
            startActivity(intent);
            finish();
        } else {
            if (DBG) Log.d(TAG, "Fail.");
            mResult.setText( mResult.getText() + "\n Fail.");
        }
    }

    private void handleNfcStateChanged(int newState) {
        switch (newState) {
        case NfcAdapter.STATE_OFF:
            if(mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            unregisterReceiver(mReceiver);
            break;

        case NfcAdapter.STATE_ON:
            break;

        case NfcAdapter.STATE_TURNING_ON:
            break;

        case NfcAdapter.STATE_TURNING_OFF:
            break;

        }
    }

    private void setRebootNotify(boolean reboot) {
        if (reboot) {
            mRebootNotifyText.setVisibility(View.VISIBLE);
        } else {
            mRebootNotifyText.setVisibility(View.GONE);
        }
    }

    private void setMode(int mode) {
        if (DBG) Log.d(TAG, "setMode " + DeviceTestAppConstants.ACTION_SET_MODE + ", mode = " +
                       mode);
            Intent intent = new Intent();
            intent.setAction( DeviceTestAppConstants.ACTION_SET_MODE );
            Bundle bundle = new Bundle();
            bundle.putInt( DeviceTestAppConstants.EXTRA_P2P_MODE, mode);
            intent.putExtras(bundle);
            mContext.sendBroadcast(intent);

            synchronized (DeviceTestApp.this) {
            mPrefs.edit().putInt(DeviceTestAppConstants.PREF_P2P_MANAGER_MODE, mode).apply();
        }
        mMustReboot = true;
        setRebootNotify(mMustReboot);
    }

    private int getMode(){
        if (DBG) Log.d(TAG, "getMode ");
            synchronized (DeviceTestApp.this) {
            return mPrefs.getInt(DeviceTestAppConstants.PREF_P2P_MANAGER_MODE, -1);
        }
    }


    class InitTestTask extends AsyncTask<Integer,String,Integer> {
        private ProgressDialog mProgressDialog;

        @Override
        protected Integer doInBackground(Integer... count) {

            Integer successCounts = new Integer(0);
            //-------- start test -----------
            for(int i=0 ; i<1; i++) {

                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    if (DBG) Log.d(TAG, e.toString());
                }
                // if NFC is enable , break . and prepare to disable
                if (mAdapter.isEnabled()) {
                    break;
                }
            }
            return successCounts;
        }

        @Override
        protected void onPreExecute(){
            mProgressDialog = ProgressDialog.show(DeviceTestApp.this, "Init Test", "Please wait ...", true);
            mProgressDialog.setCancelable(true);
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(String... progress) {
        }

        @Override
        protected void onPostExecute(Integer counts) {
            if(mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            disableNfc();
        }
    }

    class CopyConfigTask extends AsyncTask<Integer,String,Integer> {
        private ProgressDialog mProgressDialog;
        private String mCopyResult;

        @Override
        protected Integer doInBackground(Integer... count) {

            Integer successCounts = new Integer(0);
            mCopyConfigSuccess = false;

            //Path could be as below:
            //    /data/misc/nfc_conformance/DTA_Config/AT4
            //    /data/misc/nfc_conformance/DTA_Config/Comprion
            //    /data/misc/nfc_conformance/DTA_Config/Clear2Pay
            //    /data/misc/nfc_conformance/DTA_Config/Others
            // --------------------

            //get source folder
            List<String> instrumentName = Arrays.asList("AT4", "Comprion", "Clear2Pay", "Others");

            mCurrentInstrumentName = instrumentName.get(mCurrentTestInstrument);
            if (DBG) Log.d(TAG, "mCurrentInstrumentName = " + mCurrentInstrumentName );

            String sourceFolderPath = DeviceTestAppConstants.DTA_CONFIG_FOLDER_SOURCE +
              mCurrentInstrumentName;
            if (DBG) Log.d(TAG, sourceFolderPath );
            mCopyResult = "Source : \n" + sourceFolderPath + "\n";

            final File folder = new File(sourceFolderPath);
            //check folder exist
            if (folder.exists() && folder.isDirectory()) {
                //check source file
/*
                List<String> fileList = DeviceTestAppConstants.listFilesForFolder(folder);
                for(String fileName : fileList) {
                    if (DBG) Log.d(TAG, " - " + fileName );
                    mCopyResult += " - " + fileName + "\n";
}
*/

           // get dest folder path
           mCurrentDestFolderPath = sourceFolderPath;
           mCopyConfigSuccess = true;

/*
                mCurrentDestFolderPath = Environment.getExternalStorageDirectory().toString();
                if (DBG) Log.d(TAG, "Dest Folder:" + mCurrentDestFolderPath);
                mCopyResult = "Dest Folder:" + mCurrentDestFolderPath + "\n";

                //check externel storage state
                boolean mExternalStorageAvailable = false;
                boolean mExternalStorageWriteable = false;
                String state = Environment.getExternalStorageState();
                if (Environment.MEDIA_MOUNTED.equals(state)) {
                    mExternalStorageAvailable = mExternalStorageWriteable = true;
                } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                    mExternalStorageAvailable = true;
                    mExternalStorageWriteable = false;
                    mCopyResult += "storage is read-only.";
                } else {
                    mExternalStorageAvailable = mExternalStorageWriteable = false;
                    mCopyResult += "storage not availabe.";
                }

                if (DBG) Log.d(TAG, "mExternalStorageAvailable: " + mExternalStorageAvailable +
                                    ", mExternalStorageWriteable: " + mExternalStorageWriteable);

                if (mExternalStorageAvailable && mExternalStorageWriteable) {
                    //copy to dest folder
                    if (DBG) Log.d(TAG, "Source Folder:" + sourceFolderPath);
                    mCopyResult += "Source Folder:" + sourceFolderPath + "\n";

                    for(String fileName : fileList) {
                        if (DBG) Log.d(TAG, "copy " + fileName );
                        mCopyResult += "copy " + fileName + "...";
                        File srcFile = new File(sourceFolderPath + "/" + fileName);
                        File dstFile = new File(mCurrentDestFolderPath   + "/" + fileName);
                        try {
                            DeviceTestAppConstants.copy(srcFile, dstFile);
                            mCopyResult += "ok\n";
                        } catch (Exception e) {
                            if (DBG) Log.d(TAG, e.toString());
                            mCopyResult += "fail\n" + e;
                        }
                    }
                    mCopyConfigSuccess = true;
                }else {
                    mCopyConfigSuccess = false;
                }
*/
            } else {
                if (DBG) Log.d(TAG, sourceFolderPath + " does not exist");
                mCopyResult += sourceFolderPath + " does not exist\n";
                mCopyConfigSuccess = false;
            }
            return successCounts;
        }

        @Override
        protected void onPreExecute(){
            mProgressDialog = ProgressDialog.show(DeviceTestApp.this, "Copy Config Task", "Please wait ...", true);
            mProgressDialog.setCancelable(true);
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(String... progress) {
        }

        @Override
        protected void onPostExecute(Integer counts) {
            if(mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            mResult.setText(mCopyResult);

            runTestMain();
        }
    }

}

