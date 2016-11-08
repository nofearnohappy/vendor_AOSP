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

package com.mediatek.rcs.contacts.profileapp;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Profile;
import android.util.Base64;
import android.util.Log;

import com.cmcc.ccs.profile.ProfileListener;
import com.cmcc.ccs.profile.ProfileService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.gsma.joyn.JoynServiceListener;

/* ProfileManager. */
public class ProfileManager extends ProfileListener implements JoynServiceListener{

    private static ProfileManager sInstance;
    private static ProfileInfo mProfile = null;
    private ArrayList<ProfileManagerListener> mListenerList
            = new ArrayList<ProfileManagerListener>();
    private static final Uri PROFILE_CONTENT_URI
            = Uri.parse("content://com.cmcc.ccs.profile");
    private static final String TAG = ProfileManager.class.getName();
    private Context mContext;
    private ProfileService mProfileSevs;
    private HashMap<String, String> mTempProfileInfoList = new HashMap<String, String>();
    private ArrayList<String> mTempNumberList = new ArrayList<String>();
    private String mTempPortraitFileName;
    private int mQRCodeMode;

    private ProfileManagerHandler mHandler;
    private static final int MSG_GET_WHOLE_PROFILE_FROM_DB = 1000;
    private static final int MSG_GET_PROFILE_COMMON_FROM_DB = 1001;
    private static final int MSG_GET_PORTRAIT_FROM_DB = 1002;
    //private static final int MSG_UPDATE_PROFILE_TO_SERVER = 1003;
    private static final int MSG_UPDATE_WHOLE_PROFILE_TO_CONTACT_DB = 1003;
    private static final int MSG_UPDATE_PROFILE_COMMON_TO_CONTACT_DB = 1004;
    private static final int MSG_UPDATE_PORTRAIT_TO_CONTACT_DB = 1005;
    private static final int MSG_GET_PROFILE_QR_CODE_FROM_DB = 1006;

    private int mServerState;
    private static final int SERVER_INIT = 0;
    private static final int SERVER_CONNECTING = 1;
    private static final int SERVER_CONNECTED = 2;
    private static final int SERVER_GET_PROFILE = 4;
    private static final int SERVER_SET_PROFILE = 8;
    //protrait is independent of profile
    private static final int SERVER_SET_PORTRAIT = 16;
    private static final int SERVER_GET_CONTACT_PORTRAIT = 32;
    private static final int SERVER_GET_QR_CODE = 64;
    private static final int SERVER_SET_QR_CODE_MODE = 128;

    public static final int SERVER_RESULT_NONE = 0;
    public static final int SERVER_RESULT_GET_PROFILE = 1;
    public static final int SERVER_RESULT_GET_PORTRAIT = 2;
    public static final int SERVER_RESULT_SET_PROFILE = 3;
    public static final int SERVER_RESULT_SET_PORTRAIT = 4;

    private static String BUSINESS_SETTING = "business_info";

    private static final int PROFILE_TYPE_ALL = 0;
    private static final int PROFILE_TYPE_COMMON = 1;
    private static final int PROFILE_TYPE_PORTRAIT = 2;

    private ProfileManager(Context context) {
        Log.i(TAG, "ProfileManager: Constructure");
        mProfileSevs = new ProfileService(context, this);
        mProfileSevs.addProfileListener(this);
        mProfileSevs.connect();
        mServerState = SERVER_CONNECTING;
        mContext = context;

        HandlerThread thread = new HandlerThread("ProfileManagerHandler");
        thread.start();
        mHandler = new ProfileManagerHandler(thread.getLooper());

        getMyProfileFromLocal();
    }

