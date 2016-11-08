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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;

import com.mediatek.engineermode.Elog;
import com.mediatek.engineermode.FeatureSupport;
import com.mediatek.engineermode.R;
import com.mediatek.internal.telephony.ltedc.LteDcPhoneProxy;

public class RfDesenseTxTestLte extends Activity implements Button.OnClickListener,
        CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "TxTestLte";
    private static final int START = 1;
    private static final int PAUSE = 2;
    private static final int REBOOT = 3;
    private static final int UPDATE_BUTTON = 4;
    private static final int WAIT_REBOOT = 5;
    private static final int UPDATE_DELAY = 1000;

    private static final int DUPLEX_TDD = 0;
    private static final int DUPLEX_FDD = 1;
    private static final int FDD_BAND_MIN = 1;
    private static final int FDD_BAND_MAX = 31;
    private static final int TDD_BAND_MIN = 33;
    private static final int TDD_BAND_MAX = 44;
    private static final int TDD_CONFIG_MAX = 6;
    private static final int TDD_SPECIAL_MAX = 9;
    private static final int VRB_START_MIN = 0;
    private static final int VRB_START_MAX = 99;
    private static final int VRB_LENGTH_MIN = 1;
    private static final int VRB_LENGTH_MAX = 100;
    private static final int POWER_MIN = -50;
    private static final int POWER_MAX = 23;

    private static final int DEFAULT_DUPLEX = 1;
    private static final int DEFAULT_BAND = 2;
    private static final int DEFAULT_BAND_WIDTH = 0;
    private static final String DEFAULT_FREQ = "17475";
    private static final int DEFAULT_TDD_CONFIG = 0;
    private static final int DEFAULT_TDD_SPECIAL = 0;
    private static final String DEFAULT_VRB_START = "0";
    private static final String DEFAULT_VRB_LENGTH = "1";
    private static final int DEFAULT_MCS = 0;
    private static final String DEFAULT_POWER = "5";

    private static final String KEY_DUPLEX = "duplex";
    private static final String KEY_BAND = "band";
    private static final String KEY_BAND_WIDTH = "bandwidth";
    private static final String KEY_FREQ = "freq";
    private static final String KEY_TDD_CONFIG = "tdd_config";
    private static final String KEY_TDD_SPECIAL = "tdd_special";
    private static final String KEY_VRB_START = "vrb_start";
    private static final String KEY_VRB_LENGTH = "vrb_length";
    private static final String KEY_MCS = "mcs";
    private static final String KEY_POWER = "power";
    public static final String KEY_STATE = "state";

    public static final int STATE_NONE = 0;
    public static final int STATE_STARTED = 1;
    public static final int STATE_PAUSED = 2;
    public static final int STATE_STOPPED = 3;

    private RadioButton mFdd;
    private RadioButton mTdd;
    private Spinner mBand;
    private Spinner mBandWidth;
    private EditText mFreq;
    private Spinner mTddConfig;
    private Spinner mTddSpecial;
    private EditText mVrbStart;
    private EditText mVrbLength;
    private Spinner mMcs;
    private EditText mPower;
    private TextView mFreqRange;
    private Button mButtonStart;
    private Button mButtonPause;
    private Button mButtonStop;
    private Toast mToast = null;

    private Phone mPhone;

    String[] mFreqRangeArray;
    private int mState = STATE_NONE;
    private int mCurrentBand = 0;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case START:
                if (((AsyncResult) msg.obj).exception == null) {
                    showToast("START Command succeeded.");
                } else {
                    showToast("START Command failed.");
                }
                mHandler.sendEmptyMessageDelayed(UPDATE_BUTTON, 1000);
                break;
            case PAUSE:
                if (((AsyncResult) msg.obj).exception == null) {
                    showToast("PAUSE Command succeeded.");
                } else {
                    showToast("PAUSE Command failed.");
                }
                mHandler.sendEmptyMessageDelayed(UPDATE_BUTTON, 1000);
                break;
            case REBOOT:
                if (((AsyncResult) msg.obj).exception == null) {
                    showToast("REBOOT Command succeeded.");
                } else {
                    showToast("REBOOT Command failed.");
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
        setContentView(R.layout.rf_desense_tx_test_lte);
        mFdd = (RadioButton) findViewById(R.id.fdd_radio_button);
        mTdd = (RadioButton) findViewById(R.id.tdd_radio_button);
        mFdd.setChecked(false);
        mTdd.setChecked(false);
        mBand = (Spinner) findViewById(R.id.band_spinner);
        mBandWidth = (Spinner) findViewById(R.id.bandwidth_spinner);
        mFreq = (EditText) findViewById(R.id.freq_editor);
        mTddConfig = (Spinner) findViewById(R.id.tdd_config_spinner);
        mTddSpecial = (Spinner) findViewById(R.id.tdd_special_spinner);
        mVrbStart = (EditText) findViewById(R.id.vrb_start_editor);
        mVrbLength = (EditText) findViewById(R.id.vrb_length_editor);
        mMcs = (Spinner) findViewById(R.id.mcs_spinner);
        mPower = (EditText) findViewById(R.id.power_editor);
        mFreqRange = (TextView) findViewById(R.id.freq);
        mButtonStart = (Button) findViewById(R.id.button_start);
        mButtonPause = (Button) findViewById(R.id.button_pause);
        mButtonStop = (Button) findViewById(R.id.button_stop);

        ArrayAdapter<CharSequence> adapter =
                new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mBand.setAdapter(adapter);

        adapter = ArrayAdapter.createFromResource(this, R.array.rf_desense_tx_test_lte_bandwidth,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mBandWidth.setAdapter(adapter);

        adapter = ArrayAdapter.createFromResource(this, R.array.rf_desense_tx_test_lte_mcs,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mMcs.setAdapter(adapter);

        adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mTddConfig.setAdapter(adapter);

        adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mTddSpecial.setAdapter(adapter);

        mFdd.setOnCheckedChangeListener(this);
        mTdd.setOnCheckedChangeListener(this);
        mButtonStart.setOnClickListener(this);
        mButtonPause.setOnClickListener(this);
        mButtonStop.setOnClickListener(this);

        mBand.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (arg2 != mCurrentBand) {
                    mCurrentBand = arg2;
                    setDefaultValue();
                }
            }

            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        mFreqRangeArray = getResources().getStringArray(R.array.rf_desense_tx_test_lte_freq_range);

        restoreState();
    }

    @Override
    protected void onDestroy() {
        saveState();
        super.onDestroy();
    }

    @Override
    public void onCheckedChanged(CompoundButton v, boolean checked) {
        switch (v.getId()) {
        case R.id.fdd_radio_button:
        case R.id.tdd_radio_button:
            onDuplexChange();
            break;
        default:
            break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.button_start:
            if (!checkValues()) {
                break;
            }
            String command = "AT+ERFTX=6,0,1,"
                    + (mBand.getSelectedItemPosition()
                    + (mTdd.isChecked() ? TDD_BAND_MIN : FDD_BAND_MIN)) + ","
                    + mBandWidth.getSelectedItemPosition() + ","
                    + mFreq.getText().toString() + ","
                    + (mTdd.isChecked() ? DUPLEX_TDD : DUPLEX_FDD) + ","
                    + mTddConfig.getSelectedItemPosition() + ","
                    + mTddSpecial.getSelectedItemPosition() + ","
                    + mVrbStart.getText().toString() + ","
                    + mVrbLength.getText().toString() + ","
                    + mMcs.getSelectedItemPosition() + ","
                    + mPower.getText().toString();
            sendAtCommand(command, START);
            disableAllButtons();
            registerPowerOffReceiver();
            mState = STATE_STARTED;
            break;
        case R.id.button_pause:
            sendAtCommand("AT+ERFTX=6,0,0", PAUSE);
            disableAllButtons();
            mState = STATE_PAUSED;
            break;
        case R.id.button_stop:
            showDialog(REBOOT);
            break;
        default:
            break;
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case REBOOT:
            return new AlertDialog.Builder(this).setTitle("Reboot")
                    .setMessage("Reboot modem?")
                    .setPositiveButton("Reboot", new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == DialogInterface.BUTTON_POSITIVE) {
                                sendAtCommand("AT+CFUN=1,1", REBOOT);
                                disableAllButtons();
                                unregisterPowerOffReceiver();
                                mState = STATE_STOPPED;
                                showDialog(WAIT_REBOOT);
                            }
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .create();
        case WAIT_REBOOT:
            final AlertDialog dialog =  new AlertDialog.Builder(this)
                    .setTitle("Reboot")
                    .setMessage("Wait")
                    .setCancelable(false)
                    .create();
            new CountDownTimer(10000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                   dialog.setMessage("Wait for modem reboot: " + (millisUntilFinished / 1000));
                }

                @Override
                public void onFinish() {
                    dialog.cancel();
                    finish();
                }
            }
            .start();
            return dialog;
        default:
            break;
        }
        return super.onCreateDialog(id);
    }

    private void onDuplexChange() {
        boolean tdd = mTdd.isChecked();
        int bandMin = tdd ? TDD_BAND_MIN : FDD_BAND_MIN;
        int bandMax = tdd ? TDD_BAND_MAX : FDD_BAND_MAX;
        int configMax = tdd ? TDD_CONFIG_MAX : 0;
        int specialMax = tdd ? TDD_SPECIAL_MAX : 0;
        mTddConfig.setEnabled(tdd);
        mTddSpecial.setEnabled(tdd);
        Elog.i(TAG, "tdd = " + tdd);

        ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) mBand.getAdapter();
        adapter.clear();
        for (int i = bandMin; i <= bandMax; i++) {
            adapter.add(String.valueOf(i));
        }
        adapter.notifyDataSetChanged();
        mBand.setSelection(tdd ? 0 : DEFAULT_BAND);

        adapter = (ArrayAdapter<CharSequence>) mTddConfig.getAdapter();
        adapter.clear();
        for (int i = 0; i <= configMax; i++) {
            adapter.add(String.valueOf(i));
        }
        adapter.notifyDataSetChanged();

        adapter = (ArrayAdapter<CharSequence>) mTddSpecial.getAdapter();
        adapter.clear();
        for (int i = 0; i <= specialMax; i++) {
            adapter.add(String.valueOf(i));
        }
        adapter.notifyDataSetChanged();

        setDefaultValue();
    }

    private boolean checkValues() {
        try {
            int value = Integer.parseInt(mVrbStart.getText().toString());
            if (value < VRB_START_MIN || value > VRB_START_MAX) {
                showToast("Invalid VRB Start.");
                return false;
            }
            value = Integer.parseInt(mVrbLength.getText().toString());
            if (value < VRB_LENGTH_MIN || value > VRB_LENGTH_MAX) {
                showToast("Invalid VRB Length.");
                return false;
            }
            value = Integer.parseInt(mPower.getText().toString());
            if (value < POWER_MIN || value > POWER_MAX) {
                showToast("Invalid Power Level.");
                return false;
            }
        } catch (NumberFormatException e) {
            showToast("Invalid Value.");
            return false;
        }
        return true;
    }

    private void saveState() {
        SharedPreferences.Editor editor = getSharedPreferences(RfDesenseTxTest.PREF_FILE,
                MODE_WORLD_READABLE | MODE_WORLD_WRITEABLE).edit();
        if (mState != STATE_STOPPED) {
            editor.putInt(KEY_DUPLEX, mTdd.isChecked() ? DUPLEX_TDD : DUPLEX_FDD);
            editor.putInt(KEY_BAND, mBand.getSelectedItemPosition());
            editor.putInt(KEY_BAND_WIDTH, mBandWidth.getSelectedItemPosition());
            editor.putString(KEY_FREQ, mFreq.getText().toString());
            editor.putInt(KEY_TDD_CONFIG, mTddConfig.getSelectedItemPosition());
            editor.putInt(KEY_TDD_SPECIAL, mTddSpecial.getSelectedItemPosition());
            editor.putString(KEY_VRB_START, mVrbStart.getText().toString());
            editor.putString(KEY_VRB_LENGTH, mVrbLength.getText().toString());
            editor.putInt(KEY_MCS, mMcs.getSelectedItemPosition());
            editor.putString(KEY_POWER, mPower.getText().toString());
            editor.putInt(KEY_STATE, mState);
        } else {
            editor.clear();
        }
        editor.commit();
    }

    private void restoreState() {
        SharedPreferences pref = getSharedPreferences(RfDesenseTxTest.PREF_FILE,
                MODE_WORLD_READABLE | MODE_WORLD_WRITEABLE);
        int duplex = pref.getInt(KEY_DUPLEX, DEFAULT_DUPLEX);
        mFdd.setChecked(duplex == DUPLEX_FDD);
        mTdd.setChecked(duplex == DUPLEX_TDD);
        mCurrentBand = pref.getInt(KEY_BAND, DEFAULT_BAND);
        mBand.setSelection(pref.getInt(KEY_BAND, DEFAULT_BAND));
        mBandWidth.setSelection(pref.getInt(KEY_BAND_WIDTH, DEFAULT_BAND_WIDTH));
        mFreq.setText(pref.getString(KEY_FREQ, DEFAULT_FREQ));
        mTddConfig.setSelection(pref.getInt(KEY_TDD_CONFIG, DEFAULT_TDD_CONFIG));
        mTddSpecial.setSelection(pref.getInt(KEY_TDD_SPECIAL, DEFAULT_TDD_SPECIAL));
        mVrbStart.setText(pref.getString(KEY_VRB_START, DEFAULT_VRB_START));
        mVrbLength.setText(pref.getString(KEY_VRB_LENGTH, DEFAULT_VRB_LENGTH));
        mMcs.setSelection(pref.getInt(KEY_MCS, DEFAULT_MCS));
        mPower.setText(pref.getString(KEY_POWER, DEFAULT_POWER));
        mState = pref.getInt(KEY_STATE, STATE_NONE);
        updateButtons();
    }

    private void setDefaultValue() {
        mBandWidth.setSelection(DEFAULT_BAND_WIDTH);
        mFreq.setText(String.valueOf(getDefaultFreq()));
        mTddConfig.setSelection(DEFAULT_TDD_CONFIG);
        mTddSpecial.setSelection(DEFAULT_TDD_SPECIAL);
        mVrbStart.setText(DEFAULT_VRB_START);
        mVrbLength.setText(DEFAULT_VRB_LENGTH);
        mMcs.setSelection(DEFAULT_MCS);
        mPower.setText(DEFAULT_POWER);
    }

    private int getDefaultFreq() {
        int band = mBand.getSelectedItemPosition()
                + (mTdd.isChecked() ? TDD_BAND_MIN : FDD_BAND_MIN);
        String[] range = mFreqRangeArray[band - 1].split(",");
        try {
            int min = Integer.parseInt(range[0]);
            int max = Integer.parseInt(range[1]);
            return (min + max) / 2;
        } catch (NumberFormatException e) {
            throw new RuntimeException("Check the array resource");
        }
    }

    private void disableAllButtons() {
        mButtonStart.setEnabled(false);
        mButtonPause.setEnabled(false);
        mButtonStop.setEnabled(false);
    }

    private void updateButtons() {
        mButtonStart.setEnabled(mState == STATE_NONE || mState == STATE_PAUSED);
        mButtonPause.setEnabled(mState == STATE_STARTED);
        mButtonStop.setEnabled(mState != STATE_NONE);
    }

    private void showToast(String msg) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        mToast.show();
    }

    private void sendAtCommand(String str, int what) {
        String cmd[] = new String[] {str, ""};
        Elog.i(TAG, "send: " + cmd[0]);

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

        mPhone.invokeOemRilRequestStrings(cmd, mHandler.obtainMessage(what));
    }

    private void registerPowerOffReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SHUTDOWN);
        filter.addAction("android.intent.action.ACTION_SHUTDOWN_IPO");
//        registerReceiver(mPowerOffReceiver, filter);
    }

    private void unregisterPowerOffReceiver() {
//        unregisterReceiver(mPowerOffReceiver);
    }

    private BroadcastReceiver mPowerOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SHUTDOWN.equals(intent.getAction()) ||
                    "android.intent.action.ACTION_SHUTDOWN_IPO".equals(intent.getAction())) {
                Elog.i(TAG, "reset state");
                SharedPreferences.Editor editor = getSharedPreferences(RfDesenseTxTest.PREF_FILE,
                        MODE_WORLD_READABLE | MODE_WORLD_WRITEABLE).edit();
                editor.clear();
                editor.commit();
            }
        }
    };
}
