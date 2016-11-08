package com.mediatek.rcs.message.cloudbackup.modules;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import com.mediatek.rcs.common.RcsLog;
import com.mediatek.rcs.message.cloudbackup.modules.Chat1To1Composer;
import com.mediatek.rcs.message.cloudbackup.modules.ChatGroupComposer;
import com.mediatek.rcs.message.cloudbackup.modules.FtMsgComposer;
import com.mediatek.rcs.message.cloudbackup.modules.MmsComposer;
import com.mediatek.rcs.message.cloudbackup.modules.SmsComposer;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.ResultCode;
import com.mediatek.rcs.message.cloudbackup.utils.FileUtils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

/**
 * Backup message module
 * @author mtk81368
 *
 */
public class MessageBackupComposer {
    private static final String CLASS_TAG = CloudBrUtils.MODULE_TAG + "MessageBackupComposer";

    private HashMap<String, ArrayList<Integer>> mChatGroupMap;
    private HashMap<String, ArrayList<Integer>> mftGroupMap;
    private ArrayList<Integer> mFt1ToManyList;
    private ArrayList<Integer> mFt1To1List;
    private HashMap<String, ArrayList<Integer>> mGroupNumberMap;
    private HashMap<Long, String> mThreadMap;

    private FtMsgComposer mFtMsgComposer;
    private ChatGroupComposer mChatGroupComposer;
    private Chat1To1Composer mChat1To1Composer;
    private SmsComposer mSmsComposer;
   // private MmsComposer mMmsComposer;
    private File mFolderPath;
    private Context mContext;
    private ContentResolver mContentResolver;
    private boolean mCancel = false;
    private String mFavoriteFolderPath;

    /**
     *
     * @param context
     * @param folderPath
     */
    public MessageBackupComposer(Context context, String folderPath) {
        mContext = context;
        mFavoriteFolderPath = folderPath;
    }

    /**
     * This method will be called when backup service be cancel.
     *
     * @param cancel
     */
    public void setCancel(boolean cancel) {
        mCancel = cancel;
        if (mFtMsgComposer != null) {
            mFtMsgComposer.setCancel(cancel);
        }

        if (mChatGroupComposer != null) {
            mChatGroupComposer.setCancel(cancel);
        }

        if (mSmsComposer != null) {
            mSmsComposer.setCancel(cancel);
        }

//        if (mMmsComposer != null) {
//            mMmsComposer.setCancel(cancel);
//        }
        if (mChat1To1Composer != null) {
            mChat1To1Composer.setCancel(cancel);
        }
    }

    /**
     * backup data.
     * @return
     */
    public int backupData() {
        if (!createContentResolver()) {
            Log.e(CLASS_TAG, "createContentResolver error");
            return CloudBrUtils.ResultCode.OTHER_EXCEPTION;
        }

        mFolderPath = new File(mFavoriteFolderPath);
        if (mFolderPath != null && mFolderPath.exists()) {
            Log.d(CLASS_TAG, "clear mFolderPath");
            mFolderPath.delete();
        }
        mFolderPath.mkdirs();
        Log.d(CLASS_TAG, "backupData mFolderPath = " + mFolderPath);

        if (mCancel) {
            Log.d(CLASS_TAG, "backupData() service canceled");
            return CloudBrUtils.ResultCode.SERVICE_CANCELED;
        }

        Log.d(CLASS_TAG, "cacheChatMsg() cache data begin");
        cacheMessage();
        Log.d(CLASS_TAG, "cacheChatMsg() cache data end");

        int result = backupChatGroupMsg();
        if (result != CloudBrUtils.ResultCode.OK) {
            Log.e(CLASS_TAG, "backupChatGroupMsg error result " + result);
            return result;
        }
        Log.d(CLASS_TAG, "backupChatGroupMsg() ok");

        mChatGroupMap.clear();
        mChatGroupMap = null;

        result = backup1To1ChatMsg();
        if (result != CloudBrUtils.ResultCode.OK) {
            Log.e(CLASS_TAG, "backup1To1ChatMsg error result " + result);
            return result;
        }
        Log.d(CLASS_TAG, "backup1To1ChatMsg() result ok");

        result = backupFtMsg();
        if (result != CloudBrUtils.ResultCode.OK) {
            Log.e(CLASS_TAG, "backupFtMsg error result " + result);
            return result;
        }

        result = backupSms();
        if (result != CloudBrUtils.ResultCode.OK) {
            Log.e(CLASS_TAG, "backupSms error result " + result);
            return result;
        }

        //result = backupMms();
        return CloudBrUtils.ResultCode.OK;
    }

//    private int backupMms() {
//        mMmsComposer = new MmsComposer(mFolderPath.getAbsolutePath(), mContext);
//        int result = mMmsComposer.backupMmsData();
//        Log.d(CLASS_TAG, "backupMms() result = " + result);
//        mMmsComposer = null;
//        return result;
//    }

