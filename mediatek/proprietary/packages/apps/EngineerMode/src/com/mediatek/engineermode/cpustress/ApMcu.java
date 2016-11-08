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

package com.mediatek.engineermode.cpustress;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.engineermode.ChipSupport;
import com.mediatek.engineermode.R;

import java.io.File;

public class ApMcu extends CpuStressCommon implements OnClickListener, OnCheckedChangeListener {

    private static final String TAG = "EM/CpuStress_ApMcu";
    public static final int INDEX_NEON = 0;
    public static final int INDEX_CA9 = 1;
    public static final int INDEX_DHRY = 2;
    public static final int INDEX_MEMCPY = 3;
    public static final int INDEX_FDCT = 4;
    public static final int INDEX_IMDCT = 5;
    public static final int INDEX_MAX_POWER_64 = 6;
    public static final int INDEX_DHRYSTONE_64 = 7;
    public static final int INDEX_SAXPY = 8;
    public static final int INDEX_SAXPY_64 = 9;
    public static final int INDEX_ADV_SIM = 10;
    public static final int INDEX_IDLE_TO_MAX = 11;
    public static final int[] APMCU_TEST_INDEXS = {
        INDEX_NEON, INDEX_CA9, INDEX_DHRY,
        INDEX_MEMCPY, INDEX_FDCT, INDEX_IMDCT,
        INDEX_MAX_POWER_64, INDEX_DHRYSTONE_64,
        INDEX_SAXPY, INDEX_SAXPY_64,
        INDEX_ADV_SIM, INDEX_IDLE_TO_MAX,
    };

    public static final int MASK_NEON_0 = 0;
    public static final int MASK_CA9_0 = 8;
    public static final int MASK_DHRY_0 = 16;
    public static final int MASK_MEMCPY_0 = 24;
    public static final int MASK_FDCT_0 = 32;
    public static final int MASK_IMDCT_0 = 40;

    private static final String TO_DO_PATTERN = "need_to_do_cpu_test_pattern";
    private static final String NEON_VFP_MAIN_PATTERN
        = "/sys/bus/platform/drivers/slt_cpu0_vfp/slt_cpu0_vfp";
    private static final String MAX_POWER_MAIN_PATTERN
        = "/sys/bus/platform/drivers/slt_cpu0_maxpower/slt_cpu0_maxpower";
    private static final String DHRY_MAIN_PATTERN
        = "/sys/bus/platform/drivers/slt_cpu0_dhry/slt_cpu0_dhry";
    private static final String MEMCPY_MAIN_PATTERN
        = "/sys/bus/platform/drivers/slt_cpu0_memcpyL2/slt_cpu0_memcpyL2";
    private static final String FDCT_MAIN_PATTERN
        = "/sys/bus/platform/drivers/slt_cpu0_fdct/slt_cpu0_fdct";
    private static final String IMDCT_MAIN_PATTERN
        = "/sys/bus/platform/drivers/slt_cpu0_imdct/slt_cpu0_imdct";
    private static final String MAX_POWER64_MAIN_PATTERN = TO_DO_PATTERN;
    private static final String DHRYSTONE64_MAIN_PATTERN = TO_DO_PATTERN;
    private static final String SAXPY_MAIN_PATTERN
        = "/sys/bus/platform/drivers/slt_cpu0_saxpy/slt_cpu0_saxpy";
    private static final String SAXPY64_MAIN_PATTERN = TO_DO_PATTERN;
    private static final String ADV_SIM_MAIN_PATTERN
        = "/sys/bus/platform/drivers/slt_cpu0_simd/slt_cpu0_simd";
    private static final String IDLE2MAX_MAIN_PATTERN
        = "/sys/bus/platform/drivers/slt_cpu0_maxtrans/slt_cpu0_maxtrans";

    /**
     * TestItemResource was used to record test item resource.
     * @author mtk81238
     */
    private static class TestItemResource {
        public String mainCmdPattern;
        public int layoutId;
        TestItemResource(String mainCmd, int id) {
            mainCmdPattern = mainCmd;
            layoutId = id;
        }
    }

