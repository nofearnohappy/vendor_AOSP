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

package com.mediatek.bluetoothle.anp;

import android.bluetooth.BluetoothDevice;

public class ControlCommandChecker {
    private static final String TAG = "[BluetoothAns]ControlCommandChecker";

    private static final byte ENABLE_NEW_ALERT_COMMAND = 0;
    private static final byte ENABLE_UNREAD_ALERT_COMMAND = 1;
    private static final byte DISABLE_NEW_ALERT_COMMAND = 2;
    private static final byte DISABLE_UNREAD_ALERT_COMMAND = 3;
    private static final byte NOTIFY_NEW_ALERT_COMMAND = 4;
    private static final byte NOTIFY_UNREAD_ALERT_COMMAND = 5;

    private NotificationController mController;
    private BluetoothAnsCategoryManager mDetectorMananger;

    public ControlCommandChecker(NotificationController notificationController,
            BluetoothAnsCategoryManager detectorMananger) {
        mController = notificationController;
        mDetectorMananger = detectorMananger;
    }

    public boolean newControlCommand(byte[] newValue, BluetoothDevice device) {
        if (device == null) {
            return false;
        }
        String address = device.getAddress();
        if (address == null) {
            return false;
        }
        int type = 0;
        boolean isEnable = false;
        if (newValue[0] == ENABLE_NEW_ALERT_COMMAND || newValue[0] == DISABLE_NEW_ALERT_COMMAND) {
            type = NotificationController.CATEGORY_ENABLED_NEW;
        } else if (newValue[0] == ENABLE_UNREAD_ALERT_COMMAND
                || newValue[0] == DISABLE_UNREAD_ALERT_COMMAND) {
            type = NotificationController.CATEGORY_ENABLED_UNREAD;
        }
        if (newValue[0] == ENABLE_NEW_ALERT_COMMAND || newValue[0] == ENABLE_UNREAD_ALERT_COMMAND) {
            isEnable = true;
        } else if (newValue[0] == DISABLE_NEW_ALERT_COMMAND
                || newValue[0] == DISABLE_UNREAD_ALERT_COMMAND) {
            isEnable = false;
        }
        // newValue[1] is command type, newValue[2] is categoryId
        switch (newValue[0]) {

            case ENABLE_NEW_ALERT_COMMAND:
            case ENABLE_UNREAD_ALERT_COMMAND:
            case DISABLE_NEW_ALERT_COMMAND:
            case DISABLE_UNREAD_ALERT_COMMAND:
                mController.setAlertEnabled(address, type, newValue[1], isEnable);
                return true;
            case NOTIFY_NEW_ALERT_COMMAND:
                mDetectorMananger.alertImmediatelyByControl(device.getAddress(), newValue[1],
                        NotificationController.CATEGORY_ENABLED_NEW);
                return true;
            case NOTIFY_UNREAD_ALERT_COMMAND:
                mDetectorMananger.alertImmediatelyByControl(device.getAddress(), newValue[1],
                        NotificationController.CATEGORY_ENABLED_UNREAD);
                return true;
            default:
                return false;
        }
    }
}
