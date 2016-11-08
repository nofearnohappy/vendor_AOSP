package com.mediatek.mms.ext;

import android.content.Intent;

public interface IOpSmsReceiverExt {
    /**
     * @internal
     */
    void onReceiveWithPrivilege(Intent intent, String action);
}
