/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * MediaTek Inc. (C) 2015. All rights reserved.
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
package com.mediatek.mmsdk;

import android.os.Handler;
import android.view.Surface;

import com.mediatek.mmsdk.CameraEffectHalException;

import java.util.List;

/**
 * The CameraEffect class is a representation of a connection to Effect HAL
 * @hide
 */
public abstract class CameraEffect implements AutoCloseable {

    /**
     * Create the effect session.
     * @param outputs
     *            The new set of Surfaces that should be made available as
     *            targets for captured image data.
     * @param surfaceParameters
     *            the parameters which is need for the surface.
     * @param callback
     *            The callback is be notified about the status of the new
     *            capture session.
     * @param handler
     *            The handler on which the callback should be invoked, or
     *            {@code null} to use the current thread's
     *            {@link android.os.Looper looper}.
     * @return Return the current created CameraEffectSession.
     * @throws CameraEffectHalException
     *             if the effect HAL cann't be configured or in error state.
     * @hide
     */
    public abstract CameraEffectSession createCaptureSession(List<Surface> outputs,
            List<BaseParameters> surfaceParameters,
            CameraEffectSession.SessionStateCallback callback, Handler handler)
            throws CameraEffectHalException;

    /**
     * Change the settings for Effect HAL.
     * @param baseParameters
     *            the parameters to use for this Effect Hal.
     * @hide
     */
    public abstract void setParameters(BaseParameters baseParameters);

    /**
     * Get the input surface which is created by EffectHalClient,the set of
     * surface will be set to cameraService.when you have called this function
     * ,you can not change the parameters that influence the picture size and
     * surface format.
     * @return return the set of surface which is created by EffectHalClient.
     * @hide
     */
    public abstract List<Surface> getInputSurface();

    /**
     * Get the parameters which need for current effect.
     * @param parameters
     *            set the parameters to effect HAL.so Effect Hal will prepare
     *            the picture with this parameters.
     * @return according the set parameters will return other parameters must be
     *         change.
     * @hide
     */
    public abstract List<BaseParameters> getCaputreRequirement(BaseParameters parameters);

    /**
     * Close current Effect.
     * @hide
     */
    public abstract void closeEffect();

    /**
     * A callback objects for receiving updates about the state of a camera
     * Effect Hal
     * @hide
     */
    public static abstract class StateCallback {

        /**
         * indicate the effect hal is in use already.
         * @hide
         */
        public static final int ERROR_EFFECT_HAL_IN_USE = 1;

        /**
         * indicate the effect hal cannot be used due to Effect Hal in error
         * state or RemoteException.
         * @hide
         */
        public static final int ERROR_EFFECT_DISABLED = 3;

        /**
         * indicate the effect Hal is in error.
         * @hide
         */
        public static final int ERROR_EFFECT_DEVICE = 4;

        /**
         * indicate set the effect Hal listener is error.
         * @hide
         */
        public static final int ERROR_EFFECT_LISTENER = 6;

        /**
         * The method called when Effect Hal is no longer available for use.
         * @param effect
         *            the Effect Hal that has been disconnected
         * @hide
         */
        public abstract void onDisconnected(CameraEffect effect);

        /**
         * The method called when a camera device has encountered a serious
         * error.
         * @param effect
         *            the Effect Hal that has been in error state
         * @param error
         *            the detail error,see the :
         *            ERROR_EFFECT_HAL_IN_USE,ERROR_EFFECT_DISABLED
         *            ,ERROR_EFFECT_DEVICE,ERROR_EFFECT_LISTENER
         * @hide
         */
        public abstract void onError(CameraEffect effect, int error);

        /**
         * The method called when Effect Hal has been closed.
         * @param effect
         *            the Effect Hal that has been closed.
         * @hide
         */
        public void onClosed(CameraEffect effect) {
            // Default empty implementation
        }
    }

    /**
     * Closes the object and release any system resources it holds.
     */
    @Override
    public abstract void close();
}
