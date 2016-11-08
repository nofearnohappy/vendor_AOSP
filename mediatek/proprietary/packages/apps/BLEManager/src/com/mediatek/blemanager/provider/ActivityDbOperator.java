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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.mediatek.blemanager.common.CachedBleDevice;
import com.mediatek.blemanager.common.LocalBleManager;
import com.mediatek.blemanager.ui.ActivityUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ActivityDbOperator {
    private static final String TAG = BleConstants.COMMON_TAG + "[ActivityDbOperator]";

    private static ActivityDbOperator sInstance;

    private static final ArrayList<Integer> UX_ATTRIBUTE_LIST = new ArrayList<Integer>();
    private static final ArrayList<Integer> PXP_UX_ATTRIBUTE_LIST = new ArrayList<Integer>();
    private static final ArrayList<Integer> PXP_CLIENT_ATTRIBUTE_LIST = new ArrayList<Integer>();
    private static final ArrayList<Integer> ANS_ATTRIBUTE_LIST = new ArrayList<Integer>();

    private Context mContext;

    private ActivityDbOperator(Context context) {
        Log.i(TAG, "[ActivityDbOperator]new...");
        mContext = context;

        UX_ATTRIBUTE_LIST.add(CachedBleDevice.DEVICE_NAME_ATTRIBUTE_FLAG);
        // UX_ATTRIBUTE_LIST.add(CachedBluetoothLEDevice.DEVICE_IMAGE_ATTRIBUTE_FLAG);
        // UX_ATTRIBUTE_LIST.add(CachedBluetoothLEDevice.DEVICE_AUTO_CONNECT_FLAG);
        // UX_ATTRIBUTE_LIST.add(CachedBluetoothLEDevice.DEVICE_FMP_STATE_FLAG);
        UX_ATTRIBUTE_LIST.add(CachedBleDevice.DEVICE_SERVICE_LIST_CHANGE_FLAG);

        PXP_CLIENT_ATTRIBUTE_LIST.add(CachedBleDevice.DEVICE_ALERT_SWITCH_ENABLER_FLAG);
        PXP_CLIENT_ATTRIBUTE_LIST.add(CachedBleDevice.DEVICE_RANGE_ALERT_ENABLER_FLAG);
        PXP_CLIENT_ATTRIBUTE_LIST.add(CachedBleDevice.DEVICE_RANGE_VALUE_FLAG);
        PXP_CLIENT_ATTRIBUTE_LIST.add(CachedBleDevice.DEVICE_IN_OUT_RANGE_ALERT_FLAG);
        PXP_CLIENT_ATTRIBUTE_LIST.add(CachedBleDevice.DEVICE_DISCONNECTION_WARNING_EANBLER_FLAG);

        PXP_UX_ATTRIBUTE_LIST.add(CachedBleDevice.DEVICE_RINGTONE_ENABLER_FLAG);
        PXP_UX_ATTRIBUTE_LIST.add(CachedBleDevice.DEVICE_VOLUME_FLAG);
        PXP_UX_ATTRIBUTE_LIST.add(CachedBleDevice.DEVICE_VIBRATION_ENABLER_FLAG);
        // PXP_UX_ATTRIBUTE_LIST.add(CachedBluetoothLEDevice.DEVICE_SUPPORT_OPTIONAL);
        PXP_UX_ATTRIBUTE_LIST.add(CachedBleDevice.DEVICE_RANGE_INFO_DIALOG_ENABELR_FLAG);
        PXP_UX_ATTRIBUTE_LIST.add(CachedBleDevice.DEVICE_RINGTONE_URI_FLAG);
        // PXP_UX_ATTRIBUTE_LIST.add(CachedBluetoothLEDevice.DEVICE_SERVICE_LIST_CHANGE_FLAG);

        ANS_ATTRIBUTE_LIST.add(CachedBleDevice.DEVICE_INCOMING_CALL_ENABLER_FLAG);
        ANS_ATTRIBUTE_LIST.add(CachedBleDevice.DEVICE_MISSED_CALL_ENABLER_FLAG);
        ANS_ATTRIBUTE_LIST.add(CachedBleDevice.DEVICE_NEW_EMAIL_ENABLER_FLAG);
        ANS_ATTRIBUTE_LIST.add(CachedBleDevice.DEVICE_NEW_MESSAGE_ENABLER_FLAG);
        // ANS_ATTRIBUTE_LIST.add(CachedBluetoothLEDevice.DEVICE_SERVICE_LIST_CHANGE_FLAG);
    }

    public static void initialization(Context context) {
        sInstance = new ActivityDbOperator(context);
    }

    public static ActivityDbOperator getInstance() {
        return sInstance;
    }

    public boolean isInDb(CachedBleDevice device, int which) {
        String selection = BleConstants.COLUMN_BT_ADDRESS + "='" + device.getDevice().getAddress()
                + "'";
        ContentResolver cr = mContext.getContentResolver();
        Uri uri = null;
        if (which == 0) {
            uri = BleConstants.TABLE_UX_URI;
        } /*
           * else if (which == 1) { //uri = BLEConstants.TABLE_PXP_URI; } else
           * if (which == 2) { uri = BLEConstants.TABLE_ANS_URI; }
           */
        if (uri == null) {
            Log.w(TAG, "[isInDb] uri is null,return.");
            return false;
        }
        Cursor queryCursor = cr.query(uri, null, selection, null, null);
        if (queryCursor == null) {
            Log.w(TAG, "[isInDb] queryCursor is null,return.");
            return false;
        }
        if (queryCursor.getCount() == 0) {
            Log.i(TAG, "[isInDb] get count is 0.");
            queryCursor.close();
            return false;
        }
        queryCursor.close();
        Log.i(TAG, "[isInDb] device is alread in db.");

        return true;
    }

    private byte[] getDefaultImage() {
        return ActivityUtils.getDefaultImage(mContext);
    }

    public void updateDeviceAttributeToDb(CachedBleDevice cacheDevice, int which) {
        Log.i(TAG, "[updateDeviceAttributeToDb]which = " + which);
        if (cacheDevice == null || cacheDevice.getDevice() == null) {
            Log.w(TAG, "[updateDeviceAttributeToDb] cacheDevice is null");
            return;
        }
        if (UX_ATTRIBUTE_LIST.contains(which)) {
            this.updateDeviceUxAttribute(cacheDevice, which);
        }
        if (PXP_UX_ATTRIBUTE_LIST.contains(which)) {
            this.updateDevicePxpAttribute(cacheDevice, which);
        }
        // if (ANS_ATTRIBUTE_LIST.contains(which)) {
        // this.updateDeviceAnsAttribute(cacheDevice, which);
        // }
        if (PXP_CLIENT_ATTRIBUTE_LIST.contains(which)) {
            this.updateClientPxpAttribute(cacheDevice);
        }
    }

    public void initDevice(final CachedBleDevice device) {
        if (!checkParamter(device)) {
            Log.w(TAG, "[initDevice] device parameter is wrong");
            return;
        }
        if (isInDb(device, 0)) {
            Log.w(TAG, "[initDevice] device is already in db," + " no need to do initialization");
            return;
        }

        ContentValues values = new ContentValues();
        ContentResolver resolver = mContext.getContentResolver();
        values.put(BleConstants.COLUMN_BT_ADDRESS, device.getDevice().getAddress());
        values.put(BleConstants.DEVICE_SETTINGS.DEVICE_DISPLAY_ORDER,
                device.getDeviceLocationIndex());
        values.put(BleConstants.DEVICE_SETTINGS.DEVICE_NAME, device.getDeviceName());
        values.put("image_byte_array", getDefaultImage());
        initPxpData(device, values);

        Uri uri = resolver.insert(BleConstants.TABLE_UX_URI, values);
        device.setDeviceImageUri(uri);
    }

    private void updateDeviceUxAttribute(final CachedBleDevice device, final int which) {
        if (!UX_ATTRIBUTE_LIST.contains(which)) {
            Log.w(TAG, "[updateDeviceUxAttribute] attribute is not in, no need to update");
            return;
        }
        if (!checkParamter(device)) {
            Log.w(TAG, "[updateDeviceUxAttribute] checkParamter false,return.");
            return;
        }
        Log.d(TAG, "[updateDeviceUxAttribute] device name : " + device.getDeviceName()
                + ",which = " + which);

        Runnable r = new Runnable() {

            @Override
            public void run() {
                String selection = BleConstants.COLUMN_BT_ADDRESS + "='"
                        + device.getDevice().getAddress() + "'";
                ContentResolver cr = mContext.getContentResolver();

                if (!isInDb(device, 0)) {
                    Log.i(TAG, "[updateDeviceUxAttribute] device is not is db");
                    return;
                }
                
                ContentValues values = new ContentValues();
                Log.d(TAG, "[updateDeviceUxAttribute] which : " + which);
                switch (which) {
                case CachedBleDevice.DEVICE_NAME_ATTRIBUTE_FLAG:
                    values.put(BleConstants.DEVICE_SETTINGS.DEVICE_NAME, device.getDeviceName());
                    break;
                    
                case CachedBleDevice.DEVICE_SERVICE_LIST_CHANGE_FLAG:
                    buildServiceList(device.getServiceList(), values);
                    break;

                default:
                    return;
                }
                if (values.size() == 0) {
                    Log.i(TAG, "[updateDeviceUxAttribute] values is empty");
                    return;
                }
                Log.d(TAG, "[updateDeviceUxAttribute] values : " + values);
                cr.update(BleConstants.TABLE_UX_URI, values, selection, null);
            }

        };
        new Thread(r).start();
    }

    public void updateDeviceImage(CachedBleDevice cachedDevice, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            Log.w(TAG, "[updateDeviceImage] bytes is wrong,bytes = " + bytes);
            return;
        }
        if (cachedDevice == null || cachedDevice.getDevice() == null) {
            Log.w(TAG, "[updateDeviceImage] cachedDevice is null");
            return;
        }
        Log.d(TAG, "[updateDeviceImage] call to update device image in db");
        ContentValues values = new ContentValues();
        values.put("image_byte_array", bytes);
        String where = BleConstants.COLUMN_BT_ADDRESS + "='"
                + cachedDevice.getDevice().getAddress() + "'";
        this.mContext.getContentResolver().update(BleConstants.TABLE_UX_URI, values, where, null);
    }

    private void buildServiceList(List<UUID> serviceList, ContentValues values) {
        if (serviceList == null || values == null) {
            Log.w(TAG, "[buildServiceList] parameter is wrong");
            return;
        }
        if (serviceList.size() == 0) {
            Log.w(TAG, "[buildServiceList] service list is empty");
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (UUID uid : serviceList) {
            builder.append(uid.toString());
            builder.append(BleConstants.SERVICE_LIST_SEPERATER);
        }
        values.put(BleConstants.DEVICE_SETTINGS.DEVICE_SERVICE_LIST, builder.toString());
    }

    private void updateDevicePxpAttribute(final CachedBleDevice device, final int which) {
        if (!PXP_UX_ATTRIBUTE_LIST.contains(which)) {
            Log.w(TAG, "[updateDevicePxpAttribute] attribute is not in, no need to update");
            return;
        }
        if (!checkParamter(device)) {
            return;
        }
        Log.d(TAG, "[updateDevicePxpAttribute] device name : " + device.getDeviceName());

        Runnable r = new Runnable() {

            @Override
            public void run() {
                if (!isInDb(device, 0)) {
                    Log.d(TAG, "[updateDevicePxpAttribute] device is not in db");
                    return;
                }
                updatePxpData(device, which);
            }

        };
        new Thread(r).start();
    }

    /**
     * used to init pxp data if the device service has been changed, should add
     * teh device into db
     * 
     * @param device
     */
    private void initPxpData(CachedBleDevice device, ContentValues values) {
        if (device == null || device.getDevice() == null) {
            Log.w(TAG, "[initPxpData] device is null");
            return;
        }
        Log.d(TAG, "[initPxpData] enter to do init");

        values.put(BleConstants.PXP_CONFIGURATION.RANGE_ALERT_INFO_DIALOG_ENABLER,
                device.getBooleanAttribute(CachedBleDevice.DEVICE_RANGE_INFO_DIALOG_ENABELR_FLAG));
        values.put(BleConstants.PXP_CONFIGURATION.RINGTONE_ENABLER,
                device.getBooleanAttribute(CachedBleDevice.DEVICE_RINGTONE_ENABLER_FLAG));
        values.put(BleConstants.PXP_CONFIGURATION.VOLUME,
                device.getIntAttribute(CachedBleDevice.DEVICE_VOLUME_FLAG));
        values.put(BleConstants.PXP_CONFIGURATION.VIBRATION_ENABLER,
                device.getBooleanAttribute(CachedBleDevice.DEVICE_VIBRATION_ENABLER_FLAG));
        if (device.getRingtoneUri() != null) {
            values.put(BleConstants.PXP_CONFIGURATION.RINGTONE, device.getRingtoneUri().toString());
        }
        // mContext.getContentResolver().insert(BLEConstants.TABLE_PXP_URI,
        // values);
    }

    private void updatePxpData(CachedBleDevice device, int which) {
        Log.d(TAG, "[updatePxpData] which = " + which);
        String selection = BleConstants.COLUMN_BT_ADDRESS + "='" + device.getDevice().getAddress()
                + "'";
        ContentResolver cr = mContext.getContentResolver();
        ContentValues values = new ContentValues();
        switch (which) {
        // TODO update these parameters by api
        case CachedBleDevice.DEVICE_RANGE_INFO_DIALOG_ENABELR_FLAG:
            values.put(BleConstants.PXP_CONFIGURATION.RANGE_ALERT_INFO_DIALOG_ENABLER,
                    device.getBooleanAttribute(which));
            break;

        case CachedBleDevice.DEVICE_RINGTONE_ENABLER_FLAG:
            values.put(BleConstants.PXP_CONFIGURATION.RINGTONE_ENABLER,
                    device.getBooleanAttribute(which));
            break;

        case CachedBleDevice.DEVICE_RINGTONE_URI_FLAG:
            values.put(BleConstants.PXP_CONFIGURATION.RINGTONE, device.getRingtoneUri().toString());
            break;

        case CachedBleDevice.DEVICE_VOLUME_FLAG:
            values.put(BleConstants.PXP_CONFIGURATION.VOLUME, device.getIntAttribute(which));
            break;

        case CachedBleDevice.DEVICE_VIBRATION_ENABLER_FLAG:
            values.put(BleConstants.PXP_CONFIGURATION.VIBRATION_ENABLER,
                    device.getBooleanAttribute(which));
            break;

        default:
            return;
        }
        cr.update(BleConstants.TABLE_UX_URI, values, selection, null);
    }

    private void updateClientPxpAttribute(final CachedBleDevice device) {
        if (!checkParamter(device)) {
            Log.w(TAG, "[updateClientPxpAttribute] checkParamter false,return.");
            return;
        }
        Log.d(TAG, "[updateClientPxpAttribute] device name : " + device.getDeviceName());

        Runnable r = new Runnable() {

            @Override
            public void run() {
                if (!isInDb(device, 0)) {
                    Log.d(TAG, "[updateClientPxpAttribute] device is not in db");
                    return;
                }
                // TODO update client pxp data by api
                Log.d(TAG, "[updateClientPxpAttribute] call to update client data");
                if (LocalBleManager.getInstance(mContext) != null) {
                    LocalBleManager
                            .getInstance(mContext)
                            .updateClientPxpData(
                                    device.getDevice(),
                                    device.getBooleanAttribute(CachedBleDevice.DEVICE_ALERT_SWITCH_ENABLER_FLAG),
                                    device.getBooleanAttribute(CachedBleDevice.DEVICE_RANGE_ALERT_ENABLER_FLAG),
                                    device.getBooleanAttribute(CachedBleDevice.DEVICE_DISCONNECTION_WARNING_EANBLER_FLAG),
                                    device.getIntAttribute(CachedBleDevice.DEVICE_IN_OUT_RANGE_ALERT_FLAG),
                                    device.getIntAttribute(CachedBleDevice.DEVICE_RANGE_VALUE_FLAG));
                }
            }

        };
        new Thread(r).start();
    }

    // TODO, should do delete action by calling api
    private void deletePxpData(CachedBleDevice device) {
        if (device == null || device.getDevice() == null) {
            Log.w(TAG, "[deletePxpData] device is null");
            return;
        }

    }

    private void updateDeviceAnsAttribute(final CachedBleDevice device, final int which) {
        if (!ANS_ATTRIBUTE_LIST.contains(which)) {
            Log.i(TAG, "[updateDeviceAnsAttribute] attribute is not in, no need to update");
            return;
        }
        Runnable r = new Runnable() {

            @Override
            public void run() {
                if (!checkParamter(device)) {
                    Log.w(TAG, "[updateDeviceAnsAttribute] parameter is wrong");
                    return;
                }
                Log.d(TAG, "[updateDeviceAnsAttribute] device name : " + device.getDeviceName());

                String selection = BleConstants.COLUMN_BT_ADDRESS + "='"
                        + device.getDevice().getAddress() + "'";
                ContentResolver cr = mContext.getContentResolver();
                Cursor queryCursor = cr.query(BleConstants.TABLE_ANS_URI, null, selection, null,
                        null);
                if (queryCursor == null) {
                    Log.w(TAG, "[updateDeviceAnsAttribute] queryCursor is null!!");
                    return;
                }
                if (queryCursor.getCount() == 0) {
                    Log.w(TAG, "[updateDeviceAnsAttribute] device is not in db!!");
                    queryCursor.close();
                    return;
                }
                queryCursor.close();

                ContentValues values = new ContentValues();
                Log.d(TAG, "[updateDeviceAnsAttribute] which = " + which);
                switch (which) {
                case CachedBleDevice.DEVICE_INCOMING_CALL_ENABLER_FLAG:
                    values.put(BleConstants.ANS_CONFIGURATION.ANS_HOST_CALL_ALERT,
                            device.getBooleanAttribute(which) ? 3 : 0);
                    break;

                case CachedBleDevice.DEVICE_MISSED_CALL_ENABLER_FLAG:
                    values.put(BleConstants.ANS_CONFIGURATION.ANS_HOST_MISSED_CALL_ALERT,
                            device.getBooleanAttribute(which) ? 3 : 0);
                    break;

                case CachedBleDevice.DEVICE_NEW_EMAIL_ENABLER_FLAG:
                    values.put(BleConstants.ANS_CONFIGURATION.ANS_HOST_EMAIL_ALERT,
                            device.getBooleanAttribute(which) ? 3 : 0);
                    break;

                case CachedBleDevice.DEVICE_NEW_MESSAGE_ENABLER_FLAG:
                    values.put(BleConstants.ANS_CONFIGURATION.ANS_HOST_SMSMMS_ALERT,
                            device.getBooleanAttribute(which) ? 3 : 0);
                    break;

                default:
                    return;
                }
                cr.update(BleConstants.TABLE_ANS_URI, values, selection, null);

                Log.d(TAG, "[updateDeviceAnsAttribute] notify host configuration has been changed");
            }

        };
        new Thread(r).start();
    }

    public void deleteDevice(final CachedBleDevice device) {
        Runnable r = new Runnable() {

            @Override
            public void run() {
                if (!checkParamter(device)) {
                    Log.d(TAG, "[deleteDevice]parameter is wrong!");
                    return;
                }
                String selection = BleConstants.COLUMN_BT_ADDRESS + "='"
                        + device.getDevice().getAddress() + "'";
                ContentResolver cr = mContext.getContentResolver();
                Cursor queryCursor = cr.query(BleConstants.TABLE_UX_URI, null, selection, null,
                        null);
                if (queryCursor == null) {
                    Log.w(TAG, "[updateDeviceAttribute] queryCursor is null!!");
                    return;
                }
                if (queryCursor.getCount() == 0) {
                    Log.w(TAG, "[updateDeviceAttribute] device is not in db!!");
                    queryCursor.close();
                    return;
                }

                queryCursor.close();

                cr.delete(BleConstants.TABLE_UX_URI, selection, null);
                // TODO call API to delete device which in bluetoothle.db
                deletePxpData(device);
            }

        };

        new Thread(r).start();
    }

    private static boolean checkParamter(CachedBleDevice device) {
        if (device == null) {
            return false;
        }
        if (device.getDevice() == null) {
            return false;
        }
        return true;
    }

}
