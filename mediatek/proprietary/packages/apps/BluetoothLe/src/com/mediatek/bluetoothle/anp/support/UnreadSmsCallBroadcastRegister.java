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

package com.mediatek.bluetoothle.anp.support;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

public class UnreadSmsCallBroadcastRegister {

    public interface AlertChangeListener {
        void onAlertChanged(int number);
    }

    private static final String TAG = "[BluetoothAns]UnreadSmsCallBroadcastRegister";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;
    private HashMap<ComponentName, AlertChangeListener> mAlertListenerMap =
            new HashMap<ComponentName, AlertChangeListener>();
    private static UnreadSmsCallBroadcastRegister sRegister;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_UNREAD_CHANGED.equals(action)) {
                ComponentName currentName = (ComponentName) intent
                        .getExtra(Intent.EXTRA_UNREAD_COMPONENT);
                if (currentName == null) {
                    return;
                }
                if (DBG) {
                    Log.d(TAG, "onReceive(), componentName = " + currentName);
                }
                Iterator<Entry<ComponentName, AlertChangeListener>> interator =
                        mAlertListenerMap.entrySet().iterator();
                ComponentName componentName;
                while (interator.hasNext()) {
                    Entry<ComponentName, AlertChangeListener> entry = interator.next();
                    componentName = entry.getKey();
                    if (DBG) {
                        Log.d(TAG, "onReceive(), Name = " + componentName);
                    }
                    if (componentName != null && componentName.equals(currentName)) {
                        int number = intent.getIntExtra(Intent.EXTRA_UNREAD_NUMBER, -1);
                        if (DBG) {
                            Log.d(TAG, "onReceive(), number = " + number);
                        }
                        if (number != -1) {
                            entry.getValue().onAlertChanged(number);
                            // consider there is no 2 detector use the same componentName, so return
                            // it
                            return;
                        }
                    }
                }
            }
        }
    };

    private UnreadSmsCallBroadcastRegister() {
    }

    public static UnreadSmsCallBroadcastRegister getInstance() {
        if (sRegister == null) {
            sRegister = new UnreadSmsCallBroadcastRegister();
        }
        return sRegister;
    }

    public void clearAll(Context context) {
        unRegisterReceiver(context);
        sRegister = null;
    }

    public void registerAlertChangeListener(Context context, ComponentName name,
            AlertChangeListener listener) {
        if (name == null || listener == null) {
            Log.e(TAG, "registerAlertChangeListener error" +
                    ", ComponentName name = " + name + "listener==null? " + (listener == null));
            return;
        }
        Log.d(TAG, "registerAlertChangeListener " + ", ComponentName name = " + name);
        if (mAlertListenerMap.size() == 0) {
            if (DBG) {
                Log.d(TAG, "addAlertChangeListener initialize");
            }
            initialize(context);
        }
        if (mAlertListenerMap.get(name) != null) {
            Log.e(TAG, "addAlertChangeListener error, alreadInMap = "
                            + mAlertListenerMap.get(name));
            return;
        } else {
            synchronized (mAlertListenerMap) {
                mAlertListenerMap.put(name, listener);
            }
        }
    }

    public void removeAlertChangeListener(Context context, ComponentName name) {
        synchronized (mAlertListenerMap) {
            if (DBG) {
                Log.d(TAG, "removeAlertChangeListener " + name);
            }
            mAlertListenerMap.remove(name);
            if (mAlertListenerMap.isEmpty()) {
                Log.i(TAG, "removeAlertChangeListener, clearAll");
                clearAll(context);
            }
        }
    }

    private void initialize(Context context) {
        registerReceiver(context);
    }

    private void registerReceiver(Context context) {
        IntentFilter filter = new IntentFilter(Intent.ACTION_UNREAD_CHANGED);
        context.registerReceiver(mReceiver, filter);
    }

    private void unRegisterReceiver(Context context) {
        context.unregisterReceiver(mReceiver);
    }

}
