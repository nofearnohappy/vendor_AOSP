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

package com.mediatek.mediatekdm.test.server;

import com.mediatek.mediatekdm.DmConst.NotificationInteractionType;
import com.mediatek.mediatekdm.test.MockDmNotification;
import com.mediatek.mediatekdm.test.server.MockServerService.CMCCTestRequest;

public class ParameterRetrievalRetryTestCase extends MockServerTestCase {

    public ParameterRetrievalRetryTestCase() {
        super(CMCCTestRequest.PARAMETERS_RETRIEVAL_RETRY, 2, 3, 60);
    }

    @Override
    protected String getTag() {
        return "MDMTest/ParameterRetrievalRetryTestCase";
    }

    public void testcase01() {
        testTemplate("testcase01", 0, NotificationInteractionType.TYPE_INVALID,
                MockDmNotification.RESPONSE_INVALID, NotificationInteractionType.TYPE_ALERT_1101,
                MockDmNotification.RESPONSE_TRUE);
    }

    public void testcase02() {
        testTemplate("testcase02", 1, NotificationInteractionType.TYPE_INVALID,
                MockDmNotification.RESPONSE_INVALID, NotificationInteractionType.TYPE_ALERT_1101,
                MockDmNotification.RESPONSE_TRUE);
    }

    public void testcase03() {
        testTemplate("testcase03", 2, NotificationInteractionType.TYPE_NOTIFICATION_VISIBLE,
                MockDmNotification.RESPONSE_INVALID, NotificationInteractionType.TYPE_ALERT_1101,
                MockDmNotification.RESPONSE_TRUE);
    }

    public void testcase04() {
        testTemplate("testcase04", 3, NotificationInteractionType.TYPE_NOTIFICATION_INTERACT,
                MockDmNotification.RESPONSE_TRUE, NotificationInteractionType.TYPE_ALERT_1101,
                MockDmNotification.RESPONSE_TRUE);
    }
}
