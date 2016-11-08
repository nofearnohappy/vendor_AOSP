package com.mediatek.rcs.message.cloudbackup.modules;

import com.mediatek.rcs.message.cloudbackup.modules.ChatGroupParser;
import com.mediatek.rcs.message.cloudbackup.modules.ChatGroupParser.IGroupChatMsgParse;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.ChatRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.RootRecord;

import android.content.ContentResolver;
import android.content.Context;
import android.util.Log;

import java.io.File;

/**
 * This class is used to parse group msg file that get from service.
 */
class FavChatGroupDecomposer implements IGroupChatMsgParse {
    private static final String CLASS_TAG = CloudBrUtils.MODULE_TAG + "FavChatGroupDecomposer";
    private ContentResolver mContentResolver;
    private ChatGroupParser mChatGroupParser;
    private FavChat1To1Decomposer mFavChat1To1Decomposer;
    private ChatRecord mChatRecord;
    private RootRecord mRootRecord;
    private Context mContext;
    private boolean mCancel = false;
/**
 * Restore group chat file module.
 * @param contentResolver
 */
    FavChatGroupDecomposer(Context context) {
        mContext = context;
        mContentResolver = mContext.getContentResolver();
        mChatGroupParser = new ChatGroupParser(mContext, this);
        mFavChat1To1Decomposer = new FavChat1To1Decomposer(mContext);
    }

    protected int parseGroupMsg(File file) {
        if (mFavChat1To1Decomposer == null) {
            Log.e(CLASS_TAG, "mChat1To1RestoreParser == null,return");
            return CloudBrUtils.ResultCode.OTHER_EXCEPTION;
        }

        if (mChatGroupParser == null) {
            Log.e(CLASS_TAG, "mChatGroupParser == null,return");
            return CloudBrUtils.ResultCode.OTHER_EXCEPTION;
        }

        if (file == null || !file.exists()) {
            Log.e(CLASS_TAG, "parseFtMsg file = null or file is not exists, return");
            return CloudBrUtils.ResultCode.OTHER_EXCEPTION;
        }

        Log.d(CLASS_TAG, "parseFtMsg file name = " + file.getName());
        initParseRecord();

        int result = mChatGroupParser.parseChatGroupMsg(file);
        if (result != CloudBrUtils.ResultCode.OK) {
            Log.e(CLASS_TAG, "mChatGroupParser.parseChatGroupMsgImple result is not ok, return");
            return result;
        }
        Log.d(CLASS_TAG, "mChatGroupParser.parseChatGroupMsgImple end, result = ok");

        return CloudBrUtils.ResultCode.OK;
    }

    private void initParseRecord() {
        mChatRecord = null;
        mRootRecord = null;

        mChatRecord = new ChatRecord();
        mRootRecord = new RootRecord();

        mChatGroupParser.setChatRecord(mChatRecord);
        mChatGroupParser.setRootRecord(mRootRecord);
    }

    @Override
    public int dealChatRecord() {
        return CloudBrUtils.ResultCode.OK;
    }

    @Override
    public int persistMessageBody(String chatId, long threadId, String msgContent) {
        if (mCancel) {
            Log.d(CLASS_TAG, "persistMessageBody service canceled");
            return CloudBrUtils.ResultCode.SERVICE_CANCELED;
        }
        return mFavChat1To1Decomposer.persistMessageBody(chatId, msgContent);
    }

    /**
     * This method will be called when backup service be cancel.
     * @param cancel
     */
    protected void setCancel(boolean cancel) {
        mCancel = cancel;
    }
}
