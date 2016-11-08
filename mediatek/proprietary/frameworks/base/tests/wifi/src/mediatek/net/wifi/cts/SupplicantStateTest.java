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

package mediatek.net.wifi.cts;

import android.net.wifi.SupplicantState;
import android.test.AndroidTestCase;

public class SupplicantStateTest extends AndroidTestCase {

    public void testIsValidState() {
        if (!WifiFeature.isWifiSupported(getContext())) {
            // skip the test if WiFi is not supported
            return;
        }
        assertTrue(SupplicantState.isValidState(SupplicantState.DISCONNECTED));
        assertTrue(SupplicantState.isValidState(SupplicantState.INACTIVE));
        assertTrue(SupplicantState.isValidState(SupplicantState.SCANNING));
        assertTrue(SupplicantState.isValidState(SupplicantState.ASSOCIATING));
        assertTrue(SupplicantState.isValidState(SupplicantState.ASSOCIATED));
        assertTrue(SupplicantState.isValidState(SupplicantState.FOUR_WAY_HANDSHAKE));
        assertTrue(SupplicantState.isValidState(SupplicantState.GROUP_HANDSHAKE));
        assertTrue(SupplicantState.isValidState(SupplicantState.COMPLETED));
        assertTrue(SupplicantState.isValidState(SupplicantState.DORMANT));
        assertFalse(SupplicantState.isValidState(SupplicantState.UNINITIALIZED));
        assertFalse(SupplicantState.isValidState(SupplicantState.INVALID));

        // M: Add for test
        assertTrue(SupplicantState.isHandshakeState(SupplicantState.AUTHENTICATING));
        assertTrue(SupplicantState.isHandshakeState(SupplicantState.ASSOCIATING));
        assertTrue(SupplicantState.isHandshakeState(SupplicantState.ASSOCIATED));
        assertTrue(SupplicantState.isHandshakeState(SupplicantState.FOUR_WAY_HANDSHAKE));
        assertTrue(SupplicantState.isHandshakeState(SupplicantState.GROUP_HANDSHAKE));
        assertFalse(SupplicantState.isHandshakeState(SupplicantState.COMPLETED));
        assertFalse(SupplicantState.isHandshakeState(SupplicantState.DISCONNECTED));
        assertFalse(SupplicantState.isHandshakeState(SupplicantState.INTERFACE_DISABLED));
        assertFalse(SupplicantState.isHandshakeState(SupplicantState.INACTIVE));
        assertFalse(SupplicantState.isHandshakeState(SupplicantState.SCANNING));
        assertFalse(SupplicantState.isHandshakeState(SupplicantState.DORMANT));
        assertFalse(SupplicantState.isHandshakeState(SupplicantState.UNINITIALIZED));
        assertFalse(SupplicantState.isHandshakeState(SupplicantState.INVALID));
        try {
            SupplicantState.isHandshakeState(SupplicantState.valueOf("ERROR"));
        } catch (IllegalArgumentException e) {
        }
    }

}
