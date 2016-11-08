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

package android.dolby;

import android.util.Log;

/**
 * DsLogSettings class provides different log level controlling.
 */
public class DsLog
{
    /**
     *  Different log Levels.
     */
    public static final int LOG_LEVEL_0 = 0;
    public static final int LOG_LEVEL_1 = 1;
    public static final int LOG_LEVEL_2 = 2;
    public static final int LOG_LEVEL_3 = 3;

    /**
     * Default log level.
     * Change this value if we need different log.
     */
    public static final int DEFAULT_LOG_LEVEL = LOG_LEVEL_1;

    /**
     * Level 1 log wrapper which print the basic logs.
     */
    public static void log1 (String tag, String content)
    {
        if (DEFAULT_LOG_LEVEL >= LOG_LEVEL_1)
        {
            Log.i(tag, content);
        }
    }

    /**
     * Level 2 log wrapper which print the debug logs.
     */
    public static void log2 (String tag, String content)
    {
        if (DEFAULT_LOG_LEVEL >= LOG_LEVEL_2)
        {
            Log.i(tag, content);
        }
    }

    /**
     * Level 3 log wrapper which print all logs.
     */
    public static void log3 (String tag, String content)
    {
        if (DEFAULT_LOG_LEVEL == LOG_LEVEL_3)
        {
            Log.i(tag, content);
        }
    }
}
