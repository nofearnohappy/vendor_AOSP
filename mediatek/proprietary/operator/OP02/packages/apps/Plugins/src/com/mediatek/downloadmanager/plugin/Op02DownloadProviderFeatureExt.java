package com.mediatek.downloadmanager.plugin;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.media.MediaFile;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.mediatek.downloadmanager.ext.DefaultDownloadProviderFeatureExt;
import com.mediatek.common.PluginImpl;

@PluginImpl(interfaceName="com.mediatek.downloadmanager.ext.IDownloadProviderFeatureExt")
public class Op02DownloadProviderFeatureExt extends DefaultDownloadProviderFeatureExt {

    private static final String TAG = "DownloadProviderPluginEx";

    public static final String SHOW_DIALOG_REASON = "ShowDialogReason"; //DownloadInfo
    public static final String FULL_FILE_NAME = "FullFileName";

    public static final int NOT_CONTINUE_DOWNLOAD = 1;
    public static final int CONTINUE_DOWNLOAD = 2;

    public Op02DownloadProviderFeatureExt(Context context) {
        super(context);
    }

    /**
     * Get the dialog reason value from received intent.
     *
     * @param intent The intent received.
     * @return Dialog reason value.
     */
    @Override
    public int getShowDialogReasonInt(Intent intent) {
        Log.i("@M_" + TAG, "Enter: " + "getShowDialogReasonInt" + " --OP02 implement");
        return intent.getExtras().getInt(SHOW_DIALOG_REASON);
    }

    /**
     * Save continue download to the downloadInfo.
     *
     * @param contiDownload The continue download element in downloadInfo.
     * @param value The value that query from db. if ture, it means user choose
     *        continue download when file already exists.
     */
    @Override
    public void setContinueDownload(boolean contiDownload, boolean value) {
        Log.i("@M_" + TAG, "Enter: " + "setContinueDownload" + " --OP02 implement");
        contiDownload = value;
    }

    /**
     * Send notify intent when download file already exsits in storage.
     *
     * @param uri The download item uri.
     * @param packageName Package name of app that notify intent send to.
     * @param className Class name of app that notify intent send to.
     * @param fullFileName The full file name which notify.
     * @param context Context that send intent.
     */
    @Override
    public void notifyFileAlreadyExistIntent(Uri uri, String packageName, String className,
            String fullFileName, Context context) {
        Log.i("@M_" + TAG, "Enter: " + "notifyFileAlreadyExistIntent" + " --OP02 implement");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);
        intent.setClassName(packageName, className);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        intent.putExtra(SHOW_DIALOG_REASON, 1);
        intent.putExtra(FULL_FILE_NAME, fullFileName);
        context.startActivity(intent);
    }

    /**
     * Get the default download dir according to mimetype.
     *
     * @param mimeType The mimetype of donwload file.
     * @return Directory string
     */
    @Override
    public String getStorageDirectory(String mimeType) {
        Log.i("@M_" + TAG, "Enter: " + "getStorageDirectory" + " --OP02 implement");

        int fileType = MediaFile.getFileTypeForMimeType(mimeType);
        String selectionStr = null;

        if (mimeType.startsWith("audio/") || MediaFile.isAudioFileType(fileType)) {
            //base = new File(root.getPath() + Constants.OP02_CUSTOMIZATION_AUDIO_DL_SUBDIR);
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

        Log.i("@M_" + TAG, "mimeType is: " + mimeType
                + "MediaFileType is: " + fileType +
                "folder is: " + selectionStr);
        return selectionStr;
    }

    /**
     * process file already exists condition.
     *
     * @return result
     */
    public int processFileExistCondition(boolean continueDownload) {
        Log.i("@M_" + TAG, "Enter: " + "processFileExistCondition" + " --OP02 implement");
        if (!continueDownload) {
            // Add to support CU customization to deal with same file name
            return NOT_CONTINUE_DOWNLOAD;
        } else {
            // User click "OK" to ensure download with same filename.
            return CONTINUE_DOWNLOAD;
        }
    }

    /**
     * The fuction will show file already exist dialog for end user.
     * if user choose "ok", it will download continue, if choose "cancel",
     * it will cancel this download.
     *
     * @param builder AlertDialog builder
     * @param appLable Dialog lable
     * @param message Dialog message
     * @param positiveButtonString Positive button string
     * @param negativeButtonString Negative button string
     * @param listener The click listener registered
     */
    public void showFileAlreadyExistDialog(AlertDialog.Builder builder, CharSequence appLable,
            CharSequence message, String positiveButtonString, String negativeButtonString, OnClickListener listener) {
        Log.i("@M_" + TAG, "Enter: " + "showFileAlreadyExistDialog" + " --OP02 implement");

            builder.setTitle(appLable)
                    .setMessage(message)
                    .setPositiveButton(positiveButtonString, listener)
                    .setNegativeButton(negativeButtonString, listener);
    }
}
