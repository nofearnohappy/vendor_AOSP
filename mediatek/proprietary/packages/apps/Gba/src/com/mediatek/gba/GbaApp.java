/*
 * Copyright (C) 2011 The Android Open Source Project
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
 * limitations under the License.
 */

package com.mediatek.gba;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * implementation for GbaApp.
 *
 * @hide
 */
public class GbaApp extends Application {
    private static final String TAG = "GbaApp";

    private Context mContext;

    @Override
    public void onCreate() {
        // Register for telephony intent broadcasts
        Log.d(TAG, "onCreate(): GbaApp ");

        mContext = this.getBaseContext();
        startGbaService();
    }

    private void startGbaService() {
        Log.d(TAG, "starting service for persisten service");

        if (mContext.startService(new Intent(this, GbaService.class)) == null) {
            Log.e(TAG, "Can't start gba service");
        }
    }
}
