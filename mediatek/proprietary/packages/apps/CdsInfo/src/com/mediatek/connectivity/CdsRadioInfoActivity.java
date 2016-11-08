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
 * Copyright (C) 2006 The Android Open Source Project
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
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.TrafficStats;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.CellInfo;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.NeighboringCellInfo;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyManager.MultiSimVariants;
import android.telephony.VoLteServiceState;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;

import com.android.ims.ImsException;
import com.android.ims.ImsManager;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.util.HexDump;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;


public class CdsRadioInfoActivity extends Activity {
    private static final String TAG = "CDSINFO/Radio";

    private static final int EVENT_PHONE_STATE_CHANGED = 100;
    private static final int EVENT_SIGNAL_STRENGTH_CHANGED = 200;
    private static final int EVENT_SERVICE_STATE_CHANGED = 300;

    private static final int EVENT_QUERY_NEIGHBORING_CIDS_DONE = 1001;
    private static final int EVENT_QUERY_DATACALL_LIST_DONE    = 1002;
    private static final int EVENT_AT_CMD_DONE                 = 1003;

    private static final String INFO_TITLE = "Info.";
    private static final String PHONEID = "phoneId";
    private static final String NA = "N/A";
    private static final String UNKNOWN = "unknown";
    private static final String IMEI_WARNING_MSG =
            "[Failed] Please use AT+EGMR=1,7,\"IMEI\" in Phone 2 for Modem 2 of DSDA project\r\n";

    private static final String[] CMDLINES = new String[] {
        "AT+EGMR=1,7,\"\"",
        "AT+EGMR=1,10,\"\"",
        "AT+EGMR=1,11,\"\"",
        "AT+EGMR=1,12,\"\"",
        "AT+CGEQREQ=1,2,128,128",
        "AT+CGEQREQ=2,2,128,128"
    };

    private TextView mDeviceId; //DeviceId is the IMEI in GSM and the MEID in CDMA
    private TextView mImsi;
    private TextView mImpi;
    private TextView mImpu;
    private TextView mImsDomain;
    private TextView mRadioState;
    private TextView mSimState;
    private TextView mNumber;
    private TextView mCallState;
    private TextView mOperatorName;
    private TextView mRoamingState;
    private TextView mGsmState;
    private TextView mImsState;
    private TextView mImsExtra;
    private TextView mGprsAttachState;
    private TextView mGprsState;
    private TextView mNetwork;
    private TextView mDbm;
    private TextView mServiceState;
    private TextView mLocation;
    private TextView mNeighboringCids;
    private TextView mResets;
    private TextView mAttempts;
    private TextView mSuccesses;
    private TextView mDisconnects;
    private TextView mSentSinceReceived;
    private TextView mSent;
    private TextView mReceived;
    private TextView mSystemProperties;
    private Button mAtBtnCmd;


    private ArrayAdapter<String> mAutoCompleteAdapter;
    private AutoCompleteTextView mCmdLineList;

    private TelephonyManager mTelephonyManager;
    private ImsManager  mImsManager;
    private Phone mGsmPhone = null;
    private CdsPhoneStateListener mPhoneStateListener;

    private Context mContext;
    private int mPhoneId = 0;
    private int mSubId = SubscriptionManager.DEFAULT_SUBSCRIPTION_ID;
    private boolean mUserMode = false;
    private MultiSimVariants mMultiSimVariants = MultiSimVariants.UNKNOWN;

    private boolean mIsSecurity = SystemProperties.getBoolean("ro.mtk_a1_feature", false);

