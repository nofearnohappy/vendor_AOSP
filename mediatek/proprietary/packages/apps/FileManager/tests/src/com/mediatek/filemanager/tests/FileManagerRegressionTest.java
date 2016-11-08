package com.mediatek.filemanager.tests;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import android.app.AlertDialog;
import android.app.Instrumentation;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.media.MediaScannerConnection;
import android.test.TouchUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.mediatek.filemanager.AbsBaseActivity;
import com.mediatek.filemanager.ActivityTestHelper;
import com.mediatek.filemanager.FileInfo;
import com.mediatek.filemanager.FileInfoAdapter;
import com.mediatek.filemanager.FileManagerOperationActivity;
import com.mediatek.filemanager.MenuItemHelper;
import com.mediatek.filemanager.MountPointManager;
import com.mediatek.filemanager.R;
import com.mediatek.filemanager.service.FileManagerService.OperationEventListener;
import com.mediatek.filemanager.tests.utils.TestUtils;
import com.mediatek.filemanager.tests.utils.TestUtils.ClickOptionMenuRunnable;
import com.mediatek.filemanager.tests.utils.TestUtils.EditTextRunnable;
import com.mediatek.filemanager.utils.DrmManager;
import com.mediatek.filemanager.utils.FileUtils;
import com.mediatek.filemanager.utils.LogUtils;
import com.mediatek.filemanager.utils.OptionsUtils;
import com.mediatek.drm.OmaDrmClient;
import com.mediatek.storage.StorageManagerEx;

public class FileManagerRegressionTest extends AbsOperationActivityTest {
    private final static String TAG = "FileManagerRegressionTest";
    private final static Set<String> mSystemDefaultFolders = new HashSet<String>();
    private static final int FIRST_ITEM = 0;
    private static final float CUT_ICON_ALPHA = 0.6f;
    private static String DRM_CONTENT_MIMETYPE = "application/vnd.oma.drm.message";
    private static String DRM_INFO_NOT_FORWARD = "Cannot be forwarded.";
    private static String DRM_INFO_CAN_FORWARD = "Can be forwarded.";
    private static String DRM_INFO_RIGHT_NO_LIMITATION = "No limitation.";
    private static final String DRM_INFO_COUNT_LEFT_3 = "3/3";
    private static String DRM_INFO_TIME_INTERNAL_3_MINS = "";
    private static String DRM_NO_LICENSE = "No available DRM license.";

    public FileManagerRegressionTest() {
        super();
        mSystemDefaultFolders.add("Alarms");
        mSystemDefaultFolders.add("Android");
        mSystemDefaultFolders.add("DCIM");
        mSystemDefaultFolders.add("Download");
        // mSystemDefaultFolders.add("LOST.DIR");
        mSystemDefaultFolders.add("Movies");
        mSystemDefaultFolders.add("Music");
        mSystemDefaultFolders.add("Pictures");
        mSystemDefaultFolders.add("Podcasts");
        // mSystemDefaultFolders.add("Recording");
        mSystemDefaultFolders.add("Ringtones");
    }

    private void initDrmString(FileManagerOperationActivity activity) {
        DRM_INFO_NOT_FORWARD = activity
                .getString(com.mediatek.internal.R.string.drm_can_not_forward);
        DRM_INFO_CAN_FORWARD = activity
                .getString(com.mediatek.internal.R.string.drm_can_forward);
        DRM_INFO_RIGHT_NO_LIMITATION = activity
                .getString(com.mediatek.internal.R.string.drm_no_limitation);
        DRM_NO_LICENSE = activity
                .getString(com.mediatek.internal.R.string.drm_no_license);
    }

    public void test001CheckDefaultFolders() {
        LogUtils.d(TAG, "check default folders.");
        String phoneStoragePath = StorageManagerEx.getInternalStoragePath();
        assertTrue(!TextUtils.isEmpty(phoneStoragePath));

        boolean launched = launchWithPath(phoneStoragePath);
        assertTrue(launched);

        waitFor(1000);
        FileInfoAdapter mAdapter = ActivityTestHelper.getAdapter(mActivity);
        assertTrue(mAdapter != null);
        int count = mAdapter.getCount();
        assertTrue(checkSystemFolders(mAdapter));
    }

    public void test002StartEditModeByLongClick() {
        LogUtils.d(TAG, "start edit mode by long click.");
        openInternalStorage();

        boolean selRes = TestUtils.selectOneItem(this, mActivity, FIRST_ITEM);
        assertTrue(selRes);
    }

    public void test003StartEditModeByOptionsMenu() {
        LogUtils.d(TAG, "start edit mode by options menu.");
        openInternalStorage();
        FileInfoAdapter adapter = ActivityTestHelper.getAdapter(mActivity);
        assertTrue(adapter != null
                && adapter.getMode() == FileInfoAdapter.MODE_NORMAL);
        clickViewBySolo(R.id.change_mode);

        adapter = ActivityTestHelper.getAdapter(mActivity);
        assertTrue(adapter != null
                && adapter.getMode() == FileInfoAdapter.MODE_EDIT);

    }

    public void test004CopyFiles() {
        LogUtils.i(TAG, "call test004CopyFiles()");
        final String curFolderName = "testCopyPasteFiles";
        final String curPath = TestUtils.getRegressionTestPath(curFolderName);
        File loadFile = new File(curPath);
        TestUtils.deleteFile(loadFile);
        TestUtils.createDirectory(loadFile);
        int testFileNum = 5;

        for (int i = 0; i < testFileNum; i++) {
            String fileName = TestUtils.getFileName(curPath, false);
            // fileName = fileName + ".txt";
            fileName = String.format("%s(%d).txt", fileName, i);
            File file = new File(curPath + MountPointManager.SEPARATOR
                    + fileName);
            TestUtils.createFile(file);
        }

        boolean launched = launchWithPath(curPath);
        assertTrue(launched);

        TestUtils.selectAllItems(this, mActivity);
        TestUtils.sleep(2000);
        // clickView(R.id.copy);
        // mSolo = new Solo(getInstrumentation(), mActivity);
        // View view = mSolo.getView(R.id.copy);
        clickViewBySolo(R.id.copy);
        TestUtils.sleep(3000);

        clickViewBySolo(R.id.paste);
        ActivityTestHelper.waitingForService(mActivity);
        TestUtils.sleep(2000);

        for (int i = testFileNum; i < testFileNum * 2; i++) {
            String fileName = TestUtils.getFileName(curPath, false);
            // fileName = fileName + ".txt";
            fileName = String.format("%s(%d).txt", fileName, i);
            File file = new File(curPath + MountPointManager.SEPARATOR
                    + fileName);
            Log.d("REYES", fileName);
            assertTrue(file.exists());
        }

        // restore --- delete test folders or files
        TestUtils.deleteFile(loadFile);
    }

    public void test005CopyFolders() {
        LogUtils.i(TAG, "call testCopyPasteFolders()");
        final String curFolderName = "testCopyPasteFolders";
        final String curPath = TestUtils.getRegressionTestPath(curFolderName);
        File loadFile = new File(curPath);
        TestUtils.deleteFile(loadFile);
        TestUtils.createDirectory(loadFile);
        int testFileNum = 5;

        for (int i = 0; i < testFileNum; i++) {
            String fileName = String.format("%s/testFolder(%d)", curPath, i);
            File file = new File(fileName);
            TestUtils.createDirectory(file);
        }

        boolean launched = launchWithPath(curPath);
        assertTrue(launched);

        TestUtils.selectAllItems(this, mActivity);

        // mSolo = new Solo(mInst, mActivity);

        clickViewBySolo(R.id.copy);
        TestUtils.sleep(2000);

        clickViewBySolo(R.id.paste);
        ActivityTestHelper.waitingForService(mActivity);
        for (int i = testFileNum; i < testFileNum * 2; i++) {
            String fileName = String.format("%s/testFolder(%d)", curPath, i);
            File file = new File(fileName);
            assertTrue(file.exists());
        }

        // restore --- delete test folders or files
        TestUtils.deleteFile(loadFile);
    }

    public void test006CopyFilesToTargetFolder() {
        LogUtils.i(TAG, "call testCopyPasteFolders()");
        final String curFolderName = "testCopyPasteToTargetFolder";
        final String curPath = TestUtils.getRegressionTestPath(curFolderName);
        File loadFile = new File(curPath);
        TestUtils.deleteFile(loadFile);
        TestUtils.createDirectory(loadFile);

        final String targetFolder = "targetFolder";
        final String targetFolderPath = TestUtils
                .getRegressionTestPath(targetFolder);
        File target = new File(targetFolderPath);
        TestUtils.deleteFile(target);
        TestUtils.createDirectory(target);

        int testFilesNum = 40;
        for (int i = 0; i < testFilesNum; i++) {
            String fileName = String.format("%s/testFolder(%d)", curPath, i);
            File file = new File(fileName);
            TestUtils.createDirectory(file);
        }

        boolean launched = launchWithPath(TestUtils.getRegressionTestPath(""));
        assertTrue(launched);
        TestUtils.sleep(1000);
        mSolo.clickOnText(curFolderName);
        ActivityTestHelper.waitingForService(mActivity);
        TestUtils.sleep(1000);

        TestUtils.selectAllItems(this, mActivity);

        clickViewBySolo(R.id.copy);
        // copy to same folder
        TestUtils.sleep(2000);

        clickViewBySolo(R.id.paste);
        ActivityTestHelper.waitingForService(mActivity);

        TestUtils.sleep(1000);
        for (int i = testFilesNum; i < 2 * testFilesNum; i++) {
            String fileName = String.format("%s/testFolder(%d)", curPath, i);
            File file = new File(fileName);
            assertTrue(file.exists());
        }

        TestUtils.sleep(500);
        // clickBackKey(mActivity);
        mInst.sendCharacterSync(KeyEvent.KEYCODE_BACK);
        ActivityTestHelper.waitingForService(mActivity);
        TestUtils.sleep(500);
        mSolo.clickOnText(targetFolder);
        ActivityTestHelper.waitingForService(mActivity);

        FileInfoAdapter adapter = ActivityTestHelper.getAdapter(mActivity);
        assertTrue(adapter != null && adapter.getCount() == 0);
        clickViewBySolo(R.id.paste);
        ActivityTestHelper.waitingForService(mActivity);

        adapter = ActivityTestHelper.getAdapter(mActivity);
        assertTrue(adapter != null && adapter.getCount() == testFilesNum);

        // restore --- delete test folders or files
        TestUtils.deleteFile(loadFile);
        TestUtils.deleteFile(target);
    }

    public void test007CutFiles() {
        LogUtils.i(TAG, "call test006CutFiles()");
        final String curFolderName = "testCutFiles";
        final String curPath = TestUtils.getRegressionTestPath(curFolderName);
        File loadFile = new File(curPath);
        TestUtils.deleteFile(loadFile);
        TestUtils.createDirectory(loadFile);

        int testFileNum = 2;
        for (int i = 0; i < testFileNum; i++) {
            String fileName = TestUtils.getFileName(curPath, false);
            // fileName = fileName + ".txt";
            fileName = String.format("%s(%d).txt", fileName, i);
            File file = new File(curPath + MountPointManager.SEPARATOR
                    + fileName);
            TestUtils.createFile(file);
        }

        boolean launched = launchWithPath(curPath);
        assertTrue(launched);

        TestUtils.selectAllItems(this, mActivity);
        clickViewBySolo(R.id.cut);
        TestUtils.sleep(1000);

        for (int i = 0; i < testFileNum; i++) {
            View listItemView = TestUtils.getItemView(mActivity, i);
            ImageView icon = (ImageView) listItemView
                    .findViewById(R.id.edit_adapter_img);
            assertTrue(icon.getAlpha() == CUT_ICON_ALPHA);
        }

        // restore --- delete test folders or files
        TestUtils.deleteFile(loadFile);
    }

