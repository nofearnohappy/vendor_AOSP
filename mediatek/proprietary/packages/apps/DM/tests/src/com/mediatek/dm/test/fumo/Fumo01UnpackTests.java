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

package com.mediatek.dm.test.fumo;

import android.test.AndroidTestCase;
import android.util.Log;

import com.mediatek.dm.DmConst;
import com.mediatek.dm.fumo.FOTADeltaFiles;
import com.mediatek.dm.test.DmTestHelper;

import java.io.File;

public class Fumo01UnpackTests extends AndroidTestCase {
    private static final String TAG = "[TestFOTAUnpack]";

    private static final String FOTA_PACKAGE_DIR = DmConst.PathName.PATH_IN_DATA;
    private static final String FOTA_PACKAGE_PATH = DmConst.PathName.DELTA_ZIP_FILE;

    //DELTA_VERIFY_OK
    public void testFOTAPackageVerify1() {
        Boolean copyResult = DmTestHelper.copyFromAsserts(this.getTestContext(), "delta.zip",
                FOTA_PACKAGE_DIR);
        Log.v(TAG, "[testFOTAPackageVerify1]copyResult: " + copyResult);

        File fotaFile = new File(FOTA_PACKAGE_PATH);
        if (!fotaFile.exists()) {
            Log.d(TAG, "[testFOTAPackageVerify1]FOTA package doesn't exist, skip this case.");
            return;
        }

        int ret = FOTADeltaFiles.unpackAndVerify(FOTA_PACKAGE_PATH);
        Log.d(TAG, "FOTA package parsing result = " + ret);

        assertEquals(FOTADeltaFiles.DELTA_VERIFY_OK, ret);
    }

    //DELTA_CHECKSUM_ERR
    public void testFOTAPackageVerify2() {
        Boolean copyResult = DmTestHelper.copyFromAsserts(this.getTestContext(), "delta_sum_err.zip",
                FOTA_PACKAGE_DIR);
        Log.v(TAG, "[testFOTAPackageVerify2]copyResult: " + copyResult);

        File fotaFile = new File(FOTA_PACKAGE_PATH);
        if (!fotaFile.exists()) {
            Log.d(TAG, "[testFOTAPackageVerify2]FOTA package doesn't exist, skip this case.");
            return;
        }

        int ret = FOTADeltaFiles.unpackAndVerify(FOTA_PACKAGE_PATH);
        Log.d(TAG, "[testFOTAPackageVerify2]FOTA package parsing result = " + ret);

        assertEquals(FOTADeltaFiles.DELTA_CHECKSUM_ERR, ret);
    }


    //DELTA_INVALID_ZIP
    public void testFOTAPackageVerify3() {
        Boolean copyResult = DmTestHelper.copyFromAsserts(this.getTestContext(), "fumo",
                FOTA_PACKAGE_DIR);
        Log.v(TAG, "[testFOTAPackageVerify3]copyResult: " + copyResult);

        File fotaFile = new File(FOTA_PACKAGE_PATH);
        if (!fotaFile.exists()) {
            Log.d(TAG, "[testFOTAPackageVerify3]FOTA package doesn't exist, skip this case.");
            return;
        }

        int ret = FOTADeltaFiles.unpackAndVerify(FOTA_PACKAGE_PATH);
        Log.d(TAG, "[testFOTAPackageVerify3]FOTA package parsing result = " + ret);

        assertEquals(FOTADeltaFiles.DELTA_INVALID_ZIP, ret);
    }


    //DELTA_NO_STORAGE
//    public void testFOTAPackageVerify4() {
//        Boolean copyResult = DmTestHelper.copyFromAsserts(this.getTestContext(), "delta.zip",
//                FOTA_PACKAGE_DIR);
//        Log.v(TAG, "copyResult: " + copyResult);
//
//        File fotaFile = new File(FOTA_PACKAGE_PATH);
//        if (!fotaFile.exists()) {
//            Log.d(TAG, "FOTA package doesn't exist, skip this case.");
//            return;
//        }
//
//        int ret = FOTADeltaFiles.unpackAndVerify(FOTA_PACKAGE_PATH);
//        Log.d(TAG, "FOTA package parsing result = " + ret);
//
//        assertEquals(FOTADeltaFiles.DELTA_NO_STORAGE, ret);
//    }
}
