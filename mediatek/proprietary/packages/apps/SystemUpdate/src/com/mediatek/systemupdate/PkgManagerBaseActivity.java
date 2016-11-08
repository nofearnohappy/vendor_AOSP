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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;


import java.util.List;

/**
 *
 * This is a common activity.
 *
 */
class PkgManagerBaseActivity extends Activity {

    private static final String TAG = "SystemUpdate/PkgManagerBase";
    private static final String REBOOT_INTENT =
            "com.mediatek.intent.systemupdate.RebootRecoveryService";
    private static final String WRITE_COMMAND_INTENT =
            "com.mediatek.intent.systemupdate.WriteCommandService";
    private static final String COMMAND_PART2 = "COMMANDPART2";
    private static final String OTA_PATH_IN_RECOVERY_PRE = "/sdcard";

    protected void fillPkgInfo(String strAndroidNum, String strVerNum, long size, String strPath) {

        View viewPkgInfo = findViewById(R.id.pkgInfo);
        if (viewPkgInfo != null) {

            TextView viewAndroidNum = (TextView) findViewById(R.id.textAndroidNum);
            viewAndroidNum.setText(strAndroidNum);
            TextView viewVerNum = (TextView) findViewById(R.id.textVerNum);
            viewVerNum.setText("Version " + strVerNum);
            TextView viewPkgSize = (TextView) findViewById(R.id.textPkgSize);

            if (viewPkgSize != null) {

                if (size == -1) {
                    viewPkgSize.setVisibility(View.GONE);
                } else if (size >= Util.M_SIZE) {
                    viewPkgSize.setText(getString(R.string.size_M, (double) size / Util.M_SIZE));
                } else if (size >= Util.K_SIZE) {
                    viewPkgSize.setText(getString(R.string.size_K, (double) size / Util.K_SIZE));
                } else {
                    viewPkgSize.setText(getString(R.string.size_B, (double) size));
                }

            }

            TextView viewPkgPath = (TextView) findViewById(R.id.textPath);

            if (viewPkgPath != null) {

                if (strPath != null) {
                    strPath = Util.LEFT_TO_RIGHT_EMBEDDING + strPath
                            + Util.POP_DIRECTIONAL_FORMATTING;
                    viewPkgPath.setText(strPath);
                } else {
                    viewPkgPath.setVisibility(View.GONE);
                }

            }

        }

    }

    protected void fillReleaseNotes(List<String> listNotes) {

        TextView viewSummary = (TextView) findViewById(R.id.textNotesSummary);


        TextView viewNotes = (TextView) findViewById(R.id.textNotesDetail);

        if (viewNotes != null) {

            StringBuilder strNotes = new StringBuilder();

            for (String strItem : listNotes) {

                strNotes.append("- ").append(strItem).append("\n");

            }

            viewNotes.setText(strNotes);
        }

    }

    protected void installPackage(String strPkgPath, String strTarVer) {

        InstallPkgThread installThread = new InstallPkgThread(strPkgPath, strTarVer);

        installThread.start();
    }

    protected boolean checkUpgradePackage() {
        return true;
    }

    protected void requeryPackages() {
        Log.d("@M_" + TAG, "requery Packages");
        DownloadInfo.getInstance(getApplicationContext()).resetDownloadInfo();
        Intent i = new Intent(this, MainEntry.class);
        this.startActivity(i);
        this.finish();
    }

    protected void notifyUserInstall() {
        return;
    }

    private boolean setInstallInfo(String strPkgPath, String strTarVer) {
        Log.i("@M_" + TAG, "onSetRebootRecoveryFlag");

        try {
            IBinder binder = ServiceManager.getService("GoogleOtaBinder");
            SystemUpdateBinder agent = SystemUpdateBinder.Stub.asInterface(binder);

            if (agent == null) {
                Log.e("@M_" + TAG, "agent is null");
                return false;
            }

            if (Util.isEmmcSupport()) {
                if (!agent.clearUpdateResult()) {
                    Log.e("@M_" + TAG, "clearUpdateResult() false");
                    return false;
                }
            }

            DownloadInfo dlInfo = DownloadInfo.getInstance(getApplicationContext());
            dlInfo.setTargetVer(strTarVer);

            Log.i("@M_" + TAG, "setTargetVer");

            if (!agent.setRebootFlag()) {
                Log.e("@M_" + TAG, "setRebootFlag() false");
                return false;
            }

            Log.i("@M_" + TAG, "setRebootFlag");

            dlInfo.setUpgradeStartedState(true);

            dlInfo.resetDownloadInfo();

            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.mediatek.systemupdate.sysoper",
                    "com.mediatek.systemupdate.sysoper.WriteCommandService"));
            intent.putExtra(COMMAND_PART2, OTA_PATH_IN_RECOVERY_PRE + strPkgPath);
            startService(intent);
            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     *
     * create a new thread to install package.
     *
     */
    class InstallPkgThread extends Thread {

        private String mPkgPath;
        private String mTarVer;

        /**
         *
         * @param strPkgPath
         * @param strTarVer
         */
        public InstallPkgThread(String strPkgPath, String strTarVer) {
            mPkgPath = strPkgPath;
            mTarVer = strTarVer;
        }

        /**
         * Main executing function of this thread.
         */
        public void run() {
            if (checkUpgradePackage() && setInstallInfo(mPkgPath, mTarVer)) {
                notifyUserInstall();
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.mediatek.systemupdate.sysoper",
                        "com.mediatek.systemupdate.sysoper.RebootRecoveryService"));
                startService(intent);
            } else {
                return;
            }
        }

    }

}
