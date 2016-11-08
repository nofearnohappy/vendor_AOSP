package com.mediatek.rcs.message.cloudbackup.modules;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;

import com.mediatek.rcs.message.cloudbackup.modules.FtMsgBackupBuilder;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.BackupConstant;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.Favorite;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.ResultCode;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.FileObject;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.FtRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.RcsMsgRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.RootRecord;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils;

import android.content.ContentResolver;
import android.database.Cursor;
import android.util.Log;

class FavFtComposer {
    private static final String CLASS_TAG = CloudBrUtils.MODULE_TAG + "FavFtComposer";
    private String mFavBackupFolder;
    private ContentResolver mContentResolver;
    private FtMsgBackupBuilder mFtMsgBackupBuilder;
    private ArrayList<Integer> mIds;
    private boolean mCancel = false;

    /**
     * MTK used one to one format to wrap favorite ft message.
     * @param filePath
     *            backup data put here.
     * @param contentResolver
     * @param ids
     *            Ft data ids in the favorite table of rcsmessage.db.
     */
    FavFtComposer(String filePath, ContentResolver contentResolver, ArrayList<Integer> ids) {
        mFavBackupFolder = filePath;
        mContentResolver = contentResolver;
        mFtMsgBackupBuilder = new FtMsgBackupBuilder(mFavBackupFolder);
        mIds = ids;
    }

    protected int composeFtMsg() {
        Log.d(CLASS_TAG, "backup1To1FtMsg begin");
        int result = backup1To1FtMsg();
        if (result != ResultCode.OK) {
            Log.d(CLASS_TAG, "backup1ToManyMsg happen is not ok return.");
            return result;
        }

        if (mCancel) {
            Log.d(CLASS_TAG, "composeFtMsg() service canceled");
            result = CloudBrUtils.ResultCode.SERVICE_CANCELED;
        }
        return result;
    }

