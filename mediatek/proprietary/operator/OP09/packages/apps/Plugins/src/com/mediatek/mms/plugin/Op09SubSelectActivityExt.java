package com.mediatek.mms.plugin;

import android.app.Activity;
import android.content.Context;

import com.mediatek.mms.ext.DefaultOpSubSelectActivityExt;

public class Op09SubSelectActivityExt extends DefaultOpSubSelectActivityExt {

    Context mContext;
    private Op09StringReplacementExt mStringReplacementPlugin;

    @Override
    public void onCreate(Activity hostActivity) {

        mContext = hostActivity.getBaseContext();
        mStringReplacementPlugin = new Op09StringReplacementExt(mContext);
    }

    @Override
    public String[] setSaveLocation() {

        String[] saveLocationDisp = null;
        String[] location = mStringReplacementPlugin.getSaveLocationString();
        if (MessageUtils.isStringReplaceEnable() && location != null) {
            saveLocationDisp = location;
        }
        return saveLocationDisp;

    }
}
