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

import android.os.Message;
import android.util.Log;

import com.android.internal.util.IState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.mediatek.bluetoothle.BleProfileServerObjectPool;

/**
 * BleProfileManagerState class
 */
public class BleProfileManagerState extends StateMachine {
    private static final boolean DBG = true;
    private static final String TAG = BleProfileManagerState.class.getSimpleName();
    private final BleProfileManagerService mProfileManagerService;
    private final BleProfileServerObjectPool mPool;

    private static final int MESSAGE_TIMEOUT = 5000;

    /**
     * start message for handler
     */
    public static final int START_PROFILES = 8245;
    /**
     * started message for handler
     */
    public static final int PROFILES_STARTED = 8246;
    /**
     * stop message for handler
     */
    public static final int STOP_PROFILES = 8247;
    /**
     * stopped message for handler
     */
    public static final int PROFILES_STOPPED = 8248;
    /**
     * shutdown message for handler
     */
    public static final int SHUTDOWN = 8249;
    private static final int START_TIMEOUT = 8250;
    private static final int STOP_TIMEOUT = 8251;

    private final State mStopState = new StopState();
    private final State mStartingState = new StartingState();
    private final State mStartState = new StartState();
    private final State mStoppingState = new StoppingState();

    private BleProfileManagerState(final BleProfileManagerService manager,
            final BleProfileServerObjectPool pool) {
        super("BleProfileManagerState:");

        addState(mStopState);
        addState(mStartingState);
        addState(mStartState);
        addState(mStoppingState);
        setInitialState(mStopState);

        mProfileManagerService = manager;
        mPool = (null == pool) ? BleProfileServerObjectPool.getInstance() : pool;
    }

    /**
     * Method to make an instance of BleProfileManagerState
     *
     * @param manager the instance of BleProfileManagerService
     *
     * @return instance of BleProfileManagerState
     */
    public static BleProfileManagerState make(final BleProfileManagerService manager) {
        return make(manager, null);
    }

    /**
     * For Mock Test
     */
    static BleProfileManagerState make(final BleProfileManagerService manager,
            final BleProfileServerObjectPool pool) {
        final BleProfileManagerState state = new BleProfileManagerState(manager, pool);
        state.start();
        return state;
    }

    /**
     * For Mock Test
     */
    final IState getCurtate() {
        return getCurrentState();
    }

    /**
     * Method to make the state machine stopped
     */
    public void doQuit() {
        quitNow();
    }

    private class StopState extends State {
        @Override
        public void enter() {
            if (DBG) {
                Log.d(TAG, "enter StopState");
            }
        }

        @Override
        public boolean processMessage(final Message msg) {
            boolean processed = true;

            if (DBG) {
                Log.d(TAG, "StopState: incoming message:" + msg);
            }
            switch (msg.what) {
                case START_PROFILES:
                    if (DBG) {
                        Log.d(TAG, "StopState: START_PROFILES start profiles!");
                    }
                    sendMessageDelayed(START_TIMEOUT, MESSAGE_TIMEOUT);
                    mProfileManagerService.startProfileServices();
                    transitionTo(mStartingState);
                    break;
                case PROFILES_STOPPED:
                    if (BleProfileManagerState.this.getHandler().hasMessages(SHUTDOWN)) {
                        if (DBG) {
                            Log.d(TAG, "StopState: use shutdown message to stop!");
                        }
                        break;
                    }
                case SHUTDOWN:
                    if (mPool.isAllObjectReleased()) {
                        if (DBG) {
                            Log.d(TAG, "StopState: receive SHUTDOWN/PROFILES_STOPPED");
                        }
                        if (!BleProfileManagerState.this.getHandler().hasMessages(START_PROFILES)) {
                            mProfileManagerService.shutdown();
                        } else {
                            if (DBG) {
                                Log.d(TAG, "StopState: Cancel, due to has START_PROFILES message!");
                            }
                        }
                    } else {
                        if (DBG) {
                            Log.d(TAG, "StopState: sendMessageDelayed"
                                    + "(SHUTDOWN, MESSAGE_TIMEOUT)");
                        }
                        BleProfileManagerState.this.sendMessageDelayed(SHUTDOWN, MESSAGE_TIMEOUT);
                    }
                    break;
                case STOP_PROFILES:
                    if (DBG) {
                        Log.d(TAG, "StopState: Already stopped");
                    }
                    break;
                case PROFILES_STARTED:
                case START_TIMEOUT:
                case STOP_TIMEOUT:
                    if (DBG) {
                        Log.d(TAG, "StopState: Shouldn't receive the messages");
                    }
                    break;
                default:
                    processed = false;
                    if (DBG) {
                        Log.d(TAG, "StopState: Shouldn't receive the messages");
                    }
            }

            return processed;
        }

        @Override
        public void exit() {
            if (DBG) {
                Log.d(TAG, "exit StopState");
            }
        }
    }

    private class StartingState extends State {
        @Override
        public void enter() {
            if (DBG) {
                Log.d(TAG, "enter StartingState");
            }
        }

