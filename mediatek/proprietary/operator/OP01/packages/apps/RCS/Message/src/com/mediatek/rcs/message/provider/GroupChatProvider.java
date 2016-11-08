package com.mediatek.rcs.message.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.rcs.common.provider.GroupChatData;

public class GroupChatProvider extends ContentProvider {

    /**
     * Database tables
     */
    public static final String TABLE_GROUPCHAT = "groupchat";
    // Create the constants used to differentiate between the different URI requests
    private static final int GROUP_CHAT         = 1;
    private static final int GROUP_CHAT_ID      = 2;

    private static final String TAG = "RCS/Provider/GroupChatProvider";

    // Allocate the UriMatcher object
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        uriMatcher.addURI(GroupChatData.AUTHORITY, null, GROUP_CHAT);
        uriMatcher.addURI(GroupChatData.AUTHORITY, "#", GROUP_CHAT_ID);
    }

    /**
     * Database helper class
     */
    private SQLiteOpenHelper mOpenHelper;

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        Log.d(TAG, "delete uri=" + uri.toString() + ",where=" + where + ", whereArgs=" + whereArgs);
        int count = 0;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int match = uriMatcher.match(uri);
        switch(match) {
            case GROUP_CHAT:
                count = db.delete(TABLE_GROUPCHAT, where, whereArgs);
                break;
            case GROUP_CHAT_ID:
                String finalSelection = concatSelections(where, "_id=" + uri.getLastPathSegment());
                count = db.delete(TABLE_GROUPCHAT, finalSelection, whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (count > 0) {
            notifyChange();
        }
        return count;
    }

    @Override
    public String getType(Uri uri) {
        int match = uriMatcher.match(uri);
        switch(match) {
            case GROUP_CHAT:
                return "vnd.android.cursor.dir/groupchat";
            case GROUP_CHAT_ID:
                return "vnd.android.cursor.item/groupchat";
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        Log.d(TAG, "insert uri=" + uri.toString() + ", values=" + values);
        long id = 0;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int match = uriMatcher.match(uri);
        switch(match) {
            case GROUP_CHAT:
            case GROUP_CHAT_ID:
                id = db.insert(TABLE_GROUPCHAT, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (id > 0) {
            notifyChange();
        }
        return ContentUris.withAppendedId(GroupChatData.CONTENT_URI, id);
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = RCSMessageDatabaseHelper.getInstance(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        // TODO Auto-generated method stub
        Log.d(TAG, "query uri=" + uri.toString() + ", projection=" + projection + ", selection=" +
                    selection + ", selectionArgs=" + selectionArgs + ", sortOrder=" + sortOrder);
        Cursor cursor = null;
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        int match = uriMatcher.match(uri);
        switch(match) {
            case GROUP_CHAT:
                cursor = db.query(TABLE_GROUPCHAT, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case GROUP_CHAT_ID:
                String rowId = uri.getLastPathSegment();
                selection = concatSelections(selection, "_id=" + rowId);
                cursor = db.query(TABLE_GROUPCHAT, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (cursor != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), GroupChatData.CONTENT_URI);
        }
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where,
            String[] whereArgs) {
        // TODO Auto-generated method stub
        Log.d(TAG, "update uri=" + uri.toString() + ", values=" + values + ", where=" +
                    where + ", whereArgs=" + whereArgs);
        int count = 0;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int match = uriMatcher.match(uri);
        switch(match) {
            case GROUP_CHAT:
                count = db.update(TABLE_GROUPCHAT, values, where, whereArgs);
                break;
            case GROUP_CHAT_ID:
                where = concatSelections(where,
                        GroupChatData.KEY_ID + "=" + uri.getLastPathSegment());
                count = db.update(TABLE_GROUPCHAT, values, where, whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (count > 0) {
            notifyChange();
        }
        return count;
    }

    private void notifyChange() {
        getContext().getContentResolver().notifyChange(GroupChatData.CONTENT_URI, null);
    }

    private String concatSelections(String selection1, String selection2) {
        if (TextUtils.isEmpty(selection1)) {
            return selection2;
        } else if (TextUtils.isEmpty(selection2)) {
            return selection1;
        } else {
            return selection1 + " AND " + selection2;
        }
    }
}