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

package com.dolby.ds1appCoreUI;

import java.text.DecimalFormat;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Process;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class Tools {

    public static final DecimalFormat mDecFormat = new DecimalFormat("@@##");

    public static final int lcm(int x1, int x2) {
        if (x1 <= 0 || x2 <= 0) {
            throw new IllegalArgumentException("Cannot compute the least " + "common multiple of two " + "numbers if one, at least," + "is negative.");
        }
        int max, min;
        if (x1 > x2) {
            max = x1;
            min = x2;
        } else {
            max = x2;
            min = x1;
        }
        for (int i = 1; i <= min; i++) {
            if ((max * i) % min == 0) {
                return i * max;
            }
        }
        throw new Error("Cannot find the least common multiple of numbers " + x1 + " and " + x2);
    }

    public static void killMyself(Context context) {
        // first method
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        try {
            am.restartPackage(context.getPackageName());
            Thread.sleep(500);
        } catch (Throwable t) {
        }

        // second method
        try {
            Process.killProcess(Process.myPid());
            Thread.sleep(500);
        } catch (Throwable t) {
        }
    }

    public static void showVirtualKeyboard(Context context) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, 0);
    }

    public static void showVirtualKeyboard(final View v) {
        InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(v, 0);
    }

    public static boolean hideVirtualKeyboard(Activity currentActivity) {
        boolean keyboardHidden = false;
        View currentView = currentActivity.getCurrentFocus();
        if (currentView != null) {
            InputMethodManager imm = (InputMethodManager) currentActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboardHidden = imm.hideSoftInputFromWindow(currentView.getWindowToken(), 0);
        }
        return keyboardHidden;
    }

    public static boolean isLandscapeScreenOrientation(Context context) {
        return Configuration.ORIENTATION_LANDSCAPE == context.getResources().getConfiguration().orientation;
    }

    public static String floatArrayToString(float[] arr) {
        final StringBuffer sb = new StringBuffer();

        for (int i = 0; i < arr.length; i++) {
            if (sb.length() != 0) {
                sb.append(", ");
            }
            sb.append(mDecFormat.format(arr[i]));
        }

        return sb.toString();
    }

}
