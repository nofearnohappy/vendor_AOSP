package com.mediatek.camera.mode.facebeauty;

import android.media.CamcorderProfile;

import com.mediatek.camcorder.CamcorderProfileEx;
import com.mediatek.camera.ICameraContext;
import com.mediatek.camera.ISettingCtrl;
import com.mediatek.camera.ISettingRule;
import com.mediatek.camera.ISettingRule.MappingFinder;
import com.mediatek.camera.mode.facebeauty.VideoFaceBeautyRule;
import com.mediatek.camera.platform.ICameraDeviceManager;
import com.mediatek.camera.platform.Parameters;
import com.mediatek.camera.platform.ICameraDeviceManager.ICameraDevice;
import com.mediatek.camera.setting.SettingConstants;
import com.mediatek.camera.setting.SettingItem;
import com.mediatek.camera.setting.SettingUtils;
import com.mediatek.camera.setting.SettingItem.Record;
import com.mediatek.camera.setting.preference.ListPreference;
import com.mediatek.camera.util.Log;
import com.mediatek.camera.util.Util;

import java.util.ArrayList;
import java.util.List;

public class VfbQualityRule implements ISettingRule {

    private static final String TAG = "VideoFaceBeautyVideoQualityRule";

    private static final String VIDEO_QUALITY_LOW = Integer
            .toString(CamcorderProfileEx.QUALITY_LOW);
    private static final String VIDEO_QUALITY_MEDIUM = Integer
            .toString(CamcorderProfileEx.QUALITY_MEDIUM);
    private static final String VIDEO_QUALITY_HIGH = Integer
            .toString(CamcorderProfileEx.QUALITY_HIGH);
    private static final String VIDEO_QUALITY_FINE = Integer
            .toString(CamcorderProfileEx.QUALITY_FINE);
    private static final String VIDEO_QUALITY_FINE_4K2K = Integer
            .toString(CamcorderProfileEx.QUALITY_FINE_4K2K);

    private List<String> mConditions = new ArrayList<String>();
    private List<List<String>> mResults = new ArrayList<List<String>>();
    private List<MappingFinder> mMappingFinder = new ArrayList<MappingFinder>();

    private boolean mHasOverride = false;

    private String mConditionKey = null;
    private String mLastQualityValue;
    private ISettingCtrl mISettingCtrl;
    private ICameraDevice mICameraDevice;
    private ICameraDeviceManager mICameraDeviceManager;

    public VfbQualityRule(ICameraContext cameraContext, String conditionKey) {
        Log.i(TAG, "[VfbQualityRule]constructor...");
        mConditionKey = conditionKey;
        mISettingCtrl = cameraContext.getSettingController();
        mICameraDeviceManager = cameraContext.getCameraDeviceManager();
    }

