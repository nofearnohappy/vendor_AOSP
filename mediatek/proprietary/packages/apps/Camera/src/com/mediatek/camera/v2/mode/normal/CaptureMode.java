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

package com.mediatek.camera.v2.mode.normal;

import com.android.camera.FeatureSwitcher;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import com.android.camera.R;
import com.android.camera.v2.util.Storage;
import com.mediatek.camera.v2.control.IControl.IAaaController;
import com.mediatek.camera.v2.exif.Exif;
import com.mediatek.camera.v2.mode.AbstractCameraMode;
import com.mediatek.camera.v2.services.FileSaver.OnFileSavedListener;
import com.mediatek.camera.v2.stream.ICaptureStream;
import com.mediatek.camera.v2.stream.dng.IDngStream;
import com.mediatek.camera.v2.stream.IPreviewStream.PreviewSurfaceCallback;
import com.mediatek.camera.v2.stream.IRecordStream;
import com.mediatek.camera.v2.stream.ICaptureStream.CaptureStreamCallback;
import com.mediatek.camera.v2.stream.IRecordStream.RecordStreamStatus;
import com.mediatek.camera.v2.stream.IPreviewStream;
import com.mediatek.camera.v2.stream.StreamManager;
import com.mediatek.camera.v2.module.ModuleListener;
import com.mediatek.camera.v2.module.ModuleListener.CaptureType;
import com.mediatek.camera.v2.module.ModuleListener.RequestType;
import com.mediatek.camera.v2.platform.ModeChangeListener;
import com.mediatek.camera.v2.platform.app.AppController;
import com.mediatek.camera.v2.util.SettingKeys;
import com.mediatek.camera.v2.util.Utils;
import com.mediatek.camera.v2.vendortag.TagMetadata;
import com.mediatek.camera.v2.vendortag.TagRequest;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CaptureRequest.Builder;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.media.CamcorderProfile;
import android.media.Image;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Video;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.ViewGroup;

import com.mediatek.camera.v2.services.ISoundPlayback;

/**
 * Normal capture mode that is made to support preview, photo and video capture on top of
 * {@link PreviewStream}, {@link ImageReaderStream} and {@link RecordingStream}.
 * <p>
 * All sub-class of CaptureMode can decide the following component:
 * <p>
 * 1.Surfaces
 *       <li>preview:{@link #getPreviewSurface()}</li>
 *       <li>capture:{@link #getCaptureSurface()}</li>
 *       <li>record:{@link #getRecordSurface()}</li>
 * <p>
 * 2.Stream call backs
 *
 */
public class CaptureMode extends AbstractCameraMode {
    private final String TAG;
    private final String DNG_ON = "on";
    private final String DNG_OFF = "off";
    private final ConditionVariable mRawCallbackSync = new ConditionVariable();
    private boolean mPaused;
    protected Surface mPreviewSurface;
    protected Surface mCaptureSurface;
    private StreamManager mStreamManager;
    private IDngStream mDngStream;
    protected Surface mRawCaptureSurface;
    protected Surface mRecordSurface;
    private boolean mRecording = false;
    private static final long SHUTTER_BUTTON_TIMEOUT = 500L; // 500ms
    private Handler mHandler = new Handler();
    private ConditionVariable mStopRecordingSync = new ConditionVariable();
    private int mRecordingRotation;
    private CamcorderProfile mCameraCamcorderProfile;
    private String mVideoTempPath;
    private int mCurrentOrientation;
    private RecordStreamStatus mRecordStreamCallback;
    private ContentValues mCapContentValues;
    private ContentValues mRawContentValues;
    private ContentValues mVideoContentValues;
    private boolean mIsInReviewMode = false;
    private byte[] mJpegData;
    private int mImageWidth;
    private int mImageHeight;
    private Uri mUri;

    private TotalCaptureResult mCaptureResult;
    private ParcelFileDescriptor mVideoFileDescriptor;
    private long mShutterDateTaken;
    private long mRequestSizeLimit = 0;
    private int mRequestDurationLimit;

    /**
     * when file have saved ,need notify Manager
     */
    protected OnFileSavedListener mMediaSavedListener = new OnFileSavedListener() {

        @Override
        public void onMediaSaved(Uri uri) {
            Log.i(TAG, "onMediaSaved uri = " + uri);
            mUri = uri;
            // only video saving need to dismiss saving dialog.
            mAppUi.dismissSavingProgress();
            mAppController.notifyNewMedia(uri);
        }
    };

    public CaptureMode(AppController app, ModuleListener moduleListener) {
        super(app, moduleListener);
        mAppUi.switchShutterButtonLayout(R.layout.camera_shutter_photo_video_v2);
        TAG = CaptureMode.class.getSimpleName() + "(" + FEATURE_TAG + ")";
    }

    @Override
    protected int getModeId() {
        return ModeChangeListener.MODE_CAPTURE;
    }

