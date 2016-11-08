/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.mediatek.camera.addition;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import com.android.camera.R;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;

import com.mediatek.camera.ICameraContext;
import com.mediatek.camera.platform.ICameraAppUi.ViewState;
import com.mediatek.camera.platform.ICameraView;
import com.mediatek.camera.setting.SettingConstants;
import com.mediatek.camera.util.Log;

//TODO:mediatek package can't reference to the MTK non-SDK or MTK SDK
import com.mediatek.common.voicecommand.IVoiceCommandListener;
import com.mediatek.common.voicecommand.IVoiceCommandManagerService;
import com.mediatek.common.voicecommand.VoiceCommandListener;

/* A class that handles voice capture flow
 *
 */
public class VoiceCapture extends CameraAddition {
    private static String TAG = "VoiceCapture";

    private static final int VOICE_COMMAND_CAPTURE = 3;
    private static final int VOICE_COMMAND_CHEESE = 4;

    private static final int USER_GUIDE_UPDATED = 100;
    private static final int VOICE_VALUE_UPDATED = 101;
    private static final int VOICE_COMMAND_RECEIVE = 102;
    private static final int PLAY_VOICE_COMMAND = 103;
    private static final int VOICE_KEYWORDS_UPDATED = 104;
    private static final int UNKNOWN = -1;

    protected final Handler mHandler = new MainHandler();

    public interface Listener {
        void onVoiceValueUpdated(String value);
    }

    public static final String VOICE_ON = "on";
    public static final String VOICE_OFF = "off";

    private String mVoiceValue;
    private String[] mKeywords;
    // Voice command Path from framework
    private String mCommandPathKeywords;
    // Key value
    private String[] mCommandPath = new String[] { "voice0", "voice1" };
    private String mVoiceCommandPath;
    private String mPackageName;

    private int mCommandId;
    // Voice command Id for recording voice
    private int mVoiceCommandId;
    private int mVoiceCaptureSoundId;
    private int mVoiceCaptureStreamId;

    private boolean mStartUpdate;
    private boolean mIsOpened;
    private boolean mIsRegistered;
    private boolean mIsCallbackButPreferenceNull;
    private boolean mIsSupportIndicator = false;
    private boolean mIsSyncWithNative = false;

    private Context mContext;
    private SoundPool mVoiceCaptureSound;
    private Listener mListener;
    private ICameraView mICameraView;

    private IVoiceCommandManagerService mVoiceManagerService;

    // Cache voice
    private HashMap<String, Integer> mSoundMap = new HashMap<String, Integer>();
    private List<Listener> mListeners = new CopyOnWriteArrayList<Listener>();

    public VoiceCapture(ICameraContext context) {
        super(context);
        Log.i(TAG, "[VoiceCapture]constructor...");
        mPackageName = mActivity.getPackageName();
        mIsSupportIndicator = mICameraContext.getFeatureConfig().isVoiceUiSupport()
                && mIModuleCtrl.isNonePickIntent();
    }

    @Override
    public void open() {
        Log.i(TAG, "[open]...mVoiceValue = " + mVoiceValue);
        setVoiceValue();
        // notify key words if setting not ready
        notifyKeyWordsToUi();
        Log.i(TAG, "[open]mIsSupportIndicator = " + mIsSupportIndicator);
    }

    @Override
    public void close() {
        Log.i(TAG, "[close]...");
    }

    @Override
    public boolean isSupport() {
        Log.i(TAG, "[isSupport]...");
        return mIsSupportIndicator;
    }

    @Override
    public boolean isOpen() {
        Log.i(TAG, "[isOpen]...");
       return false;
    }

    @Override
    public boolean execute(AdditionActionType type, Object... arg) {
        Log.i(TAG, "[execute] ActionType = " + type);
        if (!mIsSupportIndicator) {
            return false;
        }
        switch (type) {
        case ACTION_ON_VOICE_COMMAND_NOTIFY:
            playVoiceCommandById((Integer) arg[0]);
            break;

        default:
            break;
        }

        return false;
    }

