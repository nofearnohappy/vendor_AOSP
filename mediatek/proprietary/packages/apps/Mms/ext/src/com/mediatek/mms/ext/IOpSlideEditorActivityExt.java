package com.mediatek.mms.ext;

import com.mediatek.mms.callback.ITextSizeAdjustHost;

import android.app.Activity;
import android.view.MotionEvent;

public interface IOpSlideEditorActivityExt {
    /**
     * @internal
     */
    void onStart(ITextSizeAdjustHost host, Activity activity);
    /**
     * @internal
     */
    boolean dispatchTouchEvent(MotionEvent ev);
}
