package com.orangelabs.rcs.provider.ipcall;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.gsma.joyn.ipcall.IPCallLog;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import com.orangelabs.rcs.provider.eab.RichAddressBookProvider;

/**
 * IP call history provider
 * 
 * @author owom5460
 */
public class IPCallProvider extends ContentProvider {
	// Database table
	public static final String TABLE = "ipcall";
		
	// Create the constants used to differentiate between the different
	// URI requests
	private static final int IPCALLS = 1;
	private static final int IPCALL_ID = 2;
    private static final int RCSAPI = 3;
    private static final int RCSAPI_ID = 4;
		
	// Allocate the UriMatcher object, where a URI ending in 'ipcall'
	// will correspond to a request for all ipcall, and 'ipcall'
	// with a trailing '/[rowID]' will represent a single ipcall row.
	private static final UriMatcher uriMatcher;
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI("com.orangelabs.rcs.ipcall", "ipcall", IPCALLS);
		uriMatcher.addURI("com.orangelabs.rcs.ipcall", "ipcall/#", IPCALL_ID);
		uriMatcher.addURI("org.gsma.joyn.provider.ipcall", "ipcall", RCSAPI);
		uriMatcher.addURI("org.gsma.joyn.provider.ipcall", "ipcall/#", RCSAPI_ID);
	}
			
	/**
	 * Database helper class
	 */
	private SQLiteOpenHelper openHelper;	
	 
    /**
     * Database name
     */
    public static final String DATABASE_NAME = "ipcall.db";

    /**
     * Helper class for opening, creating and managing database version control
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 1;

        public DatabaseHelper(Context ctx) {
            super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        	db.execSQL("CREATE TABLE " + TABLE + " ("
        			+ IPCallData.KEY_ID + " integer primary key autoincrement,"
        			+ IPCallData.KEY_SESSION_ID + " TEXT,"
        			+ IPCallData.KEY_CONTACT + " TEXT,"
        			+ IPCallData.KEY_STATUS + " integer,"
        			+ IPCallData.KEY_DIRECTION + " integer,"
        			+ IPCallData.KEY_TIMESTAMP + " long);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE);
            onCreate(db);
        }
    }

    @Override
    public boolean onCreate() {
        openHelper = new DatabaseHelper(getContext());
        return true;
    }
	

	@Override
	public String getType(Uri uri) {
		switch(uriMatcher.match(uri)){
			case IPCALLS:
			case RCSAPI:
				return "vnd.android.cursor.dir/ipcall";
			case IPCALL_ID:
			case RCSAPI_ID:
				return "vnd.android.cursor.item/ipcall";
			default:
				throw new IllegalArgumentException("Unsupported URI " + uri);
		}
	}
	
	@Override
    public Cursor query(Uri uri, String[] projectionIn, String selection, String[] selectionArgs, String sort) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(TABLE);

        // Generate the body of the query
        int match = uriMatcher.match(uri);
        switch(match) {
            case IPCALLS:
        	case RCSAPI:
                break;
            case IPCALL_ID:
            case RCSAPI_ID:
                qb.appendWhere(IPCallLog.ID + "=" + uri.getPathSegments().get(1));
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        SQLiteDatabase db = openHelper.getReadableDatabase();
        Cursor c = qb.query(db, projectionIn, selection, selectionArgs, null, null, sort);

    	// Register the contexts ContentResolver to be notified if the cursor result set changes.
        if (c != null) {
        	c.setNotificationUri(getContext().getContentResolver(), IPCallLog.CONTENT_URI);
        }
        return c;
	}
	
	@Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        int count = 0;
        SQLiteDatabase db = openHelper.getWritableDatabase();

        int match = uriMatcher.match(uri);
        switch (match) {
	        case IPCALLS:
	            count = db.update(TABLE, values, where, null);
	            break;
            case IPCALL_ID:
                String segment = uri.getPathSegments().get(1);
                int id = Integer.parseInt(segment);
                count = db.update(TABLE, values, IPCallLog.ID + "=" + id, null);
                break;
            default:
                throw new UnsupportedOperationException("Cannot update URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
	}
	

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        switch(uriMatcher.match(uri)){
	        case IPCALLS:
	        case IPCALL_ID:
	    		long rowId = db.insert(TABLE, null, initialValues);
	    		uri = ContentUris.withAppendedId(IPCallLog.CONTENT_URI, rowId);
	        	break;
	        default:
	    		throw new SQLException("Failed to insert row into " + uri);
        }
		getContext().getContentResolver().notifyChange(uri, null);
        return uri;
	}

	
	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        int count = 0;
        switch(uriMatcher.match(uri)){
	        case IPCALLS:
	        case RCSAPI:
	        	count = db.delete(TABLE, where, whereArgs);
	        	break;
	        case IPCALL_ID:
	        case RCSAPI_ID:
	        	String segment = uri.getPathSegments().get(1);
				count = db.delete(TABLE, IPCallLog.ID + "="
						+ segment
						+ (!TextUtils.isEmpty(where) ? " AND ("	+ where + ')' : ""),
						whereArgs);
				
				break;
	        	
	        default:
	    		throw new SQLException("Failed to delete row " + uri);
        }
		getContext().getContentResolver().notifyChange(uri, null);
        return count;    
	}
	
	public static void backupIPCallDatabase(String account) {
    	try {
	    	String packageName = "com.orangelabs.rcs";
	    	String dbFile = Environment.getDataDirectory() + "/data/" + packageName + "/databases/" + IPCallProvider.DATABASE_NAME;
	    	File file = new File(dbFile);
	    	if (file.exists())
	    	{
	    	File backupFileRoot =new File( Environment.getExternalStorageDirectory()+"/Rcs" + "/data/" + packageName + "/databases/");
	    	backupFileRoot.mkdirs();
	    	File backupFile = new File(backupFileRoot ,IPCallProvider.DATABASE_NAME +"_"+ account +".db");
	    	
	    	OutputStream outStream = new FileOutputStream(backupFile, false);
	    	InputStream inStream = new FileInputStream(dbFile);
 		    byte[] buffer = new byte[1024];
		    int length;
		    while ((length = inStream.read(buffer))>0) {
				outStream.write(buffer, 0, length);
		    }
		    outStream.flush();
		    outStream.close();
		    inStream.close();		    	
	    	}
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    		
    }
	
	public static void backupIPCallDatabase(String account, String path) {
    	try {
	    	String packageName = "com.orangelabs.rcs";
	    	String dbFile = Environment.getDataDirectory() + "/data/" + packageName + "/databases/" + IPCallProvider.DATABASE_NAME;
	    	File file = new File(dbFile);
	    	if (file.exists())
	    	{
	    	File backupFileRoot =new File( path);
	    	backupFileRoot.mkdirs();
	    	File backupFile = new File(backupFileRoot ,IPCallProvider.DATABASE_NAME +"_"+ account +".db");
	    	
	    	OutputStream outStream = new FileOutputStream(backupFile, false);
	    	InputStream inStream = new FileInputStream(dbFile);
 		    byte[] buffer = new byte[1024];
		    int length;
		    while ((length = inStream.read(buffer))>0) {
				outStream.write(buffer, 0, length);
		    }
		    outStream.flush();
		    outStream.close();
		    inStream.close();		    	
	    	}
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    		
    }
	
	public static void restoreIPCallDatabase(String account)
    {
    	
    	try {
    		
   		// if (logger.isActivated()) {
        //        logger.debug("restoreAccountMessages : account:"+account);
         //   }
   		 
	    	String packageName = "com.orangelabs.rcs";
	    	String dbFile = Environment.getDataDirectory() + "/data/" + packageName + "/databases/" + IPCallProvider.DATABASE_NAME;
	    	File backupFileRoot =new File( Environment.getExternalStorageDirectory()+"/Rcs" + "/data/" + packageName + "/databases/");   
	    	String restoreFile = backupFileRoot+"/"+ IPCallProvider.DATABASE_NAME +"_"+ account +".db";
           


	    	File file = new File(restoreFile);
	    	if (!file.exists()) {
	    	//	if (logger.isActivated()) {
	                 //logger.debug("error in restoreAccountMessages : account:"+account + "; "+restoreFile+" :file cant be created");
	       //      }
	    		return;
	    	}
	    	

                File dbOriginalFile = new File(dbFile);
              if (!dbOriginalFile.exists()) {
                            //   dbOriginalFile.delete();
				dbOriginalFile.createNewFile();
              } 
            //  dbOriginalFile.createNewFile();
              new FileOutputStream(dbFile,false).close();
	    	OutputStream outStream = new FileOutputStream(dbFile, false);
	    	InputStream inStream = new FileInputStream(file);
		    byte[] buffer = new byte[1024];
		    int length;
		    while ((length = inStream.read(buffer))>0) {
				outStream.write(buffer, 0, length);
		    }
		    outStream.flush();
		    outStream.close();
		    inStream.close();		    	
   	} catch(Exception e) {

 // if (logger.isActivated()) {
  //             logger.debug("exception in restoreAccountMessages");
  //         }
   		e.printStackTrace();

   	}
    }

	
	public static boolean restoreIPCallDatabase(String account, String path)
    {
    	
    	try {
    		
   		// if (logger.isActivated()) {
        //        logger.debug("restoreAccountMessages : account:"+account);
         //   }
   		 
	    	String packageName = "com.orangelabs.rcs";
	    	String dbFile = Environment.getDataDirectory() + "/data/" + packageName + "/databases/" + IPCallProvider.DATABASE_NAME;
	    	File backupFileRoot =new File(path);   
	    	String restoreFile = backupFileRoot+"/"+ IPCallProvider.DATABASE_NAME +"_"+ account +".db";
           


	    	File file = new File(restoreFile);
	    	if (!file.exists()) {
	    	//	if (logger.isActivated()) {
	                 //logger.debug("error in restoreAccountMessages : account:"+account + "; "+restoreFile+" :file cant be created");
	       //      }
	    		return false;
	    	}
	    	

               //delete the original file 
                File dbOriginalFile = new File(dbFile);
              if (!dbOriginalFile.exists()) {
                            //   dbOriginalFile.delete();
				dbOriginalFile.createNewFile();
              } 
            //  dbOriginalFile.createNewFile();
              new FileOutputStream(dbFile,false).close();
	    	OutputStream outStream = new FileOutputStream(dbFile, false);
	    	InputStream inStream = new FileInputStream(file);
		    byte[] buffer = new byte[1024];
		    int length;
		    while ((length = inStream.read(buffer))>0) {
				outStream.write(buffer, 0, length);
		    }
		    outStream.flush();
		    outStream.close();
		    inStream.close();		    	
   	} catch(Exception e) {

 // if (logger.isActivated()) {
  //             logger.debug("exception in restoreAccountMessages");
  //         }
   		e.printStackTrace();
        return false;
   	    }
   	return true;
    }
}
