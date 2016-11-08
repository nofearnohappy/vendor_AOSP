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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.mediatek.blemanager.R;
import com.mediatek.blemanager.common.CachedBleDevice;
import com.mediatek.blemanager.provider.BleConstants;

import java.io.InputStream;

public class ComposeRangeView extends RelativeLayout {
    private static final String TAG = BleConstants.COMMON_TAG + "[ComposeRangeView]";
    
    private Context mContext;
    private ImageView mRangeBackgroundImageView;
    private ImageView mDistanceFarImageView;
    private ImageView mDistanceMiddleImageView;
    private ImageView mDistanceNearImageView;
    private ImageView mRangeArrowImageView;

    private Bitmap mBackgroundBitmap;
    private Bitmap mArrowBitmap;
    private Bitmap mDistanceBitmap;

    private int mRange;
    private boolean mIsOutRangeChecked;
    private boolean mIsEnabled;

    public ComposeRangeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        LayoutInflater.from(context).inflate(R.layout.compose_distance_image_layout, this, true);
    }

    public ComposeRangeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    public ComposeRangeView(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Log.d(TAG, "[onFinishInflate]...");
        mRangeBackgroundImageView = (ImageView) this.findViewById(R.id.range_bg_image);
        mDistanceFarImageView = (ImageView) this.findViewById(R.id.distance_far_image);
        mDistanceMiddleImageView = (ImageView) this.findViewById(R.id.distance_middle_image);
        mDistanceNearImageView = (ImageView) this.findViewById(R.id.distance_near_image);
        mRangeArrowImageView = (ImageView) this.findViewById(R.id.range_indicator_image);
        updateViewState();
    }

    public void setState(boolean enabled, int range, boolean outRangeChecked) {
        Log.d(TAG, "[setState]enabled = " + enabled + ",range =" + range + ", outRangeChecked = "
                + outRangeChecked);
        mRange = range;
        mIsOutRangeChecked = outRangeChecked;
        mIsEnabled = enabled;
        updateViewState();
    }

    public void clearBitmap() {
        Log.d(TAG, "[clearBitmap]...");
        mRangeBackgroundImageView.setVisibility(View.GONE);
        mDistanceFarImageView.setVisibility(View.GONE);
        mDistanceMiddleImageView.setVisibility(View.GONE);
        mDistanceNearImageView.setVisibility(View.GONE);
        mRangeArrowImageView.setVisibility(View.GONE);

        mBackgroundBitmap.recycle();
        mArrowBitmap.recycle();
        mDistanceBitmap.recycle();
    }

    private Bitmap readBitMap(Context context, int resId) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPurgeable = true;
        opt.inInputShareable = true;
        InputStream is = context.getResources().openRawResource(resId);
        return BitmapFactory.decodeStream(is, null, opt);
    }

    private void updateViewState() {
        if (mRangeBackgroundImageView != null && mRangeArrowImageView != null) {
            Bitmap backgroundBitmap;
            Bitmap arrowBitmap;
            if (mIsOutRangeChecked) {
                mRangeBackgroundImageView.setImageResource(R.drawable.out_range_bg);
                backgroundBitmap = readBitMap(mContext, R.drawable.out_range_bg);
                arrowBitmap = readBitMap(mContext, R.drawable.ic_range_arrow_r);
            } else {
                backgroundBitmap = readBitMap(mContext, R.drawable.in_range_bg);
                arrowBitmap = readBitMap(mContext, R.drawable.ic_range_arrow_l);
            }
            // set image to view
            if (backgroundBitmap != null) {
                mRangeBackgroundImageView.setImageBitmap(backgroundBitmap);
                if (mBackgroundBitmap != null) {
                    mBackgroundBitmap.recycle();
                }
                mBackgroundBitmap = backgroundBitmap;
            }
            if (arrowBitmap != null) {
                mRangeArrowImageView.setImageBitmap(arrowBitmap);
                if (mArrowBitmap != null) {
                    mArrowBitmap.recycle();
                }
                mArrowBitmap = arrowBitmap;
            }
        }
        updateDistanceImage();
    }

    private void updateDistanceImage() {
        ImageView imageDis = null;
        Bitmap distanceBitmap = null;
        if (mDistanceFarImageView != null && mDistanceMiddleImageView != null && mDistanceNearImageView != null) {
            if (mRange == CachedBleDevice.PXP_RANGE_NEAR_VALUE) {
                mDistanceFarImageView.setVisibility(View.GONE);
                mDistanceMiddleImageView.setVisibility(View.GONE);
                mDistanceNearImageView.setVisibility(View.VISIBLE);
                if (mIsOutRangeChecked) {
                    distanceBitmap = readBitMap(mContext, R.drawable.out_range_near);
                } else {
                    distanceBitmap = readBitMap(mContext, R.drawable.in_range_near);
                }
                imageDis = mDistanceNearImageView;
            } else if (mRange == CachedBleDevice.PXP_RANGE_MIDDLE_VALUE) {
                mDistanceFarImageView.setVisibility(View.GONE);
                mDistanceMiddleImageView.setVisibility(View.VISIBLE);
                mDistanceNearImageView.setVisibility(View.GONE);
                if (mIsOutRangeChecked) {
                    distanceBitmap = readBitMap(mContext, R.drawable.out_range_middle);
                } else {
                    distanceBitmap = readBitMap(mContext, R.drawable.in_range_middle);
                }
                imageDis = mDistanceMiddleImageView;
            } else if (mRange == CachedBleDevice.PXP_RANGE_FAR_VALUE) {
                mDistanceFarImageView.setVisibility(View.VISIBLE);
                mDistanceMiddleImageView.setVisibility(View.GONE);
                mDistanceNearImageView.setVisibility(View.GONE);
                if (mIsOutRangeChecked) {
                    distanceBitmap = readBitMap(mContext, R.drawable.out_range_far);
                } else {
                    distanceBitmap = readBitMap(mContext, R.drawable.in_range_far);
                }
                imageDis = mDistanceFarImageView;
            }
            if (imageDis != null) {
                // set image to view
                if (distanceBitmap != null) {
                    imageDis.setImageBitmap(distanceBitmap);
                    if (mDistanceBitmap != null) {
                        mDistanceBitmap.recycle();
                    }
                    mDistanceBitmap = distanceBitmap;
                }
                if (mIsEnabled) {
                    imageDis.setImageAlpha(255);
                } else {
                    imageDis.setImageAlpha(125);
                }
            }
        }
        if (mRangeBackgroundImageView != null && mRangeArrowImageView != null) {
            if (mIsEnabled) {
                mRangeBackgroundImageView.setImageAlpha(255);
                mRangeArrowImageView.setImageAlpha(255);
            } else {
                mRangeBackgroundImageView.setImageAlpha(125);
                mRangeArrowImageView.setImageAlpha(125);
            }
        }
    }
}
