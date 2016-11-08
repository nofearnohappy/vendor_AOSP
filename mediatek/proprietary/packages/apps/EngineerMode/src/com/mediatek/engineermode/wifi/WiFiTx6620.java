/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERfETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.engineermode.wifi;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.mediatek.engineermode.ChipSupport;
import com.mediatek.engineermode.Elog;
import com.mediatek.engineermode.R;

import java.util.Arrays;
import java.util.Locale;

public class WiFiTx6620 extends WiFiTestActivity implements OnClickListener {
    private static final String TAG = "EM/WiFi_Tx";
    private static final long MAX_VALUE = 0xFFFFFFFF;
    private static final int MIN_VALUE = 0x0;
    private static final int HANDLER_EVENT_GO = 1;
    private static final int HANDLER_EVENT_STOP = 2;
    private static final int HANDLER_EVENT_TIMER = 3;
    private static final int HANDLER_EVENT_FINISH = 4;
    private static final int MAX_LOWER_RATE_NUMBER = 12;
    private static final int MAX_HIGH_RATE_NUMBER = 23;
    private static final int CCK_RATE_NUMBER = 4;
    private static final int DEFAULT_PKT_CNT = 3000;
    private static final int DEFAULT_PKT_LEN = 1024;
    private static final int DEFAULT_TX_GAIN = 0;
    private static final int ONE_SENCOND = 1000;
    private static final int BIT_8_MASK = 0xFF;
    private static final int LENGTH_3 = 3;
    private static final int BANDWIDTH_40MHZ_MASK = 0x8000;
    private static final int BANDWIDTH_INDEX_20M = 0;
    private static final int BANDWIDTH_INDEX_40M = 1;
    private static final int BANDWIDTH_INDEX_80M = 2;
    private static final int COMMAND_INDEX_OUTPUTPOWER = 4;
    private static final int COMMAND_INDEX_STOPTEST = 0;
    private static final int COMMAND_INDEX_STARTTX = 1;
    private static final int COMMAND_INDEX_LOCALFREQ = 5;
    private static final int COMMAND_INDEX_CARRIER = 6;
    private static final int COMMAND_INDEX_CARRIER_NEW = 10;
    private static final int RATE_MODE_MASK = 31;
    private static final int RATE_MCS_INDEX = 0x20;
    private static final int RATE_NOT_MCS_INDEX = 0x09;
    private static final int CWMODE_CCKPI = 5;
    private static final int CWMODE_OFDMLTF = 2;
    private static final int TXOP_LIMIT_VALUE = 0x00020000;
    private static final int TEST_MODE_TX = 0;
    private static final int TEST_MODE_DUTY = 1;
    private static final int TEST_MODE_CARRIER = 2;
    private static final int TEST_MODE_LEAKAGE = 3;
    private static final int TEST_MODE_POWEROFF = 4;
    private static final long[] PACKCONTENT_BUFFER = { 0xff220004, 0x33440006,
            0x55660008, 0x55550019, 0xaaaa001b, 0xbbbb001d };
    private static final int HIGH_RATE_PREAMBLE_BASE = 2;
    private boolean mHighRateSelected = false;
    private boolean mCCKRateSelected = true;
    private int mLastRateGroup = 0;
    private int mLastBandwidth = 0;
    private Spinner mChannelSpinner = null;
    private Spinner mGuardIntervalSpinner = null;
    private Spinner mBandwidthSpinner = null;
    private Spinner mPreambleSpinner = null;
    private EditText mEtPkt = null;
    private EditText mEtPktCnt = null;
    private EditText mEtTxGain = null;
    private Spinner mRateSpinner = null;
    private Spinner mModeSpinner = null;
    // private EditText mXTEdit = null;
    // private Button mWriteBtn = null;
    // private Button mReadBtn = null;
    // private CheckBox mALCCheck = null;
    private Button mBtnGo = null;
    private Button mBtnStop = null;
    private ArrayAdapter<String> mChannelAdapter = null;
    // private ArrayAdapter<String> mRateAdapter = null;
    private ArrayAdapter<String> mModeAdapter = null;
    private ArrayAdapter<String> mPreambleAdapter = null;
    // private ArrayAdapter<String> mGuardIntervalAdapter = null;
    // private ArrayAdapter<String> mBandwidthAdapter = null;
    private int mModeIndex = 0;
    private int mPreambleIndex = 0;
    private int mBandwidthIndex = 0;
    private int mGuardIntervalIndex = 0;
    private static final int ANTENNA = 0;

