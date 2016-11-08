package com.mediatek.mms.plugin;

import android.app.Activity;
import android.content.Context;
import android.view.MotionEvent;

import com.mediatek.mms.callback.ITextSizeAdjustHost;
import com.mediatek.mms.ext.DefaultOpSlideEditorActivityExt;

/**
 * Op01SlideEditorActivityExt.
 *
 */
public class Op01SlideEditorActivityExt extends DefaultOpSlideEditorActivityExt {

    private Op01MmsTextSizeAdjust mMmsTextSizeAdjust;

    /**
     * Construction.
     * @param context Context
     */
    public Op01SlideEditorActivityExt(Context context) {
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
