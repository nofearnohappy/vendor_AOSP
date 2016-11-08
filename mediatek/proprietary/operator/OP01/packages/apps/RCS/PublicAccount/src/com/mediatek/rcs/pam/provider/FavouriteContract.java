package com.mediatek.rcs.pam.provider;

import android.net.Uri;
import android.os.Environment;

import com.cmcc.ccs.chat.ChatMessage;

public interface FavouriteContract {
    interface FavouriteColumns {
        Uri CONTENT_URI = Uri.parse("content://com.cmcc.ccs.favor_message");

        String ID = "_id";
        String DATE = "date";
        String ADDRESS = "address";
        String SUBJECT = "subject";
        String PATH = "path";
        String CHAT_ID = "CHAT_ID";
        String SIZE = "size";

        String MESSAGE_ID = ChatMessage.MESSAGE_ID;
        String CONTACT_NUMBER = ChatMessage.CONTACT_NUMBER;
        String BODY = ChatMessage.BODY;
        String TIMESTAMP = ChatMessage.TIMESTAMP;
        String MIME_TYPE = ChatMessage.MIME_TYPE;
        String MESSAGE_STATUS = ChatMessage.MESSAGE_STATUS;
        String DIRECTION = ChatMessage.DIRECTION;
        String TYPE = ChatMessage.TYPE;
        String FLAG = ChatMessage.FLAG;
    }

    String MEDIA_FOLDER = Environment.getExternalStorageDirectory() + "/.Rcse/favorite_pa/";
    String PA_PREFIX = "PA_";
}
