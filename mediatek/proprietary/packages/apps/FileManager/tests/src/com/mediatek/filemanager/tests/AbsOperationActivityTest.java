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

import android.app.AlertDialog;
import android.app.Instrumentation;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.test.ActivityInstrumentationTestCase2;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.jayway.android.robotium.solo.Solo;

import com.mediatek.filemanager.AbsBaseActivity;
import com.mediatek.filemanager.ActivityTestHelper;
import com.mediatek.filemanager.FileInfo;
import com.mediatek.filemanager.FileManagerOperationActivity;
import com.mediatek.filemanager.MenuItemHelper;
import com.mediatek.filemanager.R;
import com.mediatek.filemanager.tests.utils.TestUtils;
import com.mediatek.filemanager.tests.utils.TestUtils.ClickOptionMenuRunnable;
import com.mediatek.filemanager.tests.utils.TestUtils.EditTextRunnable;
import com.mediatek.filemanager.tests.utils.TestUtils.SortFileRunnable;
import com.mediatek.filemanager.utils.LogUtils;

/**
 * Test CMCC cases.
 */
public abstract class AbsOperationActivityTest extends
        ActivityInstrumentationTestCase2<FileManagerOperationActivity> {
    private final static String TAG = "AbsOperationActivityTest";

    protected FileManagerOperationActivity mActivity = null;
    protected Instrumentation mInst = null;
    protected PowerManager.WakeLock mWakeLock = null;
    protected Solo mSolo = null;

    public AbsOperationActivityTest() {
        super(FileManagerOperationActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // replace input method
        // Settings.Secure.putString(getActivity().getContentResolver(),
        // Settings.Secure.DEFAULT_INPUT_METHOD,
        // "com.android.inputmethod.latin/.LatinIME");

        mInst = getInstrumentation();

        PowerManager pm = (PowerManager) mInst.getContext().getSystemService(
                Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);
        mWakeLock.acquire();

        KeyguardManager km = (KeyguardManager) mInst.getContext()
                .getSystemService(Context.KEYGUARD_SERVICE);
        km.newKeyguardLock(TAG).disableKeyguard();

        SystemProperties.set("ro.operator.optr", "OP02");
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
        mWakeLock.release();
        super.tearDown();
    }

    protected void createFolder(final String fileName) {
        LogUtils.d(TAG, "call createFolder(...)");
        TestUtils.sleep(1500);
        //View creatFolder = mActivity.findViewById(R.id.create_folder);
        clickViewBySolo(R.id.create_folder);
        //TouchUtils.clickView(AbsOperationActivityTest.this, creatFolder);
        TestUtils.sleep(3000);
        AlertDialog createFolderDialog = (AlertDialog) ActivityTestHelper
                .getDialog(mActivity, AbsBaseActivity.CREATE_FOLDER_DIALOG_TAG);
        final EditText editText = (EditText) createFolderDialog
                .findViewById(R.id.edit_text);

        TestUtils.TestFlag testFlag = new TestUtils.TestFlag();
        EditTextRunnable editTextRunnable = new EditTextRunnable(testFlag,
                editText, fileName);
        TestUtils.runUiThread(this, editTextRunnable, testFlag);

        Button button = null;
        if (fileName.equals(editText.getText().toString())) {
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
/*        TestUtils.clickViewWithIMEHiden(AbsOperationActivityTest.this,
                mActivity, button);*/
        // TouchUtils.clickView(AbsOperationActivityTest.this, button);
        // TestUtils.sleep(1500);
    }

    /**
     * This method gets a button of alertDialog according to a flag
     *
     * @param alertDialog
     *            the Dialog to choice from
     * @param isCertain
     *            true to select positive button, or negative button
     * @return the selected button
     */
    public Button getButtonOfDialog(AlertDialog alertDialog, boolean isCertain) {
        Button button = null;
        if (isCertain) {
            button = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            if (!button.isEnabled()) {
                button = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
            }
        } else {
            button = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        }
        return button;
    }

    protected boolean renameFile(final FileInfo fileInfo, final String newName,
            boolean isCertain) {
        LogUtils.d(TAG, "call renameFile(...)");
        TestUtils.sleep(500);
        int index = TestUtils.getListViewItemIndex(this, mActivity, fileInfo);
        if (!TestUtils.selectOneItem(this, mActivity, index)) {
            return false;
        }

        MenuItemHelper menuItem = new MenuItemHelper(R.id.rename);
        TestUtils.TestFlag testFlag = new TestUtils.TestFlag();
        ClickOptionMenuRunnable clickMenuRunnable = new ClickOptionMenuRunnable(
                testFlag, mActivity, menuItem);
        TestUtils.runUiThread(this, clickMenuRunnable, testFlag);

        AlertDialog reNameFolderDialog = (AlertDialog) ActivityTestHelper
                .getDialog(mActivity,
                        FileManagerOperationActivity.RENAME_DIALOG_TAG);
        final EditText editText = (EditText) reNameFolderDialog
                .findViewById(R.id.edit_text);

        EditTextRunnable editTextRunnable = new EditTextRunnable(testFlag,
                editText, newName);
        TestUtils.runUiThread(this, editTextRunnable, testFlag);

        Button button = null;
        if (newName.equals(editText.getText().toString())) {
            button = getButtonOfDialog(reNameFolderDialog, isCertain);
        } else {
            button = reNameFolderDialog
                    .getButton(DialogInterface.BUTTON_NEGATIVE);
        }

        // TouchUtils.clickView(AbsOperationActivityTest.this, button);
/*        TestUtils.clickViewWithIMEHiden(AbsOperationActivityTest.this,
                mActivity, button);*/
        mSolo.clickOnView(button);
        TestUtils.sleep(1000);
        return true;
    }

    protected boolean renameFileExtension(FileInfo fileInfo,
            final String newName, boolean isCertain) {
        LogUtils.d(TAG, "call renameFileExtension(...)");
        TestUtils.sleep(500);
        int index = TestUtils.getListViewItemIndex(this, mActivity, fileInfo);
        if (!TestUtils.selectOneItem(this, mActivity, index)) {
            return false;
        }

        MenuItemHelper menuItem = new MenuItemHelper(R.id.rename);
        TestUtils.TestFlag testFlag = new TestUtils.TestFlag();
        ClickOptionMenuRunnable clickMenuRunnable = new ClickOptionMenuRunnable(
                testFlag, mActivity, menuItem);
        TestUtils.runUiThread(this, clickMenuRunnable, testFlag);

        AlertDialog reNameFolderDialog = (AlertDialog) ActivityTestHelper
                .getDialog(mActivity,
                        FileManagerOperationActivity.RENAME_DIALOG_TAG);
        final EditText editText = (EditText) reNameFolderDialog
                .findViewById(R.id.edit_text);

        EditTextRunnable editTextRunnable = new EditTextRunnable(testFlag,
                editText, newName);
        TestUtils.runUiThread(this, editTextRunnable, testFlag);

        Button button = null;
        if (newName.equals(editText.getText().toString())) {
            button = getButtonOfDialog(reNameFolderDialog, true);
        } else {
            button = reNameFolderDialog
                    .getButton(DialogInterface.BUTTON_NEGATIVE);
        }
        // TouchUtils.clickView(AbsOperationActivityTest.this, button);
/*        TestUtils.clickViewWithIMEHiden(AbsOperationActivityTest.this,
                mActivity, button);*/
        mSolo.clickOnView(button);

        AlertDialog renameConfirmDialog = (AlertDialog) ActivityTestHelper
                .getDialog(
                        mActivity,
                        FileManagerOperationActivity.RENAME_EXTENSION_DIALOG_TAG);
        if (renameConfirmDialog == null) {
            return false;
        }
        Button confirmButton = null;
        confirmButton = getButtonOfDialog(renameConfirmDialog, isCertain);
        //TouchUtils.clickView(AbsOperationActivityTest.this, confirmButton);
        mSolo.clickOnView(confirmButton);
        TestUtils.sleep(500);
        ActivityTestHelper.waitingForService(mActivity);
        return true;
    }

    public boolean deleteAll(boolean isCertain) {
        LogUtils.d(TAG, "call deleteAll(...)");
        TestUtils.sleep(500);
        if (!TestUtils.selectAllItems(this, mActivity)) {
            return false;
        }

/*        View deleteView = mActivity.findViewById(R.id.delete);
        TouchUtils.clickView(AbsOperationActivityTest.this, deleteView);*/
        clickViewBySolo(R.id.delete);

        AlertDialog deleteDialog = (AlertDialog) ActivityTestHelper.getDialog(
                mActivity, FileManagerOperationActivity.DELETE_DIALOG_TAG);
        if (deleteDialog == null) {
            LogUtils.d(TAG, "deleteDialog == null in deleteAll()");
            return false;
        }
        Button button = null;
        button = getButtonOfDialog(deleteDialog, isCertain);
        //TouchUtils.clickView(AbsOperationActivityTest.this, button);
        mSolo.clickOnView(button);

        ActivityTestHelper.waitingForService(mActivity);
        // TestUtils.sleep(1500);
        return true;
    }

    public boolean deleteFile(FileInfo fileInfo, boolean isCertain) {
        LogUtils.d(TAG, "call deleteFile(...)");
        TestUtils.sleep(500);
        int index = TestUtils.getListViewItemIndex(this, mActivity, fileInfo);
        if (!TestUtils.selectOneItem(this, mActivity, index)) {
            return false;
        }

        TestUtils.sleep(1000);
/*        View deleteView = mActivity.findViewById(R.id.delete);
        TouchUtils.clickView(AbsOperationActivityTest.this, deleteView);*/
        clickViewBySolo(R.id.delete);

        AlertDialog deleteDialog = (AlertDialog) ActivityTestHelper.getDialog(
                mActivity, FileManagerOperationActivity.DELETE_DIALOG_TAG);
        Button button = getButtonOfDialog(deleteDialog, isCertain);
        //TouchUtils.clickView(AbsOperationActivityTest.this, button);
        mSolo.clickOnView(button);

        ActivityTestHelper.waitingForService(mActivity);
        return true;
    }

    protected boolean shareFile(FileInfo fileInfo, boolean isAll) {
        LogUtils.d(TAG, "call shareFile(...)");
        TestUtils.sleep(1000);
        int index = 0;
        if (fileInfo != null) {
            index = TestUtils.getListViewItemIndex(this, mActivity, fileInfo);
        }
        if (!TestUtils.selectOneItem(this, mActivity, index)) {
            return false;
        }

        if (isAll) {
            TestUtils.selectAllItems(this, mActivity);
        }

/*        View shareView = (View) mActivity.findViewById(R.id.share);
        TouchUtils.clickView(AbsOperationActivityTest.this, shareView);*/
        clickViewBySolo(R.id.share);
        TestUtils.sleep(500);
        return true;
    }

    protected boolean getDetail(FileInfo fileInfo) {
        LogUtils.d(TAG, "call getDetail(...)");
        TestUtils.sleep(500);
        int index = TestUtils.getListViewItemIndex(this, mActivity, fileInfo);
        if (!TestUtils.selectOneItem(this, mActivity, index)) {
            return false;
        }

        MenuItemHelper menuItem = new MenuItemHelper(R.id.details);
        TestUtils.TestFlag testFlag = new TestUtils.TestFlag();
        ClickOptionMenuRunnable clickMenuRunnable = new ClickOptionMenuRunnable(
                testFlag, mActivity, menuItem);
        TestUtils.runUiThread(this, clickMenuRunnable, testFlag);

        AlertDialog detailDialog = (AlertDialog) ActivityTestHelper
                .getDetailDialog(mActivity);
        if (detailDialog == null) {
            return false;
        }
        TestUtils.sleep(500);
        // Button button =
        // detailDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        // TouchUtils.clickView(AbsOperationActivityTest.this, button);
        detailDialog.dismiss();

        ActivityTestHelper.waitingForService(mActivity);
        mInst.sendCharacterSync(KeyEvent.KEYCODE_BACK);
        return true;
    }

    protected boolean getProtectInfo(FileInfo fileInfo) {
        LogUtils.d(TAG, "call getProtectInfo(...)");
        TestUtils.sleep(500);
        int index = TestUtils.getListViewItemIndex(this, mActivity, fileInfo);
        if (!TestUtils.selectOneItem(this, mActivity, index)) {
            return false;
        }

        MenuItemHelper menuItem = new MenuItemHelper(R.id.protection_info);
        TestUtils.TestFlag testFlag = new TestUtils.TestFlag();
        ClickOptionMenuRunnable clickMenuRunnable = new ClickOptionMenuRunnable(
                testFlag, mActivity, menuItem);
        TestUtils.runUiThread(this, clickMenuRunnable, testFlag);

        ActivityTestHelper.waitingForService(mActivity);
        return true;
    }

    public void setSort(int sort) {
        LogUtils.d(TAG, "call setSort(...)");
        TestUtils.sleep(500);

        TestUtils.TestFlag testFlag = new TestUtils.TestFlag();
        MenuItemHelper menuItem = new MenuItemHelper(R.id.sort);

        SortFileRunnable sortRunnable = new SortFileRunnable(testFlag,
                mActivity, menuItem);
        TestUtils.runUiThread(this, sortRunnable, testFlag);
        TestUtils.sleep(2000);
        ActivityTestHelper.waitingForService(mActivity);

        AlertDialog choiceDialog = (AlertDialog) ActivityTestHelper
                .getChoiceDialog(mActivity);
        if (sort == ActivityTestHelper.getSortType(mActivity)) {
            choiceDialog.dismiss();
        } else {
            ListView listView = choiceDialog.getListView();
            View view = listView.getChildAt(sort);
            //TouchUtils.clickView(AbsOperationActivityTest.this, view);
            mSolo.clickOnView(view);
        }
        TestUtils.sleep(1000);
    }

    /**
     * isAll == true, copy all files; isAll == false, copy selected one file
     *
     * @param fileInfo
     * @param isAll
     * @return
     */
    public boolean copyFile(FileInfo fileInfo, boolean isAll) {
        LogUtils.d(TAG, "call copyFile(...)");
        TestUtils.sleep(500);
        int index = 0;
        if (fileInfo != null) {
            index = TestUtils.getListViewItemIndex(this, mActivity, fileInfo);
        }
        if (!TestUtils.selectOneItem(this, mActivity, index)) {
            return false;
        }

        if (isAll) {
            TestUtils.selectAllItems(this, mActivity);
        }
        View copyView = mActivity.findViewById(R.id.copy);
        //TouchUtils.clickView(AbsOperationActivityTest.this, copyView);
        mSolo.clickOnView(copyView);
        TestUtils.sleep(500);
        return true;
    }

    public boolean cutFile(FileInfo fileInfo, boolean isAll) {
        LogUtils.d(TAG, "call cutFile(...)");
        TestUtils.sleep(500);
        int index = 0;
        if (fileInfo != null) {
            index = TestUtils.getListViewItemIndex(this, mActivity, fileInfo);
        }
        if (!TestUtils.selectOneItem(this, mActivity, index)) {
            return false;
        }

        if (isAll) {
            TestUtils.selectAllItems(this, mActivity);
        }
        View cutView = mActivity.findViewById(R.id.cut);
        // if hotknot share support, cut menu will be in more.
        if (null == cutView) {
            MenuItemHelper menuItem = new MenuItemHelper(R.id.cut);
            TestUtils.TestFlag testFlag = new TestUtils.TestFlag();
            ClickOptionMenuRunnable clickMenuRunnable = new ClickOptionMenuRunnable(
                    testFlag, mActivity, menuItem);
            TestUtils.runUiThread(this, clickMenuRunnable, testFlag);
        } else {
            //TouchUtils.clickView(AbsOperationActivityTest.this, cutView);
            mSolo.clickOnView(cutView);
        }
        TestUtils.sleep(1000);
        return true;
    }

    public boolean paste() {
        LogUtils.d(TAG, "call paste(...)");
        TestUtils.sleep(3000);
        View pasteView = mActivity.findViewById(R.id.paste);
        if (pasteView == null) {
            return false;
        }
        //TouchUtils.clickView(AbsOperationActivityTest.this, pasteView);
        mSolo.clickOnView(pasteView);
        ActivityTestHelper.waitingForService(mActivity);
        return true;
    }

    protected boolean launchWithPath(String defLaunchPath) {
        if (defLaunchPath != null) {
            Intent intent = new Intent();
            intent.putExtra(
                    FileManagerOperationActivity.INTENT_EXTRA_SELECT_PATH,
                    defLaunchPath);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            setActivityIntent(intent);
        }
        TestUtils.sleep(1000);
        mActivity = getActivity();
        ActivityTestHelper.waitingForServiceConnected(mActivity);
        TestUtils.sleep(500);
        mSolo = new Solo(mInst, mActivity);

        String curPath = ActivityTestHelper.getCurrentPath(mActivity);
        if (curPath != null && curPath.equals(defLaunchPath)) {
            LogUtils.d(TAG, "launch path:" + defLaunchPath);
            return true;
        }
        LogUtils.e(TAG, defLaunchPath + " is unmounted. curPath:" + curPath);
        return false;
    }

    public boolean launchWithStartActivity(String defLaunchPath) {
        if (defLaunchPath != null) {
            Intent intent = new Intent();
            intent.setClass(mInst.getTargetContext(),
                    FileManagerOperationActivity.class);
            intent.putExtra(
                    FileManagerOperationActivity.INTENT_EXTRA_SELECT_PATH,
                    defLaunchPath);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mInst.getContext().startActivity(intent);
        }
        TestUtils.sleep(1000);
        mActivity = getActivity();
        ActivityTestHelper.waitingForServiceConnected(mActivity);
        TestUtils.sleep(500);
        mSolo = new Solo(mInst, mActivity);

        String curPath = ActivityTestHelper.getCurrentPath(mActivity);
        if (curPath != null && curPath.equals(defLaunchPath)) {
            LogUtils.d(TAG, "launch path:" + defLaunchPath);
            return true;
        }
        LogUtils.e(TAG, defLaunchPath + " is unmounted. curPath:" + curPath);
        return false;
    }

    public void copyCancel(FileInfo fileInfo, boolean isAll) {
        LogUtils.d(TAG, "call copyCancel(...)");
        TestUtils.sleep(500);
        int index = 0;
        if (fileInfo != null) {
            index = TestUtils.getListViewItemIndex(this, mActivity, fileInfo);
        }
        if (!TestUtils.selectOneItem(this, mActivity, index)) {
            return;
        }
        if (isAll) {
            TestUtils.selectAllItems(this, mActivity);
        }
        View copyView = mActivity.findViewById(R.id.copy);
        //TouchUtils.clickView(this, copyView);
        mSolo.clickOnView(copyView);
        TestUtils.sleep(2000);
        View pasteView = mActivity.findViewById(R.id.paste);
        if (pasteView == null) {
            return;
        }
        //TouchUtils.clickView(this, pasteView);
        mSolo.clickOnView(pasteView);
        TestUtils.sleep(1000);
        AlertDialog progressDialog = (AlertDialog) ActivityTestHelper
                .getProgressDialog(mActivity);
        if (progressDialog == null) {
            return;
        }

        Button button = null;
        button = progressDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        //TouchUtils.clickView(this, button);
        mSolo.clickOnView(button);
        ActivityTestHelper.waitingForService(mActivity);
    }

    protected void clickViewBySolo(int viewId) {
        assertTrue(viewId > 0);
        //View view = mSolo.getView(viewId);
        View view = mActivity.findViewById(viewId);
        // if hotknot share support, cut menu will be in more.
        if (null == view) {
            MenuItemHelper menuItem = new MenuItemHelper(viewId);
            TestUtils.TestFlag testFlag = new TestUtils.TestFlag();
            ClickOptionMenuRunnable clickMenuRunnable = new ClickOptionMenuRunnable(testFlag, mActivity, menuItem);
            TestUtils.runUiThread(this, clickMenuRunnable, testFlag);
            TestUtils.sleep(1000);
        } else {
            //ActivityTestHelper.waitingForService(mActivity);
            TestUtils.sleep(1000);
            mSolo.clickOnView(view);
            ActivityTestHelper.waitingForService(mActivity);
        }
    }

    protected void clickMoreMenu(int menuId) {
        assertTrue(menuId > 0);
        MenuItemHelper menuItem = new MenuItemHelper(menuId);
        TestUtils.TestFlag testFlag = new TestUtils.TestFlag();
        ClickOptionMenuRunnable clickMenuRunnable = new ClickOptionMenuRunnable(testFlag, mActivity, menuItem);
        TestUtils.runUiThread(this, clickMenuRunnable, testFlag);
        TestUtils.sleep(3000);
    }

    protected String getDetailInfo(FileInfo fileInfo) {
        LogUtils.d(TAG, "call getDetail(...)");
        TestUtils.sleep(500);
        int index = TestUtils.getListViewItemIndex(this, mActivity, fileInfo);
        if (!TestUtils.selectOneItem(this, mActivity, index)) {
            return null;
        }

        MenuItemHelper menuItem = new MenuItemHelper(R.id.details);
        TestUtils.TestFlag testFlag = new TestUtils.TestFlag();
        ClickOptionMenuRunnable clickMenuRunnable = new ClickOptionMenuRunnable(testFlag, mActivity, menuItem);
        TestUtils.runUiThread(this, clickMenuRunnable, testFlag);
        ActivityTestHelper.waitingForService(mActivity);

        TestUtils.sleep(3000);
        AlertDialog detailDialog = (AlertDialog) ActivityTestHelper.getDetailDialog(mActivity);
        if (detailDialog == null) {
            return null;
        }
        TextView detailText = (TextView) detailDialog.findViewById(R.id.details_text);

        ActivityTestHelper.waitingForService(mActivity);
        mInst.sendCharacterSync(KeyEvent.KEYCODE_BACK);
        return detailText.getText().toString();
    }
}