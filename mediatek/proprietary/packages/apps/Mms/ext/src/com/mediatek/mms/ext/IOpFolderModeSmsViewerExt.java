package com.mediatek.mms.ext;

import com.mediatek.mms.callback.ITextSizeAdjustHost;

import android.app.Activity;
import android.content.Context;
import android.view.MotionEvent;

public interface IOpFolderModeSmsViewerExt {
    /**
     * @internal
     */
    String forwardMessage(Context context, String smsBody, String nameAndNumber, int boxId);
    /**
     * @internal
     */
    void onStart(ITextSizeAdjustHost host, Activity activity);
    /**
     * @internal
     */
    boolean dispatchTouchEvent(MotionEvent ev);
}
