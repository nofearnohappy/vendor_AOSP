/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
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
package com.mediatek.rcs.pam.util;


import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.util.ContextCacher;

/**
 * Defined the PageAdapter as the display adaptor for emotion icons
 */
public class PageAdapter extends BaseAdapter {

    private static final String TAG = "PageAdapter";
    private final Context mContext;
    private final List<Integer> mResIds;
    private final int mPage;
    private final HashMap<Integer,OnClickListener> mListenerMap = new HashMap<Integer,OnClickListener>();

    /**
     * OnEmotionItemSelectedListener listener list
     */
    private OnEmotionItemSelectedListener mListener;

    /**
     * Defined a listener to notify all the observer the specify emotion item is selected
     */
    public interface OnEmotionItemSelectedListener {
        void onEmotionItemSelectedListener(PageAdapter adapter, int position);
    }

    /**
     * Register the OnEmotionItemSelectedListener listener
     * 
     * @param listener The listener to be registered
     */
    public void registerListener(OnEmotionItemSelectedListener listener) {
        Log.d(TAG, "registerListener()");
        if (listener == null) {
        	Log.d(TAG, "registerListener() failed because listener is null");
            return;
        }
        mListener = listener;
    }

    /**
     * Unregister the OnEmotionItemSelectedListener listener
     */
    public void unregisterListener() {
        //Xlog.d(TAG, "unregisterListener()");
        if (mListener != null) {
            mListener = null;
        }
    }

    public PageAdapter(Context context, List<Integer> list, int page) {
        Log.d(TAG, "PageAdapter()");
        mContext = context;
        mResIds = list;
        mPage = page;
    }

    @Override
    public int getCount() {
        return mResIds.size();
    }

    @Override
    public Object getItem(int position) {
        return mResIds.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        Log.d(TAG, "getView() position=" + position);
        View currentView = null;
        if (convertView == null) {
//            currentView = new ImageView(mContext);
            Resources resources = ContextCacher.getPluginContext().getResources();
            XmlResourceParser xrp = resources.getLayout(R.layout.emoji_button);
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            ImageButton image = (ImageButton)inflater.inflate(xrp, null);
            currentView = image;
        } else {
            currentView = convertView;
        }
        Integer integer = (Integer)mResIds.get(position);
        Log.d(TAG, "getView() integer=" + integer);
        ImageView imageView = (ImageView) currentView;
        imageView.setLayoutParams(new GridView.LayoutParams(
            GridView.LayoutParams.WRAP_CONTENT, 
            GridView.LayoutParams.WRAP_CONTENT));
        imageView.setAdjustViewBounds(false);
      //  imageView.setScaleType(ImageView.ScaleType.CENTER);
        imageView.setImageDrawable(ContextCacher.getPluginContext().getResources().getDrawable(integer));
        OnClickListener listener = mListenerMap.get(Integer.valueOf(position));
        if (listener == null) {
            listener = new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onEmotionItemSelectedListener(PageAdapter.this, position);
                    }
                }
            };
            mListenerMap.put(Integer.valueOf(position), listener);
        }
        ((ImageView) currentView).setOnClickListener(listener);
        return imageView;
    }
}
