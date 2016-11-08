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

import com.mediatek.dm.data.IDmPersistentValues;
import com.redbend.vdm.PLDlPkg;
import com.redbend.vdm.VdmException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * implementation of red bend Porting layer PLDlPkg, the upgrade file.
 */
public class DmPLDlPkg implements PLDlPkg {

    private Context mContext;
    private static String sDeltaFileName = IDmPersistentValues.DELTA_FILE_NAME;
    private static final long MAX_STORAGE_SIZE = 100000000L;

    public DmPLDlPkg(Context context) {
        mContext = context;
    }

    /**
     * get the file's name.
     * @param vdmIdentifer file name
     * @return file name
     */
    public String getFilename(String vdmIdentifer) {
        return sDeltaFileName;
    }

    /**
     * get the file size limitation.
     * @return file size limitation
     */
    public long getMaxSize() {
        return MAX_STORAGE_SIZE;
    }

    /**
     * remove the upgrade package file.
     * @param fileName file name
     */
    public void remove(String fileName) {
        mContext.deleteFile(sDeltaFileName);
    }

    /**
     * write data into file.
     * @param filename file name
     * @param offset the start position to write
     * @param data data write in
     * @return write in data count bytes
     */
    public long writeChunk(String filename, long offset, byte[] data)
            throws VdmException {
        RandomAccessFile f = null;
        long size = 0;
        try {
            if (offset > 0 && !isFileExist(mContext, filename)) {
                throw new VdmException(VdmException.VdmError.MO_STORAGE);
            }
            f = new RandomAccessFile(
                    mContext.getFilesDir() + File.separator + filename, "rws");
            f.seek(offset); // Move to the correct position in the file
            f.write(data); // Write the buffer to the file
            size = offset + data.length;
            f.setLength(size);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new VdmException(VdmException.VdmError.MO_STORAGE);
        } catch (IOException e) {
            e.printStackTrace();
            throw new VdmException(VdmException.VdmError.MO_STORAGE);
        } finally {
            if (f != null) {
                try {
                    f.close();
                } catch (IOException e) {
                    Log.w("vDM Client", "Failed to close file " + filename);
                }
            }
        }
        return  size;
    }

    /**
     * get upgrade package file size.
     * @param filename file name
     * @throws VdmException red bend define error
     * @return file size
     */
    public long getPkgSize(String filename) throws VdmException {
        RandomAccessFile f = null;
        long size = 0;
        if (!isFileExist(mContext, getFilename(filename))) {
            size = 0;
        } else {
            try {
                f = new RandomAccessFile(mContext.getFilesDir() + File.separator
                        + getFilename(filename), "r");
                size = f.length();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                throw new VdmException(VdmException.VdmError.MO_STORAGE);
            } catch (IOException e) {
                e.printStackTrace();
                throw new VdmException(VdmException.VdmError.MO_STORAGE);
            } finally {
                if (f != null) {
                    try {
                        f.close();
                    } catch (IOException e) {
                        Log.w("vDM Client", "Failed to close file " + filename);
                    }
                }
            }
        }
        return  size;
    }

    /**
     * Get if a file exists.
     *
     * @param context
     *            Android context
     * @param filename
     *            The filename
     *
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
        sDeltaFileName = name;
    }
}
