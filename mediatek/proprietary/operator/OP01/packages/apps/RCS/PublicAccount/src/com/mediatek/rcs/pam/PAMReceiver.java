package com.mediatek.rcs.pam;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.gsma.joyn.JoynService;

public class PAMReceiver extends BroadcastReceiver {
    private static final String TAG = Constants.TAG_PREFIX + "PAMReceiver";

    public static final String ACTION_NEW_MESSAGE = "org.gsma.joyn.chat.action.NEW_CHAT";
    public static final String ACTION_RCS_ACCOUNT_CHANGED =
            "com.mediatek.rcs.contacts.INTENT_RCS_LOGIN";
    public static final String ACTION_JOYN_UP = JoynService.ACTION_RCS_SERVICE_UP;

    public static final String ICC_ID = "iccId";

    public PAMReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        Log.d(TAG, "Recieved: " + action);
        if (ACTION_NEW_MESSAGE.equals(action)) {
            intent.setClass(context, PAServiceImpl.class);
            context.startService(intent);
        } else if (ACTION_RCS_ACCOUNT_CHANGED.equals(action)) {
            intent.setClass(context, PAServiceImpl.class);
            context.startService(intent);
        } else if (ACTION_JOYN_UP.equals(action)) {
            intent.setClass(context, PAServiceImpl.class);
            context.startService(intent);
        }
    }
}
