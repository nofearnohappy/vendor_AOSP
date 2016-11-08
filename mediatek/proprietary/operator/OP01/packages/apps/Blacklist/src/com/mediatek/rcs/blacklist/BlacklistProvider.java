/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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

package com.mediatek.rcs.blacklist;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import com.mediatek.rcs.blacklist.BlacklistData.BlacklistTable;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * BlacklistProvider.
 */
public class BlacklistProvider extends ContentProvider {

    private static final String TAG = "Blacklist";

    private static final String DATABASE_NAME = "blacklist.db";

    private static final int DB_VERSION = 1;

    private static final int URL_CONTACTS = 1;
    private static final int URL_CONTACT_NUMBER = 2;

    private static final UriMatcher sMATCHER;
    private static final HashMap<String, String> sBlacklistProjection;
    private DatabseHelper mDbHelper = null;

    static {
        sMATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        sMATCHER.addURI(BlacklistData.AUTHORITY, null, URL_CONTACTS);
        sMATCHER.addURI(BlacklistData.AUTHORITY, "/#", URL_CONTACT_NUMBER);
        sMATCHER.addURI(BlacklistData.AUTHORITY, "/*", URL_CONTACT_NUMBER);

        sBlacklistProjection = new HashMap<String, String>();
        sBlacklistProjection.put(BaseColumns._ID, BaseColumns._ID);
        sBlacklistProjection.put(BlacklistTable.PHONE_NUMBER, BlacklistTable.PHONE_NUMBER);
        sBlacklistProjection.put(BlacklistTable.DISPLAY_NAME, BlacklistTable.DISPLAY_NAME);
    }

    /**
     * DatabseHelper.
     */
    class DatabseHelper extends SQLiteOpenHelper {

        public DatabseHelper(Context context) {
            super(context, DATABASE_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + BlacklistTable.TABLE_NAME + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + BlacklistTable.PHONE_NUMBER + " VARCHAR(40), "
                + BlacklistTable.DISPLAY_NAME + " VARCHAR(40)"
                + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + BlacklistTable.TABLE_NAME + ";");
            onCreate(db);
            return;
        }
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new DatabseHelper(getContext());
        return true;
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        String finalWhere = where;
        String number = null;
        ArrayList<String> numberList = new ArrayList<String>();
        int ret = -1;
        Boolean useOrignalSql = true;

        log("[delete]++ uri:" + uri.toString());
        if (where == null && uri.getPathSegments().size() > 0) {
            number = Uri.decode(uri.getLastPathSegment());
            log("[delete] delete by number:" + number);

            //number = BlacklistUtils.buildQueryNubmer(number);
            //finalWhere = BlacklistTable.PHONE_NUMBER + " = " + number;
            numberList.add(number);
            useOrignalSql = false;
        } else if (where != null && !where.isEmpty()) {
            log("[delete] delete by where:" + where + ", " + whereArgs);
            if (where.contains(BlacklistTable.PHONE_NUMBER)) {
                //Case, delete by number
                useOrignalSql = false;
                if (where.contains("=")) {
                    //one number case
                    StringBuilder num = new StringBuilder();
                    if (whereArgs == null) {
                        int i = where.indexOf("=");
                        for (; i < where.length(); i++) {
                            char c = where.charAt(i);
                            if (c == '+' || (c >= '0' && c <= '9')) {
                                num.append(c);
                            }
                        }
                        number = num.toString();
                    } else {
                        int size = whereArgs.length;
                        log("[delete] one number case, whereArgs.size " + size);
                        if (size > 0) {
                            number = whereArgs[0];
                        }
                    }
                    log("[delete] one number case, number:" + number);
                    numberList.add(number);
                } else if (where.contains("in") || where.contains("IN")) {
                    //many numbers case
                    String numbers = null;
                    int i = 0;
                    StringBuilder num = new StringBuilder();

                    if (whereArgs == null) {
                        i = where.indexOf("(");
                        numbers = where.substring(i + 1, where.length() - 1);
                    } else {
                        int size = whereArgs.length;
                        log("[delete] many numbers case, whereArgs.size " + size);
                        if (size > 0) {
                            numbers = whereArgs[0];
                        }
                    }

                    log("[delete] many numbers case, numbers:" + numbers);
                    if (numbers != null) {
                        String[] sp = numbers.split(",");
                        numberList.addAll(java.util.Arrays.asList(sp));
                        log("[delete] many numbers case, numbers is:" + numberList.toString());
                    } else {
                        useOrignalSql = true;
                    }
                }
            } else {
                /* delete by _id
                 * parameter is illegal
                 */
                log("[delete] maybe delete by _id or parameter is illegal!, just try it");
            }
        }

