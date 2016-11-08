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

package com.mediatek.rcs.incallui.ext;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
//import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.HashMap;

public class RCSInCallUIPlugin {
    private static final String TAG = "RCSInCallUIPlugin";
    private HashMap<String, Drawable> mIdDrawables = new HashMap<String, Drawable>();
    private HashMap<String, Drawable> mNumDrawables = new HashMap<String, Drawable>();
    private Activity  mInCallActivity;
    private Context   mContext;
    private Context   mApplicationContext;
    private View      mConfButton;
    private ImageView mContactPhoto;
    private int       mImagePanelIndex;
    private int       mVideoPanelIndex;

    private FrameLayout    mImageLayout;
    private FrameLayout    mVideoLayout;
    private RelativeLayout mPhotoLayout;
    private TextView       mResizingNameView;
    private int            mResizingTextSize;
    private ViewGroup      mHostViewGroup;

    public static final int ID_LAYOUT_IMAGE_PANEL = 5678789;
    public static final int ID_LAYOUT_VIDEO_PANEL = 5678790;

    //For RCS switch
    private boolean mRCSOpen;

    public Activity getInCallActivity() {
        return mInCallActivity;
    }

    public void setInCallActivity(Activity activity) {
        mInCallActivity = activity;
        mApplicationContext = mInCallActivity.getApplicationContext();
    }

    public Context getHostApplicationContext() {
        return mApplicationContext;
    }

    public void setPluginContext(Context cnx) {
        mContext = cnx;
    }

    public Context getPluginContext() {
        return mContext;
    }

    public void setImageResources(ViewGroup group, View conf, ImageView photo,
                                    RelativeLayout layout,
                                    FrameLayout image,
                                    TextView name) {
        mHostViewGroup = group;
        mConfButton = conf;
        mContactPhoto = photo;
        mPhotoLayout = layout;
        mImageLayout = image;
        mResizingNameView = name;
    }

    public void setVideoResources(FrameLayout video) {
        mVideoLayout = video;
    }

    public boolean shouldShowPanel() {
        //Maybe we need further check if using confButton to check if it is
        //a multiple party call.
        //if (mConfButton != null) {
            //if (mConfButton.getVisibility() == View.VISIBLE) {
                //Log.d(TAG, "shouldShowPanel mConfButton is visible~");
                //return false;
            //}
       //}
        if (isRCSEnable()) {
            return true;
        }
        return false;
    }

    public FrameLayout getImagePanelRect() {
        return mImageLayout;
    }

    public FrameLayout getVideoPanelRect() {
        return mVideoLayout;
    }

    public ViewGroup  getHostViewGroup() {
        return mHostViewGroup;
    }

    public View findViewById(int index) {
        View view = (View) mInCallActivity.findViewById(index);
        return view;
    }

    private ImageView getContactPhotoView() {
        return mContactPhoto;
    }

    //We use this api will not get contact photo correct, maybe phaseout this api???
    public Drawable getContactPhotoResource() {
        if (mContactPhoto != null) {
            return mContactPhoto.getDrawable();
        }
        return null;
    }

    public RelativeLayout getContactPhotoRect() {
        return mPhotoLayout;
    }

    public void setRCSStatus(boolean isOpen) {
        mRCSOpen = isOpen;
    }

    public boolean isRCSEnable() {
        return mRCSOpen;
    }

    public boolean checkHostLayout() {
        //Need to check host layout, or else if host layout changed, will get exception in plugin
        //If host layout changed, will not mofify host layout.
        return (getHostViewGroup() != null && getResizingNameView() != null &&
            getContactPhotoView() != null && getContactPhotoRect() != null);
    }

    public void setHostVisibility(int visible) {
        if (mContactPhoto != null) {
            mContactPhoto.setVisibility(visible);
        }
    }

    public void putDrawable(String number, Drawable drawable) {
        //Drawable table, one for id index, one for number index
        mNumDrawables.put(number, drawable);
    }

    public Drawable getDrawable(String number) {
        return mNumDrawables.get(number);
    }

    private void clearDrawable() {
        //Need to clear drawable, or else may get drawable leak
        for (Drawable draw : mNumDrawables.values()) {
            draw.setCallback(null);
        }
        mNumDrawables.clear();
    }

    public int getContactPhotoWidth() {
        if (mContactPhoto != null) {
            return mContactPhoto.getWidth();
        }
        return 0;
    }

    public int getContactPhotoHeight() {
        if (mContactPhoto != null) {
            return mContactPhoto.getHeight();
        }
        return 0;
    }

    public TextView getResizingNameView() {
        if (mResizingNameView != null) {
            return mResizingNameView;
        }
        return null;
    }

    public void releaseResource() {
        clearDrawable();

        //We need to set host reference as null for safe.
        //And this maybe before call ondestroy
        mInCallActivity = null;
        mContext = null;
        //mApplicationContext = null;

        mHostViewGroup = null;
        mConfButton = null;
        mContactPhoto = null;
        mPhotoLayout = null;
        mResizingNameView = null;

        mImageLayout = null;
        mVideoLayout = null;

        //mRCSOpen = false;
    }
}
