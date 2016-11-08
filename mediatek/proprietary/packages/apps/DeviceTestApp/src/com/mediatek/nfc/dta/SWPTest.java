
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
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import android.widget.Button;
import android.widget.TextView;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.Toast;

import java.util.ArrayList;
import com.mediatek.nfc.dta.NativeDtaManager;

import android.os.Environment;
import java.io.IOException;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.BufferedReader;
//import java.io.DataOutputStream;
//import java.io.InputStream;



public class SWPTest extends Activity implements OnClickListener, NativeDtaManager.Callback {

    private static final String TAG = "DTA";
    private static final boolean DBG = true;
    private boolean mNfcEnabled = false;

    private ProgressDialog mProgressDialog;
    private IntentFilter mIntentFilter;
    private NativeDtaManager mDtaManager;

    private Button mQuickStartButton;
    private Button mQuickStopButton;
    private Button mNormalStartButton;
    private Button mNormalStopButton;
    private TextView mResultText;

    private RadioGroup mRgSWPTestType;
    private ArrayList<RadioButton> mRbSWPTestTypeItems = new ArrayList<RadioButton>();

    private static final int MSG_UPDATE_MESSAGE = 1001;
    private static final int MSG_SWITCH_TEST_STATE = 1002;

    private Vibrator mVibrator;
    static final long[] VIBRATION_PATTERN = {0, 100, 10000};
    static final long[] VIBRATION_PATTERN_2 = {0, 200, 500};

    private boolean mIsStarted;
    private static final int TEST_TYPE = 4;
    private int mCurrentSWPTestType;

    private static final int SWP_TEST_TYPE_1 = 1;
    private static final int SWP_TEST_TYPE_2 = 2;

    private static final int QUICK_MODE = 1;
    private static final int NORMAL_MODE = 2;
    private int mCurrentMode;

    private String mConfigPath;

