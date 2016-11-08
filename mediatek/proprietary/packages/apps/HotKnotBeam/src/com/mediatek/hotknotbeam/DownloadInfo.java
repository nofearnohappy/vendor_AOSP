package com.mediatek.hotknotbeam;


import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mediatek.hotknotbeam.HotKnotFileServer.CommunicationThread;

/**
 * Stores information about an individual download.
 */
public class DownloadInfo extends ItemInfo {
    private final static String TAG = HotKnotBeamService.TAG;

    public final static String EXTRA_ITEM_ID = "DownloadId";

    private String  mUriPath;
    private String  mRootPath;
    private String  mSavePath;
    private String mDeviceName;
    private CommunicationThread mClientThread;
    private Map<String, List<String>> mFileParams;

    public DownloadInfo(String rootPath, String fileName, int fileSize, int groupId, String deviceName, CommunicationThread clientThread, Context context) {
        super(fileName, groupId, fileSize, context);
        mRootPath = rootPath;
        mClientThread = clientThread;
        mDeviceName = deviceName;
        mSavePath = "";
        mUriPath = "";
    }

    public void setExtInfo(String extInfo) {
        String decodeExtInfo = "";

        if (extInfo == null) {
            throw new NullPointerException("extInfo");
        }

        try {
            decodeExtInfo = Uri.decode(extInfo);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "extInfo:" + extInfo);
            e.printStackTrace();
            return;
        }

        mFileParams = new HashMap<String, List<String>>();
        mFileParams = getFileParameters(decodeExtInfo);

        String groupId = getFileParameter(HotKnotBeamConstants.QUERY_GROUPID);

        Log.i(TAG, "setExtInfo:" + extInfo);

