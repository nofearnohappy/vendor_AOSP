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

package android.mount.cts;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.storage.IMountService;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.os.SystemProperties;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import android.net.Uri;


import com.android.internal.R;

import com.mediatek.storage.StorageManagerEx;

import java.io.File;

public class MountServiceTest extends InstrumentationTestCase {

/*
    /// M: javaopt_removal @{
    private static final String PROP_SHARED_SDCARD = "ro.mtk_shared_sdcard";
    private static final String PROP_2SDCARD_SWAP = "ro.mtk_2sdcard_swap";
    /// @}

    private static final String TAG = "MountServiceTest";
    private static final String PATH_SD1 = (SystemProperties.get(PROP_SHARED_SDCARD).equals("1") && !SystemProperties.get(PROP_2SDCARD_SWAP).equals("1")) ? "/storage/emulated/0" : "/storage/sdcard0";
    private static final String PATH_SD2 = "/storage/sdcard1";
    private static final String INTENT_SD_SWAP = "com.mediatek.SD_SWAP";
    private IMountService mMountService = null;
    private Context mContext = null;
    private StorageManager mStorageManager = null;

    private boolean mSD1Mounted = false;
    private boolean mSD2Mounted = false;
    private boolean mSD1Checking = false;
    private boolean mSD2Checking = false;
    private boolean mSD1Unmounted = false;
    private boolean mSD2Unmounted = false;
    private boolean mSD1Eject = false;
    private boolean mSD2Eject = false;
    private boolean mSD1Shared = false;
    private boolean mSD2Shared = false;
    private boolean mSD1Unshared = false;
    private boolean mSD2Unshared = false;
    private boolean mSDSwap = false;

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                Log.d(TAG, "mBroadcastReceiver ACTION_MEDIA_MOUNTED");
            } else if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                Log.d(TAG, "mBroadcastReceiver ACTION_MEDIA_UNMOUNTED");
            } else if (action.equals(Intent.ACTION_MEDIA_SHARED)) {
                Log.d(TAG, "mBroadcastReceiver ACTION_MEDIA_SHARED");
            } else if (action.equals(Intent.ACTION_MEDIA_UNSHARED)) {
                Log.d(TAG, "mBroadcastReceiver ACTION_MEDIA_UNSHARED");
            } else if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                Log.d(TAG, "mBroadcastReceiver ACTION_MEDIA_EJECT");
            }
        }
    };

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (mContext == null) {
            mContext = getInstrumentation().getTargetContext();
        }

        if (mStorageManager == null) {
            mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        }

        IBinder service = ServiceManager.getService("mount");
        if (service != null && mMountService == null) {
            mMountService = IMountService.Stub.asInterface(service);
            Log.d(TAG, "mountService is " + mMountService);
            if (mMountService == null) {
                Log.e(TAG, "mountService is null!");
                fail("mountService is null!");
            }
        } else {
            Log.e(TAG, "service is null!");
            fail("service is null!");
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_SHARED);
        filter.addAction(Intent.ACTION_MEDIA_UNSHARED);
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        mContext.registerReceiver(mBroadcastReceiver, filter, null, null);
    }

    @Override
    protected void tearDown() throws Exception {
        mContext.unregisterReceiver(mBroadcastReceiver);
        super.tearDown();
    }

    public void testInitialState() {
        StorageVolume[] volumes = mStorageManager.getVolumeList();
        if (volumes == null) {
            assertTrue(false);
            return;
        }
        int len = volumes.length;
        for (int i = 0; i < len; i++) {
            StorageVolume volume = volumes[i];
            if (i == 0) {
                assertTrue(volume.getPath().equals(PATH_SD1));
                if (!SystemProperties.get(PROP_2SDCARD_SWAP).equals("1")  && !SystemProperties.get(PROP_SHARED_SDCARD).equals("1")) {
                    assertTrue(volume.getDescriptionId() == R.string.storage_phone);
                    assertTrue(!volume.isRemovable());
                    assertTrue(!volume.isEmulated());
                } else if (!SystemProperties.get(PROP_2SDCARD_SWAP).equals("1")  && SystemProperties.get(PROP_SHARED_SDCARD).equals("1")) {
                    Log.d(TAG, "volume.getDescriptionId()=" + volume.getDescriptionId()
                            + ", R.string.storage_internal=" + R.string.storage_internal);
                    assertTrue(volume.getDescriptionId() == R.string.storage_phone);
                    assertTrue(!volume.isRemovable());
                    assertTrue(volume.isEmulated());
                } else if (SystemProperties.get(PROP_2SDCARD_SWAP).equals("1")  && !SystemProperties.get(PROP_SHARED_SDCARD).equals("1")) {
                    assertTrue(volume.getDescriptionId() == R.string.storage_sd_card);
                    assertTrue(volume.isRemovable());
                    assertTrue(!volume.isEmulated());
                } else if (SystemProperties.get(PROP_2SDCARD_SWAP).equals("1")  && SystemProperties.get(PROP_SHARED_SDCARD).equals("1")) {
                    assertTrue(volume.getDescriptionId() == R.string.storage_sd_card);
                    assertTrue(volume.isRemovable());
                    assertTrue(!volume.isEmulated());
                }
            } else if (i == 1) {
                assertTrue(volume.getPath().equals(PATH_SD2));
                if (!SystemProperties.get(PROP_2SDCARD_SWAP).equals("1")  && !SystemProperties.get(PROP_SHARED_SDCARD).equals("1")) {
                    assertTrue(volume.getDescriptionId() == R.string.storage_sd_card);
                    assertTrue(volume.isRemovable());
                    assertTrue(!volume.isEmulated());
                } else if (!SystemProperties.get(PROP_2SDCARD_SWAP).equals("1")  && SystemProperties.get(PROP_SHARED_SDCARD).equals("1")) {
                    assertTrue(volume.getDescriptionId() == R.string.storage_sd_card);
                    assertTrue(volume.isRemovable());
                    assertTrue(!volume.isEmulated());
                } else if (SystemProperties.get(PROP_2SDCARD_SWAP).equals("1")  && !SystemProperties.get(PROP_SHARED_SDCARD).equals("1")) {
                    assertTrue(volume.getDescriptionId() == R.string.storage_phone);
                    assertTrue(!volume.isRemovable());
                    assertTrue(!volume.isEmulated());
                } else if (SystemProperties.get(PROP_2SDCARD_SWAP).equals("1")  && SystemProperties.get(PROP_SHARED_SDCARD).equals("1")) {
                    assertTrue(volume.getDescriptionId() == R.string.storage_internal);
                    assertTrue(!volume.isRemovable());
                    assertTrue(volume.isEmulated());
                }
            } else {
                break;
            }
        }
    }

    public void testDefaultPath() {
        waitForAllStorageMounted();
        StorageManagerEx.setDefaultPath(PATH_SD1);
        SystemClock.sleep(1000);
        String defaultPath = StorageManagerEx.getDefaultPath();
        assertTrue(PATH_SD1.equals(defaultPath));

        if (!SystemProperties.get(PROP_2SDCARD_SWAP).equals("1")  && !SystemProperties.get(PROP_SHARED_SDCARD).equals("1")) {
            //unmount SD1
            try {
                mMountService.unmountVolume(PATH_SD1, true, false);
            } catch (RemoteException e) {
            }
            waitForStorageState(PATH_SD1, Environment.MEDIA_UNMOUNTED);
            defaultPath = StorageManagerEx.getDefaultPath();
            assertTrue(PATH_SD2.equals(defaultPath));

            //mount SD1
            try {
                mMountService.mountVolume(PATH_SD1);
            } catch (RemoteException e) {
            }
            waitForStorageState(PATH_SD1, Environment.MEDIA_MOUNTED);
            defaultPath = StorageManagerEx.getDefaultPath();
            assertTrue(PATH_SD2.equals(defaultPath));

            //turn on/off UMS
            try {
                mMountService.setUsbMassStorageEnabled(true);
            } catch (RemoteException e) {
            }
            waitForStorageState(PATH_SD1, Environment.MEDIA_SHARED);
            waitForStorageState(PATH_SD2, Environment.MEDIA_SHARED);
            defaultPath = StorageManagerEx.getDefaultPath();
            assertTrue(PATH_SD2.equals(defaultPath));

            try {
                mMountService.setUsbMassStorageEnabled(false);
            } catch (RemoteException e) {
            }
            waitForStorageState(PATH_SD1, Environment.MEDIA_MOUNTED);
            waitForStorageState(PATH_SD2, Environment.MEDIA_MOUNTED);
            defaultPath = StorageManagerEx.getDefaultPath();
            assertTrue(PATH_SD2.equals(defaultPath));
        } else if (!SystemProperties.get(PROP_2SDCARD_SWAP).equals("1")  && SystemProperties.get(PROP_SHARED_SDCARD).equals("1")) {
            StorageManagerEx.setDefaultPath(PATH_SD2);
            //unmount SD2
            try {
                mMountService.unmountVolume(PATH_SD2, true, false);
            } catch (RemoteException e) {
            }
            waitForStorageState(PATH_SD2, Environment.MEDIA_UNMOUNTED);
            defaultPath = StorageManagerEx.getDefaultPath();
            assertTrue(PATH_SD1.equals(defaultPath));

            //mount SD2
            try {
                mMountService.mountVolume(PATH_SD2);
            } catch (RemoteException e) {
            }
            waitForStorageState(PATH_SD2, Environment.MEDIA_MOUNTED);
            defaultPath = StorageManagerEx.getDefaultPath();
            assertTrue(PATH_SD1.equals(defaultPath));

            StorageManagerEx.setDefaultPath(PATH_SD2);
            //turn on/off UMS
            try {
                mMountService.setUsbMassStorageEnabled(true);
            } catch (RemoteException e) {
            }
            waitForStorageState(PATH_SD1, Environment.MEDIA_SHARED);
            waitForStorageState(PATH_SD2, Environment.MEDIA_SHARED);
            defaultPath = StorageManagerEx.getDefaultPath();
            assertTrue(PATH_SD2.equals(defaultPath));
            try {
                mMountService.setUsbMassStorageEnabled(false);
            } catch (RemoteException e) {
            }
            waitForStorageState(PATH_SD1, Environment.MEDIA_MOUNTED);
            waitForStorageState(PATH_SD2, Environment.MEDIA_MOUNTED);
            defaultPath = StorageManagerEx.getDefaultPath();
            assertTrue(PATH_SD2.equals(defaultPath));
        } else if (SystemProperties.get(PROP_2SDCARD_SWAP).equals("1")  && !SystemProperties.get(PROP_SHARED_SDCARD).equals("1")) {
            StorageManagerEx.setDefaultPath(PATH_SD2);
            //unmount SD1
            try {
                mMountService.unmountVolume(PATH_SD1, true, false);
            } catch (RemoteException e) {
            }
            SystemClock.sleep(5000);
            waitForStorageState(PATH_SD1, Environment.MEDIA_MOUNTED);
            waitForStorageState(PATH_SD2, Environment.MEDIA_UNMOUNTED);
            defaultPath = StorageManagerEx.getDefaultPath();
            assertTrue(PATH_SD1.equals(defaultPath));

            //mount SD2
            try {
                mMountService.mountVolume(PATH_SD2);
            } catch (RemoteException e) {
            }
            SystemClock.sleep(5000);
            waitForStorageState(PATH_SD1, Environment.MEDIA_MOUNTED);
            waitForStorageState(PATH_SD2, Environment.MEDIA_MOUNTED);
            defaultPath = StorageManagerEx.getDefaultPath();
            assertTrue(PATH_SD2.equals(defaultPath));

            //turn on/off UMS
            try {
                mMountService.setUsbMassStorageEnabled(true);
            } catch (RemoteException e) {
            }
            waitForStorageState(PATH_SD1, Environment.MEDIA_SHARED);
            waitForStorageState(PATH_SD2, Environment.MEDIA_SHARED);
            defaultPath = StorageManagerEx.getDefaultPath();
            assertTrue(PATH_SD2.equals(defaultPath));

            try {
                mMountService.setUsbMassStorageEnabled(false);
            } catch (RemoteException e) {
            }
            waitForStorageState(PATH_SD1, Environment.MEDIA_MOUNTED);
            waitForStorageState(PATH_SD2, Environment.MEDIA_MOUNTED);
            defaultPath = StorageManagerEx.getDefaultPath();
            assertTrue(PATH_SD2.equals(defaultPath));
        } else if (SystemProperties.get(PROP_2SDCARD_SWAP).equals("1")  && SystemProperties.get(PROP_SHARED_SDCARD).equals("1")) {
            StorageManagerEx.setDefaultPath(PATH_SD2);
            //unmount SD1
            try {
                mMountService.unmountVolume(PATH_SD1, true, false);
            } catch (RemoteException e) {
            }
            SystemClock.sleep(5000);
            waitForStorageState(PATH_SD1, Environment.MEDIA_MOUNTED);
            waitForStorageState(PATH_SD2, Environment.MEDIA_UNMOUNTED);
            defaultPath = StorageManagerEx.getDefaultPath();
            assertTrue(PATH_SD1.equals(defaultPath));

            //mount SD2
            try {
                mMountService.mountVolume(PATH_SD2);
            } catch (RemoteException e) {
            }
            SystemClock.sleep(5000);
            waitForStorageState(PATH_SD1, Environment.MEDIA_MOUNTED);
            waitForStorageState(PATH_SD2, Environment.MEDIA_MOUNTED);
            defaultPath = StorageManagerEx.getDefaultPath();
            assertTrue(PATH_SD2.equals(defaultPath));

            //turn on/off UMS
            try {
                mMountService.setUsbMassStorageEnabled(true);
            } catch (RemoteException e) {
            }
            waitForStorageState(PATH_SD2, Environment.MEDIA_SHARED);
            defaultPath = StorageManagerEx.getDefaultPath();
            assertTrue(PATH_SD1.equals(defaultPath));

            try {
                mMountService.setUsbMassStorageEnabled(false);
            } catch (RemoteException e) {
            }
            waitForStorageState(PATH_SD1, Environment.MEDIA_MOUNTED);
            waitForStorageState(PATH_SD2, Environment.MEDIA_MOUNTED);
            defaultPath = StorageManagerEx.getDefaultPath();
            assertTrue(PATH_SD2.equals(defaultPath));
        }


    }

    public void testGetPrimaryVolume() {
        waitForAllStorageMounted();
        // because Environment.getPrimaryVolume() is private API
        // so we test it indirectly
        if (!SystemProperties.get(PROP_2SDCARD_SWAP).equals("1")  && !SystemProperties.get(PROP_SHARED_SDCARD).equals("1")) {
            assertTrue(!Environment.isExternalStorageRemovable());
            assertTrue(!Environment.isExternalStorageEmulated());
        } else if (!SystemProperties.get(PROP_2SDCARD_SWAP).equals("1")  && SystemProperties.get(PROP_SHARED_SDCARD).equals("1")) {
            assertTrue(!Environment.isExternalStorageRemovable());
            assertTrue(Environment.isExternalStorageEmulated());
        } else if (SystemProperties.get(PROP_2SDCARD_SWAP).equals("1")  && !SystemProperties.get(PROP_SHARED_SDCARD).equals("1")) {
            assertTrue(Environment.isExternalStorageRemovable());
            assertTrue(!Environment.isExternalStorageEmulated());
        } else if (SystemProperties.get(PROP_2SDCARD_SWAP).equals("1")  && SystemProperties.get(PROP_SHARED_SDCARD).equals("1")) {
            assertTrue(Environment.isExternalStorageRemovable());
            assertTrue(!Environment.isExternalStorageEmulated());
        }

        if (!SystemProperties.get(PROP_2SDCARD_SWAP).equals("1")  && !SystemProperties.get(PROP_SHARED_SDCARD).equals("1")) {
            //unmount SD2
            try {
                mMountService.unmountVolume(PATH_SD2, true, false);
            } catch (RemoteException e) {
            }
            waitForStorageState(PATH_SD2, Environment.MEDIA_UNMOUNTED);
            assertTrue(!Environment.isExternalStorageRemovable());
            assertTrue(!Environment.isExternalStorageEmulated());
        } else if (!SystemProperties.get(PROP_2SDCARD_SWAP).equals("1")  && SystemProperties.get(PROP_SHARED_SDCARD).equals("1")) {
            //unmount SD2
            try {
                mMountService.unmountVolume(PATH_SD2, true, false);
            } catch (RemoteException e) {
            }
            waitForStorageState(PATH_SD2, Environment.MEDIA_UNMOUNTED);
            assertTrue(!Environment.isExternalStorageRemovable());
            assertTrue(Environment.isExternalStorageEmulated());
        } else if (SystemProperties.get(PROP_2SDCARD_SWAP).equals("1")  && !SystemProperties.get(PROP_SHARED_SDCARD).equals("1")) {
            //unmount SD1
            try {
                mMountService.unmountVolume(PATH_SD1, true, false);
            } catch (RemoteException e) {
            }
            SystemClock.sleep(5000);
            waitForStorageState(PATH_SD1, Environment.MEDIA_MOUNTED);
            waitForStorageState(PATH_SD2, Environment.MEDIA_UNMOUNTED);
            assertTrue(Environment.isExternalStorageRemovable());
            assertTrue(!Environment.isExternalStorageEmulated());
        } else if (SystemProperties.get(PROP_2SDCARD_SWAP).equals("1")  && SystemProperties.get(PROP_SHARED_SDCARD).equals("1")) {
            //unmount SD1
            try {
                mMountService.unmountVolume(PATH_SD1, true, false);
            } catch (RemoteException e) {
            }
            SystemClock.sleep(5000);
            waitForStorageState(PATH_SD1, Environment.MEDIA_MOUNTED);
            waitForStorageState(PATH_SD2, Environment.MEDIA_UNMOUNTED);
            assertTrue(!Environment.isExternalStorageRemovable());
            assertTrue(Environment.isExternalStorageEmulated());
        }
    }

    public void testExternalCacheDir() {
        assertTrue(null == StorageManagerEx.getExternalCacheDir(null));

        StorageManagerEx.setDefaultPath(PATH_SD1);
        String path;
        File file = StorageManagerEx.getExternalCacheDir("com.mediatek.cts.mountservice");
        if(null != file) {
            path = file.getPath();
            assertTrue(path.contains(PATH_SD1));
        }

        StorageManagerEx.setDefaultPath(PATH_SD2);
        file = StorageManagerEx.getExternalCacheDir("com.mediatek.cts.mountservice");
        if(null != file) {
            path = file.getPath();
            assertTrue(path.contains(PATH_SD2));
        }
    }

    public void testInternalExternalPath() {
        waitForAllStorageMounted();
        // we will check different flow in four cases
        if (!SystemProperties.get(PROP_2SDCARD_SWAP).equals("1")  && !SystemProperties.get(PROP_SHARED_SDCARD).equals("1")) {
            assertTrue(PATH_SD1.equals(StorageManagerEx.getInternalStoragePath()));
            assertTrue(PATH_SD2.equals(StorageManagerEx.getExternalStoragePath()));

            //unmount SD1
            try {
                mMountService.unmountVolume(PATH_SD1, true, false);
            } catch (RemoteException e) {
            }
            waitForStorageState(PATH_SD1, Environment.MEDIA_UNMOUNTED);
            //unmount SD2
            try {
                mMountService.unmountVolume(PATH_SD2, true, false);
            } catch (RemoteException e) {
            }
            waitForStorageState(PATH_SD2, Environment.MEDIA_UNMOUNTED);
            assertTrue(PATH_SD1.equals(StorageManagerEx.getInternalStoragePath()));
            assertTrue(PATH_SD2.equals(StorageManagerEx.getExternalStoragePath()));

            //mount SD1
            try {
                mMountService.mountVolume(PATH_SD1);
            } catch (RemoteException e) {
            }
            waitForStorageState(PATH_SD1, Environment.MEDIA_MOUNTED);
            //mount SD2
            try {
                mMountService.mountVolume(PATH_SD2);
            } catch (RemoteException e) {
            }
            waitForStorageState(PATH_SD2, Environment.MEDIA_MOUNTED);
            assertTrue(PATH_SD1.equals(StorageManagerEx.getInternalStoragePath()));
            assertTrue(PATH_SD2.equals(StorageManagerEx.getExternalStoragePath()));

            //turn on/off UMS
            try {
                mMountService.setUsbMassStorageEnabled(true);
            } catch (RemoteException e) {
            }
            waitForStorageState(PATH_SD1, Environment.MEDIA_SHARED);
            waitForStorageState(PATH_SD2, Environment.MEDIA_SHARED);
            assertTrue(PATH_SD1.equals(StorageManagerEx.getInternalStoragePath()));
            assertTrue(PATH_SD2.equals(StorageManagerEx.getExternalStoragePath()));
            try {
                mMountService.setUsbMassStorageEnabled(false);
            } catch (RemoteException e) {
            }
            waitForStorageState(PATH_SD1, Environment.MEDIA_MOUNTED);
            waitForStorageState(PATH_SD2, Environment.MEDIA_MOUNTED);
            assertTrue(PATH_SD1.equals(StorageManagerEx.getInternalStoragePath()));
            assertTrue(PATH_SD2.equals(StorageManagerEx.getExternalStoragePath()));
        } else if (!SystemProperties.get(PROP_2SDCARD_SWAP).equals("1")  && SystemProperties.get(PROP_SHARED_SDCARD).equals("1")) {
            Log.d(TAG, "PATH_SD1=" + PATH_SD1
                    + ", StorageManagerEx.getInternalStoragePath()=" + StorageManagerEx.getInternalStoragePath());

            String PROP_SD_INTERNAL_PATH = "vold.path.internal_sd";
            String STORAGE_PATH_SD1 = "/storage/sdcard0";
            String path_sd1_changed_name="";
            try {
                path_sd1_changed_name = SystemProperties.get(PROP_SD_INTERNAL_PATH);
                Log.i(TAG, "getInternalStoragePath from Property path=" + path_sd1_changed_name);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "IllegalArgumentException when getInternalStoragePath:" + e);
            }
            // only share sd and no swap, internal path is "/storage/emulated/0", need change
            if (STORAGE_PATH_SD1.equals(path_sd1_changed_name)) {
                if (Process.myUid() == Process.SYSTEM_UID) {
                    path_sd1_changed_name = "/storage/emulated/"+Integer.toString(Process.SYSTEM_UID);
                } else {
                    path_sd1_changed_name = Environment.getExternalStorageDirectory().toString();
                }
            }

            assertTrue(path_sd1_changed_name.equals(StorageManagerEx.getInternalStoragePath()));
            assertTrue(PATH_SD2.equals(StorageManagerEx.getExternalStoragePath()));

            //unmount SD2
            try {
                mMountService.unmountVolume(PATH_SD2, true, false);
            } catch (RemoteException e) {
            }
            waitForStorageState(PATH_SD2, Environment.MEDIA_UNMOUNTED);
            assertTrue(path_sd1_changed_name.equals(StorageManagerEx.getInternalStoragePath()));
            assertTrue(PATH_SD2.equals(StorageManagerEx.getExternalStoragePath()));

            //mount SD2
            try {
                mMountService.mountVolume(PATH_SD2);
            } catch (RemoteException e) {
            }
            waitForStorageState(PATH_SD2, Environment.MEDIA_MOUNTED);
            assertTrue(path_sd1_changed_name.equals(StorageManagerEx.getInternalStoragePath()));
            assertTrue(PATH_SD2.equals(StorageManagerEx.getExternalStoragePath()));

            //turn on/off UMS
            try {
                mMountService.setUsbMassStorageEnabled(true);
            } catch (RemoteException e) {
            }
            waitForStorageState(PATH_SD2, Environment.MEDIA_SHARED);
            assertTrue(path_sd1_changed_name.equals(StorageManagerEx.getInternalStoragePath()));
            assertTrue(PATH_SD2.equals(StorageManagerEx.getExternalStoragePath()));
            try {
                mMountService.setUsbMassStorageEnabled(false);
            } catch (RemoteException e) {
            }
            waitForStorageState(PATH_SD2, Environment.MEDIA_MOUNTED);
            assertTrue(path_sd1_changed_name.equals(StorageManagerEx.getInternalStoragePath()));
            assertTrue(PATH_SD2.equals(StorageManagerEx.getExternalStoragePath()));
        } else if (SystemProperties.get(PROP_2SDCARD_SWAP).equals("1")  && !SystemProperties.get(PROP_SHARED_SDCARD).equals("1")) {
            assertTrue(PATH_SD2.equals(StorageManagerEx.getInternalStoragePath()));
            assertTrue(PATH_SD1.equals(StorageManagerEx.getExternalStoragePath()));

            //unmount SD1
            try {
                mMountService.unmountVolume(PATH_SD1, true, false);
            } catch (RemoteException e) {
            }
            SystemClock.sleep(5000);
            waitForStorageState(PATH_SD1, Environment.MEDIA_MOUNTED);
            waitForStorageState(PATH_SD2, Environment.MEDIA_UNMOUNTED);
            assertTrue(PATH_SD1.equals(StorageManagerEx.getInternalStoragePath()));
            assertTrue(PATH_SD2.equals(StorageManagerEx.getExternalStoragePath()));

            //mount SD2
            try {
                mMountService.mountVolume(PATH_SD2);
            } catch (RemoteException e) {
            }
            SystemClock.sleep(5000);
            waitForStorageState(PATH_SD1, Environment.MEDIA_MOUNTED);
            waitForStorageState(PATH_SD2, Environment.MEDIA_MOUNTED);
            assertTrue(PATH_SD2.equals(StorageManagerEx.getInternalStoragePath()));
            assertTrue(PATH_SD1.equals(StorageManagerEx.getExternalStoragePath()));

            //turn on/off UMS
            try {
                mMountService.setUsbMassStorageEnabled(true);
            } catch (RemoteException e) {
            }
            waitForStorageState(PATH_SD1, Environment.MEDIA_SHARED);
            waitForStorageState(PATH_SD2, Environment.MEDIA_SHARED);
            assertTrue(PATH_SD2.equals(StorageManagerEx.getInternalStoragePath()));
            assertTrue(PATH_SD1.equals(StorageManagerEx.getExternalStoragePath()));
            try {
                mMountService.setUsbMassStorageEnabled(false);
            } catch (RemoteException e) {
            }
            waitForStorageState(PATH_SD1, Environment.MEDIA_MOUNTED);
            waitForStorageState(PATH_SD2, Environment.MEDIA_MOUNTED);
            assertTrue(PATH_SD2.equals(StorageManagerEx.getInternalStoragePath()));
            assertTrue(PATH_SD1.equals(StorageManagerEx.getExternalStoragePath()));
        } else if (SystemProperties.get(PROP_2SDCARD_SWAP).equals("1")  && SystemProperties.get(PROP_SHARED_SDCARD).equals("1")) {
            assertTrue(PATH_SD2.equals(StorageManagerEx.getInternalStoragePath()));
            assertTrue(PATH_SD1.equals(StorageManagerEx.getExternalStoragePath()));

            //unmount SD1
            try {
                mMountService.unmountVolume(PATH_SD1, true, false);
            } catch (RemoteException e) {
            }
            SystemClock.sleep(5000);
            waitForStorageState(PATH_SD1, Environment.MEDIA_MOUNTED);
            waitForStorageState(PATH_SD2, Environment.MEDIA_UNMOUNTED);
            assertTrue(PATH_SD1.equals(StorageManagerEx.getInternalStoragePath()));
            assertTrue(PATH_SD2.equals(StorageManagerEx.getExternalStoragePath()));

            //mount SD2
            try {
                mMountService.mountVolume(PATH_SD2);
            } catch (RemoteException e) {
            }
            SystemClock.sleep(5000);
            waitForStorageState(PATH_SD1, Environment.MEDIA_MOUNTED);
            waitForStorageState(PATH_SD2, Environment.MEDIA_MOUNTED);
            assertTrue(PATH_SD2.equals(StorageManagerEx.getInternalStoragePath()));
            assertTrue(PATH_SD1.equals(StorageManagerEx.getExternalStoragePath()));

            //turn on/off UMS
            try {
                mMountService.setUsbMassStorageEnabled(true);
            } catch (RemoteException e) {
            }
            SystemClock.sleep(5000);
            waitForStorageState(PATH_SD1, Environment.MEDIA_MOUNTED);
            waitForStorageState(PATH_SD2, Environment.MEDIA_SHARED);
            assertTrue(PATH_SD1.equals(StorageManagerEx.getInternalStoragePath()));
            assertTrue(PATH_SD2.equals(StorageManagerEx.getExternalStoragePath()));
            try {
                mMountService.setUsbMassStorageEnabled(false);
            } catch (RemoteException e) {
            }
            SystemClock.sleep(5000);
            waitForStorageState(PATH_SD1, Environment.MEDIA_MOUNTED);
            waitForStorageState(PATH_SD2, Environment.MEDIA_MOUNTED);
            assertTrue(PATH_SD2.equals(StorageManagerEx.getInternalStoragePath()));
            assertTrue(PATH_SD1.equals(StorageManagerEx.getExternalStoragePath()));
        }
    }

    public void testMountUnmount() {
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Uri uri = intent.getData();
                String path = null;

                boolean validIntent = false;
                if (action != null && uri != null) {
                    path = uri.getPath();
                    if (path != null) {
                        validIntent = true;
                    }
                }
                assertTrue(validIntent);
                if (!validIntent) {
                    return;
                }
                Log.d(TAG, "testMountUnmount, receive path=" + path);
                if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                    Log.d(TAG, "testMountUnmount receive ACTION_MEDIA_MOUNTED");
                    if (path.equals(PATH_SD1)) {
                        mSD1Mounted = true;
                    } else if (path.equals(PATH_SD2)) {
                        mSD2Mounted = true;
                    }
                } else if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                    Log.d(TAG, "testMountUnmount receive ACTION_MEDIA_UNMOUNTED");
                    if (path.equals(PATH_SD1)) {
                        mSD1Unmounted = true;
                    } else if (path.equals(PATH_SD2)) {
                        mSD2Unmounted = true;
                    }
                } else if (action.equals(Intent.ACTION_MEDIA_CHECKING)) {
                    Log.d(TAG, "testMountUnmount receive ACTION_MEDIA_CHECKING");
                    if (path.equals(PATH_SD1)) {
                        mSD1Checking = true;
                    } else if (path.equals(PATH_SD2)) {
                        mSD2Checking = true;
                    }
                } else if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                    Log.d(TAG, "testMountUnmount receive ACTION_MEDIA_EJECT");
                    if (path.equals(PATH_SD1)) {
                        mSD1Eject = true;
                    } else if (path.equals(PATH_SD2)) {
                        mSD2Eject = true;
                    }
                } else if (action.equals(INTENT_SD_SWAP)) {
                    Log.d(TAG, "testMountUnmount receive INTENT_SD_SWAP");
                    mSDSwap = true;
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addDataScheme("file");
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_CHECKING);
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        filter.addAction(INTENT_SD_SWAP);
        mContext.registerReceiver(broadcastReceiver, filter, null, null);

        waitForAllStorageMounted();
        if (!SystemProperties.get(PROP_2SDCARD_SWAP).equals("1")  && !SystemProperties.get(PROP_SHARED_SDCARD).equals("1")) {
            resetIntentFlag();
            //unmount SD1
            try {
                mMountService.unmountVolume(PATH_SD1, true, false);
            } catch (RemoteException e) {
            }
            assertTrue(waitForStorageState(PATH_SD1, Environment.MEDIA_UNMOUNTED));
            SystemClock.sleep(2000);
            assertTrue(!mSD1Mounted);
            assertTrue(!mSD2Mounted);
            assertTrue(!mSD1Checking);
            assertTrue(!mSD2Checking);
            assertTrue(mSD1Unmounted);
            assertTrue(!mSD2Unmounted);
            assertTrue(mSD1Eject);
            assertTrue(!mSD2Eject);
            assertTrue(!mSDSwap);

            resetIntentFlag();
            //unmount SD2
            try {
                mMountService.unmountVolume(PATH_SD2, true, false);
            } catch (RemoteException e) {
            }
            assertTrue(waitForStorageState(PATH_SD2, Environment.MEDIA_UNMOUNTED));
            SystemClock.sleep(2000);
            assertTrue(!mSD1Mounted);
            assertTrue(!mSD2Mounted);
            assertTrue(!mSD1Checking);
            assertTrue(!mSD2Checking);
            assertTrue(!mSD1Unmounted);
            assertTrue(mSD2Unmounted);
            assertTrue(!mSD1Eject);
            assertTrue(mSD2Eject);
            assertTrue(!mSDSwap);

            resetIntentFlag();
            //mount SD1
            try {
                mMountService.mountVolume(PATH_SD1);
            } catch (RemoteException e) {
            }
            assertTrue(waitForStorageState(PATH_SD1, Environment.MEDIA_MOUNTED));
            SystemClock.sleep(2000);
            assertTrue(mSD1Mounted);
            assertTrue(!mSD2Mounted);
            assertTrue(mSD1Checking);
            assertTrue(!mSD2Checking);
            assertTrue(!mSD1Unmounted);
            assertTrue(!mSD2Unmounted);
            assertTrue(!mSD1Eject);
            assertTrue(!mSD2Eject);
            assertTrue(!mSDSwap);

            resetIntentFlag();
            //mount SD2
            try {
                mMountService.mountVolume(PATH_SD2);
            } catch (RemoteException e) {
            }
            assertTrue(waitForStorageState(PATH_SD2, Environment.MEDIA_MOUNTED));
            SystemClock.sleep(2000);
            assertTrue(!mSD1Mounted);
            assertTrue(mSD2Mounted);
            assertTrue(!mSD1Checking);
            assertTrue(mSD2Checking);
            assertTrue(!mSD1Unmounted);
            assertTrue(!mSD2Unmounted);
            assertTrue(!mSD1Eject);
            assertTrue(!mSD2Eject);
            assertTrue(!mSDSwap);
        } else if (!SystemProperties.get(PROP_2SDCARD_SWAP).equals("1")  && SystemProperties.get(PROP_SHARED_SDCARD).equals("1")) {
            resetIntentFlag();
            //unmount SD2
            try {
                mMountService.unmountVolume(PATH_SD2, true, false);
            } catch (RemoteException e) {
            }
            assertTrue(waitForStorageState(PATH_SD2, Environment.MEDIA_UNMOUNTED));
            SystemClock.sleep(2000);
            assertTrue(!mSD1Mounted);
            assertTrue(!mSD2Mounted);
            assertTrue(!mSD1Checking);
            assertTrue(!mSD2Checking);
            assertTrue(!mSD1Unmounted);
            assertTrue(mSD2Unmounted);
            assertTrue(!mSD1Eject);
            assertTrue(mSD2Eject);
            assertTrue(!mSDSwap);

            resetIntentFlag();
            //mount SD2
            try {
                mMountService.mountVolume(PATH_SD2);
            } catch (RemoteException e) {
            }
            assertTrue(waitForStorageState(PATH_SD2, Environment.MEDIA_MOUNTED));
            SystemClock.sleep(2000);
            assertTrue(!mSD1Mounted);
            assertTrue(mSD2Mounted);
            assertTrue(!mSD1Checking);
            assertTrue(mSD2Checking);
            assertTrue(!mSD1Unmounted);
            assertTrue(!mSD2Unmounted);
            assertTrue(!mSD1Eject);
            assertTrue(!mSD2Eject);
            assertTrue(!mSDSwap);
        } else if (SystemProperties.get(PROP_2SDCARD_SWAP).equals("1")  && !SystemProperties.get(PROP_SHARED_SDCARD).equals("1")) {
            resetIntentFlag();
            //unmount SD1
            try {
                mMountService.unmountVolume(PATH_SD1, true, false);
            } catch (RemoteException e) {
            }
            SystemClock.sleep(5000);
            assertTrue(waitForStorageState(PATH_SD1, Environment.MEDIA_MOUNTED));
            assertTrue(waitForStorageState(PATH_SD2, Environment.MEDIA_UNMOUNTED));
            assertTrue(mSD1Mounted);
            assertTrue(!mSD2Mounted);
            assertTrue(mSD1Checking);
            assertTrue(!mSD2Checking);
            assertTrue(mSD1Unmounted);
            assertTrue(mSD2Unmounted);
            assertTrue(mSD1Eject);
            assertTrue(mSD2Eject);
            assertTrue(mSDSwap);

            resetIntentFlag();
            //unmount SD1
            try {
                mMountService.unmountVolume(PATH_SD1, true, false);
            } catch (RemoteException e) {
            }
            assertTrue(waitForStorageState(PATH_SD1, Environment.MEDIA_UNMOUNTED));
            SystemClock.sleep(2000);
            assertTrue(!mSD1Mounted);
            assertTrue(!mSD2Mounted);
            assertTrue(!mSD1Checking);
            assertTrue(!mSD2Checking);
            assertTrue(mSD1Unmounted);
            assertTrue(!mSD2Unmounted);
            assertTrue(mSD1Eject);
            assertTrue(!mSD2Eject);
            assertTrue(!mSDSwap);

            resetIntentFlag();
            //mount SD1
            try {
                mMountService.mountVolume(PATH_SD1);
            } catch (RemoteException e) {
            }
            assertTrue(waitForStorageState(PATH_SD1, Environment.MEDIA_MOUNTED));
            SystemClock.sleep(2000);
            assertTrue(mSD1Mounted);
            assertTrue(!mSD2Mounted);
            assertTrue(mSD1Checking);
            assertTrue(!mSD2Checking);
            assertTrue(!mSD1Unmounted);
            assertTrue(!mSD2Unmounted);
            assertTrue(!mSD1Eject);
            assertTrue(!mSD2Eject);
            assertTrue(!mSDSwap);

            resetIntentFlag();
            //mount SD2
            try {
                mMountService.mountVolume(PATH_SD2);
            } catch (RemoteException e) {
            }
            SystemClock.sleep(5000);
            assertTrue(waitForStorageState(PATH_SD1, Environment.MEDIA_MOUNTED));
            assertTrue(waitForStorageState(PATH_SD2, Environment.MEDIA_MOUNTED));
            assertTrue(mSD1Mounted);
            assertTrue(mSD2Mounted);
            assertTrue(mSD1Checking);
            assertTrue(mSD2Checking);
            assertTrue(mSD1Unmounted);
            assertTrue(!mSD2Unmounted);
            assertTrue(mSD1Eject);
            assertTrue(!mSD2Eject);
            assertTrue(mSDSwap);
        } else if (SystemProperties.get(PROP_2SDCARD_SWAP).equals("1")  && SystemProperties.get(PROP_SHARED_SDCARD).equals("1")) {
            resetIntentFlag();
            //unmount SD1
            try {
                mMountService.unmountVolume(PATH_SD1, true, false);
            } catch (RemoteException e) {
            }
            SystemClock.sleep(5000);
            assertTrue(waitForStorageState(PATH_SD1, Environment.MEDIA_MOUNTED));
            assertTrue(waitForStorageState(PATH_SD2, Environment.MEDIA_UNMOUNTED));
            assertTrue(mSD1Mounted);
            assertTrue(!mSD2Mounted);
            assertTrue(mSD1Checking);
            assertTrue(!mSD2Checking);
            assertTrue(mSD1Unmounted);
            assertTrue(mSD2Unmounted);
            assertTrue(mSD1Eject);
            assertTrue(mSD2Eject);
            assertTrue(mSDSwap);

            resetIntentFlag();
            //mount SD2
            try {
                mMountService.mountVolume(PATH_SD2);
            } catch (RemoteException e) {
            }
            SystemClock.sleep(5000);
            assertTrue(waitForStorageState(PATH_SD1, Environment.MEDIA_MOUNTED));
            assertTrue(waitForStorageState(PATH_SD2, Environment.MEDIA_MOUNTED));
            assertTrue(mSD1Mounted);
            assertTrue(mSD2Mounted);
            assertTrue(mSD1Checking);
            assertTrue(mSD2Checking);
            assertTrue(mSD1Unmounted);
            assertTrue(!mSD2Unmounted);
            assertTrue(mSD1Eject);
            assertTrue(!mSD2Eject);
            assertTrue(mSDSwap);
        }

        mContext.unregisterReceiver(broadcastReceiver);
    }

    public void testUMS() {
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Uri uri = intent.getData();
                String path = null;

                boolean validIntent = false;
                if (action != null && uri != null) {
                    path = uri.getPath();
                    if (path != null) {
                        validIntent = true;
                    }
                }
                assertTrue(validIntent);
                if (!validIntent) {
                    return;
                }

                if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                    Log.d(TAG, "testMountUnmount receive ACTION_MEDIA_MOUNTED");
                    if (path.equals(PATH_SD1)) {
                        mSD1Mounted = true;
                    } else if (path.equals(PATH_SD2)) {
                        mSD2Mounted = true;
                    }
                } else if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                    Log.d(TAG, "testMountUnmount receive ACTION_MEDIA_UNMOUNTED");
                    if (path.equals(PATH_SD1)) {
                        mSD1Unmounted = true;
                    } else if (path.equals(PATH_SD2)) {
                        mSD2Unmounted = true;
                    }
                } else if (action.equals(Intent.ACTION_MEDIA_CHECKING)) {
                    Log.d(TAG, "testMountUnmount receive ACTION_MEDIA_CHECKING");
                    if (path.equals(PATH_SD1)) {
                        mSD1Checking = true;
                    } else if (path.equals(PATH_SD2)) {
                        mSD2Checking = true;
                    }
                } else if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                    Log.d(TAG, "testMountUnmount receive ACTION_MEDIA_EJECT");
                    if (path.equals(PATH_SD1)) {
                        mSD1Eject = true;
                    } else if (path.equals(PATH_SD2)) {
                        mSD2Eject = true;
                    }
                } else if (action.equals(INTENT_SD_SWAP)) {
                    Log.d(TAG, "testMountUnmount receive INTENT_SD_SWAP");
                    mSDSwap = true;
                } else if (action.equals(Intent.ACTION_MEDIA_SHARED)) {
                    Log.d(TAG, "testMountUnmount receive ACTION_MEDIA_SHARED");
                    if (path.equals(PATH_SD1)) {
                        mSD1Shared = true;
                    } else if (path.equals(PATH_SD2)) {
                        mSD2Shared = true;
                    }
                } else if (action.equals(Intent.ACTION_MEDIA_UNSHARED)) {
                    Log.d(TAG, "testMountUnmount receive ACTION_MEDIA_UNSHARED");
                    if (path.equals(PATH_SD1)) {
                        mSD1Unshared = true;
                    } else if (path.equals(PATH_SD2)) {
                        mSD2Unshared = true;
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addDataScheme("file");
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_CHECKING);
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        filter.addAction(INTENT_SD_SWAP);
        filter.addAction(Intent.ACTION_MEDIA_SHARED);
        filter.addAction(Intent.ACTION_MEDIA_UNSHARED);
        mContext.registerReceiver(broadcastReceiver, filter, null, null);

        waitForAllStorageMounted();
        if (!SystemProperties.get(PROP_2SDCARD_SWAP).equals("1")  && !SystemProperties.get(PROP_SHARED_SDCARD).equals("1")) {
            resetIntentFlag();
            //turn on UMS
            try {
                mMountService.setUsbMassStorageEnabled(true);
            } catch (RemoteException e) {
            }
            assertTrue(waitForStorageState(PATH_SD1, Environment.MEDIA_SHARED));
            assertTrue(waitForStorageState(PATH_SD2, Environment.MEDIA_SHARED));
            SystemClock.sleep(2000);
            assertTrue(!mSD1Mounted);
            assertTrue(!mSD2Mounted);
            assertTrue(!mSD1Checking);
            assertTrue(!mSD2Checking);
            assertTrue(mSD1Unmounted);
            assertTrue(mSD2Unmounted);
            assertTrue(mSD1Eject);
            assertTrue(mSD2Eject);
            assertTrue(!mSDSwap);
            assertTrue(mSD1Shared);
            assertTrue(mSD2Shared);
            assertTrue(!mSD1Unshared);
            assertTrue(!mSD2Unshared);

            resetIntentFlag();
            //turn off UMS
            try {
                mMountService.setUsbMassStorageEnabled(false);
            } catch (RemoteException e) {
            }
            assertTrue(waitForStorageState(PATH_SD1, Environment.MEDIA_MOUNTED));
            assertTrue(waitForStorageState(PATH_SD2, Environment.MEDIA_MOUNTED));
            SystemClock.sleep(2000);
            assertTrue(mSD1Mounted);
            assertTrue(mSD2Mounted);
            assertTrue(mSD1Checking);
            assertTrue(mSD2Checking);
            assertTrue(mSD1Unmounted);
            assertTrue(mSD2Unmounted);
            assertTrue(!mSD1Eject);
            assertTrue(!mSD2Eject);
            assertTrue(!mSDSwap);
            assertTrue(!mSD1Shared);
            assertTrue(!mSD2Shared);
            assertTrue(mSD1Unshared);
            assertTrue(mSD2Unshared);
        } else if (!SystemProperties.get(PROP_2SDCARD_SWAP).equals("1")  && SystemProperties.get(PROP_SHARED_SDCARD).equals("1")) {
            resetIntentFlag();
            //turn on UMS
            try {
                mMountService.setUsbMassStorageEnabled(true);
            } catch (RemoteException e) {
            }
            assertTrue(waitForStorageState(PATH_SD1, Environment.MEDIA_MOUNTED));
            assertTrue(waitForStorageState(PATH_SD2, Environment.MEDIA_SHARED));
            SystemClock.sleep(2000);
            assertTrue(!mSD1Mounted);
            assertTrue(!mSD2Mounted);
            assertTrue(!mSD1Checking);
            assertTrue(!mSD2Checking);
            assertTrue(!mSD1Unmounted);
            assertTrue(mSD2Unmounted);
            assertTrue(!mSD1Eject);
            assertTrue(mSD2Eject);
            assertTrue(!mSDSwap);
            assertTrue(!mSD1Shared);
            assertTrue(mSD2Shared);
            assertTrue(!mSD1Unshared);
            assertTrue(!mSD2Unshared);

            resetIntentFlag();
            //turn off UMS
            try {
                mMountService.setUsbMassStorageEnabled(false);
            } catch (RemoteException e) {
            }
            assertTrue(waitForStorageState(PATH_SD1, Environment.MEDIA_MOUNTED));
            assertTrue(waitForStorageState(PATH_SD2, Environment.MEDIA_MOUNTED));
            SystemClock.sleep(2000);
            assertTrue(!mSD1Mounted);
            assertTrue(mSD2Mounted);
            assertTrue(!mSD1Checking);
            assertTrue(mSD2Checking);
            assertTrue(!mSD1Unmounted);
            assertTrue(mSD2Unmounted);
            assertTrue(!mSD1Eject);
            assertTrue(!mSD2Eject);
            assertTrue(!mSDSwap);
            assertTrue(!mSD1Shared);
            assertTrue(!mSD2Shared);
            assertTrue(!mSD1Unshared);
            assertTrue(mSD2Unshared);
        } else if (SystemProperties.get(PROP_2SDCARD_SWAP).equals("1")  && !SystemProperties.get(PROP_SHARED_SDCARD).equals("1")) {
            resetIntentFlag();
            //turn on UMS
            try {
                mMountService.setUsbMassStorageEnabled(true);
            } catch (RemoteException e) {
            }
            assertTrue(waitForStorageState(PATH_SD1, Environment.MEDIA_SHARED));
            assertTrue(waitForStorageState(PATH_SD2, Environment.MEDIA_SHARED));
            SystemClock.sleep(2000);
            assertTrue(!mSD1Mounted);
            assertTrue(!mSD2Mounted);
            assertTrue(!mSD1Checking);
            assertTrue(!mSD2Checking);
            assertTrue(mSD1Unmounted);
            assertTrue(mSD2Unmounted);
            assertTrue(mSD1Eject);
            assertTrue(mSD2Eject);
            assertTrue(!mSDSwap);
            assertTrue(mSD1Shared);
            assertTrue(mSD2Shared);
            assertTrue(!mSD1Unshared);
            assertTrue(!mSD2Unshared);

            resetIntentFlag();
            //turn off UMS
            try {
                mMountService.setUsbMassStorageEnabled(false);
            } catch (RemoteException e) {
            }
            assertTrue(waitForStorageState(PATH_SD1, Environment.MEDIA_MOUNTED));
            assertTrue(waitForStorageState(PATH_SD2, Environment.MEDIA_MOUNTED));
            SystemClock.sleep(2000);
            assertTrue(mSD1Mounted);
            assertTrue(mSD2Mounted);
            assertTrue(mSD1Checking);
            assertTrue(mSD2Checking);
            assertTrue(mSD1Unmounted);
            assertTrue(mSD2Unmounted);
            assertTrue(!mSD1Eject);
            assertTrue(!mSD2Eject);
            assertTrue(!mSDSwap);
            assertTrue(!mSD1Shared);
            assertTrue(!mSD2Shared);
            assertTrue(mSD1Unshared);
            assertTrue(mSD2Unshared);
        } else if (SystemProperties.get(PROP_2SDCARD_SWAP).equals("1")  && SystemProperties.get(PROP_SHARED_SDCARD).equals("1")) {
            resetIntentFlag();
            //turn on UMS
            try {
                mMountService.setUsbMassStorageEnabled(true);
            } catch (RemoteException e) {
            }
            SystemClock.sleep(5000);
            assertTrue(waitForStorageState(PATH_SD1, Environment.MEDIA_MOUNTED));
            assertTrue(waitForStorageState(PATH_SD2, Environment.MEDIA_SHARED));
            assertTrue(mSD1Mounted);
            assertTrue(!mSD2Mounted);
            assertTrue(mSD1Checking);
            assertTrue(!mSD2Checking);
            assertTrue(mSD1Unmounted);
            assertTrue(mSD2Unmounted);
            assertTrue(mSD1Eject);
            assertTrue(mSD2Eject);
            assertTrue(mSDSwap);
            assertTrue(!mSD1Shared);
            assertTrue(mSD2Shared);
            assertTrue(!mSD1Unshared);
            assertTrue(!mSD2Unshared);

            resetIntentFlag();
            //turn off UMS
            try {
                mMountService.setUsbMassStorageEnabled(false);
            } catch (RemoteException e) {
            }
            SystemClock.sleep(5000);
            assertTrue(waitForStorageState(PATH_SD1, Environment.MEDIA_MOUNTED));
            assertTrue(waitForStorageState(PATH_SD2, Environment.MEDIA_MOUNTED));
            assertTrue(mSD1Mounted);
            assertTrue(mSD2Mounted);
            assertTrue(mSD1Checking);
            assertTrue(mSD2Checking);
            assertTrue(mSD1Unmounted);
            assertTrue(mSD2Unmounted);
            assertTrue(mSD1Eject);
            assertTrue(!mSD2Eject);
            assertTrue(mSDSwap);
            assertTrue(!mSD1Shared);
            assertTrue(!mSD2Shared);
            assertTrue(!mSD1Unshared);
            assertTrue(mSD2Unshared);
        }

        mContext.unregisterReceiver(broadcastReceiver);
    }

    private void waitForAllStorageMounted() {
        String[] paths = mStorageManager.getVolumePaths();
        if (paths == null) {
            return;
        }
        int len = paths.length;
        String state;
        for (int i = 0; i < len; i++) {
            state = mStorageManager.getVolumeState(paths[i]);
            if (state == null) {
                continue;
            }
            if (state.equals(Environment.MEDIA_UNMOUNTED)) {
                try {
                    mMountService.mountVolume(paths[i]);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException when mount " + paths[i]);
                }
            } else if (state.equals(Environment.MEDIA_SHARED)) {
                try {
                    mMountService.setUsbMassStorageEnabled(false);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException when unshare " + paths[i]);
                }
            }
            // wait for sd mount
            state = mStorageManager.getVolumeState(paths[i]);
            int retries = 30;
            while (state != null && !state.equals(Environment.MEDIA_MOUNTED) && (retries-- >= 0)) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException iex) {
                    Log.e(TAG, "Interrupted while waiting for media", iex);
                    break;
                }
                state = mStorageManager.getVolumeState(paths[i]);
            }
        }
    }

    private boolean waitForStorageState(String path, String targetState) {
        int retries = 30;
        String state = mStorageManager.getVolumeState(path);
        while (state != null && !state.equals(targetState) && (retries-- >= 0)) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException iex) {
                Log.e(TAG, "Interrupted while waiting for media", iex);
                break;
            }
            state = mStorageManager.getVolumeState(path);
        }

        return targetState.equals(state);
    }

    private void resetIntentFlag() {
        mSD1Mounted = false;
        mSD2Mounted = false;
        mSD1Checking = false;
        mSD2Checking = false;
        mSD1Unmounted = false;
        mSD2Unmounted = false;
        mSD1Eject = false;
        mSD2Eject = false;
        mSD1Shared = false;
        mSD2Shared = false;
        mSD1Unshared = false;
        mSD2Unshared = false;
        mSDSwap = false;
    }
*/
}
