package com.mediatek.effect;

import android.media.Image;
import android.util.Log;



/**
 * @hide
 * Effects are high-performance transformations that can be applied to image frames. Typical
 * frames can be images loaded from disk, or frames from the camera or other video streams.
 *
 */
public class FaceBeautyEffect extends Effect {
    private static final String TAG = "FaceBeautyEffect";

    FaceBeautyEffect() {
        native_setup(this);
    }

    /**
     * Creates a Effect object.
     * @return a new Effect object
     */
    public static Effect createEffect() {
        return new FaceBeautyEffect();
    }

    /**
     * @hide
     * Applies an effect to the target input image.
     */
    @Override
    public void apply(Image srcImage, Image targetImage) {
        Log.i(TAG , "apply(), srcImage:" + srcImage + ", targetImage:" + targetImage);
        native_apply(srcImage, targetImage);
    }

    /**
     * @hide
     * Gets the effect name.
     */
    @Override
    public String getName() {
        String effectName = "facebeautyeffect";
        Log.i(TAG , "getName(), effectName:" + effectName);
        return effectName;
    }

    /**
     * @hide
     * Sets up a filter parameter. Consult the effect documentation for a list
     * of supported parameter keys for each effect.
     */
    @Override
    public void setParameter(String parameterKey, Object value) {
        Log.i(TAG , "setParameter(), parameterKey:" + parameterKey + ", value:" + value.toString());
        native_setParameter(parameterKey, value);
    }

    /**
      * @hide
      * Releases an effect.
      *
      * <p>Releases the effect and any resources associated with it. You may call this if you need to
      * make sure acquired resources are no longer held by the effect. Releasing an effect makes it
      * invalid for reuse.</p>
      *
      */
    @Override
    public void release() {
        native_release();
    }

    private native void native_setup(FaceBeautyEffect object);
    private native void native_setParameter(String parameterKey, Object value);
    private native void native_apply(Image srcImage, Image targetImage);
    private native void native_release();
}
