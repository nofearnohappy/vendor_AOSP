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

import android.os.Bundle;
import android.os.Handler;

import com.mediatek.common.voicecommand.VoiceCommandListener;
import com.mediatek.voicecommand.adapter.IVoiceAdapter;
import com.mediatek.voicecommand.data.DataPackage;
import com.mediatek.voicecommand.mgr.ConfigurationManager;
import com.mediatek.voicecommand.mgr.IMessageDispatcher;
import com.mediatek.voicecommand.mgr.VoiceMessage;
import com.mediatek.voicecommand.util.Log;

import java.io.File;

public class VoiceTrainingBusiness extends VoiceCommandBusiness {
    private static final String TAG = "VoiceTrainingBusiness";

    private IVoiceAdapter mIJniVoiceAdapter;

    public VoiceTrainingBusiness(IMessageDispatcher dispatcher, ConfigurationManager cfgMgr,
            Handler handler, IVoiceAdapter adapter) {
        super(dispatcher, cfgMgr, handler);
        Log.i(TAG, "[VoiceTrainingBusiness]new...");
        mIJniVoiceAdapter = adapter;
    }

    @Override
    public int handleSyncVoiceMessage(VoiceMessage message) {
        Log.i(TAG, "[handleSyncVoiceMessage]mSubAction = " + message.mSubAction + ",mMainAction = "
                + message.mMainAction);
        int errorid = VoiceCommandListener.VOICE_NO_ERROR;
        switch (message.mSubAction) {
        case VoiceCommandListener.ACTION_VOICE_TRAINING_START:
        case VoiceCommandListener.ACTION_VOICE_TRAINING_INTENSITY:
        case VoiceCommandListener.ACTION_VOICE_TRAINING_STOP:
        case VoiceCommandListener.ACTION_VOICE_TRAINING_RESET:
        case VoiceCommandListener.ACTION_VOICE_TRAINING_MODIFY:
        case VoiceCommandListener.ACTION_VOICE_TRAINING_FINISH:
        case VoiceCommandListener.ACTION_VOICE_TRAINING_CONTINUE:
            sendMessageToHandler(message);
            break;

        case VoiceCommandListener.ACTION_VOICE_TRAINING_PASSWORD_FILE:
            getPasswordFilePath(message);
            break;

        default:
            // do nothing because illeage action has been filter in
            // AppDataManager
            break;
        }

        return errorid;
    }

    @Override
    public int handleAsyncVoiceMessage(VoiceMessage message) {
        Log.i(TAG, "[handleAsyncVoiceMessage]mSubAction = " + message.mSubAction
                + ",mMainAction = " + message.mMainAction);
        int errorid = VoiceCommandListener.VOICE_NO_ERROR;
        switch (message.mSubAction) {
        case VoiceCommandListener.ACTION_VOICE_TRAINING_START:
            handleTrainingStart(message);
            break;

        case VoiceCommandListener.ACTION_VOICE_TRAINING_INTENSITY:
            handleTrainingIntensity(message);
            break;

        case VoiceCommandListener.ACTION_VOICE_TRAINING_STOP:
            handleTrainingStop(message);
            break;

        case VoiceCommandListener.ACTION_VOICE_TRAINING_RESET:
            handleTrainingReset(message);
            break;

        case VoiceCommandListener.ACTION_VOICE_TRAINING_MODIFY:
            handleTrainingModify(message);
            break;

        case VoiceCommandListener.ACTION_VOICE_TRAINING_FINISH:
            handleTrainingFinish(message);
            break;

        case VoiceCommandListener.ACTION_VOICE_TRAINING_CONTINUE:
            handleTrainingContinue(message);
            break;

        default:
            break;
        }

     return errorid;
    }

