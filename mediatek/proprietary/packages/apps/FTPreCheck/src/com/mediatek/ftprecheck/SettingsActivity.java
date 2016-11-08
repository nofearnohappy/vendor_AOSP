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

package com.mediatek.ftprecheck;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class SettingsActivity extends Activity {

    private static final String TAG = "SettingsActivity";

    private EditText mStaticTimer;
    private EditText mDynamicTimer;
    private EditText mTestLocation;
    private ListView mChapterListView;
    private Button mConfirmBtn;

    public static final String PREFS_NAME = "settings";
    public static final String KEY_STATIC_TIMER = "static_timer";
    public static final String KEY_DYNAMIC_TIMER = "dynamic_timer";
    public static final String KEY_TEST_LOCATION = "test_location";
    public static final int DEFAULT_STATIC_TIMER = 60;
    public static final int DEFAULT_DYNAMIC_TIMER = 180;
    private static final int TIMER_MIN = 10;
    private static final int TIMER_MAX = 600;

    private ConditionManager mConditionManager;
    private List<TestCaseChapter> mCaseChapterList;
    private String[] mChapterArray;
    private ArrayAdapter<String> mChapterAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings);
        prepareLayout();
        initLayout();
    }

    private void prepareLayout() {
        mStaticTimer = (EditText) findViewById(R.id.static_timer_edit);
        mDynamicTimer = (EditText) findViewById(R.id.dynamic_timer_edit);
        mTestLocation = (EditText) findViewById(R.id.location_edit);
        mChapterListView = (ListView) findViewById(R.id.chapter_list);
        mConfirmBtn = (Button) findViewById(R.id.confirm_btn);
        mConfirmBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onConfirmClick();
            }
        });
    }

    private void initLayout() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME,
                MODE_PRIVATE);
        mStaticTimer.setText(String.valueOf(settings.getInt(KEY_STATIC_TIMER,
                DEFAULT_STATIC_TIMER)));
        mDynamicTimer.setText(String.valueOf(settings.getInt(KEY_DYNAMIC_TIMER,
                DEFAULT_DYNAMIC_TIMER)));
        mTestLocation.setText(settings.getString(KEY_TEST_LOCATION, ""));

        setupChapterListView();
    }

    private void setupChapterListView() {
        mConditionManager = ConditionManager.getConditionManager(this);
        mConditionManager.loadConditionsInfo();
        mCaseChapterList = mConditionManager.getCaseChapterList();

        if (mCaseChapterList == null ||
            mCaseChapterList.size() == 0 ) {
            return ;
        }
        mChapterArray = new String[mCaseChapterList.size()];
        for (int i = 0; i < mCaseChapterList.size(); i++) {
            String listItem = mCaseChapterList.get(i).getId()
                    + " "
                    + mCaseChapterList.get(i).getName();
            mChapterArray[i] = listItem;
        }

        mChapterAdapter = new ArrayAdapter<String>(SettingsActivity.this,
                android.R.layout.simple_list_item_1, mChapterArray);
        mChapterListView.setAdapter(mChapterAdapter);

        mChapterListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                    long arg3) {
                // TODO Auto-generated method stub
                FTPCLog.i(TAG, "onItemClick(): chapter = " + mChapterArray[arg2]);

                Intent intent = new Intent(SettingsActivity.this,
                        ConditionsSettingActivity.class);
                intent.putExtra("chapter_index", arg2);
                startActivity(intent);
            }
        });
    }

    private void onConfirmClick() {
        if (!inputValidate()) {
            return;
        }
        updatePrefs();
        finish();
    }

    private boolean inputValidate() {
        if (inputTimerValid(mStaticTimer.getText().toString())
                && inputTimerValid(mDynamicTimer.getText().toString())) {
            return true;
        } else {
            Toast.makeText(SettingsActivity.this,
                    String.format(getString(R.string.toast_invalid_timer), TIMER_MIN, TIMER_MAX),
                    Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private boolean inputTimerValid(String input) {
        if (null == input || input.isEmpty()) {
            //it is OK, will use default timer
            return true;
        }
        if (Integer.parseInt(input) < TIMER_MIN
                || Integer.parseInt(input) > TIMER_MAX) {
            return false;
        } else {
            return true;
        }
    }

    private void updatePrefs() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME,
                MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        String str = mStaticTimer.getText().toString();
        if (str.isEmpty()) {
            editor.putInt(KEY_STATIC_TIMER, DEFAULT_STATIC_TIMER);
        } else {
            editor.putInt(KEY_STATIC_TIMER, Integer.parseInt(str));
        }
        str = mDynamicTimer.getText().toString();
        if (str.isEmpty()) {
            editor.putInt(KEY_DYNAMIC_TIMER, DEFAULT_DYNAMIC_TIMER);
        } else {
            editor.putInt(KEY_DYNAMIC_TIMER, Integer.parseInt(str));
        }
        str = mTestLocation.getText().toString();
        editor.putString(KEY_TEST_LOCATION, str);
        editor.commit();

        Toast.makeText(SettingsActivity.this,
                R.string.setting_success,
                Toast.LENGTH_SHORT).show();
    }

}
