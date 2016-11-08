
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
import android.view.Display;
import android.view.WindowManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.KeyEvent;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.RadioGroup;

import java.util.ArrayList;
import com.mediatek.nfc.dta.NativeDtaManager;

public class PlatformTest extends Activity implements OnClickListener, NativeDtaManager.Callback {

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

    //private RadioButton mPolling;
    //private RadioButton mListen;
    private RadioGroup mRgMode;
    private ArrayList<RadioButton> mRbModeItems = new ArrayList<RadioButton>();

    private LinearLayout mSwioLayout;
    private RadioButton mSwioRadio1;
    private RadioButton mSwioRadio2;
    private RadioButton mSwioRadio3;

    private LinearLayout mUidLayout;
    private RadioButton mUidRadio1;
    private RadioButton mUidRadio2;
    private RadioButton mUidRadio3;

    private LinearLayout mDidLayout;
    private RadioButton mDidOn;
    private RadioButton mDidOff;

    private LinearLayout mFsciLayout;
    private Spinner mFsciSpinner;
    private int mCurrentPatterNumber;
    private int mCurrentFsci;
    private int mCurrentTestType = 0;

    private int mCurrentListenModeType;
    private RadioGroup mRgMsr3110ListenModeType;
    private ArrayList<RadioButton> mRbMsr3110ListenModeTypeItems = new ArrayList<RadioButton>();
    private View mMsr3110ListenModeView;

    private static final int LISTEN_MODE_TYPE_A = 0x01;
    private static final int LISTEN_MODE_TYPE_B = 0x02;
    private static final int LISTEN_MODE_TYPE_A_AND_B = 0x03;

    private static final int MODE_POLLING = 0x00; //(DTA_TEST_PLATFORM)
    private static final int MODE_LISTEN = 0x03;  //(DTA_TEST_LISTEN)

    private static final int MSG_UPDATE_MESSAGE = 1001;
    private static final int MSG_SWITCH_TEST_STATE = 1002;

    private Vibrator mVibrator;
    static final long[] VIBRATION_PATTERN = {0, 100, 10000};
    static final long[] VIBRATION_PATTERN_2 = {0, 200, 500};

    private boolean mIsStarted;
    private int mChipVersion;
    private String mConfigPath;

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
        setContentView(R.layout.platform_tset);

        mDtaManager = new NativeDtaManager();

