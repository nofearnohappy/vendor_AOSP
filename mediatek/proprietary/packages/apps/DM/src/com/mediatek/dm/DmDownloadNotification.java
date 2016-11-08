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

package com.mediatek.dm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.mediatek.dm.DmConst.TAG;
import com.mediatek.dm.data.IDmPersistentValues;
import com.mediatek.dm.data.PersistentContext;
import com.mediatek.dm.fumo.DmClient;
import com.mediatek.dm.util.DLProgressNotifier;

public class DmDownloadNotification {
    public static final int NOTIFICATION_NEW_VERSION = 1;
    public static final int NOTIFICATION_DOWNLOADING = 2;
    public static final int NOTIFICATION_DOWNLOAD_COMPLETED = 3;
    public static final int NOTIFICATION_NIA_RECEIVED = 4;

    public static final int NOTIFICATION_USERMODE_VISIBLE = 5;
    public static final int NOTIFICATION_USERMODE_INTERACT = 6;
    public static final int NOTIFICATION_NIA_START = 7;
    public static final int NOTIFICATION_NIA_ALERT1102 = 8;

    private static int sDLStatus = -1;
    private static Integer sUIVisible;

    // to show download progress in ICS style.
    private DLProgressNotifier mDLProgressNotifier;

    /**
     * Constructed function
     *
     * @param context
     * @return
     */
    public DmDownloadNotification(Context context) {
        mNotificationContext = context;
        sNotificationManager = (NotificationManager) mNotificationContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
        sDLStatus = PersistentContext.getInstance(context).getDLSessionStatus();
        PersistentContext.getInstance(context).registerObserver(mObserver);
        String opName = DmCommonFun.getOperatorName();
        if (DmConst.OperatorName.CU.equalsIgnoreCase(opName)) {
            sUIVisible = R.string.usermode_visible_cu;
        } else if (DmConst.OperatorName.CMCC.equalsIgnoreCase(opName)) {
            sUIVisible = R.string.usermode_visible_cmcc;
        } else {
            sUIVisible = R.string.usermode_visible_cu;
        }

    }

    public void showNiaNotification() {
        Log.w(TAG.NOTIFICATION, "showNiaNotification begin");
        if (sNotificationType != NOTIFICATION_NIA_START) {
            clearDownloadNotification();
            sNotificationType = NOTIFICATION_NIA_START;
        }
        setNotificationType(R.drawable.stat_download_info, R.string.nia_info);
    }

    public void showInputQueryNotification() {
        Log.i(TAG.NOTIFICATION, "show input query notification");
        if (sNotificationType != NOTIFICATION_NIA_ALERT1102) {
            clearDownloadNotification();
            sNotificationType = NOTIFICATION_NIA_ALERT1102;
        }

        setNotificationType(R.drawable.stat_download_info, R.string.nia_info);
    }

    public void showUserModeNotification(int type) {
        if (sNotificationType != type) {
            clearDownloadNotification();
            sNotificationType = type;
        }
        setNotificationType(R.drawable.stat_download_info, sUIVisible);
    }

    /**
     * For DmService to show the new version notification
     *
     * @param null
     * @return null
     */
    public void showNewVersionNotification() {
        if (sNotificationType != NOTIFICATION_NEW_VERSION) {
            clearDownloadNotification();
            sNotificationType = NOTIFICATION_NEW_VERSION;
        }
        setNotificationType(R.drawable.stat_download_info,
                R.string.status_bar_notifications_new_version);
    }

    /**
     * For DmService to show the new download completed notification
     *
     * @param null
     * @return null
     */
    public void showDownloadCompletedNotification() {
        Log.w(TAG.NOTIFICATION, "showDownloadComletedNotification enter");
        if (sNotificationType != NOTIFICATION_DOWNLOAD_COMPLETED) {
            clearDownloadNotification();
            sNotificationType = NOTIFICATION_DOWNLOAD_COMPLETED;
        }
        setNotificationType(R.drawable.stat_download_complete,
                R.string.status_bar_notifications_download_completed);
    }

    /**
     * For DmService to clear the notification
     *
     * @param null
     * @return null
     */
    public void clearDownloadNotification() {
        // when the notification is opened and start another activity, the
        // notification should be clear call this function to clear
        Log.w(TAG.NOTIFICATION, "clearDownloadNotification notification type is "
                + sNotificationType);
        if (sNotification != null) {
            sNotificationManager.cancel(sNotificationType);
            sNotification = null;
        }

        if (mDLProgressNotifier != null) {
            mDLProgressNotifier.onFinish();
        }

        return;
    }

    private static int sNotificationType;
    private Context mNotificationContext;
    private static NotificationManager sNotificationManager;
    private static Notification sNotification;

