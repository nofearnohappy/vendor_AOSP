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

package com.mediatek.widget;


import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.Handler;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RemoteViews.RemoteView;

import java.util.TimeZone;

/**
 * @hide
 */
@RemoteView
public class AnalogClockWidget extends View {

    private final float mDensity;
    private final Paint mPaint = new Paint();
    private static final float STROKE_WIDTH = 4f;
    private static final float STROKE_WIDTH_MS = 3f;
    private static final float HOUR_RADIUS_SCALE = 0.5f;
    private static final float MINUTE_RADIUS_SCALE = 0.8f;
    private static final float OFFSET_RADIUS_SCALE = 0.1f;
    private static final float RADIUS_SCALE = 0.9f;

    private Time mCalendar;
    private float mMinutes;
    private float mHour;

    private boolean mAttached;
    private final Handler mHandler = new Handler();

    public AnalogClockWidget(Context context) {
        this(context, null);
    }

    public AnalogClockWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnalogClockWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mDensity = context.getResources().getDisplayMetrics().density;
        mCalendar = new Time();
        mPaint.setStyle(Style.STROKE);
        mPaint.setStrokeWidth(STROKE_WIDTH * mDensity);
        mPaint.setColor(Color.WHITE);
        mPaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int x = (mRight - mLeft) / 2;
        int y = (mBottom - mTop) / 2;
        float radius = Math.min(x, y) * RADIUS_SCALE;

        mPaint.setStrokeWidth(STROKE_WIDTH * mDensity);
        canvas.drawCircle(x, y, radius, mPaint);

        mPaint.setStrokeWidth(STROKE_WIDTH_MS * mDensity);
        canvas.save();
        canvas.rotate(mHour / 12.0f * 360.0f, x, y);
        canvas.drawLine(x, y + (radius * OFFSET_RADIUS_SCALE),
                x, y - (radius * HOUR_RADIUS_SCALE), mPaint);
        canvas.restore();

        canvas.save();
        canvas.rotate(mMinutes / 60.0f * 360.0f, x, y);
        canvas.drawLine(x, y + (radius * OFFSET_RADIUS_SCALE),
                x, y - (radius * MINUTE_RADIUS_SCALE), mPaint);
        canvas.restore();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            getContext().registerReceiverAsUser(mIntentReceiver,
                    android.os.Process.myUserHandle(), filter, null, mHandler);
        }

        mCalendar = new Time();
        onTimeChanged();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            getContext().unregisterReceiver(mIntentReceiver);
            mAttached = false;
        }
    }

    private void onTimeChanged() {
        mCalendar.setToNow();

        int hour = mCalendar.hour;
        int minute = mCalendar.minute;
        int second = mCalendar.second;

        mMinutes = minute + second / 60.0f;
        mHour = hour + mMinutes / 60.0f;

        updateContentDescription(mCalendar);
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                String tz = intent.getStringExtra("time-zone");
                mCalendar = new Time(TimeZone.getTimeZone(tz).getID());
            }

            onTimeChanged();
            invalidate();
        }
    };

    private void updateContentDescription(Time time) {
        final int flags = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_24HOUR;
        String contentDescription = DateUtils.formatDateTime(mContext,
                time.toMillis(false), flags);
        setContentDescription(contentDescription);
    }
}
