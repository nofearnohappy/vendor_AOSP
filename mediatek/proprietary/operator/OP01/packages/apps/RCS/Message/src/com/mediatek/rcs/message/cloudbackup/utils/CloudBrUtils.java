package com.mediatek.rcs.message.cloudbackup.utils;

import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.mediatek.rcs.common.binder.RCSServiceManager;
import com.mediatek.rcs.common.provider.FavoriteMsgData;
import com.mediatek.rcs.common.provider.GroupChatData;
import com.mediatek.rcs.common.provider.GroupMemberData;
import com.mediatek.rcs.common.RcsLog;


/**
 * utils class.
 */
public class CloudBrUtils {
    public static final String MODULE_TAG = "com.mediatek.rcs.message.cloudbackup/";
    private static final String CLASS_TAG = MODULE_TAG + "CloudBrUtils";

    /**
     * get rcs account number.
     * @return
     */
    public static String getMyNumber() {
        return RCSServiceManager.getInstance().getMyNumber();
    }

    /**
     * whether the ip msg is need backup according to cmcc sepc.
     * @param msgCursor
     * @return
     */
    public static boolean isMsgNeedBackup(Cursor rcsCursor) {
        boolean result = true;
        int msgStatus = rcsCursor.getInt(rcsCursor
                .getColumnIndex(CloudBrUtils.RcsMessage.MESSAGE_COLUMN_MESSAGE_STATUS));
        int isBlock = rcsCursor.getInt(rcsCursor
                .getColumnIndex(CloudBrUtils.RcsMessage.MESSAGE_COLUMN_ISBLOCKED));
        int msgClass = rcsCursor.getInt(rcsCursor.getColumnIndex(
                CloudBrUtils.RcsMessage.MESSAGE_COLUMN_MSG_CLASS));

        return FileUtils.isNeedBackup(msgStatus, isBlock, msgClass);
    }

    /**
     * Constant class.
     */
    public static class BackupConstant {
        public static final String CHARSET_UTF8 = "utf-8";
        public static final String FROM = "From:";
        public static final String DATE = "Date:";
        public static final String SUBJECT = "Subject:";
        public static final String CONVERST_ID = "Conversation-ID:";
        public static final String CONTRIBUT_ID = "Contribution-ID:";
        public static final String IMDN_MSG_ID = "Imdn-Message-ID:";
        public static final String IN_REPLY = "InReplyTo-Contribution-ID:";
        public static final String SESSION_CONTENT_TYPE = "Content-type: multipart"
                + "/related;boundary=cpm; type=\"Application/X-CPM-Session\"";
        public static final String ROOT_CONTENT_TYPE = "Content-type: Application/X-CPM-Session";
        public static final String CPIM_MESSAGE_TYPE = "Message/CPIM";
        public static final String ANONYMOUS = "sip:anonymous@anonymous.invalid";
        public static final String ENCODING_BASE64 = "Content-Transfer-Encoding: base64";
        public static final String ENCODING_BINARY = "Content-Transfer-Encoding: binary";
        public static final String SESSION_TYPE = "session-type";
        public static final String FT_CONTENT_TYPE = "Application/X-CPM-File-Transfer";

        public static final String BOUNDDARY = "\r\n\r\n--cpm\r\n";
        public static final String BOUNDDARY_CPM = "--cpm";
        public static final String BOUNDDARY_CPM_END = "--cpm--";
        public static final String LINE_BREAK = "\r\n";

        public static final String CONTENT_TYPE = "Content-type:";
        public static final String TO = "To:";
        public static final String LENGTH = "Content-Length:";
        public static final String SEPRATOR = ":";

