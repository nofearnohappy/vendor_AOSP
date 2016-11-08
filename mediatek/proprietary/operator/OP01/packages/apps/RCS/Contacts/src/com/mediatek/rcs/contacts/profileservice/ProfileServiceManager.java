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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
//import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Base64;

//import com.mediatek.gba.GbaCredentials;
import com.mediatek.rcs.contacts.profileservice.parcel.ProfileInfo;
import com.mediatek.rcs.contacts.profileservice.profileservs.Address;
import com.mediatek.rcs.contacts.profileservice.profileservs.Birth;
import com.mediatek.rcs.contacts.profileservice.profileservs.Career;
import com.mediatek.rcs.contacts.profileservice.profileservs.CommAddr;
import com.mediatek.rcs.contacts.profileservice.profileservs.Name;
import com.mediatek.rcs.contacts.profileservice.profileservs.params.AddressParams;
import com.mediatek.rcs.contacts.profileservice.profileservs.params.BirthParams;
import com.mediatek.rcs.contacts.profileservice.profileservs.params.CareerParams;
import com.mediatek.rcs.contacts.profileservice.profileservs.params.CommAddrParams;
import com.mediatek.rcs.contacts.profileservice.profileservs.params.EmailParams;
import com.mediatek.rcs.contacts.profileservice.profileservs.params.NameParams;
import com.mediatek.rcs.contacts.profileservice.profileservs.params.PortraitParams;
import com.mediatek.rcs.contacts.profileservice.profileservs.params.TelNumParams;
import com.mediatek.rcs.contacts.profileservice.profileservs.Pcc;
import com.mediatek.rcs.contacts.profileservice.profileservs.PccParams;
import com.mediatek.rcs.contacts.profileservice.profileservs.Portrait;
import com.mediatek.rcs.contacts.profileservice.profileservs.ProfileServs;
import com.mediatek.rcs.contacts.profileservice.profileservs.QRCode;
import com.mediatek.rcs.contacts.profileservice.profileservs.xcap.ProfileXcapElement;
import com.mediatek.rcs.contacts.profileservice.profileservs.xcap.ProfileXcapException;
import com.mediatek.rcs.contacts.profileservice.utils.ProfileConstants;
import com.mediatek.rcs.contacts.profileservice.utils.ProfileServiceLog;

//import java.net.URI;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gsma.joyn.JoynServiceConfiguration;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 * class ProfileServiceManager.
 */
public class ProfileServiceManager extends Service {
    private final static String TAG = "ProfileServiceManager";
    private final static String ACTION_BIND_PROFILESERVICE =
                                "com.mediatek.rcs.contacts.ACTION_BIND_PROFILESERVICE";
    private final static String ACTION_RCS_ON = "com.mediatek.intent.rcs.stack.LaunchService";
    private final static String ACTION_RCS_OFF = "com.mediatek.intent.rcs.stack.StopService";
    //user' phone number
    //String mXui = "tel:+8613810307094";
    //remove http for apache add auto, setXcapRoot
    //String mXcapRoot = "http://122.70.137.46:8182/services/";
    String mXcapRoot;
    //String mXIntendedId = "tel:+8613810307094";
    String mXIntendedId;
    String mXui;
    String mPhoneNumber;
    JoynServiceConfiguration mJSConfig = new JoynServiceConfiguration();
    private static String mUserName = "sip:user@anritsu-cscf.com";
    private static String mPassword = "password";
    private Context mContext;
    private List mOtherNumberList;
    private ContentResolver mResolver = null;
    private MyHandler mWorkHandler = null;
    private HandlerThread mWorkThread = null;
    //private boolean hasRetry = false;
    //private static final int retryTimeout = (10*1000);
    private static final int OP_GET_PROFILE = 1;
    private static final int OP_SET_PROFILE = 2;
    private static final int OP_SET_PROFILE_PORTRAIT = 3;
    private static final int OP_GET_CONTACT_PORTRAIT = 4;
    private static final int OP_SAVE_PHONENUMBER_TO_DB = 5;
    private static final int OP_GET_QRCODE = 6;
    private static final int OP_SET_QRCODE_MODE = 7;
    private static final int OP_GET_QRCODE_MODE = 8;
    private static final int OP_STOP_SERVICE = 9;
    private static final int RSP_SET_PROFILE = 5;
    private static final int RETRY_TIMEOUT = 5;

    private static final int PORTRAIT_MAX_SIZE = 300*1024;

    private static Authenticator sAuthenticator = new HttpAuthenticator();

    private final RemoteCallbackList<IProfileObserver> mObsevers =
        new RemoteCallbackList<IProfileObserver>();
    private static final ProfileServs mProfileservs = ProfileServs.getInstance();
    private IProfileServiceManager.Stub mBinder = new IProfileServiceManager.Stub() {
        @Override
        public void getProfileInfo() {
            ProfileServiceManager.this.getProfile();
        }

        @Override
        public void updateProfileInfo(ProfileInfo profile) {
            ProfileServiceManager.this.setProfile(profile);
        }

        @Override
        public void updateProfilePortrait(String filename) {
            ProfileServiceManager.this.setProfilePortrait(filename);
        }

        @Override
        public void getContactPortrait(String number) {
            ProfileServiceManager.this.getContactPortrait(number);
        }

        @Override
        public void getQRCode() {
            ProfileServiceManager.this.getQRCode();
        }

        @Override
        public void setQRCodeMode(int mode) {
            ProfileServiceManager.this.setQRCodeMode(mode);
        }

        @Override
        public void getQRCodeMode() {
            ProfileServiceManager.this.getQRCodeMode();
        }

        @Override
        public void registerObserver(IProfileObserver observer) {
            mObsevers.register(observer);
        }

        @Override
        public void unRegisterObserver(IProfileObserver observer) {
            mObsevers.unregister(observer);
        }
    };

    /**
     * onCreate.
     */
    @Override
    public void onCreate(){
        super.onCreate();
        ProfileServiceLog.d(TAG, "onCreate");
        mContext = getApplicationContext();
        if (mContext == null) {
            ProfileServiceLog.d(TAG, "error: mContext is null");
        }
        mResolver = mContext.getContentResolver();
        mWorkThread = new HandlerThread("ProfileServiceWorkThread");
        mWorkThread.start();
        mWorkHandler = new MyHandler(mWorkThread.getLooper());

        String serverAddress = getServerAddress();
        if (!serverAddress.endsWith("/")) {
            mXcapRoot = serverAddress + "/";
        } else {
            mXcapRoot = serverAddress;
        }

        ProfileXcapElement.mHostName = getHostNameFromServerAddress(serverAddress);

        String sip = getSip();
        if (sip == null || sip.length() <= 0) {
            ProfileServiceLog.d(TAG, "no sip, RCS is not registered");
        } else {
            //mXIntendedId = getXIntentIdFromSip(sip);
            mPhoneNumber = getPhoneNumberFromSip(sip);
            if (mPhoneNumber != null) {
                mXIntendedId = "tel:" + mPhoneNumber;
                ProfileServiceLog.d(TAG, "sende msg to save mPhoneNumber to db");
                Message msg = mWorkHandler.obtainMessage(OP_SAVE_PHONENUMBER_TO_DB);
                msg.sendToTarget();
            }
        }
        //setProfileservsInitParameters(mXui, mXcapRoot, mXIntendedId, mUserName, mPassword);
        //IntentFilter intentFilter = new IntentFilter(ACTION_RCS_OFF);
        //intentFilter.addAction(ACTION_RCS_OFF);
        //mContext.registerReceiver(mProfileServiceReceiver, intentFilter);

        System.setProperty("http.digest.support", "true");
        Authenticator.setDefault(sAuthenticator);
    }

    /**
     * onBind.
     */
    @Override
    public IBinder onBind(Intent intent){
    	String action = intent.getAction();
    	if(action.equals(ACTION_BIND_PROFILESERVICE)) {
            return mBinder;
        } else {
            ProfileServiceLog.d(TAG, "error onBind, wrong intent action");
        }
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ProfileServiceLog.d(TAG, "onDestroy");
        //unregisterReceiver(mProfileServiceReceiver);
        if (mWorkThread != null) {
            mWorkThread.quit();
        }
    }

