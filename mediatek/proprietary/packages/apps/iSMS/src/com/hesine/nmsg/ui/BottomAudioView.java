package com.hesine.nmsg.ui;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hesine.nmsg.R;

public class BottomAudioView extends LinearLayout implements OnTouchListener {
    public static final class MsgType {
        public static final int STARTRECORD = 0;
        public static final int SENDRECORD = 1;
        public static final int CANCELRECORD = 2;
        public static final int FINGERUPTOCANCELRECORD = 3;
        public static final int MOTIONUPTOCANCELRECORD = 4;
        public static final int ACTION_SHARE = 5;
        public static final int ACTION_INPUT = 6;
    }

    private Context mContext;
    private View convertView;
    private View mAudioView;
    private View mBottomAudioView;
    private ImageView mImage;
    private TextView mDescription;
    private boolean mTouchSend = false;

    private ActionListener mListener;

    public BottomAudioView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        convertView = LayoutInflater.from(context).inflate(R.layout.bottom_audio_view, this, true);
        mBottomAudioView = convertView.findViewById(R.id.bottom_audio_view);
        mAudioView = convertView.findViewById(R.id.audio_view);
        mImage = (ImageView) convertView.findViewById(R.id.image);
        mDescription = (TextView) convertView.findViewById(R.id.description);
        mAudioView.setOnTouchListener(this);
        mImage.setImageResource(R.drawable.ic_recorder_off);
        mDescription.setText(mContext.getString(R.string.chat_audio_tips));
    }

    public boolean isShow() {
        return (mBottomAudioView.getVisibility() == View.VISIBLE) ? true : false;
    }

    public void show() {
        mBottomAudioView.setVisibility(View.VISIBLE);
    }

    public void hide() {
        mBottomAudioView.setVisibility(View.GONE);
    }

    public void addActionListener(ActionListener listener) {
        mListener = listener;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            mTouchSend = true;
            mDescription.setText(R.string.chat_audio_tips);
            mImage.setImageResource(R.drawable.ic_recorder_on);
            mListener.doAction(MsgType.STARTRECORD);
        } else if (action == MotionEvent.ACTION_UP) {
            mDescription.setText(R.string.chat_audio_tips);
            mImage.setImageResource(R.drawable.ic_recorder_off);
            if (mTouchSend) {
                mListener.doAction(MsgType.SENDRECORD);
            } else {
                mListener.doAction(MsgType.CANCELRECORD);
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            Rect outRect = new Rect();
            v.getGlobalVisibleRect(outRect);
            // int x = (int)event.getRawX();
            int y = (int) event.getRawY();
            if (y < outRect.top && mTouchSend) {
                mTouchSend = false;
                mDescription.setText(R.string.chat_audio_tips);
                mListener.doAction(MsgType.FINGERUPTOCANCELRECORD);
            } else if (y > outRect.top && !mTouchSend) {
                mTouchSend = true;
                mDescription.setText(R.string.chat_audio_tips);
                mListener.doAction(MsgType.MOTIONUPTOCANCELRECORD);
            }
        }
        return true;
    }

}
