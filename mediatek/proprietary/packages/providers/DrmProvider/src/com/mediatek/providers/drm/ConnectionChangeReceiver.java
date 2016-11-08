/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.mediatek.providers.drm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.mediatek.drm.OmaDrmClient;

/**
 * When connection is available, sync secure timer if necessary.
 * Only match below conditions we need sync secure timer:
 * 1. OMA DRM is enabled
 * 2. secure timer is invalid
 * 3. Not in test sim state
 * 4. Network is available
 */
public class ConnectionChangeReceiver extends BroadcastReceiver {
    private static final String TAG = "DRM/ConnectionChangeReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: " + intent);
        // only when OMA DRM enabled, we need sync secure timer
        if (OmaDrmClient.isOmaDrmEnabled()) {
            // 1. Only when secure timer is invalid, we need sync secure timer
            OmaDrmClient client = new OmaDrmClient(context);
            boolean isValid = OmaDrmHelper.checkClock(client);
            client.release();
            client = null;
            if (isValid) {
                Log.d(TAG, "Secure timer is already valid, needn't sync secure timer");
                return;
            }

            // 2. If current is in test state, needn't sync secure timer
            if (OmaDrmHelper.isTestIccCard()) {
                Log.d(TAG, "It is test sim state now, needn't sync secure timer");
                return;
            }

            // 3. Only when network is available, we need sync secure timer
            if (!OmaDrmHelper.isNetWorkAvailable(context)) {
                Log.w(TAG, "Network is not available, needn't sync secure timer");
                return;
            }

            Log.d(TAG, "Start DrmSyncTimeService to sync secure timer");
            context.startService(new Intent(context, DrmSyncTimeService.class).putExtra(
                    "action", "sync_secure_timer"));
        }
    }
}
