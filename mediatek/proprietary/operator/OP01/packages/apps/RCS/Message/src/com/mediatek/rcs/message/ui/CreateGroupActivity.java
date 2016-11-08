/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.rcs.message.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.mediatek.rcs.common.GroupManager;
import com.mediatek.rcs.common.GroupManager.IInitGroupListener;
import com.mediatek.rcs.common.IpMessageConsts;
import com.mediatek.rcs.message.R;
import com.mediatek.rcs.message.data.Contact;
import com.mediatek.rcs.message.data.ContactList;
import com.mediatek.rcs.message.utils.RcsMessageUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
/**
 * Create Group Activity.
 *
 */
public class CreateGroupActivity extends Activity implements IInitGroupListener {
    private final String TAG = "CreateGroupActivity";

    private ProgressDialog mProgressDialog;
//    private RcsGroupManager mGroupManager;
    private String mSubject;
    EditText mSubjectEditor;
    AlertDialog mSubjectDialog;

    private static final int MESSAGE_DIALOG_DISMISS = 1000;
    private static final int MAX_SUBJECT_LENGTH = 30;
    private static final String STATE_SHOW_SUBJECT_EDITOR = "show_subject_editor";
    private static final String STATE__EDITOR_CONTENT = "editor_content";

    public static final String TAG_CREATE_GROUP_BY_IDS = "ids";
    public static final String TAG_CREATE_GROUP_BY_NUMBERS = "numbers";
    public static final String ACTIVITY_ACTION = "android.intent.action.rcs.CREATE_GROUP";

    private boolean mIsVisble;
    private boolean mListeningResult;
    private HashSet<String> mToInviteNumbers = new HashSet<String>();
    private String mChatId;

    @Override
    protected void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);
        Log.d(TAG, "onCreate: " + this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        initProgressDialog();
        initSubjectDialog();
        initialize(saveBundle, getIntent());
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: " + this);
        mIsVisble = true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSubjectDialog.isShowing()) {
            outState.putBoolean(STATE_SHOW_SUBJECT_EDITOR, true);
            String content = mSubjectEditor.getText().toString();
            if (!TextUtils.isEmpty(content)) {
                outState.putString(STATE__EDITOR_CONTENT, content);
            }
        } else {
            outState.putBoolean(STATE_SHOW_SUBJECT_EDITOR, true);
        }
    }

    /// M: Add for OP09
    @Override
    protected void onNewIntent(Intent intent) {
        // TODO Auto-generated method stub
        super.onNewIntent(intent);
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop: " + this);
        super.onStop();
        mIsVisble = false;
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: " + this);
        super.onDestroy();
        if (mListeningResult) {
            GroupManager.getInstance().removeGroupListener(this);
        }
    }

    public Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_DIALOG_DISMISS:
                    CreateGroupActivity.this.finish();
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private void initProgressDialog() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setMessage(getString(R.string.creating_group));
        mProgressDialog.setCancelMessage(mHandler.obtainMessage(MESSAGE_DIALOG_DISMISS));
    }

    private void initialize(Bundle saveBundle, final Intent intent) {
        Log.d(TAG, "initialize: saveBundle = " + saveBundle);
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                processIntent(intent);
                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        showSubjectDialog();
                    }
                });
            }
        }).start();
