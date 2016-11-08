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
import com.mediatek.mediatekdm.mdm.PLFile;
import com.mediatek.mediatekdm.mdm.PLStorage.AccessMode;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class DmPLDeltaFile implements PLFile {

    @SuppressWarnings("deprecation")
    public DmPLDeltaFile(String fileName, Context context, AccessMode accessMode)
            throws IOException {
        mContext = context;
        mDeltaFileName = fileName;
        mTempFileName = "temp_" + mDeltaFileName;
        Log.i(TAG.PL, "[DmPLDeltaFile] fileName = " + mDeltaFileName + ", mTempFileName = "
                + mTempFileName);
        mAccessMode = accessMode;
        switch (mAccessMode) {
            case READ:
                mInputStream = mContext.openFileInput(mDeltaFileName);
                break;
            case WRITE:
                mOutputStream = mContext.openFileOutput(mTempFileName, Context.MODE_WORLD_READABLE);
                break;
            default:
                break;
        }
    }

    public void close(boolean commitClose) throws IOException {
        Log.d(TAG.PL, "[DmPLDeltaFile] commitClose =  " + commitClose + ", mAccessMode = "
                + mAccessMode);
        if (!commitClose && mAccessMode == AccessMode.READ) {
            mInputStream.close();
        }
        if (commitClose && mAccessMode == AccessMode.WRITE) {
            mOutputStream.close();
            File deltaSrc = mContext.getFileStreamPath(mTempFileName);
            File deltaDst = mContext.getFileStreamPath(mDeltaFileName);
            Log.i(TAG.PL, "[DmPLDeltaFile] before rename");
            Log.d(TAG.PL, "[DmPLDeltaFile] Rename " + deltaSrc.getAbsolutePath() + " to "
                    + deltaDst.getAbsolutePath());
            boolean isOk = deltaSrc.renameTo(deltaDst);
            if (!isOk) {
                Log.e(TAG.PL, "[DmPLDeltaFile] Could not rename " + deltaSrc.getName() + " to "
                        + deltaDst.getName() + "!!!");
            } else {
                Log.d(TAG.PL, "[DmPLDeltaFile] Rename succeeded.");
            }
            Log.i(TAG.PL, "[DmPLDeltaFile] after rename");
            if (deltaSrc.exists()) {
                Log.i(TAG.PL, "[DmPLDeltaFile] rename failed. keep temp file.");
                deltaDst.delete();
            }
        }
    }

    public int read(byte[] buffer) throws IOException {
        int ret = 0;
        if (mAccessMode == AccessMode.WRITE) {
            throw new IOException("[DmPLDeltaFile] Attempt to read from an output stream");
        }
        try {
            ret = mInputStream.read(buffer);
        } catch (IOException e) {
            Log.e(TAG.PL, "[DmPLDeltaFile] Read delta file error!");
        }
        return ret;
    }

    public void write(byte[] data) throws IOException {
        if (mAccessMode == AccessMode.READ) {
            throw new IOException("[DmPLDeltaFile] Attempt to write to an input stream");
        }
        try {
            Log.i(TAG.PL, "[DmPLDeltaFile] The data write to delta.zip  is [" + data + "]");
            mOutputStream.write(data);
            mOutputStream.flush();
            FileDescriptor fd = mOutputStream.getFD();
            fd.sync();
        } catch (IOException e) {
            Log.e(TAG.PL, "[DmPLDeltaFile] Write delta file error!");
        }
        return;
    }

    private FileInputStream mInputStream;
    private FileOutputStream mOutputStream;
    private Context mContext;
    private AccessMode mAccessMode;
    private String mTempFileName = null;
    private String mDeltaFileName = null;
}
