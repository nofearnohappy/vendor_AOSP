package com.mediatek.rcs.pam.provider;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.provider.PAContract.AccountColumns;
import com.mediatek.rcs.pam.provider.PAContract.CcsAccountColumns;
import com.mediatek.rcs.pam.provider.PAContract.CcsAccountInfoColumns;
import com.mediatek.rcs.pam.provider.PAContract.CcsHistoryColumns;
import com.mediatek.rcs.pam.provider.PAContract.CcsMessageColumns;
import com.mediatek.rcs.pam.provider.PAContract.CcsSearchColumns;
import com.mediatek.rcs.pam.provider.PAContract.MediaArticleColumns;
import com.mediatek.rcs.pam.provider.PAContract.MediaBasicColumns;
import com.mediatek.rcs.pam.provider.PAContract.MediaColumns;
import com.mediatek.rcs.pam.provider.PAContract.MessageColumns;
import com.mediatek.rcs.pam.provider.PAContract.MessageHistorySummaryColumns;
import com.mediatek.rcs.pam.provider.PAContract.SearchColumns;
import com.mediatek.rcs.pam.provider.PAContract.StateColumns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PAProvider extends ContentProvider {
    private static final String TAG = Constants.TAG_PREFIX + "PAProvider";

    private static final String ZWSP = new String(new char[]{0x200B});
    private static final String SEARCH_TEXT_SEPERATOR = " " + ZWSP + " ";

    private interface UriCode {
        int ACCOUNTS = 0;
        int ACCOUNTS_ID = 1;
        int MESSAGES = 2;
        int MESSAGES_ID = 3;
        int MEDIA_BASICS = 4;
        int MEDIA_BASICS_ID = 5;
        int MEDIA_ARTICLES = 6;
        int MEDIA_ARTICLES_ID = 7;
        int MEDIAS = 8;
        int MEDIAS_ID = 9;
        int SEARCH = 10;
        int MESSAGE_HISTORY_SUMMARIES = 11;
        int STATE = 12;

        /* CMCC only */
        int CCS_MESSAGES = 100;
        int CCS_HISTORY = 101;
        int CCS_ACCOUNTS = 102;
        int CCS_ACCOUNT_INFOS = 103;
        int CCS_ACCOUNT_SEARCH = 104;
    }

    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        URI_MATCHER.addURI(PAContract.AUTHORITY, "accounts", UriCode.ACCOUNTS);
        URI_MATCHER.addURI(PAContract.AUTHORITY, "accounts/#", UriCode.ACCOUNTS_ID);
        URI_MATCHER.addURI(PAContract.AUTHORITY, "messages", UriCode.MESSAGES);
        URI_MATCHER.addURI(PAContract.AUTHORITY, "messages/#", UriCode.MESSAGES_ID);
        URI_MATCHER.addURI(PAContract.AUTHORITY, "media_basics", UriCode.MEDIA_BASICS);
        URI_MATCHER.addURI(PAContract.AUTHORITY, "media_basics/#", UriCode.MEDIA_BASICS_ID);
        URI_MATCHER.addURI(PAContract.AUTHORITY, "media_articles", UriCode.MEDIA_ARTICLES);
        URI_MATCHER.addURI(PAContract.AUTHORITY, "media_articles/#", UriCode.MEDIA_ARTICLES_ID);
        URI_MATCHER.addURI(PAContract.AUTHORITY, "medias", UriCode.MEDIAS);
        URI_MATCHER.addURI(PAContract.AUTHORITY, "medias/#", UriCode.MEDIAS_ID);
        URI_MATCHER.addURI(PAContract.AUTHORITY, PAContract.SEARCH, UriCode.SEARCH);
        URI_MATCHER.addURI(PAContract.AUTHORITY,
                "message_history_summaries", UriCode.MESSAGE_HISTORY_SUMMARIES);
        URI_MATCHER.addURI(PAContract.AUTHORITY, "state", UriCode.STATE);

        /* CMCC only */
        URI_MATCHER.addURI(PAContract.CCS_ACCOUNT_MESSAGE_AUTHORITY,
                null, UriCode.CCS_MESSAGES);
        URI_MATCHER.addURI(PAContract.CCS_ACCOUNT_HISTORY_AUTHORITY,
                null, UriCode.CCS_HISTORY);
        URI_MATCHER.addURI(PAContract.CCS_ACCOUNT_AUTHORITY,
                null, UriCode.CCS_ACCOUNTS);
        URI_MATCHER.addURI(PAContract.CCS_ACCOUNT_INFO_AUTHORITY,
                null, UriCode.CCS_ACCOUNT_INFOS);
        URI_MATCHER.addURI(PAContract.CCS_ACCOUNT_SEARCH_AUTHORITY,
                null, UriCode.CCS_ACCOUNT_SEARCH);
    }

    public static final String DATABASE_NAME = "publicaccounts.db";
    private static final int DATABASE_VERSION = 2;

    interface Tables {
        String ACCOUNT = "account";
        String MESSAGE = "message";
        String MEDIA_BASIC = "media_basic";
        String MEDIA_ARTICLE = "media_article";
        String MEDIA = "media";
        String SEARCH = "search";
        String MESSAGE_HISTORY_SUMMARY_VIEW = "message_history_summary";

        /* CMCC only */
        String CCS_MESSAGE_VIEW = "ccs_message_view";
        String CCS_HISTORY = "ccs_history";
        String CCS_ACCOUNT_VIEW = "ccs_account_view";
        String CCS_ACCOUNT_INFO_VIEW = "ccs_account_info_view";
        String CCS_ACCOUNT_SEARCH = "ccs_account_search";

        /* Internal */
        String STATE = "state";
    }

    class PADatabaseHelper extends SQLiteOpenHelper {

        public PADatabaseHelper(Context context, String name, CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + Tables.ACCOUNT + "("
                    + AccountColumns.ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + AccountColumns.UUID + " TEXT NOT NULL UNIQUE,"
                    + AccountColumns.NAME + " TEXT,"
                    + AccountColumns.ID_TYPE + " INTEGER,"
                    + AccountColumns.INTRODUCTION + " TEXT,"
                    + AccountColumns.RECOMMEND_LEVEL + " INTEGER DEFAULT 1,"
                    + AccountColumns.LOGO_ID + " INTEGER,"
                    + AccountColumns.SUBSCRIPTION_STATUS + " INTEGER NOT NULL,"
                    + AccountColumns.COMPANY + " TEXT,"
                    + AccountColumns.TYPE + " TEXT,"
                    + AccountColumns.UPDATE_TIME + " INTEGER,"
                    + AccountColumns.MENU_TYPE + " INTEGER,"
                    + AccountColumns.MENU_TIMESTAMP + " INTEGER,"
                    + AccountColumns.ACCEPT_STATUS + " INTEGER,"
                    + AccountColumns.ACTIVE_STATUS + " INTEGER,"
                    + AccountColumns.TELEPHONE + " TEXT,"
                    + AccountColumns.EMAIL + " TEXT,"
                    + AccountColumns.ZIPCODE + " TEXT,"
                    + AccountColumns.ADDRESS + " TEXT,"
                    + AccountColumns.FIELD + " TEXT,"
                    + AccountColumns.QRCODE_URL + " TEXT,"
                    + AccountColumns.MENU + " TEXT,"
                    + AccountColumns.LAST_MESSAGE + " INTEGER);");

            db.execSQL("CREATE TABLE " + Tables.MESSAGE + "("
                    + MessageColumns.ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + MessageColumns.UUID + " TEXT,"
                    + MessageColumns.SOURCE_ID + " TEXT,"
                    + MessageColumns.SOURCE_TABLE + " INTEGER,"
                    + MessageColumns.CHAT_ID + " TEXT,"
                    + MessageColumns.ACCOUNT_ID + " INTEGER,"
                    + MessageColumns.ACCOUNT_UUID + " TEXT,"
                    + MessageColumns.TYPE + " INTEGER,"
                    + MessageColumns.MIME_TYPE + " TEXT,"
                    + MessageColumns.CHAT_TYPE + " INTEGER,"
                    + MessageColumns.TIMESTAMP + " INTEGER,"
                    + MessageColumns.CREATE_TIME + " INTEGER,"
                    + MessageColumns.SMS_DIGEST + " TEXT,"
                    + MessageColumns.BODY + " TEXT,"
                    + MessageColumns.TEXT + " TEXT,"
                    + MessageColumns.DIRECTION + " INTEGER NOT NULL,"
                    + MessageColumns.FORWARDABLE + " INTEGER,"
                    + MessageColumns.STATUS + " INTEGER,"
                    + MessageColumns.DATA1 + " INTEGER DEFAULT -1,"
                    + MessageColumns.DATA2 + " INTEGER DEFAULT -1,"
                    + MessageColumns.DATA3 + " INTEGER DEFAULT -1,"
                    + MessageColumns.DATA4 + " INTEGER DEFAULT -1,"
                    + MessageColumns.DATA5 + " INTEGER DEFAULT -1,"
                    + MessageColumns.SYSTEM + " INTEGER DEFAULT 0,"
                    + MessageColumns.DELETED + " INTEGER DEFAULT 0);");

            db.execSQL("CREATE TABLE " + Tables.MEDIA_BASIC + "("
                    + MediaBasicColumns.ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + MediaBasicColumns.TITLE + " TEXT,"
                    + MediaBasicColumns.FILE_SIZE + " TEXT,"
                    + MediaBasicColumns.DURATION + " TEXT,"
                    + MediaBasicColumns.FILE_TYPE + " TEXT,"
                    + MediaBasicColumns.ACCOUNT_ID + " INTEGER,"
                    + MediaBasicColumns.CREATE_TIME + " INTEGER NOT NULL,"
                    + MediaBasicColumns.MEDIA_UUID + " TEXT,"
                    + MediaBasicColumns.DESCRIPTION + " TEXT,"
                    + MediaBasicColumns.THUMBNAIL_ID + " INTEGER DEFAULT -1,"
                    + MediaBasicColumns.ORIGINAL_ID + " INTEGER DEFAULT -1);");

            db.execSQL("CREATE TABLE " + Tables.MEDIA_ARTICLE + "("
                    + MediaArticleColumns.ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + MediaArticleColumns.TITLE + " TEXT,"
                    + MediaArticleColumns.AUTHOR + " TEXT,"
                    + MediaArticleColumns.THUMBNAIL_ID + " INTEGER DEFAULT -1,"
                    + MediaArticleColumns.ORIGINAL_ID + " INTEGER DEFAULT -1,"
                    + MediaArticleColumns.SOURCE_URL + " TEXT,"
                    + MediaArticleColumns.BODY_URL + " TEXT,"
                    + MediaArticleColumns.TEXT + " TEXT,"
                    + MediaArticleColumns.FILE_TYPE + " TEXT,"
                    + MediaArticleColumns.MEDIA_UUID + " TEXT NOT NULL);");

            db.execSQL("CREATE TABLE " + Tables.MEDIA + "("
                    + MediaColumns.ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + MediaColumns.TYPE + " INTEGER,"
                    + MediaColumns.TIMESTAMP + " INTEGER,"
                    + MediaColumns.PATH + " TEXT,"
                    + MediaColumns.URL + " TEXT UNIQUE,"
                    + MediaColumns.REF_COUNT + " INTEGER DEFAULT 0);");

            db.execSQL("CREATE VIRTUAL TABLE " + Tables.SEARCH + " USING FTS4("
                    + SearchColumns.ACCOUNT_ID + " INTEGER,"
                    + SearchColumns.ACCOUNT_NAME + " TEXT,"
                    + SearchColumns.MESSAGE_TEXT + " TEXT,"
                    + SearchColumns.MESSAGE_SUMMARY + " TEXT,"
                    + SearchColumns.MESSAGE_MEDIA_TITLE1 + " TEXT,"
                    + SearchColumns.MESSAGE_MEDIA_TITLE2 + " TEXT,"
                    + SearchColumns.MESSAGE_MEDIA_TITLE3 + " TEXT,"
                    + SearchColumns.MESSAGE_MEDIA_TITLE4 + " TEXT,"
                    + SearchColumns.MESSAGE_MEDIA_TITLE5 + " TEXT,"
                    + SearchColumns.MESSAGE_ARTICLE_TEXT1 + " TEXT,"
                    + SearchColumns.MESSAGE_ARTICLE_TEXT2 + " TEXT,"
                    + SearchColumns.MESSAGE_ARTICLE_TEXT3 + " TEXT,"
                    + SearchColumns.MESSAGE_ARTICLE_TEXT4 + " TEXT,"
                    + SearchColumns.MESSAGE_ARTICLE_TEXT5 + " TEXT,"
                    + " TOKENIZE=porter);");

            db.execSQL("CREATE VIEW " + Tables.MESSAGE_HISTORY_SUMMARY_VIEW + " AS SELECT "
                    + Tables.ACCOUNT + "." + AccountColumns.ID
                    + " AS " + MessageHistorySummaryColumns.ID + ", "
                    + Tables.ACCOUNT + "." + AccountColumns.UUID
                    + " AS " + MessageHistorySummaryColumns.UUID + ","
                    + Tables.ACCOUNT + "." + AccountColumns.NAME
                    + " AS " + MessageHistorySummaryColumns.NAME + ","
                    + Tables.ACCOUNT + "." + AccountColumns.LOGO_ID
                    + " AS " + MessageHistorySummaryColumns.LOGO_ID + ","
                    + Tables.MEDIA + "." + MediaColumns.PATH
                    + " AS " + MessageHistorySummaryColumns.LOGO_PATH + ","
                    + Tables.MEDIA + "." + MediaColumns.URL
                    + " AS " + MessageHistorySummaryColumns.LOGO_URL + ","
                    + AccountColumns.LAST_MESSAGE
                    + " AS " + MessageHistorySummaryColumns.LAST_MESSAGE_ID + ","
                    + Tables.MESSAGE + "." + MessageColumns.TYPE + " AS "
                    + MessageHistorySummaryColumns.LAST_MESSAGE_TYPE + ","
                    + "MAX(" + Tables.MESSAGE + "." + MessageColumns.TIMESTAMP + ") AS "
                    + MessageHistorySummaryColumns.LAST_MESSAGE_TIMESTAMP + ","
                    + Tables.MESSAGE + "." + MessageColumns.SMS_DIGEST + " AS "
                    + MessageHistorySummaryColumns.LAST_MESSAGE_SUMMARY
                    + " FROM ((" + Tables.ACCOUNT
                    + " LEFT OUTER JOIN " + Tables.MEDIA
                    + " ON " + Tables.ACCOUNT + "." + AccountColumns.LOGO_ID + "="
                    + Tables.MEDIA + "." + MediaColumns.ID + ")"
                    + " LEFT OUTER JOIN " + Tables.MESSAGE
                    + " ON " + Tables.ACCOUNT + "." + AccountColumns.LAST_MESSAGE + "="
                    + Tables.MESSAGE + "." + MessageColumns.ID + ")"
                    + " GROUP BY " + Tables.ACCOUNT + "." + MessageHistorySummaryColumns.ID
                    + " ORDER BY " + MessageHistorySummaryColumns.LAST_MESSAGE_TIMESTAMP
                    + " DESC;");

            /* CMCC only */
            db.execSQL("CREATE VIEW " + Tables.CCS_MESSAGE_VIEW + " AS SELECT "
                    + Tables.MESSAGE + "." + MessageColumns.ID
                    + " AS " + CcsMessageColumns.ID + ", "
                    + Tables.MESSAGE + "." + MessageColumns.SOURCE_ID
                    + " AS " + CcsMessageColumns.MESSAGE_ID + ", "
                    + Tables.MESSAGE + "." + MessageColumns.ACCOUNT_UUID
                    + " AS " + CcsMessageColumns.ACCOUNT + ", "
                    + Tables.MESSAGE + "." + MessageColumns.BODY
                    + " AS " + CcsMessageColumns.BODY + ", "
                    + Tables.MESSAGE + "." + MessageColumns.TIMESTAMP
                    + " AS " + CcsMessageColumns.TIMESTAMP + ", "
                    + Tables.MESSAGE + "." + MessageColumns.MIME_TYPE
                    + " AS " + CcsMessageColumns.MIME_TYPE + ", "
                    + Tables.MESSAGE + "." + MessageColumns.STATUS
                    + " AS " + CcsMessageColumns.MESSAGE_STATUS + ", "
                    + Tables.MESSAGE + "." + MessageColumns.DIRECTION
                    + " AS " + CcsMessageColumns.DIRECTION + ", "
                    + Tables.MESSAGE + "." + MessageColumns.CHAT_TYPE
                    + " AS " + CcsMessageColumns.TYPE + " FROM " + Tables.MESSAGE + ";");

            db.execSQL("CREATE TABLE " + Tables.CCS_HISTORY + "("
                    + CcsHistoryColumns.MESSAGE_ID + " TEXT,"
                    + CcsHistoryColumns.ACCOUNT + " TEXT,"
                    + CcsHistoryColumns.BODY + " BLOB,"
                    + CcsHistoryColumns.TIMESTAMP + " LONG,"
                    + CcsHistoryColumns.MIME_TYPE + " TEXT,"
                    + CcsHistoryColumns.MESSAGE_STATUS + " INTEGER,"
                    + CcsHistoryColumns.DIRECTION + " INTEGER,"
                    + CcsHistoryColumns.TYPE + " TEXT,"
                    + CcsHistoryColumns.ID + " LONG);");

            db.execSQL("CREATE TABLE " + Tables.CCS_ACCOUNT_SEARCH + "("
                    + CcsSearchColumns.ID + " TEXT PRIMARY KEY,"
                    + CcsSearchColumns.ACCOUNT + " INTEGER,"
                    + CcsSearchColumns.NAME + " TEXT,"
                    + CcsSearchColumns.PORTRAIT_TYPE + " TEXT,"
                    + CcsSearchColumns.BREIF_INTRODUCTION + " TEXT,"
                    + CcsSearchColumns.PORTRAIT + " TEXT);");

            db.execSQL("CREATE VIEW " + Tables.CCS_ACCOUNT_VIEW + " AS SELECT "
                    + Tables.ACCOUNT + "." + AccountColumns.ID
                    + " AS " + CcsAccountColumns.ID + ", "
                    + AccountColumns.UUID
                    + " AS " + CcsAccountColumns.ACCOUNT + ", "
                    + Tables.ACCOUNT + "." + AccountColumns.NAME
                    + " AS " + CcsAccountColumns.NAME + ", "
                    + Tables.MEDIA + "." + MediaColumns.URL
                    + " AS " + CcsAccountColumns.PORTRAIT + ", "
                    + Tables.MEDIA + "." + MediaColumns.PATH
                    + " AS " + CcsAccountColumns.PORTRAIT_PATH + ", "
                    + AccountColumns.INTRODUCTION + " AS " + CcsAccountColumns.BREIF_INTRODUCTION
                    + " FROM " + Tables.ACCOUNT + " LEFT OUTER JOIN " + Tables.MEDIA
                    + " ON " + Tables.ACCOUNT + "." + AccountColumns.LOGO_ID + "="
                    + Tables.MEDIA + "." + MediaColumns.ID
                    + " WHERE " + AccountColumns.SUBSCRIPTION_STATUS
                    + "=" + Constants.SUBSCRIPTION_STATUS_YES + ";");

            db.execSQL("CREATE VIEW " + Tables.CCS_ACCOUNT_INFO_VIEW + " AS SELECT "
                    + Tables.ACCOUNT + "." + AccountColumns.ID
                    + " AS " + CcsAccountInfoColumns.ID + ", "
                    + AccountColumns.UUID + " AS " + CcsAccountInfoColumns.ACCOUNT + ", "
                    + Tables.ACCOUNT + "." + AccountColumns.NAME
                    + " AS " + CcsAccountInfoColumns.NAME + ", "
                    + Tables.MEDIA + "." + MediaColumns.URL
                    + " AS " + CcsAccountInfoColumns.PORTRAIT + ", "
                    + Tables.MEDIA + "." + MediaColumns.PATH
                    + " AS " + CcsAccountColumns.PORTRAIT_PATH + ", "
                    + AccountColumns.INTRODUCTION
                    + " AS " + CcsAccountInfoColumns.BREIF_INTRODUCTION + ", "
                    + AccountColumns.SUBSCRIPTION_STATUS
                    + " AS " + CcsAccountInfoColumns.STATE + ", "
                    + AccountColumns.MENU
                    + " AS " + CcsAccountInfoColumns.CONFIG
                    + " FROM " + Tables.ACCOUNT + " LEFT OUTER JOIN " + Tables.MEDIA
                    + " ON " + Tables.ACCOUNT + "." + AccountColumns.LOGO_ID + "="
                    + Tables.MEDIA + "." + MediaColumns.ID + ";");

            /* Internal */
            db.execSQL("CREATE TABLE " + Tables.STATE + "("
                    + StateColumns.ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + StateColumns.INITIALIZED + " INTEGER NOT NULL DEFAULT 0);");

            ContentValues cv = new ContentValues();
            cv.put(StateColumns.INITIALIZED, Constants.INIT_NO);
            db.insert(Tables.STATE, null, cv);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // This is the first version, so simply recreate tables.

            // common views
            db.execSQL("DROP VIEW IF EXISTS " + Tables.MESSAGE_HISTORY_SUMMARY_VIEW);

            // CMCC only views
            db.execSQL("DROP VIEW IF EXISTS " + Tables.CCS_MESSAGE_VIEW);
            db.execSQL("DROP VIEW IF EXISTS " + Tables.CCS_ACCOUNT_VIEW);
            db.execSQL("DROP VIEW IF EXISTS " + Tables.CCS_ACCOUNT_INFO_VIEW);

            // common tables
            db.execSQL("DROP TABLE IF EXISTS " + Tables.ACCOUNT);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.MESSAGE);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.MEDIA_BASIC);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.MEDIA_ARTICLE);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.MEDIA);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.SEARCH);

            // CMCC only tables
            db.execSQL("DROP TABLE IF EXISTS " + Tables.CCS_HISTORY);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.CCS_ACCOUNT_SEARCH);

            // Internal
            db.execSQL("DROP TABLE IF EXISTS " + Tables.STATE);

            onCreate(db);
        }

    }

    private PADatabaseHelper mDBHelper;

    @Override
    public boolean onCreate() {
        mDBHelper = new PADatabaseHelper(getContext(), DATABASE_NAME, null, DATABASE_VERSION);
        return true;
    }

    @Override
    public String getType(Uri uri) {
        final int match = URI_MATCHER.match(uri);
        switch (match) {
        case UriCode.ACCOUNTS:
            return "vnd.android.cursor.dir/pa_account";
        case UriCode.ACCOUNTS_ID:
            return "vnd.android.cursor.item/pa_account";
        case UriCode.MESSAGES:
            return "vnd.android.cursor.dir/pa_message";
        case UriCode.MESSAGES_ID:
            return "vnd.android.cursor.item/pa_message";
        case UriCode.MEDIA_BASICS:
            return "vnd.android.cursor.dir/pa_media_basic";
        case UriCode.MEDIA_BASICS_ID:
            return "vnd.android.cursor.item/pa_media_basic";
        case UriCode.MEDIA_ARTICLES:
            return "vnd.android.cursor.dir/pa_media_article";
        case UriCode.MEDIA_ARTICLES_ID:
            return "vnd.android.cursor.item/pa_media_article";
        case UriCode.MEDIAS:
            return "vnd.android.cursor.dir/pa_media";
        case UriCode.MEDIAS_ID:
            return "vnd.android.cursor.item/pa_media";
        case UriCode.SEARCH:
        case UriCode.MESSAGE_HISTORY_SUMMARIES:
            return "vnd.android.cursor.item/pa_message_history_summary";
        case UriCode.STATE:
            return "vnd.android.cursor.item/pa_state";

            /* CMCC only */
        case UriCode.CCS_MESSAGES:
            return "vnd.android.cursor.dir/ccs_pa_message";
        case UriCode.CCS_HISTORY:
            return "vnd.android.cursor.dir/ccs_pa_history";
        case UriCode.CCS_ACCOUNTS:
            return "vnd.android.cursor.dir/ccs_pa_account";
        case UriCode.CCS_ACCOUNT_INFOS:
            return "vnd.android.cursor.dir/ccs_pa_account_info";
        case UriCode.CCS_ACCOUNT_SEARCH:
            return "vnd.android.cursor.dir/ccs_pa_account_search";
        default:
            throw new IllegalArgumentException("Invalid URI");
        }
    }

    static class Pair {
        public long messageId;
        public long accountId;

        public Pair(long msgId, long acctId) {
            this.messageId = msgId;
            this.accountId = acctId;
        }
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        Log.d(TAG, "delete: " + uri);
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        final int match = URI_MATCHER.match(uri);
        int count = 0;
        db.beginTransaction();
        try {
            switch (match) {
            case UriCode.ACCOUNTS:
                count = db.delete(Tables.ACCOUNT, where, whereArgs);
                break;
            case UriCode.ACCOUNTS_ID:
                count = db.delete(Tables.ACCOUNT,
                        AccountColumns.ID + "=" + uri.getPathSegments().get(1)
                        + (TextUtils.isEmpty(where) ? null : " AND (" + where + ")"),
                        whereArgs);
                break;
            case UriCode.MESSAGES:
                Cursor c = null;
                List<Pair> infoList = new ArrayList<Pair>();
                try {
                    c = db.query(Tables.MESSAGE,
                            new String[] {
                                MessageColumns.ID, MessageColumns.ACCOUNT_ID },
                            where, whereArgs, null, null, null);
                    if (c != null && c.getCount() > 0) {
                        final int messageIdIndex = c.getColumnIndexOrThrow(MessageColumns.ID);
                        final int accountIdIndex =
                                c.getColumnIndexOrThrow(MessageColumns.ACCOUNT_ID);
                        c.moveToFirst();
                        do {
                            infoList.add(new Pair(
                                    c.getLong(messageIdIndex), c.getLong(accountIdIndex)));
                        } while (c.moveToNext());
                    }
                } finally {
                    if (c != null) {
                        c.close();
                    }
                }
                count = db.delete(Tables.MESSAGE, where, whereArgs);
                // sanity check
                if (count != infoList.size()) {
                    Log.w(TAG, "Count of messages updated doesn't match.");
                }
                SQLiteStatement deleteSearchStatement =
                        db.compileStatement("DELETE FROM " + Tables.SEARCH + " WHERE "
                        + SearchColumns.ROWID + "=?");
                for (Pair pair : infoList) {
                    deleteSearchStatement.bindLong(1, pair.messageId);
                    deleteSearchStatement.execute();
                    updateLastMessageForAccountTable(db, pair.accountId);
                }
                break;
            case UriCode.MESSAGES_ID:
                final Long id = Long.parseLong(uri.getPathSegments().get(1));
                count = db.delete(Tables.MESSAGE, AccountColumns.ID + "=" + id
                        + (TextUtils.isEmpty(where) ? null : " AND (" + where + ")"), whereArgs);
                db.delete(Tables.SEARCH, SearchColumns.ROWID + "=" + id, null);
                break;
            case UriCode.MEDIA_BASICS:
                count = db.delete(Tables.MEDIA_BASIC, where, whereArgs);
                break;
            case UriCode.MEDIA_BASICS_ID:
                count = db.delete(Tables.MEDIA_BASIC,
                        AccountColumns.ID + "=" + uri.getPathSegments().get(1)
                        + (TextUtils.isEmpty(where) ? null : " AND (" + where + ")"), whereArgs);
                break;
            case UriCode.MEDIA_ARTICLES:
                count = db.delete(Tables.MEDIA_ARTICLE, where, whereArgs);
                break;
            case UriCode.MEDIA_ARTICLES_ID:
                count = db.delete(Tables.MEDIA_ARTICLE,
                        AccountColumns.ID + "=" + uri.getPathSegments().get(1)
                        + (TextUtils.isEmpty(where) ? null : " AND (" + where + ")"), whereArgs);
                break;
            case UriCode.MEDIAS:
                count = db.delete(Tables.MEDIA, where, whereArgs);
                break;
            case UriCode.MEDIAS_ID:
                count = db.delete(Tables.MEDIA,
                        AccountColumns.ID + "=" + uri.getPathSegments().get(1)
                        + (TextUtils.isEmpty(where) ? null : " AND (" + where + ")"), whereArgs);
                break;
            case UriCode.SEARCH:
                throw new UnsupportedOperationException("Only query operation is supported.");

                /* CMCC only */
            case UriCode.CCS_MESSAGES:
            case UriCode.CCS_ACCOUNTS:
            case UriCode.CCS_ACCOUNT_INFOS:
                throw new UnsupportedOperationException("Only query operation is supported.");
            case UriCode.CCS_HISTORY:
                count = db.delete(Tables.CCS_HISTORY, where, whereArgs);
                break;
            case UriCode.CCS_ACCOUNT_SEARCH:
                // FIXME throw exception here and add a new private uri for inserting by PAService
                count = db.delete(Tables.CCS_ACCOUNT_SEARCH, where, whereArgs);
                break;

            default:
                throw new IllegalArgumentException("Invalid URI");
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.d(TAG, "insert: " + uri);
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        final int match = URI_MATCHER.match(uri);
        db.beginTransaction();
        try {
            long id = -1;
            switch (match) {
            case UriCode.ACCOUNTS:
                id = db.insert(Tables.ACCOUNT, null, values);
                uri = ContentUris.withAppendedId(uri, id);
                break;
            case UriCode.MESSAGES:
                id = db.insert(Tables.MESSAGE, null, values);
                uri = ContentUris.withAppendedId(uri, id);
                // update search table
                updateMessageSearchTable(db, id, values, true);
                // update last_message for account
                long accountId = values.getAsLong(MessageColumns.ACCOUNT_ID);
                updateLastMessageForAccountTable(db, accountId);
                break;
            case UriCode.MEDIA_BASICS:
                id = db.insert(Tables.MEDIA_BASIC, null, values);
                uri = ContentUris.withAppendedId(uri, id);
                break;
            case UriCode.MEDIA_ARTICLES:
                id = db.insert(Tables.MEDIA_ARTICLE, null, values);
                uri = ContentUris.withAppendedId(uri, id);
                break;
            case UriCode.MEDIAS:
                id = db.insert(Tables.MEDIA, null, values);
                uri = ContentUris.withAppendedId(uri, id);
                break;
            case UriCode.SEARCH:
                throw new UnsupportedOperationException("Only query operation is supported.");

                /* CMCC only */
            case UriCode.CCS_MESSAGES:
            case UriCode.CCS_ACCOUNTS:
            case UriCode.CCS_ACCOUNT_INFOS:
                throw new UnsupportedOperationException("Only query operation is supported.");
            case UriCode.CCS_HISTORY:
                id = db.insert(Tables.CCS_HISTORY, null, values);
                uri = ContentUris.withAppendedId(uri, id);
                break;
            case UriCode.CCS_ACCOUNT_SEARCH:
                // FIXME throw exception here and add a new private uri for inserting by PAService
                id = db.insert(Tables.CCS_ACCOUNT_SEARCH, null, values);
                uri = ContentUris.withAppendedId(uri, id);
                break;

            default:
                throw new IllegalArgumentException("Invalid URI");
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return uri;
    }

    @Override
    public Cursor query(Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        Log.d(TAG, "query: " + uri);
        if (projection != null) {
            projection = projection.clone();
        }
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        final int match = URI_MATCHER.match(uri);
        switch (match) {
        case UriCode.ACCOUNTS:
            if (detectAndWrapLogoFields(projection)) {
                final String tableString = Tables.ACCOUNT + " LEFT OUTER JOIN " + Tables.MEDIA
                        + " ON(" + Tables.ACCOUNT + "." + AccountColumns.LOGO_ID
                        + "=" + Tables.MEDIA + "." + MediaColumns.ID + ")";
                queryBuilder.setTables(tableString);
            } else {
                queryBuilder.setTables(Tables.ACCOUNT);
            }
            break;
        case UriCode.ACCOUNTS_ID:
            if (detectAndWrapLogoFields(projection)) {
                queryBuilder.setTables(Tables.ACCOUNT
                        + " LEFT OUTER JOIN " + Tables.MEDIA
                        + " ON(" + Tables.ACCOUNT + "." + AccountColumns.LOGO_ID +
                        "=" + Tables.MEDIA + "." + MediaColumns.ID + ")");
            } else {
                queryBuilder.setTables(Tables.ACCOUNT);
            }
            queryBuilder.appendWhere(AccountColumns.ID + "=" + uri.getPathSegments().get(1));
            break;
        case UriCode.MESSAGES:
            String flag = uri.getQueryParameter(PAContract.MESSAGES_PARAM_INCLUDING_SYSTEM);
            int includingSystem = Constants.IS_SYSTEM_NO;
            try {
                includingSystem = flag == null ? Constants.IS_SYSTEM_NO : Integer.parseInt(flag);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                Log.e(TAG, "Invalid parameter: " + includingSystem + ", treat it as false");
            }

            flag = uri.getQueryParameter(PAContract.MESSAGES_PARAM_INCLUDING_DELETED);
            int includingDeleted = Constants.INCLUDING_DELETED_NO;
            try {
                includingDeleted = flag == null ?
                        Constants.INCLUDING_DELETED_NO : Integer.parseInt(flag);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                Log.e(TAG, "Invalid parameter: " + includingDeleted + ", treat it as false");
            }

            if (includingSystem != Constants.IS_SYSTEM_YES) {
                if (selectionArgs != null) {
                    String[] newSelectionArgs = new String[selectionArgs.length + 1];
                    for (int i = 0; i < selectionArgs.length; ++i) {
                        newSelectionArgs[i] = selectionArgs[i];
                    }
                    newSelectionArgs[selectionArgs.length] =
                            Integer.toString(Constants.IS_SYSTEM_NO);
                    selectionArgs = newSelectionArgs;
                } else {
                    selectionArgs = new String[] { Integer.toString(Constants.IS_SYSTEM_NO) };
                }
                selection = "(" + selection + ") AND (" + MessageColumns.SYSTEM + "=?)";
            }
            queryBuilder.setTables(Tables.MESSAGE);
            if (includingDeleted == Constants.INCLUDING_DELETED_NO) {
                queryBuilder.appendWhere(MessageColumns.DELETED + "=" + Constants.DELETED_NO);
            }
            break;
        case UriCode.MESSAGES_ID:
            queryBuilder.setTables(Tables.MESSAGE);
            queryBuilder.appendWhere(MessageColumns.ID + "=" + uri.getPathSegments().get(1));
            break;
        case UriCode.MEDIA_BASICS:
            queryBuilder.setTables(Tables.MEDIA_BASIC);
            break;
        case UriCode.MEDIA_BASICS_ID:
            queryBuilder.setTables(Tables.MEDIA_BASIC);
            queryBuilder.appendWhere(MediaBasicColumns.ID + "=" + uri.getPathSegments().get(1));
            break;
        case UriCode.MEDIA_ARTICLES:
            queryBuilder.setTables(Tables.MEDIA_ARTICLE);
            break;
        case UriCode.MEDIA_ARTICLES_ID:
            queryBuilder.setTables(Tables.MEDIA_ARTICLE);
            queryBuilder.appendWhere(MediaArticleColumns.ID + "=" + uri.getPathSegments().get(1));
            break;
        case UriCode.MEDIAS:
            queryBuilder.setTables(Tables.MEDIA);
            break;
        case UriCode.MEDIAS_ID:
            queryBuilder.setTables(Tables.MEDIA);
            Log.d(TAG, "Uri is " + uri);
            queryBuilder.appendWhere(MediaColumns.ID + "=" + uri.getPathSegments().get(1));
            break;
        case UriCode.SEARCH:
            final String keywords = uri.getQueryParameter(PAContract.SEARCH_PARAM_KEYWORD);
            if (TextUtils.isEmpty(keywords)) {
                throw new IllegalArgumentException("Empty keywords");
            }
            projection = wrapSearchFields(projection);
            queryBuilder.setTables(
                    Tables.MESSAGE + " LEFT OUTER JOIN " + Tables.ACCOUNT + " ON (" +
                    Tables.MESSAGE + "." + MessageColumns.ACCOUNT_ID + "=" + Tables.ACCOUNT +
                    "." + AccountColumns.ID + ") LEFT OUTER JOIN " + Tables.MEDIA +
                    " ON (" + Tables.ACCOUNT + "." + AccountColumns.LOGO_ID + "=" +
                    Tables.MEDIA + "." + MediaColumns.ID + ")");
            queryBuilder.appendWhere(
                    Tables.MESSAGE + "." + MessageColumns.ID + " IN ( SELECT "
                    + SearchColumns.ROWID + " FROM " + Tables.SEARCH
                    + " WHERE " + SearchColumns.MESSAGE_TEXT + " LIKE '%" + keywords + "%' )");
            break;
        case UriCode.MESSAGE_HISTORY_SUMMARIES:
            queryBuilder.setTables(Tables.MESSAGE_HISTORY_SUMMARY_VIEW);
            break;
        /* CMCC only */
        case UriCode.CCS_MESSAGES:
            queryBuilder.setTables(Tables.CCS_MESSAGE_VIEW);
            break;
        case UriCode.CCS_HISTORY:
            queryBuilder.setTables(Tables.CCS_HISTORY);
            break;
        case UriCode.CCS_ACCOUNTS:
            if (projection == null || projection.length == 0) {
                projection = CCS_ACCOUNT_PROJECTION.clone();
            }
            if (containsColumn(projection, CcsAccountColumns.PORTRAIT_TYPE)) {
                SQLiteDatabase db = mDBHelper.getReadableDatabase();
                StringBuilder sb = new StringBuilder("SELECT ");
                for (String p : projection) {
                    sb.append(p).append(",");
                }
                sb.append("CASE ")
                  .append("WHEN ").append(CcsAccountColumns.PORTRAIT)
                  .append(" LIKE '%.jpg' THEN 'JPG' ")
                  .append("WHEN ").append(CcsAccountColumns.PORTRAIT)
                  .append(" LIKE '%.jpeg' THEN 'JPG' ")
                  .append("WHEN ").append(CcsAccountColumns.PORTRAIT)
                  .append(" LIKE '%.png' THEN 'PNG' ")
                  .append("WHEN ").append(CcsAccountColumns.PORTRAIT)
                  .append(" LIKE '%.bmp' THEN 'BMP' ")
                  .append("WHEN ").append(CcsAccountColumns.PORTRAIT)
                  .append(" LIKE '%.gif' THEN 'GIF' ")
                  .append("ELSE NULL END ")
                  .append(CcsAccountColumns.PORTRAIT_TYPE)
                  .append(" FROM ")
                  .append(Tables.CCS_ACCOUNT_VIEW)
                  .append(" WHERE ")
                  .append(selection)
                  .append(" ORDER BY ")
                  .append(sortOrder);
                String queryString = sb.toString();
                Log.d(TAG, "CCS Account query string: " + queryString);
                Cursor c = db.rawQuery(queryString, selectionArgs);
                if (c != null) {
                    c.setNotificationUri(getContext().getContentResolver(), uri);
                } else {
                    throw new Error("Failed to query DB");
                }
                return c;
            } else {
                queryBuilder.setTables(Tables.CCS_ACCOUNT_VIEW);
            }
            break;
        case UriCode.CCS_ACCOUNT_INFOS:
            if (projection == null || projection.length == 0) {
                projection = CCS_ACCOUNT_INFO_PROJECTION.clone();
            }
            if (containsColumn(projection, CcsAccountInfoColumns.PORTRAIT_TYPE)) {
                SQLiteDatabase db = mDBHelper.getReadableDatabase();
                StringBuilder sb = new StringBuilder("SELECT ");
                for (String p : projection) {
                    sb.append(p).append(",");
                }
                sb.append("CASE ")
                  .append("WHEN ").append(CcsAccountInfoColumns.PORTRAIT)
                  .append(" LIKE '%.jpg' THEN 'JPG' ")
                  .append("WHEN ").append(CcsAccountInfoColumns.PORTRAIT)
                  .append(" LIKE '%.jpeg' THEN 'JPG' ")
                  .append("WHEN ").append(CcsAccountInfoColumns.PORTRAIT)
                  .append(" LIKE '%.png' THEN 'PNG' ")
                  .append("WHEN ").append(CcsAccountInfoColumns.PORTRAIT)
                  .append(" LIKE '%.bmp' THEN 'BMP' ")
                  .append("WHEN ").append(CcsAccountInfoColumns.PORTRAIT)
                  .append(" LIKE '%.gif' THEN 'GIF' ")
                  .append("ELSE NULL END ")
                  .append(CcsAccountInfoColumns.PORTRAIT_TYPE)
                  .append(" FROM ")
                  .append(Tables.CCS_ACCOUNT_VIEW)
                  .append(" WHERE ")
                  .append(selection)
                  .append(" ORDER BY ")
                  .append(sortOrder);
                String queryString = sb.toString();
                Log.d(TAG, "CCS Account Info query string: " + queryString);
                Cursor c = db.rawQuery(queryString, selectionArgs);
                if (c != null) {
                    c.setNotificationUri(getContext().getContentResolver(), uri);
                } else {
                    throw new Error("Failed to query DB");
                }
                return c;
            } else {
                queryBuilder.setTables(Tables.CCS_ACCOUNT_INFO_VIEW);
            }
            break;
        case UriCode.CCS_ACCOUNT_SEARCH:
            queryBuilder.setTables(Tables.CCS_ACCOUNT_SEARCH);
            break;
        case UriCode.STATE:
            queryBuilder.setTables(Tables.STATE);
            break;
        default:
            throw new IllegalArgumentException("Invalid URI");
        }

        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        Cursor c = queryBuilder.query(
                db, projection, selection, selectionArgs, null, null, sortOrder);
        if (c != null) {
            c.setNotificationUri(getContext().getContentResolver(), uri);
        } else {
            throw new Error("Failed to query DB");
        }

        return c;
    }

    private boolean containsColumn(String[] projection, String column) {
        for (String p : projection) {
            if (column.equals(p)) {
                return true;
            }
        }
        return false;
    }

    private boolean detectAndWrapLogoFields(String[] projection) {
        if (projection == null) {
            return true;
        }
        boolean flag = false;
        for (int i = 0; i < projection.length; ++i) {
            String p = projection[i];
            if (p.equals(AccountColumns.ID)) {
                projection[i] = Tables.ACCOUNT + "." + AccountColumns.ID
                        + " AS " + AccountColumns.ID;
            } else if (p.equals(AccountColumns.LOGO_PATH)) {
                projection[i] = Tables.MEDIA + "." + MediaColumns.PATH
                        + " AS " + AccountColumns.LOGO_PATH;
                flag = true;
            } else if (p.equals(AccountColumns.LOGO_URL)) {
                projection[i] = Tables.MEDIA + "." + MediaColumns.URL
                        + " AS " + AccountColumns.LOGO_URL;
                flag = true;
            }
        }
        return flag;
    }

    private static final Map<String, String> SEARCH_COLUMN_MAPPING = new HashMap<String, String>();
    static {
        SEARCH_COLUMN_MAPPING.put(
                SearchColumns.ACCOUNT_ID,
                Tables.ACCOUNT + "." + AccountColumns.ID + " AS " + SearchColumns.ACCOUNT_ID);
        SEARCH_COLUMN_MAPPING.put(
                SearchColumns.ACCOUNT_NAME,
                Tables.ACCOUNT + "." + AccountColumns.NAME + " AS " + SearchColumns.ACCOUNT_NAME);
        SEARCH_COLUMN_MAPPING.put(
                SearchColumns.ACCOUNT_LOGO_ID,
                Tables.ACCOUNT + "." + AccountColumns.LOGO_ID +
                " AS " + SearchColumns.ACCOUNT_LOGO_ID);
        SEARCH_COLUMN_MAPPING.put(
                SearchColumns.ACCOUNT_LOGO_PATH,
                Tables.MEDIA + "." + MediaColumns.PATH +
                " AS " + SearchColumns.ACCOUNT_LOGO_PATH);
        SEARCH_COLUMN_MAPPING.put(
                SearchColumns.ACCOUNT_LOGO_URL,
                Tables.MEDIA + "." + MediaColumns.URL +
                " AS " + SearchColumns.ACCOUNT_LOGO_URL);
        SEARCH_COLUMN_MAPPING.put(
                SearchColumns.MESSAGE_TIMESTAMP,
                Tables.MESSAGE + "." + MessageColumns.TIMESTAMP +
                " AS " + SearchColumns.MESSAGE_TIMESTAMP);
        SEARCH_COLUMN_MAPPING.put(
                SearchColumns.MESSAGE_ID,
                Tables.MESSAGE + "." + MessageColumns.ID + " AS " + SearchColumns.MESSAGE_ID);
        SEARCH_COLUMN_MAPPING.put(
                SearchColumns.MESSAGE_TEXT,
                Tables.MESSAGE + "." + MessageColumns.TEXT + " AS " + SearchColumns.MESSAGE_TEXT);
        SEARCH_COLUMN_MAPPING.put(
                SearchColumns.MESSAGE_SUMMARY,
                Tables.MESSAGE + "." + MessageColumns.SMS_DIGEST +
                " AS " + SearchColumns.MESSAGE_SUMMARY);
    }

    private static final String[] SEARCH_PROJECTION = new String[] {
        Tables.MESSAGE + "." + MessageColumns.ID + " AS " + SearchColumns.ID,
        Tables.ACCOUNT + "." + AccountColumns.ID + " AS " + SearchColumns.ACCOUNT_ID,
        Tables.ACCOUNT + "." + AccountColumns.NAME + " AS " + SearchColumns.ACCOUNT_NAME,
        Tables.ACCOUNT + "." + AccountColumns.LOGO_ID + " AS " + SearchColumns.ACCOUNT_LOGO_ID,
        Tables.MEDIA + "." + MediaColumns.PATH + " AS " + SearchColumns.ACCOUNT_LOGO_PATH,
        Tables.MEDIA + "." + MediaColumns.URL + " AS " + SearchColumns.ACCOUNT_LOGO_URL,
        Tables.MESSAGE + "." + MessageColumns.TIMESTAMP + " AS " + SearchColumns.MESSAGE_TIMESTAMP,
        Tables.MESSAGE + "." + MessageColumns.ID + " AS " + SearchColumns.MESSAGE_ID,
        Tables.MESSAGE + "." + MessageColumns.TEXT + " AS " + SearchColumns.MESSAGE_TEXT,
        Tables.MESSAGE + "." + MessageColumns.SMS_DIGEST + " AS " + SearchColumns.MESSAGE_SUMMARY,
    };

    private static final String[] CCS_ACCOUNT_PROJECTION = new String[] {
        CcsAccountColumns.ID,
        CcsAccountColumns.ACCOUNT,
        CcsAccountColumns.NAME,
        CcsAccountColumns.PORTRAIT,
        CcsAccountColumns.PORTRAIT_TYPE,
        CcsAccountColumns.BREIF_INTRODUCTION,
        CcsAccountColumns.PORTRAIT_PATH,
    };

    private static final String[] CCS_ACCOUNT_INFO_PROJECTION = new String[] {
        CcsAccountInfoColumns.ID,
        CcsAccountInfoColumns.ACCOUNT,
        CcsAccountInfoColumns.NAME,
        CcsAccountInfoColumns.PORTRAIT,
        CcsAccountInfoColumns.PORTRAIT_TYPE,
        CcsAccountInfoColumns.BREIF_INTRODUCTION,
        CcsAccountInfoColumns.PORTRAIT_PATH,
        CcsAccountInfoColumns.STATE,
        CcsAccountInfoColumns.CONFIG,
    };

    private String[] wrapSearchFields(String[] projection) {
        if (projection == null) {
            return SEARCH_PROJECTION;
        }
        String[] result = new String[projection.length];
        for (int i = 0; i < projection.length; ++i) {
            String p = SEARCH_COLUMN_MAPPING.get(projection[i]);
            if (p != null) {
                result[i] = p;
            } else {
                result[i] = projection[i];
            }
        }
        return result;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        Log.d(TAG, "update(" + uri + ", " + where);
        int count = 0;
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        final int match = URI_MATCHER.match(uri);
        db.beginTransaction();
        try {
            switch (match) {
            case UriCode.ACCOUNTS:
                count = db.update(Tables.ACCOUNT, values, where, whereArgs);
                break;
            case UriCode.ACCOUNTS_ID:
                count = db.update(Tables.ACCOUNT,
                        values, AccountColumns.ID + "=" + uri.getPathSegments().get(1), null);
                break;
            case UriCode.MESSAGES:
                Cursor c = null;
                List<Pair> infoList = new ArrayList<Pair>();
                try {
                    c = db.query(Tables.MESSAGE, new String[] {
                            MessageColumns.ID, MessageColumns.ACCOUNT_ID },
                            where, whereArgs, null, null, null);
                    if (c != null && c.getCount() > 0) {
                        final int accountIdIndex =
                                c.getColumnIndexOrThrow(MessageColumns.ACCOUNT_ID);
                        final int messageIdIndex =
                                c.getColumnIndexOrThrow(MessageColumns.ID);
                        c.moveToFirst();
                        do {
                            infoList.add(new Pair(
                                    c.getLong(messageIdIndex), c.getLong(accountIdIndex)));
                        } while (c.moveToNext());
                    }
                } finally {
                    if (c != null) {
                        c.close();
                    }
                }
                count = db.update(Tables.MESSAGE, values, where, whereArgs);
                // sanity check
                if (count != c.getCount()) {
                    Log.w(TAG, "Count of messages updated doesn't match.");
                }
                for (Pair pair : infoList) {
                    updateLastMessageForAccountTable(db, pair.accountId);
                    updateMessageSearchTable(db, pair.messageId);

                }
                break;
            case UriCode.MESSAGES_ID:
                final long id = Long.parseLong(uri.getPathSegments().get(1));
                count = db.update(Tables.MESSAGE, values, MessageColumns.ID + "=" + id, null);
                updateMessageSearchTable(db, id);
                break;
            case UriCode.MEDIA_BASICS:
                count = db.update(Tables.MEDIA_BASIC, values, where, whereArgs);
                break;
            case UriCode.MEDIA_BASICS_ID:
                count = db.update(Tables.MEDIA_BASIC, values, MediaBasicColumns.ID + "="
                        + uri.getPathSegments().get(1), null);
                break;
            case UriCode.MEDIA_ARTICLES:
                count = db.update(Tables.MEDIA_ARTICLE, values, where, whereArgs);
                break;
            case UriCode.MEDIA_ARTICLES_ID:
                count = db.update(Tables.MEDIA_ARTICLE, values, MediaArticleColumns.ID + "="
                        + uri.getPathSegments().get(1), null);
                break;
            case UriCode.MEDIAS:
                count = db.update(Tables.MEDIA, values, where, whereArgs);
                break;
            case UriCode.MEDIAS_ID:
                count = db.update(Tables.MEDIA, values,
                        MediaColumns.ID + "=" + uri.getPathSegments().get(1), null);
                break;
            case UriCode.SEARCH:
                throw new UnsupportedOperationException("Only query operation is supported.");

                /* CMCC only */
            case UriCode.CCS_MESSAGES:
            case UriCode.CCS_ACCOUNTS:
            case UriCode.CCS_ACCOUNT_INFOS:
                throw new UnsupportedOperationException("Only query operation is supported.");
            case UriCode.CCS_HISTORY:
                count = db.update(Tables.CCS_HISTORY, values, where, whereArgs);
                break;
            case UriCode.CCS_ACCOUNT_SEARCH:
                // FIXME throw exception here and add a new private uri for inserting by PAService
                count = db.update(Tables.CCS_ACCOUNT_SEARCH, values, where, whereArgs);
                break;
            case UriCode.STATE:
                count = db.update(Tables.STATE, values, where, whereArgs);
                break;

            default:
                throw new IllegalArgumentException("Invalid URI");
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    // FIXME performance issue
    private void updateMessageSearchTable(SQLiteDatabase db, Long id) {
        Cursor c = null;
        try {
            c = db.query(
                    Tables.MESSAGE,
                    new String[] {
                            MessageColumns.TYPE,
                            MessageColumns.TEXT,
                            MessageColumns.DATA1,
                            MessageColumns.DATA2,
                            MessageColumns.DATA3,
                            MessageColumns.DATA4,
                            MessageColumns.DATA5, },
                    MessageColumns.ID + "=" + id,
                    null,
                    null,
                    null,
                    null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                ContentValues cv = new ContentValues();
                cv.put(MessageColumns.TYPE,
                        c.getInt(c.getColumnIndexOrThrow(MessageColumns.TYPE)));
                cv.put(MessageColumns.TEXT,
                        c.getString(c.getColumnIndexOrThrow(MessageColumns.TEXT)));
                cv.put(MessageColumns.DATA1,
                        c.getLong(c.getColumnIndexOrThrow(MessageColumns.DATA1)));
                cv.put(MessageColumns.DATA2,
                        c.getLong(c.getColumnIndexOrThrow(MessageColumns.DATA2)));
                cv.put(MessageColumns.DATA3,
                        c.getLong(c.getColumnIndexOrThrow(MessageColumns.DATA3)));
                cv.put(MessageColumns.DATA4,
                        c.getLong(c.getColumnIndexOrThrow(MessageColumns.DATA4)));
                cv.put(MessageColumns.DATA5,
                        c.getLong(c.getColumnIndexOrThrow(MessageColumns.DATA5)));
                updateMessageSearchTable(db, id, cv, false);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    // FIXME performance issue
    private void updateLastMessageForAccountTable(SQLiteDatabase db, long accountId) {
        if (accountId != Constants.INVALID) {
            long messageId = Constants.INVALID;
            Cursor c = null;
            try {
                c = db.rawQuery("SELECT " + MessageColumns.ID + " FROM " + Tables.MESSAGE
                        + " WHERE " + MessageColumns.ACCOUNT_ID + "=" + accountId
                        + " ORDER BY " + MessageColumns.TIMESTAMP
                        + " DESC LIMIT 1", null);
                if (c != null && c.getCount() > 0) {
                    c.moveToFirst();
                    messageId = c.getLong(c.getColumnIndexOrThrow(MessageColumns.ID));
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
            db.execSQL("UPDATE " + Tables.ACCOUNT + " SET " + AccountColumns.LAST_MESSAGE
                    + "=" + messageId + " WHERE " + AccountColumns.ID + "=" + accountId);
        }
    }

    /**
     *
     * @param db
     * @param id
     * @param values
     *            Values should contain as least type, text and data% columns.
     */
    private void updateMessageSearchTable(
            SQLiteDatabase db, long id, ContentValues values, boolean insertOnly) {
        // update search table
        ContentValues cv = new ContentValues();
        cv.put(SearchColumns.ROWID, id);
        Cursor c = null;
        final int type = values.getAsInteger(MessageColumns.TYPE);
        switch (type) {
        case Constants.MEDIA_TYPE_TEXT:
            cv.put(SearchColumns.MESSAGE_TEXT, values.getAsString(MessageColumns.TEXT));
            break;
        case Constants.MEDIA_TYPE_PICTURE:
        case Constants.MEDIA_TYPE_VIDEO:
        case Constants.MEDIA_TYPE_AUDIO:
            c = null;
            try {
                c = db.query(
                        Tables.MEDIA_BASIC,
                        new String[] { MediaBasicColumns.TITLE },
                        MediaBasicColumns.ID + "=" + values.getAsLong(MessageColumns.DATA1),
                        null,
                        null,
                        null,
                        null);
                if (c.getCount() > 0) {
                    c.moveToFirst();
                    cv.put(
                            SearchColumns.MESSAGE_TEXT,
                            c.getString(c.getColumnIndexOrThrow(MediaBasicColumns.TITLE)));
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
            break;
        case Constants.MEDIA_TYPE_SINGLE_ARTICLE:
        case Constants.MEDIA_TYPE_MULTIPLE_ARTICLE:
            c = null;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < PAContract.MESSAGE_DATA_COLUMN_LIST.length; ++i) {
                try {
                    c = db.query(
                            Tables.MEDIA_ARTICLE,
                            new String[] {
                                    MediaArticleColumns.TITLE,
                                    MediaArticleColumns.TEXT },
                            MediaBasicColumns.ID + "="
                                    + values.getAsLong(PAContract.MESSAGE_DATA_COLUMN_LIST[i]),
                            null,
                            null,
                            null,
                            null);
                    if (c != null && c.getCount() > 0) {
                        c.moveToFirst();
                        sb.append(c.getString(c.getColumnIndexOrThrow(MediaArticleColumns.TITLE)))
                          .append(SEARCH_TEXT_SEPERATOR);
                        sb.append(c.getString(c.getColumnIndexOrThrow(MediaArticleColumns.TEXT)))
                          .append(SEARCH_TEXT_SEPERATOR);
                    }
                } finally {
                    if (c != null) {
                        c.close();
                    }
                }
            }
            cv.put(SearchColumns.MESSAGE_TEXT, sb.toString());
            break;
        case Constants.MEDIA_TYPE_SMS:
        case Constants.MEDIA_TYPE_GEOLOC:
        case Constants.MEDIA_TYPE_VCARD:
            cv.put(SearchColumns.MESSAGE_TEXT, values.getAsString(MessageColumns.SMS_DIGEST));
            break;
        default:
            throw new IllegalArgumentException("Invalid message type.");
        }
        if (!insertOnly) {
            db.delete(Tables.SEARCH, SearchColumns.ROWID + "=" + id, null);
        }
        long insertId = db.insert(Tables.SEARCH, null, cv);
        if (insertId == -1) {
            Log.e(TAG, "Insert to search table failed");
        }
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentProviderResult[] results = super.applyBatch(operations);
            db.setTransactionSuccessful();
            return results;
        } finally {
            db.endTransaction();
        }
    }

}
