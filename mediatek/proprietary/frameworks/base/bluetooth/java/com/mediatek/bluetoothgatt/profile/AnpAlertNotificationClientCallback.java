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
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Log;

import com.mediatek.bluetoothgatt.GattUuid;
import com.mediatek.bluetoothgatt.characteristic.AlertNotificationControlPoint;
import com.mediatek.bluetoothgatt.characteristic.CharacteristicBase;
import com.mediatek.bluetoothgatt.characteristic.NewAlert;
import com.mediatek.bluetoothgatt.characteristic.SupportedNewAlertCategory;
import com.mediatek.bluetoothgatt.characteristic.SupportedUnreadAlertCategory;
import com.mediatek.bluetoothgatt.characteristic.UnreadAlertStatus;

import java.util.UUID;
//........................................................................ Customized End: Import //

/**
 * This class is used to implement {@link AnpAlertNotificationClient} callbacks.
 */
public class AnpAlertNotificationClientCallback extends ClientBaseCallback {
    private static final boolean DBG = true;
    private static final String TAG = "AnpAlertNotificationClientCallback";

    @Override
    void dispatchCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
            int status) {
        final UUID charUuid = characteristic.getUuid();
        final UUID srvcUuid = characteristic.getService().getUuid();
        CharacteristicBase base;

        if (srvcUuid.equals(GattUuid.SRVC_ANS)) {
             if (charUuid.equals(GattUuid.CHAR_SUPPORTED_NEW_ALERT_CATEGORY)) {
                base = new SupportedNewAlertCategory();
                base.setCharacteristic(characteristic);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    base.setValue(characteristic.getValue());
                }
                this.onAnsSupportedNewAlertCategoryReadResponse(
                        (SupportedNewAlertCategory) base, status);
                return;
            } else if (charUuid.equals(GattUuid.CHAR_SUPPORTED_UNREAD_ALERT_CATEGORY)) {
                base = new SupportedUnreadAlertCategory();
                base.setCharacteristic(characteristic);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    base.setValue(characteristic.getValue());
                }
                this.onAnsSupportedUnreadAlertCategoryReadResponse(
                        (SupportedUnreadAlertCategory) base, status);
                return;
            }
        }

        if (DBG) {
            Log.d(TAG, "Unknown Characteristic UUID=" + charUuid);
        }
    }

    @Override
    void dispatchCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
            int status) {
        final UUID charUuid = characteristic.getUuid();
        final UUID srvcUuid = characteristic.getService().getUuid();
        CharacteristicBase base;

        if (srvcUuid.equals(GattUuid.SRVC_ANS)) {
             if (charUuid.equals(GattUuid.CHAR_ALERT_NOTIFICATION_CONTROL_POINT)) {
                base = new AlertNotificationControlPoint();
                base.setCharacteristic(characteristic);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    base.setValue(characteristic.getValue());
                }
                this.onAnsAlertNotificationControlPointWriteResponse(
                        (AlertNotificationControlPoint) base, status);
                return;
            }
        }

        if (DBG) {
            Log.d(TAG, "Unknown Characteristic UUID=" + charUuid);
        }
    }

    @Override
    void dispatchCharacteristicChanged(BluetoothGatt gatt,
            BluetoothGattCharacteristic characteristic) {
        final UUID charUuid = characteristic.getUuid();
        final UUID srvcUuid = characteristic.getService().getUuid();
        CharacteristicBase base;

        if (srvcUuid.equals(GattUuid.SRVC_ANS)) {
             if (charUuid.equals(GattUuid.CHAR_NEW_ALERT)) {
                base = new NewAlert(characteristic.getValue(), characteristic);
                this.onAnsNewAlertNotify(
                        (NewAlert) base);
                return;
            } else if (charUuid.equals(GattUuid.CHAR_UNREAD_ALERT_STATUS)) {
                base = new UnreadAlertStatus(characteristic.getValue(), characteristic);
                this.onAnsUnreadAlertStatusNotify(
                        (UnreadAlertStatus) base);
                return;
            }
        }

        if (DBG) {
            Log.d(TAG, "Unknown Characteristic UUID=" + charUuid);
        }
    }

    @Override
    void dispatchDescriptorRead(BluetoothGatt gatt,
            BluetoothGattDescriptor descriptor, int status) {
        final UUID charUuid = descriptor.getCharacteristic().getUuid();
        final UUID srvcUuid = descriptor.getCharacteristic().getService().getUuid();
        final UUID descrUuid = descriptor.getUuid();

        if (srvcUuid.equals(GattUuid.SRVC_ANS)) {
             if (charUuid.equals(GattUuid.CHAR_NEW_ALERT) &&
                    descrUuid.equals(GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION)) {
                this.onAnsNewAlertCccdReadResponse(descriptor, status);
                return;
            } else if (charUuid.equals(GattUuid.CHAR_UNREAD_ALERT_STATUS) &&
                    descrUuid.equals(GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION)) {
                this.onAnsUnreadAlertStatusCccdReadResponse(descriptor, status);
                return;
            }
        }

        if (DBG) {
            Log.d(TAG, "Unknown Descriptor UUID=" + descrUuid);
        }
    }

    @Override
    void dispatchDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
            int status) {
        final UUID charUuid = descriptor.getCharacteristic().getUuid();
        final UUID srvcUuid = descriptor.getCharacteristic().getService().getUuid();
        final UUID descrUuid = descriptor.getUuid();

        if (srvcUuid.equals(GattUuid.SRVC_ANS)) {
             if (charUuid.equals(GattUuid.CHAR_NEW_ALERT) &&
                    descrUuid.equals(GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION)) {
                this.onAnsNewAlertCccdWriteResponse(descriptor, status);
                return;
            } else if (charUuid.equals(GattUuid.CHAR_UNREAD_ALERT_STATUS) &&
                    descrUuid.equals(GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION)) {
                this.onAnsUnreadAlertStatusCccdWriteResponse(descriptor, status);
                return;
            }
        }

        if (DBG) {
            Log.d(TAG, "Unknown Descriptor UUID=" + descrUuid);
        }
    }

    /**
     * Callback reporting the result of a
     * Ans:SupportedNewAlertCategory characteristic read operation.
     *
     * @param supportedNewAlertCategory Ans:SupportedNewAlertCategory characteristic
     *                       that was read from the associated remote device.
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation
     *               was completed successfully.
     */
    public void onAnsSupportedNewAlertCategoryReadResponse(
            SupportedNewAlertCategory supportedNewAlertCategory, int status) {
    }

    /**
     * Callback reporting the result of a
     * Ans:SupportedUnreadAlertCategory characteristic read operation.
     *
     * @param supportedUnreadAlertCategory Ans:SupportedUnreadAlertCategory characteristic
     *                       that was read from the associated remote device.
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation
     *               was completed successfully.
     */
    public void onAnsSupportedUnreadAlertCategoryReadResponse(
            SupportedUnreadAlertCategory supportedUnreadAlertCategory, int status) {
    }


    /**
     * Callback indicating the result of a
     * Ans:AlertNotificationControlPoint characteristic write operation.
     *
     * @param alertNotificationControlPoint Ans:AlertNotificationControlPoint characteristic
     *                       that was written to the associated remote device.
     * @param status The result of the write operation
     *               {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
     */
    public void onAnsAlertNotificationControlPointWriteResponse(
            AlertNotificationControlPoint alertNotificationControlPoint, int status) {
    }


    /**
     * Callback reporting the result of a
     * Ans:NewAlert:Cccd descriptor read operation.
     *
     * @param descriptor Descriptor that was read from the associated
     *                   remote device.
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation
     *               was completed successfully
     */
    public void onAnsNewAlertCccdReadResponse(
            BluetoothGattDescriptor descriptor, int status) {
    }

    /**
     * Callback reporting the result of a
     * Ans:UnreadAlertStatus:Cccd descriptor read operation.
     *
     * @param descriptor Descriptor that was read from the associated
     *                   remote device.
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation
     *               was completed successfully
     */
    public void onAnsUnreadAlertStatusCccdReadResponse(
            BluetoothGattDescriptor descriptor, int status) {
    }


    /**
     * Callback indicating the result of a
     * Ans:NewAlert:Cccd descriptor write operation.
     *
     * @param descriptor Descriptor that was written to the associated
     *                   remote device.
     * @param status The result of the write operation
     *               {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
     */
    public void onAnsNewAlertCccdWriteResponse(
            BluetoothGattDescriptor descriptor, int status) {
    }

    /**
     * Callback indicating the result of a
     * Ans:UnreadAlertStatus:Cccd descriptor write operation.
     *
     * @param descriptor Descriptor that was written to the associated
     *                   remote device.
     * @param status The result of the write operation
     *               {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
     */
    public void onAnsUnreadAlertStatusCccdWriteResponse(
            BluetoothGattDescriptor descriptor, int status) {
    }


    /**
     * Callback triggered as a result of a remote
     * Ans:NewAlert characteristic notification.
     *
     * @param newAlert Characteristic that has been updated as a result
     *                       of a remote notification event.
     */
    public void onAnsNewAlertNotify(
            NewAlert newAlert) {
    }

    /**
     * Callback triggered as a result of a remote
     * Ans:UnreadAlertStatus characteristic notification.
     *
     * @param unreadAlertStatus Characteristic that has been updated as a result
     *                       of a remote notification event.
     */
    public void onAnsUnreadAlertStatusNotify(
            UnreadAlertStatus unreadAlertStatus) {
    }

}

