/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.mediatek.camera.addition.objecttracking;

import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.Face;
import android.view.View;

import com.android.camera.R;
import com.mediatek.camera.ICameraContext;
import com.mediatek.camera.ICameraMode.ActionType;
import com.mediatek.camera.ICameraMode.CameraModeType;
import com.mediatek.camera.addition.CameraAddition;
import com.mediatek.camera.platform.ICameraAppUi.SpecViewType;
import com.mediatek.camera.platform.ICameraDeviceManager.ICameraDevice.OtListener;
import com.mediatek.camera.platform.ICameraView;
import com.mediatek.camera.setting.SettingConstants;
import com.mediatek.camera.util.Log;
import com.mediatek.camera.util.Util;

/* A class that handles Object tracking start/stop flow
 *
 */
public class ObjectTracking extends CameraAddition implements OtListener {
    private static final String TAG = "ObjectTracking";
    
    public static final int OBJECT_TRACKING_SUCCEED = 100;
    public static final int OBJECT_TRACKING_FAILED = 50;
    
    public static final int ORITATION_CHANGED = 1;
    public static final int UPDATE_OT_FRAME = 2;
    public static final int UPDATE_DISPLAY_ORIENTATION = 3;
    public static final int START_ANIMATION = 4;
    public static final int SET_VIEW_VISIBILITE = 5;
    public static final int SET_PREVIEW_WIDTH_HEIGHT = 6;
    public static final int PREVIEW_SIZE_CHANGED = 7;
    public static final int COMPESATION_CHANGED = 8;
    public static final int UNCROP_PREVIEW_SIZE = 9;
    public static final int UPDATE_VARIABLE_FOR_RESTART = 10;
    public static final int REMOVE_RESET_EVENT = 11;
    
    private static final String FEATURE_ON = "on";
    
    private static final int MAX_TOAST_TIMES = 3;
    private boolean mIsSupportIndicator;
    private boolean mIsObjectTrackingStarted;
    private boolean mIsParameterReady;
    private boolean mIsTakePicture;
    private String mFDStatusBackup;
    
    private int mPreviewWidth;
    private int mPreviewHeight;
    private int mUnCropWidth; // The width of Display
    private int mUnCropHeight;     // The height of Display
    private Matrix mObjextMatrix;
    private ICameraView mICameraView;
    private State mCurState = State.STATE_INIT;
    private PreviewState mPreviewState = PreviewState.STATE_STOPPED;
    private int mToastTimes;
    
    private static final int X = 1;
    private static final int Y = 2;
    private static final int INIT_VALUE = -2000;
    
    private float mOldX = INIT_VALUE;
    private float mOldY = INIT_VALUE;
    
    private enum State {
        STATE_INIT, STATE_OPENED,
    }
    
    private enum PreviewState {
        STATE_STOPPED, STATE_STARTED,
    }
    
    public ObjectTracking(ICameraContext context) {
        super(context);
        Log.i(TAG, "[ObjectTracking]constructor...");
        
        mICameraView = mICameraAppUi.getCameraView(SpecViewType.ADDITION_OBJECT_TRACKING);
        if (mICameraView != null) {
            mICameraView.init(mActivity, mICameraAppUi, mIModuleCtrl);
        }
        mObjextMatrix = new Matrix();
    }
    
    @Override
    public void open() {
        Log.i(TAG, "[open]...");
        mCurState = State.STATE_OPENED;
        Log.d(TAG, "[open]mIsSupportIndicator:" + mIsSupportIndicator);
    }
    
    @Override
    public boolean isOpen() {
        boolean isOpen = false;
        if (State.STATE_INIT != mCurState) {
            isOpen = true;
        }
        Log.d(TAG, "[isOpen] isOpen:" + isOpen);
        return isOpen;
    }
    
    @Override
    public void close() {
        Log.i(TAG, "[close]...");
        stopOT();
        mCurState = State.STATE_INIT;
        mToastTimes = 0;
        mIsParameterReady = false;
    }
    
