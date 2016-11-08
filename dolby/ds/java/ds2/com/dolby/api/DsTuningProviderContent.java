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

import android.net.Uri;

/*
 * DsTuningProviderContent.java
 *
 * Defines constants for accessing the content provider defined in TuningProvider.
 */

public class DsTuningProviderContent
{
    /**
     * The provider's authority.
     *
     */
    public static final String AUTHORITY = "com.dolby.dax.api.TuningProvider";
    
    /**
     * The name of table.
     *
     */
    public static final String TNAME = "tunings";

   /**
    * The TuningProvider content URI
    */
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + TNAME);

   //Todo: add more constants here
}
