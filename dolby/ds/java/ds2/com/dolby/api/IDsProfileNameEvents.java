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
 * @defgroup    dsprofilenameevents DS Profile Name Events Interface
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
/**
 * DS profile name events interface. An application must
 * implement this interface in order to receive and act on the
 * various DS events.
 */
public interface IDsProfileNameEvents
{
    /**
     * Event indicating that a profile name has changed.
     * The receiver may update the UI display with the specified profile name.
     *
     * @param profile The index of the profile whose name has changed.
     * @param name The new name of the specified profile.
     */
    public abstract void onProfileNameChanged(int profile, String name);
}

/**
 * @}
 */

