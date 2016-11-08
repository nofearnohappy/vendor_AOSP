package com.mediatek.rcs.common.utils;

import android.content.Context;

public class ContextCacher {

    private static Context sHostContext;
    private static Context sPluginContext;

    public static void setHostContext(Context context) {
        sHostContext = context;
    }
    public static Context getHostContext() {
        return sHostContext;
    }

    public static void setPluginContext(Context context) {
        sPluginContext = context;
    }
    public static Context getPluginContext() {
        return sPluginContext;
    }
}
