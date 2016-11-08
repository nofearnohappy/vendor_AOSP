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

import android.util.Log;

import com.mediatek.camera.v2.detection.IDetectionCaptureObserver;
import com.mediatek.camera.v2.detection.IDetectionManager.IDetectionListener;
import com.mediatek.camera.v2.detection.IDetectionPresenter;
/**
*
* Gesture detection presenter which interact face detection device with face view.
*
*/
public class GdPresenterImpl implements IGdPresenterListener, IDetectionPresenter {

    private static final String TAG = GdPresenterImpl.class.getSimpleName();

    private IGdView mGdView;
    private GdDeviceImpl mGdDeviceImpl;
    private boolean mIsGdStarted = false;

    /**
     * Gesture detection presenter constructor.
     * @param view Gesture detection view.
     * @param detectionListener Listener used for get capture callback from detection manager.
     */
    public GdPresenterImpl(IGdView view, IDetectionListener detectionListener) {
        mGdView = view;
        mGdDeviceImpl = new GdDeviceImpl(detectionListener);
        mGdDeviceImpl.setListener(this);
    }

    @Override
    public IDetectionCaptureObserver getCaptureObserver() {
        return mGdDeviceImpl.getCaptureObserver();
    }

    @Override
    public void startDetection() {
        if (mIsGdStarted) {
            Log.i(TAG, "gesture detection has been stared so return");
            return;
        }
        mGdView.showGestureView();
        mGdDeviceImpl.requestStartDetection();
        mIsGdStarted = true;
    }

    @Override
    public void stopDetection() {
        if (!mIsGdStarted) {
            Log.i(TAG, "gesture detection has been stopped or not open so return");
            return;
        }
        mGdView.hideGestureView();
        mGdDeviceImpl.requestStopDetection();
        mIsGdStarted = false;
    }

    @Override
    public void updateGestureView() {
        mGdView.updateGestureView();
    }
}
