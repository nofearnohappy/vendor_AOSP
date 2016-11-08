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

package com.mediatek.ppl.test;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.os.Handler;
import android.os.PowerManager;
import android.test.ServiceTestCase;
import android.test.mock.MockContentResolver;
import android.test.mock.MockContext;
import android.test.mock.MockResources;
import android.util.Log;

import com.android.internal.telephony.IccCardConstants;
import com.mediatek.ppl.ControlData;
import com.mediatek.ppl.MessageManager;
import com.mediatek.ppl.MessageManager.PendingMessage;
import com.mediatek.ppl.PplApplication;
import com.mediatek.ppl.PplManager;
import com.mediatek.ppl.PplService;
import com.mediatek.ppl.PplService.Intents;
import com.mediatek.ppl.R;
import com.mediatek.ppl.test.util.MockDataUtil;
import com.mediatek.ppl.test.util.MockPplUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PplServiceTest extends ServiceTestCase<PplService> {
    private static final String KEY_STATE = IccCardConstants.INTENT_KEY_ICC_STATE;
    private static final String KEY_SIM_ID = "simId";
    private static final String STATE_ABSENT = IccCardConstants.INTENT_VALUE_ICC_ABSENT;
    private static final String STATE_READY = IccCardConstants.INTENT_VALUE_ICC_READY;
    private static final String TAG = "PPL/PplServiceTest";

    private PplService mService = null;
    private TestResource mTestResource = new TestResource();
    private TestResolver mTestResolver = new TestResolver();
    private TestContext mTestContext = null;


    public PplServiceTest() {
        super(PplService.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mTestContext = new TestContext();
        setContext(mTestContext);

        MockDataUtil.preparePlatformManager((PplApplication) getApplication(), mTestContext);
        MockDataUtil.writeSampleControlData();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        mTestContext = null;
    }

    /**
     * Operation: receive intent (action is "")
     * Check: NA
     * */
    public void test00() {
        MockPplUtil.formatLog(TAG, "test00");

        new PplService.Intents();
        new MessageManager.Type();

        startPplService(mTestContext, new Intent(""));

        // Receive intent ACTION_SCREEN_OFF, no check
        Intent intent = new Intent(Intent.ACTION_SCREEN_OFF);
        receiveIntent(mTestContext, intent);
    }

    /**
     * Operation: start service via ACTION_SIM_STATE_CHANGED (only sim1)
     * Check: check sim lock flow
     * */
    public void test01() {
        MockPplUtil.formatLog(TAG, "test01");

        startPplService(mTestContext, buildSimStateIntent(STATE_READY, 0));
        startPplService(mTestContext, buildSimStateIntent(STATE_ABSENT, 1));

        assertTrue(MockDataUtil.loadControlData().isSimLocked());

        checkUnlock(0);
    }

    /**
     * Operation: receive intent ACTION_SIM_STATE_CHANGED (only sim2)
     * Check: check sim lock flow
     * */
    public void test02() {
        MockPplUtil.formatLog(TAG, "test02");
        startPplService(mTestContext, buildSimStateIntent(STATE_ABSENT, 0));
        startPplService(mTestContext, buildSimStateIntent(STATE_READY, 1));

        assertTrue(MockDataUtil.loadControlData().isSimLocked());

        checkUnlock(1);
    }

    /**
     * Operation: trigger device lock request
     * Check: intent "INTENT_NOTIFY_LOCK" has been sent, status of feature
     * */
    public void test03() {
        MockPplUtil.formatLog(TAG, "test03");
        startPplService(mTestContext, buildOperationIntent(MessageManager.Type.LOCK_REQUEST, 0));

        Intent lockIntent = mTestContext.getBroadcast();
        assertTrue(mTestContext.isSticky());
        assertTrue(PplService.Intents.INTENT_NOTIFY_LOCK.equals(lockIntent.getAction()));
        assertFalse(lockIntent.getBooleanExtra(PplService.Intents.NOTIFICATION_KEY_IS_SIM_LOCK, true));

        assertTrue(MockDataUtil.loadControlData().isLocked());

        checkUnlock(0);
    }

    /**
     * Operation: trigger reset password request
     * Check: salt & secret have changed
     * */
    public void test04() {
        MockPplUtil.formatLog(TAG, "test04");
        ControlData oldData = MockDataUtil.loadControlData();
        String salt = oldData.salt.toString();
        String secret = oldData.secret.toString().toString();

        startPplService(mTestContext, buildOperationIntent(MessageManager.Type.RESET_PW_REQUEST, 0));

        ControlData newData = MockDataUtil.loadControlData();
        assertFalse(salt.equals(newData.salt.toString()));
        assertFalse(secret.equals(newData.secret.toString()));
    }

    /**
     * Operation: request wipe & manually invoke wipe directly
     * Check: wipe flag & broadcast
     * */
    public void test05() {
        MockPplUtil.formatLog(TAG, "test05");
        checkWipeRequest(0);

        // Operation: Use java reflection to get mPPLManager, then invoke wipe() directly
        getPplManager().wipe();

        // Check: broadcast to turn off USB & format SD card
        String action = mTestContext.getBroadcast().getAction();
        assertTrue(PplService.Intents.INTENT_NOTIFY_MOUNT_SERVICE_WIPE.equals(action));

        // Operation: receive response from mount service
        receiveIntent(mTestContext, new Intent(PplService.Intents.INTENT_MOUNT_SERVICE_WIPE_RESPONSE));

        // Check: broadcast to do factory reset
        action = mTestContext.getBroadcast().getAction();
        assertTrue(PplService.Intents.INTENT_NOTIFY_WIPE.equals(action));

        // Check: fake wipe flag file exits
        File indicatorFile = new File(mTestContext.getFilesDir(), "WIPE_RESULT_INDICATOR");
        assertTrue(indicatorFile.exists());
        indicatorFile.delete();
    }

    /**
     * Operation: reboot after wipe
     * Check: wipe flag & broadcast
     * */
    public void test06() {
        MockPplUtil.formatLog(TAG, "test06");

        // Enable wipe flag and add pending message
        ControlData oldData = MockDataUtil.loadControlData();
        oldData.setWipeFlag(true);
        oldData.PendingMessageList = new LinkedList<PendingMessage>();
        oldData.PendingMessageList.add(new PendingMessage(PendingMessage.getNextId(),
                MessageManager.Type.WIPE_COMPLETED, MockPplUtil.SERVICE_NUMBER_1st, 0, null));
        MockDataUtil.writeControlData(oldData);

        // Reboot
        startPplService(mTestContext, new Intent(Intent.ACTION_BOOT_COMPLETED));

        // Check: wipe flag cleared
        ControlData controlData = MockDataUtil.loadControlData();
        assertFalse(controlData.hasWipeFlag());
    }

    /**
     * build the intent of operation
     * */
    private Intent buildOperationIntent(byte type, int simId) {
        Intent intent = new Intent(PplService.Intents.INTENT_REMOTE_INSTRUCTION_RECEIVED);
        intent.putExtra(Intents.INSTRUCTION_KEY_TYPE, type);
        intent.putExtra(Intents.INSTRUCTION_KEY_FROM, MockPplUtil.SERVICE_NUMBER_1st);
        intent.putExtra(Intents.INSTRUCTION_KEY_TO, MockPplUtil.SERVICE_NUMBER_1st);
        intent.putExtra(Intents.INSTRUCTION_KEY_SIM_ID, simId);
        return intent;
    }

    /**
     * build the intent of sim state change
     * */
    private Intent buildSimStateIntent(String state, int id) {
        Intent intent = new Intent(PplService.ACTION_SIM_STATE_CHANGED);
        intent.putExtra(KEY_STATE, state);
        intent.putExtra(KEY_SIM_ID, id);
        return intent;
    }

    /**
     * Operation: trigger device unlock request
     * Check: intent "INTENT_NOTIFY_UNLOCK" has been sent, status of feature
     *        unlock message has been sent
     * */
    private void checkUnlock(int simId) {
        startPplService(mTestContext, buildOperationIntent(MessageManager.Type.UNLOCK_REQUEST, simId));

        String action = mTestContext.getBroadcast().getAction();
        assertTrue(PplService.Intents.INTENT_NOTIFY_UNLOCK.equals(action));

        assertFalse(MockDataUtil.loadControlData().isLocked());
        assertTrue(MockPplUtil.compareSendMessage(MessageManager.Type.UNLOCK_RESPONSE, simId));
    }

    /**
     * Operation: trigger wipe request
     * Check: status of feature, wipe start message has been sent
     * */
    private void checkWipeRequest(int simId) {
        startPplService(mTestContext, buildOperationIntent(MessageManager.Type.WIPE_REQUEST, 0));

        ControlData controlData = MockDataUtil.loadControlData();
        assertTrue(controlData.hasWipeFlag());

        assertTrue(MockPplUtil.compareSendMessage(MessageManager.Type.WIPE_STARTED, simId));
    }

    /**
     * Use java reflection to get member mPPLManager
     * */
    public PplManager getPplManager() {
        PplManager pplManager = null;
        try {
            Field managerField = mService.getClass().getDeclaredField("mPPLManager");
            managerField.setAccessible(true);
            pplManager = (PplManager) managerField.get(mService);

        } catch (NoSuchFieldException e) {
            Log.e(TAG, "* NoSuchFieldException " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            Log.e(TAG, "* IllegalAccessException " + e.getMessage());
            e.printStackTrace();
        }
        return pplManager;
    }

    private void receiveIntent(Context context, Intent intent) {
        MockPplUtil.testServiceReceiver(context, mService, "PplService", intent);
    }

    private void startPplService(Context context, Intent intent) {
        intent.setClass(context, PplService.class);
        startService(intent);
        mService = getService();
    }

    class TestContext extends MockContext {
        private List<BroadcastReceiver> mReceiverList = new ArrayList<BroadcastReceiver>();
        private Intent mBroadcaseIntent = null;
        private boolean mSticky = false;

        @Override
        public ApplicationInfo getApplicationInfo() {
            return new ApplicationInfo();
        }

        @Override
        public File getFilesDir() {
            File filesDir = new File("/data/data/" + getPackageName());
            Log.i(TAG, "Use mock dir " + filesDir.toString());
            return filesDir;
        }

        @Override
        public String getOpPackageName() {
            return "com.mediatek.ppl.test";
        }

        @Override
        public String getPackageName() {
            return "com.mediatek.ppl.test";
        }

        @Override
        public Resources getResources() {
            return mTestResource;
        }

        @Override
        public ContentResolver getContentResolver() {
            return mTestResolver;
        }

        @Override
        public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
            String path = "/data/data/" + getPackageName() + "/" + name;
            return new FileOutputStream(new File(path));
        }

        @Override
        public Object getSystemService(String name) {
            if (name.equalsIgnoreCase(Context.CONNECTIVITY_SERVICE)) {
                return null;
            } else if (name.equalsIgnoreCase(Context.POWER_SERVICE)) {
                return new PowerManager(mContext, null, new Handler());
            }
            return null;
        }

        @Override
        public ComponentName startService(Intent intent) {
            Log.i(TAG, "StartService with intent " + intent);
            return new ComponentName("com.mediatek.ppl", "PplService");
        }

        @Override
        public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
            mReceiverList.add(receiver);
            return new Intent("com.mediatek.ppl.test.registerReceiver");
        }

        @Override
        public void unregisterReceiver(BroadcastReceiver receiver) {
            mReceiverList.remove(receiver);
        }

        @Override
        public void sendStickyBroadcast(Intent intent) {
            Log.i(TAG, "SendStickyBroadcast " + intent);
            mBroadcaseIntent = intent;
            mSticky = true;
        }

        @Override
        public void removeStickyBroadcast(Intent intent) {
            Log.i(TAG, "RemoveStickyBroadcast " + intent);
            mBroadcaseIntent = null;
            mSticky = false;
        }

        @Override
        public void sendBroadcast(Intent intent) {
            Log.i(TAG, "SendStickyBroadcast " + intent);
            mBroadcaseIntent = intent;
            mSticky = false;
        }

        public Intent getBroadcast() {
            return mBroadcaseIntent;
        }

        public boolean isSticky() {
            return mSticky;
        }
    }

    class TestResource extends MockResources {
        private String[] mText = {"1", "2", "3"};

        @Override
        public String[] getStringArray(int id) throws NotFoundException {
            if (id == R.array.ppl_sms_template_list) {
                return MockPplUtil.smsString;
            } else if (id == R.array.ppl_sms_pattern_list) {
                return MockPplUtil.smsString;
            }
            return mText;
        }
    }

    class TestResolver extends MockContentResolver {
    }
}
