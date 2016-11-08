package com.mediatek.rcs.message.cloudbackup.modules;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.mediatek.rcs.common.service.IMsgRestoreListener;
import com.mediatek.rcs.common.service.IRcsMessageRestoreService;
import com.mediatek.rcs.message.cloudbackup.modules.Chat1To1Decomposer;
import com.mediatek.rcs.message.cloudbackup.modules.ChatGroupDecomposer;
import com.mediatek.rcs.message.cloudbackup.modules.FtMsgDecomposer;
import com.mediatek.rcs.message.cloudbackup.utils.FileUtils;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.BackupConstant;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.BackupDataFileType;

import android.app.Service;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

/**
 * MessageRestoreComposer
 */
public class MessageRestoreComposer {
    private static final String CLASS_TAG = CloudBrUtils.MODULE_TAG + "MessageRestoreComposer";

    private Context mContext;
    private ContentResolver mContentResolver;
    private ChatGroupDecomposer mChatGroupDecomposer;
    private FtMsgDecomposer mFtMsgDecomposer;
    private Chat1To1Decomposer mChat1To1Decomposer;
    private boolean mCancel = false;
    private boolean mIsAidlServiceConnected;
    private Object mLock = new Object();
    private File mBackupDataFolder;

    /**
     *
     * @param context
     * @param backupDataFolder
     */
    public MessageRestoreComposer(Context context, File backupDataFolder) {
        mContext = context;
        mBackupDataFolder = backupDataFolder;
    }

