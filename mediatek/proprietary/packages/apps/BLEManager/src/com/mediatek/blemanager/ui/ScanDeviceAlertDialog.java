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
 * MediaTek Inc. (C) 2014. All rights reserved.
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
package com.mediatek.blemanager.ui;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

import com.mediatek.blemanager.R;
import com.mediatek.blemanager.provider.BleConstants;

public class ScanDeviceAlertDialog {
    private static final String TAG = BleConstants.COMMON_TAG + "[ScanDeviceAlertDialog]";

    public static final int SCAN = 1;
    public static final int DELETE = 2;

    private static ScanDeviceAlertDialog sInstance;

    private AlertDialog mAlertDialog;
    private ProgressBar mProgressBar;
    private ListView mDeviceListView;
    private Context mContext;
    private ArrayList<BluetoothDevice> mFoundDevicesList;

    private ScanDeviceAlertDialog() {
    }

    public static AlertDialog show(int which, Context context,
            ArrayList<BluetoothDevice> foundDevices,
            DialogInterface.OnClickListener cancelListener,
            AdapterView.OnItemClickListener litItemClickListener) {
        Log.i(TAG, "[show] which = " + which);
        if (context == null) {
            Log.d(TAG, "[show] context is null,return!");
            return null;
        }
        if (sInstance == null) {
            sInstance = new ScanDeviceAlertDialog();
        }
        if (sInstance.mAlertDialog != null && sInstance.mAlertDialog.isShowing()) {
            sInstance.mAlertDialog.dismiss();
            sInstance.mAlertDialog = null;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View customTitleView;
        if (which == SCAN) {
            customTitleView = LayoutInflater.from(context).inflate(R.layout.scan_dialog_title_view,
                    null);
        } else {
            customTitleView = LayoutInflater.from(context).inflate(
                    R.layout.delete_dialog_title_view, null);
        }
        View view = LayoutInflater.from(context).inflate(R.layout.scan_dialog_content_view, null);
        builder.setCustomTitle(customTitleView);
        builder.setView(view);
        builder.setNegativeButton(R.string.cancel, cancelListener);
        builder.setCancelable(false);

        sInstance.mFoundDevicesList = foundDevices;
        sInstance.mContext = context;
        sInstance.mProgressBar = (ProgressBar) customTitleView
                .findViewById(R.id.scan_dialog_progress);
        sInstance.mDeviceListView = (ListView) view.findViewById(R.id.scanned_device_list);
        sInstance.mDeviceListView.setAdapter(sInstance.mDeviceAdater);
        sInstance.mDeviceListView.setOnItemClickListener(litItemClickListener);

        sInstance.mAlertDialog = builder.create();
        sInstance.mAlertDialog.show();

        return sInstance.mAlertDialog;
    }

    /**
     * dismiss current dialog
     */
    public static void dismiss() {
        Log.i(TAG, "[dismiss]...");
        if (sInstance == null) {
            return;
        }
        if (sInstance.mAlertDialog == null) {
            return;
        }
        if (sInstance.mAlertDialog.isShowing()) {
            sInstance.mAlertDialog.setCancelable(true);
            sInstance.mAlertDialog.dismiss();
        }
        if (sInstance.mFoundDevicesList != null) {
            sInstance.mFoundDevicesList.clear();
        }
        sInstance = null;
    }

    public static void notifyUi() {
        Log.i(TAG, "[notifyUi]...");
        if (sInstance == null) {
            return;
        }
        if (sInstance.mDeviceAdater == null) {
            return;
        }
        sInstance.mDeviceAdater.notifyDataSetChanged();
    }

    public static void hideProgressBar() {
        Log.i(TAG, "[hideProgressBar]...");
        if (sInstance != null && sInstance.mProgressBar != null) {
            sInstance.mProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    private BaseAdapter mDeviceAdater = new BaseAdapter() {

        @Override
        public int getCount() {
            if (mFoundDevicesList != null) {
                return mFoundDevicesList.size();
            }
            return 0;
        }

        @Override
        public Object getItem(int arg0) {
            if (mFoundDevicesList != null && mFoundDevicesList.size() > 0) {
                return mFoundDevicesList.get(arg0);
            }
            return null;
        }

        @Override
        public long getItemId(int arg0) {
            return arg0;
        }

        @Override
        public View getView(int arg0, View arg1, ViewGroup arg2) {
            Log.d(TAG, "[getView]arg1 = " + arg1);
            ViewHolder holder = null;
            if (arg1 == null) {
                arg1 = LayoutInflater.from(mContext)
                        .inflate(R.layout.scan_device_item_detail, null);
                holder = new ViewHolder();
                holder.mNameText = (TextView) arg1.findViewById(R.id.device_name);
                arg1.setTag(holder);
            } else {
                holder = (ViewHolder) arg1.getTag();
            }

            String name = mFoundDevicesList.get(arg0).getName();
            String addr = mFoundDevicesList.get(arg0).getAddress();
            if (name == null || name.trim().length() == 0) {
                holder.mNameText.setText(addr);
            } else {
                holder.mNameText.setText(name);
            }
            return arg1;
        }

        class ViewHolder {
            TextView mNameText;
        }
    };
}
