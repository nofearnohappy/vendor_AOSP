
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
import android.widget.Button;
import android.widget.TextView;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.LinearLayout;
import java.util.ArrayList;
import android.os.Vibrator;   //Vibtator

public class SnepTest extends Activity implements OnClickListener {

    private static final String TAG = "DTA-SNEP";
    private static final boolean DBG = true;
    private boolean mSNEP_Extended = false;
    private NfcAdapter mAdapter;
    private boolean mNfcEnabled = false;
    private NativeDtaManager mDtaManager;

    //Button
    private Button mSendTestButton;
    private Button mStopTestButton;

    //Spinner
    private Spinner mRoleSpinner;
    private Spinner mTestDataSpinner;

    //TextView
    private TextView mResultText;

    //Role
    private RadioGroup mRgRole;
    private ArrayList<RadioButton> mRbRoleItems = new ArrayList<RadioButton>();

    //select value
    private int mCurrentRole;
    private int mCurrentProtocolType = 0;    //TypeA, TypeF-212, TypeF-424
    private int mCurrentRequestType = 0x02;   //default PUT
    private int mData;     //Test data

    private ProgressDialog mProgressDialog;
    private IntentFilter mIntentFilter;
    private IntentFilter mIntentFilter_evt;
    private IntentFilter mIntentFilter_client_res;
    private IntentFilter mIntentFilter_recv_server_req;

    private static final int REQUEST_GET = 0x01;
    private static final int REQUEST_PUT = 0x02;

    private static final String URN_SNEP_SERVER = "urn:nfc:sn:snep";
    private static final String URN_DTA_SERVER = "urn:nfc:sn:sneptest";

    private String Tmp_SENP_Data = "";