    @Override
    public boolean isSupport() {
        Log.i(TAG, "[isSupport]...mIsParameterReady = " + mIsParameterReady);
        if (!mIsParameterReady) {
            return false;
        }
        updateCameraDevice();
        mIsSupportIndicator = isSupportOtCases();
        return  mIsSupportIndicator;
    }
    
    @Override
    public boolean execute(ActionType type, Object... arg) {
        Log.i(TAG, "[execute] ActionType = " + type + "mIsObjectTrackingStarted = "
                + mIsObjectTrackingStarted + ", mIsParameterReady = " 
                + mIsParameterReady + ", mPreviewState = " + mPreviewState);
        switch (type) {
        case ACTION_ON_LONG_PRESS:
            if (!mIsParameterReady || PreviewState.STATE_STARTED != mPreviewState) {
                return false;
            }
            stopOT();
            startOT(arg[X], arg[Y]);
            break;
        
        case ACTION_ORITATION_CHANGED:
            if (mIsSupportIndicator) {
                mICameraView.update(ORITATION_CHANGED, (Integer) arg[0]);
            }
            break;
        
        case ACTION_ON_COMPENSATION_CHANGED:
            if (mIsSupportIndicator) {
                mICameraView.update(COMPESATION_CHANGED, (Integer) arg[0]);
            }
            break;
        
        case ACTION_ON_CAMERA_PARAMETERS_READY:
            if (!(Boolean)arg[0]) {
                mPreviewState = PreviewState.STATE_STARTED;
            }
            mIsParameterReady = true;
            if (mIsObjectTrackingStarted) {
                mICameraView.reset();
            }
            stopOT();
            break;
            
        case ACTION_ON_SETTING_BUTTON_CLICK:
        case ACTION_ON_FULL_SCREEN_CHANGED:
        case ACTION_SWITCH_DEVICE:
            if (mIsSupportIndicator) {
                stopOT();
            }
            break;
            
        case ACTION_ON_SINGLE_TAP_UP:
            if (mIsSupportIndicator) {
                if (mToastTimes < MAX_TOAST_TIMES) {
                    mICameraAppUi.showToast(R.string.object_track_enable_toast);
                    mToastTimes++;
                }
                stopOT();
            }
            break;
        
        case ACTION_ON_PREVIEW_DISPLAY_SIZE_CHANGED:
            Log.i(TAG, "[execute] ActionType ACTION_ON_PREVIEW_DISPLAY_SIZE_CHANGED arg[0]" + (Integer) arg[0] + ", arg[1]" + (Integer) arg[1]);
            mPreviewWidth = (Integer) arg[0];
            mPreviewHeight = (Integer) arg[1];
            break;
            
        default:
            break;
        }
        return false;
    }
    
    @Override
    public boolean execute(AdditionActionType type, Object... arg) {
        Log.i(TAG, "[execute] AdditionActionType = " + type + "mIsObjectTrackingStarted = "
                + mIsObjectTrackingStarted);
        switch (type) {
        case ACTION_TAKEN_PICTURE:// take picture
            if (mIsSupportIndicator && mIsObjectTrackingStarted) {
                mICameraView.reset();
                mIsTakePicture = true;
            }
            mIsObjectTrackingStarted = false;
            mISettingCtrl.onSettingChanged(SettingConstants.KEY_OBJECT_TRACKING, "off");
            return false;
            
        case ACTION_ON_STOP_PREVIEW:
            if (mIsSupportIndicator && mIsObjectTrackingStarted) {
                mICameraView.reset();
                //Restore face detection status.
                if (mICameraDeviceManager.getCurrentCameraId() != mICameraDeviceManager.getFrontCameraId()) {
                    mISettingCtrl.onSettingChanged(SettingConstants.KEY_CAMERA_FACE_DETECT, mFDStatusBackup);
                }
            }
            if (!mIsObjectTrackingStarted
                    && PreviewState.STATE_STARTED == mPreviewState && !mIsTakePicture) {
                mOldX = INIT_VALUE;
            }
            mIsObjectTrackingStarted = false;
            mISettingCtrl.onSettingChanged(SettingConstants.KEY_OBJECT_TRACKING, "off");
            mPreviewState = PreviewState.STATE_STOPPED;
            return false;
            
        case ACTION_ON_START_PREVIEW:
            Log.i(TAG, "[execute] ACTION_ON_START_PREVIEW mOldX = " + mOldX + ", isSupportOtCases()" + isSupportOtCases());
            if (needRestartOt()) {
                stopOT();
                reStartOT(mOldX, mOldY);
            } else {
                mIModuleCtrl.startFaceDetection();
            }
            mPreviewState = PreviewState.STATE_STARTED;
            return false;
            
        case ACTION_ON_SWITCH_PIP:
            stopOT();
            return false;
            
        default:
            return false;
        }
    }
    
