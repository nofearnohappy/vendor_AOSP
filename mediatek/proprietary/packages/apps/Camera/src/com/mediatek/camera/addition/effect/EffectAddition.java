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

import android.hardware.Camera.Size;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Surface;

import com.mediatek.camera.ICameraContext;
import com.mediatek.camera.ICameraAddition.AdditionActionType;
import com.mediatek.camera.ICameraMode.ActionType;
import com.mediatek.camera.ISettingCtrl;
import com.mediatek.camera.addition.CameraAddition;
import com.mediatek.camera.platform.ICameraAppUi.SpecViewType;
import com.mediatek.camera.platform.ICameraAppUi.ViewState;
import com.mediatek.camera.platform.ICameraDeviceManager.ICameraDevice;
import com.mediatek.camera.platform.ICameraView;
import com.mediatek.camera.platform.Parameters;
import com.mediatek.camera.setting.ParametersHelper;
import com.mediatek.camera.setting.SettingConstants;
import com.mediatek.camera.setting.preference.ListPreference;
import com.mediatek.camera.util.Log;

import java.util.List;

public class EffectAddition extends CameraAddition {
    private static final String TAG = "EffectAddition";

    private static final int DELAY_MSG_REMOVE_GRID_MS = 3000;
    private static final int MSG_ORIENTATION_CHANGED = 0;
    private static final int MSG_ON_BACK_KEY_PRESSED = 1;
    private static final int MSG_ON_PREVIEW_DISPLAY_SIZE_CHANGED = 2;
    private static final int MSG_ON_CAMERA_CLOSE = 4;
    private static final int MSG_ON_PHOTO_SHUTTER_BUTTON_CLICK = 5;
    private static final int MSG_ON_VIDEO_SHUTTER_BUTTON_CLICK = 6;
    private static final int MSG_ON_FULL_SCREEN_CHANGED = 7;
    private static final int MSG_ON_CONFIGURATION_CHANGED = 8;

    private int mNormalPreviewWidth = 0;
    private int mNormalPreviewHeight = 0;

    private boolean mShowEffects = false;

    private State mCurrentState = State.STATE_CLOSE;
    private String mCurrrentFocusMode = null;
    private ICameraView mICameraView;
    private Effect mEffect;
    private ListPreference mEffectPreference;
    private Listener mModeListener;
    private MainHandler mMainHandler;
    private boolean mIsFaceDetectionOpened = false;
    private boolean mIs3dnrOn = false;

    private enum State {
        STATE_OPEN, STATE_CLOSE,
    }

    private static final String[] MAX_SIZE_SUPPORT_BY_EFFECT = new String[] {
            "800x600", // 4:3
            "960x540", // 16:9
            "800x480", // 5:3
            "900x600", // 3:2
    };

    public EffectAddition(ICameraContext cameraContext) {
        super(cameraContext);
        Log.i(TAG, "[EffectAddition]constructor...");

        EffectListener effectListener = new EffectListener();
        mEffect = new Effect(cameraContext);
        mEffect.setListener(effectListener);

        mICameraView = mICameraAppUi.getCameraView(SpecViewType.ADDITION_EFFECT);
        mICameraView.init(cameraContext.getActivity(), mICameraAppUi, mIModuleCtrl);
        EffectViewListener effectViewListener = new EffectViewListener(this);
        mICameraView.setListener(effectViewListener);

        mMainHandler = new MainHandler(mActivity.getMainLooper());
    }

    @Override
    public void open() {
        Log.i(TAG, "[open]...");
        mCurrentState = State.STATE_OPEN;
    }

    @Override
    public boolean isSupport() {
        updateCameraDevice();
        updateFocusManager();
        return true;
    }

    @Override
    public void pause() {
        Log.i(TAG, "[pause]...");
        mShowEffects = false;
        mICameraView.update(EffectView.HIDE_EFFECT, false, 0);
    }

    @Override
    public void destory() {
        Log.i(TAG, "[destory]...");
        if (mEffect != null) {
            mEffect.release();
        }
    }

    @Override
    public void close() {
        Log.i(TAG, "[close]...");
        mCurrentState = State.STATE_CLOSE;
    }

