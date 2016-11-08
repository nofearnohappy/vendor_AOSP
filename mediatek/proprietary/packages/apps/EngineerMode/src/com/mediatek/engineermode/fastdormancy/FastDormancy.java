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

package com.mediatek.engineermode.fastdormancy;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.SubscriptionManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneProxy;
import com.mediatek.internal.telephony.dataconnection.FdManager;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;

import com.mediatek.engineermode.Elog;
import com.mediatek.engineermode.R;

/**
 * Description: A helper tool to test fast dormancy.
 *
 * @author mtk54043
 *
 */
public class FastDormancy extends Activity implements OnClickListener {
    private static final String TAG = "EM/FD";
    private static final int COUNT = 6;
    private static final int COUNT_R8 = 4;
    private static final int INDEX_OFF_LE = 0;
    private static final int INDEX_ON_LE = 1;
    private static final int INDEX_OFF_R8 = 2;
    private static final int INDEX_ON_R8 = 3;
    private static final int INDEX_ON_AP = 4;
    private static final int INDEX_OFF_AP = 5;
    private static final int MSG_SET_TIME = 101;
    private static final int MSG_SEND_FD = 102;
    private static final int DIALOG_SET_FAILED = 201;
    private static final int DIALOG_SEND_FD = 202;

    private EditText[] mFastDormancyEdit;
    private Phone mPhone = null;
    private int mFdMdEnableMode;
    private FdManager mFdMgr = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fastdormancy);
        findViewById(R.id.fd_btn_set).setOnClickListener(this);
        findViewById(R.id.fd_btn_send).setOnClickListener(this);
        findViewById(R.id.fd_btn_config).setOnClickListener(this);

        mFastDormancyEdit = new EditText[COUNT];
        mFastDormancyEdit[INDEX_OFF_LE] = (EditText) findViewById(R.id.fd_edit_screen_off_legacy);
        mFastDormancyEdit[INDEX_ON_LE] = (EditText) findViewById(R.id.fd_edit_screen_on_legacy);
        mFastDormancyEdit[INDEX_OFF_R8] = (EditText) findViewById(R.id.fd_edit_screen_off_r8fd);
        mFastDormancyEdit[INDEX_ON_R8] = (EditText) findViewById(R.id.fd_edit_screen_on_r8fd);
        mFastDormancyEdit[INDEX_ON_AP] = (EditText) findViewById(R.id.fd_edit_screen_on_ap);
        mFastDormancyEdit[INDEX_OFF_AP] = (EditText) findViewById(R.id.fd_edit_screen_off_ap);

    }

    private void initFdMgr() {
        int subId = SubscriptionManager.getDefaultDataSubId();
        mPhone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(subId));
        if (mPhone instanceof PhoneProxy) {
            Phone phone = ((PhoneProxy) mPhone).getActivePhone();
            if (phone instanceof PhoneBase) {
                mFdMgr = FdManager.getInstance((PhoneBase) phone);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String[] fastDTimerValue = null;

        initFdMgr();

        if (mFdMgr != null) {
            fastDTimerValue = mFdMgr.getFdTimerValue();
        }
        mFdMdEnableMode = Integer.parseInt(SystemProperties.get("ril.fd.mode", "0"));

        if (mFdMdEnableMode == 1) {
            if (fastDTimerValue == null) {
                Elog.e(TAG, "getFDTimerValue() == null");
                Toast.makeText(this, "Query FD parameter failed!", Toast.LENGTH_LONG).show();
            } else if (fastDTimerValue.length < COUNT_R8) {
                Elog.e(TAG, "getFDTimerValue().length < 4 ");
                Toast.makeText(this, "Query FD parameter failed!", Toast.LENGTH_LONG).show();
            } else if (fastDTimerValue.length == COUNT_R8) {
                for (int i = 0; i < COUNT_R8; i++) {
                    mFastDormancyEdit[i].setText(fastDTimerValue[i]);
                    Elog.i(TAG, "fastDTimerValue[" + i + "] = " + fastDTimerValue[i]);
                }
            } else {
                Elog.e(TAG, "getFDTimerValue().length == " + fastDTimerValue.length);
                for (int i = 0; i < fastDTimerValue.length; i++) {
                    Elog.i(TAG, "fastDTimerValue[" + i + "] = " + fastDTimerValue[i]);
                }
            }
        } else {
            String str1 = SystemProperties.get("persist.radio.fd.counter", "20");
            String str2 = SystemProperties.get("persist.radio.fd.off.counter", "20");
            mFastDormancyEdit[INDEX_ON_AP].setText(str1);
            mFastDormancyEdit[INDEX_OFF_AP].setText(str2);

            findViewById(R.id.fd_screen_on_r8fd_layout).setVisibility(View.GONE);
            findViewById(R.id.fd_screen_off_r8fd_layout).setVisibility(View.GONE);
            findViewById(R.id.fd_screen_on_legacy_layout).setVisibility(View.GONE);
            findViewById(R.id.fd_screen_off_legacy_layout).setVisibility(View.GONE);
            findViewById(R.id.fd_ap_sol_on_layout).setVisibility(View.VISIBLE);
            findViewById(R.id.fd_ap_sol_off_layout).setVisibility(View.VISIBLE);
        }

    }

    @Override
    protected void onDestroy() {
        Elog.i(TAG, "onDestroy(),removeMessages");
        mResponseHander.removeMessages(MSG_SET_TIME);
        mResponseHander.removeMessages(MSG_SEND_FD);
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
        case R.id.fd_btn_set:
            if (mFdMdEnableMode == 1) { //modem solution
                String[] fastDTimerValue = new String[COUNT_R8];
                for (int i = 0; i < COUNT_R8; i++) {
                    fastDTimerValue[i] = mFastDormancyEdit[i].getText().toString().trim();
                    try {
                        Integer.valueOf(fastDTimerValue[i]);
                    } catch (NumberFormatException e) {
                        Elog.e(TAG, "NumberFormatException");
                        Toast.makeText(this, "Your input number must be a int type!",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
    //                if ("".equals(fastDTimerValue[i])) {
    //                    Toast.makeText(this, "Please check your input number.",
    //                            Toast.LENGTH_SHORT).show();
    //                    return;
    //                }
                }
                if (mFdMgr != null) {
                    mFdMgr.setFdTimerValue(fastDTimerValue,
                            mResponseHander.obtainMessage(MSG_SET_TIME));
                }
            } else {
                //Get scri data counter for screen on
                boolean isSuccess = true;
                String str1 = mFastDormancyEdit[INDEX_ON_AP].getText().toString().trim();
                int val1 = Integer.parseInt(str1);
                if (val1 < 5 || val1 > 3600) {
                    str1 = "20";
                    isSuccess = false;
                }
                SystemProperties.set("persist.radio.fd.counter", str1);

                //Get scri data counter for screen off
                String str2 = mFastDormancyEdit[INDEX_OFF_AP].getText().toString().trim();
                int val2 = Integer.parseInt(str2);
                if (val2 < 5 || val2 > 3600) {
                    str2 = "20";
                    isSuccess = false;
                }
                SystemProperties.set("persist.radio.fd.off.counter", str2);

                if (isSuccess) {
                    Toast.makeText(this, "Success!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Please set value between 5~3600!",
                            Toast.LENGTH_LONG).show();
                    Toast.makeText(this, "Restore to default: 20", Toast.LENGTH_LONG).show();
                }
            }


            break;
        case R.id.fd_btn_send:
            if (mPhone == null) {
                Elog.e(TAG, "onClick fd_btn_send mPhone is null");
                return;
            }
            String fastDormancyAT[] = { "AT+ESCRI", "" };
            mPhone.invokeOemRilRequestStrings(fastDormancyAT,
                    mResponseHander.obtainMessage(MSG_SEND_FD));
            break;
        case R.id.fd_btn_config:
            startActivity(new Intent(this, ConfigFD.class));
            break;
        default:
            break;
        }
    }

    private Handler mResponseHander = new Handler() {
        public void handleMessage(Message msg) {
            Elog.i(TAG, "Receive msg from modem");
            AsyncResult ar;
            if (msg.what == MSG_SET_TIME) {
                Elog.i(TAG, "Receive MSG_SET_TIME");
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    Toast.makeText(FastDormancy.this, "Success!", Toast.LENGTH_LONG).show();
                } else {
                    showDialog(DIALOG_SET_FAILED);
                }
            } else if (msg.what == MSG_SEND_FD) {
                Elog.i(TAG, "Receive MSG_SEND_FD");
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    Toast.makeText(FastDormancy.this, "Success!", Toast.LENGTH_LONG).show();
                } else {
                    showDialog(DIALOG_SET_FAILED);
                }
            }
        }
    };

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (id == DIALOG_SET_FAILED) {
            return builder.setTitle("Warnning!").setMessage("Failed to set FD parameter.")
                    .setPositiveButton("OK", null).create();
        } else if (id == DIALOG_SEND_FD) {
            return builder.setTitle("Warnning!").setMessage("Failed to send FD.")
                    .setPositiveButton("OK", null).create();
        }
        // else {
        // return builder.setTitle("Warnning!").setMessage("Query failed.")
        //        .setPositiveButton("OK", null).create();
        // }
        return null;
    }
}