    private static final double PERCENT = 100.0;
    private static final int TEST_ITEM = 6;
    private static final String MAXPOWER_TYPE_9 = "CA9";
    private static final String MAXPOWER_TYPE_7 = "CA7";
    private static TestItemResource[] sApmcuTestItemResouces = {
        new TestItemResource(NEON_VFP_MAIN_PATTERN, R.id.apmcu_neon_vfp_ll),
        new TestItemResource(MAX_POWER_MAIN_PATTERN, R.id.apmcu_max_power_ll),
        new TestItemResource(DHRY_MAIN_PATTERN, R.id.apmcu_dhrystone_ll),
        new TestItemResource(MEMCPY_MAIN_PATTERN, R.id.apmcu_memcpy_ll),
        new TestItemResource(FDCT_MAIN_PATTERN, R.id.apmcu_fdct_ll),
        new TestItemResource(IMDCT_MAIN_PATTERN, R.id.apmcu_imdct_ll),
        new TestItemResource(MAX_POWER64_MAIN_PATTERN, R.id.apmcu_max_power_64_ll),
        new TestItemResource(DHRYSTONE64_MAIN_PATTERN, R.id.apmcu_dhrystone_64_ll),
        new TestItemResource(SAXPY_MAIN_PATTERN, R.id.apmcu_saxpy_ll),
        new TestItemResource(SAXPY64_MAIN_PATTERN, R.id.apmcu_saxpy_64_ll),
        new TestItemResource(ADV_SIM_MAIN_PATTERN, R.id.apmcu_adv_sim_ll),
        new TestItemResource(IDLE2MAX_MAIN_PATTERN, R.id.apmcu_idle_to_max_ll),
    };
    private static int[] sHiddenTestItemIds = {R.id.apmcu_idle_to_max_ll, };
    private EditText mEtLoopCount = null;
    private CheckBox[] mCbArray = new CheckBox[APMCU_TEST_INDEXS.length];
    private TextView[] mTvArray = new TextView[APMCU_TEST_INDEXS.length * CpuStressTestService.CORE_NUMBER_8];
    private Button mBtnStart = null;
    private TextView mTvResult = null;
    private int mAvailableTestItems = 0;

