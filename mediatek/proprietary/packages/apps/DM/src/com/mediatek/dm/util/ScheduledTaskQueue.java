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

import android.util.Log;

import com.mediatek.dm.DmConst.TAG;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ScheduledTaskQueue {
    private static final int QUEUE_SIZE = 1;

    private ScheduledThreadPoolExecutor mThreadPool;
    private List<ScheduledFuture<?>> mQueuedTasks;
    private Object mLock;

    public ScheduledTaskQueue() {
        mThreadPool = new ScheduledThreadPoolExecutor(QUEUE_SIZE);
        mQueuedTasks = new ArrayList<ScheduledFuture<?>>();
        mLock = new Object();
    }

    public void addPendingTask(Runnable task, long delayMS) {
        synchronized (mLock) {
            ScheduledFuture<?> schedTask = mThreadPool.schedule(task, delayMS,
                    TimeUnit.MILLISECONDS);
            mQueuedTasks.add(schedTask);
        }
    }

    public void removeAll() {
        synchronized (mLock) {
            for (ScheduledFuture<?> schedTask : mQueuedTasks) {
                schedTask.cancel(false);
            }
            mQueuedTasks.clear();
            Log.d(TAG.DEBUG,
                    "++ [removing all] ++, thread pool size = " + mThreadPool.getTaskCount());
        }
    }

    public void dump() {
        Log.d(TAG.DEBUG, "---- dumping sched task Q ----");
        synchronized (mLock) {
            for (ScheduledFuture<?> schedTask : mQueuedTasks) {
                long delayMS = schedTask.getDelay(TimeUnit.MILLISECONDS);
                Log.d(TAG.DEBUG, "[sched-task] delays-->" + delayMS);
            }
        }
        Log.d(TAG.DEBUG, "---- dumping finished ----");
    }
}
