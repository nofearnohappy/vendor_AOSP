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

import com.mediatek.bluetoothgatt.GattUuid;
import com.mediatek.bluetoothgatt.characteristic.FirmwareRevisionString;
import com.mediatek.bluetoothgatt.characteristic.FormatUtils;
import com.mediatek.bluetoothgatt.characteristic.HardwareRevisionString;
import com.mediatek.bluetoothgatt.characteristic.ManufacturerNameString;
import com.mediatek.bluetoothgatt.characteristic.ModelNumberString;
import com.mediatek.bluetoothgatt.characteristic.PnpId;
import com.mediatek.bluetoothgatt.characteristic.RegCertDataList;
import com.mediatek.bluetoothgatt.characteristic.SerialNumberString;
import com.mediatek.bluetoothgatt.characteristic.SoftwareRevisionString;
import com.mediatek.bluetoothgatt.characteristic.SystemId;

import java.util.UUID;

/**
 * Public API for the GATT Device Information Service.
 *
 * <p>Name: Device Information Service
 * Type: org.bluetooth.service.device_information
 * UUID: 180A
 * Last Modified: 2011-10-28
 * Revision: Yes
 */
public class Dis extends ServiceBase {
    /**
     * Device Information Service UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("180A"));

    /**
     * Get Device Information Service UUID.
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
        addManufacturerNameString();
        addModelNumberString();
        addSerialNumberString();
        addHardwareRevisionString();
        addFirmwareRevisionString();
        addSoftwareRevisionString();
        addSystemId();
        addRegCertDataList();
        addPnpId();
    }

    /**
     * Add ManufacturerNameString with default settings.
     */
    void addManufacturerNameString() {

        addCharacteristic(
                false,
                GattUuid.CHAR_MANUFACTURER_NAME_STRING,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ,
                new ManufacturerNameString().getValue()
        );

    }

    /**
     * Add ModelNumberString with default settings.
     */
    void addModelNumberString() {

        addCharacteristic(
                false,
                GattUuid.CHAR_MODEL_NUMBER_STRING,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ,
                new ModelNumberString().getValue()
        );

    }

    /**
     * Add SerialNumberString with default settings.
     */
    void addSerialNumberString() {

        addCharacteristic(
                false,
                GattUuid.CHAR_SERIAL_NUMBER_STRING,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ,
                new SerialNumberString().getValue()
        );

    }

    /**
     * Add HardwareRevisionString with default settings.
     */
    void addHardwareRevisionString() {

        addCharacteristic(
                false,
                GattUuid.CHAR_HARDWARE_REVISION_STRING,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ,
                new HardwareRevisionString().getValue()
        );

    }

    /**
     * Add FirmwareRevisionString with default settings.
     */
    void addFirmwareRevisionString() {

        addCharacteristic(
                false,
                GattUuid.CHAR_FIRMWARE_REVISION_STRING,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ,
                new FirmwareRevisionString().getValue()
        );

    }

    /**
     * Add SoftwareRevisionString with default settings.
     */
    void addSoftwareRevisionString() {

        addCharacteristic(
                false,
                GattUuid.CHAR_SOFTWARE_REVISION_STRING,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ,
                new SoftwareRevisionString().getValue()
        );

    }

    /**
     * Add SystemId with default settings.
     */
    void addSystemId() {

        addCharacteristic(
                false,
                GattUuid.CHAR_SYSTEM_ID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ,
                new SystemId().getValue()
        );

    }

    /**
     * Add RegCertDataList with default settings.
     */
    void addRegCertDataList() {

        addCharacteristic(
                false,
                GattUuid.CHAR_REG_CERT_DATA_LIST,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ,
                new RegCertDataList().getValue()
        );

    }

    /**
     * Add PnpId with default settings.
     */
    void addPnpId() {

        addCharacteristic(
                false,
                GattUuid.CHAR_PNP_ID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ,
                new PnpId().getValue()
        );

    }

}

