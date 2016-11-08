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

package com.dolby.api;

/**
 * This class encapsulates the constants related with access right that are usable
 * by a client application and the internal service.
 */
public class DsAccess
{
    /** 
     * Access right. 
     */
    public static final int ACCESS_NONE = 0x00;
    public static final int ACCESS_FOCUS = 0x01;
    public static final int ACCESS_GLOBAL = 0x02;
    public static final int ACCESS_TUNING = 0x04;

    /** 
     * Error code: The request could not be processed because the specific access
     * is granted by the application.
     */
    public static final int ERROR_ACCESS_AREADY_GRANTED = -1;

    /** 
     * Error code: The request could not be processed because the application
     * doesn't permit to request the specific access right.
     */
    public static final int ERROR_ACCESS_NOT_PERMITTED = -2;

    /** 
     * Error code: The request could not be processed because the application
     * does not grant the audio focus now.
     */
    public static final int ERROR_ACCESS_NO_AUDIOFOCUS = -3;

    /** 
     * Error code: The request could not be processed because the application which has
     * granted some access does not agree.
     */
    public static final int ERROR_ACCESS_NOT_AGREED = -4;

    /**
     * Return codes: the specific access right is not taken by any application.
     */
    public static final int NONE_APP_GRANTED = 0;

    /**
     * Return codes: the specific access right is taken by other application.
     */
    public static final int OTHER_APP_GRANTED = 1; 

    /**
     * Return codes: the specific access right is not taken by this application.
     */
    public static final int THIS_APP_GRANTED = 2;

}
