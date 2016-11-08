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

/**
 * @defgroup    dsclientevents DS Client Events Interface
 * @details     Defines the events that a DS client may receive. An application
 *              must implement this interface in order to receive and act on
 *              the various DS events.
 * @{
 */

package android.dolby;

/*
 * The comment below will appear as the Detailed Description on the class
 * documentation page. The text in @details tag above appears as the Detailed
 * Description on the Modules page.
 */
/**
 * DS client events interface. An application must
 * implement this interface in order to receive and act on the
 * various DS events.
 */
public interface IDsClientEvents
{
    /**
     * Event on connection to the service.
     */
    public abstract void onClientConnected();
    /**
     * Event on disconnection from the service.
     */
    public abstract void onClientDisconnected();

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
     * The receiver should retrieve the profile settings for the specified profile index
     * by calling getProfileSettings() and getIeqPreset() methods in order to update the UI display.
     *
     * @param profile The index of the profile whose settings have changed.
     */
    public abstract void onProfileSettingsChanged(int profile);

    /**
     * Event indicating that a profile name has changed.
     * The receiver may update the UI display with the specified profile name.
     *
     * @param profile The index of the profile whose name has changed.
     * @param name The new name of the specified profile.
     */
    public abstract void onProfileNameChanged(int profile, String name);

    /**
     * Event indicating that Geq band gains and/or the Ieq preset has changed for the specified profile.
     * The receiver should retrieve the Geq band gains for the specified profile and Ieq preset,
     * and should confirm which Ieq preset is currently selected in the profile
     * by calling getIeqPreset() and getGeq() methods in order to update the UI display.
     *
     * @param profile The index of the profile.
     * @param preset The index of the specified Ieq preset whose settings have changed.
     */
    public abstract void onEqSettingsChanged(int profile, int preset);

    /// add more events here
}

/**
 * @}
 */
