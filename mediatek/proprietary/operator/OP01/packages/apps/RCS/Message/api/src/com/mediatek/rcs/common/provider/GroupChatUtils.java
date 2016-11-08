package com.mediatek.rcs.common.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.mediatek.rcs.common.utils.Logger;
import com.mediatek.rcs.common.utils.RCSUtils;

public class GroupChatUtils {

    private static GroupChatUtils sInstance;
    private ContentResolver mCr;
    private static final String TAG = "GroupChatUtils";

    private GroupChatUtils(Context context) {
        mCr = context.getContentResolver();
    }

    public synchronized static GroupChatUtils getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new GroupChatUtils(context);
        }
        return sInstance;
    }

    public static final String[] GROUP_CHAT_PROJECTION = {
        GroupChatData.KEY_ID,
        GroupChatData.KEY_CHAT_ID,
        GroupChatData.KEY_SUBJECT,
        GroupChatData.KEY_NICKNAME,
        GroupChatData.KEY_STATUS,
        GroupChatData.KEY_SUB_ID,
        GroupChatData.KEY_ISCHAIRMEN
    };

    public Uri insertGroupChatData(String chatId, String subject, long status, int isMeChairmen) {
        Logger.d(TAG, "insertGroupChatData(), chatId=" + chatId
                + ", subject=" + subject + ", status=" + status);
        ContentValues cv = new ContentValues();
        cv.put(GroupChatData.KEY_CHAT_ID, chatId);
        cv.put(GroupChatData.KEY_SUBJECT, subject);
        cv.put(GroupChatData.KEY_STATUS, status);
        cv.put(GroupChatData.KEY_ISCHAIRMEN, isMeChairmen);
        cv.put(GroupChatData.KEY_SUB_ID, RCSUtils.getRCSSubId());
        Cursor cursor = getGroupChatDataByChatId(chatId);
        Uri uri = null;
        try {
            if (cursor != null && cursor.moveToFirst()) {
                uri = ContentUris.withAppendedId(GroupChatData.CONTENT_URI,
                        cursor.getInt(cursor.getColumnIndex(GroupChatData.KEY_ID)));
                mCr.update(uri, cv, null, null);
            } else if (cursor != null) {
                uri = mCr.insert(GroupChatData.CONTENT_URI, cv);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return uri;
    }

    public int deleteAllData() {
        return mCr.delete(GroupChatData.CONTENT_URI, null, null);
    }

    public int deleteGroupChatData(String chatId) {
        Logger.d(TAG, "deleteGroupChatData(), chatId=" + chatId);
        String selection = GroupChatData.KEY_CHAT_ID + "='" + chatId + "'";
        return mCr.delete(GroupChatData.CONTENT_URI, selection, null);
    }

    public boolean updateSubId(String chatId, int subId) {
        Logger.d(TAG, "updateSubId(), subId=" + subId + ", chatId=" + chatId);
        String selection = GroupChatData.KEY_CHAT_ID + "='" + chatId + "'";
        ContentValues cv = new ContentValues(1);
        cv.put(GroupChatData.KEY_SUB_ID, subId);
        int count = mCr.update(GroupChatData.CONTENT_URI, cv, selection, null);
        return count > 0 ? true : false;
    }

    public boolean updateChairmen(String chatId, boolean isChairmen) {
        Logger.d(TAG, "updateChairmen(), chatId=" + chatId + ", isChairmen=" + isChairmen);
        String selection = GroupChatData.KEY_CHAT_ID + "='" + chatId + "'";
        ContentValues cv = new ContentValues(1);
        int isMeChairmen = isChairmen ? 1 : 0;
        cv.put(GroupChatData.KEY_ISCHAIRMEN, isMeChairmen);
        int count = mCr.update(GroupChatData.CONTENT_URI, cv, selection, null);
        return count > 0 ? true : false;
    }

    public boolean updateStatus(String chatId, long status) {
        Logger.d(TAG, "updateStatus(), chatId=" + chatId + ", status=" + status);
        String selection = GroupChatData.KEY_CHAT_ID + "='" + chatId + "'";
        ContentValues cv = new ContentValues(1);
        cv.put(GroupChatData.KEY_STATUS, status);
        int count = mCr.update(GroupChatData.CONTENT_URI, cv, selection, null);
        return count > 0 ? true : false;
    }

    public boolean updateSubject(String chatId, String subject) {
        Logger.d(TAG, "updateSubject(), chatId=" + chatId + ", subject=" + subject);
        String selection = GroupChatData.KEY_CHAT_ID + "='" + chatId + "'";
        ContentValues cv = new ContentValues(1);
        cv.put(GroupChatData.KEY_SUBJECT, subject);
        int count = mCr.update(GroupChatData.CONTENT_URI, cv, selection, null);
        return count > 0 ? true : false;
    }

    public boolean updateNickName(String chatId, String nickName) {
        Logger.d(TAG, "updateNickName(), chatId=" + chatId + ", nickName=" + nickName);
        String selection = GroupChatData.KEY_CHAT_ID + "='" + chatId + "'";
        ContentValues cv = new ContentValues(1);
        cv.put(GroupChatData.KEY_NICKNAME, nickName);
        int count = mCr.update(GroupChatData.CONTENT_URI, cv, selection, null);
        return count > 0 ? true : false;
    }

    public Cursor getAllGroupChat() {
        Logger.d(TAG, "getAllGroupChatData()");
        return mCr.query(GroupChatData.CONTENT_URI, GROUP_CHAT_PROJECTION, null, null, null);
    }

    private Cursor getGroupChatDataByChatId(String chatId) {
        Logger.d(TAG, "getGroupChatDataByChatId(), chatId=" + chatId);
        return mCr.query(GroupChatData.CONTENT_URI, GROUP_CHAT_PROJECTION,
                GroupChatData.KEY_CHAT_ID + "='" + chatId + "'", null, null);
    }
}
