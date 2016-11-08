package com.mediatek.wifi.hotspot.em;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.net.wifi.WifiManager;
import android.content.Context;
import android.os.ServiceManager;
import android.os.RemoteException;
import android.view.MotionEvent;
import android.graphics.PorterDuff;

public class HSPT_DelCred extends Activity {

    private Button      mButtonDelCredIndex;
    private Button      mButtonClrAllCred;
    private EditText    mEditTextDelCred;
    private EditText    mEditTextDelNetwork;
    private Button      mButtonDumpCred;
    private TextView    mTextViewDumpCred;
    private TextView    mTextViewCmd;
    private Button      mButtonHsStatus;
    private Button      mButtonHsNetwork;
    private Button      mButtonDelNetworkIndex;
    

    private WifiManager mWifiMgr = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hspt_delcred);

        mWifiMgr = (WifiManager)getSystemService(WIFI_SERVICE);
        if(mWifiMgr == null) {
            MtkLog.log("ERR: getSystemService WIFI_SERVICE failed");
            return;
        }

        initWidget();

    }

    //=================== initWidget ========================\\
    private void initWidget() {
        mButtonDelCredIndex = (Button)findViewById(R.id.Button_DelCred);
        mButtonDelNetworkIndex = (Button)findViewById(R.id.Button_DelNetwork);
        mEditTextDelCred = (EditText)findViewById(R.id.EditText_DelCred);
        mEditTextDelNetwork = (EditText)findViewById(R.id.EditText_DelNetwork);
        mButtonDumpCred = (Button)findViewById(R.id.Button_DumpCred);
        mTextViewDumpCred = (TextView)findViewById(R.id.TextView_DumpCred);
        mTextViewCmd = (TextView)findViewById(R.id.TextView_DelCmd);
        mButtonClrAllCred = (Button)findViewById(R.id.Button_ClrCred);
        mButtonHsStatus = (Button)findViewById(R.id.Button_GetHsStatus);
        mButtonHsNetwork = (Button)findViewById(R.id.Button_GetHsNetwork);

        mButtonDelCredIndex.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                handleDelCredIndex();
            }
        });

        mButtonClrAllCred.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                System.out.println("mButtonClrAllCred");
                handleClrAllCred();
            }
        });

        mButtonDumpCred.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                handleDumpCred();
            }
        });

        mButtonHsStatus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                handleGetHsStatus();
            }
        });

        mButtonHsNetwork.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                handleGetHsNetwork();
            }
        });

        mButtonDelNetworkIndex.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                handleDelHsNetwork();
            }
        });
    }

    //=================== main functions ========================\\
    private void handleDelHsNetwork() {
        boolean ret = false;
        if( mWifiMgr != null ) {
            String index = mEditTextDelNetwork.getText().toString();
            if (index.isEmpty()) {
                mTextViewDumpCred.setText("handleDelHsNetwork failed due to empty index");
                return;
            }
            ret = mWifiMgr.delHsNetwork(Integer.valueOf(index));
        }
        mTextViewDumpCred.setText("handleDelHsNetwork " + (ret? "success" : "fail"));
    }
    
    private void handleDelCredIndex() {
        int index   = 0;

        String sDelCred = mEditTextDelCred.getText().toString();
        String sResult;
        if( sDelCred.length()== 0) {
            mTextViewDumpCred.setText("DeleteCredAt failed due to empty index");
            return;
        } else {
            index = Integer.valueOf(sDelCred);
        }

        MtkLog.log( "handleDelCred "+index);
        if (mWifiMgr.delHsCredential(index)) {
            sResult = "success";
        } else {
            sResult = "fail";
        }
        mTextViewDumpCred.setText("DeleteCredAt(" + index + "): " + sResult);
    }

    private void handleGetHsStatus() {
        String status = mWifiMgr.getHsStatus();
        if (status.length() > 0) {
            mTextViewDumpCred.setText(status);
        } else {
            mTextViewDumpCred.setText("empty");
        }
    }

    private void handleGetHsNetwork() {
        String network = mWifiMgr.getHsNetwork();
        if (network.length() > 0) {
            mTextViewDumpCred.setText(network);
        } else {
            mTextViewDumpCred.setText("empty");
        }
    }

    private void handleClrAllCred() {
        String ret = DumpCredential();
        boolean isDone = false;
        boolean noCred = true;
        if (ret != null) {
            String[] creds = ret.split("\n");
            for (int i=0 ; i < creds.length ; i++) {
                MtkLog.log( "handleClrAllCred creds[" + i + "]=" +  creds[i]);
                if (i > 0) {
                    noCred = false;
                    String[] tokens = creds[i].split("\t");
                    isDone = mWifiMgr.delHsCredential(Integer.parseInt(tokens[0]));
                }
            }
        }
        if (noCred) {
            mTextViewDumpCred.setText("handleClrAllCred no creds to be del");
        } else {
            mTextViewDumpCred.setText("handleClrAllCred " + (isDone? "success" : "fail"));
        }
    }

    private void handleDumpCred() {
        String sResult;
        sResult = DumpCredential();
        MtkLog.log( "handleDumpCred:" + sResult);
        mTextViewDumpCred.setText(sResult);

    }

    private String DumpCredential() {
        if( mWifiMgr != null ) {
            MtkLog.log( "DumpCredential");
            return mWifiMgr.getHsCredential();

        } else {
            MtkLog.log( "DumpCredential fail, mWifiMgr err");
            return "mWifiMgr err";
        }
    }
}
