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

import android.app.Application;
import android.app.Instrumentation;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.test.AndroidTestCase;

import java.util.Random;

public abstract class CombinedTestCase<T extends Application, U extends Service> extends
        AndroidTestCase {

    Class<T> mApplicationClass;
    Class<U> mServiceClass;

    private Context mSystemContext;

    public CombinedTestCase(Class<T> applicationClass, Class<U> serviceClass) {
        mApplicationClass = applicationClass;
        mServiceClass = serviceClass;
    }

    private T mApplication;
    private boolean mAttached = false;
    private boolean mCreated = false;

    private U mService;
    private boolean mServiceAttached = false;
    private boolean mServiceCreated = false;
    private boolean mServiceStarted = false;
    private boolean mServiceBound = false;
    private Intent mServiceIntent = null;
    private int mServiceId;

    /**
     * @return Returns the actual Application under test.
     */
    public T getApplication() {
        return mApplication;
    }

    /**
     * @return An instance of the service under test. This instance is created automatically when a
     *         test calls {@link #startService} or {@link #bindService}.
     */
    public U getService() {
        return mService;
    }

    /**
     * This will do the work to instantiate the Application under test. After this, your test code
     * must also start and stop the Application.
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // get the actual context, before the individual tests have a chance to muck with it
        mSystemContext = getContext();
    }

    /**
     * Load and attach the application under test.
     */
    private void setupApplication() {
        mApplication = null;
        try {
            mApplication = (T) Instrumentation.newApplication(mApplicationClass, getContext());
        } catch (Exception e) {
            assertNotNull(mApplication);
        }
        mAttached = true;
    }

    /**
     * Creates the service under test and attaches all injected dependencies (Context, Application)
     * to it. This is called automatically by {@link #startService} or by {@link #bindService}. If
     * you need to call {@link AndroidTestCase#setContext(Context) setContext()} or
     * {@link #setApplication setApplication()}, do so before calling this method.
     */
    protected void setupService() {
        mService = null;
        try {
            mService = mServiceClass.newInstance();
        } catch (Exception e) {
            assertNotNull(mService);
        }
        if (getApplication() == null) {
            setupApplication();
        }
        assertNotNull(mApplication);
        mService.attach(getContext(), null, // ActivityThread not actually used in Service
                mServiceClass.getName(), null, // token not needed when not talking with the
                                               // activity manager
                getApplication(), null // mocked services don't talk with the activity manager
        );

        assertNotNull(mService);

        mServiceId = new Random().nextInt();
        mServiceAttached = true;
    }

    /**
     * Start the Application under test, in the same way as if it was started by the system. If you
     * use this method to start the Application, it will automatically be stopped by
     * {@link #tearDown}. If you wish to inject a specialized Context for your test, by calling
     * {@link AndroidTestCase#setContext(Context) setContext()}, you must do so before calling this
     * method.
     */
    final protected void createApplication() {
        assertFalse(mCreated);

        if (!mAttached) {
            setupApplication();
        }
        assertNotNull(mApplication);

        mApplication.onCreate();
        mCreated = true;
    }

    /**
     * This will make the necessary calls to terminate the Application under test (it will call
     * onTerminate(). Ordinarily this will be called automatically (by {@link #tearDown}, but you
     * can call it directly from your test in order to check for proper shutdown behaviors.
     */
    final protected void terminateApplication() {
        if (mCreated) {
            mApplication.onTerminate();
        }
    }

    /**
     * Shuts down the Application under test. Also makes sure all resources are cleaned up and
     * garbage collected before moving on to the next test. Subclasses that override this method
     * should make sure they call super.tearDown() at the end of the overriding method.
     * 
     * @throws Exception
     */
    @Override
    protected void tearDown() throws Exception {
        terminateApplication();
        mApplication = null;

        // Scrub out members - protects against memory leaks in the case where someone
        // creates a non-static inner class (thus referencing the test case) and gives it to
        // someone else to hold onto
        scrubClass(CombinedTestCase.class);

        super.tearDown();
    }

    /**
     * Return a actual (not mocked or instrumented) system Context that can be used when generating
     * Mock or other Context objects for your Application under test.
     * 
     * @return Returns a reference to a normal Context.
     */
    public Context getSystemContext() {
        return mSystemContext;
    }

    /**
     * This test simply confirms that the Application class can be instantiated properly.
     * 
     * @throws Exception
     */
    final public void testApplicationTestCaseSetUpProperly() throws Exception {
        setupApplication();
        assertNotNull("Application class could not be instantiated successfully", mApplication);
    }
}