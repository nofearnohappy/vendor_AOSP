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
package com.mediatek.voicecommand.mgr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;

import com.mediatek.common.voicecommand.VoiceCommandListener;
import com.mediatek.voicecommand.adapter.IVoiceAdapter;
import com.mediatek.voicecommand.adapter.JNICommandAdapter;
import com.mediatek.voicecommand.business.BootCompletedReceiver;
import com.mediatek.voicecommand.business.VoiceCommandBusiness;
import com.mediatek.voicecommand.business.VoiceContactsBusiness;
import com.mediatek.voicecommand.business.VoiceRecognizeBusiness;
import com.mediatek.voicecommand.business.VoiceServiceInternalBusiness;
import com.mediatek.voicecommand.business.VoiceTrainingBusiness;
import com.mediatek.voicecommand.business.VoiceUiBusiness;
import com.mediatek.voicecommand.business.VoiceWakeupBusiness;
import com.mediatek.voicecommand.service.VoiceCommandManagerStub;
import com.mediatek.voicecommand.util.Log;

public class NativeDataManager extends VoiceDataManager implements IMessageDispatcher {
    private static final String TAG = "NativeDataManager";

    private Context mContext;
    private IVoiceAdapter mIJniVoiceAdapter;
    private IMessageDispatcher mIUpMsgDispatcher;
    private BootCompletedReceiver mBootCompletedReceiver;

    private VoiceCommandBusiness mVoiceUI;
    private VoiceCommandBusiness mVoiceTraining;
    private VoiceCommandBusiness mVoiceRecognize;
    // Used to deal with the logic happened in the Service itself
    private VoiceCommandBusiness mVoiceServiceInternal;
    private VoiceCommandBusiness mVoiceContacts;
    private VoiceCommandBusiness mVoiceWakeup;

    private boolean mIsBootReciverRegister = false;
    private static final String PROP_SYS_BOOT_COMPLETED = "sys.boot_completed";
    private static final String SYS_BOOT_COMPLETED = "1";
    
    //TODO:
    private static final int DELAY_MILLIS = 5000;

    public NativeDataManager(VoiceCommandManagerStub service) {
        super(service);
        Log.i(TAG, "[NativeDataManager]new ... ");

        mContext = service.mContext;
        mIJniVoiceAdapter = new JNICommandAdapter(this);

        mVoiceUI = new VoiceUiBusiness(this, service.mConfigManager, mHandler, mIJniVoiceAdapter);
        mVoiceTraining = new VoiceTrainingBusiness(this, service.mConfigManager, mHandler,
                mIJniVoiceAdapter);
        mVoiceRecognize = new VoiceRecognizeBusiness(this, service.mConfigManager, mHandler,
                mIJniVoiceAdapter);
        mVoiceContacts = new VoiceContactsBusiness(this, service.mConfigManager, mHandler,
                mIJniVoiceAdapter, mContext);
        mVoiceWakeup = new VoiceWakeupBusiness(this, service.mConfigManager, mHandler, mIJniVoiceAdapter,
                mContext);
        mVoiceServiceInternal = new VoiceServiceInternalBusiness(this, service.mConfigManager, mHandler,
                mIJniVoiceAdapter);

        initBroadcastReceiver();
    }

    private void initBroadcastReceiver() {
        // register headset plug receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        mContext.registerReceiver(mHeadsetPlugReceiver, filter);

        // register boot complete receiver
        mBootCompletedReceiver = new BootCompletedReceiver(mContext, mHandler);

        String isBootCompleted = SystemProperties.get(PROP_SYS_BOOT_COMPLETED);
        Log.i(TAG, "[initBroadcastReceiver]isBootCompleted = " + isBootCompleted);
        if (isBootCompleted != null && isBootCompleted.equals(SYS_BOOT_COMPLETED)) {
            mHandler.sendEmptyMessageDelayed(VoiceCommandBusiness.ACTION_MAIN_VOICE_BOOT_COMPLETED,
                    DELAY_MILLIS);
        } else {
            filter = new IntentFilter();
            filter.addAction(Intent.ACTION_BOOT_COMPLETED);
            mContext.registerReceiver(mBootCompletedReceiver, filter);
            mIsBootReciverRegister = true;
        }

        // register boot ipo/shutdown ipo receiver
        filter = new IntentFilter();
        filter.addAction("android.intent.action.ACTION_BOOT_IPO");
        filter.addAction("android.intent.action.ACTION_SHUTDOWN_IPO");
        mContext.registerReceiver(mIPOReceiver, filter);
    }

    /*
     * Send message to native via JNICommandAdapter
     */
    @Override
    public int dispatchMessageDown(VoiceMessage message) {
        int errorid = VoiceCommandListener.VOICE_NO_ERROR;
        Log.i(TAG, "[dispatchMessageDown]mainAction=" + message.mMainAction + " subAction="
                + message.mSubAction);
        switch (message.mMainAction) {
        case VoiceCommandListener.ACTION_MAIN_VOICE_UI:
            errorid = mVoiceUI.handleSyncVoiceMessage(message);
            break;
            
        case VoiceCommandListener.ACTION_MAIN_VOICE_TRAINING:
            errorid = mVoiceTraining.handleSyncVoiceMessage(message);
            break;
            
        case VoiceCommandListener.ACTION_MAIN_VOICE_RECOGNITION:
            errorid = mVoiceRecognize.handleSyncVoiceMessage(message);
            break;
            
        case VoiceCommandBusiness.ACTION_MAIN_VOICE_SERVICE:
            errorid = mVoiceServiceInternal.handleSyncVoiceMessage(message);
            break;
            
        case VoiceCommandListener.ACTION_MAIN_VOICE_CONTACTS:
            errorid = mVoiceContacts.handleSyncVoiceMessage(message);
            break;
            
        case VoiceCommandListener.ACTION_MAIN_VOICE_WAKEUP:
            errorid = mVoiceWakeup.handleSyncVoiceMessage(message);
            break;
            
        default:
            break;
        }
        
        return errorid;
    }

