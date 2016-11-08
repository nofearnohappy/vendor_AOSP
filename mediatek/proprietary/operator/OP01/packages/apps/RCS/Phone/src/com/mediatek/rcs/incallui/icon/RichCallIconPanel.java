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

package com.mediatek.rcs.incallui.icon;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mediatek.rcs.incallui.ext.RCSInCallUIPlugin;
import com.mediatek.rcs.incallui.RichCallPanel;
import com.mediatek.rcs.phone.R;

public class RichCallIconPanel extends RichCallPanel {
    private static final String TAG = "RichCallIconPanel";
    private static int ID_VIEW_CONTACT_PHOTO = 5678795;

    private CircleImageView   mContactPhotoView;
    private RelativeLayout    mContactPhotoLayout;
    private TextView          mResizingNameView;

    public RichCallIconPanel(Context cnx, RCSInCallUIPlugin plugin) {
        super(cnx, plugin);
        mPanelType = RCS_PANEL_ICON;
    }

    public void init() {
        Log.d(TAG, "init");

        mContactPhotoLayout = mRCSInCallUIPlugin.getContactPhotoRect();
        mContactPhotoView = new CircleImageView(mContext);
        mContactPhotoView.setUseDefaultStyle(false);
        mContactPhotoView.setVisibility(View.GONE);
        mContactPhotoView.setPadding(0, 0, 1, 0);
        RelativeLayout.LayoutParams relativeParams = new RelativeLayout.LayoutParams(
                dip2dx(mContext, R.dimen.rich_call_photo_width),
                dip2dx(mContext, R.dimen.rich_call_photo_height));

        relativeParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        mContactPhotoView.setLayoutParams(relativeParams);
        mContactPhotoView.setClickable(false);
        mContactPhotoView.setId(ID_VIEW_CONTACT_PHOTO);
        mContactPhotoView.setVisibility(View.GONE);
        mContactPhotoLayout.addView(mContactPhotoView);
        resizeTextView(false);
    }

    private void resizeTextView(boolean reset) {
        Log.d(TAG, "resizeTextView, reset = " + reset);
        mResizingNameView = mRCSInCallUIPlugin.getResizingNameView();
        RelativeLayout.LayoutParams relativeParams =
                (RelativeLayout.LayoutParams) mResizingNameView.getLayoutParams();
        if (!reset) {
            relativeParams.addRule(RelativeLayout.LEFT_OF, ID_VIEW_CONTACT_PHOTO);
        } else {
            relativeParams.removeRule(RelativeLayout.LEFT_OF);
        }
        mResizingNameView.setLayoutParams(relativeParams);
    }

    public void openPanel(Drawable drawable) {
        Log.d(TAG, "openPanel");
        if (mRCSInCallUIPlugin.shouldShowPanel()) {
            mRCSInCallUIPlugin.setHostVisibility(View.GONE);
            mContactPhotoView.setVisibility(View.VISIBLE);
            if (drawable != null) {
                mContactPhotoView.setImageDrawable(drawable);
            } else {
                mContactPhotoView.setImageResource(R.drawable.default_contact_photo);
            }
            resizeTextView(false);
            mPanelOpen = true;
        }
    }

    @Override
    public void closePanel() {
        Log.d(TAG, "closePanel, isPanelOpen = " + mPanelOpen);
        if (isPanelOpen()) {
            mContactPhotoView.setVisibility(View.GONE);
            mRCSInCallUIPlugin.setHostVisibility(View.VISIBLE);
            resizeTextView(true);
            mPanelOpen = false;
        }
    }

    @Override
    public void refreshPhoto(Drawable drawable) {
        if (mContactPhotoView != null && drawable != null) {
            Log.d(TAG, "refreshPhoto");
            mContactPhotoView.setImageDrawable(drawable);
            mContactPhotoView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void releaseResource() {
        Log.d(TAG, "releaseResource");
        if (mContactPhotoLayout != null) {
            mContactPhotoLayout.removeView(mContactPhotoView);
            mContactPhotoView = null;
            mResizingNameView = null;
            mContactPhotoLayout = null;
        }
        mRCSInCallUIPlugin = null;
    }
}
