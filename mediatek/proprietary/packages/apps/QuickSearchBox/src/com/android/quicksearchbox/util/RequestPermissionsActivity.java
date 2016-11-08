/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.quicksearchbox.util;

import android.Manifest.permission;
import android.app.Activity;

/**
 * Activity that requests permissions needed for activities interacting with QuickSearchBox.
 */
public class RequestPermissionsActivity extends RequestPermissionsActivityBase {

    private static final String[] REQUIRED_PERMISSIONS = new String[]{
        // "Contacts" group. Without this permission, the Contacts Search will not function.
        permission.READ_CONTACTS,
        // "Phone" group. This is required for Calendar search enhancement
        permission.READ_CALENDAR,
        // "Phone" group. This is required for SMS search
        permission.READ_SMS,
        // "Phone" group. This is required for Call log search
        permission.READ_CALL_LOG,
        // "Phone" group. This is required for FileManager and Music search
        permission.READ_EXTERNAL_STORAGE,
    };

    @Override
    protected String[] getRequiredPermissions() {
        return REQUIRED_PERMISSIONS;
    }

    @Override
    protected String[] getDesiredPermissions() {
        //All permissions have same handling for now. So request all.
        return REQUIRED_PERMISSIONS;
    }

    /**
     * Start Permission Activity for requesting permissions.
     * This is designed to be called inside {@link android.app.Activity#onCreate}
     *
     * @param  activity    Context variable.
     * @return  true if permissions pass.
     */
    public static boolean startPermissionActivity(Activity activity) {
        return startPermissionActivity(activity, REQUIRED_PERMISSIONS,
                RequestPermissionsActivity.class);
    }
}
