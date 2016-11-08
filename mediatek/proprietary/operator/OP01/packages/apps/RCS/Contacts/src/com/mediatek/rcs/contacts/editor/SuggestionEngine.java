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

package com.mediatek.rcs.contacts.editor;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.location.CountryDetector;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts.Photo;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.widget.EditText;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

public class SuggestionEngine extends HandlerThread {
    private static final String TAG = SuggestionEngine.class.getSimpleName();

    private static final int MESSAGE_NUMBER_CHANGE = 1;
    private static final int MESSAGE_SEND_CURSOR = 2;
    private static final int MAX_SUGGESTION_COUNT = 3;
    private static final long SUGGESTION_LOOKUP_DELAY_MILLIS = 300;
    private static final String ACCOUNT_TYPE_LOCAL_PHONE = "Local Phone Account";
      
    private Context mContext;
    private Handler mHandler;
    private Handler mMainHandler;
    private Cursor mCursor;
    private Listener mListener;
    private ArrayList<byte[]> mPhotoCache;
    private long mContactId;
    private HashMap<Integer, List<Long>> mRawContactIds;
    private String mCountryCode;

    public interface Listener {
        void onSuggestionChange(EditText view);
    }

    private static final String[] PHOTO_PROJECTION = new String[] { Photo.PHOTO };

    private static final String[] RAW_CONTACT_PROJECTION = new String[] { RawContacts._ID };

    private static final String[] PHONES_PROJECTION = new String[] {
            Phone.NUMBER, 
            Phone.DISPLAY_NAME, 
            Phone.CONTACT_ID, 
            Phone.LOOKUP_KEY, 
            Phone.PHOTO_ID,
            RawContacts._ID,
    };

    public static final int PHONE_NUMBER   = 0;
    public static final int DISPLAY_NAME   = 1;
    public static final int CONTACT_ID     = 2;
    public static final int LOOKUP_KEY     = 3;
    public static final int PHOTO_ID       = 4;
    public static final int RAW_CONTACT_ID = 5;

    public final class SearchData {
        public String mNumber;
        public EditText mEdit;
        public long mContactId;

        SearchData (String number, EditText edit, long id) {
            mNumber = number;
            mEdit = edit;
            mContactId = id;
        }
    }

    public static final class Suggestion {

        public long contactId;
        public String lookupKey;
        public String name;
        public String number;
        public byte[] photo;
        public List<Long> rawContacts;

        @Override
        public String toString() {
            return "ID: " + contactId + " lookupKey: " + lookupKey
            + " name: " + name + " number: " + number + " photo: " + photo.toString();
        }
    }

