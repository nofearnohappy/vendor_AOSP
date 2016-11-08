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

package com.mediatek.dm.scomo;

import android.os.Handler;
import android.util.Log;

import com.mediatek.dm.DmConst.TAG;
import com.mediatek.dm.DmService;
import com.mediatek.dm.data.IDmPersistentValues;
import com.redbend.vdm.DownloadDescriptor;
import com.redbend.vdm.scomo.ScomoOperationResult;
import com.redbend.vdm.scomo.VdmScomoDp;
import com.redbend.vdm.scomo.VdmScomoDpHandler;

public class DmScomoDpHandler implements VdmScomoDpHandler {
    private static final String CLASS_TAG = TAG.SCOMO + "/DpHandler";

    private static DmScomoDpHandler sHandler = new DmScomoDpHandler();

    public static DmScomoDpHandler getInstance() {
        return sHandler;
    }

    public boolean confirmDownload(VdmScomoDp dp, DownloadDescriptor dd) {
        Log.i(CLASS_TAG, "confirmdownload");
        Handler h = DmService.getInstance().getScomoHandler();
        h.sendMessage(h.obtainMessage(IDmPersistentValues.MSG_SCOMO_CONFIRM_DOWNLOAD, new Object[] { dp, dd }));
        return false;
    }

    public boolean confirmInstall(VdmScomoDp dp) {
        Log.i(CLASS_TAG, "confirm install");
        Handler h = DmService.getInstance().getScomoHandler();
        h.sendMessage(h.obtainMessage(IDmPersistentValues.MSG_SCOMO_CONFIRM_INSTALL, dp));
        return false;
    }

    public ScomoOperationResult executeInstall(VdmScomoDp dp, String deliveryPkgPath, boolean isActive) {
        Log.i(CLASS_TAG, "exec install");
        Handler h = DmService.getInstance().getScomoHandler();
        h.sendMessage(h.obtainMessage(IDmPersistentValues.MSG_SCOMO_EXEC_INSTALL, dp));
        return new ScomoOperationResult();
    }

}