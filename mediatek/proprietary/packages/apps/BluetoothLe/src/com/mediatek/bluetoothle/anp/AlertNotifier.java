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

package com.mediatek.bluetoothle.anp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.mediatek.bluetoothle.IBleProfileServer;
import com.mediatek.bluetoothle.anp.data.GattAnsAttributes;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class AlertNotifier {

    private static final String TAG = "[BluetoothAns]AlertNotifier";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    IBleProfileServer mBluetoothGattServer = null;

    AlertNotifier() {

    }

    public void setGattServer(IBleProfileServer bluetoothGattServer) {
        mBluetoothGattServer = bluetoothGattServer;
    }

    public void alertNewToDevices(byte categoryId, byte alertCount, String contentText,
            ArrayList<BluetoothDevice> devices) {
        byte[] result;
        if (contentText != null && !contentText.isEmpty()) {
            byte[] textBytes = null;
            try {
                textBytes = contentText.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            result = GattAnsAttributes.concatBytes(new byte[] {
                    categoryId, alertCount
            }, textBytes);
        } else {
            result = new byte[] {
                    categoryId, alertCount
            };
        }
        if (mBluetoothGattServer == null) {
            Log.e(TAG, "mBluetoothGattServer is null, error in alertNewToDevices");
            return;
        }
        BluetoothGattService gattService = mBluetoothGattServer
                .getService(GattAnsAttributes.ALERT_NOTIFICATION_SERVICE_UUID);
        BluetoothGattCharacteristic characteristic = gattService
                .getCharacteristic(GattAnsAttributes.NEW_ALERT_UUID);
        characteristic.setValue(result);
        for (BluetoothDevice device : devices) {
            if (VDBG) {
                Log.v(TAG, "alertNewToDevices(), device = " + device);
            }
            mBluetoothGattServer.notifyCharacteristicChanged(device, characteristic, false);
        }
    }

    public void alertUnreadToDevices(byte categoryId, byte alertCount,
            ArrayList<BluetoothDevice> devices) {
        byte[] result = {
                categoryId, alertCount
        };
        if (mBluetoothGattServer == null) {
            Log.e(TAG, "mBluetoothGattServer is null, error in alertUnreadToDevices");
            return;
        }
        BluetoothGattService gattService = mBluetoothGattServer
                .getService(GattAnsAttributes.ALERT_NOTIFICATION_SERVICE_UUID);
        BluetoothGattCharacteristic characteristic = null;
        characteristic = gattService.getCharacteristic(GattAnsAttributes.UNREAD_ALERT_STATUS);

        if (characteristic != null) {
            characteristic.setValue(result);
            for (BluetoothDevice device : devices) {
                if (VDBG) {
                    Log.v(TAG, "alertUnreadToDevices(), device = " + device);
                }
                mBluetoothGattServer.notifyCharacteristicChanged(device, characteristic, false);
            }
        }
    }
}
