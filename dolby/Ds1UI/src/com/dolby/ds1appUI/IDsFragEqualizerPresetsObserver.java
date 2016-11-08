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

package com.dolby.ds1appUI;

import android.dolby.DsClientSettings;
import android.view.View;

public interface IDsFragEqualizerPresetsObserver {

    // Temporary.
    public abstract void onEqualizerEditStart();

    public abstract void setUserProfilePopulated();

    public abstract void onProfileSettingsChanged(int profile,
            DsClientSettings settings);

    public abstract void displayTooltip(View pointToView, int idTitle,
            int idText);

    public abstract void onDsClientUseChanged(final boolean on);

    public abstract void chooseProfile(int profile);

    public abstract void equalizerPresetsAreAlive();

    public abstract void resetEqUserGains();
}