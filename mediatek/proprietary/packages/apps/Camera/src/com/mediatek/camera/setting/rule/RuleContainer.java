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
package com.mediatek.camera.setting.rule;

import com.mediatek.camera.ICameraContext;
import com.mediatek.camera.ISettingCtrl;
import com.mediatek.camera.ISettingRule;
import com.mediatek.camera.ISettingRule.MappingFinder;
import com.mediatek.camera.platform.ICameraDeviceManager;
import com.mediatek.camera.platform.ICameraDeviceManager.ICameraDevice;
import com.mediatek.camera.platform.Parameters;
import com.mediatek.camera.setting.ParametersHelper;
import com.mediatek.camera.setting.SettingConstants;
import com.mediatek.camera.setting.SettingItem;
import com.mediatek.camera.setting.SettingItem.Record;
import com.mediatek.camera.setting.SettingUtils;
import com.mediatek.camera.setting.preference.ListPreference;
import com.mediatek.camera.util.Log;

import java.util.ArrayList;
import java.util.List;

public class RuleContainer {

    private static final String TAG = "RuleContainer";
    private long PICTURE_SIZE_4M = 4000000L;
    private ICameraDeviceManager mICameraDeviceManager;
    private ICameraContext mICameraContext;
    private ISettingCtrl mISettingCtrl;

    public RuleContainer(ISettingCtrl settingCtrl, ICameraContext cameraContext) {
        mISettingCtrl = settingCtrl;
        mICameraDeviceManager = cameraContext.getCameraDeviceManager();
        mICameraContext = cameraContext;
    }

    public void addRule() {
        HDRZSDRule hdrzsdRule = new HDRZSDRule();
        mISettingCtrl.addRule(SettingConstants.KEY_HDR,
                SettingConstants.KEY_CAMERA_ZSD, hdrzsdRule);

        if (!mICameraContext.getFeatureConfig().isLowRamOptSupport()) {
            return;
        }
        LowRamPictureRule hdrLowRam = new LowRamPictureRule(SettingConstants.KEY_HDR);
        hdrLowRam.addLimitation("on", null, null);
        mISettingCtrl.addRule(SettingConstants.KEY_HDR,
                SettingConstants.KEY_PICTURE_SIZE, hdrLowRam);

        LowRamPictureRule aisLowRam = new LowRamPictureRule(SettingConstants.KEY_CAMERA_AIS);
        aisLowRam.addLimitation("ais", null, null);
        mISettingCtrl.addRule(SettingConstants.KEY_CAMERA_AIS,
                SettingConstants.KEY_PICTURE_SIZE, aisLowRam);

        LowRamPictureRule nightMode = new LowRamPictureRule(SettingConstants.KEY_SCENE_MODE);
        nightMode.addLimitation("night", null, null);
        mISettingCtrl.addRule(SettingConstants.KEY_SCENE_MODE,
                SettingConstants.KEY_PICTURE_SIZE, nightMode);
    }

    private class LowRamPictureRule implements ISettingRule {
        private SettingItem mConditionSetting;
        private SettingItem mPictureSetting;
        private String mConditionKey;
        private String mCondition;

        public LowRamPictureRule(String settingkey) {
            mConditionKey = settingkey;
            mConditionSetting = mISettingCtrl.getSetting(mConditionKey);
            Log.i(TAG, "mConditionSetting  = " + mConditionSetting);
        }

