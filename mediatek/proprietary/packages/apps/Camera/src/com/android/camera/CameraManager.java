/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.camera;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.AsdCallback;
import android.hardware.Camera.AutoRamaCallback;
import android.hardware.Camera.AutoRamaMoveCallback;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.AutoFocusMoveCallback;
import android.hardware.Camera.ContinuousShotCallback;
import android.hardware.Camera.DistanceInfoCallback;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.FaceDetectionListener;
import android.hardware.Camera.FbOriginalCallback;
import android.hardware.Camera.HdrOriginalCallback;
import android.hardware.Camera.GestureCallback;
import android.hardware.Camera.OnZoomChangeListener;
import android.hardware.Camera.ObjectTrackingListener;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.SmileCallback;
import android.hardware.Camera.ZSDPreviewDone;
import android.hardware.Camera.StereoCameraJpsCallback;
import android.hardware.Camera.StereoCameraMaskCallback;
import android.hardware.Camera.StereoCameraWarningCallback;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Log;
import android.view.SurfaceHolder;

import static com.android.camera.Util.assertError;
import com.mediatek.camera.util.CameraPerformanceTracker;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class CameraManager {
    private static final String TAG = "CameraManager";
    private String mSubTag = TAG;
    // private static CameraManager sCameraManager = new CameraManager();

    private Parameters mParameters;
    private boolean mParametersIsDirty;
    private IOException mReconnectException;

    private static final int RELEASE = 1;
    private static final int RECONNECT = 2;
    private static final int UNLOCK = 3;
    private static final int LOCK = 4;
    private static final int SET_PREVIEW_TEXTURE_ASYNC = 5;
    private static final int START_PREVIEW_ASYNC = 6;
    private static final int STOP_PREVIEW = 7;
    private static final int SET_PREVIEW_CALLBACK_WITH_BUFFER = 8;
    private static final int ADD_CALLBACK_BUFFER = 9;
    private static final int AUTO_FOCUS = 10;
    private static final int CANCEL_AUTO_FOCUS = 11;
    private static final int SET_AUTO_FOCUS_MOVE_CALLBACK = 12;
    private static final int SET_DISPLAY_ORIENTATION = 13;
    private static final int SET_ZOOM_CHANGE_LISTENER = 14;
    private static final int SET_FACE_DETECTION_LISTENER = 15;
    private static final int START_FACE_DETECTION = 16;
    private static final int STOP_FACE_DETECTION = 17;
    private static final int SET_ERROR_CALLBACK = 18;
    private static final int SET_PARAMETERS = 19;
    private static final int GET_PARAMETERS = 20;
    private static final int SET_PARAMETERS_ASYNC = 21;
    private static final int SET_HDR_ORIGINAL_CALLBACK = 23;
    private static final int SET_FB_ORIGINAL_CALLBACK = 24;
    private static final int START_OBJECT_TRACKING = 25;
    private static final int STOP_OBJECT_TRACKING = 26;
    private static final int SET_OBJECT_TRACKING_LISTENER = 27;
    private static final int SET_GESTURE_CALLBACK = 28;
    private static final int START_GD_PREVIEW = 29;
    private static final int CANCEL_GD_PREVIEW = 30;
    private static final int SET_ZSD_CAN_TAKE_CALLBACK = 31;
    // /M: JB migration start @{
    private static final int START_SMOOTH_ZOOM = 100;
    private static final int SET_AUTORAMA_CALLBACK = 101;
    private static final int SET_AUTORAMA_MV_CALLBACK = 102;
    private static final int START_AUTORAMA = 103;
    private static final int STOP_AUTORAMA = 104;
    private static final int SET_ASD_CALLBACK = 108;
    private static final int SET_SMILE_CALLBACK = 109;
    private static final int START_SD_PREVIEW = 110;
    private static final int CANCEL_SD_PREVIEW = 111;
    private static final int CANCEL_CONTINUOUS_SHOT = 112;
    private static final int SET_CONTINUOUS_SHOT_SPEED = 113;
    private static final int SET_PREVIEW_DONE_CALLBACK = 114;
    private static final int SET_CSHOT_DONE_CALLBACK = 115;
    private static final int ADD_RAW_IMAGE_CALLBACK_BUFFER = 116;
    // / @}
    private static final int SET_STEREO3D_MODE = 117;
    private static final int START_3D_SHOT = 118;
    private static final int STOP_3D_SHOT = 119;
    //
    private static final int SET_CONTINUOUS_SHOT_STATE = 122;
    // / @}
    private static final int SET_PREVIEW_SURFACEHOLDER_ASYNC = 124;

    private static final int SET_STEREO_CAMERA_JPS_CALLBACK = 125;
    private static final int SET_STEREO_CAMERA_WARNING_CALLBACK = 126;
    private static final int SET_STEREO_CAMERA_MASK_CALLBACK = 127;
    private static final int SET_STEREO_CAMERA_DISTANCE_CALLBACK = 128;
    private static final int SET_ONE_SHOT_PREVIEW_CALLBACK = 129;
    private static final int SET_MAIN_FACE_COORDINATE = 130;
    private static final int CANCEL_MAIN_FACE_INFO = 131;
    private Handler mCameraHandler;
    private CameraProxy mCameraProxy;
    private ICamera mCamera;

    // Used to retain a copy of Parameters for setting parameters.
    private Parameters mParamsToSet;

    // public static CameraManager instance() {
    // return sCameraManager;
    // }

    private Object mFaceDetectionSync = new Object();
    private boolean mFaceDetectionRunning = false;

    public CameraManager(String subTag) {
        Log.i(TAG, "[CameraManager]constructor,subTag = " + subTag);
        mSubTag = mSubTag + "/" + subTag;
        HandlerThread ht = new HandlerThread("Camera Handler" + subTag + " Thread");
        ht.start();
        mCameraHandler = new CameraHandler(ht.getLooper());
    }

    private static String getMsgLabel(int msg) {
        switch (msg) {
        case RELEASE:
            return "[release] ";
        case RECONNECT:
            return "[reconnect] ";
        case UNLOCK:
            return "[unlock] ";
        case LOCK:
            return "[lock] ";
        case SET_PREVIEW_TEXTURE_ASYNC:
            return "[setPreviewTexture] ";
        case START_PREVIEW_ASYNC:
            return "[startPreviewAsync] ";
        case STOP_PREVIEW:
            return "[stopPreview] ";
        case SET_PREVIEW_CALLBACK_WITH_BUFFER:
            return "[setPreviewCallbackWithBuffer] ";
        case ADD_CALLBACK_BUFFER:
            return "[addCallbackBuffer] ";
        case AUTO_FOCUS:
            return "[autoFocus] ";
        case CANCEL_AUTO_FOCUS:
            return "[cancelAutoFocus] ";
        case SET_AUTO_FOCUS_MOVE_CALLBACK:
            return "[setAutoFocusMoveCallback] ";
        case SET_DISPLAY_ORIENTATION:
            return "[setDisplayOrientation] ";
        case SET_ZOOM_CHANGE_LISTENER:
            return "[setZoomChangeListener] ";
        case SET_FACE_DETECTION_LISTENER:
            return "[setFaceDetectionListener] ";
        case START_FACE_DETECTION:
            return "[startFaceDetection] ";
        case STOP_FACE_DETECTION:
            return "[stopFaceDetection] ";
        case SET_ERROR_CALLBACK:
            return "[setErrorCallback] ";
        case SET_PARAMETERS:
            return "[setParameters] ";
        case GET_PARAMETERS:
            return "[getParameters] ";
        case SET_PARAMETERS_ASYNC:
            return "[setParametersAsync] ";
        case SET_HDR_ORIGINAL_CALLBACK:
            return "[setHdrOriginalCallback] ";
        case SET_FB_ORIGINAL_CALLBACK:
            return "[setFbOriginalCallback] ";
        case START_OBJECT_TRACKING:
            return "[startObjectTracking] ";
        case STOP_OBJECT_TRACKING:
            return "[stopObjectTracking] ";
        case SET_OBJECT_TRACKING_LISTENER:
            return "[setObjectTrackingListener] ";
        case SET_GESTURE_CALLBACK:
            return "[setGestureCallback] ";
        case START_GD_PREVIEW:
            return "[startGestureDetection] ";
        case CANCEL_GD_PREVIEW:
            return "[stopGestureDetection] ";
        case START_SMOOTH_ZOOM:
            return "[startSmoothZoom] ";
        case SET_AUTORAMA_CALLBACK:
            return "[setAutoRamaCallback] ";
        case SET_AUTORAMA_MV_CALLBACK:
            return "[setAutoramraMVCallback] ";
        case START_AUTORAMA:
            return "[startAutoRama] ";
        case STOP_AUTORAMA:
            return "[stopAutoRama] ";
        case SET_ASD_CALLBACK:
            return "[setAsdCallback] ";
        case SET_SMILE_CALLBACK:
            return "[setSmileCallback] ";
        case START_SD_PREVIEW:
            return "[startSmileDetection] ";
        case CANCEL_SD_PREVIEW:
            return "[stopSmileDetection] ";
        case CANCEL_CONTINUOUS_SHOT:
            return "[cancelContinuousShot] ";
        case SET_CONTINUOUS_SHOT_SPEED:
            return "[setContinuousShotSpeed] ";
        case SET_PREVIEW_DONE_CALLBACK:
            return "[setPreviewDoneCallback] ";
        case SET_CSHOT_DONE_CALLBACK:
            return "[setContinuousShotCallback] ";
        case ADD_RAW_IMAGE_CALLBACK_BUFFER:
            return "[addRawImageCallbackBuffer] ";
        case SET_STEREO3D_MODE:
            return "[setStereo3DMode] ";
        case START_3D_SHOT:
            return "[start3DShot] ";
        case STOP_3D_SHOT:
            return "[stop3DShot] ";
        case SET_CONTINUOUS_SHOT_STATE:
            return "[setContinousShotState] ";
        case SET_PREVIEW_SURFACEHOLDER_ASYNC:
            return "[setPreviewSurfaceHolderAsync] ";
        default:
            break;
        }
        return "unknown message msg id = " + msg;
    }

    private class CameraHandler extends Handler {
        CameraHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(final Message msg) {
            long now = SystemClock.uptimeMillis();
            if (mCamera == null) {
                Log.e(mSubTag, "[handleMessage] camera device is null,return! ");
                return;
            }
            Log.i(mSubTag, "[handleMessage]msg.what = " + getMsgLabel(msg.what)
                    + " pending time = " + (now - msg.getWhen()) + "ms.");
            try {
                switch (msg.what) {
                case RELEASE:
                    CameraPerformanceTracker.onEvent(TAG,
                            CameraPerformanceTracker.NAME_CAMERA_RELEASE,
                            CameraPerformanceTracker.ISBEGIN);
                    mCamera.release();
                    Log.i(mSubTag, "release camera device = " + mCamera);
                    CameraPerformanceTracker.onEvent(TAG,
                            CameraPerformanceTracker.NAME_CAMERA_RELEASE,
                            CameraPerformanceTracker.ISEND);
                    mCamera = null;
                    mCameraProxy = null;
                    mFaceDetectionRunning = false;
                    return;

                case RECONNECT:
                    mReconnectException = null;
                    try {
                        mCamera.reconnect();
                        Log.i(mSubTag, "reconnect camera device = " + mCamera);
                    } catch (IOException ex) {
                        mReconnectException = ex;
                    }
                    mFaceDetectionRunning = false;
                    return;

                case UNLOCK:
                    mCamera.unlock();
                    return;

                case LOCK:
                    mCamera.lock();
                    return;

                case SET_PREVIEW_TEXTURE_ASYNC:
                    try {
                        mCamera.setPreviewTexture((SurfaceTexture) msg.obj);
                        Log.i(mSubTag, "setPreviewTexture camera device = " + mCamera);
                    } catch (IOException e) {
                        Log.e(mSubTag, "[handleMessage] IOException. ");
                        throw new RuntimeException(e);
                    }
                    return; // no need to call mSig.open()

                case START_PREVIEW_ASYNC:
                    CameraPerformanceTracker.onEvent(TAG,
                            CameraPerformanceTracker.NAME_CAMERA_START_PREVIEW,
                            CameraPerformanceTracker.ISBEGIN);
                    mCamera.startPreview();
                    Log.i(mSubTag, " startPreview camera device = " + mCamera);
                    CameraPerformanceTracker.onEvent(TAG,
                            CameraPerformanceTracker.NAME_CAMERA_START_PREVIEW,
                            CameraPerformanceTracker.ISEND);
                    return; // no need to call mSig.open()

                case STOP_PREVIEW:
                    CameraPerformanceTracker.onEvent(TAG,
                            CameraPerformanceTracker.NAME_CAMERA_STOP_PREVIEW,
                            CameraPerformanceTracker.ISBEGIN);
                    mCamera.stopPreview();
                    Log.i(mSubTag, "stopPreview camera device = " + mCamera);
                    CameraPerformanceTracker.onEvent(TAG,
                            CameraPerformanceTracker.NAME_CAMERA_STOP_PREVIEW,
                            CameraPerformanceTracker.ISEND);
                    return;

                case SET_PREVIEW_CALLBACK_WITH_BUFFER:
                    mCamera.setPreviewCallbackWithBuffer((PreviewCallback) msg.obj);
                    return;

                case ADD_CALLBACK_BUFFER:
                    mCamera.addCallbackBuffer((byte[]) msg.obj);
                    return;

                case AUTO_FOCUS:
                    mCamera.autoFocus((AutoFocusCallback) msg.obj);
                    return;

                case CANCEL_AUTO_FOCUS:
                    mCamera.cancelAutoFocus();
                    return;

                case SET_AUTO_FOCUS_MOVE_CALLBACK:
                    mCamera.setAutoFocusMoveCallback((AutoFocusMoveCallback) msg.obj);
                    return;

                case SET_ZSD_CAN_TAKE_CALLBACK:
                    mCamera.setUncompressedImageCallback((PictureCallback) msg.obj);
                    return;

                case SET_DISPLAY_ORIENTATION:
                    mCamera.setDisplayOrientation(msg.arg1);
                    return;

                case SET_ZOOM_CHANGE_LISTENER:
                    mCamera.setZoomChangeListener((OnZoomChangeListener) msg.obj);
                    return;

                case SET_FACE_DETECTION_LISTENER:
                    mCamera.setFaceDetectionListener((FaceDetectionListener) msg.obj);
                    return;

                case START_FACE_DETECTION:
                    mCamera.startFaceDetection();
                    return;

                case STOP_FACE_DETECTION:
                    mCamera.stopFaceDetection();
                    return;

                case SET_ERROR_CALLBACK:
                    mCamera.setErrorCallback((ErrorCallback) msg.obj);
                    return;

                case SET_PARAMETERS:
                    CameraPerformanceTracker.onEvent(TAG,
                            CameraPerformanceTracker.NAME_SET_PARAMETERS,
                            CameraPerformanceTracker.ISBEGIN);
                    mParametersIsDirty = true;
                    mParamsToSet.unflatten((String) msg.obj);
                    mCamera.setParameters(mParamsToSet);
                    CameraPerformanceTracker.onEvent(TAG,
                            CameraPerformanceTracker.NAME_SET_PARAMETERS,
                            CameraPerformanceTracker.ISEND);
                    return;

                case GET_PARAMETERS:
                    CameraPerformanceTracker.onEvent(TAG,
                            CameraPerformanceTracker.NAME_GET_PARAMETERS,
                            CameraPerformanceTracker.ISBEGIN);
                    if (mParametersIsDirty) {
                        mParameters = mCamera.getParameters();
                        mParametersIsDirty = false;
                    }
                    CameraPerformanceTracker.onEvent(TAG,
                            CameraPerformanceTracker.NAME_GET_PARAMETERS,
                            CameraPerformanceTracker.ISEND);
                    return;

                case SET_PARAMETERS_ASYNC:
                    CameraPerformanceTracker.onEvent(TAG,
                            CameraPerformanceTracker.NAME_SET_PARAMETERS,
                            CameraPerformanceTracker.ISBEGIN);
                    mParametersIsDirty = true;
                    mParamsToSet.unflatten((String) msg.obj);
                    mCamera.setParameters(mParamsToSet);
                    CameraPerformanceTracker.onEvent(TAG,
                            CameraPerformanceTracker.NAME_SET_PARAMETERS,
                            CameraPerformanceTracker.ISEND);
                    return; // no need to call mSig.open()

                case START_SMOOTH_ZOOM:
                    mCamera.startSmoothZoom(msg.arg1);
                    return;
                case SET_AUTORAMA_CALLBACK:
                    mCamera.setAutoRamaCallback((AutoRamaCallback) msg.obj);
                    return;
                case SET_AUTORAMA_MV_CALLBACK:
                    mCamera.setAutoRamaMoveCallback((AutoRamaMoveCallback) msg.obj);
                    return;
                case START_AUTORAMA:
                    mCamera.startAutoRama(msg.arg1);
                    return;
                case STOP_AUTORAMA:
                    mCamera.stopAutoRama(msg.arg1);
                    return;
                case SET_ASD_CALLBACK:
                    mCamera.setAsdCallback((AsdCallback) msg.obj);
                    return;
                case SET_SMILE_CALLBACK:
                    mCamera.setSmileCallback((SmileCallback) msg.obj);
                    return;
                case START_SD_PREVIEW:
                    mCamera.startSmileDetection();
                    return;
                case CANCEL_SD_PREVIEW:
                    mCamera.stopSmileDetection();
                    return;
                case CANCEL_CONTINUOUS_SHOT:
                    mCamera.cancelContinuousShot();
                    return;
                case SET_CONTINUOUS_SHOT_SPEED:
                    mCamera.setContinuousShotSpeed(msg.arg1);
                    return;
                case SET_PREVIEW_DONE_CALLBACK:
                    mCamera.setPreviewDoneCallback((ZSDPreviewDone) msg.obj);
                    return;
                case SET_CSHOT_DONE_CALLBACK:
                    mCamera.setContinuousShotCallback((ContinuousShotCallback) msg.obj);
                    break;
                case ADD_RAW_IMAGE_CALLBACK_BUFFER:
                    mCamera.addRawImageCallbackBuffer((byte[]) msg.obj);
                    return;
                case START_OBJECT_TRACKING:
                    mCamera.startObjectTracking(msg.arg1, msg.arg2);
                    return;
                case STOP_OBJECT_TRACKING:
                    mCamera.stopObjectTracking();
                    return;
                case SET_OBJECT_TRACKING_LISTENER:
                    mCamera.setObjectTrackingListener((ObjectTrackingListener) msg.obj);
                    return;

                case SET_STEREO3D_MODE:
                    mCamera.setPreview3DModeForCamera(((Boolean) msg.obj).booleanValue());
                    return;
                case START_3D_SHOT:
                    mCamera.start3DSHOT(msg.arg1);
                    return;
                case STOP_3D_SHOT:
                    mCamera.stop3DSHOT(msg.arg1);
                    return;
                case SET_HDR_ORIGINAL_CALLBACK:
                    mCamera.setHdrOriginalCallback((HdrOriginalCallback) msg.obj);
                    return;
                case SET_FB_ORIGINAL_CALLBACK:
                    mCamera.setFbOriginalCallback((FbOriginalCallback) msg.obj);
                    return;
                case SET_GESTURE_CALLBACK:
                    mCamera.setGestureCallback((GestureCallback) msg.obj);
                    return;
                case START_GD_PREVIEW:
                    mCamera.startGestureDetection();
                    return;
                case CANCEL_GD_PREVIEW:
                    mCamera.stopGestureDetection();
                    return;
                case SET_STEREO_CAMERA_JPS_CALLBACK:
                    mCamera.setJpsCallback((StereoCameraJpsCallback) msg.obj);
                    return;
                case SET_STEREO_CAMERA_WARNING_CALLBACK:
                    mCamera.setWarningCallback((StereoCameraWarningCallback) msg.obj);
                    return;
                case SET_STEREO_CAMERA_MASK_CALLBACK:
                    mCamera.setMaskCallback((StereoCameraMaskCallback) msg.obj);
                    return;
                case SET_STEREO_CAMERA_DISTANCE_CALLBACK:
                    mCamera.setDistanceInfoCallback((DistanceInfoCallback) msg.obj);
                    return;
                case SET_PREVIEW_SURFACEHOLDER_ASYNC:
                    try {
                        mCamera.setPreviewDisplay((SurfaceHolder) msg.obj);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break; // no need to call mSig.open()
                case SET_ONE_SHOT_PREVIEW_CALLBACK:
                    mCamera.setOneShotPreviewCallback((PreviewCallback) msg.obj);
                    break;
                case SET_MAIN_FACE_COORDINATE:
                    mCamera.setMainFaceCoordinate(msg.arg1, msg.arg2);
                    break;
                case CANCEL_MAIN_FACE_INFO:
                    mCamera.cancelMainFaceInfo();
                    break;
                default:
                    throw new RuntimeException("Invalid CameraProxy message=" + msg.what);
                }
            } catch (RuntimeException e) {
                if (msg.what != RELEASE && mCamera != null) {
                    try {
                        Log.e(TAG, "[handleMessgae]release the camera.");
                        mCamera.release();
                    } catch (Exception ex) {
                        Log.e(TAG, "Fail to release the camera.");
                    }
                    mCamera = null;
                    mCameraProxy = null;
                }
                throw e;
            }
        }
    }

    //TODO will remove when native support cam hal version 1.0 legacy mode
    private static int sTrySwitchToLegacyMode = SystemProperties.getInt("mtk.camera.app.legacy", 0);
    public static ICamera openCamera(int cameraId) {
            Camera camera = null;
            if (sTrySwitchToLegacyMode > 0) {
                   // choose legacy mode in order to enter cam hal 1.0
                   camera = Camera.openLegacy(cameraId, Camera.CAMERA_HAL_API_VERSION_1_0);
               } else {
                   camera = Camera.open(cameraId);
               }
            if (null == camera) {
                Log.e(TAG, "openCamera:got null hardware camera!");
                return null;
            }
            // wrap it with ICamera
            return new AndroidCamera(camera);
    }

    // Open camera synchronously. This method is invoked in the context of a
    // background thread.
    CameraProxy cameraOpen(int cameraId) {
        // Cannot open camera in mCameraHandler, otherwise all camera events
        // will be routed to mCameraHandler looper, which in turn will call
        // event handler like Camera.onFaceDetection, which in turn will modify
        // UI and cause exception like this:
        // CalledFromWrongThreadException: Only the original thread that created
        // a view hierarchy can touch its views.
        CameraPerformanceTracker.onEvent(TAG,
                CameraPerformanceTracker.NAME_CAMERA_OPEN,
                CameraPerformanceTracker.ISBEGIN);
        mCamera = openCamera(cameraId);
        Log.i(mSubTag, "openCamera cameraId = " + cameraId + " camera device = " + mCamera);
        CameraPerformanceTracker.onEvent(TAG,
                CameraPerformanceTracker.NAME_CAMERA_OPEN,
                CameraPerformanceTracker.ISEND);
        if (mCamera != null) {
            mParametersIsDirty = true;
            if (mParamsToSet == null) {
                mParamsToSet = mCamera.getParameters();
            }
            mCameraProxy = new CameraProxy();
            return mCameraProxy;
        } else {
            return null;
        }
    }

    public class CameraProxy {
        private CameraProxy() {
            assertError(mCamera != null);
        }

        public ICamera getCamera() {
            return mCamera;
        }

        public void release() {
            mCameraHandler.sendEmptyMessage(RELEASE);
            waitDone();
        }

        public void reconnect() throws IOException {
            mCameraHandler.sendEmptyMessage(RECONNECT);
            waitDone();
            if (mReconnectException != null) {
                throw mReconnectException;
            }
        }

        public void unlock() {
            mCameraHandler.sendEmptyMessage(UNLOCK);
            waitDone();
        }

        public void lock() {
            mCameraHandler.sendEmptyMessage(LOCK);
            waitDone();
        }

        public void setPreviewTextureAsync(final SurfaceTexture surfaceTexture) {
            mCameraHandler.obtainMessage(SET_PREVIEW_TEXTURE_ASYNC, surfaceTexture).sendToTarget();
        }

        public void startPreviewAsync() {
            mCameraHandler.sendEmptyMessage(START_PREVIEW_ASYNC);
            waitDone();
        }

        public void stopPreview() {
            synchronized (mFaceDetectionSync) {
                if (mFaceDetectionRunning) {
                    mCameraHandler.sendEmptyMessage(STOP_FACE_DETECTION);
                    mFaceDetectionRunning = false;
                    Log.w(TAG, "[stopPreview]Please call stopFaceDetecton firstly:",
                            new Throwable());
                }
            }
            mCameraHandler.sendEmptyMessage(STOP_PREVIEW);
            waitDone();
        }

        public void setPreviewCallbackWithBuffer(final PreviewCallback cb) {
            mCameraHandler.obtainMessage(SET_PREVIEW_CALLBACK_WITH_BUFFER, cb).sendToTarget();
        }

        public void addCallbackBuffer(byte[] callbackBuffer) {
            mCameraHandler.obtainMessage(ADD_CALLBACK_BUFFER, callbackBuffer).sendToTarget();
        }

        public void addRawImageCallbackBuffer(byte[] callbackBuffer) {
            mCameraHandler.obtainMessage(ADD_RAW_IMAGE_CALLBACK_BUFFER, callbackBuffer)
                    .sendToTarget();
        }

        public void autoFocus(AutoFocusCallback cb) {
            mCameraHandler.obtainMessage(AUTO_FOCUS, cb).sendToTarget();
        }

        public void cancelAutoFocus() {
            mCameraHandler.removeMessages(AUTO_FOCUS);
            mCameraHandler.sendEmptyMessage(CANCEL_AUTO_FOCUS);
        }

        public void setAutoFocusMoveCallback(AutoFocusMoveCallback cb) {
            mCameraHandler.obtainMessage(SET_AUTO_FOCUS_MOVE_CALLBACK, cb).sendToTarget();
        }
        public void setUncompressedImageCallback(PictureCallback cb) {
            mCameraHandler.obtainMessage(SET_ZSD_CAN_TAKE_CALLBACK, cb).sendToTarget();
        }

        public void takePictureAsync(final ShutterCallback shutter, final PictureCallback raw,
                final PictureCallback postview, final PictureCallback jpeg) {
            mCameraHandler.post(new Runnable() {
                @Override
                public void run() {
                    Log.i(mSubTag, " takePictureAsync begin");
                    synchronized (mFaceDetectionSync) {
                        if (mCamera != null) {
                            mCamera.takePicture(shutter, raw, postview, jpeg);
                            mFaceDetectionRunning = false;
                        }
                    }
                    Log.i(mSubTag, " takePictureAsync end");
                }
            });
        }

        public void takePicture(final ShutterCallback shutter, final PictureCallback raw,
                final PictureCallback postview, final PictureCallback jpeg) {
            // Too many parameters, so use post for simplicity
            mCameraHandler.post(new Runnable() {
                @Override
                public void run() {
                    Log.i(mSubTag, " takePicture begin");
                    synchronized (mFaceDetectionSync) {
                        if (mCamera != null) {
                            mCamera.takePicture(shutter, raw, postview, jpeg);
                            mFaceDetectionRunning = false;
                        }
                    }
                    Log.i(mSubTag, " takePicture end");
                }
            });
            waitDone();
        }

        public void setDisplayOrientation(int degrees) {
            mCameraHandler.obtainMessage(SET_DISPLAY_ORIENTATION, degrees, 0).sendToTarget();
        }

        public void setZoomChangeListener(OnZoomChangeListener listener) {
            mCameraHandler.obtainMessage(SET_ZOOM_CHANGE_LISTENER, listener).sendToTarget();
        }

        public void setFaceDetectionListener(FaceDetectionListener listener) {
            mCameraHandler.obtainMessage(SET_FACE_DETECTION_LISTENER, listener).sendToTarget();
        }

        public void startFaceDetection() {
            synchronized (mFaceDetectionSync) {
                if (!mFaceDetectionRunning) {
                    mCameraHandler.sendEmptyMessage(START_FACE_DETECTION);
                    mFaceDetectionRunning = true;
                } else {
                    Log.w(TAG, "[startFaceDetection]Why you call twice:", new Throwable());
                }
            }
        }

        public void stopFaceDetection() {
            synchronized (mFaceDetectionSync) {
                if (mFaceDetectionRunning) {
                    mCameraHandler.sendEmptyMessage(STOP_FACE_DETECTION);
                    mFaceDetectionRunning = false;
                } else {
                    Log.w(TAG, "[stopFaceDetection]Why you call twice:", new Throwable());
                }
            }
        }

        public boolean getFaceDetectionStatus() {
            return mFaceDetectionRunning;
        }

        public Object getFaceDetectionSyncObject() {
            return mFaceDetectionSync;
        }

        public void setObjectTrackingListener(ObjectTrackingListener listener) {
            mCameraHandler.obtainMessage(SET_OBJECT_TRACKING_LISTENER, listener).sendToTarget();
        }

        public void startObjectTracking(int x, int y) {
            mCameraHandler.obtainMessage(START_OBJECT_TRACKING, x, y).sendToTarget();
        }

        public void stopObjectTracking() {
            mCameraHandler.sendEmptyMessage(STOP_OBJECT_TRACKING);
        }

        public void setErrorCallback(ErrorCallback cb) {
            mCameraHandler.obtainMessage(SET_ERROR_CALLBACK, cb).sendToTarget();
        }

        public void setParameters(Parameters params) {
            if (params == null) {
                Log.v(TAG, "null parameters in setParameters()");
                return;
            }
            mCameraHandler.obtainMessage(SET_PARAMETERS, params.flatten()).sendToTarget();
        }

        public void setParametersAsync(Parameters params) {
            mCameraHandler.removeMessages(SET_PARAMETERS_ASYNC);
            if (params == null) {
                Log.v(TAG, "null parameters in setParameters()");
                return;
            }
            mCameraHandler.obtainMessage(SET_PARAMETERS_ASYNC, params.flatten()).sendToTarget();
        }

        public void setParametersAsync(final Parameters params, final int zoomValue) {
            // Too many parameters, so use post for simplicity
            synchronized (CameraProxy.this) {
                if (mAsyncRunnable != null) {
                    mCameraHandler.removeCallbacks(mAsyncRunnable);
                }
                mAsyncRunnable = new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "mAsyncRunnable.run(" + zoomValue + ") this=" + mAsyncRunnable
                                + ", mCamera=" + mCamera);
                        if (mCamera != null && mCameraProxy != null) {
                            if (!mCameraProxy.tryLockParametersRun(new Runnable() {
                                @Override
                                public void run() {
                                    CameraPerformanceTracker.onEvent(TAG,
                                            CameraPerformanceTracker.NAME_SET_PARAMETERS,
                                            CameraPerformanceTracker.ISBEGIN);
                                    // Here we use zoom value instead of
                                    // parameters for that:
                                    // parameters may be different from
                                    // current parameters.
                                    if (mCamera != null && params != null) {
                                        params.setZoom(zoomValue);
                                        mCamera.setParameters(params);
                                    }
                                    CameraPerformanceTracker.onEvent(TAG,
                                            CameraPerformanceTracker.NAME_SET_PARAMETERS,
                                            CameraPerformanceTracker.ISEND);
                                }
                            })) {
                                // Second async may changed the runnable,
                                // here we sync the new runnable and post it
                                // again.
                                synchronized (CameraProxy.this) {
                                    if (mAsyncRunnable != null) {
                                        mCameraHandler.removeCallbacks(mAsyncRunnable);
                                    }
                                    mCameraHandler.post(mAsyncRunnable);
                                    Log.d(TAG, "mAsyncRunnable.post " + mAsyncRunnable);
                                }
                            }
                        }
                    }
                };
                mCameraHandler.post(mAsyncRunnable);
                Log.d(TAG, "setParametersAsync(" + zoomValue + ") and mAsyncRunnable = "
                        + mAsyncRunnable);
            }
        }

        public Parameters getParameters() {
            mCameraHandler.sendEmptyMessage(GET_PARAMETERS);
            waitDone();
            return mParameters;
        }

        // /M: JB migration start @{
        public void startSmoothZoom(int zoomValue) {
            mCameraHandler.obtainMessage(START_SMOOTH_ZOOM, zoomValue, 0).sendToTarget();
            waitDone();
        }

        public void setAutoRamaCallback(AutoRamaCallback autoRamaCallback) {
            mCameraHandler.obtainMessage(SET_AUTORAMA_CALLBACK, autoRamaCallback).sendToTarget();
            waitDone();
        }

        public void setAutoRamaMoveCallback(AutoRamaMoveCallback autoRamaMoveCallback) {
            mCameraHandler.obtainMessage(SET_AUTORAMA_MV_CALLBACK, autoRamaMoveCallback)
                    .sendToTarget();
            waitDone();
        }

        public void setHdrOriginalCallback(HdrOriginalCallback hdrOriginalCallback) {
            mCameraHandler.obtainMessage(SET_HDR_ORIGINAL_CALLBACK, hdrOriginalCallback)
                    .sendToTarget();
            waitDone();
        }

        public void setFbOriginalCallback(FbOriginalCallback fbOriginalCallback) {
            mCameraHandler.obtainMessage(SET_FB_ORIGINAL_CALLBACK, fbOriginalCallback)
                    .sendToTarget();
            waitDone();
        }

        public void startAutoRama(int num) {
            mCameraHandler.obtainMessage(START_AUTORAMA, num, 0).sendToTarget();
            waitDone();
        }

        public void stopAutoRama(int isMerge) {
            mCameraHandler.obtainMessage(STOP_AUTORAMA, isMerge, 0).sendToTarget();
            waitDone();
        }

        public void setAsdCallback(AsdCallback asdCallback) {
            mCameraHandler.obtainMessage(SET_ASD_CALLBACK, asdCallback).sendToTarget();
            waitDone();
        }

        public void setSmileCallback(SmileCallback smileCallback) {
            mCameraHandler.obtainMessage(SET_SMILE_CALLBACK, smileCallback).sendToTarget();
            waitDone();
        }

        public void setStereoCameraJpsCallback(StereoCameraJpsCallback jpsCallback) {
            mCameraHandler.obtainMessage(SET_STEREO_CAMERA_JPS_CALLBACK, jpsCallback)
                    .sendToTarget();
            waitDone();
        }

        public void setStereoCameraMaskCallback(StereoCameraMaskCallback maskCallback) {
            mCameraHandler.obtainMessage(SET_STEREO_CAMERA_MASK_CALLBACK, maskCallback)
                    .sendToTarget();
            waitDone();
        }

        public void setStereoCameraWarningCallback(StereoCameraWarningCallback warningCallback) {
            mCameraHandler.obtainMessage(SET_STEREO_CAMERA_WARNING_CALLBACK, warningCallback)
                    .sendToTarget();
            waitDone();
        }

        public void setStereoCameraDistanceCallback(DistanceInfoCallback distanceCallback) {
            mCameraHandler.obtainMessage(SET_STEREO_CAMERA_DISTANCE_CALLBACK, distanceCallback)
                    .sendToTarget();
            waitDone();
        }

        public void startSmileDetection() {
            mCameraHandler.sendEmptyMessage(START_SD_PREVIEW);
            waitDone();
        }

        public void stopSmileDetection() {
            mCameraHandler.sendEmptyMessage(CANCEL_SD_PREVIEW);
            waitDone();
        }

        public void setGestureCallback(GestureCallback gestureCallback) {
            mCameraHandler.obtainMessage(SET_GESTURE_CALLBACK, gestureCallback).sendToTarget();
            waitDone();
        }

        public void startGestureDetection() {
            mCameraHandler.sendEmptyMessage(START_GD_PREVIEW);
            waitDone();
        }

        public void stopGestureDetection() {
            mCameraHandler.sendEmptyMessage(CANCEL_GD_PREVIEW);
            waitDone();
        }

        public void cancelContinuousShot() {
            mCameraHandler.sendEmptyMessage(CANCEL_CONTINUOUS_SHOT);
            waitDone();
        }

        public void setContinuousShotSpeed(int speed) {
            mCameraHandler.obtainMessage(SET_CONTINUOUS_SHOT_SPEED, speed, 0).sendToTarget();
            waitDone();
        }

        public void setPreviewDoneCallback(ZSDPreviewDone callback) {
            mCameraHandler.obtainMessage(SET_PREVIEW_DONE_CALLBACK, callback).sendToTarget();
            waitDone();
        }

        public void setContinuousShotCallback(ContinuousShotCallback callback) {
            mCameraHandler.obtainMessage(SET_CSHOT_DONE_CALLBACK, callback).sendToTarget();
            waitDone();
        }

        public void setPreview3DModeForCamera(boolean enable) {
            mCameraHandler.obtainMessage(SET_STEREO3D_MODE, enable).sendToTarget();
            waitDone();
        }

        public void start3DSHOT(int num) {
            mCameraHandler.obtainMessage(START_3D_SHOT, num, 0).sendToTarget();
            waitDone();
        }

        public void stop3DSHOT(int isMerge) {
            mCameraHandler.obtainMessage(STOP_3D_SHOT, isMerge, 0).sendToTarget();
            waitDone();
        }

        public void setPreviewDisplayAsync(SurfaceHolder holder) {
            mCameraHandler.obtainMessage(SET_PREVIEW_SURFACEHOLDER_ASYNC, holder).sendToTarget();
            waitDone();
        }

        public void setOneShotPreviewCallback(PreviewCallback cb) {
            mCameraHandler.obtainMessage(SET_ONE_SHOT_PREVIEW_CALLBACK, cb).sendToTarget();
        }

        public void setMainFaceCoordinate(int x, int y) {
            mCameraHandler.obtainMessage(SET_MAIN_FACE_COORDINATE, x, y).sendToTarget();
        }

        public void cancelMainFaceInfo() {
            mCameraHandler.obtainMessage(CANCEL_MAIN_FACE_INFO).sendToTarget();
        }

        // /M: lock parameter for ConcurrentModificationException. @{
        private Runnable mAsyncRunnable;
        private static final int ENGINE_ACCESS_MAX_TIMEOUT_MS = 500;
        private ReentrantLock mLock = new ReentrantLock();

        private void lockParameters() throws InterruptedException {
            Log.d(TAG, "lockParameters...");
            mLock.lock();
            Log.d(TAG, "lockParameters,done");
        }

        private void unlockParameters() {
            Log.d(TAG, "lockParameters: releasing lock");
            mLock.unlock();
        }

        private boolean tryLockParameters(long timeoutMs) throws InterruptedException {
            Log.d(TAG, "try lock: grabbing lock with timeout " + timeoutMs, new Throwable());
            boolean acquireSem = mLock.tryLock(timeoutMs, TimeUnit.MILLISECONDS);
            Log.d(TAG, "try lock: grabbed lock status " + acquireSem);

            return acquireSem;
        }

        public void lockParametersRun(Runnable runnable) {
            boolean lockedParameters = false;
            try {
                lockParameters();
                lockedParameters = true;
                runnable.run();
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParametersRun() not successfull.", ex);
            } finally {
                if (lockedParameters) {
                    unlockParameters();
                }
            }
        }

        public boolean tryLockParametersRun(Runnable runnable) {
            boolean lockedParameters = false;
            try {
                lockedParameters = tryLockParameters(ENGINE_ACCESS_MAX_TIMEOUT_MS);
                if (lockedParameters) {
                    runnable.run();
                }
            } catch (InterruptedException ex) {
                Log.e(TAG, "tryLockParametersRun() not successfull.", ex);
            } finally {
                if (lockedParameters) {
                    unlockParameters();
                }
            }
            Log.d(TAG, "tryLockParametersRun(" + runnable + ") return " + lockedParameters);
            return lockedParameters;
        }
        // / @}
    }

    // return false if cancelled.
    public boolean waitDone() {
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
            mCameraHandler.post(unlockRunnable);
            try {
                waitDoneLock.wait();
            } catch (InterruptedException ex) {
                Log.v(TAG, "waitDone interrupted");
                return false;
            }
        }
        return true;
    }

}
