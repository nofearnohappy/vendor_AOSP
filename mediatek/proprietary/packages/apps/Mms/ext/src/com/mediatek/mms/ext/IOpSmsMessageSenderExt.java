package com.mediatek.mms.ext;

import android.content.ContentResolver;
import android.net.Uri;

public interface IOpSmsMessageSenderExt {
    /**
     * @internal
     */
    Uri queueMessage(int numberOfDests, ContentResolver resolver, String address, String body,
            Long date, boolean deliveryReport, long threadId, long subId, long ipmsgId);
}