        public static final String MTK_CHAT_ID = "Chat-ID:";
        public static final String MTK_CHAT_STATE = "Chat-State:";
        public static final String MTK_CHAT_STATUS = "Chat-Status:";
        public static final String MTK_CHAT_CHAIRMAN = "Chat-Chairman:";
        //rcs message table status
        public static final String MTK_MSG_STATUS = "Msg-Status:";
        //chat db message talbe status
        public static final String MTK_STATUS = "Chat-Msg-Status:";
        public static final String MTK_DIRECTION = "Direction:";
        public static final String MTK_DISPLAYED_TIME = "Displayed-Time:";
        public static final String MTK_DELIVERED_TIME = "Delivered-Time:";
        public static final String MTK_SEND_TIME = "Send-Time:";
        public static final String MTK_SEEN = "Seen:";
        public static final String MTK_LOCK = "Lock:";
        public static final String MTK_SUB_ID = "SubId:";
        //rcs message table msg_type
        public static final String MTK_TYPE = "Type:";
        //ft db ft talbe session_type
        public static final String MTK_SESSION_TYPE = "Session_Type:";
        //chat db message table type
        public static final String MTK_CHAT_TYPE = "Chat-Msg-Type:";
        //rcs message table class
        public static final String MTK_CLASS = "Msg-Class:";
        public static final String MTK_DURATION = "Duration:";
       // public static final String MTK_FILE_ICON = "File-Icon:";
        public static final String MTK_FTID = "Ft-ID:";
        public static final String MTK_FILE_NAME = "File-Name:";
        public static final String MTK_FAV_FLAG = "Msg-Flag:";

        public static final String VEM_XMLNS =
                "http://vemoticon.bj.ims.mnc000.mcc460.3gppnetwork.org/types";
        public static final String CLD_FILE_XMLNS = "http://cloudfile.cmcc.com/types";

        public static final String FILE_TRANSFER_TYPE = "file-transfer-type";
        public static final String PARTICIPANTS = "invited-participants";
        public static final String MTK_MEMBER_INFO = "member-info";
        public static final String MTK_MEMBER_NO = "number";
        public static final String MTK_MEMBER_STATE = "state";
        public static final String MTK_MEMBER_NAME = "name";

        public static final String FILE_OBJECT = "file-object";
        public static final String CID = "cid";
        public static final String SDP = "sdp";
        public static final String FILE_DESCRIPTION = "i=file transfer description";
        public static final String FILE_SENDONLY = " a=sendonly";
        public static final String FILE_SELECTOR = " a=file-selector:";
        public static final String FILE_DISPOSITION = " a=file-disposition:";
        public static final String FILE_CREATE_DATE = " a=file-date:creation:";

        public static final String DATE_TIME = "DateTime:";

        public static final String VMSG = "VMSG";
    }

    /**
     * Result code during backup and restore.
     * result dialog will according to it to show result.
     */
    public static class ResultCode {
        public static final int IO_EXCEPTION = -1;
        public static final int OK = 0;
        public static final int INSERTDB_EXCEPTION = -2;
        public static final int DB_EXCEPTION = -3;
        public static final int BACKUP_FILE_ERROR = -4;
        public static final int PARSE_XML_ERROR = -5;
        public static final int SERVICE_CANCELED = -6;
        public static final int NETWORK_ERROR = -7;
        public static final int BACKUP_BEFOR_RESTORE_EXCEPTION = -8;
        public static final int BACKUP_FOLDER_EMPTY = -9;
        public static final int OTHER_EXCEPTION = -10;
    }

    /**
     * Content type that is defined by cmcc spec.
     */
    public static class ContentType {
        public static final String GROUP_CHAT_TYPE = "multipart/related;boundary=cpm;"
                + "type=\"Application/X-CPM-Session\"";
        public static final String GROUP_FT_TYPE = "multipart/related;boundary=cpm;"
                + "type=\"Application/X-CPM-File-Transfer\"";
        public static final String TEXT_TYPE = "text/plain";
        public static final String VEMOTION_TYPE = "application/vemoticon+xml";
        public static final String CLOUDFILE_TYPE = "application/cloudfile+xml";
        public static final String LOCATION_TYPE = "application/vnd.gsma.rcspushlocation+xml";
        public static final String VCARD_TYPE = "text/vcard";
    }

    /**
     * File type when restore data from service.
     * we need analyze the file type before restore the file.
     */
    public static class BackupDataFileType {
        public static final int VMSG = 0X01;
        public static final int PDU = 0X02;
        public static final int IPMSG = 0X04;
        public static final int MMS_XML = 0X08;
    }

