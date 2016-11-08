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

package com.mediatek.configurecheck2;

import android.content.Context;

class MTKMTBFProvider extends CheckItemProviderBase {
    MTKMTBFProvider(Context c) {
         mArrayItems.add(new CheckBJTime(c, CheckItemKeySet.CI_BJ_DATA_TIME));
         mArrayItems.add(new CheckScreenOn(c, CheckItemKeySet.CI_SCREEN_ON_SLEEP));
         mArrayItems.add(new CheckScreenOn(c, CheckItemKeySet.CI_SCREEN_ON_UNLOCK));
         mArrayItems.add(new CheckScreenRotate(c, CheckItemKeySet.CI_SCREEN_ROTATE));
         mArrayItems.add(new CheckDataConnect(c, CheckItemKeySet.CI_DATA_CONNECT_ON));
         if (Utils.IS_LTE_PHONE) {
             mArrayItems.add(new CheckCurAPN(c, CheckItemKeySet.CI_CMNET));
         } else {
             mArrayItems.add(new CheckCurAPN(c, CheckItemKeySet.CI_CMWAP));
         }
         mArrayItems.add(new CheckWIFIControl(c, CheckItemKeySet.CI_WIFI_OFF));
         //mArrayItems.add(new CheckUrl(c, CheckItemKeySet.CI_BROWSER_URL));
         //mArrayItems.add(new CheckUrl(c, CheckItemKeySet.CI_BROWSER_URL_COMCAT));
         mArrayItems.add(new CheckShortcut(c, CheckItemKeySet.CI_SHORTCUT));
        // mArrayItems.add(new CheckDefaultStorage(c, CheckItemKeySet.CI_DEFAULTSTORAGE));
         mArrayItems.add(new CheckDefaultIME(c, CheckItemKeySet.CI_DEFAULTIME));
         mArrayItems.add(new CheckDefaultIME(c, CheckItemKeySet.CI_MANUL_CHECK));
         mArrayItems.add(new CheckDualSIMAsk(c, CheckItemKeySet.CI_MANUL_CHECK));
         mArrayItems.add(new CheckWebFont(c, CheckItemKeySet.CI_MANUL_CHECK));
         mArrayItems.add(new CheckLogger(c, CheckItemKeySet.CI_MANUL_CHECK));
         //mArrayItems.add(new CheckRedScreenOff(c, CheckItemKeySet.CI_MANUL_CHECK));
         mArrayItems.add(new CheckMMSSetting(c, CheckItemKeySet.CI_MANUL_CHECK));
         mArrayItems.add(new CheckEmailSetting(c, CheckItemKeySet.CI_MANUL_CHECK));
         mArrayItems.add(new CheckDefaultStorageSetting(c, CheckItemKeySet.CI_MANUL_CHECK));
         mArrayItems.add(new CheckRedScreen(c, CheckItemKeySet.CI_DISABLED_RED_SCREEN));
         mArrayItems.add(new CheckAddCallLog(c, CheckItemKeySet.CI_ADD_Mo_CALL_LOG));
    }
}
