
package com.mediatek.bluetoothle.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.mediatek.bluetooth.BleProximityProfileService;

import java.util.ArrayList;
import java.util.List;

public class DeviceParameterRecorder {

    private static final String TAG = "[BT][BLE][DeviceParameterRecorder]";

    private static final String URI_STRING = BLEConstants.HEADER
            + BLEConstants.AUTORITY + "/" + BLEConstants.CLIENT_TABLE.TABLE_NAME;

    private static final Uri PXP_URI = Uri.parse(URI_STRING);

    public static class DevicePxpParams {
        public int mAlertEnabler;
        public int mRangeAlertEnabler;
        public int mRangeType;
        public int mRangeValue;
        public int mDisconnEnabler;
    };

    /**
     * @param context
     * @param btAddr
     * @param isSupportOptional
     */
    public static void insertNewDevice(Context context, final String btAddr) {
        if (context == null) {
            Log.d(TAG, "[insertNewDevice] CONTEXT IS NULL!!");
            return;
        }
        if (btAddr == null || btAddr.trim().length() == 0) {
            Log.d(TAG, "[insertNewDevice] btAddr should not be empty!!!");
            return;
        }
        ContentValues values = new ContentValues();
        values.put(BLEConstants.COLUMN_BT_ADDRESS, btAddr);
        values.put(BLEConstants.CLIENT_TABLE.DEVICE_AUTO_CONNECT, true);
        values.put(BLEConstants.CLIENT_TABLE.RANGE_ALERT_ENABLER, true);
        values.put(BLEConstants.CLIENT_TABLE.ALERT_ENABLER, true);
        values.put(BLEConstants.CLIENT_TABLE.DISCONNECTION_WARNING_ENABLER, true);
        values.put(BLEConstants.CLIENT_TABLE.RANGE_TYPE,
                BleProximityProfileService.RANGE_ALERT_TYPE_OUT);
        values.put(BLEConstants.CLIENT_TABLE.RANGE_VALUE,
                BleProximityProfileService.RANGE_ALERT_RANGE_FAR);
        values.put(BLEConstants.CLIENT_TABLE.DEVICE_FMP_STATE, 0);
        context.getContentResolver().insert(BLEConstants.TABLE_CLIENT_URI, values);
    }

    public static void deleteDevice(Context context, final String btAddr) {
        if (context == null) {
            Log.d(TAG, "[insertNewDevice] CONTEXT IS NULL!!");
            return;
        }
        if (btAddr == null || btAddr.trim().length() == 0) {
            Log.d(TAG, "[insertNewDevice] btAddr should not be empty!!!");
            return;
        }
        Log.d(TAG, "[deleteDevice] enter");
        String selection = BLEConstants.COLUMN_BT_ADDRESS + "='" + btAddr + "'";
        context.getContentResolver().delete(BLEConstants.TABLE_CLIENT_URI, selection, null);
    }

    /**
     * Which used to update integer value that stored in db.
     *
     * @param btAddr remote bluetooth device address. according to {@link} initCondition.
     * @param type Which integer field to update.
     * @param oldValue Old value
     * @param newValue New value which will be update to db.
     */
    public static boolean setPxpClientParam(Context context, final String btAddr,
                        int alertEnabler, int rangeAlertEnabler,
                        int rangeType, int rangeValue, int disconnEnabler) {
        boolean res = false;
        if (context == null) {
            Log.d(TAG, "[setPxpClientParam] context is null");
            return false;
        }
        if (btAddr == null || btAddr.trim().length() == 0) {
            Log.d(TAG, "[setPxpClientParam] btAddr is wrong");
            return false;
        }
        String selection = BLEConstants.COLUMN_BT_ADDRESS + "='" + btAddr + "'";
        ContentValues values = new ContentValues();
        values.put(BLEConstants.CLIENT_TABLE.ALERT_ENABLER, alertEnabler);
        values.put(BLEConstants.CLIENT_TABLE.RANGE_ALERT_ENABLER, rangeAlertEnabler);
        values.put(BLEConstants.CLIENT_TABLE.DISCONNECTION_WARNING_ENABLER, disconnEnabler);
        values.put(BLEConstants.CLIENT_TABLE.RANGE_TYPE, rangeType);
        values.put(BLEConstants.CLIENT_TABLE.RANGE_VALUE, rangeValue);
        int i = context.getContentResolver().update(
                BLEConstants.TABLE_CLIENT_URI, values, selection, null);
        if (i > 0) {
            res = true;
        }
        return res;
    }

