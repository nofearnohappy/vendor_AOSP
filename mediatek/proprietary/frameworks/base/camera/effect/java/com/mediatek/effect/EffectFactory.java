package com.mediatek.effect;

import android.util.Log;


/**
 * The EffectFactory class defines the list of enhancements effects available, and provides functionality to inspect
 * and instantiate them. The face beautifier portrait enhancement is only enhancement effect available.
 * Before creating a specific Effect object, the application should confirm that the enhancement effect is
 * supported on the platform by calling isEffectSupported(String).
 *
 * <p>Here is an example of the prerequisite declaration:</p>
 * <pre class="prettyprint">
 *
 *  public class MainActivity extends Activity {
 *
 *     public void onCreate(Bundle savedInstanceState) {
 *         super.onCreate(savedInstanceState);
 *         setContentView(R.layout.activity_main);
 *         //Acquire image from ImageReader
 *         ImageReader imageReader;
 *         ImageReader.newInstance(previewSize.width, previewSize.height, ImageFormat.RGB_565, 2);
 *         Image srcImage = imageReader.acquireNextImage();
 *         Image targetImage = imageReader.acquireNextImage();;
 *         //
 *         EffectFactory sEffectFactory = EffectFactory.createEffectFactory();
 *         Effect faceBeautyEffect;
 *         if(sEffectFactory.isEffectSupported(EffectFactory.EFFECTS_FACEBEAUTIFIER))
 *         {
 *             faceBeautyEffect = (FaceBeautyEffect)sEffectFactory.createEffect(EffectFactory.EFFECTS_FACEBEAUTIFIER);
 *             // setting effect parameters here ..
 *             faceBeautyEffect.setParameter("SMOOTH_LEVEL", 3);
 *             faceBeautyEffect.setParameter("SKIN_COLOR_LEVEL", 2);
 *             faceBeautyEffect.setParameter("SLIM_FACE_LEVEL", 1);
 *             faceBeautyEffect.setParameter("ENLARGE_EYE_LEVEL", -1);  *
 *             //
 *             faceBeautyEffect.apply(mSrcImage, mTargetImage);
 *             faceBeautyEffect.release();
 *         }
 *     }
 *  }
 * </pre>
 */

public class EffectFactory {
    private static final String TAG = "EffectFactory";

    private static EffectFactory sEffectFactory;

    /**
     * <p> Applies portrait enhancements to the face on source image. </p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * <tr><td><code> SMOOTH_LEVEL </code></td>
     *     <td> Sets the level of skin smoothing to apply to the image.
     *          Supply before calling apply() for the first time.</td>
     *     <td> Integer, range: -4 ~ 4. </td>
     * </tr>
     * <tr><td><code> SKIN_COLOR_LEVEL </code></td>
     *     <td> Sets the level of skin tone adjustment to apply to image.
     *          Supply before calling apply() for the first time.</td>
     *     <td> Integer, range: -4 ~ 4. </td>
     * </tr>
     * <tr><td><code> SLIM_FACE_LEVEL </code></td>
     *     <td> Sets the level of face slimming to apply to the image.
     *          Supply before calling apply() for the first time.</td>
     *     <td> Integer, range: -4 ~ 4. </td>
     * </tr>
     * <tr><td><code> ENLARGE_EYE_LEVEL </code></td>
     *     <td> Sets the level of eye enlargement to apply to the image.
     *          Supply before calling apply() for the first time.</td>
     *     <td> Integer, range: -4 ~ 4. </td>
     * </tr>
     * <tr><td><code> EFFECT_MODE </code></td>
     *     <td> Sets whether the transformation is to be applied to a still image or video stream
     *          Supply before calling apply() for the first time.</td>
     *     <td> Integer, 0:Still mode,  1: Video mode </td>
     * </tr>
     * </table>
    */
    public static final String EFFECTS_FACEBEAUTIFIER = "FaceBeautyEffect";

    EffectFactory() {
    }

    /**
     * Creates an EffectFactory instance.
     * @return a static EffectFactory instance
     */
    public static EffectFactory createEffectFactory() {
        if (sEffectFactory == null) {
            sEffectFactory = new EffectFactory();
        }
        return sEffectFactory;
    }
    /**
     * Instantiates a new Effect object for the given enhancement effect name.
     */
    public Effect createEffect(String effectName) {
        Log.i(TAG , "createEffect(), effectName:" + effectName);
        if (effectName.equals(EFFECTS_FACEBEAUTIFIER)) {
            return FaceBeautyEffect.createEffect();
        }
        return null;
    }

    /**
     * Checks if specific enhancement effect is supported on the platform.
     */
    public boolean isEffectSupported(String effectName) {
        Log.i(TAG , "isEffectSupporteds(), effectName:" + effectName);
        return native_isEffectSupporteds(effectName)/* false */;
    }

    static {
        System.loadLibrary("jni_effects");
    }

    private native boolean native_isEffectSupporteds(String effectName);
}
