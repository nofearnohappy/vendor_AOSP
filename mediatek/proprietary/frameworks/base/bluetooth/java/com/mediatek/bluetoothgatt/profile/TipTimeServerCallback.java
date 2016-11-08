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
 * This class is used to implement {@link TipTimeServer} callbacks.
 */
public class TipTimeServerCallback extends ServerBaseCallback {
    private static final boolean DBG = true;
    private static final String TAG = "TipTimeServerCallback";

    @Override
    void dispatchCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
            BluetoothGattCharacteristic characteristic) {
        final UUID charUuid = characteristic.getUuid();
        final UUID srvcUuid = characteristic.getService().getUuid();
        CharacteristicBase base;

        if (srvcUuid.equals(GattUuid.SRVC_CTS)) {
             if (charUuid.equals(GattUuid.CHAR_CURRENT_TIME)) {
                base = new CurrentTime(characteristic.getValue(), characteristic);
                this.onCtsCurrentTimeReadRequest(
                        device, requestId, offset, (CurrentTime) base);
                return;
            } else if (charUuid.equals(GattUuid.CHAR_LOCAL_TIME_INFORMATION)) {
                base = new LocalTimeInformation(characteristic.getValue(), characteristic);
                this.onCtsLocalTimeInformationReadRequest(
                        device, requestId, offset, (LocalTimeInformation) base);
                return;
            } else if (charUuid.equals(GattUuid.CHAR_REFERENCE_TIME_INFORMATION)) {
                base = new ReferenceTimeInformation(characteristic.getValue(), characteristic);
                this.onCtsReferenceTimeInformationReadRequest(
                        device, requestId, offset, (ReferenceTimeInformation) base);
                return;
            }
        }
        if (srvcUuid.equals(GattUuid.SRVC_NDCS)) {
             if (charUuid.equals(GattUuid.CHAR_TIME_WITH_DST)) {
                base = new TimeWithDst(characteristic.getValue(), characteristic);
                this.onNdcsTimeWithDstReadRequest(
                        device, requestId, offset, (TimeWithDst) base);
                return;
            }
        }
        if (srvcUuid.equals(GattUuid.SRVC_RTUS)) {
             if (charUuid.equals(GattUuid.CHAR_TIME_UPDATE_STATE)) {
                base = new TimeUpdateState(characteristic.getValue(), characteristic);
                this.onRtusTimeUpdateStateReadRequest(
                        device, requestId, offset, (TimeUpdateState) base);
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

        if (srvcUuid.equals(GattUuid.SRVC_CTS)) {
             if (charUuid.equals(GattUuid.CHAR_CURRENT_TIME)) {
                base = new CurrentTime(characteristic.getValue(), characteristic);
                this.onCtsCurrentTimeWriteRequest(
                        device, requestId, (CurrentTime) base,
                        preparedWrite, responseNeeded, offset, value);
                return;
            } else if (charUuid.equals(GattUuid.CHAR_LOCAL_TIME_INFORMATION)) {
                base = new LocalTimeInformation(characteristic.getValue(), characteristic);
                this.onCtsLocalTimeInformationWriteRequest(
                        device, requestId, (LocalTimeInformation) base,
                        preparedWrite, responseNeeded, offset, value);
                return;
            }
        }
        if (srvcUuid.equals(GattUuid.SRVC_RTUS)) {
             if (charUuid.equals(GattUuid.CHAR_TIME_UPDATE_CONTROL_POINT)) {
                base = new TimeUpdateControlPoint(characteristic.getValue(), characteristic);
                this.onRtusTimeUpdateControlPointWriteRequest(
                        device, requestId, (TimeUpdateControlPoint) base,
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

        if (srvcUuid.equals(GattUuid.SRVC_CTS)) {
             if (charUuid.equals(GattUuid.CHAR_CURRENT_TIME) &&
                    descrUuid.equals(GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION)) {
                this.onCtsCurrentTimeCccdReadRequest(
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

        if (srvcUuid.equals(GattUuid.SRVC_CTS)) {
             if (charUuid.equals(GattUuid.CHAR_CURRENT_TIME) &&
                    descrUuid.equals(GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION)) {
                this.onCtsCurrentTimeCccdWriteRequest(
                        device, requestId, descriptor, preparedWrite, responseNeeded, offset,
                        value);
                return;
            }
        }

        sendErrorResponse(device, requestId, BluetoothGatt.GATT_WRITE_NOT_PERMITTED);
    }

    /**
     * A remote client has requested to read Cts:CurrentTime
     * local characteristic.
     *
     * @param device The remote device that has requested the read operation
     * @param requestId The Id of the request
     * @param offset Offset into the value of the characteristic
     * @param currentTime Characteristic to be read
     */
    public void onCtsCurrentTimeReadRequest(
            BluetoothDevice device, int requestId, int offset,
            CurrentTime currentTime) {
        if (DBG) {
            Log.d(TAG, "onCtsCurrentTimeReadRequest(): offset=" + offset);
        }

        sendResponse(device, requestId, offset,
                currentTime.getValue(offset));
    }

    /**
     * A remote client has requested to read Cts:LocalTimeInformation
     * local characteristic.
     *
     * @param device The remote device that has requested the read operation
     * @param requestId The Id of the request
     * @param offset Offset into the value of the characteristic
     * @param localTimeInformation Characteristic to be read
     */
    public void onCtsLocalTimeInformationReadRequest(
            BluetoothDevice device, int requestId, int offset,
            LocalTimeInformation localTimeInformation) {
        if (DBG) {
            Log.d(TAG, "onCtsLocalTimeInformationReadRequest(): offset=" + offset);
        }

        sendResponse(device, requestId, offset,
                localTimeInformation.getValue(offset));
    }

    /**
     * A remote client has requested to read Cts:ReferenceTimeInformation
     * local characteristic.
     *
     * @param device The remote device that has requested the read operation
     * @param requestId The Id of the request
     * @param offset Offset into the value of the characteristic
     * @param referenceTimeInformation Characteristic to be read
     */
    public void onCtsReferenceTimeInformationReadRequest(
            BluetoothDevice device, int requestId, int offset,
            ReferenceTimeInformation referenceTimeInformation) {
        if (DBG) {
            Log.d(TAG, "onCtsReferenceTimeInformationReadRequest(): offset=" + offset);
        }

        sendResponse(device, requestId, offset,
                referenceTimeInformation.getValue(offset));
    }

    /**
     * A remote client has requested to read Ndcs:TimeWithDst
     * local characteristic.
     *
     * @param device The remote device that has requested the read operation
     * @param requestId The Id of the request
     * @param offset Offset into the value of the characteristic
     * @param timeWithDst Characteristic to be read
     */
    public void onNdcsTimeWithDstReadRequest(
            BluetoothDevice device, int requestId, int offset,
            TimeWithDst timeWithDst) {
        if (DBG) {
            Log.d(TAG, "onNdcsTimeWithDstReadRequest(): offset=" + offset);
        }

        sendResponse(device, requestId, offset,
                timeWithDst.getValue(offset));
    }

    /**
     * A remote client has requested to read Rtus:TimeUpdateState
     * local characteristic.
     *
     * @param device The remote device that has requested the read operation
     * @param requestId The Id of the request
     * @param offset Offset into the value of the characteristic
     * @param timeUpdateState Characteristic to be read
     */
    public void onRtusTimeUpdateStateReadRequest(
            BluetoothDevice device, int requestId, int offset,
            TimeUpdateState timeUpdateState) {
        if (DBG) {
            Log.d(TAG, "onRtusTimeUpdateStateReadRequest(): offset=" + offset);
        }

        sendResponse(device, requestId, offset,
                timeUpdateState.getValue(offset));
    }


    /**
     * A remote client has requested to write to Cts:CurrentTime
     * local characteristic.
     *
     * @param device The remote device that has requested the write operation
     * @param requestId The Id of the request
     * @param currentTime Characteristic to be written to.
     * @param preparedWrite true, if this write operation should be queued for
     *                      later execution.
     * @param responseNeeded true, if the remote device requires a response
     * @param offset The offset given for the value
     * @param value The value the client wants to assign to the characteristic
     */
    public void onCtsCurrentTimeWriteRequest(
            BluetoothDevice device, int requestId,
            CurrentTime currentTime,
            boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
        if (DBG) {
            Log.d(TAG, "onCtsCurrentTimeWriteRequest()");
        }

        if (preparedWrite) {
            prepareWrite(device, currentTime, offset, value, false);
            if (responseNeeded) {
                sendResponse(device, requestId, offset, value);
            }
            return;
        }

        currentTime.setValue(offset, value);
        if (responseNeeded) {
            sendResponse(device, requestId, offset, null);
        }
    }

    /**
     * A remote client has requested to write to Cts:LocalTimeInformation
     * local characteristic.
     *
     * @param device The remote device that has requested the write operation
     * @param requestId The Id of the request
     * @param localTimeInformation Characteristic to be written to.
     * @param preparedWrite true, if this write operation should be queued for
     *                      later execution.
     * @param responseNeeded true, if the remote device requires a response
     * @param offset The offset given for the value
     * @param value The value the client wants to assign to the characteristic
     */
    public void onCtsLocalTimeInformationWriteRequest(
            BluetoothDevice device, int requestId,
            LocalTimeInformation localTimeInformation,
            boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
        if (DBG) {
            Log.d(TAG, "onCtsLocalTimeInformationWriteRequest()");
        }

        if (preparedWrite) {
            prepareWrite(device, localTimeInformation, offset, value, false);
            if (responseNeeded) {
                sendResponse(device, requestId, offset, value);
            }
            return;
        }

        localTimeInformation.setValue(offset, value);
        if (responseNeeded) {
            sendResponse(device, requestId, offset, null);
        }
    }

    /**
     * A remote client has requested to write to Rtus:TimeUpdateControlPoint
     * local characteristic.
     *
     * @param device The remote device that has requested the write operation
     * @param requestId The Id of the request
     * @param timeUpdateControlPoint Characteristic to be written to.
     * @param preparedWrite true, if this write operation should be queued for
     *                      later execution.
     * @param responseNeeded true, if the remote device requires a response
     * @param offset The offset given for the value
     * @param value The value the client wants to assign to the characteristic
     */
    public void onRtusTimeUpdateControlPointWriteRequest(
            BluetoothDevice device, int requestId,
            TimeUpdateControlPoint timeUpdateControlPoint,
            boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
        if (DBG) {
            Log.d(TAG, "onRtusTimeUpdateControlPointWriteRequest()");
        }

        if (preparedWrite) {
            prepareWrite(device, timeUpdateControlPoint, offset, value, false);
            if (responseNeeded) {
                sendResponse(device, requestId, offset, value);
            }
            return;
        }

        timeUpdateControlPoint.setValue(offset, value);
        if (responseNeeded) {
            sendResponse(device, requestId, offset, null);
        }
    }


    /**
     * A remote client has requested to read Cts:CurrentTime:
     * Cccd local descriptor.
     *
     * @param device The remote device that has requested the read operation
     * @param requestId The Id of the request
     * @param offset Offset into the value of the characteristic
     * @param descriptor Descriptor to be read
     */
    public void onCtsCurrentTimeCccdReadRequest(
            BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
        if (DBG) {
            Log.d(TAG, "onCtsCurrentTimeCccdReadRequest()");
        }

        sendResponse(device, requestId, offset, descriptor.getValue());
    }


    /**
     * A remote client has requested to write Cts:CurrentTime:
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
    public void onCtsCurrentTimeCccdWriteRequest(
            BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor,
            boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
        if (DBG) {
            Log.d(TAG, "onCtsCurrentTimeCccdWriteRequest()");
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

