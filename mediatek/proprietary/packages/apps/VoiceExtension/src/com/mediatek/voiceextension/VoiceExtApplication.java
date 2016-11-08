package com.mediatek.voiceextension;

import android.app.Application;
import android.util.Log;

import com.mediatek.voiceextension.cfg.ConfigurationManager;
import com.mediatek.voiceextension.common.CommonManager;

/**
 * VoiceExtension Application, be instantiated when the process
 * application/package is created.
 *
 */
public class VoiceExtApplication extends Application {

    private VoiceExtManagerService mService;

    @Override
    public void onCreate() {

        super.onCreate();
        ConfigurationManager.getInstance().init(getApplicationContext());
        if (CommonManager.getInstance().getSwipInteractionLocked()
                .isSwipReady()) {
            mService = new VoiceExtManagerService();
        } else {
            Log.e(CommonManager.TAG, "Service start fail !!!!");
        }
    }


}
