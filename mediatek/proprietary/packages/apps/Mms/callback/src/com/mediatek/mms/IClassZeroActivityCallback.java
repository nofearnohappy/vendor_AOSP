
package com.mediatek.mms.callback;

import android.net.Uri;
import android.telephony.SmsMessage;

public interface IClassZeroActivityCallback {

    public Uri replaceMessage(SmsMessage[] Msgs);

    public Uri storeMessage(SmsMessage[] Msgs);

    public void cancelMessageNotification();
}
