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
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.mediatek.engineermode.R;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Antenna diversity.
 */
public class AntennaDiversity extends Activity implements OnClickListener {
    private static final String TAG = "NetworkInfo";
    private static final int MSG_NW_INFO = 1;
    private static final int MSG_NW_INFO_URC = 2;
    private static final int MSG_NW_INFO_OPEN = 3;
    private static final int MSG_NW_INFO_CLOSE = 4;
    private static final int MSG_NW_INFO_QUERY_3G_ANT = 5;
    private static final int MSG_NW_INFO_QUERY_4G_ANT = 6;
    private static final int MSG_NW_INFO_SET_ANT = 7;
    private static final int FLAG = 0x08;
    private static final int FLAG_OFFSET = 8;
    private static final int ANT_PRX = 0;
    private static final int ANT_DRX = 1;
    private static final int ANT_BOTH = 2;
    private static final String DEFAULT_VALUE = "---";

    private TextView mRscpAnt0;
    private TextView mRscpAnt1;
    private TextView mRscpCombine;
    private TextView mPssiAnt0;
    private TextView mPssiAnt1;
    private TextView mPssiCombine;
    private TextView mRsrpAnt0;
    private TextView mRsrpAnt1;
    private TextView mRsrpCombine;
    private TextView mRsrqAnt0;
    private TextView mRsrqAnt1;
    private TextView mRsrqCombine;
    private TextView mRssiAnt0;
    private TextView mRssiAnt1;
    private TextView mRssiCombine;
    private TextView mSinrAnt0;
    private TextView mSinrAnt1;
    private TextView mSinrCombine;

    private ToggleButton m3GPrx;
    private ToggleButton m3GDrx;
    private ToggleButton m3GBoth;
    private ToggleButton m4GPrx;
    private ToggleButton m4GDrx;
    private ToggleButton m4GBoth;

    private int mItemCount = 2;
    private int[] mItem = {Content.EL1TX_EM_TX_INFO_INDEX,
            Content.UL1_EM_PRX_DRX_MEASURE_INFO_INDEX};
    private NetworkInfoUrcParser mUrcParser;
    private Phone mPhone = null;
    private int mFlag = 0;
    private HashMap<Integer, String> mNetworkInfo = new HashMap<Integer, String>();
    private int m3gAnt = -1;
    private int m4gAnt = -1;

