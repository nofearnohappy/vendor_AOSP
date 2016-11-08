/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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
package com.mediatek.settings.plugin;

import android.content.Context;
import android.widget.TextView;
import android.view.View;
import android.util.Log;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.mediatek.common.PluginImpl;
import com.mediatek.settings.ext.DefaultAppListExt;
import com.mediatek.op03.plugin.R;


@PluginImpl(interfaceName = "com.mediatek.settings.ext.IAppListExt")
public class OP03AppListExt extends DefaultAppListExt {
    private static final String TAG = "OP03AppListExt";
    public Context mContext;
    private int defaultPosition;
    private boolean isMMS;
    public OP03AppListExt(Context context)  {
              super(context);
              mContext = context;
              Log.i(TAG, "constructor\n");
        }

    public View addLayoutAppView(View view, TextView textView, TextView defaultLabel,
                                     int position, Drawable image, ViewGroup parent) {
        Log.i(TAG, "addLayoutAppView: " + position + "defaultPosition:- " + defaultPosition);
        if (isMMS == true)
        {
            if (position == defaultPosition) {
                LayoutInflater inflater =
                  (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View pview = inflater.inflate(R.layout.op03_app_preference_item, parent, false);

                TextView ctv = (TextView) pview.findViewById(R.id.app_label);
                TextView textDefaultLabel = (TextView) pview.findViewById(R.id.default_label);
                TextView tv = (TextView) pview.findViewById(R.id.sub_app_text);

                ImageView imageView = (ImageView) pview.findViewById(R.id.app_image);
                imageView.setImageDrawable(image);

                textDefaultLabel.setText(defaultLabel.getText());
                textDefaultLabel.setVisibility(defaultLabel.getVisibility());
                textDefaultLabel.setTextColor(defaultLabel.getTextColors());
                Log.i(TAG, "addLayoutAppView: defaultLabel:- " + defaultLabel.getVisibility());
                ctv.setText(textView.getText());
                ctv.setTextColor(textView.getTextColors());

                Log.i(TAG, "addLayoutAppView: ctv:- " + ctv.getText());
                ctv.setVisibility(View.VISIBLE);

                tv.setVisibility(View.VISIBLE);
                tv.setText(getResources().getString(R.string.recommended_by_orange));
                tv.setTextColor(textView.getTextColors());
                return pview;
            }
        }
        return view;
    }

    public void setAppListItem(String packageName, int position) {
        Log.i(TAG, "setAppListItem" + position);
        if (packageName.equals("com.android.mms")) {
            isMMS = true;
            defaultPosition = position;
        }
    }
}
