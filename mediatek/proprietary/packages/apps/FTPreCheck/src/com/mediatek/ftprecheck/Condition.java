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

public class Condition {

    private String mDescPre;
    private String mDescSep;
    private String mDescSuf;
    private String mCheckItemName;
    private String mCheckType;        //Behavior type of check item
    private String mSettingViewType;
    private String mValueLeft;        //one kind of condition value
    private String mValueRight;       //one kind of condition value
    private String mValue;            //one kind of condition value
    private String mIsCheckStr;
    private String mJudgeType;        //type of judge rule

    private CheckItemBase mCheckItem = null; //will be create when start check this condition

    /**
     * Instruct SettingsActivity what kind of view to inflate dynamically for condition setting.
     */
    final class SettingViewType {
        public static final String ONE_EDIT_TEXT = "EditText_1";
        public static final String TWO_EDIT_TEXT = "EditText_2";
        public static final String CHECK_BOX = "CheckBox";
    }

    public Condition() {

    }

    public String getDescPrefix() {
        return mDescPre;
    }

    public void setDescPrefix(String descPre) {
        this.mDescPre = descPre;
    }

    public String getDescSeparator() {
        return mDescSep;
    }

    public void setDescSeparator(String descSep) {
        this.mDescSep = descSep;
    }

    public String getDescSuffix() {
        return mDescSuf;
    }

    public void setDescSuffix(String descSuf) {
        this.mDescSuf = descSuf;
    }

    public String getCheckItemName() {
        return mCheckItemName;
    }

    public void setCheckItemName(String checkItemName) {
        this.mCheckItemName = checkItemName;
    }

    public String getCheckType() {
        return mCheckType;
    }

    public void setCheckType(String checkType) {
        this.mCheckType = checkType;
    }

    public String getSettingViewType() {
        return mSettingViewType;
    }

    public void setSettingViewType(String settingViewType) {
        this.mSettingViewType = settingViewType;
    }

    public String getValueLeft() {
        return mValueLeft;
    }

    public void setValueLeft(String valueLeft) {
        this.mValueLeft = valueLeft;
    }

    public String getValueRight() {
        return mValueRight;
    }

    public void setValueRight(String valueRight) {
        this.mValueRight = valueRight;
    }

    public String getValue() {
        return mValue;
    }

    public void setValue(String value) {
        this.mValue = value;
    }

    public String getIsCheckString() {
        return mIsCheckStr;
    }

    public void setIsCheckString(String isCheckStr) {
        this.mIsCheckStr = isCheckStr;
    }

    public boolean needCheck() {
        if (mSettingViewType.equalsIgnoreCase(SettingViewType.ONE_EDIT_TEXT)
                || mSettingViewType.equalsIgnoreCase(SettingViewType.TWO_EDIT_TEXT)) {
            return true;

        } else if (mSettingViewType.equalsIgnoreCase(SettingViewType.CHECK_BOX)) {
            if (mIsCheckStr.equalsIgnoreCase("true")) {
                return true;
            } else if (mIsCheckStr.equalsIgnoreCase("false")) {
                return false;
            } else {
                throw new IllegalArgumentException("wrong is_check string: " + mIsCheckStr);
            }

        } else {
            throw new IllegalArgumentException("wrong setting view type: " + mSettingViewType);
        }
    }

    public String getJudgeType() {
        return mJudgeType;
    }

    public void setJudgeType(String judgeType) {
        this.mJudgeType = judgeType;
    }

    public CheckItemBase getCheckItem() {
        return mCheckItem;
    }

    public void setCheckItem(CheckItemBase checkItem) {
        this.mCheckItem = checkItem;
    }

