package com.mediatek.rcs.common.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.gsma.joyn.chat.ChatLog;
import org.gsma.joyn.chat.ConferenceEventData.ConferenceUser;
import org.gsma.joyn.chat.GroupChat;
import org.gsma.joyn.ft.FileTransfer;
import org.gsma.joyn.ft.FileTransferLog;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony.Threads;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

import android.provider.ContactsContract.PhoneLookup;

import com.mediatek.rcs.common.IpMessageConsts;
import com.mediatek.rcs.common.RcsLog;
import com.mediatek.rcs.common.RcsLog.Direction;
import com.mediatek.rcs.common.RcsLog.MessageColumn;
import com.mediatek.rcs.common.RcsLog.MessageStatus;
import com.mediatek.rcs.common.RcsLog.MessageType;
import com.mediatek.rcs.common.RcsLog.Class;
import com.mediatek.rcs.common.RcsLog.ThreadFlag;
import com.mediatek.rcs.common.RcsLog.ThreadsColumn;
import com.mediatek.rcs.common.binder.RCSServiceManager;
import com.mediatek.rcs.common.provider.GroupChatCache.ChatInfo;
import com.mediatek.rcs.common.service.GroupParticipant;
import com.mediatek.rcs.common.service.Participant;
import com.mediatek.rcs.common.utils.ContextCacher;
import com.mediatek.rcs.common.utils.Logger;
import com.mediatek.rcs.common.utils.RCSUtils;

public class RCSDataBaseUtils {
    private static final String TAG = "RCSDataBaseUtils";

    private static final Uri RCS_URI_MESSAGE = ChatLog.Message.CONTENT_URI;
    private static final Uri RCS_URI_GROUP_CHAT = ChatLog.GroupChat.CONTENT_URI;
    private static final Uri RCS_URI_FT = FileTransferLog.CONTENT_URI;

    public static final Uri MMS_SMS_URI_THREAD_SETTINGS =
            Uri.parse("content://mms-sms/thread_settings/");
    public static final Uri URI_THREADS_UPDATE_STATUS =
            Uri.parse("content://mms-sms/conversations/status");

    private static final String KEY_EVENT_ROW_ID = "_id";
    private static final String KEY_EVENT_FT_ID = "ft_id";

    private static final int ERROR_IPMSG_ID = 0;

    private static final String[] PROJECTION_ONLY_ID = { KEY_EVENT_ROW_ID };

    /***********************************add for refectory start***********************************/
    public static final Uri URI_RCS_MESSAGE = RcsLog.MessageColumn.CONTENT_URI;
    public static final Uri URI_RCS_THREADS = RcsLog.ThreadsColumn.CONTENT_URI;

    public static final String[] PROJECTION_MESSAGE = {
            MessageColumn.ID,
            MessageColumn.CONTACT_NUMBER,
            MessageColumn.IPMSG_ID,
            MessageColumn.MESSAGE_ID,
            MessageColumn.CLASS,
            MessageColumn.TYPE,
            MessageColumn.CONVERSATION,
            MessageColumn.CHAT_ID,
            MessageColumn.FLAG,
            MessageColumn.FILE_PATH,
            MessageColumn.TIMESTAMP,
            MessageColumn.MIME_TYPE,
            MessageColumn.DIRECTION,
            MessageColumn.MESSAGE_STATUS,
            MessageColumn.DATE_SENT,
            MessageColumn.BODY,
            MessageColumn.SUB_ID};

    /**
     * get message accroding given messageId
     * @param context
     * @param messageId _id in table rcs_message
     * @return
     */
    public static Cursor getMessage(Context context, long messageId) {
        String selection = MessageColumn.ID + "=" + messageId;
        return context.getContentResolver().query(URI_RCS_MESSAGE, PROJECTION_MESSAGE,
                selection, null, null);
    }

