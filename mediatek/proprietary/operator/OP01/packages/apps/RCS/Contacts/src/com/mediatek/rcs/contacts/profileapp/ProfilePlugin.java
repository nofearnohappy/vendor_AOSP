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

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import android.widget.ImageView;

import com.mediatek.rcs.contacts.R;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * ProfilePlugin: Contact update profile icon from profile server.
 */
public class ProfilePlugin implements ProfileManager.ProfileManagerListener{

    private static ProfilePlugin sInstance;
    private Context mContext;

    private static int STATE_NOT_UPDATED = 0;
    private static int STATE_UPDATING = 1;
    private static int STATE_UPDATED = 2;
    private static int STATE_PAUSED = 3;

    // Added Local Account Type
    public static final String ACCOUNT_TYPE_SIM = "SIM Account";
    public static final String ACCOUNT_TYPE_USIM = "USIM Account";
    public static final String ACCOUNT_TYPE_UIM = "UIM Account";


    private static HashMap<Long, Thread> sRunningMap = new HashMap<Long, Thread>();
    private static final String TAG = "RCSProfilePlugin";

    private ProfilePlugin() {

    }

    public static ProfilePlugin getInstance() {
        if (sInstance == null) {
            sInstance = new ProfilePlugin();
        }
        return sInstance;
    }

    /**
     * Interface called by op01 plugin api.
     * @param: rawContactId
     * @param: context
     */
    public void getContactPhotoFromServer(long rawContactId, Context context, ImageView photo) {
        mContext = context;
        if (sRunningMap.containsKey(rawContactId)) {
            Log.d(TAG, "Exist job id" + rawContactId);
            return;
        }

        ProfileManager pm = ProfileManager.getInstance(mContext);
        pm.registerProfileManagerListener(this);
        ContactInfoCache cache = new ContactInfoCache(
                rawContactId, photo, null, STATE_NOT_UPDATED, 0);
        Thread iThread = new ServerContactUpdateThread(
                String.valueOf(rawContactId), cache);
        iThread.start();

        sRunningMap.put(rawContactId, iThread);
    }

    /**
     * Query if a contact has primary photo.
     * @param rawContactId
     * @param primary primary number: 1 / 0
     * @return :true / false
     */
    private synchronized boolean isContactHasPrimaryPhoto(long rawContactId, int primary) {

        String[] projections = {Data._ID};
        String selection = Data.MIMETYPE + " = ?"
                + " AND (" + Data.RAW_CONTACT_ID + " = ?)"
                + " AND (" + Photo.IS_PRIMARY + " = ?)";
        String[] selectionArgs = new String[] {
                Photo.CONTENT_ITEM_TYPE,
                String.valueOf(rawContactId),
                String.valueOf(primary)
        };

        Cursor c = mContext.getContentResolver().query(
                ContactsContract.Data.CONTENT_URI,
                projections, selection, selectionArgs, null, null);

        boolean ret = (c != null) && (c.getCount() >= 1);
        c.close();
        return ret;
    }

    /**
     * get contact numbers by getAllContactNumbers.
     * @param rawContactId getAllContactNumbers.
     * @return number of raw contact id.
     */
    private synchronized ArrayList<String> getAllContactNumbers(long rawContactId) {
        ArrayList<String> numbers = new ArrayList<String>();

        String[] projections = {Data._ID, Phone.NUMBER};
        String selection = Data.MIMETYPE + " = ?"
                + " AND (" + Data.RAW_CONTACT_ID + " = ?)";
        String[] selectionArgs = new String[] {
                Phone.CONTENT_ITEM_TYPE,
                String.valueOf(rawContactId)
        };

        Cursor c = mContext.getContentResolver().query(
                ContactsContract.Data.CONTENT_URI,
                projections, selection, selectionArgs, null, null);

        if (c != null) {
            while (c.moveToNext()) {
                String number = c.getString(c.getColumnIndex(Phone.NUMBER));
                numbers.add(number.replaceAll(" ", ""));
            }
        }
        c.close();

        return numbers;
    }

