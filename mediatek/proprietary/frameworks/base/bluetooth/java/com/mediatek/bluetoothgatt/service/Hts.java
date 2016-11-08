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
import com.mediatek.bluetoothgatt.characteristic.FormatUtils;
import com.mediatek.bluetoothgatt.characteristic.IntermediateTemperature;
import com.mediatek.bluetoothgatt.characteristic.MeasurementInterval;
import com.mediatek.bluetoothgatt.characteristic.TemperatureMeasurement;
import com.mediatek.bluetoothgatt.characteristic.TemperatureType;

import java.util.UUID;

/**
 * Public API for the GATT Health Thermometer Service.
 *
 * <p>Name: Health Thermometer Service
 * Type: org.bluetooth.service.health_thermometer
 * UUID: 1809
 * Last Modified: None
 * Revision: None
 */
public class Hts extends ServiceBase {
    /**
     * Health Thermometer Service UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("1809"));

    /**
     * Get Health Thermometer Service UUID.
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
        addTemperatureMeasurement();
        addTemperatureType();
        addIntermediateTemperature();
        addMeasurementInterval();
    }

    /**
     * Add TemperatureMeasurement with default settings.
     */
    void addTemperatureMeasurement() {

        addCharacteristic(
                true,
                GattUuid.CHAR_TEMPERATURE_MEASUREMENT,
                BluetoothGattCharacteristic.PROPERTY_INDICATE,
                0,
                new TemperatureMeasurement().getValue()
        );

        addDescriptor(
                true,
                GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION,
                BluetoothGattDescriptor.PERMISSION_READ |
                        BluetoothGattDescriptor.PERMISSION_WRITE
        );
    }

    /**
     * Add TemperatureType with default settings.
     */
    void addTemperatureType() {

        addCharacteristic(
                false,
                GattUuid.CHAR_TEMPERATURE_TYPE,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ,
                new TemperatureType().getValue()
        );

    }

    /**
     * Add IntermediateTemperature with default settings.
     */
    void addIntermediateTemperature() {

        addCharacteristic(
                false,
                GattUuid.CHAR_INTERMEDIATE_TEMPERATURE,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                0,
                new IntermediateTemperature().getValue()
        );

        addDescriptor(
                true,
                GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION,
                BluetoothGattDescriptor.PERMISSION_READ |
                        BluetoothGattDescriptor.PERMISSION_WRITE
        );
    }

    /**
     * Add MeasurementInterval with default settings.
     */
    void addMeasurementInterval() {

        addCharacteristic(
                false,
                GattUuid.CHAR_MEASUREMENT_INTERVAL,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ,
                new MeasurementInterval().getValue()
        );

        addDescriptor(
                false,
                GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION,
                BluetoothGattDescriptor.PERMISSION_READ |
                        BluetoothGattDescriptor.PERMISSION_WRITE
        );

        addDescriptor(
                false,
                GattUuid.DESCR_VALID_RANGE,
                BluetoothGattDescriptor.PERMISSION_READ
        );
    }

}

