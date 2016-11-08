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
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;

import com.mediatek.engineermode.Elog;
import com.mediatek.engineermode.FeatureSupport;
import com.mediatek.engineermode.ModemCategory;
import com.mediatek.engineermode.R;
import com.mediatek.internal.telephony.ltedc.LteDcPhoneProxy;

import java.util.ArrayList;

public class RfDesenseTxTest extends Activity implements OnItemClickListener {
    public static final String TAG = "RfDesenseTxTest";

    public static final String PREF_FILE = "tx_test";
    private static final String KEY_REBOOT = "rebooted";

    private static final int DIALOG_CONFIRM = 0;
    private static final int FLIGHT_MODE = 0;

    ArrayList<String> mItems;
    private int mPosition = 0;

    private ProgressDialog mIndicator = null;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == FLIGHT_MODE) {
                if (mIndicator != null) {
                    mIndicator.dismiss();
                }

                AsyncResult ar = (AsyncResult) msg.obj;
                String text;
                if (ar.exception == null) {
                    start();
                    text = "Enter flight mode.";
                } else {
                    text = "Failed to enter flight mode.";
                }

                Toast.makeText(RfDesenseTxTest.this, text, Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rf_desense_tx_test);

        ListView simTypeListView = (ListView) findViewById(R.id.list);

        mItems = new ArrayList<String>();
        mItems.add(getString(R.string.rf_desense_tx_test_gsm));
        if (ModemCategory.getModemType() == ModemCategory.MODEM_TD) {
            mItems.add(getString(R.string.rf_desense_tx_test_td));
        } else if (ModemCategory.getModemType() == ModemCategory.MODEM_FDD) {
            mItems.add(getString(R.string.rf_desense_tx_test_fd));
        }
        if (FeatureSupport.isSupported(FeatureSupport.FK_LTE_SUPPORT)) {
            mItems.add(getString(R.string.rf_desense_tx_test_lte));
        }
        if (ModemCategory.isCdma()) {
            mItems.add(getString(R.string.rf_desense_tx_test_cdma));
        }
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mItems);
        simTypeListView.setAdapter(adapter);
        simTypeListView.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        SharedPreferences pref = getSharedPreferences(PREF_FILE,
                MODE_WORLD_READABLE | MODE_WORLD_WRITEABLE);
        if (pref.getBoolean(KEY_REBOOT, false)) {
            resetSettings();
        }

        mPosition = position;

        int stateGsm = pref.getInt(RfDesenseTxTestGsm.KEY_STATE, RfDesenseTxTestGsm.STATE_NONE);
        int stateTd = pref.getInt(RfDesenseTxTestTd.KEY_STATE, RfDesenseTxTestTd.STATE_NONE);
        int stateLte = pref.getInt(RfDesenseTxTestLte.KEY_STATE, RfDesenseTxTestLte.STATE_NONE);
        int stateCdma = pref.getInt(RfDesenseTxTestCdma.KEY_STATE, RfDesenseTxTestCdma.STATE_NONE);

        if (stateGsm == RfDesenseTxTestGsm.STATE_NONE && stateTd == RfDesenseTxTestTd.STATE_NONE
                 && stateLte == RfDesenseTxTestLte.STATE_NONE
                 && stateCdma == RfDesenseTxTestCdma.STATE_NONE) {
            showDialog(DIALOG_CONFIRM);
        } else {
            start();
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_CONFIRM:
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    if (whichButton == DialogInterface.BUTTON_POSITIVE) {
                        enterFlightMode();
                    }
                    dialog.dismiss();
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            return builder.setTitle("TX Test")
                          .setMessage("Force entering flight mode!")
                          .setPositiveButton("OK", listener)
                          .setNegativeButton("Cancel", listener)
                          .create();

        default:
            break;
        }
        return null;
    }

    private void start() {
        Intent intent = new Intent();
        String item = mItems.get(mPosition);
        if (item.equals(getString(R.string.rf_desense_tx_test_gsm))) {
            intent.setClass(this, RfDesenseTxTestGsm.class);
            this.startActivity(intent);
        } else if (item.equals(getString(R.string.rf_desense_tx_test_fd))
                || item.equals(getString(R.string.rf_desense_tx_test_td))) {
            intent.setClass(this, RfDesenseTxTestTd.class);
            this.startActivity(intent);
        } else if (item.equals(getString(R.string.rf_desense_tx_test_lte))) {
            intent.setClass(this, RfDesenseTxTestLte.class);
            this.startActivity(intent);
        } else if (item.equals(getString(R.string.rf_desense_tx_test_cdma))) {
            intent.setClass(this, RfDesenseTxTestCdma.class);
            this.startActivity(intent);
        }
    }

    private void enterFlightMode() {
        String[] cmd = new String[2];
        cmd[0] = "AT+CFUN=4";
        cmd[1] = "";
        Elog.i(TAG, "send: " + cmd[0]);

        Phone phone = null;
        if (TelephonyManager.getDefault().getPhoneCount() > 1) {
            phone = PhoneFactory.getPhone(PhoneConstants.SIM_ID_1);
        } else {
            phone = PhoneFactory.getDefaultPhone();
        }

        if (!mItems.get(mPosition).equals(getString(R.string.rf_desense_tx_test_cdma))) {
            if (FeatureSupport.isSupported(FeatureSupport.FK_MTK_C2K_SUPPORT)) {
                if ((FeatureSupport.isSupported(FeatureSupport.FK_MTK_SVLTE_SUPPORT)
                     || FeatureSupport.isSupported(FeatureSupport.FK_SRLTE_SUPPORT))
                        && phone instanceof LteDcPhoneProxy) {
                    phone = ((LteDcPhoneProxy) phone).getLtePhone();
                }
                if (FeatureSupport.isSupported(FeatureSupport.FK_EVDO_DT_SUPPORT)
                        && phone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
                    phone = PhoneFactory.getPhone(PhoneConstants.SIM_ID_2);
                }
            }
        }

        phone.invokeOemRilRequestStrings(cmd, mHandler.obtainMessage(FLIGHT_MODE));
        mIndicator = new ProgressDialog(this);
        if (mIndicator != null) {
            mIndicator.setMessage("Enter flight mode");
            mIndicator.show();
        }
    }

    private void resetSettings() {
        SharedPreferences.Editor pref = getSharedPreferences(PREF_FILE,
                MODE_WORLD_READABLE | MODE_WORLD_WRITEABLE).edit();
        pref.clear();
        pref.commit();
    }
}
