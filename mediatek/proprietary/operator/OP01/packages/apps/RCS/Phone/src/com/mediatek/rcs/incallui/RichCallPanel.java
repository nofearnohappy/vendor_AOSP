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
//import android.util.Log;

import com.mediatek.rcs.incallui.RichCallAdapter.RichCallInfo;
import com.mediatek.rcs.incallui.ext.RCSInCallUIPlugin;

public class RichCallPanel {
    private static final String TAG = "RichCallPanel";
    public static final int RCS_PANEL_IMAGE = 1;
    public static final int RCS_PANEL_VIDEO = 2;
    public static final int RCS_PANEL_ICON = 3;
    protected Context mContext;
    protected RCSInCallUIPlugin mRCSInCallUIPlugin;
    protected boolean           mPanelOpen;
    protected   int             mPanelType;

    public RichCallPanel(Context cnx, RCSInCallUIPlugin plugin) {
        mContext = cnx;
        mRCSInCallUIPlugin = plugin;
    }

    public void init() {
        //Do nothing
    }

    public void openPanel(RichCallInfo info) {
        //Do nothing
    }

    public void closePanel() {
        //Do nothing
    }

    public void pause() {
        //Do nothing
    }

    public void refreshPhoto(Drawable drawable) {
        //Do nothing
    }

    public void updateAudioState(boolean visible) {
        //Do nothing
    }

    public void releaseResource() {
        //Do nothing
    }

    public boolean isPanelOpen() {
        return mPanelOpen;
    }

    public int getPanelType() {
        return mPanelType;
    }

    protected int dip2dx(Context cnx, int dimension) {
        float dpValue = cnx.getResources().getDimension(dimension);
        final float scale = cnx.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5);
    }

    protected int sp2dx(Context cnx, int dimension) {
        float dpValue = cnx.getResources().getDimension(dimension);
        final float fontScale = cnx.getResources().getDisplayMetrics().scaledDensity;
        return (int) (dpValue * fontScale + 0.5);
    }
}
