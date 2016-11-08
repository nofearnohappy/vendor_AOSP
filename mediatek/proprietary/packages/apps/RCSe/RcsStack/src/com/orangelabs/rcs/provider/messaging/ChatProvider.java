/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.orangelabs.rcs.provider.messaging;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import com.orangelabs.rcs.provider.ipcall.IPCallProvider;
import com.orangelabs.rcs.utils.PhoneUtils;

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

/**
 * Chat provider
 * 
 * @author Jean-Marc AUFFRET
 */
public class ChatProvider extends ContentProvider {
	/**
	 * Database tables
	 */
    private static final String TABLE_CHAT = "chat";
    private static final String TABLE_MESSAGE = "message";
    private static final String TABLE_GROUPMEMBER = "GroupMember";
    private static final String TABLE_MULTIMESSAGE = "multimessage";

	// Create the constants used to differentiate between the different URI requests
	private static final int CHATS = 1;
    private static final int CHAT_ID = 2;
    private static final int RCSAPI_CHATS = 3;
    private static final int RCSAPI_CHAT_ID = 4;
    
	private static final int MESSAGES = 5;
    private static final int MESSAGE_ID = 6;
    private static final int RCSAPI_MESSAGES = 7;
    private static final int RCSAPI_MESSAGE_ID = 8;
    
   
    private static final int ADD_CHAT_CONVERSATION_ID_COLUMN = 9;
    private static final int ADD_MSG_CONVERSATION_ID_COLUMN = 10;
    
    
    //group member
    private static final int GROUP_MEMBER = 11;
    
    //multi message
    
