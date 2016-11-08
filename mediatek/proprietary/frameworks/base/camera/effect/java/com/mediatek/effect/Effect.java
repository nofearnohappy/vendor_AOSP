package com.mediatek.effect;
import android.media.Image;



/**
 * High-performance enhancement effects that can be applied to image frames. Typical
 * frames can be images loaded from disk, frames from the camera or other video streams.
 *
 */
public abstract class Effect {
    private static final String TAG = "Effect";

    /**
     * @hide
     */
    protected Effect() {
    }

    /**
     * Some enhancement effects may issue callbacks to inform the host of updates to the enhancement effect state.
     * This is the listener interface for receiving those callbacks.
     */
    public interface EffectUpdateListener {
        /**
         * Called when the enhancement effect state updates.
         * @param effect
         *     The enhancement effect that has updated.
         * @param info
         *     A value that gives more information about the update. See the documentation
         *     for more details on this object's contents.
         */
        public void onEffectUpdateds(Effect effect, Object info);
    }


    /**
     * Applies an enhancement effect to the target input image.
     */
    public abstract void apply(Image srcImage, Image targetImage);

    /**
     * Gets the enhancement effect name.
     */
    public abstract String getName();

    /**
     * Sets up filter parameter. Consult the EffectFactory Constants for a list
     * of supported parameter keys for each enhancement effect.
     *
     */
    public abstract void setParameter(String parameterKey, Object value);

    /**
     * Sets up an Effect listener.
     * @param listener The listener receive Effect
     */
    public void setUpdateListener(EffectUpdateListener listener) {
        native_setUpdateListener(listener);
    }


    /**
      * Releases an Effect object.
      *
      * <p>Releases the Effect object and any resources associated with it. You may call this if you need to
      * make sure the resources acquired  are no longer held by the Effect. Releasing an Effect makes it
      * unavailable for reuse.</p>
      *
      */
    public abstract void release();

    /**
     * @hide
     */
    protected native void native_setUpdateListener(EffectUpdateListener listener);

}
