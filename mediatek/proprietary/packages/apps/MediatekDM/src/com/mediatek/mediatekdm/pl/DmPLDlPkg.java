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

import android.content.Context;
import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.PLDlPkg;

import java.io.IOException;
import java.io.RandomAccessFile;

public class DmPLDlPkg implements PLDlPkg {

    public DmPLDlPkg(Context context) {
        mPlDlContext = context;
    }

    public String getFilename(String mdmIdentifer) {
        return sFumoFileName;
    }

    public int getMaxSize() {
        return MAX_STORAGE_SIZE;
    }

    public void remove(String fileName) {
        mPlDlContext.deleteFile(sFumoFileName);
    }

    public int writeChunk(String filename, int offset, byte[] data) throws MdmException {
        Log.d(TAG.PL, "+writeChunk(" + filename + ")");
        RandomAccessFile f = null;
        long size = -1;
        try {
            if (offset > 0 && !isFileExist(mPlDlContext, filename)) {
                throw new MdmException(MdmException.MdmError.MO_STORAGE);
            }
            f = new RandomAccessFile(mPlDlContext.getFilesDir() + "/" + filename, "rws");
            f.seek(offset);
            f.write(data);
            size = offset + data.length;
        } catch (IOException e) {
            throw new MdmException(MdmException.MdmError.MO_STORAGE);
        } finally {
            if (f != null) {
                try {
                    f.close();
                } catch (IOException e) {
                    Log.w(TAG.PL, "Failed to close file " + filename);
                }
            }
        }
        return (int) size;

    }

    public int getPkgSize(String filename) throws MdmException {
        RandomAccessFile f = null;
        long size;
        if (!isFileExist(mPlDlContext, getFilename(filename))) {
            size = 0;
        } else {
            try {
                f = new RandomAccessFile(mPlDlContext.getFilesDir() + "/" + getFilename(filename),
                        "r");
                size = f.length();
            } catch (IOException e) {
                throw new MdmException(MdmException.MdmError.MO_STORAGE);
            } finally {
                if (f != null) {
                    try {
                        f.close();
                    } catch (IOException e) {
                        Log.w(TAG.PL, "Failed to close file " + filename);
                    }
                }
            }
        }
        return (int) size;
    }

    /**
     * Get if a file exists.
     *
     * @param context
     *        Android context
     * @param filename
     *        The filename
     * @return true if it exists, false otherwise
     */
    public static boolean isFileExist(Context context, String filename) {
        String[] fnames = context.fileList();
        for (String fname : fnames) {
            if (fname.equals(filename)) {
                return true;
            }
        }
        return false;
    }

    public static void setDeltaFileName(String name) {
        Log.d(TAG.PL, "setDeltaFileName(" + name + ")");
        sFumoFileName = name;
    }

    private Context mPlDlContext;
    private static String sFumoFileName = null;
    private static final int MAX_STORAGE_SIZE = 100000000;
}
