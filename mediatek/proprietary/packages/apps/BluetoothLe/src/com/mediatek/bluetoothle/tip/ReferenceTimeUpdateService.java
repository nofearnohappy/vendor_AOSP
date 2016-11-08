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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import com.mediatek.bluetooth.BleGattUuid;

import java.text.DateFormat;
import java.util.Date;
import java.util.UUID;

class ReferenceTimeUpdateService extends TipService {
    private static final String TAG = "ReferenceTimeUpdateService";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    static final int STATE_IDLE = 0;
    static final int STATE_UPDATE_PENDING = 1;

    static final int RESULT_SUCCESS = 0;
    static final int RESULT_CANCELED = 1;
    static final int RESULT_NO_CONNECTION = 2;
    static final int RESULT_ERROR = 3;
    static final int RESULT_TIMEOUT = 4;
    static final int RESULT_UPDATE_NOT_ATTEMPED = 5;

    private TipServerService mTipService = null;
    private TimeUpdateStateMachine mUpdateStateMachine = null;

    private final TimeUpdateControlPoint mControlPointChar = new TimeUpdateControlPoint();
    private final TimeUpdateState mUpdateStateChar = new TimeUpdateState();

    ReferenceTimeUpdateService(final TipServerService tipService) {
        mTipService = tipService;
        mUpdateStateMachine = TimeUpdateStateMachine.make(this);
    }

    @Override
    void onReadCharacteristic(final BluetoothGattCharacteristic characteristic,
            final BluetoothDevice device) {
        final UUID charUuid = characteristic.getUuid();
        if (DBG) Log.d(TAG, "onReadCharacteristic: charUuid = " + charUuid);
        if (charUuid.equals(BleGattUuid.Char.TIME_UPDATE_STATE)) {
            onUpdateStateRead(characteristic);
        } else {
            Log.e(TAG, "Unsupported Characteristic: charUuid = " + charUuid);
        }
    }

    @Override
    void onWriteCharateristic(final BluetoothGattCharacteristic characteristic,
            final BluetoothDevice device, final byte[] value) {
        final UUID charUuid = characteristic.getUuid();
        if (DBG) Log.d(TAG, "onWriteCharateristic: charUuid = " + charUuid);
        if (charUuid.equals(BleGattUuid.Char.TIME_UPDATE_CTRL_POINT)) {
            onUpdateCpWrite(characteristic);
        } else {
            Log.e(TAG, "Unsupported Characteristic: charUuid = " + charUuid);
        }
    }

    @Override
    void uninit() {
        if (DBG) Log.d(TAG, "uninit");
        mUpdateStateMachine.doQuit();
    }

    boolean startTimeUpdate() {
        if (VDBG) Log.v(TAG, "startTimeUpdate");
        final Message m = mUpdateStateMachine
                .obtainMessage(TimeUpdateStateMachine.MSG_START_REFERENCE_UPDATE);
        mUpdateStateMachine.sendMessage(m);
        return true;
    }

    boolean cancelTimeUpdate() {
        if (VDBG) Log.v(TAG, "cancelTimeUpdate");
        final Message m = mUpdateStateMachine
                .obtainMessage(TimeUpdateStateMachine.MSG_CANCEL_REFERENCE_UPDATE);
        mUpdateStateMachine.sendMessage(m);
        return true;
    }

    void onStateUpdate(final int state, final int result) {
        if (DBG) Log.d(TAG, "onStateUpdate: state = " + state + ", result = " + result);
        synchronized (mUpdateStateChar) {
            mUpdateStateChar.setCurrentState(state);
            mUpdateStateChar.setResult(result);
        }
    }

    void onStateUpdate(final int state) {
        if (DBG) Log.d(TAG, "onStateUpdate: state = " + state);
        synchronized (mUpdateStateChar) {
            mUpdateStateChar.setCurrentState(state);
        }
    }

    void onTimeUpdate(final long time) {
        if (DBG) Log.d(TAG, "onTimeUpdate: time = " + time);

        // / ***** For log
        final DateFormat df = DateFormat.getDateTimeInstance();
        final Date currentTime = new Date();
        currentTime.setTime(time);
        if (DBG) Log.d(TAG, "time (Date) = " + df.format(currentTime));
        // / *****

        // Notify CTS
        mTipService.onTimeUpdated(time);

        // Set system time
        SystemClock.setCurrentTimeMillis(time);
    }

    int getUpdateState() {
        if (VDBG) Log.v(TAG, "getUpdateState");
        synchronized (mUpdateStateChar) {
            final int state = mUpdateStateChar.getCurrentState();
            if (DBG) Log.d(TAG, "state = " + state);
            return state;
        }
    }

    void onUpdateCpWrite(final BluetoothGattCharacteristic charactertistic) {
        if (VDBG) Log.v(TAG, "onUpdateCpWrite");
        mControlPointChar.fromGattCharacteristic(charactertistic);
        final int value = mControlPointChar.getControlPoint();
        switch (value) {
            case TimeUpdateControlPoint.UPDATE_GET:
                startTimeUpdate();
                break;
            case TimeUpdateControlPoint.UPDATE_CANCEL:
                cancelTimeUpdate();
                break;
            default:
                Log.e(TAG, "Unsupported value = " + value);
                break;
        }
    }

    void onUpdateStateRead(final BluetoothGattCharacteristic charactertistic) {
        if (VDBG) Log.v(TAG, "onUpdateStateRead");
        synchronized (mUpdateStateChar) {
            final int state = mUpdateStateChar.getCurrentState();
            final int result = mUpdateStateChar.getResult();
            if (DBG) Log.d(TAG, "state = " + state + ", result = " + result);
            mUpdateStateChar.setGattCharacteristic(charactertistic);
        }
    }
}
