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

package com.mediatek.bluetoothle.bleservice;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * BleReceiver class
 */
public class BleReceiver extends BroadcastReceiver {
    private static final boolean DBG = true;
    private static final String TAG = BleReceiver.class.getSimpleName();
    static final String ACTION_CHANGE_SERVICE_STATE =
            "com.mediatek.bluetoothle.bleservice.action.change.service.state";
    private static boolean sStarted = false;

    @Override
    public void onReceive(final Context ctxt, final Intent intent) {
        final boolean bBackgroundEnabled = BleProfileManagerService.isBackgroundModeEnabled(ctxt);
        if (null == intent) {
            Log.e(TAG, "onReceive: intent is null!");
            return;
        }

        final String action = intent.getAction();
        final int state =
                intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
        if (DBG) {
            Log.d(TAG, "onReceive: action=" + action);
            Log.d(TAG, "onReceive: bBackgroundEnabled=" + bBackgroundEnabled);
            Log.d(TAG, "onReceive: sStarted=" + sStarted);
            Log.d(TAG, "onReceive: state="
                    + (state == BluetoothAdapter.STATE_ON ? "BT On" : "BT Off"));
        }
        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)
                || BleReceiver.ACTION_CHANGE_SERVICE_STATE.equals(action)) {
            if (DBG) {
                Log.d(TAG, "onReceive: state=" + state);
            }
            if ((bBackgroundEnabled && state == BluetoothAdapter.STATE_ON)
                    || (state == BluetoothAdapter.STATE_OFF)
                    || (BleReceiver.ACTION_CHANGE_SERVICE_STATE.equals(action))) {
                if ((!sStarted && state == BluetoothAdapter.STATE_ON)
                        || (sStarted && state == BluetoothAdapter.STATE_OFF)) {
                    // /Auto Start BleProfileManagerService
                    sendIntentToBleProfileManagerService(ctxt, state);
                    // /Auto Start BleDeviceManagerService
                    sendIntentToBleDeviceManagerService(ctxt, state);
                    // / invert the flag
                    sStarted = !sStarted;
                } else {
                    if (DBG) {
                        Log.d(TAG, "ignore it!");
                    }
                }
            }
        }
    }

    private static void sendIntentToBleProfileManagerService(final Context ctxt, final int state) {
        com.mediatek.common.jpe.a aa = new com.mediatek.common.jpe.a();
        aa.a();
        if (null != ctxt) {
            final Intent intent = new Intent(ctxt, BleProfileManagerService.class);
            intent.putExtra(BleApp.EXTRA_ACTION, BleApp.ACTION_SERVICE_STATE_CHANGED);
            intent.putExtra(BluetoothAdapter.EXTRA_STATE, state);
            if (DBG) {
                Log.d(TAG,
                        "sendIntentToBleProfileManagerService: [Start Service] Name="
                        + BleProfileManagerService.class.getName() + ",Component ="
                        + ctxt.startService(intent));
            }
        } else {
            Log.e(TAG, "sendIntentToBleProfileManagerService: ctxt=null");
        }
    }

    private static void sendIntentToBleDeviceManagerService(final Context ctxt, final int state) {
        com.mediatek.common.jpe.a aa = new com.mediatek.common.jpe.a();
        aa.a();
        if (null != ctxt) {
            final Intent intent = new Intent(ctxt, BleDeviceManagerService.class);
            intent.putExtra(BleApp.EXTRA_ACTION, BleApp.ACTION_SERVICE_STATE_CHANGED);
            intent.putExtra(BluetoothAdapter.EXTRA_STATE, state);
            if (DBG) {
                Log.d(TAG,
                        "sendIntentToBleDeviceManagerService: [Start Service] Name="
                        + BleDeviceManagerService.class.getName() + ",Component ="
                        + ctxt.startService(intent));
            }
        } else {
            Log.e(TAG, "sendIntentToBleDeviceManagerService: ctxt=null");
        }
    }

}
