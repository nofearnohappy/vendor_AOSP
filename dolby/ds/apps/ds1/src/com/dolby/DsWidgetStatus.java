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

package com.dolby;

public class DsWidgetStatus {

    private static DsWidgetStatus instance_ = null;

    /**
     * The ON/OFF status of the effect.
     *
     * @internal
     */
    private boolean on_;

    /**
     * The Modified status.
     *
     * @internal
     */
    private boolean modified_;

    /**
     * Current selected profile of the effect.
     *
     * @internal
     */
    private int profile_;

    /**
     * Current selected profile string of the effect.
     *
     * @internal
     */
    private String profileName_;

    // add other status information

    private DsWidgetStatus() {
        on_ = false;
        profile_ = 0;
        profileName_ = "";
        modified_ = false;
    }

    public static DsWidgetStatus getInstance() {
        if (instance_ == null) {
            instance_ = new DsWidgetStatus();
        }
        return instance_;
    }

    /**
     * Store the ON/OFF status.
     *
     * @param on
     *            The latest status of the effect.
     */
    public void setOn(boolean on) {
        on_ = on;
    }

    /**
     * Store the profile.
     *
     * @param profile
     *            The latest profile of the effect.
     */
    public void setProfile(int profile) {
        profile_ = profile;
    }

    /**
     * Store profile modified state.
     *
     * @param mod
     *            The latest profile modified state.
     */
    public void setModified(boolean mod) {
        modified_ = mod;
    }

    /**
     * Store the profile name.
     *
     * @param profileString
     *            The latest profile string of the effect.
     */
    public void setProfileName(String profileName) {
        profileName_ = profileName;
    }

    /**
     * Get the stored ON/OFF status.
     *
     * @return The latest status of the effect.
     */
    public boolean getOn() {
        return on_;
    }

    /**
     * Get the stored selected profile.
     *
     * @return The latest profile of the effect.
     */
    public int getProfile() {
        return profile_;
    }

    /**
     * Get the stored profile modified state.
     *
     * @return The latest profile state of the effect.
     */
    public boolean getModified() {
        return modified_;
    }

    /**
     * Get the stored selected profile string.
     *
     * @return The latest profile string of the effect.
     */
    public String getProfileName() {
        return profileName_;
    }
}
