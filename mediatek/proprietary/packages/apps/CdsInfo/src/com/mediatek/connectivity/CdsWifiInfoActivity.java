/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.connectivity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.BatchedScanSettings;
import android.net.wifi.BatchedScanResult;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder ;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Show the current status details of Wifi related fields
 */
public class CdsWifiInfoActivity extends Activity {

    private static final String TAG = "CDSINFO/WifiInfo";

    private static final int MAC_ADDRESS_ID = 30;
    private static final int MAC_ADDRESS_DIGITS = 6;
    private static final int MAX_ADDRESS_VALUE = 0xff;
    private static final int INVALID_RSSI = -200;

    private static final String MAC_ADDRESS_FILENAME = "/data/nvram/APCFG/APRDEB/WIFI";

    private static final String[] WIFI_SYSTEM_PROPERTY = new String[] {
        "net.hostname",
        "dhcp.wlan0.ipaddress",
        "net.dns1",
        "net.dns2",
        "dhcp.wlan0.leasetime",
        "dhcp.wlan0.gateway",
        "dhcp.wlan0.mask",
        "dhcp.wlan0.dns1",
        "dhcp.wlan0.dns2",
        "dhcp.wlan0.dns3",
        "dhcp.wlan0.dns4",
        "init.svc.dhcpcd_wlan0",
        "wlan.driver.status",
        "wifi.interface",
        "dhcp.wlan0.pid",
        "dhcp.wlan0.server",
        "dhcp.wlan0.reason",
        "dhcp.wlan0.result",
        "mediatek.wlan.ctia"
    };

    private Button   mUpdateButton;
    private Button   mScanButton;
    private TextView mWifiState;
    private TextView mNetworkState;
    private TextView mSupplicantState;
    private TextView mRSSI;
    private TextView mBSSID;
    private TextView mSSID;
    private TextView mHiddenSSID;
    private TextView mIPAddr;
    private TextView mMACAddr;
    private TextView mNetworkId;
    private TextView mWifiCapability;
    private TextView mLinkSpeed;
    private TextView mScanList;
    private TextView mSystemProperties;

    private TextView mMacAddrLabel;
    private EditText mMacAddrEdit;
    private Button   mMacAddBtn;
    private short[]  mRandomMacAddr;

    private Toast mToast;

    private static String MacAddressRandom = "";
    private boolean mUserMode = false;

    private WifiManager mWifiManager;
    private IntentFilter mWifiStateFilter;

    //poor link
    private CheckBox mCbPoorLinkBtn = null;
    private boolean mPoorLinkEnabledInfo = false;
    private CheckBox mCbProfiling = null;
    private TextView mPoorLinkGoodLabel;
    private EditText mPoorLinkGoodEdit;
    private TextView mPoorLinkBadLabel;
    private EditText mPoorLinkBadEdit;
    private boolean mProfilingInfo = false;
    private Button   mPoorLinkAddBtn;
    private TextView mPoorLinkRssiLabel;
    private EditText mPoorLinkRssiEdit;
    private TextView mPoorLinkLinkSpeedLabel;
    private EditText mPoorLinkLinkSpeedEdit;

    ///M: Batched Scan
    private Button mBatchedScanStartBtn;
    private Button mBatchedScanStopBtn;
    private Button mBatchedScanPollBtn;
    private TextView mMaxScanPerBatchLabel;
    private EditText mMaxScanPerBatchEdit;
    private TextView mMaxApPerScanLabel;
    private EditText mMaxApPerScanEdit;
    private TextView mChannelSetLabel;
    private EditText mChannelSetEdit;
    private TextView mScanIntervalSecLabel;
    private EditText mScanIntervalSecEdit;
    private TextView mMaxApPerDistanceLabel;
    private EditText mMaxApPerDistanceEdit;
    private IntentFilter mBatchedScanFilter;
    private BatchedScanSettings mBatchedScantSettings;
    private TextView mBatchedScanList;
    private TextView mBatchedScanListLabel;


    ///M: hotspot optimization
    private CheckBox mHotspotOptBtn = null;
    ///M:stop Autojoin Scan When Connected
    private CheckBox mStopAutojoinScanWhenConnectedBtn = null;

    private Button   mLunchWifiTestButton;

    //============================
    // Activity lifecycle
    //============================

    private final static ArrayList<Integer> channelsFrequency = new ArrayList<Integer>(
        Arrays.asList(0, 2412, 2417, 2422, 2427, 2432, 2437, 2442, 2447,
                      2452, 2457, 2462, 2467, 2472, 2484));

    private  Integer getFrequencyFromChannel(int channel) {
        return channelsFrequency.get(channel);
    }

