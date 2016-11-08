package com.mediatek.rcs.message.provider;

import org.gsma.joyn.chat.ChatLog;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class DAPIChatProvider extends ContentProvider {

    /**
     * Database tables
     */
    private static final int CHAT         = 1;
    private static final int CHAT_ID      = 2;

    private static final String AUTHORITY = "com.cmcc.ccs.group_chat";

    private static final String TAG = "D-API/Provider/Chat";

    private static final String COLUMN_CHAT_ID      = "GROUPCHATSERVICE_CHAT_ID";
    private static final String COLUMN_STATE        = "GROUPCHATSERVICE_STATE";
    private static final String COLUMN_SUBJECT      = "GROUPCHATSERVICE_SUBJECT";
    private static final String COLUMN_CHAIRMEN     = "GROUPCHATSERVICE_CHAIRMEN";
    private static final String COLUMN_NICK_NAME    = "GROUPCHATSERVICE_NICK_NAME";
    private static final String COLUMN_TIMESTAMP    = "GROUPCHATSERVICE_TIMESTAMP";
    private static final String COLUMN_DIRECTION    = "GROUPCHATSERVICE_DIRECTION";

    private static final String[] COLUMN_MAP = {COLUMN_CHAT_ID, COLUMN_STATE, COLUMN_SUBJECT,
        COLUMN_CHAIRMEN, COLUMN_NICK_NAME, COLUMN_TIMESTAMP, COLUMN_DIRECTION};

    private static final String TAPI_CHAT_ID        = ChatLog.GroupChat.CHAT_ID;
    private static final String TAPI_STATE          = ChatLog.GroupChat.STATE;
    private static final String TAPI_SUBJECT        = ChatLog.GroupChat.SUBJECT;
    private static final String TAPI_CHAIRMEN       = ChatLog.GroupChat.CHAIRMAN;
    private static final String TAPI_NICK_NAME      = ChatLog.GroupChat.NICKNAME;
    private static final String TAPI_TIMESTAMP      = ChatLog.GroupChat.TIMESTAMP;
    private static final String TAPI_DIRECTION      = ChatLog.GroupChat.DIRECTION;

    private static final String[] TAPI_MAP = {TAPI_CHAT_ID, TAPI_STATE, TAPI_SUBJECT,
        TAPI_CHAIRMEN, TAPI_NICK_NAME, TAPI_TIMESTAMP, TAPI_DIRECTION};

    private static final Uri CONTENT_URI = Uri.parse("content://org.gsma.joyn.provider.chat/chat");

    // Allocate the UriMatcher object
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        uriMatcher.addURI(AUTHORITY, null, CHAT);
        uriMatcher.addURI(AUTHORITY, "#", CHAT_ID);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        throw new UnsupportedOperationException("Not Support URI " + uri);
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Not Support URI " + uri);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        Log.d(TAG, "query uri=" + uri.toString() + ", projection=" + projection + ", selection=" +
                    selection + ", selectionArgs=" + selectionArgs + ", sortOrder=" + sortOrder);
        String[] newProjection = translateProjection(projection);
        String newSelection = translateSelection(selection);
        Cursor cursor = null;
        ContentResolver resolver = getContext().getContentResolver();
        int match = uriMatcher.match(uri);
        switch(match) {
            case CHAT:
                cursor = resolver.query(CONTENT_URI, newProjection, newSelection,
                        selectionArgs, sortOrder);
                break;
            case CHAT_ID:
                String rowId = uri.getLastPathSegment();
                selection = concatSelections(selection, "_id=" + rowId);
                cursor = resolver.query(CONTENT_URI, newProjection, newSelection,
                        selectionArgs, sortOrder);
                break;
            default:
                throw new UnsupportedOperationException("Unknown URI " + uri);
        }
        if (cursor != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where,
            String[] whereArgs) {
        throw new UnsupportedOperationException("Not Support URI " + uri);
    }

    private String[] translateProjection(String[] projection) {
        String[] newProjection = null;
        if (projection != null && projection.length > 0) {
            newProjection = new String[projection.length];
            for (int i = 0; i < projection.length; i++) {
                for (String column : COLUMN_MAP) {
                    if (column.equals(projection[i])) {
                        newProjection[i] = TAPI_MAP[i] + " AS " + projection[i];
                        break;
                    }
                }
            }
        }
        return newProjection;
    }

    private String translateSelection(String selection) {
        if (!TextUtils.isEmpty(selection)) {
            for (int i = 0; i < COLUMN_MAP.length; i++) {
                if (selection.contains(COLUMN_MAP[i])) {
                    selection = selection.replace(COLUMN_MAP[i], TAPI_MAP[i]);
                }
            }
        }
        return selection;
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