    @Override
    public boolean execute(ActionType type, Object... arg) {
        Log.i(TAG, "[execute]type = " + type);
        Message msg = mMainHandler.obtainMessage();
        switch (type) {
        case ACTION_ON_COMPENSATION_CHANGED:
            Log.i(TAG, "[execute]onOrientation = " + arg[0]);
            msg.what = MSG_ORIENTATION_CHANGED;
            msg.arg1 = (Integer) arg[0];
            mMainHandler.sendMessage(msg);
            break;

        case ACTION_ON_BACK_KEY_PRESS:
            Log.i(TAG, "[execute] on back key pressed");
            msg.what = MSG_ON_BACK_KEY_PRESSED;
            boolean isShowing = false;
            if (mShowEffects) {
                isShowing = true;
            }
            mMainHandler.sendMessage(msg);
            return isShowing;

        case ACTION_ON_PREVIEW_DISPLAY_SIZE_CHANGED:
            Log.i(TAG, "[execute] on preview size changed: width:" + arg[0] + ", " + "height:"
                    + arg[1]);
            msg.what = MSG_ON_PREVIEW_DISPLAY_SIZE_CHANGED;
            msg.arg1 = (Integer) arg[0];
            msg.arg2 = (Integer) arg[1];
            mMainHandler.sendMessage(msg);
            break;

        case ACTION_ON_CAMERA_CLOSE:
            msg.what = MSG_ON_CAMERA_CLOSE;
            mMainHandler.sendMessage(msg);
            break;
        case ACTION_PHOTO_SHUTTER_BUTTON_CLICK:
            msg.what = MSG_ON_PHOTO_SHUTTER_BUTTON_CLICK;
            mMainHandler.sendMessage(msg);
            break;

        case ACTION_VIDEO_SHUTTER_BUTTON_CLICK:
            msg.what = MSG_ON_VIDEO_SHUTTER_BUTTON_CLICK;
            mMainHandler.sendMessage(msg);
            break;
        case ACTION_ON_FULL_SCREEN_CHANGED:
            msg.what = MSG_ON_FULL_SCREEN_CHANGED;
            msg.obj = arg[0];
            mMainHandler.sendMessage(msg);
            break;
        case ACTION_ON_CONFIGURATION_CHANGED:
            msg.what = MSG_ON_CONFIGURATION_CHANGED;
            mMainHandler.sendMessage(msg);
            break;
        default:
            break;
        }

        return false;
    }

    @Override
    public boolean execute(AdditionActionType type, Object... arg) {
        Log.i(TAG, "[execute]type = " + type);
        switch (type) {
        case ACTION_EFFECT_CLICK:
            showEffect();
            break;
        default:
            break;
        }
        return false;
    }

    @Override
    public void setListener(Listener listener) {
        mModeListener = listener;
    }

    private class EffectViewListener implements EffectView.Listener {
        private EffectAddition mEffectAddition;

        public EffectViewListener(EffectAddition effectAddition) {
            mEffectAddition = effectAddition;
        }

        @Override
        public void onInitialize() {
            mEffect.onInitialize();
        }

        @Override
        public void onSurfaceAvailable(Surface surface, int width, int height, int effectIndex) {
            mEffect.onSurfaceAvailable(surface, width, height, effectIndex);
        }

        @Override
        public void onUpdateEffect(int pos, int effectIndex) {
            mEffect.onUpdateEffect(pos, effectIndex);
        }

        @Override
        public void onReceivePreviewFrame(boolean received) {
            mEffect.onReceivePreviewFrame(received);
        }

        @Override
        public void onRelease() {
            mEffect.onRelease();
        }

        @Override
        public void onItemClick(String value) {
            Parameters parameter = mICameraDevice.getParameters();
            if (parameter != null && !parameter.getColorEffect().equals(value)) {
                Log.i(TAG, "effect selected:" + value);
                ISettingCtrl settingCtrl = mICameraContext.getSettingController();
                if (settingCtrl != null) {
                    settingCtrl.onSettingChanged(SettingConstants.KEY_COLOR_EFFECT, value);
                    mICameraDevice.applyParameters();
                }
            }
            mEffectAddition.hideEffect(true, DELAY_MSG_REMOVE_GRID_MS);
        }

        @Override
        public void hideEffect(boolean anmition, int animationTime) {
            mEffectAddition.hideEffect(anmition, animationTime);
        }
    };

    private class EffectListener implements Effect.Listener {
        @Override
        public void onEffectsDone() {
            mICameraView.update(EffectView.ON_EFFECT_DONE);
        }
    }

