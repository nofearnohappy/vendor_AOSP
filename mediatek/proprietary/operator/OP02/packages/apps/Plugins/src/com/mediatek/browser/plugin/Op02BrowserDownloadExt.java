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
import com.mediatek.op02.plugin.R;

import java.io.File;

@PluginImpl(interfaceName="com.mediatek.browser.ext.IBrowserDownloadExt")
public class Op02BrowserDownloadExt extends DefaultBrowserDownloadExt {

    private static final String TAG = "BrowserPluginEx";

    private Context mContext;

    public Op02BrowserDownloadExt(Context context) {
        super();
        mContext = context;
    }

    public void setRequestDestinationDir(String downloadPath, Request mRequest, String filename, String mimeType) {
        Log.i("@M_" + TAG, "Enter: " + "setRequestDestinationDir" + " --OP02 implement");
        String op02Folder = getStorageDirectoryForOperator(mimeType);

        String path = "file://" + downloadPath.substring(0, downloadPath.lastIndexOf("/")) + File.separator
                                        + op02Folder + File.separator + filename;
        Uri pathUri = Uri.parse(path);
        mRequest.setDestinationUri(pathUri);
        Log.i("@M_" + TAG, "For OP02: selected download full path is: " +
                path + " MimeType is: " + mimeType + " and Uri is: " + pathUri);
    }

    public String getStorageDirectoryForOperator(String mimeType) {

        Log.i("@M_" + TAG, "Enter: " + "getStorageDirectoryForOperator" + " --OP02 implement");

        // if mimeType is null, return the default download folder.
        if (mimeType == null) {
            return Environment.DIRECTORY_DOWNLOADS;
        }

        // This is for OP02
        int fileType = MediaFile.getFileTypeForMimeType(mimeType);
        String selectionStr = null;

        if (mimeType.startsWith("audio/") || MediaFile.isAudioFileType(fileType)) {
            selectionStr = "Music";

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

        Log.d("@M_" + TAG, "mimeType is: " + mimeType
                 + "MediaFileType is: " + fileType +
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
        Log.d("@M_" + TAG, "Enter: " + "shouldShowToastWithFileSize" + " --OP02 implement");
        if (contentLength > 0) {
            Toast.makeText(activity, mContext.getResources().getString(R.string.download_pending_with_file_size)
                + Formatter.formatFileSize(activity, contentLength), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(activity, mContext.getResources().getString(R.string.download_pending), Toast.LENGTH_SHORT).show();
        }
    }

}
