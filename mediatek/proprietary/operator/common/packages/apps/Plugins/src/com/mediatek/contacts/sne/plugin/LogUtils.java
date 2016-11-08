package com.mediatek.contacts.sne.plugin;

import android.util.Log;


public class LogUtils {
    public final static String TAG = "Contacts/Sne";

    public static void d(String tag, String msg) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(tag).append("]").append(":").append(msg);
        Log.d(TAG, sb.toString());
    }

    public static void w(String tag, String msg) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(tag).append("]").append(":").append(msg);
        Log.w(TAG, sb.toString());
    }

    public static void e(String tag, String msg) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(tag).append("]").append(":").append(msg);
        Log.e(TAG, sb.toString());
    }
}
