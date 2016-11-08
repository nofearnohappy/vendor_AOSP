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

package com.mediatek.rcs.incallui.image;

import android.content.Context;
//import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.mediatek.rcs.incallui.RichCallAdapter.RichCallInfo;
import com.mediatek.rcs.incallui.ext.RCSInCallUIPlugin;
import com.mediatek.rcs.incallui.RichCallPanel;
import com.mediatek.rcs.phone.R;

public class RichCallImagePanel extends RichCallPanel {

    private static final String TAG = "RichCallImagePanel";

    private static int ID_VIEW_RICH_PIC = 5678791;
    private static int ID_VIEW_RICH_GIF = 5678792;
    private static int ID_VIEW_RICH_TEXT = 5678793;

    private AsyncPhotoManager mAsyncPhotoManager;
    private ImageView         mRichView;
    private ImageView         mRecordImageView;
    private RichGIFView       mRichGIFView;
    private TextView          mGreetingView;
    private FrameLayout       mRichLayout;
    private boolean           mDefaultPanel;
    public RichCallImagePanel(Context cnx, RCSInCallUIPlugin plugin) {
        super(cnx, plugin);
        mPanelType = RCS_PANEL_IMAGE;
    }

    public void init() {
        Log.d(TAG, "init");
        mDefaultPanel = true;
        mRichLayout = mRCSInCallUIPlugin.getImagePanelRect();

        //RichView for Image file.
        mRichView = new ImageView(mContext);
        mRichView.setId(ID_VIEW_RICH_PIC);
        mRichView.setVisibility(View.GONE);

        FrameLayout.LayoutParams imageParams = new FrameLayout.LayoutParams(
                                                      FrameLayout.LayoutParams.MATCH_PARENT,
                                                      FrameLayout.LayoutParams.MATCH_PARENT);
        //Greeting info for feature use.
        //mGreetingView = new TextView(mContext);
        //mGreetingView.setId(ID_VIEW_RICH_TEXT);
        //mGreetingView.setGravity(Gravity.CENTER);
        //mGreetingView.setTextSize(sp2dx(mContext, R.dimen.rich_call_text_size));
        //mGreetingView.setTextColor(Color.RED);
        //mGreetingView.setVisibility(View.GONE);
        //FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                                                    //FrameLayout.LayoutParams.WRAP_CONTENT,
                                                    //FrameLayout.LayoutParams.WRAP_CONTENT);
        //mRichLayout.addView(mGreetingView, textParams);

        //GIF view for gif file.
        mRichGIFView = new RichGIFView(mContext);
        mRichView.setId(ID_VIEW_RICH_GIF);
        mRichView.setVisibility(View.GONE);

        FrameLayout.LayoutParams recordParams =
                new FrameLayout.LayoutParams(
                    dip2dx(mContext, R.dimen.incall_record_icon_size),
                    dip2dx(mContext, R.dimen.incall_record_icon_size));

        recordParams.setMargins(0,
                dip2dx(mContext, R.dimen.incall_record_icon_margin),
                dip2dx(mContext, R.dimen.incall_record_icon_margin), 0);

        recordParams.gravity = Gravity.RIGHT;

        //Record view for phone record.
        mRecordImageView = new ImageView(mContext);
        mRecordImageView.setLayoutParams(recordParams);
        mRecordImageView.setImageResource(R.drawable.voice_record);
        mRecordImageView.setVisibility(View.GONE);

        mRichLayout.addView(mRichView, imageParams);
        mRichLayout.addView(mRichGIFView, imageParams);
        mRichLayout.addView(mRecordImageView, recordParams);
        mRichLayout.setVisibility(View.GONE);

        mAsyncPhotoManager =
                new AsyncPhotoManager(mRichView,
                        mRichGIFView, mGreetingView, mContext, mRCSInCallUIPlugin);
    }

    public void openDefaultPanel() {
        if (mRCSInCallUIPlugin.shouldShowPanel()) {
            Log.d(TAG, "openDefaultPanel");
            mRichGIFView.setVisibility(View.GONE);
            mPanelOpen = true;

            mRCSInCallUIPlugin.setHostVisibility(View.GONE);

            mRichView.setVisibility(View.VISIBLE);
            mRichLayout.setVisibility(View.VISIBLE);

            mRichView.setImageResource(R.drawable.default_rich_screen);
        }
    }

    public void openDefaultPanelEx(boolean cached) {
        if ((mRCSInCallUIPlugin.shouldShowPanel() && mDefaultPanel) || !cached) {
            Log.d(TAG, "openDefaultPanelEx");
            //Bug fixed, we should show default panel when richinfo is in quering status,
            //we need to make gif view invisible, or else will get issue when hold call is gif
            //and fg active call is in querying status
            if (!cached) {
                mRichGIFView.setVisibility(View.GONE);
            }

            mDefaultPanel = false;
            mPanelOpen = true;

            mRCSInCallUIPlugin.setHostVisibility(View.GONE);

            mRichView.setVisibility(View.VISIBLE);
            mRichLayout.setVisibility(View.VISIBLE);

            mRichView.setImageResource(R.drawable.default_rich_screen);
        }
    }

    @Override
    public void openPanel(RichCallInfo info) {
        Log.d(TAG, "openPanel, isPanelOpen = " + mPanelOpen);
        if (mRCSInCallUIPlugin.shouldShowPanel()) {
            mRCSInCallUIPlugin.setHostVisibility(View.GONE);
            mRichLayout.setVisibility(View.VISIBLE);

            mAsyncPhotoManager.loadRichPhoto(info);
            mPanelOpen = true;
        }
    }

    @Override
    public void closePanel() {
        Log.d(TAG, "closePanel, isPanelOpen = " + mPanelOpen);
        if (isPanelOpen()) {
            //mDefaultPanel = true;
            mRichView.setVisibility(View.GONE);
            mRichGIFView.setVisibility(View.GONE);
            //mGreetingView.setVisibility(View.GONE);
            mRichLayout.setVisibility(View.GONE);

            mAsyncPhotoManager.stopRichPhoto();
            mPanelOpen = false;

            mRCSInCallUIPlugin.setHostVisibility(View.VISIBLE);
        }
    }

    @Override
    public void pause() {
        Log.d(TAG, "pause");
        mAsyncPhotoManager.stopRichPhoto();
    }

    @Override
    public void releaseResource() {
        Log.d(TAG, "releaseResource");
        mAsyncPhotoManager.clearPhotoCache();

        if (mRichGIFView != null) {
            mRichGIFView.releaseResource();
        }

        if (mRichLayout != null) {
            mRichLayout.removeView(mRichView);
            mRichLayout.removeView(mRichGIFView);
            mRichLayout.removeView(mRecordImageView);
            mRichView = null;
            mRichGIFView = null;
            mRecordImageView = null;
            //mGreetingView = null;
        }

        ViewGroup group = mRCSInCallUIPlugin.getHostViewGroup();
        if (group != null) {
            group.removeView(mRichLayout);
            mRichLayout = null;
        }
        mAsyncPhotoManager = null;
        mRCSInCallUIPlugin = null;
    }

    @Override
    public void updateAudioState(boolean visible) {
        if (isPanelOpen()) {
            mRecordImageView.setVisibility(visible ? View.VISIBLE : View.GONE);
            AnimationDrawable ad = (AnimationDrawable) mRecordImageView.getDrawable();
            if (ad != null) {
                if (visible && !ad.isRunning()) {
                    ad.start();
                } else if (!visible && ad.isRunning()) {
                    ad.stop();
                }
            }
        }
    }
}
