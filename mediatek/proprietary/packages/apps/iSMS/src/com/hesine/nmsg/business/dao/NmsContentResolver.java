package com.hesine.nmsg.business.dao;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class NmsContentResolver {

    public static Cursor query(ContentResolver cr, Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        Cursor cursor = null;
        if (null == cr) {
            return null;
        }
        cursor = cr.query(uri, projection, selection, selectionArgs, sortOrder);
        return cursor;
    }

    public static int delete(ContentResolver cr, Uri uri, String where, String[] selectionArgs) {
        if (null == cr) {
            return 0;
        }
        return cr.delete(uri, where, selectionArgs);
    }

    public static Uri insert(ContentResolver cr, Uri uri, ContentValues values) {
        if (null == cr) {
            return null;
        }
        return cr.insert(uri, values);
    }

    public static int update(ContentResolver cr, Uri uri, ContentValues values, String where,
            String[] selectionArgs) {
        try {
            if (null == cr) {
                return 0;
            }
            return cr.update(uri, values, where, selectionArgs);
        } catch (NullPointerException e) {
            return 0;
        }
    }

}
