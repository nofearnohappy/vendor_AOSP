package com.mediatek.hotknotbeam;

import android.util.Log;

import java.util.Random;


public class Utils {
    private final static String TAG = HotKnotBeamService.TAG;

    public static int getGroupId() {
        Random rn = new Random(System.currentTimeMillis());
        int id = rn.nextInt(HotKnotBeamConstants.MAX_GROUP_MAX_ID - HotKnotBeamConstants.MAX_GROUP_MIN_ID + 1) + HotKnotBeamConstants.MAX_GROUP_MAX_ID;
        Log.d(TAG, "getGroupId:" + id);
        return id;
    }

    public static int getId() {
        Random rn = new Random(System.currentTimeMillis());
        int id = rn.nextInt(HotKnotBeamConstants.MAX_MAX_ID - HotKnotBeamConstants.MAX_MIN_ID + 1) + HotKnotBeamConstants.MAX_MAX_ID;
        Log.d(TAG, "getId:" + id);
        return id;
    }
}