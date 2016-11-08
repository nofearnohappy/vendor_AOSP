package com.mediatek.rcs.message.cloudbackup.modules;

import java.io.File;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.mediatek.rcs.message.cloudbackup.modules.Chat1To1RestoreParser;
import com.mediatek.rcs.message.cloudbackup.utils.FileUtils;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.FavoriteRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.MessageRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.RcsMsgRecord;

class FavChat1To1Decomposer {
    private static final String CLASS_TAG = CloudBrUtils.MODULE_TAG + "FavChat1To1Decomposer";
    private ContentResolver mContentResolver;
    private Chat1To1RestoreParser mChat1To1RestoreParser;
    private MessageRecord mMessageRecord;
    private RcsMsgRecord mRcsMsgRecord;
    private Context mContext;
    private boolean mCancel = false;

    FavChat1To1Decomposer(Context context) {
        mContext = context;
        mContentResolver = mContext.getContentResolver();
        mChat1To1RestoreParser = new Chat1To1RestoreParser();
    }

    protected int parseOneToOneMsg(File file) {
        Log.d(CLASS_TAG, "parseOneToOneMsg begin, filename = " + file.getName());
        initParse();
        int result = mChat1To1RestoreParser.parseOneToOneMsg(file);
        if (result != CloudBrUtils.ResultCode.OK) {
            Log.e(CLASS_TAG, "parseOneToOneMsg result is not ok, return");
            return result;
        }
        Log.d(CLASS_TAG, "parseOneToOneMsg end, result = ok");
        FavoriteRecord favoriteRecord = restructureRecord();
        Uri uri = FileUtils.insertFavoriteDb(mContentResolver, favoriteRecord);
        if (uri == null) {
            Log.e(CLASS_TAG, "insert favorite db error, return false");
            return CloudBrUtils.ResultCode.INSERTDB_EXCEPTION;
        }
        Log.d(CLASS_TAG, "insertDataBase end, result = ok");
        result = CloudBrUtils.ResultCode.OK;

        if (mCancel) {
            Log.d(CLASS_TAG, "parseOneToOneMsg service canceled");
            return CloudBrUtils.ResultCode.SERVICE_CANCELED;
        }
        return result;
    }

    /**
     * This method will be called when backup service be cancel.
     * @param cancel
     */
    protected void setCancel(boolean cancel) {
        mCancel = cancel;
    }

    private void initParse() {
        mMessageRecord = null;
        mRcsMsgRecord = null;
        mRcsMsgRecord = new RcsMsgRecord();
        mMessageRecord = new MessageRecord();
        mChat1To1RestoreParser.setMessageRecord(mMessageRecord);
        mChat1To1RestoreParser.setRcsMsgRecord(mRcsMsgRecord);
    }

    protected int persistMessageBody(String chatId, String msgContent) {
        if (mCancel) {
            Log.d(CLASS_TAG, "persistMessageBody service canceled");
            return CloudBrUtils.ResultCode.SERVICE_CANCELED;
        }

        Log.d(CLASS_TAG, "persistMessageBody begin, msgContent = " + msgContent);
        initParse();

        int result = mChat1To1RestoreParser.persistMessageBody(chatId, 0, msgContent);
        if (result != CloudBrUtils.ResultCode.OK) {
            Log.e(CLASS_TAG, "persistMessageBody result is not ok, return");
            return result;
        }
        Log.d(CLASS_TAG, "persistMessageBody end, result = ok");

        FavoriteRecord favoriteRecord = restructureRecord();
        Uri uri = FileUtils.insertFavoriteDb(mContentResolver, favoriteRecord);
        if (uri == null) {
            Log.e(CLASS_TAG, "insert favorite db error, return false");
            return CloudBrUtils.ResultCode.INSERTDB_EXCEPTION;
        }
        Log.d(CLASS_TAG, "insertDataBase end, result = ok");
        return CloudBrUtils.ResultCode.OK;
    }

    private FavoriteRecord restructureRecord() {
        FavoriteRecord favoriteRecord = new FavoriteRecord();
        favoriteRecord.setDate(mRcsMsgRecord.getTimestamp());
        favoriteRecord.setChatId(mRcsMsgRecord.getChatId());
        favoriteRecord.setDataSent(mRcsMsgRecord.getDataSent());
        favoriteRecord.setDate(mRcsMsgRecord.getTimestamp());
        favoriteRecord.setDirection(mRcsMsgRecord.getDirection());
        favoriteRecord.setFlag(mRcsMsgRecord.getFlag());
        favoriteRecord.setStatus(mRcsMsgRecord.getStatus());
        favoriteRecord.setType(mRcsMsgRecord.getType());
        favoriteRecord.setMsgId(mRcsMsgRecord.getIpmsgId());

        String from = mRcsMsgRecord.getFrom();
        if (from.equals(CloudBrUtils.getMyNumber())) {
            from = null;
        }
        favoriteRecord.setContactNum(from);
        String body = new String(mRcsMsgRecord.getBody());
        favoriteRecord.setBody(body);
        favoriteRecord.setMimeType(mRcsMsgRecord.getMimeType());

        // int type = mMessageRecord.getType();
        // String strType = null;
        // if (type == Message.Type.VEMOTION) {
        // strType = CloudBrUtils.ContentType.VEMOTION_TYPE;
        // } else if (type == Message.Type.CLOUDFILE) {
        // strType = CloudBrUtils.ContentType.CLOUDFILE_TYPE;
        // } else {
        // strType = CloudBrUtils.ContentType.TEXT_TYPE;
        // }
        // favoriteRecord.setCt(strType);
        return favoriteRecord;
    }

}
