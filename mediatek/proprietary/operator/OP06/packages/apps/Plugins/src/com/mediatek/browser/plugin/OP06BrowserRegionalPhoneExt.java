
/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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
package com.mediatek.browser.plugin;

import android.content.Context;
import android.database.ContentObserver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BrowserContract;
import android.util.Log;
import android.os.Handler;

import com.mediatek.browser.ext.DefaultBrowserRegionalPhoneExt;
import com.mediatek.common.regionalphone.RegionalPhone;
import com.mediatek.common.PluginImpl;



@PluginImpl(interfaceName="com.mediatek.browser.ext.IBrowserRegionalPhoneExt")
public class OP06BrowserRegionalPhoneExt extends DefaultBrowserRegionalPhoneExt {

    private static final String TAG = "OP06BrowserRegionalPhoneExt";
    private static final String SHARED_PREFRENCE_STRING = "mcc_mnc_timestamp";
    private Context mContext = null;
    private SharedPreferences mPrefs;
    private Uri mBrowserUri = RegionalPhone.BROWSER_URI;
    private Uri mSearchEngineUri = RegionalPhone.SEARCHENGINE_URI;
    private static final String PREF_BKM_MCC_MNC_TIMESTAMP = "pref_bkm_mcc_mnc_timestamp";
    private static final String PREF_SRCH_MCC_MNC_TIMESTAMP = "pref_srch_mcc_mnc_timestamp";
    public static final String SEARCH_ENGINE_PREF = "search_engine";
    private static final int PARENT = 1;
    // intent action used to notify Browser that user has has changed search engine setting
    private static final String ACTION_BROWSER_SEARCH_ENGINE_CHANGED
            = "com.android.browser.SEARCH_ENGINE_CHANGED";
    private RegionalPhoneContentObserver mObserver = null;
    private Context appContext = null;
    private boolean sentBroadcast = false;

    public OP06BrowserRegionalPhoneExt(Context context) {
        mContext = context;
    }

