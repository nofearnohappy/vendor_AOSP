package com.mediatek.mms.ext;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.mediatek.mms.callback.IClassZeroActivityCallback;

public interface IOpClassZeroActivityExt {

    /**
     * @internal
     */
    public void handleMessage(Message msg, Handler handler, boolean read,
            IClassZeroActivityCallback callback, Context context);

    /**
     * @internal
     */
    public void displayZeroMessage(AlertDialog dialog);
}
