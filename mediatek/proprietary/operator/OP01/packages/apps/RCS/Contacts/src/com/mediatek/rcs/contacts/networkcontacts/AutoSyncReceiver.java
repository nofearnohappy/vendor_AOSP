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

package com.mediatek.rcs.contacts.networkcontacts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.gsma.joyn.JoynServiceConfiguration;


/**
 * handle Broadcast :BOOT_COMPLETED,TIME_SET,auto backup alarm message.
 * @author MTK80963
 *
 */
public class AutoSyncReceiver extends BroadcastReceiver {
    private static final String TAG = "NetworkContacts::AutoSyncReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            Log.d(TAG, "intent == null");
            return;
        }
        String action = intent.getAction();
        Log.d(TAG, "action = " + action);
        //Note: Firstly handle RCS on/off intent
        if (action == null) {
            return;
        } else if (action.equalsIgnoreCase(Const.Action.LAUNCH_RCS_SERVICE)) {
            //reset auto backup  message
            startAutoBackup(context, false);
        } else if (action.equalsIgnoreCase(Const.Action.STOP_RCS_SERVICE)) {
            //stop auto backup  message
            stopAutoBackup(context);
        }
        //If RCS switch is on,handle other intents
        if (!JoynServiceConfiguration.isServiceActivated(context)) {
            Log.d(TAG, "RCS is OFF");
            return;
        } else {
            if (action.equalsIgnoreCase(Const.Action.AUTO_BACKUP)) {
                intent.setClass(context, SyncService.class);
                context.startService(intent);
                //reset auto backup  message
                startAutoBackup(context, true);
            } else if (action.equalsIgnoreCase(Const.Action.TIME_SET)) {
                //reset auto backup  message
                startAutoBackup(context, true);
            } else if (action.equalsIgnoreCase(Const.Action.BOOT_COMPLETED)) {
                /* check if complete normally */
                intent.setAction(Const.Action.CHECK_RESTORE);
                intent.setClass(context, SyncService.class);
                context.startService(intent);
                // start auto backup
                startAutoBackup(context, false);
            }
        }
    }

    private void startAutoBackup(Context context, boolean onlyTimely) {
        SettingsSharedPreference sharePref = new SettingsSharedPreference(
                context);
        int type = sharePref.getAutoBackupType();
        if (onlyTimely
                && type == SettingsSharedPreference.BACKUP_TYPE_IMMEDIATELY) {
            return;
        }

        AutoSyncManager autoSyncCalendar = new AutoSyncManager(context);
        if (sharePref.isAutoBackup()) {
            autoSyncCalendar.startAutoSync(type);
        }
    }

    private void stopAutoBackup(Context context) {
        AutoSyncManager autoSyncCalendar = new AutoSyncManager(context);
        autoSyncCalendar.stopAutoSync();
    }
}