    private static final String DTA_IN_PROGRESS_FILE = "mtknfcdtaInProgress.txt";


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
                                "Detection !!!");
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
            setContentView(R.layout.swp_tset);

            //parse intent
            Intent intent = getIntent();
            String action = intent.getAction();
            if (action.equals(DeviceTestAppConstants.ACTION_DTA_SWP_START)) {
                Bundle bundle = intent.getExtras();
                if(bundle != null){
                    mConfigPath = bundle.getString(DeviceTestAppConstants.DTA_DEST_CONFIG_PATH, DeviceTestAppConstants.DTA_CONFIG_FOLDER_DEST);
                }
                if (DBG) Log.d(TAG,  "ConfigPath : " + mConfigPath);
            } else {
                if (DBG) Log.d(TAG, "Wrong action : " + action);
                mConfigPath = DeviceTestAppConstants.DTA_CONFIG_FOLDER_DEST;
            }

            initUI();
            disableNfc();
        }

    @Override
        protected void onDestroy() {
            if (mDtaManager != null) {
                mDtaManager.releaseSoundPool();
                mDtaManager.deinitialize();
            }

            //enable Home Key
            // L remove FLAG_HOMEKEY_DISPATCHED
            //this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_HOMEKEY_DISPATCHED );
            super.onDestroy();
        }

    @Override
        public void notifyMessageListener() {
            //Update Message
            Message msg = mHandler.obtainMessage();
            msg.what = SWPTest.MSG_UPDATE_MESSAGE;
            msg.obj = null;
            mHandler.sendMessage(msg);
        }

    @Override
        public void switchTestState() {
            Message msg = mHandler.obtainMessage();
            msg.what = SWPTest.MSG_SWITCH_TEST_STATE;
            msg.obj = null;
            mHandler.sendMessage(msg);
        }

    @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.quick_start_test:
                    if (DBG) Log.d(TAG, "Quick Start Test");
                    runStartTest(QUICK_MODE);
                    break;

                case R.id.quick_stop_test:
                    if (DBG) Log.d(TAG, "Quick Stop Test");
                    runStopTest(QUICK_MODE);
                    break;

                case R.id.normal_start_test:
                    if (DBG) Log.d(TAG, "Normal Start Test");
                    runStartTest(NORMAL_MODE);
                    break;

                case R.id.normal_stop_test:
                    if (DBG) Log.d(TAG, "Normal Stop Test");
                    runStopTest(NORMAL_MODE);
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


    @Override
        public void onAttachedToWindow() {
            if (DBG) Log.d(TAG, "onAttachedToWindow");
            if(this.getWindow() != null){
                //this.getWindow().setType( WindowManager.LayoutParams.TYPE_KEYGUARD );

                // disable Home key
                // L remove FLAG_HOMEKEY_DISPATCHED
                //this.getWindow().addFlags( WindowManager.LayoutParams.FLAG_HOMEKEY_DISPATCHED );

                // full screen
                //this.getWindow().setFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN,
                                         //WindowManager.LayoutParams.FLAG_FULLSCREEN );

                //super.onAttachedToWindow();
            }else{
                if (DBG) Log.d(TAG, "fail onAttachedToWindow: getWindos is null!");
            }
        }


    @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            if (DBG) Log.d(TAG, "onKeyDown " + keyCode);

            if (mIsStarted) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_MENU:
                        if (DBG) Log.d(TAG, "KEYCODE_MENU");
                        return true;

                    case KeyEvent.KEYCODE_BACK:
                        if (DBG) Log.d(TAG, "KEYCODE_BACK");
                        Toast.makeText(this,"Please STOP test !",Toast.LENGTH_SHORT).show();
                        return true;

                    case KeyEvent.KEYCODE_HOME:
                        if (DBG) Log.d(TAG, "KEYCODE_HOME");
                        return true;

                    default:
                        //return true;
                        break;
                }
            }
            return super.onKeyDown(keyCode, event);
        }


    private void initTest() {
        mDtaManager = new NativeDtaManager();
        //init Sound
        mDtaManager.initSoundPool(this);
        mDtaManager.initialize(this);
    }

    private void disableNfc(){
        if (DBG) Log.d(TAG, "SWPTest disable Nfc");
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        if (adapter != null){
            mNfcEnabled = adapter.isEnabled();
            if (mNfcEnabled) {
                if (DBG) Log.d(TAG, "Nfc is on");
                   new NativeDtaManager().setDtaQuickMode(0);
                   if (DBG) Log.d(TAG, "[QE]Nfc is on,Now to disable NFC");
                   if (DBG) Log.d(TAG, "[QE]change Nfc Adapter to state_changed");
                mIntentFilter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
                   if (DBG) Log.d(TAG, "[QE]Start  DTA-JNI de-init");
                registerReceiver(mReceiver, mIntentFilter);
                   if (DBG) Log.d(TAG, "[QE]setDtaQuickMode =1 ");
                   new NativeDtaManager().setDtaQuickMode(1);
                if (adapter.disable()) {
                    mProgressDialog = ProgressDialog.show(SWPTest.this, "Disable Nfc", "Please wait ...", true);
                    mProgressDialog.show();
                }
            }
            if (DBG) Log.d(TAG, "Nfc is off");
                new InitTestTask().execute(0);
        }else{
            if (DBG) Log.d(TAG, "Device isn't support nfc");
        }

    }

    private void initUI() {
        //vibrator
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        mRgSWPTestType = (RadioGroup) findViewById(R.id.test_type);
        mRbSWPTestTypeItems.add((RadioButton) findViewById(R.id.test_type_1));
        mRbSWPTestTypeItems.add((RadioButton) findViewById(R.id.test_type_2));
        mRgSWPTestType
            .setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                    mCurrentSWPTestType = getSelectionId(checkedId);
                    mVibrator.vibrate(VIBRATION_PATTERN, -1);
                    mResultText.setText("Test Type : " + mCurrentSWPTestType);
                    }
                    private int getSelectionId(int radioId) {
                    final int[] idxs =
                    {
                    R.id.test_type_1,
                    R.id.test_type_2,
                    };
                    final int[] ids =
                    {
                    SWP_TEST_TYPE_1,
                    SWP_TEST_TYPE_2,
                    };

                    for (int i = 0; i < idxs.length; i++) {
                    if (idxs[i] == radioId) {
                        return ids[i];
                    }
                    }
                    Log.e(TAG, "Ghost RadioGroup checkId " + radioId);

                    return SWP_TEST_TYPE_2;  //default
                    }
            });

        mResultText = (TextView) findViewById(R.id.test_result);

        //quick mode
        mQuickStartButton = (Button) findViewById(R.id.quick_start_test);
        mQuickStartButton.setOnClickListener(this);
        mQuickStopButton = (Button) findViewById(R.id.quick_stop_test);
        mQuickStopButton.setOnClickListener(this);

        //normal mode
        mNormalStartButton = (Button) findViewById(R.id.normal_start_test);
        mNormalStartButton.setOnClickListener(this);
        mNormalStopButton = (Button) findViewById(R.id.normal_stop_test);
        mNormalStopButton.setOnClickListener(this);

        // default
        mRgSWPTestType.check(R.id.test_type_2);
        mQuickStopButton.setEnabled(false);
        mNormalStopButton.setEnabled(false);

        //set dta in progress
        if (checkDtaInProgress()){
            mCurrentMode = NORMAL_MODE;
            setInProgress(true);
        }

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
        if (QUICK_MODE == mCurrentMode) {
            mQuickStartButton.setEnabled(!inProgress);
            mQuickStopButton.setEnabled(inProgress);

            if (inProgress) {
                mNormalStartButton.setEnabled(false);
                mNormalStopButton.setEnabled(false);
            } else {
                mNormalStartButton.setEnabled(true);
            }

            mIsStarted = inProgress;
        } else if (NORMAL_MODE == mCurrentMode) {
            mNormalStartButton.setEnabled(!inProgress);
            mNormalStopButton.setEnabled(inProgress);

            if (inProgress) {
                mQuickStartButton.setEnabled(false);
                mQuickStopButton.setEnabled(false);
            } else {
                mQuickStartButton.setEnabled(true);
            }
            mIsStarted = false;
        }

    }

    private void runStartTest(int mode) {
        mCurrentMode = mode;
        if (mCurrentMode != QUICK_MODE && mCurrentMode != NORMAL_MODE) {
            if (DBG) Log.d(TAG, "Ghost mode, return.");
            return;
        }
        if (DBG) Log.d(TAG, "Start Test");
        mResultText.setText("");
        setInProgress(true);
        mVibrator.vibrate(VIBRATION_PATTERN, -1);
        new StartTestTask().execute(0);
    }

    private void runStopTest(int mode) {
        mCurrentMode = mode;
        if (mCurrentMode != QUICK_MODE && mCurrentMode != NORMAL_MODE) {
            if (DBG) Log.d(TAG, "Ghost mode, return.");
            return;
        }
        if (DBG) Log.d(TAG, "Stop Test");
        new StopTestTask().execute(0);
    }

    private boolean writeToFile(String filename, String word) {

        try {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_REMOVED) ||
                    Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
                if (DBG) Log.d(TAG, "sdcard is removed or read-only");
                return false;
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename, false));
            writer.write(word);
            writer.close();
        } catch (Exception e) {
            if (DBG) Log.d(TAG, "writeToFile : \n" + e);
            return false;
        }
        return true;
    }

    private boolean checkDtaInProgress(){
        boolean isDtaInProgress = false;
        try {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_REMOVED) ||
                    Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
                if (DBG) Log.d(TAG, "sdcard is removed or read-only");
                return isDtaInProgress;
            }
            String filename = Environment.getExternalStorageDirectory().toString() + "/" + DTA_IN_PROGRESS_FILE;
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String tmp = reader.readLine();
            reader.close();
            if (tmp != null && ( tmp.charAt(0) == '1')) {
                isDtaInProgress = true;
            }
        } catch (Exception e) {
            if (DBG) Log.d(TAG, "checkDtaInProgress : \n" + e);
            return isDtaInProgress;
        }
        return isDtaInProgress;
    }

    private void setDtaInProgress() {
        writeToFile(Environment.getExternalStorageDirectory().toString() + "/" + DTA_IN_PROGRESS_FILE, "1," + mCurrentSWPTestType);
    }

    private void unsetDtaInProgress() {
        writeToFile(Environment.getExternalStorageDirectory().toString() + "/" + DTA_IN_PROGRESS_FILE, "0");
    }

    class InitTestTask extends AsyncTask<Integer,String,Integer> {
        private ProgressDialog mProgressDialog;

        @Override
            protected Integer doInBackground(Integer... count) {

                Integer successCounts = new Integer(0);
                //-------- start test -----------
                initTest();

                return successCounts;
            }

        @Override
            protected void onPreExecute(){
                mProgressDialog = ProgressDialog.show(SWPTest.this, "Start Test", "Please wait ...", true);
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
                getChipVersion();
            }
    }



    class StartTestTask extends AsyncTask<Integer,String,Integer> {
        private ProgressDialog mProgressDialog;

        @Override
            protected Integer doInBackground(Integer... count) {

                Integer successCounts = new Integer(0);
                //-------- start test -----------
                if (DBG) Log.d(TAG, "set config path " + mConfigPath);
                mDtaManager.setDtaConfigPath(mConfigPath);
                if (NORMAL_MODE == mCurrentMode) {
                    // write dta inProgress file
                    setDtaInProgress();
                } else {
                    // quick mode
                    unsetDtaInProgress();
                    mDtaManager.enableDiscovery( mCurrentSWPTestType, TEST_TYPE);
                }

                return successCounts;
            }

        @Override
            protected void onPreExecute(){
                mProgressDialog = ProgressDialog.show(SWPTest.this, "Start Test", "Please wait ...", true);
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

                if (mCurrentMode == NORMAL_MODE) {
                    Toast.makeText(SWPTest.this,"Please REBOOT !",Toast.LENGTH_SHORT).show();
                }
            }
    }

    class StopTestTask extends AsyncTask<Integer,String,Integer> {
        private ProgressDialog mProgressDialog;

        @Override
            protected Integer doInBackground(Integer... count) {

                Integer successCounts = new Integer(0);
                //-------- stop test -----------
                if (NORMAL_MODE == mCurrentMode) {
                    // remove dta inProgress file
                    unsetDtaInProgress();
                } else {
                    // quick mode
                    mDtaManager.disableDiscovery();
                }

                return successCounts;
            }

        @Override
            protected void onPreExecute(){
                mProgressDialog = ProgressDialog.show(SWPTest.this, "Stop Test", "Please wait ...", true);
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
                setInProgress(false);

                if (mCurrentMode == NORMAL_MODE) {
                    Toast.makeText(SWPTest.this,"Please REBOOT !",Toast.LENGTH_SHORT).show();
                }
            }
    }

    private void handleNfcStateChanged(int newState) {
        switch (newState) {
            case NfcAdapter.STATE_OFF:
                if(mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
                unregisterReceiver(mReceiver);
                new InitTestTask().execute(0);

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

