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
package com.android.camera;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.mediatek.camera.util.Log;

import android.app.Activity;
import android.content.Context;
import android.os.SystemProperties;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioSystem;

public class FeatureSwitcher {
    private static final String TAG = "FeatureSwitcher-L";

    private static final String MTK_FEATURE_SUPPORT = "1";
    private static final String MTK_EMULATOR_SUPPORT = "ro.mtk_emulator_support";
    private static final String MTK_AUDIO_HD_REC_SUPPORT = "MTK_AUDIO_HD_REC_SUPPORT";
    private static final String MTK_AUDIO_HD_REC_SUPPORT_ON = "MTK_AUDIO_HD_REC_SUPPORT=true";
    private static final String MTK_CHIP_0321 = "0321";
    private static final String MTK_CHIP_0335 = "0335";
    private static final String MTK_CHIP_0337 = "0337";
    private static final int MTK_RAM_512M = 512 * 1024;

    // used as flag to decide whether support double tap up to zoom/default
    // enabled=false means don't support double tap up
    // add for ais and mfll feature,
    public static final String MFB_AIS = "ais";
    public static final String MFB_OFF = "off";

    // and enabled = false can make touch focus faster 300ms than suppot double
    // tap up
    public static boolean isSupportDoubleTapUp() {
        boolean enabled = false;
        Log.d(TAG, "[isSupportDoubleTapUp]return : " + enabled);
        return enabled;
    }

    // M: the feature option of MTK_CAM_MFB_SUPPORT will return 4 number
    // 0 means off ,1 means mfll,2 means ais,3 means both ,but ap layer just
    // care ais feature
    public static boolean isAisSupported() {
        int enabled = SystemProperties.getInt("ro.mtk_cam_mfb_support", 0);
        boolean issupported = false;
        if (2 == enabled || 3 == enabled) {
            issupported = true;
        }
        Log.d(TAG, "[isAisMfllSupported]return :" + issupported);
        return issupported;

    }

    public static boolean isStereo3dEnable() {
        boolean enabled = false;
        Log.d(TAG, "isStereo3dEnable = " + enabled);
        return enabled;
    }

    public static boolean is2SdCardSwapSupport() {
        boolean enable = SystemProperties.getInt("ro.mtk_2sdcard_swap", 0) == 1 ? true : false;
        Log.d(TAG, "is2SdCardSwapSupport = " + enable);
        return enable;
    }

    public static boolean isVssEnable() {
        // TODO:
        boolean enabled = false;
        // enabled = FeatureOption.MTK_CAM_VSS_SUPPORT;
        Log.d(TAG, "isVSSEnable = " + enabled);
        return enabled;
    }

    public static boolean isStereoSingle3d() {
        boolean enabled = false;
        Log.d(TAG, "isStereoSingle3d = " + enabled);
        return enabled;
    }

    public static boolean isSlowMotionSupport() {
        boolean enable = SystemProperties.getInt("ro.mtk_slow_motion_support", 0) == 1 ? true
                : false;
        // add for slow motion on/off debug,
        // 0 means to get featureOption default values,
        // 1 means to force slowMotion on,
        // 2 means to force slowMotion off.
        int slowMotionOnOff = SystemProperties.getInt("slow_motion_on_off", 0);
        if (slowMotionOnOff == 1) {
            enable = true;
        } else if (slowMotionOnOff == 2) {
            enable = false;
        }
        Log.d(TAG, "isSlowMotionSupport() return " + enable);
        return enable;
    }

    public static boolean isGestureShotSupport() {
        // TODO:
        // boolean enabled = FeatureOption.MTK_CAM_GESTURE_SUPPORT;
        boolean enabled = false;
        Log.d(TAG, "isGestureShotSupport() return " + enabled);
        return enabled;
    }

    public static boolean isVoiceEnabled() {
        boolean enable = SystemProperties.getInt("ro.mtk_voice_ui_support", 0) == 1 ? true : false;
        Log.d(TAG, "isVoiceEnabled() return " + enable);
        return enable;
    }

    public static boolean isGmoROM() {
        boolean enable = SystemProperties.getInt("ro.mtk_gmo_rom_optimize", 0) == 1 ? true : false;
        Log.d(TAG, "isGmoRom() return " + enable);
        return enable;
    }

    public static boolean isGmoRAM() {
        boolean enable = SystemProperties.getInt("ro.mtk_gmo_ram_optimize", 0) == 1 ? true : false;
        Log.d(TAG, "isGmoRam() return " + enable);
        return enable;
    }

    public static boolean isLowRAM() {
        boolean enable = !(Util.getDeviceRam() > MTK_RAM_512M);
        Log.d(TAG, "isLowRAM() return " + enable);
        return enable;
    }

