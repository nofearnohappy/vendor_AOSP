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
package com.mediatek.blemanager;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.mediatek.blemanager.common.CachedBleDevice;
import com.mediatek.blemanager.common.CachedBleDeviceManager;
import com.mediatek.blemanager.common.LocalBleManager;
import com.mediatek.blemanager.provider.ActivityDbOperator;
import com.mediatek.blemanager.provider.BleConstants;

import java.util.ArrayList;

public class BleApp extends Application {
    private static final String TAG = BleConstants.COMMON_TAG + "[BleApp]";

    private CachedBleDeviceManager mCachedBleDeviceManager;
    private LocalBleManager mLocalBleManager;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "[onCreate]...");
        mLocalBleManager = LocalBleManager.getInstance(this);
        mCachedBleDeviceManager = CachedBleDeviceManager.getInstance();
        ActivityDbOperator.initialization(this.getApplicationContext());
        initDevices();
    }

    /*@Override
    public void onLowMemory() {
        Log.i(TAG, "[onLowMemory]...");
        if (mLocalBleManager != null) {
            mLocalBleManager.close();
        } else {
            Log.e(TAG, "[onLowMemory]mLocalBluetoothLEManager is null!");
        }
        super.onLowMemory();
    }*/

    @Override
    public void onTerminate() {
        Log.i(TAG, "[onTerminate]...");
        if (mLocalBleManager != null) {
            mLocalBleManager.close();
        } else {
            Log.e(TAG, "[onTerminate]mLocalBluetoothLEManager is null!");
        }
        super.onTerminate();
    }

    /**
     * init device from db, and construct the cached manager
     */
    private void initDevices() {
        ContentResolver cr = this.getContentResolver();
        Cursor c1 = cr.query(BleConstants.TABLE_UX_URI, null, null, null, null);
        doInitialization(c1);
        if (c1 != null) {
            c1.close();
        }
    }

    private void doInitialization(Cursor cursor) {
        Log.d(TAG, "[doInitialization]...");
        if (cursor == null || cursor.getCount() == 0) {
            Log.w(TAG, "[doInitialization] cursor is null or empty,cursor = " + cursor);
            return;
        }
        
        if (cursor.moveToFirst()) {
             do {
                int disOrder = cursor.getInt(
                        cursor.getColumnIndex(BleConstants.DEVICE_SETTINGS.DEVICE_DISPLAY_ORDER));
                String deviceAddress = cursor.getString(
                        cursor.getColumnIndex(BleConstants.COLUMN_BT_ADDRESS));
                String name = cursor.getString(
                        cursor.getColumnIndex(BleConstants.DEVICE_SETTINGS.DEVICE_NAME));
                BluetoothDevice device =
                    BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
                Uri imageUri = Uri.parse(BleConstants.TABLE_UX_URI_STRING + "/" +
                        cursor.getInt(cursor.getColumnIndex(BleConstants.COLUMN_ID)));
                Log.d(TAG, "[doInitialization] imageUri : " + imageUri);
                CachedBleDevice cachedDevice = mCachedBleDeviceManager.addDevice(device, disOrder);
                if (cachedDevice != null) {
                    cachedDevice.setInitFromDb(true);
                    cachedDevice.setDeviceName(name);
                    cachedDevice.setDeviceImageUri(imageUri);
                    getServiceList(cachedDevice, cursor);
                    initPxpConfiguration(cachedDevice, deviceAddress);
                    cachedDevice.setInitFromDb(false);
                }
            } while(cursor.moveToNext());
        }
    }

    private void getServiceList(CachedBleDevice cachedDevice, Cursor cursor) {
        if (cachedDevice == null || cursor == null || cursor.getCount() == 0) {
            Log.w(TAG, "[getServiceList] parameter is wrong,return!");
            return;
        }
        String str = cursor.getString(
                cursor.getColumnIndex(BleConstants.DEVICE_SETTINGS.DEVICE_SERVICE_LIST));
        if (str == null || str.trim().length() == 0) {
            Log.w(TAG, "[getServiceList] service list is null or empty,str = " + str);
            return;
        }
        ArrayList<String> retList = new ArrayList<String>();
        String[] list = str.split(BleConstants.SERVICE_LIST_SEPERATER);
        for (String s : list) {
            if (s.trim().length() != 0) {
                retList.add(s);
            }
        }
        cachedDevice.setServiceListFromDb(retList);
    }

    private void initPxpConfiguration(CachedBleDevice cachedDevice, String address) {
        initPxpConfigurationFromUxTable(cachedDevice, address);
    }

    private void initPxpConfigurationFromUxTable(
            CachedBleDevice cachedDevice, String address) {
        String selection = BleConstants.COLUMN_BT_ADDRESS + "='" + address + "'";
        Cursor cursor = this.getContentResolver().query(
                BleConstants.TABLE_UX_URI, null, selection, null, null);
        if (cursor == null) {
            Log.w(TAG, "[initPxpConfiguration] cursor is null!");
            return;
        }
        if (cursor.getCount() == 0) {
            cursor.close();
            Log.w(TAG, "[initPxpConfiguration] cursor.getCount is 0 !");
            return;
        }
        
        if (cursor.moveToFirst()) {
            cachedDevice.setBooleanAttribute(CachedBleDevice.DEVICE_RINGTONE_ENABLER_FLAG,
                    int2Boolean(cursor.getInt(cursor.getColumnIndex(
                            BleConstants.PXP_CONFIGURATION.RINGTONE_ENABLER))));
            cachedDevice.setBooleanAttribute(CachedBleDevice.DEVICE_VIBRATION_ENABLER_FLAG,
                    int2Boolean(cursor.getInt(cursor.getColumnIndex(
                            BleConstants.PXP_CONFIGURATION.VIBRATION_ENABLER))));
            cachedDevice.setBooleanAttribute(
                    CachedBleDevice.DEVICE_RANGE_INFO_DIALOG_ENABELR_FLAG,
                    int2Boolean(cursor.getInt(cursor.getColumnIndex(
                            BleConstants.PXP_CONFIGURATION.RANGE_ALERT_INFO_DIALOG_ENABLER))));
            cachedDevice.setIntAttribute(CachedBleDevice.DEVICE_VOLUME_FLAG,
                    cursor.getInt(cursor.getColumnIndex(BleConstants.PXP_CONFIGURATION.VOLUME)));

            String str = cursor.getString(
                    cursor.getColumnIndex(BleConstants.PXP_CONFIGURATION.RINGTONE));
            if (str != null) {
                cachedDevice.setRingtoneUri(Uri.parse(str));
            }
        }
        cursor.close();
    }

    private boolean int2Boolean(int value) {
        if (value == 0) {
            return false;
        }
        return true;
    }
}
