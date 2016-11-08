package com.mediatek.wifitest;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.RttManager;
import android.net.wifi.RttManager.RttResult;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiLinkLayerStats;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiScanner;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;



import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;






/**
 * wifi Test
 */
public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "wifiTest";

    public static final String LLS_RESULTS_AVAILABLE_ACTION =
            "android.net.wifi.LLS_RESULTS";
    public static final String EXTRA_LLS_INFO = "extra_lls_info";

    private EditText[] mChannelSetEdit;
    private EditText[] mChannelSetEdit2;
    int support_gscan_set = 0;
    private EditText mSupportSetEdit;

    private EditText mBandEdit;

    private TextView mScanList;
    private TextView mOnChangingList;
    private TextView mOnQuiescenceList;

    private EditText mWifiChangeEdit;
    private EditText mWifiChangeEdit2;

    private EditText mhotlistEdit;
    private TextView mHotlistResultList;
    private TextView mLlsResultList;

    private TextView mWifiCapability;
    private TextView mWifiValidChannel;
    private TextView mWifiRttResult;

    private TextView mWifiHs20;

    WifiManager mWifiManager;
    WifiScanner mWifiScanner;
    RttManager mRttManager;

//extend to 8 set
    WifiScanner.ScanListener[] scanlistener;
    WifiScanner.WifiChangeListener wifichangelistener;
    WifiScanner.BssidListener bssidListener ;

    RttManager.RttListener rttListener;
    RttManager.RttParams[] mRttParalist;

   private IntentFilter mllsFilter;

    Toast toast;




    public static class ScanResultViewInfo {
        ScanResult[] list;
        TextView tv;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.start_gscan_btn).setOnClickListener(this);
        findViewById(R.id.stop_gscan_btn).setOnClickListener(this);
        findViewById(R.id.get_gscan_result_btn).setOnClickListener(this);

        findViewById(R.id.start_track_btn).setOnClickListener(this);
        findViewById(R.id.stop_track_btn).setOnClickListener(this);

        findViewById(R.id.set_wifichange_btn).setOnClickListener(this);

        findViewById(R.id.start_hotlist_btn).setOnClickListener(this);
        findViewById(R.id.stop_hotlist_btn).setOnClickListener(this);
        findViewById(R.id.start_ranging_btn).setOnClickListener(this);
        findViewById(R.id.stop_ranging_btn).setOnClickListener(this);

        findViewById(R.id.wificap_btn).setOnClickListener(this);
        findViewById(R.id.wifihs20_btn).setOnClickListener(this);

         findViewById(R.id.wifiavalchannel_btn).setOnClickListener(this);

        mSupportSetEdit = (EditText) findViewById(R.id.supportset_value);
        mBandEdit = (EditText) findViewById(R.id.band_value);

        mChannelSetEdit = new EditText[8];
        mChannelSetEdit2 = new EditText[8];

        for (int i = 0; i < 8 ; i++) {
            mChannelSetEdit[i] = (EditText) new EditText(this);
            mChannelSetEdit2[i] = (EditText) new EditText(this);
        }

        mChannelSetEdit[0] = (EditText) findViewById(R.id.set1_value1);
        mChannelSetEdit2[0] = (EditText) findViewById(R.id.set1_value2);
        mChannelSetEdit[1] = (EditText) findViewById(R.id.set2_value1);
        mChannelSetEdit2[1] = (EditText) findViewById(R.id.set2_value2);
        mChannelSetEdit[2] = (EditText) findViewById(R.id.set3_value1);
        mChannelSetEdit2[2] = (EditText) findViewById(R.id.set3_value2);
        mChannelSetEdit[3] = (EditText) findViewById(R.id.set4_value1);
        mChannelSetEdit2[3] = (EditText) findViewById(R.id.set4_value2);
        mChannelSetEdit[4] = (EditText) findViewById(R.id.set5_value1);
        mChannelSetEdit2[4] = (EditText) findViewById(R.id.set5_value2);
        mChannelSetEdit[5] = (EditText) findViewById(R.id.set6_value1);
        mChannelSetEdit2[5] = (EditText) findViewById(R.id.set6_value2);
        mChannelSetEdit[6] = (EditText) findViewById(R.id.set7_value1);
        mChannelSetEdit2[6] = (EditText) findViewById(R.id.set7_value2);
        mChannelSetEdit[7] = (EditText) findViewById(R.id.set8_value1);
        mChannelSetEdit2[7] = (EditText) findViewById(R.id.set8_value2);

        scanlistener = new WifiScanner.ScanListener[8];
        for (int i = 0; i < 8; i++) {
            scanlistener[i] = null;
        }

        mScanList = (TextView) findViewById(R.id.scan_list);
        mOnChangingList = (TextView) findViewById(R.id.on_change_list);
        mOnQuiescenceList = (TextView) findViewById(R.id.on_mquiescence_list);

        mLlsResultList = (TextView) findViewById(R.id.lls_list);
        mWifiChangeEdit = (EditText) findViewById(R.id.wifi_change_value);
        mWifiChangeEdit2 = (EditText) findViewById(R.id.wifi_change_value2);

        mhotlistEdit = (EditText) findViewById(R.id.hotlist_value);
        mHotlistResultList = (TextView) findViewById(R.id.hotlist_list);

        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

        mWifiScanner = (WifiScanner) getSystemService(WIFI_SCANNING_SERVICE);
        mRttManager = (RttManager) getSystemService(WIFI_RTT_SERVICE);

        findViewById(R.id.start_gscan_btn).setVisibility(View.VISIBLE);
        findViewById(R.id.stop_gscan_btn).setVisibility(View.GONE);

        findViewById(R.id.start_track_btn).setVisibility(View.VISIBLE);
        findViewById(R.id.stop_track_btn).setVisibility(View.GONE);

        findViewById(R.id.start_hotlist_btn).setVisibility(View.VISIBLE);
        findViewById(R.id.stop_hotlist_btn).setVisibility(View.GONE);
        findViewById(R.id.start_ranging_btn).setVisibility(View.VISIBLE);
        findViewById(R.id.stop_ranging_btn).setVisibility(View.GONE);

        mllsFilter = new IntentFilter(LLS_RESULTS_AVAILABLE_ACTION);

        mWifiCapability = (TextView) findViewById(R.id.wifi_capability);
        mWifiHs20 = (TextView) findViewById(R.id.wifi_hs20);

        mWifiRttResult = (TextView) findViewById(R.id.wifi_rtt_result);


        mWifiValidChannel = (TextView) findViewById(R.id.wifi_avaliable_channel);

        findViewById(R.id.wificap_btn).setVisibility(View.VISIBLE);
        findViewById(R.id.wifihs20_btn).setVisibility(View.VISIBLE);



        findViewById(R.id.wifiavalchannel_btn).setVisibility(View.VISIBLE);

        initDefaultValue();
    }


    private void initDefaultValue() {
        mSupportSetEdit.setText("1");

        mChannelSetEdit[0].setText("0,1000,0,3");
        mChannelSetEdit2[0].setText("(2412,1,1)");

        mWifiChangeEdit.setText("5,3,3,3,2000");

        mBandEdit.setText("1");

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        //M: batched scan
        registerReceiver(mLlsReceiver, mllsFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        //M: batched scan
        unregisterReceiver(mLlsReceiver);
    }

    private Boolean stopGScan(int setNum) {

        if (scanlistener[setNum] != null) {
            mWifiScanner.stopBackgroundScan(scanlistener[setNum]);
            scanlistener[setNum] = null;
            mScanList.setText("");
        }
        return true;
    }
    private WifiScanner.ScanSettings getGscanSetting(int setNum) {

        String s1 = null;
        String s2 = null;
        try{
            s1 = mChannelSetEdit[setNum].getText().toString();
            s2 = mChannelSetEdit2[setNum].getText().toString();
        }catch (Exception e) {

        }
        Log.d(TAG, "start_gscan_btn first line= " + s1);
        Log.d(TAG, "start_gscan_btn 2nd line= " + s2);
        //1. get scanSettings -- support 5 channel settings
        WifiScanner.ScanSettings ssettings = new WifiScanner.ScanSettings();
        List<String> band_period_rEvents_num =
            new ArrayList(Arrays.asList(s1.split(",")));
        if (band_period_rEvents_num.size() != 4) {
            Log.d(TAG, "innput wrong size()="
                + band_period_rEvents_num.size() +
                " band_period_rEvents_num=" + band_period_rEvents_num);
            for (int a = 0; a < band_period_rEvents_num.size(); a++) {
                Log.d(TAG, a + "= " + band_period_rEvents_num.get(a));
            }
            showToast("band_period_rEvents_num input wrong");
            return null;
        }
        ssettings.band = Integer.parseInt(band_period_rEvents_num.get(0));
        ssettings.periodInMs = Integer.parseInt(band_period_rEvents_num.get(1));
        ssettings.reportEvents = Integer.parseInt(band_period_rEvents_num.get(2));
        ssettings.numBssidsPerScan = Integer.parseInt(band_period_rEvents_num.get(3));

        Collection<String> input2 =
            new ArrayList(Arrays.asList(mChannelSetEdit2[setNum].getText().toString().split(";")));
        int i = 0;
        boolean parsingDone = true;
        if (input2.size() > 0) {
            ssettings.channels = new WifiScanner.ChannelSpec[input2.size()];
            for (String channel : input2) {
                Log.d(TAG, "channel " + i + ":" + channel);
                String modifiedString = channel.replaceAll("\\(", "");
                modifiedString = modifiedString.replaceAll("\\)", "");
                modifiedString = modifiedString.replaceAll(";", "");
                modifiedString = modifiedString.replaceAll(" ", "");
                String[] tokens = modifiedString.split(",");
                Log.d(TAG, "channels.length:" + tokens.length);
                if (tokens.length != 3) {
                    showToast("channels input wrong");
                    return null;
                }
                WifiScanner.ChannelSpec ch =
                    new WifiScanner.ChannelSpec(Integer.parseInt(tokens[0], 10));
                ch.passive = (Integer.parseInt(tokens[1], 10) == 1) ? true : false;
                ch.dwellTimeMS = Integer.parseInt(tokens[2], 10);
                ssettings.channels[i] = ch;
                Log.d(TAG, "ssettings.channelSet = " + ssettings.channels);
                i++;
            }
        } else {
            ssettings.channels = null;
        }
        if (!isValidScanSettings(ssettings)) {
            return null;
        }
        return ssettings;

    }

    @Override
    public void onClick(View v) {
        // 1. Start G-Scan
        if (v == findViewById(R.id.start_gscan_btn)) {

            //1. get support set            
            try{
                support_gscan_set =  (int) Integer.parseInt(mSupportSetEdit.getText().toString(), 10);
            } catch (Exception e) {
                support_gscan_set = 0;
            }
            Log.d(TAG, "support_gscan_set " + support_gscan_set);
            if (support_gscan_set < 1 || support_gscan_set > 8) {
                showToast("support set is wrong " + support_gscan_set);
                return;
            }
  
            for (int i = 0; i < support_gscan_set ; i++) {
                //1. get scan settings
                WifiScanner.ScanSettings ssettings = getGscanSetting(i);
                if (ssettings == null) {
                    Log.d(TAG, "getGscanSetting fail on " + i);
                    for (int k = 0; k < i - 1; k++) {
                        stopGScan(k);
                    }
                    showToast(" set " + (i + 1) + " settings is wrong ");
                    return;
                }
                //2. get scanlistener
                if (scanlistener[i] != null) {
                    //stop previous first.
                    mWifiScanner.stopBackgroundScan(scanlistener[i]);
                    scanlistener[i] = null;
                    mScanList.setText("");
                }


                scanlistener[i] = new WifiScanner.ScanListener() {
                    public void onPeriodChanged(int periodInMs) {
                        Log.d(TAG, "ScanListener onPeriodChanged");
                    }
                    public void onResults(WifiScanner.ScanData[] results) {
                        Log.d(TAG, "ScanListener onResults QQ");
                       // setScanListToView(results, mScanList);
//                        setScanList(results,mScanList);
                    }
                    public void onFullResult(ScanResult fullScanResult) {
                        Log.d(TAG, "ScanListener onFullResult");
                    }
                    public void onSuccess() {
                        Log.d(TAG, "ScanListener onSuccess");
                    }
                    public void onFailure(int reason, String description) {
                        Log.d(TAG, "ScanListener onFailure reason=" +
                            reason + " description=" + description);
                    }
                } ;
                if (ssettings == null || scanlistener == null || mWifiScanner == null) {
                    Log.d(TAG, "ssettings==null || scanlistener==null || mWifiScanner==null");
                    return;
                }
                Log.d(TAG, "(call mWifiScanner.startBackgroundScan");
                mWifiScanner.startBackgroundScan(ssettings, scanlistener[i]);
            }
            findViewById(R.id.start_gscan_btn).setVisibility(View.GONE);
            findViewById(R.id.stop_gscan_btn).setVisibility(View.VISIBLE);
        // 2. Stop G-Scan
        } else if (v == findViewById(R.id.stop_gscan_btn)) {
            Log.d(TAG, "stop_gscan_btn");
            for (int i = 0; i < support_gscan_set; i++) {
                Log.d(TAG, "stopGScan" + i);
                stopGScan(i);
            }
            findViewById(R.id.start_gscan_btn).setVisibility(View.VISIBLE);
            findViewById(R.id.stop_gscan_btn).setVisibility(View.GONE);
        // 3. Get G Scan Result
        } else if (v == findViewById(R.id.get_gscan_result_btn)) {
            Log.d(TAG, "get_gscan_result_btn");
            //wangfj
/*            ScanResult[] list = mWifiScanner.getScanResults();
            if (list != null) {
                Log.e(TAG, "setScanList A");
                setScanList(list, mScanList);
            } else {
                setScanListString("getScanResults null", mScanList);
            }*/
        ///4. Start tracking wifi change
        } else if (v == findViewById(R.id.start_track_btn)) {
            Log.d(TAG, "start_track_btn");
            if (wifichangelistener != null) {
                mWifiScanner.stopTrackingWifiChange(wifichangelistener);
                wifichangelistener = null;
                Log.e(TAG, "setScanList B");
                setScanList(null, mOnChangingList);
                Log.e(TAG, "setScanList C");
                setScanList(null, mOnQuiescenceList);
            }
            wifichangelistener = new WifiScanner.WifiChangeListener() {
                public void onChanging(ScanResult[] results) {
                    Log.d(TAG, "WifiChangeListener onChanging");
                    setScanListToView(results, mOnChangingList);
                    //setScanList(results,mOnChangingList);
                }
                public void onQuiescence(ScanResult[] results) {
                    Log.d(TAG, "WifiChangeListener onQuiescence");
                    setScanListToView(results, mOnQuiescenceList);
                }
                public void onSuccess() {
                    Log.d(TAG, "WifiChangeListener onSuccess");
                }
                public void onFailure(int reason, String description) {
                    Log.d(TAG, "WifiChangeListener onFailure reason=" +
                        reason + " description=" + description);
                }
            } ;
            mWifiScanner.startTrackingWifiChange(wifichangelistener);
            findViewById(R.id.start_track_btn).setVisibility(View.GONE);
            findViewById(R.id.stop_track_btn).setVisibility(View.VISIBLE);
         //5. Stop Tracking wifi change
        } else if (v == findViewById(R.id.stop_track_btn)) {
            Log.d(TAG, "stop_track_btn");
            if (wifichangelistener != null) {
                mWifiScanner.stopTrackingWifiChange(wifichangelistener);
                wifichangelistener = null;
                setScanList(null, mOnChangingList);
                setScanList(null, mOnQuiescenceList);
            }
            findViewById(R.id.start_track_btn).setVisibility(View.VISIBLE);
            findViewById(R.id.stop_track_btn).setVisibility(View.GONE);
        ///6. config wifi change
        } else if (v == findViewById(R.id.set_wifichange_btn)) {
            Log.d(TAG, "set_wifichange_btn");
            List<String> para1 =
                new ArrayList(Arrays.asList(mWifiChangeEdit.getText().toString().split(",")));
            List<String> para2 =
                new ArrayList(Arrays.asList(mWifiChangeEdit2.getText().toString().split(";")));
            if (para1.size() != 5 || para2 == null) {
                Log.d(TAG, "input wrong: intput1.size=" + para1.size() + " input2=" + para2);
                showToast("input wrong");
                return;
            }
            WifiScanner.BssidInfo[] bssidInfos = new WifiScanner.BssidInfo[para2.size()];
            for (int i = 0; i < para2.size(); i++) {
                List<String> bssidinfolist =
                    new ArrayList(Arrays.asList(para2.get(i).toString().split(",")));
                WifiScanner.BssidInfo  bssidonfo = new WifiScanner.BssidInfo();
                bssidonfo.bssid = bssidinfolist.get(0);
                bssidonfo.low = Integer.parseInt(bssidinfolist.get(1), 10);
                bssidonfo.high = Integer.parseInt(bssidinfolist.get(2), 10);
                bssidonfo.frequencyHint = Integer.parseInt(bssidinfolist.get(3), 10);
                bssidInfos[i] = bssidonfo;
            }
            mWifiScanner.configureWifiChange(
                Integer.parseInt(para1.get(0), 10),
                Integer.parseInt(para1.get(1), 10),
                Integer.parseInt(para1.get(2), 10),
                Integer.parseInt(para1.get(3), 10),
                Integer.parseInt(para1.get(4), 10),
                bssidInfos
            );
        //7. start tracking hotlist
        } else if (v == findViewById(R.id.start_hotlist_btn)) {
            Log.d(TAG, "start_hotlist_btn");
            if (bssidListener != null) {
                mWifiScanner.stopTrackingBssids(bssidListener);
                bssidListener = null;
                setScanList(null, mHotlistResultList);
            }
            List<String> hotlistString =
                new ArrayList(Arrays.asList(mhotlistEdit.getText().toString().split(";")));
            if (hotlistString == null) {
                Log.d(TAG, "hotlistString null");
                return;
            }
            WifiScanner.BssidInfo[] hotlistInfo = new WifiScanner.BssidInfo[hotlistString.size()];
            try{
                for (int i = 0; i < hotlistString.size(); i++) {
                    List<String> bssidinfolist =
                        new ArrayList(Arrays.asList(hotlistString.get(i).toString().split(",")));
                    WifiScanner.BssidInfo  hotitem = new WifiScanner.BssidInfo();
                    hotitem.bssid = bssidinfolist.get(0);
                    hotitem.low = Integer.parseInt(bssidinfolist.get(1), 10);
                    hotitem.high = Integer.parseInt(bssidinfolist.get(2), 10);
                    hotitem.frequencyHint = Integer.parseInt(bssidinfolist.get(3), 10);
                    hotlistInfo[i] = hotitem;
                }
            }catch (Exception e){
                Log.d(TAG, "error"+ e);
                showToast(" something wrong ");
                return;
            }


            bssidListener = new WifiScanner.BssidListener() {
                public void onFound(ScanResult[] results) {
                    Log.d(TAG, "BssidListener onFound");
                    setScanListToView(results, mHotlistResultList);
                }
                public void onLost(ScanResult[] results){

                }
                public void onSuccess() {
                    Log.d(TAG, "BssidListener onSuccess");
                }
                public void onFailure(int reason, String description) {
                    Log.d(TAG, "BssidListener onFailure reason=" +
                        reason + " description=" + description);

                }
            } ;
            mWifiScanner.startTrackingBssids(hotlistInfo, 0, bssidListener);

            findViewById(R.id.start_hotlist_btn).setVisibility(View.GONE);
            findViewById(R.id.stop_hotlist_btn).setVisibility(View.VISIBLE);
        // 8. Stop Tracking hotlist
        } else if (v == findViewById(R.id.stop_hotlist_btn)) {
            Log.d(TAG, "stop_hotlist_btn");
            if (bssidListener != null) {
                Log.d(TAG, " kill bssidListenerList[0]");
                mWifiScanner.stopTrackingBssids(bssidListener);
                bssidListener = null;
                setScanList(null, mHotlistResultList);
            }
            findViewById(R.id.start_hotlist_btn).setVisibility(View.VISIBLE);
            findViewById(R.id.stop_hotlist_btn).setVisibility(View.GONE);
        //9. start RTT
        } else if (v == findViewById(R.id.start_ranging_btn)) {
            Log.d(TAG, "start_ranging_btn");
            if (rttListener != null) {
                mRttManager.stopRanging(rttListener);
                rttListener = null;
            }
            boolean result = rttTest();
            if (mRttManager != null && result) {
                try {
                    mRttManager.startRanging(mRttParalist, rttListener);
                    findViewById(R.id.start_ranging_btn).setVisibility(View.GONE);
                    findViewById(R.id.stop_ranging_btn).setVisibility(View.VISIBLE);

                } catch (IllegalStateException e) {
                    Log.d(TAG, "IllegalStateException " + e);
                    findViewById(R.id.start_ranging_btn).setVisibility(View.VISIBLE);
                    findViewById(R.id.stop_ranging_btn).setVisibility(View.GONE);
                    showToast("IllegalStateException " + e);
                } catch (IllegalArgumentException e) {
                    Log.d(TAG, "IllegalArgumentException " + e);
                    findViewById(R.id.start_ranging_btn).setVisibility(View.VISIBLE);
                    findViewById(R.id.stop_ranging_btn).setVisibility(View.GONE);
                    showToast("IllegalArgumentException " + e);
                }
            } else {
                Log.d(TAG, "mRttManager==null or result fail");
                findViewById(R.id.start_ranging_btn).setVisibility(View.VISIBLE);
                findViewById(R.id.stop_ranging_btn).setVisibility(View.GONE);
            }
        //10. stop RTT
        } else if (v == findViewById(R.id.stop_ranging_btn)) {
            Log.d(TAG, "stop_ranging_btn");
            if (rttListener != null) {
                mRttManager.stopRanging(rttListener);
                rttListener = null;
            }
            findViewById(R.id.start_ranging_btn).setVisibility(View.VISIBLE);
            findViewById(R.id.stop_ranging_btn).setVisibility(View.GONE);
        //11. get capability
        } else if (v == findViewById(R.id.wificap_btn)) {
            Log.d(TAG, "wificap_btn");
            String s = getWifiCapability() + getRttCapability();
            mWifiCapability.setText(String.valueOf(s));



        //12. get valid band
        } else if (v == findViewById(R.id.wifiavalchannel_btn)) {
            Log.d(TAG, "wifiavalchannel_btn");
            int band = 0;
            try {
                band =  (int) Integer.parseInt(mBandEdit.getText().toString(), 10);
            } catch (Exception e) {
                Log.d(TAG, "can't get band set to 0");
                band = 0;
            }
            mWifiValidChannel.setText(String.valueOf(getValidChannel(band)));
        } else if (v == findViewById(R.id.wifihs20_btn)) {
            Log.d(TAG, "wificap_btn");

            Log.d(TAG, "testPasspoint");

            mWifiHs20.setText("processing");

            testPasspoint();
            mWifiHs20.setText("done");

            Log.d(TAG, "testPasspoint done");
        }
    }

    private final BroadcastReceiver mLlsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(LLS_RESULTS_AVAILABLE_ACTION)) {
                Log.e(TAG, "Received BATCHED_SCAN_RESULTS_AVAILABLE_ACTION");
                StringBuffer llsList = new StringBuffer();
                WifiLinkLayerStats stats =
                    (WifiLinkLayerStats) intent.getParcelableExtra(EXTRA_LLS_INFO);
                llsList.append(stats);
                mLlsResultList.setText(llsList);
            } else {
                Log.e(TAG, "Received an unknown Wifi Intent");
            }
        }
    };

    private void testPasspoint() {
        String mycert = "";
        X509Certificate cert = null;

        Log.d(TAG, "setPasspointNetwork");

        cert = geneCacertificate();
        Log.d(TAG, "cert=" + cert);

        //add passpoint network
        WifiConfiguration config = new WifiConfiguration();

        config.providerFriendlyName  = SystemProperties.get("wifi.hs20.test.fname", "noset");
        config.FQDN = SystemProperties.get("wifi.hs20.test.fqdn", "noset");

        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X);


        long[] roamingConsortiumIds = new long[1];


        long lg;
        String strlg = SystemProperties.get("wifi.hs20.test.roamcos", "zzz");
        Log.d(TAG, "get roamingConsortiumIds str = " + strlg);
        if (strlg.equals("zzz")) {
            Log.d(TAG, "no roamingConsortiumIds");
        } else {



            lg = Long.parseLong(strlg, 16);
            Log.d(TAG, "get roamingConsortiumIds = " + lg);
            roamingConsortiumIds[0] = lg;
            config.roamingConsortiumIds = roamingConsortiumIds;
        }

