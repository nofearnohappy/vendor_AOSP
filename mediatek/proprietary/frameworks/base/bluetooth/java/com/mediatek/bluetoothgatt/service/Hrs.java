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
import com.mediatek.bluetoothgatt.characteristic.BodySensorLocation;
import com.mediatek.bluetoothgatt.characteristic.FormatUtils;
import com.mediatek.bluetoothgatt.characteristic.HeartRateControlPoint;
import com.mediatek.bluetoothgatt.characteristic.HeartRateMeasurement;

import java.util.UUID;

/**
 * Public API for the GATT Heart Rate Service.
 *
 * <p>Name: Heart Rate Service
 * Type: org.bluetooth.service.heart_rate
 * UUID: 180D
 * Last Modified: None
 * Revision: None
 */
public class Hrs extends ServiceBase {
    /**
     * Heart Rate Service UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("180D"));

    /**
     * Get Heart Rate Service UUID.
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
        addHeartRateMeasurement();
        addBodySensorLocation();
        addHeartRateControlPoint();
    }

    /**
     * Add HeartRateMeasurement with default settings.
     */
    void addHeartRateMeasurement() {

        addCharacteristic(
                true,
                GattUuid.CHAR_HEART_RATE_MEASUREMENT,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                0,
                new HeartRateMeasurement().getValue()
        );

        addDescriptor(
                true,
                GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION,
                BluetoothGattDescriptor.PERMISSION_READ |
                        BluetoothGattDescriptor.PERMISSION_WRITE
        );
    }

    /**
     * Add BodySensorLocation with default settings.
     */
    void addBodySensorLocation() {

        addCharacteristic(
                false,
                GattUuid.CHAR_BODY_SENSOR_LOCATION,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ,
                new BodySensorLocation().getValue()
        );

    }

    /**
     * Add HeartRateControlPoint with default settings.
     */
    void addHeartRateControlPoint() {

        addCharacteristic(
                false,
                GattUuid.CHAR_HEART_RATE_CONTROL_POINT,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE,
                new HeartRateControlPoint().getValue()
        );

    }

}

