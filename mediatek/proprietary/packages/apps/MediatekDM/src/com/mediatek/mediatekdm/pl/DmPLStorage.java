/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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

package com.mediatek.mediatekdm.pl;

import android.util.Log;

import com.mediatek.mediatekdm.DmApplication;
import com.mediatek.mediatekdm.DmConst;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.DmOperation;
import com.mediatek.mediatekdm.DmOperationManager;
import com.mediatek.mediatekdm.DmService;
import com.mediatek.mediatekdm.IDmComponent;
import com.mediatek.mediatekdm.PlatformManager;
import com.mediatek.mediatekdm.mdm.PLFile;
import com.mediatek.mediatekdm.mdm.PLStorage;

import java.io.File;
import java.io.IOException;

public class DmPLStorage implements PLStorage {
    private DmService mService;

    public DmPLStorage(DmService service) {
        mService = service;
        Log.i(TAG.PL, "DmPLStorage created.");
    }

    public void delete(ItemType itemType) {
        Log.d(TAG.PL, "DmPLStorage delete() with type " + itemType);
        String fileName = null;
        if (ItemType.DMTREE == itemType) {
            fileName = DmConst.Path.DM_TREE_FILE;
        } else if (ItemType.DLRESUME == itemType) {
            final DmOperation operation = DmOperationManager.getInstance().current();
            for (IDmComponent component : DmApplication.getInstance().getComponents()) {
                if (component.acceptOperation(null, operation)) {
                    fileName = component.getDlResumeFilename();
                }
            }
        } else {
            Log.e(TAG.PL, "The PL file type is wrong!");
        }
        if (fileName != null) {
            Log.d(TAG.PL, "DmPLStorage delete() " + fileName);
            mService.deleteFile(fileName);
        }
    }

    public PLFile open(ItemType fileType, AccessMode accessMode) throws IOException {
        String fileName = null;
        PLFile file = null;
        if (ItemType.DMTREE == fileType) {
            fileName = DmConst.Path.DM_TREE_FILE;
            Log.i(TAG.PL, "open DMTREE: " + fileName);
            Log.i(TAG.PL, "mService context is " + mService.getFilesDir());
            file = DmPLDmTreeFile.getInstance(fileName, mService, accessMode);
        } else if (ItemType.DLRESUME == fileType) {
            final DmOperation operation = DmOperationManager.getInstance().current();
            for (IDmComponent component : DmApplication.getInstance().getComponents()) {
                if (component.acceptOperation(null, operation)) {
                    fileName = component.getDlResumeFilename();
                }
            }
            Log.i(TAG.PL, "open DLRESUME: " + fileName);
            File f = new File(PlatformManager.getInstance().getPathInData(mService, fileName));
            if (!f.exists()) {
                Log.w(TAG.PL, f.getAbsolutePath() + " does not exist, create a new one!");
                f.createNewFile();
            }
            file = new DmPLDeltaFile(fileName, mService, accessMode);
        } else {
            Log.e(TAG.PL, "The PL file type is wrong!");
        }

        Log.i(TAG.PL, "open type: " + fileType + " return: " + file);
        return file;
    }

}
