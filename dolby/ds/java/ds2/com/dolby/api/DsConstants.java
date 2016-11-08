/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2011-2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

/**
 * @defgroup    dsconstants DS Constants
 * @details     This class encapsulates the constants that are usable
 *              by a client application and the internal service.
 * @{
 */
package com.dolby.api;

/*
 * The comment below will appear as the Detailed Description on the class
 * documentation page. The text in @details tag above appears as the Detailed
 * Description on the Modules page.
 */
/**
 * This class encapsulates the constants that are usable
 * by a client application and the internal service.
 */
public class DsConstants
{
    /**
     * Index of movie profile.
     */
    public static final int PROFILE_MOVIE = 0;
    /**
     * Index of music profile.
     */
    public static final int PROFILE_MUSIC = 1;
    /**
     * Index of game profile.
     */
    public static final int PROFILE_GAME = 2;
    /**
     * Index of voice profile.
     */
    public static final int PROFILE_VOICE = 3;
    /**
     * Index of custom 1 profile.
     */
    public static final int PROFLIE_CUSTOM_1 = 4;
     /**
     * Index of custom 2 profile.
     */
    public static final int PROFLIE_CUSTOM_2 = 5;

    /**
     * Total profile count.
     */
    public static final int PROFILES_NUMBER = 6;

    /**
     * Index of first profile.
     */
    public static final int PROFILE_INDEX_MIN = PROFILE_MOVIE;
    /**
     * Index of last profile.
     */
    public static final int PROFILE_INDEX_MAX = PROFLIE_CUSTOM_2;

    /**
     * First index of the profile which can be customed.
     */
    public static final int PROFILE_INDEX_FIRST_CUSTOM = PROFLIE_CUSTOM_1;

    /**
     * Index of off preset.
     */
    public static final int IEQ_PRESET_OFF = 0;
    /**
     * Index of open preset.
     */
    public static final int IEQ_PRESET_OPEN = 1;
    /**
     * Index of rich preset.
     */
    public static final int IEQ_PRESET_RICH = 2;
    /**
     * Index of focused preset.
     */
    public static final int IEQ_PRESET_FOCUSED = 3;

    /**
     * Total Ieq preset count.
     */
    public static final int IEQ_PRESETS_NUMBER = 4;

    /**
     * Index of first Ieq preset.
     */
    public static final int IEQ_PRESET_INDEX_MIN = IEQ_PRESET_OFF;
    /**
     * Index of last Ieq preset.
     */
    public static final int IEQ_PRESET_INDEX_MAX = IEQ_PRESET_FOCUSED;

    /**
     * The upper/lower bound of Geq band gains.
     */
    public static final float[] GEQ_BAND_GAIN_RANGE = {-36.0f, 36.0f};

    /** 
     * Error code: The request was successful.
     */
    public static final int DS_NO_ERROR = 0;

    /** 
     * Error code: The request could not be processed because the effect is
     * currently unavailable.
     */
    public static final int DS_REQUEST_FAILED_EFFECT_SUSPENDED = 1;
    
    /** 
     * States of Ds audio effect.
     */
    public static final int DS_STATE_OFF = 0;
    public static final int DS_STATE_ON = 1;
    public static final int DS_STATE_SUSPENDED = 2;

    /**
     * Off type.
     */
    public static final int DS_OFF_BYPASSED_TYPE = 0;
    public static final int DS_OFF_PARAMETERIZED_TYPE = 1;

    // Please add other contants here
    public static final String DSSERVICE_START_ACTION = "DSSERVICE_START_ACTION"; 
}
/**
 * @}
 */
