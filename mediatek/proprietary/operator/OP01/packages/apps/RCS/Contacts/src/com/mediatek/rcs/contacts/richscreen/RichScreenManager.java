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

package com.mediatek.rcs.contacts.richscreen;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import android.widget.Toast;

import com.mediatek.rcs.contacts.R;
import com.mediatek.rcs.contacts.util.WeakAsyncTask;

import java.util.ArrayList;

/**
 * Manager that set rich screen for contacts or groups.
 */
public class RichScreenManager  {
    private static final String TAG = RichScreenManager.class.getSimpleName();

    private static final String MODE_SINGLE_CONTACT = "1";
    private static final String MODE_GROUP = "2";
    private static final String MODE_ALL_CONTACT = "3";
    private static final String PKG_NAME_RICH_SCREEN = "com.cmdm.rcs";

    private static final String[] PHONES_PROJECTION = new String[] {
            Phone.NUMBER,
            Phone.DISPLAY_NAME,
    };
    private static final int PHONE_NUMBER = 0;
    private static final int DISPLAY_NAME = 1;

    private static final int NO_ERROR = 0;
    private static final int NO_NUMBER = -1;
    private static final int NO_PACKAGE = -2;

    private static final long INVALID_ID = -1;

    private Context mContext;

    /**
     * constructed function.
     * @param context Context
     */
    public RichScreenManager(Context context) {
        mContext = context;
    }

    /**
     * launch rich screen apk from one contact.
     * @param uri Uri
     * @param context Context
     */
    public void startRichScreenForSingleContact(Uri uri, Context context) {
        Activity activity = (Activity) context;
        new ContactQueryTask(activity).execute(uri);
    }

    /**
     * Query name and number of one contact.
     */
    public class ContactQueryTask extends WeakAsyncTask<Uri, Void, QueryResult, Activity> {
        private ProgressDialog mProgress;

         /**
             * constructed function.
             * @param target Activity
             */
        public ContactQueryTask(Activity target) {
            super(target);
        }

        @Override
        protected void onPreExecute(final Activity target) {
            Log.d(TAG, "ContactQueryTask onPreExecute");
            //mProgress = new ProgressDialog(target);
            //mProgress.setMessage(mContext.getText(R.string.please_wait));
            //mProgress.setCancelable(false);
            //mProgress.setIndeterminate(true);
            //mProgress.show();
            super.onPreExecute(target);
        }

        @Override
        protected QueryResult doInBackground(Activity target, Uri... param) {
            Uri uri = param[0];
            QueryResult queryResult;

            Cursor cursor = null;
            long contactId = INVALID_ID;
            try {
                cursor = target.getContentResolver().query(uri,
                        new String[]{Contacts._ID}, null, null, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        contactId = cursor.getLong(0);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                    cursor = null;
                }
            }

            if (contactId != INVALID_ID) {
                Log.d(TAG, "ContactQueryTask contactId: " + contactId);
                ArrayList<String> phoneList = new ArrayList<String>();
                ArrayList<String> nameList = new ArrayList<String>();
                final StringBuilder whereBuilder = new StringBuilder();
                whereBuilder.append(RawContacts.CONTACT_ID + "=?");
                try {
                    cursor = target.getContentResolver().query(Phone.CONTENT_URI,
                            PHONES_PROJECTION, whereBuilder.toString(),
                            new String[] { String.valueOf(contactId) }, null);
                    if (cursor != null) {
                        while (cursor.moveToNext()) {
                            String number = cursor.getString(0);
                            Log.i(TAG, "ContactQueryTask number: " + number);
                            phoneList.add(number);
                            String name = cursor.getString(1);
                            Log.i(TAG, "ContactQueryTask name: " + name);
                            //name is same
                            nameList.add(name);
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                        cursor = null;
                    }
                }
                queryResult = encodeQueryResult(target, MODE_SINGLE_CONTACT, phoneList, nameList);
            } else {
                queryResult = null;
            }
            //wait ProgressDialog to show
            //try {
            //    Thread.sleep(1000);
            //} catch (Exception e) {
            //}
            return queryResult;
        }

        @Override
        protected void onPostExecute(final Activity target, QueryResult queryResult) {
            Log.d(TAG, "ContactQueryTask onPostExecute");
            //if (!target.isFinishing() && mProgress != null && mProgress.isShowing()) {
            //    dismissDialogSafely(mProgress);
            //    mProgress = null;
            //}
            super.onPostExecute(target, queryResult);
            if (queryResult != null) {
                decodeQueryResult(target, queryResult, MODE_SINGLE_CONTACT);
            }
        }
    }

    /**
     * launch rich screen apk from one group.
     * @param groupId group id
     * @param context Context
     */
    public void startRichScreenForGroup(long groupId, Context context) {
        Log.i(TAG, "group id: " + groupId);
        if (groupId > 0) {
            Activity activity = (Activity) context;
            new GroupQueryTask(activity).execute(groupId);
        }
    }

    /**
     * Query names and numbers of serveral contacts in one group.
     */
    public class GroupQueryTask extends WeakAsyncTask<Long, Void, QueryResult, Activity> {
        private ProgressDialog mProgress;

