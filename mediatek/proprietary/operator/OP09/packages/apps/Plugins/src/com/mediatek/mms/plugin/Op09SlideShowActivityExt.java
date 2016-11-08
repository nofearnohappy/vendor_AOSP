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

import android.content.Context;
import android.view.MotionEvent;

import com.mediatek.common.PluginImpl;
import com.mediatek.mms.ext.DefaultOpSlideshowActivityExt;
import android.util.Log;

/**
 * M: Op09SlideShowActivityExt.
 *
 */
public class Op09SlideShowActivityExt extends DefaultOpSlideshowActivityExt {
    private static final String TAG = "Mms/OP09MmsSlideShowExt";
    private MotionEvent mStartMotionEvent;
    private MotionEvent mEndMotionEvent;

    private static final int FLING_LENGTH = 100;

    float mStartX = 0;
    float mStartY = 0;
    float mEndX = 0;
    float mEndY = 0;

    /**
     * The Constructor.
     * @param context the Context.
     */
    public Op09SlideShowActivityExt(Context context) {

    }

    @Override
    public Direction dispatchTouchEvent(MotionEvent e) {
        if (!MessageUtils.enableTurnPageWithFlingScreen()) {
            return Direction.NO_ACTION;
        }

        Log.d("@M_" + TAG, "setMotionEventAndParse ext");
        if (e == null) {
            return Direction.NO_ACTION;
        }
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            this.mStartMotionEvent = e;
            mStartX = mStartMotionEvent.getX();
            mStartY = mStartMotionEvent.getY();
        } else if (e.getAction() == MotionEvent.ACTION_UP) {
            this.mEndMotionEvent = e;

            mEndX = mEndMotionEvent.getX();
            mEndY = mEndMotionEvent.getY();

            if (mStartX - mEndX > FLING_LENGTH) { // move left
                return Direction.LEFT;
            } else if (mEndX - mStartX > FLING_LENGTH) { // MOVE right
                return Direction.RIGHT;
            } else if (mStartY - mEndY > FLING_LENGTH) { // MOVE up
                return Direction.UP;
            } else if (mEndY - mStartY > FLING_LENGTH) { // MOVE down
                return Direction.DOWN;
            }
        }
        return Direction.NO_ACTION;
    }
}
