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

package com.mediatek.apst.target.util;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import com.mediatek.apst.target.data.provider.message.SmsContent;
import com.mediatek.apst.target.data.proxy.contacts.USIMUtils;
import com.mediatek.apst.target.data.proxy.sysinfo.SystemInfoProxy;
import com.mediatek.apst.util.FeatureOptionControl;
import com.mediatek.apst.util.command.sysinfo.SimDetailInfo;
import com.mediatek.apst.util.entity.contacts.RawContact;
import com.mediatek.apst.util.entity.message.Message;
import com.mediatek.internal.telephony.ITelephonyEx;
//import com.mediatek.telephony.SimInfoManager;
//import com.mediatek.telephony.SimInfoManager.SimInfoRecord;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author MTK81255
 *
 */
public abstract class Global {
    // ==============================================================
    // Constants
    // ==============================================================
    // 400K is fastest on emulator
    // private static final int DEFAULT_BUFFER_SIZE_EMULATOR = 400000;
    // 800K is fastest on OPPO
    // private static final int DEFAULT_BUFFER_SIZE_TARGET = 800000;
    // public static final int DEFAULT_BUFFER_SIZE_PC = 400000;
    public static final int DEFAULT_BUFFER_SIZE = 800000;

    // mContext will be initialed in MainService. Added by Shaoying Han
    public static Context sContext;