//        if (saveBundle != null) {
//            //
//            boolean show = saveBundle.getBoolean(STATE_SHOW_SUBJECT_EDITOR);
//            if (show) {
//                String content = saveBundle.getString(STATE__EDITOR_CONTENT);
//                if (!TextUtils.isEmpty(content)) {
//                    mSubjectEditor.setText(content);
//                    showSubjectDialog();
//                }
//            }
//        } else {
//
//        }
    }

    private void processIntent(Intent intent) {
        List<ContactList> contactLists = new ArrayList<ContactList>(2);

        List<String> numbers = intent.getStringArrayListExtra(TAG_CREATE_GROUP_BY_NUMBERS);
        int numberCount = 0;
        if (numbers != null) {
            ContactList list = ContactList.getByNumbers(numbers, true);
            if (list != null) {
                contactLists.add(list);
                numberCount += list.size();
            }
        }
        long[] contactIds = intent.getLongArrayExtra(TAG_CREATE_GROUP_BY_IDS);
        if (contactIds != null) {
            ContactList list = ContactList.blockingGetByIds(contactIds);
            if (list != null) {
                contactLists.add(list);
                numberCount += list.size();
            }
        }
        if (numberCount == 0) {
            Toast.makeText(CreateGroupActivity.this, "can not process intent", Toast.LENGTH_SHORT)
                            .show();
            finish();
            return;
        }

        int count = 0;
        StringBuilder sb = new StringBuilder();
        for (ContactList list : contactLists) {
            for (Contact c : list) {
                mToInviteNumbers.add(c.getNumber());
                if (count < numberCount) {
                    sb.append(c.getName());
                    if (count < numberCount - 1) {
                        sb.append(",");
                    }
                }
                count++;
            }
        }
        mSubject = getMaxByteSequence(sb.toString(), MAX_SUBJECT_LENGTH).toString();
    }

    private void createGroupByNumbers(final HashSet<String> numbers) {
        mProgressDialog.show();
        mChatId = GroupManager.getInstance().initGroupChat(numbers,
                                        mSubjectEditor.getText().toString());
        if (mChatId == null) {
            Log.e(TAG, "create fail");
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            Toast.makeText(CreateGroupActivity.this, R.string.create_group_failed,
                                            Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        GroupManager.getInstance().addGroupListener(this);
        mListeningResult = true;
        Log.d(TAG, "createGroupByNumbers: " + this);
    }

    private void showSubjectDialog() {
        mSubjectDialog.show();
        mSubjectEditor.setText(mSubject);
    }

    private void initSubjectDialog() {
        LayoutInflater factory = LayoutInflater.from(this);
        final View view = factory.inflate(R.layout.subject_dialog_editor, null);
        mSubjectEditor = (EditText) view.findViewById(R.id.editor);
        mSubjectDialog = new AlertDialog.Builder(this)
            .setTitle(R.string.set_subject)
            .setView(view)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    Log.w(TAG, "create group chat to go");
                    createGroupByNumbers(mToInviteNumbers);
                }
            })
            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    Log.w(TAG, "cancel create group chat");
                    finish();
                }
            })
            .create();

        mSubjectDialog.setCancelMessage(mHandler.obtainMessage(MESSAGE_DIALOG_DISMISS));
        mSubjectEditor.setFilters(new InputFilter[]{new InputFilter() {

            @Override
            public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned dest, int dstart, int dend) {
                int destOldLength = dest.toString().getBytes().length;
                int destReplaceLength = dest.subSequence(dstart, dend).toString().getBytes().length;
                String sourceSubString = source.subSequence(start, end).toString();
                sourceSubString = sourceSubString.replace(" ", "");
                int sourceReplaceLength = sourceSubString.toString().getBytes().length;
                int newLength =  destOldLength - destReplaceLength + sourceReplaceLength;
                if (newLength > MAX_SUBJECT_LENGTH) {
                    // need cut the new input charactors
                    Toast.makeText(CreateGroupActivity.this, R.string.subject_len_reach_max,
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
        } });
        mSubjectEditor.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                Button button = mSubjectDialog.getButton(Dialog.BUTTON_POSITIVE);
                if (s.toString().length() <= 0) { // characters
                    // not allowed
                    if (button != null) {
                        button.setEnabled(false);
                    }
                } else {
                    if (button != null) {
                        button.setEnabled(true);
                    }
                }
            }
        });
    }

    @Override
    public void onInitGroupResult(final int result, final long threadId, final String chatId) {
        Log.d(TAG, "onInitGroupResult: " + this + chatId);
        if (!mChatId.equals(chatId)) {
            Log.w(TAG, "onInitGroupResult: is not my group: mChat = " + mChatId);
            return;
        }
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                if (mProgressDialog != null && mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
                if (result == IpMessageConsts.GroupActionList.VALUE_SUCCESS) {
                    openCreatedGroup(threadId);
                    if (mIsVisble) {
//                        openCreatedGroup(threadId);
                    } else {

                    }
                } else {
                    Toast.makeText(CreateGroupActivity.this, R.string.create_group_failed,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
    }

    private void openCreatedGroup(long threadId) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setType("vnd.android-dir/mms-sms");
        intent.putExtra("thread_id", threadId);
        intent.setPackage("com.android.mms");
        startActivity(intent);
        finish();
    }

    private CharSequence getMaxByteSequence(CharSequence str, int keep) {
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

    public void onAcceptGroupInvitationResult(int result, long threadId,
            String chatId) {
        // TODO Auto-generated method stub

    }

    public void onRejectGroupInvitationResult(int result, long threadId,
            String chatId) {
        // TODO Auto-generated method stub

    }
}
