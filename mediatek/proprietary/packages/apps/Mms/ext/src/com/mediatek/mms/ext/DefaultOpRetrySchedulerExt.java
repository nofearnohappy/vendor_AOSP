package com.mediatek.mms.ext;

import android.content.Context;
import android.content.ContextWrapper;

import com.mediatek.mms.callback.IDefaultRetrySchemeCallback;

public class DefaultOpRetrySchedulerExt extends ContextWrapper implements
        IOpRetrySchedulerExt {

    public DefaultOpRetrySchedulerExt(Context base) {
        super(base);
    }

    @Override
    public void scheduleRetry(IDefaultRetrySchemeCallback defaultRetryScheme,
            int msgType) {

    }

    @Override
    public boolean noNeedSendMsgToToastHandler() {
        return false;
    }

}
