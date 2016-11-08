/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SubscriptionManager;

import com.android.internal.telephony.PhoneConstants;
import com.android.mms.MmsApp;
import com.android.mms.util.MmsLog;


/**
 * Handle incoming Mms transaction finished.
 */
public class MmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        onReceiveWithPrivilege(context, intent, false);
    }

    protected void onReceiveWithPrivilege(Context context, Intent intent, boolean privileged) {
        MmsLog.d(
                MmsApp.TXN_TAG, "MmsReceiver: onReceiveWithPrivilege(). sub Id = "
                        + intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY,
                                SubscriptionManager.INVALID_SUBSCRIPTION_ID)
            + ", Action = " + intent.getAction()
            + ", result = " + getResultCode()
            + ", uri = " + intent.getStringExtra(TransactionBundle.URI));
        intent.setClass(context, TransactionService.class);
        intent.putExtra("result", getResultCode());
        beginStartingService(context, intent);
    }

    // N.B.: <code>beginStartingService</code> and
    // <code>finishStartingService</code> were copied from
    // <code>com.android.calendar.AlertReceiver</code>.  We should
    // factor them out or, even better, improve the API for starting
    // services under wake locks.

    /**
     * Start the service to process the current event notifications, acquiring
     * the wake lock before returning to ensure that the service will run.
     */
    public static void beginStartingService(Context context, Intent intent) {
        context.startService(intent);
    }

}
