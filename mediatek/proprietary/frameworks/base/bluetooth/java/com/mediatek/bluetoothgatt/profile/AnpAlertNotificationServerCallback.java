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
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Log;

import com.mediatek.bluetoothgatt.GattUuid;
import com.mediatek.bluetoothgatt.characteristic.AlertNotificationControlPoint;
import com.mediatek.bluetoothgatt.characteristic.CharacteristicBase;
import com.mediatek.bluetoothgatt.characteristic.SupportedNewAlertCategory;
import com.mediatek.bluetoothgatt.characteristic.SupportedUnreadAlertCategory;

import java.util.UUID;
//........................................................................ Customized End: Import //

/**
 * This class is used to implement {@link AnpAlertNotificationServer} callbacks.
 */
public class AnpAlertNotificationServerCallback extends ServerBaseCallback {
    private static final boolean DBG = true;
    private static final String TAG = "AnpAlertNotificationServerCallback";

    @Override
    void dispatchCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
            BluetoothGattCharacteristic characteristic) {
        final UUID charUuid = characteristic.getUuid();
        final UUID srvcUuid = characteristic.getService().getUuid();
        CharacteristicBase base;

        if (srvcUuid.equals(GattUuid.SRVC_ANS)) {
             if (charUuid.equals(GattUuid.CHAR_SUPPORTED_NEW_ALERT_CATEGORY)) {
                base = new SupportedNewAlertCategory(characteristic.getValue(), characteristic);
                this.onAnsSupportedNewAlertCategoryReadRequest(
                        device, requestId, offset, (SupportedNewAlertCategory) base);
                return;
            } else if (charUuid.equals(GattUuid.CHAR_SUPPORTED_UNREAD_ALERT_CATEGORY)) {
                base = new SupportedUnreadAlertCategory(characteristic.getValue(), characteristic);
                this.onAnsSupportedUnreadAlertCategoryReadRequest(
                        device, requestId, offset, (SupportedUnreadAlertCategory) base);
                return;
            }
        }

        sendErrorResponse(device, requestId, BluetoothGatt.GATT_READ_NOT_PERMITTED);
    }

    @Override
    void dispatchCharacteristicWriteRequest(BluetoothDevice device, int requestId,
            BluetoothGattCharacteristic characteristic, boolean preparedWrite,
            boolean responseNeeded, int offset, byte[] value) {
        final UUID charUuid = characteristic.getUuid();
        final UUID srvcUuid = characteristic.getService().getUuid();
        CharacteristicBase base;

        if (srvcUuid.equals(GattUuid.SRVC_ANS)) {
             if (charUuid.equals(GattUuid.CHAR_ALERT_NOTIFICATION_CONTROL_POINT)) {
                base = new AlertNotificationControlPoint(characteristic.getValue(), characteristic);
                this.onAnsAlertNotificationControlPointWriteRequest(
                        device, requestId, (AlertNotificationControlPoint) base,
                        preparedWrite, responseNeeded, offset, value);
                return;
            }
        }

        sendErrorResponse(device, requestId, BluetoothGatt.GATT_WRITE_NOT_PERMITTED);
    }

    @Override
    void dispatchDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
            BluetoothGattDescriptor descriptor) {
        final UUID charUuid = descriptor.getCharacteristic().getUuid();
        final UUID srvcUuid = descriptor.getCharacteristic().getService().getUuid();
        final UUID descrUuid = descriptor.getUuid();

        if (srvcUuid.equals(GattUuid.SRVC_ANS)) {
             if (charUuid.equals(GattUuid.CHAR_NEW_ALERT) &&
                    descrUuid.equals(GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION)) {
                this.onAnsNewAlertCccdReadRequest(
                        device, requestId, offset, descriptor);
                return;
            } else if (charUuid.equals(GattUuid.CHAR_UNREAD_ALERT_STATUS) &&
                    descrUuid.equals(GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION)) {
                this.onAnsUnreadAlertStatusCccdReadRequest(
                        device, requestId, offset, descriptor);
                return;
            }
        }

        sendErrorResponse(device, requestId, BluetoothGatt.GATT_READ_NOT_PERMITTED);
    }

    @Override
    void dispatchDescriptorWriteRequest(BluetoothDevice device, int requestId,
            BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded,
            int offset, byte[] value) {
        final UUID charUuid = descriptor.getCharacteristic().getUuid();
        final UUID srvcUuid = descriptor.getCharacteristic().getService().getUuid();
        final UUID descrUuid = descriptor.getUuid();

        if (srvcUuid.equals(GattUuid.SRVC_ANS)) {
             if (charUuid.equals(GattUuid.CHAR_NEW_ALERT) &&
                    descrUuid.equals(GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION)) {
                this.onAnsNewAlertCccdWriteRequest(
                        device, requestId, descriptor, preparedWrite, responseNeeded, offset,
                        value);
                return;
            } else if (charUuid.equals(GattUuid.CHAR_UNREAD_ALERT_STATUS) &&
                    descrUuid.equals(GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION)) {
                this.onAnsUnreadAlertStatusCccdWriteRequest(
                        device, requestId, descriptor, preparedWrite, responseNeeded, offset,
                        value);
                return;
            }
        }

        sendErrorResponse(device, requestId, BluetoothGatt.GATT_WRITE_NOT_PERMITTED);
    }

    /**
     * A remote client has requested to read Ans:SupportedNewAlertCategory
     * local characteristic.
     *
     * @param device The remote device that has requested the read operation
     * @param requestId The Id of the request
     * @param offset Offset into the value of the characteristic
     * @param supportedNewAlertCategory Characteristic to be read
     */
    public void onAnsSupportedNewAlertCategoryReadRequest(
            BluetoothDevice device, int requestId, int offset,
            SupportedNewAlertCategory supportedNewAlertCategory) {
        if (DBG) {
            Log.d(TAG, "onAnsSupportedNewAlertCategoryReadRequest(): offset=" + offset);
        }

        sendResponse(device, requestId, offset,
                supportedNewAlertCategory.getValue(offset));
    }

    /**
     * A remote client has requested to read Ans:SupportedUnreadAlertCategory
     * local characteristic.
     *
     * @param device The remote device that has requested the read operation
     * @param requestId The Id of the request
     * @param offset Offset into the value of the characteristic
     * @param supportedUnreadAlertCategory Characteristic to be read
     */
    public void onAnsSupportedUnreadAlertCategoryReadRequest(
            BluetoothDevice device, int requestId, int offset,
            SupportedUnreadAlertCategory supportedUnreadAlertCategory) {
        if (DBG) {
            Log.d(TAG, "onAnsSupportedUnreadAlertCategoryReadRequest(): offset=" + offset);
        }

        sendResponse(device, requestId, offset,
                supportedUnreadAlertCategory.getValue(offset));
    }


    /**
     * A remote client has requested to write to Ans:AlertNotificationControlPoint
     * local characteristic.
     *
     * @param device The remote device that has requested the write operation
     * @param requestId The Id of the request
     * @param alertNotificationControlPoint Characteristic to be written to.
     * @param preparedWrite true, if this write operation should be queued for
     *                      later execution.
     * @param responseNeeded true, if the remote device requires a response
     * @param offset The offset given for the value
     * @param value The value the client wants to assign to the characteristic
     */
    public void onAnsAlertNotificationControlPointWriteRequest(
            BluetoothDevice device, int requestId,
            AlertNotificationControlPoint alertNotificationControlPoint,
            boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
        if (DBG) {
            Log.d(TAG, "onAnsAlertNotificationControlPointWriteRequest()");
        }

        if (preparedWrite) {
            prepareWrite(device, alertNotificationControlPoint, offset, value, false);
            if (responseNeeded) {
                sendResponse(device, requestId, offset, value);
            }
            return;
        }

        alertNotificationControlPoint.setValue(offset, value);
        if (responseNeeded) {
            sendResponse(device, requestId, offset, null);
        }
    }


    /**
     * A remote client has requested to read Ans:NewAlert:
     * Cccd local descriptor.
     *
     * @param device The remote device that has requested the read operation
     * @param requestId The Id of the request
     * @param offset Offset into the value of the characteristic
     * @param descriptor Descriptor to be read
     */
    public void onAnsNewAlertCccdReadRequest(
            BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
        if (DBG) {
            Log.d(TAG, "onAnsNewAlertCccdReadRequest()");
        }

        sendResponse(device, requestId, offset, descriptor.getValue());
    }

    /**
     * A remote client has requested to read Ans:UnreadAlertStatus:
     * Cccd local descriptor.
     *
     * @param device The remote device that has requested the read operation
     * @param requestId The Id of the request
     * @param offset Offset into the value of the characteristic
     * @param descriptor Descriptor to be read
     */
    public void onAnsUnreadAlertStatusCccdReadRequest(
            BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
        if (DBG) {
            Log.d(TAG, "onAnsUnreadAlertStatusCccdReadRequest()");
        }

        sendResponse(device, requestId, offset, descriptor.getValue());
    }


    /**
     * A remote client has requested to write Ans:NewAlert:
     * Cccd local descriptor.
     *
     * @param device The remote device that has requested the write operation
     * @param requestId The Id of the request
     * @param descriptor Descriptor to be written to.
     * @param preparedWrite true, if this write operation should be queued for
     *                      later execution.
     * @param responseNeeded true, if the remote device requires a response
     * @param offset The offset given for the value
     * @param value The value the client wants to assign to the descriptor
     */
    public void onAnsNewAlertCccdWriteRequest(
            BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor,
            boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
        if (DBG) {
            Log.d(TAG, "onAnsNewAlertCccdWriteRequest()");
        }

        if (preparedWrite) {
            prepareWrite(device, descriptor, offset, value, true);
            if (responseNeeded) {
                sendResponse(device, requestId, offset, value);
            }
            return;
        }

        if (!updateCccd(device, descriptor, value)) {
            if (responseNeeded) {
                sendErrorResponse(device, requestId, BluetoothGatt.GATT_FAILURE);
            }
            return;
        }

        descriptor.setValue(value);
        if (responseNeeded) {
            sendResponse(device, requestId, offset, null);
        }
    }

    /**
     * A remote client has requested to write Ans:UnreadAlertStatus:
     * Cccd local descriptor.
     *
     * @param device The remote device that has requested the write operation
     * @param requestId The Id of the request
     * @param descriptor Descriptor to be written to.
     * @param preparedWrite true, if this write operation should be queued for
     *                      later execution.
     * @param responseNeeded true, if the remote device requires a response
     * @param offset The offset given for the value
     * @param value The value the client wants to assign to the descriptor
     */
    public void onAnsUnreadAlertStatusCccdWriteRequest(
            BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor,
            boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
        if (DBG) {
            Log.d(TAG, "onAnsUnreadAlertStatusCccdWriteRequest()");
        }

        if (preparedWrite) {
            prepareWrite(device, descriptor, offset, value, true);
            if (responseNeeded) {
                sendResponse(device, requestId, offset, value);
            }
            return;
        }

        if (!updateCccd(device, descriptor, value)) {
            if (responseNeeded) {
                sendErrorResponse(device, requestId, BluetoothGatt.GATT_FAILURE);
            }
            return;
        }

        descriptor.setValue(value);
        if (responseNeeded) {
            sendResponse(device, requestId, offset, null);
        }
    }

}

