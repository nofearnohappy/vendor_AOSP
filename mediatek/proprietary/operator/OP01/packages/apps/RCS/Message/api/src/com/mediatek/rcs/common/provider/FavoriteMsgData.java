package com.mediatek.rcs.common.provider;

import android.net.Uri;
import com.cmcc.ccs.chat.ChatMessage;

public class FavoriteMsgData {

//    public static final Uri CONTENT_URI = Uri.parse("content://com.mediatek.message.favorite");
    public static final Uri CONTENT_URI = Uri.parse("content://com.cmcc.ccs.favor_message");

    public static final String AUTHORITY = "com.cmcc.ccs.favor_message";

    public static final String COLUMN_ID            = "_id";
    public static final String COLUMN_DATE          = "date";
   // public static final String COLUMN_ADDRESS       = "address";
   // public static final String COLUMN_SUBJECT       = "subject";
   // public static final String COLUMN_PATH          = "path";
    public static final String COLUMN_CHATID        = "CHATMESSAGE_CHAT_ID";
   // public static final String COLUMN_SIZE          = "size";
    //device api
    public static final String COLUMN_DA_ID         = ChatMessage.MESSAGE_ID;
    public static final String COLUMN_DA_CONTACT    = ChatMessage.CONTACT_NUMBER;
    public static final String COLUMN_DA_BODY       = ChatMessage.BODY;
    public static final String COLUMN_DA_TIMESTAMP  = ChatMessage.TIMESTAMP;
    public static final String COLUMN_DA_MIME_TYPE  = ChatMessage.MIME_TYPE;
    public static final String COLUMN_DA_MESSAGE_STATUS  = ChatMessage.MESSAGE_STATUS;
    public static final String COLUMN_DA_DIRECTION  = ChatMessage.DIRECTION;
    public static final String COLUMN_DA_TYPE       = ChatMessage.TYPE;
    public static final String COLUMN_DA_FLAG       = ChatMessage.FLAG;
    public static final String COLUMN_DA_FILENAME   = ChatMessage.FILENAME; //only use in ft record
    public static final String COLUMN_DA_FILEICON   = ChatMessage.FILEICON;//only use in ft record
    public static final String COLUMN_DA_FILESIZE   = ChatMessage.FILESIZE;//only use in ft record

}
