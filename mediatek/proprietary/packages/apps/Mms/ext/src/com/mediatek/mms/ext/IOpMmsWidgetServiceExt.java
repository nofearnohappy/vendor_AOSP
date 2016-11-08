package com.mediatek.mms.ext;

import com.mediatek.mms.callback.IConversationCallback;

import android.content.Intent;

public interface IOpMmsWidgetServiceExt {
    public static final String ACTION_FOLDER_MODE = "com android.mms.widget.ACTION_FOLDER_MODE";
    public static final String EXTRA_KEY_FOLDER_TYPE = "folder_type";
    public static final String EXTRA_KEY_CONVSATION_TYPE = "conversation_type";
    public static final String EXTRA_KEY_THREAD_ID = "thread_id";

    public static final int FOLDER_HAS_UNREAD = 1;
    public static final int FOLDER_CB_OR_PUSH = 2;
    public static final int FOLDER_HAS_DRAFT  = 3;
    public static final int FOLDER_HAS_ERROR  = 4;
    public static final int FOLDER_NORMAL     = 5;

    public static final int MORE_MESSAGES     = 600;

    /**
     * @internal
     */
    boolean getViewAt(Intent intent, boolean dirMode, IConversationCallback convCallback);

    /**
     * @internal
     */
    boolean getViewMoreConversationsView(Intent intent, boolean dirMode);
}
