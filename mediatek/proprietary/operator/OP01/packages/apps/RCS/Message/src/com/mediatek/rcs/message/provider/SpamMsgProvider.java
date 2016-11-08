package com.mediatek.rcs.message.provider;

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
import android.text.TextUtils;

import com.mediatek.rcs.common.provider.SpamMsgData;

public class SpamMsgProvider extends ContentProvider {

    /**
     * Database tables
     */
    public static final String TABLE_SPAM = "spam";

    // Create the constants used to differentiate between the different URI requests
    private static final int URI_SPAM         = 1;
    private static final int URI_SPAM_ID      = 2;

    // Allocate the UriMatcher object
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        uriMatcher.addURI(SpamMsgData.AUTHORITY, null, URI_SPAM);
        uriMatcher.addURI(SpamMsgData.AUTHORITY, "#", URI_SPAM_ID);
    }

    private static final String[] DELETE_QUERY_PROJECTION = {
        SpamMsgData.COLUMN_ID,
        SpamMsgData.COLUMN_IPMSG_ID,
        SpamMsgData.COLUMN_TYPE
    };

    private static final Uri TEXT_IPMSG_CONTENT_URI = Uri.parse("content://org.gsma.joyn.provider.chat/message");
    private static final Uri FT_IPMSG_CONTENT_URI   = Uri.parse("content://org.gsma.joyn.provider.ft/ft");
    /**
     * Database helper class
     */
    private SQLiteOpenHelper mOpenHelper;

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        // TODO Auto-generated method stub
        int match = uriMatcher.match(uri);
        int count = 0;
        String finalSelection = null;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        switch (match) {
            case URI_SPAM:
                finalSelection = where;
                break;
            case URI_SPAM_ID:
                finalSelection = concatSelections(where, "_id=" + uri.getLastPathSegment());
                break;
            default:
                throw new UnsupportedOperationException("Cannot delete URI " + uri);
        }
        boolean deleteStackMsg = TextUtils.isEmpty(uri.getQueryParameter("restore"));
        count = deleteSpamMsgs(db, finalSelection, whereArgs, deleteStackMsg);
        if (count > 0) {
            notifyChange();
        }
        return count;
    }

    private int deleteSpamMsgs(SQLiteDatabase db, String where, String[] whereArgs, boolean deleteStackMsg) {
        int count = 0;
        Cursor cursor = null;
        Set<Long> textIpMsgIds = new HashSet<Long>();
        Set<Long> ftIpMsgIds = new HashSet<Long>();
        try {
            cursor = db.query(TABLE_SPAM, DELETE_QUERY_PROJECTION, where, whereArgs, null, null, null);
            while(cursor.moveToNext()) {
                long ipMsgId = cursor.getLong(cursor.getColumnIndexOrThrow(SpamMsgData.COLUMN_IPMSG_ID));
                switch(cursor.getInt(cursor.getColumnIndexOrThrow(SpamMsgData.COLUMN_TYPE))) {
                    case SpamMsgData.Type.TYPE_IP_TEXT_MSG:
                        textIpMsgIds.add(ipMsgId);
                        break;
                    case SpamMsgData.Type.TYPE_IP_FT_MSG:
                        ftIpMsgIds.add(ipMsgId);
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception e) {
        } finally {
            cursor.close();
        }
        count = db.delete(TABLE_SPAM, where, whereArgs);
        if (deleteStackMsg) {
            deleteIpMsgByIds(TEXT_IPMSG_CONTENT_URI, textIpMsgIds);
            deleteIpMsgByIds(FT_IPMSG_CONTENT_URI, ftIpMsgIds);
        }
        return count;
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
            case URI_SPAM:
            case URI_SPAM_ID:
                id = db.insert(TABLE_SPAM, null, values);
                result = ContentUris.withAppendedId(SpamMsgData.CONTENT_URI, id);
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
            case URI_SPAM:
                qb.setTables(TABLE_SPAM);
                break;
            case URI_SPAM_ID:
                qb.setTables(TABLE_SPAM);
                selection = concatSelections(selection, "_id=" + uri.getLastPathSegment());
                break;
            default:
                throw new UnsupportedOperationException("Cannot query URI " + uri);
        }
        qb.setDistinct(true);
        Cursor cursor = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), SpamMsgData.CONTENT_URI);
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
        cr.notifyChange(SpamMsgData.CONTENT_URI, null, false);
    }

    private int deleteIpMsgByIds(Uri uri, Set<Long> ids) {
        int count = 0;
        if (ids != null && ids.size() > 0) {
            count = getContext().getContentResolver().delete(uri, "_id " + formatInClause(ids), null);
        }
        return count;
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
}
