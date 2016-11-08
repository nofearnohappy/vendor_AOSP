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

package com.mediatek.bluetoothle;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;

import com.mediatek.bluetooth.BleProfile;

import java.util.List;
import java.util.UUID;

/**
 * Common interface for BleProfileServer {@hide}
 */
public interface IBleProfileServer {
    /**
     * Find Me Profile
     */
    int FMP = BleProfile.FMP;
    /**
     * Proximity Profile
     */
    int PXP = BleProfile.PXP;
    /**
     * Time Profile
     */
    int TIP = BleProfile.TIP;
    /**
     * Alert Notification Profile
     */
    int ANP = BleProfile.ANP;
    /**
     * Phone Alert Status Profile
     */
    int PASP = BleProfile.PASP;

    /**
     * Return Code
     */
    int RET_SUCCESS = 1;
    /**
     * Return Code
     */
    int RET_FAILURE = 2;
    /**
     * Return Code for startProfileServices
     */
    int RET_PROFILE_ALREADY_STARTED = 3;
    /**
     * Status Code
     */
    int RET_PROFILE_NO_REGISTERED = 4;
    /**
     * Status Code
     */
    int RET_PROFILE_PARTIAL_REGISTERED = 5;
    /**
     * Status Code
     */
    int RET_PROFILE_ALL_REGISTERED = 6;

    /**
     * Query the profile which the BleProfileServer instance bound with
     *
     * @return the id of the Ble profile
     */
    int getProfileId();

    /**
     * Get all the services belongs to the profile
     *
     * @return all the GATT services belongs to the profile
     */
    List<BluetoothGattService> getServices();

    /**
     * Get a service belongs to the profile based on a uuid
     *
     * @param uuid uuid for the ble services
     * @return a service belongs to the profile and its UUID is uuid
     */
    BluetoothGattService getService(UUID uuid);

    /**
     * Create a server instance with the BT stack To add all the services bound
     * with the profile
     *
     * @param callback Callback used to receive incoming request
     * @return RET_SUCCESS, the action to start profile is successfully
     *         RET_PROFILE_ALREADY_STARTED, the profile is already started
     *         RET_FAILURE, the action to start profile is failure
     */
    int startProfileServices(BluetoothGattServerCallback callback);

    /**
     * To remove all the services bound with the profile
     */
    void stopProfileServices();

    /**
     * Destroy a server instance with the BT stack
     */
    void close();

    /**
     * Send a notification or indication that a local characteristic has been
     * updated
     *
     * @param device The device you want to notify
     * @param characteristic The updated characteristic
     * @param confirm true, you need to be confirmed by the client false,you
     *            needn't to be confirmed by the client
     * @return true if success. otherwise, false.
     */
    boolean notifyCharacteristicChanged(BluetoothDevice device,
            BluetoothGattCharacteristic characteristic, boolean confirm);

    /**
     * Send a response to a read or write request to a remote device
     *
     * @param device The remote device to send this response to
     * @param requestId The ID of the request that was received with the
     *            callback
     * @param status The status of the request to be sent to the remote devices
     * @param offset Value offset for partial read/write response
     * @param value The value of the attribute that was read/written (optional)
     * @return true, sendResponse is successful false, it fail to sendResponse
     */
    boolean sendResponse(BluetoothDevice device, int requestId,
            int status, int offset, byte[] value);

    /**
     * Initiate a connection to a Bluetooth GATT capable device
     *
     * @param device The device you want to connect
     * @param autoConnect Whether to directly connect to the remote device
     *            (false) or to automatically connect as soon as the remote
     *            device becomes available (true)
     * @return true if success. otherwise, false.
     */
    boolean connect(BluetoothDevice device, boolean autoConnect);

    /**
     * Disconnects an established connection, or cancels a connection attempt
     * currently in progress
     *
     * @param device the bluetooth device
     */
    void cancelConnection(BluetoothDevice device);

}
