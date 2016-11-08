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

package com.mediatek.dm.session;

import android.util.Log;

import com.mediatek.dm.DmConst.TAG;
import com.mediatek.dm.option.Options;

import com.redbend.android.RbException.VdmError;

import java.util.ArrayList;

public class SessionEventQueue {
    /***
     * Session Event type definitions
     */
    public static final int EVENT_DL_SESSION_UNKNOWN = 0;
    public static final int EVENT_DL_SESSION_STARTED = 1;
    public static final int EVENT_DL_SESSION_ABORTED = 2;
    public static final int EVENT_DL_SESSION_COMPLETED = 3;

    public static final int EVENT_CONN_UNKNOWN = 10;
    public static final int EVENT_CONN_CONNECTED = 11;
    public static final int EVENT_CONN_DISCONNECTED = 12;

    // const queue size
    private static final int QUEUE_QUOTA = 20;

    private ArrayList<Event> mEventQueue;

    private Object mLock;

    private int mLastConnEvent;

    private Event mLastSessionEvent;

    public SessionEventQueue() {
        mEventQueue = new ArrayList<Event>(QUEUE_QUOTA);
        mLock = new Object();
        mLastConnEvent = EVENT_CONN_UNKNOWN;
        mLastSessionEvent = null;
    }

    public void queueEvent(int eventType, Object extra) {
        Event event = new Event();
        event.mType = eventType;
        event.mTimestamp = System.currentTimeMillis();
        event.mExtra = extra;

        synchronized (mLock) {
            if (mEventQueue.size() == QUEUE_QUOTA) {
                mEventQueue.remove(0);
            }

            if (eventType == EVENT_DL_SESSION_STARTED) {
                Log.d(TAG.DEBUG, "[event-queue]->new session started, reset Q.");
                mEventQueue.clear();
            }

            mEventQueue.add(event);

            if (isConnEvent(eventType)) {
                // cache last connection state.
                mLastConnEvent = eventType;
            } else {
                // cache last session event here.
                mLastSessionEvent = event;
            }
        }
    }

    public void dump() {
        Log.d(TAG.DEBUG, "------ dumping event queue ------");
        synchronized (mLock) {
            for (Event evt : mEventQueue) {
                Log.d(TAG.DEBUG, "" + evt);
            }
        }
        Log.d(TAG.DEBUG, "------ dumping finished ------");
    }

    public DLAbortState analyzeAbortState(long nowMillis) {
        DLAbortState state = new DLAbortState();

        synchronized (mLock) {
            if (mEventQueue.size() == 0) {
                return state;
            }

            if (mLastSessionEvent == null) {
                Log.w(TAG.DEBUG, "[DLAbortState]->no cached last session event.");
                return state;
            }

            boolean isConnIssue = false;

            if (mLastSessionEvent.mType == EVENT_DL_SESSION_ABORTED) {
                Integer lastErrorObj = (Integer) mLastSessionEvent.mExtra;
                int errCode = lastErrorObj.intValue();

                if (errCode == VdmError.COMMS_FATAL.val || errCode == VdmError.COMMS_NON_FATAL.val
                        || errCode == VdmError.COMMS_SOCKET_ERROR.val
                        || errCode == VdmError.COMMS_HTTP_ERROR.val
                        || errCode == VdmError.BAD_URL.val) {
                    // we only analyze abort issues caused by network issue.
                    // except SOCKET_TIMEOUT
                    isConnIssue = true;
                }
            }

            if (!isConnIssue) {
                Log.w(TAG.DEBUG, "[DLAbortState]->no need to analyze, not ABORT via network issue.");
                return state;
            }

            boolean hasDisconnectEvent = false;
            int indexOfReconnectEvent = -1;
            for (int index = mEventQueue.size() - 1; index >= 0; index--) {
                Event evt = mEventQueue.get(index);
                if (evt.mType == EVENT_CONN_DISCONNECTED) {
                    hasDisconnectEvent = true;
                    if (nowMillis - evt.mTimestamp > Options.DLTimeoutWait.WAIT_INTERVAL) {
                        state.mValue = DLAbortState.STATE_ALREADY_TIMEOUT;
                        break;
                    } else {

                        if (indexOfReconnectEvent > index) {
                            // network already recovered
                            state.mValue = DLAbortState.STATE_NEED_RESUME_NOW;
                        } else {
                            state.mValue = DLAbortState.STATE_HAVENOT_TIMEOUT;
                            state.mLeftTime = Options.DLTimeoutWait.WAIT_INTERVAL
                                    - (nowMillis - evt.mTimestamp);
                        }
                        break;
                    }
                } else if (evt.mType == EVENT_CONN_CONNECTED) {
                    if (indexOfReconnectEvent == -1) {
                        indexOfReconnectEvent = index;
                    }
                }
            }

            // if disconnect event haven't been received
            if (!hasDisconnectEvent) {
                state.mValue = DLAbortState.STATE_HAVENOT_TIMEOUT;
                state.mLeftTime = Options.DLTimeoutWait.WAIT_INTERVAL
                        - (nowMillis - mLastSessionEvent.mTimestamp);
            }

        }

        Log.d(TAG.DEBUG, "[DLAbortState]analyze result=>" + state.mValue + "," + state.mLeftTime);
        return state;
    }

    public boolean isNetworkConnected() {
        boolean isConnected = false;
        synchronized (mLock) {
            if (mLastConnEvent == EVENT_CONN_CONNECTED) {
                isConnected = true;
            }
        }
        Log.d(TAG.DEBUG, "[event-queue]->isNetworkConnected:" + isConnected);
        return isConnected;
    }

    private boolean isConnEvent(int eventType) {
        return (eventType == EVENT_CONN_UNKNOWN || eventType == EVENT_CONN_CONNECTED
                || eventType == EVENT_CONN_DISCONNECTED);
    }

    public static class Event {
        public int mType;
        public Object mExtra;
        public long mTimestamp;

        public Event() {
            mType = EVENT_DL_SESSION_UNKNOWN;
            mExtra = null;
            mTimestamp = 0L;
        }

        @Override
        public String toString() {
            return String.format("[session event]type=%d, extra=%s, time=%d", mType, mExtra,
                    mTimestamp);
        }
    }

    public static class DLAbortState {
        public static final int STATE_UNDEFINE = 100;
        // already passed 5 min
        public static final int STATE_ALREADY_TIMEOUT = 101;

        // still within 5 min
        public static final int STATE_HAVENOT_TIMEOUT = 102;

        // still within 5 min, and connection has been re-setup.
        public static final int STATE_NEED_RESUME_NOW = 103;

        public int mValue = STATE_UNDEFINE;
        public long mLeftTime;
    }
};
