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
 * Copyright (C) 2008 Esmertec AG.
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
package com.mediatek.mms.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.R;
import com.android.mms.util.FeatureOption;
import com.mediatek.mms.folder.util.FolderModeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** M:
 * CMCC Only
 * SubinfoSelectedActivity
 */
public class SubinfoSelectedActivity extends Activity {
    private static final String TAG = "SubinfoSelectedActivity";
    private String where;
    private List<SubscriptionInfo> mSubInfoList;
    private int mSubCount = 0;
    private static final String VIEW_ITEM_KEY_IMAGE     = "simCardPicid";
    private static final String VIEW_ITEM_KEY_SIMNAME   = "simCardTexid";
    private static final String VIEW_ITEM_KEY_SELECT    = "selectedid";
    private static final String VIEW_ITEM_KEY_COLOR     = "color";
    private static final String VIEW_ITEM_KEY           = "simcardkey";
    private ListView listview;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.siminfo_seleted_framelayout);
        listview = (ListView)findViewById(R.id.siminfolist);
        LayoutInflater inflater = getLayoutInflater();
        View header = inflater.inflate(R.layout.select_siminfo_header, null);
        listview.addHeaderView(header, null, false);
        getSubInfoList();
        initListAdapter();

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                String where = null;
                int mindex = 0;
                Map<String, Object> map
                        = (Map<String, Object>) listview.getItemAtPosition(position);
                List<SubscriptionInfo> subinfoList = SubscriptionManager.from(
                        MmsApp.getApplication()).getActiveSubscriptionInfoList();
                if (subinfoList == null || map.get(VIEW_ITEM_KEY).equals("0")) {
                    where = null;
                    mindex = 0;
                } else if (map.get(VIEW_ITEM_KEY).equals("1")) {
                    SubscriptionInfo si = subinfoList.get(0);
                    if (si == null) {
                        finish();
                    } else {
                        where = "sub_id = " + (int) si.getSubscriptionId();
                        mindex = 1;
                    }
                } else {
                    SubscriptionInfo mSimInfo = subinfoList.get(1);
                    if (mSimInfo == null) {
                        finish();
                    } else {
                        where = "sub_id = " + (int) mSimInfo.getSubscriptionId();
                        mindex = 2;
                    }
                }
                FolderModeUtils.setSimCardInfo(mindex);
                Intent mIntent = new Intent();
                mIntent.putExtra("sub_id", where);
                setResult(RESULT_OK, mIntent);
                finish();
            }
        });
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                   finish();
                return true;
            case KeyEvent.KEYCODE_SEARCH:
                return true;
            case KeyEvent.KEYCODE_MENU:
                return true;
            case KeyEvent.KEYCODE_HOME:
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void initListAdapter() {
        SimpleAdapter mSimAdapter = new SubinfoAdapter(this, getSimData(),
            R.layout.select_simcard_dialog_view,
            new String[] {"simCardPicid", "simCardTexid","selectedid"},
            new int[] { R.id.simCardPicid, R.id.simCardTexid, R.id.selectedid});
        listview.setAdapter(mSimAdapter);
    }

    private void getSubInfoList() {
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            mSubInfoList = SubscriptionManager.from(MmsApp.getApplication())
                    .getActiveSubscriptionInfoList();
            mSubCount = mSubInfoList == null? 0: mSubInfoList.size();
        }
    }

    private List<Map<String, Object>> getSimData() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Map<String, Object> map = new HashMap<String, Object>();
        Resources res = getResources();
        int msimindex = FolderModeUtils.getSimCardInfo();
        if (msimindex > mSubCount) {
            msimindex = msimindex-1;
        }
        map.put(VIEW_ITEM_KEY_SIMNAME, res.getText(R.string.allmessage));
        if (msimindex == 0) {
            map.put(VIEW_ITEM_KEY_SELECT, true);
        } else {
            map.put(VIEW_ITEM_KEY_SELECT, false);
        }
        map.put(VIEW_ITEM_KEY, "0");

        list.add(map);
        if (mSubCount == 2) {
            map = new HashMap<String, Object>();
            if (mSubInfoList.get(0).getSimSlotIndex() == 0) {
                // MR1 need fix
                 map.put(VIEW_ITEM_KEY_IMAGE,
                 mSubInfoList.get(0).createIconBitmap(SubinfoSelectedActivity.this));
                map.put(VIEW_ITEM_KEY_SIMNAME, mSubInfoList.get(0).getDisplayName().toString());
                map.put(VIEW_ITEM_KEY_COLOR, mSubInfoList.get(0).getIconTint());
            } else {
                 map.put(VIEW_ITEM_KEY_IMAGE,
                 mSubInfoList.get(1).createIconBitmap(SubinfoSelectedActivity.this));
                map.put(VIEW_ITEM_KEY_SIMNAME, mSubInfoList.get(1).getDisplayName().toString());
                map.put(VIEW_ITEM_KEY_COLOR, mSubInfoList.get(1).getIconTint());
            }
            if (msimindex == 1) {
                map.put(VIEW_ITEM_KEY_SELECT, true);
            } else {
                map.put(VIEW_ITEM_KEY_SELECT, false);
            }
            map.put(VIEW_ITEM_KEY, "1");
            list.add(map);

            map = new HashMap<String, Object>();
            if (mSubInfoList.get(0).getSimSlotIndex() == 1) {
                 map.put(VIEW_ITEM_KEY_IMAGE,
                 mSubInfoList.get(0).createIconBitmap(SubinfoSelectedActivity.this));
                map.put(VIEW_ITEM_KEY_SIMNAME, mSubInfoList.get(0).getDisplayName().toString());
                map.put(VIEW_ITEM_KEY_COLOR, mSubInfoList.get(0).getIconTint());
            } else {
                 map.put(VIEW_ITEM_KEY_IMAGE,
                 mSubInfoList.get(1).createIconBitmap(SubinfoSelectedActivity.this));
                map.put(VIEW_ITEM_KEY_SIMNAME, mSubInfoList.get(1).getDisplayName().toString());
                map.put(VIEW_ITEM_KEY_COLOR, mSubInfoList.get(1).getIconTint());
            }
            if (msimindex == 2) {
                map.put(VIEW_ITEM_KEY_SELECT, true);
            } else {
                map.put(VIEW_ITEM_KEY_SELECT, false);
            }
            map.put(VIEW_ITEM_KEY, "2");
            list.add(map);
        } else if (mSubCount == 1) {
            int slotId = 0;
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                map = new HashMap<String, Object>();
                 map.put(VIEW_ITEM_KEY_IMAGE,
                 mSubInfoList.get(0).createIconBitmap(SubinfoSelectedActivity.this));
                map.put(VIEW_ITEM_KEY_SIMNAME, mSubInfoList.get(0).getDisplayName().toString());
                map.put(VIEW_ITEM_KEY_COLOR, mSubInfoList.get(0).getIconTint());
                if (msimindex == 1) {
                    map.put(VIEW_ITEM_KEY_SELECT, true);
                } else {
                    map.put(VIEW_ITEM_KEY_SELECT, false);
                }
                map.put(VIEW_ITEM_KEY, "1");
                list.add(map);
            }
        }
        return list;
    }
    class SubinfoAdapter extends SimpleAdapter {
        List<? extends Map<String, ?>> mData;
        public SubinfoAdapter(Context context,
                List<? extends Map<String, ?>> data, int resource,
                String[] from, int[] to) {
            super(context, data, resource, from, to);
            mData = data;
            // TODO Auto-generated constructor stub
        }

        public View getView(int position, View convertView, ViewGroup parent){
            View view = super.getView(position, convertView, parent);
            Integer color = (Integer)mData.get(position).get(VIEW_ITEM_KEY_COLOR);
            TextView text = (TextView)view.findViewById(R.id.simCardTexid);
            if(text != null) {
                Log.d(TAG, "getView: color = " + color);
                if (color != null) {
                    text.setTextColor(color);
                } else {
                    text.setTextColor(Color.BLACK);
                }
            }
            Bitmap bitmap = (Bitmap) mData.get(position).get(VIEW_ITEM_KEY_IMAGE);
            ImageView image = (ImageView)view.findViewById(R.id.simCardPicid);
            if (image != null) {
                if (bitmap != null) {
                    image.setImageBitmap(bitmap);
                    image.setVisibility(View.VISIBLE);
                } else {
                    image.setVisibility(View.GONE);
                }
            }
            return view;
        }
    }
}
