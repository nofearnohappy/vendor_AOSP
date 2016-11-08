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
package com.mediatek.camera.v2.detection.smiledetection;

import android.app.Activity;
import android.util.Log;

import com.android.camera.R;
import com.mediatek.camera.v2.platform.app.AppUi;

/**
 * The class acts as a role of a view(although there is not any view of smile Detection currently).
 */
public class SdView implements ISdView {
    private static final String TAG = SdView.class.getSimpleName();
    private static final int SHOW_INFO_LENGTH_LONG = 5 * 1000;
    private static final int CAPTURE_INTERVAL_MS = 2 * 1000; // wait 2s for next action;
    private long mLastCaptureTime = 0;

    private final AppUi mAppUi;
    private final Activity mActivity;

    /**
     * Constructor of smile detection view.
     *
     * @param activity
     *            The camera activity.
     * @param appUi
     *            The camera application UI.
     */
    public SdView(Activity activity, AppUi appUi) {
        mAppUi = appUi;
        mActivity = activity;
    }

    @Override
    public void showSmileView() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onSmileStarted");
                mAppUi.showInfo(mActivity.getString(R.string.smileshot_guide_capture),
                        SHOW_INFO_LENGTH_LONG);
                mLastCaptureTime = 0;
            }
        });
    }

    @Override
    public void updateSmileView() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onSmile perform ShutterButton click");
                long currentInterval = System.currentTimeMillis() - mLastCaptureTime;
                if (currentInterval < CAPTURE_INTERVAL_MS) {
                    Log.e(TAG, "onSmile waitting for next capture:" + currentInterval + "ms");
                    return;
                }
                if (mAppUi.getPreviewVisibility() == AppUi.PREVIEW_VISIBILITY_UNCOVERED) {
                    mAppUi.performShutterButtonClick(false);
                }
                mLastCaptureTime = System.currentTimeMillis();
            }
        });
    }

    @Override
    public void hideSmileView() {
        mAppUi.dismissInfo(false);
    }

}
