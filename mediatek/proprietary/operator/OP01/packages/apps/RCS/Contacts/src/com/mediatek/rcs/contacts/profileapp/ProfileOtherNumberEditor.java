/*
* This software/firmware and related documentation ("MediaTek Software") are
* protected under relevant copyright laws. The information contained herein
* is confidential and proprietary to MediaTek Inc. and/or its licensors.
* Without the prior written permission of MediaTek inc. and/or its licensors,
* any reproduction, modification, use or disclosure of MediaTek Software,
* and information contained herein, in whole or in part, shall be strictly prohibited.
*/
/* MediaTek Inc. (C) 2014. All rights reserved.
*
* BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
* THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
* RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
* AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
* EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
* NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
* SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
* SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
* THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
* THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
* CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
* SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
* STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
* CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
* AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
* OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
* MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
*
* The following software/firmware and/or related documentation ("MediaTek Software")
* have been modified by MediaTek Inc. All revisions are subject to any receiver's
* applicable license agreements with MediaTek Inc.
*/

package com.mediatek.rcs.contacts.profileapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.mediatek.rcs.contacts.R;

import java.util.ArrayList;

/**
 * ProfileOtherNumberEditor: Profile other number editor.
 */
public class ProfileOtherNumberEditor {

    public Activity mActivity;
    private LinearLayout mListContainer;
    private TextView mAddNew;
    private ProfileInfo mProfile;

    public String[] mNumberTypeSet = new String[] {
            ProfileInfo.OTHER_NUMBER_HOME,
            ProfileInfo.OTHER_NUMBER_WORK,
            ProfileInfo.OTHER_NUMBER_FIXED,
            ProfileInfo.OTHER_NUMBER_OTHER};
    public int[] mNumberTypeTitleSet = new int[] {
            R.string.profile_info_home_number,
            R.string.profile_info_work_number,
            R.string.profile_info_fixed_number,
            R.string.profile_info_other_number
    };

    public static final int VALUE_NUMBER_TYPE_HOME = 0;
    public static final int VALUE_NUMBER_TYPE_WORK = 1;
    public static final int VALUE_NUMBER_TYPE_FIXED = 2;
    public static final int VALUE_NUMBER_TYPE_OTHER = 3;
    public static final int VALUE_NUMBER_TYPE_SIZE = 4;

    private static final int MAX_OTHER_NUMBER_COUNT = 6;
    private static final String TAG = ProfileOtherNumberEditor.class.getName();

    private static ProfileOtherNumberEditor sInstance;
    private ProfileOtherNumberEditorListener mListener;
    private int mLastEmptyItem = -1;

    /**
     * Private constuctor.
     */
    private ProfileOtherNumberEditor() {

    }

    /**
     * Single instance achive method.
     * @return : current instance
     */
    public static ProfileOtherNumberEditor getInstance() {
        if (sInstance == null) {
            sInstance = new ProfileOtherNumberEditor();
        }
        return sInstance;
    }

    /**
     * Create list view item.
     * @param : number
     * @param : type
     * @return : ViewEntry
     */
    private View createItemView(String number, int type) {
        View v = LayoutInflater.from(mActivity).inflate(R.layout.other_number_editor_list_item, null);
        int position = mListContainer.getChildCount();
        final EditText editor = (EditText)v.findViewById(R.id.number);
        ProfileEditorUtils.addNumberEditorLimit(editor, ProfileInfo.PHONE_NUMBER_SECOND);
        editor.setTag(Integer.valueOf(position));
        final ImageButton delete = (ImageButton)v.findViewById(R.id.delete);
        final Spinner spinner = (Spinner)v.findViewById(R.id.type);
        configureEditText(editor, delete, number);
        configureDeleteBtn(editor, delete);
        configureTypeSpinner(spinner, type);
        return v;
    }
    
    /**
     * Refresh list view items.
     */
    private void refreshListItems() {
        mLastEmptyItem = -1;
        for (int i = 0; i < mListContainer.getChildCount(); i++) {
            View v = mListContainer.getChildAt(i);
            final EditText editor = (EditText)v.findViewById(R.id.number);
            editor.setTag(Integer.valueOf(i));
            String text = editor.getText().toString();
            if (text == null || text.equals("")) {
                mLastEmptyItem = i;
            }
        }
    }
    /**
     * Init other number list data.
     * @param : void
     * @return : void
     */
    private void initOtherNumberList() {

        mProfile = ProfileManager.getInstance(mActivity)
                .getMyProfileFromLocal();

        ArrayList<ProfileInfo.OtherNumberInfo> otherNumberArrayList
                = new ArrayList<ProfileInfo.OtherNumberInfo>();
        otherNumberArrayList.addAll(mProfile.getAllOtherNumber());

        if (otherNumberArrayList.size() == 0) {
            mListContainer.addView(createItemView("", VALUE_NUMBER_TYPE_HOME));
            mAddNew.setVisibility(View.GONE);
        } else {
            for (ProfileInfo.OtherNumberInfo info : otherNumberArrayList) {
                mListContainer.addView(createItemView(info.number, info.type));
            }
            mAddNew.setVisibility(
                    mListContainer.getChildCount() < MAX_OTHER_NUMBER_COUNT ?
                            View.VISIBLE : View.GONE);
        }
    }

