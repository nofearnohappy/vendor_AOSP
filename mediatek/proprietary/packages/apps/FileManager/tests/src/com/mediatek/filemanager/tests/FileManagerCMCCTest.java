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

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.KeyEvent;

import com.mediatek.filemanager.ActivityTestHelper;
import com.mediatek.filemanager.FileInfo;
import com.mediatek.filemanager.MenuItemHelper;
import com.mediatek.filemanager.MountPointManager;
import com.mediatek.filemanager.R;
import com.mediatek.filemanager.service.FileManagerService.OperationEventListener;
import com.mediatek.filemanager.tests.utils.TestUtils;
import com.mediatek.filemanager.utils.FileUtils;
import com.mediatek.filemanager.utils.LogUtils;
import com.mediatek.filemanager.tests.annotation.*;

import java.io.File;

/**
 * Test CMCC cases.
 */
public class FileManagerCMCCTest extends AbsOperationActivityTest {
    private final static String TAG = "FileManagerCMCCTest";

    public FileManagerCMCCTest() {
        super();
    }

    public void test001CMCCLaunch() {
        LogUtils.i(TAG, "call testCMCCLaunch");
        mActivity = getActivity();
        ActivityTestHelper.waitingForServiceConnected(mActivity);
    }

    public void test002LaunchNotExistPath() {
        LogUtils.i(TAG, "call testLaunchNotExsitPath");
        final String curFolderName = "NotExistPath";
        final String curPath = TestUtils.getTestPath(curFolderName);
        File loadFile = new File(curPath);
        TestUtils.deleteFile(loadFile);
        boolean launched = launchWithPath(curPath);
        assertFalse(launched);
    }

    @UiAnnotation
    public void test003CreateFolder() {
        LogUtils.i(TAG, "call testCreateFolder");
        final String curFolderName = "testCreateFolder";
        final String curPath = TestUtils.getTestPath(curFolderName);
        File loadFile = new File(curPath);
        TestUtils.deleteFile(loadFile);
        TestUtils.createDirectory(loadFile);
        boolean launched = launchWithPath(curPath);
        assertTrue(launched);
        TestUtils.sleep(2000);

        // Get a file name that the file is not exists
        String folderName = TestUtils.getFileName(curPath, false);

        // 1. test create a new folder.(The folder is not exists)
        createFolder(folderName);
        File newFolderFile = new File(curPath + MountPointManager.SEPARATOR + folderName);
        assertTrue(newFolderFile.exists());

        // 2. test create a folder. (The folder is exists)
        createFolder(folderName);

        // 3. test create a folder with an invalid name
        String invalidName = "?%^&$!@~";
        createFolder(invalidName);
        File invalidFolderFile = new File(curPath + MountPointManager.SEPARATOR + invalidName);
        assertFalse(invalidFolderFile.exists());

        // 4. test create a folder with a name too long
        StringBuilder sb = new StringBuilder();
        while (OperationEventListener.ERROR_CODE_NAME_TOO_LONG != FileUtils.checkFileName(sb
                .toString())) {
            sb.append("A");
        }
        String tooLongName = sb.toString();
        createFolder(tooLongName);
        File tooLongFolderFile = new File(curPath + MountPointManager.SEPARATOR + tooLongName);
        assertFalse(tooLongFolderFile.exists());

        // 5. test create hidden a hidden folder
        String hiddenFolderName = TestUtils.getFileName(curPath, true);
        createFolder(hiddenFolderName);
        File newHiddenFolderFile = new File(curPath + MountPointManager.SEPARATOR
                + hiddenFolderName);
        assertTrue(newHiddenFolderFile.exists());

        // delete the create folders
        TestUtils.deleteFile(newFolderFile);
        TestUtils.deleteFile(newHiddenFolderFile);
    }

