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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.FileUtils;

import com.mediatek.common.voicecommand.VoiceCommandListener;
import com.mediatek.voicecommand.cfg.ConfigurationXml;
import com.mediatek.voicecommand.cfg.VoiceCustomization;
import com.mediatek.voicecommand.cfg.VoiceKeyWordInfo;
import com.mediatek.voicecommand.cfg.VoiceLanguageInfo;
import com.mediatek.voicecommand.cfg.VoiceProcessInfo;
import com.mediatek.voicecommand.cfg.VoiceWakeupInfo;
import com.mediatek.voicecommand.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

public class ConfigurationManager {
    private static final String TAG = "ConfigurationManager";

    private volatile static ConfigurationManager sCfgMgr = null;

    private final Context mContext;
    private ConfigurationXml mConfigurationXml;

    private final HashMap<String, String> mPathsMap = new HashMap<String, String>();
    private final HashMap<String, VoiceProcessInfo> mVoiceProcessInfosMap = new HashMap<String, VoiceProcessInfo>();
    private final HashMap<String, VoiceKeyWordInfo> mVoiceKeyWordInfosMap = new HashMap<String, VoiceKeyWordInfo>();

    private ArrayList<VoiceLanguageInfo> mVoiceLanguageInfoArrayList = new ArrayList<VoiceLanguageInfo>();
    private ArrayList<String> mVoiceUiFeatureNameArrayList = new ArrayList<String>();
    private ArrayList<VoiceWakeupInfo> mWakeupAnyoneArrayList = new ArrayList<VoiceWakeupInfo>();
    private ArrayList<VoiceWakeupInfo> mWakeupCommandArrayList = new ArrayList<VoiceWakeupInfo>();

    private static final String UI_PATTERN = "UIPattern";
    private static final String UBM_FILE = "UBMFile";
    private static final String MODE_FILE = "ModeFile";
    private static final String CONTACTS_DB_FILE = "ContactsdbFile";
    private static final String CONTACTS_MODE_FILE = "ContactsModeFile";

    private static final String TRAINING_UNLOCK_PATH = "/training/unlock/";
    private static final String TRAINING_ANYONE_PATH = "/training/anyone/";
    private static final String TRAINING_COMMAND_PATH = "/training/command/";

    private static final String RECOG_PATTERN = "recogpattern/";
    private static final String FEATURE_FILE = "featurefile/";
    private static final String PASSWORD_FILE = "passwordfile/";

    private static final String UNLOCK_RECOG_PATTERN = "UnlockRecogPattern";
    private static final String UNLOCK_FEATURE_FILE = "UnlockFeatureFile";
    private static final String UNLOCK_PSWD_FILE = "UnlockPswdFile";

    private static final String ANYONE_RECOG_PATTERN = "AnyoneRecogPattern";
    private static final String ANYONE_FEATURE_FILE = "AnyoneFeatureFile";
    private static final String ANYONE_PSWD_FILE = "AnyonePswdFile";

    private static final String COMMAND_RECOG_PATTERN = "CommandRecogPattern";
    private static final String COMMAND_FEATURE_FILE = "CommandFeatureFile";
    private static final String COMMAND_PSWD_FILE = "CommandPswdFile";

    private static final String WAKE_UP_INFO_PATH = "/wakeupinfo/";
    private static final String WAKE_UP_INFO_FILE = "WakeupinfoFile";
    private static final String WAKE_UP_ANYONE_PATH = "wakeup/anyone.xml";
    private static final String WAKE_UP_COMMAND_PATH = "wakeup/command.xml";

    private static final String VOICE_UI_CACHE_FILE = "com.mediatek.voicecommand_preferences";
    private static final String VOICE_LANGUAGE_CACHE_FILE = "Voice_Language";
    private static final String CUR_LANGUAGE_INDEX = "CurLanguageIndex";
    private static final String IS_FIRST_BOOT = "IsFirstBoot";
    private static final String CUR_SYSTEM_LANGUAGE_INDEX = "CurSystemLanguageIndex";
    private static final String CONTACTS_DB_PATH = "/contacts/";

    private int mWakeupMode = 1;
    private int mWakeupStatus = 0;
    private int mCurrentLanguageIndex = -1;

