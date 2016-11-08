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

package com.mediatek.filemanager;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.view.MenuItem;

import com.mediatek.filemanager.AlertDialogFragment.ChoiceDialogFragment;
import com.mediatek.filemanager.FileManagerOperationActivity.HeavyOperationListener;
import com.mediatek.filemanager.service.FileManagerService;
import com.mediatek.filemanager.tests.utils.TestUtils;
import com.mediatek.filemanager.utils.LogUtils;

/**
 * The is a help class used for testing protect method or variable.
 */
public class ActivityTestHelper {
    private final static String TAG = "ActivityTestHelper";

    public static String getCurrentPath(AbsBaseActivity baseActivity) {
        return baseActivity.mCurrentPath;
    }

    public static void waitingForServiceConnected(AbsBaseActivity baseActivity) {
        LogUtils.i(TAG, "------waitingForServiceConnected----------");
        while (baseActivity.mService == null) {
            TestUtils.sleep(100);
        }
        while (baseActivity.mService.isBusy(baseActivity.getClass().getName())) {
            TestUtils.sleep(100);
        }
    }

    public static boolean isServiceBusy(AbsBaseActivity activity) {
        return activity.mService.isBusy(activity.getClass().getName());
    }

    public static FileManagerService getServiceInstance(AbsBaseActivity activity) {
        return activity.mService;
    }

    public static void waitingForService(AbsBaseActivity activity) {
        do {
            TestUtils.sleep(100);
        } while (ActivityTestHelper.isServiceBusy(activity));

        TestUtils.sleep(1000);
    }

    public static int getSortType(FileManagerOperationActivity activity) {
        return activity.mSortType;
    }

    public static String getInitPath(AbsBaseActivity activity) {
        return activity.initCurrentFileInfo();
    }

    public static Dialog getDialog(AbsBaseActivity baseActivity, String tag) {
        TestUtils.sleep(1000);
        int time = 0;
        DialogFragment dialogFragment = null;
        do {
            dialogFragment = (DialogFragment) baseActivity.getFragmentManager().findFragmentByTag(
                    tag);
            TestUtils.sleep(100);
            time++;
            LogUtils.d(TAG, "waiting to get dialog");
        } while ((dialogFragment == null) && (time < 100));
        return (AlertDialog) dialogFragment.getDialog();
    }

    public static Dialog getDetailDialog(FileManagerOperationActivity baseActivity) {
        return getDialog(baseActivity,
                FileManagerOperationActivity.DetailInfoListener.DETAIL_DIALOG_TAG);
    }

    public static Dialog getChoiceDialog(FileManagerOperationActivity baseActivity) {
        return getDialog(baseActivity, ChoiceDialogFragment.CHOICE_DIALOG_TAG);
    }

    public static Dialog getProgressDialog(FileManagerOperationActivity baseActivity) {
        return getDialog(baseActivity, HeavyOperationListener.HEAVY_DIALOG_TAG);
    }

    public static void onActionModeActionItem(FileManagerOperationActivity activity, MenuItem menuItem) {
        activity.mActionModeCallBack.onActionItemClicked(activity.getActionMode(), menuItem);
    }

    public static void onActionModePopupMenu(FileManagerOperationActivity activity, MenuItem menuItem) {
        activity.mActionModeCallBack.onMenuItemClick(menuItem);
    }

    public static FileInfoAdapter getAdapter(FileManagerOperationActivity baseActivity) {
        return baseActivity.mAdapter;
    }

    public static void onBackPressed(FileManagerOperationActivity baseActivity) {
        baseActivity.onBackPressed();
    }
}