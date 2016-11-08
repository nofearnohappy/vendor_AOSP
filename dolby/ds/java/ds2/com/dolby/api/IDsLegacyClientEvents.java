/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C)  2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

/**
 * @defgroup    dslegacyclientevents DS Legacy Client Events Interface
 * @details     Defines the events that a DS client may receive. An application
 *              must implement this interface in order to receive and act on
 *              the various DS events.
 * @{
 */

package com.dolby.api;

/*
 * The comment below will appear as the Detailed Description on the class
 * documentation page. The text in @details tag above appears as the Detailed
 * Description on the Modules page.
 */
 /*
  * DS Legacy Client events interface.
  */
public interface IDsLegacyClientEvents
{
    /**
     * Event indicating that a DS1 client bound to DsService wants to set DS state or profile.
     * The DS2 client can decide whether to give the access to the DS1 clients.
     * If it is oK to relinquish the access, it should return true, otherwise return false.
     * This callback is designed as a synchronize call. The implementation must be quick and non blocking. 
     *
     * @return true if the app granting DsGlobal access right agree, else false.
     */
    public abstract boolean onLegacyClientSetting();
}

/**
 * @}
 */

