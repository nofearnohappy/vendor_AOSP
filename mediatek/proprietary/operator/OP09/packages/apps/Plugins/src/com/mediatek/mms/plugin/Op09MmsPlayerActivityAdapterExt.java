/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
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

package com.mediatek.mms.plugin;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.mediatek.widget.ImageViewEx;
import com.mediatek.mms.ext.DefaultOpMmsPlayerActivityAdapterExt;


public class Op09MmsPlayerActivityAdapterExt extends DefaultOpMmsPlayerActivityAdapterExt {

    Op09MmsUnSupportedFilesExt mMmsUnsupportedFilesPlugin;
    public Op09MmsPlayerActivityAdapterExt(Context base) {
        super(base);
        mMmsUnsupportedFilesPlugin = Op09MmsUnSupportedFilesExt.getIntance(base);
    }

    private static String TAG = "Op09MmsPlayerActivityAdapterExt";

    @Override
    public void getView(Uri imageUri, String imageType, ImageViewEx image, Bitmap t,
            String videoType,
            ImageView video, String audioName, String audioType, ImageView audioIcon, View audio,
            LinearLayout viewGroup) {
        if (imageUri != null) {
            /// M: OP09 Feature: Unsupported Files @{
            if (MessageUtils.isUnsupportedFilesOn()) {
                if (!mMmsUnsupportedFilesPlugin.isSupportedFile(imageType, null)) {
                    mMmsUnsupportedFilesPlugin.setImageUnsupportedIcon(image, imageType, null);
                    mMmsUnsupportedFilesPlugin.setUnsupportedMsg((LinearLayout) image.getParent()
                            .getParent(), (View) image.getParent(), true);
                    Log.d(TAG, "add image unsupported view");
                } else {
                    Log.d(TAG, "remove image unsupported view");
                    mMmsUnsupportedFilesPlugin.setUnsupportedMsg((LinearLayout) image.getParent(),
                            (View) image.getParent(), false);
                }
            }
            /// @}
        }

        if (t != null) {
            /// M: OP09 Feature: Unsupported Files @{
            if (MessageUtils.isUnsupportedFilesOn()) {
                if (!mMmsUnsupportedFilesPlugin.isSupportedFile(videoType, null)) {
                    mMmsUnsupportedFilesPlugin.setVideoUnsupportedIcon(video, videoType, null);
                    mMmsUnsupportedFilesPlugin.setUnsupportedMsg((LinearLayout) (video.getParent()
                            .getParent().getParent()), (View) (video.getParent().getParent()),
                            true);
                    Log.d(TAG, "add video unsupported view");
                } else {
                    Log.d(TAG, "remove video unsupported view");
                    mMmsUnsupportedFilesPlugin.setUnsupportedMsg((LinearLayout) (video.getParent()
                            .getParent().getParent()), (View) (video.getParent().getParent()),
                            false);
                }
            }
            /// @}
        }

        if (audioName != null) {
            /// M: OP09 Feature: Unsupported Files @{
            if (MessageUtils.isUnsupportedFilesOn()) {
                if (!mMmsUnsupportedFilesPlugin.isSupportedFile(audioType, audioName)) {
                    mMmsUnsupportedFilesPlugin.setAudioUnsupportedIcon(audioIcon, audioType,
                            audioName);
                    mMmsUnsupportedFilesPlugin.setUnsupportedMsg(viewGroup, audio, true);
                    Log.d(TAG, "add audio unsupported view");
                } else {
                    Log.d(TAG, "remove audio unsupported view");
                    mMmsUnsupportedFilesPlugin.setUnsupportedMsg(viewGroup, audio, false);
                }
            }
            /// @}
        }
    }
}
