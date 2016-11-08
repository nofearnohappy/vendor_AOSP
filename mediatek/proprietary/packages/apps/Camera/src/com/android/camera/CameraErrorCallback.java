/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.camera;

import android.util.Log;

/**
 * The class used to handle camera error callback.
 */
public class CameraErrorCallback implements android.hardware.Camera.ErrorCallback {
    private static final String TAG = "CameraErrorCallback";

    @Override
    public void onError(int error, android.hardware.Camera camera) {
        Log.e(TAG, "onError got camera error callback. error = " + error);
        if (android.hardware.Camera.CAMERA_ERROR_SERVER_DIED == error
                || android.hardware.Camera.CAMERA_ERROR_EVICTED == error) {
            // We are not sure about the current state of the app when camera server died or
            // camera was disconnected due to use by higher priority user (in preview or
            // snapshot or recording). Closing the app is better than creating a
            // new Camera object.
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }
}