    public static final Uri CHAT_CONTENT_URI = Uri
            .parse("content://org.gsma.joyn.provider.chat/message");
    public static final Uri CHAT_CHAT_URI = Uri
            .parse("content://org.gsma.joyn.provider.chat/chat");
    public static final Uri CHAT_MULTI_URI = Uri
            .parse("content://org.gsma.joyn.provider.chat/multimessage");
    public static final Uri GROUP_MEMBER_URI = GroupMemberData.CONTENT_URI;
    public static final Uri FT_URI = Uri.parse("content://org.gsma.joyn.provider.ft/ft");
    public static final Uri SMS_URI = Uri.parse("content://sms");
    public static final Uri RCS_URI = Uri.parse("content://rcs");
    public static final Uri SMS_THREAD_URI = Uri.parse("content://threads");
    public static final Uri FAVOTIRE_URI = FavoriteMsgData.CONTENT_URI;
    public static final Uri GROUP_CHAT_URI = GroupChatData.CONTENT_URI;
    public static final Uri MMS_SMS_URI = Uri.parse("content://mms-sms/conversations/");

    public static final String STORAGE_PATH = "/storage/sdcard0/cloudBackup";
    public static final String BACKUP_RESULT = "backupResult";
    public static final String RESTORE_RESULT = "restoreResult";
    public static final String MESSAGE_PATH = "messagePath";
    public static final String FAVORITE_PATH = "favoritePath";

    /**
     * The name of the column containing the unique ID for a row.
     * <P>
     * Type: primary key
     * </P>
     */
    public static final String ID = "_id";

    /**
     * The name of the column containing the unique ID of the group chat.
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String CHAT_ID = "chat_id";

/*    public static class Sms {
        public static final String SUB_ID = "sub_id";
        public static final String TYPE = "type";
        public static final String READ = "read";
        public static final String BODY = "body";
        public static final String THREAD_ID = "thread_id";
        public static final String ADDRESS = "address";
        public static final String IPMSG_ID = "ipmsg_id";
        public static final String DATE = "date";
    }*/

    /**
     *Rcs message tablee colunm.
     */
    public static class RcsMessage {
        public static final String MESSAGE_COLUMN_DATE_SENT = RcsLog.MessageColumn.DATE_SENT;
        public static final String MESSAGE_COLUMN_SEEN = RcsLog.MessageColumn.SEEN;
        public static final String MESSAGE_COLUMN_LOCKED = RcsLog.MessageColumn.LOCKED;
        public static final String MESSAGE_COLUMN_SUB_ID = RcsLog.MessageColumn.SUB_ID;
        public static final String MESSAGE_COLUMN_IPMSG_ID = RcsLog.MessageColumn.IPMSG_ID;
        public static final String MESSAGE_COLUMN_MSG_CLASS = RcsLog.MessageColumn.CLASS;
        public static final String MESSAGE_COLUMN_FILE_PATH = RcsLog.MessageColumn.FILE_PATH;
        public static final String MESSAGE_COLUMN_MESSAGE_ID =
                RcsLog.MessageColumn.MESSAGE_ID;
        public static final String MESSAGE_COLUMN_CHAT_ID = RcsLog.MessageColumn.CHAT_ID;
        public static final String MESSAGE_COLUMN_CONTACT_NUMBER =
                RcsLog.MessageColumn.CONTACT_NUMBER;
        public static final String MESSAGE_COLUMN_BODY = RcsLog.MessageColumn.BODY;
        public static final String MESSAGE_COLUMN_TIMESTAMP = RcsLog.MessageColumn.TIMESTAMP;
        public static final String MESSAGE_COLUMN_MESSAGE_STATUS =
                RcsLog.MessageColumn.MESSAGE_STATUS;
        public static final String MESSAGE_COLUMN_TYPE = RcsLog.MessageColumn.TYPE;
        public static final String MESSAGE_COLUMN_DIRECTION = RcsLog.MessageColumn.DIRECTION;
        public static final String MESSAGE_COLUMN_FLAG = RcsLog.MessageColumn.FLAG;
        public static final String MESSAGE_COLUMN_ISBLOCKED = RcsLog.MessageColumn.ISBLOCKED;
        public static final String MESSAGE_COLUMN_CONVERSATION =
                RcsLog.MessageColumn.CONVERSATION;
        public static final String MESSAGE_COLUMN_MIME_TYPE = RcsLog.MessageColumn.MIME_TYPE;
    }
    /**
     * Group chat
     */
    public static class GroupChat {
        public static final String STATUS = GroupChatData.KEY_STATUS;
        public static final String THREAD_ID = GroupChatData.KEY_THREAD_ID;
        public static final String CHAT_ID = GroupChatData.KEY_CHAT_ID;
    }
    /**
     * Group chat
     */
    public static class Chat {

