package com.mediatek.rcs.pam.provider;

import android.net.Uri;

import org.gsma.joyn.chat.ChatLog.Message;
import org.gsma.joyn.ft.FileTransferLog;

// FIXME use value from RCSe instead
public interface RcseProviderContract {
    public interface FileTransferColumns {
        Uri CONTENT_URI = Uri.parse("content://com.gsma.joyn.provider.ft/ft");
        String FT_ID = FileTransferLog.FT_ID;
        String CONTACT_NUMBER = FileTransferLog.CONTACT_NUMBER;
        String FILENAME = FileTransferLog.FILENAME;
        String TYPE = FileTransferLog.MIME_TYPE;
        String FILEICON = FileTransferLog.FILEICON;
        String DIRECTION = FileTransferLog.DIRECTION;
        String FILE_SIZE = FileTransferLog.FILESIZE;
        String TRANSFERRED = FileTransferLog.TRANSFERRED;
        String TIMESTAMP = FileTransferLog.TIMESTAMP;
        String STATE = FileTransferLog.STATE;
    }

    String[] FILE_TRANSFER_FULL_PROJECTION = {
        FileTransferColumns.FT_ID,
        FileTransferColumns.CONTACT_NUMBER,
        FileTransferColumns.FILENAME,
        FileTransferColumns.TYPE,
        FileTransferColumns.FILEICON,
        FileTransferColumns.DIRECTION,
        FileTransferColumns.FILE_SIZE,
        FileTransferColumns.TRANSFERRED,
        FileTransferColumns.TIMESTAMP,
        FileTransferColumns.STATE,
    };

    public interface MessageColumns {
        Uri CONTENT_URI = Uri.parse("content://org.gsma.joyn.provider.chat/message");
        String MESSAGE_ID = Message.MESSAGE_ID;
        String CHAT_ID = Message.CHAT_ID;
        String CONTACT_NUMBER = Message.CONTACT_NUMBER;
        String BODY = Message.BODY;
        String TIMESTAMP = Message.TIMESTAMP;
        String TIMESTAMP_SENT = Message.TIMESTAMP_SENT;
        String TIMESTAMP_DELIVERED = Message.TIMESTAMP_DELIVERED;
        String TIMESTAMP_DISPLAYED = Message.TIMESTAMP_DISPLAYED;
        String MIME_TYPE = Message.MIME_TYPE;
        String MESSAGE_STATUS = Message.MESSAGE_STATUS;
        String DIRECTION = Message.DIRECTION;
        String MESSAGE_TYPE = Message.MESSAGE_TYPE;
    }

    String[] MESSAGE_FULL_PROJECTION = {
        MessageColumns.MESSAGE_ID,
        MessageColumns.CHAT_ID,
        MessageColumns.CONTACT_NUMBER,
        MessageColumns.BODY,
        MessageColumns.TIMESTAMP,
        MessageColumns.TIMESTAMP_SENT,
        MessageColumns.TIMESTAMP_DELIVERED,
        MessageColumns.TIMESTAMP_DISPLAYED,
        MessageColumns.MIME_TYPE,
        MessageColumns.MESSAGE_STATUS,
        MessageColumns.DIRECTION,
        MessageColumns.MESSAGE_TYPE,
    };
}
