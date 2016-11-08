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

import android.content.Context;
import android.util.Log;

public class DolbyWidgetSmallProvider extends AbstractDolbyWidgetProvider
        implements IDolbyWidgetUpdateStatus {

    static {
        addWidgetSmallClass(DolbyWidgetSmallProvider.class);
    }

    private static DolbyWidgetSmallProvider sInstance;

    static synchronized DolbyWidgetSmallProvider getInstance() {
        if (sInstance == null) {
            sInstance = new DolbyWidgetSmallProvider();
        }
        return sInstance;
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        // Send out intent to notify the service
        sendInitIntent(this.getClass().getName());
        Log.d(Tag.WIDGET, "SmallWidget.sendout init intent");
    }

    public void notifyStatusUpdate(DsService service, DsWidgetStatus status) {
        if (hasInstances(service)) {
            mDsOn = status.getOn();
            mSelectedProfile = status.getProfile();
            mModified = status.getModified();
            if (mModified) {
                mSelectedProfileName = status.getProfileName();
            }
            updateWidgets(DolbyWidgetSmallProvider.class, false, false);
        }
    }
}
