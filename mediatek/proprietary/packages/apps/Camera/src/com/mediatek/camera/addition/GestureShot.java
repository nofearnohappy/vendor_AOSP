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
package com.mediatek.camera.addition;

import android.graphics.drawable.Drawable;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;

import com.android.camera.R;

import com.mediatek.camera.ICameraContext;
import com.mediatek.camera.ICameraAddition.AdditionActionType;
import com.mediatek.camera.ICameraMode.ActionType;
import com.mediatek.camera.platform.ICameraAppUi.ViewState;
import com.mediatek.camera.platform.ICameraDeviceManager.ICameraDevice.GestureShotListener;
import com.mediatek.camera.platform.ISelfTimeManager;
import com.mediatek.camera.setting.SettingConstants;
import com.mediatek.camera.util.Log;

public class GestureShot extends CameraAddition {
    private static final String TAG = "GestureShot";

    public static final int SHOW_INFO_LENGTH_LONG = 5 * 1000;
    private static final int GESTURE_COUNT_DOWN = 2000;
    private static final int GESTRUE_VOLUME = 200;
    private static final int MSG_START_GESTURE_SHOT = 1000;
    private static final int BACK_ID = 0;
    private static final int FRONT_ID = 1;

    private boolean mCameraClosed = false;
    private boolean mIsStartingSelfTimer = false;
    private boolean mIsFullScreenChanged = true;

    private final MainHandler mMainHandler;

    private Drawable mGestureDrawable;
    private Listener mListener;
    private ToneGenerator mGestureTone;
    private ISelfTimeManager mISelfTimeManager;
    private State mCurState = State.STATE_IDLE;
    private boolean[] mInfoShown = new boolean[2];
    private int mCurrentId = -1;

    private enum State {
        STATE_IDLE, STATE_IN_PROGRESS,
    }

    public GestureShot(ICameraContext context) {
        super(context);
        Log.i(TAG, "[GestureShot]constructor...");
        mMainHandler = new MainHandler(mActivity.getMainLooper());
        mGestureDrawable = mActivity.getResources().getDrawable(R.drawable.ic_gesture_on);
        mISelfTimeManager = context.getSelfTimeManager();
    }

    @Override
    public void setListener(Listener listener) {
        mListener = listener;
    }

    @Override
    public void open() {
        Log.i(TAG, "[open]...");
        mCurrentId = mICameraDeviceManager.getCurrentCameraId();
        mMainHandler.sendEmptyMessage(MSG_START_GESTURE_SHOT);
    }

    @Override
    public boolean isOpen() {
        boolean isOpen = false;
        if (State.STATE_IDLE != mCurState) {
            isOpen = true;
        }
        Log.d(TAG, "[isOpen] isOpen:" + isOpen);
        return isOpen;
    }

    @Override
    public void close() {
        Log.i(TAG, "[close]...");
        resetInfoState();
        stopGuestureDetection();
    }

    @Override
    public boolean isSupport() {
        boolean isSupport = false;
        if ("on".equals(mISettingCtrl.getSettingValue(SettingConstants.KEY_GESTURE_SHOT))) {
            isSupport = true;
        }
        Log.i(TAG, "[isSupport] isSupport:" + isSupport);
        return isSupport;
    }

    @Override
    public boolean execute(ActionType type, Object... arg) {
        Log.d(TAG, "[execute] type:" + type);
        switch (type) {
        case ACTION_ON_CAMERA_OPEN:
            mCameraClosed = false;
            break;

        case ACTION_ON_CAMERA_CLOSE:
            mCameraClosed = true;
            stopGuestureDetection();
            break;

        case ACTION_ON_FULL_SCREEN_CHANGED:
            mIsFullScreenChanged = (Boolean) arg[0];
            break;

        default:
            break;
        }

        return false;
    }

    @Override
    public boolean execute(AdditionActionType type, Object... arg) {
        switch (type) {
        case ACTION_ON_START_PREVIEW:
                Log.i(TAG, "[execute] on start preview ,mIsStartingSelfTimer = "
                        + mIsStartingSelfTimer);
                if (mIsStartingSelfTimer) {
                mISelfTimeManager.setSelfTimerDuration(String.valueOf(0));
                mIsStartingSelfTimer = false;
            }
            break;

        default:
            break;
        }

        return false;
    }

    private final class MainHandler extends Handler {
        public MainHandler(Looper mainLooper) {
            super(mainLooper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_START_GESTURE_SHOT:
                Log.i(TAG, "[handleMessage]msg.what = MSG_START_GESTURE_SHOT.");
                startGuestureDetection();
                break;

            default:
                break;
            }
        }
    }

    private void resetInfoState() {
        if (mCurrentId == mICameraDeviceManager.getBackCameraId()) {
            mInfoShown[BACK_ID] = false;
        }
        if (mCurrentId == mICameraDeviceManager.getFrontCameraId()) {
            mInfoShown[FRONT_ID] = false;
        }
    }

