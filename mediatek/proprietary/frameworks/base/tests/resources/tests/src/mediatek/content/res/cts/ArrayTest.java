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

import android.content.res.Resources;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.mediatek.cts.resource.stub.R;

public class ArrayTest extends AndroidTestCase {
    private Resources mResources;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResources = mContext.getResources();
    }

    private void checkEntry(final int resid, final int index, final Object res,
            final Object expected) {
        assertEquals("in resource 0x" + Integer.toHexString(resid)
                + " at index " + index, expected, res);
    }

    private void checkStringArray(final int resid, final String[] expected) {
        final String[] res = mResources.getStringArray(resid);
        assertEquals(res.length, expected.length);
        for (int i = 0; i < expected.length; i++) {
            checkEntry(resid, i, res[i], expected[i]);
        }
    }

    private void checkTextArray(final int resid, final String[] expected) {
        final CharSequence[] res = mResources.getTextArray(resid);
        assertEquals(res.length, expected.length);
        for (int i = 0; i < expected.length; i++) {
            checkEntry(resid, i, res[i], expected[i]);
        }
    }

    private void checkIntArray(final int resid, final int[] expected) {
        final int[] res = mResources.getIntArray(resid);
        assertEquals(res.length, expected.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals("in resource 0x" + Integer.toHexString(resid)
                    + " at index " + i, expected[i], res[i]);
        }
    }

    @SmallTest
    public void testStrings() throws Exception {
        checkStringArray(R.array.strings, new String[] {"zero", "1", "here"});
        checkTextArray(R.array.strings, new String[] {"zero", "1", "here"});
        checkStringArray(R.array.integers, new String[] {null, null, null});
        checkTextArray(R.array.integers, new String[] {null, null, null});
    }

    @SmallTest
    public void testIntegers() throws Exception {
        checkIntArray(R.array.strings, new int[] {0, 0, 0});
        checkIntArray(R.array.integers, new int[] {0, 1, 101});
    }
}