    /**
     * update message status according messageId
     * @param context
     * @param messageId _id in table rcs_message
     * @param status
     * @return
     */
    public static int updateMessageStatus(Context context, long messageId, int status) {
        Logger.d(TAG, "updateMessageStatus, msgId = " + messageId + " to " +
                   translateStatusToString(status));
        Uri uri = ContentUris.withAppendedId(URI_RCS_MESSAGE, messageId);
        ContentValues values = new ContentValues();
        values.put(MessageColumn.MESSAGE_STATUS, status);
        return context.getContentResolver().update(uri, values, null, null);
    }

    /**
     * update message status according messageId
     * @param context
     * @param messageId unique String ID for each message
     * @param status
     * @return
     */
    public static int updateMessageStatus(Context context, String messageId, int status) {
        Logger.d(TAG, "updateMessageStatus, string msgId = " + messageId + " to " +
                translateStatusToString(status));
        ContentValues values = new ContentValues();
        int direction = Direction.OUTGOING;
        if (status == MessageStatus.READ || status == MessageStatus.UNREAD) {
            direction = Direction.INCOMING;
        }
        values.put(MessageColumn.MESSAGE_STATUS, status);
        if (status == MessageStatus.SENT) {
            values.put(MessageColumn.DATE_SENT, System.currentTimeMillis());
        }
        String where = MessageColumn.MESSAGE_ID + "='" + messageId + "'" +
                " AND " + MessageColumn.DIRECTION + "=" + direction;
        return context.getContentResolver().update(URI_RCS_MESSAGE, values, where, null);
    }

    /**
     * combine stack and message database, using String ID and ipMsgId
     * @param context
     * @param msgId
     * @param stackMsgId
     * @return
     */
    public static int combineMsgId(Context context, long msgId, String stackMsgId, int type) {
        Logger.d(TAG, "combineMsgId, msgId= " + msgId + ", type is " + translateTypeToString(type));
        ContentValues cv = new ContentValues();
        long ipMsgId = 0;
        if (type == MessageType.IM) {
            ipMsgId = getStackMessageId(context, stackMsgId, ChatLog.Message.Direction.OUTGOING);
        } else if (type == MessageType.FT) {
            ipMsgId = getStackFTMessageId(context, stackMsgId, FileTransfer.Direction.OUTGOING);
            cv.put(MessageColumn.BODY, stackMsgId);
        }
        Uri uri = ContentUris.withAppendedId(URI_RCS_MESSAGE, msgId);
        cv.put(MessageColumn.IPMSG_ID, ipMsgId);
        cv.put(MessageColumn.MESSAGE_ID, stackMsgId);
        return context.getContentResolver().update(uri, cv, null, null);
    }

