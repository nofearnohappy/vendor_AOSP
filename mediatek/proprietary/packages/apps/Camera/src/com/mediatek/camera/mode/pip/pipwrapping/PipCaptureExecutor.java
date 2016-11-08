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
package com.mediatek.camera.mode.pip.pipwrapping;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import com.android.camera.Exif;
import com.mediatek.camera.util.Util;
import com.mediatek.camera.util.jpegcodec.JpegDecoder;
import com.mediatek.camera.util.jpegcodec.JpegEncoder;
import com.mediatek.camera.util.jpegcodec.JpegEncoder.JpegCallback;
import com.mediatek.camera.v2.exif.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class PipCaptureExecutor {
    private static final String TAG = PipCaptureExecutor.class.getSimpleName();
    private static final int NUM_FOR_SINGLE_CAPTURE = 2;
    private static final int MAX_PENDING_CAPTURE_COUNT = 2;
    private final Context mContext;
    private final RendererManager mRendererManager;
    private final JpegHeaderWrapper mJpegHeaderWrapper;
    private final ImageCallback mImageCallback;
    private final HandlerThread mImageReceiveThread;
    private final Handler mImageReceiveHandler;
    private final Executor mImageProcessExecutor;
    private final BlockingQueue<Runnable> mBlockingQueue;
    // A queue used to make a strong reference to CaptureInitRunnable instance
    // otherwise,CaptureInitRunnable instance may be GC by FinalizerDaemon.
    private final BlockingQueue<CaptureInitRunnable> mCaptureInitRunnableQueue;
    private final BlockingQueue<JpegProcessingRunnable> mCaptureProcessRunnableQueue;

    private int mImageOffered = 0;
    private ImageReader mImageReader;
    private ConditionVariable mImageReaderSync;
    private byte[] mCurrentJpegHeader;
    private JpegEncoder mVssJpegEncoder;
    private boolean mReleased;

    public interface ImageCallback {
        void onPictureTaken(byte[] jpegData);
        void unlockNextCapture();
        RendererManager getRendererManager();
        AnimationRect getPreviewAnimationRect();
    }

    public PipCaptureExecutor(Context context,
            RendererManager rendererManager, ImageCallback callback) {
        mContext = context;
        mRendererManager = rendererManager;
        mImageCallback = callback;
        mJpegHeaderWrapper = new JpegHeaderWrapper();
        mImageReceiveThread = new HandlerThread("Pip-Image-Receive");
        mImageReceiveThread.start();
        mImageReceiveHandler = new Handler(mImageReceiveThread.getLooper());

        mBlockingQueue = new LinkedBlockingQueue<Runnable>();
        mCaptureInitRunnableQueue = new LinkedBlockingQueue<CaptureInitRunnable>();
        mCaptureProcessRunnableQueue = new LinkedBlockingQueue<JpegProcessingRunnable>();
        mImageProcessExecutor = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS , mBlockingQueue);
        mImageReaderSync = new ConditionVariable();
        mImageReaderSync.open();
    }

    public void init() {
        mReleased = false;
    }

    public Surface getVssSurface(int width, int height) {
        if (mReleased) {
            Log.w(TAG, "getVssSurface return null.");
            return null;
        }
        mVssJpegEncoder = JpegEncoder.newInstance(mContext, false);
        Surface jpegInputSurface = mVssJpegEncoder.configInputSurface(mVssJpegCallback,
                width, height, PixelFormat.RGBA_8888);
        mVssJpegEncoder.startEncodeAndReleaseWhenDown();
        return jpegInputSurface;
    }

    public void setUpCapture(Size bottomJpegSize, Size topJpegSize) {
        Log.i(TAG, "setUpCapture" + "released:" + mReleased);
        if (mReleased) {
            return;
        }
        CaptureInitRunnable r = new CaptureInitRunnable(bottomJpegSize, topJpegSize);
        try {
            mCaptureInitRunnableQueue.put(r);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mImageProcessExecutor.execute(r);
    }

    public void offerJpegData(byte[] jpegData, Size jpegSize, boolean isBottom) {
        Log.i(TAG, "[offerJpegData]+ isBottom:" + isBottom
                + ",peding size:" + mBlockingQueue.size()
                + "released:" + mReleased
                + ",mCaptureProcessRunnableQueue size:"
                + mCaptureProcessRunnableQueue.size());
        if (mReleased) {
            return;
        }
        mImageOffered++;
        JpegProcessingRunnable r = new JpegProcessingRunnable(jpegData, isBottom);
        try {
            mCaptureProcessRunnableQueue.put(r);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mImageProcessExecutor.execute(r);
        if (mImageOffered == NUM_FOR_SINGLE_CAPTURE) {
            mImageOffered = 0;
            if (!blockingWhenMaxCaptureCountReached(MAX_PENDING_CAPTURE_COUNT,
                    false/*non-blocking*/)) {
                mImageCallback.unlockNextCapture();
            }
        }
        Log.i(TAG, "[offerJpegData]-");
    }

    public void unInit() {
        Log.i(TAG, "[unInit]+");
        mReleased = true;
        blockingWhenMaxCaptureCountReached(1, true/*blocking*/);
        releaseImageReader();
        mRendererManager.unInitCapture();
        mCurrentJpegHeader = null;
        mImageOffered = 0;
        Log.i(TAG, "[unInit]- CaptureInit RunnableQueue size:" + mCaptureInitRunnableQueue.size());
    }

    private Surface setUpImageReader(int width, int height, int format) {
        Log.i(TAG, "[setUpImageReader]+");
        mImageReaderSync.block();
        mImageReader = ImageReader.newInstance(width, height, format, 1);
        mImageReader.setOnImageAvailableListener(mImageAvailableListener, mImageReceiveHandler);
        mImageReaderSync.close();
        Log.i(TAG, "[setUpImageReader]-");
        return mImageReader.getSurface();
    }

    private void releaseImageReader() {
        Log.i(TAG, "releaseImageReader");
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
        mImageReaderSync.open();
    }

    private void updateJpegHeader(byte[] jpegHeader) {
        mCurrentJpegHeader = jpegHeader;
    }

    private OnImageAvailableListener mImageAvailableListener = new OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.i(TAG, "[onImageAvailable]+ mCurrentJpegHeader:" + mCurrentJpegHeader);
            mCaptureInitRunnableQueue.remove();
            mCaptureProcessRunnableQueue.remove();
            mCaptureProcessRunnableQueue.remove();
            Image image = reader.acquireNextImage();
            int format = image.getFormat();
            byte[] jpegData = null;
            if (ImageFormat.JPEG == format) {
                jpegData = Util.acquireJpegBytesAndClose(image);
                if (mCurrentJpegHeader != null) {
                    try {
                        jpegData = mJpegHeaderWrapper.writeJpegHeader(jpegData, mCurrentJpegHeader);
                        jpegData = setJpegRotationToZeroInExif(jpegData);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    mCurrentJpegHeader = null;
                }
            }
            if (jpegData != null) {
                mImageCallback.onPictureTaken(jpegData);
                jpegData = null;
            }
            releaseImageReader();
            Log.i(TAG, "[onImageAvailable]- format:" + format);
        }
    };

    private JpegCallback mVssJpegCallback = new JpegCallback() {
        @Override
        public void onJpegAvailable(byte[] jpegData) {
            mImageCallback.onPictureTaken(jpegData);
        }
    };

    private JpegCallback mCaptureJpegCallback = new JpegCallback() {
        @Override
        public void onJpegAvailable(byte[] jpegData) {
            Log.i(TAG, "mCaptureJpegCallback [onJpegAvailable]+");
            mCaptureInitRunnableQueue.remove();
            mCaptureProcessRunnableQueue.remove();
            mCaptureProcessRunnableQueue.remove();
            if (mCurrentJpegHeader != null) {
                try {
                    jpegData = mJpegHeaderWrapper.writeJpegHeader(jpegData, mCurrentJpegHeader);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mCurrentJpegHeader = null;
            }
            mImageCallback.onPictureTaken(jpegData);
        }
    };

    private class CaptureInitRunnable implements Runnable {
        private static final String TAG = "CaptureInitRunnable";
        private Size mBottomSz;
        private Size mTopSz;
        private JpegEncoder mJpegEncoder;
        public CaptureInitRunnable(Size bottomSz, Size topSz) {
            mBottomSz = bottomSz;
            mTopSz = topSz;
        }

        @Override
        public void run() {
            Log.i(TAG, "CaptureInitRunnable [run]+");
            // Make config pipeline: Buffer ==> GPU ==> JpegEncoder ==> BitStream
            Surface jpegInputSurface = null;
            if (JpegEncoder.isHwEncoderSupported(mContext)) {
                Surface jpegOutputSurface = setUpImageReader(mBottomSz.getWidth(),
                        mBottomSz.getHeight(), ImageFormat.JPEG);
                mJpegEncoder = JpegEncoder.newInstance(mContext, true);

                int pixelFormat = mRendererManager.initCapture(
                        mJpegEncoder.getSupportedInputFormats());
                jpegInputSurface = mJpegEncoder.configInputSurface(jpegOutputSurface,
                        mBottomSz.getWidth(), mBottomSz.getHeight(), pixelFormat);
            } else {
                mJpegEncoder = JpegEncoder.newInstance(mContext, false);
                int pixelFormat = mRendererManager.initCapture(
                        mJpegEncoder.getSupportedInputFormats());
                jpegInputSurface = mJpegEncoder.configInputSurface(mCaptureJpegCallback,
                        mBottomSz.getWidth(), mBottomSz.getHeight(), pixelFormat);
            }

            mRendererManager.setCaptureSurface(jpegInputSurface);
            mRendererManager.setCaptureSize(mBottomSz, mTopSz);

            mJpegEncoder.startEncodeAndReleaseWhenDown();
            Log.i(TAG, "CaptureInitRunnable [run]-");
        }
    }

    private class JpegProcessingRunnable implements Runnable {
        private static final String TAG = "JpegProcessingRunnable";
        private byte[] mJpegData;
        private boolean mIsBottom;
        private JpegDecoder mJpegDecoder;
        public JpegProcessingRunnable(byte[] jpegData, boolean isBottom) {
            mJpegData = jpegData;
            mIsBottom = isBottom;
        }
        @Override
        public void run() {
            Log.i(TAG, "JpegProcessingRunnable [run]+ isBottom:" + mIsBottom);
            mJpegDecoder = JpegDecoder.newInstance(mIsBottom ?
                    mRendererManager.getBottomCapSt() : mRendererManager.getTopCapSt());
            mRendererManager.setJpegRotation(mIsBottom, Exif.getOrientation(mJpegData));
            if (mIsBottom) {
                try {
                    updateJpegHeader(mJpegHeaderWrapper.readJpegHeader(mJpegData));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            mJpegDecoder.decode(mJpegData);
            mJpegDecoder.release();
            mJpegDecoder = null;
            mJpegData = null;
            Log.i(TAG, "JpegProcessingRunnable [run]-");
        }
    }

    // pip GPU will rotate buffer if need, here remove original jpeg's rotation
    private byte[] setJpegRotationToZeroInExif(byte[] sourceJpeg) {
        ExifInterface exif = new ExifInterface();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            exif.readExif(sourceJpeg);
            exif.setTagValue(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.Orientation.TOP_LEFT);
            exif.writeExif(sourceJpeg, out);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    private boolean blockingWhenMaxCaptureCountReached(int maxCaptureCount, boolean sync) {
        // one pip capture need 3 runnable
        if (mBlockingQueue.size() >= maxCaptureCount * 3) {
            if (sync) {
                waitDone(mImageProcessExecutor);
            } else {
                waitDoneAsync(mImageProcessExecutor);
            }
            return true;
        }
        return false;
    }

    private boolean waitDone(Executor executor) {
        final Object waitDoneLock = new Object();
        final Runnable unlockRunnable = new Runnable() {
            @Override
            public void run() {
                synchronized (waitDoneLock) {
                    waitDoneLock.notifyAll();
                }
            }
        };
        synchronized (waitDoneLock) {
            executor.execute(unlockRunnable);
            try {
                waitDoneLock.wait();
            } catch (InterruptedException ex) {
                Log.i(TAG, "waitDone interrupted");
                return false;
            }
        }
        return true;
    }

    private void waitDoneAsync(Executor executor) {
        final Runnable unlockRunnable = new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "waitDoneAsync comes!");
                mImageCallback.unlockNextCapture();
            }
        };
        executor.execute(unlockRunnable);
    }
}
