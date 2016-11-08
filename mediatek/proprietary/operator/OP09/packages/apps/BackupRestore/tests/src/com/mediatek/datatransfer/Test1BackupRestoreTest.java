package com.mediatek.datatransfer;


import android.app.ActionBar;
import android.app.Instrumentation;
import android.app.ListActivity;
import android.app.ActionBar.Tab;
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
    public void test5MountSDCard() {
        Intent mountIntent = new Intent("com.mediatek.autotest.unmount");
        getActivity().sendBroadcast(mountIntent);
        Log.d(TAG, "BackupRestoreTest unmount SDCard");
        sleep(4000);
        mountIntent = new Intent("com.mediatek.autotest.mount");
        getActivity().sendBroadcast(mountIntent);
        Log.d(TAG, "BackupRestoreTest mount SDCard");
        sleep(6000);
    }

    public void test0NOPersonalData() {
        Log.d(TAG, "BackupRestoreTest test1 : test for backup all apps");
        boolean result = false;
        startCurrentActivity(new Intent(activity, PersonalDataBackupActivity.class));
        sleep(2000);
        mSolo.goBack();
    }
    public void test1AppBackup() {
        Log.d(TAG, "BackupRestoreTest test1 : test for backup all apps");
        boolean result = false;
        startCurrentActivity(new Intent(activity, AppBackupActivity.class));
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
        mSolo.clickOnButton(getActivity().getText(R.string.btn_ok).toString());
        Log.d(TAG, "1 : test for backup all app_module finish");
        sleep(500);
        mSolo.goBack();
    }

    public void test2RestoreApp() {
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
        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_DOWN);
        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_DOWN);
        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_CENTER);
        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_CENTER);
        mSolo.clickOnButton(getActivity().getText(R.string.restore).toString());
        sleep(5000);
        mSolo.clickOnButton(getActivity().getText(R.string.btn_ok).toString());
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
        mSolo.clickOnButton(getActivity().getText(R.string.btn_ok).toString());
        sleep(3000);
        mSolo.goBack();
        sleep(1000);
    }


    /**
     * Describe <code>testRestorePersonalData</code> method here.
     * test for restore all module
     */
    public void test3RestorePersonalData() {
        init();
        Log.d(TAG, "BackupRestoreTest : test for restore personaldata");
        boolean result;
        //startCurrentActivity(new Intent(activity,PersonalDataRestoreActivity.class));
        sleep(3000);
        //mSolo.clickOnText(getActivity().getText(R.string.restore).toString());
        this.sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
        this.sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);
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
        mSolo.clickOnButton(getActivity().getText(R.string.btn_ok).toString());
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
        mSolo.clickOnButton(getActivity().getText(R.string.btn_ok).toString());
        sleep(2000);
        mSolo.goBack();
        sleep(500);
    }

    /**
     * Describe <code>testRestorePersonalData</code> method here.
     * test for restore all module
     */

    public void test6Delete() throws Exception {
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
        getInstrumentation().callActivityOnResume(getActivity());
        sleep(1000);
        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_CENTER);
        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_CENTER);
        for (int i = 0; i < 10; i++) {
            mSolo.sendKey(KeyEvent.KEYCODE_DPAD_DOWN);
        }
        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_LEFT);
        sleep(100);
        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_CENTER);
        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_RIGHT);
        sleep(100);
        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_CENTER);

        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_LEFT);
        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_CENTER);

        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_RIGHT);
        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_RIGHT);
        mSolo.sendKey(KeyEvent.KEYCODE_DPAD_RIGHT);
        sleep(100);
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
