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

package com.mediatek.rcs.contacts.profileapp;

import android.app.Fragment;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.mediatek.rcs.contacts.R;

import java.util.ArrayList;

public class ProfileEntryFragment extends Fragment implements OnItemClickListener {
    public ProfileEntryListener mListener;
    public ArrayList<ListItem> mListItems = new ArrayList<ListItem>();
    private ProfileInfo mProfile = null;
    private ProfileManager mProfileMgr;
    private ListView mListView = null;
    private ProfileEntryListAdapter mAdapter;

    private static final int VIEW_TYPE_PERSONAL_TYPE = 0;
    private static final int VIEW_TYPE_PLUGIN_TYPE = 1;
    private static final int VIEW_ITEM_TYPE_COUNT = 2;

    private static final String PKG_NAME_PLUGIN_CENTER = "com.cmri.rcs.plugincenter";
    private static final String PKG_NAME_CIRCLES = "com.feinno.circles";
    private static final String PKG_NAME_NET_HALL = "cn.com.onlinebusiness";
    private static final String PKG_NAME_RICH_SCREEN = "com.cmdm.rcs";
    private String[] mPluginPkgNameSet = {PKG_NAME_PLUGIN_CENTER, PKG_NAME_CIRCLES,
            PKG_NAME_NET_HALL, PKG_NAME_RICH_SCREEN};
    private int[] mPluginTitleSet = {R.string.plugin_center, R.string.circles,
            R.string.nethall, R.string.rich_screen};
    private int[] mPluginIconSet = {R.drawable.plugin_center, R.drawable.circles,
            R.drawable.nethall, R.drawable.rich_screen};
    private int[] mPluginErrorSet = {
            R.string.please_download_it_android_market, 
            R.string.please_download_it_plugin_center, 
            R.string.please_download_it_plugin_center,
            R.string.please_download_it_plugin_center};
    private static final String TAG = "ProfileEntryFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView:");
        View contentView =  inflater.inflate(R.layout.fragment_profile_entry, null);
        mListView = (ListView)contentView.findViewById(android.R.id.list);
        mAdapter = new ProfileEntryListAdapter();
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        
        return contentView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {   
        mProfileMgr = ProfileManager.getInstance(getActivity().getApplicationContext());
        mProfile = mProfileMgr.getMyProfileFromLocal();
        super.onCreate(savedInstanceState);
    }

    /**
     * @param profile.
     */
    public void updateProfileInfo(ProfileInfo profile) {
    
        Log.d(TAG, "updateProfileInfo:");
        mProfile = profile;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                prepareListViewData();
                if (mAdapter!= null) {
                    mAdapter.notifyDataSetChanged();
                }
            }
        });

    }

    private void prepareListViewData() {
        
        Log.d(TAG, "prepareListViewData:");
        Bitmap icon = null;
        String name = "";
        String number = ProfileInfo.getContentByKey(ProfileInfo.PHONE_NUMBER);
        if (mProfile != null &&  mProfile.photo != null) {
            byte[] photo = mProfile.photo;
            if (photo != null) {
                if (ProfilePhotoUtils.isGifFormatStream(photo)) {
                    icon = ProfilePhotoUtils.getGifFrameBitmap(photo, 0);
                } else {
                    icon = BitmapFactory.decodeByteArray(photo, 0, photo.length);
                }
            }

        }
        if (mProfile.getName()!= null) {
            name = mProfile.getName();
        }
        mListItems.clear();
        addPluginListItems(mListItems);
        mListItems.add(new PersonalListItem(icon, name, VIEW_TYPE_PERSONAL_TYPE, number));
    }

    private void addPluginListItems(ArrayList<ListItem> list) {

        for (int i = 0; i < mPluginPkgNameSet.length; i++) {
            
            Resources res = getActivity().getResources();
            PluginListItem item = new PluginListItem(
                     res.getDrawable(mPluginIconSet[i]),
                     res.getString(mPluginTitleSet[i]),
                     VIEW_TYPE_PLUGIN_TYPE,
                     mPluginPkgNameSet[i],
                     res.getString(mPluginErrorSet[i]));
            
            list.add(item);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView , View view, int i, long l) {
        String pkgName = null;
        String error = null;
        int type = mListItems.get(i).mViewType;
        if (type == VIEW_TYPE_PLUGIN_TYPE) {
            PluginListItem item = (PluginListItem)(mListItems.get(i));
            pkgName = item.mPkgName;
            error = item.mError;
        }
        mListener.onItemClicked(type, pkgName, error);

    }

    public void registerListener(ProfileEntryListener listener) {
        mListener = listener;
    }

    /* Listener to notify entry fragment status .*/
    interface ProfileEntryListener {
        /* notify list item click .*/
        void onItemClicked(int type, String pkgName, String error);

    }

    /* Entry list adapter. */
    public class ProfileEntryListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mListItems.size();
        }

        @Override
        public java.lang.Object getItem(int position) {
            return mListItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemViewType(int position) {
            return mListItems.get(position).getItemViewType();
        }

        @Override
        public int getViewTypeCount() {
            return VIEW_ITEM_TYPE_COUNT;
        }

        public View getView(int position, View convertView, ViewGroup viewGroup) {

            int type = mListItems.get(position).getItemViewType();
            if (type == VIEW_TYPE_PERSONAL_TYPE) {
                convertView = getPersonalItemView(position, convertView);
            } else if (type == VIEW_TYPE_PLUGIN_TYPE) {
                convertView = getPluginItemView(position, convertView);
            }
            return convertView;


        }

        private View getPersonalItemView(int position, View convertView) {
            ViewHolder vh;
            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity())
                        .inflate(R.layout.profile_entry_personal_item, null);
                ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
                TextView title = (TextView) convertView.findViewById(R.id.name);
                TextView number = (TextView) convertView.findViewById(R.id.number);
                vh = new ViewHolder(icon, title);
                vh.setNumber(number);
                convertView.setTag(vh);
            } else {
                vh = (ViewHolder)convertView.getTag();
            }
            PersonalListItem item = (PersonalListItem) mListItems.get(position);
            Bitmap itemIcon = item.mItemIcon;
            if (itemIcon != null) {
                vh.mIcon.setImageBitmap(itemIcon);
            } else {
                vh.mIcon.setImageResource(R.drawable.ic_contact_picture_holo_light);
            }
            vh.mTitle.setText(item.mItemName);
            String displayNumber = "";
            if (item.mNumber != null && !item.mNumber.equals("")) {
                displayNumber = getString(R.string.subtitle_phone_number) + item.mNumber;
            }
            vh.mNumber.setText(displayNumber);
            return convertView;
        }

        private View getPluginItemView(int position, View convertView) {
            ViewHolder vh;
            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity())
                        .inflate(R.layout.profile_entry_list_item, null);
                ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
                TextView title = (TextView) convertView.findViewById(R.id.title);
                vh = new ViewHolder(icon, title);
                convertView.setTag(vh);
            } else {
                vh = (ViewHolder) convertView.getTag();
            }
            PluginListItem item = (PluginListItem)(mListItems.get(position));
            Drawable itemIcon = item.mDrawable;
            if (itemIcon != null) {
                vh.mIcon.setImageDrawable(itemIcon);
            }
            vh.mTitle.setText(mListItems.get(position).mItemName);
            return convertView;
        }
    }

    /* List item data item */
    public class ListItem {
        Bitmap mItemIcon;
        String mItemName;
        int mViewType;
        public ListItem(Bitmap icon, String title, int viewType) {
            this.mItemIcon = icon;
            this.mItemName = title;
            this.mViewType = viewType;
        }

        public int getItemViewType() {
            return mViewType;
        }
    }

    public class PersonalListItem extends ListItem {
        String mNumber;

        public PersonalListItem(Bitmap icon, String title, int viewType, String number) {
            super(icon, title, viewType);
            mNumber = number;
        }

    }

    public class PluginListItem extends ListItem {
        Intent mIntent;
        Drawable mDrawable;
        String mPkgName;
        String mError;
    
        public PluginListItem(Drawable d, String title, int viewType, String pkgName, String error) {
            super(null, title, viewType);
            mPkgName = pkgName;
            mDrawable = d;
            mError = error;
        }
    }

    /* List item view holder */
    public static class ViewHolder {
        ImageView mIcon;
        TextView mTitle;
        TextView mNumber;
        public ViewHolder(ImageView icon, TextView title) {
            this.mIcon = icon;
            this.mTitle = title;
        }

        public void setNumber(TextView number) {
            mNumber = number;
        }
    }
}
