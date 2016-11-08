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

package com.dolby.instoredemoapp; 

import android.graphics.Color;

public class ConstValue {
	/*
	 * UPDATE_TEXT is used to enable/disable the text updateing feature
	 */
	public static final boolean UPDATE_TEXT = false;
	
	public static final String APINFO_FILE_NAME = "instore_demo_autopilot.xml";
	
	public static final int AP_MSG_ID = 2012;
	public static final int UPDATE_TXT_MSG_ID = 2013;
	public static final int START_LOOP_MEDIA_PLAYBACK = 2014;
	public static final int START_DEMO_MEDIA_PLAYBACK = 2015;
	public static final int DS1_SERVICE_CONNECTED = 2016;
	public static final int DS1_INSTOREDEMO_QUIT = 2017;
	
	public static final int PROFILE_MOVIE = 0;
	public static final int PROFILE_MUSIC = 1;
	public static final int PROFILE_GAME =  2;
	public static final int PROFILE_VOICE = 3;
	
	public static final int IEQ_OFF = 0;
	public static final int IEQ_OPEN = 1;
	public static final int IEQ_RICH = 2;
	public static final int IEQ_FOCUSED = 3;
	/*
	 * Note:The following ieq have not been implemented in ds1
	 */
	public static final int IEQ_WARM = 4;
	public static final int IEQ_BRIGHT = 5;
	public static final int IEQ_BALANCED = 6;
	
	public static final int TEXT_COLOR_WHITE = Color.WHITE;
	public static final int TEXT_COLOR_BLACK = Color.BLACK;
	public static final int TEXT_COLOR_RED = Color.RED;
	public static final int TEXT_COLOR_YELLOW = Color.YELLOW;
	public static final int TEXT_COLOR_BLUE = Color.BLUE;
	
}
