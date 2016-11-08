package com.mediatek.rcs.common.provider;

import android.net.Uri;

public class GroupChatData {

    /**
     * Content provider URI for chat messages
     */
    public static final Uri CONTENT_URI = Uri.parse("content://rcs-groupchat");

    public static final String AUTHORITY = "rcs-groupchat";
    /**
     * The name of the column containing the  ID.
     * <P>Type: TEXT</P>
     */
    public static final String KEY_ID = "_id";

    /**
     * The name of the column containing the thread ID.
     * <P>Type: INTEGER</P>
     */
    @Deprecated
    public static final String KEY_THREAD_ID = "thread_id";

    /**
     * The name of the column containing the chat ID.
     * <P>Type: TEXT</P>
     */
    public static final String KEY_CHAT_ID = "chat_id";

    /**
     * The name of the column containing the subject.
     * <P>Type: TEXT</P>
     */
    public static final String KEY_SUBJECT = "subject";

    /**
     * The name of the column containing the nickname.
     * <P>Type: TEXT</P>
     */
    public static final String KEY_NICKNAME = "nickname";

    /**
     * The name of the column containing the status.
     * <P>Type: INTEGER</P>
     */
    public static final String KEY_STATUS = "status";

    /**
     * The name of column containing the isChairmen.
     * <P>Type: INTEGER</P>
     */
    public static final String KEY_ISCHAIRMEN = "chairmen";

    /**
     * The name of column containing the subId.
     * <P>Type: INTEGER</P>
     */
    public static final String KEY_SUB_ID = "sub_id";

    /**
     * The name of column containing the rejoinId.
     * <P>Type: TEXT</P>
     */
    public static final String KEY_REJOIN_ID = "rejoin_id";
}