    private final BroadcastReceiver mProfileServiceReceiver = new BroadcastReceiver () {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            ProfileServiceLog.d(TAG, "mProfileServiceReceiver, action: " + action);
            if (action.equals(ACTION_RCS_OFF)) {
                //Message msg = new Message(OP_STOP_SERVICE);
                //mWorkHandler.sendMessageAtFrontOfQueue(msg);
                ProfileServiceManager.this.stopSelf();
            } else if (action.equals(ACTION_RCS_ON)) {

            } else {

            }
        }
    };

    private final class MyHandler extends Handler {

        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage (Message Msg) {
            ProfileServiceLog.d(TAG, "MyHandler handleMessage, msg:" + Msg.what);
            int result = ProfileConstants.RES_OK;
            if(null == Msg)
            {
                ProfileServiceLog.d(TAG, "msg is null");
                return;
            }
            switch (Msg.what) {
                case OP_GET_PROFILE:
                    if (mXIntendedId == null || mXIntendedId.length() <= 0) {
                        ProfileServiceLog.d(TAG, "no mXIntendedId to get profile");
                        String sip = getSip();
                        if (sip == null || sip.length() <= 0) {
                            ProfileServiceLog.d(TAG, "still no sip, RCS is not registered");
                            result = ProfileConstants.RES_UNAUTHORIZED;
                            notifyGetDone(false, result);
                        } else {
                            mPhoneNumber = getPhoneNumberFromSip(sip);
                            if (mPhoneNumber != null) {
                                //phoneNumber2IntentId(mPhoneNumber);
                                mXIntendedId = "tel:" + mPhoneNumber;
                                result = handleGetProfile(true);
                                notifyGetDone(false, result);
                                result = handleGetProtrait();
                                notifyGetDone(true, result);
                            } else {
                                ProfileServiceLog.d(TAG, "mPhoneNumber is null, sip format error");
                                result = ProfileConstants.RES_UNAUTHORIZED;
                                notifyGetDone(false, result);
                            }
                        }
                    } else {
                        result = handleGetProfile(false);
                        notifyGetDone(false, result);
                        result = handleGetProtrait();
                        notifyGetDone(true, result);
                    }
                    break;

                case OP_SET_PROFILE:
                    if (mXIntendedId == null || mXIntendedId.length() <= 0) {
                        ProfileServiceLog.d(TAG, "no mXIntendedId to set profile");
                        String sip = getSip();
                        if (sip == null || sip.length() <= 0) {
                            ProfileServiceLog.d(TAG, "still no sip, RCS is not registered");
                            result = ProfileConstants.RES_UNAUTHORIZED;
                            notifySetDone(false, result);
                        } else {
                            mPhoneNumber = getPhoneNumberFromSip(sip);
                            if (mPhoneNumber != null) {
                                //phoneNumber2IntentId(mPhoneNumber);
                                mXIntendedId = "tel:" + mPhoneNumber;
                                ProfileServiceLog.d(TAG, "get new mXIntendedId");
                                result = handleSetProfile((HashMap)Msg.obj, true);
                                notifySetDone(false, result);
                            } else {
                                ProfileServiceLog.d(TAG, "mPhoneNumber is null, sip format error");
                                result = ProfileConstants.RES_UNAUTHORIZED;
                                notifySetDone(false, result);
                            }
                        }
                    } else {
                        result = handleSetProfile((HashMap)Msg.obj, false);
                        notifySetDone(false, result);
                    }
                    break;

                case OP_SET_PROFILE_PORTRAIT:
                    if (mXIntendedId == null || mXIntendedId.length() <= 0) {
                        ProfileServiceLog.d(TAG, "no mXIntendedId to set profile");
                        String sip = getSip();
                        if (sip == null || sip.length() <= 0) {
                            ProfileServiceLog.d(TAG, "still no sip, RCS is not registered");
                            result = ProfileConstants.RES_UNAUTHORIZED;
                            notifySetDone(true, result);
                        } else {
                            mPhoneNumber = getPhoneNumberFromSip(sip);
                            if (mPhoneNumber != null) {
                                //phoneNumber2IntentId(mPhoneNumber);
                                mXIntendedId = "tel:" + mPhoneNumber;
                                ProfileServiceLog.d(TAG, "get new mXIntendedId");
                                result = handleSetProfilePortrait((String)Msg.obj, true);
                                notifySetDone(true, result);
                            } else {
                                ProfileServiceLog.d(TAG, "mPhoneNumber is null, sip format error");
                                result = ProfileConstants.RES_UNAUTHORIZED;
                                notifySetDone(true, result);
                            }
                        }
                    } else {
                        result = handleSetProfilePortrait((String)Msg.obj, false);
                        notifySetDone(true, result);
                    }
                    break;

                case OP_GET_CONTACT_PORTRAIT:
                    handleGetContactPortrait((String)Msg.obj);
                    break;

                case OP_SAVE_PHONENUMBER_TO_DB:
                    if (mPhoneNumber != null) {
                        savePhoneNumber();
                    } else {
                        ProfileServiceLog.d(TAG, "error: mPhoneNumber is null");
                    }
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

                case OP_STOP_SERVICE:

                    break;

                 default:
                    break;
            }
        }
    }

    private void getProfile() {
        ProfileServiceLog.d(TAG, "getProfile");
        Message msg = mWorkHandler.obtainMessage(OP_GET_PROFILE);
        msg.sendToTarget();
    }

    private int handleGetProfile(boolean saveNumber) {
        ProfileServiceLog.d(TAG, "handleGetProfile, saveNumber: " + saveNumber);
        int result = ProfileConstants.RES_OK;
        ContentValues values = null;
        mXui = mXIntendedId;

        if (saveNumber) {
            values = new ContentValues();
        }
        setProfileservsInitParameters(mXui, mXcapRoot, mXIntendedId, mUserName, mPassword);
        //parser the pcc.xml
        String[] projection = {ProfileConstants.PCC_ETAG};
        Cursor cursor = mResolver.query(ProfileConstants.CONTENT_URI, projection, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int pccETagIndex = cursor.getColumnIndex(ProfileConstants.PCC_ETAG);
            ProfileXcapElement.mPccETag = cursor.getString(pccETagIndex);
            cursor.close();
            if (ProfileXcapElement.mPccETag == null) {
                ProfileServiceLog.d(TAG, "mPccETag got from db is null");
                ProfileXcapElement.mPccETag = "";
            }
        } else {
            ProfileXcapElement.mPccETag = "";
        }
        try {
            mProfileservs.buildDocumentUri(ProfileConstants.CONTENT_TYPE_PCC);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        //getProfile only support get whole pcc.xml at present, so we must call getPccContent at first
        do {
            try {
                getPccContent();
            } catch (ProfileXcapException e) {
                ProfileServiceLog.d(TAG, "have exeption on getPccContent");
                result = handleException(e);
                if(result != ProfileConstants.RES_NOUPDATE){
                    break;
                }
            }
            if (values == null) {
                values = new ContentValues();
            }
            //get CommAddr content at on time
            if(result != ProfileConstants.RES_NOUPDATE) {
                getCommAddrContent(values);
                //get other pcc content
                String birthDate = getBirthInfo();
                Name nameInc = getNameInstance();
                String firstName = getFirstName(nameInc);
                String lastName = getLastName(nameInc);
                String homeAddress = getHomeAddress();
                String workAddress = getWorkAddress();
                String company = getCompany();
                String title = getTitle();
                ProfileServiceLog.d(TAG, "get profile content, "
                    + "\nbirth: " + birthDate
                    + "\nname: " + lastName + " " + firstName
                    + "\nhomeAddress: " + homeAddress
                    + "\nworkAddress: " + workAddress
                    + "\ncompany: " + company
                    + "\ntitle: " + title);
                values.put(ProfileConstants.FIRST_NAME, firstName);
                values.put(ProfileConstants.LAST_NAME, lastName);
                values.put(ProfileConstants.ADDRESS, homeAddress);
                values.put(ProfileConstants.BIRTHDAY, birthDate);
                values.put(ProfileConstants.COMPANY, company);
                values.put(ProfileConstants.TITLE, title);
                values.put(ProfileConstants.COMPANY_ADDR, workAddress);
                values.put(ProfileConstants.PCC_ETAG, ProfileXcapElement.mPccETag);
            } else {
                //pcc.xml no update
                ProfileServiceLog.d(TAG, "pcc not modified");
            }
        } while (false);
        //use do-while(0), to separate portrait from pcc,
        //so we can save part content to db
        //int valueSize = values.size();
        if (saveNumber) {
            ProfileServiceLog.d(TAG, "need to save number to db");
            values.put(ProfileConstants.PHONE_NUMBER, mPhoneNumber);
        }
        if (values != null && values.size() > 0) {
            ProfileServiceLog.d(TAG, "values.size is: " + values.size());
            if (!saveProfileToDB(values)) {
                ProfileServiceLog.d(TAG, "error in saveProfileToDB");
            }
        } else {
            ProfileServiceLog.d(TAG, "no value to set");
        }
        ProfileServiceLog.d(TAG, "handleGetProfile, result is: " + result);

        return result;
    }


    private int handleGetProtrait() {
        ProfileServiceLog.d(TAG, "handleGetProtrait");
        int result = ProfileConstants.RES_OK;
        Portrait portrait = null;
        String portraitData = null;
        String mimeType = null;
        String portraitType = null;
        ContentValues values = null;

        mXui = mXIntendedId;
        setProfileservsInitParameters(mXui, mXcapRoot, mXIntendedId, mUserName, mPassword);

        String[] projection = {ProfileConstants.PORTRAIT_ETAG};
        Cursor cursor = mResolver.query(ProfileConstants.CONTENT_URI, projection, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int portraitETagIndex = cursor.getColumnIndex(ProfileConstants.PORTRAIT_ETAG);
            ProfileXcapElement.mPortraitETag = cursor.getString(portraitETagIndex);
            cursor.close();
            if (ProfileXcapElement.mPortraitETag == null) {
                ProfileServiceLog.d(TAG, "mPortraitETag got from db is null");
                ProfileXcapElement.mPortraitETag = "";
            }
        } else {
            ProfileXcapElement.mPortraitETag = "";
        }

        values = new ContentValues();

        try {
            mProfileservs.buildDocumentUri(ProfileConstants.CONTENT_TYPE_PORTRAIT);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        try {
            portrait = mProfileservs.getPortraitInstance(true,
                null, ProfileConstants.CONTENT_TYPE_PORTRAIT);
            if (portrait == null) {
                ProfileServiceLog.d(TAG, "error: portrait is null");
                result = ProfileConstants.RES_INTERNEL_ERROR;
                ProfileServiceLog.d(TAG, "handleGetProtrait, result is: " + result);
                return result;
            }
            portraitData = portrait.getPortrait();
            mimeType = portrait.getMimeType();
            portraitType = mimeTypeToPortraitType(mimeType);
        } catch (ProfileXcapException e) {
            ProfileServiceLog.d(TAG, "have exeption on getPortraitData");
            result = handleException(e);
            if (result != ProfileConstants.RES_NOUPDATE) {
                ProfileServiceLog.d(TAG, "handleGetProtrait, result is: " + result);
                return result;
            }
        }
        //ProfileServiceLog.d(TAG, "portrait is long string: " + portraitData);
        if (result != ProfileConstants.RES_NOUPDATE) {
            //it must be "ok", for other result, it break before
            ProfileServiceLog.d(TAG, "put portrait and etag");
            values.put(ProfileConstants.PORTRAIT, portraitData);
            values.put(ProfileConstants.PORTRAIT_TYPE, portraitType);
            values.put(ProfileConstants.PORTRAIT_ETAG, ProfileXcapElement.mPortraitETag);
            /*the etag is static,  the only one, so clear the etag after every set operation
            for the next get operatrion, we get etag from db*/
            ProfileXcapElement.mPortraitETag = "";
        } else {
            //portrait is no update
        }

        if (values != null && values.size() > 0) {
            ProfileServiceLog.d(TAG, "values.size is: " + values.size());
            if (!saveProfileToDB(values)) {
                ProfileServiceLog.d(TAG, "error in saveProfileToDB");
            }
        } else {
            ProfileServiceLog.d(TAG, "no value to set");
        }
        ProfileServiceLog.d(TAG, "handleGetProtrait, result is: " + result);

        return result;
    }


    private void setProfile(ProfileInfo profile) {
        ProfileServiceLog.d(TAG, "setProfile");
        HashMap profileMap = profile.mProfileMap;
        Message msg = mWorkHandler.obtainMessage(OP_SET_PROFILE);
        msg.obj = profileMap;
        msg.sendToTarget();
    }

    private void setProfilePortrait(String filename) {
        ProfileServiceLog.d(TAG, "setProfilePortrait, filename: " + filename);
        String fileName = filename;
        Message msg = mWorkHandler.obtainMessage(OP_SET_PROFILE_PORTRAIT);
        msg.obj = fileName;
        msg.sendToTarget();
    }

    private int handleSetProfile(HashMap profile, boolean saveNumber) {
        ProfileServiceLog.d(TAG, "handleSetProfile");
        //saveProfileToDB(profile);
        ContentValues values = new ContentValues();
        int result = ProfileConstants.RES_OK;
        mXui = mXIntendedId;
        setProfileservsInitParameters(mXui, mXcapRoot, mXIntendedId, mUserName, mPassword);
        try {
            mProfileservs.buildDocumentUri(ProfileConstants.CONTENT_TYPE_PART);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        String key = null;
        String value = null;
        String portraitData = null;
        String portraitType = null;
        boolean hasPortrait = false;
        String firstName = null;
        String lastName = null;
        Iterator iter = profile.entrySet().iterator();
        do {
            try {
                while (iter.hasNext()) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    key = (String)entry.getKey();
                    value = (String)entry.getValue();
                    if (key.equals(ProfileConstants.PHONE_NUMBER)) {
                        continue; //for server not support mobilenumber at present
                        //setMobileNumber(value);
                        //values.put(key, value);
                        //continue;
                    } else if (key.equals(ProfileConstants.FIRST_NAME)) {
                        if (lastName == null) {
                            firstName = value;
                        } else {
                            setName(value, lastName);
                            values.put(key, value);
                            values.put(ProfileConstants.LAST_NAME, lastName);
                        }
                        //setName(value, value);
                        continue;
                    } else if (key.equals(ProfileConstants.LAST_NAME)) {
                        if (firstName == null) {
                            lastName = value;
                        } else {
                            setName(firstName, value);
                            values.put(ProfileConstants.FIRST_NAME, firstName);
                            values.put(key, value);
                        }
                        //setName(value, value);
                        continue;
                    } else if (key.equals(ProfileConstants.PORTRAIT)) {
                        portraitData = value;
                        hasPortrait = true;
                        continue;
                    } else if (key.equals(ProfileConstants.PORTRAIT_TYPE)) {
                        portraitType = value;
                        continue;
                    } else if (key.equals(ProfileConstants.ADDRESS)) {
                        setHomeAddress(value);
                        values.put(key, value);
                        continue;
                    } else if (key.equals(ProfileConstants.PHONE_NUMBER_SECOND)) {
                        setOtherNumber(value);
                        values.put(key, value);
                        continue;
                    } else if (key.equals(ProfileConstants.EMAIL)) {
                        setEmail(value);
                        values.put(key, value);
                        continue;
                    } else if (key.equals(ProfileConstants.BIRTHDAY)) {
                        setBirth(value);
                        values.put(key, value);
                        continue;
                    } else if (key.equals(ProfileConstants.COMPANY)) {
                        setCompany(value);
                        values.put(key, value);
                        continue;
                    } else if (key.equals(ProfileConstants.COMPANY_TEL)) {
                        setOfficeNumber(value);
                        values.put(key, value);
                        continue;
                    } else if (key.equals(ProfileConstants.TITLE)) {
                        setDutyTitle(value);
                        values.put(key, value);
                        continue;
                    } else if (key.equals(ProfileConstants.COMPANY_ADDR)) {
                        setWorkAddress(value);
                        values.put(key, value);
                        continue;
                    } else if (key.equals(ProfileConstants.COMPANY_FAX)) {
                        setCompanyFax(value);
                        values.put(key, value);
                        continue;
                    } else {
                        ProfileServiceLog.d(TAG, "not profileinfo, is " + key);
                        continue;
                    }
                }
            }catch (ProfileXcapException e){
                ProfileServiceLog.d(TAG, "have ProfileXcapException on set " + key);
                e.printStackTrace();
                result = handleException(e);
                break;
            }
        }while(false);

        if(values.size() > 0) {
            values.put(ProfileConstants.PCC_ETAG, ProfileXcapElement.mPccETag);
            /*for the etag is static,  clear the etag after every set operation
            for the next get operatrion, we get etag from db*/
            ProfileXcapElement.mPccETag = "";
        }
        //set portrait
        if (hasPortrait) {
            ProfileServiceLog.d(TAG, "hasPortrait is true, set Portrait");
            try {
                mProfileservs.buildDocumentUri(ProfileConstants.CONTENT_TYPE_PORTRAIT);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            try {
                if (portraitType == null) {
                    ProfileServiceLog.d(TAG, "error: portraitType is null");
                }
                if (portraitData != null && portraitData.length() > 0) {
                    setPortrait(portraitData, portraitType);
                    values.put(ProfileConstants.PORTRAIT, portraitData);
                    values.put(ProfileConstants.PORTRAIT_TYPE, portraitType);
                    values.put(ProfileConstants.PORTRAIT_ETAG, ProfileXcapElement.mPortraitETag);
                    /*for the etag is static,  clear the etag after every set operation
                    for the next get operatrion, we get etag from db*/
                    ProfileXcapElement.mPortraitETag = "";
                } else {
                    ProfileServiceLog.d(TAG, "no portraitData");
                }
            } catch (ProfileXcapException e) {
                ProfileServiceLog.d(TAG, "have exeption on set portrait");
                e.printStackTrace();
                result = handleException(e);
            }
        }

        if (saveNumber) {
            values.put(ProfileConstants.PHONE_NUMBER, mPhoneNumber);
        }
        if (values.size() > 0) {
            saveProfileToDB(values);
        }
        return result;
    }


    private int handleSetProfilePortrait(String fileName, boolean saveNumber) {
        ProfileServiceLog.d(TAG, "handleSetProfilePortrait, fileName: " + fileName);
        //saveProfileToDB(profile);
        int result = ProfileConstants.RES_OK;
        mXui = mXIntendedId;
        setProfileservsInitParameters(mXui, mXcapRoot, mXIntendedId, mUserName, mPassword);
        try {
            mProfileservs.buildDocumentUri(ProfileConstants.CONTENT_TYPE_PORTRAIT);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        ContentValues values = new ContentValues();
        String portraitData = null;
        String portraitType = null;

        Bitmap bm = BitmapFactory.decodeFile(fileName);
        portraitType = ProfileConstants.PNG;
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        int quality = 100;
        if (bm.getByteCount() > PORTRAIT_MAX_SIZE) {
            quality *= PORTRAIT_MAX_SIZE / bm.getByteCount();
        }
        bm.compress(Bitmap.CompressFormat.PNG, quality, byteStream);
        //we need to convert byte[] to string to save to db
        portraitData = Base64.encodeToString(byteStream.toByteArray(), Base64.DEFAULT);

        try {
            if (portraitData != null && portraitData.length() > 0) {
                setPortrait(portraitData, portraitType);
                values.put(ProfileConstants.PORTRAIT, portraitData);
                values.put(ProfileConstants.PORTRAIT_TYPE, portraitType);
                values.put(ProfileConstants.PORTRAIT_ETAG, ProfileXcapElement.mPortraitETag);
                /*for the etag is static,  clear the etag after every set operation
                for the next get operatrion, we get etag from db*/
                ProfileXcapElement.mPortraitETag = "";
            } else {
                ProfileServiceLog.d(TAG, "no portraitData");
                result = ProfileConstants.RES_INTERNEL_ERROR;
            }
        } catch (ProfileXcapException e) {
            ProfileServiceLog.d(TAG, "have exeption on set portrait");
            e.printStackTrace();
            result = handleException(e);
        }
        if (saveNumber) {
            values.put(ProfileConstants.PHONE_NUMBER, mPhoneNumber);
        }
        if (values.size() > 0) {
            saveProfileToDB(values);
        }
        return result;
    }

    private void getContactPortrait(String number){
        ProfileServiceLog.d(TAG, "getContactPortrait, number: " + number);
        Message msg = mWorkHandler.obtainMessage(OP_GET_CONTACT_PORTRAIT);
        msg.obj = number;
        msg.sendToTarget();
    }

    private void handleGetContactPortrait(String number) {
        ProfileServiceLog.d(TAG, "handleGetContactPortrait, number" + number);
        //String portrait = null;
        Portrait portrait = null;
        int result = ProfileConstants.RES_OK;
        String portraitData = null;
        String mimeType = null;
        String portraitType = null;

        if (mXIntendedId == null || mXIntendedId.length() <= 0) {
            ProfileServiceLog.d(TAG, "no mXIntendedId to get contact profile");
            String sip = getSip();
            if (sip == null || sip.length() <= 0) {
                ProfileServiceLog.d(TAG, "still no sip, RCS is not registered");
                result = ProfileConstants.RES_UNAUTHORIZED;
            } else {
                mPhoneNumber = getPhoneNumberFromSip(sip);
                if (mPhoneNumber != null) {
                    mXIntendedId = "tel:" + mPhoneNumber;
                    ProfileServiceLog.d(TAG,
                            "get new mXIntendedId, and send msg to save mPhoneNumber to db");
                    Message msg = mWorkHandler.obtainMessage(OP_SAVE_PHONENUMBER_TO_DB);
                    msg.sendToTarget();
                } else {
                    ProfileServiceLog.d(TAG, "mPhoneNumber is null, sip format error");
                }
            }
        }
        if (mXIntendedId != null && mXIntendedId.length() > 0) {
            setProfileservsInitParameters(mXui, mXcapRoot, mXIntendedId, mUserName, mPassword);
            try {
                String xui = null;
                if (number.startsWith("+86")) {
                    xui = "tel:" + number;
                } else {
                    xui = "tel:" + "+86" + number;
                }
                mProfileservs.setContactXui(xui);
                mProfileservs.buildDocumentUri(ProfileConstants.CONTENT_TYPE_CONTACT_PORTRAIT);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            try {
                //portrait = getContactPortraitData();
                portrait = mProfileservs.getPortraitInstance(true, null,
                        ProfileConstants.CONTENT_TYPE_CONTACT_PORTRAIT);
                if (portrait == null) {
                    ProfileServiceLog.d(TAG, "error: portrait is null");
                    result = ProfileConstants.RES_INTERNEL_ERROR;
                } else {
                    portraitData = portrait.getPortrait();
                    mimeType = portrait.getMimeType();
                    portraitType = mimeTypeToPortraitType(mimeType);
                }
            } catch (ProfileXcapException e) {
                ProfileServiceLog.d(TAG, "have exeption on getContactPortraitData or mimetype");
                result = handleException(e);
            }
        } else {
            result = ProfileConstants.RES_UNAUTHORIZED;
            ProfileServiceLog.d(TAG, "no mXIntendedId, GetContactPortrait fail");
        }

        final int length = mObsevers.beginBroadcast();
        for (int i = 0; i < length; i++) {
            try {
                mObsevers.getBroadcastItem(i).onGetContactPortrait(
                        result, portraitData, number, portraitType);
            } catch (RemoteException e) {
            } catch (RuntimeException e) {
            }
        }
        mObsevers.finishBroadcast();
    }

    //need to confirm for further use
    private int handleSetPcc() {
        int result = ProfileConstants.RES_OK;
        Pcc pcc = null;
        //mOtherNumberList = new HashMap<String, String>();
        parseOtherNumber("othernumber");
        try {
            CommAddr commAddr = InitCommAddr(0, "fax", "office", mOtherNumberList, "email");
            Name name = InitName("firstName", "familyName");
            Birth birth = InitBirth("birthDate");
            Address address = InitAddress(Address.ADDR_TYPE_ALL, "homeAddress", "workAddress");
            Career career = InitCareer("company", "dutytitle");
            pcc = InitPcc(commAddr, name, birth, address, career);
        } catch (ProfileXcapException e) {
            e.printStackTrace();
            result = handleException(e);
            return result;
        }
        try {
            pcc.setPccContent();
        } catch (ProfileXcapException e){
            e.printStackTrace();
            result = handleException(e);
            return result;
        }
        // need check exception, and get result
        return result;
    }

    private void getPccContent() throws ProfileXcapException {
        ProfileServiceLog.d(TAG, "getPccContent");
        //get the whole xml content form server, and make up document, save to mCurrDoc
        Pcc pcc = mProfileservs.getPccInstance(true, null);
    }

    private String getFaxNumber(){
        ProfileServiceLog.d(TAG, "getFaxNumber");
        CommAddr commAddr = null;
        try {
            commAddr = mProfileservs.getCommAddrInstance(true, null);
        } catch (ProfileXcapException e) {
            e.printStackTrace();
            ProfileServiceLog.d(TAG, "have exception on getCommAddrInstance");
        }
        if (commAddr == null) {
            ProfileServiceLog.d(TAG, "error: commAddr is null");
            return null;
        }
        String faxNumber = commAddr.getTelNumber(ProfileConstants.TEL_TYPE_FAX);
        return faxNumber;
    }

    private String getOfficeNumber(){
        ProfileServiceLog.d(TAG, "getOfficeNumber");
        CommAddr commAddr = null;
        try {
            commAddr = mProfileservs.getCommAddrInstance(true, null);
        } catch (ProfileXcapException e) {
            e.printStackTrace();
            ProfileServiceLog.d(TAG, "have exception on getCommAddrInstance");
        }
        if (commAddr == null) {
            ProfileServiceLog.d(TAG, "error: commAddr is null");
            return null;
        }
        String officeNumber = commAddr.getTelNumber(ProfileConstants.TEL_TYPE_WORK);
        return officeNumber;
    }

    private String getOtherNumber(){
        ProfileServiceLog.d(TAG, "getOtherNumber");
        CommAddr commAddr = null;
        try {
            commAddr = mProfileservs.getCommAddrInstance(true, null);
        } catch (ProfileXcapException e) {
            e.printStackTrace();
            ProfileServiceLog.d(TAG, "have exception on getCommAddrInstance");
        }
        if (commAddr == null) {
            ProfileServiceLog.d(TAG, "error: commAddr is null");
            return null;
        }
        String otherNumber = commAddr.getOtherNumber();
        return otherNumber;
    }

    private String getEmail(){
        ProfileServiceLog.d(TAG, "getEmail");
        CommAddr commAddr = null;
        try {
            commAddr = mProfileservs.getCommAddrInstance(true, null);
        } catch (ProfileXcapException e) {
            e.printStackTrace();
            ProfileServiceLog.d(TAG, "have exception on getCommAddrInstance");
        }
        if (commAddr == null) {
            ProfileServiceLog.d(TAG, "error: commAddr is null");
            return null;
        }
        String email = commAddr.getEmail();
        return email;
    }

    private void getCommAddrContent(ContentValues values){
        ProfileServiceLog.d(TAG, "getCommAddrContent");
        CommAddr commAddr = null;
        try {
            commAddr = mProfileservs.getCommAddrInstance(true, null);
        } catch (ProfileXcapException e) {
            e.printStackTrace();
            ProfileServiceLog.d(TAG, "have exception on getCommAddrInstance");
        }
        if (commAddr == null) {
            ProfileServiceLog.d(TAG, "error: commAddr is null");
            return;
        }
        String faxNumber = commAddr.getTelNumber("Fax");
        String officeNumber = commAddr.getTelNumber("Work");
        String otherNumber = commAddr.getOtherNumber();
        String email = commAddr.getEmail();

        values.put(ProfileConstants.COMPANY_TEL, officeNumber);
        values.put(ProfileConstants.COMPANY_FAX, faxNumber);
        values.put(ProfileConstants.PHONE_NUMBER_SECOND, otherNumber);
        values.put(ProfileConstants.EMAIL, email);
        ProfileServiceLog.d(TAG, "get profile content, "
                            + "\nfaxNumber: " + faxNumber
                            + "\nofficeNumber: " + officeNumber
                            + "\notherNumber: " + otherNumber
                            + "\nemail: " + email);
    }

    private String getBirthInfo(){
        ProfileServiceLog.d(TAG, "getBirthInfo");
        Birth birth = null;
        try {
            birth = mProfileservs.getBirthInstance(true, null);
        } catch (ProfileXcapException e) {
            e.printStackTrace();
            ProfileServiceLog.d(TAG, "have exception on getBirthInstance");
        }
        if (birth == null) {
            ProfileServiceLog.d(TAG, "error: birth is null");
            return null;
        }
        String birthDate = birth.getBirthDate();
        return birthDate;
    }

    private String getFirstName(Name name){
        ProfileServiceLog.d(TAG, "getFirstName");
        String firstName = name.getFirstName();
        return firstName;
    }

    private String getLastName(Name name){
        ProfileServiceLog.d(TAG, "getLastName");
        String lastName = name.getFamilyName();
        return lastName;
    }

    private Name getNameInstance(){
        ProfileServiceLog.d(TAG, "getNameInstance");
        Name name = null;
        try {
            name = mProfileservs.getNameInstance(true, null);
        } catch (ProfileXcapException e) {
            e.printStackTrace();
            ProfileServiceLog.d(TAG, "have exception on getNameInstance");
        }
        return name;
    }

    private String getPortraitData() throws ProfileXcapException {
        ProfileServiceLog.d(TAG, "getPortraitData");
        Portrait portrait = null;
        portrait = mProfileservs.getPortraitInstance(true, null,
                ProfileConstants.CONTENT_TYPE_PORTRAIT);
        if (portrait == null) {
            ProfileServiceLog.d(TAG, "error: portrait is null");
            return null;
        }
        String portraitContent = portrait.getPortrait();
        return portraitContent;
    }

    private String getContactPortraitData() throws ProfileXcapException {
        ProfileServiceLog.d(TAG, "getContactPortraitData");
        //need coding
        Portrait portrait = null;
        portrait = mProfileservs.getPortraitInstance(true, null,
                ProfileConstants.CONTENT_TYPE_CONTACT_PORTRAIT);
        if (portrait == null) {
            ProfileServiceLog.d(TAG, "error: portrait is null");
            return null;
        }
        String portraitContent = portrait.getPortrait();
        return portraitContent;
    }

    private String getHomeAddress(){
        ProfileServiceLog.d(TAG, "getHomeAddress");
        Address address = null;
        try {
            address = mProfileservs.getAddressInstance(true, null);
        } catch (ProfileXcapException e) {
            e.printStackTrace();
            ProfileServiceLog.d(TAG, "have exception on getAddressInstance");
        }
        if (address == null) {
            ProfileServiceLog.d(TAG, "error: address is null");
            return null;
        }
        String homeAddress = address.getHomeAddress();
        return homeAddress;
    }

    private String getWorkAddress() {
        ProfileServiceLog.d(TAG, "getWorkAddress");
        Address address = null;
        try {
            address = mProfileservs.getAddressInstance(true, null);
        } catch (ProfileXcapException e) {
            e.printStackTrace();
            ProfileServiceLog.d(TAG, "have exception on getAddressInstance");
        }
        if (address == null) {
            ProfileServiceLog.d(TAG, "error: address is null");
            return null;
        }
        String workAddress = address.getWorkAddress();
        return workAddress;
    }

    private String getCompany() {
        ProfileServiceLog.d(TAG, "getCompany");
        Career career = null;
        try {
            career = mProfileservs.getCareerInstance(true, null);
        } catch (ProfileXcapException e) {
            e.printStackTrace();
            ProfileServiceLog.d(TAG, "have exception on getCareerInstance");
        }
        if (career == null) {
            ProfileServiceLog.d(TAG, "error: career is null");
            return null;
        }
        String company = career.getEmployer();
        return company;
    }

    private String getTitle() {
        ProfileServiceLog.d(TAG, "getTitle");
        Career career = null;
        try {
            career = mProfileservs.getCareerInstance(true, null);
        } catch (ProfileXcapException e) {
            e.printStackTrace();
            ProfileServiceLog.d(TAG, "have exception on getCareerInstance");
        }
        if (career == null) {
            ProfileServiceLog.d(TAG, "error: career is null");
            return null;
        }
        String title = career.getDuty();
        return title;
    }

    private Pcc InitPcc(CommAddr commAddr, Name name, Birth birth,
                Address address, Career career) throws ProfileXcapException {
        ProfileServiceLog.d(TAG, "InitPcc");
        PccParams params = new PccParams(commAddr, name, birth, address, career);
        Pcc pcc = mProfileservs.getPccInstance(false, (Object)params);;
        return pcc;
    }

    private CommAddr InitCommAddr(int numType, String faxNum,
                String officeNum, List otherNum, String email) throws ProfileXcapException {
        ProfileServiceLog.d(TAG, "InitCommAddr, numType: " + numType
                            + "\nfaxNum: " + faxNum
                            + "\nofficeNum: " + officeNum
                            + "\nemail: " + email);
        TelNumParams telParams = null;
        EmailParams emailParams = null;
        CommAddr commAddr = null;
        if ((faxNum != null) ||
            (officeNum != null) ||
            (otherNum != null) ) {
            telParams = new TelNumParams(numType, faxNum, officeNum, otherNum);
        }
        if (email != null) {
            emailParams = new EmailParams(email);
        }
        if (telParams == null && email == null) {
            ProfileServiceLog.d(TAG, "error: no telParams or email");
            return null;
        }
        CommAddrParams params = new CommAddrParams (telParams, emailParams);
        commAddr = mProfileservs.getCommAddrInstance(false, (Object)params);
        return commAddr;
    }

    private Birth InitBirth(String brithDate) throws ProfileXcapException{
        ProfileServiceLog.d(TAG, "InitBirth, brithDate: " + brithDate);
        BirthParams params = new BirthParams (brithDate);
        Birth birth = mProfileservs.getBirthInstance(false, (Object)params);
        return birth;
    }

    private Name InitName(String firstName, String familyName) throws ProfileXcapException {
        ProfileServiceLog.d(TAG, "InitName, firstName: " + firstName +
                " familyName: " + familyName);
        NameParams params = new NameParams (firstName, familyName);
        Name name = mProfileservs.getNameInstance(false, (Object)params);
        return name;
    }

    private Address InitAddress(int addressType, String homeAddress,
            String workAddress) throws ProfileXcapException {
        ProfileServiceLog.d(TAG, "InitAddress, addressType: " + addressType +
                " homeAddress: " + homeAddress + " workAddress" + workAddress);
        AddressParams params = new AddressParams (addressType, homeAddress, workAddress);
        Address address = mProfileservs.getAddressInstance(false, (Object)params);
        return address;
    }

    private Career InitCareer(String company, String title) throws ProfileXcapException{
        ProfileServiceLog.d(TAG, "InitCareer, company: " + company + " title: " + title);
        CareerParams params = new CareerParams(company, title);
        Career career = mProfileservs.getCareerInstance(false, (Object)params);
        return career;
    }

    private void setBirth(String birthDate) throws ProfileXcapException {
        ProfileServiceLog.d(TAG, "setBirth, birth: " + birthDate);
        Birth birth = InitBirth(birthDate);
        if (birth != null) {
            birth.setBirthDate();
        } else {
            ProfileServiceLog.d(TAG, "birth is null");
        }
    }

    private void setCompanyFax(String number)throws ProfileXcapException{
        ProfileServiceLog.d(TAG, "setCompanyFax, number: " + number);
        CommAddr commAddr = InitCommAddr(CommAddr.NUMBER_TYPE_FAX,
                    number, null, null, null);
        if (commAddr != null) {
            commAddr.setTelNumber("Fax");
        } else {
            ProfileServiceLog.d(TAG, "commAddr is null");
        }
    }

    private void setOfficeNumber(String number)throws ProfileXcapException{
        ProfileServiceLog.d(TAG, "setOfficeNumber, number: " + number);
        CommAddr commAddr = InitCommAddr(CommAddr.NUMBER_TYPE_OFFICE,
                null, number, null, null);
        if (commAddr != null) {
            commAddr.setTelNumber("Work");
        } else {
            ProfileServiceLog.d(TAG, "commAddr is null");
        }
    }

    private void setOtherNumber(String number)throws ProfileXcapException{
        ProfileServiceLog.d(TAG, "setOtherNumber, number: " + number);
        mOtherNumberList = new ArrayList<String[]>();
        parseOtherNumber(number);

        CommAddr commAddr = InitCommAddr(CommAddr.NUMBER_TYPE_OTHER,
                null, null, mOtherNumberList, null);
        if (commAddr != null) {
            commAddr.setOtherNumber();
        } else {
            ProfileServiceLog.d(TAG, "commAddr is null");
        }
    }

    private void setEmail(String email)throws ProfileXcapException{
        ProfileServiceLog.d(TAG, "setEmail, email:" + email);
        CommAddr commAddr = InitCommAddr(0,
                null, null, null, email);
        if (commAddr != null) {
            commAddr.setEmail();
        } else {
            ProfileServiceLog.d(TAG, "commAddr is null");
        }
    }

    private void setName(String firstName, String familyName)throws ProfileXcapException{
        ProfileServiceLog.d(TAG, "setName, first: " + firstName + " family: " + familyName);
        if (firstName == null || familyName == null) {
            ProfileServiceLog.d(TAG, "firstName or familyName is null");
            return;
        }
        Name name = InitName(firstName, familyName);
        if (name != null) {
            name.setName();
        } else {
            ProfileServiceLog.d(TAG, "name is null");
        }
    }

    private void setCompany(String company) throws ProfileXcapException{
        ProfileServiceLog.d(TAG, "setCompany");
        Career career = InitCareer(company, null);
        if (career != null) {
            career.setEmployer();
        } else {
            ProfileServiceLog.d(TAG, "career is null");
        }
    }

    private void setDutyTitle(String title)throws ProfileXcapException {
        ProfileServiceLog.d(TAG, "setDutyTitle");
        Career career = InitCareer(null, title);
        if (career != null) {
            career.setDuty();
        } else {
            ProfileServiceLog.d(TAG, "career is null");
        }
    }

    private void setCareer(String company, String title)throws ProfileXcapException {
        ProfileServiceLog.d(TAG, "setCareer");
        Career career = InitCareer(company, title);
        if (career != null) {
            career.setCareer();
        } else {
            ProfileServiceLog.d(TAG, "career is null");
        }
    }

    private void setHomeAddress(String addressString)throws ProfileXcapException{
        ProfileServiceLog.d(TAG, "setHomeAddress");
        Address address = InitAddress(Address.ADDR_TYPE_HOME, addressString, null);
        if (address != null) {
            address.setHomeAddress();
        } else {
            ProfileServiceLog.d(TAG, "address is null");
        }
    }

    private void setWorkAddress(String addressString) throws ProfileXcapException{
        ProfileServiceLog.d(TAG, "setWorkAddress");
        Address address = InitAddress(Address.ADDR_TYPE_WORK, null, addressString);
        if (address != null) {
            address.setWorkAddress();
        } else {
            ProfileServiceLog.d(TAG, "address is null");
        }
    }

    private void setPortrait(String portraitData, String portraitType) throws ProfileXcapException {
        ProfileServiceLog.d(TAG, "setPortrait");
        String decode = "base64";
        String mimeType = portraitTypeToMimeType(portraitType);
        if (portraitData.length() == 0) {
            ProfileServiceLog.d(TAG, "no portrait content, to delete portrait");
            decode = "";
        }
        PortraitParams params = new PortraitParams(mimeType, decode, portraitData);
        Portrait portrait = mProfileservs.getPortraitInstance(false,
                (Object)params, ProfileConstants.CONTENT_TYPE_PORTRAIT);
        if (portrait != null) {
            portrait.setPortrait();
        } else {
            ProfileServiceLog.d(TAG, "portrait is null");
        }
    }

    private void setProfileservsInitParameters(String XUI, String XcapRoot,
            String IntendedId, String UserName, String Password) {
        ProfileServiceLog.d(TAG, "setProfileservsInitParameters");
        mXui = XUI;
        mXcapRoot = XcapRoot;
        mXIntendedId = IntendedId;
        mUserName = UserName;
        mPassword = Password;
        mProfileservs.setXui(XUI);
        mProfileservs.setXcapRoot(XcapRoot);
        mProfileservs.setIntendedId(IntendedId);
        mProfileservs.setHttpCredential(UserName,Password);
        //mProfileservs.setGbaCredential(new GbaCredentials(mContext, XcapRoot));
    }

    private boolean saveProfileToDB (ContentValues values) {
        ProfileServiceLog.d(TAG, "saveProfileToDB");
        int count = 0;
        String[] projection = {"_id"};
        Cursor cursor = mResolver.query(ProfileConstants.CONTENT_URI, projection, null, null, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                count = mResolver.update(ProfileConstants.CONTENT_URI, values, null, null);
            } else {
                Uri uri = mResolver.insert(ProfileConstants.CONTENT_URI, values);
                ProfileServiceLog.d(TAG, "no record, inssert one, and uri is: " + uri.toString());
            }
            cursor.close();
        } else {
            ProfileServiceLog.d(TAG, "fatal error, cursor is null!");
        }
        ProfileServiceLog.d(TAG, "count is " + count);
        return count > 0;
    }

    private void notifyGetDone(boolean isPortrait, int result){
        ProfileServiceLog.d(TAG, "notifyGetDone");
        final int length = mObsevers.beginBroadcast();
        //actually  the length should always be "1"
        for (int i = 0; i < length; i++) {
            try {
                if (isPortrait) {
                    mObsevers.getBroadcastItem(i).onGetProfilePortrait(result);
                } else {
                    mObsevers.getBroadcastItem(i).onGetProfile(result);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
        mObsevers.finishBroadcast();
    }

    private void notifySetDone(boolean isPortrait, int result){
        ProfileServiceLog.d(TAG, "notifySetDone, isPortrait: " + isPortrait);
        final int length = mObsevers.beginBroadcast();
        for (int i = 0; i < length; i++) {
            try {
                if (isPortrait) {
                    mObsevers.getBroadcastItem(i).onUpdateProfilePortrait(result);
                } else {
                    mObsevers.getBroadcastItem(i).onUpdateProfile(result);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
        mObsevers.finishBroadcast();
    }

    private void parseOtherNumber(String numberSecond) {
        ProfileServiceLog.d(TAG, "parseOtherNumber, othter number: " + numberSecond);
        String[] subStr = numberSecond.split(";");
        for (int i = 0; i < subStr.length; i++) {
            ProfileServiceLog.d(TAG, "subStr: " + subStr[i]);
            //String[]key_value = new String[2];
            String[]tmp = subStr[i].split("=");
            if (tmp.length >= 2 && tmp[0] != null && tmp[1] != null) {
                String[]key_value = tmp;
                mOtherNumberList.add(key_value);
            } else {
                String[]key_value = {"", ""};
                mOtherNumberList.add(key_value);
                break;
            }
        }
    }

    private void parseOtherNumberExt(String numberSecond) {
        ProfileServiceLog.d(TAG, "parseOtherNumberExt, othter number: " + numberSecond);
        int sepratePos = 0;
        int equalPos = 0;
        String subStr = null;
        String key = null;
        String value = null;
        String tempStr = numberSecond;
        int strLen = numberSecond.length();
        ProfileServiceLog.d(TAG, "strLen: " + strLen);
        while (tempStr.length() > 0) {
            ProfileServiceLog.d(TAG, "tempStr: " + tempStr);
            sepratePos = tempStr.indexOf(";");
            ProfileServiceLog.d(TAG, "sepratePos: " + sepratePos);
            if (sepratePos > 0) {
                subStr = tempStr.substring(0, sepratePos);
                ProfileServiceLog.d(TAG, "subStr: " + subStr);
                equalPos = subStr.indexOf("=");
                if (equalPos == -1) {
                    //no "=", that is not key-value format, so quit
                    ProfileServiceLog.d(TAG, "no \"=\", that is not key-value format, so quit");
                    break;
                }
                key = subStr.substring(0, equalPos);
                value = subStr.substring(equalPos + 1);
                ProfileServiceLog.d(TAG, "key: " + key + " value: " + value);
                String[]key_value = {key, value};
                //save key&value
                mOtherNumberList.add(key_value);
                //strLen = tempStr.length();
                //to parse follow content
                if(sepratePos + 1 < tempStr.length()) {
                    tempStr = tempStr.substring(sepratePos + 1);
                } else {
                    ProfileServiceLog.d(TAG, "parse done!");
                    break;
                }
            } else {
                //no ";" tag, so it is the last key-value String, maybe the only one
                //or if sepratePos equals to "0", it suggest that only ";" but no other char
                ProfileServiceLog.d(TAG, "no \";\" tag, maybe the only one");
                subStr = tempStr;
                equalPos = subStr.indexOf("=");
                if (equalPos == -1) {
                    //no "=", that is not key-value format, so quit
                    //error log
                    ProfileServiceLog.d(TAG, "no \"=\", that is not key-value format, so quit");
                    break;
                }
                key = subStr.substring(0, equalPos);
                value = subStr.substring(equalPos + 1);
                ProfileServiceLog.d(TAG, "key: " + key + " value: " + value);
                String[]key_value = {key, value};
                //save key&value
                mOtherNumberList.add(key_value);
                //no more content to parse, break
                break;
            }
        }
    }

    private int handleException (ProfileXcapException exception) {
        ProfileServiceLog.d(TAG, "handleException");
        int response =  ProfileConstants.RES_UNKNOW;
        int exceptionCode = exception.getExceptionCode();
        if (exceptionCode != ProfileXcapException.NO_EXCEPTION) {
            ProfileServiceLog.d(TAG, "exceptionCode: " + exceptionCode);
            switch (exceptionCode) {
                case ProfileXcapException.CONNECTION_POOL_TIMEOUT_EXCEPTION:
                case ProfileXcapException.CONNECT_TIMEOUT_EXCEPTION:
                case ProfileXcapException.NO_HTTP_RESPONSE_EXCEPTION:
                case ProfileXcapException.HTTP_CONNNECT_EXCEPTION:
                    response = ProfileConstants.RES_TIMEOUT;
                    break;

                default:
                    response = ProfileConstants.RES_UNKNOW;
                    break;
            }
        } else {
            int httpRspCode = exception.getHttpErrorCode();
            ProfileServiceLog.d(TAG, "httpRspCode: " + httpRspCode);
            switch (httpRspCode) {
                case 401:
                    response = ProfileConstants.RES_UNAUTHORIZED;
                    //dont need to retry cause http will do it
                    break;

                case 403:
                    response = ProfileConstants.RES_FORBIDEN;
                    break;

                case 404:
                    response = ProfileConstants.RES_NOTFOUND;
                    break;

                case 500:
                    response = ProfileConstants.RES_INTERNEL_ERROR;
                    break;

                case 304:
                    response = ProfileConstants.RES_NOUPDATE;
                    break;

                default:
                    response = ProfileConstants.RES_UNKNOW;
                    break;
            }
        }
        ProfileServiceLog.d(TAG, "handleException response: " + response);
        return response;
    }

    private String mimeTypeToPortraitType(String mimeType) {
        if (mimeType == null) {
            return ProfileConstants.PNG;
        } else if (mimeType.endsWith("jpeg")) {
            return ProfileConstants.JPEG;
        } else if (mimeType.endsWith("png")) {
            return ProfileConstants.PNG;
        } else if (mimeType.endsWith("bmp")) {
            return ProfileConstants.BMP;
        } else if (mimeType.endsWith("gif")) {
            return ProfileConstants.GIF;
        } else {
            ProfileServiceLog.d(TAG, "unknown mimeType, set default to png");
            return ProfileConstants.PNG;
        }
    }

    private String portraitTypeToMimeType(String portraitType) {
        if (portraitType == null) {
            return "";
        } else if (portraitType.equals(ProfileConstants.JPEG)) {
            return "image/jpeg";
        } else if (portraitType.equals(ProfileConstants.PNG)) {
            return "image/png";
        } else if (portraitType.equals(ProfileConstants.BMP)) {
            return "image/bmp";
        } else if (portraitType.equals(ProfileConstants.GIF)) {
            return "image/gif";
        } else {
            ProfileServiceLog.d(TAG, "unknown portraitType, set default to png");
            return "";
        }
    }

    private String getServerAddress() {
        String address = mJSConfig.getProfileAddress(mContext);
        //String port = mJSConfig.getProfileAddressPort(mContext);
        if (address != null) {
            ProfileServiceLog.d(TAG, "getServerAddresss, address is: " + address);
        }
        return address;
    }

    /*get Host name, actually, it is the server ip address*/
    private String getHostNameFromServerAddress(String serverAddress){
        ProfileServiceLog.d(TAG, "getHostNameFromServerAddress addrss: " + serverAddress);
        String PrefixHttp = "http://";
        String tmpStr = null;
        String ipAddress = null;
        int endIdx = 0;
        if (serverAddress.startsWith(PrefixHttp)) {
            //remove "http://" first
            tmpStr = serverAddress.substring(PrefixHttp.length());
            endIdx = tmpStr.indexOf(":");
            ipAddress = tmpStr.substring(0, endIdx);
            ProfileServiceLog.d(TAG, "ipAddress is: " + ipAddress);
        } else {
            ProfileServiceLog.d(TAG, "error format");
        }
        return ipAddress;
    }

    private String getSip(){
        String sip = mJSConfig.getPublicUri(mContext);
        ProfileServiceLog.d(TAG, "getSip, sip is: " + sip);
        return sip;
    }

    private String getPhoneNumberFromSip (String sip) {
        ProfileServiceLog.d(TAG, "getPhoneNumberFromSip sip: " + sip);
        int startIdx;
        String telNum;
        int endIdx;
        if (sip.startsWith("sip:")) {
            startIdx = sip.indexOf(":") + 1;
            endIdx = sip.indexOf("@");
            telNum = sip.substring(startIdx, endIdx);
            ProfileServiceLog.d(TAG, "subStr is: " + telNum);
            return telNum;
        } else if (sip.startsWith("tel:")) {
            startIdx = sip.indexOf(":") + 1;
            telNum = sip.substring(startIdx);
            ProfileServiceLog.d(TAG, "subStr is: " + telNum);
            return telNum;
        } else {
            ProfileServiceLog.d(TAG, "error, sip format is unexpected");
        }
        return null;
    }
    private String phoneNumber2IntentId (String phonenumber) {
        ProfileServiceLog.d(TAG, "phoneNumber2IntentId phonenumber: " + phonenumber);
        return ("tel:" + phonenumber);
    }

    private void savePhoneNumber() {
        ContentValues values = new ContentValues();
        values.put(ProfileConstants.PHONE_NUMBER, mPhoneNumber);
        saveProfileToDB(values);
    }

    private void getQRCode() {
        ProfileServiceLog.d(TAG, "getQRCode");
        Message msg = mWorkHandler.obtainMessage(OP_GET_QRCODE);
        msg.sendToTarget();

    }
    private void handleGetQRCode() {
        ProfileServiceLog.d(TAG, "handleGetQRCode");
        int result = ProfileConstants.RES_OK;
        mXui = mXIntendedId;
        QRCode qrCode = null;
        String qrCodeData = null;
        int mode = 0;
        ContentValues values = null;

        setProfileservsInitParameters(mXui, mXcapRoot, mXIntendedId, mUserName, mPassword);
        try {
            mProfileservs.buildDocumentUri(ProfileConstants.CONTENT_TYPE_QRCODE);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        String[] projection = {ProfileConstants.QRCODE_ETAG};
        Cursor cursor = mResolver.query(ProfileConstants.CONTENT_URI, projection, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int qrCodeETagIndex = cursor.getColumnIndex(ProfileConstants.QRCODE_ETAG);
            ProfileXcapElement.mQRCodeETag = cursor.getString(qrCodeETagIndex);
            cursor.close();
            if (ProfileXcapElement.mQRCodeETag == null) {
                ProfileServiceLog.d(TAG, "QRCodeETag gotten from db is null");
                ProfileXcapElement.mQRCodeETag = "";
            }
        } else {
            ProfileXcapElement.mQRCodeETag = "";
        }
        try {
            qrCode = mProfileservs.getQRCodeInstance(true, null,
                    ProfileConstants.CONTENT_TYPE_QRCODE);
            if (qrCode == null) {
                ProfileServiceLog.d(TAG, "error: qrCode is null");
                result = ProfileConstants.RES_INTERNEL_ERROR;
            } else {
                qrCodeData = qrCode.getQRCodeData();
                if (qrCodeData == null || qrCodeData.length() <= 0) {
                    ProfileServiceLog.d(TAG, "no qrCodeData content");
                } else {
                    //the result must be "OK" here since not return before, so save the QRCode content to db
                    values = new ContentValues();
                    values.put(ProfileConstants.QRCODE, qrCodeData);
                    values.put(ProfileConstants.QRCODE_ETAG, ProfileXcapElement.mQRCodeETag);
                }
                mode = qrCode.getFlag();
                //we don't care about mimeType or decode
            }
        } catch (ProfileXcapException e) {
            ProfileServiceLog.d(TAG, "have exeption on getQRCode");
            result = handleException(e);
            //have exception, the result should be "no update" or other error
            //anyway, no need to save to db, so just return the result
            //return result;
        }
        /*for the etag is static,  clear the etag after every set operation
        for the next get operatrion, we get etag from db*/
        ProfileXcapElement.mQRCodeETag = "";
        if (values != null && values.size() > 0) {
            if (!saveProfileToDB(values)) {
                ProfileServiceLog.d(TAG, "error in saveProfileToDB");
            }
        } else {
            ProfileServiceLog.d(TAG, "no value to save");
        }
        final int length = mObsevers.beginBroadcast();
        for (int i = 0; i < length; i++) {
            try {
                mObsevers.getBroadcastItem(i).onGetQRCode(result, mode);
            } catch (RemoteException e) {
            } catch (RuntimeException e) {
            }
        }
        mObsevers.finishBroadcast();
    }

    private void setQRCodeMode(int mode) {
        ProfileServiceLog.d(TAG, "setQRCodeMode, flag: " + mode);
        Message msg = mWorkHandler.obtainMessage(OP_SET_QRCODE_MODE);
        msg.obj = mode;
        msg.sendToTarget();
    }

    private void handleSetQRCodeMode(int mode){
        ProfileServiceLog.d(TAG, "handleSetQRCodeMode, mode: " + mode);

        int result = ProfileConstants.RES_OK;
        mXui = mXIntendedId;
        setProfileservsInitParameters(mXui, mXcapRoot, mXIntendedId, mUserName, mPassword);
        try {
            mProfileservs.buildDocumentUri(ProfileConstants.CONTENT_TYPE_QRCODE_MODE);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        try {
            QRCode qrCode = mProfileservs.getQRCodeInstance(false,
                    (Object)mode, ProfileConstants.CONTENT_TYPE_QRCODE_MODE);
            if (qrCode != null) {
                qrCode.setFlag(); //set the mode to server
            } else {
                ProfileServiceLog.d(TAG, "error: qrCode is null");
                result = ProfileConstants.RES_INTERNEL_ERROR;
            }
        } catch (ProfileXcapException e) {
                ProfileServiceLog.d(TAG, "have ProfileXcapException on set QRCdoe Mode");
                e.printStackTrace();
                result = handleException(e);
        }

        final int length = mObsevers.beginBroadcast();
        for (int i = 0; i < length; i++) {
            try {
                mObsevers.getBroadcastItem(i).onUpdateQRCodeMode(result, mode);
            } catch (RemoteException e) {
            } catch (RuntimeException e) {
            }
        }
        mObsevers.finishBroadcast();
    }

    private void getQRCodeMode() {
        ProfileServiceLog.d(TAG, "getQRCodeMode");
        Message msg = mWorkHandler.obtainMessage(OP_GET_QRCODE_MODE);
        msg.sendToTarget();
    }

    private void handleGetQRCodeMode() {
        ProfileServiceLog.d(TAG, "handleGetQRCodeMode");
        int result = ProfileConstants.RES_OK;
        mXui = mXIntendedId;
        QRCode qrCode = null;
        int mode = 0;
        ContentValues values = null;

        setProfileservsInitParameters(mXui, mXcapRoot, mXIntendedId, mUserName, mPassword);
        try {
            mProfileservs.buildDocumentUri(ProfileConstants.CONTENT_TYPE_QRCODE_MODE);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        try {
            qrCode = mProfileservs.getQRCodeInstance(true, null,
                    ProfileConstants.CONTENT_TYPE_QRCODE_MODE);
            if (qrCode == null) {
                ProfileServiceLog.d(TAG, "error: qrCode is null");
                result = ProfileConstants.RES_INTERNEL_ERROR;
            } else {
                mode = qrCode.getFlag();
                //we don't care about mimeType or decode
            }
        } catch (ProfileXcapException e) {
            ProfileServiceLog.d(TAG, "have exeption on getPortraitData");
            result = handleException(e);
            //have exception, the result should be "no update" or other error
            //anyway, no need to save to db, so just return the result
            //return result;
        }
        final int length = mObsevers.beginBroadcast();
        for (int i = 0; i < length; i++) {
            try {
                mObsevers.getBroadcastItem(i).onGetQRCodeMode(result, mode);
            } catch (RemoteException e) {
            } catch (RuntimeException e) {
            }
        }
        mObsevers.finishBroadcast();
    }

    private static class HttpAuthenticator extends Authenticator {
        private static PasswordAuthentication sPasswordAuthentication;

        protected PasswordAuthentication getPasswordAuthentication() {
            ProfileServiceLog.d(TAG, "getPasswordAuthentication");

            if (sPasswordAuthentication == null) {
                sPasswordAuthentication = new PasswordAuthentication(mUserName,
                        mPassword.toCharArray());
            }
            return sPasswordAuthentication;
        }
    }
}

