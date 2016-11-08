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

import java.io.File;

import android.app.Instrumentation;
import android.app.SearchManager;
import android.content.Intent;
import android.content.ContentResolver;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.provider.MediaStore;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.widget.ListView;

import com.mediatek.filemanager.ActivityTestHelper;
import com.mediatek.filemanager.FileManagerSearchActivity;
import com.mediatek.filemanager.MountPointManager;
import com.mediatek.filemanager.R;
import com.mediatek.filemanager.tests.utils.TestUtils;

public class SearchActivityTest extends ActivityInstrumentationTestCase2<FileManagerSearchActivity> {
    private FileManagerSearchActivity mActivity = null;
    private Instrumentation mInst = null;
    private String testFolderName = "testSearchActivityTest";
    private String mPath = TestUtils.getTestPath(testFolderName);
    private String mTestFolderName = "android";
    private String mTestFolderNotExistName = "$$";

    private boolean mScanCompleted = false;

    public SearchActivityTest() {
        super(FileManagerSearchActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInst = getInstrumentation();
        TestUtils.createDirectory(new File(mPath + MountPointManager.SEPARATOR + mTestFolderName));
    }

    @Override
    protected void tearDown() throws Exception {
        if (mActivity != null) {
            mActivity.finish();
            mActivity = null;
        }
        if (mInst != null) {
            mInst = null;
        }
        super.tearDown();
    }

    protected boolean launchWithPath(String defLaunchPath, String action, String target) {
        if (defLaunchPath != null) {
            Intent intent = new Intent();
            intent.putExtra(FileManagerSearchActivity.CURRENT_PATH, defLaunchPath);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(action);
            intent.putExtra(SearchManager.QUERY, target);
            setActivityIntent(intent);
        }
        TestUtils.sleep(1000);
        mActivity = getActivity();
        ActivityTestHelper.waitingForServiceConnected(mActivity);
        TestUtils.sleep(1000);

        String curPath = ActivityTestHelper.getCurrentPath(mActivity);
        if (curPath != null && curPath.equals(defLaunchPath)) {
            // LogUtils.d(TAG, "launch path:" + defLaunchPath);
            return true;
        }
        // LogUtils.e(TAG, defLaunchPath + " is unmounted. curPath:" + curPath);
        return false;
    }

    public void testPrepareSearchFile() {

        launchWithPath(mPath, Intent.ACTION_SEARCH, mTestFolderName);
        mActivity = getActivity();
        TestUtils.sleep(1000);

        File file = new File(mPath);
        assertTrue(file.exists());

        MediaScannerConnection.OnScanCompletedListener listener = new MediaScannerConnection.OnScanCompletedListener() {
            @Override
            public void onScanCompleted(String arg0, Uri arg1) {
                mScanCompleted = true;
                Log.d("SearchActivityTest", "scan file: " + arg0);
            }
        };

        MediaScannerConnection.scanFile(mActivity, new String[]{mPath}, null, listener);
        int waitOut = 0;
        synchronized (mActivity.getApplicationContext()) {
            while ((!mScanCompleted) && (waitOut < 20000)) {
                TestUtils.sleep(200);
                waitOut += 200;
            }
        }
        Log.d("SearchActivityTest", " scanCompleted flag: " + mScanCompleted);

        ContentResolver cr = mActivity.getContentResolver();
        Uri uri = MediaStore.Files.getContentUri("external");
        String[] projection = { MediaStore.Files.FileColumns.DATA, };
        StringBuilder sb = new StringBuilder();

        sb.append(MediaStore.Files.FileColumns.FILE_NAME + " like ");
        DatabaseUtils.appendEscapedSQLString(sb, "%" + mTestFolderName + "%");
        sb.append(" and ").append(MediaStore.Files.FileColumns.DATA + " like ");
        DatabaseUtils.appendEscapedSQLString(sb, "%" + file.getParent() + "%");

        String selection = sb.toString();
        Cursor cursor = cr.query(uri, projection, selection, null, null);
        assertTrue(cursor != null);
        //assertTrue(cursor.getCount() > 0);
        cursor.close();
    }

    public void testSearchFileExists() {

        launchWithPath(mPath, Intent.ACTION_SEARCH, mTestFolderName);
        mActivity = getActivity();
        TestUtils.sleep(1000);
        ListView listView = (ListView) mActivity.findViewById(R.id.list_view);

        //assertTrue(listView.getAdapter().getCount() > 0);
        TestUtils.sleep(1000);
    }

    public void testSearchFileNotExists() {
        launchWithPath(mPath, Intent.ACTION_SEARCH, mTestFolderNotExistName);
        TestUtils.sleep(1000);
        ListView listView = (ListView) mActivity.findViewById(R.id.list_view);
        TestUtils.sleep(1000);
        assertTrue(listView.getAdapter().getCount() == 0);
    }
}

