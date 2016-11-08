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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.engineermode.ChipSupport;
import com.mediatek.engineermode.Elog;
import com.mediatek.engineermode.FeatureHelpPage;
import com.mediatek.engineermode.R;
import com.mediatek.engineermode.emsvr.AFMFunctionCallEx;
import com.mediatek.engineermode.emsvr.FunctionReturn;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class CpuStressTest extends CpuStressCommon implements OnItemClickListener,
        RadioGroup.OnCheckedChangeListener, CheckBox.OnCheckedChangeListener,
        DialogInterface.OnClickListener, View.OnClickListener {

    private static final String TAG = "EM/CpuStress";
    public static final int INDEX_DEFAULT = 0;
    public static final int INDEX_TEST = 1;
    public static final int INDEX_SINGLE = 2;
    public static final int INDEX_DUAL = 3;
    public static final int INDEX_TRIPLE = 4;
    public static final int INDEX_QUAD = 5;
    public static final int INDEX_OCTA = 6;
    public static final int INDEX_CUSTOM = 7;
    private static final int RADIO_BUTTON_COUNT = 8;
    public static final int TEST_BACKUP = 20;
    public static final int TEST_RESTORE = 40;
    private static final int ITEM_COUNT = 3;
    private static final String TYPE_LOAD_ENG = "eng";
    private static final String ERROR = "ERROR";

    static final int DISABLE_FORCE_CORE = 7;
    static final int FORCE_CORE_RUN = 1;
    static final int FORCE_CORE_IDLE = 2;
    static final int FORCE_CORE_OFF = 0;

    private static final String[] HQA_CPU_STRESS_TEST_ITEMS = new String[ITEM_COUNT];
    private ArrayList<String> mListCpuTestItem = null;
    private RadioButton mRbDefault = null;
    private RadioButton mRbTest = null;
    private RadioButton mRbSingle = null;
    private RadioButton mRbDual = null;
    private RadioButton mRbTriple = null;
    private RadioButton mRbQuad = null;
    private RadioButton mRbOcta = null;

    private RadioButton[] mRdoBtn = new RadioButton[RADIO_BUTTON_COUNT];
    private CheckBox mCbThermal = null;

    private CheckBox[] mCbCustomForceCores = null;
    private Spinner[] mSpnCustomOpts = null;

    private int mCpuTestMode;
    private View mCustomOptsLayout = null;
    private AlertDialog mOptDialog = null;
    private CpuTestRequest mReqChoice = null;
    private String mTipMsg = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hqa_cpustress);
        if (!Build.TYPE.equals("eng")) {
            Toast.makeText(this, R.string.hqa_cpustress_toast_load_notsupport,
                    Toast.LENGTH_LONG).show();
            Log.d("@M_" + TAG, "Not eng load, finish");
            finish();
            return;
        }
        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                Log.v("@M_" + TAG, "mHandler receive message: " + msg.what);
                CpuStressTestService.sIsThermalSupport = new File(
                        CpuStressTestService.THERMAL_ETC_FILE).exists();
                switch (msg.what) {
                case INDEX_UPDATE_RADIOBTN:
                    switch (CpuStressTestService.sCoreNumber) {
                    case CpuStressTestService.CORE_NUMBER_8:
                        updateCbThermal();
                        break;
                    case CpuStressTestService.CORE_NUMBER_4:
                        updateCbThermal();
                        mRbOcta.setVisibility(View.GONE);
                        break;
                    case CpuStressTestService.CORE_NUMBER_3:
                        updateCbThermal();
                        mRbQuad.setVisibility(View.GONE);
                        mRbOcta.setVisibility(View.GONE);
                        break;
                    case CpuStressTestService.CORE_NUMBER_2:
                        updateCbThermal();
                        mRbTriple.setVisibility(View.GONE);
                        mRbQuad.setVisibility(View.GONE);
                        mRbOcta.setVisibility(View.GONE);
                        break;
                    default:
                        mRbDual.setVisibility(View.GONE);
                        mRbSingle.setVisibility(View.GONE);
                        mRbTriple.setVisibility(View.GONE);
                        mRbQuad.setVisibility(View.GONE);
                        mRbOcta.setVisibility(View.GONE);
                        mCbThermal.setVisibility(View.GONE);
                        break;
                    }
                    checkRdoBtn(CpuStressTestService.sIndexMode);
                    updateRadioGroup(!mBoundService.isTestRun());
                    break;
                case INDEX_UPDATE_RADIOGROUP:
                    updateRadioGroup(!mBoundService.isTestRun());
                    break;
                default:
                    super.handleMessage(msg);
                    break;
                }
            }
        };
        HQA_CPU_STRESS_TEST_ITEMS[0] = getString(R.string.hqa_cpustress_apmcu_name);
        HQA_CPU_STRESS_TEST_ITEMS[1] = getString(R.string.hqa_cpustress_swvideo_name);
        HQA_CPU_STRESS_TEST_ITEMS[2] = getString(R.string.hqa_cpustress_clockswitch_name);
        ListView testItemList = (ListView) findViewById(R.id.listview_hqa_cpu_main);
        RadioGroup  rgRadioGroup = (RadioGroup) findViewById(R.id.hqa_cpu_main_radiogroup);
        mRbDefault = (RadioButton) findViewById(R.id.hqa_cpu_main_raidobutton_default);
        mRbTest = (RadioButton) findViewById(R.id.hqa_cpu_main_raidobutton_test);
        mRbSingle = (RadioButton) findViewById(R.id.hqa_cpu_main_raidobutton_single);
        mRbDual = (RadioButton) findViewById(R.id.hqa_cpu_main_raidobutton_dual);
        mRbTriple = (RadioButton) findViewById(R.id.hqa_cpu_main_raidobutton_triple);
        mRbQuad = (RadioButton) findViewById(R.id.hqa_cpu_main_raidobutton_quad);
        mRbOcta = (RadioButton) findViewById(R.id.hqa_cpu_main_raidobutton_octa);
        mCbThermal = (CheckBox) findViewById(R.id.hqa_cpu_main_checkbox);
        mRdoBtn[0] = mRbDefault;
        mRdoBtn[1] = mRbTest;
        mRdoBtn[2] = mRbSingle;
        mRdoBtn[3] = mRbDual;
        mRdoBtn[4] = mRbTriple;
        mRdoBtn[5] = mRbQuad;
        mRdoBtn[6] = mRbOcta;
        mRdoBtn[7] = (RadioButton) findViewById(R.id.hqa_cpu_main_raidobutton_custom);
        mListCpuTestItem = new ArrayList<String>(Arrays.asList(HQA_CPU_STRESS_TEST_ITEMS));
        if (ChipSupport.MTK_6795_SUPPORT == ChipSupport.getChip()) {
            mListCpuTestItem.add(getString(R.string.hqa_cpustress_dvfs));
        }
        mListCpuTestItem.add(getString(R.string.help));
        if (ChipSupport.MTK_6755_SUPPORT == ChipSupport.getChip()) {
            mListCpuTestItem.remove(getString(R.string.hqa_cpustress_clockswitch_name));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, mListCpuTestItem);
        testItemList.setAdapter(adapter);
        testItemList.setOnItemClickListener(this);
        setListViewItemsHeight(testItemList);
        rgRadioGroup.setOnCheckedChangeListener(this);
        mCbThermal.setOnCheckedChangeListener(this);
        updateRadioGroup(false);
        mCpuTestMode = CpuStressTestService.getCpuTestMode();
        initLayout(mCpuTestMode);
        startService(new Intent(this, CpuStressTestService.class));
        Log.i("@M_" + TAG, "start cpu test service");
    }

    private void updateCbThermal() {
        mCbThermal.setEnabled(false);
        mCbThermal.setChecked(CpuStressTestService.sIsThermalDisabled);
        mCbThermal.setEnabled(CpuStressTestService.sIsThermalSupport);
    }

    private void checkRdoBtn(int indexRdoBtn) {
        mRdoBtn[indexRdoBtn].setEnabled(false);
        mRdoBtn[indexRdoBtn].setChecked(true);
        mRdoBtn[indexRdoBtn].setEnabled(true);
    }

    /**
     * Update radio group and check box status
     *
     * @param testRunning
     *            Test running or not
     */
    protected void updateRadioGroup(boolean testRunning) {
        for (int i = 0; i < RADIO_BUTTON_COUNT; i++) {
            boolean enabled = testRunning;
            if (mCpuTestMode == CpuStressTestService.MODE_CUSTOM_V2) {
                if (mBoundService != null) {
                    CpuTestRequest record = mBoundService.getForceCoreReqRecord(true);
                    for (int n = 0; n < CpuStressTestService.sCoreNumber; n++) {
                        if (record.getCpuTestCore(n) == FORCE_CORE_IDLE) {
                            enabled = false;
                            break;
                        }
                    }
                }
            }
            mRdoBtn[i].setEnabled(enabled);
        }
        mCbThermal.setEnabled(testRunning);
        if (testRunning && (!CpuStressTestService.sIsThermalSupport)) {
            mCbThermal.setEnabled(false);
        }
        if (mCbCustomForceCores != null) {
            for (int i = 0; i < mCbCustomForceCores.length; i++) {
                mCbCustomForceCores[i].setEnabled(testRunning);
            }
        }
        removeDialog(DIALOG_WAIT);
    }

    private boolean isAllowEnterItem(int itemIndex) {
        if (0 == CpuStressTestService.sIndexMode) {
            mTipMsg = getString(R.string.hqa_cpustress_toast_mode);
            return false;
        }
        if (mBoundService != null) {
            if (CpuStressTestService.sIndexMode == INDEX_CUSTOM) {
                if (mCpuTestMode == CpuStressTestService.MODE_CHECK_CUSTOM ||
                        mCpuTestMode == CpuStressTestService.MODE_CUSTOM_V2) {
                    CpuTestRequest record = mBoundService.getForceCoreReqRecord(true);
                    for (int i = 0; i < CpuStressTestService.sCoreNumber; i++) {
                        int val = record.getCpuTestCore(i);
                        if (val > 0 && val < DISABLE_FORCE_CORE) {
                            return true;
                        }
                    }
                    mTipMsg = getString(R.string.hqa_cpustress_custom_mode_tip);
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        Intent intent = null;
        Log.i("@M_" + TAG, "User select: " + mListCpuTestItem.get(arg2));
        if (mListCpuTestItem.get(arg2).equals(getString(R.string.help))) {
            intent = new Intent(this, FeatureHelpPage.class);
            intent.putExtra(FeatureHelpPage.HELP_TITLE_KEY, R.string.help);
            if (ChipSupport.MTK_6589_SUPPORT <= ChipSupport.getChip()) {
                intent.putExtra(FeatureHelpPage.HELP_MESSAGE_KEY,
                        R.string.hqa_cpustress_help_msg_new);
            } else {
                intent.putExtra(FeatureHelpPage.HELP_MESSAGE_KEY,
                        R.string.hqa_cpustress_help_msg);
            }
            startActivity(intent);
            return;
        }
        if (!isAllowEnterItem(arg2)) {
            Toast.makeText(this, mTipMsg, Toast.LENGTH_SHORT).show();
            Log.d("@M_" + TAG, "Not select mode");
        } else {
            if (mListCpuTestItem.get(arg2).equals(
                    getString(R.string.hqa_cpustress_apmcu_name))) {
                intent = new Intent(this, ApMcu.class);
            } else if (mListCpuTestItem.get(arg2).equals(
                    getString(R.string.hqa_cpustress_swvideo_name))) {
                intent = new Intent(this, SwVideoCodec.class);
            } else if (mListCpuTestItem.get(arg2).equals(
                    getString(R.string.hqa_cpustress_clockswitch_name))) {
                intent = new Intent(this, ClockSwitch.class);
                if (2 > CpuStressTestService.sIndexMode
                        && CpuStressTestService.CORE_NUMBER_2 <= CpuStressTestService.sCoreNumber) {
                    Toast.makeText(this, R.string.hqa_cpustress_toast_not_force,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            } else if (mListCpuTestItem.get(arg2).equals(
                    getString(R.string.hqa_cpustress_dvfs))) {
                intent = new Intent(this, Dvfs.class);
            }
            if (null == intent) {
                Toast.makeText(this, R.string.hqa_cpustress_toast_item_error,
                        Toast.LENGTH_LONG).show();
                Log.d("@M_" + TAG, "Select error");
            } else {
                this.startActivity(intent);
            }
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        Log.v("@M_" + TAG, "Enter onCheckedChanged: " + checkedId);
        int index = 0;
        for (; index < RADIO_BUTTON_COUNT; index++) {
            if (checkedId == mRdoBtn[index].getId()) {
                break;
            }
        }
        int lastIndex = CpuStressTestService.sIndexMode;
        if (index < RADIO_BUTTON_COUNT) {
            mBoundService.setIndexMode(index);
        }
        handleModeIndexChange(lastIndex, index);
    }

    private void handleModeIndexChange(int lastIndex, int index) {
        if (lastIndex == index) {
            Elog.d(TAG, "handleModeIndexChange lastIndex == index");
            return;
        }
        if (mCpuTestMode == CpuStressTestService.MODE_CHECK_CUSTOM ||
                mCpuTestMode == CpuStressTestService.MODE_CUSTOM_V2) {
            mBoundService.requestTestManner(lastIndex, false);
            mBoundService.requestTestManner(index, true);
            if (lastIndex == INDEX_CUSTOM) {
                onSelectCustomMode(false);
            }
        }
    }

    private boolean onTapForceCoreCheck(CheckBox cb) {
        if (mBoundService == null) {
            return false;
        }
        int target = -1;
        for (int i = 0; i < mCbCustomForceCores.length; i++) {
            if (cb.getId() == mCbCustomForceCores[i].getId()) {
                target = i;
                break;
            }
        }
        Elog.d(TAG, "onTapForceCoreCheck, INDEX:" + target + " checked:" + cb.isChecked());
        if (target == -1) {
            return false;
        }
        if (target == 4 && !cb.isChecked()) {
            boolean selectBig = false;
            for (int i = 5; i < mCbCustomForceCores.length; i++) {
                CheckBox check = mCbCustomForceCores[i];
                if (check.isChecked()) {
                    selectBig = true;
                    break;
                }
            }
            if (selectBig) {
                CheckBox cbCore4 = mCbCustomForceCores[4];
                if (!cbCore4.isChecked()) {
                    cbCore4.setOnCheckedChangeListener(null);
                    cbCore4.setChecked(true);
                    cbCore4.setOnCheckedChangeListener(this);
                    Toast.makeText(this, "Select Big core must force core 4", Toast.LENGTH_SHORT).show();
                    return true;
                }
            }
        }
        if (target > 4 && cb.isChecked()) {
            CheckBox cbCore4 = mCbCustomForceCores[4];
            if (!cbCore4.isChecked()) {
                cbCore4.setOnCheckedChangeListener(null);
                cbCore4.setChecked(true);
                cbCore4.setOnCheckedChangeListener(this);
                mReqChoice.setCpuTestCore(4, FORCE_CORE_RUN);
            }
        }
        int val = FORCE_CORE_OFF;
        if (cb.isChecked()) {
            val = FORCE_CORE_RUN;
        }
        mReqChoice.setCpuTestCore(target, val);
        return true;
    }

    private void onSelectCustomMode(boolean selected) {
        if (mBoundService == null) {
            return;
        }
        if (!selected) {
            if (mCpuTestMode == CpuStressTestService.MODE_CHECK_CUSTOM) {
                for (int i = 1; i < CpuStressTestService.sCoreNumber; i++) {
                    CheckBox cb = mCbCustomForceCores[i];
                    cb.setOnCheckedChangeListener(null);
                    cb.setChecked(false);
                    cb.setOnCheckedChangeListener(this);
                }
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.isEnabled()) {
            if (buttonView.getId() == mCbThermal.getId()) {
                Log.v("@M_" + TAG, "check box checked: " + isChecked);
                doThermalDisable(isChecked);
            } else if (onTapForceCoreCheck((CheckBox) buttonView)) {
                Log.d("@M_" + TAG, "Done onTapForceCoreCheck");
            } else {
                Log.v("@M_" + TAG, "Unknown checkbox");
            }
        }
    }

    /**
     * Set thermal disable/enable
     *
     * @param disableThermal
     *            True to disable thermal, false to enable thermal
     */
    private void doThermalDisable(boolean disableThermal) {
        Log.v("@M_" + TAG, "Enter doThermalDisable: " + disableThermal);
        CpuStressTestService.sIsThermalDisabled = disableThermal;
        StringBuilder build = new StringBuilder();
        AFMFunctionCallEx functionCall = new AFMFunctionCallEx();
        boolean result = functionCall.startCallFunctionStringReturn(
                AFMFunctionCallEx.FUNCTION_EM_CPU_STRESS_TEST_THERMAL);
        functionCall.writeParamNo(1);
        functionCall.writeParamInt(disableThermal ? 0 : 1);
        if (result) {
            FunctionReturn r;
            do {
                r = functionCall.getNextResult();
                if (r.mReturnString.isEmpty()) {
                    break;
                }
                build.append(r.mReturnString);
            } while (r.mReturnCode == AFMFunctionCallEx.RESULT_CONTINUE);
            if (r.mReturnCode == AFMFunctionCallEx.RESULT_IO_ERR) {
                Log.d("@M_" + TAG, "AFMFunctionCallEx: RESULT_IO_ERR");
                build.replace(0, build.length(), ERROR);
            }
        } else {
            Log.d("@M_" + TAG, "AFMFunctionCallEx return false");
            build.append(ERROR);
        }
        Log.v("@M_" + TAG, "doThermalDisable response: " + build.toString());
    }

    private void initCustomLayoutV2() {
        int[][] customOptIds = {
                {R.id.custom_opts_core1_tv, R.id.custom_opts_core1_spn},
                {R.id.custom_opts_core2_tv, R.id.custom_opts_core2_spn},
                {R.id.custom_opts_core3_tv, R.id.custom_opts_core3_spn},
                {R.id.custom_opts_core4_tv, R.id.custom_opts_core4_spn},
                {R.id.custom_opts_core5_tv, R.id.custom_opts_core5_spn},
                {R.id.custom_opts_core6_tv, R.id.custom_opts_core6_spn},
                {R.id.custom_opts_core7_tv, R.id.custom_opts_core7_spn},
        };
        String textPrefix = getString(R.string.hqa_cpustress_main_force_core);
        mCustomOptsLayout = getLayoutInflater().inflate(R.layout.hqa_cpustress_custom_opts_v2, null);
        int coreNum = CpuStressTestService.sCoreNumber;
        mSpnCustomOpts = new Spinner[coreNum - 1];
        for (int i = 0; i < customOptIds.length; i++) {
            int spId = customOptIds[i][1];
            int tvId = customOptIds[i][0];
            TextView tv = (TextView) mCustomOptsLayout.findViewById(tvId);
            Spinner sp = (Spinner) mCustomOptsLayout.findViewById(spId);
            if (i >= coreNum - 1) {
                tv.setVisibility(View.GONE);
                sp.setVisibility(View.GONE);
                continue;
            }
            tv.setText(textPrefix + " " + (i + 1));
            String[] itemStrs = {getString(R.string.hqa_cpustress_force_core_run),
                    getString(R.string.hqa_cpustress_force_core_idle),
                    getString(R.string.hqa_cpustress_force_core_off), };
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, itemStrs);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sp.setAdapter(adapter);
            mSpnCustomOpts[i] = sp;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.hqa_cpustress_radiobutton_custom_mode));
        builder.setView(mCustomOptsLayout);
        builder.setPositiveButton(R.string.hqa_cpustress_ok, this);
        builder.setCancelable(false);
        mOptDialog = builder.create();
    }

    private void initCheckCustomLayout() {
        int[] customOptIds = {R.id.hqa_cpu_main_force_core1_cb,
                R.id.hqa_cpu_main_force_core2_cb,
                R.id.hqa_cpu_main_force_core3_cb,
                R.id.hqa_cpu_main_force_core4_cb,
                R.id.hqa_cpu_main_force_core5_cb,
                R.id.hqa_cpu_main_force_core6_cb,
                R.id.hqa_cpu_main_force_core7_cb,
                R.id.hqa_cpu_main_force_core8_cb, };
        String textPrefix = getString(R.string.hqa_cpustress_main_force_core);
        int coreNum = CpuStressTestService.sCoreNumber;
        mCbCustomForceCores = new CheckBox[coreNum];
        mCustomOptsLayout = getLayoutInflater().inflate(R.layout.hqa_cpustress_check_opts, null);
        for (int i = 0; i < customOptIds.length; i++) {
            CheckBox cb = (CheckBox) mCustomOptsLayout.findViewById(customOptIds[i]);
            if (i >= coreNum) {
                cb.setVisibility(View.GONE);
                continue;
            }
            String showText = null;
            showText = textPrefix + " " + i;
//            if (CpuStressTestService.sCpuArch == CpuStressTestService.CPU_ARCH_BIG_LITTLE) {
//                if (i < coreNum / 2) {
//                    showText = showText + " (" + getString(R.string.hqa_cpustress_core_little) + ")";
//                } else {
//                    showText = showText + " (" + getString(R.string.hqa_cpustress_core_big) + ")";
//                }
//            }
            cb.setText(showText);
            mCbCustomForceCores[i] = cb;
            cb.setOnCheckedChangeListener(this);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.hqa_cpustress_radiobutton_custom_mode));
        builder.setView(mCustomOptsLayout);
        builder.setPositiveButton(R.string.hqa_cpustress_ok, this);
        builder.setCancelable(false);
        mOptDialog = builder.create();
    }

    private void initLayout(int mode) {
        if (mode != CpuStressTestService.MODE_BACKUP_RESTORE) {
            int[] normalOptIds = {R.id.hqa_cpu_main_raidobutton_octa,
                    R.id.hqa_cpu_main_raidobutton_quad,
                    R.id.hqa_cpu_main_raidobutton_triple,
                    R.id.hqa_cpu_main_raidobutton_dual,
                    R.id.hqa_cpu_main_raidobutton_single, };
            for (int i = 0; i < normalOptIds.length; i++) {
                findViewById(normalOptIds[i]).setVisibility(View.GONE);
            }
            View customView = findViewById(R.id.hqa_cpu_main_raidobutton_custom);
            customView.setVisibility(View.VISIBLE);
            customView.setOnClickListener(this);

            if (mode == CpuStressTestService.MODE_CHECK_CUSTOM) {
                initCheckCustomLayout();
            } else if (mode == CpuStressTestService.MODE_CUSTOM_V2) {
                initCustomLayoutV2();
            }
        }
    }

    private void showCheckCustomOptions(boolean visible) {
        if (visible) {
            if (mOptDialog == null) {
                Log.d("@M_" + TAG, "mOptDialog IS NULL");
                return;
            }
            CpuTestRequest record = mBoundService.getForceCoreReqRecord(true);
            mReqChoice = record.copy(CpuTestRequest.FLAG_PACK_CPU_INFO);
            mReqChoice.setCpuTestCore(0, FORCE_CORE_RUN);
            for (int i = 0; i < CpuStressTestService.sCoreNumber; i++) {
                int val = mReqChoice.getCpuTestCore(i);
                if (val > 0 && val < DISABLE_FORCE_CORE) {
                    Log.d("@M_" + TAG, i + " set check true ");
                    mCbCustomForceCores[i].setChecked(true);
                } else {
                    mCbCustomForceCores[i].setChecked(false);
                }
            }
            mCbCustomForceCores[0].setEnabled(false);
            mCustomOptsLayout.invalidate();
            mOptDialog.show();
        } else {
            if (mOptDialog != null) {
                mOptDialog.dismiss();
            }
        }

    }

    private void showCustomOptionV2() {
        if (mOptDialog == null) {
            Log.d("@M_" + TAG, "showCustomOptionV2 mOptDialog IS NULL");
            return;
        }
        CpuTestRequest record = mBoundService.getForceCoreReqRecord(true);
        mReqChoice = record.copy(CpuTestRequest.FLAG_PACK_CPU_INFO);
        if (!mReqChoice.isSetCpuTestCore()) {
            for (int i = 0; i < mSpnCustomOpts.length; i++) {
                Spinner sp = mSpnCustomOpts[i];
                sp.setSelection(0);
            }
        } else {
            for (int i = 1; i < CpuStressTestService.sCoreNumber; i++) {
                int val = mReqChoice.getCpuTestCore(i);
                Spinner sp = mSpnCustomOpts[i - 1];
                if (val == FORCE_CORE_RUN) {
                    sp.setSelection(0);
                } else if (val == FORCE_CORE_IDLE) {
                    sp.setSelection(1);
                } else if (val == FORCE_CORE_OFF) {
                    sp.setSelection(2);
                }
            }
        }
        mOptDialog.show();
    }

    static void setListViewItemsHeight(ListView listview) {
        if (listview == null) {
            return;
        }
        ListAdapter adapter = listview.getAdapter();
        int totalHeight = 0;
        for (int i = 0; i < adapter.getCount(); i++) {
            View itemView = adapter.getView(i, null, listview);
            itemView.measure(0, 0);
            totalHeight += itemView.getMeasuredHeight();
        }
        totalHeight += (adapter.getCount() - 1) * listview.getDividerHeight();
        ViewGroup.LayoutParams params = listview.getLayoutParams();
        params.height = totalHeight;
        listview.setLayoutParams(params);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            if (mCpuTestMode == CpuStressTestService.MODE_CUSTOM_V2) {
                mReqChoice.setCpuTestCore(0, FORCE_CORE_RUN);
                for (int i = 0; i < mSpnCustomOpts.length; i++) {
                    Spinner sp = mSpnCustomOpts[i];
                    int cpuIdx = i + 1;
                    int selectIdx = sp.getSelectedItemPosition();
                    if (selectIdx == 0) {
                        mReqChoice.setCpuTestCore(cpuIdx, FORCE_CORE_RUN);
                    } else if (selectIdx == 1) {
                        mReqChoice.setCpuTestCore(cpuIdx, FORCE_CORE_IDLE);
                        updateRadioGroup(false);
                    } else if (selectIdx == 2) {
                        mReqChoice.setCpuTestCore(cpuIdx, FORCE_CORE_OFF);
                    } else {
                        Log.d("@M_" + TAG, "handle click ok; unknown selectIdx:" + selectIdx + " i:" + i);
                    }
                }
            }
            mBoundService.requestCustomForceCores(mReqChoice);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.hqa_cpu_main_raidobutton_custom) {
            if (mCpuTestMode == CpuStressTestService.MODE_CHECK_CUSTOM) {
                showCheckCustomOptions(true);
            } else if (mCpuTestMode == CpuStressTestService.MODE_CUSTOM_V2) {
                showCustomOptionV2();
            }
        }
    }

}

