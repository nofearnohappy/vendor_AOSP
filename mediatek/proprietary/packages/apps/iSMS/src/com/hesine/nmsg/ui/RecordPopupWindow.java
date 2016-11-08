package com.hesine.nmsg.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
//import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.hesine.nmsg.R;
import com.hesine.nmsg.common.CommonUtils;

public class RecordPopupWindow {

    private Context mContext;
    private PopupWindow mPopWindow;
    private LayoutInflater mInflater;
    private View mParent;
    private View mContentView;
    // private ImageView ivImage;
    private TextView tvAudioTime;
    private TextView mTips;
    private boolean isdismissed = false;

    public RecordPopupWindow(Context context, View parent) {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);

        mParent = parent;
        constructRecordWinsow();
    }

    private void constructRecordWinsow() {

        mContentView = mInflater.inflate(R.layout.record, null);

        // ivImage = (ImageView)
        // mContentView.findViewById(R.id.voice_rcd_hint_anim);

        tvAudioTime = (TextView) mContentView.findViewById(R.id.AudioTime);
        mTips = (TextView) mContentView.findViewById(R.id.cancel_tips);

        mPopWindow = new PopupWindow(mContentView, CommonUtils.dp2px(mContext, 220), CommonUtils.dp2px(
                mContext, 46));
    }

    public void startRecordAndShowWindow() {
        isdismissed = false;
        mPopWindow.showAtLocation(mParent, Gravity.CENTER, 0, 0);
        mPopWindow.setFocusable(false);
        mPopWindow.setTouchable(false);
        // ivImage.setBackgroundResource(R.drawable.ic_tcl);
        mTips.setText(mContext.getString(R.string.chat_audio_up_motion_cancel));
        mTips.setTextColor(mContext.getResources().getColor(R.color.white));
    }

    public void showWindow() {

        isdismissed = false;
        mPopWindow.showAtLocation(mParent, Gravity.CENTER, 0, 0);
        mPopWindow.setFocusable(false);
        mPopWindow.setTouchable(false);
    }

    public void dissWindow() {

        isdismissed = true;
        mPopWindow.dismiss();
    }

    public boolean isShow() {
        return !isdismissed;
    }

    public void setTime(String t) {
        tvAudioTime.setText(t);
    }

    public void setReleaseToSend() {
        // ivImage.setBackgroundResource(R.drawable.ic_tcl);
        mTips.setText(mContext.getString(R.string.chat_audio_up_motion_cancel));
        mTips.setTextColor(mContext.getResources().getColor(R.color.white));
    }

    public void setMotionUpToCancel() {
        // ivImage.setBackgroundResource(R.drawable.ic_tcl);
        mTips.setText(mContext.getString(R.string.chat_audio_up_motion_cancel));
        mTips.setTextColor(mContext.getResources().getColor(R.color.white));
    }

    public void setReleaseToCancel() {
        // ivImage.setBackgroundResource(R.drawable.ic_msg_failed);
        mTips.setText(mContext.getString(R.string.chat_audio_finger_up_cancel));
        mTips.setTextColor(mContext.getResources().getColor(R.color.red));
    }
}
