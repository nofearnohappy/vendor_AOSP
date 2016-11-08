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
package com.android.quicksearchbox.preferences;

import com.android.quicksearchbox.QsbApplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Activity to choose correct version of Search Activity dependent on Android version.
 */
public class SearchSettingsActivityChooser extends Activity {

    private static final String TAG = "QSB.SearchSettingsActivityChooser";
    private static final boolean DBG = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent settingsIntent = new Intent(Intent.ACTION_DEFAULT);
        Class<?> activityClass;
        /// M: Whether use fragment in search setting activity or not. @{
        if (QsbApplication.useSettingFragment()) {
            activityClass = getHoneycombActivityClass();
        } else {
            activityClass = getLegacyActivityClass();
        }
        if (DBG) {
            Log.d(TAG, "useSettingFragment: " + QsbApplication.isHoneycombOrLater() +
                    " class = " + activityClass);
        }
        /// @}
        settingsIntent.setClass(this, activityClass);
        startActivity(settingsIntent);
        finish();
    }

    protected Class<?> getLegacyActivityClass() {
        return LegacySearchSettingsActivity.class;
    }

    protected Class<?> getHoneycombActivityClass() {
        return SearchSettingsActivity.class;
    }
}