        public static final String CHAT_ID = "chat_id";
        /**
         * The name of the column containing the state of the group chat.
         * <P>
         * Type: INTEGER
         * </P>
         * @see GroupChat.State
         */
        public static final String STATE = "state";

        /**
         * The name of the column containing the subject of the group chat.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String SUBJECT = "subject";
        public static final String REJOIN_ID = "rejoin_id";

        /**
         * The name of the column containing the subject of the group chat.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String PARTICIPANTS_LIST = "participants";
        /**
         * The name of the column containing the direction of the group chat.
         * <P>
         * Type: INTEGER
         * </P>
         * @see GroupChat.Direction
         */
        public static final String DIRECTION = "direction";

        /**
         * The name of the column containing the time when group chat is
         * created.
         * <P>
         * Type: LONG
         * </P>
         */
        public static final String TIMESTAMP = "timestamp";

        /**
         * The name of the column containing the conversation ID.(in case of CPM
         * only its added)
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String CONVERSATION_ID = "conversation_id";

        /**
         * CMCC changes
         */

        /**
         * The name of the column containing the direction of the group chat.
         * <P>
         * Type: INTEGER
         * </P>
         *
         * @see GroupChat.Direction
         */
        public static final String CHAIRMAN = "chairman";

    }

    public static class Message {

        /**
         * Content provider URI for chat messages of a given conversation. In
         * case of single chat the conversation is identified by the contact
         * phone number. In case of group chat the the conversation is
         * identified by the unique chat ID.
         */
        public static final Uri CONTENT_CHAT_URI = Uri
                .parse("content://org.gsma.joyn.provider.chat/message/#");

        /**
         * The name of the column containing the unique ID for a row.
         * <P>
         * Type: primary key
         * </P>
         */
        public static final String ID = "_id";

        /**
         * The name of the column containing the chat ID.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String CHAT_ID = "chat_id";

        /**
         * The name of the column containing the message ID.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String MESSAGE_ID = "msg_id";

        /**
         * The name of the column containing the message status.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String MESSAGE_STATUS = "status";

        /**
         * The name of the column containing the message direction.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String DIRECTION = "direction";

        /**
         * The name of the column containing the type of message.
         * <P>
         * Type: INTEGER
         * </P>
         * @see ChatLog.Message.Type
         */
        public static final String MESSAGE_TYPE = "msg_type";

        /**
         * The name of the column containing the identity of the sender of the
         * message.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String CONTACT_NUMBER = "sender";

        /**
         * The name of the column containing the message body.
         * <P>
         * Type: BLOB
         * </P>
         */
        public static final String BODY = "body";

        /**
         * The name of the column containing the time when message is created.
         * <P>
         * Type: LONG
         * </P>
         */
        public static final String TIMESTAMP = "timestamp";

        /**
         * The name of the column containing the time when message is sent. If 0
         * means not sent.
         * <P>
         * Type: LONG
         * </P>
         */
        public static final String TIMESTAMP_SENT = "timestamp_sent";

        /**
         * The name of the column containing the time when message is delivered.
         * If 0 means not delivered.
         * <P>
         * Type: LONG
         * </P>
         */
        public static final String TIMESTAMP_DELIVERED = "timestamp_delivered";

        /**
         * The name of the column containing the time when message is displayed.
         * If 0 means not displayed.
         * <P>
         * Type: LONG
         * </P>
         */
        public static final String TIMESTAMP_DISPLAYED = "timestamp_displayed";

        /**
         * The name of the column containing the MIME-TYPE of the message body.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String MIME_TYPE = "mime_type";

        /**
         * M:for adding alias or display name The name of the column containing
         * the display name user. in case of O2O only in SYSTEM_MSG this value
         * will be set. in case of GROUP CHAT, it will be set for all messages
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String DISPLAY_NAME = "display_name";
        /**@*/