    /**
     *
     * @return
     */
    public int restoreData() {
        if (!createContentResolver()) {
            Log.e(CLASS_TAG, "createContentResolver error");
            return CloudBrUtils.ResultCode.BACKUP_FILE_ERROR;
        }
        if (!mBackupDataFolder.exists()) {
            Log.d(CLASS_TAG, "no file exited!");
            return CloudBrUtils.ResultCode.BACKUP_FILE_ERROR;
        }

        Log.d(CLASS_TAG, "begin bind remote service");
        Intent intent = new Intent();
        intent.setClassName("com.mediatek.rcs.messageservice",
                "com.mediatek.rcs.messageservice.RcsMessageRestoreService");
        try {
            mContext.bindService(intent, mConn, Service.BIND_AUTO_CREATE);
        } catch (Exception e) {
            Log.e(CLASS_TAG, "bind RcsMessageRestoreService exception, return");
            mIsAidlServiceConnected = true;
            e.printStackTrace();
            return CloudBrUtils.ResultCode.OTHER_EXCEPTION;
        }
        Log.d(CLASS_TAG, "bind remote service end");

        synchronized (mLock) {
            while (!mIsAidlServiceConnected) {
                Log.d(CLASS_TAG, "wait remote service connected");
                try {
                    mLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        Log.d(CLASS_TAG, "remote service connected begin action");

        Log.d(CLASS_TAG, "begin delete database");
        int result;
        if (clearMessageDb()) {
            Log.d(CLASS_TAG, "restoreData(), clear msg db true");
            result = restoreData(mBackupDataFolder);
        } else {
            Log.d(CLASS_TAG, "restoreData(), clear msg db error, return");
            result = CloudBrUtils.ResultCode.OTHER_EXCEPTION;
        }

        Log.d(CLASS_TAG, "restoreData() end, unbindService");
        try {
            if (mService != null) {
                mService.setListener(null);
            }
            mContext.unbindService(mConn);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

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

    protected int restoreData(File backupDataFolder) {
        Log.d(CLASS_TAG, "restoreData backupDataFolder = " + backupDataFolder.getAbsolutePath());
        int result = CloudBrUtils.ResultCode.OK;
        for (File file : backupDataFolder.listFiles()) {
            if (!file.isDirectory()) {
                Log.d(CLASS_TAG, "file name = " + file.getName());
                result = parserMsgFromFile(file);
                if (result != CloudBrUtils.ResultCode.OK) {
                    return result;
                }
            } else {
                Log.d(CLASS_TAG, "folder name = " + file.getName());
                result = restoreData(file);
                if (result != CloudBrUtils.ResultCode.OK) {
                    return result;
                }
            }
        }
        return CloudBrUtils.ResultCode.OK;
    }

    /**
     * This method will be called when restore service be cancel.
     *
     * @param cancel
     */
    public void setCancel(boolean cancel) {
        mCancel = cancel;
        if (mFtMsgDecomposer != null) {
            mFtMsgDecomposer.setCancel(cancel);
        }

        if (mChatGroupDecomposer != null) {
            mChatGroupDecomposer.setCancel(cancel);
        }
        if (mService != null) {
            try {
                mService.setCancel(true);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        if (mChat1To1Decomposer != null) {
            mChat1To1Decomposer.setCancel(cancel);
        }
    }

    private boolean mIsRestoreEnd = false;
    private int mRemoteRestoreResult = CloudBrUtils.ResultCode.OK;
    /**
     * Get file type(ft/group chat/1-n msg), and call corresponding class to
     * parse the file.
     *
     * @param file
     *            backup file that get from server.
     * @return parse file result.
     */
    protected int parserMsgFromFile(File file) {
        Log.d(CLASS_TAG, "parserMsgFromFile filename  = " + file.getName());
        int result = CloudBrUtils.ResultCode.OK;
        String filePath = file.getAbsolutePath();
        Log.d(CLASS_TAG, "filePath = " + filePath);

        int fileType = FileUtils.anysisFileType(file);
        Log.d(CLASS_TAG, "parserMsgFromFile fileType = " + fileType);
        if (fileType == -1) {
            Log.e(CLASS_TAG, "anysisFileType error");
            return CloudBrUtils.ResultCode.OTHER_EXCEPTION;
        }

        if (fileType == BackupDataFileType.VMSG) {
            Log.d(CLASS_TAG, "parserMsgFromFile is a sms vmsg");

            boolean ret = false;
            if (mService != null) {
                try {
                    mService.setListener(mCallback);
                    ret = mService.restoreSms(file.getAbsolutePath());
                } catch (RemoteException e) {
                      e.printStackTrace();
                }
            }
            if (!ret) {
                Log.d(CLASS_TAG, "mService.restoreSms restore false");
                return CloudBrUtils.ResultCode.BACKUP_FILE_ERROR;
            }

            mIsRestoreEnd = false;
            synchronized (mLock) {
                while (!mIsRestoreEnd) {
                    Log.d(CLASS_TAG, "wait remote service restore sms end");
                    try {
                        mLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            Log.d(CLASS_TAG, "remote service restore sms end, result = " + mRemoteRestoreResult);

            return mRemoteRestoreResult;
        } else if (fileType == BackupDataFileType.MMS_XML) {
            Log.d(CLASS_TAG, "this file is a mms xml, skip");
            return CloudBrUtils.ResultCode.OK;
        } else if (fileType == BackupDataFileType.PDU) {
            Log.d(CLASS_TAG, "parserMsgFromFile is pdu, skip");
            return result;
        }

        Log.d(CLASS_TAG, "parserMsgFromFile is ipmsg");
        InputStream instream = null;
        try {
            instream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return CloudBrUtils.ResultCode.OTHER_EXCEPTION;
        }

        Log.d(CLASS_TAG, "getMsgTypeFromFile file = " + file);
        InputStreamReader inreader = new InputStreamReader(instream);
        BufferedReader buffreader = new BufferedReader(inreader);
        String line = null;
        String content = null;
        try {
            while (((line = buffreader.readLine()) != null)) {
                if (!(line.startsWith("Content-type") || line.startsWith("Content-Type"))) {
                    Log.d(CLASS_TAG, "parserMsgFromFile line = " + line);
                    continue;
                } else if (line.equals(BackupConstant.BOUNDDARY_CPM)) {
                    Log.e(CLASS_TAG, "not found content_type, return error");
                    return CloudBrUtils.ResultCode.BACKUP_FILE_ERROR;
                } else {
                    Log.d(CLASS_TAG, "file " + file.getName() + " type = " + line);
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    break;
                }
            }
            if (buffreader != null) {
                buffreader.close();
            }
            if (content == null) {
                Log.d(CLASS_TAG, "getMsgTypeFromFile not found content_type, error");
                return CloudBrUtils.ResultCode.OTHER_EXCEPTION;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return CloudBrUtils.ResultCode.IO_EXCEPTION;
        }

        if (content.equals(CloudBrUtils.ContentType.GROUP_CHAT_TYPE)) {
            Log.d(CLASS_TAG, "group chat");
            if (mChatGroupDecomposer == null) {
                mChatGroupDecomposer = new ChatGroupDecomposer(mContext);
            }
            return mChatGroupDecomposer.parseGroupMsg(file);
        } else if (content.equals(CloudBrUtils.ContentType.GROUP_FT_TYPE)) {
            Log.d(CLASS_TAG, "ft msg");
            if (mFtMsgDecomposer == null) {
                mFtMsgDecomposer = new FtMsgDecomposer(mContext);
            }
            return mFtMsgDecomposer.parseFtMsg(file);
        } else {
            Log.d(CLASS_TAG, "is a 1 to 1 text msg content = " + content);
            if (mChat1To1Decomposer == null) {
                mChat1To1Decomposer = new Chat1To1Decomposer(mContext);
            }
            return mChat1To1Decomposer.parseOneToOneMsg(file);
        }
    }

    IRcsMessageRestoreService mService;
    private ServiceConnection mConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(final ComponentName name, final IBinder service) {
            Log.d(CLASS_TAG, "onServiceConnected" + name);
            mService = IRcsMessageRestoreService.Stub.asInterface(service);
            synchronized (mLock) {
                mIsAidlServiceConnected = true;
                mLock.notifyAll();
            }
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            Log.d(CLASS_TAG, "onServiceDisconnected" + name);
            mIsAidlServiceConnected = false;
            mService = null;
        }
    };

    private IMsgRestoreListener.Stub mCallback = new IMsgRestoreListener.Stub() {
        @Override
        public void onWorkResult(int result) throws RemoteException {
            Log.d(CLASS_TAG, "onWorkResult result = " + result);
            mRemoteRestoreResult = result;
            synchronized (mLock) {
                mIsRestoreEnd = true;
                mLock.notifyAll();
            }
        }
    };

    private boolean clearMessageDb() {
        ContentResolver contentResolver = mContext.getContentResolver();
        int count = contentResolver.delete(CloudBrUtils.CHAT_CONTENT_URI, null, null);
        Log.d(CLASS_TAG, "delete message table entre count = " + count);

        count = contentResolver.delete(CloudBrUtils.RCS_URI, null, null);
        Log.d(CLASS_TAG, "delete rcs message table entre count = " + count);

        count = contentResolver.delete(CloudBrUtils.CHAT_MULTI_URI, null, null);
        Log.d(CLASS_TAG, "delete  multi table entre count = " + count);

        Log.d(CLASS_TAG, "use remote service to delete  sms mms table begin");
        count = 0;
        if (mService != null) {
            try {
                count = mService.delMsg(CloudBrUtils.MMS_SMS_URI);
            } catch (RemoteException e) {
                Log.e(CLASS_TAG, "mService.delMsg happen exception, return false");
                e.printStackTrace();
                return false;
            }
        }
        Log.d(CLASS_TAG, "delete  sms mms table entre count = " + count);

        count = contentResolver.delete(CloudBrUtils.FAVOTIRE_URI, null, null);
        Log.d(CLASS_TAG, "delete  favorite table entre count = " + count);

        count = contentResolver.delete(CloudBrUtils.FT_URI, null, null);
        Log.d(CLASS_TAG, "delete  ft table entre count = " + count);
        return true;
    }
}
