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
import com.mediatek.mmsdk.BaseParameters;

/**
 * A configured capture session for a CameraEffect, used for process the images
 * from the Effect HAL
 * @hide
 */
public abstract class CameraEffectSession implements AutoCloseable {

    /**
     * Submit a capture request to Effect Hal.
     * @param callback
     *            The callback object to be notified once this request has been
     *            processed.
     * @param handler
     *            the handler on which the listener should be invoked, or
     *            {@code null} to use the current thread's
     *            {@link android.os.Looper looper}.
     * @hide
     */
    public abstract void startCapture(CaptureCallback callback, Handler handler);

    /**
     * set the frame parameters on which frame and surface.
     * @param isInput
     *            if is true means set the parameters on input frame;otherwise
     *            is output frame.
     * @param index
     *            which the index of surface want to change the parameters.
     * @param baseParameters
     *            the parameters need to set.
     * @param timestamp
     *            which frame will have the effect on the frame.
     * @param repeating
     *            whether need to set the parameters to all the frames.if is
     *            true,means all the frames will be effected by the
     *            parameters.otherwise will just the time stamp frame will
     *            effect.
     * @hide
     */
    public abstract void setFrameParameters(boolean isInput, int index,
            BaseParameters baseParameters, long timestamp, boolean repeating);

    /**
     * between camera service and Effect Hal or between APP framework and Effect
     * Hal whether need sync the surface and parameters.
     * @param isInput
     *            where the synchronize mode will set.if is true,means the
     *            parameters on the surface need to sync between Camera service
     *            and Effect Hal;false means the parameters and surface will be
     *            effect the output frame.
     * @param index
     *            the index of which surface need this setting.
     * @param sync
     *            whether need synchronize the parameters on the surface.true
     *            means need sync;otherwise will asynchronize
     * @return if the value is not negative,means set success.
     * @hide
     */
    public abstract int setFrameSyncMode(boolean isInput, int index, boolean sync);

    /**
     * get the current sync mode between camera service and Effect Hal or
     * between APP framework and Effect Hal.
     * @param isInputSync
     *            if isInputSync is true,means current get the sync mode status
     *            between camera service and Effect Hal;otherwise will get the
     *            sync mode status between APP framework and Effect Hal.
     * @param index
     *            the index of which surface need this setting.
     * @return true means current the between surface and parameters is in
     *         synchronize mode.otherwise is in asynchronize mode.
     * @hide
     */
    public abstract boolean getFrameSyncMode(boolean isInputSync, int index);

    /**
     * stop the current capture.
     * @param baseParameters
     *            the parameters which need to set the Effect Hal,such as
     *            whether need merge the pictures if you want merge all the
     *            pictures in one; if you do noting ,the baseParameters you can
     *            set to null.
     * @hide
     */
    public abstract void stopCapture(BaseParameters baseParameters);

    /**
     * close the current session if you leave this session or the output surface
     * is changed.
     * @hide
     */
    public abstract void closeSession();

    /**
     * A callback for receiving updates about the state of a camera effect on
     * this session.
     * @hide
     */
    public static abstract class SessionStateCallback {

        /**
         * if have call Effect Hal Client to prepare, when have prepared
         * done,the onPrepared() will be fired on.
         * @param session
         *            the onPrepared() on which session is take effect.
         * @hide
         */
        public abstract void onPrepared(CameraEffectSession session);

        /**
         * if have call Effect Hal Client to configure, when have configure
         * success, this will be fired on.
         * @param session
         *            the onConfigured() on which session is take effect.
         * @hide
         */
        public abstract void onConfigured(CameraEffectSession session);

        /**
         * if have call Effect Hal Client to configure, when have configured
         * fail, this will be fired on.
         * @param session
         *            the onConfigureFailed() on which session is take effect.
         * @hide
         */
        public abstract void onConfigureFailed(CameraEffectSession session);

        /**
         * if have call Effect Hal Client to abort, when have abort done, this
         * will be fired on.
         * @param session
         *            the onClosed() on which session is take effect.
         * @hide
         */
        public abstract void onClosed(CameraEffectSession session);
    }

    /**
     * A callback object for tracking the progress of a CaptureRequest submitted
     * to the Effect Hal Client. This callback is invoked when a request
     * triggers a capture to start, and when the capture is complete. In case on
     * an error capturing an image, the onCaptureFailed method is triggered
     * instead of the completion method.
     * @hide
     */
    public static abstract class CaptureCallback {

        /**
         * when Effect Hal is processing the input buffer,this function will be
         * on fired.
         * @param session
         *            which sesssion's buffer is processing.
         * @param parameter
         *            which parameters is effect on current processing buffer.
         * @param partialResult
         *            which actual parameters is effect on this input buffer.
         * @hide
         */
        public abstract void onInputFrameProcessed(CameraEffectSession session,
                BaseParameters parameter, BaseParameters partialResult);

        /**
         * when Effect Hal is processing the output buffer,this function will be
         * on fired. means the buffer will be pass to Framework.
         * @param session
         *            which sesssion's buffer is processing.
         * @param parameter
         *            which parameters is effect on current processing buffer.
         * @param partialResult
         *            which actual parameters is effect on this input buffer.
         * @hide
         */
        public abstract void onOutputFrameProcessed(CameraEffectSession session,
                BaseParameters parameter, BaseParameters partialResult);

        /**
         * when current effect session have been completed,will be notified.
         * @param session
         *            which sesssion's buffer is processing.
         * @param result
         *            which actual parameters is effect on this input buffer.
         * @param uid
         *            which session is completed,the id is match with before
         *            started id.
         * @hide
         */
        public abstract void onCaptureSequenceCompleted(CameraEffectSession session,
                BaseParameters result, long uid);

        /**
         * when user want stop current capture session,need to call abort()
         * function.when native have finished the action, this function will be
         * on fired.
         * @param session
         *            which sesssion's buffer is processing.
         * @param result
         *            which actual parameters is effect on this input buffer.
         * @hide
         */
        public abstract void onCaptureSequenceAborted(CameraEffectSession session,
                BaseParameters result);

        /**
         * when current session capture is failed,this function will be notify.
         * @param session
         *            which sesssion's buffer is processing.
         * @param result
         *            which actual parameters is effect on this input buffer.
         * @hide
         */
        public abstract void onCaptureFailed(CameraEffectSession session, BaseParameters result);

    }

    /**
     * Closes the object and release any system resources it holds.
     */
    @Override
    public abstract void close();
}
