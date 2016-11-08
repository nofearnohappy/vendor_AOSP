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
 *              This API can be used by Dolby apps to control the
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
 * API for controlling DS.
 *
 * This API can be used by Dolby apps to control the
 * DS global audio effect integrated on the device.
 * The implementation uses inter-process communication to interact
 * with the DS service running on the device.
 *
 */
public class DsGlobalEx extends DsGlobal
{
    private static final String TAG = "DsGlobalEx";

    /**
     * Get the internal version information.
     * This method is used by Dolby app internally for debug purpose.
     */
    public String getInternalVersion()
    {
        String version = "";
        String[] paramString = new String[1];
        int error = 0;
        try
        {
            error = iDs_.iGetDapLibraryVersion(paramString);
        }
        catch(Exception e)
        {
            handleException(e, "getInternalVersion");
        }
        convertErrorCodeToException(error);
        version = "API version is " + getApiVersion() + " Service version is " + getDsVersion() + " DAP version is " + paramString[0] + ".";
        return version;
    }

    /**
     * Set the name information for the specified DS profile.
     *
     * @param profile The index of the profile whose name are being changed.
     * @param name The new name for the specified profile.
     *
     * @throws DsAccessException if there is an access right required.
     *
     * @return DsConstants.DS_NO_ERROR - Success.
     *         or other warnings defined in DsConstants.java.
     */
    public int setProfileName(int profile, DsProfileName name) throws DsAccessException
    {
        int error = DsCommon.DS_UNKNOWN_ERROR;
        try
        {
            error = iDs_.iSetProfileName(clientHandle_, profile, name);
        }
        catch(Exception e)
        {
            handleException(e, "setProfileName");
        }
        if (error == DsCommon.DS_ACCESS_LOCK_NOT_AVAILABLE)
        {
            throw (new DsAccessException("Exception: access right."));
        }
        else
        {
            convertErrorCodeToException(error);
        }
        return error;
    }

    /**
     * Get the name information for the specified DS profile.
     *
     * @param profile The index of the profile whose name are being retrieved.
     *
     * @throws DsAccessException if there is an access right required.
     *
     * @return The name of the specified profile.
     */
    public DsProfileName getProfileName(int profile)
    {
        int error = DsCommon.DS_UNKNOWN_ERROR;
        DsProfileName[] paramName = new DsProfileName[1];
        DsProfileName name = null;

        try
        {
            error = iDs_.iGetProfileName(clientHandle_, profile, paramName);
        }
        catch(Exception e)
        {
            handleException(e, "getIeqPreset");
        }
        convertErrorCodeToException(error);
        name = paramName[0];
        return name;
    }
 
    /**
     *  Register for the DS Profile Name Events.
     *
     * @param listener The object interests in the event.
     *
     */
    public void registerProfileNameEvents(IDsProfileNameEvents listener)
    {
        int error = DsCommon.DS_UNKNOWN_ERROR;
                    
        try
        {
            if (listener != null)
            {
                if(profileNameListener_ == null)
                {
                    profileNameListener_ = listener;
                }
                error = DsCommon.DS_NO_ERROR;
            }
            else
            {
                error = DsCommon.DS_INVALID_ARGUMENT;
            }
        }
        catch(Exception e)
        {
            handleException(e, "registerProfileNameEvents");
        }
        convertErrorCodeToException(error);
    }

    /**
     * unregister for the DS Profile Name Events.
     *
     */
    public void unregisterProfileNameEvents()
    {
        profileNameListener_ = null;
    }

    /**
     *  Register for the DS Legacy Client Events.
     *
     * @param listener The object interests in the event.
     *
     */
    public void registerDsLegacyClientEvents(IDsLegacyClientEvents listener)
    {
        int error = DsCommon.DS_UNKNOWN_ERROR;
                    
        try
        {
            if (listener != null)
            {
                if(legacyClientListener_ == null)
                {
                    legacyClientListener_ = listener;
                }
                error = DsCommon.DS_NO_ERROR;
            }
            else
            {
                error = DsCommon.DS_INVALID_ARGUMENT;
            }
        }
        catch(Exception e)
        {
            handleException(e, "registerDsLegacyClientEvents");
        }
        convertErrorCodeToException(error);
    }

    /**
     * unregister for the DS Legacy Client Events.
     *
     */
    public void unregisterDsLegacyClientEvents()
    {
        legacyClientListener_ = null;
    }
}

/**
 * @}
 */
