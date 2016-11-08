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

import com.mediatek.bluetoothgatt.GattUuid;
import com.mediatek.bluetoothgatt.characteristic.AlertStatus;
import com.mediatek.bluetoothgatt.characteristic.FormatUtils;
import com.mediatek.bluetoothgatt.characteristic.RingerControlPoint;
import com.mediatek.bluetoothgatt.characteristic.RingerSetting;

import java.util.UUID;

/**
 * Public API for the GATT Phone Alert Status Service.
 *
 * <p>Name: Phone Alert Status Service
 * Type: org.bluetooth.service.phone_alert_status
 * UUID: 180E
 * Last Modified: None
 * Revision: None
 */
public class Pass extends ServiceBase {
    /**
     * Phone Alert Status Service UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("180E"));

    /**
     * Get Phone Alert Status Service UUID.
     *
     * @return Service UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Load default GATT service configuration.
     */
    @Override
    protected void loadServiceConfig() {
        addAlertStatus();
        addRingerSetting();
        addRingerControlPoint();
    }

    /**
     * Add AlertStatus with default settings.
     */
    void addAlertStatus() {

        addCharacteristic(
                true,
                GattUuid.CHAR_ALERT_STATUS,
                BluetoothGattCharacteristic.PROPERTY_READ |
                                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ,
                new AlertStatus().getValue()
        );

        addDescriptor(
                true,
                GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION,
                BluetoothGattDescriptor.PERMISSION_READ |
                        BluetoothGattDescriptor.PERMISSION_WRITE
        );
    }

    /**
     * Add RingerSetting with default settings.
     */
    void addRingerSetting() {

        addCharacteristic(
                true,
                GattUuid.CHAR_RINGER_SETTING,
                BluetoothGattCharacteristic.PROPERTY_READ |
                                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ,
                new RingerSetting().getValue()
        );

        addDescriptor(
                true,
                GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION,
                BluetoothGattDescriptor.PERMISSION_READ |
                        BluetoothGattDescriptor.PERMISSION_WRITE
        );
    }

    /**
     * Add RingerControlPoint with default settings.
     */
    void addRingerControlPoint() {

        addCharacteristic(
                true,
                GattUuid.CHAR_RINGER_CONTROL_POINT,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE,
                new RingerControlPoint().getValue()
        );

    }

}

