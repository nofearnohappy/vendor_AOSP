package com.mediatek.mms.ext;

import com.mediatek.mms.callback.ITextSizeAdjustHost;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.MotionEvent;

public class DefaultOpWPMessageActivityExt extends ContextWrapper implements
        IOpWPMessageActivityExt {

    public DefaultOpWPMessageActivityExt(Context base) {
        super(base);
    }

    @Override
    public void onStart(ITextSizeAdjustHost host, Activity activity) {

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return false;
    }
}
