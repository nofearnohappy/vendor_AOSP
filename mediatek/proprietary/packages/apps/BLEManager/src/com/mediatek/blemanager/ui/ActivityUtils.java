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
package com.mediatek.blemanager.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.mediatek.blemanager.R;
import com.mediatek.blemanager.provider.BleConstants;
import com.mediatek.storage.StorageManagerEx;

import java.io.ByteArrayOutputStream;
import java.io.File;

public class ActivityUtils {
    private static final String TAG = BleConstants.COMMON_TAG + "[ActivityUtils]";

    private static final String ROOT_DIR = StorageManagerEx.getDefaultPath();
    private static final String FILE_DIR = ROOT_DIR + "/ble";

    /**
     * 
     * @param activity
     * @param which
     */
    public static void startImageChooser(Activity activity, int which) {
        if (activity == null) {
            Log.w(TAG, "[startImageChooser] activity is null,return!");
            return;
        }
        Intent intent = null;
        Log.i(TAG, "[startImageChooser] which : " + which);
        switch (which) {
        case 0:
            intent = new Intent(activity, DeviceDefaultImageChooser.class);
            break;

        case 1:
            intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Uri uri = getTempFileUri();
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            break;

        case 2:
            intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            break;

        default:
            throw new IllegalArgumentException("not recognized id!!");
        }
        if (intent != null) {
            activity.startActivityForResult(intent, which);
        }
    }

    /**
     * 
     * @param activity
     * @param which
     * @param uri
     * @param photoX
     * @param photoY
     */
    public static void handlePhotoCrop(Activity activity, int which, Uri uri) {
        Log.i(TAG, "[handlePhotoCrop] which : " + which + ",uri = " + uri);
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", true);
        intent.putExtra("scale", true);
        intent.putExtra("scaleUpIfNeeded", true);
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 510);
        intent.putExtra("outputY", 565);
        activity.startActivityForResult(intent, which);
    }

    public static Uri getDrawableUri(int drawableId) {
        String uriString = "android.resource://com.mediatek.blelinker/" + drawableId;
        return Uri.parse(uriString);
    }

    public static Bitmap getDrawbleBitmap(Context context, int drawableId) {
        if (context == null) {
            Log.d(TAG, "[getDrawbleBitmap] context is null");
            return null;
        }
        Resources r = context.getResources();
        Bitmap map = BitmapFactory.decodeResource(r, drawableId);

        return map;
    }

    /**
     * get default image from resource.
     * 
     * @param context
     * @return
     */
    public static byte[] getDefaultImage(Context context) {
        if (context == null) {
            Log.w(TAG, "[getDefaultImage] context is null");
            return null;
        }
        Resources r = context.getResources();
        Bitmap map = BitmapFactory.decodeResource(r, R.drawable.image_device);
        if (map == null) {
            Log.w(TAG, "[getDefaultImage] map is null");
            return null;
        }

        return comproseBitmapToByteArray(map);
    }

    /**
     * 
     * @param bitmap
     * @return
     */
    public static byte[] comproseBitmapToByteArray(Bitmap bitmap) {
        Log.i(TAG, "[comproseBitmapToByteArray]...");
        if (bitmap == null) {
            Log.w(TAG, "[comproseBitmapToByteArray] bitmap is null");
            return null;
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);

        return bos.toByteArray();
    }

    /**
     * 
     * @param bytes
     * @return
     */
    public static Bitmap decodeByteArrayToBitmap(byte[] bytes) {
        Log.i(TAG, "[decodeByteArrayToBitmap]...");
        if (bytes == null) {
            Log.w(TAG, "[decodeByteArrayToBitmap] bytes is null");
            return null;
        }
        if (bytes.length == 0) {
            Log.w(TAG, "[decodeByteArrayToBitmap] bytes length is 0");
            return null;
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    /**
     * Save the image which choose from gallery. Then return the saved uri,
     * which indicate to the new folder. This method try to avoid the image
     * which you choose from gallery and later delete from gallery
     * 
     * @param context
     * @param tagId
     * @param uri
     * @return
     */
    public static Bitmap saveImageFromCustom(Context context, Uri uri, String fileName) {
        if (context == null) {
            Log.w(TAG, "[saveImageFromCustom] context is null!!");
            return null;
        }
        if (uri == null) {
            Log.w(TAG, "[saveImageFromCustom] uri is null!!");
            return null;
        }

        String srcPath = getUriPath(context, uri);
        Log.d(TAG, "[saveImageFromCustom] srcPath : " + srcPath + ",uri = " + uri);
        if (srcPath == null || srcPath.trim().length() == 0) {
            Log.w(TAG, "[saveImageFromCustom] srcPath is null or EMPTY,return!");
            return null;
        }

        File srcFile = new File(srcPath);
        Bitmap bitmap = getCornorBitmap(srcFile);

        return bitmap;
    }

    public static Uri getTempFileUri() {
        String fileName = "device_temp.png";
        File f = new File(FILE_DIR);
        if (!f.exists()) {
            f.mkdir();
        }
        File file = new File(FILE_DIR + "/" + fileName);
        return Uri.fromFile(file);
    }

    private static String getUriPath(Context context, Uri uri) {
        if (context == null) {
            Log.w(TAG, "[getUriPath] context is null!!");
            return "";
        }
        if (uri == null) {
            Log.w(TAG, "[getUriPath] uri is null!!");
            return "";
        }

        String fileName = null;
        if (uri.getScheme().toString().compareTo("content") == 0) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                Log.d(TAG, "[getUriPath] column_index : " + columnIndex);
                fileName = cursor.getString(columnIndex);
                Log.d(TAG, "[getUriPath] fileName : " + fileName);
                cursor.close();
            } else if (cursor != null && cursor.getCount() == 0) {
                cursor.close();
            }
        } else if (uri.getScheme().compareTo("file") == 0) {
            fileName = uri.toString();
            fileName = uri.toString().replace("file://", "");
            if (!fileName.startsWith("/mnt")) {
                fileName += "/mnt";
            }
        }
        Log.d(TAG, "[getUriPath] fileName : " + fileName);
        return fileName;
    }

    /**
     * 
     * @param context
     * @param uri
     * @return
     */
    private static Bitmap getCornorBitmap(File file) {
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        Drawable imageDrawable = new BitmapDrawable(bitmap);

        Bitmap outputBitmap = Bitmap.createBitmap(510, 565, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(outputBitmap);
        RectF outerRect = new RectF(0, 0, 510, 565);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.RED);
        canvas.drawRoundRect(outerRect, 10, 10, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        imageDrawable.setBounds(0, 0, 510, 565);
        canvas.saveLayer(outerRect, paint, Canvas.ALL_SAVE_FLAG);
        imageDrawable.draw(canvas);
        canvas.restore();
        
        return outputBitmap;
    }
}
