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
 * MediaTek Inc. (C) 2010. All rights reserved.
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

package com.mediatek.bluetoothle.ext;

import android.content.Context;

import android.util.Log;

public class BluetoothAnsDetector {
    private static final String TAG = "[BluetoothAns]BluetoothAnsDetector";
    private static final boolean DBG = true;

    public interface DetectorChangedEventProcessor {
        void onChanged(String address, byte categoryId, int type);

        void onNewAlertTextChanged(byte categoryId, String text);
    }

    protected Context mContext;
    protected byte mCategoryId;
    private DetectorChangedEventProcessor mEventProcessor = null;
    protected int mNewCount = 0;
    protected int mUnreadCount = 0;

    /**
     * initialize the class, must initialize the value of mCategoryId
     */
    public BluetoothAnsDetector(Context context) {
        mContext = context;
    }

    /**
     * initialize alert status change event listener
     */
    public void initializeAll() {

    }

    /**
     * unregister the alert status change event listener
     */
    public void clearAll() {
    }

    public final void registListener(DetectorChangedEventProcessor listener) {
        mEventProcessor = listener;
    }

    public final void removeListener() {
        mEventProcessor = null;
    }

    public final int getCurrentNewAlertCount() {
        return mNewCount;
    }

    public final int getCurrentUnreadAlertCount() {
        return mUnreadCount;
    }

    public final byte getDetectorCategory() {
        return mCategoryId;
    }

    /**
     * send a request to notify to client immediately.
     *
     * @param address is null value, means notify to all client. If address is not null, only need
     *            notify to the client.
     * @param type is 0x01, means notify new alert, type is 0x02, means notify unread alert. The
     *            constant is defined in com.mediatek.bluetooth.BleAlertNotificationProfileService.
     *            0x01 is CATEGORY_VALUE_NEW_ALERT_ENABLED, 0x02 is
     *            CATEGORY_VALUE_UNREAD_ALERT_ENABLED
     */
    protected final void onAlertNotify(String address, int type) {
        if (DBG) {
            Log.d(TAG, "onAlertNotify(), " + "type = " + type + "categoryId:" + mCategoryId
                    + "address:" + address);
        }
        if (mEventProcessor != null) {
            mEventProcessor.onChanged(address, mCategoryId, type);
        }
    }

    /**
     * update new alert text
     *
     * @param text the string value need notify to client
     */
    protected final void setNewAlertText(String text) {
        if (mEventProcessor != null) {
            mEventProcessor.onNewAlertTextChanged(mCategoryId, text);
        }
    }
}
