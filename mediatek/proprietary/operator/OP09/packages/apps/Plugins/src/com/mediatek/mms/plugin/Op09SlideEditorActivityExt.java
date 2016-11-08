package com.mediatek.mms.plugin;

import android.app.Activity;
import android.content.Context;
import android.view.MotionEvent;

import com.mediatek.mms.callback.ITextSizeAdjustHost;
import com.mediatek.mms.ext.DefaultOpSlideEditorActivityExt;

/**
 * Op09SlideEditorActivityExt.
 *
 */
public class Op09SlideEditorActivityExt extends DefaultOpSlideEditorActivityExt {

    private Op09MmsTextSizeAdjustExt mMmsTextSizeAdjust;

    /**
     * Construction.
     * @param context Context
     */
    public Op09SlideEditorActivityExt(Context context) {
        super(context);
        mMmsTextSizeAdjust = Op09MmsTextSizeAdjustExt.getInstance();
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
