package com.mediatek.rcs.message.cloudbackup.modules;

import com.mediatek.rcs.common.RcsLog;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.Favorite;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.ResultCode;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

public class FavMsgBackup {
    private static final String CLASS_TAG = CloudBrUtils.MODULE_TAG + "FavMsgBackup";

    private Context mContext;
    private ContentResolver mContentResolver;
    private boolean mCancel = false;
    private File mFavoriteFolder;
    private String mFavoriteFolderPath;
    private ArrayList<Integer> mFtIdList;
    private ArrayList<Integer> mSmsIdList;
    private ArrayList<Integer> mMmsIdList;
    private ArrayList<Integer> mChatIdList;

    private FavFtComposer mFavFtComposer;
    private FavChat1To1Composer mFavChat1To1Composer;
    private FavSmsComposer mFavSmsComposer;
   // private FavMmsComposer mFavMmsComposer;

    /**
     * @param context
     * @param folderPath
     *            favorite backup data will put folder path.
     */
    public FavMsgBackup(Context context, String folderPath) {
        mContext = context;
        mFavoriteFolderPath = folderPath;
    }

    /**
     * This method will be called when backup service be cancel.
     * @param cancel
     */
    public void setCancel(boolean cancel) {
        mCancel = cancel;

        if (mFavFtComposer != null) {
            mFavFtComposer.setCancel(cancel);
        }

        if (mFavSmsComposer != null) {
            mFavSmsComposer.setCancel(cancel);
        }

//        if (mFavMmsComposer != null) {
//            mFavMmsComposer.setCancel(cancel);
//        }
        if (mFavChat1To1Composer != null) {
            mFavChat1To1Composer.setCancel(cancel);
        }
    }

    /**
     * backup favorite table in rcsmessage.db.
     * @return backup result.
     */
    public int backupData() {
        if (!createContentResolver()) {
            Log.e(CLASS_TAG, "createContentResolver error");
            return CloudBrUtils.ResultCode.OTHER_EXCEPTION;
        }

        Cursor favCursor = mContentResolver
                .query(CloudBrUtils.FAVOTIRE_URI, null, null, null, null);
        if (favCursor == null) {
            Log.d(CLASS_TAG, "backupData() favorite query database erro, return");
            return CloudBrUtils.ResultCode.DB_EXCEPTION;
        }
        if (favCursor.getCount() <= 0) {
            Log.d(CLASS_TAG, "backupData() favorite no data, return");
            favCursor.close();
            return CloudBrUtils.ResultCode.OK;
        }
        favCursor.close();

        mFavoriteFolder = new File(mFavoriteFolderPath);
        if (mFavoriteFolder != null && mFavoriteFolder.exists()) {
            Log.d(CLASS_TAG, "mFavFolderPath clear ");
            mFavoriteFolder.delete();
        }
        mFavoriteFolder.mkdirs();

        if (mCancel) {
            Log.d(CLASS_TAG, "backupData() service canceled");
            return CloudBrUtils.ResultCode.SERVICE_CANCELED;
        }

        cacheFavTable();
        int result = backupFavoriteData();
        return result;
    }

