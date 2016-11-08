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

package com.mediatek.dm;

import android.content.Context;
import android.util.Log;

import com.mediatek.dm.DmConst.TAG;
import com.redbend.vdm.PLFile;
import com.redbend.vdm.PLStorage.AccessMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.SyncFailedException;

public class DmPLDmTreeFile implements PLFile {

    private static FileInputStream sInputStream;
    private static FileOutputStream sOutputStream;
    private static Context sDmTreeContext;
    private static AccessMode sAccessMode;
    private static String sDmTreeFileName;
    private static String sTempTreeFileName;
    private static DmPLDmTreeFile sPlTreeinstance;
    private static final String TEMP_FILE_HEADER = "temp_";

    private DmPLDmTreeFile(String fileName, Context context, AccessMode accessMode) throws FileNotFoundException {
        sDmTreeContext = context;
        sDmTreeFileName = fileName;
        sTempTreeFileName = TEMP_FILE_HEADER + sDmTreeFileName;
        sAccessMode = accessMode;

        switch (accessMode) {
        case READ:
            sInputStream = sDmTreeContext.openFileInput(sDmTreeFileName);
            Log.i(TAG.PL, "Dm tree file opened");
            break;
        case WRITE:
            sOutputStream = sDmTreeContext.openFileOutput(sTempTreeFileName, 0);
            Log.i(TAG.PL, "Dm tree file opened");
            break;
        default:
            break;
        }
    }

    public static synchronized DmPLDmTreeFile getInstance(String fileName, Context context, AccessMode accessMode) {
        if (sPlTreeinstance == null) {
            try {
                sPlTreeinstance = new DmPLDmTreeFile(fileName, context, accessMode);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        return sPlTreeinstance;
    }

    public synchronized void close(boolean commitClose) throws IOException {
        try {
            if (!commitClose && sAccessMode == AccessMode.READ && sInputStream != null) {
                sInputStream.close();
                Log.i(TAG.PL, "Dm tree file closed");
            } else if (commitClose && sAccessMode == AccessMode.WRITE && sOutputStream != null) {
                try {
                    sOutputStream.getFD().sync();
                    File tempTree = sDmTreeContext.getFileStreamPath(sTempTreeFileName);
                    File tree = sDmTreeContext.getFileStreamPath(sDmTreeFileName);
                    Log.d(TAG.PL, "about to rename tree.xml");
                    boolean isOk = tempTree.renameTo(tree);
                    if (!isOk) {
                        Log.e(TAG.PL, new StringBuilder("Could not rename ").append(tempTree.getName()).append(" to ")
                                .append(tree.getName()).append("!!!").toString());
                    }
                } catch (SyncFailedException e) {
                    Log.e(TAG.PL, "mOutputStream sync fail");
                    e.printStackTrace();
                }
                sOutputStream.close();
                Log.i(TAG.PL, "Dm tree file closed");
            } else {
                Log.e(TAG.PL, "There are something wrong in closing delta file");
            }

        } catch (IOException e) {
            Log.e(TAG.PL, "There is IOException in close dm tree file");
        } finally {
            sOutputStream = null;
            sInputStream = null;
            sPlTreeinstance = null;
        }

    }

    public synchronized int read(byte[] buffer) throws IOException {
        int ret = 0;
        // only input stream can read
        if (sPlTreeinstance == null) {
            Log.w(TAG.PL, "read plTreeinstance is null,create new");
            try {
                sPlTreeinstance = new DmPLDmTreeFile(sDmTreeFileName, sDmTreeContext, sAccessMode);
            } catch (FileNotFoundException e) {
                Log.e(TAG.PL, e.getMessage());
            }
        }
        if (sAccessMode == AccessMode.WRITE || sInputStream == null) {
            throw new IOException("Attempt to read from an output stream");
        }
        try {
            ret = sInputStream.read(buffer);
        } catch (IOException e) {
            Log.e(TAG.PL, "Read dm tree file error!");
            e.printStackTrace();
        }
        return ret;
    }

    public synchronized void write(byte[] data) throws IOException {
        // only output stream can write
        if (sPlTreeinstance == null) {
            Log.w(TAG.PL, "write plTreeinstance is null,create new");
            try {
                sPlTreeinstance = new DmPLDmTreeFile(sDmTreeFileName, sDmTreeContext, sAccessMode);
            } catch (FileNotFoundException e) {
                Log.e(TAG.PL, e.getMessage());
            }
        }
        if (sAccessMode == AccessMode.READ || sOutputStream == null) {
            throw new IOException("Attempt to write to an input stream");
        }
        try {
            sOutputStream.write(data);
        } catch (IOException e) {
            Log.e(TAG.PL, "Write dm tree file error!");
            e.printStackTrace();
        }
        return;
    }
}
