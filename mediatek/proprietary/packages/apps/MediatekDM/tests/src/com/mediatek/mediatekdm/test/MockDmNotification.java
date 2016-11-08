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

package com.mediatek.mediatekdm.test;

import android.content.Intent;
import android.util.Log;

import com.mediatek.mediatekdm.DmConst;
import com.mediatek.mediatekdm.DmConst.NotificationInteractionType;
import com.mediatek.mediatekdm.DmNotification;
import com.mediatek.mediatekdm.DmService;
import com.mediatek.mediatekdm.test.server.IServiceTest;

import junit.framework.TestCase;

public class MockDmNotification extends DmNotification {
    private DmService mService;
    private IServiceTest mTestCase;
    private final int mNotificationResponse;
    private final int mAlertResponse;

    public static final int RESPONSE_INVALID = -1;
    public static final int RESPONSE_FALSE = 0;
    public static final int RESPONSE_TRUE = 1;
    public static final int RESPONSE_TIMEOUT = 2;

    private int mNotificationCount = 0;
    private int mAlertCount = 0;

    public MockDmNotification(DmService service, IServiceTest testCase, int notificationResponse,
            int alertResponse) {
        super(service);
        Log.d("MDMTest/MockDmNotification", "MockDmNotification");
        mService = service;
        mTestCase = testCase;
        mNotificationResponse = notificationResponse;
        mAlertResponse = alertResponse;
    }

    @Override
    public void showNotification(int type) {
        Intent serviceIntent = null;
        switch (type) {
            case NotificationInteractionType.TYPE_NOTIFICATION_VISIBLE:
                mTestCase.getChecklist().fillCheckItem("notification_type_" + mNotificationCount,
                        type);
                mNotificationCount += 1;
                clear();
                break;
            case NotificationInteractionType.TYPE_NOTIFICATION_INTERACT:
                mTestCase.getChecklist().fillCheckItem("notification_type_" + mNotificationCount,
                        type);
                mNotificationCount += 1;
                serviceIntent = new Intent(mService, DmService.class);
                if (mNotificationResponse == RESPONSE_FALSE) {
                    serviceIntent.setAction(DmConst.IntentAction.DM_NOTIFICATION_RESPONSE);
                    serviceIntent.putExtra("response", false);
                } else if (mNotificationResponse == RESPONSE_TRUE) {
                    serviceIntent.setAction(DmConst.IntentAction.DM_NOTIFICATION_RESPONSE);
                    serviceIntent.putExtra("response", true);
                } else if (mNotificationResponse == RESPONSE_TIMEOUT) {
                    serviceIntent.setAction(DmConst.IntentAction.DM_NOTIFICATION_TIMEOUT);
                    serviceIntent.putExtra("type", type);
                } else {
                    TestCase.assertEquals(RESPONSE_INVALID, mNotificationResponse);
                }
                mTestCase.startServiceEmulation(serviceIntent);
                break;
            case NotificationInteractionType.TYPE_ALERT_1101:
                mTestCase.getChecklist().fillCheckItem("alert_type_" + mAlertCount, type);
                mAlertCount += 1;
                serviceIntent = new Intent(mService, DmService.class);
                if (mAlertResponse == RESPONSE_FALSE) {
                    serviceIntent.setAction(DmConst.IntentAction.DM_ALERT_RESPONSE);
                    serviceIntent.putExtra("response", false);
                } else if (mAlertResponse == RESPONSE_TRUE) {
                    serviceIntent.setAction(DmConst.IntentAction.DM_ALERT_RESPONSE);
                    serviceIntent.putExtra("response", true);
                } else if (mAlertResponse == RESPONSE_TIMEOUT) {
                    serviceIntent.setAction(DmConst.IntentAction.DM_ALERT_TIMEOUT);
                    serviceIntent.putExtra("type", type);
                } else {
                    TestCase.assertEquals(RESPONSE_INVALID, mAlertResponse);
                }
                mTestCase.startServiceEmulation(serviceIntent);
                break;
            default:
                if (type != NotificationInteractionType.TYPE_INVALID) {
                    TestCase.fail("Invalid NotificationInteractionType: " + type);
                }
        }
    }

    @Override
    public void clear() {
        Log.d("MDMTest/MockDmNotification", "clear");
        mService.cancelNiaAlertTimeout();
    }
}
