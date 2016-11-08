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
 * Public API for the GATT Proximity Profile(Client).
 *
 * Name: Proximity
 * Type: org.bluetooth.profile.proximity
 * Last Modified: None
 * Revision: None
 * Role: ProximityMonitor
 */
public class PxpProximityMonitor extends ClientBase {
    private static final boolean DBG = true;
    private static final String TAG = "PxpProximityMonitor";

    /**
     * Create a PxpProximityMonitor object and init value.
     *
     * @param context App context
     */
    public PxpProximityMonitor(Context context) {
        super(context);

        if (DBG) {
            Log.d(TAG, "PxpProximityMonitor()");
        }
    }

    @Override
    protected boolean handleServicesDiscovered(BluetoothGatt gatt, int status) {
        // Check mandatory service is exist.
        return true &&
                (gatt.getService(GattUuid.SRVC_LLS) != null) &&
                (gatt.getService(GattUuid.SRVC_IAS) != null) &&
                (gatt.getService(GattUuid.SRVC_TPS) != null);
    }

    /**
     * Reads Lls:AlertLevel characteristic
     * from the associated remote device.
     *
     * @return true, if the read operation was initiated successfully
     */
    public boolean readLlsAlertLevel() {
        if (DBG) {
            Log.d(TAG, "readLlsAlertLevel()");
        }
        return readCharacteristic(
                GattUuid.SRVC_LLS,
                GattUuid.CHAR_ALERT_LEVEL);
    }

    /**
     * Reads Tps:TxPowerLevel characteristic
     * from the associated remote device.
     *
     * @return true, if the read operation was initiated successfully
     */
    public boolean readTpsTxPowerLevel() {
        if (DBG) {
            Log.d(TAG, "readTpsTxPowerLevel()");
        }
        return readCharacteristic(
                GattUuid.SRVC_TPS,
                GattUuid.CHAR_TX_POWER_LEVEL);
    }


    /**
     * Writes Lls:AlertLevel characteristic and its values
     * to the associated remote device.
     *
     * @param characteristic Characteristic to write on the remote device
     * @return true, if the write operation was initiated successfully
     */
    public boolean writeLlsAlertLevel(CharacteristicBase characteristic) {
        if (DBG) {
            Log.d(TAG, "writeLlsAlertLevel()");
        }
        return writeCharacteristic(
                GattUuid.SRVC_LLS,
                characteristic);
    }

    /**
     * Writes Ias:AlertLevel characteristic and its values
     * to the associated remote device.
     *
     * @param characteristic Characteristic to write on the remote device
     * @return true, if the write operation was initiated successfully
     */
    public boolean writeIasAlertLevel(CharacteristicBase characteristic) {
        if (DBG) {
            Log.d(TAG, "writeIasAlertLevel()");
        }
        return writeCharacteristic(
                GattUuid.SRVC_IAS,
                characteristic);
    }





}


