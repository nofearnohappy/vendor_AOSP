package com.mediatek.mms.plugin;

import android.app.Activity;
import android.content.Context;
import android.view.MotionEvent;

import com.mediatek.mms.callback.ITextSizeAdjustHost;
import com.mediatek.mms.ext.DefaultOpWPMessageActivityExt;

/**
 * Op01WPMessageActivityExt.
 *
 */
public class Op01WPMessageActivityExt extends DefaultOpWPMessageActivityExt {

    private Op01MmsTextSizeAdjust mMmsTextSizeAdjust;

    /**
     * Construction.
     * @param context Context
     */
    public Op01WPMessageActivityExt(Context context) {
        super(context);
        mMmsTextSizeAdjust = new Op01MmsTextSizeAdjust();
    }

    @Override
    public void onStart(ITextSizeAdjustHost host, Activity activity) {
        mMmsTextSizeAdjust.init(host, activity);
        mMmsTextSizeAdjust.refresh();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return mMmsTextSizeAdjust.dispatchTouchEvent(ev);
    }
}
