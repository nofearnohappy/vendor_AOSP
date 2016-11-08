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
 * Public API for the GATT Alert Notification Profile(Client).
 *
 * Name: Alert Notification
 * Type: org.bluetooth.profile.alert_notification
 * Last Modified: None
 * Revision: None
 * Role: AlertNotificationClient
 */
public class AnpAlertNotificationClient extends ClientBase {
    private static final boolean DBG = true;
    private static final String TAG = "AnpAlertNotificationClient";

    /**
     * Create a AnpAlertNotificationClient object and init value.
     *
     * @param context App context
     */
    public AnpAlertNotificationClient(Context context) {
        super(context);

        if (DBG) {
            Log.d(TAG, "AnpAlertNotificationClient()");
        }
    }

    @Override
    protected boolean handleServicesDiscovered(BluetoothGatt gatt, int status) {
        // Check mandatory service is exist.
        return true &&
                (gatt.getService(GattUuid.SRVC_ANS) != null);
    }

    /**
     * Reads Ans:SupportedNewAlertCategory characteristic
     * from the associated remote device.
     *
     * @return true, if the read operation was initiated successfully
     */
    public boolean readAnsSupportedNewAlertCategory() {
        if (DBG) {
            Log.d(TAG, "readAnsSupportedNewAlertCategory()");
        }
        return readCharacteristic(
                GattUuid.SRVC_ANS,
                GattUuid.CHAR_SUPPORTED_NEW_ALERT_CATEGORY);
    }

    /**
     * Reads Ans:SupportedUnreadAlertCategory characteristic
     * from the associated remote device.
     *
     * @return true, if the read operation was initiated successfully
     */
    public boolean readAnsSupportedUnreadAlertCategory() {
        if (DBG) {
            Log.d(TAG, "readAnsSupportedUnreadAlertCategory()");
        }
        return readCharacteristic(
                GattUuid.SRVC_ANS,
                GattUuid.CHAR_SUPPORTED_UNREAD_ALERT_CATEGORY);
    }


    /**
     * Writes Ans:AlertNotificationControlPoint characteristic and its values
     * to the associated remote device.
     *
     * @param characteristic Characteristic to write on the remote device
     * @return true, if the write operation was initiated successfully
     */
    public boolean writeAnsAlertNotificationControlPoint(CharacteristicBase characteristic) {
        if (DBG) {
            Log.d(TAG, "writeAnsAlertNotificationControlPoint()");
        }
        return writeCharacteristic(
                GattUuid.SRVC_ANS,
                characteristic);
    }


    /**
     * Reads Ans:NewAlert:Cccd descriptor
     * from the associated remote device.
     *
     * @return true, if the read operation was initiated successfully
     */
    public boolean readAnsNewAlertCccd() {
        if (DBG) {
            Log.d(TAG, "readAnsNewAlertCccd()");
        }
        return readDescriptor(
                GattUuid.SRVC_ANS,
                GattUuid.CHAR_NEW_ALERT,
                GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION);
    }

    /**
     * Reads Ans:UnreadAlertStatus:Cccd descriptor
     * from the associated remote device.
     *
     * @return true, if the read operation was initiated successfully
     */
    public boolean readAnsUnreadAlertStatusCccd() {
        if (DBG) {
            Log.d(TAG, "readAnsUnreadAlertStatusCccd()");
        }
        return readDescriptor(
                GattUuid.SRVC_ANS,
                GattUuid.CHAR_UNREAD_ALERT_STATUS,
                GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION);
    }


    /**
     * Writes Ans:NewAlert:Cccd descriptor and
     * its values to the associated remote device.
     *
     * @param value Descriptor value to write on the remote device
     * @return true, if the write operation was initiated successfully
     */
    public boolean writeAnsNewAlertCccd(byte[] value) {
        if (DBG) {
            Log.d(TAG, "writeAnsNewAlertCccd()");
        }
        return writeDescriptor(
                GattUuid.SRVC_ANS,
                GattUuid.CHAR_NEW_ALERT,
                GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION,
                value);
    }

    /**
     * Writes Ans:UnreadAlertStatus:Cccd descriptor and
     * its values to the associated remote device.
     *
     * @param value Descriptor value to write on the remote device
     * @return true, if the write operation was initiated successfully
     */
    public boolean writeAnsUnreadAlertStatusCccd(byte[] value) {
        if (DBG) {
            Log.d(TAG, "writeAnsUnreadAlertStatusCccd()");
        }
        return writeDescriptor(
                GattUuid.SRVC_ANS,
                GattUuid.CHAR_UNREAD_ALERT_STATUS,
                GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION,
                value);
    }

}


