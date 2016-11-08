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
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.mediatek.common.PluginImpl;
import com.mediatek.incallui.ext.DefaultRCSeInCallExt;
import com.mediatek.incallui.ext.IInCallScreenExt;
import com.mediatek.incallui.ext.IRCSeInCallExt;
import com.mediatek.rcs.incallui.RichCallController;
import com.mediatek.rcs.incallui.utils.RichCallInsController;

import org.gsma.joyn.JoynServiceConfiguration;

@PluginImpl(interfaceName="com.mediatek.incallui.ext.IRCSeInCallExt")
public class RCSInCallUiExtension extends DefaultRCSeInCallExt {

    private static final String TAG = "RCSInCallUiExtension";

    private Context mContext;

    private RichCallController mRichCallController;
    private RCSInCallUIPlugin mRCSInCallUIPlugin;

    public RCSInCallUiExtension(Context context) {
        super();
        Log.d(TAG, "RCSInCallUiExtension");
        mContext = context;
    }

    /**
      * Interface to setup incallactivity instance to Plugin side
      *
      * @param icicle
      * @param inCallActivity the incallactivity
      * @param iInCallScreenExt
      */
    @Override
    public void onCreate(Bundle icicle,
            Activity inCallActivity, IInCallScreenExt iInCallScreenExt) {
        Log.d(TAG, "onCreate");
        mRCSInCallUIPlugin = new RCSInCallUIPlugin();
        mRCSInCallUIPlugin.setInCallActivity(inCallActivity);
        mRCSInCallUIPlugin.setPluginContext(mContext);
        JoynServiceConfiguration config = new JoynServiceConfiguration();
        boolean isRCSOpen = config.isServiceActivated(mContext);
        mRCSInCallUIPlugin.setRCSStatus(isRCSOpen);
        Log.d(TAG, "onCreate with rcs status = " + isRCSOpen);

        mRichCallController = new RichCallController(mRCSInCallUIPlugin);
        if (mRCSInCallUIPlugin.isRCSEnable()) {
            mRichCallController.onStart();
        }
        RichCallInsController.insertMaps(inCallActivity, mRichCallController, mRCSInCallUIPlugin);
    }

    /**
      * Interface to tell plugin incallactivity maybe reenter
      * @param intent
      */
    @Override
    public void onNewIntent(Intent intent) {
        refrehInstance();
        if (mRCSInCallUIPlugin != null &&
            mRCSInCallUIPlugin.isRCSEnable() &&
            mRCSInCallUIPlugin.checkHostLayout()) {
            mRichCallController.onNewIntent(intent);
        }
    }

    /**
      * Interface to let service presenter to unbind service and do some clean process
      * @param inCallActivity thr incallactivity
      */
    @Override
    public void onDestroy(Activity inCallActivity) {
        Log.d(TAG, "onDestroy");
        RichCallController richCallController =
                RichCallInsController.getController(inCallActivity);
        RCSInCallUIPlugin  rcsInCallUIPlugin =
                RichCallInsController.getInCallUIPlugin(inCallActivity);
        if (richCallController != null) {
            richCallController.onVanish();
        }

        if (rcsInCallUIPlugin != null) {
            rcsInCallUIPlugin.releaseResource();
        }
        RichCallInsController.clearActivityMap(inCallActivity);
    }

    private void refrehInstance() {
        if (!RichCallInsController.isNeedRefreshInstance()) {
            return;
        }

        Activity currActivity = RichCallInsController.getCurrentActivity();
        mRichCallController = RichCallInsController.getController(currActivity);
        mRCSInCallUIPlugin = RichCallInsController.getInCallUIPlugin(currActivity);
    }
}