    @Override
    public void resume() {
    }
    
    @Override
    public void pause() {
        Log.i(TAG, "[pause]... mIsSupportIndicator = " + mIsSupportIndicator);
        if (mIsSupportIndicator) {
            stopOT();
        }
        mCurState = State.STATE_INIT;
        mOldX = INIT_VALUE;
        mToastTimes = 0;
        mIsParameterReady = false;
    }
    
    /**
     * object tracking callback data.
     * 
     * @param face
     *            face data
     * @param camera
     */
    public void onObjectTracking(Face face,Camera camera) {
        if (face == null
                || (face.score != OBJECT_TRACKING_SUCCEED && face.score != OBJECT_TRACKING_FAILED)) {
            Log.i(TAG, "[onObjectTracking]face = " + face);
            stopOT();
            return;
        }
        if (mCurState != State.STATE_INIT) {
            mICameraView.update(UPDATE_OT_FRAME, face);
            mOldX = calculateMiddlePoint(face.rect.left, face.rect.right);
            mOldY = calculateMiddlePoint(face.rect.top, face.rect.bottom);
        }
    }
    
    
    /**
     * Start object tracking.
     * 
     * @param arg0
     *            X
     * @param arg1
     *            Y
     */
    private void startOT(Object arg0, Object arg1) {
        Log.i(TAG, "[startOT] mIsObjectTrackingStarted = " + mIsObjectTrackingStarted);
        if (mIsObjectTrackingStarted || !isSupportOtCases()) {
            return;
        }
        int x = (Integer) arg0;
        int y = (Integer) arg1;
        if (x > mPreviewWidth || y > mPreviewHeight) {
            return;
        }
        mICameraView.update(REMOVE_RESET_EVENT);
        mICameraDevice.setObjectTrackingListener(this);
        Log.i(TAG, "[StartOT] x = " + x + ", y = " + y);
        int[] pt = init(x, y);
        if (mIsSupportIndicator) {
            mICameraView.update(UPDATE_DISPLAY_ORIENTATION, mIModuleCtrl.getDisplayOrientation());
        }
        //Record face detection status before stop FD.
        mFDStatusBackup = getFaceDetectStatus();
        mISettingCtrl.onSettingChanged(SettingConstants.KEY_CAMERA_FACE_DETECT,"off");
        mISettingCtrl.onSettingChanged(SettingConstants.KEY_OBJECT_TRACKING, "on");
        mIModuleCtrl.stopFaceDetection();
        mICameraDevice.startObjectTracking(pt[0], pt[1]);
        if (mIsSupportIndicator) {
            mICameraView.update(START_ANIMATION, x, y);
        }
        mIsObjectTrackingStarted = true;
    }
    
    private int[] init(int x, int y) {
        setMatrix();
        int[] pt = calculateTapPoint(x, y);
        mICameraView.show();
        mICameraView.update(SET_VIEW_VISIBILITE, View.VISIBLE);
        mICameraView.update(SET_PREVIEW_WIDTH_HEIGHT, mPreviewWidth, mPreviewHeight);
        calculateUnCropWidthAndHeight(mPreviewWidth, mPreviewHeight);
        mICameraView.update(UNCROP_PREVIEW_SIZE, mUnCropWidth, mUnCropHeight);
        Log.i(TAG, "pt[0]" + pt[0] + ", pt[1]" + pt[1]);
        return pt;
    }
    
