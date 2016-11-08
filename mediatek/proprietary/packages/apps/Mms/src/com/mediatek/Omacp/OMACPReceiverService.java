/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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

/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.omacp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.preference.PreferenceManager;
import android.util.Log;


/** M:
 * This service essentially plays the role of a "worker thread", allowing us to update
 * the configuration. without blocking the main thread that SmsReceiver runs on.
 */
public class OMACPReceiverService extends Service {
    private static final String TAG = "OmacpMessageReceiverService";
    private ServiceHandler mServiceHandler;
    private Looper mServiceLooper;
    public static final String APP_ID_KEY = "appId";
    public static final String MMS_ID = "w4";
    private static final String MMS_SETTING_ACTION = "com.mediatek.omacp.settings";
    private static final String MMS_CAPABILITY_ACTION = "com.mediatek.omacp.capability";
    private static final String APP_SETTING_RESULT_ACTION = "com.mediatek.omacp.settings.result";
    private static final String MMS_CAPABILITY_RESULT_ACTION
            = "com.mediatek.omacp.capability.result";
    @Override
    public void onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.
        Log.i(TAG, "OmacpMessageReceiverService onCreate");
        HandlerThread thread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        mServiceLooper = thread.getLooper();
        if (mServiceLooper != null) {
            mServiceHandler = new ServiceHandler(mServiceLooper);
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "OmacpMessageReceiverService onStartCommand");
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        mServiceHandler.sendMessage(msg);
        Log.i(TAG, "OmacpMessageReceiverService sendMessage: " + msg);
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        mServiceLooper.quit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        /**
         * Handle incoming configure requests.
         */
        @Override
        public void handleMessage(Message msg) {
            int serviceId = msg.arg1;
            Intent intent = (Intent) msg.obj;
            Log.i(TAG, "OmacpMessageReceiverService got intent: " + intent);
            if (intent != null) {
                String action = intent.getAction();
                if (action.equals(MMS_SETTING_ACTION)) {
                    handleOMACPSettingReceived(intent);
                } else if (action.equals(MMS_CAPABILITY_ACTION)) {
                    handleOMACPCapabilityReceived(intent);
                }
            }
            // NOTE: We MUST not call stopSelf() directly, since we need to
            // make sure the wake lock acquired by AlertReceiver is released.
            OMACPReceiver.finishStartingService(OMACPReceiverService.this, serviceId);
        }
    }

    private void handleOMACPSettingReceived(Intent intent) {
        // actually process configuration
        Context context = getBaseContext();
        if (null == context) {
            Log.i(TAG, "OmacpMessageReceiverService return");
            return;
        }
        SharedPreferences.Editor editor = PreferenceManager
            .getDefaultSharedPreferences(context)
            .edit();
        if (null != editor) {
            // parse and configuration
            String cmSetting = intent.getStringExtra("CM");
            if (null != cmSetting) {
                if (cmSetting.equals("R")) {
                    editor.putString("pref_key_mms_creation_mode", "RESTRICTED");
                } else if (cmSetting.equals("W")) {
                    editor.putString("pref_key_mms_creation_mode", "WARNING");
                } else if (cmSetting.equals("F")) {
                    editor.putString("pref_key_mms_creation_mode", "FREE");
                }

            }
            //editor.putString("pref_mms_creation_mode_values",hM.get("rm"));
            //editor.putString("pref_mms_creation_mode_values",hM.get("ms"));
            //editor.putString("pref_mms_creation_mode_values",hM.get("pc_addr"));
            //editor.putString("pref_mms_creation_mode_values",hM.get("ma"));
            if (editor.commit()) {
                Intent resultIntent = new Intent();
                resultIntent.setAction(APP_SETTING_RESULT_ACTION);
                resultIntent.putExtra(APP_ID_KEY, MMS_ID);
                resultIntent.putExtra("result", true);
                context.sendBroadcast(resultIntent);
                Log.i(TAG, "OmacpMessageReceiverService send intent: " + intent);
            }
        }
    }

    private void handleOMACPCapabilityReceived(Intent intent) {
        Intent resultIntent = new Intent();
        resultIntent.setAction(MMS_CAPABILITY_RESULT_ACTION);
        resultIntent.putExtra(APP_ID_KEY, MMS_ID);
        resultIntent.putExtra("mms", true);
        resultIntent.putExtra("mms_mmsc_name", false);
        resultIntent.putExtra("mms_to_proxy", false);
        resultIntent.putExtra("mms_to_napid", false);
        resultIntent.putExtra("mms_mmsc", false);
        resultIntent.putExtra("mms_cm", true);
        resultIntent.putExtra("mms_rm", false);
        resultIntent.putExtra("mms_ms", false);
        resultIntent.putExtra("mms_pc_addr", false);
        resultIntent.putExtra("mms_ma", false);
        Context context = getBaseContext();
        if (null == context) {
            Log.i(TAG, "OmacpMessageReceiverService return");
            return;
        }
        context.sendBroadcast(resultIntent);
        Log.i(TAG, "OmacpMessageReceiverService send intent: " + resultIntent);
    }
}
