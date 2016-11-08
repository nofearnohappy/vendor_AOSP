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
 * @defgroup    dsvisualizerevents DS Visualizer Events Interface
 * @details     Defines the visualizer events that a DS client may receive. An
 *              application must implement this interface in order to receive
 *              and act on the event.
 * @{
 */

package com.dolby.api;

/*
 * The comment below will appear as the Detailed Description on the class
 * documentation page. The text in @details tag above appears as the Detailed
 * Description on the Modules page.
 */
/**
 * DS visualizer events interface. An application must
 * implement this interface in order to receive and act on the event.
 */
public interface IDsVisualizerEvents
{
    /**
     * Event indicating the visualizer data has changed.
     *
     * @param excitations The array of excitation values.
     * @param gains The array of gain values.
     */
    public abstract void onVisualizerUpdate(float[] excitations, float[] gains);

    /**
     * Event indicating the visualizer data has suspended.
     *
     * @param isSuspended True if the visualizer data is suspended, false otherwise.
     */
    public abstract void onVisualizerSuspended(boolean isSuspended);
}

/**
 * @}
 */
