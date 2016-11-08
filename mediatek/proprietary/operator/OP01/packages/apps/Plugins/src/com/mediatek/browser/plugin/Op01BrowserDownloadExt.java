package com.mediatek.browser.plugin;

import android.app.Activity;
import android.app.DownloadManager.Request;
import android.content.Context;
import android.media.MediaFile;
import android.net.Uri;
import android.os.Environment;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;

import com.mediatek.browser.ext.DefaultBrowserDownloadExt;
import com.mediatek.common.PluginImpl;
import com.mediatek.op01.plugin.R;
import com.mediatek.storage.StorageManagerEx;

@PluginImpl(interfaceName="com.mediatek.browser.ext.IBrowserDownloadExt")
public class Op01BrowserDownloadExt extends DefaultBrowserDownloadExt {

    private static final String TAG = "Op01BrowserDownloadExt";

    private static final String DEFAULT_DOWNLOAD_DIRECTORY_OP01 = "/storage/sdcard0/MyFavorite";
    private static final String DEFAULT_MY_FAVORITE_FOLDER = "/MyFavorite";

    private Context mContext;

    public Op01BrowserDownloadExt(Context context) {
        super();
        mContext = context;
    }

    public void setRequestDestinationDir(String downloadPath, Request mRequest,
                    String filename, String mimeType) {
        Log.i("@M_" + TAG, "Enter: " + "setRequestDestinationDir" + " --OP01 implement");

        String path = null;
        String defaultDownloadPath = DEFAULT_DOWNLOAD_DIRECTORY_OP01;
        String defaultStorage = StorageManagerEx.getDefaultPath();
        if (null != defaultStorage) {
            defaultDownloadPath = defaultStorage + DEFAULT_MY_FAVORITE_FOLDER;
        }
        if (downloadPath.equalsIgnoreCase(defaultDownloadPath)) {
            String folder = getStorageDirectoryForOperator(mimeType);
            path = "file://" + downloadPath + "/" + folder + "/" + filename;
        } else {
            path = "file://" + downloadPath + "/" + filename;
        }

        Uri downloadUri = Uri.parse(path);
        Log.i("@M_" + TAG, "device default storage is: " + defaultStorage +
            ", defaultPath is: " + defaultDownloadPath + ", full path is: " +
            path + ", MimeType is: " + mimeType + ", Uri is: " + downloadUri);
        mRequest.setDestinationUri(downloadUri);
    }

    public String getStorageDirectoryForOperator(String mimeType) {
        Log.i("@M_" + TAG, "Enter: " + "getStorageDirectoryForOperator" + " --OP01 implement");
        // if mimeType is null, return the default download folder.
        if (mimeType == null) {
            return Environment.DIRECTORY_DOWNLOADS;
        }

        // This is for OP02
        int fileType = MediaFile.getFileTypeForMimeType(mimeType);
        String selectionStr = null;

        if (mimeType.startsWith("audio/") || MediaFile.isAudioFileType(fileType)) {
            selectionStr = "Ringtone";
        } else if (mimeType.startsWith("image/") || MediaFile.isImageFileType(fileType)) {
            selectionStr = "Photo";
        } else if (mimeType.startsWith("video/") || MediaFile.isVideoFileType(fileType)) {
            selectionStr = "Video";
        } else if (mimeType.startsWith("text/") || mimeType.equalsIgnoreCase("application/msword")
                || mimeType.equalsIgnoreCase("application/vnd.ms-powerpoint")
                || mimeType.equalsIgnoreCase("application/pdf")) {
            selectionStr = "Document";
        } else {
            selectionStr = Environment.DIRECTORY_DOWNLOADS;
        }

        Log.d("@M_" + TAG, "mimeType is: " + mimeType + "MediaFileType is: " + fileType +
                "folder is: " + selectionStr);
        return selectionStr;
    }
    /**
     * Show the download toast with file size.
     * @param activity the activity
     * @param contentLength the download file content length
     * @param text toast information
     */
    public void showToastWithFileSize(Activity activity, long contentLength, String text) {
        Log.d("@M_" + TAG, "Enter: " + "showToastWithFileSize" + " --OP01 implement");
        if (contentLength > 0) {
            Toast.makeText(activity, mContext.getResources().getString(R.string.download_pending_with_file_size)
                + Formatter.formatFileSize(activity, contentLength), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(activity, mContext.getResources().getString(R.string.download_pending), Toast.LENGTH_SHORT).show();
        }
    }
}
