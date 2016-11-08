/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony.Sms.Intents;
import android.telephony.SubscriptionManager;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.os.PowerManager;

/// M:
import com.android.internal.telephony.PhoneConstants;
import com.android.mms.MmsApp;
import com.android.mms.util.DraftCache;
import com.android.mms.util.MmsLog;
import com.mediatek.ipmsg.util.IpMessageUtils;
import com.mediatek.mms.ext.IOpSmsReceiverExt;
import com.mediatek.mms.ipmessage.IIpSpamMsgReceiverExt;
import com.mediatek.mms.util.PermissionCheckUtil;
import com.mediatek.opmsg.util.OpMessageUtils;

/// @ M: fix bug ALPS00935474 , only show Mms Widget in primary user mode.
import android.os.UserHandle;
import android.content.pm.PackageManager;
import com.android.mms.util.FeatureOption;
/// @
/**
 * Handle incoming SMSes.  Just dispatches the work off to a Service.
 */
public class SmsReceiver extends BroadcastReceiver {
    static final Object mStartingServiceSync = new Object();
    static PowerManager.WakeLock mStartingService;
    private static SmsReceiver sInstance;

    public static final String RADIO_HIDE_STATE_START = "start";
    public static final String RADIO_HIDE_STATE_STOP = "stop";

    private static SparseArray<String> sCurrentState = new SparseArray<String>();
    private static SparseBooleanArray sNeedRecover = new SparseBooleanArray();

    // add for ip spam message
    private IIpSpamMsgReceiverExt mIpSpamMsgReceiver;
    // M: add for op
    private IOpSmsReceiverExt mOpSmsReceiverExt;

    public static SmsReceiver getInstance() {
        if (sInstance == null) {
            sInstance = new SmsReceiver();
        }
        return sInstance;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ///@ M: fix bug ALPS00935474 , only show Mms Widget in primary user mode.
      /// M: ALPS01301350 Message APP exist in HOME menu @{
        if ((intent.getAction() != null) 
            && intent.getAction().equals("android.intent.action.USER_SWITCHED_FOR_MULTIUSER_APP")
            && FeatureOption.MTK_ONLY_OWNER_SIM_SUPPORT) {
            MmsLog.d(MmsApp.TXN_TAG, "smsReceive handling MULTI_USER_CHANGED");
      /// @}
            if (UserHandle.myUserId() == UserHandle.USER_OWNER) {
                //context.getPackageManager().setApplicationEnabledSetting("com.android.mms", PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 0);
                context.getPackageManager().setComponentEnabledSetting(
                        new ComponentName("com.android.mms", "com.android.mms.ui.BootActivity"),
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP);
                context.getPackageManager().setComponentEnabledSetting(
                        new ComponentName("com.android.mms",
                                "com.android.mms.widget.MmsWidgetProvider"),
                                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                                PackageManager.DONT_KILL_APP);
                context.getPackageManager().setComponentEnabledSetting(
                        new ComponentName("com.android.mms",
                                "com.android.mms.ui.ComposeMessageActivity"),
                                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                                PackageManager.DONT_KILL_APP);
                context.getPackageManager().setComponentEnabledSetting(
                        new ComponentName("com.android.mms",
                                "com.mediatek.mms.ui.ShareVCardViaMMSActivity"),
                                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                                PackageManager.DONT_KILL_APP);
            } else {
                //context.getPackageManager().setApplicationEnabledSetting("com.android.mms",
                //PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
                context.getPackageManager().setComponentEnabledSetting(
                        new ComponentName("com.android.mms", "com.android.mms.ui.BootActivity"),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
                context.getPackageManager().setComponentEnabledSetting(
                        new ComponentName("com.android.mms",
                                "com.android.mms.widget.MmsWidgetProvider"),
                                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                PackageManager.DONT_KILL_APP);
                context.getPackageManager().setComponentEnabledSetting(
                        new ComponentName("com.android.mms",
                                "com.android.mms.ui.ComposeMessageActivity"),
                                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                PackageManager.DONT_KILL_APP);
                context.getPackageManager().setComponentEnabledSetting(
                        new ComponentName("com.android.mms",
                                "com.mediatek.mms.ui.ShareVCardViaMMSActivity"),
                                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                PackageManager.DONT_KILL_APP);
            }
        }
        ///@

        if (!PermissionCheckUtil.checkRequiredPermissions(context)) {
            MmsLog.d(MmsApp.TXN_TAG, "SmsReceiver: onReceive() no permissions return !!");
            return;
        } else {
            MmsLog.d(MmsApp.TXN_TAG, "SmsReceiver: onReceive() has permisson: "
                    + DraftCache.getInstance());
            if (DraftCache.getInstance() == null) {
                MmsApp.getApplication().onRequestPermissionsResult();
            }
        }

        onReceiveWithPrivilege(context, intent, false);
    }

    protected void onReceiveWithPrivilege(Context context, Intent intent, boolean privileged) {
        // If 'privileged' is false, it means that the intent was delivered to the base
        // no-permissions receiver class.  If we get an SMS_RECEIVED message that way, it
        // means someone has tried to spoof the message by delivering it outside the normal
        // permission-checked route, so we just ignore it.
        if (!privileged && intent.getAction().equals(Intents.SMS_DELIVER_ACTION)) {
            return;
        }

        // add for ipmessage
        mIpSpamMsgReceiver = IpMessageUtils.getIpMessagePlugin(context).getIpSpamMsgReceiver();
        if (mIpSpamMsgReceiver.onIpReceiveSpamMsg(context, intent, false)) {
            return;
        }

        mOpSmsReceiverExt = OpMessageUtils.getOpMessagePlugin().getOpSmsReceiverExt();
        mOpSmsReceiverExt.onReceiveWithPrivilege(intent, SmsReceiverService.ACTION_SEND_MESSAGE);

        MmsLog.d(MmsApp.TXN_TAG, "SmsReceiver: onReceiveWithPrivilege(). SubId = "
                        + intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY,
                                SubscriptionManager.INVALID_SUBSCRIPTION_ID)
            + ", Action = " + intent.getAction()
            + ", result = " + getResultCode());
        intent.setClass(context, SmsReceiverService.class);
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
        synchronized (mStartingServiceSync) {
            if (mStartingService == null) {
                PowerManager pm =
                    (PowerManager)context.getSystemService(Context.POWER_SERVICE);
                mStartingService = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                        "StartingAlertService");
                mStartingService.setReferenceCounted(false);
            }
            mStartingService.acquire();
            context.startService(intent);
        }
    }

    /**
     * Called back by the service when it has finished processing notifications,
     * releasing the wake lock if the service is now stopping.
     */
    public static void finishStartingService(Service service, int startId) {
        /// M:
        MmsLog.d(MmsApp.TXN_TAG, "Sms finishStartingService");
        synchronized (mStartingServiceSync) {
            if (mStartingService != null) {
                if (service.stopSelfResult(startId)) {
                    mStartingService.release();
                }
            }
        }
    }

    public static String getCurrentState(int subId) {
        String state = sCurrentState.get((int) subId);
        if (state == null) {
            state = RADIO_HIDE_STATE_STOP;
        }
        return state;
    }
}
