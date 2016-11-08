package com.mediatek.mms.plugin;

import android.content.Context;
import android.widget.TextView;

import com.mediatek.mms.ext.DefaultOpMmsPlayerActivityAdapterExt;

/**
 * Op01MmsPlayerActivityAdapterExt.
 *
 */
public class Op01MmsPlayerActivityAdapterExt extends
        DefaultOpMmsPlayerActivityAdapterExt {

    /**
     * Construction.
     * @param context Context
     */
    public Op01MmsPlayerActivityAdapterExt(Context context) {
        super(context);
    }

    @Override
    public void setExtendUrlSpan(TextView textView) {
        Op01MmsConfigExt.setExtendUrlSpan(textView);
    }
}
