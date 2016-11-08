package com.mediatek.mediatekdm.mdm;

import android.util.Log;

/**
 * A wrapper of android.util.Log. The tag is formatted to include current thread name. Format is:
 * tag(threadname)
 */
public class MdmLog {

    public static void d(String tag, String message) {
        Log.d(tag + "(" + Thread.currentThread().getName() + ")", message);
    }

    public static void e(String tag, String message) {
        Log.e(tag + "(" + Thread.currentThread().getName() + ")", message);
    }

    public static void v(String tag, String message) {
        Log.v(tag + "(" + Thread.currentThread().getName() + ")", message);
    }

    public static void i(String tag, String message) {
        Log.i(tag + "(" + Thread.currentThread().getName() + ")", message);
    }

    public static void w(String tag, String message) {
        Log.w(tag + "(" + Thread.currentThread().getName() + ")", message);
    }
}
