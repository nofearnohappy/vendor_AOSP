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

package com.mediatek.dm.util;

import android.util.Log;

import com.mediatek.dm.DmConst;

import java.nio.ByteBuffer;

public class NiaDecoder {
    private static final String CLASS_TAG = DmConst.TAG.LOG_TAG_PREFIX + "NiaDecoder";

    static void decode(byte[] msg) {
        assert (msg != null);
        assert (msg.length != 0);
        ByteBuffer buffer = ByteBuffer.wrap(msg);

        // digest: 128 bit
        buffer.getDouble();
        buffer.getDouble();
        // version: 10 bit
        byte b1 = buffer.get();
        byte b2 = buffer.get();
        int version = ((b1 << 2) + (b2 >>> 6)) & 0xff; // b2 use logical
                                                       // right-shift
        // ui mode: 2 bit, 10 10 1010
        int uiMode = ((b2 << 2) >>> 6) & 3;
        // initiator: 1 bit
        int initiator = ((b2 << 4) >> 7) & 1;
        // future used: 27 bit
        buffer.get();
        buffer.get();
        buffer.get();
        // session id: 16 bit
        short sessionId = buffer.getShort();
        // server identifier length: 8 bit
        int serverIdLength = buffer.get();
        byte[] dst = new byte[serverIdLength];
        buffer.get(dst);
        String serverId = new String(dst);

        Log.i(CLASS_TAG, "NiaDecoder: begin");
        Log.i(CLASS_TAG, "version: " + version);
        Log.i(CLASS_TAG, "uiMode: " + uiMode);
        Log.i(CLASS_TAG, "initiator: " + initiator);
        Log.i(CLASS_TAG, "sessionId: " + sessionId);
        Log.i(CLASS_TAG, "serverId length: " + serverIdLength);
        Log.i(CLASS_TAG, "serverId: " + serverId);
        Log.i(CLASS_TAG, "NiaDecoder: end");
    }
}
