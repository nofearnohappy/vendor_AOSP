package com.mediatek.camera.ext;

import android.media.MediaRecorder;

import java.util.ArrayList;

public interface ICameraFeatureExt {

    /**
     * Get the duration of capture quick view.
     * @return The duration of capture quick view, unit is millisecond, nagtive or zero
     *     means that it don't need to delay start preview after capture.
     * @internal
     */
    int getQuickViewDisplayDuration();

    /**
     * Update setting item, such as WB, Scene.
     *
     * @param key The key used to indicate the setting item which needs to be updated.
     * @param entries The displayed values of current setting item.
     * @param entryValues The logical values of current setting item.
     * @internal
     */
    void updateSettingItem(String key, ArrayList<CharSequence> entries,
            ArrayList<CharSequence> entryValues);

    /**
     * Configure video recorder.
     *
     * @param recorder The recorder which needs to be configured.
     * @internal
     */
    void configRecorder(MediaRecorder recorder);
}
