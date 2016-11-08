/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2011-2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

package com.dolby.ds1appUI;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;

import com.dolby.ds1appCoreUI.Constants;
import com.dolby.ds1appCoreUI.DS1Application;

public class GraphicVisualiserPainter {

    public static final int ROWS = 48;

    public static final int ROWS_RED = 12;

    public static final int ROWS_YELLOW = 6;

    private final BitmapDrawable mBg;

    private final BitmapDrawable mBarEmpty;

    private final BitmapDrawable mBarBlue;

    private final BitmapDrawable mBarBlueLight;

    private final BitmapDrawable mBarYellow;

    private final BitmapDrawable mBarRed;

    private Bitmap mScaledBg;

    private Bitmap mScaledBarEmpty;

    private Bitmap mScaledBarBlue;

    private Bitmap mScaledBarBlueLight;

    private Bitmap mScaledBarYellow;

    private Bitmap mScaledBarRed;

    private final float[] mExcitations = new float[Constants.BANDS];

    private final float[] mGains;

    private final float[] mGainsUser;

    private final Context mContext;

    private int mWidth;

    private int mHeight;

    private boolean mEnabled = true;

    private final Object barLock = new Object();

    private final GraphicVisualiser mVisualizer;

    public GraphicVisualiserPainter(Context context,
            GraphicVisualiser visualizer, float[] gainsUi, float[] gainsUser) {
        this.mContext = context;
        this.mVisualizer = visualizer;
        this.mGains = gainsUi;
        this.mGainsUser = gainsUser;

        final Resources res = mContext.getResources();
        mBg = (BitmapDrawable) res.getDrawable(R.drawable.eq_background);
        mBarEmpty = (BitmapDrawable) res.getDrawable(R.drawable.mock_gv_brick);
        mBarBlue = (BitmapDrawable) res.getDrawable(R.drawable.mock_gv_brick_blue);
        mBarYellow = (BitmapDrawable) res.getDrawable(R.drawable.mock_gv_brick_yellow);
        mBarRed = (BitmapDrawable) res.getDrawable(R.drawable.mock_gv_brick_red);
        mBarBlueLight = (BitmapDrawable) res.getDrawable(R.drawable.mock_gv_brick_blue_light);
    }

    public void onDraw(final Canvas canvas) {
        if (canvas == null) {
            return;
        }
        synchronized (barLock) {
            final boolean suspended = mVisualizer.mSuspended;
            final float[] gainsPaint = suspended ? mGainsUser : mGains;
            if (mScaledBg != null && !mScaledBg.isRecycled()) {
                canvas.drawBitmap(mScaledBg, 0, 0, null);
            }

            if (mScaledBarBlueLight == null) {
                return;
            }

            final boolean enabled = mEnabled;
            final int barHeight = mScaledBarBlueLight.getHeight();
            final int h = mHeight - barHeight;
            Bitmap bmp;
            Paint line_paint = new Paint();
            line_paint.setColor(Color.BLACK);

            final int bar_width = (mWidth - 1) / Constants.BANDS;
            final int bar_height = (mHeight - 1) / ROWS;
            for (int c = 0; c < Constants.BANDS; c++) {
                final int x = (bar_width * c) + 1;
                canvas.drawLine(x - 1, 0, x - 1, mHeight, line_paint);
                final int excitation = (int) (convertValue(mExcitations[c], (ROWS - 1)) + 0.5f);
                for (int r = 0; r < ROWS; r++) {
                    final int y = (bar_height * r) + 1;
                    if (c == 0)
                        canvas.drawLine(0, y - 1, mWidth, y - 1, line_paint);
                    //DS1SOC-566, In LPA mode, no visualizer data and no need draw the excitation bars
                    if (DS1Application.VISUALIZER_ENABLE) {
	                    if (enabled && !suspended) {
	                        if ((excitation < (ROWS - 1 - r)) || (excitation > (ROWS - 1))) {
	                            bmp = mScaledBarEmpty;
	                        } else if (r < ROWS_RED) {
	                            bmp = mScaledBarRed;
	                        } else if (r < ROWS_RED + ROWS_YELLOW) {
	                            bmp = mScaledBarYellow;
	                        } else {
	                            bmp = mScaledBarBlue;
	                        }
	                    } else {
	                        bmp = mScaledBarEmpty;
	                    }
	                    if (bmp != null && !bmp.isRecycled()) {
	                        canvas.drawBitmap(bmp, x, y, null);
	                    }
                    }
                }
                if (enabled && DS1Application.VISUALIZER_ENABLE) {
                    final int gainY = (int) (convertValue(gainsPaint[c], h) + barHeight / 2);
                    canvas.drawBitmap(mScaledBarBlueLight, Math.round(x), mHeight - barHeight - gainY, null);
                }
            }
        }
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        synchronized (barLock) {

            final int barw = ((w - 1) / Constants.BANDS) - 1;
            final int barh = ((h - 1) / ROWS) - 1;

            if (barw > 0 && barh > 0) {

                this.mWidth = w;
                this.mHeight = h;

                if (mScaledBg != null) {
                    mScaledBg.recycle();
                    mScaledBg = null;
                }
                if (mScaledBarEmpty != null) {
                    mScaledBarEmpty.recycle();
                    mScaledBarEmpty = null;
                }
                if (mScaledBarBlue != null) {
                    mScaledBarBlue.recycle();
                    mScaledBarBlue = null;
                }
                if (mScaledBarYellow != null) {
                    mScaledBarYellow.recycle();
                    mScaledBarYellow = null;
                }
                if (mScaledBarRed != null) {
                    mScaledBarRed.recycle();
                    mScaledBarRed = null;
                }
                if (mScaledBarBlueLight != null) {
                    mScaledBarBlueLight.recycle();
                    mScaledBarBlueLight = null;
                }

                mScaledBg = Bitmap.createScaledBitmap(mBg.getBitmap(), this.mWidth, h, true);
                mScaledBarEmpty = Bitmap.createScaledBitmap(mBarEmpty.getBitmap(), barw, barh, true);
                mScaledBarBlue = Bitmap.createScaledBitmap(mBarBlue.getBitmap(), barw, barh, true);
                mScaledBarYellow = Bitmap.createScaledBitmap(mBarYellow.getBitmap(), barw, barh, true);
                mScaledBarRed = Bitmap.createScaledBitmap(mBarRed.getBitmap(), barw, barh, true);
                mScaledBarBlueLight = Bitmap.createScaledBitmap(mBarBlueLight.getBitmap(), barw, barh, true);
            }
        }
    }

    private static float convertValue(float dB, float height) {
        float abs = dB - Constants.MIN_VISIBLE_GAIN;
        float v = (abs * height) / Constants.VISIBLE_GAIN_SPAN;
        return (int) v;
    }

    /**
     * Set current excitations values. Thread-safe.
     * 
     * @param excitations
     *            updated excitations
     */
    public void setExcitations(final float[] excitations) {
        System.arraycopy(excitations, 0, this.mExcitations, 0, this.mExcitations.length);
    }

    public void setEnabled(boolean enabled) {
        this.mEnabled = enabled;
    }

}
