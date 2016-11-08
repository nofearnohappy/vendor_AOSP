/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2014. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */
package com.mediatek.blemanager.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class BleManagerProvider extends ContentProvider {
    private static final String TAG = BleConstants.COMMON_TAG + "[BleManagerProvider]";

    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    private SQLiteOpenHelper mDBOpenHelper;

    private static final String LIST_TYPE = "com.android.bluetooth.BLE.list";
    private static final String ITEM_TYPE = "com.android.bluetooth.BLE.item";

    private static final int TABLE_DEVICE_SETTING = 1;
    private static final int TABLE_DEVICE_SETTING_ID = 2;

    static {
        URI_MATCHER.addURI(BleConstants.BLEMANAGER_AUTHORITY, "device_setting",
                TABLE_DEVICE_SETTING);
        URI_MATCHER.addURI(BleConstants.BLEMANAGER_AUTHORITY, "device_setting/#",
                TABLE_DEVICE_SETTING_ID);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Log.i(TAG, "[delete] uri : " + uri + ", selection : " + selection + ", selectionArgs : "
                + selectionArgs);
        if (!isExisted(uri, selection)) {
            Log.w(TAG, "[delete] not exist in DB, cann't delete!!!");
            return 0;
        }

        SQLiteDatabase db = mDBOpenHelper.getWritableDatabase();
        int match = URI_MATCHER.match(uri);
        String table = null;
        int whichTable = 0;
        Log.i(TAG, "[delete] match = " + match);
        switch (match) {
        case TABLE_DEVICE_SETTING:
        case TABLE_DEVICE_SETTING_ID:
            table = BleConstants.DEVICE_SETTINGS.TABLE_NAME;
            whichTable = TABLE_DEVICE_SETTING;
            break;

        default:
            throw new IllegalArgumentException("[delete] Unknown URI: " + uri);
        }
        Log.i(TAG, "[delete] table : " + table);
        if (table == null) {
            return 0;
        }
        int re = db.delete(table, selection, selectionArgs);
        if (re > 0 && whichTable != 0) {
            this.notifyChange(whichTable);
        }
        return re;
    }

    @Override
    public String getType(Uri uri) {
        switch (URI_MATCHER.match(uri)) {
        case TABLE_DEVICE_SETTING:
            return LIST_TYPE;

        case TABLE_DEVICE_SETTING_ID:
            return ITEM_TYPE;

        default:
            throw new IllegalArgumentException("[getType] Unknown URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.i(TAG, "[insert] uri : " + uri + ", values : " + values);
        if (values == null) {
            Log.w(TAG, "[insert] values is null, pls check!!!");
            return null;
        }
        if (values.size() == 0) {
            Log.w(TAG, "[insert] values size is 0, pls check!!!");
            return null;
        }
        if (!values.containsKey(BleConstants.COLUMN_BT_ADDRESS)) {
            Log.w(TAG, "[insert] MUST CONTAINS ADDRESS, PLS CHECK!!!");
            return null;
        }
        String addrFromValue = values.getAsString(BleConstants.COLUMN_BT_ADDRESS);
        if (addrFromValue == null || addrFromValue.trim().length() == 0) {
            Log.w(TAG, "[insert] WRONG ADDRESS, SHOULD BE VALID BT ADDRESS, PLS CHECK!!!");
            return null;
        }
        String selection = BleConstants.COLUMN_BT_ADDRESS + "='" + addrFromValue + "'";
        if (isExisted(uri, selection)) {
            Log.i(TAG, "[insert] device is already in db,return.");
            return null;
        }
        SQLiteDatabase db = mDBOpenHelper.getWritableDatabase();
        int match = URI_MATCHER.match(uri);
        String table = null;
        int whichTable = 0;
        ContentValues finalValue = new ContentValues(values);
        Log.i(TAG, "[insert] match = " + match);
        switch (match) {
        case TABLE_DEVICE_SETTING:
            table = BleConstants.DEVICE_SETTINGS.TABLE_NAME;
            whichTable = TABLE_DEVICE_SETTING;
            byte[] array = values.getAsByteArray("image_byte_array");
            finalValue.remove("image_byte_array");
            if (array != null && array.length != 0) {
                String fileName = this.buildDeviceImageFile(array);
                if (fileName != null) {
                    Log.d(TAG, "[insert] fileName is : " + fileName);
                    finalValue.put(BleConstants.DEVICE_SETTINGS.DEVICE_IAMGE_DATA, fileName);
                } else {
                    Log.w(TAG, "[insert] fileName is null");
                }
            } else {
                Log.w(TAG, "[insert] array is null or empty");
            }
            break;

        default:
            throw new IllegalArgumentException("[insert] Unknown URI: " + uri);
        }

        long id = db.insert(table, null, finalValue);
        Log.d(TAG, "[insert]ret = " + id);
        if (id > 0 && whichTable != 0) {
            this.notifyChange(whichTable);
        }
        if (id < 0) {
            Log.w(TAG, "[insert] insert failed");
            return null;
        }
        return Uri.parse(uri + "/" + id);
    }

    @Override
    public boolean onCreate() {
        Log.i(TAG, "[onCreate] call create db");
        mDBOpenHelper = BleManagerDatabaseHelper.getInstance(getContext());
        return true;
    }

    /**
     * Which used to query data from db.
     * 
     * @param uri
     * @param projection
     * @param selection
     * @param selectionArgs
     * @param sortOrder
     * @return
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        SQLiteDatabase db = mDBOpenHelper.getReadableDatabase();
        Cursor retCursor = null;
        int match = URI_MATCHER.match(uri);
        Log.i(TAG, "[query] match : " + match);
        switch (match) {
        case TABLE_DEVICE_SETTING_ID:
            builder.setTables(BleConstants.DEVICE_SETTINGS.TABLE_NAME);
            builder.appendWhere(BleConstants.COLUMN_ID + "=" + uri.getPathSegments().get(1));
            retCursor = builder.query(db, projection, selection, selectionArgs, null, null,
                    sortOrder);
            break;

        case TABLE_DEVICE_SETTING:
            Log.d(TAG, "[query] selection : " + selection);
            builder.setTables(BleConstants.DEVICE_SETTINGS.TABLE_NAME);
            String select = builder.buildQuery(projection, selection, null, null, sortOrder, null);
            Log.d(TAG, "[query] select : " + select);
            retCursor = db.rawQuery(select, selectionArgs);
            Log.d(TAG, "[query] retCursor : " + retCursor.getCount());
            break;

        default:
            throw new IllegalArgumentException("[query] Unknown URI: " + uri);
        }

        return retCursor;
    }

    /**
     * @param uri
     * @param values
     *            A map from column names to new column values. null is a valid
     *            value that will be translated to NULL.
     * @param selection
     *            the optional WHERE clause to apply when updating. Passing null
     *            will update all rows.
     * @param selectionArgs
     *            You may include ? s in the where clause, which will be
     *            replaced by the values from whereArgs. The values will be
     *            bound as Strings.
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (!isExisted(uri, selection)) {
            Log.w(TAG, "[update] not exist in DB, cann't update");
            return 0;
        }

        SQLiteDatabase db = mDBOpenHelper.getWritableDatabase();
        int returnValue = 0;
        int whichTable = 0;
        int match = URI_MATCHER.match(uri);

        Log.i(TAG, "[update] uri : " + uri + ", selection :" + selection + ",match = " + match);
        switch (match) {
        case TABLE_DEVICE_SETTING:
            ContentValues finalValue = new ContentValues(values);
            if (finalValue.containsKey("image_byte_array")) {
                deleteImageFile(uri, selection);
                byte[] array = finalValue.getAsByteArray("image_byte_array");
                finalValue.remove("image_byte_array");
                if (array != null && array.length != 0) {
                    String fileName = this.buildDeviceImageFile(array);
                    if (fileName != null) {
                        Log.d(TAG, "[update] fileName is : " + fileName);
                        finalValue.put(BleConstants.DEVICE_SETTINGS.DEVICE_IAMGE_DATA, fileName);
                    } else {
                        Log.w(TAG, "[update] fileName is null");
                    }
                } else {
                    Log.w(TAG, "[update] array is null or empty");
                }
            }
            returnValue = db.update(BleConstants.DEVICE_SETTINGS.TABLE_NAME, finalValue, selection,
                    selectionArgs);
            whichTable = TABLE_DEVICE_SETTING;
            break;

        case TABLE_DEVICE_SETTING_ID:
            ContentValues finalValue1 = new ContentValues(values);
            if (finalValue1.containsKey("image_byte_array")) {
                deleteImageFile(uri, selection);
                byte[] array = finalValue1.getAsByteArray("image_byte_array");
                finalValue1.remove("image_byte_array");
                if (array != null && array.length != 0) {
                    String fileName = this.buildDeviceImageFile(array);
                    if (fileName != null) {
                        Log.d(TAG, "[update] fileName is : " + fileName);
                        finalValue1.put(BleConstants.DEVICE_SETTINGS.DEVICE_IAMGE_DATA, fileName);
                    } else {
                        Log.w(TAG, "[update] fileName is null");
                    }
                } else {
                    Log.w(TAG, "[update] array is null or empty");
                }
            }
            returnValue = db.update(BleConstants.DEVICE_SETTINGS.TABLE_NAME, finalValue1,
                    selection, selectionArgs);
            whichTable = TABLE_DEVICE_SETTING;
            break;

        default:
            throw new IllegalArgumentException("[update] Unknown URI: " + uri);
        }
        Log.d(TAG, "[update] affected rows : " + returnValue);
        if (returnValue > 0 && whichTable != 0) {
            Log.d(TAG, "[update] whichTable : " + whichTable);
            notifyChange(whichTable);
        }
        return returnValue;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (uri == null) {
            Log.d(TAG, "[openFile] uri is null");
            return null;
        }
        int match = URI_MATCHER.match(uri);
        if (match != TABLE_DEVICE_SETTING && match != TABLE_DEVICE_SETTING_ID) {
            Log.w(TAG, "[openFile] uri is not match ux table,match = " + match);
            return null;
        }

        return openFileHelper(uri, mode);
    }

    private void deleteImageFile(Uri uri, String selection) {
        if (uri == null) {
            Log.w(TAG, "[deleteImageFile] uri is null");
            return;
        }
        int match = URI_MATCHER.match(uri);
        Cursor cursor = null;
        Log.d(TAG, "[deleteImageFile] match = " + match);
        if (match == TABLE_DEVICE_SETTING) {
            if (selection == null || selection.trim().length() == 0) {
                Log.w(TAG, "[deleteImageFile] selection must not be null or empty,"
                        + " while match is TABLE_UX_ID");
                return;
            }
            cursor = this.query(uri,
                    new String[] { BleConstants.DEVICE_SETTINGS.DEVICE_IAMGE_DATA }, selection,
                    null, null);

        } else if (match == TABLE_DEVICE_SETTING_ID) {
            cursor = this.query(uri,
                    new String[] { BleConstants.DEVICE_SETTINGS.DEVICE_IAMGE_DATA }, selection,
                    null, null);
        } else {
            Log.w(TAG, "[deleteImageFile] wrong match with UX table");
        }

        if (cursor == null) {
            Log.w(TAG, "[deleteImageFile] cursor is null");
            return;
        }
        if (cursor.getCount() == 0) {
            Log.w(TAG, "[deleteImageFile] cursor count is 0");
            cursor.close();
            return;
        }
        cursor.moveToFirst();
        String path = cursor.getString(0);
        cursor.close();
        if (path == null || path.trim().length() == 0) {
            Log.w(TAG, "[deleteImageFile] path is empty");
            return;
        }
        File file = new File(path);
        if (file.isFile() && file.exists()) {
            file.delete();
            Log.d(TAG, "[deleteImageFile] delete the image file");
        }
    }

    private String buildDeviceImageFile(byte[] bytes) {
        if (bytes == null) {
            Log.w(TAG, "[buildDeviceImageFile] bytes is null");
            return null;
        }
        if (bytes.length == 0) {
            Log.w(TAG, "[buildDeviceImageFile] bytes length is 0");
            return null;
        }
        Bitmap map = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        String path = buildFilePath();
        File file = new File(path);
        try {
            if (!file.exists()) {
                Log.i(TAG, "[buildDeviceImageFile] file is not exist, create a new one");
                file.createNewFile();
            }
            FileOutputStream fo = new FileOutputStream(file);
            map.compress(Bitmap.CompressFormat.PNG, 100, fo);
            fo.flush();
            fo.close();
        } catch (IOException ex) {
            Log.e(TAG, "[buildDeviceImageFile]IOException.");
            ex.printStackTrace();
            return null;
        }
        if (!file.exists()) {
            Log.w(TAG, "[buildDeviceImageFile] file is not exist");
            return null;
        }
        Log.d(TAG, "[buildDeviceImageFile] file getAbsolutePath : " + file.getAbsolutePath());
        return file.getAbsolutePath();
    }

    private String buildFilePath() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.getContext().getFilesDir());
        builder.append("/");
        String deviceImageName = "device_" + System.currentTimeMillis() + ".png";
        builder.append(deviceImageName);
        Log.d(TAG, "[buildFilePath] file path : " + builder.toString());
        return builder.toString();
    }

    private void notifyChange(int whichTable) {
        Log.d(TAG, "[notifyChange] whichTable : " + whichTable);
        Uri uri = null;
        switch (whichTable) {
        case TABLE_DEVICE_SETTING:
            uri = BleConstants.TABLE_UX_URI;
            break;

        default:
            throw new IllegalArgumentException("[notifyChange] unknown id!");
        }
        Log.d(TAG, "[notifyChange] notify change uri : " + uri);

        if (uri == null) {
            Log.w(TAG, "[notifyChange] uri is null !!");
            return;
        }
        this.getContext().getContentResolver().notifyChange(uri, null);
    }

    private boolean isExisted(Uri uri, String selection) {
        if (uri == null) {
            Log.w(TAG, "[isExisted] uri is null,return false.");
            return false;
        }

        SQLiteDatabase db = mDBOpenHelper.getReadableDatabase();
        int match = URI_MATCHER.match(uri);
        Cursor cur = null;
        Log.d(TAG, "[isExisted] uri : " + uri + ", selection : " + selection + ",match = " + match);
        try {
            switch (match) {
            case TABLE_DEVICE_SETTING:
            case TABLE_DEVICE_SETTING_ID:
                cur = db.query(BleConstants.DEVICE_SETTINGS.TABLE_NAME, null, selection, null,
                        null, null, null);
                break;

            default:
                throw new IllegalArgumentException("[isExisted] Unknown URI: " + uri);
            }
        } catch (IllegalArgumentException e) {
            Log.d(TAG, e.toString());
            return false;
        } finally {
            if (cur == null) {
                Log.w(TAG, "[isExisted] cursor is null!!!");
                return false;
            }
            if (cur.getCount() == 0) {
                cur.close();
                Log.w(TAG, "[isExisted] cursor count is 0!!!");
                return false;
            }
            cur.close();
        }
        return true;
    }
}
