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

package com.mediatek.rcs.incallui.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;

import com.mediatek.gifdecoder.GifDecoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class AsyncPhotoLoader {
    private static final String TAG = "AsyncPhotoLoader";

    private static final int MESSAGE_TYPE_PIC = 0;
    private static final int MESSAGE_TYPE_GIF = 1;
    private static final int MESSAGE_TYPE_VID = 2;
    private static final int MESSAGE_TYPE_NONE = 3;
    private static final WorkHandler sWorkHandler;

    //Result callback handler for main thread.
    private static final Handler mResultHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //Log.d(TAG, "handleMessage, type = " + msg.what);
            switch(msg.what) {
                case MESSAGE_TYPE_PIC:
                    WorkArgs args = (WorkArgs) msg.obj;
                    args.mListener.onImageLoadComplete(args.mPhotoUri,
                            args.mBitmapList, args.mIntegerList,
                            MESSAGE_TYPE_PIC, args.mGreeting);
                    break;
                case MESSAGE_TYPE_GIF:
                    args = (WorkArgs) msg.obj;
                    GifDecoder decoder = args.mGifDecoder;
                    if (decoder != null) {
                        //Do not release decoder, will get NE. But we need to check if we will
                        // get memory problem.
                        //Log.d(TAG, "close decoder~");
                        //decoder.close();
                    }
                    args.mListener.onImageLoadComplete(args.mPhotoUri,
                            args.mBitmapList, args.mIntegerList,
                            MESSAGE_TYPE_GIF, args.mGreeting);
                    break;
                default:
                    break;
            }
        }
    };

    static {
        HandlerThread thread = new HandlerThread("AsyncPhotoLoader");
        thread.start();
        sWorkHandler = new WorkHandler(thread.getLooper());
    }

    public void startObtainPhoto(Context cnx,
            int height, int width, String uri, int type, String greeting, Listener listener) {

        Log.d(TAG, "startObtainPhoto, type = " + type +
                ", height = " + height +  ", width = " + width);
        Message message = sWorkHandler.obtainMessage(type);
        WorkArgs args = new WorkArgs();
        args.mContext = cnx;
        args.mPhotoUri = uri;
        args.mType = type;
        args.mListener = listener;
        args.mHeight = height;
        args.mWidth  = width;
        args.mGreeting = greeting;
        message.obj = args;
        sWorkHandler.sendMessage(message);
    }

    public interface Listener {
        public void onImageLoadComplete(String uri,
                ArrayList<Bitmap> bitmaps, ArrayList<Integer> list, int type, String greeting);
    }

    private static class WorkHandler extends Handler {
        WorkHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case MESSAGE_TYPE_PIC:
                    this.loadingPhoto((WorkArgs) msg.obj);
                    break;
                case MESSAGE_TYPE_GIF:
                    this.loadingGIF((WorkArgs) msg.obj);
                    break;
                default:
                    Log.d(TAG, "handleMessage, unknown type!");
                    break;
            }
        }

        private void loadingGIF(WorkArgs args) {
            Log.d(TAG, "loadingGIF");
            InputStream inputStream = null;
            Context context = args.mContext;
            String uri = args.mPhotoUri;
            File file = new File(uri);
            if (file.exists()) {
                //Using gif decoder to get gif info.
                GifDecoder decoder = new GifDecoder(uri);
                int count = decoder.getTotalFrameCount();
                for (int i = 0; i < count; i++) {
                    Bitmap richImage = decoder.getFrameBitmap(i);

                    int width = args.mWidth;
                    int height = args.mHeight;

                    int origWidth = richImage.getWidth();
                    int origHeight = richImage.getHeight();

                    float scaleX = ((float) origWidth) / width;
                    float scaleY = ((float) origHeight) / height;

                    int newWidth = (int) (origWidth / scaleX);
                    int newHeight = (int) (origHeight / scaleY);
                    Bitmap bitmap = Bitmap.createScaledBitmap(richImage,
                                                                newWidth, newHeight, true);
                    if (bitmap != richImage) {
                        richImage.recycle();
                    }

                    args.mBitmapList.add(bitmap);
                    args.mIntegerList.add(decoder.getFrameDuration(i));
                    //Log.d(TAG, "loadingPhoto, origWidth = " + origWidth +
                                                            //", origHeight = " + origHeight);
                }
                args.mGifDecoder = decoder;
            } else {
                Log.d(TAG, "loadingGIF, null bitmap!");
            }

            Message message = mResultHandler.obtainMessage(MESSAGE_TYPE_GIF);
            message.obj = args;
            mResultHandler.sendMessage(message);
        }

        private void loadingPhoto(WorkArgs args) {
            Log.d(TAG, "loadingPhoto");
            InputStream inputStream = null;
            Context context = args.mContext;
            String uri = args.mPhotoUri;
            try {
                File file = new File(uri);
                if (file.exists()) {
                    inputStream = new FileInputStream(file);
                }

                if (inputStream != null) {
                    //Create bitmap using gave file path.
                    Bitmap contactImage = BitmapFactory.decodeStream(inputStream);
                    if (contactImage != null) {
                        int width = args.mWidth;
                        int height = args.mHeight;

                        int origWidth = contactImage.getWidth();
                        int origHeight = contactImage.getHeight();

                        float scaleX = ((float) origWidth) / width;
                        float scaleY = ((float) origHeight) / height;

                        int newWidth = (int) (origWidth / scaleX);
                        int newHeight = (int) (origHeight / scaleY);
                        Bitmap bitmap = Bitmap.createScaledBitmap(contactImage,
                                                                newWidth, newHeight, true);
                        if (bitmap != contactImage) {
                            contactImage.recycle();
                        }
                        args.mBitmapList.add(bitmap);
                    } else {
                        Log.d(TAG, "loadingPhoto, contactImage is null!");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error opening photo input stream");
                //Maybe error handling
                //To do.
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Unable to close input stream.");
                    }
                }
                Message message = mResultHandler.obtainMessage(MESSAGE_TYPE_PIC);
                message.obj = args;
                mResultHandler.sendMessage(message);
            }
        }
    }

    private static final class WorkArgs {
        public String mPhotoUri;
        public int mType;
        public ArrayList<Bitmap> mBitmapList = new ArrayList<Bitmap>();
        //The time between the bitmap frame
        public ArrayList<Integer> mIntegerList = new ArrayList<Integer>();
        public String  mGreeting;
        public Context mContext;
        public View mView;
        public int mHeight;
        public int mWidth;
        public Listener mListener;
        //We move decode here, because when we release decoder resource, NE happended,
        //So we may send this to main thread handler, and release in main thread, but it still
        //got NE. So current no use.
        public GifDecoder mGifDecoder;
    }
}