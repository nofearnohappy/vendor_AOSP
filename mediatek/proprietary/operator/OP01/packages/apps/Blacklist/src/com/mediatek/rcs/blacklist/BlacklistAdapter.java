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

import android.content.Context;
import android.database.Cursor;
//import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.mediatek.rcs.blacklist.BlacklistData.BlacklistTable;

/**
 * BlacklistAdapter.
 * list adapter for member list and delete screen
 */
public class BlacklistAdapter extends SimpleCursorAdapter {

    private static final String[] LISTCOLUMNS = {BlacklistTable.DISPLAY_NAME,
                                                 BlacklistTable.PHONE_NUMBER};
    private static final int[] RESOURCEVIEW = {R.id.display_name, R.id.number};

    public static final int LIST_VIEW_NORMAL = 0;
    public static final int LIST_VIEW_PICKER = 1;
    public static final int SELECT_ALL = 1;
    public static final int DESELECT_ALL = 2;


    private int mMode = LIST_VIEW_NORMAL;
    private CheckStatusCallBack mCheckStatusCallBack = null;

    /**
     * Constructor.
     * @param context Context
     * @param layout int
     * @param c Cursor
     * @param from String[]
     * @param to String[]
     */
    public BlacklistAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
        super(context, layout, c, from, to, FLAG_REGISTER_CONTENT_OBSERVER);
    }

    /**
     * Constructor.
     * @param context Context
     * @param c Cursor
     */
    public BlacklistAdapter(Context context, Cursor c) {
        super(context, R.layout.blacklist_item, c, LISTCOLUMNS, RESOURCEVIEW,
                FLAG_REGISTER_CONTENT_OBSERVER);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final CheckBox checkView = (CheckBox) view.findViewById(R.id.checked);
        final TextView nameView = (TextView) view.findViewById(RESOURCEVIEW[0]);
        final TextView numberView = (TextView) view.findViewById(RESOURCEVIEW[1]);

        boolean bNumberShowAsName = false;

        if (mMode == 0) {
            checkView.setVisibility(View.GONE);
        } else {
            checkView.setVisibility(View.VISIBLE);
        }

        if (mMode == LIST_VIEW_PICKER) {
            if (mCheckStatusCallBack != null) {
                boolean status = mCheckStatusCallBack.getItemChecked(cursor.getPosition());
                checkView.setChecked(status);
            }
        }

        String name = cursor.getString(cursor.getColumnIndexOrThrow(LISTCOLUMNS[0]));
        String number = cursor.getString(cursor.getColumnIndexOrThrow(LISTCOLUMNS[1]));

        if (name == null || name.isEmpty()) {
            bNumberShowAsName = true;
        }

        if (bNumberShowAsName) {
            nameView.setText(number);
            numberView.setVisibility(View.GONE);
        } else {
            nameView.setText(name);
            numberView.setText(number);
            numberView.setVisibility(View.VISIBLE);
        }
    }

    protected void setListViewMode(int mode) {
        mMode = mode;
        if (mode != LIST_VIEW_PICKER) {
            mCheckStatusCallBack = null;
        }
    }

    /**
     * CheckStatusCallBack
     * For delete fragment, set check box status.
     */
    protected interface CheckStatusCallBack {
        boolean getItemChecked(int id);
    }

    protected void setCheckStatusCallBack(CheckStatusCallBack cb) {
        mCheckStatusCallBack = cb;
    }
}
