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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.mediatek.ftprecheck.TestCase.TestCaseResult;

import android.content.Context;
import android.os.SystemProperties;


public class ConditionManager {

    private static final String TAG = "ConditionManager";

    /**
     * There might be two condition file:
     * (1)conditions_original.xml:
     *   - stored in assets folder of our apk. Resource in assets folder is read only.
     *   - may used for restore function in future.
     * (2)conditions.xml:
     *   - stored in /data/data/com.mediatek.ftprecheck/files/ where are readable and writable,
     *   and would be clear if uninstall apk.
     *   - used for conditions setting in SettingsActivity.
     */
    private static final String FILE_CONDS_ORIG = "conditions_original.xml";
    private static final String FILE_CONDS = "conditions.xml";

    //DC=Dual Call
    public static final boolean IS_SGLTE_PHONE =
            SystemProperties.get("ro.mtk_lte_support").equals("1")
            && SystemProperties.get("ro.mtk_umts_tdd128_mode").equals("1")
            && SystemProperties.get("ro.mtk_lte_dc_support").equals("1");

    public static final boolean IS_CSFB_PHONE =
            SystemProperties.get("ro.mtk_lte_support").equals("1")
            && SystemProperties.get("ro.mtk_umts_tdd128_mode").equals("1")
            && !SystemProperties.get("ro.mtk_lte_dc_support").equals("1");

    private static ConditionManager sConditionManager;
    private Context mContext;
    private boolean mIsConditionsChange; //flag to indicate if content of conditions.xml is changed
    private List<TestCaseChapter> mCaseChapterList;
    private List<TestCase> mStaticCaseList;
    private List<TestCase> mDynamicCaseList;

    private List<TestCase> mExecCaseList;
    private List<TestCase> mUnexecCaseList;

    // <channel index, AP number>
    private HashMap<Integer, Integer> mWifiChannelInfo;

    private ConditionManager(Context c) {
        mContext = c;
        //force to read conditions.xml when ConditionManager initiates and conditions.xml exists.
        mIsConditionsChange = true;
        mCaseChapterList = new ArrayList<TestCaseChapter>();
        mStaticCaseList = new ArrayList<TestCase>();
        mDynamicCaseList = new ArrayList<TestCase>();
        mExecCaseList = new ArrayList<TestCase>();
        mUnexecCaseList = new ArrayList<TestCase>();
        mWifiChannelInfo = new HashMap<Integer, Integer>();
    }

    public static ConditionManager getConditionManager(Context c) {
        if (null == sConditionManager) {
            sConditionManager = new ConditionManager(c);
        }
        return sConditionManager;
    }

    public List<TestCase> getStaticCases() {
        return mStaticCaseList;
    }

    public List<TestCase> getDynamicCases() {
        return mDynamicCaseList;
    }

    public List<TestCase> getCurrentCheckCases(boolean isStaticCheck) {
        if (isStaticCheck) {
            return mStaticCaseList;
        } else {
            return mDynamicCaseList;
        }
    }

    public List<TestCaseChapter> getCaseChapterList() {
        return mCaseChapterList;
    }

    public TestCaseChapter getChapterCases(int index) {
        if (mCaseChapterList == null) {
            return null;
        }
        return mCaseChapterList.get(index);
    }

    public List<TestCase> getResultExecCases() {
        return mExecCaseList;
    }

    public List<TestCase> getResultUnexecCases() {
        return mUnexecCaseList;
    }

    /**
     * if content of conditions.xml is changed.
     */
    public boolean isConditionsChange() {
        return mIsConditionsChange;
    }

    /**
     * Call to set flag true when user changes some conditions in SettingsActivity.
     * Call to set flag false when ConditionManager has loaded current conditions.xml.
     */
    public void setChangeFlag(boolean change) {
        mIsConditionsChange = change;
    }

    /**
     * Read conditions information from file.
     * @param isFromOrig if read from conditions_original.xml.
     * @return conditions information.
     */
    public List<TestCaseChapter> readConditions(boolean isFromOrig) {
        InputStream inStream = null;
        try {
            if (isFromOrig) {
                inStream = mContext.getAssets().open(FILE_CONDS_ORIG);
            } else {
                inStream = mContext.openFileInput(FILE_CONDS);
            }
        } catch (Exception e) {
            FTPCLog.d(TAG, "read conditions fail, isFromOrig = " + isFromOrig);
            e.printStackTrace();
        }

        if (null == inStream) {
            return null;
        }
        return ConditionXmlParser.readConditions(inStream);
    }

