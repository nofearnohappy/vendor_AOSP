/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2011-2012 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

package com.dolby.ds1appUI;

import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ToggleButton;

public class ToggleSlideListener implements OnTouchListener {

    private boolean mDetected;

    private float mStartX;

    private float mMinDist;

    private boolean mRight;

    private float mHalfWidth;

    @Override
    public boolean onTouch(View v, MotionEvent e) {
        final int action = e.getAction();
        if (MotionEvent.ACTION_DOWN == action) {
            mDetected = false;
            mStartX = e.getX();
            mHalfWidth = v.getWidth() / 2;
            mMinDist = v.getWidth() / 4;
            if (v instanceof ToggleButton) {
                // determine required sliding direction
                mRight = !((ToggleButton) v).isChecked();
            }
            if (testXonOtherHalf(mStartX)) {
                onDetected(v);
            }
            return mDetected;
        }

        if (mDetected) {
            return true;
        }

        if (MotionEvent.ACTION_MOVE == action) {
            final float x = e.getX();
            if (mRight) {
                // update starting X coordinate when sliding right
                if (x >= 0 && x < mStartX) {
                    mStartX = x;
                }
            } else {
                // update starting X coordinate when sliding left
                if (x > mStartX && x < v.getWidth()) {
                    mStartX = x;
                }
            }

            if (testXonOtherHalf(x)) {
                // went to other half of the button
                onDetected(v);
            } else if (mRight == (x > mStartX) && Math.abs(x - mStartX) >= mMinDist) {
                // slid over minimum required distance
                onDetected(v);
            }
        }

        return mDetected;
    }

    private void onDetected(View v) {
        mDetected = true;
        v.performClick();
    }

    private boolean testXonOtherHalf(float x) {
        return mRight == (x > mHalfWidth);
    }
}
