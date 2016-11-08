package com.mediatek.filemanager.tests;

import android.view.View;
import android.widget.LinearLayout;

import com.mediatek.filemanager.IconManager;
import com.mediatek.filemanager.MountPointManager;
import com.mediatek.filemanager.R;
import com.mediatek.filemanager.tests.utils.TestUtils;
import com.mediatek.filemanager.utils.LogUtils;

import java.io.File;

public class FunctionTest extends AbsOperationActivityTest {

    private final String TAG = "FunctionTest";
    //MediaStoreHelper mMediaProviderHelper = new MediaStoreHelper(mActivity);

    public void test001Navigation() {
        LogUtils.i(TAG, "call testNavigation()");
        final int FOLDER_COUNT = 10;
        String curFolderName = "testNavigation";
        String baseName = "test_";
        StringBuilder pathBuilder = new StringBuilder();

        for (int i = 0; i < FOLDER_COUNT; i++) {
            pathBuilder.append(MountPointManager.SEPARATOR + baseName + i);
        }

        String curPath = TestUtils.getTestPath(curFolderName + MountPointManager.SEPARATOR
                + pathBuilder.toString());
        File newFolder = new File(curPath);
        TestUtils.createDirectory(newFolder);

        boolean launched = launchWithPath(curPath);
        assertTrue(launched);
        TestUtils.sleep(500);

        LinearLayout tabsHolder = (LinearLayout) mActivity.findViewById(R.id.tabs_holder);
        assertFalse(tabsHolder == null);
        int count = tabsHolder.getChildCount();
        View buttonView;
        for (int i = count - 1; i >= 0; i--) {
            buttonView = tabsHolder.getChildAt(i);
            assertTrue(buttonView != null);
            //TouchUtils.clickView(this, buttonView);
            mSolo.clickOnView(buttonView);
            TestUtils.sleep(500);
        }

    }

//    public void test002PopupMenu() {
//        LogUtils.i(TAG, "call testPopupMenu()");
//
//        String curFolderName = "testPopupMenu";
//        String curPath = TestUtils.getTestPath(curFolderName);
//        File loadFile = new File(curPath);
//        TestUtils.createDirectory(loadFile);
//
//        File subFile = new File(curPath + MountPointManager.SEPARATOR + "EditPopupMenu");
//        TestUtils.createDirectory(subFile);
//
//        boolean launched = launchWithPath(curPath);
//        assertTrue(launched);
//        TestUtils.sleep(500);
//
//        // // MenuItemHelper menuItem = new MenuItemHelper(R.id.popup_menu);
//        // MenuItemHelper menuItem = new MenuItemHelper(R.id.popup_menu);
//        // TestUtils.TestFlag testFlag = new TestUtils.TestFlag();
//        // ClickMenuRunnable clickMenuRunnable = new ClickMenuRunnable(testFlag,
//        // mActivity, menuItem);
//        // TestUtils.runUiThread(this, clickMenuRunnable, testFlag);
//        // // mActivity.onOptionsItemSelected(menuItem);
//
//        View popupMenu = mActivity.findViewById(R.id.popup_menu);
//        TestUtils.sleep(1000);
//        TouchUtils.clickView(this, popupMenu);
//        TestUtils.sleep(500);
//        mInst.sendCharacterSync(KeyEvent.KEYCODE_BACK);
//        TestUtils.sleep(500);
//
//        // MenuItemHelper menuItem = new MenuItemHelper(R.id.change_mode);
//        // TestUtils.TestFlag testFlag = new TestUtils.TestFlag();
//        // ClickMenuRunnable clickMenuRunnable = new ClickMenuRunnable(testFlag,
//        // mActivity, menuItem);
//        // TestUtils.runUiThread(this, clickMenuRunnable, testFlag);
//        // TestUtils.sleep(500);
//
//        FileInfo fileInfo = new FileInfo(subFile);
//
//        int index = TestUtils.getListViewItemIndex(this, mActivity, fileInfo);
//        TestUtils.selectOneItem(this, mActivity, index);
//        TestUtils.sleep(500);
//
//        popupMenu = mActivity.findViewById(R.id.popup_menu);
//        TestUtils.sleep(1000);
//        TouchUtils.clickView(this, popupMenu);
//        TestUtils.sleep(100);
//        mInst.sendCharacterSync(KeyEvent.KEYCODE_BACK);
//        // View textSelect = mActivity.findViewById(R.id.text_select);
//        // TestUtils.sleep(1000);
//        // TouchUtils.clickView(this, textSelect);
//
//        TestUtils.sleep(100);
//        mInst.sendCharacterSync(KeyEvent.KEYCODE_BACK);
//
//    }

    public void test003Update3gppMineType() {
      ///M:@#3gp#@{the follow code is to handle the 3gp file.not do special handle now
//        LogUtils.i(TAG, "call testUpdate3gppMineType()");
//        final String folderName = "test_update3gppMineType";
//        mActivity = getActivity();
//        FileManagerService tempService = ActivityTestHelper.getServiceInstance(mActivity);
//        String curPath = TestUtils.getTestPath(folderName);
//
//        String fileName = TestUtils.getFileName(curPath, false);
//        fileName = fileName + ".3gp";
//        File file = new File(curPath + MountPointManager.SEPARATOR + fileName);
//        TestUtils.createFile(file);
//        FileInfo fileInfo = new FileInfo(file);
//        String mimetype = tempService.update3gppMimetype(fileInfo);
//        mimetype = fileInfo.getFileOriginMimeType();
        ///M:@{the top code is to handle the 3gp file.not do special handle now
    }

    public void test004CreateBeamUris() {
        LogUtils.i(TAG, "call testCreateBeamUris()");
        mActivity = getActivity();
        mActivity.createBeamUris(null);
    }

    public void test005LogUtils() {
        LogUtils.i(TAG, "call testLogUtils()");
        Throwable t = new Throwable();
        //LogUtils.d(TAG, "test LogUtils.d", t);
        //LogUtils.e(TAG, "test LogUtils.e", t);
        //LogUtils.i(TAG, "test LogUtils.i", t);
        //LogUtils.v(TAG, "test LogUtils.v", t);
        //LogUtils.w(TAG, "test LogUtils.w", t);
    }

    public void test006ExternalIcon() {
        LogUtils.i(TAG, "call testExternalIcon()");
        IconManager iconManager = IconManager.getInstance();
        iconManager.getExternalIcon(R.drawable.fm_unknown);
        iconManager.createExternalIcon(iconManager.getDefaultIcon(R.drawable.fm_unknown));
    }
}
