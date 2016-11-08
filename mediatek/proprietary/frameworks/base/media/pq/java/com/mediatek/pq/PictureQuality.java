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
 * MediaTek Inc. (C) 2010. All rights reserved.
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

/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.mediatek.pq;

import android.util.Log;
import android.os.SystemProperties;

public class PictureQuality {

    static boolean sLibStatus = true;

    /**
     * @internal
     */
    public static final int MODE_NORMAL = 0x000000;

    /**
     * @internal
     */
    public static final int MODE_CAMERA = 0x000001;
    public static final int MODE_VIDEO = 0x000002;
    /**
     * @internal
     */
    public static final int MODE_MASK   = 0x000001;

    public static final int PIC_MODE_STANDARD = 0;
    public static final int PIC_MODE_VIVID = 1;
    public static final int PIC_MODE_USER_DEF = 2;

    public static final int DCHIST_INFO_NUM = 20;
    public static class Hist {
        public int info[];

        public Hist() {
            info = new int[DCHIST_INFO_NUM];
            for (int i = 0; i < DCHIST_INFO_NUM; i++) {
                set(i, 0);
            }
        }

        public void set(int index, int value) {
            if ((0 <= index) && (index < DCHIST_INFO_NUM)) {
                this.info[index] = value;
            }
        }
    }

    public static class Range {
        public int min;
        public int max;
        public int defaultValue;

        public Range() {
            set(0, 0, 0);
        }

        public void set(int min, int max, int defaultValue) {
            this.min = min;
            this.max = max;
            this.defaultValue = defaultValue;
        }
    }

    public static final int GAMMA_LUT_SIZE = 512;
    public static class GammaLut {
        public int hwid;
        public int lut[];

        public GammaLut() {
            lut = new int[GAMMA_LUT_SIZE];
            for (int i = 0; i < GAMMA_LUT_SIZE; i++) {
                set(i, 0);
            }
        }

        public void set(int index, int value) {
            if ((0 <= index) && (index < GAMMA_LUT_SIZE)) {
                this.lut[index] = value;
            }
        }
    }

    static {
        try {
            Log.v("JNI_PQ", "loadLibrary");
            System.loadLibrary("jni_pq");
        } catch (UnsatisfiedLinkError e) {
            Log.e("JNI_PQ", "UnsatisfiedLinkError");
            sLibStatus = false;
        }
    }

    public static boolean getLibStatus() {
        return sLibStatus;
    }

    public static final int CAPABILITY_MASK_COLOR       = 0x00000001;
    public static final int CAPABILITY_MASK_SHARPNESS   = 0x00000002;
    public static final int CAPABILITY_MASK_GAMMA       = 0x00000004;
    public static final int CAPABILITY_MASK_DC          = 0x00000008;
    public static final int CAPABILITY_MASK_OD          = 0x00000010;
        public static int getCapability() {
        return nativeGetCapability();
    }


    public static String setMode(int mode)
    {
        if(mode == PictureQuality.MODE_CAMERA)
        {
            nativeSetCameraPreviewMode();
        }
        else if (mode == PictureQuality.MODE_VIDEO)
        {
            nativeSetVideoPlaybackMode();
        }

        else
        {
            nativeSetGalleryNormalMode();
        }

        return null;
    }

    public static Hist getDynamicContrastHistogram(byte[] srcBuffer, int srcWidth, int srcHeight) {
        Hist outHist = new Hist();
        nativeGetDynamicContrastHistogram(srcBuffer, srcWidth, srcHeight, outHist);
        return outHist;
    }

    // enable PQ: 0 is disable, 1 is enable
    public static boolean enablePQ(int isEnable)
    {
        return nativeEnablePQ(isEnable);
    }

    // enable Color: 0 is disable, 1 is enable
    public static boolean enableColor(int isEnable)
    {
        return nativeEnableColor(isEnable);
    }

    // enable Sharpness: 0 is disable, 1 is enable
    public static boolean enableSharpness(int isEnable)
    {
        return nativeEnableSharpness(isEnable);
    }

    // enable Dynamic Contrast: 0 is disable, 1 is enable
    public static boolean enableDynamicContrast(int isEnable)
    {
        return nativeEnableDynamicContrast(isEnable);
    }

    // enable Color Effect: 0 is disable, 1 is enable
    public static boolean enableColorEffect(int isEnable)
    {
        return nativeEnableColorEffect(isEnable);
    }

    // enable Gamma: 0 is disable, 1 is enable
    public static boolean enableGamma(int isEnable)
    {
        return nativeEnableGamma(isEnable);
    }

    // enable OD Demo: 0 is disable, 1 is enable
    public static boolean enableOD(int isEnable) {
        return nativeEnableOD(isEnable);
    }

    // Picture Mode: STANDARD / VIVID / USER_DEF
    public static int getPictureMode()
    {
        return nativeGetPictureMode();
    }

    // Picture Mode: STANDARD / VIVID / USER_DEF
    public static boolean setPictureMode(int mode)
    {
        return nativeSetPictureMode(mode);
    }

    // COLOR ROI.
    public static boolean setColorRegion(int isEnable, int startX, int startY,
            int endX, int endY)
    {
        return nativeSetColorRegion(isEnable, startX, startY, endX, endY);
    }

