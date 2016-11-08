/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

/**
 * @defgroup    DsTuningConstants DS Tuning Constants
 * @details     This class encapsulates the constants about tuning api that are usable
 *              by a client application and the internal service.
 * @{
 */
package com.dolby.api;

/*
 * The comment below will appear as the Detailed Description on the class
 * documentation page. The text in @details tag above appears as the Detailed
 * Description on the Modules page.
 */
/**
 * This class encapsulates the constants about tuning api that are usable
 * by a client application and the internal service.
 */
public class DsTuningConstants
{
    /** 
     * The physical port that is used to connect audio rendering devices.
     */
    public static final int DS_ENDPOINT_PORT_INTERNALSPEAKER = 0;
    public static final int DS_ENDPOINT_PORT_HEADPHONE = 1;
    public static final int DS_ENDPOINT_PORT_HEADSET = 2;
    public static final int DS_ENDPOINT_PORT_HDMI = 3;
    public static final int DS_ENDPOINT_PORT_BLUETOOTH = 4;
    public static final int DS_ENDPOINT_PORT_USB = 5;
    public static final int DS_ENDPOINT_PORT_OTHER = 6;

    /**
     * The actual type of device connected to the endpoint.
     */
    public static final int DS_ENDPOINT_TYPE_SPEAKER = 0;
    public static final int DS_ENDPOINT_TYPE_HEADPHONE = 1;
    public static final int DS_ENDPOINT_TYPE_PASSTHROUGH = 2;
    public static final int DS_ENDPOINT_TYPE_OTHER = 3;

    //Todo: add more constants here
}
/**
 * @}
 */

