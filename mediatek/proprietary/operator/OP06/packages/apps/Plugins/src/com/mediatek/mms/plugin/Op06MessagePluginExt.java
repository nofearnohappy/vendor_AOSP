package com.mediatek.mms.plugin;

import android.content.Context;

import com.mediatek.common.PluginImpl;
import com.mediatek.mms.ext.DefaultOpMessagePluginExt;
import com.mediatek.mms.ext.IOpMmsAppExt;

@PluginImpl(interfaceName = "com.mediatek.mms.ext.IOpMessagePluginExt")
public class Op06MessagePluginExt extends DefaultOpMessagePluginExt {

    private Context mContext;
    public Op06MessagePluginExt(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public IOpMmsAppExt getOpMmsAppExt() {
        return new Op06MmsAppExt(mContext);
    }
}
