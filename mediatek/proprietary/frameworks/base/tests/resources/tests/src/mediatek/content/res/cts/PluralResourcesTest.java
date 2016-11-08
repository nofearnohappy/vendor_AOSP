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

import java.util.Locale;

import junit.framework.Assert;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.mediatek.cts.resource.stub.R;

public class PluralResourcesTest extends AndroidTestCase {
    public static boolean DEBUG = false;
    private static final String TAG = "PluralResourcesTest";

    private Resources mResources;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResources = mContext.getResources();
    }

    private Resources resourcesForLanguage(final String lang) {
        final Configuration config = new Configuration();
        config.updateFrom(mResources.getConfiguration());
        config.locale = new Locale(lang);
        return new Resources(mResources.getAssets(), mResources.getDisplayMetrics(), config);
    }

    @SmallTest
    public void testPlurals() {
        CharSequence cs;
        final Resources res = resourcesForLanguage("en");

        cs = res.getQuantityText(R.plurals.plurals_test, 0);
        if (DEBUG) {
            Log.d(TAG, "english 0 cs=" + cs);
        }
        Assert.assertEquals(cs.toString(), "Some dogs");

        cs = res.getQuantityText(R.plurals.plurals_test, 1);
        if (DEBUG) {
            Log.d(TAG, "english 1 cs=" + cs);
        }
        Assert.assertEquals(cs.toString(), "A dog");

        cs = res.getQuantityText(R.plurals.plurals_test, 2);
        Assert.assertEquals(cs.toString(), "Some dogs");

        cs = res.getQuantityText(R.plurals.plurals_test, 5);
        Assert.assertEquals(cs.toString(), "Some dogs");

        cs = res.getQuantityText(R.plurals.plurals_test, 500);
        Assert.assertEquals(cs.toString(), "Some dogs");
    }

    @SmallTest
    public void testCzech() {
        CharSequence cs;
        final Resources res = resourcesForLanguage("cs");

        cs = res.getQuantityText(R.plurals.plurals_test, 0);
        if (DEBUG) {
            Log.d(TAG, "czech 0 cs=" + cs);
        }
        Assert.assertEquals(cs.toString(), "Some Czech dogs");

        cs = res.getQuantityText(R.plurals.plurals_test, 1);
        if (DEBUG) {
            Log.d(TAG, "czech 1 cs=" + cs);
        }
        Assert.assertEquals(cs.toString(), "A Czech dog");

        cs = res.getQuantityText(R.plurals.plurals_test, 2);
        if (DEBUG) {
            Log.d(TAG, "czech 2 cs=" + cs);
        }
        Assert.assertEquals(cs.toString(), "Few Czech dogs");

        cs = res.getQuantityText(R.plurals.plurals_test, 5);
        Assert.assertEquals(cs.toString(), "Some Czech dogs");

        cs = res.getQuantityText(R.plurals.plurals_test, 500);
        Assert.assertEquals(cs.toString(), "Some Czech dogs");
    }
}