    private boolean mIsCfgPrepared = true;
    private boolean mIsSystemLanguage = false;

    private VoiceCustomization mVoiceCustomization = new VoiceCustomization();

    /*
     * ConfigurationManager for control configuration
     * 
     * @param context
     */
    private ConfigurationManager(Context context) {
        Log.i(TAG, "[ConfigurationManager]new ...");
        mContext = context.getApplicationContext();
        mConfigurationXml = new ConfigurationXml(context);

        mConfigurationXml.readVoiceProcessInfoFromXml(mVoiceProcessInfosMap,
                mVoiceUiFeatureNameArrayList);
        mConfigurationXml.readVoiceCommandPathFromXml(mPathsMap);
        mCurrentLanguageIndex = mConfigurationXml
                .readVoiceLanguangeFromXml(mVoiceLanguageInfoArrayList);
        mConfigurationXml.readVoiceWakeupFromXml(mWakeupAnyoneArrayList, WAKE_UP_ANYONE_PATH);
        mConfigurationXml.readVoiceWakeupFromXml(mWakeupCommandArrayList, WAKE_UP_COMMAND_PATH);

        String dataDir = context.getApplicationInfo().dataDir;
        mPathsMap.put(CONTACTS_DB_FILE, dataDir + CONTACTS_DB_PATH);

        mPathsMap.put(UNLOCK_RECOG_PATTERN, dataDir + TRAINING_UNLOCK_PATH + RECOG_PATTERN);
        mPathsMap.put(UNLOCK_FEATURE_FILE, dataDir + TRAINING_UNLOCK_PATH + FEATURE_FILE);
        mPathsMap.put(UNLOCK_PSWD_FILE, dataDir + TRAINING_UNLOCK_PATH + PASSWORD_FILE);

        mPathsMap.put(ANYONE_RECOG_PATTERN, dataDir + TRAINING_ANYONE_PATH + RECOG_PATTERN);
        mPathsMap.put(ANYONE_FEATURE_FILE, dataDir + TRAINING_ANYONE_PATH + FEATURE_FILE);
        mPathsMap.put(ANYONE_PSWD_FILE, dataDir + TRAINING_ANYONE_PATH + PASSWORD_FILE);

        mPathsMap.put(COMMAND_RECOG_PATTERN, dataDir + TRAINING_COMMAND_PATH + RECOG_PATTERN);
        mPathsMap.put(COMMAND_FEATURE_FILE, dataDir + TRAINING_COMMAND_PATH + FEATURE_FILE);
        mPathsMap.put(COMMAND_PSWD_FILE, dataDir + TRAINING_COMMAND_PATH + PASSWORD_FILE);

        mPathsMap.put(WAKE_UP_INFO_FILE, dataDir + WAKE_UP_INFO_PATH);

        mConfigurationXml.readVoiceCustomizationFromXml(mVoiceCustomization);

        mIsSystemLanguage = mVoiceCustomization.mIsSystemLanguage;
        if (mIsSystemLanguage) {
            Log.i(TAG, "[ConfigurationManager]new,isSystemLanguage.");
            // Register Local change receiver.
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_LOCALE_CHANGED);
            mContext.registerReceiver(mSystemLanguageReceiver, filter);
        }