    /**
     * create other number editor dialog.
     * @param activity ProfileEditorActivity
     * @param listener ProfileOtherNumberEditorListener
     * @return editor dialog
     */
    public Dialog createOtherNumberEditor(Activity activity, ProfileOtherNumberEditorListener listener) {

        mActivity = activity;
        mListener = listener;

        final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(mActivity.getString(R.string.edit)
                + mActivity.getString(R.string.profile_info_other_number));
        final View v = LayoutInflater.from(mActivity).inflate(R.layout.other_number_editor, null);
        mListContainer = (LinearLayout)v.findViewById(R.id.list_container);

        mAddNew = (TextView) v.findViewById(R.id.add_new);
        mAddNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mListContainer.addView(createItemView("", VALUE_NUMBER_TYPE_HOME));
                view.setVisibility(View.GONE);
            }
        });
        v.requestFocus();
        builder.setView(v);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int j) {
                ArrayList<ProfileInfo.OtherNumberInfo> list = new ArrayList<ProfileInfo.OtherNumberInfo>();
                ProfileInfo.clearOtherNumbers();
                
                for (int i = 0; i < mListContainer.getChildCount(); i++) {
                    View v = mListContainer.getChildAt(i);
                    EditText editor = (EditText)v.findViewById(R.id.number);
                    Spinner type = (Spinner)v.findViewById(R.id.type);
                    String number = editor.getText().toString();
                    if (number == null || number.equals("")) {
                        continue;
                    }
                    int selection = type.getSelectedItemPosition();
                    String key = ProfileInfo
                            .getOtherNumberKeyByType(mNumberTypeSet[selection]);
                    list.add(new ProfileInfo.OtherNumberInfo(selection, number, key));
                }
                ProfileManager.getInstance(mActivity).updateProfileOtherNumber(list);
                mListener.onConfirmClicked();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mListener.onCancelClicked();
            }
        });

        initOtherNumberList();
        return builder.create();
    }

    /**
       * configure editText attribute. such as onclick listener, text change listener.
       * @param position : list position.
       * @param editor : the edit text.
       * @param delete : the delete button.
       */
    private void configureEditText(final EditText editor, final ImageButton delete, String number) {

        editor.setHint(R.string.profile_info_phone_number);
        editor.setText(number);
        if (number == null || number.equals("")) {
            editor.requestFocus();
            mLastEmptyItem = (Integer)editor.getTag();
        }
        editor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text,
                                          int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence text,
                                      int start, int count, int after) {
            }
            @Override
            public void afterTextChanged(Editable editable) {

                String text = editable.toString();
                int position = (Integer)editor.getTag();
                Log.d(TAG, "afterTextChanged, text = " + text + "position = " + position);
                if (text == null || text.equals("")) {
                    delete.setVisibility(View.INVISIBLE);
                    mAddNew.setVisibility(View.GONE);
                    if (mLastEmptyItem >= 0 && mLastEmptyItem != position
                                && mLastEmptyItem < mListContainer.getChildCount()) {
                        mListContainer.removeViewAt(mLastEmptyItem);
                        refreshListItems();
                    }
                    mLastEmptyItem = (Integer)editor.getTag();
                } else {
                    if (mLastEmptyItem == position) {
                        mLastEmptyItem = -1;
                    }
                    delete.setVisibility(View.VISIBLE);
                    mAddNew.setVisibility(
                            mListContainer.getChildCount() < MAX_OTHER_NUMBER_COUNT
                                    && mLastEmptyItem < 0 ?
                                    View.VISIBLE : View.GONE);
                }
                
            }
        });
    }

  /**
     * configure delete button attribute. such as onclick listener
     * @param position : list position.
     * @param delete : the delete button.
     */
    private void configureDeleteBtn(final EditText editor, final ImageButton delete) {

        String number = editor.getText().toString();
        if (number == null || number.equals("")) {
            delete.setVisibility(View.INVISIBLE);
        } else {
            delete.setVisibility(View.VISIBLE);
        }
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = (Integer)editor.getTag();
                if (mListContainer.getChildCount() == 1) {
                    View v = mListContainer.getChildAt(0);
                    final EditText editor = (EditText)v.findViewById(R.id.number);
                    editor.setText("");
                    mLastEmptyItem = 0;
                    mAddNew.setVisibility(View.GONE);
                } else {
                    mListContainer.removeViewAt(position);
                    refreshListItems();
                    mAddNew.setVisibility(mLastEmptyItem >= 0 ? View.GONE : View.VISIBLE);
                }
            }
        });
    }

  /**
     * configure spinner attribute. such as onclick listener
     * @param position : list position.
     * @param type : the spinner button.
     */
    private void configureTypeSpinner(Spinner type, int selection) {
        type.setAdapter(new SpinnerAdapter());
        type.setSelection(selection);
    }

  /**
     * SpinnerAdapter.
     * Other number type spinner adapter
     */
    private class SpinnerAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return VALUE_NUMBER_TYPE_SIZE;
        }

        @Override
        public java.lang.Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mActivity)
                        .inflate(R.layout.other_number_typer_spinner_item, null);
            }
            TextView text = (TextView) convertView.findViewById(R.id.textView);
            text.setText(mNumberTypeTitleSet[position]);
            return convertView;
        }
    }

    /**
     * ProfileOtherNumberEditorListener
     * Current class listener.
     */
    interface ProfileOtherNumberEditorListener {

        void onConfirmClicked();

        void onCancelClicked();

    }
}
