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

package com.mediatek.dialer.plugin.calllog;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.mediatek.common.PluginImpl;
import com.mediatek.dialer.ext.DefaultCallLogExtension;
import com.mediatek.dialer.plugin.OP09DialerPluginUtil;
import com.mediatek.op09.plugin.R;

@PluginImpl(interfaceName="com.mediatek.dialer.ext.ICallLogExtension")
public class OP09CallLogExt extends DefaultCallLogExtension {
	private static final String TAG = "OP09CallLogExt";
	private static final String ITEM_INFO = "calllog_item_info";
	private static final String ACCOUNT_LABEL = "call_account_label";
	private static final String ID = "id";
	public static final int ICON_ID = 100;

    /**
     *for OP09
     * set account for call log list
     *
     * @param Context context
     * @param View view
     * @param PhoneAccountHandle phoneAccountHandle
     */
    public void setCallAccountForCallLogList(Context context, View view,
                                             PhoneAccountHandle phoneAccountHandle) {
        log("setCallAccountForCallLogList");
        TelephonyManager telephonyManager = (TelephonyManager) context.
                getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager.getDefault().getPhoneCount() < 2) {
            return;
        }
        OP09DialerPluginUtil dialerPlugin = new OP09DialerPluginUtil(context);
        Context pluginContext = dialerPlugin.getPluginContext();
        Resources resource = context.getResources();
        String packageName = context.getPackageName();
        TelecomManager telecomManager = (TelecomManager) context.getSystemService(
                Context.TELECOM_SERVICE);
        PhoneAccount account = telecomManager.getPhoneAccount(phoneAccountHandle);
        if (null == view || null == account || null == context) {
            return;
        }
        ViewGroup callLogItem =
                (ViewGroup) view.findViewById(
                        resource.getIdentifier(ITEM_INFO, ID, packageName));
        View accountLable =
                view.findViewById(
                        resource.getIdentifier(ACCOUNT_LABEL, ID, packageName));
        callLogItem.removeView(accountLable);
        Bitmap iconBitmap = account.getIcon().getBitmap();
        ViewGroup viewChild = (ViewGroup)callLogItem.findViewById(ICON_ID);
        if (null == viewChild && null != iconBitmap && null != callLogItem) {
            LayoutInflater mInflater;
            mInflater = LayoutInflater.from(pluginContext);
            ViewGroup iconLayout = (ViewGroup) mInflater.inflate(R.layout.ct_sim_indicator, null);
            iconLayout.setId(ICON_ID);
            ImageView simIndicator = (ImageView)iconLayout.findViewById(R.id.simIndicator);
            simIndicator.setVisibility(View.VISIBLE);
            simIndicator.setImageDrawable(new BitmapDrawable(iconBitmap));
            iconLayout.addView(accountLable, 1);
            callLogItem.addView(iconLayout,3);
        } else if (null != iconBitmap && null != callLogItem) {
            View simIndicator = viewChild.getChildAt(0);
            if (null != simIndicator && (simIndicator instanceof ImageView)) {
                simIndicator.setVisibility(View.VISIBLE);
                ((ImageView)simIndicator).setImageDrawable(new BitmapDrawable(iconBitmap));
            }
        }
    }

    private void log(String msg) {
        Log.d(TAG, msg);
    }
}

