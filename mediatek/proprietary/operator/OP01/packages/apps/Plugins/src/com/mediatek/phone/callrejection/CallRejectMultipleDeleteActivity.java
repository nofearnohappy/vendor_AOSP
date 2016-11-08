package com.mediatek.phone.callrejection;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu.OnMenuItemClickListener;

import com.mediatek.op01.plugin.R;
import com.mediatek.phone.callrejection.CallRejectDropMenu.DropDownMenu;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class CallRejectMultipleDeleteActivity extends Activity implements CallRejectListAdapter.CheckSelectCallBack {
    private static final String TAG = "CallRejectMultipleDeleteActivity";

    private static final Uri URI = Uri.parse("content://reject/list");
    private static final int ID_INDEX = 0;
    private static final int NUMBER_INDEX = 1;
    private static final int NAME_INDEX = 3;

    private static final int CALL_LIST_DIALOG_WAIT = 2;

    private ListView mListView;
    private CallRejectListAdapter mCallRejectListAdapter;

    private static final int MENU_ID_SELECT_ALL = Menu.FIRST;
    private static final int MENU_ID_UNSELECT_ALL = Menu.FIRST + 1;
    private static final int MENU_ID_TRUSH = Menu.FIRST + 2;

    private ArrayList<CallRejectListItem> mCRLItemArray = new ArrayList<CallRejectListItem>();
    private ArrayList<CallRejectListItem> mCRLItemArrayTemp = new ArrayList<CallRejectListItem>();
    private DelContactsTask mDelContactsTask = null;
    private ReadContactsTask mReadContactsTask = null;
    private boolean mIsGetover = false;
    /// New feature: For ALPS00670111 @{
    private DropDownMenu mSelectionMenu;
    /// @}
    private Set<Long> mCheckedIds = new HashSet<Long>();
    private static final String KEY_CHECKEDIDS = "checkedids";
    class ReadContactsTask extends AsyncTask<Integer, Integer, String> {

        @Override
        protected void onPreExecute() {
            showDialog(CALL_LIST_DIALOG_WAIT);
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Integer... params) {
            getCallRejectListItems();
            return "";
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
        }

        @Override
        protected void onPostExecute(String result) {
            if (!this.isCancelled()) {
                mCRLItemArray.clear();
                mCRLItemArray.addAll(mCRLItemArrayTemp);
                for (CallRejectListItem callrejectitem : mCRLItemArrayTemp) {
                    Log.v(TAG, "ReadContactsTask:onPostExecute: name=" + callrejectitem.getName() +
                            " number=" + callrejectitem.getPhoneNum());
                }
                int index = 0;
                for (CallRejectListItem item : mCRLItemArray) {
                    Log.v(TAG, "ReadContactsTask:onPostExecute: name=" + item.getName()
                        + " number=" + item.getPhoneNum()
                        + " mId:" + item.getId() +
                        " ischecked" + item.getIsChecked());
                    if (mCheckedIds.contains(Long.parseLong(item.getId())))item.setIsChecked(true);
                }

                for (CallRejectListItem item : mCRLItemArray) {
                     Log.v(TAG, "ReadContactsTask:onPostExecute1: name=" + item.getName()
                        + " number=" + item.getPhoneNum()
                        + " mId:" + item.getId()
                        + " ischecked" + item.getIsChecked());
                }
                mCallRejectListAdapter.notifyDataSetChanged();
                mIsGetover = true;
                dismissDialog(CALL_LIST_DIALOG_WAIT);
            }
            super.onPostExecute(result);
        }
    }

    class DelContactsTask extends AsyncTask<Integer, String, Integer> {
        @Override
        protected void onPreExecute() {
            showDialog(CALL_LIST_DIALOG_WAIT);
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(Integer... params) {
            int index = params[0];
            int size = params[1];
            while ((index < size) && !isCancelled()) {
                //CallRejectListItem callrejectitem = mCRLItemArray.get(index);
                CallRejectListItem callrejectitem = mCRLItemArrayTemp.get(index);
                if (callrejectitem.getIsChecked()) {
                    String id = callrejectitem.getId();
                    //mCRLItemArray.remove(index);
                    mCRLItemArrayTemp.remove(index);
                    Log.v(TAG, "doInBackground---------------");
                    //publishProgress("fire");
                    //if (isCurTypeVtAndVoice(id)) {
                     //   updateRowById(id);
                    //} else {
                    deleteRowById(id);
                    //}
                    size--;
                } else {
                    index++;
                }
            }
            return size;
        }

        @Override
        protected void onProgressUpdate(String... id) {
            Log.v(TAG, "onProgressUpdate---------------");
            super.onProgressUpdate(id);
            //mCallRejectListAdapter.notifyDataSetChanged();
        }

        @Override
        protected void onPostExecute(Integer size) {
            if (!this.isCancelled()) {
                mCRLItemArray.clear();
                mCRLItemArray.addAll(mCRLItemArrayTemp);
                mCallRejectListAdapter.notifyDataSetChanged();
                dismissDialog(CALL_LIST_DIALOG_WAIT);
                mListView.invalidateViews();
                /*if (size == 0) {
                    CallRejectListModify.this.finish();
                }
                updateSelectedItemsView(getString(R.string.selected_item_count, 0));
                /// JE: For ALPS00688770 @{
                updateOkButton(null);
                /// @}
                */
                CallRejectMultipleDeleteActivity.this.finish();
            }
            super.onPostExecute(size);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.call_reject_list_modify);
        //getCallRejectListItems();
        mListView = (ListView) findViewById(android.R.id.list);
        mCallRejectListAdapter = new CallRejectListAdapter(this, mCRLItemArray);
        if (mListView != null) {
            mListView.setAdapter(mCallRejectListAdapter);
            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Log.v(TAG, "onItemClick-----position:" + position + " id:" + id);
                    CheckBox checkboxView = (CheckBox) view.findViewById(R.id.call_reject_contact_check_btn);
                    if (checkboxView != null) {
                        checkboxView.setChecked(!checkboxView.isChecked());
                        String sid = mCRLItemArray.get(position).getId();
                        if (checkboxView.isChecked()) {

                            mCheckedIds.add(Long.parseLong(sid));
                        } else {
                            mCheckedIds.remove(Long.parseLong(sid));
                        }
                    }
                }
            });
        }
        mCallRejectListAdapter.setCheckSelectCallBack(this);
        configureActionBar();
        Log.v(TAG, "onCreate: mCheckedIds.size():" + mCheckedIds.size());
        updateSelectedItemsView(getString(R.string.selected_item_count, mCheckedIds.size()));
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == CALL_LIST_DIALOG_WAIT) {
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage(getResources().getString(R.string.call_reject_please_wait));
            dialog.setCancelable(false);
            dialog.setIndeterminate(true);
            return dialog;
        }
        return null;
    }

    @Override
    public void onResume() {
        Log.v(TAG, "onResume---------------mIsGetover:" + mIsGetover);
        super.onResume();
        if ((mDelContactsTask != null) && (mDelContactsTask.getStatus() == AsyncTask.Status.RUNNING)) {
            Log.v(TAG, "onResume-------no update again--------");
        } else if (!mIsGetover) {
            mCRLItemArray.clear();
            mCRLItemArrayTemp.clear();
            mReadContactsTask = new ReadContactsTask();
            mReadContactsTask.execute(0, 0);
            Log.v(TAG, "onResume---------------");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDelContactsTask != null) {
            mDelContactsTask.cancel(true);
        }
        if (mReadContactsTask != null) {
            mReadContactsTask.cancel(true);
        }
    }

    private void getCallRejectListItems() {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(URI, new String[] {
                "_id", "Number"}, null, null, null);

            if (cursor == null) {
                return;
            }
            cursor.moveToFirst();
            mCRLItemArrayTemp.clear();
            while (!cursor.isAfterLast()) {
                String id = cursor.getString(ID_INDEX);
                String numberDB = cursor.getString(NUMBER_INDEX);
                String name = CallRejectUtils.getContactsName(this, numberDB);
                Log.d(TAG, "id=" + id);
                Log.d(TAG, "numberDB=" + numberDB);
                Log.d(TAG, "name=" + name);
                CallRejectListItem crli = new CallRejectListItem(name, numberDB, id, false);
                //mCRLItemArray.add(crli);
                mCRLItemArrayTemp.add(crli);
                cursor.moveToNext();
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void selectAll() {
        for (CallRejectListItem callrejectitem : mCRLItemArrayTemp) {
            callrejectitem.setIsChecked(true);
            mCheckedIds.add(Long.parseLong(callrejectitem.getId()));
            Log.i(TAG, "Enter deleteSecection Function:" + callrejectitem.getId());
        }
        updateSelectedItemsView(getString(R.string.selected_item_count, mCRLItemArrayTemp.size()));
        mListView.invalidateViews();
    }

    private void unSelectAll() {
        for (CallRejectListItem callrejectitem : mCRLItemArrayTemp) {
            callrejectitem.setIsChecked(false);
        }
        mCheckedIds.clear();
        updateSelectedItemsView(getString(R.string.selected_item_count, 0));
        mListView.invalidateViews();
    }

    private void deleteSelection() {
        Log.i(TAG, "Enter deleteSecection Function");
        boolean isSelected = false;
        for (CallRejectListItem callrejectitem : mCRLItemArrayTemp) {
            isSelected |= callrejectitem.getIsChecked();
        }
        if (isSelected) {
            mDelContactsTask  = new DelContactsTask();
            mCRLItemArrayTemp.clear();
            mCRLItemArrayTemp.addAll(mCRLItemArray);
            mDelContactsTask.execute(0, mCRLItemArray.size());
        }
    }

    private void updateRowById(String id) {
        try {
            Uri existNumberURI = ContentUris.withAppendedId(URI, Integer.parseInt(id));
            int result = getContentResolver().update(existNumberURI, null, null, null);
            Log.i(TAG, "result is " + result);
        } catch (NumberFormatException e) {
            Log.e(TAG, "parseInt failed, the index is " + id);
        }
    }

    private void deleteRowById(String id) {
        try {
            Uri existNumberURI = ContentUris.withAppendedId(URI, Integer.parseInt(id));
            Log.i(TAG, "existNumberURI is " + existNumberURI);
            int result = getContentResolver().delete(existNumberURI, null, null);
            Log.i(TAG, "result is " + result);
        } catch (NumberFormatException e) {
            Log.e(TAG, "parseInt failed, the index is " + id);
        }
    }

    /// New feature: For ALPS0000670111 @{
    protected OnClickListener getClickListenerOfActionBarOKButton() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteSelection();
                Log.v(TAG, "configureActionBar, delete");
                return;
            }
        };
    }

    /**
     * add dropDown menu on the selectItems.The menu is "Select all" or "Deselect all"
     * @param customActionBarView
     */
    public void updateSelectionMenu(View customActionBarView) {
        CallRejectDropMenu dropMenu = new CallRejectDropMenu(this);
        // new and add a menu.
        mSelectionMenu = dropMenu.addDropDownMenu((Button) customActionBarView
                .findViewById(R.id.select_items), R.menu.selection);
        // new and add a menu.
        Button selectView = (Button) customActionBarView
                .findViewById(R.id.select_items);
        //selectView.setBackgroundDrawable(getResources().getDrawable(R.drawable.dropdown_normal_holo_dark));
        selectView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                View parent = (View) v.getParent();
                updateSelectionMenu(parent);
                mSelectionMenu.show();
                return;
            }
        });
        MenuItem item = mSelectionMenu.findItem(R.id.action_select_all);
        int count = 0;
        for (CallRejectListItem callrejectitem : mCRLItemArrayTemp) {
            if (callrejectitem.getIsChecked()) {
            count++;
            }
        }
        boolean mIsSelectedAll = mCRLItemArrayTemp.size() == count;
        // if select all items, the menu is "Deselect all"; else the menu is "Select all".
        if (mIsSelectedAll) {
            Log.e(TAG, "mIsSelectedAll:" + mIsSelectedAll + "select none string:" + getString(R.string.menu_select_none));
            item.setChecked(true);
            item.setTitle(R.string.menu_select_none);
            // click the menu, deselect all items
            dropMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    Log.e(TAG, "select none click");
                    unSelectAll();
                    return false;
                }
            });
        } else {
            Log.e(TAG, "mIsSelectedAll:" + mIsSelectedAll + "select all string:" + getString(R.string.menu_select_all));
            item.setChecked(false);
            item.setTitle(R.string.menu_select_all);
            //click the menu, select all items.
            dropMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    Log.e(TAG, "select all click");
                    selectAll();
                    return false;
                }
            });
        }
        return;
    }

    /**
     * when the selected item changed, update the show count"
     * return the count
     */
    public int updateOkButton(Button okButton) {
        Log.e(TAG, "updateOkButton");
        int count = 0;
        for (CallRejectListItem callrejectitem : mCRLItemArrayTemp) {
            if (callrejectitem.getIsChecked()) {
                count++;
            }
        }
        /// JE: For ALPS00688770 @{
        Button deleteView = null;
        if (okButton == null) {
            Log.v(TAG, "updateOkButton, okButton is null, reload again");
            deleteView = (Button) getActionBar().getCustomView().findViewById(R.id.delete);
        } else {
            Log.v(TAG, "updateOkButton, okButton is not null");
            deleteView = okButton;
        }
        /// @}
        Log.v(TAG, "updateOkButton, checked count= " + count);
        if (count == 0) {
            // if there is no item selected, the "OK" button is disable.
            deleteView.setEnabled(false);
            deleteView.setTextColor(Color.GRAY);
        } else {
            deleteView.setEnabled(true);
            deleteView.setTextColor(Color.WHITE);
        }
        return count;
    }

    /// @}
    private void updateSelectedItemsView(String checkedItemsCount) {
        Button selectedItemsView = (Button) getActionBar().getCustomView().findViewById(R.id.select_items);
        if (selectedItemsView == null) {
            return;
        }
        selectedItemsView.setText(checkedItemsCount);
    }

    private void configureActionBar() {
        Log.v(TAG, "configureActionBar() mIsGetover:" + mIsGetover);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View customActionBarView = inflater.inflate(R.layout.call_reject_list_modify_action_bar, null);
        /// New feature: For ALPS0000670111 @{
        //dispaly the "select_items" .
        Button selectView = (Button) customActionBarView
                .findViewById(R.id.select_items);
        //selectView.setBackgroundDrawable(getResources().getDrawable(R.drawable.dropdown_normal_holo_dark));
        selectView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                View parent = (View) v.getParent();
                updateSelectionMenu(parent);
                mSelectionMenu.show();
                Log.v(TAG, "configureActionBar, tophome");
                return;
            }
        });

        //dispaly the "OK" button.
        Button deleteView = (Button) customActionBarView
                .findViewById(R.id.delete);
        updateOkButton(deleteView);
        deleteView.setOnClickListener(getClickListenerOfActionBarOKButton());
        /// @}
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
                    | ActionBar.DISPLAY_SHOW_TITLE);
            actionBar.setCustomView(customActionBarView);
            //actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
            //actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void setChecked(boolean isChecked) {
        int count = 0;
        /// New feature: For ALPS0000670111 @{
        count = updateOkButton(null);
        /// @}
        updateSelectedItemsView(getString(R.string.selected_item_count, count));
    }

    @Override
    public void onRestoreInstanceState(Bundle savedState) {
        super.onRestoreInstanceState(savedState);

        if (savedState == null) {
            Log.v(TAG, "[restoreSavedState]saved state is null");
            return;
        }

        if (mCheckedIds == null) {
            mCheckedIds = new HashSet<Long>();
        }
        mCheckedIds.clear();

        long[] ids = savedState.getLongArray(KEY_CHECKEDIDS);
        int checkedItemSize = ids.length;
        Log.v(TAG, "[restoreSavedState]restore " + checkedItemSize + " ids");
        for (int index = 0; index < checkedItemSize; ++index) {
            mCheckedIds.add(Long.valueOf(ids[index]));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        final int checkedItemsCount = mCheckedIds.size();
        long[] checkedIds = new long[checkedItemsCount];
        int index = 0;
        for (Long id : mCheckedIds) {
            checkedIds[index++] = id;
        }
        Log.v(TAG, "[onSaveInstanceState]save " + checkedIds.length + " ids");
        outState.putLongArray(KEY_CHECKEDIDS, checkedIds);
    }
}