        /* query data from DB and compare number with PhoneNumberUtils
         * handle internal number (+) case
         * eg. ALPS02113937
         */
        if (useOrignalSql) {
            log("[delete] delete where: " + finalWhere);
            ret = db.delete(BlacklistTable.TABLE_NAME, finalWhere, whereArgs);
        } else {
            StringBuilder idBuilder = new StringBuilder();
            Cursor blackCursor = query(BlacklistTable.CONTENT_URI, new String[] {BaseColumns._ID,
                                        BlacklistTable.PHONE_NUMBER}, null, null, null);

            try {
                for (String delNumber : numberList) {
                    if (!delNumber.isEmpty()) {
                        while (blackCursor.moveToNext()) {
                            String rawId = blackCursor.getString(0);
                            String rawNumber = blackCursor.getString(1);
                            if (PhoneNumberUtils.compare(delNumber, rawNumber, false)) {
                                idBuilder.append(rawId);
                                idBuilder.append(",");
                                log("[delete] will delete number:" + rawNumber);
                            }
                        }
                        blackCursor.moveToFirst();
                    }
                }
                if (idBuilder.length() > 1) {
                    idBuilder.deleteCharAt(idBuilder.length() - 1);
                }
            } finally {
                blackCursor.close();
            }

            log("[delete] delete id(s):" + idBuilder.toString());
            ret = db.delete(
                BlacklistTable.TABLE_NAME, "_id in (" + idBuilder.toString() + ")", null);
        }

        log("[delete]-- ret = " + ret);
        return ret;
    }

    @Override
    public String getType(Uri uri) {
        String type = null;
        final int match = sMATCHER.match(uri);

        switch(match) {
            case URL_CONTACTS:
                type = BlacklistTable.CONTENT_TYPE;
                break;

            case URL_CONTACT_NUMBER:
                type = BlacklistTable.CONTENT_ITEM_TYPE;
                break;

            default:
        }

        return type;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        String savedNumber = "";

        if (!values.containsKey(BlacklistTable.PHONE_NUMBER)) {
                throw new IllegalArgumentException("values inserted doesn't contain Phone_number");
        } else {
            savedNumber = values.getAsString(BlacklistTable.PHONE_NUMBER);
        }

        log("insert number is " + savedNumber);
        if (savedNumber == null || savedNumber.isEmpty()) {
            throw new IllegalArgumentException("Phone_number is null or empty");
        }

        // chec number if already exist in blacklist
        log("insert number, check blacklist db +");

        Cursor blackCursor = query(BlacklistTable.CONTENT_URI, new String[] {BaseColumns._ID,
                                    BlacklistTable.PHONE_NUMBER}, null, null, null);
        try {
            if (blackCursor != null) {
                int count = blackCursor.getCount();
                if (count >= BlacklistTable.RECORDS_NUMBER_MAX) {
                    log("records number in db reached to: " + count + " cannot be added.");
                    return null;
                }

                while (blackCursor.moveToNext()) {
                    String existId = blackCursor.getString(0);
                    String existNumber = blackCursor.getString(1);
                    if (PhoneNumberUtils.compare(savedNumber, existNumber, false)) {
                        log("It already exists in db, id: " + existId +
                            " , number is: " + existNumber);
                        return null;
                    }
                }
            }
        } finally {
            blackCursor.close();
            log("insert number, check blacklist db -");
        }

        // check name if exists in contacts
        log("insert number, query contacts db +");
        if (!values.containsKey(BlacklistTable.DISPLAY_NAME)) {
            log("query display name from contacts ");
            Cursor contactsCursor = getContext().getContentResolver().query(
                Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(savedNumber)),
                new String[] {BaseColumns._ID, PhoneLookup.NUMBER, PhoneLookup.DISPLAY_NAME},
                null, null,
                BaseColumns._ID + " DESC");

            try {
                if (contactsCursor == null || contactsCursor.getCount() == 0) {
                    log("Contacts does not contain the number: " + savedNumber);
                } else {
                    while (contactsCursor.moveToNext()) {
                        String contactNumber = contactsCursor.getString(1);
                        String contactName = contactsCursor.getString(2);
                        if (PhoneNumberUtils.compare(savedNumber, contactNumber)) {
                            log("contacts number matched: " + contactNumber);
                            values.put(BlacklistTable.DISPLAY_NAME, contactName);
                        }
                    }
                }
            } finally {
                contactsCursor.close();
                log("insert number, query contacts db -");
            }
        }

        long rowId = db.insert(BlacklistTable.TABLE_NAME, null, values);
        log("insert db row id: " + rowId);

        if (rowId > 0) {
             Uri resultUri = ContentUris.withAppendedId(BlacklistData.AUTHORITY_URI, rowId);
             return resultUri;
        }

        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                            String sortOrder) {

        final SQLiteDatabase db = mDbHelper.getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(BlacklistTable.TABLE_NAME);

        if (projection == null) {
            qb.setProjectionMap(sBlacklistProjection);
        }

        switch (sMATCHER.match(uri)) {
            case URL_CONTACTS:
                break;

            case URL_CONTACT_NUMBER:
                if (uri.getPathSegments().size() > 1) {
                    String number = Uri.decode(uri.getLastPathSegment());
                    log("query by number: " + number);

                    number = BlacklistUtils.buildQueryNubmer(number);
                    qb.appendWhere(BlacklistTable.PHONE_NUMBER + "=" + number);
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null,
                            BlacklistTable.DEFAULT_SORT_ORDER);

        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        String finalWhere = selection;
        log("[update] uri: " + uri.toString());
        if (selection == null && uri.getPathSegments().size() > 0) {
            finalWhere = BaseColumns._ID + " = " + Uri.decode(uri.getLastPathSegment());
        }

        int count = db.update(BlacklistTable.TABLE_NAME, values, finalWhere, selectionArgs);

        return count;
    }

    private void log(String message) {
        Log.d(TAG, "[" + getClass().getSimpleName() + "] " + message);
    }
}
