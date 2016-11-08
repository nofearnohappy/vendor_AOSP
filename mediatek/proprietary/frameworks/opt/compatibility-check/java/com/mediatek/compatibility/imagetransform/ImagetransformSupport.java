package com.mediatek.compatibility.imagetransform;

import android.util.Log;

import com.mediatek.imagetransform.ImageTransformFactory;

public class ImagetransformSupport {

    public static final String TAG = "ImagetransformSupport";
    public static boolean isImagetransformFeatureAvaliable() {
        try {
            ImageTransformFactory imageTransformFactory = 
                    ImageTransformFactory.createImageTransformFactory();
            return true;
        } catch (Throwable e) {
            Log.e(TAG, "Imagetransform feature is not available");
            return false;
        }
    }

}
