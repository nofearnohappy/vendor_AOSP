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

package com.mediatek.camera.v2.mode.facebeauty;

import android.content.ContentValues;
import android.filterfw.geometry.Point;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.Log;
import android.view.ViewGroup;
import com.android.camera.R;
import com.android.camera.v2.util.Storage;

import com.mediatek.camera.v2.mode.normal.CaptureMode;
import com.mediatek.camera.v2.module.ModuleListener;
import com.mediatek.camera.v2.platform.ModeChangeListener;
import com.mediatek.camera.v2.platform.app.AppController;
import com.mediatek.camera.v2.services.ISoundPlayback;
import com.mediatek.camera.v2.stream.ICaptureStream.CaptureStreamCallback;
import com.mediatek.camera.v2.stream.StreamManager;
import com.mediatek.camera.v2.stream.facebeauty.ICfbStreamController;
import com.mediatek.camera.v2.stream.facebeauty.ICfbStreamController.CaptureStatusCallback;
import com.mediatek.camera.v2.stream.facebeauty.ICfbStreamController.StreamStatusCallback;
import com.mediatek.camera.v2.util.SettingKeys;
import com.mediatek.camera.v2.util.Utils;
import com.mediatek.mmsdk.BaseParameters;

import junit.framework.Assert;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CFB:Capture Face Beauty.
 */
public class CfbCaptureMode extends CaptureMode {
    private static final String TAG = "CfbCaptureMode";

    // this String must be same as native,will move to a static file for this,
    // so the base parameters may not need the PUBLIC STATIC FINAL String xxxxxx
    private static final String KEY_JPEG_QUALITY = "jpeg_quality";
    private static final String KEY_FACE_POSITION = "fb-face-position";
    private static final boolean DEBUG = true;

    private static final int FACE_DETECTION_NATIVE_RANGE = 2000;

    private static final int UNKNOW_INDEX = -1;

    // when session is opened,now can set the parameters and other action
    private boolean mIsReadyForCapture = false;

    private boolean mIsInCapturing = false;

    // how to get the default value TODO
    private String mShapeValue;
    // private String mShapeSupportedMax;
    // private String mShapeSupportedMin;

    private String mSkinColorValue;
    // private String mSkinColorSupportedMax;
    // private String mSkinColorSupportedMin;

    private String mSmoothValue;
    // private String mSmoothSupportedMax;
    // private String mSmoothSupportedMin;
    private String mCurrentCameraId;

    /**
     * Current the parameters will have follow Keys: KEY_FACE_BEAUTY_SHARP
     * KEY_FACE_BEAUTY_SKIN_COLOR KEY_FACE_BEAUTY_SMOOTH KEY_PICTURE_ROTATION
     * KEY_JPEG_QUALITY KEY_FACE_POSITION
     */
    private static final String[] PARAMETERS_INDEX = new String[] {
            SettingKeys.KEY_FACE_BEAUTY_SHARP,
            SettingKeys.KEY_FACE_BEAUTY_SKIN_COLOR,
            SettingKeys.KEY_FACE_BEAUTY_SMOOTH,
            BaseParameters.KEY_PICTURE_ROTATION, KEY_JPEG_QUALITY,
            KEY_FACE_POSITION };

    private List<String> mKeyParameters = new ArrayList<String>(
            PARAMETERS_INDEX.length);
    private List<String> mValuePrameters = new ArrayList<String>(
            PARAMETERS_INDEX.length);

    private int mJpegExifOrientation = 0;
    private int mSensorOrientation = 0;
    private int mOrientation = 0;

    private CameraCharacteristics mCameraCharacteristics;
    private CaptureStatusCallbackImpl mCaptureStatusCallbackImpl = new CaptureStatusCallbackImpl();
    private StreamStatusCallbackImpl mStreamStatusCallbackImpl = new StreamStatusCallbackImpl();
    private StreamManager mStreamManager;
    private CaptureStreamCallback mCaptureStreamCallback;
    private ContentValues mCapContentValues;
    private ICfbStreamController mICfbStreamController;

    private boolean mIsInVideoMode = false;
    private TotalCaptureResult mTotalCaptureResult;
    private boolean mPaused = false;