    public static String whichDeanliChip() {
        byte[] buf = new byte[8];
        StringBuilder sb = new StringBuilder();
        try {
            FileInputStream dir = new FileInputStream("/proc/chip/hw_code");
            int hasRead = 0;
            while ((hasRead = dir.read(buf)) > 0) {
                sb.append(new String(buf, 0, hasRead-1));
            }
            if (dir != null) {
                dir.close();
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException ", e);
        } catch (IOException e) {
            Log.e(TAG, "exception 1: ", e);
        }
        Log.i(TAG, "whichDeanliChip  " + sb.toString());
        if (MTK_CHIP_0321.equals(sb.toString())) {

            Log.i(TAG, "whichDeanliChip MTK_CHIP_0321");
            return MTK_CHIP_0321;

        } else if (MTK_CHIP_0335.equals(sb.toString())) {

            Log.i(TAG, "whichDeanliChip MTK_CHIP_0335");
            return MTK_CHIP_0335;

        } else if (MTK_CHIP_0337.equals(sb.toString())) {

            Log.i(TAG, "whichDeanliChip MTK_CHIP_0337");
            return MTK_CHIP_0337;

        } else {

            Log.i(TAG, "whichDeanliChip null");
            return null;
        }
    }

    public static boolean isOnlyCheckBackCamera() {
        // false will check all camera
        // true will only check back camera
        return false;
    }

    public static boolean isMtkFatOnNand() {
        boolean enable = SystemProperties.getInt("ro.mtk_fat_on_nand", 0) == 1 ? true : false;
        Log.d(TAG, "isMtkFatOnNand() return " + enable);
        return enable;
    }

    public static boolean isTablet() {
        boolean sIsScreenLarge = android.os.SystemProperties.get("ro.build.characteristics")
                .equals("tablet");
        Log.d(TAG, "IsTablet = " + sIsScreenLarge);
        return sIsScreenLarge;
    }

    public static boolean isMtkCaptureAnimationEnable() {
        boolean enabled = true;
        Log.d(TAG, "isMR2CaptureAnimationEnable() return " + enabled);
        return enabled;
    }

    public static boolean isNativePIPEnabled() {
        // TODO:need to check the system properity
        boolean enable = SystemProperties.getInt("ro.mtk_cam_native_pip_support", 0) == 1 ? true
                : false;
        boolean enabled = enable
                && CameraHolder.UNKONW_ID != CameraHolder.instance().getBackCameraId()
                && CameraHolder.UNKONW_ID != CameraHolder.instance().getFrontCameraId();
        Log.i(TAG, "isNativePIPEnabled() return " + enabled);
        return enabled;
    }

    public static boolean isLomoEffectEnabled() {
        boolean enable = SystemProperties.getInt("ro.mtk_cam_lomo_support", 0) == 1 ? true : false;
        Log.d(TAG, "[isLomoEffectEnabled]return :" + enable);
        return enable;
    }

    public static boolean isAppGuideEnable() {
        boolean enable = SystemProperties.getInt("camera.appguide.enable", 1) == 1 ? true : false;
        Log.i(TAG, "[isAppGuideEnable]return :" + enable);
        return enable;
    }

    public static boolean isVfbEnable() {
        boolean enable = SystemProperties.getInt("ro.mtk_cam_vfb", 0) == 1 ? true : false;
        //Log.i(TAG, "[isVfbEnable]return: " + enable);
        return enable;
    }

    public static boolean isEmulatorSupported() {
        boolean enable = MTK_FEATURE_SUPPORT.equals(SystemProperties.get(MTK_EMULATOR_SUPPORT));
        Log.i(TAG, "isEmulatorSupported() return " + enable);
        return enable;
    }

    public static boolean isHdRecordingEnabled() {
        String state = AudioSystem.getParameters(MTK_AUDIO_HD_REC_SUPPORT);
        boolean enabled = false;
        if (state != null) {
            enabled = state.equalsIgnoreCase(MTK_AUDIO_HD_REC_SUPPORT_ON);
        }
        Log.d(TAG, "isHdRecordingEnabled() return " + enabled);
        return enabled;
    }

    public static boolean isPrioritizePreviewSize() {
        return false;
    }

    public static boolean isDualCameraEnable() {
        String support = android.hardware.Camera.getProperty(
                "MTK_STEREO_FEATURE_SUPPORT", "false");
        boolean enabled = "true".equals(support);
        Log.i(TAG, "isDualCameraEnable() return " + enabled);
        return enabled;
    }

    public static boolean isSubSettingEnabled() {
        return isTablet() && !isLomoEffectEnabled();
    }

    public static boolean isZSDHDRSupported() {
        boolean enabled = SystemProperties.getInt("ro.mtk_zsdhdr_support", 0) == 1 ? true : false;
        Log.i(TAG, "isZSDHDRSupported(), enabled:" + enabled);
        return enabled;
    }

    public static boolean isApi2Enable(Activity activity) {
        boolean enable = false;
        CameraManager camManager = (CameraManager)
                activity.getSystemService(Context.CAMERA_SERVICE);
        int backCamLevel = -1;
        int frontCamLevel = -1;
        try {
            String[] cameraIds = camManager.getCameraIdList();
            if (cameraIds != null) {
                for (int i = 0; i < cameraIds.length; i++) {
                    if (String.valueOf(
                            CameraCharacteristics.LENS_FACING_FRONT).equalsIgnoreCase(
                                    cameraIds[i])) {
                        frontCamLevel = camManager.getCameraCharacteristics(cameraIds[i]).get(
                                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                    } else if (String.valueOf(
                            CameraCharacteristics.LENS_FACING_BACK).equalsIgnoreCase(
                                    cameraIds[i])) {
                        backCamLevel = camManager.getCameraCharacteristics(cameraIds[i]).get(
                                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                    }
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.i(TAG, "Check hardwarelevel with exception: " + e);
        }
        Log.i(TAG, "isHAL3Enable backCamLevel: " + backCamLevel +
                " frontCamLevel: " + frontCamLevel);
        if (backCamLevel != -1) {
            enable = enable || backCamLevel !=
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
        }
        if (frontCamLevel != -1) {
            enable = enable || frontCamLevel !=
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
        }
        int propValue = SystemProperties.getInt("camera.mtkapp.api2.enable", 0);
        enable = enable || (propValue > 0);
        Log.i(TAG, "isApi2Enable enable : " + enable + " propValue = " + propValue);
        return enable;
    }
}