    private void showEffect() {
        updateCameraDevice();
        Log.i(TAG, "[showEffect]mShowEffects = " + mShowEffects + ", mICameraDevice:"
                + mICameraDevice);
        if (mICameraDevice == null) {
            return;
        }

        if (!mShowEffects) {
            mShowEffects = true;
            mICameraAppUi.setSwipeEnabled(false);
            // mISettingCtrl = mICameraContext.getSettingController();
            mEffectPreference = mISettingCtrl.getListPreference(SettingConstants.KEY_COLOR_EFFECT);

            setEffectParameters(mICameraDevice);
            mEffect.onReceivePreviewFrame(true);

            int currentCameraId = mICameraDeviceManager.getCurrentCameraId();
            int frontCameraId = mICameraDeviceManager.getFrontCameraId();
            boolean mirror = (currentCameraId == frontCameraId ? true : false);
            mICameraView.update(EffectView.SHOW_EFFECT, mEffectPreference, mirror);
            closeSmileShotAndGetureShot();
        }
    }

    private void hideEffect(boolean animation, int animationTime) {
        Log.i(TAG, "[hideEffect], mShowEffects:" + mShowEffects);
        if (mShowEffects) {
            mShowEffects = false;
            mICameraAppUi.setSwipeEnabled(true);
            mEffect.onReceivePreviewFrame(false);
            resetParameters(mICameraDevice);
            mICameraView.update(EffectView.HIDE_EFFECT, animation, animationTime);
            revertSmileShotAndGetureShot();
        }
    }

    private boolean isSmileShotOpened = false;
    private boolean isGestureShotOpened = false;
    private void closeSmileShotAndGetureShot() {
        int cameraId = mICameraDeviceManager.getCurrentCameraId();
        if ("on".equals(mISettingCtrl.getSettingValue(SettingConstants.KEY_SMILE_SHOT))) {
            isSmileShotOpened = true;
            mISettingCtrl.setSettingValue(SettingConstants.KEY_SMILE_SHOT, "off", cameraId);
        }
        if ("on".equals(mISettingCtrl.getSettingValue(SettingConstants.KEY_GESTURE_SHOT))) {
            isGestureShotOpened = true;
            mISettingCtrl.setSettingValue(SettingConstants.KEY_GESTURE_SHOT, "off", cameraId);
        }
        mICameraContext.getAdditionManager().onCameraParameterReady(true);
    }

    private void revertSmileShotAndGetureShot() {
        int cameraId = mICameraDeviceManager.getCurrentCameraId();
        if (isSmileShotOpened) {
            isSmileShotOpened = false;
            mISettingCtrl.setSettingValue(SettingConstants.KEY_SMILE_SHOT, "on", cameraId);
        }

        if (isGestureShotOpened) {
            isGestureShotOpened = false;
            mISettingCtrl.setSettingValue(SettingConstants.KEY_GESTURE_SHOT, "on", cameraId);
        }
        mICameraContext.getAdditionManager().onCameraParameterReady(true);
    }

    private void setEffectParameters(ICameraDevice cameraDevice) {
        Parameters parameters = cameraDevice.getParameters();
        int cameraId = mICameraDeviceManager.getCurrentCameraId();
        if (parameters != null) {
            if (parameters.getColorEffect() != Parameters.EFFECT_NONE) {
                parameters.setColorEffect(Parameters.EFFECT_NONE);
            }

            Size normalSize = parameters.getPreviewSize();
            mNormalPreviewWidth = normalSize.width;
            mNormalPreviewHeight = normalSize.height;
            Size effectSize = getEffectPreviewSize(parameters, normalSize);
            parameters.setPreviewSize(effectSize.width, effectSize.height);
            if (mModeListener != null) {
                mModeListener.restartPreview(true);
            }
            mICameraAppUi.setViewState(ViewState.VIEW_STATE_LOMOEFFECT_SETTING);
            mCurrrentFocusMode = parameters.getFocusMode();
            List<String> supportedFocusModes = parameters.getSupportedFocusModes();
            if (supportedFocusModes.contains(Parameters.FOCUS_MODE_INFINITY)) {
                parameters.setFocusMode(Parameters.FOCUS_MODE_INFINITY);
            }
            // Stop face detection and noise reduction when matrix display opened.
            String fdvalue = mISettingCtrl.getSettingValue(
                    SettingConstants.KEY_CAMERA_FACE_DETECT);
            if ("on".equals(fdvalue)) {
                mIsFaceDetectionOpened = true;
                mIModuleCtrl.stopFaceDetection();
            }
            String noiseReducationValue = mISettingCtrl.getSettingValue(
                    SettingConstants.KEY_VIDEO_3DNR);
            if ("on".equals(noiseReducationValue)) {
                mIs3dnrOn = true;
                ParametersHelper.setParametersValue(parameters, cameraId,
                        SettingConstants.KEY_VIDEO_3DNR, "off");
            }
            cameraDevice.applyParameters();
        }
    }

