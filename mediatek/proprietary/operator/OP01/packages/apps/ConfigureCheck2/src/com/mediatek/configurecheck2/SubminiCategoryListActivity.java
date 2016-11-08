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

package com.mediatek.configurecheck2;

import com.mediatek.configurecheck2.R;

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

public class SubminiCategoryListActivity extends Activity {

    private static final String TAG = "SubminiCategoryListActivity";
    private ListView mSubCategoryListView;
    private String[] mSubCategoryArray;
    private ArrayAdapter<String> mSubCategoryAdapter;
    private String mInputCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        CTSCLog.i(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sub_mini_category_list);
        setTitle(getIntent().getStringExtra("Category"));

        mSubCategoryListView = (ListView) findViewById(R.id.sub_mini_category_list);
        mInputCategory = getIntent().getStringExtra("Category");
        if (mInputCategory.equals(SubminiCategoryListActivity.this
                            .getString(R.string.tds_pro_con))) {
            mSubCategoryArray = getResources().getStringArray(
                    R.array.tds_pro_sub_category);
            mSubCategoryAdapter = new ProtocolCateAdapter(this,
                    R.layout.sub_mini_category_item, mSubCategoryArray);
        } else if (mInputCategory.equals(SubminiCategoryListActivity.this
                            .getString(R.string.lte_usim))){
            mSubCategoryArray = getResources().getStringArray(
                    R.array.lte_usim_sub_category);
            mSubCategoryAdapter = new ProtocolCateAdapter(this,
                    R.layout.sub_mini_category_item, mSubCategoryArray);
        } else if (mInputCategory.equals(SubminiCategoryListActivity.this
                            .getString(R.string.lte_ns_iot))){
            mSubCategoryArray = getResources().getStringArray(
                    R.array.lte_ns_iot_sub_category);
            mSubCategoryAdapter = new ProtocolCateAdapter(this,
                    R.layout.sub_mini_category_item, mSubCategoryArray);
        } else if (mInputCategory.equals(SubminiCategoryListActivity.this
                            .getString(R.string.lte_ipv6))){
            mSubCategoryArray = getResources().getStringArray(
                    R.array.lte_IPv6_sub_category);
            mSubCategoryAdapter = new ProtocolCateAdapter(this,
                    R.layout.sub_mini_category_item, mSubCategoryArray);
        }else if (mInputCategory.equals(SubminiCategoryListActivity.this
                            .getString(R.string.LTE_internal_roaming))){
            mSubCategoryArray = getResources().getStringArray(
                    R.array.lte_internal_roaming_category);
            mSubCategoryAdapter = new ProtocolCateAdapter(this,
                    R.layout.sub_mini_category_item, mSubCategoryArray);
        } else if (mInputCategory.equals(SubminiCategoryListActivity.this
                            .getString(R.string.ns_iot_6291))) {
            mSubCategoryArray = getResources().getStringArray(
                    R.array.ns_iot_6291_category);
            mSubCategoryAdapter = new ProtocolCateAdapter(this,
                    R.layout.sub_mini_category_item, mSubCategoryArray);
        } else if (mInputCategory.equals(SubminiCategoryListActivity.this
                            .getString(R.string.nv_iot_6291))) {
            mSubCategoryArray = getResources().getStringArray(
                    R.array.nv_iot_6291_category);
            mSubCategoryAdapter = new ProtocolCateAdapter(this,
                    R.layout.sub_mini_category_item, mSubCategoryArray);
        } else {
            mSubCategoryArray = getResources().getStringArray(
                R.array.protocol_conformance_sub_category);
            mSubCategoryAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, mSubCategoryArray);
        }

        mSubCategoryListView.setAdapter(mSubCategoryAdapter);

        mSubCategoryListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                long arg3) {
                // TODO Auto-generated method stub
                CTSCLog.i(TAG, "onItemClick(): category = " + mSubCategoryArray[arg2]);
                Intent intent =
                  new Intent(SubminiCategoryListActivity.this, CheckResultActivity.class);
                intent.putExtra("Category", mSubCategoryArray[arg2]);
                startActivity(intent);
            }
        });
    }

    private class ProtocolCateAdapter extends ArrayAdapter<String> {

        private LayoutInflater mInflater;
        private int mLayoutResId;

        class ViewHolder {
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

                holder.mTestType = (TextView)convertView.findViewById(R.id.test_type_mini);
                convertView.setTag(holder);//to associate(store) the holder as a tag in convertView
            } else {
                holder = (ViewHolder)convertView.getTag();
            }
            holder.mTestType.setVisibility(View.VISIBLE);
            holder.mTestType.setText(getItem(position));

            return convertView;
        }
    }
}
