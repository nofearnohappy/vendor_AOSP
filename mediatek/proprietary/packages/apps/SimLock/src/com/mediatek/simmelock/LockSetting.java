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

public class LockSetting extends SimLockBaseActivity {
    private static final String TAG = "SIMMELOCK";
    private static final int LOCK_ICC_SML_COMPLETE = 120;
    private static final int DIALOG_LOCKFAIL = 3;
    private static final int DIALOG_PASSWORDLENGTHINCORRECT = 1;
    private static final int DIALOG_LOCKSUCCEED = 2;
    private static final int DIALOG_PASSWORDWRONG = 4;

    private EditText mEtPwd = null;
    private EditText mEtPwdConfirm = null;
    // the true password string which need to be compared with the input string
    private String mLockPassword = null;
    private boolean mClickFlag = false;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;
            switch (msg.what) {
            case LOCK_ICC_SML_COMPLETE:
                if (ar.exception != null) {
                    showDialog(DIALOG_LOCKFAIL);
                } else {
                    showDialog(DIALOG_LOCKSUCCEED);
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
        setContentView(R.layout.locksetting);

        // set the regulation of EditText
        mEtPwd = (EditText) findViewById(R.id.idEditInputPassword);
        mEtPwd.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        mEtPwd.setOnLongClickListener(mOnLongClickListener);
        SMLCommonProcess.limitEditTextPassword(mEtPwd, 8);
        if (mLockCategory == 0 || mLockCategory == 1 || mLockCategory == 2
                || mLockCategory == 3 || mLockCategory == 4) {
            TextView t = (TextView) findViewById(R.id.idInputPasswordAgain);
            t.setVisibility(View.VISIBLE);
            mEtPwdConfirm = (EditText) findViewById(R.id.idEditInputPasswordAgain);
            mEtPwdConfirm.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            SMLCommonProcess.limitEditTextPassword(mEtPwdConfirm, 8);
            mEtPwdConfirm.setVisibility(View.VISIBLE);
            mEtPwdConfirm.setOnLongClickListener(mOnLongClickListener);
        }

        Button btnConfirm = (Button) findViewById(R.id.idButtonConfirm);
        // Yu for ICS
        btnConfirm.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                // whether some lock category is disabled?
                // make sure the password's length is correct
                // make sure the length of inputed password is 4 to 8
                Log.i(TAG, "mClickFlag: " + mClickFlag);
                if (mClickFlag) {
                    return;
                } else {
                    mClickFlag = true;
                }
                if ((mEtPwd.getText().length() < 4) || ((mEtPwd.getText().length() > 8))) {
                    showDialog(DIALOG_PASSWORDLENGTHINCORRECT);
                    return;
                }
                if (mLockCategory == 0 || mLockCategory == 1 || mLockCategory == 2
                        || mLockCategory == 3 || mLockCategory == 4) {
                    if ((mEtPwd.getText().toString()
                            .equals(mEtPwdConfirm.getText().toString())) == false) {
                        showDialog(DIALOG_PASSWORDWRONG);
                        return;
                    }
                }

                Message callback = Message.obtain(mHandler, LOCK_ICC_SML_COMPLETE);
                Phone phone = PhoneFactory.getPhone(mSlotId);
                phone.getIccCard().setIccNetworkLockEnabled(mLockCategory, 1,
                        mEtPwd.getText().toString(), null, null, null, callback);
            }
        });

        Button btnCancel = (Button) findViewById(R.id.idButtonCancel);
        btnCancel.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                LockSetting.this.finish();
            }
        });
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_LOCKFAIL || id == DIALOG_PASSWORDLENGTHINCORRECT
                || id == DIALOG_LOCKSUCCEED || id == DIALOG_PASSWORDWRONG) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.strAttention)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setOnKeyListener(this);

            switch (id) {
            case DIALOG_LOCKFAIL: // Fail
                builder.setMessage(R.string.strLockFail).setNegativeButton(R.string.strYes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                mEtPwd.setText("");
                                if (mEtPwdConfirm != null) {
                                    mEtPwdConfirm.setText("");
                                }
                                mClickFlag = false;
                                dialog.cancel();
                            }
                        });
                return builder.create();
            case DIALOG_PASSWORDLENGTHINCORRECT:// Length is incorrect
                builder.setMessage(R.string.strPasswordLengthIncorrect).setNegativeButton(
                        R.string.strYes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                mEtPwd.setText("");
                                if (mEtPwdConfirm != null) {
                                    mEtPwdConfirm.setText("");
                                }
                                mClickFlag = false;
                                dialog.cancel();
                            }
                        });
                return builder.create();
            case DIALOG_LOCKSUCCEED:// Succeed
                builder.setMessage(R.string.strLockSucceed).setPositiveButton(R.string.strYes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                mEtPwd.setText("");
                                if (mEtPwdConfirm != null) {
                                    mEtPwdConfirm.setText("");
                                }
                                dialog.cancel();
                                mClickFlag = false;
                                LockSetting.this.finish();
                            }
                        });
                return builder.create();
            case DIALOG_PASSWORDWRONG:// Wrong password
                builder.setMessage(R.string.str_simme_passwords_dont_match).setNegativeButton(
                        R.string.strYes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                mEtPwd.setText("");
                                if (mEtPwdConfirm != null) {
                                    mEtPwdConfirm.setText("");
                                }
                                dialog.cancel();
                                mEtPwd.requestFocus();
                                mClickFlag = false;
                            }
                        });
                return builder.create();
            default:
                break;
            }
        }
        return super.onCreateDialog(id);
    }
}
