package com.mediatek.compatibility.effect;

import android.util.Log;

import com.mediatek.effect.EffectFactory;

public class EffectSupport {

    public static final String TAG = "EffectSupport";
    public static boolean isEffectFeatureAvaliable() {
        try {
            EffectFactory effectFactory = EffectFactory.createEffectFactory();
            return true;
        } catch (Throwable e) {
            Log.e(TAG, "Effect feature is not available");
            return false;
        }
    }
}
