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

package com.mediatek.camera.mode.pip.pipwrapping;

import java.util.ArrayList;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLDisplay;
import android.util.Log;

/**
 * This class is used to create a specific EGLConfig.
*/
public class EGLConfigWrapper {
    private static final String TAG = EGLConfigWrapper.class.getSimpleName();
    private static final int EGL_RECORDABLE_ANDROID = 0x3142;
    private static final int EGL_OPENGL_ES2_BIT = 4;
    private EGLConfigChooser mEGLConfigChooser;
    private int mSelectedPixelFormat = -1;
    private ArrayList<Integer> mSupportedFormats = new ArrayList<Integer>();

    /**
     * An interface for choosing an EGLConfig configuration from a list of
     * potential configurations.
     * <p>
     * This interface must be implemented by clients wishing to call
     * {@link PipEGLConfigWrapper#setEGLConfigChooser(EGLConfigChooser)}
     */
    private interface EGLConfigChooser {
        /**
         * Choose a configuration from the list. Implementors typically
         * implement this method by calling
         * {@link EGL14#eglChooseConfig} and iterating through the results. Please consult the
         * EGL specification available from The Khronos Group to learn how to call eglChooseConfig.
         */
        EGLConfig chooseConfigEGL14(EGLDisplay display, boolean recording);
    }

    public EGLConfigWrapper() {

    }
    public void setSupportedFormats(int[] formats) {
        for(int format:formats) {
            Log.i(TAG, "setSupportedFormats,format:" + format);
            mSupportedFormats.add(format);
        }
    }

    /**
      * Install a custom EGLConfigChooser.
      * <p>If this method is
      * called, it must be called before GL thread initialize.
      * <p>
      * If no setEGLConfigChooser method is called, then by default the
      * view will choose a config as close to 16-bit RGB as possible, with
      * a depth buffer as close to 16 bits as possible.
      * @param configChooser
      */
     public void setEGLConfigChooser(EGLConfigChooser configChooser) {
         mEGLConfigChooser = configChooser;
     }

    /**
     * Install a config chooser which will choose a config
     * with at least the specified component sizes, and as close
     * to the specified component sizes as possible.
     * <p>If this method is
     * called, it must be called before GL thread initialize.
     * <p>
     * If no setEGLConfigChooser method is called, then by default the
     * view will choose a config as close to 16-bit RGB as possible, with
     * a depth buffer as close to 16 bits as possible.
     *
     */
    public void setEGLConfigChooser(int redSize, int greenSize, int blueSize,
            int alphaSize, int depthSize, int stencilSize) {
        setEGLConfigChooser(new ComponentSizeChooser(redSize, greenSize,
                blueSize, alphaSize, depthSize, stencilSize));
    }

    public EGLConfig chooseConfigEGL14(EGLDisplay display, boolean recording) {
        if (mEGLConfigChooser == null) {
            mEGLConfigChooser = new SimpleEGLConfigChooser(recording);
        }
        if (mSupportedFormats.size() <= 0) {
            mSupportedFormats.add(PixelFormat.RGBA_8888);
        }
        return mEGLConfigChooser.chooseConfigEGL14(display, recording);
    }

    public int getSelectedPixelFormat() {
        return mSelectedPixelFormat;
    }

    private abstract class BaseConfigChooser
        implements EGLConfigChooser {
        public BaseConfigChooser(int[] configSpec) {
            mConfigSpec = configSpec;
        }

        @Override
        public EGLConfig chooseConfigEGL14(
                EGLDisplay display, boolean recording) {
            EGLConfig[] configs = new EGLConfig[100];
            int[] numConfigs = new int[1];
            int surfaceType = EGL14.EGL_WINDOW_BIT | EGL14.EGL_PBUFFER_BIT;
            if (recording) {
                surfaceType = EGL14.EGL_WINDOW_BIT;
                mConfigSpec[mConfigSpec.length - 3] = EGL_RECORDABLE_ANDROID;
                mConfigSpec[mConfigSpec.length - 2] = 1;
            }
            mConfigSpec[mConfigSpec.length - 5] = EGL14.EGL_SURFACE_TYPE;
            mConfigSpec[mConfigSpec.length - 4] = surfaceType;
            if (!EGL14.eglChooseConfig(display, mConfigSpec, 0, configs, 0, configs.length,
                    numConfigs, 0)) {
                throw new RuntimeException("unable to find ES2 EGL config in EGL14");
            }
            EGLConfig config = chooseConfigEGL14(display, configs, numConfigs[0], recording);
            if (config == null) {
                throw new IllegalArgumentException("No config chosen");
            }
            return config;
        }

        abstract EGLConfig chooseConfigEGL14(EGLDisplay display,
                EGLConfig[] configs, int config_num, boolean recording);

        protected int[] mConfigSpec;
    }

    private class ComponentSizeChooser extends BaseConfigChooser {
        public ComponentSizeChooser(int redSize, int greenSize, int blueSize,
                int alphaSize, int depthSize, int stencilSize) {
            super(new int[] {
                    EGL14.EGL_RED_SIZE, redSize,
                    EGL14.EGL_GREEN_SIZE, greenSize,
                    EGL14.EGL_BLUE_SIZE, blueSize,
                    EGL14.EGL_ALPHA_SIZE, alphaSize,
                    EGL14.EGL_DEPTH_SIZE, depthSize,
                    EGL14.EGL_STENCIL_SIZE, stencilSize,
                    EGL14.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_NONE, 0, // placeholder for surface type
                    EGL14.EGL_NONE, 0, // placeholder for recordable
                    EGL14.EGL_NONE});
            mValue = new int[1];
            mRedSize = redSize;
            mGreenSize = greenSize;
            mBlueSize = blueSize;
            mAlphaSize = alphaSize;
            mDepthSize = depthSize;
            mStencilSize = stencilSize;
            Log.i(TAG, "R:" + mRedSize + ",G:" + mGreenSize
                    + "B:" + mBlueSize + ",A:" + mAlphaSize
                    + "Depth:" + mDepthSize + ",Stencil:" + mStencilSize);
        }

