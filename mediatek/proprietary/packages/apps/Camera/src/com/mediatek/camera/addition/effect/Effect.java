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

package com.mediatek.camera.addition.effect;

import android.graphics.ImageFormat;
//TODO: mediatek package can't reference to the MTK non-SDK or MTK SDK or Google non-SDK API
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;

import android.os.ConditionVariable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.Surface;

import com.mediatek.camera.ICameraContext;
import com.mediatek.camera.platform.ICameraDeviceManager;
import com.mediatek.camera.platform.ICameraDeviceManager.ICameraDevice;
import com.mediatek.camera.util.Log;
//TODO: mediatek package can't reference to the MTK non-SDK or MTK SDK
import com.mediatek.matrixeffect.MatrixEffect;
import com.mediatek.matrixeffect.MatrixEffect.EffectsCallback;

import java.util.ArrayList;

public class Effect {
    private static final String TAG = "Effect";

    private ICameraContext mCameraContext;

    private MatrixEffect mMatrixEffect;
    private EffectView mEffectView;
    private EffectHandler mHandler;

    private final static int MSG_INIT_EFFECT = 100;
    private final static int MSG_REGISTER_BUFFERS = 101;
    private final static int MSG_SET_SURFACET_NATIVE = 102;
    private final static int MSG_PROCESS_EFFECT = 103;
    private final static int MSG_RELEASE_EFFECT = 104;
    private final static int EFFECT_NUM_OF_PAGE = 12;
    private final static int PAGE_NUM = 3;
    private final static int CACHE_BUFFER_NUM = 3;
    private static final int NUM_OF_DROP = 6;
    private static final int MAX_NUM_OF_PROCESSING = 2;

    private long mInputStartTime;

    private int mCacheIndex = 0;
    private int mInputFrames = 0;
    private int mCurrentNumOfProcess = 0;
    private int mNumOfDropFrame = NUM_OF_DROP;

    private int[] mEffectIndexs = new int[EFFECT_NUM_OF_PAGE];
    private Surface[] mSurfaceArray = new Surface[EFFECT_NUM_OF_PAGE];
    private byte[][] mEffectsBuffers = new byte[EFFECT_NUM_OF_PAGE * PAGE_NUM][];
    private byte[][] mPreviewCallbackBuffers = new byte[CACHE_BUFFER_NUM][];
    private ArrayList<byte[]> mCacheBuffer = new ArrayList<byte[]>();

    private boolean mRealsed = true;
    private boolean mRegisterBufferDone = false;
    private Listener mListener;
    private ConditionVariable mReleaseCondition = new ConditionVariable();

    public interface Listener {
        public void onEffectsDone();
    }

