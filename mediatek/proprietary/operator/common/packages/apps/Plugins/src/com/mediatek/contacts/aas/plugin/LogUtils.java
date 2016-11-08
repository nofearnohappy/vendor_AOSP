package com.mediatek.contacts.aas.plugin;

import android.util.Log;

public class LogUtils {
    private static final String TAG = "Contacts/OP03";
    private static final String LOG_MSG_TSTART = "[";
    private static final String LOG_MSG_TEND = "]";

    public static void d(String tag, String msg) {
        Log.d(TAG, getLogMsg(tag, msg));
    }

    public static void w(String tag, String msg) {
        Log.w(TAG, getLogMsg(tag, msg));
    }

    public static void e(String tag, String msg) {
        Log.e(TAG, getLogMsg(tag, msg));
    }

    private static String getLogMsg(String tag, String msg) {
        StringBuilder sb = new StringBuilder();
        sb.append(LOG_MSG_TSTART).append(tag).append(LOG_MSG_TEND).append(msg);
        return sb.toString();
    }
}
