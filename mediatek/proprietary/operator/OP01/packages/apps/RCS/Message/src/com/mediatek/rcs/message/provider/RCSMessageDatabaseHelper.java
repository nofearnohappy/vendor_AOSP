package com.mediatek.rcs.message.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.mediatek.rcs.common.provider.FavoriteMsgData;
import com.mediatek.rcs.common.provider.GroupMemberData;
import com.mediatek.rcs.common.provider.SpamMsgData;
import com.mediatek.rcs.common.provider.GroupChatData;
import com.mediatek.rcs.message.provider.GroupChatProvider;

public class RCSMessageDatabaseHelper extends SQLiteOpenHelper {

    /**
     * Database name
     */
    public static final String DATABASE_NAME = "rcsmessage.db";
    public static final int    DATABASE_VERSION = 7;

    private static RCSMessageDatabaseHelper sInstance = null;

    private RCSMessageDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized RCSMessageDatabaseHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new RCSMessageDatabaseHelper(context);
        }
        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createSpamTable(db);
        createGroupChatTable(db);
        createFavoriteTable(db);
        createGroupMemberTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch(oldVersion) {
            case 1:
                if (newVersion <= 1) {
                    return;
                }
                updateToVersion2(db);
            case 2:
                if (newVersion <= 2) {
                    return;
                }
                updateToVersion3(db);
            case 3:
                if (newVersion <= 3) {
                    return;
                }
                updateToVersion4(db);
            case 4:
                if (newVersion <= 4) {
                    return;
                }
                updateToVersion5(db);
            case 5:
                if (newVersion <= 5) {
                    return;
                }
                updateToVersion6(db);
            case 6:
                if (newVersion <= 6) {
                    return;
                }
                updateToVersion7(db);
            default:
                break;
        }
    }

    private void createSpamTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + SpamMsgProvider.TABLE_SPAM + " (" +
                SpamMsgData.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                SpamMsgData.COLUMN_BODY + " TEXT," +
                SpamMsgData.COLUMN_DATE + " INTEGER DEFAULT 0," +
                SpamMsgData.COLUMN_ADDRESS + " TEXT," +
                SpamMsgData.COLUMN_TYPE + " INTEGER DEFAULT 0," +
                SpamMsgData.COLUMN_SUB_ID + " LONG DEFAULT -1," +
                SpamMsgData.COLUMN_IPMSG_ID + " INTEGER DEFAULT 0," +
                SpamMsgData.COLUMN_MESSAGE_ID + " TEXT" +
                ");");

    }

    private void createGroupMemberTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + GroupMemberProvider.TABLE_GROUPMEMBER + " ("
                + GroupMemberData.COLUMN_ID + " INTEGER primary key autoincrement,"
                + GroupMemberData.COLUMN_CHAT_ID + " TEXT,"
                + GroupMemberData.COLUMN_CONTACT_NUMBER + " TEXT,"
                + GroupMemberData.COLUMN_CONTACT_NAME + " TEXT,"
                + GroupMemberData.COLUMN_STATE + " INTEGER DEFAULT 0,"
                + GroupMemberData.COLUMN_TYPE + " INTEGER DEFAULT 0,"
                + GroupMemberData.COLUMN_PORTRAIT + " TEXT,"
                + GroupMemberData.COLUMN_PORTRAIT_NAME + " TEXT);");
    }

    private void createGroupChatTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + GroupChatProvider.TABLE_GROUPCHAT + " ("
                + GroupChatData.KEY_ID + " INTEGER primary key autoincrement,"
                + GroupChatData.KEY_SUBJECT + " TEXT,"
                + GroupChatData.KEY_NICKNAME + " TEXT,"
                + GroupChatData.KEY_STATUS + " INTEGER DEFAULT 0,"
                + GroupChatData.KEY_ISCHAIRMEN + " INTEGER DEFAULT 0,"
                + GroupChatData.KEY_SUB_ID + " INTEGER DEFAULT 0,"
                + GroupChatData.KEY_CHAT_ID + " TEXT,"
                + GroupChatData.KEY_REJOIN_ID + " TEXT);");
    }

    private void createFavoriteTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + FavoriteMsgProvider.TABLE_FAVORITE + " (" +
            FavoriteMsgData.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            FavoriteMsgData.COLUMN_DATE + " INTEGER DEFAULT 0," +
            FavoriteMsgData.COLUMN_CHATID + " TEXT," +
            FavoriteMsgData.COLUMN_DA_ID + " TEXT," +
            FavoriteMsgData.COLUMN_DA_CONTACT + " TEXT," +
            FavoriteMsgData.COLUMN_DA_BODY + " TEXT," +
            FavoriteMsgData.COLUMN_DA_TIMESTAMP + " LONG DEFAULT 0," +
            FavoriteMsgData.COLUMN_DA_MIME_TYPE + " TEXT," +
            FavoriteMsgData.COLUMN_DA_MESSAGE_STATUS + " INTEGER DEFAULT 0," +
            FavoriteMsgData.COLUMN_DA_DIRECTION + " INTEGER DEFAULT 0," +
            FavoriteMsgData.COLUMN_DA_TYPE + " INTEGER DEFAULT 0," +
            FavoriteMsgData.COLUMN_DA_FLAG + " INTEGER DEFAULT 0," +
            FavoriteMsgData.COLUMN_DA_FILENAME + " TEXT," +
            FavoriteMsgData.COLUMN_DA_FILEICON + " TEXT," +
            FavoriteMsgData.COLUMN_DA_FILESIZE + " LONG DEFAULT 0" +
            ");");
    }

    private void updateToVersion2(SQLiteDatabase db) {
        createGroupMemberTable(db);
    }

    private void updateToVersion3(SQLiteDatabase db) {
    }

    private void updateToVersion4(SQLiteDatabase db) {
        String sql = "ALTER TABLE threadmap  ADD COLUMN " +
                GroupChatData.KEY_SUB_ID + " INTEGER DEFAULT 0";
        db.execSQL(sql);
    }

    private void updateToVersion5(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS favorite;");
        createFavoriteTable(db);
    }

    private void updateToVersion6(SQLiteDatabase db) {
        db.execSQL("CREATE TRIGGER rejoin_data_delete AFTER DELETE ON threadmap BEGIN DELETE FROM "
                + " rejoin WHERE rejoin.chat_id = OLD.chat_id; END;");
    }

    private void updateToVersion7(SQLiteDatabase db) {
        String sql = "ALTER TABLE " +
                SpamMsgProvider.TABLE_SPAM + " ADD COLUMN " +
                SpamMsgData.COLUMN_MESSAGE_ID + " TEXT ";
        db.execSQL(sql);
        db.execSQL("ALTER TABLE " + GroupMemberProvider.TABLE_GROUPMEMBER + " ADD COLUMN " +
                    GroupMemberData.COLUMN_PORTRAIT_NAME + " TEXT;");
        db.execSQL("ALTER TABLE " +  GroupMemberProvider.TABLE_GROUPMEMBER + " RENAME TO temp;");
        createGroupMemberTable(db);
        db.execSQL("INSERT INTO " + GroupMemberProvider.TABLE_GROUPMEMBER + " SELECT * FROM temp;");
        db.execSQL("DROP TABLE IF EXISTS temp;");
        db.execSQL("ALTER TABLE threadmap RENAME TO temp;");
        createGroupChatTable(db);
        // TODO
        db.execSQL("INSERT INTO " + GroupChatProvider.TABLE_GROUPCHAT +
                "( " + GroupChatData.KEY_ID + ", " +
                       GroupChatData.KEY_SUBJECT + ", " +
                       GroupChatData.KEY_NICKNAME + ", " +
                       GroupChatData.KEY_STATUS + ", " +
                       GroupChatData.KEY_ISCHAIRMEN + ", " +
                       GroupChatData.KEY_SUB_ID + ", " +
                       GroupChatData.KEY_CHAT_ID + ", " +
                       GroupChatData.KEY_REJOIN_ID + ")" +
                " SELECT " +
                " temp." + GroupChatData.KEY_ID + " AS " + GroupChatData.KEY_ID + ", " +
                " temp." + GroupChatData.KEY_SUBJECT + " AS " + GroupChatData.KEY_SUBJECT + ", " +
                " temp." + GroupChatData.KEY_NICKNAME + " AS " + GroupChatData.KEY_NICKNAME + ", " +
                " temp." + GroupChatData.KEY_STATUS + " AS " + GroupChatData.KEY_STATUS + ", " +
                " temp." + GroupChatData.KEY_ISCHAIRMEN + " AS " + GroupChatData.KEY_ISCHAIRMEN +
                     ", " +
                " temp." + GroupChatData.KEY_SUB_ID + " AS " + GroupChatData.KEY_SUB_ID + ", " +
                " temp." + GroupChatData.KEY_CHAT_ID + " AS " + GroupChatData.KEY_CHAT_ID + ", " +
                " rejoin.rejoin_id AS " + GroupChatData.KEY_REJOIN_ID +
                " FROM temp LEFT JOIN rejoin ON temp.chat_id=rejoin.chat_id;");
        db.execSQL("DROP TABLE IF EXISTS temp;");
        db.execSQL("DROP TABLE IF EXISTS rejoin;");
        db.execSQL("DROP TABLE IF EXISTS favorite;");
        createFavoriteTable(db);
    }

}
