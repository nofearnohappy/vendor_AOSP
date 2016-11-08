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

import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.util.Log;

import com.mediatek.camera.v2.detection.IDetectionCaptureObserver;
import com.mediatek.camera.v2.detection.IDetectionDevice;
import com.mediatek.camera.v2.detection.IDetectionManager.IDetectionListener;
import com.mediatek.camera.v2.module.ModuleListener.CaptureType;
import com.mediatek.camera.v2.module.ModuleListener.RequestType;
import com.mediatek.camera.v2.vendortag.TagRequest;
import com.mediatek.camera.v2.vendortag.TagResult;

/**
 * Gesture detection device which used to interact with capture device.
 */
public class GdDeviceImpl implements IDetectionDevice {
    private static final String TAG = GdDeviceImpl.class.getSimpleName();

    private IGdPresenterListener mListener;

    private IDetectionListener mDetectionListener;
    private GdCaptureObserver mCaptureObserver = new GdCaptureObserver();

    private static final int GESTURE_RESULT_SIMPLE = 1;
    private boolean mIsGdRequested = false;

    /**
     * Constructor of gesture detection device.
     *
     * @param detectionListener
     *            Listener used for get capture callback from detection manager.
     */
    public GdDeviceImpl(IDetectionListener detectionListener) {
        mDetectionListener = detectionListener;
    }

    public IDetectionCaptureObserver getCaptureObserver() {
        return mCaptureObserver;
    }

    public void setListener(IGdPresenterListener listener) {
        mListener = listener;
    }

    @Override
    public void requestStartDetection() {
        Log.i(TAG, "requestStartDetection");
        mIsGdRequested = true;
        mDetectionListener.requestChangeCaptureRequest(false,
                mDetectionListener.getRepeatingRequestType(), CaptureType.REPEATING_REQUEST);
    }

    @Override
    public void requestStopDetection() {
        Log.i(TAG, "requestStopDetection");
        mIsGdRequested = false;
        mDetectionListener.requestChangeCaptureRequest(false,
                mDetectionListener.getRepeatingRequestType(), CaptureType.REPEATING_REQUEST);

    }

    /**
     * Class used for get capture callback from detection manager.
     */
    private class GdCaptureObserver implements IDetectionCaptureObserver {

        @Override
        public void configuringRequests(CaptureRequest.Builder requestBuilder,
                RequestType requestType) {
            int gdMode = mIsGdRequested ? CaptureRequest.STATISTICS_FACE_DETECT_MODE_SIMPLE
                    : CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF;
            requestBuilder.set(TagRequest.STATISTICS_GESTURE_MODE, gdMode);
            Log.i(TAG, "configuringRequests done,gdMode = " + gdMode);
        }

        @Override
        public void onCaptureStarted(CaptureRequest request, long timestamp, long frameNumber) {
            Log.i(TAG, "onCaptureStarted");
        }

        @Override
        public void onCaptureCompleted(CaptureRequest request, TotalCaptureResult result) {
            Integer gsResult = result.get(TagResult.STATISTICS_GESTURE_DETECTED_RESULT);
            Log.i(TAG, "onCaptureCompleted gsResult is " + gsResult);
            if (gsResult != null && gsResult == GESTURE_RESULT_SIMPLE) {
                mListener.updateGestureView();
            }
        }
    }

}
