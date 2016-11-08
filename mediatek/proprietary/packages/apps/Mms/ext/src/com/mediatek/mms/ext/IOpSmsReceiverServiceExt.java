package com.mediatek.mms.ext;

import com.mediatek.mms.callback.ISmsReceiverServiceCallback;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.SmsMessage;

public interface IOpSmsReceiverServiceExt {
    /**
     * @internal
     */
    void onCreate(Service service, ISmsReceiverServiceCallback callback);
    /**
     * @internal
     */
    void onDestroy(Service service);

    /**
     * @internal
     */
    Uri handleSmsReceived(Context context, SmsMessage[] msgs, Intent intent, int error);

    /**
     * @internal
     */
    Intent displayClassZeroMessage(Intent intent);

    /**
     * @internal
     */
    boolean storeMessage(SmsMessage[] msgs, SmsMessage sms, ContentValues values);
}
