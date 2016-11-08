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

package com.dolby.ds1appUI;

import android.content.Context;
import android.graphics.Typeface;

public class Assets {

    // Ref.:
    // http://docs.oracle.com/javase/tutorial/java/javaOO/enum.html
    // http://docs.oracle.com/javase/1.5.0/docs/guide/language/enums.html
    public enum FontType {
        REGULAR, LIGHT, MEDIUM
    }

    private static Typeface sFontRegular;
    private static Typeface sFontLight;
    private static Typeface sFontMedium;

    public static void init(Context context) {
        sFontRegular = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Regular.ttf");
        sFontLight = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Light.ttf");
        sFontMedium = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Medium.ttf");
    }

    public static final Typeface getFont(FontType type) {

        switch (type) {
        case LIGHT:
            return sFontLight;
        case MEDIUM:
            return sFontMedium;
        case REGULAR:
        default:
            return sFontRegular;
        }
    }
}