    /**
     * get contact account type.
     * @param rawContactId .
     * @return account type string.
     */
    private synchronized String getAccountType(long rawContactId) {
        String accountType = null;
        Uri uri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId);
        String[] projections = {RawContacts._ID, RawContacts.ACCOUNT_TYPE};

        Cursor c = mContext.getContentResolver().query(
                uri, projections, null, null, null, null);

        if (c != null && c.getCount() > 0) {
            if (c.moveToFirst()) {
                accountType = c.getString(c.getColumnIndex(RawContacts.ACCOUNT_TYPE));
            }
        }
        c.close();
        return accountType;

    }

    /**
     * M: if the account name is one of Icc Card account names, like
     * "USIM Account" return true, otherwise, means the account is not a
     * SIM/USIM/UIM account.
     *
     * FIXME: this implementation is not good, not OO. should try to remove it
     * in future refactor
     *
     * @param accountTypeString
     *            generally, it's a string like "USIM Account" or
     *            "Local Phone Account"
     * @return if it's a IccCard account, return true, otherwise false.
     */
    private synchronized boolean isAccountTypeIccCard(String accountTypeString) {
        boolean isIccCardAccount = (ACCOUNT_TYPE_SIM.equals(accountTypeString)
                || ACCOUNT_TYPE_USIM.equals(accountTypeString)
                || ACCOUNT_TYPE_UIM.equals(accountTypeString));
        Log.d(TAG, "account " + accountTypeString + " is IccCard? " + isIccCardAccount);
        return isIccCardAccount;
    }

    /**
     * Update contact photo to contact db.
     * @param rawContactId contact raw contact id
     * @param photo photo data
     */
    private synchronized void setContactPhotoToDB(long rawContactId, byte[] photo) {
        if (photo != null) {
            if (ProfilePhotoUtils.isGifFormatStream(photo)) {
                Bitmap map = ProfilePhotoUtils.getGifFrameBitmap(photo, 0);
                ByteArrayOutputStream outstream = new ByteArrayOutputStream();
                map.compress(Bitmap.CompressFormat.PNG, 100, outstream);
                photo = outstream.toByteArray();
            }
        }

        ContentValues values = new ContentValues();
        values.put(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
        values.put(Data.RAW_CONTACT_ID, rawContactId);
        values.put(Photo.PHOTO, photo);
        Log.d(TAG, "Is photo null" + (photo == null));

        if (isContactHasPrimaryPhoto(rawContactId, 0)) {
            String[] projections = {Data._ID, Phone.NUMBER};
            String selection = Data.MIMETYPE + " = ?"
                    + " AND (" + Data.RAW_CONTACT_ID + " = ?)"
                    + " AND (" + Photo.IS_PRIMARY + " = ?)";
            String[] selectionArgs = new String[] {
                    ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE,
                    String.valueOf(rawContactId),
                    String.valueOf(0)
            };
            mContext.getContentResolver().update(
                    ContactsContract.Data.CONTENT_URI,
                    values, selection, selectionArgs);
        } else {
            mContext.getContentResolver().insert(
                    ContactsContract.Data.CONTENT_URI, values);
        }
    }

    /**
     * ProfileListener:
     * listener when get Profile QR Code call back.
     * @param result:
     * @param mode:
     */
    public void onGetProfileQRCode (int result, int mode) {
        Log.d(TAG, "onGetProfileQRCode: result = " + result + " mode = " + mode);

    }

    /**
     * ProfileListener:
     * listener when get Profile QR Code mode call back.
     * @param result:
     * @param mode:
     */
    public void onUpdateProfileQRCodeMode (int result, int mode) {
        Log.d(TAG, "onUpdateProfileQRCodeMode: result = " + result + " mode = " + mode);

    }

    /* Notify profile updating information. */
    public void onProfileInfoUpdated(int flag, int operation, ProfileInfo profile) {

    }

    /**
     * Update call back get contact icon
      * @param flag Server update flag
     * @param number Contact number
     * @param icon Photo
     */
    public void onContactIconGotten(int flag, String number, byte[]icon) {

        Log.i(TAG, "onContactIconGotten : flag = " + flag + "number = " + number);
        Collection<Thread> values = sRunningMap.values();

        for (Thread thread : values) {

            ServerContactUpdateThread updateThread = (ServerContactUpdateThread)thread;
            if (updateThread.getUpdateNumber().equals(number)) {

                if (flag == 0) {
                    Log.i(TAG, "onContactIconGotten : setContactPhotoToDB");
                    setContactPhotoToDB(updateThread.getContactId(), icon);
                    updateThread.setUpdateState(STATE_UPDATED);
                    updateThread.restartThread();
                    sRunningMap.remove(updateThread.getContactId());
                } else {
                    updateThread.restartThread();
                }
            }
        }
    }

    /**
     * ContactUpdateInfoCache : Contact info data
     */
    class ContactInfoCache {

        public ArrayList<String> numbers = new ArrayList<String>();
        public long rawContactId;
        public int state;
        public int updateIndex;
        public ImageView photo;

        public ContactInfoCache(long rawContactId, ImageView photo,
                                ArrayList<String> numbers, int state, int updateIndex) {
            //this.numbers.addAll(numbers);
            this.rawContactId = rawContactId;
            this.state = state;
            this.updateIndex = updateIndex;
            this.photo = photo;
        }
    }

    /**
     * ServerContactUpdateThread .
     * Get contact icon from server number by number.
     * uitil get one available photo.
     */
    private class ServerContactUpdateThread extends Thread {
        ContactInfoCache mInfo;

        ServerContactUpdateThread(String name, ContactInfoCache info) {
            super(name);
            mInfo = info;

        }

        @Override
        public void run() {

            if (isContactHasPrimaryPhoto(mInfo.rawContactId, 1)) {
                Log.d(TAG, "Has primary photo portrait!");
                stopThread();
                return;
            }

            ArrayList<String> numbers = getAllContactNumbers(mInfo.rawContactId);
            if (numbers.size() < 1) {
                stopThread();
                return;
            }
            mInfo.numbers.clear();
            mInfo.numbers.addAll(numbers);
            Log.d(TAG, numbers.toString());

            String accountType = getAccountType(mInfo.rawContactId);

            if (accountType == null || isAccountTypeIccCard(accountType)) {
                Log.d(TAG, "accountType: " + accountType + " stop thread!");
                stopThread();
                return;
            }

            while (mInfo.updateIndex < mInfo.numbers.size()
                    && mInfo.state != STATE_UPDATED) {
                Log.i(TAG, "ServerContactUpdateThread: State = " + mInfo.state
                        + "updateIndex = " + mInfo.updateIndex);
                setUpdateState(STATE_UPDATING);
                ProfileManager.getInstance(mContext).getContactPortraitByNumber(getUpdateNumber());
                pauseThread();
            }
            stopThread();
        }

        public int getUpdateState() {
            return mInfo.state;
        }

        public void setUpdateState(int state) {
            mInfo.state = state;
        }

        public long getContactId() {
            return mInfo.rawContactId;
        }

        public String getUpdateNumber() {
            Log.i(TAG, "getUpdateNumber : index=" + mInfo.updateIndex
                    + "numbers" + mInfo.numbers);
            Log.d(TAG, "getUpdateNumber : threadId:" + getId());
            String number = "";
            if (mInfo.updateIndex < mInfo.numbers.size()) {
                number = mInfo.numbers.get(mInfo.updateIndex);
            }
            Log.d(TAG, "getUpdateNumber : number=" + number);
            return number;
        }

        public void restartThread() {
            Log.d(TAG, "restartThread : updateIndex =" + mInfo.updateIndex + " Id=" + getId());
            mInfo.updateIndex++;
            synchronized (this) {
                notify();
            }
        }

        public void stopThread() {
            Log.d(TAG, "stopThread : Id =" + getId());

            sRunningMap.remove(mInfo.rawContactId);
        }

        public void pauseThread() {
            if (getState() == State.RUNNABLE) {
                synchronized (this) {
                    try {
                        setUpdateState(STATE_PAUSED);
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

}
