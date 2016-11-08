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
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.SeekBar;

import com.mediatek.blemanager.R;
import com.mediatek.blemanager.common.CachedBleDevice;
import com.mediatek.blemanager.provider.BleConstants;

public class RangeComposePreference extends Preference {
    private static final String TAG = BleConstants.COMMON_TAG + "[RangeComposePreference]";

    private static final int OUT_OF_RANGE_CLICK_FLAG = 1;
    private static final int IN_RANGE_CLICK_FLAG = 2;

    private int mSeekBarStopProgress;
    private int mSeekBarStartProgress;

    private boolean mIsOutOfRangeChecked;
    private boolean mIsEnabled;

    private SeekbarWithText mRangeDistanceSeekBar;
    private View mOutOfRangeAlertLayoutView;
    private View mInRangeAlertLayoutView;
    private RadioButton mOutOfRangeRadioBtn;
    private RadioButton mInRangeRadioBtn;
    private ComposeRangeView mComposeRangeView;

    private ComposePreferenceChangedListener mComposePreferenceChangedListener;

    public RangeComposePreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RangeComposePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.setLayoutResource(R.layout.range_distance_preference_layout);
    }

    public RangeComposePreference(Context context) {
        this(context, null);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mRangeDistanceSeekBar = (SeekbarWithText) view.findViewById(R.id.range_distance_seek_bar);
        mOutOfRangeAlertLayoutView = view.findViewById(R.id.out_range_alert_linear);
        mInRangeAlertLayoutView = view.findViewById(R.id.in_range_alert_linear);
        mOutOfRangeRadioBtn = (RadioButton) view.findViewById(R.id.out_range_alert_radio);
        mInRangeRadioBtn = (RadioButton) view.findViewById(R.id.in_range_alert_radio);
        mComposeRangeView = (ComposeRangeView) view.findViewById(R.id.compose_range_view);

        initView();
    }

    public void setState(boolean enabled, int rangeValue, int inOutAlert) {
        mSeekBarStopProgress = rangeValue;
        if (inOutAlert == 1) {
            mIsOutOfRangeChecked = true;
        } else {
            mIsOutOfRangeChecked = false;
        }
        mIsEnabled = enabled;
        updateViewState();
    }

    public void setChangeListener(ComposePreferenceChangedListener listener) {
        if (listener == null) {
            Log.d(TAG, "[setChangeListener] listener is null!!");
            return;
        }
        mComposePreferenceChangedListener = listener;
    }

    public interface ComposePreferenceChangedListener {
        void onSeekBarProgressChanged(int startPorgress, int stopProgress);

        void onRangeChanged(boolean outRangeChecked);
    }

    public void clear() {
        if (mComposeRangeView != null) {
            mComposeRangeView.clearBitmap();
        }
    }

    private void initView() {
        mOutOfRangeAlertLayoutView.setOnClickListener(new RadioLinearClickListener(
                OUT_OF_RANGE_CLICK_FLAG));
        mOutOfRangeRadioBtn
                .setOnClickListener(new RadioLinearClickListener(OUT_OF_RANGE_CLICK_FLAG));
        mInRangeAlertLayoutView
                .setOnClickListener(new RadioLinearClickListener(IN_RANGE_CLICK_FLAG));
        mInRangeRadioBtn.setOnClickListener(new RadioLinearClickListener(IN_RANGE_CLICK_FLAG));

        mRangeDistanceSeekBar.setOnSeekBarChangedListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int dis = seekBar.getProgress();
                if (dis >= 0 && dis < 10) {
                    mSeekBarStopProgress = CachedBleDevice.PXP_RANGE_NEAR_VALUE;
                } else if (dis >= 10 && dis < 30) {
                    mSeekBarStopProgress = CachedBleDevice.PXP_RANGE_MIDDLE_VALUE;
                } else if (dis >= 30 && dis <= 40) {
                    mSeekBarStopProgress = CachedBleDevice.PXP_RANGE_FAR_VALUE;
                }
                mComposePreferenceChangedListener.onSeekBarProgressChanged(mSeekBarStartProgress,
                        mSeekBarStopProgress);
                updateViewState();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mSeekBarStartProgress = seekBar.getProgress();
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }
        });
        updateViewState();
    }

    private void updateViewState() {
        if (mOutOfRangeRadioBtn != null && mInRangeRadioBtn != null) {
            if (mIsOutOfRangeChecked) {
                mOutOfRangeRadioBtn.setChecked(true);
                mInRangeRadioBtn.setChecked(false);
            } else {
                mOutOfRangeRadioBtn.setChecked(false);
                mInRangeRadioBtn.setChecked(true);
            }
        }
        if (mRangeDistanceSeekBar != null) {
            mRangeDistanceSeekBar.setProgress(mSeekBarStopProgress);
        }
        if (mComposeRangeView != null) {
            mComposeRangeView.setState(mIsEnabled, mSeekBarStopProgress, mIsOutOfRangeChecked);
        }
    }

    private class RadioLinearClickListener implements View.OnClickListener {

        private int mWhich;

        public RadioLinearClickListener(int which) {
            mWhich = which;
        }

        @Override
        public void onClick(View v) {
            switch (mWhich) {
            case OUT_OF_RANGE_CLICK_FLAG:
                if (!mIsOutOfRangeChecked) {
                    mIsOutOfRangeChecked = true;
                }
                break;

            case IN_RANGE_CLICK_FLAG:
                if (mIsOutOfRangeChecked) {
                    mIsOutOfRangeChecked = false;
                }
                break;
            default:
                break;
            }
            updateViewState();
            mComposePreferenceChangedListener.onRangeChanged(mIsOutOfRangeChecked);
        }
    }
}
