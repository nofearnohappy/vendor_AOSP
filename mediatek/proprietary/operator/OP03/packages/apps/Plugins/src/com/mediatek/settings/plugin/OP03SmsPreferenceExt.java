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
package com.mediatek.settings.plugin;

import android.content.ComponentName;

import android.content.Context;
import android.os.Bundle;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.preference.ListPreference;
import android.util.Log;
import com.mediatek.common.PluginImpl;
import com.mediatek.settings.ext.DefaultSmsPreferenceExt;
import com.android.internal.telephony.SmsApplication;


@PluginImpl(interfaceName = "com.mediatek.settings.ext.ISmsPreferenceExt")
public class OP03SmsPreferenceExt extends DefaultSmsPreferenceExt {

    public static final String SMS_UPDATE_RECEIVED = "com.action.update.smsapplication";
    public static final String SMS_UPDATE_CANCELED = "com.action.updatecancel.smsapplication";
    private String mNewPackageName = null;
    private SmsApplicationBroadcastReceiver mSmsApplicationBroadcastReceiver;
    private boolean setSummary = true;

    private static final String TAG = "OP03SmsPreferenceExt";

    ListPreference mSmsApplicationPreference;
    Context mcontext;
    /**
     * If user set 3rd party xMS as default, set a notification to notify user it is not
     * the manufacturer one.
     * And give out the dialog again to switch back.
     * */
    public void createBroadcastReceiver(Context context, ListPreference listPreference) {
     Log.i(TAG, "createBroadcastReceiver\n");
       mSmsApplicationBroadcastReceiver = new SmsApplicationBroadcastReceiver();
       IntentFilter intentFilter = new IntentFilter();
       intentFilter.addAction(SMS_UPDATE_RECEIVED);
       intentFilter.addAction(SMS_UPDATE_CANCELED);
       context.registerReceiver(mSmsApplicationBroadcastReceiver, intentFilter);
       mSmsApplicationPreference = listPreference;
       mcontext = context;
    }
    public boolean getBroadcastIntent(Context context, String newValue) {
         Log.i(TAG, "getBroadcastIntent\n");
        mNewPackageName = newValue.toString();
        Intent intent = new Intent(
                "android.provider.Telephony.ACTION_CHANGE_DEFAULT");
        intent.setPackage("com.android.settings");
        Bundle bundle = new Bundle();
        bundle.putString("package", newValue);
        intent.putExtras(bundle);
        context.startActivity(intent);
        setSummary = false;
        return false;
    }
    public boolean canSetSummary() {
        Log.i(TAG, "canSetSummary" + setSummary);
        boolean ifset = setSummary;
        setSummary = true;
        return ifset;
    }
      public void updateSmsApplicationSetting()
      {
         Log.i(TAG, "updateSmsApplicationSetting\n");
        //  log("Plugin::updateSmsApplicationSetting:");
                  ComponentName appName = SmsApplication.getDefaultSmsApplication(mcontext, true);
                  if (appName != null) {
                      String packageName = appName.getPackageName();

                      CharSequence[] values = mSmsApplicationPreference.getEntryValues();
                      for (int i = 0; i < values.length; i++) {
                          if (packageName.contentEquals(values[i])) {
                              mSmsApplicationPreference.setValueIndex(i);
                              mSmsApplicationPreference.setSummary(
                                mSmsApplicationPreference.getEntries()[i]);
                              break;
                          }
                      }
                  }
      }

public void deregisterBroadcastReceiver(Context context) {
         Log.i(TAG, "deregisterBroadcastReceiver\n");
context.unregisterReceiver(mSmsApplicationBroadcastReceiver);
}
      public class SmsApplicationBroadcastReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "SmsApplicationBroadcastReceiver::onReceive\n");
            if (mSmsApplicationPreference != null) {
              if (intent.getAction().equals(SMS_UPDATE_RECEIVED)) {
                  //SmsApplication.setDefaultApplication(mNewPackageName, getActivity());
                  updateSmsApplicationSetting();
              } else if (intent.getAction().equals(SMS_UPDATE_CANCELED)) {
                  String oldComName = intent.getStringExtra("old_sms_app");
                  SmsApplication.setDefaultApplication(oldComName, mcontext);
                  updateSmsApplicationSetting();
              }
            }
        }
    }

}
