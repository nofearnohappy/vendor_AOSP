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
 * MediaTek Inc. (C) 2015. All rights reserved.
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

package com.mediatek.bluetoothgatt.profile;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattDescriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Public APIs for GATT Server profile to handle each remote device
 * Client-Characteristic-Configuration descriptor value.
 *
 */
class ClientCharacteristicConfiguration {
    final private List<Entry> mCccdList = new ArrayList<>();

    void add(UUID srvcUuid, UUID charUuid) {
        if (find(srvcUuid, charUuid) == null) {
            mCccdList.add(new Entry(srvcUuid, charUuid));
        }
    }

    Entry find(UUID srvcUuid, UUID charUuid) {
        for (Entry entry : mCccdList) {
            if (entry.mSrvcUuid.equals(srvcUuid) && entry.mCharUuid.equals(charUuid)) {
                return entry;
            }
        }
        return null;
    }

    byte[] getValue(BluetoothDevice device, UUID srvcUuid, UUID charUuid) {
        Entry entry = find(srvcUuid, charUuid);
        if (entry == null) {
            return BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
        } else {
            return entry.getValue(device);
        }
    }

    void setValue(BluetoothDevice device, UUID srvcUuid, UUID charUuid, byte[] value) {
        Entry entry = find(srvcUuid, charUuid);
        if (entry != null) {
            entry.setValue(device, value);
        }
    }

    Map<BluetoothDevice, byte[]> getDevices(UUID srvcUuid, UUID charUuid) {
        for (Entry cccd : mCccdList) {
            if (cccd.mSrvcUuid.equals(srvcUuid) && cccd.mCharUuid.equals(charUuid)) {
                return cccd.mDevices;
            }
        }
        return null;
    }

    void removeDevice(BluetoothDevice device) {
        for (Entry cccd : mCccdList) {
            cccd.removeDevice(device);
        }
    }

    /**
     * This class is using to store remote device Client-Characteristic-Configuration value.
     */
    class Entry {
        final UUID mSrvcUuid;
        final UUID mCharUuid;
        final Map<BluetoothDevice, byte[]> mDevices = new HashMap<BluetoothDevice, byte[]>();

        Entry(UUID srvcUuid, UUID charUuid) {
            mSrvcUuid = srvcUuid;
            mCharUuid = charUuid;
        }

        byte[] getValue(BluetoothDevice device) {
            if (mDevices.containsKey(device)) {
                return mDevices.get(device);
            } else {
                return BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
            }
        }

        void setValue(BluetoothDevice device, byte[] value) {
            synchronized (mDevices) {
                if (mDevices.containsKey(device)) {
                    mDevices.remove(device);
                }
                if (!Arrays.equals(value, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
                    mDevices.put(device, value);
                }
            }
        }

        void removeDevice(BluetoothDevice device) {
            synchronized (mDevices) {
                if (mDevices.containsKey(device)) {
                    mDevices.remove(device);
                }
            }
        }
    }
}
