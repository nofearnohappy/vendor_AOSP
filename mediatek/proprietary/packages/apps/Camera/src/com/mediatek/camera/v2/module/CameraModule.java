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

package com.mediatek.camera.v2.module;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mediatek.camera.v2.control.ControlImpl;
import com.mediatek.camera.v2.control.IControl.IAaaController;
import com.mediatek.camera.v2.detection.DetectionManager;
import com.mediatek.camera.v2.mode.AbstractCameraMode;
import com.mediatek.camera.v2.platform.ModeChangeListener;
import com.mediatek.camera.v2.platform.app.AppController;
import com.mediatek.camera.v2.platform.device.CameraDeviceManager;
import com.mediatek.camera.v2.platform.device.CameraDeviceProxy;
import com.mediatek.camera.v2.platform.device.CameraDeviceManager.CameraStateCallback;
import com.mediatek.camera.v2.platform.device.CameraDeviceProxy.CameraSessionCallback;
import com.mediatek.camera.v2.util.Utils;

import android.app.Activity;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureRequest.Builder;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;

public class CameraModule extends AbstractCameraModule {
    private static final String TAG = CameraModule.class.getSimpleName();
    private CameraDeviceManager mCameraDeviceManager;
    private CameraDeviceProxy mCameraDevice;
    private Handler mCameraHandler;

    private int mOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
    private boolean mFirstFrameArrived = false;
    private RequestType mCurrentRepeatingRequest = RequestType.PREVIEW;

    private ModeChangeListanerImpl mModeChangedListener;

    public CameraModule(AppController app) {
        super(app);
        mDetectionManager = new DetectionManager(app, this, null);
        mAaaControl              = new ControlImpl(app, this, true, null);
    }

    @Override
    public void open(Activity activity, boolean isSecureCamera,
            boolean isCaptureIntent) {
        Log.i(TAG, "[open]+");
        super.open(activity, isSecureCamera, isCaptureIntent);

        HandlerThread thread = new HandlerThread("CameraModule.CameraHandler");
        thread.start();
        mCameraHandler = new Handler(thread.getLooper());

        // initialize module ui
        mAbstractModuleUI = new CameraModuleUi(activity, this,
                mAppUi.getModuleLayoutRoot(), mStreamManager.getPreviewCallback());
        mAppController.setModuleUiListener(mAbstractModuleUI);

        mModeChangedListener = new ModeChangeListanerImpl();
        mAppController.setModeChangeListener(mModeChangedListener);

        // initialize camera manager
        mCameraDeviceManager = mAppController.getCameraManager();
        mCameraId = mSettingController.getCurrentCameraId();
        mAppController.addPreviewAreaSizeChangedListener(this);
        Log.i(TAG, "[open]-");
    }

    @Override
    public void resume() {
        Log.i(TAG, "[resume]+");
        super.resume();
        openCamera();
        Log.i(TAG, "[resume]-");
    }

    @Override
    public void pause() {
        Log.i(TAG, "[pause]+");
        /**
         * Flow: close camera --> mCurrentMode.pause.
         * Otherwise, there may be a request asked to configure, but there is no
         * valid surface can be used.
         */
        closeCamera();
        super.pause();
        Log.i(TAG, "[pause]-");
    }

    @Override
    public void close() {
        super.close();
        Log.i(TAG, "[close]+");
        mCameraHandler.getLooper().quitSafely();
        mAppController.removePreviewAreaSizeChangedListener(this);
        Log.i(TAG, "[close]-");
    }

    @Override
    public void onCameraPicked(String newCameraId) {
        Log.i(TAG, "[onCameraPicked]+ newCameraId: " + newCameraId);
        if (mPaused /**|| mAppController.getCameraProvider().waitingForCamera()**/) {
            Log.i(TAG, "[onCameraPicked]- mPaused = " + mPaused);
            return;
        }
        super.onCameraPicked(newCameraId);
        mPendingSwitchCameraId = Integer.valueOf(newCameraId);
        switchCamera();
        mCurrentMode.switchCamera(newCameraId);
        Log.i(TAG, "[onCameraPicked]- ");
    }

    @Override
    public void onLayoutOrientationChanged(boolean isLandscape) {

    }

