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
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.android.internal.telephony.IccUtils;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.uicc.IccConstants;
import com.android.internal.telephony.uicc.IccFileHandler;

public class CPAddLockSetting extends SimLockBaseActivity {
    private static final String TAG = "SIMMELOCK";
    private static final int DIALOG_MCCMNCLENGTHINCORRECT = 1;
    private static final int DIALOG_ADDLOCKFAIL = 2;
    private static final int DIALOG_ADDLOCKSUCCEED = 3;
    private static final int DIALOG_PASSWORDLENGTHINCORRECT = 4;
    private static final int DIALOG_PASSWORDWRONG = 5;
    private static final int DIALOG_GID1WRONG = 6;
    private static final int DIALOG_GID2WRONG = 7;
    private static final int ADDLOCK_ICC_SML_COMPLETE = 120;
    private static final int EVENT_GET_SIM_GID1 = 36;
    private static final int EVENT_GET_SIM_GID2 = 37;

    private EditText mEtMccMnc = null;
    private EditText mEtGid1 = null;
    private EditText mEtGid2 = null;
    private EditText mEtPwd = null;
    private EditText mEtPwdConfirm = null;
    private Spinner mSpinner1;
    private Spinner mSpinner2;
    private Spinner mSpinner3;

    private String mSimMccMnc = null;
    private String mSimGid1 = null;
    private String mSimGid2 = null;
    private boolean mMccMncReadSim = false;
    private boolean mGid1ReadSim = false;
    private boolean mGid2ReadSim = false;
    private boolean mSimGid1Valid = false;
    private boolean mSimGid2Valid = false;
    private boolean mClickFlag = false;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;
            switch (msg.what) {
            case ADDLOCK_ICC_SML_COMPLETE:
                if (ar.exception != null) {
                    showDialog(DIALOG_ADDLOCKFAIL);
                } else {
                    showDialog(DIALOG_ADDLOCKSUCCEED);
                }
                break;
            case EVENT_GET_SIM_GID1:
                if (ar.exception != null) {
                    log("fail to get SIM GID1");
                    ar.exception.printStackTrace();
                    mSimGid1Valid = false;
                } else {
                    log("succeed to get SIM GID1");
                    byte[] data = (byte[]) (ar.result);
                    log("SIM GID :" + data);
                    String hexSIMGID1 = IccUtils.bytesToHexString(data);
                    if ((data[0] & 0xff) == 0xff) {
                        log("SIM GID1 not initialized");
                        mSimGid1Valid = false;
                    } else {
                        mSimGid1Valid = true;
                        try {
                            if (hexSIMGID1 != null && hexSIMGID1.length() >= 2) {
                                mSimGid1 = String.valueOf(Integer.parseInt(hexSIMGID1.substring(0, 2), 16));
                            } else {
                                mSimGid1 = String.valueOf(Integer.parseInt(hexSIMGID1, 16));
                            }
                            log("Normal SIM GID1 :" + mSimGid1);
                        } catch (NumberFormatException e) {
                            mSimGid1Valid = false;
                            log("Wrong format: " + hexSIMGID1);
                        }
                    }
                }
                break;
            case EVENT_GET_SIM_GID2:
                if (ar.exception != null) {
                    log("fail to get SIM GID2");
                    ar.exception.printStackTrace();
                    mSimGid2Valid = false;
                } else {
                    log("succeed to get SIM GID2");
                    byte[] data = (byte[]) (ar.result);
                    log("SIM GID2 :" + data);
                    String hexSIMGID2 = IccUtils.bytesToHexString(data);
                    if ((data[0] & 0xff) == 0xff) {
                        log("SIM GID2 not initialized");
                        mSimGid2Valid = false;
                    } else {
                        mSimGid2Valid = true;
                        try {
                            if (hexSIMGID2 != null && hexSIMGID2.length() >= 2) {
                                mSimGid2 = String.valueOf(Integer.parseInt(hexSIMGID2.substring(0, 2), 16));
                            } else {
                                mSimGid2 = String.valueOf(Integer.parseInt(hexSIMGID2, 16));
                            }
                            log("Normal SIM GID2 :" + mSimGid2);
                        } catch (NumberFormatException e) {
                            mSimGid2Valid = false;
                            log("Wrong format: " + hexSIMGID2);
                        }
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
        setContentView(R.layout.cpaddlocksetting);

        // set the regulation of EditText
        mEtMccMnc = (EditText) findViewById(R.id.idcpaddlockEditInputMCCMNC);
        mEtGid1 = (EditText) findViewById(R.id.idcpaddlockEditInputGID1);
        mEtGid2 = (EditText) findViewById(R.id.idcpaddlockEditInputGID2);
        mEtPwd = (EditText) findViewById(R.id.idcpaddlockEditInputPassword);
        // Let the user to choose "put in" for just read MCCMNC from the SIM
        // card
        mSpinner1 = (Spinner) findViewById(R.id.spinnercp1);
        mSpinner2 = (Spinner) findViewById(R.id.spinnercp2);
        mSpinner3 = (Spinner) findViewById(R.id.spinnercp3);
        mEtPwdConfirm = (EditText) findViewById(R.id.idcpaddlockEditInputPasswordAgain);

        mEtMccMnc.setOnLongClickListener(mOnLongClickListener);
        mEtGid1.setOnLongClickListener(mOnLongClickListener);
        mEtGid2.setOnLongClickListener(mOnLongClickListener);
        mEtPwd.setOnLongClickListener(mOnLongClickListener);
        mEtPwdConfirm.setOnLongClickListener(mOnLongClickListener);

        // Press the CONFIRM Button
        Button btnConfirm = (Button) findViewById(R.id.idcpaddlockButtonConfirm);
        Button btnCancel = (Button) findViewById(R.id.idcpaddlockButtonCancel);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.Input_mode, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner1.setAdapter(adapter);
        AdapterView.OnItemSelectedListener l = new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (arg2 == 0) {
                    mEtMccMnc.setVisibility(View.VISIBLE);
                    // set the regulation of EditText
                    SMLCommonProcess.limitEditText(mEtMccMnc, 6);
                    mMccMncReadSim = false;
                } else {
                    mMccMncReadSim = true;
                    mEtMccMnc.setVisibility(View.GONE);
                }
            }

            public void onNothingSelected(AdapterView<?> arg0) {

            }
        };
        mSpinner1.setOnItemSelectedListener(l);

        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(this,
                R.array.Input_mode, android.R.layout.simple_spinner_item);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner2.setAdapter(adapter2);
        AdapterView.OnItemSelectedListener l2 = new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (arg2 == 0) {
                    mEtGid1.setVisibility(View.VISIBLE);
                    // set the regulation of EditText
                    SMLCommonProcess.limitEditText(mEtGid1, 3);
                    mGid1ReadSim = false;
                    mSimGid1Valid = false;
                } else {
                    mGid1ReadSim = true;
                    mSimGid1Valid = false;
                    mEtGid1.setVisibility(View.GONE);
                }
            }