    /**
     * get _id in rcs_message table according given String msgId
     * @param context
     * @param msgId
     * @param direction
     * @return
     */
    public static int getRcsMessageId(Context context, String msgId, int direction) {
        String selection = MessageColumn.MESSAGE_ID + "='" + msgId + "'" +
                " AND " + MessageColumn.DIRECTION + "=" + direction;
        Cursor cursor = context.getContentResolver().query(URI_RCS_MESSAGE,
                PROJECTION_MESSAGE, selection, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(cursor.getColumnIndex(MessageColumn.ID));
            } else {
                return 0;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static int getMessageStatus(Context context, long msgId) {
        Uri uri = ContentUris.withAppendedId(URI_RCS_MESSAGE, msgId);
        Cursor cursor = context.getContentResolver().query(uri,
                PROJECTION_MESSAGE, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(cursor.getColumnIndex(MessageColumn.MESSAGE_STATUS));
            } else {
                return -1;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
    /**
     * get ipmsgId in stack message db according given string msgId
     * @param context
     * @param msgId
     * @param direction
     * @return
     */
    public static long getStackMessageId(Context context, String msgId, int direction) {
        if (TextUtils.isEmpty(msgId)) {
            Logger.e(TAG, "getStackMessageId(), invalid msgId: " + msgId);
            return ERROR_IPMSG_ID;
        }
        Uri uri = null;
        String where = null;
        uri = RCS_URI_MESSAGE;
        where = ChatLog.Message.MESSAGE_ID + "='" + msgId + "'" + " AND " +
                ChatLog.Message.DIRECTION + "=" + direction;
        Cursor cursor = context.getContentResolver().query(uri, PROJECTION_ONLY_ID, where, null,
                null);
        try {
            if (null != cursor && cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndex(KEY_EVENT_ROW_ID));
            }
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }
        return ERROR_IPMSG_ID;
    }

    public static String getStackMessageBody(Context context, String msgId) {
        String result = null;
        Cursor cursor = context.getContentResolver().query(RCS_URI_MESSAGE, new String[] {
                ChatLog.Message.MESSAGE_ID, ChatLog.Message.BODY }, "("
                + ChatLog.Message.MESSAGE_ID + "='" + msgId + "')", null,
                ChatLog.GroupChat.TIMESTAMP + " DESC");
        if (cursor.moveToFirst()) {
            byte[] blobText = null;
            blobText = cursor.getBlob(1);
            if (blobText != null) {
                String status = new String(blobText);
                result = status;
            }
        }
        cursor.close();
        return result;
    }

    public static long getConversationId(Context context, String chatId,
            Set<String> contacts, boolean invite) {
        long conversationId = 0;
        if (!TextUtils.isEmpty(chatId)) {
            String[] projection = {MessageColumn.ID};
            String selection = ThreadsColumn.RECIPIENTS + "='" + chatId + "' AND " +
                    ThreadsColumn.FLAG + "=" + ThreadFlag.MTM;
            Cursor cursor = context.getContentResolver().query(URI_RCS_THREADS, projection,
                    selection, null, null);
            if (cursor == null) {
                return 0;
            }
            try {
                if (cursor.moveToFirst()) {
                    conversationId = cursor.getLong(0);
                } else {
                    int status = invite ? IpMessageConsts.GroupActionList.GROUP_STATUS_INVITING :
                        RCSUtils.getRCSSubId();
                    conversationId = createGroupThread(context, chatId, contacts, status);
                }
            } finally {
                cursor.close();
            }
        } else {
            conversationId = Threads.getOrCreateThreadId(context, contacts);
        }
        return conversationId;
    }

    public static Cursor getConversation(Context context, long threadId) {
        String[] projection = {ThreadsColumn.FLAG, ThreadsColumn.ID, ThreadsColumn.RECIPIENTS};
        String selection = ThreadsColumn.ID + "=" + threadId;
        return context.getContentResolver().query(URI_RCS_THREADS, projection,
                selection, null, null);
    }

    public static int deleteMessage(Context context, long msgId) {
        Uri uri = ContentUris.withAppendedId(URI_RCS_MESSAGE, msgId);
        return context.getContentResolver().delete(uri, null, null);
    }

    public static int deleteBurnMessage(Context context, String messageId, int direction) {
        String where = MessageColumn.MESSAGE_ID + "='" + messageId + "' " +
                " AND " + MessageColumn.CLASS + "=" + Class.BURN +
                " AND " + MessageColumn.DIRECTION + "=" + direction;
        return context.getContentResolver().delete(RCS_URI_MESSAGE, where, null);
    }

    public static long getFTMessageId(ContentResolver resolver, long ipmsgId) {

        String selection = MessageColumn.TYPE + "=" + MessageType.FT +
                " AND " + MessageColumn.IPMSG_ID + "=" + ipmsgId;
        Cursor cursor = resolver.query(URI_RCS_MESSAGE,
                PROJECTION_MESSAGE, selection, null, null);
        try {
            if (cursor.moveToFirst()) {
                long mmsDbId = cursor.getLong(cursor.getColumnIndex(MessageColumn.ID));
                return mmsDbId;
            } else {
                return 0;
            }
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }
    }

    public static int getFTMessageStatus(Context context, long ipmsgId) {
        Logger.d(TAG, "getFTMessageId() entry, ipMsgId: " + ipmsgId);

        String selection = MessageColumn.TYPE + "=" + MessageType.FT +
                " AND " + MessageColumn.IPMSG_ID + "=" + ipmsgId;
        Cursor cursor = context.getContentResolver().query(URI_RCS_MESSAGE,
                PROJECTION_MESSAGE, selection, null, null);
        try {
            if (cursor.moveToFirst()) {
                int status = cursor.getInt(cursor.getColumnIndex(MessageColumn.MESSAGE_STATUS));
                Logger.d(TAG, "getFTMessageId() mmsDbId: " + status);
                return status;
            } else {
                Logger.d(TAG, "getFTMessageId() empty cursor");
                return 0;
            }
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }
    }

    public static int updateFTMsgFilePath(Context context, String filePath, long ipMsgId) {
        ContentValues cv = new ContentValues();
        cv.put(MessageColumn.FILE_PATH, filePath);
        String selection = MessageColumn.TYPE + "=" + MessageType.FT +
                " AND " + MessageColumn.IPMSG_ID + "=" + ipMsgId;
        return context.getContentResolver().update(URI_RCS_MESSAGE, cv, selection, null);
    }

    public static long getStackFTMessageId(Context context, String ftId, int direction) {
        String selection = FileTransferLog.FT_ID + "='" + ftId + "' AND " +
                FileTransferLog.DIRECTION + "=" + direction;
        String[] projection = {FileTransferLog.ID};
        Cursor cursor = context.getContentResolver().query(RCS_URI_FT, projection,
                selection, null, null);
        if (cursor == null) {
            return 0;
        }
        try {
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            } else {
                return 0;
            }
        } finally {
            cursor.close();
        }
    }

    public static String getContactDisplayName(Context context, String number) {
        Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number));
        Cursor cursor = context.getContentResolver().query(uri,
                new String[]{PhoneLookup._ID, PhoneLookup.DISPLAY_NAME},
                null, null, null);
        String name = number;
        try {
            if (cursor != null && cursor.moveToFirst()) {
                name = cursor.getString(cursor.getColumnIndex(PhoneLookup.DISPLAY_NAME));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Logger.d(TAG, "contact name=" + name + " for number " + number);
        return name;
    }

    public static String getGroupSubject(Context ctx, String chatId) {
        Logger.i(TAG, "getGroupSubject(): chatId = " + chatId);
        String selection = ChatLog.GroupChat.CHAT_ID + "='" + chatId + "'";
        String subject = null;
        Cursor cursor = ctx.getContentResolver().query(RCSUtils.RCS_URI_GROUP_CHAT,
                RCSUtils.PROJECTION_GROUP_INFO, selection, null, null);
        try {
            if (cursor.moveToFirst()) {
                subject = cursor.getString(cursor.getColumnIndex(ChatLog.GroupChat.SUBJECT));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Logger.i(TAG, "getGroupSubject(): subject = " + subject);
        return subject;
    }

    public static final void addRejoinId(Context ctx, String chatId, String rejoinId) {
        ContentResolver resolver = ctx.getContentResolver();
        String[] projection = {GroupChatData.KEY_ID
                             , GroupChatData.KEY_CHAT_ID
                             , GroupChatData.KEY_REJOIN_ID};
        String selection = GroupChatData.KEY_CHAT_ID + "='" + chatId +"'";
        Cursor cursor = resolver.query(GroupChatData.CONTENT_URI, projection,
                selection, null, null);
        ContentValues values = new ContentValues();
        values.put(GroupChatData.KEY_REJOIN_ID, rejoinId);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                resolver.update(GroupChatData.CONTENT_URI, values, selection, null);
            } else if (cursor != null) {
                values.put(GroupChatData.KEY_CHAT_ID, chatId);
                resolver.insert(GroupChatData.CONTENT_URI, values);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static List<Participant> getGroupParticipants(Context ctx, String chatId) {
        Logger.i(TAG, "getGroupParticipants(): chatId = " + chatId);
        List<Participant> participants = new ArrayList<Participant>();
        String memberSelection = GroupMemberData.COLUMN_CHAT_ID + "='" + chatId + "'";
        Cursor cursor = ctx.getContentResolver().query(RCSUtils.RCS_URI_GROUP_MEMBER,
                RCSUtils.PROJECTION_GROUP_MEMBER, memberSelection, null, null);
        try {
            while (cursor.moveToNext()) {
                Participant participant = new Participant(cursor.getString(cursor
                        .getColumnIndex(GroupMemberData.COLUMN_CONTACT_NUMBER)),
                        cursor.getString(cursor
                                .getColumnIndex(GroupMemberData.COLUMN_CONTACT_NAME)));
                participant.setState(cursor.getInt(cursor.getColumnIndex(
                        GroupMemberData.COLUMN_STATE)));
                participants.add(participant);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Logger.i(TAG, "getGroupParticipants(): recipients size = " + participants.size());
        return participants;
    }

    public static int modifyGroupNickName(Context context, String chatId, String nickName) {
        Logger.d(TAG, "modifyGroupNickName, nickName=" + nickName);
        ContentValues values = new ContentValues();
        values.put(ChatLog.GroupChat.NICKNAME, nickName);
        String where = ChatLog.GroupChat.CHAT_ID + "='" + chatId + "'";
        int count = context.getContentResolver().update(RCS_URI_GROUP_CHAT, values, where, null);
        Logger.d(TAG, "modifyGroupNickName, affectRow=" + count);
        return count;
    }

    public static Set<String> getGroupAvailableParticipants(Context ctx, String chatId) {
        Logger.i(TAG, "getGroupParticipants(): chatId = " + chatId);
        Set<String> participants = new HashSet<String>();
        String memberSelection = GroupMemberData.COLUMN_CHAT_ID + "='" + chatId + "'" +
                " AND " + GroupMemberData.COLUMN_STATE + "<>" + GroupMemberData.STATE.STATE_PENDING;
        Cursor cursor = ctx.getContentResolver().query(RCSUtils.RCS_URI_GROUP_MEMBER,
                RCSUtils.PROJECTION_GROUP_MEMBER, memberSelection, null, null);
        try {
            while (cursor.moveToNext()) {
                participants.add(cursor.getString(cursor
                        .getColumnIndex(GroupMemberData.COLUMN_CONTACT_NUMBER)));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Logger.i(TAG, "getGroupParticipants(): recipients size = " + participants.size());
        return participants;
    }

    /**
     * get thread id for a group chat accroding to given chat id
     * @param context
     * @param chatId
     * @return
     */
    public static long findThreadIdForGroup(Context context, String chatId) {
        String[] columns = {ThreadsColumn.ID, ThreadsColumn.RECIPIENTS};
        String where = ThreadsColumn.RECIPIENTS + "='" + chatId + "' AND " +
                ThreadsColumn.FLAG + "=" + ThreadFlag.MTM;
        Cursor cursor = context.getContentResolver().query(ThreadsColumn.CONTENT_URI,
                columns, where, null, null);
        long threadId = 0;
        try {
            if (cursor != null && cursor.moveToFirst()) {
                threadId = cursor.getLong(cursor.getColumnIndex(ThreadsColumn.ID));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return threadId;
    }

    private static String translateStatusToString(int status) {
        switch (status) {
            case MessageStatus.READ:
                return "read";
            case MessageStatus.UNREAD:
                return "unread";
            case MessageStatus.SENDING:
                return "sending";
            case MessageStatus.SENT:
                return "sent";
            case MessageStatus.FAILED:
                return "failed";
            case MessageStatus.DELIVERED:
                return "delivered";
            case MessageStatus.TO_SEND:
                return "to_send";
            default:
                return "unknow";
        }
    }

    private static String translateTypeToString(int type) {
        switch (type) {
            case MessageType.IM:
                return "IM";
            case MessageType.FT:
                return "FT";
            case MessageType.SMSMMS:
                return "smsmms";
            case MessageType.XML:
                return "XML";
            default:
                return "unknow";
        }
    }
    /***********************************add for refectory end*************************************/

    @Deprecated
    public static String findMsgIdInRcsDb(Context ctx, long ipMsgId) {
        Logger.d(TAG, "findMsgIdInRcsDb() entry, ipMsgId: " + ipMsgId);

        long ftIpMsgId = ipMsgId;
        Cursor cursor = ctx.getContentResolver().query(RCS_URI_FT,
                RCSUtils.PROJECTION_FILE_TRANSFER,
                KEY_EVENT_ROW_ID + "=" + ftIpMsgId, null, null);
        try {
            if (null != cursor && cursor.moveToFirst()) {
                String msgId = cursor.getString(cursor.getColumnIndex(KEY_EVENT_FT_ID));
                Logger.d(TAG, "findMsgIdInRcsDb()msgId: " + msgId);
                return msgId;
            } else {
                Logger.w(TAG, "findIdInRcseDb() invalid cursor: " + cursor);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    public static long createGroupThread(
            Context ctx, String chatId, Set<String> participants, int status) {
        Logger.d(TAG, "createGroupThread, status= " + status);
        Uri.Builder builder = RCSUtils.MMS_SMS_URI_ADD_THREAD.buildUpon();
        builder.appendQueryParameter("chatId", chatId);
        for (String participant : participants) {
            Logger.d(TAG, "createGroupThread, participant number = " + participant);
            builder.appendQueryParameter("recipient", participant);
        }
        ContentValues values = new ContentValues();
        values.put(Threads.STATUS, status);
        Uri result = ctx.getContentResolver().insert(builder.build(), values);
        Logger.d(TAG, "createGroupThread, uri=" + result);
        return Long.parseLong(result.getLastPathSegment());
    }

    /**
     * Create group thread id by chatId. Must ensure the thread id is exist.
     *
     * @param chatId group chat id
     * @return real thread id
     */
    public static long createGroupThreadByChatId(Context ctx, String chatId) {
        Logger.d(TAG, "[ensureGroupThreadId],chatId=" + chatId);
        Set<String> participants = getGroupAvailableParticipants(ctx, chatId);
        return getConversationId(ctx, chatId, participants, false);
    }

    /**
     * createGroupThreadId for backupAndRestore
     * @param chatId
     * @param rejoinId
     * @param participants
     * @param subject
     * @param chairmen
     * @param timestamp
     * @param state
     * @return
     */
    /// M: for backup restore {@
    public static long createGroupThreadId(
                                           Context ctx
                                         , String chatId
                                         , String rejoinId
                                         , List<Participant> participants
                                         , String subject
                                         , String chairmen
                                         , long timestamp
                                         , int state) {
        Set<String> contacts = new HashSet<String>();
        for (Participant participant : participants) {
            contacts.add(participant.getContact());
        }
        long threadId = createGroupThread(ctx, chatId, contacts, state);
        String myNumber = RCSServiceManager.getInstance().getMyNumber();
        boolean isMeChairmen = PhoneNumberUtils.compare(myNumber, chairmen);
        ChatInfo info = GroupChatCache.getInstance().getInfoByChatId(chatId);
        if (info == null) {
            GroupChatCache.getInstance().addChatInfo(chatId, subject, state, isMeChairmen);
        } else {
            GroupChatCache.getInstance().updateSubjectByChatId(chatId, subject);
            GroupChatCache.getInstance().updateStatusByChatId(chatId, state);
            GroupChatCache.getInstance().updateIsMeChairmenByChatId(chatId, isMeChairmen);
        }
        addRejoinId(ctx, chatId, rejoinId);
        return threadId;
    }
     /// end }@

     //for group conference notify start
     public static GroupParticipant getSingleGroupParticipant(
             Context ctx, String chatId, String contact) {
         GroupParticipant groupParticipant = null;
         String[] projection = { GroupMemberData.COLUMN_CONTACT_NUMBER,
                 GroupMemberData.COLUMN_CONTACT_NAME, GroupMemberData.COLUMN_STATE, };
         String selection = GroupMemberData.COLUMN_CHAT_ID + "='" + chatId + "' AND "
                 + GroupMemberData.COLUMN_CONTACT_NUMBER + "='" + contact + "'";
        Cursor cursor = ctx.getContentResolver().query(
                GroupMemberData.CONTENT_URI,
                projection,
                selection,
                null,
                null);
         if (cursor == null)
             return null;
         while (cursor.moveToNext()) {
             String displayName = cursor.getString(cursor
                     .getColumnIndex(GroupMemberData.COLUMN_CONTACT_NAME));
             int state = cursor.getInt(cursor.getColumnIndex(GroupMemberData.COLUMN_STATE));
             Logger.v(TAG, "getGroupParticipant from DB #contact: " + contact + ",#displayName:"
                     + displayName + ",#state:" + state);
             groupParticipant = new GroupParticipant(new Participant(contact, displayName),
                     transferMemberStatusToString(state));
         }
         cursor.close();
         return groupParticipant;
     }

     public static void addGroupParticipant(
             Context ctx, String chatId, String contact, String displayName, String status) {
         Logger.v(TAG, "addGroupParticipant To DB #chatId: " + chatId + ",#contact:" + contact
                 + ",#status:" + status);
         ContentValues values = new ContentValues();
         values.put(GroupMemberData.COLUMN_CHAT_ID, chatId);
         if (contact == null){
             contact = "+8601234567890";
         }
         values.put(GroupMemberData.COLUMN_CONTACT_NUMBER, contact);

         if (displayName != null) {
             values.put(GroupMemberData.COLUMN_CONTACT_NAME, displayName);
         }

         if (status != null) {
             int state = transferMemberStatusToInt(status);
             values.put(GroupMemberData.COLUMN_STATE, state);
         }
         ctx.getContentResolver().insert(GroupMemberData.CONTENT_URI, values);
     }

     public static void updateGroupParticipant(
             Context ctx, String chatId, String contact, String displayName, String status) {
         Logger.v(TAG, "updateGroupParticipant To DB #chatId: " + chatId + ",#contact:"
                 + contact + ",#status:" + status);
         ContentValues values = new ContentValues();

         if (status != null) {
             int state = transferMemberStatusToInt(status);
             values.put(GroupMemberData.COLUMN_STATE, state);
         }
         if (displayName != null) {
             values.put(GroupMemberData.COLUMN_CONTACT_NAME, displayName);
         }
         String where = GroupMemberData.COLUMN_CHAT_ID + "='" + chatId + "' AND "
                 + GroupMemberData.COLUMN_CONTACT_NUMBER + "='" + contact + "'";
         ctx.getContentResolver().update(GroupMemberData.CONTENT_URI, values, where, null);
     }

     public static String transferMemberStatusToString(int status) {
         String result = null;
         switch (status) {
         case GroupMemberData.STATE.STATE_CONNECTED:
             result = ConferenceUser.Status.CONNECTED;
             break;
         case GroupMemberData.STATE.STATE_DISCONNECTED:
             result = ConferenceUser.Status.DISCONNECTED;
             break;
         case GroupMemberData.STATE.STATE_PENDING:
             result = ConferenceUser.Status.PENDING;
             break;
         default:
             break;
         }
         return result;
     }

     public static int transferMemberStatusToInt(String status) {
         int result = -1;
         if (status.equalsIgnoreCase(ConferenceUser.Status.CONNECTED))
             result = GroupMemberData.STATE.STATE_CONNECTED;
         else if (status.equalsIgnoreCase(ConferenceUser.Status.DISCONNECTED))
             result = GroupMemberData.STATE.STATE_DISCONNECTED;
         else if (status.equalsIgnoreCase(ConferenceUser.Status.PENDING))
             result = GroupMemberData.STATE.STATE_PENDING;
         return result;
     }

     public static void removeGroupParticipant(Context ctx, String chatId, String contact) {
        int deletedRows = ctx.getContentResolver().delete(
                GroupMemberData.CONTENT_URI,
                GroupMemberData.COLUMN_CHAT_ID + " = '" +
                        chatId + "' AND " + GroupMemberData.COLUMN_CONTACT_NUMBER + " = '"
                        + contact + "'",
                null);
         Logger.v(TAG, "removeGroupParticipant from DB #chatId: " + chatId + ",#contact:"
                 + contact + ",#deletedRows:" + deletedRows);
     }
     //for group conference notify end
}
