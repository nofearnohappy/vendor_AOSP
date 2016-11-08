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

package com.mediatek.rcs.common;

import android.util.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mediatek.rcs.common.MessageStatusUtils.IFileTransfer.Status;

/**
 * RCSCacheManager.
 */
public class RCSCacheManager{
    public static Map<Long, IpMessage> sCachedSendMessage =
            new ConcurrentHashMap<Long, IpMessage>();
    private static final String TAG = "RCSCacheManager";

    private static final int MAX_CACHE_SIZE = 100;

    private RCSCacheManager() {

    }

    /**
     * getIpMessage.
     * @param ipmsgId.
     * @return
     */
    public static IpMessage getIpMessage(long ipmsgId) {
         Log.d(TAG, "getIpMessage() enter, ipmsgId = " + ipmsgId);
         synchronized (sCachedSendMessage) {
            if (sCachedSendMessage.containsKey(ipmsgId)) {
                Log.d(TAG, "get ft ipmsg from sCachedSendMessage ipMessage = " +
                            sCachedSendMessage.get(Long.valueOf(ipmsgId)));
                return sCachedSendMessage.get(ipmsgId);
            }
        }
        return null;
    }

    /**
     * setIpMessage.
     * @param ipMsg
     * @param ipmsgId
     */
    public static void setIpMessage(IpMessage ipMsg, long ipmsgId) {
        Log.d(TAG, "setIpMessage() enter, ipmsgId = " + ipmsgId);
        if (ipMsg != null) {
            synchronized (sCachedSendMessage) {
                if (!sCachedSendMessage.containsKey(ipmsgId)) {
                    if (sCachedSendMessage.size() < MAX_CACHE_SIZE) {
                        sCachedSendMessage.put(ipmsgId, ipMsg);
                    } else {
                        Log.d(TAG, "setIpMessage(), exceed MAX_CACHE_SIZE and clear cache");
                        sCachedSendMessage.clear();
                        sCachedSendMessage.put(ipmsgId, ipMsg);
                    }
                }
            }
        }
    }

    /**
     * updateStatus.
     * @param ipmsgId
     * @param rcsStatus
     * @param smsStatus
     */
    public static void updateStatus(long ipmsgId, Status rcsStatus, int smsStatus) {
        Log.d(TAG, "updateStatus() enter, ipmsgId = " + ipmsgId + " rcsStatus = " + rcsStatus +
                " smsStatus " + smsStatus);
        IpMessage ipMessage;
        synchronized (sCachedSendMessage) {
            if (sCachedSendMessage.containsKey(ipmsgId)) {
                Log.d(TAG, "updateStatus ipMessageId = " + ipmsgId);
                ipMessage =  sCachedSendMessage.get(ipmsgId);
                if (ipMessage instanceof IpAttachMessage) {
                    ((IpAttachMessage) ipMessage).setRcsStatus(rcsStatus);
                    ipMessage.setStatus(smsStatus);
                    sCachedSendMessage.remove(ipmsgId);
                    sCachedSendMessage.put(ipmsgId, ipMessage);
                }
            }
        }
    }

    /**
     * setFilePath.
     * @param ipmsgId
     * @param filePath
     */
    public static void setFilePath(long ipmsgId, String filePath) {
        Log.d(TAG, "setFilePath() enter, ipmsgId = " + ipmsgId + " filePath = " + filePath);
        IpMessage ipMessage;
        synchronized (sCachedSendMessage) {
            if (sCachedSendMessage.containsKey(ipmsgId)) {
                Log.d(TAG, "updateStatus ipMessageId = " + ipmsgId);
                ipMessage =  sCachedSendMessage.get(ipmsgId);
                if (ipMessage instanceof IpAttachMessage) {
                    ((IpAttachMessage) ipMessage).setPath(filePath);
                    sCachedSendMessage.remove(ipmsgId);
                    sCachedSendMessage.put(ipmsgId, ipMessage);
                }
            }
        }
    }

    /**
     * removeIpMessage.
     * @param ipmsgId
     */
    public static void removeIpMessage(long ipmsgId) {
        Log.d(TAG, "removeIpMessage() enter, ipmsgId = " + ipmsgId);
        synchronized (sCachedSendMessage) {
            if (sCachedSendMessage.containsKey(ipmsgId)) {
                Log.d(TAG, "removeIpMessage , ipmsgId = " + ipmsgId);
                sCachedSendMessage.remove(ipmsgId);
            }
        }
    }

    /**
     * clearCache.
     */
    public static void clearCache() {
        Log.d(TAG, "clearCache() enter ");
        if (sCachedSendMessage != null) {
            synchronized (sCachedSendMessage) {
                sCachedSendMessage.clear();
            }
        }
    }

}