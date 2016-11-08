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

package com.cmcc.ccs.profile;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
//import com.cmcc.ccs.RCSService;
//import com.cmcc.ccs.RCSServiceListener;
import com.mediatek.common.mom.MobileManagerUtils;
import com.mediatek.common.mom.SubPermissions;
import com.mediatek.rcs.contacts.profileservice.IProfileServiceManager;
import com.mediatek.rcs.contacts.profileservice.parcel.ProfileInfo;
import com.mediatek.rcs.contacts.profileservice.IProfileObserver;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gsma.joyn.JoynService;
import org.gsma.joyn.JoynServiceListener;


public class ProfileService extends JoynService {

    private final static String TAG = "ProfileService";
    private final static String ACTION_BIND_PROFILESERVICE =
            "com.mediatek.rcs.contacts.ACTION_BIND_PROFILESERVICE";

    private static final String PROVIDER_AUTHORITY = "com.cmcc.ccs.profile";
    private static final Uri CONTENT_URI = Uri.parse("content://" + PROVIDER_AUTHORITY);

    private static final int OP_GET_PROFILE = 1;
    private static final int OP_SET_PROFILE = 2;
    private static final int OP_SET_PORTRAIT = 3;
    private static final int OP_GET_CONTACT_PORTRAIT = 4;
    private static final int OP_GET_QRCODE = 5;
    private static final int OP_SET_QRCODE_MODE = 6;
    private static final int OP_GET_QRCODE_MODE = 7;

    public static final int NOUPDATE = 1;
    public static final int PART_OK = 1;
    public static final int OK = 0;
    public static final int TIMEOUT = -1;
    public static final int UNKNOW = -2;
    public static final int UNAUTHORIZED = -3;
    public static final int FORBIDEN = -4;
    public static final int NOTFOUND = -5;
    public static final int INTERNEL_ERROR = -6;

    public static final String PHONE_NUMBER = "PROFILE_PHONENUMBER";
    public static final String FIRST_NAME = "PROFILE_FIRST_NAME";
    public static final String LAST_NAME = "PROFILE_LAST_NAME";
    public static final String PORTRAIT = "PROFILE_PORTRAIT";
    public static final String PORTRAIT_TYPE = "PROFILE_PORTRAIT_TYPE";
    public static final String ADDRESS = "PROFILE_ADDRESS";
    public static final String PHONE_NUMBER_SECOND = "PROFILE_PHONE_NUMBER_SECOND";
    public static final String EMAIL = "PROFILE_EMAIL";
    public static final String BIRTHDAY = "PROFILE_BIRTHDAY";
    public static final String COMPANY = "PROFILE_COMPANY";
    public static final String COMPANY_TEL = "PROFILE_COMPANY_TEL";
    public static final String TITLE = "PROFILE_TITLE";
    public static final String COMPANY_ADDR = "PROFILE_COMPANY_ADDR";
    public static final String COMPANY_FAX = "PROFILE_COMPANY_FAX";

    public static final String HOME1 = "PROFILE_HOME1";
    public static final String HOME2 = "PROFILE_HOME2";
    public static final String HOME3 = "PROFILE_HOME3";
    public static final String HOME4 = "PROFILE_HOME4";
    public static final String HOME5 = "PROFILE_HOME5";
    public static final String HOME6 = "PROFILE_HOME6";
    public static final String WORK1 = "PROFILE_WORK1";
    public static final String WORK2 = "PROFILE_WORK2";
    public static final String WORK3 = "PROFILE_WORK3";
    public static final String WORK4 = "PROFILE_WORK4";
    public static final String WORK5 = "PROFILE_WORK5";
    public static final String WORK6 = "PROFILE_WORK6";
    public static final String OTHER1 = "PROFILE_OTHER1";
    public static final String OTHER2 = "PROFILE_OTHER2";
    public static final String OTHER3 = "PROFILE_OTHER3";
    public static final String OTHER4 = "PROFILE_OTHER4";
    public static final String OTHER5 = "PROFILE_OTHER5";
    public static final String OTHER6 = "PROFILE_OTHER6";

    public static final String JPEG = "JPEG";
    public static final String BMP = "BMP";
    public static final String PNG = "PNG";
    public static final String GIF = "GIF";

