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

package com.mediatek.bluetoothle.ext;

import android.content.Context;
import android.util.Log;

import com.mediatek.common.MPlugin;

/**
   * AnsExtentionManager managers ANS detector extention,
   * defined in com.mediatek.bluetoothle.ext .jar,  to void that BluetoothLe depends on Plugin.
   */
public class BLEExtentionManager {
    private static final String TAG = "[BluetoothAns]AnsExtentionManager";
    private static final boolean DBG = true;

    public static final int BLE_ANS_EXTENTION = 0x0001;

    private static IBluetoothLeAnsExtension sAnsExtension = null;

    /**
     * Host can initial Extention object by this method, Just pass the application
     * context or others`
     *
     * @param context
     */
    public static void initExtentions(Context context) {

        // Duplicate message plugin
        sAnsExtension = (IBluetoothLeAnsExtension) MPlugin.createInstance(
                IBluetoothLeAnsExtension.class.getName(), context);
        if (sAnsExtension == null) {
            sAnsExtension = new BluetoothLeAnsExtensionImpl();
        }
        if (DBG) {
            Log.d("@M_" + TAG, "className " + IBluetoothLeAnsExtension.class.getName());
            Log.d("@M_" + TAG, "operator mAnsExtension = " + sAnsExtension);
        }

    }

    /**
     * Get the Plugin by Plugin type defined in AnsExtentionManager {
     * BLE_ANS_EXTENTION 0x0001 }
     *
     * @param type : type
     * @return: plugin object
     */
    public static Object getExtentionObject(int type, Context context) {
        Object obj = null;
        if (DBG) {
            Log.d("@M_" + TAG, "getExtentionObject, type = " + type);
        }
        switch (type) {
            case BLE_ANS_EXTENTION:
                if (sAnsExtension == null) {
                    initExtentions(context);
                }
                obj = sAnsExtension;
                break;
            default:
                if (DBG) {
                    Log.e("@M_" + TAG, "getExtentionObject, type = " + type + " doesn't exist");
                }
        }
        return obj;
    }

}