        @Override
        EGLConfig chooseConfigEGL14(
                EGLDisplay display,
                EGLConfig[] configs, int config_num, boolean recording) {
            EGLConfig closestConfig = null;
            if (recording) {
                closestConfig = findRecordEglConfig(display, configs, config_num);
            }
            // if recordable config not support, try find a cloest config
            if (closestConfig == null) {
                closestConfig = findClosestEglConfig(display, configs, config_num);
            }
            return closestConfig;
        }

        private int findConfigAttrib(EGLDisplay display,
                EGLConfig config, int attribute, int defaultValue) {
            if (EGL14.eglGetConfigAttrib(display, config, attribute, mValue, 0)) {
                return mValue[0];
            }
            return defaultValue;
        }

        private EGLConfig findRecordEglConfig(EGLDisplay display,
                EGLConfig[] configs, int config_num) {
            EGLConfig closestConfig = null;
            int closestDistance = 1000;
            for (int i = 0; i < config_num; i++) {
                int visualId = findConfigAttrib(display, configs[i],
                        EGL14.EGL_NATIVE_VISUAL_ID, 0);
                int r = findConfigAttrib(display, configs[i],
                        EGL14.EGL_RED_SIZE, 0);
                int g = findConfigAttrib(display, configs[i],
                        EGL14.EGL_GREEN_SIZE, 0);
                int b = findConfigAttrib(display, configs[i],
                        EGL14.EGL_BLUE_SIZE, 0);
                int distance =
                        Math.abs(r - mRedSize)
                      + Math.abs(g - mGreenSize)
                      + Math.abs(b - mBlueSize);
                if (isInSupportedFormats(visualId) &&
                        isYuvVisualId(visualId) &&
                            distance < closestDistance) {
                    closestDistance = distance;
                    closestConfig = configs[i];
                    mSelectedPixelFormat = visualId;
                }
            }
            Log.i(TAG, "Try find recordable config, with result:" + closestConfig
                    + " visualId = " + mSelectedPixelFormat);
            return closestConfig;
        }

        private EGLConfig findClosestEglConfig(EGLDisplay display,
                EGLConfig[] configs, int config_num) {
            EGLConfig closestConfig = null;
            int closestDistance = 1000;
            for (int i = 0; i < config_num; i++) {
                int d = findConfigAttrib(display, configs[i],
                        EGL14.EGL_DEPTH_SIZE, 0);
                int s = findConfigAttrib(display, configs[i],
                        EGL14.EGL_STENCIL_SIZE, 0);
                int visualId = findConfigAttrib(display, configs[i],
                        EGL14.EGL_NATIVE_VISUAL_ID, 0);
                int surfaceType = findConfigAttrib(display, configs[i],
                        EGL14.EGL_SURFACE_TYPE, 0);
                if (d >= mDepthSize && s >= mStencilSize) {
                    int r = findConfigAttrib(display, configs[i],
                            EGL14.EGL_RED_SIZE, 0);
                    int g = findConfigAttrib(display, configs[i],
                            EGL14.EGL_GREEN_SIZE, 0);
                    int b = findConfigAttrib(display, configs[i],
                            EGL14.EGL_BLUE_SIZE, 0);
                    int a = findConfigAttrib(display, configs[i],
                            EGL14.EGL_ALPHA_SIZE, 0);
                    int distance =
                              Math.abs(r - mRedSize)
                            + Math.abs(g - mGreenSize)
                            + Math.abs(b - mBlueSize)
                            + Math.abs(a - mAlphaSize);
                    Log.i(TAG, "Try EGL14 choose: depth = " + d
                            + " stencil = " + s
                            + " red = " + r
                            + " green = " + g
                            + " blue = " + b
                            + " alpha = " + a
                            + " distance = " + distance
                            + " visual id = " + visualId
                            + " surfaceType = " + surfaceType);
                    if (isInSupportedFormats(visualId) &&
                            distance < closestDistance) {
                        closestDistance = distance;
                        closestConfig = configs[i];
                        mSelectedPixelFormat = visualId;
                    }
                }
            }
            Log.i(TAG, "Find format: " + mSelectedPixelFormat);
            return closestConfig;
        }

        private boolean isYuvVisualId(int visualId) {
            switch (visualId) {
            case ImageFormat.YV12:
            case ImageFormat.YUV_420_888:
            case ImageFormat.NV21:
                return true;
            default:
                return false;
            }
        }

        private int[] mValue;
        protected int mRedSize;
        protected int mGreenSize;
        protected int mBlueSize;
        protected int mAlphaSize;
        protected int mDepthSize;
        protected int mStencilSize;
    }

    private class SimpleEGLConfigChooser extends ComponentSizeChooser {
        public SimpleEGLConfigChooser(boolean recording) {
            super(8, 8, 8, recording ? 0 : 8, 0, 0);
        }
    }
    private boolean isInSupportedFormats(int format) {
        return mSupportedFormats.contains(format);
    }
}