    private int backup1To1FtMsg() {
        int result = ResultCode.OK;
        Log.d(CLASS_TAG, "backup1To1FtMsg begin");
        for (int id : mIds) {
            if (mCancel) {
                Log.d(CLASS_TAG, "backup1To1FtMsg service canceled");
                result = CloudBrUtils.ResultCode.SERVICE_CANCELED;
            }

            String selection = CloudBrUtils.ID + " = " + id;
            Log.d(CLASS_TAG, "backup1To1FtMsg selection = " + selection);
            Cursor favCursor = mContentResolver.query(CloudBrUtils.FAVOTIRE_URI, null, selection,
                    null, null);
            favCursor.moveToFirst();

            String filePath = favCursor.getString(favCursor
                    .getColumnIndex(Favorite.COLUMN_PATH));
            File file = new File(filePath);
            if (file == null || !file.exists()) {
                Log.e(CLASS_TAG, filePath + "is not exists, continue");
                favCursor.close();
                continue;
            }

            String type = favCursor.getString(favCursor.getColumnIndex(
                    Favorite.COLUMN_DA_MIME_TYPE));
            FtRecord ftRecord = new FtRecord();
            RcsMsgRecord rcsMsgRecord = new RcsMsgRecord();
            RootRecord rootRecord = new RootRecord();
            FileObject fileObjectRecord = new FileObject();
            getFileObjectInfo(favCursor, fileObjectRecord);
            getFavInfo(favCursor, ftRecord, rcsMsgRecord);

            String from = favCursor.getString(favCursor
                    .getColumnIndex(Favorite.COLUMN_CONTACT_NUB));
            if (from == null) {
                from = CloudBrUtils.getMyNumber();
            }
            ftRecord.setFrom(from);
            rcsMsgRecord.setFrom(from);
            rootRecord.setParticipants(from);
            rootRecord.setSessionType(CloudBrUtils.FileTransferType.ONE_TO_ONE);

            String header = mFtMsgBackupBuilder.build1ToNHeader(ftRecord, rcsMsgRecord);
            String fileObject = mFtMsgBackupBuilder.buildFileObject(fileObjectRecord);
            String root = mFtMsgBackupBuilder.buildRootRecord(rootRecord);

            Writer chatWriter = null;
            OutputStream stream = null;
            File backupfile = new File(mFavBackupFolder + File.separator + id + "ft.txt");

            try {
                backupfile.createNewFile();
                stream = new FileOutputStream(backupfile);
                chatWriter = new BufferedWriter(new OutputStreamWriter(stream));
                chatWriter.write(header);
                chatWriter.write(root);
                chatWriter.write(BackupConstant.LINE_BREAK);
                chatWriter.write(fileObject);
            } catch (IOException e) {
                Log.e(CLASS_TAG, "create file fail ft id = " + id);
                e.printStackTrace();
                return ResultCode.IO_EXCEPTION;
            }
            result = mFtMsgBackupBuilder.addMsgBody(chatWriter, stream, type, filePath);
            if (result != ResultCode.OK) {
                Log.d(CLASS_TAG, "copy ft id = " + id + "happen error." + " copyResult = " + result
                        + "return");
                if (favCursor != null) {
                    favCursor.close();
                    favCursor = null;
                }
                return result;
            }
            try {
                if (chatWriter != null) {
                    chatWriter.close();
                }
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (favCursor != null) {
                favCursor.close();
                favCursor = null;
            }
        }
        if (mCancel) {
            Log.d(CLASS_TAG, "backup1To1FtMsg service canceled");
            result = CloudBrUtils.ResultCode.SERVICE_CANCELED;
        }
        return result;
    }

    private int getFileObjectInfo(Cursor ftCursor, FileObject fileObject) {
        Log.d(CLASS_TAG, "composeFileObject begin");
        fileObject.setCid(ftCursor.getString(ftCursor.getColumnIndex(Favorite.COLUMN_DA_ID)));
        String fileName = ftCursor.getString(ftCursor.getColumnIndex(Favorite.COLUMN_PATH));
        fileName = fileName.substring(fileName.lastIndexOf(File.separator) + 1);
        fileObject.setName(fileName);
        fileObject.setType(ftCursor.getString(ftCursor.getColumnIndex(
                Favorite.COLUMN_DA_MIME_TYPE)));
        fileObject.setSize(Long.parseLong(ftCursor.getString(ftCursor
                .getColumnIndex(Favorite.COLUMN_SIZE))));
        fileObject.setDate(ftCursor.getLong(ftCursor.getColumnIndex(Favorite.COLUMN_DATE)));
        return CloudBrUtils.ResultCode.OK;
    }

    private int getFavInfo(Cursor favCs, FtRecord ftRecord, RcsMsgRecord rcsMsgRecord) {
        ftRecord.setMsgId(favCs.getString(favCs.getColumnIndex(Favorite.COLUMN_DA_ID)));
        ftRecord.setTimestamp(favCs.getLong(favCs.getColumnIndex(Favorite.COLUMN_DATE)));
        ftRecord.setFtId(favCs.getString(favCs.getColumnIndex(Favorite.COLUMN_DA_ID)));

        rcsMsgRecord.setChatId(favCs.getString(favCs.getColumnIndex(Favorite.COLUMN_CHATID)));
        rcsMsgRecord.setContactNum(favCs.getString(
                                    favCs.getColumnIndex(Favorite.COLUMN_CONTACT_NUB)));
        rcsMsgRecord.setDirection(favCs.getInt(favCs.getColumnIndex(
                Favorite.COLUMN_DA_DIRECTION)));
        rcsMsgRecord.setFilePath(favCs.getString(favCs.getColumnIndex(Favorite.COLUMN_PATH)));
        rcsMsgRecord.setFlag(favCs.getInt(favCs.getColumnIndex(Favorite.COLUMN_DA_FLAG)));
        rcsMsgRecord.setIpmsgId(favCs.getInt(favCs.getColumnIndex(Favorite.COLUMN_DA_ID)));
        rcsMsgRecord.setMimeType(favCs.getString(favCs.getColumnIndex(
                Favorite.COLUMN_DA_MIME_TYPE)));
        rcsMsgRecord.setStatus(favCs.getInt(favCs.getColumnIndex(
                Favorite.COLUMN_DA_MESSAGE_STATUS)));
        rcsMsgRecord.setTimestamp(favCs.getLong(favCs.getColumnIndex(Favorite.COLUMN_DATE)));
        rcsMsgRecord.setDataSent(favCs.getInt(favCs.getColumnIndex(Favorite.COLUMN_DA_TIMESTAMP)));
        rcsMsgRecord.setType(favCs.getInt(favCs.getColumnIndex(Favorite.COLUMN_DA_TYPE)));
        return CloudBrUtils.ResultCode.OK;
    }

    /**
     * This method will be called when backup service be cancel.
     * @param cancel
     */
    protected void setCancel(boolean cancel) {
        mCancel = cancel;
    }
}