    private int handleTrainingStart(VoiceMessage message) {
        Log.d(TAG, "[handleTrainingStart]...");
        int errorid = VoiceCommandListener.VOICE_NO_ERROR;

        if (message.mExtraData == null) {
            errorid = VoiceCommandListener.VOICE_ERROR_COMMON_INVALID_DATA;
        } else {
            int commandid = message.mExtraData.getInt(VoiceCommandListener.ACTION_EXTRA_SEND_INFO);
            int trainingMode = message.mExtraData
                    .getInt(VoiceCommandListener.ACTION_EXTRA_SEND_INFO1);
            int[] commandMask = message.mExtraData
                    .getIntArray(VoiceCommandListener.ACTION_EXTRA_SEND_INFO2);
            if (commandid < 0) {
                errorid = VoiceCommandListener.VOICE_ERROR_COMMON_INVALID_DATA;
            } else {
                String pwdpath = mCfgMgr.getPasswordFilePath(trainingMode);
                String patternpath = mCfgMgr.getVoiceRecognitionPatternFilePath(trainingMode);
                String featurepath = mCfgMgr.getFeatureFilePath(trainingMode);
                String umbpath = mCfgMgr.getUbmFilePath();
                String wakeupinfoPath = mCfgMgr.getWakeupInfoPath();
                if (pwdpath == null || patternpath == null || featurepath == null
                        || umbpath == null || wakeupinfoPath == null) {
                    Log.d(TAG, "[handleTrainingStart] error pwdpath=" + pwdpath + " patternpath="
                            + patternpath + " featurepath=" + featurepath + " umbpath=" + umbpath
                            + " wakeupinfoPath =" + wakeupinfoPath);
                    errorid = VoiceCommandListener.VOICE_ERROR_COMMON_SERVICE;
                } else {
                    errorid = mIJniVoiceAdapter.startVoiceTraining(pwdpath, patternpath,
                            featurepath, umbpath, commandid, commandMask, trainingMode,
                            wakeupinfoPath, message.mPkgName, message.pid);
                }
            }
        }
        Log.d(TAG, "[handleTrainingStart]errorid = " + errorid);

        sendMessageToApps(message, errorid);

        return errorid;
    }

    private int handleTrainingStop(VoiceMessage message) {
        Log.d(TAG, "[handleTrainingStop]...");
        mIJniVoiceAdapter.stopVoiceTraining(message.mPkgName, message.pid);

        message.mExtraData = DataPackage.packageSuccessResult();

        mDispatcher.dispatchMessageUp(message);

        return VoiceCommandListener.VOICE_NO_ERROR;
    }

    private int handleTrainingIntensity(VoiceMessage message) {
        Log.d(TAG, "[handleTrainingIntensity]...");
        int intensity = mIJniVoiceAdapter.getNativeIntensity();
        message.mExtraData = DataPackage.packageResultInfo(
                VoiceCommandListener.ACTION_EXTRA_RESULT_SUCCESS, intensity, 0);

        mDispatcher.dispatchMessageUp(message);

        return VoiceCommandListener.VOICE_NO_ERROR;
    }

    private int getPasswordFilePath(VoiceMessage message) {
        int errorid = VoiceCommandListener.VOICE_NO_ERROR;
        Bundle bundle = null;
        if (message.mExtraData == null) {
            errorid = VoiceCommandListener.VOICE_ERROR_COMMON_INVALID_DATA;
            bundle = DataPackage.packageErrorResult(errorid);
        } else {
            int commandid = message.mExtraData.getInt(VoiceCommandListener.ACTION_EXTRA_SEND_INFO);
            int trainingMode = message.mExtraData
                    .getInt(VoiceCommandListener.ACTION_EXTRA_SEND_INFO1);
            String passwordpath = mCfgMgr.getPasswordFilePath(trainingMode);
            if (commandid < 0 || passwordpath == null) {
                errorid = VoiceCommandListener.VOICE_ERROR_COMMON_INVALID_DATA;
                bundle = DataPackage.packageErrorResult(errorid);
            } else {
                String path = passwordpath + commandid + ".dat";
                File file = new File(path);
                if (file.exists()) {
                    bundle = DataPackage.packageResultInfo(
                            VoiceCommandListener.ACTION_EXTRA_RESULT_SUCCESS, path, null);
                } else {
                    errorid = VoiceCommandListener.VOICE_ERROR_COMMON_INVALID_DATA;
                    bundle = DataPackage.packageErrorResult(errorid);
                }
            }
        }
        message.mExtraData = bundle;
        mDispatcher.dispatchMessageUp(message);

        return errorid;
    }

