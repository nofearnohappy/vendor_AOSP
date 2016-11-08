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

package com.mediatek.engineermode.rfdesense;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;

import com.mediatek.engineermode.Elog;
import com.mediatek.engineermode.FeatureSupport;
import com.mediatek.engineermode.R;
import com.mediatek.internal.telephony.ltedc.LteDcPhoneProxy;

/**
 * RfDesenseTxTestCdma Activity.
 */
public class RfDesenseTxTestCdma extends Activity {
    public static final String TAG = "TxTestBaseCdma";

    public static final String KEY_BAND = "Band_CDMA";
    public static final String KEY_MODULATION = "Modulation_CDMA";
    public static final String KEY_CHANNEL = "Channel_CDMA";
    public static final String KEY_POWER = "Power_CDMA";
    public static final String KEY_AFC = "AFC_CDMA";
    public static final String KEY_TSC = "TSC_CDMA";
    public static final String KEY_PATTERN = "Pattern_CDMA";
    public static final String KEY_STATE = "Started_CDMA";
    public static final int STATE_NONE = 0;
    public static final int STATE_STARTED = 1;
    public static final int STATE_PAUSED = 2;
    public static final int STATE_STOPPED = 3;

    private static final int START = 1;
    private static final int PAUSE = 2;
    private static final int REBOOT = 3;
    private static final int UPDATE_BUTTON = 4;
    private static final String MD1 = "MD1";
    private static final String MD3 = "MD3";

    private static final int REBOOT_DONE = 5;
    private static final String[][] REBOOT_CMD = new String[][] {
        {"AT+EFUN=0", MD1},
        {"AT+CPOF", MD3},
        {"AT+EMDSTATUS=1,1", MD1},
        {"AT+EFUN=1", MD1},
        {"AT+EMDSTATUS=1,1", MD3},
        {"AT+CPON", MD3}};

    private static final int UPDATE_DELAY = 1000;

    private static final int CHANNEL_DEFAULT = 0;
    private static final int CHANNEL_MIN = 1;
    private static final int CHANNEL_MAX = 2;
    private static final int CHANNEL_MIN2 = 3;
    private static final int CHANNEL_MAX2 = 4;
    private static final int POWER_DEFAULT = 5;
    private static final int POWER_MIN = 6;
    private static final int POWER_MAX = 7;

    private Spinner mBand;
    private RadioGroup mModulation;
    private EditText mChannel;
    private EditText mPower;
    private Button mButtonStart;
    private Button mButtonPause;
    private Button mButtonStop;

    private Phone mPhone;
    private Phone mCdmaPhone;
    private int mState = STATE_NONE;

    private int mCurrentBand = -1;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String command = "";
            switch (msg.what) {
            case START:
                command = "START Command";
                break;
            case PAUSE:
                command = "PAUSE Command";
                break;
            case REBOOT:
                command = "REBOOT Command";
                break;
            default:
            }