    @Override
    public int dispatchMessageUp(VoiceMessage message) {
        int errorid = VoiceCommandListener.VOICE_NO_ERROR;
        Log.i(TAG, "[dispatchMessageUp]mainAction=" + message.mMainAction + " subAction="
                + message.mSubAction);
        switch (message.mMainAction) {
        case VoiceCommandBusiness.ACTION_MAIN_VOICE_SERVICE:
            if (message.mSubAction == VoiceCommandBusiness.ACTION_VOICE_SERVICE_SELFEXIT) {
                mContext.unregisterReceiver(mHeadsetPlugReceiver);
                mContext.unregisterReceiver(mIPOReceiver);
                if (mIsBootReciverRegister) {
                    mContext.unregisterReceiver(mBootCompletedReceiver);
                }
                mBootCompletedReceiver.handleDataRelease();
            }
            errorid = mVoiceServiceInternal.handleSyncVoiceMessage(message);
            break;
            
        default:
            errorid = mIUpMsgDispatcher.dispatchMessageUp(message);
            break;
        }
        
        return errorid;
    }

    @Override
    public void setDownDispatcher(IMessageDispatcher dispatcher) {
        Log.i(TAG, "[setDownDispatcher]dispatcher: " + dispatcher);
        // Don't need next dispatcher because this dispatcher send message to
        // native directly
    }

    @Override
    public void setUpDispatcher(IMessageDispatcher dispatcher) {
        Log.i(TAG, "[setUpDispatcher]dispatcher: " + dispatcher);
        mIUpMsgDispatcher = dispatcher;
    }

    private BroadcastReceiver mHeadsetPlugReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            VoiceMessage msg = new VoiceMessage();
            msg.mMainAction = VoiceCommandBusiness.ACTION_MAIN_VOICE_BROADCAST;
            if (intent.getIntExtra("state", 0) == 0) {
                msg.mSubAction = VoiceCommandBusiness.ACTION_VOICE_BROADCAST_HEADSETPLUGOUT;
            } else {
                msg.mSubAction = VoiceCommandBusiness.ACTION_VOICE_BROADCAST_HEADSETPLUGIN;
            }
            Log.d(TAG, "[onReceive]mHeadsetPlugReceiver, msg.mSubAction: " + msg.mSubAction);
            mVoiceServiceInternal.handleSyncVoiceMessage(msg);
        }
    };

    private BroadcastReceiver mIPOReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "[onReceive]IPO Receiver action: " + action);
            if ("android.intent.action.ACTION_BOOT_IPO".equals(action)) {
                mBootCompletedReceiver.sendWakeupInitMessage();
            } else if ("android.intent.action.ACTION_SHUTDOWN_IPO".equals(action)) {
                VoiceMessage msg = new VoiceMessage();
                msg.mMainAction = VoiceCommandListener.ACTION_MAIN_VOICE_WAKEUP;
                msg.mSubAction = VoiceCommandListener.ACTION_VOICE_WAKEUP_IPO_SHUTDOWN_STATUS;
                mVoiceWakeup.handleAsyncVoiceMessage(msg);
            }
        }
    };

    // After OnReceive bootcompleted then unreigster boot completed receiver
    private void handleAsyncVoiceMessage() {
        mContext.unregisterReceiver(mBootCompletedReceiver);
        mIsBootReciverRegister = false;
    }

    // Service accident dead, need registerobserver once again
    private void handleAsyncVoiceBootCompleted() {
        mBootCompletedReceiver.registerObserver();
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "[handleMessage]msg.what = " + msg.what);
            switch (msg.what) {
            case VoiceCommandListener.ACTION_MAIN_VOICE_UI:
                mVoiceUI.handleAsyncVoiceMessage((VoiceMessage) msg.obj);
                break;

            case VoiceCommandListener.ACTION_MAIN_VOICE_TRAINING:
                mVoiceTraining.handleAsyncVoiceMessage((VoiceMessage) msg.obj);
                break;

            case VoiceCommandListener.ACTION_MAIN_VOICE_RECOGNITION:
                mVoiceRecognize.handleAsyncVoiceMessage((VoiceMessage) msg.obj);
                break;

            case VoiceCommandListener.ACTION_MAIN_VOICE_CONTACTS:
                mVoiceContacts.handleAsyncVoiceMessage((VoiceMessage) msg.obj);
                break;

            case VoiceCommandListener.ACTION_MAIN_VOICE_WAKEUP:
                mVoiceWakeup.handleAsyncVoiceMessage((VoiceMessage) msg.obj);
                break;

            case VoiceCommandBusiness.ACTION_MAIN_VOICE_SERVICE:
            case VoiceCommandBusiness.ACTION_MAIN_VOICE_BROADCAST:
                mVoiceServiceInternal.handleAsyncVoiceMessage((VoiceMessage) msg.obj);
                break;

            case VoiceCommandBusiness.ACTION_MAIN_VOICE_BROADCAST_BOOT_COMPLETED:
                handleAsyncVoiceMessage();
                break;

            case VoiceCommandBusiness.ACTION_MAIN_VOICE_BOOT_COMPLETED:
                handleAsyncVoiceBootCompleted();
                break;

            default:
                break;
            }
        }
    };
}
