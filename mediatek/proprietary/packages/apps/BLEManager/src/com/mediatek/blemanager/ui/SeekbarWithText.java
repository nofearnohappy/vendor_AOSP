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
package com.mediatek.blemanager.ui;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.mediatek.blemanager.R;
import com.mediatek.blemanager.common.CachedBleDevice;
import com.mediatek.blemanager.provider.BleConstants;

public class SeekbarWithText extends RelativeLayout {
    private static final String TAG = BleConstants.COMMON_TAG + "[SeekbarWithText]";

    private static final int CUSTOM_COLOR = Color.rgb(0, 153, 204);

    private SeekBar mDistanceSeekBar;
    private ImageView mNearDotImageView;
    private ImageView mMiddleDotImageView;
    private ImageView mFarDotImageView;

    private TextView mNearTopicTextView;
    private TextView mMiddleTopicTextView;
    private TextView mFarTopicTextView;

    private TextView mNearSumTextView;
    private TextView mMiddleSumTextView;
    private TextView mFarSumTextView;

    public SeekbarWithText(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.distance_seekbar_with_text, this, true);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mDistanceSeekBar = (SeekBar) this.findViewById(R.id.distance_seek_bar);
        this.mNearDotImageView = (ImageView) this.findViewById(R.id.near_dot_image);
        this.mMiddleDotImageView = (ImageView) this.findViewById(R.id.middle_dot_image);
        this.mFarDotImageView = (ImageView) this.findViewById(R.id.far_dot_image);

        this.mNearTopicTextView = (TextView) this.findViewById(R.id.distance_topic_near_text);
        this.mMiddleTopicTextView = (TextView) this.findViewById(R.id.distance_topic_middle_text);
        this.mFarTopicTextView = (TextView) this.findViewById(R.id.distance_topic_far_text);

        this.mNearSumTextView = (TextView) this.findViewById(R.id.distance_sum_near_text);
        this.mMiddleSumTextView = (TextView) this.findViewById(R.id.distance_sum_middle_text);
        this.mFarSumTextView = (TextView) this.findViewById(R.id.distance_sum_far_text);
    }

    /**
     * 
     * @param progress
     *            0(near),1(middle),2(far)
     */
    public void setProgress(int progress) {
        int pro;
        if (progress == CachedBleDevice.PXP_RANGE_NEAR_VALUE) {
            pro = 0;
        } else if (progress == CachedBleDevice.PXP_RANGE_FAR_VALUE) {
            pro = 40;
        } else {
            pro = 20;
        }
        mDistanceSeekBar.setProgress(pro);
        updateDotImages(progress);
    }

    public void setOnSeekBarChangedListener(SeekBar.OnSeekBarChangeListener listener) {
        if (listener == null) {
            Log.w(TAG, "[setOnSeekBarChangedListener] listener is null,return!");
            return;
        }
        mDistanceSeekBar.setOnSeekBarChangeListener(listener);
    }

    private void updateDotImages(int pro) {
        Log.d(TAG, "[updateDotImages]pro = " + pro);
        if (pro == CachedBleDevice.PXP_RANGE_NEAR_VALUE) {
            mNearDotImageView.setVisibility(View.INVISIBLE);
            mMiddleDotImageView.setVisibility(View.VISIBLE);
            mFarDotImageView.setVisibility(View.VISIBLE);

            mNearTopicTextView.setTextColor(CUSTOM_COLOR);
            mMiddleTopicTextView.setTextColor(Color.BLACK);
            mFarTopicTextView.setTextColor(Color.BLACK);

            mNearSumTextView.setTextColor(CUSTOM_COLOR);
            mMiddleSumTextView.setTextColor(Color.BLACK);
            mFarSumTextView.setTextColor(Color.BLACK);
        } else if (pro == CachedBleDevice.PXP_RANGE_MIDDLE_VALUE) {
            mNearDotImageView.setVisibility(View.VISIBLE);
            mMiddleDotImageView.setVisibility(View.INVISIBLE);
            mFarDotImageView.setVisibility(View.VISIBLE);

            mNearTopicTextView.setTextColor(Color.BLACK);
            mMiddleTopicTextView.setTextColor(CUSTOM_COLOR);
            mFarTopicTextView.setTextColor(Color.BLACK);

            mNearSumTextView.setTextColor(Color.BLACK);
            mMiddleSumTextView.setTextColor(CUSTOM_COLOR);
            mFarSumTextView.setTextColor(Color.BLACK);
        } else if (pro == CachedBleDevice.PXP_RANGE_FAR_VALUE) {
            mNearDotImageView.setVisibility(View.VISIBLE);
            mMiddleDotImageView.setVisibility(View.VISIBLE);
            mFarDotImageView.setVisibility(View.INVISIBLE);

            mNearTopicTextView.setTextColor(Color.BLACK);
            mMiddleTopicTextView.setTextColor(Color.BLACK);
            mFarTopicTextView.setTextColor(CUSTOM_COLOR);

            mNearSumTextView.setTextColor(Color.BLACK);
            mMiddleSumTextView.setTextColor(Color.BLACK);
            mFarSumTextView.setTextColor(CUSTOM_COLOR);
        }
    }
}