    /**
     * construct the full description of condition.
     * @return full description of the condition
     */
    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        if (mSettingViewType.equalsIgnoreCase(SettingViewType.ONE_EDIT_TEXT)) {
            desc.append(mDescPre)
                .append(mDescSep)
                .append(mValue)
                .append(mDescSuf);
        } else if (mSettingViewType.equalsIgnoreCase(SettingViewType.TWO_EDIT_TEXT)) {
            desc.append(mDescPre)
                .append(mValueLeft)
                .append(mDescSep)
                .append(mValueRight)
                .append(mDescSuf);
        } else if (mSettingViewType.equalsIgnoreCase(SettingViewType.CHECK_BOX)) {
            desc.append(mDescPre)
                .append(mDescSep)
                .append(mDescSuf);
        } else {
            throw new IllegalArgumentException("no such setting view type: " + mSettingViewType);
        }
        return desc.toString();
    }

    @Override
    public String toString() {
        return "Condition(" + this.hashCode() + "): "
                + "{"
                + "descPre: " + (null != mDescPre ? mDescPre : "null") + ", "
                + "descSep: " + (null != mDescSep ? mDescSep : "null") + ", "
                + "descSuf: " + (null != mDescSuf ? mDescSuf : "null") + ", "
                + "check_item_name: " + (null != mCheckItemName ? mCheckItemName : "null") + ", "
                + "check_type: " + (null != mCheckType ? mCheckType : "null") + ", "
                + "setting_view_type: " + (null != mSettingViewType ? mSettingViewType : "null") + ", "
                + "value_left: " + (null != mValueLeft ? mValueLeft : "null") + ", "
                + "value_right: " + (null != mValueRight ? mValueRight : "null") + ", "
                + "value: " + (null != mValue ? mValue : "null") + ", "
                + "is_check: " + (null != mIsCheckStr ? mIsCheckStr : "null") + ", "
                + "judge_type: " + (null != mJudgeType ? mJudgeType : "null") + ", "
                + "check item: " + (null != mCheckItem ? mCheckItem.getClass().getName() : "null")
                + "}";
    }
}

class TestCase {

    private String mId;
    private String mName;
    private String mCaseType;
    private List<Condition> mConditionList;

    private TestCaseResult mResult;

    final class TestCaseType {
        public static final String STATIC = "static";
        public static final String DYNAMIC = "dynamic";
    }

    enum TestCaseResult {
        EXECUTABLE,
        UNEXECUTABLE
    }

    public TestCase() {
        mConditionList = new ArrayList<Condition>();
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        this.mId = id;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getCaseType() {
        return mCaseType;
    }

    public void setCaseType(String caseType) {
        this.mCaseType = caseType;
    }

    public List<Condition> getConditionList() {
        return mConditionList;
    }

    public void setConditionList(List<Condition> conditionList) {
        this.mConditionList = conditionList;
    }

    public Condition getCondition(int location) {
        return mConditionList.get(location);
    }

    public boolean addCondition(Condition condition) {
        return mConditionList.add(condition);
    }

    public TestCaseResult getResult() {
        return mResult;
    }

    public void setResult(TestCaseResult result) {
        this.mResult = result;
    }

    public boolean isStatic() {
        if (mCaseType.equalsIgnoreCase(TestCaseType.STATIC)) {
            return true;
        } else if (mCaseType.equalsIgnoreCase(TestCaseType.DYNAMIC)) {
            return false;
        } else {
            throw new RuntimeException("wrong case type, mCaseType: " + mCaseType);
        }
    }

    @Override
    public String toString() {
        return "TestCase(" + hashCode() + "): "
                + "{"
                + "id: " + (null != mId ? mId : "null") + ", "
                + "name: " + (null != mName ? mName : "null") + ", "
                + "case_type: " + (null != mCaseType ? mCaseType : "null") + ", "
                + "mConditionList: " + (null != mConditionList ? mConditionList : "null") + ", "
                + "}";
    }

}

class TestCaseChapter {

    private String mId;
    private String mName;
    private String mPhoneType;
    private List<TestCase> mTestCaseList;

    final class PhoneType {
        public static final String COMMON = "common"; //SGLTE & CSFB
        public static final String SGLTE = "SGLTE";
        public static final String CSFB = "CSFB";
    }

    public TestCaseChapter() {
        mTestCaseList = new ArrayList<TestCase>();
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        this.mId = id;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getPhoneType() {
        return mPhoneType;
    }

    public void setPhoneType(String phoneType) {
        this.mPhoneType = phoneType;
    }

    public List<TestCase> getTestCaseList() {
        return mTestCaseList;
    }

    public void setTestCaseList(List<TestCase> testCaseList) {
        this.mTestCaseList = testCaseList;
    }

    public TestCase getTestCase(int location) {
        return mTestCaseList.get(location);
    }

    public boolean addTestCase(TestCase testCase) {
        return mTestCaseList.add(testCase);
    }

    public boolean supportCsfbOnly() {
        return mPhoneType.equalsIgnoreCase(PhoneType.CSFB);
    }

    public boolean supportSglteOnly() {
        return mPhoneType.equalsIgnoreCase(PhoneType.SGLTE);
    }

    @Override
    public String toString() {
        return "TestCaseChapter(" + hashCode() + "): "
                + "{"
                + "id: " + (null != mId ? mId : "null") + ", "
                + "name: " + (null != mName ? mName : "null") + ", "
                + "phone_type: " + (null != mPhoneType ? mPhoneType : "null") + ", "
                + "mTestCaseList: " + (null != mTestCaseList ? mTestCaseList : "null") + ", "
                + "}";
    }

}
