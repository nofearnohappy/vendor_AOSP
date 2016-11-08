package com.mediatek.mms.plugin;

import android.content.Context;

import com.mediatek.mms.ext.DefaultOpSmsSingleRecipientSenderExt;

public class Op03SmsSingleRecipientSenderExt extends
        DefaultOpSmsSingleRecipientSenderExt {

    @Override
    public int sendMessagePrepare(Context context, int codingType) {
        return Op03MessageUtils.getSmsEncodingType(context);
    }

}
