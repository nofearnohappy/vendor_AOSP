/*
* This software/firmware and related documentation ("MediaTek Software") are
* protected under relevant copyright laws. The information contained herein
* is confidential and proprietary to MediaTek Inc. and/or its licensors.
* Without the prior written permission of MediaTek inc. and/or its licensors,
* any reproduction, modification, use or disclosure of MediaTek Software,
* and information contained herein, in whole or in part, shall be strictly prohibited.
*/
/* MediaTek Inc. (C) 2014. All rights reserved.
*
* BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
* THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
* RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
* AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
* EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
* NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
* SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
* SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
* THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
* THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
* CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
* SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
* STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
* CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
* AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
* OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
* MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
*
* The following software/firmware and/or related documentation ("MediaTek Software")
* have been modified by MediaTek Inc. All revisions are subject to any receiver's
* applicable license agreements with MediaTek Inc.
*/

package com.mediatek.rcs.incallui.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.telecom.Call;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.mediatek.rcs.phone.R;
import com.mediatek.services.rcs.phone.ICallStatusService;
import com.mediatek.services.rcs.phone.IServicePresenterCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class CallStatusServicePresenter extends Handler
    implements CallStatusController.OnCallChangedListener {

    private static final String TAG = "CallStatusServicePresenter";

    private static final int ITEM_TYPE_NOTIFY = 1;
    private static final int ITEM_TYPE_REGISTER = 2;

    private static int STATUS_IDLE = 0;
    private static int STATUS_ACTIVE = 1;
    private static int STATUS_SELECT = 2;

    private int mStatus = STATUS_IDLE;

    private static final int STATE_NEW = 0;
    private static final int STATE_DIALING = 1;
    private static final int STATE_RINGING = 2;
    private static final int STATE_HOLDING = 3;
    private static final int STATE_ACTIVE = 4;
    private static final int STATE_DISCONNECTED = 7;
    private static final int STATE_PRE_DIAL_WAIT = 8;
    private static final int STATE_CONNECTING = 9;
    private static final int STATE_DISCONNECTING = 10;

    private static final int MESSAGE_DEALY_CLEAR = 100;
    private static final int MESSAGE_DEALY_RETRY = 101;
    private static final int MESSAGE_STATUS_UPDATE = 102;
    private static final int MESSAGE_CALL_UPDATE = 103;

    private static final int STATUS_ID_NAME = 1;
    private static final int STATUS_ID_STATUS = 2;
    private static final int STATUS_ID_TIME = 3;

    private static final int STATUS_ORIGIN_PRESENTER = 1;
    private static final int STATUS_ORIGIN_MESSAGE = 2;

    private TextView mUserName;
    private String   mUserString;
    private TextView mUserTime;
    private String   mUserStatus;

    private CallTimer mCalltimer;
    private String mCallIndex = "";

    private int mPreviousCallState;
    private int mCurrentCallState;

    private static final int MAX_RETRY_TIMES = 5;
    private static final int MESSAGE_DELAY_TIMES = 2000;
    private static final int STATUS_DELAY_TIME = 2000;
    private static final long EVERAGE_TIME_TICK = 1000;

    private final Object mServiceAndQueueLock = new Object();

    private ArrayList<QueueItem> mQueueList = new ArrayList<QueueItem>();
    private HashMap<String, Call> mCallMap = new HashMap<String, Call>();

    private ICallStatusService mCallStatusService;
    private ServiceConnection mConnection = null;

    private Context mContext;
    private int mRetryTimes;
    private boolean mIncallUIDestroy = true;

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch(msg.what) {
            case MESSAGE_DEALY_RETRY:
                handleRetryConnection();
                break;
            case MESSAGE_DEALY_CLEAR:
                if ((mStatus & STATUS_SELECT) == STATUS_SELECT) {
                    releaseResource();
                }
                break;
            case MESSAGE_STATUS_UPDATE:
                handleUpdateStatus(msg.arg1, msg.arg2);
                break;
            case MESSAGE_CALL_UPDATE:
                handleCallStatus((HashMap<String, Call>) msg.obj);
                break;
            default:
                break;
        }
    }

    public CallStatusServicePresenter(Context cxt) {
        mContext = cxt;
        //Need to check if this will cause memory leak???????
        mCalltimer = new CallTimer(new Runnable() {
            @Override
            public void run() {
                onTimeTicked();
            }
        });
    }

    private void handleRetryConnection() {
        Log.i(TAG, "handleRetryConnection");
        if (mRetryTimes < MAX_RETRY_TIMES) {
            mRetryTimes++;

            removeMessages(MESSAGE_DEALY_RETRY);

            if (mConnection != null || mIncallUIDestroy) {
                Log.i(TAG, "handleRetryConnection check unpass, just return!");
                return;
            }

            setupCallStatusServiceConnection();
        } else {
            Log.i(TAG, "handleRetryConnection retry time > 5.");
        }
    }

    private void handleClearStatus() {
        Log.i(TAG, "handleClearStatus");
        if ((mStatus & STATUS_SELECT) == STATUS_SELECT) {
            notifyStatusToClient("", "", "");
        }

        mCallIndex = "";
        mUserString = "";
        clearCurrentStatus();

        mPreviousCallState = STATE_NEW;
        mCurrentCallState = STATE_NEW;
    }

    private void handleUpdateStatus(int origin, int post) {
        Log.i(TAG, "handleUpdateStatus, origin = " + origin + ", post = " + post);
        if (origin == STATUS_ORIGIN_PRESENTER) {
            if (post == STATUS_IDLE) {
                mCallIndex = "";
                mCalltimer.stopTimeTick();
            }
        }

        if (origin == STATUS_ORIGIN_MESSAGE) {
            if (post == STATUS_IDLE) {
                mCalltimer.stopTimeTick();
                clearCurrentStatus();
            } else if (post == STATUS_SELECT) {
                mStatus = mStatus | post;
            } else {
                Log.i(TAG, "handleUpdateStatus, shouldn't go to here");
            }
        }

        if ((mStatus & STATUS_SELECT) == STATUS_SELECT && !TextUtils.isEmpty(mCallIndex)) {
            Call callItem = getCallById(mCallIndex);
            Log.i(TAG, "callItem id = " + mCallIndex);
            if (callItem != null) {
                mCurrentCallState = callItem.getState();
                Log.i(TAG, "end before set status,  mPreviousCallState = " + mPreviousCallState);
                if (mCurrentCallState == STATE_DIALING) {
                    createAndNotifyStatus(STATE_DIALING);
                } else if (mCurrentCallState == STATE_ACTIVE) {
                    mCalltimer.startTimeTick(EVERAGE_TIME_TICK);
                } else if (mCurrentCallState == STATE_HOLDING) {
                    mCalltimer.stopTimeTick();
                    createAndNotifyStatus(STATE_HOLDING);
                } else if (mCurrentCallState == STATE_DISCONNECTING) {
                    mCalltimer.stopTimeTick();
                    createAndNotifyStatus(STATE_DISCONNECTING);
                } else if (mCurrentCallState == STATE_DISCONNECTED) {
                    mCalltimer.stopTimeTick();
                    createAndNotifyStatus(STATE_DISCONNECTED);
                    sendMessageDelayed(obtainMessage(MESSAGE_DEALY_CLEAR), STATUS_DELAY_TIME);
                }
                mPreviousCallState = mCurrentCallState;
                Log.i(TAG, "end before set status,  mCurrentCallState = " + mCurrentCallState);
            }
        }
    }

    private void handleCallStatus(HashMap<String, Call> call) {
        //We need to check if activity is destroyed, if true just ignore the status change.
        //Or else will bind service again, and will always have a running service
        if (mIncallUIDestroy) {
            Log.i(TAG, "Already destroyed, just return");
            return;
        }

        mCallMap = call;

        registerCallback();

        if ((mStatus & STATUS_SELECT) == STATUS_SELECT && !TextUtils.isEmpty(mCallIndex)) {
            Log.i(TAG, "callItem id = " + mCallIndex);
            Call callItem = getCallById(mCallIndex);
            if (callItem != null) {
                mCurrentCallState = callItem.getState();
                Log.i(TAG, "end before set status,  mPreviousCallState = " + mPreviousCallState);
                if (mCurrentCallState != mPreviousCallState) {
                    if (mCurrentCallState == STATE_DIALING) {
                        createAndNotifyStatus(STATE_DIALING);
                    } else if (mCurrentCallState == STATE_ACTIVE) {
                        mCalltimer.startTimeTick(EVERAGE_TIME_TICK);
                    } else if (mCurrentCallState == STATE_HOLDING) {
                        createAndNotifyStatus(STATE_HOLDING);
                        mCalltimer.stopTimeTick();
                    } else if (mCurrentCallState == STATE_DISCONNECTING) {
                        createAndNotifyStatus(STATE_DISCONNECTING);
                        mCalltimer.stopTimeTick();
                    } else if (mCurrentCallState == STATE_DISCONNECTED) {
                        createAndNotifyStatus(STATE_DISCONNECTED);
                        mCalltimer.stopTimeTick();
                        sendMessageDelayed(obtainMessage(MESSAGE_DEALY_CLEAR), STATUS_DELAY_TIME);
                    }
                }
                mPreviousCallState = mCurrentCallState;
                Log.i(TAG, "end before set status,  mCurrentCallState = " + mCurrentCallState);
            }
        }
    }

    @Override
    public void onViewSetup(HashMap<Integer, TextView> map) {
        Log.i(TAG, "onViewSetup.");
        mIncallUIDestroy = false;

        Iterator iterator = map.keySet().iterator();
        while (iterator.hasNext()) {
            Integer integer = (Integer) iterator.next();
            TextView view = (TextView) map.get(integer);
            int value = integer.intValue();
            Log.i(TAG, "value = " + value);

            if (value == STATUS_ID_NAME) {
                mUserName = view;
            } else if (value == STATUS_ID_TIME) {
                mUserTime = view;
            } else {
                Log.d(TAG, "Not any valid view id");
            }
        }
        //registerCallback();
    }

    @Override
    public void onMenuItemSelected(Call call) {
        Log.i(TAG, "onMenuItemSelected");
        if (call != null) {
            mCallIndex = call.getCallId();
            mUserString = "";
            clearCurrentStatus();

            mPreviousCallState = STATE_NEW;
            mCurrentCallState = STATE_NEW;
        } else {
            Log.i(TAG, "Not any call exists, delay notify!");
            sendMessageDelayed(obtainMessage(MESSAGE_DEALY_CLEAR), STATUS_DELAY_TIME);
        }
    }

    @Override
    public void onCallStatusChange(HashMap<String, Call> call) {
        //Log.i(TAG, "onCallStatusChange.");
        Message msg = obtainMessage(MESSAGE_CALL_UPDATE, call);
        sendMessage(msg);
    }

    @Override
    public void onVanish() {
        Log.i(TAG, "onVanish.");
        mIncallUIDestroy = true;
        if ((mStatus & STATUS_SELECT) != STATUS_SELECT) {
            releaseResource();
        }

        //Fix leak problem, we need to set these as null
        mUserName = null;
        mUserTime = null;
    }

    private void releaseResource() {
        synchronized (mServiceAndQueueLock) {
            if (mCallStatusService != null) {
                handleClearStatus();
                unregisterCallback();
                mContext.unbindService(mConnection);
                mCallStatusService = null;
            }
            mConnection = null;
        }
    }

    private int getCallCount() {
        int count = 0;
        for (Call call : mCallMap.values()) {
            count++;
        }
        return count;
    }

    private Call getCallById(String index) {
        for (Call call : mCallMap.values()) {
            String id = call.getCallId();
            if (id.equals(index)) {
                return call;
            }
        }
        return null;
    }

    private class CallStatusServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "onServiceConnected.");
            if (mCallStatusService != null) {
                Log.d(TAG, "Service alreay connected, service = " + mCallStatusService);
                return;
            }
            onCallStatusServiceConnected(ICallStatusService.Stub.asInterface(service));
        }

        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "onServiceDisconnected.");
            if (mCallStatusService != null) {
                unregisterCallback();
                mContext.unbindService(mConnection);
                mCallStatusService = null;
            }
            mConnection = null;
        }
    }

    private void onCallStatusServiceConnected(ICallStatusService callstatusService) {
        synchronized (mServiceAndQueueLock) {
            Log.d(TAG, "onCallStatusServiceConnected.");
            mCallStatusService = callstatusService;
            deQueueMessage();
        }
    }

    private Intent getCallStatusServiceIntent() {
        final Intent intent = new Intent(ICallStatusService.class.getName());
        final ComponentName component = new ComponentName("com.mediatek.rcs.phone",
                                    "com.mediatek.rcs.incallui.service.CallStatusService");
        intent.setComponent(component);
        return intent;
    }

    private void setupCallStatusServiceConnection() {
        Log.i(TAG, "setupCallStatusServiceConnection.");
        synchronized (mServiceAndQueueLock) {
            if (mCallStatusService == null || mConnection == null) {
                mConnection = new CallStatusServiceConnection();
                boolean failedConnection = false;

                Intent intent = getCallStatusServiceIntent();
                if (!mContext.bindService(intent, mConnection,
                        Context.BIND_AUTO_CREATE)) {
                    Log.d(TAG, "Bind service failed!");
                    mConnection = null;
                    failedConnection = true;
                }

                if (failedConnection) {
                    sendRetryConnectionRequest();
                }
            } else {
                Log.d(TAG, "Already bind service!");
            }
        }
    }

    private void sendRetryConnectionRequest() {
        Log.d(TAG, "sendRetryConnectionRequest.");
        Message msg = obtainMessage(MESSAGE_DEALY_RETRY);
        sendMessageDelayed(msg, MESSAGE_DELAY_TIMES);
    }

    private void createAndNotifyStatus(int status) {
        Log.i(TAG, "createAndNotifyStatus, status = " + status);
        if (TextUtils.isEmpty(mUserString) || getCallCount() == 1) {
            if (mUserName != null) {
                mUserString = mUserName.getText().toString();
            } else {
                mUserString = "";
            }
        }

        String time = "";
        switch(status) {
            case STATE_DIALING:
                mUserStatus = mContext.getString(R.string.call_status_dialing);
                break;
            case STATE_ACTIVE:
                mUserStatus = mContext.getString(R.string.call_status_active);
                if (mUserTime != null) {
                    time = mUserTime.getText().toString();
                } else {
                    time = "";
                }
                break;
            case STATE_HOLDING:
                mUserStatus = mContext.getString(R.string.call_status_holding);
                break;
            case STATE_DISCONNECTING:
                mUserStatus = mContext.getString(R.string.call_status_disconnecting);
                break;
            case STATE_DISCONNECTED:
                mUserStatus = mContext.getString(R.string.call_status_disconnected);
                break;
            default:
                break;
        }

        notifyStatusToClient(mUserString, mUserStatus, time);
    }

    private void notifyStatusToClient(String name, String status, String time) {
        try {
            synchronized (mServiceAndQueueLock) {
                HashMap<Integer, String> map = new HashMap<Integer, String>();
                map.put(new Integer(1), name);
                map.put(new Integer(2), status);
                map.put(new Integer(3), time);
                if (mCallStatusService == null || mConnection == null) {
                    QueueItem item = new QueueItem(ITEM_TYPE_NOTIFY, map);
                    enQueueMessage(item);
                    setupCallStatusServiceConnection();
                } else {
                    mCallStatusService.notifyToClient(name, status, time);
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void registerCallback() {
        synchronized (mServiceAndQueueLock) {
            if (getQueueCount() > 0) {
                Log.d(TAG, "Queue count is not null!");
                return;
            }

            if (mCallStatusService == null || mConnection == null) {
                QueueItem item = new QueueItem(ITEM_TYPE_REGISTER, null);
                enQueueMessage(item);
                setupCallStatusServiceConnection();
            } else {
                //Log.d(TAG, "Already registered!");
            }
        }
    }

    private void unregisterCallback() {
        Log.d(TAG, "unregisterCallback.");
        try {
            synchronized (mServiceAndQueueLock) {
                if (mCallStatusService != null) {
                    mCallStatusService.unregisterPresenterCallback(mServicePresenterCallback);
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void onTimeTicked() {
        Log.i(TAG, "onTimeTicked");
        if ((mStatus & STATUS_SELECT) == STATUS_SELECT && !TextUtils.isEmpty(mCallIndex)) {
            Call call = getCallById(mCallIndex);
            if (call != null) {
                createAndNotifyStatus(call.getState());
            }
        }
    }

    private void updateCurrentStatus(int origin, int status) {
        Message msg = obtainMessage(MESSAGE_STATUS_UPDATE, origin, status);
        sendMessage(msg);
    }

    private void clearCurrentStatus() {
        mStatus = mStatus & 0;
    }

    private final IServicePresenterCallback.Stub mServicePresenterCallback =
                                            new IServicePresenterCallback.Stub() {
        @Override
        public void setCurrentStatus(int origin, int status) {
            try {
                Log.i(TAG, "setCurrentStatus");
                updateCurrentStatus(origin, status);
            } catch (Exception e) {
                Log.e(TAG, "Error setCurrentStatus", e);
            }
        }
    };

    private int getQueueCount() {
        int count = 0;
        for (QueueItem item : mQueueList) {
            count++;
        }
        return count;
    }

    private void enQueueMessage(QueueItem item) {
        Log.i(TAG, "enQueueMessage, type = " + item.queueType);
        mQueueList.add(item);
    }

    private void deQueueMessage() {
        for (QueueItem item : mQueueList) {
            Log.i(TAG, "deQueueMessage, type = " + item.queueType);
            switch(item.queueType) {
                case ITEM_TYPE_NOTIFY:
                    processNotifyItem(item.object);
                    break;
                case ITEM_TYPE_REGISTER:
                    processRegisterItem();
                    break;
                default:
                    break;
            }
        }
        mQueueList.clear();
    }

    private void processRegisterItem() {
      Log.d(TAG, "processRegisterItem.");
        try {
            synchronized (mServiceAndQueueLock) {
                if (mCallStatusService != null) {
                    mCallStatusService.registerPresenterCallback(
                                                    mServicePresenterCallback);
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void processNotifyItem(Object obj) {
        Log.d(TAG, "processViewItem.");
        try {
            synchronized (mServiceAndQueueLock) {
                if (mCallStatusService != null) {
                    String name = "";
                    String status = "";
                    String time = "";
                    HashMap<Integer, String> map = (HashMap<Integer, String>) obj;
                    Iterator iterator = map.keySet().iterator();
                    while (iterator.hasNext()) {
                        Integer integer = (Integer) iterator.next();
                        String string = (String) map.get(integer);
                        int value = integer.intValue();
                        Log.i(TAG, "value = " + value);

                        if (value == STATUS_ID_NAME) {
                            name = string;
                        } else if (value == STATUS_ID_STATUS) {
                            status = string;
                        } else if (value == STATUS_ID_TIME) {
                            time = string;
                        } else {
                            Log.d(TAG, "Not any valid view id");
                        }
                    }
                    mCallStatusService.notifyToClient(name, status, time);
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private class QueueItem {
        int     queueType;
        Object  object;
        QueueItem(int type, Object obj) {
            object = obj;
            queueType = type;
        }
    }
}