        if (!makeDirForPath(mPathsMap.get(UNLOCK_RECOG_PATTERN))
                || !makeDirForPath(mPathsMap.get(UNLOCK_FEATURE_FILE))
                || !makeDirForPath(mPathsMap.get(UNLOCK_PSWD_FILE))
                || !makeDirForPath(mPathsMap.get(ANYONE_RECOG_PATTERN))
                || !makeDirForPath(mPathsMap.get(ANYONE_FEATURE_FILE))
                || !makeDirForPath(mPathsMap.get(ANYONE_PSWD_FILE))
                || !makeDirForPath(mPathsMap.get(COMMAND_RECOG_PATTERN))
                || !makeDirForPath(mPathsMap.get(COMMAND_FEATURE_FILE))
                || !makeDirForPath(mPathsMap.get(COMMAND_PSWD_FILE))
                || !makeDirForPath(mPathsMap.get(WAKE_UP_INFO_FILE))
                || !makeDirForPath(mPathsMap.get(CONTACTS_DB_FILE))) {
            mIsCfgPrepared = false;
        }
        if (mIsCfgPrepared) {
            Log.i(TAG, "[ConfigurationManager]new,mIsCfgPrepared is true.");
            checkVoiceCachePref();
            if (mCurrentLanguageIndex >= 0) {
                mConfigurationXml.readKeyWordFromXml(mVoiceKeyWordInfosMap,
                        mVoiceLanguageInfoArrayList.get(mCurrentLanguageIndex).mFilePath);
            }
        }
    }

    public static ConfigurationManager getInstance(Context context) {
        if (null == sCfgMgr) {
            synchronized (ConfigurationManager.class) {
                if (null == sCfgMgr) {
                    sCfgMgr = new ConfigurationManager(context);
                }
            }

        }
        return sCfgMgr;
    }

    /**
     * Handle the case of using the system language.
     * 
     */
    public void useSystemLanguage() {
        mCurrentLanguageIndex = -1;
        String systemLanguage = Locale.getDefault().getLanguage() + "-"
                + Locale.getDefault().getCountry();

        SharedPreferences languagePref = mContext.getSharedPreferences(VOICE_LANGUAGE_CACHE_FILE,
                Context.MODE_PRIVATE);
        boolean isFirstBoot = languagePref.getBoolean(IS_FIRST_BOOT, true);
        Log.i(TAG, "[useSystemLanguage]isFirstBoot = " + isFirstBoot + ",systemLanguage = "
                + systemLanguage);
        updateCurLanguageIndex(systemLanguage);
        if (mCurrentLanguageIndex < 0) {
            if (isFirstBoot) {
                updateCurLanguageIndex(mVoiceCustomization.mDefaultLanguage);
            } else {
                mCurrentLanguageIndex = languagePref.getInt(CUR_SYSTEM_LANGUAGE_INDEX,
                        mCurrentLanguageIndex);
            }
        }
        if (isFirstBoot) {
            languagePref.edit().putBoolean(IS_FIRST_BOOT, false).apply();
        }
    }

    /**
     * Update mCurrentLanguageIndex.
     * 
     * @param language
     *            local language string
     */
    public void updateCurLanguageIndex(String language) {
        Log.w(TAG, "[updateCurLanguageIndex]language = " + language);
        if (language == null) {
            Log.w(TAG, "[updateCurLanguageIndex]language is null,return.");
            return;
        }
        for (int i = 0; i < mVoiceLanguageInfoArrayList.size(); i++) {
            if (language.equals(mVoiceLanguageInfoArrayList.get(i).mLanguageCode)) {
                mCurrentLanguageIndex = i;
                SharedPreferences languagePref = mContext.getSharedPreferences(
                        VOICE_LANGUAGE_CACHE_FILE, Context.MODE_PRIVATE);
                languagePref.edit().putInt(CUR_SYSTEM_LANGUAGE_INDEX, mCurrentLanguageIndex)
                        .apply();
                break;
            }
        }
    }

    /**
     * Check whether configuration file is prepared.
     */
    public boolean isCfgPrepared() {
        return mIsCfgPrepared;
    }

    public boolean getIsSystemLanguage() {
        return mIsSystemLanguage;
    }

    /**
     * Check whether process is Allowed to register or not.
     * 
     * @param processname
     *            process name
     * @return result
     */
    public int isAllowProcessRegister(String processname) {
        return mVoiceProcessInfosMap.containsKey(processname) ? VoiceCommandListener.VOICE_NO_ERROR
                : VoiceCommandListener.VOICE_ERROR_COMMON_ILLEGAL_PROCESS;
    }

    /**
     * Get the numbers of UiFeature.
     * 
     * @return the numbers of UiFeature
     */
    public int getUiFeatureNumber() {
        return mVoiceUiFeatureNameArrayList.size();
    }

    /**
     * Check whether process has permission to do action or not.
     * 
     * @param featurename
     *            feature or package name of application
     * @param mainaction
     *            main action
     * @return true if has operation permission
     */
    public boolean hasOperationPermission(String featurename, int mainaction) {
        Log.i(TAG, "[hasOperationPermission]featurename = " + featurename);
        VoiceProcessInfo info = mVoiceProcessInfosMap.get(featurename);
        if (info == null) {
            Log.i(TAG, "[hasOperationPermission]no this permission,return false.");
            return false;
        }
        return info.mPermissionIDList.contains(mainaction);
    }

    /**
     * Check whether process has permission to do actions in array or not.
     * 
     * @param featurename
     *            feature or package name of application
     * @param mainaction
     *            main action
     * @return true if has operation permission
     */
    public boolean containOperationPermission(String featurename, int[] mainaction) {
        Log.i(TAG, "[containOperationPermission]featurename = " + featurename);
        if (mainaction == null || mainaction.length == 0) {
            Log.w(TAG, "[containOperationPermission]mainaction = " + mainaction);
            return false;
        }
        VoiceProcessInfo info = mVoiceProcessInfosMap.get(featurename);
        if (info == null) {
            Log.w(TAG, "[containOperationPermission]info is null.");
            return false;
        }
        for (int i = 0; i < mainaction.length; i++) {
            if (info.mPermissionIDList.contains(mainaction[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the feature name list and the process enable list.
     * 
     * @return the feature name list
     */
    public String[] getFeatureNameList() {
        int size = mVoiceUiFeatureNameArrayList.size();
        if (size == 0) {
            Log.w(TAG, "[getFeatureNameList]size is 0.");
            return null;
        }
        return mVoiceUiFeatureNameArrayList.toArray(new String[size]);
    }

    /**
     * Get the feature enable list.
     * 
     * @return the feature enable list
     */
    public int[] getFeatureEnableArray() {
        int size = mVoiceUiFeatureNameArrayList.size();
        if (size == 0) {
            Log.w(TAG, "[getFeatureEnableArray]size is 0.");
            return null;
        }
        int[] isEnableArray = new int[size];

        for (int i = 0; i < size; i++) {
            VoiceProcessInfo info = mVoiceProcessInfosMap.get(mVoiceUiFeatureNameArrayList.get(i));
            if (info != null) {
                isEnableArray[i] = info.mIsVoiceEnable ? 1 : 0;
            }
        }
        return isEnableArray;
    }

    /**
     * Update Feature enable in mVoiceProcessInfos.
     * 
     * @param featurename
     *            application feature name
     * @param enable
     *            true if want to enable
     * @return true if update success
     */
    public boolean updateFeatureEnable(String featurename, boolean enable) {
        Log.i(TAG, "[updateFeatureEnable]featurename = " + featurename + ",enable = " + enable);
        VoiceProcessInfo info = mVoiceProcessInfosMap.get(featurename);
        if (info == null) {
            Log.w(TAG, "[updateFeatureEnable]info is null.");
            return false;
        }

        info.mIsVoiceEnable = enable;
        if (info.mRelationProcessName != null
                && !info.mRelationProcessName.equals(ConfigurationXml.sPublicFeatureName)) {
            updateFeatureEnable(info.mRelationProcessName, enable);
        }
        updateFeatureListEnableToPref();

        return true;
    }

    /**
     * Get language list that this feature support.
     * 
     * @return language list
     */
    public String[] getLanguageList() {
        int size = mVoiceLanguageInfoArrayList.size();
        if (size == 0) {
            Log.w(TAG, "[getLanguageList]size is 0.");
            return null;
        }
        String[] language = new String[size];
        for (int i = 0; i < size; i++) {
            language[i] = mVoiceLanguageInfoArrayList.get(i).mName;
        }

        return language;
    }

    /**
     * Get Current Language.
     * 
     * @return the current Language
     */
    public int getCurrentLanguage() {
        return mCurrentLanguageIndex;
    }

    /**
     * Get Current Language ID defined in voicelanguage.xml.
     * 
     * @return the current Language ID
     */
    public int getCurrentLanguageID() {
        return mCurrentLanguageIndex < 0 ? mCurrentLanguageIndex : mVoiceLanguageInfoArrayList
                .get(mCurrentLanguageIndex).mLanguageID;
    }

    /**
     * Get Current Wakeup Info defined in voiceweakup.xml.
     * 
     * @param mode
     *            wakeup mode
     * @return wakeup information
     */
    public VoiceWakeupInfo[] getCurrentWakeupInfo(int mode) {
        if (VoiceCommandListener.VOICE_WAKEUP_MODE_SPEAKER_INDEPENDENT == mode) {
            if (mWakeupAnyoneArrayList == null) {
                return null;
            }
            return mWakeupAnyoneArrayList
                    .toArray(new VoiceWakeupInfo[mWakeupAnyoneArrayList.size()]);
        }
        if (VoiceCommandListener.VOICE_WAKEUP_MODE_SPEAKER_DEPENDENT == mode) {
            if (mWakeupCommandArrayList == null) {
                return null;
            }
            return mWakeupCommandArrayList.toArray(new VoiceWakeupInfo[mWakeupCommandArrayList
                    .size()]);
        }
        return null;
    }

    /**
     * Get APP Label from package name and class name.
     * 
     * @param packageName
     *            package name
     * @param className
     *            class name
     * @return application label
     */
    public String getAppLabel(String packageName, String className) {

        ComponentName component = new ComponentName(packageName, className);
        ActivityInfo info;
        try {
            info = mContext.getPackageManager().getActivityInfo(component, 0);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "[getAppLabel] not found packageName: " + packageName);
            return null;
        }
        CharSequence name = info.loadLabel(mContext.getPackageManager());
        String appLabel = name.toString();
        Log.i(TAG, "[getAppLabel] appLabel:" + appLabel);

        return appLabel;
    }

    public void updateCurLanguageKeyword() {
        synchronized (mVoiceKeyWordInfosMap) {
            mVoiceKeyWordInfosMap.clear();
            if (mCurrentLanguageIndex >= 0) {
                mConfigurationXml.readKeyWordFromXml(mVoiceKeyWordInfosMap,
                        mVoiceLanguageInfoArrayList.get(mCurrentLanguageIndex).mFilePath);
            }
        }
    }

    /**
     * Set Current Language.
     * 
     * @param languageIndex
     *            the current language index
     */
    public void setCurrentLanguage(int languageIndex) {
        mCurrentLanguageIndex = languageIndex;
        updateCurLanguageToPref();
    }

    /**
     * Get process ID defined in voiceprocessinfo.xml.
     * 
     * @param featurename
     *            application feature name
     * 
     * @return process ID
     */
    public int getProcessID(String featurename) {
        VoiceProcessInfo processinfo = mVoiceProcessInfosMap.get(featurename);
        if (processinfo == null) {
            return -1;
        }

        return processinfo.mID;
    }

    /**
     * Check process is enable or not.
     * 
     * @param featurename
     *            application feature name
     * @return true if the feature name is enable
     */
    public boolean isProcessEnable(String featurename) {
        VoiceProcessInfo processinfo = mVoiceProcessInfosMap.get(featurename);
        if (processinfo == null) {
            return false;
        }
        return processinfo.mIsVoiceEnable;
    }

    /**
     * Get VoiceProcessInfo from processname.
     * 
     * @param processname
     *            process name
     * @return VoiceProcessInfo
     */
    public VoiceProcessInfo getProcessInfo(String processname) {
        return mVoiceProcessInfosMap.get(processname);
    }

    /**
     * Get Voice Recognition Pattern File Path from data/data.
     * 
     * @param mode
     *            Voice Wakeup mode
     * @return data/data/com.mediatek.voicecommand/training/***
     */
    public String getVoiceRecognitionPatternFilePath(int mode) {
        if (VoiceCommandListener.VOICE_WAKEUP_MODE_SPEAKER_INDEPENDENT == mode) {
            return mPathsMap.get(ANYONE_RECOG_PATTERN);
        }
        if (VoiceCommandListener.VOICE_WAKEUP_MODE_SPEAKER_DEPENDENT == mode) {
            return mPathsMap.get(COMMAND_RECOG_PATTERN);
        }
        return mPathsMap.get(UNLOCK_RECOG_PATTERN);
    }

    /**
     * Get Feature File Path from data/data.
     * 
     * @param mode
     *            Voice Wakeup mode
     * @return data/data/com.mediatek.voicecommand/training/***
     */
    public String getFeatureFilePath(int mode) {
        if (VoiceCommandListener.VOICE_WAKEUP_MODE_SPEAKER_INDEPENDENT == mode) {
            return mPathsMap.get(ANYONE_FEATURE_FILE);
        }
        if (VoiceCommandListener.VOICE_WAKEUP_MODE_SPEAKER_DEPENDENT == mode) {
            return mPathsMap.get(COMMAND_FEATURE_FILE);
        }
        return mPathsMap.get(UNLOCK_FEATURE_FILE);
    }

    /**
     * Get Password File Path from data/data.
     * 
     * @param mode
     *            Voice Wakeup mode
     * @return data/data/com.mediatek.voicecommand/training/***
     */
    public String getPasswordFilePath(int mode) {

        if (VoiceCommandListener.VOICE_WAKEUP_MODE_SPEAKER_INDEPENDENT == mode) {
            return mPathsMap.get(ANYONE_PSWD_FILE);
        }
        if (VoiceCommandListener.VOICE_WAKEUP_MODE_SPEAKER_DEPENDENT == mode) {
            return mPathsMap.get(COMMAND_PSWD_FILE);
        }
        return mPathsMap.get(UNLOCK_PSWD_FILE);
    }

    /**
     * Get Voice Wakeup info Path from data/data/com.mediatek.voicecommand.
     * 
     * @return Voice Wakeup info Path
     */
    public String getWakeupInfoPath() {
        return mPathsMap.get(WAKE_UP_INFO_FILE);
    }

    /**
     * Get Voice Wakeup mode from setting provider.
     * 
     * @return Voice Wakeup mode
     */
    public int getWakeupMode() {
        return mWakeupMode;
    }

    /**
     * Set Voice Wakeup mode to setting provider.
     * 
     * @param mode
     *            Voice Wakeup mode
     */
    public void setWakeupMode(int mode) {
        mWakeupMode = mode;
    }

    /**
     * Get Voice Wakeup Status from setting provider.
     * 
     * @return Voice Wakeup Status
     */
    public int getWakeupCmdStatus() {
        return mWakeupStatus;
    }

    /**
     * Set Voice Wakeup Status to setting provider.
     * 
     * @param wakeupStatus
     *            Voice Wakeup Status
     */
    public void setWakeupStatus(int wakeupStatus) {
        mWakeupStatus = wakeupStatus;
    }

    /**
     * Get VoiceUI Pattern Path from voicecommandpath.xml.
     * 
     * @return VoiceUI Pattern Path
     */
    public String getVoiceUIPatternPath() {
        return mPathsMap.get(UI_PATTERN);
    }

    /**
     * Get Contacts db File Path from voicecommandpath.xml.
     * 
     * @return Contacts db File Path
     */
    public String getContactsdbFilePath() {
        return mPathsMap.get(CONTACTS_DB_FILE);
    }

    /**
     * Get UBM File Path from voicecommandpath.xml.
     * 
     * @return UBM File Path
     */
    public String getUbmFilePath() {
        return mPathsMap.get(UBM_FILE);
    }

    /**
     * Get Model File Path from voicecommandpath.xml.
     * 
     * @return Model File Path
     */
    public String getModelFile() {
        return mPathsMap.get(MODE_FILE);
    }

    /**
     * Get Contacts Model File Path from voicecommandpath.xml.
     * 
     * @return Contacts Model File Path
     */
    public String getContactsModelFile() {
        return mPathsMap.get(CONTACTS_MODE_FILE);
    }

    /**
     * Get KeyWord refer to processName defined in keyword.xml.
     * 
     * @param processName
     *            feature name
     * @return KeyWord list
     */
    public String[] getKeyWord(String processName) {
        synchronized (mVoiceKeyWordInfosMap) {
            // return mVoiceKeyWordInfos.get(processName).mKeyWordArray;
            VoiceKeyWordInfo info = mVoiceKeyWordInfosMap.get(processName);
            if (info == null) {
                Log.w(TAG, "[getKeyWord]info is null.1");
                return null;
            }
            return info.mKeyWordArray;
        }
    }

    /**
     * Get command path refer to processName and id defined in keyword.xml.
     * 
     * @param processName
     *            feature name
     * 
     * @return command path
     */
    public String getCommandPath(String processName) {
        synchronized (mVoiceKeyWordInfosMap) {
            // return mVoiceKeyWordInfos.get(processName).mKeyWordPath;
            VoiceKeyWordInfo info = mVoiceKeyWordInfosMap.get(processName);
            if (info == null) {
                Log.w(TAG, "[getCommandPath]info is null.1");
                return null;
            }
            return info.mKeyWordPath;
        }
    }

    /**
     * Get KeyWord refer to processName defined in keyword.xml.
     * 
     * @param processName
     *            feature name
     * @return KeyWord list
     */
    public String[] getKeyWordForSettings(String processName) {
        synchronized (mVoiceKeyWordInfosMap) {
            VoiceProcessInfo info = mVoiceProcessInfosMap.get(processName);
            if (info != null && info.mRelationProcessName != null) {
                ArrayList<String> keywordList = new ArrayList<String>();
                // String[] keyword =
                // mVoiceKeyWordInfos.get(processName).mKeyWordArray;
                VoiceKeyWordInfo keyWordInfo = mVoiceKeyWordInfosMap.get(processName);
                if (keyWordInfo == null) {
                    Log.w(TAG, "[getKeyWordForSettings]keyWordInfo is null.1");
                    return null;
                }
                String[] keyword = keyWordInfo.mKeyWordArray;
                if (keyword != null) {
                    for (int i = 0; i < keyword.length; i++) {
                        keywordList.add(keyword[i]);
                    }
                }
                // String[] keywordRelation =
                // mVoiceKeyWordInfos.get(info.mRelationProcessName).mKeyWordArray;
                keyWordInfo = mVoiceKeyWordInfosMap.get(info.mRelationProcessName);
                if (keyWordInfo == null) {
                    Log.w(TAG, "[getKeyWordForSettings]keyWordInfo is null.2");
                    return null;
                }
                String[] keywordRelation = keyWordInfo.mKeyWordArray;
                if (keywordRelation != null) {
                    for (int i = 0; i < keywordRelation.length; i++) {
                        if (!keywordList.contains(keywordRelation[i])) {
                            keywordList.add(keywordRelation[i]);
                        }
                    }
                }

                return keywordList.toArray(new String[keywordList.size()]);
            } else {
                // return mVoiceKeyWordInfos.get(processName).mKeyWordArray;
                VoiceKeyWordInfo keyWordInfo = mVoiceKeyWordInfosMap.get(processName);
                if (keyWordInfo == null) {
                    Log.w(TAG, "[getKeyWordForSettings]keyWordInfo is null.3");
                    return null;
                }
                return keyWordInfo.mKeyWordArray;
            }
        }
    }

    /**
     * Get Process Name list according to featureName.
     * 
     * @param featureName
     *            feature name
     * @return process name list
     */
    public ArrayList<String> getProcessName(String featureName) {
        VoiceProcessInfo processinfo = mVoiceProcessInfosMap.get(featureName);
        if (processinfo == null) {
            return null;
        }
        return processinfo.mProcessNameList;
    }

    /**
     * ConfigurationManager release when VoiceCommandManagerService destroy.
     */
    public void release() {
        Log.i(TAG, "[release]mIsSystemLanguage : " + mIsSystemLanguage);
        if (mIsSystemLanguage) {
            mContext.unregisterReceiver(mSystemLanguageReceiver);
            sCfgMgr = null;
        }
    }

    /**
     * Create dir and file.
     * 
     * @param path
     *            file path
     * @return true if make dir success
     */
    public static boolean makeDirForPath(String path) {
        if (path == null) {
            Log.w(TAG, "[makeDirForPath]path is null");
            return false;
        }
        try {
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdirs();
                FileUtils.setPermissions(dir.getPath(), 0775, -1, -1); // dwxrwxr-x
            }
        } catch (NullPointerException ex) {
            Log.e(TAG, "[makeDirForPath]NullPointerException.");
            return false;
        }
        return true;
    }

    /**
     * Create dir and file.
     * 
     * @param file
     *            file which want to be made dir
     * @return true if make dir success
     */
    public static boolean makeDirForFile(String file) {
        if (file == null) {
            Log.w(TAG, "[makeDirForFile]file is null");
            return false;
        }
        try {
            File f = new File(file);
            File dir = f.getParentFile();
            if (dir != null && !dir.exists()) {
                dir.mkdirs();
                FileUtils.setPermissions(dir.getPath(), 0775, -1, -1); // dwxrwxr-x
            }
            FileUtils.setPermissions(f.getPath(), 0666, -1, -1); // -rw-rw-rw-
        } catch (NullPointerException ex) {
            Log.e(TAG, "[makeDirForFile]NullPointerException.");
            return false;
        }

        return true;
    }

    /**
     * Read Pref in mVoiceProcessInfos and mCurrentLanguageIndex.
     */
    private void checkVoiceCachePref() {
        SharedPreferences processPref = mContext.getSharedPreferences(VOICE_UI_CACHE_FILE,
                Context.MODE_PRIVATE);
        Map<String, Boolean> enableMap;
        try {
            enableMap = (Map<String, Boolean>) processPref.getAll();
        } catch (NullPointerException ex) {
            Log.e(TAG, "[checkVoiceCachePref] NullPointerException.");
            enableMap = null;
        }

        if (enableMap != null) {
            Iterator iter = enableMap.entrySet().iterator();
            while (iter.hasNext()) {
                Entry entry = (Entry) iter.next();
                String name = (String) entry.getKey();
                VoiceProcessInfo info = mVoiceProcessInfosMap.get(name);
                if (info != null) {
                    info.mIsVoiceEnable = (Boolean) entry.getValue();
                    if (info.mRelationProcessName != null) {
                        VoiceProcessInfo relationInfo = mVoiceProcessInfosMap
                                .get(info.mRelationProcessName);
                        if (relationInfo != null) {
                            relationInfo.mIsVoiceEnable = info.mIsVoiceEnable;
                        }
                    }
                }
            }
        }

        if (mIsSystemLanguage) {
            useSystemLanguage();
        } else {
            processPref = mContext.getSharedPreferences(VOICE_LANGUAGE_CACHE_FILE,
                    Context.MODE_PRIVATE);
            mCurrentLanguageIndex = processPref.getInt(CUR_LANGUAGE_INDEX, mCurrentLanguageIndex);
        }
        Log.d(TAG, "[checkVoiceCachePref]mCurrentLanguageIndex = " + mCurrentLanguageIndex);
    }

    /**
     * Receive Local change broadcast.
     */
    private BroadcastReceiver mSystemLanguageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "[onReceive]...");
            if ((Intent.ACTION_LOCALE_CHANGED).equals(intent.getAction())) {
                String systemLanguage = Locale.getDefault().getLanguage() + "-"
                        + Locale.getDefault().getCountry();
                Log.d(TAG, "[onReceive]mSystemLanguageReceiver systemLanguage : " + systemLanguage);
                updateCurLanguageIndex(systemLanguage);
                updateCurLanguageKeyword();
            }
        }
    };

    /**
     * Save Current Language to Shared Preferences.
     */
    private void updateCurLanguageToPref() {
        Log.d(TAG, "[updateCurLanguageToPref]...");
        synchronized (mVoiceLanguageInfoArrayList) {
            SharedPreferences processPref = mContext.getSharedPreferences(
                    VOICE_LANGUAGE_CACHE_FILE, Context.MODE_PRIVATE);
            processPref.edit().putInt(CUR_LANGUAGE_INDEX, mCurrentLanguageIndex).apply();
        }
        updateCurLanguageKeyword();
    }

    /**
     * Save Feature enable to Shared Preferences.
     */
    private void updateFeatureListEnableToPref() {
        Log.d(TAG, "[updateFeatureListEnableToPref]...");
        synchronized (mVoiceProcessInfosMap) {
            SharedPreferences processPref = mContext.getSharedPreferences(VOICE_UI_CACHE_FILE,
                    Context.MODE_PRIVATE);
            for (String featurename : mVoiceUiFeatureNameArrayList) {
                VoiceProcessInfo info = mVoiceProcessInfosMap.get(featurename);
                if (info != null) {
                    processPref.edit().putBoolean(featurename, info.mIsVoiceEnable).apply();
                }
            }
        }
    }
}