    private void setInfoShownState() {
        if (mCurrentId == mICameraDeviceManager.getBackCameraId()) {
            mInfoShown[BACK_ID] = true;
        }
        if (mCurrentId == mICameraDeviceManager.getFrontCameraId()) {
            mInfoShown[FRONT_ID] = true;
        }
    }

    private boolean needShowInfo() {
        if (mCurrentId == mICameraDeviceManager.getBackCameraId()) {
            return !mInfoShown[BACK_ID];
        }
        if (mCurrentId == mICameraDeviceManager.getFrontCameraId()) {
            return !mInfoShown[FRONT_ID];
        }
        return true;
    }

    private void startGuestureDetection() {
        Log.d(TAG, "[startGuestureDetection]mCurState:" + mCurState);
        if (mCurState == State.STATE_IN_PROGRESS) {
            return;
        }
        updateCameraDevice();
        if (mICameraDevice == null) {
            Log.w(TAG, "[startGuestureDetection]mICameraDevice is null.");
            return;
        }
        // in this case should not show gestureShot info: leave out VFB mode by
        // the preview not have face in 5s, and then detected face automatic
        // into VFB mode.
        if (needShowInfo() && !mICameraAppUi.isNeedBackToVFBMode()) {
            String guideString = mActivity
                    .getString(R.string.gestureshot_guide_capture);
            mGestureDrawable.setBounds(0, 0,
                    mGestureDrawable.getIntrinsicWidth(),
                    mGestureDrawable.getIntrinsicHeight());

            ImageSpan span = new ImageSpan(mGestureDrawable,
                    ImageSpan.ALIGN_BASELINE);
            SpannableString spanStr = new SpannableString(guideString + "1");
            spanStr.setSpan(span, spanStr.length() - 1, spanStr.length(),
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            mICameraAppUi.showInfo(spanStr, SHOW_INFO_LENGTH_LONG);
            setInfoShownState();
        }
        mICameraDevice.setGestureCallback(mGestureListener);
        mICameraDevice.startGestureDetection();
        mCurState = State.STATE_IN_PROGRESS;
    }

    private void stopGuestureDetection() {
        Log.d(TAG, "[stopGuestureDetection]mCurState:" + mCurState);
        if (mCurState == State.STATE_IDLE) {
            return;
        }

        if (mICameraDevice != null) {
            mICameraDevice.stopGestureDetection();
            mICameraDevice.setGestureCallback(null);
        }

        mCurState = State.STATE_IDLE;
    }

    private GestureShotListener mGestureListener = new GestureShotListener() {

        public void onGesture() {
            Log.d(TAG, "[onGesture]mCurState = " + mCurState + ",mCameraClosed = " + mCameraClosed
                    + ",viewState:" + mICameraAppUi.getViewState());
            if (mCurState != State.STATE_IN_PROGRESS || mListener == null) {
                Log.e(TAG, "gesture callback in error state, please check");
                return;
            }

            if (mISelfTimeManager == null) {
                Log.w(TAG, "[onGesture]SelfTimerManager is null,return.");
                return;
            }

            if (!mIsFullScreenChanged) {
                Log.d(TAG, "onFullScreenChanged is false, return");
                return;
            }
            // when settings show, can not do capture(for User Experience)
            if (!mCameraClosed && (mICameraAppUi.getViewState() != ViewState.VIEW_STATE_SETTING)) {
                // vibrate when gesture is detected
                // Vibrator vibrator = mModuelCtrl.getVibrator();
                // vibrator.vibrate(new long[]{0, 50, 50, 100, 50}, -1);
                if (mGestureTone != null) {
                    mGestureTone.startTone(ToneGenerator.TONE_DTMF_9, GESTRUE_VOLUME);
                }

                if (mISelfTimeManager.isSelfTimerEnabled()) {
                    Log.d(TAG, "[onGesture]isSelfTimerEnabled is true.");

                    mICameraAppUi.getPhotoShutter().performClick();
                } else {
                    // stopGuestureDetection();
                    Log.d(TAG, "[onGesture]startSelfTimer ");
                    if (mICameraAppUi.updateRemainStorage() <= 0) {
                        mISelfTimeManager.setLowStorage();
                    } else {
                        mICameraAppUi.setSwipeEnabled(false);
                        mISelfTimeManager.setSelfTimerDuration(String
                                .valueOf(GESTURE_COUNT_DOWN));
                        mISelfTimeManager.needPlaySound(true);
                        mIsStartingSelfTimer = true;
                        mISelfTimeManager.startSelfTimer();
                        mICameraAppUi
                                .setViewState(ViewState.VIEW_STATE_CAPTURE);
                    }
                }
            }
        }
    };
}
