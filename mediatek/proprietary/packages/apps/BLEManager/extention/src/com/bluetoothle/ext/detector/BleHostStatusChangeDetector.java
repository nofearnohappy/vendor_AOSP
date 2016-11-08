package com.bluetoothle.ext.detector;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;

import com.bluetoothle.ext.BLEConstants;

import java.util.HashMap;

import com.mediatek.bluetoothle.ext.BluetoothAnsDetector;

public class BleHostStatusChangeDetector extends BluetoothAnsDetector {

    private static final String TAG = "[BluetoothAns]BleHostStatusChangeDetector";
    private static final Uri PXP_URI = Uri.parse(BLEConstants.PXP.TABLE_PXP);
    private SparseArray<PxpStateRegister> mStateMap = new SparseArray<PxpStateRegister>();
    private static final int CATEGORY_TYPE_NEW = 0x01;
    private static final int CATEGORY_TYPE_UNREAD = 0x02;

    private static final String[] PROJECTION_PXP = {
        BLEConstants.COLUMN_ID,
        BLEConstants.COLUMN_BT_ADDRESS,
        BLEConstants.PXP.ALERT_ENABLER,
        BLEConstants.PXP.RANGE_ALERT_ENABLER,
        BLEConstants.PXP.RNAGLE_ALERT_INFO_DIALOG_ENABLER,
        BLEConstants.PXP.RANGE_VALUE,
        BLEConstants.PXP.RANGE_TYPE,
        BLEConstants.PXP.DISCONNECTION_WARNING_ENABLER,
        //BLEConstants.PXP.RINGTONE_ENABLER,
        //BLEConstants.PXP.VIBRATION_ENABLER,
        //BLEConstants.PXP.IS_SUPPORT_OPTIONAL,
    };

    private static final int COLUMN_ID = 0;
    private static final int COLUMN_BT_ADDRESS = 1;
    private static final int COLUMN_ALERT_ENABLER = 2;
    private static final int COLUMN_RANGE_ALERT_ENABLER = 3;
    private static final int COLUMN_RNAGLE_ALERT_INFO_DIALOG_ENABLER = 4;
    private static final int COLUMN_RANGE_VALUE = 5;
    private static final int COLUMN_RANGE_TYPE = 6;
    private static final int COLUMN_DISCONNECTION_WARNING_ENABLER = 7;
    //private static final int COLUMN_RINGTONE_ENABLER = 7;
    //private static final int COLUMN_VIBRATION_ENABLER = 8;
    //private static final int COLUMN_IS_SUPPORT_OPTIONAL = 9;

    private static final String[] ALERT_STRING_GROUP = {
        null, //this line is for matching index, never used
        null,
        "alert enable changed to ",
        "range alert enable changed to ",
        "range alert dialog enable changed to ",
        "range value changed to ",
        "range type changed to ",
        "disconnection warning enable changed to "
    };

    private ContentObserver mObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange, Uri uri) {
            Cursor cursor = mContext.getContentResolver().query(
                    uri, PROJECTION_PXP, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    PxpStateRegister register = new PxpStateRegister(cursor);
                    PxpStateRegister oldRegister = null;
                    String notifyString = null;
                    int id = 0;
                    String idString = uri.getLastPathSegment();
                    synchronized (mStateMap) {
                        if (idString != null) {
                            id = Integer.parseInt(idString);
                            if (mStateMap != null) {
                                oldRegister = mStateMap.get(id);
                            }
                        }
                        if (mStateMap != null) {
                            mStateMap.put(id, register);
                        }
                    }
                    if (oldRegister != null) {
                        compareStatusAndNotify(register, oldRegister);
                    }
                } else {
                    String idString = uri.getLastPathSegment();
                    Log.d(TAG, "remove uri = " + uri);
                    if (idString != null) {
                        int id = Integer.parseInt(idString);
                        synchronized (mStateMap) {
                            if (mStateMap.get(id) != null) {
                                mStateMap.remove(id);
                                Log.d(TAG, "remove pxp device from detector, id = " + id);
                            }
                        }
                    }

                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        };
    };

    public static final byte CATEGORY_ID_HOST_STATUS = 14;
    private HashMap<String, SparseArray<Integer>> mDeviceRegisterMap =
            new HashMap<String, SparseArray<Integer>>();

    public BleHostStatusChangeDetector(Context context) {
        super(context);
        mCategoryId = CATEGORY_ID_HOST_STATUS;
    }

    @Override
    public void initializeAll() {
        initNewAlertStatus();
        initNewDetector();
    }

    private void initNewAlertStatus() {
        new Thread() {
            @Override
            public void run() {
                Cursor cursor = mContext.getContentResolver().query(
                        PXP_URI, PROJECTION_PXP, null, null, null);
                if (cursor != null) {
                    try {
                        synchronized (mStateMap) {
                            while (cursor.moveToNext()) {
                                int id = cursor.getInt(COLUMN_ID);
                                mStateMap.put(id, new PxpStateRegister(cursor));
                            }
                        }
                    } finally {
                        cursor.close();
                    }
                }
            }
        } .start();
    }

    @Override
    public void clearAll() {
        mContext.getContentResolver().unregisterContentObserver(mObserver);
    }

    private void initNewDetector() {
        mContext.getContentResolver().registerContentObserver(PXP_URI, true, mObserver);
    }

    private void compareStatusAndNotify(PxpStateRegister register, PxpStateRegister oldRegister) {
        String address = register.mAddress;
        if (address != null) {
            String notifyString = register.compareRegister(oldRegister);
            if (notifyString != null) {
                setNewAlertText(notifyString);
                onAlertNotify(address, CATEGORY_TYPE_NEW);
            }
        }

    }

    private class PxpStateRegister {
        public String mAddress = null;
        public SparseArray<Integer> mIntegerStatusMap = new SparseArray<Integer>();

        public PxpStateRegister(Cursor cursor) {
            mAddress = cursor.getString(COLUMN_BT_ADDRESS);
            for (int i = COLUMN_ALERT_ENABLER; i <= COLUMN_DISCONNECTION_WARNING_ENABLER; i++) {
                mIntegerStatusMap.put(i, cursor.getInt(i));
            }
        }

        public String compareRegister(PxpStateRegister oldReg) {
            if (oldReg == null || mAddress != oldReg.mAddress) {
                return null;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = COLUMN_ALERT_ENABLER; i <= COLUMN_DISCONNECTION_WARNING_ENABLER; i++) {
                    if (mIntegerStatusMap.get(i) < oldReg.mIntegerStatusMap.get(i)) {
                        if (stringBuilder.length() > 0) {
                            stringBuilder.append("\n");
                        }
                        stringBuilder.append(ALERT_STRING_GROUP[i]);
                        stringBuilder.append(mIntegerStatusMap.get(i));
                    }
                }
                if (stringBuilder.length() > 0) {
                    return stringBuilder.toString();
                } else {
                    return null;
                }
            }
        }
    }
}