    private CaptureStreamCallback mCaptureStreamCallback = new CaptureStreamCallback() {
       @Override
       public void onCaptureCompleted(Image image) {
           if (image.getFormat() == ImageFormat.JPEG) {
                            int width = image.getWidth();
                            int height = image.getHeight();
                            byte[] jpegData = Utils.acquireJpegBytesAndClose(image);
                mJpegData = jpegData;
                mImageWidth = width;
                mImageHeight = height;
                if (!mIsCaptureIntent) {
                            int orientation = Exif.getOrientation(jpegData);
                            updateCaptureContentValues(width, height, orientation);
                            mCameraServices.getMediaSaver().addImage(jpegData, mCapContentValues,
                                    mMediaSavedListener,
                                    mAppController.getActivity().getContentResolver());
                }
                mAppController.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mIsCaptureIntent) {
                            mAppUi.showReviewView(mJpegData, mImageHeight);
                            mAppUi.switchShutterButtonLayout(R.layout.camera_shutter_ok_cancel_v2);
                        } else {
                            mAppUi.setAllCommonViewEnable(true);
                            mAppUi.setSwipeEnabled(true);
                        }
                                }
                            });
            } else if (image.getFormat() == ImageFormat.RAW_SENSOR) {
                //try wait for result callback
                mRawCallbackSync.block();
                mRawCallbackSync.close();
                CameraCharacteristics characteristics = Utils.getCameraCharacteristics(
                        mAppController.getActivity(), mSettingServant.getCameraId());
                ByteArrayOutputStream rawImage = Utils.convertRawToDng(
                        image, characteristics, mCaptureResult);
                image.close();
                //no need to update UI, since Jpeg has done
                if (rawImage != null) {
                    updateRawCaptureContentValues();
                    try {
                        byte[] dngData = rawImage.toByteArray();
                        rawImage.close();
                        mCameraServices.getMediaSaver().addImage(dngData, mRawContentValues,
                        mMediaSavedListener, mAppController.getActivity().getContentResolver());
                    } catch (IOException e) {
                        Log.e(TAG, "dng output stream error");
                    }
                }
            }
        }
    };

    private CaptureStreamCallback mVssStreamCallback = new CaptureStreamCallback() {
        @Override
        public void onCaptureCompleted(Image image) {
            if (image.getFormat() == ImageFormat.JPEG) {
                int width = image.getWidth();
                int height = image.getHeight();
                byte[] jpegData = Utils.acquireJpegBytesAndClose(image);
                int orientation = Exif.getOrientation(jpegData);
                Log.i(TAG, "parse jpeg orientation:" + orientation);
                updateCaptureContentValues(width, height, orientation);
                mCameraServices.getMediaSaver().addImage(jpegData,
                               mCapContentValues,
                               mMediaSavedListener,
                               mAppController.getActivity().getContentResolver());
                mAppController.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAppUi.setShutterButtonEnabled(true, false/**video shutter**/);
                    }
                });
            }
        }
    };

    @Override
    protected CaptureStreamCallback getCaptureStreamCallback() {
        if (mRecording) {
            return mVssStreamCallback;
        } else {
            return mCaptureStreamCallback;
        }
    }

    @Override
    protected RecordStreamStatus getRecordStreamCallback() {
        if (mRecordStreamCallback == null) {
            mRecordStreamCallback = new RecordStreamStatus() {
                @Override
                public void onRecordingStarted() {

                }
                @Override
                public void onRecordingStoped(boolean video_saved) {
                    Log.i(TAG, "onRecordingStoped");
                    mSettingCtroller.doSettingChange(SettingKeys.KEY_VIDEO, "off", false);
                    mAppController.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mIsCaptureIntent) {
                                FileDescriptor mFileDescriptor = null;
                                if (mVideoFileDescriptor != null) {
                                    mFileDescriptor = mVideoFileDescriptor
                                            .getFileDescriptor();
                                }
                                mAppUi.showReviewView(Utils
                                        .createBitmapFromVideo(
                                                mVideoTempPath,
                                                mFileDescriptor,
                                                mCameraCamcorderProfile.videoFrameWidth));
                                mAppUi.switchShutterButtonLayout(
                                        R.layout.camera_shutter_ok_cancel_v2);
                                mIsInReviewMode = true;
                            }
                        }
                    });

                    if (mVideoFileDescriptor == null) {
                        updateVideoContentValues();
                        mCameraServices.getMediaSaver().addVideo(
                                mVideoTempPath,
                                mVideoContentValues,
                                mMediaSavedListener,
                                mAppController.getActivity()
                                        .getContentResolver());
                    }
                    mVideoTempPath = null;
                }

                @Override
                public void onInfo(int what, int extra) {
                    Log.v(TAG, "[onInfo] what = " + what + "   extra = " + extra);
                    switch (what) {
                    case IRecordStream.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
                        if (mRecording) {
                            mAppUi.showInfo(mActivity.getResources().getString(
                                    R.string.video_reach_size_limit));
                            videoShutterButtonClicked();
                        }
                        break;
                    case IRecordStream.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
                        if (mRecording) {
                            videoShutterButtonClicked();
                        }
                        break;
                    default :
                        break;
                    }
                }

                @Override
                public void onError(int what, int extra) {

                }
            };
        }
        return mRecordStreamCallback;
    }

    @Override
    protected Size getPreviewSize() {
        if (mRecording && mCameraCamcorderProfile != null) {
            return new Size(mCameraCamcorderProfile.videoFrameWidth,
                    mCameraCamcorderProfile.videoFrameHeight);
        }
        if (MediaStore.ACTION_VIDEO_CAPTURE.equals(mIntent.getAction())) {
            mCameraCamcorderProfile = mVideoHelper.fetchProfile(
                    mVideoHelper.getRecordingQuality(
                            Integer.valueOf(mSettingServant.getCameraId())),
                    Integer.valueOf(mSettingServant.getCameraId()));
            return new Size(mCameraCamcorderProfile.videoFrameWidth,
                    mCameraCamcorderProfile.videoFrameHeight);
        }
        return super.getPreviewSize();
    }

    @Override
    protected Size getCaptureSize() {
        if (mRecording && mCameraCamcorderProfile != null) {
            return new Size(mCameraCamcorderProfile.videoFrameWidth,
                    mCameraCamcorderProfile.videoFrameHeight);
        }
        CameraCharacteristics characteristics = Utils.getCameraCharacteristics(
                mAppController.getActivity(), mSettingServant.getCameraId());
        if (DNG_ON.equals(mSettingServant.getSettingValue(SettingKeys.KEY_DNG))) {
            mDngStream.updateCameraCharacteristics(characteristics);
        }
        return super.getCaptureSize();
    }

    @Override
    protected boolean changingModePictureSize() {
        mCaptureSurface = mCaptureController.getCaptureInputSurface().get(
                ICaptureStream.CAPUTRE_SURFACE_KEY);
        Log.i(TAG, "changingModePictureSize :" + mCaptureSurface);
        if (DNG_ON.equals(mSettingServant.getSettingValue(SettingKeys.KEY_DNG))) {
            mRawCaptureSurface = mDngStream.getCaptureInputSurface()
                    .get(IDngStream.CAPUTRE_RAW_SURFACE_KEY);
        }
        return false;
    }

    protected boolean updatePictureSize() {
        Log.i(TAG, "[updatePictureSize]+");
        boolean pictureSizeChange;
        int format = getCaptureFormat();
        Size pictureSize = getCaptureSize();
        if (pictureSize == null) {
            Log.i(TAG, "why picture size is nulll?");
        return false;
        }
        Log.i(TAG, "[updatePictureSize]- pictureSize = "
                + pictureSize.getWidth() + " x " + pictureSize.getHeight()
                + " format = " + format);
        if (DNG_ON.equals(mSettingServant.getSettingValue(SettingKeys.KEY_DNG))) {
            pictureSizeChange = mDngStream.updateCaptureSize(pictureSize, format);
            mDngStream.setCaptureStreamCallback(getCaptureStreamCallback());
        } else {
            mDngStream.releaseCaptureStream();
            pictureSizeChange = mCaptureController.updateCaptureSize(pictureSize, format);
            mCaptureController.setCaptureStreamCallback(getCaptureStreamCallback());
        }
        pictureSizeChange = changingModePictureSize() || pictureSizeChange;
        if (pictureSizeChange && !MediaStore.ACTION_VIDEO_CAPTURE.equals(mIntent.getAction())) {
            String pictureFormat = pictureSize.getWidth()
                    + "x" + pictureSize.getHeight() + "-superfine";
            mAppUi.showLeftCounts(Utils.getImageSize(pictureFormat), true);
        }
        return pictureSizeChange;
    }

    @Override
    public void open(StreamManager streamManager, ViewGroup parentView,
            boolean isCaptureIntent) {
        Log.i(TAG, "[open]+");
        mStreamManager = streamManager;
        super.open(streamManager, parentView, isCaptureIntent);
        mDngStream = streamManager.getDngStreamController();
        Log.i(TAG, "[open]-");
    }

    @Override
    public void resume() {
        Log.i(TAG, "[resume]+");
        mPaused = false;
        super.resume();
        if (MediaStore.ACTION_VIDEO_CAPTURE.equals(mIntent.getAction()) && !mIsInReviewMode) {
            mAppUi.showLeftTime(((mCameraCamcorderProfile.videoBitRate +
                    mCameraCamcorderProfile.audioBitRate) >> 3) / 1000);
        }
        Log.i(TAG, "[resume]-");
    }

    @Override
    public void pause() {
        Log.i(TAG, "[pause]+");
        mPaused = true;
        super.pause();
        mDngStream.releaseCaptureStream();
        if (mRecording) {
            onShutterClicked(true/**video**/);
        }
        if (mIsCaptureIntent && MediaStore.ACTION_IMAGE_CAPTURE.equals(mIntent.getAction())) {
            mAppUi.switchShutterButtonLayout(R.layout.camera_shutter_photo_v2);
            mAppUi.hideReviewView();
        }
        Log.i(TAG, "[pause]-");
    }

    @Override
    public void close() {
        Log.i(TAG, "[close]+");
        super.close();
        Log.i(TAG, "[close]-");
    }

    @Override
    public void onShutterPressed(boolean isVideo) {

    }

    @Override
    public void onShutterClicked(boolean isVideo) {
        if (isVideo) {
            videoShutterButtonClicked();
        } else {
            if (mRecording) {
                videoSnapshotShutterButtonClicked();
            } else {
                photoShutterButtonClicked();
            }
        }
    }

    @Override
    public void onShutterLongPressed(boolean isVideo) {

    }

    @Override
    public void onShutterReleased(boolean isVideo) {

    }

    @Override
    public void onOrientationChanged(int orientation) {
        mCurrentOrientation = orientation;
    }

    @Override
    public void onOkClick() {
        String action = mIntent.getAction();
        Log.i(TAG, "[onOkClick], action:" + action);
        if (MediaStore.ACTION_IMAGE_CAPTURE.equals(action)) {
            doPhotoAttach();
        } else if (MediaStore.ACTION_VIDEO_CAPTURE.equals(action)) {
            doVideoAttach();
        }
        mIsInReviewMode = false;
    }

    @Override
    public void onCancelClick() {
        Log.i(TAG, "[onCancelClick]...");
        doCancel();
        mIsInReviewMode = false;
    }

    @Override
    protected boolean doSettingChanged(Map<String, String> result) {
        boolean needChangeSession = false;
        final String dngChanged = result.get(SettingKeys.KEY_DNG);
        if (dngChanged != null) {
            needChangeSession = !mRecording;
            updatePictureSize();
        }
        String video3dnr = result.get(SettingKeys.KEY_VIDEO_3DNR);
        if (video3dnr != null) {
            mModuleListener.requestChangeCaptureRequets(false/*sync*/,
                    mRecording ? RequestType.RECORDING : RequestType.PREVIEW,
                    CaptureType.REPEATING_REQUEST);
        }
        return needChangeSession;
    }

    protected void updateCaredSettingChangedKeys() {
        super.updateCaredSettingChangedKeys();
        addCaredSettingChangedKeys(SettingKeys.KEY_DNG);
        addCaredSettingChangedKeys(SettingKeys.KEY_VIDEO_3DNR);
    }

    @Override
    public void configuringSessionOutputs(List<Surface> sessionOutputSurfaces,
            boolean bottomCamera) {
        Log.i(TAG, "configuringOutputs");
        Assert.assertNotNull(sessionOutputSurfaces);
        checkPreviewSurfaceReady();

        Log.i(TAG, "configuringOutputs, preview surface: " + mPreviewSurface);
        sessionOutputSurfaces.add(mPreviewSurface);
        Log.i(TAG, "configuringOutputs, capture surface: " + mCaptureSurface);
        if (mCaptureSurface != null && mCaptureSurface.isValid()) {
            sessionOutputSurfaces.add(mCaptureSurface);
        }

        Log.i(TAG, "configuringOutputs, raw surface:" + mRawCaptureSurface);
        //TODO: if dng is on
        if ((DNG_ON.equals(mSettingServant.getSettingValue(SettingKeys.KEY_DNG)))
                && mRawCaptureSurface != null
                && !mRecording) {
            sessionOutputSurfaces.add(mRawCaptureSurface);
        }

        Log.i(TAG, "configuringOutputs, record surface:" + mRecordSurface);
        if (mRecording && mRecordSurface != null) {
            sessionOutputSurfaces.add(mRecordSurface);
        }
    }

    @Override
    public void configuringSessionRequests(Map<RequestType,
            CaptureRequest.Builder> requestBuilders, boolean bottomCamera) {
        Set<RequestType> keySet = requestBuilders.keySet();
        Iterator<RequestType> iterator = keySet.iterator();
        while (iterator.hasNext()) {
            RequestType requestType = iterator.next();
            configre3DnrValue(requestBuilders.get(requestType));
            Log.i(TAG, "configuringSessionRequests requestType = "
                    + requestType + " request number = " + keySet.size());
            switch (requestType) {
            case PREVIEW:
                configuringPreviewRequests(requestBuilders.get(requestType));
                break;
            case STILL_CAPTURE:
                configuringCaptureRequests(requestBuilders.get(requestType));
                break;
            case VIDEO_SNAP_SHOT:
                configuringCaptureRequests(requestBuilders.get(requestType));
                configreEisValue(requestBuilders.get(requestType));
                break;
            case RECORDING:
                configuringRecordingRequests(requestBuilders.get(requestType));
                break;
            default:
                break;
            }
        }
    }

    @Override
    public CaptureCallback getCaptureCallback() {
        return new CaptureCallback() {
            @Override
            public void onCaptureStarted(CameraCaptureSession session,
                    CaptureRequest request, long timestamp, long frameNumber) {
                mCameraServices.getSoundPlayback().play(ISoundPlayback.SHUTTER_CLICK);
            }
            @Override
            public void onCaptureCompleted(CameraCaptureSession session,
                    CaptureRequest request, TotalCaptureResult result) {
                Log.i(TAG, "CaptureCallback onCaptureCompleted request: "
                        + request + " result: " + result);
                mCaptureResult = result;
                mRawCallbackSync.open();
            }
        };
    }

    @Override
    public boolean onBackPressed() {
        if (mRecording) {
            onShutterClicked(true/**video shutter**/);
            return true;
        }
        return false;
    }

    public static final String CAN_SHARE           = "CanShare";
    @Override
    public void onPlay() {
        Log.i(TAG, "[onPlay]...");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Bundle extra = intent.getExtras();
        boolean canShowVideoShare = true;
        if (extra != null) {
            canShowVideoShare = extra.getBoolean(CAN_SHARE, true);
        }
        intent.putExtra(CAN_SHARE, canShowVideoShare);
        intent.setDataAndType(mUri,
                mVideoHelper.convertOutputFormatToMimeType(mCameraCamcorderProfile.fileFormat));
        mActivity.startActivity(intent);
    }

    @Override
    public void onRetake() {
        String action = mIntent.getAction();
        Log.i(TAG, "[onRetake], action:" + action);
        mAppUi.hideReviewView();
        if (MediaStore.ACTION_IMAGE_CAPTURE.equals(action)) {
            mAppUi.switchShutterButtonLayout(R.layout.camera_shutter_photo_v2);
        } else if (MediaStore.ACTION_VIDEO_CAPTURE.equals(action)) {
            mAppUi.switchShutterButtonLayout(R.layout.camera_shutter_video_v2);
        }
        mAppUi.setAllCommonViewEnable(true);
        mAppUi.setShutterButtonEnabled(true, false/**video shutter**/);
    }

    private void configuringPreviewRequests(CaptureRequest.Builder requestBuilder) {
        Assert.assertNotNull(requestBuilder);
        if (mPreviewSurface != null && mPreviewSurface.isValid()) {
            requestBuilder.addTarget(mPreviewSurface);
        }
    }

    private void configuringCaptureRequests(CaptureRequest.Builder requestBuilder) {
        Assert.assertNotNull(requestBuilder);
        requestBuilder.addTarget(mPreviewSurface);
        if (mCaptureSurface != null) {
            requestBuilder.addTarget(mCaptureSurface);
        }

        if ((DNG_ON.equals(mSettingServant.getSettingValue(SettingKeys.KEY_DNG)))
                && mRawCaptureSurface != null) {
            requestBuilder.addTarget(mRawCaptureSurface);
        }
        // jpeg quality
        requestBuilder.set(CaptureRequest.JPEG_QUALITY, JPEG_QUALITY);
        // jpeg orientation
        requestBuilder.set(CaptureRequest.JPEG_ORIENTATION,
                Utils.getJpegRotation(mCurrentOrientation, Utils.getCameraCharacteristics(
                        mAppController.getActivity(), mSettingServant.getCameraId())));
    }

    private void configuringRecordingRequests(CaptureRequest.Builder requestBuilder) {
        requestBuilder.addTarget(mPreviewSurface);
        if (mRecordSurface != null) {
            requestBuilder.addTarget(mRecordSurface);
        }
        configreEisValue(requestBuilder);
    }

    private void photoShutterButtonClicked() {
        Log.i(TAG, "photoShutterButtonClicked");
        if (mAppController.getAvailableStorageSpace() <= 0) {
            Log.w(TAG, "Not enough space or storage not available, " +
                    "remaining:" + mAppController.getAvailableStorageSpace());
            return;
        }
        mShutterDateTaken = System.currentTimeMillis();
        mAppUi.setAllCommonViewEnable(false);
        mAppUi.setSwipeEnabled(false);

        IAaaController aaaController = mModuleListener.get3AController(null);
        aaaController.aePreTriggerAndCapture();
    }

    private void videoSnapshotShutterButtonClicked() {
        mAppUi.setShutterButtonEnabled(false, false/**video shutter**/);
        mModuleListener.requestChangeCaptureRequets(false,
                RequestType.VIDEO_SNAP_SHOT, CaptureType.CAPTURE);
    }

    private void videoShutterButtonClicked() {
        Log.i(TAG, "videoShutterButtonClicked mRecording = " + mRecording);
        if (!mRecording && mAppController.getAvailableStorageSpace() <= 0) {
            Log.w(TAG, "Not enough space or storage not available, " +
                    "remaining:" + mAppController.getAvailableStorageSpace());
            mAppUi.showPickerManagerUi();
            return;
        }
        mAppUi.setShutterButtonEnabled(false, false);
        mAppUi.setShutterButtonEnabled(false, true);
        if (mRecording) {
            stopRecording();
            mAppUi.stopShowCommonUI(false);
            if (!mIsCaptureIntent) {
                mAppUi.showSettingUi();
                mAppUi.showIndicatorManagerUi();
                mAppUi.showPickerManagerUi();
            }
            mAppUi.switchShutterButtonImageResource(R.drawable.btn_video, true/**video shutter**/);
            mAppUi.setSwipeEnabled(true);
            mAppUi.showModeOptionsUi();
            mAppUi.setThumbnailManagerEnable(true);
            Size pictureSize = getCaptureSize();
            String pictureFormat = pictureSize.getWidth() +
                    "x" + pictureSize.getHeight() + "-superfine";
            mAppUi.showLeftCounts(Utils.getImageSize(pictureFormat), true);
        } else {
            mCameraServices.getSoundPlayback().play(ISoundPlayback.START_VIDEO_RECORDING);
            startRecording();
            mAppUi.stopShowCommonUI(true);
            mAppUi.hideSettingUi();
            mAppUi.switchShutterButtonImageResource(R.drawable.btn_video_mask,
                    true/**video shutter**/);
            CameraCharacteristics characteristics = Utils.getCameraCharacteristics(
                    mAppController.getActivity(), mSettingServant.getCameraId());
            mAppUi.setSwipeEnabled(false);
            mAppUi.dismissInfo(true);
            mAppUi.hideModeOptionsUi();
            mAppUi.hideIndicatorManagerUi();
            mAppUi.hidePickerManagerUi();
            mAppUi.setThumbnailManagerEnable(false);
            long bytePerMs = ((mCameraCamcorderProfile.videoBitRate +
                    mCameraCamcorderProfile.audioBitRate) >> 3) / 1000;
            mAppUi.showLeftTime(bytePerMs);
        }
    }

    private void startRecording() {
        Log.i(TAG, "[startRecording]+");
        mRecording = true;
        pauseAudioPlayback();
        prepareRecording();
        updatePictureSize();
        updatePreviewSize(new PreviewSurfaceCallback() {
            @Override
            public void onPreviewSufaceIsReady(boolean surfaceChanged) {
                Log.i(TAG, "[startRecording] onPreviewSufaceIsReady");
                mModuleListener.requestChangeSessionOutputs(true/*sync*/);
                mSettingCtroller.doSettingChange(SettingKeys.KEY_VIDEO, "on", false);
                mModuleListener.requestChangeCaptureRequets(true/*sync*/,
                        RequestType.RECORDING, CaptureType.REPEATING_REQUEST);
                mRecordController.startRecord();
                mAppController.enableKeepScreenOn(true);
                mHandler.postDelayed(mEnableShutterButtonRunnable,
                        SHUTTER_BUTTON_TIMEOUT);
                Log.i(TAG, "[startRecording]-");
            }
        });
    }

    private void stopRecording() {
        Log.i(TAG, "stopRecording");
        mRecording = false;
        releaseAudioFocus();
        if (mPaused) {
            doStopRecording(true);
            return;
        }
        updatePictureSize();
        updatePreviewSize(new PreviewSurfaceCallback() {
            @Override
            public void onPreviewSufaceIsReady(boolean surfaceChanged) {
                doStopRecording(false);
            }
        });
    }

    private void initializeRequestedLimits() {
        closeVideoFileDescriptor();
        initializeLimiteds();
    }

    private void initializeLimiteds() {
        mRequestSizeLimit = mVideoHelper.getRequestSizeLimit(
                mCameraCamcorderProfile, true, mIsCaptureIntent, mIntent);
        mRequestDurationLimit = mIntent.getIntExtra(
                MediaStore.EXTRA_DURATION_LIMIT, 0);
        if (mIsCaptureIntent) {
            Uri saveUri = mIntent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
            if (saveUri != null) {
                try {
                    mVideoFileDescriptor = mActivity.getContentResolver()
                            .openFileDescriptor(saveUri, "rw");
                    mUri = saveUri;
                } catch (java.io.FileNotFoundException ex) {
                    Log.e(TAG, ex.toString());
                }
            }
        }
    }

    private void closeVideoFileDescriptor() {
        mVideoHelper.closeVideoFileDescriptor(mVideoFileDescriptor);
        mVideoFileDescriptor = null;
    }

    private void prepareRecording() {
        int cameraId = Integer.valueOf(mSettingServant.getCameraId());
        int videoQualityValue = mVideoHelper.getRecordingQuality(cameraId);
        mCameraCamcorderProfile = mVideoHelper.fetchProfile(videoQualityValue, cameraId);
        String mirc = mSettingServant.getSettingValue(SettingKeys.KEY_VIDEO_RECORD_AUDIO);
        boolean enableAudio = "on".equals(mirc);
        mRecordingRotation = Utils.getRecordingRotation(mCurrentOrientation,
                Utils.getCameraCharacteristics(mAppController.getActivity(),
                        mSettingServant.getCameraId()));
        if (mIsCaptureIntent) {
            initializeRequestedLimits();
        }
        if (mVideoFileDescriptor != null) {
            mRecordController.setOutputFile(mVideoFileDescriptor
                    .getFileDescriptor());
        } else {
            mVideoTempPath = mVideoHelper.generateVideoFileName(
                    mCameraCamcorderProfile.fileFormat, null);
            mRecordController.setOutputFile(mVideoTempPath);
        }
        Log.i(TAG, "prepareRecording enableAudio = " + enableAudio);
        prepareMediaRecordingParamters();
        mRecordController.setMaxFileSize(mVideoHelper
                .getRecorderMaxSize(mRequestSizeLimit));
        mRecordController.setMaxDuration(1000 * mRequestDurationLimit);
        mRecordController.setRecordingProfile(mCameraCamcorderProfile);
        mRecordController.enalbeAudioRecording(enableAudio);
        mRecordController.setOutputFile(mVideoTempPath);
        mRecordController.setOrientationHint(mRecordingRotation);
        mRecordController.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mRecordController.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        if (FeatureSwitcher.isHdRecordingEnabled()) {
            mRecordController.setHDRecordMode(
                    mSettingServant.getSettingValue(SettingKeys.KEY_VIDEO_HD_AUDIO_RECORDING));
        }
        mRecordController.prepareRecord();

        mRecordSurface = mRecordController.getRecordInputSurface();
    }

    protected void checkPreviewSurfaceReady() {
        mPreviewSurface = mPreviewController.getPreviewInputSurfaces().
                get(IPreviewStream.PREVIEW_SURFACE_KEY);
    }

    private void updateRawCaptureContentValues() {
        mRawContentValues = new ContentValues();
        String title = Utils.createDngName(mShutterDateTaken);
        String filename = title + "_RAW" + ".dng";
        String filePath = Storage.getFileDirectory() + '/' + filename;

        mRawContentValues.put(ImageColumns.DATE_TAKEN, mShutterDateTaken);
        mRawContentValues.put(ImageColumns.TITLE, title);
        mRawContentValues.put(ImageColumns.DISPLAY_NAME, filename);
        mRawContentValues.put(ImageColumns.DATA, filePath);
        Log.i(TAG, "updateRawCaptureContentValues filename: " + filename);
    }

    private void updateCaptureContentValues(int width, int height, int orientation) {
        mCapContentValues = new ContentValues();
        long dateTaken;
        String title;
        String filename;
        if (DNG_ON.equals(mSettingServant.getSettingValue(SettingKeys.KEY_DNG))) {
            dateTaken = mShutterDateTaken;
            title = Utils.createJpegName(dateTaken);
            filename = title + "_RAW" + ".jpg";
        } else {
            dateTaken = System.currentTimeMillis();
            title = Utils.createJpegName(dateTaken);
            filename = title + ".jpg";
        }

        String mime = "image/jpeg";
        String path = Storage.getFileDirectory() + '/' + filename;
        String tmpPath = path + ".tmp";

        mCapContentValues.put(ImageColumns.DATE_TAKEN, dateTaken);
        mCapContentValues.put(ImageColumns.TITLE, title);
        mCapContentValues.put(ImageColumns.DISPLAY_NAME, filename);
        mCapContentValues.put(ImageColumns.DATA, path);
        mCapContentValues.put(ImageColumns.MIME_TYPE, mime);

        mCapContentValues.put(ImageColumns.WIDTH, width);
        mCapContentValues.put(ImageColumns.HEIGHT, height);
        mCapContentValues.put(ImageColumns.ORIENTATION, orientation);

        mLocation = mLocationManager.getCurrentLocation();
        if (mLocation != null) {
            mCapContentValues.put(ImageColumns.LATITUDE,
                    mLocation.getLatitude());
            mCapContentValues.put(ImageColumns.LONGITUDE,
                    mLocation.getLongitude());
        }

        Log.i(TAG, "updateCaptureContentValues orientation: " + orientation);
    }

    private void updateVideoContentValues() {
        long dateTaken = System.currentTimeMillis();
        String title = mVideoHelper.createFileTitle(dateTaken, mAppController);
        String mime = mVideoHelper.convertOutputFormatToMimeType(
                mCameraCamcorderProfile.fileFormat);
        String filename = title + mVideoHelper.convertOutputFormatToFileExt(
                mCameraCamcorderProfile.fileFormat);
        String path = Storage.getFileDirectory() + '/' + filename;
        long duration = mVideoHelper.getDuration(mVideoTempPath);

        mVideoContentValues = new ContentValues();
        mVideoContentValues.put(Video.Media.DURATION, duration);
        mVideoContentValues.put(Video.Media.TITLE, title);
        mVideoContentValues.put(Video.Media.DISPLAY_NAME, filename);
        mVideoContentValues.put(Video.Media.DATE_TAKEN, dateTaken);
        mVideoContentValues.put(MediaColumns.DATE_MODIFIED, dateTaken / 1000);
        mVideoContentValues.put(Video.Media.MIME_TYPE, mime);
        mVideoContentValues.put(Video.Media.DATA, path);
        mVideoContentValues.put(Video.Media.WIDTH, mCameraCamcorderProfile.videoFrameWidth);
        mVideoContentValues.put(Video.Media.HEIGHT, mCameraCamcorderProfile.videoFrameHeight);
        mVideoContentValues.put(Video.Media.ORIENTATION, mRecordingRotation);
        mVideoContentValues.put(Video.Media.RESOLUTION,
                Integer.toString(mCameraCamcorderProfile.videoFrameWidth) + "x"
                + Integer.toString(mCameraCamcorderProfile.videoFrameHeight));
        mVideoContentValues.put(Video.Media.SIZE, new File(mVideoTempPath).length());

        mLocation = mLocationManager.getCurrentLocation();
        if (mLocation != null) {
            mVideoContentValues.put(ImageColumns.LATITUDE,
                    mLocation.getLatitude());
            mVideoContentValues.put(ImageColumns.LONGITUDE,
                    mLocation.getLongitude());
        }
    }

    private String createName(long dateTaken) {
        Date date = new Date(dateTaken);
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                mAppController.getActivity().getString(R.string.video_file_name_format));

        return dateFormat.format(date);
    }

    private static final String             EXTRA_PHOTO_CROP_VALUE = "crop";
    private static final String             TEMP_CROP_FILE_NAME = "crop-temp";
    private static final int                REQUEST_CROP = 1000;

    private void doPhotoAttach() {
        // add image do data base.
        int orientation = Exif.getOrientation(mJpegData);
        updateCaptureContentValues(mImageWidth, mImageHeight, orientation);
        mCameraServices.getMediaSaver().addImage(mJpegData, mCapContentValues,
                mMediaSavedListener, mAppController.getActivity().getContentResolver());

        Uri saveUri = mIntent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
        String cropValue = mIntent.getStringExtra(EXTRA_PHOTO_CROP_VALUE);
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
                        outputStream.write(mJpegData);
                        outputStream.close();
                    }

                    mAppController.setResultExAndFinish(Activity.RESULT_OK);
                } catch (IOException ex) {
                    Log.e(TAG, "IOException, when doAttach");
                    // ignore exception
                } finally {
                    Utils.closeSilently(outputStream);
                }
            } else {
                Bitmap bitmap = Utils.makeBitmap(mJpegData, 50 * 1024);
                bitmap = Utils.rotate(bitmap, orientation);
                mAppController.setResultExAndFinish(Activity.RESULT_OK,
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
                tempStream.write(mJpegData);
                tempStream.close();
                tempUri = Uri.fromFile(path);
            } catch (FileNotFoundException ex) {
                mAppController.setResultExAndFinish(Activity.RESULT_CANCELED);
                Log.e(TAG, "FileNotFoundException, when doAttach");
                return;
            } catch (IOException ex) {
                mAppController.setResultExAndFinish(Activity.RESULT_CANCELED);
                Log.e(TAG, "IOException2, when doAttach");
                return;
            } finally {
                Utils.closeSilently(tempStream);
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

           /* if (mContext.isSecureCamera()) {
                newExtras.putBoolean(CropExtras.KEY_SHOW_WHEN_LOCKED, true);
            }*/

            Intent cropIntent = new Intent("com.android.camera.action.CROP");
            cropIntent.setData(tempUri);
            cropIntent.putExtras(newExtras);

            mActivity.startActivityForResult(cropIntent, REQUEST_CROP);
        }
    }

    private void doVideoAttach() {
        Intent resultIntent = new Intent();
        int resultCode;
        resultCode = Activity.RESULT_OK;
        resultIntent.setData(mUri);
        mAppController.setResultExAndFinish(resultCode, resultIntent);
    }

    private void doCancel() {
        mAppController.setResultExAndFinish(Activity.RESULT_CANCELED, new Intent());
    }

    private void configre3DnrValue(Builder requestBuilder) {
        List<String> values = mSettingServant.getSupportedValues(SettingKeys.KEY_VIDEO_3DNR);
        if (values == null || values.size() <= 1) {
            Log.i(TAG, "not support 3dnr,not configure 3dnr session");
            return;
        }
        String nr3dValue = mSettingServant.getSettingValue(SettingKeys.KEY_VIDEO_3DNR);
        Log.i(TAG, "configre3DnrValue nr3dValue = " + nr3dValue);
        if ("on".equals(nr3dValue)) {
            requestBuilder.set(TagRequest.STATISTICS_3DNR_MODE,
                    TagMetadata.MTK_3DNR_MODE_ON);
        } else {
            requestBuilder.set(TagRequest.STATISTICS_3DNR_MODE,
                    TagMetadata.MTK_3DNR_MODE_OFF);
        }
    }

    private void configreEisValue(Builder requestBuilder) {
        String eisValue = mSettingServant.getSettingValue(SettingKeys.KEY_VIDEO_EIS);
        Log.i(TAG, "configuringRecordingRequests eisValue = " + eisValue);
        if ("on".equals(eisValue)) {
            requestBuilder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON);
        } else {
            requestBuilder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF);
        }
    }

    private void prepareMediaRecordingParamters() {
        List<String> recordParamters = new ArrayList<String>();
        recordParamters.add(IRecordStream.RECORDER_INFO_SUFFIX
                + IRecordStream.MEDIA_RECORDER_INFO_BITRATE_ADJUSTED);
        recordParamters.add(IRecordStream.RECORDER_INFO_SUFFIX
                + IRecordStream.MEDIA_RECORDER_INFO_FPS_ADJUSTED);
        recordParamters.add(IRecordStream.RECORDER_INFO_SUFFIX
                + IRecordStream.MEDIA_RECORDER_INFO_START_TIMER);
        recordParamters.add(IRecordStream.RECORDER_INFO_SUFFIX
                + IRecordStream.MEDIA_RECORDER_INFO_WRITE_SLOW);
        recordParamters.add(IRecordStream.RECORDER_INFO_SUFFIX
                + IRecordStream.MEDIA_RECORDER_INFO_CAMERA_RELEASE);
        if (mIsCaptureIntent) {
            recordParamters.add(IRecordStream.RECORDER_INFO_SUFFIX
                    + IRecordStream.MEDIA_RECORDER_INFO_RECORDING_SIZE);
        }
        mRecordController.setMediaRecorderParameters(recordParamters);
    }

    private Runnable mEnableShutterButtonRunnable = new Runnable() {
        @Override
        public void run() {
            mAppUi.setShutterButtonEnabled(true, false);
            mAppUi.setShutterButtonEnabled(true, true);
        }
    };

    private void doStopRecording(boolean activityPaused) {
        Log.i(TAG, "[doStopRecording]+ mRecordSurface : "
                + mRecordSurface);
        mRecordSurface = null;
        if (!activityPaused) {
            mModuleListener.requestChangeSessionOutputs(true/*sync*/);
            mModuleListener.requestChangeCaptureRequets(true/*sync*/,
                    RequestType.PREVIEW, CaptureType.REPEATING_REQUEST);
        }
        try {
            mRecordController.stopRecord(true);
            if (mVideoFileDescriptor == null) {
                // If video file descriptor is null, it needs to save video file locally.
                mAppUi.showSavingProgress(mAppController.getActivity()
                        .getResources().getString(R.string.saving));
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            Log.e(TAG, "doStopRecording with exception:" + e);
            mCameraCamcorderProfile = null;
        } finally {
            mCameraServices.getSoundPlayback().play(ISoundPlayback.STOP_VIDEO_RECORDING);
            mAppController.enableKeepScreenOn(false);
            mAppUi.setShutterButtonEnabled(true, false);
            mAppUi.setShutterButtonEnabled(true, true);
            Log.i(TAG, "[doStopRecording]-");
        }
    }
}