    byte[] DATA1 = new byte[] {(byte)0xC1, 0x01, 0x00, 0x00, 0x00, (byte)0x1E, 0x54, 0x02,
                               (byte)0x6C, (byte)0x61, (byte)0x4C, (byte)0x6F, (byte)0x72,
                               (byte)0x65, (byte)0x6D, 0x20, (byte)0x69, (byte)0x70, (byte)0x73,
                               (byte)0x75, (byte)0x6D, 0x20, (byte)0x64, (byte)0x6F, (byte)0x6C,
                               (byte)0x6F, (byte)0x72, 0x20, (byte)0x73, (byte)0x69, (byte)0x74,
                               0x20, (byte)0x61, (byte)0x6D, (byte)0x65, (byte)0x74, (byte)0x2E};
    byte[] DATA2 = new byte[] {(byte)0xC1, 0x01, 0x00, 0x00, 0x01, (byte)0xEF, 0x54, 0x02,
                               (byte)0x6C, (byte)0x61, (byte)0x4C, (byte)0x6F, (byte)0x72,
                               (byte)0x65, (byte)0x6D, 0x20, (byte)0x69, (byte)0x70, (byte)0x73,
                               (byte)0x75, (byte)0x6D, 0x20, (byte)0x64, (byte)0x6F, (byte)0x6C,
                               (byte)0x6F, (byte)0x72, 0x20, (byte)0x73, (byte)0x69, (byte)0x74,
                               0x20, (byte)0x61, (byte)0x6D, (byte)0x65, (byte)0x74, (byte)0x2C,
                               0x20, (byte)0x63, (byte)0x6F, (byte)0x6E, (byte)0x73, (byte)0x65,
                               (byte)0x63, (byte)0x74, (byte)0x65, (byte)0x74, (byte)0x75,
                               (byte)0x72, 0x20, (byte)0x61, (byte)0x64, (byte)0x69, (byte)0x70,
                               (byte)0x69, (byte)0x73, (byte)0x63, (byte)0x69, (byte)0x6E,
                               (byte)0x67, 0x20, (byte)0x65, (byte)0x6C, (byte)0x69, 0x74, 0x2E,
                               0x20, 0x50, 0x68, 0x61, 0x73, 0x65, 0x6C, 0x6C, 0x75, 0x73, 0x20,
                               0x61, 0x74, 0x20, 0x6C, 0x6F, 0x72, 0x65, 0x6D, 0x20, 0x6E, 0x75,
                               0x6E, 0x63, 0x2C, 0x20, 0x75, 0x74, 0x20, 0x76, 0x65, 0x6E, 0x65,
                               0x6E, 0x61, 0x74, 0x69, 0x73, 0x20, 0x71, 0x75, 0x61, 0x6D, 0x2E,
                               0x20, 0x45, 0x74, 0x69, 0x61, 0x6D, 0x20, 0x69, 0x64, 0x20, 0x64,
                               0x6F, 0x6C, 0x6F, 0x72, 0x20, 0x71, 0x75, 0x61, 0x6D, 0x2C, 0x20,
                               0x61, 0x74, 0x20, 0x76, 0x69, 0x76, 0x65, 0x72, 0x72, 0x61, 0x20,
                               0x64, 0x6F, 0x6C, 0x6F, 0x72, 0x2E, 0x20, 0x50, 0x68, 0x61, 0x73,
                               0x65, 0x6C, 0x6C, 0x75, 0x73, 0x20, 0x65, 0x75, 0x20, 0x6C, 0x61,
                               0x63, 0x75, 0x73, 0x20, 0x6C, 0x69, 0x67, 0x75, 0x6C, 0x61, 0x2C,
                               0x20, 0x71, 0x75, 0x69, 0x73, 0x20, 0x65, 0x75, 0x69, 0x73, 0x6D,
                               0x6F, 0x64, 0x20, 0x65, 0x72, 0x61, 0x74, 0x2E, 0x20, 0x53, 0x65,
                               0x64, 0x20, 0x66, 0x65, 0x75, 0x67, 0x69, 0x61, 0x74, 0x2C, 0x20,
                               0x6C, 0x69, 0x67, 0x75, 0x6C, 0x61, 0x20, 0x61, 0x74, 0x20, 0x6D,
                               0x6F, 0x6C, 0x6C, 0x69, 0x73, 0x20, 0x61, 0x6C, 0x69, 0x71, 0x75,
                               0x65, 0x74, 0x2C, 0x20, 0x6A, 0x75, 0x73, 0x74, 0x6F, 0x20, 0x6C,
                               0x61, 0x63, 0x75, 0x73, 0x20, 0x63, 0x6F, 0x6E, 0x64, 0x69, 0x6D,
                               0x65, 0x6E, 0x74, 0x75, 0x6D, 0x20, 0x65, 0x72, 0x6F, 0x73, 0x2C,
                               0x20, 0x6E, 0x6F, 0x6E, 0x20, 0x74, 0x69, 0x6E, 0x63, 0x69, 0x64,
                               0x75, 0x6E, 0x74, 0x20, 0x6E, 0x65, 0x71, 0x75, 0x65, 0x20, 0x69,
                               0x70, 0x73, 0x75, 0x6D, 0x20, 0x65, 0x75, 0x20, 0x72, 0x69, 0x73,
                               0x75, 0x73, 0x2E, 0x20, 0x53, 0x65, 0x64, 0x20, 0x61, 0x64, 0x69,
                               0x70, 0x69, 0x73, 0x63, 0x69, 0x6E, 0x67, 0x20, 0x64, 0x75, 0x69,
                               0x20, 0x65, 0x75, 0x69, 0x73, 0x6D, 0x6F, 0x64, 0x20, 0x74, 0x65,
                               0x6C, 0x6C, 0x75, 0x73, 0x20, 0x75, 0x6C, 0x6C, 0x61, 0x6D, 0x63,
                               0x6F, 0x72, 0x70, 0x65, 0x72, 0x20, 0x6F, 0x72, 0x6E, 0x61, 0x72,
                               0x65, 0x2E, 0x20, 0x50, 0x68, 0x61, 0x73, 0x65, 0x6C, 0x6C, 0x75,
                               0x73, 0x20, 0x6D, 0x61, 0x74, 0x74, 0x69, 0x73, 0x20, 0x72, 0x69,
                               0x73, 0x75, 0x73, 0x20, 0x65, 0x74, 0x20, 0x6C, 0x65, 0x63, 0x74,
                               0x75, 0x73, 0x20, 0x65, 0x75, 0x69, 0x73, 0x6D, 0x6F, 0x64, 0x20,
                               0x65, 0x75, 0x20, 0x66, 0x65, 0x72, 0x6D, 0x65, 0x6E, 0x74, 0x75,
                               0x6D, 0x20, 0x73, 0x65, 0x6D, 0x20, 0x63, 0x75, 0x72, (byte)0x73,
                               (byte)0x75, (byte)0x73, (byte)0x2E, 0x20, 0x50, (byte)0x68,
                               (byte)0x61, (byte)0x73, (byte)0x65, (byte)0x6C, 0x6C, (byte)0x75,
                               (byte)0x73, 0x20, (byte)0x74, (byte)0x72, (byte)0x69, (byte)0x73,
                               (byte)0x74, (byte)0x69, (byte)0x71, (byte)0x75, (byte)0x65, 0x20,
                               0x63, (byte)0x6F, (byte)0x6E, (byte)0x73, (byte)0x65, (byte)0x63,
                               (byte)0x74, (byte)0x65, (byte)0x74, (byte)0x75, (byte)0x72, 0x20,
                               (byte)0x6D, 0x61, (byte)0x75, (byte)0x72, (byte)0x69, (byte)0x73,
                               0x20, (byte)0x65, (byte)0x75, 0x20, (byte)0x70, (byte)0x6F,
                               (byte)0x72, (byte)0x74, (byte)0x74, (byte)0x69, (byte)0x74,
                               (byte)0x6F, (byte)0x72, 0x2E, 0x20, 0x53, (byte)0x65, 0x64, 0x20,
                               (byte)0x6C, (byte)0x6F, (byte)0x62, (byte)0x6F, (byte)0x72,
                               (byte)0x74, (byte)0x69, (byte)0x73, 0x20, (byte)0x70, (byte)0x6F,
                               (byte)0x72, (byte)0x74, (byte)0x74, (byte)0x69, (byte)0x74,
                               (byte)0x6F, (byte)0x72, 0x20, (byte)0x6F, (byte)0x72, (byte)0x63,
                               (byte)0x69, 0x2E};

