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
 * MediaTek Inc. (C) 2014. All rights reserved.
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

package com.mediatek.bluetooth;

import java.util.UUID;

/**
 * Definition of GATT-related UUIDs
 *
 * @hide
 */

public final class BleGattUuid {

    /**
     * Definition of GATT services UUIDs
     */
    public static final class Service {

        /**
          * @internal
          */
        public static final UUID CURRENT_TIME = UUID
                .fromString("00001805-0000-1000-8000-00805f9b34fb");

        /**
          * @internal
          */
        public static final UUID NEXT_DST_CHANGE = UUID
                .fromString("00001807-0000-1000-8000-00805f9b34fb");

        /**
          * @internal
          */
        public static final UUID REFERENCE_TIME_UPDATE = UUID
                .fromString("00001806-0000-1000-8000-00805f9b34fb");

        /**
          * @internal
          */
        public static final UUID IMMEDIATE_ALERT = UUID
                .fromString("00001802-0000-1000-8000-00805f9b34fb");

        /**
          * @internal
          */
        public static final UUID LINK_LOST = UUID
                .fromString("00001803-0000-1000-8000-00805f9b34fb");

        /**
          * @internal
          */
        public static final UUID TX_POWER = UUID.fromString("00001804-0000-1000-8000-00805f9b34fb");

        public static final UUID ALERT_NOTIFICATION = UUID
                .fromString("00001811-0000-1000-8000-00805f9b34fb");
    }

    /**
     * Definition of GATT characteristic UUIDs
     */
    public static final class Char {

        /**
          * @internal
          */
        public static final UUID CURRENT_TIME = UUID
                .fromString("00002a2b-0000-1000-8000-00805f9b34fb");

        /**
          * @internal
          */
        public static final UUID LOCAL_TIME_INFO = UUID
                .fromString("00002a0f-0000-1000-8000-00805f9b34fb");

        /**
          * @internal
          */
        public static final UUID REFERENCE_TIME_INFO = UUID
                .fromString("00002a14-0000-1000-8000-00805f9b34fb");
        /**
          * @internal
          */
        public static final UUID TIME_WITH_DST = UUID
                .fromString("00002a11-0000-1000-8000-00805f9b34fb");

        /**
          * @internal
          */
        public static final UUID  TIME_UPDATE_CTRL_POINT = UUID
                .fromString("00002a16-0000-1000-8000-00805f9b34fb");

        /**
          * @internal
          */
        public static final UUID TIME_UPDATE_STATE = UUID
                .fromString("00002a17-0000-1000-8000-00805f9b34fb");

        /**
          * @internal
          */
        public static final UUID ALERT_LEVEL = UUID
                .fromString("00002a06-0000-1000-8000-00805f9b34fb");

        /**
          * @internal
          */
        public static final UUID TX_POWER_LEVEL = UUID
                .fromString("00002a07-0000-1000-8000-00805f9b34fb");

        public static final UUID ALERT_CONTROL_POINT = UUID
                .fromString("00002a44-0000-1000-8000-00805f9b34fb");

        public static final UUID UNREAD_ALERT_STATUS = UUID
                .fromString("00002a45-0000-1000-8000-00805f9b34fb");

        public static final UUID NEW_ALERT = UUID
                .fromString("00002a46-0000-1000-8000-00805f9b34fb");

        public static final UUID SUPPORTED_NEW_ALERT_CATEGORY = UUID
                .fromString("00002a47-0000-1000-8000-00805f9b34fb");

        public static final UUID SUPPORTED_UNREAD_ALERT_CATEGORY = UUID
                .fromString("00002a48-0000-1000-8000-00805f9b34fb");
    }

    /**
     * Definition of GATT characteristic descriptor UUIDs
     */
    public static final class Desc {

        /**
          * @internal
          */
        public static final UUID CLIENT_CHAR_CONFIG = UUID
                .fromString("00002902-0000-1000-8000-00805f9b34fb");

        public static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID
                .fromString("00002902-0000-1000-8000-00805f9b34fb");
    }

}
