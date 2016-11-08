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

// Customized Start: Import ........................................................................
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.util.Log;

import com.mediatek.bluetoothgatt.GattUuid;
import com.mediatek.bluetoothgatt.characteristic.CharacteristicBase;
//........................................................................ Customized End: Import //

/**
 * Public API for the GATT Phone Alert Status Profile(Client).
 *
 * Name: Phone Alert Status
 * Type: org.bluetooth.profile.phone_alert_status
 * Last Modified: None
 * Revision: None
 * Role: PhoneAlertClient
 */
public class PaspPhoneAlertClient extends ClientBase {
    private static final boolean DBG = true;
    private static final String TAG = "PaspPhoneAlertClient";

    /**
     * Create a PaspPhoneAlertClient object and init value.
     *
     * @param context App context
     */
    public PaspPhoneAlertClient(Context context) {
        super(context);

        if (DBG) {
            Log.d(TAG, "PaspPhoneAlertClient()");
        }
    }

    @Override
    protected boolean handleServicesDiscovered(BluetoothGatt gatt, int status) {
        // Check mandatory service is exist.
        return true &&
                (gatt.getService(GattUuid.SRVC_PASS) != null);
    }

    /**
     * Reads Pass:AlertStatus characteristic
     * from the associated remote device.
     *
     * @return true, if the read operation was initiated successfully
     */
    public boolean readPassAlertStatus() {
        if (DBG) {
            Log.d(TAG, "readPassAlertStatus()");
        }
        return readCharacteristic(
                GattUuid.SRVC_PASS,
                GattUuid.CHAR_ALERT_STATUS);
    }

    /**
     * Reads Pass:RingerSetting characteristic
     * from the associated remote device.
     *
     * @return true, if the read operation was initiated successfully
     */
    public boolean readPassRingerSetting() {
        if (DBG) {
            Log.d(TAG, "readPassRingerSetting()");
        }
        return readCharacteristic(
                GattUuid.SRVC_PASS,
                GattUuid.CHAR_RINGER_SETTING);
    }


    /**
     * Writes Pass:RingerControlPoint characteristic and its values
     * to the associated remote device.
     *
     * @param characteristic Characteristic to write on the remote device
     * @return true, if the write operation was initiated successfully
     */
    public boolean writePassRingerControlPoint(CharacteristicBase characteristic) {
        if (DBG) {
            Log.d(TAG, "writePassRingerControlPoint()");
        }
        return writeCharacteristic(
                GattUuid.SRVC_PASS,
                characteristic);
    }


    /**
     * Reads Pass:AlertStatus:Cccd descriptor
     * from the associated remote device.
     *
     * @return true, if the read operation was initiated successfully
     */
    public boolean readPassAlertStatusCccd() {
        if (DBG) {
            Log.d(TAG, "readPassAlertStatusCccd()");
        }
        return readDescriptor(
                GattUuid.SRVC_PASS,
                GattUuid.CHAR_ALERT_STATUS,
                GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION);
    }

    /**
     * Reads Pass:RingerSetting:Cccd descriptor
     * from the associated remote device.
     *
     * @return true, if the read operation was initiated successfully
     */
    public boolean readPassRingerSettingCccd() {
        if (DBG) {
            Log.d(TAG, "readPassRingerSettingCccd()");
        }
        return readDescriptor(
                GattUuid.SRVC_PASS,
                GattUuid.CHAR_RINGER_SETTING,
                GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION);
    }


    /**
     * Writes Pass:AlertStatus:Cccd descriptor and
     * its values to the associated remote device.
     *
     * @param value Descriptor value to write on the remote device
     * @return true, if the write operation was initiated successfully
     */
    public boolean writePassAlertStatusCccd(byte[] value) {
        if (DBG) {
            Log.d(TAG, "writePassAlertStatusCccd()");
        }
        return writeDescriptor(
                GattUuid.SRVC_PASS,
                GattUuid.CHAR_ALERT_STATUS,
                GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION,
                value);
    }

    /**
     * Writes Pass:RingerSetting:Cccd descriptor and
     * its values to the associated remote device.
     *
     * @param value Descriptor value to write on the remote device
     * @return true, if the write operation was initiated successfully
     */
    public boolean writePassRingerSettingCccd(byte[] value) {
        if (DBG) {
            Log.d(TAG, "writePassRingerSettingCccd()");
        }
        return writeDescriptor(
                GattUuid.SRVC_PASS,
                GattUuid.CHAR_RINGER_SETTING,
                GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION,
                value);
    }

}


