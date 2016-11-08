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
package com.mediatek.blemanager.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class BleManagerDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = BleConstants.COMMON_TAG + "[BleManagerDatabaseHelper]";

    private static final String DATABASE_NAME = "blemanager.db";

    private static final int DEFAULT_VERSION = 1;

    private static BleManagerDatabaseHelper sInstance;

    // private Context mContext;

    private BleManagerDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DEFAULT_VERSION);
        // mContext = context;
        Log.d(TAG, "[Constructor] DATABASE_NAME : " + DATABASE_NAME + ", DEFAULT_VERSION :"
                + DEFAULT_VERSION);
    }

    public static BleManagerDatabaseHelper getInstance(Context context) {
        if (context == null) {
            Log.e(TAG, "[getInstance] WRONG PARAMETER!! Context is null!!");
            return null;
        }
        if (sInstance == null) {
            sInstance = new BleManagerDatabaseHelper(context);
        }
        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createUxTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "[onUpgrade] oldVersion : " + oldVersion + ", newVersion : " + newVersion);
    }

    private void createUxTable(SQLiteDatabase db) {
        Log.d(TAG, "[createUxTable]...");
        String uxCreateString = "CREATE TABLE " + BleConstants.DEVICE_SETTINGS.TABLE_NAME + " ("
                + BleConstants.COLUMN_ID + " INTEGER PRIMARY KEY," + BleConstants.COLUMN_BT_ADDRESS
                + " TEXT," + BleConstants.DEVICE_SETTINGS.DEVICE_DISPLAY_ORDER + " INTEGER,"
                + BleConstants.DEVICE_SETTINGS.DEVICE_NAME + " TEXT,"
                + BleConstants.DEVICE_SETTINGS.DEVICE_IAMGE_DATA + " TEXT,"
                + BleConstants.DEVICE_SETTINGS.DEVICE_SERVICE_LIST + " TEXT,"
                + BleConstants.PXP_CONFIGURATION.RANGE_ALERT_INFO_DIALOG_ENABLER + " INTEGER,"
                + BleConstants.PXP_CONFIGURATION.RINGTONE_ENABLER + " INTEGER,"
                + BleConstants.PXP_CONFIGURATION.RINGTONE + " TEXT,"
                + BleConstants.PXP_CONFIGURATION.VIBRATION_ENABLER + " INTEGER,"
                + BleConstants.PXP_CONFIGURATION.VOLUME + " INTEGER)";
        // BLEConstants.PXP_CONFIGURATION.IS_SUPPORT_OPTIONAL + " INTEGER)";
        db.execSQL(uxCreateString);
    }

}
