package com.mediatek.browser.plugin;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaFile;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import com.mediatek.browser.ext.DefaultBrowserDownloadExt;
import com.mediatek.common.PluginImpl;
import com.mediatek.op09.plugin.R;
import com.mediatek.storage.StorageManagerEx;

@PluginImpl(interfaceName="com.mediatek.browser.ext.IBrowserDownloadExt")
public class Op09BrowserDownloadExt extends DefaultBrowserDownloadExt {

    private static final String TAG = "Op09BrowserDownloadExt";

    private static final String DEFAULT_DOWNLOAD_DIRECTORY_OP09 = StorageManagerEx.getDefaultPath()
            + "/Download";

    private static final String EXTERNAL_STORAGE_DIR = "/storage/emulated/0";

    private static final long LowSpaceThreshold = 10 * 1024 * 1024;

    private Context mContext;

    public Op09BrowserDownloadExt(Context context) {
        super();
        mContext = context;
    }

    public void setRequestDestinationDir(String downloadPath, Request mRequest,
                    String filename, String mimeType) {
        Log.i("@M_" + TAG, "Enter: " + "setRequestDestinationDir" + " --OP09 implement");

        String path = null;
        if (downloadPath.equalsIgnoreCase(DEFAULT_DOWNLOAD_DIRECTORY_OP09)) {
            String folder = getStorageDirectoryForOperator(mimeType);
            path = "file://" + downloadPath + "/" + folder + "/" + filename;
        } else {
            path = "file://" + downloadPath + "/" + filename;
        }

        Uri downloadUri = Uri.parse(path);
        Log.i("@M_" + TAG, "For OP09: selected download full path is: " + path + " MimeType is: "
                + mimeType + " and Uri is: " + downloadUri);
        mRequest.setDestinationUri(downloadUri);
    }

    public String getStorageDirectoryForOperator(String mimeType) {

        Log.i("@M_" + TAG, "Enter: " + "getStorageDirectoryForOperator" + " --OP09 implement");

        // if mimeType is null, do not set sub dir.
        if (mimeType == null) {
            return "Others";
        }

        int fileType = MediaFile.getFileTypeForMimeType(mimeType);
        String selectionStr = null;

        if (mimeType.startsWith("audio/") || MediaFile.isAudioFileType(fileType)) {
            selectionStr = "Music";
        } else if (mimeType.startsWith("image/") || MediaFile.isImageFileType(fileType)) {
            selectionStr = "Picture";
        } else if (mimeType.startsWith("video/") || MediaFile.isVideoFileType(fileType)) {
            selectionStr = "Video";
        } else if (mimeType.startsWith("text/") || mimeType.equalsIgnoreCase("application/msword")
                || mimeType.equalsIgnoreCase("application/vnd.ms-powerpoint")
                || mimeType.equalsIgnoreCase("application/pdf")) {
            selectionStr = "Document";
        } else if (mimeType.equalsIgnoreCase("application/vnd.android.package-archive")) {
            selectionStr = "Application";
        } else {
            selectionStr = "Others";
        }

        Log.d("@M_" + TAG, "mimeType is: " + mimeType + ",MediaFileType is: " +
            fileType + ",folder is: " + selectionStr);

        return selectionStr;
    }

    public boolean checkStorageBeforeDownload(Activity activity, String downloadPath, long contentLength) {
        Log.i("@M_" + TAG, "Enter: "  + "checkStorageBeforeDownload" + " --OP09 implement");
        if (contentLength <= 0) {
            return false;
        }

        Log.i("@M_" + TAG, "before checkIfHaveAvailableStoreage(),contentLength: " + contentLength);
        return checkIfHaveAvailableStoreage(downloadPath, activity, contentLength);
    }

    private boolean checkIfHaveAvailableStoreage(String path, Activity activity, long contentLength) {
        String downloadPath = Uri.parse(path).getPath();
        if (downloadPath != null) {
            String storagePath = EXTERNAL_STORAGE_DIR;
            if (!downloadPath.startsWith(EXTERNAL_STORAGE_DIR)) {
                String[] str = downloadPath.split("/");
                if (str == null || str.length < 3) {
                    Log.e("@M_" + TAG, "downloadPath is invalid, path=" + downloadPath);
                    return false;
                }
                storagePath = str[0] + "/" + str[1] + "/" + str[2];
            }
            Log.i("@M_" + TAG, "downloadPath=" + downloadPath + ", storagePath=" + storagePath);

            if (availableStorage(storagePath, activity) < contentLength) {
                if (StorageManagerEx.isExternalSDCard(storagePath)) {
                    Log.i("@M_" + TAG, "extra storage is download path, " +
                        "can to download because of low storeage " +
                        "and will popup low storeage dialog");

                    new AlertDialog.Builder(activity)
                        .setTitle(mContext.getResources().getString(
                            R.string.low_storage_dialog_title_on_extra))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(mContext.getResources().getString(
                            R.string.low_storage_dialog_msg_on_extra))
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                } else {
                    Log.i("@M_" + TAG, "external storage is download path, " +
                        "can to download because of low storeage " +
                        "and will popup low storeage dialog");

                    new AlertDialog.Builder(activity)
                        .setTitle(mContext.getResources().getString(
                            R.string.low_storage_dialog_title_on_external))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(mContext.getResources().getString(
                            R.string.low_storage_dialog_msg_on_external))
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                }
                return true;
            }
        }

        return false;
    }

    private long availableStorage(String path, Activity activity) {
        long availableStorage = getAvailableBytesInFileSystemAtGivenRoot(path) - getDownloadsInProgressWillOccupyBytes(activity) - LowSpaceThreshold;
        Log.i("@M_" + TAG, "check storage before download, availableStorage : " + availableStorage + ", about" + availableStorage / (1 * 1024 * 1024) + "M");
        return availableStorage;
    }

    private long getAvailableBytesInFileSystemAtGivenRoot(String path) {
        StatFs stat = new StatFs(path);
        long availableBlocks = (long) stat.getAvailableBlocks();
        long size = stat.getBlockSize() * availableBlocks;
        return size;
    }

    private long getDownloadsInProgressWillOccupyBytes(Activity activity) {
        long downloadsWillOccupyBytes = 0L;
        Cursor cursor = null;
        DownloadManager manager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        try {
           cursor = manager.query(new DownloadManager.Query().setFilterByStatus(DownloadManager.STATUS_RUNNING));
           if (cursor != null) {
               for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                   long downloadID = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID));
                   long totalBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                   long currentBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                   if (totalBytes > 0 && currentBytes > 0 && totalBytes - currentBytes > 0) {
                       downloadsWillOccupyBytes += totalBytes - currentBytes;
                       Log.i("@M_" + TAG, "Download id :" + downloadID + " in downloading, totalBytes: " + totalBytes + ",currentBytes: " + currentBytes);
                   }
               }
           }
        } catch (IllegalStateException e) {
            Log.i("@M_" + TAG, "getDownloadsInProgressWillOccupyBytes: query encounter exception");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        Log.i("@M_" + TAG, "getDownloadsInProgressWillOccupyBytes: return downloadsWillOccupyBytes:" + downloadsWillOccupyBytes);
        return downloadsWillOccupyBytes;
    }

}
