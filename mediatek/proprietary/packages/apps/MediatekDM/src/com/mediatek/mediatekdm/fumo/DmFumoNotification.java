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

package com.mediatek.mediatekdm.fumo;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.mediatek.mediatekdm.DmApplication;
import com.mediatek.mediatekdm.DmConst;
import com.mediatek.mediatekdm.DmConst.NotificationInteractionType;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.DmOperation;
import com.mediatek.mediatekdm.DmOperation.KEY;
import com.mediatek.mediatekdm.DmOperation.Type;
import com.mediatek.mediatekdm.DmService;
import com.mediatek.mediatekdm.R;
import com.mediatek.mediatekdm.util.DLProgressNotifier;

public class DmFumoNotification implements IDmFumoStateObserver, IDmFumoDownloadProgressObserver {

    private int mType = NotificationInteractionType.TYPE_INVALID;
    private DmService mService;
    private NotificationManager mNotificationManager;
    private Handler mHandler;
    private DLProgressNotifier mDLProgressNotifier = null;

    /**
     * Constructor
     *
     * @param service
     *        Context object of android environment
     * @return
     */
    public DmFumoNotification(DmService service) {
        mService = service;
        mNotificationManager = (NotificationManager) mService
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mHandler = new Handler(service.getMainLooper());
    }

    /**
     * Clear other notifications and show new version detection notification.
     *
     * @param null
     * @return null
     */
    private void showNewVersionNotification() {
        Log.d(TAG.NOTIFICATION, "+showNewVersionNotification()");
        if (mType != NotificationInteractionType.TYPE_FUMO_NEW_VERSION) {
            clear();
            mType = NotificationInteractionType.TYPE_FUMO_NEW_VERSION;
        }
        Notification notification = new Notification.Builder(mService)
                .setContentTitle(mService.getText(R.string.status_bar_notifications_title))
                .setContentText(mService.getText(R.string.status_bar_notifications_new_version))
                .setTicker(mService.getString(R.string.status_bar_notifications_new_version))
                .setContentIntent(makeDownloadNotificationPendingIntent())
                .setSmallIcon(R.drawable.stat_download_info).build();

        notification.flags = Notification.FLAG_AUTO_CANCEL;
        notification.defaults = Notification.DEFAULT_ALL;
        mNotificationManager.notify(mType, notification);
        Log.d(TAG.NOTIFICATION, "-showNewVersionNotification()");
    }

    /**
     * For DmService to show the new download completed notification
     *
     * @return null
     */
    private void showDownloadCompletedNotification() {
        Log.d(TAG.NOTIFICATION, "+showDownloadCompletedNotification()");
        if (mType != NotificationInteractionType.TYPE_FUMO_DOWNLOAD_COMPLETED) {
            clear();
            mType = NotificationInteractionType.TYPE_FUMO_DOWNLOAD_COMPLETED;
        }
        Notification notification = new Notification.Builder(mService)
                .setContentTitle(mService.getText(R.string.status_bar_notifications_title))
                .setContentText(
                        mService.getString(R.string.status_bar_notifications_download_completed))
                .setTicker(mService.getString(R.string.status_bar_notifications_download_completed))
                .setContentIntent(makeDownloadNotificationPendingIntent())
                .setSmallIcon(R.drawable.stat_download_complete).build();

        notification.flags = Notification.FLAG_AUTO_CANCEL;
        notification.defaults = Notification.DEFAULT_ALL;
        mNotificationManager.notify(mType, notification);
        Log.d(TAG.NOTIFICATION, "-showDownloadCompletedNotification()");
    }

    /**
     * For DmService to clear the notification. When the notification is clicked to start an
     * activity, the notification should be cleared.
     *
     * @return null
     */
    public void clear() {
        Log.w(TAG.NOTIFICATION, "clearDownloadNotification notification type is " + mType);
        mNotificationManager.cancel(mType);
        mType = NotificationInteractionType.TYPE_INVALID;
        if (mDLProgressNotifier != null) {
            mDLProgressNotifier.onFinish();
        }
    }

    private PendingIntent makeDownloadNotificationPendingIntent() {
        Intent intent = new Intent(mService, DmService.class);
        intent.setAction(DmConst.IntentAction.DM_DL_FOREGROUND);
        PendingIntent mPendingIntent = PendingIntent.getService(mService, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return mPendingIntent;
    }

    @Override
    /* IDmFumoStateObserver */
    public void notify(final int newState, final int previousState, final DmOperation operation,
            final Object extra) {
        Log.d(TAG.FUMO, "FUMO state notified in DmFumoNotification: " + newState);
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                switch (newState) {
                    case DmFumoState.UPDATE_COMPLETE:
                        clear();
                        break;

                    case DmFumoState.NEW_VERSION_FOUND:
                        if (operation.getProperty(KEY.TYPE).equals(Type.TYPE_DL)
                                && operation.getBooleanProperty(KEY.FUMO_TAG, false)) {
                            if (!operation.getProperty(KEY.INITIATOR).equals("User")) {
                                // User triggered FUMO DL session
                                showNewVersionNotification();
                            }
                        } else {
                            throw new Error("Invalid operation type");
                        }
                        break;

                    case DmFumoState.DOWNLOAD_COMPLETE:
                        showDownloadCompletedNotification();
                        break;

                    default:
                        break;
                }
            }
        });
    }

    @Override
    /* IDmFumoDownloadProgressObserver */
    public void updateProgress(final long current, final long total) {
        if (mType != NotificationInteractionType.TYPE_FUMO_DOWNLOADING) {
            clear();
            mType = NotificationInteractionType.TYPE_FUMO_DOWNLOADING;
        }
        FumoComponent component = (FumoComponent) DmApplication.getInstance().findComponentByName(
                FumoComponent.NAME);
        int state = component.getFumoManager().getFumoState();
        Log.d(TAG.NOTIFICATION, "FUMO state is " + state);
        if (state == DmFumoState.DOWNLOADING) {
            // We can only update UI in main thread
            mService.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    if (mDLProgressNotifier == null) {
                        mDLProgressNotifier = new DLProgressNotifier(mService,
                                makeDownloadNotificationPendingIntent());
                    }
                    mDLProgressNotifier.onProgressUpdate(current, total);
                }
            });
        }
    }
}
