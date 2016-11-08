package com.mediatek.rcs.message.cloudbackup.modules;

import com.mediatek.rcs.common.utils.RCSUtils;
import com.mediatek.rcs.message.cloudbackup.modules.FtMsgRestoreParser.IinitFileName;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.FileTransferType;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.Ft;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.RcsMessage;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.ChatRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.FtRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.RcsMsgRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.RootRecord;
import com.mediatek.rcs.message.cloudbackup.utils.FileUtils;
import com.mediatek.rcs.message.utils.RcsMessageUtils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.io.File;

class FtMsgDecomposer implements IinitFileName {
    private static final String CLASS_TAG = CloudBrUtils.MODULE_TAG + "FtMsgDecomposer";
    private ContentResolver mContentResolver;
    private FtRecord mFtRecord;
    private ChatRecord mChatRecord;
    private RootRecord mRootRecord;
    // private FileObject mFileObject;
    private RcsMsgRecord mRcsMsgRecord;
    private boolean mCancel = false;
    private FtMsgRestoreParser mFtMsgRestoreParser;
    private Context mContext;

    FtMsgDecomposer(Context context) {
        mContext = context;
        mContentResolver = mContext.getContentResolver();
        mFtMsgRestoreParser = new FtMsgRestoreParser(this);
    }

    protected int parseFtMsg(File file) {
        if (mFtMsgRestoreParser == null) {
            Log.e(CLASS_TAG, "parseFtMsg mFtMsgRestoreParser == null,return");
            return CloudBrUtils.ResultCode.OTHER_EXCEPTION;
        }

        if (file == null || !file.exists()) {
            Log.e(CLASS_TAG, "parseFtMsg file = null or file is not exists, return");
            return CloudBrUtils.ResultCode.OTHER_EXCEPTION;
        }
        Log.d(CLASS_TAG, "parseFtMsg file name = " + file.getName());
        initParseRecord();

        int result = mFtMsgRestoreParser.parseFtMsg(file);
        if (result != CloudBrUtils.ResultCode.OK) {
            Log.e(CLASS_TAG, "mFtMsgRestoreParser.parseFtMsgImple result is not ok, return");
            return result;
        }
        Log.d(CLASS_TAG, "mFtMsgRestoreParser end, result = ok");

        result = restructureRecord();
        if (result != CloudBrUtils.ResultCode.OK) {
            Log.e(CLASS_TAG, "restructureRecord result is not ok, return");
            return result;
        }
        Log.d(CLASS_TAG, "restructureRecord end, result = ok");

        if (mCancel) {
            Log.d(CLASS_TAG, "parseFtMsg service canceled");
            result = CloudBrUtils.ResultCode.SERVICE_CANCELED;
        }

        result = insertDataBase();
        if (result != CloudBrUtils.ResultCode.OK) {
            Log.e(CLASS_TAG, "insertDataBase result is not ok, return");
            return result;
        }
        Log.d(CLASS_TAG, "insertDataBase end, result = ok");

        if (mCancel) {
            Log.d(CLASS_TAG, "parseFtMsg service canceled");
            result = CloudBrUtils.ResultCode.SERVICE_CANCELED;
        }
        return result;
    }

    private void initParseRecord() {
        mChatRecord = null;
        mRootRecord = null;
        mFtRecord = null;
        mRcsMsgRecord = null;

        mChatRecord = new ChatRecord();
        mRootRecord = new RootRecord();
        mFtRecord = new FtRecord();
        mRcsMsgRecord = new RcsMsgRecord();

        mFtMsgRestoreParser.setChatRecord(mChatRecord);
        mFtMsgRestoreParser.setFtRecord(mFtRecord);
        mFtMsgRestoreParser.setRootRecord(mRootRecord);
        mFtMsgRestoreParser.setRcsMsgRecord(mRcsMsgRecord);
    }

    protected int insertDataBase() {
        String fileTransferType = mRootRecord.getSessionType();
        if (fileTransferType.equals(FileTransferType.GROUP)) {
            // detected whether chat info exist in chat table ,
            // if exited, then skip insert chat info to chat db,
            // and groupMumber info not need to insert table.
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

                result = FileUtils.insertgroupNumberRecord(mChatRecord, mRootRecord,
                        mContentResolver);
                if (result != CloudBrUtils.ResultCode.OK) {
                    Log.e(CLASS_TAG, "insertgroupNumberRecord error");
                    return result;
                }
            }
        }

        ContentValues ftCv = new ContentValues();
        combineFtRecord(ftCv);
        ContentValues rcsCv = new ContentValues();
        combineRcsMsgRecord(rcsCv);

        Uri uri = mContentResolver.insert(CloudBrUtils.FT_URI, ftCv);
        long id = ContentUris.parseId(uri);
        Log.d(CLASS_TAG, "ft id = " + id + " insert db successs");

