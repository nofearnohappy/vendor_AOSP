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

package com.mediatek.dm;

import android.app.Application;
import android.util.Log;

import com.mediatek.dm.DmConst.TAG;
import com.mediatek.dm.session.SessionEventQueue;
import com.mediatek.dm.util.ScheduledTaskQueue;

public class DmApplication extends Application {
    private static DmApplication sInstance;

    private final ScheduledTaskQueue mSchedTaskQ = new ScheduledTaskQueue();
    private final SessionEventQueue mEventQueue = new SessionEventQueue();

    public void queueEvent(int type, Object extra) {
        mEventQueue.queueEvent(type, extra);
    }

    public void queueEvent(int type) {
        mEventQueue.queueEvent(type, null);
    }

    public synchronized void cancelAllPendingJobs() {
        mSchedTaskQ.removeAll();
    }

    public synchronized void scheduleJob(Runnable job, long delayMillis) {
        Log.d(TAG.DEBUG, "[pending-job]->scheduled after ms:" + delayMillis);
        mSchedTaskQ.addPendingTask(job, delayMillis);
        mSchedTaskQ.dump();
    }

    public synchronized void scheduleBatchJobs(Runnable job, long periodMillis, long totalMillis) {
        if (totalMillis < periodMillis) {
            return;
        }
        final int times = (int) (totalMillis / periodMillis);
        for (int i = 1; i < times; i++) {
            mSchedTaskQ.addPendingTask(job, periodMillis * i);
            Log.d(TAG.DEBUG, "[pending-job]->scheduled batch job:" + periodMillis * i);
        }
        mSchedTaskQ.dump();
    }

    public SessionEventQueue.DLAbortState analyzeDLAbortState() {
        mEventQueue.dump();
        long nowMillis = System.currentTimeMillis();
        return mEventQueue.analyzeAbortState(nowMillis);
    }

    public boolean isDMWapConnected() {
        return mEventQueue.isNetworkConnected();
    }

    public void onCreate() {
        super.onCreate();
        sInstance = this;

        Log.d(TAG.APPLICATION, "~~~DM application created~~~");
    }

    public static DmApplication getInstance() {
        return sInstance;
    }
}
