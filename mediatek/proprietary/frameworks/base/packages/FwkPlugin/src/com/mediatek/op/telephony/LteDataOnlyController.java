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
import android.content.Intent;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;

import com.android.internal.telephony.PhoneConstants;

import com.mediatek.common.PluginImpl;
import com.mediatek.common.telephony.ILteDataOnlyController;
import com.mediatek.internal.telephony.cdma.CdmaFeatureOptionUtils;

/**
 * For check Lte data only mode that whether show dialog prompt.
 */
@PluginImpl(interfaceName = "com.mediatek.common.telephony.ILteDataOnlyController")
public class LteDataOnlyController implements ILteDataOnlyController {
    private static final String TAG = "LteDataOnlyController";

    private static final String CHECK_PERMISSION_SERVICE_PACKAGE =
            "com.android.phone";
    private static final String ACTION_CHECK_PERMISSISON_SERVICE =
            "com.mediatek.intent.action.LTE_DATA_ONLY_MANAGER";
    protected Context mContext;

    public static final int CT_SIM = 5;
    public static final int CT4G_SIM = 4;
    public static final int CT3G_SIM = 3;
    public static final int GSM_SIM = 2;
    public static final int ERROR_SIM = -1;


    /**
     * Constructor method.
     * @param context For start service.
     */
    public LteDataOnlyController(Context context) {
        mContext = context;
    }

    @Override
    public boolean checkPermission() {
        if (isSupportTddDataOnlyCheck() && is4GDataOnly()) {
            startService();
            Rlog.d(TAG, "checkPermission result : false");
            return false;
        }
        Rlog.d(TAG, "checkPermission result : true");
        return true;
    }

    @Override
    public boolean checkPermission(int subId) {
        int slotId = SubscriptionManager.getSlotId(subId);
        Rlog.d(TAG, "checkPermission subId=" + subId + ", slotId=" + slotId);
        if (CdmaFeatureOptionUtils.getExternalModemSlot() == slotId) {
            return checkPermission();
        } else {
            return true;
        }
    }