    private void resetParameters(ICameraDevice cameraDevice) {
        Parameters parameters = cameraDevice.getParameters();
        int cameraId = mICameraDeviceManager.getCurrentCameraId();
        if (parameters != null) {
            parameters.setPreviewSize(mNormalPreviewWidth, mNormalPreviewHeight);
            parameters.setColorEffect(mEffectPreference.getValue());
            if (mModeListener != null) {
                mModeListener.restartPreview(true);
            }
            parameters.setFocusMode(mCurrrentFocusMode);
            // Recover face detection and noise reduction when matrix display closed.
            if (mIsFaceDetectionOpened) {
                mIsFaceDetectionOpened = false;
                mIModuleCtrl.startFaceDetection();
            }
            if (mIs3dnrOn) {
                mIs3dnrOn = false;
                ParametersHelper.setParametersValue(parameters, cameraId,
                        SettingConstants.KEY_VIDEO_3DNR, "on");
            }
            cameraDevice.applyParameters();
        }
    }

    private Size getEffectPreviewSize(Parameters parameters, final Size normalSize) {
        Size targetSize = normalSize;
        int normalWidth = normalSize.width;
        int normalHeight = normalSize.height;
        double targetRatio = (double) normalWidth / normalHeight;
        int maxWidth = 0;
        int maxHeight = 0;
        for (int i = 0; i < MAX_SIZE_SUPPORT_BY_EFFECT.length; i++) {
            String maxSize = MAX_SIZE_SUPPORT_BY_EFFECT[i];
            int index = maxSize.indexOf('x');
            if (index != -1) {
                int width = Integer.parseInt(maxSize.substring(0, index));
                int height = Integer.parseInt(maxSize.substring(index + 1));
                if (width == 0 || height == 0) {
                    continue;
                }
                double ratio = (double) width / height;
                if (Math.abs(ratio - targetRatio) > 0.02) {
                    continue;
                }
                maxWidth = width;
                maxHeight = height;
            }
        }
        // width must be divided by 32 with no remainder
        if (normalWidth * normalHeight > maxWidth * maxHeight || normalWidth % 32 != 0) {
            List<Size> sizes = parameters.getSupportedPreviewSizes();
            for (int i = sizes.size() - 1; i >= 0; i--) {
                Size size = sizes.get(i);
                int width = size.width;
                int height = size.height;
                double ratio = (double) width / height;
                if ((width * height > maxWidth * maxHeight)
                        || Math.abs(ratio - targetRatio) > 0.001
                        || width % 32 != 0) {
                    continue;
                } else {
                    targetSize = size;
                    break;
                }
            }
        }
        Log.i(TAG, "[getEffectPreviewSize] preview size:" + targetSize.width + ", "
                + targetSize.height);
        return targetSize;
    }

    private class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "[handleMessage]msg.what = " + msg.what);
            switch (msg.what) {
            case MSG_ORIENTATION_CHANGED:
                mICameraView.onOrientationChanged(msg.arg1);
                break;

            case MSG_ON_BACK_KEY_PRESSED:
                hideEffect(true, DELAY_MSG_REMOVE_GRID_MS);
                break;

            case MSG_ON_PREVIEW_DISPLAY_SIZE_CHANGED:
                mICameraView.update(EffectView.ON_SIZE_CHANGED, msg.arg1, msg.arg2);
                break;

            case MSG_ON_CAMERA_CLOSE:
                mICameraView.update(EffectView.ON_CAMERA_CLOSE);
                break;

            case MSG_ON_PHOTO_SHUTTER_BUTTON_CLICK:
                hideEffect(false, 0);
                break;

            case MSG_ON_VIDEO_SHUTTER_BUTTON_CLICK:
                hideEffect(false, 0);
                break;

            case MSG_ON_FULL_SCREEN_CHANGED:
                boolean full = (Boolean) msg.obj;
                if (!full) {
                    hideEffect(false, 0);
                }
                break;
            case MSG_ON_CONFIGURATION_CHANGED:
                hideEffect(false, 0);
                break;
            default:
                break;
            }
        }
    }
}
