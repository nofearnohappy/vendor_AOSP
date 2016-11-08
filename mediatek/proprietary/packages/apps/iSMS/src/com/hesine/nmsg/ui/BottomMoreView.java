package com.hesine.nmsg.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;

import com.hesine.nmsg.R;
import com.hesine.nmsg.common.CommonUtils;
import com.hesine.nmsg.common.FileEx;

public class BottomMoreView extends LinearLayout implements OnClickListener {
    public static final class MsgType {
        public static final int IMAGE = 1;
        public static final int CAMERA = 2;
        public static final int RECORDER = 3;
    }

    // private Context mContext;
    private View convertView;
    private View mBottomMoreView;
    private View mImage;
    private View mCamera;
    private View mRecorder;
    private ActionListener mListener;

    public BottomMoreView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // mContext = context;
        convertView = LayoutInflater.from(context).inflate(R.layout.bottom_more_view, this, true);
        mBottomMoreView = (View) convertView.findViewById(R.id.bottom_more_view);
        mImage = (View) convertView.findViewById(R.id.image);
        mCamera = (View) convertView.findViewById(R.id.camera);
        mRecorder = (View) convertView.findViewById(R.id.recorder);
        mImage.setOnClickListener(this);
        mCamera.setOnClickListener(this);
        mRecorder.setOnClickListener(this);
    }

    public void onClick(View v) {
        int type = getActionType(v);
        if (v.equals(mImage) || v.equals(mCamera) || v.equals(mRecorder)) {
            if (!FileEx.getSDCardStatus()) {
                CommonUtils.createLoseSDCardNotice(getContext());
                return;
            }

        }
        mListener.doAction(type);
    }

    private int getActionType(View v) {
        int type = 0;
        if (v == mImage) {
            type = MsgType.IMAGE;
        } else if (v == mCamera) {
            type = MsgType.CAMERA;
        } else if (v == mRecorder) {
            type = MsgType.RECORDER;
        }
        return type;
    }

    public boolean isShow() {
        return (mBottomMoreView.getVisibility() == View.VISIBLE) ? true : false;
    }

    public void show() {
        mBottomMoreView.setVisibility(View.VISIBLE);
    }

    public void hide() {
        mBottomMoreView.setVisibility(View.GONE);
    }

    public void addActionListener(ActionListener listener) {
        mListener = listener;
    }
}
