package com.mediatek.mms.plugin;


import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.mediatek.mms.ext.DefaultOpAttachmentEditorExt;
import com.mediatek.op09.plugin.R;

public class Op09AttachmentEditorExt extends DefaultOpAttachmentEditorExt {

    private static String TAG = "Op09AttachmentEditorExt";
    static final int MSG_SEND_SLIDESHOW   = 2;
    private Handler mHandler;
    private Op09DualSendButton mDualSendButton = null;
    private Context mResourceContext;
    private int mSubCount;
    private ImageButton mBigButton;
    private ImageButton mSmallButton;
    private View mOriginButton;

    Op09AttachmentEditorExt(Context context) {
        mResourceContext = context;
    }

    @Override
    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    @Override
    public void updateSendButton(boolean canSend) {
        /// M: For Op09Feature: dualSendButton;
        if (mDualSendButton == null) {
            Log.e(TAG, "updateSendButton: dualSendButton can not be initialized");
            return;
        }
        mDualSendButton.setEnabled(canSend);
    }

    @Override
    public void createSlideshowView(Context context,
            LinearLayout slideshowPanel, Button sendButton, Button removeButton) {
        if (mDualSendButton == null) {
            mDualSendButton = new Op09DualSendButton(mResourceContext);
            mBigButton = (ImageButton) slideshowPanel.getChildAt(0);
            mSmallButton = (ImageButton) slideshowPanel.getChildAt(1);
            mOriginButton = sendButton;
            mDualSendButton.initView(context, mSmallButton, mBigButton, null);
            mDualSendButton.updateSendButton();
            mDualSendButton.setOnClickListener(mDualBtnListener);
            updateSendButtonLayout();
        }
    }

    /**
     * notifySubChanged.
     */
    public void notifySubChanged() {
        updateSendButtonLayout();
        if (mDualSendButton != null) {
            mDualSendButton.updateSendButton();
        }
    }

    private void updateSendButtonLayout() {
        int subCount = MessageUtils.getActiveSubCount(mResourceContext);
        if (mBigButton != null) {
            android.view.ViewGroup.LayoutParams lp = mBigButton.getLayoutParams();
            if (subCount == 1) {
                lp.width = mResourceContext.getResources().getDimensionPixelOffset(
                        R.dimen.attchment_view_send_button_length);
            } else if (subCount == 2) {
                lp.width = mResourceContext.getResources().getDimensionPixelOffset(
                        R.dimen.attchment_view_send_button_half_length);
            }
            mBigButton.setLayoutParams(lp);
        }
        if (mOriginButton != null) {
            if (subCount > 0) {
                mOriginButton.setVisibility(View.GONE);
            } else {
                mOriginButton.setVisibility(View.VISIBLE);
            }
        }
    }


    /// M: for op09Feature: DualSendBtn; the dual send button listener;
    Op09DualSendButton.OnClickListener mDualBtnListener = new Op09DualSendButton.OnClickListener() {

        @Override
        public void onClick(View view, int subId) {
            Message msg = Message.obtain(mHandler, MSG_SEND_SLIDESHOW);
            Bundle data = new Bundle();
            data.putInt("send_sub_id", subId);
            msg.setData(data);
            msg.sendToTarget();
        }
    };
}
