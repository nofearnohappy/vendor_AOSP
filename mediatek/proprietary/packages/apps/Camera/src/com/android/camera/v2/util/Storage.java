/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.camera.v2.util;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;

import com.android.camera.FeatureSwitcher;

import com.mediatek.storage.StorageManagerEx;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

public class Storage {
    private static final String TAG = "Storage";

    public static final String DCIM  = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DCIM).toString();

    public static final String DIRECTORY  = DCIM + "/Camera";

    public static final String FOLDER_PATH = "/" + Environment.DIRECTORY_DCIM + "/Camera";

    // Match the code in MediaProvider.computeBucketValues().
    public static final String BUCKET_ID = String.valueOf(DIRECTORY.toLowerCase().hashCode());

    public static final long UNAVAILABLE                 = -1L;
    public static final long PREPARING                   = -2L;
    public static final long UNKNOWN_SIZE                = -3L;
    public static final long FULL_SDCARD                 = -4L;
    public static final long LOW_STORAGE_THRESHOLD;
    public static final long RECORD_LOW_STORAGE_THRESHOLD;

    // / M: for more file type and picture type @{
    public static final int CANNOT_STAT_ERROR            = -2;
    public static final int PICTURE_TYPE_JPG             = 0;
    public static final int PICTURE_TYPE_MPO             = 1;
    public static final int PICTURE_TYPE_JPS             = 2;
    public static final int PICTURE_TYPE_MPO_3D          = 3;

    public static final int FILE_TYPE_PHOTO              = 0;
    public static final int FILE_TYPE_VIDEO              = 1;
    public static final int FILE_TYPE_PANO               = 2;
    public static final int FILE_TYPE_PIP_VIDEO          = 3;


    private static StorageManager                       sStorageManager;
    //TODO add for sanity issue, need check
    private static Context mContext;
    private static String                               sMountPoint;

    static {
        if (FeatureSwitcher.isMtkFatOnNand() || FeatureSwitcher.isGmoROM()) {
            LOW_STORAGE_THRESHOLD = 10000000;
            RECORD_LOW_STORAGE_THRESHOLD = 9600000;
            Log.i(TAG, "LOW_STORAGE_THRESHOLD= 10000000");
        } else {
            LOW_STORAGE_THRESHOLD = 50000000;
            RECORD_LOW_STORAGE_THRESHOLD = 48000000;
            Log.i(TAG, "LOW_STORAGE_THRESHOLD= 50000000");
        }
    }

    public static boolean initializeStorageState() {
        StorageManager storageManager = getStorageManager();
        String defaultPath = StorageManagerEx.getDefaultPath();
        boolean diff = false;
        String old = sMountPoint;
        sMountPoint = defaultPath;
        if (old != null && old.equalsIgnoreCase(sMountPoint)) {
            diff = true;
        }
        String state = storageManager.getVolumeState(sMountPoint);
        setStorageReady(Environment.MEDIA_MOUNTED.equals(state));
        Log.d(TAG, "initializeStorageState() old=" + old + ", sMountPoint=" + sMountPoint
                + " return " + diff);
        return diff;
    }

    private static StorageManager getStorageManager() {
        if (sStorageManager == null) {
            try {
                sStorageManager = new StorageManager(mContext, null);
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
        return sStorageManager;
    }

    public static void setContext(Context context) {
        mContext = context;
    }

    public static boolean isSDCard() {
        StorageManager storageManager = getStorageManager();
        String storagePath = sMountPoint; // storageManager.getDefaultPath();
        StorageVolume[] volumes = storageManager.getVolumeList();
        int nVolume = -1;
        if (volumes != null) {
            for (int i = 0; i < volumes.length; i++) {
                if (volumes[i].getPath().equals(storagePath)) {
                    nVolume = i;
                    break;
                }
            }
            boolean isSd = false;
            if (nVolume != -1) {
                isSd = volumes[nVolume].isRemovable();
            }
            Log.d(TAG, "isSDCard() storagePath=" + storagePath + " return " + isSd);
            return isSd;
        } else {
            return false;
        }

    }

    public static boolean isMultiStorage() {
        StorageManager storageManager = getStorageManager();
        StorageVolume[] volumes = storageManager.getVolumeList();
        if (volumes != null) {
            return volumes.length > 1;
        } else {
            return false;
        }
    }

    public static Long getStorageCapbility() {
        StorageManager storageManager = getStorageManager();
        String storagePath = sMountPoint; // storageManager.getDefaultPath();
        StorageVolume[] volumes = storageManager.getVolumeList();
        int nVolume = -1;
        if (volumes != null) {
            for (int i = 0; i < volumes.length; i++) {
                if (volumes[i].getPath().equals(storagePath)) {
                    nVolume = i;
                    break;
                }
            }
            Long maxFileSize = 0L;
            if (nVolume != -1) {
                maxFileSize = volumes[nVolume].getMaxFileSize();
                Log.i(TAG, "getStorageCapbility maxFileSize = " + maxFileSize + ",nVolume = "
                        + nVolume);
            }
            return maxFileSize;
        } else {
            return 0L;
        }
    }

    public static boolean isHaveExternalSDCard() {
        StorageManager storageManager = getStorageManager();
        StorageVolume[] volumes = storageManager.getVolumeList();
        for (int i = 0; i < volumes.length; i++) {
            if (volumes[i].isRemovable()
                    && Environment.MEDIA_MOUNTED.equals(storageManager.getVolumeState(volumes[i]
                            .getPath()))) {
                return true;
            }
        }
        return false;
    }

    // get internal volume path
    public static String getInternalVolumePath() {
        StorageManager storageManager = getStorageManager();
        StorageVolume[] volumes = storageManager.getVolumeList();
        for (int i = 0; i < volumes.length; i++) {
            if (!volumes[i].isRemovable()
                    && Environment.MEDIA_MOUNTED.equals(storageManager.getVolumeState(volumes[i]
                            .getPath()))) {
                return volumes[i].getPath();
            }
        }
        return null;
    }

    public static long getAvailableSpace() {
        String state;
        StorageManager storageManager = getStorageManager();
        state = storageManager.getVolumeState(sMountPoint);
        // Log.d(TAG, "External storage state=" + state + ", mount point = " +
        // sMountPoint);
        if (Environment.MEDIA_CHECKING.equals(state)) {
            return PREPARING;
        }
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return UNAVAILABLE;
        }

        File dir = new File(getFileDirectory());
        dir.mkdirs();
        boolean isDirectory = dir.isDirectory();
        boolean canWrite = dir.canWrite();
        if (!isDirectory || !canWrite) {
            Log.d(TAG, "getAvailableSpace() isDirectory=" + isDirectory + ", canWrite=" + canWrite);
            return FULL_SDCARD;
        }

        try {
            // Here just use one directory to stat fs.
            StatFs stat = new StatFs(getFileDirectory());
            return stat.getAvailableBlocks() * (long) stat.getBlockSize();
        } catch (Exception e) {
            Log.e(TAG, "Fail to access external storage", e);
        }
        return UNKNOWN_SIZE;
    }

    /**
     * OSX requires plugged-in USB storage to have path /DCIM/NNNAAAAA to be
     * imported. This is a temporary fix for bug#1655552.
     */
    public static void ensureOSXCompatible() {
        File nnnAAAAA = new File(DCIM, "100ANDRO"); // should check dcim
        if (!(nnnAAAAA.exists() || nnnAAAAA.mkdirs())) {
            Log.e(TAG, "Failed to create " + nnnAAAAA.getPath());
        }
    }

    public static String getMountPoint() {
        return sMountPoint;
    }

    private static boolean sStorageReady;

    public static boolean isStorageReady() {
        Log.i(TAG, "isStorageReady() mount point = " + sMountPoint + ", return " + sStorageReady);
        return sStorageReady;
    }

    public static void setStorageReady(boolean ready) {
        Log.d(TAG, "setStorageReady(" + ready + ") sStorageReady=" + sStorageReady);
        sStorageReady = ready;
    }

    public static void mkFileDir(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            Log.d(TAG, "dir not exit,will create this, path = " + path);
            dir.mkdirs();
        }
    }
    public static boolean updateDefaultDirectory() {
        mkFileDir(getFileDirectory());
        return initializeStorageState();
    }

    public static boolean updateDirectory(String path) {
        StorageManager storageManager = getStorageManager();
        boolean diff = false;
        String old = sMountPoint;
        sMountPoint = path;
        if (old != null && old.equalsIgnoreCase(sMountPoint)) {
            diff = true;
        }
        mkFileDir(getFileDirectory());
        String state = storageManager.getVolumeState(sMountPoint);
        setStorageReady(Environment.MEDIA_MOUNTED.equals(state));
        Log.d(TAG, "updateDefaultDirectory() old=" + old + ", sMountPoint=" + sMountPoint
                + " return " + diff);
        return diff;
    }

    public static String getFileDirectory() {
        String path = sMountPoint + FOLDER_PATH;
        return path;
    }

    public static String getCameraScreenNailPath() {

        String path = sMountPoint + FOLDER_PATH;

        final String prefix = "/local/all/";
        String cameraPath = null;
        cameraPath = prefix + getBucketId(getFileDirectory());
        Log.d(TAG, "getCameraScreenNailPath() " + ", return " + cameraPath);
        return cameraPath;
    }

    private static String getBucketId(String directory) {
        return String.valueOf(directory.toLowerCase(Locale.ENGLISH).hashCode());
    }

    public static String getBucketId() {
        return getBucketId(getFileDirectory());
    }

    public static String generateFilepath(String fileName) {
        // if filename have the folder name ,so need first create the folder
        int lastIndex = fileName.lastIndexOf("/");
        if (lastIndex != -1) {
            mkFileDir(getFileDirectory() + "/" + fileName.substring(0, lastIndex));
        }
        return getFileDirectory() + '/' + fileName;
    }

    private static final AtomicLong LEFT_SPACE = new AtomicLong(0);

    public static long getLeftSpace() {
        long left = LEFT_SPACE.get();
        Log.d(TAG, "getLeftSpace() return " + left);
        return LEFT_SPACE.get();
    }

    public static void setLeftSpace(long left) {
        LEFT_SPACE.set(left);
        Log.d(TAG, "setLeftSpace(" + left + ")");
    }
}
