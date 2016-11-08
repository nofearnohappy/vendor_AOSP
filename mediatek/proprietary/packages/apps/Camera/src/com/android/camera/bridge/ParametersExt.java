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
package com.android.camera.bridge;

import android.hardware.Camera.Area;
import android.hardware.Camera.Size;

import com.android.camera.CameraManager;

import com.mediatek.camera.platform.Parameters;

import java.util.List;

public class ParametersExt implements Parameters {


    private final CameraManager.CameraProxy mCameraDevice;
    private android.hardware.Camera.Parameters mParameters;

    public ParametersExt(CameraManager.CameraProxy cameraDevice,
            android.hardware.Camera.Parameters parameters) {
        mCameraDevice = cameraDevice;
        mParameters = parameters;
    }

    public void setparameters(android.hardware.Camera.Parameters parameters) {
        mParameters = parameters;
    }

    @Override
    public void set(final String key, final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.set(key, value);
            }
        });
    }

    @Override
    public void set(final String key, final int value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.set(key, value);
            }
        });
    }

    @Override
    public String get(String key) {
        return mParameters.get(key);
    }

    @Override
    public int getInt(String key) {
        return mParameters.getInt(key);
    }

    @Override
    public void setPreviewSize(final int width, final int height) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setPreviewSize(width, height);
            }
        });
    }

    @Override
    public Size getPreviewSize() {
        return mParameters.getPreviewSize();
    }

    @Override
    public List<Size> getSupportedPreviewSizes() {
        return mParameters.getSupportedPreviewSizes();
    }

    @Override
    public List<Size> getSupportedVideoSizes() {
        return mParameters.getSupportedVideoSizes();
    }

    @Override
    public Size getPreferredPreviewSizeForVideo() {
        return mParameters.getPreferredPreviewSizeForVideo();
    }

    @Override
    public Size getPreferredPreviewSizeForSlowMotionVideo() {
        return mParameters.getPreferredPreviewSizeForSlowMotionVideo();
    }

    @Override
    public List<Size> getSupportedSlowMotionVideoSizes() {
        return mParameters.getSupportedSlowMotionVideoSizes();
    }

    @Override
    public void setJpegThumbnailSize(final int width, final int height) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setJpegThumbnailSize(width, height);
            }
        });
    }

    @Override
    public Size getJpegThumbnailSize() {
        return mParameters.getJpegThumbnailSize();
    }

    @Override
    public List<Size> getSupportedJpegThumbnailSizes() {
        return mParameters.getSupportedJpegThumbnailSizes();
    }

    @Override
    public void setJpegThumbnailQuality(final int quality) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setJpegThumbnailQuality(quality);
            }
        });
    }

    @Override
    public int getJpegThumbnailQuality() {
        return mParameters.getJpegThumbnailQuality();
    }

    @Override
    public void setJpegQuality(final int quality) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setJpegQuality(quality);
            }
        });
    }

    @Override
    public int getJpegQuality() {
        return mParameters.getJpegQuality();
    }

    @Override
    public void setPreviewFrameRate(final int fps) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setPreviewFrameRate(fps);
            }
        });
    }

    @Override
    public int getPreviewFrameRate() {
        return mParameters.getPreviewFrameRate();
    }

    @Override
    public List<Integer> getSupportedPreviewFrameRates() {
        return mParameters.getSupportedPreviewFrameRates();
    }

    @Override
    public void setPreviewFpsRange(final int min, final int max) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setPreviewFpsRange(min, max);
            }
        });
    }

    @Override
    public void getPreviewFpsRange(int[] range) {
        mParameters.getPreviewFpsRange(range);
    }

    @Override
    public List<int[]> getSupportedPreviewFpsRange() {
        return mParameters.getSupportedPreviewFpsRange();
    }

    @Override
    public void setPreviewFormat(final int pixel_format) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setPreviewFormat(pixel_format);
            }
        });
    }

    @Override
    public int getPreviewFormat() {
        return mParameters.getPreviewFormat();
    }

    @Override
    public List<Integer> getSupportedPreviewFormats() {
        return mParameters.getSupportedPreviewFormats();
    }

    @Override
    public void setPictureSize(final int width, final int height) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setPictureSize(width, height);
            }
        });
    }

    @Override
    public Size getPictureSize() {
        return mParameters.getPictureSize();
    }

    @Override
    public List<Size> getSupportedPictureSizes() {
        return mParameters.getSupportedPictureSizes();
    }

    @Override
    public void setPictureFormat(final int pixel_format) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setPictureFormat(pixel_format);
            }
        });
    }

    @Override
    public int getPictureFormat() {
        return mParameters.getPictureFormat();
    }

    @Override
    public List<Integer> getSupportedPictureFormats() {
        return mParameters.getSupportedPictureFormats();
    }

    @Override
    public void setRotation(final int rotation) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setRotation(rotation);
            }
        });
    }

    @Override
    public void setGpsLatitude(final double latitude) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setGpsLatitude(latitude);
            }
        });
    }

    @Override
    public void setGpsLongitude(final double longitude) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setGpsLongitude(longitude);
            }
        });
    }

    @Override
    public void setGpsAltitude(final double altitude) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setGpsAltitude(altitude);
            }
        });
    }

    @Override
    public void setGpsTimestamp(final long timestamp) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setGpsTimestamp(timestamp);
            }
        });
    }

    @Override
    public void setGpsProcessingMethod(final String processing_method) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setGpsProcessingMethod(processing_method);
            }
        });
    }

    @Override
    public void removeGpsData() {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.removeGpsData();
            }
        });
    }

    @Override
    public String getWhiteBalance() {
        return mParameters.getWhiteBalance();
    }

    @Override
    public void setWhiteBalance(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setWhiteBalance(value);
            }
        });
    }

    @Override
    public List<String> getSupportedWhiteBalance() {
        return mParameters.getSupportedWhiteBalance();
    }

    @Override
    public String getColorEffect() {
        return mParameters.getColorEffect();
    }

    @Override
    public void setColorEffect(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setColorEffect(value);
            }
        });
    }

    @Override
    public List<String> getSupportedColorEffects() {
        return mParameters.getSupportedColorEffects();
    }

    @Override
    public String getAntibanding() {
        return mParameters.getAntibanding();
    }

    @Override
    public void setAntibanding(final String antibanding) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setAntibanding(antibanding);
            }
        });
    }

    @Override
    public List<String> getSupportedAntibanding() {
        return mParameters.getSupportedAntibanding();
    }

    @Override
    public String getEisMode() {
        return mParameters.getEisMode();
    }

    @Override
    public void setEisMode(final String eis) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setEisMode(eis);
            }
        });
    }

    @Override
    public List<String> getSupportedEisMode() {
        return mParameters.getSupportedEisMode();
    }

    @Override
    public String getAFLampMode() {
        return mParameters.getAFLampMode();
    }

    @Override
    public void setAFLampMode(final String aflamp) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setAFLampMode(aflamp);
            }
        });
    }

    @Override
    public List<String> getSupportedAFLampMode() {
        return mParameters.getSupportedAFLampMode();
    }

    @Override
    public String getSceneMode() {
        return mParameters.getSceneMode();
    }

    @Override
    public void setSceneMode(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setSceneMode(value);
            }
        });
    }

    @Override
    public List<String> getSupportedSceneModes() {
        return mParameters.getSupportedSceneModes();
    }

    @Override
    public String getFlashMode() {
        return mParameters.getFlashMode();
    }

    @Override
    public void setFlashMode(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setFlashMode(value);
            }
        });
    }

    @Override
    public List<String> getSupportedFlashModes() {
        return mParameters.getSupportedFlashModes();
    }

    @Override
    public String getFocusMode() {
        return mParameters.getFocusMode();
    }

    @Override
    public void setFocusMode(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setFocusMode(value);
            }
        });
    }

    @Override
    public List<String> getSupportedFocusModes() {
        return mParameters.getSupportedFocusModes();
    }

    @Override
    public float getFocalLength() {
        return mParameters.getFocalLength();
    }

    @Override
    public float getHorizontalViewAngle() {
        return mParameters.getHorizontalViewAngle();
    }

    @Override
    public float getVerticalViewAngle() {
        return mParameters.getVerticalViewAngle();
    }

    @Override
    public int getExposureCompensation() {
        return mParameters.getExposureCompensation();
    }

    @Override
    public void setExposureCompensation(final int value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setExposureCompensation(value);
            }
        });
    }

    @Override
    public int getMaxExposureCompensation() {
        return mParameters.getMaxExposureCompensation();
    }

    @Override
    public int getMinExposureCompensation() {
        return mParameters.getMinExposureCompensation();
    }

    @Override
    public float getExposureCompensationStep() {
        return mParameters.getExposureCompensationStep();
    }

    @Override
    public void setAutoExposureLock(final boolean toggle) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setAutoExposureLock(toggle);
            }
        });
    }

    @Override
    public boolean getAutoExposureLock() {
        return mParameters.getAutoExposureLock();
    }

    @Override
    public boolean isAutoExposureLockSupported() {
        return mParameters.isAutoExposureLockSupported();
    }

    @Override
    public void setAutoWhiteBalanceLock(final boolean toggle) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setAutoWhiteBalanceLock(toggle);
            }
        });
    }

    @Override
    public boolean getAutoWhiteBalanceLock() {
        return mParameters.getAutoWhiteBalanceLock();
    }

    @Override
    public boolean isAutoWhiteBalanceLockSupported() {
        return mParameters.isAutoWhiteBalanceLockSupported();
    }

    @Override
    public int getZoom() {
        return mParameters.getZoom();
    }

    @Override
    public void setZoom(final int value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setZoom(value);
            }
        });
    }

    @Override
    public boolean isZoomSupported() {
        return mParameters.isZoomSupported();
    }

    @Override
    public int getMaxZoom() {
        return mParameters.getMaxZoom();
    }

    @Override
    public List<Integer> getZoomRatios() {
        return mParameters.getZoomRatios();
    }

    @Override
    public boolean isSmoothZoomSupported() {
        return mParameters.isSmoothZoomSupported();
    }

    @Override
    public void setCameraMode(final int value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setCameraMode(value);
            }
        });
    }

    @Override
    public String getISOSpeed() {
        return mParameters.getISOSpeed();
    }

    @Override
    public void setISOSpeed(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setISOSpeed(value);
            }
        });
    }

    @Override
    public List<String> getSupportedISOSpeed() {
        return mParameters.getSupportedISOSpeed();
    }

    @Override
    public int getMaxNumDetectedObjects() {
        return mParameters.getMaxNumDetectedObjects();
    }

    @Override
    public String getFDMode() {
        return mParameters.getFDMode();
    }

    @Override
    public void setFDMode(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setFDMode(value);
            }
        });
    }

    @Override
    public List<String> getSupportedFDMode() {
        return mParameters.getSupportedFDMode();
    }

    @Override
    public String getEdgeMode() {
        return mParameters.getEdgeMode();
    }

    @Override
    public void setEdgeMode(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setEdgeMode(value);
            }
        });
    }

    @Override
    public List<String> getSupportedEdgeMode() {
        return mParameters.getSupportedEdgeMode();
    }

    @Override
    public String getHueMode() {
        return mParameters.getHueMode();
    }

    @Override
    public void setHueMode(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setHueMode(value);
            }
        });
    }

    @Override
    public List<String> getSupportedHueMode() {
        return mParameters.getSupportedHueMode();
    }

    @Override
    public String getSaturationMode() {
        return mParameters.getSaturationMode();
    }

    @Override
    public void setSaturationMode(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setSaturationMode(value);
            }
        });
    }

    @Override
    public List<String> getSupportedSaturationMode() {
        return mParameters.getSupportedSaturationMode();
    }

    @Override
    public String getBrightnessMode() {
        return mParameters.getBrightnessMode();
    }

    @Override
    public void setBrightnessMode(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setBrightnessMode(value);
            }
        });
    }

    @Override
    public List<String> getSupportedBrightnessMode() {
        return mParameters.getSupportedBrightnessMode();
    }

    @Override
    public String getContrastMode() {
        return mParameters.getContrastMode();
    }

    @Override
    public void setContrastMode(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setContrastMode(value);
            }
        });
    }

    @Override
    public List<String> getSupportedContrastMode() {
        return mParameters.getSupportedContrastMode();
    }

    @Override
    public String getCaptureMode() {
        return mParameters.getCaptureMode();
    }

    @Override
    public void setCaptureMode(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setCaptureMode(value);
            }
        });
    }

    @Override
    public List<String> getSupportedCaptureMode() {
        return mParameters.getSupportedCaptureMode();
    }

    @Override
    public void setCapturePath(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setCapturePath(value);
            }
        });
    }

    @Override
    public void setBurstShotNum(final int value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setBurstShotNum(value);
            }
        });
    }

    @Override
    public void setFocusEngMode(final int mode) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setFocusEngMode(mode);
            }
        });
    }

    @Override
    public int getBestFocusStep() {
        return mParameters.getBestFocusStep();
    }

    @Override
    public void setRawDumpFlag(final boolean toggle) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setRawDumpFlag(toggle);
            }
        });
    }

    @Override
    public void setPreviewRawDumpResolution(final int value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setPreviewRawDumpResolution(value);
            }
        });
    }

    @Override
    public int getMaxFocusStep() {
        return mParameters.getMaxFocusStep();
    }

    @Override
    public int getMinFocusStep() {
        return mParameters.getMinFocusStep();
    }

    @Override
    public void setFocusEngStep(final int step) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setFocusEngStep(step);
            }
        });
    }

    @Override
    public void setExposureMeterMode(final String mode) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setExposureMeterMode(mode);
            }
        });
    }

    @Override
    public String getExposureMeterMode() {
        return mParameters.getExposureMeterMode();
    }

    @Override
    public int getSensorType() {
        return mParameters.getSensorType();
    }

    @Override
    public void setEngAEEnable(final int enable) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setEngAEEnable(enable);
            }
        });
    }

    @Override
    public void setEngFlashDuty(final int duty) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setEngFlashDuty(duty);
            }
        });
    }

    @Override
    public void setEngZSDEnable(final int enable) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setEngZSDEnable(enable);
            }
        });
    }

    @Override
    public int getEngPreviewShutterSpeed() {
        return mParameters.getEngPreviewShutterSpeed();
    }

    @Override
    public int getEngPreviewSensorGain() {
        return mParameters.getEngPreviewSensorGain();
    }

    @Override
    public int getEngPreviewISPGain() {
        return mParameters.getEngPreviewISPGain();
    }

    @Override
    public int getEngPreviewAEIndex() {
        return mParameters.getEngPreviewAEIndex();
    }

    @Override
    public int getEngCaptureSensorGain() {
        return mParameters.getEngCaptureSensorGain();
    }

    @Override
    public int getEngCaptureISPGain() {
        return mParameters.getEngCaptureISPGain();
    }

    @Override
    public int getEngCaptureShutterSpeed() {
        return mParameters.getEngCaptureShutterSpeed();
    }

    @Override
    public int getEngCaptureISO() {
        return mParameters.getEngCaptureISO();
    }

    @Override
    public int getEngFlashDutyMin() {
        return mParameters.getEngFlashDutyMin();
    }

    @Override
    public int getEngFlashDutyMax() {
        return mParameters.getEngFlashDutyMax();
    }

    @Override
    public int getEngPreviewFPS() {
        return mParameters.getEngPreviewFPS();
    }

    @Override
    public String getEngEngMSG() {
        return mParameters.getEngEngMSG();
    }

    @Override
    public void setEngFocusFullScanFrameInterval(final int n) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setEngFocusFullScanFrameInterval(n);
            }
        });
    }

    @Override
    public int getEngFocusFullScanFrameIntervalMax() {
        return mParameters.getEngFocusFullScanFrameIntervalMax();
    }

    @Override
    public int getEngFocusFullScanFrameIntervalMin() {
        return mParameters.getEngFocusFullScanFrameIntervalMin();
    }

    @Override
    public int getEngPreviewFrameIntervalInUS() {
        return mParameters.getEngPreviewFrameIntervalInUS();
    }

    @Override
    public void setEngParameter1(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setEngParameter1(value);
            }
        });
    }

    @Override
    public void setEngParameter2(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setEngParameter2(value);
            }
        });
    }

    @Override
    public void setEngParameter3(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setEngParameter3(value);
            }
        });
    }

    @Override
    public void setEngSaveShadingTable(final int save) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setEngSaveShadingTable(save);
            }
        });
    }

    @Override
    public void setEngShadingTable(final int shading_table) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setEngShadingTable(shading_table);
            }
        });
    }

    @Override
    public int getEngEVCalOffset() {
        return mParameters.getEngEVCalOffset();
    }

    @Override
    public void setMATVDelay(final int ms) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setMATVDelay(ms);
            }
        });
    }

    @Override
    public String getStereo3DType() {
        return mParameters.getStereo3DType();
    }

    @Override
    public void setStereo3DMode(final boolean enable) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setStereo3DMode(enable);
            }
        });
    }

    @Override
    public void setContinuousSpeedMode(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setContinuousSpeedMode(value);
            }
        });
    }

    @Override
    public String getZSDMode() {
        return mParameters.getZSDMode();
    }

    @Override
    public void setZSDMode(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setZSDMode(value);
            }
        });
    }

    @Override
    public List<String> getSupportedZSDMode() {
        return mParameters.getSupportedZSDMode();
    }

    @Override
    public void getFocusDistances(float[] output) {
        mParameters.getFocusDistances(output);
    }

    @Override
    public List<Area> getFocusAreas() {
        return mParameters.getFocusAreas();
    }

    @Override
    public void setFocusAreas(final List<Area> focusAreas) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setFocusAreas(focusAreas);
            }
        });
    }

    @Override
    public int getMaxNumMeteringAreas() {
        return mParameters.getMaxNumMeteringAreas();
    }

    @Override
    public List<Area> getMeteringAreas() {
        return mParameters.getMeteringAreas();
    }

    @Override
    public void setMeteringAreas(final List<Area> meteringAreas) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setMeteringAreas(meteringAreas);
            }
        });
    }

    @Override
    public int getMaxNumDetectedFaces() {
        return mParameters.getMaxNumDetectedFaces();
    }

    @Override
    public void setRecordingHint(final boolean hint) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setRecordingHint(hint);
            }
        });
    }

    @Override
    public boolean isVideoSnapshotSupported() {
        return mParameters.isVideoSnapshotSupported();
    }

    @Override
    public void enableRecordingSound(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.enableRecordingSound(value);
            }
        });
    }

    @Override
    public void setVideoStabilization(final boolean toggle) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setVideoStabilization(toggle);
            }
        });
    }

    @Override
    public boolean getVideoStabilization() {
        return mParameters.getVideoStabilization();
    }

    @Override
    public boolean isVideoStabilizationSupported() {
        return mParameters.isVideoStabilizationSupported();
    }

    @Override
    public List<Integer> getPIPFrameRateZSDOn() {
        return mParameters.getPIPFrameRateZSDOn();
    }

    @Override
    public List<Integer> getPIPFrameRateZSDOff() {
        return mParameters.getPIPFrameRateZSDOff();
    }

    @Override
    public boolean getDynamicFrameRate() {
        return mParameters.getDynamicFrameRate();
    }

    @Override
    public void setDynamicFrameRate(final boolean toggle) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setDynamicFrameRate(toggle);
            }
        });
    }

    @Override
    public boolean isDynamicFrameRateSupported() {
        return mParameters.isDynamicFrameRateSupported();
    }

    @Override
    public void setRefocusJpsFileName(final String fineName) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setRefocusJpsFileName(fineName);
            }
        });
    }

    @Override
    public String getDepthAFMode() {
        return mParameters.getDepthAFMode();
    }

    @Override
    public String getDistanceMode() {
        return mParameters.getDistanceMode();
    }

    @Override
    public void setDepthAFMode(final boolean isDepthAfMode) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setDepthAFMode(isDepthAfMode);
            }
        });
    }

    @Override
    public void setDistanceMode(final boolean isDistanceMode) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setDistanceMode(isDistanceMode);
            }
        });
    }


    public void setRefocusMode(final boolean isOpen) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setRefocusMode(isOpen);
            }
        });
    }

    private void lockRun(Runnable runnable) {
        if (mCameraDevice != null) {
            mCameraDevice.lockParametersRun(runnable);
        }
    }

}
