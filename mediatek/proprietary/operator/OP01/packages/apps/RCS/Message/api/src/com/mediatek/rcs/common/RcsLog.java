package com.mediatek.rcs.common;

import android.net.Uri;

/**
 * RcsLog.
 */
public class RcsLog {

    /**
     * MessageColumn.
     */
    public static class MessageColumn {
        public static final Uri CONTENT_URI = Uri.parse("content://rcs");

        public static final String ID               = "_id";
        public static final String DATE_SENT        = "date_sent";
        public static final String SEEN             = "seen";
        public static final String LOCKED           = "locked";
        public static final String SUB_ID           = "sub_id";
        public static final String IPMSG_ID         = "ipmsg_id";
        public static final String STATE            = "state";
        public static final String CLASS            = "class";
        public static final String FILE_PATH        = "file_path";
        public static final String MESSAGE_ID       = "CHATMESSAGE_MESSAGE_ID";
        public static final String CHAT_ID          = "CHATMESSAGE_CHAT_ID";
        public static final String CONTACT_NUMBER   = "CHATMESSAGE_CONTACT_NUMBER";
        public static final String BODY             = "CHATMESSAGE_BODY";
        public static final String TIMESTAMP        = "CHATMESSAGE_TIMESTAMP";
        public static final String MESSAGE_STATUS   = "CHATMESSAGE_MESSAGE_STATUS";
        public static final String TYPE             = "CHATMESSAGE_TYPE";
        public static final String DIRECTION        = "CHATMESSAGE_DIRECTION";
        public static final String FLAG             = "CHATMESSAGE_FLAG";
        public static final String ISBLOCKED        = "CHATMESSAGE_ISBLOCKED";
        public static final String CONVERSATION     = "CHATMESSAGE_CONVERSATION";
        public static final String MIME_TYPE        = "CHATMESSAGE_MIME_TYPE";
    }

    /**
     * ThreadsColumn.
     */
    public static class ThreadsColumn {
        public static final Uri CONTENT_URI = Uri.parse("content://mms-sms-rcs/conversations");
        public static final Uri CONTENT_URI_STATUS =
                Uri.parse("content://mms-sms-rcs/conversations/status");

        public static final String ID               = "_id";
        public static final String SNIPPET          = "snippet";
        public static final String SNIPPET_CS       = "snippet_cs";
        public static final String TYPE             = "type";
        public static final String DATE             = "date";
        public static final String READCOUNT        = "readcount";
        public static final String MESSAGE_COUNT    = "message_count";
        public static final String ERROR            = "error";
        public static final String READ             = "read";
        public static final String HAS_ATTACHMENT   = "has_attachment";
        public static final String STATUS           = "status";
        public static final String RECIPIENT_IDS    = "recipient_ids";
        public static final String ARCHIVED         = "archived";
        public static final String CLASS            = "class";
        public static final String RECIPIENTS       = "CHATMESSAGE_RECIPIENTS";
        public static final String FLAG             = "CHATMESSAGE_FLAG";
        public static final String MESSAGE_TYPE     = "CHATMESSAGE_TYPE";
        public static final String MIME_TYPE        = "CHATMESSAGE_MIME_TYPE";
    }

    /**
     * Class.
     */
    public static final class Class {
        public static final int NORMAL     = 0;
        public static final int BURN       = 1;
        public static final int EMOTICON   = 2;
        public static final int CLOUD      = 3;
        public static final int SYSTEM     = 11;
        public static final int INVITATION = 12;
    }

    /**
     * MessageStatus.
     */
    public static final class MessageStatus {
        public static final int UNREAD           = 0;
        public static final int READ             = 2;
        public static final int SENDING          = 3;
        public static final int SENT             = 4;
        public static final int FAILED           = 5;
        public static final int TO_SEND          = 6;
        public static final int DELIVERED        = 7;
    }

    /**
     * MessageType.
     */
    public static final class MessageType {
        public static final int SMSMMS     = 0;
        public static final int IM         = 1;
        public static final int FT         = 2;
        public static final int XML        = 3;
    }

    /**
     * ThreadFlag.
     */
    public static final class ThreadFlag {
        public static final int OTO        = 1;
        public static final int OTM        = 2;
        public static final int MTM        = 3;
        public static final int OFFICIAL   = 4;
    }

    /**
     * Direction.
     */
    public static final class Direction {
        public static final int INCOMING      = 0;
        public static final int OUTGOING      = 1;
    }
}
