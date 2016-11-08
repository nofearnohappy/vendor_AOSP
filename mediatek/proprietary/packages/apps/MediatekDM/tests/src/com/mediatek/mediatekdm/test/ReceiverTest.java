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

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.test.AndroidTestCase;
import android.util.Log;

import com.mediatek.mediatekdm.DmApplication;
import com.mediatek.mediatekdm.DmOperationManager;
import com.mediatek.mediatekdm.DmReceiver;

import java.lang.reflect.Field;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class ReceiverTest extends AndroidTestCase {
    private class ReceiverMockContext extends MockRenamingDelegatingContext {
        public ReceiverMockContext() {
            super(DmApplication.getInstance());
            Log.w(TAG, "ReceiverMockContext.<init>()");
        }

        @Override
        public ComponentName startService(Intent intent) {
            notifyStartService(intent);
            return null;
        }
    }

    public static final String TAG = "MDMTest/ReceiverTest";
    private static final int REGISTERED_SIM = 0;
    private static final int WAIT_TIMEOUT = 5;

    private BlockingQueue<Intent> mBlockingQueue;
    private DmReceiver mDmReceiver;
    private ReceiverMockContext mMockContext;


    public void testcase07() {
        Intent intent = new Intent("other.intent");
        mDmReceiver.onReceive(mMockContext, intent);
        Intent resultIntent = new Intent(intent);
        assertTrue("Intent should be forwarded as is: " + resultIntent,
                waitStartService(resultIntent));
    }

    private void notifyStartService(Intent intent) {
        try {
            Log.d(TAG, "notifyStartService intent is " + intent);
            mBlockingQueue.put(intent);
            Log.d(TAG, "intent has been put to queue");
        } catch (InterruptedException e) {
            throw new Error(e);
        }
    }

    private void setOperationManager(DmOperationManager manager) {
        try {
            Field field = DmOperationManager.class.getDeclaredField("sInstance");
            field.setAccessible(true);
            field.set(DmOperationManager.class, manager);
            field.setAccessible(false);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private boolean waitStartService(Intent expectedIntent) {
        Intent receivedIntent = null;
        try {
            receivedIntent = mBlockingQueue.poll(WAIT_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            new Error(e);
        }
        Log.d(TAG, "waitStartService expectedIntent is " + expectedIntent);
        Log.d(TAG, "waitStartService receivedIntent is " + receivedIntent);
        if (receivedIntent == null && expectedIntent == null) {
            return true;
        }
        if (receivedIntent == null
                || !expectedIntent.getAction().equals(receivedIntent.getAction())) {
            Log.e(TAG, "Actions do not match.");
            return false;
        }
        Bundle expectedExtras = expectedIntent.getExtras();
        Bundle extras = receivedIntent.getExtras();
        if (extras == null && expectedExtras == null) {
            return true;
        } else if (extras == null && expectedExtras != null) {
            Log.e(TAG, "Extras is missing.");
            return false;
        } else if (extras != null && expectedExtras == null) {
            return true;
        } else {
            for (String key : expectedExtras.keySet()) {
                if (!extras.containsKey(key)) {
                    Log.e(TAG, "Key " + key + " is missing.");
                    return false;
                }
                if (!extras.get(key).equals(expectedExtras.get(key))) {
                    Log.e(TAG, "Value of " + key + " is wrong: " + extras.get(key));
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    protected void setUp() throws Exception {
        Log.d(TAG, "super.setUp()");
        super.setUp();
        Log.d(TAG, "ReceiverTest.setUp()");
        mMockContext = new ReceiverMockContext();
        MockPlatformManager.setUp();
        mDmReceiver = new DmReceiver();
        mBlockingQueue = new ArrayBlockingQueue<Intent>(1);
    }

    @Override
    protected void tearDown() throws Exception {
        Log.d(TAG, "ReceiverTest.tearDown()");
        MockPlatformManager.tearDown();
        mMockContext = null;
        mDmReceiver = null;
        setOperationManager(null);
        Log.d(TAG, "super.tearDown()");
        super.tearDown();
    }
}
