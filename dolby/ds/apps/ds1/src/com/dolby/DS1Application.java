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

package com.dolby;

import android.app.Application;
import android.content.res.Configuration;
import android.util.Log;

public class DS1Application extends Application {

    private static DS1Application mContext;

    public static DS1Application getStaticContext() {
        return mContext;
    }

    private static void setStaticContext(DS1Application context) {
        mContext = context;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        setStaticContext(this);

        final int screenLayoutSize = getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK;
        final int densityDpi = getResources().getDisplayMetrics().densityDpi;
        Log.d(Tag.MAIN, "screenLayoutSize: " + screenLayoutSize);
        Log.d(Tag.MAIN, "densityDpi: " + densityDpi);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        setStaticContext(null);
    }
}