    public CfbCaptureMode(
            AppController app, ModuleListener moduleListener) {
        super(app, moduleListener);
        mAppUi.switchShutterButtonLayout(R.layout.camera_shutter_photo_video_v2);
        // TAG = CfbCaptureMode.class.getSimpleName() + "(" + FEATURE_TAG + ")";
        mCurrentCameraId = mSettingServant.getCameraId();
        mCameraCharacteristics = Utils.getCameraCharacteristics(
                app.getActivity(), mCurrentCameraId);
        mSensorOrientation = mCameraCharacteristics
                .get(CameraCharacteristics.SENSOR_ORIENTATION);

        // initialize the list of key parameters
        for (int i = 0; i < PARAMETERS_INDEX.length; i++) {
            mKeyParameters.add(PARAMETERS_INDEX[i]);
            mValuePrameters.add(i, null);
        }

        Log.i(TAG, "[CfbCaptureMode] , mSensorOrientation = "
                + mSensorOrientation);
    }

    @Override
    protected void updateCaredSettingChangedKeys() {
        Log.i(TAG, "[updateCaredSettingChangedKeys] ++++");
        super.updateCaredSettingChangedKeys();
        addCaredSettingChangedKeys(SettingKeys.KEY_FACE_BEAUTY_SHARP);
        addCaredSettingChangedKeys(SettingKeys.KEY_FACE_BEAUTY_SKIN_COLOR);
        addCaredSettingChangedKeys(SettingKeys.KEY_FACE_BEAUTY_SMOOTH);
        Log.i(TAG, "[updateCaredSettingChangedKeys] ----");
    }