    @UiAnnotation
    public void test004RenameFolder() {
        LogUtils.i(TAG, "call testRenameFolder");

        final String curFolderName = "testRenameFolder";
        final String curPath = TestUtils.getTestPath(curFolderName);
        // check the launch path is exist or not. if not, create.
        File loadFile = new File(curPath);
        TestUtils.deleteFile(loadFile);
        TestUtils.createDirectory(loadFile);
        boolean launched = launchWithPath(curPath);
        assertTrue(launched);

        // create a new folder
        String folderName = TestUtils.getFileName(curPath, false);
        createFolder(folderName);

        // 1. test rename a folder with an invalid name.
        String invalidFolderName = "?%^&$!@~";
        File oldFolderFile = new File(curPath + MountPointManager.SEPARATOR + folderName);
        FileInfo fileInfo = new FileInfo(oldFolderFile);
        renameFile(fileInfo, invalidFolderName, true);
        assertTrue(oldFolderFile.exists());
        File invalidFolderFile = new File(curPath + MountPointManager.SEPARATOR + invalidFolderName);
        assertFalse(invalidFolderFile.exists());

        // 2. test rename a folder with a too long name
        StringBuilder sb = new StringBuilder();
        while (OperationEventListener.ERROR_CODE_NAME_TOO_LONG != FileUtils.checkFileName(sb
                .toString())) {
            sb.append("A");
        }
        String tooLongFolderName = sb.toString();
        renameFile(fileInfo, tooLongFolderName, true);
        assertTrue(oldFolderFile.exists());
        File tooLongFolderFile = new File(curPath + MountPointManager.SEPARATOR + tooLongFolderName);
        assertFalse(tooLongFolderFile.exists());

        // 3. test rename a folder with an exist name
        renameFile(fileInfo, folderName, true);
        assertTrue(oldFolderFile.exists());

        // 4. test rename a folder with valid new name, but cancel
        String newFolderName = TestUtils.getFileName(curPath, false);
        renameFile(fileInfo, newFolderName, false);
        File newFolderFile = new File(curPath + MountPointManager.SEPARATOR + newFolderName);
        assertFalse(newFolderFile.exists());
        assertTrue(oldFolderFile.exists());

        // 5. test rename a folder with valid new name, and certain
        renameFile(fileInfo, newFolderName, true);
        assertTrue(newFolderFile.exists());
        assertFalse(oldFolderFile.exists());

        // 6. test rename a folder with a hidden type name
        String newHiddenName = TestUtils.getFileName(curPath, true);
        fileInfo = new FileInfo(newFolderFile);
        renameFile(fileInfo, newHiddenName, true);
        File newHiddenFolderFile = new File(curPath + MountPointManager.SEPARATOR + newHiddenName);
        assertTrue(newHiddenFolderFile.exists());
        assertFalse(newFolderFile.exists());

        // delete created folder
        TestUtils.deleteFile(newHiddenFolderFile);

    }

    public void test005RenameFileExtension() {
        LogUtils.i(TAG, "call testRenameFileExtension");

        final String curFolderName = "testRenameFileExtension";
        final String curPath = TestUtils.getTestPath(curFolderName);
        File loadFile = new File(curPath);
        TestUtils.deleteFile(loadFile);
        TestUtils.createDirectory(loadFile);

        String fileName = TestUtils.getFileName(curPath, false);
        fileName = fileName + ".txt";
        File file = new File(curPath + MountPointManager.SEPARATOR + fileName);
        FileInfo fileInfo = new FileInfo(file);
        TestUtils.createFile(file);

        boolean launched = launchWithPath(curPath);
        assertTrue(launched);

        // test rename a file, change its suffix, but cancel
        String newFileName = fileName + "t";
        renameFileExtension(fileInfo, newFileName, false);
        assertTrue(file.exists());
        File newFile = new File(curPath + MountPointManager.SEPARATOR + newFileName);
        assertFalse(newFile.exists());

        // test rename a file, change its suffix, and certain
        renameFileExtension(fileInfo, newFileName, true);
        assertFalse(file.exists());
        assertTrue(newFile.exists());

        // delete created file
        TestUtils.deleteFile(newFile);
    }

    public void test006DeleteFile() {
        // TODO 1. test delete a exists file
        LogUtils.i(TAG, "call testDeleteFile");
        final String curFolderName = "testDeleteFile";
        final String curPath = TestUtils.getTestPath(curFolderName);
        File loadFile = new File(curPath);
        TestUtils.deleteFile(loadFile);
        TestUtils.createDirectory(loadFile);

        String fileName = TestUtils.getFileName(curPath, false);
        File file = new File(curPath + MountPointManager.SEPARATOR + fileName);
        FileInfo fileInfo = new FileInfo(file);
        TestUtils.createFile(file);

        boolean launched = launchWithPath(curPath);
        assertTrue(launched);

        // delete a file
        deleteFile(fileInfo, false);
        assertTrue(file.exists());
        deleteFile(fileInfo, true);
        assertFalse(file.exists());

        // create new folders
        for (int i = 0; i < 3; i++) {
            String folderName = TestUtils.getFileName(curPath, false);
            createFolder(folderName);
        }
        // delete all
        deleteAll(false);
        deleteAll(true);
    }

