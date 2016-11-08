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
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
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

package com.mediatek.engineermode.networkinfo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.mediatek.engineermode.R;

import java.util.HashMap;
import java.util.List;

/**
 * Security Status activity.
 */
public class SecurityStatus extends Activity {
    private static final String TAG = "NetworkInfo";
    private static final int MSG_NW_INFO = 1;
    private static final int MSG_NW_INFO_URC = 2;
    private static final int MSG_NW_INFO_OPEN = 3;
    private static final int MSG_NW_INFO_CLOSE = 4;
    private static final int FLAG = 0x08;
    private static final int FLAG_OFFSET = 8;

    private TextView m2gCipher;
    private TextView m2gGprs;
    private TextView m3gCipher;
    private TextView m3gIntegrity;
    private TextView m4gEnasCipher;
    private TextView m4gEnasIntegrity;
    private TextView m4gErrcCipher;
    private TextView m4gErrcIntegrity;

    private int mItemCount = 6;
    private int[] mItem = {
            Content.CHANNEL_INDEX,
            Content.LLC_EM_INFO_INDEX,
            Content.SECURITY_CONFIGURATION_INDEX,
            Content.ERRC_EM_SEC_PARAM_INDEX,
            Content.EMM_L4C_EMM_INFO_INDEX,
            Content.ERRC_EM_ERRC_STATE_INDEX};
    private NetworkInfoUrcParser mUrcParser;
    private Phone mPhone = null;
    private int mFlag = 0;
    private HashMap<Integer, String> mNetworkInfo = new HashMap<Integer, String>();
    private MyPhoneStateListener mPhoneStateListener;
    private TelephonyManager mTelephonyManager;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            boolean flightMode = intent.getBooleanExtra("state", false);
            Log.v("@M_" + TAG, action + flightMode);
            if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action) && !flightMode) {
                mNetworkInfo.clear();
                registerNetwork();
                updateUI();
            }
        }
    };

    private Handler mATCmdHander = new Handler() {
        public void handleMessage(Message msg) {
            AsyncResult ar;
            switch (msg.what) {
            case MSG_NW_INFO:
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    String[] data = (String[]) ar.result;
                    Log.v("@M_" + TAG, "data[0] is : " + data[0]);
                    Log.v("@M_" + TAG, "flag is : " + data[0].substring(FLAG_OFFSET));
                    mFlag = Integer.valueOf(data[0].substring(FLAG_OFFSET));
                    mFlag = mFlag | FLAG;
                    Log.v("@M_" + TAG, "flag change is : " + mFlag);
                    for (int j = 0; j < mItemCount; j++) {
                        String[] atCommand = new String[2];
                        atCommand[0] = "AT+EINFO=" + mFlag + "," + mItem[j] + ",0";
                        atCommand[1] = "+EINFO";
                        sendATCommand(atCommand, MSG_NW_INFO_OPEN);
                    }
                } else {
                    Log.v("@M_" + TAG, ar.exception.getMessage());
                    mTelephonyManager.listen(mPhoneStateListener,
                             PhoneStateListener.LISTEN_SERVICE_STATE);
                    updateServiceState();
                }
                // fall through
            case MSG_NW_INFO_OPEN:
            case MSG_NW_INFO_CLOSE:
                ar = (AsyncResult) msg.obj;
                if (ar.exception != null) {
                    Toast.makeText(SecurityStatus.this, getString(R.string.send_at_fail),
                            Toast.LENGTH_SHORT);
                }
                break;
            default:
                break;
            }
        }
    };

    private final Handler mUrcHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == MSG_NW_INFO_URC) {
                AsyncResult ar = (AsyncResult) msg.obj;
                String[] data = (String[]) ar.result;
                Log.v("@M_" + TAG, "Receive URC: " + data[0] + ", " + data[1]);

                int type = -1;
                try {
                    type = Integer.parseInt(data[0]);
                } catch (NumberFormatException e) {
                    Toast.makeText(SecurityStatus.this,
                            "Return type error", Toast.LENGTH_SHORT).show();
                    return;
                }

                mNetworkInfo.put(type, data[1]);
                updateUI();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.security_status);
        m2gCipher = (TextView) findViewById(R.id.security_status_2g_cipher);
        m2gGprs = (TextView) findViewById(R.id.security_status_2g_gprs);
        m3gCipher = (TextView) findViewById(R.id.security_status_3g_cipher);
        m3gIntegrity = (TextView) findViewById(R.id.security_status_3g_integrity);
        m4gEnasCipher = (TextView) findViewById(R.id.security_status_4g_enas_cipher);
        m4gEnasIntegrity = (TextView) findViewById(R.id.security_status_4g_enas_integrity);
        m4gErrcCipher = (TextView) findViewById(R.id.security_status_4g_errc_cipher);
        m4gErrcIntegrity = (TextView) findViewById(R.id.security_status_4g_errc_integrity);
        mUrcParser = new NetworkInfoUrcParser(this);
        mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        mPhoneStateListener = new MyPhoneStateListener(getSubId(0));
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNetworkInfo.clear();
        registerNetwork();
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        registerReceiver(mReceiver, intentFilter);
        updateUI();
    }

    @Override
    public void onPause() {
        unregisterNetwork();
        unregisterReceiver(mReceiver);
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void updateUI() {
        String[] info = mUrcParser.parseSecurityStatus(Content.CHANNEL_INDEX,
                mNetworkInfo.get(Content.CHANNEL_INDEX));
        m2gCipher.setText(info[0]);

        info = mUrcParser.parseSecurityStatus(Content.LLC_EM_INFO_INDEX,
                mNetworkInfo.get(Content.LLC_EM_INFO_INDEX));
        m2gGprs.setText(info[0]);

        info = mUrcParser.parseSecurityStatus(Content.SECURITY_CONFIGURATION_INDEX,
                mNetworkInfo.get(Content.SECURITY_CONFIGURATION_INDEX));
        m3gCipher.setText(info[0]);
        m3gIntegrity.setText(info[1]);

        info = mUrcParser.parseSecurityStatus(Content.EMM_L4C_EMM_INFO_INDEX,
                mNetworkInfo.get(Content.EMM_L4C_EMM_INFO_INDEX));
        m4gEnasCipher.setText(info[0]);
        m4gEnasIntegrity.setText(info[1]);

        info = mUrcParser.parseSecurityStatus(Content.ERRC_EM_SEC_PARAM_INDEX,
                mNetworkInfo.get(Content.ERRC_EM_SEC_PARAM_INDEX));
        m4gErrcCipher.setText(info[0]);
        m4gErrcIntegrity.setText(info[1]);

        info = mUrcParser.parseSecurityStatus(
                Content.ERRC_EM_ERRC_STATE_INDEX,
                mNetworkInfo.get(Content.ERRC_EM_ERRC_STATE_INDEX));
        if (!info[0].equals("---") && !info[0].equals("3") && !info[0].equals("6")) {
            m4gErrcCipher.setText("N/A");
            m4gErrcIntegrity.setText("N/A");
            mNetworkInfo.put(Content.ERRC_EM_SEC_PARAM_INDEX, "FFFFFFFF");
            mNetworkInfo.remove(Content.ERRC_EM_ERRC_STATE_INDEX);
        }
    }

    private void registerNetwork() {
        mPhone = PhoneFactory.getDefaultPhone();
        mPhone.registerForNetworkInfo(mUrcHandler, MSG_NW_INFO_URC, null);

        String[] atCommand = {"AT+EINFO?", "+EINFO"};
        sendATCommand(atCommand, MSG_NW_INFO);
    }

    private void unregisterNetwork() {
        mPhone.unregisterForNetworkInfo(mUrcHandler);

        mFlag = mFlag & ~FLAG;
        Log.v("@M_" + TAG, "The close flag is :" + mFlag);
        String[] atCloseCmd = new String[2];
        atCloseCmd[0] = "AT+EINFO=" + mFlag;
        atCloseCmd[1] = "";
        sendATCommand(atCloseCmd, MSG_NW_INFO_CLOSE);
    }

    private void sendATCommand(String[] atCommand, int msg) {
        mPhone.invokeOemRilRequestStrings(atCommand, mATCmdHander.obtainMessage(msg));
//        Toast.makeText(this,
//                atCommand[0], Toast.LENGTH_SHORT).show();
    }

    private void updateServiceState() {
        ServiceState serviceState = mPhone.getServiceState();
        if (serviceState == null) {
            return;
        }
        if (serviceState.getState() != ServiceState.STATE_POWER_OFF) {
            mNetworkInfo.clear();
            registerNetwork();
            updateUI();
            mTelephonyManager.listen((PhoneStateListener) mPhoneStateListener,
                    PhoneStateListener.LISTEN_NONE);
        }
    }

    private int getSubId(int slotId) {
        SubscriptionInfo result =
                SubscriptionManager.from(this).getActiveSubscriptionInfoForSimSlotIndex(slotId);
        if (result != null) {
            return result.getSubscriptionId();
        }
        return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    }

    /**
     * Phone state listener.
     */
    class MyPhoneStateListener extends PhoneStateListener {
        public MyPhoneStateListener(int subId) {
            super(subId);
        }

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            updateServiceState();
        }
    }
}
