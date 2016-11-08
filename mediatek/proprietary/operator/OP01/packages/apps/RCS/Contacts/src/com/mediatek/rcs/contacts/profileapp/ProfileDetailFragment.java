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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.cmcc.ccs.profile.ProfileService;
import com.mediatek.rcs.contacts.R;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;


public class ProfileDetailFragment extends Fragment implements AbsListView.OnItemClickListener {

    private ProfileDetailListener mListener;

    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mListView;

    private ProfileDetailListAdapter mAdapter;

    /**
     * The profileInfo of my own information.
     */
    private ProfileInfo mProfile = null;
    private ProfileManager mProfileMgr;
    /**
     * Define listview item type.
     */
    public static final int VIEW_ITEM_TYPE_COMMON_ENTRY = 0;
    public static final int VIEW_ITEM_TYPE_PHOTO_ENTRY = 1;
    public static final int VIEW_ITEM_TYPE_OTHER_NUMBER_ENTRY = 2;
    public static final int VIEW_ITEM_TYPE_COMMON_DIVIDER = 3;
    public static final int VIEW_ITEM_TYPE_OTHER_NUMBER_DIVIDER = 4;
    public static final int VIEW_ITEM_TYPE_QR_CODE = 5;
    public static final int VIEW_ITEM_TYPE_COUNT = 6;

    /* Start activity result code. */
    public static final int RET_CODE_CHOOSE_PHOTO = 0;
    public static final int RET_CODE_TAKE_PHOTO = 1;
    public static final int RET_CODE_CLIP_PHOTO = 2;

    private static final String TAG = ProfileDetailFragment.class.getName();

    public static int[] mPrimaryInfoTitleSet = new int[] {
            R.string.profile_info_phone_number,
            R.string.profile_info_name,
            R.string.profile_info_email,
            R.string.profile_info_address,
            R.string.profile_info_birthday};

    public static int[] mCompanyInfoTitleSet = new int[] {
            R.string.profile_info_company,
            R.string.profile_info_title,
            R.string.profile_info_company_tel,
            R.string.profile_info_company_addr,
            R.string.profile_info_company_fax};

    public int[] mNumberTypeTitleSet = new int[] {
            R.string.profile_info_home_number,
            R.string.profile_info_work_number,
            R.string.profile_info_fixed_number,
            R.string.profile_info_other_number
    };

    /**
     * List item arraylist.
     */
    ArrayList<ViewEntry> mListItems = new ArrayList<ViewEntry>();
    ArrayList<ProfileInfo.OtherNumberInfo> mOtherNumberList
            = new ArrayList<ProfileInfo.OtherNumberInfo>();
    private Uri mTempOutputUri;  // uri witch original file, not clipped

    /* profile editor photo listener */
    public ProfilePhotoHandler.ProfilePhotoHandleListener mPhotoHandleListener
            = new ProfilePhotoHandler.ProfilePhotoHandleListener() {
        @Override
        public void onChoosePhotoChosen() {
            mTempOutputUri = ProfilePhotoUtils
                    .generateTempPhotoUri(getActivity(), "Media");
            //this mTempOutputUri is not clipped yet
            Intent i = ProfilePhotoUtils.getChoosePhotoIntent(getActivity(), mTempOutputUri);
            startActivityForResult(i, RET_CODE_CHOOSE_PHOTO);
        }

        @Override
        public void onTakePhotoChosen() {
            mTempOutputUri = ProfilePhotoUtils
                    .generateTempPhotoUri(getActivity(), "Camera");
            //this mTempOutputUri is not clipped yet
            Intent i = ProfilePhotoUtils.getTakePhotoIntent(getActivity(), mTempOutputUri);
            startActivityForResult(i, RET_CODE_TAKE_PHOTO);
        }

        @Override
        public void onDeletePhotoChosen() {
            HashMap<String, String> map = new HashMap<String, String>();
            map.put(ProfileInfo.PORTRAIT, "");
            map.put(ProfileInfo.PORTRAIT_TYPE, "");
            ProfileManager.getInstance(ProfileDetailFragment.this.getActivity().getApplicationContext())
                    .updateProfilePortrait(null, map);
        }
    };


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ProfileDetailFragment() {

    }

