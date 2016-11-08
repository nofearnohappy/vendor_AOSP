package com.mediatek.browser.plugin;

import android.os.Build;
import android.util.Log;

import com.mediatek.browser.ext.DefaultBrowserSettingExt;
import com.mediatek.common.PluginImpl;

@PluginImpl(interfaceName="com.mediatek.browser.ext.IBrowserSettingExt")
public class Op03BrowserSettingExt extends DefaultBrowserSettingExt {

    private static final String TAG = "Op03BrowserSettingExt";

    public String getOperatorUA(String defaultUA) {
        Log.i("@M_" + TAG, "Enter: " + "getOperatorUA, default UA: " + defaultUA + " --OP03 implement");
        String op03UA = defaultUA;
        if (!defaultUA.contains("-orange")) {
            op03UA = defaultUA.replace(Build.MODEL, Build.MODEL + "-orange");
        }
        return op03UA;
    }

}
