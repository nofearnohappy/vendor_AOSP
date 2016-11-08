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

package com.mediatek.camera.addition.objecttracking;

import android.app.Activity;
import android.hardware.Camera.Face;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.android.camera.R;

import com.mediatek.camera.platform.ICameraAppUi;
import com.mediatek.camera.platform.IModuleCtrl;
import com.mediatek.camera.ui.CameraView;
import com.mediatek.camera.util.Log;

public class ObjectTrackingView extends CameraView {
    private static final String TAG = "ObjectTrackingView";

    private static final int RESET_VIEW = 0;
    private static final int UNINT_LAYOUT = 1;
    // The width of the preview frame layout.
    private int mPreviewWidth;
    // The height of the preview frame layout.
    private int mPreviewHeight;

    private ObjectViewComponent mObjectView;
    private ICameraAppUi mICameraAppUi;
    private Handler mHandler;

    public ObjectTrackingView(Activity activity) {
        super(activity);
        Log.i(TAG, "[ObjectView]constructor...");
        mHandler = new MainHandler(activity.getMainLooper());
    }

    @Override
    public void init(Activity activity, ICameraAppUi cameraAppUi, IModuleCtrl moduleCtrl) {
        Log.i(TAG, "[init]...");
        mICameraAppUi = cameraAppUi;
    }

    @Override
    public void uninit() {
        Log.i(TAG, "[unint]...");
        mHandler.sendEmptyMessage(UNINT_LAYOUT);
    }

    @Override
    public View getView() {
        Log.i(TAG, "[getView]...");
        View view = View.inflate(mActivity, R.layout.object_track_view, null);
        mObjectView = (ObjectViewComponent) view.findViewById(R.id.object_view1);
        return view;
    }

    @Override
    public void addView(View view) {
        Log.i(TAG, "[addView]...");
        ((ViewGroup) mICameraAppUi.getPreviewFrameLayout()).addView(view);
    }

    @Override
    public void reset() {
        Log.i(TAG, "[reset]...");
        mHandler.sendEmptyMessage(RESET_VIEW);
    }

    @Override
    public boolean update(int type, Object...args) {
        Log.d(TAG, "[update] type = " + type);
        if (mObjectView == null) {
            return false;
        }
        switch (type) {
        case ObjectTracking.ORITATION_CHANGED:
            mObjectView.setOrientation((Integer) args[0]);
            break;

        case ObjectTracking.COMPESATION_CHANGED:
            mObjectView.setOrientationCompesation((Integer) args[0]);
            break;

        case ObjectTracking.UPDATE_OT_FRAME:
            mObjectView.setObject((Face) args[0]);
            break;

        case ObjectTracking.UPDATE_VARIABLE_FOR_RESTART:
            mObjectView.resetVariable();
            break;

        case ObjectTracking.UPDATE_DISPLAY_ORIENTATION:
            mObjectView.setDisplayOrientation((Integer) args[0]);
            break;

        case ObjectTracking.START_ANIMATION:
            StartAnimate((Integer) args[0], (Integer) args[1]);
            break;

        case ObjectTracking.SET_VIEW_VISIBILITE:
            mObjectView.setVisibility(View.VISIBLE);
            break;

        case ObjectTracking.SET_PREVIEW_WIDTH_HEIGHT:
            mObjectView.setPreviewWidthAndHeight((Integer) args[0], (Integer) args[1]);
            mPreviewHeight = (Integer) args[1];
            mPreviewWidth = (Integer) args[0];

            break;

        case ObjectTracking.UNCROP_PREVIEW_SIZE:
            mObjectView.setUnCropWidthAndHeight((Integer) args[0], (Integer) args[1]);
            break;

        case ObjectTracking.REMOVE_RESET_EVENT:
            mHandler.removeMessages(RESET_VIEW);
            mHandler.removeMessages(UNINT_LAYOUT);
            break;

            default:
                return true;
            }
        return true;
    }

    private void StartAnimate(int x, int y) {
        Log.i(TAG, "ObjectTrackingView StartAnimate mPreviewWidth = " + mPreviewWidth
                + ", mPreviewHeight" + mPreviewHeight);
        mObjectView.initView();
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mObjectView
                .getLayoutParams();
        int len = Math.min(mPreviewWidth, mPreviewHeight) / 3;
        params.width = len;
        params.height = len;
        int left = clamp(x - len / 2, 0, mPreviewWidth - len);
        int top = clamp(y - len / 2, 0, mPreviewHeight - len);
        params.setMargins(left, top, 0, 0);
        mObjectView.requestLayout();
        mObjectView.showStart();
    }

    private int clamp(int x, int min, int max) {
        if (x > max)
            return max;
        if (x < min)
            return min;
        return x;
    }

    private void callSuperUninit() {
        Log.i(TAG, "callSuperUninit");
        super.uninit();
    }

    private class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "[handleMessage]msg.what = " + msg.what);
            switch (msg.what) {
            case RESET_VIEW:
                mObjectView.clear();
                break;

            case UNINT_LAYOUT:
                ObjectTrackingView.this.callSuperUninit();
                break;

            default:
                break;
            }
        }
    }
}