        if (groupId != null) {
            try {
                mGroupId = Integer.parseInt(groupId);
                String tmp = getFileParameter(HotKnotBeamConstants.QUERY_ORDER);

                if (tmp != null) {
                    mOrder = Integer.parseInt(tmp) + 1;
                }

                tmp = getFileParameter(HotKnotBeamConstants.QUERY_NUM);

                if (tmp != null) {
                    mCount = Integer.parseInt(tmp);
                }

                String totalSize = getFileParameter(HotKnotBeamConstants.QUERY_TOTAL_SIZE);
                //ToDo
            } catch (NumberFormatException ee) {
                ee.printStackTrace();
            }

            Log.i(TAG, "GroupInfo:" + mGroupId + ":" + mOrder + ":" + mCount);
        }
    }

    public void setCommunicationThread(CommunicationThread clientThread) {
        mClientThread = clientThread;
    }

    public CommunicationThread getClientThread() {
        return mClientThread;
    }

    public String getTag() {
        if (mGroupId == HotKnotBeamConstants.NON_GROUP_ID) {
            return mId + ":" + mTotalBytes;
        } else {
            return String.valueOf(mGroupId);
        }
    }

    public Uri getUri() {
        Uri uri = null;

        try {
            //Save the first URI path
            if (mSavePath.charAt(mSavePath.length() - 1) == '/') {
                mUriPath = mSavePath +  Uri.encode(mFileName);
            } else {
                mUriPath = mSavePath +  "/" + Uri.encode(mFileName);
            }

            uri = Uri.parse("file://" + mUriPath);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "[Failed]uri parse:" + e.getMessage());
        }

        Log.d(TAG, "getUri:" + uri);
        return uri;
    }

    public String getMimeType() {
        String mimeType = getFileParameter(HotKnotBeamConstants.QUERY_MIMETYPE);

        if (mimeType != null) {
            return mimeType;
        }

        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        String extension = MimeTypeMap.getFileExtensionFromUrl(mFileName).toLowerCase();

        if (TextUtils.isEmpty(extension)) {
            int dotPos = mFileName.lastIndexOf('.');

            if (0 <= dotPos) {
                extension = mFileName.substring(dotPos + 1);
                extension = extension.toLowerCase();
            }
        }

        mimeType = mimeTypeMap.getMimeTypeFromExtension(extension);

        if (mimeType == null) {
            //Check self-defined
            mimeType = MimeUtilsEx.guessMimeTypeFromExtension(extension);

            if (mimeType == null) {
                Log.e(TAG, "No corresponding mime type");
            }
        }

        return mimeType;
    }

    public String toString() {
        StringBuilder str = new StringBuilder("TAG:");

        str.append(mId + " " + getTag() + " " + mState + " " + mCurrentBytes + "/" + mTotalBytes + "/" + mGroupId + "/" + mIsSucceed + "/" + mOrder + "/" + mCount);

        return str.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DownloadInfo)) {
            return false;
        }

        DownloadInfo info = (DownloadInfo) obj;

        return mId == info.mId;
    }

    @Override
    public int hashCode() {
        return ((mId + 41) * 41);
    }

    public void setSaveFolder(String saveFolder) {
        mSavePath = mRootPath + "/" + saveFolder;
    }

    public String getSaveFolder() {
        String folderPath = getFileParameter(HotKnotBeamConstants.QUERY_FOLDER);

        if (mSavePath.length() != 0) { //Use query string firstly
            return mSavePath;
        }

        //Check the first query string, if support, use it
        if (folderPath != null && folderPath.length() > 0) {
            if (folderPath.charAt(0) != '/') {
                mSavePath = mRootPath + "/" + folderPath;
            } else {
                mSavePath = mRootPath + folderPath;
            }
        } else { //No folder path information, use root path
            mSavePath = mRootPath;
        }

        return mSavePath;
    }

    public boolean isCompressed() {
        return checkParameterValue(HotKnotBeamConstants.QUERY_ZIP, HotKnotBeamConstants.QUERY_VALUE_YES, true);
    }

    public String getAppIntent() {
        return getFileParameter(HotKnotBeamConstants.QUERY_INTENT);
    }

    public boolean isMimeTypeCheck() {
        return checkParameterValue(HotKnotBeamConstants.QUERY_IS_MIMETYPE, HotKnotBeamConstants.QUERY_VALUE_NO, false);
    }

    public boolean isShowNotification() {
        return checkParameterValue(HotKnotBeamConstants.QUERY_SHOW, HotKnotBeamConstants.QUERY_VALUE_NO, false);
    }

    public boolean isShowUiApp() {
        if (isGroup()) {
            return true;
        }

        long displaySize = SystemProperties.getLong("sys.hotknot.show.size", HotKnotBeamConstants.MAX_FILE_DISPLAY_SIZE);

        if (getTotalBytes() > displaySize) {
            return true;
        }

        return false;
    }

    public boolean isRenameFile() {
        return checkParameterValue(HotKnotBeamConstants.QUERY_FORMAT, "raw", false);
    }

    private boolean checkParameterValue(String keyObject, String keyValue, boolean expectedValue) {
        boolean isMatch = !expectedValue;

        String obj = getFileParameter(keyObject);

        if (obj != null) {
            obj = obj.toLowerCase();

            if (obj.equalsIgnoreCase(keyValue)) {
                isMatch = expectedValue;
            }
        }

        Log.d(TAG, "key:" + keyObject + ":" + isMatch + ":" + obj);
        return isMatch;
    }

    public String getFileParameter(String keyObject) {
        if (keyObject == null) {
            throw new NullPointerException("key");
        }

        if (mFileParams == null) {
            return null;
        }

        List<String> values = mFileParams.get(keyObject);

        if (values == null) {
            return null;
        }

        return (String) values.get(0);
    }

    private static Map<String, List<String>> getFileParameters(String line) {
        Map<String, List<String>> params = new HashMap<String, List<String>>();

        for (String param : line.split("&")) {
            String pair[] = param.split("=");
            String key = pair[0];
            String value = "";

            if (pair.length > 1) {
                value = pair[1];
            }

            List<String> values = params.get(key);

            if (values == null) {
                values = new ArrayList<String>();
                params.put(key, values);
            }

            values.add(value);
        }

        return params;
    }

    public String getFailureTitle() {
        final Resources res = mContext.getResources();
        String title = "";

        if (isGroup()) {
            if (getDoneItem() == 0) {
                title = res.getString(R.string.notification_group_received_failed, mCount);
            } else {
                title = res.getString(R.string.notification_group_received_partial_failed, (mCount - mDoneItem), mDoneItem);
            }
        } else {
            title = getTitle();
        }

        return title;
    }

    public String getFailureText() {
        final Resources res = mContext.getResources();
        String detailReason = "";

        switch (mFailReason) {
        case LOW_STORAGE:
            detailReason = res.getString(R.string.notification_download_failure, res.getText(R.string.low_storage));
            break;
        case CONNECTION_ISSUE:
            detailReason = res.getString(R.string.notification_download_failure, res.getText(R.string.connect_failure));
            break;
        case USER_CANCEL_RX:
            detailReason = res.getString(R.string.notification_download_cancel);
            break;
        default:
            detailReason = res.getString(R.string.notification_download_failure, res.getText(R.string.unknown_error));
            break;
        }

        Log.i(TAG, "getFailureReason:" + detailReason);

        return detailReason;
    }

    public String getDeviceName() {
        return mDeviceName;
    }
}
