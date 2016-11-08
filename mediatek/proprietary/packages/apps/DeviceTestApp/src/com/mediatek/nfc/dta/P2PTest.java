
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

import android.widget.Toast;

import android.view.KeyEvent;
import android.view.WindowManager;

import com.mediatek.nfc.dta.NativeDtaManager;

public class P2PTest extends Activity implements OnClickListener, NativeDtaManager.Callback {

    private static final String TAG = "DTA";
    private static final boolean DBG = true;
    private boolean mNfcEnabled = false;

    private ProgressDialog mProgressDialog;
    private IntentFilter mIntentFilter;
    private NativeDtaManager mDtaManager;
    private Button mStartButton;
    private Button mStopButton;
    //private Button mResetButton;
    private Spinner mTestTypeSpinner;
    private Spinner mPatternNumberSpinner;
    private TextView mResultText;
    private int mCurrentPatterNumber;
    private static final int mCurrentTestType = 2;

    private static final int MSG_UPDATE_MESSAGE = 1001;
    private static final int MSG_SWITCH_TEST_STATE = 1002;

    private Vibrator mVibrator;
    static final long[] VIBRATION_PATTERN = {0, 100, 10000};
    static final long[] VIBRATION_PATTERN_2 = {0, 200, 500};

    private boolean mIsStarted;
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
                                         "detection !!!");
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
        setContentView(R.layout.p2p_test);

        //parse intent
        Intent intent = getIntent();
    	String action = intent.getAction();
        if(action != null){
            if (action.equals(DeviceTestAppConstants.ACTION_DTA_P2P_START)) {
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
        msg.what = P2PTest.MSG_UPDATE_MESSAGE;
        msg.obj = null;
        mHandler.sendMessage(msg);
    }

    @Override
    public void switchTestState() {
        Message msg = mHandler.obtainMessage();
        msg.what = P2PTest.MSG_SWITCH_TEST_STATE;
        msg.obj = null;
        mHandler.sendMessage(msg);
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


    @Override
    public void onAttachedToWindow() {
        if (DBG) Log.d(TAG, "onAttachedToWindow");
            if(this.getWindow() !=null){
            //this.getWindow().setType( WindowManager.LayoutParams.TYPE_KEYGUARD );

            // disable Home key
                // L remove FLAG_HOMEKEY_DISPATCHED
            //this.getWindow().addFlags( WindowManager.LayoutParams.FLAG_HOMEKEY_DISPATCHED );

            // full screen
            //this.getWindow().setFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN,
                              //WindowManager.LayoutParams.FLAG_FULLSCREEN );

            //super.onAttachedToWindow();
        }else{
            if (DBG) Log.d(TAG, "fail onAttachedToWindow: null");
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
           if (DBG) Log.d(TAG, "P2PTest disable Nfc");
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
                    mProgressDialog = ProgressDialog.show(P2PTest.this, "Disable Nfc", "Please wait ...", true);
                    mProgressDialog.show();
                }
            }

            if (DBG) Log.d(TAG, "Nfc is off");
                new InitTestTask().execute(0);
        }else{
            if(DBG) Log.d(TAG,"Device isn't support Nfc");
        }
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

        //mResetButton = (Button) findViewById(R.id.reset);
        //mResetButton.setOnClickListener(this);

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

    private void setInProgress(boolean inProgress) {
        mStartButton.setEnabled(!inProgress);
        mStopButton.setEnabled(inProgress);
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
            mProgressDialog = ProgressDialog.show(P2PTest.this, "Start Test", "Please wait ...", true);
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
            mDtaManager.enableDiscovery(mCurrentPatterNumber, mCurrentTestType);

            return successCounts;
        }

        @Override
        protected void onPreExecute(){
            mProgressDialog = ProgressDialog.show(P2PTest.this, "Start Test", "Please wait ...", true);
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
            mDtaManager.disableDiscovery();

            return successCounts;
        }

        @Override
        protected void onPreExecute(){
            mProgressDialog = ProgressDialog.show(P2PTest.this, "Stop Test", "Please wait ...", true);
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
            //-------- reset -----------
            //mDtaManager.deinitialize();

            //mDtaManager.reset();

            //mDtaManager.initialize(DeviceTestApp.this);

            return successCounts;
        }

        @Override
        protected void onPreExecute(){
            mProgressDialog = ProgressDialog.show(P2PTest.this, "Reset", "Please wait ...", true);
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

