/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

/**
 * @defgroup    ds Ds
 * @details     Main API for controlling DS.
 *
 *              This API can be used by audio panel activities to control the
 *              global DS audio effect integrated on the device.
 *              The implementation uses inter-process communication to interact
 *              with the DS service running on the device.
 * @{
 */

package com.dolby.api;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

/*
 * The comment below will appear as the Detailed Description on the class
 * documentation page. The text in @details tag above appears as the Detailed
 * Description on the Modules page.
 */
/**
 * Main API for controlling DS.
 *
 * This API can be used by audio panel activities to control the
 * DS global audio effect integrated on the device.
 * The implementation uses inter-process communication to interact
 * with the DS service running on the device.
 *
 */
public class DsGlobal extends DsFocus
{
    private static final String TAG = "DsGlobal";

    /**
     * The constructor.
     *
     */
    public DsGlobal()
    {
        super.setConnectionInfo(DsAccess.ACCESS_GLOBAL);
    }

    /**
     * Request DsGlobal access right.
     *
     * @return DsConstants.DS_NO_ERROR - Success.
     *         or other warnings defined in DsAccess.java.
     */
    public int requestAccessRight()
    {
        int error = DsCommon.DS_UNKNOWN_ERROR;
        try
        {
            error = super.requestAccessRight(DsAccess.ACCESS_GLOBAL);
        }
        catch(Exception e)
        {
            Log.e(TAG, "Exception in requestAccessRight");
            e.printStackTrace();
        }
        return error;
    }

    /**
     * Abandon DsGlobal access right.
     *
     * @return DsConstants.DS_NO_ERROR - Success.
     *         or other warnings defined in DsConstants.java.
     */
    public int abandonAccessRight()
    {
        int error = DsCommon.DS_UNKNOWN_ERROR;
        try
        {
            error = super.abandonAccessRight(DsAccess.ACCESS_GLOBAL);
        }
        catch(Exception e)
        {
            Log.e(TAG, "Exception in abandonAccessRight");
            e.printStackTrace();
        }
        return error;
    }
    
    /**
     * Query if DsGlobal access right already hold by another clients.
     *
     * @return The state of the DsGlobal access right.
     */
    public int checkAccessRight()
    {
        int ret_val = -1;
        try
        {
            ret_val = super.checkAccessRight(DsAccess.ACCESS_GLOBAL);
        }
        catch(Exception e)
        {
            handleException(e, "checkAccessRight");
        }
        return ret_val;
    }
    
    /**
     * Get the access right current client can request.
     *
     * @return The type of the access right current client can request.
     */
    public int getAvailableAccessRight() 
    {
        return DsAccess.ACCESS_GLOBAL;
    }

    /**
     * Query the off type of DS audio processing.
     * There are two kinds of "off", using off profile which still contains the devices processing and totally bypass DS audio processing.
     *
     * @return The off type of DS.
     */
    public int getOffType()
    {
        int ret_val = -1;
        int[] paramInt = new int[1];
        int error = 0;
        try
        {
            error = iDs_.iGetOffType(paramInt);
        }
        catch(Exception e)
        {
            handleException(e, "getOffType");
        }
        convertErrorCodeToException(error);
        ret_val = paramInt[0];
        return ret_val;
    }
}

/**
 * @}
 */
