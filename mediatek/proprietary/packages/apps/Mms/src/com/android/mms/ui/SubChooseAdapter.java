/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.mms.ui;


import java.util.List;
import android.util.Log;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.graphics.Color;
import com.mediatek.widget.AccountItemView;
import com.mediatek.internal.telephony.uicc.SvlteUiccUtils;
import com.mediatek.telephony.TelephonyManagerEx;

public class SubChooseAdapter extends BaseAdapter {

    Context mContext;
    List<SubscriptionInfo> mList;

    public SubChooseAdapter(Context context, List<SubscriptionInfo> list) {
        mContext = context;
        mList = list;
    }
    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean isEnabled(int position) {
        if (isCTOMFeature2Work(position) || isCTOMFeature15Work(position)) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        AccountItemView accountView;
        if (convertView == null) {
            accountView = new AccountItemView(mContext);
        } else {
            accountView = (AccountItemView) convertView;
        }
        SubscriptionInfo subRecord = mList.get(position);
        // Set theme of the item is LIGHT
        // accountView.setThemeType(SubscriptionView.LIGHT_THEME);
        if (subRecord.getSimSlotIndex() == SubscriptionManager.SIM_NOT_INSERTED) {
            accountView.setAccountName(subRecord.getDisplayName().toString());
            accountView.setAccountNumber(null);
            accountView.findViewById(com.android.internal.R.id.icon).setVisibility(View.GONE);
            accountView.setClickable(true);
        } else {
            accountView.setClickable(false);
            if (isCTOMFeature2Work(position) || isCTOMFeature15Work(position)) {
                accountView.setAccountIcon(new BitmapDrawable(mContext.getResources(), subRecord
                        .createIconBitmap(mContext, Color.GRAY)));
            } else {
                accountView.setAccountIcon(new BitmapDrawable(mContext.getResources(), subRecord
                        .createIconBitmap(mContext)));
            }
            accountView.setAccountName(subRecord.getDisplayName().toString());
            accountView.setAccountNumber(subRecord.getNumber());
            accountView.findViewById(com.android.internal.R.id.icon).setVisibility(View.VISIBLE);
        }
        if (isCTOMFeature2Work(position) || isCTOMFeature15Work(position)) {
            accountView.setAlpha(0.3f);
        } else {
            accountView.setAlpha(1.0f);
        }
        return accountView;
    }

    public void setAdapterData(List<SubscriptionInfo> list) {
        mList = list;
    }


    private boolean isCTOMFeature2Work(int position) {
        int defaultDataSubId = SubscriptionManager.getDefaultDataSubId();
        int defaultDataSlotId = SubscriptionManager.getSlotId(defaultDataSubId);
        Log.d("SubChooseAdapter", "defaultDataSubId = " + defaultDataSubId);
        if (SvlteUiccUtils.getInstance().getSimType(defaultDataSlotId) != SvlteUiccUtils.SIM_TYPE_CDMA
                || !TelephonyManagerEx.getDefault().isInHomeNetwork(defaultDataSubId)) {
            return false;
        }
        if (mList != null) {
            int subId = mList.get(position).getSubscriptionId();
            int slotId = SubscriptionManager.getSlotId(subId);
            Log.d("SubChooseAdapter", "subId = " + subId);
            if (subId != defaultDataSubId
                    && SvlteUiccUtils.getInstance().getSimType(slotId) == SvlteUiccUtils.SIM_TYPE_CDMA
                    && TelephonyManagerEx.getDefault().isInHomeNetwork(subId)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCTOMFeature15Work(int position) {
        if (mList != null && !MessageUtils.isC2KSolution2Support()) {
            int currentSubId = mList.get(position).getSubscriptionId();
            int currentSlotId = SubscriptionManager.getSlotId(currentSubId);
            int currentSlotType = SvlteUiccUtils.getInstance().getSimType(currentSlotId);
            int mainCapabilitySlotId = MessageUtils.getMainCapabilitySlotId();
            int mainCapabilitySlotType = SvlteUiccUtils.getInstance().getSimType(mainCapabilitySlotId);
            Log.d("SubChooseAdapter", "subId = " + currentSubId + " currentSlotType = " + currentSlotType);
            if (mainCapabilitySlotType == SvlteUiccUtils.SIM_TYPE_GSM
                    && currentSlotType == SvlteUiccUtils.SIM_TYPE_CDMA
                    && TelephonyManagerEx.getDefault().isInHomeNetwork(currentSubId)) {
                return true;
            }
        }
        return false;
    }
}