            AsyncResult ar = null;
            String text = null;
            switch (msg.what) {
            case START:
            case PAUSE:
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    text = command + " success.";
                } else {
                    text = command + " failed.";
                }
                Toast.makeText(RfDesenseTxTestCdma.this, text, Toast.LENGTH_SHORT).show();
                break;
            case REBOOT:
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    text = command + " success.";
                } else {
                    text = command + " failed.";
                }

                int rebootStep = msg.arg1;
                if (msg.arg1 >= REBOOT_DONE) {
                    Toast.makeText(RfDesenseTxTestCdma.this, text, Toast.LENGTH_SHORT).show();
                    RfDesenseTxTestCdma.this.finish();
                } else {
                    rebootStep++;
                    if (ar.exception == null) {
                        sendAtCommand(REBOOT_CMD[rebootStep][0], "", REBOOT, rebootStep,
                                REBOOT_CMD[rebootStep][1]);
                    } else {
                        Toast.makeText(RfDesenseTxTestCdma.this, text, Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case UPDATE_BUTTON:
                updateButtons();
                break;
            default:
                break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rf_desense_tx_test_cdma);

        mBand = (Spinner) findViewById(R.id.band_spinner);
        mModulation = (RadioGroup)  findViewById(R.id.modulation_radio_group);
        mChannel = (EditText) findViewById(R.id.channel_editor);
        mPower = (EditText) findViewById(R.id.power_editor);
        mButtonStart = (Button) findViewById(R.id.button_start);
        mButtonPause = (Button) findViewById(R.id.button_pause);
        mButtonStop = (Button) findViewById(R.id.button_stop);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.rf_desense_tx_test_cdma_band,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mBand.setAdapter(adapter);

        final String[] bandValues =
                getResources().getStringArray(R.array.rf_desense_tx_test_cdma_band_values);

        Button.OnClickListener listener = new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                case R.id.button_start:
                    if (!checkValues()) {
                        disableAllButtons();
                        mHandler.sendMessageDelayed(Message.obtain(mHandler, UPDATE_BUTTON), 1000);
                        break;
                    }

                    String band = bandValues[mBand.getSelectedItemPosition()];
                    long modulation = mModulation.getCheckedRadioButtonId();
                    String channel = mChannel.getText().toString();
                    String power = mPower.getText().toString();
                    String command = "AT+ECRFTX=1," + channel + "," + band + "," + power + ","
                                         + (modulation == R.id.modulation_1x ? 0 : 1);
                    sendAtCommand(command, "", START);
                    disableAllButtons();
                    mHandler.sendMessageDelayed(Message.obtain(mHandler, UPDATE_BUTTON), 1000);
                    mState = STATE_STARTED;
                    break;
                case R.id.button_pause:
                    sendAtCommand("AT+ECRFTX=0", "", PAUSE);
                    disableAllButtons();
                    mHandler.sendMessageDelayed(Message.obtain(mHandler, UPDATE_BUTTON), 1000);
                    mState = STATE_PAUSED;
                    break;
                case R.id.button_stop:
                    showDialog(REBOOT);
                    break;
                default:
                    break;
                }
            }
        };

        mButtonStart.setOnClickListener(listener);
        mButtonPause.setOnClickListener(listener);
        mButtonStop.setOnClickListener(listener);

        RadioGroup.OnCheckedChangeListener radioListener
                = new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // TODO: do nothing currently
            }
        };

        mModulation.setOnCheckedChangeListener(radioListener);

        AdapterView.OnItemSelectedListener l = new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (mCurrentBand != mBand.getSelectedItemPosition()) {
                    mCurrentBand = mBand.getSelectedItemPosition();
                    // TODO: do nothing currently
                }
            }

            public void onNothingSelected(AdapterView<?> arg0) {

            }
        };

        mBand.setOnItemSelectedListener(l);

        SharedPreferences pref = getSharedPreferences(RfDesenseTxTest.PREF_FILE,
                MODE_WORLD_READABLE | MODE_WORLD_WRITEABLE);
        mBand.setSelection(pref.getInt(KEY_BAND, 0));
        mModulation.check(pref.getInt(KEY_MODULATION, R.id.modulation_1x));
        mChannel.setText(pref.getString(KEY_CHANNEL, ""));
        mPower.setText(pref.getString(KEY_POWER, ""));
        mState = pref.getInt(KEY_STATE, STATE_NONE);
        updateButtons();

        if (!FeatureSupport.isSupported(FeatureSupport.FK_MTK_C2K_SUPPORT)) {
            // Should not be here
            finish();
            return;
        }

        mCdmaPhone = PhoneFactory.getPhone(PhoneConstants.SIM_ID_1);
        if ((FeatureSupport.isSupported(FeatureSupport.FK_MTK_SVLTE_SUPPORT)
             || FeatureSupport.isSupported(FeatureSupport.FK_SRLTE_SUPPORT))
                && mCdmaPhone instanceof LteDcPhoneProxy) {
            mPhone = ((LteDcPhoneProxy) mCdmaPhone).getLtePhone();
        } else if (FeatureSupport.isSupported(FeatureSupport.FK_EVDO_DT_SUPPORT)
                && mCdmaPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
            mPhone = PhoneFactory.getPhone(PhoneConstants.SIM_ID_2);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateButtons();
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences.Editor pref = getSharedPreferences(RfDesenseTxTest.PREF_FILE,
                MODE_WORLD_READABLE | MODE_WORLD_WRITEABLE).edit();
        if (mState != STATE_STOPPED) {
            pref.putInt(KEY_BAND, mBand.getSelectedItemPosition());
            pref.putInt(KEY_MODULATION, mModulation.getCheckedRadioButtonId());
            pref.putString(KEY_CHANNEL, mChannel.getText().toString());
            pref.putString(KEY_POWER, mPower.getText().toString());
            pref.putInt(KEY_STATE, mState);
        } else {
            pref.clear();
        }
        pref.commit();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == REBOOT) {
            OnClickListener listener = new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        int rebootStep = 0;
                        sendAtCommand(REBOOT_CMD[rebootStep][0], "", REBOOT, rebootStep,
                                REBOOT_CMD[rebootStep][1]);
                        disableAllButtons();
                        mState = STATE_STOPPED;
                    }
                    dialog.dismiss();
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            return builder.setTitle("Reboot")
                          .setMessage("Reboot modem?")
                          .setPositiveButton("Reboot", listener)
                          .setNegativeButton("Cancel", listener)
                          .create();
        }
        return super.onCreateDialog(id);
    }

    protected void sendAtCommand(String str1, String str2, int what, int arg1, String whichModem) {
        String[] cmd = new String[2];
        cmd[0] = str1;
        cmd[1] = str2;
        Elog.i(TAG, "send: " + cmd[0] + " to " + whichModem);
        if (whichModem.equals(MD1)) {
            if (mPhone != null) {
                mPhone.invokeOemRilRequestStrings(cmd, mHandler
                        .obtainMessage(what, arg1, 0));
            }
        } else {
            if (mCdmaPhone != null) {
                mCdmaPhone.invokeOemRilRequestStrings(cmd, mHandler
                        .obtainMessage(what, arg1, 0));
            }
        }
    }

    protected void sendAtCommand(String str1, String str2, int what) {
        sendAtCommand(str1, str2, what, 0, MD3);
    }

    protected boolean checkValues() {
        return true;
    }

    protected void disableAllButtons() {
        mButtonStart.setEnabled(false);
        mButtonPause.setEnabled(false);
        mButtonStop.setEnabled(false);
    }

    protected void updateButtons() {
        mButtonStart.setEnabled(mState == STATE_NONE || mState == STATE_PAUSED);
        mButtonPause.setEnabled(mState == STATE_STARTED);
        mButtonStop.setEnabled(mState != STATE_NONE);
    }
}
