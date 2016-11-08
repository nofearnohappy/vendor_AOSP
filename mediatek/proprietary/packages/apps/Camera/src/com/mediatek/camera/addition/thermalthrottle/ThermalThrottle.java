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
package com.mediatek.camera.addition.thermalthrottle;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.android.camera.R;
import com.mediatek.camera.ICameraContext;
import com.mediatek.camera.addition.CameraAddition;
import com.mediatek.camera.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ThermalThrottle extends CameraAddition {
    private static final String TAG = "ThermalThrottle";

    private static final String THERMAL_THROTTLE_PATH = "/proc/driver/cl_cam";
    // The min thermal value for camera activity
    private static final int THERMAL_MAX_VALUE = 1;
    private WarningDialog mAlertDialog;

    protected final Handler mHandler = new MainHandler();
    private WorkerHandler mWorkerHandler;
    private HandlerThread mHandlerThread;

    private static final int MSG_READ_THERMAL = 0;
    private static final int MSG_UPDATE_TIME = 1;
    private static final int DELAY = 5000;
    private static final int UPDATE_TIME_DELAY = 1000;
    private static final int WAITING_TIME = 30;

    private int mWatingTime;

    private boolean mIsResumed = false;
    private boolean mIsThermalTooHigh = false;

    public ThermalThrottle(ICameraContext cameraContext) {
        super(cameraContext);
        Log.i(TAG, "[ThermalThrottle]constructor...");
        mAlertDialog = new WarningDialog(mActivity);
        if (queryCPUThermalTooHigh()) {
            showThermalDlg(mActivity, R.string.pref_thermal_dialog_title,
                    R.string.pref_thermal_dialog_content1);
        }

        mHandlerThread = new HandlerThread("ThermalThrottle-thread");
        mHandlerThread.start();
        mWorkerHandler = new WorkerHandler(mHandlerThread.getLooper());
        mWorkerHandler.sendEmptyMessageDelayed(MSG_READ_THERMAL, DELAY);
        mWatingTime = WAITING_TIME;
    }

    @Override
    public boolean isSupport() {
        return true;
    }

    @Override
    public void resume() {
        Log.i(TAG, "[resume]...");
        mIsResumed = true;
        mWatingTime = WAITING_TIME;
        if (mWorkerHandler != null) {
            mWorkerHandler.sendEmptyMessageDelayed(MSG_READ_THERMAL, DELAY);
        }
    }

    @Override
    public void pause() {
        Log.i(TAG, "[pause]...");
        mIsResumed = false;
        if (mWorkerHandler != null) {
            mWorkerHandler.removeCallbacksAndMessages(null);
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void open() {
        Log.i(TAG, "[open]...");
    }

    @Override
    public void close() {
        Log.i(TAG, "[close]...");
        if (mWorkerHandler != null) {
            mWorkerHandler.getLooper().quit();
        }
        if (mHandlerThread != null) {
            mHandlerThread.quit();
        }
    }

    @Override
    public void destory() {
        Log.i(TAG, "[destory]...");
        if (mWorkerHandler != null) {
            mWorkerHandler.getLooper().quit();
        }
        if (mHandlerThread != null) {
            mHandlerThread.quit();
        }
    }

    @Override
    public boolean execute(AdditionActionType type, Object... arg) {
        return false;
    }

    private void updateCountDownTime(final Activity activity) {
        Log.i(TAG, "[updateCountDownTime]mCountDown = " + mWatingTime + ",mIsResumed = "
                + mIsResumed);
        if (isTemperTooHigh()) {
            if (mWatingTime > 0) {
                mWatingTime--;
                mAlertDialog.setCountDownTime(String.valueOf(mWatingTime));
                if (mIsResumed) {
                    mHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, UPDATE_TIME_DELAY);
                }
            } else if (mWatingTime == 0) {
                mIFileSaver.waitDone();
                activity.finish();
            }
        } else {
            if (mAlertDialog.isShowing()) {
                mAlertDialog.hide();
                mWatingTime = WAITING_TIME;
            }
        }
    }

    class MainHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "[handleMessage]MainHandler,msg.what = " + msg.what);
            switch (msg.what) {
            case MSG_UPDATE_TIME:
                updateCountDownTime(mActivity);
                break;

            default:
                break;
            }
        }
    }

    private class WorkerHandler extends Handler {
        public WorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "[handleMessage]WorkerHandler, msg.what = " + msg.what);
            switch (msg.what) {
            case MSG_READ_THERMAL:
                if (queryCPUThermalTooHigh() && !mAlertDialog.isShowing()) {
                    Log.i(TAG, "[handleMessage]WorkerHandler, mCountDown = " + mWatingTime);
                    if (mWatingTime == WAITING_TIME) {
                        showThermalDlg(mActivity, R.string.pref_thermal_dialog_content2);
                        mHandler.removeMessages(MSG_UPDATE_TIME);
                        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, UPDATE_TIME_DELAY);
                    }
                }
                mWorkerHandler.sendEmptyMessageDelayed(MSG_READ_THERMAL, DELAY);
                break;

            default:
                break;
            }
        }
    }

    private void showThermalDlg(final Activity activity, final int resId) {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                mAlertDialog.hide();
            }
        };
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAlertDialog.hide();
                mAlertDialog.showAlertDialog(null, mActivity.getString(resId),
                        mActivity.getString(android.R.string.ok), runnable);
            }
        });
    }

    private void showThermalDlg(final Activity activity, int titleId, int msgId) {
        DialogInterface.OnClickListener buttonListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                activity.finish();
            }
        };

        new AlertDialog.Builder(activity).setCancelable(false)
                .setIconAttribute(android.R.attr.alertDialogIcon).setTitle(titleId)
                .setMessage(msgId).setNeutralButton(R.string.dialog_ok, buttonListener).show();
    }

    private boolean queryCPUThermalTooHigh() {
        String temper = null;
        int temperInt = 0;
        try {
            FileReader fls = new FileReader(THERMAL_THROTTLE_PATH);
            BufferedReader br = new BufferedReader(fls);
            temper = br.readLine();
            temperInt = Integer.valueOf(temper);
            br.close();
            fls.close();

        } catch (IOException err) {
            System.out.println(err.toString());
        } finally {
            Log.i("Thermal", "queryCPUThermal temperInt:" + temperInt);
        }
        if (temper != null && temperInt == THERMAL_MAX_VALUE) {
            mIsThermalTooHigh = true;
            return mIsThermalTooHigh;
        } else {
            mIsThermalTooHigh = false;
            return mIsThermalTooHigh;
        }
    }

    private boolean isTemperTooHigh() {
        return mIsThermalTooHigh;
    }
}
