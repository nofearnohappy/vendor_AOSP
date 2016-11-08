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

import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

import com.mediatek.dm.DmConst.TAG;
import com.mediatek.dm.DmService;
import com.mediatek.dm.R;
import com.mediatek.dm.util.DialogFactory;
import com.redbend.vdm.VdmException;
import com.redbend.vdm.scomo.VdmScomoDp;

public class DmScomoConfirmActivity extends Activity implements OnClickListener {
    private static final String CLASS_TAG = TAG.SCOMO + "/ConfirmActivity";
    static final String EXTRA_ACTION = "action";

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        ((NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE))
                .cancel(DmScomoNotification.NOTIFICATION);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        int action = getIntent().getIntExtra(EXTRA_ACTION, -1);
        Log.d(CLASS_TAG, "get from intent extra, action is " + action);
        switch(action) {
        case DmScomoState.CONFIRM_DOWNLOAD:
        case DmScomoState.INSTALL_FAILED:
        case DmScomoState.DOWNLOAD_FAILED:
        case DmScomoState.INSTALL_OK:
        case DmScomoState.GENERIC_ERROR:
            showDialog(action);
            break;
        default:
            finish();
            break;
        }
    }

    @Override
    protected Dialog onCreateDialog(final int id) {
        Dialog dialog = null;
        switch (id) {
        case DmScomoState.CONFIRM_DOWNLOAD:
            dialog = onConfirmDownload();
            break;
        case DmScomoState.CONFIRM_INSTALL:
            dialog = checkPackageError();
            if (dialog == null) {
                dialog = checkSpaceError();
            }
            if (dialog == null) {
                dialog = onConfirmInstall();
            }
            break;
        case DmScomoState.INSTALL_FAILED:
            dialog = onInstallFailed();
            break;
        case DmScomoState.DOWNLOAD_FAILED:
            dialog = onDownloadFailed();
            break;
        case DmScomoState.INSTALL_OK:
            dialog = onInstallOk();
            break;
        case DmScomoState.GENERIC_ERROR:
            dialog = onGenericError();
            break;
        default:
            break;
        }
        return dialog;
    }

    private Dialog onGenericError() {
        Log.v(CLASS_TAG, "onGenericError");
        return DialogFactory.newAlert(this).setCancelable(false)
                .setTitle(R.string.software_update)
                .setMessage(R.string.unknown_error)
                .setNeutralButton(R.string.ok, this)
                .create();
    }

    private Dialog onConfirmInstall() {
        Log.v(CLASS_TAG, " onConfirmInstall");
        DmScomoState scomoState = DmScomoState.getInstance(DmScomoConfirmActivity.this);
        boolean packageInstalled = DmScomoPackageManager.getInstance()
                .isPackageInstalled(scomoState.getPackageName());

        StringBuilder sb = new StringBuilder()
                .append(getString(R.string.download_complete)).append("\n")
                .append(getString(R.string.name)).append(scomoState.getName()).append("\n")
                .append(getString(R.string.version)).append(scomoState.getVersion()).append("\n")
                .append(getString(R.string.size)).append(scomoState.getSize() / 1024).append("KB\n");

        CharSequence text = "";
        if (packageInstalled) {
            sb.append(getString(R.string.confirm_upgrade_msg));
            text = getText(R.string.upgrade);
        } else {
            sb.append(getString(R.string.confirm_install_msg));
            text = getText(R.string.install);
        }

        return DialogFactory.newAlert(this).setCancelable(false)
                .setTitle(R.string.software_update)
                .setMessage(sb.toString())
                .setPositiveButton(text, new OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        Log.v(CLASS_TAG, "positive button clicked, start to install");
                        VdmScomoDp dp = DmScomoState.getInstance(DmScomoConfirmActivity.this).mCurrentDp;
                        try {
                            dp.executeInstall();
                        } catch (VdmException e) {
                            Log.e(CLASS_TAG, "onConfirmInstall, executeInstall error!!!");
                            e.printStackTrace();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, this)
                .create();
    }

    private Dialog checkPackageError() {
        Log.v(CLASS_TAG, "checkPackageError");
        boolean ret = true;
        DmScomoState scomoState = DmScomoState.getInstance(DmScomoConfirmActivity.this);
        DmScomoPackageManager.ScomoPackageInfo info = DmScomoPackageManager.getInstance()
                .getMinimalPackageInfo(scomoState.mArchiveFilePath);
        ret = (info != null);
        if (!ret) {
            Log.w(CLASS_TAG, "checkPackageError: error! ScomoPackageInfo is null");
            return DialogFactory.newAlert(this).setCancelable(false)
                    .setTitle(R.string.software_update)
                    .setMessage(R.string.package_format_error)
                    .setNeutralButton(R.string.ok, this)
                    .create();
        }
        return null;
    }

    private Dialog checkSpaceError() {
        Log.v(CLASS_TAG, " checkSpaceError");
        boolean ret = true;
        DmScomoState scomoState = DmScomoState.getInstance(DmScomoConfirmActivity.this);
        String archiveFilePath = scomoState.mArchiveFilePath;
        ret = DmScomoPackageManager.getInstance().checkSpace(archiveFilePath);
        if (!ret) {
            Log.w(CLASS_TAG, "checkSpaceError: error! checkSpace fail");
            return DialogFactory.newAlert(this).setCancelable(false)
                    .setTitle(R.string.software_update)
                    .setMessage(R.string.insufficent_storage)
                    .setNeutralButton(R.string.ok, this)
                    .create();
        } else {
            return null;
        }
    }

    private Dialog onDownloadFailed() {
        Log.v(CLASS_TAG, "onDownloadFailed");
        return DialogFactory.newAlert(this).setCancelable(false)
                .setTitle(R.string.software_update)
                .setMessage(R.string.download_failed)
                .setNeutralButton(R.string.ok, this)
                .create();
    }

    private Dialog onInstallOk() {
        Log.v(CLASS_TAG, "onInstallOk");
        return DialogFactory.newAlert(this).setCancelable(false)
                .setTitle(R.string.software_update)
                .setMessage(R.string.install_complete)
                .setNeutralButton(R.string.ok, this)
                .create();
    }

    private Dialog onInstallFailed() {
        Log.v(CLASS_TAG, "onInstallFailed");
        return DialogFactory.newAlert(this).setCancelable(false)
                .setTitle(R.string.software_update)
                .setMessage(R.string.install_failed)
                .setNeutralButton(R.string.ok, this)
                .create();
    }

    private Dialog onConfirmDownload() {
        Log.v(CLASS_TAG, "onConfirmDownload");
        return DialogFactory.newAlert(this).setCancelable(false)
                .setTitle(R.string.software_update)
                .setMessage(R.string.confirm_download_msg)
                .setPositiveButton(R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        Log.v(CLASS_TAG, "positive button clicked, start to download");
                        DmService.getInstance().startDlScomoPkg();
                    }
                })
                .setNegativeButton(R.string.cancel, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
        case DialogInterface.BUTTON_POSITIVE:
            Log.w(CLASS_TAG, "positive button should not be clicked");
            break;
        case DialogInterface.BUTTON_NEGATIVE:
            Log.v(CLASS_TAG, "negative button clicked,reset scomo state & cancelDLScomoPkg");
            DmService.getInstance().pauseDlScomoPkg(); // cancel DL session
            DmService.getInstance().cancelDlScomoPkg(); // trigger report session
            // fallthru
        case DialogInterface.BUTTON_NEUTRAL:
            Log.v(CLASS_TAG, "neutral button clicked, reset scomo state");
            DmScomoState scomoState = DmScomoState.getInstance(DmScomoConfirmActivity.this);
            scomoState.mState = DmScomoState.IDLE;
            DmScomoState.store(DmScomoConfirmActivity.this);
            break;
        default:
            break;
        }
        DmScomoConfirmActivity.this.finish();
    }
}
