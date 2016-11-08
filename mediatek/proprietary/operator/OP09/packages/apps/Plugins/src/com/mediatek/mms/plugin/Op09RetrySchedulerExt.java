package com.mediatek.mms.plugin;

import android.content.Context;

import com.mediatek.mms.callback.IDefaultRetrySchemeCallback;
import com.mediatek.mms.ext.DefaultOpRetrySchedulerExt;

public class Op09RetrySchedulerExt extends DefaultOpRetrySchedulerExt {

    public Op09RetrySchedulerExt(Context context) {
        super(context);
    }

    @Override
    public void scheduleRetry(IDefaultRetrySchemeCallback defaultRetryScheme, int msgType) {
        defaultRetryScheme.setRetrySchemeCallback(
                Op09MmsConfigExt.getInstance().getMmsRetryScheme(msgType));
    }

    @Override
    public boolean noNeedSendMsgToToastHandler() {
        return true;
    }
}