    private void reStartOT(float x, float y) {
        Log.i(TAG, "[reStartOT] mIsObjectTrackingStarted = " + mIsObjectTrackingStarted);
        if (mIsObjectTrackingStarted || !isSupportOtCases()) {
            return;
        }
        mICameraView.update(REMOVE_RESET_EVENT);
        // readdView to Preview frame layout
        init(0, 0);
        mICameraDevice.setObjectTrackingListener(this);
        Log.i(TAG, "[reStartOT] x = " + x + ", y = " + y);
        if (mIsSupportIndicator) {
            mICameraView.update(UPDATE_VARIABLE_FOR_RESTART);
            mICameraView.update(UPDATE_DISPLAY_ORIENTATION, mIModuleCtrl.getDisplayOrientation());
        }
        mISettingCtrl.onSettingChanged(SettingConstants.KEY_CAMERA_FACE_DETECT,"off");
        mISettingCtrl.onSettingChanged(SettingConstants.KEY_OBJECT_TRACKING, "on");
        mIModuleCtrl.stopFaceDetection();
        mICameraDevice.startObjectTracking((int)x, (int)y);
        mIsObjectTrackingStarted = true;
    }
    
    /**
     * Stop object tracking.
     * 
     * @param arg0
     *            X
     * @param arg1
     *            Y
     */
    private void stopOT() {
        Log.i(TAG, "[stopOT] mIsObjectTrackingStarted = " + mIsObjectTrackingStarted);
        if (!mIsObjectTrackingStarted) {
            return;
        }
        mICameraDevice.stopObjectTracking();
        mICameraDevice.setObjectTrackingListener(null);
        mIsObjectTrackingStarted = false;
        mICameraView.reset();
        mICameraView.uninit();
        //Restore face detection status.
        if (mICameraDeviceManager.getCurrentCameraId() != mICameraDeviceManager.getFrontCameraId()) {
            mISettingCtrl.onSettingChanged(SettingConstants.KEY_CAMERA_FACE_DETECT,mFDStatusBackup);
        }
        mISettingCtrl.onSettingChanged(SettingConstants.KEY_OBJECT_TRACKING, "off");
        mIModuleCtrl.startFaceDetection();
        mOldX = INIT_VALUE;
    }
    
    // In Object track, the inverted matrix converts the UI
    // coordinates to driver coordinates.
    private void setMatrix() {
        Log.i(TAG, "[setMatrix] mPreviewWidth " + mPreviewWidth + ", mPreviewHeight" + mPreviewHeight);
        if (mPreviewWidth != 0 && mPreviewHeight != 0) {
            Matrix objectMatrix = new Matrix();
            Util.prepareMatrix(objectMatrix, false, mIModuleCtrl.getDisplayOrientation(),
                    mPreviewWidth, mPreviewHeight);
            objectMatrix.invert(mObjextMatrix);
        }
    }
    
    private int[] calculateTapPoint(int x, int y) {
        float[] pts = new float[2];
        pts[0] = (float) x;
        pts[1] = (float) y;
        mObjextMatrix.mapPoints(pts);
        return Util.pointFToPoint(pts);
    }
    
