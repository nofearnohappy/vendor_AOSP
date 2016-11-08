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

public class DolbyWidgetExtraLargeProvider extends AbstractDolbyWidgetProvider
        implements IDolbyWidgetUpdateStatus {

    static {
        addWidgetExtraLargeClass(DolbyWidgetExtraLargeProvider.class);
    }

    private static DolbyWidgetExtraLargeProvider sInstance;

    public static synchronized DolbyWidgetExtraLargeProvider getInstance() {
        if (sInstance == null) {
            sInstance = new DolbyWidgetExtraLargeProvider();
        }
        return sInstance;
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        // Send out intent to notify the service
        sendInitIntent(this.getClass().getName());
        Log.d(Tag.WIDGET, "ExtraLargeWidget.sendout init intent");
    }

    public void notifyStatusUpdate(DsService service,
            DsWidgetStatus status) {
        if (hasInstances(service)) {
            mDsOn = status.getOn();
            mSelectedProfile = status.getProfile();
            updateWidgets(DolbyWidgetExtraLargeProvider.class, true, true);
        }
    }
}
