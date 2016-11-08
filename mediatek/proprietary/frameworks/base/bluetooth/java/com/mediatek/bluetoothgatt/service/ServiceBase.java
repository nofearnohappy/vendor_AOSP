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

package com.mediatek.bluetoothgatt.service;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.UUID;

/**
 * Public APIs for the Bluetooth GATT service.
 */
public abstract class ServiceBase {
    public static final boolean DBG = true;
    private static final String TAG = "ServiceBase";

    /**
     * Create a ServiceBase object.
     */
    public ServiceBase() {
        loadServiceConfig();
    }

    /**
     * Returns the UUID of this characteristic.
     *
     * @return UUID of this characteristic
     */
    public abstract UUID getUuid();

    protected abstract void loadServiceConfig();

    private ServiceCfg mCfg = new ServiceCfg(
            getUuid(), 0, BluetoothGattService.SERVICE_TYPE_PRIMARY);

    public ServiceCfg getServiceCfg() {
        return mCfg;
    }

    void addIncludeService(boolean support, UUID uuid, int serviceType, int instanceId) {
        mCfg.addIncludeService(support, uuid, serviceType, instanceId);
    }

    void addCharacteristic(boolean support, UUID uuid, int properties, int permissions) {
        mCfg.addCharacteristic(support, uuid, properties, permissions);
    }

    void addCharacteristic(boolean support, UUID uuid, int properties, int permissions,
            byte[] value) {
        mCfg.addCharacteristic(support, uuid, properties, permissions, value);
    }

    void addDescriptor(boolean support, UUID uuid, int permissions) {
        mCfg.addDescriptor(support, uuid, permissions);
    }

    void addDescriptor(boolean support, UUID uuid, int permissions, byte[] value) {
        mCfg.addDescriptor(support, uuid, permissions, value);
    }

    /**
     * Returns the gatt-service this service belongs to.
     *
     * @return BluetoothGattService
     */
    public BluetoothGattService getService() {
        if (!mCfg.mSupport) {
            return null;
        }

        BluetoothGattService service = new BluetoothGattService(mCfg.mUuid, mCfg.mServiceType);
        // TODO: Set service instance ID
        // service.setInstanceId(mCfg.mInstanceId);

        BluetoothGattCharacteristic characteristic = null;

        for (ServiceCfg.Entry entry : mCfg.getEntries()) {
            if (characteristic != null && entry.mType != ServiceCfg.TYPE_DESCRIPTOR) {
                service.addCharacteristic(characteristic);
                characteristic = null;
            }

            if (entry.mType == ServiceCfg.TYPE_SERVICE) {
                if (entry.mSupport) {
                    BluetoothGattService includeService =
                            new BluetoothGattService(entry.mUuid, entry.mServiceType);
                    // TODO: Set include service instance ID
                    // includeService.setInstanceId(entry.mInstanceId);
                    service.addService(includeService);
                }
            } else if (entry.mType == ServiceCfg.TYPE_CHARACTERISTIC) {
                if (entry.mSupport) {
                    characteristic = new BluetoothGattCharacteristic(
                            entry.mUuid,
                            entry.mProperties,
                            entry.mPermissions);
                    characteristic.setValue(entry.mValue);
                }
            } else if (entry.mType == ServiceCfg.TYPE_DESCRIPTOR) {
                if (entry.mSupport && characteristic != null) {
                    BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
                            entry.mUuid,
                            entry.mPermissions
                    );
                    descriptor.setValue(entry.mValue);
                    characteristic.addDescriptor(descriptor);
                }
            }
        }

        if (characteristic != null) {
            service.addCharacteristic(characteristic);
        }

        return service;
    }
}