    public void test008CutFolders() {
        LogUtils.i(TAG, "call test006CutFolders()");
        final String curFolderName = "testCutFolders";
        final String curPath = TestUtils.getRegressionTestPath(curFolderName);
        File loadFile = new File(curPath);
        TestUtils.deleteFile(loadFile);
        TestUtils.createDirectory(loadFile);

        int testFileNum = 2;
        for (int i = 0; i < testFileNum; i++) {
            String fileName = TestUtils.getFileName(curPath, false);
            // fileName = fileName + ".txt";
            fileName = String.format("%s(%d)", fileName, i);
            File file = new File(curPath + MountPointManager.SEPARATOR
                    + fileName);
            TestUtils.createDirectory(file);
        }

        boolean launched = launchWithPath(curPath);
        assertTrue(launched);

        TestUtils.selectAllItems(this, mActivity);
        clickViewBySolo(R.id.cut);
        TestUtils.sleep(500);

        for (int i = 0; i < testFileNum; i++) {
            View listItemView = TestUtils.getItemView(mActivity, i);
            ImageView icon = (ImageView) listItemView
                    .findViewById(R.id.edit_adapter_img);
            assertTrue(icon.getAlpha() == CUT_ICON_ALPHA);
        }

        // restore --- delete test folders or files
        TestUtils.deleteFile(loadFile);
    }

    public void test009CutFilesToTargetFolder() {
        LogUtils.i(TAG, "call test008CutFilesToTargetFolder()");
        final String curFolderName = "testCutFoldersToTarget";
        final String curPath = TestUtils.getRegressionTestPath(curFolderName);
        File loadFile = new File(curPath);
        TestUtils.deleteFile(loadFile);
        TestUtils.createDirectory(loadFile);

        int testFileNum = 10;
        for (int i = 0; i < testFileNum; i++) {
            String fileName = TestUtils.getFileName(curPath, false);
            // fileName = fileName + ".txt";
            fileName = String.format("%s(%d).txt", fileName, i);
            File file = new File(curPath + MountPointManager.SEPARATOR
                    + fileName);
            TestUtils.createDirectory(file);
        }

        File subFolder = new File(curPath + MountPointManager.SEPARATOR
                + "childTargetFolder");
        TestUtils.deleteFile(subFolder);
        TestUtils.createDirectory(subFolder);
        assertTrue(loadFile.listFiles().length == (testFileNum + 1));

        boolean launched = launchWithPath(curPath);
        assertTrue(launched);
        // mSolo = new Solo(mInst, mActivity);

        TestUtils.selectAllItems(this, mActivity);
        clickViewBySolo(R.id.cut);
        TestUtils.sleep(500);

        mSolo.clickOnText("childTargetFolder");
        ActivityTestHelper.waitingForService(mActivity);
        TestUtils.sleep(500);

        clickViewBySolo(R.id.paste);
        ActivityTestHelper.waitingForService(mActivity);
        TestUtils.sleep(500);

        FileInfoAdapter adapter = ActivityTestHelper.getAdapter(mActivity);
        assertTrue(adapter != null && adapter.getCount() == testFileNum);

        assertTrue(loadFile.listFiles().length == (0 + 1));
        // restore --- delete test folders or files
        TestUtils.deleteFile(loadFile);
        TestUtils.deleteFile(subFolder);
    }

    public void test010DeleteFiles() {
        LogUtils.i(TAG, "call test009DeleteFiles()");
        final String curFolderName = "testDeleteFiles";
        final String curPath = TestUtils.getRegressionTestPath(curFolderName);
        File loadFile = new File(curPath);
        TestUtils.deleteFile(loadFile);
        TestUtils.createDirectory(loadFile);

        int createFilesNum = 4;
        for (int i = 0; i < createFilesNum; i++) {
            String fileName = TestUtils.getFileName(curPath, false);
            // fileName = fileName + ".txt";
            fileName = String.format("%s(%d).txt", fileName, i);
            File file = new File(curPath + MountPointManager.SEPARATOR
                    + fileName);
            TestUtils.createFile(file);
        }

        boolean launched = launchWithPath(curPath);
        assertTrue(launched);
        // mSolo = new Solo(mInst, mActivity);
        FileInfoAdapter adapter = ActivityTestHelper.getAdapter(mActivity);
        assertTrue(adapter != null && adapter.getCount() == createFilesNum);

        TestUtils.selectAllItems(this, mActivity);
        clickViewBySolo(R.id.delete);
        TestUtils.sleep(2000);

        AlertDialog deleteConfirmDialog = (AlertDialog) ActivityTestHelper
                .getDialog(mActivity,
                        FileManagerOperationActivity.DELETE_DIALOG_TAG);

        // click cancel button to cancel delete
        Button cancelButton = deleteConfirmDialog
                .getButton(DialogInterface.BUTTON_NEGATIVE);
        // TestUtils.clickViewWithIMEHiden(this, mActivity, cancelButton);
        // TouchUtils.clickView(cancelButton);
        // TouchUtils.clickView(this,cancelButton);
        mSolo.clickOnView(cancelButton);
        TestUtils.sleep(1000);

        adapter = ActivityTestHelper.getAdapter(mActivity);
        assertTrue(adapter.getCount() == createFilesNum);

        // click ok button to delete all files
        TestUtils.selectAllItems(this, mActivity);
        clickViewBySolo(R.id.delete);
        TestUtils.sleep(2000);

        deleteConfirmDialog = (AlertDialog) ActivityTestHelper.getDialog(
                mActivity, FileManagerOperationActivity.DELETE_DIALOG_TAG);

        Button okButton = deleteConfirmDialog
                .getButton(DialogInterface.BUTTON_POSITIVE);
        // TouchUtils.clickView(this,okButton);
        mSolo.clickOnView(okButton);
        ActivityTestHelper.waitingForService(mActivity);
        TestUtils.sleep(2000);

        adapter = ActivityTestHelper.getAdapter(mActivity);
        assertTrue(adapter.getCount() == 0);
        // restore
        TestUtils.deleteFile(loadFile);
    }

    public void test011DeleteFolders() {
        LogUtils.i(TAG, "call test010DeleteFolders()");
        final String curFolderName = "testDeleteFolders";
        final String curPath = TestUtils.getRegressionTestPath(curFolderName);
        File loadFile = new File(curPath);
        TestUtils.deleteFile(loadFile);
        TestUtils.createDirectory(loadFile);

        int createFilesNum = 4;
        for (int i = 0; i < createFilesNum; i++) {
            String fileName = TestUtils.getFileName(curPath, false);
            // fileName = fileName + ".txt";
            fileName = String.format("%s(%d)", fileName, i);
            File file = new File(curPath + MountPointManager.SEPARATOR
                    + fileName);
            TestUtils.createDirectory(file);
        }

        boolean launched = launchWithPath(curPath);
        assertTrue(launched);
        // mSolo = new Solo(mInst, mActivity);
        FileInfoAdapter adapter = ActivityTestHelper.getAdapter(mActivity);
        assertTrue(adapter != null && adapter.getCount() == createFilesNum);

        TestUtils.selectAllItems(this, mActivity);
        clickViewBySolo(R.id.delete);
        TestUtils.sleep(1000);

        AlertDialog deleteConfirmDialog = (AlertDialog) ActivityTestHelper
                .getDialog(mActivity,
                        FileManagerOperationActivity.DELETE_DIALOG_TAG);

        // click cancel button to cancel delete
        Button cancelButton = deleteConfirmDialog
                .getButton(DialogInterface.BUTTON_NEGATIVE);
        // TestUtils.clickViewWithIMEHiden(this, mActivity, cancelButton);
        // TouchUtils.clickView(cancelButton);
        // TouchUtils.clickView(this,cancelButton);
        mSolo.clickOnView(cancelButton);
        TestUtils.sleep(500);

        adapter = ActivityTestHelper.getAdapter(mActivity);
        assertTrue(adapter.getCount() == createFilesNum);

        // click ok button to delete all files
        TestUtils.selectAllItems(this, mActivity);
        clickViewBySolo(R.id.delete);
        TestUtils.sleep(500);

        deleteConfirmDialog = (AlertDialog) ActivityTestHelper.getDialog(
                mActivity, FileManagerOperationActivity.DELETE_DIALOG_TAG);

        Button okButton = deleteConfirmDialog
                .getButton(DialogInterface.BUTTON_POSITIVE);
        // TouchUtils.clickView(this,okButton);
        mSolo.clickOnView(okButton);
        ActivityTestHelper.waitingForService(mActivity);
        TestUtils.sleep(500);

        adapter = ActivityTestHelper.getAdapter(mActivity);
        assertTrue(adapter.getCount() == 0);
        // restore
        TestUtils.deleteFile(loadFile);
    }

    public void test012RenameFile() {
        LogUtils.i(TAG, "call testRenameFileExtension");

        final String curFolderName = "testRenameFile";
        final String curPath = TestUtils.getRegressionTestPath(curFolderName);
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

        // cancel rename
        String newFileName = "newFileName" + ".txt";
        renameFile(fileInfo, newFileName, false);
        assertTrue(file.exists());
        File newFile = new File(curPath + MountPointManager.SEPARATOR
                + newFileName);
        assertFalse(newFile.exists());

        // confirm rename one file
        renameFile(fileInfo, newFileName, true);
        assertFalse(file.exists());
        assertTrue(newFile.exists());

        // delete created file
        TestUtils.deleteFile(loadFile);
    }

    public void test013RenameFolder() {
        LogUtils.i(TAG, "call testRenameFileExtension");

        final String curFolderName = "testRenameFolder";
        final String curPath = TestUtils.getRegressionTestPath(curFolderName);
        File loadFile = new File(curPath);
        TestUtils.deleteFile(loadFile);
        TestUtils.createDirectory(loadFile);

        String fileName = TestUtils.getFileName(curPath, false);
        File file = new File(curPath + MountPointManager.SEPARATOR + fileName);
        FileInfo fileInfo = new FileInfo(file);
        TestUtils.createDirectory(file);

        boolean launched = launchWithPath(curPath);
        assertTrue(launched);

        // cancel rename
        String newFileName = "RenameToFolder";
        renameFile(fileInfo, newFileName, false);
        assertTrue(file.exists());
        File newFile = new File(curPath + MountPointManager.SEPARATOR
                + newFileName);
        assertFalse(newFile.exists());

        // confirm rename one file
        renameFile(fileInfo, newFileName, true);
        assertFalse(file.exists());
        assertTrue(newFile.exists());

        // delete created file
        TestUtils.deleteFile(loadFile);
    }

    public void test014DetailInfoOfFile() {
        LogUtils.i(TAG, "call testDetailOfFile");

        String curFolderName = "testFileDetailInfo";
        final String curPath = TestUtils.getRegressionTestPath(curFolderName);
        File loadFile = new File(curPath);
        TestUtils.deleteFile(loadFile);
        TestUtils.createDirectory(loadFile);

        File file = new File(curPath + MountPointManager.SEPARATOR
                + "minutes.mp3");
        LogUtils.e(TAG, file.getAbsolutePath());
        TestUtils.createFile(file);

        TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                com.mediatek.filemanager.tests.R.raw.minutes, file);

        boolean launched = launchWithPath(curPath);
        assertTrue(launched);

        FileInfo destFile = new FileInfo(file);

        String detailInfo = getDetailInfo(destFile);
        LogUtils.d(TAG, "detailInfo: " + detailInfo);

        StringBuilder exspectValue = new StringBuilder();
        exspectValue.append(mActivity.getString(R.string.name)).append(": ")
                .append(destFile.getFileName()).append("\n").toString();
        exspectValue.append(mActivity.getString(R.string.size)).append(": ")
                .append(FileUtils.sizeToString(destFile.getFileSize()))
                .append(" \n").toString();
        long time = destFile.getFileLastModifiedTime();

        exspectValue.append(mActivity.getString(R.string.modified_time))
                .append(": ")
                .append(DateFormat.getDateInstance().format(new Date(time)))
                .append("\n").toString();