        /*
         * M: CPM related changes
         */
        /**
         * The name of the column containing the conversation ID.(in case of CPM
         * only its added)
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String CONVERSATION_ID = "conversation_id";

        /*
         * @: ENDS
         */

        /**
         * Type of the message
         */
        public static class Type {
            /**
             * Content message
             */
            public static final int CONTENT = 0;

            /**
             * System message
             */
            public static final int SYSTEM = 1;

            /**
             * Spam message
             */
            public static final int SPAM = 2;

            /**
             * burn message
             */
            public static final int BURN = 3;

            /**
             * vemotion message
             */
            public static final int VEMOTION = 4;

            /**
             * cloudfile message
             */
            public static final int CLOUDFILE = 5;

        }

        /**
         * Direction of the message
         */
        public static class Direction {
            /**
             * Incoming message
             */
            public static final int INCOMING = 0;

            /**
             * Outgoing message
             */
            public static final int OUTGOING = 1;

            /**
             * Irrelevant or not applicable (e.g. for a system message)
             */
            public static final int IRRELEVANT = 2;
        }

        /**
         * Status of the message
         */
        public static class Status {
            /**
             * Status of a content message
             */
            public static class Content {
                /**
                 * The message has been delivered, but we don't know if the
                 * message has been read by the remote
                 */
                public static final int UNREAD = 0;

                /**
                 * The message has been delivered and a displayed delivery
                 * report is requested, but we don't know if the message has
                 * been read by the remote
                 */
                public static final int UNREAD_REPORT = 1;

                /**
                 * The message has been read by the remote (i.e. displayed)
                 */
                public static final int READ = 2;

                /**
                 * The message is in progress of sending
                 */
                public static final int SENDING = 3;

                /**
                 * The message has been sent
                 */
                public static final int SENT = 4;

                /**
                 * The message is failed to be sent
                 */
                public static final int FAILED = 5;

                /**
                 * The message is queued to be sent by joyn service when
                 * possible
                 */
                public static final int TO_SEND = 6;

                /**
                 * The message is a spam message
                 */
                public static final int BLOCKED = 7;
            }

            /**
             * Status of the system message
             */
            public static class System {
                /**
                 * Invitation of a participant is pending
                 */
                public static final int PENDING = 0;

                /**
                 * Invitation accepted by a participant
                 */
                public static final int ACCEPTED = 1;

                /**
                 * Invitation declined by a participant
                 */
                public static final int DECLINED = 2;
                /**
                 * Invitation of a participant has failed
                 */
                public static final int FAILED = 3;
                /**
                 * Participant has joined the group chat
                 */
                public static final int JOINED = 4;
                /**
                 * Participant has left the group chat (i.e. departed)
                 */
                public static final int GONE = 5;

                /**
                 * Participant has been disconnected from the group chat (i.e.
                 * booted)
                 */
                public static final int DISCONNECTED = 6;

                /**
                 * Participant is busy
                 */
                public static final int BUSY = 7;
            }
        }
    }

    /**
     * RCSe/RcsStack/src/org/gsma/joyn/ft/FileTransferLog.java.
     */
    public static class Ft {
        /**
         * The name of the column containing the unique ID of the file transfer.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String FT_ID = "ft_id";

        /**
         * The name of the column containing the MSISDN of the sender.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String CONTACT_NUMBER = "contact_number";

        /**
         * The name of the column containing the filename (absolute path).
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String FILENAME = "filename";

        /**
         * The name of the column containing the file size to be transferred (in
         * bytes).
         * <P>
         * Type: LONG
         * </P>
         */
        public static final String FILESIZE = "filesize";

        /**
         * The name of the column containing the MIME-type of the file.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String MIME_TYPE = "mime_type";

        /**
         * The name of the column containing the direction of the transfer.
         * <P>
         * Type: INTEGER
         * </P>
         *
         * @see FileTransfer.Direction
         */
        public static final String DIRECTION = "direction";

        /**
         * The name of the column containing the amount of data transferred (in
         * bytes).
         * <P>
         * Type: LONG
         * </P>
         */
        public static final String TRANSFERRED = "transferred";

