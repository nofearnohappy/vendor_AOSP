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

package com.mediatek.filemanager.tests;

import android.test.TouchUtils;
import android.view.View;
import android.widget.ListView;

import com.mediatek.filemanager.ActivityTestHelper;
import com.mediatek.filemanager.FileInfoAdapter;
import com.mediatek.filemanager.MountPointManager;
import com.mediatek.filemanager.R;
import com.mediatek.filemanager.tests.utils.TestUtils;
import com.mediatek.filemanager.utils.LogUtils;

import java.io.File;

/**
 * Performance cases in FileManager. (Loading a directory with 10000 files;
 * copy/paste 10000 files, delete 10000 files etc)
 */
public class FileManagerPerformanceTest extends AbsOperationActivityTest {

    private final static String TAG = "FileManagerPerformanceTest";
    private final static String PERFORMANCE_PATH = TestUtils.getTestPath("test_performance");
    private final static int FOLDER_LOADING = 0;
    private final static int FOLDER_COPY_PASTE = 1;
    private final static String[] PERFORMANCE_FOLDER = {
            PERFORMANCE_PATH + MountPointManager.SEPARATOR + "10000",
            PERFORMANCE_PATH + MountPointManager.SEPARATOR + "1000" };

    /**
     * test performance of operation: copy/paste 100 files
     */
    public void test001PerformanceCopy() {
        File launchFolder = new File(PERFORMANCE_PATH);
        if (!launchFolder.exists()) {
            launchFolder.mkdirs();
        }
        // 1. assert the folder with 10000 files is exists
        File copyFolder = new File(PERFORMANCE_FOLDER[FOLDER_COPY_PASTE]);
        if (!copyFolder.exists()) {
            TestUtils.createPerformanceFolder(copyFolder, 1000);
        }

        boolean launched = launchWithPath(PERFORMANCE_PATH);
        assertTrue(launched);

        ListView listView = (ListView) mActivity.findViewById(R.id.list_view);
        FileInfoAdapter adapter = (FileInfoAdapter) listView.getAdapter();
        assertTrue(adapter.isMode(FileInfoAdapter.MODE_NORMAL));

        // 2. find the copy folder view
        int position = 0;
        final int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            if (copyFolder.getName().equals(adapter.getItem(i).getFileName())) {
                position = i;
                break;
            }
        }
        // 3. long click the view to select it.
        View copyFolderView = listView.getChildAt(position);
        assertNotNull(copyFolderView);
        TouchUtils.longClickView(this, copyFolderView);
        TestUtils.sleep(500);
        assertEquals(adapter.getCheckedItemsCount(), 1);

        // 4. click copy icon
        View copy = mActivity.findViewById(R.id.copy);
        TouchUtils.clickView(this, copy);
        TestUtils.sleep(500);

        // 5. click paste icon
        ActivityTestHelper.waitingForService(mActivity); // make sure the
        // service is aviable
        // for
        // paste
        View paste = mActivity.findViewById(R.id.paste);
        TestUtils.sleep(500);
        if (paste == null) {
            LogUtils.e(TAG, "View of Paste is null; can't get paste view");
        }
        TouchUtils.clickView(this, paste);
        LogUtils.performance("Copy/Paste 1000files start [" + System.currentTimeMillis() + "]");
        ActivityTestHelper.waitingForService(mActivity);
        LogUtils.performance("Copy/Paste 1000files end [" + System.currentTimeMillis() + "]");
        // 6. delete the copy files
        String toDelete = copyFolder.getName() + "(";
        File[] files = launchFolder.listFiles();
        for (File f : files) {
            if (f.getName().startsWith(toDelete)) {
                TestUtils.deleteFile(f);
            }
        }
    }

    /**
     * <p>
     * public void testPerformanceCut() { mActivity = getActivity();
     * ActivityTestHelper.waitingForServiceConnected(mActivity); }
     * </p>
     */
    public void test002PerformanceDelete() {
        mActivity = getActivity();
        ActivityTestHelper.waitingForServiceConnected(mActivity);
        // TODO 1. test delete 10000 files
    }

    /**
     * Test performance of loading a folder with 10000 files
     */
    public void test003PerformanceLoading() {
        File launchFolder = new File(PERFORMANCE_PATH);
        if (!launchFolder.exists()) {
            launchFolder.mkdirs();
        }
        // 1. assert the folder with 10000 files is exists
        File loadingFolder = new File(PERFORMANCE_FOLDER[FOLDER_LOADING]);
        if (!loadingFolder.exists()) {
            TestUtils.createPerformanceFolder(loadingFolder, 10000);
        }

        boolean launched = launchWithPath(PERFORMANCE_PATH);
        assertTrue(launched);
        // 2. find the loading folder view
        ListView listView = (ListView) mActivity.findViewById(R.id.list_view);
        FileInfoAdapter adapter = (FileInfoAdapter) listView.getAdapter();
        assertTrue(adapter.isMode(FileInfoAdapter.MODE_NORMAL));
        int position = 0;
        final int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            if (loadingFolder.getName().equals(adapter.getItem(i).getFileName())) {
                position = i;
                break;
            }
        }
        // 3. click the view to start loading.
        ActivityTestHelper.waitingForService(mActivity);
        View loadingView = listView.getChildAt(position);
        assertNotNull(loadingView);
        TestUtils.sleep(1500);
        TouchUtils.clickView(this, loadingView);
        LogUtils.performance("Loading 10000files start [" + System.currentTimeMillis() + "]");
        ActivityTestHelper.waitingForService(mActivity);
        LogUtils.performance("Loading 10000files end [" + System.currentTimeMillis() + "]");
    }

}
