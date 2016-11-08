package com.mediatek.rcs.message.plugin;

import java.lang.reflect.Array;
import java.util.List;

import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.provider.Telephony.Sms.Conversations;
import android.provider.Telephony.Threads;
import android.provider.Telephony.ThreadSettings;
import android.util.Log;

import com.android.mms.data.Conversation;
import com.android.mms.transaction.MessagingNotification;

import com.mediatek.mms.callback.IConversationCallback;
import com.mediatek.mms.ipmessage.DefaultIpConversationExt;

import com.mediatek.rcs.common.IpMessageConsts;
import com.mediatek.rcs.common.RcsLog;
import com.mediatek.rcs.common.RcsLog.MessageColumn;
import com.mediatek.rcs.common.RcsLog.MessageStatus;
import com.mediatek.rcs.common.RcsLog.ThreadsColumn;
import com.mediatek.rcs.common.RcsLog.MessageType;
import com.mediatek.rcs.common.provider.RCSDataBaseUtils;
import com.mediatek.rcs.common.provider.GroupChatCache;
import com.mediatek.rcs.common.provider.GroupChatCache.ChatInfo;
import com.mediatek.rcs.message.utils.RcsMessageUtils;
import com.mediatek.rcs.message.utils.ThreadNumberCache;

/**
 * class RcsConversation, plugin implements response Conversation.java.
 *
 */
public class RcsConversation extends DefaultIpConversationExt {
    private static final String TAG = "RcsConversation";

    /// M: use this instead of the host default to query more columns for rcs
    static String[] sRCSThreadsProjections;
    /// M: use this uri instead of the host one to query more columns from rcs_threads
    static final Uri sRCSAllThreadsUri = ThreadsColumn.CONTENT_URI;

    public static final String[] PROJECTION_NEW_GROUP_INVITATION = {
        Threads._ID,
        Threads.DATE,
        Threads.READ,
        Telephony.Threads.STATUS
    };
    public static final Uri URI_CONVERSATION =
            Uri.parse("content://mms-sms/conversations").buildUpon()
                    .appendQueryParameter("simple", "true").build();

    public static final String SELECTION_NEW_GROUP_INVITATION_BY_STATUS =
            "(" + Telephony.Threads.STATUS +
            " = " + IpMessageConsts.GroupActionList.GROUP_STATUS_INVITING
            + " OR " + Telephony.Threads.STATUS +
            " = " + IpMessageConsts.GroupActionList.GROUP_STATUS_INVITING_AGAIN
            + " OR " + Telephony.Threads.STATUS +
            " = " + IpMessageConsts.GroupActionList.GROUP_STATUS_INVITE_EXPAIRED
            + ")";
    public static final String SELECTION_NEW_GROUP_INVITATION_BY_THREAD = "threads._id = ?";

    private static final Uri URI_CONVERSATION_LOCKED = Uri.parse("content://mms-sms-rcs/locked");
    public static final int TYPE_GROUP = 110;
    private static final int RCS_COLUMN_BASE = Conversation.ALL_THREADS_PROJECTION_EXTEND.length -1;
    public static final int MSG_CLASS =                  RCS_COLUMN_BASE; //
    public static final int CHAT_RECIPIENTS =           RCS_COLUMN_BASE + 1;
    public static final int CHAT_TYPE =                 RCS_COLUMN_BASE + 2; //O2O, O2M, Group,
    public static final int CHAT_MESSAGE_TYPE =         RCS_COLUMN_BASE + 3; //SMSMMS, IM, FT, XML
    public static final int FT_MESSAGE_MIME_TYPE =      RCS_COLUMN_BASE + 4;
    public static final int STICKY =                    RCS_COLUMN_BASE + 5;
    /*The projections extends for rcs, need combine origin projections together*/
    private static final String[] RCS_THREADS_PROJECTION_EXTENDS_ONLY = {
        ThreadsColumn.CLASS,
        ThreadsColumn.RECIPIENTS,
        ThreadsColumn.FLAG,
        ThreadsColumn.MESSAGE_TYPE,
        ThreadsColumn.MIME_TYPE,
        Telephony.ThreadSettings.TOP
    };

