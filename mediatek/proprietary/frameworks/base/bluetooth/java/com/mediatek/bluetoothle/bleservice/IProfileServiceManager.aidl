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

package com.mediatek.bluetoothle.bleservice;

import com.mediatek.bluetoothle.bleservice.IProfileServiceManagerCallback;

/**
 * Interface for IProfileServiceManager
 *
 * @hide
 */
interface IProfileServiceManager {
    /**
     * Get all supported Ble profile servers in the platform
     *
     * @return array of id in the BleProfile.java
     */
    int[] getCurSupportedServerProfiles();

    /**
     * Query the status of the server by using profile id defined in the BleProfile.java
     *
     * @param profile the profile id
     *
     * @return current state of the profile server
     */
    int getProfileServerState(int profile);

    /**
     * Enable/Disable Bluetooth LE services in the background mode
     *
     * @param bEnabled if true, it enables the background mode
     *          otherwise, it disables the background mode
     */
    boolean setBackgroundMode(boolean bEnabled);


    /**
     * Querty the status of the background mode
     *
     * @return true, if background mode is enabled
     *         otherwise, if background mode is disabled
     */
    boolean isBackgroundModeEnabled();

    /**
     * Launch Bluetooth LE services when background mode is disabled
     * Pre-condition: Bluetooth must be turn on
     *
     * @return true,if this operation starts
     *         false,if this operation fails
     */
    boolean launchServices();

    /**
     * Shutdown Bluetooth LE services when background mode is disabled
     * Pre-condition: Bluetooth must be turn on
     *
     * @return true,if this operation starts
     *         false,if this operation fails
     */
    boolean shutdownServices();

    /**
     * register callback
     *
     * @param callback callback
     */
    void registerCallback(in IProfileServiceManagerCallback callback);

    /**
     * unregister callback
     *
     * @param callback callback
     */
    void unregisterCallback(in IProfileServiceManagerCallback callback);
}
