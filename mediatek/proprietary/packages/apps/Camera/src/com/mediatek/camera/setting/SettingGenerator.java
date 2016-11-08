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
package com.mediatek.camera.setting;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.text.TextUtils;
import android.util.FloatMath;

import com.android.camera.ExtensionHelper;
import com.android.camera.R;
//TODO:CamcorderProfileEx
import com.mediatek.camcorder.CamcorderProfileEx;

import com.mediatek.camera.ext.ICameraFeatureExt;
import com.mediatek.camera.ICameraContext;
import com.mediatek.camera.platform.ICameraDeviceManager;
import com.mediatek.camera.platform.ICameraDeviceManager.ICameraDevice;
import com.mediatek.camera.platform.IModuleCtrl;
import com.mediatek.camera.platform.Parameters;
import com.mediatek.camera.setting.preference.ListPreference;
import com.mediatek.camera.setting.preference.PreferenceGroup;
import com.mediatek.camera.setting.preference.PreferenceInflater;
import com.mediatek.camera.setting.preference.SharedPreferencesTransfer;
import com.mediatek.camera.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SettingGenerator {
    private static final String TAG = "SettingGenerator";
    public static boolean isSupport4K2K = false;
    private static final int VIDEO_2K42_WIDTH = 3840;
    public static final String VIDEO_QUALITY_FINE_4K2K = Integer
            .toString(CamcorderProfileEx.QUALITY_FINE_4K2K);
    public static final String SLOW_MOTION_QUALITY_HD_120FPS = Integer
            .toString(CamcorderProfileEx.SLOW_MOTION_HD_120FPS);
    public static final String SLOW_MOTION_QUALITY_HD_180FPS = Integer
            .toString(CamcorderProfileEx.SLOW_MOTION_HD_180FPS);
    public static final String SLOW_MOTION_QUALITY_FHD_120FPS = Integer
            .toString(CamcorderProfileEx.SLOW_MOTION_FHD_120FPS);
    public static final String SLOW_MOTION_QUALITY_VGA_120FPS = Integer
            .toString(CamcorderProfileEx.SLOW_MOTION_VGA_120FPS);
    public static final String SLOW_MOTION_QUALITY_HD_60FPS = Integer
            .toString(CamcorderProfileEx.SLOW_MOTION_HD_60FPS);
    public static final String SLOW_MOTION_QUALITY_FHD_60FPS = Integer
            .toString(CamcorderProfileEx.SLOW_MOTION_FHD_60FPS);
    public static final String SLOW_MOTION_QUALITY_HD_240FPS = Integer
            .toString(CamcorderProfileEx.SLOW_MOTION_HD_240FPS);

    private static final String VIDEO_QUALITY_LOW = Integer
            .toString(CamcorderProfileEx.QUALITY_LOW);
    private static final String VIDEO_QUALITY_MEDIUM = Integer
            .toString(CamcorderProfileEx.QUALITY_MEDIUM);
    private static final String VIDEO_QUALITY_HIGH = Integer
            .toString(CamcorderProfileEx.QUALITY_HIGH);
    private static final String VIDEO_QUALITY_FINE = Integer
            .toString(CamcorderProfileEx.QUALITY_FINE);

    private static final int[]
            SLOW_MOTION_SUPPORT_QUALIYS = new int[] {
        CamcorderProfileEx.SLOW_MOTION_VGA_120FPS,
        CamcorderProfileEx.SLOW_MOTION_HD_60FPS,
        CamcorderProfileEx.SLOW_MOTION_HD_120FPS,
        CamcorderProfileEx.SLOW_MOTION_HD_180FPS,
        CamcorderProfileEx.SLOW_MOTION_FHD_60FPS,
        CamcorderProfileEx.SLOW_MOTION_FHD_120FPS,
        CamcorderProfileEx.SLOW_MOTION_HD_240FPS};

    // SLOW_MOTION_SUPPORT_QUALIY_STRING order should be match with
    // SLOW_MOTION_SUPPORT_QUALIY
    private static final String[]
            SLOW_MOTION_SUPPORT_QUALIYS_STRING = new String[] {
            SLOW_MOTION_QUALITY_VGA_120FPS,
            SLOW_MOTION_QUALITY_HD_60FPS,
            SLOW_MOTION_QUALITY_HD_120FPS,
            SLOW_MOTION_QUALITY_HD_180FPS,
            SLOW_MOTION_QUALITY_FHD_60FPS,
            SLOW_MOTION_QUALITY_FHD_120FPS,
            SLOW_MOTION_QUALITY_HD_240FPS};

    private static final CharSequence[] COLOR_EFFECT_SUPPORT_BY_3RD = new CharSequence[] {
        "none",
        "mono",
        "sepia",
        "negative",
        "solarize",
        "aqua",
        "pastel",
        "mosaic",
        "red-tint",
        "blue-tint",
        "green-tint",
        "blackboard",
        "whiteboard",
        "sepiablue",
        "sepiagreen",
    };

    private static final int NOT_FOUND = -1;
    private static final int NORMAL_RECORD_FPS = 30;
    private static final String DEFAULT_ON = "on";
    private int mCameraId;
    private int mPreferenceRes = 0;

    private ICameraContext mICameraContext;
    private ICameraDeviceManager mICameraDeviceManager;
    private ICameraDevice mICameraDevice;
    private IModuleCtrl mIModuleCtrl;

    private CameraInfo[] mCameraInfo;
    private List<String> mSupportedImageProperties;
    private List<String> mSupportedFaceBeautyProperties;
    private List<String> mSupportedDualCamera = new ArrayList<String>();

    private Context mContext;
    private SharedPreferencesTransfer mPrefTransfer;
    private PreferenceInflater mInflater;

    private ArrayList<SettingItem> mSettingList = new ArrayList<SettingItem>();
    private HashMap<Integer, PreferenceGroup> mPreferencesGroupMap;
    private HashMap<Integer, ArrayList<ListPreference>> mPreferencesMap;
    private HashMap<Integer, ArrayList<SettingItem>> mSettingItemsMap;

    public SettingGenerator(ICameraContext cameraContext, SharedPreferencesTransfer prefTransfer) {
        mICameraContext = cameraContext;
        mContext = cameraContext.getActivity();
        mICameraDeviceManager = cameraContext.getCameraDeviceManager();
        mIModuleCtrl = cameraContext.getModuleController();
        mPrefTransfer = prefTransfer;
        mCameraInfo = mICameraDeviceManager.getCameraInfo();
        mCameraId = mICameraDeviceManager.getCurrentCameraId();
        mICameraDevice = mICameraDeviceManager.getCameraDevice(mCameraId);

        int cameraCounts = mICameraDeviceManager.getNumberOfCameras();
        mPreferencesGroupMap = new HashMap<Integer, PreferenceGroup>(cameraCounts);
        mPreferencesMap = new HashMap<Integer, ArrayList<ListPreference>>(cameraCounts);
        mSettingItemsMap = new HashMap<Integer, ArrayList<SettingItem>>(cameraCounts);
    }

    /**
     * Create all the setting objects
     *
     * @param group
     *            the group contain setting listPreference.
     */
    public void createSettings(int preferenceRes) {
        mPreferenceRes = preferenceRes;
        mInflater = new PreferenceInflater(mContext, mPrefTransfer);
        int currentCameraId = mICameraDeviceManager.getCurrentCameraId();
        PreferenceGroup group = (PreferenceGroup) mInflater.inflate(preferenceRes);
        mPreferencesGroupMap.put(currentCameraId, group);
        createSettingItems();
        createPreferences(group, currentCameraId);
    }

    public void updatePreferences() {
        int currentCameraId = mICameraDeviceManager.getCurrentCameraId();
        mICameraDevice = mICameraDeviceManager.getCameraDevice(currentCameraId);
        mCameraId = currentCameraId;
        // get all list preference which current camera supports.
        ArrayList<ListPreference> preferences = mPreferencesMap.get(currentCameraId);
        Log.i(TAG, "[updatePreferences], currentCameraId:" + currentCameraId + ", "
                + "preferences:" + preferences);
        if (preferences == null) {
            PreferenceGroup group = (PreferenceGroup) mInflater.inflate(mPreferenceRes);
            mPreferencesGroupMap.put(currentCameraId, group);
            createPreferences(group, currentCameraId);
        } else {
            ArrayList<SettingItem> settingItems = mSettingItemsMap.get(currentCameraId);
            for (int i = 0; i < preferences.size(); i++) {
                SettingItem settingItem = settingItems.get(i);
                ListPreference preference = preferences.get(i);
                updateSettingItem(settingItem, preference);
                settingItem.clearAllOverrideRecord();
                settingItem.setLastValue(settingItem.getDefaultValue());
                if (preference != null) {
                    preference.setOverrideValue(null);
                }
            }
            
            SettingItem picRatioSetting = getSettingItem(SettingConstants.ROW_SETTING_PICTURE_RATIO, mCameraId);
            if (picRatioSetting != null) {
                picRatioSetting.setLastValue(null);
            }
            
            SettingItem captureModeSetting = getSettingItem(SettingConstants.ROW_SETTING_CAPTURE_MODE, mCameraId);
            if (captureModeSetting != null) {
                captureModeSetting.setLastValue(null);
            }
            
            SettingItem recordingHintSetting = getSettingItem(SettingConstants.ROW_SETTING_RECORDING_HINT, mCameraId);
            if (recordingHintSetting != null) {
                recordingHintSetting.setLastValue(null);
            }
            
            overrideSettingByIntent();
        }
    }

    public SettingItem getSettingItem(String key) {
        int settingId = SettingConstants.getSettingId(key);
        return getSettingItem(settingId);
    }

    public SettingItem getSettingItem(int settingId) {
        int currentCameraId = mICameraDeviceManager.getCurrentCameraId();
        return getSettingItem(settingId, currentCameraId);
    }

    public SettingItem getSettingItem(int settingId, int cameraId) {
        ArrayList<SettingItem> settingItems = mSettingItemsMap.get(cameraId);
        if (settingItems == null) {
            return null;
        }
        return settingItems.get(settingId);
    }

    public PreferenceGroup getPreferenceGroup() {
        int currentCameraId = mICameraDeviceManager.getCurrentCameraId();
        return mPreferencesGroupMap.get(currentCameraId);
    }

    public ListPreference getListPreference(int row) {
        int currentCameraId = mICameraDeviceManager.getCurrentCameraId();
        ArrayList<ListPreference> preferences = mPreferencesMap.get(currentCameraId);
        if (preferences == null) {
            Log.e(TAG, "Call setting before setting updated, return null");
            return null;
        }
        return preferences.get(row);
    }

    public ListPreference getListPreference(String key) {
        int settingId = SettingConstants.getSettingId(key);
        return getListPreference(settingId);
    }

    public void restoreSetting(int cameraId) {
        ArrayList<SettingItem> settingItems = mSettingItemsMap.get(cameraId);
        if (settingItems != null) {
            for (int i = 0; i < settingItems.size(); i++) {
                SettingItem settingItem = settingItems.get(i);
                settingItem.setValue(settingItem.getDefaultValue());
            }
        }

        ArrayList<ListPreference> preferences = mPreferencesMap.get(cameraId);
        if (preferences != null) {
            for (int i = 0; i < preferences.size(); i++) {
                ListPreference pref = preferences.get(i);
                if (pref != null) {
                    pref.setOverrideValue(null, false);
                }
            }
        }

        // need update picture ratio setting as full ratio.
        SharedPreferences sharePreferences = mPrefTransfer.getSharedPreferences(SettingConstants.KEY_PICTURE_RATIO);
        SettingItem pictureRatioSetting = getSettingItem(SettingConstants.KEY_PICTURE_RATIO);
        ListPreference pictureRatioPref = pictureRatioSetting.getListPreference();
        if (pictureRatioPref != null) {
            String ratio = sharePreferences.getString(SettingConstants.KEY_PICTURE_RATIO,
                    String.valueOf(4d / 3));
            pictureRatioPref.setValue(ratio);
            pictureRatioSetting.setValue(ratio);
            pictureRatioSetting.setDefaultValue(ratio);
        }

        List<String> supportedFBMode = getSupportedFaceBeautyMode();
        if (supportedFBMode != null && supportedFBMode.size() > 0) {
            String defaultFBMode = supportedFBMode.get(0);
            SettingItem fbModeSetting = getSettingItem(SettingConstants.KEY_MULTI_FACE_BEAUTY);
            fbModeSetting.setValue(defaultFBMode);
            fbModeSetting.setDefaultValue(defaultFBMode);
        }
        
        SettingItem captureModeSetting = getSettingItem(SettingConstants.KEY_CAPTURE_MODE);
        if (captureModeSetting != null) {
            captureModeSetting.setLastValue(null);
        }
        
        overrideSettingByIntent();
    }

    private void createSettingItems() {
        int cameraCounts = mICameraDeviceManager.getNumberOfCameras();
        for (int i = 0; i < cameraCounts; i++) {
            ArrayList<SettingItem> settingItems = new ArrayList<SettingItem>();
            for (int settingId = 0; settingId < SettingConstants.SETTING_COUNT; settingId++) {
                SettingItem settingItem = new SettingItem(settingId);
                String key = SettingConstants.getSettingKey(settingId);
                int type = SettingConstants.getSettingType(settingId);
                settingItem.setKey(key);
                settingItem.setType(type);
                settingItems.add(settingItem);
            }
            mSettingItemsMap.put(i, settingItems);
        }

    }

    private void createPreferences(PreferenceGroup group, int cameraId) {
        Log.i(TAG, "[createPreferences], cameraId:" + cameraId + ", group:" + group);
        ArrayList<ListPreference> preferences = mPreferencesMap.get(cameraId);
        mSupportedImageProperties = new ArrayList<String>();
        mSupportedFaceBeautyProperties = new ArrayList<String>();
        if (preferences == null) {
            preferences = new ArrayList<ListPreference>();
            ArrayList<SettingItem> settingItems = mSettingItemsMap.get(cameraId);
            for (int settingId = 0; settingId < SettingConstants.SETTING_COUNT; settingId++) {
                String key = SettingConstants.getSettingKey(settingId);
                ListPreference preference = group.findPreference(key);

                preferences.add(preference);

                SettingItem settingItem = settingItems.get(settingId);
                settingItem.setListPreference(preference);
            }
            mPreferencesMap.put(cameraId, preferences);
        }
        // every camera maintain one setting item list.
        filterPreferences(preferences, cameraId);
    }

    private void filterPreferences(ArrayList<ListPreference> preferences, int cameraId) {
        ArrayList<SettingItem> settingItems = mSettingItemsMap.get(cameraId);
        limitPreferencesByIntent();
        for (int i = 0; i < preferences.size(); i++) {
            // filter list preference.
            ListPreference preference = preferences.get(i);
            boolean isRemove = filterPreference(preference);
            if (isRemove) {
                preference = null;
                preferences.set(i, null);
            }
            // update setting's value and default value.
            SettingItem settingItem = settingItems.get(i);
            updateSettingItem(settingItem, preference);
        }

        overrideSettingByIntent();
    }

    /**
     * Update setting's value and default value.
     *
     * @param settingItem
     *            setting instance{@link SettingItem}.
     * @param preference
     *            setting's preference {@link ListPreference}.
     */
    private void updateSettingItem(SettingItem settingItem, ListPreference preference) {
        int settingId = settingItem.getSettingId();
        int type = SettingConstants.getSettingType(settingId);
        String defaultValue = settingItem.getDefaultValue();
        switch (type) {
        case SettingConstants.NEITHER_IN_PARAMETER_NOR_IN_PREFERENCE:
        case SettingConstants.ONLY_IN_PARAMETER:
            // set setting default value and value, the value is initialized to
            // default value.
            defaultValue = SettingDataBase.getDefaultValue(settingId);
            settingItem.setDefaultValue(defaultValue);
            settingItem.setValue(defaultValue);
            break;

        case SettingConstants.BOTH_IN_PARAMETER_AND_PREFERENCE:
        case SettingConstants.ONLY_IN_PEFERENCE:
            // if setting has preferences, its default value and value get from
            // preference.
            if (preference != null) {
                preference.reloadValue();
                // these setting do not have default value in xml.
                if (settingItem.getKey().equals(SettingConstants.KEY_PICTURE_RATIO)
                        || settingItem.getKey().equals(SettingConstants.KEY_PICTURE_SIZE)
                        || settingItem.getKey().equals(SettingConstants.KEY_MULTI_FACE_BEAUTY)) {
                    if (settingItem.getDefaultValue() == null) {
                        defaultValue = preference.getValue();
                    }
                } else {
                    defaultValue = preference.getDefaultValue();
                }
                settingItem.setDefaultValue(defaultValue);
                settingItem.setValue(preference.getValue());
                Log.i(TAG, "[updateSettingItem] key:" + settingItem.getKey() + ", defaultValue:"
                        + defaultValue + ", value:" + settingItem.getValue());
            } else {
                settingItem.setEnable(false);
                Log.i(TAG, "[updateSettingItem] preference is null, key:" + settingItem.getKey());
            }
            break;
        default:
            break;
        }
    }

    private boolean filterPreference(final ListPreference preference) {
        Parameters parameters = mICameraDevice.getParameters();
        String key = null;
        int settingId = -1;
        boolean removePreference = false;
        if (preference != null) {
            key = preference.getKey();
            settingId = SettingConstants.getSettingId(key);
        }

        switch (settingId) {

        case SettingConstants.ROW_SETTING_FLASH:
        case SettingConstants.ROW_SETTING_ANTI_FLICKER:
        case SettingConstants.ROW_SETTING_ISO:
        case SettingConstants.ROW_SETTING_AIS:
        case SettingConstants.ROW_SETTING_3DNR:
            removePreference = filterUnsupportedOptions(preference,
                    ParametersHelper.getParametersSupportedValues(parameters, key), settingId);
            break;

        case SettingConstants.ROW_SETTING_COLOR_EFFECT:
            boolean isNonePickIntent = mIModuleCtrl.isNonePickIntent();
            if (!isNonePickIntent) {
                preference.setOriginalEntryValues(COLOR_EFFECT_SUPPORT_BY_3RD);
            }
            removePreference = filterUnsupportedOptions(preference,
                    ParametersHelper.getParametersSupportedValues(parameters, key), settingId);
            break;

        case SettingConstants.ROW_SETTING_DUAL_CAMERA:
            removePreference = buildCameraId(preference, settingId);
            break;

        case SettingConstants.ROW_SETTING_EXPOSURE:
            removePreference = buildExposureCompensation(preference, settingId);
            break;

        case SettingConstants.ROW_SETTING_SCENCE_MODE:
        case SettingConstants.ROW_SETTING_WHITE_BALANCE:
            updateSettingItem(SettingConstants.getSettingKey(settingId), preference);
            removePreference = filterUnsupportedOptions(preference,
                    ParametersHelper.getParametersSupportedValues(parameters, key), settingId);
            break;

        case SettingConstants.ROW_SETTING_SHARPNESS:
        case SettingConstants.ROW_SETTING_HUE:
        case SettingConstants.ROW_SETTING_SATURATION:
        case SettingConstants.ROW_SETTING_BRIGHTNESS:
        case SettingConstants.ROW_SETTING_CONTRAST:
            removePreference = filterUnsupportedOptions(preference,
                    ParametersHelper.getParametersSupportedValues(parameters, key), settingId);
            if (!removePreference) {
                buildSupportedListperference(mSupportedImageProperties, preference);
            }
            break;

        case SettingConstants.ROW_SETTING_IMAGE_PROPERTIES:
            removePreference = filterUnsupportedEntries(preference, mSupportedImageProperties,
                    true, settingId);
            break;

        case SettingConstants.ROW_SETTING_ZSD:
            if (mICameraContext.getFeatureConfig().isLowRamOptSupport()) {
                removePreference = true;
            } else {
                removePreference = filterUnsupportedOptions(preference,
                        ParametersHelper.getParametersSupportedValues(parameters, key), settingId);
            }
            break;

        case SettingConstants.ROW_SETTING_FACEBEAUTY_PROPERTIES:
            if (mICameraContext.getFeatureConfig().isVfbEnable()
                    || (!mICameraContext.getFeatureConfig().isVfbEnable()
                    && !ParametersHelper.isCfbSupported(parameters))) {
                removePreference = true;
            } else {
                removePreference = filterUnsupportedEntries(preference, mSupportedFaceBeautyProperties,
                        true, settingId);
            }

            break;

        case SettingConstants.ROW_SETTING_FACEBEAUTY_SMOOTH:
            removePreference = buildFaceBeautyPreference(ParametersHelper.KEY_FACEBEAUTY_SMOOTH,
                    preference, settingId);
            if (!removePreference) {
                buildSupportedListperference(mSupportedFaceBeautyProperties, preference);
            }
            break;

        case SettingConstants.ROW_SETTING_FACEBEAUTY_SKIN_COLOR:
            removePreference = buildFaceBeautyPreference(
                    ParametersHelper.KEY_FACEBEAUTY_SKIN_COLOR, preference, settingId);
            if (!removePreference) {
                buildSupportedListperference(mSupportedFaceBeautyProperties, preference);
            }
            break;
        case SettingConstants.ROW_SETTING_FACEBEAUTY_SHARP:
            removePreference = buildFaceBeautyPreference(ParametersHelper.KEY_FACEBEAUTY_SHARP,
                    preference, settingId);
            if (!removePreference) {
                buildSupportedListperference(mSupportedFaceBeautyProperties, preference);
            }
            break;
        case SettingConstants.ROW_SETTING_CAMERA_FACE_DETECT:
            break;
        case SettingConstants.ROW_SETTING_RECORD_LOCATION:
            break;
        case SettingConstants.ROW_SETTING_MICROPHONE:
            break;
        case SettingConstants.ROW_SETTING_AUDIO_MODE:
            break;
        case SettingConstants.ROW_SETTING_SELF_TIMER:
            break;

        case SettingConstants.ROW_SETTING_TIME_LAPSE:
            resetIfInvalid(preference);
            break;

        // TODO need break ?
        case SettingConstants.ROW_SETTING_VIDEO_QUALITY:// video
            removePreference = filterUnsupportedOptions(preference, getMTKSupportedVideoQuality(),
                    settingId);
            break;

        case SettingConstants.ROW_SETTING_SLOW_MOTION_VIDEO_QUALITY:
            removePreference = filterUnsupportedOptions(preference,
                    getMTKSupportedSlowMotionVideoQuality(), settingId);
            break;

        case SettingConstants.ROW_SETTING_CONTINUOUS_NUM:
            if (mICameraContext.getFeatureConfig().isLowRamOptSupport() || 
                    ParametersHelper.getParametersSupportedValues(parameters,
                            SettingConstants.KEY_CAPTURE_MODE)
                            .indexOf(Parameters.CAPTURE_MODE_CONTINUOUS_SHOT) <= 0) {
                removePreference = true;
            }
            break;

        case SettingConstants.ROW_SETTING_SLOW_MOTION:
            if (!mICameraContext.getFeatureConfig().isSlowMotionSupport()
                    || getMTKSupportedSlowMotionVideoQuality().size() < 1
                    || getMaxPreviewFrameRate() <= NORMAL_RECORD_FPS) {
                removePreference = true;
            }
            break;

        case SettingConstants.ROW_SETTING_PICTURE_RATIO:
            List<String> supportedRatios = SettingUtils.buildPreviewRatios(mContext, parameters);
            removePreference = filterUnsupportedOptions(preference, supportedRatios, settingId);
            break;

        case SettingConstants.ROW_SETTING_PICTURE_SIZE:// camera special case
            // M: filter supported values.
            List<String> supportedPictureSizes = sizeListToStringList(parameters
                    .getSupportedPictureSizes());
            //may be third party will limit the picture sizes.
            int limitedResolution = SettingUtils.getLimitResolution();
            if (limitedResolution > 0) {
                SettingUtils.filterLimitResolution(supportedPictureSizes);
            }
            
            removePreference = filterUnsupportedOptions(preference, supportedPictureSizes, false,
                    settingId);
            // M: for picture size was ordered, here we don't set it to index 0.
            ListPreference pictureRatioPref = getListPreference(SettingConstants.ROW_SETTING_PICTURE_RATIO);
            String pictureRatio = SettingUtils.getRatioString(4d / 3);
            if (pictureRatioPref != null) {
                pictureRatio = pictureRatioPref.getValue();
            }
            List<String> supportedForRatio = SettingUtils
                    .buildSupportedPictureSizeByRatio(parameters, pictureRatio);
            removePreference = filterDisabledOptions(preference, supportedForRatio, false,
                    settingId);
            break;

        case SettingConstants.ROW_SETTING_VOICE:
            if (!mICameraContext.getFeatureConfig().isVoiceUiSupport() || !mIModuleCtrl.isNonePickIntent()) {
                removePreference = true;
            }
            break;

        case SettingConstants.ROW_SETTING_HDR:
            if (ParametersHelper.getParametersSupportedValues(parameters,
                    SettingConstants.KEY_SCENE_MODE).indexOf(Parameters.SCENE_MODE_HDR) <= 0) {
                removePreference = true;
            }
            break;

        case SettingConstants.ROW_SETTING_MULTI_FACE_MODE:
            // add for vFB
            if (mICameraContext.getFeatureConfig().isVfbEnable()) {
                filterUnsupportedOptions(preference, getSupportedFaceBeautyMode(),true, settingId);
            } else {
                removePreference = true;
            }

            break;

        case SettingConstants.ROW_SETTING_STEREO_MODE:
            // if (!FeatureSwitcher.isStereo3dEnable()) {
            removePreference = true;
            // }
            break;

        case SettingConstants.ROW_SETTING_SMILE_SHOT:
            if (ParametersHelper.getParametersSupportedValues(parameters,
                    SettingConstants.KEY_CAPTURE_MODE).indexOf(Parameters.CAPTURE_MODE_SMILE_SHOT) <= 0) {
                removePreference = true;
            }
            break;

        case SettingConstants.ROW_SETTING_ASD:
            if (ParametersHelper.getParametersSupportedValues(parameters,
                    SettingConstants.KEY_CAPTURE_MODE).indexOf(Parameters.CAPTURE_MODE_ASD) <= 0) {
                removePreference = true;
            }
            break;

        case SettingConstants.ROW_SETTING_GESTURE_SHOT:
            if (!ParametersHelper.isGestureShotSupported(parameters)) {
                removePreference = true;
            }
            break;

        case SettingConstants.ROW_SETTING_FAST_AF:
            if (ParametersHelper.isDepthAfSupported(parameters) && mIModuleCtrl.isNonePickIntent()) {
                resetIfInvalid(preference, true);
                mSupportedDualCamera.add(key);
            } else {
                removePreference = true;
            }
            break;

        case SettingConstants.ROW_SETTING_DISTANCE:
            if (ParametersHelper.isDistanceInfoSuppported(parameters) && mIModuleCtrl.isNonePickIntent()) {
                resetIfInvalid(preference, true);
                mSupportedDualCamera.add(key);
            } else {
                removePreference = true;
            }
            break;
            
        case SettingConstants.ROW_SETTING_VIDEO_STABLE:
            if (!"true".equals(ParametersHelper.getParametersValue(parameters,SettingConstants.KEY_VIDEO_EIS))) {
                removePreference = true;
            }
            break;
            
        case SettingConstants.ROW_SETTING_DUAL_CAMERA_MODE:
            if ((!ParametersHelper.isDepthAfSupported(parameters) 
                    && !ParametersHelper.isDistanceInfoSuppported(parameters)) || !mIModuleCtrl.isNonePickIntent()) {
                removePreference = true;
            } 
            break;
            
        case SettingConstants.ROW_SETTING_HEARTBEAT_MONITOR:
            if (!ParametersHelper.isHeartbeatMonitorSupported(parameters)) {
                removePreference = true;
            }
            break;
            
        case SettingConstants.ROW_SETTING_DNG:
            if (!ParametersHelper.isDngSupported(parameters)) {
                removePreference = true;
            }
            break;

        default:
            break;
        }
        Log.i(TAG, "[filterPreference], key:" + key + ", " + "removePreference:" + removePreference);
        return removePreference;
    }

    private void limitPreferencesByIntent() {
        boolean isNonePickIntent = mIModuleCtrl.isNonePickIntent();
        if (!isNonePickIntent) {
            int currentCameraId = mICameraDeviceManager.getCurrentCameraId();
            ArrayList<ListPreference> preferences = mPreferencesMap.get(currentCameraId);
            int[] unSupportedBy3rdParty = SettingConstants.UN_SUPPORT_BY_3RDPARTY;
            for (int i = 0; i < unSupportedBy3rdParty.length; i++) {
                preferences.set(unSupportedBy3rdParty[i], null);
            }
        }

        int currentCameraId = mICameraDeviceManager.getCurrentCameraId();
        Log.i(TAG, "currentCameraId:" + currentCameraId + ", frontCameraId:" + mICameraDeviceManager.getFrontCameraId());
        if (currentCameraId == mICameraDeviceManager.getFrontCameraId()) {
            ArrayList<ListPreference> preferences = mPreferencesMap.get(currentCameraId);
            int[] unSupportedByFrontCamera = SettingConstants.UN_SUPPORT_BY_FRONT_CAMERA;
            for (int i = 0; i < unSupportedByFrontCamera.length; i++) {
                preferences.set(unSupportedByFrontCamera[i], null);
            }
        }
    }

    private void overrideSettingByIntent() {
        if (mIModuleCtrl.isImageCaptureIntent()) {
            ListPreference zsd = getListPreference(SettingConstants.KEY_CAMERA_ZSD);
            if (zsd != null) {
                zsd.setOverrideValue("off");
                SettingItem zsdSetting = getSettingItem(SettingConstants.KEY_CAMERA_ZSD);
                zsdSetting.setValue("off");
                zsdSetting.setEnable(false);
            }

        }
    }

    private void updateSettingItem(String key, ListPreference item) {
        if (item == null) {
            return;
        }
        ICameraFeatureExt updateString =  ExtensionHelper.getCameraFeatureExtension(mContext);
        CharSequence[] entries = item.getEntries();
        CharSequence[] entryValues = item.getEntryValues();
        int length = entries.length;
        ArrayList<CharSequence> newEntries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> newEntryValues = new ArrayList<CharSequence>();
        for (int i = 0; i < length; i++) {
            newEntries.add(entries[i]);
            newEntryValues.add(entryValues[i]);
        }
        updateString.updateSettingItem(key, newEntries, newEntryValues);
        length = newEntryValues.size();

        item.setOriginalEntryValues(newEntryValues.toArray(new CharSequence[length]));
        item.setOriginalEntries(newEntries.toArray(new CharSequence[length]));
    }

    private boolean filterUnsupportedOptions(ListPreference pref, List<String> supported, int row) {
        return filterUnsupportedOptions(pref, supported, true, row);
    }

    private boolean filterUnsupportedOptions(ListPreference pref, List<String> supported,
            boolean resetFirst, int row) {
        if (supported != null) {
            pref.filterUnsupported(supported);
        }

        if (pref.getEntryValues().length == 1) {
            SettingItem settingItem = getSettingItem(row);
            CharSequence[] values = pref.getEntryValues();
            settingItem.setDefaultValue(values[0].toString());
            settingItem.setValue(values[0].toString());
        }

        // Remove the preference if the parameter is not supported or there is
        // only one options for the settings.
        if (supported == null || supported.size() <= 1) {
            return true;
        }

        if (pref.getEntries().length <= 1) {
            return true;
        }
        resetIfInvalid(pref, resetFirst);
        return false;
    }

    // add for filter unsupported image properties
    // image properties just can be filtered by entries
    private boolean filterUnsupportedEntries(ListPreference pref, List<String> supported,
            boolean resetFirst, int row) {
        if (supported == null || supported.size() <= 0) {
            return true;
        }
        pref.filterUnsupportedEntries(supported);
        if (pref.getEntries().length <= 0) {
            return true;
        }
        resetIfInvalid(pref, resetFirst);
        return false;
    }

    private void buildSupportedListperference(List<String> supportedList, ListPreference list) {
        if (list != null && supportedList != null) {
            supportedList.add(list.getKey());
        }
    }

    private boolean filterDisabledOptions(ListPreference pref, List<String> supported,
            boolean resetFirst, int row) {

        // Remove the preference if the parameter is not supported or there is
        // only one options for the settings.
        if (supported == null || supported.size() < 1) {
            return true;
        }

        pref.filterDisabled(supported);
        if (pref.getEntries().length < 1) {
            return true;
        }

        resetIfInvalid(pref, resetFirst);
        return false;
    }

    private void resetIfInvalid(ListPreference pref) {
        resetIfInvalid(pref, true);
    }

    private void resetIfInvalid(ListPreference pref, boolean first) {
        // Set the value to the first entry if it is invalid.
        String value = pref.getValue();
        if (pref.findIndexOfValue(value) == NOT_FOUND) {
            if (first) {
                pref.setValueIndex(0);
            } else if (pref.getEntryValues() != null && pref.getEntryValues().length > 0) {
                pref.setValueIndex(pref.getEntryValues().length - 1);
            }
        }
    }

    private static List<String> sizeListToStringList(List<Size> sizes) {
        ArrayList<String> list = new ArrayList<String>();
        for (Size size : sizes) {
            list.add(String.format(Locale.ENGLISH, "%dx%d", size.width, size.height));
        }
        return list;
    }

    private boolean buildCameraId(ListPreference preference, int row) {
        int numOfCameras = mCameraInfo.length;
        if (numOfCameras < 2) {
            return true;
        }

        CharSequence[] entryValues = new CharSequence[2];
        for (int i = 0; i < mCameraInfo.length; ++i) {
            int index = (mCameraInfo[i].facing == CameraInfo.CAMERA_FACING_FRONT) ? CameraInfo.CAMERA_FACING_FRONT
                    : CameraInfo.CAMERA_FACING_BACK;
            if (entryValues[index] == null) {
                entryValues[index] = "" + i;
                if (entryValues[((index == 1) ? 0 : 1)] != null) {
                    break;
                }
            }
        }
        preference.setEntryValues(entryValues);
        return false;
    }

    private ArrayList<String> getMTKSupportedVideoQuality() {
        ArrayList<String> supported = new ArrayList<String>();
        // Check for supported quality, pip mode always check main camera's
        // quality
        int cameraId = /*
                        * mContext.isCurrentPIPEnable() ?
                        * CameraHolder.instance().getBackCameraId() :
                        */mCameraId;
        if (CamcorderProfile.hasProfile(cameraId, CamcorderProfileEx.QUALITY_LOW)) {
            supported.add(VIDEO_QUALITY_LOW);
        }
        if (CamcorderProfile.hasProfile(cameraId, CamcorderProfileEx.QUALITY_MEDIUM)) {
            supported.add(VIDEO_QUALITY_MEDIUM);
        }
        if (CamcorderProfile.hasProfile(cameraId, CamcorderProfileEx.QUALITY_HIGH)) {
            supported.add(VIDEO_QUALITY_HIGH);
        }
        if (CamcorderProfile.hasProfile(cameraId, CamcorderProfileEx.QUALITY_FINE)) {
            supported.add(VIDEO_QUALITY_FINE);
        }
        if (CamcorderProfile.hasProfile(cameraId, CamcorderProfileEx.QUALITY_FINE_4K2K)) {
            List<Size> sizes = mICameraDevice.getParameters().getSupportedVideoSizes();
            Iterator<Size> it = sizes.iterator();
            boolean support = false;
            while (it.hasNext()) {
                Size size = it.next();
                if (size.width >= VIDEO_2K42_WIDTH) {
                    support = true;
                    isSupport4K2K = true;
                    break;
                }
            }
            if (support) {
                supported.add(VIDEO_QUALITY_FINE_4K2K);
            }
        }

        return supported;
    }

    // should be refactored for icons
    private boolean buildExposureCompensation(ListPreference exposure, int row) {
        Parameters parameters = mICameraDevice.getParameters();
        int max = parameters.getMaxExposureCompensation();
        int min = parameters.getMinExposureCompensation();
        if (max == 0 && min == 0) {
            return true;
        }
        float step = parameters.getExposureCompensationStep();

        // show only integer values for exposure compensation
        int maxValue = (int) FloatMath.floor(max * step);
        int minValue = (int) FloatMath.ceil(min * step);
        ArrayList<String> entryValuesList = new ArrayList<String>();
        for (int i = minValue; i <= maxValue; ++i) {
            String value = Integer.toString(Math.round(i / step));
            entryValuesList.add(String.valueOf(value));
        }
        exposure.filterUnsupported(entryValuesList);
        return false;
    }

    /*
     * private boolean removePreference(PreferenceGroup group, String key, int
     * row) { for (int i = 0, n = group.size(); i < n; i++) { CameraPreference
     * child = group.get(i); if (child instanceof PreferenceGroup) { if
     * (removePreference((PreferenceGroup) child, key, row)) { return true; } }
     * if (child instanceof ListPreference && ((ListPreference)
     * child).getKey().equals(key)) { group.removePreference(i);
     * removeListPreference(row); return true; } } return false; }
     */

    // should be refactored for icons
    private boolean buildFaceBeautyPreference(String key, ListPreference fbPreference, int row) {
        Parameters parameters = mICameraDevice.getParameters();
        int max = ParametersHelper.getMaxLevel(parameters, key);
        int min = ParametersHelper.getMinLevel(parameters, key);
        if (max == 0 && min == 0) {
            // removePreference(mPreferenceGroup, fbPreference.getKey(), row);
            return true;
        }
        
        CharSequence[] faceBeautyValue = new CharSequence[] {
                String.valueOf(min),
                String.valueOf(0),
                String.valueOf(max),
        };
        fbPreference.setEntryValues(faceBeautyValue);
        return false;
    }

    private ArrayList<String> getMTKSupportedSlowMotionVideoQuality() {
        ArrayList<String> supported = new ArrayList<String>();
        if (!mIModuleCtrl.isNonePickIntent()
                || !mICameraContext.getFeatureConfig().isSlowMotionSupport()) {
            return supported;
        }
        List<SlowMotionParam> slowMotionParam = getSupportedPreviewSizesAndFps();
        int qualitys = SLOW_MOTION_SUPPORT_QUALIYS.length;
        for (int i = 0; i < qualitys; i++) {
            if (CamcorderProfile.hasProfile(mCameraId, SLOW_MOTION_SUPPORT_QUALIYS[i])
                    && isParametersSupport(SLOW_MOTION_SUPPORT_QUALIYS[i], slowMotionParam)) {
                supported.add(SLOW_MOTION_SUPPORT_QUALIYS_STRING[i]);
            }
        }
        if (supported.size() == 1) {
            SharedPreferences pref = mPrefTransfer
                    .getSharedPreferences(SettingConstants.KEY_SLOW_MOTION_VIDEO_QUALITY);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString(SettingConstants.KEY_SLOW_MOTION_VIDEO_QUALITY, supported.get(0));
            editor.apply();

            int settingId = SettingConstants
                    .getSettingId(SettingConstants.KEY_SLOW_MOTION_VIDEO_QUALITY);
            SettingItem settingItem = getSettingItem(settingId);
            settingItem.setValue(supported.get(0));
        }

        Log.i(TAG, "supported slowMotion quality = " + supported);
        return supported;
    }

    private Integer getMaxPreviewFrameRate() {
        Parameters parameters = mICameraDevice.getParameters();
        List<Integer> frameRates = null;
        frameRates = parameters.getSupportedPreviewFrameRates();
        Integer max = Collections.max(frameRates);
        Log.i(TAG, "getMaxPreviewFrameRate max = " + max);
        return max;
    }

    private List<SlowMotionParam> getSupportedPreviewSizesAndFps() {
        Parameters parameters = mICameraDevice.getParameters();
        String str = parameters.get(ParametersHelper.KEY_HSVR_SIZE_FPS);
        return splitSize(str);
    }

    // Splits a comma delimited string to an ArrayList of Size.
    // Return null if the passing string is null or the size is 0.
    private ArrayList<SlowMotionParam> splitSize(String str) {
        if (str == null)
            return null;
        TextUtils.StringSplitter splitter = new TextUtils.SimpleStringSplitter(',');
        splitter.setString(str);
        ArrayList<SlowMotionParam> sizeList = new ArrayList<SlowMotionParam>();
        for (String s : splitter) {
            SlowMotionParam size = strToSize(s);
            if (size != null)
                sizeList.add(size);
        }
        if (sizeList.size() == 0)
            return null;
        return sizeList;
    }

    private SlowMotionParam strToSize(String str) {
        if (str == null)
            return null;
        int pos1 = str.indexOf('x');
        int pos2 = str.lastIndexOf('x');
        if (pos1 != -1 && pos2 != -1) {
            String width = str.substring(0, pos1);
            String height = str.substring(pos1 + 1, pos2);
            String fps = str.substring(pos2 + 1);
            return new SlowMotionParam(Integer.parseInt(width), Integer.parseInt(height),
                    Integer.parseInt(fps));
        }
        Log.e(TAG, "Invalid size parameter string=" + str);
        return null;
    }

    public boolean isParametersSupport(int quality, List<SlowMotionParam> slowMotionParam) {
        int currentCamera = mICameraDeviceManager.getCurrentCameraId();
        CamcorderProfile profile = CamcorderProfileEx.getProfile(currentCamera,
                quality);
        if (slowMotionParam == null) {
            Log.v(TAG, "slowMotionParam = " + slowMotionParam);
            return false;
        }
        Iterator<SlowMotionParam> it = slowMotionParam.iterator();
        boolean support = false;
        if (profile != null) {
            while (it.hasNext()) {
                SlowMotionParam size = it.next();
                if (size.width == profile.videoFrameWidth
                        && size.height == profile.videoFrameHeight
                        && size.fps == profile.videoFrameRate) {
                    support = true;
                    break;
                }
            }
        }
        Log.i(TAG, "isParametersSupport profile " + profile + ": support = " + support);
        return support;
    }

    private class SlowMotionParam {
        public SlowMotionParam(int w, int h, int f) {
            width = w;
            height = h;
            fps = f;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof SlowMotionParam)) {
                return false;
            }
            SlowMotionParam s = (SlowMotionParam) obj;
            return width == s.width && height == s.height && fps == s.fps;
        }

        @Override
        public int hashCode() {
            return width * 32713 + height;
        }

        private int width;
        private int height;
        private int fps;
    };


    private ArrayList<String> getSupportedFaceBeautyMode() {
        ArrayList<String> supported = new ArrayList<String>();
        supported.add(mContext.getResources().getString(R.string.face_beauty_single_mode));
        supported.add(mContext.getResources().getString(R.string.face_beauty_multi_mode));
        supported.add(mContext.getResources().getString(R.string.pref_face_beauty_mode_off));
        if (isOnlyMultiFaceBeautySupported()) {
            supported.remove(0);
        }
        Log.i(TAG, "getSupportedFaceBeautyMode : " + supported);
        return supported;
    }

    public boolean isOnlyMultiFaceBeautySupported() {
        // fb-extreme-beauty-supported is false means just supported Multi face
        // mode,
        // so need remove the single face mode in settings
        boolean isOnlySupported = "false".equals(mICameraDevice.getParameters().get(
                SettingConstants.KEY_FB_EXTEME_BEAUTY_SUPPORTED));
        Log.i(TAG, "isOnlyMultiFaceBeautySupported = " + isOnlySupported);
        return isOnlySupported;
    }
}
