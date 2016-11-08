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

import android.os.ParcelUuid;
import android.bluetooth.BluetoothDevice;
import com.mediatek.bluetooth.parcel.ParcelBluetoothGattService;
import com.mediatek.bluetooth.parcel.ParcelBluetoothGattCharacteristic;
import com.mediatek.bluetooth.parcel.ParcelBluetoothGattDescriptor;
import com.mediatek.bluetooth.IBleDeviceManagerCallback;

/**
 * System private API for talking with the BleDeviceManagerService.
 *
 * {@hide}
 */
interface IBleDeviceManager
{
    int registerClient(in ParcelUuid appId, in BluetoothDevice device, in IBleDeviceManagerCallback callback);
    
    void unregisterClient(in int clientIf);

    ///Interface for device control
    boolean connectDevice(in int clientIf, in BluetoothDevice device);
    boolean disconnectDevice(in int clientIf, in BluetoothDevice device);
    
    boolean discoverServices(in int clientIf, in BluetoothDevice device);
    List<ParcelBluetoothGattService> getServices(in BluetoothDevice device);
    ParcelBluetoothGattService getService(in BluetoothDevice device, in ParcelUuid uuid);
    int getState(in int clientIf, in BluetoothDevice device);
        
    ///Interface for profile client
    
    boolean readCharacteristic(in int clientID, in int profileID, in BluetoothDevice device, 
                               in ParcelBluetoothGattCharacteristic characteristic);

    boolean writeCharacteristic(in int clientID, in int profileID, in BluetoothDevice device, 
                                in ParcelBluetoothGattCharacteristic characteristic);

    boolean readDescriptor(in int clientID, in int profileID, in BluetoothDevice device, 
                           in ParcelBluetoothGattDescriptor descriptor);

    boolean writeDescriptor(in int clientID, in int profileID, in BluetoothDevice device, 
                            in ParcelBluetoothGattDescriptor descriptor);

    boolean setCharacteristicNotification(in int clientID, in int profileID, in BluetoothDevice device,
                                          in ParcelBluetoothGattCharacteristic characteristic,
                                          in boolean enable);
                                          
    boolean readRemoteRssi(in int clientID, in int profileID, in BluetoothDevice device);
    
    boolean beginReliableWrite(in int clientID, in int profileID, in BluetoothDevice device);

    boolean executeReliableWrite(in int clientID, in int profileID, in BluetoothDevice device);

    void abortReliableWrite(in int clientID, in int profileID, in BluetoothDevice device);
    
    void addGattDevice(in BluetoothDevice device);
    
    void deleteGattDevice(in BluetoothDevice device);
}
