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
package com.mediatek.camera.platform;

import android.hardware.Camera.Area;
import android.hardware.Camera.Size;

import java.util.List;

public interface Parameters {

    public static final String WHITE_BALANCE_TUNGSTEN = "tungsten";

    // Values for white balance settings.
    public static final String WHITE_BALANCE_AUTO = "auto";
    public static final String WHITE_BALANCE_INCANDESCENT = "incandescent";
    public static final String WHITE_BALANCE_FLUORESCENT = "fluorescent";
    public static final String WHITE_BALANCE_WARM_FLUORESCENT = "warm-fluorescent";
    public static final String WHITE_BALANCE_DAYLIGHT = "daylight";
    public static final String WHITE_BALANCE_CLOUDY_DAYLIGHT = "cloudy-daylight";
    public static final String WHITE_BALANCE_TWILIGHT = "twilight";
    public static final String WHITE_BALANCE_SHADE = "shade";

    // Values for color effect settings.
    public static final String EFFECT_NONE = "none";
    public static final String EFFECT_MONO = "mono";
    public static final String EFFECT_NEGATIVE = "negative";
    public static final String EFFECT_SOLARIZE = "solarize";
    public static final String EFFECT_SEPIA = "sepia";
    public static final String EFFECT_POSTERIZE = "posterize";
    public static final String EFFECT_WHITEBOARD = "whiteboard";
    public static final String EFFECT_BLACKBOARD = "blackboard";
    public static final String EFFECT_AQUA = "aqua";

    // Values for antibanding settings.
    public static final String ANTIBANDING_AUTO = "auto";
    public static final String ANTIBANDING_50HZ = "50hz";
    public static final String ANTIBANDING_60HZ = "60hz";
    public static final String ANTIBANDING_OFF = "off";

    public static final String KEY_STEREO3D_TYPE = "type";
    public static final String KEY_STEREO3D_MODE = "mode";
    public static final String STEREO3D_TYPE_OFF = "off";
    public static final String STEREO3D_TYPE_FRAMESEQ = "frame_seq";
    public static final String STEREO3D_TYPE_SIDEBYSIDE = "sidebyside";
    public static final String STEREO3D_TYPE_TOPBOTTOM = "topbottom";
    public static final String EIS_MODE_ON = "on";
    public static final String EIS_MODE_OFF = "off";
    public static final String FLASH_MODE_OFF = "off";
    public static final String FLASH_MODE_AUTO = "auto";
    public static final String FLASH_MODE_ON = "on";
    public static final String FLASH_MODE_RED_EYE = "red-eye";
    public static final String FLASH_MODE_TORCH = "torch";
    public static final String SCENE_MODE_AUTO = "auto";
    public static final String SCENE_MODE_ACTION = "action";
    public static final String SCENE_MODE_PORTRAIT = "portrait";
    public static final String SCENE_MODE_LANDSCAPE = "landscape";
    public static final String SCENE_MODE_NIGHT = "night";
    public static final String SCENE_MODE_NIGHT_PORTRAIT = "night-portrait";
    public static final String SCENE_MODE_THEATRE = "theatre";
    public static final String SCENE_MODE_BEACH = "beach";
    public static final String SCENE_MODE_SNOW = "snow";
    public static final String SCENE_MODE_SUNSET = "sunset";
    public static final String SCENE_MODE_STEADYPHOTO = "steadyphoto";
    public static final String SCENE_MODE_FIREWORKS = "fireworks";
    public static final String SCENE_MODE_SPORTS = "sports";
    public static final String SCENE_MODE_PARTY = "party";
    public static final String SCENE_MODE_CANDLELIGHT = "candlelight";
    public static final String SCENE_MODE_BARCODE = "barcode";
    public static final String SCENE_MODE_HDR = "hdr";
    public static final String FOCUS_MODE_AUTO = "auto";
    public static final String FOCUS_MODE_INFINITY = "infinity";
    public static final String FOCUS_MODE_MACRO = "macro";
    public static final String FOCUS_MODE_FIXED = "fixed";
    public static final String FOCUS_MODE_EDOF = "edof";
    public static final int CAMERA_MODE_NORMAL = 0;
    public static final int CAMERA_MODE_MTK_PRV = 1;
    public static final int CAMERA_MODE_MTK_VDO = 2;
    public static final int CAMERA_MODE_MTK_VT = 3;
    public static final int FOCUS_ENG_MODE_NONE = 0;
    public static final int FOCUS_ENG_MODE_BRACKET = 1;
    public static final int FOCUS_ENG_MODE_FULLSCAN = 2;
    public static final int FOCUS_ENG_MODE_FULLSCAN_REPEAT = 3;
    public static final int FOCUS_ENG_MODE_REPEAT = 4;
    public static final String FOCUS_MODE_MANUAL = "manual";
    public static final String FOCUS_MODE_FULLSCAN = "fullscan";
    public static final int PREVIEW_DUMP_RESOLUTION_NORMAL = 0;
    public static final int PREVIEW_DUMP_RESOLUTION_CROP = 1;
    public static final String CAPTURE_MODE_NORMAL = "normal";
    public static final String CAPTURE_MODE_BEST_SHOT = "bestshot";
    public static final String CAPTURE_MODE_EV_BRACKET_SHOT = "evbracketshot";
    public static final String CAPTURE_MODE_BURST_SHOT = "burstshot";
    public static final String CAPTURE_MODE_SMILE_SHOT = "smileshot";
    public static final String CAPTURE_MODE_GESTURE_SHOT = "gestureshot";
    public static final String CAPTURE_MODE_PANORAMA_SHOT = "autorama";
    public static final String CAPTURE_MODE_HDR = "hdr";
    public static final String CAPTURE_MODE_ASD = "asd";
    public static final String CAPTURE_MODE_FB = "face_beauty";
    public static final String KEY_MAX_NUM_DETECTED_OBJECT = "max-num-ot";
    public static final String CAPTURE_MODE_S3D = "single3d";
    public static final String CAPTURE_MODE_PANORAMA3D = "panorama3dmode";
    public static final String CAPTURE_MODE_CONTINUOUS_SHOT = "continuousshot";
    public static final String SENSOR_DEV_MAIN = "main";
    public static final String SENSOR_DEV_SUB = "sub";
    public static final String SENSOR_DEV_ATV = "atv";
    public static final String FOCUS_MODE_CONTINUOUS_VIDEO = "continuous-video";
    public static final String FOCUS_MODE_CONTINUOUS_PICTURE = "continuous-picture";
    public static final int FOCUS_DISTANCE_NEAR_INDEX = 0;
    public static final int FOCUS_DISTANCE_OPTIMAL_INDEX = 1;
    public static final int FOCUS_DISTANCE_FAR_INDEX = 2;
    public static final int PREVIEW_FPS_MIN_INDEX = 0;
    public static final int PREVIEW_FPS_MAX_INDEX = 1;

