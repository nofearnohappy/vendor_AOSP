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


import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.SystemProperties;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;


public class ManSelfTestActivity extends Activity {

    private static final String TAG = "CategoryListActivity";
    private String mCtscVerStr;
    private ListView mSelfTestCateList;
    private String[] mSelfTestCateArray;
    private ArrayAdapter<String> mSelfTestCateAdapter;
    private ListView mSendTestCateList;
    private String[] mSendTestCateArray;
    private ArrayAdapter<String> mSendTestCateAdapter;
    private ListView mOthersCateList;
    private String[] mOthersCateArray;
    private ArrayAdapter<String> mOthersCateAdapter;

    private static final int MENU_CTSC_VER = 1;
    private static final String RO_MTBF = "ro.mtk_ctsc_mtbf_intersup";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        CTSCLog.i(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.self_test);

        mCtscVerStr = getCTSCVersion();
        mCtscVerStr = String.format(getString(R.string.CTSC_version), mCtscVerStr);

        setupSendTestListview();
        setupSelfTestListview();
        setupOthersListview();
    }


    private boolean checkIsSubCategory(int pos) {
        if(pos < mSelfTestCateArray.length) {
            if (mSelfTestCateArray[pos].equals(ManSelfTestActivity.this
                    .getString(R.string.protocol_conformance_test))) {
                return true;
            } else if (mSelfTestCateArray[pos].equals(ManSelfTestActivity.this
                 .getString(R.string.function_test))) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private void setupSelfTestListview() {
        mSelfTestCateList = (ListView) findViewById(R.id.manufacturer_self_test_list);
        mSelfTestCateArray = getResources().getStringArray(R.array.category_manufacturer_self_test);
        mSelfTestCateAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, mSelfTestCateArray);
        mSelfTestCateList.setAdapter(mSelfTestCateAdapter);

        mSelfTestCateList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                    long arg3) {
                // TODO Auto-generated method stub
                CTSCLog.i(TAG, "onItemClick(): category = " + mSelfTestCateArray[arg2]);

                if (checkIsSubCategory(arg2)) {
                    Intent intent = new Intent(ManSelfTestActivity.this,
                        SubCategoryListActivity.class);
                    intent.putExtra("Category", mSelfTestCateArray[arg2]);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(ManSelfTestActivity.this,
                        CheckResultActivity.class);
                    intent.putExtra("Category", mSelfTestCateArray[arg2]);
                    startActivity(intent);
                }
            }
        });
    }


    private void setupSendTestListview() {
        mSendTestCateList = (ListView) findViewById(R.id.CT_send_test_list);
        if (SystemProperties.getInt(RO_MTBF, 1) == 0) {
             mSendTestCateList.setVisibility(View.GONE);
             TextView mTitle = (TextView) findViewById(R.id.CT_send_test_list);
             View mView = (View) findViewById(R.id.single_line2);
             mTitle.setVisibility(View.GONE);
             mView.setVisibility(View.GONE);
        }
        mSendTestCateArray = getResources().getStringArray(R.array.category_CT_send_test);
        mSendTestCateAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, mSendTestCateArray);
        mSendTestCateList.setAdapter(mSendTestCateAdapter);

        mSendTestCateList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                    long arg3) {
                // TODO Auto-generated method stub
                CTSCLog.i(TAG, "onItemClick(): category = " + mSendTestCateArray[arg2]);

                Intent intent = new Intent(ManSelfTestActivity.this,
                        CheckResultActivity.class);
                intent.putExtra("Category", mSendTestCateArray[arg2]);
                startActivity(intent);
            }
        });
    }

    private void setupOthersListview() {
        mOthersCateList = (ListView) findViewById(R.id.others_list);
        mOthersCateArray = getResources().getStringArray(R.array.category_others);
        mOthersCateAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, mOthersCateArray);
        mOthersCateList.setAdapter(mOthersCateAdapter);

        mOthersCateList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                    long arg3) {
                // TODO Auto-generated method stub
                CTSCLog.i(TAG, "onItemClick(): category = " + mOthersCateArray[arg2]);
                Intent intent = new Intent(ManSelfTestActivity.this, HelpActivity.class);
                intent.putExtra("TestType", "ManSelfTest");
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // TODO Auto-generated method stub
        menu.add(0, MENU_CTSC_VER, 0, getString(R.string.menu_CTSC_ver));

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        switch (item.getItemId()) {
        case MENU_CTSC_VER:
            showVerionDlg(mCtscVerStr);
            break;
        default:
            break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showVerionDlg(String text) {
        final Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(text);
        builder.setPositiveButton("OK",  null);
        builder.create().show();
    }

    public String getCTSCVersion() {
        PackageManager pm = getPackageManager();
        PackageInfo pInfo = null;
        try {
            pInfo = pm.getPackageInfo(getPackageName(), 0);
        } catch (NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return "";
        }
        return pInfo.versionName;
    }
}
