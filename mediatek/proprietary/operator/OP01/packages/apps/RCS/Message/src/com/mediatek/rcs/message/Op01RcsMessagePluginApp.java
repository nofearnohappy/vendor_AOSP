package com.mediatek.rcs.message;

import com.mediatek.rcs.message.data.Contact;
import com.mediatek.rcs.message.proxy.RcsProxyManager;
import com.mediatek.rcs.message.proxy.RcsProxyServices;

import android.app.Application;
import android.util.Log;

public class Op01RcsMessagePluginApp extends Application {

    private final String TAG = "Op01RcsMessagePluginApp";

    @Override
    public void onCreate() {
        super.onCreate();
        this.getApplicationContext();
        Log.e("mtk80999", "Op01RcsMessagePluginApp, context = " + this.getApplicationContext());
        startService(RcsProxyManager.getRcsProxyServiceIntent());
        RcsProxyManager.init(this);
        Contact.init(getApplicationContext());
    }

    @Override
    public void onTerminate() {
        stopService(RcsProxyManager.getRcsProxyServiceIntent());
        RcsProxyManager.deInit(this);
        super.onTerminate();
    }
}
