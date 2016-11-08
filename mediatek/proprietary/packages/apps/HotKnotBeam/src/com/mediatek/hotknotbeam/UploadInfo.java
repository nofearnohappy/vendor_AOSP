package com.mediatek.hotknotbeam;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;



/**
 * Stores information about an individual upload.
 */
public class UploadInfo extends ItemInfo {
    private final static String TAG = HotKnotBeamService.TAG;

    private static FileUploadTask mFileUploadTask;

    public UploadInfo(String filename, int totalSize, int groupId, int order, int count, FileUploadTask fileUploadTask, Context context) {
        super(filename, groupId, totalSize, context);
        mOrder = order + 1;
        mCount = count;
        mFileUploadTask = fileUploadTask;

        if (order != 0) {
            throw new IllegalArgumentException();
        }
    }

    public boolean isCompressed() {
        return false;
    }

    public void setFileUploadTask(FileUploadTask uploadTask) {
        mFileUploadTask = uploadTask;
    }

    public String getFailureTitle() {
        final Resources res = mContext.getResources();
        String title = "";

        if (isGroup()) {
            if (getDoneItem() == 0) {
                title = res.getString(R.string.notification_group_sent_failed, mCount);
            } else {
                title = res.getString(R.string.notification_group_sent_partial_failed, (mCount - mDoneItem), mDoneItem);
            }
        } else {
            title = getTitle();
        }

        return title;
    }

    public String getTag() {
        if (mGroupId == HotKnotBeamConstants.NON_GROUP_ID) {
            return mId + ":" + mTotalBytes;
        } else {
            return String.valueOf(mGroupId) + "UL";
        }
    }



    public String getFailureText() {
        final Resources res = mContext.getResources();
        String detailReason = "";

        switch (mFailReason) {
        case CONNECTION_ISSUE:
            detailReason = res.getString(R.string.notification_upload_failed, res.getText(R.string.connect_failure));
            break;
        case USER_CANCEL_TX:
            detailReason = res.getString(R.string.notification_upload_cancel);
            break;
        default:
            detailReason = res.getString(R.string.notification_upload_failed, res.getText(R.string.unknown_error));
            break;
        }

        Log.i(TAG, "getFailureReason:" + detailReason);

        return detailReason;
    }
}