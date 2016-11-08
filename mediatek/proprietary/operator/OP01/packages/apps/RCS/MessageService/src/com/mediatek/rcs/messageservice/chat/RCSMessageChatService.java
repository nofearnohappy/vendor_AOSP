package com.mediatek.rcs.messageservice.chat;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.mediatek.rcs.messageservice.utils.Logger;

public class RCSMessageChatService extends Service {
    public static final String TAG = "RCSMessageChatService";

    public static final String RCS_SYNC_GROUP_CHATS = "com.mediatek.rcs.messageservice.SYNC_CHATS";
    // add to manifest as a action.
    public static final String RCS_SYNC_GROUP_CHATS_DONE =
            "com.mediatek.rcs.message.SYNC_CHATS_DONE";

    private static RCSChatServiceBinder sServiceBinder = null;

    public RCSMessageChatService() {
        super();
    }

    @Override
    public void onCreate() {
        Logger.d(TAG, "onCreate");
        if (sServiceBinder == null) {
            sServiceBinder = new RCSChatServiceBinder(this);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.d(TAG, "onStartCommand: " + intent.getAction() +
                "flags: " + flags +
                "startId: " + startId);
        if (flags == Service.START_FLAG_REDELIVERY || flags == Service.START_FLAG_RETRY) {
            return Service.START_STICKY;
        }
        if (intent.getAction().equals(RCS_SYNC_GROUP_CHATS)) {
            // stack will create a thread for async.
            sServiceBinder.syncAllGroupChats();
        }
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        Logger.d(TAG, "onDestroy");
        sServiceBinder.onDestroy();
        sServiceBinder = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Logger.d(TAG, "onBind: " + intent.getAction());
        return sServiceBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Logger.d(TAG, "onUnbind: " + intent.getAction());
        return true;
    }
}
