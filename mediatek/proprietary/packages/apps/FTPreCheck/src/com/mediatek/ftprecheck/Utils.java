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

import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

import android.util.Xml;

import com.mediatek.ftprecheck.CheckItemBase.CheckResult;
import com.mediatek.ftprecheck.Condition.SettingViewType;
import com.mediatek.ftprecheck.TestCase.TestCaseResult;

class ConditionXmlParser {

    private static final String LOG_TAG = "ConditionXmlParser";

    private static final String TAG_ROOT = "ft_testcase_conditions";
    private static final String TAG_CHAPTER = "testcase_chapter";
    private static final String TAG_CASE = "testcase";
    private static final String TAG_CONDITION = "condition";
    private static final String TAG_CHECK_ITEM_NAME = "check_item_name";
    private static final String TAG_CHECK_TYPE = "check_type";
    private static final String TAG_SETTING_VIEW_TYPE = "setting_view_type";
    private static final String TAG_VALUE_LEFT = "value_left";
    private static final String TAG_VALUE_RIGHT = "value_right";
    private static final String TAG_VALUE = "value";
    private static final String TAG_IS_CHECK = "is_check";
    private static final String TAG_JUDGE_TYPE = "judge_type";

    private static final String ATT_ID = "id";
    private static final String ATT_NAME = "name";
    private static final String ATT_PHONE_TYPE = "phone_type";
    private static final String ATT_CASE_TYPE = "case_type";
    private static final String ATT_DESC_PRE = "descPrefix";
    private static final String ATT_DESC_SEP = "descSeparator";
    private static final String ATT_DESC_SUF = "descSuffix";

