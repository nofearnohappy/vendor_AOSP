
package com.mediatek.bluetoothle.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import java.util.HashSet;

public class TipDbOperator {

    private static final String TAG = "[TipDbOperator]";

    /**
     * insert data to tip table
     *
     * @param context
     * @param btAddr
     * @param value
     */
    public static void insertData(Context context, String btAddr, boolean value) {
        if (!verifyParameter(context, btAddr)) {
            Log.d(TAG, "[insertData] parameter is wrong");
            return;
        }
        ContentValues values = new ContentValues();
        values.put(BLEConstants.COLUMN_BT_ADDRESS, btAddr);
        values.put(BLEConstants.TIP.TIP_NOTIFIER, value ? 1 : 0);
        context.getContentResolver().insert(BLEConstants.TABLE_TIP_URI, values);
    }

    /**
     * update value which in tip table
     *
     * @param context
     * @param btAddr
     * @param type
     * @param newValue
     */
    public static void updateData(Context context, String btAddr, String type, boolean newValue) {
        if (!verifyParameter(context, btAddr)) {
            Log.d(TAG, "[updateData] parameter is wrong");
            return;
        }
        String where = BLEConstants.COLUMN_BT_ADDRESS + "='" + btAddr + "'";
        int updateValue = newValue ? 1 : 0;
        ContentValues values = new ContentValues();
        values.put(BLEConstants.TIP.TIP_NOTIFIER, updateValue);
        context.getContentResolver().update(BLEConstants.TABLE_TIP_URI, values, where, null);
    }

    /**
     * delete entry which in tip table according btAddr
     *
     * @param context
     * @param btAddr
     */
    public static void deleteData(Context context, String btAddr) {
        if (!verifyParameter(context, btAddr)) {
            Log.d(TAG, "[deleteData] parameter is wrong");
            return;
        }
        String where = BLEConstants.COLUMN_BT_ADDRESS + "='" + btAddr + "'";
        context.getContentResolver().delete(BLEConstants.TABLE_TIP_URI, where, null);
    }

    /**
     * get boolean value from tip table according bt address
     *
     * @param context
     * @param btAddr
     * @param type
     * @return
     */
    public static boolean getBooleanData(Context context, String btAddr, String type) {
        if (!verifyParameter(context, btAddr)) {
            Log.d(TAG, "[getBooleanData] parameter is wrong");
            return false;
        }
        String where = BLEConstants.COLUMN_BT_ADDRESS + "='" + btAddr + "'";
        Cursor cur = context.getContentResolver().query(BLEConstants.TABLE_TIP_URI,
                new String[] {
                    BLEConstants.TIP.TIP_NOTIFIER
                }, where, null, null);
        if (cur == null) {
            Log.d(TAG, "[getBooleanData] cursor is null");
            return false;
        }
        if (cur.getCount() == 0) {
            Log.d(TAG, "[getBooleanData] cursor count is 0");
            cur.close();
            return false;
        }
        boolean retValue = false;
        try {
            if (cur.moveToFirst()) {
                int va = cur.getInt(0);
                if (va == 1) {
                    retValue = true;
                } else {
                    retValue = false;
                }
            }
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
        return retValue;
    }

    /**
     * get bt addressed according to queryValue
     *
     * @param context
     * @param queryValue
     * @return
     */
    public static HashSet<String> getNotifyDeviceAddresses(Context context, boolean queryValue) {
        if (context == null) {
            Log.d(TAG, "[getNotifyDeviceAddresses] context is null");
            return null;
        }
        int qV = 0;
        if (queryValue) {
            qV = 1;
        }
        String selection = BLEConstants.TIP.TIP_NOTIFIER + "=" + qV;
        Cursor cur = context.getContentResolver().query(
                BLEConstants.TABLE_TIP_URI, new String[] {
                        BLEConstants.COLUMN_BT_ADDRESS
                },
                selection, null, null);
        if (cur == null) {
            return null;
        }
        if (cur.getCount() == 0) {
            cur.close();
            return null;
        }
        HashSet<String> retStringSet = new HashSet<String>();
        try {
            if (cur.moveToFirst()) {
                do {
                    String va = cur.getString(0);
                    retStringSet.add(va);
                } while (cur.moveToNext());
            }
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
        return retStringSet;
    }

    /**
     * check parameter effective
     *
     * @param context
     * @param btAddr
     * @return
     */
    private static boolean verifyParameter(Context context, String btAddr) {
        if (context == null) {
            Log.d(TAG, "[verifyParameter] context is null");
            return false;
        }
        if (btAddr == null || btAddr.trim().length() == 0) {
            Log.d(TAG, "[verifyParameter] btAddr is wrong");
            return false;
        }
        Log.d(TAG, "[verifyParameter] return true");
        return true;
    }

}
