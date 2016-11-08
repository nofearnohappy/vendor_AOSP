package com.mediatek.contacts.plugin;

import com.mediatek.contacts.aas.plugin.AasTagActivity;
import com.mediatek.contacts.aas.plugin.AasTagInfoAdapter;
import com.mediatek.contacts.aas.plugin.LogUtils;
import com.mediatek.contacts.aas.plugin.SimUtils;
import com.mediatek.contacts.aas.plugin.MessageAlertDialogFragment;
import com.mediatek.contacts.aas.plugin.AlertDialogFragment.EditTextDialogFragment;
import com.mediatek.op03.plugin.R;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.test.InstrumentationTestCase;
import android.test.TouchUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

public class AasTagActivityTest extends ActivityInstrumentationTestCase2<AasTagActivity> {
    private static final String TAG = "AasTagActivityTest";

    private static final String TAG_IVALID_NAME = "!@@#!@@*&^%%&jdkaladknv-=11a;(((&^%%$#^^&&&91827aaakdhcnlso;s";

    private static final int USIM_SLOT = 0;
    private static final String CREATE_AAS_TAG_DIALOG = "create_aas_tag_dialog";
    private static final String EDIT_AAS_NAME = "edit_aas_name";
    private static final String DELETE_TAG_DIALOG = "delet_tag_dialog";
    private static final String EDIT_TAG_DIALOG = "edit_tag_dialog";

    private AasTagActivity mActivity = null;

    public AasTagActivityTest() {
        super(AasTagActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction("com.android.contacts.action.EDIT_AAS");
        intent.putExtra(SimUtils.KEY_SLOT, USIM_SLOT);
        setActivityIntent(intent);
    }

    @Override
    protected void tearDown() throws Exception {
        if (mActivity != null) {
            mActivity.finish();
            mActivity = null;
        }
        super.tearDown();
    }

    public void testLaunchWithErrorSlot() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction("com.android.contacts.action.EDIT_AAS");
        intent.putExtra(SimUtils.KEY_SLOT, -1);
        setActivityIntent(intent);
        mActivity = getActivity();
    }

    public void testBackKey() {
        mActivity = getActivity();
        sleep(500);
        ListView listView = (ListView) mActivity.findViewById(R.id.custom_aas);
        AasTagInfoAdapter aasAdapter = (AasTagInfoAdapter) listView.getAdapter();
        if (aasAdapter.isMode(AasTagInfoAdapter.MODE_NORMAL)) {
            changeToMode(AasTagInfoAdapter.MODE_EDIT, listView, aasAdapter);
        }
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        sleep(500);
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        sleep(300);
    }

    /**
     * Test Add/Modify/Delete Operations
     */
    public void testAddTag() {
        mActivity = getActivity();
        sleep(500);

        ListView listView = (ListView) mActivity.findViewById(R.id.custom_aas);
        AasTagInfoAdapter aasAdapter = (AasTagInfoAdapter) listView.getAdapter();
        if (aasAdapter.isFull()) {
            LogUtils.w(TAG, "AAS TAG is full, delete a tag for testing.");
            deleteTag(listView, aasAdapter);
        }
        int count = aasAdapter.getCount();
        createNewTag(TAG_IVALID_NAME);
        assertEquals(count, aasAdapter.getCount());
        createNewTag("Tag1");
    }

    private void createNewTag(String newTag) {
        View aasAdd = mActivity.findViewById(R.id.menu_add_new);
        TouchUtils.clickView(this, aasAdd);
        AlertDialog dialog = getDialog(mActivity, CREATE_AAS_TAG_DIALOG);
        final EditText editText = (EditText) dialog.findViewById(R.id.edit_text);
        runUiThread(this, new EditTextRunnable(editText, newTag));

        TouchUtils.clickView(this, dialog.getButton(DialogInterface.BUTTON_POSITIVE));
        sleep(500);
    }

    private void deleteTag(ListView listView, AasTagInfoAdapter aasAdapter) {
        changeToMode(AasTagInfoAdapter.MODE_EDIT, listView, aasAdapter);
        if (aasAdapter.getCount() > 0) {
            View view = listView.getChildAt(0);
            TouchUtils.clickView(this, view);
            sleep(500);

            View delect = mActivity.findViewById(R.id.menu_delete);
            TouchUtils.clickView(this, delect);

            AlertDialog dialog = getDeleteDialog(mActivity, DELETE_TAG_DIALOG);
            TouchUtils.clickView(this, dialog.getButton(DialogInterface.BUTTON_POSITIVE));
            sleep(500);
        } else {
            LogUtils.w(TAG, "deleteTag() failed, no tag");
        }

        changeToMode(AasTagInfoAdapter.MODE_NORMAL, listView, aasAdapter);
    }