    public void test007Detail() {
        LogUtils.i(TAG, "call testDetail");

        String curFolderName = "testDetail";
        final String curPath = TestUtils.getTestPath(curFolderName);
        File loadFile = new File(curPath);
        TestUtils.deleteFile(loadFile);
        TestUtils.createDirectory(loadFile);

        String fileName = TestUtils.getFileName(curPath, false);
        File file = new File(curPath + MountPointManager.SEPARATOR + fileName
                + MountPointManager.SEPARATOR + "minutes.mp3");
        Log.e(TAG, file.getAbsolutePath());
        File folderFile = new File(curPath + MountPointManager.SEPARATOR + fileName);
        FileInfo fileInfo = new FileInfo(folderFile);
        TestUtils.createDirectory(folderFile);
        TestUtils.createFile(file);

        TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                com.mediatek.filemanager.tests.R.raw.minutes, file);

        boolean launched = launchWithPath(curPath);
        assertTrue(launched);

        getDetail(fileInfo);
        copyFile(fileInfo, true);
        paste();
    }

    public void test008ShowHidden() {
        LogUtils.i(TAG, "call testShowHidden");
        final String curFolderName = "testShowHidden";
        final String hiddenFile = ".YouCanNotSeeMe";
        final String PREF_SHOW_HIDEN_FILE = "pref_show_hiden_file";
        final String curPath = TestUtils.getTestPath(curFolderName);
        int count = 0;

        File testFolder = new File(curPath);
        File testFile = new File(curPath + MountPointManager.SEPARATOR + hiddenFile);
        TestUtils.deleteFile(testFolder);
        TestUtils.deleteFile(testFile);
        TestUtils.createDirectory(testFolder);
        TestUtils.createFile(testFile);

        boolean launched = launchWithPath(curPath);
        assertTrue(launched);

        SharedPreferences sp = mActivity.getPreferences(Context.MODE_PRIVATE);
        boolean isShowHidden = sp.getBoolean(PREF_SHOW_HIDEN_FILE, false);
        if (isShowHidden) {
            assertTrue(TestUtils.getListViewCount(mActivity) == 1);

            ActivityTestHelper.waitingForService(mActivity);
            MenuItemHelper menuItem = new MenuItemHelper(R.id.hide);
            mActivity.onOptionsItemSelected(menuItem);
            ActivityTestHelper.waitingForService(mActivity);
            // mActivity.onMenuItemClick(menuItem);
            TestUtils.sleep(100);
            assertTrue(TestUtils.getListViewCount(mActivity) == 0);
        } else {
            assertTrue(TestUtils.getListViewCount(mActivity) == 0);

            ActivityTestHelper.waitingForService(mActivity);
            MenuItemHelper menuItem = new MenuItemHelper(R.id.hide);
            mActivity.onOptionsItemSelected(menuItem);
            ActivityTestHelper.waitingForService(mActivity);
            // mActivity.onMenuItemClick(menuItem);
            TestUtils.sleep(100);
            assertTrue(TestUtils.getListViewCount(mActivity) == 1);
        }
    }

    public void test009Sort() {
        LogUtils.i(TAG, "call testSort");
        final String curFolderName = null;
        final String curPath = TestUtils.getTestPath(curFolderName);
        boolean launched = launchWithPath(curPath);
        assertTrue(launched);
        for (int i = 0; i < 4; i++) {
            setSort(i);
            assertEquals(i, ActivityTestHelper.getSortType(mActivity));
        }
        // the function below will make sortDialog touch cancel button
        setSort(3);
        assertEquals(3, ActivityTestHelper.getSortType(mActivity));
    }

    public void test010CopyPaste() {
        LogUtils.i(TAG, "call testCopyPaste()");
        final String curFolderName = "testCopyPaste";
        final String curPath = TestUtils.getTestPath(curFolderName);
        File loadFile = new File(curPath);
        TestUtils.deleteFile(loadFile);
        TestUtils.createDirectory(loadFile);

        String folderName = TestUtils.getFileName(curPath, false);
        File folderFile = new File(curPath + MountPointManager.SEPARATOR + folderName);
        FileInfo fileInfo = new FileInfo(folderFile);
        TestUtils.createDirectory(folderFile);

        for (int i = 0; i < 5; i++) {
            String fileName = TestUtils.getFileName(curPath, false);
            fileName = fileName + ".txt";
            File file = new File(curPath + MountPointManager.SEPARATOR + fileName);
            TestUtils.createFile(file);
        }

        boolean launched = launchWithPath(curPath);
        assertTrue(launched);

        // copy to sub folder
        copyFile(fileInfo, false);
        int index = TestUtils.getListViewItemIndex(this, mActivity, fileInfo);
        TestUtils.clickOneItem(this, mActivity, index);
        TestUtils.sleep(500);
        paste();

        // copy to same folder
        mInst.sendCharacterSync(KeyEvent.KEYCODE_BACK);
        TestUtils.sleep(500);
        paste();

        // copy all to same folder, contains a folder with maxLength name(with
        // will cause copy fail)
        StringBuilder sb = new StringBuilder();
        while (OperationEventListener.ERROR_CODE_NAME_TOO_LONG != FileUtils.checkFileName(sb
                .toString())) {
            sb.append("A");
        }
        sb.deleteCharAt(sb.length() - 1);
        String maxName = sb.toString();
        createFolder(maxName);
        copyFile(null, true);
        TestUtils.sleep(500);
        paste();

        folderFile = new File(curPath + MountPointManager.SEPARATOR + folderName + "(0)");
        File maxLengthFile = new File(curPath + MountPointManager.SEPARATOR + maxName + "(0)");
        assertTrue(folderFile.exists());
        assertFalse(maxLengthFile.exists());
    }

    public void test011CutPasteInSameCard() {
        LogUtils.i(TAG, "call testCutPasteInSameCard");
        final String curFolderName = "testCutPaste";
        final String curPath = TestUtils.getTestPath(curFolderName);
        File loadFile = new File(curPath);
        TestUtils.deleteFile(loadFile);
        TestUtils.createDirectory(loadFile);

        String fileName01 = TestUtils.getFileName(curPath, false);
        File folderFile01 = new File(curPath + MountPointManager.SEPARATOR + fileName01);
        FileInfo fileInfo01 = new FileInfo(folderFile01);
        TestUtils.createDirectory(folderFile01);
        String fileName02 = TestUtils.getFileName(curPath, false);
        File folderFile02 = new File(curPath + MountPointManager.SEPARATOR + fileName02);
        FileInfo fileInfo02 = new FileInfo(folderFile02);
        TestUtils.createDirectory(folderFile02);

        boolean launched = launchWithPath(curPath);
        assertTrue(launched);
        // cut to sub folder
        cutFile(fileInfo01, false);
        int index = TestUtils.getListViewItemIndex(this, mActivity, fileInfo01);
        TestUtils.clickOneItem(this, mActivity, index);
        TestUtils.sleep(1000);
        paste();
        assertTrue(folderFile01.exists());
        // cut to same folder
        mInst.sendCharacterSync(KeyEvent.KEYCODE_BACK);
        TestUtils.sleep(500);
        cutFile(fileInfo01, false);
        paste();
        assertTrue(folderFile01.exists());
        // cut to other folder
        TestUtils.sleep(500);
        cutFile(fileInfo01, false);
        index = TestUtils.getListViewItemIndex(this, mActivity, fileInfo02);
        TestUtils.clickOneItem(this, mActivity, index);
        TestUtils.sleep(1000);
        paste();
        assertFalse(folderFile01.exists());
    }

    public void test012CutPasteInDiffCard() {
        LogUtils.i(TAG, "call testCutPasteInDiffCard");
        assertTrue(launchWithPath(TestUtils.getTestPath(null)));
        MountPointManager mountPointManager = MountPointManager.getInstance();
        int count = mountPointManager.getMountPointFileInfo().size();
        if (count < 2) {
            LogUtils.e(TAG, "Only phone storage,  cancel this test case");
            return;
        }
        final String curFolderName = "testCutPasteInDiffCard";
        final String SDPath = "/storage/sdcard1";
        final String curPath = TestUtils.getTestPath(curFolderName);
        File loadFile = new File(curPath);
        TestUtils.deleteFile(loadFile);
        TestUtils.createDirectory(loadFile);

        File dstFolder = new File(SDPath + MountPointManager.SEPARATOR + curFolderName);
        TestUtils.deleteFile(dstFolder);
        TestUtils.createDirectory(dstFolder);
        FileInfo dstFileInfo = new FileInfo(dstFolder);

        String fileName01 = TestUtils.getFileName(curPath, false);
        File folderFile01 = new File(curPath + MountPointManager.SEPARATOR + fileName01);
        FileInfo fileInfo01 = new FileInfo(folderFile01);
        TestUtils.createDirectory(folderFile01);

        String fileName02 = TestUtils.getFileName(curPath, false);
        File file02 = new File(curPath + MountPointManager.SEPARATOR + fileName02);
        FileInfo fileInfo02 = new FileInfo(file02);
        TestUtils.createFile(file02);

        boolean launched = launchWithStartActivity(curPath);
        assertTrue(launched);
        // cut to other folder
        TestUtils.sleep(500);
        // cutFile(fileInfo01, false);
        cutFile(fileInfo01, true);

        // switch to SD card
        assertTrue(launchWithStartActivity(dstFolder.getAbsolutePath()));

        paste();
        TestUtils.sleep(100);
        assertFalse(folderFile01.exists());
    }

    // public void testGetSelectOnUi() {
    // LogUtils.i(TAG, "call testGetSelectOnUi");
    // final String curFolderName = "testGetSelectOnUi";
    // final String curPath = TestUtils.getTestPath(curFolderName);
    // File loadFile = new File(curPath);
    // TestUtils.deleteFile(loadFile);
    // TestUtils.createDirectory(loadFile);
    //
    // // create a file and a folder
    // String fileName01 = "MINE.txt";
    // final File file01 = new File(curPath + MountPointManager.SEPARATOR +
    // fileName01);
    // TestUtils.createFile(file01);
    //
    // for (int i = 0; i < 3; i++) {
    // String fileName = TestUtils.getFileName(curPath, false);
    // File file = new File(curPath + MountPointManager.SEPARATOR + fileName);
    // TestUtils.createFile(file);
    // }
    //
    // for (int i = 0; i < 10; i++) {
    // String folderName = TestUtils.getFileName(curPath, false);
    // File folder = new File(curPath + MountPointManager.SEPARATOR + folderName
    // + MountPointManager.SEPARATOR + folderName);
    // TestUtils.createDirectory(folder);
    // }
    // boolean launched = launchWithPath(curPath);
    // assertTrue(launched);
    //
    // FileInfo fileInfo = new FileInfo(file01);
    // TestUtils.getListViewItemIndex(this, mActivity, fileInfo);
    // deleteFile(fileInfo, true);
    // assertFalse(file01.exists());
    // }

    public void test013Share() {
        /*
        LogUtils.i(TAG, "call testShare");
        final String curFolderName = "testShare";
        final String curPath = TestUtils.getTestPath(curFolderName);
        File loadFile = new File(curPath);
        TestUtils.deleteFile(loadFile);
        TestUtils.createDirectory(loadFile);

        String fileName = TestUtils.getFileName(curPath, false);
        final File file = new File(curPath + MountPointManager.SEPARATOR + fileName);
        FileInfo fileInfo = new FileInfo(file);
        TestUtils.createFile(file);

        boolean launched = launchWithPath(curPath);
        assertTrue(launched);

        shareFile(fileInfo, false);
        mInst.sendCharacterSync(KeyEvent.KEYCODE_BACK);

        // share multiple files
        File newFile;
        FileInfo newFileInfo;
        for (int i = 0; i < 5; i++) {
            fileName = TestUtils.getFileName(curPath, false);
            newFile = new File(curPath + MountPointManager.SEPARATOR + fileName);
            TestUtils.createFile(newFile);
        }
        shareFile(null, true);
        mInst.sendCharacterSync(KeyEvent.KEYCODE_BACK);
        // delete all test files in current path
        TestUtils.deleteFile(loadFile);
        */
    }

    public void test014DrmFile() {
        LogUtils.i(TAG, "call testDrmFile");

        final String curFolderName = "testDrmFile";
        final String curPath = TestUtils.getTestPath(curFolderName);
        File loadFile = new File(curPath);
        TestUtils.deleteFile(loadFile);
        TestUtils.createDirectory(loadFile);

        String fileName01 = "pictrue.dcf";
        File file01 = new File(curPath + MountPointManager.SEPARATOR + fileName01);
        FileInfo fileInfo01 = new FileInfo(file01);
        TestUtils.createFile(file01);

        String fileName02 = "audio.dcf";
        final File file02 = new File(curPath + MountPointManager.SEPARATOR + fileName02);
        FileInfo fileInfo02 = new FileInfo(file02);
        TestUtils.createFile(file02);

        TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                com.mediatek.filemanager.tests.R.raw.picture, file01);
        TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                com.mediatek.filemanager.tests.R.raw.audio, file02);

        boolean launched = launchWithPath(curPath);
        assertTrue(launched);

        getProtectInfo(fileInfo01);
        mInst.sendCharacterSync(KeyEvent.KEYCODE_BACK);
        TestUtils.sleep(1000);

        copyFile(null, true);
        paste();
        deleteFile(fileInfo01, true);

        shareFile(fileInfo02, false);
        mInst.sendCharacterSync(KeyEvent.KEYCODE_BACK);
        shareFile(null, true);
        mInst.sendCharacterSync(KeyEvent.KEYCODE_BACK);

    }

    public void test015Cancel() {
        LogUtils.i(TAG, "call testCancel()");
        final String curFolderName = "test_performance";
        final String curPath = TestUtils.getTestPath(curFolderName);
        File loadFile = new File(curPath);
        TestUtils.createDirectory(loadFile);

        File copyFolder = new File(curPath + MountPointManager.SEPARATOR + "1000");
        FileInfo fileInfo = new FileInfo(copyFolder);
        if (!copyFolder.exists()) {
            TestUtils.createPerformanceFolder(copyFolder, 1000);
        }
        boolean launched = launchWithPath(curPath);
        assertTrue(launched);
        copyCancel(fileInfo, false);
    }

    public void test016ClickItem() {
        LogUtils.i(TAG, "call testClickItem()");
        String curFolderName = "testClickItem";
        final String curPath = TestUtils.getTestPath(curFolderName);
        File loadFile = new File(curPath);
        TestUtils.deleteFile(loadFile);
        TestUtils.createDirectory(loadFile);

        // create a file and a folder
        String fileName = TestUtils.getFileName(curPath, false);
        File folderFile = new File(curPath + MountPointManager.SEPARATOR + fileName);
        FileInfo folderFileInfo = new FileInfo(folderFile);
        TestUtils.createDirectory(folderFile);
        fileName = fileName + ".t";
        File file = new File(curPath + MountPointManager.SEPARATOR + fileName);
        FileInfo fileInfo = new FileInfo(file);
        TestUtils.createFile(file);
        boolean launched = launchWithPath(curPath);
        assertTrue(launched);

        TestUtils.sleep(1000);
        int index = TestUtils.getListViewItemIndex(this, mActivity, folderFileInfo);
        TestUtils.clickOneItem(this, mActivity, index);
        TestUtils.sleep(1000);
        mInst.sendCharacterSync(KeyEvent.KEYCODE_BACK);
        TestUtils.sleep(1000);

        index = TestUtils.getListViewItemIndex(this, mActivity, fileInfo);
        TestUtils.clickOneItem(this, mActivity, index);
        TestUtils.sleep(1000);
        mInst.sendCharacterSync(KeyEvent.KEYCODE_BACK);
    }

    public void test017LaunchTwice() {
        LogUtils.i(TAG, "call testLaunchTwice");
        String curFolderName = null;
        final String curPath = TestUtils.getTestPath(curFolderName);
        boolean launched = launchWithPath(curPath);
        assertTrue(launched);

        // get a method launchWithStartActivity(String curPath)
        curFolderName = "testLaunchTwice";
        final String curPath01 = TestUtils.getTestPath(curFolderName);
        File loadFile01 = new File(curPath01);
        TestUtils.deleteFile(loadFile01);
        launched = launchWithStartActivity(curPath01);
        assertFalse(launched);

        final String curPath02 = TestUtils.getTestPath(curFolderName);
        File loadFile02 = new File(curPath02);
        TestUtils.createDirectory(loadFile02);
        launched = launchWithStartActivity(curPath02);
        assertTrue(launched);

        mInst.sendCharacterSync(KeyEvent.KEYCODE_BACK);
    }

}
