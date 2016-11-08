/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2011-2013 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

package com.dolby.ds1appCoreUI;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;

public class DS1Application extends Application {
    public static final String TAG = DS1Application.class.getSimpleName();

    public static final Handler HANDLER = new Handler(Looper.getMainLooper());
    
    //For CR:DS1SOC-225 , the default profile name in ds1-default.xml
    private static final String DS_DEFAULT_NAME_CUSTOM1 = "Custom 1";
    private static final String DS_DEFAULT_NAME_CUSTOM2 = "Custom 2";
    //DS1SOC-566, enable/disable the visualizer data 
    public static final boolean VISUALIZER_ENABLE = true;

    public static String getDefaultProfileNameCustom1(){
    	return DS_DEFAULT_NAME_CUSTOM1;
    }
    
    public static String getDefaultProfileNameCustom2(){
    	return DS_DEFAULT_NAME_CUSTOM2;
    }

    private final static String PREFS_NAME = "DsUICustomProfile";
    public static final int CUSTOM_NAME_NOT_MODIFIED = 0;
    public static final int CUSTOM_1_NAME_MODIFIED = 1 << 0;
    public static final int CUSTOM_2_NAME_MODIFIED = 1 << 1;
    
    public static int getCustomModifyFlag(Context context) {
        boolean bModified_Custom1 = false;
        boolean bModified_Custom2 = false;

		if (context != null) {
			SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_WORLD_READABLE | Context.MODE_MULTI_PROCESS);
			bModified_Custom1 = sp.getBoolean("bModified_Custom1", false);
			bModified_Custom2 = sp.getBoolean("bModified_Custom2", false);
		} else {
			Log.e(TAG, "getCustomModifyFlag(), context == null");
		}

		int ret = CUSTOM_NAME_NOT_MODIFIED;
		if (true == bModified_Custom1) {
			ret |= CUSTOM_1_NAME_MODIFIED;
		}
		if (true == bModified_Custom2) {
			ret |= CUSTOM_2_NAME_MODIFIED;
		}
		return ret;
    }

    public static void saveCustomNameModifiedStatus(Context context, boolean bModified_Custom1, boolean bModified_Custom2) { 
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_WORLD_READABLE | Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("bModified_Custom1", bModified_Custom1);
        editor.putBoolean("bModified_Custom2", bModified_Custom2);
        editor.commit();
        //Log.d("TAG", "bModified_Custom1 = " + bModified_Custom1 + ", bModified_Custom2 = " + bModified_Custom2);
    }

    public void printScreenSpecs() {
        final int screenLayoutSize = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        final int densityDpi = getResources().getDisplayMetrics().densityDpi;
        Log.d(Tag.MAIN, "screenLayoutSize: " + screenLayoutSize);
        Log.d(Tag.MAIN, "densityDpi: " + densityDpi);
    }

    /**
     * Get screen resolution in pixels. It includes height of status bar.
     * 
     * @return
     */
    public Point getScreenResolution() {
        Point p = new Point();
        final DisplayMetrics dm = getResources().getDisplayMetrics();

        int screenW = dm.widthPixels;
        int screenH = dm.heightPixels + Constants.STATUS_BAR_HEIGHT;
        if (screenH > screenW) {
            int tmp = screenH;
            screenH = screenW;
            screenW = tmp;
        }

        p.x = screenW;
        p.y = screenH;

        return p;
    }

    /**
     * Replace screen size if necessary in Android configuration. Requirement:
     * xlarge size IF AND OLNY IF screen width >= 1280 AND screen height >= 800;
     * large size IF AND ONLY IF screen width >= 1024 AND screen height >= 600.
     * This way proper layouts are used after padding main UI area with black
     * space.
     * 
     * @param screenW
     *            detected screen width
     * @param screenH
     *            detected screen height
     */
    public void checkAndReplaceScreenSize(int screenW, int screenH) {
        final Resources res = getResources();
        final DisplayMetrics dm = res.getDisplayMetrics();
        final Configuration conf = res.getConfiguration();
        int screenLayout = conf.screenLayout;
        final int nativeScreenSize = (screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK);

        if (Configuration.SCREENLAYOUT_SIZE_LARGE == nativeScreenSize || Configuration.SCREENLAYOUT_SIZE_XLARGE == nativeScreenSize) {
            if (screenW <= 0 || screenH <= 0) {
                final Point screenRes = getScreenResolution();
                screenW = screenRes.x;
                screenH = screenRes.y;
            } else if (screenH > screenW) {
                final int t = screenW;
                screenW = screenH;
                screenH = t;

            }
            if (screenW >= Constants.LAYOUT_XLARGE_MAX_WIDTH && screenH >= Constants.LAYOUT_XLARGE_MAX_HEIGHT) {
                screenLayout -= nativeScreenSize;
                screenLayout |= Configuration.SCREENLAYOUT_SIZE_XLARGE;
            } else if (screenW >= Constants.LAYOUT_LARGE_MAX_WIDTH && screenH >= Constants.LAYOUT_LARGE_MAX_HEIGHT) {
                screenLayout -= nativeScreenSize;
                screenLayout |= Configuration.SCREENLAYOUT_SIZE_LARGE;
            }
            // Force MDPI only when not in XXLARGE.
            if (screenW >= Constants.LAYOUT_XXLARGE_MAX_WIDTH || screenH >= Constants.LAYOUT_XXLARGE_MAX_WIDTH) {
                dm.densityDpi = DisplayMetrics.DENSITY_HIGH;
            } else {
                dm.densityDpi = DisplayMetrics.DENSITY_MEDIUM;
            }
            dm.density = ((float) dm.densityDpi) / ((float) DisplayMetrics.DENSITY_DEFAULT);
            dm.scaledDensity = ((float) dm.densityDpi) / ((float) DisplayMetrics.DENSITY_DEFAULT);
        }

        conf.screenLayout = screenLayout;
        res.updateConfiguration(conf, dm);
    }
}
