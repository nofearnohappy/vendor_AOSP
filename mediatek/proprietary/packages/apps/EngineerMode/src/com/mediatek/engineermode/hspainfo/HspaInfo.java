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

package com.mediatek.engineermode.hspainfo;

import android.app.Activity;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;

import com.mediatek.engineermode.FeatureSupport;
import com.mediatek.engineermode.ModemCategory;
import com.mediatek.engineermode.R;
import com.mediatek.internal.telephony.ltedc.LteDcPhoneProxy;

public class HspaInfo extends Activity {
    private static final String TAG = "HspaInfo";
    private static final int EVENT_HSPA_INFO = 1;
    private static final int EVENT_DC_HSPA_INFO = 2;
    private static final int EVENT_NW_INFO = 3;
    private static final int EVENT_NW_INFO_URC = 4;
    private static final int EVENT_NW_INFO_OPEN = 5;
    private static final int EVENT_NW_INFO_CLOSE = 6;
    private static final String QUERY_CMD = "AT+EHSM?";
    private static final String RESPONSE_CMD = "+EHSM: ";
    private static final String QUERY_DC_CMD = "AT+PSBEARER?";
    private static final String RESPONSE_DC_CMD = "+PSBEARER: ";
    private static final int EM_INFO_TYPE = 173;
    private static final int FLAG = 0x08;
    private static final int FLAG_OFFSET = 8;
    private static final int DC_HSDPA_MIN = 12;
    private static final int DC_HSDPA_MAX = 17;

    private Phone mPhone = null;
    private TextView mTextView;
    private TextView mTextView2;
    private int mFlag = 0;

