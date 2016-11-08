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

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.services.rcs.phone.ICallStatusService;
import com.mediatek.services.rcs.phone.IServiceMessageCallback;
import com.mediatek.services.rcs.phone.IServicePresenterCallback;

import java.util.HashMap;
import java.util.Iterator;

public class CallStatusService extends Service {
    private static final String TAG = "CallStatusService";

    private static final int STATUS_ID_NAME = 1;
    private static final int STATUS_ID_STATUS = 2;
    private static final int STATUS_ID_TIME = 3;

    private static final int STATUS_IDLE = 0;
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_SELECT = 2;

    private static final int STATUS_START_NOTIFY = 1;
    private static final int STATUS_MESSAGE_REG_CALLBACK = 2;
    private static final int STATUS_PRESENTER_REG_CALLBACK = 3;
    private static final int STATUS_MESSAGE_UNREG_CALLBACK = 4;
    private static final int STATUS_PRESENTER_UNREG_CALLBACK = 5;

    private static final int STATUS_ORIGIN_PRESENTER = 1;
    private static final int STATUS_ORIGIN_MESSAGE = 2;

    private MainHandler mHandler;

    private final RemoteServiceCallbackList<IServiceMessageCallback> mMessageCallback =
                                        new RemoteServiceCallbackList<IServiceMessageCallback>();
    private final RemoteServiceCallbackList<IServicePresenterCallback> mPresenterCallback =
                                        new RemoteServiceCallbackList<IServicePresenterCallback>();

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind");
        return false;
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
        super.onCreate();

        if (mHandler == null) {
            mHandler = new MainHandler();
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();

        mMessageCallback.kill();
        mPresenterCallback.kill();
        mHandler = null;
    }

    private final ICallStatusService.Stub mBinder = new ICallStatusService.Stub() {

        @Override
        public void notifyToClient(String name, String status, String time) {
            try {
                HashMap<Integer, String> map = new HashMap<Integer, String>();
                map.put(new Integer(1), name);
                map.put(new Integer(2), status);
                map.put(new Integer(3), time);
                Log.i(TAG, "notifyToClient");
                mHandler.sendMessage(mHandler.obtainMessage(
                                                    STATUS_START_NOTIFY, map));
            } catch (Exception e) {
                Log.e(TAG, "Error notifyToClient", e);
            }
        }

        @Override
        public void registerMessageCallback(IServiceMessageCallback callback) {
            try {
                Log.i(TAG, "registerMessageCallback, callback = "
                                                        + callback.toString());
                mHandler.sendMessage(mHandler.obtainMessage(
                                        STATUS_MESSAGE_REG_CALLBACK, callback));
            } catch (Exception e) {
                Log.e(TAG, "Error registerMessageCallback", e);
            }
        }

        @Override
        public void registerPresenterCallback(IServicePresenterCallback callback) {
            try {
                Log.i(TAG, "registerPresenterCallback, callback = " +
                                                            callback.toString());
                mHandler.sendMessage(mHandler.obtainMessage(
                                        STATUS_PRESENTER_REG_CALLBACK, callback));
            } catch (Exception e) {
                Log.e(TAG, "Error registerPresenterCallback", e);
            }
        }

        @Override
        public void unregisterMessageCallback(IServiceMessageCallback callback) {
            try {
                Log.i(TAG, "unregisterMessageCallback, callback = "
                                                        + callback.toString());
                mHandler.sendMessage(mHandler.obtainMessage(
                                            STATUS_MESSAGE_UNREG_CALLBACK, callback));
            } catch (Exception e) {
                Log.e(TAG, "Error unregisterClientCallback", e);
            }
        }

        @Override
        public void unregisterPresenterCallback(IServicePresenterCallback callback) {
            try {
                Log.i(TAG, "unregisterPresenterCallback, callback = "
                                                                + callback.toString());
                mHandler.sendMessage(mHandler.obtainMessage(
                                            STATUS_PRESENTER_UNREG_CALLBACK, callback));
            } catch (Exception e) {
                Log.e(TAG, "Error unregisterClientCallback", e);
            }
        }
    };