    public SuggestionEngine(Context context) {
        super("SuggestionEngine", Process.THREAD_PRIORITY_BACKGROUND);
        mContext = context;
        mMainHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                SuggestionEngine.this.deliverResult((EditText) msg.obj);
            }
        };
        mPhotoCache = new ArrayList(MAX_SUGGESTION_COUNT);
        mRawContactIds = new HashMap<Integer, List<Long>>(MAX_SUGGESTION_COUNT);
        setCountryCode();
    }

    private void setCountryCode() {
        if (mCountryCode == null) {
            final CountryDetector countryDetector =
                    (CountryDetector) mContext.getSystemService(Context.COUNTRY_DETECTOR);
            mCountryCode = countryDetector.detectCountry().getCountryIso();
        }
        Log.d(TAG, "setCountryCode: " + mCountryCode);
    }
    
    protected Handler getHandler() {
        if (mHandler == null) {
            mHandler = new Handler(getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case MESSAGE_NUMBER_CHANGE:
                            loadSuggestions((SearchData) msg.obj);
                            break;

                        default:
                            Log.e(TAG, "handleMessage unknown");
                            break;
                    }
                }
            };
        }
        return mHandler;
    }

    public void scheduleSuggestionLookup(String number, EditText view) {
        Log.d(TAG, "scheduleSuggestionLookup");
        Handler handler = getHandler();
        handler.removeMessages(MESSAGE_NUMBER_CHANGE);
        SearchData data = null;
        data = new SearchData(number, view, mContactId);
        Message msg = handler.obtainMessage(MESSAGE_NUMBER_CHANGE, data);
        handler.sendMessageDelayed(msg, SUGGESTION_LOOKUP_DELAY_MILLIS);
    }

    private void loadPhoto(long photoId, int index) {
        final StringBuilder selection = new StringBuilder();       
        selection.append(Photo._ID + "=?");
        String[] selectArgs = new String[] { Long.toString(photoId) };
        
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(Data.CONTENT_URI,
                    PHOTO_PROJECTION, selection.toString(), selectArgs, null);
        
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    byte[] bytes = cursor.getBlob(0);
                    mPhotoCache.add(index, bytes);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void loadRawContacts(long contactId, int index) {
        final StringBuilder selection = new StringBuilder();       
        selection.append(RawContacts.CONTACT_ID + "=?");
        String[] selectArgs = new String[] { Long.toString(contactId) };
        
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(RawContacts.CONTENT_URI,
                    RAW_CONTACT_PROJECTION, selection.toString(), selectArgs, null);
        
            if (cursor != null) {
                List<Long> list = mRawContactIds.get(index);
                if (list == null) {
                    Log.i(TAG, "loadRawContacts new RawContact list: " + index);
                    list = Lists.newArrayList();
                }
                
                while (cursor.moveToNext()) {
                    long rawContactId = cursor.getLong(0);
                    if (!containsRawContact(list, rawContactId)) {
                        Log.i(TAG, "loadRawContacts add RawContact: " + rawContactId);
                        list.add(rawContactId);                       
                    }                  
                }
                mRawContactIds.put(index, list);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private Cursor filterCursor(Cursor originalCursor) {
        if (originalCursor == null) {
            Log.w(TAG, "filterCursor is null.");
            return null;
        }

        if (originalCursor.getCount() == 0) {
            Log.w(TAG, "filterCursor no count.");
            originalCursor.close();
            return null;
        }

        MatrixCursor cursor = new MatrixCursor(originalCursor.getColumnNames());
        long currentContactId = -1;
        int itemCounts = 0;
        while (originalCursor.moveToNext()) {                                  
            long contactId = originalCursor.getLong(CONTACT_ID);
            if (contactId != currentContactId) {
                Object[] columnArray = new Object[PHONES_PROJECTION.length];
                columnArray[0] = originalCursor.getString(PHONE_NUMBER);
                columnArray[1] = originalCursor.getString(DISPLAY_NAME);
                columnArray[2] = contactId;
                columnArray[3] = originalCursor.getString(LOOKUP_KEY);
                columnArray[4] = originalCursor.getLong(PHOTO_ID);

                try {
                    itemCounts++;
                    if (itemCounts > MAX_SUGGESTION_COUNT) {
                        Log.i(TAG, "filterCursor to max count");
                        break;
                    }
                    cursor.addRow(columnArray);
                } catch (Exception e) {
                    Log.e(TAG, "filterCursor error");
                }
                        
                currentContactId = contactId;
                Log.i(TAG, "currentContactId changed: " + currentContactId);
            }
        }

        if (originalCursor != null) {
            originalCursor.close();
        }
        Log.i(TAG, "filterCursor counts:" + cursor.getCount());
        return cursor;
    }

    private void loadSuggestions(SearchData searchData) {
        String number = searchData.mNumber;
        Log.d(TAG, "loadSuggestions number: " + number);
        long contactId = searchData.mContactId;
        Log.d(TAG, "loadSuggestions contactId: " + contactId);
        
        Builder builder = Phone.CONTENT_URI.buildUpon();
        //Builder builder = Phone.CONTENT_FILTER_URI.buildUpon();
        //builder.appendPath(number);
        builder.appendQueryParameter(ContactsContract.REMOVE_DUPLICATE_ENTRIES, "true");  
        Uri uri = builder.build();

        final StringBuilder selection = new StringBuilder();
        selection.append("(");
        selection.append("(" + "(" + RawContacts.ACCOUNT_TYPE + " IS NULL " 
                + " AND " + RawContacts.ACCOUNT_NAME + " IS NULL )"
                + " OR " + "(" + RawContacts.ACCOUNT_TYPE + "=? )");
        selection.append(")");
        selection.append(" AND " + RawContacts.CONTACT_ID + "!=?");
        selection.append(" AND ");
        selection.append("(" + "(" + Phone.NUMBER + "=? )");
        String numberE164 = PhoneNumberUtils.formatNumberToE164(number, mCountryCode);
        Log.d(TAG, "loadSuggestions numberE164: " + numberE164);
        if (numberE164 != null) {
           selection.append(" OR " + "(" + Phone.NORMALIZED_NUMBER + "=? )");
        }
        selection.append(")");
        selection.append(")");
        Log.d(TAG, "loadSuggestions selection: " + selection.toString());
        
        String[] selectArgs;
        if (numberE164 == null) {
            selectArgs = new String[] { ACCOUNT_TYPE_LOCAL_PHONE,
                    String.valueOf(contactId), number };
        } else {
            selectArgs = new String[] { ACCOUNT_TYPE_LOCAL_PHONE,
                    String.valueOf(contactId), number, numberE164 };
        }
        Cursor cursor = null;
               
        cursor = mContext.getContentResolver().query(uri, PHONES_PROJECTION, 
                selection.toString(), selectArgs, Phone.CONTACT_ID);

        Cursor newCursor = null;
        newCursor = filterCursor(cursor);
        
        if (newCursor != null) {
            // If a new request is pending, discard old one
            if (getHandler().hasMessages(MESSAGE_NUMBER_CHANGE)) {
                Log.d(TAG, "loadSuggestions new message coming");
                newCursor.close();
                return;
            }
            
            newCursor.moveToPosition(-1);
            mPhotoCache.clear();
            mRawContactIds.clear();
            for (int i = 0; i < newCursor.getCount(); i++) {
                newCursor.moveToNext();
                long photoId = newCursor.getLong(PHOTO_ID);
                if (photoId != 0) {
                    loadPhoto(photoId, i);
                } else {
                    mPhotoCache.add(i, null);
                }
                long contact = newCursor.getLong(CONTACT_ID);
                loadRawContacts(contact, i);
            }

            setCursor(newCursor);
            EditText view = searchData.mEdit;
            Message msg = mMainHandler.obtainMessage(MESSAGE_SEND_CURSOR, view);
            mMainHandler.sendMessage(msg);           
        }
    }

    private void setCursor(Cursor dataCursor) {
        Log.i(TAG, "setCursor");
        if (mCursor != null) {
            mCursor.close();
        }
        mCursor = dataCursor;
    }

    public void setContactId(long contactId) {
        if (contactId != mContactId) {            
            mContactId = contactId;
        }
    }

    public int getSuggestedCount() {
        return mCursor != null ? mCursor.getCount() : 0;
    }

    private void deliverResult(EditText view) {       
        if (mListener != null) {
            Log.i(TAG, "deliverResult");
            mListener.onSuggestionChange(view);
        }
    }

    @Override
    public boolean quit() {
        Log.i(TAG, "handle quit");
        if (mCursor != null) {
            mCursor.close();
        }
        mCursor = null;
        return super.quit();
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public boolean containsRawContact(List<Long> list, long rawContactId) {
        if (list != null) {
            int count = list.size();
            for (int i = 0; i < count; i++) {
                if (list.get(i) == rawContactId) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<Suggestion> getSuggestions() {
        ArrayList<Suggestion> list = Lists.newArrayList();
        if (mCursor != null) {
            Suggestion suggestion = null;
            mCursor.moveToPosition(-1);
            for (int i = 0; i < mCursor.getCount(); i++) {
                mCursor.moveToNext();
                suggestion = new Suggestion();
                suggestion.contactId = mCursor.getLong(CONTACT_ID);                    
                suggestion.lookupKey = mCursor.getString(LOOKUP_KEY);
                suggestion.name = mCursor.getString(DISPLAY_NAME);
                suggestion.number = mCursor.getString(PHONE_NUMBER);
                suggestion.photo = mPhotoCache.get(i);
                suggestion.rawContacts = mRawContactIds.get(i);   
                list.add(suggestion);
            }
        }
        return list;
    }
}
