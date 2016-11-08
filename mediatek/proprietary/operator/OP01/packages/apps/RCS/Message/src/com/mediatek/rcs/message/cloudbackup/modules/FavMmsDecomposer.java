package com.mediatek.rcs.message.cloudbackup.modules;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.MultimediaMessagePdu;
import com.google.android.mms.pdu.EncodedStringValue;
import com.mediatek.rcs.common.RcsLog;
import com.mediatek.rcs.message.cloudbackup.modules.MmsRestoreParser;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.Favorite;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.FavoriteRecord;
import com.mediatek.rcs.message.cloudbackup.utils.FileUtils;
import com.mediatek.rcs.message.cloudbackup.utils.FileUtils.ModulePath;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

class FavMmsDecomposer {
    private static final String CLASS_TAG = CloudBrUtils.MODULE_TAG + "FavMmsDecomposer";
    private ContentResolver mContentResolver;
    private boolean mCancel = false;
    private File mFavFileFolder;

    FavMmsDecomposer(ContentResolver contentResolver) {
        mContentResolver = contentResolver;
    }

    protected int restoreMmsData(File pduFile) {
        if (pduFile == null || !pduFile.exists()) {
            Log.d(CLASS_TAG, "backup file error. return");
            return CloudBrUtils.ResultCode.BACKUP_FILE_ERROR;
        }
        // String path = pduFile.getAbsolutePath();
        mFavFileFolder = new File(ModulePath.FAVORITE_FOLDER);
        if (mFavFileFolder != null && !mFavFileFolder.exists()) {
            mFavFileFolder.mkdirs();
        }

        int result = implementComposeEntity(pduFile);
        onEnd();
        if (mCancel) {
            Log.d(CLASS_TAG, "backupMmsData() service canceled");
            result = CloudBrUtils.ResultCode.SERVICE_CANCELED;
        }
        return result;
    }

    protected int implementComposeEntity(File pduFile) {
        int result = CloudBrUtils.ResultCode.OTHER_EXCEPTION;
        if (mCancel) {
            Log.d(CLASS_TAG, "backupMmsData() service canceled");
            result = CloudBrUtils.ResultCode.SERVICE_CANCELED;
        }

        FavoriteRecord favoriteRecord = new FavoriteRecord();
        favoriteRecord.setSize(pduFile.length());
        String pduName = pduFile.getName();
        if (!pduName.endsWith("pdu")) {
            pduName = pduName + ".pdu";
        }
        String backupPduPath = pduFile.getAbsolutePath();
        String favPduPath = mFavFileFolder.getAbsolutePath() + File.separator + pduName;
        favoriteRecord.setPath(favPduPath);

        Log.d(CLASS_TAG, "backupPduPath:" + backupPduPath);
        byte[] pduByteArray = readFileContent(backupPduPath);
        if (pduByteArray != null) {
            result = CloudBrUtils.ResultCode.OK;
        }
        if (result == CloudBrUtils.ResultCode.OK) {
            GenericPdu genericPdu = new PduParser(pduByteArray, false).parse(true);
            if (genericPdu == null) {
                Log.d(CLASS_TAG, "genericPdu is null return");
                return CloudBrUtils.ResultCode.BACKUP_FILE_ERROR;
            }

            String from = null;
            EncodedStringValue encodeFrom = genericPdu.getFrom();
            if (encodeFrom != null) {
                from = encodeFrom.getString();
            }

            Log.d(CLASS_TAG, "pdu from = " + from);
            if (from.contains("insert")) {
                from = null;
            }
            String subject = null;
            long date = System.currentTimeMillis();
            if (genericPdu instanceof MultimediaMessagePdu) {
                EncodedStringValue encodeSubject = ((MultimediaMessagePdu) genericPdu).getSubject();
                if (encodeSubject != null) {
                    subject = encodeSubject.getString();
                }
                date = ((MultimediaMessagePdu) genericPdu).getDate();
                Log.d(CLASS_TAG, "date = " + date);
            } else {
                Log.d(CLASS_TAG, "not a MultimediaMessagePdu. no date and subject");
            }
            favoriteRecord.setBody(subject);
            favoriteRecord.setDate(date);
            favoriteRecord.setChatId(from);

            if (from.equals(CloudBrUtils.getMyNumber())) {
                favoriteRecord.setDirection(1);//outcoming
            } else {
                favoriteRecord.setDirection(0);
            }
            favoriteRecord.setFlag(RcsLog.ThreadFlag.OFFICIAL);
            favoriteRecord.setType(RcsLog.MessageType.SMSMMS);
        }

        FileUtils.copyPduFile(backupPduPath, favPduPath);

        Uri uri = FileUtils.insertFavoriteDb(mContentResolver, favoriteRecord);
        Log.d(CLASS_TAG, "favPduPath = " + favPduPath);
        Log.d(CLASS_TAG, "insert favorite database uri = " + uri);
        if (uri == null) {
            Log.e(CLASS_TAG, "insert favorite db error, return false");
            return CloudBrUtils.ResultCode.INSERTDB_EXCEPTION;
        }
        return result;
    }

    protected boolean onEnd() {
        Log.d(CLASS_TAG, "onEnd()");
        return true;
    }

    /**
     * Describe <code>readFileContent</code> method here.
     * @param fileName
     *            a <code>String</code> value
     * @return a <code>byte[]</code> value
     */
    private byte[] readFileContent(String fileName) {
        try {
            InputStream is = new FileInputStream(fileName);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int len = -1;
            byte[] buffer = new byte[512];
            while ((len = is.read(buffer, 0, 512)) != -1) {
                baos.write(buffer, 0, len);
            }

            is.close();
            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * This method will be called when backup service be cancel.
     * @param cancel
     */
    protected void setCancel(boolean cancel) {
        mCancel = cancel;
    }
}