    public static DevicePxpParams getPxpClientParam(Context context, final String btAddr) {
        if (context == null) {
            Log.d(TAG, "[getPxpClientParam] context is null");
            return null;
        }
        if (btAddr == null || btAddr.trim().length() == 0) {
            Log.d(TAG, "[getAutoConnectFlag] btAddr is worng");
            return null;
        }

        DevicePxpParams pxpParam = new DevicePxpParams();

        String selection = BLEConstants.COLUMN_BT_ADDRESS + "='" + btAddr + "'";
        String[] projection = new String[] {
                BLEConstants.CLIENT_TABLE.ALERT_ENABLER,
                BLEConstants.CLIENT_TABLE.RANGE_ALERT_ENABLER,
                BLEConstants.CLIENT_TABLE.DISCONNECTION_WARNING_ENABLER,
                BLEConstants.CLIENT_TABLE.RANGE_TYPE, BLEConstants.CLIENT_TABLE.RANGE_VALUE
        };
        Cursor cursor = context.getContentResolver().query(BLEConstants.TABLE_CLIENT_URI,
                projection, selection, null, null);
        if (cursor == null) {
            Log.d(TAG, "[getAutoConnectFlag] cursor is null");
            return null;
        }
        if (cursor.getCount() == 0) {
            Log.d(TAG, "[getAutoConnectFlag] cursor count is 0");
            cursor.close();
            return null;
        }
        if (cursor.moveToFirst()) {
            pxpParam.mAlertEnabler = cursor.getInt(cursor.getColumnIndex(
                    BLEConstants.CLIENT_TABLE.ALERT_ENABLER));
            pxpParam.mRangeAlertEnabler = cursor.getInt(cursor.getColumnIndex(
                    BLEConstants.CLIENT_TABLE.RANGE_ALERT_ENABLER));
            pxpParam.mRangeType = cursor.getInt(cursor.getColumnIndex(
                    BLEConstants.CLIENT_TABLE.RANGE_TYPE));
            pxpParam.mRangeValue = cursor.getInt(cursor.getColumnIndex(
                    BLEConstants.CLIENT_TABLE.RANGE_VALUE));
            pxpParam.mDisconnEnabler = cursor.getInt(cursor.getColumnIndex(
                    BLEConstants.CLIENT_TABLE.DISCONNECTION_WARNING_ENABLER));
        }
        cursor.close();

        Log.d(TAG, "[getPxpClientParam] end " + " AlertEnabler " + pxpParam.mAlertEnabler +
                " rangeAlertEnabler " + pxpParam.mRangeAlertEnabler);
        return pxpParam;
    }

    public static void setAutoConnectFlag(
            Context context, final String btAddr, final int autoConnect) {
        if (context == null) {
            Log.d(TAG, "[setAutoConnectFlag] context is null");
            return;
        }
        if (btAddr == null || btAddr.trim().length() == 0) {
            Log.d(TAG, "[setAutoConnectFlag] btAddr is wrong");
            return;
        }
        String selection = BLEConstants.COLUMN_BT_ADDRESS + "='" + btAddr + "'";
        ContentValues values = new ContentValues();
        values.put(BLEConstants.CLIENT_TABLE.DEVICE_AUTO_CONNECT, autoConnect);
        context.getContentResolver().update(BLEConstants.TABLE_CLIENT_URI, values, selection, null);
        return;
    }

    public static boolean getAutoConnectFlag(
            Context context, final String btAddr, Integer autoConnect) {
        if (context == null) {
            Log.d(TAG, "[getAutoConnectFlag] context is null");
            return false;
        }
        if (btAddr == null || btAddr.trim().length() == 0) {
            Log.d(TAG, "[getPxpClientParam] btAddr is worng");
            return false;
        }
        String selection = BLEConstants.COLUMN_BT_ADDRESS + "='" + btAddr + "'";
        String[] projection = new String[] {
                BLEConstants.CLIENT_TABLE.DEVICE_AUTO_CONNECT
        };
        Cursor cursor = context.getContentResolver().query(BLEConstants.TABLE_CLIENT_URI,
                projection, selection, null, null);
        if (cursor == null) {
            Log.d(TAG, "[getPxpClientParam] cursor is null");
            return false;
        }
        if (cursor.getCount() == 0) {
            Log.d(TAG, "[getPxpClientParam] cursor count is 0");
            cursor.close();
            return false;
        }
        if (cursor.moveToFirst()) {
            autoConnect = cursor.getInt(cursor.getColumnIndex(
                    BLEConstants.CLIENT_TABLE.DEVICE_AUTO_CONNECT));
        }
        cursor.close();
        return true;
    }

