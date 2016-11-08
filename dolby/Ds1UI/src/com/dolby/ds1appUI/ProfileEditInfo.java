/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2011-2012 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

package com.dolby.ds1appUI;

import android.widget.EditText;
import android.widget.TextView;

public class ProfileEditInfo {

    public int mPosition;
    public TextView mTextView;
    public EditText mEditText;

    public ProfileEditInfo(int position, TextView textView, EditText editText) {
        mPosition = position;
        mTextView = textView;
        mEditText = editText;
    }
}
