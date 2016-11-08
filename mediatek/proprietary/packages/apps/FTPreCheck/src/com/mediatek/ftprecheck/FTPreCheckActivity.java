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

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Toast;

import com.mediatek.internal.telephony.ModemSwitchHandler;
import com.mediatek.internal.telephony.worldphone.WorldPhoneUtil;
import com.mediatek.internal.telephony.worldphone.WorldMode;

public class FTPreCheckActivity extends Activity {

    private static final String TAG = "FTPreCheckActivity";
    private RadioButton mStaticRadio;
    private RadioButton mDynamicRadio;
    private Button mStartButton;
    private String mCheckedType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!supportFTPreCheck()) {
            Toast.makeText(FTPreCheckActivity.this, R.string.only_support_cmcc,
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        setContentView(R.layout.ft_pre_check);

        mStaticRadio = (RadioButton) findViewById(R.id.radio_static_check);
        mDynamicRadio = (RadioButton) findViewById(R.id.radio_dynamic_check);
        mStartButton = (Button) findViewById(R.id.btn_start_check);
        mStartButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                FTPCLog.i(TAG, "start to check, mCheckedType = " + mCheckedType);
                if (mStaticRadio.isChecked()) {
                    mCheckedType = mStaticRadio.getText().toString();
                } else if (mDynamicRadio.isChecked()) {
                    mCheckedType = mDynamicRadio.getText().toString();
                } else {
                    Toast.makeText(FTPreCheckActivity.this,
                            R.string.toast_select_type, Toast.LENGTH_SHORT).show();
                    return;
                }
                goToActivity(CheckResultActivity.class, "check_type", mCheckedType);
            }
        });
    }

    /**
     * only support current loaded modem type is SGLTE or LTTG(TDD CSFB) modem.
     */
    private boolean supportFTPreCheck() {
        int modemType = getModemType();
        FTPCLog.d(TAG, "Get modem type: " + modemType);
        //L.AOSP.EARLY.DEV has no SGLTE
        if (!WorldPhoneUtil.isWorldModeSupport()) {
            if (modemType == ModemSwitchHandler.MD_TYPE_TG
                || modemType == ModemSwitchHandler.MD_TYPE_LTG) {
                return true;
            } else {
                return false;
            }
        } else {
            if (modemType == WorldMode.MD_WORLD_MODE_LTG ||
                modemType == WorldMode.MD_WORLD_MODE_LWTG ||
                modemType == WorldMode.MD_WORLD_MODE_LWCG ||
                modemType == WorldMode.MD_WORLD_MODE_LWCTG ||
                modemType == WorldMode.MD_WORLD_MODE_LTTG) {
                return true;
            } else {
                return false;
            }
        }
    }

    private int getModemType() {
        int modemType = 0;

        if (!WorldPhoneUtil.isWorldModeSupport()) {
            modemType = ModemSwitchHandler.getActiveModemType();
        } else {
            modemType = WorldMode.getWorldMode();
        }
        FTPCLog.d(TAG, "getModemType=" + modemType);
        return modemType;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater sinflater = getMenuInflater();
        if (sinflater != null) {
            sinflater.inflate(R.menu.ftpre_check, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        switch (item.getItemId()) {
        case R.id.action_settings:
            goToActivity(SettingsActivity.class);
            break;
        case R.id.action_about:
            goToActivity(AboutActivity.class);
            break;
        default:
            break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void goToActivity(Class<?> targetActivity) {
        Intent intent = new Intent(FTPreCheckActivity.this, targetActivity);
        startActivity(intent);
    }

    private void goToActivity(Class<?> targetActivity, String name, String value) {
        Intent intent = new Intent(FTPreCheckActivity.this, targetActivity);
        intent.putExtra(name, value);
        startActivity(intent);
    }

}
