/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2011-2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

package com.dolby.api;

/**
 * The callback interface adopted by the global Service to send synchronous
 * Ds effect on/off/configuraton change notifications back to its
 * clients.  Note that this is a one-way interface so the service does not
 * block waiting for the client.
 */
interface IDsCallbacks
{
    void onDsOn(boolean on);

    void onDsSuspended(boolean isSuspended);
    
    void onProfileSelected(int profile);

    void onProfileSettingsChanged(int profile);

    void onVisualizerUpdated(in float[] gains, in float[] excitations);

    void onVisualizerSuspended(boolean isSuspended);

    void onAccessForceReleased(String app, int type);

    void onAccessAvailable();

    boolean onAccessRequested(String app, int type);
    
    void onProfileNameChanged(int profile, String name);
}
