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

/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.op01.plugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

public class WifiConnectNotifyDialog extends AlertActivity
        implements DialogInterface.OnClickListener {
    private static final String TAG = "WifiConnectNotifyDialog";
    private static final String PREF_REMIND_CONNECT = "pref_remind_connect";

    private Context mContext;
    private CheckBox mCheckbox;
    private TextView mTimeView;
    private WifiCount mWifiCount;
    private IntentFilter mIntentFilter;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(
                        WifiManager.EXTRA_NETWORK_INFO);
                NetworkInfo.DetailedState dState = info.getDetailedState();
                Log.d("@M_" + TAG, " receive action");
                if (dState == NetworkInfo.DetailedState.DISCONNECTED) {
                    Log.d("@M_" + TAG, " disconnected, finish dialog");
                    finish();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setFinishOnTouchOutside(false);
        mContext = this;
        final AlertController.AlertParams p = mAlertParams;
        p.mTitle = getString(R.string.wifi_cmcc_costs_title);
        Log.d("@M_" + TAG, " mTimeView = " + mTimeView);
        p.mView = createView();
        p.mPositiveButtonText = getString(android.R.string.ok);
        p.mPositiveButtonListener = this;
        setupAlert();
        mIntentFilter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mContext.registerReceiver(mReceiver, mIntentFilter);
    }

    private View createView() {
        Log.d("@M_" + TAG, " createView()");
        StringBuilder summary = new StringBuilder();
        summary.append(getString(R.string.wifi_cmcc_costs_msg));
        summary.append("\n");
        summary.append(String.format(getString(R.string.wifi_count_down_sec), "5"));

        View view = getLayoutInflater().inflate(R.layout.setting_notify_dialog, null);
        mTimeView = (TextView) view.findViewById(R.id.content);
        mTimeView.setText(summary.toString());
        mCheckbox = (CheckBox) view.findViewById(R.id.closeReminder);
        return view;
    }

    /* define one internal timer class */
    class WifiCount extends CountDownTimer {
        private int mTimes = 6;
        public WifiCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            Log.d("@M_" + TAG, " WifiCount onFinish()");
            mWifiCount = null;
            ((AlertActivity) mContext).finish();
        }

        @Override
        public void onTick(long millisUntilFinished) {
            Log.d("@M_" + TAG, " WifiCount onTick()");
            StringBuilder summary = new StringBuilder();
            mTimes--;
            summary.append(getString(R.string.wifi_cmcc_costs_msg));
            summary.append("\n");
            summary.append(String.format(getString(R.string.wifi_count_down_sec),
                                        String.valueOf(mTimes)));
            mTimeView.setText(summary.toString());
        }
    }

    @Override
    protected void onResume() {
        Log.d("@M_" + TAG, " onResume()");
        super.onResume();
        if (mWifiCount == null) {
            mWifiCount = new WifiCount(6000, 1000);
            mWifiCount.start();
        }
    }

    @Override
    protected void onDestroy() {
        Log.d("@M_" + TAG, " onDestroy()");
        super.onDestroy();
        if (mWifiCount != null) {
            mWifiCount.cancel();
            mWifiCount = null;
        }
        mContext.unregisterReceiver(mReceiver);
    }

    private void onPositive() {
        Log.d("@M_" + TAG, " onPositive()");
        if (mCheckbox.isChecked()) {
            SharedPreferences sh = this.getSharedPreferences(
                "wifi_connect_notify",
                this.MODE_WORLD_READABLE);
            Editor editor = sh.edit();
            editor.putBoolean(PREF_REMIND_CONNECT, false);
            editor.commit();
        }
        finish();
    }

    private void onNegative() {
        Log.d("@M_" + TAG, " onNegative()");
        finish();
    }

    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                onPositive();
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                onNegative();
                break;
            default:
                /// do nothing.
        }
    }

}
