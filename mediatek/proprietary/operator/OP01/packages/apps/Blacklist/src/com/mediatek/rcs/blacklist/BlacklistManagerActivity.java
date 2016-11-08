/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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

package com.mediatek.rcs.blacklist;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


/**
 * BlacklistManagerActivity.
 * Control fragments
 */
public class BlacklistManagerActivity extends Activity {

    private static final String TAG = "Blacklist";
    protected static final int PLEASEWAIT_DIALOG = 100;
    ProgressDialog mPlzWaitDlg;

    static final String PROCESSING_DIALOG_EXTRA = "BlacklistManagerActivity:process_dialog";
    private static final String LOW_MEMORY_EXTRA = "BlacklistManagerActivity:low_memory";

    private boolean mProgressFlag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);

        setContentView(R.layout.activity_blacklistmanager);

        if (savedInstanceState != null) {
            mProgressFlag = savedInstanceState.getBoolean(PROCESSING_DIALOG_EXTRA);
            log("savedInstanceState != null, mProgressFlag:" + mProgressFlag);
        }

        BlacklistFragment listFragment = new BlacklistFragment();
        getFragmentManager().beginTransaction()
            .replace(R.id.blacklistActivity, listFragment, BlacklistFragment.FRAGMENT_TAG)
            //.commit();
            .commitAllowingStateLoss();

        log("onCreate");
    }

    @Override
    public void onResume() {
        super.onResume();
        log("onResume");

        if (mProgressFlag) {
            log("dismiss dialog");
            dismissDialogSafely(PLEASEWAIT_DIALOG);
            mProgressFlag = false;
        } else if (mPlzWaitDlg != null && mPlzWaitDlg.isShowing()) {
            log("update message of ProgressDialog");
            mPlzWaitDlg.setMessage(getResources().getString(R.string.please_wait));
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == PLEASEWAIT_DIALOG) {
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage(getResources().getString(R.string.please_wait));
            dialog.setCancelable(false);
            dialog.setIndeterminate(true);

            return dialog;
        }

        return null;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        if (id == PLEASEWAIT_DIALOG) {
            log("onPrepareDialog");
            mPlzWaitDlg = (ProgressDialog) dialog;
        }

        super.onPrepareDialog(id, dialog, args);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        log("onSaveInstanceState");
        if (mPlzWaitDlg != null && mPlzWaitDlg.isShowing()) {
            log("onSaveInstanceState progressing");
            outState.putBoolean(BlacklistManagerActivity.PROCESSING_DIALOG_EXTRA, true);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        log("onLowMemory");
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        log("onTrimMemory: " + level);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    protected void dismissDialogSafely(int id) {
        try {
            if (id == PLEASEWAIT_DIALOG) {
                mPlzWaitDlg = null;
            }
            log("dismissDialogSafely");

            dismissDialog(id);
        } catch (IllegalArgumentException e) {
            log("dimissDialog exception");
        }
    }

    private void log(String message) {
        Log.d(TAG, "[" + getClass().getSimpleName() + "] " + message);
    }
}
