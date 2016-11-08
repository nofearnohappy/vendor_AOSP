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

package com.mediatek.bluetoothle.anp.detector;

import android.content.ComponentName;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.mediatek.bluetoothle.anp.NotificationController;
import com.mediatek.bluetoothle.anp.support.UnreadSmsCallBroadcastRegister;
import com.mediatek.bluetoothle.anp.support.UnreadSmsCallBroadcastRegister.AlertChangeListener;
import com.mediatek.bluetoothle.ext.BluetoothAnsDetector;

public class CalllogAlertDetector extends BluetoothAnsDetector {

    private static final String TAG = "[BluetoothAns]CalllogAlertDetector";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    private static final Uri CALL_LOG_URI = Uri.parse("content://call_log/calls");
    private static final String[] PROJECTION = {
            "name", "number"
    };
    private static final String SELECTION = "type=3";
    private static final String ORDER_BY = "date DESC LIMIT 1";

    private static final int COLUMN_NAME = 0;
    private static final int COLUMN_NUMBER = 1;

    private UnreadSmsCallBroadcastRegister mRegister;
    // different project have different name
    private ComponentName mName;
    private ComponentName mName2;

    private AlertChangeListener mNewMissedCallListener = new AlertChangeListener() {
        public void onAlertChanged(int number) {
            // get last missed Caller's name or number
            String name = getLastMissedCall();
            if (DBG) {
                Log.d(TAG, "onAlertChanged, number = " + number + ", name = " + name);
            }
            mNewCount = number;
            setNewAlertText(name);
            onAlertNotify(null, NotificationController.CATEGORY_ENABLED_NEW);
        }
    };

    public CalllogAlertDetector(Context context) {
        super(context);
        mCategoryId = NotificationController.CATEGORY_ID_MISSED_CALL;
    }

    @Override
    public void initializeAll() {
        initNewAlertStatus();
        initNewDetector();
    }

    @Override
    public void clearAll() {
        clearNewDetector();
    }

    private void initNewDetector() {
        if (DBG) {
            Log.d(TAG, "initNewDetector");
        }
        mName = new ComponentName("com.android.dialer", "com.android.dialer.DialtactsActivity");
        mName2 = new ComponentName(
                "com.android.contacts", "com.android.contacts.activities.DialtactsActivity");
        mRegister = UnreadSmsCallBroadcastRegister.getInstance();
        mRegister.registerAlertChangeListener(mContext, mName, mNewMissedCallListener);
        mRegister.registerAlertChangeListener(mContext, mName2, mNewMissedCallListener);
    }

    private void initNewAlertStatus() {
        new Thread() {
            public void run() {
                if (DBG) {
                    Log.d(TAG, "initNewAlertStatus, start");
                }
                Cursor cursor = null;
                try {
                    cursor = mContext.getContentResolver().query(CALL_LOG_URI, PROJECTION,
                            SELECTION, null, ORDER_BY);
                    if (cursor != null) {
                        mNewCount = cursor.getCount();
                        if (DBG) {
                            Log.d(TAG, "initNewAlertStatus, count = " + cursor.getCount());
                        }
                    } else {
                        mNewCount = 0;
                    }
                } catch (SecurityException ex) {
                    ex.printStackTrace();
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        } .start();
    }

    private void clearNewDetector() {
        if (DBG) {
            Log.d(TAG, "clearNewDetector");
        }
        mRegister.removeAlertChangeListener(mContext, mName);
        mRegister.removeAlertChangeListener(mContext, mName2);
    }

    private String getLastMissedCall() {
        if (DBG) {
            Log.d(TAG, "getLastMissedCall");
        }
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(CALL_LOG_URI, PROJECTION, SELECTION, null,
                    ORDER_BY);
            if (cursor == null || !cursor.moveToFirst()) {
                return null;
            } else {
                String name = cursor.getString(COLUMN_NAME);
                if (name != null) {
                    return name;
                } else {
                    return cursor.getString(COLUMN_NUMBER);
                }
            }
        } catch (SecurityException ex) {
            ex.printStackTrace();
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
