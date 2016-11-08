package com.mediatek.mms.plugin;

import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony.Threads;

import com.mediatek.mms.ext.DefaultOpConversationExt;

public class OP09ConversationExt extends DefaultOpConversationExt {
    private static final int ID = 0;
    private static final int DATE = 1;
    private static final int MESSAGE_COUNT = 2;
    private static final int READCOUNT = 10;

    public long mDateSent;
    public int mRecipSize;
    public boolean mHasDraft;
    public long mDate;
    public Uri mUri;
    public int mUnreadCount;

    private static final int THREAD_DATE_SENT = 17;

    public OP09ConversationExt(Context context) {
        super(context);
    }

    @Override
    public void fillFromCursor(Cursor cursor, int recipSize, boolean hasDraft) {
        mDateSent = cursor.getLong(THREAD_DATE_SENT);
        mRecipSize = recipSize;
        mHasDraft = hasDraft;
        mDate = cursor.getLong(DATE);
        mUri = getUri(cursor.getLong(ID));
        mUnreadCount = cursor.getInt(MESSAGE_COUNT) - cursor.getInt(READCOUNT);
        return;
    }

    public Uri getUri(long id) {
        if (id <= 0) {
            return null;
        }
        return ContentUris.withAppendedId(Threads.CONTENT_URI, id);
    }



    public static final String DEFAULT_SORT_ORDER = "date DESC";

    public static final Uri sAllUnreadMessagesUri = Uri.parse("content://mms-sms/unread_count");


    @Override
    public boolean startQuery(AsyncQueryHandler handler, int token, String selection) {
        if (selection != null && selection.equals("allunread")) {
            if (MessageUtils.isMassTextEnable()) {
                handler.startQuery(token, null,
                        Uri.parse("content://mms-sms/conversations/group/unread_count"), null,
                        selection, null, DEFAULT_SORT_ORDER);
                return true;
            }
        }
        return false;

    }
}
