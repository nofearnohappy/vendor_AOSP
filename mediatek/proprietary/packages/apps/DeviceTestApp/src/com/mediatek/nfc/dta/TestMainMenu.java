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

import android.content.Context;

import com.mediatek.nfc.dta.NativeDtaManager;

public class TestMainMenu extends Activity implements OnClickListener {

    private static final String TAG = "DTA";
    private static final boolean DBG = true;
    private boolean mNfcEnabled = false;

    //public static Context sContext;

    private ProgressDialog mProgressDialog;
    private IntentFilter mIntentFilter;

    private Button mPlatformTestButton;
    private Button mOperationTestButton;
    private Button mP2PTestButton;
    private Button mSWPTestButton;
    private Button mLLCPTestButton;
    private NfcAdapter mAdapter;
    private TextView mInstrumentName;

    private static final int MSG_UPDATE_MESSAGE = 1001;
    private static final int MSG_SWITCH_TEST_STATE = 1002;

    private Vibrator mVibrator;
    static final long[] VIBRATION_PATTERN = {0, 100, 10000};
    static final long[] VIBRATION_PATTERN_2 = {0, 200, 500};

    private String mInstrument;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_main_menu);

        //sContext = this; // d-load

        mAdapter = NfcAdapter.getDefaultAdapter(this);

        initUI();

        //parse intent
        Intent intent = getIntent();
        if (intent != null) {
        	String action = intent.getAction();
            if (action != null && action.equals(DeviceTestAppConstants.ACTION_DTA_MAIN_START)) {
                Bundle bundle = intent.getExtras();
                if(bundle != null){
                    mInstrument = bundle.getString(DeviceTestAppConstants.DTA_INSTRUMENT_NAME, "Default");
                    mConfigPath = bundle.getString(DeviceTestAppConstants.DTA_DEST_CONFIG_PATH, DeviceTestAppConstants.DTA_CONFIG_FOLDER_DEST);
                    mInstrumentName.setText(mInstrument);
                }
                if (DBG) Log.d(TAG, "Instrument : " + mInstrument + ", ConfigPath : " + mConfigPath);
            }
        } else {
            if (DBG) Log.d(TAG, "Intent is null");
        }

            new InitTestTask().execute(0);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.platform_test:
                if (DBG) Log.d(TAG, "Platform Test");
                runPlatformTest();
                break;

            case R.id.operation_test:
                if (DBG) Log.d(TAG, "Operation Test");
                runOperationTest();
                break;

            case R.id.p2p_test:
                if (DBG) Log.d(TAG, "P2P Test");
                runP2PTest();
                break;

            case R.id.swp_test:
                if (DBG) Log.d(TAG, "SWP Test");
                runSWPTest();
                break;

            case R.id.llcp_test:
                if (DBG) Log.d(TAG, "LLCP Test");
                runLLCPTest();
                break;

            case R.id.snep_test:
                if (DBG) Log.d(TAG, "SNEP Test");
                runSNEPTest();
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

    private void disableNfc(){
        if (DBG) Log.d(TAG, "TestMainMenu disable Nfc");

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
                mProgressDialog = ProgressDialog.show(TestMainMenu.this, "Disable Nfc", "Please wait ...", true);
                mProgressDialog.show();
            }
        } else {
            if (DBG) Log.d(TAG, "Nfc is off");
        }
    }

    private void enableNfc() {
        if (DBG) Log.d(TAG, "enable Nfc");
        if (mNfcEnabled) {
            mAdapter.enable();
        }
    }

    private void initUI() {
        //vibrator
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        mPlatformTestButton = (Button) findViewById(R.id.platform_test);
        mPlatformTestButton.setOnClickListener(this);

        mOperationTestButton = (Button) findViewById(R.id.operation_test);
        mOperationTestButton.setOnClickListener(this);

        mP2PTestButton = (Button) findViewById(R.id.p2p_test);
        mP2PTestButton.setOnClickListener(this);

        mSWPTestButton = (Button) findViewById(R.id.swp_test);
        mSWPTestButton.setOnClickListener(this);

        mLLCPTestButton = (Button) findViewById(R.id.llcp_test);
        mLLCPTestButton.setOnClickListener(this);

        mLLCPTestButton = (Button) findViewById(R.id.snep_test);
        mLLCPTestButton.setOnClickListener(this);

        mInstrumentName = (TextView) findViewById(R.id.instrument);
    }

    private void runPlatformTest() {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();

        intent.setClass(TestMainMenu.this, PlatformTest.class);
        intent.setAction(DeviceTestAppConstants.ACTION_DTA_PLATFORM_START);
        bundle.putString(DeviceTestAppConstants.DTA_DEST_CONFIG_PATH, mConfigPath );
        intent.putExtras(bundle);

        startActivity(intent);
        //finish();
    }

    private void runOperationTest() {
    	Intent intent = new Intent();
        Bundle bundle = new Bundle();

        intent.setClass(TestMainMenu.this, OperationTest.class);
        intent.setAction(DeviceTestAppConstants.ACTION_DTA_OPERATION_START);
        bundle.putString(DeviceTestAppConstants.DTA_DEST_CONFIG_PATH, mConfigPath );
        intent.putExtras(bundle);

        startActivity(intent);
        finish();
    }

    private void runP2PTest() {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();

        intent.setClass(TestMainMenu.this, P2PTest.class);
        intent.setAction(DeviceTestAppConstants.ACTION_DTA_P2P_START);
        bundle.putString(DeviceTestAppConstants.DTA_DEST_CONFIG_PATH, mConfigPath );
        intent.putExtras(bundle);

        startActivity(intent);
        //finish();
    }

    private void runSWPTest() {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();

        intent.setClass(TestMainMenu.this, SWPTest.class);
        intent.setAction(DeviceTestAppConstants.ACTION_DTA_SWP_START);
        bundle.putString(DeviceTestAppConstants.DTA_DEST_CONFIG_PATH, mConfigPath );
        intent.putExtras(bundle);

        startActivity(intent);
    }

    private void runLLCPTest() {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();

        intent.setClass(TestMainMenu.this, LlcpTest.class);
        intent.setAction(DeviceTestAppConstants.ACTION_DTA_LLCP_START);
        bundle.putString(DeviceTestAppConstants.DTA_DEST_CONFIG_PATH, mConfigPath );
        intent.putExtras(bundle);

        startActivity(intent);
    }

    private void runSNEPTest() {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();

        intent.setClass(TestMainMenu.this, SnepTest.class);
        intent.setAction(DeviceTestAppConstants.ACTION_DTA_SNEP_START);
        bundle.putString(DeviceTestAppConstants.DTA_DEST_CONFIG_PATH, mConfigPath );
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

                }
                if (mAdapter.isEnabled()) {
                    break;
                }
            }
            return successCounts;
        }

        @Override
        protected void onPreExecute(){
            mProgressDialog = ProgressDialog.show(TestMainMenu.this, "Init Test", "Please wait ...", true);
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
}