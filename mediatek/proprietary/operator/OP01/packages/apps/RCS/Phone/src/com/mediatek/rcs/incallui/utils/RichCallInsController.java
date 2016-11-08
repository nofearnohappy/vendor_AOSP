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

package com.mediatek.rcs.incallui.utils;

import android.app.Activity;
import android.util.Log;

import com.mediatek.rcs.incallui.RichCallController;
import com.mediatek.rcs.incallui.ext.RCSInCallUIPlugin;

import java.util.HashMap;

public class RichCallInsController {

    private static final String TAG = "RichCallInsController";

    private static HashMap<Integer, Activity> mHashActivity =
                    new HashMap<Integer, Activity>();

    private static HashMap<Activity, RichCallController> mHashController =
                    new HashMap<Activity, RichCallController>();

    private static HashMap<Activity, RCSInCallUIPlugin> mHashInCallUiPlugin =
                    new HashMap<Activity, RCSInCallUIPlugin>();

    private static boolean mNeedRefresh = false;

    public static boolean isNeedRefreshInstance() {
        Log.d(TAG, "isNeedRefreshInstance, mNeedRefresh = " + mNeedRefresh);
        return mNeedRefresh;
    }

    public static void insertMaps(Activity activity,
                RichCallController controller, RCSInCallUIPlugin plugin) {
        dumpMapInfo();
        int sizeActivity = mHashActivity.size();
        int index = sizeActivity + 1;
        mHashActivity.put(new Integer(index), activity);
        mHashController.put(activity, controller);
        mHashInCallUiPlugin.put(activity, plugin);

        //Meaning onCreate twice, so need to refresh instance
        if (index > 1) {
            mNeedRefresh = true;
        }
    }

    private static void dumpMapInfo() {
        int sizeActivity = mHashActivity.size();
        int sizeController = mHashController.size();
        int sizeInCallUiPlugin = mHashInCallUiPlugin.size();
        Log.d(TAG, "insertActivityMap + sizeActivity = " + sizeActivity + ", sizeController = " +
            sizeController + ", sizeInCallUiPlugin = " + sizeInCallUiPlugin);
    }

    public static void clearActivityMap(Activity activity) {
        Log.d(TAG, "clearActivityMap");
        mHashActivity.remove(activity);
        mHashController.remove(activity);
        mHashInCallUiPlugin.remove(activity);
        int sizeActivity = mHashActivity.size();

        //Meaning there is no instance, so no need to refresh
        if (sizeActivity == 0) {
            mNeedRefresh = false;
        }
        dumpMapInfo();
    }

    public static Activity getCurrentActivity() {
        int sizeActivity = mHashActivity.size();
        Log.d(TAG, "getCurrentActivity, sizeActivity = " + sizeActivity);
        Activity activity = mHashActivity.get(new Integer(sizeActivity));
        return activity;
    }

    public static RichCallController getController(Activity activity) {
        RichCallController controller = mHashController.get(activity);
        return controller;
    }

    public static RCSInCallUIPlugin getInCallUIPlugin(Activity activity) {
        RCSInCallUIPlugin plugin = mHashInCallUiPlugin.get(activity);
        return plugin;
    }
}
