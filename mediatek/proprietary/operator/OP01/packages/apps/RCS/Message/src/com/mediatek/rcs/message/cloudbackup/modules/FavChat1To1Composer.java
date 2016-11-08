package com.mediatek.rcs.message.cloudbackup.modules;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import com.mediatek.rcs.message.cloudbackup.modules.Chat1To1BackupBuilder;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.Favorite;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.MessageRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.RcsMsgRecord;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils;

import android.content.ContentResolver;
import android.database.Cursor;
import android.util.Log;

/**
 * Backup favorite ip text message module.
 *
 */
class FavChat1To1Composer {
    private static final String CLASS_TAG = CloudBrUtils.MODULE_TAG + "FavChat1To1Composer";
    private Chat1To1BackupBuilder mChat1To1BackupBuilder;
    private ArrayList<Integer> mIds;
    private String mFavBackupFolder;
    private ContentResolver mContentResolver;
    private boolean mCancel = false;

    FavChat1To1Composer(String filePath, ContentResolver contentResolver,
            ArrayList<Integer> ids) {
        mFavBackupFolder = filePath;
        mContentResolver = contentResolver;
        mChat1To1BackupBuilder = new Chat1To1BackupBuilder();
        mIds = ids;
    }

    protected int backupChatMsg() {
        Log.d(CLASS_TAG, "backupChatMsg() begin");
        for (int id : mIds) {
            if (mCancel) {
                Log.d(CLASS_TAG, "backup1To1ChatMsg() service canceled");
                return CloudBrUtils.ResultCode.SERVICE_CANCELED;
            }

            String selection = CloudBrUtils.ID + " = " + id;
            Log.d(CLASS_TAG, "backup1To1ChatMsg selection = " + selection);
            Cursor favCursor = mContentResolver.query(CloudBrUtils.FAVOTIRE_URI, null, selection,
                    null, null);
            favCursor.moveToFirst();

            File file = new File(mFavBackupFolder + File.separator + id + "chat.txt");
            String backupMsg = composeChatMsg(favCursor);
            try {
                file.createNewFile();
                BufferedWriter chatWriter = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(file)));
                chatWriter.write(backupMsg);
                chatWriter.close();
            } catch (IOException e) {
                Log.e(CLASS_TAG, "create file fail chat id = " + id);
                e.printStackTrace();
                return CloudBrUtils.ResultCode.IO_EXCEPTION;
            }
            if (favCursor != null) {
                favCursor.close();
            }
        }
        if (mCancel) {
            Log.d(CLASS_TAG, "backup1To1ChatMsg() service canceled");
            return CloudBrUtils.ResultCode.SERVICE_CANCELED;
        }
        return CloudBrUtils.ResultCode.OK;
    }

    private String composeChatMsg(Cursor msgCs) {
        RcsMsgRecord rcsMsgRecord = new RcsMsgRecord();
        getMessageInfo(msgCs, rcsMsgRecord);

        String backupedMsg = mChat1To1BackupBuilder.build1To1ChatMsg(null, rcsMsgRecord);
        return backupedMsg;
    }

    private static int getMessageInfo(Cursor msgCs, RcsMsgRecord rcsMsgRecord) {
        String from = msgCs.getString(msgCs.getColumnIndex(Favorite.COLUMN_CONTACT_NUB));
        if (from == null) {
            from = CloudBrUtils.getMyNumber();
        }
        rcsMsgRecord.setFrom(from);
        rcsMsgRecord.setTo(from);
        rcsMsgRecord.setTimestamp(msgCs.getLong(msgCs.getColumnIndex(Favorite.COLUMN_DATE)));
        String favBody = msgCs.getString(msgCs.getColumnIndex(Favorite.COLUMN_BODY));
        rcsMsgRecord.setBody(favBody);

        String mimeType = msgCs.getString(msgCs.getColumnIndex(Favorite.COLUMN_DA_MIME_TYPE));
        Log.d(CLASS_TAG, "ct = " + mimeType);
        rcsMsgRecord.setMimeType(mimeType);

        rcsMsgRecord.setChatId(msgCs.getString(msgCs.getColumnIndex(Favorite.COLUMN_CHATID)));
        rcsMsgRecord.setContactNum(msgCs.getString(
                                    msgCs.getColumnIndex(Favorite.COLUMN_CONTACT_NUB)));
        rcsMsgRecord.setDirection(msgCs.getInt(msgCs.getColumnIndex(Favorite.COLUMN_DA_DIRECTION)));
        rcsMsgRecord.setFlag(msgCs.getInt(msgCs.getColumnIndex(Favorite.COLUMN_DA_FLAG)));
        rcsMsgRecord.setIpmsgId(msgCs.getInt(msgCs.getColumnIndex(Favorite.COLUMN_DA_ID)));
        rcsMsgRecord.setStatus(msgCs.getInt(
                                    msgCs.getColumnIndex(Favorite.COLUMN_DA_MESSAGE_STATUS)));
        rcsMsgRecord.setTimestamp(msgCs.getLong(msgCs.getColumnIndex(Favorite.COLUMN_DATE)));
        rcsMsgRecord.setDataSent(msgCs.getInt(msgCs.getColumnIndex(Favorite.COLUMN_DA_TIMESTAMP)));
        rcsMsgRecord.setType(msgCs.getInt(msgCs.getColumnIndex(Favorite.COLUMN_DA_TYPE)));
        // if (type.equals(CloudBrUtils.ContentType.VEMOTION_TYPE)) {
        // msgRecord.setType(Message.Type.VEMOTION);
        // } else if (type.equals(CloudBrUtils.ContentType.CLOUDFILE_TYPE)) {
        // msgRecord.setType(Message.Type.CLOUDFILE);
        // } else {
        // msgRecord.setType(Message.Type.CONTENT);
        // }
        return CloudBrUtils.ResultCode.OK;
    }

    /**
     * This method will be called when backup service be cancel.
     *
     * @param cancel
     */
    protected void setCancel(boolean cancel) {
        mCancel = cancel;
    }
}
