/*
* This software/firmware and related documentation ("MediaTek Software") are
* protected under relevant copyright laws. The information contained herein
* is confidential and proprietary to MediaTek Inc. and/or its licensors.
* Without the prior written permission of MediaTek inc. and/or its licensors,
* any reproduction, modification, use or disclosure of MediaTek Software,
* and information contained herein, in whole or in part, shall be strictly prohibited.
*/
/* MediaTek Inc. (C) 2014. All rights reserved.
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

package com.mediatek.rcs.contacts.list;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.mediatek.rcs.contacts.list.GroupListLoader;
import com.mediatek.rcs.contacts.util.SubInfoUtils;
import com.mediatek.rcs.contacts.R;

import com.google.common.base.Objects;

import java.util.List;

public class GroupListAdapter extends BaseAdapter {

    private static final String TAG = GroupListAdapter.class.getSimpleName();

    public static final String ACCOUNT_TYPE_USIM = "USIM Account";
    public static final String ACCOUNT_TYPE_LOCAL_PHONE = "Local Phone Account";
    public static final String USIM = "USIM";    
    
    private final Context mContext;
    private final LayoutInflater mLayoutInflater;
    private Cursor mCursor;

    public GroupListAdapter(Context context) {
        mContext = context;
        mLayoutInflater = LayoutInflater.from(context);
    }

    public void setCursor(Cursor cursor) {
        mCursor = cursor;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return (mCursor == null || mCursor.isClosed()) ? 0 : mCursor.getCount();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public GroupListItem getItem(int position) {
        if (mCursor == null || mCursor.isClosed() || !mCursor.moveToPosition(position)) {
            Log.e(TAG, "mCursor: " + mCursor + ", position: " + position);
            return null;
        }
        String accountName = mCursor.getString(GroupListLoader.GROUP_LIST_ACCOUNT_NAME);
        String accountType = mCursor.getString(GroupListLoader.GROUP_LIST_ACCOUNT_TYPE);       
        long groupId = mCursor.getLong(GroupListLoader.GROUP_LIST_ID);
        String title = mCursor.getString(GroupListLoader.GROUP_LIST_TITLE);
        String dataSet = mCursor.getString(GroupListLoader.GROUP_LIST_DATA_SET);
        int memberCount = mCursor.getInt(GroupListLoader.GROUP_LIST_MEMBER_COUNT);

        // Figure out if this is the first group for this account name / account type pair by
        // checking the previous entry. This is to determine whether or not we need to display an
        // account header in this item.
        int previousIndex = position - 1;
        boolean isFirstGroupInAccount = true;
        if (previousIndex >= 0 && mCursor.moveToPosition(previousIndex)) {
            String preAccountName = mCursor.getString(GroupListLoader.GROUP_LIST_ACCOUNT_NAME);
            String preAccountType = mCursor.getString(GroupListLoader.GROUP_LIST_ACCOUNT_TYPE);
            String preDataSet = mCursor.getString(GroupListLoader.GROUP_LIST_DATA_SET);

            if (accountName.equals(preAccountName) && accountType.equals(preAccountType) 
                    && Objects.equal(dataSet, preDataSet)) {
                isFirstGroupInAccount = false;
            }
        }

        return new GroupListItem(accountName, accountType, dataSet, groupId, title,
                isFirstGroupInAccount, memberCount);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        GroupListItem entry = getItem(position);
        View result;
        GroupListItemViewCache viewCache;
        if (convertView != null) {
            result = convertView;
            viewCache = (GroupListItemViewCache) result.getTag();
        } else {
            result = mLayoutInflater.inflate(R.layout.rcs_group_list_item, parent, false);           
            viewCache = new GroupListItemViewCache(result);
            result.setTag(viewCache);
        }

        //Add a header if this is the first group in an account and hide the divider
        if(entry.isFirstGroupInAccount()) {
            bindHeaderView(entry, viewCache);
            viewCache.accountHeader.setVisibility(View.VISIBLE);
            viewCache.divider.setVisibility(View.GONE);
            if (position == 0) {
                viewCache.accountHeaderExtraTopPadding.setVisibility(View.VISIBLE);
            } else {
                viewCache.accountHeaderExtraTopPadding.setVisibility(View.GONE);
            }
        } else {
            viewCache.accountHeader.setVisibility(View.GONE);
            viewCache.divider.setVisibility(View.VISIBLE);
            viewCache.accountHeaderExtraTopPadding.setVisibility(View.GONE);
        }

        String memberCountString = mContext.getResources().getQuantityString(
                R.plurals.group_list_num_contacts_in_group, entry.getMemberCount(),
                entry.getMemberCount());
        viewCache.groupTitle.setText(entry.getTitle());
        viewCache.groupMemberCount.setText(memberCountString);
        
        return result;
    }

    private void bindHeaderView(GroupListItem entry, GroupListItemViewCache viewCache) {
        viewCache.accountType.setText(getAccountType(entry.getAccountType()));
        
        if (ACCOUNT_TYPE_LOCAL_PHONE.equals(entry.getAccountType())) {
            viewCache.accountName.setVisibility(View.GONE);
        } else {
            viewCache.accountName.setVisibility(View.VISIBLE);
            viewCache.accountName.setText(getAccountName(entry.getAccountName()));
        }
    }

    private CharSequence getAccountType(String type) {
        CharSequence result;
        
        if (ACCOUNT_TYPE_LOCAL_PHONE.equals(type)) {
            result = mContext.getResources().getText(R.string.account_phone_only);    
        } else if (ACCOUNT_TYPE_USIM.equals(type)) {
            result = mContext.getResources().getText(R.string.account_usim_only);
        } else {
            Log.e(TAG, "getAccountType not found: " + type);
            result = type;
        }

        return result;
    }

    private String getAccountName(String name) {
        String displayName = null;
        int simId;
        Log.e(TAG, "getAccountName name: " + name);
        
        if (name.indexOf(USIM) != -1) {
            int startIndex = USIM.length();
            simId = Integer.valueOf(name.substring(startIndex)); 
            Log.e(TAG, "getAccountName simId: " + simId);
            displayName = SubInfoUtils.getDisplaynameUsingSubId(simId, mContext);
            Log.e(TAG, "getAccountName displayName: " + displayName);
        } else {
            Log.e(TAG, "getAccountName not found: " + name);
            displayName = name;
        }

        return displayName;
    }

    public static class GroupListItemViewCache {
        public final TextView accountType;
        public final TextView accountName;
        public final TextView groupTitle;
        public final TextView groupMemberCount;
        public final View accountHeader;
        public final View accountHeaderExtraTopPadding;
        public final View divider;

        public GroupListItemViewCache(View view) {
            accountType = (TextView) view.findViewById(R.id.account_type);            
            accountName = (TextView) view.findViewById(R.id.account_name);
            groupTitle = (TextView) view.findViewById(R.id.label);
            groupMemberCount = (TextView) view.findViewById(R.id.count);
            accountHeader = view.findViewById(R.id.group_list_header);
            accountHeaderExtraTopPadding = view.findViewById(R.id.header_extra_top_padding);
            divider = view.findViewById(R.id.divider);
        }
    }
}
