package com.mediatek.mms.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.TextView;

import com.google.android.mms.pdu.PduBody;

import com.mediatek.mms.callback.IMmsPlayerActivityCallback;
import com.mediatek.mms.callback.ITextSizeAdjustHost;
import com.mediatek.mms.ext.DefaultOpMmsPlayerActivityExt;

/**
 * Op09MmsPlayerActivityExt.
 *
 */
public class Op09MmsPlayerActivityExt extends DefaultOpMmsPlayerActivityExt {

    private Op09MmsTextSizeAdjustExt mMmsTextSizeAdjustExt;

    /**
     * Construction.
     * @param context Context
     */
    public Op09MmsPlayerActivityExt(Context context) {
        super(context);
        mMmsTextSizeAdjustExt = Op09MmsTextSizeAdjustExt.getInstance();
    }

    @Override
    public void onStart(ITextSizeAdjustHost host, Activity activity) {
        mMmsTextSizeAdjustExt.init(host, activity);
        mMmsTextSizeAdjustExt.refresh();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return mMmsTextSizeAdjustExt.dispatchTouchEvent(ev);
    }
}
