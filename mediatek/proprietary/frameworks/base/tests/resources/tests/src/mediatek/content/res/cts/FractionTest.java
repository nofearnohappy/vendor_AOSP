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
import android.util.TypedValue;
import com.mediatek.cts.resource.stub.R;

public class FractionTest extends AndroidTestCase {

    private Resources mResources;
    private TypedValue mValue;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResources = mContext.getResources();
        mValue = new TypedValue();
    }

    @SmallTest
    public void testFractions() throws Exception {
        tryFraction(R.dimen.frac100perc, 1, 1, 1);
        tryFraction(R.dimen.frac1perc, 1, 1, .01f);
        tryFraction(R.dimen.fracp1perc, 1, 1, .001f);
        tryFraction(R.dimen.fracp01perc, 1, 1, .0001f);
        tryFraction(R.dimen.frac0perc, 1, 1, 0);
        tryFraction(R.dimen.frac1p1perc, 1, 1, .011f);
        tryFraction(R.dimen.frac100p1perc, 1, 1, 1.001f);
        tryFraction(R.dimen.frac25510perc, 1, 1, 255.1f);
        tryFraction(R.dimen.frac25610perc, 1, 1, 256.1f);
        tryFraction(R.dimen.frac6553510perc, 1, 1, 65535.1f);
        tryFraction(R.dimen.frac6553610perc, 1, 1, 65536.1f);

        tryFraction(R.dimen.frac100perc, 100, 1, 100);
        tryFraction(R.dimen.frac1perc, 100, 1, .01f * 100);
        tryFraction(R.dimen.fracp1perc, 100, 1, .001f * 100);
        tryFraction(R.dimen.fracp01perc, 100, 1, .0001f * 100);
        tryFraction(R.dimen.frac0perc, 100, 1, 0);
        tryFraction(R.dimen.frac1p1perc, 100, 1, .011f * 100);
        tryFraction(R.dimen.frac100p1perc, 100, 1, 1.001f * 100);
        tryFraction(R.dimen.frac25510perc, 100, 1, 255.1f * 100);
        tryFraction(R.dimen.frac25610perc, 100, 1, 256.1f * 100);
        tryFraction(R.dimen.frac6553510perc, 100, 1, 65535.1f * 100);
        tryFraction(R.dimen.frac6553610perc, 100, 1, 65536.1f * 100);

        tryFraction(R.dimen.frac100pperc, 100, 2, 2);
        tryFraction(R.dimen.frac1pperc, 100, 2, .01f * 2);
        tryFraction(R.dimen.fracp1pperc, 100, 2, .001f * 2);
        tryFraction(R.dimen.fracp01pperc, 100, 2, .0001f * 2);
        tryFraction(R.dimen.frac0pperc, 100, 2, 0);
        tryFraction(R.dimen.frac1p1pperc, 100, 2, .011f * 2);
        tryFraction(R.dimen.frac100p1pperc, 100, 2, 1.001f * 2);
        tryFraction(R.dimen.frac25510pperc, 100, 2, 255.1f * 2);
        tryFraction(R.dimen.frac25610pperc, 100, 2, 256.1f * 2);
        tryFraction(R.dimen.frac6553510pperc, 100, 2, 65535.1f * 2);
        tryFraction(R.dimen.frac6553610pperc, 100, 2, 65536.1f * 2);
    }

    private void tryFraction(final int resid, final float base, final float pbase,
            final float expected) {
        mResources.getValue(resid, mValue, true);
        float res = mValue.getFraction(base, pbase);
        float diff = Math.abs(expected - res);
        float prec = expected * 1e-4f;
        if (prec < 1e-5f) {
            prec = 1e-5f;
        }

        assertFalse("Expecting value " + expected + " got " + res + ": in resource 0x"
                + Integer.toHexString(resid) + " " + mValue, diff > prec);
    }
}