        //parse intent
        Intent intent = getIntent();
    	String action = intent.getAction();
        if(action != null){
            if (action.equals(DeviceTestAppConstants.ACTION_DTA_PLATFORM_START)) {
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
        msg.what = PlatformTest.MSG_UPDATE_MESSAGE;
        msg.obj = null;
        mHandler.sendMessage(msg);
    }

    @Override
    public void switchTestState() {
        Message msg = mHandler.obtainMessage();
        msg.what = PlatformTest.MSG_SWITCH_TEST_STATE;
        msg.obj = null;
        mHandler.sendMessage(msg);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.mode_polling:
            case R.id.mode_listen:
                initButton();
                break;

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
            Log.d(TAG, "fail onAttachedToWindow:null");
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
        //init Sound
        mDtaManager.initSoundPool(this);
        mDtaManager.initialize(this);
    }

    private void disableNfc(){
        if (DBG) Log.d(TAG, "PlatformTest disable Nfc");
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        if(adapter != null){
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
                    mProgressDialog = ProgressDialog.show(PlatformTest.this, "Disable Nfc", "Please wait ...", true);
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
        Display display = getWindowManager().getDefaultDisplay();
        int width = display.getWidth();

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
        mStartButton.setWidth(width/2);

        mStopButton = (Button) findViewById(R.id.stop_test);
        mStopButton.setOnClickListener(this);
        mStopButton.setWidth(width/2);
        mStopButton.setEnabled(false);

        //mPolling = (RadioButton) findViewById(R.id.mode_polling);
        //mPolling.setOnClickListener(this);

        //mListen = (RadioButton) findViewById(R.id.mode_listen);
        //mListen.setOnClickListener(this);
        mRgMode = (RadioGroup) findViewById(R.id.mode_select);
        mRbModeItems.add((RadioButton) findViewById(R.id.mode_polling));
        mRbModeItems.add((RadioButton) findViewById(R.id.mode_listen));
        mRgMode
            .setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {

                    mCurrentTestType = getSelectionId(checkedId);
                    if (DBG) Log.d(TAG, "onCheckedChanged :mCurrentTestType"+mCurrentTestType);
                }
                private int getSelectionId(int radioId) {
// 2016/06/11 bug fix
//                    final int[] idxs =
//                        {
//                          R.id.mode_polling,
//                          R.id.mode_listen,
//                          };
//                    final int[] ids =
//                        {
//                          MODE_POLLING,
//                          MODE_LISTEN,
//                          };
//
//                    for (int i = 0; i < idxs.length; i++) {
//                        if (idxs[i] == radioId) {
//                            return ids[i];
//                        }
//                    }
//                    Log.e(TAG, "Ghost RadioGroup checkId " + radioId);
                int type =-1;
                RadioGroup getType = (RadioGroup) findViewById(R.id.mode_select);
                switch(getType.getCheckedRadioButtonId()){
                    case R.id.mode_polling:
                         type=MODE_POLLING;
                            break;
                    case R.id.mode_listen:
                         type=MODE_LISTEN;
                            break;
                    default :
                         type=MODE_POLLING; // default polling
                        }
                    return type;
                }
            });



        mSwioLayout = (LinearLayout) findViewById(R.id.layout_swio);

        mSwioRadio1 = (RadioButton) findViewById(R.id.swio_1);
        mSwioRadio2 = (RadioButton) findViewById(R.id.swio_2);
        mSwioRadio3 = (RadioButton) findViewById(R.id.swio_3);

        mUidLayout = (LinearLayout) findViewById(R.id.layout_uid);

        mUidRadio1 = (RadioButton) findViewById(R.id.uid_1);
        mUidRadio2 = (RadioButton) findViewById(R.id.uid_2);
        mUidRadio3 = (RadioButton) findViewById(R.id.uid_3);

        mDidLayout = (LinearLayout) findViewById(R.id.layout_did);

        mDidOn = (RadioButton) findViewById(R.id.did_on);
        mDidOff = (RadioButton) findViewById(R.id.did_off);

        mFsciLayout = (LinearLayout) findViewById(R.id.layout_fsci);

        //fsci spinner
        mFsciSpinner = (Spinner)findViewById(R.id.fsci_spinner);

        ArrayAdapter fsciAdapter = ArrayAdapter.createFromResource(this, R.array.fsci_table,
                      android.R.layout.simple_spinner_item);
        fsciAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mFsciSpinner.setAdapter(fsciAdapter);

        mFsciSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                mCurrentFsci = arg2;
                if (DBG) Log.d(TAG, "mCurrentFsci : " + mCurrentFsci + ", " + arg2);
            }

            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });

        initButton();
    }

    private void initButton()
    {
        //mPolling.setEnabled(true);
        //mListen.setEnabled(true);

        mRbModeItems.get(0).setEnabled(true);
        mRbModeItems.get(1).setEnabled(true);

        mStartButton.setEnabled(true);
        mStopButton.setEnabled(false);

        //if(mPolling.isChecked())
        if (mRbModeItems.get(0).isChecked())
        {
            mSwioLayout.setVisibility(LinearLayout.GONE);
            mUidLayout.setVisibility(LinearLayout.GONE);
            mDidLayout.setVisibility(LinearLayout.GONE);
            mFsciLayout.setVisibility(LinearLayout.GONE);
        }
        else
        {
            //mSwioLayout.setVisibility(LinearLayout.VISIBLE);
            //mUidLayout.setVisibility(LinearLayout.VISIBLE);
            //mDidLayout.setVisibility(LinearLayout.VISIBLE);
            //mFsciLayout.setVisibility(LinearLayout.VISIBLE);
            mSwioLayout.setVisibility(LinearLayout.GONE);
            mUidLayout.setVisibility(LinearLayout.GONE);
            mDidLayout.setVisibility(LinearLayout.GONE);
            mFsciLayout.setVisibility(LinearLayout.GONE);
        }
    }

    private int getChipVersion() {
        //chip version
        int chip = mDtaManager.getChipVersion();
        if (chip == 0x02) {
            mResultText.setText("MT6605 Test");
        } else {
            mResultText.setText("Others");
        }
        return chip;
    }

    private void setInProgress(boolean inProgress) {
        mStartButton.setEnabled(!inProgress);
        mStopButton.setEnabled(inProgress);
        //mPolling.setEnabled(!inProgress);
        //mListen.setEnabled(!inProgress);
        mRbModeItems.get(0).setEnabled(!inProgress);
        mRbModeItems.get(1).setEnabled(!inProgress);
        mUidRadio1.setEnabled(!inProgress);
        mUidRadio2.setEnabled(!inProgress);
        mUidRadio3.setEnabled(!inProgress);
        mDidOn.setEnabled(!inProgress);
        mDidOff.setEnabled(!inProgress);
        mPatternNumberSpinner.setEnabled(!inProgress);
        mFsciSpinner.setEnabled(!inProgress);

        mIsStarted = inProgress;
    }

    private void runStartTest() {
        if (DBG) Log.d(TAG, "Start Test , Pattern Number is " + mCurrentPatterNumber);
        mResultText.setText("");
        setInProgress(true);
        mVibrator.vibrate(VIBRATION_PATTERN, -1);
        new StartTestTask().execute(0);

    }

    private void runStopTest() {
        if (DBG) Log.d(TAG, "Stop Test");

        new StopTestTask().execute(0);
    }


    private void runReset() {
        new ResetTask().execute(0);
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
            mProgressDialog = ProgressDialog.show(PlatformTest.this, "Start Test", "Please wait ...", true);
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
            mChipVersion = getChipVersion();
        }
    }



    class StartTestTask extends AsyncTask<Integer,String,Integer> {
        private ProgressDialog mProgressDialog;

        @Override
        protected Integer doInBackground(Integer... count) {

            Integer successCounts = new Integer(0);

            //polling or listen
            //if(mPolling.isChecked())
            //{
            //    mCurrentTestType = 0;   //DTA_TEST_PLATFORM
            //}
            //else
            //{
            //    mCurrentTestType = 3;   //DTA_TEST_LISTEN
            //}

            //swio
            if(mSwioRadio1.isChecked())
            {
                mDtaManager.mListenSwio = 1;
            }
            else if(mSwioRadio2.isChecked())
            {
                mDtaManager.mListenSwio = 2;
            }
            else
            {
                mDtaManager.mListenSwio = 3;
            }

            //uid
            if(mUidRadio1.isChecked())
            {
                mDtaManager.mListenUidLevel = 1;
            }
            else if(mUidRadio2.isChecked())
            {
                mDtaManager.mListenUidLevel = 2;
            }
            else
            {
                mDtaManager.mListenUidLevel = 3;
            }

            //did
            if(mDidOn.isChecked())
            {
                mDtaManager.mListenDidSupport = 1;
            }
            else
            {
                mDtaManager.mListenDidSupport = 0;
            }

            //-------- start test -----------
            {
                if (DBG) Log.d(TAG, "set config path " + mConfigPath);
                mDtaManager.setDtaConfigPath(mConfigPath);
                mDtaManager.enableDiscovery(mCurrentPatterNumber, mCurrentTestType);
            }

            return successCounts;
        }

        @Override
        protected void onPreExecute(){
            mProgressDialog = ProgressDialog.show(PlatformTest.this, "Start Test", "Please wait ...", true);
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
        }
    }

    class StopTestTask extends AsyncTask<Integer,String,Integer> {
        private ProgressDialog mProgressDialog;

        @Override
        protected Integer doInBackground(Integer... count) {

            Integer successCounts = new Integer(0);

            //-------- stop test -----------
            {
                mDtaManager.disableDiscovery();
            }

            return successCounts;
        }

        @Override
        protected void onPreExecute(){
            mProgressDialog = ProgressDialog.show(PlatformTest.this, "Stop Test", "Please wait ...", true);
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
        }
    }


    class ResetTask extends AsyncTask<Integer,String,Integer> {
        private ProgressDialog mProgressDialog;

        @Override
        protected Integer doInBackground(Integer... count) {

            Integer successCounts = new Integer(0);

            return successCounts;
        }

        @Override
        protected void onPreExecute(){
            mProgressDialog = ProgressDialog.show(PlatformTest.this, "Reset", "Please wait ...", true);
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
