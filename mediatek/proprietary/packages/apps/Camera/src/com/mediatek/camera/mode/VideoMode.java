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
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.location.Location;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaActionSound;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;

import com.android.camera.ExtensionHelper;
import com.android.camera.R;
//TODO: remove Storage
import com.android.camera.Storage;

import com.mediatek.camera.AdditionManager;
import com.mediatek.camera.ICameraAddition.AdditionActionType;
import com.mediatek.camera.ICameraMode.ModeState;
import com.mediatek.camera.ICameraContext;
import com.mediatek.camera.mode.facebeauty.VfbQualityRule;
import com.mediatek.camera.mode.facebeauty.VideoFaceBeautyRule;
import com.mediatek.camera.platform.ICameraAppUi.CommonUiType;
import com.mediatek.camera.platform.ICameraAppUi.ShutterButtonType;
import com.mediatek.camera.platform.ICameraAppUi.ViewState;
import com.mediatek.camera.platform.ICameraView;
import com.mediatek.camera.platform.IFileSaver.FILE_TYPE;
import com.mediatek.camera.platform.IFileSaver.OnFileSavedListener;
import com.mediatek.camera.platform.IFocusManager.FocusListener;
import com.mediatek.camera.platform.Parameters;
import com.mediatek.camera.setting.preference.ListPreference;
import com.mediatek.camera.setting.SettingConstants;
import com.mediatek.camera.ui.RecordingView;
import com.mediatek.camera.ui.ReviewManager;
import com.mediatek.camera.util.Log;
import com.mediatek.camera.util.Util;
//TODO:MediaRecorderEx
import com.mediatek.media.MediaRecorderEx;

import junit.framework.Assert;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;