    @Override
    public void execute() {
        String value = mISettingCtrl.getSettingValue(SettingConstants.KEY_VIDEO);
        mICameraDevice = getCameraDevice();
        Parameters parameters = mICameraDevice.getParameters();
        int index = conditionSatisfied(value);

        SettingItem setting = mISettingCtrl.getSetting(SettingConstants.KEY_VIDEO_QUALITY);
        ListPreference pref = mISettingCtrl.getListPreference(SettingConstants.KEY_VIDEO_QUALITY);
        Log.i(TAG, "[execute] index = " + index);

        if (index == -1) {
            int overrideCount = setting.getOverrideCount();
            Record record = setting.getOverrideRecord(mConditionKey);
            if (record == null) {
                return;
            }
            setting.removeOverrideRecord(mConditionKey);
            overrideCount--;
            String quality = null;
            if (overrideCount > 0) {
                Record topRecord = setting.getTopOverrideRecord();
                if (topRecord != null) {
                    quality = topRecord.getValue();
                    setting.setValue(quality);
                    String overrideValue = topRecord.getOverrideValue();
                    setting.setValue(quality);
                    if (pref != null) {
                        pref.setOverrideValue(overrideValue);
                    }
                }
            } else {
                if (pref != null) {
                    quality = pref.getValue();
                    pref.setOverrideValue(null);
                }
                setting.setValue(quality);
            }
            Log.i(TAG, "set quality:" + quality);

        } else {
            if (parameters != null
                    && Util.VIDEO_FACE_BEAUTY_ENABLE.equals(parameters
                            .get(Util.KEY_VIDEO_FACE_BEAUTY))) {

                // override video quality and write value to setting.
                List<String> supportedValues = getSupportedVideoQualities();
                String currentQuality = setting.getValue();
                String quality = getQuality(currentQuality, supportedValues);
                setting.setValue(quality);
                Log.i(TAG, "set quality:" + quality);

                // update video quality setting ui.
                String overrideValue = null;
                if (pref != null && supportedValues != null) {
                    String[] values = new String[supportedValues.size()];
                    overrideValue = SettingUtils.buildEnableList(supportedValues.toArray(values),
                            quality);
                    pref.setOverrideValue(overrideValue);
                }

                Record record = setting.new Record(quality, overrideValue);
                setting.addOverrideRecord(mConditionKey, record);
            }
        }
    }

    @Override
    public void addLimitation(String condition, List<String> result, MappingFinder mappingFinder) {
        Log.i(TAG, "[addLimitation]condition = " + condition);
        mConditions.add(condition);
        mResults.add(result);
        mMappingFinder.add(mappingFinder);
    }

    private ICameraDevice getCameraDevice() {
        ICameraDevice device = null;
        if (mICameraDeviceManager != null) {
            int camerId = mICameraDeviceManager.getCurrentCameraId();
            device = mICameraDeviceManager.getCameraDevice(camerId);
        }
        return device;
    }

    private int conditionSatisfied(String conditionValue) {
        int index = mConditions.indexOf(conditionValue);
        Log.i(TAG, "[conditionSatisfied]limitation index:" + index);
        return index;
    }

    private List<String> getSupportedVideoQualities() {
        Log.i(TAG, "[getSupportedVideoQualities]");
        ArrayList<String> supported = new ArrayList<String>();
        if (checkVideoFaceBeautyQuality(CamcorderProfileEx.QUALITY_FINE)) {
            supported.add(VIDEO_QUALITY_FINE);
        }
        if (checkVideoFaceBeautyQuality(CamcorderProfileEx.QUALITY_HIGH)) {
            supported.add(VIDEO_QUALITY_HIGH);
        }
        if (checkVideoFaceBeautyQuality(CamcorderProfileEx.QUALITY_MEDIUM)) {
            supported.add(VIDEO_QUALITY_MEDIUM);
        }
        if (checkVideoFaceBeautyQuality(CamcorderProfileEx.QUALITY_LOW)) {
            supported.add(VIDEO_QUALITY_LOW);
        }
        int size = supported.size();
        if (size > 0) {
            return supported;
        }
        return null;
    }

    private boolean checkVideoFaceBeautyQuality(int quality) {
        int cameraId = mICameraDeviceManager.getCurrentCameraId();
        if (CamcorderProfile.hasProfile(cameraId, quality)) {
            CamcorderProfile profile = CamcorderProfileEx.getProfile(cameraId, quality);
            return profile.videoFrameWidth <= FaceBeautyParametersHelper.VIDEO_FACE_BEAUTY_MAX_SOLUTION_WIDTH;
        }
        return false;
    }

    private String getQuality(String current, List<String> supportedList) {
        String supported = current;
        if (supportedList != null && !supportedList.contains(current)) {
            if (Integer.toString(CamcorderProfileEx.QUALITY_FINE).equals(current)) {
                // match normal fine quality to high in pip mode
                supported = Integer.toString(CamcorderProfileEx.QUALITY_HIGH);
            }
        }
        if (!supportedList.contains(supported)) {
            supported = supportedList.get(0);
        }
        return supported;
    }
}
