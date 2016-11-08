package com.mediatek.rcs.message.cloudbackup.modules;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.mediatek.rcs.common.RcsLog;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.RcsMessage;
import com.mediatek.rcs.message.cloudbackup.utils.FileUtils;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.BackupConstant;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.Ft;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.GroupChatMember;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.ResultCode;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.GroupChat;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.ChatRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.FileObject;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.FtRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.GroupNumberRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.RcsMsgRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.RootRecord;

import android.content.ContentResolver;
import android.database.Cursor;
import android.util.Log;

class FtMsgComposer {
    private static final String CLASS_TAG = CloudBrUtils.MODULE_TAG + "FtMsgComposer";
    private String mIpBackupFolder;
    private ContentResolver mContentResolver;
    private HashMap<String, ArrayList<Integer>> mGroupNumberMap;
    private HashMap<String, ArrayList<Integer>> mFtGroupMap;
    private ArrayList<Integer> mFt1ToManyList;
    private ArrayList<Integer> mFt1To1List;
    private boolean mCancel = false;
    private FtMsgBackupBuilder mFtMsgBackupBuilder;

    FtMsgComposer(String filePath, ContentResolver contentResolver) {
        mIpBackupFolder = filePath;
        mContentResolver = contentResolver;
        mFtMsgBackupBuilder = new FtMsgBackupBuilder(mIpBackupFolder);
    }

    /**
     * This method will be called when backup service be cancel.
     * @param cancel
     */
    protected void setCancel(boolean cancel) {
        mCancel = cancel;
    }

    protected void setBackupParam(ArrayList<Integer> ft1ToManyList,
            HashMap<String, ArrayList<Integer>> ftGroupMap,
            HashMap<String, ArrayList<Integer>> groupNumberMap,
            ArrayList<Integer> ft1To1List) {
        mFt1ToManyList = ft1ToManyList;
        mFtGroupMap = ftGroupMap;
        mGroupNumberMap = groupNumberMap;
        mFt1To1List = ft1To1List;
    }

    protected int backupFtMsg() {
        Log.d(CLASS_TAG, "composerFtMsg begin");
        int result = backupGroupFtMsg();
        if (result != ResultCode.OK) {
            Log.d(CLASS_TAG, "backupGroupFtMsg happen exception return.");
            return result;
        }

        Log.d(CLASS_TAG, "backup1To1FtMsg begin");
        result = backup1To1FtMsg();
        if (result != ResultCode.OK) {
            Log.d(CLASS_TAG, "backup1ToManyMsg happen exception return.");
            return result;
        }

        result = backup1ToManyFtMsg();
        Log.d(CLASS_TAG, "backup1ToManyMsg begin");
        if (result != ResultCode.OK) {
            Log.d(CLASS_TAG, "backup1ToManyMsg happen exception return.");
            return result;
        }

        if (mCancel) {
            Log.d(CLASS_TAG, "backupFtMsg() service canceled");
            result = CloudBrUtils.ResultCode.SERVICE_CANCELED;
        }
        return ResultCode.OK;
    }

