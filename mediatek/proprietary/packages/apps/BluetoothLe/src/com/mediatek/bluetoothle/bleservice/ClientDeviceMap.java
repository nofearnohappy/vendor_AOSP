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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An internal utility class to help keep track of all known GATT devices
 */

public class ClientDeviceMap {

    private final ConcurrentHashMap<BluetoothDevice, Pair<BluetoothGatt, BluetoothGattCallback>> mDeviceMap =
            new ConcurrentHashMap<BluetoothDevice, Pair<BluetoothGatt, BluetoothGattCallback>>();

    public BluetoothGatt getDeviceClient(final BluetoothDevice device) {
        final Pair<BluetoothGatt, BluetoothGattCallback> clientData = mDeviceMap.get(device);

        if (clientData == null) {
            return null;
        } else {
            return clientData.first;
        }
    }

    public List<BluetoothGatt> getDeviceClientList() {
        final List<BluetoothGatt> gattClientList = new ArrayList<BluetoothGatt>();

        final Collection<Pair<BluetoothGatt, BluetoothGattCallback>> colleciton =
                mDeviceMap.values();

        final Iterator<Pair<BluetoothGatt, BluetoothGattCallback>> iter = colleciton.iterator();

        while (iter.hasNext()) {
            Pair<BluetoothGatt, BluetoothGattCallback> devicePair = iter.next();
            gattClientList.add(devicePair.first);
        }

        return gattClientList;
    }

    public void setDeviceClientData(final BluetoothDevice device, final BluetoothGatt gatt,
            final BluetoothGattCallback cb) {
        final Pair<BluetoothGatt, BluetoothGattCallback> data =
                new Pair<BluetoothGatt, BluetoothGattCallback>(gatt, cb);

        mDeviceMap.put(device, data);
    }

    public BluetoothGattCallback getDeviceClientCallback(final BluetoothDevice device) {
        final Pair<BluetoothGatt, BluetoothGattCallback> clientData = mDeviceMap.get(device);

        if (clientData == null) {
            return null;
        } else {
            return clientData.second;
        }
    }
}