        @Override
        public void execute() {
            mPictureSetting = mISettingCtrl.getSetting(SettingConstants.KEY_PICTURE_SIZE);
            String resultValue = mPictureSetting.getValue();
            String conditionSettingValue = mConditionSetting.getValue();
            int type = mPictureSetting.getType();
            boolean isConditionSatisfied = false;
            if (mCondition != null && mCondition.equals(conditionSettingValue)) {
                isConditionSatisfied = true;
            }
            Log.i(TAG, "[execute], mConditionKey:" + mConditionKey + ", " +
                    "resulteKey:" + SettingConstants.KEY_PICTURE_SIZE + ", " +
                        "isConditionSatisfied:" + isConditionSatisfied);
            if (isConditionSatisfied) {
                ListPreference pref = mPictureSetting.getListPreference();
                CharSequence[] entryValues = pref.getEntryValues();
                List<String> supportedValue = removeUnsupportedSize(entryValues, PICTURE_SIZE_4M);

                // if resultValue is not bigger than PICTURE_SIZE_4M use it, otherwise, set it
                // as the last element in the list of supported values.
                if (supportedValue.size() >= 1 &&
                        supportedValue.indexOf(resultValue) < 0) {
                    resultValue = supportedValue.get(supportedValue.size() - 1);
                }

                String[] values = new String[supportedValue.size()];
                String overrideValue = SettingUtils.buildEnableList(
                        supportedValue.toArray(values), resultValue);
                if (mPictureSetting.isEnable()) {
                    setResultSettingValue(type, resultValue, overrideValue,
                            true, mPictureSetting);
                }
                Record record = mPictureSetting.new Record(resultValue, overrideValue);
                mPictureSetting.addOverrideRecord(mConditionKey, record);
            } else {
                // restore picture size after set hdr off
                int overrideCount = mPictureSetting.getOverrideCount();
                Record record = mPictureSetting.getOverrideRecord(mConditionKey);
                if (record == null) {
                    mISettingCtrl.executeRule(SettingConstants.KEY_PICTURE_RATIO,
                            SettingConstants.KEY_PICTURE_SIZE);
                    return;
                }
                Log.i(TAG, "overrideCount:" + overrideCount);
                mPictureSetting.removeOverrideRecord(mConditionKey);
                overrideCount--;

                Record topRecord = mPictureSetting.getTopOverrideRecord();
                if (topRecord != null) {
                    String value = topRecord.getValue();
                    String overrideValue = topRecord.getOverrideValue();
                    ListPreference pref = mPictureSetting.getListPreference();
                    if (pref != null
                            && SettingUtils.isBuiltList(overrideValue)) {
                        // Build override value again.
                        pref.setEnabled(true);
                        String prefValue = pref.getValue();
                        List<String> list = SettingUtils
                                .getEnabledList(overrideValue);
                        if (list.contains(prefValue)) {
                            if (!prefValue.equals(value)) {
                                String[] values = new String[list
                                        .size()];
                                overrideValue = SettingUtils
                                        .buildEnableList(
                                                list.toArray(values),
                                                prefValue);
                            }
                            value = prefValue;
                        }
                    }
                    setResultSettingValue(type, value, overrideValue,
                            true, mPictureSetting);
                } else {
                    mISettingCtrl.executeRule(SettingConstants.KEY_PICTURE_RATIO,
                            SettingConstants.KEY_PICTURE_SIZE);
                }
            }
        }

        @Override
        public void addLimitation(String condition, List<String> result,
                MappingFinder mappingFinder) {
            mCondition = condition;
        }

        private List<String> removeUnsupportedSize(CharSequence[] entryValues, long threshold) {
            List<String> supporedValues = new ArrayList<String>();
            for (int i = 0; i < entryValues.length; i++) {
                String value = entryValues[i].toString();
                int index = value.indexOf('x');
                long width = (long) Integer.parseInt(value.substring(0, index));
                long height = (long) Integer.parseInt(value.substring(index + 1));
                if (width * height <= threshold) {
                    supporedValues.add("" + width + "x" + height);
                }
            }
            return supporedValues;
        }
    }

    private class HDRZSDRule implements ISettingRule {

