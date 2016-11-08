package com.mediatek.rcs.message.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.mediatek.rcs.common.GroupManager;
import com.mediatek.rcs.common.RCSGroup.IGroupActionListener;
import com.mediatek.rcs.common.IpMessageConsts.GroupActionList;
import com.mediatek.rcs.common.IpMessageConsts.GroupNotificationType;
import com.mediatek.rcs.common.RCSGroup;
import com.mediatek.rcs.common.service.Participant;
//import com.mediatek.rcs.common.service.PortraitService;
import com.mediatek.rcs.message.R;
import com.mediatek.rcs.message.utils.RcsMessageUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RcsGroupManagementSetting extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "Rcs/RcsGroupManagementSetting";
    private static final String SCHATIDKEY = "chatId";
    private static final boolean DEBUG = true;
    private String mGroupChatId;
    private SelectContactAsyncTask mAsyncTask;
    private RCSGroupAsyncTask mRcsGroupAsyncTask;
    private ActionListener mListener = new ActionListener();
    private RCSGroup mRCSGroup;
    private boolean mIsMeChairmen = false;
    private String mChairmen;
    private static final int MAX_SUBJECT_LENGTH = 30;
    private Button mExitButton;
    private String mRequestRemoveContact;

    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        mGroupChatId = getIntent().getStringExtra("SCHATIDKEY");
        if (DEBUG && (mGroupChatId == null)) {
            mGroupChatId = "-1";
        }
        Log.d(TAG, "onCreate() mGroupChatId:" + mGroupChatId);
        mRcsGroupAsyncTask = new RCSGroupAsyncTask(this);
        mRcsGroupAsyncTask.execute();
        mAsyncTask = (SelectContactAsyncTask) getLastNonConfigurationInstance();
        if (mAsyncTask != null) {
            mAsyncTask.attach(this);
        }
    }

    /**
     * Describe <code>onResume</code> method here.
     *
     */
    public final void onResume() {
        super.onResume();
        if (mAsyncTask != null) {
            if (mAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                showProgressDialog(getString(R.string.rcs_is_adding_group_member));
            }

            Log.d(TAG, "onResume() mAsyncTask.getStates()" + mAsyncTask.getStatus());
        } else {
            Log.d(TAG, "onResume() mAsyncTask is null");
        }
        Preference preference = getMemberListPreference();
        if (preference != null) {
            ((RcsGroupMember) preference).onResume();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            this.finish();
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    public final Object onRetainNonConfigurationInstance() {
        if (mAsyncTask != null) {
            mAsyncTask.detach();
        }

        return mAsyncTask;
    }

    /**
     * Describe <code>onDestroy</code> method here.
     *
     */
    public final void onDestroy() {
        dismissProgressDialog();
        if (mRCSGroup != null) {
            mRCSGroup.removeActionListener(mListener);
            GroupManager.getInstance().releaseRCSGroup(mGroupChatId);
        }

        Preference preference = getMemberListPreference();
        if (preference != null) {
            ((RcsGroupMember) preference).onDestroy();
        }
        super.onDestroy();
    }

    private static final int MSG_PRE_ADD_GROUP_MEMBER = 0;
    private static final int MSG_ADD_GROUP_MEMBER = 1;
    private static final int MSG_ADD_GROUP_MEMBER_DONE = 2;
    private static final int MSG_REMOVE_GROUP_MEMBER_DONE = 3;

    private static final int MSG_GROUP_MEMBER_RESULT_SUCCESS = 0;
    private static final int MSG_ADD_GROUP_MEMBER_RESULT_FAIL = -1;
    private static final int MSG_EXIT_GROUP = 500;

    private Handler mUiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage(): msg.what:" + msg.what);

            int result;
            String mData;
            switch (msg.what) {
            case MSG_PRE_ADD_GROUP_MEMBER:
                long[] ids = (long[]) msg.obj;
                if (ids.length > 0) {
                    showProgressDialog(getString(R.string.rcs_is_adding_group_member));
                    mProgressDialog.setOnCancelListener(null);
                    onPreRequestAddContact(ids);
                }
                break;

            case MSG_ADD_GROUP_MEMBER:
                List<String> contacts = (ArrayList) msg.obj;
                onRequestAddContact(contacts);
                break;

            case MSG_ADD_GROUP_MEMBER_DONE:
                result = msg.arg1;
                String addedContact = (String) msg.obj;
                onAddParticipientsDone(result, addedContact);
                break;

            case RcsGroupMember.REQUEST_CODE_REMOVE_CONTACT:
                //String removeContact = (String)msg.obj;
                //onRequestRemoveContact(removeContact);
                mRequestRemoveContact = (String)msg.obj;
                showDialog(REMOVE_GROUP_MEMBER_CONFIRM);
                break;

            case MSG_REMOVE_GROUP_MEMBER_DONE:
                result = msg.arg1;
                String removed = (String) msg.obj;
                onRemoveParticipientsDone(result, removed);
                break;

            case MSG_MODIFY_GROUP_SUBJECT_DONE:
                result = msg.arg1;
                String groupName = (String) msg.obj;
                dismissProgressDialog();
                if (result == MSG_GROUP_MEMBER_RESULT_SUCCESS) {
                    if (groupName != null) {
                        mGroupNameSettings.setSummary(groupName);
                        mGroupNameSettings.setText(groupName);
                    }
                } else {
                    Toast.makeText(RcsGroupManagementSetting.this,
                            getString(R.string.setting_fail), Toast.LENGTH_LONG).show();
                    mGroupNameSettings.setText(checkSummary(mRCSGroup.getSubject()));
                }
                break;

            case MSG_MODIFY_GROUP_NICK_NAME_DONE:
                result = msg.arg1;
                mData = (String) msg.obj;
                dismissProgressDialog();
                if (result == MSG_GROUP_MEMBER_RESULT_SUCCESS) {
                    if (mData != null) {
                        mNickName.setSummary(mData);
                        mNickName.setText(mData);
                    }
                } else {
                    Toast.makeText(RcsGroupManagementSetting.this,
                            getString(R.string.setting_fail), Toast.LENGTH_LONG).show();
                    mNickName.setText(checkSummary(mRCSGroup.getGroupNickName()));
                }
                break;

            case MSG_MODIFY_GROUP_SELF_NICK_NAME_DONE:
                result = msg.arg1;
                mData = (String) msg.obj;
                dismissProgressDialog();
                if (result == MSG_GROUP_MEMBER_RESULT_SUCCESS) {
                    if (mData != null) {
                        mSelfNickName.setSummary(checkSummary(mData));
                        mSelfNickName.setText(checkSummary(mData));
                        Preference preference = getMemberListPreference();
                        if (preference != null) {
                            ((RcsGroupMember) preference).onSelfNickNameModified(mData);
                        }
                    } else {
                        mSelfNickName.setSummary(checkSummary(mRCSGroup.getMyNickName()));
                        mSelfNickName.setText(checkSummary(mRCSGroup.getMyNickName()));
                    }
                } else {
                    Toast.makeText(RcsGroupManagementSetting.this,
                            getString(R.string.setting_fail), Toast.LENGTH_LONG).show();
                    mSelfNickName.setSummary(checkSummary(mRCSGroup.getMyNickName()));
                    mSelfNickName.setText(checkSummary(mRCSGroup.getMyNickName()));
                }
                break;

            case MSG_TRANSFER_CHAIRMEN_DONE:
                result = msg.arg1;
                dismissProgressDialog();
                if (result == MSG_GROUP_MEMBER_RESULT_SUCCESS) {
                    boolean oldIsMeChairmen = mIsMeChairmen;
                    String oldChairmen = mChairmen;
                    if (mRCSGroup != null) {
                        mIsMeChairmen = mRCSGroup.isMeChairmen();
                        mChairmen = mRCSGroup.getChairmen();
                        Log.d(TAG, "handleMessage() oldIsMeChairmen:" + oldIsMeChairmen
                                + ",mIsMeChairmen:" + mIsMeChairmen + ",oldChairmen:" + oldChairmen
                                + ",mChairmen:" + mChairmen);
                    }

                    if (oldIsMeChairmen != mIsMeChairmen) {
                        if (isMeChairmen()) {
                            mGroupNameSettings.setSelectable(true);

                            mGroupSettingScreen.addPreference(mGroupManagerCategory);
                            mManagerTransfer
                                    .setOnPreferenceChangeListener(RcsGroupManagementSetting.this);
                        } else {
                            mGroupNameSettings.setSelectable(false);

                            mGroupSettingScreen.removePreference(mGroupManagerCategory);
                        }

                        if (isMeChairmen()) {
                            mExitButton.setText(getString(R.string.pref_disband_and_exit_group));
                        } else {
                            mExitButton.setText(getString(R.string.pref_exit_group));
                        }
                    }

                    Preference preference = getMemberListPreference();
                    if (preference != null) {
                        ((RcsGroupMember) preference).onChairmenTransferred(mChairmen,
                                mIsMeChairmen);
                    }
                } else {
                    Toast.makeText(RcsGroupManagementSetting.this,
                            getString(R.string.setting_fail), Toast.LENGTH_LONG).show();
                }
                break;

            case MSG_MESSAGE_STATUS:
                result = msg.arg1;
                mData = (String) msg.obj;
                dismissProgressDialog();
                if (result == MSG_GROUP_MEMBER_RESULT_SUCCESS) {
                    if (mData != null) {
                        mGroupMessageSettings.setSummary(mData);
                    }
                } else {
                    Toast.makeText(RcsGroupManagementSetting.this,
                            getString(R.string.setting_fail), Toast.LENGTH_LONG).show();
                }
                break;

            case MSG_EXIT_GROUP:
                result = msg.arg1;
                dismissProgressDialog();
                if (result == GroupActionList.VALUE_SUCCESS) {
                    RcsGroupManagementSetting.this.finish();
                } else {
                    Toast.makeText(RcsGroupManagementSetting.this,
                            getString(R.string.setting_fail), Toast.LENGTH_LONG).show();
                }
                break;
            case MSG_ME_REMOVED:
                RcsGroupManagementSetting.this.finish();
                break;

            case MSG_GROUP_ABORTED:
                RcsGroupManagementSetting.this.finish();
                break;
            default:
                break;
            }
        }
    };

    protected String getChatId() {
        return mGroupChatId;
    }

    protected Handler getUiHandler() {
        return mUiHandler;
    }

    protected List<Participant> getParticipants() {
        return mRCSGroup.getParticipants();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(): requestCode = " + requestCode + ", resultCode = "
                + resultCode + ", data = " + data);
        if (resultCode != Activity.RESULT_OK) {
            Log.d(TAG, "fail due to resultCode=" + resultCode);
            return;
        }

        switch (requestCode) {
        case RcsGroupMember.REQUEST_CODE_ADD_CONTACT:
            if (data != null) {
                final long[] contactsIds = data
                        .getLongArrayExtra("com.mediatek.contacts.list.pickdataresult");
                // onPreRequestAddContact(contactsIds);
                Message msg = mUiHandler.obtainMessage(MSG_PRE_ADD_GROUP_MEMBER, contactsIds);
                mUiHandler.sendMessage(msg);
            }
            break;

        case RcsGroupMember.REQUEST_CODE_VIEW_CONTACT:
            break;

        default:
            break;
        }
    }

    private Preference getMemberListPreference() {
        if (getPreferenceScreen() == null) {
            return null;
        }
        Preference preference = ((PreferenceCategory) getPreferenceScreen().getPreference(0))
                .getPreference(0);
        if (preference != null && preference instanceof RcsGroupMember) {
            return preference;
        }

        return null;
    }

    private void onPreRequestAddContact(long[] contactsIds) {

        mAsyncTask = new SelectContactAsyncTask(this);
        mAsyncTask.execute(contactsIds);
    }

    private void onRequestAddContact(List<String> contacts) {
        Log.d(TAG, "onRequestAddContact() contact:" + Arrays.toString(contacts.toArray()));
        if (contacts.size() > 0 && mRCSGroup != null) {
            mRCSGroup.addParticipants(contacts);
        } else {
            dismissProgressDialog();
            Toast.makeText(this, getString(R.string.rcs_add_group_member_failed), Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void onAddParticipientsDone(int result, String contact) {
        dismissProgressDialog();
        // TODO:
        if (result == MSG_GROUP_MEMBER_RESULT_SUCCESS) {
            Preference preference = getMemberListPreference();
            if (preference != null) {
                ((RcsGroupMember) preference).onAddDone(contact);
                Log.d(TAG, "onAddParticipientsDone() contact:" + contact);
            }
        } else {
            Toast.makeText(this, getString(R.string.rcs_add_group_member_failed), Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void onRequestRemoveContact(String contact) {
        Log.d(TAG, "onRequestRemoveContact() contact:" + contact);
        if (contact != null && mRCSGroup != null) {
            showProgressDialog(getString(R.string.rcs_is_deleting_group_member));
            mProgressDialog.setOnCancelListener(null);
            List<String> contacts = new ArrayList<String>();
            contacts.add(contact);
            mRCSGroup.removeParticipants(contacts);
        } else {
            Toast.makeText(this, getString(R.string.rcs_delete_group_member_failed),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void onRemoveParticipientsDone(int result, String contact) {
        dismissProgressDialog();
        // todo:
        if (result == MSG_GROUP_MEMBER_RESULT_SUCCESS) {
            Preference preference = getMemberListPreference();
            if (preference != null) {
                ((RcsGroupMember) preference).onRemoveDone(contact);
                Log.d(TAG, "onRemoveParticipientsDone() contact:" + contact);
            }
        } else {
            Toast.makeText(this, getString(R.string.rcs_delete_group_member_failed),
                    Toast.LENGTH_LONG).show();
        }
    }

    private ProgressDialog mProgressDialog = null;

    private ProgressDialog createProgressDlg(String msg) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(msg);
            mProgressDialog.setCancelable(true);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setIndeterminate(true);
        } else {
            mProgressDialog.setMessage(msg);
        }
        return mProgressDialog;
    }

    private void showProgressDialog(String msg) {
        if (mProgressDialog == null) {
            mProgressDialog = createProgressDlg(msg);
        } else {
            mProgressDialog.setMessage(msg);
        }

        mProgressDialog.show();
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null) {
            try {
                if (mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mProgressDialog = null;
            }
        }
    }

    private void onPreSelect() {
        // showProgressDialog("is adding group member...");
    }

    private void onPostSelect(List<String> contacts) {
        Message msg = mUiHandler.obtainMessage(MSG_ADD_GROUP_MEMBER, contacts);
        mUiHandler.sendMessage(msg);
    }

    private static final Uri PHONES_WITH_PRESENCE_URI = Data.CONTENT_URI;
    private static final String[] CALLER_ID_PROJECTION = new String[] { Phone._ID, // 0
            Phone.NUMBER, // 1
            Phone.LABEL, // 2
            Phone.DISPLAY_NAME, // 3
            Phone.CONTACT_ID, // 4
            Phone.CONTACT_PRESENCE, // 5
            Phone.CONTACT_STATUS, // 6
            // Phone.NORMALIZED_NUMBER, // 7
            Contacts.SEND_TO_VOICEMAIL // 8
    };

    private static final int PHONE_ID_COLUMN = 0;
    private static final int PHONE_NUMBER_COLUMN = 1;
    private static final int PHONE_LABEL_COLUMN = 2;
    private static final int CONTACT_NAME_COLUMN = 3;
    private static final int CONTACT_ID_COLUMN = 4;
    private static final int CONTACT_PRESENCE_COLUMN = 5;
    private static final int CONTACT_STATUS_COLUMN = 6;
    private static final int PHONE_NORMALIZED_NUMBER = 7;
    private static final int SEND_TO_VOICEMAIL = 8;

    private static final class SelectContactAsyncTask extends AsyncTask<long[], Void, List<String>> {
        private Context mContext;

        public SelectContactAsyncTask(Context context) {
            attach(context);
        }

        @Override
        protected void onPreExecute() {
            if (mContext != null) {
                ((RcsGroupManagementSetting) mContext).onPreSelect();
            }
        }

        @Override
        protected List<String> doInBackground(long[]... params) {
            long[] ids = params[0];
            Log.d(TAG, "doInBackground() ids:" + Arrays.toString(ids));

            List<String> contacts = new ArrayList<String>(getContactInfoForPhoneIds(ids));

            try {
                new Thread().sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return contacts;
        }

        @Override
        protected void onPostExecute(List<String> result) {
            super.onPostExecute(result);
            if (mContext != null) {
                if (DEBUG) {
                    Log.d(TAG, "onPostExecute() result:" + Arrays.toString(result.toArray()));
                }
                ((RcsGroupManagementSetting) mContext).onPostSelect(result);
            }
        }

        public void detach() {
            mContext = null;
        }

        public void attach(Context context) {
            mContext = context;
        }

        private Set<String> getContactInfoForPhoneIds(long ids[]) {
            Set<String> entries = new HashSet<String>();
            if (ids != null && ids.length > 0) {
                StringBuilder idSetBuilder = new StringBuilder();
                boolean first = true;
                for (long id : ids) {
                    if (first) {
                        first = false;
                        idSetBuilder.append(id);
                    } else {
                        idSetBuilder.append(',').append(id);
                    }
                }

                final String whereClause = Phone._ID + " IN (" + idSetBuilder.toString() + ")";
                Cursor cursor = mContext.getContentResolver().query(PHONES_WITH_PRESENCE_URI,
                        CALLER_ID_PROJECTION, whereClause, null, null);
                if (cursor == null) {
                    return entries;
                }

                try {
                    while (cursor.moveToNext()) {
                        String entry = cursor.getString(PHONE_NUMBER_COLUMN);
                        entries.add(entry);
                    }
                } finally {
                    cursor.close();
                }
            }

            return entries;
        }
    }

    private class ActionListener implements IGroupActionListener {
        public void onParticipantAdded(Participant participant) {
            // todo: adjust if different with onAddParticipantsResult
            Message msg = mUiHandler.obtainMessage(MSG_ADD_GROUP_MEMBER_DONE,
                    MSG_GROUP_MEMBER_RESULT_SUCCESS, 0, participant.getContact());
            mUiHandler.sendMessage(msg);
            Log.d(TAG, "onParticipantAdded() participant:" + participant.getContact());
        }

        public void onParticipantLeft(Participant participant) {
            Message msg = mUiHandler.obtainMessage(MSG_REMOVE_GROUP_MEMBER_DONE,
                    MSG_GROUP_MEMBER_RESULT_SUCCESS, 0, participant.getContact());
            mUiHandler.sendMessage(msg);
            Log.d(TAG, "onParticipantLeft() ,removedContacts:" + participant.getContact());
        }

        public void onParticipantRemoved(Participant participant) {
            Message msg = mUiHandler.obtainMessage(MSG_REMOVE_GROUP_MEMBER_DONE,
                    MSG_GROUP_MEMBER_RESULT_SUCCESS, 0, participant.getContact());
            mUiHandler.sendMessage(msg);
            Log.d(TAG, "onParticipantRemoved()participant:" + participant.getContact());
        }

        public void onChairmenTransferred(Participant newChairmen) {
            Message msg = mUiHandler.obtainMessage(MSG_TRANSFER_CHAIRMEN_DONE,
                    MSG_GROUP_MEMBER_RESULT_SUCCESS, 0, newChairmen);
            mUiHandler.sendMessage(msg);
            Log.d(TAG, "onChairmenTransferred() ,newChairmen:" + newChairmen);
        }

        public void onSubjectModified(String newSubject) {
            Message msg = mUiHandler.obtainMessage(MSG_MODIFY_GROUP_SUBJECT_DONE,
                    MSG_GROUP_MEMBER_RESULT_SUCCESS, 0, newSubject);
            mUiHandler.sendMessage(msg);
            Log.d(TAG, "onSubjectModified() ,newSubject:" + newSubject);
        }

        public void onNickNameModified(String newNickName) {
            Message msg = mUiHandler.obtainMessage(MSG_MODIFY_GROUP_NICK_NAME_DONE,
                    MSG_GROUP_MEMBER_RESULT_SUCCESS, 0, newNickName);
            mUiHandler.sendMessage(msg);
            Log.d(TAG, "onNickNameModified() ,newNickName:" + newNickName);
        }

        public void onSelfNickNameModified(String newSelfNickName) {
            Message msg = mUiHandler.obtainMessage(MSG_MODIFY_GROUP_SELF_NICK_NAME_DONE,
                    MSG_GROUP_MEMBER_RESULT_SUCCESS, 0, newSelfNickName);
            mUiHandler.sendMessage(msg);
            Log.d(TAG, "onSelfNickNameModified() ,newSelfNickName:" + newSelfNickName);
        }

        public void onMeRemoved() {
            Message msg = mUiHandler.obtainMessage(MSG_ME_REMOVED);
            mUiHandler.sendMessage(msg);
            Log.d(TAG, "onMeRemoved()");
        }

        public void onGroupAborted() {
            Message msg = mUiHandler.obtainMessage(MSG_GROUP_ABORTED);
            mUiHandler.sendMessage(msg);
            Log.d(TAG, "onGroupAborted()");
        }

        public void onAddParticipantsResult(int result) {
            Message msg = mUiHandler.obtainMessage(MSG_ADD_GROUP_MEMBER_DONE, result, 0, null);
            mUiHandler.sendMessage(msg);
            Log.d(TAG, "onAddParticipantsResult() result:" + result);
        }

        public void onRemoveParticipantResult(int result) {
            Message msg = mUiHandler.obtainMessage(MSG_REMOVE_GROUP_MEMBER_DONE, result, 0, null);
            mUiHandler.sendMessage(msg);
            Log.d(TAG, "onRemoveParticipants() result:" + result);
        }

        public void onTransferChairmenResult(int result) {
            Message msg = mUiHandler.obtainMessage(MSG_TRANSFER_CHAIRMEN_DONE, result, 0, null);
            mUiHandler.sendMessage(msg);
            Log.d(TAG, "onTransferChairmenResult() result:" + result);
        }

        public void onModifySubjectResult(String subject, int result) {
            Message msg = mUiHandler.obtainMessage(MSG_MODIFY_GROUP_SUBJECT_DONE, result, 0,
                    subject);
            mUiHandler.sendMessage(msg);
            Log.d(TAG, "onModifySubjectResult() result:" + result);
        }

        public void onModifyNickNameResult(int result) {
            String newSelfNickName = mRCSGroup.getGroupNickName();
            Message msg = mUiHandler.obtainMessage(MSG_MODIFY_GROUP_NICK_NAME_DONE, result, 0,
                    checkSummary(newSelfNickName));
            mUiHandler.sendMessage(msg);
            Log.d(TAG, "onModifyNickNameResult(),result:" + result + ",newSelfNickName"
                    + newSelfNickName);
        }

        public void onModifySelfNickNameResult(String selfNickName, int result) {
            Message msg = mUiHandler.obtainMessage(MSG_MODIFY_GROUP_SELF_NICK_NAME_DONE, result, 0,
                    selfNickName);
            mUiHandler.sendMessage(msg);
            Log.d(TAG, "onModifySelfNickNameResult(),selfNickName:" + selfNickName);
        }

        public void onExitGroupResult(int result) {
            Message msg = mUiHandler.obtainMessage(MSG_EXIT_GROUP, result, 0, null);
            mUiHandler.sendMessage(msg);
            Log.d(TAG, "onExitGroupResult() result:" + result);
        }

        public void onDestroyGroupResult(int result) {
            Message msg = mUiHandler.obtainMessage(MSG_EXIT_GROUP, result, 0, null);
            mUiHandler.sendMessage(msg);
            Log.d(TAG, "onDestroyGroupResult() result:" + result);
        }

        public void onAddParticipantFail(Participant participant) {
            // TODO Auto-generated method stub

        }
    }

    /**
     * ruofei added
     */
    private static final String GROUP_SETTING_SCREEN = "pref_group_setting_screen";
    private static final String GROUP_NICK_NAME = "pref_key_group_notes";
    private static final String GROUP_SELF_NICK_NAME = "pref_key_group_alias";
    private static final String GROUP_MSG_SETTINGS = "pref_key_group_msg_settings";
    private static final String GROUP_MANAGER_TRANSFER = "pref_key_group_manager_transfer";
    private static final String GROUP_INFO = "pref_group_info";
    private static final String GROUP_NAME_SETTINGS = "pref_key_group_name_settings";
    // private static final String GROUP_NAME = "pref_key_group_name";
    private static final String GROUP_MANAGER_TRANSFER_INFO = "pref_group_manager_transfer_info";
    private static final String GROUP_MENBER = "rcsgroupmember";
    private static final int EXIT_GROUP = 200;
    private static final int MANAGER_TRANSFER = 300;
    private static final int REMOVE_GROUP_MEMBER_CONFIRM = 400;

    private static final int MSG_MODIFY_GROUP_SUBJECT_DONE = 40;
    private static final int MSG_MODIFY_GROUP_NICK_NAME_DONE = 50;
    private static final int MSG_MODIFY_GROUP_SELF_NICK_NAME_DONE = 60;
    private static final int MSG_TRANSFER_CHAIRMEN_DONE = 70;
    private static final int MSG_MESSAGE_STATUS = 80;
    private static final int MSG_ME_REMOVED = 90;
    private static final int MSG_GROUP_ABORTED = 100;
    // boolean mGroupManager = true;
    PreferenceScreen mGroupSettingScreen;
    EditTextPreference mGroupNameSettings;
    EditTextPreference mNickName;
    EditTextPreference mSelfNickName;
    ListPreference mGroupMessageSettings;
    PreferenceCategory mGroupManagerCategory;
    Preference mManagerTransfer;

    // RcsGroupMember mGroupMember;
    // PortraitService mPortraitService;
    private void setupPreference() {
        addPreferencesFromResource(R.layout.group_management_setting);
        // mPortraitService = new PortraitService(this, 0, 0);

        mGroupSettingScreen = (PreferenceScreen) findPreference(GROUP_SETTING_SCREEN);
        mGroupNameSettings = (EditTextPreference) findPreference(GROUP_NAME_SETTINGS);
        mGroupNameSettings.setSummary(checkSummary(mRCSGroup.getSubject()));
        mGroupNameSettings.setText(checkSummary(mRCSGroup.getSubject()));
        mGroupNameSettings.setOnPreferenceChangeListener(this);
        mGroupNameSettings.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                EditTextPreference editPref = (EditTextPreference) preference;
                editPref.getEditText().setSelection(editPref.getText().length());
                return true;
            }
        });
        EditTextInputFilter filter = new EditTextInputFilter();
        filter.setTrimSpace(true);
        filter.setNoticeInfo(R.string.subject_len_reach_max);
        mGroupNameSettings.getEditText().setFilters(new InputFilter[] { filter });
        if (isMeChairmen()) {
            mGroupNameSettings.setSelectable(true);
        } else {
            mGroupNameSettings.setSelectable(false);
            // mGroupNameSettings.setEnabled(false);
            // mGroupNameSettings.setOnPreferenceChangeListener(null);
        }
        mGroupNameSettings.getEditText().addTextChangedListener(new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    String value = mGroupNameSettings.getEditText().getText().toString();
                    boolean enable =  !value.matches(" *");
                    Dialog dlg = mGroupNameSettings.getDialog();
                    if (dlg instanceof AlertDialog) {
                        Button btn = ((AlertDialog)dlg).getButton(AlertDialog.BUTTON_POSITIVE);
                        if (btn != null) {
                            btn.setEnabled(enable);
                        }
                    }
                }
            });

        mNickName = (EditTextPreference) findPreference(GROUP_NICK_NAME);
        mNickName.setSummary(checkSummary(mRCSGroup.getGroupNickName()));
        mNickName.setText(checkSummary(mRCSGroup.getGroupNickName()));
        mNickName.setOnPreferenceChangeListener(this);
        mNickName.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                EditTextPreference editPref = (EditTextPreference) preference;
                editPref.getEditText().setSelection(editPref.getText().length());
                return true;
            }
        });
        EditTextInputFilter nickNameFilter = new EditTextInputFilter();
        nickNameFilter.setNoticeInfo(R.string.nick_name_len_reach_max);
        mNickName.getEditText().setFilters(new InputFilter[] { nickNameFilter });

        mSelfNickName = (EditTextPreference) findPreference(GROUP_SELF_NICK_NAME);
        mSelfNickName.setSummary(checkSummary(mRCSGroup.getMyNickName()));
        mSelfNickName.setText(checkSummary(mRCSGroup.getMyNickName()));
        mSelfNickName.setOnPreferenceChangeListener(this);
        mSelfNickName.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                EditTextPreference editPref = (EditTextPreference) preference;
                editPref.getEditText().setSelection(editPref.getText().length());
                return true;
            }
        });
        EditTextInputFilter selfNameFilter = new EditTextInputFilter();
        selfNameFilter.setNoticeInfo(R.string.self_nick_name_len_reach_max);
        mSelfNickName.getEditText().setFilters(new InputFilter[] { selfNameFilter });
        // mGroupMember = (RcsGroupMember)findPreference(GROUP_MENBER);
        mGroupMessageSettings = (ListPreference) findPreference(GROUP_MSG_SETTINGS);
        mGroupMessageSettings.setOnPreferenceChangeListener(this);
        String[] msgSettingValues = getResources().getStringArray(
                R.array.pref_msg_group_msg_notice_choices);
        switch (mRCSGroup.getNotificationEnable()) {
        case GroupNotificationType.NOTIFICATION_ENABLE:
            mGroupMessageSettings.setSummary(msgSettingValues[0]);
            mGroupMessageSettings.setValue(msgSettingValues[0]);
            break;

        case GroupNotificationType.NOTIFICATION_DISABLE:
            mGroupMessageSettings.setSummary(msgSettingValues[1]);
            mGroupMessageSettings.setValue(msgSettingValues[1]);
            break;

        case GroupNotificationType.NOTIFICATION_REJECT:
            mGroupMessageSettings.setSummary(msgSettingValues[2]);
            mGroupMessageSettings.setValue(msgSettingValues[2]);
            break;

        default:
            Log.d(TAG, "setupPreference() mRCSGroup.getNotificationEnable():"
                  + mRCSGroup.getNotificationEnable() + ",match none!");
            mGroupMessageSettings.setSummary(msgSettingValues[0]);
            mGroupMessageSettings.setValue(msgSettingValues[0]);
            break;
        }
        mGroupManagerCategory = (PreferenceCategory) findPreference(GROUP_MANAGER_TRANSFER_INFO);
        mManagerTransfer = findPreference(GROUP_MANAGER_TRANSFER);
        if (isMeChairmen()) {
            // mGroupManagerCategory.setSummary(getChairmen());
            mManagerTransfer.setOnPreferenceChangeListener(this);
        } else {
            // mGroupManagerCategory.removeAll();
            mGroupSettingScreen.removePreference(mGroupManagerCategory);
        }
        initButton();
    }

    private void initButton() {
        LinearLayout l = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.exit_button,
                null);

        mExitButton = (Button) l.findViewById(R.id.exit_button);
        mExitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(EXIT_GROUP);
            }
        });
        if (isMeChairmen()) {
            mExitButton.setText(getString(R.string.pref_disband_and_exit_group));
        } else {
            mExitButton.setText(getString(R.string.pref_exit_group));
        }
        getListView().addFooterView(l);
    }

    public boolean onPreferenceChange(Preference arg0, Object arg1) {
        Log.d(TAG, "onPreferenceChange() arg0.getKey()" + arg0.getKey() + ",arg1:" + arg1);
        String msg = this.getString(R.string.is_setting);
        switch (arg0.getKey()) {
        case GROUP_NAME_SETTINGS:
            String groupName = (String) arg1;
            if (groupName == null || groupName.matches(" *")) {
                Toast.makeText(RcsGroupManagementSetting.this,
                               getString(R.string.invalid_group_name),
                               Toast.LENGTH_LONG).show();
                return false;
            }
            if (!groupName.equals(mRCSGroup.getSubject())) {
                boolean result = mRCSGroup.modifySubject(groupName);
                if (result) {
                    showProgressDialog(msg);
                    mProgressDialog.setOnCancelListener(new OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                mGroupNameSettings.setText(checkSummary(mRCSGroup.getSubject()));
                            }
                        });
                } else {
                    Toast.makeText(RcsGroupManagementSetting.this,
                                   getString(R.string.setting_fail),
                                   Toast.LENGTH_LONG).show();
                    return false;
                }
            } else {
                Log.d(TAG, "onPreferenceChange() same group name! groupName:" + groupName);
                return false;
            }
            break;

        case GROUP_NICK_NAME:
            String groupNickName = (String)arg1;
            groupNickName = groupNickName.replaceAll("^ +| +$", "");
            if (!groupNickName.equals(mRCSGroup.getGroupNickName())) {
                boolean result = mRCSGroup.modifyNickName(groupNickName);
                if (result) {
                    // just save to db, no async result, so no need showProgressDialog
                    mNickName.setSummary(groupNickName);
                    // after trim space, may be different from arg1,
                    // so for convenience, just setText and return false
                    mNickName.setText(groupNickName);
                } else {
                    Toast.makeText(RcsGroupManagementSetting.this,
                                   getString(R.string.setting_fail),
                                   Toast.LENGTH_LONG).show();
                }
            } else {
                Log.d(TAG, "onPreferenceChange() same group nick name! groupNickName:"
                      + groupNickName);
            }
            return false;

        case GROUP_SELF_NICK_NAME:
            String selfName = (String) arg1;
            selfName = selfName.replaceAll("^ +| +$", "");
            if (!selfName.equals(mRCSGroup.getMyNickName())) {
                boolean result = mRCSGroup.modifySelfNickName(selfName);
                if (result) {
                    showProgressDialog(msg);
                    mProgressDialog.setOnCancelListener(new OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                mSelfNickName.setText(checkSummary(mRCSGroup.getMyNickName()));
                            }
                        });
                } else {
                    Toast.makeText(RcsGroupManagementSetting.this,
                                   getString(R.string.setting_fail),
                                   Toast.LENGTH_LONG).show();
                    return false;
                }
            } else {
                Log.d(TAG, "onPreferenceChange() same self nick name! selfName:" + selfName);
                return false;
            }
            break;

        case GROUP_MSG_SETTINGS:
            String msgSettings = (String) arg1;
            mGroupMessageSettings.setSummary(msgSettings);
            String[] msgSettingValues = getResources().getStringArray(
                    R.array.pref_msg_group_msg_notice_choices);
            if (msgSettingValues[0].equals(msgSettings)) {
                mRCSGroup.setNotificationEnable(GroupNotificationType.NOTIFICATION_ENABLE);
            } else if (msgSettingValues[1].equals(msgSettings)) {
                mRCSGroup.setNotificationEnable(GroupNotificationType.NOTIFICATION_DISABLE);
            } else if (msgSettingValues[2].equals(msgSettings)) {
                mRCSGroup.setNotificationEnable(GroupNotificationType.NOTIFICATION_REJECT);
            } else {
                Log.d(TAG, "onPreferenceChange() msgSettings:" + msgSettings + ",match none!");
            }
            break;

        // case GROUP_MANAGER_TRANSFER:
        //     String data = (String)arg1;
        //     break;

        default:
            break;
        }
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mManagerTransfer) {
            // showDialog(MANAGER_TRANSFER);
            final MemberNameAdapter memberNameAdapter = new MemberNameAdapter(
                    RcsGroupManagementSetting.this, getGroupMember());
            Dialog dialog;
            int position = memberNameAdapter.getChairmenPosition();
            dialog = new AlertDialog.Builder(RcsGroupManagementSetting.this)
                    .setTitle(R.string.pref_group_manager_transfer)
                    .setSingleChoiceItems(memberNameAdapter, position,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                String newChairmen = ((RcsGroupMember.GroupMember) memberNameAdapter
                                        .getItem(which)).getNumber();
                                Log.d(TAG, "MANAGER_TRANSFER newChairmen:" + newChairmen
                                      + ",getChairmen():" + getChairmen());
                                if (!newChairmen.equals(getChairmen())) {
                                    mRCSGroup.transferChairmen(newChairmen);
                                    showProgressDialog(getString(R.string.pref_please_wait));
                                    mProgressDialog.setOnCancelListener(null);
                                }

                            }
                        })
                    .setNegativeButton(R.string.pref_cancel, new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface arg0, final int arg1) {

                        }
                    }).create();
            dialog.show();
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        Dialog dialog = null;
        switch (id) {
        case EXIT_GROUP:
            String messageInfo = isMeChairmen() ? getString(R.string.pref_exit_group_manager_info)
                    : getString(R.string.pref_exit_group_info);
            dialog = new AlertDialog.Builder(RcsGroupManagementSetting.this)
                    .setMessage(messageInfo)
                    .setPositiveButton(R.string.pref_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface arg0, final int arg1) {
                            if (isMeChairmen()) {
                                mRCSGroup.destroyGroup();
                            } else {
                                mRCSGroup.exitGroup();
                            }
                            showProgressDialog(getString(R.string.pref_please_wait));
                            mProgressDialog.setOnCancelListener(null);
                        }
                    })
                    .setNegativeButton(R.string.pref_cancel, new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface arg0, final int arg1) {

                        }
                    }).create();
            break;

        case REMOVE_GROUP_MEMBER_CONFIRM:
            dialog = new AlertDialog.Builder(RcsGroupManagementSetting.this)
                .setMessage(getString(R.string.remove_group_member_confirm))
                .setPositiveButton(R.string.pref_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface arg0, final int arg1) {
                            onRequestRemoveContact(mRequestRemoveContact);
                        }
                    })
                .setNegativeButton(R.string.pref_cancel, new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface arg0, final int arg1) {
                            mRequestRemoveContact = null;
                        }
                    }).create();
            break;

        default:
            break;
        }

        return dialog;
    }

    private ArrayList<RcsGroupMember.GroupMember> getGroupMember() {
        Preference preference = getMemberListPreference();
        if (preference != null) {
            return ((RcsGroupMember) preference).getGroupMember();
        }
        return new ArrayList<RcsGroupMember.GroupMember>();
    }

    public class MemberNameAdapter extends BaseAdapter {
        ArrayList<RcsGroupMember.GroupMember> mData;
        private Context mContext;
        private LayoutInflater mInflater;

        public MemberNameAdapter(Context context, ArrayList<RcsGroupMember.GroupMember> data) {
            mData = data;
            mContext = context;
            mInflater = LayoutInflater.from(mContext);
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public Object getItem(int index) {
            return mData.get(index);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup viewGroupg) {
            if (convertView == null) {
                convertView = mInflater.inflate(android.R.layout.simple_list_item_single_choice,
                        viewGroupg, false);
            }
            if (convertView != null) {
                CheckedTextView textView = (CheckedTextView) convertView
                        .findViewById(android.R.id.text1);
                textView.setText(mData.get(position).getName());
                String chairmen = getChairmen();
                String currentNumber = mData.get(position).getNumber();
                if (chairmen.equals(currentNumber)) {
                    textView.setChecked(true);
                } else {
                    textView.setChecked(false);
                }
                Log.d(TAG, "getView() position:" + position + ",chairmen:" + chairmen
                        + ",currentNumber:" + currentNumber);
            }
            return convertView;
        }

        public int getChairmenPosition() {
            int position = -1;
            int size = mData.size();
            for (int i = 0; i < size; i++) {
                if (mData.get(i).getNumber().equals(getChairmen())) {
                    return i;
                }
            }

            Log.d(TAG, "getChairmenPosition()" + position);
            return position;
        }
    }

    public boolean isMeChairmen() {
        Log.d(TAG, "isMeChairmen()" + mIsMeChairmen);
        return mIsMeChairmen;
    }

    public String getChairmen() {
        Log.d(TAG, "getChairmen()" + mChairmen);
        return mChairmen;
    }

    public String getMyNickName() {
        return mRCSGroup.getMyNickName();
    }

    private class RCSGroupAsyncTask extends AsyncTask<Void, Void, Long> {
        private Context mContext;

        public RCSGroupAsyncTask(Context context) {
            mContext = context;
        }

        @Override
        protected void onPostExecute(Long result) {
            super.onPostExecute(result);
            if (mRCSGroup != null) {
                setupPreference();
            }
        }

        @Override
        protected Long doInBackground(Void... arg0) {
            mRCSGroup = GroupManager.getInstance().getRCSGroup(mGroupChatId);
            if (mRCSGroup != null) {
                mRCSGroup.addActionListener(mListener);
                mIsMeChairmen = mRCSGroup.isMeChairmen();
                mChairmen = mRCSGroup.getChairmen();
            } else {
                Log.d(TAG, "doInBackground() getRCSGroup fail");
            }
            return null;
        }
    }

    private String checkSummary(String summary) {
        if (summary == null) {
            return "";
        }
        return summary;
    }

    private class EditTextInputFilter implements InputFilter {
        private boolean mTrimSpace = false;
        private int mNoticeInfo = -1;
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
                                   int dstart, int dend) {
            int destOldLength = dest.toString().getBytes().length;
            int destReplaceLength = dest.subSequence(dstart, dend).toString().getBytes().length;
            String sourceSubString = source.subSequence(start, end).toString();
            if (mTrimSpace) {
                sourceSubString = sourceSubString.replaceAll(" ", "");
            }
            int sourceReplaceLength = sourceSubString.toString().getBytes().length;
            int newLength = destOldLength - destReplaceLength + sourceReplaceLength;
            if (newLength > MAX_SUBJECT_LENGTH) {
                // need cut the new input charactors
                Toast.makeText(RcsGroupManagementSetting.this, mNoticeInfo,
                               Toast.LENGTH_SHORT).show();
                int keep = MAX_SUBJECT_LENGTH - (destOldLength - destReplaceLength);
                if (sourceReplaceLength >= 2) {
                    //emotion contail 2 char at least
                    if (RcsMessageUtils.containsEmoji(sourceSubString)) {
                        //if emoji, discard all content
                        keep = 0;
                    }
                }
                if (keep <= 0) {
                    return "";
                } else {
                    return getMaxByteSequence(sourceSubString, keep);
                }
            } else {
                return sourceSubString; // can replace
            }
        }

        public CharSequence getMaxByteSequence(CharSequence str, int keep) {
            String source = str.toString();
            int byteSize = source.getBytes().length;
            if (byteSize <= keep) {
                return str;
            } else {
                int charSize = source.length();
                while (charSize > 0) {
                    source = source.substring(0, source.length() - 1);
                    charSize--;
                    if (source.getBytes().length <= keep) {
                        break;
                    }
                }
                return source;
            }
        }

        public void setTrimSpace(boolean trimSpace) {
            mTrimSpace = trimSpace;
        }

        public void setNoticeInfo(int noticeInfo) {
            mNoticeInfo = noticeInfo;
        }
    }

}