    @Override
    public void onOrientationChanged(int orientation) {
        // We keep the last known orientation. So if the user first orient
        // the camera then point the camera to floor or sky, we still have
        // the correct orientation.
        if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
            return;
        }
        int newOrientation = Utils.roundOrientation(orientation, mOrientation);

        if (mOrientation != newOrientation) {
            mOrientation = newOrientation;
        }
        mAbstractModuleUI.onOrientationChanged(mOrientation);
        mCurrentMode.onOrientationChanged(mOrientation);
        mDetectionManager.onOrientationChanged(mOrientation);
    }

    @Override
    public boolean onSingleTapUp(float x, float y) {
        Log.i(TAG, "[onSingleTapUp]+ x = " + x + " y = " + y);
        if (mPaused || mCameraDevice == null) {
            Log.i(TAG, "[onSingleTapUp]- mPaused : " + mPaused +
                    " mCameraDevice: " + mCameraDevice);
            return false;
        }
        if (super.onSingleTapUp(x, y)) {
            return true;
        }
        mDetectionManager.onSingleTapUp(x, y);
        mAaaControl.onSingleTapUp(x, y);
        Log.i(TAG, "[onSingleTapUp]-");
        return false;
    }

    @Override
    public boolean onLongPress(float x, float y) {
        Log.i(TAG, "onLongPress x = " + x + " y = " + y);
        if (super.onLongPress(x, y)) {
            return true;
        }
        mDetectionManager.onLongPressed(x, y);
        return false;
    }

    @Override
    public void requestChangeCaptureRequets(boolean sync, RequestType requestType,
            CaptureType captureType) {
        sendRequestChanging(requestType, captureType, sync);
    }

    @Override
    public void requestChangeCaptureRequets(boolean isMainCamera, boolean sync,
            RequestType requestType, CaptureType captureType) {
        sendRequestChanging(requestType, captureType, sync);
    }

    @Override
    public void requestChangeSessionOutputs(boolean sync) {
        if (mCameraDevice == null) {
            Log.i(TAG, "requestChangeSessionOutputs but CameraDevice is null!!!");
            return;
        }
        mCameraDevice.requestChangeSessionOutputs(sync);
    }

    @Override
    public IAaaController get3AController(String cameraId) {
        return mAaaControl;
    }

    @Override
    protected void closeMode(AbstractCameraMode mode) {
        super.closeMode(mode);
        mode.pause();
        mode.close();
    }

    @Override
    protected void openMode(AbstractCameraMode mode) {
        super.openMode(mode);
        mode.open(mStreamManager, mAppUi.getModuleLayoutRoot(), mIsCaptureIntent);
        // Make sure do mode changed after old mode closed and before new mode resumed.
        doModeChange(mOldModeIndex, mCurrentModeIndex);
        mode.resume();
    }

    @Override
    protected boolean checkSatisfyCaptureCondition() {
        if (mCameraDevice == null) {
            Log.i(TAG, "checkSatisfyCaptureCondition" +
                    " Photo Shutter Cliecked but mCameraDevice = " + mCameraDevice);
            return false;
        }
        return true;
    }

    private CameraStateCallback mCameraStateCallback = new CameraStateCallback() {
        @Override
        public void onOpened(CameraDeviceProxy camera) {
            Log.i(TAG, "onOpened[mCameraStateCallback],mPreviewSurfaceIsReadyForOpen = "
                    + mPreviewSurfaceIsReadyForOpen);
            mPendingSwitchCameraId = UNKNOWN;
            mCameraDevice = camera;
            if (mPreviewSurfaceIsReadyForOpen) {
                requestChangeSessionOutputs(true);
            }
        }

        @Override
        public void onError(int error) {
            showErrorAndFinish(error);
        }
    };

    private CameraSessionCallback mCameraSessionCallback = new CameraSessionCallback() {
        @Override
        public void onSessionConfigured() {
            requestChangeCaptureRequets(true/*sync*/,
                    getDefaultRepeatingRequest(), CaptureType.REPEATING_REQUEST);
        };

        @Override
        public void onSessionActive() {
            mFirstFrameArrived = false;
        }

        @Override
        public CaptureCallback configuringSessionRequests(Builder requestBuilder,
                RequestType requestType, CaptureType captureType) {
            CaptureCallback captureCallback = null;
            switch (requestType) {
            case RECORDING:
            case PREVIEW:
                captureCallback = mPreviewCapProgressCallback;
                break;
            case STILL_CAPTURE:
                captureCallback = mCurrentMode.getCaptureCallback();
                break;
            default:
                break;
            }
            // apply crop region
            requestBuilder.set(CaptureRequest.SCALER_CROP_REGION, Utils.cropRegionForZoom(
                    mAppController.getActivity(),
                    mCameraId,
                    1f));

            Map<RequestType, CaptureRequest.Builder> requestBuilders =
                    new HashMap<RequestType, CaptureRequest.Builder>();
            requestBuilders.put(requestType, requestBuilder);
            // 1. apply 3A control
            mAaaControl.configuringSessionRequests(requestBuilders, captureType, true);
            // 2. apply addition parameter
            mDetectionManager.configuringSessionRequests(requestBuilders, captureType);
            // 3. apply mode parameter
            mCurrentMode.configuringSessionRequests(requestBuilders, true);

            return captureCallback;
        }

        @Override
        public void configuringSessionOutputs(
                List<Surface> sessionOutputSurfaces) {
            mCurrentMode.configuringSessionOutputs(sessionOutputSurfaces, true);
        }
    };

    private CaptureCallback                    mPreviewCapProgressCallback = new CaptureCallback() {
        @Override
        public void onCaptureStarted(CameraCaptureSession session,
                CaptureRequest request, long timestamp, long frameNumber) {
            mAaaControl.onPreviewCaptureStarted(request, timestamp, frameNumber);
            mDetectionManager.onCaptureStarted(request, timestamp, frameNumber);
            mCurrentMode.onPreviewCaptureStarted(request, timestamp, frameNumber);
        }

        public void onCaptureProgressed(CameraCaptureSession session,
                CaptureRequest request, CaptureResult partialResult) {
            mAaaControl.onPreviewCaptureProgressed(request, partialResult);
        };

        @Override
        public void onCaptureCompleted(CameraCaptureSession session,
                CaptureRequest request, TotalCaptureResult result) {
            mAaaControl.onPreviewCaptureCompleted(request, result);
            mDetectionManager.onCaptureCompleted(request, result);
            mCurrentMode.onPreviewCaptureCompleted(request, result);
            if (!mFirstFrameArrived) {
                mCurrentMode.onFirstFrameAvailable();
                mFirstFrameArrived = true;
            }
        }
    };

    private class ModeChangeListanerImpl implements ModeChangeListener {
        @Override
        public void onModeSelected(int modeIndex) {
            switchToNewMode(modeIndex);
        }
    }

    private void openCamera() {
        if (mCameraDeviceManager == null) {
            throw new IllegalStateException("openCamera, but CameraManager is null!");
        }
        // switch camera need another setting's preview size.
        mCurrentMode.prepareSurfaceBeforeOpenCamera();
        mCurrentRepeatingRequest = RequestType.PREVIEW;
        mCameraDeviceManager.open(mCameraId, mCameraStateCallback,
                mCameraSessionCallback, mCameraHandler);
    }

    private void closeCamera() {
        Log.i(TAG, "closeCamera");
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        mPreviewSurfaceIsReadyForOpen = false;
    }

    private void switchCamera() {
        closeCamera();
        mCameraId = String.valueOf(mPendingSwitchCameraId);
        openCamera();
    }

    private RequestType getDefaultRepeatingRequest() {
        RequestType requiredRequestType = RequestType.PREVIEW;
        if (mCurrentRepeatingRequest == RequestType.RECORDING) {
            requiredRequestType = RequestType.RECORDING;
        }
        return requiredRequestType;
    }

    private void sendRequestChanging(RequestType requestType,
            CaptureType captureType, boolean sync) {
        if (mCameraDevice == null) {
            Log.i(TAG, "requestChangeCaptureRequets but CameraDevice is null!!!");
            return;
        }
        if (RequestType.RECORDING == requestType || RequestType.PREVIEW == requestType) {
            mCurrentRepeatingRequest = requestType;
        }
        mCameraDevice.requestChangeCaptureRequets(sync, requestType, captureType);
    }
}
