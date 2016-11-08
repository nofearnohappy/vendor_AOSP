package com.hesine.nmsg.business.dao;

import java.io.File;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.hesine.nmsg.Application;
import com.hesine.nmsg.common.MLog;

public final class LocalStore {

    public static final String TABLE_MESSAGES = "messages";
    public static final String TABLE_MESSAGE_ITEM = "message_item";
    public static final String TABLE_ACCOUNTS = "accounts";
    public static final String TABLE_USERINFO = "userinfo";    
    public static final String TABLE_MESSAGES_QUEUE = "message_queue"; 

    private static final int DB_VERSION = 14;
    public static final String DB_NAME = "localstore.db";

    private static LocalStore mLocalStore = null;
    private String mDbDir;
    private SQLiteDatabase mDatabase = null;

    public static synchronized  LocalStore instance() {
        if (mLocalStore == null) {
            mLocalStore = new LocalStore();
        }
        return mLocalStore;
    }

    private LocalStore() {
        mDbDir = Application.getInstance().getApplicationContext().getFilesDir().getAbsolutePath()
                + "/";
        openDatabase();
    }

    private void openDatabase() {

        SQLiteDatabase db = null;
        String dbFileName = mDbDir + DB_NAME;
        try {
            Context context = Application.getInstance().getApplicationContext();
            if (!new File(dbFileName).exists()) {
                db = context.openOrCreateDatabase(dbFileName, Context.MODE_PRIVATE, null);

            } else {
                db = context.openOrCreateDatabase(dbFileName, Context.MODE_PRIVATE, null);

            }
        } catch (SQLiteException e) {
            MLog.error(MLog.getStactTrace(e));
        }

        if (db == null) {
            return;
        }
        mDatabase = db;

    }

    public void closeDatabase() {
        synchronized (mLocalStore) {
            if (mDatabase != null) {
                mDatabase.close();                
            }
        }
    }

    public synchronized long insert(String table, String nullColumnHack, ContentValues values) {
        return mDatabase.insert(table, nullColumnHack, values);
    }

    public synchronized int update(String table, ContentValues values, String whereClause,
            String[] whereArgs) {
        return mDatabase.update(table, values, whereClause, whereArgs);
    }

    public synchronized long replace(String table, String nullColumnHack,
            ContentValues initialValues) {
        return mDatabase.replace(table, nullColumnHack, initialValues);
    }

    public synchronized long replaceOrThrow(String table, String nullColumnHack,
            ContentValues initialValues) {
        return mDatabase.replaceOrThrow(table, nullColumnHack, initialValues);
    }

    public synchronized Cursor query(String table, String[] columns, String selection,
            String[] selectionArgs, String groupBy, String having, String orderBy) {
        Cursor c = mDatabase.query(table, columns, selection, selectionArgs, groupBy, having,
                orderBy);
        return c;
    }

    public synchronized Cursor rewquery(String sql, String[] selectionArgs) {
        Cursor c = mDatabase.rawQuery(sql, selectionArgs);
        return c;
    }

    public synchronized int del(String tableName, int id) {
        return mDatabase.delete(tableName, "_id=?", new String[] { String.valueOf(id) });
    }

    public synchronized int delete(String tableName, String whereClause, String[] whereArgs) {
        return mDatabase.delete(tableName, whereClause, whereArgs);
    }

    public synchronized SQLiteDatabase getDatabase() {
        return mDatabase;
    }

    public void delete() {
        synchronized (mLocalStore) {
            if (mDatabase != null) {
                mDatabase.close();
                mDatabase = null;
            }

            File dbFileName = new File(mDbDir + DB_NAME);
            if (dbFileName.exists()) {
                dbFileName.delete();
            }
        }
    }

    public Boolean isDbUpgrade() {
        if (mDatabase.getVersion() < DB_VERSION) {
            return true;
        }
        return false;
    }

