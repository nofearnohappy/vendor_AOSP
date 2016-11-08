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

public class Constants {

    public static final int BANDS = 20;
    
    public static final int DEMO_POSITION = 0;

    public static final float MIN_VISIBLE_GAIN = -12f;

    public static final float MAX_VISIBLE_GAIN = 36f;

    public static final float VISIBLE_GAIN_SPAN = MAX_VISIBLE_GAIN - MIN_VISIBLE_GAIN;

    // Ref.: http://developer.android.com/guide/practices/screens_support.html
    // See Configuration Examples section.
    // "Normal" layouts.
    public static final int LAYOUT_NORMAL_MAX_WIDTH = 800;

    public static final int LAYOUT_NORMAL_MAX_HEIGHT = 480;

    public static final int LAYOUT_LARGE_MAX_WIDTH = 1024;

    public static final int LAYOUT_LARGE_MAX_HEIGHT = 600;

    public static final int LAYOUT_XLARGE_MAX_WIDTH = 1280;

    public static final int LAYOUT_XLARGE_MAX_HEIGHT = 800;

    public static final int LAYOUT_XXLARGE_MAX_WIDTH = 1920;

    public static final int LAYOUT_XXLARGE_MAX_HEIGHT = 1200;

    /** Number of predefined profiles with localized names. */
    public static final int PREDEFINED_PROFILE_COUNT = 4;
	public static final int PROFILE_CUSTOM1_INDEX = PREDEFINED_PROFILE_COUNT + 1;
	public static final int PROFILE_CUSTOM2_INDEX = PREDEFINED_PROFILE_COUNT + 2;

    /**
     * Number of Intelligent Equalizer presets.
     * 
     * The presets are: 0=Custom=OFF, 1=WARM, 2=BRIGHT, 3=OPEN, 4=BALANCED,
     * 5=RICH, 6=FOCUSED
     */
    public static final int IEQ_PRESETS = 4;

    /** Default levels for Intelligent Equalizer. */
    public static final float[][] IEQ_PRESET_GAIN = {
            { 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
                    0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f },

            { 2.2182097f, 6.4605746f, 7.462244f, 7.639009f, 5.950213f, -0.957674f, -9.0f, -12.0f,
                    -9.0f, -3.0f, 0.0f, 1.865561f, 7.283635f, 12.5231f, 15.088074f, 17.412592f,
                    20.746881f, 24.66059f, 27.393106f, 28.099707f },

            { 12.0f, 10.865561f, 8.5966835f, 7.639009f, 5.63567f, 1.0986977f, -1.2581711f,
                    1.1576195f, 6.887953f, 11.101248f, 12.0f, 12.0f, 12.0f, 12.0f, 12.0f, 12.0f,
                    12.0f, 11.219091f, 9.657272f, 8.876364f },

            { 5.812435f, 4.3593264f, 1.4531088f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.8066387f,
                    5.1253076f, 6.342729f, 6.0481205f, 6.165964f, 6.4016514f, 6.519495f,
                    5.4589043f, 3.5144877f, 2.9252706f, 3.3377228f, 3.4555664f },

            { 11.985073f, 12.0f, 12.0f, 12.0f, 12.0f, 12.0f, 10.629873f, 7.8896213f, 4.339391f,
                    -0.6689559f, -4.498869f, -6.148677f, -7.091424f, -7.150346f, -6.6789713f,
                    -6.443284f, -6.502207f, -6.6200514f, -6.737894f, -6.9146595f },

            { 18.0f, 16.0f, 14.0f, 12.0f, 10.276342f, 6.8290286f, 3.8290286f, 1.2763429f, 0.0f,
                    0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -2.7892563f, -8.367769f, -11.157025f,
                    -11.367769f, -11.789256f },

            { -12.0f, -6.0f, 6.0f, 18.0f, 18.0f, 9.0f, 3.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
                    0.0f, 0.0f, 0.0f, 0.0f, -3.0f, -9.0f, -12.0f } };

    public static int STATUS_BAR_HEIGHT = 48;
}