    private static final String[] RCSMSG_UNREAD_PROJECTION = {
        MessageColumn.CONVERSATION,
        MessageColumn.MESSAGE_STATUS
    };
    private static final String RCSMSG_UNREAD_SELECTION =
                "(" + MessageColumn.MESSAGE_STATUS + "=" +MessageStatus.UNREAD
                +" OR " + MessageColumn.SEEN + "=0)"
                + "AND " + MessageColumn.CONVERSATION + "=?";
    private static final int UPDATE_LIMIT = 50;
    private static final String UNREAD_SELECTION_WITH_LIMIT =
            "(" + MessageColumn.MESSAGE_STATUS + "=" +MessageStatus.UNREAD
            +" OR" + MessageColumn.SEEN + "=0)"
            + "and _id in (select _id from "
                + "(select _id from rcs_message where "
                    + "(" + MessageColumn.MESSAGE_STATUS + "=" +MessageStatus.UNREAD
                    + " OR " + MessageColumn.SEEN + "=0) and "  + MessageColumn.CONVERSATION + "=?"
                    + " order by _id limit 0," + UPDATE_LIMIT + ")";
    private static ContentValues sReadContentValues;
    private long mThreadId = 0;
    private String mNumber;
    private boolean mIsSticky;
    private int mChatType; // O2O : 0; O2M: 1; Group: 2
    boolean mIsGroup;
    String mGroupChatId;
    private int mLastMsgType;
    private String mFtMimeType;
    private int mLastMsgCatogery;

    IConversationCallback mConversationCallback;

    public static void init() {
        sRCSThreadsProjections = RcsMessageUtils.combineTwoStringArrays(
                Conversation.ALL_THREADS_PROJECTION_EXTEND, RCS_COLUMN_BASE,
                RCS_THREADS_PROJECTION_EXTENDS_ONLY, RCS_THREADS_PROJECTION_EXTENDS_ONLY.length);
    }

    /**
     *  Override DefaultIpConversationExt's onIpFillFromCursor. Host class is ConversationList.
     * @param context Context
     * @param c cursor
     * @param recipientSize recipientsize
     * @param number number
     * @param type type
     * @param date date
     * @return int
     */
    public int onIpFillFromCursor(Context context, Cursor c, int recipientSize, String number,
            int type, long date) {
        mThreadId = c.getLong(0);
        Log.d(TAG, "onIpFillFromCursor, threadId = " + mThreadId);
        Log.d(TAG, "onIpFillFromCursor, stick Time = " + c.getLong(STICKY));
        mIsSticky = (c.getLong(STICKY) != 0);
        if (RcsConversationList.mStickyThreadsSet != null) {
            if (mIsSticky) {
                RcsConversationList.mStickyThreadsSet.add(mThreadId);
            } else {
                RcsConversationList.mStickyThreadsSet.remove(mThreadId);
            }
        }
        mLastMsgType = c.getInt(MSG_CLASS);
        mChatType = c.getInt(CHAT_TYPE);
        if (mChatType == RcsLog.ThreadFlag.MTM) {
            //group
            mGroupChatId = c.getString(CHAT_RECIPIENTS);
            mIsGroup = true;
        }
        mLastMsgCatogery = c.getInt(CHAT_MESSAGE_TYPE);
        mFtMimeType = c.getString(FT_MESSAGE_MIME_TYPE);
        Log.d(TAG, "mChatType = " + mChatType + ", mGroupChatId = " + mGroupChatId);
        if (mIsGroup) {
            return TYPE_GROUP;
        }
        return type;
    }

    public long getThreadId() {
        return mThreadId;
    }

    public void setThreadId(long threadId) {
        mThreadId = threadId;
    }

    public int getLastMsgType() {
        return mLastMsgType;
    }

    public int getLastMsgCatogery() {
        return mLastMsgCatogery;
    }

    public String getFtMimeType() {
        return mFtMimeType;
    }

    public boolean isSticky() {
        return mIsSticky;
    }

    /**
     * Override DefaultIpConversationExt's guaranteeIpThreadId.
     * @param threadId
     * @return real threadId, if threadId is group chat, return real threadid; else return 0.
     */
    public long guaranteeIpThreadId(long threadId) {
        if (!mIsGroup) {
            return 0;
        }
        if (mThreadId <= 0) {
            mThreadId = RCSDataBaseUtils.createGroupThreadByChatId(mGroupChatId);
        }
        Log.d(TAG, "[guaranteeIpThreadId] mThreadId = " + mThreadId);
        return mThreadId;
    }

    /**
     * Override DefaultIpConversationExt's onIpInit.
     * @param callback use this to callback to Conversation in mms host.
     */
    public void onIpInit(IConversationCallback callback) {
        mConversationCallback = callback;
    }

    /**
     * Get this conversation's contact list from mms host.
     * @return list of IpContact.
     */
//    public List<IpContact> getIpContactList() {
//        return mConversationCallback.getIpContactList();
//    }

    /**
     * Get this conversation's message count from mms host.
     * @return message count.
     */
    public int getMessageCount() {
        return mConversationCallback.getMessageCount();
    }

    /**
     * Set Group chat ID. Use this it can update threadid in guaranteeIpThreadId.
     * @param chatId Group chat's chat id.
     */
    public void setGroupChatId(String chatId) {
        if (chatId == null) {
            return;
        }
        mIsGroup = true;
        mGroupChatId = chatId;
        ChatInfo info = GroupChatCache.getInstance().getInfoByChatId(chatId);
//        mThreadId = info.getThreadId();
    }

