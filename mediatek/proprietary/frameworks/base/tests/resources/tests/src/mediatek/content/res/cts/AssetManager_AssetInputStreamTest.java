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
 * Copyright (C) 2008 The Android Open Source Project
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

import java.io.IOException;

import android.content.res.AssetManager;
import android.test.AndroidTestCase;

public class AssetManager_AssetInputStreamTest extends AndroidTestCase {
    private AssetManager.AssetInputStream mAssetInputStream;
    private final String CONTENT_STRING = "OneTwoThreeFourFiveSixSevenEightNineTen";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mAssetInputStream = (AssetManager.AssetInputStream) mContext.getAssets().open("text.txt");
    }

    public void testClose() throws IOException {
        mAssetInputStream.close();

        try {
            mAssetInputStream.read();
            fail("should throw exception");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testGetAssetInt() {
        try {
            // getAssetInt is no longer supported.
            mAssetInputStream.getAssetInt();
            fail();
        } catch (UnsupportedOperationException expected) {
        }
    }

    public void testMarkReset() throws IOException {
        assertTrue(mAssetInputStream.markSupported());
        final int readlimit = 10;
        final byte[] bytes = CONTENT_STRING.getBytes();
        for (int i = 0; i < readlimit; i++) {
            assertEquals(bytes[i], mAssetInputStream.read());
        }
        mAssetInputStream.mark(readlimit);
        mAssetInputStream.reset();
        for (int i = 0; i < readlimit; i++) {
            assertEquals(bytes[i + readlimit], mAssetInputStream.read());
        }
    }

    public void testReadMethods() throws IOException {
        // test available()
        final byte[] bytes = CONTENT_STRING.getBytes();
        int len = mAssetInputStream.available();
        int end = -1;
        assertEquals(CONTENT_STRING.length(), len);
        for (int i = 0; i < len; i++) {
            assertEquals(bytes[i], mAssetInputStream.read());
        }
        assertEquals(end, mAssetInputStream.read());

        // test read(byte[])
        mAssetInputStream.reset();
        int dataLength = 10;
        byte[] data = new byte[dataLength];
        int ret = mAssetInputStream.read(data);
        assertEquals(dataLength, ret);
        for (int i = 0; i < dataLength; i++) {
            assertEquals(bytes[i], data[i]);
        }
        data = new byte[len - dataLength];
        assertEquals(len - dataLength, mAssetInputStream.read(data));
        for (int i = 0; i < len - dataLength; i++) {
            assertEquals(bytes[i + dataLength], data[i]);
        }
        assertEquals(end, mAssetInputStream.read(data));

        // test read(bytep[], int, int)
        mAssetInputStream.reset();
        int offset = 0;
        ret = mAssetInputStream.read(data, offset, dataLength);
        assertEquals(dataLength, ret);
        for (int i = offset; i < ret; i++) {
            assertEquals(bytes[i], data[offset + i]);
        }
        mAssetInputStream.reset();
        offset = 2;
        ret = mAssetInputStream.read(data, offset, dataLength);
        assertEquals(dataLength, ret);
        for (int i = offset; i < ret; i++) {
            assertEquals(bytes[i], data[offset + i]);
        }
        data = new byte[len + offset];
        ret = mAssetInputStream.read(data, offset, len);
        assertEquals(len - dataLength, ret);
        for (int i = offset; i < ret; i++) {
            assertEquals(bytes[i + dataLength], data[offset + i]);
        }
        assertEquals(end, mAssetInputStream.read(data, offset, len));
        // test len is zero,
        mAssetInputStream.reset();
        assertEquals(0, mAssetInputStream.read(data, 0, 0));
        // test skip(int)
        int skipLenth = 8;
        mAssetInputStream.reset();
        mAssetInputStream.skip(skipLenth);
        assertEquals(CONTENT_STRING.charAt(skipLenth), mAssetInputStream.read());

        // test read(byte[] b), b is null
        try {
            mAssetInputStream.read(null);
            fail("should throw NullPointerException ");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            mAssetInputStream.read(null, 0, mAssetInputStream.available());
            fail("should throw NullPointerException ");
        } catch (NullPointerException e) {
            // expected
        }
        // test read(bytep[], int, int): offset is negative,
        try {
            data = new byte[10];
            mAssetInputStream.read(data, -1, mAssetInputStream.available());
            fail("should throw IndexOutOfBoundsException ");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        // test read(bytep[], int, int): len+offset greater than data length
        try {
            data = new byte[10];
            assertEquals(0, mAssetInputStream.read(data, 0, data.length + 2));
            fail("should throw IndexOutOfBoundsException ");
        } catch (IndexOutOfBoundsException e) {
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mAssetInputStream.close();
    }

}
