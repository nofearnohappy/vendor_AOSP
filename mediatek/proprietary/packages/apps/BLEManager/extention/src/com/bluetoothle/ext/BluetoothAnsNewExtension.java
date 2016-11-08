package com.bluetoothle.ext;

import android.content.Context;
import android.util.Log;

import com.bluetoothle.ext.detector.BleHostStatusChangeDetector;

import java.util.ArrayList;
import java.util.TreeSet;

import com.mediatek.bluetoothle.ext.BluetoothAnsDetector;
import com.mediatek.bluetoothle.ext.IBluetoothLeAnsExtension;

public class BluetoothAnsNewExtension implements IBluetoothLeAnsExtension {

    ArrayList<BluetoothAnsDetector> mDetectorList = null;

    private static final String TAG = "BluetoothLeAnsStateBar";
    public ArrayList<BluetoothAnsDetector> getDetectorArray(Context context) {
        Log.d(TAG, "getDetectorArray");
        if (mDetectorList == null) {
            createDetectorList(context);
        }
        return mDetectorList;
    }

    public TreeSet<Byte> getExtraCategoryId() {
        TreeSet<Byte> extraCategoryIdSet = new TreeSet<Byte>();
        extraCategoryIdSet.add(BleHostStatusChangeDetector.CATEGORY_ID_HOST_STATUS);
        return extraCategoryIdSet;
    }

    private void createDetectorList(Context context) {
        mDetectorList = new ArrayList<BluetoothAnsDetector>();
        mDetectorList.add(new BleHostStatusChangeDetector(context));
    }
}
