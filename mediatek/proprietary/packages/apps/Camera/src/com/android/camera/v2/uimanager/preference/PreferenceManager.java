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
package com.android.camera.v2.uimanager.preference;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.provider.MediaStore;
import android.util.Log;

import com.android.camera.v2.app.SettingAgent;
import com.android.camera.v2.util.CameraUtil;
import com.android.camera.v2.util.SettingKeys;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PreferenceManager {
    private static final String                TAG = "PreferenceManager";
    private Context                            mContext = null;
    private SettingAgent                       mSettingAgent = null;
    private Map<Integer, PreferenceGroup> mPreferenceGroupMap =
            new HashMap<Integer, PreferenceGroup>();
    private int                                mCameraId = 0;
    private Activity                           mActivity;
    private String[]                           mSettingKeys = null;

    public PreferenceManager(Activity activity, SettingAgent settingAgent) {
        Log.i(TAG, "[PreferenceManager], instructor");
        mActivity = activity;
        mContext = activity.getApplicationContext();
        mSettingAgent = settingAgent;

        Intent intent = mActivity.getIntent();
        String action = null;
        if (intent != null) {
            action = intent.getAction();
        }
        Log.i(TAG, "[PreferenceManager], action:" + action);

        // copy SettingKeys.KEYS_FOR_SETTING to mSettingKeys.
        mSettingKeys = new String[SettingKeys.KEYS_FOR_SETTING.length];
        for (int i = 0; i < SettingKeys.KEYS_FOR_SETTING.length; i++) {
            mSettingKeys[i] = SettingKeys.KEYS_FOR_SETTING[i];
        }

        if (MediaStore.ACTION_IMAGE_CAPTURE.equals(action)
                || MediaStore.ACTION_VIDEO_CAPTURE.equals(action)
                || CameraUtil.ACTION_STEREO3D.equals(action)) {
            for (int i = 0; i < SettingKeys.KEYS_UN_SUPPORTED_BY_3TH.length; i++) {
                int settingId = SettingKeys.KEYS_UN_SUPPORTED_BY_3TH[i];
                mSettingKeys[settingId] = null;
            }
        }
    }

    public void initializePreferences(int preferencesRes, int cameraId) {
        Log.i(TAG, "[initializePreferences], start, cameraId:" + cameraId);
        mCameraId = cameraId;
        PreferenceGroup group = mPreferenceGroupMap.get(cameraId);
        if (group == null) {
            //constructor listPreference from xml.
            PreferenceInflater inflater = new PreferenceInflater(mContext);
            group = (PreferenceGroup) inflater.inflate(preferencesRes);
            mPreferenceGroupMap.put(cameraId, group);
            for (int i = 0; i < mSettingKeys.length; i++) {
                String key = mSettingKeys[i];
                if (key == null) {
                    continue;
                }
                ListPreference preference = group.findPreference(key);
                if (preference == null) {
                    continue;
                }

                int type = SettingKeys.getSettingType(key);
                // only decided by by app layer.
                if (type == SettingKeys.DECIDE_BY_APP) {
                    continue;
                }
                // get the supported values of setting which is decide by native from native.
                List<String> supportedValues =
                        mSettingAgent.getSupportedValues(key, String.valueOf(mCameraId));
                String supportedValue = null;
                if (supportedValues != null) {
                    for (int k = 0; k < supportedValues.size(); k++) {
                        String value = supportedValues.get(k);
                        supportedValue = supportedValue + value + ",";
                    }
                }
                Log.d(TAG, "key:" + key + ", supportedValue:" + supportedValue);
                //filter listPreference.
                filterUnSupportedValues(preference, supportedValues);
            }
            filterGroupListPrference(group, SettingKeys.KEY_IMAGE_PROPERTIES);
            filterGroupListPrference(group, SettingKeys.KEY_FACE_BEAUTY_PROPERTIES);

            for (int i = 0; i < mSettingKeys.length; i++) {
                String key = mSettingKeys[i];
                if (key == null) {
                    continue;
                }
                ListPreference preference = group.findPreference(key);
                if (preference != null) {
                    Log.i(TAG, "key:" + key + ", defaultValue:" + preference.getDefaultValue());
                }
            }
        }

        // camera id may be changed, so do camera id setting change every time.
        mSettingAgent.doSettingChange(SettingKeys.KEY_CAMERA_ID, String.valueOf(cameraId));

        // setting may have last setting changed value, when change to a new camera, firstly
        // make all the settings' value to default value.
        Map<String, String> defaultSettings = new LinkedHashMap<String, String>();
        for (int i = 0; i < mSettingKeys.length; i++) {
            String key = mSettingKeys[i];
            if (key == null) {
                continue;
            }
            ListPreference preference = group.findPreference(key);
            if (preference == null) {
                continue;
            }
            preference.setOverrideValue(null);
            String defaultValue = preference.getDefaultValue();
            if (defaultValue == null) {
                List<String> supportedValues = mSettingAgent
                        .getSupportedValues(key, String.valueOf(mCameraId));
                if (supportedValues != null) {
                    defaultValue = supportedValues.get(0);
                }
                preference.setDefaultValue(defaultValue);
            }
            defaultSettings.put(key, defaultValue);

            // synch listPreference value to the value in sharedpreferences.
            String sharePreferencesValue =
                    mSettingAgent.getSharedPreferencesValue(key, String.valueOf(mCameraId));
            if (sharePreferencesValue != null) {
                preference.setValue(sharePreferencesValue);
            }
        }
       // do not need to configurate camera id.
        defaultSettings.remove(SettingKeys.KEY_CAMERA_ID);
        mSettingAgent.configurateSetting(defaultSettings);

        Log.i(TAG, "[initializePreferences], end");
    }

    public ListPreference getListPreference(String key) {
        if (key == null) {
            return null;
        }
        // if key do not in mSettingKeys, return null;
        boolean isContains = false;
        for (int i = 0; i < mSettingKeys.length; i++) {
            if (mSettingKeys[i] != null && mSettingKeys[i].equals(key)) {
                isContains = true;
                break;
            }
        }
        if (!isContains) {
            return null;
        }

        PreferenceGroup group = mPreferenceGroupMap.get(mCameraId);
        return group.findPreference(key);
    }

    public PreferenceGroup getPreferenceGroup() {
        PreferenceGroup group = mPreferenceGroupMap.get(mCameraId);
        return group;
    }

    public void updateSettingResult(Map<String, String> values,
            Map<String, String> overrideValues) {
        Log.i(TAG, "[updateSettingResult]");
        if (values != null) {
            Set<String> set = values.keySet();
            Iterator<String> iterator = set.iterator();
            PreferenceGroup group = mPreferenceGroupMap.get(mCameraId);
            while (iterator.hasNext()) {
                String key = iterator.next();
                String value = values.get(key);
                String overrideValue = CameraUtil.buildEnabledList(overrideValues.get(key), value);
                ListPreference preference = group.findPreference(key);
                if (preference != null  && preference.isVisibled()) {
                    preference.setValue(value);
                    preference.setOverrideValue(overrideValue);
                }
            }
        }
    }

    public void restoreSetting() {
        Map<String, String> defaultSettings = new HashMap<String, String>();
        PreferenceGroup group = mPreferenceGroupMap.get(mCameraId);
        for (int i = 0; i < mSettingKeys.length; i++) {
            String key = mSettingKeys[i];
            if (key == null) {
                continue;
            }

            ListPreference pref = group.findPreference(key);
            if (pref != null) {
                String defaultValue = pref.getDefaultValue();
                pref.setValue(defaultValue);
                defaultSettings.put(key, defaultValue);
            }
        }
        // do not need to reset camera id.
        defaultSettings.remove(SettingKeys.KEY_CAMERA_ID);
        mSettingAgent.doSettingChange(defaultSettings);
        clearSharedPreferencesValue();
    }

    public void clearSharedPreferencesValue() {
        Set<Integer> set = mPreferenceGroupMap.keySet();
        Iterator<Integer> iterator = set.iterator();
        // play the keys which do not want to clear as null.
        String[] clearKeys = new String[mSettingKeys.length];
        for (int i = 0; i < mSettingKeys.length; i++) {
            clearKeys[i] = mSettingKeys[i];
        }
        for (int i = 0; i < SettingKeys.KEYS_UN_CLEAR.length; i++) {
            int settingId = SettingKeys.KEYS_UN_CLEAR[i];
            clearKeys[settingId] = null;
        }

        while (iterator.hasNext()) {
            String cameraId = iterator.next().toString();
            mSettingAgent.clearSharedPreferencesValue(clearKeys, cameraId);
        }
    }

    private void filterUnSupportedValues(ListPreference pref, List<String> supportedValues) {
        if (supportedValues != null) {
            pref.filterUnsupported(supportedValues);
        }

        if (supportedValues == null || supportedValues.size() <= 1) {
            pref.setVisibled(false);
            return;
        }

        if (pref.getEntries().length <= 1) {
            pref.setVisibled(false);
            return;
        }

        resetIfInvalid(pref, true);
    }

    private void filterGroupListPrference(PreferenceGroup group, String key) {
        ListPreference groupPref = group.findPreference(key);
        if (groupPref == null) {
            return;
        }
        CharSequence[] entryValues = groupPref.getOriginalEntries();
        if (entryValues == null) {
            groupPref.setVisibled(false);
            return;
        }

        List<ListPreference> mChildPrefernce = new ArrayList<ListPreference>();
        for (CharSequence value : entryValues) {
            ListPreference pref = group.findPreference(value.toString());
            if (pref != null && pref.isVisibled()) {
                mChildPrefernce.add(pref);
            }
        }
        if (mChildPrefernce.size() <= 0) {
            groupPref.setVisibled(false);
        } else {
            groupPref.setChildPreferences(mChildPrefernce.toArray(
                    new ListPreference[mChildPrefernce.size()]));
        }
    }

    private void resetIfInvalid(ListPreference pref, boolean first) {
        // Set the value to the first entry if it is invalid.
        String value = pref.getValue();
        if (pref.findIndexOfValue(value) == -1) {
            if (first) {
                pref.setValueIndex(0);
            } else if (pref.getEntryValues() != null && pref.getEntryValues().length > 0) {
                pref.setValueIndex(pref.getEntryValues().length - 1);
            }
        }
    }
}