    private  int getChannelFromFrequency(int frequency) {
        return channelsFrequency.indexOf(Integer.valueOf(frequency));
    }

    private final BroadcastReceiver mWifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) {
                return;
            }

            if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                handleWifiStateChanged(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                                       WifiManager.WIFI_STATE_UNKNOWN));
            } else if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                handleNetworkStateChanged(
                    (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO));
            } else if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                handleScanResultsAvailable();
            } else if (intent.getAction().equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                /* TODO: handle supplicant connection change later */
            } else if (intent.getAction().equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
                handleSupplicantStateChanged(
                    (SupplicantState) intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE),
                    intent.hasExtra(WifiManager.EXTRA_SUPPLICANT_ERROR),
                    intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, 0));
            } else if (intent.getAction().equals(WifiManager.RSSI_CHANGED_ACTION)) {
                handleSignalChanged(intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, 0));
            } else if (intent.getAction().equals(WifiManager.NETWORK_IDS_CHANGED_ACTION)) {
                /* TODO: handle network id change info later */
            } else {
                Log.e(TAG, "Received an unknown Wifi Intent");
            }
        }
    };

    private final BroadcastReceiver mBatchedScanStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(WifiManager.BATCHED_SCAN_RESULTS_AVAILABLE_ACTION)) {
                Log.e(TAG, "Received BATCHED_SCAN_RESULTS_AVAILABLE_ACTION");

                if (mWifiManager.isBatchedScanSupported() == true && SystemProperties.get("wifi.dbg.bscan", "").equals("true") == true) {
                    handleBatchedScanResultsAvailable();
                }
            } else {
                Log.e(TAG, "Received an unknown Wifi Intent");
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

        mWifiStateFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mWifiStateFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mWifiStateFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mWifiStateFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mWifiStateFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        mWifiStateFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);


        setContentView(R.layout.wifi_status_test);

        mUpdateButton = (Button) findViewById(R.id.update);
        mUpdateButton.setOnClickListener(mUpdateButtonHandler);

        mScanButton = (Button) findViewById(R.id.scan);
        mScanButton.setOnClickListener(mScanButtonHandler);

        mWifiState = (TextView) findViewById(R.id.wifi_state);
        mNetworkState = (TextView) findViewById(R.id.network_state);
        mSupplicantState = (TextView) findViewById(R.id.supplicant_state);
        mRSSI = (TextView) findViewById(R.id.rssi);
        mBSSID = (TextView) findViewById(R.id.bssid);
        mSSID = (TextView) findViewById(R.id.ssid);
        mHiddenSSID = (TextView) findViewById(R.id.hidden_ssid);
        mIPAddr = (TextView) findViewById(R.id.ipaddr);
        mMACAddr = (TextView) findViewById(R.id.macaddr);
        mNetworkId = (TextView) findViewById(R.id.networkid);
        mWifiCapability = (TextView) findViewById(R.id.wifi_capability);
        mLinkSpeed = (TextView) findViewById(R.id.link_speed);
        mScanList = (TextView) findViewById(R.id.scan_list);

        mSystemProperties = (TextView) findViewById(R.id.system_property);

        mMacAddrLabel = (TextView) findViewById(R.id.mac_label);
        mMacAddrEdit = (EditText) findViewById(R.id.macid);
        mMacAddBtn = (Button) findViewById(R.id.mac_update_btn);
        mMacAddBtn.setOnClickListener(mMacEditButtonHandler);

        mUserMode = SystemProperties.get("ro.build.type").equals("user");

        mToast = Toast.makeText(this, null, Toast.LENGTH_SHORT);

        //poor link
        initPoorLink();
        //batched scan
        initBatchedScan();


       mHotspotOptBtn = (CheckBox) findViewById(R.id.hotspotOpt_enable_btn);

        mHotspotOptBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean newState = mHotspotOptBtn.isChecked();
                Log.v(TAG, "mHotspotOptBtn.setOnClickListener : " + newState);
                mWifiManager.setHotspotOptimization(newState);
            }
        });

        mStopAutojoinScanWhenConnectedBtn =
            (CheckBox) findViewById(R.id.stopScanWhenConnected_btn);
        mStopAutojoinScanWhenConnectedBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean newState = mStopAutojoinScanWhenConnectedBtn.isChecked();
                Log.v(TAG, "stop scan button.setOnClickListener : " + newState);
                if (newState == true) {
                    mWifiManager.setAutoJoinScanWhenConnected(false);
                } else {
                    mWifiManager.setAutoJoinScanWhenConnected(true);
                }
            }
        });

        mLunchWifiTestButton = (Button) findViewById(R.id.lunch_wifitest_btn);
        mLunchWifiTestButton.setOnClickListener(mLunchWifiTestButtonHandler);

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mWifiStateReceiver, mWifiStateFilter);
        refreshWifiStatus();
        initPoorLink();

        //M: batched scan
        registerReceiver(mBatchedScanStateReceiver, mBatchedScanFilter);


    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mWifiStateReceiver);
        //M: batched scan
        unregisterReceiver(mBatchedScanStateReceiver);

    }

    OnClickListener mScanButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            try {
                mWifiManager.startScan();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    OnClickListener mUpdateButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            refreshWifiStatus();
        }
    };

    OnClickListener mMacEditButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            updateMacAddr();
        }
    };

    OnClickListener mPoorLinkButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            Log.v(TAG, "mPoorLinkButtonHandler click ");
            updatePooorLinkInfo();
        }
    };

    OnClickListener mBatchedScanButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
            case R.id.batchedscan_start_btn:
                Log.v(TAG, "mBatchedScanButtonHandler click  START");
                startBatchedScan();
                boolean b = mWifiManager.requestBatchedScan(mBatchedScantSettings);
                mToast.setText("requestBatchedScan = " + b);
                mToast.show();

                break;
            case R.id.batchedscan_stop_btn:
                Log.v(TAG, "mBatchedScanButtonHandler click  STOP");
                mWifiManager.stopBatchedScan(mBatchedScantSettings);
                break;
            case R.id.batchedscan_poll_btn:
                Log.v(TAG, "mBatchedScanButtonHandler click  POLL");
                mWifiManager.pollBatchedScan();
                // it was the second button
                break;
            }
        }
    };

    OnClickListener mLunchWifiTestButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            Intent myIntent = new Intent();
            myIntent.setClassName("com.mediatek.wifitest", "com.mediatek.wifitest.MainActivity");
            try{
                startActivity(myIntent);
            }catch (Exception e){
                Log.v(TAG, "start wifiTest activity fail");

            }
        }
    };

    private void startBatchedScan() {

        try {
            mBatchedScantSettings.maxScansPerBatch =  (short) Integer.parseInt(mMaxScanPerBatchEdit.getText().toString(), 10);

            Log.v(TAG, "mBatchedScantSettings.maxScansPerBatch= " + mBatchedScantSettings.maxScansPerBatch);
        } catch (Exception e) {
            mBatchedScantSettings.maxScansPerBatch = 0;
        }

        try {
            mBatchedScantSettings.maxApPerScan =  (int) Integer.parseInt(mMaxApPerScanEdit.getText().toString(), 10);
            Log.v(TAG, "mBatchedScantSettings.maxApPerScan= " + mBatchedScantSettings.maxApPerScan);
        } catch (Exception e) {
            mBatchedScantSettings.maxApPerScan = 0;
        }

        try {
            mBatchedScantSettings.channelSet = new ArrayList(Arrays.asList(mChannelSetEdit.getText().toString().split(",")));
            Log.v(TAG, "mBatchedScantSettings.channelSet= " + mBatchedScantSettings.channelSet);
        } catch (Exception e) {
            mBatchedScantSettings.channelSet = null;
        }

        try {
            mBatchedScantSettings.scanIntervalSec =  (short) Integer.parseInt(mScanIntervalSecEdit.getText().toString(), 10);
            Log.v(TAG, "mBatchedScantSettings.scanIntervalSec= " + mBatchedScantSettings.scanIntervalSec);
        } catch (Exception e) {
            mBatchedScantSettings.scanIntervalSec = 0;
        }

        try {
            mBatchedScantSettings.maxApForDistance =  (short) Integer.parseInt(mMaxApPerDistanceEdit.getText().toString(), 10);
            Log.v(TAG, "mBatchedScantSettings.maxApForDistance= " + mBatchedScantSettings.maxApForDistance);
        } catch (Exception e) {
            mBatchedScantSettings.maxApForDistance = 0;
        }
    }

    private void setProfilingInfo(boolean state) {
        Log.v(TAG, "setProfilingInfo " + state);
        mProfilingInfo = state;

        if (mProfilingInfo == false)SystemProperties.set("persist.sys.poorlinkProfile", "0");
        else SystemProperties.set("persist.sys.poorlinkProfile", "1");
    }

    private void updatePooorLinkInfo() {

        Log.i(TAG, "updatePooorLinkInfo in");

        boolean isPoorLinkDetectEnabled = SystemProperties.getBoolean("persist.sys.poorlinkEnable", false) ;

        if (isPoorLinkDetectEnabled == false) {
            Log.i(TAG, "updatePoorLink isPoorLinkDetectEnabled= " + isPoorLinkDetectEnabled);
            mPoorLinkGoodLabel.setVisibility(View.GONE);
            mPoorLinkGoodEdit.setVisibility(View.GONE);
            mPoorLinkBadLabel.setVisibility(View.GONE);
            mPoorLinkBadEdit.setVisibility(View.GONE);
            mPoorLinkAddBtn.setVisibility(View.GONE);
            mCbProfiling.setVisibility(View.GONE);
            mCbPoorLinkBtn.setVisibility(View.GONE);
            mPoorLinkRssiLabel.setVisibility(View.GONE);
            mPoorLinkRssiEdit.setVisibility(View.GONE);
            mPoorLinkLinkSpeedLabel.setVisibility(View.GONE);
            mPoorLinkLinkSpeedEdit.setVisibility(View.GONE);
            return;
        }

        //update isProfiling
        boolean newState = mCbProfiling.isChecked();
        Log.v(TAG, "mCbProfiling.isChecked : " + newState);
         mProfilingInfo = newState;

         boolean plebtn = mCbPoorLinkBtn.isChecked();
         Log.v(TAG, "mCbPoorLinkBtn.isChecked : " + plebtn);
         mPoorLinkEnabledInfo = plebtn;
        setProfilingInfo(mProfilingInfo);
        mWifiManager.setPoorLinkProfilingOn(mProfilingInfo);

        //update poor link/good link threshold
        double plink = Double.parseDouble(mPoorLinkBadEdit.getText().toString());
        double glink = Double.parseDouble(mPoorLinkGoodEdit.getText().toString());

        double rssi = Double.parseDouble(mPoorLinkRssiEdit.getText().toString());
        double linkspeed = Double.parseDouble(mPoorLinkLinkSpeedEdit.getText().toString());

        if (glink > plink || glink > 1.0 || glink < 0 || plink > 1.0 || plink < 0) {
            Log.v(TAG,   "updatePooorLinkInfo, fail good= " + glink + " poor =" + plink);
            mToast.setText("Invalid threshold value good= " + glink + " poor=" + plink);
            mToast.show();
            return ;
        }

        if (rssi < -100 || rssi > -50) {
            Log.v(TAG,   "updatePooorLinkInfo, fail rssi= " + rssi);
            mToast.setText("Invalid threshold value rssi= " + rssi);
            mToast.show();
            return ;
        }

        if (linkspeed > 13 || linkspeed < 1) {
            Log.v(TAG,   "updatePooorLinkInfo, fail linkspeed= " + linkspeed);
            mToast.setText("Invalid threshold value linkspeed= " + linkspeed);
            mToast.show();
            return ;
        }

        mWifiManager.setPoorLinkThreshold("goodlink", glink);
        mWifiManager.setPoorLinkThreshold("poorlink", plink);

        mWifiManager.setPoorLinkThreshold("rssi", rssi);
        mWifiManager.setPoorLinkThreshold("linkspeed", linkspeed);

        mToast.setText("Update Success. Please restart WiFi");
        mToast.show();
    }
    ///M: add for Batched Scan
    private void initBatchedScan() {
        Log.v(TAG,   "initBatchedScan,  supported =" + mWifiManager.isBatchedScanSupported() + "wifi.dbg.bscan= " + SystemProperties.get("wifi.dbg.bscan", ""));


        mBatchedScanStartBtn = (Button) findViewById(R.id.batchedscan_start_btn);
        mBatchedScanStopBtn  = (Button) findViewById(R.id.batchedscan_stop_btn);
        mBatchedScanPollBtn  = (Button) findViewById(R.id.batchedscan_poll_btn);


        mMaxScanPerBatchLabel  = (TextView) findViewById(R.id.batchedscan_max_scan_label);
        mMaxScanPerBatchEdit = (EditText) findViewById(R.id.batchedscan_max_scan_value);
        mMaxApPerScanLabel  = (TextView) findViewById(R.id.batchedscan_max_ap_label);
        mMaxApPerScanEdit = (EditText) findViewById(R.id.batchedscan_max_ap_value);
        mChannelSetLabel  = (TextView) findViewById(R.id.batchedscan_channel_label);
        mChannelSetEdit = (EditText) findViewById(R.id.batchedscan_channel_value);
        mScanIntervalSecLabel  = (TextView) findViewById(R.id.batchedscan_interval_label);
        mScanIntervalSecEdit = (EditText) findViewById(R.id.batchedscan_interval_value);
        mMaxApPerDistanceLabel  = (TextView) findViewById(R.id.batchedscan_distance_label);
        mMaxApPerDistanceEdit = (EditText) findViewById(R.id.batchedscan_distance_value);

        mBatchedScanList = (TextView) findViewById(R.id.batched_scan_list);
        mBatchedScanListLabel = (TextView) findViewById(R.id.batched_scan_list_label);

        mBatchedScanFilter = new IntentFilter(WifiManager.BATCHED_SCAN_RESULTS_AVAILABLE_ACTION);

        if (mWifiManager.isBatchedScanSupported() == false || SystemProperties.get("wifi.dbg.bscan", "").equals("true") == false) {
            mBatchedScanStartBtn.setVisibility(View.GONE);
            mBatchedScanStopBtn.setVisibility(View.GONE);
            mBatchedScanPollBtn.setVisibility(View.GONE);

            mMaxScanPerBatchLabel.setVisibility(View.GONE);
            mMaxScanPerBatchEdit.setVisibility(View.GONE);
            mMaxApPerScanLabel.setVisibility(View.GONE);
            mMaxApPerScanEdit.setVisibility(View.GONE);
            mChannelSetLabel.setVisibility(View.GONE);
            mChannelSetEdit.setVisibility(View.GONE);
            mScanIntervalSecLabel.setVisibility(View.GONE);
            mScanIntervalSecEdit.setVisibility(View.GONE);
            mMaxApPerDistanceLabel.setVisibility(View.GONE);
            mMaxApPerDistanceEdit.setVisibility(View.GONE);
            mBatchedScanList.setVisibility(View.GONE);
            mBatchedScanListLabel.setVisibility(View.GONE);
            return;
        }

        mBatchedScanStartBtn.setOnClickListener(mBatchedScanButtonHandler);
        mBatchedScanStopBtn.setOnClickListener(mBatchedScanButtonHandler);
        mBatchedScanPollBtn.setOnClickListener(mBatchedScanButtonHandler);

        mBatchedScanStartBtn.setVisibility(View.VISIBLE);
        mBatchedScanStopBtn.setVisibility(View.VISIBLE);
        mBatchedScanPollBtn.setVisibility(View.VISIBLE);

        mMaxScanPerBatchLabel.setVisibility(View.VISIBLE);
        mMaxScanPerBatchEdit.setVisibility(View.VISIBLE);
        mMaxApPerScanLabel.setVisibility(View.VISIBLE);
        mMaxApPerScanEdit.setVisibility(View.VISIBLE);
        mChannelSetLabel.setVisibility(View.VISIBLE);
        mChannelSetEdit.setVisibility(View.VISIBLE);
        mScanIntervalSecLabel.setVisibility(View.VISIBLE);
        mScanIntervalSecEdit.setVisibility(View.VISIBLE);
        mMaxApPerDistanceLabel.setVisibility(View.VISIBLE);
        mMaxApPerDistanceEdit.setVisibility(View.VISIBLE);

        mBatchedScantSettings = new BatchedScanSettings();

    }

    private void initPoorLink() {
        mPoorLinkGoodLabel = (TextView) findViewById(R.id.poorlink_good_label);
        mPoorLinkGoodEdit = (EditText) findViewById(R.id.poorlink_goodvalue);
        mPoorLinkBadLabel = (TextView) findViewById(R.id.poorlink_bad_label);
        mPoorLinkBadEdit = (EditText) findViewById(R.id.poorlink_badvalue);
        mPoorLinkAddBtn = (Button) findViewById(R.id.poorlink_update_btn);
        mPoorLinkAddBtn.setOnClickListener(mPoorLinkButtonHandler);

        mCbPoorLinkBtn = (CheckBox) findViewById(R.id.poorlink_enable_btn);
        mCbProfiling = (CheckBox) findViewById(R.id.poorlink_Profiling_Screen);
        mPoorLinkRssiLabel =  (TextView) findViewById(R.id.poorlink_rssi_label);
        mPoorLinkRssiEdit = (EditText) findViewById(R.id.poorlink_rssivalue);
        mPoorLinkLinkSpeedLabel = (TextView) findViewById(R.id.poorlink_linkspeed_label);
        mPoorLinkLinkSpeedEdit = (EditText) findViewById(R.id.poorlink_linkspeedvalue);


        boolean isPoorLinkDetectEnabled = SystemProperties.getBoolean("persist.sys.poorlinkEnable", false) ;

        if (isPoorLinkDetectEnabled == false) {
            Log.i(TAG, "poor link function disable no show poor link option");
            mPoorLinkGoodLabel.setVisibility(View.GONE);
            mPoorLinkGoodEdit.setVisibility(View.GONE);
            mPoorLinkBadLabel.setVisibility(View.GONE);
            mPoorLinkBadEdit.setVisibility(View.GONE);
            mPoorLinkAddBtn.setVisibility(View.GONE);
            mCbProfiling.setVisibility(View.GONE);

            mCbPoorLinkBtn.setVisibility(View.GONE);
            mPoorLinkRssiLabel.setVisibility(View.GONE);
            mPoorLinkRssiEdit.setVisibility(View.GONE);
            mPoorLinkLinkSpeedLabel.setVisibility(View.GONE);
            mPoorLinkLinkSpeedEdit.setVisibility(View.GONE);
            return;
        }

        mPoorLinkGoodLabel.setVisibility(View.VISIBLE);
        mPoorLinkGoodEdit.setVisibility(View.VISIBLE);
        mPoorLinkBadLabel.setVisibility(View.VISIBLE);
        mPoorLinkBadEdit.setVisibility(View.VISIBLE);
        mPoorLinkAddBtn.setVisibility(View.VISIBLE);

        mPoorLinkRssiLabel.setVisibility(View.VISIBLE);
        mPoorLinkRssiEdit.setVisibility(View.VISIBLE);
        mPoorLinkLinkSpeedLabel.setVisibility(View.VISIBLE);
        mPoorLinkLinkSpeedEdit.setVisibility(View.VISIBLE);
        double poorlinkGood = mWifiManager.getPoorLinkThreshold(true);
        double poorlinkPoor = mWifiManager.getPoorLinkThreshold(false);

        Log.i(TAG, "getPoorLink poorlinkGood= " + poorlinkGood + " poorlinkPoor= " + poorlinkPoor);

        boolean isprofilingEnable = false;

        if (SystemProperties.getBoolean("persist.sys.poorlinkProfile", false) == true) {
            isprofilingEnable = true;
        }

        mPoorLinkGoodEdit.setText(poorlinkGood + "");
        mPoorLinkBadEdit.setText(poorlinkPoor + "");
        mPoorLinkRssiEdit.setText("-85");
        mPoorLinkLinkSpeedEdit.setText("5");

        int poorlinken = 0;
        if (poorlinken == 1) {
            mCbPoorLinkBtn.setChecked(true);
            mPoorLinkEnabledInfo = true;
        } else {
            mCbPoorLinkBtn.setChecked(false);
            mPoorLinkEnabledInfo = false;
        }

        if (isprofilingEnable == true) {
            mCbProfiling.setChecked(true);
            setProfilingInfo(true);
        } else {
            mCbProfiling.setChecked(false);
            setProfilingInfo(false);
        }

        mCbPoorLinkBtn.setVisibility(View.VISIBLE);
        mCbProfiling.setVisibility(View.VISIBLE);

        mCbPoorLinkBtn.setOnClickListener(new View.OnClickListener() {
                   public void onClick(View v) {
                       boolean newState2 = mCbPoorLinkBtn.isChecked();
                       Log.v(TAG, "mCbPoorLinkBtn.setOnClickListener : " + newState2);
                       mPoorLinkEnabledInfo = newState2;
                       Global.putInt(getContentResolver(),
                       Global.WIFI_WATCHDOG_POOR_NETWORK_TEST_ENABLED,
                       mPoorLinkEnabledInfo ? 1 : 0);
                   }
               });

        mCbProfiling.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean newState = mCbProfiling.isChecked();
                Log.v(TAG, "mCbProfiling.setOnClickListener : " + newState);
                //   mProfilingInfo = newState;
                //   setProfilingInfo(mProfilingInfo);
                mProfilingInfo = newState;
                setProfilingInfo(mProfilingInfo);
                mWifiManager.setPoorLinkProfilingOn(mProfilingInfo);
            }
        });
    }

    private void getMacAddr() {


        try {
            IBinder binder = ServiceManager.getService("NvRAMAgent");
            NvRAMAgent agent = NvRAMAgent.Stub.asInterface(binder);

            mRandomMacAddr = new short[MAC_ADDRESS_DIGITS];

            if (mUserMode) {
                mMacAddrLabel.setVisibility(View.GONE);
                mMacAddrEdit.setVisibility(View.GONE);
                mMacAddBtn.setVisibility(View.GONE);
            } else {
                StringBuilder sb = new StringBuilder();
                Random rand = new Random();
                NumberFormat formatter = new DecimalFormat("00");
                int end1 = rand.nextInt(100);
                int end2 = rand.nextInt(100);
                String num1 = formatter.format(end1);
                String num2 = formatter.format(end2);

                sb.append("00:08:22:11:");
                sb.append(num1).append(":").append(num2);

                mMacAddrLabel.setVisibility(View.VISIBLE);
                mMacAddrEdit.setVisibility(View.VISIBLE);
                mMacAddBtn.setVisibility(View.VISIBLE);
                System.out.println("string buffer:" + sb);
                mMacAddrEdit.setText(sb);
                MacAddressRandom = sb.toString();

            }
        } catch (Exception e) {
            e.printStackTrace();
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

    private void updateMacAddr() {

        try {
            int i = 0;
            IBinder binder = ServiceManager.getService("NvRAMAgent");
            NvRAMAgent agent = NvRAMAgent.Stub.asInterface(binder);

            //parse mac address firstly
            StringTokenizer txtBuffer = new StringTokenizer(mMacAddrEdit.getText().toString(), ":");

            while (txtBuffer.hasMoreTokens()) {
                mRandomMacAddr[i] = (short) Integer.parseInt(txtBuffer.nextToken(), 16);
                System.out.println(i + ":" + mRandomMacAddr[i]);
                i++;
            }

            if (i != 6) {
                mToast.setText("The format of mac address is not correct");
                mToast.show();
                return;
            }

            byte[] buff = null;

            try {
                buff = agent.readFileByName(MAC_ADDRESS_FILENAME);
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (i = 0; i < MAC_ADDRESS_DIGITS; i ++) {
                buff[i + 4] = (byte) mRandomMacAddr[i];
            }

            int flag = 0;

            try {
                flag = agent.writeFileByName(MAC_ADDRESS_FILENAME, buff);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (flag > 0) {
                mToast.setText("Update successfully.\r\nPlease reboot this device");
                mToast.show();
            } else {
                mToast.setText("Update failed");
                mToast.show();
            }

        } catch (Exception e) {
            mToast.setText(e.getMessage() + ":" + e.getCause());
            mToast.show();
            e.printStackTrace();
        }
    }

    private void refreshWifiStatus() {
        final WifiInfo wifiInfo = mWifiManager.getConnectionInfo();

        if (wifiInfo == null) {
            return;
        }

        Log.i(TAG, "refreshWifiStatus is called");
        setWifiStateText(mWifiManager.getWifiState());
        mBSSID.setText(wifiInfo.getBSSID());
        try {
            mHiddenSSID.setText(String.valueOf(wifiInfo.getHiddenSSID()));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        int ipAddr = wifiInfo.getIpAddress();
        StringBuffer ipBuf = new StringBuffer();
        ipBuf.append(ipAddr  & 0xff).append('.').
        append((ipAddr >>>= 8) & 0xff).append('.').
        append((ipAddr >>>= 8) & 0xff).append('.').
        append((ipAddr >>>= 8) & 0xff);
        mIPAddr.setText(ipBuf);



        if (wifiInfo.getLinkSpeed() > 0) {
            mLinkSpeed.setText(String.valueOf(wifiInfo.getLinkSpeed()) + " Mbps");
        } else {
            mLinkSpeed.setText(R.string.unknown_string);
        }

        mMACAddr.setText(wifiInfo.getMacAddress());
        mNetworkId.setText(String.valueOf(wifiInfo.getNetworkId()));

        mWifiCapability.setText(String.valueOf(getWifiCapability()));

        if (wifiInfo.getRssi() != INVALID_RSSI) {
            mRSSI.setText(String.valueOf(wifiInfo.getRssi()));
        } else {
            mRSSI.setText(R.string.na_string);
        }

        if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
            mSSID.setText(wifiInfo.getSSID());
        } else {
            mSSID.setText("");
        }

        SupplicantState supplicantState = wifiInfo.getSupplicantState();
        setSupplicantStateText(supplicantState);

        getMacAddr();

        updateSystemProperties();
    }

    private void updateSystemProperties() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < WIFI_SYSTEM_PROPERTY.length; i++) {
            sb.append("[" + WIFI_SYSTEM_PROPERTY[i] + "]: [" + SystemProperties.get(WIFI_SYSTEM_PROPERTY[i], "") + "]\r\n");
        }

        mSystemProperties.setText(sb);

    }

    private void setSupplicantStateText(SupplicantState supplicantState) {
        if (SupplicantState.FOUR_WAY_HANDSHAKE.equals(supplicantState)) {
            mSupplicantState.setText("FOUR WAY HANDSHAKE");
        } else if (SupplicantState.ASSOCIATED.equals(supplicantState)) {
            mSupplicantState.setText("ASSOCIATED");
        } else if (SupplicantState.ASSOCIATING.equals(supplicantState)) {
            mSupplicantState.setText("ASSOCIATING");
        } else if (SupplicantState.COMPLETED.equals(supplicantState)) {
            mSupplicantState.setText("COMPLETED");
        } else if (SupplicantState.DISCONNECTED.equals(supplicantState)) {
            mSupplicantState.setText("DISCONNECTED");
        } else if (SupplicantState.DORMANT.equals(supplicantState)) {
            mSupplicantState.setText("DORMANT");
        } else if (SupplicantState.GROUP_HANDSHAKE.equals(supplicantState)) {
            mSupplicantState.setText("GROUP HANDSHAKE");
        } else if (SupplicantState.INACTIVE.equals(supplicantState)) {
            mSupplicantState.setText("INACTIVE");
        } else if (SupplicantState.INVALID.equals(supplicantState)) {
            mSupplicantState.setText("INVALID");
        } else if (SupplicantState.SCANNING.equals(supplicantState)) {
            mSupplicantState.setText("SCANNING");
        } else if (SupplicantState.UNINITIALIZED.equals(supplicantState)) {
            mSupplicantState.setText("UNINITIALIZED");
        } else {
            mSupplicantState.setText("BAD");
            Log.e(TAG, "supplicant state is bad");
        }
    }

    private void setWifiStateText(int wifiState) {
        String wifiStateString;

        switch (wifiState) {
        case WifiManager.WIFI_STATE_DISABLING:
            wifiStateString = getString(R.string.wifi_state_disabling);
            break;
        case WifiManager.WIFI_STATE_DISABLED:
            wifiStateString = getString(R.string.wifi_state_disabled);
            break;
        case WifiManager.WIFI_STATE_ENABLING:
            wifiStateString = getString(R.string.wifi_state_enabling);
            break;
        case WifiManager.WIFI_STATE_ENABLED:
            wifiStateString = getString(R.string.wifi_state_enabled);
            break;
        case WifiManager.WIFI_STATE_UNKNOWN:
            wifiStateString = getString(R.string.wifi_state_unknown);
            break;
        default:
            wifiStateString = "BAD";
            Log.e(TAG, "wifi state is bad");
            break;
        }

        if (wifiState == WifiManager.WIFI_STATE_DISABLED) {
            mScanList.setText("");
        }

        mWifiState.setText(wifiStateString);
    }

    private void handleSignalChanged(int rssi) {
        if (rssi != INVALID_RSSI) {
            mRSSI.setText(String.valueOf(rssi));
        } else {
            mRSSI.setText(R.string.na_string);
        }
    }

    private void handleWifiStateChanged(int wifiState) {
        setWifiStateText(wifiState);
    }

    private void handleScanResultsAvailable() {
        List<ScanResult> list = mWifiManager.getScanResults();

        StringBuffer scanList = new StringBuffer();

        if (list != null) {
            for (int i = list.size() - 1; i >= 0; i--) {
                final ScanResult scanResult = list.get(i);

                if (scanResult == null) {
                    continue;
                }

                if (TextUtils.isEmpty(scanResult.SSID)) {
                    continue;
                }

                scanList.append(scanResult.SSID);
                scanList.append("(Ch:" + getChannelFromFrequency(scanResult.frequency) + ")-" + scanResult.level + "dBm\r\n");

                if (scanResult.capabilities.equals("[ESS]")) {
                    scanList.append("[OPEN] \r\n\r\n");
                } else {
                    scanList.append(scanResult.capabilities + " \r\n\r\n");
                }
            }
        }

        mScanList.setText(scanList);
    }


    private void handleBatchedScanResultsAvailable() {

        mBatchedScanList.setText("");

        List<BatchedScanResult>  blist = mWifiManager.getBatchedScanResults();

        StringBuffer bscanList = new StringBuffer();

        if (blist != null) {
            bscanList.append("currentTime= " + System.currentTimeMillis() + "\n");
            bscanList.append("size =" + blist.size() + "\n");
            Log.i(TAG, "bscanList: blist.size()" + blist.size());

            for (int i = 0; i < blist.size() ; i++) {
                bscanList.append("blist# =" + i + "\n");
                final BatchedScanResult bscanResult = blist.get(i);
                bscanList.append(bscanResult.toString());
                bscanList.append("\n");
                Log.i(TAG, "bscanList" + bscanList);

            }
        }

        mBatchedScanList.setText(bscanList);
        Log.i(TAG, "bscanList done");
    }



    private void handleSupplicantStateChanged(SupplicantState state, boolean hasError, int error) {
        if (hasError) {
            mSupplicantState.setText("ERROR AUTHENTICATING");
        } else {
            setSupplicantStateText(state);
        }
    }

    private void handleNetworkStateChanged(NetworkInfo networkInfo) {
        if (mWifiManager.isWifiEnabled()) {
            final WifiInfo wifiInfo = mWifiManager.getConnectionInfo();

            if (wifiInfo != null) {
                String summary = Summary.get(this, wifiInfo.getSSID(),
                                             networkInfo.getDetailedState());
                mNetworkState.setText(summary);
            }
        }
    }
}
