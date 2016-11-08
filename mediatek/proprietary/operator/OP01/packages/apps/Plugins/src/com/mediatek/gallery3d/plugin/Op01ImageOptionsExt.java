package com.mediatek.gallery3d.plugin;

import android.util.Log;

import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.ui.PhotoView.Picture;
import com.android.gallery3d.ui.PositionController;
import com.android.gallery3d.ui.ScreenNail;
import com.android.gallery3d.ui.TileImageViewAdapter;

import com.mediatek.common.PluginImpl;
import com.mediatek.gallery3d.ext.DefaultImageOptionsExt;
import com.mediatek.galleryframework.base.MediaData;
import com.mediatek.galleryframework.base.MediaData.MediaType;

/**
 * OP01 plugin implementation of gallery image feature.
 */
@PluginImpl(interfaceName = "com.mediatek.gallery3d.ext.IImageOptionsExt")
public class Op01ImageOptionsExt extends DefaultImageOptionsExt {
    private static final String TAG = "Gallery2/Op01ImageOptionsExt";
    public static final int THUMBNAIL_TARGET_SIZE = 640;
    private MediaItem mMediaItem;

    @Override
    public void setMediaItem(MediaItem mediaItem) {
        mMediaItem = mediaItem;
        Log.i(TAG, "<setMediaItem> mMediaItem " + mMediaItem);
    }

    @Override
    public float getImageDisplayScale(float initScale) {
        Log.i(TAG, "<getImageDisplayScale> input initScale: " + initScale);
        if (mMediaItem == null || !checkIfNeedOriginalSize(mMediaItem)) {
            return initScale;
        }
        // we think of specified size image as special image
        // such as 1024 x 1, 1600 x 1
        int w = mMediaItem.getWidth();
        int h = mMediaItem.getHeight();
        int scale = Math.max(w, h) / THUMBNAIL_TARGET_SIZE;
        if (scale != 0 && (w / scale == 0 || h / scale == 0)) {
            Log.i(TAG, "<getImageDisplayScale> is special image, w: " + w
                    + ", h: " + h + ", not use original size!");
            return initScale;
        }
        float result = Math.min(initScale, 1.0f);
        Log.i(TAG, "<getImageDisplayScale> final initScale: " + result);
        return result;
    }

    @Override
    public float getMinScaleLimit(MediaType mediaType, float scale) {
        if (mediaType == null
                || mediaType == MediaType.VIDEO
                || mediaType == MediaType.PANORAMA) {
            Log.i(TAG, "<getMinScaleLimit> video or null, not use original size!");
            return scale;
        }
        float minScaleLimit = Math.min(scale, 1.0f);
        Log.i(TAG, "<getMinScaleLimit> minScaleLimit: " + minScaleLimit);
        return minScaleLimit;
    }

    @Override
    public void updateTileProviderWithScreenNail(TileImageViewAdapter adapter,
            ScreenNail screenNail) {
        if (adapter == null || screenNail == null
                || screenNail.getMediaItem() == null) {
            return;
        }

        if (!checkIfNeedOriginalSize(screenNail.getMediaItem())) {
            return;
        }

        adapter.updateWidthAndHeight(screenNail.getMediaItem());
    }

    @Override
    public void updateMediaType(Picture picture, ScreenNail screenNail) {
        if (picture == null || screenNail == null
                || screenNail.getMediaItem() == null
                || screenNail.getMediaItem().getMediaData() == null) {
            if (picture != null) {
                Log.i(TAG, "<updateMediaType> mediaType null for empty image");
                picture.updateMediaType(null);
            }
            Log.d(TAG, "[DBG] updateMediaType returned for item or data null");
            return;
        }
        MediaData data = screenNail.getMediaItem().getMediaData();
        Log.i(TAG, "<updateMediaType> mediaType " + data);
        picture.updateMediaType(data.mediaType);
    }

    @Override
    public void updateBoxMediaType(PositionController controller, int index,
            MediaType mediaType) {
        if (controller == null) {
            Log.i(TAG, "<updateBoxMediaType> return!");
            return;
        }
        controller.setMediaType(index, mediaType);
    }

    private boolean checkIfNeedOriginalSize(MediaItem item) {
        if (item == null || item.getMediaType() == MediaObject.MEDIA_TYPE_UNKNOWN) {
            Log.i(TAG, "<checkIfNeedOriginalSize> MediaItem is null or unknow!");
            return false;
        }

        MediaData data = item.getMediaData();
        if (data == null) {
            Log.i(TAG, "<checkIfNeedOriginalSize> MediaData is null!");
            return false;
        }
        if (data.mediaType == null
                || data.mediaType == MediaType.VIDEO
                || data.mediaType == MediaType.PANORAMA) {
            Log.i(TAG, "<checkIfNeedOriginalSize> video or null, not use original size!");
            return false;
        }
        return true;
    }
}
