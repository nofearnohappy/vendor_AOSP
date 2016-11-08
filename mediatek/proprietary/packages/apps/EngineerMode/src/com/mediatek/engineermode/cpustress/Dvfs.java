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

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.engineermode.Elog;
import com.mediatek.engineermode.R;
/**
 * DVFS test Activity.
 */
public class Dvfs extends CpuStressCommon implements OnClickListener {
    private static final String TAG = "EM/CpuStress_DVFS";
    private static final int OPP_NUM = 8;
    private static final int HANDLE_GET_OPPNUM = 3;
    private static final int DIALOG_WAIT = 1;
    private static final int DIALOG_WAIT_STOP = 2;

    private CheckBox[] mCbOpp = new CheckBox[OPP_NUM];
    private EditText mEtInterval = null;
    private Button mBtnStart = null;
    private int mOppNumber;
    private TextView mTxResult = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hqa_cpustress_dvfs);
        initUiComponents();
        initHandler();
    }
    private void initHandler() {
        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                Elog.v(TAG, "mHandler receive message: " + msg.what);
                switch (msg.what) {
                case INDEX_UPDATE_RADIOBTN:
                    updateStartTestView(); // service connected, update view by last state.
                    break;
                case INDEX_UPDATE_RADIOGROUP:
                    updateTestResult(); // update oppidx
                    break;
                case HANDLE_GET_OPPNUM:
                    removeDialog(DIALOG_WAIT);  // update opp checkbox
                    updateComponents(msg.arg1, msg.arg2);
                    break;
                default:
                    break;
                }
                super.handleMessage(msg);
            }
        };
    }
    private void initUiComponents() {
        mCbOpp[0] = (CheckBox) findViewById(R.id.dvfs_op_0);
        mCbOpp[1] = (CheckBox) findViewById(R.id.dvfs_op_1);
        mCbOpp[2] = (CheckBox) findViewById(R.id.dvfs_op_2);
        mCbOpp[3] = (CheckBox) findViewById(R.id.dvfs_op_3);
        mCbOpp[4] = (CheckBox) findViewById(R.id.dvfs_op_4);
        mCbOpp[5] = (CheckBox) findViewById(R.id.dvfs_op_5);
        mCbOpp[6] = (CheckBox) findViewById(R.id.dvfs_op_6);
        mCbOpp[7] = (CheckBox) findViewById(R.id.dvfs_op_7);
        mEtInterval = (EditText) findViewById(R.id.dvfs_interval);
        mBtnStart = (Button) findViewById(R.id.dvfs_btn_start);
        mBtnStart.setOnClickListener(this);
        mTxResult = (TextView) findViewById(R.id.dvfs_result);
    }

    private void updateComponents(int idx, int oppNum) {
        mOppNumber = oppNum;
        for (int i = 0; i < OPP_NUM; i++) {
            if (i < mOppNumber) {
                mCbOpp[i].setVisibility(View.VISIBLE);
            } else {
                mCbOpp[i].setVisibility(View.GONE);
            }
        }
        mTxResult.setText(Integer.toString(idx));
    }
    private void updateStartTestView() {
        DvfsTest data = mBoundService.getDvfsTest();
        if (data == null) {
            return;
        }
        for (int i = 0; i < data.mOppNumber; i++) {
            if ((data.mOppCode & (1 << i)) != 0) {
                mCbOpp[i].setChecked(true);
            } else {
                mCbOpp[i].setChecked(false);
            }
        }
        mTxResult.setText(Integer.toString(data.mResultIdx));
        mEtInterval.setText(Integer.toString(data.mInterval));
        updateTestView(data.mIsRunning);
    }
    private void updateTestResult() {
        DvfsTest data = mBoundService.getDvfsTest();
        if (data == null) {
            return;
        }
        mTxResult.setText(Integer.toString(data.mResultIdx));
        if (!data.mIsRunning) {
            updateTestView(false);
            removeDialog(DIALOG_WAIT_STOP);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        showDialog(DIALOG_WAIT);
        mOppNumber = 0;
        new Thread(new Runnable() {
            public void run() {
                int num = DvfsTest.getOppNumber();
                int idx = DvfsTest.getOppIdx();
                Message msg = mHandler.obtainMessage(HANDLE_GET_OPPNUM);
                msg.arg1 = idx;
                msg.arg2 = num;
                mHandler.sendMessage(msg);
            }
        }).start();
    }
    @Override
    protected Dialog onCreateDialog(int id) {
        ProgressDialog dialog = null;
        if (id == DIALOG_WAIT) {
            dialog = new ProgressDialog(this);
            dialog.setTitle(R.string.hqa_cpustress_dialog_waiting_title);
            dialog.setMessage(getString(R.string.hqa_cpustress_dialog_waiting_message));
            dialog.setCancelable(false);
            dialog.setIndeterminate(true);
        } else if (id == DIALOG_WAIT_STOP) {
            dialog = new ProgressDialog(this);
            dialog.setTitle(R.string.hqa_cpustress_dialog_waiting_title);
            dialog.setMessage(getString(R.string.hqa_cpustress_dvfs_stopping_message));
            dialog.setCancelable(false);
            dialog.setIndeterminate(true);
        }
        return dialog;
    }
    @Override
    public void onClick(View arg0) {
        if (mBtnStart.getId() == arg0.getId()) {
            Elog.v(TAG, mBtnStart.getText() + " is clicked");
            if (mBtnStart.getText().toString().equals(
                    getResources().getString(
                            R.string.hqa_cpustress_dvfs_start))) {
                int interval = 0;  // check interval is valid or not
                try {
                    interval = Integer.valueOf(mEtInterval.getText().toString());
                } catch (NumberFormatException nfe) {
                    Elog.d(TAG, nfe.getMessage());
                    Toast.makeText(Dvfs.this, "Interval value error",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                int oppCode = 0;  // check if opp is selected.
                for (int i = 0; i < mOppNumber; i++) {
                    if (mCbOpp[i].isChecked()) {
                        oppCode |= 1 << i;
                    }
                }
                if (oppCode == 0) {
                    Toast.makeText(Dvfs.this, "Please select opp to start",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                updateTestView(true);
                if (null != mBoundService) {
                    mBoundService.startDvfsTest(mOppNumber, oppCode, interval);
                }
            } else {
                showDialog(DIALOG_WAIT_STOP);
                if (null != mBoundService) {
                    mBoundService.stopTest();
                    mBoundService.updateWakeLock();
                }
            }
        } else {
            Elog.v(TAG, "Unknown event");
        }
    }
    private void updateTestView(boolean isRun) {
        if (isRun) {
            mBtnStart.setText(R.string.hqa_cpustress_dvfs_stop);
            mEtInterval.setEnabled(false);
            for (int i = 0; i < mOppNumber; i++) {
                mCbOpp[i].setEnabled(false);
            }
        } else {
            mBtnStart.setText(R.string.hqa_cpustress_dvfs_start);
            mEtInterval.setEnabled(true);
            for (int i = 0; i < mOppNumber; i++) {
                mCbOpp[i].setEnabled(true);
            }
        }
    }
}