        @Override
        public boolean processMessage(final Message msg) {
            boolean processed = true;

            if (DBG) {
                Log.d(TAG, "incoming message:" + msg);
            }
            switch (msg.what) {
                case PROFILES_STARTED:
                    if (DBG) {
                        Log.d(TAG, "StartingState: "
                                + "PROFILES_STARTED profiles started successfully");
                    }
                    removeMessages(START_TIMEOUT);
                    transitionTo(mStartState);
                    break;
                case START_TIMEOUT:
                    if (DBG) {
                        Log.d(TAG, "StartingState: START_TIMEOUT profiles fail to start");
                    }
                    mProfileManagerService.dumpProfileServiceInfo();
                    transitionTo(mStartState);
                    break;
                case STOP_PROFILES:
                    if (DBG) {
                        Log.d(TAG, "StartingState: "
                                + "STOP_PROFILES defer stop_profiles to start state");
                    }
                    deferMessage(msg);
                    break;
                case START_PROFILES:
                    if (DBG) {
                        Log.d(TAG, "StartingState: START_PROFILES defer it");
                    }
                    deferMessage(msg);
                    break;
                case SHUTDOWN:
                case PROFILES_STOPPED:
                case STOP_TIMEOUT:
                    if (DBG) {
                        Log.d(TAG, "StartingState: Shouldn't receive the messages");
                    }
                    break;
                default:
                    processed = false;
                    if (DBG) {
                        Log.d(TAG, "StartingState: Shouldn't receive the messages");
                    }
            }

            return processed;
        }

        @Override
        public void exit() {
            if (DBG) {
                Log.d(TAG, "exit StartingState");
            }
        }
    }

    private class StartState extends State {
        @Override
        public void enter() {
            if (DBG) {
                Log.d(TAG, "enter StartState");
            }
        }

        @Override
        public boolean processMessage(final Message msg) {
            boolean processed = true;

            if (DBG) {
                Log.d(TAG, "incoming message:" + msg);
            }
            switch (msg.what) {
                case STOP_PROFILES:
                    if (DBG) {
                        Log.d(TAG, "StartState: STOP_PROFILES stop profiles!");
                    }
                    sendMessageDelayed(STOP_TIMEOUT, MESSAGE_TIMEOUT);
                    mProfileManagerService.stopProfileServices();
                    transitionTo(mStoppingState);
                    break;
                case START_PROFILES:
                    if (DBG) {
                        Log.d(TAG, "StartState: START_PROFILES Already started");
                    }
                    break;
                case SHUTDOWN:
                case PROFILES_STARTED:
                case PROFILES_STOPPED:
                case START_TIMEOUT:
                case STOP_TIMEOUT:
                    if (DBG) {
                        Log.d(TAG, "StartState: Shouldn't receive the messages");
                    }
                    break;
                default:
                    processed = false;
                    if (DBG) {
                        Log.d(TAG, "StartState: Shouldn't receive the messages");
                    }
            }

            return processed;
        }

        @Override
        public void exit() {
            if (DBG) {
                Log.d(TAG, "exit StartState");
            }
        }
    }

    private class StoppingState extends State {
        @Override
        public void enter() {
            if (DBG) {
                Log.d(TAG, "enter StoppingState");
            }
        }

        @Override
        public boolean processMessage(final Message msg) {
            boolean processed = true;

            if (DBG) {
                Log.d(TAG, "incoming message:" + msg);
            }
            switch (msg.what) {
                case PROFILES_STOPPED:
                    if (DBG) {
                        Log.d(TAG, "StoppingState: "
                                + "PROFILES_STOPPED profiles stopped successfully");
                    }
                    removeMessages(STOP_TIMEOUT);
                    transitionTo(mStopState);
                    if (!BleProfileManagerState.this.getHandler().hasMessages(SHUTDOWN)) {
                        if (DBG) {
                            Log.d(TAG, "BleProfileManagerState.this.sendMessage(SHUTDOWN)");
                        }
                        BleProfileManagerState.this.sendMessage(SHUTDOWN);
                    } else {
                        if (DBG) {
                            Log.d(TAG, "Already has SHUTDOWN message");
                        }
                    }
                    break;
                case STOP_TIMEOUT:
                    if (DBG) {
                        Log.d(TAG, "StoppingState: STOP_TIMEOUT profiles fail to stop");
                    }
                    mProfileManagerService.dumpProfileServiceInfo();
                    transitionTo(mStopState);
                    break;
                case STOP_PROFILES:
                    if (DBG) {
                        Log.d(TAG, "StoppingState: STOP_PROFILES defer it");
                    }
                    deferMessage(msg);
                    break;
                case START_PROFILES:
                    if (DBG) {
                        Log.d(TAG, "StoppingState: "
                                + "START_PROFILES defer start_profiles to stop state");
                    }
                    deferMessage(msg);
                    break;
                case SHUTDOWN:
                case PROFILES_STARTED:
                case START_TIMEOUT:
                    if (DBG) {
                        Log.d(TAG, "StoppingState: Shouldn't receive the messages");
                    }
                    break;
                default:
                    processed = false;
                    if (DBG) {
                        Log.d(TAG, "StoppingState: Shouldn't receive the messages");
                    }
            }

            return processed;
        }

        @Override
        public void exit() {
            if (DBG) {
                Log.d(TAG, "exit StoppingState");
            }
        }
    }
}
