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
import android.os.PowerManager;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;

import com.jayway.android.robotium.solo.Solo;

import com.mediatek.filemanager.FileInfo;
import com.mediatek.filemanager.FileManagerSelectFileActivity;
import com.mediatek.filemanager.MountPointHelper;
import com.mediatek.filemanager.MountPointManager;
import com.mediatek.filemanager.R;
import com.mediatek.filemanager.tests.utils.TestUtils;

import java.io.File;

public class SelectFileActivityTest extends
        ActivityInstrumentationTestCase2<FileManagerSelectFileActivity> {
    private final static String TAG = "SelectFileActivityTest";
    private FileManagerSelectFileActivity mActivity = null;
    private Instrumentation mInst = null;
    private PowerManager.WakeLock mWakeLock = null;

    private String testFolderName = "1TestSelectFileActivity";
    private String mPath = MountPointHelper.getDefaultPath() + MountPointManager.SEPARATOR
            + testFolderName;
    private String mFilePath = mPath + MountPointManager.SEPARATOR + "selectFile";
    private Solo mSolo = null;

    public SelectFileActivityTest() {
        super(FileManagerSelectFileActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInst = getInstrumentation();

        TestUtils.createDirectory(new File(mPath));
        TestUtils.createFile(new File(mFilePath));
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

    public void testSelectFileCancel() {
        mActivity = getActivity();
        View cancelButton = mActivity.findViewById(R.id.select_cancel);
        //TouchUtils.clickView(SelectFileActivityTest.this, cancelButton);
        mSolo = new Solo(mInst, mActivity);
        mSolo.clickOnView(cancelButton);
        TestUtils.sleep(3000);
        assertTrue(mActivity.isFinishing());
    }

    public void testSelectFileItemClick() {
        mActivity = getActivity();
        mSolo = new Solo(mInst, mActivity);

        FileInfo rootFileInfo = new FileInfo(MountPointManager.getInstance().getDefaultPath());
        int index = TestUtils.getListViewItemIndex(this, mActivity, rootFileInfo);
        View view = TestUtils.getItemView(mActivity, index);
        //TouchUtils.clickView(SelectFileActivityTest.this, view);
        mSolo.clickOnView(view);
        TestUtils.sleep(500);

        FileInfo testFolderInfo = new FileInfo(mPath);
        index = TestUtils.getListViewItemIndex(this, mActivity, testFolderInfo);
        view = TestUtils.getItemView(mActivity, index);
        mSolo.clickOnView(view);
        //TouchUtils.clickView(SelectFileActivityTest.this, view);
        TestUtils.sleep(500);

        FileInfo fileInfo = new FileInfo(mFilePath);
        index = TestUtils.getListViewItemIndex(this, mActivity, fileInfo);
        view = TestUtils.getItemView(mActivity, index);
        //TouchUtils.clickView(SelectFileActivityTest.this, view);
        mSolo.clickOnView(view);

        TestUtils.sleep(3000);
        assertTrue(mActivity.isFinishing());
    }

}
