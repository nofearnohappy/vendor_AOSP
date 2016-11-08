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
package com.mediatek.camera.mode;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Face;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;
import com.android.camera.Exif;
import com.android.camera.ExtensionHelper;
import com.android.camera.R;

import com.mediatek.camera.AdditionManager;
import com.mediatek.camera.ICameraAddition;
import com.mediatek.camera.ICameraAddition.AdditionActionType;
import com.mediatek.camera.ICameraContext;
import com.mediatek.camera.platform.ICameraAppUi.ShutterButtonType;
import com.mediatek.camera.platform.ICameraAppUi.ViewState;
import com.mediatek.camera.platform.ICameraDeviceManager.ICameraDevice.AutoFocusMvCallback;
import com.mediatek.camera.platform.IFileSaver.FILE_TYPE;
import com.mediatek.camera.platform.IFocusManager.FocusListener;
import com.mediatek.camera.setting.SettingConstants;
import com.mediatek.camera.util.Log;
import com.mediatek.camera.util.Util;

import junit.framework.Assert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class PhotoMode extends CameraMode implements FocusListener, ICameraAddition.Listener {
    private static final String TAG = "PhotoMode";

    private static final int MSG_RESTART_PREVIEW = 101;
    private static final int REQUEST_CROP = 1000;
    private static final String TEMP_CROP_FILE_NAME = "crop-temp";
    protected CameraCategory mCameraCategory;
    protected AdditionManager mAdditionManager;
    protected long mCaptureStartTime = 0;
    protected boolean mCameraClosed = false;
    private MainHandler mHandler;
    private byte[] mJpegImageData;
    private long mPostViewPictureCallbackTime = 0;
    private long mShutterCallbackTime = 0;
    private long mRawPictureCallbackTime = 0;
    private long mJpegPictureCallbackTime;
    private long mPictureDisplayedToJpegCallbackTime;

    private boolean mIsAutoFocusCallback = false;
    // record the image count in compressing stage, it will increase when onCanCapture,
    // and it will decrease when onPictureTaken(Jpeg);
    // usually,Application restartPreview in onPictureTaken(Jpeg),
    // but if application receive onCanCapture firstly, move the restartPreview to onCanCapture.
    private int mCapturedImageCount = 0;

    public PhotoMode(ICameraContext cameraContext) {
        super(cameraContext);
        Log.i(TAG, "[PhotoMode]constructor...");

        if (mIModuleCtrl.isImageCaptureIntent()) {
            mICameraAppUi.switchShutterType(ShutterButtonType.SHUTTER_TYPE_PHOTO);
        } else {
            mICameraAppUi.switchShutterType(ShutterButtonType.SHUTTER_TYPE_PHOTO_VIDEO);
        }
        mAdditionManager = cameraContext.getAdditionManager();
        mCameraCategory = new CameraCategory();
        mHandler = new MainHandler(mActivity.getMainLooper());
    }

    @Override
    public boolean open() {
        mAdditionManager.setListener(this);
        mAdditionManager.open(true);
        return true;
    }

    @Override
    public boolean close() {
        Log.i(TAG, "[close] current state = " + getModeState());
        super.close();
        mAdditionManager.close(true);
        if (ModeState.STATE_CLOSED == getModeState()) {
            return true;
        }
        stopFaceDetection();
        // force hide the FD entry icon,case the onFaceDetection not will
        // callback when have leave this mode,so the UI will always have the VFB
        // entry icon, the 0 means current FD number is zero
        onFaceDetected(0);
        return true;
    }

    @Override
    public boolean execute(ActionType type, Object... arg) {
        Log.i(TAG, "[execute]type = " + type);
        boolean result = mAdditionManager.execute(type, true, arg);
        if (type == ActionType.ACTION_ON_BACK_KEY_PRESS && result) {
            return true;
        }
        return executeAction(type, arg);
    }

    @Override
    public void autoFocus() {
        Log.i(TAG, "[autoFocus]...");
        mICameraDevice.autoFocus(mAutoFocusCallback);
        mICameraAppUi.setViewState(ViewState.VIEW_STATE_FOCUSING);
        setModeState(ModeState.STATE_FOCUSING);
    }

    @Override
    public void cancelAutoFocus() {
        Log.i(TAG, "[cancelAutoFocus]...current view state = " + mICameraAppUi.getViewState());
        if (ViewState.VIEW_STATE_CAPTURE == mICameraAppUi.getViewState()) {
            return;
        }
        mICameraDevice.cancelAutoFocus();
        if (!mISelfTimeManager.isSelfTimerCounting()
                && mICameraAppUi.getViewState() != ViewState.VIEW_STATE_LOMOEFFECT_SETTING) {
            mICameraAppUi.restoreViewState();
        }
        setFocusParameters();
        setModeState(ModeState.STATE_IDLE);
    }

    @Override
    public boolean capture() {
        Log.i(TAG, "[capture]...");

        long start = System.currentTimeMillis();
        mCaptureStartTime = System.currentTimeMillis();
        mPostViewPictureCallbackTime = 0;
        mJpegImageData = null;
        mIFileSaver.init(FILE_TYPE.JPEG, 0, null, -1);
        mICameraAppUi.setSwipeEnabled(false);
        mICameraAppUi.showRemaining();
        mCameraCategory.takePicture();
        setModeState(ModeState.STATE_CAPTURING);

        Log.d(TAG, "[capture] Capture time = " + (System.currentTimeMillis() - start));
        return true;
    }

    @Override
    public void startFaceDetection() {
        Log.i(TAG, "[startFaceDetection]...");
        mIModuleCtrl.startFaceDetection();
    }

    @Override
    public void stopFaceDetection() {
        Log.i(TAG, "[stopFaceDetection]...");
        mIModuleCtrl.stopFaceDetection();
    }

    @Override
    public void setFocusParameters() {
        Log.i(TAG, "[setFocusParameters]mIsAutoFocusCallback = " + mIsAutoFocusCallback);
        mIModuleCtrl.applyFocusParameters(!mIsAutoFocusCallback);
        mIsAutoFocusCallback = false;
    }

    @Override
    public void playSound(int soundId) {
        if (mCameraSound != null) {
            mCameraSound.play(soundId);
        }
    }

    protected final AutoFocusMvCallback mAutoFocusMoveCallback = new AutoFocusMvCallback() {
        @Override
        public void onAutoFocusMoving(boolean moving, android.hardware.Camera camera) {
            Log.i(TAG, "[onAutoFocusMoving]moving=" + moving + ",mode state=" + getModeState());
            // Don't show continuous focus ui when capturing.
            if (ModeState.STATE_CAPTURING == getModeState()) {
                return;
            }
            if ("on".equals(mISettingCtrl.getSettingValue(SettingConstants.KEY_OBJECT_TRACKING))) {
                Log.i(TAG, "[onAutoFocusMoving] objectTracking is opened");
                mIFocusManager.clearView();
                return;
            } else {
                mIFocusManager.onAutoFocusMoving(moving);
            }
        }
    };

    protected boolean executeAction(ActionType type, Object... arg) {
        switch (type) {
        case ACTION_ON_CAMERA_OPEN:
            super.updateDevice();
            mCameraClosed = false;
            break;

        case ACTION_FACE_DETECTED:
            if (arg != null) {
                Log.d(TAG, "faceLength = " + ((Face[]) arg).length);
                onFaceDetected(((Face[]) arg).length);
            }
            break;

        case ACTION_PHOTO_SHUTTER_BUTTON_CLICK:
            onShutterButtonClick();
            break;

        case ACTION_SHUTTER_BUTTON_FOCUS:
            Assert.assertTrue(arg.length == 1);
            if (arg.length < 1) {
                Log.i(TAG, "[execute] illegal parameter");
                return false;
            }
            boolean press = (Boolean) arg[0];
            onShutterButtonFocus(press);
            break;

        case ACTION_ON_CAMERA_PARAMETERS_READY:
            Assert.assertTrue(arg.length == 1);
            onCameraParameterReady((Boolean) arg[0]);
            break;

        case ACTION_ON_SINGLE_TAP_UP:
            Assert.assertTrue(arg.length == 3);
            onSingleTapUp((View) arg[0], (Integer) arg[1], (Integer) arg[2]);
            break;

        case ACTION_OK_BUTTON_CLICK:
            doAttach();
            break;

        case ACTION_ON_BACK_KEY_PRESS:
            return onBackPressed();

        case ACTION_ON_CAMERA_CLOSE:
            onCameraClose();
            break;

        case ACTION_ON_START_PREVIEW:
            Assert.assertTrue(arg.length == 1);
            startPreview((Boolean) arg[0]);
            break;

        case ACTION_ON_STOP_PREVIEW:
            stopPreview();
            break;

        case ACTION_ON_RESTORE_SETTINGS:
            mICameraAppUi.changeBackToVFBModeStatues(false);
            break;

        default:
            return false;
        }
        return true;
    }

    private boolean onBackPressed() {
        Log.i(TAG, "[onBackPressed] mCurrentState:" + getModeState());

        if (mIModuleCtrl.isImageCaptureIntent()
                && mICameraAppUi.getShutterType() == ShutterButtonType.SHUTTER_TYPE_OK_CANCEL) {
            Log.i(TAG, "[onBackPressed] isImageCaptureIntent");
            mIModuleCtrl.setResultAndFinish(Activity.RESULT_CANCELED, new Intent());
            return false;
        }
        if (ModeState.STATE_IDLE != getModeState()) {
            return true;
        }
        return false;
    }

    private void onCameraParameterReady(boolean startPreview) {
        Log.d(TAG, "[onCameraParameterReady] startPreview:" + startPreview + "modeState:"
                + getModeState());
        if (getModeState() == ModeState.STATE_UNKNOWN) {
            setModeState(ModeState.STATE_IDLE);
        }
        updateParameters();
        if (mICameraDevice.getParameters().getMaxNumDetectedObjects() == 2) {
            mAdditionManager.removeVideoOt();
        }
        mICameraDevice.setAutoFocusMoveCallback(mAutoFocusMoveCallback);
        mAdditionManager.onCameraParameterReady(true);
    }

    private void onSingleTapUp(View view, int x, int y) {
        Log.i(TAG, "[onSingleTapUp]mCameraClosed:" + mCameraClosed + ",mCurrentState = "
                + getModeState() + ",mIFocusManager = " + mIFocusManager);
        if (mIFocusManager == null || mCameraClosed || ModeState.STATE_IDLE != getModeState()) {
            return;
        }
        String focusMode = mIFocusManager.getFocusMode();
        // when devices support both continuous and infinity focus mode.
        if (focusMode == null || Parameters.FOCUS_MODE_INFINITY.equals(focusMode)) {
            Log.w(TAG, "[onSingleTapUp]focusMode:" + focusMode);
            return;
        }

        // Check if metering area or focus area is supported.
        if (!mIFocusManager.getFocusAreaSupported()) {
            Log.i(TAG, "[onSingleTapUp] getFocusAreaSupported is false");
            return;
        }
        mIFocusManager.onSingleTapUp(x, y);
    }

    private void onShutterButtonFocus(boolean pressed) {
        Log.d(TAG, "[onShutterButtonFocus]pressed:" + pressed);
        mICameraAppUi.collapseViewManager(true);
    }

    private void onShutterButtonClick() {
        boolean isEnoughSpace = mIFileSaver.isEnoughSpace();
        Log.i(TAG, "[onShutterButtonClick]isEnoughSpace = " + isEnoughSpace + ",mCameraClosed = "
                + mCameraClosed + ",mCurrentState = " + getModeState());
        // Do not take the picture if there is not enough storage or camera is not available.
        if (!isEnoughSpace || isCameraNotAvailable()) {
            Log.w(TAG, "[onShutterButtonClick]return.");
            return;
        }
        Log.i(TAG,
                "[CMCC Performance test][Camera][Camera] camera capture start ["
                        + System.currentTimeMillis() + "]");
        if (mIFocusManager != null) {
            mIFocusManager.focusAndCapture();
        }
    }

    private void onCameraClose() {
        Log.d(TAG, "[onCameraClose]");
        mAdditionManager.close(true);
        mCameraClosed = true;
        // Reset the capture image count when camera closed
        // due to the jpeg callback maybe lost in this case.
        mCapturedImageCount = 0;
        mHandler.removeMessages(MSG_RESTART_PREVIEW);
        // force hide the FD entry icon,case the onFaceDetection not will
        // callback when have leave this mode,so the UI will always have the VFB
        // entry icon, the 0 means current FD number is zero
        onFaceDetected(0);
        if (mIModuleCtrl.isImageCaptureIntent()) {
            mICameraAppUi.hideReview();
        } else {
            // device had closed, the the callback may be lost,
            // so we need to restore device related view state.
            ModeState state = getModeState();
            Log.d(TAG, "[onCameraClose]ModeState=" + state);
            if (ModeState.STATE_FOCUSING == state) {
                mICameraAppUi.restoreViewState();
            } else if (ModeState.STATE_CAPTURING == state) {
                mICameraAppUi.restoreViewState();
                mICameraAppUi.setSwipeEnabled(true);
            }
        }
        setModeState(ModeState.STATE_CLOSED);
    }

    private final AutoFocusCallback mAutoFocusCallback = new AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if (isCameraNotAvailable()) {
                Log.w(TAG, "[mAutoFocusCallback] camera is busy or closed, return");
                return;
            }
            if (!mISelfTimeManager.isSelfTimerCounting()
                    && ModeState.STATE_FOCUSING == getModeState()) {
                mICameraAppUi.restoreViewState();
            }
            // Must set mode state to ModeState.STATE_IDLE before update focus status.
            // The reason is that user may do capture at focusing state,capture will be operated
            // untill focus completed.The mode state will be set to ModeState.CAPTURING when take
            // picture.set mode state to ModeState.STATE_IDLE after take capture,preview will not
            // restarted because the mode state is wrong.
            setModeState(ModeState.STATE_IDLE);
            mIFocusManager.onAutoFocus(success);
            mIsAutoFocusCallback = true;
        }
    };

    private final ShutterCallback mShutterCallback = new ShutterCallback() {
        @Override
        public void onShutter() {
            mShutterCallbackTime = System.currentTimeMillis();
            long shutterLag = mShutterCallbackTime - mCaptureStartTime;
            Log.d(TAG, "[mShutterCallback] mShutterLag = " + shutterLag + "ms");
        }
    };

    private final PictureCallback mRawPictureCallback = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            mRawPictureCallbackTime = System.currentTimeMillis();
            Log.d(TAG, "mShutterToRawCallbackTime = "
                    + (mRawPictureCallbackTime - mShutterCallbackTime) + "ms");
        }
    };
    private final PictureCallback mUncompressedImageCallback = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.i(TAG, "[UncompressedImageCallback]onCanCapture");
            ++mCapturedImageCount;
            restartPreview(false);
        }
    };

    private final PictureCallback mJpegPictureCallback = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] jpegData, Camera camera) {
            Log.i(TAG, "[mJpegPictureCallback]onPictureTaken");
            if (jpegData == null) {
                Log.w(TAG, "[mJpegPictureCallback] jpegData is null");
                mICameraAppUi.setSwipeEnabled(true);
                mICameraAppUi.restoreViewState();
                restartPreview(false);
                return;
            }
            if (mCameraClosed) {
                Log.i(TAG, "[onPictureTaken] mCameraClosed:" + mCameraClosed);
                return;
            }

            mJpegPictureCallbackTime = System.currentTimeMillis();
            // If postview callback has arrived, the captured image is displayed
            // in postview callback. If not, the captured image is displayed in
            // raw picture callback.
            if (mPostViewPictureCallbackTime != 0) {
                mPictureDisplayedToJpegCallbackTime = mJpegPictureCallbackTime
                        - mPostViewPictureCallbackTime;
            } else {
                mPictureDisplayedToJpegCallbackTime = mJpegPictureCallbackTime
                        - mRawPictureCallbackTime;
            }
            Log.d(TAG, "[onPictureTaken]mPictureDisplayedToJpegCallbackTime = "
                    + mPictureDisplayedToJpegCallbackTime + "ms");
            mIFocusManager.updateFocusUI(); // Ensure focus indicator

            /* For intent image capture, need stop preview as the quick view.*/
            boolean isNeedStartPreview = !mIModuleCtrl.isImageCaptureIntent()
            /* If mode state is not CAPUTRING, means that application had been notified
             * through onCanCapture, add change the mode state.*/
                    && (ModeState.STATE_CAPTURING == getModeState()
            /* mCapturedImageCount > 0,means that onCanCapture had sent to application
             * mCapturedImageCount times, application don't need to restartPreview.*/
                    && (mCapturedImageCount == 0));
            if (isNeedStartPreview) {
                long delay = ExtensionHelper.getCameraFeatureExtension(null)
                        .getQuickViewDisplayDuration() - mPictureDisplayedToJpegCallbackTime;
                if (delay <= 0) {
                    restartPreview(true);
                } else {
                    Message msg = mHandler.obtainMessage(MSG_RESTART_PREVIEW);
                    mHandler.sendMessageDelayed(msg, delay);
                }
            }
            // add this check due to the onCanCapture don't send to application in some condition.
            if (mCapturedImageCount > 0) {
                --mCapturedImageCount;
            }
            // Calculate the width and the height of the jpeg.
            if (!mIModuleCtrl.isImageCaptureIntent()) {
                mIFileSaver.savePhotoFile(jpegData, null, mCaptureStartTime,
                        mIModuleCtrl.getLocation(), 0, null);
            } else {
                mJpegImageData = jpegData;
                if (!mIModuleCtrl.isQuickCapture()) {
                    mICameraAppUi.showReview(null, null);
                    mICameraAppUi.switchShutterType(ShutterButtonType.SHUTTER_TYPE_OK_CANCEL);
                } else {
                    doAttach();
                }
            }

            long now = System.currentTimeMillis();
            long jpegCallbackFinishTime = now - mJpegPictureCallbackTime;
            Log.d(TAG, "[mJpegPictureCallback] mJpegCallbackFinishTime = " + jpegCallbackFinishTime
                    + "ms");
            mJpegPictureCallbackTime = 0;
        }
    };

    @Override
    public boolean restartPreview(boolean needStop) {
        Log.i(TAG, "[restartPreview]needStop:" + needStop);
        mIsAutoFocusCallback = false;
        startPreview(needStop);
        mICameraAppUi.restoreViewState();
        mICameraAppUi.setSwipeEnabled(true);
        startFaceDetection();
        return true;
    }

    @Override
    public void onFileSaveing() {
        setModeState(ModeState.STATE_SAVING);
    }

    protected void startPreview(boolean needStop) {
        if (!mIModuleCtrl.isFirstStartUp()) {
            mActivity.runOnUiThread(new Runnable() {
                public void run() {
                    if (mIFocusManager != null) {
                        mIFocusManager.resetTouchFocus();
                    }
                }
            });
            boolean isZsdCapture = "on".equals(mISettingCtrl
                    .getSettingValue(SettingConstants.KEY_CAMERA_ZSD))
                    && ModeState.STATE_CAPTURING == getModeState();
            Log.i(TAG, "[startPreview] needStop:" + needStop + ",isZsdCapture = " + isZsdCapture);
            if (needStop && !isZsdCapture) {
                stopPreview();
            }

            mIFocusManager.setAeLock(false); // Unlock AE and AWB.
            mIFocusManager.setAwbLock(false);
            mIModuleCtrl.applyFocusParameters(false);
        }
        mICameraDevice.startPreview();
        mAdditionManager.execute(AdditionActionType.ACTION_ON_START_PREVIEW);
        mIFocusManager.onPreviewStarted();
        setModeState(ModeState.STATE_IDLE);
        mICameraDevice.setAutoFocusMoveCallback(mAutoFocusMoveCallback);
        mICameraDevice.setUncompressedImageCallback(getUncompressedImageCallback());
    }

    protected void stopPreview() {
        Log.i(TAG, "[stopPreview]mCurrentState = " + getModeState());
        if (ModeState.STATE_CLOSED == getModeState()) {
            Log.i(TAG, "[stopPreview]Preview is stopped.");
            return;
        }
        // stop preview may not be called in main thread,we need to stop face
        // detection firstly
        // Exception Case: touch focus between onCamearOpenDone and startPreview
        // Done,press home key to exit camera and then enter immediately
        if (mICameraDevice == null) {
            updateDevice();
        }
        if (mICameraDevice.getFaceDetectionStatus()) {
            stopFaceDetection();
        }
        mAdditionManager.execute(AdditionActionType.ACTION_ON_STOP_PREVIEW);
        mICameraDevice.cancelAutoFocus(); // Reset the focus.
        mICameraDevice.setAutoFocusMoveCallback(null);
        mICameraDevice.stopPreview();


        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                if (mIFocusManager != null) {
                    mIFocusManager.onPreviewStopped();
                }
            }
        });
    }
    protected PictureCallback getUncompressedImageCallback() {
        return mUncompressedImageCallback;
    }

    private void doAttach() {
        Log.d(TAG, "[doAttach] mCameraClosed:" + mCameraClosed);
        if (mCameraClosed) {
            return;
        }
        byte[] data = mJpegImageData;
        // M: insert record into Media DB.
        mIFileSaver.savePhotoFile(mJpegImageData, null, mCaptureStartTime,
                mIModuleCtrl.getLocation(), 0, null);
        Uri saveUri = mIModuleCtrl.getSaveUri();
        String cropValue = mIModuleCtrl.getCropValue();
        if (cropValue == null) {
            // First handle the no crop case -- just return the value. If the
            // caller specifies a "save uri" then write the data to its
            // stream. Otherwise, pass back a scaled down version of the bitmap
            // directly in the extras.
            if (saveUri != null) {
                OutputStream outputStream = null;
                try {
                    outputStream = mActivity.getContentResolver().openOutputStream(saveUri);
                    if (outputStream != null) {
                        outputStream.write(data);
                        outputStream.close();
                    }

                    mIModuleCtrl.setResultAndFinish(Activity.RESULT_OK);
                } catch (IOException ex) {
                    Log.w(TAG, "IOException, when doAttach");
                    // ignore exception
                } finally {
                    Util.closeSilently(outputStream);
                }
            } else {
                int orientation = Exif.getOrientation(data);
                Bitmap bitmap = Util.makeBitmap(data, 50 * 1024);
                bitmap = Util.rotate(bitmap, orientation);
                mIModuleCtrl.setResultAndFinish(Activity.RESULT_OK,
                        new Intent("inline-data").putExtra("data", bitmap));
            }
        } else {
            // Save the image to a temp file and invoke the cropper
            Uri tempUri = null;
            FileOutputStream tempStream = null;
            try {
                File path = mActivity.getFileStreamPath(TEMP_CROP_FILE_NAME);
                path.delete();
                tempStream = mActivity.openFileOutput(TEMP_CROP_FILE_NAME, 0);
                tempStream.write(data);
                tempStream.close();
                tempUri = Uri.fromFile(path);
            } catch (FileNotFoundException ex) {
                mIModuleCtrl.setResultAndFinish(Activity.RESULT_CANCELED);
                return;
            } catch (IOException ex) {
                mIModuleCtrl.setResultAndFinish(Activity.RESULT_CANCELED);
                return;
            } finally {
                Util.closeSilently(tempStream);
            }

            Bundle newExtras = new Bundle();
            if (cropValue.equals("circle")) {
                newExtras.putString("circleCrop", "true");
            }
            if (saveUri != null) {
                newExtras.putParcelable(MediaStore.EXTRA_OUTPUT, saveUri);
            } else {
                newExtras.putBoolean("return-data", true);
            }

            if (mIModuleCtrl.isSecureCamera()) {
                // TODO can use other way?
                // newExtras.putBoolean(CropExtras.KEY_SHOW_WHEN_LOCKED, true);
            }

            Intent cropIntent = new Intent("com.android.camera.action.CROP");

            cropIntent.setData(tempUri);
            cropIntent.putExtras(newExtras);

            mActivity.startActivityForResult(cropIntent, REQUEST_CROP);
        }
    }

    private void updateParameters() {
        super.updateDevice();
        super.updateFocusManager();
        if (mIFocusManager != null) {
            mIFocusManager.setListener(this);
        }
    }

    private final class MainHandler extends Handler {

        public MainHandler(Looper mainLooper) {
            super(mainLooper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "[handleMessage]msg id=" + msg.what);
            switch (msg.what) {
            case MSG_RESTART_PREVIEW:
                if (!mCameraClosed) {
                    restartPreview(true);
                }
                break;

            default:
                break;
            }
        }
    }

    protected class CameraCategory {

        public void takePicture() {
            if (!mAdditionManager.execute(AdditionActionType.ACTION_TAKEN_PICTURE)) {
                mICameraDevice.takePicture(mShutterCallback, mRawPictureCallback,
                        null, mJpegPictureCallback);
                mICameraAppUi.setViewState(ViewState.VIEW_STATE_CAPTURE);
            }
        }
    }

    private void onFaceDetected(int faceLength) {
        boolean isSmileShotOn = "on".equals(mISettingCtrl
                .getSettingValue(SettingConstants.KEY_SMILE_SHOT));
        Log.d(TAG, "[onFaceDetected] isSmaileShotOn = " + isSmileShotOn);
        if (isSmileShotOn) {
            // launch camera,preview with face,and then turn on VFB; switch to front camera->switch
            // to back  camera,now not preview face,let the mode auto back to normal mode;
            // go to settings turn on to  smile shot,now preview with face,
            // will found auto back to VFB mode this time just show FB entry icon
            mICameraAppUi.changeBackToVFBModeStatues(false);
        }

        // the entry view may be null when not supported in VFB project
        mICameraAppUi.updateFaceBeatuyEntryViewVisible(isNeedShowFBEntry(faceLength));
    }

    // Need Check follow condition add other case
    private boolean isNeedShowFBEntry(int length) {
        boolean isNeedShow = mIModuleCtrl.isNonePickIntent()
                && mICameraAppUi.getViewState() == ViewState.VIEW_STATE_NORMAL
                && mICameraContext.getFeatureConfig().isVfbEnable()
                && mISettingCtrl.getSettingValue(SettingConstants.KEY_MULTI_FACE_BEAUTY) != null
                && !mActivity
                        .getResources()
                        .getString(R.string.pref_face_beauty_mode_off)
                        .equals(mISettingCtrl
                                .getSettingValue(SettingConstants.KEY_MULTI_FACE_BEAUTY))
                && !"on".equals(mISettingCtrl.getSettingValue(SettingConstants.KEY_SLOW_MOTION))
                && !"on".equals(mISettingCtrl.getSettingValue(SettingConstants.KEY_HDR))
                && length > 0
                && "on".equals(mISettingCtrl.getSettingValue(
                  SettingConstants.KEY_CAMERA_FACE_DETECT));

        Log.d(TAG,
                "[isNeedShowFBEntry] faceLength = " + length + ",ViewState = "
                        + mICameraAppUi.getViewState() + ",SlowMotion : "
                        + mISettingCtrl.getSettingValue(SettingConstants.KEY_SLOW_MOTION)
                        + ",hdr = " + mISettingCtrl.getSettingValue(SettingConstants.KEY_HDR)
                        + ",FaceBeauty Setting: "
                        + mISettingCtrl.getSettingValue(SettingConstants.KEY_MULTI_FACE_BEAUTY)
                        + ",FD value = " +
                          mISettingCtrl.getSettingValue(SettingConstants.KEY_CAMERA_FACE_DETECT)
                        + ",isNeedShow = " + isNeedShow);

        return isNeedShow;
    }

    /**
     * Whether the camera not available to use or not.No need to update auto focus state and can not
     * do capture when camera is not available.
     *
     * @return True if the camera mode state is not available(busy in capturing or saving state or
     *         closed),false if not.
     */
    private boolean isCameraNotAvailable() {
        ModeState modeState = getModeState();
        Log.d(TAG, "isCameraNotAvailable modeState " + modeState);
        return (ModeState.STATE_CAPTURING == modeState || ModeState.STATE_SAVING == modeState ||
                ModeState.STATE_CLOSED == modeState) ? true : false;
    }
}