    // ==============================================================
    // Fields
    // ==============================================================
    private static ByteBuffer sByteBuffer = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE);

    // ==============================================================
    // Constructors
    // ==============================================================

    // ==============================================================
    // Getters
    // ==============================================================
    /**
     * @return A byte buffer.
     */
    public static ByteBuffer getByteBuffer() {
        return sByteBuffer;
    }

    /**
     * @param simId
     *            The sim id.
     * @return The sim name.
     */
    public static String getSimName(int simId) {
        String simName = "";
        Debugger.logD(new Object[] { simId }, "The simId is " + simId);
        if (simId < SmsContent.SIM_ID_MIM || simId > SmsContent.SIM_ID_MAX) {
            Debugger.logW("The simId is wrong!");
            return simName;
        }
        if (null != sContext) {
            SubscriptionInfo info = SubscriptionManager.from(sContext)
                    .getActiveSubscriptionInfo(simId);
            if (null != info) {
                simName = info.getDisplayName().toString();
                Debugger.logD(new Object[] { simId }, "The simName is " + simName);
            }
        } else {
            Debugger.logW("mContext is null");
        }

        return simName;
    }

    /**
     * @param simId
     *            The simId.
     * @return The sourceLocation.
     */
    public static int getSourceLocationById(int simId) {
        int sourceLocation = RawContact.SOURCE_NONE;
        Debugger.logD(new Object[] { simId }, "The indicateSimOrPhone is " + simId);
        if (simId == -1) {
            return RawContact.SOURCE_PHONE;
        }
        if (simId < SmsContent.SIM_ID_MIM || simId > SmsContent.SIM_ID_MAX) {
            return sourceLocation;
        }
        if (null != sContext) {
            SubscriptionInfo info = SubscriptionManager.from(sContext)
                    .getActiveSubscriptionInfo(simId);
            if (null != info) {
                if (Config.MTK_GEMINI_SUPPORT) {
                    sourceLocation = info.getSimSlotIndex() + 1; // info.slotId +
                                                                // 1;
                } else {
                    sourceLocation = info.getSimSlotIndex(); // info.slotId;
                }
            }

            Debugger.logW("The sim slot from SimInfoRecord is " + sourceLocation);
        } else {
            Debugger.logW("mContext is null");
        }

        return sourceLocation;
    }

    /**
     * @param simId
     *            The sim id.
     * @return The info about sim with sim id.
     */
    public static SimDetailInfo getSimInfoById(int simId) {
        if (simId < SmsContent.SIM_ID_MIM || simId > SmsContent.SIM_ID_MAX) {
            Debugger.logW("The simId is wrong! The simId is " + simId);
            return new SimDetailInfo();
        }
        if (null != sContext) {
            SubscriptionInfo info = SubscriptionManager.from(sContext)
                    .getActiveSubscriptionInfo(simId);
            SimDetailInfo deailInfo = new SimDetailInfo();
            if (null != info) {
                deailInfo.setColor(info.getIconTint());
                deailInfo.setDisplayName(info.getDisplayName().toString());
                deailInfo.setNumber(info.getNumber());
                deailInfo.setSimId((int) info.getSubscriptionId());
                deailInfo.setICCId(info.getIccId());
                deailInfo.setSlotId((int) info.getSimSlotIndex());
                if (Config.MTK_GEMINI_SUPPORT) {
                    deailInfo.setAccessible(SystemInfoProxy
                            .getSimAccessibleBySlot((int) info.getSimSlotIndex()));
                }
            } else {
                Debugger.logW("Get simInfo is null by simId = " + simId);
                return new SimDetailInfo();
            }

            // final ITelephony iTel =
            // ITelephony.Stub.asInterface(ServiceManager
            // .getService(Context.TELEPHONY_SERVICE));
            ITelephonyEx iTel = ITelephonyEx.Stub.asInterface(ServiceManager
                    .getService(Context.TELEPHONY_SERVICE_EX));
            if (iTel == null) {
                return deailInfo;
            }
            try {
                if (Config.MTK_GEMINI_SUPPORT) { // Here need not modification
                                                 // for 4 SIM.
                    if ("USIM".equals(iTel.getIccCardType(info.getSubscriptionId()))) {
                        deailInfo.setSIMType(SimDetailInfo.SIM_TYPE_USIM);
                    } else {
                        deailInfo.setSIMType(SimDetailInfo.SIM_TYPE_SIM);
                    }
                } else {
                    if ("USIM".equals(iTel.getIccCardType(0))) {
                        deailInfo.setSIMType(SimDetailInfo.SIM_TYPE_USIM);
                    } else {
                        deailInfo.setSIMType(SimDetailInfo.SIM_TYPE_SIM);
                    }
                }
            } catch (RemoteException e) {
                Debugger.logI(e.getMessage());
            }

            return deailInfo;

        } else {
            Debugger.logW("mContext is null");
        }

        return new SimDetailInfo();
    }

    /**
     * @param slotId
     *            The id of the slot.
     * @return The info about sim with slot id.
     */
    public static SimDetailInfo getSimInfoBySlot(int slotId) {
        Debugger.logD(new Object[] { slotId }, "The slotId is " + slotId);
        SimDetailInfo deailInfo = new SimDetailInfo();
        deailInfo.setSlotId(slotId);
        if (Config.MTK_GEMINI_SUPPORT) {
            deailInfo.setAccessible(SystemInfoProxy.getSimAccessibleBySlot(slotId));
        }
        if ((Message.SIM3_ID == slotId && false == Config.MTK_3SIM_SUPPORT)
                || (Message.SIM4_ID == slotId && false == Config.MTK_4SIM_SUPPORT)) {
            return deailInfo;
        }
        if (null != sContext) {
            SubscriptionInfo subInfoList = SubscriptionManager.from(sContext)
                    .getActiveSubscriptionInfoForSimSlotIndex(slotId);
            if (subInfoList != null) {
                SubscriptionInfo info = subInfoList;
                if (null != info) {
                    deailInfo.setColor(info.getIconTint());
                    deailInfo.setDisplayName(info.getDisplayName().toString());
                    deailInfo.setNumber(info.getNumber());
                    deailInfo.setSimId((int) info.getSubscriptionId());
                    deailInfo.setICCId(info.getIccId());

                }
                if (USIMUtils.isSimUsimType((int) info.getSubscriptionId())) {
                    deailInfo.setSIMType(SimDetailInfo.SIM_TYPE_USIM);
                } else {
                    deailInfo.setSIMType(SimDetailInfo.SIM_TYPE_SIM);
                }
            }

            // SubInfoRecord info =
            // SubscriptionManager.getSubInfoForSubscriber(slotId);

            // if (null != info) {
            // deailInfo.setColor(info.color);
            // deailInfo.setDisplayName(info.displayName);
            // deailInfo.setNumber(info.number);
            // deailInfo.setSimId((int) info.subId);
            // deailInfo.setICCId(info.iccId);
            //
            // }
            // final ITelephony iTel =
            // ITelephony.Stub.asInterface(ServiceManager
            // .getService(Context.TELEPHONY_SERVICE));
            /*
             * ITelephonyEx iTel =
             * ITelephonyEx.Stub.asInterface(ServiceManager.getService
             * (Context.TELEPHONY_SERVICE_EX)); if (iTel == null) { return
             * deailInfo; } try { if (Config.MTK_GEMINI_SUPPORT) { // Here need
             * not modification for 4 SIM. if
             * ("USIM".equals(iTel.getIccCardType(slotId + 1))) {
             * deailInfo.setSIMType(SimDetailInfo.SIM_TYPE_USIM); } else {
             * deailInfo.setSIMType(SimDetailInfo.SIM_TYPE_SIM); } } else { if
             * ("USIM".equals(iTel.getIccCardType(1))) {
             * deailInfo.setSIMType(SimDetailInfo.SIM_TYPE_USIM); } else {
             * deailInfo.setSIMType(SimDetailInfo.SIM_TYPE_SIM); } } } catch
             * (RemoteException e) { Debugger.logI(e.getMessage()); }
             */
            return deailInfo;
        } else {
            Debugger.logW("mContext is null");
        }

        return deailInfo;
    }

    /**
     * @param slotId
     *            The slot id.
     * @return The sim id or -1.
     */
    public static int getSimIdBySlot(int slotId) {
        Debugger.logD(new Object[] { slotId }, "The slotId is " + slotId);
        if ((Message.SIM3_ID == slotId && false == Config.MTK_3SIM_SUPPORT)
                || (Message.SIM4_ID == slotId && false == Config.MTK_4SIM_SUPPORT)) {
            Debugger.logW("This is not 3 sim or 4 sim phone.");
            return -1;
        }
        if (null != sContext) {
            SubscriptionInfo info = SubscriptionManager.from(sContext)
                    .getActiveSubscriptionInfo(slotId);
            if (null != info) {
                Debugger.logD(new Object[] { slotId }, "The simId is "
            + (int) info.getSubscriptionId());
                return (int) info.getSubscriptionId(); //info.subId;
            }
        } else {
            Debugger.logW("mContext is null");
        }
        return -1;
    }

    /**
     * @return The list of all sim information or null.
     */
    public static List<SimDetailInfo> getAllSIMList() {
        if (null != sContext) {
            List<SimDetailInfo> detailInfoList = new ArrayList<SimDetailInfo>();
            List<SubscriptionInfo> list = SubscriptionManager.from(sContext)
                    .getAllSubscriptionInfoList();
            if (null != list) {
                int size = list.size();
                // detailInfoList = new ArrayList<SimDetailInfo>();
                for (int i = 0; i < size; i++) {
                    SubscriptionInfo info = list.get(i);
                    SimDetailInfo deailInfo = new SimDetailInfo();
                    if (null != info) {
                        deailInfo.setColor(info.getIconTint());
                        deailInfo.setDisplayName(info.getDisplayName().toString());
                        deailInfo.setNumber(info.getNumber());
                        deailInfo.setSimId((int) info.getSubscriptionId());
                        deailInfo.setICCId(info.getIccId());
                        deailInfo.setSlotId((int) info.getSimSlotIndex());
                        detailInfoList.add(deailInfo);
                        if (Config.MTK_GEMINI_SUPPORT) {
                            deailInfo.setAccessible(SystemInfoProxy
                                    .getSimAccessibleBySlot((int) info.getSimSlotIndex()));
                        }
                    }
                }
            }
            return detailInfoList;

        } else {
            Debugger.logW("mContext is null");
        }

        return null;
    }

    /**
     * Init the feature option.
     */
    public static void initFeatureOptionList() {
        FeatureOptionControl.BACKUP_N_BACKAPP = 0x1;
        FeatureOptionControl.CONTACT_N_USIMGROUP = 0x1 << 1;
        FeatureOptionControl.MESSAGE_C_DRAFTMSGDISPLAY = 0x1 << 2;
        FeatureOptionControl.BACKUP_N_SDSWAP = 0x1 << 3;
        FeatureOptionControl.BACKUP_N_EMAIL = 0x1 << 4;
    }
}