//        roamingConsortiumIds.add(0x506F9AL);
//        roamingConsortiumIds.add(0x222222L);
  //      roamingConsortiumIds.add(0x333333333L);





        WifiEnterpriseConfig enterpriseConfig = new WifiEnterpriseConfig();

        String eap = SystemProperties.get("wifi.hs20.test.eap", "TLS");
        if (eap.equals("TLS")) {
            enterpriseConfig.setEapMethod(WifiEnterpriseConfig.Eap.TLS);

            String clientAlias = SystemProperties.get("wifi.hs20.test.clientAlias", "aaa");
            if (clientAlias.equals("aaa")) {
                showToast("eap method set fail");
                return;
            }
            enterpriseConfig.setClientCertificateAlias(clientAlias);
        } else if (eap.equals("TTLS")) {
            enterpriseConfig.setEapMethod(WifiEnterpriseConfig.Eap.TTLS);
        } else if (eap.equals("SIM")) {
           enterpriseConfig.setEapMethod(WifiEnterpriseConfig.Eap.SIM);
           String plmn = SystemProperties.get("wifi.hs20.test.plmn", "0");
           enterpriseConfig.setPlmn(plmn);

        } else {
            showToast("eap method set fail");
            return;
//            enterpriseConfig.setEapMethod(WifiEnterpriseConfig.Eap.TTLS);
        }

        enterpriseConfig.setPhase2Method(WifiEnterpriseConfig.Phase2.MSCHAPV2);
        String id = SystemProperties.get("wifi.hs20.test.id", "noset");
        String pwd = SystemProperties.get("wifi.hs20.test.pwd", "noset");
        String realm = SystemProperties.get("wifi.hs20.test.realm", "noset");

        if (!id.equals("noset") && !pwd.equals("noset")) {
            enterpriseConfig.setIdentity(id);
            enterpriseConfig.setPassword(pwd);
        }

        enterpriseConfig.setCaCertificate(cert);
        enterpriseConfig.setRealm(realm);
        config.enterpriseConfig = enterpriseConfig;

        mWifiManager.addNetwork(config);
        mWifiManager.saveConfiguration();

        Log.d(TAG, "addNetwork passpoint done");
    }

    private X509Certificate geneCacertificate() {
        X509Certificate cert = null;
        //1. get ca certificate file path
        String file = Environment.getDataDirectory() +
                "/misc/wifi/ca.pem";

        if (Environment.getExternalStorageState() == null) {
            file = Environment.getDataDirectory() + "/Download/";
        } else if (Environment.getExternalStorageState() != null) {
            file = Environment.getExternalStorageDirectory() + "/Download/";
        }
        Log.d(TAG, "file path  =" + file);
        //2. get file name
        String filename = SystemProperties.get("wifi.hs20.test.ca", "ca.pem");

        Log.d(TAG, "filename  =" + filename);

        file = file + filename;
        Log.d(TAG, "file path  =" + file);
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(file));
            CertificateFactory cFactory = CertificateFactory.getInstance("X.509");
            cert = (X509Certificate) cFactory.generateCertificate(in);
        } catch (CertificateException e) {
            cert = null;
            Log.d(TAG, "CertificateException" + e);
         } catch (FileNotFoundException e) {
                 Log.d(TAG, "FileNotFoundException" + e);
         } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.d(TAG, "IOException" + e);
                }
            }
        }
        return cert;
    }

    private boolean isValidScanSettings(WifiScanner.ScanSettings s) {
        if (s.band <= WifiScanner.WIFI_BAND_UNSPECIFIED ||
            s.band > WifiScanner.WIFI_BAND_BOTH_WITH_DFS) {
            Log.e(TAG, "isValidScanSettings: band invalid set to WIFI_BAND_UNSPECIFIED");
            s.band = WifiScanner.WIFI_BAND_UNSPECIFIED;
            showToast("band invalid. set to WIFI_BAND_UNSPECIFIED");
        }
        if (s.channels == null) {
            if (s.band == WifiScanner.WIFI_BAND_UNSPECIFIED) {
                showToast("Failure. s.channels = null and no band");
                return false;
            }
        }
        if (s.periodInMs < WifiScanner.MIN_SCAN_PERIOD_MS  ||
            s.periodInMs > WifiScanner.MAX_SCAN_PERIOD_MS) {
            showToast("Failure. periodInMs = invalid");
            return false;
        }
        if (s.reportEvents < WifiScanner.REPORT_EVENT_AFTER_BUFFER_FULL
            || s.reportEvents > WifiScanner.REPORT_EVENT_FULL_SCAN_RESULT) {
            showToast("Failure. reportEvents = invalid");
            return false;
        }
        if (s.numBssidsPerScan <= 0) {
            showToast("Failure. numBssidsPerScan = invalid");
            return false;
        }
        return true;
    }
    private void showToast(String s) {
        toast = Toast.makeText(this, s, Toast.LENGTH_LONG);
        toast.show();
    }

    private void setScanListString(String s, TextView tv) {
        tv.setText(s);
        return;
    }
    private void setScanList(ScanResult[] list, TextView tv) {

        StringBuffer scanList = new StringBuffer();
        if (list != null) {
            for (int i = list.length - 1; i >= 0; i--) {
                final ScanResult scanResult = list[i];

                if (scanResult == null) {
                    continue;
                }

                if (TextUtils.isEmpty(scanResult.SSID)) {
                    continue;
                }
                scanList.append(list[i]);
            }
            tv.setText(scanList);
        } else {
            tv.setText("");
            return;
        }
    }

    private void setScanListToView(ScanResult[] list, TextView tv) {

        Message msg = handler.obtainMessage();
        ScanResultViewInfo sv = new ScanResultViewInfo();
        sv.list = list;
        sv.tv = tv;
        msg.obj = sv;
        handler.sendMessage(msg);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {

            ScanResultViewInfo sv = (ScanResultViewInfo) msg.obj;
            setScanList(sv.list, sv.tv);
        }
    };

    private String getValidChannel(int band) {
        List<Integer> channels = mWifiScanner.getAvailableChannels(band);
        if (channels == null) {
            Log.e(TAG, "getAvailableChannels fail");
            return "getAvailableChannels fail";
        }
         StringBuilder sb = new StringBuilder();
         sb.append(" channel number =" + channels.size() + "\n");
        for (int i = channels.size() - 1; i >= 0; i--) {
            final Integer ch = channels.get(i);
             sb.append(ch + ", ");
        }
        return sb.toString();
    }

    private String getRttCapability() {
        RttManager.RttCapabilities rttCap = mRttManager.getRttCapabilities();

        if (rttCap != null) {
            return rttCap.toString();
        } else {
            return "fail";
        }
    }

    private String getWifiCapability() {
        StringBuilder sb = new StringBuilder();
        sb.append("Support for 5 GHz Band: " + mWifiManager.is5GHzBandSupported() + "\n");
        sb.append("Wifi-Direct Support: " + mWifiManager.isP2pSupported() + "\n");
        sb.append("GAS/ANQP Support: "
                + mWifiManager.isPasspointSupported() + "\n");
        sb.append("Soft AP Support: "
                + mWifiManager.isPortableHotspotSupported() + "\n");
        sb.append("WifiScanner APIs Support: "
                + mWifiManager.isWifiScannerSupported() + "\n");
        sb.append("Neighbor Awareness Networking Support: "
                + mWifiManager.isNanSupported() + "\n");
        sb.append("Device-to-device RTT Support: "
                + mWifiManager.isDeviceToDeviceRttSupported() + "\n");
        sb.append("Device-to-AP RTT Support: "
                + mWifiManager.isDeviceToApRttSupported() + "\n");
        sb.append("Preferred network offload Support: "
                + mWifiManager.isPreferredNetworkOffloadSupported() + "\n");
        sb.append("Tunnel directed link setup Support: "
                + mWifiManager.isTdlsSupported() + "\n");
        sb.append("Enhanced power reporting: "
                    + mWifiManager.isEnhancedPowerReportingSupported() + "\n");
        return sb.toString();
    }

    private boolean rttTest() {

        String file = Environment.getDataDirectory() + "/Download/rtt.txt";
        final String num_STR = "num=";
        final String deviceType_STR = "deviceType=";
        final String requestType_STR = "requestType=";
        final String bssid_STR = "bssid=";
        final String frequency_STR = "frequency=";
        final String channelWidth_STR = "channelWidth=";
        final String centerFreq0_STR = "centerFreq0=";
        final String centerFreq1_STR = "centerFreq1=";
        final String numberBurst_STR = "numberBurst=";
        final String interval_STR = "interval=";
        final String numSamplesPerBurst_STR = "numSamplesPerBurst=";
        final String numRetriesPerMeasurementFrame_STR = "numRetriesPerMeasurementFrame=";
        final String numRetriesPerFTMR_STR = "numRetriesPerFTMR=";
        final String LCIRequest_STR = "LCIRequest=";
        final String LCRRequest_STR = "LCRRequest=";
        final String burstTimeout_STR = "burstTimeout=";
        final String preamble_STR = "preamble=";
        final String bandwidth_STR = "bandwidth=";
        final String DELIMITER_STR = "====";
        final String END_STR = "####";


        if (Environment.getExternalStorageState() == null) {
            file = Environment.getDataDirectory() + "/Download/rtt.txt";
        } else if (Environment.getExternalStorageState() != null) {
            file = Environment.getExternalStorageDirectory() + "/Download/rtt.txt";
        }

        Log.d(TAG, "rttStartTest file path = " + file);

        FileReader f =null;
        int num = 0;

        int deviceType = RttManager.RTT_PEER_TYPE_AP; //default value
        int requestType = RttManager.RTT_TYPE_ONE_SIDED;  //default value
        String bssid = null;
        int frequency = 0;
        int channelWidth = 0;
        int centerFreq0 = 0;
        int centerFreq1 = 0;
        int numberBurst = 0;  //default value
        int interval = 0;  //default value
        int numSamplesPerBurst = 8;  //default value
        int numRetriesPerMeasurementFrame = 0;  //default value
        int numRetriesPerFTMR = 0;  //default value
        boolean LCIRequest = false;
        boolean LCRRequest = false;
        int burstTimeout = 15;  //default value
        int preamble = RttManager.PREAMBLE_HT;  //default value
        int bandwidth = RttManager.RTT_BW_20_SUPPORT;  //default value
        try {
            f = new FileReader(file);
            BufferedReader reader = new BufferedReader(f);
            String         line = null;

            //1. get num
            line = reader.readLine();
            Log.e(TAG, "line = " + line);
            if ( line != null && line.startsWith(num_STR)) {
                try {
                    num = Integer.parseInt(line.substring(num_STR.length()));
                } catch (NumberFormatException e) {
                    num = 0;
                }
            }
            if(num == 0) {
                showToast("Failure. num = invalid");
                return false;
            }
            mRttParalist = new RttManager.RttParams[num];
            Log.e(TAG, "Total num = " + num);
            int count = 0;
            while ((line = reader.readLine() ) != null ) {
                Log.e(TAG, "line = " + line);
                if (line.startsWith(deviceType_STR)) {
                    try {
                        deviceType = Integer.parseInt(line.substring(deviceType_STR.length()));
                    } catch (NumberFormatException e) {
                        deviceType = 0;
                    }
                } else if (line.startsWith(requestType_STR)) {
                    try {
                        requestType = Integer.parseInt(line.substring(requestType_STR.length()));
                    } catch (NumberFormatException e) {
                        requestType = 0;
                    }

                } else if (line.startsWith(bssid_STR)) {
                    bssid = line.substring(bssid_STR.length());
                } else if (line.startsWith(frequency_STR)) {
                    try {
                        frequency = Integer.parseInt(line.substring(frequency_STR.length()));
                    } catch (NumberFormatException e) {
                        frequency = 0;
                    }

                } else if (line.startsWith(channelWidth_STR)) {
                    try {
                        channelWidth = Integer.parseInt(line.substring(channelWidth_STR.length()));
                    } catch (NumberFormatException e) {
                        channelWidth = 0;
                    }

                } else if (line.startsWith(centerFreq0_STR)) {

                    try {
                        centerFreq0 = Integer.parseInt(line.substring(centerFreq0_STR.length()));
                    } catch (NumberFormatException e) {
                        centerFreq0 = 0;
                    }

                } else if (line.startsWith(centerFreq1_STR)) {
                    try {
                        centerFreq1 = Integer.parseInt(line.substring(centerFreq1_STR.length()));
                    } catch (NumberFormatException e) {
                        centerFreq1 = 0;
                    }

                } else if (line.startsWith(numberBurst_STR)) {
                    try {
                        numberBurst = Integer.parseInt(line.substring(numberBurst_STR.length()));
                    } catch (NumberFormatException e) {
                        numberBurst = 0;
                    }

                } else if (line.startsWith(interval_STR)) {
                    try {
                        interval = Integer.parseInt(line.substring(interval_STR.length()));
                    } catch (NumberFormatException e) {
                        interval = 0;
                    }

                } else if (line.startsWith(numSamplesPerBurst_STR)) {
                    try {
                        numSamplesPerBurst = Integer.parseInt(
                            line.substring(numSamplesPerBurst_STR.length()));
                    } catch (NumberFormatException e) {
                        numSamplesPerBurst = 0;
                    }

                } else if (line.startsWith(numRetriesPerMeasurementFrame_STR)) {
                    try {
                        numRetriesPerMeasurementFrame = Integer.parseInt(
                            line.substring(numRetriesPerMeasurementFrame_STR.length()));
                    } catch (NumberFormatException e) {
                        numRetriesPerMeasurementFrame = 0;
                    }

                } else if (line.startsWith(numRetriesPerFTMR_STR)) {
                    try {
                        numRetriesPerFTMR = Integer.parseInt(
                            line.substring(numRetriesPerFTMR_STR.length()));
                    } catch (NumberFormatException e) {
                        numRetriesPerFTMR = 0;
                    }
                } else if (line.startsWith(LCIRequest_STR)) {
                    try {
                        if (Integer.parseInt(line.substring(LCIRequest_STR.length()) ) == 0 ) {
                            LCIRequest = false;
                        } else {
                            LCIRequest = true;
                        }

                    } catch (NumberFormatException e) {
                        LCIRequest = false;
                    }

                } else if (line.startsWith(LCRRequest_STR)) {
                    try {
                        if (Integer.parseInt(line.substring(LCRRequest_STR.length()) ) == 0) {
                            LCRRequest = false;
                        } else {
                            LCRRequest = true;
                        }

                    } catch (NumberFormatException e) {
                        LCRRequest = false;
                    }
                } else if (line.startsWith(burstTimeout_STR)) {
                    try {
                        burstTimeout = Integer.parseInt(line.substring(burstTimeout_STR.length()));
                    } catch (NumberFormatException e) {
                        burstTimeout = 0;
                    }

                } else if (line.startsWith(preamble_STR)) {
                   try {
                       preamble = Integer.parseInt(line.substring(preamble_STR.length()));
                   } catch (NumberFormatException e) {
                       preamble = 0;
                   }

                } else if (line.startsWith(bandwidth_STR)) {
                    try {
                       bandwidth = Integer.parseInt(line.substring(bandwidth_STR.length()));
                   } catch (NumberFormatException e) {
                       bandwidth = 0;
                   }

                } else if (line.startsWith(DELIMITER_STR)) {
                    //1. new a RttParams
                    Log.e(TAG, "count = " + count);
                    RttManager.RttParams rttpara = new RttManager.RttParams();
                    rttpara.deviceType = deviceType;
                    rttpara.requestType = requestType;
                    rttpara.bssid = bssid;
                    rttpara.frequency = frequency;
                    rttpara.channelWidth = channelWidth;
                    rttpara.centerFreq0 = centerFreq0;
                    rttpara.centerFreq1 = centerFreq1;
                    rttpara.numberBurst = numberBurst;
                    rttpara.interval = interval;
                    rttpara.numSamplesPerBurst = numSamplesPerBurst;
                    rttpara.numRetriesPerMeasurementFrame = numRetriesPerMeasurementFrame;
                    rttpara.numRetriesPerFTMR = numRetriesPerFTMR;
                    rttpara.LCIRequest = LCIRequest;
                    rttpara.LCRRequest = LCRRequest;
                    rttpara.burstTimeout = burstTimeout;
                    rttpara.preamble = preamble;
                    rttpara.bandwidth = bandwidth;
                    mRttParalist[count] = rttpara;
                    count++;

                    //2. reset
                    deviceType = RttManager.RTT_PEER_TYPE_AP; //default value
                    requestType = RttManager.RTT_TYPE_ONE_SIDED;  //default value
                    bssid = null;
                    frequency = 0;
                    channelWidth = 0;
                    centerFreq0 = 0;
                    centerFreq1 = 0;
                    numberBurst = 0;  //default value
                    interval = 0;  //default value
                    numSamplesPerBurst = 8;  //default value
                    numRetriesPerMeasurementFrame = 0;  //default value
                    numRetriesPerFTMR = 0;  //default value
                    LCIRequest = false;
                    LCRRequest = false;
                    burstTimeout = 15;  //default value
                    preamble = RttManager.PREAMBLE_HT;  //default value
                    bandwidth = RttManager.RTT_BW_20_SUPPORT;  //default value
                }
            }
        }
        catch ( FileNotFoundException e) {
             Log.d(TAG, "File not found:" + e);
             showToast("File not found:" + e);
             return false;
        }
        catch ( IOException e) {
             Log.d(TAG, "Exception:" + e);
             showToast("Exception:" + e);
             return false;
        }
        finally {
            try {
                if (f != null) {
                    f.close();
                }
            }
            catch ( IOException e) {
                Log.d(TAG, "Exception:" + e);
                showToast("Exception:" + e);
                 return false;
            }
        }

        printRttParams(mRttParalist);

        rttListener = new RttManager.RttListener() {
            public void onSuccess(RttResult[] results) {
                 Log.d(TAG, "RttListener onSuccess");
                 showToast("RttListener onSuccess");
                 mWifiRttResult.setText("RttListener onSuccess");
                findViewById(R.id.start_ranging_btn).setVisibility(View.VISIBLE);
                findViewById(R.id.stop_ranging_btn).setVisibility(View.GONE);
                String result = showRttResult(results);
                mWifiRttResult.setText(result);
            }
            public void onFailure(int reason, String description) {
                Log.d(TAG, "RttListener onFailure reason=" +
                    reason + " description=" + description);
                showToast("RttListener onFailure");
                mWifiRttResult.setText("RttListener onFailure");

                findViewById(R.id.start_ranging_btn).setVisibility(View.VISIBLE);
                findViewById(R.id.stop_ranging_btn).setVisibility(View.GONE);

            }
            public void onAborted() {
                Log.d(TAG, "RttListener onAborted");
                showToast("RttListener onAborted");
                mWifiRttResult.setText("RttListener onAborted");
                findViewById(R.id.start_ranging_btn).setVisibility(View.VISIBLE);
                findViewById(R.id.stop_ranging_btn).setVisibility(View.GONE);
            }
        } ;
        return true;
    }
    private String  showRttResult(RttResult[] results) {

        StringBuilder sb = new StringBuilder();
        int index =0;
        for(RttManager.RttResult rttResult : results) {
                sb.append(index +" : " + rttResult.bssid + " " + rttResult.burstNumber +
                    " " + rttResult.measurementFrameNumber + " " +
                    rttResult.successMeasurementFrameNumber + " " +
                    rttResult.frameNumberPerBurstPeer + "\n");
                sb.append(rttResult.status + " " + rttResult.measurementType + " " +
                    rttResult.retryAfterDuration + " " + rttResult.ts + " " +
                    rttResult.rssi + "\n");
                sb.append(rttResult.rssiSpread + " " + rttResult.txRate + " " +
                    rttResult.rxRate + " " + rttResult.rtt + " " +
                    rttResult.rttStandardDeviation + "\n");
                sb.append(rttResult.rttSpread + " " + rttResult.distance + " " +
                    rttResult.distanceStandardDeviation + " " + rttResult.distanceSpread + " " +
                    rttResult.burstDuration + " " + rttResult.negotiatedBurstNum + "\n");
                sb.append("--------------\n");
                index ++;
        }
        return sb.toString();
    }
    private void printRttParams(RttManager.RttParams[] para) {
        int index = 0;
        Log.d(TAG, "print RttParams start ==== ");
       for ( RttManager.RttParams rttParam : para) {
           Log.d(TAG, "index = " + index);
           Log.d(TAG, "deviceType = " + rttParam.deviceType);
           Log.d(TAG, "requestType = " + rttParam.requestType);
           Log.d(TAG, "bssid = " + rttParam.bssid);
           Log.d(TAG, "frequency = " + rttParam.frequency);
           Log.d(TAG, "channelWidth = " + rttParam.channelWidth);
           Log.d(TAG, "centerFreq0 = "  + rttParam.centerFreq0);
           Log.d(TAG, "centerFreq1 = " + rttParam.centerFreq1);
           Log.d(TAG, "numberBurst = " + rttParam.numberBurst);
           Log.d(TAG, "interval = " + rttParam.interval);
           Log.d(TAG, "numSamplesPerBurst = " + rttParam.numSamplesPerBurst);
           Log.d(TAG, "numRetriesPerMeasurementFrame = " + rttParam.numRetriesPerMeasurementFrame);
           Log.d(TAG, "numRetriesPerFTMR = " + rttParam.numRetriesPerFTMR);
           Log.d(TAG, "LCIRequest = " + rttParam.LCIRequest);
           Log.d(TAG, "LCRRequest = " + rttParam.LCRRequest);
           Log.d(TAG, "burstTimeout = " + rttParam.burstTimeout);
           Log.d(TAG, "preamble = " + rttParam.preamble);
           Log.d(TAG, "bandwidth = " + rttParam.bandwidth);
           index++;
           Log.d(TAG, "print RttParams end ==== ");

       }

    }
}
