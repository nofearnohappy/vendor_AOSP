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
package com.mediatek.voicecommand.business;

import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.provider.Settings;

import com.mediatek.common.voicecommand.VoiceCommandListener;
import com.mediatek.voicecommand.adapter.IVoiceAdapter;
import com.mediatek.voicecommand.mgr.ConfigurationManager;
import com.mediatek.voicecommand.mgr.IMessageDispatcher;
import com.mediatek.voicecommand.mgr.VoiceMessage;
import com.mediatek.voicecommand.util.Log;

/**
 * Manage Voice Wake up business.
 * 
 */
public class VoiceWakeupBusiness extends VoiceCommandBusiness {
    private static final String TAG = "VoiceWakeupBusiness";

    public static final int MSG_GET_WAKEUP_INIT = 10000;
    public static final int MSG_GET_WAKEUP_MODE = MSG_GET_WAKEUP_INIT + 1;
    public static final int MSG_GET_WAKEUP_COMMAND_STATUS = MSG_GET_WAKEUP_INIT + 2;

    public static final String VOICE_WAKEUP_MODE = Settings.System.VOICE_WAKEUP_MODE;
    public static final String VOICE_WAKEUP_COMMAND_STATUS = Settings.System.VOICE_WAKEUP_COMMAND_STATUS;

    private static final String MTK_VOW_SUPPORT_State = "MTK_VOW_SUPPORT";
    private static final String MTK_VOW_SUPPORT_on = "MTK_VOW_SUPPORT=true";

    private IVoiceAdapter mIJniVoiceAdapter;
    private Context mContext;

    /**
     * VoiceWakeup constructor.
     * 
     * @param dispatcher
     *            NativeDataManager instance
     * @param cfgMgr
     *            ConfigurationManager instance
     * @param handler
     *            the handler to run voice wake up message
     * @param adapter
     *            SwipAdapter instance
     * @param context
     *            context
     */
    public VoiceWakeupBusiness(IMessageDispatcher dispatcher, ConfigurationManager cfgMgr,
            Handler handler, IVoiceAdapter adapter, Context context) {
        super(dispatcher, cfgMgr, handler);
        Log.i(TAG, "[VoiceWakeupBusiness]new...");
        mIJniVoiceAdapter = adapter;
        mContext = context;
    }

    @Override
    public int handleSyncVoiceMessage(VoiceMessage message) {
        Log.i(TAG, "[handleSyncVoiceMessage]mSubAction = " + message.mSubAction);
        int errorid = VoiceCommandListener.VOICE_NO_ERROR;
        switch (message.mSubAction) {
        case VoiceCommandListener.ACTION_VOICE_WAKEUP_START:
            errorid = sendMessageToHandler(message);
            break;
            
        case VoiceCommandListener.ACTION_VOICE_WAKEUP_ENABLE:
            errorid = sendMessageToHandler(message);
            break;
            
        case VoiceCommandListener.ACTION_VOICE_WAKEUP_DISABLE:
            errorid = sendMessageToHandler(message);
            break;
            
        default:
            break;
        }
        Log.i(TAG, "[handleSyncVoiceMessage]errorid = " + errorid);

        return errorid;
    }

    @Override
    public int handleAsyncVoiceMessage(VoiceMessage message) {
        Log.i(TAG, "[handleAsyncVoiceMessage]mSubAction = " + message.mSubAction);
        int errorid = VoiceCommandListener.VOICE_NO_ERROR;
        if (!isWakeupSupport(mContext)) {
            errorid = VoiceCommandListener.VOICE_ERROR_COMMON_INVALID_DATA;
            Log.i(TAG, "[handleAsyncVoiceMessage]Voice Wakeup feature is off, return!");
            sendMessageToApps(message, errorid);
            return errorid;
        }
        switch (message.mSubAction) {
        case VoiceCommandListener.ACTION_VOICE_WAKEUP_START:
            errorid = handleWakeupStart(message);
            break;

        case VoiceCommandListener.ACTION_VOICE_WAKEUP_ENABLE:
            errorid = handleWakeupEnable(message);
            break;

        case VoiceCommandListener.ACTION_VOICE_WAKEUP_DISABLE:
            errorid = handleWakeupDisable(message);
            break;

        case VoiceCommandListener.ACTION_VOICE_WAKEUP_INIT:
            errorid = handleWakeupInit(message);
            break;

        case VoiceCommandListener.ACTION_VOICE_WAKEUP_MODE:
            errorid = handleWakeupMode(message);
            break;

        case VoiceCommandListener.ACTION_VOICE_WAKEUP_COMMAND_STATUS:
            errorid = handleWakeupCmdStatus(message);
            break;

        case VoiceCommandListener.ACTION_VOICE_WAKEUP_IPO_SHUTDOWN_STATUS:
            errorid = handleWakeupShutdownIpo(message);
            break;

        default:
            break;
        }
        Log.i(TAG, "[handleAsyncVoiceMessage]errorid = " + errorid);

        return errorid;
    }

