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

import android.app.Instrumentation;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.widget.ImageButton;

import com.jayway.android.robotium.solo.Solo;

import com.mediatek.filemanager.ActivityTestHelper;
import com.mediatek.filemanager.FileInfo;
import com.mediatek.filemanager.FileManagerSelectPathActivity;
import com.mediatek.filemanager.MountPointManager;
import com.mediatek.filemanager.R;
import com.mediatek.filemanager.tests.utils.TestUtils;

import java.io.File;


public class SelectPathActivityTest extends
        ActivityInstrumentationTestCase2<FileManagerSelectPathActivity> {
    private FileManagerSelectPathActivity mActivity = null;
    private Instrumentation mInst = null;
    private String testFolderName = "testSelectPathActivityTest";
    private String mPath = TestUtils.getTestPath(testFolderName);
    private String mFolderPath = mPath + MountPointManager.SEPARATOR + "TestSelectPathItemFolder";
    private String mFilePath = mFolderPath + MountPointManager.SEPARATOR + "TestSelectPathItemFile";
    private Solo mSolo = null;

    public SelectPathActivityTest() {
        super(FileManagerSelectPathActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInst = getInstrumentation();
        TestUtils.createDirectory(new File(mFolderPath));

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

    protected boolean launchWithPath(String defLaunchPath) {
        if (defLaunchPath != null) {
            Intent intent = new Intent();
            intent.putExtra(FileManagerSelectPathActivity.DOWNLOAD_PATH_KEY, defLaunchPath);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            setActivityIntent(intent);
        }
        TestUtils.sleep(1000);
        mActivity = getActivity();
        mSolo = new Solo(mInst, mActivity);
        ActivityTestHelper.waitingForServiceConnected(mActivity);
        TestUtils.sleep(500);

        String curPath = ActivityTestHelper.getCurrentPath(mActivity);
        if (curPath != null && curPath.equals(defLaunchPath)) {
            // LogUtils.d(TAG, "launch path:" + defLaunchPath);
            return true;
        }
        // LogUtils.e(TAG, defLaunchPath + " is unmounted. curPath:" + curPath);
        return false;
    }

    public void testSelectPath() {

        launchWithPath(mPath);
        mActivity = getActivity();
        FileInfo fileInfo = new FileInfo(mFolderPath);

        int index = TestUtils.getListViewItemIndex(this, mActivity, fileInfo);
        View view = TestUtils.getItemView(mActivity, index);
        //TouchUtils.clickView(this, view);
        mSolo.clickOnView(view);
        assertFalse(mActivity.isFinishing());
        TestUtils.sleep(500);

        View okButton = mActivity.findViewById(R.id.download_btn_save);
        //TouchUtils.clickView(this, okButton);
        mSolo.clickOnView(okButton);
        TestUtils.sleep(3000);
        assertTrue(mActivity.isFinishing());
    }

    public void testCancelSelect() {
        launchWithPath(mPath);
        mActivity = getActivity();
        View cancelButton = mActivity.findViewById(R.id.download_btn_cancel);
        //TouchUtils.clickView(this, cancelButton);
        mSolo.clickOnView(cancelButton);
        TestUtils.sleep(3000);
        assertTrue(mActivity.isFinishing());
    }

    public void testSelectNewFolder() {
        mActivity = getActivity();
        View mBtnCreateFolder = (ImageButton) mActivity.findViewById(R.id.btn_create_folder);
        assertNotNull(mBtnCreateFolder);
    }

}
