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

package com.mediatek.camera.mode.stereocamera;

import com.android.camera.R;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.PictureCallback;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;

import com.mediatek.camera.ICameraContext;
import com.mediatek.camera.ISettingCtrl;
import com.mediatek.camera.ISettingRule;
import com.mediatek.camera.mode.PhotoMode;
import com.mediatek.camera.platform.ICameraAppUi.ViewState;
import com.mediatek.camera.platform.ICameraDeviceManager.ICameraDevice;
import com.mediatek.camera.platform.ICameraDeviceManager.ICameraDevice.StereoJpsCallback;
import com.mediatek.camera.platform.ICameraDeviceManager.ICameraDevice.StereoMaskCallback;
import com.mediatek.camera.platform.ICameraDeviceManager.ICameraDevice.StereoWarningCallback;
import com.mediatek.camera.platform.ICameraView;
import com.mediatek.camera.platform.IFileSaver.FILE_TYPE;
import com.mediatek.camera.platform.IFileSaver.OnFileSavedListener;
import com.mediatek.camera.platform.Parameters;
import com.mediatek.camera.setting.SettingConstants;
import com.mediatek.camera.setting.SettingItem;
import com.mediatek.camera.setting.SettingItem.Record;
import com.mediatek.camera.setting.SettingUtils;
import com.mediatek.camera.setting.preference.ListPreference;
import com.mediatek.camera.util.Log;
import com.mediatek.camera.util.xmp.XmpOperator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StereoCameraMode extends PhotoMode {
    private static final String TAG = "StereoCameraMode";
    // Stereo 3d warning message
    private static final int DUAL_CAMERA_LENS_COVERED = 0;
    private static final int DUAL_CAMERA_LOW_LIGHT = 1;
    private static final int DUAL_CAMERA_TOO_CLOSE = 2;
    private static final int DUAL_CAMERA_READY = 3;

    private static final int GEO_INFO = 48;
    private static final int PHO_INFO = 52;
    private static final int PASS_NUM = 0;
    private static final int WARN_NUM = 1;
    private static final int FAIL_NUM = 2;

    private static final int MSG_SAVE_FILE = 10000;
    private static final int MSG_SET_PROGRESS = 10001;
    private static final int MSG_HIDE_VIEW = 10003;
    private static final int MSG_FILE_SAVED = 10004;

    private static final int JPS_CONFIG_SIZE = 116;
    private static final String REFOCUS_TAG = "refocus";
    private static final String SUBFFIX_TAG = ".jpg";
    private static final String ZSD_MODE_ON = "on";
    private static final String GEO_QUALITY = "Geometric Quality: ";
    private static final String PHO_QUALITY = "Photo Quality: ";
    private static final String PASS = "Pass";
    private static final String WARN = "Pass(warnning)";
    private static final String FAIL = "Fail";
    private static final int TAG_REFOCUS_IMAGE = 1;
    private static final int TAG_NORAML_IMAGE = 0;
    private boolean mIsReady = true;
    private boolean mNeedSaveOrignal;
    private Stereo3dCallback mStereoCameraJpsCallback = new Stereo3dCallback();
    private MaskCallback mStereoCameraMaskCallback = new MaskCallback();
    private WarningCallback mStereoCameraWarningCallback = new WarningCallback();
    private int mCallbackNum;
    private byte[] mJpsData;
    private byte[] mMaskData;
    private byte[] mConfigData;
    private byte[] mMaskAndConfigData;
    private byte[] mOriginalJpegData;
    private byte[] mXmpJpegData;

    private Handler mHandler;
    private ICameraView mICameraView;

    public StereoCameraMode(ICameraContext cameraContext) {
        super(cameraContext);
        Log.i(TAG, "[StereoCameraMode]constructor...");
        mHandler = new Stereo3dHandler(mActivity.getMainLooper());
        mCameraCategory = new Stereo3dCameraCategory();
        setRefocusSettingRules(cameraContext);

    }

    @Override
    public boolean close() {
        Log.i(TAG, "[closeMode]...");
        super.close();
        return true;
    }

    @Override
    public boolean execute(ActionType type, Object... arg) {
        if (type != ActionType.ACTION_ORITATION_CHANGED) {
            Log.i(TAG, "[execute]type = " + type);
        }
        switch (type) {
        case ACTION_SHUTTER_BUTTON_LONG_PRESS:
            mICameraAppUi
                    .showInfo(mActivity
                            .getString(R.string.accessibility_switch_to_dual_camera)
                            + mActivity
                                    .getString(R.string.camera_continuous_not_supported));
            break;

        case ACTION_ON_CAMERA_OPEN:
            updateDevice();
            mCameraClosed = false;
            mICameraDevice.setStereoWarningCallback(mStereoCameraWarningCallback);
            break;

        case ACTION_FACE_DETECTED:
            // Do-Noting,Because not need show super's entry FB icon
            break;

        default:
            return super.execute(type, arg);
        }

        return true;
    }
    @Override
    public void resume() {

    }
    @Override
    public void pause() {

    }

    private class Stereo3dHandler extends Handler {
        public Stereo3dHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "[handleMessage]msg.what= " + msg.what);
            switch (msg.what) {
            case MSG_SAVE_FILE:
                break;

            case MSG_FILE_SAVED:
                break;

            case MSG_SET_PROGRESS:
                break;

            case MSG_HIDE_VIEW:
                mICameraView.hide();
                break;

            default:
                break;
            }
        }
    }

    private OnFileSavedListener mFileSaverListener = new OnFileSavedListener() {
        @Override
        public void onFileSaved(Uri uri) {
            Log.d(TAG, "[onFileSaved]uri= " + uri);
            mHandler.sendEmptyMessage(MSG_FILE_SAVED);
        }
    };

    private void saveFile(byte[] data, int refocus) {
        Log.i(TAG, "[saveFile]...");
        Location location = mIModuleCtrl.getLocation();
        mIFileSaver.savePhotoFile(data, null, mCaptureStartTime, location, refocus,
                mFileSaverListener);
    }

    private void setRefocusSettingRules(ICameraContext cameraContext) {
        Log.i(TAG, "[setRefocusSettingRules]...");
        StereoPreviewSizeRule previewSizeRule = new StereoPreviewSizeRule(cameraContext);
        previewSizeRule.addLimitation("on", null, null);
        mISettingCtrl.addRule(SettingConstants.KEY_REFOCUS,
                SettingConstants.KEY_PICTURE_RATIO, previewSizeRule);
    }


    @Override
    public boolean capture() {
        Log.i(TAG, "capture()");
        if (mIsReady) {
            Log.i(TAG, "set Callback capture()");
            mICameraDevice.setStereoJpsCallback(mStereoCameraJpsCallback);
            mICameraDevice.setStereoMaskCallback(mStereoCameraMaskCallback);
            mICameraDevice.getParameters().setRefocusJpsFileName(REFOCUS_TAG);
            mNeedSaveOrignal = mIsReady;
            mCallbackNum = 0;
        } else {
            mNeedSaveOrignal = mIsReady;
            mICameraDevice.setStereoJpsCallback(null);
            mICameraDevice.setStereoMaskCallback(null);
        }
        mIFileSaver.init(FILE_TYPE.JPEG, 0, null, -1);
        mCaptureStartTime = System.currentTimeMillis();
        mICameraAppUi.setSwipeEnabled(false);
        mICameraAppUi.showRemaining();
        mCameraCategory.takePicture();
        setModeState(ModeState.STATE_CAPTURING);
        return true;
    }

    private class Stereo3dCallback implements StereoJpsCallback {
        public void onCapture(byte[] JPSData) {
            Log.i(TAG, "onCapture JPSData:" + JPSData.length);
            if (JPSData == null) {
                Log.i(TAG, "JPS data is null");
                return;
            }
            mJpsData = JPSData;
            notifyMergeData();
        }
    }

    private class MaskCallback implements StereoMaskCallback {
        public void onCapture(byte[] maskData) {
            Log.i(TAG, "onCapture MaskData:" + maskData.length);
            if (maskData == null) {
                Log.i(TAG, "Mask data is null");
                return;
            }
            mMaskAndConfigData = maskData;
            notifyMergeData();
        }
    }

    private synchronized void notifyMergeData() {
        Log.i(TAG, "notifyMergeData mCallbackNum = " + mCallbackNum);
        mCallbackNum++;
        if (mCallbackNum == 3) {
            Log.i(TAG, "notifyMergeData");
            if (mMaskAndConfigData != null && mJpsData != null
                    && mOriginalJpegData != null) {
                XmpOperator operator = new XmpOperator();
                mXmpJpegData = operator.writeJpsAndMaskAndConfigToJpgBuffer(
                        generateName(mCaptureStartTime),
                        mOriginalJpegData, mJpsData, mMaskAndConfigData);
                if (mXmpJpegData != null) {
                    saveFile(mXmpJpegData, TAG_REFOCUS_IMAGE);
                }
            }
            mCallbackNum = 0;
        }
    }

    private class WarningCallback implements StereoWarningCallback {
        public void onWarning(int type) {
            Log.i(TAG, "onWarning type = " + type);
            switch (type) {
            case DUAL_CAMERA_LOW_LIGHT:
                mICameraAppUi.showToast(R.string.dual_camera_lowlight_toast);
                mIsReady = false;
                break;
            case DUAL_CAMERA_READY:
                mIsReady = true;
                break;
            case DUAL_CAMERA_TOO_CLOSE:
                mICameraAppUi.showToast(R.string.dual_camera_too_close_toast);
                mIsReady = false;
                break;
            case DUAL_CAMERA_LENS_COVERED:
                mICameraAppUi
                        .showToast(R.string.dual_camera_lens_covered_toast);
                mIsReady = false;
                break;
            default:
                Log.w(TAG, "Warning message don't need to show");
                break;
            }
        }
    };

    private final PictureCallback mJpegPictureCallback = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] jpegData, Camera camera) {
            Log.d(TAG, "[mJpegPictureCallback]");
            if (jpegData == null) {
                Log.i(TAG, "[mJpegPictureCallback] jpegData is null");
                mICameraAppUi.setSwipeEnabled(true);
                mICameraAppUi.restoreViewState();
                restartPreview(false);
                return;
            }
            mOriginalJpegData = jpegData;
            if (mCameraClosed) {
                Log.i(TAG, "[mJpegPictureCallback] mCameraClosed:"
                        + mCameraClosed);
                mICameraAppUi.restoreViewState();
                mICameraAppUi.setSwipeEnabled(true);
                return;
            }
            mIFocusManager.updateFocusUI(); // Ensure focus indicator
            restartPreview(true);
            if (!mNeedSaveOrignal) {
                saveFile(mOriginalJpegData, TAG_NORAML_IMAGE);
            }
            notifyMergeData();
            Log.d(TAG, "[mJpegPictureCallback] end");
        }
    };

    private String generateName(long dateTaken) {
        Date date = new Date(dateTaken);
        String result = new SimpleDateFormat(
                mActivity.getString(R.string.image_file_name_format),
                Locale.ENGLISH).format(date);
        result += SUBFFIX_TAG;
        Log.i(TAG, "generateName result = " + result);
        return result;
    }
    private final ShutterCallback mShutterCallback = new ShutterCallback() {
        @Override
        public void onShutter() {
            Log.d(TAG, "[mShutterCallback]");
        }
    };

    class Stereo3dCameraCategory extends CameraCategory {
        public Stereo3dCameraCategory() {
        }

        public void takePicture() {
            mICameraDevice.takePicture(mShutterCallback, null, null, mJpegPictureCallback);
            mICameraAppUi.setViewState(ViewState.VIEW_STATE_CAPTURE);
        }
    }
    private class StereoPreviewSizeRule implements ISettingRule {
        private SettingItem mCurrentSettingItem;
        private ICameraContext mCameraContext;
        private ISettingCtrl mISettingCtrl;
        private SettingItem mPictureRatioSetting;

        public StereoPreviewSizeRule(ICameraContext cameraContext) {
            mCameraContext = cameraContext;
        }
        @Override
        public void execute() {
            mISettingCtrl = mCameraContext.getSettingController();
            mCurrentSettingItem = mISettingCtrl.getSetting(SettingConstants.KEY_REFOCUS);
            mPictureRatioSetting = mISettingCtrl.getSetting(SettingConstants.KEY_PICTURE_RATIO);
            String resultValue = mPictureRatioSetting.getValue();
            String currentValue = mCurrentSettingItem.getValue();
            int currentCameraId = mICameraDeviceManager.getCurrentCameraId();
            ICameraDevice cameraDevice = mICameraDeviceManager
                    .getCameraDevice(currentCameraId);
            Parameters parameters = cameraDevice.getParameters();
            ListPreference pref = mPictureRatioSetting.getListPreference();
            if ("on".equals(currentValue)) {
                String overrideValue = "1.7778";
                resultValue = "1.7778";
                if (mPictureRatioSetting.isEnable()) {
                    mPictureRatioSetting.setValue(resultValue);
                    if (pref != null) {
                         pref.setOverrideValue(overrideValue, true);
                    }
                    SettingUtils.setPreviewSize(mCameraContext.getActivity(),
                            parameters, overrideValue);
                }

                Record record = mPictureRatioSetting.new Record(resultValue, overrideValue);
                mPictureRatioSetting.addOverrideRecord(SettingConstants.KEY_REFOCUS, record);
            } else {
                mPictureRatioSetting.removeOverrideRecord(SettingConstants.KEY_REFOCUS);
                int count = mPictureRatioSetting.getOverrideCount();
                if (count > 0) {
                    Record topRecord = mPictureRatioSetting.getTopOverrideRecord();

                    if (topRecord != null) {
                        String value = topRecord.getValue();
                        String overrideValue = topRecord.getOverrideValue();
                        mPictureRatioSetting.setValue(value);
                        pref = mPictureRatioSetting.getListPreference();
                        if (pref != null) {
                             pref.setOverrideValue(overrideValue);
                        }
                    }
                } else {
                    pref = mPictureRatioSetting.getListPreference();
                    String value = pref.getValue();
                    if (pref != null) {
                        pref.setOverrideValue(null);
                   }
                    mPictureRatioSetting.setValue(resultValue);
                    SettingUtils.setPreviewSize(mICameraContext.getActivity(),
                            parameters, resultValue);
                }
            }
        }

        @Override
        public void addLimitation(String condition, List<String> result,
                MappingFinder mappingFinder) {
            Log.i(TAG, "[addLimitation]condition = " + condition);
        }
    }

    private void showQualityStatus(int geoFlag, int photoFlag) {
        if (!isDebugOpened()) {
            return;
        }
        String msg = null;
        msg = formateShow(geoFlag, photoFlag);
        DialogInterface.OnClickListener buttonListener =
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        };

        new AlertDialog.Builder(mActivity).setCancelable(false)
                .setIconAttribute(android.R.attr.alertDialogIcon).setTitle("")
                .setMessage(msg).setNeutralButton(R.string.dialog_ok, buttonListener)
                .show();
    }

    private String formateShow(int geoFlag, int photoFlag) {
        Log.i(TAG, "geoFlag = " + geoFlag +  "photoFlag = " + photoFlag);
        String geo = null;
        String photo = null;
        if (geoFlag == PASS_NUM) {
            geo = PASS;
        } else if (geoFlag == FAIL_NUM) {
            geo = FAIL;
        } else {
            geo = WARN;
        }
        if (photoFlag == PASS_NUM) {
            photo = PASS;
        } else if (photoFlag == FAIL_NUM) {
            photo = FAIL;
        } else {
            photo = WARN;
        }
        return GEO_QUALITY + geo + "\n" + PHO_QUALITY + photo;
    }

    private boolean isDebugOpened() {
        boolean enable = SystemProperties
                .getInt("debug.STEREO.enable_verify", 0) == 1 ? true : false;
        Log.i(TAG, "[isDebugOpened]return :" + enable);
        return enable;
    }
}
