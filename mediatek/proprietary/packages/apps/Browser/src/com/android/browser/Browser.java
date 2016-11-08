/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.browser;

import android.app.Application;
import android.os.Build;
import android.os.SystemProperties;
import android.util.Log;
import android.webkit.CookieSyncManager;

/// M: Add for Regional Phone support. @{
import com.mediatek.browser.ext.IBrowserRegionalPhoneExt;
/// @}

public class Browser extends Application {

    private final static String LOGTAG = "browser";

    // Set to true to enable verbose logging.
    final static boolean LOGV_ENABLED = false;

    // Set to true to enable extra debug logging.
    final static boolean LOGD_ENABLED = true;

    // M : flag for auto on/off log
    final static boolean DEBUG =
        Build.TYPE.equals("eng") ? true : SystemProperties.getBoolean("ro.debug.browser", false);

    @Override
    public void onCreate() {
        super.onCreate();

        if (LOGV_ENABLED)
            Log.v(LOGTAG, "Browser.onCreate: this=" + this);

        // create CookieSyncManager with current Context
        CookieSyncManager.createInstance(this);
        BrowserSettings.initialize(getApplicationContext());
        Preloader.initialize(getApplicationContext());

        /// M: Add for Regional Phone support. @{
        IBrowserRegionalPhoneExt browserRegionalPhone = Extensions.getRegionalPhonePlugin(getApplicationContext());
        browserRegionalPhone.updateBookmarks(getApplicationContext());
        BrowserSettings.getInstance().updateSearchEngineSetting();
        /// @}
    }

}