    public Effect(ICameraContext cameraContext) {
        Log.i(TAG, "[Effect]constructor...");
        mCameraContext = cameraContext;
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void onInitialize() {
        Log.i(TAG, "[onInitialize]mHandler:" + mHandler);
        mMatrixEffect = MatrixEffect.getInstance();
        if (mHandler == null) {
            HandlerThread ht = new HandlerThread("draw buffer handler thread", Thread.MAX_PRIORITY);
            ht.start();
            mHandler = new EffectHandler(ht.getLooper());
        }

        for (int i = 0; i < mEffectIndexs.length; i++) {
            mEffectIndexs[i] = -1;
        }

        int bufferSize = getBufferSize();
        if (mCacheBuffer.size() == 0) {
            for (int i = 0; i < CACHE_BUFFER_NUM; i++) {
                byte[] cacheBuffer = new byte[bufferSize];
                mCacheBuffer.add(cacheBuffer);
            }
        }
        mCacheIndex = 0;
        mMatrixEffect.setCallback(mEffectsCallback);
        mHandler.sendEmptyMessage(MSG_INIT_EFFECT);
    }

    public void onSurfaceAvailable(Surface surface, int width, int height, int position) {
        mSurfaceArray[position] = surface;

        if (!mRegisterBufferDone) {
            int bufferWidth = width / 32 * 32;
            int bufferHeight = height / 16 * 16;
            int bufferSize = bufferWidth * bufferHeight * 3 / 2;
            for (int i = 0; i < EFFECT_NUM_OF_PAGE * PAGE_NUM; i++) {
                if (mEffectsBuffers[i] == null) {
                    byte[] b = new byte[bufferSize];
                    mEffectsBuffers[i] = b;
                }
            }
            Log.d(TAG, "[onSurfaceAvailable]Register buffer size, bufferWidth:" + bufferWidth
                    + ", bufferHeight:" + bufferHeight);
            if (mHandler != null) {
                mHandler.obtainMessage(MSG_REGISTER_BUFFERS, bufferWidth, bufferHeight)
                        .sendToTarget();
                mRegisterBufferDone = true;
            }

        }
        if (mHandler != null) {
            mHandler.obtainMessage(MSG_SET_SURFACET_NATIVE, position, 0, surface).sendToTarget();
        }

    }

    public void onUpdateEffect(int pos, int effectId) {
        mEffectIndexs[pos] = effectId;
    }

    public void onReceivePreviewFrame(boolean receive) {
        Log.i(TAG, "[onReceivePreviewFrame]receive:" + receive);
        ICameraDeviceManager cameraDeviceManager = mCameraContext.getCameraDeviceManager();
        ICameraDevice cameraDevice = cameraDeviceManager.getCameraDevice(cameraDeviceManager
                .getCurrentCameraId());
        if (!receive) {
            if (mHandler != null) {
                mHandler.removeMessages(MSG_PROCESS_EFFECT);
            }
            if (cameraDevice != null) {
                cameraDevice.setPreviewCallbackWithBuffer(null);
            }

        } else {
            int bufferSize = getBufferSize();
            for (int i = 0; i < mPreviewCallbackBuffers.length; i++) {
                if (mPreviewCallbackBuffers[i] == null) {
                    mPreviewCallbackBuffers[i] = new byte[bufferSize];
                }
                cameraDevice.addCallbackBuffer(mPreviewCallbackBuffers[i]);
            }
            cameraDevice.setPreviewCallbackWithBuffer(mPreviewCallback);
            mNumOfDropFrame = 0;
        }
    }

    public void onRelease() {
        Log.i(TAG, "[onRelease]mRealsed:" + mRealsed + ", mHandler:" + mHandler);
        if (mRealsed) {
            return;
        }

        ICameraDeviceManager cameraDeviceManager = mCameraContext.getCameraDeviceManager();
        ICameraDevice cameraDevice = cameraDeviceManager.getCameraDevice(cameraDeviceManager
                .getCurrentCameraId());
        if (cameraDevice != null) {
            cameraDevice.setPreviewCallbackWithBuffer(null);
        }
        if (mHandler != null) {
            if (mHandler.hasMessages(MSG_PROCESS_EFFECT)) {
                mHandler.removeMessages(MSG_PROCESS_EFFECT);
            }
            mHandler.sendEmptyMessage(MSG_RELEASE_EFFECT);
            // Make sure the mHandler quit and create in the same thread
            // which is UI thread so wait the effect is released.
            Log.i(TAG, "waiting for release effect in onRelease()");
            mReleaseCondition.block();
            mReleaseCondition.close();

            mHandler.getLooper().quit();
            mHandler = null;
        }
    }

    public void release() {
        Log.i(TAG, "[release], mRealsed:" + mRealsed + ", mHandler:" + mHandler);
        // some times the onRelease may be not called, so it needs initiative to release effect when
        // camera destroy
        if (mRealsed) {
            return;
        }
        if (mHandler != null) {
            mHandler.sendEmptyMessage(MSG_RELEASE_EFFECT);
            Log.i(TAG, "waiting for release effect in release()");
            mReleaseCondition.block();
            mReleaseCondition.close();
            mHandler.getLooper().quit();
            mHandler = null;
        }
        mMatrixEffect = null;
    }

    private int getBufferSize() {
        ICameraDeviceManager cameraDeviceManager = mCameraContext.getCameraDeviceManager();
        ICameraDevice cameraDevice = cameraDeviceManager.getCameraDevice(cameraDeviceManager
                .getCurrentCameraId());
        Size size = cameraDevice.getParameters().getPreviewSize();
        int imageFormat = cameraDevice.getParameters().getPreviewFormat();
        Log.d(TAG, "[getBufferSize]size.width = " + size.width + " size.height = " + size.height
                + ", " + "PreviewFormat:" + imageFormat);

        return size.width * size.height * ImageFormat.getBitsPerPixel(imageFormat) / 8;
    }

    private boolean isNeedDropFrame() {
        return mNumOfDropFrame < NUM_OF_DROP;
    }

    private void processEffect(byte[] data) {
        if (data == null) {
            Log.w(TAG, "[processEffect] data is null,return!");
            return;
        }

        byte[] cacheBuf = mCacheBuffer.get(mCacheIndex);
        if (data.length != cacheBuf.length) {
            Log.w(TAG, "[processEffect]preview buffer size is larger,return!");
            return;
        }
        System.arraycopy(data, 0, cacheBuf, 0, data.length);
        mCacheIndex = (mCacheIndex + 1) % CACHE_BUFFER_NUM;
        mCurrentNumOfProcess++;
        if (mHandler != null) {
            Message msg = mHandler.obtainMessage(MSG_PROCESS_EFFECT, mCacheIndex, 0, cacheBuf);
            mHandler.sendMessage(msg);
        }

    }

    private class EffectHandler extends Handler {
        public EffectHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "[handleMessage]msg.what = " + msg.what + ",mRealsed = " + mRealsed);
            switch (msg.what) {
            case MSG_INIT_EFFECT:
                ICameraDeviceManager cameraDeviceManager = mCameraContext.getCameraDeviceManager();
                ICameraDevice cameraDevice = cameraDeviceManager
                        .getCameraDevice(cameraDeviceManager.getCurrentCameraId());
                Size size = cameraDevice.getParameters().getPreviewSize();
                mMatrixEffect.initialize(size.width, size.height, EFFECT_NUM_OF_PAGE, 11);
                mRealsed = false;
                break;

            case MSG_REGISTER_BUFFERS:
                if (mRealsed) {
                    return;
                }
                mMatrixEffect.setBuffers(msg.arg1, msg.arg2, mEffectsBuffers);
                break;

            case MSG_SET_SURFACET_NATIVE:
                if (mRealsed) {
                    return;
                }
                mMatrixEffect.setSurface((Surface) msg.obj, msg.arg1);
                break;

            case MSG_PROCESS_EFFECT:
                long beginTime = System.currentTimeMillis();
                mMatrixEffect.process((byte[]) msg.obj, mEffectIndexs);
                mCurrentNumOfProcess--;
                long endTime = System.currentTimeMillis();
                Log.d(TAG, "process_time:" + (endTime - beginTime));
                break;

            case MSG_RELEASE_EFFECT:
                releaseEffect();
                break;

            default:
                break;
            }
        }
    }

    private void releaseEffect() {
        Log.i(TAG, "[releaseEffect]mRealsed:" + mRealsed);
        if (!mRealsed) {
            mMatrixEffect.setCallback(null);
            mMatrixEffect.release();
            mRealsed = true;
            mRegisterBufferDone = false;

            for (int i = 0; i < mEffectsBuffers.length; i++) {
                mEffectsBuffers[i] = null;
            }

            for (int i = 0; i < mPreviewCallbackBuffers.length; i++) {
                mPreviewCallbackBuffers[i] = null;
            }

            for (int i = 0; i < mSurfaceArray.length; i++) {
                mSurfaceArray[i] = null;
            }

            mCacheBuffer.clear();
        }
        mReleaseCondition.open();
    }

    private PreviewCallback mPreviewCallback = new PreviewCallback() {
        @Override
        public void onPreviewFrame(final byte[] data, android.hardware.Camera camera) {
            ICameraDeviceManager cameraDeviceManager = mCameraContext.getCameraDeviceManager();
            ICameraDevice cameraDevice = cameraDeviceManager.getCameraDevice(cameraDeviceManager
                    .getCurrentCameraId());
            Log.d(TAG, "[onPreviewFrame]mCurrentNumOfProcess:" + mCurrentNumOfProcess
                    + ",mNumOfDropFrame:" + mNumOfDropFrame + ", mRegisterBufferDone:"
                    + mRegisterBufferDone);
            if (mInputFrames == 0) {
                mInputStartTime = System.currentTimeMillis();
            }
            mInputFrames++;
            if (mInputFrames % 20 == 0) {
                long duration = System.currentTimeMillis() - mInputStartTime;
                Log.d(TAG, "[onPreviewFrame]pv callback Fps:" + (20 * 1000) / duration);
                mInputStartTime = System.currentTimeMillis();
            }

            if (mCurrentNumOfProcess == MAX_NUM_OF_PROCESSING) {
                if (mHandler != null) {
                    mHandler.removeMessages(MSG_PROCESS_EFFECT);
                    mCurrentNumOfProcess = 1;
                    Log.i(TAG, "dropFrame");
                }

            }
            if (mCurrentNumOfProcess < MAX_NUM_OF_PROCESSING && !isNeedDropFrame()
                    && mRegisterBufferDone) {
                processEffect(data);
            } else if (isNeedDropFrame()) {
                mNumOfDropFrame++;
            }

            if (cameraDevice != null) {
                cameraDevice.addCallbackBuffer(data);
            }
        }
    };

    private EffectsCallback mEffectsCallback = new EffectsCallback() {
        @Override
        public void onEffectsDone() {
            Log.i(TAG, "[onEffectsDone]...");
            if (mListener != null) {
                mListener.onEffectsDone();
            }

        }
    };

}
