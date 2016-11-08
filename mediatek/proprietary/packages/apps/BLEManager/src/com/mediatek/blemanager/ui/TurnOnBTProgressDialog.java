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
 * MediaTek Inc. (C) 2014. All rights reserved.
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
package com.mediatek.blemanager.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.mediatek.blemanager.R;
import com.mediatek.blemanager.common.LocalBleManager;
import com.mediatek.blemanager.provider.BleConstants;

public class TurnOnBTProgressDialog {
    private static final String TAG = BleConstants.COMMON_TAG + "[TurnOnBTProgressDialog]";

    private static final int SHOW_TIMEOUT = 30 * 1000; // 30s

    private static final int SHOW_DIALOG = 1;
    private static final int DISMISS_DIALOG = 2;
    private static final int SHOW_TIMEOUT_FLAG = 3;

    private ProgressDialog mProDialog;
    private static TurnOnBTProgressDialog sInstance;
    private LocalBleManager mLocalBleManager;

    public static void show(Activity activity) {
        if (activity == null) {
            Log.w(TAG, "[show] activity is null,return.");
            return;
        }
        if (sInstance == null) {
            sInstance = new TurnOnBTProgressDialog(activity);
        }
        Log.i(TAG, "[show]turnOnBluetooth.");
        sInstance.mLocalBleManager.turnOnBluetooth();

        Message msg = sInstance.mHandler.obtainMessage();
        msg.what = SHOW_DIALOG;
        msg.obj = activity;
        sInstance.mHandler.sendMessage(msg);

    }

    public static void dismiss() {
        Log.i(TAG, "[dismiss]...");
        if (sInstance != null) {
            Message msg = sInstance.mHandler.obtainMessage();
            msg.what = DISMISS_DIALOG;
            sInstance.mHandler.sendMessage(msg);
            sInstance.mHandler.removeMessages(SHOW_TIMEOUT_FLAG);
        }
    }

    private TurnOnBTProgressDialog(Activity activity) {
        mLocalBleManager = LocalBleManager.getInstance(activity);
    }

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            Log.d(TAG, "[handleMessage]what = " + what);
            switch (what) {
            case SHOW_DIALOG:
                if (mProDialog != null && mProDialog.isShowing()) {
                    Log.i(TAG,
                            "[handleMessage]SHOW_DIALOG,already show,don't show it again,return!");
                    break;
                }
                Activity activity = (Activity) msg.obj;
                String str = activity.getResources().getString(R.string.turning_on_bluetooth);
                mProDialog = ProgressDialog.show(activity, "", str);
                mProDialog.setCancelable(false);

                Message msg1 = this.obtainMessage();
                msg1.what = SHOW_TIMEOUT_FLAG;
                mHandler.sendMessageDelayed(msg1, SHOW_TIMEOUT);
                break;

            case DISMISS_DIALOG:
                if (mProDialog != null && mProDialog.isShowing()) {
                    mProDialog.setCancelable(true);
                    mProDialog.dismiss();
                }
                break;

            case SHOW_TIMEOUT_FLAG:
                if (mProDialog != null && mProDialog.isShowing()) {
                    mProDialog.setCancelable(true);
                    mProDialog.dismiss();
                }
                mLocalBleManager.turnOffBluetooth();
                break;

            default:
                Log.w(TAG, "[handleMessage]mHandler unknown id");
                return;
            }
        }

    };
}
