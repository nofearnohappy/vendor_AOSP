package com.mediatek.mms.folder.util;

import com.android.mms.R;
import com.android.mms.MmsApp;
import com.mediatek.mms.folder.ui.FolderViewList;
import com.mediatek.mms.folder.ui.FolderModeSmsViewer;
import com.android.mms.ui.MessageUtils;
import com.android.mms.ui.SearchActivity;
import com.mediatek.mms.callback.IMmsWidgetProxyActivityCallback;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;

public class FolderModeUtils {
    private static final String TAG = "FolderModeUtils";
    public static final String ACTION_FOLDER_MODE = "com android.mms.widget.ACTION_FOLDER_MODE";
    public static final String ACTION_CONVERSATION_MODE
            = "com android.mms.widget.ACTION_CONVERSATION_MODE";
    public static final String EXTRA_KEY_FOLDER_TYPE = "folder_type";
    public static final String EXTRA_KEY_CONVSATION_TYPE = "conversation_type";
    public static final String EXTRA_KEY_THREAD_ID = "thread_id";

    public static final int FOLDER_HAS_UNREAD = 1;
    public static final int FOLDER_CB_OR_PUSH = 2;
    public static final int FOLDER_HAS_DRAFT  = 3;
    public static final int FOLDER_HAS_ERROR  = 4;
    public static final int FOLDER_NORMAL     = 5;

    public static void setMmsDirMode(boolean mode) {
        SharedPreferences sp =
            PreferenceManager.getDefaultSharedPreferences(MmsApp.getApplication());
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("CmccMmsUiMode", mode);
        editor.commit();
    }

    public static boolean getMmsDirMode() {
        SharedPreferences sp =
            PreferenceManager.getDefaultSharedPreferences(MmsApp.getApplication());
        boolean dirMode = sp.getBoolean("CmccMmsUiMode", false);
        return dirMode;
    }

    public static void setSimCardInfo(int simcard) {
        SharedPreferences sp =
            PreferenceManager.getDefaultSharedPreferences(MmsApp.getApplication());
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("CmccSimCardInfo", simcard);
        editor.commit();
    }

    public static int getSimCardInfo() {
        SharedPreferences sp =
            PreferenceManager.getDefaultSharedPreferences(MmsApp.getApplication());
        int siminfo = sp.getInt("CmccSimCardInfo", 0);
        return siminfo;
    }

    public static boolean startFolderViewList(boolean folderMode,
            Context context, Activity activity, int flags, int box) {
        if (!folderMode || !getMmsDirMode()) {
            return false;
        }

        Intent intent = new Intent(context, FolderViewList.class);
        intent.putExtra("floderview_key", box); // show inbox by default
        if (flags != 0) {
            intent.setFlags(flags);
        }
        if (activity != null) {
            activity.finish();
        }
        context.startActivity(intent);

        return true;
    }

    public static boolean startFolderViewList(boolean folderMode,
            Context context, Activity activity, int flags) {
        return startFolderViewList(folderMode, context,
                activity, flags, FolderViewList.OPTION_INBOX);
    }

    public static void setFolderModeSmsViewerIntent(
            Context context, Intent intent, Uri uri, int msgType) {
        intent.setClass(context,FolderModeSmsViewer.class);
        intent.setData(uri);
        intent.putExtra("msg_type", msgType);
    }

    public static void startActivityForFolder(Context context, Intent intent,
            IMmsWidgetProxyActivityCallback callback) {
        Intent ic = new Intent(context, FolderViewList.class);
        boolean shouldStartActivity = true;
        int boxType = FolderViewList.OPTION_INBOX;
        switch (intent.getIntExtra(EXTRA_KEY_FOLDER_TYPE, 0)) {

        case FOLDER_HAS_UNREAD:
            Log.d(TAG, "folder mode -- has unread");
            long threadId = intent.getLongExtra("thread_id", 0);
            if (threadId > 0) {
                shouldStartActivity = false;
                callback.getUnreadInfoCallback(threadId);
            }
            break;

        case FOLDER_HAS_DRAFT:
            Log.d(TAG, "folder mode -- has draft message");
            boxType = FolderViewList.OPTION_DRAFTBOX;
            break;

        case FOLDER_HAS_ERROR:
            Log.d(TAG, "folder mode -- has error");
            boxType = FolderViewList.OPTION_OUTBOX;
            break;

        case FOLDER_NORMAL:
            Log.d(TAG, "folder mode -- normal message");
            threadId = intent.getLongExtra("thread_id", 0);
            if (threadId > 0) {
                shouldStartActivity = false;
                callback.getThreadInfoCallback(threadId);
            }
            break;

        default:
            Log.d(TAG, "folder mode -- CB or PUSH or default");
            break;
        }

        ic.putExtra(FolderViewList.FOLDERVIEW_KEY, boxType);
        Log.d(TAG, "folder mode -- boxType = " + boxType);
        if (shouldStartActivity) {
            try {
                ic.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                context.startActivity(ic);
            } catch (Exception e) {
                Log.d(TAG, "Failed to start intent activity", e);
            }
        }
    }
}