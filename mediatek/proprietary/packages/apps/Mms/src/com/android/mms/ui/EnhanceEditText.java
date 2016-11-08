package com.android.mms.ui;

import android.R.id;
import android.widget.EditText;
import android.content.Context;
import android.util.AttributeSet;

public class EnhanceEditText extends EditText {
    private static final int ID_PASTE = android.R.id.paste;
    private boolean mIsMenuItemClicked = false;

    public EnhanceEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean onTextContextMenuItem(int id) {
        mIsMenuItemClicked = true;
        boolean result = super.onTextContextMenuItem(id);
        mIsMenuItemClicked = false;
        return result;
    }

    public boolean checkIsMenuItemClicked() {
        return mIsMenuItemClicked;
    }
}