    public void updateBookmarks(Context cntx) {
        Log.i("@M_" + TAG, "Enter: " + "updateBookmarks" + " --OP06 implement");
        String timeStampPref = readPrefValue(cntx, PREF_BKM_MCC_MNC_TIMESTAMP);
        String timeStampDB = null;
        boolean isUpdated = false;
        appContext = cntx;
        Log.i("@M_" + TAG, "timestamp in preference = " + timeStampPref);
        Cursor cr = mContext.getContentResolver().query(
                mBrowserUri,   // The content URI
                new String[]{RegionalPhone.BROWSER.MCC_MNC_TIMESTAMP}, // The columns to return for each row
                null,                    // Selection criteria
                null,                     // Selection criteria
                null);
        if (cr != null && cr.getCount() > 0) {
            cr.moveToFirst();
            timeStampDB = cr.getString(cr.getColumnIndex(RegionalPhone.BROWSER.MCC_MNC_TIMESTAMP));
            Log.i("@M_" + TAG, "timestamp read from DB = " + timeStampDB);
            cr.close();
            cr = null;
        } else {
            if (cr != null) {
                cr.close();
            }
            Log.i("@M_" + TAG, "Register contentobserver to listen database change. \n");
            mObserver = new RegionalPhoneContentObserver(new Handler());
            mContext.getContentResolver().registerContentObserver(mBrowserUri, true, mObserver);
        }
        if (timeStampPref == null || !(timeStampPref.equals(timeStampDB))) {
            Log.i("@M_" + TAG, "Need to read bkm from DB ");
            Cursor cursor = mContext.getContentResolver().query(
                mBrowserUri,   // The content URI
                new String[]{RegionalPhone.BROWSER._ID, RegionalPhone.BROWSER.BOOKMARK_URL, RegionalPhone.BROWSER.BOOKMARK_TITLE,
                    RegionalPhone.BROWSER.THUMBNAIL, RegionalPhone.BROWSER.IS_FOLDER, RegionalPhone.BROWSER.PARENT}, // The columns to return for each row
                    null,                    // Selection criteria
                    null,                     // Selection criteria
                    null);
//            Log.i("@M_" + TAG, "Number of rows in result " +cursor.getCount());
            if (cursor != null && cursor.getCount() > 0) {
                Log.i("@M_" + TAG, "Number of rows in result " + cursor.getCount());
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    int _id = cursor.getInt(cursor.getColumnIndex(RegionalPhone.BROWSER._ID));
                    String url = cursor.getString(cursor.getColumnIndex(RegionalPhone.BROWSER.BOOKMARK_URL));
                    String title = cursor.getString(cursor.getColumnIndex(RegionalPhone.BROWSER.BOOKMARK_TITLE));
                    byte[] thumbnail = cursor.getBlob(cursor.getColumnIndex(RegionalPhone.BROWSER.THUMBNAIL));
                    String is_folder = cursor.getString(cursor.getColumnIndex(RegionalPhone.BROWSER.IS_FOLDER));
                    String parent = cursor.getString(cursor.getColumnIndex(RegionalPhone.BROWSER.PARENT));
                    Log.i("@M_" + TAG, "DATA read from DB = " + url + "," + title + "," + thumbnail + "," + is_folder + "," + parent);
                    updatePrefValue(PREF_BKM_MCC_MNC_TIMESTAMP, timeStampDB);
                    try {
                        ContentValues values = new ContentValues();
                        values.put(BrowserContract.Bookmarks.TITLE, title);
                        values.put(BrowserContract.Bookmarks.URL, url);
                        values.put(BrowserContract.Bookmarks.IS_FOLDER, is_folder);
                        values.put(BrowserContract.Bookmarks.THUMBNAIL, thumbnail); //bitmapToBytes(thumbnail));
                        values.put(BrowserContract.Bookmarks.PARENT, parent);
                        Uri newUri = null;
                        int existId = sameNameBkmExist(title, PARENT);
                        Log.i("@M_" + TAG, "already exist result = " + existId);
                        if (existId != -1) {
                            newUri = ContentUris.withAppendedId(BrowserContract.Bookmarks.CONTENT_URI, existId);
                            Log.i("@M_" + TAG, "new URI updated after replace = " + newUri);
                            int rows = mContext.getContentResolver().update(newUri, values, null, null);
                            Log.i("@M_" + TAG, "update result = " + rows);
                        } else {
                        newUri = mContext.getContentResolver().insert(BrowserContract.Bookmarks.CONTENT_URI, values);
                        }
                        Log.i("@M_" + TAG, "new URI updated = " + newUri);
                        if (newUri != null) {
                            isUpdated = true;
                        }
                    } catch (IllegalStateException e) {
                        Log.e("@M_" + TAG, "exception when try to update ");
                    }
                    cursor.moveToNext();
                } // end of while
                cursor.close();
            }
            if (cursor != null) {
                cursor.close();
            }
        }
        Log.i("@M_" + TAG, "is updated = " + isUpdated);
        //return isUpdated;
    }

    public String getSearchEngine(SharedPreferences mPrefs, Context cntx) {

        Log.i("@M_" + TAG, "Enter: " + "getsearchengine" + " --OP06 implement");
        String timeStampPref = readPrefValue(cntx, PREF_SRCH_MCC_MNC_TIMESTAMP);
        Log.i("@M_" + TAG, "timestamp in preference = " + timeStampPref);
        String timeStampDB = null;
        String nameDB = null;
        appContext = cntx;
        Cursor cr = mContext.getContentResolver().query(
                mSearchEngineUri,   // The content URI
                new String[]{RegionalPhone.SEARCHENGINE.MCC_MNC_TIMESTAMP, RegionalPhone.SEARCHENGINE.SEARCH_ENGINE_NAME}, // The columns to return for each row
                null,                    // Selection criteria
                null,                     // Selection criteria
                null);
        if (cr != null && cr.getCount() > 0) {
            cr.moveToFirst();
            timeStampDB = cr.getString(cr.getColumnIndex(RegionalPhone.SEARCHENGINE.MCC_MNC_TIMESTAMP));
            nameDB = cr.getString(cr.getColumnIndex(RegionalPhone.SEARCHENGINE.SEARCH_ENGINE_NAME));
            Log.i("@M_" + TAG, "timestamp read from DB = " + timeStampDB);
            Log.i("@M_" + TAG, "nameDB read from DB = " + nameDB);
        }
        if (cr != null) {
            cr.close();
        }
        if (timeStampPref == null || !timeStampPref.equals(timeStampDB)) {
            Log.i("@M_" + TAG, "Need to read search engine from DB ");
            Log.i("@M_" + TAG, "searchengine = " + nameDB);
            updatePrefValue(PREF_SRCH_MCC_MNC_TIMESTAMP, timeStampDB);
            return nameDB;
        }
    return null;
    }

    private String readPrefValue(Context cntx, String prefKey) {
        mPrefs = cntx.getSharedPreferences(SHARED_PREFRENCE_STRING, 0);
        String timeStampValue = mPrefs.getString(prefKey, null);
        return timeStampValue;
    }

    private void updatePrefValue(String prefKey, String prefValue) {
        SharedPreferences.Editor edit = mPrefs.edit();
        edit.putString(prefKey, prefValue);
        edit.commit();
    }

    private int sameNameBkmExist(String bkmToAdd, int parentId) {
        String where = BrowserContract.Bookmarks.PARENT + " = ?"
                + " AND " + BrowserContract.Bookmarks.TITLE + " = ?"
                + " AND " + BrowserContract.Bookmarks.IS_FOLDER + "= 0";
        Cursor cursor = mContext.getContentResolver().query(
            BrowserContract.Bookmarks.CONTENT_URI,
            new String[]{BrowserContract.Bookmarks._ID},
            where,
            new String[]{String.valueOf(parentId), bkmToAdd},
            null);
        Log.d("@M_" + TAG, "Query for same name " + bkmToAdd);
        if (cursor != null) {
            Log.d("@M_" + TAG, "cursor not null, value = " + cursor);
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getInt(0);
                }
            } finally {
                cursor.close();
            }
        }
        Log.d("@M_" + TAG, "cursor is null, no duplicates");
        return -1;
    }


