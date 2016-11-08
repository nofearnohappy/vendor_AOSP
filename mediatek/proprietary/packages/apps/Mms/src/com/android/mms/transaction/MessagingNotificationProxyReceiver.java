/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
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

package com.android.mms.transaction;

import java.util.List;
import com.android.mms.ui.ConversationList;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;

import com.android.mms.MmsConfig;
import com.android.mms.ui.ComposeMessageActivity;
import com.android.mms.ui.MessageUtils;

import com.android.mms.util.MmsLog;
import com.android.mms.ui.BootActivity;
import com.mediatek.mms.folder.ui.FolderModeSmsViewer;
import com.mediatek.mms.folder.util.FolderModeUtils;

public class MessagingNotificationProxyReceiver extends BroadcastReceiver {
    private static final String TAG = "MessagingNotificationProxyReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        processIntent(context, intent);
    }
    private void processIntent(Context context, Intent intent) {
        Intent toProcessIntent = null;
        int thread_count = intent.getIntExtra("thread_count", 0);
        long thread_id = intent.getLongExtra("thread_id", 0);
        int message_count = intent.getIntExtra("message_count", 0);
        boolean isSms = intent.getBooleanExtra("isSms", false);
        MmsLog.d(TAG, "processIntent: thread_count = " + thread_count +
                         ", thread_id = " + thread_id +
                         ", message_count = " + message_count +
                         "isSms = " + isSms);

        final boolean isFailed = intent.getBooleanExtra("isFailed", false);
        final boolean isDownloadFailed = intent.getBooleanExtra("failed_download_flag", false);
        final boolean isSendFailed = intent.getBooleanExtra("undelivered_flag", false);
        MmsLog.d(TAG, "processIntent: isFailed = " + isFailed +
                         ", isDownloadFailed = " + isDownloadFailed +
                         ", isSendFailed = " + isSendFailed);

        Uri uri = intent.getData();
        ComposeMessageActivity composer
                = (ComposeMessageActivity) ComposeMessageActivity.getComposeContext();
        if (composer != null && handleTopActivity(context, composer, intent)) {
            return;
        }
        boolean isForwardMode = (composer != null
                && composer.getIntent().getBooleanExtra("forwarded_message", false));
        if (FolderModeUtils.getMmsDirMode()) {
            //Folder Mode
            if (thread_count == 1 && isSms && message_count == 1) {
                //folderSmsViewer
                toProcessIntent = new Intent(context, FolderModeSmsViewer.class);
                toProcessIntent.putExtra("msg_type", 1);
                toProcessIntent.setFlags(0);//clear the flag.
                toProcessIntent.setData(uri);
                toProcessIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            } else {
                if (composer != null && !composer.isFinishing()
                        && composer.isWorthSaving() && !composer.hasValidRecipient()) {
                    //composer to notice
                    toProcessIntent = new Intent(context, ComposeMessageActivity.class);
                    if (isForwardMode) {
                        toProcessIntent.setClassName(context,
                                "com.android.mms.ui.ForwardMessageActivity");
                    }
                    toProcessIntent.putExtra("notification", true);
                    toProcessIntent.putExtra("message_count", message_count);
                    if (isFailed && isDownloadFailed) {
                        toProcessIntent.putExtra("failed_download_flag", true);
                    } else if (isFailed && isSendFailed) {
                        toProcessIntent.putExtra("undelivered_flag", true);
                    }
                    toProcessIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                } else {
                    toProcessIntent = new Intent(Intent.ACTION_MAIN);
                    toProcessIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    if (isFailed && isSendFailed) {
                        toProcessIntent.putExtra("floderview_key", 1); // need send inbox
                    } else {
                        toProcessIntent.putExtra("floderview_key", 0); // need show inbox
                    }
                    toProcessIntent.setClassName("com.android.mms",
                            "com.mediatek.mms.folder.ui.FolderViewList");
                }
            }
        } else {
            //ConversationMode
            if (composer != null && !composer.isFinishing()
                    && composer.isWorthSaving() && !composer.hasValidRecipient()) {
                if (thread_count > 1) {
                    toProcessIntent = new Intent(context, ComposeMessageActivity.class);
                    toProcessIntent.putExtra("notification", true);
                    toProcessIntent.putExtra("thread_count", thread_count);
                } else {
                    toProcessIntent = ComposeMessageActivity.createIntent(context, thread_id);
                }
                if (isForwardMode) {
                    toProcessIntent.setClassName(context, "com.android.mms.ui.ForwardMessageActivity");
                }
                if (isFailed && isDownloadFailed) {
                    toProcessIntent.putExtra("failed_download_flag", true);
                } else if (isFailed && isSendFailed) {
                    toProcessIntent.putExtra("undelivered_flag", true);
                }
                toProcessIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            } else {
                if (thread_count > 1) {
                    //toProcessIntent = new Intent(Intent.ACTION_MAIN);
                    toProcessIntent = new Intent(context, BootActivity.class);
                    toProcessIntent.putExtra("thread_count", thread_count);
                    toProcessIntent.setType("vnd.android-dir/mms-sms");
                    toProcessIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                } else {
                    toProcessIntent = createIntentToComposer(context, thread_id);
                    toProcessIntent.putExtra("finish", true);
                    if (isFailed && isDownloadFailed) {
                        toProcessIntent.putExtra("failed_download_flag", true);
                    } else if (isFailed && isSendFailed) {
                        toProcessIntent.putExtra("undelivered_flag", true);
                    }
                }
            }
        }
        toProcessIntent.putExtra("notification", true);
        context.startActivity(toProcessIntent);
    }

    private boolean handleTopActivity(final Context context, ComposeMessageActivity composer, final Intent intent) {
        boolean handled = false;
        if (composer == null) {
            return handled;
        }
        final int thread_count = intent.getIntExtra("thread_count", 0);
        final long thread_id = intent.getLongExtra("thread_id", 0);
        final int message_count = intent.getIntExtra("message_count", 0);
        final boolean isSms = intent.getBooleanExtra("isSms", false);

        final boolean isFailed = intent.getBooleanExtra("isFailed", false);
        final boolean isDownloadFailed = intent.getBooleanExtra("failed_download_flag", false);
        final boolean isSendFailed = intent.getBooleanExtra("undelivered_flag", false);

        if (isSms && message_count == 1) {
            return handled;
        }
        final Uri uri = intent.getData();
        if (!composer.isFinishing()
                && composer.isWorthSaving() && !composer.hasValidRecipient()) {
            int composer_task_id = composer.getTaskId();
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(10);
            ActivityManager.RunningTaskInfo composerTaskInfor = null;
            if (list != null) {
                int size = list.size();
                for (int index = 0; index < size; index ++) {
                    if (list.get(index).id == composer_task_id) {
                        composerTaskInfor = list.get(index);
                        break;
                    }
                }
            }
            if (composerTaskInfor != null) {
                String topActivityClassName = composerTaskInfor.topActivity.getClassName();
                MmsLog.d(TAG, "topActivityClassName: " + topActivityClassName);
                if (topActivityClassName.contains("com.android.mms.ui.ComposeMessageActivity") ||
                        topActivityClassName.contains("com.android.mms.ui.ForwardMessageActivity")) {
                    am.moveTaskToFront(composer_task_id, 0);
                    MessageUtils.showDiscardDraftConfirmDialog(composer, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            Intent toIntent = null;
                            if (FolderModeUtils.getMmsDirMode()) {
                                //folder mode, single sms case don't process in this function
                                if (isFailed && isSendFailed) {
                                    toIntent = createIntentToFolderViewList(context, 1); 
                                } else {
                                    toIntent = createIntentToFolderViewList(context, 0);
                                }
                            } else {
                                //conversationMode
                                if (thread_count > 1) {
                                    toIntent = createIntentToConversationList(context);
                                } else {
                                    toIntent = createIntentToComposer(context, thread_id);
                                    if(isDownloadFailed) {
                                        toIntent.putExtra("failed_download_flag", true);
                                    } else if(isSendFailed) {
                                        toIntent.putExtra("undelivered_flag", true);
                                    }
                                }
                            }
                            if (toIntent != null) {
                                context.startActivity(toIntent);
                            }
                        }
                    });
                    handled = true;
                }
            }
        }
        return handled;
    }

    private Intent createIntentToFolderViewList(Context context, int boxId) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("floderview_key", boxId);
        intent.setClassName("com.android.mms", "com.mediatek.mms.folder.ui.FolderViewList");
        return intent;
    }

    private Intent createIntentToConversationList(Context context) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setType("vnd.android-dir/mms-sms");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    private Intent createIntentToComposer(Context context, long threadId) {
        Intent  intent = ComposeMessageActivity.createIntent(context, threadId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

}
