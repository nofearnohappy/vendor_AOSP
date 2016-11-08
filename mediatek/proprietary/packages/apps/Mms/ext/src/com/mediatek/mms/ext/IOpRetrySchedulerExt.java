package com.mediatek.mms.ext;

import com.mediatek.mms.callback.IDefaultRetrySchemeCallback;

public interface IOpRetrySchedulerExt {
    /**
     * @internal
     */
    void scheduleRetry(IDefaultRetrySchemeCallback defaultRetryScheme, int msgType);
    /**
     * @internal
     */
    boolean noNeedSendMsgToToastHandler();
}
