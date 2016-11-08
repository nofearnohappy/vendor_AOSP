package com.mediatek.wifi.hotspot.em;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.net.wifi.WifiManager;
import android.content.Context;
//UNMARK: import android.net.wifi.passpoint.PasspointProvisionManager;
import android.os.ServiceManager;
//UNMARK: import android.net.wifi.passpoint.IPasspointProvisionManager;
import android.os.RemoteException;

public class HSPT_AddCred extends Activity {

    private RadioButton     mRadioButtonUName;
    private RadioButton     mRadioButtonSim;
    private RadioButton     mRadioButtonCert;
    private EditText        mEditTextUserName;
    private EditText        mEditTextPassWord;
    private EditText        mEditTextIMSI;
    private EditText        mEditTextMNC;
    private EditText        mEditTextMCC;
    private EditText        mEditTextRootCA;
    private EditText        mEditTextRealm;
    private EditText        mEditTextFQDN;
    private EditText        mEditTextClientCA;
    private EditText        mEditTextSimslot;
    private EditText        mEditTextPriority;
    private EditText        mEditTextRC;
    private EditText        mEditTextMilenage;
    private Button          mButtonAddCred;
    private Button          mButtonClrScr;
    private Button          mButtonQaddSimCred;
    private Button          mButtonQaddTtlsCred;

    private WifiManager mWifiMgr = null;
    //UNMARK: private IPasspointProvisionManager mPPPmgr = null;