    public void set(String key, String value);

    public void set(String key, int value);

    public String get(String key);

    public int getInt(String key);

    public void setPreviewSize(int width, int height);

    public Size getPreviewSize();

    public List<Size> getSupportedPreviewSizes();

    public List<Size> getSupportedVideoSizes();

    public Size getPreferredPreviewSizeForVideo();

    public Size getPreferredPreviewSizeForSlowMotionVideo();

    public List<Size> getSupportedSlowMotionVideoSizes();

    public void setJpegThumbnailSize(int width, int height);

    public Size getJpegThumbnailSize();

    public List<Size> getSupportedJpegThumbnailSizes();

    public void setJpegThumbnailQuality(int quality);

    public int getJpegThumbnailQuality();

    public void setJpegQuality(int quality);

    public int getJpegQuality();

    public void setPreviewFrameRate(int fps);

    public int getPreviewFrameRate();

    public List<Integer> getSupportedPreviewFrameRates();

    public void setPreviewFpsRange(int min, int max);

    public void getPreviewFpsRange(int[] range);

    public List<int[]> getSupportedPreviewFpsRange();

    public void setPreviewFormat(int pixel_format);

    public int getPreviewFormat();

    public List<Integer> getSupportedPreviewFormats();

    public void setPictureSize(int width, int height);

    public Size getPictureSize();

    public List<Size> getSupportedPictureSizes();

    public void setPictureFormat(int pixel_format);

    public int getPictureFormat();

    public List<Integer> getSupportedPictureFormats();

    public void setRotation(int rotation);

    public void setGpsLatitude(double latitude);

    public void setGpsLongitude(double longitude);

    public void setGpsAltitude(double altitude);

    public void setGpsTimestamp(long timestamp);

    public void setGpsProcessingMethod(String processing_method);

    public void removeGpsData();

    public String getWhiteBalance();

    public void setWhiteBalance(String value);

    public List<String> getSupportedWhiteBalance();

    public String getColorEffect();

    public void setColorEffect(String value);

    public List<String> getSupportedColorEffects();

    public String getAntibanding();

    public void setAntibanding(String antibanding);

    public List<String> getSupportedAntibanding();

    public String getEisMode();

    public void setEisMode(String eis);

    public List<String> getSupportedEisMode();

    public String getAFLampMode();

    public void setAFLampMode(String aflamp);

    public List<String> getSupportedAFLampMode();

    public String getSceneMode();

    public void setSceneMode(String value);

    public List<String> getSupportedSceneModes();

    public String getFlashMode();

    public void setFlashMode(String value);

    public List<String> getSupportedFlashModes();

    public String getFocusMode();

    public void setFocusMode(String value);

