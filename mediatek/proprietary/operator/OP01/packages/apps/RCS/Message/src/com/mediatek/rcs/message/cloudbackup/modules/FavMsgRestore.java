package com.mediatek.rcs.message.cloudbackup.modules;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.BackupConstant;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.BackupDataFileType;
import com.mediatek.rcs.message.cloudbackup.utils.FileUtils;


import android.content.ContentResolver;
import android.content.Context;
import android.util.Log;

public class FavMsgRestore {
    private static final String CLASS_TAG = CloudBrUtils.MODULE_TAG + "FavMsgRestore";

    private Context mContext;
    private ContentResolver mContentResolver;
    private FavChatGroupDecomposer mFavChatGroupDecomposer;
    private FavFtDecomposer mFavFtDecomposer;
    private FavChat1To1Decomposer mFavChat1To1Decomposer;
    private FavSmsDecomposer mFavSmsDecomposer;
   // private FavMmsDecomposer mFavMmsDecomposer;
    private File mBackupDataFolder;

    public FavMsgRestore(Context context, File backupDataFolder) {
        Log.d(CLASS_TAG, "messageParse");
        mContext = context;
        createContentResolver();
        mBackupDataFolder = backupDataFolder;
    }

    private boolean createContentResolver() {
        if (mContentResolver != null) {
            Log.d(CLASS_TAG, "createContentResolver() Resolver exit!");
            return true;
        }
        if (mContext == null) {
            Log.e(CLASS_TAG, "createContentResolver mContext = null, return");
            return false;
        }
        mContentResolver = mContext.getContentResolver();
        if (mContentResolver == null) {
            Log.e(CLASS_TAG, "cacheData mContentResolver = null, return");
            return false;
        }
        return true;
    }

   public int restoreData(File backupDataFolder) {
        int result = CloudBrUtils.ResultCode.OK;
        for(File file : backupDataFolder.listFiles()) {
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
     * @param cancel
     */
    public void setCancel(boolean cancel) {
        if (mFavFtDecomposer != null) {
            mFavFtDecomposer.setCancel(cancel);
        }

        if (mFavSmsDecomposer !=null) {
            mFavSmsDecomposer.setCancel(cancel);
        }

//        if (mFavMmsDecomposer !=null) {
//            mFavMmsDecomposer.setCancel(cancel);
//        }
        if (mFavChat1To1Decomposer != null) {
            mFavChat1To1Decomposer.setCancel(cancel);
        }
    }

    /**
     * Get file type(ft/group chat/1-n msg), and call corresponding class to parse the file.
     * @param file backup file that get from server.
     * @return parse file result.
     */
    protected int parserMsgFromFile(File file) {
        int result = CloudBrUtils.ResultCode.OK;
        String filePath = file.getAbsolutePath();
        Log.d(CLASS_TAG, "filePath = " + filePath);
      /*  if (filePath.endsWith("pdu")) {
            Log.d(CLASS_TAG, "parserMsgFromFile if a pdu return true");
            return CloudBrUtils.ResultCode.OK;
        }

        if (filePath.endsWith("vmsg")) {
            Log.d(CLASS_TAG, "parserMsgFromFile is a sms vmsg");
            if (mFavSmsDecomposer == null) {
                mFavSmsDecomposer = new FavSmsDecomposer(mContentResolver);
            }
            result = mFavSmsDecomposer.retoreData(file);
            mFavSmsDecomposer = null;
            return result;
        }

        if (filePath.endsWith("xml")) {
            Log.d(CLASS_TAG, "parserMsgFromFile is mms xml");
            if (mFavMmsDecomposer == null) {
                mFavMmsDecomposer = new FavMmsDecomposer(file, mContentResolver);
            }
            result = mFavMmsDecomposer.retoreMmsData();
            mFavMmsDecomposer = null;
            return result;
        }*/
        int fileType = FileUtils.anysisFileType(file);
        Log.d(CLASS_TAG, "parserMsgFromFile fileType = " + fileType);
        if (fileType == -1) {
            Log.e(CLASS_TAG, "anysisFileType error");
            return CloudBrUtils.ResultCode.OTHER_EXCEPTION;
        }
        if (fileType == BackupDataFileType.VMSG) {
            Log.d(CLASS_TAG, "parserMsgFromFile is a sms vmsg");
            if (mFavSmsDecomposer == null) {
                mFavSmsDecomposer = new FavSmsDecomposer(mContentResolver);
            }
            result = mFavSmsDecomposer.retoreData(file);
            mFavSmsDecomposer = null;
            Log.d(CLASS_TAG, "restore sms end result = " + result);
            return result;
        } else if (fileType == BackupDataFileType.MMS_XML) {
            Log.d(CLASS_TAG, "this file is a mms xml, skip");
            return CloudBrUtils.ResultCode.OK;
        } else if (fileType == BackupDataFileType.PDU) {
            Log.d(CLASS_TAG, "parserMsgFromFile is pdu");
//            if (mFavMmsDecomposer == null) {
//                mFavMmsDecomposer = new FavMmsDecomposer(mContentResolver);
//            }
//            result = mFavMmsDecomposer.restoreMmsData(file);
//            return result;
            return CloudBrUtils.ResultCode.OK;
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
            if (mFavChatGroupDecomposer == null) {
                mFavChatGroupDecomposer = new FavChatGroupDecomposer(mContext);
            }
           return mFavChatGroupDecomposer.parseGroupMsg(file);
        } else if (content.equals(CloudBrUtils.ContentType.GROUP_FT_TYPE)) {
            Log.d(CLASS_TAG, "ft msg");
            if (mFavFtDecomposer == null) {
                mFavFtDecomposer = new FavFtDecomposer(mContext);
            }
           return mFavFtDecomposer.parseFtMsg(file);
        } else {
            Log.d(CLASS_TAG, "is a 1 to 1 text msg content = " + content);
            if (mFavChat1To1Decomposer == null) {
                mFavChat1To1Decomposer = new FavChat1To1Decomposer(mContext);
            }
           return mFavChat1To1Decomposer.parseOneToOneMsg(file);
        }
    }
}
