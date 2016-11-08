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

package com.mediatek.usbchecker.ext;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceGroup;
import android.util.Log;

import com.mediatek.common.PluginImpl;
import com.mediatek.settings.ext.DefaultDevExt;
import com.mediatek.usbchecker.UsbCheckerConstants;
import com.mediatek.usbchecker.UsbCheckerService;

@PluginImpl(interfaceName="com.mediatek.settings.ext.IDevExt")
public class UsbCheckerExt extends DefaultDevExt {

    private Preference mPreference;
    private EventReceiver mReceiver;
    private Context mContext; 

    private static final String TAG = "UsbChecker/UsbCheckerExt";

    private class EventReceiver extends BroadcastReceiver {

        public void initialize() {
            Log.d(TAG, "EventReceiver initialize()");
            IntentFilter filter = new IntentFilter();
            filter.addAction(UsbCheckerConstants.INTENT_USB_ACTIVATION);
            mContext.registerReceiver(this, filter);
        }

        public void destroy() {
            Log.d(TAG, "EventReceiver destroy()");
            mContext.unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive():" + intent.getAction());
            
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            
            if (action.equals(UsbCheckerConstants.INTENT_USB_ACTIVATION)) {
                if (mPreference != null) {
                    updateUI();
                }                
            }
        }
    }
    
    public UsbCheckerExt(Context context) {
        super(context);
        mContext = context;
    }

    public void customUSBPreference(Preference pref) {
        Log.d(TAG, "customUSBPreference()");
        
        if (null != pref) {
            if (pref instanceof Preference) {
                mPreference = pref;
                updateUI();
            }
        }
    }

    private void updateUI() {
        boolean enable = true;
        if(!UsbCheckerService.getActivateState()) {
            enable = false;
            if (mReceiver == null) {
                mReceiver = new EventReceiver();
                mReceiver.initialize();
            }
        } else {
            if (mReceiver != null) {
                mReceiver.destroy();
                mReceiver = null;
            }
        }
        Log.d(TAG, "updateUI() enable state = " + enable);
        mPreference.setEnabled(enable);
    }
}
