package com.mediatek.mms.plugin;

import com.mediatek.mms.ext.DefaultOpDefaultRetrySchemeExt;

public class Op09DefaultRetrySchemeExt extends DefaultOpDefaultRetrySchemeExt {

    @Override
    public int[] init(int... messageType) {
        if (messageType != null && messageType.length == 1) {
            return new Op09MmsConfigExt().getMmsRetryScheme(messageType[0]);
        }
        return null;
    }
}