    // check the cases support ot.
    private boolean isSupportOtCases() {
        Log.i(TAG, "[isSupportOtCase] mISettingCtrl" + mISettingCtrl);
        boolean featureSupport = false;
        boolean deviceReady = false;
        featureSupport = !(FEATURE_ON.equals(mISettingCtrl
                .getSettingValue(SettingConstants.KEY_SMILE_SHOT))
                || FEATURE_ON.equals(mISettingCtrl
                        .getSettingValue(SettingConstants.KEY_GESTURE_SHOT))
                || FEATURE_ON.equals(mISettingCtrl
                        .getSettingValue(SettingConstants.KEY_HDR))
                || FEATURE_ON.equals(mISettingCtrl
                        .getSettingValue(SettingConstants.KEY_ASD))
                || FEATURE_ON.equals(mISettingCtrl
                        .getSettingValue(SettingConstants.KEY_PANORAMA))
                || FEATURE_ON.equals(mISettingCtrl
                        .getSettingValue(SettingConstants.KEY_SLOW_MOTION)));
        deviceReady = mICameraDeviceManager.getCurrentCameraId() != mICameraDeviceManager.getFrontCameraId()
                && mIModuleCtrl.isNonePickIntent()
                && mICameraDevice != null && mICameraDevice.getParameters() != null
                && mICameraDevice.getParameters().getMaxNumDetectedObjects() > 0
                && !("true".equals(mICameraDevice.getParameters().get(Util.KEY_VIDEO_FACE_BEAUTY)));
        Log.i(TAG, "[isSupportOtCase] featureSupport = " + featureSupport + ", deviceReady = " + deviceReady);
        return featureSupport && deviceReady;
    }
    
    
    private float calculateMiddlePoint(float x, float y) {
        return x + (y - x) / 2;
    }
    
    private void calculateUnCropWidthAndHeight(int width, int height) {
        if (mICameraDevice.getParameters() != null && mICameraDevice.getParameters().getPreviewSize() != null) {
            int w = mICameraDevice.getParameters().getPreviewSize().width;
            int h = mICameraDevice.getParameters().getPreviewSize().height;
            double mAspectRatio = (double) w / h;
            if (width > height) {
                mUnCropWidth = Math.max(width, (int) (height * mAspectRatio));
                mUnCropHeight = Math.max(height, (int) (width / mAspectRatio));
            } else {
                mUnCropWidth = Math.max(width, (int) (height / mAspectRatio));
                mUnCropHeight = Math.max(height, (int) (width * mAspectRatio));
            }
        } else {
            // 4:3 mode,parameter is null, then will invoke the following code
            mUnCropWidth = width;
            mUnCropHeight = height;
        }
        if (((mUnCropHeight > mUnCropWidth) && ((mIModuleCtrl.getDisplayOrientation() == 0) || (mIModuleCtrl.getDisplayOrientation() == 180)))
                || ((mUnCropHeight < mUnCropWidth) && ((mIModuleCtrl.getDisplayOrientation() == 90) || (mIModuleCtrl.getDisplayOrientation() == 270)))) {
            int temp = mUnCropWidth;
            mUnCropWidth = mUnCropHeight;
            mUnCropHeight = temp;
        }
        Log.i(TAG, "[calculateUnCropWidthAndHeight] mUnCropWidth = " + mUnCropWidth + ", mUnCropHeight" + mUnCropHeight);
    }
    
    private String getFaceDetectStatus() {
        String faceDetection = mISettingCtrl
                .getSettingValue(SettingConstants.KEY_CAMERA_FACE_DETECT);
        Log.d(TAG, "[getFaceDetectStatus]faceDetection = " + faceDetection);
        return faceDetection;
    }
    
    private boolean needRestartOt() {
        boolean need  = false;
        if (mOldX != INIT_VALUE && isSupportOtCases() && mIModuleCtrl != null) {
            need = (mIModuleCtrl.getPrevMode() == CameraModeType.EXT_MODE_PHOTO 
                    && (mIModuleCtrl.getNextMode() == CameraModeType.EXT_MODE_PHOTO
                    || mIModuleCtrl.getNextMode() == CameraModeType.EXT_MODE_VIDEO));
            if (mIsTakePicture) {
                need = true;
            }
            Log.d(TAG, "[needRestartOt]mIModuleCtrl.getPrevMode() = " + mIModuleCtrl.getPrevMode()
                    + ", mIModuleCtrl.getNextMode()" + mIModuleCtrl.getNextMode() + ", mIsTakePicture = "
                    + mIsTakePicture);
        }
        mIsTakePicture = false;
        Log.d(TAG, "[needRestartOt] need = " + need);
        return need;
    }
}