    private final String EAP_TYPE_SIM = "SIM";
    private final String EAP_TYPE_TTLS = "TTLS";
    private final String EAP_TYPE_TLS = "TLS";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hspt_addcred);

        mWifiMgr = (WifiManager)getSystemService(WIFI_SERVICE);
        if(mWifiMgr == null) {
            MtkLog.log("ERR: getSystemService MtkWifiManager failed");
            return;
        }
        initWidget();

    }

    //=================== initWidget ========================\\
    private void initWidget() {
        mRadioButtonUName = (RadioButton)findViewById(R.id.RadioButton_UName);
        mRadioButtonSim = (RadioButton)findViewById(R.id.RadioButton_Sim);
        mRadioButtonCert = (RadioButton)findViewById(R.id.RadioButton_Cert);
        mEditTextUserName = (EditText)findViewById(R.id.EditText_UserName);
        mEditTextPassWord = (EditText)findViewById(R.id.EditText_Password);
        mEditTextIMSI = (EditText)findViewById(R.id.EditText_IMSI);
        mEditTextMNC = (EditText)findViewById(R.id.EditText_MNC);
        mEditTextMCC = (EditText)findViewById(R.id.EditText_MCC);
        mEditTextIMSI.setEnabled(false);
        mEditTextMNC.setEnabled(false);
        mEditTextMCC.setEnabled(false);
        mEditTextRootCA = (EditText)findViewById(R.id.EditText_RootCA);
        mEditTextRealm = (EditText)findViewById(R.id.EditText_Realm);

        mEditTextFQDN = (EditText)findViewById(R.id.EditText_FQDN);
        mEditTextClientCA = (EditText)findViewById(R.id.EditText_ClientCA);
        mButtonAddCred = (Button)findViewById(R.id.Button_AddCred);
        mButtonClrScr = (Button)findViewById(R.id.Button_ClrScr);
        mButtonQaddSimCred = (Button)findViewById(R.id.Button_QaddSimCred);
        mButtonQaddTtlsCred = (Button)findViewById(R.id.Button_QaddTtlsCred);
        mEditTextSimslot = (EditText)findViewById(R.id.EditText_SimSlot);
        mEditTextPriority = (EditText)findViewById(R.id.EditText_Priority);
        mEditTextRC = (EditText)findViewById(R.id.EditText_RC);
        mEditTextMilenage = (EditText)findViewById(R.id.EditText_Milenage);

        mButtonAddCred.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                handleAddCred();
            }
        });

        mButtonClrScr.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                handleClrScr();
            }
        });

        mButtonQaddSimCred.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                handleQaddSimCred();
            }
        });

        mButtonQaddTtlsCred.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                handleQaddTtlsCred();
            }
        });

        mRadioButtonUName.setChecked(true);
    }

    //=================== main functions ========================\\
    private void handleClrScr() {
        mEditTextUserName.setText("");
        mEditTextPassWord.setText("");
        mEditTextIMSI.setText("");
        mEditTextMNC.setText("");
        mEditTextMCC.setText("");
        mEditTextRootCA.setText("");
        mEditTextRealm.setText("");
        mEditTextFQDN.setText("");
        mEditTextClientCA.setText("");
        mEditTextSimslot.setText("");
        mEditTextPriority.setText("");
        mEditTextRC.setText("");
        mEditTextMilenage.setText("");
    }

    private void handleQaddSimCred() {
        mRadioButtonSim.setChecked(true);
        mEditTextUserName.setText("");
        mEditTextPassWord.setText("90dca4eda45b53cf0f12d7c9c3bc6a89:cb9cccc4b9258e6dca4760379fb82581");
        mEditTextIMSI.setText("232010000000000");
        mEditTextMNC.setText("026");
        mEditTextMCC.setText("310");
        mEditTextRootCA.setText("");
        mEditTextRealm.setText("mail.example.com");
        mEditTextFQDN.setText("");
        mEditTextClientCA.setText("");
    }

    private void handleQaddTtlsCred() {
        mRadioButtonUName.setChecked(true);
        mEditTextUserName.setText("puff");
        mEditTextPassWord.setText("111222333");
        mEditTextIMSI.setText("");
        mEditTextMNC.setText("");
        mEditTextMCC.setText("");
        mEditTextRootCA.setText("cas.pem");
        mEditTextRealm.setText("wi-fi.org");
        mEditTextFQDN.setText("");
        mEditTextClientCA.setText("");
    }

    private void handleAddCred() {
        String sType        = null;
        String sUserName        = mEditTextUserName.getText().toString();
        String sPassWord        = mEditTextPassWord.getText().toString();
        String sImsi        = mEditTextIMSI.getText().toString();
        String sMnc     = mEditTextMNC.getText().toString();
        String sMcc     = mEditTextMCC.getText().toString();
        String sRoocCA      = mEditTextRootCA.getText().toString();
        String sRealm       = mEditTextRealm.getText().toString();
        String sFqdn        = mEditTextFQDN.getText().toString();
        String sClientCA        = mEditTextClientCA.getText().toString();

        String simslot = mEditTextSimslot.getText().toString();
        String priority = mEditTextPriority.getText().toString();
        String roamingconsortium = mEditTextRC.getText().toString();
        String milenage = mEditTextMilenage.getText().toString();

        MtkLog.log( "handleAddCred");
        if(mRadioButtonUName.isChecked()) {
            sType = EAP_TYPE_TTLS;
        } else if(mRadioButtonSim.isChecked()) {
            sType = EAP_TYPE_SIM;
        } else {
            sType = EAP_TYPE_TLS;
        }

        AddCredential(sType, sUserName, sPassWord, sImsi, sRoocCA,
            sRealm, sFqdn, sClientCA, milenage, simslot, priority, roamingconsortium, sMcc+sMnc);

    }

    private void AddCredential(String type, String username, String passwd, String imsi, String root_ca,
        String realm, String fqdn, String client_ca, String milenage, String simslot, String priority,
        String roamingconsortium, String mcc_mnc) {
        if( mWifiMgr != null ) {
            MtkLog.log( "AddCredential: " + type + "," + username + "," + passwd + "," + imsi + "," +
                root_ca + "," + realm + "," + fqdn + "," + client_ca + "," + milenage + "," + simslot + "," +
                priority + "," + roamingconsortium + "," + mcc_mnc);

            if( username.length() == 0 ) {
                username = null;
            }
            if( passwd.length() == 0 ) {
                passwd = null;
            }
            if( imsi.length() == 0 ) {
                imsi = null;
            }
            if( root_ca.length() == 0 ) {
                root_ca = null;
            }
            if( realm.length() == 0 ) {
                realm = null;
            }
            if( fqdn.length() == 0 ) {
                fqdn = null;
            }
            if( client_ca.length() == 0 ) {
                client_ca = null;
            }
            if( milenage.length() == 0 ) {
                milenage = null;
            }
            if( simslot.length() == 0 ) {
                simslot = null;
            }
            if( priority.length() == 0 ) {
                priority = null;
            }
            if( roamingconsortium.length() == 0 ) {
                roamingconsortium = null;
            }
            if( mcc_mnc.length() == 0 ) {
                mcc_mnc = null;
            }

            mWifiMgr.addHsCredential(type, username, passwd, imsi,
                                     root_ca, realm, fqdn, client_ca,
                                     milenage, simslot, priority,
                                     roamingconsortium, mcc_mnc);
        }

    }
}