    private static final String[] RADIO_SYSTEM_PROPERTY = new String[] {
        "gsm.sim.state",
        "gsm.sim.ril.testsim",
        "gsm.sim.ril.testsim.2",
        "gsm.sim.ril.testsim.3",
        "gsm.sim.ril.testsim.4",
        "gsm.operator.alpha",
        "gsm.sim.operator.default-name",
        "gsm.sim.operator.iso-country",
        "gsm.sim.operator.alpha",
        "gsm.sim.operator.numeric",
        "gsm.mNetwork.type",
        "gsm.cs.mNetwork.type",
        "gsm.ril.uicctype",
        "gsm.baseband.capability",
        "gsm.gcf.testmode",
        "gsm.sim.ril.phbready",
        "gsm.sim.retry.pin1",
        "gsm.sim.retry.pin2",
        "gsm.sim.retry.puk1",
        "gsm.sim.retry.puk2",
        "gsm.operator.alpha.2",
        "gsm.sim.operator.default-name.2",
        "gsm.sim.operator.iso-country.2",
        "gsm.sim.operator.alpha.2",
        "gsm.sim.operator.numeric.2",
        "gsm.mNetwork.type.2",
        "gsm.cs.mNetwork.type.2",
        "gsm.ril.uicctype.2",
        "gsm.baseband.capability2",
        "gsm.gcf.testmode2",
        "gsm.sim.retry.pin1.2",
        "gsm.sim.retry.pin2.2",
        "gsm.sim.retry.puk1.2",
        "gsm.sim.retry.puk2.2",
        "gsm.sim.ril.phbready.2",
        "gsm.3gswitch",
        "gsm.current.phone-type",
        "gsm.defaultpdpcontext.active",
        "gsm.nitz.time",
        "gsm.phone.created",
        "gsm.sim.inserted",
        "gsm.siminfo.ready",
        "gsm.version.baseband",
        "init.svc.ril-daemon",
        "init.svc.gsm0710muxd",
        "persist.gemini.sim_num",
        "persist.radio.default_sim",
        "persist.radio.default_sim_mode",
        "persist.radio.fd.counter",
        "persist.radio.fd.off.counter",
        "persist.radio.gprs.attach.type",
        "ro.mediatek.gemini_support",
    };


    class CdsPhoneStateListener extends PhoneStateListener {

        public CdsPhoneStateListener(int subId) {
            super(subId);
        }

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            updateServiceState();
            updatePowerState();
        }

        @Override
        public void onDataConnectionStateChanged(int state, int networkType) {
            updateDataState();
            updateDataStats();
            updateNetworkType();
        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            updateSignalStrength();
        }

        @Override
        public void onDataActivity(int direction) {
            updateDataStats2();
        }

        @Override
        public void onCellLocationChanged(CellLocation location) {
            if (location == null) return;

            Log.i(TAG, "sim id:" + mPhoneId + " " + location.toString());
            updateLocation(location);
        }

        @Override
        public void onMessageWaitingIndicatorChanged(boolean mwi) {

        }

        @Override
        public void onCallForwardingIndicatorChanged(boolean cfi) {

        }

