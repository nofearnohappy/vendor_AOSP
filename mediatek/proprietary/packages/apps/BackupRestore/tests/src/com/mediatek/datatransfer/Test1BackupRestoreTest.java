package com.mediatek.datatransfer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.app.ActionBar;
import android.app.Instrumentation;
import android.app.ListActivity;
import android.app.ActionBar.Tab;
import android.content.Context;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.ListView;

import com.jayway.android.robotium.solo.Solo;


/**
 * Describe class BackupRestoreTest here.
 *
 *
 * Created: Sun Jul 15 15:01:46 2012
 *
 * @author <a href="mailto:mtk80359@mbjswglx259">Zhanying Liu (MTK80359)</a>
 * @version 1.0
 */
public class Test1BackupRestoreTest extends ActivityInstrumentationTestCase2<MainActivity> {
    private static final String TAG = "BackupRestoreTest";
    private Solo mSolo = null;
    Instrumentation mInstrumentation = null;
    MainActivity activity = null;
    /**
     * Creates a new <code>BackupRestoreTest</code> instance.
     *
     */
    public Test1BackupRestoreTest() {
        super(MainActivity.class);
    }

    /**
     * Describe <code>setUp</code> method here.
     *
     * @exception Exception if an error occurs
     */
    public final void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        mSolo = new Solo(getInstrumentation(), getActivity());
        activity = getActivity();
        Log.d(TAG, "setUp");
    }

    /**
     * Describe <code>tearDown</code> method here.
     *
     * @exception Exception if an error occurs
     */
    public final void tearDown() throws Exception {
        //
//        try {
//            mSolo.finalize();
//        } catch (Throwable t) {
//            t.printStackTrace();
//        }

        if (activity != null) {
            activity.finish();
            activity = null;
        }
        super.tearDown();
        Log.d(TAG, "tearDown");
        sleep(5000);

    }
    public void test6MountSDCard() {
        Intent mountIntent = new Intent("com.mediatek.autotest.unmount");
        getActivity().sendBroadcast(mountIntent);
        Log.d(TAG, "BackupRestoreTest unmount SDCard");
        sleep(4000);
        mountIntent = new Intent("com.mediatek.autotest.mount");
        getActivity().sendBroadcast(mountIntent);
        Log.d(TAG, "BackupRestoreTest mount SDCard");
        sleep(6000);
    }

    public void test1NOPersonalData() {
        Log.d(TAG, "BackupRestoreTest test1 : test for backup all apps");
        boolean result = false;
        startCurrentActivity(new Intent(activity, PersonalDataBackupActivity.class));
        sleep(2000);
        mSolo.goBack();
    }
    public void test2AppBackup() {
        Log.d(TAG, "BackupRestoreTest test1 : test for backup all apps");
        boolean result = false;
        startCurrentActivity(new Intent(activity, AppBackupActivity.class));
        sleep(500);
        ListActivity la = (ListActivity) mSolo.getCurrentActivity();
        while (la.getListAdapter().getCount() == 0) {
            Log.d(TAG, "BackupRestoreTest test1 : sleep = ");
            sleep(1000);
        }
        sleep(500);
        mSolo.clickOnCheckBox(0);
        sleep(500);
        mSolo.clickOnCheckBox(0);
        sleep(1000);

        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_DOWN);
        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_DOWN);
        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_DOWN);
        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_CENTER);
        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_CENTER);
        mSolo.clickOnButton(getActivity().getText(R.string.backuptosd).toString());
        sleep(1000);
        int count = 0;
        while (count++ < 10000) {
            result = mSolo.searchText(getActivity().getText(R.string.backup_result).toString());
            if (result) {
                break;
            }
            sleep(1000);
        }
        sleep(2000);
        mSolo.clickOnButton(getActivity().getText(android.R.string.ok).toString());
        Log.d(TAG, "1 : test for backup all app_module finish");
        sleep(500);
        mSolo.goBack();
    }

    public void test3RestoreApp() {
        init();
        Log.d(TAG, "BackupRestoreTest testRestoreApp : test for restore app");
        boolean result;
        sleep(1000);
        ListView mView = (ListView) mSolo.getView(android.R.id.list);

        result = mSolo.searchText(getActivity().getText(R.string.backup_app_data_preference_title).toString());
        Log.d(TAG, "BackupRestoreTest testRestoreApp : searchText result = " + result);
        assertTrue(result);
        sleep(1000);
        Log.d(TAG, "BackupRestoreTest testRestoreApp : searchText view = " + mView.getChildAt(mView.getChildCount() - 1));
        mSolo.clickOnView(mView.getChildAt(mView.getChildCount() - 1));
        sleep(1000);
        mSolo.clickOnCheckBox(0);
        sleep(500);
        mSolo.clickOnCheckBox(0);
        sleep(1000);
        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_DOWN);
        sleep(500);
        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_DOWN);
        sleep(500);
        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_DOWN);
        sleep(500);
        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_CENTER);
        sleep(500);
        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_CENTER);
        sleep(500);
        mSolo.clickOnButton(getActivity().getText(R.string.restore).toString());
        sleep(2000);
        mSolo.clickOnButton(getActivity().getText(android.R.string.ok).toString());
        sleep(1000);
        int count = 0;
        while (count++ < 10000) {
            result = mSolo.searchText(getActivity().getText(R.string.restore_result).toString());
            if (result) {
                break;
            }
            sleep(1000);
        }
        result = mSolo.searchText(getActivity().getText(R.string.result_success).toString());
        assertTrue(result);
        sleep(2000);
        mSolo.clickOnButton(getActivity().getText(android.R.string.ok).toString());
        sleep(3000);
        mSolo.goBack();
        sleep(1000);
    }

    private void initPreData() {
        try {
            unZip(getInstrumentation().getContext(), AT_PRE_DATA_SRC_PATH, AT_PRE_DATA_PATH);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    public static void unZip(Context context, String assetName,
            String outputDirectory) throws IOException {
        // 创建解压目标目录
        File file = new File(outputDirectory);
        // 如果目标目录不存在，则创建
        if (!file.exists()) {
            file.mkdirs();
        }
        InputStream inputStream = null;
        // 打开压缩文件
        inputStream = context.getAssets().open(assetName);
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        // 读取一个进入点
        ZipEntry zipEntry = zipInputStream.getNextEntry();
        // 使用1Mbuffer
        byte[] buffer = new byte[1024 * 1024];
        // 解压时字节计数
        int count = 0;
        // 如果进入点为空说明已经遍历完所有压缩包中文件和目录
        while (zipEntry != null) {
            // 如果是一个目录
            if (zipEntry.isDirectory()) {
                // String name = zipEntry.getName();
                // name = name.substring(0, name.length() - 1);
                file = new File(outputDirectory + File.separator
                        + zipEntry.getName());
                file.mkdir();
            } else {
                // 如果是文件
                file = new File(outputDirectory + File.separator
                        + zipEntry.getName());
                // 创建该文件
                Log.e(TAG, "getName = " + file.getName());
                file.createNewFile();
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                while ((count = zipInputStream.read(buffer)) > 0) {
                    fileOutputStream.write(buffer, 0, count);
                }
                fileOutputStream.close();
            }
            // 定位到下一个文件入口
            zipEntry = zipInputStream.getNextEntry();
        }
        zipInputStream.close();
    }
    private static final String AT_PRE_DATA_PATH = "storage/sdcard1/backup/Data/AT_pre_data/";
    private static final String AT_PRE_DATA_SRC_PATH = "AT_pre_data.zip";

    /**
     * Describe <code>testRestorePersonalData</code> method here.
     * test for restore all module
     */
    public void test4RestorePersonalData() {
        initPreData();
        sleep(1000);
        init();
        sleep(1000);
        Log.d(TAG, "BackupRestoreTest : test for restore personaldata");
        boolean result;
        Intent intent = new Intent();
        intent.putExtra("filename", AT_PRE_DATA_PATH);
        intent.setClass(getActivity(), PersonalDataRestoreActivity.class);
        startCurrentActivity(intent);
        sleep(1000);
        ListActivity la = (ListActivity) mSolo.getCurrentActivity();
        while (la.getListAdapter().getCount() == 0) {
            Log.d(TAG, "BackupRestoreTest test2 : sleep = ");
            sleep(1000);
        }
        sleep(3000);
        mSolo.clickOnCheckBox(0);
        sleep(500);
        mSolo.clickOnCheckBox(0);
        sleep(1000);
        mSolo.clickOnButton(getActivity().getText(R.string.restore).toString());
        sleep(1000);
        Log.d(TAG, "BackupRestoreTest test2 : clickOnButton restore ");
        mSolo.clickOnButton(getActivity().getText(android.R.string.ok).toString());
        Log.d(TAG, "BackupRestoreTest test2 : clickOnButton restore ");
        sleep(500);
        int count = 0;
        while (count++ < 1000) {
            result = mSolo.searchText(getActivity().getText(R.string.restore_result).toString());
            if (result) {
                break;
            }
            sleep(1000);
        }
        result = mSolo.searchText(getActivity().getText(R.string.result_success).toString());
        assertTrue(result);
        sleep(2000);
        mSolo.clickOnButton(getActivity().getText(android.R.string.ok).toString());
        sleep(2000);
        mSolo.goBack();
        sleep(500);
    }

    /**
     * Describe <code>testRestorePersonalData</code> method here.
     * test for restore all module
     */

    public void test5Delete() throws Exception {
        Log.d(TAG, "BackupRestoreTest testDelete -- PersonalData History");
        init();
        boolean result;
        ListView mView = (ListView) mSolo.getView(android.R.id.list);
        result = mSolo.searchText(getActivity().getText(R.string.backup_app_data_preference_title).toString());
        Log.d(TAG, "BackupRestoreTest testDelete : searchText result = " + result);
        assertTrue(result);
        sleep(1000);
        Log.d(TAG, "BackupRestoreTest testDelete : searchText view = " + mView.getChildAt(mView.getChildCount() - 1));
        mSolo.clickLongOnView(mView.getChildAt(1));
//        getInstrumentation().callActivityOnResume(getActivity());
        sleep(1000);
        mSolo.clickOnView(mView.getChildAt(1));
        sleep(500);
        mSolo.clickOnView(mView.getChildAt(1));
        for (int i = 0; i < 10; i++) {
            mSolo.sendKey(KeyEvent.KEYCODE_DPAD_DOWN);
            sleep(500);
        }

        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_LEFT);
        sleep(500);
        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_CENTER);
        sleep(500);
        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_RIGHT);
        sleep(500);
        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_CENTER);
        sleep(500);
        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_LEFT);
        sleep(500);
        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_CENTER);
        sleep(500);
        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_RIGHT);
        sleep(500);
        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_RIGHT);
        sleep(500);
        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_CENTER);
        sleep(5000);
        mSolo.goBack();
        sleep(1000);

    }
    protected void sendKeysDownUpSync(int... keys) throws InterruptedException {
        for (int key : keys) {
            mInstrumentation.sendKeyDownUpSync(key);
            sleep(1000);
        }
    }
    public void init() {
        ActionBar mActionBar = activity.getActionBar();
        Tab mTab = mActionBar.getTabAt(1);
        Log.d(TAG, "BackupRestoreTest init : mTab " + mTab.getText());
        mSolo.clickOnText(mTab.getText().toString());
        sleep(1000);
    }




    /**
     * Describe startActivity.
     *
     */
    public void startCurrentActivity(Intent i) {
        activity.startActivity(i);
    }

    /**
     * Describe <code>sleep</code> method here.
     *
     * @param time a <code>int</code> value
     */
    public void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
        }
    }

}
