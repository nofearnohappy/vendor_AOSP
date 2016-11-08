package com.mediatek.mms.plugin;

import android.content.Context;

import com.mediatek.mms.callback.IMmsAppCallback;
import com.mediatek.mms.ext.DefaultOpMmsAppExt;

/**
 * Op01MmsAppExt.
 *
 */
public class Op01MmsAppExt extends DefaultOpMmsAppExt {

    /**
     * Construction.
     * @param context Context
     */
    public Op01MmsAppExt(Context context) {
        super(context);
    }

    @Override
    public void onCreate(IMmsAppCallback mmsApp) {
        mmsApp.initMuteCache();
    }
}