    private int handleTrainingReset(VoiceMessage message) {
        Log.d(TAG, "[handleTrainingReset]...");
        int errorid = VoiceCommandListener.VOICE_NO_ERROR;

        if (message.mExtraData == null) {
            errorid = VoiceCommandListener.VOICE_ERROR_COMMON_INVALID_DATA;
        } else {
            int commandid = message.mExtraData.getInt(VoiceCommandListener.ACTION_EXTRA_SEND_INFO);
            int trainingMode = message.mExtraData
                    .getInt(VoiceCommandListener.ACTION_EXTRA_SEND_INFO1);
            if (commandid < 0) {
                errorid = VoiceCommandListener.VOICE_ERROR_COMMON_INVALID_DATA;
            } else {
                String pwdpath = mCfgMgr.getPasswordFilePath(trainingMode);
                String patternpath = mCfgMgr.getVoiceRecognitionPatternFilePath(trainingMode);
                String featurepath = mCfgMgr.getFeatureFilePath(trainingMode);

                if (pwdpath == null || patternpath == null || featurepath == null) {
                    Log.d(TAG, "[handleTrainingReset] error pwdpath=" + pwdpath + " patternpath="
                            + patternpath + " featurepath=" + featurepath);
                    errorid = VoiceCommandListener.VOICE_ERROR_COMMON_SERVICE;
                } else {
                    errorid = mIJniVoiceAdapter.resetVoiceTraining(pwdpath, patternpath,
                            featurepath, commandid);
                }
            }
        }
        Log.d(TAG, "[handleTrainingReset]errorid = " + errorid);
        sendMessageToApps(message, errorid);

        return errorid;
    }

    private int handleTrainingModify(VoiceMessage message) {
        Log.d(TAG, "[handleTrainingModify]...");
        int errorid = VoiceCommandListener.VOICE_NO_ERROR;

        if (message.mExtraData == null) {
            errorid = VoiceCommandListener.VOICE_ERROR_COMMON_INVALID_DATA;
        } else {
            int commandid = message.mExtraData.getInt(VoiceCommandListener.ACTION_EXTRA_SEND_INFO);
            int trainingMode = message.mExtraData
                    .getInt(VoiceCommandListener.ACTION_EXTRA_SEND_INFO1);
            if (commandid < 0) {
                errorid = VoiceCommandListener.VOICE_ERROR_COMMON_INVALID_DATA;
            } else {
                String pwdpath = mCfgMgr.getPasswordFilePath(trainingMode);
                String patternpath = mCfgMgr.getVoiceRecognitionPatternFilePath(trainingMode);
                String featurepath = mCfgMgr.getFeatureFilePath(trainingMode);

                if (pwdpath == null || patternpath == null || featurepath == null) {
                    Log.d(TAG, "[handleTrainingModify] error pwdpath=" + pwdpath + " patternpath="
                            + patternpath + " featurepath=" + featurepath);
                    errorid = VoiceCommandListener.VOICE_ERROR_COMMON_SERVICE;
                } else {
                    errorid = mIJniVoiceAdapter.modifyVoiceTraining(pwdpath, patternpath,
                            featurepath, commandid);
                }
            }
        }
        Log.d(TAG, "[handleTrainingModify]errorid = " + errorid);
        sendMessageToApps(message, errorid);
        return errorid;
    }

    private int handleTrainingFinish(VoiceMessage message) {
        int errorid = mIJniVoiceAdapter.finishVoiceTraining(message.mPkgName, message.pid);
        Log.d(TAG, "[handleTrainingFinish]errorid = " + errorid);
        sendMessageToApps(message, errorid);

        return errorid;
    }

    private int handleTrainingContinue(VoiceMessage message) {
        int errorid = VoiceCommandListener.VOICE_NO_ERROR;
        errorid = mIJniVoiceAdapter.continueVoiceTraining(message.mPkgName, message.pid);
        Log.d(TAG, "[handleTrainingContinue]errorid = " + errorid);
        sendMessageToApps(message, errorid);

        return errorid;
    }
}
