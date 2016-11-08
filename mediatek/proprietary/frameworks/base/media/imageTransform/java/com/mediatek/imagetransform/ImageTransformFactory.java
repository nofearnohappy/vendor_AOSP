package com.mediatek.imagetransform;
import android.graphics.Rect;
import android.media.Image;
import android.util.Log;

/**
 * Creates ImageTransform objects from various sources, including files, streams, and byte-arrays.
 *
 *
 * <p>Here is an example of prerequisite declaration:</p>
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
 *         ImageTransformFactory sImageTransformFactory = ImageTransformFactory.createImageTransformFactory();
 *         //setting the image transform options here ..
 *         ImageTransformFactory.Options options = sImageTransformFactory.new Options();
 *         mOptions.cropRoi = new Rect(0, 0, 100, 100);
 *         mOptions.flip = ImageTransformFactory.FLIP_H;
 *         mOptions.rotation = ImageTransformFactory.ROT_180;
 *         mOptions.dither = true;
 *         mOptions.sharpness = 3;
 *         //
 *         sImageTransformFactory.applyTransform(mSrcImage, mTargetImage, mOptions);
 *
 *     }
 *  }
 * </pre>
 */
public class ImageTransformFactory {
    private static final String TAG = "ImageTransformFactory";
    private static ImageTransformFactory sImageTransformFactory;

/**
 * Rotates source image 0 degrees clockwise.
 */
    public static final int ROT_0 = 0;

/**
 * Rotates source image 90 degrees clockwise.
 */
    public static final int ROT_90 = 90;

/**
 * Rotates source image 180 degrees clockwise.
 */
    public static final int ROT_180 = 180;

/**
 *Rotates source image 270 degrees clockwise.
 */
    public static final int ROT_270 = 270;

/**
 * Flips source image horizontally (around the vertical axis).
 */
    public static final String FLIP_H = "horizontally"; //[Need Check]

/**
 * Flips source image vertically (around the vertical axis).
 */
    public static final String FLIP_V = "vertically"; //[Need Check]



    ImageTransformFactory() {
    }

    /**
     * Creates an ImageTransformFactory object.
     * @return a static ImageTransformFactory object
     */
    public static ImageTransformFactory createImageTransformFactory() {
        if (sImageTransformFactory == null) {
            sImageTransformFactory = new ImageTransformFactory();
        }
        return sImageTransformFactory;
    }
    /**
    * Transfers source image into target image by setting up properties
    */
    public boolean applyTransform(Image srcImage, Image targetImage, Options options) {
        Log.i(TAG , "applyTransform()");
        return native_applyTransform(srcImage, targetImage, options);
    }

    /**
     * Creates an ImageTransform Options object, which if left unchanged, the
     * same result will be given from the applytranform as if null is passed.
     *
     */
    public class Options {

        /**
         * Creates an ImageTransformFactory.Options instance.
         */
        public Options(){
        }
        /**
         * Crops the input image by user-defined region.
         */
        private Rect cropRoi;

        /**
         * Flips the target image vertically or horizontally.
         */
        private String flip;

        /**
         * Rotates the target image clockwise.
         */
        private int rotation;

        /**
         * Sets up the JPEG encoding quality from 0 to 100 (100 means the
         * highest quality) if the target image format is JPEG.
         */
        private int encodingQuality;

        /**
         * Enables or disables dither by setting it to true or false.
         */
        private boolean dither;

        /**
         * Sets up the sharpness level.
         * Value range: 0 ~ 5 (5 means more high sharpness).
         */
        private int sharpness;

        /**
         * Set crops the input image by user-defined region.
         * @param cropRoi crops the input image by user-defined region
         */
        public void setCropRoi(Rect cropRoi) {
            this.cropRoi = cropRoi;
        }

        /**
         * Get crops the input image by user-defined region.
         * @return crops the input image by user-defined region
         */
        public Rect getCropRoi() {
            return this.cropRoi;
        }

        /**
         * Set flips the target image vertically or horizontally.
         * @param flip flips the target image vertically or horizontally
         */
        public void setFlip(String flip) {
            this.flip = flip;
        }

        /**
         * Get flips the target image vertically or horizontally.
         * @return flips the target image vertically or horizontally
         */
        public String getFlip() {
            return this.flip;
        }

        /**
         * Set rotation of the target image clockwise.
         * @param rotation rotation of the target image clockwise
         */
        public void setRotation(int rotation) {
            this.rotation = rotation;
        }

        /**
         * Get rotation of the target image clockwise.
         * @return rotation of the target image clockwise
         */
        public int getRotation() {
            return this.rotation;
        }

        /**
         * Set up the JPEG encoding quality.
         * @param encodingQuality the JPEG encoding quality
         */
        public void setEncodingQuality(int encodingQuality) {
            this.encodingQuality = encodingQuality;
        }

        /**
         * Get the JPEG encoding quality.
         * @return the JPEG encoding quality
         */
        public int getEncodingQuality() {
            return this.encodingQuality;
        }

        /**
         * Set dither enable or disable.
         * @param dither enables or disables dither by setting it to true or false
         */
        public void setDither(boolean dither) {
            this.dither = dither;
        }

        /**
         * Is dither enable or disable.
         * @return true or flase
         */
        public boolean isDither() {
            return this.dither;
        }

        /**
         * Set the sharpness level.
         * @param sharpness the sharpness level
         */
        public void setSharpness(int sharpness) {
            this.sharpness = sharpness;
        }

        /**
         * Get the sharpness level.
         * @return the sharpness level
         */
        public int getSharpness() {
            return this.sharpness;
        }
    }

    static {
        System.loadLibrary("jni_imagetransform");
    }

    private static native boolean native_applyTransform(Image srcImage, Image targetImage, Options options);
}
