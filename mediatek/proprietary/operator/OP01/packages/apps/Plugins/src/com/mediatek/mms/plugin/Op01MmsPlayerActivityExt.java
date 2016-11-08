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
import com.mediatek.op01.plugin.R;

/**
 * Op01MmsPlayerActivityExt.
 *
 */
public class Op01MmsPlayerActivityExt extends DefaultOpMmsPlayerActivityExt {

    private Op01MmsTextSizeAdjust mMmsTextSizeAdjustExt;

    /**
     * Construction.
     * @param context Context
     */
    public Op01MmsPlayerActivityExt(Context context) {
        super(context);
        mMmsTextSizeAdjustExt = new Op01MmsTextSizeAdjust();
    }

    @Override
    public boolean haveSomethingToCopyToSDCard(Context context,
            PduBody body, IMmsPlayerActivityCallback callback) {
        return callback.hasAttachFiles(context, body);
    }

    @Override
    public void initListAdapter(TextView text, int size) {
        if (size > 1) {
            text.setText(getString(R.string.multi_files));
        }
    }

    @Override
    public void onOptionsItemSelected(Intent intent) {
        Bundle data = new Bundle();
        data.putLong(Op01AttachmentEnhance.MMS_SAVE_MODE,
                Op01AttachmentEnhance.MMS_SAVE_ALL_ATTACHMENT);
        intent.putExtras(data);
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