    /**
     * Query wakeup mode from setting provider.
     * 
     * @param context
     *            context
     * @return wakeup mode
     */
    public static int getWakeupMode(Context context) {
        int mode = Settings.System.getInt(context.getContentResolver(), VOICE_WAKEUP_MODE, 1);
        Log.i(TAG, "[getWakeupMode]mode : " + mode);

        return mode;
    }

    /**
     * Query wakeup command status from setting provider.
     * 
     * @param context
     *            context
     * @return wakeup command status
     */
    public static int getWakeupCmdStatus(Context context) {
        int cmdStatus = Settings.System.getInt(context.getContentResolver(),
                VOICE_WAKEUP_COMMAND_STATUS, 0);
        Log.i(TAG, "[getWakeupCmdStatus]cmdStatus : " + cmdStatus);

        return cmdStatus;
    }

    /**
     * get wakeup enable status through wakeup command status.
     * 
     * @param cmdStatus
     *            wakeup command status
     * @return wakeup enable status
     */
    public static int getWakeupEnableStatus(int cmdStatus) {
        int wakeupEnableStatus = 0;
        if ((cmdStatus == VoiceCommandListener.VOICE_WAKEUP_STATUS_NOCOMMAND_UNCHECKED)
                || (cmdStatus == VoiceCommandListener.VOICE_WAKEUP_STATUS_COMMAND_UNCHECKED)) {
            wakeupEnableStatus = VoiceCommandListener.VOICE_WAKEUP_STATUS_NOCOMMAND_UNCHECKED;
        } else if (cmdStatus == VoiceCommandListener.VOICE_WAKEUP_STATUS_COMMAND_CHECKED) {
            wakeupEnableStatus = VoiceCommandListener.VOICE_WAKEUP_STATUS_COMMAND_UNCHECKED;
        }
        Log.i(TAG, "[getWakeupEnableStatus]wakeupEnableStatus: " + wakeupEnableStatus);

        return wakeupEnableStatus;
    }

    /**
     * Save wakeup status to setting provider.
     * 
     * @param context
     *            context
     * @param cmdStatus
     *            wakeup status
     */
    public static void setWakeupCmdStatus(Context context, int cmdStatus) {
        Log.i(TAG, "[setWakeupCmdStatus] to setting provider cmdStatus : " + cmdStatus);
        Settings.System
                .putInt(context.getContentResolver(), VOICE_WAKEUP_COMMAND_STATUS, cmdStatus);
    }

