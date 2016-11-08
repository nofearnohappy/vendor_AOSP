package com.mediatek.rcs.message.provider;

import com.mediatek.rcs.common.provider.GroupMemberData;
import com.mediatek.rcs.common.utils.Logger;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class GroupMemberProvider extends ContentProvider {

    /**
     * Database tables
     */
    public static final String TABLE_GROUPMEMBER = "groupmember";

    private static final String TAG = "GroupMemberProvider";
    // Create the constants used to differentiate between the different URI requests
    private static final int URI_GROUPMEMBER         = 1;
    private static final int URI_GROUPMEMBER_ID      = 2;
    private static final int URI_DAPI_GROUPMEMBER    = 3;
    private static final int URI_DAPI_GROUPMEMBER_ID = 4;

    private static final String DAPI_AUTHORITY = "com.cmcc.ccs.group_member";

    // Allocate the UriMatcher object
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        uriMatcher.addURI(GroupMemberData.AUTHORITY, null, URI_GROUPMEMBER);
        uriMatcher.addURI(GroupMemberData.AUTHORITY, "#", URI_GROUPMEMBER_ID);

        uriMatcher.addURI(DAPI_AUTHORITY, null, URI_DAPI_GROUPMEMBER);
        uriMatcher.addURI(DAPI_AUTHORITY, "#", URI_DAPI_GROUPMEMBER_ID);
    }

    /**
     * Database helper class
     */
    private SQLiteOpenHelper mOpenHelper;

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        Logger.d(TAG, "delete start, Uri=" + uri + ", where=" + where);
        int match = uriMatcher.match(uri);
        int count = 0;
        String finalSelection = null;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        switch (match) {
            case URI_GROUPMEMBER:
                finalSelection = where;
                break;
            case URI_GROUPMEMBER_ID:
                finalSelection = concatSelections(where, "_id=" + uri.getLastPathSegment());
                break;
            default:
                throw new UnsupportedOperationException("Cannot delete URI " + uri);
        }
        count = db.delete(TABLE_GROUPMEMBER, finalSelection, whereArgs);
        Logger.d(TAG, "delete end, affected rows=" + count);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Logger.d(TAG, "insert start");
        int match = uriMatcher.match(uri);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Uri result;
        switch (match) {
        case URI_GROUPMEMBER:
            long id = db.insert(TABLE_GROUPMEMBER, null, values);
            result = ContentUris.withAppendedId(GroupMemberData.CONTENT_URI, id);
            break;
        default:
            throw new UnsupportedOperationException("Cannot delete URI " + uri);
        }
        Logger.d(TAG, "insert end, uri = " + result);
        return result;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = RCSMessageDatabaseHelper.getInstance(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        Logger.d(TAG, "quert start, selection=" + selection);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        int match = uriMatcher.match(uri);
        switch (match) {
            case URI_GROUPMEMBER:
            case URI_DAPI_GROUPMEMBER:
                qb.setTables(TABLE_GROUPMEMBER);
                break;
            case URI_GROUPMEMBER_ID:
            case URI_DAPI_GROUPMEMBER_ID:
                qb.setTables(TABLE_GROUPMEMBER);
                selection = concatSelections(selection, "_id=" + uri.getLastPathSegment());
                break;
            default:
                throw new UnsupportedOperationException("Cannot query URI " + uri);
        }
        qb.setDistinct(true);
        Cursor cursor = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        Logger.d(TAG, "quert end, cursor count=" + cursor.getCount());
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where,
            String[] whereArgs) {
        Log.d(TAG, "update start, uri=" + uri.toString() + ", values=" + values + ", where=" +
                where + ", whereArgs=" + whereArgs);
        int count = 0;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int match = uriMatcher.match(uri);
        switch(match) {
            case URI_GROUPMEMBER:
                count = db.update(TABLE_GROUPMEMBER, values, where, whereArgs);
                break;
            case URI_GROUPMEMBER_ID:
                where = concatSelections(where,
                            GroupMemberData.COLUMN_ID + "=" + uri.getLastPathSegment());
                count = db.update(TABLE_GROUPMEMBER, values, where, whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        Logger.d(TAG, "update end, affected rows=" + count);
        return count;
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
