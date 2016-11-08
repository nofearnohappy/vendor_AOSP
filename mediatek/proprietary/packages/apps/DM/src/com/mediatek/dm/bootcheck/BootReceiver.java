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

package com.mediatek.dm.bootcheck;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.DiskInfo;
import com.mediatek.dm.DmConst;
import com.mediatek.dm.DmService;
import com.mediatek.dm.data.IDmPersistentValues;
import com.mediatek.dm.data.PersistentContext;
import com.mediatek.dm.option.Options;
import com.mediatek.dm.polling.PollingScheduler;
import com.mediatek.dm.util.DmThreadPool;

import java.io.File;
import java.util.concurrent.ExecutorService;

/**
 * @author MTK80800 Receive boot_complete to Check FOTA result; check unprocessed NIA;check polling
 *         time up; check LAWMO wipe status
 *
 */

public class BootReceiver extends BroadcastReceiver {
    private static final String CLASS_TAG = "DM/BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(CLASS_TAG, "onReceive: " + intent);
        if (intent != null && context != null) {
            String action = intent.getAction();
            if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
                checkReboot(context);
                checkLawmoWipe(context);
            }
        }
    }

    private void checkLawmoWipe(Context context) {
        File file = new File(DmConst.PathName.WIPE_FILE);
        if (file.exists()) {
            // temporary disable format and factory reset till new method is found
            //Intent eraseIntent = new Intent(ExternalStorageFormatter.FORMAT_AND_FACTORY_RESET);
            //eraseIntent.setComponent(ExternalStorageFormatter.COMPONENT_NAME);
            //context.startService(eraseIntent);
            // Format Phone Memory
            StorageManager mStorage = context.getSystemService(StorageManager.class);
            for (VolumeInfo vol : mStorage.getVolumes()) {
                String diskId = vol.getDiskId();
                if (diskId != null && diskId.equals("disk:179,0")) {
                    Log.i(CLASS_TAG, "Storage manager notified to format phone memory");
                mStorage.format(vol.getId());
              }
            }
            // Perform Factory reset
            context.sendBroadcast(new Intent("android.intent.action.MASTER_CLEAR"));
            Log.i(CLASS_TAG, "Broadcast sent to perform factory reset");
            // context.sendBroadcast(new Intent("android.intent.action.MASTER_CLEAR"));
        }
    }

    private void checkReboot(Context context) {
        ExecutorService exec = DmThreadPool.getInstance();

        if (exec != null) {
            exec.execute(new CheckReboot(context));
        }

    }

    public static class CheckReboot implements Runnable {
        private Context mContext;

        public CheckReboot(Context context) {
            mContext = context;
        }

        public void run() {
            Bundle bundle = new Bundle();

            boolean updated = checkUpdateStatus(mContext);
            if (updated) {
                Log.i(CLASS_TAG, "CheckReboot Start boot service, this is update reboot");
                bundle.putBoolean(DmService.INTENT_EXTRA_UPDATE, true);
            }

            boolean niaExist = checkUnproceedNia();
            if (niaExist) {
                Log.i(CLASS_TAG, "CheckReboot Start boot service really, this is nia exist");
                bundle.putBoolean(DmService.INTENT_EXTRA_NIA, true);
            }

            boolean needPolling = checkPolling(mContext);
            if (needPolling) {
                Log.i(CLASS_TAG, "CheckReboot start boot service, polling timeup");
                bundle.putBoolean(DmService.INTENT_EXTRA_POLLING, true);
            }

            if (updated || niaExist || needPolling) {
                Log.d(CLASS_TAG, "+++ starting service...");
                Intent intent = new Intent(mContext, BootService.class);
                intent.putExtras(bundle);
                mContext.startService(intent);
            } else {
                Log.d(CLASS_TAG, "--- no need to start service.");
            }
        }

        public static boolean checkUpdateStatus(Context context) {
            Log.i(CLASS_TAG, "checkUpdateStatus enter");
            int status = PersistentContext.getInstance(context).getDLSessionStatus();
            Log.v(CLASS_TAG, "checkUpdateStatus status = " + status);
            return IDmPersistentValues.STATE_UPDATE_RUNNING == status
                    || IDmPersistentValues.STATE_UPDATE_COMPLETE == status;

        }

        public static boolean checkPolling(Context context) {
            if (Options.USE_SCHEDULED_POLLING) {
                Log.d(CLASS_TAG, "scheduled polling, check timeup");
                return PollingScheduler.getInstance(context).checkTimeup();

            } else {
                Log.d(CLASS_TAG, "NOT use scheduled polling");
                return false;
            }
        }

        public static boolean checkUnproceedNia() {
            Log.i(CLASS_TAG, "checkUnproceedNia enter");
            boolean ret = false;
            String niaFolder = DmConst.PathName.NIA_FILE;
            File folder = new File(niaFolder);
            if (!folder.exists()) {
                Log.w(CLASS_TAG, "CheckNia the nia dir is noet exist");
                return ret;
            }

            String[] fileExist = folder.list();
            if (fileExist == null || fileExist.length <= 0) {
                Log.w(CLASS_TAG, "CheckNia there is no unproceed message");
                return ret;
            }
            ret = true;

            return ret;
        }
    }
}
