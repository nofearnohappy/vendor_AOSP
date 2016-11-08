package com.mediatek.mms.ext;

import android.graphics.Bitmap;

public interface IOpSlideshowPresenterExt {
    /**
     * @internal
     */
    Bitmap presentImage(String contentType, String src, boolean isImageVisible);
}
