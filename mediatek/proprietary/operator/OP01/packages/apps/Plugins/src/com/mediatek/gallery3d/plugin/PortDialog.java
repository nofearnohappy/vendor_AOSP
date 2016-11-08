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

/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.mediatek.gallery3d.plugin;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.mediatek.op01.plugin.R;

/**
 * OP01 plugin implementation of AlertDialog.
 */
public class PortDialog extends AlertDialog implements TextWatcher,
        DialogInterface.OnClickListener {
    private static final String TAG = "Gallery2/VideoPlayer/PortDialog";
    private static final boolean LOG = true;

    private static final int UNKNOWN_PORT = -1;
    private static final int ERROR_NONE = -1;
    private static final int ERROR_MAX_EMPTY = 0;
    private static final int ERROR_MIN_EMPTY = 1;
    private static final int ERROR_MAX_INVALID = 2;
    private static final int ERROR_MIN_INVALID = 3;

    private static final String SETTING_KEY_MAX_PORT = MediaStore.Streaming.Setting.MAX_UDP_PORT;
    private static final String SETTING_KEY_MIN_PORT = MediaStore.Streaming.Setting.MIN_UDP_PORT;

    private static final int BTN_OK = DialogInterface.BUTTON_POSITIVE;
    private static final int BTN_CANCEL = DialogInterface.BUTTON_NEGATIVE;
    private static final int RTSP_MIN_UDP_PORT = 1024;

    private Context mContext;
    private ContentResolver mCr;

    private View mView;
    private EditText mMaxField;
    private EditText mMinField;
    private TextView mMaxErrMsg;
    private TextView mMinErrMsg;

    private String mMax;
    private String mMin;

    /**
     * @hide
     *
     * @param context context instance
     */
    public PortDialog(Context context) {
        super(context);
        mContext = context;
        mCr = mContext.getContentResolver();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTitle(R.string.udp_port_settings);
        mView = getLayoutInflater().inflate(R.layout.m_port_dialog, null);
        if (mView != null) {
            setView(mView);
        }
        //setInverseBackgroundForced(true);

        mMaxField = (EditText) mView.findViewById(R.id.max_port);
        mMinField = (EditText) mView.findViewById(R.id.min_port);

        if (mMaxField != null && mMinField != null) {
            int maxPort = Settings.System.getInt(mCr, SETTING_KEY_MAX_PORT, UNKNOWN_PORT);
            if (maxPort == UNKNOWN_PORT) {
                mMax = "";
            } else {
                try {
                    mMax = Integer.toString(maxPort);
                } catch (NumberFormatException ex) {
                    Log.e(TAG, ex.toString());
                    mMax = "";
                }
            }
            mMaxField.setText(mMax != null ? mMax : "");
            mMaxField.addTextChangedListener(this);

            int minPort = Settings.System.getInt(mCr, SETTING_KEY_MIN_PORT, UNKNOWN_PORT);
            if (minPort == UNKNOWN_PORT) {
                mMin = "";
            } else {
                try {
                    mMin = Integer.toString(minPort);
                } catch (NumberFormatException ex) {
                    Log.e(TAG, ex.toString());
                    mMin = "";
                }
            }
            mMinField.setText(mMin != null ? mMin : "");
            mMinField.addTextChangedListener(this);
        }

        mMaxErrMsg = (TextView) mView.findViewById(R.id.max_port_err_msg);
        if (mMaxErrMsg != null) {
            mMaxErrMsg.setText("");
        }

        mMinErrMsg = (TextView) mView.findViewById(R.id.min_port_err_msg);
        if (mMinErrMsg != null) {
            mMinErrMsg.setText("");
        }

        setButton(BTN_OK, mContext.getString(android.R.string.ok), this);
        setButton(BTN_CANCEL, mContext.getString(android.R.string.cancel), this);

        super.onCreate(savedInstanceState);

        validate();

    }

    /**
     * @hide
     *
     * @param dialogInterface dialogInterface instance
     * @param button button type
     */
    public void onClick(DialogInterface dialogInterface, int button) {
        if (button == BTN_OK) {
            savePort();
        }
    }

    /**
     * @hide
     *
     * @param s string
     * @param start start index
     * @param before before index
     * @param count char count
     */
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    /**
     * @hide
     *
     * @param s string
     * @param start start index
     * @param count char count
     * @param after after index
     */
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    /**
     * @hide
     *
     * @param editable editable or not
     */
    public void afterTextChanged(Editable editable) {
        validate();
    }

    private void validate() {
        mMax = mMaxField.getText().toString().trim();
        mMin = mMinField.getText().toString().trim();
        boolean isValid = true;

        mMaxField.setHint("");
        mMinField.setHint("");
        mMaxErrMsg.setText("");
        mMinErrMsg.setText("");

        if (mMax != null && mMin != null) {
            if (mMax.length() == 0 && mMin.length() != 0) {
                showError(ERROR_MAX_EMPTY);
                isValid = false;
            }
            if (mMax.length() != 0 && mMin.length() == 0) {
                showError(ERROR_MIN_EMPTY);
                isValid = false;
            }
            int maxPort = UNKNOWN_PORT;
            if (mMax.length() != 0) {
                try {
                    maxPort = Integer.parseInt(mMax);
                    if (maxPort <= 0 || maxPort > 0xFFFF) {
                        showError(ERROR_MAX_INVALID);
                        isValid = false;
                    }
                } catch (NumberFormatException ex) {
                    Log.w(TAG, ex.toString());
                    showError(ERROR_MAX_INVALID);
                    isValid = false;
                }
            }
            int minPort = UNKNOWN_PORT;
            if (mMin.length() != 0) {
                try {
                    minPort = Integer.parseInt(mMin);
                    if (minPort < RTSP_MIN_UDP_PORT || minPort > 0xFFFF) {
                        showError(ERROR_MIN_INVALID);
                        isValid = false;
                    }
                } catch (NumberFormatException ex) {
                    Log.w(TAG, ex.toString());
                    showError(ERROR_MIN_INVALID);
                    isValid = false;
                }
            }
            if (maxPort != UNKNOWN_PORT && minPort != UNKNOWN_PORT && maxPort < minPort) {
                showError(ERROR_MAX_INVALID);
                isValid = false;
            }

            if (getButton(BTN_OK) != null) {
                if (isValid) {
                    getButton(BTN_OK).setEnabled(true);
                } else {
                    getButton(BTN_OK).setEnabled(false);
                }
            }

        }
    }

    private void showError(int errCode) {
        String[] errMsg = mContext.getResources().getStringArray(R.array.proxy_error);

        switch (errCode) {
        case ERROR_MAX_EMPTY:
            mMaxField.setHint(errMsg[ERROR_MIN_EMPTY]); //ERROR_MAX_EMPTY
            break;

        case ERROR_MIN_EMPTY:
            mMinField.setHint(errMsg[ERROR_MIN_EMPTY]);
            break;

        case ERROR_MAX_INVALID:
            mMaxErrMsg.setText(errMsg[ERROR_MIN_INVALID]); //ERROR_MAX_INVALID
            break;

        case ERROR_MIN_INVALID:
            mMinErrMsg.setText(errMsg[ERROR_MIN_INVALID]);
            break;
        default:
            break;
        }

        if (getButton(BTN_OK) != null) {
            getButton(BTN_OK).setEnabled(false);
        }

    }

    private void savePort() {
        boolean save = false;
        if (mMax != null && mMin != null) {
            if (mMax.length() != 0 && mMin.length() != 0) {
                try {
                    Settings.System.putString(mCr, SETTING_KEY_MAX_PORT, mMax);
                    Settings.System.putString(mCr, SETTING_KEY_MIN_PORT, mMin);
                    save = true;
                } catch (final SQLiteException e) {
                    e.printStackTrace();
                }
            }
        }
        if (!save) {
            Settings.System.putString(mCr, SETTING_KEY_MAX_PORT, String.valueOf(UNKNOWN_PORT));
            Settings.System.putString(mCr, SETTING_KEY_MIN_PORT, String.valueOf(UNKNOWN_PORT));
        }
    }
}
