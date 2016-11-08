package com.android.mms.widget;

import com.android.mms.data.Conversation;
import com.android.mms.ui.ComposeMessageActivity;
import com.android.mms.ui.ConversationList;
import com.mediatek.mms.folder.ui.FolderViewList;
import com.android.mms.util.MmsLog;
import com.mediatek.wappush.ui.WPMessageActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import android.provider.Telephony;
import com.mediatek.cb.cbmsg.CBMessageListActivity;

import com.mediatek.mms.callback.IMmsWidgetProxyActivityCallback;
import com.mediatek.mms.folder.util.FolderModeUtils;

public class MmsWidgetProxyActivity extends Activity implements IMmsWidgetProxyActivityCallback {
    private static final String TAG = "MmsWidgetProxyActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MmsLog.d(TAG, "onCreate is called");
        Intent intent = getIntent();
        Context context = getApplicationContext();
        MmsLog.d(TAG, "Action is " + intent.getAction());
        if (MmsWidgetService.ACTION_CONVERSATION_MODE.equals(intent.getAction())) {
            startActivityForConv(context, intent);
        } else if (FolderModeUtils.ACTION_FOLDER_MODE.equals(intent.getAction())) {
            FolderModeUtils.startActivityForFolder(context, intent, this);
        } else if (MmsWidgetProvider.ACTION_COMPOSE_NEW_MESSAGE.equals(intent.getAction())) {
            startActivityForNew(context);
        }
        finish();
    }

    private void startActivityForNew(Context context) {
        MmsLog.d(TAG, "compose new message");
        Intent ic = new Intent(context, ComposeMessageActivity.class);
        startActivityInternal(context, ic);
    }

    private void startActivityForConv(Context context, Intent intent) {
        Intent ic = null;
        int msgType = intent.getIntExtra(MmsWidgetService.EXTRA_KEY_CONVSATION_TYPE, 0);
        long threadId = intent.getLongExtra("thread_id", 0);
        MmsLog.d(TAG, "conversation mode -- msgType=" + msgType + "  thread_id=" + threadId);
        Conversation conv = Conversation.createNew(context);
        switch (msgType) {
        case Telephony.Threads.CELL_BROADCAST_THREAD:
            MmsLog.d(TAG, "conversation mode -- CB");
            ic = CBMessageListActivity.createIntent(context, threadId);
            break;
        case Telephony.Threads.WAPPUSH_THREAD:
            MmsLog.d(TAG, "conversation mode -- push");
            ic = WPMessageActivity.createIntent(context, threadId);
            break;
        case MmsWidgetService.MORE_MESSAGES:
            MmsLog.d(TAG, "conversation mode -- more message");
            ic = new Intent(context, ConversationList.class);
            break;
        default:
            MmsLog.d(TAG, "conversation mode -- normal message");
            ic = ComposeMessageActivity.createIntent(context, threadId);
            break;
        }
        startActivityInternal(context, ic);
    }

    private void getUnreadInfo(final Uri uri, final Context context) {
        MmsLog.d(TAG, "folder mode -- has unread");
        new Thread(new Runnable() {

            public void run() {
                Intent ic = new Intent(context, FolderViewList.class);
                Cursor cursor = context.getContentResolver().query(uri, null, " read=0 ", null, null);
                int boxType = FolderViewList.OPTION_INBOX;
                if (cursor != null) {
                    try {
                        while (cursor.moveToNext()) {
                            int msgBox = cursor.getInt(1);
                            if (msgBox == 1) {
                                boxType = FolderViewList.OPTION_INBOX;
                                break;
                            } else if (msgBox >= 4) {
                                boxType = FolderViewList.OPTION_OUTBOX;
                            }
                        }
                    } finally {
                        cursor.close();
                    }
                }
                ic.putExtra(FolderViewList.FOLDERVIEW_KEY, boxType);
                startActivityInternal(context, ic);
            }
        }).start();
    }

    private void getThreadInfo(final Uri uri, final Context context) {
        MmsLog.d(TAG, "getThreadInfo, uri = " + uri);
        new Thread(new Runnable() {

            public void run() {
                Intent it = new Intent(context, FolderViewList.class);
                Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
                int msgBox = 0;
                int boxType = FolderViewList.OPTION_INBOX;
                if (cursor != null && cursor.moveToFirst()) {
                    msgBox = cursor.getInt(1);
                    MmsLog.d(TAG, "getThreadInfo, thread msgBox = " + msgBox);
                    if (2 == msgBox) {
                        boxType = FolderViewList.OPTION_SENTBOX;
                    } else if (3 == msgBox) {
                        boxType = FolderViewList.OPTION_DRAFTBOX;
                    } else if (4 <= msgBox) {
                        boxType = FolderViewList.OPTION_OUTBOX;
                    }
                } else {
                    MmsLog.d(TAG, "getThreadInfo, cursor is null or cursor count is 0");
                }
                if (cursor != null) {
                    cursor.close();
                }
                it.putExtra(FolderViewList.FOLDERVIEW_KEY, boxType);
                startActivityInternal(context, it);
            }
        }).start();
    }

    private Uri getQueryUri(long threadId) {
        return Uri.parse("content://mms-sms/widget/thread/" + threadId);
    }

    private void startActivityInternal(Context context, Intent intent) {
        MmsLog.d(TAG, "startActivityInternal is called");
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            MmsLog.d(TAG, "Failed to start intent activity", e);
        }
    }

    public void getUnreadInfoCallback(long threadId) {
        getUnreadInfo(getQueryUri(threadId), this);
    }

    public void getThreadInfoCallback(long threadId) {
        getThreadInfo(getQueryUri(threadId), this);
    }
}