        /**
         * The name of the column containing the time when transfer is
         * initiated.
         * <P>
         * Type: LONG
         * </P>
         */
        public static final String TIMESTAMP = "timestamp";

        /**
         * The name of the column containing the time when file is sent. If 0
         * means not sent.
         * <P>
         * Type: LONG
         * </P>
         */
        public static final String TIMESTAMP_SENT = "timestamp_sent";

        /**
         * The name of the column containing the time when file is delivered. If
         * 0 means not delivered.
         * <P>
         * Type: LONG
         * </P>
         */
        public static final String TIMESTAMP_DELIVERED = "timestamp_delivered";

        /**
         * The name of the column containing the time when file is displayed. If
         * 0 means not displayed.
         * <P>
         * Type: LONG
         * </P>
         */
        public static final String TIMESTAMP_DISPLAYED = "timestamp_displayed";

        /**
         * The name of the column containing the state of the transfer.
         * <P>
         * Type: INTEGER
         * </P>
         * @see FileTransfer.State
         */
        public static final String STATE = "state";

        /**
         * The name of the column containing the file icon (absolute path).
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String FILEICON = "fileicon";

        /**
         * The name of the column containing the chat ID used for the file
         * transfer in group chat.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String CHAT_ID = "chat_id";

        public static final String MSG_ID = "msg_id";

        public static final String DURATION = "duration";

        public static final String SESSION_TYPE = "session_type";

        /**
         * RCSe/RcsStack/src/org/gsma/joyn/ft/FileTransfer.java.
         */
        public static class Type {
            /**
             * Content
             */
            public static final int CHAT = 0;

            /**
             * BURN message
             */
            public static final int BURN = 1;

        }

        /**
         * RCSe/RcsStack/src/org/gsma/joyn/ft/FileTransfer.java.
         */
        public static class State {
            /**
             * Unknown state
             */
            public final static int UNKNOWN = 0;

            /**
             * File transfer invitation received
             */
            public final static int INVITED = 1;

            /**
             * File transfer invitation sent
             */
            public final static int INITIATED = 2;

            /**
             * File transfer is started
             */
            public final static int STARTED = 3;

            /**
             * File transfer has been transferred with success
             */
            public final static int TRANSFERRED = 4;

            /**
             * File transfer has been aborted
             */
            public final static int ABORTED = 5;

            /**
             * File transfer has failed
             */
            public final static int FAILED = 6;

            /**
             * File transfer has been delivered
             */
            public final static int DELIVERED = 7;

            /**
             * File transfer has been displayed or opened
             */
            public final static int DISPLAYED = 8;

            /**
             * File transfer has been PAUSED
             */
            public final static int PAUSED = 9;
        }

        /**
         * Direction of the transfer
         * RCSe/RcsStack/src/org/gsma/joyn/ft/FileTransfer.java.
         */
        public static class Direction {
            /**
             * Incoming transfer
             */
            public static final int INCOMING = 0;
            /**
             * Outgoing transfer
             */
            public static final int OUTGOING = 1;
        }
    }

    /**
     * Chat message from a single chat or group chat
     */
    public static class MultiMessage {
        /**
         * Content provider URI for chat messages
         */
        public static final Uri CONTENT_URI = Uri
                .parse("content://org.gsma.joyn.provider.chat/multimessage");

        /**
         * Content provider URI for chat messages of a given conversation. In
         * case of single chat the conversation is identified by the contact
         * phone number. In case of group chat the the conversation is
         * identified by the unique chat ID.
         */
        public static final Uri CONTENT_CHAT_URI = Uri
                .parse("content://org.gsma.joyn.provider.chat/multimessage/#");

        /**
         * The name of the column containing the unique ID for a row.
         * <P>
         * Type: primary key
         * </P>
         */
        public static final String ID = "_id";

        /**
         * The name of the column containing the unique ID of the group chat.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String CHAT_ID = "chat_id";

        /**
         * The name of the column containing the state of the group chat.
         * <P>
         * Type: INTEGER
         * </P>
         * @see GroupChat.State
         */
        public static final String STATE = "state";

