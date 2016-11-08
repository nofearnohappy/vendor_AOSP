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

import org.xmlpull.v1.XmlPullParser;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.Resources.Theme;
import android.test.AndroidTestCase;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.util.Xml;

import com.mediatek.cts.resource.stub.R;


public class Resources_ThemeTest extends AndroidTestCase {

    private Resources.Theme mResTheme;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResTheme = getContext().getResources().newTheme();
    }

    public void testSetMethods() {
        // call a native method, and have no way to get the style
        mResTheme.applyStyle(R.raw.testmp3, false);
        // call a native method, this method is just for debug to the log
        mResTheme.dump(1, "hello", "world");
        // call a native method
        final Theme other = getContext().getTheme();
        mResTheme.setTo(other);
    }

    public void testObtainStyledAttributes() {
        final int[] attrs = new int[1];
        attrs[0] = R.raw.testmp3;

        TypedArray testTypedArray = mResTheme.obtainStyledAttributes(attrs);
        assertNotNull(testTypedArray);
        assertTrue(testTypedArray.length() > 0);
        testTypedArray.recycle();

        testTypedArray = mResTheme.obtainStyledAttributes(R.raw.testmp3, attrs);
        assertNotNull(testTypedArray);
        assertTrue(testTypedArray.length() > 0);
        testTypedArray.recycle();

        XmlPullParser parser = getContext().getResources().getXml(R.xml.colors);
        AttributeSet set = Xml.asAttributeSet(parser);
        attrs[0] = R.xml.colors;
        testTypedArray = mResTheme.obtainStyledAttributes(set, attrs, 0, 0);
        assertNotNull(testTypedArray);
        assertTrue(testTypedArray.length() > 0);
        testTypedArray.recycle();
    }

    public void testResolveAttribute() {
        final TypedValue value = new TypedValue();
        getContext().getResources().getValue(R.raw.testmp3, value, true);
        assertFalse(mResTheme.resolveAttribute(R.raw.testmp3, value, false));
    }

}