    private Handler mAtCmdHandler = new Handler() {
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
                    Toast.makeText(AntennaDiversity.this, getString(R.string.send_at_fail),
                            Toast.LENGTH_SHORT);
                }
                break;
            case MSG_NW_INFO_QUERY_3G_ANT:
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    String[] data = (String[]) ar.result;
                    if (data == null || data.length < 1) {
                        Toast.makeText(AntennaDiversity.this, "Query fail", Toast.LENGTH_SHORT);
                        break;
                    }
                    Log.v("@M_" + TAG, "data[0] is : " + data[0]);
                    Pattern pat = Pattern.compile("PRI\\[(.*)\\].*DIV\\[(.*)\\]");
                    Matcher matcher = pat.matcher(data[0]);
                    while (matcher.find()) {
                        String pri = matcher.group(1);
                        String div = matcher.group(2);
                        if ("1".equals(pri) && "1".equals(div)) {
                            m3gAnt = ANT_BOTH;
                        } else if ("1".equals(pri) && "0".equals(div)) {
                            m3gAnt = ANT_PRX;
                        } else if ("0".equals(pri) && "1".equals(div)) {
                            m3gAnt = ANT_DRX;
                        }
                        updateUI();
                        updateButtons();
                    }
                } else {
                    Toast.makeText(AntennaDiversity.this, getString(R.string.send_at_fail),
                            Toast.LENGTH_SHORT);
                }
                break;
            case MSG_NW_INFO_QUERY_4G_ANT:
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    String[] data = (String[]) ar.result;
                    if (data == null || data.length < 1) {
                        Toast.makeText(AntennaDiversity.this, "Query fail", Toast.LENGTH_SHORT);
                        break;
                    }
                    Log.v("@M_" + TAG, "data[0] is : " + data[0]);
                    Pattern pat = Pattern.compile("%MIMOANTCHECK: (\\d)");
                    Matcher matcher = pat.matcher(data[0]);
                    while (matcher.find()) {
                        String ant = matcher.group(1);
                        if ("0".equals(ant)) {
                            m4gAnt = ANT_BOTH;
                        } else if ("1".equals(ant)) {
                            m4gAnt = ANT_DRX;
                        } else if ("2".equals(ant)) {
                            m4gAnt = ANT_PRX;
                        }
                        updateUI();
                        updateButtons();
                    }
                } else {
                    Toast.makeText(AntennaDiversity.this, getString(R.string.send_at_fail),
                            Toast.LENGTH_SHORT);
                }
                break;
            case MSG_NW_INFO_OPEN:
            case MSG_NW_INFO_CLOSE:
            case MSG_NW_INFO_SET_ANT:
                ar = (AsyncResult) msg.obj;
                if (ar.exception != null) {
                    Toast.makeText(AntennaDiversity.this, getString(R.string.send_at_fail),
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
                    Toast.makeText(AntennaDiversity.this,
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
        setContentView(R.layout.antenna_diversity);
        mRscpAnt0 = (TextView) findViewById(R.id.antenna_diversity_rscp_ant0);
        mRscpAnt1 = (TextView) findViewById(R.id.antenna_diversity_rscp_ant1);
        mRscpCombine = (TextView) findViewById(R.id.antenna_diversity_rscp_combined);
        mPssiAnt0 = (TextView) findViewById(R.id.antenna_diversity_pssi_ant0);
        mPssiAnt1 = (TextView) findViewById(R.id.antenna_diversity_pssi_ant1);
        mPssiCombine = (TextView) findViewById(R.id.antenna_diversity_pssi_combined);
        mRsrpAnt0 = (TextView) findViewById(R.id.antenna_diversity_rsrp_ant0);
        mRsrpAnt1 = (TextView) findViewById(R.id.antenna_diversity_rsrp_ant1);
        mRsrpCombine = (TextView) findViewById(R.id.antenna_diversity_rsrp_combined);
        mRsrqAnt0 = (TextView) findViewById(R.id.antenna_diversity_rsrq_ant0);
        mRsrqAnt1 = (TextView) findViewById(R.id.antenna_diversity_rsrq_ant1);
        mRsrqCombine = (TextView) findViewById(R.id.antenna_diversity_rsrq_combined);
        mRssiAnt0 = (TextView) findViewById(R.id.antenna_diversity_rssi_ant0);
        mRssiAnt1 = (TextView) findViewById(R.id.antenna_diversity_rssi_ant1);
        mRssiCombine = (TextView) findViewById(R.id.antenna_diversity_rssi_combined);
        mSinrAnt0 = (TextView) findViewById(R.id.antenna_diversity_sinr_ant0);
        mSinrAnt1 = (TextView) findViewById(R.id.antenna_diversity_sinr_ant1);
        mSinrCombine = (TextView) findViewById(R.id.antenna_diversity_sinr_combined);
        m3GPrx = (ToggleButton) findViewById(R.id.antenna_diversity_3g_prx);
        m3GDrx = (ToggleButton) findViewById(R.id.antenna_diversity_3g_drx);
        m3GBoth = (ToggleButton) findViewById(R.id.antenna_diversity_3g_prx_drx);
        m4GPrx = (ToggleButton) findViewById(R.id.antenna_diversity_4g_prx);
        m4GDrx = (ToggleButton) findViewById(R.id.antenna_diversity_4g_drx);
        m4GBoth = (ToggleButton) findViewById(R.id.antenna_diversity_4g_prx_drx);
        m3GPrx.setOnClickListener(this);
        m3GDrx.setOnClickListener(this);
        m3GBoth.setOnClickListener(this);
        m4GPrx.setOnClickListener(this);
        m4GDrx.setOnClickListener(this);
        m4GBoth.setOnClickListener(this);
        mUrcParser = new NetworkInfoUrcParser(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNetworkInfo.clear();
        registerNetwork();
        updateUI();
    }

    @Override
    public void onPause() {
        unregisterNetwork();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View arg0) {
        String cmd = "";
        int old3gAnt = m3gAnt;
        int old4gAnt = m4gAnt;
        if (arg0.getId() == m3GPrx.getId()) {
            m3gAnt = ANT_PRX;
            cmd = "AT%RXMRD=2,1";
        } else if (arg0.getId() == m3GDrx.getId()) {
            m3gAnt = ANT_DRX;
            cmd = "AT%RXMRD=1,1";
        } else if (arg0.getId() == m3GBoth.getId()) {
            m3gAnt = ANT_BOTH;
            cmd = "AT%RXMRD=0,1";
        } else if (arg0.getId() == m4GPrx.getId()) {
            m4gAnt = ANT_PRX;
            cmd = "AT%MIMOANTCHECK=2,1";
        } else if (arg0.getId() == m4GDrx.getId()) {
            m4gAnt = ANT_DRX;
            cmd = "AT%MIMOANTCHECK=1,1";
        } else if (arg0.getId() == m4GBoth.getId()) {
            m4gAnt = ANT_BOTH;
            cmd = "AT%MIMOANTCHECK=0,1";
        }
        if (old3gAnt != m3gAnt) {
            sendATCommand(new String[] {cmd, ""}, MSG_NW_INFO_SET_ANT);
            mNetworkInfo.remove(Content.UL1_EM_PRX_DRX_MEASURE_INFO_INDEX);
            updateUI();
        } else if (old4gAnt != m4gAnt) {
            sendATCommand(new String[] {cmd, ""}, MSG_NW_INFO_SET_ANT);
            mNetworkInfo.remove(Content.EL1TX_EM_TX_INFO_INDEX);
            updateUI();
        }
        updateButtons();
    }

    private void updateUI() {
        setDefaultText();
        String[] info = mUrcParser.parseAntennaDiversity(
                Content.UL1_EM_PRX_DRX_MEASURE_INFO_INDEX,
                mNetworkInfo.get(Content.UL1_EM_PRX_DRX_MEASURE_INFO_INDEX));
        if (info != null) {
            switch (m3gAnt) {
            case ANT_PRX:
                mRscpAnt0.setText(info[0]);
                mPssiAnt0.setText(info[1]);
                break;
            case ANT_DRX:
                mRscpAnt1.setText(info[2]);
                mPssiAnt1.setText(info[3]);
                break;
            case ANT_BOTH:
                mRscpCombine.setText(info[4]);
                mPssiCombine.setText(info[5]);
                break;
            default:
                break;
            }
        }
        info = mUrcParser.parseAntennaDiversity(
                Content.EL1TX_EM_TX_INFO_INDEX,
                mNetworkInfo.get(Content.EL1TX_EM_TX_INFO_INDEX));
        if (info != null) {
            switch (m4gAnt) {
            case ANT_PRX:
                mRsrpAnt0.setText(info[0]);
                mRsrqAnt0.setText(info[1]);
                mRssiAnt0.setText(info[2]);
                mSinrAnt0.setText(info[3]);
                break;
            case ANT_DRX:
                mRsrpAnt1.setText(info[4]);
                mRsrqAnt1.setText(info[5]);
                mRssiAnt1.setText(info[6]);
                mSinrAnt1.setText(info[7]);
                break;
            case ANT_BOTH:
                mRsrpCombine.setText(info[8]);
                mRsrqCombine.setText(info[9]);
                mRssiCombine.setText(info[10]);
                mSinrCombine.setText(info[11]);
                break;
            default:
                break;
            }
        }
    }

    private void setDefaultText() {
        mRscpAnt0.setText(DEFAULT_VALUE);
        mPssiAnt0.setText(DEFAULT_VALUE);
        mRscpAnt1.setText(DEFAULT_VALUE);
        mPssiAnt1.setText(DEFAULT_VALUE);
        mRscpCombine.setText(DEFAULT_VALUE);
        mPssiCombine.setText(DEFAULT_VALUE);
        mRsrpAnt0.setText(DEFAULT_VALUE);
        mRsrqAnt0.setText(DEFAULT_VALUE);
        mRssiAnt0.setText(DEFAULT_VALUE);
        mSinrAnt0.setText(DEFAULT_VALUE);
        mRsrpAnt1.setText(DEFAULT_VALUE);
        mRsrqAnt1.setText(DEFAULT_VALUE);
        mRssiAnt1.setText(DEFAULT_VALUE);
        mSinrAnt1.setText(DEFAULT_VALUE);
        mRsrpCombine.setText(DEFAULT_VALUE);
        mRsrqCombine.setText(DEFAULT_VALUE);
        mRssiCombine.setText(DEFAULT_VALUE);
        mSinrCombine.setText(DEFAULT_VALUE);
    }

    private void updateButtons() {
        m3GPrx.setChecked(m3gAnt == ANT_PRX);
        m3GDrx.setChecked(m3gAnt == ANT_DRX);
        m3GBoth.setChecked(m3gAnt == ANT_BOTH);
        m4GPrx.setChecked(m4gAnt == ANT_PRX);
        m4GDrx.setChecked(m4gAnt == ANT_DRX);
        m4GBoth.setChecked(m4gAnt == ANT_BOTH);
    }

    private void registerNetwork() {
        mPhone = PhoneFactory.getDefaultPhone();
        mPhone.registerForNetworkInfo(mUrcHandler, MSG_NW_INFO_URC, null);

        String[] atCommand = {"AT+EINFO?", "+EINFO"};
        sendATCommand(atCommand, MSG_NW_INFO);

        atCommand = new String[] {"AT%RXMRD?", "\2PRI["};
        sendATCommand(atCommand, MSG_NW_INFO_QUERY_3G_ANT);

        atCommand = new String[] {"AT%MIMOANTCHECK?", "\2%MIMOANTCHECK:"};
        sendATCommand(atCommand, MSG_NW_INFO_QUERY_4G_ANT);
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
        mPhone.invokeOemRilRequestStrings(atCommand, mAtCmdHandler.obtainMessage(msg));
    }
}
