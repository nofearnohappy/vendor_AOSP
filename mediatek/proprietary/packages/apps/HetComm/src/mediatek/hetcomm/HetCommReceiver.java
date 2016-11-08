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

package com.mediatek.hetcomm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;


public class HetCommReceiver extends BroadcastReceiver {
    private static final String TAG = "HetCommReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action != null) {
            Log.d(TAG, "action:" + action);
        }

        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || action.equals("android.intent.action.ACTION_BOOT_IPO")) {
            checkHetCommService(context);
        }
    }

    private void checkHetCommService(Context context) {
        boolean isEnabled = (Settings.Global.getInt(context.getContentResolver(),
                    Settings.Global.HETCOMM_ENABLED, 0) == 1 ? true : false);

        Log.d(TAG, "HetComm setting:" + isEnabled);
        if (isEnabled) {
            Intent serviceIntent = new Intent(context, HetCommService.class);
            context.startService(serviceIntent);
        }
    }

}