    private boolean isForceHidden(int id) {
        for (int i = 0; i < sHiddenTestItemIds.length; i++) {
            if (id == sHiddenTestItemIds[i]) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hqa_cpustress_apmcu);
        mEtLoopCount = (EditText) findViewById(R.id.apmcu_loopcount);
        mCbArray[INDEX_NEON]   = (CheckBox) findViewById(R.id.apmcu_neon_test);
        mCbArray[INDEX_CA9]    = (CheckBox) findViewById(R.id.apmcu_ca9_test);
        mCbArray[INDEX_DHRY]   = (CheckBox) findViewById(R.id.apmcu_dhrystone_test);
        mCbArray[INDEX_MEMCPY] = (CheckBox) findViewById(R.id.apmcu_memcpy_test);
        mCbArray[INDEX_FDCT]   = (CheckBox) findViewById(R.id.apmcu_fdct_test);
        mCbArray[INDEX_IMDCT]  = (CheckBox) findViewById(R.id.apmcu_imdct_test);
        mCbArray[INDEX_MAX_POWER_64]  = (CheckBox) findViewById(R.id.apmcu_max_power_64_test_cb);
        mCbArray[INDEX_DHRYSTONE_64]  = (CheckBox) findViewById(R.id.apmcu_dhrystone_64_test_cb);
        mCbArray[INDEX_SAXPY]  = (CheckBox) findViewById(R.id.apmcu_saxpy_test_cb);
        mCbArray[INDEX_SAXPY_64]  = (CheckBox) findViewById(R.id.apmcu_saxpy_64_test_cb);
        mCbArray[INDEX_ADV_SIM]  = (CheckBox) findViewById(R.id.apmcu_adv_sim_test_cb);
        mCbArray[INDEX_IDLE_TO_MAX]  = (CheckBox) findViewById(R.id.apmcu_idle_to_max_test_cb);
        mBtnStart = (Button) findViewById(R.id.apmcu_btn);

        int cpuTestMode = CpuStressTestService.getCpuTestMode();

        for (int i = 0; i < sApmcuTestItemResouces.length; i++) {
            TestItemResource testRes = sApmcuTestItemResouces[i];
            String cmdPattern = testRes.mainCmdPattern;
            if (!(new File(cmdPattern).exists()) || isForceHidden(testRes.layoutId)) {
                View layout = findViewById(testRes.layoutId);
                if (layout == null) {
                    Log.w("@M_" + TAG, "Invalid testItem layoutId:" + testRes.layoutId
                            + " for " + testRes.mainCmdPattern);
                    continue;
                }
                layout.setVisibility(View.GONE);
            } else {
                mAvailableTestItems++;
            }
        }
        if (mAvailableTestItems <= 0) {
            mBtnStart.setEnabled(false);
        }

        if (!(CpuStressTest.INDEX_OCTA == CpuStressTestService.sIndexMode ||
                (CpuStressTest.INDEX_TEST == CpuStressTestService.sIndexMode &&
                CpuStressTestService.CORE_NUMBER_8 == CpuStressTestService.sCoreNumber) ||
                (CpuStressTest.INDEX_CUSTOM == CpuStressTestService.sIndexMode &&
                        CpuStressTestService.CORE_NUMBER_8 == CpuStressTestService.sCoreNumber))) {
            findViewById(R.id.apmcu_neon_result_octa).setVisibility(View.GONE);
            findViewById(R.id.apmcu_ca9_result_octa).setVisibility(View.GONE);
            findViewById(R.id.apmcu_dhrystone_result_octa).setVisibility(View.GONE);
            findViewById(R.id.apmcu_memcpy_result_octa).setVisibility(View.GONE);
            findViewById(R.id.apmcu_fdct_result_octa).setVisibility(View.GONE);
            findViewById(R.id.apmcu_imdct_result_octa).setVisibility(View.GONE);
            findViewById(R.id.apmcu_max_power_64_result_octa).setVisibility(View.GONE);
            findViewById(R.id.apmcu_dhrystone_64_result_octa).setVisibility(View.GONE);
            findViewById(R.id.apmcu_saxpy_result_octa).setVisibility(View.GONE);
            findViewById(R.id.apmcu_saxpy_64_result_octa).setVisibility(View.GONE);
            findViewById(R.id.apmcu_adv_sim_result_octa).setVisibility(View.GONE);
            findViewById(R.id.apmcu_idle_to_max_result_octa).setVisibility(View.GONE);
        }

        String[] textViewIds = {"apmcu_neon_result", "apmcu_ca9_result", "apmcu_dhrystone_result",
                "apmcu_memcpy_result", "apmcu_fdct_result", "apmcu_imdct_result",
                "apmcu_max_power_64_result", "apmcu_dhrystone_64_result",
                "apmcu_saxpy_result", "apmcu_saxpy_64_result",
                "apmcu_adv_sim_result", "apmcu_idle_to_max_result"};
        for (int group = 0; group < APMCU_TEST_INDEXS.length; group++) {
            for (int column = 0; column < CpuStressTestService.CORE_NUMBER_8; column++) {
                String idStr = textViewIds[group] + "_" + column;
                // Id is like "R.id.apmcu_neon_result_1", see hqa_cpustree_apmcu.xml
                int id = getResources().getIdentifier(idStr, "id", getPackageName());
                View view = findViewById(id);
                if (view == null) {
                    throw new RuntimeException("Check the text view id:" + idStr);
                }
                mTvArray[group * CpuStressTestService.CORE_NUMBER_8 + column] = (TextView) view;
            }
        }
        mTvResult = (TextView) findViewById(R.id.apmcu_result);
        for (int i = 0; i < APMCU_TEST_INDEXS.length; i++) {
            mCbArray[i].setOnCheckedChangeListener(this);
        }
        mBtnStart.setOnClickListener(this);
        if (ChipSupport.MTK_6589_SUPPORT > ChipSupport.getChip()) {
            // Hide DHRY test, Memcpy test, FDCT test and IMDCT test
            for (int i = MASK_DHRY_0; i < mTvArray.length; i++) {
                mTvArray[i].setVisibility(View.GONE);
            }
            for (int i = INDEX_DHRY; i < mCbArray.length; i++) {
                mCbArray[i].setVisibility(View.GONE);
            }
        } else {
            // Show VFP test and CA7 test
            //mCbArray[INDEX_CA9].setText(mCbArray[1].getText().toString().replaceAll(
            //        MAXPOWER_TYPE_9, MAXPOWER_TYPE_7));
            mCbArray[INDEX_CA9].setText(R.string.hqa_cpustress_apmcu_max_power);
            mCbArray[INDEX_NEON].setText(getString(R.string.hqa_cpustress_apmcu_vfp));
        }
        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                Log.v("@M_" + TAG, "mHandler receive message: " + msg.what);
                if (INDEX_UPDATE_RADIOBTN == msg.what || INDEX_UPDATE_RADIOGROUP == msg.what) {
                    updateTestResult();
                }
            }
        };
    }

    /**
     * Update apmcu test status
     */
    private void updateTestResult() {
        Log.v("@M_" + TAG, "Enter updateTestResult");
        if (mBoundService != null) {
            TestDataSet testDataSet = mBoundService.getTestDataSet(ApMcu.class.getSimpleName());
            if (testDataSet != null) {
                updateTestUi(testDataSet);
                updateTestCount(testDataSet);
            }
        }
    }

    private void updateTestUi(TestDataSet testDataSet) {
        Log.v("@M_" + TAG, "updateTestUI: " + testDataSet.running + " " + testDataSet.leftTestCount);
        mEtLoopCount.setEnabled(!testDataSet.running);
        mEtLoopCount.setText(String.valueOf(testDataSet.leftTestCount));
        mEtLoopCount.setSelection(mEtLoopCount.getText().length());
        mBtnStart.setText(testDataSet.running ? R.string.hqa_cpustress_apmcu_stop
                : R.string.hqa_cpustress_apmcu_start);
        updateCbStatus(testDataSet);

        showTestResult(CpuStressTestService.sIndexMode, testDataSet);

        // Clear text views of unchecked items
        for (int i = 0; i < APMCU_TEST_INDEXS.length; i++) {
            if (!testDataSet.running || !mCbArray[i].isChecked()) {
                for (int j = 0; j < CpuStressTestService.CORE_NUMBER_8; j++) {
                    mTvArray[i * CpuStressTestService.CORE_NUMBER_8 + j]
                            .setText(R.string.hqa_cpustress_result);
                }
            }
        }
        if (!testDataSet.wantStop) {
            removeDialog(DIALOG_WAIT);
        }
    }

    private void showTestResult(int indexMode, TestDataSet testDataSet) {
        int index = 0;
        switch (indexMode) {
        case CpuStressTest.INDEX_SINGLE:
            index = 0;
            break;
        case CpuStressTest.INDEX_DUAL:
            index = 1;
            break;
        case CpuStressTest.INDEX_TRIPLE:
            index = 2;
            break;
        case CpuStressTest.INDEX_QUAD:
            index = 3;
            break;
        case CpuStressTest.INDEX_OCTA:
            index = 7;
            break;
        case CpuStressTest.INDEX_TEST:
            if (CpuStressTestService.CORE_NUMBER_8 == CpuStressTestService.sCoreNumber) {
                index = 7;
            } else if (CpuStressTestService.CORE_NUMBER_4 == CpuStressTestService.sCoreNumber) {
                index = 3;
            } else if (CpuStressTestService.CORE_NUMBER_2 == CpuStressTestService.sCoreNumber) {
                index = 1;
            } else {
                index = 0;
            }
            break;
        case CpuStressTest.INDEX_CUSTOM:
            index = CpuStressTestService.sCoreNumber - 1;
            break;
        default:
            break;
        }
        int testCoreMaxIndex = index;
        for (int i = 0; i < APMCU_TEST_INDEXS.length; i++) {
            TestData testData = testDataSet.getTestData(i);
            Log.d("@M_" + TAG, "showTestResult: i:" + i + " testData:" + testData);
            if (testData.enabled) {
                for (int j = 0; j <= testCoreMaxIndex; j++) {
                    TextView tv = mTvArray[i * CpuStressTestService.CORE_NUMBER_8 + j];
                    int result = testData.getTestResult(j);
                    if (result == TestData.TEST_PASS) {
                        tv.setText(R.string.hqa_cpustress_result_pass);
                    } else if (result == TestData.TEST_FAIL) {
                        tv.setText(R.string.hqa_cpustress_result_fail);
                    } else if (result == TestData.TEST_NO_TEST) {
                        tv.setText(R.string.hqa_cpustress_result_no_test);
                    } else {
                        tv.setText(R.string.hqa_cpustress_result);
                    }
                }
            }
        }
    }

    private void updateCbStatus(TestDataSet testDataSet) {
        if (testDataSet == null) {
            return;
        }
        for (int i = 0; i < APMCU_TEST_INDEXS.length; i++) {
            TestData data = testDataSet.getTestData(i);
            mCbArray[i].setEnabled(false);  // Don't callback onCheckedChanged()
            mCbArray[i].setChecked(data.enabled);
            mCbArray[i].setEnabled(true);
        }
    }

    /**
     * Update test result by percent
     *
     */
    private void updateTestCount(TestDataSet testDataSet) {
        StringBuffer sb = new StringBuffer();
        String[] testDetails = {getString(R.string.hqa_cpustress_apmcu_result_vfp),
                getString(R.string.hqa_cpustress_apmcu_result_ca9),
                getString(R.string.hqa_cpustress_apmcu_result_dhry),
                getString(R.string.hqa_cpustress_apmcu_result_memcpy),
                getString(R.string.hqa_cpustress_apmcu_result_fdct),
                getString(R.string.hqa_cpustress_apmcu_result_imdct),
                getString(R.string.hqa_cpustress_apmcu_result_maxpower64),
                getString(R.string.hqa_cpustress_apmcu_result_dhry64),
                getString(R.string.hqa_cpustress_apmcu_result_saxpy),
                getString(R.string.hqa_cpustress_apmcu_result_saxpy64),
                getString(R.string.hqa_cpustress_apmcu_result_adv_sim),
                getString(R.string.hqa_cpustress_apmcu_result_idle2max), };
        if (CpuStressTestService.CORE_NUMBER_4 <= CpuStressTestService.sCoreNumber) {
            testDetails[INDEX_NEON] = getString(R.string.hqa_cpustress_apmcu_result_neon);
        }

        for (int i = 0; i < testDetails.length; i++) {
            TestData testData = testDataSet.getTestData(i);
            if (testData.testedCount != 0) {
                sb.append(String.format(testDetails[i], testData.passedCount, testData.testedCount,
                        testData.passedCount * PERCENT / testData.testedCount));
            }
            sb.append("\t");
        }

        Log.v("@M_" + TAG, "test result: " + sb.toString());
        mTvResult.setText(sb.toString());
    }

    @Override
    public void onClick(View arg0) {
        if (mBtnStart.getId() == arg0.getId()) {
            TestDataSet testDataSet = mBoundService.getTestDataSet(ApMcu.class.getSimpleName());
            Log.v("@M_" + TAG, mBtnStart.getText() + " is clicked");
            if (getResources().getString(R.string.hqa_cpustress_apmcu_start)
                    .equals(mBtnStart.getText())) {
                // Start test
                long count = 0;
                try {
                    count = Long.valueOf(mEtLoopCount.getText().toString());
                } catch (NumberFormatException nfe) {
                    Log.d("@M_" + TAG, "Loopcount string: " + mEtLoopCount.getText()
                            + nfe.getMessage());
                    Toast.makeText(this,
                            R.string.hqa_cpustress_toast_loopcount_error,
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                testDataSet.leftTestCount = count;
                Log.d("@M_" + TAG, "testDataSet.leftTestCount:" + testDataSet.leftTestCount);
                int cbMask = 0;
                for (int i = 0; i < APMCU_TEST_INDEXS.length; i++) {
                    TestData td = testDataSet.getTestData(i);
                    td.enabled = mCbArray[i].isChecked();
                }
                mBoundService.startApmcuTest(testDataSet);
                updateStartUi();
            } else {
                // Stop test
                showDialog(DIALOG_WAIT);
                mBoundService.stopTest();
            }
        } else {
            Log.v("@M_" + TAG, "Unknown event");
        }
    }

    /**
     * Update test result on UI
     */
    private void updateStartUi() {
        Log.d("@M_" + TAG, "updateStartUi");
        mEtLoopCount.setEnabled(false);
        mBtnStart.setText(R.string.hqa_cpustress_apmcu_stop);
        for (int i = 0; i < mTvArray.length; i++) {
            mTvArray[i].setText(R.string.hqa_cpustress_result);
        }
        mTvResult.setText(R.string.hqa_cpustress_result);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.isEnabled()) {
            int cbMask = 0;
            TestDataSet testDataSet = mBoundService.getTestDataSet(ApMcu.class.getSimpleName());
            for (int i = 0; i < APMCU_TEST_INDEXS.length; i++) {
                TestData data = testDataSet.getTestData(i);
                if (data != null) {
                    data.enabled = mCbArray[i].isChecked();
                }
            }
        }
    }
}