        public HDRZSDRule() {

        }
        @Override
        public void execute() {
            SettingItem hdrSetting = mISettingCtrl.getSetting(SettingConstants.KEY_HDR);
            String hdr = hdrSetting.getValue();
            SettingItem zsdSetting = mISettingCtrl.getSetting(SettingConstants.KEY_CAMERA_ZSD);
            int cameraId = mICameraDeviceManager.getCurrentCameraId();
            ICameraDevice cameraDevice = mICameraDeviceManager.getCameraDevice(cameraId);
            Parameters parameters = cameraDevice.getParameters();

            // if isZSDHDRSupported is true, this is no restriction between hdr and zsd, and preview
            // is not hdr preview.
            // if isZSDHDRSupported is false, zsd will be off when hdr is on.
            boolean isZSDHDRSupported = mICameraContext.getFeatureConfig().isZSDHDRSupported();
            Log.i(TAG, "isZSDHDRSupported:" + isZSDHDRSupported);
            if (isZSDHDRSupported) {
                if ("on".equals(hdr)) {
                    ListPreference pref = zsdSetting.getListPreference();
                    if (pref != null) {
                        pref.setEnabled(false);
                        Record record = zsdSetting.new Record(zsdSetting.getValue(),
                                zsdSetting.getValue());
                        zsdSetting.addOverrideRecord(SettingConstants.KEY_HDR, record);
                    }
                    ParametersHelper.setParametersValue(parameters, cameraId,
                            SettingConstants.KEY_CAMERA_ZSD, zsdSetting.getValue());
                } else {
                    Record record  = zsdSetting.getOverrideRecord(SettingConstants.KEY_HDR);
                    if (record == null) {
                        return;
                    }

                    zsdSetting.removeOverrideRecord(SettingConstants.KEY_HDR);
                    int overrideCount = zsdSetting.getOverrideCount();
                    if (overrideCount > 0) {
                        Record topRecord = zsdSetting.getTopOverrideRecord();
                        if (topRecord != null) {
                            ParametersHelper.setParametersValue(parameters, cameraId,
                                    SettingConstants.KEY_CAMERA_ZSD, topRecord.getValue());
                            ListPreference pref = zsdSetting.getListPreference();
                            if (pref != null) {
                                pref.setOverrideValue(topRecord.getOverrideValue());
                            }
                        }
                    } else {
                        ListPreference pref = zsdSetting.getListPreference();
                        if (pref != null) {
                            pref.setEnabled(true);
                        }
                    }
                }
                if ("on".equals(zsdSetting.getValue())) {
                    ParametersHelper.setParametersValue(parameters, cameraId,
                            SettingConstants.KEY_HDR, "off");
                }

            }

            if (!isZSDHDRSupported) {
                if ("on".equals(hdr)) {
                    ParametersHelper.setParametersValue(parameters, cameraId,
                            SettingConstants.KEY_CAMERA_ZSD, "off");
                    ListPreference pref = zsdSetting.getListPreference();
                    if (pref != null) {
                        pref.setOverrideValue("off");
                    }

                    Record record = zsdSetting.new Record("off", "off");
                    zsdSetting.addOverrideRecord(SettingConstants.KEY_HDR, record);
                } else {
                    Record record  = zsdSetting.getOverrideRecord(SettingConstants.KEY_HDR);
                    if (record == null) {
                        return;
                    }

                    zsdSetting.removeOverrideRecord(SettingConstants.KEY_HDR);
                    int overrideCount = zsdSetting.getOverrideCount();
                    if (overrideCount > 0) {
                        Record topRecord = zsdSetting.getTopOverrideRecord();
                        if (topRecord != null) {
                            ParametersHelper.setParametersValue(parameters, cameraId,
                                    SettingConstants.KEY_CAMERA_ZSD, topRecord.getValue());
                            ListPreference pref = zsdSetting.getListPreference();
                            if (pref != null) {
                                pref.setOverrideValue(topRecord.getOverrideValue());
                            }
                        }
                    } else {
                        ListPreference pref = zsdSetting.getListPreference();
                        if (pref != null) {
                            ParametersHelper.setParametersValue(parameters, cameraId,
                                    SettingConstants.KEY_CAMERA_ZSD, pref.getValue());
                            pref.setOverrideValue(null);
                        }
                    }
                }
            }
        }

        @Override
        public void addLimitation(String condition,
                List<String> result, MappingFinder mappingFinder) {

        }
    }
    // now setResultSettingValue is only use for hdr ram optimize rule ,if use to another
    // rule should reference to common rule setResultSettingValue
    private void setResultSettingValue(int settingType, String value,
            String overrideValue, boolean restoreSupported, SettingItem item) {
        int currentCameraId = mICameraDeviceManager.getCurrentCameraId();
        ICameraDevice cameraDevice = mICameraDeviceManager
                .getCameraDevice(currentCameraId);
        Parameters parameters = cameraDevice.getParameters();
        item.setValue(value);
        ListPreference pref = item.getListPreference();
        if (pref != null) {
             pref.setOverrideValue(overrideValue, restoreSupported);
        }
        ParametersHelper.setParametersValue(parameters, currentCameraId,
                    item.getKey(), value);

    }
}