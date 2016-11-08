package com.mediatek.mms.ext;

import android.content.ContentResolver;
import android.net.Uri;

public class DefaultOpSmsMessageSenderExt implements IOpSmsMessageSenderExt {

    @Override
    public Uri queueMessage(int numberOfDests, ContentResolver resolver, String address,
            String body, Long date, boolean deliveryReport, long threadId, long subId,
            long ipmsgId) {
        // TODO Auto-generated method stub
        return null;
    }

}
