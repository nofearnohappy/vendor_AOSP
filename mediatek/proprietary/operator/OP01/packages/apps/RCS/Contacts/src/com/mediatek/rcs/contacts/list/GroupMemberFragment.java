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

package com.mediatek.rcs.contacts.list;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.CountryDetector;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.mediatek.rcs.contacts.R;
import com.mediatek.rcs.contacts.list.GroupListFragment;
import com.mediatek.rcs.contacts.list.GroupListItem;
import com.mediatek.rcs.contacts.util.WeakAsyncTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class GroupMemberFragment extends GroupListFragment {

    private static final String TAG = GroupMemberFragment.class.getSimpleName();
    private static final String RESULT = "com.mediatek.contacts.list.pickdataresult";

    private Context mContext;
    private String[] mExistNumbers;
    private String mCountryCode;
    private Set<String> mNumbers = new HashSet<String>();
    private Set<String> mNumbersE164 = new HashSet<String>();
    
    public static final int GROUP_RESULT_CODE = 1;

    public GroupMemberFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        Intent intent = activity.getIntent();
        mExistNumbers = intent.getStringArrayExtra("ExistNumberArray");
        mCountryCode = intent.getStringExtra("CountryCode");
    }

    private void setCountryCode() {
        if (mCountryCode == null) {
            final CountryDetector countryDetector =
                    (CountryDetector) mContext.getSystemService(Context.COUNTRY_DETECTOR);
            mCountryCode = countryDetector.detectCountry().getCountryIso();
        }
        Log.d(TAG, "setCountryCode: " + mCountryCode);
    }

    private void initFilterNumbers() {   
        setCountryCode();
        String numberE164 = null;
        if (mExistNumbers == null) {
            Log.d(TAG, "initFilterNumbers null");
            return;
        }
        
        for (String number : mExistNumbers) {
            if (number != null && !mNumbers.contains(number)) {
                Log.i(TAG, "initFilterNumbers number: " + number);
                mNumbers.add(number);
                numberE164 = PhoneNumberUtils.formatNumberToE164(number, mCountryCode);
                if (numberE164 != null && !mNumbersE164.contains(numberE164)) {
                    Log.i(TAG, "initFilterNumbers numberE164: " + numberE164);
                    mNumbersE164.add(numberE164);
                }
            }
        }
    }

    private boolean isExistNumber(String number, String numberE164) {
        boolean result = false;
        if (number != null && mNumbers.contains(number)) {
            Log.i(TAG, "isExistNumber number: " + number);
            result = true;
        }
        if (numberE164 != null && mNumbersE164.contains(numberE164)) {
            Log.i(TAG, "isExistNumber numberE164: " + numberE164);
            result = true;
        }
        return result;
    }

    protected OnItemClickListener configOnItemClickListener() {
        return new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                GroupListItem item = mAdapter.getItem(position);
                int count = item.getMemberCount();
                if (count > 0) {
                    new GroupMemberQueryTask(getActivity()).execute(item.getGroupId());
                } else {
                    Log.i(TAG, "onItemClick count: " + count);
                    final Activity activity = getActivity();
                    activity.setResult(Activity.RESULT_CANCELED, new Intent());
                    activity.finish();
                }
            }
        };
    }

    public class GroupMemberQueryTask extends WeakAsyncTask<Long, Void, long[], Activity> {
        private ProgressDialog mProgress;

        public GroupMemberQueryTask(Activity target) {
            super(target);
        }

        @Override
        protected void onPreExecute(final Activity target) {
            Log.d(TAG, "onPreExecute");
            mProgress = new ProgressDialog(target);
            mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgress.show(target, null, target.getText(R.string.please_wait), true);
            super.onPreExecute(target);
        }

        @Override
        protected long[] doInBackground(Activity target, Long... param) {
            long groupId = param[0];

            if (groupId <= 0) {
                Log.e(TAG, "doInBackground groupId error: " + groupId);
                return null;
            }
            Log.d(TAG, "doInBackground groupId: " + groupId);

            final StringBuilder whereBuilder = new StringBuilder();
            whereBuilder.append(Data.MIMETYPE + "='" + GroupMembership.CONTENT_ITEM_TYPE + "'");
            whereBuilder.append(" AND ");
            whereBuilder.append(Data.DATA1 + "=?");
            String sql = "select " + Data.RAW_CONTACT_ID + 
                    " from view_data where (" + whereBuilder + ")";
            Log.i(TAG, "doInBackground sql: " + whereBuilder.toString());

            whereBuilder.delete(0, whereBuilder.length());
            whereBuilder.append("(" + Data.MIMETYPE + " ='");
            whereBuilder.append(Phone.CONTENT_ITEM_TYPE + "') ");
            whereBuilder.append("AND " + Data.RAW_CONTACT_ID + " IN (" + sql);
            whereBuilder.append(")");
            Log.i(TAG, "doInBackground where: " + whereBuilder.toString());

            ArrayList<Long> phoneIdsList = new ArrayList<Long>();
            Cursor cursor = null;
            try {                
                cursor = target.getContentResolver().query(Data.CONTENT_URI, 
                        new String[] { Data._ID, Data.DATA1, Data.DATA4 }, whereBuilder.toString(), 
                        new String[] { String.valueOf(groupId) }, null);
                if (cursor != null) {
                    //init filter numbers
                    if (cursor.getCount() != 0) {
                        initFilterNumbers();
                    }
                    cursor.moveToPosition(-1);
                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(0);
                        String number = cursor.getString(1);
                        String numberE164 = cursor.getString(2);                        
                        if (!isExistNumber(number, numberE164)) {
                            Log.i(TAG, "doInBackground dataId is " + String.valueOf(id));
                            phoneIdsList.add(Long.valueOf(id));
                        }
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                    cursor = null;
                }
            }

            Log.d(TAG, "doInBackground size is " + phoneIdsList.size());
            long[] phoneIds = new long[phoneIdsList.size()];
            int index = 0;
            for (Long id : phoneIdsList) {
                phoneIds[index++] = id.longValue();
            }

            return phoneIds;
        }

        @Override
        protected void onPostExecute(final Activity target, long[] ids) {
            Log.d(TAG, "onPostExecute");
            if (!target.isFinishing() && mProgress != null && mProgress.isShowing()) {
                dismissDialogSafely(mProgress);
                mProgress = null;
            }
            super.onPostExecute(target, ids);
            CheckResult(ids);
            target.finish();
        }

        private void CheckResult(long[] ids) {
            if (ids == null || ids.length == 0) {
                Log.d(TAG, "CheckResult no available member");
                getActivity().setResult(Activity.RESULT_CANCELED, new Intent());
            } else {
                boolean group = getActivity().getIntent().getBooleanExtra("Group", false);
                Log.d(TAG, "CheckResult: " + group);
                if (group) {
                    getActivity().setResult(GROUP_RESULT_CODE, 
                            new Intent().putExtra("checkedids", ids));
                } else {
                    getActivity().setResult(Activity.RESULT_OK, new Intent().putExtra(RESULT, ids));
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
}
