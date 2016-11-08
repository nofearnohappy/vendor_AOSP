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
package com.mediatek.camera.mode.pip.recorder;

import android.media.MediaCodec;

public class Encoder {
    private static final String TAG = "Encoder";

    // Sync object to protect encode state access from multiple threads.
    private final Object mStateLock = new Object();
    private final Object mPausedDurationLock = new Object();
    private final Object mPresentationTimeUsLock = new Object();
    protected static int ENCODE_STATE_IDLE = 0;
    protected static int ENCODE_STATE_CONFIGURED = 1;
    protected static int ENCODE_STATE_RECORDING = 2;
    protected static int ENCODE_STATE_PAUSED = 3;
    protected static int ENCODE_STATE_STOPPED = 4;

    private int mEncodeState = ENCODE_STATE_IDLE;
    private long mPausedDurationUs = 0L;
    private long mLatestPrentationTimeUs = 0L;

    public Encoder() {
    }

    public MediaCodec getMediaCodec() {
        return null;
    }

    // Thread-safe access to the encode state.
    protected synchronized void setEncodeState(int state) {
        synchronized (mStateLock) {
            if (state < ENCODE_STATE_IDLE) {
                throw new IllegalStateException("try to set an invalid state");
            }
            mEncodeState = state;
        }
    }

    protected int getEncodeState() {
        synchronized (mStateLock) {
            return mEncodeState;
        }
    }

    public void setPausedDurationUs(long pausedDurationUs) {
        synchronized (mPausedDurationLock) {
            mPausedDurationUs = pausedDurationUs;
        }
    }

    public long getPausedDurationUs() {
        synchronized (mPausedDurationLock) {
            return mPausedDurationUs;
        }
    }

    public void setLatestPresentationTimeUs(long ptUs) {
        synchronized (mPresentationTimeUsLock) {
            mLatestPrentationTimeUs = ptUs;
        }
    }

    public long getLatestPresentationTimeUs() {
        synchronized (mPresentationTimeUsLock) {
            return mLatestPrentationTimeUs;
        }
    }
}
