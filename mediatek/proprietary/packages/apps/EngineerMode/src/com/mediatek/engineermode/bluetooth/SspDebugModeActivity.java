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

package com.mediatek.engineermode.bluetooth;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.Settings;

import com.mediatek.engineermode.EngineerMode;
import com.mediatek.engineermode.R;

/**
 * Do BT SSP debug mode test.
 * Only click the checkbox when the bluetooth is on
 * @author mtk54040
 *
 */
public class SspDebugModeActivity extends Activity implements OnClickListener {
    private static final String TAG = "SSPDebugMode";

    // Message ID
    private static final int OP_OPEN_BT = 1;
    private static final int OP_CLOSE_BT = 2;
    private static final int OP_OPEN_BT_FINISHED = 3;
    private static final int OP_CLOSE_BT_FINISHED = 4;

    // Dialog ID
    private static final int OPEN_BT = 11;
    private static final int CLOSE_BT = 12;
    private static final int EXIT_EM_BT = 13;

    private static final String VALUE_TRUE = "true";
    private static final String VALUE_FALSE = "false";

    private static final int SLEEP_TIME = 300;

    // UI component
    private CheckBox mChecked;

    private boolean mSspModeOn = false;
    private WorkHandler mWorkHandler = null;

    private Handler mUiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == OP_OPEN_BT_FINISHED) {
                removeDialog(OPEN_BT);
            } else if (msg.what == OP_CLOSE_BT_FINISHED) {
                removeDialog(CLOSE_BT);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.ssp_debug_mode);

        TextView tv = (TextView) findViewById(R.id.SSPDebugModetxv);
        tv.setText(Html.fromHtml(getString(R.string.SSPDebugMode)));

        mChecked = (CheckBox) findViewById(R.id.SSPDebugModeCb);
        mChecked.setOnClickListener(this);

        // 1. load the default value
        boolean isSspDebugModeOn = (Settings.Secure.getInt(getContentResolver(),
            Settings.Secure.BLUETOOTH_SSP_DEBUG_MODE, 0) == 1);
        mChecked.setChecked(isSspDebugModeOn);

        // 2. configSspDebugMode
        BluetoothAdapter.getDefaultAdapter().configSspDebugMode(isSspDebugModeOn);

        HandlerThread workThread = new HandlerThread(TAG);
        workThread.start();

        Looper looper = workThread.getLooper();
        mWorkHandler = new WorkHandler(looper);
        Log.e(TAG,"onCreate");
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Log.v("@M_" + TAG, "-->onCreateDialog");
        if (id == OPEN_BT) {
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage(getString(R.string.BT_open));
            dialog.setCancelable(false);
            dialog.setIndeterminate(true);
            Log.v("@M_" + TAG, "OPEN_BT ProgressDialog succeed");
            return dialog;
        } else if (id == EXIT_EM_BT) {
            AlertDialog dialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.Success)
                .setMessage(R.string.BT_exit_em)
                .setPositiveButton(R.string.OK,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            backToEMEntry();
                        }
                    }).create();
            return dialog;
        } else if (id == CLOSE_BT) {
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage(getString(R.string.BT_close));
            dialog.setCancelable(false);
            dialog.setIndeterminate(true);
            return dialog;
        }
        return null;
    }

    private void backToEMEntry() {
        Intent intent = new Intent(this, EngineerMode.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        showDialog(OPEN_BT);
        mWorkHandler.sendEmptyMessage(OP_OPEN_BT);
        super.onBackPressed();
    }

    @Override
    public void onClick(View v) {
        Log.e(TAG, "onClick: v=" + v + " mChecked="+ mChecked);
        if (v.equals(mChecked)) {
            int msgId = (mChecked.isChecked()) ? R.string.SetSspDebugModeEnableSuccess
                : R.string.SetSspDebugModeDisableSuccess ;
            Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.BLUETOOTH_SSP_DEBUG_MODE, mChecked.isChecked() ? 1 : 0);
            Toast.makeText(this, getString(msgId), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Deal with function request.
     *
     * @author mtk54040
     *
     */
    private final class WorkHandler extends Handler {
        private WorkHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            BluetoothAdapter adapter =
                BluetoothAdapter.getDefaultAdapter();

            if (msg.what == OP_OPEN_BT) {
                if (adapter.getState() != BluetoothAdapter.STATE_ON) {
                    // Open Bluetooth through mAdapter
                    adapter.enable();
                    while (adapter.getState() == BluetoothAdapter.STATE_TURNING_ON) {
                        Log.i("@M_" + TAG, "Bluetooth turning on ...");
                        try {
                            Thread.sleep(SLEEP_TIME);
                        } catch (InterruptedException e) {
                            Log.i("@M_" + TAG, e.getMessage());
                        }
                    }
                }
                mUiHandler.sendEmptyMessage(OP_OPEN_BT_FINISHED);
            } else if (msg.what == OP_CLOSE_BT) {
                if (adapter.getState() != BluetoothAdapter.STATE_OFF) {
                    // Cloese bluetooth
                    adapter.disable();
                    while (adapter.getState() == BluetoothAdapter.STATE_TURNING_OFF) {
                         Log.v("@M_" + TAG, "Bluetooth turning off ...");
                        try {
                            Thread.sleep(SLEEP_TIME);
                        } catch (InterruptedException e) {
                            Log.i("@M_" + TAG, e.getMessage());
                        }
                    }
                }
                mUiHandler.sendEmptyMessage(OP_CLOSE_BT_FINISHED);
            }
        }
    }
}

