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

package com.mediatek.engineermode;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.RadioAccessFamily;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneConstants;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.internal.telephony.worldphone.WorldPhoneUtil;

/**
 * <p>
 * Description: To get modem type.
 *
 * @author mtk54043
 *
 */

public class ModemCategory {

    private static final String TAG = "EM_ModemCategory";

    public static final int MODEM_FDD = 1;
    public static final int MODEM_TD = 2;
    public static final int MODEM_NO3G = 3;

    public static final int MODEM_MASK_GPRS = 0x01;
    public static final int MODEM_MASK_EDGE = 0x02;
    public static final int MODEM_MASK_WCDMA = 0x04;
    public static final int MODEM_MASK_TDSCDMA = 0x08;
    public static final int MODEM_MASK_HSDPA = 0x10;
    public static final int MODEM_MASK_HSUPA = 0x20;

    /**
     *
     * @return modem type
     */
    public static int getModemType() {
    int mode = MODEM_NO3G;
    int mask = WorldPhoneUtil.get3GDivisionDuplexMode();

    if ((1 == mask) || (2 == mask)) {
            mode = mask;
        }

        Log.i("@M_" + TAG, "mode = " + mode);
        return mode;
    }

    public static boolean isCdma() {
        return FeatureSupport.isSupported(FeatureSupport.FK_MTK_C2K_SUPPORT);
    }

    public static int getCapabilitySim() {
        try {
            ITelephonyEx iTelEx = ITelephonyEx.Stub.asInterface(
                    ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
            if (iTelEx != null) {
                int ret = iTelEx.getMainCapabilityPhoneId();
                Log.i("@M_" + TAG, "getMainCapabilityPhoneId() = " + ret);
                return ret;
            } else {
                Log.e("@M_" + TAG, "ITelephonyEx iTelEx = null");
                return PhoneConstants.SIM_ID_1;
            }
        } catch (RemoteException e) {
            Log.e("@M_" + TAG, "ITelephonyEx RemoteException");
            e.printStackTrace();
            return PhoneConstants.SIM_ID_1;
        }
    }
}
