/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.mms.plugin;

import com.mediatek.mms.callback.ITextSizeAdjustHost;

import android.app.Activity;
import android.content.Context;
import android.view.MotionEvent;

/**
 * Op01MmsTextSizeAdjust.
 *
 */
public class Op01MmsTextSizeAdjust {

    private static final String TAG = "Mms/Op01MmsTextSizeAdjustExt";
    private Context mHostContext;
    private ScaleDetector mScaleDetector;
    private float mTextsize = 18;
    private ScaleDetector.SimpleOnScaleListener mListener;
    private ITextSizeAdjustHost mHost = null;

    /**
     * Construction.
     */
    public Op01MmsTextSizeAdjust() {
        mListener = new Op01OnScaleListener();
        mScaleDetector = new ScaleDetector(mListener);
    }

    /**
     *
     * @param host     the reference to ITextSizeAdjustHost
     * @param activity  the reference to the current activity
     */
    public void init(ITextSizeAdjustHost host, Activity activity) {
        mHost = host;
        mScaleDetector.setActivity(activity);
        setHostContext(activity);
    }

    /**
     * getHost.
     * @return ITextSizeAdjustHost.
     */
    public ITextSizeAdjustHost getHost() {
        return mHost;
    }

    /**
     * setHostContext.
     * @param context Context
     */
    public void setHostContext(Context context) {
        mHostContext = context;
        if (mHostContext != null) {
            mTextsize = Op01MmsUtils.getTextSize(mHostContext);
            mListener.setContext(mHostContext);
        }
        mListener.setTextSize(mTextsize);
    }

    /**
     * multiTouch used to change textsize.
     * @param event MotionEvent
     * @return boolean: if consume the event,return true
     */
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (mScaleDetector != null) {
            return mScaleDetector.onTouchEvent(event);
        }
        return false;
    }

    /**
     * refresh.
     */
    public void refresh() {
        //get the lasted font size and set.
        if (mHostContext != null) {
            float size = Op01MmsUtils.getTextSize(mHostContext);
            if (mTextsize != size) {
                mTextsize = size;
                mListener.setTextSize(mTextsize);
                getHost().setTextSize(mTextsize);
            }
        }
    }

    /**
     * Op01OnScaleListener.
     *
     */
    public class Op01OnScaleListener extends ScaleDetector.SimpleOnScaleListener {

        /**
         * Construction.
         */
        public Op01OnScaleListener() {
            super();
        }

        /**
         * Construction.
         * @param context Context
         * @param initTextSize init text size
         */
        public Op01OnScaleListener(Context context, float initTextSize) {
            super(context, initTextSize);
        }

        @Override
        protected void performChangeText(float size) {
            if (mTextsize != size) {
                mTextsize = size;
                ITextSizeAdjustHost host = getHost();
                if (host != null) {
                    host.setTextSize(size);
                }
            }
        }
    }

}

