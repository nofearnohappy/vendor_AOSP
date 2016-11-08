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

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

class TimeUpdateState extends TipCharacteristic {
    private static final String TAG = "TimeUpdateState";
    private static final boolean DBG = true;

    private static final int CURRENT_STATE_FORMAT_OFFSET_BASE = 0;
    private static final int CURRENT_STATE_FORMAT = BluetoothGattCharacteristic.FORMAT_UINT8;
    private static final int CURRENT_STATE_FORMAT_OFFSET = CURRENT_STATE_FORMAT_OFFSET_BASE;

    private static final int RESULT_OFFSET_BASE = CURRENT_STATE_FORMAT_OFFSET
            + TipServerService.getTypeLen(CURRENT_STATE_FORMAT);
    private static final int RESULT_FORMAT = BluetoothGattCharacteristic.FORMAT_UINT8;
    private static final int RESULT_OFFSET = RESULT_OFFSET_BASE;

    private int mCurrentState = ReferenceTimeUpdateService.STATE_IDLE;
    private int mResult = ReferenceTimeUpdateService.RESULT_UPDATE_NOT_ATTEMPED;

    int getCurrentState() {
        if (DBG) Log.d(TAG, "getCurrentState: mCurrentState = " + mCurrentState);
        return mCurrentState;
    }

    boolean setCurrentState(final int state) {
        if (DBG) Log.d(TAG, "setCurrentState: state = " + state);
        mCurrentState = state;
        return true;
    }

    int getResult() {
        if (DBG) Log.d(TAG, "getResult: mResult = " + mResult);
        return mResult;
    }

    boolean setResult(final int result) {
        if (DBG) Log.d(TAG, "setResult: result = " + result);
        mResult = result;
        return true;
    }

    @Override
    boolean fromGattCharacteristic(final BluetoothGattCharacteristic characteristic) {
        return false;
    }

    @Override
    boolean setGattCharacteristic(final BluetoothGattCharacteristic characteristic) {
        boolean ret = false;
        ret = setResult(characteristic, mResult);
        ret = setCurrentState(characteristic, mCurrentState);
        return ret;
    }

    private boolean setResult(final BluetoothGattCharacteristic characteristic, final int result) {
        return characteristic.setValue(result, RESULT_FORMAT, RESULT_OFFSET);
    }

    private boolean setCurrentState(final BluetoothGattCharacteristic characteristic,
            final int state) {
        return characteristic.setValue(state, CURRENT_STATE_FORMAT, CURRENT_STATE_FORMAT_OFFSET);
    }
}
