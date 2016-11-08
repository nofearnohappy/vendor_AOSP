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

package com.mediatek.camera.v2.stream;

import java.util.concurrent.ConcurrentHashMap;

import com.mediatek.camera.v2.stream.IPreviewStream.PreviewCallback;
import com.mediatek.camera.v2.platform.ModeChangeListener;
import com.mediatek.camera.v2.stream.dng.DngStream;
import com.mediatek.camera.v2.stream.dng.IDngStream;
import com.mediatek.camera.v2.stream.facebeauty.CfbStreamControllerImpl;
import com.mediatek.camera.v2.stream.facebeauty.ICfbStreamController;
import com.mediatek.camera.v2.stream.pip.PipStream;
import com.mediatek.camera.v2.stream.pip.IPipStream;

import android.app.Activity;
import android.util.Log;
import android.view.ViewGroup;

/**
 *  A manager used to manage streams.
 */
public class StreamManager {
    private static final String            TAG = "StreamManager";
    private final Activity                 mActivity;
    private ViewGroup                      mParentViewGroup;
    private boolean                        mIsCaptureIntent;

    private PreviewStream                  mPreviewStream;
    private CaptureStream                  mCaptureStream;
    private RecordStream                   mRecordStream;

    // feature stream
    private CfbStreamControllerImpl        mFbStream;
    private PipStream                      mPipStream;
    private DngStream                      mDngStream;

    private static ConcurrentHashMap<Activity, StreamManager>
                     sStreamManangerList = new ConcurrentHashMap<Activity, StreamManager>();

    private StreamManager(Activity activity) {
        mActivity = activity;
    }

    public static synchronized StreamManager getInstance(Activity activity) {
        StreamManager streamManager = sStreamManangerList.get(activity);
        if (streamManager == null) {
            streamManager = new StreamManager(activity);
            sStreamManangerList.put(activity, streamManager);
        }
        return streamManager;
    }

    public void open(ViewGroup parentView, boolean isCaptureIntent) {
        mParentViewGroup = parentView;
        mIsCaptureIntent = isCaptureIntent;
    }

    public void close(Activity activity) {
        if (activity.isFinishing()) {
            Log.i(TAG, "close activity:" + activity);
            StreamManager streamManager = sStreamManangerList.get(activity);
            sStreamManangerList.remove(activity);
            if (streamManager != null) {
                streamManager.releaseStreams();
            }
        }
    }

    public IPreviewStream getPreviewController(int modeId) {
        return choosePreviewStreamByModeId(modeId);
    }

    public PreviewCallback getPreviewCallback() {
        return getPreviewStream();
    }

    public ICaptureStream getCaptureController(int modeId) {
        return chooseCaptureStreamByModeId(modeId);
    }

    public IRecordStream getRecordController(int modeId) {
        return chooseRecordStreamByModeId(modeId);
    }

    public IDngStream getDngStreamController() {
        return getDngStream();
    }

    public ICfbStreamController getFbStreamController() {
        return getFbStream();
    }

    public IPipStream getPipStreamController() {
        return getPipStream();
    }

    protected void releaseStreams() {
        if (mPreviewStream != null) {
            mPreviewStream.releasePreviewStream();
            mPreviewStream = null;
        }

        if (mCaptureStream != null) {
            mCaptureStream.releaseCaptureStream();
            mCaptureStream = null;
        }

        if (mRecordStream != null) {
            mRecordStream.releaseRecordStream();
            mRecordStream = null;
        }
        if (mFbStream != null) {
            mFbStream.releaseCaptureStream();
            mFbStream = null;
        }
    }

    private IPreviewStream choosePreviewStreamByModeId(int modeId) {
        Log.i(TAG, " choosePreviewStreamByModeId modeId = " + modeId);
        IPreviewStream controller = getPreviewStream();
        switch (modeId) {
        case ModeChangeListener.MODE_CAPTURE:
            controller = getPreviewStream();
            break;
        case ModeChangeListener.MODE_FACE_BEAUTY:
            break;
        case ModeChangeListener.MODE_HDR:
            break;
        case ModeChangeListener.MODE_PIP:
            controller = getPipStream();
            break;
        default:
            break;
        }
        return controller;
    }

    private ICaptureStream chooseCaptureStreamByModeId(int modeId) {
        Log.i(TAG, " chooseCaptureStreamByModeId modeId = " + modeId);
        ICaptureStream controller = getCaptureStream();
        switch (modeId) {
        case ModeChangeListener.MODE_CAPTURE:
            controller =  getCaptureStream();
            break;
        case ModeChangeListener.MODE_FACE_BEAUTY:
            controller = getFbStream();
            break;
        case ModeChangeListener.MODE_PIP:
            controller = getPipStream();
            break;
        default:
            break;
        }
        return controller;
    }

    private IRecordStream chooseRecordStreamByModeId(int modeId) {
        Log.i(TAG, " chooseRecordStreamByModeId modeId = " + modeId);
        IRecordStream controller = getRecordStream();
        switch (modeId) {
        case ModeChangeListener.MODE_CAPTURE:
            controller =  getRecordStream();
            break;
        case ModeChangeListener.MODE_FACE_BEAUTY:
            break;
        case ModeChangeListener.MODE_HDR:
            break;
        case ModeChangeListener.MODE_PIP:
            controller = getPipStream();
            break;
        default:
            break;
        }
        return controller;
    }

    private PreviewStream getPreviewStream() {
        if (mPreviewStream == null) {
            mPreviewStream = new PreviewStream();
        }
        return mPreviewStream;
    }

    private RecordStream getRecordStream() {
        if (mRecordStream == null) {
            mRecordStream = new RecordStream();
        }
        return mRecordStream;
    }

    private CaptureStream getCaptureStream() {
        if (mCaptureStream == null) {
            mCaptureStream = new CaptureStream();
        }
        return mCaptureStream;
    }

    private DngStream getDngStream() {
        if (mDngStream == null) {
            mDngStream = new DngStream(getCaptureStream());
        }
        return mDngStream;
    }

    private CfbStreamControllerImpl getFbStream() {
        if (mFbStream == null) {
            mFbStream = new CfbStreamControllerImpl(getCaptureStream());
        }
        return mFbStream;
    }

    private PipStream getPipStream() {
        if (mPipStream == null) {
            mPipStream = new PipStream(getPreviewStream(), getCaptureStream(), getRecordStream());
        }
        return mPipStream;
    }
}
