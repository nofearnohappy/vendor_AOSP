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

import com.redbend.vdm.PLFile;
import com.redbend.vdm.VdmLog;

import java.io.*;

/**
 * Implements output files, only.
 */
public class DmPLOutputFile implements PLFile {
    FileOutputStream mStream;
    private Context mContext;
    private String mName;

    /**
     * Constructor.
     *
     * Opens a file for writing.
     *
     * @param   name    The file name
     * @param   context Android context
     *
     * @throws  FileNotFoundException   An error if file does not exist
     */
    DmPLOutputFile(String name, Context context) throws FileNotFoundException {
        mName = new String(name);
        mStream = context.openFileOutput("tmp_" + mName, 0);
        mContext = context;
    }

    /**
     * Throws an I/O error message, since you shouldn't read from an output file.
     * @param buf read buffer
     * @return nothing
     * @throws IOException always throw
     */
    public int read(byte[] buf) throws IOException {
        throw new IOException("Attempt to read from an output stream");
    }

    /**
     * write to file stream.
     * @param data write in data
     * @throws IOException if error
     */
    public void write(byte[] data) throws IOException {
        mStream.write(data);
    }

    /**
     * close the file stream.
     * @param commit red bend define flag
     * @throws IOException if error
     */
    public void close(boolean commit) throws IOException {
        mStream.getFD().sync();
        mStream.close();
        if (commit == true) {
            File src = mContext.getFileStreamPath("tmp_" + mName);
            File dst = mContext.getFileStreamPath(mName);
            boolean isOk = src.renameTo(dst);
            if (!isOk) {
                VdmLog.e("vDM Client", "Could't rename " + src.getName() + " to " + dst.getName());
            }
            if (src.exists()) {
                src.delete();
            }
        }
    }
}

