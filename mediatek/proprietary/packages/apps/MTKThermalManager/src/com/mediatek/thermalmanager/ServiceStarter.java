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

package com.mediatek.thermalmanager;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.util.Slog;

import java.io.IOException;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;

public class ServiceStarter extends BroadcastReceiver {
	
	static final String LOG_TAG = "thermalmanager.ServiceStarter";
	static final String BOOTUP = "android.intent.action.BOOT_COMPLETED";
	static final String THERMAL_WARNING = "android.intent.action.THERMAL_WARNING";

	@Override
	public void onReceive(Context contxt, Intent intent) {
		
		Log.d("@M_" + LOG_TAG, "onReceiver()\n");
		// This BroadcastReceiver is registered in AndroidManifest.xml. 
		// Its onReceive will be invoked after boot? 
		
	    if (intent.getAction().equals(BOOTUP)) {
	        Log.d("@M_" + LOG_TAG, "onReceiver() " + BOOTUP + "\n");
	        // First time starting the service get its onCreate() invoked.
	        //contxt.startService(new Intent(contxt, ThermalEventRepeater.class));
	        
	        File a = new File("/proc/driver/thermal/clsd_rst");
            if (a.exists())
            {
                try {
                    FileOutputStream fs = new FileOutputStream(a);
                    DataOutputStream ds = new DataOutputStream(fs);
                    ds.write(new String("1").getBytes());
                    ds.flush();
                    ds.close();
                    fs.close();
                }
                catch (IOException ex) {
                    Log.e("@M_" + LOG_TAG, "onReceiver() write mtk_cl_sd_rst exception\n");
                }
            }
	      } else if (intent.getAction().equals(THERMAL_WARNING)) {
	          Log.d("@M_" + LOG_TAG, "onReceiver() " + THERMAL_WARNING + "\n");
	          Intent activateIntent = new Intent();
	          activateIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
	          //intent.setClass(this, ShutDownAlertDialogActivity.class);
	          activateIntent.setComponent(new ComponentName("com.mediatek.thermalmanager",
                                "com.mediatek.thermalmanager.ShutDownAlertDialogActivity"));
            //this.startActivity(intent);
            contxt.startActivity(activateIntent);
        }
	}

}
