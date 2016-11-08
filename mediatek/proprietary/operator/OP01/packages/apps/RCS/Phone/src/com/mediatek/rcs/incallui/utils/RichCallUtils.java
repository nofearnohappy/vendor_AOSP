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

package com.mediatek.rcs.incallui.utils;

import android.content.Context;
import android.telecom.Call;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.rcs.incallui.RichCallAdapter;
import com.mediatek.rcs.incallui.RichCallAdapter.RichCallInfo;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class RichCallUtils implements RichCallAdapter.Listener {

    private static final String TAG = "RichCallUtils";

    private HashMap<String, RichCallInfo> mHashMapInfo =
                    new HashMap<String, RichCallInfo>();

    private HashMap<String, Call> mHashCall =
                    new HashMap<String, Call>();

    private HashMap<String, Set<CallInfoFetchedListener>> mCallfetchCallbacks =
                    new HashMap<String, Set<CallInfoFetchedListener>>();

    private HashMap<String, Set<CallInfoUpdatedListener>> mCallUpdateCallbacks =
                    new HashMap<String, Set<CallInfoUpdatedListener>>();

    private Set<GeoInfoUptatedListener> mGeoUpdateCallbacks = Collections.newSetFromMap(
                    new ConcurrentHashMap<GeoInfoUptatedListener, Boolean>(3, 0.9f, 1));

    private Set<SysLoginListener> mSysLoginCallbacks = Collections.newSetFromMap(
                    new ConcurrentHashMap<SysLoginListener, Boolean>(3, 0.9f, 1));
    //Login status
    public static final int LOGIN_INIT = 0;
    public static final int LOGIN_PROCESSING = 1;
    public static final int LOGIN_FAILED = 2;
    public static final int LOGIN_SUCCESS = 3;

    //Adapter for vendor sdk func interface
    private RichCallAdapter mRichCallAdapter;

    public RichCallUtils() {
        mRichCallAdapter = new RichCallAdapter();
    }

    public interface CallInfoFetchedListener {
        public void onRichCallInfoFetched(Call call, String str,
                                            RichCallInfo info, boolean result);
    }
    public interface CallInfoUpdatedListener {
        public void onRichCallInfoUpdated(String str, boolean succeed);
    }
    public interface GeoInfoUptatedListener {
        public void onRichCallGeoUpdated(boolean succeed);
    }
    public interface SysLoginListener {
        public void onRichCallSyncLogin(int status);
    }

    public boolean isCallInfoCached(String number) {
        //Maybe should added for SRVCC
        if (!TextUtils.isEmpty(number)) {
            return mHashMapInfo.containsKey(number);
        }
        return false;
    }

    public boolean isCallInfoQuring(String number) {
        if (!TextUtils.isEmpty(number)) {
            return mCallfetchCallbacks.containsKey(number);
        }
        return false;
    }

    public void initRichCallSystem(Context cnx) {
        Log.d(TAG, "initRichCallSystem");
        //cacheSize is memory size, it should more than 5
        int cacheSize = 8;
        mRichCallAdapter.initRichCallSystem(cacheSize, cnx);
    }

    public void loginRichCallSystem(Context cnx, SysLoginListener listener) {
        Log.d(TAG, "loginRichCallSystem");
        mSysLoginCallbacks.add(listener);
        for (SysLoginListener callback : mSysLoginCallbacks) {
            callback.onRichCallSyncLogin(LOGIN_PROCESSING);
        }

        mRichCallAdapter.loginRichCallSystem(cnx, this);
    }

    public void updatedRichCallInfo(String number,
            String status, CallInfoUpdatedListener callback) {

        Set<CallInfoUpdatedListener> callbacks =
                new CopyOnWriteArraySet<CallInfoUpdatedListener>();
        callbacks.add(callback);
        mCallUpdateCallbacks.put(number, callbacks);

        mRichCallAdapter.updatedRichCallInfo(number, status, this);
    }

    public void getRichCallInfo(Call call,
            String number, String event, CallInfoFetchedListener callback) {
        //Maybe need add for SRVCC
        RichCallInfo info = mHashMapInfo.get(number);
        if (info != null) {
            callback.onRichCallInfoFetched(call, number, info, true);
            return;
        }

        mHashCall.put(number, call);
        Set<CallInfoFetchedListener> callbacks = new CopyOnWriteArraySet<CallInfoFetchedListener>();
        callbacks.add(callback);
        mCallfetchCallbacks.put(number, callbacks);

        mRichCallAdapter.getRichCallInfo(number, event, this);
    }

    public void updateGeoInfo(String event, GeoInfoUptatedListener callback) {
        mGeoUpdateCallbacks.add(callback);
        mRichCallAdapter.updateGeoInfo(event, this);
    }

    public void notifyDataConnectionChange() {
        mRichCallAdapter.notifyDataConnectionChange();
    }

    public void releaseAdapterResource() {
        mRichCallAdapter.releaseAdapterResource();
    }

    @Override
    public void onRichCallInfoFetched(String str, RichCallInfo info, boolean result) {
        Log.d(TAG, "onRichCallInfoFetched, result = " + result);
        if (info != null) {
            mHashMapInfo.put(str, info);
            //Maybe need add for SRVCC
            Call call = mHashCall.get(str);
            final Set<CallInfoFetchedListener> callbacks = mCallfetchCallbacks.get(str);
            if (callbacks != null) {
                for (CallInfoFetchedListener callback : callbacks) {
                    callback.onRichCallInfoFetched(call, str, info, result);
                }
                callbacks.clear();
                mCallfetchCallbacks.remove(str);
            }
        }
    }

    @Override
    public void onRichCallInfoUpdated(String str, boolean succeed) {
        Log.d(TAG, "onRichCallInfoUpdated, succeed = " + succeed);
        final Set<CallInfoUpdatedListener> callbacks = mCallUpdateCallbacks.get(str);
        if (callbacks != null) {
            for (CallInfoUpdatedListener callback : callbacks) {
                callback.onRichCallInfoUpdated(str, succeed);
            }
            callbacks.clear();
            mCallUpdateCallbacks.remove(str);
        }
    }

    @Override
    public void onRichCallGeoUpdated(boolean succeed) {
        Log.d(TAG, "onRichCallGeoUpdated, succeed = " + succeed);
        for (GeoInfoUptatedListener callback : mGeoUpdateCallbacks) {
            callback.onRichCallGeoUpdated(succeed);
        }
        mGeoUpdateCallbacks.clear();
    }

    @Override
    public void onRichCallSyncLogin(boolean result) {
        Log.d(TAG, "onRichCallSyncLogin, result = " + result);
        for (SysLoginListener callback : mSysLoginCallbacks) {
            if (result) {
                callback.onRichCallSyncLogin(LOGIN_SUCCESS);
            } else {
                callback.onRichCallSyncLogin(LOGIN_FAILED);
            }
        }
        mSysLoginCallbacks.clear();
    }

    public void clearRichCallInfo(String number) {
        mHashMapInfo.remove(number);
        mHashCall.remove(number);
    }

    public void clearRichCallInfos() {
        mHashMapInfo.clear();
        mHashCall.clear();
    }

    public void clearCallbacks() {
        Log.d(TAG, "clearCallbacks");

        Iterator iter = mCallfetchCallbacks.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String str = (String) entry.getKey();
            Set<CallInfoFetchedListener> set = (Set<CallInfoFetchedListener>) entry.getValue();
            set.clear();
            Log.d(TAG, "clear CallInfoFetchedListener~");
        }
        mCallfetchCallbacks.clear();

        iter = mCallUpdateCallbacks.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String str = (String) entry.getKey();
            Set<CallInfoUpdatedListener> set = (Set<CallInfoUpdatedListener>) entry.getValue();
            set.clear();
            Log.d(TAG, "clear callUpdateCallbacks~");
        }
        mCallUpdateCallbacks.clear();

        mGeoUpdateCallbacks.clear();
        mSysLoginCallbacks.clear();
    }
}
