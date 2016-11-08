package com.mediatek.hotknot.common.ui;

import android.app.Application;
import android.content.Context;

public class HotKnotCommonUi extends Application {

    static final String TAG = "HotKnotCommonUi";
    static Context sApplicationContext;

    @Override
    public void onCreate() {
        super.onCreate();
        sApplicationContext = this;
    }

}