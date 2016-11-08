/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2014. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.camera.v2.stream;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;
import android.os.ConditionVariable;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

/**
 * A stream used for preview display.
 * <p>
 * preview stream is controlled by {@link PreviewStreamController} and reported status by
 * {@link PreviewStreamCallback}.
 * Any client uses this stream should consider how to use {@link PreviewStreamController}
 * and {@link PreviewStreamCallback}.
 */
public class PreviewStream implements IPreviewStream, IPreviewStream.PreviewCallback {
    private static final String         TAG = PreviewStream.class.getSimpleName();
    private PreviewStreamCallback       mStreamCallback;
    private PreviewSurfaceCallback      mSurfaceCallback;
    private ConditionVariable           mSurfaceReadySync = new ConditionVariable();
    private Surface                     mPreviewSurface;
    private int                         mPreviewWidth;
    private int                         mPreviewHeight;

    public PreviewStream() {

    }

    @Override
    public boolean updatePreviewSize(Size size) {
        Log.i(TAG, "[updatePreviewSize]+ size:" + size.getWidth() + " x " + size.getHeight());
        Assert.assertNotNull(size);
        int width = size.getWidth();
        int height = size.getHeight();
        if (width == mPreviewWidth && height == mPreviewHeight && mPreviewSurface != null) {
            if (mSurfaceCallback != null) {
                mSurfaceCallback.onPreviewSufaceIsReady(false);
                mSurfaceCallback = null;
            }
            Log.i(TAG, "[updatePreviewSize]- with the same previe size");
            return false;
        }
        // when preview size changed, should be updated with a new surface
        mPreviewSurface = null;
        // close condition variable to wait new surface really comes
        mSurfaceReadySync.close();
        mPreviewWidth = width;
        mPreviewHeight = height;
        Log.i(TAG, "[updatePreviewSize]-");
        return true;
    }

    @Override
    public Map<String, Surface> getPreviewInputSurfaces() {
        Log.i(TAG, "[getPreviewInputSurfaces]+");
        Map<String, Surface> surfaceMap = new HashMap<String, Surface>();
        if (mPreviewSurface == null) {
            mSurfaceReadySync.block();
        }
        surfaceMap.put(PREVIEW_SURFACE_KEY, mPreviewSurface);
        Log.i(TAG, "[getPreviewInputSurfaces]- mPreviewSurface:" + mPreviewSurface);
        return surfaceMap;
    }

    @Override
    public void setPreviewStreamCallback(PreviewStreamCallback callback) {
        mStreamCallback = callback;
    }

    @Override
    public void setOneShotPreviewSurfaceCallback(PreviewSurfaceCallback surfaceCallback) {
        mSurfaceCallback = surfaceCallback;
    }

    @Override
    public void onFirstFrameAvailable() {
        Log.i(TAG, "onFirstFrameAvailable mStreamCallback:" + mStreamCallback);
        if (mStreamCallback != null) {
            mStreamCallback.onFirstFrameAvailable();
        }
    }

    @Override
    public void surfaceAvailable(Surface surface, int width, int height) {
        Log.i(TAG, "surfaceAvailable surface = " + surface
                + " width = " + width
                + " height = " + height);
        if (width == mPreviewWidth && height == mPreviewHeight) {
            mPreviewSurface = surface;
            mSurfaceReadySync.open();

            if (mSurfaceCallback != null) {
                mSurfaceCallback.onPreviewSufaceIsReady(true);
                mSurfaceCallback = null;
            }
        }
    }

    @Override
    public void surfaceSizeChanged(Surface surface, int width, int height) {
        Log.i(TAG, "surfaceSizeChanged surface = " + surface
                + " width = " + width
                + " height = " + height);
        if (width == mPreviewWidth && height == mPreviewHeight) {
            mPreviewSurface = surface;
            mSurfaceReadySync.open();
            if (mSurfaceCallback != null) {
                mSurfaceCallback.onPreviewSufaceIsReady(true);
                mSurfaceCallback = null;
            }
        }
    }

    @Override
    public void surfaceDestroyed(Surface surface) {
        Log.i(TAG, "surfaceDestroyed surface = " + surface);
        // destroy current surface
        if (surface == mPreviewSurface) {
            mPreviewSurface  = null;
            mPreviewWidth    = 0;
            mPreviewHeight   = 0;
        }
    }

    @Override
    public void releasePreviewStream() {

    }
}