    public static List<String> getAutoConnectDeviceAddresses(Context context) {
        final String[] projection = {
                BLEConstants.COLUMN_BT_ADDRESS, BLEConstants.CLIENT_TABLE.DEVICE_AUTO_CONNECT
        };
        final String selection = BLEConstants.CLIENT_TABLE.DEVICE_AUTO_CONNECT + " = ?";
        final String[] selectionArgs = {
                "1"
        };

        List<String> addrList = null;

        final Cursor addrCursor = context.getContentResolver().query(BLEConstants.TABLE_CLIENT_URI,
                projection, selection, selectionArgs, null);

        try {

            if (null == addrCursor) {

                Log.e(TAG, "Table Cursor is NULL");

                return addrList;
            }

            final int deviceCount = addrCursor.getCount();

            if (deviceCount < 1) {

                Log.e(TAG, "No device should be auto-connected");

                return addrList;
            }

            addrList = new ArrayList<String>();

            while (addrCursor.moveToNext()) {
                final String address = addrCursor.getString(0);
                final int auto = addrCursor.getInt(1);

                if (address != null) {
                    addrList.add(address);
                }

                Log.e(TAG, "Auto-connect device:" + address + ", auto_connect:" + auto);
            }
        } finally {
            if (addrCursor != null) {
                addrCursor.close();
            }
        }

        return addrList;
    }

    public static boolean setTagetFMLevel(Context context, final String btAddr, final int level) {
        if (context == null) {
            Log.d(TAG, "[setTagetFMLevel] context is null");
            return false;
        }
        if (btAddr == null || btAddr.trim().length() == 0) {
            Log.d(TAG, "[setTagetFMLevel] btAddr is wrong");
            return false;
        }
        String selection = BLEConstants.COLUMN_BT_ADDRESS + "='" + btAddr + "'";
        ContentValues values = new ContentValues();
        values.put(BLEConstants.CLIENT_TABLE.DEVICE_FMP_STATE, level);
        context.getContentResolver().update(BLEConstants.TABLE_CLIENT_URI, values, selection, null);
        return true;
    }

    public static int getTagetFMLevel(Context context, final String btAddr) {
        int level = 0;
        if (context == null) {
            Log.d(TAG, "[getTagetFMLevel] context is null");
            return 0;
        }
        if (btAddr == null || btAddr.trim().length() == 0) {
            Log.d(TAG, "[getTagetFMLevel] btAddr is worng");
            return 0;
        }
        String selection = BLEConstants.COLUMN_BT_ADDRESS + "='" + btAddr + "'";
        String[] projection = new String[] {
                BLEConstants.CLIENT_TABLE.DEVICE_FMP_STATE
        };
        Cursor cursor = context.getContentResolver().query(BLEConstants.TABLE_CLIENT_URI,
                projection, selection, null, null);
        if (cursor == null) {
            Log.d(TAG, "[getPxpClientParam] cursor is null");
            return 0;
        }
        if (cursor.getCount() == 0) {
            Log.d(TAG, "[getPxpClientParam] cursor count is 0");
            cursor.close();
            return 0;
        }
        if (cursor.moveToFirst()) {
            level = cursor.getInt(cursor.getColumnIndex(
                    BLEConstants.CLIENT_TABLE.DEVICE_FMP_STATE));
        }
        cursor.close();
        return level;
    }

    public static ArrayList<String> getDeviceAddresses(Context context) {
        if (context == null) {
            Log.d(TAG, "[getDeviceAddresses] context is null");
            return null;
        }
        ArrayList<String> retList = new ArrayList<String>();

        Cursor cur = context.getContentResolver().query(BLEConstants.TABLE_CLIENT_URI,
                new String[] {
                    BLEConstants.COLUMN_BT_ADDRESS
                }, null, null, null);
        if (cur == null) {
            Log.d(TAG, "[getDeviceAddresses] cur is null!!!");
            return null;
        }
        if (cur.getCount() == 0) {
            Log.d(TAG, "[getDeviceAddresses] cur count is 0 !!!");
            cur.close();
            return retList;
        }
        Log.d(TAG, "[getDeviceAddresses] cur count : " + cur.getCount());
        try {
            if (cur.moveToFirst()) {
                do {
                    retList.add(cur.getString(0));
                } while (cur.moveToNext());
            }
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
        return retList;
    }

    /**
     * register a content observer which used to listen db changed action
     *
     * @param context
     * @param observer
     */
    public static void registerRecoderObserver(Context context, ContentObserver observer) {
        if (context == null) {
            Log.d(TAG, "[registerRecoderObserver] context is null");
            return;
        }
        if (observer == null) {
            Log.d(TAG, "[registerRecoderObserver] observer is null");
            return;
        }
        Log.d(TAG, "[registerRecoderObserver] register content observer");
        context.getContentResolver().registerContentObserver(
                BLEConstants.TABLE_CLIENT_URI, true, observer);
    }

    /**
     * unregister the content observer which has been registered in @link registerDbChangeCallback
     *
     * @param context
     * @param observer
     */
    public static void unregisterRecorderObserver(Context context, ContentObserver observer) {
        if (context == null) {
            Log.d(TAG, "[unregisterRecorderObserver] context is null");
            return;
        }
        if (observer == null) {
            Log.d(TAG, "[unregisterRecorderObserver] observer is null");
            return;
        }
        Log.d(TAG, "[unregisterRecorderObserver] unregister content observer");
        context.getContentResolver().unregisterContentObserver(observer);
    }

}
