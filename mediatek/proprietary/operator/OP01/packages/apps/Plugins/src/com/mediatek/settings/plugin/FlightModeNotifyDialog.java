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

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.CheckBox;
import android.provider.Settings.System;
import android.util.Log;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

public class FlightModeNotifyDialog extends AlertActivity implements DialogInterface.OnClickListener {
    private static final String TAG = "FlightModeNotifyDialog";
    private static final String PREF_REMIND = "pref_remind";

    private Context mContext;
    private CheckBox mCheckbox;

    private static void log(String msg) {
        Log.d("@M_" + TAG, msg);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setFinishOnTouchOutside(false);
        mContext = this;

        final AlertController.AlertParams p = mAlertParams;
        p.mTitle = getString(R.string.flight_mode_title);
        p.mView = createView();
        p.mPositiveButtonText = getString(android.R.string.ok);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(android.R.string.cancel);
        p.mNegativeButtonListener = this;
        setupAlert();
        registerAirplaneModeObserver();
    }

    private View createView() {
        log("createView");
        View view = getLayoutInflater().inflate(R.layout.flight_mode_notify_dialog, null);
        TextView contentView = (TextView) view.findViewById(R.id.content);
        contentView.setText(getString(R.string.msg_flight_mode_notify));
        mCheckbox = (CheckBox) view.findViewById(R.id.closeReminder);
        return view;
    }

    public void registerAirplaneModeObserver() {
        log("registerObserver()");
        mContext.getContentResolver().registerContentObserver(
                System.getUriFor(System.AIRPLANE_MODE_ON), true,
                mAirplaneModeObserver);
    }

    public void unRegisterAirplaneObserver() {
        log("unRegisterObserver()");
        mContext.getContentResolver().unregisterContentObserver(mAirplaneModeObserver);
    }

    private ContentObserver mAirplaneModeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            log("onChange()");
            boolean state = System.getInt(mContext.getContentResolver(),
                System.AIRPLANE_MODE_ON, 0) != 0;
            if (!state) {
                finish();
            }
        }
    };

    @Override
    protected void onDestroy() {
        log("onDestroy()");
        unRegisterAirplaneObserver();
        super.onDestroy();
    }

    private void onPositive() {
        log("onOK");
        if (mCheckbox.isChecked()) {
            SharedPreferences sh = this.getSharedPreferences("flight_mode_notify", this.MODE_WORLD_READABLE);
            Editor editor = sh.edit();
            editor.putBoolean(PREF_REMIND, false);
            editor.commit();
        }
        finish();
    }

    private void onNegative() {
        log("onCancel");
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
