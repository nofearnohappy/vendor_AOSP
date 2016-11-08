/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.bluetooth.bpp;

import com.mediatek.bluetooth.R;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Slog;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import android.app.NotificationManager;


public class BluetoothBppAuthenticating extends AlertActivity
                                  implements DialogInterface.OnClickListener {

    private static final String TAG = "BluetoothBppAuthenticating";

    private AlertController.AlertParams mPara;
    private View mView = null;
    private EditText mPasscodeEdit;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v("@M_" + TAG, "OnCreate");

        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String action = intent.getAction();


        setUpDialog();
    }

    @Override
    protected void onStart() {
        Log.v("@M_" + TAG, "onStart");

        super.onStart();     
    }


    @Override
    protected void onDestroy() {
        Log.v("@M_" + TAG, "onDestroy()");

        super.onDestroy();
    }

    @Override
    protected void onStop() {
        Log.v("@M_" + TAG, "onStop()");
        
        super.onStop();
    }


    private void setUpDialog() {
        Log.v("@M_" + TAG, "setUpDialog");

        //final AlertController.AlertParams p = mAlertParams;
        mPara = mAlertParams;
        mPara.mIconId = android.R.drawable.ic_dialog_info;
        mPara.mTitle = getString(R.string.app_name);

        mPara.mPositiveButtonText = getString(R.string.auth_ok);
        mPara.mPositiveButtonListener = this;
        mPara.mNegativeButtonText = getString(R.string.auth_cancel);
        mPara.mNegativeButtonListener = this;

        mPara.mView = createView();
        setupAlert();
    }


    private View createView() {
        Log.v("@M_" + TAG, "createView");

        mView = getLayoutInflater().inflate(R.layout.bt_bpp_authenticating_dialog, null);

        mPasscodeEdit = (EditText)mView.findViewById(R.id.pass_code_edit);

        return mView;
    }



    public void onClick(DialogInterface dialog, int which) {
        Log.v("@M_" + TAG, "onClick");

        if (which ==  DialogInterface.BUTTON_POSITIVE) {
            Log.v("@M_" + TAG, "positive button");

            Intent intent = new Intent(this, BluetoothBppManager.class);
            intent.putExtra("action", BluetoothBppManager.ACTION_AUTH_INFO);
            intent.putExtra(BluetoothBppManager.EXTRA_AUTH_PASSWD, mPasscodeEdit.getText().toString());
            startService(intent);

        }
        else if (which == DialogInterface.BUTTON_NEGATIVE) {
            Log.v("@M_" + TAG, "negative button");

            Intent intent = new Intent(this, BluetoothBppManager.class);
            intent.putExtra("action", BluetoothBppManager.ACTION_CANCEL);
            startService(intent);

        }
        finish();
    }
}