    // Contrast
    public static Range getContrastIndexRange() {
        Range r = new Range();
        nativeGetContrastIndexRange(r);
        return r;
    }

    public static int getContrastIndex() {
        return nativeGetContrastIndex();
    }

    public static void setContrastIndex(int index) {
        nativeSetContrastIndex(index);
    }

    // Saturation
    public static Range getSaturationIndexRange() {
        Range r = new Range();
        nativeGetSaturationIndexRange(r);
        return r;
    }

    public static int getSaturationIndex() {
        return nativeGetSaturationIndex();
    }

    public static void setSaturationIndex(int index) {
        nativeSetSaturationIndex(index);
    }

    // PicBrightness
    public static Range getPicBrightnessIndexRange() {
        Range r = new Range();
        nativeGetPicBrightnessIndexRange(r);
        return r;
    }

    public static int getPicBrightnessIndex() {
        return nativeGetPicBrightnessIndex();
    }

    public static void setPicBrightnessIndex(int index) {
        nativeSetPicBrightnessIndex(index);
    }

    // Sharpness
    public static Range getSharpnessIndexRange() {
        Range r = new Range();
        nativeGetSharpnessIndexRange(r);
        return r;
    }

    public static int getSharpnessIndex() {
        return nativeGetSharpnessIndex();
    }

    public static void setSharpnessIndex(int index) {
        nativeSetSharpnessIndex(index);
    }

    // Dynamic Contrast: 0 is disable, 1 is enable
    public static Range getDynamicContrastIndexRange() {
        Range r = new Range();
        nativeGetDynamicContrastIndexRange(r);
        return r;
    }

    public static int getDynamicContrastIndex() {
        return nativeGetDynamicContrastIndex();
    }

    public static void setDynamicContrastIndex(int index) {
        nativeSetDynamicContrastIndex(index);
    }

    // Color Effect: 0 is disable, 1 is enable
    public static Range getColorEffectIndexRange() {
        Range r = new Range();
        nativeGetColorEffectIndexRange(r);
        return r;
    }

    public static int getColorEffectIndex() {
        return nativeGetColorEffectIndex();
    }

    public static void setColorEffectIndex(int index) {
        nativeSetColorEffectIndex(index);
    }

    private static final String GAMMA_INDEX_PROPERTY_NAME = "persist.sys.gamma.index";

    /**
     * Get index range of gamma. The valid gamma index is in [Range.min,
     * Range.max].
     *
     * @see getGammaIndex, setGammaIndex
     */
    public static Range getGammaIndexRange() {
        Range r = new Range();
        nativeGetGammaIndexRange(r);
        return r;
    }

    /**
     * Set gamma index. The index value should be in [Range.min, Range.max].
     *
     * @see getGammaIndexRange, getGammaIndex
     */
    public static void setGammaIndex(int index) {
        SystemProperties.set(GAMMA_INDEX_PROPERTY_NAME, Integer.toString(index));
        nativeSetGammaIndex(index);
    }

    /**
     * Get current gamma index setting.
     *
     * @see setGammaIndex
     */
    public static int getGammaIndex() {
        return SystemProperties
                .getInt(GAMMA_INDEX_PROPERTY_NAME, getGammaIndexRange().defaultValue);
    }

    private static native int nativeGetCapability();

    private static native void nativeSetCameraPreviewMode();
    private static native void nativeSetGalleryNormalMode();
    private static native void nativeSetVideoPlaybackMode();
    private static native void nativeGetDynamicContrastHistogram(byte[] srcBuffer, int srcWidth, int srcHeight, Hist outHist);

    private static native boolean nativeEnablePQ(int isEnable);
    private static native boolean nativeEnableColor(int isEnable);
    private static native boolean nativeEnableSharpness(int isEnable);
    private static native boolean nativeEnableDynamicContrast(int isEnable);
    private static native boolean nativeEnableColorEffect(int isEnable);
    private static native boolean nativeEnableGamma(int isEnable);
    private static native boolean nativeEnableOD(int isEnable);
    private static native int nativeGetPictureMode();
    private static native boolean nativeSetPictureMode(int mode);
    private static native boolean nativeSetColorRegion(int isEnable, int startX, int startY,
            int endX, int endY);
    private static native void nativeGetContrastIndexRange(Range r);
    private static native int nativeGetContrastIndex();
    private static native void nativeSetContrastIndex(int index);
    private static native void nativeGetSaturationIndexRange(Range r);
    private static native int nativeGetSaturationIndex();
    private static native void nativeSetSaturationIndex(int index);
    private static native void nativeGetPicBrightnessIndexRange(Range r);
    private static native int nativeGetPicBrightnessIndex();
    private static native void nativeSetPicBrightnessIndex(int index);
    private static native void nativeGetSharpnessIndexRange(Range r);
    private static native int nativeGetSharpnessIndex();
    private static native void nativeSetSharpnessIndex(int index);
    private static native void nativeGetDynamicContrastIndexRange(Range r);
    private static native int nativeGetDynamicContrastIndex();
    private static native void nativeSetDynamicContrastIndex(int index);
    private static native void nativeGetColorEffectIndexRange(Range r);
    private static native int nativeGetColorEffectIndex();
    private static native void nativeSetColorEffectIndex(int index);
    private static native void nativeGetGammaIndexRange(Range r);
    private static native void nativeSetGammaIndex(int index);
}