    /**
     * Write condition information to conditions.xml.
     */
    public void writeConditions(List<TestCaseChapter> caseChapterList) {
        String conditionsStr = ConditionXmlParser.writeConditions(caseChapterList);
        FileOutputStream outputStream;
        try {
            outputStream = mContext.openFileOutput(FILE_CONDS, Context.MODE_PRIVATE);
            if (conditionsStr != null) {
                outputStream.write(conditionsStr.getBytes("UTF-8"));
            }
            outputStream.close();
        } catch (Exception e) {
            FTPCLog.d(TAG, "write conditions.xml fail!");
            e.printStackTrace();
        }
    }

    public boolean conditionsFileExists() {
        File condFile = new File(mContext.getFilesDir(), FILE_CONDS);
        FTPCLog.d(TAG, "conditions.xml exists: " + condFile.exists());
        return condFile.exists();
    }

    /**
     * load conditions info to ConditionManager's fields.
     */
    public void loadConditionsInfo() {
        boolean needUpdate = false; //if update mStaticCaseList & mDynamicCaseList
        if (!conditionsFileExists()) {
            mCaseChapterList = readConditions(true);
            if (mCaseChapterList != null) {
                writeConditions(mCaseChapterList);
            }
            needUpdate = true;
            mIsConditionsChange = false;
        } else if (mIsConditionsChange) {
            mCaseChapterList = readConditions(false);
            needUpdate = true;
            mIsConditionsChange = false;
        }

        if (!needUpdate) {
            return;
        }

        if (!mStaticCaseList.isEmpty()) {
            mStaticCaseList.clear();
        }
        if (!mDynamicCaseList.isEmpty()) {
            mDynamicCaseList.clear();
        }
        if (mCaseChapterList == null) {
            return ;
        }
        for (TestCaseChapter caseChapter : mCaseChapterList) {
            if ((IS_CSFB_PHONE && caseChapter.supportSglteOnly())
                    || (IS_SGLTE_PHONE && caseChapter.supportCsfbOnly())) {
                continue;
            }
            for (TestCase testCase : caseChapter.getTestCaseList()) {
                if (testCase.isStatic()) {
                    mStaticCaseList.add(testCase);
                } else {
                    mDynamicCaseList.add(testCase);
                }
            }
        }
    }

    /**
     * construct executable & unexecutable case list.
     * @param caseList cases that need to check.
     */
    public void constructResultCases(List<TestCase> caseList) {
        for (TestCase testCase : caseList) {
            testCase.setResult(CaseResultJudge.judge(testCase));
        }

        if (!mExecCaseList.isEmpty()) {
            mExecCaseList.clear();
        }
        if (!mUnexecCaseList.isEmpty()) {
            mUnexecCaseList.clear();
        }
        for (TestCase testCase : caseList) {
            if (testCase.getResult().equals(TestCaseResult.EXECUTABLE)) {
                mExecCaseList.add(testCase);
            } else {
                mUnexecCaseList.add(testCase);
            }
        }
    }

    /**
     * create check items for conditions.
     * @param caseList cases that need to check.
     */
    public void createCheckItems(List<TestCase> caseList) {
        for (TestCase testCase : caseList) {
            for (Condition condition : testCase.getConditionList()) {
                if (condition.needCheck()) {
                    CheckItemBase checkItem = CheckItemFactory.getCheckItem(
                            mContext, condition.getCheckItemName(),
                            condition.getCheckType(), condition.getValue(),
                            condition.getValueLeft(), condition.getValueRight());
                    condition.setCheckItem(checkItem);
                }
            }
        }
    }

    /**
     * Set wifi channel info.
     * @param infoMap The info to be set. If is null, will clear the
     * channel info that stored in ConditionManager.
     */
    public void setWifiChanneInfo(HashMap<Integer, Integer> infoMap) {
        // only store the latest info to be set.
        if (null == infoMap) {
            mWifiChannelInfo.clear();
            return;
        }
        if (!mWifiChannelInfo.isEmpty()) {
            mWifiChannelInfo.clear();
        }
        mWifiChannelInfo.putAll(infoMap);
    }

    public HashMap<Integer, Integer> getWifiChanneInfo() {
        return mWifiChannelInfo;
    }

}
