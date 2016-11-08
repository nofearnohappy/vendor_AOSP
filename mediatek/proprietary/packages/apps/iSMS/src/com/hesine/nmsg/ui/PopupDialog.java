package com.hesine.nmsg.ui;

import com.hesine.nmsg.R;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

public class PopupDialog implements View.OnClickListener {
    public static class ActionType {
        public static final int ACTION_LEFT = 0;
        public static final int ACTION_RIGHT = 1;
    }

    private Context mContext;
    private TextView mInfo;
    private TextView mLeft;
    private TextView mRight;
    private ActionListener mListener;
    private Dialog mDialog;
    private Object mStore;

    public PopupDialog(Context context) {
        mContext = context;
        mDialog = new Dialog(mContext, R.style.Theme_pop_dialog);
        mDialog.setContentView(R.layout.popup_dialog);
        mInfo = (TextView) mDialog.findViewById(R.id.info);
        mLeft = (TextView) mDialog.findViewById(R.id.left);
        mRight = (TextView) mDialog.findViewById(R.id.right);
        mLeft.setOnClickListener(this);
        mRight.setOnClickListener(this);
    }

    public PopupDialog show() {
        mDialog.show();
        return this;
    }

    public PopupDialog setCancelable(boolean cancelable) {
        mDialog.setCancelable(cancelable);
        return this;
    }

    public PopupDialog setInfo(int resId) {
        setInfo(mContext.getText(resId).toString());
        return this;
    }

    public PopupDialog setInfo(String title) {
        mInfo.setText(title);
        return this;
    }

    public PopupDialog setLeft(int resid) {
        mLeft.setText(mContext.getText(resid));
        return this;
    }

    public PopupDialog setLeft(String title) {
        mLeft.setText(title);
        return this;
    }

    public PopupDialog setRight(int resid) {
        mRight.setText(mContext.getText(resid));
        return this;
    }

    public PopupDialog setRight(String title) {
        mRight.setText(title);
        return this;
    }

    public PopupDialog setListener(ActionListener listener) {
        mListener = listener;
        return this;
    }

    public void setTag(Object obj) {
        mStore = obj;
    }

    public Object getTag() {
        return mStore;
    }

    public void dismiss() {
        mDialog.dismiss();
    }

    public void onClick(View v) {
        if (mListener == null) {
            return;
        }
        switch (v.getId()) {
            case R.id.left:
                mListener.doAction(ActionType.ACTION_LEFT);
                break;
            case R.id.right:
                mListener.doAction(ActionType.ACTION_RIGHT);
                break;
            default:
                break;
        }
    }
}