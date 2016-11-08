package com.mediatek.rcs.message.cloudbackup.modules;

import android.content.ContentResolver;
import android.net.Uri; //import android.provider.Telephony;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

import com.mediatek.rcs.common.RcsLog;
import com.mediatek.rcs.message.cloudbackup.modules.SmsRestoreParser;
import com.mediatek.rcs.message.cloudbackup.modules.SmsRestoreParser.SmsRestoreEntry;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.Favorite;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.ResultCode;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.FavoriteRecord;
import com.mediatek.rcs.message.cloudbackup.utils.FileUtils;

class FavSmsDecomposer {
    private static final String CLASS_TAG = CloudBrUtils.MODULE_TAG + "FavSmsDecomposer";
    private ArrayList<SmsRestoreEntry> mVmessageList;
    private SmsRestoreParser mSmsRestoreParser;
    private ContentResolver mContentResolver;
    private boolean mCancel = false;

    FavSmsDecomposer(ContentResolver contentResolver) {
        mContentResolver = contentResolver;
        mSmsRestoreParser = new SmsRestoreParser();
    }

    int retoreData(File file) {
        if (file == null || !file.exists()) {
            Log.d(CLASS_TAG, "backup file error. return");
            return CloudBrUtils.ResultCode.BACKUP_FILE_ERROR;
        }

        if (mSmsRestoreParser == null) {
            Log.e(CLASS_TAG, "retoreData mSmsRestoreParser = null, return");
            return CloudBrUtils.ResultCode.OTHER_EXCEPTION;
        }
        initRestore();
        int result = mSmsRestoreParser.parseVmsg(file);
        if (result != CloudBrUtils.ResultCode.OK) {
            Log.e(CLASS_TAG, "mSmsRestoreParser.parseVmsg result is not ok, return");
            return result;
        }

        result = implementComposeEntity();
        if (result != CloudBrUtils.ResultCode.OK) {
            Log.e(CLASS_TAG, "implementComposeEntity() result is not ok, return");
            return result;
        }
        onEnd();

        if (mCancel) {
            Log.d(CLASS_TAG, "retoreData() service canceled");
            return CloudBrUtils.ResultCode.SERVICE_CANCELED;
        }
        Log.d(CLASS_TAG, "restore sms success,return ok");
        return CloudBrUtils.ResultCode.OK;
    }

    private void initRestore() {
        mVmessageList = null;
        mVmessageList = new ArrayList<SmsRestoreEntry>();
        mSmsRestoreParser.setVmessageList(mVmessageList);
    }

    private int implementComposeEntity() {
        int result = CloudBrUtils.ResultCode.OK;
        for (int index = 0; index < mVmessageList.size(); index++) {

            if (mCancel) {
                Log.d(CLASS_TAG, "implementComposeEntity() service canceled");
                return CloudBrUtils.ResultCode.SERVICE_CANCELED;
            }

            SmsRestoreEntry vMsgFileEntry = mVmessageList.get(index);

            if (vMsgFileEntry != null) {
                FavoriteRecord favoriteRecord = getFavoreteInfo(vMsgFileEntry);
                Uri uri = FileUtils.insertFavoriteDb(mContentResolver, favoriteRecord);
                Log.d(CLASS_TAG, "insert favorite database uri = " + uri);
                if (uri == null) {
                    Log.e(CLASS_TAG, "insert favorite db error, return false");
                    return ResultCode.DB_EXCEPTION;
                }
            }
        }
        return result;
    }

    private FavoriteRecord getFavoreteInfo(SmsRestoreEntry smsRestoreEntry) {
        FavoriteRecord favoriteRecord = new FavoriteRecord();
        String from = smsRestoreEntry.getSmsAddress();
        if (from.equals(CloudBrUtils.getMyNumber())) {
            from = null;
            favoriteRecord.setDirection(1);
        }
        favoriteRecord.setContactNum(from);
        favoriteRecord.setBody(smsRestoreEntry.getBody());
        favoriteRecord.setDate(Long.parseLong(smsRestoreEntry.getTimeStamp()));
        favoriteRecord.setChatId(from);

        favoriteRecord.setFlag(RcsLog.ThreadFlag.OFFICIAL);
        favoriteRecord.setType(RcsLog.MessageType.SMSMMS);
        return favoriteRecord;
    }

    private boolean onEnd() {
        if (mVmessageList != null) {
            mVmessageList.clear();
        }

        Log.d(CLASS_TAG, "onEnd()");
        Log.d(CLASS_TAG, "smsRestore end:" + System.currentTimeMillis());
        return true;
    }

    /**
     * This method will be called when restore service be cancel.
     *
     * @param cancel
     */
    public void setCancel(boolean cancel) {
        mCancel = cancel;
    }
}
