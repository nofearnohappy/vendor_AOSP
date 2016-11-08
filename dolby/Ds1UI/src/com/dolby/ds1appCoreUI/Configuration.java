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

import android.content.Context;
import android.util.Log;

import com.dolby.ds1appUI.R;

public class Configuration {

    private static Configuration dynamicInstance;
    private static final float DEFAULT_MIN_EDIT_GAIN = -12.0f;
    private static final float DEFAULT_MAX_EDIT_GAIN = 12.0f;
    private float minEditGain = DEFAULT_MIN_EDIT_GAIN;
    private float maxEditGain = DEFAULT_MAX_EDIT_GAIN;

    private Configuration(Context ctx) {
        try {
            minEditGain = Float.parseFloat(ctx.getResources().getString(R.string.min_edit_gain));
            maxEditGain = Float.parseFloat(ctx.getResources().getString(R.string.max_edit_gain));
        } catch (NumberFormatException nfe) {
            minEditGain = Float.NaN;
            maxEditGain = Float.NaN;
            Log.e(Tag.MAIN, "Some of values from configuration.xml were not float type!");
        } catch (NullPointerException npe) {
            minEditGain = Float.NaN;
            maxEditGain = Float.NaN;
            Log.e(Tag.MAIN, "Some of values from configuration.xml were not loaded!");
        }
    }

    /**
     * @param ctx
     *            - needs to be context but be aware not to use the Activity
     *            object. Instead use the method getApplicationContext
     * @return - Configuration object
     */
    public static Configuration getInstance(Context ctx) {
        if (dynamicInstance == null) {
            dynamicInstance = new Configuration(ctx);
        }
        return dynamicInstance;
    }

    public float getMaxEditGain() {
        if (Float.isNaN(maxEditGain))
            return DEFAULT_MAX_EDIT_GAIN;
        else
            return maxEditGain;
    }

    public float getMinEditGain() {
        if (Float.isNaN(minEditGain))
            return DEFAULT_MIN_EDIT_GAIN;
        else
            return minEditGain;
    }

}
