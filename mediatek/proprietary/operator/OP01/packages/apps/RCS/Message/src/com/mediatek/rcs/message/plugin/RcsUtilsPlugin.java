package com.mediatek.rcs.message.plugin;

import android.app.ActivityManager;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.provider.Telephony.Sms;
import android.text.TextUtils;
import android.util.Log;


import com.android.mms.MmsApp;
import com.mediatek.mms.callback.IUtilsCallback;
import com.mediatek.mms.ipmessage.DefaultIpUtilsExt;
import com.mediatek.mms.ipmessage.IIpMessageItemExt;

import com.mediatek.rcs.common.binder.RCSServiceManager;
import com.mediatek.rcs.common.IpMessageConsts;
import com.mediatek.rcs.common.RcsLog;
import com.mediatek.rcs.common.provider.GroupChatCache;
import com.mediatek.rcs.common.utils.ContextCacher;
import com.mediatek.rcs.common.utils.EmojiShop;
import com.mediatek.rcs.common.utils.Logger;
import com.mediatek.rcs.common.GroupManager;
import com.mediatek.rcs.common.RcsLog.MessageColumn;
import com.mediatek.rcs.common.RCSMessageManager;

import com.mediatek.rcs.message.data.RcsProfile;
import com.mediatek.rcs.message.group.PortraitManager;
import com.mediatek.rcs.message.utils.NewMessageReceiver;
import com.mediatek.rcs.message.utils.RcsMessageUtils;
import com.mediatek.rcs.message.R;
import com.mediatek.rcs.message.SendModeChangedNotifyService;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * RcsUtilsPlugin. extends DefaultIpUtilsExt.
 *
 */
public class RcsUtilsPlugin extends DefaultIpUtilsExt {

    public static final String TAG = "RcsUtilsPlugin";
    private static RcsUtilsPlugin sInstance;
    private Context mPluginContext;
    static Context sHostContext;

    /**
     * RcsUtilsPlugin Construction.
     * @param context Context
     */
    public RcsUtilsPlugin(Context context) {
        mPluginContext = context;
    }

    private static IUtilsCallback sCallback;

    @Override
    public boolean initIpUtils(IUtilsCallback ipUtilsCallback) {
        sCallback = ipUtilsCallback;
        return true;
    }

    @Override
    public boolean onIpBootCompleted(Context context) {
        Log.d(TAG, "onIpBootCompleted()");
        SendModeChangedNotifyService.resetWhenReBoot(mPluginContext);
        setSendingRcsMessageFailed(context);
        return false;
    }

    @Override
    public boolean onIpMmsCreate(final Context context) {
        ContextCacher.setHostContext(context);
        sHostContext = context;
        Logger.d(TAG, "onIpMmsCreate. start");
        RcsConversation.init();
        String processName = getCurProcessName(context);
        Logger.d(TAG, "createManager, processName=" + processName);
        if (processName != null && processName.equalsIgnoreCase("com.android.mms")) {
            GroupManager.createInstance(context);
            RCSMessageManager.createInstance(context);
            RCSServiceManager.createManager(context);
            NewMessageReceiver.init(context);
            RcsProfile.init(context);
            new Thread(new Runnable() {

                public void run() {
                    Logger.d(TAG, "RCSCreateChatsThread start run");
                    GroupChatCache.createInstance(ContextCacher.getHostContext());
                }
            }, "RCSCreateChatsThread").start();
            PortraitManager.init(mPluginContext);
            EmojiShop.init(context.getApplicationContext());
        }
        return false;
    }

    @Override
    public CharSequence formatIpMessage(CharSequence inputChars, boolean showImg,
            CharSequence inputBuf) {
        Log.d(TAG, "formatIpMessage(): inputChars = " + inputChars);
        if (TextUtils.isEmpty(inputChars)) {
            return super.formatIpMessage(inputChars, showImg, inputBuf);
        }

        EmojiImpl emoji = EmojiImpl.getInstance(ContextCacher.getPluginContext());
        if (inputBuf == null) {
            return emoji.getEmojiExpression(inputChars, showImg);
        } else {
            String bufStr = inputBuf.toString();
            String inputStr = inputChars.toString();
            int start = bufStr.indexOf(inputStr);
            if (start == -1) {
                return inputBuf;
            }
            return emoji.getEmojiExpression(inputBuf, showImg);
        }
    }

