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

package com.mediatek.bluetoothle.tip;

import android.os.Message;
import android.util.Log;

import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

final class TimeUpdateStateMachine extends StateMachine {
    private static final String TAG = "TimeUpdateStateMachine";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    static final int MSG_START_REFERENCE_UPDATE = 0;
    static final int MSG_CANCEL_REFERENCE_UPDATE = 1;
    static final int MSG_NO_CONNECTION = 2;
    static final int MSG_TIME_ERROR = 3;
    static final int MSG_TIMEOUT = 4;
    static final int MSG_NEW_TIME = 5;

    static final int UPDATER_SNTP_CLIENT = 0;

    private final IdleState mIdleState = new IdleState();
    private final UpdatePendingState mUpdatePendingState = new UpdatePendingState();
    private final ReferenceTimeUpdateService mRtus;
    private final ITimeUpdater mTimeUpdater = makeUpdater(UPDATER_SNTP_CLIENT);

    private final TimeUpdateCallback mTimeUpdateCb = new TimeUpdateCallback() {
        @Override
        void onTimeUpdated(final long time, final int result) {
            if (DBG) Log.d(TAG, "onStateUpdated: time = " + time + ", result = " + result);
            final Message msg = new Message();

            switch (result) {
                case ReferenceTimeUpdateService.RESULT_SUCCESS:
                    msg.what = TimeUpdateStateMachine.MSG_NEW_TIME;
                    msg.obj = Long.valueOf(time);
                    sendMessage(msg);
                    break;
                case ReferenceTimeUpdateService.RESULT_ERROR:
                    msg.what = TimeUpdateStateMachine.MSG_TIME_ERROR;
                    sendMessage(msg);
                    break;
                case ReferenceTimeUpdateService.RESULT_NO_CONNECTION:
                    msg.what = TimeUpdateStateMachine.MSG_NO_CONNECTION;
                    sendMessage(msg);
                    break;
                case ReferenceTimeUpdateService.RESULT_TIMEOUT:
                    msg.what = TimeUpdateStateMachine.MSG_TIMEOUT;
                    sendMessage(msg);
                    break;
                default:
                    break;
            }
        }
    };

    private TimeUpdateStateMachine(final ReferenceTimeUpdateService rtus) {
        super("TimeUpdateStateMachine");
        addState(mIdleState);
        addState(mUpdatePendingState);
        mRtus = rtus;
        setInitialState(mIdleState);
    }

    private class IdleState extends State {
        @Override
        public void enter() {
            Log.i(TAG, "Entering IdleState");
        }

        @Override
        public boolean processMessage(final Message msg) {
            switch (msg.what) {
                case MSG_START_REFERENCE_UPDATE:
                    if (DBG) Log.d(TAG, "IdleState: MSG_START_REFERENCE_UPDATE");
                    transitionTo(mUpdatePendingState);
                    mRtus.onStateUpdate(ReferenceTimeUpdateService.STATE_UPDATE_PENDING);
                    startUpdate();
                    break;
                case MSG_CANCEL_REFERENCE_UPDATE:
                    if (DBG) Log.d(TAG, "IdleState: MSG_CANCEL_REFERENCE_UPDATE");
                    mRtus.onStateUpdate(ReferenceTimeUpdateService.STATE_IDLE,
                            ReferenceTimeUpdateService.RESULT_CANCELED);
                    break;
                default:
                    Log.e(TAG, "Unexpected message! IdleState: " + msg.what);
                    return false;
            }
            return true;
        }
    }

    private class UpdatePendingState extends State {
        @Override
        public void enter() {
            Log.i(TAG, "Entering UpdatePendingState");
        }

        @Override
        public boolean processMessage(final Message msg) {
            switch (msg.what) {
                case MSG_NEW_TIME:
                    if (DBG) Log.d(TAG, "UpdatePendingState: MSG_NEW_TIME");
                    final Long time = (Long) msg.obj;
                    // Got updated time
                    transitionTo(mIdleState);
                    mRtus.onStateUpdate(ReferenceTimeUpdateService.STATE_IDLE,
                            ReferenceTimeUpdateService.RESULT_SUCCESS);
                    mRtus.onTimeUpdate(time);
                    break;
                case MSG_CANCEL_REFERENCE_UPDATE:
                    if (DBG) Log.d(TAG, "UpdatePendingState: MSG_CANCEL_REFERENCE_UPDATE");
                    transitionTo(mIdleState);
                    mRtus.onStateUpdate(ReferenceTimeUpdateService.STATE_IDLE,
                            ReferenceTimeUpdateService.RESULT_CANCELED);
                    break;
                case MSG_START_REFERENCE_UPDATE:
                    if (DBG) Log.d(TAG, "UpdatePendingState: MSG_START_REFERENCE_UPDATE");
                    mRtus.onStateUpdate(ReferenceTimeUpdateService.STATE_UPDATE_PENDING);
                    break;
                case MSG_NO_CONNECTION:
                    if (DBG) Log.d(TAG, "UpdatePendingState: MSG_NO_CONNECTION");
                    transitionTo(mIdleState);
                    mRtus.onStateUpdate(ReferenceTimeUpdateService.STATE_IDLE,
                            ReferenceTimeUpdateService.RESULT_NO_CONNECTION);
                    break;
                case MSG_TIME_ERROR:
                    if (DBG) Log.d(TAG, "UpdatePendingState: MSG_TIME_ERROR");
                    transitionTo(mIdleState);
                    mRtus.onStateUpdate(ReferenceTimeUpdateService.STATE_IDLE,
                            ReferenceTimeUpdateService.RESULT_ERROR);
                    break;
                case MSG_TIMEOUT:
                    if (DBG) Log.d(TAG, "UpdatePendingState: MSG_TIMEOUT");
                    transitionTo(mIdleState);
                    mRtus.onStateUpdate(ReferenceTimeUpdateService.STATE_IDLE,
                            ReferenceTimeUpdateService.RESULT_TIMEOUT);
                    break;
                default:
                    Log.e(TAG, "Unexpected message! UpdatePendingState: " + msg.what);
                    return false;
            }
            return true;
        }
    }

    static TimeUpdateStateMachine make(final ReferenceTimeUpdateService rtus) {
        if (VDBG) Log.v(TAG, "make");
        final TimeUpdateStateMachine sm = new TimeUpdateStateMachine(rtus);
        sm.start();
        return sm;
    }

    private void startUpdate() {
        if (VDBG) Log.v(TAG, "startUpdate");
        new Thread(new Runnable() {
            @Override
            public void run() {
                mTimeUpdater.updateTime(mTimeUpdateCb);
            }
        }).start();
    }

    static ITimeUpdater makeUpdater(final int type) {
        if (DBG) Log.d(TAG, "updaterFactory: type = " + type);
        ITimeUpdater updater = null;
        switch (type) {
            case UPDATER_SNTP_CLIENT:
                updater = new SntpClientTimeUpdater();
                break;
            default:
                Log.e(TAG, "Unsupported type");
                break;
        }
        return updater;
    }

    void doQuit() {
        if (VDBG) Log.v(TAG, "doQuit: Quit TimeUpdateStateMachine now");
        quitNow();
    }
}
