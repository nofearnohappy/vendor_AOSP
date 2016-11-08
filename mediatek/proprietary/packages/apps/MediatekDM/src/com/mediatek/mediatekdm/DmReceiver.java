/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.mediatekdm;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.IDmComponent.DispatchResult;

public class DmReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null || context == null) {
            Log.w(TAG.RECEIVER, "Invalid arguments. Exit.");
            return;
        }

        Log.i(TAG.RECEIVER, "Received intent: " + intent);

        if (intent.getAction().equals(DmConst.IntentAction.DM_BOOT_COMPLETED)) {
            startKickOffAlarm(context);
        }

        boolean accepted = false;
        for (IDmComponent component : DmApplication.getInstance().getComponents()) {
            if (component.dispatchBroadcast(context, intent) == DispatchResult.ACCEPT) {
                accepted = true;
                break;
            }
        }
        if (!accepted) {
            /* Check AndroidManifest.xml for details. */
            if (intent.getAction().equals(DmConst.IntentAction.DM_BOOT_COMPLETED)) {
                Log.d(TAG.RECEIVER, "Receive action BOOT_COMPLETE, ignore");
            } else {
                Log.w(TAG.RECEIVER, "Normal intent. Forward it to service.");
                Intent serviceIntent = new Intent(intent);
                serviceIntent.setClass(context, DmService.class);
                context.startService(serviceIntent);
            }
        }
    }

    /**
     * Start an alarm which retries to send message (in case process killed by system)
     */
    private void startKickOffAlarm(Context context) {
        Intent intent = new Intent(DmConst.IntentAction.DM_KICK_OFF);
        intent.setClass(context, DmService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        PlatformManager.getInstance().startAlarm(context, pendingIntent,
                PlatformManager.DELAY_KICK_OFF);
    }

}
