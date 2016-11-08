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
 * @defgroup    dsapparamevents DS Audio Processing Parameter change Events Interface
 * @details     Defines the DS audio processing parameter change events that
 *              a DS client may receive. An application must implement this
 *              interface in order to receive the DS audio processing parameter
 *              change events.
 * @{
 */

package android.dolby;

/*
 * The comment below will appear as the Detailed Description on the class
 * documentation page. The text in @details tag above appears as the Detailed
 * Description on the Modules page.
 */
/**
 * DS audio processing parameter change events interface. An application must
 * implement this interface in order to receive the DS audio processing
 * parameter change events triggered by the expert API.
 */
public interface IDsApParamEvents
{
    public abstract void onDsApParamChange(int profile, String paramName);

    /// add more events here
}

/**
 * @}
 */
