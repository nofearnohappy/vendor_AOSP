package com.mediatek.phone.callrejection;

import java.util.ArrayList;

import android.app.ActionBar;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.mediatek.op01.plugin.R;

public class CallRejectActivity extends PreferenceActivity implements
    Button.OnClickListener, OnItemClickListener {

    private static final String LOG_TAG = "CallRejectActivity";
    private static final int CALL_LIST_DIALOG_EDIT = 0;
    private static final int CALL_LIST_DIALOG_SELECT = 1;
    private static final int CALL_LIST_DIALOG_WAIT = 2;

    private static final int ID_INDEX = 0;
    private static final int NUMBER_INDEX = 1;
    private static final int TYPE_INDEX = 2;

    private static final Uri CALLREJECT_URI = Uri.parse("content://reject/list");
    private static final Uri CONTACT_URI = Data.CONTENT_URI;
    private static final Uri CALLLOG_URI = Uri.parse("content://call_log/calls");

    private static final String CONTACTS_ADD_ACTION = "android.intent.action.contacts.list.PICKMULTIPHONES";
    private static final String CONTACTS_ADD_ACTION_RESULT = "com.mediatek.contacts.list.pickdataresult";
    private static final String CALL_LOG_SEARCH = "android.intent.action.SEARCH";

    /// M: For ALPS01188072, fro ex, B phone fire "hide call id" funtion,
    ///the calllog realy savenumber is -2, and show"private number"@{
    private static final String CALL_LOG_HIDE_NUMBER = "-2";
    private static final String CALL_LOG_SPACE_NUMBER = "-1";
    /// @}

    private static final int CALL_REJECT_CONTACTS_REQUEST = 125;
    private static final int CALL_REJECT_LOG_REQUEST = 126;

    private static final int MENU_ID_DELETE = Menu.FIRST;
    private static final int MENU_ID_ADD = Menu.FIRST + 2;
    private static final int PHONE_NUMBER_COLUMN = 1;

    private static final String[] CALLER_ID_PROJECTION = new String[] {
        Phone._ID,                      // 0
        Phone.NUMBER,                   // 1
        Phone.LABEL,                    // 2
        Phone.DISPLAY_NAME,             // 3
    };

    public static final String[] CALL_LOG_PROJECTION = new String[] {
        Calls._ID,                       // 0
        Calls.NUMBER,                    // 1
        Calls.DATE,                      // 2
        Calls.DURATION,                  // 3
        Calls.TYPE,                      // 4
        Calls.COUNTRY_ISO,               // 5
        Calls.VOICEMAIL_URI,             // 6
        Calls.GEOCODED_LOCATION,         // 7
        Calls.CACHED_NAME,               // 8
        Calls.CACHED_NUMBER_TYPE,        // 9
        Calls.CACHED_NUMBER_LABEL,       // 10
        Calls.CACHED_LOOKUP_URI,         // 11
        Calls.CACHED_MATCHED_NUMBER,     // 12
        Calls.CACHED_NORMALIZED_NUMBER,  // 13
        Calls.CACHED_PHOTO_ID,           // 14
        Calls.CACHED_FORMATTED_NUMBER,   // 15
    };

    public static final int ID = 0;
    public static final int NUMBER = 1;
    public static final int DATE = 2;
    public static final int DURATION = 3;
    public static final int CALL_TYPE = 4;
    public static final int COUNTRY_ISO = 5;
    public static final int VOICEMAIL_URI = 6;
    public static final int GEOCODED_LOCATION = 7;
    public static final int CACHED_NAME = 8;
    public static final int CACHED_NUMBER_TYPE = 9;
    public static final int CACHED_NUMBER_LABEL = 10;
    public static final int CACHED_LOOKUP_URI = 11;
    public static final int CACHED_MATCHED_NUMBER = 12;
    public static final int CACHED_NORMALIZED_NUMBER = 13;
    public static final int CACHED_PHOTO_ID = 14;
    public static final int CACHED_FORMATTED_NUMBER = 15;

    private ListView mListView;
    private Button mDialogSaveBtn;
    private Button mDialogCancelBtn;
    private ImageButton mAddContactsBtn;
    private EditText mNumberEditText;

    private Intent mResultIntent;
    private ArrayList<String> mRejectNumbers = new ArrayList<String>();
    private ArrayList<String> mRejectNames = new ArrayList<String>();
    private PreferenceScreen mPreferenceScreen;
    private boolean mNeedQuery = false;
    private static final int REJECT_LIST_FULL = 100;

    private MyHandler mHandler = new MyHandler(this);

    private class MyHandler extends Handler {

        static final int MESSAGE_FULL = 0;
        private Context mContext = null;

        MyHandler(Context context){
            mContext = context;
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_FULL:
                    Toast.makeText(mContext, mContext.getResources().getString(R.string.reject_list_full), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.call_reject_list);

        mPreferenceScreen = getPreferenceManager().createPreferenceScreen(this);
        setPreferenceScreen(mPreferenceScreen);

        mListView = (ListView) findViewById(R.id.list);

        setTitle(getResources().getString(R.string.call_reject_list_title));
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        getContentResolver().registerContentObserver(CONTACT_URI, true, mContactsObserver);
        //queryDisplayNumbers();
        mNeedQuery = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        log("onResume----mNeedQuery=" + mNeedQuery);
        if ((mAddContactsCallLogTask != null) && (mAddContactsCallLogTask.getStatus() == AsyncTask.Status.RUNNING)) {
            log("onResume-------no update again--------");
        } else if (mNeedQuery) {
            updataCallRejectListView();
        }
        mNeedQuery = false;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstatanceState) {
        super.onSaveInstanceState(savedInstatanceState);
        String numberKey = "rejectNumbers";
        String nameKey = "rejectNames";
        savedInstatanceState.putStringArrayList(numberKey, mRejectNumbers);
        savedInstatanceState.putStringArrayList(nameKey, mRejectNames);
        log("onSaveInstanceState mRejectNumbers size: " + mRejectNumbers.size());
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstatanceState) {
        super.onRestoreInstanceState(savedInstatanceState);
        String numberKey = "rejectNumbers";
        String nameKey = "rejectNames";
        mRejectNumbers = savedInstatanceState.getStringArrayList(numberKey);
        mRejectNames = savedInstatanceState.getStringArrayList(nameKey);
        log("onRestoreInstanceState mRejectNumbers size: " + mRejectNumbers.size());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log("onDestroy");
        if (mAddContactsCallLogTask != null) {
            mAddContactsCallLogTask.cancel(true);
        }
        if (mUpdateCallRejectTask != null) {
            mUpdateCallRejectTask.cancel(true);
        }
        //mRejectNumbers.clear();
        //mRejectNames.clear();
        getContentResolver().unregisterContentObserver(mContactsObserver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_ID_DELETE, 0, R.string.call_reject_list_delete);
        menu.add(Menu.NONE, MENU_ID_ADD, 1, R.string.call_reject_list_add);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        int count = getPreferenceScreen().getPreferenceCount();
        log("[preference count=" + count + "]");
        menu.getItem(0).setVisible(count != 0);
        menu.getItem(1).setEnabled(true);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ID_ADD:
            showDialog(CALL_LIST_DIALOG_EDIT);
            break;
        case MENU_ID_DELETE:
            Intent it = new Intent(this, CallRejectMultipleDeleteActivity.class);
            mNeedQuery = true;
            startActivity(it);
            break;
        case android.R.id.home:
            finish();
            return true;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == CALL_LIST_DIALOG_EDIT) {
            Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.call_reject_dialog);
            dialog.setTitle(getResources().getString(R.string.add_call_reject_number));
            log("--------------[MYREJECT:0000---------------");
            mAddContactsBtn = (ImageButton) dialog.findViewById(R.id.select_contact);
            if (mAddContactsBtn != null) {
                mAddContactsBtn.setOnClickListener(this);
            }

            mDialogSaveBtn = (Button) dialog.findViewById(R.id.save);
            if (mDialogSaveBtn != null) {
                mDialogSaveBtn.setOnClickListener(this);
            }

            mDialogCancelBtn = (Button) dialog.findViewById(R.id.cancel);
            if (mDialogCancelBtn != null) {
                mDialogCancelBtn.setOnClickListener(this);
            }
            mNumberEditText = (EditText) dialog.findViewById(R.id.EditNumber);
            //set EditText min width
            WindowManager wm = (WindowManager) this.getWindowManager(); 
            int minWidth = wm.getDefaultDisplay().getWidth()/2;
            mNumberEditText.setMinimumWidth(minWidth);
            return dialog;
        } else if (id == CALL_LIST_DIALOG_SELECT) {
            Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.call_reject_dialog_contact);
            dialog.setTitle(getResources().getString(R.string.select_from));
            ListView listview = (ListView) dialog.findViewById(R.id.list);
            listview.setOnItemClickListener(this);
            return dialog;
        } else if (id == CALL_LIST_DIALOG_WAIT) {
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage(getResources().getString(R.string.call_reject_please_wait));
            dialog.setCancelable(false);
            dialog.setIndeterminate(true);
            return dialog;
        }
        return null;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        switch(id) {
        case CALL_LIST_DIALOG_EDIT:
            if (mNumberEditText != null) {
                mNumberEditText.setText("");
            }
            break;
        default:
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mAddContactsBtn) {
            dismissDialog(CALL_LIST_DIALOG_EDIT);
            showDialog(CALL_LIST_DIALOG_SELECT);
        } else if (v == mDialogSaveBtn) {
            dismissDialog(CALL_LIST_DIALOG_EDIT);
            if (mRejectNumbers.size() >= REJECT_LIST_FULL) {
                log("self add is full");
                Toast.makeText(this, R.string.reject_list_full, Toast.LENGTH_SHORT).show();
                return;
            }
            if (mNumberEditText == null
                || mNumberEditText.getText().toString().isEmpty()) {
                return;
            }
            String rejectNumber = CallRejectUtils.allWhite(mNumberEditText.getText().toString());
            String rejectName = CallRejectUtils.getContactsName(this, rejectNumber);
            showDialog(CALL_LIST_DIALOG_WAIT);
            if (insertNumber(rejectNumber, true)) {
                mRejectNumbers.add(rejectNumber);
                mRejectNames.add(rejectName);
                addNumberOnPreference(rejectName, rejectNumber);
            }
            dismissDialog(CALL_LIST_DIALOG_WAIT);
        } else if (v == mDialogCancelBtn) {
            dismissDialog(CALL_LIST_DIALOG_EDIT);
        }
    }

    @Override
    public void onItemClick(AdapterView<?>arg0, View arg1, int arg2, long arg3) {
        log("onItemClick:arg2=" + arg2 + " arg2=" + arg3);
        if (arg2 == 0) {
            Intent intent = new Intent(CONTACTS_ADD_ACTION);
            intent.setType(Phone.CONTENT_TYPE);
            try {
                startActivityForResult(intent, CALL_REJECT_CONTACTS_REQUEST);
                dismissDialog(CALL_LIST_DIALOG_SELECT);
            } catch (ActivityNotFoundException e) {
                log(e.toString());
            }
        } else if (arg2 == 1) {
            Intent intent = new Intent();
            intent.setClassName("com.android.dialer",
                        "com.mediatek.dialer.activities.CallLogMultipleChoiceActivity");
            final String CALL_LOG_TYPE_FILTER = "call_log_type_filter";
            final int CALL_TYPE_ALL = -1;
            intent.putExtra(CALL_LOG_TYPE_FILTER, CALL_TYPE_ALL);
            try {
                startActivityForResult(intent, CALL_REJECT_LOG_REQUEST);
                dismissDialog(CALL_LIST_DIALOG_SELECT);
            } catch (ActivityNotFoundException e) {
                log(e.toString());
            }
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        mAddContactsCallLogTask = new AddContactsCallLogTask();
        mResultIntent = data;
        mAddContactsCallLogTask.execute(requestCode, resultCode);
    }

    private UpdateCallRejectTask mUpdateCallRejectTask = null;
    /*sync the name begine*/
    class UpdateCallRejectTask extends AsyncTask<Integer, Integer, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showDialog(CALL_LIST_DIALOG_WAIT);
        }

        @Override
        protected String doInBackground(Integer... params) {
            log("UpdateCallRejectTask-----doInBackground");
            queryCallRejectNumbers();
            return "";
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            log("UpdateCallRejectTask-----onPostExecute");
            displayCallRejectNumbers();
            dismissDialogSafely(CALL_LIST_DIALOG_WAIT);
            invalidateOptionsMenu();
        }

        @Override
        protected void onCancelled(String result) {
            super.onCancelled(result);
        }
    }

    private AddContactsCallLogTask mAddContactsCallLogTask = null;
    class AddContactsCallLogTask extends AsyncTask<Integer, Integer, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showDialog(CALL_LIST_DIALOG_WAIT);
        }

        @Override
        protected String doInBackground(Integer... params) {
            log("AddContactsCallLogTask-----doInBackground");
            addCallRejectNumbers(params[0], params[1], mResultIntent);
            return "";
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            log("AddContactsCallLogTask-----onPostExecute");
            displayCallRejectNumbers();
            dismissDialogSafely(CALL_LIST_DIALOG_WAIT);
            invalidateOptionsMenu();
        }

        @Override
        protected void onCancelled(String result) {
            super.onCancelled(result);
        }
    }

    private void queryDisplayNumbers() {
        mUpdateCallRejectTask = new UpdateCallRejectTask();
        mUpdateCallRejectTask.execute(RESULT_OK, RESULT_OK);
    }

    private void queryCallRejectNumbers() {
        mPreferenceScreen.removeAll();
        Cursor cursor = getContentResolver().query(CALLREJECT_URI, new String[] {"_id", "Number"}, null, null, null);
        if (cursor == null) return;
        cursor.moveToFirst();
        try {
            while (!cursor.isAfterLast()) {
                String number = cursor.getString(NUMBER_INDEX);
                String name = "";
                log("queryCallRejectNumbers rejectDB..number:" + number + " name:" + name);
                if (number == null || number.isEmpty()) {
                    log("queryCallRejectNumbers--number is null");
                } else {
                    log("queryCallRejectNumbers--number: " + number);
                    mRejectNumbers.add(number);
                }
                cursor.moveToNext();
            }
        } finally {
            cursor.close();
        }
        mRejectNames = CallRejectUtils.getContactsNames(this, mRejectNumbers);
    }

    private void displayCallRejectNumbers() {
        // remove all preferenceScreen item
        if(mPreferenceScreen.getPreferenceCount() > 0) {
            mPreferenceScreen.removeAll();
        }
        if (mRejectNumbers.size() > 0) {
            int nameSize = mRejectNames.size();
            for (int i = 0; i < mRejectNumbers.size(); i++) {
                log("mRejectNumbers[" + i + "]: " + mRejectNumbers.get(i));
                if(i < nameSize){
                    addNumberOnPreference(mRejectNames.get(i), mRejectNumbers.get(i));
                }else {
                    addNumberOnPreference(this.getResources().getString(R.string.call_reject_no_name), mRejectNumbers.get(i));
                }
            }
        }
    }

    private void addCallRejectNumbers(int requestCode, int resultCode, Intent data) {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        ArrayList<String> contactCallLogNumbers = new ArrayList<String>();
        switch(resultCode) {
            case RESULT_OK:
                if (requestCode == CALL_REJECT_CONTACTS_REQUEST) {
                    final long[] contactId = data.getLongArrayExtra(CONTACTS_ADD_ACTION_RESULT);
                    if (contactId == null || contactId.length < 0) {
                        break;
                    }
                    for (int i = 0; i < contactId.length && !mAddContactsCallLogTask.isCancelled(); i++) {
                        getContactsNumbers((int) contactId[i], contactCallLogNumbers);
                    }
                } else if (requestCode == CALL_REJECT_LOG_REQUEST) {
                    final String callLogId = data.getStringExtra("calllogids");
                    log("callLogId: " + callLogId);
                    if (callLogId == null || callLogId.isEmpty()) return;
                    if (!callLogId.startsWith("_id")) return;
                    getCallLogNumbers(callLogId, contactCallLogNumbers);
                }
                break;
            default:
                break;
        }

        removeDuplicateAndOutOfRangeNumbers(contactCallLogNumbers);

        if (contactCallLogNumbers.size() > 0) {
            for (int l = 0; l < contactCallLogNumbers.size(); l++) {
                insertNumber(contactCallLogNumbers.get(l), false);
            }
            mRejectNumbers.addAll(contactCallLogNumbers);
            ArrayList<String> phoneNames = CallRejectUtils.getContactsNames(this, contactCallLogNumbers);
            mRejectNames.addAll(phoneNames);
        }
    }

    private void getContactsNumbers(int id, ArrayList<String> numbers) {
        Uri existNumberURI = ContentUris.withAppendedId(CONTACT_URI, id);
        Cursor cursor = getContentResolver().query(existNumberURI, CALLER_ID_PROJECTION, null, null, null);
        cursor.moveToFirst();
        try {
            while (!cursor.isAfterLast()) {
                String number = cursor.getString(PHONE_NUMBER_COLUMN);
                log("getContactsNumbers number:" + number);
                if (number == null || number.isEmpty()) {
                    log("getContactsNumbers number is null");
                } else {
                    number = CallRejectUtils.allWhite(number);
                    numbers.add(number);
                }
                cursor.moveToNext();
           }
        } finally {
            cursor.close();
        }
    }

    private void getCallLogNumbers(String callLogId, ArrayList<String> numbers) {
        String ids = callLogId.substring(8, callLogId.length() - 1);
        String [] idsArray = ids.split(",");
        for (int i = 0; i < idsArray.length && !mAddContactsCallLogTask.isCancelled(); i++) {
            try {
                int id = Integer.parseInt(idsArray[i].substring(1, idsArray[i].length() - 1));
                Uri existNumberURI = ContentUris.withAppendedId(CALLLOG_URI, id);
                Cursor cursor = getContentResolver().query(existNumberURI, CALL_LOG_PROJECTION, null, null, null);
                cursor.moveToFirst();
                log("----getCallLogNumbers---[calllogid" + id + "]-------");
                try {
                    if (!cursor.isAfterLast()) {
                        String number = cursor.getString(NUMBER);
                        if (number.isEmpty()
                                || number.equals(CALL_LOG_HIDE_NUMBER)
                                || number.equals(CALL_LOG_SPACE_NUMBER)) {
                            log("callLogId:" + id + " the number is invalid");
                        } else {
                            number = CallRejectUtils.allWhite(number);
                            numbers.add(number);
                        }
                        cursor.moveToNext();
                    }
                } finally {
                    cursor.close();
                }
                log("id is " + id);
            } catch (NumberFormatException e) {
                log("parseInt failed, the id is " + e);
            }
        }
    }

    private boolean insertNumber(String number, boolean needCompare) {
        log("insertNumberb number:" + number);
        number = CallRejectUtils.allWhite(number);
        if (mRejectNumbers.size() > 0 && needCompare) {
            for (int i = 0; i < mRejectNumbers.size(); i++) {
                if (CallRejectUtils.equalsNumber(number, mRejectNumbers.get(i))) {
                    log("same number:" + number);
                    return false;
                }
            }
        }
        //insert reject numbers
        ContentValues contentValues = new ContentValues();
        contentValues.put("Number", number);
        getContentResolver().insert(CALLREJECT_URI, contentValues);
        return true;
    }

    private void addNumberOnPreference(String phoneName, String phoneNumber) {
        log("addNumberOnPreference");
        Preference preference = new Preference(this);
        preference.setTitle(phoneName);
        preference.setSummary(phoneNumber);
        mPreferenceScreen.addPreference(preference);
    }

    private void dismissDialogSafely(int id) {
        try {
            dismissDialog(id);
        } catch (IllegalArgumentException e) {
            log("IllegalArgumentException");
        }
    }

    private void removeDuplicateAndOutOfRangeNumbers(ArrayList<String> contactCallLogNumbers) {
        for (int i = 0; i < contactCallLogNumbers.size(); i++) {
            for (int j = i + 1; j < contactCallLogNumbers.size(); j++) {
                if (contactCallLogNumbers.get(i).equals(contactCallLogNumbers.get(j))) {
                    log("remove duplicate number: " + contactCallLogNumbers.get(j));
                    contactCallLogNumbers.remove(j--);
                }
            }
        }

        if (mRejectNumbers.size() > 0) {
            for (int k = 0; k < contactCallLogNumbers.size(); k++) {
                for (int l = 0; l < mRejectNumbers.size(); l++) {
                    if (CallRejectUtils.equalsNumber(contactCallLogNumbers.get(k), mRejectNumbers.get(l))) {
                        log("remove duplicate number with already number:" + contactCallLogNumbers.get(k));
                        contactCallLogNumbers.remove(k--);
                        break;
                    }
                }
            }
        }

        if (mRejectNumbers.size() >= REJECT_LIST_FULL) {
            log("reject list is full");
            mHandler.sendMessage(mHandler.obtainMessage(MyHandler.MESSAGE_FULL));
        }

        if (mRejectNumbers.size() + contactCallLogNumbers.size() >= REJECT_LIST_FULL) {
            mHandler.sendMessage(mHandler.obtainMessage(MyHandler.MESSAGE_FULL));
            int deleteStartIndex = REJECT_LIST_FULL - mRejectNumbers.size();
            log("reject list is full, remove out of range. deleteStartIndex: " + deleteStartIndex);
            for (int m = deleteStartIndex; m < contactCallLogNumbers.size(); m++) {
                contactCallLogNumbers.remove(m--);
            }
        }
    }

    private void updataCallRejectListView() {
        mRejectNumbers.clear();
        mRejectNames.clear();
        queryDisplayNumbers();
        mNeedQuery = false;
    }

    private final ContentObserver mContactsObserver = new CustomContentObserver();
    public class CustomContentObserver extends ContentObserver {
        public CustomContentObserver() {
            super(new Handler());
        }
        @Override
        public void onChange(boolean selfChange) {
            log("Contact db Change, update Contacts name");
            updataCallRejectListView();
        }
    }

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
