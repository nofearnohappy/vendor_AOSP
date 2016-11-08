package com.mediatek.datatransfer.utils;

import android.content.Context;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.util.Log;

import com.mediatek.datatransfer.R;
import com.mediatek.datatransfer.utils.Constants.LogTag;
import com.mediatek.datatransfer.utils.Constants.ModulePath;
import com.mediatek.storage.StorageManagerEx;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author
 *
 */
public class SDCardUtils {

    public final static int MINIMUM_SIZE = 512;
    public final static String CLASS_TAG = "SDCardUtils";

    /**
     * @return String.
     */
    public static String getBackupStoragePath() {
        String storagePath = null;
        StorageManager storageManager = null;

        /*
        try {
            storageManager = new StorageManager(Looper.getMainLooper());
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
        */

        storagePath = StorageManagerEx.getExternalStoragePath();
        if (storagePath == null || storagePath.isEmpty()) {
            storagePath = StorageManagerEx.getInternalStoragePath();
            MyLogger.logE(CLASS_TAG, "Can't find External Storage use Internal Storage instead.");
        }
        if (!Environment.MEDIA_MOUNTED.equals(storageManager.getVolumeState(storagePath))) {
            MyLogger.logE(CLASS_TAG, storagePath + "  MEDIA_UNMOUNTED!!!");
            storagePath = StorageManagerEx.getInternalStoragePath();
            MyLogger.logE(CLASS_TAG, storagePath + "  getInternalStoragePath it's state is "
                    + storageManager.getVolumeState(storagePath));
            if (!Environment.MEDIA_MOUNTED.equals(storageManager.getVolumeState(storagePath))) {
                return "";
            }
        }
        return storagePath;
    }

    /**
     * @return String.
     */
    public static String getInternalStoragePath() {
        String storagePath = null;
        StorageManager storageManager = null;
        /*
        try {
            storageManager = new StorageManager(Looper.getMainLooper());
        } catch (RemoteException e) {
            e.printStackTrace();
            return "";
        }
        */

        storagePath = StorageManagerEx.getInternalStoragePath();
        if (storagePath != null && !storagePath.isEmpty()
                && Environment.MEDIA_MOUNTED.equals(storageManager.getVolumeState(storagePath))) {
            return storagePath;
        }
        return storagePath == null ? "" : storagePath;
    }


    /**
     * @return true SD card  Available
     * @deprecated use {@link #getBackupStoragePath} instead
     */
    @Deprecated
    public static String getExternalStoragePath() {
        String storagePath = null;
        StorageManager storageManager = null;
        /*
        try {
            storageManager = new StorageManager(Looper.getMainLooper());
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
        */

        storagePath = StorageManagerEx.getExternalStoragePath();
        if (storagePath == null || storagePath.isEmpty()) {
            MyLogger.logE("SDCardUtils", "storagePath is null");
            return null;
        }
        if (!Environment.MEDIA_MOUNTED.equals(storageManager.getVolumeState(storagePath))) {
            return null;
        }
        return storagePath;
    }

    /**
     * @param context
     * @return String.
     */
    public static String getSDStatueMessage(Context context) {
        String message = context.getString(R.string.nosdcard_notice);
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_SHARED) ||
            status.equals(Environment.MEDIA_UNMOUNTED)) {
            message = context.getString(R.string.sdcard_busy_message);
        }
        return message;
    }

    /**
     * @return String
     */
    public static String getStoragePath() {
//      This method has been out-time
//      String storagePath = getExternalStoragePath();
        String storagePath = getBackupStoragePath();
        if (storagePath == null || storagePath.trim().equals("")) {
            return null;
        }
        MyLogger.logD(CLASS_TAG, "getExternalStoragePath: path is " + storagePath);
        storagePath = makePersonalDataPath(storagePath);
        MyLogger.logD(CLASS_TAG, "getStoragePath: path is " + storagePath);
        return checkFile(storagePath);
    }

    /**
     * @return List<String>
     */
    public static List<String> getRestoreStoragePath() {
        List<String> result = new ArrayList<String>();
//      This method has been out-time
//      String storagePath = getExternalStoragePath();
        String storagePath = getBackupStoragePath();
        String internalPath = getInternalStoragePath();
        if (!storagePath.startsWith(internalPath)) {
            if (null != checkFile(makePersonalDataPath(internalPath))) {
                result.add(checkFile(makePersonalDataPath(internalPath)));
            }
        }
        MyLogger.logD(CLASS_TAG, "getExternalStoragePath: path is " + storagePath);
        storagePath = makePersonalDataPath(storagePath);
        MyLogger.logD(CLASS_TAG, "getStoragePath: path is " + storagePath);
        String tempPath = checkFile(storagePath);
        if (tempPath != null) {
            result.add(tempPath);
        }
        return result;
    }

    private static String makePersonalDataPath(String storagePath) {
        return storagePath + File.separator + "ct_backup" + File.separator + "backup_all"
                + File.separator;
    }

    private static String checkFile(String storagePath) {
        File file = new File(storagePath);
        if (file != null) {

            if (file.exists() && file.isDirectory()) {
                File temp = new File(storagePath + File.separator
                        + ".BackupRestoretemp");
                boolean ret;
                if (temp.exists()) {
                    ret = temp.delete();
                } else {
                    try {
                        ret = temp.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(LogTag.LOG_TAG,
                                "getStoragePath: " + e.getMessage());
                        ret = false;
                    } finally {
                        temp.delete();
                    }
                }
                if (ret) {
                    return storagePath;
                } else {
                    return null;
                }

            } else if (file.mkdirs()) {
                return storagePath;
            } else {
                MyLogger.logE(CLASS_TAG, "file.mkdir(): FAILED!");
            }
        } else {
            MyLogger.logE(LogTag.LOG_TAG,
                "getStoragePath: path is not ok");
            return null;
        }
        return null;
    }
    /**
     * @return true SD card  Available
     * @deprecated use {@link #getBackupStoragePath} instead
     */
    @Deprecated
    public static String getPersonalDataBackupPath() {
        String path = getStoragePath();
        if (path != null) {
            return path + File.separator;
        }

        return path;
    }

    /**
     * @return String
     */
    public static String getAppsBackupPath() {
        String path = getStoragePath();
        if (path != null) {
            return path + ModulePath.FOLDER_APP;
        }
        return path;
    }
    /**
     * @return true SD card  Available
     * @deprecated use {@link #isStorageAvailable} instead
     */
    @Deprecated
    public static boolean isSdCardAvailable() {
        return (getStoragePath() != null);
    }
    /**
     * @return boolean
     */
    public static boolean isStorageAvailable() {
        return (getStoragePath() != null);
    }

    /**
     * @param file
     * @return long
     */
    public static long getAvailableSize(String file) {
        android.os.StatFs stat = new android.os.StatFs(file);
        long count = stat.getAvailableBlocks();
        long size = stat.getBlockSize();
        long totalSize = count * size;
        Log.v(LogTag.LOG_TAG, "file remain size = " + totalSize);
        return totalSize;
    }
}