    /* single instance class */
    public static ProfileManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ProfileManager(context);
        }
        return sInstance;
    }

    public void onServiceConnected() {
        Log.i(TAG, "onServiceConnected: serverState = " + mServerState);
        if ((mServerState & SERVER_GET_PROFILE) > 0) {
            mProfileSevs.getProfileInfo();
        }
        //need add portrait part
        if ((mServerState & SERVER_SET_PROFILE) > 0) {
            mProfileSevs.updateProfileInfo(mTempProfileInfoList);
            mTempProfileInfoList.clear();
        }
        if ((mServerState & SERVER_SET_PORTRAIT) > 0) {
            mProfileSevs.updateProfilePortrait(mTempPortraitFileName);
            mTempPortraitFileName = null;
        }
        if ((mServerState & SERVER_GET_CONTACT_PORTRAIT) > 0) {
            Log.i(TAG, "onServiceConnected: number size = " + mTempNumberList.size());
            for (String number : mTempNumberList) {
                mProfileSevs.getContactPortrait(number);
            }
            mTempNumberList.clear();
        }
        if ((mServerState & SERVER_GET_QR_CODE) > 0) {
            mProfileSevs.getProfileQRCode();
        }

        if ((mServerState & SERVER_SET_QR_CODE_MODE) > 0) {
            mProfileSevs.setProfileQRCodeMode(mQRCodeMode);
        }
        mServerState = SERVER_CONNECTED;
    }

    public void onServiceDisconnected(int code) {
        Log.i(TAG, "onServiceDisconnected");
        mServerState = SERVER_INIT;
    }

    /**
     * register listener.
     * @param listener :the listener you what to registered.
     */
    public void registerProfileManagerListener(ProfileManagerListener listener) {
        if (!mListenerList.contains(listener)) {
            mListenerList.add(listener);
            /* notify listener after register */
            listener.onProfileInfoUpdated(0, SERVER_RESULT_NONE, mProfile);
        }
    }

    /**
     * unregister listener.
     * @param listener
     */
    public void unregisterProfileManagerListener(ProfileManagerListener listener) {
        if (mListenerList.contains(listener)) {
            mListenerList.remove(listener);
        }
    }

    /**
     * Get my profile from server
     */
    public void getMyProfileFromServer() {
        Log.i(TAG, "getMyProfileFromServer: serverState = " + mServerState);
        if ((mServerState & SERVER_CONNECTED) > 0) {
            mProfileSevs.getProfileInfo();
        } else if ((mServerState & SERVER_CONNECTING) > 0) {
            mServerState |= SERVER_GET_PROFILE;
        } else {
            mServerState |= SERVER_GET_PROFILE;
            mProfileSevs.connect();
            mServerState |= SERVER_CONNECTING;
        }
    }

    /**
     * Get my profile from local variable.
     * @return ProfileInfo :personal profile information.
     */
    public ProfileInfo getMyProfileFromLocal() {
        Log.i(TAG, "getMyProfileFromLocal:");
        if (mProfile == null) {
            mProfile = new ProfileInfo();
            Message msg = mHandler.obtainMessage(MSG_GET_WHOLE_PROFILE_FROM_DB);
            msg.arg1 = 0;
            msg.sendToTarget();
        }
        return mProfile;
    }

    /**
     * Get my profile from data base.
     * @return void
     */
    public void getMyProfileFromDB() {
        Log.i(TAG, "getMyProfileFromDB:");
        removeMessageIfExist(MSG_GET_WHOLE_PROFILE_FROM_DB);
        Message msg = mHandler.obtainMessage(MSG_GET_WHOLE_PROFILE_FROM_DB);
        msg.arg1 = 0;
        msg.sendToTarget();
    }

    /**
     * Get my profile QR Code from data base.
     * @return void
     */
    public void getProfileQRCodeFromServer() {
        Log.d(TAG, "getProfileQRCodeFromServer: server state = " + mServerState);
        if ((mServerState & SERVER_CONNECTED) > 0) {
            mProfileSevs.getProfileQRCode();
        } else if ((mServerState & SERVER_CONNECTING) > 0) {
            mServerState |= SERVER_GET_QR_CODE;
        } else {
            mServerState |= SERVER_GET_QR_CODE;
            mProfileSevs.connect();
            mServerState |= SERVER_CONNECTING;
        }
    }

    /**
     * Get my profile qr code from data base.
     * @return void
     */
    public void getProfileQRCodeFromLocal() {
        Log.i(TAG, "getProfileQRCodeFromLocal:");
        removeMessageIfExist(MSG_GET_PROFILE_QR_CODE_FROM_DB);
        Message msg = mHandler.obtainMessage(MSG_GET_PROFILE_QR_CODE_FROM_DB);
        msg.arg1 = 0;
        msg.sendToTarget();
    }

    /**
     * Get contact icon by number.
     * @param number contact number
     */
    public void getContactPortraitByNumber(final String number) {

        Log.i(TAG, "getContactPortraitByNumber: serverState = " + mServerState
              + "; number = " + number);
        if ((mServerState & SERVER_CONNECTED) > 0) {
            mProfileSevs.getContactPortrait(number);
        } else if ((mServerState & SERVER_CONNECTING) > 0) {
            mServerState |= SERVER_GET_CONTACT_PORTRAIT;
            mTempNumberList.add(number);
        } else {
            mServerState |= SERVER_GET_CONTACT_PORTRAIT;
            mProfileSevs.connect();
            mServerState |= SERVER_CONNECTING;
            mTempNumberList.add(number);
        }

    }

    /**
     * API entry for update Profile other number.
     * @param list : other number list.
     * @return void
     */
    public void updateProfileOtherNumber(ArrayList<ProfileInfo.OtherNumberInfo> list) {

        mProfile.setAllOtherNumber(list);

        HashMap<String, String> map = new HashMap<String, String>();
        map.put(ProfileInfo.PHONE_NUMBER_SECOND, mProfile.getOtherNumberToString());

        updateProfileByType(map);
    }

    /**
     * API entry for update Profile to Server and database by data type.
     * @param type : data type.
     * @param content : data content.
     */
    public void updateProfileByType(HashMap map) {
        Iterator iter = map.entrySet().iterator();
        String type = null;
        String content = null;
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            type = (String)entry.getKey();
            content = (String)entry.getValue();
            Log.d(TAG, "updateProfileByType: type = " + type + " content = " + content);
            if (type == ProfileInfo.PORTRAIT) {
                Log.d(TAG, "error: should never run in ever, for portrait");
                if (content != null && !content.equals("")) {
                    byte[] photo = Base64.decode(content, Base64.DEFAULT);
                    mProfile.setPhoto(photo);
                } else {
                    mProfile.setPhoto(null);
                }
            } else if (type != ProfileInfo.PHONE_NUMBER_SECOND) {
                mProfile.setContentByKey(type, content);
            }
        }
        removeMessageIfExist(MSG_UPDATE_PROFILE_COMMON_TO_CONTACT_DB);
        Message msg = mHandler.obtainMessage(MSG_UPDATE_PROFILE_COMMON_TO_CONTACT_DB);
        msg.sendToTarget();
        msg.arg1 = PROFILE_TYPE_COMMON;
        updateProfileToServer(map);
        notifyProfileInfoUpdate(0, SERVER_RESULT_NONE);
    }

    /**
     * API entry for update portrait to Server and database by data type.
     * @param type : data type.
     * @param content : data content.
     */
    public void updateProfilePortrait(String fileName, HashMap map) {
        Log.d(TAG, "updateProfilePortrait: fileName: " + fileName);
        mProfile.setPhotoFileName(fileName);
        String type = null;
        String content = null;
        Iterator iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            type = (String)entry.getKey();
            content = (String)entry.getValue();
            if (type == ProfileInfo.PORTRAIT) {
                if (content != null && !content.equals("")) {
                    byte[] photo = Base64.decode(content, Base64.DEFAULT);
                    mProfile.setPhoto(photo);
                } else {
                    mProfile.setPhoto(null);
                }
            }
        }
        removeMessageIfExist(MSG_UPDATE_PORTRAIT_TO_CONTACT_DB);
        Message msg = mHandler.obtainMessage(MSG_UPDATE_PORTRAIT_TO_CONTACT_DB);
        msg.sendToTarget();

        updatePortraitToServer(fileName);
        notifyProfileInfoUpdate(0, SERVER_RESULT_NONE);
    }

    /**
     * Update Profile to Server by data type.
     * @param type : data type.
     * @param content : data content.
     * @return void
     */
    private void updateProfileToServer(HashMap map) {
        Log.d(TAG, "updateProfileToServer: serverState = " + mServerState);
        if ((mServerState & SERVER_CONNECTED) > 0) {
            mProfileSevs.updateProfileInfo(map);
        } else if ((mServerState & SERVER_CONNECTING) > 0) {
            mServerState |= SERVER_SET_PROFILE;
        } else {
            mServerState |= SERVER_SET_PROFILE;
            mProfileSevs.connect();
            mServerState |= SERVER_CONNECTING;
        }
        mTempProfileInfoList.clear();
        mTempProfileInfoList.putAll(map);
    }

    /**
     * Update Profile to Server by data type.
     * @param type : data type.
     * @param content : data content.
     * @return void
     */
    private void updatePortraitToServer(String photoFileName) {
        Log.d(TAG, "updatePortraitToServer: serverState = " + mServerState);
        if ((mServerState & SERVER_CONNECTED) > 0) {
            if (photoFileName == null) {
                Log.d(TAG, "erro, photoFileName should not be null here");
            } else {
                mProfileSevs.updateProfilePortrait(photoFileName);
            }
        } else if ((mServerState & SERVER_CONNECTING) > 0) {
            mServerState |= SERVER_SET_PORTRAIT;  //need change here
        } else {
            mServerState |= SERVER_SET_PORTRAIT;
            mProfileSevs.connect();
            mServerState |= SERVER_CONNECTING;
        }
        mTempPortraitFileName = photoFileName;
    }


    /**
     * Update Profile qr code mode to Server by data type.
     * @param mode : buniness info mode.
     * @return void
     */
    public void updateProfileQRCodeModeToServer(int mode) {
        Log.d(TAG, "updateProfileQRCodeModeToServer: serverState = " + mServerState);

        if ((mServerState & SERVER_CONNECTED) > 0) {
            mProfileSevs.setProfileQRCodeMode(mode);
        } else if ((mServerState & SERVER_CONNECTING) > 0) {
            mServerState |= SERVER_SET_QR_CODE_MODE;
        } else {
            mServerState |= SERVER_SET_QR_CODE_MODE;
            mProfileSevs.connect();
            mServerState |= SERVER_CONNECTING;
        }
        mQRCodeMode = mode;
    }

    /**
     * Update Profile to Database by data type.
     * never be called for updating DB in profileservice
     * @param type : data type.
     * @param content : data content.
     * @return void
     */
    private void updateProfileDbByType(String type, String content) {
        Log.d(TAG, "updateProfileDbByType: type = " + type + " content = " + content);

        ContentValues values = new ContentValues();
        values.put(type, content);

        String[] projections = {"_id"};
        ContentResolver resolver = mContext.getContentResolver();
        Cursor c = resolver.query(PROFILE_CONTENT_URI, projections, null, null, null);

        if (c != null && c.getCount() >= 1) {
            c.moveToFirst();
            long id = c.getLong(c.getColumnIndex("_id"));
            String selection = "_id = " + String.valueOf(id);
            resolver.update(PROFILE_CONTENT_URI, values, selection, null);
        } else {
            resolver.insert(PROFILE_CONTENT_URI, values);
        }
        c.close();
    }

    /**
     * Update profile info from data base.
     * @param context
     */
    private void getProfileFromDBbyType(Context context, int profileType) {
        Log.d(TAG, "getProfileFromDB: profileType: " + profileType);
        String[] keySet = null;
        if (profileType == PROFILE_TYPE_COMMON) {
            keySet = ProfileInfo.mProfileCommonKeySet;
        } else if (profileType == PROFILE_TYPE_PORTRAIT) {
            keySet = ProfileInfo.mPortraitKeySet;
        } else {
            keySet = ProfileInfo.mAllProfileKeySet;
        }
        ContentResolver resolver = context.getContentResolver();
        Cursor c = resolver.query(PROFILE_CONTENT_URI,
                keySet, null, null, null);
        if (c != null) {
            c.moveToFirst();
            if (c.getCount() >= 1) {
                for (String key : keySet) {
                    if (key.equals(ProfileInfo.PORTRAIT)) {
                        String photoStr = c.getString(c.getColumnIndex(ProfileInfo.PORTRAIT));
                        if (photoStr != null && !photoStr.equals("")) {
                            byte[] photo = Base64.decode(photoStr, Base64.DEFAULT);
                            mProfile.setPhoto(photo);
                        } else {
                            mProfile.setPhoto(null);
                        }
                    } else if (key.equals(ProfileInfo.PHONE_NUMBER_SECOND)) {
                        String otherNumber = c.getString(c.getColumnIndex(ProfileInfo.PHONE_NUMBER_SECOND));
                        mProfile.parseOtherNumberStringToMap(otherNumber);
                    } else {
                        mProfile.setContentByKey(key, c.getString(c.getColumnIndex(key)));
                    }
                }
            } else {
                mProfile.clearAll();
            }
        }
        c.close();

        int msgWhat = MSG_UPDATE_WHOLE_PROFILE_TO_CONTACT_DB;
        if (profileType == PROFILE_TYPE_COMMON) {
            msgWhat = MSG_UPDATE_PROFILE_COMMON_TO_CONTACT_DB;
        } else if (profileType == PROFILE_TYPE_PORTRAIT) {
            msgWhat = MSG_UPDATE_PORTRAIT_TO_CONTACT_DB;
        }
        removeMessageIfExist(msgWhat);
        Message msg = mHandler.obtainMessage(msgWhat);
        //we can not put profileType to arg1 because handler.hasMessages can not judge it
        //So it maybe result in remove the message with different arg1 in error,
        //so we need define 3 msg for 3 tyes
        msg.sendToTarget();
    }

    /**
         * Update profile qr code from data base.
         * @param context
         */
    private void getProfileQRCodeFromDB(Context context) {
        Log.d(TAG, "getProfileQRCodeFromDB: ");
        ContentResolver resolver = context.getContentResolver();
        String[] projection = {ProfileInfo.QR_CODE};
        Cursor c = resolver.query(PROFILE_CONTENT_URI,
                projection, null, null, null);
        if (c != null) {
            c.moveToFirst();
            if (c.getCount() >= 1) {

                String qrcodeStr = c.getString(c.getColumnIndex(ProfileInfo.QR_CODE));
                if (qrcodeStr != null && !qrcodeStr.equals("")) {
                    byte[] qrcode = Base64.decode(qrcodeStr, Base64.DEFAULT);
                    mProfile.setQrcode(qrcode);
                } else {
                    mProfile.setQrcode(null);
                }
            } else {
                mProfile.setQrcode(null);
            }
        }
        c.close();
    }

    /**
     * Notify listener to update profile information.
     */
    private void notifyProfileInfoUpdate(int flag, int operation) {
        for (ProfileManagerListener listener: mListenerList) {
            listener.onProfileInfoUpdated(flag, operation, mProfile);
        }
    }

    /**
     * Notify all listeners that contact info changed.
     * @param flag
     * @param number
     * @param photo
     */
    private void notifyContactInfoUpdate(int flag, String number, byte[] photo) {
        for (ProfileManagerListener listener: mListenerList) {
            listener.onContactIconGotten(flag, number, photo);
        }
    }

    /**
     * Notify all listeners that qr code changed.
     * @param flag
     * @param number
     * @param photo
     */
    private void notifyQRCodeGotten(int result, int mode) {
        for (ProfileManagerListener listener: mListenerList) {
            listener.onGetProfileQRCode(result, mode);
        }
    }

    /**
     * Update profile info to Contact Profile DB.
     * @param type: isPortrait. whether portrait or not
     */
    private void updateProfileInfoToContactDB(int profileType) {
        Log.d(TAG, "updateProfileInfoToContactDB: profileType: " + profileType);
        long rawContactId;
        ContentResolver resolver = mContext.getContentResolver();

        String[] projections = {Profile._ID, Profile.NAME_RAW_CONTACT_ID};

        Cursor c = resolver.query(Profile.CONTENT_URI, projections, null, null, null);
        Uri dataUri = Profile.CONTENT_URI.buildUpon().appendPath("data").build();

        if (c.getCount() >= 1 && c.moveToFirst()) {
            rawContactId = c.getLong(c.getColumnIndex(Profile.NAME_RAW_CONTACT_ID));
        } else {
            ContentValues values = new ContentValues();
            values.put(Profile.DISPLAY_NAME, ProfileInfo.getContentByKey(ProfileInfo.NAME));
            Uri newUri = resolver.insert(Profile.CONTENT_RAW_CONTACTS_URI, values);
            rawContactId = ContentUris.parseId(newUri);
        }
        c.close();
        switch (profileType) {
            case PROFILE_TYPE_COMMON:
                updateProfileNameToContactDB(rawContactId, mContext, dataUri);
                updateProfileNumberToContactDB(rawContactId, mContext, dataUri);
                break;
            case PROFILE_TYPE_PORTRAIT:
                updateProfilePhotoToContactDB(rawContactId, mContext, dataUri);
                break;
            case PROFILE_TYPE_ALL:
                updateProfileNameToContactDB(rawContactId, mContext, dataUri);
                updateProfileNumberToContactDB(rawContactId, mContext, dataUri);
                updateProfilePhotoToContactDB(rawContactId, mContext, dataUri);
                break;
            default:
                //error
                break;
        }
    }

    /**
     * Update profile info' name to Contact Profile DB.
     * @param rawId:  raw contact ID.
     * @param context: context.
     * @param dataUri:  Data table Uri.
     */
    private void updateProfileNameToContactDB(long rawId, Context context, Uri dataUri) {
        Log.d(TAG, "updateProfileNameToContactDB: name = " + mProfile.getName());
        ContentResolver resolver = context.getContentResolver();
        ContentValues values =  new ContentValues();

        String[] projections = {Data._ID};
        String selection = Data.MIMETYPE + " = ?" + " AND (" + Data.RAW_CONTACT_ID + " = ?)";
        String[] selectionArgs = new String[] {
                StructuredName.CONTENT_ITEM_TYPE,
                String.valueOf(rawId) };

        Cursor c = resolver.query(dataUri, projections, selection, selectionArgs, null, null);
        if (c != null && c.getCount() >= 1) {
            values.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
            values.put(StructuredName.DISPLAY_NAME, mProfile.getName());
            values.put(StructuredName.GIVEN_NAME, ProfileInfo.getContentByKey(ProfileInfo.FIRST_NAME));
            values.put(StructuredName.FAMILY_NAME, ProfileInfo.getContentByKey(ProfileInfo.LAST_NAME));
            values.put(StructuredName.RAW_CONTACT_ID, rawId);
            resolver.update(dataUri, values, selection, selectionArgs);
        } else {
            values.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
            values.put(StructuredName.DISPLAY_NAME, mProfile.getName());
            values.put(StructuredName.GIVEN_NAME, ProfileInfo.getContentByKey(ProfileInfo.FIRST_NAME));
            values.put(StructuredName.FAMILY_NAME, ProfileInfo.getContentByKey(ProfileInfo.LAST_NAME));
            values.put(StructuredName.RAW_CONTACT_ID, rawId);
            resolver.insert(dataUri, values);
        }
        c.close();


    }

    /**
     * Update profile info' photo to Contact Profile DB.
     * @param rawId:  raw contact ID.
     * @param context
     * @param dataUri:  Data table Uri.
     */
    private void updateProfilePhotoToContactDB(long rawId, Context context, Uri dataUri) {

        ContentResolver resolver = context.getContentResolver();
        ContentValues values =  new ContentValues();

        String[] projections = {Data._ID};
        String selection = Data.MIMETYPE + " = ?" + " AND (" + Data.RAW_CONTACT_ID + " = ?)";
        String[] selectionArgs = new String[] { Photo.CONTENT_ITEM_TYPE, String.valueOf(rawId) };

        Cursor c = resolver.query(dataUri, projections, selection, selectionArgs, null, null);
        if (c != null && c.getCount() >= 1) {
            values.put(Photo.PHOTO, mProfile.photo);
            values.put(Photo.RAW_CONTACT_ID, rawId);
            resolver.update(dataUri, values, selection, selectionArgs);
        } else {
            values.put(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
            values.put(Photo.PHOTO, mProfile.photo);
            values.put(Photo.RAW_CONTACT_ID, rawId);
            resolver.insert(dataUri, values);
        }
        c.close();
    }

    /**
     * Update profile info' Number to Contact Profile DB.
     * @param rawId:  raw contact ID.
     * @param context
     * @param dataUri:  Data table Uri.
     */
    private void updateProfileNumberToContactDB(long rawId, Context context, Uri dataUri) {

        ContentResolver resolver = context.getContentResolver();
        ContentValues values =  new ContentValues();

        String[] projections = {Data._ID};
        String selection = Data.MIMETYPE + " = ?" + " AND (" + Data.RAW_CONTACT_ID + " = ?)";
        String[] selectionArgs = new String[] { Phone.CONTENT_ITEM_TYPE, String.valueOf(rawId) };

        Cursor c = resolver.query(dataUri, projections, selection, selectionArgs, null, null);
        String number = mProfile.getContentByKey(ProfileInfo.PHONE_NUMBER);
        Log.d(TAG, "updateProfileNumberToContactDB: number = " + number);
        if (c != null && c.getCount() >= 1) {
            if (number == null || number.equals("")) {
                resolver.delete(dataUri, selection, selectionArgs);
            } else {
                values.put(Phone.NUMBER, number);
                values.put(Phone.RAW_CONTACT_ID, rawId);
                resolver.update(dataUri, values, selection, selectionArgs);
            }
        } else if (number != null && !number.equals("")) {
            values.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
            values.put(Phone.NUMBER, number);
            values.put(Phone.RAW_CONTACT_ID, rawId);
            resolver.insert(dataUri, values);
        }

        c.close();

    }

    /**
     * remove unhandled message.
     * @param what:  msg id
     */
    private void removeMessageIfExist(int what) {
        if (mHandler.hasMessages(what)) {
            Log.d(TAG, "onGetProfile: remove messages " + what);
            mHandler.removeMessages(what);
        }
    }

    private final class ProfileManagerHandler extends Handler {

            public ProfileManagerHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage (Message msg) {
                Log.d(TAG, "handleMessage: msg = " + msg.what);
                if(null == msg)
                {
                    return;
                }
                switch (msg.what) {
                    case MSG_GET_WHOLE_PROFILE_FROM_DB:
                        getProfileFromDBbyType(mContext, PROFILE_TYPE_ALL);
                        notifyProfileInfoUpdate(msg.arg1, msg.arg2);
                        break;

                    case MSG_GET_PROFILE_COMMON_FROM_DB:
                        getProfileFromDBbyType(mContext, PROFILE_TYPE_COMMON);
                        notifyProfileInfoUpdate(msg.arg1, msg.arg2);
                        break;

                    case MSG_GET_PORTRAIT_FROM_DB:
                        getProfileFromDBbyType(mContext, PROFILE_TYPE_PORTRAIT);
                        notifyProfileInfoUpdate(msg.arg1, msg.arg2);
                        break;

                    case MSG_UPDATE_WHOLE_PROFILE_TO_CONTACT_DB:
                        updateProfileInfoToContactDB(PROFILE_TYPE_ALL);
                        break;

                    case MSG_UPDATE_PROFILE_COMMON_TO_CONTACT_DB:
                        updateProfileInfoToContactDB(PROFILE_TYPE_COMMON);
                        break;

                    case MSG_UPDATE_PORTRAIT_TO_CONTACT_DB:
                        updateProfileInfoToContactDB(PROFILE_TYPE_PORTRAIT);
                        break;

                    case MSG_GET_PROFILE_QR_CODE_FROM_DB:
                        getProfileQRCodeFromDB(mContext);
                        notifyQRCodeGotten(msg.arg1, msg.arg2);
                        break;

                    default:
                        break;
                }
            }
    }


    /* Profile manager listener communicate with UI */
    interface ProfileManagerListener {

        /* Notify profile updating information */
        void onProfileInfoUpdated(int flag, int operation, ProfileInfo profile);

        /* Notify Contact icon update information */
        void onContactIconGotten(int flag, String number, byte[]icon);

        /* Notify QR Code gotted */
        void onGetProfileQRCode (int result, int mode);

        /* Notify QR Code mode setted */
        void onUpdateProfileQRCodeMode (int result, int mode);
    }

     /**
      * ProfileListener:
      * listener when get profile result back.
      * @param result:
      */
     public void onGetProfile(int result) {
         Log.d(TAG, "onGetProfile: result = " + result);
         removeMessageIfExist(MSG_GET_PROFILE_COMMON_FROM_DB);
         Message msg = mHandler.obtainMessage(MSG_GET_PROFILE_COMMON_FROM_DB);
         msg.arg1 = result;
         msg.arg2 = SERVER_RESULT_GET_PROFILE;
         msg.sendToTarget();
     }

    /**
     * ProfileListener:
     * listener when get portrait result back.
     * @param result:
     */
    public void onGetProfilePortrait(int result) {
        Log.d(TAG, "onGetProfilePortrait: result = " + result);
        removeMessageIfExist(MSG_GET_PORTRAIT_FROM_DB);
        Message msg = mHandler.obtainMessage(MSG_GET_PORTRAIT_FROM_DB);
        msg.arg1 = result;
        msg.arg2 = SERVER_RESULT_GET_PORTRAIT;
        msg.sendToTarget();
    }

    /**
     * ProfileListener:
     * listener call back when profile update done.
     * @param result:
     */
    public void onUpdateProfile(int result) {
        Log.i(TAG, "onUpdateProfile: result = " + result);
        if (result == ProfileService.OK || result == ProfileService.NOUPDATE) {
            notifyProfileInfoUpdate(result, SERVER_RESULT_SET_PROFILE);
        } else {
            removeMessageIfExist(MSG_GET_PROFILE_COMMON_FROM_DB);
            Message msg = mHandler.obtainMessage(MSG_GET_PROFILE_COMMON_FROM_DB);
            msg.arg1 = result;
            msg.arg2 = SERVER_RESULT_SET_PROFILE;
            msg.sendToTarget();
        }
        if (result == ProfileService.OK) {
            getProfileQRCodeFromServer();
        }
    }

    /**
     * ProfileListener:
     * listener call back when portrait update done.
     * @param result:
     */
    public void onUpdateProfilePortrait(int result) {
        Log.i(TAG, "onUpdateProfilePortrait: result = " + result);
        if (result == ProfileService.OK || result == ProfileService.NOUPDATE) {
            notifyProfileInfoUpdate(result, SERVER_RESULT_SET_PORTRAIT);
        } else {
            removeMessageIfExist(MSG_GET_PORTRAIT_FROM_DB);
            Message msg = mHandler.obtainMessage(MSG_GET_PORTRAIT_FROM_DB);
            msg.arg1 = result;
            msg.arg2 = SERVER_RESULT_SET_PORTRAIT;
            msg.sendToTarget();
        }
        if (result == ProfileService.OK) {
            getProfileQRCodeFromServer();
        }
    }

   /**
     * ProfileListener:
     * listener when get Contact portrait get back.
     * @param result:
     * @param number:
     * @param portrait:
     */
   public void onGetContactPortrait (int result, String portrait, String number, String mimeType) {
       Log.i(TAG, "onGetContactPortrait: result = " + result + " number = " + number);
       byte[] photo = null;
       if (portrait != null) {
           photo = Base64.decode(portrait, Base64.DEFAULT);
       }
       notifyContactInfoUpdate(result, number, photo);
   }

    /**
     * ProfileListener:
     * listener when get Profile QR Code call back.
     * @param result:
     * @param mode:
     */
    public void onGetProfileQRCode (int result, int mode) {
        Log.d(TAG, "onGetProfileQRCode: result = " + result + " mode = " + mode);
        removeMessageIfExist(MSG_GET_PROFILE_QR_CODE_FROM_DB);
        Message msg = mHandler.obtainMessage(MSG_GET_PROFILE_QR_CODE_FROM_DB);
        msg.arg1 = result;
        msg.arg2 = mode;
        msg.sendToTarget();
        if (result == 0) {
            boolean qrmode = (mode == 1);
            SharedPreferences settings = mContext
                    .getSharedPreferences(mContext.getPackageName(), 0);
            settings.edit().putBoolean(BUSINESS_SETTING, qrmode).apply();
        }
    }

    /**
     * ProfileListener:
     * listener when get Profile QR Code mode call back.
     * @param result:
     * @param mode:
     */
    public void onUpdateProfileQRCodeMode (int result, int mode) {
        Log.d(TAG, "onUpdateProfileQRCodeMode: result = " + result + " mode = " + mode);
        SharedPreferences settings = mContext
                .getSharedPreferences(mContext.getPackageName(), 0);
        boolean qrmode;
        if (result == 0) {
            qrmode = (mode == 1);
            getProfileQRCodeFromServer();
        } else {
            qrmode = (mode == 0);
        }
        settings.edit().putBoolean(BUSINESS_SETTING, qrmode).apply();
        for (ProfileManagerListener listener: mListenerList) {
            listener.onUpdateProfileQRCodeMode(result, mode);
        }
    }

    public void onGetProfileQRCodeMode (int result, int mode) {

    }

}
