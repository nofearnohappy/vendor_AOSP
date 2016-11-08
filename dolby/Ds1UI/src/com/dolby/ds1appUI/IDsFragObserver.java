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

import android.dolby.DsClient;

public interface IDsFragObserver {

    // Method to get a reference to the DsClient to use.
    // Ideally, this reference should come in as a parameter.
    // CONSIDER THIS TEMPORARY FOR THE MOMENT. This might change.
    public abstract DsClient getDsClient();

    // Method to know if the DsClient we know about (see above)
    // has connected to the the service.
    // CONSIDER THIS TEMPORARY FOR THE MOMENT. This might change.
    public abstract boolean isDolbyClientConnected();

    // Method to let the observed object whether to use the DS API
    // or not while handling UI event.
    // CONSIDER THIS TEMPORARY FOR THE MOMENT. This might change.
    public abstract boolean useDsApiOnUiEvent();

    // Method used to notify there was
    // an error while using Dolby's API.
    public abstract void onDsApiError();

    public abstract void exitActivity();
}