    public static List<TestCaseChapter> readConditions(InputStream inStream) {
        Condition condition = null;
        TestCase testCase = null;
        TestCaseChapter caseChapter = null;
        List<TestCaseChapter> caseChapterList = null;

        XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setInput(inStream, "UTF-8");
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                case XmlPullParser.START_DOCUMENT:
                    caseChapterList = new ArrayList<TestCaseChapter>();
                    break;

                case XmlPullParser.START_TAG:
                    String name = parser.getName();
                    if (name.equalsIgnoreCase(TAG_ROOT)) {
                        //do nothing
                    } else if (name.equalsIgnoreCase(TAG_CHAPTER)) {
                        caseChapter = new TestCaseChapter();
                        caseChapter.setId(parser.getAttributeValue(null, ATT_ID));
                        caseChapter.setName(parser.getAttributeValue(null, ATT_NAME));
                        caseChapter.setPhoneType(parser.getAttributeValue(null, ATT_PHONE_TYPE));

                    } else if (name.equalsIgnoreCase(TAG_CASE)) {
                        testCase = new TestCase();
                        testCase.setId(parser.getAttributeValue(null, ATT_ID));
                        testCase.setName(parser.getAttributeValue(null, ATT_NAME));
                        testCase.setCaseType(parser.getAttributeValue(null, ATT_CASE_TYPE));

                    } else if (name.equalsIgnoreCase(TAG_CONDITION)) {
                        condition = new Condition();
                        condition.setDescPrefix(parser.getAttributeValue(null, ATT_DESC_PRE));
                        condition.setDescSeparator(parser.getAttributeValue(null, ATT_DESC_SEP));
                        condition.setDescSuffix(parser.getAttributeValue(null, ATT_DESC_SUF));

                    } else if (name.equalsIgnoreCase(TAG_CHECK_ITEM_NAME) && (condition != null)) {
                        condition.setCheckItemName(parser.nextText());

                    } else if (name.equalsIgnoreCase(TAG_CHECK_TYPE) && (condition != null)) {
                        condition.setCheckType(parser.nextText());

                    } else if (name.equalsIgnoreCase(TAG_SETTING_VIEW_TYPE) && (condition != null)) {
                        condition.setSettingViewType(parser.nextText());

                    } else if (name.equalsIgnoreCase(TAG_VALUE_LEFT) && (condition != null)) {
                        condition.setValueLeft(parser.nextText());

                    } else if (name.equalsIgnoreCase(TAG_VALUE_RIGHT) && (condition != null)) {
                        condition.setValueRight(parser.nextText());

                    } else if (name.equalsIgnoreCase(TAG_VALUE) && (condition != null)) {
                        condition.setValue(parser.nextText());

                    } else if (name.equalsIgnoreCase(TAG_IS_CHECK) && (condition != null)) {
                        condition.setIsCheckString(parser.nextText());

                    } else if (name.equalsIgnoreCase(TAG_JUDGE_TYPE) && (condition != null)) {
                        condition.setJudgeType(parser.nextText());

                    } else {
                        FTPCLog.e(LOG_TAG, "readConditions error, no such start tag: " + name);
                    }
                    break;

                case XmlPullParser.END_TAG:
                    name = parser.getName();
                    if (name.equalsIgnoreCase(TAG_CONDITION) && (condition != null)) {
                        testCase.addCondition(condition);
                        condition = null;

                    } else if (name.equalsIgnoreCase(TAG_CASE) && (testCase != null)) {
                        caseChapter.addTestCase(testCase);
                        testCase = null;

                    } else if (name.equalsIgnoreCase(TAG_CHAPTER) && (caseChapter != null)) {
                        caseChapterList.add(caseChapter);
                        caseChapter = null;

                    } else if (name.equalsIgnoreCase(TAG_ROOT)) {
                        //do nothing
                    } else {
                        FTPCLog.e(LOG_TAG, "readConditions error, no such end tag: " + name);
                    }
                    break;

                default:
                    break;
                }
                eventType = parser.next();
            }
            return caseChapterList;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String writeConditions(List<TestCaseChapter> caseChapterList) {
        XmlSerializer serializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();
        try {
            serializer.setOutput(writer);
            serializer.startDocument("UTF-8", true);

            serializer.startTag("", TAG_ROOT);
            for (TestCaseChapter caseChapter : caseChapterList) {
                serializer.startTag("", TAG_CHAPTER);
                serializer.attribute("", ATT_ID,
                        null != caseChapter.getId() ? caseChapter.getId() : "");
                serializer.attribute("", ATT_NAME,
                        null != caseChapter.getName() ? caseChapter.getName() : "");
                serializer.attribute("", ATT_PHONE_TYPE,
                        null != caseChapter.getPhoneType() ? caseChapter.getPhoneType() : "");

                for (TestCase testCase : caseChapter.getTestCaseList()) {
                    serializer.startTag("", TAG_CASE);
                    serializer.attribute("", ATT_ID,
                            null != testCase.getId() ? testCase.getId() : "");
                    serializer.attribute("", ATT_NAME,
                            null != testCase.getName() ? testCase.getName() : "");
                    serializer.attribute("", ATT_CASE_TYPE,
                            null != testCase.getCaseType() ? testCase.getCaseType() : "");

                    for (Condition condition : testCase.getConditionList()) {
                        serializer.startTag("", TAG_CONDITION);
                        serializer.attribute("", ATT_DESC_PRE,
                                null != condition.getDescPrefix() ? condition.getDescPrefix() : "");
                        serializer.attribute("", ATT_DESC_SEP,
                                null != condition.getDescSeparator() ? condition.getDescSeparator() : "");
                        serializer.attribute("", ATT_DESC_SUF,
                                null != condition.getDescSuffix() ? condition.getDescSuffix() : "");

                        serializer.startTag("", TAG_CHECK_ITEM_NAME);
                        serializer.text(null != condition.getCheckItemName() ? condition.getCheckItemName() : "");
                        serializer.endTag("", TAG_CHECK_ITEM_NAME);

                        serializer.startTag("", TAG_CHECK_TYPE);
                        serializer.text(null != condition.getCheckType() ? condition.getCheckType() : "");
                        serializer.endTag("", TAG_CHECK_TYPE);

                        String viewType = condition.getSettingViewType();
                        serializer.startTag("", TAG_SETTING_VIEW_TYPE);
                        serializer.text(null != viewType ? viewType : "");
                        serializer.endTag("", TAG_SETTING_VIEW_TYPE);

                        if (null != viewType) {
                            if (viewType.equalsIgnoreCase(SettingViewType.TWO_EDIT_TEXT)) {
                                serializer.startTag("", TAG_VALUE_LEFT);
                                serializer.text(null != condition.getValueLeft() ? condition.getValueLeft() : "");
                                serializer.endTag("", TAG_VALUE_LEFT);
                                serializer.startTag("", TAG_VALUE_RIGHT);
                                serializer.text(null != condition.getValueRight() ? condition.getValueRight() : "");
                                serializer.endTag("", TAG_VALUE_RIGHT);
                            } else if (viewType.equalsIgnoreCase(SettingViewType.ONE_EDIT_TEXT)) {
                                serializer.startTag("", TAG_VALUE);
                                serializer.text(null != condition.getValue() ? condition.getValue() : "");
                                serializer.endTag("", TAG_VALUE);
                            } else if (viewType.equalsIgnoreCase(SettingViewType.CHECK_BOX)) {
                                serializer.startTag("", TAG_IS_CHECK);
                                serializer.text(null != condition.getIsCheckString() ? condition.getIsCheckString() : "");
                                serializer.endTag("", TAG_IS_CHECK);
                            } else {
                                FTPCLog.e("LOG_TAG", "writeConditions error, no such setting view type: " + viewType);
                            }
                        }

                        serializer.startTag("", TAG_JUDGE_TYPE);
                        serializer.text(null != condition.getJudgeType() ? condition.getJudgeType() : "");
                        serializer.endTag("", TAG_JUDGE_TYPE);

                        serializer.endTag("", TAG_CONDITION);

                    } //end for testCase.getConditionList()
                    serializer.endTag("", TAG_CASE);

                } //end for caseChapter.getTestCaseList()
                serializer.endTag("", TAG_CHAPTER);

            } //end for caseChapterList
            serializer.endTag("", TAG_ROOT);
            serializer.endDocument();
            return writer.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

/**
 * Wrapped the judge method for test case's result.
 * Attention:
 * This class's implementation is totally limited by the condition table.
 * So, if condition table's judge types or specific judge rule change,
 * this class's implementation should changes to align.
 */
class CaseResultJudge {

    //Type of judge rule.
    private static final String JUDGE_TYPE_V = "V";
    private static final String JUDGE_TYPE_V1 = "V1";
    private static final String JUDGE_TYPE_V2 = "V2";

    private static List<Boolean> mCondResults_V;
    private static List<Boolean> mCondResults_V1;
    private static List<Boolean> mCondResults_V2;

    static {
        mCondResults_V = new ArrayList<Boolean>();
        mCondResults_V1 = new ArrayList<Boolean>();
        mCondResults_V2 = new ArrayList<Boolean>();
    }

    /**
     * Get the result of test case by specific judge rule.
     * @param testCase the test case that needs to be judged.
     * @return Result of test case, TestCaseResult.EXECUTABLE or TestCaseResult.UNEXECUTABLE.
     *
     * Example of specific judge rule:
     * -------------------------------------------------------------------------------------
     *        | condition1 | condition2 | condition3 | condition4 | condition5 | condition6
     *        |  (result1) |  (result2) |  (result3) |  (result4) |  (result5) |  (result6)
     * -------------------------------------------------------------------------------------
     *  case  |     V      |     V      |     V1     |     V1     |     V2     |     V2
     * -------------------------------------------------------------------------------------
     * Result of case =
     * (result1 && result2) && ( (result3 && result4) || (result5 && result6) )
     */
    public static TestCaseResult judge(TestCase testCase) {
        initJudgeList(testCase);
        boolean bCaseResult = judgeRule();
        if (bCaseResult) {
            return TestCaseResult.EXECUTABLE;
        } else {
            return TestCaseResult.UNEXECUTABLE;
        }
    }

    //V && (V1 || V2)
    private static boolean judgeRule() {
        if (mCondResults_V.isEmpty()
                && mCondResults_V1.isEmpty()
                && mCondResults_V2.isEmpty()) {
            return true;
        } else if (mCondResults_V.isEmpty()
                && mCondResults_V1.isEmpty()
                && !mCondResults_V2.isEmpty()) {
            return listResult_And(mCondResults_V2);
        } else if (mCondResults_V.isEmpty()
                && !mCondResults_V1.isEmpty()
                && mCondResults_V2.isEmpty()) {
            return listResult_And(mCondResults_V1);
        } else if (mCondResults_V.isEmpty()
                && !mCondResults_V1.isEmpty()
                && !mCondResults_V2.isEmpty()) {
            return listResult_And(mCondResults_V1) || listResult_And(mCondResults_V2);
        } else if (!mCondResults_V.isEmpty()
                && mCondResults_V1.isEmpty()
                && mCondResults_V2.isEmpty()) {
            return listResult_And(mCondResults_V);
        } else if (!mCondResults_V.isEmpty()
                && mCondResults_V1.isEmpty()
                && !mCondResults_V2.isEmpty()) {
            return listResult_And(mCondResults_V) && listResult_And(mCondResults_V2);
        } else if (!mCondResults_V.isEmpty()
                && !mCondResults_V1.isEmpty()
                && mCondResults_V2.isEmpty()) {
            return listResult_And(mCondResults_V) && listResult_And(mCondResults_V1);
        } else {
            //!mCondResults_V.isEmpty() && !mCondResults_V1.isEmpty() && !mCondResults_V2.isEmpty()
            return listResult_And(mCondResults_V)
                    && (listResult_And(mCondResults_V1) || listResult_And(mCondResults_V1));
        }
    }

    private static void initJudgeList(TestCase testCase) {
        if (!mCondResults_V.isEmpty()) {
            mCondResults_V.clear();
        }
        if (!mCondResults_V1.isEmpty()) {
            mCondResults_V1.clear();
        }
        if (!mCondResults_V2.isEmpty()) {
            mCondResults_V2.clear();
        }
        for (Condition condition : testCase.getConditionList()) {
            if (!condition.needCheck()) {
                continue;
            }
            Boolean resultBoolean = getResultBoolean(
                    condition.getCheckItem().getCheckResult());
            if (condition.getJudgeType().equalsIgnoreCase(JUDGE_TYPE_V)) {
                mCondResults_V.add(resultBoolean);
            } else if (condition.getJudgeType().equalsIgnoreCase(JUDGE_TYPE_V1)) {
                mCondResults_V1.add(resultBoolean);
            } else if (condition.getJudgeType().equalsIgnoreCase(JUDGE_TYPE_V2)) {
                mCondResults_V2.add(resultBoolean);
            } else {
                throw new IllegalArgumentException(
                        "no such judge type: " + condition.getJudgeType());
            }
        }
    }

    private static Boolean getResultBoolean(CheckResult result) {
        if (result.equals(CheckResult.SATISFIED)) {
            return Boolean.valueOf(true);
        }
        return Boolean.valueOf(false);
    }

    /**
     * Logical AND the elements in list.
     */
    private static boolean listResult_And(List<Boolean> list) {
        for (int i = 0; i < list.size(); i++) {
            if (!list.get(i).booleanValue()) {
                return false;
            }
        }
        return true;
    }

}
