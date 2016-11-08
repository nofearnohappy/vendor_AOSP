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
import com.mediatek.bluetoothgatt.characteristic.CharacteristicBase;
import com.mediatek.bluetoothgatt.characteristic.CurrentTime;
import com.mediatek.bluetoothgatt.characteristic.LocalTimeInformation;
import com.mediatek.bluetoothgatt.characteristic.ReferenceTimeInformation;
import com.mediatek.bluetoothgatt.characteristic.TimeUpdateControlPoint;
import com.mediatek.bluetoothgatt.characteristic.TimeUpdateState;
import com.mediatek.bluetoothgatt.characteristic.TimeWithDst;

import java.util.UUID;
//........................................................................ Customized End: Import //

/**
 * This class is used to implement {@link TipTimeClient} callbacks.
 */
public class TipTimeClientCallback extends ClientBaseCallback {
    private static final boolean DBG = true;
    private static final String TAG = "TipTimeClientCallback";

    @Override
    void dispatchCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
            int status) {
        final UUID charUuid = characteristic.getUuid();
        final UUID srvcUuid = characteristic.getService().getUuid();
        CharacteristicBase base;

        if (srvcUuid.equals(GattUuid.SRVC_CTS)) {
             if (charUuid.equals(GattUuid.CHAR_CURRENT_TIME)) {
                base = new CurrentTime();
                base.setCharacteristic(characteristic);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    base.setValue(characteristic.getValue());
                }
                this.onCtsCurrentTimeReadResponse(
                        (CurrentTime) base, status);
                return;
            } else if (charUuid.equals(GattUuid.CHAR_LOCAL_TIME_INFORMATION)) {
                base = new LocalTimeInformation();
                base.setCharacteristic(characteristic);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    base.setValue(characteristic.getValue());
                }
                this.onCtsLocalTimeInformationReadResponse(
                        (LocalTimeInformation) base, status);
                return;
            } else if (charUuid.equals(GattUuid.CHAR_REFERENCE_TIME_INFORMATION)) {
                base = new ReferenceTimeInformation();
                base.setCharacteristic(characteristic);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    base.setValue(characteristic.getValue());
                }
                this.onCtsReferenceTimeInformationReadResponse(
                        (ReferenceTimeInformation) base, status);
                return;
            }
        }
        if (srvcUuid.equals(GattUuid.SRVC_NDCS)) {
             if (charUuid.equals(GattUuid.CHAR_TIME_WITH_DST)) {
                base = new TimeWithDst();
                base.setCharacteristic(characteristic);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    base.setValue(characteristic.getValue());
                }
                this.onNdcsTimeWithDstReadResponse(
                        (TimeWithDst) base, status);
                return;
            }
        }
        if (srvcUuid.equals(GattUuid.SRVC_RTUS)) {
             if (charUuid.equals(GattUuid.CHAR_TIME_UPDATE_STATE)) {
                base = new TimeUpdateState();
                base.setCharacteristic(characteristic);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    base.setValue(characteristic.getValue());
                }
                this.onRtusTimeUpdateStateReadResponse(
                        (TimeUpdateState) base, status);
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

        if (srvcUuid.equals(GattUuid.SRVC_CTS)) {
             if (charUuid.equals(GattUuid.CHAR_CURRENT_TIME)) {
                base = new CurrentTime();
                base.setCharacteristic(characteristic);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    base.setValue(characteristic.getValue());
                }
                this.onCtsCurrentTimeWriteResponse(
                        (CurrentTime) base, status);
                return;
            } else if (charUuid.equals(GattUuid.CHAR_LOCAL_TIME_INFORMATION)) {
                base = new LocalTimeInformation();
                base.setCharacteristic(characteristic);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    base.setValue(characteristic.getValue());
                }
                this.onCtsLocalTimeInformationWriteResponse(
                        (LocalTimeInformation) base, status);
                return;
            }
        }
        if (srvcUuid.equals(GattUuid.SRVC_RTUS)) {
             if (charUuid.equals(GattUuid.CHAR_TIME_UPDATE_CONTROL_POINT)) {
                base = new TimeUpdateControlPoint();
                base.setCharacteristic(characteristic);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    base.setValue(characteristic.getValue());
                }
                this.onRtusTimeUpdateControlPointWriteResponse(
                        (TimeUpdateControlPoint) base, status);
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

        if (srvcUuid.equals(GattUuid.SRVC_CTS)) {
             if (charUuid.equals(GattUuid.CHAR_CURRENT_TIME)) {
                base = new CurrentTime(characteristic.getValue(), characteristic);
                this.onCtsCurrentTimeNotify(
                        (CurrentTime) base);
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

        if (srvcUuid.equals(GattUuid.SRVC_CTS)) {
             if (charUuid.equals(GattUuid.CHAR_CURRENT_TIME) &&
                    descrUuid.equals(GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION)) {
                this.onCtsCurrentTimeCccdReadResponse(descriptor, status);
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

        if (srvcUuid.equals(GattUuid.SRVC_CTS)) {
             if (charUuid.equals(GattUuid.CHAR_CURRENT_TIME) &&
                    descrUuid.equals(GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION)) {
                this.onCtsCurrentTimeCccdWriteResponse(descriptor, status);
                return;
            }
        }

        if (DBG) {
            Log.d(TAG, "Unknown Descriptor UUID=" + descrUuid);
        }
    }

    /**
     * Callback reporting the result of a
     * Cts:CurrentTime characteristic read operation.
     *
     * @param currentTime Cts:CurrentTime characteristic
     *                       that was read from the associated remote device.
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation
     *               was completed successfully.
     */
    public void onCtsCurrentTimeReadResponse(
            CurrentTime currentTime, int status) {
    }

    /**
     * Callback reporting the result of a
     * Cts:LocalTimeInformation characteristic read operation.
     *
     * @param localTimeInformation Cts:LocalTimeInformation characteristic
     *                       that was read from the associated remote device.
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation
     *               was completed successfully.
     */
    public void onCtsLocalTimeInformationReadResponse(
            LocalTimeInformation localTimeInformation, int status) {
    }

    /**
     * Callback reporting the result of a
     * Cts:ReferenceTimeInformation characteristic read operation.
     *
     * @param referenceTimeInformation Cts:ReferenceTimeInformation characteristic
     *                       that was read from the associated remote device.
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation
     *               was completed successfully.
     */
    public void onCtsReferenceTimeInformationReadResponse(
            ReferenceTimeInformation referenceTimeInformation, int status) {
    }

    /**
     * Callback reporting the result of a
     * Ndcs:TimeWithDst characteristic read operation.
     *
     * @param timeWithDst Ndcs:TimeWithDst characteristic
     *                       that was read from the associated remote device.
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation
     *               was completed successfully.
     */
    public void onNdcsTimeWithDstReadResponse(
            TimeWithDst timeWithDst, int status) {
    }

    /**
     * Callback reporting the result of a
     * Rtus:TimeUpdateState characteristic read operation.
     *
     * @param timeUpdateState Rtus:TimeUpdateState characteristic
     *                       that was read from the associated remote device.
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation
     *               was completed successfully.
     */
    public void onRtusTimeUpdateStateReadResponse(
            TimeUpdateState timeUpdateState, int status) {
    }


    /**
     * Callback indicating the result of a
     * Cts:CurrentTime characteristic write operation.
     *
     * @param currentTime Cts:CurrentTime characteristic
     *                       that was written to the associated remote device.
     * @param status The result of the write operation
     *               {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
     */
    public void onCtsCurrentTimeWriteResponse(
            CurrentTime currentTime, int status) {
    }

    /**
     * Callback indicating the result of a
     * Cts:LocalTimeInformation characteristic write operation.
     *
     * @param localTimeInformation Cts:LocalTimeInformation characteristic
     *                       that was written to the associated remote device.
     * @param status The result of the write operation
     *               {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
     */
    public void onCtsLocalTimeInformationWriteResponse(
            LocalTimeInformation localTimeInformation, int status) {
    }

    /**
     * Callback indicating the result of a
     * Rtus:TimeUpdateControlPoint characteristic write operation.
     *
     * @param timeUpdateControlPoint Rtus:TimeUpdateControlPoint characteristic
     *                       that was written to the associated remote device.
     * @param status The result of the write operation
     *               {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
     */
    public void onRtusTimeUpdateControlPointWriteResponse(
            TimeUpdateControlPoint timeUpdateControlPoint, int status) {
    }


    /**
     * Callback reporting the result of a
     * Cts:CurrentTime:Cccd descriptor read operation.
     *
     * @param descriptor Descriptor that was read from the associated
     *                   remote device.
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation
     *               was completed successfully
     */
    public void onCtsCurrentTimeCccdReadResponse(
            BluetoothGattDescriptor descriptor, int status) {
    }


    /**
     * Callback indicating the result of a
     * Cts:CurrentTime:Cccd descriptor write operation.
     *
     * @param descriptor Descriptor that was written to the associated
     *                   remote device.
     * @param status The result of the write operation
     *               {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
     */
    public void onCtsCurrentTimeCccdWriteResponse(
            BluetoothGattDescriptor descriptor, int status) {
    }


    /**
     * Callback triggered as a result of a remote
     * Cts:CurrentTime characteristic notification.
     *
     * @param currentTime Characteristic that has been updated as a result
     *                       of a remote notification event.
     */
    public void onCtsCurrentTimeNotify(
            CurrentTime currentTime) {
    }

}

