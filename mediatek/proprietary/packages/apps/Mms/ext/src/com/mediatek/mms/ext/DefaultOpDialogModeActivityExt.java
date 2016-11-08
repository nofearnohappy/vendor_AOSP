package com.mediatek.mms.ext;

import com.mediatek.mms.callback.IDialogModeActivityCallback;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.database.Cursor;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DefaultOpDialogModeActivityExt extends ContextWrapper
        implements IOpDialogModeActivityExt {

    public DefaultOpDialogModeActivityExt(Context context) {
        super(context);
    }

    @Override
    public String getNotificationContentString(String from,
            String subject, String msgSizeTxt, String expireTxt) {
        return msgSizeTxt + "\n" + expireTxt;
    }

    @Override
    public boolean openThread() {
        return false;
    }

    public boolean simSelection(int selectedSubId, int subCount,
            String number, int messageSubId, Intent intent,
            long currentSubId, int[] subIdList,
            IDialogModeActivityCallback callback) {
        return false;
    }

    @Override
    public void updateCounter(TextView counter, int textLineCount, int remainingInCurrentMessage,
            int msgCount, String counterText) {
    }

    public void initDialogView(TextView subName, Button download, TextView recvTime,
            TextView sentTime, LinearLayout timeLayout, LinearLayout counterLinearLayout,
            TextView counter, Cursor cursor) {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
    }

    @Override
    public void updateSendButtonState(boolean enable) {
    }

    @Override
    public void setHost(IDialogModeActivityCallback host) {
    }

    public void setDialogView(Context applicationContext, int subId, boolean isCurSMS, int type,
            String sentTime, String receivedTime, ImageView iv) {
    }

    @Override
    public boolean onResume() {
        return false;
    }
}