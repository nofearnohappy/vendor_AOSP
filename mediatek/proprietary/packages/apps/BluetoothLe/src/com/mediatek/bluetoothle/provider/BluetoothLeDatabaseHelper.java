
package com.mediatek.bluetoothle.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.mediatek.bluetoothle.ext.BLEExtentionManager;
import com.mediatek.bluetoothle.ext.IBluetoothLeAnsExtension;

import java.util.TreeSet;

public class BluetoothLeDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "[BT][BLE][BluetoothLeDatabaseHelper]";

    private static final String DATABASE_NAME = "bluetoothle.db";

    private static final int DEFAULT_VERSION = 1;

    private static BluetoothLeDatabaseHelper sInstance;

    private Context mContext;

    private BluetoothLeDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DEFAULT_VERSION);
        mContext = context;
        Log.d(TAG, "[Constructor] DATABASE_NAME : " + DATABASE_NAME
                + ", DEFAULT_VERSION :" + DEFAULT_VERSION);
    }

    public static BluetoothLeDatabaseHelper getInstance(Context context) {
        if (context == null) {
            Log.e(TAG, "[getInstance] WRONG PARAMETER!! Context is null!!");
            return null;
        }
        if (sInstance == null) {
            sInstance = new BluetoothLeDatabaseHelper(context);
        }
        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        createClientTable(db);
        createAnsTable(db);
        // createPXPTable(db);
        // createUXTable(db);
        createTIPTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        Log.d(TAG, "[onUpgrade] oldVersion : " + oldVersion + ", newVersion : " + newVersion);
    }

    private void createClientTable(SQLiteDatabase db) {
        Log.d(TAG, "[createClientTable] create client table enter");
        String clientTableCreateString =
                "CREATE TABLE " + BLEConstants.CLIENT_TABLE.TABLE_NAME + " (" +
                            BLEConstants.COLUMN_ID + " INTEGER PRIMARY KEY," +
                            BLEConstants.COLUMN_BT_ADDRESS + " TEXT," +
                            BLEConstants.CLIENT_TABLE.DEVICE_AUTO_CONNECT + " INTEGER," +
                            BLEConstants.CLIENT_TABLE.ALERT_ENABLER + " INTEGER," +
                            BLEConstants.CLIENT_TABLE.RANGE_ALERT_ENABLER + " INTEGER," +
                            BLEConstants.CLIENT_TABLE.RANGE_TYPE + " INTEGER," +
                            BLEConstants.CLIENT_TABLE.RANGE_VALUE + " INTEGER," +
                            BLEConstants.CLIENT_TABLE.DISCONNECTION_WARNING_ENABLER + " INTEGER," +
                            BLEConstants.CLIENT_TABLE.DEVICE_FMP_STATE + " INTEGER)";
        db.execSQL(clientTableCreateString);
    }

    private void createAnsTable(SQLiteDatabase db) {
        Log.d(TAG, "[createAnsTable] create ANS table enter");
        String ansDefaultHostString =
                "h_simple INTEGER," +
                        "h_email INTEGER," +
                        "h_news INTEGER," +
                        "h_call INTEGER," +
                        "h_missed_call INTEGER," +
                        "h_smsmms INTEGER," +
                        "h_voice_mail INTEGER," +
                        "h_schedule INTEGER," +
                        "h_high_prioritized INTEGER," +
                        "h_instant_message INTEGER,";

        String ansDefaultRemoteString =
                "r_simple INTEGER," +
                        "r_email INTEGER," +
                        "r_news INTEGER," +
                        "r_call INTEGER," +
                        "r_missed_call INTEGER," +
                        "r_smsmms INTEGER," +
                        "r_voice_mail INTEGER," +
                        "r_schedule INTEGER," +
                        "r_high_prioritized INTEGER," +
                        "r_instant_message INTEGER,";

        IBluetoothLeAnsExtension ansExtention = (IBluetoothLeAnsExtension) BLEExtentionManager.
                getExtentionObject(BLEExtentionManager.BLE_ANS_EXTENTION, mContext);

        StringBuilder hostBuilder = new StringBuilder();
        StringBuilder remoteBuilder = new StringBuilder();
        if (ansExtention != null) {
            TreeSet<Byte> extraCategorySet = ansExtention.getExtraCategoryId();
            if (extraCategorySet != null) {
                for (Byte id : extraCategorySet) {
                    hostBuilder.append(BLEConstants.ANS.ANS_HOST_EXTRA_CATEGORY);
                    remoteBuilder.append(BLEConstants.ANS.ANS_REMOTE_EXTRA_CATEGORY);
                    hostBuilder.append(id);
                    remoteBuilder.append(id);
                    hostBuilder.append(" INTEGER,");
                    remoteBuilder.append(" INTEGER,");
                }
            }
        }
        String ansExtraHostString = hostBuilder.toString();
        String ansExtraRemoteString = remoteBuilder.toString();

        String ansCreateString = "CREATE TABLE ans (" +
                "_id INTEGER PRIMARY KEY," +
                "bt_address TEXT," +
                ansDefaultHostString +
                ansExtraHostString +
                ansDefaultRemoteString +
                ansExtraRemoteString +
                "new_client_config INTEGER," +
                "unread_client_config INTEGER" +
                ")";
        db.execSQL(ansCreateString);
    }

    private void createTIPTable(SQLiteDatabase db) {
        Log.d(TAG, "[createTIPTable] create table enter");
        String tipCreateString = "CREATE TABLE tip (" +
                "_id INTEGER PRIMARY KEY," +
                "bt_address TEXT," +
                "notify INTEGER)";
        db.execSQL(tipCreateString);
    }

}
