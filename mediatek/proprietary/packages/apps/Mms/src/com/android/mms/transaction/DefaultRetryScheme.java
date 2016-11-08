/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.mms.transaction;

import android.content.Context;
import android.util.Config;
import android.util.Log;

import com.mediatek.mms.callback.IDefaultRetrySchemeCallback;
import com.mediatek.mms.ext.IOpDefaultRetrySchemeExt;
import com.mediatek.opmsg.util.OpMessageUtils;

/**
 * Default retry scheme, based on specs.
 */
public class DefaultRetryScheme extends AbstractRetryScheme
        implements IDefaultRetrySchemeCallback {
    private static final String TAG = "DefaultRetryScheme";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = DEBUG ? Config.LOGD : Config.LOGV;

    private static final int[] sDefaultRetryScheme = {
        0, 1 * 60 * 1000, 5 * 60 * 1000, 10 * 60 * 1000, 30 * 60 * 1000};

    private static int[] sRetryScheme = null;
    // add for op
    private IOpDefaultRetrySchemeExt mOpDefaultRetryScheme;

    public DefaultRetryScheme(Context context, int retriedTimes, int... messageType) {
        super(retriedTimes);

        mOpDefaultRetryScheme = OpMessageUtils.getOpMessagePlugin().getOpDefaultRetrySchemeExt();
        sRetryScheme = mOpDefaultRetryScheme.init(messageType);
        if (sRetryScheme == null) {
            sRetryScheme = sDefaultRetryScheme;
        }

        mRetriedTimes = mRetriedTimes < 0 ? 0 : mRetriedTimes;
        mRetriedTimes = mRetriedTimes >= sRetryScheme.length
                ? sRetryScheme.length - 1 : mRetriedTimes;
    }

    @Override
    public int getRetryLimit() {
        Log.d(TAG, "getRetryLimit, " + sRetryScheme.length);
        return sRetryScheme.length;
    }

    @Override
    public long getWaitingInterval() {
        if (LOCAL_LOGV) {
            Log.v(TAG, "Next int: " + sRetryScheme[mRetriedTimes]);
        }
        return sRetryScheme[mRetriedTimes];
    }

    public void setRetryScheme(int[] retryScheme) {
        sRetryScheme = retryScheme;
    }

    public void setRetrySchemeCallback(int[] retryScheme) {
        setRetryScheme(retryScheme);
    }
}