    private int backupSms() {
        mSmsComposer = new SmsComposer(mFolderPath.getAbsolutePath(), mContext);
        int result = mSmsComposer.backupSmsData();
        Log.d(CLASS_TAG, "backupSms() result = " + result);
        mSmsComposer = null;
        return result;
    }

    private int backupFtMsg() {
        if (mCancel) {
            Log.d(CLASS_TAG, "backupData() service canceled");
            return CloudBrUtils.ResultCode.SERVICE_CANCELED;
        }

        Cursor ftCursor = mContentResolver.query(CloudBrUtils.FT_URI, null, null, null, null);
        if (ftCursor != null && ftCursor.getCount() <= 0) {
            Log.d(CLASS_TAG, "backupFtMsg ft table is empty. return ok");
            ftCursor.close();
            return ResultCode.OK;
        }
        if (ftCursor != null) {
            ftCursor.close();
        }

        mFtMsgComposer = new FtMsgComposer(mFolderPath.getAbsolutePath(), mContentResolver);
        cacheFtMsg();
        Log.d(CLASS_TAG, "mFt1ToManyList size = " + mFt1ToManyList.size());
        mFtMsgComposer.setBackupParam(mFt1ToManyList, mftGroupMap, mGroupNumberMap,
                mFt1To1List);
        int result = mFtMsgComposer.backupFtMsg();
        Log.d(CLASS_TAG, "backupFtMsg end");
        mFtMsgComposer = null;
        return result;
    }

    private int backup1To1ChatMsg() {
        if (mCancel) {
            Log.d(CLASS_TAG, "backupData() service canceled");
            return CloudBrUtils.ResultCode.SERVICE_CANCELED;
        }
        mChat1To1Composer = new Chat1To1Composer(mFolderPath.getAbsolutePath(), mContentResolver);
        int result = mChat1To1Composer.backup1To1ChatMsg();
        Log.d(CLASS_TAG, "backup1To1ChatMsg end");
        mChat1To1Composer = null;
        return result;
    }

    private int backupChatGroupMsg() {
        // backup group message
        mChatGroupComposer = new ChatGroupComposer(mFolderPath.getAbsolutePath(), mContentResolver);
        mChatGroupComposer.setBackupParam(mGroupNumberMap, mChatGroupMap);
        int result = mChatGroupComposer.backupChatGroupMsg();
        Log.d(CLASS_TAG, "group msg backup end");
        mChatGroupComposer = null;
        return result;
    }

    private boolean createContentResolver() {
        if (mContentResolver != null) {
            Log.d(CLASS_TAG, "createContentResolver() Resolver exit!");
            return true;
        }
        if (mContext == null) {
            Log.e(CLASS_TAG, "cacheData mContext = null, return");
            return false;
        }
        mContentResolver = mContext.getContentResolver();
        if (mContentResolver == null) {
            Log.e(CLASS_TAG, "cacheData mContentResolver = null, return");
            return false;
        }
        return true;
    }

    private boolean cacheMessage() {
        if (!createContentResolver()) {
            Log.e(CLASS_TAG, "createContentResolver error");
            return false;
        }
        cacheGroupMessage();
        cacheGroupNumber();
        return true;
    }

