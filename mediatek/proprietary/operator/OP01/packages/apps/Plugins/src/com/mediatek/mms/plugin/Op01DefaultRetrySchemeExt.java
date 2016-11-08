package com.mediatek.mms.plugin;

import com.mediatek.mms.ext.DefaultOpDefaultRetrySchemeExt;

/**
 * Op01DefaultRetrySchemeExt.
 *
 */
public class Op01DefaultRetrySchemeExt extends DefaultOpDefaultRetrySchemeExt {

    private Op01MmsConfigExt mMmsConfigExt;

    @Override
    public int[] init(int... messageType) {
        mMmsConfigExt = new Op01MmsConfigExt();
        return mMmsConfigExt.getMmsRetryScheme();
    }
}