        if (file.canRead()) {
            exspectValue.append(mActivity.getString(R.string.readable) + ": ")
                    .append(mActivity.getString(R.string.yes));
        } else {
            exspectValue.append(mActivity.getString(R.string.readable) + ": ")
                    .append(mActivity.getString(R.string.no));
        }
        exspectValue.append("\n");

        if (file.canWrite()) {
            exspectValue.append(mActivity.getString(R.string.writable) + ": ")
                    .append(mActivity.getString(R.string.yes));
        } else {
            exspectValue.append(mActivity.getString(R.string.writable) + ": ")
                    .append(mActivity.getString(R.string.no));
        }
        exspectValue.append("\n");

        if (file.canExecute()) {
            exspectValue
                    .append(mActivity.getString(R.string.executable) + ": ")
                    .append(mActivity.getString(R.string.yes));
        } else {
            exspectValue
                    .append(mActivity.getString(R.string.executable) + ": ")
                    .append(mActivity.getString(R.string.no));
        }

        LogUtils.d(TAG, "detailInfo: " + exspectValue.toString());
        assertTrue(detailInfo.equals(exspectValue.toString()));

        // restore
        TestUtils.deleteFile(loadFile);
    }

    public void test015DetailInfoOfFolder() {
        LogUtils.i(TAG, "call testDetailofFolder");

        String curFolderName = "testDetailInfoOfFolder";
        final String curPath = TestUtils.getTestPath(curFolderName);
        File loadFile = new File(curPath);
        TestUtils.deleteFile(loadFile);
        TestUtils.createDirectory(loadFile);

        String fileName = TestUtils.getFileName(curPath, false);
        File folderFile = new File(curPath + MountPointManager.SEPARATOR
                + fileName);
        TestUtils.createDirectory(folderFile);

        FileInfo destFile = new FileInfo(new File(curPath
                + MountPointManager.SEPARATOR + fileName));

        boolean launched = launchWithPath(curPath);
        assertTrue(launched);

        String detailInfo = getDetailInfo(destFile);

        LogUtils.d(TAG, "detailInfo: " + detailInfo);
        StringBuilder exspectValue = new StringBuilder();
        exspectValue.append(mActivity.getString(R.string.name)).append(": ")
                .append(destFile.getFileName()).append("\n").toString();
        exspectValue
                .append(mActivity.getString(R.string.size))
                .append(": ")
                .append(FileUtils.sizeToString(destFile.getFileSize() > 0 ? destFile
                        .getFileSize() : 0)).append(" \n").toString();
        long time = destFile.getFileLastModifiedTime();

        exspectValue.append(mActivity.getString(R.string.modified_time))
                .append(": ")
                .append(DateFormat.getDateInstance().format(new Date(time)))
                .append("\n").toString();

        if (folderFile.canRead()) {
            exspectValue.append(mActivity.getString(R.string.readable) + ": ")
                    .append(mActivity.getString(R.string.yes));
        } else {
            exspectValue.append(mActivity.getString(R.string.readable) + ": ")
                    .append(mActivity.getString(R.string.no));
        }
        exspectValue.append("\n");

        if (folderFile.canWrite()) {
            exspectValue.append(mActivity.getString(R.string.writable) + ": ")
                    .append(mActivity.getString(R.string.yes));
        } else {
            exspectValue.append(mActivity.getString(R.string.writable) + ": ")
                    .append(mActivity.getString(R.string.no));
        }
        exspectValue.append("\n");

        if (folderFile.canExecute()) {
            exspectValue
                    .append(mActivity.getString(R.string.executable) + ": ")
                    .append(mActivity.getString(R.string.yes));
        } else {
            exspectValue
                    .append(mActivity.getString(R.string.executable) + ": ")
                    .append(mActivity.getString(R.string.no));
        }

        LogUtils.d(TAG, "detailInfo: " + exspectValue.toString());
        assertTrue(detailInfo.equals(exspectValue.toString()));
    }

    public void test016CreateFolder() {
        LogUtils.i(TAG, "test0014CreateFolder");
        final String curFolderName = "testCreateFolder";
        final String curPath = TestUtils.getRegressionTestPath(curFolderName);
        File loadFile = new File(curPath);
        TestUtils.deleteFile(loadFile);
        TestUtils.createDirectory(loadFile);

        boolean launched = launchWithPath(curPath);
        assertTrue(launched);
        // mSolo = new Solo(mInst, mActivity);
        TestUtils.sleep(2000);

        // Get a file name that the file is not exists
        String folderName = TestUtils.getFileName(curPath, false);

        // 1. test create a new folder.(The folder is not exists)
        createFolder(folderName);
        File newFolderFile = new File(curPath + MountPointManager.SEPARATOR
                + folderName);
        assertTrue(newFolderFile.exists());

        // 2. test create a folder. (The folder is exists)
        createFolder(folderName);

        // 3. test create a folder with an invalid name
        String invalidName = "?%^&$!@~";
        createFolder(invalidName);
        File invalidFolderFile = new File(curPath + MountPointManager.SEPARATOR
                + invalidName);
        assertFalse(invalidFolderFile.exists());

        // 4. test create a folder with a name too long
        StringBuilder sb = new StringBuilder();
        while (OperationEventListener.ERROR_CODE_NAME_TOO_LONG != FileUtils
                .checkFileName(sb.toString())) {
            sb.append("A");
        }
        String tooLongName = sb.toString();
        createFolder(tooLongName);
        File tooLongFolderFile = new File(curPath + MountPointManager.SEPARATOR
                + tooLongName);
        assertFalse(tooLongFolderFile.exists());

        // 5. test create hidden a hidden folder
        String hiddenFolderName = TestUtils.getFileName(curPath, true);
        createFolder(hiddenFolderName);
        File newHiddenFolderFile = new File(curPath
                + MountPointManager.SEPARATOR + hiddenFolderName);
        assertTrue(newHiddenFolderFile.exists());

        // delete the create folders
        TestUtils.deleteFile(newFolderFile);
        TestUtils.deleteFile(newHiddenFolderFile);
        TestUtils.deleteFile(loadFile);
    }

    public void test017CheckFL_Audio_ProtectionInfo() {
        if (OptionsUtils.isMtkDrmApp()) {

            final String testFolderName = "testImageProtectionInfo";
            final String drmExtension = "." + DrmManager.EXT_DRM_CONTENT;
            final String drmFileName = "drmfl.dcf";

            final OmaDrmClient drmClient = new OmaDrmClient(mActivity);

            String path = TestUtils.getRegressionTestPath(testFolderName);
            File testFolder = new File(path);
            TestUtils.deleteFile(testFolder);
            TestUtils.createDirectory(testFolder);

            File drmFile = new File(path + MountPointManager.SEPARATOR
                    + drmFileName);
            FileInfo drmFileInfo = new FileInfo(drmFile);
            TestUtils.createFile(drmFile);
            TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                    com.mediatek.filemanager.tests.R.raw.faaac, drmFile);

            int installRes = drmClient.installDrmMsg(drmFile.getAbsolutePath());
            assertTrue(installRes == OmaDrmClient.ERROR_NONE);
            // scanDrmFile(drmFile.getAbsolutePath(),DRM_CONTENT_MIMETYPE);
            TestUtils.sleep(2000);

            boolean launched = launchWithPath(path);
            assertTrue(launched);
            initDrmString(mActivity);

            // mSolo = new Solo(getInstrumentation(), mActivity);

            assertTrue(TestUtils.selectOneItem(this, mActivity, 0));
            clickMoreMenu(R.id.protection_info);

            // TestUtils.sleep(3000);
            TextView fileName = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_file_name_value);
            assertTrue(fileName != null);
            assertTrue(fileName.getText().equals("drmfl"));

            TextView canForward = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_protection_status_value);
            assertTrue(canForward != null);
            assertTrue(canForward.getText().equals(DRM_INFO_NOT_FORWARD));

            TextView rightBegin = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_begin_value);
            assertTrue(rightBegin != null);
            LogUtils.d("REYES", "begin: " + rightBegin.getText());
            assertTrue(rightBegin.getText()
                    .equals(DRM_INFO_RIGHT_NO_LIMITATION));

            TextView rightEnd = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_end_value);
            assertTrue(rightEnd != null);
            assertTrue(rightEnd.getText().equals(DRM_INFO_RIGHT_NO_LIMITATION));

            TextView rightCount = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_use_left_value);
            assertTrue(rightCount != null);
            assertTrue(rightCount.getText()
                    .equals(DRM_INFO_RIGHT_NO_LIMITATION));

            LogUtils.d("REYES", fileName.getText() + " " + canForward.getText()
                    + " " + rightBegin.getText() + " " + rightEnd.getText()
                    + " " + rightCount.getText());
            // restore --- delete created files
            // TestUtils.deleteFile(drmFile);
            drmClient.removeAllRights();
            drmClient.release();
            TestUtils.deleteFile(testFolder);
        }
    }

    public void test018ForbidFL_Audio_Share() {
        if (OptionsUtils.isMtkDrmApp()) {
            final String testFolderName = "testImageShare";
            final String drmExtension = "." + DrmManager.EXT_DRM_CONTENT;
            final String drmFileName = "drmfl.dcf";

            final OmaDrmClient drmClient = new OmaDrmClient(mActivity);

            String path = TestUtils.getRegressionTestPath(testFolderName);
            File testFolder = new File(path);
            TestUtils.deleteFile(testFolder);
            TestUtils.createDirectory(testFolder);

            File drmFile = new File(path + MountPointManager.SEPARATOR
                    + drmFileName);
            FileInfo drmFileInfo = new FileInfo(drmFile);
            TestUtils.createFile(drmFile);
            TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                    com.mediatek.filemanager.tests.R.raw.faaac, drmFile);

            int installRes = drmClient.installDrmMsg(drmFile.getAbsolutePath());
            assertTrue(installRes == OmaDrmClient.ERROR_NONE);
            // scanDrmFile(drmFile.getAbsolutePath(),DRM_CONTENT_MIMETYPE);
            TestUtils.sleep(2000);

            boolean launched = launchWithPath(path);
            assertTrue(launched);
            assertTrue(TestUtils.selectOneItem(this, mActivity, 0));

            View shareMenu = mActivity.findViewById(R.id.share);
            assertTrue(shareMenu != null && !shareMenu.isEnabled());
            // restore --- delete created files
            drmClient.removeAllRights();
            drmClient.release();
            TestUtils.deleteFile(testFolder);
        }
    }

    public void test019ForbidFL_Audio_CopyPaste() {
        if (OptionsUtils.isMtkDrmApp()) {
            final String testFolderName = "testImageCopyPaste";
            final String drmExtension = "." + DrmManager.EXT_DRM_CONTENT;
            final String drmFileName = "drmfl.dcf";

            final OmaDrmClient drmClient = new OmaDrmClient(mActivity);

            String path = TestUtils.getRegressionTestPath(testFolderName);
            File testFolder = new File(path);
            TestUtils.deleteFile(testFolder);
            TestUtils.createDirectory(testFolder);

            File drmFile = new File(path + MountPointManager.SEPARATOR
                    + drmFileName);
            FileInfo drmFileInfo = new FileInfo(drmFile);
            TestUtils.createFile(drmFile);
            TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                    com.mediatek.filemanager.tests.R.raw.faaac, drmFile);

            int installRes = drmClient.installDrmMsg(drmFile.getAbsolutePath());
            assertTrue(installRes == OmaDrmClient.ERROR_NONE);
            // scanDrmFile(drmFile.getAbsolutePath(),DRM_CONTENT_MIMETYPE);
            TestUtils.sleep(2000);

            boolean launched = launchWithPath(path);
            assertTrue(launched);
            // mSolo = new Solo(mInst, mActivity);

            assertTrue(TestUtils.selectOneItem(this, mActivity, 0));

            // make sure there is only one file.
            FileInfoAdapter adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            View shareMenu = mActivity.findViewById(R.id.copy);
            assertTrue(shareMenu != null && shareMenu.isEnabled());
            mSolo.clickOnView(shareMenu);
            TestUtils.sleep(1000);

            View pasteMenu = mActivity.findViewById(R.id.paste);
            assertTrue(pasteMenu != null && pasteMenu.isEnabled());
            mSolo.clickOnView(pasteMenu);
            TestUtils.sleep(1000);

            // make sure that copy paste FL drm file failed.
            adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            // restore --- delete created files
            // TestUtils.deleteFile(drmFile);
            drmClient.removeAllRights();
            drmClient.release();
            TestUtils.deleteFile(testFolder);
        }
    }

    public void test020ForbidFLSD_Audio_Share() {
        if (OptionsUtils.isMtkDrmApp()) {
            final String testFolderName = "testAudioProtectionInfo";
            final String drmExtension = "." + DrmManager.EXT_DRM_CONTENT;
            final String drmFileName = "drmflsd.dcf";

            final OmaDrmClient drmClient = new OmaDrmClient(mActivity);

            String path = TestUtils.getRegressionTestPath(testFolderName);
            File testFolder = new File(path);
            TestUtils.deleteFile(testFolder);
            TestUtils.createDirectory(testFolder);

            File drmFile = new File(path + MountPointManager.SEPARATOR
                    + drmFileName);
            FileInfo drmFileInfo = new FileInfo(drmFile);
            TestUtils.createFile(drmFile);
            TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                    com.mediatek.filemanager.tests.R.raw.flsdimage, drmFile);

            int installRes = drmClient.installDrmMsg(drmFile.getAbsolutePath());
            assertTrue(installRes == OmaDrmClient.ERROR_NONE);
            // scanDrmFile(drmFile.getAbsolutePath(),DRM_CONTENT_MIMETYPE);
            TestUtils.sleep(2000);

            boolean launched = launchWithPath(path);
            assertTrue(launched);

            assertTrue(TestUtils.selectOneItem(this, mActivity, FIRST_ITEM));

            // make sure there is only one file.
            FileInfoAdapter adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            View shareMenu = mActivity.findViewById(R.id.share);
            assertTrue(shareMenu != null && !shareMenu.isEnabled());

            // restore --- delete created files
            drmClient.removeAllRights();
            drmClient.release();
            TestUtils.deleteFile(testFolder);
        }
    }

    public void test021ForbidFLSD_Audio_CopyPaste() {
        if (OptionsUtils.isMtkDrmApp()) {
            final String testFolderName = "testAudioCopyPaste";
            final String drmExtension = "." + DrmManager.EXT_DRM_CONTENT;
            final String drmFileName = "drmflsd.dcf";

            final OmaDrmClient drmClient = new OmaDrmClient(mActivity);

            String path = TestUtils.getRegressionTestPath(testFolderName);
            File testFolder = new File(path);
            TestUtils.deleteFile(testFolder);
            TestUtils.createDirectory(testFolder);

            File drmFile = new File(path + MountPointManager.SEPARATOR
                    + drmFileName);
            FileInfo drmFileInfo = new FileInfo(drmFile);
            TestUtils.createFile(drmFile);
            TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                    com.mediatek.filemanager.tests.R.raw.flsdimage, drmFile);

            int installRes = drmClient.installDrmMsg(drmFile.getAbsolutePath());
            assertTrue(installRes == OmaDrmClient.ERROR_NONE);
            // scanDrmFile(drmFile.getAbsolutePath(),DRM_CONTENT_MIMETYPE);
            TestUtils.sleep(2000);

            boolean launched = launchWithPath(path);
            assertTrue(launched);
            // mSolo = new Solo(mInst, mActivity);

            assertTrue(TestUtils.selectOneItem(this, mActivity, 0));
            TestUtils.sleep(2000);

            // make sure there is only one file.
            FileInfoAdapter adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            View shareMenu = mActivity.findViewById(R.id.copy);
            assertTrue(shareMenu != null && shareMenu.isEnabled());
            mSolo.clickOnView(shareMenu);
            TestUtils.sleep(1000);

            View pasteMenu = mActivity.findViewById(R.id.paste);
            assertTrue(pasteMenu != null && pasteMenu.isEnabled());
            mSolo.clickOnView(pasteMenu);
            TestUtils.sleep(1000);

            // make sure that copy paste FL drm file failed.
            adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            // restore --- delete created files
            // TestUtils.deleteFile(drmFile);
            drmClient.removeAllRights();
            drmClient.release();
            TestUtils.deleteFile(testFolder);
        }
    }

    public void test022CheckCD_Audio_ProtectionInfo() {
        if (OptionsUtils.isMtkDrmApp()) {
            final String testFolderName = "testAudioProtectionInfo";
            final String drmExtension = "." + DrmManager.EXT_DRM_CONTENT;
            final String drmFileName = "drmcd.dcf";

            final OmaDrmClient drmClient = new OmaDrmClient(mActivity);

            String path = TestUtils.getRegressionTestPath(testFolderName);
            File testFolder = new File(path);
            TestUtils.deleteFile(testFolder);
            TestUtils.createDirectory(testFolder);

            File drmFile = new File(path + MountPointManager.SEPARATOR
                    + drmFileName);
            FileInfo drmFileInfo = new FileInfo(drmFile);
            TestUtils.createFile(drmFile);
            TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                    com.mediatek.filemanager.tests.R.raw.ccaamrnb, drmFile);

            int installRes = drmClient.installDrmMsg(drmFile.getAbsolutePath());
            assertTrue(installRes == OmaDrmClient.ERROR_NONE);
            // scanDrmFile(drmFile.getAbsolutePath(),DRM_CONTENT_MIMETYPE);
            TestUtils.sleep(2000);

            boolean launched = launchWithPath(path);
            assertTrue(launched);
            initDrmString(mActivity);

            // mSolo = new Solo(getInstrumentation(), mActivity);

            // make sure there is only one file.
            FileInfoAdapter adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            assertTrue(TestUtils.selectOneItem(this, mActivity, 0));
            TestUtils.sleep(1000);

            clickMoreMenu(R.id.protection_info);

            TextView fileName = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_file_name_value);
            assertTrue(fileName != null);
            assertTrue(fileName.getText().equals("drmcd"));

            TextView canForward = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_protection_status_value);
            assertTrue(canForward != null);
            assertTrue(canForward.getText().equals(DRM_INFO_NOT_FORWARD));

            TextView rightBegin = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_begin_value);
            assertTrue(rightBegin != null);
            assertTrue(rightBegin.getText()
                    .equals(DRM_INFO_RIGHT_NO_LIMITATION));

            TextView rightEnd = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_end_value);
            assertTrue(rightEnd != null);
            assertTrue(rightEnd.getText().equals(DRM_INFO_RIGHT_NO_LIMITATION));

            TextView rightCount = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_use_left_value);
            assertTrue(rightCount != null);
            String viewContent = rightCount.getText().toString();
            LogUtils.d("EYES", viewContent + "  dd" + DRM_INFO_COUNT_LEFT_3);
            assertTrue(viewContent != null
                    && viewContent.contains(DRM_INFO_COUNT_LEFT_3));

            LogUtils.d("REYES", fileName.getText() + " " + canForward.getText()
                    + " " + rightBegin.getText() + " " + rightEnd.getText()
                    + " " + rightCount.getText());
            LogUtils.d("EYES", rightCount.getText().toString());
            // restore --- delete created files
             TestUtils.deleteFile(testFolder); drmClient.removeAllRights();
             drmClient.removeAllRights();
             drmClient.release();
        }
    }

    public void test023CheckDrmFL_Image_ProtectionInfo() {
        if (OptionsUtils.isMtkDrmApp()) {
            final String testFolderName = "testImageProtectionInfo";
            final String drmExtension = "." + DrmManager.EXT_DRM_CONTENT;
            final String drmFileName = "drmfl.dcf";

            final OmaDrmClient drmClient = new OmaDrmClient(mActivity);

            String path = TestUtils.getRegressionTestPath(testFolderName);
            File testFolder = new File(path);
            TestUtils.deleteFile(testFolder);
            TestUtils.createDirectory(testFolder);

            File drmFile = new File(path + MountPointManager.SEPARATOR
                    + drmFileName);
            FileInfo drmFileInfo = new FileInfo(drmFile);
            TestUtils.createFile(drmFile);
            TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                    com.mediatek.filemanager.tests.R.raw.fijpg, drmFile);

            int installRes = drmClient.installDrmMsg(drmFile.getAbsolutePath());
            assertTrue(installRes == OmaDrmClient.ERROR_NONE);
            TestUtils.sleep(2000);

            boolean launched = launchWithPath(path);
            assertTrue(launched);
            initDrmString(mActivity);

            assertTrue(TestUtils.selectOneItem(this, mActivity, 0));
            clickMoreMenu(R.id.protection_info);

            TextView fileName = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_file_name_value);
            assertTrue(fileName != null);
            assertTrue(fileName.getText().equals("drmfl"));

            TextView canForward = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_protection_status_value);
            assertTrue(canForward != null);
            LogUtils.d("EYES", "canForward " + canForward.getText() + " constant: " + DRM_INFO_NOT_FORWARD);
            assertTrue(canForward.getText().equals(DRM_INFO_NOT_FORWARD));

            TextView rightBegin = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_begin_value);
            assertTrue(rightBegin != null);
            assertTrue(rightBegin.getText()
                    .equals(DRM_INFO_RIGHT_NO_LIMITATION));

            TextView rightEnd = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_end_value);
            assertTrue(rightEnd != null);
            assertTrue(rightEnd.getText().equals(DRM_INFO_RIGHT_NO_LIMITATION));

            TextView rightCount = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_use_left_value);
            assertTrue(rightCount != null);
            assertTrue(rightCount.getText()
                    .equals(DRM_INFO_RIGHT_NO_LIMITATION));

            // restore --- delete created files
            drmClient.removeAllRights();
            drmClient.release();
            TestUtils.deleteFile(testFolder);
        }
    }

    public void test024ForbidFL_Image_Share() {
        if (OptionsUtils.isMtkDrmApp()) {
            final String testFolderName = "testImageShare";
            final String drmExtension = "." + DrmManager.EXT_DRM_CONTENT;
            final String drmFileName = "drmfl.dcf";

            final OmaDrmClient drmClient = new OmaDrmClient(mActivity);

            String path = TestUtils.getRegressionTestPath(testFolderName);
            File testFolder = new File(path);
            TestUtils.deleteFile(testFolder);
            TestUtils.createDirectory(testFolder);

            File drmFile = new File(path + MountPointManager.SEPARATOR
                    + drmFileName);
            FileInfo drmFileInfo = new FileInfo(drmFile);
            TestUtils.createFile(drmFile);
            TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                    com.mediatek.filemanager.tests.R.raw.fijpg, drmFile);

            int installRes = drmClient.installDrmMsg(drmFile.getAbsolutePath());
            assertTrue(installRes == OmaDrmClient.ERROR_NONE);
            // scanDrmFile(drmFile.getAbsolutePath(),DRM_CONTENT_MIMETYPE);
            TestUtils.sleep(2000);

            boolean launched = launchWithPath(path);
            assertTrue(launched);
            assertTrue(TestUtils.selectOneItem(this, mActivity, 0));

            View shareMenu = mActivity.findViewById(R.id.share);
            assertTrue(shareMenu != null && !shareMenu.isEnabled());
            // restore --- delete created files
            // TestUtils.deleteFile(drmFile);
            TestUtils.deleteFile(testFolder);
            drmClient.removeAllRights();
            drmClient.release();
        }
    }

    public void test025ForbidFL_Image_CopyPaste() {
        if (OptionsUtils.isMtkDrmApp()) {
            final String testFolderName = "testImageCopyPaste";
            final String drmExtension = "." + DrmManager.EXT_DRM_CONTENT;
            final String drmFileName = "drmfl.dcf";

            final OmaDrmClient drmClient = new OmaDrmClient(mActivity);

            String path = TestUtils.getRegressionTestPath(testFolderName);
            File testFolder = new File(path);
            TestUtils.deleteFile(testFolder);
            TestUtils.createDirectory(testFolder);

            File drmFile = new File(path + MountPointManager.SEPARATOR
                    + drmFileName);
            FileInfo drmFileInfo = new FileInfo(drmFile);
            TestUtils.createFile(drmFile);
            TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                    com.mediatek.filemanager.tests.R.raw.fijpg, drmFile);

            int installRes = drmClient.installDrmMsg(drmFile.getAbsolutePath());
            assertTrue(installRes == OmaDrmClient.ERROR_NONE);
            // scanDrmFile(drmFile.getAbsolutePath(),DRM_CONTENT_MIMETYPE);
            TestUtils.sleep(2000);

            boolean launched = launchWithPath(path);
            assertTrue(launched);

            assertTrue(TestUtils.selectOneItem(this, mActivity, 0));

            // make sure there is only one file.
            FileInfoAdapter adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            View shareMenu = mActivity.findViewById(R.id.copy);
            assertTrue(shareMenu != null && shareMenu.isEnabled());
            clickViewBySolo(R.id.copy);
            TestUtils.sleep(4000);
            View pasteMenu = mActivity.findViewById(R.id.paste);
            assertTrue(pasteMenu != null && pasteMenu.isEnabled());
            clickViewBySolo(R.id.paste);
            TestUtils.sleep(4000);

            // make sure that copy paste FL drm file failed.
            adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            // restore --- delete created files
            // TestUtils.deleteFile(drmFile);
            TestUtils.deleteFile(testFolder);
            drmClient.removeAllRights();
            drmClient.release();
        }
    }

    public void test026CheckCD_Image_ProtectionInfo() {
        // regression test case use 1 hour right ,here use 3-mins
        if (OptionsUtils.isMtkDrmApp()) {
            final String testFolderName = "testImageProtectionInfo";
            final String drmExtension = "." + DrmManager.EXT_DRM_CONTENT;
            final String drmFileName = "drmcd.dcf";

            final OmaDrmClient drmClient = new OmaDrmClient(mActivity);

            String path = TestUtils.getRegressionTestPath(testFolderName);
            File testFolder = new File(path);
            TestUtils.deleteFile(testFolder);
            TestUtils.createDirectory(testFolder);

            File drmFile = new File(path + MountPointManager.SEPARATOR
                    + drmFileName);
            FileInfo drmFileInfo = new FileInfo(drmFile);
            TestUtils.createFile(drmFile);
            TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                    com.mediatek.filemanager.tests.R.raw.ciipng, drmFile);

            int installRes = drmClient.installDrmMsg(drmFile.getAbsolutePath());
            assertTrue(installRes == OmaDrmClient.ERROR_NONE);
            // scanDrmFile(drmFile.getAbsolutePath(),DRM_CONTENT_MIMETYPE);
            TestUtils.sleep(2000);

            boolean launched = launchWithPath(path);
            assertTrue(launched);
            initDrmString(mActivity);

            // make sure there is only one file.
            FileInfoAdapter adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            // mSolo = new Solo(getInstrumentation(), mActivity);

            assertTrue(TestUtils.selectOneItem(this, mActivity, 0));
            TestUtils.sleep(1000);

            clickMoreMenu(R.id.protection_info);
            // mSolo.clickOnMenuItem(mActivity.getString(com.mediatek.internal.R.string.drm_protectioninfo_title));

            // TestUtils.sleep(3000);
            TextView fileName = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_file_name_value);
            assertTrue(fileName != null);
            assertTrue(fileName.getText().equals("drmcd"));

            TextView canForward = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_protection_status_value);
            assertTrue(canForward != null);
            assertTrue(canForward.getText().equals(DRM_INFO_NOT_FORWARD));

            TextView rightBegin = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_begin_value);
            assertTrue(rightBegin != null);
            assertTrue(rightBegin.getText()
                    .equals(DRM_INFO_RIGHT_NO_LIMITATION));

            TextView rightEnd = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_end_value);
            assertTrue(rightEnd != null);
            assertTrue(rightEnd.getText().equals(DRM_INFO_RIGHT_NO_LIMITATION));

            TextView rightCount = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_use_left_value);
            assertTrue(rightCount != null);