    private int backupFavoriteData() {
        Log.d(CLASS_TAG, "backupFavoriteData begin...");
        Log.d(CLASS_TAG, "backup Favorite ft Data begin...");
        if (mFtIdList != null && mFtIdList.size() > 0) {
            mFavFtComposer = new FavFtComposer(mFavoriteFolder.getAbsolutePath(), mContentResolver,
                    mFtIdList);
            int result = mFavFtComposer.composeFtMsg();
            if (result != ResultCode.OK) {
                Log.e(CLASS_TAG, "favFtComposer.composeFtMsg() error result " + result);
                return result;
            }
        }
        mFavFtComposer = null;

        Log.d(CLASS_TAG, "backup Favorite chat Data begin...");
        if (mChatIdList != null && mChatIdList.size() > 0) {
            mFavChat1To1Composer = new FavChat1To1Composer(mFavoriteFolder.getAbsolutePath(),
                    mContentResolver, mChatIdList);
            int result = mFavChat1To1Composer.backupChatMsg();
            if (result != ResultCode.OK) {
                Log.e(CLASS_TAG, "favChatBackupComposer.backupChatMsg error result " + result);
                return result;
            }
        }
        mFavChat1To1Composer = null;

        Log.d(CLASS_TAG, "backup Favorite sms Data begin...");
        if (mSmsIdList != null && mSmsIdList.size() > 0) {
            mFavSmsComposer = new FavSmsComposer(mFavoriteFolder.getAbsolutePath(),
                    mContentResolver, mSmsIdList);
            int result = mFavSmsComposer.backupSmsData();
            if (result != ResultCode.OK) {
                Log.e(CLASS_TAG, "favSmsBackupComposer.backupSmsData() is not ok result " + result);
                return result;
            }
        }
        mFavSmsComposer = null;

//        Log.d(CLASS_TAG, "backup Favorite mms Data begin...");
//        if (mMmsIdList != null && mMmsIdList.size() > 0) {
//            mFavMmsComposer = new FavMmsComposer(mFavoriteFolder.getAbsolutePath(),
//                    mContentResolver, mMmsIdList);
//            int result = mFavMmsComposer.backupMmsData();
//            if (result != ResultCode.OK) {
//                Log.e(CLASS_TAG, "favMmsBackupComposer.backupMmsData() error result " + result);
//                return result;
//            }
//        }
//        mFavMmsComposer = null;

        Log.d(CLASS_TAG, "backup Favorite Data end");
        return ResultCode.OK;
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

    private void cacheFavTable() {
        String[] message_projection = new String[] { Favorite.COLUMN_ID,
                Favorite.COLUMN_DA_TYPE, Favorite.COLUMN_DA_FLAG, Favorite.COLUMN_PATH};
        mFtIdList = new ArrayList<Integer>();
        mSmsIdList = new ArrayList<Integer>();
        mMmsIdList = new ArrayList<Integer>();
        mChatIdList = new ArrayList<Integer>();

        Cursor cursor = mContentResolver.query(CloudBrUtils.FAVOTIRE_URI, message_projection, null,
                null, null);
        int count = cursor.getCount();
        Log.d(CLASS_TAG, "favorite table has data count = " + count);
        int type;
        int id;
        int flag;
        String fileName;
        cursor.moveToFirst();
        int columnType = cursor.getColumnIndex(Favorite.COLUMN_DA_TYPE);
        int columnId = cursor.getColumnIndex(Favorite.COLUMN_ID);
        int columnFlag = cursor.getColumnIndex(Favorite.COLUMN_DA_FLAG);
        int columnFileName = cursor.getColumnIndex(Favorite.COLUMN_PATH);

        while (!cursor.isAfterLast()) {
            type = cursor.getInt(columnType);
            id = cursor.getInt(columnId);
            flag = cursor.getInt(columnFlag);
            fileName = cursor.getString(columnFileName);
            if (flag == Favorite.Flag.PUBLIC) {
                cursor.moveToNext();
                continue;
            }

            if (type == Favorite.Type.SMS) {
                    Log.d(CLASS_TAG, "id + " + id + "is a sms msg");
                    mSmsIdList.add(id);
            } else if (type == Favorite.Type.MMS) {
                Log.d(CLASS_TAG, "id + " + id + "is a mms msg");
                mMmsIdList.add(id);
            } else if (type == Favorite.Type.IM) {
                Log.d(CLASS_TAG, "id + " + id + "is a IM msg");
                mChatIdList.add(id);
            } else if (type == Favorite.Type.FT) {
                Log.d(CLASS_TAG, "id + " + id + "is a FT msg");
                mFtIdList.add(id);
            } else {
                Log.d(CLASS_TAG, "id + " + id + "is a unknown type msg, skip");
            }
            cursor.moveToNext();
        }

        Log.d(CLASS_TAG, "cacheFavoriteTable end.");
        cursor.close();
    }

}