    private RateInfo mRate = null;
    private ChannelInfo mChannel = null;
    private long mPktLenNum = DEFAULT_PKT_LEN;
    private long mCntNum = DEFAULT_PKT_CNT;
    private long mTxGainVal = DEFAULT_TX_GAIN;
    private boolean mTestInPorcess = false;
    private HandlerThread mTestThread = null;
    private final Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (HANDLER_EVENT_FINISH == msg.what) {
                Log.v("@M_" + TAG, "receive HANDLER_EVENT_FINISH");
                setViewEnabled(true);
            }
        }
    };
    private Handler mEventHandler = null;

    String[] mMode = { "continuous packet tx", "100% duty cycle",
            "carrier suppression", "local leakage", "enter power off" };
    String[] mPreamble = { "Normal", "CCK short", "802.11n mixed mode",
            "802.11n green field", "802.11ac"};
    String[] mBandwidth = { "20MHz", "40MHz", "U20MHz", "L20MHz", "Advanced"};
    String[] mGuardInterval = { "800ns", "400ns", };
    private Spinner mSpBwAdvCbw = null;
    private Spinner mSpBwAdvDbw = null;
    private Spinner mSpBwAdvPrimCh = null;
    int mChannelBandwidth = 0;
    int mDataBandwidth = 0;
    int mPrimarySetting = 0;
    private static final String[] BW_ADVANCED_ITEMS = {"BW20", "BW40", "BW80"};
    private static final int BW_INDX_ADVANCED = 4;
    private ArrayAdapter<String> mDbwAdapter = null;
    private ArrayAdapter<String> mPrimChAdapter = null;
    private boolean mIsAdvancedMode = false;
    private ArrayAdapter<String> mRateAdapter = null;
    private int mRateUpdateCounter = 0;
    private int mTargetModeIndex = 0;

    static class RateInfo {
        private static final short EEPROM_RATE_GROUP_CCK = 0;
        private static final short EEPROM_RATE_GROUP_OFDM_6_9M = 1;
        private static final short EEPROM_RATE_GROUP_OFDM_12_18M = 2;
        private static final short EEPROM_RATE_GROUP_OFDM_24_36M = 3;
        private static final short EEPROM_RATE_GROUP_OFDM_48_54M = 4;
        private static final short EEPROM_RATE_GROUP_OFDM_MCS0_32 = 5;
        private static final short EEPROM_RATE_GROUP_OFDM_MEDIUM = 6;

        int mRateIndex = 0;

        int mOFDMStartIndex = 4;

        private final short[] mUcRateGroupEep = { EEPROM_RATE_GROUP_CCK,
                EEPROM_RATE_GROUP_CCK, EEPROM_RATE_GROUP_CCK,
                EEPROM_RATE_GROUP_CCK, EEPROM_RATE_GROUP_OFDM_6_9M,
                EEPROM_RATE_GROUP_OFDM_6_9M, EEPROM_RATE_GROUP_OFDM_12_18M,
                EEPROM_RATE_GROUP_OFDM_12_18M, EEPROM_RATE_GROUP_OFDM_24_36M,
                EEPROM_RATE_GROUP_OFDM_24_36M, EEPROM_RATE_GROUP_OFDM_48_54M,
                EEPROM_RATE_GROUP_OFDM_48_54M,
                /* for future use */
                EEPROM_RATE_GROUP_OFDM_MCS0_32, EEPROM_RATE_GROUP_OFDM_MCS0_32,
                EEPROM_RATE_GROUP_OFDM_MCS0_32, EEPROM_RATE_GROUP_OFDM_MCS0_32,
                EEPROM_RATE_GROUP_OFDM_MCS0_32, EEPROM_RATE_GROUP_OFDM_MCS0_32,
                EEPROM_RATE_GROUP_OFDM_MCS0_32, EEPROM_RATE_GROUP_OFDM_MCS0_32,
                EEPROM_RATE_GROUP_OFDM_MCS0_32, EEPROM_RATE_GROUP_OFDM_MCS0_32,
                EEPROM_RATE_GROUP_OFDM_MCS0_32, };
        private final String[] mPszRate = { "1M", "2M", "5.5M", "11M", "6M",
                "9M", "12M", "18M", "24M", "36M", "48M", "54M",
                /* for future use */
                "MCS0", "MCS1", "MCS2", "MCS3", "MCS4", "MCS5", "MCS6", "MCS7",
                "MCS8", "MCS9", "MCS32",

        };

        private static final String[] ADVANCED_RATE_20M = {"1M", "2M", "5.5M", "11M", "6M",
                "9M", "12M", "18M", "24M", "36M", "48M", "54M",
                "MCS0", "MCS1", "MCS2", "MCS3", "MCS4", "MCS5", "MCS6", "MCS7",
                "MCS8", };

        private static final String[] ADVANCED_RATE_40M = {"MCS0", "MCS1", "MCS2", "MCS3", "MCS4", "MCS5", "MCS6", "MCS7",
                "MCS8", "MCS9", "MCS32", };

        private static final String[] ADVANCED_RATE_80M = {"MCS0", "MCS1", "MCS2", "MCS3", "MCS4", "MCS5", "MCS6", "MCS7",
                "MCS8", "MCS9", };

        private void setRateAdapterTo(ArrayAdapter<String> adapter, String[] rateArr) {
            adapter.clear();
            for (int i = 0; i < rateArr.length; i++) {
                adapter.add(rateArr[i]);
            }
        }

        private final int[] mRateCfg = { 2, 4, 11, 22, 12, 18, 24, 36, 48, 72,
                96, 108,
                /* here we need to add cfg data for MCS*** */
                22, 12, 18, 24, 36, 48, 72, 96, 108 };

        /**
         * Get total rate number
         *
         * @return The total rate number
         */
        int getRateNumber() {
            return mPszRate.length;
        }

        /**
         * Get rate string by {@link #mRateIndex}
         *
         * @return Rate string
         */
        String getRate() {
            return mPszRate[mRateIndex];
        }

        /**
         * Get rate configured data
         *
         * @return Rate data
         */
        int getRateCfg() {
            return mRateCfg[mRateIndex];
        }

        /**
         * Get group the rate belong to
         *
         * @return Group ID
         */
        int getUcRateGroupEep() {
            return mUcRateGroupEep[mRateIndex];
        }

        int getRateGroup(int rateIndex) {
            int group = -1;
            if (rateIndex >= 0 && rateIndex < mUcRateGroupEep.length) {
                group = mUcRateGroupEep[rateIndex];
            }
            return group;
        }

        int getRateGroupExt(int rateIndex) {
            int group = getRateGroup(rateIndex);
            if (group > EEPROM_RATE_GROUP_CCK && group < EEPROM_RATE_GROUP_OFDM_MCS0_32) {
                group = EEPROM_RATE_GROUP_OFDM_MEDIUM;
            }
            return group;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_tx_6620);

        if (WiFi.is11acSupported()) {
            mIsAdvancedMode = true;
        }
        mChannelSpinner = (Spinner) findViewById(R.id.WiFi_Channel_Spinner);
        mPreambleSpinner = (Spinner) findViewById(R.id.WiFi_Preamble_Spinner);
        mEtPkt = (EditText) findViewById(R.id.WiFi_Pkt_Edit);
        mEtPktCnt = (EditText) findViewById(R.id.WiFi_Pktcnt_Edit);
        mEtTxGain = (EditText) findViewById(R.id.WiFi_Tx_Gain_Edit); // Tx gain
        mRateSpinner = (Spinner) findViewById(R.id.WiFi_Rate_Spinner);
        mModeSpinner = (Spinner) findViewById(R.id.WiFi_Mode_Spinner);
        // mXTEdit = (EditText) findViewById(R.id.WiFi_XtalTrim_Edit);
        // mWriteBtn = (Button) findViewById(R.id.WiFi_XtalTrim_Write);
        // mReadBtn = (Button) findViewById(R.id.WiFi_XtalTrim_Read);
        // mALCCheck = (CheckBox) findViewById(R.id.WiFi_ALC);
        mBtnGo = (Button) findViewById(R.id.WiFi_Go);
        mBtnStop = (Button) findViewById(R.id.WiFi_Stop);
        mBandwidthSpinner = (Spinner) findViewById(R.id.WiFi_Bandwidth_Spinner);
        mGuardIntervalSpinner = (Spinner) findViewById(R.id.WiFi_Guard_Interval_Spinner);

        mTestThread = new HandlerThread("Wifi Tx Test");
        mTestThread.start();
        mEventHandler = new EventHandler(mTestThread.getLooper());
        mBtnGo.setOnClickListener(this);
        mBtnStop.setOnClickListener(this);
        mChannel = new ChannelInfo();
        mRate = new RateInfo();
        // mEtPktCnt.setOnKeyListener(new View.OnKeyListener() {
        // public boolean onKey(View v, int keyCode, KeyEvent event) {
        // CharSequence inputVal = mEtPktCnt.getText();
        // if (TextUtils.equals(inputVal, "0")) {
        // Toast.makeText(WiFiTx6620.this,
        // R.string.wifi_toast_packet_error,
        // Toast.LENGTH_SHORT).show();
        // mEtPktCnt.setText(String.valueOf(DEFAULT_PKT_CNT));
        // }
        // return false;
        // }
        // });
        mChannelAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item);
        mChannelAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mChannel.addSupported2dot4gChannels(mChannelAdapter, false);
        mChannelSpinner.setAdapter(mChannelAdapter);
        mChannelSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                if (EMWifi.sIsInitialed) {
                    String name = mChannelAdapter.getItem(position);
                    mChannel.setSelectedChannel(name);
                    int channelId = ChannelInfo.parseChannelId(name);
                    EMWifi.setChannel(ChannelInfo.getChannelFrequency(channelId));
                    uiUpdateTxPower();
                } else {
                    showDialog(DIALOG_WIFI_ERROR);
                }
            }

            public void onNothingSelected(AdapterView<?> arg0) {
                Log.v("@M_" + TAG, "onNothingSelected");
            }
        });
        mRateAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item);
        mRateAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (!mIsAdvancedMode) {
            for (int i = 0; i < mRate.getRateNumber(); i++) {
                mRateAdapter.add(mRate.mPszRate[i]);
            }
        }

        mRateSpinner.setAdapter(mRateAdapter);
        mRateSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> arg0, View arg1,
                    int arg2, long arg3) {
                if (!EMWifi.sIsInitialed) {
                    showDialog(DIALOG_WIFI_ERROR);
                    return;
                }

                int lastGroup = mRate.getRateGroupExt(mRate.mRateIndex);
                mRate.mRateIndex = arg2;
                int targetIndex = arg2;
                if (mIsAdvancedMode) {
                    targetIndex = convertAdvancedRateIndex2Normal(mChannelBandwidth, targetIndex);
                    mRate.mRateIndex = targetIndex;
                }
                int currentGroup = mRate.getRateGroupExt(mRate.mRateIndex);

                // set Tx Rate
                Log.i("@M_" + TAG, "The mRateIndex is : " + arg2 + " targetIndex:" + targetIndex);

                boolean updatePreamble = (lastGroup != currentGroup) ? true : false;
                if (updatePreamble) {
                    // judge if high rate item selected MCS0~MCS7 MCS32
                    mHighRateSelected = targetIndex >= MAX_LOWER_RATE_NUMBER ? true
                            : false;
                    int delta = mHighRateSelected ? HIGH_RATE_PREAMBLE_BASE : 0;
                    mPreambleAdapter.clear();
                    AddPreambleItems(currentGroup);
                    mPreambleIndex = delta;
                    mPreambleSpinner.setAdapter(mPreambleAdapter);
                }
                uiUpdateTxPower();

                if (targetIndex >= CCK_RATE_NUMBER) {
                    if (mCCKRateSelected) {
                        mCCKRateSelected = false;
                        mModeAdapter.remove(mMode[2]);
                        mModeSpinner.setSelection(0);
                    }
                } else {
                    if (!mCCKRateSelected) {
                        mCCKRateSelected = true;
                        mModeAdapter.insert(mMode[2], 2);
                        mModeSpinner.setSelection(0);
                    }
                }
                mRateUpdateCounter++;
                updateChannels();
                mLastRateGroup = mRate.getUcRateGroupEep();
            }

            public void onNothingSelected(AdapterView<?> arg0) {
                Log.v("@M_" + TAG, "onNothingSelected");
            }
        });
        if (!mIsAdvancedMode) {
            mRateSpinner.setSelection(mRate.mOFDMStartIndex); //show 5G channel default.
        }
        mModeAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item);
        mModeAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (int i = 0; i < mMode.length; i++) {
            mModeAdapter.add(mMode[i]);
        }
        mModeSpinner.setAdapter(mModeAdapter);
        mModeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> arg0, View arg1,
                    int arg2, long arg3) {
                mModeIndex = arg2;
                Log.i("@M_" + TAG, "The mModeIndex is : " + arg2);
                if (!mCCKRateSelected) {
                    if (arg2 >= 2) {
                        mModeIndex++;
                    }
                }
            }

            public void onNothingSelected(AdapterView<?> arg0) {
                Log.v("@M_" + TAG, "onNothingSelected");
            }
        });
        // 802.11n select seetings
        mPreambleAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item);
        mPreambleAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        AddPreambleItems(RateInfo.EEPROM_RATE_GROUP_CCK);
        mPreambleSpinner.setAdapter(mPreambleAdapter);
        mPreambleSpinner
                .setOnItemSelectedListener(new OnItemSelectedListener() {

                    public void onItemSelected(AdapterView<?> arg0, View arg1,
                            int arg2, long arg3) {
                        mPreambleIndex = arg2 + (mHighRateSelected ? 2 : 0);

                    }

                    public void onNothingSelected(AdapterView<?> arg0) {
                        Log.v("@M_" + TAG, "onNothingSelected");
                    }
                });
        // Bandwidth seetings
        ArrayAdapter<String> bandwidthAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item);
        bandwidthAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        if (mIsAdvancedMode) {
            bandwidthAdapter.add(mBandwidth[BW_INDX_ADVANCED]);
        } else {
            for (int i = 0; i < BW_INDX_ADVANCED; i++) {
                bandwidthAdapter.add(mBandwidth[i]);
            }
        }
        mBandwidthSpinner.setAdapter(bandwidthAdapter);
        mBandwidthSpinner
                .setOnItemSelectedListener(new OnItemSelectedListener() {

                    public void onItemSelected(AdapterView<?> arg0, View arg1,
                            int arg2, long arg3) {
                        // max Bandwidth setting value is 4
                        Elog.d(TAG, "mBandwidthSpinner.onItemSelected:" + arg2 + " mBandwidthIndex:" + mBandwidthIndex + " mLastBandwidth:" + mLastBandwidth);
                        mBandwidthIndex = arg2 < mBandwidth.length ? arg2
                                : mBandwidthIndex;
                        if (mBandwidth[BW_INDX_ADVANCED].equals(mBandwidthSpinner.getSelectedItem().toString())) {
                            mBandwidthIndex = BW_INDX_ADVANCED;
                        }
                        if (mBandwidthIndex == BW_INDX_ADVANCED) {
                            onAdvancedBandwidthSelected();
                        } else {
                            updateChannels();
                            findViewById(R.id.wifi_bandwidth_advanced_ll).setVisibility(View.GONE);
                        }
                        mLastBandwidth = mBandwidthIndex;
                    }

                    public void onNothingSelected(AdapterView<?> arg0) {
                        Log.v("@M_" + TAG, "onNothingSelected");
                    }
                });
        // Guard Interval seetings
        ArrayAdapter<String> guardIntervalAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item);
        guardIntervalAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (int i = 0; i < mGuardInterval.length; i++) {
            guardIntervalAdapter.add(mGuardInterval[i]);
        }
        mGuardIntervalSpinner.setAdapter(guardIntervalAdapter);
        mGuardIntervalSpinner
                .setOnItemSelectedListener(new OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> arg0, View arg1,
                            int arg2, long arg3) {
                        mGuardIntervalIndex = arg2 < 2 ? arg2
                                : mGuardIntervalIndex;
                    }

                    public void onNothingSelected(AdapterView<?> arg0) {
                        Log.v("@M_" + TAG, "onNothingSelected");
                    }
                });
        mSpBwAdvCbw = (Spinner) findViewById(R.id.wifi_bandwidth_cbw_spn);
        mSpBwAdvDbw = (Spinner) findViewById(R.id.wifi_bandwidth_dbw_spn);
        mSpBwAdvPrimCh = (Spinner) findViewById(R.id.wifi_bandwidth_prim_ch_spn);
        setViewEnabled(true);
        initUiComponent();
    }

    private void AddPreambleItems(int rateGroup) {
        if (rateGroup == RateInfo.EEPROM_RATE_GROUP_OFDM_MCS0_32) {
            for (int i = HIGH_RATE_PREAMBLE_BASE; i < mPreamble.length; i++) {
                mPreambleAdapter.add(mPreamble[i]);
            }
        } else if (rateGroup == RateInfo.EEPROM_RATE_GROUP_CCK) {
            for (int i = 0; i < HIGH_RATE_PREAMBLE_BASE; i++) {
                mPreambleAdapter.add(mPreamble[i]);
            }
        } else if (rateGroup == RateInfo.EEPROM_RATE_GROUP_OFDM_MEDIUM) {
            mPreambleAdapter.add(mPreamble[0]);
        } else {
            Elog.d(TAG, "AddPreambleItems; INVALID rateGroup:" + rateGroup);
        }
    }

    private void initUiComponent() {
        ArrayAdapter<String> cbwAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, BW_ADVANCED_ITEMS);
        cbwAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpBwAdvCbw.setAdapter(cbwAdapter);
        mSpBwAdvCbw.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                Elog.d(TAG, "mSpBwAdvCbw onItemSelected position:" + position + " mChannelBandwidth:" + mChannelBandwidth);
                if (mChannelBandwidth == position) {
                    return;
                }
                mChannelBandwidth = position;
                // update DBW
                mDbwAdapter.clear();
                for (int i = 0; i <= position; i++) {
                    mDbwAdapter.add(BW_ADVANCED_ITEMS[i]);
                }
                mSpBwAdvDbw.setAdapter(mDbwAdapter);

                // update PRIMARY CH
                mPrimChAdapter.clear();
                int maxPrimCh = (int) Math.pow(2, position) - 1;
                for (int i = 0; i <= maxPrimCh; i++) {
                    mPrimChAdapter.add(String.valueOf(i));
                }
                mSpBwAdvPrimCh.setAdapter(mPrimChAdapter);

                onAdvancedSelectChanged(mChannelBandwidth, mDataBandwidth, mPrimarySetting);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d("@M_" + TAG, "onNothingSelected() mSpBwAdvCbw");
            }

        });

        mDbwAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item);
        mDbwAdapter.add(BW_ADVANCED_ITEMS[0]);
        mDbwAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpBwAdvDbw.setAdapter(mDbwAdapter);
        mSpBwAdvDbw.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                if (position == mDataBandwidth) {
                    return;
                }
                mDataBandwidth = position;
                //onAdvancedSelectChanged(mChannelBandwidth, mDataBandwidth, mPrimarySetting);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d("@M_" + TAG, "onNothingSelected() mSpBwAdvDbw");
            }
        });

        mPrimChAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item);
        mPrimChAdapter.add("0");
        mPrimChAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpBwAdvPrimCh.setAdapter(mPrimChAdapter);
        mSpBwAdvPrimCh.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                if (position == mPrimarySetting) {
                    return;
                }
                mPrimarySetting = position;
                //onAdvancedSelectChanged(mChannelBandwidth, mDataBandwidth, mPrimarySetting);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d("@M_" + TAG, "onNothingSelected() mSpBwAdvPrimCh");
            }
        });
    }

    private void updateChannelByRateBandwidth(int rateIndex, int bandwidthIndex) {
        Elog.d(TAG, "updateChannelByRateBandwidth: rateIndex:" + rateIndex + " bandwidthIndex:" + bandwidthIndex);
        if (mRate.getRateGroup(rateIndex) == RateInfo.EEPROM_RATE_GROUP_CCK) {
            mChannel.remove5GChannels(mChannelAdapter);
        } else {
            if (!ChannelInfo.isAllChannelSupported()) {
                mChannel.removeChannels(new int[]{14}, mChannelAdapter);
            }
        }
        if (bandwidthIndex == BANDWIDTH_INDEX_40M) {
            mChannel.removeBw40mUnsupported2dot4GChannels(mChannelAdapter);
            mChannel.remove5GChannels(mChannelAdapter);
            if (mRate.getRateGroup(rateIndex) != RateInfo.EEPROM_RATE_GROUP_CCK) {
                mChannel.insertBw40MChannels(mChannelAdapter);
            }
        }
    }

    private int convertAdvancedRateIndex2Normal(int cbw, int selectedIndex) {
        int targetIndex = selectedIndex;
        if (cbw == BANDWIDTH_INDEX_20M) {
            targetIndex = selectedIndex;
        } else if (cbw == BANDWIDTH_INDEX_40M) {
            targetIndex = 12 + selectedIndex;
        } else if (cbw == BANDWIDTH_INDEX_80M) {
            targetIndex = 12 + selectedIndex;
        }
        Elog.d(TAG, "convertAdvancedRateIndex2Normal: cbw: " + cbw + "targetIndex:" + targetIndex);
        return targetIndex;
    }

    private void updateRateByBandwidth(ArrayAdapter<String> adapter, int bandwidth) {
        String[] rateItems = null;
        if (bandwidth == BANDWIDTH_INDEX_20M) {
            rateItems = RateInfo.ADVANCED_RATE_20M;
        } else if (bandwidth == BANDWIDTH_INDEX_40M) {
            rateItems = RateInfo.ADVANCED_RATE_40M;
        } else if (bandwidth == BANDWIDTH_INDEX_80M) {
            rateItems = RateInfo.ADVANCED_RATE_80M;
        } else {
            Elog.d(TAG, "updateRateByBandwidth:Invalid bandwith " + bandwidth);
        }
        if (rateItems == null) {
            return;
        }
        adapter.clear();
        for (int i = 0; i < rateItems.length; i++) {
            adapter.add(rateItems[i]);
        }
    }

    private void onAdvancedBandwidthSelected() {
        findViewById(R.id.wifi_bandwidth_advanced_ll).setVisibility(View.VISIBLE);
        updateChannels();
    }

    private void updateChannelByAdvancedSetting(int cbw, int dbw, int primCh) {
        Elog.d(TAG, "updateChannelByAdvancedSetting: cbw:" + cbw);
        if (cbw == BANDWIDTH_INDEX_20M) {
            mChannelAdapter.clear();
            mChannel.addSupported2dot4gChannels(mChannelAdapter, true);
            mChannel.addSupported5gChannelsByBandwidth(mChannelAdapter, ChannelInfo.BW_20M, true);
        } else if (cbw == BANDWIDTH_INDEX_40M) {
            mChannelAdapter.clear();
            mChannel.addSupported2dot4gChannels(mChannelAdapter, true);
            mChannel.removeBw40mUnsupported2dot4GChannels(mChannelAdapter);
            mChannel.addSupported5gChannelsByBandwidth(mChannelAdapter, ChannelInfo.BW_40M, true);
        } else if (cbw == BANDWIDTH_INDEX_80M) {
            mChannelAdapter.clear();
            mChannel.addSupported5gChannelsByBandwidth(mChannelAdapter, ChannelInfo.BW_80M, true);
        }
        updateRateByBandwidth(mRateAdapter, cbw);
        if (mRateUpdateCounter == 0) {
            // force to call rateSpinner's callback
            mRateSpinner.setAdapter(mRateAdapter);
        } else {
            mRateUpdateCounter = 0;
        }
    }


    private void onAdvancedSelectChanged(int cbw, int dbw, int primCh) {
        updateChannels();
    }

    //    A Row was component by {BandWith, CBW, DBW, Primary Ch}
    //    BandWith: 0:20MHz, 1:40MHz, 2:U20MHz, 3:L20MHz
    //    CBW: 0:BW20, 1:BW40, 2:BW80, 4:BW160
    //    DBW: 0:BW20, 1:BW40, 2:BW80, 4:BW160
    //    Primary Ch : 0 ~ 7