    private void startService() {
        int subId[] = SubscriptionManager
                .getSubId(CdmaFeatureOptionUtils.getExternalModemSlot());
        Intent serviceIntent = new Intent(ACTION_CHECK_PERMISSISON_SERVICE);
        serviceIntent.setPackage(CHECK_PERMISSION_SERVICE_PACKAGE);
        if (subId != null) {
            serviceIntent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, subId[0]);
        }
        if (mContext != null) {
            mContext.startService(serviceIntent);
        }
    }

    /// M: [C2K][SVLTE] Define SVLTE RAT mode. @{
    /**
     * For C2K SVLTE RAT controll, LTE preferred mode.
     * @hide
     */
    public static final int SVLTE_RAT_MODE_4G = 0;

    /**
     * For C2K SVLTE RAT controll, EVDO preferred mode, will disable LTE.
     * @hide
     */
    public static final int SVLTE_RAT_MODE_3G = 1;

    /**
     * For C2K SVLTE RAT controll, LTE Data only mode, will disable CDMA and only allow LTE PS.
     * @hide
     */
    public static final int SVLTE_RAT_MODE_4G_DATA_ONLY = 2;
    /// @}

    private String getCdmaRatModeKey(int subId) {
        if (("OP09").equals(SystemProperties.get("ro.operator.optr", "OM"))) {
            return Settings.Global.LTE_ON_CDMA_RAT_MODE;
        } else {
            return Settings.Global.LTE_ON_CDMA_RAT_MODE + subId;
        }
    }

    private boolean is4GDataOnly() {
        if (mContext == null) {
            Rlog.e(TAG, "is4GDataOnly error,mContext == null");
            return false;
        }
        int subId[] = SubscriptionManager
                .getSubId(CdmaFeatureOptionUtils.getExternalModemSlot());


        if (subId != null) {
            Rlog.e(TAG, "cdma subId = " + subId[0]);
            int patternLteDataOnly = Settings.Global.getInt(
                    mContext.getContentResolver(), getCdmaRatModeKey(subId[0]),
                    SVLTE_RAT_MODE_4G);
            return (patternLteDataOnly == SVLTE_RAT_MODE_4G_DATA_ONLY);
        }
        return false;
    }

    /**
     * get getFullIccCardTypeExt type .
     * @param
     * @return sim string type
     */
    public static String getFullIccCardTypeExt() {
        Rlog.i(TAG, "getFullIccCardTypeExt cardType = "
                + SystemProperties.get("gsm.ril.fulluicctype"));

        return SystemProperties.get("gsm.ril.fulluicctype");
    }

    /**
     * get sim type .
     * @param
     * @return sim type
     */
    public static int getSimType() {
        String fullUiccType = getFullIccCardTypeExt();

        if (fullUiccType != null) {
            if (fullUiccType.contains("CSIM") || fullUiccType.contains("RUIM")) {

                if (fullUiccType.contains("CSIM")
                        || fullUiccType.contains("USIM")) {
                    return CT4G_SIM;
                } else if (fullUiccType.contains("SIM")) {
                    return CT3G_SIM;
                }
                return CT_SIM;
            } else if (fullUiccType.contains("SIM") || fullUiccType.contains("USIM")) {
                Rlog.d(TAG, "getSimType is GSM sim");
                return GSM_SIM;
            } else {
                Rlog.d(TAG, "getSimType not GSM, CT34G");
                return ERROR_SIM;
            }
        }
        Rlog.d(TAG, "getSimType fullUiccType null");
        return ERROR_SIM;
    }

    /**
     * get the sim card is ct card or not.
     * @return boolean is ct card or not.
     */
    public static boolean isCTCardType() {
        Rlog.i(TAG, "isCTCardType = "
                        + ((LteDataOnlyController.getSimType() == LteDataOnlyController.CT4G_SIM)
                        || (LteDataOnlyController.getSimType() == LteDataOnlyController.CT3G_SIM)
                        || (LteDataOnlyController.getSimType() == LteDataOnlyController.CT_SIM)));
        return (LteDataOnlyController.getSimType() == LteDataOnlyController.CT4G_SIM)
        || (LteDataOnlyController.getSimType() == LteDataOnlyController.CT3G_SIM)
        || (LteDataOnlyController.getSimType() == LteDataOnlyController.CT_SIM);
    }

    /**
     * get the sim card is ct 4g card or not.
     * @return boolean is ct 4g card or not.
     */
    public static boolean isCTLTECardType() {
        Rlog.i(TAG, "isCTLTECardType = " +
                (LteDataOnlyController.getSimType() == LteDataOnlyController.CT4G_SIM));
        return LteDataOnlyController.getSimType() == LteDataOnlyController.CT4G_SIM;
    }

    /**
     * get the sim card is cdma card or not.
     * @return boolean is cdma card or not.
     */
    public static boolean isCDMACardType() {
        Rlog.i(TAG, "isCDMACardType = " +
                (LteDataOnlyController.getSimType() == LteDataOnlyController.CT3G_SIM));
        return LteDataOnlyController.getSimType() == LteDataOnlyController.CT3G_SIM;
    }

    private boolean isSupportTddDataOnlyCheck() {
        //boolean isCT4gCard = (IccCardConstants.CardType.CT_4G_UICC_CARD
        //    == UiccController.getInstance().getCardType());
        boolean isCT4gCard = isCTLTECardType();
        boolean isCdmaLteDcSupport = CdmaFeatureOptionUtils.isCdmaLteDcSupport();
        boolean isExternalModemSlot = (CdmaFeatureOptionUtils.getExternalModemSlot() >= 0);
        boolean checkResult = false;

        if (isCT4gCard && isCdmaLteDcSupport && isExternalModemSlot) {
           checkResult = true;
        }
        Rlog.d(TAG, "isCT4gCard : " + isCT4gCard
            + ", isCdmaLteDcSupport : " + isCdmaLteDcSupport
            + ", isSlot1ExternalModemSlot : " + isExternalModemSlot
            + ", isSupportTddDataOnlyCheck return " + checkResult);
        return checkResult;
    }
}

