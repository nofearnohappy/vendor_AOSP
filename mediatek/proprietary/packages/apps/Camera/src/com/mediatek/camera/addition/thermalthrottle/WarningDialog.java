/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2014. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.camera.addition.thermalthrottle;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.camera.R;
import com.mediatek.camera.ui.CameraView;
import com.mediatek.camera.util.Log;

public class WarningDialog extends CameraView {
    private static final String TAG = "WarningDialog";

    private View mWarningDialogTitleLayout;
    private View mWarningDialogButtonLayout;
    private TextView mWarningDialogTitle;
    private TextView mWarningDialogText;
    private TextView mWarningDlgButton;
    private View mWarningDialogTitleDivider;
    private ImageView mWarningDialogImageView;
    private TextView mWarningDialogTime;
    private TextView mWarningDialogtitleName;

    private String mTitle;
    private String mMessage;
    private String mButton;
    private Runnable mRunnable;

    public WarningDialog(Activity activity) {
        super(activity);
    }

    @Override
    protected View getView() {
        View v = inflate(R.layout.warning_dialog);
        mWarningDialogTitleLayout = v.findViewById(R.id.alert_dialog_title_layout);
        mWarningDialogButtonLayout = v.findViewById(R.id.alert_dialog_button_layout);
        mWarningDialogTitle = (TextView) v.findViewById(R.id.alert_dialog_title);
        mWarningDialogText = (TextView) v.findViewById(R.id.alert_dialog_text);
        mWarningDlgButton = (Button) v.findViewById(R.id.alert_dialog_button1);
        mWarningDialogTitleDivider = (View) v.findViewById(R.id.alert_dialog_title_divider);
        mWarningDialogImageView = (ImageView) v.findViewById(R.id.alert_dialog_title_icon);
        mWarningDialogTime = (TextView) v.findViewById(R.id.alert_dialog_time);
        mWarningDialogtitleName = (TextView) v.findViewById(R.id.alert_dialog_title_name);

        return v;
    }

    @Override
    public void uninit() {
        if (isShowing()) {
            hide();
            return;
        }

        super.uninit();
    }

    @Override
    public void reset() {
        mTitle = null;
        mMessage = null;
        mButton = null;
        mRunnable = null;
    }

    @Override
    public void refresh() {
        resetRotateDialog();
        if (mTitle != null && mWarningDialogTitle != null) {
            mWarningDialogTitle.setTextColor(getMainColor(getContext()));
            mWarningDialogTitle.setText(mTitle);
            if (mWarningDialogTitleLayout != null) {
                mWarningDialogTitleLayout.setVisibility(View.VISIBLE);
            }
            if (mWarningDialogTitleDivider != null) {
                mWarningDialogTitleDivider.setBackgroundColor(getMainColor(getContext()));
            }
        }
        if (mWarningDialogText != null) {
            mWarningDialogText.setText(mMessage);

        }
        if (mWarningDialogtitleName != null) {
            mWarningDialogtitleName.setText(R.string.pref_thermal_dialog_title);
        }
        if (mWarningDialogTime != null) {
            mWarningDialogTime.setText(R.string.pref_thermal_dialog_timer);
        }
        if (mWarningDialogImageView != null) {
            mWarningDialogImageView.setImageResource(R.drawable.ic_dialog_alert);
        }
        if (mButton != null) {
            mWarningDlgButton.setText(mButton);
            mWarningDlgButton.setContentDescription(mButton);
            mWarningDlgButton.setVisibility(View.VISIBLE);
            mWarningDlgButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mRunnable != null) {
                        mRunnable.run();
                    }
                    hide();
                }
            });
            mWarningDialogButtonLayout.setVisibility(View.VISIBLE);
        }
        Log.i(TAG, "onRefresh() mTitle=" + mTitle + ", mMessage=" + mMessage + ", mButton="
                + mButton + ", mRunnable=" + mRunnable);
    }

    @Override
    protected Animation getFadeInAnimation() {
        return AnimationUtils.loadAnimation(getContext(), R.anim.setting_popup_grow_fade_in);
    }

    @Override
    protected Animation getFadeOutAnimation() {
        return AnimationUtils.loadAnimation(getContext(), R.anim.setting_popup_shrink_fade_out);
    }

    public void setCountDownTime(String time) {
        mWarningDialogTime.setText(time);
    }

    public void showAlertDialog(String title, String msg, String button1Text, final Runnable r) {
        reset();
        mTitle = title;
        mMessage = msg;
        mButton = button1Text;
        mRunnable = r;
        super.show();
    }

    private void resetRotateDialog() {
        mWarningDialogTitleLayout.setVisibility(View.GONE);
        mWarningDlgButton.setVisibility(View.GONE);
        mWarningDialogButtonLayout.setVisibility(View.GONE);
    }

    private int getMainColor(Context context) {
        int finalColor = 0;
        finalColor = context.getResources().getColor(R.color.setting_item_text_color_highlight);
        return finalColor;
    }
}
