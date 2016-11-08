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

package com.mediatek.rcs.contacts.provider;

import com.mediatek.rcs.contacts.profileservice.utils.*;

import java.util.HashMap;
import android.content.Context;
import android.content.ContentProvider;
import android.content.UriMatcher;
import android.content.ContentUris;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.database.sqlite.SQLiteQueryBuilder;
import android.content.ContentResolver;
import android.content.ContentValues;


public class ProfileProvider extends ContentProvider {
    private static final String TAG = "ProfileProvider";
    private static final String AUTHORITY = "com.cmcc.ccs.profile";
    private static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    private DatabaseHelper mHelper = null;
    private ContentResolver mResolver = null;
    private static final String DB_NAME = "profile.db";
    private static final String DB_TABLE = "profileTable";
    private static final int PROFILE = 1;

    private static final String ID = "_id";
    private static final String PHONE_NUMBER = ProfileConstants.PHONE_NUMBER;
    private static final String FIRST_NAME = ProfileConstants.FIRST_NAME;
    private static final String LAST_NAME = ProfileConstants.LAST_NAME;
    private static final String PORTRAIT = ProfileConstants.PORTRAIT;
    private static final String PORTRAIT_TYPE = ProfileConstants.PORTRAIT_TYPE;
    private static final String ADDRESS = ProfileConstants.ADDRESS;
    private static final String PHONE_NUMBER_SECOND = ProfileConstants.PHONE_NUMBER_SECOND;
    private static final String EMAIL = ProfileConstants.EMAIL;
    private static final String BIRTHDAY = ProfileConstants.BIRTHDAY;
    private static final String COMPANY = ProfileConstants.COMPANY;
    private static final String COMPANY_TEL = ProfileConstants.COMPANY_TEL;
    private static final String TITLE = ProfileConstants.TITLE;
    private static final String COMPANY_ADDR = ProfileConstants.COMPANY_ADDR;
    private static final String COMPANY_FAX = ProfileConstants.COMPANY_FAX;
    private static final String PCC_ETAG = ProfileConstants.PCC_ETAG;
    private static final String PORTRAIT_ETAG = ProfileConstants.PORTRAIT_ETAG;
    //for QRcode
    private static final String QRCODE = ProfileConstants.QRCODE;
    private static final String QRCODE_ETAG = ProfileConstants.QRCODE_ETAG;

    private static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, null, PROFILE);
    }

    private static final HashMap<String, String> profileProjectionMap;

    static {
        profileProjectionMap = new HashMap<String, String>();
        profileProjectionMap.put(ID, ID);
        profileProjectionMap.put(PHONE_NUMBER, PHONE_NUMBER);
        profileProjectionMap.put(FIRST_NAME, FIRST_NAME);
        profileProjectionMap.put(LAST_NAME, LAST_NAME);
        profileProjectionMap.put(PORTRAIT, PORTRAIT);
        profileProjectionMap.put(PORTRAIT_TYPE, PORTRAIT_TYPE);
        profileProjectionMap.put(ADDRESS, ADDRESS);
        profileProjectionMap.put(PHONE_NUMBER_SECOND, PHONE_NUMBER_SECOND);
        profileProjectionMap.put(EMAIL, EMAIL);
        profileProjectionMap.put(BIRTHDAY, BIRTHDAY);
        profileProjectionMap.put(COMPANY, COMPANY);
        profileProjectionMap.put(COMPANY_TEL, COMPANY_TEL);
        profileProjectionMap.put(TITLE, TITLE);
        profileProjectionMap.put(COMPANY_ADDR, COMPANY_ADDR);
        profileProjectionMap.put(COMPANY_FAX, COMPANY_FAX);
        profileProjectionMap.put(PCC_ETAG, PCC_ETAG);
        profileProjectionMap.put(PORTRAIT_ETAG, PORTRAIT_ETAG);
        //for  QRCode
        profileProjectionMap.put(QRCODE, QRCODE);
        profileProjectionMap.put(QRCODE_ETAG, QRCODE_ETAG);
    }

    @Override
    public boolean onCreate(){
        mResolver = getContext().getContentResolver();
        mHelper = new DatabaseHelper(getContext());
        return (mHelper != null);
    }

    @Override
    public Cursor query(Uri uri, String[] projection,
        String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = mHelper.getReadableDatabase();
        SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
        switch (uriMatcher.match(uri)) {
            case PROFILE:
                sqlBuilder.setTables(DB_TABLE);
                sqlBuilder.setProjectionMap(profileProjectionMap);
                break;

            default:
                //error log
                break;
        }
        Cursor cursor = sqlBuilder.query(db, projection, selection, selectionArgs,
                                        null, null, sortOrder);
        cursor.setNotificationUri(mResolver, uri);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case PROFILE:
                return "profile";

            default:
                //error log
                break;
        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /*only the first time to save the profile info to db*/
        if (mHelper == null) {
            //error exception
        }
        SQLiteDatabase db = mHelper.getWritableDatabase();
        long id = db.insert(DB_TABLE, null, values);
        if (id < 0) {
            //error exception
        }
        Uri newUri = ContentUris.withAppendedId(uri, id);
        mResolver.notifyChange(newUri, null);
        return newUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        //not support delete profile
        SQLiteDatabase db = mHelper.getWritableDatabase();
        int count = 0;
        switch (uriMatcher.match(uri)) {
            case PROFILE:
                count = db.delete(DB_TABLE, selection, selectionArgs);
                break;

            default:
                //error log
                break;
        }
        mResolver.notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
            SQLiteDatabase db = mHelper.getWritableDatabase();
            int count = 0;
            switch (uriMatcher.match(uri)) {
                case PROFILE:
                    //log
                    count = db.update(DB_TABLE, values, selection, selectionArgs);
                    break;

                default:
                    //log error no uri match
                    break;
            }
            mResolver.notifyChange(uri, null);
            return count;
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String DB_NAME = "profile.db";
        private static final  int DB_VERSION = 1;
        private static final String DB_CREATE = "CREATE TABLE IF NOT EXISTS " + DB_TABLE +
                                "(" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                PHONE_NUMBER + " TEXT, " +
                                FIRST_NAME + " TEXT, " +
                                LAST_NAME + " TEXT, " +
                                PORTRAIT + " TEXT, " +
                                PORTRAIT_TYPE + " TEXT, " +
                                ADDRESS + " TEXT, " +
                                PHONE_NUMBER_SECOND + " TEXT, " +
                                EMAIL + " TEXT, " +
                                BIRTHDAY + " TEXT, " +
                                COMPANY + " TEXT, " +
                                COMPANY_TEL + " TEXT, " +
                                TITLE + " TEXT, " +
                                COMPANY_ADDR + " TEXT, " +
                                COMPANY_FAX + " TEXT, " +
                                PCC_ETAG + " TEXT, " +
                                PORTRAIT_ETAG + " TEXT, " +
                                QRCODE + " TEXT, " +
                                QRCODE_ETAG + " TEXT" + ");";

        public DatabaseHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DB_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE);
            this.onCreate(db);
        }
    }
}