    @Override
    public void resume() {
        Log.i(TAG, "[resume]...");
        startUpdateVoiceState();
    }

    @Override
    public void pause() {
        Log.i(TAG, "[pause]...");
        stopUpdateVoiceState();
        mIsSyncWithNative = false;
    }

    @Override
    public void destory() {
        Log.i(TAG, "[destory]...");
        if (mIsSupportIndicator) {
            unBindVoiceService();
        }
    }

    private void enableVoice() {
        Log.d(TAG, "[enableVoice]mIsOpened = " + mIsOpened);
        if (mIsOpened && !isSupportVoiceCase()) {
            return;
        }
        if (mVoiceManagerService == null) {
            bindVoiceService();
        } else {
            registerVoiceCommand(mPackageName);
            startVoiceCommand(mPackageName, VoiceCommandListener.ACTION_MAIN_VOICE_UI,
                    VoiceCommandListener.ACTION_VOICE_UI_ENABLE, null);
            startVoiceCommand(mPackageName, VoiceCommandListener.ACTION_MAIN_VOICE_COMMON,
                    VoiceCommandListener.ACTION_VOICE_COMMON_KEYWORD, null);
            mHandler.sendEmptyMessage(USER_GUIDE_UPDATED);
            mIsOpened = true;
        }
    }


    private void setVoiceValue() {
        Log.d(TAG, "[setVoiceValue]mIsSyncWithNative = (" + mIsSyncWithNative + ")");
        String value = null;
        if (mIsCallbackButPreferenceNull) {
            updateSettingValue();
            mIsCallbackButPreferenceNull = false;
        }
        if (mISettingCtrl != null
                && mISettingCtrl.getListPreference(SettingConstants.KEY_VOICE) != null) {
            value = mISettingCtrl.getListPreference(SettingConstants.KEY_VOICE).getValue();
            if (isSupportVoiceCase()) {
                mISettingCtrl.setSettingValue(SettingConstants.KEY_VOICE, value,
                        mICameraDeviceManager.getCurrentCameraId());
            }
        }
        if (mIsSyncWithNative
                && (mVoiceValue == null || !mVoiceValue.equals(value))) {
            mVoiceValue = value;
            syncVoiceSwitch();
        }
        Log.d(TAG, "[setVoiceValue](" + value + ") mVoiceValue=" + mVoiceValue);
    }

    private void playVoiceCommandById(int commandId) {
        Log.d(TAG, "playVoiceCommandById commandId = " + commandId);
        mVoiceCommandId = commandId;
        mHandler.sendEmptyMessage(PLAY_VOICE_COMMAND);
    }

    private void unBindVoiceService() {
        Log.i(TAG, "[unBindVoiceService]...");
        if (mVoiceManagerService != null) {
            mActivity.unbindService(mVoiceSerConnection);
            mIsRegistered = false;
            mVoiceManagerService = null;
        }
    }

