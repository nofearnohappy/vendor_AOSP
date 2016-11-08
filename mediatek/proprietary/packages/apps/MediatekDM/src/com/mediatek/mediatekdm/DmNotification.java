/*
 * Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are protected under
 * relevant copyright laws. The information contained herein is confidential and proprietary to
 * MediaTek Inc. and/or its licensors. Without the prior written permission of MediaTek inc. and/or
 * its licensors, any reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES THAT THE
 * SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE") RECEIVED FROM MEDIATEK AND/OR ITS
 * REPRESENTATIVES ARE PROVIDED TO RECEIVER ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS
 * ANY AND ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT. NEITHER DOES MEDIATEK
 * PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED
 * BY, INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO
 * SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT
 * IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN
 * MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE
 * TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM. RECEIVER'S SOLE
 * AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK
 * SOFTWARE RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK
 * SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software") have been
 * modified by MediaTek Inc. All revisions are subject to any receiver's applicable license
 * agreements with MediaTek Inc.
 */

package com.mediatek.mediatekdm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.mediatek.mediatekdm.DmConst.IntentAction;
import com.mediatek.mediatekdm.DmConst.NotificationInteractionType;
import com.mediatek.mediatekdm.DmConst.TAG;

public class DmNotification {
    private int mType = NotificationInteractionType.TYPE_INVALID;
    private DmService mService;
    private NotificationManager mNotificationManager;
    private final Integer mNotificationText;

    public DmNotification(DmService service) {
        Log.d(TAG.NOTIFICATION, "DmNotification.<init>(" + service + ")");
        mService = service;
        mNotificationManager = (NotificationManager) mService
                .getSystemService(Context.NOTIFICATION_SERVICE);
        String opName = DmConfig.getInstance().getCustomizedOperator();
        if (opName != null && opName.equalsIgnoreCase("cu")) {
            mNotificationText = R.string.usermode_visible_cu;
        } else if (opName != null && opName.equalsIgnoreCase("cmcc")) {
            mNotificationText = R.string.usermode_visible_cmcc;
        } else {
            mNotificationText = R.string.usermode_visible;
        }
    }

    public void showNotification(int type) {
        Log.d(TAG.NOTIFICATION, "+showNotification(" + type + ")");
        if (mType != type) {
            clear();
            mType = type;
        }
        Notification notification = null;
        switch (type) {
            case NotificationInteractionType.TYPE_NOTIFICATION_VISIBLE:
            case NotificationInteractionType.TYPE_NOTIFICATION_INTERACT:
                notification = new Notification.Builder(mService)
                        .setContentTitle(mService.getText(R.string.app_name))
                        .setContentText(mService.getText(mNotificationText))
                        .setTicker(mService.getString(mNotificationText))
                        .setContentIntent(makeNotificationPendingIntent(type))
                        .setSmallIcon(R.drawable.stat_download_info).build();

                Log.d(TAG.NOTIFICATION, "Have notification, keep");
                notification.flags = Notification.FLAG_NO_CLEAR;
                notification.defaults = Notification.DEFAULT_ALL;
                break;
            case NotificationInteractionType.TYPE_ALERT_1101:
                notification = new Notification.Builder(mService)
                        .setContentTitle(mService.getText(R.string.app_name))
                        .setContentText(mService.getText(R.string.nia_info))
                        .setTicker(mService.getString(R.string.nia_info))
                        .setContentIntent(makeNotificationPendingIntent(type))
                        .setSmallIcon(R.drawable.stat_download_info).build();

                Log.d(TAG.NOTIFICATION, "Have alert, keep");
                notification.flags = Notification.FLAG_NO_CLEAR;
                notification.defaults = Notification.DEFAULT_ALL;
                break;
            // We may add support for other alert types here.
            default:
                throw new Error("Invalid notification type " + type);
        }
        mNotificationManager.notify(type, notification);
        Log.d(TAG.NOTIFICATION, "-showNotification()");
    }

    public void clear() {
        Log.w(TAG.NOTIFICATION, "clear notification type is " + mType);
        mService.cancelNiaAlertTimeout();
        mService.sendBroadcast(new Intent(IntentAction.DM_CLOSE_DIALOG));
        mNotificationManager.cancel(mType);
        mType = NotificationInteractionType.TYPE_INVALID;
    }

    private PendingIntent makeNotificationPendingIntent(int type) {
        Intent intent = new Intent(mService, DmNIInteractionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(DmNIInteractionActivity.EXTRA_KEY_TYPE, type);
        mService.startActivity(intent);
        return PendingIntent.getActivity(mService, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

}
