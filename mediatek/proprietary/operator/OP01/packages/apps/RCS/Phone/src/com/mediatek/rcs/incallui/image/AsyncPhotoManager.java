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
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.mediatek.rcs.incallui.RichCallAdapter.RichCallInfo;
import com.mediatek.rcs.incallui.ext.RCSInCallUIPlugin;
import com.mediatek.rcs.phone.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class AsyncPhotoManager implements AsyncPhotoLoader.Listener {
    private static final String TAG = "AsyncPhotoManager";

    private static int RES_TYPE_PIC = 0;
    private static int RES_TYPE_GIF = 1;
    private static int RES_TYPE_VID = 2;
    private static int RES_TYPE_NONE = 3;

    private ImageView   mImageView;
    private RichGIFView mRichGIFView;
    private TextView    mGreetingView;
    private Context     mContext;
    private int         mViewHeight;
    private int         mViewWidth;

    //To get the view size. Strange, some time we got JE because the image view size is 0.
    // We need to get the size info using the layout saved in RCSInCallUIPlugin.
    private RCSInCallUIPlugin         mRCSInCallUIPlugin;
    //Photoloader to analysis PIC && GIF
    private AsyncPhotoLoader          mAsyncPhotoLoader;
    //HashMap to save the bitmap pictures, which decoded in Loader
    private HashMap<String, ViewItem> mViewCache = new HashMap<String, ViewItem>();

    AsyncPhotoManager(ImageView image,
            RichGIFView gifView, TextView text, Context cnx, RCSInCallUIPlugin plugin) {
        mContext = cnx;
        mImageView = image;
        mRichGIFView = gifView;
        mGreetingView = text;
        mAsyncPhotoLoader = new AsyncPhotoLoader();
        mRCSInCallUIPlugin = plugin;
    }

    public void loadRichPhoto(RichCallInfo info) {
        initViewScale();
        ViewItem item = mViewCache.get(info.mUri);
        if (item == null) {
            //Load photo according to give info which include resource file path.
            //We need gave the greeting info to async loader, and it will give it back to us.
            //We should not use the greeting here at once, or else the pic and greeting info will
            // not show at the same time.
            //And now the mGreet info is null, this maybe for future used.
            mAsyncPhotoLoader.startObtainPhoto(mContext, mViewHeight, mViewWidth,
                                    info.mUri, info.mResourceType, info.mGreet, this);
        } else {
            //For decode exception maybe, so just show default picture.
            if (item.getTotalNumber() == 0) {
                Log.d(TAG, "loadRichPhoto, info no pic, show default!");
                mImageView.setVisibility(View.VISIBLE);
                mRichGIFView.setVisibility(View.GONE);
                mImageView.setImageResource(R.drawable.default_rich_screen);
                return;
            }

            if (info.mResourceType == RES_TYPE_PIC) {
                //Normal image file show
                mImageView.setVisibility(View.VISIBLE);
                mRichGIFView.setVisibility(View.GONE);

                mImageView.setImageBitmap(item.getCurrentBitmap());
                setGreetingView(info.mGreet);
            } else if (info.mResourceType == RES_TYPE_GIF) {
                //Gif images show
                mImageView.setVisibility(View.GONE);
                mRichGIFView.setVisibility(View.VISIBLE);

                mRichGIFView.setRect(mViewHeight, mViewWidth);
                mRichGIFView.setViewItem(item);
                setGreetingView(info.mGreet);
            } else {
                Log.d(TAG, "loadRichPhoto, unknown res type!");
            }
        }
    }

    public void stopRichPhoto() {
        Log.d(TAG, "stopRichPhoto");
        if (mRichGIFView != null) {
            mRichGIFView.pauseGif();
        }
    }

    @Override
    public void onImageLoadComplete(String uri, ArrayList<Bitmap> bitmaps,
                            ArrayList<Integer> intervals, int type, String greeting) {
        Log.d(TAG, "onImageLoadComplete, type = " + type);
        ViewItem item = new ViewItem();
        item.setResources(bitmaps, intervals, type);
        mViewCache.put(uri, item);

        if (bitmaps == null || bitmaps.size() == 0) {
            Log.d(TAG, "onImageLoadComplete, no pic!");
            mImageView.setVisibility(View.VISIBLE);
            mRichGIFView.setVisibility(View.GONE);
            mImageView.setImageResource(R.drawable.default_rich_screen);
            return;
        }

        if (type == RES_TYPE_PIC) {
            //Normal image file show
            mImageView.setVisibility(View.VISIBLE);
            mRichGIFView.setVisibility(View.GONE);

            mImageView.setImageBitmap(item.getCurrentBitmap());
            setGreetingView(greeting);
        } else if (type == RES_TYPE_GIF) {
            //GIF image file show
            mImageView.setVisibility(View.GONE);
            mRichGIFView.setVisibility(View.VISIBLE);

            mRichGIFView.setRect(mViewHeight, mViewWidth);
            mRichGIFView.setViewItem(item);
            setGreetingView(greeting);
        } else {
            Log.d(TAG, "onImageLoadComplete, unknown res type!");
        }
    }

    private void initViewScale() {
        if (mViewHeight == 0 || mViewWidth == 0) {
            if (mImageView != null) {
                mViewWidth = mImageView.getWidth();
                mViewHeight = mImageView.getHeight();
            }
        }

        if (mViewHeight == 0 || mViewWidth == 0) {
            if (mRichGIFView != null) {
                mViewWidth = mImageView.getWidth();
                mViewHeight = mImageView.getHeight();
            }
        }

        if (mViewHeight == 0 || mViewWidth == 0) {
            mViewWidth = mRCSInCallUIPlugin.getContactPhotoWidth();
            mViewHeight = mRCSInCallUIPlugin.getContactPhotoHeight();
        }
        Log.d(TAG, "initViewScale, mViewHeight = " + mViewHeight +
                                        ", mViewWidth = " + mViewWidth);
    }

    private void resetViewScale() {
        mRCSInCallUIPlugin = null;
        mViewWidth = 0;
        mViewHeight = 0;
    }

    public void clearPhotoCache() {
        Log.d(TAG, "clearPhotoCache");
        Iterator iterator = mViewCache.keySet().iterator();
        while (iterator.hasNext()) {
            String photoUri = (String) iterator.next();
            ViewItem item = (ViewItem) mViewCache.get(photoUri);
            int total = item.getTotalNumber();
            for (int i = total; i > 0; i--) {
                boolean result = item.removeBitmapById(i - 1);
            }
        }
        mViewCache.clear();

        //Reset Image size
        resetViewScale();
    }

    private void setGreetingView(String greeting) {
        if (!TextUtils.isEmpty(greeting) && mGreetingView != null) {
            mGreetingView.setVisibility(View.VISIBLE);
            mGreetingView.setText(greeting);
        }
    }

    public class ViewItem {
        private ArrayList<Bitmap> mBitMapCache = new ArrayList<Bitmap>();
        private ArrayList<Integer> mIntervals = new ArrayList<Integer>();
        private int  mViewType;
        private int  mBitmapId;
        private int  mIntegerId;
        private int  mTotalCount;
        public void setResources(ArrayList<Bitmap> bitmaps,
                                    ArrayList<Integer> intervals, int type) {
            mBitMapCache = bitmaps;
            mIntervals = intervals;
            mViewType = type;

            if (bitmaps != null) {
                mTotalCount = bitmaps.size();
            }

            Log.d(TAG, "ViewItem, setResources, mTotalCount = " + mTotalCount);
        }

        public void resetCurentId() {
            mBitmapId = 0;
            mIntegerId = 0;
        }

        public Bitmap getCurrentBitmap() {
            Bitmap bitmap = getBitmapById(mBitmapId);
            return bitmap;
        }

        public Bitmap getBitmapById(int index) {
            if (index < mTotalCount) {
                Bitmap bitmap = mBitMapCache.get(index);
                return bitmap;
            }
            return null;
        }

        public Bitmap getNextBitmap() {
            mBitmapId = mBitmapId % mTotalCount;
            if (mBitMapCache != null) {
                Bitmap bitmap = mBitMapCache.get(mBitmapId);
                mBitmapId++;
                return bitmap;
            }
            return null;
        }

        public boolean removeBitmapById(int index) {
            if (index < mTotalCount) {
                Bitmap bitmap = mBitMapCache.get(index);
                mBitMapCache.remove(bitmap);
                if (bitmap != null) {
                    bitmap.recycle();
                    return true;
                }
            }
            return false;
        }

        public int getTotalNumber() {
            return mTotalCount;
        }

        public int getInternalById(int index) {
            if (index < mTotalCount) {
                int internal = mIntervals.get(index);
                return internal;
            }
            return 0;
        }

        public int getCurrentInternal() {
            int internal = getInternalById(mIntegerId);
            return internal;
        }

        public int getNextInternal() {
            mIntegerId = mIntegerId % mTotalCount;
            if (mIntervals != null) {
                int internal = mIntervals.get(mIntegerId);
                mIntegerId++;
                return internal;
            }
            return 0;
        }
    }
}
