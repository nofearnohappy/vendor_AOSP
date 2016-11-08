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

package com.mediatek.bluetoothle.anp.data;

import java.util.UUID;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class GattAnsAttributes {

    // / uuid of service
    public static final UUID ALERT_NOTIFICATION_SERVICE_UUID = UUID
            .fromString("00001811-0000-1000-8000-00805f9b34fb");
    public static final UUID ALERT_NOTIFICATION_CONTROL_POINT_UUID = UUID
            .fromString("00002a44-0000-1000-8000-00805f9b34fb");
    public static final UUID UNREAD_ALERT_STATUS = UUID
            .fromString("00002a45-0000-1000-8000-00805f9b34fb");
    public static final UUID NEW_ALERT_UUID = UUID
            .fromString("00002a46-0000-1000-8000-00805f9b34fb");
    public static final UUID SUPPORTED_NEW_ALERT_CATEGORY_UUID = UUID
            .fromString("00002a47-0000-1000-8000-00805f9b34fb");
    public static final UUID SUPPORTED_UNREAD_ALERT_CATEGORY_UUID = UUID
            .fromString("00002a48-0000-1000-8000-00805f9b34fb");
    // descriptor
    public static final UUID CLIENT_CHARACTERISTIC_CONFIG_DES_UUID = UUID
            .fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final int SUPPORT_CATEGORY_MASK_SIMPLE = 1;
    public static final int SUPPORT_CATEGORY_MASK_EMAIL = 2;
    public static final int SUPPORT_CATEGORY_MASK_NEWS = 4;
    public static final int SUPPORT_CATEGORY_MASK_INCOMING_CALL = 8;
    public static final int SUPPORT_CATEGORY_MASK_MISSED_CALL = 16;
    public static final int SUPPORT_CATEGORY_MASK_SMS = 32;
    public static final int SUPPORT_CATEGORY_MASK_VOICE_MAIL = 64;
    public static final int SUPPORT_CATEGORY_MASK_SCHEDULE = 128;
    public static final int SUPPORT_CATEGORY_MASK_HIGH_PRIORITIZED = 256;
    public static final int SUPPORT_CATEGORY_MASK_INSTANT_MESSAGE = 512;

    public static byte[] concatBytes(byte[] firstArray, byte[] secondArray) {
        byte[] result = new byte[secondArray.length + firstArray.length];
        System.arraycopy(firstArray, 0, result, 0, firstArray.length);
        System.arraycopy(secondArray, 0, result, firstArray.length, secondArray.length);
        return result;
    }

}
