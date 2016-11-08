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
package com.mediatek.rcs.contacts.profileservice;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.mediatek.rcs.contacts.profileapp.ProfileManager;
import com.mediatek.rcs.contacts.profileservice.utils.ProfileConstants;
import com.mediatek.rcs.contacts.profileservice.utils.ProfileServiceLog;

import java.util.List;

//import org.gsma.joyn.JoynServiceConfiguration;

public class ProfileServiceReceiver extends BroadcastReceiver {

    private final static String TAG = "ProfileServiceReceiver";
    private static final String SIM_CHANGE = "com.mediatek.rcs.contacts.INTENT_RCS_LOGIN";
    private static final String VIEW_SETTINGS = "org.gsma.joyn.action.VIEW_SETTINGS";
    private static final String RCS_OFF = "com.mediatek.intent.rcs.stack.StopService";
    private static final String CONFIGURATION_STATUS = "com.orangelabs.rcs.CONFIGURATION_STATUS_TO_APP";  //RCSSetting.CONFIGURATION_STATUS
    private static final String CORE_CONFIGURATION_STATUS = "status";  //RCSSetting.CORE_CONFIGURATION_STATUS

    private final static int DELETE_PROFILE_DB = 1;
    private final static int GET_PROFILE_FROM_SERVER = 2;

    private ContentResolver mResolver;
    private ReceiverHandler mHandler;
    private HandlerThread mThread;
    private Context mContext;

    private final static int ILLEGAL_STATE = -1;
    private final static int RCS_CORE_IMS_CONNECTED = 4;


    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        if (intent.getAction().equals(SIM_CHANGE)) {
            ProfileServiceLog.d(TAG, "onReceive, SIM_CHANGE");
            if (mThread == null) {
            mThread = new HandlerThread("ProfileServiceReseiverThread");
            mThread.start();
            mHandler = new ReceiverHandler(mThread.getLooper());
        }
            mResolver = context.getContentResolver();
            Message msg = mHandler.obtainMessage(DELETE_PROFILE_DB);
            msg.sendToTarget();
        } else if (intent.getAction().equals(CONFIGURATION_STATUS)) {
            boolean status = intent.getBooleanExtra(CORE_CONFIGURATION_STATUS, false);
            ProfileServiceLog.d(TAG, "onReceive, CORE_CONFIGURATION_STATUS: " + status);
            if (status == true) {
                if (mThread == null) {
                    mThread = new HandlerThread("ProfileServiceReseiverThread");
                    mThread.start();
                    mHandler = new ReceiverHandler(mThread.getLooper());
                }
                Message msg = mHandler.obtainMessage(GET_PROFILE_FROM_SERVER);
                msg.sendToTarget();
            }
        } else if (intent.getAction().equals(RCS_OFF)) {
            ProfileServiceLog.d(TAG, "onReceive, RCS OFF. KILL" + mContext.getPackageName());
            ActivityManager ams = (ActivityManager)mContext
                        .getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.AppTask> tasks = ams.getAppTasks();
            for (ActivityManager.AppTask task : tasks) {
                ProfileServiceLog.d(TAG, "exclude task" + task.getTaskInfo().id);
                task.finishAndRemoveTask();
            }
            ams.forceStopPackage(mContext.getPackageName());
        }
    }

    private final class ReceiverHandler extends Handler {

        public ReceiverHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage (Message Msg) {
            ProfileServiceLog.d(TAG, "MyHandler handleMessage, msg:" + Msg.what);
            if(null == Msg)
            {
                ProfileServiceLog.d(TAG, "msg is null");
                return;
            }
            ProfileManager pm = ProfileManager.getInstance(mContext.getApplicationContext());
            switch (Msg.what) {
                case DELETE_PROFILE_DB:
                    deleteProfileDataBase();
                    pm.getMyProfileFromDB();
                    break;

                case GET_PROFILE_FROM_SERVER:
                    pm.getMyProfileFromServer();
                    break;

                 default:
                    break;
            }
            mThread.quitSafely();
            mThread = null;
        }
    }

    private void deleteProfileDataBase() {
        ProfileServiceLog.d(TAG, "deleteProfileDataBase");
        int count = 0;
        String[] projection = {"_id"};
        Cursor cursor = mResolver.query(ProfileConstants.CONTENT_URI, projection, null, null, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                count = mResolver.delete(ProfileConstants.CONTENT_URI, null, null);
            } else {
                ProfileServiceLog.d(TAG, "no record, no need to delete");
            }
        } else {
            ProfileServiceLog.d(TAG, "cursor is null! no need to delete");
        }
        cursor.close();
    }
    //check if RCS account is changed. return true if changed
    /*
    private boolean isAccountChanged(){
        ProfileServiceLog.d(TAG, "deleteProfileDataBase");
        String currentPhoneNumber;
        boolean retval = false;
        int count = 0;
        String[] projection = {ProfileConstants.PHONE_NUMBER};
        Cursor cursor = mResolver.query(ProfileConstants.CONTENT_URI, projection, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            int phoneNumberIndex = cursor.getColumnIndex(ProfileConstants.PHONE_NUMBER);
            currentPhoneNumber = cursor.getString(phoneNumberIndex);
            cursor.close();
        } else {
            ProfileServiceLog.d(TAG, "no curser yet, a new account");
            return true;
        }
        if (currentPhoneNumber != null && currentPhoneNumber.length() > 0) {
            JoynServiceConfiguration mJSConfig = new JoynServiceConfiguration();
            String sip = mJSConfig.getPublicUri(mContext);
            ProfileServiceLog.d(TAG, "sip is: " + sip + ", currentPhoneNumber is: " + currentPhoneNumber);
            retval = (!sip.contains(currentPhoneNumber));
        } else {
            ProfileServiceLog.d(TAG, "no phonenumber yet, a new account");
            return true;
        }
        return retval;
    }
    */
}