            public void onNothingSelected(AdapterView<?> arg0) {

            }
        };
        mSpinner2.setOnItemSelectedListener(l2);

        ArrayAdapter<CharSequence> adapter3 = ArrayAdapter.createFromResource(this,
                R.array.Input_mode, android.R.layout.simple_spinner_item);
        adapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner3.setAdapter(adapter3);
        AdapterView.OnItemSelectedListener l3 = new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (arg2 == 0) {
                    mEtGid2.setVisibility(View.VISIBLE);
                    // set the regulation of EditText
                    SMLCommonProcess.limitEditText(mEtGid2, 3);
                    mGid2ReadSim = false;
                    mSimGid2Valid = false;
                } else {
                    mGid2ReadSim = true;
                    mSimGid2Valid = false;
                    mEtGid2.setVisibility(View.GONE);
                }
            }

            public void onNothingSelected(AdapterView<?> arg0) {

            }
        };
        mSpinner3.setOnItemSelectedListener(l3);

        mEtPwd.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        SMLCommonProcess.limitEditTextPassword(mEtPwd, 8);

        mEtPwdConfirm.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        SMLCommonProcess.limitEditTextPassword(mEtPwdConfirm, 8);

        // Yu for ICS
        btnConfirm.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                log("mClickFlag: " + mClickFlag);
                if (mClickFlag) {
                    return;
                } else {
                    mClickFlag = true;
                }
                if ((!mMccMncReadSim)
                        && ((5 > mEtMccMnc.getText().length())
                            || (6 < mEtMccMnc.getText().length()))) {
                    showDialog(DIALOG_MCCMNCLENGTHINCORRECT);
                } else if (mMccMncReadSim
                        && ((mSimMccMnc == null) || (5 > mSimMccMnc.length())
                            || (6 < mSimMccMnc.length()))) {
                    showDialog(DIALOG_MCCMNCLENGTHINCORRECT);
                } else if ((!mGid1ReadSim) && mEtGid1.getText().length() < 1) {
                    showDialog(DIALOG_GID1WRONG);
                } else if ((!mGid2ReadSim) && mEtGid2.getText().length() < 1) {
                    showDialog(DIALOG_GID2WRONG);
                } else if ((!mGid1ReadSim) && ((Integer.parseInt(mEtGid1.getText().toString()) < 0)
                        || (Integer.parseInt(mEtGid1.getText().toString()) > 254))) {
                    showDialog(DIALOG_GID1WRONG);
                } else if ((!mGid2ReadSim) && ((Integer.parseInt(mEtGid2.getText().toString()) < 0)
                        || (Integer.parseInt(mEtGid2.getText().toString()) > 254))) {
                    showDialog(DIALOG_GID2WRONG);
                } else if (mGid1ReadSim && !mSimGid1Valid) {
                    showDialog(DIALOG_GID1WRONG);
                } else if (mGid2ReadSim && !mSimGid2Valid) {
                    showDialog(DIALOG_GID2WRONG);
                } else if ((mEtPwd.getText().length() < 4) || ((mEtPwd.getText().length() > 8))) {
                    showDialog(DIALOG_PASSWORDLENGTHINCORRECT);
                } else if (!mEtPwd.getText().toString()
                        .equals(mEtPwdConfirm.getText().toString())) {
                    showDialog(DIALOG_PASSWORDWRONG);
                } else {
                    Message callback = Message.obtain(mHandler, ADDLOCK_ICC_SML_COMPLETE);
                    setIccNetworkLockEnabled(3, 2, mEtPwd.getText().toString(),
                            mMccMncReadSim ? mSimMccMnc : mEtMccMnc.getText().toString(),
                            mGid1ReadSim ? mSimGid1 : mEtGid1.getText().toString(),
                            mGid2ReadSim ? mSimGid2 : mEtGid2.getText().toString(), callback);
                }
            }
        });

        // Press the CANCEL Button
        btnCancel.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                // do nothing to quit the edit page
                CPAddLockSetting.this.finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isSimReady()) {
            log("Add NP lock fail : SIM not ready!");
            return;
        }

        // To get the MCC+MNC+GID1+GID2 from SIM card
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        mSimMccMnc = telephonyManager.getSimOperator(mSubId);
        log("getSimOperator(" + mSubId + "): " + mSimMccMnc);

        Phone phone = PhoneFactory.getPhone(mSlotId);
        IccFileHandler iccFh = ((PhoneProxy) phone).getIccFileHandler();
        if (iccFh != null) {
            iccFh.loadEFTransparent(IccConstants.EF_GID1,
                    mHandler.obtainMessage(EVENT_GET_SIM_GID1));
            iccFh.loadEFTransparent(IccConstants.EF_GID2,
                    mHandler.obtainMessage(EVENT_GET_SIM_GID2));
        }

        // To get the GID2 from SIM card //TEMP
        mSimGid2 = telephonyManager.getSimOperatorNameForSubscription(mSubId);
        log("getSimOperatorName(" + mSubId + "): " + mSimGid2);
        if (mSimGid2 == null) {
            log("Fail to read SIM GID2!");
        } else {
            log("[Gemini]Succeed to read SIM GID2. SIM GID2 is " + mSimGid2);
        }

        if (mSimMccMnc == null) {
            log("Fail to read SIM MCC+MNC!");
        } else {
            log("Read SIM MCC+MNC: " + mSimMccMnc);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setCancelable(false)
            .setTitle(R.string.strAttention)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setOnKeyListener(this);

        switch (id) {
        case DIALOG_ADDLOCKFAIL: // Fail
            builder.setMessage(R.string.strAddLockFail).setPositiveButton(R.string.strYes,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            mEtGid1.setText("");
                            mEtGid2.setText("");
                            mEtMccMnc.setText("");
                            mEtPwd.setText("");
                            mEtPwdConfirm.setText("");
                            dialog.cancel();
                            mClickFlag = false;
                        }
                    });
            return builder.create();
        case DIALOG_PASSWORDLENGTHINCORRECT:// Length is incorrect
            builder.setMessage(R.string.strPasswordLengthIncorrect).setPositiveButton(
                    R.string.strYes,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            mEtPwd.setText("");
                            mEtPwdConfirm.setText("");
                            dialog.cancel();
                            mClickFlag = false;
                        }
                    });
            return builder.create();
        case DIALOG_MCCMNCLENGTHINCORRECT:// Length is incorrect
            builder.setMessage(R.string.strMCCMNCLengthIncorrect).setPositiveButton(R.string.strYes,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            mEtMccMnc.setText("");
                            dialog.cancel();
                            mClickFlag = false;
                        }
                    });
            return builder.create();
        case DIALOG_ADDLOCKSUCCEED:// Succeed
            builder.setMessage(R.string.strAddLockSucceed).setPositiveButton(R.string.strYes,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            mClickFlag = false;
                            CPAddLockSetting.this.finish();
                        }
                    });
            return builder.create();
        case DIALOG_GID1WRONG:// Wrong GID1
            builder.setMessage(R.string.strGID1WRONG).setPositiveButton(R.string.strYes,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            mEtGid1.setText("");
                            dialog.cancel();
                            mClickFlag = false;
                        }
                    });
            return builder.create();
        case DIALOG_GID2WRONG:// Wrong GID2
            builder.setMessage(R.string.strGID2WRONG).setPositiveButton(R.string.strYes,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            mEtGid2.setText("");
                            dialog.cancel();
                            mClickFlag = false;
                        }
                    });
            return builder.create();
        case DIALOG_PASSWORDWRONG:// Wrong password
            builder.setMessage(R.string.str_simme_passwords_dont_match).setPositiveButton(
                    R.string.strYes,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            mEtPwd.setText("");
                            mEtPwdConfirm.setText("");
                            dialog.cancel();
                            mClickFlag = false;
                        }
                    });
            return builder.create();
        default:
            break;
        }
        return super.onCreateDialog(id);
    }
}
