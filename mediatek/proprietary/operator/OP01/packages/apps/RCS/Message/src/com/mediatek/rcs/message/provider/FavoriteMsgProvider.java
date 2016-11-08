package com.mediatek.rcs.message.provider;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;

import com.cmcc.ccs.chat.ChatService;
import com.mediatek.rcs.common.provider.FavoriteMsgData;

public class FavoriteMsgProvider extends ContentProvider {

    /**
     * Database tables
     */
    public static final String TABLE_FAVORITE = "favorite";

    // Create the constants used to differentiate between the different URI requests
    private final static String TAG = "RCS/Provider/Favorite";
    private static final int URI_FAVORITE_ALL = 1;
    private static final int URI_FAVORITE_ID  = 2;

    public static final int FAVORITEEMOJI = 6;

    // Allocate the UriMatcher object
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        uriMatcher.addURI(FavoriteMsgData.AUTHORITY, null, URI_FAVORITE_ALL);
        uriMatcher.addURI(FavoriteMsgData.AUTHORITY, "#", URI_FAVORITE_ID);
    }

    /**
     * Database helper class
     */
    private SQLiteOpenHelper mOpenHelper;

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        // TODO Auto-generated method stub
        Log.d(TAG, "delete uri = " + uri);
        Log.d(TAG, "where uri = " + where);
        int match = uriMatcher.match(uri);
        int count = 0;
        String finalSelection = null;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        switch (match) {
            case URI_FAVORITE_ALL:
                finalSelection = where;
                break;
            case URI_FAVORITE_ID:
                finalSelection = concatSelections(where, "_id=" + uri.getLastPathSegment());
                break;
            default:
                throw new UnsupportedOperationException("Cannot delete URI " + uri);
        }
        count = deleteFavoriteMsgs(db, finalSelection, whereArgs);
        Log.d(TAG, "count = " + count);
         if (count > 0) {
            notifyChange();
        }
        return count;
    }

    private int deleteFavoriteMsgs(SQLiteDatabase db,
            String selection, String[] selectionArgs) {
        Cursor cursor = db.query(TABLE_FAVORITE,
            new String[] { FavoriteMsgData.COLUMN_DA_TYPE,FavoriteMsgData.COLUMN_DA_FILENAME },
                selection, selectionArgs, null, null, null);
        if (cursor == null) {
            // FIXME: This might be an error, ignore it may cause
            // unpredictable result.
            return 0;
        }
        try {
            if (cursor.getCount() == 0) {
                return 0;
            }
            while (cursor.moveToNext()) {
                try {
                    // Delete the associated files saved on file-system.
                    int mType = cursor.getInt(
                                cursor.getColumnIndexOrThrow(FavoriteMsgData.COLUMN_DA_TYPE));
                    if (mType == ChatService.MMS || mType == ChatService.FT
                                    || mType == ChatService.XML) {
                        String path = cursor.getString(cursor.getColumnIndexOrThrow(
                                            FavoriteMsgData.COLUMN_DA_FILENAME));
                        if (path != null) {
                            new File(path).delete();
                        }
                    }
                } catch (Throwable ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                }
            }
        } finally {
            cursor.close();
        }

        return db.delete(TABLE_FAVORITE, selection, selectionArgs);
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long id = 0;
        Uri result = null;
        int match = uriMatcher.match(uri);
        switch (match) {
            case URI_FAVORITE_ALL:
            case URI_FAVORITE_ID:
                id = db.insert(TABLE_FAVORITE, null, values);
                result = ContentUris.withAppendedId(FavoriteMsgData.CONTENT_URI, id);
                break;
            default:
                throw new UnsupportedOperationException("Cannot insert URI " + uri);
        }
        if (id > 0) {
            notifyChange();
        }
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
        // TODO Auto-generated method stub
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        int match = uriMatcher.match(uri);
        switch (match) {
            case URI_FAVORITE_ALL:
                qb.setTables(TABLE_FAVORITE);
                break;
            case URI_FAVORITE_ID:
                qb.setTables(TABLE_FAVORITE);
                selection = concatSelections(selection, "_id=" + uri.getLastPathSegment());
                break;
            default:
                throw new UnsupportedOperationException("Cannot query URI " + uri);
        }
        qb.setDistinct(true);
        Cursor cursor = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), FavoriteMsgData.CONTENT_URI);
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where,
            String[] whereArgs) {
        // TODO Auto-generated method stub
        return 0;
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

    private void notifyChange() {
        ContentResolver cr = getContext().getContentResolver();
        cr.notifyChange(FavoriteMsgData.CONTENT_URI, null, false);
    }

    private String formatInClause(Set<Long> ids) {
        /* to IN sql */
        if (ids == null || ids.size() == 0) {
            return " IN ()";
        }
        String in = " IN ";
        in += ids.toString();
        in = in.replace('[', '(');
        in = in.replace(']', ')');
        return in;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
         Cursor c = query(uri, new String[]{FavoriteMsgData.COLUMN_DA_FILENAME}, null, null, null);
         int count = (c != null) ? c.getCount() : 0;
         if (count != 1) {
             // If there is not exactly one result, throw an appropriate
             // exception.
             if (c != null) {
                 c.close();
             }
             if (count == 0) {
                 throw new FileNotFoundException("No entry for " + uri);
             }
             throw new FileNotFoundException("Multiple items at " + uri);
         }

         c.moveToFirst();
         int i = c.getColumnIndex(FavoriteMsgData.COLUMN_DA_FILENAME);
         String path = (i >= 0 ? c.getString(i) : null);
         c.close();
         if (path == null) {
             throw new FileNotFoundException("Column _data not found.");
         }

         int modeBits = ParcelFileDescriptor.parseMode(mode);
         return ParcelFileDescriptor.open(new File(path), modeBits);
    }
}