    private Handler mATCmdHander = new Handler() {
        public void handleMessage(Message msg) {
            AsyncResult ar;
            switch (msg.what) {
            case EVENT_NW_INFO:
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    String[] data = (String[]) ar.result;
                    Log.v("@M_" + TAG, "data[0] is : " + data[0]);
                    Log.v("@M_" + TAG, "flag is : " + data[0].substring(FLAG_OFFSET));
                    mFlag = Integer.valueOf(data[0].substring(FLAG_OFFSET));
                    mFlag = mFlag | FLAG;
                    Log.v("@M_" + TAG, "flag change is : " + mFlag);
                    String[] atCommand = new String[2];
                    atCommand[0] = "AT+EINFO=" + mFlag + "," + EM_INFO_TYPE + ",0";
                    atCommand[1] = "+EINFO";
                    sendATCommand(atCommand, EVENT_NW_INFO_OPEN);
                }
                // fall through
            case EVENT_NW_INFO_OPEN:
            case EVENT_NW_INFO_CLOSE:
                ar = (AsyncResult) msg.obj;
                if (ar.exception != null) {
                    Toast.makeText(HspaInfo.this, "Send AT command failed", Toast.LENGTH_LONG);
                }
                break;
            case EVENT_HSPA_INFO:
                ar = (AsyncResult) msg.obj;
                if (ar != null && ar.exception == null) {
                    handleQuery((String[]) ar.result, msg.what);
                } else {
                    Toast.makeText(HspaInfo.this, "Send AT command failed", Toast.LENGTH_LONG);
                }
                break;
            case EVENT_DC_HSPA_INFO:
                ar = (AsyncResult) msg.obj;
                if (ar != null && ar.exception == null) {
                    handleQuery((String[]) ar.result, msg.what);
                } else {
                    Toast.makeText(HspaInfo.this, "Send AT command failed", Toast.LENGTH_LONG);
                }
                break;
            default:
                break;
            }
        }
    };

    private final Handler mUrcHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == EVENT_NW_INFO_URC) {
                AsyncResult ar = (AsyncResult) msg.obj;
                String[] data = (String[]) ar.result;
                Log.v("@M_" + TAG, "Receive URC: " + data[0] + ", " + data[1]);
                try {
                    if (Integer.parseInt(data[0]) == EM_INFO_TYPE) {
                        updateDcUI(Integer.parseInt(data[1]));
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(HspaInfo.this,
                            "Return type error", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hspa_info);
        mTextView = (TextView) findViewById(R.id.text_view);
        mTextView2 = (TextView) findViewById(R.id.text_view2);
        if (FeatureSupport.isSupported(FeatureSupport.FK_TC1_FEATURE)) {
            mTextView2.setText("DC HSDPA off");
        } else {
            mTextView2.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String[] cmd = new String[2];
        cmd[0] = QUERY_CMD;
        cmd[1] = RESPONSE_CMD;
        if (TelephonyManager.getDefault().getPhoneCount() > 1) {
            mPhone = PhoneFactory.getPhone(PhoneConstants.SIM_ID_1);
        } else {
            mPhone = PhoneFactory.getDefaultPhone();
        }

        if (FeatureSupport.isSupported(FeatureSupport.FK_MTK_C2K_SUPPORT)) {
            if ((FeatureSupport.isSupported(FeatureSupport.FK_MTK_SVLTE_SUPPORT)
                 || FeatureSupport.isSupported(FeatureSupport.FK_SRLTE_SUPPORT))
                    && mPhone instanceof LteDcPhoneProxy) {
                mPhone = ((LteDcPhoneProxy) mPhone).getLtePhone();
            }
            if (FeatureSupport.isSupported(FeatureSupport.FK_EVDO_DT_SUPPORT)
                    && mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
                mPhone = PhoneFactory.getPhone(PhoneConstants.SIM_ID_2);
            }
        }

        mPhone.invokeOemRilRequestStrings(cmd, mATCmdHander.obtainMessage(EVENT_HSPA_INFO));

        if (FeatureSupport.isSupported(FeatureSupport.FK_TC1_FEATURE)) {
            mPhone.invokeOemRilRequestStrings(new String[] {QUERY_DC_CMD, RESPONSE_DC_CMD},
                    mATCmdHander.obtainMessage(EVENT_DC_HSPA_INFO));
            registerNetwork();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (FeatureSupport.isSupported(FeatureSupport.FK_TC1_FEATURE)) {
            unregisterNetwork();
        }
    }

    private void handleQuery(String[] result, int queryWhat) {
        if (result != null && result.length > 0) {
            Log.v("@M_" + TAG, "Modem return: " + result[0]);
            Log.v("@M_" + TAG, "queryWhat: " + queryWhat);
            if (queryWhat == EVENT_HSPA_INFO) {
                String[] mode = result[0]
                        .substring(RESPONSE_CMD.length(), result[0].length()).split(",");
                if (mode != null && mode.length > 0) {
                    try {
                        Log.v("@M_" + TAG, "mode[0]: \"" + mode[0] + "\"");
                        updateUI(Integer.parseInt(mode[0]));
                    } catch (NumberFormatException e) {
                        Log.e("@M_" + TAG, "Modem return invalid mode: " + mode[0]);
                        mTextView.setText("Error: invalid mode: " + mode[0]);
                    }
                    return;
                }
            } else if (queryWhat == EVENT_DC_HSPA_INFO) {
                String[] mode = result[0]
                        .substring(RESPONSE_DC_CMD.length(), result[0].length()).split(",");
                if (mode != null && mode.length > 1) {
                    try {
                        Log.v("@M_" + TAG, "mode[1]: \"" + mode[1] + "\"");
                        updateDcUI(Integer.parseInt(mode[1].trim()));
                    } catch (NumberFormatException e) {
                        Log.e("@M_" + TAG, "Modem return invalid mode: " + mode[1]);
                        mTextView.setText("Error: invalid mode: " + mode[1]);
                    }
                    return;
                }
            }
        }
        mTextView.setText("Error: invalid mode.");
    }

    private void updateUI(int mode) {
        String[] modeArray;
        if (ModemCategory.getModemType() == ModemCategory.MODEM_TD) {
            modeArray = getResources().getStringArray(R.array.hspa_info_mode_array_td);
        } else {
            modeArray = getResources().getStringArray(R.array.hspa_info_mode_array_fd);
        }

        if (mode < 0 || mode >= modeArray.length) {
            Log.e("@M_" + TAG, "Modem return invalid mode: " + mode);
            mTextView.setText("Error: invalid mode: " + mode);
            return;
        }

        mTextView.setText(modeArray[mode]);
    }

    private void updateDcUI(int info) {
        if (info >= DC_HSDPA_MIN && info <= DC_HSDPA_MAX) {
            mTextView2.setText("DC HSDPA on");
        } else {
            mTextView2.setText("DC HSDPA off");
        }
    }

    private void registerNetwork() {
        mPhone.registerForNetworkInfo(mUrcHandler, EVENT_NW_INFO_URC, null);

        String[] atCommand = {"AT+EINFO?", "+EINFO"};
        sendATCommand(atCommand, EVENT_NW_INFO);
    }

    private void unregisterNetwork() {
        mPhone.unregisterForNetworkInfo(mUrcHandler);

        mFlag = mFlag & ~FLAG;
        Log.v("@M_" + TAG, "The close flag is :" + mFlag);
        String[] atCloseCmd = new String[2];
        atCloseCmd[0] = "AT+EINFO=" + mFlag;
        atCloseCmd[1] = "";
        sendATCommand(atCloseCmd, EVENT_NW_INFO_CLOSE);
    }

    private void sendATCommand(String[] atCommand, int msg) {
        mPhone.invokeOemRilRequestStrings(atCommand, mATCmdHander.obtainMessage(msg));
    }
}
