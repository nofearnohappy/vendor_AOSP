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
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.telecom.Call.Details;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.incallui.CallList;
import com.android.incallui.ContactInfoCache;
import com.mediatek.common.PluginImpl;
import com.mediatek.incallui.ext.DefaultRCSeCallCardExt;
import com.mediatek.incallui.ext.IRCSeCallCardExt;
import com.mediatek.rcs.incallui.RichCallController;
import com.mediatek.rcs.incallui.utils.RichCallInsController;

import java.util.HashMap;

@PluginImpl(interfaceName="com.mediatek.incallui.ext.IRCSeCallCardExt")
public class RCSCallCardExtension extends DefaultRCSeCallCardExt {

    private static final String TAG = "RCSCallCardExtension";

    private Context mHostContext;
    private String mHostPackage;
    private Resources mHostResources;

    private static int ID_LAYOUT_IMAGE_PANEL = 5678789;
    private static int ID_LAYOUT_VIDEO_PANEL = 5678790;

    private Context mContext;
    //private HashMap<Integer, TextView> map = new HashMap<Integer, TextView>();
    private RichCallController mRichCallController;
    private RCSInCallUIPlugin mRCSInCallUIPlugin;

    public RCSCallCardExtension(Context context) {
        super();
        Log.d(TAG, "RCSCallCardExtension");
        mContext = context;
    }

    /**
      * Interface to get name and status textview, inorder to get the string and notify to msg
      *
      * @param context the incallactivity context
      * @param view the callcard view
      */
    @Override
    public void onViewCreated(Context context, View view) {
        Log.d(TAG, "onViewCreated.");
        refrehInstance(true);
        mHostContext = context;

        mHostPackage = getPackageName();
        mHostResources = getResources();

        if (mRCSInCallUIPlugin.isRCSEnable()) {
            createVideoPanel(view);
            createImagePanel(view);
        }

        TextView username = (TextView) view.findViewById(
                mHostResources.getIdentifier("name", "id", mHostPackage));
        TextView elapsedTime = (TextView) view.findViewById(
                mHostResources.getIdentifier("elapsedTime", "id", mHostPackage));

        //Move the hashmap code here to fix memory leak
        HashMap<Integer, TextView> map = new HashMap<Integer, TextView>();
        map.put(new Integer(1), username);
        map.put(new Integer(3), elapsedTime);

        mRichCallController.onViewSetup(map);
    }

    private String getPackageName() {
        return mRCSInCallUIPlugin.getInCallActivity().getPackageName();
    }

    private Resources getResources() {
        return mRCSInCallUIPlugin.getInCallActivity().getResources();
    }

    private void createImagePanel(View view) {
        Log.d(TAG, "createImagePanel");
        ImageView photo = (ImageView) view.findViewById(
                                mHostResources.getIdentifier("photo", "id", mHostPackage));

        //Maybe we can use this button to judge if in conference call screen???
        View confButton = (View) view.findViewById(
                mHostResources.getIdentifier("manage_conference_call_button", "id", mHostPackage));
        //The relative we used to add circle contact image view
        RelativeLayout photoLayout = (RelativeLayout) view.findViewById(
                mHostResources.getIdentifier("nameAndPhoto", "id", mHostPackage));
        TextView name = (TextView) view.findViewById(mHostResources.getIdentifier("name",
                                "id", mHostPackage));

        ViewGroup photoGroup = null;
        if (photo != null) {
            photoGroup = (ViewGroup) photo.getParent();
        }

        //Image panel framelayout
        FrameLayout image = new FrameLayout(mContext/*mRCSInCallUIPlugin.getInCallActivity()*/);
        image.setId(RCSInCallUIPlugin.ID_LAYOUT_IMAGE_PANEL);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        image.setLayoutParams(params);
        if (photoGroup != null) {
            photoGroup.addView(image);
        }
        image.setVisibility(View.GONE);
        mRCSInCallUIPlugin.setImageResources(photoGroup, confButton, photo,
                                                        photoLayout, image, name);
    }

    private void createVideoPanel(View view) {
        Log.d(TAG, "createVideoPanel");
        ImageView photo = (ImageView) view.findViewById(mHostResources.getIdentifier("photo",
                                "id", mHostPackage));

        ViewGroup photoGroup = null;
        if (photo != null) {
            photoGroup = (ViewGroup) photo.getParent();
        }

        //Video panel framelayout
        FrameLayout layout = new FrameLayout(mContext/*mRCSInCallUIPlugin.getInCallActivity()*/);
        layout.setId(RCSInCallUIPlugin.ID_LAYOUT_VIDEO_PANEL);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        layout.setLayoutParams(params);

        if (photoGroup != null) {
            photoGroup.addView(layout, params);
        }
        layout.setVisibility(View.GONE);
        mRCSInCallUIPlugin.setVideoResources(layout);
    }

    @Override
    public void updateVoiceRecordIcon(boolean visible) {
        //Log.d(TAG, "updateVoiceRecordIcon");
        refrehInstance(false);
        if (mRCSInCallUIPlugin.isRCSEnable() && mRCSInCallUIPlugin.checkHostLayout()) {
            mRichCallController.onRecordStatusUpdated(visible);
        }
    }

    @Override
    public void onImageLoaded(String callId, Object entry) {
        //Log.d(TAG, "updateVoiceRecordIcon");
        refrehInstance(false);
        if (!mRCSInCallUIPlugin.isRCSEnable()) {
            return;
        }

        if (!mRCSInCallUIPlugin.checkHostLayout()) {
            return;
        }

        if (!(entry instanceof ContactInfoCache.ContactCacheEntry)) {
            return;
        }

        ContactInfoCache.ContactCacheEntry contactEntry =
                (ContactInfoCache.ContactCacheEntry) entry;

        com.android.incallui.Call inCallCall = CallList.getInstance().getCallById(callId);
        //String telecomCallId = inCallCall.getTelecommCall().getCallId();

        if (inCallCall != null) {
            android.telecom.Call telecomCall = inCallCall.getTelecommCall();
            if (telecomCall != null && telecomCall.getDetails() != null) {
                Details details = telecomCall.getDetails();
                if (details.getHandle() != null) {
                    String scheme = details.getHandle().getScheme();
                    String uriString = details.getHandle().getSchemeSpecificPart();
                    if ("tel".equals(scheme)) {
                        Drawable photo = contactEntry.photo;
                        mRCSInCallUIPlugin.putDrawable(uriString, photo);
                        mRichCallController.onCallPhotoChanged(telecomCall);
                    }
                }
            }
        }
    }

    private void refrehInstance(boolean isForceRefresh) {
        if (!RichCallInsController.isNeedRefreshInstance() && !isForceRefresh) {
            return;
        }

        Activity currActivity = RichCallInsController.getCurrentActivity();
        mRichCallController = RichCallInsController.getController(currActivity);
        mRCSInCallUIPlugin = RichCallInsController.getInCallUIPlugin(currActivity);
    }
}
