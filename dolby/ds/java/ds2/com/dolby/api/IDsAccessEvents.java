/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2013-2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

/**
 * @defgroup    dsevents DS Access Events Interface
 * @details     Defines the events that a DS client will receive. An application
 *              must implement this interface in order to receive and act on
 *              the various DS access events.
 * @{
 */

package com.dolby.api;

/*
 * The comment below will appear as the Detailed Description on the class
 * documentation page. The text in @details tag above appears as the Detailed
 * Description on the Modules page.
 */
/**
 * DS access events interface. An application must
 * implement this interface in order to receive and act on the
 * various DS access events. 
 * Only when the application gets the correct access right can it change DS parameters.
 */
public interface IDsAccessEvents
{
    /**
     * Event on connection to the service.
     */
    public abstract void onClientConnected();
    /**
     * Event on disconnection from the service.
     * This callback will not be called when a client unbinds from DsService.
     * It will only be called when the DsService crashes.
     */
    public abstract void onClientDisconnected();

    /**
     * Event indicating the access right of current client is released by DsService.
     * When an app loses access by another app request an access and DsService grants the access right, 
     * this callback will be called to let the original app know.
     *
     * @param app The package name of the app which makes Ds service force released the access right of current client.
     * @param type The type of the access right.
     */
    public abstract void onAccessForceReleased(String app, int type);

    /**
     * Event indicating the access right is available again.
     * If an app's access is forced release by DsService, when the access is available again, 
     * DsService will call this callback to let the app know.
     */
    public abstract void onAccessAvailable();

    /**
     * Event indicating that another client wants to get access right from current client.
     * Current client can decide whether to give the access to ohrt clients.
     * If it is oK the relinquish the access, it should return true, otherwise return false.
     * This callback is designed as a synchronize call. The implementation must be quick and non blocking. 
     *
     * @param app The package name of the app which wants to get access right from current client.
     * @param type The type of the access right.
     *
     * @return true if the app granting DsFocus access right agree, else false.
     */
    public abstract boolean onAccessRequested(String app, int type);

    /// add more events here
}

/**
 * @}
 */
