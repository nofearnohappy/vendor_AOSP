package com.mediatek.mms.plugin;

import com.mediatek.mms.callback.IClassZeroActivityCallback;
import com.mediatek.mms.ext.DefaultOpClassZeroActivityExt;
import com.mediatek.mms.plugin.Op09MessagePluginExt;

import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.telephony.SmsMessage;

public class Op09ClassZeroActivityExt extends DefaultOpClassZeroActivityExt {

    private static final int ON_AUTO_SAVE = 1;

    private static final int SAVE_WITHOUT_DESTROY_ACTIVITY = 2;

    public static final long THREAD_ALL = -1;

    @Override
    public void handleMessage(Message msg, Handler handler, boolean read,
            IClassZeroActivityCallback callback, Context context) {
        if (msg.what == ON_AUTO_SAVE) {
        /// M: add for OP09. @{
        } else if (MessageUtils.isClassZeroModelShowLatestEnable()
                && msg.what == SAVE_WITHOUT_DESTROY_ACTIVITY) {
            handler.removeMessages(ON_AUTO_SAVE);
            saveMessage((SmsMessage[]) msg.obj, read, callback, context);
        } /// @}
    }

    @Override
    public void displayZeroMessage(AlertDialog dialog) {
        /// M: for OP09 @{
        if (dialog != null && MessageUtils.isClassZeroModelShowLatestEnable()) {
            dialog.dismiss();
        }
        /// @}
    }

    /// M: add for OP09. @{
    private void saveMessage(SmsMessage[] message, boolean read,
            IClassZeroActivityCallback callback, Context context) {
        Uri messageUri = null;
        if (message[0].isReplace()) {
            messageUri = callback.replaceMessage(message);
        } else {
            messageUri = callback.storeMessage(message);
        }
        if (!read && messageUri != null) {
            Op09MessagePluginExt.sCallback.nonBlockingUpdateNewMessageIndicatorCallback(context,
                    THREAD_ALL, false);
        }
        /// M: Code analyze 002, unknown. @{
        callback.cancelMessageNotification();
        Op09MessagePluginExt.sCallback.deleteRecyclerOldMessages(false);
        /// M: fix bug ALPS00379747, update mms widget after save class0 msg @{
        Op09MessagePluginExt.sCallback.notifyDatasetChangedCallback();
        /// @}
    }
    /// @}

}