//    private static final int[][] sAdvancedBwMappingTable = {
//        {0, 0, 0, 0},
//        {3, 1, 0, 0},
//        {2, 1, 0, 1},
//    };
//    private int mappingAdvancedSetting2Bw(int cbw, int dbw, int primaryCh) {
//        int targetBw = -1;
//        for (int i = 0; i < sAdvancedBwMappingTable.length; i++) {
//            int[] mapping = sAdvancedBwMappingTable[i];
//            if (mapping[1] == cbw && mapping[2] == dbw && mapping[3] == primaryCh) {
//                targetBw = mapping[0];
//            }
//        }
//        return targetBw;
//    }


    @Override
    public void onClick(View view) {
        if (!EMWifi.sIsInitialed) {
            showDialog(DIALOG_WIFI_ERROR);
            return;
        }
        Log.d("@M_" + TAG, "view_id = " + view.getId());
        if (view.getId() == mBtnGo.getId()) {
            onClickBtnTxGo();
        } else if (view.getId() == mBtnStop.getId()) {
            onClickBtnTxStop();
        }
    }

    @Override
    protected void onDestroy() {
        if (mEventHandler != null) {
            mEventHandler.removeMessages(HANDLER_EVENT_TIMER);
            if (mTestInPorcess) {
                if (EMWifi.sIsInitialed) {
                    EMWifi.setATParam(ATPARAM_INDEX_COMMAND,
                            COMMAND_INDEX_STOPTEST);
                }
                mTestInPorcess = false;
            }
        }
        mTestThread.quit();
        super.onDestroy();
    }

    /**
     * Update Tx power
     */
    private void uiUpdateTxPower() {
        short ucGain = 0;
        long i4TxPwrGain = 0;
        // long i4OutputPower = 0;
        // long i4targetAlc = 0;
        long[] gain = new long[LENGTH_3];
        int comboChannelIndex = mChannel.getSelectedChannelId();
        // 40MHz 0x8000 | mChannel.mChannelIndex else mChannel.mChannelIndex
        comboChannelIndex |= ((mBandwidthIndex == BANDWIDTH_INDEX_40M) ? BANDWIDTH_40MHZ_MASK
                : 0);
        // may change to array[3];
        Log.w("@M_" + TAG, "channelIdx " + comboChannelIndex + " rateIdx "
                + mRate.mRateIndex + " gain " + Arrays.toString(gain) + " Len "
                + LENGTH_3);
        if (0 == EMWifi.readTxPowerFromEEPromEx(comboChannelIndex,
                mRate.mRateIndex, gain, LENGTH_3)) {
            i4TxPwrGain = gain[0];
            // i4OutputPower = gain[1];
            // i4targetAlc = gain[2];
            Log.i("@M_" + TAG, "i4TxPwrGain from uiUpdateTxPower is " + i4TxPwrGain);
            ucGain = (short) (i4TxPwrGain & BIT_8_MASK);
        }
        /*
         * if (ucGain == 0x00 || ucGain == 0xFF) { if (mRate.getUcRateGroupEep()
         * <= mRate.EEPROM_RATE_GROUP_CCK) { mTxGainEdit.setText("20"); } else {
         * mTxGainEdit.setText("22"); } } else { // long val = ucGain;
         * mTxGainEdit.setText(Long.toHexString(ucGain)); }
         */
        mEtTxGain.setText(String.format(Locale.ENGLISH,
                getString(R.string.wifi_tx_gain_format), ucGain / 2.0));
        // mTxGainEdit.setText(Long.toHexString(ucGain));
    }

    /**
     * Update channels
     */
    private void updateChannels() {
        boolean bUpdateWifiChannel = false;
        int targetBandwidth = mBandwidthIndex;
        mChannel.resetSupportedChannels(mChannelAdapter);
        if (mBandwidthIndex == BW_INDX_ADVANCED) {
            updateChannelByAdvancedSetting(mChannelBandwidth, mDataBandwidth, mPrimarySetting);
        }
        updateChannelByRateBandwidth(mRate.mRateIndex, targetBandwidth);

        bUpdateWifiChannel = true;
        if (mChannelAdapter.getCount() == 0) {
            mBtnGo.setEnabled(false);
            bUpdateWifiChannel = false;
        } else {
            mBtnGo.setEnabled(true);
        }
        if (bUpdateWifiChannel) {
            updateWifiChannel(mChannel, mChannelAdapter, mChannelSpinner);
            uiUpdateTxPower();
        }
    }


    /*
     * private void onClickBtnXtalTrimRead() { long[] val = new long[1];
     * EMWifi.getXtalTrimToCr(val); Log.d(TAG, "VAL=" + val[0]);
     * mXTEdit.setText(String.valueOf(val[0])); }
     *
     * private void onClickBtnXtaltrimWrite() { long u4XtalTrim = 0; try {
     * u4XtalTrim = Long.parseLong(mXTEdit.getText().toString()); } catch
     * (NumberFormatException e) { Toast.makeText(WiFi_Tx_6620.this,
     * "invalid input value", Toast.LENGTH_SHORT).show(); return; }
     *
     * Log.d(TAG, "u4XtalTrim =" + u4XtalTrim);
     * EMWifi.setXtalTrimToCr(u4XtalTrim); }
     */
    private void onClickBtnTxGo() {
        long u4TxGainVal = 0;
        int i = 0;
        long pktNum;
        long cntNum;
        CharSequence inputVal;
        try {
            float pwrVal = Float.parseFloat(mEtTxGain.getText().toString());
            u4TxGainVal = (long) (pwrVal * 2);
            mEtTxGain
                    .setText(String.format(Locale.ENGLISH,
                            getString(R.string.wifi_tx_gain_format),
                            u4TxGainVal / 2.0));
            // u4TxGainVal = Long.parseLong(mTxGainEdit.getText().toString(),
            // 16);
        } catch (NumberFormatException e) {
            Toast.makeText(WiFiTx6620.this, "invalid input value",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        mTxGainVal = u4TxGainVal;
        mTxGainVal = mTxGainVal > BIT_8_MASK ? BIT_8_MASK : mTxGainVal;
        mTxGainVal = mTxGainVal < 0 ? 0 : mTxGainVal;
        Log.i("@M_" + TAG, "Wifi Tx Test : " + mMode[mModeIndex]);
        mTargetModeIndex = mModeIndex;
        switch (mModeIndex) {
        case TEST_MODE_TX:
            try {
                pktNum = Long.parseLong(mEtPkt.getText().toString());
                cntNum = Long.parseLong(mEtPktCnt.getText().toString());
            } catch (NumberFormatException e) {
                Toast.makeText(WiFiTx6620.this, "invalid input value",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            mPktLenNum = pktNum;
            mCntNum = cntNum;
            break;
        case TEST_MODE_DUTY:
            // EMWifi.setOutputPower(mRate.getRateCfg(), u4TxGainVal,
            // u4Antenna);//for mt5921
            // set output power
            // setp 1:set rate
            // setp 2:set Tx gain
            // setp 3:set Antenna
            // setp 4:start output power test
            mPktLenNum = 100;
            mCntNum = 100;
            mTargetModeIndex = TEST_MODE_TX;
            break;

        case TEST_MODE_CARRIER:
            /*
             * int i4ModeType; if (mRate.getRateCfg() <=
             * mRate.EEPROM_RATE_GROUP_CCK) { i4ModeType = 0; } else {
             * i4ModeType = 1; }
             *
             * //EMWifi.setCarrierSuppression(i4ModeType, u4TxGainVal,
             * u4Antenna);//for mt5921
             */
            // setp 1:set EEPROMRate Info
            // setp 2:set Tx gain
            // setp 3:set Antenna
            // step 4:start RF Carriar Suppression Test
            break;
        case TEST_MODE_LEAKAGE:
            // EMWifi.setLocalFrequecy(u4TxGainVal, u4Antenna);//for mt5921
            // setp 1:set Tx gain
            // setp 2:set Antenna
            // step 3:start Local Frequency Test

            break;
        case TEST_MODE_POWEROFF:
            // EMWifi.setNormalMode();
            // EMWifi.setOutputPin(20, 0);
            // EMWifi.setPnpPower(4);
            break;
        default:
            break;
        }
        if (mEventHandler == null) {
            Log.w("@M_" + TAG, "eventHandler = null");
        } else {
            mEventHandler.sendEmptyMessage(HANDLER_EVENT_GO);
            // mGoBtn.setEnabled(false);
            setViewEnabled(false);
        }
    }

    private void setViewEnabled(boolean state) {
        mChannelSpinner.setEnabled(state);
        mGuardIntervalSpinner.setEnabled(state);
        mBandwidthSpinner.setEnabled(state);
        mPreambleSpinner.setEnabled(state);
        mEtPkt.setEnabled(state);
        mEtPktCnt.setEnabled(state);
        mEtTxGain.setEnabled(state);
        mRateSpinner.setEnabled(state);
        mModeSpinner.setEnabled(state);
        // mXTEdit.setEnabled(state);
        // mWriteBtn.setEnabled(state);
        // mReadBtn.setEnabled(state);
        // mALCCheck.setEnabled(state);
        mBtnGo.setEnabled(state);
        mBtnStop.setEnabled(!state);
        mSpBwAdvCbw.setEnabled(state);
        mSpBwAdvDbw.setEnabled(state);
        mSpBwAdvPrimCh.setEnabled(state);
    }

    private void onClickBtnTxStop() {
        if (mEventHandler == null) {
            Log.w("@M_" + TAG, "eventHandler = null");
        } else {
            mEventHandler.sendEmptyMessage(HANDLER_EVENT_STOP);
        }
        switch (mModeIndex) {
        case TEST_MODE_TX:
            break;
        case TEST_MODE_POWEROFF:
            EMWifi.setPnpPower(1);
            EMWifi.setTestMode();
            EMWifi.setChannel(mChannel.getSelectedFrequency());
            uiUpdateTxPower();
            // mGoBtn.setEnabled(true);
            break;
        default:
            EMWifi.setStandBy();
            // mGoBtn.setEnabled(true);
            break;
        }
    }

    class EventHandler extends Handler {

        /**
         * Constructor
         *
         * @param looper
         *            Use the provided queue instead of the default one
         */
        public EventHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (!EMWifi.sIsInitialed) {
                showDialog(DIALOG_WIFI_ERROR);
                return;
            }
            Log.d("@M_" + TAG, "new msg");
            // long i = 0;
            int rateIndex;
            long[] u4Value = new long[1];
            switch (msg.what) {
            case HANDLER_EVENT_GO:
                switch (mTargetModeIndex) {
                case TEST_MODE_TX:
                    // EMWifi.setChannel(mChannel.getChannelFreq());
                    // set Tx gain of RF
                    EMWifi.setATParam(ATPARAM_INDEX_POWER, mTxGainVal);
                    Log.i("@M_" + TAG, "The mPreambleIndex is : " + mPreambleIndex);
                    EMWifi.setATParam(ATPARAM_INDEX_PREAMBLE, mPreambleIndex);
                    // u4Antenna = 0, never be changed since first
                    // valued
                    EMWifi.setATParam(ATPARAM_INDEX_ANTENNA, ANTENNA);
                    // set package length, is there a maximum packet
                    // length? mtk80758-2010-11-2
                    EMWifi.setATParam(ATPARAM_INDEX_PACKLENGTH, mPktLenNum);
                    // set package length, is there a maximum packet
                    // length? mtk80758-2010-11-2
                    // if cntNum = 0, send continious unless stop button
                    // is pressed
                    EMWifi.setATParam(ATPARAM_INDEX_PACKCOUNT, mCntNum);
                    // package interval in unit of us, no need to allow
                    // user to set this value
                    EMWifi.setATParam(ATPARAM_INDEX_PACKINTERVAL, 20);
                    // if (mALCCheck.isChecked() == false) {
                    // i = 0;
                    // } else {
                    // i = 1;
                    // }
                    // 9, means temperature conpensation
                    EMWifi.setATParam(ATPARAM_INDEX_TEMP_COMPENSATION, 0);
                    // TX enable enable ? what does this mean
                    EMWifi.setATParam(ATPARAM_INDEX_TXOP_LIMIT,
                            TXOP_LIMIT_VALUE);
                    // set Tx content
                    for (int i = 0; i < PACKCONTENT_BUFFER.length; i++) {
                        EMWifi.setATParam(ATPARAM_INDEX_PACKCONTENT,
                                PACKCONTENT_BUFFER[i]);
                    }
                    // packet retry limit
                    EMWifi.setATParam(ATPARAM_INDEX_RETRY_LIMIT, 1);
                    // QoS queue -AC2
                    EMWifi.setATParam(ATPARAM_INDEX_QOS_QUEUE, 2);
                    Log.i("@M_" + TAG, "The mGuardIntervalIndex is : "
                            + mGuardIntervalIndex);
                    // GuardInterval setting
                    EMWifi.setATParam(ATPARAM_INDEX_GI, mGuardIntervalIndex);
                    Log.i("@M_" + TAG, "The mBandwidthIndex is : " + mBandwidthIndex);
                    // Bandwidth setting
                    if (BW_INDX_ADVANCED == mBandwidthIndex) {
                        Log.d("@M_" + TAG, "mChannelBandwidth:" + mChannelBandwidth + " mDataBandwidth:" + mDataBandwidth + " mPrimarySetting:" + mPrimarySetting);
                        EMWifi.setATParam(ATPARAM_INDEX_CHANNEL_BANDWIDTH, mChannelBandwidth);
                        EMWifi.setATParam(ATPARAM_INDEX_DATA_BANDWIDTH, mDataBandwidth);
                        EMWifi.setATParam(ATPARAM_INDEX_PRIMARY_SETTING, mPrimarySetting);
                    } else {
                        EMWifi.setATParam(ATPARAM_INDEX_BANDWIDTH, mBandwidthIndex);
                    }
                    rateIndex = mRate.mRateIndex;
                    if (mHighRateSelected) {
                        rateIndex -= MAX_LOWER_RATE_NUMBER;
                        if (rateIndex > RATE_NOT_MCS_INDEX) {
                            rateIndex = RATE_MCS_INDEX; // for MCS32
                        }
                        rateIndex |= (1 << RATE_MODE_MASK);
                    }
                    // rateIndex |= (1 << 31);
                    Log.i("@M_" + TAG, String.format("TXX rate index = 0x%08x",
                            rateIndex));
                    EMWifi.setATParam(ATPARAM_INDEX_RATE, rateIndex);
                    int number = mChannel.getSelectedFrequency();
                    EMWifi.setChannel(number);
                    Log.i("@M_" + TAG, "target channel freq ="
                            + number);
                    // start tx test
                    if (0 == EMWifi.setATParam(ATPARAM_INDEX_COMMAND,
                            COMMAND_INDEX_STARTTX)) {
                        mTestInPorcess = true;
                    }
                    sendEmptyMessageDelayed(HANDLER_EVENT_TIMER, ONE_SENCOND);
                    break;
                case TEST_MODE_DUTY:
                    // EMWifi.setOutputPower(mRate.getRateCfg(),
                    // u4TxGainVal, u4Antenna);//for mt5921
                    // set output power
                    // setp 1:set rate
                    // setp 2:set Tx gain
                    // setp 3:set Antenna
                    // setp 4:start output power test
                    if (BW_INDX_ADVANCED == mBandwidthIndex) {
                        Log.d("@M_" + TAG, "mChannelBandwidth:" + mChannelBandwidth + " mDataBandwidth:" + mDataBandwidth + " mPrimarySetting:" + mPrimarySetting);
                        EMWifi.setATParam(ATPARAM_INDEX_CHANNEL_BANDWIDTH, mChannelBandwidth);
                        EMWifi.setATParam(ATPARAM_INDEX_DATA_BANDWIDTH, mDataBandwidth);
                        EMWifi.setATParam(ATPARAM_INDEX_PRIMARY_SETTING, mPrimarySetting);
                    } else {
                        EMWifi.setATParam(ATPARAM_INDEX_BANDWIDTH, mBandwidthIndex);
                    }
                    rateIndex = mRate.mRateIndex;
                    if (mHighRateSelected) {
                        rateIndex -= MAX_LOWER_RATE_NUMBER;
                        if (rateIndex > RATE_NOT_MCS_INDEX) {
                            rateIndex = RATE_MCS_INDEX; // for MCS32
                        }
                        rateIndex |= (1 << RATE_MODE_MASK);
                    }
                    Log.i("@M_" + TAG, String.format("Tx rate index = 0x%08x",
                            rateIndex));
                    EMWifi.setATParam(ATPARAM_INDEX_RATE, rateIndex);
                    EMWifi.setATParam(ATPARAM_INDEX_POWER, mTxGainVal);
                    Log.i("@M_" + TAG, "The mPreambleIndex is : " + mPreambleIndex);
                    EMWifi.setATParam(ATPARAM_INDEX_PREAMBLE, mPreambleIndex);
                    EMWifi.setATParam(ATPARAM_INDEX_ANTENNA, ANTENNA);
                    // start output power test
                    if (0 == EMWifi.setATParam(ATPARAM_INDEX_COMMAND,
                            COMMAND_INDEX_OUTPUTPOWER)) {
                        mTestInPorcess = true;
                    }
                    break;
                case TEST_MODE_CARRIER:
                    // setp 1:set EEPROMRate Info
                    // setp 2:set Tx gain
                    // setp 3:set Antenna
                    // step 4:start RF Carriar Suppression Test
                    EMWifi.setATParam(ATPARAM_INDEX_POWER, mTxGainVal);
                    EMWifi.setATParam(ATPARAM_INDEX_ANTENNA, ANTENNA);
                    // start carriar suppression test
                    if (ChipSupport.getChip() == ChipSupport.MTK_6573_SUPPORT) {
                        if (0 == EMWifi.setATParam(ATPARAM_INDEX_COMMAND,
                                COMMAND_INDEX_CARRIER)) {
                            mTestInPorcess = true;
                        }
                    } else {
                        if (mCCKRateSelected) {
                            EMWifi.setATParam(ATPARAM_INDEX_CWMODE,
                                    CWMODE_CCKPI);
                        } else {
                            EMWifi.setATParam(ATPARAM_INDEX_CWMODE,
                                    CWMODE_OFDMLTF);
                        }
                        if (0 == EMWifi.setATParam(ATPARAM_INDEX_COMMAND,
                                COMMAND_INDEX_CARRIER_NEW)) {
                            mTestInPorcess = true;
                        }
                    }
                    break;
                case TEST_MODE_LEAKAGE:
                    // Wifi.setLocalFrequecy(u4TxGainVal, u4Antenna);
                    // setp 1:set Tx gain
                    // setp 2:set Antenna
                    // step 3:start Local Frequency Test
                    EMWifi.setATParam(ATPARAM_INDEX_POWER, mTxGainVal);
                    EMWifi.setATParam(ATPARAM_INDEX_ANTENNA, ANTENNA);
                    // start carriar suppression test
                    if (0 == EMWifi.setATParam(ATPARAM_INDEX_COMMAND,
                            COMMAND_INDEX_LOCALFREQ)) {
                        mTestInPorcess = true;
                    }
                    break;
                case TEST_MODE_POWEROFF:
                    // Wifi.setNormalMode();
                    // Wifi.setOutputPin(20, 0);
                    // Wifi.setPnpPower(4);
                    break;
                default:
                    break;
                }
                break;
            case HANDLER_EVENT_STOP:
                Log.i("@M_" + TAG, "The Handle event is : HANDLER_EVENT_STOP");
                if (mTestInPorcess) {
                    u4Value[0] = EMWifi.setATParam(ATPARAM_INDEX_COMMAND,
                            COMMAND_INDEX_STOPTEST);
                    mTestInPorcess = false;
                }
                // driver does not support query operation on
                // functionIndex = 1 , we can only judge whether this
                // operation is processed successfully through the
                // return value
                if (mEventHandler != null) {
                    mEventHandler.removeMessages(HANDLER_EVENT_TIMER);
                }
                mHandler.sendEmptyMessage(HANDLER_EVENT_FINISH);
                break;
            case HANDLER_EVENT_TIMER:
                u4Value[0] = 0;
                long pktCnt = 0;
                Log.i("@M_" + TAG, "The Handle event is : HANDLER_EVENT_TIMER");
                if (mModeIndex == TEST_MODE_DUTY) {
                    pktCnt = 100;
                    boolean completed = false;
                    if (0 == EMWifi
                            .getATParam(ATPARAM_INDEX_TRANSMITCOUNT, u4Value)) {
                        Log.d("@M_" + TAG,
                                "query Transmitted packet count succeed, count = "
                                        + u4Value[0] + " target count = " + pktCnt);
                        if (u4Value[0] == pktCnt) {
                            completed = true;
                        }
                    } else {
                        Log.w("@M_" + TAG, "query Transmitted packet count failed");
                    }
                    if (!completed) {
                        u4Value[0] = EMWifi.setATParam(ATPARAM_INDEX_COMMAND,
                                COMMAND_INDEX_STOPTEST);

                    }
                    mTargetModeIndex = TEST_MODE_DUTY;
                    sendEmptyMessage(HANDLER_EVENT_GO);
                    return;
                }
                try {
                    pktCnt = Long.parseLong(mEtPktCnt.getText().toString());
                } catch (NumberFormatException e) {
                    Toast.makeText(WiFiTx6620.this, "invalid input value",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                // here we need to judge whether target number packet is
                // finished sent or not
                if (0 == EMWifi
                        .getATParam(ATPARAM_INDEX_TRANSMITCOUNT, u4Value)) {
                    Log.d("@M_" + TAG,
                            "query Transmitted packet count succeed, count = "
                                    + u4Value[0] + " target count = " + pktCnt);
                    if (u4Value[0] == pktCnt) {
                        removeMessages(HANDLER_EVENT_TIMER);
                        mHandler.sendEmptyMessage(HANDLER_EVENT_FINISH);
                        break;
                    }
                } else {
                    Log.w("@M_" + TAG, "query Transmitted packet count failed");
                }
                sendEmptyMessageDelayed(HANDLER_EVENT_TIMER, ONE_SENCOND);
                break;
            default:
                break;
            }
        }
    }
}
