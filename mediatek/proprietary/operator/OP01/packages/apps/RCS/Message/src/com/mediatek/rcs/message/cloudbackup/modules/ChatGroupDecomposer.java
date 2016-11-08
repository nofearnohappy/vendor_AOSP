package com.mediatek.rcs.message.cloudbackup.modules;

import com.mediatek.rcs.message.cloudbackup.modules.ChatGroupParser.IGroupChatMsgParse;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.ChatRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.RootRecord;
import com.mediatek.rcs.message.cloudbackup.utils.FileUtils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import java.io.File;


/**
 * This class is used to parse group msg file that get from service.
 */
class ChatGroupDecomposer implements IGroupChatMsgParse {
    private static final String CLASS_TAG = CloudBrUtils.MODULE_TAG + "ChatGroupDecomposer";
    private ContentResolver mContentResolver;
    private ChatGroupParser mChatGroupParser;
    private Chat1To1Decomposer mChat1To1Decomposer;
    private ChatRecord mChatRecord;
    private RootRecord mRootRecord;
    private Context mContext;
    private boolean mCancel = false;

    /**
     * @param contentResolver
     */
    ChatGroupDecomposer(Context context) {
        mContext = context;
        mContentResolver = mContext.getContentResolver();
        mChatGroupParser = new ChatGroupParser(mContext, this);
        mChat1To1Decomposer = new Chat1To1Decomposer(mContext);
    }

    /**
     * This method will be called when backup service be cancel.
     * @param cancel
     */
    public void setCancel(boolean cancel) {
        mCancel = cancel;
    }

    protected int parseGroupMsg(File file) {
        if (mChat1To1Decomposer == null) {
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

        if (mCancel) {
            Log.d(CLASS_TAG, "backup1To1ChatMsg() service canceled");
            return CloudBrUtils.ResultCode.SERVICE_CANCELED;
        }

        int result = mChatGroupParser.parseChatGroupMsg(file);
        if (result != CloudBrUtils.ResultCode.OK) {
            Log.e(CLASS_TAG, "mChatGroupParser.parseChatGroupMsgImple result is not ok, return");
            return result;
        }
        Log.d(CLASS_TAG, "mChatGroupParser.parseChatGroupMsgImple end, result = ok");

        if (mCancel) {
            Log.d(CLASS_TAG, "backup1To1ChatMsg() service canceled");
            return CloudBrUtils.ResultCode.SERVICE_CANCELED;
        }
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

    protected int insertDataBase() {
        boolean isChatInfoExited = true;
        String selection = CloudBrUtils.CHAT_ID + " is \"" + mChatRecord.getChatId() + "\"";
        Log.d(CLASS_TAG, "selection = " + selection);
        Cursor chatCursor = mContentResolver.query(CloudBrUtils.CHAT_CHAT_URI, null, selection,
                null, null);
        if (chatCursor.getCount() <= 0) {
            Log.d(CLASS_TAG, "chat info have not exited in chat table, need insert");
            isChatInfoExited = false;
        }
        if (chatCursor != null) {
            chatCursor.close();
        }

        if (!isChatInfoExited) {
            Log.d(CLASS_TAG, "insertDataBase need inset chat db and groupnumber table");
            int result = FileUtils.insertChatRecord(mChatRecord, mContentResolver);
            if (result != CloudBrUtils.ResultCode.OK) {
                Log.e(CLASS_TAG, "insertChatRecord error");
                return result;
            }

            result = FileUtils.insertgroupNumberRecord(mChatRecord, mRootRecord, mContentResolver);
            if (result != CloudBrUtils.ResultCode.OK) {
                Log.e(CLASS_TAG, "insertgroupNumberRecord error");
                return result;
            }
        }

        long threadId = FileUtils.createGroupThread(mContext, mChatRecord);
        Log.d(CLASS_TAG, "insertDataBase createGroupThread = " + threadId);
        mChatRecord.setThreadId(threadId);
        return CloudBrUtils.ResultCode.OK;
    }

    private int restructureRecord() {
        String chatId = mChatRecord.getChatId();
        String rejoinId = mChatRecord.getRejoinId();

        if (rejoinId == null) {
            Log.d(CLASS_TAG, "session identify id is null, cant restore continue, return");
            return CloudBrUtils.ResultCode.BACKUP_FILE_ERROR;
        }
        if (chatId == null) {
            Log.d(CLASS_TAG, "chat id is null, rejone id assgin to chatId");
            chatId = rejoinId;
            mChatRecord.setChatId(mChatRecord.getRejoinId());
        }

        Log.d(CLASS_TAG, "mRootRecord.getParticipants() = " + mRootRecord.getParticipants());
        mChatRecord.setParticipants(mRootRecord.getParticipants());
        return CloudBrUtils.ResultCode.OK;
    }

    @Override
    public int dealChatRecord() {
        int result = restructureRecord();
        if (result != CloudBrUtils.ResultCode.OK) {
            Log.e(CLASS_TAG, "restructureRecord result is not ok, return");
            return result;
        }
        Log.d(CLASS_TAG, "restructureRecord end, result = ok");

        result = insertDataBase();
        Log.d(CLASS_TAG, "insertDataBase end, result = " + result);
        return result;
    }

    @Override
    public int persistMessageBody(String chatId, long threadId, String msgContent) {
        if (mCancel) {
            Log.d(CLASS_TAG, "backup1To1ChatMsg() service canceled");
            return CloudBrUtils.ResultCode.SERVICE_CANCELED;
        }
        return mChat1To1Decomposer.persistMessageBody(chatId, threadId, msgContent);
    }
}
