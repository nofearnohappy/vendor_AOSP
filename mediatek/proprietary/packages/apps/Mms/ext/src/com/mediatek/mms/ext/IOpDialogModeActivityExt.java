package com.mediatek.mms.ext;

import com.mediatek.mms.callback.IDialogModeActivityCallback;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public interface IOpDialogModeActivityExt {
    /**
     * @internal
     */
    boolean openThread();

    /**
     * @internal
     */
    boolean simSelection(int selectedSubId, int subCount, String number,
            int messageSubId, Intent intent, long currentSubId, int[] subIdList,
            IDialogModeActivityCallback callback);

    /**
     * @internal
     */
    String getNotificationContentString(String from, String subject, String msgSizeTxt,
            String expireTxt);

    /**
     * @internal
     */
    void setDialogView(Context applicationContext, int subId, boolean isCurSMS, int type,
            String sentTime, String receivedTime, ImageView iv);

    /**
     * @internal
     */
    public void updateCounter(TextView counter, int textLineCount, int remainingInCurrentMessage,
            int msgCount, String counterText);

    /**
     * @internal
     */
    void onReceive(Context context, Intent intent);

    /**
     * @internal
     */
    void updateSendButtonState(boolean enable);

    /**
     * @internal
     */
    void setHost(IDialogModeActivityCallback host);

    /**
     * @internal
     */
    public void initDialogView(TextView subName, Button download, TextView recvTime,
            TextView sentTime, LinearLayout timeLayout, LinearLayout counterLinearLayout,
            TextView counter, Cursor cursor);

    /**
     * @internal
     */
    boolean onResume();
}
