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

package com.mediatek.op.telephony;

import android.content.Context;
import android.database.Cursor;
import android.net.LinkAddress;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.RetryManager;
import com.android.internal.telephony.dataconnection.DcFailCause;

import com.mediatek.common.PluginImpl;
import com.mediatek.common.telephony.IGsmDCTExt;

@PluginImpl(interfaceName="com.mediatek.common.telephony.IGsmDCTExt")
public class GsmDCTExt implements IGsmDCTExt {
    static final String TAG = "GsmDCTExt";
    private int mIsSmCauseOn = 0;

    public GsmDCTExt() {
    }

    public GsmDCTExt(Context context) {
        isSmCauseOn();
    }

    public Cursor getOptPreferredApn(String imsi, String operator, int simID) {
        return null;
    }

    public boolean isDomesticRoamingEnabled() {
        return false;
    }

    public boolean isDataAllowedAsOff(String apnType) {
        if (TextUtils.equals(apnType, PhoneConstants.APN_TYPE_DEFAULT)) {
            return false;
        }
        return true;
    }

    public boolean getFDForceFlag(boolean forceFlag) {
        // Only for operator (not CMCC) have the chance to set forceFlag as true when SCREEN is ON
        // Force to send SCRI msg to NW if MTK_FD_FORCE_REL_SUPPORT is true
        // ALPS00071650 for FET NW issue
        if ((SystemProperties.getInt("ro.mtk_fd_force_rel_support", 0) == 1)) {
            return true;
        }
        return forceFlag;
    }

    public int getPsAttachSimWhenRadioOn() {
        return -1;
    }

    public boolean isPsDetachWhenAllPdpDeactivated() {
        return true;
    }
    public int getDelayTime() {
        return 20000;
    }
    public boolean needDelayedRetry(int cause) {
        return false;
    }

    public boolean needSmRetry(Object cause) {
        if (isCctSmRetry()){
            DcFailCause tmpcause = (DcFailCause) cause;
            log("[OP] Check sm cause:" +  tmpcause);
            if (tmpcause == DcFailCause.USER_AUTHENTICATION ||
                tmpcause == DcFailCause.SERVICE_OPTION_NOT_SUBSCRIBED) {
                return true;
            }
        } else {
            log("default: no needSmRetry");
        }
        return false;
    }
    public boolean doSmRetry(Object cause, Object obj1, Object obj2) {
        if (isCctSmRetry()){
            log("[OP] doSmRetry: setSmRetryConfig");
            setSmRetryConfig(obj1);
            return true;
        }
        //obj1 = RetryManager, obj2 = etc...
        log("default: no op sm retry");
        return false;
    }
    public boolean setSmRetryConfig(Object retryManager) {
        if (isCctSmRetry()){
            RetryManager rm = (RetryManager) retryManager;
            int maxRetryCount = 2;
            int retryTime = 45000;
            int randomizationTime = 0;
            int retryCount = rm.getRetryCount();
            log("[OP] set SmRetry Config:" + maxRetryCount + "/"
                + retryTime + "/" + randomizationTime);
            rm.configure(maxRetryCount, retryTime, randomizationTime);
            rm.setRetryCount(retryCount);
            return true;
        }
        return false;
    }
    public boolean needRacUpdate() {
        if (isSmCauseOn() != 0) {
            return true;
        }
        return false;
    }
    public boolean isFdnEnableSupport() {
        // Default is return false, set to true if needed
        return false;
    }

    public boolean isPermanentFail(Object dcFailCause) {
        // Default Return false; to prevent from permanentfail for SM_CAUSE
        DcFailCause tmpcause = (DcFailCause) dcFailCause;
        if (isSmCauseOn() != 0) {
            log("[OP] Check sm cause isPermanentFail:" +  tmpcause);
            return tmpcause.isPermanentFail();
        } else {
            return false;
        }
        /*  uncomment following code or move following code to GsmDCTExtOp for customizing
                    return Permanent failed by failed cause
                */
    }

    public boolean isCctSmRetry() {
        if (SystemProperties.getInt("persist.data.cc33.support", 0) == 1) {
            //FeatureOption.MTK_CC33_SUPPORT
            return true;
        } else {
            return false;
        }
    }

    private int isSmCauseOn() {
        mIsSmCauseOn = SystemProperties.getInt("ril.specific.sm_cause", 0);
        return mIsSmCauseOn;
    }

    /** IPV6RA feature. */
    public long getIPv6Valid(Object address) {
        LinkAddress addr = (LinkAddress) address;
        if (addr != null) {
            return addr.getValid();
        }
        return -1000;
    }

    /*op01 iot*/
    public void onDcActivated(String[] apnTypes, String ifc) {
    }

    public void onDcDeactivated(String[] apnTypes, String ifc) {
    }

    /** getDisconnectDoneRetryTimer. */
    public int getDisconnectDoneRetryTimer(String reason, int defaultTimer) {
        int timer = defaultTimer;
        if (Phone.REASON_RA_FAILED.equals(reason)) {
            // RA failed, retry after 90s
            timer = 90000;
        }
        return timer;
    }

    public boolean isIgnoredCause(Object cause) {
        DcFailCause tmpCause = (DcFailCause) cause;
        log("[OP] Check sm cause:" +  tmpCause);
        return false;
    }

    public void log(String text) {
        Log.d(TAG, text);
    }
}
