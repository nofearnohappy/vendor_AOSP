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

package com.mediatek.rcs.incallui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.telecom.Call;
import android.util.Log;

import com.mediatek.rcs.incallui.RichCallAdapter.RichCallInfo;
import com.mediatek.rcs.incallui.ext.RCSInCallUIPlugin;
import com.mediatek.rcs.incallui.icon.RichCallIconPanel;
import com.mediatek.rcs.incallui.image.RichCallImagePanel;
import com.mediatek.rcs.incallui.utils.PhoneUtils;
import com.mediatek.rcs.incallui.video.RichCallVideoPanel;

import java.util.ArrayList;

public class RichCallPanelController {
    private static final String TAG = "RichCallPanelController";
    private ArrayList<RichCallPanel> mPanels = new ArrayList<RichCallPanel>();
    RCSInCallUIPlugin mRCSInCallUIPlugin;

    public RichCallPanelController(Context cnx, RCSInCallUIPlugin plugin) {
        mRCSInCallUIPlugin = plugin;
        RichCallPanel image = new RichCallImagePanel(cnx, plugin);
        RichCallPanel video = new RichCallVideoPanel(cnx, plugin);
        RichCallPanel icon  = new RichCallIconPanel(cnx, plugin);

        mPanels.add(image);
        mPanels.add(video);
        mPanels.add(icon);
    }

    public void init() {
        Log.d(TAG, "init");
        for (RichCallPanel panel : mPanels) {
            panel.init();
        }
    }

    public void closePanel() {
        Log.d(TAG, "closePanel");
        for (RichCallPanel panel : mPanels) {
            if (panel.isPanelOpen()) {
                panel.closePanel();
            }
        }
    }

    public void pause() {
        Log.d(TAG, "pause");
        for (RichCallPanel panel : mPanels) {
            if (panel.isPanelOpen()) {
                panel.pause();
            }
        }
    }

    public void releaseResource() {
        mRCSInCallUIPlugin = null;
        Log.d(TAG, "releaseResource");
        for (RichCallPanel panel : mPanels) {
            panel.releaseResource();
        }
        mPanels.clear();
    }

    public void updateAudioState(boolean visible) {
        for (RichCallPanel panel : mPanels) {
            panel.updateAudioState(visible);
        }
    }

    public void refreshPhoto(Drawable drawable) {
        Log.d(TAG, "refreshPhoto");
        for (RichCallPanel panel : mPanels) {
            panel.refreshPhoto(drawable);
        }
    }

    public void loadPanel(Call call, RichCallInfo info) {
        if (info == null || !PhoneUtils.isFileExists(info)) {
            Log.d(TAG, "info is null or file is null, just show default panel.");
            openDefaultPanel();
            closePanelByType(RichCallPanel.RCS_PANEL_VIDEO);
            return;
        }

        Log.d(TAG, "loadPanel, type = " + info.mResourceType);
        if (info.mResourceType == RichCallAdapter.RES_TYPE_PIC ||
            info.mResourceType == RichCallAdapter.RES_TYPE_GIF) {
            //Open Image panel and close video panel.
            openPanelByType(RichCallPanel.RCS_PANEL_IMAGE, info);
            closePanelByType(RichCallPanel.RCS_PANEL_VIDEO);
        } else if (info.mResourceType == RichCallAdapter.RES_TYPE_VID) {
            //We meet strange issue, we need not remove surface, or else will show backgroud.
            //So we need close video panel before open another panel.
            if (isPanelOpen(RichCallPanel.RCS_PANEL_VIDEO)) {
                openDefaultPanel();
                closePanelByType(RichCallPanel.RCS_PANEL_VIDEO);
                RichCallPanel imagePanel = findPanelByType(RichCallPanel.RCS_PANEL_IMAGE);
                RichCallPanel videoPanel = findPanelByType(RichCallPanel.RCS_PANEL_VIDEO);
                PanelRunnable runnable = new PanelRunnable(imagePanel, videoPanel, info);
                Handler handler = new Handler();
                handler.postDelayed(runnable, 100);
            } else {
                //Open video panel and close image panel.
                openPanelByType(RichCallPanel.RCS_PANEL_VIDEO, info);
                closePanelByType(RichCallPanel.RCS_PANEL_IMAGE);
            }
        } else {
            Log.d(TAG, "loadPanel, type = ");
        }

    }

    public void openIconPanel(Call call) {
        Log.d(TAG, "openIconPanel");
        RichCallIconPanel iconPanel =
                (RichCallIconPanel) findPanelByType(RichCallPanel.RCS_PANEL_ICON);
        if (iconPanel != null) {
            iconPanel.openPanel(getIconDrawable(call));
        }
    }

    public void openDefaultPanel() {
        Log.d(TAG, "openDefaultPanel");
        RichCallImagePanel imagePanel =
                (RichCallImagePanel) findPanelByType(RichCallPanel.RCS_PANEL_IMAGE);
        if (imagePanel != null) {
            imagePanel.openDefaultPanel();
        }
    }

    public void openDefaultPanelEx(boolean cached) {
        Log.d(TAG, "openDefaultPanelEx");
        RichCallImagePanel imagePanel =
                (RichCallImagePanel) findPanelByType(RichCallPanel.RCS_PANEL_IMAGE);
        if (imagePanel != null) {
            imagePanel.openDefaultPanelEx(cached);
        }
    }

    public boolean isPanelOpen(int type) {
        RichCallPanel panel = findPanelByType(type);
        if (panel != null) {
            return panel.isPanelOpen();
        }
        return false;
    }

    public void openPanelByType(int type, RichCallInfo info) {
        RichCallPanel panel = findPanelByType(type);
        if (panel != null) {
            panel.openPanel(info);
        }
    }

    public void closePanelByType(int type) {
        RichCallPanel panel = findPanelByType(type);
        if (panel != null) {
            panel.closePanel();
        }
    }

    private RichCallPanel findPanelByType(int type) {
        for (RichCallPanel panel : mPanels) {
            if (panel.getPanelType() == type) {
                return panel;
            }
        }
        return null;
    }

    private class PanelRunnable implements Runnable {
        private RichCallPanel mImagePanel;
        private RichCallPanel mVideoPanel;
        private RichCallInfo  mInfo;
        public PanelRunnable(RichCallPanel image,
                RichCallPanel video, RichCallInfo info) {
            mImagePanel = image;
            mVideoPanel = video;
            mInfo       = info;
        }

        @Override
        public void run() {
            Log.d(TAG, "PanelRunnable, run");
            if (mVideoPanel != null && mInfo != null) {
                mVideoPanel.openPanel(mInfo);
            }

            if (mImagePanel != null) {
                mImagePanel.closePanel();
            }

            mImagePanel = null;
            mVideoPanel = null;
            mInfo = null;
        }

    }

    private Drawable getIconDrawable(Call call) {
        Drawable drawable = null;
        String number = PhoneUtils.parseNumber(call);
        return mRCSInCallUIPlugin.getDrawable(number);
    }
}