    @Override
    public String getIpTextMessageType(IIpMessageItemExt item) {
        if (item == null) {
            return null;
        }
        RcsMessageItem rcsItem = (RcsMessageItem) item;
        if (rcsItem.mType.equals("rcs")) {
            return mPluginContext.getString(R.string.rcs_message_type);
        }
        return null;
    }

    @Override
    public long getKey(String type, long msgId) {
        return RcsMessageListAdapter.getKey(type, msgId);
    }

    /**
     * Get Notification Resource Id from host.
     * @return Notification resource id.
     */
    public static int getNotificationResourceId() {
        if (sCallback != null) {
            return sCallback.getNotificationResourceId();
        } else {
            return 0;
        }
    }

    /**
     * Unblocking notify new message. Called when receive one ipmessage.
     * @param context Context
     * @param newMsgThreadId Thread id belong to.
     * @param newSmsId The id in sms table.
     * @param ipMessageId the id in stack table.
     */
    public static void unblockingNotifyNewIpMessage(Context context, long newMsgThreadId,
            long newSmsId, long ipMessageId) {
        Log.d(TAG, "unblockingNotifyNewIpMessage: id = " + newSmsId);
        unblockingIpUpdateNewMessageIndicator(context, newMsgThreadId, false, null);
        if (isIpPopupNotificationEnable(context, newMsgThreadId)) {
            notifyNewIpMessageDialog(context, newSmsId, ipMessageId);
        }
        notifyIpWidgetDatasetChanged(context);
    }

