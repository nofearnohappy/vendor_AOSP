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
 * MediaTek Inc. (C) 2014. All rights reserved.
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

package com.mediatek.bluetoothle.tip;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.mediatek.bluetooth.BleGattUuid;
import com.mediatek.bluetoothle.IBleProfileServer;
/// For TIP Server Reconnect
import com.mediatek.bluetoothle.provider.DeviceParameterRecorder;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/// For TIP Server Reconnect @{
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
/// @}
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

class Casio6900abHandler extends IotHandler {
    private static final String TAG = "Casio6900abHandler";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    Casio6900abHandler(final TipServerService tipService) {
        super(tipService);
        setName(TipServerService.CASIO_GB6900AB);
    }

    @Override
    boolean handleConnStateChange(final BluetoothDevice device,
            final int status, final int newState) {
        if (DBG) Log.d(TAG, "handleConnStateChange: device = " + device + ", status = " + status
                + ", newState = " + newState);
        if (BluetoothAdapter.STATE_DISCONNECTED == newState && !isUserDisconnect(device)) {
            final IBleProfileServer bleProfileServer = getTipService().getBleProfileServer();
            if (DBG) Log.d(TAG, "Not disconnected by user, re-connect: device = " + device);
            bleProfileServer.connect(device, false);
        }
        return true;
    }

    @Override
    boolean handleNotify(final BluetoothDevice device,
            final BluetoothGattCharacteristic characteristic, final boolean confirm) {
        if (DBG) Log.d(TAG, "handleNotify: device = " + device
                + ", characteristic = " + characteristic.getUuid() + ", confirm = " + confirm);
        boolean ret = false;
        // Store original format
        final CurrentTime defaultCurrentTime = new CurrentTime();
        defaultCurrentTime.fromGattCharacteristic(characteristic);
        // Notify converted characteristic
        ret = convertCurrentTimeDefaultToUTC(characteristic);
        final IBleProfileServer bleProfileServer = getTipService().getBleProfileServer();
        bleProfileServer.notifyCharacteristicChanged(device, characteristic, confirm);
        // Restore original format
        ret = defaultCurrentTime.setGattCharacteristic(characteristic);
        return ret;
    }

    @Override
    boolean handleCharRead(final BluetoothGattCharacteristic characteristic) {
        boolean ret = false;
        final UUID charUuid = characteristic.getUuid();
        if (DBG) Log.d(TAG, "handleCharRead: charUuid = " + charUuid);
        if (charUuid.equals(BleGattUuid.Char.CURRENT_TIME)) {
            ret = handleCurrentTimeRead(characteristic);
        } else if (charUuid.equals(BleGattUuid.Char.TIME_WITH_DST)) {
            ret = handleTimeWithDSTRead(characteristic);
        } else {
            if (DBG) Log.d(TAG, "No need to handle");
            ret = true;
        }
        return ret;
    }

    private boolean handleCurrentTimeRead(final BluetoothGattCharacteristic characteristic) {
        if (VDBG) Log.v(TAG, "handleCurrentTimeRead");
        return convertCurrentTimeDefaultToUTC(characteristic);
    }

    private boolean handleTimeWithDSTRead(final BluetoothGattCharacteristic characteristic) {
        if (VDBG) Log.v(TAG, "handleTimeWithDSTRead");
        return convertTimeWithDSTDefaultToUTC(characteristic);
    }

    private boolean convertCurrentTimeDefaultToUTC(
            final BluetoothGattCharacteristic characteristic) {
        if (VDBG) Log.v(TAG, "convertCurrentTimeDefaultToUTC");
        final CurrentTime currentTime = new CurrentTime();
        currentTime.fromGattCharacteristic(characteristic);
        final DateTime defaultDateTime = currentTime.getDateTime();
        final DateTime utcDateTime = convertDateTimeDefaultToUTC(defaultDateTime);
        final int dowWatch = convertDayOfWeek(currentTime.getDayOfWeek());
        currentTime.setDateTime(utcDateTime);
        currentTime.setDayOfWeek(dowWatch);
        return currentTime.setGattCharacteristic(characteristic);
    }

    private boolean convertTimeWithDSTDefaultToUTC(
            final BluetoothGattCharacteristic characteristic) {
        if (VDBG) Log.v(TAG, "convertCurrentTimeDefaultToUTC");
        final TimeWithDST timeWithDST = new TimeWithDST();
        timeWithDST.fromGattCharacteristic(characteristic);
        final DateTime defaultDateTime = timeWithDST.getDateTime();
        final DateTime utcDateTime = convertDateTimeDefaultToUTC(defaultDateTime);
        timeWithDST.setDateTime(utcDateTime);
        return timeWithDST.setGattCharacteristic(characteristic);
    }

    private DateTime convertDateTimeDefaultToUTC(final DateTime dt) {
        if (DBG) Log.d(TAG, "convertDateTimeDefaultToUTC: dt = "
                + dt.toString("yyyy-MM-dd E HH:mm:ss.SSS ZZZZ Z", Locale.ENGLISH));
        final DateTime dtUTC = dt.withZone(DateTimeZone.UTC);
        if (DBG) Log.d(TAG, "dtUTC = "
                + dtUTC.toString("yyyy-MM-dd E HH:mm:ss.SSS ZZZZ Z", Locale.ENGLISH));
        return dtUTC;
    }

    private int convertDayOfWeek(final int dow) {
        if (DBG) Log.d(TAG, "convertDayOfWeek: dow = " + dow);
        int convertedDow = 0;
        convertedDow = dow + 1;
        if (DBG) Log.d(TAG, "convertedDow = " + convertedDow);
        return convertedDow;
    }

    /// For TIP Server Reconnect @{
    private boolean isUserDisconnect(final BluetoothDevice device) {
        if (DBG) Log.d(TAG, "isUserDisconnect: device = " + device);
        final List<BluetoothDevice> autoConnectDeviceList = getAutoConnectDevices();
        final Set<BluetoothDevice> s = new HashSet<BluetoothDevice>(
                autoConnectDeviceList);
        if (s.contains(device)) {
            if (DBG) Log.d(TAG, "Auto-connect device, return false");
            return false;
        } else {
            if (DBG) Log.d(TAG, "Not auto-connect device, return true");
            return true;
        }
    }

    private List<BluetoothDevice> getAutoConnectDevices() {
        final List<BluetoothDevice> deviceList = new ArrayList<BluetoothDevice>();

        final List<String> addrList = DeviceParameterRecorder.
                getAutoConnectDeviceAddresses(getTipService());

        if (addrList == null) {
            if (DBG) Log.d(TAG, "No auto-connect devices");
            return deviceList;
        }

        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        for (String addr : addrList) {
            if (addr == null) continue;

            final BluetoothDevice device = adapter.getRemoteDevice(addr);

            if (device != null) {
                deviceList.add(device);
                if (DBG) Log.d(TAG, "Auto-connect device: " + addr);
            }
        }

        return deviceList;
    }
    /// @}
}
