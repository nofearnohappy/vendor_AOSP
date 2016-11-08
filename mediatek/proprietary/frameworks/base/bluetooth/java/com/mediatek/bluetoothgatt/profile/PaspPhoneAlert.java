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
 * MediaTek Inc. (C) 2015. All rights reserved.
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


package com.mediatek.bluetoothgatt.profile;

// Customized Start: Import ........................................................................
import android.content.Context;
import android.util.Log;

import com.mediatek.bluetoothgatt.GattUuid;
import com.mediatek.bluetoothgatt.characteristic.CharacteristicBase;
import com.mediatek.bluetoothgatt.service.Pass;
//........................................................................ Customized End: Import //

/**
 * Public API for the GATT Phone Alert Status Profile(Server).
 *
 * Name: Phone Alert Status
 * Type: org.bluetooth.profile.phone_alert_status
 * Last Modified: None
 * Revision: None
 * Role: PhoneAlert
 */
public class PaspPhoneAlert extends ServerBase {
    private static final boolean DBG = true;
    private static final String TAG = "PaspPhoneAlert";

    /**
     * Create a PaspPhoneAlert object and init value.
     *
     * @param context App context
     */
    public PaspPhoneAlert(Context context) {
        super(context);

        if (DBG) {
            Log.d(TAG, "PaspPhoneAlert()");
        }
    }

    @Override
    protected void loadServicesConfig() {
        addService(new Pass());
        cfgService(GattUuid.SRVC_PASS).setSupport(true);
        cfgCharacteristic(GattUuid.SRVC_PASS, GattUuid.CHAR_ALERT_STATUS)
                .setSupport(true);
        cfgCharacteristic(GattUuid.SRVC_PASS, GattUuid.CHAR_RINGER_CONTROL_POINT)
                .setSupport(true);
        cfgCharacteristic(GattUuid.SRVC_PASS, GattUuid.CHAR_RINGER_SETTING)
                .setSupport(true);
    }

    /**
     * Send a notification or indication that a local characteristic has been
     * updated.
     *
     * @param characteristic The local characteristic that has been updated
     */
    public void notifyPassAlertStatus(
            CharacteristicBase characteristic) {
        if (DBG) {
            Log.d(TAG, "notifyPassAlertStatus()");
        }

        notify(GattUuid.SRVC_PASS,
                GattUuid.CHAR_ALERT_STATUS,
                characteristic.getValue());
    }

    /**
     * Send a notification or indication that a local characteristic has been
     * updated.
     *
     * @param characteristic The local characteristic that has been updated
     */
    public void notifyPassRingerSetting(
            CharacteristicBase characteristic) {
        if (DBG) {
            Log.d(TAG, "notifyPassRingerSetting()");
        }

        notify(GattUuid.SRVC_PASS,
                GattUuid.CHAR_RINGER_SETTING,
                characteristic.getValue());
    }

}