    private boolean cacheFtMsg() {
        Log.d(CLASS_TAG, "cacheFtMsg begin");
        if (!createContentResolver()) {
            Log.e(CLASS_TAG, "createContentResolver error");
            return false;
        }

        mftGroupMap = new HashMap<String, ArrayList<Integer>>();
        mFt1ToManyList = new ArrayList<Integer>();
        mFt1To1List = new ArrayList<Integer>();
        String[] rcs_message_projection = new String[] { CloudBrUtils.ID,
                CloudBrUtils.RcsMessage.MESSAGE_COLUMN_CHAT_ID,
                CloudBrUtils.RcsMessage.MESSAGE_COLUMN_MESSAGE_STATUS,
                CloudBrUtils.RcsMessage.MESSAGE_COLUMN_FLAG,
                CloudBrUtils.RcsMessage.MESSAGE_COLUMN_ISBLOCKED,
                CloudBrUtils.RcsMessage.MESSAGE_COLUMN_MSG_CLASS
                };
        String rcsMessageSel = CloudBrUtils.RcsMessage.MESSAGE_COLUMN_TYPE + " = " +
                RcsLog.MessageType.FT;
        Log.d(CLASS_TAG, "rcsMessageSel = " + rcsMessageSel);
        Cursor rcsCursor = mContentResolver
                .query(CloudBrUtils.RCS_URI, rcs_message_projection, rcsMessageSel, null, null);
        rcsCursor.moveToFirst();
        int id;
        int msgClass;
        int msgStatus;
        int isBlock;
        int flag;
        String chatId;
        int idIndex = rcsCursor.getColumnIndex(CloudBrUtils.ID);
        int chatIdIndex = rcsCursor.getColumnIndex(CloudBrUtils.RcsMessage.MESSAGE_COLUMN_CHAT_ID);
        int msgClassIndex = rcsCursor.getColumnIndex(
                CloudBrUtils.RcsMessage.MESSAGE_COLUMN_MSG_CLASS);
        int msgStatusIndex = rcsCursor.getColumnIndex(
                CloudBrUtils.RcsMessage.MESSAGE_COLUMN_MESSAGE_STATUS);
        int msgFlagIndex = rcsCursor.getColumnIndex(
                CloudBrUtils.RcsMessage.MESSAGE_COLUMN_FLAG);
        int isBlockIndex = rcsCursor.getColumnIndex(
                CloudBrUtils.RcsMessage.MESSAGE_COLUMN_ISBLOCKED);
        Log.d(CLASS_TAG, "cacheMessageTable ft count is " + rcsCursor.getCount());

        while (!rcsCursor.isAfterLast()) {
            id = rcsCursor.getInt(idIndex);
            chatId = rcsCursor.getString(chatIdIndex);
            msgClass = rcsCursor.getInt(msgClassIndex);
            msgStatus = rcsCursor.getInt(msgStatusIndex);
            isBlock = rcsCursor.getInt(isBlockIndex);
            flag = rcsCursor.getInt(msgFlagIndex);

            if (FileUtils.isNeedBackup(msgStatus, isBlock, msgClass)) {
                Log.d(CLASS_TAG, "ft need backup");
                if (flag == RcsLog.ThreadFlag.MTM
                        && chatId != null) {
                    ArrayList<Integer> idList;
                    Log.d(CLASS_TAG, "id + " + id + " is a group ft msg");
                    if (mftGroupMap.containsKey(chatId)) {
                        idList = mftGroupMap.get(chatId);
                    } else {
                        idList = new ArrayList<Integer>();
                    }
                    idList.add(id);
                    mftGroupMap.put(chatId, idList);
                } else if (flag == RcsLog.ThreadFlag.OTM) {
                    Log.d(CLASS_TAG, "id + " + id + " is a 1 to multi ft msg");
                    mFt1ToManyList.add(id);
                } else if (flag == RcsLog.ThreadFlag.OTO) {
                    Log.d(CLASS_TAG, "id + " + id + " is a 1 to 1 ft msg");
                    mFt1To1List.add(id);
                }
            } else {
                Log.d(CLASS_TAG, "ft not need backup");
            }
            rcsCursor.moveToNext();
        }

        Log.d(CLASS_TAG, "cache ip group text msg from rcs message table end.");
        rcsCursor.close();
        return true;
    }

    /**
     * cache groupNumber table of rcsmessage.db.
     */
    private void cacheGroupNumber() {
        String[] groupnumber_proj = new String[] { CloudBrUtils.GroupChatMember.COLUMN_CHAT_ID,
                CloudBrUtils.ID };
        mGroupNumberMap = new HashMap<String, ArrayList<Integer>>();
        Cursor cursor = mContentResolver.query(CloudBrUtils.GROUP_MEMBER_URI, groupnumber_proj,
                null, null, null);
        int count = cursor.getCount();
        Log.d(CLASS_TAG, "group numbers table has data count = " + count);
        cursor.moveToFirst();
        int columnChatId = cursor.getColumnIndex(CloudBrUtils.GroupChatMember.COLUMN_CHAT_ID);
        int columnId = cursor.getColumnIndex(CloudBrUtils.ID);

        String chatId;
        int id;

        while (!cursor.isAfterLast()) {
            chatId = cursor.getString(columnChatId);
            if (chatId == null) {
                Log.d(CLASS_TAG, "cacheGroupNumber() chat id is null, skip");
                cursor.moveToNext();
                continue;
            }

            id = cursor.getInt(columnId);
            ArrayList<Integer> idList;
            Log.d(CLASS_TAG, "id + " + chatId);
            if (mGroupNumberMap.containsKey(chatId)) {
                idList = mGroupNumberMap.get(chatId);
            } else {
                idList = new ArrayList<Integer>();
            }
            idList.add(id);
            mGroupNumberMap.put(chatId, idList);
            cursor.moveToNext();
        }

        Log.d(CLASS_TAG, "cacheGroupNumberTable end.");
        cursor.close();
    }