    /**
     * This Handler is used to post message back onto the main thread of the
     * application
     */
    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "[handleMessage]msg.what = " + msg.what);
            switch (msg.what) {
            case USER_GUIDE_UPDATED:
                notifyUserGuideIfNeed();
                break;

            case VOICE_VALUE_UPDATED:
                notifyStateChangedIfNeed();
                break;

            case VOICE_COMMAND_RECEIVE:
                notifyCommandIfNeed(mCommandId);
                break;

            case PLAY_VOICE_COMMAND:
                playVoiceCommandSound(mVoiceCommandId);
                break;

            case VOICE_KEYWORDS_UPDATED:
                notifyKeyWordsToUi();
                break;

            default:
                break;
            }
        }
    }

    private void playVoiceCommandSound(int voiceId) {
        Log.d(TAG, "[playVoiceCommandSound]voiceId=" + voiceId);
        int voiceCommandId = mSoundMap.get(mCommandPath[voiceId]);
        mVoiceCaptureStreamId = mVoiceCaptureSound.play(voiceCommandId, 1, 1, 0, 0, 1);
    }

    private void notifyUserGuideIfNeed() {
        Log.d(TAG, "[notifyUserGuideIfNeed]mKeywords=" + mKeywords);
        if (mKeywords != null) {
            String userGuide = getUserVoiceGuide(mKeywords);
            if (userGuide != null && mIsSupportIndicator && checkNeedShowToast()) {
                mICameraAppUi.showToast(userGuide);
            }
        }
    }

    private void notifyStateChangedIfNeed() {
        Log.d(TAG, "[notifyStateChangedIfNeed]...mVoiceValue  = " + mVoiceValue);
        for (Listener listener : mListeners) {
            listener.onVoiceValueUpdated(mVoiceValue);
        }
        Log.d(TAG, "[notifyStateChangedIfNeed]...mIsSyncWithNative  = "
                + mIsSyncWithNative + ", mISettingCtrl" + mISettingCtrl);
        if (!mIsSyncWithNative) {
            updateSettingValue();
        }
        syncVoiceSwitch();
    }

    private void updateSettingValue() {
        if (mVoiceValue != null && mISettingCtrl != null
                && mISettingCtrl.getListPreference(SettingConstants.KEY_VOICE) != null) {
            mISettingCtrl.getListPreference(SettingConstants.KEY_VOICE)
                    .setValue(mVoiceValue);
            if (checkNeedCaptureCase()) {
                mISettingCtrl.setSettingValue(SettingConstants.KEY_VOICE,
                        mVoiceValue, mICameraDeviceManager.getCurrentCameraId());
            }
            mIsSyncWithNative = true;
        } else {
            mIsCallbackButPreferenceNull = true;
        }
    }

    private void syncVoiceSwitch() {
        if (VOICE_ON.equals(mVoiceValue)) {
            enableVoice();
        } else {
            disableVoice();
        }
    }

    private void notifyCommandIfNeed(int commandId) {
        Log.d(TAG, "[notifyCommandIfNeed]commandId = " + commandId);
        if (VOICE_COMMAND_CAPTURE == commandId || VOICE_COMMAND_CHEESE == commandId) {
            if (mICameraAppUi.getPhotoShutter() != null
                    && checkNeedCaptureCase()) {
                mICameraAppUi.getPhotoShutter().performClick();
            }
        }
    }

    private void notifyKeyWordsToUi() {
        Log.d(TAG, "[notifyKeyWordsToUi] mKeywords=" + mKeywords + ", mISettingCtrl = "
                + mISettingCtrl);
        if (mKeywords != null && mISettingCtrl != null
                && mISettingCtrl.getListPreference(SettingConstants.KEY_VOICE) != null) {
            mISettingCtrl.getListPreference(SettingConstants.KEY_VOICE)
                    .setExtendedValues(mKeywords);
        }
    }

    private void notifyCachePathIfNeed() {
        Log.d(TAG, "[notifyCachePathIfNeed]mCommandPathKeywords = " + mCommandPathKeywords);
        mVoiceCaptureSound = null;
        mVoiceCaptureSound = new SoundPool(1, AudioManager.STREAM_SYSTEM_ENFORCED, 0);
        for (int i = 0; i < mCommandPath.length; i++) {
            String path = mCommandPathKeywords + i + ".ogg";
            mSoundMap.put(mCommandPath[i], mVoiceCaptureSound.load(path, 1));
        }
    }

    private String getUserVoiceGuide(String[] voice) {
        String userGuide = null;
        if (voice != null && voice.length >= 2) {
            userGuide = mActivity.getString(R.string.voice_guide, voice[0], voice[1]);
        }
        Log.d(TAG, "[getUserVoiceGuide]userGuide = " + userGuide);

        return userGuide;
    }

    private void startUpdateVoiceState() {
        Log.d(TAG, "[startUpdateVoiceState]mStartUpdate=" + mStartUpdate
                + ", mIsSupportIndicator = " + mIsSupportIndicator);
        if (mIsSupportIndicator) {
            if (!mStartUpdate) {
                startGetVoiceState();
                mStartUpdate = true;
            }
        }
    }

    private void stopUpdateVoiceState() {
        Log.d(TAG, "[stopUpdateVoiceState]mStartUpdate = " + mStartUpdate);
        if (mIsSupportIndicator) {
            if (mStartUpdate) {
                stopVoice();
                // set voice value off for don't update indicator before get
                // voice state.
                mVoiceValue = VOICE_OFF;
                mStartUpdate = false;
            }
        }
    }

    private void disableVoice() {
        Log.d(TAG, "[disableVoice]mIsOpened = " + mIsOpened);
        if (!mIsOpened) {
            return;
        }
        if (mVoiceManagerService != null) {
            startVoiceCommand(mPackageName, VoiceCommandListener.ACTION_MAIN_VOICE_UI,
                    VoiceCommandListener.ACTION_VOICE_UI_DISALBE, null);
            unRegisterVoiceCommand(mPackageName);
            mIsOpened = false;
        }
    }

    private void stopVoice() {
        Log.d(TAG, "[stopVoice]...");
        if (mVoiceManagerService != null) {
            startVoiceCommand(mPackageName, VoiceCommandListener.ACTION_MAIN_VOICE_UI,
                    VoiceCommandListener.ACTION_VOICE_UI_STOP, null);
            unRegisterVoiceCommand(mPackageName);
            release();
            mIsOpened = false;
        }
    }

    private void startGetVoiceState() {
        Log.d(TAG, "[startGetVoiceState]...");
        if (mVoiceManagerService == null) {
            bindVoiceService();
        } else {
            registerVoiceCommand(mPackageName);
            startVoiceCommand(mPackageName, VoiceCommandListener.ACTION_MAIN_VOICE_COMMON,
                    VoiceCommandListener.ACTION_VOICE_COMMON_PROCESS_STATE, null);
            startVoiceCommand(mPackageName, VoiceCommandListener.ACTION_MAIN_VOICE_COMMON,
                    VoiceCommandListener.ACTION_VOICE_COMMON_COMMAND_PATH, null);
            startVoiceCommand(mPackageName, VoiceCommandListener.ACTION_MAIN_VOICE_COMMON,
                    VoiceCommandListener.ACTION_VOICE_COMMON_KEYWORD, null);
        }
    }

    private void bindVoiceService() {
        Log.d(TAG, "[bindVoiceService]...");
        Intent mVoiceServiceIntent = new Intent();
        mVoiceServiceIntent.setAction(VoiceCommandListener.VOICE_SERVICE_ACTION);
        mVoiceServiceIntent.addCategory(VoiceCommandListener.VOICE_SERVICE_CATEGORY);
        mVoiceServiceIntent.setPackage(VoiceCommandListener.VOICE_SERVICE_PACKAGE_NAME);
        mActivity.bindService(mVoiceServiceIntent, mVoiceSerConnection, Context.BIND_AUTO_CREATE);
    }

    private void printExtraData(Bundle extraData) {
        Set<String> keys = extraData.keySet();
        for (String key : keys) {
            Log.d(TAG, "[printExtraData]extraData[" + key + "]=" + extraData.get(key));
        }
    }

    private void release() {
        Log.i(TAG, "[release]...");
        mHandler.removeMessages(USER_GUIDE_UPDATED);
        mHandler.removeMessages(VOICE_VALUE_UPDATED);
        mHandler.removeMessages(VOICE_COMMAND_RECEIVE);
        mHandler.removeMessages(PLAY_VOICE_COMMAND);
        if (mVoiceCaptureSound != null) {
            mVoiceCaptureSound.stop(mVoiceCaptureStreamId);
            mVoiceCaptureSound.unload(mVoiceCaptureSoundId);
            mVoiceCaptureSound.release();
        }

    }

    // Callback used to notify apps
    private IVoiceCommandListener mCallback = new IVoiceCommandListener.Stub() {

        @Override
        public void onVoiceCommandNotified(int mainAction, int subAction, Bundle extraData) {
            Log.i(TAG, "[onVoiceCommandNotified]" + mainAction + ", " + subAction + ", "
                    + extraData + ")");
            int result = UNKNOWN;
            switch (mainAction) {
            case VoiceCommandListener.ACTION_MAIN_VOICE_UI:
                switch (subAction) {
                case VoiceCommandListener.ACTION_VOICE_UI_ENABLE:
                    break;

                case VoiceCommandListener.ACTION_VOICE_UI_DISALBE:
                    break;

                case VoiceCommandListener.ACTION_VOICE_UI_START:
                    break;

                case VoiceCommandListener.ACTION_VOICE_UI_STOP:
                    break;

                case VoiceCommandListener.ACTION_VOICE_UI_NOTIFY:
                    if (extraData != null) {
                        printExtraData(extraData);
                        result = extraData
                                .getInt(VoiceCommandListener.ACTION_EXTRA_RESULT, UNKNOWN);
                        if (result == VoiceCommandListener.ACTION_EXTRA_RESULT_SUCCESS) {
                            mCommandId = extraData.getInt(
                                    VoiceCommandListener.ACTION_EXTRA_RESULT_INFO, UNKNOWN);
                            mHandler.sendEmptyMessage(VOICE_COMMAND_RECEIVE);
                        }
                    }
                    break;

                default:
                    break;
                }
                break;

            case VoiceCommandListener.ACTION_MAIN_VOICE_COMMON:
                Log.i(TAG, "[onVoiceCommandNotified]subAction = " + subAction);
                switch (subAction) {
                case VoiceCommandListener.ACTION_VOICE_COMMON_KEYWORD:
                    if (extraData != null) {
                        printExtraData(extraData);
                        result = extraData
                                .getInt(VoiceCommandListener.ACTION_EXTRA_RESULT, UNKNOWN);
                        if (result == VoiceCommandListener.ACTION_EXTRA_RESULT_SUCCESS) {
                            mKeywords = extraData
                                    .getStringArray(VoiceCommandListener.ACTION_EXTRA_RESULT_INFO);
                            mHandler.sendEmptyMessage(VOICE_KEYWORDS_UPDATED);
                        }
                    }
                    break;

                case VoiceCommandListener.ACTION_VOICE_COMMON_PROCESS_STATE:
                    if (extraData != null) {
                        printExtraData(extraData);
                        result = extraData
                                .getInt(VoiceCommandListener.ACTION_EXTRA_RESULT, UNKNOWN);
                        if (result == VoiceCommandListener.ACTION_EXTRA_RESULT_SUCCESS) {
                            boolean enabled = extraData.getBoolean(
                                    VoiceCommandListener.ACTION_EXTRA_RESULT_INFO, false);
                            mVoiceValue = (enabled ? VOICE_ON : VOICE_OFF);
                            mHandler.sendEmptyMessage(VOICE_VALUE_UPDATED);
                        }
                    }
                    break;

                case VoiceCommandListener.ACTION_VOICE_COMMON_COMMAND_PATH:
                    if (extraData != null) {
                        printExtraData(extraData);
                        result = extraData
                                .getInt(VoiceCommandListener.ACTION_EXTRA_RESULT, UNKNOWN);
                        if (result == VoiceCommandListener.ACTION_EXTRA_RESULT_SUCCESS) {
                            mCommandPathKeywords = extraData
                                    .getString(VoiceCommandListener.ACTION_EXTRA_RESULT_INFO);
                            notifyCachePathIfNeed();
                        }
                    }
                    break;

                default:
                    break;
                }
                break;

            default:
                break;
            }
        }
    };

    private ServiceConnection mVoiceSerConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "[onServiceConnected]...");
            mVoiceManagerService = IVoiceCommandManagerService.Stub.asInterface(service);
            registerVoiceCommand(mPackageName);
            startVoiceCommand(mPackageName, VoiceCommandListener.ACTION_MAIN_VOICE_COMMON,
                    VoiceCommandListener.ACTION_VOICE_COMMON_PROCESS_STATE, null);
            startVoiceCommand(mPackageName, VoiceCommandListener.ACTION_MAIN_VOICE_COMMON,
                    VoiceCommandListener.ACTION_VOICE_COMMON_COMMAND_PATH, null);
            startVoiceCommand(mPackageName, VoiceCommandListener.ACTION_MAIN_VOICE_COMMON,
                    VoiceCommandListener.ACTION_VOICE_COMMON_KEYWORD, null);

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "[onServiceDisconnected]...");
            mIsRegistered = false;
            mVoiceManagerService = null;
        }
    };

    private void startVoiceCommand(String pkgName, int mainAction, int subAction, Bundle extra) {
        Log.i(TAG, "[startVoiceCommand]pkgName = " + pkgName + ",mainAction = " + mainAction
                + ", subAction = " + subAction + ",extra =  " + extra + ")");
        if (mVoiceManagerService != null) {
            try {
                mVoiceManagerService.sendCommand(pkgName, mainAction, subAction, extra);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void registerVoiceCommand(String pkgName) {
        if (!mIsRegistered) {
            try {
                int errorid = mVoiceManagerService.registerListener(pkgName, mCallback);
                Log.i(TAG, "[registerVoiceCommand]pkgName = " + pkgName + ",errorid = " + errorid);
                if (errorid == VoiceCommandListener.VOICE_NO_ERROR) {
                    mIsRegistered = true;
                } else {
                    Log.w(TAG, "[registerVoiceCommand]Register voice Listener failure ");
                }
            } catch (RemoteException e) {
                mIsRegistered = false;
                mVoiceManagerService = null;
                Log.e(TAG,
                        "[registerVoiceCommand]Register voice Listener RemoteException = "
                                + e.getMessage());
            }
        } else {
            Log.v(TAG, "[registerVoiceCommand]App has register voice listener success");
        }
    }

    private void unRegisterVoiceCommand(String pkgName) {
        try {
            int errorid = mVoiceManagerService.unregisterListener(pkgName, mCallback);
            Log.v(TAG, "[unRegisterVoiceCommand]errorid = " + errorid);
            if (errorid == VoiceCommandListener.VOICE_NO_ERROR) {
                mIsRegistered = false;
            }
        } catch (RemoteException e) {
            Log.e(TAG,
                    "[unRegisterVoiceCommand]Unregister error in handler RemoteException = "
                            + e.getMessage());
            mIsRegistered = false;
            mVoiceManagerService = null;
        }
    }

 // check the cases capture or not.
    private boolean checkNeedCaptureCase() {
        Log.i(TAG, "[checkNeedCaptureCase] mISettingCtrl" + mISettingCtrl);
        boolean isNoNeed = false;
        isNoNeed = !isSupportVoiceCase()
                || mICameraAppUi.getViewState() == ViewState.VIEW_STATE_SETTING
                || mICameraAppUi.getViewState() == ViewState.VIEW_STATE_LOMOEFFECT_SETTING;
        Log.i(TAG, "[checkNeedCaptureCase] isNoNeed = " + isNoNeed);
        return !isNoNeed;
    }

    private boolean checkNeedShowToast() {
        boolean isNeed = false;
        if (mISettingCtrl != null) {
            isNeed = "on".equals(mISettingCtrl.getSettingValue(SettingConstants.KEY_VOICE))
                    && isSupportVoiceCase();
        }
        Log.i(TAG, "[checkNeedShowToast] isNeed" + isNeed);
        return isNeed;
    }

    private boolean isSupportVoiceCase() {
        Log.i(TAG, "[isSupportVoiceCase] mISettingCtrl" + mISettingCtrl);
        boolean isNoNeed = false;
        isNoNeed = !mIsSupportIndicator
                || "on".equals(mISettingCtrl.getSettingValue(SettingConstants.KEY_PANORAMA))
                || "on".equals(mISettingCtrl.getSettingValue(SettingConstants.KEY_VIDEO))
                || "on".equals(mISettingCtrl.getSettingValue(SettingConstants.KEY_VIDEO_PIP));
        Log.i(TAG, "[isSupportVoiceCase] isNoNeed = " + isNoNeed);
        return !isNoNeed;
    }
}
