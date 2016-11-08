package com.mediatek.mms.plugin;

import android.content.Context;

import com.mediatek.common.PluginImpl;
import com.mediatek.mms.ext.DefaultOpMessagePluginExt;
import com.mediatek.mms.ext.IOpMmsConfigExt;

@PluginImpl(interfaceName = "com.mediatek.mms.ext.IOpMessagePluginExt")
public class Op07MessagePluginExt extends DefaultOpMessagePluginExt {

    public Op07MessagePluginExt(Context context) {
        super(context);
    }

    @Override
    public IOpMmsConfigExt getOpMmsConfigExt() {
        return new Op07MmsConfigExt();
    }
}