        /**
         * The name of the column containing the subject of the group chat.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String SUBJECT = "subject";

        /**
         * The name of the column containing the subject of the group chat.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String PARTICIPANTS_LIST = "participants";

        /**
         * The name of the column containing the direction of the group chat.
         * <P>
         * Type: INTEGER
         * </P>
         * @see GroupChat.Direction
         */
        public static final String DIRECTION = "direction";

        /**
         * The name of the column containing the time when group chat is
         * created.
         * <P>
         * Type: LONG
         * </P>
         */
        public static final String TIMESTAMP = "timestamp";

        /*
         * @: ENDS
         */

        /**
         * Type of the message
         */
        public static class Type {
            /**
             * Content message
             */
            public static final int CONTENT = 0;

            /**
             * System message
             */
            public static final int SYSTEM = 1;

            /**
             * Spam message
             */
            public static final int SPAM = 2;
        }

        /**
         * Direction of the message
         */
        public static class Direction {
            /**
             * Incoming message
             */
            public static final int INCOMING = 0;

            /**
             * Outgoing message
             */
            public static final int OUTGOING = 1;

            /**
             * Irrelevant or not applicable (e.g. for a system message)
             */
            public static final int IRRELEVANT = 2;
        }

        /**
         * Status of the message
         */
        public static class Status {
            /**
             * Status of a content message
             */
            public static class Content {
                /**
                 * The message has been delivered, but we don't know if the
                 * message has been read by the remote
                 */
                public static final int UNREAD = 0;

                /**
                 * The message has been delivered and a displayed delivery
                 * report is requested, but we don't know if the message has
                 * been read by the remote
                 */
                public static final int UNREAD_REPORT = 1;

                /**
                 * The message has been read by the remote (i.e. displayed)
                 */
                public static final int READ = 2;

                /**
                 * The message is in progress of sending
                 */
                public static final int SENDING = 3;

                /**
                 * The message has been sent
                 */
                public static final int SENT = 4;

                /**
                 * The message is failed to be sent
                 */
                public static final int FAILED = 5;

                /**
                 * The message is queued to be sent by joyn service when
                 * possible
                 */
                public static final int TO_SEND = 6;

                /**
                 * The message is a spam message
                 */
                public static final int BLOCKED = 7;
            }

            /**
             * Status of the system message
             */
            public static class System {
                /**
                 * Invitation of a participant is pending
                 */
                public static final int PENDING = 0;

                /**
                 * Invitation accepted by a participant
                 */
                public static final int ACCEPTED = 1;

                /**
                 * Invitation declined by a participant
                 */
                public static final int DECLINED = 2;

                /**
                 * Invitation of a participant has failed
                 */
                public static final int FAILED = 3;

                /**
                 * Participant has joined the group chat
                 */
                public static final int JOINED = 4;

                /**
                 * Participant has left the group chat (i.e. departed)
                 */
                public static final int GONE = 5;

                /**
                 * Participant has been disconnected from the group chat (i.e.
                 * booted)
                 */
                public static final int DISCONNECTED = 6;

                /**
                 * Participant is busy
                 */
                public static final int BUSY = 7;
            }
        }
    }

    public static class GroupChatMember {
        public static final String COLUMN_CHAT_ID = GroupMemberData.COLUMN_CHAT_ID;
        public static final String COLUMN_CONTACT_NUMBER = GroupMemberData.COLUMN_CONTACT_NUMBER;
        public static final String COLUMN_CONTACT_NAME = GroupMemberData.COLUMN_CONTACT_NAME;
        public static final String COLUMN_TYPE = GroupMemberData.COLUMN_TYPE;
        public static final String COLUMN_PORTRAIT = GroupMemberData.COLUMN_PORTRAIT;
        public static final String COLUMN_STATE = GroupMemberData.COLUMN_STATE;

        public static final class STATE {
            /**
                 *
                 */
            public static final int STATE_PENDING = 0;

            /**
                 *
                 */
            public static final int STATE_CONNECTED = 1;

            /**
                 *
                 */
            public static final int STATE_DISCONNECTED = 2;

        }

    }