/**
 *
 * @author mtk33075
 *
 */
private class RegionalPhoneContentObserver extends ContentObserver {

    public RegionalPhoneContentObserver(Handler handler) {
        super(handler);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        Log.i("@M_" + TAG, "onChange(boolean selfChange, Uri uri)" + selfChange + ", " + uri.toString());
        Log.i("@M_" + TAG, "Content observer called, inside it");
        String timeStampDB = null;
        String timeStampBkmPref = null;
        String timeStampSearchPref = null;
        String nameDB = null;
        Cursor cr = mContext.getContentResolver().query(
                mSearchEngineUri,   // The content URI
                new String[]{RegionalPhone.SEARCHENGINE.MCC_MNC_TIMESTAMP, RegionalPhone.SEARCHENGINE.SEARCH_ENGINE_NAME}, // The columns to return for each row
                null,                    // Selection criteria
                null,                     // Selection criteria
                null);
        timeStampBkmPref = readPrefValue(appContext, PREF_BKM_MCC_MNC_TIMESTAMP);
        timeStampSearchPref = readPrefValue(appContext, PREF_SRCH_MCC_MNC_TIMESTAMP);
        if (cr != null && cr.getCount() > 0) {
            cr.moveToFirst();
            timeStampDB = cr.getString(cr.getColumnIndex(RegionalPhone.SEARCHENGINE.MCC_MNC_TIMESTAMP));
            nameDB = cr.getString(cr.getColumnIndex(RegionalPhone.SEARCHENGINE.SEARCH_ENGINE_NAME));
            Log.i("@M_" + TAG, "In Contentobserver:timestamp read from DB = " + timeStampDB);
        }
        if (cr != null) {
            cr.close();
        }
        //if(timeStampBkmPref == null || !(timeStampBkmPref.equals(timeStampDB)))
         {
            Log.i("@M_" + TAG, "In Contentobserver:Need to read bkm from DB ");
            Cursor cursor = mContext.getContentResolver().query(
                    uri,   // The content URI
                new String[]{RegionalPhone.BROWSER._ID, RegionalPhone.BROWSER.BOOKMARK_URL, RegionalPhone.BROWSER.BOOKMARK_TITLE,
                    RegionalPhone.BROWSER.THUMBNAIL, RegionalPhone.BROWSER.IS_FOLDER, RegionalPhone.BROWSER.PARENT}, // The columns to return for each row
                    null,                    // Selection criteria
                    null,                     // Selection criteria
                    null);
//            Log.i("@M_" + TAG, "In Contentobserver:Number of rows in result " +cursor.getCount());
            if (cursor != null && cursor.getCount() > 0) {
                Log.i("@M_" + TAG, "In Contentobserver:Number of rows in result " + cursor.getCount());
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    int _id = cursor.getInt(cursor.getColumnIndex(RegionalPhone.BROWSER._ID));
                    String url = cursor.getString(cursor.getColumnIndex(RegionalPhone.BROWSER.BOOKMARK_URL));
                    String title = cursor.getString(cursor.getColumnIndex(RegionalPhone.BROWSER.BOOKMARK_TITLE));
                    byte[] thumbnail = cursor.getBlob(cursor.getColumnIndex(RegionalPhone.BROWSER.THUMBNAIL));
                    String is_folder = cursor.getString(cursor.getColumnIndex(RegionalPhone.BROWSER.IS_FOLDER));
                    String parent = cursor.getString(cursor.getColumnIndex(RegionalPhone.BROWSER.PARENT));
                    Log.i("@M_" + TAG, "In Contentobserver:DATA read from DB = " + url + "," + title + "," + thumbnail + "," + is_folder + "," + parent);
                    updatePrefValue(PREF_BKM_MCC_MNC_TIMESTAMP, timeStampDB);
                    try {
                        ContentValues values = new ContentValues();
                        values.put(BrowserContract.Bookmarks.TITLE, title);
                        values.put(BrowserContract.Bookmarks.URL, url);
                        values.put(BrowserContract.Bookmarks.IS_FOLDER, is_folder);
                        values.put(BrowserContract.Bookmarks.THUMBNAIL, thumbnail); //bitmapToBytes(thumbnail));
                        values.put(BrowserContract.Bookmarks.PARENT, parent);
                        Uri newUri = null;
                        int existId = sameNameBkmExist(title, PARENT);
                        Log.i("@M_" + TAG, "In contentobserver:already exist result = " + existId);
                        if (existId != -1) {
                            newUri = ContentUris.withAppendedId(BrowserContract.Bookmarks.CONTENT_URI, existId);
                            Log.i("@M_" + TAG, "In contentobserver:new URI updated after replace = " + newUri);
                            int rows = mContext.getContentResolver().update(newUri, values, null, null);
                            Log.i("@M_" + TAG, "In contentobserver:update result = " + rows);
                        } else {
                        newUri = mContext.getContentResolver().insert(BrowserContract.Bookmarks.CONTENT_URI, values);
                        }
                        Log.i("@M_" + TAG, "In Contentobserver:new URI updated = " + newUri);
                        if (newUri != null) {
                            //isUpdated = true;
                        }
                    } catch (IllegalStateException e) {
                        Log.e("@M_" + TAG, "In Contentobserver:exception when try to update ");
                    }
                    cursor.moveToNext();
                } // end of while
                cursor.close();
            }
            if (cursor != null) {
                cursor.close();
            }
        }
        if (timeStampSearchPref == null || !timeStampSearchPref.equals(timeStampDB)) {
            Log.i("@M_" + TAG, "In Contentobserver:Need to read search engine from DB ");
            Log.i("@M_" + TAG, "In Contentobserver:searchengine = " + nameDB);
            updatePrefValue(PREF_SRCH_MCC_MNC_TIMESTAMP, timeStampDB);
        }
        if (nameDB != null) {
            if (!sentBroadcast) {
            Intent intent = new Intent(ACTION_BROWSER_SEARCH_ENGINE_CHANGED);
            intent.setPackage("com.android.browser");
            /// M: using getDefaultSearchEngineName for flexibility @{
            intent.putExtra(SEARCH_ENGINE_PREF, nameDB);
                Log.i("@M_" + TAG, "In Contentobserver:sending broadcast");
            mContext.sendBroadcast(intent);
                sentBroadcast = true;
        }
        }

//        mContext.getContentResolver().unregisterContentObserver(mObserver);
    }
 public void onChange(boolean selfChange) {
        super.onChange(selfChange);
}
}


}