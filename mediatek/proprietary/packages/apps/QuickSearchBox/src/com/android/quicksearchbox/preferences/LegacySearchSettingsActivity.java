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

import com.android.quicksearchbox.R;

/**
 * Activity for setting global search preferences. This is the version for Gingerbread and earlier
 * which to not have support for fragments.
 */
public class LegacySearchSettingsActivity extends LegacySearchSettingsActivityBase {

    @Override
    protected int getPreferencesResourceId() {
        return R.xml.legacy_preferences;
    }

}
