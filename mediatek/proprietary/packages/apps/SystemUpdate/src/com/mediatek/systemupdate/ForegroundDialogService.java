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

package com.mediatek.systemupdate;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;


/**
 * The activity to reminder install.
 *
 * @author mtk80357
 *
 */
public class ForegroundDialogService extends Service {
    private static final String TAG = "Systemupdate/ForegroundDialogService";
    static final String DLG_ID = "dlg_id";
    static final String UPGRADE_RESULT_KEY = "upgrade_result";
    static final int DIALOG_INSTALL_REMINDER = 0;
    static final int DIALOG_UPDATE_RESULT = 1;

    private AlertDialog mInstallReminderDlg = null;

    private boolean mErrorCode;

    @Override
    public void onCreate() {

        super.onCreate();

    }

    @Override
    public void onDestroy() {
        Log.d("@M_" + TAG, "Service onDestroy");

        if ((mInstallReminderDlg != null) && (mInstallReminderDlg.isShowing())) {
            Log.d("@M_" + TAG, "mInstallReminderDlg dismiss");
            mInstallReminderDlg.dismiss();
            mInstallReminderDlg = null;
        }
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("@M_" + TAG, "onStartCommand");

        if (intent == null) {
            Log.e("@M_" + TAG, "Intent is null!!");
            return super.onStartCommand(intent, flags, startId);
        }

        int dlgId = intent.getIntExtra(DLG_ID, -1);

        if (dlgId == DIALOG_INSTALL_REMINDER) {
            showForeGroundDlg(DIALOG_INSTALL_REMINDER);
        } else if (dlgId == DIALOG_UPDATE_RESULT) {
            mErrorCode = intent.getBooleanExtra(UPGRADE_RESULT_KEY, false);
            Log.i("@M_" + TAG, "mErrorCode = " + mErrorCode);
            showForeGroundDlg(dlgId);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    private void showForeGroundDlg(int id) {

        Builder builder = new AlertDialog.Builder(ForegroundDialogService.this);
        // AlertDialog foreGroundDlg = builder.create();
        switch (id) {
        case DIALOG_INSTALL_REMINDER:
            Log.i("@M_" + TAG, "show dlg of DIALOG_INSTALL_REMINDER");
            LayoutInflater flater = LayoutInflater.from(this);
            if (flater == null) {
                Log.e("@M_" + TAG, "flater is null!!");

                return;
            }

            View installReminderView = flater.inflate(R.layout.install_reminder_dlg, null);

            builder.setTitle(R.string.install_sd_title)
                        .setPositiveButton(R.string.install_reminder_now,
                                new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            Log.i("@M_" + TAG, "install now");
                            Intent intent = new Intent(Util.Action.ACTION_OTA_MANAGER);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            mInstallReminderDlg = null;
                            stopSelf();
                        }
                                })
                        .setNegativeButton(R.string.install_reminder_not_now,
                                new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            mInstallReminderDlg = null;
                            stopSelf();
                        }
                    }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                        public void onCancel(DialogInterface dialog) {
                            mInstallReminderDlg = null;
                            stopSelf();
                        }
                    }).setView(installReminderView);

            mInstallReminderDlg = builder.create();
            if (mInstallReminderDlg != null) {
                Window win = mInstallReminderDlg.getWindow();
                win.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                mInstallReminderDlg.show();
            }

            break;

        case DIALOG_UPDATE_RESULT:
            Log.i("@M_" + TAG, "show dlg of DIALOG_UPDATE_RESULT");
            DownloadInfo downloadStatus = DownloadInfo.getInstance(ForegroundDialogService.this);

            String message = downloadStatus.getTargetVer().isEmpty() ? downloadStatus
                      .getVerNum() : downloadStatus.getTargetVer();
            builder.setTitle(R.string.app_name)
                    .setMessage(
                            getString(mErrorCode ? R.string.updateSuccess : R.string.updateFailed,
                                    message))
                    .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            stopSelf();
                        }
                    }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                        public void onCancel(DialogInterface dialog) {

                            stopSelf();
                        }
                    });
            AlertDialog foreGroundDlg = builder.create();
            if (foreGroundDlg != null) {
                Window win = foreGroundDlg.getWindow();
                win.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                foreGroundDlg.show();
            }

            break;
        default:
            break;

        }

    }

}
