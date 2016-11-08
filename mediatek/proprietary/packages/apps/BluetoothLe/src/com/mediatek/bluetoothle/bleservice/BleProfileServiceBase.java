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

package com.mediatek.bluetoothle.bleservice;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.util.HashMap;

/**
 * Base class for all BleProfileService
 */
public abstract class BleProfileServiceBase extends Service {
    private static final boolean DBG = true;
    private static final int PROFILE_SERVICE_MODE = Service.START_NOT_STICKY;
    private static BluetoothAdapter sAdapter = BluetoothAdapter.getDefaultAdapter();
    protected String mName = BleProfileServiceBase.class.getSimpleName();
    protected IProfileServiceBinder mBinder;
    protected boolean mStartError = false;
    private boolean mCleaningUp = false;

    // For Debugging only
    private static HashMap<String, Integer> sReferenceCount = new HashMap<String, Integer>();

    /**
     * Interface for IBinder
     */
    public interface IProfileServiceBinder extends IBinder {
        /**
         * Method to do cleanup when binder is finished
         *
         * @return true, if cleanup is success. flase, if cleanup failed.
         */
        boolean cleanup();
    }

    protected String getName() {
        return getClass().getSimpleName();
    }

    protected boolean isAvailable() {
        return !mStartError && !mCleaningUp;
    }

    protected abstract IProfileServiceBinder initBinder();

    protected boolean start() {
        return true;
    }

    protected boolean stop() {
        return true;
    }

    protected boolean cleanup() {
        return true;
    }

    protected BleProfileServiceBase() {
        mName = getName();
        if (DBG) {
            synchronized (sReferenceCount) {
                Integer refCount = sReferenceCount.get(mName);
                if (refCount == null) {
                    refCount = 1;
                } else {
                    refCount = refCount + 1;
                }
                sReferenceCount.put(mName, refCount);
                log("REFCOUNT: CREATED. INSTANCE_COUNT=" + refCount);
            }
        }
    }

    @Override
    protected void finalize() {
        if (DBG) {
            synchronized (sReferenceCount) {
                Integer refCount = sReferenceCount.get(mName);
                if (refCount != null) {
                    refCount = refCount - 1;
                } else {
                    refCount = 0;
                }
                sReferenceCount.put(mName, refCount);
                log("REFCOUNT: FINALIZED. INSTANCE_COUNT=" + refCount);
            }
        }
    }

    @Override
    public void onCreate() {
        com.mediatek.common.jpe.a aa = new com.mediatek.common.jpe.a();
        aa.a();
        log("onCreate");
        super.onCreate();
        mBinder = initBinder();
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        log("onStartCommand(): intent=" + intent + ", flags=" + flags + ",startId=" + startId);

        if (mStartError || null == sAdapter) {
            log("onStartCommand(): Stopping profile service -"
                    + " due to fail to start, call stopSelf()");
            stopSelf();
            return PROFILE_SERVICE_MODE;
        }

        if (null == intent) {
            // it shouldn't happen when PROFILE_SERVICE_MODE =
            // Service.START_NOT_STICKY
            log("Restarting profile service... PROFILE_SERVICE_MODE:" + PROFILE_SERVICE_MODE);
            return PROFILE_SERVICE_MODE;
        }

        final String action = intent.getStringExtra(BleApp.EXTRA_ACTION);
        if (BleApp.ACTION_SERVICE_STATE_CHANGED.equals(action)) {
            final int state =
                    intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            if (state == BluetoothAdapter.STATE_OFF) {
                log("onStartCommand(): Received stop request...Stopping profile...");
                doStop(intent);
            } else if (state == BluetoothAdapter.STATE_ON) {
                log("onStartCommand(): Received start request. Starting profile...");
                doStart(intent);
            }
        } else {
            log("onStartCommand(): Unknown action=" + action);
        }
        return PROFILE_SERVICE_MODE;
    }

    private void doStart(final Intent intent) {
        log("doStart: before start()");
        mStartError = !start();
        log("doStart: after start()");
        if (!mStartError) {
            notifyProfileServiceStateChanged(BluetoothAdapter.STATE_ON);
        } else {
            log("doStart: Error starting profile due to fail to start");
            stopSelf();
            log("doStart: after stopSelf()");
        }
    }

    private void doStop(final Intent intent) {
        log("doStop: before stop()");
        if (stop()) {
            log("doStop: after stop()");
            stopSelf();
            log("doStop: [dump message queue] " + Looper.myQueue().dumpMessageQueue());
            log("doStop: after stopSelf()");
        } else {
            log("doStop: Unable to stop profile");
        }
    }

    protected void notifyProfileServiceStateChanged(final int state) {
        log("notifyProfileServiceStateChanged:" + state);
        // Notify to bleProfileManagerService
        final BleProfileManagerService bleProfileManagerService = BleProfileManagerService
                .getBleProfileManagerService();
        if (bleProfileManagerService != null) {
            bleProfileManagerService.onProfileServiceStateChanged(getClass().getName(), state);
        }
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        log("Destroying service.");
        if (mCleaningUp) {
            log("Cleanup already started... Skipping cleanup()...");
        } else {
            log("cleanup()");
            mCleaningUp = true;
            cleanup();
            if (mBinder != null) {
                mBinder.cleanup();
                mBinder = null;
            }
        }
        notifyProfileServiceStateChanged(BluetoothAdapter.STATE_OFF);
        super.onDestroy();
    }

    protected void log(final String msg) {
        if (DBG) {
            Log.d(mName, msg);
        }
    }
}
