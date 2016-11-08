package com.mediatek.mms.ext;

import java.util.ArrayList;

import android.app.PendingIntent;
import android.content.Context;

import com.mediatek.mms.ext.IOpSmsSingleRecipientSenderExt;

public class DefaultOpSmsSingleRecipientSenderExt implements IOpSmsSingleRecipientSenderExt {

    @Override
    public boolean sendMessage(Context context, int subId, String dest, String serviceCenter,
            ArrayList<String> messages, ArrayList<PendingIntent> sentIntents,
            ArrayList<PendingIntent> deliveryIntents) {
        return false;
    }

    @Override
    public boolean sendOpMessage(Context context, int subId,
            ArrayList<PendingIntent> deliveryIntents) {
        return false;
    }

    @Override
    public int sendMessagePrepare(Context context, int codingType) {
        return codingType;
    }
}
