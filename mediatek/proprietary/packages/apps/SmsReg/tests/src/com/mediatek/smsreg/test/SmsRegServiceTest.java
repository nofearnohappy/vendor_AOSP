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

package com.mediatek.smsreg.test;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.test.ServiceTestCase;
import android.test.mock.MockContentResolver;
import android.test.mock.MockContext;
import android.util.Log;

import com.mediatek.smsreg.BlackListUnit;
import com.mediatek.smsreg.SmsBuilder;
import com.mediatek.smsreg.SmsRegApplication;
import com.mediatek.smsreg.SmsRegConst;
import com.mediatek.smsreg.SmsRegService;
import com.mediatek.smsreg.XmlGenerator;
import com.mediatek.smsreg.test.util.MockPlatformManager;
import com.mediatek.smsreg.test.util.MockSmsRegUtil;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class SmsRegServiceTest extends ServiceTestCase<SmsRegService> {
    private static final String TAG = "SmsReg/SmsRegServiceTest";

    private SmsRegService mService = null;
    private TestContext mTestContext = null;
    private TestResolver mTestResolver = new TestResolver();

    public static final String CMCC_IMSI_0 = MockPlatformManager.CMCC_IMSI_0;
    public static final String CMCC_IMSI_1 = MockPlatformManager.CMCC_IMSI_1;
    public static final String MESSAGE_CONTENT = "IMEI:" + MockPlatformManager.VALUE_IMEI + "/"
            + MockPlatformManager.VALUE_MANUFACTURER + "/" + MockPlatformManager.VALUE_PRODUCT
            + "/" + MockPlatformManager.VALUE_VERSION;

    public SmsRegServiceTest() {
        super(SmsRegService.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mTestContext = new TestContext();
        setContext(mTestContext);

        MockSmsRegUtil.preparePlatformManager((SmsRegApplication) getApplication());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        mTestContext = null;
    }

    /**
     * Operation: bind service Check: service is null
     */
    public void test00() {
        MockSmsRegUtil.formatLog(TAG, "test00");
        Intent intent = new Intent();
        intent.setClass(getContext(), SmsRegService.class);
        IBinder service = bindService(intent);

        assertNull(service);
    }

    /**
     * Operation: Start with "" action Check: NA
     */
    public void test01() {
        MockSmsRegUtil.formatLog(TAG, "test01");

        startSmsRegService(new Intent(""));
    }

    /**
     * Operation: boot -> display dialog -> user reject Check: 1. Activity is started 2. Sim 0's
     * IMSI is blocked
     */
    public void test02() {
        MockSmsRegUtil.formatLog(TAG, "test02");

        startSmsRegService(new Intent(SmsRegConst.ACTION_BOOT_COMPLETED));

        // Reset black file
        BlackListUnit blackListUnit = (BlackListUnit) getServiceMember("mBlackListUnit");
        blackListUnit.resetBlackFile();

        startSmsRegService(new Intent(SmsRegConst.ACTION_DISPLAY_DIALOG));

        // Check: mSendSimId should be 0 and invoke startActivity
        int simId = (Integer) getServiceMember("mSendSimId");
        assertEquals(simId, 0);
        assertTrue(mTestContext.isActivityStarted());

        // User reject to send message
        startWithResponseDialog(false);

        // Check: Sim 0's IMSI is blocked
        int index = blackListUnit.getMinAvailId(new String[] { CMCC_IMSI_0 });
        Log.i(TAG, "Available index is " + index);
        assertEquals(-1, index);
    }

    /**
     * Operation: boot -> display dialog -> user agree -> send finish Check: 1. State of receivers
     * 2. Message content & mIsSendMsg 3. SavedImsi & broadcast
     */
    public void test03() {
        MockSmsRegUtil.formatLog(TAG, "test03");

        startSmsRegService(new Intent(SmsRegConst.ACTION_BOOT_COMPLETED));

        try {
            checkBootUp();

            // Reset black file
            BlackListUnit blackListUnit = (BlackListUnit) getServiceMember("mBlackListUnit");
            blackListUnit.resetBlackFile();

            startSmsRegService(new Intent(SmsRegConst.ACTION_DISPLAY_DIALOG));

            startWithResponseDialog(true);

            // Use reflection to invoke some methods
            invokeMethods();
            checkMessageSend();

            startWithFinishSend(CMCC_IMSI_0);

            // Check: save IMSI CMCC_IMSI_1
            MockPlatformManager platformManager = (MockPlatformManager) getServiceMember("mPlatformManager");
            String savedImsi = platformManager.getSavedImsi();
            Log.i(TAG, "Saved imsi is " + savedImsi);
            assertTrue(CMCC_IMSI_0.equals(savedImsi));

            // Check: broadcast "DM_SMSREG_MESSAGE_NEW"
            assertTrue(SmsRegConst.DM_SMSREG_MESSAGE_NEW.equals(mTestContext.getBroadcastIntent()
                    .getAction()));

        } catch (ClassNotFoundException e) {
            Log.i(TAG, "* ClassNotFoundException:" + e.getMessage());
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            Log.i(TAG, "* NoSuchMethodException:" + e.getMessage());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            Log.i(TAG, "* IllegalAccessException:" + e.getMessage());
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            Log.i(TAG, "* InvocationTargetException:" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Operation: boot -> modify operator -> boot -> retry send Check: Message content & mIsSendMsg
     */
    public void test04() {
        MockSmsRegUtil.formatLog(TAG, "test04");

        startSmsRegService(new Intent(SmsRegConst.ACTION_BOOT_COMPLETED));
        setOperatorId("cu");
        startSmsRegService(new Intent(SmsRegConst.ACTION_BOOT_COMPLETED));

        // Operation: retry to send registered message
        startSmsRegService(new Intent(SmsRegConst.ACTION_RETRY_SEND_SMS));

        checkMessageSend();
    }

    /**
     * Operation: boot -> modify operator -> boot -> sim_state_change -> modify isSendMsg ->
     * phoneStateListener Check: Message content & mIsSendMsg
     */
    public void test05() {
        MockSmsRegUtil.formatLog(TAG, "test05");

        startSmsRegService(new Intent(SmsRegConst.ACTION_BOOT_COMPLETED));

        setOperatorId("cu");
        startSmsRegService(new Intent(SmsRegConst.ACTION_BOOT_COMPLETED));

        // Operation: all sim ready, try to send message
        startWithStateChanged();
        checkMessageSend();

        // modify isSendMsg to false
        setIsSendMsg(false);

        // Operation: sim 0 state in service, send message
        PhoneStateListener[] phoneStateListener = (PhoneStateListener[]) getServiceMember("mPhoneStateListener");

        ServiceState serviceState = new ServiceState();
        serviceState.setState(ServiceState.STATE_IN_SERVICE);
        phoneStateListener[0].onServiceStateChanged(serviceState);

        checkMessageSend();
    }

    /**
     * Operation: boot -> modify operator -> boot -> invoke Receiver.onReceive() Check: Service is
     * started with expected intent
     */
    public void test06() {
        MockSmsRegUtil.formatLog(TAG, "test06");

        startSmsRegService(new Intent(SmsRegConst.ACTION_BOOT_COMPLETED));

        setOperatorId("cu");
        startSmsRegService(new Intent(SmsRegConst.ACTION_BOOT_COMPLETED));

        try {
            checkBootUp();

            String action = SmsRegConst.ACTION_SIM_STATE_CHANGED;
            BroadcastReceiver stateReceiver = (BroadcastReceiver) getServiceMember("mSimStateReceiver");
            stateReceiver.onReceive(mTestContext, new Intent(action));
            assertTrue(mTestContext.getStartServiceIntent().getAction().equals(action));

            action = SmsRegConst.ACTION_FINISH_SEND_SMS;
            BroadcastReceiver receivedReceiver = (BroadcastReceiver) getServiceMember("mSmsReceivedReceiver");
            receivedReceiver.onReceive(mTestContext, new Intent(action));
            assertTrue(mTestContext.getStartServiceIntent().getAction().equals(action));

        } catch (ClassNotFoundException e) {
            Log.i(TAG, "* ClassNotFoundException:" + e.getMessage());
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            Log.i(TAG, "* NoSuchMethodException:" + e.getMessage());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            Log.i(TAG, "* IllegalAccessException:" + e.getMessage());
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            Log.i(TAG, "* InvocationTargetException:" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Check: message content is MESSAGE_CONTENT
     */
    private void checkMessageSend() {
        boolean isSendMsg = (Boolean) getServiceMember("mIsSendMsg");
        assertTrue(isSendMsg);

        MockPlatformManager platformManager = (MockPlatformManager) getServiceMember("mPlatformManager");
        String message = platformManager.getMessageInfo();
        Log.i(TAG, "send message " + message);
        assertTrue(MESSAGE_CONTENT.equals(message));
    }

    /**
     * Check: state of *Receiver after boot
     */
    private void checkBootUp() throws ClassNotFoundException, NoSuchMethodException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        Log.i(TAG, "checkBootUp");
        String operator = (String) getServiceMember("mOperatorId");
        Log.i(TAG, "Operator is " + operator);

        Object stateReceiver = getServiceMember("mSimStateReceiver");
        Object receivedReceiver = getServiceMember("mSmsReceivedReceiver");

        if (operator.equalsIgnoreCase("cu")) {
            assertNotNull(stateReceiver);
            assertNotNull(receivedReceiver);
        } else {
            assertNull(stateReceiver);
            assertNull(receivedReceiver);
        }
    }

    /**
     * Invoke methods of SmsBuilder & BlackListUnit
     */
    private void invokeMethods() throws ClassNotFoundException, SecurityException,
            NoSuchMethodException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {

        new SmsBuilder();

        XmlGenerator mXmlG = (XmlGenerator) getServiceMember("mXmlG");
        SmsBuilder.getContentInfo(mXmlG, "getimsi", 0);
        SmsBuilder.getContentInfo(mXmlG, "getvendor", 0);
        SmsBuilder.getContentInfo(mXmlG, "getOem", 0);
        SmsBuilder.getContentInfo(mXmlG, "invalid_cmd", 0);

        // No use, only invoke
        new BlackListUnit();
    }

    /**
     * Reflection to change member "mOperatorId"
     */
    private void setOperatorId(String operatorId) {
        try {
            Field field = mService.getClass().getDeclaredField("mOperatorId");
            field.setAccessible(true);
            field.set(mService, operatorId);

        } catch (NoSuchFieldException e) {
            Log.e(TAG, "* NoSuchFieldException " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            Log.e(TAG, "* IllegalAccessException " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Reflection to change member "isSendMsg"
     */
    private void setIsSendMsg(boolean isSendMsg) {
        try {
            Field field = mService.getClass().getDeclaredField("mIsSendMsg");
            field.setAccessible(true);
            field.set(mService, isSendMsg);

        } catch (NoSuchFieldException e) {
            Log.e(TAG, "* NoSuchFieldException " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            Log.e(TAG, "* IllegalAccessException " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Start service via intent "ACTION_RESPONSE_DIALOG" with user's response
     */
    private void startWithResponseDialog(boolean result) {
        Intent intent = new Intent(SmsRegConst.ACTION_RESPONSE_DIALOG);
        intent.putExtra(SmsRegConst.EXTRA_IS_NEED_SEND, result);
        startSmsRegService(intent);
    }

    /**
     * Start service via intent "ACTION_FINISH_SEND_SMS" with result code & IMSI info
     */
    private void startWithFinishSend(String imsi) {
        Intent intent = new Intent(SmsRegConst.ACTION_FINISH_SEND_SMS);
        intent.putExtra(SmsRegConst.EXTRA_RESULT_CODE, Activity.RESULT_OK);
        intent.putExtra(SmsRegConst.EXTRA_IMSI, imsi);
        startSmsRegService(intent);
    }

    /**
     * Start service via intent "ACTION_FINISH_SEND_SMS" with result code & IMSI info
     */
    private void startWithStateChanged() {
        Intent intent = new Intent(SmsRegConst.ACTION_SIM_STATE_CHANGED);
        intent.putExtra(SmsRegConst.KEY_ICC_STATE, SmsRegConst.VALUE_ICC_LOADED);
        startSmsRegService(intent);
    }

    /**
     * Start SmsRegService and init "mService"
     */
    private void startSmsRegService(Intent intent) {
        intent.setClass(mTestContext, SmsRegService.class);
        startService(intent);
        mService = getService();
    }

    /**
     * Get the declared field of SmsRegService
     */
    private Object getServiceMember(String fieldName) {
        Log.i(TAG, "Get service member " + fieldName);
        Object object = null;

        try {
            Field field = mService.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            object = field.get(mService);

        } catch (NoSuchFieldException e) {
            Log.e(TAG, "* NoSuchFieldException " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            Log.e(TAG, "* IllegalAccessException " + e.getMessage());
            e.printStackTrace();
        }
        return object;
    }

    class TestContext extends MockContext {
        private List<BroadcastReceiver> mReceiverList = new ArrayList<BroadcastReceiver>();
        private boolean mStartActivity = false;
        private Intent mStartServiceIntent = null;
        private Intent mBroadcaseIntent = null;

        @Override
        public ApplicationInfo getApplicationInfo() {
            return new ApplicationInfo();
        }

        @Override
        public void startActivity(Intent intent) {
            mStartActivity = true;
            Log.i(TAG, "StartActivity with intent " + intent);
        }

        @Override
        public ContentResolver getContentResolver() {
            return mTestResolver;
        }

        @Override
        public File getFilesDir() {
            File filesDir = new File("/data/data/" + getPackageName() + "/files");
            return filesDir;
        }

        @Override
        public String getPackageName() {
            return "com.mediatek.smsreg";
        }

        @Override
        public ComponentName startService(Intent intent) {
            Log.i(TAG, "StartService with intent " + intent);
            mStartServiceIntent = intent;
            return new ComponentName("com.mediatek.smsreg", "SmsRegService");
        }

        @Override
        public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
            mReceiverList.add(receiver);
            return new Intent("com.mediatek.smsreg.test.registerReceiver");
        }

        @Override
        public void unregisterReceiver(BroadcastReceiver receiver) {
            mReceiverList.remove(receiver);
        }

        @Override
        public void sendBroadcast(Intent intent) {
            Log.i(TAG, "SendBroadcast " + intent);
            mBroadcaseIntent = intent;
        }

        public Intent getBroadcastIntent() {
            return mBroadcaseIntent;
        }

        private Intent getStartServiceIntent() {
            return mStartServiceIntent;
        }

        public boolean isActivityStarted() {
            return mStartActivity;
        }
    }

    class TestResolver extends MockContentResolver {
    }
}