    public void updateProfileInfo(ProfileInfo profile) {
        mProfile = profile;
        updateListViewEntryData();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mProfileMgr = ProfileManager.getInstance(getActivity().getApplicationContext());
        mProfile = mProfileMgr.getMyProfileFromLocal();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_detail, container, false);
        // Set the adapter
        mListView = (AbsListView) view.findViewById(android.R.id.list);
        mAdapter = new ProfileDetailListAdapter();
        mListView.setAdapter(mAdapter);
        updateListViewEntryData();

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);
        return view;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mListItems != null) {
            ViewEntry item = mListItems.get(position);

            if (item.viewType == VIEW_ITEM_TYPE_PHOTO_ENTRY) {
                int mode = ((PhotoViewEntry) item).photo == null ?
                        ProfilePhotoHandler.PHOTO_ACTION_MODE_NEW
                        :ProfilePhotoHandler.PHOTO_ACTION_MODE_UPDATE;
                ProfilePhotoHandler.createPopupMenu(getActivity(),
                        view, mode, mPhotoHandleListener).show();

            } else if (item.viewType != VIEW_ITEM_TYPE_OTHER_NUMBER_ENTRY
                    && item.dataType != ProfileInfo.PHONE_NUMBER) {
                mListener.onItemClick(position, item.dataType, item.title);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "resultCode=" + resultCode);
        switch (requestCode) {
            case RET_CODE_CHOOSE_PHOTO:
            case RET_CODE_TAKE_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri;
                    if (data != null && data.getData() != null) {
                        Log.d(TAG, "DataUri=" + data.getData().toSafeString());
                        //file uri witch get to be clipped
                        uri = data.getData();

                    } else {
                        uri = mTempOutputUri;
                    }
                    doCropPhoto(uri);
                }
                break;
            case RET_CODE_CLIP_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    //data.getData get the url after clipping
                    if (data != null && data.getData() != null) {
                        Log.d(TAG, "DataUri=" + data.getData().toSafeString());
                        //file uri witch is clipped done
                        doUpdatePhoto(data.getData());
                    }
                }
                break;
        }

    }

    /**
     * Crop the selected Photo, which to be used as profile portait.
     * @param uri    : The photo uri to be croped
     */
    private void doCropPhoto(Uri uri) {
        if (uri != null) {
            try {
                Intent i = ProfilePhotoUtils.getPhotoCropIntent(getActivity(), uri);
                startActivityForResult(i, RET_CODE_CLIP_PHOTO);
            } catch (ActivityNotFoundException e) {
                Log.d(TAG, "Can not crop photo");
            }
        }
    }

    /**
     * Save the cropped Photo as profile portait.
     * @param uri    : The photo uri to be saved
     */
    private void doUpdatePhoto(Uri uri) {
        Log.d(TAG, "doUpdatePhoto, uri" + uri.toString());
        String photoStr = ProfilePhotoUtils.doCompressPhoto(uri, getActivity());

        //we need to workaround here to get the photo file path
        String absFilePath = ProfilePhotoUtils.getPhotoFilePath(uri, getActivity());
        Log.d(TAG, "doUpdatePhoto, absFilePath: " + absFilePath);
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(ProfileInfo.PORTRAIT, photoStr);
        map.put(ProfileInfo.PORTRAIT_TYPE, ProfileService.PNG);
        mProfileMgr.updateProfilePortrait(absFilePath, map);
        //mProfileMgr.updateProfileByType(map);

        //"uri" is the finally uri, so remove the temple original photo
        if (mTempOutputUri != null) {
            Log.d(TAG, "delete tmp uri =" + mTempOutputUri.toSafeString());
            getActivity().getContentResolver().delete(mTempOutputUri, null, null);
            mTempOutputUri = null;
        }

    }

    /**
     * Initialize list items data.
     */
    public void updateListViewEntryData() {

        if (mAdapter!= null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mListItems.clear();
                    mListItems.add(new PhotoViewEntry(mProfile.photo,
                            getString(R.string.profile_info_portrait), null,
                            VIEW_ITEM_TYPE_PHOTO_ENTRY, ProfileInfo.PORTRAIT));
                    mListItems.add(new ViewEntry(null, null,
                            VIEW_ITEM_TYPE_COMMON_DIVIDER, null));

                    addProfilePrimaryInfo();
                    addProfileOtherNumber();
                    addProfileCompanyInfo();
                    mListItems.add(new PhotoViewEntry(mProfile.photo,
                            getString(R.string.profile_info_portrait), null,
                            VIEW_ITEM_TYPE_QR_CODE, ProfileInfo.QR_CODE));
                    mAdapter.notifyDataSetChanged();
                }
            });
        }

    }

    /**
     * Initialize list items data of Primary information.
     * Such as name, number, birthday, email
     */
    private void addProfilePrimaryInfo() {

        for (int i = 0; i < mPrimaryInfoTitleSet.length; i++) {
            int titleResId = mPrimaryInfoTitleSet[i];
            String key = ProfileInfo.mPrimaryInfoKeySet[i];
            String content;
            if (key.equals(ProfileInfo.NAME)) {
                content = mProfile.getName();
            } else {
                content = mProfile.getContentByKey(key);
            }
            ViewEntry viewEntry = new ViewEntry(titleResId, content,
                    VIEW_ITEM_TYPE_COMMON_ENTRY, key);
            mListItems.add(viewEntry);
            mListItems.add(new ViewEntry(null, null,
                    VIEW_ITEM_TYPE_COMMON_DIVIDER, null));
        }

    }

    /**
     * Initialize list items data of Company information.
     * Such as company name, title, company fax, compay address
     */
    private void addProfileCompanyInfo() {
        for (int i = 0; i < mCompanyInfoTitleSet.length; i++) {
            int titleResId = mCompanyInfoTitleSet[i];
            String key = ProfileInfo.mCompanyInfoKeySet[i];
            String content = mProfile.getContentByKey(key);
            ViewEntry viewEntry = new ViewEntry(titleResId, content,
                    VIEW_ITEM_TYPE_COMMON_ENTRY, key);
            mListItems.add(viewEntry);
            mListItems.add(new ViewEntry(null, null,
                    VIEW_ITEM_TYPE_COMMON_DIVIDER, null));
        }
    }

    /**
     * Initialize list items data of other numbers.
     * Can add max 6 other numbers. accepted type is work, home, fix, other.
     */
    private void addProfileOtherNumber() {
        ViewEntry viewEntry = new ViewEntry(R.string.profile_info_other_number, "",
                VIEW_ITEM_TYPE_COMMON_ENTRY, ProfileInfo.PHONE_NUMBER_SECOND);
        mListItems.add(viewEntry);
        mListItems.add(new ViewEntry(null, null,
                VIEW_ITEM_TYPE_COMMON_DIVIDER, null));

        //mOtherNumberList = mProfile.mOtherNumberArrayList;
        mOtherNumberList = mProfile.getAllOtherNumber();
        int count = 0;
        for (ProfileInfo.OtherNumberInfo info : mOtherNumberList) {
            int titleRes = mNumberTypeTitleSet[info.type];
            mListItems.add(new ViewEntry(titleRes, info.number,
                    VIEW_ITEM_TYPE_OTHER_NUMBER_ENTRY, ProfileInfo.PHONE_NUMBER_SECOND));
            if (count == mOtherNumberList.size() - 1) {
                mListItems.add(new ViewEntry(null, null,
                        VIEW_ITEM_TYPE_COMMON_DIVIDER, null));
            } else {
                mListItems.add(new ViewEntry(null, null,
                        VIEW_ITEM_TYPE_OTHER_NUMBER_DIVIDER, null));
            }
            count++;
        }
    }

    /**
     * @param listener.
     */
    public void registerListener(ProfileDetailListener listener) {
        mListener = listener;
    }

    /**
     * ProfileDetailListener.
     */
    public interface ProfileDetailListener {
        public void onItemClick(int position, String dataType, String title);
    }

    /* Entry list adapter */
    public class ProfileDetailListAdapter extends BaseAdapter {
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

        /**
         * @param position    :listView position
         * @param convertView :convert view
         * @param parent      :viewGroup
         * @return Current view
         */
        public View getView(int position, View convertView, ViewGroup parent) {
            int type = getItemViewType(position);
            if (type == VIEW_ITEM_TYPE_COMMON_ENTRY) {
                return getCommonTypeView(position, convertView, parent);
            } else if (type == VIEW_ITEM_TYPE_PHOTO_ENTRY) {
                return getPhotoTypeView(position, convertView, parent);
            } else if (type == VIEW_ITEM_TYPE_OTHER_NUMBER_ENTRY) {
                return getOtherNumberTypeView(position, convertView, parent);
            } else if (type == VIEW_ITEM_TYPE_COMMON_DIVIDER) {
                return getCommonDividerTypeView(position, convertView, parent);
            } else if (type == VIEW_ITEM_TYPE_OTHER_NUMBER_DIVIDER) {
                return getOtherNumberDividerTypeView(position, convertView, parent);
            } else if (type == VIEW_ITEM_TYPE_QR_CODE) {
                return getQRCodeView(position, convertView, parent);
            } else {
                throw new IllegalStateException("Invalid view type" + type);
            }
        }

        /**
         * Item type. Common list display
         * @param position    :listView position
         * @param convertView :convert view
         * @return Current view
         */
        private View getCommonTypeView(int position, View convertView, ViewGroup parent) {
            CommonViewCache viewCache;
            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity())
                        .inflate(R.layout.profile_detail_common_item, null, false);
                TextView content = (TextView) convertView.findViewById(R.id.content);
                TextView title = (TextView) convertView.findViewById(R.id.title);
                ImageView next = (ImageView)convertView.findViewById(R.id.next);
                viewCache = new CommonViewCache(title, content, next);
                convertView.setTag(viewCache);
            } else {
                viewCache = (CommonViewCache) convertView.getTag();
            }
            if (mListItems.get(position).dataType == ProfileInfo.PHONE_NUMBER) {
                viewCache.next.setVisibility(View.INVISIBLE);
            } else {
                viewCache.next.setVisibility(View.VISIBLE);
            }
            viewCache.content.setText(mListItems.get(position).content);
            viewCache.title.setText(mListItems.get(position).title);
            return convertView;
        }

        /**
         * Item type. Photo list display
         * @param position     : listView position
         * @param convertView: convert view
         * @return Current view
         */
        private View getPhotoTypeView(int position, View convertView, ViewGroup parent) {
            PhotoViewCache viewCache;
            View result;
            if (convertView == null) {
                result = LayoutInflater.from(getActivity())
                        .inflate(R.layout.profile_detail_photo_item, null, false);
                ImageView photo = (ImageView) result.findViewById(R.id.photo);
                TextView title = (TextView) result.findViewById(R.id.title);
                viewCache = new PhotoViewCache(photo, title);
                result.setTag(viewCache);
            } else {
                result = convertView;
                viewCache = (PhotoViewCache) result.getTag();
            }
            PhotoViewEntry viewEntry = (PhotoViewEntry) mListItems.get(position);
            if (viewEntry.photo == null) {
                viewCache.photo.setImageResource(R.drawable.ic_contact_picture_holo_light);
            } else {
                Bitmap map;
                if (ProfilePhotoUtils.isGifFormatStream(viewEntry.photo)) {
                    map = ProfilePhotoUtils.getGifFrameBitmap(viewEntry.photo, 0);
                } else {
                    map = BitmapFactory
                        .decodeByteArray(viewEntry.photo, 0, viewEntry.photo.length);
                }
                viewCache.photo.setImageBitmap(map);
            }
            viewCache.title.setText(mListItems.get(position).title);
            return result;
        }

        /**
         * Item type. Other number display
         * @param position     : listView position
         * @param convertView: convert view
         * @return Current view
         */
        private View getOtherNumberTypeView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity())
                        .inflate(R.layout.profile_detail_other_number_list_item, null);
            }

            TextView title = (TextView)convertView.findViewById(R.id.title);
            title.setText(mListItems.get(position).title);
            TextView content = (TextView)convertView.findViewById(R.id.content);
            content.setText(mListItems.get(position).content);
            return convertView;
        }

        /**
         * Item type. Common Divider
         * @param position     : listView position
         * @param convertView: convert view
         * @return Current view
         */
        private View getCommonDividerTypeView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity())
                        .inflate(R.layout.profile_detail_divider, parent, false);
            }
            return convertView;
        }

        /**
         * Item type. Other Number Divider
         * @param position     : listView position
         * @param convertView: convert view
         * @return Current view
         */
        private View getOtherNumberDividerTypeView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity())
                        .inflate(R.layout.other_number_divider, parent, false);
            }
            return convertView;
        }

       /**
         * Item type. QR code view
         * @param position     : listView position
         * @param convertView: convert view
         * @return Current view
         */
        private View getQRCodeView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity())
                        .inflate(R.layout.profile_detail_qr_code_item, parent, false);
            }
            return convertView;
        }

        private class ViewCache {
            int resId;
            TextView title;
            ImageView next;
            public ViewCache(TextView title) {
                this.title = title;
            }

        }

        private class PhotoViewCache extends ViewCache {
            ImageView photo;
            public PhotoViewCache(ImageView photo, TextView title) {
                super(title);
                this.photo = photo;
                resId = R.layout.profile_detail_photo_item;
            }
        }

        private class CommonViewCache extends ViewCache {
            TextView content;
            public CommonViewCache(TextView title, TextView content, ImageView next) {
                super(title);
                this.content = content;
                this.next = next;
                resId = R.layout.profile_detail_common_item;

            }
        }

    }

    public class ViewEntry {
        String dataType;
        String title;
        String content;
        int viewType;

        public ViewEntry() {

        }

        public ViewEntry(String title, String content, int viewType, String dataType) {
            this.title = title;
            this.content = content;
            this.viewType = viewType;
            this.dataType = dataType;
        }

        public ViewEntry(int titleResId, String content, int viewType, String dataType) {
            this.title = getString(titleResId);
            this.content = content;
            this.viewType = viewType;
            this.dataType = dataType;
        }

        public int getItemViewType() {
            return viewType;
        }

        public void setViewType(int type) {
            viewType = type;
        }
    }

    public class PhotoViewEntry extends ViewEntry {
        byte[] photo = null;

        public PhotoViewEntry(byte[] photo, String title, String content, int viewType, String dataType) {
            super(title, content, viewType, dataType);
            this.photo = photo;
        }

        @Override
        public int getItemViewType() {
            return viewType;
        }
    }
}
