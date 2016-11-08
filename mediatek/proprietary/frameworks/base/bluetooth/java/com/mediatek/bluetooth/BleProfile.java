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

package com.mediatek.bluetooth;

import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

/**
 * Public API for the GATT-based BLE Profiles
 *
 * <p>This class is the base class for all BLE profile clients.
 * All profile client SDK classes need to inherit this class
 *
 * <p>Profile clients are used to do profile-specific operation with remote Bluetooth Smart devices
 *
 * @hide
 */

public abstract class BleProfile {

    /**
      * @internal
      */
    public static final int FMP = 0;

    /**
      * @internal
      */
    public static final int PXP = 1;

    /**
      * @internal
      */
    public static final int TIP = 2;

    /**
      * @internal
      */
    public static final int ANP = 3;

    /**
      * @internal
      */
    public static final int PASP = 4;

    /**
     * Return the device for the profile
     */
    public abstract BluetoothDevice getDevice();

    /**
     * The method to register with ClientManagerService
     */
    abstract void open();

    /**
     * The method to unregister with ClientManagerService
     */
    abstract void close();

    public interface BleProfileCallback {

    }
}

/**
 * Internal GATT API for Profile Client
 *
 * <p>This class is used to interact with a remote GATT capable device
 *
 * @hide
 */
interface IBleGatt {

    void registerClientCallback(int profileID, IBleGattCallback callback);

    void unregisterClientCallback(int profileID);

    BluetoothDevice getDevice();

    List<BluetoothGattService> getServices();

    BluetoothGattService getService(UUID uuid);

    ///GATT Characteristic/Descriptor operations
    boolean readCharacteristic(int profileID, BluetoothGattCharacteristic characteristic);

    boolean writeCharacteristic(int profileID, BluetoothGattCharacteristic characteristic);

    boolean readDescriptor(int profileID, BluetoothGattDescriptor descriptor);

    boolean writeDescriptor(int profileID, BluetoothGattDescriptor descriptor);

    boolean beginReliableWrite(int profileID);

    boolean executeReliableWrite(int profileID);

    void abortReliableWrite(int profileID);

    boolean setCharacteristicNotification(int profileID,
            BluetoothGattCharacteristic characteristic, boolean enable);

    boolean readRemoteRssi(int profileID);

}

/**
 * Internal GATT Callback for Profile Client
 *
 * <p>This class is used to receive GATT response/notification from a remote GATT capable device
 *
 * @hide
 */
interface IBleGattCallback {

    void onCharacteristicRead(IBleGatt gatt,
            BluetoothGattCharacteristic characteristic, int status);

    void onCharacteristicWrite(IBleGatt gatt,
            BluetoothGattCharacteristic characteristic, int status);

    void onCharacteristicChanged(IBleGatt gatt,
            BluetoothGattCharacteristic characteristic);

    void onDescriptorRead(IBleGatt gatt,
            BluetoothGattDescriptor descriptor, int status);

    void onDescriptorWrite(IBleGatt gatt,
            BluetoothGattDescriptor descriptor, int status);

    void onReliableWriteCompleted(IBleGatt gatt, int status);

    void onReadRemoteRssi(IBleGatt gatt, int rssi, int status);

}