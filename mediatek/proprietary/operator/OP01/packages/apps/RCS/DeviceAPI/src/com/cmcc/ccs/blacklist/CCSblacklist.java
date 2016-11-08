/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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

package com.cmcc.ccs.blacklist;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

/**
 * This class is responsible for adding a number to device's blacklist or
 * a number from device's blacklist.
 */
public class CCSblacklist {

    private static final String TAG = "Blacklist";

    private static final Uri CONTENT_URI = Uri.parse("content://com.cmcc.ccs.black_list");

    /**
     * The phone number as the user entered it.
     * This field is mandatory.
     * <P>Type: TEXT </P>
     */
    public static final String PHONE_NUMBER = "PHONE_NUMBER";

    /**
     * The name as the user entered it.
     * This field is optional.
     * <P>Type: TEXT </P>
     */
    public static final String NAME = "NAME";

    private Context mContext;

    /**
     * Create an instance to invoke it's methods.
     * @param ctx Context
     */
    public CCSblacklist(Context ctx) {
        mContext = ctx;
    }

    /**
     * Add a pair of number and name to device's blacklist.
     *
     * @param contact the phone number, it's mandatory.
     * @param name the display name, it's optional.
     *
     * @return ture if the data was added successfully, otherwise false.
     */
    public boolean addblackNumber(String contact, String name) {

        log("adddblackNumber");

        ContentValues values = new ContentValues();
        values.put(PHONE_NUMBER, contact);
        if (name != null && !name.isEmpty()) {
            values.put(NAME, name);
        }

        Uri resultUri = mContext.getContentResolver().insert(CONTENT_URI, values);

        if (resultUri == null) {
            return false;
        }

        return true;
    }

    /**
     * Remove a number from device's blacklist.
     *
     * @param contact the phone number, it's mandatory.
     *
     * @return ture if the data was removed successfully, otherwise false.
     */
    public boolean removeblackNumber(String contact) {

        log("removeblackNumber");

        Uri uri = Uri.withAppendedPath(CONTENT_URI, Uri.encode(contact));

        int retCount = mContext.getContentResolver().delete(uri, null, null);

        if (retCount <= 0) {
            return false;
        }

        return true;
    }

    private void log(String message) {
        Log.d(TAG, "[" + getClass().getSimpleName() + "] " + message);
    }
}
