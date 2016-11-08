package com.mediatek.mms.plugin;

import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;

import com.mediatek.mms.callback.IConversationCallback;
import com.mediatek.mms.ext.DefaultOpMmsWidgetServiceExt;

/**
 * Op01MmsWidgetServiceExt.
 *
 */
public class Op01MmsWidgetServiceExt extends DefaultOpMmsWidgetServiceExt {

    /**
     * Op01MmsWidgetServiceExt.
     * @param context Context
     */
    public Op01MmsWidgetServiceExt(Context context) {
        super(context);
    }

    @Override
    public boolean getViewAt(Intent intent, boolean dirMode, IConversationCallback conversation) {
        if (!dirMode) {
            return false;
        }
        intent.setAction(ACTION_FOLDER_MODE);
        if (conversation.hasUnreadMessagesCallback()) {
            intent.putExtra(EXTRA_KEY_FOLDER_TYPE, FOLDER_HAS_UNREAD);
            intent.putExtra(EXTRA_KEY_THREAD_ID, conversation.getThreadIdCallback());
        } else if (conversation.hasDraftCallback()) {
            intent.putExtra(EXTRA_KEY_FOLDER_TYPE, FOLDER_HAS_DRAFT);
        } else if (conversation.hasErrorCallback()) {
            intent.putExtra(EXTRA_KEY_FOLDER_TYPE, FOLDER_HAS_ERROR);
        } else if (Telephony.Threads.WAPPUSH_THREAD == conversation.getTypeCallback() ||
                Telephony.Threads.CELL_BROADCAST_THREAD == conversation.getTypeCallback()) {
            intent.putExtra(EXTRA_KEY_FOLDER_TYPE, FOLDER_CB_OR_PUSH);
        } else {
            intent.putExtra(EXTRA_KEY_FOLDER_TYPE, FOLDER_NORMAL);
            intent.putExtra(EXTRA_KEY_THREAD_ID, conversation.getThreadIdCallback());
        }
        return true;
    }

    @Override
    public boolean getViewMoreConversationsView(Intent intent, boolean dirMode) {
        if (!dirMode) {
            return false;
        }
        intent.setAction(ACTION_FOLDER_MODE);
        intent.putExtra(EXTRA_KEY_FOLDER_TYPE, MORE_MESSAGES);
        return true;
    }
}