        rcsCv.put(RcsMessage.MESSAGE_COLUMN_IPMSG_ID, id);
        rcsCv.put(RcsMessage.MESSAGE_COLUMN_MESSAGE_ID, id);
        Uri rcsMsgUri = mContentResolver.insert(CloudBrUtils.RCS_URI, rcsCv);
        Log.d(CLASS_TAG, "rcsmessageUri = " + rcsMsgUri);
        return CloudBrUtils.ResultCode.OK;
    }

    private void combineRcsMsgRecord(ContentValues rcsMsgCv) {
        rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_BODY, mRcsMsgRecord.getBody());
        rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_CHAT_ID, mRcsMsgRecord.getChatId());
        rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_CONTACT_NUMBER, mRcsMsgRecord.getContactNum());
        rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_CONVERSATION, mRcsMsgRecord.getConversation());
        rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_DATE_SENT, mRcsMsgRecord.getDataSent());
        rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_DIRECTION, mRcsMsgRecord.getDirection());
        rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_FLAG, mRcsMsgRecord.getFlag());
        rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_ISBLOCKED, mRcsMsgRecord.getIsBlocked());
        rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_LOCKED, mRcsMsgRecord.getLocked());
        rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_MESSAGE_STATUS, mRcsMsgRecord.getStatus());
        rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_MIME_TYPE, mRcsMsgRecord.getMimeType());
        rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_MSG_CLASS, mRcsMsgRecord.getMsgClass());
        rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_SEEN, mRcsMsgRecord.getSeen());
        rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_SUB_ID, mRcsMsgRecord.getSubID());
        rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_TIMESTAMP, mRcsMsgRecord.getTimestamp());
        rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_TYPE, mRcsMsgRecord.getType());
        rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_FILE_PATH, mRcsMsgRecord.getFilePath());
    }

    private void combineFtRecord(ContentValues ftCv) {
        ftCv.put(Ft.CHAT_ID, mFtRecord.getChatId());
        ftCv.put(Ft.CONTACT_NUMBER, mFtRecord.getContactNumber());
        ftCv.put(Ft.DIRECTION, mFtRecord.getDirection());
        ftCv.put(Ft.DURATION, mFtRecord.getDuration());
        ftCv.put(Ft.FILENAME, mFtRecord.getFileName());
        ftCv.put(Ft.FILESIZE, mFtRecord.getSize());
        ftCv.put(Ft.FT_ID, mFtRecord.getFtId());
        ftCv.put(Ft.MIME_TYPE, mFtRecord.geMimeType());
        ftCv.put(Ft.SESSION_TYPE, mFtRecord.getSessionType());
        ftCv.put(Ft.STATE, mFtRecord.getStatus());
        ftCv.put(Ft.TIMESTAMP, mFtRecord.getTimestamp());
        ftCv.put(Ft.TIMESTAMP_DELIVERED, mFtRecord.getDisplayedTimestamp());
        ftCv.put(Ft.TIMESTAMP_DISPLAYED, mFtRecord.getDisplayedTimestamp());
        ftCv.put(Ft.TIMESTAMP_SENT, mFtRecord.getSendTimestamp());
    }

    protected int restructureRecord() {
        String fileTransferType = mRootRecord.getSessionType();
        String participants = mRootRecord.getParticipants();
        long threadId = 0;
        Log.d(CLASS_TAG, "fileTransferType = " + fileTransferType);
        Log.d(CLASS_TAG, "participants = " + participants);
        if (fileTransferType == null || participants == null) {
            Log.e(CLASS_TAG, "fileTransferType = null error, return");
            return CloudBrUtils.ResultCode.BACKUP_FILE_ERROR;
        }

        if (fileTransferType.equals(FileTransferType.GROUP)) {
            // if this msg not chat id session, we put rejoin id.
            // the chat id done't exit if the msg is not generated by mtk ,
            // chatid
            // must fill.
            if (mChatRecord.getChatId() == null) {
                mChatRecord.setChatId(mChatRecord.getRejoinId());
            }
            mFtRecord.setChatId(mChatRecord.getChatId());
            mRcsMsgRecord.setChatId(mChatRecord.getChatId());
            mChatRecord.setParticipants(participants);
            if (!mRcsMsgRecord.getFrom().equals(CloudBrUtils.getMyNumber())) {
                mFtRecord.setContactNumber(mRcsMsgRecord.getFrom());
                mRcsMsgRecord.setContactNum(mRcsMsgRecord.getFrom());
            } else {
                mFtRecord.setContactNumber(mChatRecord.getRejoinId());
                mRcsMsgRecord.setContactNum(null);
            }
            threadId = FileUtils.createGroupThread(mContext, mChatRecord);
            Log.d(CLASS_TAG, "insertDataBase createGroupThread = " + threadId);
            mChatRecord.setThreadId(threadId);
            mRcsMsgRecord.setConversation((int) threadId);
        } else if (fileTransferType.equals(FileTransferType.ONE_TO_MANY)) {
            mFtRecord.setContactNumber(participants);
            mRcsMsgRecord.setContactNum(participants);
            mFtRecord.setChatId(participants);
            mRcsMsgRecord.setChatId(participants);
            threadId = FileUtils.getOrCreate1TNThreadId(mContext, participants);
            Log.d(CLASS_TAG, "insertDataBase create1TM = " + threadId);
            mRcsMsgRecord.setConversation((int) threadId);
        } else if (fileTransferType.equals(CloudBrUtils.FileTransferType.ONE_TO_ONE)) {
            String contact = null;
            if (mRcsMsgRecord.getFrom().equals(CloudBrUtils.getMyNumber())) {
                mFtRecord.setContactNumber(participants);
                mRcsMsgRecord.setContactNum(participants);
                mFtRecord.setChatId(participants);
                mRcsMsgRecord.setChatId(participants);
                contact = participants;
            } else {
                mFtRecord.setContactNumber(mRcsMsgRecord.getFrom());
                mRcsMsgRecord.setContactNum(mRcsMsgRecord.getFrom());
                mFtRecord.setChatId(mRcsMsgRecord.getFrom());
                mRcsMsgRecord.setChatId(mRcsMsgRecord.getFrom());
                contact = mRcsMsgRecord.getFrom();
            }
            threadId = FileUtils.getOrCreate1TNThreadId(mContext, contact);
            Log.d(CLASS_TAG, "insertDataBase create1TN FT = " + threadId);
            mRcsMsgRecord.setConversation((int) threadId);
        } else {
            Log.d(CLASS_TAG, "no such file transfer type error , return");
            return CloudBrUtils.ResultCode.BACKUP_FILE_ERROR;
        }
        String rcsFileName = RCSUtils.getSaveBody(false, mFtRecord.getFileName());
        Log.d(CLASS_TAG, "rcsFileName = " + rcsFileName);
        mRcsMsgRecord.setFilePath(rcsFileName);
        mRcsMsgRecord.setMimeType(mFtRecord.geMimeType());
        mRcsMsgRecord.setBody(mFtRecord.getFtId());
        mRcsMsgRecord.setIsBlocked(0);//0 is normal msg, 1 is spam msg
        mRcsMsgRecord.setState(1);//0 is not delivered, 1 deliverd

        mFtRecord.setDirection(mRcsMsgRecord.getDirection());
        mFtRecord.setTimestamp(mRcsMsgRecord.getTimestamp());
        return CloudBrUtils.ResultCode.OK;
    }

    @Override
    public String initFtFilePath(String name) {
        int direction = mFtRecord.getDirection();
        String fileName = null;
        if (direction == Ft.Direction.INCOMING) {
            File folder = new File(FileUtils.ModulePath.FT_FILE_PATHRECEIVE);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            fileName = FileUtils.ModulePath.FT_FILE_PATHRECEIVE + name;
        } else {
            File sendFolder = new File(FileUtils.ModulePath.FT_FILE_PATHSEND);
            if (!sendFolder.exists()) {
                sendFolder.mkdirs();
            }
            fileName = FileUtils.ModulePath.FT_FILE_PATHSEND + name;
        }
        Log.d(CLASS_TAG, "ftCv put file name = " + fileName);

        String fileNameNoExt = null;
        String fileExt = null;
        if (name != null) {
            fileNameNoExt = name.substring(0, name.indexOf(".") - 1);
            fileExt = name.substring(name.indexOf(".") + 1);
        }
        Log.d(CLASS_TAG, "fileNameNoExt = " + fileNameNoExt);
        Log.d(CLASS_TAG, "fileExt = " + fileExt);
        for (int i = 0; i < 1000; i++) {
            File file = new File(fileName);
            if (file.exists()) {
                Log.d(CLASS_TAG, "initFtFilePath fileName exit, rename");
                fileName = FileUtils.ModulePath.FT_FILE_PATHRECEIVE + fileNameNoExt + "(" + i + ")"
                        + "." + fileExt;
            } else {
                Log.d(CLASS_TAG, "fileName = " + fileName);
                break;
            }
        }

        mFtRecord.setFileName(fileName);
        return fileName;
    }

    /**
     * This method will be called when backup service be cancel.
     *
     * @param cancel
     */
    void setCancel(boolean cancel) {
        mCancel = cancel;
    }

//    private String getFilePath(String mimeType) {
//        String path;
//        if (mimeType != null) {
//            if (mimeType.contains(RCSUtils.FILE_TYPE_IMAGE)) {
//                path = RcsMessageUtils.getPicTempPath(mContext);
//            } else if (mimeType.contains(RCSUtils.FILE_TYPE_AUDIO)) {
//                path = RcsMessageUtils.getAudioTempPath(mContext);
//            } else if (mimeType.contains(RCSUtils.FILE_TYPE_VIDEO)) {
//                path = RcsMessageUtils.getVideoTempPath(mContext);
//            } else if (mimeType.contains("vcard")) {
//                path = RcsMessageUtils.getVcardTempPath(mContext);
//            } else if (mimeType.contains("location")) {
//                path = RcsMessageUtils.getGeolocTempPath(mContext);
//            } else {
//                path = RcsMessageUtils.getPicTempPath(mContext);//others type put picture path
//            }
//        }
//        return path;
//    }
}