    private boolean cacheGroupMessage() {
        Log.d(CLASS_TAG, "cacheMessageTable begin");
        if (!createContentResolver()) {
            Log.e(CLASS_TAG, "cacheMessageTable createContentResolver error");
            return false;
        }

        mChatGroupMap = new HashMap<String, ArrayList<Integer>>();
        String[] rcs_message_projection = new String[] { CloudBrUtils.ID,
                CloudBrUtils.RcsMessage.MESSAGE_COLUMN_CHAT_ID,
                CloudBrUtils.RcsMessage.MESSAGE_COLUMN_TYPE,
                CloudBrUtils.RcsMessage.MESSAGE_COLUMN_MSG_CLASS,
                CloudBrUtils.RcsMessage.MESSAGE_COLUMN_MESSAGE_STATUS,
                CloudBrUtils.RcsMessage.MESSAGE_COLUMN_ISBLOCKED};
        //Flag is MTM and type is IM or XML
        String rcsMessageSel = CloudBrUtils.RcsMessage.MESSAGE_COLUMN_FLAG + " = 3 AND (" +
               CloudBrUtils.RcsMessage.MESSAGE_COLUMN_TYPE + " = 1 OR " +
               CloudBrUtils.RcsMessage.MESSAGE_COLUMN_TYPE +" = 3)";
        Log.d(CLASS_TAG, "rcsMessageSel = " + rcsMessageSel);
        Cursor rcsCursor = mContentResolver
                .query(CloudBrUtils.RCS_URI, rcs_message_projection, rcsMessageSel, null, null);
        rcsCursor.moveToFirst();
        int id;
        int msgType;
        int msgStatus;
        int isBlock;
        int msgClass;

        String chatId;
        int idIndex = rcsCursor.getColumnIndex(CloudBrUtils.ID);
        int chatIdIndex = rcsCursor.getColumnIndex(CloudBrUtils.RcsMessage.MESSAGE_COLUMN_CHAT_ID);
        int msgClassIndex = rcsCursor.getColumnIndex(
                CloudBrUtils.RcsMessage.MESSAGE_COLUMN_MSG_CLASS);
        int msgStatusIndex = rcsCursor.getColumnIndex(
                CloudBrUtils.RcsMessage.MESSAGE_COLUMN_MESSAGE_STATUS);
        int isBlockIndex = rcsCursor.getColumnIndex(
                CloudBrUtils.RcsMessage.MESSAGE_COLUMN_ISBLOCKED);
        int msgTypeIndex = rcsCursor.getColumnIndex(
                CloudBrUtils.RcsMessage.MESSAGE_COLUMN_TYPE);
        Log.d(CLASS_TAG, "cacheMessageTable count is " + rcsCursor.getCount());

        while (!rcsCursor.isAfterLast()) {
            id = rcsCursor.getInt(idIndex);
            chatId = rcsCursor.getString(chatIdIndex);
            msgClass = rcsCursor.getInt(msgClassIndex);
            msgStatus = rcsCursor.getInt(msgStatusIndex);
            msgType = rcsCursor.getInt(msgTypeIndex);
            isBlock = rcsCursor.getInt(isBlockIndex);

            if (chatId != null && FileUtils.isNeedBackup(msgStatus, isBlock, msgClass)) {
                ArrayList<Integer> idList;
                Log.d(CLASS_TAG, "id + " + id + " is a group chat msg");
                if (mChatGroupMap.containsKey(chatId)) {
                    idList = mChatGroupMap.get(chatId);
                } else {
                    idList = new ArrayList<Integer>();
                }
                idList.add(id);
                mChatGroupMap.put(chatId, idList);
            }
            rcsCursor.moveToNext();
        }

        Log.d(CLASS_TAG, "cache ip group text msg from rcs message table end.");
        rcsCursor.close();
        return true;
    }
}
