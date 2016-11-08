package com.mediatek.mms.plugin;

import android.graphics.Bitmap;

import com.mediatek.mms.ext.DefaultOpSlideshowPresenterExt;

public class Op09SlideshowPresenterExt extends DefaultOpSlideshowPresenterExt {

    @Override
    public Bitmap presentImage(String contentType, String src, boolean isImageVisible) {

        Op09MmsUnSupportedFilesExt mmsUnsupportedFilesExt = new Op09MmsUnSupportedFilesExt(null);
        Bitmap bitmap = null;

        if (MessageUtils.isUnsupportedFilesOn()
                && !mmsUnsupportedFilesExt.isSupportedFile(contentType, src)) {
            bitmap = mmsUnsupportedFilesExt.getUnsupportedImageIcon();
        }

        if (MessageUtils.isUnsupportedFilesOn()) {
            if (!mmsUnsupportedFilesExt.isSupportedFile(contentType, src)) {
                mmsUnsupportedFilesExt.setUnsupportedViewVisibilityForImage(isImageVisible);
            } else {
                mmsUnsupportedFilesExt.setUnsupportedViewVisibilityForImage(false);
            }
        }

        return bitmap;
    }


}
