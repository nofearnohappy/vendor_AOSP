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
 * MediaTek Inc. (C) 2015. All rights reserved.
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

package com.mediatek.camera.util.jpegcodec;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import com.mediatek.mmsdk.BaseParameters;
import com.mediatek.mmsdk.CameraEffect;
import com.mediatek.mmsdk.CameraEffect.StateCallback;
import com.mediatek.mmsdk.CameraEffectHalException;
import com.mediatek.mmsdk.CameraEffectManager;
import com.mediatek.mmsdk.CameraEffectSession;
import com.mediatek.mmsdk.CameraEffectSession.CaptureCallback;
import com.mediatek.mmsdk.CameraEffectSession.SessionStateCallback;
import com.mediatek.mmsdk.EffectHalVersion;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

class HwJpegEncodeImpl extends JpegEncoder{
    private static final String TAG = HwJpegEncodeImpl.class.getSimpleName();
    private CameraEffectManager mCameraEffectManager;
    private CameraEffect mCameraEffect;
    private CameraEffectSession mCameraJpegHalSession;

    private BaseParameters mBaseParameters;
    private List<BaseParameters> mSurfaceParameters = new ArrayList<BaseParameters>();

    private int mMajorVersion = 1;
    private int mMinorVersion = 0;
    private EffectHalVersion mJpegHalVersion;

    private Handler mEffectHalHandler;
    private boolean mCloseWhenEncodeDone;

    HwJpegEncodeImpl(Context context) {
        mEffectHalHandler = createHandler("JpegEncoder");
        mCameraEffectManager = new CameraEffectManager(context);
        init();
        openJpegHal();
    }

    @Override
    public int[] getSupportedInputFormats() {
        //TODO, wait for native provide api for querying this.
        int[] supportedFormats = new int[2];
        supportedFormats[0] = PixelFormat.RGBA_8888;
        supportedFormats[1] = ImageFormat.YV12;
        return supportedFormats;
    }

    @Override
    public Surface configInputSurface(Surface outputSurface, int width,
            int height, int format) {
        List<Surface> outputSurfaces = new ArrayList<Surface>();
        outputSurfaces.add(outputSurface);
        updateJpegSize(new Size(width, height), format);
        setOutputSurface(outputSurfaces, mEffectHalHandler);
        return getInputSurface().get(0);
    }

    @Override
    public void startEncode() {
        startCapture(mEffectHalHandler);
    }

    @Override
    public void startEncodeAndReleaseWhenDown() {
        mCloseWhenEncodeDone = true;
        startCapture(mEffectHalHandler);
    }

    @Override
    public void release() {
        close();
    }

    private Handler createHandler(String handlerName) {
        HandlerThread thread = new HandlerThread(handlerName);
        thread.start();
        return new Handler(thread.getLooper());
    }

    private void init() {
        Log.i(TAG, "init");

        mBaseParameters = new BaseParameters();
        mBaseParameters.set(BaseParameters.KEY_OUT_PUT_CAPTURE_NUMBER, 1);
        mSurfaceParameters.add(mBaseParameters);

        // prepare the JPEG HAL version
        mJpegHalVersion = new EffectHalVersion();
        mJpegHalVersion.setName("Jpg");
        mJpegHalVersion.setMajor(mMajorVersion);
        mJpegHalVersion.setMinor(mMinorVersion);
    }

    private void updateJpegSize(Size pictureSize, int pictureFormat) {
        Assert.assertNotNull(pictureSize);
        Log.i(TAG, "[updateJpegSize] picture witdth = " + pictureSize.getWidth() + ",H = "
                + pictureSize.getHeight() + ",pictureFormat = " + pictureFormat
                + ",mCameraEffect = " + mCameraEffect);

        // add the picture size and format in the base parameters
        mBaseParameters.set(BaseParameters.KEY_PICTURE_WIDTH, pictureSize.getWidth());
        mBaseParameters.set(BaseParameters.KEY_PICTURE_HEIGHT, pictureSize.getHeight());
        mBaseParameters.set(BaseParameters.KEY_IMAGE_FORMAT, pictureFormat);

        // if current Effect is not null ,set the parameters to EFFECT HAL
        if (mCameraEffect != null) {
            Log.i(TAG, "[updateJpegSize] set the parameters to Effect HAL");
            mCameraEffect.setParameters(mBaseParameters);
        }
    }

    private void openJpegHal() {
        Log.i(TAG, "openJpegHal");
        try {
            mCameraEffect = mCameraEffectManager.openEffectHal(mJpegHalVersion,
                    mEffectStateCallback, mEffectHalHandler);
        } catch (CameraEffectHalException e) {
            e.printStackTrace();
        }
    }

    private List<Surface> getInputSurface() {
        // before get the inputSurface ,set the base parameters to EFFECT HAL
        mCameraEffect.setParameters(mBaseParameters);

        List<Surface> mInputSurface = mCameraEffect.getInputSurface();
        for (Surface surface : mInputSurface) {
            Log.i(TAG, "[getInputSurface] surface is : " + surface);
        }
        return mInputSurface;
    }

    private void setOutputSurface(List<Surface> surfaces, Handler handler) {
        Assert.assertNotNull(surfaces);
        Log.i(TAG, "[setOutputSurface] surfaces = " + surfaces);

        try {
            mCameraJpegHalSession = mCameraEffect.createCaptureSession(surfaces,
                    mSurfaceParameters, mJpegSessionStateCallback, handler);
        } catch (CameraEffectHalException e) {
            e.printStackTrace();
        }
    }

    private void startCapture(Handler handler) {
        Log.i(TAG, "[startCapture] first set the parameters : " + mBaseParameters);
        mCameraEffect.setParameters(mBaseParameters);
        Log.i(TAG, "[startCapture] second start Captrue");

        mCameraJpegHalSession.startCapture(mJpegSessionCaptureCallback, handler);
    }

    private void close() {
        Log.i(TAG, "close");
        mCameraEffect.closeEffect();
        mEffectHalHandler.getLooper().quitSafely();
    }

    private StateCallback mEffectStateCallback = new StateCallback() {

        @Override
        public void onError(CameraEffect effect, int error) {

        }

        @Override
        public void onDisconnected(CameraEffect effect) {

        }
    };

    private SessionStateCallback mJpegSessionStateCallback = new SessionStateCallback() {
        @Override
        public void onPrepared(CameraEffectSession session) {

        }

        @Override
        public void onConfigured(CameraEffectSession session) {

        }

        @Override
        public void onConfigureFailed(CameraEffectSession session) {

        }

        @Override
        public void onClosed(CameraEffectSession session) {

        }

    };
    private CaptureCallback mJpegSessionCaptureCallback = new CaptureCallback() {

        @Override
        public void onInputFrameProcessed(CameraEffectSession session, BaseParameters parameter,
                BaseParameters partialResult) {
            Log.i(TAG, "onInputFrameProcessed");
        }

        @Override
        public void onOutputFrameProcessed(CameraEffectSession session, BaseParameters parameter,
                BaseParameters partialResult) {
        }

        @Override
        public void onCaptureSequenceCompleted(CameraEffectSession session, BaseParameters result,
                long uid) {
            Log.i(TAG, "onCaptureSequenceCompleted");
        }

        @Override
        public void onCaptureSequenceAborted(CameraEffectSession session, BaseParameters result) {
            Log.i(TAG, "onCaptureSequenceAborted");
            if (mCloseWhenEncodeDone) {
                close();
                mCloseWhenEncodeDone = false;
            }
        }

        @Override
        public void onCaptureFailed(CameraEffectSession session, BaseParameters result) {

        }
    };
}