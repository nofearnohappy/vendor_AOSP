package com.mediatek.mms.ext;

import android.content.Context;
import android.content.ContextWrapper;

import com.mediatek.mms.callback.IMmsAppCallback;

public class DefaultOpMmsAppExt extends ContextWrapper implements IOpMmsAppExt {

    public DefaultOpMmsAppExt(Context base) {
        super(base);
    }

    @Override
    public void onCreate(IMmsAppCallback mmsApp) {

    }

}
