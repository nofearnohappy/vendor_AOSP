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

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Base64;
import android.util.Log;

import com.mediatek.gifdecoder.GifDecoder;
import com.mediatek.rcs.contacts.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * ProfilePhotoUtils: Profile photo utils.
 */
public class ProfilePhotoUtils {

    //PROFILE_AUTHORITY is defined to this value for manifest.xml
    private static String PROFILE_AUTHORITY = "com.mediatek.profileapp";

    public static final String TAG = "ProfilePhotoUtils";

    public static final int LIMIT_PHOTO_MAX_SIZE = 300*1024;

    /**
     * Create choose photo intent from gallery.
     * @param context
     * @param outputUri: output photo uri.
     * @return intent: Get content intent
     */
    public static Intent getChoosePhotoIntent(Context context, Uri outputUri) {
        final Intent i = new Intent(Intent.ACTION_GET_CONTENT, null);
        i.setType("image/*");
        addPhotoPickExtras(i, outputUri);
        return i;
    }

    /**
     * Create take photo intent via camera.
     * @param context 
     * @param outputUri: output photo uri.
     * @return intent: take photo intent
     */
    public static Intent getTakePhotoIntent(Context context, Uri outputUri) {
        final Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE, null);
        addPhotoPickExtras(i, outputUri);
        return i;
    }

    /**
     * Create crop photo intent via camera.
     * @param context 
     * @param inputUri: input photo uri.
     * @return intent: crop photo intent
     */
    public static Intent getPhotoCropIntent(Context context, Uri inputUri) {
        Uri outputUri = generateTempPhotoUri(context, "crop");
        final Intent i = new Intent("com.android.camera.action.CROP");
        i.setDataAndType(inputUri, "image/*");
        addPhotoPickExtras(i, outputUri);
        addCropExtras(i, 300);
        return i;
    }

    /**
     * Add photo pick extras to intent.
     * @param i: intent to be added.
     * @param outUri: output photo uri.
     */
    private static void addPhotoPickExtras(Intent i, Uri outUri) {
        i.putExtra(MediaStore.EXTRA_OUTPUT, outUri);
        i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        i.setClipData(ClipData.newRawUri(MediaStore.EXTRA_OUTPUT, outUri));
    }

    /**
     * Add crop extras to intent.
     * @param i: intent to be added.
     * @param photoSize. size of crop dimension.
     * @return void.
     */
    private static void addCropExtras(Intent i, int photoSize) {
        i.putExtra("crop", "true");
        i.putExtra("scale", true);
        i.putExtra("scaleUpIfNeeded", true);
        i.putExtra("aspectX", 1);
        i.putExtra("aspectY", 1);
        i.putExtra("outputX", photoSize);
        i.putExtra("outputY", photoSize);
    }

    /**
     * Generate a temp image in data/data/....
     * @param context
     * @param prefix :name prefix
     * @return Uri. temp photo uri.
     */
    public static Uri generateTempPhotoUri(Context context, String prefix) {
        Log.d(TAG, "generateTempPhotoUri");
        final File dir = context.getCacheDir();
        dir.mkdir();
        File tmpFile = new File(dir, prefix + "_temp_profile_photo");
        Uri outputUri;
        String absPath = tmpFile.getAbsolutePath();
        Log.d(TAG, "absPath: " + absPath);
        outputUri = FileProvider.getUriForFile(context,
                PROFILE_AUTHORITY, new File(absPath));
        return outputUri;
    }

    /**
     * Compress image to defined quality png String via Base64.
     * @param uri    :to compressed image.
     * @param context :context
     * @return String : compressed photo string.
     */
    public static String doCompressPhoto(Uri uri, Context context) {
        try {
            InputStream imageStream = context.getContentResolver().openInputStream(uri);
            Bitmap bm = BitmapFactory.decodeStream(imageStream);
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

            int quality = 100;
            if (bm.getByteCount() > LIMIT_PHOTO_MAX_SIZE) {
                quality *= LIMIT_PHOTO_MAX_SIZE / bm.getByteCount();
            }

            bm.compress(Bitmap.CompressFormat.PNG, quality, byteStream);
            Log.d(TAG, "doCompressPhoto quality:" + quality);
            imageStream.close();
            return Base64.encodeToString(byteStream.toByteArray(), Base64.DEFAULT);
        } catch (FileNotFoundException e) {
            Log.d(TAG, "file not find :" + uri.getPath());
            return null;
        } catch (IOException e) {
            Log.d(TAG, "fail to close uri:" + uri);
            return null;
        }
    }


    public static String getPhotoFilePath(Uri uri, Context context) {
        Log.d(TAG, "getPhotoFilePath, uri: " + uri);
        String cacheDir = null;
        String fileName = null;
        String fileAbsPath = null;
        String uriString = uri.toString();
        int startIdx = uriString.indexOf("crop_");
        if (startIdx <= 0) {
            Log.d(TAG, "error, uri have not file name crop_xxx");
            return null;
        }
        final File dir = context.getCacheDir();
        cacheDir = dir.getPath();
        Log.d(TAG, "cacheDir: " + cacheDir);
        if (cacheDir == null) {
            Log.d(TAG, "error, get no cacheDir");
            return null;
        }
        fileName = uriString.substring(startIdx);
        fileAbsPath = cacheDir + "/" + fileName;
        Log.d(TAG, "fileAbsPath: " + fileAbsPath);
        return fileAbsPath;
    }


    /**
     * Copy image from uri to uri.
     * @param context :context
     * @param fromUri  :source image.
     * @param toUri     :destiny image.
     * @return boolean :copy result. 
     */
    public static boolean savePhotoFromUriToUri(Context context, Uri fromUri, Uri toUri) {
        FileOutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            outputStream = context.getContentResolver()
                    .openAssetFileDescriptor(toUri, "rw").createOutputStream();
            inputStream = context.getContentResolver().openInputStream(fromUri);
            final byte[]  buffer = new byte[8 * 1024];
            int length;
            int totalLength = 0;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
                totalLength += length;
            }
            Log.d(TAG, "totalLength=" + totalLength);
            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            Log.d(TAG, "fail to write uri:" + fromUri.toString() + "because : " + e);
            return false;
        } finally {
            //context.getContentResolver().delete(fromUri, null, null);
            return true;
        }
    }

    /**
     * Judge if a string stream is a GIF format.
     * @param: data
     * @return boolean. GIF true; else false
     */
    public static boolean isGifFormatStream(byte[] data) {
        String header = new String(data, 0, 3);
        return header.equals("GIF");
    }

    /**
     * Judge if a string stream is a GIF format.
     * @param gif : gif data stream.   
     * @param index : gif index.
     * @return Bitmap: GIF bitmap
     */
    public static Bitmap getGifFrameBitmap(byte[] gif, int index) {

       GifDecoder gifDecoder = new GifDecoder(gif, 0, gif.length);
       return gifDecoder.getFrameBitmap(index);
       
    }
}
