package com.mediatek.rcs.message.cloudbackup.modules;

import android.content.ContentResolver;
import android.database.Cursor;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.Favorite;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.MmsXmlInfo;
import com.mediatek.rcs.message.cloudbackup.utils.FileUtils;

/**
 * Favorite mms backup module.
 */
class FavMmsComposer {
    private static final String CLASS_TAG = CloudBrUtils.MODULE_TAG + "FavMmsComposer";

    protected String mFavBackupFolder;
    private MmsXmlComposer mXmlComposer;
    private ArrayList<Integer> mIds;
    private boolean mCancel = false;
    private ContentResolver mContentResolver;

    FavMmsComposer(String filePath, ContentResolver contentResolver, ArrayList<Integer> ids) {
        mFavBackupFolder = filePath;
        mIds = ids;
        mContentResolver = contentResolver;
    }

    /**
     * This method will be called when backup service be cancel.
     * @param cancel
     */
    void setCancel(boolean cancel) {
        mCancel = cancel;
    }

    int backupMmsData() {
        if (mIds.size() <= 0) {
            Log.d(CLASS_TAG, "mms is no data, return ok");
            return CloudBrUtils.ResultCode.OK;
        }
        int result = init();
        if (result != CloudBrUtils.ResultCode.OK) {
            Log.e(CLASS_TAG, "backupChatGroupMsg error result " + result);
            return result;
        }

        result = implementComposeEntity();
        if (result != CloudBrUtils.ResultCode.OK) {
            Log.e(CLASS_TAG, "backupChatGroupMsg error result " + result);
            return result;
        }

        result = onEnd();
        if (mCancel) {
            Log.d(CLASS_TAG, "backupMmsData() service canceled");
            result = CloudBrUtils.ResultCode.SERVICE_CANCELED;
        }
        return result;
    }

    private int init() {
        int result = CloudBrUtils.ResultCode.OK;
        if (mIds.size() > 0) {
            mXmlComposer = new MmsXmlComposer();
            if (mXmlComposer != null) {
                mXmlComposer.startCompose();
            }

            File path = new File(mFavBackupFolder);
            if (!path.exists()) {
                path.mkdirs();
            }
        }

        if (mCancel) {
            Log.d(CLASS_TAG, "backup1To1ChatMsg() service canceled");
            result = CloudBrUtils.ResultCode.SERVICE_CANCELED;
        }

        Log.d(CLASS_TAG, "init():" + result + " count:" + mIds.size());
        return result;
    }

    private int implementComposeEntity() {
        int result = CloudBrUtils.ResultCode.OK;
        int sequence = 0;
        for (int id : mIds) {
            if (mCancel) {
                Log.d(CLASS_TAG, "backup1To1ChatMsg() service canceled");
                result = CloudBrUtils.ResultCode.SERVICE_CANCELED;
            }

            String selection = CloudBrUtils.ID + " = " + id;
            Log.d(CLASS_TAG, "backup1To1ChatMsg selection = " + selection);
            Cursor favCursor = mContentResolver.query(CloudBrUtils.FAVOTIRE_URI, null, selection,
                    null, null);
            favCursor.moveToFirst();

            MmsXmlInfo mRecord = new MmsXmlInfo();
            String pduPath = favCursor.getString(favCursor
                    .getColumnIndex(Favorite.COLUMN_PATH));
            File pduFile = new File(pduPath);
            if (pduFile == null || !pduFile.exists()) {
                Log.e(CLASS_TAG, pduPath + "is not exists, continue");
                favCursor.close();
                mXmlComposer = null;
                continue;
            }
            String fileName = sequence + ".pdu";
            String backupFilePath = mFavBackupFolder + File.separator + fileName;
            mRecord.setID(fileName);
            mRecord.setIsRead("1");
            mRecord.setMsgBox("1");
            mRecord.setDate(Long.toString(favCursor.getLong(favCursor
                    .getColumnIndex(Favorite.COLUMN_DATE))));
            mRecord.setSize(Long.toString(favCursor.getLong(favCursor
                    .getColumnIndex(Favorite.COLUMN_SIZE))));
            mRecord.setSimId("0");
            mRecord.setIsLocked("0");

            if (mXmlComposer != null) {
                mXmlComposer.addOneMmsRecord(mRecord);
            }
            FileUtils.copyPduFile(pduPath, backupFilePath);

            if (favCursor != null) {
                favCursor.close();
                favCursor = null;
            }
            sequence++;
        }

        Log.d(CLASS_TAG, "implementComposeOneEntity:" + result);
        return result;
    }

    /**
     * Describe <code>writeToFile</code> method here.
     * @param fileName
     *            a <code>String</code> value
     * @param buf
     *            a <code>byte</code> value
     * @exception IOException
     *                if an error occurs
     */
    private void writeToFile(String fileName, byte[] buf) {
        try {
            FileOutputStream outStream = new FileOutputStream(fileName);
            outStream.write(buf, 0, buf.length);
            outStream.flush();
            outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Describe <code>onEnd</code> method here.
     */
    private int onEnd() {
        if (mCancel) {
            Log.d(CLASS_TAG, "backup1To1ChatMsg() service canceled");
            return CloudBrUtils.ResultCode.SERVICE_CANCELED;
        }
        if (mXmlComposer != null) {
            mXmlComposer.endCompose();
            String msgXmlInfo = mXmlComposer.getXmlInfo();
            if (mIds.size() > 0 && msgXmlInfo != null) {
                writeToFile(mFavBackupFolder + File.separator + FileUtils.ModulePath.MMS_XML,
                        msgXmlInfo.getBytes());

            }
        }
        return CloudBrUtils.ResultCode.OK;
    }
}
