/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.configurecheck;

import com.mediatek.configurecheck.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class SubCategoryListActivity extends Activity {

    private static final String TAG = "SubCategoryListActivity";
    private ListView mSubCategoryListView;
    private String[] mSubCategoryArray;
    private ArrayAdapter<String> mSubCategoryAdapter;
    private String mRequiredCategory;
    private int mCTSCateTitlePos;
    private int mCTLTECateTitlePos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        CTSCLog.i(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sub_category_list);

        setTitle(getIntent().getStringExtra("Category"));

        mSubCategoryListView = (ListView) findViewById(R.id.sub_category_list);
        if (isProtocolTestCategory()) {
            mSubCategoryArray = getResources().getStringArray(
                       R.array.protocol_conformance_sub_category);
            mSubCategoryAdapter = new ProtocolCateAdapter(this,
                        R.layout.sub_category_item, mSubCategoryArray);
            getCategoryTitlePos(mSubCategoryArray);
        } else if (isFunctionTestCategory()) {
            mSubCategoryArray = getResources().getStringArray(
                       R.array.function_sub_category);
            mSubCategoryAdapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_list_item_1, mSubCategoryArray);
            getCategoryTitlePos(mSubCategoryArray);
        }else {
            CTSCLog.d(TAG, "here is impossiable ");
        }

        mSubCategoryListView.setAdapter(mSubCategoryAdapter);

        mSubCategoryListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                long arg3) {
                // TODO Auto-generated method stub
                CTSCLog.i(TAG, "onItemClick(): category = " + mSubCategoryArray[arg2]);
                if (!isFunctionTestCategory()) {
                    if (arg2 == mCTSCateTitlePos || arg2 == mCTLTECateTitlePos) {
                        //just do nothing
                        return;
                    }
                }

               if (false == checkProviderIsEmpty(mSubCategoryArray[arg2])) {
                    Intent intent =
                      new Intent(SubCategoryListActivity.this, CheckResultActivity.class);
                    intent.putExtra("Category", mSubCategoryArray[arg2]);
                    startActivity(intent);
                } else {
                    AlertDialog builder = new AlertDialog.Builder(SubCategoryListActivity.this)
                                   .setMessage(getString(R.string.str_noitem_message))
                                   .setPositiveButton(android.R.string.ok, null)
                                   .create();
                    builder.show();
                }
            }
        });
    }

    private void getCategoryTitlePos(String[] categoryArray) {
        for (int i = 0; i < categoryArray.length; i++) {
            if (categoryArray[i].equals(SubCategoryListActivity.this
                    .getString(R.string.CTS_category_title))) {
                mCTSCateTitlePos = i;
            } else if (categoryArray[i].equals(SubCategoryListActivity.this
                    .getString(R.string.Modem_CT_LTE_category_title))){
                mCTLTECateTitlePos = i;
                break;
            }
        }
    }

    private boolean checkProviderIsEmpty(String catogory) {
        CheckItemProviderBase provider = ProviderFactory.getProvider(this, catogory);

        int count = provider.getItemCount();

        if (0 == count) {
            return true;
        }

        return false;
    }

    public Boolean isProtocolTestCategory() {

        Intent intent = getIntent();
        mRequiredCategory = intent.getStringExtra("Category");

        if (mRequiredCategory.equals(
                getString(R.string.protocol_conformance_test))) {
            return true;
        } else if (mRequiredCategory.equals(
                getString(R.string.function_test))) {
            return false;
        } else {
            throw new RuntimeException("No such category!");
        }
    }

    public Boolean isFunctionTestCategory() {

        Intent intent = getIntent();
        mRequiredCategory = intent.getStringExtra("Category");

        if (mRequiredCategory.equals(
                getString(R.string.function_test))) {
            return true;
        } else if (mRequiredCategory.equals(
                getString(R.string.protocol_conformance_test))) {
            return false;
        } else {
            throw new RuntimeException("No such category!");
        }
    }

    private class ProtocolCateAdapter extends ArrayAdapter<String> {

        private LayoutInflater mInflater;
        private int mLayoutResId;

        class ViewHolder {
            private TextView mCategoryTitle;
            private TextView mTestType;
        }

        private ProtocolCateAdapter(Context context, int resourceId, String[] array) {
            super(context, resourceId, array);
            mInflater = LayoutInflater.from(context);
            mLayoutResId = resourceId;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            CTSCLog.d(TAG, "getView() position = " + position + " convertView = " + convertView);
            CTSCLog.d(TAG, "getItem(position) = " + getItem(position));
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = mInflater.inflate(mLayoutResId, null);

                holder.mCategoryTitle = (TextView)convertView.findViewById(R.id.category_title);
                holder.mTestType = (TextView)convertView.findViewById(R.id.test_type);
                convertView.setTag(holder);//to associate(store) the holder as a tag in convertView
            } else {
                holder = (ViewHolder)convertView.getTag();
            }

            if (position == mCTSCateTitlePos || position == mCTLTECateTitlePos) {
                holder.mCategoryTitle.setVisibility(View.VISIBLE);
                holder.mCategoryTitle.setText(getItem(position));
                holder.mTestType.setVisibility(View.GONE);
            } else {
                holder.mCategoryTitle.setVisibility(View.GONE);
                holder.mTestType.setVisibility(View.VISIBLE);
                holder.mTestType.setText(getItem(position));
            }

            return convertView;
        }
    }
}
