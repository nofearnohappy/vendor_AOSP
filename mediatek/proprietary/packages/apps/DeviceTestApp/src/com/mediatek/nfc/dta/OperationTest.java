
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
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import android.widget.Button;
import android.widget.TextView;
import android.os.Vibrator;   //Vibtator


import com.mediatek.nfc.dta.NativeDtaManager;

public class OperationTest extends Activity implements OnClickListener {

    private static final String TAG = "DTA";
    private static final boolean DBG = true;
    private boolean mNfcEnabled = false;

    private ProgressDialog mProgressDialog;
    private IntentFilter mIntentFilter;
    private NativeDtaManager mDtaManager;
    private Button mStartButton;
    private Button mStopButton;

    private Spinner mPatternNumberSpinner;
    private TextView mResultText;
    private int mCurrentPatterNumber;
    private static int mCurrentTestType = 1;

    private String mConfigPath;

    private static final int MSG_UPDATE_MESSAGE = 1001;
    private static final int MSG_SWITCH_TEST_STATE = 1002;

    private Vibrator mVibrator;
    static final long[] VIBRATION_PATTERN = {0, 100, 10000};
    static final long[] VIBRATION_PATTERN_2 = {0, 200, 500};

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

    final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_MESSAGE:
                    mResultText.setText( mResultText.getText() + "\n" +
                                         mDtaManager.dumpMessage());
                    break;
                case  MSG_SWITCH_TEST_STATE:
                    mResultText.setText( mResultText.getText() + "\n" +
                                         "Card detection !!!");
                    setInProgress(false);
                    mVibrator.vibrate(VIBRATION_PATTERN, -1);
                    mDtaManager.playSound(NativeDtaManager.SOUND_END);
                    break;
            }
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.operation_test);

        //parse intent
        Intent intent = getIntent();
    	String action = intent.getAction();
        if(action != null){
            if (action.equals(DeviceTestAppConstants.ACTION_DTA_OPERATION_START)) {
                Bundle bundle = intent.getExtras();
                if(bundle != null){
                    mConfigPath = bundle.getString(DeviceTestAppConstants.DTA_DEST_CONFIG_PATH, DeviceTestAppConstants.DTA_CONFIG_FOLDER_DEST);
                }
            if (DBG) Log.d(TAG,  "ConfigPath : " + mConfigPath);
            } 
        }else {
            if (DBG) Log.d(TAG, "Wrong action : " + action);
            mConfigPath = DeviceTestAppConstants.DTA_CONFIG_FOLDER_DEST;
        }
        
        initUI();
        mDtaManager = new NativeDtaManager();
        mDtaManager.initSoundPool(this);
        getChipVersion();
    }

    @Override
    protected void onDestroy() {
        if (mDtaManager != null) {
            mDtaManager.releaseSoundPool();
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_test:
                if (DBG) Log.d(TAG, "Start Test");
                runStartTest();
                break;

            case R.id.stop_test:
                if (DBG) Log.d(TAG, "Stop Test");
                runStopTest();
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

    private void initUI() {
        //vibrator
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        //pattern spinner
        mPatternNumberSpinner = (Spinner)findViewById(R.id.pattern_number_spinner);
        ArrayAdapter adapter = ArrayAdapter.createFromResource(this, R.array.pattern_number_table,
                      android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPatternNumberSpinner.setAdapter(adapter);

        mPatternNumberSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                mCurrentPatterNumber = arg2;
                if (DBG) Log.d(TAG, "mCurrentPatterNumber : " + mCurrentPatterNumber + ", " + arg2);
            }

            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });

        mResultText = (TextView) findViewById(R.id.test_result);

        mStartButton = (Button) findViewById(R.id.start_test);
        mStartButton.setOnClickListener(this);
        mStopButton = (Button) findViewById(R.id.stop_test);
        mStopButton.setOnClickListener(this);
        mStopButton.setEnabled(false);

     }

    private void getChipVersion() {
        //chip version
        int chip = mDtaManager.getChipVersion();
        if (chip == 0x02) {
            mResultText.setText("MT6605");
        } else {
            mResultText.setText("Others");
        }
    }

    private void setInProgress(boolean inProgress) {
        mStartButton.setEnabled(!inProgress);
        mStopButton.setEnabled(inProgress);
    }

    private void runStartTest() {
        if (DBG) Log.d(TAG, "Start Test , Pattern Number is " + mCurrentPatterNumber);
        mResultText.setText("");
        setInProgress(true);
        mVibrator.vibrate(VIBRATION_PATTERN, -1);
        runOperationTest();
        finish();
    }

    private void runStopTest() {
        if (DBG) Log.d(TAG, "Stop Test");
        setInProgress(false);
    }

    private void runOperationTest(){
        //notify 6605
        if (mDtaManager.getChipVersion() == 0x02) {
            if (DBG) Log.d(TAG, "set config path " + mConfigPath); 
            mDtaManager.setDtaConfigPath(mConfigPath);
            
            if (DBG) Log.d(TAG, "runOperationTest (6605), Set Pattern Number = " + mCurrentPatterNumber);            
            mDtaManager.setPatternNumber(mCurrentPatterNumber);
            mResultText.setText(mResultText.getText() + "\n Pattern Number " + mCurrentPatterNumber);
        }

    	Intent intent = new Intent();
        Bundle bundle = new Bundle();
        intent.setClass(OperationTest.this, NdefReadWrite.class);
    	intent.setAction(DeviceTestAppConstants.ACTION_OPERATION_TEST_START);
        bundle.putInt(DeviceTestAppConstants.DATA_PATTERN_NUMBER, mCurrentPatterNumber);
    	intent.putExtras(bundle);
    	startActivity(intent);
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
}







