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

import java.util.ArrayList;
import java.util.List;

import com.mediatek.ftprecheck.Condition.SettingViewType;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ConditionsSettingActivity extends Activity {

    private TextView mSettingTitle;
    private Button mConfirmBtn;
    private LinearLayout mSettingBody;
    private LayoutInflater mInflater;

    private int mChapterIndex;
    private ConditionManager mConditionManager;
    private TestCaseChapter mTargetChapter;

    private List<CaseItem> mCaseItemList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        setContentView(R.layout.conditions_setting);
        mInflater = getLayoutInflater();
        mChapterIndex = getIntent().getIntExtra("chapter_index", 0);
        mConditionManager = ConditionManager.getConditionManager(this);
        mConditionManager.loadConditionsInfo();
        mTargetChapter = mConditionManager.getChapterCases(mChapterIndex);

        prepareLayout();
        if (mTargetChapter != null) {
            initLayout(); 
        }
    }

    private void prepareLayout() {
        mSettingTitle = (TextView) findViewById(R.id.setting_title);
        mConfirmBtn = (Button) findViewById(R.id.setting_confirm_btn);
        mConfirmBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onConfirmClick();
            }
        });

        mSettingBody = (LinearLayout) findViewById(R.id.setting_body_layout);
    }

    private void initLayout() {        
        mSettingTitle.setText(mTargetChapter.getId() + " " + mTargetChapter.getName());
        initSettingBody();
    }

    private class CaseItem {
        public TestCase mTestCase;
        public LinearLayout mItem;
        public TextView mCaseId;
        public LinearLayout mConditionsLayout;
        public List<ConditionItem> mConditionItemList;

        public CaseItem(TestCase testCase) {
            mTestCase = testCase;
            mItem = (LinearLayout) mInflater.inflate(R.layout.setting_case_item, null);
            mCaseId = (TextView) mItem.findViewById(R.id.case_id);
            mConditionsLayout = (LinearLayout) mItem.findViewById(R.id.conditions_layout);
            mCaseId.setText(mTestCase.getId() + " " + mTestCase.getName());

            mConditionItemList = new ArrayList<ConditionItem>();
        }
    }

    private class ConditionItem {
        public Condition mCondition;
        public RelativeLayout mItem;

        public ConditionItem(Condition condition, int layoutId) {
            mCondition = condition;
            mItem = (RelativeLayout) mInflater.inflate(layoutId, null);
        }
    }

    private class OneEditTextItem extends ConditionItem {
        public TextView mDescPreSuf;
        public TextView mDescSep;
        public EditText mValueEdit;

        public OneEditTextItem(Condition condition) {
            super(condition, R.layout.edittext_1_item);
            mValueEdit = (EditText) mItem.findViewById(R.id.value_edit);
            mDescSep = (TextView) mItem.findViewById(R.id.desc_sep1);
            mDescPreSuf = (TextView) mItem.findViewById(R.id.desc_pre_suf1);

            mValueEdit.setText(mCondition.getValue());
            mDescSep.setText(mCondition.getDescSeparator());
            if (!mCondition.getDescSuffix().isEmpty()) {
                mDescPreSuf.setText(mCondition.getDescPrefix()
                        + "(" + mCondition.getDescSuffix() + ")");
            } else {
                mDescPreSuf.setText(mCondition.getDescPrefix());
            }
        }
    }

    private class TwoEditTextItem extends ConditionItem {
        public TextView mDescPreSuf;
        public EditText mValueLeftEdit;
        public TextView mDescSep;
        public EditText mValueRightEdit;

        public TwoEditTextItem(Condition condition) {
            super(condition, R.layout.edittext_2_item);
            mValueRightEdit = (EditText) mItem.findViewById(R.id.value_right_edit);
            mDescSep = (TextView) mItem.findViewById(R.id.desc_sep2);
            mValueLeftEdit = (EditText) mItem.findViewById(R.id.value_left_edit);
            mDescPreSuf = (TextView) mItem.findViewById(R.id.desc_pre_suf2);

            mValueRightEdit.setText(mCondition.getValueRight());
            mDescSep.setText(mCondition.getDescSeparator());
            mValueLeftEdit.setText(mCondition.getValueLeft());
            if (!mCondition.getDescSuffix().isEmpty()) {
                mDescPreSuf.setText(mCondition.getDescPrefix()
                        + "(" + mCondition.getDescSuffix() + ")");
            } else {
                mDescPreSuf.setText(mCondition.getDescPrefix());
            }
        }
    }

    private class CheckBoxItem extends ConditionItem {
        public TextView mDescPre;
        public CheckBox mIsCheckBox;

        public CheckBoxItem(Condition condition) {
            super(condition, R.layout.checkbox_item);
            mIsCheckBox = (CheckBox) mItem.findViewById(R.id.is_check);
            mDescPre = (TextView) mItem.findViewById(R.id.desc_pre);

            if (mCondition.needCheck()) {
                mIsCheckBox.setChecked(true);
            } else {
                mIsCheckBox.setChecked(false);
            }
            mDescPre.setText(mCondition.getDescPrefix());
        }
    }

    private void initSettingBody() {
        mCaseItemList = new ArrayList<CaseItem>();

        for (TestCase testCase : mTargetChapter.getTestCaseList()) {
            CaseItem caseItem = new CaseItem(testCase);

            for (Condition condition : testCase.getConditionList()) {
                ConditionItem conditionItem = null;
                if (condition.getSettingViewType().
                        equalsIgnoreCase(SettingViewType.ONE_EDIT_TEXT)) {
                    conditionItem = new OneEditTextItem(condition);
                } else if (condition.getSettingViewType().
                        equalsIgnoreCase(SettingViewType.TWO_EDIT_TEXT)) {
                    conditionItem = new TwoEditTextItem(condition);
                } else if (condition.getSettingViewType().
                        equalsIgnoreCase(SettingViewType.CHECK_BOX)) {
                    conditionItem = new CheckBoxItem(condition);
                } else {
                    throw new IllegalArgumentException(
                            "no such setting view type: " + condition.getSettingViewType());
                }
                caseItem.mConditionsLayout.addView(conditionItem.mItem);
                caseItem.mConditionItemList.add(conditionItem);
            }

            mSettingBody.addView(caseItem.mItem);
            mCaseItemList.add(caseItem);
        }
    }

    private void onConfirmClick() {
        if (!modifyValidate()) {
            return;
        }
        updateConditions();
        finish();
    }

    private boolean modifyValidate() {
        boolean isValid = true;
        for (CaseItem caseItem : mCaseItemList) {
            for (ConditionItem conditionItem : caseItem.mConditionItemList) {
                if (conditionItem.mCondition.getSettingViewType().
                        equalsIgnoreCase(SettingViewType.ONE_EDIT_TEXT)) {
                    if (!editInputValid(((OneEditTextItem) conditionItem).mValueEdit)) {
                        isValid = false;
                        break;
                    }
                } else if (conditionItem.mCondition.getSettingViewType().
                        equalsIgnoreCase(SettingViewType.TWO_EDIT_TEXT)) {
                    if (!editInputValid(((TwoEditTextItem) conditionItem).mValueLeftEdit)
                            || !editInputValid(((TwoEditTextItem) conditionItem).mValueRightEdit)) {
                        isValid = false;
                        break;
                    }
                } else if (conditionItem.mCondition.getSettingViewType().
                        equalsIgnoreCase(SettingViewType.CHECK_BOX)) {
                    continue;
                } else {
                    throw new IllegalArgumentException(
                            "no such setting view type: "
                    + conditionItem.mCondition.getSettingViewType());
                }
            }
            if (!isValid) {
                break;
            }
        }
        if (!isValid) {
            Toast.makeText(ConditionsSettingActivity.this,
                    String.format(getString(R.string.toast_invalid_edit)),
                    Toast.LENGTH_SHORT).show();
        }
        return isValid;
    }

    private boolean editInputValid(EditText editText) {
        if (editText.getText().toString().isEmpty()) {
            return false;
        }
        return true;
    }

    private void updateConditions() {
        //update conditions of current chapter cases
        for (CaseItem caseItem : mCaseItemList) {
            for (ConditionItem conditionItem : caseItem.mConditionItemList) {
                if (conditionItem.mCondition.getSettingViewType().
                        equalsIgnoreCase(SettingViewType.ONE_EDIT_TEXT)) {
                    OneEditTextItem item = (OneEditTextItem) conditionItem;
                    String editValue = item.mValueEdit.getText().toString();
                    if (!editValue.equalsIgnoreCase(item.mCondition.getValue())) {
                        item.mCondition.setValue(editValue);
                    }

                } else if (conditionItem.mCondition.getSettingViewType().
                        equalsIgnoreCase(SettingViewType.TWO_EDIT_TEXT)) {
                    TwoEditTextItem item = (TwoEditTextItem) conditionItem;
                    String editValue = item.mValueLeftEdit.getText().toString();
                    if (!editValue.equalsIgnoreCase(item.mCondition.getValueLeft())) {
                        item.mCondition.setValueLeft(editValue);
                    }
                    editValue = item.mValueRightEdit.getText().toString();
                    if (!editValue.equalsIgnoreCase(item.mCondition.getValueRight())) {
                        item.mCondition.setValueRight(editValue);
                    }

                } else if (conditionItem.mCondition.getSettingViewType().
                        equalsIgnoreCase(SettingViewType.CHECK_BOX)) {
                    CheckBoxItem item = (CheckBoxItem) conditionItem;
                    String checkStr = (item.mIsCheckBox.isChecked() ? "true" : "false");
                    if (!checkStr.equalsIgnoreCase(item.mCondition.getIsCheckString())) {
                        item.mCondition.setIsCheckString(checkStr);
                    }

                } else {
                    throw new IllegalArgumentException(
                            "no such setting view type: "
                    + conditionItem.mCondition.getSettingViewType());
                }
            }
        }

        //update conditions.xml
        mConditionManager.writeConditions(mConditionManager.getCaseChapterList());

        Toast.makeText(ConditionsSettingActivity.this,
                R.string.setting_success,
                Toast.LENGTH_SHORT).show();
    }

}
