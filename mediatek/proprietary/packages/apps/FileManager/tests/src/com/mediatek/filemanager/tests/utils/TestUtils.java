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

package com.mediatek.filemanager.tests.utils;

import android.app.Activity;
import android.content.Context;
import android.test.InstrumentationTestCase;
import android.test.TouchUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;

import com.mediatek.filemanager.AbsBaseActivity;
import com.mediatek.filemanager.ActivityTestHelper;
import com.mediatek.filemanager.FileInfo;
import com.mediatek.filemanager.FileInfoAdapter;
import com.mediatek.filemanager.FileManagerOperationActivity;
import com.mediatek.filemanager.MenuItemHelper;
import com.mediatek.filemanager.MountPointHelper;
import com.mediatek.filemanager.MountPointManager;
import com.mediatek.filemanager.R;
import com.mediatek.filemanager.utils.LogUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TestUtils {
    public final static String TAG = "TestUtils";
    public static final int BUFFER_SIZE = 256 * 1024;
    private final static String TESTFOLDER = "FileManager_Test";
    private final static String TESTFOLDER_FOR_REGRESSION = "FileManager_Regression_Test";

    public static String getTestPath(String folderName) {
        if (TextUtils.isEmpty(folderName)) {
            return MountPointHelper.getDefaultPath() + MountPointManager.SEPARATOR + TESTFOLDER;
        } else {
            return MountPointHelper.getDefaultPath() + MountPointManager.SEPARATOR + TESTFOLDER
                    + MountPointManager.SEPARATOR + folderName;
        }
    }

    public static String getRegressionTestPath(String folderName) {
        if (TextUtils.isEmpty(folderName)) {
            return MountPointHelper.getDefaultPath() + MountPointManager.SEPARATOR + TESTFOLDER_FOR_REGRESSION;
        } else {
            return MountPointHelper.getDefaultPath() + MountPointManager.SEPARATOR + TESTFOLDER_FOR_REGRESSION
                    + MountPointManager.SEPARATOR + folderName;
        }
    }

    public static void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method will get a not exist file( or hidden file) name.
     *
     * @param path
     * @return A not exist file name. Example: test_2
     */
    public static String getFileName(String path, boolean hidden) {
        final String defName = hidden ? ".test_" : "test_";
        int count = 0;
        while (true) {
            String fileName = defName + count + ".jpg";
            File file = new File(path + MountPointManager.SEPARATOR + fileName);
            if (!file.exists()) {
                return fileName;
            }
            count++;
        }
    }

    public static String getFileNameFromPath(String path) {
        if(TextUtils.isEmpty(path)) {
            return null;
        }
        int index = path.lastIndexOf(MountPointManager.SEPARATOR);
        if(index > 0) {
            return path.substring(index);
        } else {
            return null;
        }
    }

    /**
     * This method will a file if the file is not exist or is not file( is
     * directory or others)
     *
     * @param file
     */
    public static void createFile(File file) {
        if (file.exists() && file.isFile()) {
            return;
        }
        try {
            file.createNewFile();
        } catch (IOException e) {
            LogUtils.e(TAG, "createNewFile failed in creatFile(...)");
            return;
        }
    }

    /**
     * This method will create a directory if file is not exist or is not
     * directory
     *
     * @param file
     */
    public static void createDirectory(File file) {
        if (file.exists() && file.isDirectory()) {
            return;
        }
        file.mkdirs();
    }

    /**
     * This method will delete curFile itself and all of its contains on
     * FileSystem.
     *
     * @param curFile
     */
    public static void deleteFile(File curFile) {
        if (!curFile.exists()) {
            return;
        }
        if (curFile.isDirectory()) {
            File[] files = curFile.listFiles();
            for (File file : files) {
                deleteFile(file);
            }
            curFile.delete();
        } else {
            curFile.delete();
        }
    }

    public static void createPerformanceFolder(File folder, int size) {
        if (!folder.exists()) {
            folder.mkdir();
        }

        final String PATH = folder.getAbsolutePath();
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            for (File f : files) {
                f.delete();
            }

            for (int i = 0; i < size; i++) {
                new File(PATH + MountPointManager.SEPARATOR + "file_" + i).mkdir();
            }
        }
    }

    public static boolean copyRawFile(Context context, int resId, File dstFile) {
        InputStream in = null;
        OutputStream out = null;
        byte[] buffer = new byte[BUFFER_SIZE];
        int length = 0;
        try {
            in = context.getResources().openRawResource(resId);
            out = new FileOutputStream(dstFile);
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException in copyRawFile");
            return false;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
                LogUtils.i(TAG, "IOException in copyRawFile");
                return false;
            }
        }
        return true;
    }

    public static class TestFlag {
        public boolean threadIsRunning;
    }

    public abstract static class TestCaseRunnable implements Runnable {
        TestFlag mFlag;

        TestCaseRunnable(TestFlag testFlag) {
            mFlag = testFlag;
        }

        @Override
        public void run() {
            mFlag.threadIsRunning = false;
        }
    }

    public static class EditTextRunnable extends TestCaseRunnable {
        EditText mEditText;
        String mText;

        public EditTextRunnable(TestFlag testFlag, EditText editText, String text) {
            super(testFlag);
            mEditText = editText;
            mText = text;
            // TODO Auto-generated constructor stub
        }

        @Override
        public void run() {
            mEditText.setText(mText);
            super.run();
        }
    }

    public static class ClickOptionMenuRunnable extends TestCaseRunnable {
        FileManagerOperationActivity mActivity;
        MenuItemHelper mMenuItem;

        public ClickOptionMenuRunnable(TestFlag testFlag, FileManagerOperationActivity activity,
                MenuItemHelper menuItem) {
            super(testFlag);
            mActivity = activity;
            mMenuItem = menuItem;
        }

        @Override
        public void run() {
            ListView listView = (ListView) mActivity.findViewById(R.id.list_view);
            if (((FileInfoAdapter) listView.getAdapter()).isMode(FileInfoAdapter.MODE_NORMAL)) {
                mActivity.onOptionsItemSelected(mMenuItem);
            } else if (((FileInfoAdapter) listView.getAdapter()).isMode(FileInfoAdapter.MODE_EDIT)) {
                ActivityTestHelper.onActionModeActionItem(mActivity, mMenuItem);
            }
            super.run();
        }
    }

    public static class ClickPopupMenuRunnable extends TestCaseRunnable {
        FileManagerOperationActivity mActivity;
        MenuItemHelper mMenuItem;

        public ClickPopupMenuRunnable(TestFlag testFlag, FileManagerOperationActivity activity,
                MenuItemHelper menuItem) {
            super(testFlag);
            mActivity = activity;
            mMenuItem = menuItem;
        }

        @Override
        public void run() {
            ListView listView = (ListView) mActivity.findViewById(R.id.list_view);
            if (((FileInfoAdapter) listView.getAdapter()).isMode(FileInfoAdapter.MODE_NORMAL)) {
                mActivity.onOptionsItemSelected(mMenuItem);
            } else if (((FileInfoAdapter) listView.getAdapter()).isMode(FileInfoAdapter.MODE_EDIT)) {
                ActivityTestHelper.onActionModePopupMenu(mActivity, mMenuItem);
            }
            super.run();
        }
    }

    public static class SetListViewRunnable extends TestCaseRunnable {
        ListView mListView;
        int mIndex;

        public SetListViewRunnable(TestFlag testFlag, ListView listView, int indexOfAdapter) {
            super(testFlag);
            mListView = listView;
            mIndex = indexOfAdapter;
        }

        @Override
        public void run() {
            mListView.setSelectionFromTop(mIndex, 0);
            super.run();
        }
    }

    /*
     * TestFlag of runnable must be same as this parameter testFlag
     */
    public static void runUiThread(InstrumentationTestCase testCase, TestCaseRunnable runnable,
            TestFlag testFlag) {
        // final TestFlag testFlag = new TestFlag();
        testFlag.threadIsRunning = true;
        try {
            testCase.runTestOnUiThread(runnable);
            while (testFlag.threadIsRunning) {
                TestUtils.sleep(100);
                LogUtils.i(TAG, "waitingForUiThreadFinished - " + testFlag.threadIsRunning);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            testFlag.threadIsRunning = false;
        }
        TestUtils.sleep(500);
    }

    public static int getListViewItemIndex(InstrumentationTestCase testCase,
            AbsBaseActivity activity, FileInfo fileInfo) {

        while (ActivityTestHelper.isServiceBusy(activity)) {
            TestUtils.sleep(500);
        }

        final ListView listView = (ListView) activity.findViewById(R.id.list_view);
        final FileInfoAdapter adapter = (FileInfoAdapter) listView.getAdapter();
        final int indexOfAdapter = adapter.getPosition(fileInfo);

        final TestFlag testFlag = new TestFlag();
        testFlag.threadIsRunning = true;
        try {
            testCase.runTestOnUiThread(new Runnable() {

                @Override
                public void run() {
                    Log.e(TAG, "indexOfAdapter = " + indexOfAdapter);
                    listView.setSelectionFromTop(indexOfAdapter, 0);
                    adapter.notifyDataSetChanged();
                    Log.e(TAG, "notifyDataSetChanged");
                    testFlag.threadIsRunning = false;
                }
            });
            while (testFlag.threadIsRunning) {
                TestUtils.sleep(100);
                LogUtils.i(TAG, "waitingForUiThreadFinished - " + testFlag.threadIsRunning);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            testFlag.threadIsRunning = false;
        }
        TestUtils.sleep(500);
        int indexOfFirstItem = listView.getFirstVisiblePosition();
        int indexOnView = indexOfAdapter - indexOfFirstItem;
        Log.d(TAG, " " + indexOfAdapter + "--" + indexOfFirstItem + "--" + indexOnView);
        // if (0 == indexOnView) {
        // listView.setSelectionFromTop(indexOfAdapter, 0);
        return indexOnView;
    }

    public static View getItemView(Activity activity, int indexOnView) {
        ListView listView = (ListView) activity.findViewById(R.id.list_view);
        if (listView.getCount() < 1) {
            LogUtils.d(TAG, "cann't select a file(folder) in getSelectItem()");
            return null;
        }
        View selectView = listView.getChildAt(indexOnView);
        if (selectView != null) {
            return selectView;
        } else {
            return null;
        }
    }

    public static int getListViewCount(Activity activity) {
        ListView listView = (ListView) activity.findViewById(R.id.list_view);
        int count = listView.getCount();
        LogUtils.d(TAG, "getListViewCount = " + count);
        return count;
    }

    public static boolean selectOneItem(InstrumentationTestCase testCase, Activity activity,
            int indexOnView) {
        ListView listView = (ListView) activity.findViewById(R.id.list_view);
        View selectView = getItemView(activity, indexOnView);
        if (selectView == null) {
            return false;
        }
        if (((FileInfoAdapter) listView.getAdapter()).isMode(FileInfoAdapter.MODE_NORMAL)) {
            TouchUtils.longClickView(testCase, selectView);
            sleep(500);
        }
        if (((FileInfoAdapter) listView.getAdapter()).getCheckedItemsCount() == 0) {
            return false;
        }
        return true;
    }

    public static boolean selectAllItems(InstrumentationTestCase testCase, Activity activity) {
        ListView listView = (ListView) activity.findViewById(R.id.list_view);
        if (!selectOneItem(testCase, activity, 0)) {
            return false;
        }
        if (!((FileInfoAdapter) listView.getAdapter()).isMode(FileInfoAdapter.MODE_EDIT)) {
            return false;
        }
        if (((FileInfoAdapter) listView.getAdapter()).getCheckedItemsCount() != listView.getCount()) {
            MenuItemHelper menuItem = new MenuItemHelper(R.id.select);
            TestUtils.TestFlag testFlag = new TestUtils.TestFlag();
            ClickPopupMenuRunnable clickMenuRunnable = new ClickPopupMenuRunnable(testFlag,
                    (FileManagerOperationActivity) activity, menuItem);
            TestUtils.runUiThread(testCase, clickMenuRunnable, testFlag);
        }
        return true;
    }

    public static boolean clickOneItem(InstrumentationTestCase testCase, Activity activity,
            int indexOnView) {
        ListView listView = (ListView) activity.findViewById(R.id.list_view);
        if (((FileInfoAdapter) listView.getAdapter()).isMode(FileInfoAdapter.MODE_EDIT)
                && ((FileInfoAdapter) listView.getAdapter()).getCheckedItemsCount() != 0) {
            return true;
        }
        if (((FileInfoAdapter) listView.getAdapter()).isMode(FileInfoAdapter.MODE_NORMAL)) {
            View selectView = getItemView(activity, indexOnView);
            if (selectView == null) {
                return false;
            }
            TouchUtils.clickView(testCase, selectView);
            return true;
        }
        return true;
    }

    public static void clickViewWithIMEHiden(InstrumentationTestCase test, Activity activity, View v) {
        // hide soft input
        InputMethodManager imm = (InputMethodManager) activity
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isActive()) {
            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT,
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
        sleep(500);
        TouchUtils.clickView(test, v);
    }

    public static class SortFileRunnable extends TestCaseRunnable {
        FileManagerOperationActivity mActivity;
        MenuItemHelper mMenuItem;

        public SortFileRunnable(TestFlag testFlag, FileManagerOperationActivity activity,
                MenuItemHelper menuItem) {
            super(testFlag);
            mActivity = activity;
            mMenuItem = menuItem;
        }

        @Override
        public void run() {
            mActivity.onOptionsItemSelected(mMenuItem);
            super.run();
        }
    }

}
