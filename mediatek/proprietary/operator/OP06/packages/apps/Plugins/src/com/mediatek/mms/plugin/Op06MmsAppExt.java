package com.mediatek.mms.plugin;

import android.content.Context;

import com.mediatek.mms.callback.IMmsAppCallback;
import com.mediatek.mms.ext.DefaultOpMmsAppExt;

public class Op06MmsAppExt extends DefaultOpMmsAppExt {
    private Op06RegionalPhoneSmsConfigExt mConfig;

    public Op06MmsAppExt(Context context) {
        super(context);
        mConfig = new Op06RegionalPhoneSmsConfigExt(context);
    }

    public void onCreate(IMmsAppCallback mmsApp) {
        mConfig.init(mmsApp);
    }
}
