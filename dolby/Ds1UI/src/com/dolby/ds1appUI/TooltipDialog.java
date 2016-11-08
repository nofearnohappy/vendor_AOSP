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

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class TooltipDialog extends DialogFragment {
    public static TooltipDialog newInstance(int title, int text) {
    	TooltipDialog f = new TooltipDialog();
        Bundle args = new Bundle();
        args.putInt("Title", title);
        args.putInt("Text", text);
        f.setArguments(args);
        return f;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, 0);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.tooltip1, container, false);
        View tv = v.findViewById(R.id.text);
        ((TextView)tv).setText(getArguments().getInt("Text"));
        tv = v.findViewById(R.id.title);
        ((TextView)tv).setText(getArguments().getInt("Title"));
        this.getDialog().setCanceledOnTouchOutside(true);
        return v;
    }
}