/*            assertTrue(rightCount.getText().equals(
                    DRM_INFO_TIME_INTERNAL_3_MINS));*/
            assertTrue(rightCount.getText()
                    .equals(DRM_INFO_RIGHT_NO_LIMITATION));

            // restore --- delete created files
            // TestUtils.deleteFile(drmFile);

            TestUtils.deleteFile(testFolder);
            drmClient.removeAllRights();
            drmClient.release();
        }
    }

    public void test027ForbidCD_Image_Share() {
        // regression test case use 1 hour right ,here use 3-mins
        if (OptionsUtils.isMtkDrmApp()) {
            final String testFolderName = "testImageShare";
            final String drmExtension = "." + DrmManager.EXT_DRM_CONTENT;
            final String drmFileName = "drmcd.dcf";

            final OmaDrmClient drmClient = new OmaDrmClient(mActivity);

            String path = TestUtils.getRegressionTestPath(testFolderName);
            File testFolder = new File(path);
            TestUtils.deleteFile(testFolder);
            TestUtils.createDirectory(testFolder);

            File drmFile = new File(path + MountPointManager.SEPARATOR
                    + drmFileName);
            FileInfo drmFileInfo = new FileInfo(drmFile);
            TestUtils.createFile(drmFile);
            TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                    com.mediatek.filemanager.tests.R.raw.ciipng, drmFile);

            int installRes = drmClient.installDrmMsg(drmFile.getAbsolutePath());
            assertTrue(installRes == OmaDrmClient.ERROR_NONE);
            // scanDrmFile(drmFile.getAbsolutePath(),DRM_CONTENT_MIMETYPE);
            TestUtils.sleep(2000);

            boolean launched = launchWithPath(path);
            assertTrue(launched);

            // make sure there is only one file.
            FileInfoAdapter adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            assertTrue(TestUtils.selectOneItem(this, mActivity, 0));
            TestUtils.sleep(2000);

            View shareMenu = mActivity.findViewById(R.id.share);
            assertTrue(shareMenu != null && !shareMenu.isEnabled());
            // restore --- delete created files
            // TestUtils.deleteFile(drmFile);
            TestUtils.deleteFile(testFolder);
            drmClient.removeAllRights();
            drmClient.release();
        }
    }

    public void test028ForbidCD_Image_CopyPaste() {
        // regression test case use 1 hour right ,here use 3-mins
        if (OptionsUtils.isMtkDrmApp()) {
            final String testFolderName = "testImageCopyPaste";
            final String drmExtension = "." + DrmManager.EXT_DRM_CONTENT;
            final String drmFileName = "drmcd.dcf";

            final OmaDrmClient drmClient = new OmaDrmClient(mActivity);

            String path = TestUtils.getRegressionTestPath(testFolderName);
            File testFolder = new File(path);
            TestUtils.deleteFile(testFolder);
            TestUtils.createDirectory(testFolder);

            File drmFile = new File(path + MountPointManager.SEPARATOR
                    + drmFileName);
            FileInfo drmFileInfo = new FileInfo(drmFile);
            TestUtils.createFile(drmFile);
            TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                    com.mediatek.filemanager.tests.R.raw.ciipng, drmFile);

            int installRes = drmClient.installDrmMsg(drmFile.getAbsolutePath());
            assertTrue(installRes == OmaDrmClient.ERROR_NONE);
            // scanDrmFile(drmFile.getAbsolutePath(),DRM_CONTENT_MIMETYPE);
            TestUtils.sleep(2000);

            boolean launched = launchWithPath(path);
            assertTrue(launched);

            // make sure there is only one file.
            FileInfoAdapter adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            assertTrue(TestUtils.selectOneItem(this, mActivity, 0));
            TestUtils.sleep(2000);

            clickViewBySolo(R.id.copy);
            TestUtils.sleep(2000);
            clickViewBySolo(R.id.paste);
            ActivityTestHelper.waitingForService(mActivity);
            TestUtils.sleep(2000);

            adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            // copy paste folder to do

            // restore --- delete created files
            // TestUtils.deleteFile(drmFile);
            TestUtils.deleteFile(testFolder);
            drmClient.removeAllRights();
            drmClient.release();
        }
    }

    public void test029ForbidCD_Audio_Share() {
        // regression test case use 1 hour right ,here use 3-mins
        if (OptionsUtils.isMtkDrmApp()) {
            final String testFolderName = "testAudioShare";
            final String drmExtension = "." + DrmManager.EXT_DRM_CONTENT;
            final String drmFileName = "drmcd.dcf";

            final OmaDrmClient drmClient = new OmaDrmClient(mActivity);

            String path = TestUtils.getRegressionTestPath(testFolderName);
            File testFolder = new File(path);
            TestUtils.deleteFile(testFolder);
            TestUtils.createDirectory(testFolder);

            File drmFile = new File(path + MountPointManager.SEPARATOR
                    + drmFileName);
            FileInfo drmFileInfo = new FileInfo(drmFile);
            TestUtils.createFile(drmFile);
            TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                    com.mediatek.filemanager.tests.R.raw.ccaamrnb, drmFile);

            int installRes = drmClient.installDrmMsg(drmFile.getAbsolutePath());
            assertTrue(installRes == OmaDrmClient.ERROR_NONE);
            // scanDrmFile(drmFile.getAbsolutePath(),DRM_CONTENT_MIMETYPE);
            TestUtils.sleep(2000);

            boolean launched = launchWithPath(path);
            assertTrue(launched);

            // make sure there is only one file.
            FileInfoAdapter adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            assertTrue(TestUtils.selectOneItem(this, mActivity, 0));
            TestUtils.sleep(2000);

            View shareMenu = mActivity.findViewById(R.id.share);
            assertTrue(shareMenu != null && !shareMenu.isEnabled());
            // restore --- delete created files
            TestUtils.deleteFile(testFolder);
            drmClient.removeAllRights();
            drmClient.release();
        }
    }

    public void test030ForbidCD_Audio_CopyPaste() {
        // regression test case use 1 hour right ,here use 3-mins
        if (OptionsUtils.isMtkDrmApp()) {
            final String testFolderName = "testAudioCopyPaste";
            final String drmExtension = "." + DrmManager.EXT_DRM_CONTENT;
            final String drmFileName = "drmcd.dcf";

            final OmaDrmClient drmClient = new OmaDrmClient(mActivity);

            String path = TestUtils.getRegressionTestPath(testFolderName);
            File testFolder = new File(path);
            TestUtils.deleteFile(testFolder);
            TestUtils.createDirectory(testFolder);

            File drmFile = new File(path + MountPointManager.SEPARATOR
                    + drmFileName);
            FileInfo drmFileInfo = new FileInfo(drmFile);
            TestUtils.createFile(drmFile);
            TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                    com.mediatek.filemanager.tests.R.raw.ccaamrnb, drmFile);

            int installRes = drmClient.installDrmMsg(drmFile.getAbsolutePath());
            assertTrue(installRes == OmaDrmClient.ERROR_NONE);
            // scanDrmFile(drmFile.getAbsolutePath(),DRM_CONTENT_MIMETYPE);
            TestUtils.sleep(2000);

            boolean launched = launchWithPath(path);
            assertTrue(launched);

            // make sure there is only one file.
            FileInfoAdapter adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            assertTrue(TestUtils.selectOneItem(this, mActivity, 0));
            TestUtils.sleep(2000);

            clickViewBySolo(R.id.copy);
            TestUtils.sleep(2000);
            clickViewBySolo(R.id.paste);
            ActivityTestHelper.waitingForService(mActivity);
            TestUtils.sleep(2000);

            adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            // copy paste folder to do

            // restore --- delete created files
            TestUtils.deleteFile(testFolder);
            drmClient.removeAllRights();
            drmClient.release();
        }
    }

    public void test031CheckSD_Image_ProtectionInfo() {
        // regression test case use 1 hour right ,here use 3-mins
        if (OptionsUtils.isMtkDrmApp()) {
            final String testFolderName = "testImageProtectionInfo";
            final String drmExtension = "." + DrmManager.EXT_DRM_CONTENT;
            final String drmFileName = "drmcd.dcf";

            final OmaDrmClient drmClient = new OmaDrmClient(mActivity);

            String path = TestUtils.getRegressionTestPath(testFolderName);
            File testFolder = new File(path);
            TestUtils.deleteFile(testFolder);
            TestUtils.createDirectory(testFolder);

            File drmFile = new File(path + MountPointManager.SEPARATOR
                    + drmFileName);
            FileInfo drmFileInfo = new FileInfo(drmFile);
            TestUtils.deleteFile(drmFile);
            TestUtils.createFile(drmFile);

            TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                    com.mediatek.filemanager.tests.R.raw.siiagif, drmFile);

            TestUtils.sleep(500);
            File fileAfter = new File(path + MountPointManager.SEPARATOR
                    + drmFileName);

            boolean launched = launchWithPath(path);
            assertTrue(launched);
            initDrmString(mActivity);

            // make sure there is only one file.
            FileInfoAdapter adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            // mSolo = new Solo(getInstrumentation(), mActivity);

            assertTrue(TestUtils.selectOneItem(this, mActivity, 0));
            TestUtils.sleep(1000);

            clickMoreMenu(R.id.protection_info);
            // mSolo.clickOnMenuItem(mActivity.getString(com.mediatek.internal.R.string.drm_protectioninfo_title));

            // TestUtils.sleep(3000);
            TestUtils.sleep(1000);
            TextView canForward = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_protection_status_value);
            assertTrue(canForward != null);
            assertTrue(canForward.getText().equals(DRM_INFO_CAN_FORWARD));

            TextView rightBegin = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_begin);
            assertTrue(rightBegin != null);
            assertTrue(rightBegin.getText().equals(DRM_NO_LICENSE));
            // restore --- delete created files
            TestUtils.deleteFile(testFolder);
            drmClient.removeAllRights();
            drmClient.release();
        }
    }

    public void test032ForbidSD_Image_Share() {
        // regression test case use 1 hour right ,here use 3-mins
        if (OptionsUtils.isMtkDrmApp()) {
            final String testFolderName = "testImageShare";
            final String drmExtension = "." + DrmManager.EXT_DRM_CONTENT;
            final String drmFileName = "drmcd.dcf";

            final OmaDrmClient drmClient = new OmaDrmClient(mActivity);

            String path = TestUtils.getRegressionTestPath(testFolderName);
            File testFolder = new File(path);
            TestUtils.deleteFile(testFolder);
            TestUtils.createDirectory(testFolder);

            File drmFile = new File(path + MountPointManager.SEPARATOR
                    + drmFileName);
            FileInfo drmFileInfo = new FileInfo(drmFile);
            TestUtils.createFile(drmFile);
            TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                    com.mediatek.filemanager.tests.R.raw.siiagif, drmFile);

            int installRes = drmClient.installDrmMsg(drmFile.getAbsolutePath());
            assertTrue(installRes == OmaDrmClient.ERROR_NONE);
            // scanDrmFile(drmFile.getAbsolutePath(),DRM_CONTENT_MIMETYPE);
            TestUtils.sleep(2000);

            boolean launched = launchWithPath(path);
            assertTrue(launched);

            // make sure there is only one file.
            FileInfoAdapter adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            assertTrue(TestUtils.selectOneItem(this, mActivity, 0));
            TestUtils.sleep(2000);

            View shareMenu = mActivity.findViewById(R.id.share);
            assertTrue(shareMenu != null && !shareMenu.isEnabled());

            // restore --- delete created files
            TestUtils.deleteFile(testFolder);
            drmClient.removeAllRights();
            drmClient.release();
        }
    }

    public void test033ForbidSD_Image_CopyPaste() {
        // regression test case use 1 hour right ,here use 3-mins
        if (OptionsUtils.isMtkDrmApp()) {
            final String testFolderName = "testImageCopyPaste";
            final String drmExtension = "." + DrmManager.EXT_DRM_CONTENT;
            final String drmFileName = "drmcd.dcf";

            final OmaDrmClient drmClient = new OmaDrmClient(mActivity);

            String path = TestUtils.getRegressionTestPath(testFolderName);
            File testFolder = new File(path);
            TestUtils.deleteFile(testFolder);
            TestUtils.createDirectory(testFolder);

            File drmFile = new File(path + MountPointManager.SEPARATOR
                    + drmFileName);
            FileInfo drmFileInfo = new FileInfo(drmFile);
            TestUtils.createFile(drmFile);
            TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                    com.mediatek.filemanager.tests.R.raw.siiagif, drmFile);

            int installRes = drmClient.installDrmMsg(drmFile.getAbsolutePath());
            assertTrue(installRes == OmaDrmClient.ERROR_NONE);
            // scanDrmFile(drmFile.getAbsolutePath(),DRM_CONTENT_MIMETYPE);
            TestUtils.sleep(500);

            boolean launched = launchWithPath(path);
            assertTrue(launched);

            // make sure there is only one file.
            FileInfoAdapter adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            assertTrue(TestUtils.selectOneItem(this, mActivity, 0));
            TestUtils.sleep(2000);

            clickViewBySolo(R.id.copy);
            TestUtils.sleep(2000);
            clickViewBySolo(R.id.paste);
            ActivityTestHelper.waitingForService(mActivity);
            TestUtils.sleep(2000);

            adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            // copy paste folder to do

            // restore --- delete created files
            TestUtils.deleteFile(testFolder);
            drmClient.removeAllRights();
            drmClient.release();
        }
    }

    public void test034CheckDrmFLSD_Image_ProtectionInfo() {
        if (OptionsUtils.isMtkDrmApp()) {

            final String testFolderName = "testImageProtectionInfo";
            final String drmExtension = "." + DrmManager.EXT_DRM_CONTENT;
            final String drmFileName = "drmflsd.dcf";

            final OmaDrmClient drmClient = new OmaDrmClient(mActivity);

            String path = TestUtils.getRegressionTestPath(testFolderName);
            File testFolder = new File(path);
            TestUtils.deleteFile(testFolder);
            TestUtils.createDirectory(testFolder);

            File drmFile = new File(path + MountPointManager.SEPARATOR
                    + drmFileName);
            FileInfo drmFileInfo = new FileInfo(drmFile);
            TestUtils.createFile(drmFile);
            TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                    com.mediatek.filemanager.tests.R.raw.flsdimage, drmFile);

            int installRes = drmClient.installDrmMsg(drmFile.getAbsolutePath());
            assertTrue(installRes == OmaDrmClient.ERROR_NONE);
            TestUtils.sleep(2000);

            boolean launched = launchWithPath(path);
            assertTrue(launched);
            initDrmString(mActivity);

            assertTrue(TestUtils.selectOneItem(this, mActivity, 0));
            clickMoreMenu(R.id.protection_info);

            TextView fileName = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_file_name_value);
            assertTrue(fileName != null);
            assertTrue(fileName.getText().equals("drmflsd"));

            TextView canForward = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_protection_status_value);
            assertTrue(canForward != null);
            assertTrue(canForward.getText().equals(DRM_INFO_NOT_FORWARD));

            TextView rightBegin = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_begin);
            assertTrue(rightBegin != null);
            assertTrue(rightBegin.getText().equals(DRM_NO_LICENSE));

            /*
             * TextView rightEnd = (TextView) mSolo
             * .getView(com.mediatek.internal.R.id.drm_end_value);
             * assertTrue(rightEnd != null);
             * assertTrue(rightEnd.getText().equals
             * (DRM_INFO_RIGHT_NO_LIMITATION));
             * 
             * TextView rightCount = (TextView) mSolo
             * .getView(com.mediatek.internal.R.id.drm_use_left_value);
             * assertTrue(rightCount != null); assertTrue(rightCount.getText()
             * .equals(DRM_INFO_RIGHT_NO_LIMITATION));
             * 
             * LogUtils.d("REYES", fileName.getText() + " " +
             * canForward.getText() + " " + rightBegin.getText() + " " +
             * rightEnd.getText() + " " + rightCount.getText());
             */
            // restore --- delete created files
            // TestUtils.deleteFile(drmFile);
            drmClient.removeAllRights();
            drmClient.release();
            TestUtils.deleteFile(testFolder);
        }
    }

    public void test035ForbidFLSD_Image_CopyPaste() {
        if (OptionsUtils.isMtkDrmApp()) {
            final String testFolderName = "testImageCopyPaste";
            final String drmExtension = "." + DrmManager.EXT_DRM_CONTENT;
            final String drmFileName = "drmflsd.dcf";

            final OmaDrmClient drmClient = new OmaDrmClient(mActivity);

            String path = TestUtils.getRegressionTestPath(testFolderName);
            File testFolder = new File(path);
            TestUtils.deleteFile(testFolder);
            TestUtils.createDirectory(testFolder);

            File drmFile = new File(path + MountPointManager.SEPARATOR
                    + drmFileName);
            FileInfo drmFileInfo = new FileInfo(drmFile);
            TestUtils.createFile(drmFile);
            TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                    com.mediatek.filemanager.tests.R.raw.flsdimage, drmFile);

            int installRes = drmClient.installDrmMsg(drmFile.getAbsolutePath());
            assertTrue(installRes == OmaDrmClient.ERROR_NONE);
            // scanDrmFile(drmFile.getAbsolutePath(),DRM_CONTENT_MIMETYPE);
            TestUtils.sleep(2000);

            boolean launched = launchWithPath(path);
            assertTrue(launched);

            // make sure there is only one file.
            FileInfoAdapter adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            assertTrue(TestUtils.selectOneItem(this, mActivity, 0));
            TestUtils.sleep(2000);

            clickViewBySolo(R.id.copy);
            TestUtils.sleep(2000);
            clickViewBySolo(R.id.paste);
            ActivityTestHelper.waitingForService(mActivity);
            TestUtils.sleep(2000);

            adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            // copy paste folder to do

            // restore --- delete created files
            TestUtils.deleteFile(testFolder);
            drmClient.removeAllRights();
            drmClient.release();
        }
    }

    public void test036CheckFL_Video_ProtectionInfo() {
        // regression test case use 1 hour right ,here use 3-mins
        if (OptionsUtils.isMtkDrmApp()) {
            final String testFolderName = "testVideoProtectionInfo";
            final String drmExtension = "." + DrmManager.EXT_DRM_CONTENT;
            final String drmFileName = "drmfl.dcf";

            final OmaDrmClient drmClient = new OmaDrmClient(mActivity);

            String path = TestUtils.getRegressionTestPath(testFolderName);
            File testFolder = new File(path);
            TestUtils.deleteFile(testFolder);
            TestUtils.createDirectory(testFolder);

            File drmFile = new File(path + MountPointManager.SEPARATOR
                    + drmFileName);
            FileInfo drmFileInfo = new FileInfo(drmFile);
            TestUtils.createFile(drmFile);
            TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                    com.mediatek.filemanager.tests.R.raw.fvmp4, drmFile);

            int installRes = drmClient.installDrmMsg(drmFile.getAbsolutePath());
            assertTrue(installRes == OmaDrmClient.ERROR_NONE);
            // scanDrmFile(drmFile.getAbsolutePath(),DRM_CONTENT_MIMETYPE);
            TestUtils.sleep(2000);

            boolean launched = launchWithPath(path);
            assertTrue(launched);
            initDrmString(mActivity);
            // make sure there is only one file.
            FileInfoAdapter adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            // mSolo = new Solo(getInstrumentation(), mActivity);

            assertTrue(TestUtils.selectOneItem(this, mActivity, 0));
            TestUtils.sleep(1000);

            clickMoreMenu(R.id.protection_info);
            // mSolo.clickOnMenuItem(mActivity.getString(com.mediatek.internal.R.string.drm_protectioninfo_title));

            TextView fileName = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_file_name_value);
            assertTrue(fileName != null);
            assertTrue(fileName.getText().equals("drmfl"));

            TextView canForward = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_protection_status_value);
            assertTrue(canForward != null);
            assertTrue(canForward.getText().equals(DRM_INFO_NOT_FORWARD));

            TextView rightBegin = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_begin_value);
            assertTrue(rightBegin != null);
            assertTrue(rightBegin.getText()
                    .equals(DRM_INFO_RIGHT_NO_LIMITATION));

            TextView rightEnd = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_end_value);
            assertTrue(rightEnd != null);
            assertTrue(rightEnd.getText().equals(DRM_INFO_RIGHT_NO_LIMITATION));

            TextView rightCount = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_use_left_value);
            assertTrue(rightCount != null);
            assertTrue(rightCount.getText()
                    .equals(DRM_INFO_RIGHT_NO_LIMITATION));

            LogUtils.d("REYES", fileName.getText() + " " + canForward.getText()
                    + " " + rightBegin.getText() + " " + rightEnd.getText()
                    + " " + rightCount.getText());
            // restore --- delete created files
            TestUtils.deleteFile(testFolder);
            drmClient.removeAllRights();
            drmClient.release();
        }
    }

    public void test037ForbidFL_Video_Share() {
        // regression test case use 1 hour right ,here use 3-mins
        if (OptionsUtils.isMtkDrmApp()) {
            final String testFolderName = "testVideoShare";
            final String drmExtension = "." + DrmManager.EXT_DRM_CONTENT;
            final String drmFileName = "drmcd.dcf";

            final OmaDrmClient drmClient = new OmaDrmClient(mActivity);

            String path = TestUtils.getRegressionTestPath(testFolderName);
            File testFolder = new File(path);
            TestUtils.deleteFile(testFolder);
            TestUtils.createDirectory(testFolder);

            File drmFile = new File(path + MountPointManager.SEPARATOR
                    + drmFileName);
            FileInfo drmFileInfo = new FileInfo(drmFile);
            TestUtils.createFile(drmFile);
            TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                    com.mediatek.filemanager.tests.R.raw.fvmp4, drmFile);

            int installRes = drmClient.installDrmMsg(drmFile.getAbsolutePath());
            assertTrue(installRes == OmaDrmClient.ERROR_NONE);
            // scanDrmFile(drmFile.getAbsolutePath(),DRM_CONTENT_MIMETYPE);
            TestUtils.sleep(2000);

            boolean launched = launchWithPath(path);
            assertTrue(launched);

            // make sure there is only one file.
            FileInfoAdapter adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            assertTrue(TestUtils.selectOneItem(this, mActivity, 0));
            TestUtils.sleep(2000);

            View shareMenu = mActivity.findViewById(R.id.share);
            assertTrue(shareMenu != null && !shareMenu.isEnabled());

            // restore --- delete created files
            TestUtils.deleteFile(testFolder);
            drmClient.removeAllRights();
            drmClient.release();
        }
    }

    public void test038ForbidFL_Video_CopyPaste() {
        // regression test case use 1 hour right ,here use 3-mins
        if (OptionsUtils.isMtkDrmApp()) {
            final String testFolderName = "testVideoCopyPaste";
            final String drmExtension = "." + DrmManager.EXT_DRM_CONTENT;
            final String drmFileName = "drmcd.dcf";

            final OmaDrmClient drmClient = new OmaDrmClient(mActivity);

            String path = TestUtils.getRegressionTestPath(testFolderName);
            File testFolder = new File(path);
            TestUtils.deleteFile(testFolder);
            TestUtils.createDirectory(testFolder);

            File drmFile = new File(path + MountPointManager.SEPARATOR
                    + drmFileName);
            FileInfo drmFileInfo = new FileInfo(drmFile);
            TestUtils.createFile(drmFile);
            TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                    com.mediatek.filemanager.tests.R.raw.fvmp4, drmFile);

            int installRes = drmClient.installDrmMsg(drmFile.getAbsolutePath());
            assertTrue(installRes == OmaDrmClient.ERROR_NONE);
            // scanDrmFile(drmFile.getAbsolutePath(),DRM_CONTENT_MIMETYPE);
            TestUtils.sleep(2000);

            boolean launched = launchWithPath(path);
            assertTrue(launched);

            // make sure there is only one file.
            FileInfoAdapter adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            assertTrue(TestUtils.selectOneItem(this, mActivity, 0));
            TestUtils.sleep(2000);

            clickViewBySolo(R.id.copy);
            TestUtils.sleep(2000);
            clickViewBySolo(R.id.paste);
            ActivityTestHelper.waitingForService(mActivity);
            TestUtils.sleep(2000);

            adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            // copy paste folder to do

            // restore --- delete created files
            TestUtils.deleteFile(testFolder);
            drmClient.removeAllRights();
            drmClient.release();
        }
    }

    public void test039CheckCD_Video_ProtectionInfo() {
        // regression test case use 1 hour right ,here use 3-mins
        if (OptionsUtils.isMtkDrmApp()) {
            final String testFolderName = "testVideoProtectionInfo";
            final String drmExtension = "." + DrmManager.EXT_DRM_CONTENT;
            final String drmFileName = "drmcd.dcf";

            final OmaDrmClient drmClient = new OmaDrmClient(mActivity);

            String path = TestUtils.getRegressionTestPath(testFolderName);
            File testFolder = new File(path);
            TestUtils.deleteFile(testFolder);
            TestUtils.createDirectory(testFolder);

            File drmFile = new File(path + MountPointManager.SEPARATOR
                    + drmFileName);
            FileInfo drmFileInfo = new FileInfo(drmFile);
            TestUtils.createFile(drmFile);
            TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                    com.mediatek.filemanager.tests.R.raw.ccvmp4, drmFile);

            int installRes = drmClient.installDrmMsg(drmFile.getAbsolutePath());
            assertTrue(installRes == OmaDrmClient.ERROR_NONE);
            // scanDrmFile(drmFile.getAbsolutePath(),DRM_CONTENT_MIMETYPE);
            TestUtils.sleep(2000);

            boolean launched = launchWithPath(path);
            assertTrue(launched);
            initDrmString(mActivity);

            // mSolo = new Solo(getInstrumentation(), mActivity);

            assertTrue(TestUtils.selectOneItem(this, mActivity, 0));
            TestUtils.sleep(1000);

            clickMoreMenu(R.id.protection_info);
            // mSolo.clickOnMenuItem(mActivity.getString(com.mediatek.internal.R.string.drm_protectioninfo_title));

            // TestUtils.sleep(3000);
            TextView fileName = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_file_name_value);
            assertTrue(fileName != null);
            assertTrue(fileName.getText().equals("drmcd"));

            TextView canForward = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_protection_status_value);
            assertTrue(canForward != null);
            assertTrue(canForward.getText().equals(DRM_INFO_NOT_FORWARD));

            TextView rightBegin = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_begin_value);
            assertTrue(rightBegin != null);
            assertTrue(rightBegin.getText()
                    .equals(DRM_INFO_RIGHT_NO_LIMITATION));

            TextView rightEnd = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_end_value);
            assertTrue(rightEnd != null);
            assertTrue(rightEnd.getText().equals(DRM_INFO_RIGHT_NO_LIMITATION));

            TextView rightCount = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_use_left_value);
            assertTrue(rightCount != null);
            assertTrue(rightCount.getText().equals(DRM_INFO_COUNT_LEFT_3));

            LogUtils.d("REYES", fileName.getText() + " " + canForward.getText()
                    + " " + rightBegin.getText() + " " + rightEnd.getText()
                    + " " + rightCount.getText());
            // restore --- delete created files
            TestUtils.deleteFile(testFolder);
            drmClient.removeAllRights();
            drmClient.release();
        }
    }

    public void test040ForbidCD_Video_Share() {
        // regression test case use 1 hour right ,here use 3-mins
        if (OptionsUtils.isMtkDrmApp()) {
            final String testFolderName = "testVideoShare";
            final String drmExtension = "." + DrmManager.EXT_DRM_CONTENT;
            final String drmFileName = "drmcd.dcf";

            final OmaDrmClient drmClient = new OmaDrmClient(mActivity);

            String path = TestUtils.getRegressionTestPath(testFolderName);
            File testFolder = new File(path);
            TestUtils.deleteFile(testFolder);
            TestUtils.createDirectory(testFolder);

            File drmFile = new File(path + MountPointManager.SEPARATOR
                    + drmFileName);
            FileInfo drmFileInfo = new FileInfo(drmFile);
            TestUtils.createFile(drmFile);
            TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                    com.mediatek.filemanager.tests.R.raw.ccvmp4, drmFile);

            int installRes = drmClient.installDrmMsg(drmFile.getAbsolutePath());
            assertTrue(installRes == OmaDrmClient.ERROR_NONE);
            // scanDrmFile(drmFile.getAbsolutePath(),DRM_CONTENT_MIMETYPE);
            TestUtils.sleep(2000);

            boolean launched = launchWithPath(path);
            assertTrue(launched);

            // make sure there is only one file.
            FileInfoAdapter adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            assertTrue(TestUtils.selectOneItem(this, mActivity, 0));
            TestUtils.sleep(2000);

            View shareMenu = mActivity.findViewById(R.id.share);
            assertTrue(shareMenu != null && !shareMenu.isEnabled());

            // restore --- delete created files
            TestUtils.deleteFile(testFolder);
            drmClient.removeAllRights();
            drmClient.release();
        }
    }

    public void test041ForbidCD_Video_CopyPaste() {
        // regression test case use 1 hour right ,here use 3-mins
        if (OptionsUtils.isMtkDrmApp()) {
            final String testFolderName = "testVideoCopyPaste";
            final String drmExtension = "." + DrmManager.EXT_DRM_CONTENT;
            final String drmFileName = "drmcd_video.dcf";

            final OmaDrmClient drmClient = new OmaDrmClient(mActivity);

            String path = TestUtils.getRegressionTestPath(testFolderName);
            File testFolder = new File(path);
            TestUtils.deleteFile(testFolder);
            TestUtils.createDirectory(testFolder);

            File drmFile = new File(path + MountPointManager.SEPARATOR
                    + drmFileName);
            FileInfo drmFileInfo = new FileInfo(drmFile);
            TestUtils.createFile(drmFile);
            TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                    com.mediatek.filemanager.tests.R.raw.ccvmp4, drmFile);

            int installRes = drmClient.installDrmMsg(drmFile.getAbsolutePath());
            assertTrue(installRes == OmaDrmClient.ERROR_NONE);
            // scanDrmFile(drmFile.getAbsolutePath(),DRM_CONTENT_MIMETYPE);
            TestUtils.sleep(2000);

            boolean launched = launchWithPath(path);
            assertTrue(launched);

            // make sure there is only one file.
            FileInfoAdapter adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            assertTrue(TestUtils.selectOneItem(this, mActivity, 0));
            TestUtils.sleep(2000);

            clickViewBySolo(R.id.copy);
            TestUtils.sleep(2000);
            clickViewBySolo(R.id.paste);
            ActivityTestHelper.waitingForService(mActivity);
            TestUtils.sleep(2000);

            adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            // copy paste folder to do

            // restore --- delete created files
            TestUtils.deleteFile(testFolder);
            drmClient.removeAllRights();
            drmClient.release();
        }
    }

    public void test042CheckSD_Video_ProtectionInfo() {
        // regression test case use 1 hour right ,here use 3-mins
        if (OptionsUtils.isMtkDrmApp()) {
            final String testFolderName = "testVideoProtectionInfo";
            final String drmExtension = "." + DrmManager.EXT_DRM_CONTENT;
            final String drmFileName = "drmsd.dcf";

            final OmaDrmClient drmClient = new OmaDrmClient(mActivity);

            String path = TestUtils.getRegressionTestPath(testFolderName);
            File testFolder = new File(path);
            TestUtils.deleteFile(testFolder);
            TestUtils.createDirectory(testFolder);

            File drmFile = new File(path + MountPointManager.SEPARATOR
                    + drmFileName);
            FileInfo drmFileInfo = new FileInfo(drmFile);
            TestUtils.createFile(drmFile);
            TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                    com.mediatek.filemanager.tests.R.raw.scvmp4, drmFile);

            // int installRes =
            // drmClient.installDrmMsg(drmFile.getAbsolutePath());
            // assertTrue(installRes == OmaDrmClient.ERROR_NONE);
            // scanDrmFile(drmFile.getAbsolutePath(),DRM_CONTENT_MIMETYPE);
            TestUtils.sleep(500);

            boolean launched = launchWithPath(path);
            assertTrue(launched);
            initDrmString(mActivity);

            // mSolo = new Solo(getInstrumentation(), mActivity);

            assertTrue(TestUtils.selectOneItem(this, mActivity, 0));
            TestUtils.sleep(1000);

            clickMoreMenu(R.id.protection_info);
            // mSolo.clickOnMenuItem(mActivity.getString(com.mediatek.internal.R.string.drm_protectioninfo_title));

            // TestUtils.sleep(3000);
            TextView fileName = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_file_name_value);
            assertTrue(fileName != null);
            assertTrue(fileName.getText().equals("drmsd"));

            TextView canForward = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_protection_status_value);
            assertTrue(canForward != null);
            assertTrue(canForward.getText().equals(DRM_INFO_CAN_FORWARD));

            TextView rightBegin = (TextView) mSolo
                    .getView(com.mediatek.internal.R.id.drm_begin);
            assertTrue(rightBegin != null);
            assertTrue(rightBegin.getText().equals(DRM_NO_LICENSE));

            // restore --- delete created files
            TestUtils.deleteFile(testFolder);
            drmClient.removeAllRights();
            drmClient.release();
        }
    }

    public void test043CheckSD_Video_Share() {
        // regression test case use 1 hour right ,here use 3-mins
        if (OptionsUtils.isMtkDrmApp()) {
            final String testFolderName = "testVideoShare";
            final String drmExtension = "." + DrmManager.EXT_DRM_CONTENT;
            final String drmFileName = "drmcd.dcf";

            final OmaDrmClient drmClient = new OmaDrmClient(mActivity);

            String path = TestUtils.getRegressionTestPath(testFolderName);
            File testFolder = new File(path);
            TestUtils.deleteFile(testFolder);
            TestUtils.createDirectory(testFolder);

            File drmFile = new File(path + MountPointManager.SEPARATOR
                    + drmFileName);
            FileInfo drmFileInfo = new FileInfo(drmFile);
            TestUtils.createFile(drmFile);
            TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                    com.mediatek.filemanager.tests.R.raw.scvmp4, drmFile);

            boolean launched = launchWithPath(path);
            assertTrue(launched);

            // make sure there is only one file.
            FileInfoAdapter adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            assertTrue(TestUtils.selectOneItem(this, mActivity, 0));
            TestUtils.sleep(2000);

            View shareMenu = mActivity.findViewById(R.id.share);
            assertTrue(shareMenu != null && shareMenu.isEnabled());

            // restore --- delete created files
            TestUtils.deleteFile(testFolder);
            drmClient.removeAllRights();
            drmClient.release();
        }
    }

    public void test044ForbidSD_Video_CopyPaste() {
        // regression test case use 1 hour right ,here use 3-mins
        if (OptionsUtils.isMtkDrmApp()) {
            final String testFolderName = "testVideoCopyPaste";
            final String drmExtension = "." + DrmManager.EXT_DRM_CONTENT;
            final String drmFileName = "drmcd.dcf";

            final OmaDrmClient drmClient = new OmaDrmClient(mActivity);

            String path = TestUtils.getRegressionTestPath(testFolderName);
            File testFolder = new File(path);
            TestUtils.deleteFile(testFolder);
            TestUtils.createDirectory(testFolder);

            File drmFile = new File(path + MountPointManager.SEPARATOR
                    + drmFileName);
            FileInfo drmFileInfo = new FileInfo(drmFile);
            TestUtils.createFile(drmFile);
            TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                    com.mediatek.filemanager.tests.R.raw.scvmp4, drmFile);

            /*
             * int installRes =
             * drmClient.installDrmMsg(drmFile.getAbsolutePath());
             * assertTrue(installRes == OmaDrmClient.ERROR_NONE); //
             * scanDrmFile(drmFile.getAbsolutePath(),DRM_CONTENT_MIMETYPE);
             * TestUtils.sleep(2000);
             */

            boolean launched = launchWithPath(path);
            assertTrue(launched);

            // make sure there is only one file.
            FileInfoAdapter adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            assertTrue(TestUtils.selectOneItem(this, mActivity, 0));
            TestUtils.sleep(2000);

            clickViewBySolo(R.id.copy);
            TestUtils.sleep(2000);
            clickViewBySolo(R.id.paste);
            ActivityTestHelper.waitingForService(mActivity);
            TestUtils.sleep(2000);

            adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            // copy paste folder to do

            // restore --- delete created files
            TestUtils.deleteFile(testFolder);
            drmClient.removeAllRights();
            drmClient.release();
        }
    }

    // flsd video no file current
    public void test956CheckFLSD_Video_ProtectionInfo() {

    }

    public void test960ForbidFLSD_Video_Share() {

    }

    public void test961ForbidFLSD_Video_CopyPaste() {

    }

    public void test048CheckSD_Audio_Share() {
        // regression test case use 1 hour right ,here use 3-mins
        if (OptionsUtils.isMtkDrmApp()) {
            final String testFolderName = "testAudioShare";
            final String drmExtension = "." + DrmManager.EXT_DRM_CONTENT;
            final String drmFileName = "drmsd.dcf";

            final OmaDrmClient drmClient = new OmaDrmClient(mActivity);

            String path = TestUtils.getRegressionTestPath(testFolderName);
            File testFolder = new File(path);
            TestUtils.deleteFile(testFolder);
            TestUtils.createDirectory(testFolder);

            File drmFile = new File(path + MountPointManager.SEPARATOR
                    + drmFileName);
            FileInfo drmFileInfo = new FileInfo(drmFile);
            TestUtils.createFile(drmFile);
            TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                    com.mediatek.filemanager.tests.R.raw.ssea3gp, drmFile);

            /*
             * int installRes =
             * drmClient.installDrmMsg(drmFile.getAbsolutePath());
             * assertTrue(installRes == OmaDrmClient.ERROR_NONE); //
             * scanDrmFile(drmFile.getAbsolutePath(),DRM_CONTENT_MIMETYPE);
             * TestUtils.sleep(2000);
             */

            boolean launched = launchWithPath(path);
            assertTrue(launched);

            // make sure there is only one file.
            FileInfoAdapter adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            assertTrue(TestUtils.selectOneItem(this, mActivity, 0));
            TestUtils.sleep(2000);

            View shareMenu = mActivity.findViewById(R.id.share);
            assertTrue(shareMenu != null && shareMenu.isEnabled());

            // restore --- delete created files
            TestUtils.deleteFile(testFolder);
            drmClient.removeAllRights();
            drmClient.release();
        }
    }

    public void test049ForbidSD_Audio_CopyPaste() {
        // regression test case use 1 hour right ,here use 3-mins
        if (OptionsUtils.isMtkDrmApp()) {
            final String testFolderName = "testAudioCopyPaste";
            final String drmExtension = "." + DrmManager.EXT_DRM_CONTENT;
            final String drmFileName = "drmsd.dcf";

            final OmaDrmClient drmClient = new OmaDrmClient(mActivity);

            String path = TestUtils.getRegressionTestPath(testFolderName);
            File testFolder = new File(path);
            TestUtils.deleteFile(testFolder);
            TestUtils.createDirectory(testFolder);

            File drmFile = new File(path + MountPointManager.SEPARATOR
                    + drmFileName);
            FileInfo drmFileInfo = new FileInfo(drmFile);
            TestUtils.createFile(drmFile);
            TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                    com.mediatek.filemanager.tests.R.raw.ssea3gp, drmFile);

            /*
             * int installRes =
             * drmClient.installDrmMsg(drmFile.getAbsolutePath());
             * assertTrue(installRes == OmaDrmClient.ERROR_NONE); //
             * scanDrmFile(drmFile.getAbsolutePath(),DRM_CONTENT_MIMETYPE);
             * TestUtils.sleep(2000);
             */

            boolean launched = launchWithPath(path);
            assertTrue(launched);

            // make sure there is only one file.
            FileInfoAdapter adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            assertTrue(TestUtils.selectOneItem(this, mActivity, 0));
            TestUtils.sleep(2000);

            clickViewBySolo(R.id.copy);
            TestUtils.sleep(2000);
            clickViewBySolo(R.id.paste);
            ActivityTestHelper.waitingForService(mActivity);
            TestUtils.sleep(2000);

            adapter = ActivityTestHelper.getAdapter(mActivity);
            assertTrue(adapter != null && adapter.getCount() == 1);

            // copy paste folder to do

            // restore --- delete created files
            TestUtils.deleteFile(testFolder);
            drmClient.removeAllRights();
            drmClient.release();
        }
    }

    private void scanDrmFile(String filePath, String mimeType) {
        String paths[] = { filePath };
        String mimeTypes[] = { mimeType };
        MediaScannerConnection.scanFile(mActivity, paths, mimeTypes, null);
    }

    private void openInternalStorage() {
        String phoneStoragePath = StorageManagerEx.getInternalStoragePath();
        assertTrue(!TextUtils.isEmpty(phoneStoragePath));

        boolean launched = launchWithPath(phoneStoragePath);
        assertTrue(launched);
    }

    private void clickBackKey(final FileManagerOperationActivity activity) {
        try {
            runTestOnUiThread(new Runnable() {
                public void run() {
                    ActivityTestHelper.onBackPressed(activity);
                }
            });
        } catch (Throwable e) {

        }
    }

    protected void createFolder(final String folderName) {
        clickViewBySolo(R.id.create_folder);
        TestUtils.sleep(2000);
        AlertDialog createFolderDialog = (AlertDialog) ActivityTestHelper
                .getDialog(mActivity, AbsBaseActivity.CREATE_FOLDER_DIALOG_TAG);
        final EditText editText = (EditText) createFolderDialog
                .findViewById(R.id.edit_text);

        TestUtils.TestFlag testFlag = new TestUtils.TestFlag();
        EditTextRunnable editTextRunnable = new EditTextRunnable(testFlag,
                editText, folderName);
        TestUtils.runUiThread(this, editTextRunnable, testFlag);

        TestUtils.sleep(1000);
        Button button = null;
        if (folderName.equals(editText.getText().toString())) {
            button = createFolderDialog
                    .getButton(DialogInterface.BUTTON_POSITIVE);
            if (!button.isEnabled()) {
                button = createFolderDialog
                        .getButton(DialogInterface.BUTTON_NEGATIVE);
            }
        } else {
            button = createFolderDialog
                    .getButton(DialogInterface.BUTTON_NEGATIVE);
        }
        mSolo.clickOnView(button);
        TestUtils.sleep(2000);
    }

    private boolean checkSystemFolders(FileInfoAdapter adapter) {
        int count = adapter.getCount();
        assertTrue(count >= mSystemDefaultFolders.size());
        int checkedNum = 0;
        for (int i = 0; i < count; i++) {
            FileInfo curFileInfo = adapter.getItem(i);
            if (mSystemDefaultFolders.contains(curFileInfo.getShowName())) {
                checkedNum += 1;
            }
        }
        LogUtils.d(TAG, "checkedNum: " + checkedNum + " totalNum: "
                + mSystemDefaultFolders.size());
        // assertTrue(checkedNum==mSystemDefaultFolders.size());
        return checkedNum == mSystemDefaultFolders.size();
    }

    private void waitFor(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            LogUtils.d(TAG, "waitFor exception: " + e);
        }
    }
}
