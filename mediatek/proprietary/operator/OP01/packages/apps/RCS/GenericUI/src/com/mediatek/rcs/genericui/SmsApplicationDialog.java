/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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

/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.rcs.genericui;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.internal.telephony.SmsApplication;
import com.android.internal.telephony.SmsApplication.SmsApplicationData;

/**
 * confirm SMS app dialog.
 */
public class SmsApplicationDialog extends AlertActivity
        implements DialogInterface.OnClickListener {
    private static final String TAG = "SmsApplicationDialog";

    private String mPackageName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setFinishOnTouchOutside(false);

        final AlertController.AlertParams p = mAlertParams;
        p.mTitle = getString(R.string.sms_change_default_dialog_title);
        p.mMessage = createMessage();
        if (p.mMessage == null) {
            SmsApplication.setDefaultApplication(mPackageName, this);
            Log.d("@M_" + TAG, " onCreate() set");
            finish();
        }
        p.mPositiveButtonText = getString(android.R.string.yes);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(android.R.string.no);
        p.mNegativeButtonListener = this;
        setupAlert();
    }

    private String createMessage() {
        Log.d("@M_" + TAG, " createView()");
        mPackageName = getIntent().getStringExtra("packageName");

        SmsApplicationData newSmsApplicationData =
            SmsApplication.getSmsApplicationData(mPackageName, this);

        SmsApplicationData oldSmsApplicationData = null;
        ComponentName oldSmsComponent = SmsApplication.getDefaultSmsApplication(this, true);
        if (oldSmsComponent != null) {
            oldSmsApplicationData = SmsApplication.getSmsApplicationData(
                    oldSmsComponent.getPackageName(), this);
            if (oldSmsApplicationData.mPackageName.equals(
                    newSmsApplicationData.mPackageName)) {
                Log.d("@M_" + TAG, " createView() same package");
                return null;
            }
        }

        if (oldSmsApplicationData == null) {
            Log.d("@M_" + TAG, " createView() old package is null");
            return null;
        }

        String summary = getString(R.string.sms_change_default_dialog_text,
                                newSmsApplicationData.mApplicationName,
                                oldSmsApplicationData.mApplicationName);
        return summary;
    }

    @Override
    protected void onResume() {
        Log.d("@M_" + TAG, " onResume()");
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        Log.d("@M_" + TAG, " onDestroy()");
        super.onDestroy();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                SmsApplication.setDefaultApplication(mPackageName, this);
                finish();
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                finish();
                break;
            default:
                /// do nothing.
        }
    }

}