public class VideoMode extends CameraMode implements MediaRecorder.OnErrorListener,
        MediaRecorder.OnInfoListener, FocusListener {

    protected static final int MEDIA_RECORDER_INFO_BITRATE_ADJUSTED = 898;
    protected static final int MEDIA_RECORDER_INFO_RECORDING_SIZE = 895;
    protected static final int MEDIA_RECORDER_INFO_FPS_ADJUSTED = 897;
    protected static final int MEDIA_RECORDER_INFO_START_TIMER = 1998;
    protected static final int MEDIA_RECORDER_INFO_WRITE_SLOW = 899;
    protected static final int MEDIA_RECORDER_ENCODER_ERROR = -1103;
    protected static final int MEDIA_RECORDER_INFO_CAMERA_RELEASE = 1999;

    protected static final int INIT_SHUTTER_STATUS = 6;
    protected static final int STOP_RETURN_UNVALID = 3;
    protected static final int SHOW_SAVING_DIALOG = 9;
    protected static final int UPDATE_RECORD_TIME = 5;
    protected static final int STOP_SHOW_ALERT = 4;
    protected static final int UPDATE_SNAP_UI = 15;
    protected static final int STOP_NORMAL = 1;
    protected static final int STOP_RETURN = 2;
    protected static final int STOP_FAIL = 5;
    protected boolean mIsMediaRecoderRecordingPaused = false;
    protected boolean mIsMediaRecorderRecording = false;
    protected boolean mIsRecorderCameraReleased = true;
    protected boolean mIsAutoFocusCallback = false;
    protected boolean mIsRecordAudio = false;

    protected long mRecordingPausedDuration = 0;
    protected long mTotalRecordingDuration = 0;
    protected long mRecordingStartTime;

    protected int mStoppingAction = STOP_NORMAL;
    protected int mSaveTempVideo = SystemProperties.getInt("camera.save.temp.video", 0);

    protected String mCurrentVideoFilename;
    protected String mConditionSettingKey = SettingConstants.KEY_VIDEO;
    protected String mVideoTempPath;
    protected String mVideoFilename;

    protected ParcelFileDescriptor mVideoFileDescriptor;
    protected VideoPreviewRule mVideoPreviewSizeRule;
    protected AdditionManager mAdditionManager;
    protected VideoModeHelper mVideoModeHelper;
    protected MediaRecorder mMediaRecorder;
    protected RecordingView mRecordingView;
    protected VideoHdrRule mVideoHdrRul;
    protected CamcorderProfile mProfile;
    protected Thread mVideoSavingTask;
    protected Parameters mParameters;
    protected Uri mCurrentVideoUri;
    protected Handler mHandler;

    private static final String CONTENT_URI_PREFIX = "content://mms_temp_file";
    private static final String TAG = "VideoMode";
    private static final String RECORDER_INFO_SUFFIX = "media-recorder-info=";

    private static final int SLOW_MOTION_VIDEO_TAG_IN_FILE = 1;
    private static final int SLOW_MOTION_VIDEO_TAG_IN_DB = 4;
    private static final int DELAY_MSG_SCREEN_SWITCH = 2 * 60 * 1000;
    private static final int NORMAL_VIDEO_TAG_IN_DB = 0;
    private static final int MSG_CLEAR_SCREEN_DELAY = 7;
    private static final int START_FOCUSING = -1;
    private static final int FOCUS_IDLE = 3;
    private static final int FOCUSING = 1;
    private static final int FOCUSED = 2;

    private int mCurrentShowIndicator = 0;
    private int mRequestDurationLimit;
    private int mTimeLapseValue = 0;
    private int mFocusState = 0;

    // when user start VR is in landscape,and then rotate the device to Portrait,
    // at now,will stop VR,go to gallery check the video in fancy,will found the 
    // picture will be elongated.
    private int mRecordingStartOrientation;

    private long mRequestSizeLimit = 0;
    private long mFocusStartTime;

    private boolean mIsModeReleased = false;
    private boolean mIsTimeLapseEnable = false;

    private final AutoFocusCallback mAutoFocusCallback = new AutoFocusCallback();
    private ContentResolver mContentResolver;

    public VideoMode(ICameraContext cameraContext) {
        super(cameraContext);
        Log.i(TAG, "[VideoMode]constructor...");
        setModeState(ModeState.STATE_UNKNOWN);
        mAdditionManager = cameraContext.getAdditionManager();
        mIsModeReleased = false;
        mRecordingView = new RecordingView(mActivity);
        mRecordingView.setListener(mVideoPauseResumeListner);
        mRecordingView.setOrientation(mIModuleCtrl.getOrientationCompensation());
        mRecordingView.getView();
        mHandler = new MainHandler(mActivity.getMainLooper());
        setVideoRule();
        mICameraAppUi
                .switchShutterType(mIModuleCtrl.isNonePickIntent() ? ShutterButtonType.SHUTTER_TYPE_PHOTO_VIDEO
                        : ShutterButtonType.SHUTTER_TYPE_VIDEO);
        mVideoModeHelper = new VideoModeHelper(mActivity, mIModuleCtrl, mISettingCtrl);
    }

    @Override
    public boolean execute(ActionType type, Object... arg) {
        Log.i(TAG, "[execute]type = " + type);
        mAdditionManager.execute(type, true, arg);
        switch (type) {
        case ACTION_ON_CAMERA_OPEN:
            super.updateDevice();
            break;

        case ACTION_ON_CAMERA_PARAMETERS_READY:
            doOnCameraParameterReady(((Boolean) arg[0]).booleanValue());
            if (ModeState.STATE_RECORDING != getModeState()) {
            setModeState(ModeState.STATE_IDLE);
            }
            break;

        case ACTION_PHOTO_SHUTTER_BUTTON_CLICK:
            takeASnapshot();
            break;

        case ACTION_ON_STOP_PREVIEW:
            stopPreview();
            break;

        case ACTION_VIDEO_SHUTTER_BUTTON_CLICK:
            onVideoShutterButtonClick();
            break;

        case ACTION_ON_COMPENSATION_CHANGED:
            if (mRecordingView != null) {
                mRecordingView.onOrientationChanged((Integer) arg[0]);
            }
            break;

        case ACTION_ON_SINGLE_TAP_UP:
            if (arg[0] != null && arg[1] != null && arg[2] != null) {
                onSingleTapUp((View) arg[0], (Integer) arg[1], (Integer) arg[2]);
            }
            break;

        case ACTION_ON_KEY_EVENT_PRESS:
            if (KeyEvent.ACTION_DOWN == ((KeyEvent) arg[1]).getAction()) {
                return onKeyDown((Integer) arg[0], (KeyEvent) arg[1]);
            }
            break;

        case ACTION_ON_CAMERA_CLOSE:
            onCameraClose();
            setModeState(ModeState.STATE_CLOSED);
            break;

        case ACTION_ON_MEDIA_EJECT:
            onMediaEject();
            break;

        case ACTION_ON_RESTORE_SETTINGS:
            onRestoreSettings();
            break;

        case ACTION_ON_BACK_KEY_PRESS:
            return onBackPressed();

        case ACTION_ON_USER_INTERACTION:
            return onUserInteraction();

        case ACTION_OK_BUTTON_CLICK:
            mVideoModeHelper.doReturnToCaller(true, mCurrentVideoUri);
            break;

        case ACTION_DISABLE_VIDEO_RECORD:
            stopVideoRecordingAsync(true);
            break;
            
        case ACTION_ON_CONFIGURATION_CHANGED:
            mRecordingView.reInflate();
            break;

        default:
            return false;
        }

        return true;
    }

    @Override
    public boolean open() {
        mAdditionManager.open(true);
        return true;
    }

    @Override
    public boolean close() {
        Log.i(TAG, "[close]...");
        mHandler.removeMessages(INIT_SHUTTER_STATUS);
        if (!mIModuleCtrl.isVideoCaptureIntent()) {
            mICameraAppUi.updateSnapShotUIView(false);
        }
        if (mIFocusManager != null) {
            mIFocusManager.removeMessages();
        }
        if (mRecordingView != null) {
            mRecordingView.uninit();
        }
        mIsMediaRecoderRecordingPaused = false;
        mIsAutoFocusCallback = false;
        mIsModeReleased = true;
        mIFocusManager = null;
        return true;
    }

    public boolean capture() {
        return false;
    }

    @Override
    public void startFaceDetection() {
    }

    @Override
    public void stopFaceDetection() {
    }

    @Override
    public void setFocusParameters() {
        mIModuleCtrl.applyFocusParameters(!mIsAutoFocusCallback);
    }

    @Override
    public void playSound(int soundId) {
        if (soundId == MediaActionSound.FOCUS_COMPLETE && mIsMediaRecorderRecording
                && !mIsMediaRecoderRecordingPaused) {
            Log.i(TAG, "[playSound]Don't play focus sound when recording");
            return;
        }
        mCameraSound.play(soundId);
    }

    @Override
    public void autoFocus() {
        Log.i(TAG, "[autoFocus]");
        mFocusStartTime = System.currentTimeMillis();
        mICameraDevice.autoFocus(mAutoFocusCallback);
        setFocusState(FOCUSING);
    }

    @Override
    public void cancelAutoFocus() {
        Log.i(TAG, "[cancelAutoFocus] " + " mICameraDevice = " + mICameraDevice);
        if (mICameraDevice != null) {
            mICameraDevice.cancelAutoFocus();
        }
        setFocusState(FOCUS_IDLE);
        setFocusParameters();
        mIsAutoFocusCallback = false;
    }

    // from MediaRecorder.OnErrorListener
    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        Log.e(TAG, "[onError] what = " + what + ". extra = " + extra);
        if (MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN == what
                || MEDIA_RECORDER_ENCODER_ERROR == extra) {
            // We may have run out of space on the SD card.
            stopVideoRecordingAsync(true);
        }
    }

    // from MediaRecorder.OnInfoListener
    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        Log.v(TAG, "[onInfo] what = " + what + "   extra = " + extra);
        switch (what) {
        case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
            stopVideoRecordingAsync(true);
            break;

        case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
            stopVideoRecordingAsync(true);
            mICameraAppUi.showToastForShort(R.string.video_reach_size_limit);
            break;

        case MEDIA_RECORDER_INFO_CAMERA_RELEASE:
            if (mVideoSavingTask != null) {
                synchronized (mVideoSavingTask) {
                    Log.i(TAG, "[onInfo] MediaRecorder camera released");
                    mVideoSavingTask.notifyAll();
                }
            }
            break;

        case MEDIA_RECORDER_INFO_START_TIMER:
            if (!mIsTimeLapseEnable) {
                mRecordingStartTime = SystemClock.uptimeMillis();
                updateRecordingTime();
            }
            break;

        case MEDIA_RECORDER_INFO_FPS_ADJUSTED:
        case MEDIA_RECORDER_INFO_BITRATE_ADJUSTED:
            mICameraAppUi.showToast(R.string.video_bad_performance_drop_quality);
            break;

        case MEDIA_RECORDER_INFO_WRITE_SLOW:
            mICameraAppUi.showToast(R.string.video_bad_performance_auto_stop);
            stopVideoRecordingAsync(true);
            break;

        case MEDIA_RECORDER_INFO_RECORDING_SIZE:
            long limitSize = mVideoModeHelper.getRequestSizeLimit(null, false);
            if (0 < limitSize) {
                int progress = (int) (extra * 100 / limitSize);
                if (100 >= progress) {
                    mRecordingView.setCurrentSize(extra);
                    mRecordingView.setSizeProgress(progress);
                }
            }
            break;

        default:
            break;
        }
    }

    public CameraModeType getCameraModeType() {
        return CameraModeType.EXT_MODE_VIDEO;
    }

    public class SavingTask extends Thread {
        public void run() {
            Log.i(TAG, "[SavingTask.run()] begin " + this + ", mMediaRecorderRecording = "
                    + mIsMediaRecorderRecording);
            boolean fail = false;
            if (mIsMediaRecorderRecording) {
                try {
                    stopRecording();
                    mCurrentVideoFilename = mVideoFilename;
                    Log.i(TAG, "[SavingTask.run()] Setting current video filename: "
                            + mCurrentVideoFilename);
                } catch (RuntimeException e) {
                    Log.e(TAG, "[SavingTask.run()] stop fail", e);
                    fail = true;
                    if (mVideoFilename != null) {
                        mVideoModeHelper.deleteVideoFile(mVideoFilename);
                    }
                }
            }
            doAfterStopRecording(fail);
            mIsMediaRecorderRecording = false;
            Log.i(TAG, "[SavingTask.run()] end " + this + ", mCurrentVideoUri = "
                    + mCurrentVideoUri);
        }
    }

    protected void doOnCameraParameterReady(boolean isNeedStartPreview) {
        Log.i(TAG, "[onCameraParameterReady](" + isNeedStartPreview + ")");
        updateParameters();
        if (mIsMediaRecorderRecording ) {
            Log.i(TAG, "mIsMediaRecorderRecording is true so not doOnCameraParameterReady");
            mAdditionManager.onCameraParameterReady(true);
            return;
        }
        mHandler.sendEmptyMessage(INIT_SHUTTER_STATUS);
        mParameters = mICameraDevice.getParameters();
        setFocusParameters();
        mContentResolver = mActivity.getContentResolver();
        keepScreenOnAwhile();
        if (isNeedStartPreview) {
            startPreview();
        } else {
            if (!mIModuleCtrl.isNonePickIntent()) {
                mVideoPreviewSizeRule.updateProfile();
            }
        }
        restoreReviewIfNeed();
        mAdditionManager.onCameraParameterReady(true);
    }

    protected void initVideoRecordingFirst() {
        mTimeLapseValue = mVideoModeHelper.getTimeLapseMs();
        mIsTimeLapseEnable = mTimeLapseValue > 0;
        mIsRecordAudio = mVideoModeHelper.getMicrophone();
        Log.i(TAG, "[initVideoRecordingFirst], = " + ",mTimeLapseValue = " + mTimeLapseValue
                + ",mRecordAudio = " + mIsRecordAudio);
    }

    protected void doStartPreview() {
        Log.i(TAG, "[doStartPreview] mICameraDevice = " + mICameraDevice + "mIFocusManager = "
                + mIFocusManager);
        if (mICameraDevice != null) {
            mICameraDevice.startPreview();
        }
        if (mIFocusManager != null) {
            mIFocusManager.onPreviewStarted();
        }
    }

    protected void stopPreview() {
        Log.i(TAG, "[stopPreview]");
        if (mICameraDevice != null) {
            mICameraDevice.stopPreview();
        }
        mAdditionManager.execute(AdditionActionType.ACTION_ON_STOP_PREVIEW);
    }

    /**
     * For Video Recording
     * 
     * @return true means this action have been execute, false means current
     *         Manager do noting
     */
    protected boolean onVideoShutterButtonClick() {
        Log.i(TAG, "[onVideoShutterButtonClick], mMediaRecorderRecording = "
                + mIsMediaRecorderRecording);
        if (Storage.getLeftSpace() <= 0) {
            mICameraAppUi.restoreViewState();
            backToLastModeIfNeed();
            Log.i(TAG, "[onShutterButtonClick],Storage have no space ");
            return false;
        }
        if (ModeState.STATE_CLOSED == getModeState()) {
            Log.i(TAG, "[onShutterButtonClick],mode state is closed,so return ");
            return false;
        }
        
        if (mIsMediaRecorderRecording) {
            stopVideoRecordingAsync(true);
        } else {
            if (!mIModuleCtrl.isNonePickIntent() && mIFocusManager != null) {
                mIFocusManager.resetTouchFocus();
                mIFocusManager.updateFocusUI();
            }
            mICameraAppUi.setSwipeEnabled(false);
            startVideoRecording();
        }
        return true;
    }

    protected boolean startVideoRecording() {
        Log.i(TAG, "[startVideoRecording()] mIsMediaRecorderRecording = "
                + mIsMediaRecorderRecording);
        if (ModeState.STATE_IDLE != getModeState() || mIsMediaRecorderRecording) {
            Log.i(TAG, "[startVideoRecording()] current state is :" + getModeState());
            return false;
        }
        setModeState(ModeState.STATE_RECORDING);
        updateViewState(true);
        initVideoRecordingFirst();
        initializeRecordingView();
        mCurrentVideoUri = null;
        initializeNormalRecorder();
        pauseAudioPlayback();
        if (!startRecording()) {
            Log.i(TAG, "[startVideoRecording()] mStartRecordingFailed.");
            mICameraAppUi.showToast(R.string.video_recording_error);
            backToLastTheseCase();
            mICameraAppUi.setSwipeEnabled(true);// Need Check whether Need add
                                                // to here
            return false;
        }
        mIsMediaRecoderRecordingPaused = false;
        mRecordingPausedDuration = 0;
        mTotalRecordingDuration = 0;
        mIsRecorderCameraReleased = false;
        mStoppingAction = STOP_NORMAL;
        mIsMediaRecorderRecording = true;
        mRecordingStartOrientation = mIModuleCtrl.getOrientation();
        mRecordingStartTime = SystemClock.uptimeMillis();
        updateRecordingTime();
        if (!mIsTimeLapseEnable) {
            // we just update recording time once,because it will be called in
            // onInfo(xxx).update time to 00:00 or 00:30 .
            mHandler.removeMessages(UPDATE_RECORD_TIME);
        }
        keepScreenOn();
        Log.i(TAG, "[startVideoRecording()] end");
        return true;
    }

    protected void updateViewState(boolean hide) {
        Log.d(TAG, "[updateViewState] hide:" + hide);
        mICameraAppUi.setViewState(hide ? ViewState.VIEW_STATE_RECORDING
                : ViewState.VIEW_STATE_NORMAL);
    }

    protected void initializeRecordingView() {
        long sizeLimit = mVideoModeHelper.getRequestSizeLimit(null, false);
        if (sizeLimit > 0) {
            mRecordingView.setTotalSize(sizeLimit);
            mRecordingView.setCurrentSize(0L);
            mRecordingView.setSizeProgress(0);
            mRecordingView.setRecordingSizeVisible(true);
        }
        mRecordingView.setRecordingIndicator(true);
        mRecordingView.setPauseResumeVisible(true);
        mRecordingView.show();
    }

    protected void initializeNormalRecorder() {
        Log.i(TAG, "[initializeNormalRecorder()]");
        // updateProfile for 3th app after change timelapse
        if (!mIModuleCtrl.isNonePickIntent()) {
            mVideoPreviewSizeRule.updateProfile();
        }
        mProfile = mVideoPreviewSizeRule.getProfile();
        initializeRequestedLimits();
        mMediaRecorder = new MediaRecorder();
        mICameraDevice.unlock();
        mMediaRecorder.setCamera(mICameraDevice.getCamera());
        if (!mIsTimeLapseEnable && mIsRecordAudio) {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        }
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setOutputFormat(mProfile.fileFormat);
        mMediaRecorder.setVideoFrameRate(mProfile.videoFrameRate);
        mMediaRecorder.setVideoEncodingBitRate(mProfile.videoBitRate);
        mMediaRecorder.setVideoSize(mProfile.videoFrameWidth, mProfile.videoFrameHeight);
        mMediaRecorder.setVideoEncoder(mProfile.videoCodec);
        ExtensionHelper.getCameraFeatureExtension(mICameraContext.getActivity())
                .configRecorder(mMediaRecorder);
        if (!mIsTimeLapseEnable && mIsRecordAudio) {
            mMediaRecorder.setAudioEncodingBitRate(mProfile.audioBitRate);
            mMediaRecorder.setAudioChannels(mProfile.audioChannels);
            mMediaRecorder.setAudioSamplingRate(mProfile.audioSampleRate);
            mMediaRecorder.setAudioEncoder(mProfile.audioCodec);
            if (mICameraContext.getFeatureConfig().isHdRecordingEnabled()) {
                MediaRecorderEx.setHDRecordMode(mMediaRecorder, mVideoModeHelper.getRecordMode(
                        mISettingCtrl
                                .getSettingValue(SettingConstants.KEY_VIDEO_HD_AUDIO_RECORDING),
                        mIsRecordAudio), true);
            }
        }
        mMediaRecorder.setMaxDuration(1000 * mRequestDurationLimit);
        if (mIsTimeLapseEnable) {
            mMediaRecorder.setCaptureRate((1000 / (double) mTimeLapseValue));
        }
        Location location = mIModuleCtrl.getLocation();
        if (location != null) {
            mMediaRecorder.setLocation((float) location.getLatitude(),
                    (float) location.getLongitude());
        }
        try {
            mMediaRecorder.setMaxFileSize(mVideoModeHelper.getRecorderMaxSize(mRequestSizeLimit));
        } catch (RuntimeException exception) {
            Log.w(TAG, "initializeNormalRecorder()", exception);
        }
        Log.i(TAG, "[initializeNormalRecorder()], mVideoFileDescriptor = " + mVideoFileDescriptor);
        if (mVideoFileDescriptor != null) {
            mMediaRecorder.setOutputFile(mVideoFileDescriptor.getFileDescriptor());
        } else {
            generateVideoFilename(mProfile.fileFormat, null);
            mMediaRecorder.setOutputFile(mVideoFilename);
        }
        setSlowMotionVideoFileSpeed(mMediaRecorder, SLOW_MOTION_VIDEO_TAG_IN_FILE);
        setMediaRecorderParameters(mMediaRecorder);
        setOrientationHint(Util.getRecordingRotation(mIModuleCtrl.getOrientation(),
                mICameraDeviceManager.getCurrentCameraId(),
                mICameraDeviceManager.getCameraInfo(mICameraDeviceManager.getCurrentCameraId())));

        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "[initializeNormalRecorder()] prepare failed", e);
            releaseMediaRecorder();
            throw new RuntimeException(e);
        }
        mMediaRecorder.setOnErrorListener(this);
        mMediaRecorder.setOnInfoListener(this);
        mMediaRecorder.setOnCameraReleasedListener(this);
        mHandler.removeCallbacks(mReleaseOnInfoListener);
    }

    protected void pauseAudioPlayback() {
        Log.i(TAG, "[pauseAudioPlayback]");
        AudioManager am = (AudioManager) mActivity.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    protected void releaseAudioFocus() {
        Log.i(TAG, "[releaseAudioFocus]");
        AudioManager am = (AudioManager) mActivity.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            am.abandonAudioFocus(null);
        }
    }
    
    protected boolean startRecording() {
        boolean startSuccess = startNormalRecording();
        mICameraAppUi.setVideoShutterMask(true);
        return startSuccess;
    }

    protected boolean startNormalRecording() {
        Log.i(TAG, "[startNormalRecording()]");
        boolean isSuccess = true;
        try {
            mMediaRecorder.start();
            // mediaRecorder will set video-size to parameters ,so after start
            // recording use fetchParametersFromServer to update parameters to
            // make sure Camera ap's parameters is the latest
            mICameraDevice.fetchParametersFromServer();
        } catch (RuntimeException e) {
            Log.e(TAG, "[startNormalRecording()] Could not start media recorder. ", e);
            isSuccess = false;
            releaseMediaRecorder();
            mICameraDevice.lock();
        }
        return isSuccess;
    }

    protected void updateRecordingTime() {
        if (!mIsMediaRecorderRecording) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        mTotalRecordingDuration = now - mRecordingStartTime;
        if (mIsMediaRecoderRecordingPaused) {
            mTotalRecordingDuration = mRecordingPausedDuration;
        }
        long targetNextUpdateDelay = 1000;
        if (!mIsTimeLapseEnable) {
            mRecordingView.showTime(mTotalRecordingDuration, false);
        } else {
            // The length of time lapse video is different from the length
            // of the actual wall clock time elapsed. Display the video length
            // only in format hh:mm:ss.dd, where dd are the centi seconds.
            mRecordingView.showTime(
                    mVideoModeHelper.getTimeLapseLength(mTotalRecordingDuration, mProfile), true);
            targetNextUpdateDelay = mTimeLapseValue;
        }
        mCurrentShowIndicator = 1 - mCurrentShowIndicator;
        if (mIsMediaRecoderRecordingPaused && 1 == mCurrentShowIndicator) {
            mRecordingView.setTimeVisible(false);
        } else {
            mRecordingView.setTimeVisible(true);
        }
        long actualNextUpdateDelay = 500;
        if (!mIsMediaRecoderRecordingPaused) {
            actualNextUpdateDelay = targetNextUpdateDelay
                    - (mTotalRecordingDuration % targetNextUpdateDelay);
        }
        Log.d(TAG, "[updateRecordingTime()],actualNextUpdateDelay = " + actualNextUpdateDelay);
        mHandler.sendEmptyMessageDelayed(UPDATE_RECORD_TIME, actualNextUpdateDelay);
    }

    protected void stopVideoRecordingAsync(boolean needShowSavingUi) {
        Log.i(TAG, "[stopVideoRecordingAsync()] mMediaRecorderRecording = "
                + mIsMediaRecorderRecording + ",needShowSavingUi = " + needShowSavingUi);
        if (ModeState.STATE_RECORDING != getModeState() || !mIsMediaRecorderRecording) {
            Log.i(TAG, "[stopVideoRecordingAsync] current state is " + getModeState());
            return;
        }
        releaseAudioFocus();
        setModeState(ModeState.STATE_SAVING);
        mICameraAppUi.setSwipeEnabled(true);
        mICameraAppUi.setVideoShutterMask(false);
        mHandler.removeMessages(UPDATE_RECORD_TIME);
        mRecordingView.hide();
        mICameraAppUi.setVideoShutterEnabled(false);
        if (needShowSavingUi) {
            mHandler.sendEmptyMessage(SHOW_SAVING_DIALOG);
        }
        mVideoSavingTask = new SavingTask();
        mVideoSavingTask.start();
        Log.i(TAG, "[stopVideoRecordingAsync()] end of stopVideoRecordingAsync");
    }

    protected void stopVideoOnPause() {
        Log.d(TAG, "[stopVideoOnPause()] mMediaRecorderRecording =  " + mIsMediaRecorderRecording);
        boolean videoSaving = false;
        if (mIsMediaRecorderRecording) {
            if (!mIModuleCtrl.isNonePickIntent()) {
                mStoppingAction = STOP_SHOW_ALERT;
            }
            stopVideoRecordingAsync(false);
            videoSaving = isVideoProcessing();
        } else {
            releaseMediaRecorder();
            boolean hasSaveRunnable = false;
            if (mVideoSavingTask != null) {
                synchronized (mVideoSavingTask) {
                    hasSaveRunnable = mHandler
                            .hasCallbacks(mVideoSavedRunnable);
                    Log.d(TAG,
                            "[stopVideoOnPause()] has mVideoSavedRunnable = "
                                    + hasSaveRunnable);
                }
            }
            if (mIModuleCtrl.isNonePickIntent() && !isVideoProcessing()
                    && !hasSaveRunnable) {
                backToLastTheseCase();
            }
        }
        if (videoSaving) {
            waitForRecorder();
        } else if (ModeState.STATE_IDLE == getModeState()) {
            closeVideoFileDescriptor();
        }
        // keep sequence, stop preview--> close camera
        stopPreview();
        Log.i(TAG, "[stopVideoOnPause()] " + " videoSaving = " + videoSaving
                + ", mVideoSavingTask = " + mVideoSavingTask + ", mMediaRecorderRecording = "
                + mIsMediaRecorderRecording);
    }

    protected void stopRecording() {
        Log.i(TAG, "[stopRecording] begin");
        mMediaRecorder.stop();
        mMediaRecorder.setOnCameraReleasedListener(null);
        Log.i(TAG, "[stopRecording] end");
    }

    protected void doAfterStopRecording(boolean fail) {
        Log.i(TAG, "[doAfterStopRecording],fail = " + fail);
        if (!mIModuleCtrl.isNonePickIntent()) {
            if (!fail && STOP_RETURN_UNVALID != mStoppingAction) {
                if (mIModuleCtrl.isQuickCapture()) {
                    mStoppingAction = STOP_RETURN;
                } else {
                    mStoppingAction = STOP_SHOW_ALERT;
                }
            }
        } else if (fail) {
            mStoppingAction = STOP_FAIL;
        }
        releaseMediaRecorder();
        if (!fail) {
            addVideoToMediaStore();
        }
        synchronized (mVideoSavingTask) {
            mVideoSavingTask.notifyAll();
            mHandler.removeCallbacks(mVideoSavedRunnable);
            mHandler.post(mVideoSavedRunnable);
        }
    }

    protected void releaseMediaRecorder() {
        Log.i(TAG, "[releaseMediaRecorder()] mMediaRecorder = " + mMediaRecorder
                + " mRecorderCameraReleased = " + mIsRecorderCameraReleased);
        if (mMediaRecorder != null && !mIsRecorderCameraReleased) {
            cleanupEmptyFile();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mIsRecorderCameraReleased = true;
            mHandler.post(mReleaseOnInfoListener);
        }
        mVideoFilename = null;
    }

    protected final Runnable mReleaseOnInfoListener = new Runnable() {
        @Override
        public void run() {
            if (mMediaRecorder != null) {
                mMediaRecorder.setOnInfoListener(null);
                mMediaRecorder.setOnErrorListener(null);
                mMediaRecorder = null;
            }
        }
    };

    protected void addVideoToMediaStore() {
        if (mVideoFileDescriptor == null) {
            FILE_TYPE fileType = FILE_TYPE.VIDEO;
            int videoTag = NORMAL_VIDEO_TAG_IN_DB;

            if ("on".equals(mISettingCtrl.getSettingValue(SettingConstants.KEY_SLOW_MOTION))) {
                videoTag = SLOW_MOTION_VIDEO_TAG_IN_DB;
                fileType = FILE_TYPE.SLOWMOTION;
            }

            mIFileSaver.init(
                    fileType,
                    mProfile.fileFormat,
                    Integer.toString(mProfile.videoFrameWidth) + "x"
                            + Integer.toString(mProfile.videoFrameHeight), Util
                            .getRecordingRotation(mRecordingStartOrientation,
                                    mICameraDeviceManager.getCurrentCameraId(),
                                    mICameraDeviceManager.getCameraInfo(mICameraDeviceManager
                                            .getCurrentCameraId())));
            mIFileSaver.saveVideoFile(mIModuleCtrl.getLocation(), mVideoTempPath,
                    computeDuration(), videoTag, mFileSavedListener);
            mCurrentVideoFilename = mIFileSaver.getVideoSaveRequest().getFilePath();
            Log.i(TAG, "[addVideoToMediaStore] mCurrentVideoFilename =  " + mCurrentVideoFilename);
        }
    }

    protected Runnable mVideoSavedRunnable = new Runnable() {
        public void run() {
            Log.i(TAG, "[mVideoSavedRunnable.run()] begin " + ", mStoppingAction = "
                    + mStoppingAction);
            updateViewState(false);
            mICameraAppUi.dismissProgress();
            mICameraAppUi.setVideoShutterEnabled(true);
            int action = (ModeState.STATE_CLOSED == getModeState()
                    && mStoppingAction != STOP_NORMAL && mStoppingAction != STOP_FAIL) ? STOP_SHOW_ALERT
                    : mStoppingAction;
            switch (action) {

            case STOP_SHOW_ALERT:
                showAlert();
                break;

            case STOP_RETURN_UNVALID:
                mVideoModeHelper.doReturnToCaller(false, mCurrentVideoUri);
                break;

            case STOP_RETURN:
                mVideoModeHelper.doReturnToCaller(true, mCurrentVideoUri);
                break;

            default:
                break;
            }
            if (ModeState.STATE_CLOSED == getModeState()) {
                closeVideoFileDescriptor();
            }
            if (ModeState.STATE_CLOSED != getModeState()
                    && ((mFocusState == START_FOCUSING) || (mFocusState == FOCUSING))) {
                changeFocusState();
            }
            setModeState(ModeState.STATE_IDLE);
            backToLastModeIfNeed();
            Log.i(TAG, "[mVideoSavedRunnable.run()] end ");
        };
    };

    protected void backToLastModeIfNeed() {
        Log.i(TAG, "[backToLastModeIfNeed()]");
        if (mIModuleCtrl.isVideoCaptureIntent()) {
            if (!mICameraAppUi.getCameraView(CommonUiType.REVIEW).isShowing()) {
                mICameraAppUi.switchShutterType(ShutterButtonType.SHUTTER_TYPE_VIDEO);
            }
        } else {
            mIModuleCtrl.backToLastMode();
            mICameraAppUi.setPhotoShutterEnabled(true);
        }
    }

    /**
     * For take picture [Snap Shot]
     * 
     * @return true means this action have been execute, false means current
     *         Manager do noting
     */
    protected boolean takeASnapshot() {
        Log.i(TAG, "[takeASnapshot]");
        if (ModeState.STATE_RECORDING != getModeState()) {
            Log.i(TAG, "[takeASnapshot] Video snapshot fail state = " + getModeState());
            return false;
        }
        mICameraAppUi.updateSnapShotUIView(true);
        mICameraAppUi.setPhotoShutterEnabled(false);
        mICameraDevice.takePicture(null, null, null, new JpegPictureCallback());
        return true;
    }

    public final class JpegPictureCallback implements PictureCallback {
        @Override
        public void onPictureTaken(byte[] jpegData, android.hardware.Camera camera) {
            Log.i(TAG, "[onPictureTaken]");
            if (jpegData == null) {
                mHandler.sendEmptyMessage(UPDATE_SNAP_UI);
                Log.i(TAG, "[onPictureTaken],data is null,return");
                return;
            }

            mIFileSaver.init(FILE_TYPE.JPEG, 0, null, -1);
            long time = System.currentTimeMillis();
            String title = Util.createNameFormat(time,
                    mActivity.getString(R.string.image_file_name_format))
                    + ".jpg";
            mIFileSaver.savePhotoFile(jpegData, null, time, mIModuleCtrl.getLocation(), 0,
                    null);
            mHandler.sendEmptyMessage(UPDATE_SNAP_UI);
        }
    }

    /**
     * decide shutter button's status(disable or enable)
     */
    protected void initializeShutterStatus() {
        if (mIsModeReleased) {
            Log.i(TAG, "[initializeShutterStatus] mode is closed,so return");
            return;
        }
        if (CameraInfo.CAMERA_FACING_FRONT == mICameraDeviceManager
                .getCameraInfo(mICameraDeviceManager.getCurrentCameraId()).facing
                || "on".equals(mISettingCtrl.getSettingValue(SettingConstants.KEY_SLOW_MOTION))) {
            mICameraAppUi.setPhotoShutterEnabled(false);
        } else {
            mICameraAppUi.setPhotoShutterEnabled(isVssSupported());
        }
    }

    protected void pauseVideoRecording() {
        Log.d(TAG, "[pauseVideoRecording()] mMediaRecorderRecording = " + mIsMediaRecorderRecording
                + " mMediaRecoderRecordingPaused = " + mIsMediaRecoderRecordingPaused);
        mRecordingView.setRecordingIndicator(false);
        if (mIsMediaRecorderRecording && !mIsMediaRecoderRecordingPaused) {
            try {
                MediaRecorderEx.pause(mMediaRecorder);
            } catch (IllegalStateException e) {
                Log.e(TAG, "[pauseVideoRecording()] Could not pause media recorder. ");
            }
            mRecordingPausedDuration = SystemClock.uptimeMillis() - mRecordingStartTime;
            mIsMediaRecoderRecordingPaused = true;
        }
    }

    protected boolean onUserInteraction() {
        Log.i(TAG, "[onUserInteraction] mMediaRecorderRecording = " + mIsMediaRecorderRecording);
        return mIsMediaRecorderRecording;
    }

    protected void onSingleTapUp(View view, int x, int y) {
        Log.i(TAG, "[onSingleTapUp] view = " + view + ",x = " + x + ",y = " + y);
        if (ModeState.STATE_UNKNOWN == getModeState() || ViewState.VIEW_STATE_PRE_RECORDING== mICameraAppUi.getViewState()) {
            return;
        }
        String focusMode = null;
        if (mIFocusManager != null) {
            focusMode = mIFocusManager.getFocusMode();
        }
        if (focusMode == null || (Parameters.FOCUS_MODE_INFINITY.equals(focusMode))
                || (Parameters.FOCUS_MODE_CONTINUOUS_PICTURE.equals(focusMode))) {
            Log.i(TAG, "[onSingleTapUp] focus mode is error ,so return");
            return;
        }
        if (ModeState.STATE_CLOSED == getModeState() || mICameraDevice == null) {
            Log.i(TAG, "[onSingleTapUp] mode state is closed or cameraDevice is null,so return");
            return;
        }
        if (!mIFocusManager.getFocusAreaSupported()) {
            Log.i(TAG, "[onSingleTapUp] focusArea is not supported");
            return;
        }
        Log.i(TAG, "[onSingleTapUp](" + x + ", " + y + ")" + ",focusMode = " + focusMode
                + ",mMediaRecorderRecording = " + mIsMediaRecorderRecording);
        if (mIsMediaRecorderRecording) {
            setFocusState(START_FOCUSING);
        }
        mIFocusManager.onSingleTapUp(x, y);

    }

    protected void onMediaEject() {
        stopVideoRecordingAsync(true);
    }

    protected boolean onBackPressed() {
        Log.d(TAG, "[onBackPressed()] CurrentModeState " + getModeState());
        if (ModeState.STATE_IDLE == getModeState()) {
            return false;
        }
        if (ModeState.STATE_RECORDING == getModeState()) {
            stopVideoRecordingAsync(true);
        }
        return true;
    }

    protected boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.i(TAG, "keyCode = " + keyCode + " event = " + event);
        if (ModeState.STATE_CLOSED == getModeState()) {
            return true;
        }
        switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_CENTER:
        case KeyEvent.KEYCODE_CAMERA:
            if (event.getRepeatCount() == 0) {
                if (!mICameraAppUi.getCameraView(CommonUiType.REVIEW).isShowing()) {
                    onVideoShutterButtonClick();
                }
                return true;
            }
            break;

        case KeyEvent.KEYCODE_MENU:
            if (mIsMediaRecorderRecording) {
                return true;
            }
            break;

        default:
            break;
        }
        return false;
    }

    protected String generateVideoFilename(int outputFileFormat, String suffix) {
        if (!Storage.isStorageReady()) {
            Storage.initializeStorageState();
        }
        String mDisplayName = "videorecorder";
        String filename = mDisplayName
                + mVideoModeHelper.convertOutputFormatToFileExt(outputFileFormat);
        if (suffix == null) {
            mVideoTempPath = Storage.getFileDirectory() + '/' + filename + ".tmp";
        } else {
            mVideoTempPath = Storage.getFileDirectory() + '/' + filename + "_" + suffix + ".tmp";
        }
        mVideoFilename = mVideoTempPath;
        Log.i(TAG, "[generateVideoFilename] mVideoFilename = " + mVideoFilename);
        return mVideoFilename;
    }

    protected void cleanupEmptyFile() {
        if (mVideoFilename != null) {
            File f = new File(mVideoFilename);
            if (f.length() == 0 && f.delete()) {
                Log.d(TAG, "[cleanupEmptyFile] Empty video file deleted: " + mVideoFilename);
                mVideoFilename = null;
            }
        }
    }

    protected void onRestoreSettings() {
        if (!mIModuleCtrl.isNonePickIntent()) {
            updateViewState(false);
        } else {
            mICameraAppUi.setPhotoShutterEnabled(true);
        }
    }

    protected void onCameraClose() {
        Log.i(TAG, "[onCameraClose()]");
        mHandler.removeMessages(UPDATE_RECORD_TIME);
        mHandler.removeMessages(UPDATE_SNAP_UI);
        // avoid performance is bad,the UPDATE_SNAP_UI msg had not been
        // handleMessage,
        // it was removed,the red frame will show always.

        if (!mIModuleCtrl.isVideoCaptureIntent()) {
            mICameraAppUi.updateSnapShotUIView(false);
        }
        mIsAutoFocusCallback = false;

        if (mICameraDevice != null) {
            mICameraDevice.cancelAutoFocus();
        }
        if (mIFocusManager != null) {
            mIFocusManager.onPreviewStopped();
        }
        stopVideoOnPause();
        if (mICameraDevice == null) {
            return;
        }
        resetScreenOn();
    }

    protected void setOrientationHint(int orientation) {
        if (mMediaRecorder != null) {
            mMediaRecorder.setOrientationHint(orientation);
        }
    }

    protected boolean isVideoProcessing() {
        boolean isVideoProcess = mVideoSavingTask != null && mVideoSavingTask.isAlive();
        Log.i(TAG, "[isVideoProcessing] : " + isVideoProcess);
        return isVideoProcess;
    }

    protected void waitForRecorder() {
        synchronized (mVideoSavingTask) {
            while (!mIsRecorderCameraReleased) {
                try {
                    Log.i(TAG, "[waitForRecorder] wait for releasing camera done in MediaRecorder");
                    mVideoSavingTask.wait();
                } catch (InterruptedException e) {
                    Log.w(TAG, "[waitForRecorder] Got notify from Media recorder()", e);
                }
            }
        }
    }

    protected long computeDuration() {
        long duration = mVideoModeHelper.getDuration(mCurrentVideoFilename);
        Log.i(TAG, "[computeDuration()] return " + duration);
        return duration;
    }

    /**
     * if video is started by MMS,when after recording,user want recording again
     * ,so we need delete current video;
     */
    protected void deleteCurrentVideo() {
        Log.d(TAG, "[deleteCurrentVideo()] mCurrentVideoFilename = " + mCurrentVideoFilename
                + " mSaveTempVideo = " + mSaveTempVideo);
        if (mCurrentVideoFilename != null) {
            if (mSaveTempVideo > 0) {
                mVideoModeHelper.renameVideoFile(mCurrentVideoFilename);
            } else {
                mVideoModeHelper.deleteVideoFile(mCurrentVideoFilename);
            }
            mCurrentVideoFilename = null;
            if (mCurrentVideoUri != null) {
                mContentResolver.delete(mCurrentVideoUri, null, null);
                mCurrentVideoUri = null;
            }
        }
    }

    /**
     * when file have saved ,need notify Manager
     */
    protected OnFileSavedListener mFileSavedListener = new OnFileSavedListener() {

        @Override
        public void onFileSaved(Uri uri) {
            Log.d(TAG, "[onFileSaved] uri = " + uri);
            mCurrentVideoUri = uri;
            mHandler.removeMessages(SHOW_SAVING_DIALOG);
            setModeState(ModeState.STATE_IDLE);
        }
    };

    protected void setSlowMotionVideoFileSpeed(MediaRecorder recorder, int value) {
        if ("on".equals(mISettingCtrl.getSettingValue(SettingConstants.KEY_SLOW_MOTION))) {
            recorder.setParametersExtra("media-param-slowmotion=" + value);
        }
    }

    protected void updateParameters() {
        super.updateDevice();
        super.updateFocusManager();
        if (mIFocusManager != null) {
            mIFocusManager.setListener(this);
        }
    }

    protected void updateTimeLapseStatus(boolean enable) {
        mIsTimeLapseEnable = enable;
    }

    protected boolean getTimeLapseStauts() {
        return mIsTimeLapseEnable;
    }

    private void startPreview() {
        Log.i(TAG, "[startPreview]...");
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                // when video is closed, the mIFocusManager will be null, but
                // then to run the thread in UI,then JE
                if (mIFocusManager != null) {
                    mIFocusManager.resetTouchFocus();
                }
            }
        });
        stopPreview();
        doStartPreview();
        mAdditionManager.execute(AdditionActionType.ACTION_ON_START_PREVIEW);
    }

    private void showAlert() {
        Log.d(TAG, "[showAlert()]");
        if (Storage.isStorageReady()) {
            FileDescriptor mFileDescriptor = null;
            if (mVideoFileDescriptor != null) {
                mFileDescriptor = mVideoFileDescriptor.getFileDescriptor();
                mICameraAppUi.showReview(null, mFileDescriptor);
            } else if (mCurrentVideoFilename != null) {
                mICameraAppUi.showReview(mCurrentVideoFilename, null);
            }
            mICameraAppUi.setReviewListener(mRetakeListener, mReviewPlayListener);
            mICameraAppUi.switchShutterType(ShutterButtonType.SHUTTER_TYPE_OK_CANCEL);
            if (ModeState.STATE_CLOSED == getModeState()) {
                closeVideoFileDescriptor();
            }
        }
    }
    private class MainHandler extends Handler {
        public MainHandler(Looper mainLooper) {
            super(mainLooper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "[MainHandler.handleMessage](" + msg + ")");
            switch (msg.what) {
            case UPDATE_RECORD_TIME:
                updateRecordingTime();
                break;

            case UPDATE_SNAP_UI:
                mVideoModeHelper.broadcastNewPicture(mActivity, null);
                if (!mIModuleCtrl.isVideoCaptureIntent()) {
                    mICameraAppUi.updateSnapShotUIView(false);
                }
                mICameraAppUi.setPhotoShutterEnabled(true);
                break;

            case INIT_SHUTTER_STATUS:
                initializeShutterStatus();
                break;

            case MSG_CLEAR_SCREEN_DELAY:
                resetScreenOn();
                break;

            case SHOW_SAVING_DIALOG:
                mICameraAppUi.showProgress(mActivity.getResources().getString(R.string.saving));
                break;
            default:
                break;
            }
        }
    }

    private void resetScreenOn() {
        Log.d(TAG, "[resetScreenOn()]");
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mHandler.removeMessages(MSG_CLEAR_SCREEN_DELAY);
                mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });
    }

    private void keepScreenOnAwhile() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "[keepScreenOnAwhile()]");
                mHandler.removeMessages(MSG_CLEAR_SCREEN_DELAY);
                mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                mHandler.sendEmptyMessageDelayed(MSG_CLEAR_SCREEN_DELAY, DELAY_MSG_SCREEN_SWITCH);
            }
        });
    }

    private void keepScreenOn() {
        Log.d(TAG, "[keepScreenOn()]");
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mActivity.getWindow() != null) {
                    mHandler.removeMessages(MSG_CLEAR_SCREEN_DELAY);
                    mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }
        });

    }

    private OnClickListener mVideoPauseResumeListner = new OnClickListener() {
        public void onClick(View v) {
            Log.i(TAG, "[mVideoPauseResumeListner.onClick()] mMediaRecoderRecordingPaused = "
                    + mIsMediaRecoderRecordingPaused);
            if (ModeState.STATE_RECORDING != getModeState()) {
                return;
            }
            if (mIsMediaRecoderRecordingPaused) {
                mRecordingView.setRecordingIndicator(true);
                try {
                    mMediaRecorder.start();
                    mRecordingStartTime = SystemClock.uptimeMillis() - mRecordingPausedDuration;
                    mRecordingPausedDuration = 0;
                    mIsMediaRecoderRecordingPaused = false;
                } catch (IllegalStateException e) {
                    Log.e(TAG, "[mVideoPauseResumeListner] Could not start media recorder. ", e);
                    mICameraAppUi.showToast(R.string.toast_video_recording_not_available);
                    releaseMediaRecorder();
                }
            } else {
                pauseVideoRecording();
            }
            Log.i(TAG, "[mVideoPauseResumeListner.onClick()] end");
        }
    };

    private void initializeRequestedLimits() {
        closeVideoFileDescriptor();
        initializeLimiteds();
    }

    private void closeVideoFileDescriptor() {
        mVideoModeHelper.closeVideoFileDescriptor(mVideoFileDescriptor);
        mVideoFileDescriptor = null;
    }

    private void restoreReviewIfNeed() {
        if (mICameraAppUi.getCameraView(CommonUiType.REVIEW).isShowing()) {
            if (!mIModuleCtrl.isNonePickIntent() && mVideoFileDescriptor == null) {
                Intent intent = mIModuleCtrl.getIntent();
                Uri saveUri = intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
                if (saveUri != null) {
                    try {
                        if (saveUri.toString().startsWith(CONTENT_URI_PREFIX)) {
                            mVideoFileDescriptor = mContentResolver
                                    .openFileDescriptor(saveUri, "r");
                        } else {
                            mVideoFileDescriptor = mContentResolver.openFileDescriptor(saveUri,
                                    "rw");
                        }
                    } catch (java.io.FileNotFoundException ex) {
                        Log.e(TAG, "initializeNormalRecorder()", ex);
                    }
                }
            }
        }
        Log.d(TAG, "[restoreReviewIfNeed()]  mVideoFileDescriptor = " + mVideoFileDescriptor
                + ", mCurrentVideoFilename = " + mCurrentVideoFilename);
    }

    private void backToLastTheseCase() {
        mRecordingView.hide();
        mICameraAppUi.restoreViewState();
        backToLastModeIfNeed();
        return;
    }

    private void initializeLimiteds() {
        mRequestSizeLimit = mVideoModeHelper.getRequestSizeLimit(mProfile, true);
        mRequestDurationLimit = mVideoModeHelper.getRequestDurationLimited();
        if (mIModuleCtrl.isVideoCaptureIntent()) {
            Intent mIntent = mIModuleCtrl.getIntent();
            Uri saveUri = mIntent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
            if (saveUri != null) {
                try {
                    mVideoFileDescriptor = mContentResolver.openFileDescriptor(saveUri, "rw");
                    mCurrentVideoUri = saveUri;
                } catch (java.io.FileNotFoundException ex) {
                    Log.e(TAG, ex.toString());
                }
            }
        }
    }

    private void setVideoRule() {
        VideoFaceBeautyRule slowMotionRule = new VideoFaceBeautyRule(mICameraContext);
        mISettingCtrl.addRule(SettingConstants.KEY_VIDEO,
                SettingConstants.KEY_SLOW_MOTION, slowMotionRule);
        slowMotionRule.addLimitation("on", null, null);
        
        VfbQualityRule vfbQualityRule = new VfbQualityRule(
                mICameraContext, SettingConstants.KEY_FACE_BEAUTY);
        mISettingCtrl.addRule(SettingConstants.KEY_VIDEO, SettingConstants.KEY_VIDEO_QUALITY,
                vfbQualityRule);
        vfbQualityRule.addLimitation("on", null, null);
        
        mVideoPreviewSizeRule = new VideoPreviewRule(mICameraContext, getCameraModeType());
        mISettingCtrl.addRule(mVideoPreviewSizeRule.getConditionKey(),
                SettingConstants.KEY_PICTURE_RATIO, mVideoPreviewSizeRule);
        mVideoPreviewSizeRule.addLimitation("on", null, null);
        
        mVideoHdrRul = new VideoHdrRule(mICameraContext);
        mISettingCtrl.addRule(SettingConstants.KEY_VIDEO, SettingConstants.KEY_HDR, mVideoHdrRul);
        mVideoHdrRul.addLimitation("on", null, null);
    }

    private final class AutoFocusCallback implements android.hardware.Camera.AutoFocusCallback {
        public void onAutoFocus(boolean focused, android.hardware.Camera camera) {
            Log.i(TAG, "mAutoFocusTime = " + (System.currentTimeMillis() - mFocusStartTime) + "ms"
                    + ",mFocusManager.onAutoFocus( " + focused + ")");
            if (ModeState.STATE_CLOSED == getModeState()) {
                return;
            }
            setFocusState(FOCUSED);
            mIFocusManager.onAutoFocus(focused);
            mIsAutoFocusCallback = true;
        }
    }

    private final class AutoFocusMoveCallback implements
            android.hardware.Camera.AutoFocusMoveCallback {
        @Override
        public void onAutoFocusMoving(boolean moving, android.hardware.Camera camera) {
            Log.i(TAG, "[onAutoFocusMoving] moving = " + moving);
            mIFocusManager.onAutoFocusMoving(moving);
        }
    }

    private void changeFocusState() {
        Log.i(TAG, "[changeFocusState()]");
        if (mICameraDevice != null) {
            mICameraDevice.cancelAutoFocus();
        }
        mIsAutoFocusCallback = false;
        if (mIFocusManager != null) {
            mIFocusManager.resetTouchFocus();
            mIFocusManager.updateFocusUI();
        }
        setFocusParameters();
    }

    private void setFocusState(int state) {
        Log.i(TAG, "[setFocusState](" + state + ") mMediaRecorderRecording = "
                + mIsMediaRecorderRecording);
        mFocusState = state;
        if (mIsMediaRecorderRecording || ModeState.STATE_CLOSED == getModeState()) {
            return;
        }
        switch (state) {
        case FOCUSING:
            mICameraAppUi.setViewState(ViewState.VIEW_STATE_FOCUSING);
            break;

        case FOCUS_IDLE:
        case FOCUSED:
            if ((ViewState.VIEW_STATE_REVIEW != mICameraAppUi.getViewState())) {
                updateViewState(false);
            } 
            break;

        default:
            break;
        }
    }

    private OnClickListener mReviewPlayListener = new OnClickListener() {
        public void onClick(View v) {
            Log.i(TAG, "[mReviewPlayListener],onClick");
            mVideoModeHelper.startPlayVideoActivity(mCurrentVideoUri, mProfile);
        }
    };

    private OnClickListener mRetakeListener = new OnClickListener() {
        public void onClick(View v) {
            Log.i(TAG, "[mRetakeListener],onClick");
            deleteCurrentVideo();
            mICameraAppUi.hideReview();
            mICameraAppUi.setVideoShutterEnabled(true);
            mICameraAppUi.switchShutterType(ShutterButtonType.SHUTTER_TYPE_VIDEO);
        }
    };
    
    private boolean isVssSupported() {
        boolean isSupported = true;
        ListPreference fastAf = mISettingCtrl.getListPreference(SettingConstants.KEY_FAST_AF);
        ListPreference distance = mISettingCtrl.getListPreference(SettingConstants.KEY_DISTANCE);
        isSupported = mParameters.isVideoSnapshotSupported() 
            && (fastAf == null || (fastAf != null
            && "off".equals(fastAf.getValue()))) 
            && (distance == null || (distance != null
            && "off".equals(distance.getValue())));
        Log.i(TAG, "[isVssSupported], isSupported = " + isSupported);
        return isSupported;
    }

    private void setMediaRecorderParameters(MediaRecorder mediaRecorder) {
        try {
            mediaRecorder.setParametersExtra(RECORDER_INFO_SUFFIX
                    + MEDIA_RECORDER_INFO_BITRATE_ADJUSTED);
            mediaRecorder.setParametersExtra(RECORDER_INFO_SUFFIX
                    + MEDIA_RECORDER_INFO_FPS_ADJUSTED);
            mediaRecorder.setParametersExtra(RECORDER_INFO_SUFFIX
                    + MEDIA_RECORDER_INFO_START_TIMER);
            mediaRecorder.setParametersExtra(RECORDER_INFO_SUFFIX
                    + MEDIA_RECORDER_INFO_WRITE_SLOW);
            mediaRecorder.setParametersExtra(RECORDER_INFO_SUFFIX
                    + MEDIA_RECORDER_INFO_CAMERA_RELEASE);
            if (mIModuleCtrl.isVideoCaptureIntent()) {
                mediaRecorder.setParametersExtra(RECORDER_INFO_SUFFIX
                        + MEDIA_RECORDER_INFO_RECORDING_SIZE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
