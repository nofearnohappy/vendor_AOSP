package com.mediatek.downloadmanager.plugin;

import android.app.Activity;
import android.content.Context;
import android.media.MediaFile;
import android.util.Log;

import java.io.File;

import com.mediatek.common.PluginImpl;
import com.mediatek.downloadmanager.ext.DefaultDownloadProviderFeatureExt;


@PluginImpl(interfaceName="com.mediatek.downloadmanager.ext.IDownloadProviderFeatureExt")
public class Op09DownloadProviderFeatureExt extends DefaultDownloadProviderFeatureExt {

    private static final String TAG = "DownloadProviderPluginEx";

    private static final String DEFAULT_DOWNLOAD_FOLDER_OP09 = "Download";

    public Op09DownloadProviderFeatureExt(Context context) {
        super(context);
    }

    /**
     * Get the default download dir according to mimetype.
     *
     * @param mimeType The mimetype of donwload file.
     * @return Directory string
     */
    @Override
    public String getStorageDirectory(String mimeType) {
        Log.i("@M_" + TAG, "Enter: " + "getStorageDirectory" + " --OP09 implement");

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

        selectionStr = DEFAULT_DOWNLOAD_FOLDER_OP09 + File.separator + selectionStr;

        Log.i("@M_" + TAG, "mimeType is: " + mimeType + "MediaFileType is: " + fileType +
                "folder is: " + selectionStr);
        return selectionStr;
    }
}
