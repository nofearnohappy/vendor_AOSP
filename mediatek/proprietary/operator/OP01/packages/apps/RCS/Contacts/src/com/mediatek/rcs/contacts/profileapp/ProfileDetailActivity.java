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
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import com.cmcc.ccs.profile.ProfileService;
import com.mediatek.rcs.contacts.R;

import java.util.Calendar;
import java.util.HashMap;


public class ProfileDetailActivity extends Activity implements
        ProfileManager.ProfileManagerListener,
        DatePickerDialog.OnDateSetListener ,
        ProfileOtherNumberEditor.ProfileOtherNumberEditorListener,
        ProfileVCardBuilder.VcardBuilderCallback {

    private static final int DIALOG_EDIT_BIRTHDAY = 1000;
    private static final int DIALOG_EDIT_COMMON_ITEM = 1001;
    private static final int DIALOG_EDIT_OTHER_NUMBER = 1002;
    private static final int DIALOG_EDIT_NAME = 1003;

    private static final String TAG_DATA_TYPE = "dataType";
    private static final String TAG_TITLE = "title";
    public static final String ARG_CALLING_ACTIVITY = "CALLING_ACTIVITY";
    private static final String TAG = "ProfileDetailActivity";

    private static final int DATE_PICKER_MAX = 2030;
    private static final int DATE_PICKER_MIN = 1930;
    private ProfileInfo mProfile;
    private ProfileManager mProfileMgr;

    private ProfileDetailFragment.ProfileDetailListener mDetailListener
            = new ProfileDetailFragment.ProfileDetailListener() {
        @Override
        public void onItemClick(int position, String dataType, String title) {
            if(dataType == ProfileInfo.BIRTHDAY) {
                showDialog(DIALOG_EDIT_BIRTHDAY);
            } else if (dataType == ProfileInfo.PHONE_NUMBER_SECOND) {
                showDialog(DIALOG_EDIT_OTHER_NUMBER);
            } else if (dataType == ProfileInfo.NAME) {
                showDialog(DIALOG_EDIT_NAME);
            } else if (dataType == ProfileInfo.QR_CODE) {
                Intent i = new Intent();
                i.setClassName(getPackageName(), ProfileQRCodeActivity.class.getName());
                startActivity(i);
            } else {
                Bundle data = new Bundle();
                data.putString(TAG_DATA_TYPE, dataType);
                data.putString(TAG_TITLE, title);
                showDialog(DIALOG_EDIT_COMMON_ITEM, data);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_detail);

        ProfileDetailFragment fragment = (ProfileDetailFragment)getFragmentManager()
                .findFragmentById(R.id.profile_detail);
        fragment.registerListener(mDetailListener);

        mProfileMgr = ProfileManager.getInstance(getApplicationContext());
        mProfile = mProfileMgr.getMyProfileFromLocal();
        mProfileMgr.registerProfileManagerListener(this);
        mProfileMgr.getProfileQRCodeFromServer();
        getActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menu.add(Menu.NONE, Menu.FIRST, 0, R.string.share_profile)
                .setIcon(R.drawable.ic_menu_share_holo_dark)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /* Press home as up, finish current activity */
        if (item.getItemId() == android.R.id.home) {
            finish();
        } else if (item.getItemId() == Menu.FIRST) {
            ProfileVCardBuilder.getInstance()
                    .buildProfileVCard(this, getApplicationContext());
        }
        return super.onOptionsItemSelected(item);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mProfileMgr.unregisterProfileManagerListener(this);
    }

    @Override
    public Dialog onCreateDialog(int id, Bundle data) {
        switch (id) {
            case DIALOG_EDIT_BIRTHDAY: {
                Calendar c = Calendar.getInstance();
                DatePickerDialog datePicker =
                        new DatePickerDialog(this, this, c.get(Calendar.YEAR),
                                c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
                c.set(DATE_PICKER_MIN, Calendar.JANUARY, 1);
                datePicker.getDatePicker().setMinDate(c.getTimeInMillis());
                c.set(DATE_PICKER_MAX, Calendar.DECEMBER, 31);
                datePicker.getDatePicker().setMaxDate(c.getTimeInMillis());
                return datePicker;
            }

            case DIALOG_EDIT_COMMON_ITEM: {
                String title = data.getString(TAG_TITLE);
                String dataType = data.getString(TAG_DATA_TYPE);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder = configureCommonItemEditor(title, dataType, builder);
                AlertDialog dialog = builder.create();
                dialog.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                return dialog;
            }

            case DIALOG_EDIT_OTHER_NUMBER: {
                ProfileOtherNumberEditor editor = ProfileOtherNumberEditor.getInstance();
                Dialog dialog =  editor.createOtherNumberEditor(this, this);
                dialog.getWindow().clearFlags(
                        WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
                dialog.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                return dialog;
            }

            case DIALOG_EDIT_NAME: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder = configureNameItemEditor(builder);
                AlertDialog dialog = builder.create();
                dialog.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                return dialog;
            }

            default:
                return null;
        }
    }

    /**
     * Configure common type editor style.
     * @param title
     * @param dataType
     * @param builder
     * @return Dialog builder
     */
    private AlertDialog.Builder configureCommonItemEditor(String title, final String dataType, AlertDialog.Builder builder) {

        builder.setTitle(getString(R.string.edit) + title);
        View  v = LayoutInflater.from(this).inflate(R.layout.profile_common_item_editor, null);
        final EditText editor = (EditText)v.findViewById(R.id.editor);
        editor.setText(ProfileInfo.getContentByKey(dataType));
        ProfileEditorUtils.addNumberEditorLimit(editor, dataType);
        builder.setView(v);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                removeDialog(DIALOG_EDIT_COMMON_ITEM);
                handleProfileCommonInfoUpdate(editor.getText().toString(), dataType);
            }
        });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                removeDialog(DIALOG_EDIT_COMMON_ITEM);
            }
        });

        return builder;

    }

    /**
     * Configure name type editor style.
     * @param title
     * @param dataType
     * @param builder
     * @return Dialog builder
     */
    private AlertDialog.Builder configureNameItemEditor(AlertDialog.Builder builder) {

        builder.setTitle(getString(R.string.edit) + getString(R.string.profile_info_name));
        View  v = LayoutInflater.from(this).inflate(R.layout.profile_name_item_editor, null);
        final EditText fn_editor = (EditText)v.findViewById(R.id.fn_editor);
        final EditText gn_editor = (EditText)v.findViewById(R.id.gn_editor);
        fn_editor.setText(ProfileInfo.getContentByKey(ProfileInfo.LAST_NAME));
        gn_editor.setText(ProfileInfo.getContentByKey(ProfileInfo.FIRST_NAME));
        ProfileEditorUtils.addNumberEditorLimit(fn_editor, ProfileInfo.NAME);
        ProfileEditorUtils.addNumberEditorLimit(gn_editor, ProfileInfo.NAME);
        builder.setView(v);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                removeDialog(DIALOG_EDIT_NAME);
                HashMap<String, String> map = new HashMap<String, String>();
                map.put(ProfileInfo.FIRST_NAME, checkNotNull(gn_editor.getText().toString()));
                map.put(ProfileInfo.LAST_NAME, checkNotNull(fn_editor.getText().toString()));
                mProfileMgr.updateProfileByType(map);

            }
        });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                removeDialog(DIALOG_EDIT_NAME);
            }
        });

        return builder;

    }

    /**
     * update common type profile information, likes name, birthday, etc...
     * @param newContent
     * @param dataType
     */
    private void handleProfileCommonInfoUpdate(String newContent, String dataType) {
        HashMap<String, String> map = new HashMap<String, String>();

        map.put(dataType, checkNotNull(newContent));
        mProfileMgr.updateProfileByType(map);
    }

    /**
     * check if the str is null, if null return "", else return str.
     * @param str
     * @param String
     */
    private String checkNotNull(String str) {
        return (str == null) ? "" : str;
    }

    /**
     * Override ProfileManagerListener. called when profile information updated.
     * @param flag: :  update flag
     * @param profile: profile information
     */
    @Override
    public void onProfileInfoUpdated(int flag, int operation, ProfileInfo profile) {
        ProfileDetailFragment detailFragment = (ProfileDetailFragment)getFragmentManager()
                .findFragmentById(R.id.profile_detail);
        /* Update profile info to fragment */
        mProfile = profile;
        detailFragment.updateProfileInfo(profile);
        int resId = -1;
        if (flag == ProfileService.OK || flag == ProfileService.NOUPDATE) {
            if (operation == ProfileManager.SERVER_RESULT_GET_PROFILE) {
                resId = R.string.profile_get_sucess;
            } else if (operation == ProfileManager.SERVER_RESULT_GET_PORTRAIT) {
                resId = R.string.profile_get_portrait_sucess;
            } else if (operation == ProfileManager.SERVER_RESULT_SET_PROFILE) {
                resId = R.string.profile_set_sucess;
            } else if (operation == ProfileManager.SERVER_RESULT_SET_PORTRAIT) {
                resId = R.string.profile_set_portrait_sucess;
            }
        } else {
            if (operation == ProfileManager.SERVER_RESULT_GET_PROFILE) {
                resId = R.string.profile_get_fail;
            } else if (operation == ProfileManager.SERVER_RESULT_GET_PORTRAIT) {
                resId = R.string.profile_get_portrait_fail;
            } else if (operation == ProfileManager.SERVER_RESULT_SET_PROFILE) {
                resId = R.string.profile_set_fail;
            } else if (operation == ProfileManager.SERVER_RESULT_SET_PORTRAIT) {
                resId = R.string.profile_set_portrait_fail;
            }
        }
        if (resId > 0) {
            final int id = resId;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ProfileDetailActivity.this, id, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Override ProfileManagerListener. called when contact portrait updated.
     * @param flag :  update flag
     * @param number: contact number.
     * @param icon:   contact portrait
     */
    @Override
    public void onContactIconGotten(int flag, String number, byte[]icon) {

    }

    /**
     * ProfileListener:
     * listener when get Profile QR Code call back.
     * @param result:
     * @param mode:
     */
    public void onGetProfileQRCode (int result, int mode) {
        Log.d(TAG, "onGetProfileQRCode: result = " + result + " mode = " + mode);

    }

    /**
     * ProfileListener:
     * listener when get Profile QR Code mode call back.
     * @param result:
     * @param mode:
     */
    public void onUpdateProfileQRCodeMode (int result, int mode) {
        Log.d(TAG, "onUpdateProfileQRCodeMode: result = " + result + " mode = " + mode);

    }

    /**
     * OnDatePickerListener, called when date setted done
     * @param datePicker
     * @param year
     * @param month
     * @param day
     */
    @Override
    public void onDateSet(DatePicker datePicker, int year, int month, int day) {

        StringBuffer buffer = new StringBuffer();
        buffer.append(String.valueOf(year));
        if (month < 9) {
            buffer.append("0");
        }
        buffer.append(String.valueOf(month + 1));
        if (day < 10) {
            buffer.append("0");
        }
        buffer.append(String.valueOf(day));
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(ProfileInfo.BIRTHDAY, buffer.toString());
        mProfileMgr.updateProfileByType(map);
    }

    /**
    * ProfileOtherNumberEditor.ProfileOtherNumberEditorListener
    * onConfirmClicked
    **/
    @Override
    public void onConfirmClicked() {
        removeDialog(DIALOG_EDIT_OTHER_NUMBER);

    }

    /**
    * ProfileOtherNumberEditor.ProfileOtherNumberEditorListener
    * onCancelClicked
    **/
    @Override
    public void onCancelClicked() {
        removeDialog(DIALOG_EDIT_OTHER_NUMBER);
    }

    /**
    * ProfileVCardBuilder.VcardBuilderCallback
    *@Param: uri. the profile vcard uri.
    **/
    @Override
    public void onVcardBuildDone(Uri uri) {
        Log.i(TAG, uri.toString());

        final Intent intent = new Intent(Intent.ACTION_SEND);

        intent.setType(Contacts.CONTENT_VCARD_TYPE);
        intent.putExtra("userProfile", "true");

        intent.putExtra(Intent.EXTRA_STREAM, uri);

        intent.putExtra(ARG_CALLING_ACTIVITY,
                ProfileDetailActivity.class.getName());

        // Launch chooser to share contact via applications
        final CharSequence chooseTitle = getText(R.string.share_via);
        final Intent chooseIntent = Intent.createChooser(intent, chooseTitle);

        try {
            this.startActivity(chooseIntent);
        } catch (final ActivityNotFoundException ex) {
            Toast.makeText(this, R.string.share_error, Toast.LENGTH_SHORT).show();
        }
    }

}