    private Vibrator mVibrator;
    static final long[] VIBRATION_PATTERN = {0, 100, 10000};

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (NfcAdapter.ACTION_ADAPTER_STATE_CHANGED.equals(action)) {
                handleNfcStateChanged(intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE,
                        NfcAdapter.STATE_OFF));
            }
                        if (DeviceTestAppConstants.ACTION_LLCP_EVT.equals(action)) {
                            //LLCP activated/deactivated handle
                 handleLLCPStatusChanged(intent.getIntExtra(DeviceTestAppConstants.LLCP_STATUS,-1));
                        }
                        if (DeviceTestAppConstants.ACTION_SNEP_CLIENT_RESPONSE.equals(action)) {
                            //SNEP client response handle
              handleSNEPResData(intent.getIntExtra(DeviceTestAppConstants.SNEP_CLIENT_RES_CODE,-1),
                                intent.getStringExtra(DeviceTestAppConstants.SNEP_CLIENT_RES_DATA));
                        }
                        if (DeviceTestAppConstants.ACTION_SNEP_SERVER_REQ.equals(action)) {
                            //SNEP server receive request, AF notify APK handle
                   handleSNEPServerReq(intent.getIntExtra(DeviceTestAppConstants.SNEP_REQ_CODE,-1),
                   intent.getStringExtra(DeviceTestAppConstants.SNEP_REQ_DATA),
                   intent.getStringExtra(DeviceTestAppConstants.SNEP_SERVER_SN));
                        }
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.snep_test);
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        initUI();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.test_send:
                if (DBG) Log.d(TAG, "Test start");
                runSendTest();
                break;

            case R.id.test_stop:
                if (DBG) Log.d(TAG, "Test stop");
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


