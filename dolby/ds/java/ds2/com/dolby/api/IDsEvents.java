/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2013 - 2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

/**
 * @defgroup    dsevents DS Events Interface
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
 * DS events interface. An application must
 * implement this interface in order to receive and act on the
 * various DS events.
 */
public interface IDsEvents
{
    /**
     * Event indicating on/off state has changed.
     * The receiver may update the UI display with the specified on/off state.
     *
     * @param on The on/off state.
     */
    public abstract void onDsOn(boolean on);
 
    /**
     * Event indicating the selected profile has changed.
     * The receiver may update the UI display with the specified profile index.
     *
     * @param profile The index of the selected profile.
     */
    public abstract void onProfileSelected(int profile);

    /**
     * Event indicating that profile settings have changed.
     *
     * @param profile The index of the profile whose settings have changed.
     */
    public abstract void onProfileSettingsChanged(int profile);

    /**
     * Event indicating that global DS effect is suspended by the system.
     *
     * @param isSuspended The state of global DS effect.
     */
    public abstract void onDsSuspended(boolean isSuspended);
    /// Please add more events here
}

/**
 * @}
 */
