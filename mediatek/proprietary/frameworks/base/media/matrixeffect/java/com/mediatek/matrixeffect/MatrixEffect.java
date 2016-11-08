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

package com.mediatek.matrixeffect;

import android.view.Surface;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;

public class MatrixEffect {
    private static final String TAG = "MatrixEffect_Framework";
    private EventHandler mEventHandler;
    private static MatrixEffect sMatrixEffect;
    private EffectsCallback mEffectsListener;
    private final static int MSG_EFFECT_DONE = 100;

    MatrixEffect() {
        Looper looper;
        if ((looper = Looper.myLooper()) != null) {
            mEventHandler = new EventHandler(looper);
        } else {
            mEventHandler = new EventHandler(Looper.getMainLooper());
        }
        native_setup(new WeakReference<MatrixEffect>(this));
    }
    
    /**
     * @internal
     * @return
     */
    public static MatrixEffect getInstance() {
        if (sMatrixEffect == null) {
            sMatrixEffect = new MatrixEffect();
        }
        return sMatrixEffect;
    }
    
    /**
     * @internal
     *
     */
    public interface EffectsCallback {
        public void onEffectsDone();
    }
    
    /**
     * @internal
     * @param listener
     */
    public void setCallback(EffectsCallback listener) {
        mEffectsListener = listener;
    }

    /**
     * @internal
     * @param surface
     * @param surfaceNumber
     */
    public void setSurface(Surface surface, int surfaceNumber) {
        native_setSurfaceToNative(surface, surfaceNumber);
    }
    
    /**
     * @internal
     * @param previewWidth
     * @param previewHeight
     * @param effectNumOfPage
     * @param format
     */
    public void initialize(int previewWidth, int previewHeight,
            int effectNumOfPage, int format) {
        native_initializeEffect(previewWidth, previewHeight, effectNumOfPage, format);
    }

    /**
     * @internal
     * @param bufferWidth
     * @param bufferHeight
     * @param buffers
     */
    public void setBuffers(int bufferWidth, int bufferHeight, byte[][] buffers) {
        native_registerEffectBuffers(bufferWidth, bufferHeight, buffers);
    }
    
    /**
     * @internal
     * @param previewData
     * @param effectId
     */
    public void process(byte[] previewData, int[] effectId) {
        native_processEffect(previewData, effectId);
    }
    
    /**
     * @internal
     */
    public void release() {
        native_releaseEffect();
    }

    private class EventHandler extends Handler {

        public EventHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "handleMessage:" + msg);
            switch(msg.what) {
            case MSG_EFFECT_DONE:
                if (mEffectsListener != null) {
                    mEffectsListener.onEffectsDone();
                }
                break;
            }
        }
    }

    private static void postEventFromNative(Object matrixeffect_pref, int what) {
        MatrixEffect le = (MatrixEffect) ((WeakReference) matrixeffect_pref).get();
        le.mEventHandler.obtainMessage(what).sendToTarget();
    }

    static {
        System.loadLibrary("jni_lomoeffect");
    }

    private native void native_setup(Object weak_this);
    private native void native_setSurfaceToNative(Surface surface, int surfaceNumber);
    private native void native_displayEffect(byte[] effectData, int surfaceNumber);
    private native void native_initializeEffect(int previewWidth, int previewHeight,
            int effectNumOfPage, int format);
    private native void native_registerEffectBuffers(int bufferWidth, int bufferHeight, byte[][] buffers);
    private native void native_processEffect(byte[] previewData, int[] effectIndex);
    private native void native_releaseEffect();
}

