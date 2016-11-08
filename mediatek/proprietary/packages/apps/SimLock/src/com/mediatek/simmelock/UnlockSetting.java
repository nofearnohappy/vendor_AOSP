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

package com.android.simmelock;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

public class UnlockSetting extends SimLockBaseActivity {
    private static final String TAG = "SIMMELOCK";
    private static final int DIALOG_PASSWORDLENGTHINCORRECT = 1;
    private static final int DIALOG_UNLOCKSUCCEED = 2;
    private static final int DIALOG_UNLOCKFAILED = 3;
    private static final int DIALOG_QUERYFAIL = 4;
    private static final int UNLOCK_ICC_SML_COMPLETE = 120;
    private static final int UNLOCK_ICC_SML_QUERYLEFTTIMES = 110;

    EditText mEtPwd = null;
    TextView mEtPwdLeftChances = null;
    private int mPwdLeftChances = 5;
    private boolean mClickFlag = false;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;
            switch (msg.what) {
            case UNLOCK_ICC_SML_COMPLETE:
                if (mClickFlag == true) {
                    Log.i(TAG, "set ar: " + ar.exception);
                    if (ar.exception != null) {  // fail to unlock
                        queryLeftTimes();
                        showDialog(DIALOG_UNLOCKFAILED);
                    } else {
                        showDialog(DIALOG_UNLOCKSUCCEED);
                    }
                    Log.i(TAG, "handler UNLOCK_ICC_SML_COMPLETE mPwdLeftChances: "
                            + mPwdLeftChances);
                }
                break;
            case UNLOCK_ICC_SML_QUERYLEFTTIMES:
                Log.i(TAG, "handler query");
                Log.i(TAG, "query ar: " + ar.exception);
                if (ar.exception != null) {
                    showDialog(DIALOG_QUERYFAIL); // Query fail!
                } else {
                    AsyncResult ar1 = (AsyncResult) msg.obj;
                    int[] lockState = (int[]) ar1.result;
                    if (lockState[2] > 0) {
                        // still have chances to unlock
                        mPwdLeftChances = lockState[2];
                        mEtPwdLeftChances.setText(String.valueOf(mPwdLeftChances));
                        Log.i(TAG, "query mPwdLeftChances: " + mPwdLeftChances);
                    } else {
                        UnlockSetting.this.finish();
                    }
                }
                break;
            default:
                break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.unlocksetting);

        Bundle bundle = this.getIntent().getExtras();
        if (bundle != null) {
            String localName = bundle.getString("LOCALNAME");
            Configuration conf = getResources().getConfiguration();
            String locale = conf.locale.getDisplayName(conf.locale);
            Log.i(TAG, "localName: " + localName + "    || getLocalClassName: " + locale);
            if (localName != null && !localName.equals(locale)) {
                finish();
                return;
            }
        }

        // initial left password input chances
        mEtPwdLeftChances = (TextView) findViewById(R.id.idunlockEditInputChancesleft);

        // set the regulation of EditText
        mEtPwd = (EditText) findViewById(R.id.idunlockEditInputPassword);
        mEtPwd.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        SMLCommonProcess.limitEditTextPassword(mEtPwd, 8);
        mEtPwd.setOnLongClickListener(mOnLongClickListener);

        Button btnConfirm = (Button) findViewById(R.id.idunlockButtonConfirm);
        // Yu for ICS
        btnConfirm.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                Log.i(TAG, "mClickFlag: " + mClickFlag);
                if (mClickFlag) {
                    return;
                } else {
                    mClickFlag = true;
                }

                if ((mEtPwd.getText().length() < 4) || ((mEtPwd.getText().length() > 8))) {
                    showDialog(DIALOG_PASSWORDLENGTHINCORRECT);
                } else {
                    // get the left chances to unlock(less than 5)
                    Message callback = Message.obtain(mHandler, UNLOCK_ICC_SML_COMPLETE);
                    Phone phone = PhoneFactory.getPhone(mSlotId);
                    phone.getIccCard().setIccNetworkLockEnabled(mLockCategory, 0,
                            mEtPwd.getText().toString(), null, null, null, callback);
                }
            }
        });

        Button btnCancel = (Button) findViewById(R.id.idunlockButtonCancel);
        btnCancel.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                UnlockSetting.this.finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        queryLeftTimes();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_UNLOCKFAILED || id == DIALOG_PASSWORDLENGTHINCORRECT
                || id == DIALOG_UNLOCKSUCCEED || id == DIALOG_QUERYFAIL) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.strAttention)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setOnKeyListener(this);

            switch (id) {
            case DIALOG_UNLOCKFAILED: // Fail
                Log.i(TAG, "show DIALOG_UNLOCKFAILED");
                builder.setMessage(R.string.strUnlockFail).setPositiveButton(R.string.strYes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Log.i(TAG, "query mPwdLeftChances: " + mPwdLeftChances);
                                mEtPwd.setText("");
                                dialog.cancel();
                                mClickFlag = false;
                            }
                        });
                return builder.create();
            case DIALOG_PASSWORDLENGTHINCORRECT:// Length is incorrect
                Log.i(TAG, "show DIALOG_PASSWORDLENGTHINCORRECT");
                builder.setMessage(R.string.strPasswordLengthIncorrect).setPositiveButton(
                        R.string.strYes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                mEtPwd.setText("");
                                dialog.cancel();
                                mClickFlag = false;
                            }
                        });
                return builder.create();
            case DIALOG_UNLOCKSUCCEED:// Succeed
                Log.i(TAG, "show DIALOG_UNLOCKSUCCEED");
                builder.setMessage(R.string.strUnlockSucceed).setPositiveButton(R.string.strYes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (id == AlertDialog.BUTTON_POSITIVE) {
                                    Log.i(TAG, "Success dialog UnlockSetting dismissed.");
                                    mClickFlag = false;
                                    try {
                                        if (null != dialog) {
                                            dialog.cancel();
                                        }
                                    } catch (IllegalArgumentException e) {
                                        Log.e(TAG, "Catch IllegalArgumentException");
                                    }
                                    finish();
                                    Log.i(TAG, "Success dialog dismissed.");
                                }
                            }
                        });
                return builder.create();
            case DIALOG_QUERYFAIL:// Query fail
                Log.i(TAG, "show DIALOG_QUERYFAIL");
                builder.setMessage(R.string.strQueryFailed).setPositiveButton(R.string.strYes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                mEtPwd.setText("");
                                dialog.cancel();
                                mClickFlag = false;
                            }
                        });
                return builder.create();
            default:
                break;
            }
        }
        Log.i(TAG, "show null");
        return super.onCreateDialog(id);
    }

    private void queryLeftTimes() {
        Message callbackQuery = Message.obtain(mHandler, UNLOCK_ICC_SML_QUERYLEFTTIMES);
        Phone phone = PhoneFactory.getPhone(mSlotId);
        phone.getIccCard().queryIccNetworkLock(mLockCategory, callbackQuery);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.i(TAG, "[UnlckSetting]onConfigurationChanged ");
        super.onConfigurationChanged(newConfig);
    }
}