    private int backup1To1FtMsg() {
        Log.d(CLASS_TAG, "backup1To1FtMsg begin");
        Log.d(CLASS_TAG, "backup1To1FtMsg mFt1To1List size = " + mFt1To1List.size());
        int result = ResultCode.OK;
        if (mFt1To1List == null || mFt1To1List.size() <= 0) {
            Log.d(CLASS_TAG,
                    "mFt1To1List == null or mFt1To1List.size() <= 0 return ResultCode.OK");
            return result;
        }

        for (int ftId : mFt1To1List) {
            if (mCancel) {
                Log.d(CLASS_TAG, "backup1To1FtMsg() service canceled");
                return CloudBrUtils.ResultCode.SERVICE_CANCELED;
            }

            String rcsMsgSel = CloudBrUtils.ID + " = " + ftId;
            Log.d(CLASS_TAG, "backup 1 to 1 ft msg rcsMsgSel = " + rcsMsgSel);
            Cursor rcsMsgCursor = mContentResolver.query(CloudBrUtils.RCS_URI, null,
                    rcsMsgSel, null, null);
            rcsMsgCursor.moveToFirst();
            RcsMsgRecord rcsMsgRecord = null;
            int ipMsgId = rcsMsgCursor.getInt(rcsMsgCursor.getColumnIndex
                    (CloudBrUtils.RcsMessage.MESSAGE_COLUMN_IPMSG_ID));
            Cursor ftCursor = null;

            String ftSelec = CloudBrUtils.ID + " = " + ipMsgId;
            Log.d(CLASS_TAG, "backup ft msg ftSelec = " + ftSelec);
            ftCursor = mContentResolver
                    .query(CloudBrUtils.FT_URI, null, ftSelec, null, null);
            if (ftCursor == null || ftCursor.getCount() <= 0) {
                Log.d(CLASS_TAG, "ft table havnt this msg record , ftSelec = " + ftSelec);
                continue;
            }
            rcsMsgCursor.moveToFirst();
            ftCursor.moveToFirst();

            if (!CloudBrUtils.isMsgNeedBackup(rcsMsgCursor)) {
                Log.d(CLASS_TAG, "backup1To1FtMsg this msg need not backup");
                if (rcsMsgCursor != null) {
                    rcsMsgCursor.close();
                }
                if (ftCursor != null) {
                    ftCursor.close();
                }
                continue;
            }

            String type = ftCursor.getString(ftCursor.getColumnIndex(Ft.MIME_TYPE));
            String filePath = ftCursor.getString(ftCursor.getColumnIndex(Ft.FILENAME));
            File ftFile = new File(filePath);
            if (ftFile == null || !ftFile.exists()) {
                Log.e(CLASS_TAG, "1 to 1 filePath = " + filePath + "is not exited, skip bakup");
                if (rcsMsgCursor != null) {
                    rcsMsgCursor.close();
                }
                if (ftCursor != null) {
                    ftCursor.close();
                }
                continue;
            }
            rcsMsgRecord = new RcsMsgRecord();
            FtRecord ftRecord = new FtRecord();
            RootRecord rootRecord = new RootRecord();
            FileObject fileObjectRecord = new FileObject();

            getFileObjectInfo(ftCursor, fileObjectRecord);
            FileUtils.getRcsMessageInfo(rcsMsgCursor, rcsMsgRecord);
            getFtInfo(ftCursor, ftRecord);

            String contactNumber = rcsMsgRecord.getContactNum();
            String from = null;
            String to = null;
            int direction = rcsMsgRecord.getDirection();
            if (direction == Ft.Direction.OUTGOING) {
                from = CloudBrUtils.getMyNumber();
                to = contactNumber;
            } else if (direction == Ft.Direction.INCOMING) {
                from = contactNumber;
                to = CloudBrUtils.getMyNumber();
            }
            rcsMsgRecord.setFrom(from);
            rootRecord.setParticipants(to);
            rootRecord.setSessionType(CloudBrUtils.FileTransferType.ONE_TO_ONE);

            String header = mFtMsgBackupBuilder.build1ToNHeader(ftRecord, rcsMsgRecord);
            String fileObject = mFtMsgBackupBuilder.buildFileObject(fileObjectRecord);
            String root = mFtMsgBackupBuilder.buildRootRecord(rootRecord);

            if (ftCursor != null) {
                ftCursor.close();
                ftCursor = null;
            }
            if (rcsMsgCursor != null) {
                rcsMsgCursor.close();
                rcsMsgCursor = null;
            }

            Writer chatWriter = null;
            File file = new File(mIpBackupFolder + File.separator + ftId + "ft.txt");
            OutputStream stream = null;
            try {
                file.createNewFile();
                stream = new FileOutputStream(file);
                chatWriter = new BufferedWriter(new OutputStreamWriter(stream));
                chatWriter.write(header);
                chatWriter.write(root);
                chatWriter.write(BackupConstant.LINE_BREAK);
                chatWriter.write(fileObject);
            } catch (IOException e) {
                Log.e(CLASS_TAG, "backup1To1FtMsg create file fail ft id = " + ftId);
                e.printStackTrace();
                return ResultCode.IO_EXCEPTION;
            }

            result = mFtMsgBackupBuilder.addMsgBody(chatWriter, stream, type, filePath);
            if (result != ResultCode.OK) {
                Log.d(CLASS_TAG, "backup1To1FtMsg copy ft id = " + ftId + "happen error."
                        + " copyResult = " + result + "return");
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
            Log.d(CLASS_TAG, "backup end ftId " + ftId);
        }
        return ResultCode.OK;

    }

    private int backup1ToManyFtMsg() {
        Log.d(CLASS_TAG, "backup1ToManyFtMsg begin");
        Log.d(CLASS_TAG, "backup1ToManyFtMsg mFt1ToManyList size = " + mFt1ToManyList.size());
        int result = ResultCode.OK;
        if (mFt1ToManyList == null || mFt1ToManyList.size() <= 0) {
            Log.d(CLASS_TAG,
                    "mFt1ToManyList == null or mFt1ToManyList.size() <= 0 return ResultCode.OK");
            return result;
        }

        for (int ftId : mFt1ToManyList) {
            if (mCancel) {
                Log.d(CLASS_TAG, "backup1ToManyFtMsg() service canceled");
                return CloudBrUtils.ResultCode.SERVICE_CANCELED;
            }

            String rcsMsgSel = CloudBrUtils.ID + " = " + ftId;
            Log.d(CLASS_TAG, "backup 1 to muti ft msg rcsMsgSel = " + rcsMsgSel);
            Cursor rcsMsgCursor = mContentResolver.query(CloudBrUtils.RCS_URI, null,
                    rcsMsgSel, null, null);
            rcsMsgCursor.moveToFirst();
            RcsMsgRecord rcsMsgRecord = null;
            int ipMsgId = rcsMsgCursor.getInt(rcsMsgCursor.getColumnIndex
                    (CloudBrUtils.RcsMessage.MESSAGE_COLUMN_IPMSG_ID));
            Cursor ftCursor = null;
            String ftSelec = CloudBrUtils.ID + " = " + ipMsgId;
            Log.d(CLASS_TAG, "backup1ToManyFtMsg ft msg ftSelec = " + ftSelec);
            ftCursor = mContentResolver
                    .query(CloudBrUtils.FT_URI, null, ftSelec, null, null);
            if (ftCursor == null || ftCursor.getCount() <= 0) {
                Log.d(CLASS_TAG, "backup1ToManyFtMsg this msg record , ftSelec = " + ftSelec);
                continue;
            }
            rcsMsgCursor.moveToFirst();
            ftCursor.moveToFirst();

            if (!CloudBrUtils.isMsgNeedBackup(rcsMsgCursor)) {
                Log.d(CLASS_TAG, "backup1ToManyFtMsg this msg need not backup");
                if (rcsMsgCursor != null) {
                    rcsMsgCursor.close();
                }
                if (ftCursor != null) {
                    ftCursor.close();
                }
                continue;
            }

            String type = ftCursor.getString(ftCursor.getColumnIndex(Ft.MIME_TYPE));
            String filePath = ftCursor.getString(ftCursor.getColumnIndex(Ft.FILENAME));
            File ftFile = new File(filePath);
            if (ftFile == null || !ftFile.exists()) {
                Log.e(CLASS_TAG, "1 to m filePath = " + filePath + "is not exited, skip bakup");
                if (rcsMsgCursor != null) {
                    rcsMsgCursor.close();
                }
                if (ftCursor != null) {
                    ftCursor.close();
                }
                continue;
            }

            rcsMsgRecord = new RcsMsgRecord();
            FtRecord ftRecord = new FtRecord();
            RootRecord rootRecord = new RootRecord();
            FileObject fileObjectRecord = new FileObject();
            getFileObjectInfo(ftCursor, fileObjectRecord);
            FileUtils.getRcsMessageInfo(rcsMsgCursor, rcsMsgRecord);
            getFtInfo(ftCursor, ftRecord);

            rcsMsgRecord.setFrom(CloudBrUtils.getMyNumber());
            rootRecord.setParticipants(rcsMsgRecord.getContactNum());
            rootRecord.setSessionType(CloudBrUtils.FileTransferType.ONE_TO_MANY);

            String header = mFtMsgBackupBuilder.build1ToNHeader(ftRecord, rcsMsgRecord);
            String fileObject = mFtMsgBackupBuilder.buildFileObject(fileObjectRecord);
            String root = mFtMsgBackupBuilder.buildRootRecord(rootRecord);
            if (ftCursor != null) {
                ftCursor.close();
                ftCursor = null;
            }
            if (rcsMsgCursor != null) {
                rcsMsgCursor.close();
                rcsMsgCursor = null;
            }

            Writer chatWriter = null;
            File file = new File(mIpBackupFolder + File.separator + ftId + "ft.txt");
            OutputStream stream = null;
            try {
                file.createNewFile();
                stream = new FileOutputStream(file);
                chatWriter = new BufferedWriter(new OutputStreamWriter(stream));
                chatWriter.write(header);
                chatWriter.write(root);
                chatWriter.write(BackupConstant.LINE_BREAK);
                chatWriter.write(fileObject);
            } catch (IOException e) {
                Log.e(CLASS_TAG, "backup1ToManyFtMsg create file fail ft id = " + ftId);
                e.printStackTrace();
                return ResultCode.IO_EXCEPTION;
            }
            result = mFtMsgBackupBuilder.addMsgBody(chatWriter, stream, type, filePath);

            if (result != ResultCode.OK) {
                Log.d(CLASS_TAG, "backup1ToManyFtMsg copy ft id = " + ftId + "happen error."
                        + " copyResult = " + result + "return");
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
            Log.d(CLASS_TAG, "backup end ftId " + ftId);
        }
        return ResultCode.OK;
    }

    private int backupGroupFtMsg() {
        Iterator it = mFtGroupMap.keySet().iterator();
        int result = ResultCode.OK;
        while (it.hasNext()) {
            if (mCancel) {
                Log.d(CLASS_TAG, "backupGroupFtMsg() service canceled");
                return CloudBrUtils.ResultCode.SERVICE_CANCELED;
            }

            String chatId = (String) it.next();
            Log.d(CLASS_TAG, "backupGroupFtMsg chatId = " + chatId);
            ArrayList<Integer> idFtList = mFtGroupMap.get(chatId);
            result = backupGroupFtMsgImpl(chatId, idFtList);
            if (result != ResultCode.OK) {
                Log.d(CLASS_TAG, "backupGroupFtMsg backupGroupFtMsgImpl" + " error chatId = "
                        + chatId + " result = " + result);
                return result;
            }
        }
        Log.d(CLASS_TAG, "backupGroupFtMsg end");
        return 0;
    }

    private final String[] chatProjection = new String[] { CloudBrUtils.Chat.CHAIRMAN,
            CloudBrUtils.Chat.TIMESTAMP, CloudBrUtils.Chat.SUBJECT,
            CloudBrUtils.Chat.CONVERSATION_ID, CloudBrUtils.Chat.PARTICIPANTS_LIST,
            CloudBrUtils.Chat.REJOIN_ID, CloudBrUtils.Chat.STATE, CloudBrUtils.CHAT_ID };

    private int backupGroupFtMsgImpl(String chatId, ArrayList<Integer> idFtList) {
        String selection = CloudBrUtils.CHAT_ID + " is \"" + chatId + "\"";
        Log.d(CLASS_TAG, "selection = " + selection);
        Cursor threadMapCursor = mContentResolver.query(CloudBrUtils.GROUP_CHAT_URI, null,
                selection, null, null);
        if (threadMapCursor == null || threadMapCursor.getCount() == 0) {
            Log.e(CLASS_TAG, "threadMapCursor is null or count is 0, skip this chat return ok, " +
                    "treadmap no this chat id info, no data chat id = " + chatId);
            return CloudBrUtils.ResultCode.OK;
        }
        threadMapCursor.moveToFirst();
        int localChatStatus = threadMapCursor.getInt(threadMapCursor.getColumnIndex(
                GroupChat.STATUS));
        if (threadMapCursor != null) {
            threadMapCursor.close();
        }

        Cursor chatCursor = mContentResolver.query(CloudBrUtils.CHAT_CHAT_URI, chatProjection,
                selection, null, null);
        if (chatCursor == null) {
            Log.e(CLASS_TAG, "backupGroupFtMsgImpl chat table no such chat id data, error");
            Log.e(CLASS_TAG, "backupGroupFtMsgImpl CloudBrUtils.ResultCode.DB_EXCEPTION");
            return CloudBrUtils.ResultCode.DB_EXCEPTION;
        }
        chatCursor.moveToFirst();

        ChatRecord chatRecord = new ChatRecord();
        RootRecord rootRecord = new RootRecord();
        int result = EntryRecord.getChatInfo(chatCursor, chatRecord);
        chatRecord.setThreadMapStatus(localChatStatus);

        String rejoinId = chatRecord.getRejoinId();// ft table set rejoinId if
                                                   // msg is send by myself.
        // get root info
        String participants = chatCursor.getString(chatCursor
                .getColumnIndex(CloudBrUtils.Chat.PARTICIPANTS_LIST));
        if (chatCursor != null) {
            chatCursor.close();
        }

        ArrayList<Integer> numberIdList = mGroupNumberMap.get(chatId);
        EntryRecord.getRootNumbersInfo(mContentResolver, numberIdList, rootRecord);
        rootRecord.setParticipants(participants);
        rootRecord.setSessionType(CloudBrUtils.FileTransferType.GROUP);
        String rootInfo = mFtMsgBackupBuilder.buildRootRecord(rootRecord);

        for (int id : idFtList) {
            if (mCancel) {
                Log.d(CLASS_TAG, "backupData() service canceled");
                return CloudBrUtils.ResultCode.SERVICE_CANCELED;
            }

            String rcsMsgSel = CloudBrUtils.ID + " = " + id;
            Log.d(CLASS_TAG, "backupOneftMsg selection = " + selection);
            Cursor rcsMsgCursor = mContentResolver.query(CloudBrUtils.RCS_URI, null,
                    rcsMsgSel, null, null);
            rcsMsgCursor.moveToFirst();
            RcsMsgRecord rcsMsgRecord = null;

            int ipMsgId = rcsMsgCursor.getInt(rcsMsgCursor.getColumnIndex
                    (CloudBrUtils.RcsMessage.MESSAGE_COLUMN_IPMSG_ID));
            Cursor ftCursor = null;

            String ftSelec = CloudBrUtils.ID + " = " + ipMsgId;
            Log.d(CLASS_TAG, "backup ft msg ftSelec = " + ftSelec);

            ftCursor = mContentResolver
                    .query(CloudBrUtils.FT_URI, null, ftSelec, null, null);

            if (ftCursor == null || ftCursor.getCount() <= 0) {
                Log.d(CLASS_TAG, "ft table havnt this msg record , ftSelec = " + ftSelec);
                continue;
            }
            rcsMsgCursor.moveToFirst();
            ftCursor.moveToFirst();
            if (!CloudBrUtils.isMsgNeedBackup(rcsMsgCursor)) {
                Log.d(CLASS_TAG, "this msg need not backup");
                if (rcsMsgCursor != null) {
                    rcsMsgCursor.close();
                }
                if (ftCursor != null) {
                    ftCursor.close();
                }
                continue;
            }

            String type = ftCursor.getString(ftCursor.getColumnIndex(Ft.MIME_TYPE));
            String filePath = ftCursor.getString(ftCursor.getColumnIndex(Ft.FILENAME));
            File ftFile = new File(filePath);
            if (ftFile == null || !ftFile.exists()) {
                Log.e(CLASS_TAG, "group ft filePath = " + filePath + "is not exited, skip bakup");
                if (rcsMsgCursor != null) {
                    rcsMsgCursor.close();
                }
                if (ftCursor != null) {
                    ftCursor.close();
                }
                continue;
            }
            rcsMsgRecord = new RcsMsgRecord();
            FtRecord ftRecord = new FtRecord();
            FileObject fileObjectRecord = new FileObject();

            getFileObjectInfo(ftCursor, fileObjectRecord);
            FileUtils.getRcsMessageInfo(rcsMsgCursor, rcsMsgRecord);
            getFtInfo(ftCursor, ftRecord);

            String contactNum = rcsMsgRecord.getContactNum();
            String from = null;
            int direction = rcsMsgRecord.getDirection();
            if (direction == RcsLog.Direction.OUTGOING) {
                from = CloudBrUtils.getMyNumber();
            } else if (direction == Ft.Direction.INCOMING) {
                from = contactNum;
            }
            rcsMsgRecord.setFrom(from);

            if (ftCursor != null) {
                ftCursor.close();
                ftCursor = null;
            }
            if (rcsMsgCursor != null) {
                rcsMsgCursor.close();
                rcsMsgCursor = null;
            }

            Writer chatWriter = null;
            OutputStream stream = null;
            File file = new File(mIpBackupFolder + File.separator + id + "ft.txt");
            String header = mFtMsgBackupBuilder.buildGroupHeader(chatRecord, ftRecord,
                                                            rcsMsgRecord);
            String fileObject = mFtMsgBackupBuilder.buildFileObject(fileObjectRecord);
            try {
                stream = new FileOutputStream(file);
                chatWriter = new BufferedWriter(new OutputStreamWriter(stream));
                chatWriter.write(header);
                chatWriter.write(rootInfo);
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
            rcsMsgRecord = null;
            ftRecord = null;
            fileObjectRecord = null;
        }
        return ResultCode.OK;
    }

    private int getFileObjectInfo(Cursor ftCursor, FileObject fileObject) {
        Log.d(CLASS_TAG, "composeFileObject begin");

        String filePath = ftCursor.getString(ftCursor.getColumnIndex(Ft.FILENAME));
        String fileName = filePath.substring(filePath.lastIndexOf(File.separator) + 1);
        fileObject.setName(fileName);

        fileObject.setCid(ftCursor.getString(ftCursor.getColumnIndex(Ft.FT_ID)));
        fileObject.setType(ftCursor.getString(ftCursor.getColumnIndex(Ft.MIME_TYPE)));
        String size = ftCursor.getString(ftCursor.getColumnIndex(Ft.FILESIZE));
        if (size != null) {
            fileObject.setSize(Long.parseLong(size));
        }

        fileObject.setDate(ftCursor.getLong(ftCursor.getColumnIndex(Ft.TIMESTAMP)));
        return CloudBrUtils.ResultCode.OK;
    }

    private int getFtInfo(Cursor ftCs, FtRecord ftRecord) {
        ftRecord.setMsgId(ftCs.getString(ftCs.getColumnIndex(Ft.MSG_ID)));
        ftRecord.setStatus(ftCs.getInt(ftCs.getColumnIndex(Ft.STATE)));
        ftRecord.setSessionType(ftCs.getInt(ftCs.getColumnIndex(Ft.SESSION_TYPE)));
        ftRecord.setFtId(ftCs.getString(ftCs.getColumnIndex(Ft.FT_ID)));
        ftRecord.setDuration(ftCs.getLong(ftCs.getColumnIndex(Ft.DURATION)));

        return CloudBrUtils.ResultCode.OK;
    }
}
