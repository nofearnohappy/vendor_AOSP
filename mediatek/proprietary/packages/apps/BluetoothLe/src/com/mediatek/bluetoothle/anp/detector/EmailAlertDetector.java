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
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import com.mediatek.bluetoothle.anp.NotificationController;
import com.mediatek.bluetoothle.anp.support.UnreadSmsCallBroadcastRegister;
import com.mediatek.bluetoothle.anp.support.UnreadSmsCallBroadcastRegister.AlertChangeListener;
import com.mediatek.bluetoothle.ext.BluetoothAnsDetector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EmailAlertDetector extends BluetoothAnsDetector {

    private static final String TAG = "[BluetoothAns]EmailAlertDetector";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    private static final Uri NEW_OBSERVER_URI =
            Uri.parse("content://com.android.email.provider/message");
    private static final Uri NEW_QUERY_URI =
            Uri.parse("content://com.android.email.provider/message");
    private static final String[] NEW_PROJECTION = {
            "_id ", "displayName"
    };

    private static final String NEW_SELECTION = "flagSeen=0";
    private static final String NEW_ORDER_BY = "_id DESC";

    private static final Uri UNREAD_QUERY_URI =
            Uri.parse("content://com.android.email.provider/message");
    private static final String[] UNREAD_PROJECTION = {
            "_id "
    };
    private static final String UNREAD_SELECTION = "flagRead=0";
    private static final String UNREAD_ORDER_BY = null;

    private static final int ID_COLUMN = 0;
    private static final int DISPLAY_NAME_COLUMN = 1;

    private UnreadSmsCallBroadcastRegister mRegister;
    private ComponentName mName;
    private int mLastEmailId = 0;
    private String mNewEmailSelection = UNREAD_SELECTION;

    private AlertChangeListener mUnreadEmailListener = new AlertChangeListener() {
        public void onAlertChanged(int number) {
            if (DBG) {
                Log.d(TAG, "mUnreadEmailListener, onAlertChanged, number = " + number);
            }
            mUnreadCount = number;
            onAlertNotify(null, NotificationController.CATEGORY_ENABLED_UNREAD);
        }
    };

    private ContentObserver mObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            // Log.d(TAG, "ContentObserver onChange");

            Cursor cursor = null;
            try {
                cursor = mContext.getContentResolver().query(NEW_QUERY_URI, NEW_PROJECTION,
                        mNewEmailSelection, null, NEW_ORDER_BY);
                if (cursor != null && cursor.moveToFirst()) {
                    int lastId = cursor.getInt(ID_COLUMN);
                    String displayName = null;
                    displayName = cursor.getString(DISPLAY_NAME_COLUMN);
                    mNewCount = cursor.getCount();
                    setNewAlertText(displayName);
                    if (lastId > mLastEmailId) {
                        if (DBG) {
                            Log.d(TAG, "ContentObserver onChange, count = " + cursor.getCount()
                                    + ", name = " + displayName);
                        }
                        mLastEmailId = lastId;
                        onAlertNotify(null, NotificationController.CATEGORY_ENABLED_NEW);
                    } else {
                        mLastEmailId = lastId;
                    }
                } else {
                    if (DBG) {
                        Log.d(TAG, "ContentObserver onChange, no new Email");
                    }
                    mNewCount = 0;
                    setNewAlertText(null);
                }
            } catch (SecurityException ex) {
                ex.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    };

    public EmailAlertDetector(Context context) {
        super(context);
        mCategoryId = NotificationController.CATEGORY_ID_EMAIL;
    }

    @Override
    public void initializeAll() {
        initNewAlertStatus();
        initUnreadAlertStatus();
        initNewDetector();
        initUnreadDetector();
    }

    @Override
    public void clearAll() {
        clearNewDetector();
        clearUnreadDetector();
    }

    private void initUnreadDetector() {
        if (DBG) {
            Log.d(TAG, "initUnreadDetector");
        }
        mName = new ComponentName("com.android.email", "com.android.email.activity.Welcome");
        mRegister = UnreadSmsCallBroadcastRegister.getInstance();
        mRegister.registerAlertChangeListener(mContext, mName, mUnreadEmailListener);
    }

    private void initNewDetector() {
        if (DBG) {
            Log.d(TAG, "initNewDetector");
        }
        mContext.getContentResolver().registerContentObserver(NEW_OBSERVER_URI, false, mObserver);
    }

    private void initNewAlertStatus() {
        // some old branch(before KK) has no this column
        if (columnExistsInTable("flagSeen", NEW_QUERY_URI)) {
            mNewEmailSelection = NEW_SELECTION;
        } else {
            mNewEmailSelection = UNREAD_SELECTION;
        }
        new Thread() {
            public void run() {
                if (DBG) {
                    Log.d(TAG, "initNewAlertStatus, start");
                }
                Cursor cursor = null;
                try {
                     cursor = mContext.getContentResolver().query(
                            NEW_QUERY_URI, NEW_PROJECTION, mNewEmailSelection, null, NEW_ORDER_BY);
                    if (cursor != null && cursor.moveToFirst()) {
                        int lastId = cursor.getInt(ID_COLUMN);
                        mLastEmailId = lastId;
                        mNewCount = cursor.getCount();
                        if (DBG) {
                            Log.d(TAG, "initNewAlertStatus, lastId = " + lastId + ", count = "
                                    + cursor.getCount());
                        }
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

    private void initUnreadAlertStatus() {
        new Thread() {
            public void run() {
                if (DBG) {
                    Log.d(TAG, "initUnreadAlertStatus, start");
                }
                Cursor cursor = null;
                try {
                    cursor = mContext.getContentResolver().query(
                            UNREAD_QUERY_URI, UNREAD_PROJECTION, UNREAD_SELECTION, null,
                            UNREAD_ORDER_BY);
                    if (cursor != null && cursor.moveToFirst()) {
                        mUnreadCount = cursor.getCount();
                        if (DBG) {
                            Log.d(TAG, "initUnreadAlertStatus, count = " + cursor.getCount());
                        }
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

    private void clearUnreadDetector() {
        mRegister.removeAlertChangeListener(mContext, mName);
    }

    private void clearNewDetector() {
        mContext.getContentResolver().unregisterContentObserver(mObserver);
    }

    private boolean columnExistsInTable(String columnName, Uri dbUri) {
        // Log.i(TAG, "columnExistsInTable++, columnName = " + columnName);
        boolean ret = false;
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(dbUri, null, null, null, "_id LIMIT 1");
            if (cursor != null) {
                List<String> columnList = new ArrayList<String>(Arrays.asList(cursor.getColumnNames()));
                // Log.i(TAG, "colum = " + columnList.toString());
                if (columnList.contains(columnName)) {
                    // Log.i(TAG, "columnExistsInTable return true");
                    ret = true;
                }
            }
        } catch (SecurityException ex) {
            ex.printStackTrace();
        } finally {
            if (cursor != null) { 
                cursor.close();
            }
        }
        return ret;
    }
}
