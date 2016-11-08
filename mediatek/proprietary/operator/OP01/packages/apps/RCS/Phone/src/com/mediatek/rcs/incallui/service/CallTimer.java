/*
 * Copyright (C) 2013 The Android Open Source Project
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
 * limitations under the License
 */

package com.mediatek.rcs.incallui.service;

import android.os.Handler;
import android.os.SystemClock;

import com.google.common.base.Preconditions;

public class CallTimer extends Handler {
    private Runnable mInternalCallback;
    private Runnable mCallback;
    private long mLastReportedTime;
    private long mInterval;
    private boolean mRunning;

    public CallTimer(Runnable callback) {
        Preconditions.checkNotNull(callback);

        mInterval = 0;
        mLastReportedTime = 0;
        mRunning = false;
        mCallback = callback;
        mInternalCallback = new CallTimerCallback();
    }

    public boolean startTimeTick(long interval) {
        if (interval <= 0) {
            return false;
        }

        // cancel any previous timer
        stopTimeTick();

        mInterval = interval;
        mLastReportedTime = SystemClock.uptimeMillis();

        mRunning = true;
        periodicUpdateTimer();

        return true;
    }

    public void stopTimeTick() {
        removeCallbacks(mInternalCallback);
        mRunning = false;
    }

    private void periodicUpdateTimer() {
        if (!mRunning) {
            return;
        }

        final long now = SystemClock.uptimeMillis();
        long nextReport = mLastReportedTime + mInterval;
        while (now >= nextReport) {
            nextReport += mInterval;
        }

        postAtTime(mInternalCallback, nextReport);
        mLastReportedTime = nextReport;

        // Run the callback
        mCallback.run();
    }

    private class CallTimerCallback implements Runnable {
        @Override
        public void run() {
            periodicUpdateTimer();
        }
    }
}