         /**
             * constructed function.
             * @param target Activity
             */
        public GroupQueryTask(Activity target) {
            super(target);
        }

        @Override
        protected void onPreExecute(final Activity target) {
            Log.d(TAG, "GroupQueryTask onPreExecute");
            mProgress = new ProgressDialog(target);
            mProgress.setMessage(mContext.getText(R.string.please_wait));
            mProgress.setCancelable(false);
            mProgress.setIndeterminate(true);
            mProgress.show();
            super.onPreExecute(target);
        }

        @Override
        protected QueryResult doInBackground(Activity target, Long... param) {
            long groupId = param[0];

            final StringBuilder whereBuilder = new StringBuilder();
            whereBuilder.append(Data.MIMETYPE + "='" + GroupMembership.CONTENT_ITEM_TYPE + "'");
            whereBuilder.append(" AND ");
            whereBuilder.append(Data.DATA1 + "=?");
            String sql = "select " + Data.RAW_CONTACT_ID +
                    " from view_data where (" + whereBuilder + ")";

            whereBuilder.delete(0, whereBuilder.length());
            whereBuilder.append("(" + Data.MIMETYPE + " ='");
            whereBuilder.append(Phone.CONTENT_ITEM_TYPE + "') ");
            whereBuilder.append("AND " + Data.RAW_CONTACT_ID + " IN (" + sql);
            whereBuilder.append(")");

            ArrayList<String> phoneList = new ArrayList<String>();
            ArrayList<String> nameList = new ArrayList<String>();
            Cursor cursor = null;
            try {
                cursor = target.getContentResolver().query(Phone.CONTENT_URI, PHONES_PROJECTION,
                        whereBuilder.toString(), new String[] { String.valueOf(groupId) }, null);
                if (cursor != null) {
                    cursor.moveToPosition(-1);
                    while (cursor.moveToNext()) {
                        String number = cursor.getString(0);
                        Log.i(TAG, "GroupQueryTask number: " + number);
                        phoneList.add(number);
                        String name = cursor.getString(1);
                        Log.i(TAG, "GroupQueryTask name: " + name);
                        nameList.add(name);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                    cursor = null;
                }
            }

            return encodeQueryResult(target, MODE_GROUP, phoneList, nameList);
        }

        @Override
        protected void onPostExecute(final Activity target, QueryResult queryResult) {
            Log.d(TAG, "GroupQueryTask onPostExecute");
            if (!target.isFinishing() && mProgress != null && mProgress.isShowing()) {
                dismissDialogSafely(mProgress);
                mProgress = null;
            }
            super.onPostExecute(target, queryResult);
            decodeQueryResult(target, queryResult, MODE_GROUP);
        }
    }

    /**
     * Query result struct.
     */
    public final class QueryResult {
        private final Intent mIntent;
        private final int mResult;

         /**
             * constructed function.
             * @param intent Intent
             * @param result int
             */
        public QueryResult(Intent intent, int result) {
            mIntent = intent;
            mResult = result;
        }
    }

    private QueryResult encodeQueryResult(Activity target, String mode,
            ArrayList<String> phoneList, ArrayList<String> nameList) {
        Intent intent = null;
        int result = NO_ERROR;
        PackageManager pm = target.getPackageManager();
        try {
            ApplicationInfo info = pm.getApplicationInfo(PKG_NAME_RICH_SCREEN, 0);
        } catch (NameNotFoundException e) {
            result = NO_PACKAGE;
        }
        if (phoneList.size() > 0) {
            intent = new Intent("com.cmdm.rcs.RCSEntryAction");
            intent.putStringArrayListExtra("MsisdnList", phoneList);
            intent.putStringArrayListExtra("NameList", nameList);
            intent.putExtra("Mode", mode);
        } else {
            result = NO_NUMBER;
        }
        return new QueryResult(intent, result);
    }

    private void decodeQueryResult(Activity target, QueryResult queryResult, String mode) {
        Intent intent = queryResult.mIntent;
        int result = queryResult.mResult;
        if (result == NO_PACKAGE) {
            //show no rich screen package
            Log.d(TAG, "decodeQueryResult no package");
            String noPackage = mContext.getResources().
                    getString(R.string.please_download_it_plugin_center);
            Toast.makeText(target, noPackage, Toast.LENGTH_SHORT).show();
        } else if (result == NO_NUMBER) {
            //show no number toast
            Log.d(TAG, "decodeQueryResult no number");
            String noNumber;
            if (mode.equals(MODE_SINGLE_CONTACT)) {
                noNumber = mContext.getResources().getString(R.string.contact_no_number);
            } else {
                noNumber = mContext.getResources().getString(R.string.group_member_no_number);
            }
            Toast.makeText(target, noNumber, Toast.LENGTH_SHORT).show();
        } else {
            try {
                target.startActivity(intent);
            } catch (ActivityNotFoundException ex) {
                String activityNoFound = mContext.getResources().
                        getString(R.string.activity_no_found);
                Toast.makeText(mContext, activityNoFound, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void dismissDialogSafely(ProgressDialog dialog) {
        try {
            dialog.dismiss();
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "IllegalArgumentException");
        }
    }

}
