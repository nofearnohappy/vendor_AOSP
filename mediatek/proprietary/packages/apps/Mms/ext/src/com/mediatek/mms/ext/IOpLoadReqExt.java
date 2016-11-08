package com.mediatek.mms.ext;

import android.content.Context;

public interface IOpLoadReqExt {

    /**
     * @internal
     */
    void executeReq(Context context, long threadId, IOpMmsDraftDataExt opDraftData);
}