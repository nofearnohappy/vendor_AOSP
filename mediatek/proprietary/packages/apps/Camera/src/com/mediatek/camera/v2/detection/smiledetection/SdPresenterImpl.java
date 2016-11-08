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

import android.util.Log;

import com.mediatek.camera.v2.detection.IDetectionCaptureObserver;
import com.mediatek.camera.v2.detection.IDetectionManager.IDetectionListener;
import com.mediatek.camera.v2.detection.IDetectionPresenter;
/**
*
* Smile detection presenter which interact face detection device with face view.
*
*/
public class SdPresenterImpl implements ISdPresenterListener, IDetectionPresenter {

    private static final String TAG = SdPresenterImpl.class.getSimpleName();
    private ISdView mSdView;
    private SdDeviceImpl mSdDeviceImpl;
    private boolean mIsSdStarted = false;

    /**
     * Smile detection presenter constructor.
     * @param view Simle detection view.
     * @param detectionListener Listener used for get capture callback from detection manager.
     */
    public SdPresenterImpl(ISdView view, IDetectionListener detectionListener) {
        mSdView = view;
        mSdDeviceImpl = new SdDeviceImpl(detectionListener);
        mSdDeviceImpl.setListener(this);

    }

    @Override
    public void startDetection() {
        if (mIsSdStarted) {
            Log.i(TAG, "smile detection has been stared so return");
            return;
        }
        mSdView.showSmileView();
        mSdDeviceImpl.requestStartDetection();
        mIsSdStarted = true;
    }

    @Override
    public void stopDetection() {
        if (!mIsSdStarted) {
            Log.i(TAG, "smile detection has been stopped or not open so return");
            return;
        }
        mSdView.hideSmileView();
        mSdDeviceImpl.requestStopDetection();
        mIsSdStarted = false;
    }

    @Override
    public IDetectionCaptureObserver getCaptureObserver() {
        return mSdDeviceImpl.getCaptureObserver();
    }

    @Override
    public void onSmile() {
        mSdView.updateSmileView();
    }
}
