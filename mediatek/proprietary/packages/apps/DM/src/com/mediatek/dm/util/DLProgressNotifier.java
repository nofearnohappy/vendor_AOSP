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

package com.mediatek.dm.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.util.Log;

import com.mediatek.dm.DmConst.TAG;
import com.mediatek.dm.DmDownloadNotification;
import com.mediatek.dm.R;

/***
 * ICS style notification for downloading progress.
 *
 * @author MTK81019
 *
 */
public class DLProgressNotifier {
    private static final int DOWNLOADING_ICON = R.drawable.stat_download_waiting;
    private static final int DOWNLOADING_STR = R.string.status_bar_notifications_downloading;
    private static final int DOWNLOADING_NOTIFY_TYPE = DmDownloadNotification.NOTIFICATION_DOWNLOADING;

    private Context mContext;
    private PendingIntent mRespondIntent;
    private NotificationManager mNotifyManager;
    private Notification.Builder mNotifyBuilder;

    public DLProgressNotifier(Context context, PendingIntent intent) {
        mContext = context;
        mRespondIntent = intent;
        mNotifyManager = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);

        mNotifyBuilder = new Notification.Builder(mContext);
        mNotifyBuilder.setOngoing(true).setSmallIcon(DOWNLOADING_ICON)
                .setContentTitle(mContext.getString(DOWNLOADING_STR))
                .setWhen(System.currentTimeMillis())
                .setContentIntent(mRespondIntent).setProgress(100, 0, false);
    }

    public void onProgressUpdate(int currentSize, int totalSize) {
        int progress = (int) ((float) currentSize / (float) totalSize * 100);
        String percent = String.valueOf(progress) + "%";

        mNotifyBuilder.setProgress(100, progress, false);
        mNotifyBuilder.setContentInfo(percent);
        mNotifyManager.notify(DOWNLOADING_NOTIFY_TYPE,
                mNotifyBuilder.getNotification());
    }

    public void onFinish() {
        Log.d(TAG.COMMON, "### download notification cancelled ###");
        mNotifyManager.cancel(DOWNLOADING_NOTIFY_TYPE);
    }
}
