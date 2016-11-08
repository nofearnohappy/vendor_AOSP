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
 * Public API for the GATT Time Profile(Client).
 *
 * Name: Time
 * Type: org.bluetooth.profile.time
 * Last Modified: None
 * Revision: None
 * Role: TimeClient
 */
public class TipTimeClient extends ClientBase {
    private static final boolean DBG = true;
    private static final String TAG = "TipTimeClient";

    /**
     * Create a TipTimeClient object and init value.
     *
     * @param context App context
     */
    public TipTimeClient(Context context) {
        super(context);

        if (DBG) {
            Log.d(TAG, "TipTimeClient()");
        }
    }

    @Override
    protected boolean handleServicesDiscovered(BluetoothGatt gatt, int status) {
        // Check mandatory service is exist.
        return true &&
                (gatt.getService(GattUuid.SRVC_CTS) != null);
    }

    /**
     * Reads Cts:CurrentTime characteristic
     * from the associated remote device.
     *
     * @return true, if the read operation was initiated successfully
     */
    public boolean readCtsCurrentTime() {
        if (DBG) {
            Log.d(TAG, "readCtsCurrentTime()");
        }
        return readCharacteristic(
                GattUuid.SRVC_CTS,
                GattUuid.CHAR_CURRENT_TIME);
    }

    /**
     * Reads Cts:LocalTimeInformation characteristic
     * from the associated remote device.
     *
     * @return true, if the read operation was initiated successfully
     */
    public boolean readCtsLocalTimeInformation() {
        if (DBG) {
            Log.d(TAG, "readCtsLocalTimeInformation()");
        }
        return readCharacteristic(
                GattUuid.SRVC_CTS,
                GattUuid.CHAR_LOCAL_TIME_INFORMATION);
    }

    /**
     * Reads Cts:ReferenceTimeInformation characteristic
     * from the associated remote device.
     *
     * @return true, if the read operation was initiated successfully
     */
    public boolean readCtsReferenceTimeInformation() {
        if (DBG) {
            Log.d(TAG, "readCtsReferenceTimeInformation()");
        }
        return readCharacteristic(
                GattUuid.SRVC_CTS,
                GattUuid.CHAR_REFERENCE_TIME_INFORMATION);
    }

    /**
     * Reads Ndcs:TimeWithDst characteristic
     * from the associated remote device.
     *
     * @return true, if the read operation was initiated successfully
     */
    public boolean readNdcsTimeWithDst() {
        if (DBG) {
            Log.d(TAG, "readNdcsTimeWithDst()");
        }
        return readCharacteristic(
                GattUuid.SRVC_NDCS,
                GattUuid.CHAR_TIME_WITH_DST);
    }

    /**
     * Reads Rtus:TimeUpdateState characteristic
     * from the associated remote device.
     *
     * @return true, if the read operation was initiated successfully
     */
    public boolean readRtusTimeUpdateState() {
        if (DBG) {
            Log.d(TAG, "readRtusTimeUpdateState()");
        }
        return readCharacteristic(
                GattUuid.SRVC_RTUS,
                GattUuid.CHAR_TIME_UPDATE_STATE);
    }


    /**
     * Writes Cts:CurrentTime characteristic and its values
     * to the associated remote device.
     *
     * @param characteristic Characteristic to write on the remote device
     * @return true, if the write operation was initiated successfully
     */
    public boolean writeCtsCurrentTime(CharacteristicBase characteristic) {
        if (DBG) {
            Log.d(TAG, "writeCtsCurrentTime()");
        }
        return writeCharacteristic(
                GattUuid.SRVC_CTS,
                characteristic);
    }

    /**
     * Writes Cts:LocalTimeInformation characteristic and its values
     * to the associated remote device.
     *
     * @param characteristic Characteristic to write on the remote device
     * @return true, if the write operation was initiated successfully
     */
    public boolean writeCtsLocalTimeInformation(CharacteristicBase characteristic) {
        if (DBG) {
            Log.d(TAG, "writeCtsLocalTimeInformation()");
        }
        return writeCharacteristic(
                GattUuid.SRVC_CTS,
                characteristic);
    }

    /**
     * Writes Rtus:TimeUpdateControlPoint characteristic and its values
     * to the associated remote device.
     *
     * @param characteristic Characteristic to write on the remote device
     * @return true, if the write operation was initiated successfully
     */
    public boolean writeRtusTimeUpdateControlPoint(CharacteristicBase characteristic) {
        if (DBG) {
            Log.d(TAG, "writeRtusTimeUpdateControlPoint()");
        }
        return writeCharacteristic(
                GattUuid.SRVC_RTUS,
                characteristic);
    }


    /**
     * Reads Cts:CurrentTime:Cccd descriptor
     * from the associated remote device.
     *
     * @return true, if the read operation was initiated successfully
     */
    public boolean readCtsCurrentTimeCccd() {
        if (DBG) {
            Log.d(TAG, "readCtsCurrentTimeCccd()");
        }
        return readDescriptor(
                GattUuid.SRVC_CTS,
                GattUuid.CHAR_CURRENT_TIME,
                GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION);
    }


    /**
     * Writes Cts:CurrentTime:Cccd descriptor and
     * its values to the associated remote device.
     *
     * @param value Descriptor value to write on the remote device
     * @return true, if the write operation was initiated successfully
     */
    public boolean writeCtsCurrentTimeCccd(byte[] value) {
        if (DBG) {
            Log.d(TAG, "writeCtsCurrentTimeCccd()");
        }
        return writeDescriptor(
                GattUuid.SRVC_CTS,
                GattUuid.CHAR_CURRENT_TIME,
                GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION,
                value);
    }

}


