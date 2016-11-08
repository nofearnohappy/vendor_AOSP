/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.dm.scomo;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.mediatek.dm.DmConst.TAG;
import com.mediatek.dm.R;

public class DmScomoNotification implements OnDmScomoUpdateListener {
    private static final String CLASS_TAG = TAG.SCOMO + "/Notification";
    public static final int NOTIFICATION = 11;
    private static final String ERROR_SIZE = "NaN";
    private static final int KB = 1024;

    private Context mContext;
    private NotificationManager mNotificationManager;
    private PendingIntent mDownloadDetailPendingIntent;

    public void onScomoUpdated() {
        DmScomoState scomoState = DmScomoState.getInstance(mContext);
        if (scomoState == null || !scomoState.mVerbose) {
            return;
        }
        int state = scomoState.mState;
        Log.i(CLASS_TAG, "notification onUpdate, the state is " + state);
        switch (state) {
        case DmScomoState.DOWNLOADING:
            CharSequence tickerText = mContext.getText(R.string.downloading_scomo);
            int totalSize = scomoState.mTotalSize;
            int currentSize = scomoState.mCurrentSize;
            CharSequence contentTitle;
            if (totalSize == 0) {
                contentTitle = ERROR_SIZE;
            } else {
                contentTitle = currentSize * 100 / totalSize + "%";
            }
            CharSequence contentText = mContext.getResources().getString(R.string.downloading, scomoState.getName());
            notifyDownloadNotification(tickerText, contentTitle, contentText, currentSize, totalSize);
            break;
        case DmScomoState.DOWNLOADING_STARTED:
        case DmScomoState.RESUMED:
            totalSize = scomoState.mTotalSize;
            currentSize = scomoState.mCurrentSize;
            tickerText = mContext.getText(R.string.downloading_scomo);
            contentText = new StringBuilder().append(currentSize / KB).append("KB/").append(totalSize / KB).append("KB");
            contentTitle = mContext.getText(R.string.connecting);
            notifyDownloadNotification(tickerText, contentTitle, contentText, currentSize, totalSize);
            break;
        case DmScomoState.PAUSED:
            totalSize = scomoState.mTotalSize;
            currentSize = scomoState.mCurrentSize;
            tickerText = mContext.getText(R.string.scomo_downloading_paused);
            contentText = new StringBuilder().append(currentSize / KB).append("KB/").append(totalSize / KB).append("KB");
            contentTitle = mContext.getText(R.string.scomo_downloading_paused);
            notifyDownloadNotification(tickerText, contentTitle, contentText, currentSize, totalSize);
            break;
        case DmScomoState.CONFIRM_UPDATE:
            tickerText = mContext.getText(R.string.confirm_update_scomo);
            contentText = mContext.getText(R.string.confirm_update_scomo);
            contentTitle = mContext.getText(R.string.click_for_detail);
            notifyConfirmNotification(tickerText, contentTitle, contentText, state);
            break;
        case DmScomoState.DOWNLOAD_FAILED:
            Intent intent = new Intent(mContext, DmScomoConfirmActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(DmScomoConfirmActivity.EXTRA_ACTION, state);
            mContext.startActivity(intent);
            break;
        case DmScomoState.INSTALL_FAILED:
            tickerText = mContext.getText(R.string.scomo_install_failed);
            contentText = mContext.getText(R.string.software_update);
            contentTitle = mContext.getText(R.string.scomo_install_failed);
            notifyConfirmNotification(tickerText, contentTitle, contentText, state);
            break;
        case DmScomoState.INSTALL_OK:
            tickerText = mContext.getText(R.string.scomo_install_ok);
            contentText = mContext.getText(R.string.software_update);
            contentTitle = mContext.getText(R.string.scomo_install_ok);
            notifyConfirmNotification(tickerText, contentTitle, contentText, state);
            break;
        case DmScomoState.CONFIRM_DOWNLOAD:
            tickerText = mContext.getText(R.string.new_scomo_available);
            contentText = mContext.getString(R.string.new_scomo_available);
            contentTitle = scomoState.getName();
            notifyConfirmNotification(tickerText, contentTitle, contentText, state);
            break;
        case DmScomoState.CONFIRM_INSTALL:
            tickerText = mContext.getText(R.string.confirm_install_scomo);
            contentText = mContext.getText(R.string.software_update);
            contentTitle = mContext.getText(R.string.download_complete);
            notifyConfirmNotification(tickerText, contentTitle, contentText, state);
            // fallthru
        case DmScomoState.IDLE:
            // fallthru
        case DmScomoState.ABORTED:
            Log.d(CLASS_TAG, "state is idle or aborted");
            mNotificationManager.cancel(NOTIFICATION);
            break;
        case DmScomoState.GENERIC_ERROR:
            tickerText = mContext.getText(R.string.scomo_failed);
            contentText = mContext.getText(R.string.software_update);
            contentTitle = mContext.getText(R.string.scomo_failed);
            notifyConfirmNotification(tickerText, contentTitle, contentText, state);
            break;
        case DmScomoState.INSTALLING:
            intent = new Intent(mContext, DmScomoActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_ONE_SHOT);
            Notification.Builder notifyBuilder = new Notification.Builder(mContext).setOngoing(true)
                    .setSmallIcon(R.drawable.stat_download_waiting).setWhen(System.currentTimeMillis())
                    .setContentIntent(pendingIntent).setTicker(mContext.getText(R.string.installing_scomo))
                    .setContentTitle(mContext.getText(R.string.installing_scomo))
                    .setContentText(mContext.getResources().getString(R.string.installing, scomoState.getName()));
            mNotificationManager.notify(NOTIFICATION, notifyBuilder.getNotification());
            break;
        default:
            break;
        }
    }

    private void notifyDownloadNotification(CharSequence tickerText, CharSequence contentTitle, CharSequence contentText,
            int currentSize, int totalSize) {
        int progress = (int) ((float) currentSize / (float) totalSize * 100);
        Notification.Builder notifyBuilder = new Notification.Builder(mContext).setOngoing(true)
                .setSmallIcon(R.drawable.stat_download_waiting).setWhen(System.currentTimeMillis())
                .setContentIntent(mDownloadDetailPendingIntent).setTicker(tickerText).setContentTitle(contentTitle)
                .setContentText(contentText).setProgress(100, progress, false);
        mNotificationManager.notify(NOTIFICATION, notifyBuilder.getNotification());
    }

    private void notifyConfirmNotification(CharSequence tickerText, CharSequence contentTitle, CharSequence contentText,
            int state) {
        Intent intent = new Intent(mContext, DmScomoConfirmActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(DmScomoConfirmActivity.EXTRA_ACTION, state);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        Notification.Builder notifyBuilder = new Notification.Builder(mContext).setAutoCancel(true)
                .setSmallIcon(R.drawable.stat_download_waiting).setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent).setTicker(tickerText).setContentTitle(contentTitle)
                .setContentText(contentText);
        mNotificationManager.notify(NOTIFICATION, notifyBuilder.getNotification());
    }

    public DmScomoNotification(Context context) {
        this.mContext = context;
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(context, DmScomoDownloadDetailActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mDownloadDetailPendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

}
