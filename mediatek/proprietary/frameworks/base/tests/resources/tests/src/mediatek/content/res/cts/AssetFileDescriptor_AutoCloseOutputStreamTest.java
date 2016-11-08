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

/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mediatek.content.res.cts;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import android.content.res.AssetFileDescriptor;
import android.os.ParcelFileDescriptor;
import android.test.AndroidTestCase;

public class AssetFileDescriptor_AutoCloseOutputStreamTest extends AndroidTestCase {

    private static final String FILE_NAME = "testAssertFileDescriptorAutoCloseOutputStream";
    private static final int FILE_LENGTH = 100;
    private static final int FILE_END = -1;
    private static final byte[] FILE_DATA = new byte[] {
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08
            };

    private AssetFileDescriptor mAssetFileDes;

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        // As {@link AssetFileDescripter#createOutputStream()}
        // and {@link AssetFileDescripter#createInputStream()} doc,
        // the input and output stream will be auto closed when the AssetFileDescriptor closed.
        // Here is no need to close AutoCloseOutputStream, as AssetFileDescriptor will do it for us.
        if (mAssetFileDes != null) {
            mAssetFileDes.close();
        }
        getContext().deleteFile(FILE_NAME);
    }

    /*
     * Test AutoCloseOutputStream life circle.
     * 1. Write file data into test file.
     */
    public void testAutoCloseOutputStream() throws IOException {
        File file = new File(getContext().getFilesDir(), FILE_NAME);
        file.createNewFile();
        ParcelFileDescriptor fd = ParcelFileDescriptor.open(file,
                ParcelFileDescriptor.MODE_READ_WRITE);
        mAssetFileDes = new AssetFileDescriptor(fd, 0, FILE_LENGTH);
        AssetFileDescriptor.AutoCloseOutputStream outputStream =
                new AssetFileDescriptor.AutoCloseOutputStream(mAssetFileDes);
        outputStream.write(FILE_DATA[0]);
        outputStream.write(FILE_DATA, 1, 5);
        outputStream.write(FILE_DATA);
        outputStream.flush();
        mAssetFileDes.close();
        mAssetFileDes = null;

        fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE);
        mAssetFileDes = new AssetFileDescriptor(fd, 0, FILE_LENGTH);
        FileInputStream inputStream = mAssetFileDes.createInputStream();
        for (int i = 0; i < 6; i++) {
            assertEquals(FILE_DATA[i], inputStream.read());
        }
        for (int i = 0; i < FILE_DATA.length; i++) {
            assertEquals(FILE_DATA[i], inputStream.read());
        }
        assertEquals(FILE_END, inputStream.read());
        mAssetFileDes.close();
        mAssetFileDes = null;
    }
}
