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
package com.mediatek.camera.v2.detection.gesturedetection;

import android.app.Activity;
import android.app.Service;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Vibrator;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.ViewGroup;

import com.android.camera.R;
import com.mediatek.camera.v2.platform.app.AppController;
import com.mediatek.camera.v2.platform.app.AppUi;
import com.mediatek.camera.v2.services.ISoundPlayback;
import com.mediatek.camera.v2.setting.ISettingServant;
import com.mediatek.camera.v2.ui.CountDownView;
import com.mediatek.camera.v2.util.SettingKeys;
/**
 * Gesture detection view used to show toast and count down view.
 */
public class GdView implements CountDownView.OnCountDownStatusListener, IGdView {

    private static final String TAG = GdView.class.getSimpleName();
    private AppUi mAppUi;
    private GdView mGestureDetectionView;
    private Activity mActivity;
    private String mSelfTimerValue;
    private Drawable mGestureDrawable;
    private ISoundPlayback mSoundPlayback;
    private ISettingServant mSettingServant;
    private CountDownView mCountdownView;
    private static final int COUNTDOWN_TIME_SEC = 2;
    private static final int COUNTDOWN_TIME_MS = 2 * 1000;
    private static final int SHOW_INFO_DURATION_MS = 5 * 1000;
    private static final int CAPTURE_INTERVAL_MS = 2 * 1000; // wait 2s for next action;
    private static final float VOLUME = 0.6f;
    //pattern an array of longs of times for which to turn the vibrator on or off.
    private static final long[] VIBRATOR_PATTERN_ARRARY = new long[] { 0, 50, 50, 100, 50 };
    private long mLastCaptureTime = 0;
    /**
     * Constructor of gesture detection view.
     * @param app The camera app controller.
     * @param settingServant The current setting instance.
     */
    public GdView(AppController app, ISettingServant settingServant) {
        mSettingServant = settingServant;
        mAppUi = app.getCameraAppUi();
        mSoundPlayback = app.getServices().getSoundPlayback();

    }

    @Override
    public void init(Activity activity, ViewGroup parentView) {
        mActivity = activity;
        mGestureDrawable = mActivity.getResources().getDrawable(R.drawable.ic_gesture_on);
        // inflate CountDownView
        activity.getLayoutInflater().inflate(R.layout.count_down_view, parentView, true);
        mCountdownView = (CountDownView) parentView.findViewById(R.id.count_down_view);
    }

    @Override
    public void onPreviewAreaChanged(RectF previewArea) {
        Log.d(TAG, "onPreviewAreaChanged");
        mCountdownView.onPreviewAreaChanged(previewArea);
    }

    @Override
    public void onRemainingSecondsChanged(int remainingSeconds) {
        Log.d(TAG, "onRemainingSecondsChanged remainingSeconds is " + remainingSeconds);
        if (remainingSeconds == 1) {
            mSoundPlayback.play(R.raw.timer_final_second, VOLUME);
        } else if (remainingSeconds == 2) {
            mSoundPlayback.play(R.raw.timer_increment, VOLUME);
        }
    }

    @Override
    public void onCountDownFinished() {
        Log.d(TAG, "onCountDownFinished");
        switchCommonUiByCountingDown(false);
        mAppUi.performShutterButtonClick(false);
        mLastCaptureTime = System.currentTimeMillis();
        mSettingServant.doSettingChange(SettingKeys.KEY_SELF_TIMER, mSelfTimerValue, true);
    }

    @Override
    public void showGestureView() {
        Log.d(TAG, "onGestureStarted");
        String guideString = mActivity.getString(R.string.gestureshot_guide_capture);
        mGestureDrawable.setBounds(0, 0, mGestureDrawable.getIntrinsicWidth(),
                mGestureDrawable.getIntrinsicHeight());
        ImageSpan span = new ImageSpan(mGestureDrawable, ImageSpan.ALIGN_BASELINE);
        SpannableString spanStr = new SpannableString(guideString + "1");
        spanStr.setSpan(span, spanStr.length() - 1, spanStr.length(),
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        mAppUi.showInfo(spanStr, SHOW_INFO_DURATION_MS);
        mLastCaptureTime = 0;
    }

    @Override
    public void updateGestureView() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onGesture perform called");
                if (mCountdownView.isCountingDown()) {
                    Log.d(TAG, "onGesture isCountingDown so return");
                    return;
                }
                long currentInterval = System.currentTimeMillis() - mLastCaptureTime;
                if (currentInterval < CAPTURE_INTERVAL_MS) {
                    Log.e(TAG, "onGesture waitting for next capture:" + currentInterval + "ms");
                    return;
                }
                if (mAppUi.getPreviewVisibility() == AppUi.PREVIEW_VISIBILITY_UNCOVERED) {
                    Log.d(TAG, "onGesture perform ShutterButton click");
                    // vibrate when gesture is detected
                    Vibrator vibrator = (Vibrator) mActivity.getApplication().getSystemService(
                            Service.VIBRATOR_SERVICE);
                    vibrator.vibrate(VIBRATOR_PATTERN_ARRARY, -1);
                    mCountdownView.setCountDownStatusListener(GdView.this);
                    mCountdownView.startCountDown(COUNTDOWN_TIME_SEC);
                    switchCommonUiByCountingDown(true);
                    mAppUi.showInfo(mActivity.getString(R.string.count_down_title_text),
                            COUNTDOWN_TIME_MS);
                    // override SelfTimer to 0 to disable
                    mSelfTimerValue = mSettingServant.getSettingValue(SettingKeys.KEY_SELF_TIMER);
                    mSettingServant.doSettingChange(SettingKeys.KEY_SELF_TIMER, "0", true);
                }
            }
        });
    }

    @Override
    public void hideGestureView() {
        mAppUi.dismissInfo(false);
    }

    /**
     * Common UI except shutter button should be hide when in counting down and show when count down
     * finished.
     *
     * @param isCountingDown
     *            Whether it is in counting down
     */
    private void switchCommonUiByCountingDown(boolean isCountingDown) {
        if (isCountingDown) {
            mAppUi.setShutterButtonEnabled(false, false);
            mAppUi.setShutterButtonEnabled(false, true);
            mAppUi.setAllCommonViewButShutterVisible(false);
            mAppUi.setSwipeEnabled(false);
        } else {
            mAppUi.setShutterButtonEnabled(true, false);
            mAppUi.setShutterButtonEnabled(true, true);
            mAppUi.setAllCommonViewButShutterVisible(true);
            mAppUi.setSwipeEnabled(true);
        }
    }

    @Override
    public void cancelCountDown() {
        if (mCountdownView.isCountingDown()) {
            mCountdownView.cancelCountDown();
        }
    }

}
