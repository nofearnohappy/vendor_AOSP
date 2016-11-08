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

import com.mediatek.rcs.contacts.list.GroupChatLoader;
import com.mediatek.rcs.contacts.R;

import java.util.List;

public class GroupChatAdapter extends BaseAdapter {

    private static final String TAG = GroupChatAdapter.class.getSimpleName();
    
    private final Context mContext;
    private final LayoutInflater mLayoutInflater;
    private Cursor mCursor;

    public GroupChatAdapter(Context context) {
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
    public GroupChatItem getItem(int position) {
        if (mCursor == null || mCursor.isClosed() || !mCursor.moveToPosition(position)) {
            Log.e(TAG, "mCursor: " + mCursor + ", position: " + position);
            return null;
        }
     
        String id = mCursor.getString(GroupChatLoader.GROUP_CHAT_ID);
        String title = mCursor.getString(GroupChatLoader.GROUP_CHAT_TITLE);
        String nickName = mCursor.getString(GroupChatLoader.GROUP_CHAT_NICKNAME);
        
        return new GroupChatItem(id, -1, title, nickName);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        GroupChatItem entry = getItem(position);
        View result;
        GroupListItemViewCache viewCache;
        if (convertView != null) {
            result = convertView;
            viewCache = (GroupListItemViewCache) result.getTag();
        } else {
            result = mLayoutInflater.inflate(R.layout.rcs_group_chat_item, parent, false);           
            viewCache = new GroupListItemViewCache(result);
            result.setTag(viewCache);
        }

        if (position == 0) {
            viewCache.divider.setVisibility(View.GONE);
        } else {
            viewCache.divider.setVisibility(View.VISIBLE);
        }
    
        viewCache.groupTitle.setText(getTitle(entry));
        
        return result;
    }

    private String getTitle(GroupChatItem entry) {
        String title = null;

        if (entry.existNickName()) {
            title = entry.getNickName();    
        } else {
            title = entry.getSubject();
        }
        
        Log.d(TAG, "getTitle: " + title);
        return title;
    }

    public static class GroupListItemViewCache {
        public final TextView groupTitle;
        public final View divider;
        
        public GroupListItemViewCache(View view) {
            groupTitle = (TextView) view.findViewById(R.id.label);
            divider = view.findViewById(R.id.divider);
        }
    }
}
