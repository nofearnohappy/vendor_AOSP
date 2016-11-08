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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.mediatek.blemanager.R;
import com.mediatek.blemanager.provider.BleConstants;

import java.util.ArrayList;

public class DeviceDefaultImageChooser extends Activity {
    private static final String TAG = BleConstants.COMMON_TAG + "[DeviceDefaultImageChooser]";

    private ArrayList<Integer> mDeviceImageList;

    private LayoutInflater mInflater;
    private GridView mGridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.default_image_chooser_layout);
        mGridView = (GridView) this.findViewById(R.id.default_image_list);

        mDeviceImageList = new ArrayList<Integer>();
        mDeviceImageList.add(R.drawable.image_baby);
        mDeviceImageList.add(R.drawable.image_key);
        mDeviceImageList.add(R.drawable.image_pet);
        mDeviceImageList.add(R.drawable.image_wallet);
        mDeviceImageList.add(R.drawable.image_watch);

        mInflater = LayoutInflater.from(this);
        mGridView.setAdapter(new DefaultImageAdapter());
        mGridView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                int item = (Integer) arg0.getItemAtPosition(arg2);
                Log.d(TAG, "[onItemClick] item : " + item);
                setActivityResult(item);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Log.i(TAG, "[onOptionsItemSelected]home key,finish activity.");
            this.finish();
        }
        return true;
    }

    private class DefaultImageAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mDeviceImageList.size();
        }

        @Override
        public Object getItem(int arg0) {
            return mDeviceImageList.get(arg0);
        }

        @Override
        public long getItemId(int arg0) {
            return arg0;
        }

        @Override
        public View getView(int arg0, View arg1, ViewGroup arg2) {
            ViewHolder holder;
            if (arg1 == null) {
                arg1 = mInflater.inflate(R.layout.device_default_image_item, null);
                holder = new ViewHolder();
                holder.mImage = (ImageView) arg1.findViewById(R.id.device_default_image);
                arg1.setTag(holder);
            } else {
                holder = (ViewHolder) arg1.getTag();
            }
            holder.mImage.setImageResource(mDeviceImageList.get(arg0));
            return arg1;
        }

        private class ViewHolder {
            private ImageView mImage;
        }
    }

    private void setActivityResult(int result) {
        Intent intent = new Intent();
        intent.putExtra("which", result);
        setResult(RESULT_OK, intent);
        finish();
    }
}
