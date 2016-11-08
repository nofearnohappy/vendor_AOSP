package com.mediatek.rcs.message.cloudbackup.modules;

import com.mediatek.rcs.message.cloudbackup.modules.FtMsgRestoreParser;
import com.mediatek.rcs.message.cloudbackup.modules.FtMsgRestoreParser.IinitFileName;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.ChatRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.FavoriteRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.FtRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.RcsMsgRecord;

import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.RootRecord;
import com.mediatek.rcs.message.cloudbackup.utils.FileUtils;
import com.mediatek.rcs.message.utils.RcsMessageUtils;
//import com.mediatek.rcs.common.utils.RCSUtils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.io.File;

class FavFtDecomposer implements IinitFileName {
    private static final String CLASS_TAG = CloudBrUtils.MODULE_TAG + "FavFtDecomposer";
    private ContentResolver mContentResolver;
    private FtRecord mFtRecord;
    private ChatRecord mChatRecord;
    private RootRecord mRootRecord;
    // private FileObject mFileObject;
    private RcsMsgRecord mRcsMsgRecord;
    private FtMsgRestoreParser mFtMsgRestoreParser;
    private boolean mCancel = false;
    private Context mContext;

    FavFtDecomposer(Context context) {
        mContext = context;
        mContentResolver = mContext.getContentResolver();
        mFtMsgRestoreParser = new FtMsgRestoreParser(this);
    }

    protected int parseFtMsg(File file) {
        if (mFtMsgRestoreParser == null) {
            Log.e(CLASS_TAG, "parseFtMsg mFtMsgParserImpl == null,return");
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
            Log.e(CLASS_TAG, "mFtMsgParserImpl.parseFtMsgImple result is not ok, return");
            return result;
        }
        Log.d(CLASS_TAG, "mFtMsgParserImpl end, result = ok");

        if (mCancel) {
            Log.d(CLASS_TAG, "parseFtMsg service canceled");
            result = CloudBrUtils.ResultCode.SERVICE_CANCELED;
        }

        FavoriteRecord favoriteRecord = new FavoriteRecord();
        result = restructureRecord(favoriteRecord);
        if (result != CloudBrUtils.ResultCode.OK) {
            Log.e(CLASS_TAG, "restructureRecord result is not ok, return");
            return result;
        }
        Log.d(CLASS_TAG, "restructureRecord end, result = ok");

        //because file transfer file have exited in the path that database order,
        //so can't copy file again after insert databases.
        Uri uri = FileUtils.insertFavoriteDb(mContentResolver, favoriteRecord);
        Log.d(CLASS_TAG, "insert favorite database uri = " + uri);
        if (uri == null) {
            Log.e(CLASS_TAG, "insert favorite db error, return false");
            return CloudBrUtils.ResultCode.INSERTDB_EXCEPTION;
        } else {
            Cursor cr = mContentResolver.query(uri,
                    new String[] {CloudBrUtils.Favorite.COLUMN_PATH}, null, null, null);
            cr.moveToFirst();
            String filePath = cr.getString(cr.getColumnIndex(CloudBrUtils.Favorite.COLUMN_PATH));
            cr.close();
            Log.d(CLASS_TAG, "filePath = " + filePath);

            File origFile = new File(favoriteRecord.getPath());
            File destFile = new File(filePath);
            boolean renameRet = origFile.renameTo(destFile);
            Log.d(CLASS_TAG, "renameRet " + renameRet);
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

    protected int restructureRecord(FavoriteRecord favoriteRecord) {
        String from = mRcsMsgRecord.getFrom();
        if (from.equals(CloudBrUtils.getMyNumber())) {
            from = null;
        }
        favoriteRecord.setContactNum(from);
        String filePath = mFtRecord.getFileName();
        Log.d(CLASS_TAG, "filePath = " + filePath);
        favoriteRecord.setPath(filePath);
        favoriteRecord.setMimeType(mFtRecord.geMimeType());
        favoriteRecord.setSize(mFtRecord.getSize());

        favoriteRecord.setChatId(mRcsMsgRecord.getChatId());
        favoriteRecord.setDataSent(mRcsMsgRecord.getDataSent());
        favoriteRecord.setDate(mRcsMsgRecord.getTimestamp());
        favoriteRecord.setDirection(mRcsMsgRecord.getDirection());
        favoriteRecord.setFlag(mRcsMsgRecord.getFlag());
        favoriteRecord.setStatus(mRcsMsgRecord.getStatus());
        favoriteRecord.setType(mRcsMsgRecord.getType());
        favoriteRecord.setMsgId(mRcsMsgRecord.getIpmsgId());

        return CloudBrUtils.ResultCode.OK;
    }

    @Override
    public String initFtFilePath(String name) {
        File folder = new File(FileUtils.getFavFtFilePath(mContext));
        if (!folder.exists()) {
            folder.mkdirs();
        }
        String fileName = FileUtils.getFavFtFilePath(mContext) + File.separator + name;
        Log.d(CLASS_TAG, "ftCv put file name = " + fileName);
        mFtRecord.setFileName(fileName);
        return fileName;
    }

    /**
     * This method will be called when backup service be cancel.
     * @param cancel
     */
    protected void setCancel(boolean cancel) {
        mCancel = cancel;
    }
}
