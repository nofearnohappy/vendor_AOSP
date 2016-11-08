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

import com.android.camera.R;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.mediatek.camera.ICameraContext;
import com.mediatek.camera.ICameraMode.ActionType;
import com.mediatek.camera.platform.ICameraAppUi.ViewState;
import com.mediatek.camera.platform.ICameraDeviceManager.ICameraDevice.SmileShotListener;
import com.mediatek.camera.setting.SettingConstants;
import com.mediatek.camera.util.Log;

public class SmileShot extends CameraAddition {
    private static final String TAG = "SmileShot";

    private static final int MSG_START_SMILE_SHOT = 1000;
    private static final int SHOW_INFO_LENGTH_LONG = 5 * 1000;

    private final MainHandler mMainHandler;
    private boolean mCameraClosed = false;

    private enum State {
        STATE_IDLE, STATE_IN_PROGRESS,
    }

    private State mCurState = State.STATE_IDLE;

    public SmileShot(ICameraContext context) {
        super(context);
        Log.i(TAG, "[SmileShot]constructor...");

        mMainHandler = new MainHandler(mActivity.getMainLooper());
    }

    @Override
    public void open() {
        Log.i(TAG, "[open]...");
        mMainHandler.sendEmptyMessage(MSG_START_SMILE_SHOT);
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
        stopSmileShot();
    }

    @Override
    public boolean isSupport() {
        boolean isSupport = false;
        if ("on".equals(mISettingCtrl.getSettingValue(SettingConstants.KEY_SMILE_SHOT))) {
            isSupport = true;
        }
        Log.i(TAG, "[isSupport] isSupport:" + isSupport);
        return isSupport;
    }

    @Override
    public boolean execute(ActionType type, Object... arg) {
        switch (type) {
        case ACTION_ON_CAMERA_OPEN:
            Log.d(TAG, "[execute] type:" + type);
            mCameraClosed = false;
            break;

        case ACTION_ON_CAMERA_CLOSE:
            Log.d(TAG, "[execute] type:" + type);
            mCameraClosed = true;
            stopSmileShot();
            break;

        default:
            break;
        }
        return false;
    }

    private void startSmileShot() {
        Log.d(TAG, "[startSmileShot]mCurState:" + mCurState);
        if (mCurState == State.STATE_IN_PROGRESS) {
            return;
        }
        updateCameraDevice();
        if (mICameraDevice == null) {
            return;
        }
        mICameraAppUi.showInfo(mActivity.getString(R.string.smileshot_guide_capture),
                SHOW_INFO_LENGTH_LONG);
        mICameraDevice.setSmileCallback(mSmileListener);
        mICameraDevice.startSmileDetection();
        mCurState = State.STATE_IN_PROGRESS;
    }

    private void stopSmileShot() {
        Log.d(TAG, "[stopGuestureDetection]mCurState:" + mCurState);
        if (mCurState == State.STATE_IDLE) {
            return;
        }
        if (mICameraDevice != null) {
            mICameraDevice.stopSmileDetection();
            mICameraDevice.setSmileCallback(null);
        }
        mCurState = State.STATE_IDLE;
    }

    private SmileShotListener mSmileListener = new SmileShotListener() {
        public void onSmile() {
            Log.d(TAG, "[onSmile] mCameraClosed = " + mCameraClosed +
                    ", mICameraAppUi.getViewState() = " + mICameraAppUi.getViewState());
            if (mCurState != State.STATE_IN_PROGRESS) {
                Log.w(TAG, "[onSmile]gesture callback in error state, please check");
                return;
            }

            // when settings show, can not do capture(for User Experience)
            if (!mCameraClosed && (mICameraAppUi.getViewState() != ViewState.VIEW_STATE_SETTING)) {
                mICameraAppUi.getPhotoShutter().performClick();
            }
        }
    };

    private final class MainHandler extends Handler {
        public MainHandler(Looper mainLooper) {
            super(mainLooper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "[handleMessage]msg.what =" + msg.what);
            switch (msg.what) {
            case MSG_START_SMILE_SHOT:
                startSmileShot();
                break;

            default:
                break;
            }
        }
    }
}
