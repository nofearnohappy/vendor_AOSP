
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
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import java.util.ArrayList;

import android.os.Vibrator;   //Vibtator
import android.view.WindowManager;

import com.mediatek.nfc.dta.NativeDtaManager;

public class LlcpTest extends Activity implements OnClickListener, NativeDtaManager.Callback {

    private static final String TAG = "DTA-LLCP";
    private static final boolean DBG = true;
    private NfcAdapter mAdapter;
    private boolean mNfcEnabled = false;
    private NativeDtaManager mDtaManager;

    //Role
    private RadioGroup mRgRole;
    private ArrayList<RadioButton> mRbRoleItems = new ArrayList<RadioButton>();

    //Button
    private Button mSendTestButton;
    private Button mStopButton;

    //Spinner
    private Spinner mTestGroupSpinner;

    //EditText
    private EditText mEditTextBox;

    //TextView
    private TextView mResultText;

    private ProgressDialog mProgressDialog;
    private IntentFilter mIntentFilter;
    private IntentFilter mIntentFilter_evt;

    private static final int ROLE_INITIATOR = 0x01;
    private static final int ROLE_TARGET = 0x02;

    private int mCurrentPatternNO = 0;
    private int mCurrentProtocolType = 0;    //TypeA, TypeF-212, TypeF-424
    private boolean mIsStarted;
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
            if (DeviceTestAppConstants.ACTION_LLCP_EVT.equals(action)) {
                handleLLCPStatusChanged(intent.getIntExtra(DeviceTestAppConstants.LLCP_STATUS,-1));
            }
        }
    };

    final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_MESSAGE:
                    mResultText.setText( mResultText.getText() + "\n" +mDtaManager.dumpMessage());
                    break;
                case  MSG_SWITCH_TEST_STATE:
                    mResultText.setText( mResultText.getText() + "\n" + "detection !!!");
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
        setContentView(R.layout.llcp_test);

        //parse intent
        Intent intent = getIntent();
        String action = intent.getAction();
        if (action.equals(DeviceTestAppConstants.ACTION_DTA_LLCP_START)) {
            Bundle bundle = intent.getExtras();
            mConfigPath = bundle.getString(DeviceTestAppConstants.DTA_DEST_CONFIG_PATH,
                                           DeviceTestAppConstants.DTA_CONFIG_FOLDER_DEST);
            if (DBG) Log.d(TAG,  "ConfigPath : " + mConfigPath);
        } else {
            if (DBG) Log.d(TAG, "Wrong action : " + action);
            mConfigPath = DeviceTestAppConstants.DTA_CONFIG_FOLDER_DEST;
        }
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        initUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void notifyMessageListener() {
        //Update Message
        Message msg = mHandler.obtainMessage();
        msg.what = LlcpTest.MSG_UPDATE_MESSAGE;
        msg.obj = null;
        mHandler.sendMessage(msg);
    }

    @Override
    public void switchTestState() {
        Message msg = mHandler.obtainMessage();
        msg.what = LlcpTest.MSG_SWITCH_TEST_STATE;
        msg.obj = null;
        mHandler.sendMessage(msg);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.llcptest_send:
                if (DBG) Log.d(TAG, "Start Test");
                runSendTest();
                break;

            case R.id.llcptest_stop:
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

        @Override
        public void onAttachedToWindow() {
        if (DBG) Log.d(TAG, "onAttachedToWindow");

            //this.getWindow().setType( WindowManager.LayoutParams.TYPE_KEYGUARD );

            // disable Home key
            // L remove FLAG_HOMEKEY_DISPATCHED
            //this.getWindow().addFlags( WindowManager.LayoutParams.FLAG_HOMEKEY_DISPATCHED );

            // full screen
            //this.getWindow().setFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN,
                                        //WindowManager.LayoutParams.FLAG_FULLSCREEN );

            //super.onAttachedToWindow();
        }

        private void initTest() {
        mDtaManager = new NativeDtaManager();
        //init Sound
        mDtaManager.initSoundPool(this);
        mDtaManager.initialize(this);
    }

//    private void disableNfc(){
//        if (DBG) Log.d(TAG, "disable Nfc");
//        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
//        if (adapter.isEnabled()) {
//            if (DBG) Log.d(TAG, "Nfc is on");
//                mIntentFilter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
//               mIntentFilter_evt = new IntentFilter(DeviceTestAppConstants.ACTION_LLCP_EVT);
//               registerReceiver(mReceiver, mIntentFilter);
//               registerReceiver(mReceiver, mIntentFilter_evt);
//               unregisterReceiver(mReceiver);
//            if (adapter.disable()) {
//                mProgressDialog = ProgressDialog.show(LlcpTest.this, "Disable Nfc",
//                               "Please wait ...", true);
//                mProgressDialog.show();
//            }
//        }
//    }
    /**
     *
     * 2016/06/08  Nfc must to be disable , kill nfcstackp
     */
    private void disableNfc(){
    if (DBG) Log.d(TAG, "Llcp disable Nfc");

        mNfcEnabled = mAdapter.isEnabled();

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
            if (mAdapter.disable()) {
                mProgressDialog = ProgressDialog.show(LlcpTest.this, "Disable Nfc",
                                                      "Please wait ...", true);
                mProgressDialog.show();
            }
        } else {
            if (DBG) Log.d(TAG, "Nfc is off");
        }
    }
        private void enableNfc(){
        if (DBG) Log.d(TAG, "enable Nfc");
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        if (!adapter.isEnabled()) {
            if (DBG) Log.d(TAG, "Nfc is off");
            mIntentFilter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
            mIntentFilter_evt = new IntentFilter(DeviceTestAppConstants.ACTION_LLCP_EVT);
            registerReceiver(mReceiver, mIntentFilter);
            registerReceiver(mReceiver, mIntentFilter_evt);
            if (adapter.enable()) {
                mProgressDialog = ProgressDialog.show(LlcpTest.this, "Enable Nfc",
                                                      "Please wait ...", true);
                mProgressDialog.show();
            }
        }
    }

    private void initUI() {
        //vibrator
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        Display display = getWindowManager().getDefaultDisplay();
        int width = display.getWidth();

        mSendTestButton = (Button) findViewById(R.id.llcptest_send);
        mSendTestButton.setOnClickListener(this);
        mSendTestButton.setWidth(width/2);

        mStopButton = (Button) findViewById(R.id.llcptest_stop);
        mStopButton.setOnClickListener(this);
        mStopButton.setWidth(width/2);
        mStopButton.setEnabled(false);

        mResultText = (TextView) findViewById(R.id.test_result);

        //init spinner
        mTestGroupSpinner = (Spinner)findViewById(R.id.llcptest_pattern_no);

        //Pattern number
        ArrayAdapter adapter = ArrayAdapter.createFromResource(this, R.array.llcp_group_table,
                      android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mTestGroupSpinner.setAdapter(adapter);

        mTestGroupSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                mCurrentPatternNO= arg2;
                    switch (mCurrentPatternNO) {
                        case 0:
                            mCurrentPatternNO = 0x1200;
                            if (DBG) Log.d(TAG, "CurrentPattern number : 0x1200");
                            break;
                        case 1:
                            mCurrentPatternNO = 0x1240;
                            if (DBG) Log.d(TAG, "CurrentPattern number : 0x1240");
                            break;
                        case 2:
                            mCurrentPatternNO = 0x1280;
                            if (DBG) Log.d(TAG, "CurrentPattern number : 0x1280");
                            break;
                    }
           }

            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });

        //Extend pattern number
        mEditTextBox = (EditText) findViewById(R.id.llcptest_extend_pattern_no);
        mEditTextBox.setText("0");
        /*mEditTextBox.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
             public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                 boolean handled = false;
                 if (actionId == EditorInfo.IME_ACTION_SEND) {
                     sendMessage();
                     handled = true;
                 }
                 return handled;
             }
        });*/

        //Protocol type
        mRgRole = (RadioGroup) findViewById(R.id.protocol_type_select);
        mRbRoleItems.add((RadioButton) findViewById(R.id.typeA));
        mRbRoleItems.add((RadioButton) findViewById(R.id.typeF212));
        mRbRoleItems.add((RadioButton) findViewById(R.id.typeF424));
        mRgRole
        .setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            mCurrentProtocolType = getSelectionId(checkedId);
        }
        private int getSelectionId(int radioId) {
            final int[] idxs =
            {
                R.id.typeA,
                R.id.typeF212,
                R.id.typeF424,
            };
            final int[] ids =
            {
                0x00,
                0x00010000,
                0x00020000,
            };

            for (int i = 0; i < idxs.length; i++) {
                if (idxs[i] == radioId) {
                    return ids[i];
                }
            }
            Log.e(TAG, "Ghost RadioGroup checkId " + radioId);

            return 0x00;
        }
        });
    }

        private void setInProgress(boolean inProgress) {
        mSendTestButton.setEnabled(!inProgress);
        mStopButton.setEnabled(inProgress);
        mIsStarted = inProgress;
    }

        private void getChipVersion() {
        //chip version
        int chip = mDtaManager.getChipVersion();
        if (chip == 0x01) {
            mResultText.setText("MSR3110");
        } else if (chip == 0x02) {
            mResultText.setText("MT6605");
        } else {
            mResultText.setText("Others");
        }
    }

    private void runSendTest() {

        int extendPatternNo = Integer.parseInt(mEditTextBox.getText().toString()) << 18;
        int sendPatternNO = extendPatternNo | mCurrentProtocolType | mCurrentPatternNO;

        Intent intent = new Intent();
        intent.setAction( DeviceTestAppConstants.ACTION_LLCP_SET_PATTERN);
        Bundle bundle = new Bundle();
        bundle.putInt( DeviceTestAppConstants.LLCP_PATTERN, sendPatternNO);
        intent.putExtras(bundle);
        sendBroadcast(intent);

        if (DBG) Log.d(TAG, "Start Test , Pattern Number is " + mCurrentPatternNO +
                       ",send intent parameter is " + sendPatternNO);
        mResultText.setText("");
        setInProgress(true);
        mVibrator.vibrate(VIBRATION_PATTERN, -1);
        enableNfc();
    }

        private void runStopTest() {
        int sendPatternNO = 0xFFFFFFFF;     //reset

        Intent intent = new Intent();
        intent.setAction( DeviceTestAppConstants.ACTION_LLCP_SET_PATTERN);
        Bundle bundle = new Bundle();
        bundle.putInt( DeviceTestAppConstants.LLCP_PATTERN, sendPatternNO);
        intent.putExtras(bundle);
        sendBroadcast(intent);

        if (DBG) Log.d(TAG, "Stop Test");
            setInProgress(false);
            disableNfc();
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
            mProgressDialog = ProgressDialog.show(LlcpTest.this, "Start Test",
                                                  "Please wait ...", true);
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

    private void handleNfcStateChanged(int newState) {
        switch (newState) {
            case NfcAdapter.STATE_OFF:
                if(mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
                //unregisterReceiver(mReceiver);
                //new InitTestTask().execute(0);
                break;

            case NfcAdapter.STATE_ON:
                if(mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
                break;

            case NfcAdapter.STATE_TURNING_ON:
                break;

            case NfcAdapter.STATE_TURNING_OFF:
                break;

        }
    }

    private void handleLLCPStatusChanged(int newState) {
                Log.d(TAG, "handleLLCPStatusChanged enter:");
                switch (newState) {
                    case 0:
                        mResultText.setText( mResultText.getText() + "LLCP Deactivated.\n");
                        break;

                    case 1:
                       mResultText.setText( mResultText.getText() + "LLCP Activated.\n");
                       //mDtaManager.playSound(NativeDtaManager.SOUND_END);
                       break;

                    default:
                       Log.d(TAG, "ghost LLCP status");
                       break;
                }
    }
}