    private void changeToMode(int mode, ListView listView, AasTagInfoAdapter aasAdapter) {
        if (aasAdapter.isMode(mode)) {
            return;
        }

        if (aasAdapter.isMode(AasTagInfoAdapter.MODE_NORMAL) && AasTagInfoAdapter.MODE_EDIT == mode) {
            View view = mActivity.findViewById(R.id.menu_deletion);
            TouchUtils.clickView(this, view);
        } else {
            getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        }

        int i = 0;
        while (!aasAdapter.isMode(mode) && i < 50) {
            sleep(100);
            i++;
            LogUtils.w(TAG, "changeToMode() waiting for the click:" + mode);
        }
        if (!aasAdapter.isMode(mode)) {
            getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
            sleep(300);
            assertTrue(aasAdapter.isMode(mode));
        }
    }

    public void testEditTag() {
        mActivity = getActivity();
        sleep(500);

        ListView listView = (ListView) mActivity.findViewById(R.id.custom_aas);
        AasTagInfoAdapter aasAdapter = (AasTagInfoAdapter) listView.getAdapter();
        if (aasAdapter.getCount() == 0) {
            createNewTag("ForTest");
            LogUtils.w(TAG, "AAS TAG is full, delete a tag for testing.");
        }
        if (aasAdapter.getCount() > 0) {
            editTag(listView.getChildAt(0), TAG_IVALID_NAME);
            editTag(listView.getChildAt(0), "edit01");
        } else {
            LogUtils.e(TAG, "ERROR, AAS TAG IS EMPTY.");
        }
    }

    private void editTag(View view, String newTag) {
        TouchUtils.longClickView(this, view);

        AlertDialog dialog = getDialog(mActivity, EDIT_AAS_NAME);

        final EditText editText = (EditText) dialog.findViewById(R.id.edit_text);
        runUiThread(this, new EditTextRunnable(editText, newTag));

        TouchUtils.clickView(this, dialog.getButton(DialogInterface.BUTTON_POSITIVE));
        sleep(500);
    }

    public void testDeleteTag() {
        LogUtils.d(TAG, "testDeleteTag");
        mActivity = getActivity();
        sleep(500);

        ListView listView = (ListView) mActivity.findViewById(R.id.custom_aas);
        AasTagInfoAdapter aasAdapter = (AasTagInfoAdapter) listView.getAdapter();

        LogUtils.e(TAG, "testDeleteTag-QQQQQQQ");
        changeToMode(AasTagInfoAdapter.MODE_EDIT, listView, aasAdapter);
        LogUtils.e(TAG, "testDeleteTag-aaaaaaaaaa");
        assertTrue(aasAdapter.isMode(AasTagInfoAdapter.MODE_EDIT));

        // to delete without select
        View delect = mActivity.findViewById(R.id.menu_delete);
        TouchUtils.clickView(this, delect);
        sleep(500);
        // selsect all
        View selectAll = mActivity.findViewById(R.id.menu_select_all);
        TouchUtils.clickView(this, selectAll);
        sleep(500);
        // diselect all
        View diselectAll = mActivity.findViewById(R.id.menu_disselect_all);
        TouchUtils.clickView(this, diselectAll);
        sleep(500);

        LogUtils.w(TAG, "testDeleteTag(): aas count=" + aasAdapter.getCount());
        if (aasAdapter.getCount() > 0) {
            deleteTag(listView, aasAdapter);
        }
    }

    private static final void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static AlertDialog getDialog(AasTagActivity activity, String tag) {
        int time = 0;
        EditTextDialogFragment createItemDialogFragment = null;
        do {
            createItemDialogFragment = (EditTextDialogFragment) activity.getFragmentManager().findFragmentByTag(tag);
            sleep(100);
            time++;
            LogUtils.d(TAG, "waiting to getDialog");
        } while ((createItemDialogFragment == null) && (time < 100));
        return (AlertDialog) createItemDialogFragment.getDialog();
    }

    private static AlertDialog getDeleteDialog(AasTagActivity activity, String tag) {
        int time = 0;
        MessageAlertDialogFragment alertFragment = null;
        do {
            alertFragment = (MessageAlertDialogFragment) activity.getFragmentManager().findFragmentByTag(tag);
            sleep(100);
            time++;
            LogUtils.d(TAG, "waiting to getDialog");
        } while ((alertFragment == null) && (time < 100));
        return (AlertDialog) alertFragment.getDialog();
    }

    private static void runUiThread(InstrumentationTestCase testCase, EditTextRunnable runnable) {
        try {
            testCase.runTestOnUiThread(runnable);
            while (runnable.flag) {
                sleep(100);
                LogUtils.w(TAG, "runUiThread waiting for Runner ");
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        sleep(200);
    }

    private static class EditTextRunnable implements Runnable {
        boolean flag = false;

        EditText mEditText;
        String mText;

        public EditTextRunnable(EditText editText, String text) {
            mEditText = editText;
            mText = text;
            flag = true;
        }

        @Override
        public void run() {
            mEditText.setText(mText);
            flag = false;
        }
    }
}