//    private void disableNfc(){
//        if (DBG) Log.d(TAG, "disable Nfc");
//        mAdapter = NfcAdapter.getDefaultAdapter(this);
//        if (mAdapter.isEnabled()) {
//            //mIntentFilter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
//            //registerReceiver(mReceiver, mIntentFilter);
//            if (mAdapter.disable()) {
//                mProgressDialog = ProgressDialog.show(SnepTest.this, "Disable Nfc",
//                      "Please wait ...", true);
//                mProgressDialog.show();
//            }
//        }
//    }
    /**
     *
     * 2016/06/08  Nfc must to be disable , kill nfcstackp
     */
    private void disableNfc(){
    if (DBG) Log.d(TAG, "SnepTest disable Nfc");

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
                mProgressDialog = ProgressDialog.show(SnepTest.this, "Disable Nfc",
                                                                       "Please wait ...", true);
                mProgressDialog.show();
            }
        } else {
            if (DBG) Log.d(TAG, "Nfc is off");
        }
    }

    private void enableNfc() {
    if (DBG) Log.d(TAG, "enable Nfc");
        if ( mAdapter == null) {
            mAdapter = NfcAdapter.getDefaultAdapter(this);
        }
        if ( !mAdapter.isEnabled()) {
            mIntentFilter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
            mIntentFilter_evt = new IntentFilter(DeviceTestAppConstants.ACTION_LLCP_EVT);
            mIntentFilter_client_res =
                              new IntentFilter(DeviceTestAppConstants.ACTION_SNEP_CLIENT_RESPONSE);
            mIntentFilter_recv_server_req =
                                 new IntentFilter(DeviceTestAppConstants.ACTION_SNEP_SERVER_REQ);
            registerReceiver(mReceiver, mIntentFilter);
            registerReceiver(mReceiver, mIntentFilter_evt);
            registerReceiver(mReceiver, mIntentFilter_client_res);
            registerReceiver(mReceiver, mIntentFilter_recv_server_req);
            if(mAdapter.enable()) {
                mProgressDialog = ProgressDialog.show(SnepTest.this, "Enable Nfc",
                                                      "Please wait ...", true);
                mProgressDialog.show();
            }

            //when SNEP client to send request
            if(mCurrentRole == 0)
            {
                snepClientRequest();
            }

    }
    }


    private void initUI() {
        Display display = getWindowManager().getDefaultDisplay();
        int width = display.getWidth();

        //vibrator
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        mSendTestButton = (Button) findViewById(R.id.test_send);
        mSendTestButton.setOnClickListener(this);
        mSendTestButton.setWidth(width/2);

        mStopTestButton = (Button) findViewById(R.id.test_stop);
        mStopTestButton.setOnClickListener(this);
        mStopTestButton.setWidth(width/2);
        mStopTestButton.setEnabled(false);

        mResultText = (TextView) findViewById(R.id.test_result);

        //init spinner
        mRoleSpinner = (Spinner)findViewById(R.id.test_role);
        mTestDataSpinner = (Spinner)findViewById(R.id.test_data);

        //Role
        ArrayAdapter adapter = ArrayAdapter.createFromResource(this, R.array.snep_role_table,
                      android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mRoleSpinner.setAdapter(adapter);

        mRoleSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                mCurrentRole= arg2;
                if (DBG) Log.d(TAG, "mCurrentRole : " + mCurrentRole );
                setupTestGroup(mCurrentRole);
    }

            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });

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

        //Type
        mRgRole = (RadioGroup) findViewById(R.id.request_select);
        mRbRoleItems.add((RadioButton) findViewById(R.id.request_put));
        mRbRoleItems.add((RadioButton) findViewById(R.id.request_get));
        mRgRole
            .setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    mCurrentRequestType = getSelectionId(checkedId);
                }
                private int getSelectionId(int radioId) {
                    final int[] idxs =
                        {
                          R.id.request_put,
                          R.id.request_get,
                          };
                    final int[] ids =
                        {
                          REQUEST_PUT,
                          REQUEST_GET,
                          };

                    for (int i = 0; i < idxs.length; i++) {
                        if (idxs[i] == radioId) {
                            return ids[i];
                        }
                    }
                    Log.e(TAG, "Ghost RadioGroup checkId " + radioId);

                    return REQUEST_PUT;
                }
            });

        //Data
        ArrayAdapter data_adapter = ArrayAdapter.createFromResource(this, R.array.snep_data_table,
                      android.R.layout.simple_spinner_item);
        data_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mTestDataSpinner.setAdapter(data_adapter);

        mTestDataSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                mData= arg2;
                if (DBG) Log.d(TAG, "mData : " + mData );
                //setupTestGroup(mCurrentRole);
    }

            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });

    }


    private void setupTestGroup (int role) {
        ArrayAdapter adapter;
        TextView type_TextView,data_TextView;
        LinearLayout type_llayout1,type_llayout2,data_llayout1,data_llayout2;

        type_TextView = (TextView) findViewById(R.id.request_type_testview);
        type_llayout1 = (LinearLayout) findViewById(R.id.request_type_LLayout1);
        type_llayout2 = (LinearLayout) findViewById(R.id.request_type_LLayout2);
        data_TextView = (TextView) findViewById(R.id.request_data_view);
        data_llayout1 = (LinearLayout) findViewById(R.id.request_data_LLayout1);
        data_llayout2 = (LinearLayout) findViewById(R.id.request_data_LLayout2);

        switch (role) {
            case 0 : //client
                mTestDataSpinner.setVisibility(View.VISIBLE);
                type_TextView.setVisibility(View.VISIBLE);
                type_llayout1.setVisibility(View.VISIBLE);
                type_llayout2.setVisibility(View.VISIBLE);
                data_TextView.setVisibility(View.VISIBLE);
                data_llayout1.setVisibility(View.VISIBLE);
                data_llayout2.setVisibility(View.VISIBLE);
                break;
            case 1 : //server
                mTestDataSpinner.setVisibility(View.GONE);
                type_TextView.setVisibility(View.GONE);
                type_llayout1.setVisibility(View.GONE);
                type_llayout2.setVisibility(View.GONE);
                data_TextView.setVisibility(View.GONE);
                data_llayout1.setVisibility(View.GONE);
                data_llayout2.setVisibility(View.GONE);
                break;
            default :
                Log.d(TAG, "Ghost ...");
                break;
        }

    }

    private void runSendTest() {

        int mCurrentPatternNO = 0x1200;

        //Client and Get will send Pattern no 0x1201,  TC_C_GET_BV_03 workround
        if((mCurrentRole == 0) && (mCurrentRequestType == REQUEST_GET))
        {
            mCurrentPatternNO = 0x1201;
        }
        int sendPatternNO = mCurrentProtocolType | mCurrentPatternNO;

        Intent intent = new Intent();
        intent.setAction( DeviceTestAppConstants.ACTION_LLCP_SET_PATTERN);
        Bundle bundle = new Bundle();
        bundle.putInt( DeviceTestAppConstants.LLCP_PATTERN, sendPatternNO);
        intent.putExtras(bundle);
         sendBroadcast(intent);

        if (DBG) Log.d(TAG, "runSendTest, PatternNo:" + sendPatternNO);
        mResultText.setText("");
        setInProgress(true);
        mVibrator.vibrate(VIBRATION_PATTERN, -1);
        enableNfc();
    }

        private void runStopTest() {
        if (DBG) Log.d(TAG, "runStopTest");
        setInProgress(false);
        disableNfc();
    }

        private void setInProgress(boolean inProgress) {
        mSendTestButton.setEnabled(!inProgress);
        mStopTestButton.setEnabled(inProgress);
    }

    private void handleNfcStateChanged(int newState) {
        switch (newState) {
        case NfcAdapter.STATE_OFF:
            if(mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
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
        switch (newState) {
            case 0:
                mResultText.setText( mResultText.getText() + "\nLLCP Deactivated.");
                if(mCurrentRole == 0)
                {
                    snepClientRequest();
                }

                break;

            case 1:
                mResultText.setText( mResultText.getText() + "LLCP Activated.");
                //mDtaManager.playSound(NativeDtaManager.SOUND_END);

                //SNEP client send response event
                if(mCurrentRole == 0) {
                if(mCurrentRequestType == 0x01)
                {
                    mResultText.setText( mResultText.getText() + "\nSN:" + URN_DTA_SERVER);
                }
                else if(mCurrentRequestType == 0x02)
                {
                    mResultText.setText( mResultText.getText() + "\nSN:" + URN_SNEP_SERVER);
                }
                if(mData == 0)
                {
                    mResultText.setText( mResultText.getText() + "\nData:data1");
                }
                else if(mData == 1)
                {
                    mResultText.setText( mResultText.getText() + "\nData:data2");
                }

                }
                break;

            default:
                Log.d(TAG, "ghost LLCP status");
                break;

        }
    }

    private void snepClientRequest() {
        Log.d(TAG, "Send request to SNEP server.");

        //Send SNEP client request to AF
        Intent intent = new Intent();
        intent.setAction(DeviceTestAppConstants.ACTION_SNEP_CLIENT_REQUEST);
        //Bundle bundle = new Bundle();
        intent.putExtra(DeviceTestAppConstants.SNEP_CLIENT_REQ_CODE, mCurrentRequestType);

        if(mCurrentRequestType == 0x01)
        {
            //mResultText.setText( mResultText.getText() + "\nSN:" + URN_DTA_SERVER);
            intent.putExtra(DeviceTestAppConstants.SNEP_CLIENT_SN, URN_DTA_SERVER);
        }
        else if(mCurrentRequestType == 0x02)
        {
            //mResultText.setText( mResultText.getText() + "\nSN:" + URN_SNEP_SERVER);
            intent.putExtra(DeviceTestAppConstants.SNEP_CLIENT_SN, URN_SNEP_SERVER);
        }

        if(mData == 0)
        {
            String DataString = new String(DATA1);
            //mResultText.setText( mResultText.getText() + "\nData:data1");
            intent.putExtra(DeviceTestAppConstants.SNEP_CLIENT_REQ_DATA, DATA1);
        }
        else if(mData == 1)
        {
            String DataString = new String(DATA2);
            //mResultText.setText( mResultText.getText() + "\nData:data2");
            intent.putExtra(DeviceTestAppConstants.SNEP_CLIENT_REQ_DATA, DATA2);
        }

        //intent.putExtras(bundle);
        sendBroadcast(intent);
    }

    private void handleSNEPResData(int resCode, String resData) {
        switch (resCode) {
            case 129:   // 81h
                mResultText.setText( mResultText.getText() + "\n" + resData);
                break;

            case 255:   // FF
                mResultText.setText( mResultText.getText() + "\nSNEP reject.");
                break;

            default:
                Log.d(TAG, "other SNEP res code:" + resCode);
                break;

        }
    }

    private void handleSNEPServerReq(int reqCode, String Data, String SN) {
        int res_code = 0;
        String res_data = "";

        Log.d(TAG, "SNEP Server received connection.");
        mResultText.setText( mResultText.getText() + "\nSNEP Server received connection.");
        mResultText.setText( mResultText.getText() + "\nSN:" + SN);
        mResultText.setText( mResultText.getText() + "\nData:" + Data);

        if(URN_DTA_SERVER.equals(SN)) {
            //Extended
            mSNEP_Extended = true;
        }
        else {
            //Default Server
            mSNEP_Extended = false;
        }

        if((!mSNEP_Extended) && (reqCode == REQUEST_GET)){
            res_code = 0xE0;   //Default no support GET, Not Implemented
        }
        else {
            res_code = 0x81;   //Success
        }

        if(reqCode == REQUEST_PUT){
            //keep the data from SNEP client
            Tmp_SENP_Data = Data;
            res_data = "";
        }
        else if(reqCode == REQUEST_GET){
            //response the data
            res_data = Tmp_SENP_Data;
        }

        //Send SNEP server received data to AF
        Intent intent = new Intent();
        intent.setAction(DeviceTestAppConstants.ACTION_SNEP_SERVER_RES);
        Bundle bundle = new Bundle();
        bundle.putInt(DeviceTestAppConstants.SNEP_RES_CODE, 0);
        bundle.putString(DeviceTestAppConstants.SNEP_RES_DATA, res_data);
        intent.putExtras(bundle);
        sendBroadcast(intent);
    }

}