    private static final int MULTI_PARTICIPANT_CHATS = 12;
    private static final int MULTI_PARTICIPANT_CHAT_ID = 13;
    
    
	// Allocate the UriMatcher object
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        uriMatcher.addURI("com.orangelabs.rcs.chat", "chat", CHATS);
        uriMatcher.addURI("com.orangelabs.rcs.chat", "chat/#", CHAT_ID);
		uriMatcher.addURI("org.gsma.joyn.provider.chat", "chat", RCSAPI_CHATS);
		uriMatcher.addURI("org.gsma.joyn.provider.chat", "chat/#", RCSAPI_CHAT_ID);	
        uriMatcher.addURI("com.orangelabs.rcs.chat", "message", MESSAGES);
        uriMatcher.addURI("com.orangelabs.rcs.chat", "message/#", MESSAGE_ID);
		uriMatcher.addURI("org.gsma.joyn.provider.chat", "message", RCSAPI_MESSAGES);
		uriMatcher.addURI("org.gsma.joyn.provider.chat", "message/*", RCSAPI_MESSAGE_ID);
		uriMatcher.addURI("com.orangelabs.rcs.chat", "add_chat_conv_id", ADD_CHAT_CONVERSATION_ID_COLUMN);
		uriMatcher.addURI("com.orangelabs.rcs.chat", "add_msg_conv_id", ADD_MSG_CONVERSATION_ID_COLUMN);
		uriMatcher.addURI("com.orangelabs.rcs.chat", "groupmember", GROUP_MEMBER);
		uriMatcher.addURI("org.gsma.joyn.provider.chat", "groupmember", GROUP_MEMBER);
		 uriMatcher.addURI("com.orangelabs.rcs.chat", "multimessage", MULTI_PARTICIPANT_CHATS);
		 uriMatcher.addURI("org.gsma.joyn.provider.chat", "multimessage", MULTI_PARTICIPANT_CHATS);
	     uriMatcher.addURI("com.orangelabs.rcs.chat", "multimessage/#", MULTI_PARTICIPANT_CHAT_ID);
	     uriMatcher.addURI("org.gsma.joyn.provider.chat", "multimessage/#", MULTI_PARTICIPANT_CHAT_ID);
    }

    /**
     * Database helper class
     */
    private SQLiteOpenHelper openHelper;
    
    /**
     * Database name
     */
    public static final String DATABASE_NAME = "chat.db";

    /**
     * Helper class for opening, creating and managing database version control
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 6;

        public DatabaseHelper(Context ctx) {
            super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        	db.execSQL("CREATE TABLE " + TABLE_CHAT + " ("
        			+ ChatData.KEY_ID + " integer primary key autoincrement,"
        			+ ChatData.KEY_CHAT_ID + " TEXT,"
        			+ ChatData.KEY_REJOIN_ID + " TEXT,"
        			+ ChatData.KEY_SUBJECT + " TEXT,"
        			+ ChatData.KEY_PARTICIPANTS + " TEXT,"
        			+ ChatData.KEY_CHAIRMAN + " TEXT,"
        			+ ChatData.KEY_NICKNAME + " TEXT,"
        			+ ChatData.KEY_STATUS + " integer,"
        			+ ChatData.KEY_DIRECTION + " integer,"
        			+ ChatData.KEY_ISBLOCKED + " integer DEFAULT 0,"
        			+ ChatData.KEY_TIMESTAMP + " long);");
        	
        	db.execSQL("CREATE TABLE " + TABLE_MESSAGE + " ("
        			+ MessageData.KEY_ID + " integer primary key autoincrement,"
        			+ MessageData.KEY_CHAT_ID + " TEXT,"
        			+ MessageData.KEY_CONTACT + " TEXT,"
        			+ MessageData.KEY_DISPLAY_NAME + " TEXT,"
        			+ MessageData.KEY_MSG_ID + " TEXT,"
        			+ MessageData.KEY_TYPE + " integer,"
        			+ MessageData.KEY_CONTENT + " BLOB,"
        			+ MessageData.KEY_CONTENT_TYPE + " TEXT,"
        			+ MessageData.KEY_DIRECTION + " integer,"
        			+ MessageData.KEY_STATUS + " integer,"
        			+ MessageData.KEY_TIMESTAMP + " long,"
        			+ MessageData.KEY_TIMESTAMP_SENT + " long,"
        			+ MessageData.KEY_TIMESTAMP_DELIVERED + " long,"
        			+ MessageData.KEY_TIMESTAMP_DISPLAYED + " long);");
        	
        	
        	db.execSQL("CREATE TABLE " + TABLE_MULTIMESSAGE + " ("
        			+ MultiMessageData.KEY_ID + " integer primary key autoincrement,"
        			+ MultiMessageData.KEY_MESSAGE_ID + " TEXT,"
        			+ MultiMessageData.KEY_CHAT_ID + " TEXT,"
        			+ MultiMessageData.KEY_SUBJECT + " TEXT,"
        			+ MultiMessageData.KEY_PARTICIPANTS + " TEXT,"
        			+ MultiMessageData.KEY_STATUS + " integer,"
        			+ MultiMessageData.KEY_DIRECTION + " integer,"
        			+ MultiMessageData.KEY_TIMESTAMP + " long);");
        	
        	db.execSQL("CREATE TABLE " + TABLE_GROUPMEMBER + " ("
        			+ GroupMemberData.KEY_ID + " integer primary key autoincrement,"
        			+ GroupMemberData.KEY_CHAT_ID + " TEXT ,"
        			+ GroupMemberData.KEY_CONTACT_NUMBER + " TEXT,"
        			+ GroupMemberData.KEY_CONTACT_TYPE + " TEXT,"
        			+ GroupMemberData.KEY_MEMBER_NAME + " TEXT,"
        			+ GroupMemberData.KEY_PORTRAIT + " TEXT);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHAT);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGE);
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
        int match = uriMatcher.match(uri);
        switch(match) {
            case CHATS:
			case RCSAPI_CHATS:
                return "vnd.android.cursor.dir/chat";
            case CHAT_ID:
			case RCSAPI_CHAT_ID:
                return "vnd.android.cursor.item/chat";
            case MESSAGES:
			case RCSAPI_MESSAGES:
                return "vnd.android.cursor.dir/message";
            case MESSAGE_ID:
			case RCSAPI_MESSAGE_ID:
                return "vnd.android.cursor.item/message";
			case ADD_CHAT_CONVERSATION_ID_COLUMN:
                return "vnd.android.cursor.item/chat";
			case ADD_MSG_CONVERSATION_ID_COLUMN :
                return "vnd.android.cursor.item/message";
			case MULTI_PARTICIPANT_CHATS:
                return "vnd.android.cursor.dir/multipart_chat";
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projectionIn, String selection, String[] selectionArgs, String sort) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        boolean isAlterTableQuery = false;
        
        // Generate the body of the query
        int match = uriMatcher.match(uri);
        switch(match) {
            case CHATS:
			case RCSAPI_CHATS:
		        qb.setTables(TABLE_CHAT);
		        break;
			case MESSAGES:
			case RCSAPI_MESSAGES:
		        qb.setTables(TABLE_MESSAGE);
                break;
			case CHAT_ID:
			case RCSAPI_CHAT_ID:
		        qb.setTables(TABLE_CHAT);
                qb.appendWhere(ChatData.KEY_CHAT_ID + "=");
                qb.appendWhere(uri.getPathSegments().get(1));
                break;
			case GROUP_MEMBER: 
				qb.setTables(TABLE_GROUPMEMBER);
               // qb.appendWhere(GroupMemberData.KEY_CHAT_ID + "=");
               // qb.appendWhere(uri.getPathSegments().get(1));
				break;
			case MESSAGE_ID:
			case RCSAPI_MESSAGE_ID:
		        qb.setTables(TABLE_MESSAGE);
                qb.appendWhere(MessageData.KEY_CHAT_ID + "= '" +
                		PhoneUtils.formatNumberToInternational(uri.getPathSegments().get(1)) + "'");
                break;
			case ADD_CHAT_CONVERSATION_ID_COLUMN:
				qb.setTables(TABLE_CHAT);
				isAlterTableQuery = true;
				break;
			case ADD_MSG_CONVERSATION_ID_COLUMN:
				qb.setTables(TABLE_MESSAGE);
				isAlterTableQuery = true;
				break;
			  case MULTI_PARTICIPANT_CHATS:
			        qb.setTables(TABLE_MULTIMESSAGE);
			        break;
			  case MULTI_PARTICIPANT_CHAT_ID:
				  qb.setTables(TABLE_MULTIMESSAGE);
	                qb.appendWhere(MultiMessageData.KEY_CHAT_ID + "=");
	                qb.appendWhere(uri.getPathSegments().get(1));
			        break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        
        SQLiteDatabase db = openHelper.getReadableDatabase();
        
        //if its a alter table query
        if(isAlterTableQuery){
        	String alterTableQuery = "ALTER TABLE " +qb.getTables();
        	alterTableQuery += " ADD COLUMN ";
        	if(qb.getTables().equals(TABLE_CHAT)){
        		
        		alterTableQuery += ChatData.KEY_CONVERSATION_ID + "  TEXT DEFAULT '' ";
        	}else if(qb.getTables().equals(TABLE_MESSAGE)){
        		alterTableQuery += MessageData.KEY_CONVERSATION_ID + "  TEXT DEFAULT '' ";
        	}
        	
        	db.execSQL(alterTableQuery);
        			
        	return null;
        }
        else{
	        Cursor c = qb.query(db, projectionIn, selection, selectionArgs, null, null, sort);
	
			// Register the contexts ContentResolver to be notified if the cursor result set changes
	        if (c != null) {
	            c.setNotificationUri(getContext().getContentResolver(), uri);
	        }
	        return c;
        }

    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        int count = 0;
        SQLiteDatabase db = openHelper.getWritableDatabase();

        int match = uriMatcher.match(uri);
        switch(match) {
	        case CHATS:
	        case RCSAPI_CHATS:
	            count = db.update(TABLE_CHAT, values, where, whereArgs);
		        break;
			case MESSAGES:
			case RCSAPI_MESSAGES:
	            count = db.update(TABLE_MESSAGE, values, where, null);
	            break;
			case GROUP_MEMBER :
	            count = db.update(TABLE_GROUPMEMBER, values, where, null);
	            break;
			case CHAT_ID:
                count = db.update(TABLE_CHAT, values,
                		ChatData.KEY_ID + "=" + Integer.parseInt(uri.getPathSegments().get(1)), null);
	            break;
			case MESSAGE_ID:
                count = db.update(TABLE_MESSAGE, values,
                		MessageData.KEY_ID + "=" + Integer.parseInt(uri.getPathSegments().get(1)), null);
	            break;
			 case MULTI_PARTICIPANT_CHATS:
		            count = db.update(TABLE_MULTIMESSAGE, values, where, null);
			        break;
			 case MULTI_PARTICIPANT_CHAT_ID:
	                count = db.update(TABLE_MULTIMESSAGE, values,
	                		MultiMessageData.KEY_CHAT_ID + "=" + Integer.parseInt(uri.getPathSegments().get(1)), null);
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
        switch(uriMatcher.match(uri)) {
	        case CHATS:
	        case CHAT_ID:
	        case RCSAPI_CHATS:
	        case RCSAPI_CHAT_ID:	
	    		long chatRowId = db.insert(TABLE_CHAT, null, initialValues);
	    		uri = ContentUris.withAppendedId(ChatData.CONTENT_URI, chatRowId);
	        	break;
	        case MESSAGES:
	        case MESSAGE_ID:
	        case RCSAPI_MESSAGE_ID:
	        case RCSAPI_MESSAGES:
	    		long msgRowId = db.insert(TABLE_MESSAGE, null, initialValues);
	    		uri = ContentUris.withAppendedId(MessageData.CONTENT_URI, msgRowId);
	        	break;
	        case GROUP_MEMBER:
	    		long grpMember = db.insert(TABLE_GROUPMEMBER, null, initialValues);
	    		uri = ContentUris.withAppendedId(GroupMemberData.CONTENT_URI, grpMember);
	        	break;
	        case MULTI_PARTICIPANT_CHAT_ID:
	        case MULTI_PARTICIPANT_CHATS:
	    		long multi_chatRowId = db.insert(TABLE_MULTIMESSAGE, null, initialValues);
	    		uri = ContentUris.withAppendedId(MultiMessageData.CONTENT_URI, multi_chatRowId);
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
        switch(uriMatcher.match(uri)) {
	        case CHATS:
	        case RCSAPI_CHATS:
	        	count = db.delete(TABLE_CHAT, where, whereArgs);
	        	break;
	        case GROUP_MEMBER:
	        	count = db.delete(TABLE_GROUPMEMBER, where, whereArgs);
	        	break;
	        case CHAT_ID:
	        case RCSAPI_CHAT_ID:
				count = db.delete(TABLE_CHAT, ChatData.KEY_ID + "="
						+ uri.getPathSegments().get(1)
						+ (!TextUtils.isEmpty(where) ? " AND ("	+ where + ')' : ""),
						whereArgs);
				break;
	        case MESSAGES:
	        case RCSAPI_MESSAGES:
	        	count = db.delete(TABLE_MESSAGE, where, whereArgs);
	        	break;
	        case MESSAGE_ID:
	        case RCSAPI_MESSAGE_ID:
				count = db.delete(TABLE_MESSAGE, MessageData.KEY_ID + "="
						+ PhoneUtils.formatNumberToInternational(uri.getPathSegments().get(1))
						+ (!TextUtils.isEmpty(where) ? " AND ("	+ where + ')' : ""),
						whereArgs);
				break;
	        case MULTI_PARTICIPANT_CHATS:
	        	count = db.delete(TABLE_MULTIMESSAGE, where, whereArgs);
	        	break;
	        case MULTI_PARTICIPANT_CHAT_ID:
				count = db.delete(TABLE_MULTIMESSAGE, MultiMessageData.KEY_ID + "="
						+ uri.getPathSegments().get(1)
						+ (!TextUtils.isEmpty(where) ? " AND ("	+ where + ')' : ""),
						whereArgs);
				break;
	        default:
	    		throw new SQLException("Failed to delete row " + uri);
        }
		getContext().getContentResolver().notifyChange(uri, null);
        return count;    
    }

	public static void backupChatDatabase(String account) {
	    	try {
		    	String packageName = "com.orangelabs.rcs";
		    	String dbFile = Environment.getDataDirectory() + "/data/" + packageName + "/databases/" + ChatProvider.DATABASE_NAME;
		    	File file = new File(dbFile);
		    	if (file.exists())
		    	{
		    	File backupFileRoot =new File( Environment.getExternalStorageDirectory()+"/Rcs" + "/data/" + packageName + "/databases/");
		    	backupFileRoot.mkdirs();
		    	File backupFile =new File(backupFileRoot,  ChatProvider.DATABASE_NAME +"_"+ account +".db");
		    	
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
    
	public static void backupChatDatabase(String account,String path) {
    	try {
	    	String packageName = "com.orangelabs.rcs";
	    	String dbFile = Environment.getDataDirectory() + "/data/" + packageName + "/databases/" + ChatProvider.DATABASE_NAME;
	    	File file = new File(dbFile);
	    	if (file.exists())
	    	{
	    	File backupFileRoot =new File( path);
	    	backupFileRoot.mkdirs();
	    	File backupFile =new File(backupFileRoot,  ChatProvider.DATABASE_NAME +"_"+ account +".db");
	    	
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
	
	public static void restoreChatDatabase(String account)
    {
    
    	try {
    		
   		// if (logger.isActivated()) {
        //        logger.debug("restoreAccountMessages : account:"+account);
         //   }
   		 
	    	String packageName = "com.orangelabs.rcs";
	    	String dbFile = Environment.getDataDirectory() + "/data/" + packageName + "/databases/" + ChatProvider.DATABASE_NAME;
	    	File backupFileRoot =new File( Environment.getExternalStorageDirectory()+"/Rcs" + "/data/" + packageName + "/databases/");
	    	String restoreFile = backupFileRoot +"/"+ ChatProvider.DATABASE_NAME +"_"+ account +".db";
           


	    	File file = new File(restoreFile);
	    	if (!file.exists()) {
	    	//	if (logger.isActivated()) {
	                 //logger.debug("error in restoreAccountMessages : account:"+account + "; "+restoreFile+" :file cant be created");
	       //      }
	    		return;
	    	}
	    	

               //delete the original file 
                File dbOriginalFile = new File(dbFile);
	              if (!dbOriginalFile.exists()) {
	                          //     dbOriginalFile.delete();
				dbOriginalFile.createNewFile();
              } 

                      new FileOutputStream(dbFile,false).close();
	             // dbOriginalFile.createNewFile();
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
	
	
	public static boolean restoreChatDatabase(String account, String path)
    {
    
    	try {
    		
   		// if (logger.isActivated()) {
        //        logger.debug("restoreAccountMessages : account:"+account);
         //   }
   		 
	    	String packageName = "com.orangelabs.rcs";
	    	String dbFile = Environment.getDataDirectory() + "/data/" + packageName + "/databases/" + ChatProvider.DATABASE_NAME;
	    	File backupFileRoot =new File( path);
	    	String restoreFile = backupFileRoot +"/"+ ChatProvider.DATABASE_NAME +"_"+ account +".db";
           


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
                             //  dbOriginalFile.delete();
				dbOriginalFile.createNewFile();
              } 

              new FileOutputStream(dbFile,false).close();
	    	OutputStream outStream = new FileOutputStream(dbFile,false);
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