    public static class Favorite {
        public static final String COLUMN_ID = FavoriteMsgData.COLUMN_ID;
        public static final String COLUMN_DATE = FavoriteMsgData.COLUMN_DATE;
        public static final String COLUMN_CONTACT_NUB = FavoriteMsgData.COLUMN_DA_CONTACT;
        public static final String COLUMN_BODY = FavoriteMsgData.COLUMN_DA_BODY;
        public static final String COLUMN_PATH = FavoriteMsgData.COLUMN_DA_FILENAME;
        public static final String COLUMN_CHATID = FavoriteMsgData.COLUMN_CHATID;
        public static final String COLUMN_SIZE = FavoriteMsgData.COLUMN_DA_FILESIZE;
        public static final String COLUMN_DA_ID = FavoriteMsgData.COLUMN_DA_ID;
        public static final String COLUMN_DA_CONTACT = FavoriteMsgData.COLUMN_DA_CONTACT;
        public static final String COLUMN_DA_TIMESTAMP = FavoriteMsgData.COLUMN_DA_TIMESTAMP;
        public static final String COLUMN_DA_MIME_TYPE = FavoriteMsgData.COLUMN_DA_MIME_TYPE;
        public static final String COLUMN_DA_MESSAGE_STATUS =
                FavoriteMsgData.COLUMN_DA_MESSAGE_STATUS;
        public static final String COLUMN_DA_DIRECTION = FavoriteMsgData.COLUMN_DA_DIRECTION;
        public static final String COLUMN_DA_TYPE = FavoriteMsgData.COLUMN_DA_TYPE;
        public static final String COLUMN_DA_FLAG = FavoriteMsgData.COLUMN_DA_FLAG;
        public static final String COLUMN_DA_ICON = FavoriteMsgData.COLUMN_DA_FILEICON;

        public static class Type {
            public static final int SMS = 1;
            public static final int MMS = 2;
            public static final int IM = 3;
            public static final int FT = 4;
            public static final int XML = 5;
        }

        public static class Flag {
            public static final int OTO = 1;
            public static final int OTM = 2;
            public static final int MTM = 3;
            public static final int PUBLIC = 4;
        }

    }

    public static class MmsXml {
        public static final String ROOT = "mms";
        public static final String RECORD = "record";
        public static final String ID = "_id";
        public static final String ISREAD = "isread";
        public static final String MSGBOX = "msg_box";
        public static final String DATE = "date";
        public static final String SIZE = "m_size";
        public static final String SIMID = "sim_id";
        public static final String ISLOCKED = "islocked";
    }

    public static class Vmsg {
        public static final String UTF = "UTF-8";
        public static final String QUOTED = "QUOTED-PRINTABLE";
        public static final String CHARSET = "CHARSET=";
        public static final String ENCODING = "ENCODING=";
        public static final String VMESSAGE_END_OF_SEMICOLON = ";";
        public static final String VMESSAGE_END_OF_COLON = ":";
        public static final String VMESSAGE_END_OF_LINE = "\r\n";
        public static final String BEGIN_VMSG = "BEGIN:VMSG";
        public static final String END_VMSG = "END:VMSG";
        public static final String VERSION = "VERSION:";
        public static final String BEGIN_VCARD = "BEGIN:VCARD";
        public static final String END_VCARD = "END:VCARD";
        public static final String BEGIN_VBODY = "BEGIN:VBODY";
        public static final String END_VBODY = "END:VBODY";
        public static final String FROMTEL = "TEL:";
        public static final String XBOX = "X-BOX:";
        public static final String XREAD = "X-READ:";
        public static final String XSEEN = "X-SEEN:";
        public static final String XSIMID = "X-SIMID:";
        public static final String XLOCKED = "X-LOCKED:";
        public static final String XTYPE = "X-TYPE:";
        public static final String DATE = "Date:";
        public static final String SUBJECT = "Subject;";
    }

    public class FileTransferType {
        public static final String ONE_TO_ONE = "1-1";
        public static final String ONE_TO_MANY = "1-many";
        public static final String GROUP = "Ad-Hoc";
    }

    public static class MessageID {
        public static final int BACKUP_END = 0x10;
        public static final int RESTORE_END = 0x11;
        public static final int CANCEL_END = 0x12;
        public static final int PRESS_BACK = 0X501;
    }
}
