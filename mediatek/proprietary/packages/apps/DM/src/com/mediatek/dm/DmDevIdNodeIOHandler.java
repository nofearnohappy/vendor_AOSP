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

package com.mediatek.dm;

import android.app.Service;
import android.content.Context;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.mediatek.dm.DmConst.TAG;
import com.mediatek.dm.option.Options;
import com.redbend.vdm.NodeIoHandler;
import com.redbend.vdm.VdmException;

public class DmDevIdNodeIOHandler implements NodeIoHandler {
    private Context mContext;
    private static final String IMEI_HEADER = "IMEI:";

    public DmDevIdNodeIOHandler(Context context) {
        mContext = context;
    }

    public int read(int offset, byte[] data) throws VdmException {
        TelephonyManager telMgr = (TelephonyManager) mContext.getSystemService(Service.TELEPHONY_SERVICE);
        if (telMgr == null) {
            Log.e(TAG.NODE_IO_HANDLER, "Get TelephonyManager failed.");
            return 0;
        }

        String imei = "IMEI:" + telMgr.getDeviceId();
        if (Options.USE_SMS_REGISTER) {
            int subId = DmCommonFun.getRegisterSubID(mContext);
            if (subId != -1) {
                int slotId = SubscriptionManager.getSlotId(subId);
                imei = IMEI_HEADER + telMgr.getDeviceId(slotId);
            }
        }
        byte[] imeiNumber = imei.getBytes();
        int srcLen = imeiNumber.length;
        Log.i(TAG.NODE_IO_HANDLER, imei);

        if (data == null) {
            Log.i(TAG.NODE_IO_HANDLER, "Input buffer is null, just return the data length : "
                    + srcLen);
            return srcLen;
        }

        Log.i(TAG.NODE_IO_HANDLER, "Input buffer is not null, copy build number to it.");
        for (int i = 0; i < srcLen; i++) {
            data[offset + i] = imeiNumber[i];
        }

        return srcLen;
    }

    public void write(int offset, byte[] data, int totalsize) throws VdmException {

    }
}