    /**
     * Returns a content:// URI referring to this conversation,
     * or null if it does not exist on disk yet.
     */
    public Uri getUri() {
        if (mThreadId <= 0)
            return null;
        return getConversationUri(mThreadId);
    }

    static Uri getConversationUri(long threadId) {
        return ContentUris.withAppendedId(sRCSAllThreadsUri, threadId);
    }

    private void buildReadContentValues() {
        if (sReadContentValues == null) {
            sReadContentValues = new ContentValues(2);
            sReadContentValues.put(MessageColumn.SEEN, 1);
            sReadContentValues.put(MessageColumn.MESSAGE_STATUS, MessageStatus.READ);
        }
    }

    @Override
    public boolean markAsRead(final Context context, final int needUpdateSmsMmsCount) {
     // If we have no Uri to mark (as in the case of a conversation that
        // has not yet made its way to disk), there's nothing to do.
        Log.d(TAG, "markAsRead(): needUpdateCount = " + needUpdateSmsMmsCount);
        Uri uri = MessageColumn.CONTENT_URI;
        Uri threadUriOfUnreadCount =
                    getUri().buildUpon().appendQueryParameter("read", "true").build();
            int rcsUnreadCount = 0;
            Cursor c = context.getContentResolver().query(uri,
                    RCSMSG_UNREAD_PROJECTION, RCSMSG_UNREAD_SELECTION,
                    new String[]{Long.toString(mThreadId)}, null);
            if (c != null) {
                try {
                    rcsUnreadCount = c.getCount();
                    Log.d(TAG, "markAsRead(): rcsUnreadCount= " + rcsUnreadCount);
                } finally {
                    c.close();
                }
            }
            /// M: Code analyze 060, For bug ALPS00272115, add for WappushNotification canceled
            /// with other message . @{
            if (rcsUnreadCount > 0) {
                Log.d(TAG, "markAsRead: update read/seen for thread uri: " + uri);
                final int allNeedUpdateCount = rcsUnreadCount;
                int updateCount = UPDATE_LIMIT;
                buildReadContentValues();
                if (allNeedUpdateCount >= UPDATE_LIMIT) {
                    while (updateCount > 0) {
                        updateCount = context.getContentResolver().update(
                                uri, sReadContentValues,
                                UNREAD_SELECTION_WITH_LIMIT,
                                new String[] {mThreadId + "", mThreadId + "", mThreadId + ""});
                        Log.d(TAG, "markAsRead-updateThread: updateCount=" + updateCount);
                        synchronized (this) {
                            try {
                                this.wait(UPDATE_LIMIT * 4);
                            } catch (InterruptedException ex) {
                                Log.d(TAG, "InterruptedException");
                            }
                        }
                    }
                } else {
                    updateCount = context.getContentResolver().update(uri, sReadContentValues,
                            RCSMSG_UNREAD_SELECTION, new String[]{Long.toString(mThreadId)});
                    Log.d(TAG, "markAsRead-updateThread: updateCount=" + updateCount);
                }
                if (needUpdateSmsMmsCount <= 0) {
                    MessagingNotification.blockingUpdateAllNotifications(context);
                }
            }
        return false;
    }


    /**
     * Start a query for in the database on the specified AsyncQueryHandler with the specified
     * "where" clause.
     *
     * @param handler An AsyncQueryHandler that will receive onQueryComplete
     *                upon completion of the query
     * @param token   The token that will be passed to onQueryComplete
     * @param selection   A where clause (can be null) to select particular conv items.
     */
    public static boolean startQuery(AsyncQueryHandler handler, String[] projection,
            int token, String selection) {
//        handler.startQuery(token, null, sRCSAllThreadsUri, sRCSThreadsProjections,
//                selection, null, Conversations.DEFAULT_SORT_ORDER);
        if (selection == null || !selection.equals("allunread")) {
            handler.startQuery(token, null, sRCSAllThreadsUri,sRCSThreadsProjections, selection,
                    null, Conversations.DEFAULT_SORT_ORDER);
            return true;
        }
        return false;
    }

    public static boolean startQueryHaveLockedMessages(AsyncQueryHandler handler, int token,
            Object cookie, Uri uri, String[] projection, String selection, String[] selectionArgs) {
        uri = URI_CONVERSATION_LOCKED;
        handler.startQuery(token, cookie, uri, projection, selection, selectionArgs, null);
        return true;
    }

    @Override
    public Cursor loadFromThreadId(Context context, Uri uri, String[] projection, long threadId) {
        uri = getConversationUri(threadId);
        Cursor cursor = context.getContentResolver().query(sRCSAllThreadsUri,
                                    sRCSThreadsProjections,
                                    "rcs_threads._id=" + threadId, null, null);
        return cursor;
    }
}
