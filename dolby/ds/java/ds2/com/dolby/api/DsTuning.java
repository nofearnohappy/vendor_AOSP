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
 * @defgroup    ds DsTuning
 * @details     Main API for controlling DS by DsTuning access right.
 *
 *              This API can be used by media activities to control the
 *              DS audio effect integrated on the device.
 *              The implementation uses inter-process communication to interact
 *              with the DS service running on the device.
 * @{
 */

package com.dolby.api;

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
public class DsTuning extends DsBase
{
    /**
     * Request DsTuning access right.
     *
     * @return DsConstants.DS_NO_ERROR - Success.
     *         or other warnings defined in DsAccess.java.
     */
    public int requestAccessRight()
    {
        int error = DsCommon.DS_UNKNOWN_ERROR;
        try
        {
            error = super.requestAccessRight(DsAccess.ACCESS_TUNING);
        }
        catch(Exception e)
        {
            handleException(e, "requestAccessRight");
        }
        return error;
    }

    /**
     * Abandon DsTuning access right.
     *
     * @return DsConstants.DS_NO_ERROR - Success.
     *         or other warnings defined in DsAccess.java.
     */
    public int abandonAccessRight()
    {
        int error = DsCommon.DS_UNKNOWN_ERROR;
        try
        {
            error = super.abandonAccessRight(DsAccess.ACCESS_TUNING);
        }
        catch(Exception e)
        {
            handleException(e, "abandonAccessRight");
        }
        return error;
    }

    /**
     * Query if DsTuning access right already hold by another clients.
     *
     * @return The state of the DsTuning access right.
     */
    public int checkAccessRight()
    {
        int ret_val = -1;
        try
        {
            ret_val = super.checkAccessRight(DsAccess.ACCESS_TUNING);
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
        return DsAccess.ACCESS_TUNING;
    }

    /**
     * Activate an existing tuning data for a specific device to tuning database.
     *
     * @param endpointPort The endpoint port of the specific device.
     * @param productId The identification of the specific product.
     *
     * @throws DsAccessException if the tuning access right isn't available for this client.
     *
     * @return DsConstants.DS_NO_ERROR - Success.
     *         or other warnings defined in DsAccess.java.
     */
    public int activateTuning(int endpointPort, String productId) throws DsAccessException
    {
        int error = DsCommon.DS_UNKNOWN_ERROR;
        try
        {
            error = iDs_.iActivateTuning(clientHandle_, endpointPort, productId);
        }
        catch(Exception e)
        {
            handleException(e, "activateTuning");
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
     * Deactivate an existing tuning data for a specific device to tuning database.
     *
     * @param endpointPort The endpoint port of the specific device.
     * @param productId The identification of the specific product.
     *
     * @throws DsAccessException if the tuning access right isn't available for this client.
     *
     * @return DsConstants.DS_NO_ERROR - Success.
     *         or other warnings defined in DsAccess.java.
     */
    public int deactivateTuning(int endpointPort, String productId) throws DsAccessException
    {
        int error = DsCommon.DS_UNKNOWN_ERROR;
        try
        {
            error = iDs_.iDeactivateTuning(clientHandle_, endpointPort, productId);
        }
        catch(Exception e)
        {
            handleException(e, "deactivateTuning");
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
}
/**
 * @}
 */