    private class RemoteServiceCallbackList <E extends IInterface>
                                            extends RemoteCallbackList<E> {
        @Override
        public void onCallbackDied(E callback) {
            Log.d(TAG, "onCallbackDied");
            super.onCallbackDied(callback);
        }

        @Override
        public void onCallbackDied(E callback, Object cookie) {
            Log.d(TAG, "onCallbackDied");
            super.onCallbackDied(callback, cookie);
        }
    }

    private class MainHandler extends Handler {
        MainHandler() {
            super(getApplicationContext().getMainLooper(), null, true);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "handleMessage:" + msg);
            executeMessage(msg);
        }
    }

    private void executeMessage(Message msg) {
        switch(msg.what) {
            case STATUS_START_NOTIFY:
                notifyStatusToClient((HashMap<Integer, String>) msg.obj);
                break;
            case STATUS_MESSAGE_REG_CALLBACK:
                onMessageCallbackRegister((IServiceMessageCallback) msg.obj);
                break;
            case STATUS_MESSAGE_UNREG_CALLBACK:
                onMessageCallbackUnregister((IServiceMessageCallback) msg.obj);
                break;
            case STATUS_PRESENTER_REG_CALLBACK:
                onPresenterCallbackRegister((IServicePresenterCallback) msg.obj);
                break;
            case STATUS_PRESENTER_UNREG_CALLBACK:
                onPresenterCallbackUnregister((IServicePresenterCallback) msg.obj);
                break;
            default:
                break;
        }
    }

    private void notifyStatusToClient(HashMap<Integer, String> map) {
        String name = "";
        String status = "";
        String time = "";

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

        Log.i(TAG, "notifyStatusToClient, name = " + name +
                                        " , status = " + status + " , time = " + time);
        int callbackNum = mMessageCallback.beginBroadcast();
        for (int i = 0; i < callbackNum; i++) {
            try {
                Log.i(TAG, "getBroadcastItem, index = " + i);
                mMessageCallback.getBroadcastItem(i).updateMsgStatus(name, status, time);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        mMessageCallback.finishBroadcast();

        if (TextUtils.isEmpty(name) && TextUtils.isEmpty(status) &&
                TextUtils.isEmpty(time)) {
            notifyClientToStop();
        }
    }

    private void notifyClientToStop() {
        Log.i(TAG, "notifyClientToStop");

        int callbackNum = mMessageCallback.beginBroadcast();
        for (int i = 0; i < callbackNum; i++) {
            try {
                Log.i(TAG, "notifyClientToStop, index = " + i);
                mMessageCallback.getBroadcastItem(i).stopfromClient();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        mMessageCallback.finishBroadcast();
    }

    private void setClientStatus(int origin, int status) {
        Log.i(TAG, "setClientStatus, status = " + status);

        int callbackNum = mPresenterCallback.beginBroadcast();
        for (int i = 0; i < callbackNum; i++) {
            try {
                Log.i(TAG, "setClientStatus, index = " + i);
                mPresenterCallback.getBroadcastItem(i).setCurrentStatus(origin, status);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        mPresenterCallback.finishBroadcast();
    }

    private void onPresenterCallbackRegister(IServicePresenterCallback cb) {
        Log.i(TAG, "onPresenterCallbackRegister");

        if (cb != null) {
            mPresenterCallback.register(cb);
        }
    }

    private void onPresenterCallbackUnregister(IServicePresenterCallback cb) {
        Log.i(TAG, "onPresenterCallbackUnregister");

        if (cb != null) {
            mPresenterCallback.unregister(cb);
        }
        setClientStatus(STATUS_ORIGIN_PRESENTER, STATUS_IDLE);
    }

    private void onMessageCallbackRegister(IServiceMessageCallback cb) {
        Log.i(TAG, "onMessageCallbackRegister");

        if (cb != null) {
            mMessageCallback.register(cb);
        }
        setClientStatus(STATUS_ORIGIN_MESSAGE, STATUS_SELECT);
    }

    private void onMessageCallbackUnregister(IServiceMessageCallback cb) {
        Log.i(TAG, "onMessageCallbackUnregister");

        if (cb != null) {
            mMessageCallback.unregister(cb);
        }
        setClientStatus(STATUS_ORIGIN_MESSAGE, STATUS_IDLE);
    }
}
