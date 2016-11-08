package com.hesine.nmsg.ui;

import com.hesine.nmsg.R;

import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class EditAccoutDialog implements View.OnClickListener {
    public static class ActionType {
        public static final int ACTION_LEFT = 0;
        public static final int ACTION_RIGHT = 1;
    }

    private Context mContext;
    private TextView mInfo;
    private EditText mEditText;
    private TextView mLeft;
    private TextView mRight;
    private ActionListener mListener;
    private Dialog mDialog;
    private Object mStore;

    public EditAccoutDialog(Context context) {
        mContext = context;
        mDialog = new Dialog(mContext, R.style.Theme_pop_dialog);
        mDialog.setContentView(R.layout.edit_account_diaglog);
        mInfo = (TextView) mDialog.findViewById(R.id.info);
        mEditText = (EditText) mDialog.findViewById(R.id.edit_text);
        mLeft = (TextView) mDialog.findViewById(R.id.left);
        mRight = (TextView) mDialog.findViewById(R.id.right);
        mLeft.setOnClickListener(this);
        mRight.setOnClickListener(this);
    }

    public EditAccoutDialog show() {
        mDialog.show();
        return this;
    }

    public EditAccoutDialog setCancelable(boolean cancelable) {
        mDialog.setCancelable(cancelable);
        return this;
    }

    public EditAccoutDialog setInfo(int resId) {
        setInfo(mContext.getText(resId).toString());
        return this;
    }

    public EditAccoutDialog setInfo(String title) {
        mInfo.setText(title);
        return this;
    }

    public EditAccoutDialog setLeft(int resid) {
        mLeft.setText(mContext.getText(resid));
        return this;
    }

    public EditAccoutDialog setLeft(String title) {
        mLeft.setText(title);
        return this;
    }

    public EditAccoutDialog setRight(int resid) {
        mRight.setText(mContext.getText(resid));
        return this;
    }

    public EditAccoutDialog setRight(String title) {
        mRight.setText(title);
        return this;
    }

    public EditAccoutDialog setListener(ActionListener listener) {
        mListener = listener;
        return this;
    }

    public void setTag(Object obj) {
        mStore = obj;
    }

    public Object getTag() {
        return mStore;
    }

    public void setEditTextInputType(int type) {
        mEditText.setInputType(type);
    }

    public void setFilters(InputFilter[] type) {
        mEditText.setFilters(type);
    }

    public void setText(CharSequence text) {
        mEditText.setText(text);
    }

    public void append(CharSequence text) {
        mEditText.append(text);
    }

    public Editable getText() {
        return mEditText.getText();
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
