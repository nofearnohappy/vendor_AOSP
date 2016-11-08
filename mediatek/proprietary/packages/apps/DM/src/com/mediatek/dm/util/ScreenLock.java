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
 * MediaTek Inc. (C) 2010. All rights reserved.
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

package com.mediatek.dm.util;

import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class ScreenLock {
    private static WakeLock sFullWakelock;
    private static WakeLock sPartialWakelock;
    @SuppressWarnings("deprecation")
    private static KeyguardLock sKeyguardLock;
    private static final String TAG = "DM/ScreenLock";
    private static final String FULL_LOCK_TAG = "dm_FullLock";
    private static final String KEYGUARD_TAG = "dm_KL";

    public static void acquirePartialWakelock(Context context) {
        // get a PowerManager instance
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (sPartialWakelock == null) {
            // get WakeLock
            sPartialWakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "dm_PartialLock");
            if (!sPartialWakelock.isHeld()) {
                Log.d(TAG, "need to aquire partial wake up");
                // wake lock
                sPartialWakelock.acquire();
            } else {
                sPartialWakelock = null;
                Log.d(TAG, "not need to aquire partial wake up");
            }
        }
    }

    public static void acquireFullWakelock(Context context) {
        // get a PowerManager instance
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        if (sFullWakelock == null) {
            // get WakeLock
            sFullWakelock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                    | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                    FULL_LOCK_TAG);
            if (!sFullWakelock.isHeld()) {
                Log.d(TAG, "need to aquire full wake up");
                // wake lock
                sFullWakelock.acquire();
            } else {
                sFullWakelock = null;
                Log.d(TAG, "not need to aquire full wake up");
            }
        }
    }

    @SuppressWarnings("deprecation")
    public static void disableKeyguard(Context context) {
        // get a KeyguardManager instance
        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);

        if (sKeyguardLock == null) {
            // get KeyguardLock
            sKeyguardLock = km.newKeyguardLock(KEYGUARD_TAG);
            if (km.inKeyguardRestrictedInputMode()) {
                Log.d(TAG, "need to disableKeyguard");
                // release key guard lock
                sKeyguardLock.disableKeyguard();
            } else {
                sKeyguardLock = null;
                Log.d(TAG, "not need to disableKeyguard");
            }
        }
    }

    public static void releaseFullWakeLock(Context context) {
        if (sFullWakelock != null) {
            if (sFullWakelock.isHeld()) {
                sFullWakelock.release();
                sFullWakelock = null;
                Log.d(TAG, "releaseFullWakeLock release");
            } else {
                Log.d(TAG, "releaseFullWakeLock mWakelock.isHeld() == false");
            }
        } else {
            Log.d(TAG, "releaseFullWakeLock mWakelock == null");
        }
    }

    public static void releasePartialWakeLock(Context context) {
        if (sPartialWakelock != null) {
            if (sPartialWakelock.isHeld()) {
                sPartialWakelock.release();
                sPartialWakelock = null;
                Log.d(TAG, "releasePartialWakeLock release");
            } else {
                Log.d(TAG, "releasePartialWakeLock mWakelock.isHeld() == false");
            }
        } else {
            Log.d(TAG, "releasePartialWakeLock mWakelock == null");
        }
    }

    @SuppressWarnings("deprecation")
    public static void enableKeyguard(Context context) {
        if (sKeyguardLock != null) {
            sKeyguardLock.reenableKeyguard();
            sKeyguardLock = null;
            Log.d(TAG, "enableKeyguard reenableKeyguard");
        } else {
            Log.d(TAG, "enableKeyguard mKeyguardLock == null");
        }
    }

    public static void releaseWakeLock(Context context) {
        releasePartialWakeLock(context);
        releaseFullWakeLock(context);
    }

}
