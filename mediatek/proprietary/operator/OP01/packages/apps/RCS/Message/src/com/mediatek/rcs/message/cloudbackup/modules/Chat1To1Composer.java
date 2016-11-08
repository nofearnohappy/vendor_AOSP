package com.mediatek.rcs.message.cloudbackup.modules;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;

import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils;
import com.mediatek.rcs.message.cloudbackup.utils.FileUtils;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.Message;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.RcsMessage;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.MessageRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.RcsMsgRecord;

import android.content.ContentResolver;
import android.database.Cursor;
import android.util.Log;

/**
 * Backup all 1 to 1 ip text message.
 */
class Chat1To1Composer {
    private static final String CLASS_TAG = CloudBrUtils.MODULE_TAG + "Chat1To1Composer";
    private Chat1To1BackupBuilder mChat1To1BackupBuilder;
    private String mBackupFolder;
    private ContentResolver mContentResolver;
    private boolean mCancel = false;

    Chat1To1Composer(String filePath, ContentResolver contentResolver) {
        mBackupFolder = filePath;
        mContentResolver = contentResolver;
        mChat1To1BackupBuilder = new Chat1To1BackupBuilder();
    }

    /**
     * This method will be called when backup service be cancel.
     * @param cancel
     */
    public void setCancel(boolean cancel) {
        mCancel = cancel;
    }

    public int backup1To1ChatMsg() {
        Log.d(CLASS_TAG, "backup1To1ChatMsg begin...");

        //msg is o2o o2m and type is im or xml and is normal or emotion or cloud
        String rcsMessageSel = RcsMessage.MESSAGE_COLUMN_FLAG + " is not 3 AND (" +
                RcsMessage.MESSAGE_COLUMN_TYPE + " is 1 OR " +
                RcsMessage.MESSAGE_COLUMN_TYPE + " is 3 )" + " AND (" +
                RcsMessage.MESSAGE_COLUMN_MSG_CLASS + " = 0 OR " +
                RcsMessage.MESSAGE_COLUMN_MSG_CLASS + " = 2 OR " +
                RcsMessage.MESSAGE_COLUMN_MSG_CLASS + " = 3)" ;
        Log.d(CLASS_TAG, "rcsMessageSel = " + rcsMessageSel);

        Cursor rcsCursor = mContentResolver
                 .query(CloudBrUtils.RCS_URI, null, rcsMessageSel, null, null);
        rcsCursor.moveToFirst();
        int ipmsgId;
        int id;

        int ipmsgIdIndex = rcsCursor.getColumnIndex(RcsMessage.MESSAGE_COLUMN_IPMSG_ID);
        int idIndex = rcsCursor.getColumnIndex(CloudBrUtils.ID);
        Log.d(CLASS_TAG, "chat 1 to 1 msg count is " + rcsCursor.getCount());

        while (!rcsCursor.isAfterLast()) {
            ipmsgId = rcsCursor.getInt(ipmsgIdIndex);
            id = rcsCursor.getInt(idIndex);

            Log.d(CLASS_TAG, "backup1To1ChatMsg msgid = " + id);

            String msgSelection = "_id is " + ipmsgId;
            Cursor msgCursor = mContentResolver.query(CloudBrUtils.CHAT_CONTENT_URI, null,
                    msgSelection, null, null);
            Log.d(CLASS_TAG, "backup1To1ChatMsg msgCursor.getCount() = " + msgCursor.getCount());
            if (msgCursor == null || msgCursor.getCount() <= 0) {
                Log.d(CLASS_TAG, "msg table havnt this msg record ,msgSelection = " + msgSelection);
                rcsCursor.moveToNext();
                if (msgCursor != null) {
                    msgCursor.close();
                }
                continue;
            }
            msgCursor.moveToFirst();
            boolean isNeedBackup = CloudBrUtils.isMsgNeedBackup(rcsCursor);
            if (!isNeedBackup) {
                Log.d(CLASS_TAG, "this msg neednot backup");
                rcsCursor.moveToNext();
                if (msgCursor != null) {
                    msgCursor.close();
                }
                continue;
            }

            Writer chatWriter = null;
            String msgBody = null;
            File file = null;
            file = new File(mBackupFolder + File.separator + ipmsgId + "chat.txt");
            try {
                file.createNewFile();
                chatWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
            } catch (IOException e) {
                Log.e(CLASS_TAG, "create file fail chat id = " + ipmsgId);
                e.printStackTrace();
                return CloudBrUtils.ResultCode.IO_EXCEPTION;
            }
            msgBody = composeChatMsg(msgCursor, rcsCursor);

            if (msgCursor != null) {
                msgCursor.close();
                msgCursor = null;
            }

            if (msgBody == null) {
                Log.d(CLASS_TAG, "chat1To1MsgComposer.composeChatMsg happen error, msgBody = null");
                if (rcsCursor != null) {
                    rcsCursor.close();
                }
                return CloudBrUtils.ResultCode.OTHER_EXCEPTION;
            }

            if (chatWriter != null) {
                try {
                    chatWriter.write(msgBody);
                    chatWriter.close();
                    chatWriter = null;
                } catch (IOException e) {
                    e.printStackTrace();
                    if (rcsCursor != null) {
                        rcsCursor.close();
                    }
                    return CloudBrUtils.ResultCode.IO_EXCEPTION;
                }
            }
            rcsCursor.moveToNext();
        }

        Log.d(CLASS_TAG, "backup1To1ChatMsg end");
        if (rcsCursor != null) {
            rcsCursor.close();
            rcsCursor = null;
        }

        if (mCancel) {
            Log.d(CLASS_TAG, "backup1To1ChatMsg() service canceled");
            return CloudBrUtils.ResultCode.SERVICE_CANCELED;
        }
        return CloudBrUtils.ResultCode.OK;
    }

    private String composeChatMsg(Cursor msgCs, Cursor rcsCs) {
        MessageRecord msgRecord = new MessageRecord();
        RcsMsgRecord rcsMsgRecord = new RcsMsgRecord();

        FileUtils.getRcsMessageInfo(rcsCs, rcsMsgRecord);
        FileUtils.getChatMessageInfo(msgCs, msgRecord);
        String contactNum = rcsCs.getString(rcsCs
                .getColumnIndex(RcsMessage.MESSAGE_COLUMN_CONTACT_NUMBER));
        int direction = rcsCs.getInt(rcsCs.getColumnIndex(RcsMessage.MESSAGE_COLUMN_DIRECTION));
        String from;
        String to;
        if (direction == CloudBrUtils.Message.Direction.INCOMING) {
            from = contactNum;
            to = CloudBrUtils.getMyNumber();
        } else if (direction == CloudBrUtils.Message.Direction.OUTGOING) {
            to = contactNum;
            from = CloudBrUtils.getMyNumber();
        } else {
            Log.d(CLASS_TAG, "Irrelevant or not applicable (e.g. for a system message)");
            from = CloudBrUtils.getMyNumber();
            to = null;
        }
        rcsMsgRecord.setFrom(from);
        rcsMsgRecord.setTo(to);
        String backupedMsg = mChat1To1BackupBuilder.build1To1ChatMsg(msgRecord, rcsMsgRecord);
        return backupedMsg;
    }
}
