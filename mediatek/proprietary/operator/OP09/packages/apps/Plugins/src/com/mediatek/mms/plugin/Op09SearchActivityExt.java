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

package com.mediatek.mms.plugin;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.text.TextUtils;

import com.mediatek.mms.ext.DefaultOpSearchActivityExt;
import android.util.Log;

import com.mediatek.op09.plugin.R;

public class Op09SearchActivityExt extends DefaultOpSearchActivityExt {
    private static final String TAG = "Mms/Op09SearchActivityExt";
    public static final String ADVANCED_SEARCH_QUERY = "advanced_query";
    public static final String ADVANCED_SEARCH_CONTENT = "content";
    public static final String ADVANCED_SEARCH_NAME = "name";
    public static final String ADVANCED_SEARCH_NUMBER = "number";
    public static final String ADVANCED_SEARCH_BEGIN_DATE = "begin_date";
    public static final String ADVANCED_SEARCH_END_DATE = "end_date";

    private static final String ADVANCE_URI = "content://mms-sms/searchAdvanced";

    private String mContent;
    private String mName;
    private String mNumber;
    private long mBeginDate;
    private long mEndDate;
    private boolean mIsAdvancedQuery;
    private Context mContext;

    public Op09SearchActivityExt(Context context) {
        mContext = context;
    }

    @Override
    public Uri startQuery(boolean folderMode, String searchString) {
        if (!mIsAdvancedQuery) {
            return null;
        }
        Builder builder = Uri.parse(ADVANCE_URI).buildUpon();
        if (!TextUtils.isEmpty(mContent)) {
            builder.appendQueryParameter(ADVANCED_SEARCH_CONTENT, mContent);
        }
        if (!TextUtils.isEmpty(mName)) {
            builder.appendQueryParameter(ADVANCED_SEARCH_NAME, mName);
        }
        if (!TextUtils.isEmpty(mNumber)) {
            builder.appendQueryParameter(ADVANCED_SEARCH_NUMBER, mNumber);
        }
        if (mBeginDate > 0L) {
            builder.appendQueryParameter(ADVANCED_SEARCH_BEGIN_DATE, mBeginDate + "");
        }
        if (mEndDate > 0L) {
            builder.appendQueryParameter(ADVANCED_SEARCH_END_DATE, mEndDate + "");
        }
        Log.d("@M_" + TAG, "buildAdvanceSearchUri: uri:" + builder.build().toString());
        return builder.build();
    }

    @Override
    public void onCreate(Context context, Intent intent) {
        Log.d("@M_" + TAG, "initAdvancedData");
        mIsAdvancedQuery = intent.getBooleanExtra(ADVANCED_SEARCH_QUERY,false);
        mContent = intent.getStringExtra(ADVANCED_SEARCH_CONTENT);
        mName = intent.getStringExtra(ADVANCED_SEARCH_NAME);
        mNumber = intent.getStringExtra(ADVANCED_SEARCH_NUMBER);
        mBeginDate = intent.getLongExtra(ADVANCED_SEARCH_BEGIN_DATE, 0);
        mEndDate = intent.getLongExtra(ADVANCED_SEARCH_END_DATE, 0);
    }

    @Override
    public String getSearchString() {
        if (mIsAdvancedQuery) {
            return "";
        } else {
            return null;
        }
    }

    @Override
    public CharSequence onQueryComplete(Cursor c) {
        int cursorCount = (c == null) ? 0 : c.getCount();
        if (mIsAdvancedQuery) {
            return mContext.getResources().getQuantityString(
                    R.plurals.advance_search_results_title, cursorCount, cursorCount);
        } else {
            return null;
        }
    }
}