    /**
     * Make the notification pending intent
     *
     * @param null
     * @return the pending intent
     */
    private PendingIntent makeNotificationPendingIntent(int icon) {
        Log.w(TAG.NOTIFICATION, "makeNotificationPendingIntent begin");
        Intent mNotificationIntent = new Intent(mNotificationContext, DmService.class);
        mNotificationIntent.setAction(DmConst.IntentAction.DM_DL_FOREGROUND);
        PendingIntent mPendingIntent = PendingIntent.getService(mNotificationContext, 0,
                mNotificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        return mPendingIntent;
    }

    private PendingIntent makeNiaStartNotificationPendingIntent(int icon) {
        Log.w(TAG.NOTIFICATION, "makeNiaStartPendingIntent begin");
        Intent mNotificationIntent = new Intent(mNotificationContext, DmNiInfoActivity.class);
        mNotificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (sNotificationType == NOTIFICATION_NIA_START) {
            mNotificationIntent.putExtra(DmNiInfoActivity.EXTRA_TYPE,
                    DmConst.ServerMessage.TYPE_ALERT_1101);
        } else if (sNotificationType == NOTIFICATION_NIA_ALERT1102) {
            mNotificationIntent.putExtra(DmNiInfoActivity.EXTRA_TYPE,
                    DmConst.ServerMessage.TYPE_ALERT_1102);
        }

        PendingIntent mPendingIntent = PendingIntent.getActivity(mNotificationContext, 0,
                mNotificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        return mPendingIntent;
    }

    private PendingIntent makeUserModeNotificationPendingIntent(int icon) {
        Log.w(TAG.NOTIFICATION, "makeUserModeNotificationPendingIntent begin");
        Intent mNotificationIntent = new Intent(mNotificationContext, DmNiInfoActivity.class);
        mNotificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (sNotificationType == NOTIFICATION_USERMODE_VISIBLE) {
            mNotificationIntent.putExtra(DmNiInfoActivity.EXTRA_TYPE,
                    DmConst.ServerMessage.TYPE_UIMODE_VISIBLE);
        } else if (sNotificationType == NOTIFICATION_USERMODE_INTERACT) {
            mNotificationIntent.putExtra(DmNiInfoActivity.EXTRA_TYPE,
                    DmConst.ServerMessage.TYPE_UIMODE_INTERACT);
        }

        PendingIntent mPendingIntent = PendingIntent.getActivity(mNotificationContext, 0,
                mNotificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        return mPendingIntent;
    }

    /**
     * According the notification icon and string to set the notification type
     *
     * @param icon
     *            , notification content
     * @return null
     */
    private void setNotificationType(int downloadIcon, int notificationString) {
        CharSequence notificationText = mNotificationContext.getText(notificationString);
        String notificationContent = mNotificationContext.getString(notificationString);
        // Set the icon, scrolling text and timestamp

        // if the typeId is for downing and set the progress bar
        if (sNotification == null) {
            sNotification = new Notification(downloadIcon, notificationContent,
                    System.currentTimeMillis());
        }

        if (downloadIcon == R.drawable.stat_download_waiting) {
            Log.i(TAG.NOTIFICATION, "icon is download waiting, do nothing");
            // if(mNotification==null)
            // {
            // mNotification = new Notification(downloadIcon,
            // notificationContent,System.currentTimeMillis());
            // mNotification.contentIntent=
            // makeNotificationPendingIntent(downloadIcon);
            // }
            // if(mNotification!=null)
            // {
            // //using downloadingnotification.xml
            // mNotification.contentView = new
            // RemoteViews(mNotificationContext.getPackageName(),R.layout.downloadnotification);
            // //set the progress bar, the progress is currentProgress
            // mNotification.icon = R.drawable.stat_download_waiting ;
            // mNotification.contentView.setProgressBar(R.id.pb, 100,
            // mCurrentProgress, false);
            // //do not let the user to clear the downloading notification
            // mNotification.flags = Notification.FLAG_ONGOING_EVENT;
            // CharSequence text = "  " + Integer.toString(mCurrentProgress) +
            // "%    " ;
            // mNotification.contentView.setTextViewText(R.id.downloadratial,
            // text);
            // }
        } else {
            CharSequence title = "";
            PendingIntent pendingIntent = null;
            switch (sNotificationType) {
            case NOTIFICATION_NIA_START:
            case NOTIFICATION_NIA_ALERT1102:
                sNotification.flags = Notification.FLAG_AUTO_CANCEL;
                title = mNotificationContext.getText(R.string.app_name);
                pendingIntent = makeNiaStartNotificationPendingIntent(downloadIcon);
                break;
            case NOTIFICATION_USERMODE_VISIBLE:
            case NOTIFICATION_USERMODE_INTERACT:
                sNotification.flags = Notification.FLAG_AUTO_CANCEL;
                title = mNotificationContext.getText(R.string.app_name);
                pendingIntent = makeUserModeNotificationPendingIntent(downloadIcon);
                break;
            case NOTIFICATION_NEW_VERSION:
            case NOTIFICATION_DOWNLOAD_COMPLETED:
                sNotification.flags = Notification.FLAG_AUTO_CANCEL;
                title = mNotificationContext.getText(R.string.status_bar_notifications_title);
                pendingIntent = makeNotificationPendingIntent(downloadIcon);
                break;
            default:
                break;
            }
            sNotification.setLatestEventInfo(mNotificationContext, title, notificationText,
                    pendingIntent);
            sNotification.defaults = Notification.DEFAULT_ALL;
        }

        // Send the notification.
        showNotification();
    }

    /**
     * show notification
     *
     * @param null
     * @return null
     */
    private void showNotification() {
        Log.v(TAG.NOTIFICATION, "showNotification, id is " + sNotificationType);
        if (sNotificationManager == null) {
            Log.w(TAG.NOTIFICATION, "showNotification mNotificationManager is null");
            return;
        }
        sNotificationManager.notify(sNotificationType, sNotification);
    }

    public void setFlag(int flag) {
        if (sNotification != null) {
            sNotification.flags = flag;
        }

    }

    PersistentContext.FumoUpdateObserver mObserver = new PersistentContext.FumoUpdateObserver() {

        public void syncDLstatus(int status) {
            Log.d(TAG.NOTIFICATION, "[syncDLstatus]:DL status is " + status);
            switch (status) {
            case IDmPersistentValues.STATE_NEW_VERSION_DETECTED:
                Log.d(TAG.NOTIFICATION, "[syncDLstatus]:new version detected!");
                if (DmService.getInstance() != null) {
                    int sessionInitor = DmService.getInstance().getSessionInitor();
                    if (sessionInitor == IDmPersistentValues.SERVER) {
                        Log.d(TAG.NOTIFICATION, "----server inited session, show notif----");
                        showNewVersionNotification();
                    } else if (sessionInitor == IDmPersistentValues.CLIENT_POLLING) {
                        Log.d(TAG.NOTIFICATION, "----client polling session, show notif----");
                        showNewVersionNotification();
                    } else {
                        Log.d(TAG.NOTIFICATION, "----user pull session, don't show notif----");
                    }
                }
                break;
            case IDmPersistentValues.STATE_DOWNLOADING:
                if (sNotificationType != NOTIFICATION_DOWNLOADING) {
                    clearDownloadNotification();
                    sNotificationType = NOTIFICATION_DOWNLOADING;
                }
                Log.d(TAG.NOTIFICATION, "[syncDLstatus]dmStatus is " + sDLStatus);
                if (mDLProgressNotifier == null) {
                    mDLProgressNotifier = new DLProgressNotifier(mNotificationContext,
                            makeNotificationPendingIntent(R.drawable.stat_download_waiting));
                }
                int currentSize = (int) PersistentContext.getInstance(mNotificationContext)
                        .getDownloadedSize();
                int totalSize = (int) PersistentContext.getInstance(mNotificationContext).getSize();
                mDLProgressNotifier.onProgressUpdate(currentSize, totalSize);
                break;
            case IDmPersistentValues.STATE_DL_PKG_COMPLETE:
            case IDmPersistentValues.STATE_VERIFY_NO_STORAGE:
            case IDmPersistentValues.STATE_VERIFY_FAIL:
                DmClient client = DmClient.getVdmClientInstance();
                if (client != null && client.isActive()) {
                    // DmClient is active, we needn't show notification.
                    if (sNotificationType == NOTIFICATION_DOWNLOADING) {
                        clearDownloadNotification();
                        sNotificationType = NOTIFICATION_DOWNLOAD_COMPLETED;
                    }
                } else {
                    showDownloadCompletedNotification();
                }
                break;
            case IDmPersistentValues.STATE_PAUSE_DOWNLOAD:
            case IDmPersistentValues.STATE_NOT_DOWNLOAD:
                clearDownloadNotification();
                break;
            default:
                break;
            }
            Log.d(TAG.NOTIFICATION, "[syncDLstatus]new mNotificationType is " + sNotificationType);
            sDLStatus = status;
        }

        public void syncDmSession(int status) {
            Log.d(TAG.NOTIFICATION, "[syncDmSession]:DM status is " + status);
            switch (status) {
            case IDmPersistentValues.STATE_DM_NIA_ALERT:
                showNiaNotification();
                break;
            case IDmPersistentValues.MSG_NIA_ALERT_1102:
                showInputQueryNotification();
                break;
            case IDmPersistentValues.STATE_DM_USERMODE_VISIBLE:
                showUserModeNotification(NOTIFICATION_USERMODE_VISIBLE);
                break;
            case IDmPersistentValues.STATE_DM_USERMODE_INTERACT:
                showUserModeNotification(NOTIFICATION_USERMODE_INTERACT);
                break;
            case IDmPersistentValues.STATE_DM_NIA_COMPLETE:
                clearDownloadNotification();
                break;
            default:
                break;
            }
            Log.d(TAG.NOTIFICATION, "[syncDmSession]new mNotificationType is " + sNotificationType);
        }
    };

}
