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

package com.mediatek.engineermode.worldphone;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyIntents;

import com.mediatek.internal.telephony.ModemSwitchHandler;
import com.mediatek.internal.telephony.worldphone.IWorldPhone;
import com.mediatek.internal.telephony.worldphone.WorldPhoneUtil;
import com.mediatek.engineermode.R;

public class ModemSwitch extends Activity implements View.OnClickListener {
    private final static String TAG = "EM/ModemSwitch";

    private static final int PROJ_TYPE_NOT_SUPPORT = 0;
    private static final int PROJ_TYPE_WORLD_PHONE = 1;

    private RadioButton mRadioWg;
    private RadioButton mRadioTg;
    private RadioButton mRadioLwg;
    private RadioButton mRadioLtg;
    private RadioButton mRadioAuto;
    private TextView mText;
    private EditText mTimer;
    private Button mButtonSet;
    private Button mButtonSetTimer;
    private AlertDialog alertDialog;
    private static int sProjectType;
    private static IWorldPhone sWorldPhone = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.modem_switch);

        if (WorldPhoneUtil.isWorldPhoneSupport()) {
            Log.d("@M_" + TAG, "World Phone Project");
            sWorldPhone = PhoneFactory.getWorldPhone();
            sProjectType = PROJ_TYPE_WORLD_PHONE;
        } else {
            Log.d("@M_" + TAG, "Not Supported Project");
            sProjectType = PROJ_TYPE_NOT_SUPPORT;
        }
        mRadioWg = (RadioButton) findViewById(R.id.modem_switch_wg);
        mRadioTg = (RadioButton) findViewById(R.id.modem_switch_tg);
        mRadioLwg = (RadioButton) findViewById(R.id.modem_switch_fdd_csfb);
        mRadioLtg = (RadioButton) findViewById(R.id.modem_switch_tdd_csfb);
        mRadioAuto = (RadioButton) findViewById(R.id.modem_switch_auto);
        mButtonSet = (Button) findViewById(R.id.modem_switch_set);
        String optr = SystemProperties.get("ro.operator.optr");
        if (sProjectType == PROJ_TYPE_WORLD_PHONE) {
            if (WorldPhoneUtil.isLteSupport()) {
                mRadioWg.setVisibility(View.GONE);
                mRadioTg.setVisibility(View.GONE);
            } else {
                mRadioLwg.setVisibility(View.GONE);
                mRadioLtg.setVisibility(View.GONE);
            }
        } else if (sProjectType == PROJ_TYPE_NOT_SUPPORT) {
            mRadioWg.setVisibility(View.GONE);
            mRadioTg.setVisibility(View.GONE);
            mRadioLwg.setVisibility(View.GONE);
            mRadioLtg.setVisibility(View.GONE);
            mRadioAuto.setVisibility(View.GONE);
            mButtonSet.setVisibility(View.GONE);
        }
        mText = (TextView) findViewById(R.id.modem_switch_current_value);
        mTimer = (EditText) findViewById(R.id.modem_switch_timer);
        mButtonSet.setOnClickListener(this);
        mButtonSetTimer = (Button) findViewById(R.id.modem_switch_set_timer);
        mButtonSetTimer.setOnClickListener(this);
        alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Switching Mode");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TelephonyIntents.ACTION_MD_TYPE_CHANGE);
        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    protected void onResume() {
        Log.d("@M_" + TAG, "onResume()");
        super.onResume();

        int modemType = getModemType();
        Log.d("@M_" + TAG, "Get modem type: " + modemType);

        if (modemType == ModemSwitchHandler.MD_TYPE_WG) {
            mText.setText(R.string.modem_switch_is_wg);
            mRadioWg.setChecked(true);
        } else if (modemType == ModemSwitchHandler.MD_TYPE_TG) {
            mText.setText(R.string.modem_switch_is_tg);
            mRadioTg.setChecked(true);
        } else if (modemType == ModemSwitchHandler.MD_TYPE_LWG) {
            mText.setText(R.string.modem_switch_is_fdd_csfb);
            mRadioLwg.setChecked(true);
        } else if (modemType == ModemSwitchHandler.MD_TYPE_LTG) {
            mText.setText(R.string.modem_switch_is_tdd_csfb);
            mRadioLtg.setChecked(true);
        } else {
            mText.setText(R.string.modem_switch_current_value);
            Toast.makeText(this, "Query Modem type failed: " + modemType, Toast.LENGTH_SHORT).show();
        }

        if (Settings.Global.getInt(getContentResolver(),
                Settings.Global.WORLD_PHONE_AUTO_SELECT_MODE, IWorldPhone.SELECTION_MODE_AUTO) == IWorldPhone.SELECTION_MODE_AUTO) {
            mRadioWg.setChecked(false);
            mRadioTg.setChecked(false);
            mRadioLwg.setChecked(false);
            mRadioLtg.setChecked(false);
            mRadioAuto.setChecked(true);
        }

        int timer = Settings.Global.getInt(getContentResolver(),
                Settings.Global.WORLD_PHONE_FDD_MODEM_TIMER, 0);
        mTimer.setText(String.valueOf(timer));
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("@M_" + TAG, "[Receiver]+");
            Log.d("@M_" + TAG, "Action: " + action);
            if (TelephonyIntents.ACTION_MD_TYPE_CHANGE.equals(action)) {
                int mdType = intent.getIntExtra(TelephonyIntents.EXTRA_MD_TYPE, ModemSwitchHandler.MD_TYPE_UNKNOWN);
                Log.d("@M_" + TAG, "mdType: " + mdType);
                updateUi(mdType);
            }
            Log.d("@M_" + TAG, "[Receiver]-");
        }
    };

    @Override
    public void onClick(View v) {
        if (v == mButtonSetTimer) {
            int timer = 0;
            try {
                timer = Integer.parseInt(mTimer.getText().toString());
            } catch (NumberFormatException e) {
                Log.w("@M_" + TAG, "Invalid format: " + mTimer.getText());
                timer = 0;
            }
            Settings.Global.putInt(getContentResolver(),
                    Settings.Global.WORLD_PHONE_FDD_MODEM_TIMER, timer);
            Toast.makeText(ModemSwitch.this, "Set timer succeed.", Toast.LENGTH_SHORT).show();
            return;
        }

        int oldMdType = getModemType();
        int airplaneMode = Settings.Global.getInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);
        Log.d("@M_" + TAG, "airplaneMode: " + airplaneMode);
        if (airplaneMode == 1) {
            Toast.makeText(ModemSwitch.this, "Modem switch is not allowed in flight mode", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mRadioWg.isChecked()) {
            Log.d("@M_" + TAG, "Set modem type: " + ModemSwitchHandler.MD_TYPE_WG);
            if (sProjectType == PROJ_TYPE_WORLD_PHONE) {
                sWorldPhone.setModemSelectionMode(IWorldPhone.SELECTION_MODE_MANUAL, ModemSwitchHandler.MD_TYPE_WG);
            }
        } else if (mRadioTg.isChecked()) {
            Log.d("@M_" + TAG, "Set modem type: " + ModemSwitchHandler.MD_TYPE_TG);
            if (sProjectType == PROJ_TYPE_WORLD_PHONE) {
                sWorldPhone.setModemSelectionMode(IWorldPhone.SELECTION_MODE_MANUAL, ModemSwitchHandler.MD_TYPE_TG);
            }
        } else if (mRadioLwg.isChecked()) {
            Log.d("@M_" + TAG, "Set modem type: " + ModemSwitchHandler.MD_TYPE_LWG);
            if (sProjectType == PROJ_TYPE_WORLD_PHONE) {
                sWorldPhone.setModemSelectionMode(IWorldPhone.SELECTION_MODE_MANUAL, ModemSwitchHandler.MD_TYPE_LWG);
            }
        } else if (mRadioLtg.isChecked()) {
            Log.d("@M_" + TAG, "Set modem type: " + ModemSwitchHandler.MD_TYPE_LTG);
            if (sProjectType == PROJ_TYPE_WORLD_PHONE) {
                sWorldPhone.setModemSelectionMode(IWorldPhone.SELECTION_MODE_MANUAL, ModemSwitchHandler.MD_TYPE_LTG);
            }
        } else if (mRadioAuto.isChecked()) {
            Log.d("@M_" + TAG, "Set modem type: auto");
            if (sProjectType == PROJ_TYPE_WORLD_PHONE) {
                sWorldPhone.setModemSelectionMode(IWorldPhone.SELECTION_MODE_AUTO, 0);
            }
        } else {
            return;
        }
        Toast.makeText(ModemSwitch.this, "Please Wait", Toast.LENGTH_SHORT).show();
        /*int newMdType = getModemType();
        Log.d("@M_" + TAG, "oldMdType: " + oldMdType + ", newMdType: " + newMdType);
        if (newMdType == ModemSwitchHandler.MD_TYPE_WG) {
            mText.setText(R.string.modem_switch_is_wg);
        } else if (newMdType == ModemSwitchHandler.MD_TYPE_TG) {
            mText.setText(R.string.modem_switch_is_tg);
        } else if (newMdType == ModemSwitchHandler.MD_TYPE_LWG) {
            mText.setText(R.string.modem_switch_is_fdd_csfb);
        } else if (newMdType == ModemSwitchHandler.MD_TYPE_LTG) {
            mText.setText(R.string.modem_switch_is_tdd_csfb);
        }
        if (oldMdType != newMdType) {
            switchModemAlert(10000, 1000);
        } else {
            Toast.makeText(ModemSwitch.this, "Switch not executed", Toast.LENGTH_SHORT).show();
        }*/
    }

    private void switchModemAlert(long millisUntilFinished, long countDownInterval) {
        alertDialog.setMessage("Wait");
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();

        new CountDownTimer(millisUntilFinished, countDownInterval) {
            @Override
            public void onTick(long millisUntilFinished) {
               alertDialog.setMessage("Wait " + (millisUntilFinished / 1000) + " seconds");
            }

            @Override
            public void onFinish() {
                alertDialog.cancel();
            }
        } .start();
    }

    private void updateUi(int mdType) {
        if (mdType == ModemSwitchHandler.MD_TYPE_WG) {
            mText.setText(R.string.modem_switch_is_wg);
        } else if (mdType == ModemSwitchHandler.MD_TYPE_TG) {
            mText.setText(R.string.modem_switch_is_tg);
        } else if (mdType == ModemSwitchHandler.MD_TYPE_LWG) {
            mText.setText(R.string.modem_switch_is_fdd_csfb);
        } else if (mdType == ModemSwitchHandler.MD_TYPE_LTG) {
            mText.setText(R.string.modem_switch_is_tdd_csfb);
        }
    }

    private int getModemType() {
        return ModemSwitchHandler.getActiveModemType();
    }
}
