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

import java.util.ArrayList;

public class FPSCounter {

    private final ArrayList<Long> mTimestamps = new ArrayList<Long>();

    // private long mFPSmeasureTimestamp;

    private double mFps;

    public void nextFrame() {
        nextFrame(System.currentTimeMillis());
    }

    public void nextFrame(long now) {
        mTimestamps.add(now);
        while (mTimestamps.size() > 2 && mTimestamps.get(0) < now - 1000) {
            mTimestamps.remove(0);
        }

        // if (now - mFPSmeasureTimestamp >= 100 && mTimestamps.size() ==
        // 10) {
        // mFPSmeasureTimestamp = now;
        double fps = mTimestamps.get(mTimestamps.size() - 1) - mTimestamps.get(0);
        fps /= mTimestamps.size() - 1;
        fps = 1000 / fps;
        mFps = fps;
        // }
    }

    public double getFPS() {
        return mFps;
    }

}