    @Override
    protected CaptureStreamCallback getCaptureStreamCallback() {
        if (mCaptureStreamCallback == null) {
            mCaptureStreamCallback = new CaptureStreamCallback() {

                @Override
                public void onCaptureCompleted(Image image) {
                    Log.i(TAG, "[onCaptureCompleted] +++");
                    if (image.getFormat() == ImageFormat.JPEG) {
                        updateCaptureContentValues(image);
                        byte[] data = Utils.acquireJpegBytesAndClose(image);
                        if (mTotalCaptureResult != null) {
                            CfbCaptureHelper.saveJpegExifInfo(data,
                                    mTotalCaptureResult, mJpegExifOrientation);
                            // when use the result,need initialize again
                        }
                        mCameraServices.getMediaSaver().addImage(
                                data,
                                mCapContentValues,
                                mMediaSavedListener,
                                mAppController.getActivity()
                                        .getContentResolver());
                        mAppController.getActivity().runOnUiThread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                // because CFB capture use the HWJpeg encoder
                                // for compress jpeg;but snap shot not use this method, so need
                                // reset the shutter button state when take picture down.
                                if (mIsInVideoMode) {
                                    mAppUi.setShutterButtonEnabled(true, false);
                                }
                                mTotalCaptureResult = null;
                            }
                                });
                    }

                    Log.i(TAG, "[onCaptureCompleted] ---");
                }
            };
        }
        return mCaptureStreamCallback;
    }

    @Override
    public void open(StreamManager streamManager, ViewGroup parentView,
            boolean isCaptureIntent) {
        Log.i(TAG, "[open]++++++++");
        mStreamManager = streamManager;
        super.open(streamManager, parentView, isCaptureIntent);
        Log.i(TAG, "[open]--------");
    }

    @Override
    public void resume() {
        Log.i(TAG, "[resume]");
        mPaused = false;
        openCfbEffectHal();
        super.resume();
    }

    @Override
    public void pause() {
        Log.i(TAG, "[pause]");
        mPaused = true;
        closeCfbEffectHal();
        super.pause();
    }

    @Override
    protected int getModeId() {
        return ModeChangeListener.MODE_FACE_BEAUTY;
    }

    @Override
    public void onSettingChanged(Map<String, String> result) {
        Log.i(TAG, "[onSettingChanged],mPaused = " + mPaused);
        if (mPaused) {
            Log.i(TAG, "[onSettingChanged] because this mode is paused,will return");
            return;
        }
        updateFaceBeautyPrameters(result);
        super.onSettingChanged(result);
    }

    @Override
    public void close() {
        Log.i(TAG, "[close]");
        closeCfbEffectHal();
        super.close();
    }

    @Override
    public void onShutterClicked(boolean isVideo) {
        Log.i(TAG, "[onShutterClicked] isVideo = " + isVideo
                + ",mIsInVideoMode = " + mIsInVideoMode + ",mPaused = " + mPaused);
        if (isVideo) {
            mIsInVideoMode = mIsInVideoMode == true ? false : true;
            mICfbStreamController.setCurrentVideoTag(mIsInVideoMode);

            // if current is in Video Recording,need close the Effect HAL;when
            // stop VR,will reopen the Effect HAL
            // CR:ALPS02020315
            if (mIsInVideoMode) {
                mICfbStreamController.closeStream();
            } else if (!mPaused) {
                mICfbStreamController.openStream(mStreamStatusCallbackImpl);
            }
        }

        // when current is in VideoRecording, the snap shot not supported in
        // CFB, so will run normal capture
        if (!mIsInVideoMode && !isVideo && mICfbStreamController != null
                && mValuePrameters != null && !mValuePrameters.isEmpty()) {
            // add the picture rotation to the parameters
            int rotationIndex = mKeyParameters
                    .indexOf(BaseParameters.KEY_PICTURE_ROTATION);
            if (UNKNOW_INDEX != rotationIndex) {
                updateKeyValue(rotationIndex,
                        BaseParameters.KEY_PICTURE_ROTATION,
                        Integer.toString(mJpegExifOrientation));
            }

            // add the jpeg quality key and value into the Arraylist
            int jpegQalityIndex = mKeyParameters.indexOf(KEY_JPEG_QUALITY);
            if (UNKNOW_INDEX != jpegQalityIndex) {
                updateKeyValue(jpegQalityIndex, KEY_JPEG_QUALITY,
                        Byte.toString(JPEG_QUALITY));
            }

            int facePotstionIndex = mKeyParameters.indexOf(KEY_FACE_POSITION);
            if (UNKNOW_INDEX != facePotstionIndex) {
                setFacePosition(facePotstionIndex);
            }

            // set the parameters for EffectHalClient
            mICfbStreamController
                    .setParameters(mKeyParameters, mValuePrameters);

            if (DEBUG) {
                Log.i(TAG, "[onShutterClicked] end ,mValuePrameters = "
                        + mValuePrameters + ",mKeyParameters = "
                        + mKeyParameters);
            }
        }

        mJpegExifOrientation = Utils.getJpegRotation(mOrientation,
                mCameraCharacteristics);

        if (!mIsInVideoMode && !isVideo && mICfbStreamController != null) {
            mICfbStreamController.startCapture(mCaptureStatusCallbackImpl);
            mIsInCapturing = true;
        }
        super.onShutterClicked(isVideo);
    }

    @Override
    public boolean switchCamera(String cameraId) {
        mCurrentCameraId = cameraId;
        mCameraCharacteristics = Utils.getCameraCharacteristics(
                mAppController.getActivity(), mCurrentCameraId);
        mSensorOrientation = mCameraCharacteristics
                .get(CameraCharacteristics.SENSOR_ORIENTATION);
        Log.i(TAG, "[switchCamera] , mSensorOrientation = "
                + mSensorOrientation + ",cameraId = " + cameraId);
        return super.switchCamera(cameraId);
    }

    @Override
    public void onOrientationChanged(int orientation) {
        super.onOrientationChanged(orientation);
        mOrientation = orientation;
    }

    @Override
    public void onPreviewCaptureCompleted(CaptureRequest request,
            TotalCaptureResult result) {
        super.onPreviewCaptureCompleted(request, result);
        Assert.assertNotNull(result);
        Rect[] rectangles = result
                .get(CaptureResult.STATISTICS_FACE_RECTANGLES);
        Rect cropRegion = result.get(CaptureResult.SCALER_CROP_REGION);
        storeFaceBeautyLocation(rectangles, cropRegion);
    }

    @Override
    public CaptureCallback getCaptureCallback() {
        return new CaptureCallback() {

            @Override
            public void onCaptureStarted(CameraCaptureSession session,
                    CaptureRequest request, long timestamp) {
                mCameraServices.getSoundPlayback().play(ISoundPlayback.SHUTTER_CLICK);
                Log.i(TAG, "[onCaptureStarted],session = " + session
                        + ",timestamp = " + timestamp);
            }

            @Override
            public void onCaptureProgressed(CameraCaptureSession session,
                    CaptureRequest request, CaptureResult partialResult) {
                Log.i(TAG, "[onCaptureProgressed]");
            }

            @Override
            public void onCaptureCompleted(CameraCaptureSession session,
                    CaptureRequest request, TotalCaptureResult result) {
                Log.i(TAG, "[onCaptureCompleted]");
                mTotalCaptureResult = result;
            }

        };
    }

    @Override
    public ModeGestureListener getModeGestureListener() {
        return mModeGestureListener;
    }

    private void openCfbEffectHal() {
        // at this time, Mode can get the CFB stream controller by StreamManager
        mICfbStreamController = mStreamManager.getFbStreamController();
        // open the FB effect HAL
        mICfbStreamController.openStream(mStreamStatusCallbackImpl);
        // get current setting values
        getSettingFaceBeautyValues();
    }

    private void closeCfbEffectHal() {
        Log.i(TAG, "[closeCfbEffectHal] mICfbStreamController = "
                + mICfbStreamController);
        if (mICfbStreamController != null) {
            mICfbStreamController.closeStream();
        }
    }

    private void updateFaceBeautyPrameters(Map<String, String> result) {
        Log.i(TAG, "[updateFaceBeautyPrameters]");

        mShapeValue = result.get(SettingKeys.KEY_FACE_BEAUTY_SHARP);
        if (mShapeValue != null) {
            // first clear the key and value
            int shapeIndex = mKeyParameters
                    .indexOf(SettingKeys.KEY_FACE_BEAUTY_SHARP);
            updateKeyValue(shapeIndex, SettingKeys.KEY_FACE_BEAUTY_SHARP,
                    CfbCaptureHelper.workAroundValue(mShapeValue));
        }

        mSkinColorValue = result.get(SettingKeys.KEY_FACE_BEAUTY_SKIN_COLOR);
        if (mSkinColorValue != null) {
            int shinColorIndex = mKeyParameters
                    .indexOf(SettingKeys.KEY_FACE_BEAUTY_SKIN_COLOR);
            updateKeyValue(shinColorIndex,
                    SettingKeys.KEY_FACE_BEAUTY_SKIN_COLOR,
                    CfbCaptureHelper.workAroundValue(mSkinColorValue));
        }

        mSmoothValue = result.get(SettingKeys.KEY_FACE_BEAUTY_SMOOTH);
        if (mSmoothValue != null) {
            int smoothIndex = mKeyParameters
                    .indexOf(SettingKeys.KEY_FACE_BEAUTY_SMOOTH);
            updateKeyValue(smoothIndex, SettingKeys.KEY_FACE_BEAUTY_SMOOTH,
                    CfbCaptureHelper.workAroundValue(mSmoothValue));
        }

        Log.i(TAG, "[updateFaceBeautyPrameters] ----,mShapeValue = "
                + mShapeValue + ",mSkinColorValue = " + mSkinColorValue
                + ",mSmoothValue = " + mSmoothValue + ",mValuePrameters = "
                + mValuePrameters);
    }

    private void updateKeyValue(int index, String key, String value) {
        Log.d(TAG, "[updateKeyValue] index = " + index + ",key = " + key
                + ",value = " + value);
        if (UNKNOW_INDEX != index) {
            mKeyParameters.remove(index);
            if (mValuePrameters != null && mValuePrameters.size() > index) {
                mValuePrameters.remove(index);
            }
            mKeyParameters.add(index, key);
            mValuePrameters.add(index, value);
        }
    }

    private void updateCaptureContentValues(Image image) {
        mCapContentValues = new ContentValues();
        long dateTaken = System.currentTimeMillis();
        String title = Utils.createJpegName(dateTaken);

        String filename = title + ".jpg";
        String mime = "image/jpeg";
        String path = Storage.DIRECTORY + '/' + filename;
        String tmpPath = path + ".tmp";

        mCapContentValues.put(ImageColumns.DATE_TAKEN, dateTaken);
        mCapContentValues.put(ImageColumns.TITLE, title);
        mCapContentValues.put(ImageColumns.DISPLAY_NAME, filename);
        mCapContentValues.put(ImageColumns.DATA, path);
        mCapContentValues.put(ImageColumns.MIME_TYPE, mime);

        mCapContentValues.put(ImageColumns.WIDTH, image.getWidth());
        mCapContentValues.put(ImageColumns.HEIGHT, image.getHeight());
        mCapContentValues.put(ImageColumns.ORIENTATION, mJpegExifOrientation);

        mLocation = mLocationManager.getCurrentLocation();
        if (mLocation != null) {
            mCapContentValues.put(ImageColumns.LATITUDE, mLocation.getLatitude());
            mCapContentValues.put(ImageColumns.LONGITUDE, mLocation.getLongitude());
        }
        Log.i(TAG, "[updateCaptureContentValues] width = " + image.getWidth()
                + ",height = " + image.getHeight() + ",mJpegExifOrientation = "
                + mJpegExifOrientation);

    }

    private void getSettingFaceBeautyValues() {

        String shape = mSettingServant
                .getSettingValue(SettingKeys.KEY_FACE_BEAUTY_SHARP);
        String skinValue = mSettingServant
                .getSettingValue(SettingKeys.KEY_FACE_BEAUTY_SKIN_COLOR);
        String smooth = mSettingServant
                .getSettingValue(SettingKeys.KEY_FACE_BEAUTY_SMOOTH);
        if (shape != null) {
            // first clear the key and value
            int shapeIndex = mKeyParameters
                    .indexOf(SettingKeys.KEY_FACE_BEAUTY_SHARP);
            updateKeyValue(shapeIndex, SettingKeys.KEY_FACE_BEAUTY_SHARP,
                    CfbCaptureHelper.workAroundValue(shape));
        }

        if (skinValue != null) {
            int shinColorIndex = mKeyParameters
                    .indexOf(SettingKeys.KEY_FACE_BEAUTY_SKIN_COLOR);
            updateKeyValue(shinColorIndex,
                    SettingKeys.KEY_FACE_BEAUTY_SKIN_COLOR,
                    CfbCaptureHelper.workAroundValue(skinValue));
        }

        if (smooth != null) {
            int smoothIndex = mKeyParameters
                    .indexOf(SettingKeys.KEY_FACE_BEAUTY_SMOOTH);
            updateKeyValue(smoothIndex, SettingKeys.KEY_FACE_BEAUTY_SMOOTH,
                    CfbCaptureHelper.workAroundValue(smooth));
        }

        Log.i(TAG, "[getSettingFaceBeautyValues] shape = " + shape
                + ",skinValue = " + skinValue + ",smooth = " + smooth);
    }

    /**
     * ******************************************************************
     * ***************follow is used for Store the Face Position ********
     * ******************************************************************
     */
    private ArrayList<Integer> mFacesPosition = new ArrayList<Integer>();

    private void storeFaceBeautyLocation(Rect[] faceRect, Rect cropRegion) {
        int index = 0;
        // First :clear last time stored values
        if (mFacesPosition != null && mFacesPosition.size() != 0) {
            mFacesPosition.clear();
        }
        // Seconded: store the new values to the list
        if (faceRect != null) {
            for (int i = 0; i < faceRect.length; i++) {
                Point origin = new Point(cropRegion.left, cropRegion.top);
                float width = cropRegion.width();
                float heigth = cropRegion.height();

                // because CFB need the face position which is same as FD client
                // detected location. add native find the positon is used this
                // method:
                // face_rect.p.x = ((mpDetectedFaces->faces[i].rect[0]+1000) *
                // cropRegion.s.w/2000) + cropRegion.p.x; //Left
                // face_rect.p.y = ((mpDetectedFaces->faces[i].rect[1]+1000) *
                // cropRegion.s.h/2000) + cropRegion.p.y; //Top
                // face_rect.s.w = ((mpDetectedFaces->faces[i].rect[2]+1000) *
                // cropRegion.s.w/2000) + cropRegion.p.x; //Right
                // face_rect.s.h = ((mpDetectedFaces->faces[i].rect[3]+1000) *
                // cropRegion.s.h/2000) + cropRegion.p.y; //Bottom
                float left = (float) (faceRect[i].left - origin.x)
                        * FACE_DETECTION_NATIVE_RANGE / width
                        - FACE_DETECTION_NATIVE_RANGE / 2;
                float top = (float) (faceRect[i].top - origin.y)
                        * FACE_DETECTION_NATIVE_RANGE / heigth
                        - FACE_DETECTION_NATIVE_RANGE / 2;
                float right = (float) (faceRect[i].right - origin.x)
                        * FACE_DETECTION_NATIVE_RANGE / width
                        - FACE_DETECTION_NATIVE_RANGE / 2;
                float bottom = (float) (faceRect[i].bottom - origin.y)
                        * FACE_DETECTION_NATIVE_RANGE / heigth
                        - FACE_DETECTION_NATIVE_RANGE / 2;

                mFacesPosition.add(index++, (int) left);
                mFacesPosition.add(index++, (int) top);
                mFacesPosition.add(index++, (int) right);
                mFacesPosition.add(index++, (int) bottom);
            }
            // Log.d(TAG, "[storeFaceBeautyLocation] mFacesPosition = " +
            // mFacesPosition.toString());
        }

    }

    private void setFacePosition(int index) {
        if (mFacesPosition == null) {
            Log.i(TAG, "[setFacePosition],current points is null,return");
            return;
        }

        String value = udpateFacePositionToString();
        updateKeyValue(index, KEY_FACE_POSITION, value);

        Log.d(TAG, "[setFacePosition] index = " + index + ",value = " + value);
    }

    private String udpateFacePositionToString() {
        StringBuilder value = new StringBuilder();
        int tempIndex = 0;
        for (int i = 0; i < mFacesPosition.size(); i++) {
            tempIndex = i + 1;
            value.append(mFacesPosition.get(i));
            // why need (i +1) != mvFBFacesPoint.size() ?
            // because at the end of value,not need any symbol
            // the value format is: xxx:yyy,x1:y1
            // why need the 4 ?
            // because the facesPostion have store four values: left
            // /top/right/button
            if (tempIndex != mFacesPosition.size()) {
                if (tempIndex % 4 != 0) {
                    value.append(",");
                } else {
                    value.append(":");
                }
            }
        }
        Log.i(TAG, "[udpateFacePositionToString],vaue = " + value);
        return String.valueOf(value);
    }

    // Current the callback maybe not use,but this part will be used for a
    // example for use Effect HAl
    private class StreamStatusCallbackImpl implements StreamStatusCallback {

        @Override
        public void onStreamClosed() {
        }

        @Override
        public void onStreamOpenFailed() {
            mIsReadyForCapture = false;
        }

        @Override
        public void onStreamError() {
            mIsReadyForCapture = false;

        }

        @Override
        public void onReadyForCapture() {
            mIsReadyForCapture = true;
        }

        @Override
        public void onSetupFailed() {
            mIsReadyForCapture = false;

        }

    }

    // Current the callback maybe not use,but this part will be used for a
    // example for use Effect HAl
    private class CaptureStatusCallbackImpl implements CaptureStatusCallback {

        @Override
        public void onInputFrameProcessed(BaseParameters parameter,
                BaseParameters partialResult) {
            if (DEBUG) {
                Log.i(TAG,
                        "["
                                + Thread.currentThread().getStackTrace()[2]
                                        .getMethodName() + "]");
            }

        }

        @Override
        public void onOutputFrameProcessed(BaseParameters parameter,
                BaseParameters partialResult) {
            if (DEBUG) {
                Log.i(TAG,
                        "["
                                + Thread.currentThread().getStackTrace()[2]
                                        .getMethodName() + "]");
            }

        }

        // can notify update current view state and others TODO
        @Override
        public void onCaptureCompleted(BaseParameters result, long uid) {
            if (DEBUG) {
                Log.i(TAG,
                        "["
                                + Thread.currentThread().getStackTrace()[2]
                                        .getMethodName() + "],uid = " + uid);
            }
            mIsInCapturing = false;

            mAppController.getActivity().runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            Log.i(TAG, "[onCaptureCompleted] will update all the views");
                            mAppUi.setShutterButtonEnabled(true,
                                    false /* video shutter */);
                            mAppUi.setAllCommonViewEnable(true);
                            mAppUi.setSwipeEnabled(true);
                        }
                    });
        }

        @Override
        public void onCaptureAborted(BaseParameters result) {
            if (DEBUG) {
                Log.i(TAG,
                        "["
                                + Thread.currentThread().getStackTrace()[2]
                                        .getMethodName() + "]");
            }

        }

        @Override
        public void onCaptureFailed(BaseParameters result) {
            if (DEBUG) {
                Log.i(TAG,
                        "["
                                + Thread.currentThread().getStackTrace()[2]
                                        .getMethodName() + "]");
            }

            mIsInCapturing = false;
        }
    }

    private ModeGestureListener mModeGestureListener = new ModeGestureListener() {

        @Override
        public boolean onUp() {
            return false;
        }

        @Override
        public boolean onSingleTapUp(float x, float y) {
            Log.i(TAG, "[onSingleTapUp] mIsInCapturing = " + mIsInCapturing);
            return mIsInCapturing;
        }

        @Override
        public boolean onScroll(float dx, float dy, float totalX, float totalY) {
            return false;
        }

        @Override
        public boolean onLongPress(float x, float y) {
            return false;
        }

        @Override
        public boolean onDown(float x, float y) {
            return false;
        }
    };

}