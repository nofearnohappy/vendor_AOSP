
package com.mediatek.bluetoothle.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class BluetoothLeProvider extends ContentProvider {

    private static final String TAG = "[BT][BLE][BluetoothLeProvider]";

    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    private SQLiteOpenHelper mDBOpenHelper;

    /** URI MATCHER */
    private static final int ANS_TABLE = 1;
    private static final int ANS_TABLE_ID = 2;

    private static final int CLIENT_TABLE = 3;
    private static final int CLIENT_TABLE_ID = 4;

    private static final int TIP_TABLE = 5;
    private static final int TIP_TABLE_ID = 6;

    private static final String LIST_TYPE = "com.android.bluetooth.BLE.list";
    private static final String ITEM_TYPE = "com.android.bluetooth.BLE.item";

    static {
        URI_MATCHER.addURI(BLEConstants.AUTORITY, "ans", ANS_TABLE);
        URI_MATCHER.addURI(BLEConstants.AUTORITY, "ans/#", ANS_TABLE_ID);

        URI_MATCHER.addURI(BLEConstants.AUTORITY, "client_table", CLIENT_TABLE);
        URI_MATCHER.addURI(BLEConstants.AUTORITY, "client_table/#", CLIENT_TABLE_ID);

        URI_MATCHER.addURI(BLEConstants.AUTORITY, "tip", TIP_TABLE);
        URI_MATCHER.addURI(BLEConstants.AUTORITY, "tip/#", TIP_TABLE_ID);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        Log.d(TAG, "[delete] uri : " + uri + ", selection : " + selection + ", selectionArgs : "
                + selectionArgs);
        if (!isExisted(uri, selection)) {
            Log.d(TAG, "[delete] not exist in DB, cann't delete!!!");
            return 0;
        }
        SQLiteDatabase db = mDBOpenHelper.getWritableDatabase();
        int match = URI_MATCHER.match(uri);
        String table = null;
        int whichTable = 0;

        switch (match) {
            case ANS_TABLE:
            case ANS_TABLE_ID:
                table = BLEConstants.ANS.TABLE_ANS;
                whichTable = ANS_TABLE;
                break;

            case CLIENT_TABLE:
            case CLIENT_TABLE_ID:
                table = BLEConstants.CLIENT_TABLE.TABLE_NAME;
                whichTable = CLIENT_TABLE;
                break;

            case TIP_TABLE:
            case TIP_TABLE_ID:
                table = BLEConstants.TIP.TABLE_TIP;
                whichTable = TIP_TABLE;
                break;

            default:
                throw new IllegalArgumentException("[delete] Unknown URI: " + uri);
        }
        Log.d(TAG, "[delete] table : " + table);
        if (table == null) {
            return 0;
        }
        int re = db.delete(table, selection, selectionArgs);
        if (re > 0 && whichTable != 0) {
            this.notifyChange(whichTable);
        }
        return re;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        switch (URI_MATCHER.match(uri)) {
            case ANS_TABLE:
                return LIST_TYPE;

            case ANS_TABLE_ID:
                return ITEM_TYPE;

            default:
                throw new IllegalArgumentException("[getType] Unknown URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        Log.d(TAG, "[insert] uri : " + uri + ", values : " + values);
        if (values == null) {
            Log.d(TAG, "[insert] values is null, pls check!!!");
            return null;
        }
        if (values.size() == 0) {
            Log.d(TAG, "[insert] values size is 0, pls check!!!");
            return null;
        }
        if (!values.containsKey(BLEConstants.COLUMN_BT_ADDRESS)) {
            Log.d(TAG, "[insert] MUST CONTAINS ADDRESS, PLS CHECK!!!");
            return null;
        }
        String addrFromValue = values.getAsString(BLEConstants.COLUMN_BT_ADDRESS);
        if (addrFromValue == null || addrFromValue.trim().length() == 0) {
            Log.d(TAG, "[insert] WRONG ADDRESS, SHOULD BE VALID BT ADDRESS, PLS CHECK!!!");
            return null;
        }
        String selection = BLEConstants.COLUMN_BT_ADDRESS + "='" + addrFromValue + "'";
        if (isExisted(uri, selection)) {
            Log.d(TAG, "[insert] device is already in db.");
            return null;
        }
        SQLiteDatabase db = mDBOpenHelper.getWritableDatabase();
        int match = URI_MATCHER.match(uri);
        String table = null;
        int whichTable = 0;
        ContentValues finalValue = new ContentValues(values);

        switch (match) {
            case ANS_TABLE:
                table = BLEConstants.ANS.TABLE_ANS;
                whichTable = ANS_TABLE;
                break;

            case CLIENT_TABLE:
                table = BLEConstants.CLIENT_TABLE.TABLE_NAME;
                whichTable = CLIENT_TABLE;
                break;

            case TIP_TABLE:
                table = BLEConstants.TIP.TABLE_TIP;
                whichTable = TIP_TABLE;
                break;

            default:
                throw new IllegalArgumentException("[insert] Unknown URI: " + uri);
        }
        long id = db.insert(table, null, finalValue);
        if (id > 0 && whichTable != 0) {
            this.notifyChange(whichTable);
        }
        if (id < 0) {
            Log.d(TAG, "[insert] insert failed");
            return null;
        }
        return Uri.parse(uri + "/" + id);
    }

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub
        Log.d(TAG, "[onCreate] call create db");
        mDBOpenHelper = BluetoothLeDatabaseHelper.getInstance(getContext());
        return true;
    }

    /**
     * Which used to query data from db.
     *
     * @param uri
     * @param projection
     * @param selection
     * @param selectionArgs
     * @param sortOrder
     * @return
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        // TODO Auto-generated method stub
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        SQLiteDatabase db = mDBOpenHelper.getReadableDatabase();
        Cursor retCursor = null;
        int match = URI_MATCHER.match(uri);
        String where = null;

        switch (match) {
            case ANS_TABLE:
            case ANS_TABLE_ID:
                Log.d(TAG, "[query] ANS_TABLE selection : " + selection);
                builder.setTables(BLEConstants.ANS.TABLE_ANS);
                where = builder.buildQuery(projection, selection, null, null, sortOrder, null);
                Log.d(TAG, "[query] ANS_TABLE where : " + where);
                retCursor = db.rawQuery(where, selectionArgs);
                Log.d(TAG, "[query] ANS_TABLE retCursor : " + retCursor.getCount());
                break;

            case CLIENT_TABLE:
                Log.d(TAG, "[query] CLIENT_TABLE selection : " + selection);
                builder.setTables(BLEConstants.CLIENT_TABLE.TABLE_NAME);
                where = builder.buildQuery(projection, selection, null, null, sortOrder, null);
                Log.d(TAG, "[query] CLIENT_TABLE where : " + where);
                retCursor = db.rawQuery(where, selectionArgs);
                Log.d(TAG, "[query] CLIENT_TABLE retCursor : " + retCursor.getCount());
                break;

            case CLIENT_TABLE_ID:
                builder.setTables(BLEConstants.CLIENT_TABLE.TABLE_NAME);
                builder.appendWhere(BLEConstants.COLUMN_ID + "=" + uri.getPathSegments().get(1));
                retCursor = builder.query(db, projection, selection, selectionArgs, null, null,
                        sortOrder);
                break;

            case TIP_TABLE:
            case TIP_TABLE_ID:
                Log.d(TAG, "[query] TIP_TABLE selection : " + selection);
                builder.setTables(BLEConstants.TIP.TABLE_TIP);
                String select1 = builder.buildQuery(projection, selection, null, null, sortOrder,
                        null);
                Log.d(TAG, "[query] TIP_TABLE select : " + select1);
                retCursor = db.rawQuery(select1, selectionArgs);
                Log.d(TAG, "[query] TIP_TABLE retCursor : " + retCursor.getCount());
                break;

            default:
                throw new IllegalArgumentException("[query] Unknown URI: " + uri);
        }

        return retCursor;
    }

    /**
     * @param uri
     * @param values A map from column names to new column values. null is a valid value that will
     *            be translated to NULL.
     * @param selection the optional WHERE clause to apply when updating. Passing null will update
     *            all rows.
     * @param selectionArgs You may include ? s in the where clause, which will be replaced by the
     *            values from whereArgs. The values will be bound as Strings.
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        Log.d(TAG, "[update] uri : " + uri + ", values : " + values + ", selection :" + selection);
        if (!isExisted(uri, selection)) {
            Log.d(TAG, "[update] not exist in DB, cann't update");
            return 0;
        }
        SQLiteDatabase db = mDBOpenHelper.getWritableDatabase();
        int returnValue = 0;
        int match = URI_MATCHER.match(uri);
        int whichTable = 0;

        switch (match) {
            case ANS_TABLE:
            case ANS_TABLE_ID:
                returnValue = db.update(BLEConstants.ANS.TABLE_ANS, values, selection,
                        selectionArgs);
                whichTable = ANS_TABLE;
                break;

            case CLIENT_TABLE:
                returnValue = db.update(BLEConstants.CLIENT_TABLE.TABLE_NAME, values, selection,
                        selectionArgs);
                whichTable = CLIENT_TABLE;
                break;

            case CLIENT_TABLE_ID:
                Log.d(TAG, "[update] CLIENT_TABLE_ID enter");
                // returnValue = db.update(BLEConstants.ANS.TABLE_ANS, values, selection,
                // selectionArgs);
                break;

            case TIP_TABLE:
            case TIP_TABLE_ID:
                returnValue = db.update(BLEConstants.TIP.TABLE_TIP, values, selection,
                        selectionArgs);
                whichTable = TIP_TABLE;
                break;

            default:
                throw new IllegalArgumentException("[update] Unknown URI: " + uri);
        }
        Log.d(TAG, "[update] affected rows : " + returnValue);
        if (returnValue > 0 && whichTable != 0) {
            Log.d(TAG, "[update] whichTable : " + whichTable);
            notifyChange(whichTable);
        }
        return returnValue;
    }

    private void notifyChange(int whichTable) {
        Log.d(TAG, "[notifyChange] whichTable : " + whichTable);
        Uri uri = null;
        switch (whichTable) {
            case ANS_TABLE:
                uri = BLEConstants.TABLE_ANS_URI;
                break;

            case CLIENT_TABLE:
                uri = BLEConstants.TABLE_CLIENT_URI;
                break;

            case TIP_TABLE:
                uri = BLEConstants.TABLE_TIP_URI;
                break;
            default:
                throw new IllegalArgumentException("[notifyChange] unknown id!");
        }
        Log.d(TAG, "[notifyChange] notify change uri : " + uri);

        if (uri == null) {
            Log.d(TAG, "[notifyChange] uri is null !!");
            return;
        }
        this.getContext().getContentResolver().notifyChange(uri, null);
    }

    private boolean isExisted(Uri uri, String selection) {
        Log.d(TAG, "[isExisted] uri : " + uri + ", selection : " + selection);
        if (uri == null) {
            return false;
        }

        SQLiteDatabase db = mDBOpenHelper.getReadableDatabase();
        int match = URI_MATCHER.match(uri);
        Cursor cur = null;
        try {
            switch (match) {
                case ANS_TABLE:
                case ANS_TABLE_ID:
                    cur = db.query(BLEConstants.ANS.TABLE_ANS, null, selection, null, null, null,
                            null);
                    break;

                case CLIENT_TABLE:
                case CLIENT_TABLE_ID:
                    cur = db.query(BLEConstants.CLIENT_TABLE.TABLE_NAME, null, selection, null,
                            null, null, null);
                    break;

                case TIP_TABLE:
                case TIP_TABLE_ID:
                    cur = db.query(BLEConstants.TIP.TABLE_TIP, null, selection, null, null, null,
                            null);
                    break;

                default:
                    throw new IllegalArgumentException("[isExisted] Unknown URI: " + uri);
            }
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        } finally {
            if (cur == null) {
                Log.d(TAG, "[isExisted] cursor is null!!!");
                return false;
            }
            if (cur.getCount() == 0) {
                cur.close();
                Log.d(TAG, "[isExisted] cursor count is 0!!!");
                return false;
            }
            cur.close();
        }
        return true;
    }
}