    private static final int NOTIFY_TYPE_GET_PROFILE = 0;
    private static final int NOTIFY_TYPE_GET_PROFILE_PORTRAIT = 1;
    private static final int NOTIFY_TYPE_SET_PROFILE = 2;
    private static final int NOTIFY_TYPE_SET_PROFILE_PORTRAIT = 3;
    private static final int NOTIFY_TYPE_GET_CONTACT_PORTRAIT = 4;
    private static final int NOTIFY_TYPE_GET_QRCODE = 5;
    private static final int NOTIFY_TYPE_SET_QRCODE_MODE = 6;
    private static final int NOTIFY_TYPE_GET_QRCODE_MODE = 7;

    //public Context mContext = null;
    //private RCSServiceListener mServiceListener;
    private IProfileServiceManager mPSM = null;
    private static List<ProfileListener> mListeners = new ArrayList<ProfileListener>();
    private ProfileServiceHandler mProfileServiceHandler = null;
    private HandlerThread mHandlerThread = null;
    private ContentResolver mResolver = null;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            mPSM = IProfileServiceManager.Stub.asInterface(service);
            try {
                mPSM.registerObserver(mProfileObserver);
                serviceListener.onServiceConnected();
            } catch (RemoteException re) {
                re.printStackTrace();
                serviceListener.onServiceDisconnected(0);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "onServiceDisconnected");
            serviceListener.onServiceDisconnected(0);
            mPSM = null;
        }
    };

    private IProfileObserver mProfileObserver = new IProfileObserver.Stub() {
        @Override
        public void onUpdateProfile(int result) {
            Log.d(TAG, "IProfileObserver onUpdateProfile");
            notifyProfileListener(NOTIFY_TYPE_SET_PROFILE, result);
        }

        @Override
        public void onUpdateProfilePortrait(int result) {
            Log.d(TAG, "IProfileObserver onUpdateProfile");
            notifyProfileListener(NOTIFY_TYPE_SET_PROFILE_PORTRAIT, result);
        }

        @Override
        public void onGetProfile(int result) {
            Log.d(TAG, "IProfileObserver onGetProfile");
            notifyProfileListener(NOTIFY_TYPE_GET_PROFILE, result);
        }

        @Override
        public void onGetProfilePortrait(int result) {
            Log.d(TAG, "IProfileObserver onGetProfile");
            notifyProfileListener(NOTIFY_TYPE_GET_PROFILE_PORTRAIT, result);
        }

        @Override
        public void onGetContactPortrait(int result, String portrait,
                String number, String mimeType){
            Log.d(TAG, "IProfileObserver onGetContactPortrait");
            notifyPortraitListener(result, portrait, number, mimeType);
        }

        @Override
        public void onGetQRCode(int result, int mode){
            Log.d(TAG, "IProfileObserver onGetProfileQRCode");
            notifyQRCodeListener(NOTIFY_TYPE_GET_QRCODE, result, mode);
        }

        @Override
        public void onUpdateQRCodeMode(int result, int mode){
            Log.d(TAG, "IProfileObserver onGetProfileQRCode");
            notifyQRCodeListener(NOTIFY_TYPE_SET_QRCODE_MODE, result, mode);
        }

        @Override
        public void onGetQRCodeMode(int result, int mode){
            Log.d(TAG, "IProfileObserver onGetProfileQRCode");
            notifyQRCodeListener(NOTIFY_TYPE_GET_QRCODE_MODE, result, mode);
        }
    };

    public ProfileService(Context context, JoynServiceListener listener){
        super(context, listener);
        Log.d(TAG, "constructor ProfileService");
        //mContext = ctx;
        //mServiceListener = listener;
        mResolver = ctx.getContentResolver();
        mHandlerThread = new HandlerThread("ProfileServiceThread");
        mHandlerThread.start();
        mProfileServiceHandler = new ProfileServiceHandler(mHandlerThread.getLooper());
    }

    private final class ProfileServiceHandler extends Handler {

        public ProfileServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage (Message Msg) {
            if(null == Msg)
            {
                Log.d(TAG, "ServiceHandler handleMessage msg is null");
                return;
            }
            Log.d(TAG, "ServiceHandler handleMessage msg is: " + Msg.what);

            switch (Msg.what) {
                case OP_GET_PROFILE:
                    // get profile
                    handleGetProfile();
                    break;

                case OP_SET_PROFILE:
                    // get profile
                    handleSetProfile((HashMap)Msg.obj);
                    break;

                 case OP_SET_PORTRAIT:
                     // get profile
                     handleSetPortrait((String)Msg.obj);
                     break;


                 case OP_GET_CONTACT_PORTRAIT:
                     // get profile
                     handleGetContactPortrait((String)Msg.obj);
                     break;

                case OP_GET_QRCODE:
                    handleGetQRCode();
                    break;

                case OP_SET_QRCODE_MODE:
                    handleSetQRCodeMode((int)Msg.obj);
                    break;

                case OP_GET_QRCODE_MODE:
                    handleGetQRCodeMode();
                    break;

                 default:
                    break;
            }
        }
    }

    private void handleGetProfile () {
        Log.d(TAG, "handleGetProfile");
        try {
           mPSM.getProfileInfo();
        } catch (RemoteException re) {
            re.printStackTrace();
        }
    }

    private void handleSetProfile (HashMap profile) {
        Log.d(TAG, "handleSetProfile");
        //saveProfileToDB(profile); //need confirm
        //param need implement Parcelable
        ProfileInfo profileInfo = new ProfileInfo();
        profileInfo.setProfileMap(profile);
        try {
           mPSM.updateProfileInfo(profileInfo);
        } catch (RemoteException re) {
            re.printStackTrace();
        }
    }

    private void handleSetPortrait (String fileName) {
        Log.d(TAG, "handleSetPortrait");
        try {
           mPSM.updateProfilePortrait(fileName);
        } catch (RemoteException re) {
            re.printStackTrace();
        }
    }


    private void handleGetContactPortrait (String number) {
        Log.d(TAG, "handleGetContactPortrait, number: " + number);
        try {
           mPSM.getContactPortrait(number);
        } catch (RemoteException re) {
            re.printStackTrace();
        }
    }

    private void handleGetQRCode () {
        Log.d(TAG, "handleGetQRCode");
        try {
           mPSM.getQRCode();
        } catch (RemoteException re) {
            re.printStackTrace();
        }
    }

    private void handleSetQRCodeMode (int mode) {
        Log.d(TAG, "handleSetQRCodeMode");
        try {
           mPSM.setQRCodeMode(mode);
        } catch (RemoteException re) {
            re.printStackTrace();
        }
    }

    private void handleGetQRCodeMode () {
        Log.d(TAG, "handleGetQRCodeMode");
        try {
           mPSM.getQRCodeMode();
        } catch (RemoteException re) {
            re.printStackTrace();
        }
    }

    public void connect(){
        Log.d(TAG, "connect");
        if (mPSM == null) {
            try {
                //Intent intent = new Intent(mContext, ProfileServiceManager.class);
                /*Intent intent = new Intent().setComponent(new ComponentName(
                        "com.mediatek.rcs.contacts.profileservice",
                        "com.mediatek.rcs.contacts.profileservice.ProfileServiceManager"));
                        */
                /*or the follow method*/
                //for L change
                Intent intent = new Intent(ACTION_BIND_PROFILESERVICE);
                intent.setClassName("com.mediatek.rcs.contacts",
                        "com.mediatek.rcs.contacts.profileservice.ProfileServiceManager");
                //Intent intent = new Intent(IProfileServiceManager.class.getName());
                ctx.bindService(intent, mConnection, ctx.BIND_AUTO_CREATE) ;            
            } catch (Exception e) {
                Log.d(TAG, "error in connect");
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "error: mPSM already exist");
            serviceListener.onServiceConnected();
        }
    }

    public void disconnect(){
        //disconnect from remote server
        Log.d(TAG, "disconnect");
        synchronized (mConnection) {
            if (mPSM != null) {
                try {
                    mPSM.unRegisterObserver(mProfileObserver);
                    ctx.unbindService(mConnection);
                } catch (RemoteException re) {
                    Log.d(TAG, "error in unRegisterObserver");
                    re.printStackTrace();
                } catch (Exception e) {
                    Log.d(TAG, "error in disconnect");
                    e.printStackTrace();
                }
            }
        }
    }

    public void getProfileInfo () {
        Log.d(TAG, "getProfileInfo");
        Message msg = mProfileServiceHandler.obtainMessage(OP_GET_PROFILE);
        msg.sendToTarget();
    }

    public void updateProfileInfo (HashMap Profile) {
        Log.d(TAG, "updateProfileInfo");
        if (MobileManagerUtils.isSupported()) {
            if (false == MobileManagerUtils.checkPermission(
                    SubPermissions.RCS_SET_PROFILE_INFO, Binder.getCallingUid())) {
                Log.d(TAG, "User denied updateProfileInfo");
                return;
            }
        }
        Message msg = mProfileServiceHandler.obtainMessage(OP_SET_PROFILE);
        msg.obj = Profile;
        msg.sendToTarget();
    }

    public void updateProfilePortrait (String filename) {
        Log.d(TAG, "updateProfilePortrait, filename: " + filename);
        if (MobileManagerUtils.isSupported()) {
            if (false == MobileManagerUtils.checkPermission(
                    SubPermissions.RCS_SET_PROFILE_INFO, Binder.getCallingUid())) {
                Log.d(TAG, "User denied updateProfileInfo");
                return;
            }
        }
        Message msg = mProfileServiceHandler.obtainMessage(OP_SET_PORTRAIT);
        msg.obj = filename;
        msg.sendToTarget();
    }


    /*we keep listener in client process, don't send them to remote service process*/
    public void addProfileListener(ProfileListener listener) {
        Log.d(TAG, "addProfileListener");
        mListeners.add(listener);
    }

    public void removeProfileListener(ProfileListener listener) {
        Log.d(TAG, "removeProfileListener");
        mListeners.remove(listener);
    }

    /*for internal use*/
    public void getContactPortrait (String number) {
        Log.d(TAG, "getContactPortrait");
        Message msg = mProfileServiceHandler.obtainMessage(OP_GET_CONTACT_PORTRAIT);
        msg.obj = number;
        msg.sendToTarget();
    }

    public void getProfileQRCode () {
        Log.d(TAG, "getProfileQRCode");
        Message msg = mProfileServiceHandler.obtainMessage(OP_GET_QRCODE);
        msg.sendToTarget();
    }

    public void setProfileQRCodeMode (int mode) {
        Log.d(TAG, "setProfileQRCodeMode, mode: " + mode);
        Message msg = mProfileServiceHandler.obtainMessage(OP_SET_QRCODE_MODE);
        msg.obj = mode;
        msg.sendToTarget();
    }

    public void getProfileQRCodeMode () {
        Log.d(TAG, "getProfileQRCodeMode");
        Message msg = mProfileServiceHandler.obtainMessage(OP_GET_QRCODE_MODE);
        msg.sendToTarget();
    }

    private boolean saveProfileToDB (HashMap profile) {
        Log.d(TAG, "saveProfileToDB");
        ContentValues values = new ContentValues();
        String key = null;
        String value = null;
        Iterator iter = profile.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            key = (String)entry.getKey();
            value = (String)entry.getValue();
            values.put(key, value);
        }
        int count = mResolver.update(CONTENT_URI, values, null, null);
        Log.d(TAG, "count is " + count);
        return count > 0;
    }

    private void notifyProfileListener(int notifyType, int result){
        Log.d(TAG, "notifyListener, type: " + notifyType + " result: " + result);
        for (int i = 0; i < mListeners.size(); i++){
            switch (notifyType) {
                case NOTIFY_TYPE_GET_PROFILE:
                    mListeners.get(i).onGetProfile(result);
                    break;
                case NOTIFY_TYPE_GET_PROFILE_PORTRAIT:
                    mListeners.get(i).onGetProfilePortrait(result);
                    break;
                case NOTIFY_TYPE_SET_PROFILE:
                    mListeners.get(i).onUpdateProfile(result);
                    break;
                case NOTIFY_TYPE_SET_PROFILE_PORTRAIT:
                    mListeners.get(i).onUpdateProfilePortrait(result);
                    break;
                default:
                    //error
                    break;
            }
        }
    }

    private void notifyPortraitListener(int result, String portrait,
            String number, String mimeType){
        Log.d(TAG, "notifyPortraitListener, result: " + result);
        for (int i=0; i < mListeners.size(); i++){
                mListeners.get(i).onGetContactPortrait(result, portrait, number, mimeType);
        }
    }

    private void notifyQRCodeListener(int notifyType, int result, int mode){
        Log.d(TAG, "notifyQRCodeListener, result: " + result + " mode: " + mode +
            " notifyType: " + notifyType);
        switch (notifyType) {
            case NOTIFY_TYPE_GET_QRCODE:
                for (int i=0; i < mListeners.size(); i++){
                        mListeners.get(i).onGetProfileQRCode(result, mode);
                }
                break;

            case NOTIFY_TYPE_SET_QRCODE_MODE:
                for (int i=0; i < mListeners.size(); i++){
                        mListeners.get(i).onUpdateProfileQRCodeMode(result, mode);
                }
                break;

            case NOTIFY_TYPE_GET_QRCODE_MODE:
                for (int i=0; i < mListeners.size(); i++){
                        mListeners.get(i).onGetProfileQRCodeMode(result, mode);
                }
                break;

            default:
                Log.d(TAG, "error: unexpected notifyType");
                break;
        }
    }
}