    private static void unblockingIpUpdateNewMessageIndicator(final Context context,
            final long newMsgThreadId, final boolean isStatusMessage,
            final Uri statusMessageUri) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                sCallback.blockingIpUpdateNewMessageIndicator(context, newMsgThreadId,
                        isStatusMessage, statusMessageUri);
            }
        }, "MessagingNotification.nonBlockingUpdateNewMessageIndicator").start();
    }

    private static void blockingIpUpdateNewMessageIndicator(Context context,
            long newMsgThreadId, boolean isStatusMessage, Uri statusMessageUri) {
        sCallback.blockingIpUpdateNewMessageIndicator(context, newMsgThreadId,
                isStatusMessage, statusMessageUri);
    }

    private static boolean isIpPopupNotificationEnable(Context context, long threadId) {
        // get setting value in mms setting for dialogmode
        boolean isPopUpEnable = sCallback.isIpPopupNotificationEnable();
        Log.d(TAG, "[isIpPopupNotificationEnable]: thread is =  " + threadId
                + "in mms setting is: " + isPopUpEnable);
        String chatId = RcsMessageUtils.blockingGetGroupChatIdByThread(context, threadId);
        if (isPopUpEnable && !TextUtils.isEmpty(chatId)) {
            // get group setting whether forbid dialogmode(receive msg but not notify)
            Uri threadSettingsUri = ContentUris.withAppendedId(
                            Uri.parse("content://mms-sms/thread_settings/"), (int) threadId);
            Cursor c = context.getContentResolver().query(threadSettingsUri,
                    new String[] {Telephony.ThreadSettings.NOTIFICATION_ENABLE}, null, null, null);
            if (c != null) {
                try {
                    if (c.getCount() > 0) {
                        c.moveToFirst();
                        isPopUpEnable = c.getInt(0) == 0 ? false : true;
                        Log.d(TAG, "[isIpPopupNotificationEnable]: in this group setting is: "
                        + isPopUpEnable);
                    }
                } finally {
                    c.close();
                }
            }
        }
        return isPopUpEnable;
    }

    private static void notifyIpWidgetDatasetChanged(Context context) {
        sCallback.notifyIpWidgetDatasetChanged(context);
    }

    private static void notifyNewIpMessageDialog(Context context, long rcsId, long ipmessageId) {
        Log.d(TAG, "notifyNewIpMessageDialog,id:" + rcsId);
        if (rcsId == 0) {
            return;
        }
        if (sCallback.isIpHome(context)) {
            Log.d(TAG, "at launcher");
            Intent intent = sCallback.getDialogModeIntent(context);
            Uri uri = ContentUris.withAppendedId(MessageColumn.CONTENT_URI, rcsId);
            intent.putExtra("com.android.mms.transaction.new_msg_uri", uri.toString());
            intent.putExtra("ipmessage", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            intent.putExtra(IpMessageConsts.MessageAction.KEY_IPMSG_ID, ipmessageId);
            context.startActivity(intent);
        } else {
            Log.d(TAG, "not at launcher");
        }
    }

    /**
     * Send mms. Will callback to host to send mms.
     * @param uri mms's uri
     * @param messageSize pdu's size
     * @param subId   which sub id to send
     * @param threadId the mms belong to
     * @return true if sent out, else return false.
     */
    public static boolean sendMms(Uri uri, long messageSize, int subId, long threadId) {
        if (sCallback != null) {
            return sCallback.sendMms(ContextCacher.getHostContext(), uri, messageSize, subId,
                    threadId);
        }
        return false;
    }

    /**
     * Get timeStamp format string. will callback to mms host.
     * @param when  long
     * @param fullFormat . if true, will have year, month, day and time
     * @return format string
     */
    public static String formatIpTimeStampString(long when, boolean fullFormat) {
        if (sCallback != null) {
            return sCallback.formatIpTimeStampString(ContextCacher.getHostContext(), when,
                    fullFormat);
        }
        return null;
    }

    private static String getCurProcessName(Context context) {
        int pid = android.os.Process.myPid();
        ActivityManager am = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo appProcess : am.getRunningAppProcesses()) {
            if (appProcess.pid == pid) {
                return appProcess.processName;
            }
        }
        return null;
    }

    @Override
    public boolean startQueryForConversation(AsyncQueryHandler handler, String[] projection,
            int token, String selection) {
        return RcsConversation.startQuery(handler, projection, token, selection);
    }


    @Override
    public boolean startQueryHaveLockedMessages(AsyncQueryHandler handler, int token, Object cookie,
            Uri uri, String[] projection, String selection, String[] selectionArgs) {
        return RcsConversation.startQueryHaveLockedMessages(handler, token, cookie, uri,
                        projection, selection, selectionArgs);
    }

    private static final String[] SEEN_PROJECTION = new String[] {
        "seen"
        };
    @Override
    public void blockingMarkAllIpMessageAsSeen(Context context) {
        ContentResolver resolver = context.getContentResolver();
        Uri uri = MessageColumn.CONTENT_URI;
        Cursor cursor = resolver.query(uri, SEEN_PROJECTION, "seen=0",  null, null);
        int count = 0;
        if (cursor != null) {
            try {
                count = cursor.getCount();
            } finally {
                cursor.close();
            }
        }
        if (count == 0) {
            return;
        }
        ContentValues values = new ContentValues(1);
        values.put("seen", 1);
        resolver.update(uri, values, "seen=0", null);
    }

    @Override
    public Uri startDeleteForConversation(Uri uri) {
        long now = System.currentTimeMillis();
        uri = uri.buildUpon().appendQueryParameter("date", Long.toString(now))
                .appendQueryParameter("deleteThread", "true")
                .appendQueryParameter("rcs", "true").build();
        return uri;
    }

    public static void setSendingRcsMessageFailed(Context context) {
        Uri uri = RcsLog.MessageColumn.CONTENT_URI;
        String[] projection = new String[] {RcsLog.MessageColumn.ID,
                                            RcsLog.MessageColumn.MESSAGE_STATUS};
        String selection = RcsLog.MessageColumn.MESSAGE_STATUS + "=" + RcsLog.MessageStatus.SENDING;
        ContentValues values = new ContentValues(1);
        values.put(RcsLog.MessageColumn.MESSAGE_STATUS, RcsLog.MessageStatus.FAILED);
        ContentResolver resolver = context.getContentResolver();
        int updateCount = resolver.update(uri, values, selection, null);
        Log.d(TAG, "setSendingRcsMessageFailed: update count = " + updateCount);
    }
}
