package com.mediatek.camera.ext;

import android.content.Context;
import android.content.ContextWrapper;
import android.media.MediaRecorder;

import java.util.ArrayList;
import java.util.Iterator;


public class DefaultCameraFeatureExt extends ContextWrapper implements ICameraFeatureExt {
    private static final String KEY_SCENE_MODE = "pref_camera_scenemode_key";
    protected Context mContext;
    public DefaultCameraFeatureExt(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public int getQuickViewDisplayDuration() {
        return 0;
    }

    @Override
    public void updateSettingItem(String key, ArrayList<CharSequence> entries,
            ArrayList<CharSequence> entryValues) {
        if (KEY_SCENE_MODE.equals(key)) {
            int index = 0;
            for (Iterator<CharSequence> iter = entryValues.iterator(); iter.hasNext(); ) {
                CharSequence value = String.valueOf(iter.next());
                if ("normal".equals(value)) {
                    iter.remove();
                    entries.remove(index);
                    break;
                }
                index++;
            }
        }
    }

    @Override
    public void configRecorder(MediaRecorder recorder) {
    }
}