    public List<String> getSupportedFocusModes();

    public float getFocalLength();

    public float getHorizontalViewAngle();

    public float getVerticalViewAngle();

    public int getExposureCompensation();

    public void setExposureCompensation(int value);

    public int getMaxExposureCompensation();

    public int getMinExposureCompensation();

    public float getExposureCompensationStep();

    public void setAutoExposureLock(boolean toggle);

    public boolean getAutoExposureLock();

    public boolean isAutoExposureLockSupported();

    public void setAutoWhiteBalanceLock(boolean toggle);

    public boolean getAutoWhiteBalanceLock();

    public boolean isAutoWhiteBalanceLockSupported();

    public int getZoom();

    public void setZoom(int value);

    public boolean isZoomSupported();

    public int getMaxZoom();

    public List<Integer> getZoomRatios();

    public boolean isSmoothZoomSupported();

    public void setCameraMode(int value);

    public String getISOSpeed();

    public void setISOSpeed(String value);

    public List<String> getSupportedISOSpeed();

    public int getMaxNumDetectedObjects();

    public String getFDMode();

    public void setFDMode(String value);

    public List<String> getSupportedFDMode();

    public String getEdgeMode();

    public void setEdgeMode(String value);

    public List<String> getSupportedEdgeMode();

    public String getHueMode();

    public void setHueMode(String value);

    public List<String> getSupportedHueMode();

    public String getSaturationMode();

    public void setSaturationMode(String value);

    public List<String> getSupportedSaturationMode();

    public String getBrightnessMode();

    public void setBrightnessMode(String value);

    public List<String> getSupportedBrightnessMode();

    public String getContrastMode();

    public void setContrastMode(String value);

    public List<String> getSupportedContrastMode();

    public String getCaptureMode();

    public void setCaptureMode(String value);

    public List<String> getSupportedCaptureMode();

    public void setCapturePath(String value);

    public void setBurstShotNum(int value);

    public void setFocusEngMode(int mode);

    public int getBestFocusStep();

    public void setRawDumpFlag(boolean toggle);

    public void setPreviewRawDumpResolution(int value);

    public int getMaxFocusStep();

    public int getMinFocusStep();

    public void setFocusEngStep(int step);

    public void setExposureMeterMode(String mode);

    public String getExposureMeterMode();

    public int getSensorType();

    public void setEngAEEnable(int enable);

    public void setEngFlashDuty(int duty);

    public void setEngZSDEnable(int enable);

    public int getEngPreviewShutterSpeed();

    public int getEngPreviewSensorGain();

    public int getEngPreviewISPGain();

    public int getEngPreviewAEIndex();

    public int getEngCaptureSensorGain();

    public int getEngCaptureISPGain();

    public int getEngCaptureShutterSpeed();

    public int getEngCaptureISO();

    public int getEngFlashDutyMin();

    public int getEngFlashDutyMax();

    public int getEngPreviewFPS();

    public String getEngEngMSG();

    public void setEngFocusFullScanFrameInterval(int n);

    public int getEngFocusFullScanFrameIntervalMax();

    public int getEngFocusFullScanFrameIntervalMin();

    public int getEngPreviewFrameIntervalInUS();

    public void setEngParameter1(String value);

    public void setEngParameter2(String value);

    public void setEngParameter3(String value);

    public void setEngSaveShadingTable(int save);

    public void setEngShadingTable(int shading_table);

    public int getEngEVCalOffset();

    public void setMATVDelay(int ms);

    public String getStereo3DType();

    public void setStereo3DMode(boolean enable);

    public void setContinuousSpeedMode(String value);

    public String getZSDMode();

    public void setZSDMode(String value);

    public List<String> getSupportedZSDMode();

    public void getFocusDistances(float[] output);

    public List<Area> getFocusAreas();

    public void setFocusAreas(List<Area> focusAreas);

    public int getMaxNumMeteringAreas();

    public List<Area> getMeteringAreas();

    public void setMeteringAreas(List<Area> meteringAreas);

    public int getMaxNumDetectedFaces();

    public void setRecordingHint(boolean hint);

    public boolean isVideoSnapshotSupported();

    public void enableRecordingSound(String value);

    public void setVideoStabilization(boolean toggle);

    public boolean getVideoStabilization();

    public boolean isVideoStabilizationSupported();

    public List<Integer> getPIPFrameRateZSDOn();

    public List<Integer> getPIPFrameRateZSDOff();

    public boolean getDynamicFrameRate();

    public void setDynamicFrameRate(boolean toggle);

    public boolean isDynamicFrameRateSupported();

    public void setRefocusJpsFileName(String fineName);

    public String getDepthAFMode();

    public String getDistanceMode();

    public void setDepthAFMode(boolean isDepthAfMode);

    public void setDistanceMode(boolean isDistanceMode);
}