        @Override
        public void onVoLteServiceStateChanged(VoLteServiceState stateInfo) {

        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            updatePhoneState();
        }

    };


    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            AsyncResult ar;

            switch (msg.what) {

            case EVENT_QUERY_NEIGHBORING_CIDS_DONE:
                updateNeighboringCids();
                break;

            case EVENT_AT_CMD_DONE:
                ar = (AsyncResult) msg.obj;
                handleAtCmdResponse(ar);
                break;
            default:
                break;

            }
        }
    };

    private int getSubId(int slotId) {
        SubscriptionInfo result = SubscriptionManager.from(mContext).getActiveSubscriptionInfoForSimSlotIndex(slotId);

        if (result != null) {
            Log.i(TAG, "SubscriptionInfo:" + result.getSubscriptionId());
            return result.getSubscriptionId();
        }

        return SubscriptionManager.DEFAULT_SUBSCRIPTION_ID;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Intent intent = getIntent();
        mPhoneId = intent.getIntExtra(PHONEID, 0);
        mContext = this.getBaseContext();
        mSubId = getSubId(mPhoneId);
        Log.i(TAG, "The Phone/Slot ID is " + mPhoneId);

        setContentView(R.layout.radio_info);

        mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        int phoneId = 0;

        if (mPhoneId < 0 || mPhoneId >= TelephonyManager.getDefault().getPhoneCount()) {
            mPhoneId = 0;
        }

        mGsmPhone = PhoneFactory.getPhone(mPhoneId);

        mAutoCompleteAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, CMDLINES);

        mCmdLineList = (AutoCompleteTextView) findViewById(R.id.AtComLine);
        mCmdLineList.setThreshold(3);
        mCmdLineList.setAdapter(mAutoCompleteAdapter);
        mCmdLineList.setText("AT+");

        mDeviceId         = (TextView) findViewById(R.id.imei);
        mImsi             = (TextView) findViewById(R.id.imsi);
        mImpi             = (TextView) findViewById(R.id.impi);
        mImpu             = (TextView) findViewById(R.id.impu);
        mImsDomain        = (TextView) findViewById(R.id.imsDomain);
        mRadioState        = (TextView) findViewById(R.id.radioState);
        mSimState          = (TextView) findViewById(R.id.simState);
        mNumber            = (TextView) findViewById(R.id.number);
        mCallState         = (TextView) findViewById(R.id.call);
        mOperatorName      = (TextView) findViewById(R.id.operator);
        mRoamingState      = (TextView) findViewById(R.id.roaming);
        mGsmState          = (TextView) findViewById(R.id.gsm);
        mImsState          = (TextView) findViewById(R.id.ims_service_state);
        mImsExtra          = (TextView) findViewById(R.id.ims_service_extra);
        mGprsAttachState   = (TextView) findViewById(R.id.gprs_attach);
        mGprsState         = (TextView) findViewById(R.id.gprs);
        mNetwork           = (TextView) findViewById(R.id.network);
        mServiceState     = (TextView) findViewById(R.id.service_state);
        mDbm               = (TextView) findViewById(R.id.dbm);


        mLocation         = (TextView) findViewById(R.id.location);
        mNeighboringCids  = (TextView) findViewById(R.id.neighboring);

        mResets            = (TextView) findViewById(R.id.resets);
        mAttempts          = (TextView) findViewById(R.id.attempts);
        mSuccesses         = (TextView) findViewById(R.id.successes);
        mDisconnects       = (TextView) findViewById(R.id.disconnects);

        mResets.setVisibility(View.GONE);
        mAttempts.setVisibility(View.GONE);
        mSuccesses.setVisibility(View.GONE);
        mDisconnects.setVisibility(View.GONE);

        mSentSinceReceived = (TextView) findViewById(R.id.sentSinceReceived);
        mSent              = (TextView) findViewById(R.id.sent);
        mReceived          = (TextView) findViewById(R.id.received);

        mHandler.obtainMessage(EVENT_QUERY_NEIGHBORING_CIDS_DONE);

        mAtBtnCmd = (Button) findViewById(R.id.Submit);
        mAtBtnCmd.setOnClickListener(mAtButtonHandler);

        if (mIsSecurity) {
            mCmdLineList.setVisibility(View.GONE);
            mAtBtnCmd.setVisibility(View.GONE);
        }


        mDeviceId.requestFocus();

        mSystemProperties = (TextView) findViewById(R.id.system_property);

        mUserMode = SystemProperties.get("ro.build.type").equals("user");

        if (mSubId != SubscriptionManager.DEFAULT_SUBSCRIPTION_ID) {
            mPhoneStateListener = new CdsPhoneStateListener(mSubId);
        }

        mImsManager = ImsManager.getInstance(mContext, mSubId);
    }

    @Override
    protected void onResume() {
        super.onResume();

        updatePhoneState();
        updatePowerState();
        updateSignalStrength();
        updateServiceState();
        updateDataState();
        updateDataStats();
        updateDataStats2();
        updateProperties();

        Log.i(TAG, "[RadioInfo] onResume: register phone & data intents");

        mTelephonyManager.listen((PhoneStateListener) mPhoneStateListener,
                                 PhoneStateListener.LISTEN_SERVICE_STATE
                                 | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                                 | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                                 | PhoneStateListener.LISTEN_DATA_ACTIVITY
                                 | PhoneStateListener.LISTEN_CALL_STATE
                                 | PhoneStateListener.LISTEN_CELL_LOCATION
                                 | PhoneStateListener.LISTEN_VOLTE_STATE
                                );
        updateSystemProperties();
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.i(TAG, "[RadioInfo] onPause: unregister phone & data intents");

        mTelephonyManager.listen((PhoneStateListener) mPhoneStateListener,
                                 PhoneStateListener.LISTEN_NONE
                                );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }


    private boolean isRadioOn() {
        try {
            return mGsmPhone.getServiceState().getState() != ServiceState.STATE_POWER_OFF;
        } catch (NullPointerException ee) {
            ee.printStackTrace();
            return false;
        }
    }

    private void updatePowerState() {
        String buttonText = isRadioOn() ?
                            getString(R.string.radioInfo_service_on) :
                            getString(R.string.radioInfo_service_off);
        mRadioState.setText(buttonText);
    }


    private final void
    updateSignalStrength() {
        // TODO PhoneStateIntentReceiver is deprecated and PhoneStateListener
        // should probably used instead.

        try {
            int state = mGsmPhone.getServiceState().getState();
            Resources r = getResources();

            if ((ServiceState.STATE_OUT_OF_SERVICE == state) ||
                    (ServiceState.STATE_POWER_OFF == state)) {
                mDbm.setText("0");
            }

            int signalDbm = mGsmPhone.getSignalStrength().getDbm();

            if (-1 == signalDbm) signalDbm = 0;

            int signalAsu = mGsmPhone.getSignalStrength().getAsuLevel();

            if (-1 == signalAsu) signalAsu = 0;

            mDbm.setText(String.valueOf(signalDbm) + " "
                         + r.getString(R.string.radioInfo_display_dbm) + "   "
                         + String.valueOf(signalAsu) + " "
                         + r.getString(R.string.radioInfo_display_asu));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }



    private final void
    updateServiceState() {
        ServiceState serviceState = mGsmPhone.getServiceState();

        if (serviceState == null) return;

        int state = serviceState.getState();
        Resources r = getResources();
        String display = r.getString(R.string.radioInfo_unknown);

        switch (state) {
        case ServiceState.STATE_IN_SERVICE:
            display = r.getString(R.string.radioInfo_service_in);
            break;
        case ServiceState.STATE_OUT_OF_SERVICE:
            display = r.getString(R.string.radioInfo_service_out);
            break;
        case ServiceState.STATE_EMERGENCY_ONLY:
            display = r.getString(R.string.radioInfo_service_emergency);
            break;
        case ServiceState.STATE_POWER_OFF:
            display = r.getString(R.string.radioInfo_service_off);
            break;
        default:
            break;
        }

        mGsmState.setText(display);

        try {
            if (mImsManager.getImsRegInfo()) {
                mImsState.setText("REGISTERED");
            } else {
                mImsState.setText("UNREGISTERED");
            }
        } catch (ImsException e) {
            e.printStackTrace();
        }

        try {
            mImsExtra.setText(mImsManager.getImsExtInfo());
        } catch (ImsException e) {
            e.printStackTrace();
        }

        if (serviceState.getRoaming()) {
            mRoamingState.setText(R.string.radioInfo_roaming_in);
        } else {
            mRoamingState.setText(R.string.radioInfo_roaming_not);
        }

        mOperatorName.setText(serviceState.getOperatorAlphaLong());
        mServiceState.setText(serviceState.toString());

        state = serviceState.getDataRegState();

        display = r.getString(R.string.radioInfo_unknown);

        switch (state) {
        case ServiceState.STATE_IN_SERVICE:
            display = r.getString(R.string.radioInfo_service_in);
            break;
        case ServiceState.STATE_OUT_OF_SERVICE:
            display = r.getString(R.string.radioInfo_service_out);
            break;
        case ServiceState.STATE_EMERGENCY_ONLY:
            display = r.getString(R.string.radioInfo_service_emergency);
            break;
        case ServiceState.STATE_POWER_OFF:
            display = r.getString(R.string.radioInfo_service_off);
            break;
        default:
            break;
        }

        mGprsAttachState.setText(display);
        mGprsAttachState.setVisibility(View.GONE);
    }


    private final void
    updatePhoneState() {
        PhoneConstants.State state = mGsmPhone.getState();
        Resources r = getResources();
        String display = r.getString(R.string.radioInfo_unknown);

        switch (state) {
        case IDLE:
            display = r.getString(R.string.radioInfo_phone_idle);
            break;
        case RINGING:
            display = r.getString(R.string.radioInfo_phone_ringing);
            break;
        case OFFHOOK:
            display = r.getString(R.string.radioInfo_phone_offhook);
            break;
        default:
            break;
        }

        mCallState.setText(display);
    }

    private final void updateDataState() {
        int state = mTelephonyManager.getDataState();
        Resources r = getResources();
        String display = r.getString(R.string.radioInfo_unknown);

        switch (state) {
        case TelephonyManager.DATA_CONNECTED:
            display = r.getString(R.string.radioInfo_data_connected);
            break;
        case TelephonyManager.DATA_CONNECTING:
            display = r.getString(R.string.radioInfo_data_connecting);
            break;
        case TelephonyManager.DATA_DISCONNECTED:
            display = r.getString(R.string.radioInfo_data_disconnected);
            break;
        case TelephonyManager.DATA_SUSPENDED:
            display = r.getString(R.string.radioInfo_data_suspended);
            break;
        }

        mGprsState.setText(display);
    }

    private final void updateNetworkType() {
        Resources r = getResources();
        String display = r.getString(R.string.radioInfo_unknown);
        if (mGsmPhone.getServiceState() != null) {
            int nwType = mGsmPhone.getServiceState().getDataNetworkType();
            display = TelephonyManager.getNetworkTypeName(nwType);
        }
        mNetwork.setText(display);
    }

    private final void
    updateProperties() {
        String s;
        Resources r = getResources();

        Log.i(TAG, "updateProperties:" + mPhoneId + ":" + mSubId);

        s = mTelephonyManager.getDeviceId(mPhoneId);

        if (s == null) s = r.getString(R.string.radioInfo_unknown);

        mDeviceId.setText(s);


        if (mSubId == SubscriptionManager.DEFAULT_SUBSCRIPTION_ID) {
            mNumber.setText(r.getString(R.string.radioInfo_unknown));
            mImsi.setText(r.getString(R.string.radioInfo_unknown));
        } else {
            s = mTelephonyManager.getLine1NumberForSubscriber(mSubId);
            if (s == null) {
                s = r.getString(R.string.radioInfo_unknown);
            }

            mNumber.setText(s);
            s = mTelephonyManager.getSubscriberId(mSubId);

            if (s == null) {
                s = r.getString(R.string.radioInfo_unknown);
            }

            mImsi.setText(s);
        }


        try {
            int state = mTelephonyManager.getSimState(mPhoneId);

            switch(state) {
                case TelephonyManager.SIM_STATE_ABSENT:
                    s = "ABSENT";
                    break;
                case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                    s = "PIN_REQUIRED";
                    break;
                case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                    s = "PUK_REQUIRED";
                    break;
                case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                    s = "NETWORK_LOCKED";
                    break;
                case TelephonyManager.SIM_STATE_READY:
                    s = "READY";
                    break;
                case TelephonyManager.SIM_STATE_CARD_IO_ERROR:
                    s = "CARD_IO_ERROR";
                    break;
                default:
                    s = r.getString(R.string.radioInfo_unknown);
                    break;
            }
            mSimState.setText(s);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }


        try {
            s = mGsmPhone.getIsimRecords().getIsimImpi();

            if (s == null) s = r.getString(R.string.radioInfo_unknown);

            mImpi.setText(s);

            s = mGsmPhone.getIsimRecords().getIsimDomain();

            if (s == null) s = r.getString(R.string.radioInfo_unknown);

            mImsDomain.setText(s);

            String impu[] = mGsmPhone.getIsimRecords().getIsimImpu();

            if (impu == null) {
                s = r.getString(R.string.radioInfo_unknown);
            } else {
                s = "";

                for (int i = 0; i < impu.length; i++) {
                    if (impu[i].length() != 0) {
                        s += "\r\n[" + impu[i] + "]";
                    }
                }
            }

            mImpu.setText(s);
        } catch (NullPointerException ee) {
            ee.printStackTrace();
        }
    }

    private final void updateLocation(CellLocation location) {
        Resources r = getResources();

        Log.i(TAG, "GsmCellLocation:" + location.toString());

        if (location instanceof GsmCellLocation) {
            GsmCellLocation loc = (GsmCellLocation) location;
            int lac = loc.getLac();
            int cid = loc.getCid();

            mLocation.setText(r.getString(R.string.radioInfo_lac) + " = "
                              + ((lac == -1) ? "unknown" : lac + "[0x" + Integer.toHexString(lac) + "]")
                              + "\n"
                              + r.getString(R.string.radioInfo_cid) + " = "
                              + ((cid == -1) ? "unknown" : cid + "[0x" + Integer.toHexString(cid) + "]"));
        } else {
            mLocation.setText("unknown");
        }

    }

    private final void updateNeighboringCids() {
        StringBuilder sb = new StringBuilder();
        List<CellInfo> cids = mTelephonyManager.getAllCellInfo();

        if (cids != null) {
            if (cids.size() == 0) {
                sb.append("no neighboring cells");
            } else {
                for (CellInfo cell : cids) {
                    sb.append(cell.toString()).append(" ");
                }
            }
        } else {
            sb.append("unknown");
        }

        mNeighboringCids.setText(sb.toString());
    }


    private final void updateDataStats() {
        String s;

        s = SystemProperties.get("net.gsm.radio-reset", "0");
        mResets.setText(s);

        s = SystemProperties.get("net.gsm.attempt-gprs", "0");
        mAttempts.setText(s);

        s = SystemProperties.get("net.gsm.succeed-gprs", "0");
        mSuccesses.setText(s);

        s = SystemProperties.get("net.gsm.disconnect", "0");
        mDisconnects.setText(s);

        s = SystemProperties.get("net.ppp.reset-by-timeout", "0");
        mSentSinceReceived.setText(s);
    }

    private final void updateDataStats2() {
        Resources r = getResources();

        long txPackets = TrafficStats.getMobileTxPackets();
        long rxPackets = TrafficStats.getMobileRxPackets();
        long txBytes   = TrafficStats.getMobileTxBytes();
        long rxBytes   = TrafficStats.getMobileRxBytes();

        String packets = r.getString(R.string.radioInfo_display_packets);
        String bytes   = r.getString(R.string.radioInfo_display_bytes);

        mSent.setText(txPackets + " " + packets + ", " + txBytes + " " + bytes);
        mReceived.setText(rxPackets + " " + packets + ", " + rxBytes + " " + bytes);
    }

    OnClickListener mAtButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            String atCmdLine = "";

            atCmdLine = mCmdLineList.getText().toString();

            Log.v(TAG , "Execute AT command:" + atCmdLine + ":" + mUserMode);

            if (mUserMode && (atCmdLine.toUpperCase()).startsWith("AT+EGMR=1")) {
                showInfo("This command is not allowed in UserBuild");
                return;
            } else if (mIsSecurity && (atCmdLine.toUpperCase()).startsWith("AT+EGMR=1")) {
                showInfo("This command is not allowed in A1 project");
                return;
            } else if (mMultiSimVariants == MultiSimVariants.DSDA &&
                    (atCmdLine.toUpperCase()).startsWith("AT+EGMR=1,10")) {
                showInfo("IMEI_WARNING_MSG");
                return;
            }

            try {
                byte[] rawData = atCmdLine.getBytes();
                byte[] cmdByte = new byte[rawData.length + 1];
                System.arraycopy(rawData, 0, cmdByte, 0, rawData.length);
                cmdByte[cmdByte.length - 1] = 0;
                mGsmPhone.invokeOemRilRequestRaw(cmdByte,
                                                 mHandler.obtainMessage(EVENT_AT_CMD_DONE));
            } catch (NullPointerException ee) {
                ee.printStackTrace();
            }

        }
    };

    void handleAtCmdResponse(AsyncResult ar) {
        if (ar.exception != null) {
            Log.i(TAG, "The response of command is failed");
            showInfo("AT command is failed to send");
        } else {
            try {
                byte[] rawData = (byte[]) ar.result;
                Log.i(TAG, "HexDump:" + HexDump.dumpHexString(rawData));
                String txt = new String(rawData, "UTF-8");
                Log.i(TAG, "The resopnse is " + txt);
                showInfo("AT command is sent:" + txt);
            } catch (NullPointerException e) {
                showInfo("Something is wrong");
                e.printStackTrace();
            } catch (UnsupportedEncodingException ee) {
                ee.printStackTrace();
            }
        }
    }

    private void updateSystemProperties() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < RADIO_SYSTEM_PROPERTY.length; i++) {
            sb.append("[" + RADIO_SYSTEM_PROPERTY[i] + "]: [" + SystemProperties.get(RADIO_SYSTEM_PROPERTY[i], "") + "]\r\n");
        }

        mSystemProperties.setText(sb);
    }

    private void showInfo(String info) {
        if (isFinishing()) return;
        AlertDialog.Builder infoDialog = new AlertDialog.Builder(this);
        infoDialog.setTitle(INFO_TITLE);
        infoDialog.setMessage(info);
        infoDialog.setIcon(android.R.drawable.ic_dialog_alert);
        infoDialog.show();
    }
}