    public void dbDbUpgrade() {
        synchronized (mLocalStore) {
            int ver = mDatabase.getVersion();
            if (ver < DB_VERSION) {
                mDatabase.execSQL("DROP TABLE IF EXISTS messages");
                mDatabase.execSQL("CREATE TABLE messages (" + "_id INTEGER PRIMARY KEY, "
                        + "account TEXT, " + "thread_id INTEGER default 0, "
                        + "sms_id INTEGER default 0, " + "msg_uuid TEXT, "
                        + "msg_type INTEGER  default 0, " + "sender TEXT, " + "receiver TEXT, "
                        + "sms TEXT, " + "status INTEGER  default 0, " + "update_time INTEGER"
                        + ")");

                mDatabase.execSQL("DROP TABLE IF EXISTS message_item");
                mDatabase.execSQL("CREATE TABLE message_item (" + "_id INTEGER PRIMARY KEY, "
                        + "msg_id INTEGER default 0, " + "msg_uuid TEXT, "
                        + "item_id INTEGER default 0, " + "subject TEXT, "
                        + "desc TEXT, " + "short_link TEXT, " + "long_link TEXT, " + "body TEXT, "
                        + "attach_type TEXT, " + "attach_name TEXT, " + "attach_size TEXT, "
                        + "attach_url TEXT, " + "attachment TEXT" + ")");

                mDatabase.execSQL("DROP TABLE IF EXISTS accounts");
                mDatabase.execSQL("CREATE TABLE accounts (" + "_id INTEGER PRIMARY KEY, "
                        + "account TEXT, " + "account_name TEXT, " + "email TEXT, "
                        + "phone_number TEXT, " + "desc TEXT, " + "status INTEGER default 1, "
                        + "icon TEXT, " + "is_insert INTEGER default 0, "
                        + "is_exist INTEGER default 0, " + "update_time INTEGER" + ")");

                mDatabase.execSQL("DROP TABLE IF EXISTS userinfo");
                mDatabase.execSQL("CREATE TABLE userinfo (" + "_id INTEGER PRIMARY KEY, "
                        + "account TEXT, " + "user_name TEXT, " + "email TEXT, "
                        + "phone_number TEXT, " + "user_sex  int, " + "icon int, " + "sign TEXT, "
                        + "age INTEGER, " + "update_time INTEGER" + ")");
                
                mDatabase.execSQL("DROP TABLE IF EXISTS message_queue");
                mDatabase.execSQL("CREATE TABLE message_queue(" + "_id INTEGER PRIMARY KEY AUTOINCREMENT, " 
                         + "account TEXT, " + "msg_uuid TEXT, " + "retry_time INTEGER default 0, " 
                         + "pending INTEGER default 0, " + "update_time INTEGER, " + "create_time INTEGER" + ")");

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mDatabase.execSQL("CREATE INDEX messages_idx_tid on messages(thread_id)");
                        mDatabase.execSQL("CREATE INDEX messages_idx_sid on messages(sms_id)");
                        mDatabase.execSQL("CREATE INDEX messages_idx_uuid on messages(msg_uuid)");
                        mDatabase.execSQL("CREATE INDEX messages_idx_sms on messages(sms)");
                        mDatabase
                                .execSQL("CREATE INDEX messages_idx_time on messages(update_time)");
                        mDatabase
                                .execSQL("CREATE INDEX message_item_idx_uuid on message_item(msg_uuid)");
                        mDatabase
                                .execSQL("CREATE INDEX message_item_idx_id on message_item(item_id)");
                        mDatabase
                                .execSQL("CREATE INDEX message_item_idx_body on message_item(body)");
                        mDatabase
                                .execSQL("CREATE INDEX message_item_idx_desc on message_item(desc)");
                        mDatabase.execSQL("CREATE INDEX accounts_idx_account on accounts(account)");
                        mDatabase
                                .execSQL("CREATE INDEX accounts_idx_name on accounts(account_name)");
                        mDatabase
                                .execSQL("CREATE INDEX accounts_idx_insert on accounts(is_insert)");
                        mDatabase
                                .execSQL("CREATE INDEX accounts_idx_time on accounts(update_time)");
                        mDatabase.execSQL("CREATE INDEX userinfo_idx_name on userinfo(user_name)");
                        mDatabase
                                .execSQL("CREATE INDEX userinfo_idx_number on userinfo(phone_number)");
                    }
                });
            }
            mDatabase.setVersion(DB_VERSION);
        }
    }

}
