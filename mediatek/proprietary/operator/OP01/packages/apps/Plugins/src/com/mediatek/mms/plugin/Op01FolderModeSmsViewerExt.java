package com.mediatek.mms.plugin;

import android.app.Activity;
import android.content.Context;
import android.view.MotionEvent;

import com.mediatek.mms.callback.ITextSizeAdjustHost;
import com.mediatek.mms.ext.DefaultOpFolderModeSmsViewerExt;

/**
 * Op01FolderModeSmsViewerExt.
 *
 */
public class Op01FolderModeSmsViewerExt extends DefaultOpFolderModeSmsViewerExt {

    private Op01MmsTextSizeAdjust mMmsTextSizeAdjust;

    /**
     * Construction.
     * @param context Context
     */
    public Op01FolderModeSmsViewerExt(Context context) {
        super(context);
        mMmsTextSizeAdjust = new Op01MmsTextSizeAdjust();
    }

    @Override
    public String forwardMessage(Context context, String smsBody,
            String nameAndNumber, int boxId) {
        return new Op01MmsPreference(getBaseContext()).formatSmsBody(
                                        context, smsBody, nameAndNumber, boxId);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return mMmsTextSizeAdjust.dispatchTouchEvent(ev);
    }

    @Override
    public void onStart(ITextSizeAdjustHost host, Activity activity) {
        mMmsTextSizeAdjust.init(host, activity);
        mMmsTextSizeAdjust.refresh();
    }
}