    /**
     * Check if support voice wakeup feature.
     * 
     * @param context
     *            context
     * @return true if support, otherwise false
     */
    public static boolean isWakeupSupport(Context context) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) {
            Log.e(TAG, "[isWakeupSupport] get audio service is null");
            return false;
        }
        String state = am.getParameters(MTK_VOW_SUPPORT_State);
        if (state != null) {
            return state.equalsIgnoreCase(MTK_VOW_SUPPORT_on);
        }
        return false;
    }

    /**
     * Start the wake up business in native adapter.
     * 
     * @param message
     *            VoiceMessage instance
     * 
     * @return result
     */
    private int handleWakeupStart(VoiceMessage message) {
        Log.d(TAG, "[handleWakeupStart]...");
        int errorid = VoiceCommandListener.VOICE_NO_ERROR;
        errorid = mIJniVoiceAdapter.startVoiceWakeup(message.mPkgName, message.pid);
        sendMessageToApps(message, errorid);

        return errorid;
    }

    private int handleWakeupEnable(VoiceMessage message) {
        Log.d(TAG, "[handleWakeupEnable]...");
        int errorid = VoiceCommandListener.VOICE_NO_ERROR;
        int wakeupCmdStatus = mCfgMgr.getWakeupCmdStatus();
        if (wakeupCmdStatus == VoiceCommandListener.VOICE_WAKEUP_STATUS_NOCOMMAND_UNCHECKED) {
            errorid = VoiceCommandListener.VOICE_ERROR_COMMON_INVALID_DATA;
        } else if (wakeupCmdStatus == VoiceCommandListener.VOICE_WAKEUP_STATUS_COMMAND_UNCHECKED) {
            setWakeupCmdStatus(mContext, VoiceCommandListener.VOICE_WAKEUP_STATUS_COMMAND_CHECKED);
        }
        Log.d(TAG, "[handleWakeupEnable]errorid = " + errorid);
        sendMessageToApps(message, errorid);
        
        return errorid;
    }

    private int handleWakeupDisable(VoiceMessage message) {
        Log.d(TAG, "[handleWakeupDisable]...");
        int errorid = VoiceCommandListener.VOICE_NO_ERROR;
        int wakeupCmdStatus = mCfgMgr.getWakeupCmdStatus();
        if (wakeupCmdStatus == VoiceCommandListener.VOICE_WAKEUP_STATUS_COMMAND_CHECKED) {
            setWakeupCmdStatus(mContext, VoiceCommandListener.VOICE_WAKEUP_STATUS_COMMAND_UNCHECKED);
        }
        sendMessageToApps(message, errorid);
        
        return errorid;
    }

    private int handleWakeupInit(VoiceMessage message) {
        Log.d(TAG, "[handleWakeupInit]...");
        int errorid = VoiceCommandListener.VOICE_NO_ERROR;
        int mode = message.mExtraData.getInt(VoiceCommandListener.ACTION_EXTRA_SEND_INFO);
        int cmdStatus = message.mExtraData.getInt(VoiceCommandListener.ACTION_EXTRA_SEND_INFO1);
        int[] cmdIds = message.mExtraData.getIntArray(VoiceCommandListener.ACTION_EXTRA_SEND_INFO2);
        String patternPath = mCfgMgr.getVoiceRecognitionPatternFilePath(mode);
        String anyOnePatternPath = mCfgMgr
                .getVoiceRecognitionPatternFilePath(VoiceCommandListener.VOICE_WAKEUP_MODE_SPEAKER_INDEPENDENT);
        String anyOnePasswordPath = mCfgMgr
                .getPasswordFilePath(VoiceCommandListener.VOICE_WAKEUP_MODE_SPEAKER_INDEPENDENT);
        String commandPatternPath = mCfgMgr
                .getVoiceRecognitionPatternFilePath(VoiceCommandListener.VOICE_WAKEUP_MODE_SPEAKER_DEPENDENT);
        String commandPasswordPath = mCfgMgr
                .getPasswordFilePath(VoiceCommandListener.VOICE_WAKEUP_MODE_SPEAKER_DEPENDENT);
        String ubmPath = mCfgMgr.getUbmFilePath();
        String wakeupinfoPath = mCfgMgr.getWakeupInfoPath();
        if (patternPath == null || anyOnePatternPath == null || anyOnePasswordPath == null
                || commandPatternPath == null || commandPasswordPath == null || ubmPath == null
                || wakeupinfoPath == null) {
            Log.d(TAG, "[handleWeakupInit] error patternPath=" + patternPath
                    + " anyOnePatternPath=" + anyOnePatternPath + " anyOnePasswordPath="
                    + anyOnePasswordPath + " commandPatternPath=" + commandPatternPath
                    + " commandPasswordPath=" + commandPasswordPath + " ubmPath=" + ubmPath
                    + "wakeupinfoPath=" + wakeupinfoPath);
            errorid = VoiceCommandListener.VOICE_ERROR_COMMON_SERVICE;
        } else {
            int wakeupEnableStatus = getWakeupEnableStatus(cmdStatus);
            mCfgMgr.setWakeupMode(mode);
            mCfgMgr.setWakeupStatus(cmdStatus);
            errorid = mIJniVoiceAdapter.initVoiceWakeup(mode, wakeupEnableStatus, cmdIds,
                    patternPath, VoiceCommandListener.VOICE_WAKEUP_MODE_SPEAKER_INDEPENDENT,
                    anyOnePatternPath, anyOnePasswordPath,
                    VoiceCommandListener.VOICE_WAKEUP_MODE_SPEAKER_DEPENDENT, commandPatternPath,
                    commandPasswordPath, ubmPath, wakeupinfoPath);
        }
        Log.d(TAG, "[handleWakeupInit]errorid = " + errorid);

        return errorid;
    }

    private int handleWakeupMode(VoiceMessage message) {
        Log.d(TAG, "[handleWakeupMode]...");
        int errorid = VoiceCommandListener.VOICE_NO_ERROR;
        int mode = message.mExtraData.getInt(VoiceCommandListener.ACTION_EXTRA_SEND_INFO);
        mCfgMgr.setWakeupMode(mode);
        String ubmPath = mCfgMgr.getUbmFilePath();
        if (ubmPath == null) {
            Log.i(TAG, "[handleWakeupMode] error ubmPath=" + ubmPath);
        } else {
            errorid = mIJniVoiceAdapter.sendVoiceWakeupMode(mode, ubmPath);
        }
        Log.d(TAG, "[handleWakeupMode]errorid = " + errorid);

        return errorid;
    }

    private int handleWakeupCmdStatus(VoiceMessage message) {
        Log.d(TAG, "[handleWakeupCmdStatus]...");
        int errorid = VoiceCommandListener.VOICE_NO_ERROR;
        int wakeupCmdStatus = message.mExtraData
                .getInt(VoiceCommandListener.ACTION_EXTRA_SEND_INFO);
        int wakeupEnableStatus = getWakeupEnableStatus(wakeupCmdStatus);
        mCfgMgr.setWakeupStatus(wakeupCmdStatus);
        errorid = mIJniVoiceAdapter.sendVoiceWakeupCmdStatus(wakeupEnableStatus);
        Log.d(TAG, "[handleWakeupCmdStatus]errorid = " + errorid);

        return errorid;
    }

    private int handleWakeupShutdownIpo(VoiceMessage message) {
        Log.d(TAG, "[handleWakeupShutdownIpo]...");
        int errorid = VoiceCommandListener.VOICE_NO_ERROR;
        errorid = mIJniVoiceAdapter
                .sendVoiceWakeupCmdStatus(VoiceCommandListener.VOICE_WAKEUP_STATUS_NOCOMMAND_UNCHECKED);
        Log.d(TAG, "[handleWakeupShutdownIpo]errorid = " + errorid);

        return errorid;
    }
}
