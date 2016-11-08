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

import android.database.Cursor;
import android.util.Log;
//import android.view.MenuItem;

import com.mediatek.mms.callback.IColumnsMapCallback;

import java.util.HashMap;
import java.util.Map;

/**
 * Op01MmsMultiDeleteAndForward.
 *
 */
public class Op01MmsMultiDeleteAndForward {
    private static final String TAG = "Mms/Op01MmsMultiDeleteAndForwardExt";

    private int mMenuForward = 0;
//    private MenuItem item = null;

    //add for multi-forward
    private Map<Long, BodyandAddress> mBodyandAddressItem;
    /**
     * BodyandAddress.
     *
     */
    public static class BodyandAddress {
        String mAddress;
        String mBody;
        int mBoxType;
        int mIpMsgId;
        /**
         * Construction.
         * @param mAddress String
         * @param mBody String
         * @param boxType int
         * @param mIpMsgId int
         */
        public BodyandAddress(String mAddress, String mBody, int boxType, int mIpMsgId) {
            super();
            this.mAddress = mAddress;
            this.mBody = mBody;
            this.mBoxType = boxType;
            this.mIpMsgId = mIpMsgId;
        }
    }

    /**
     * Op01MmsMultiDeleteAndForward.
     */
    public Op01MmsMultiDeleteAndForward() {
        initBodyandAddress();
    }

    /**
     * initBodyandAddress.
     */
    public void initBodyandAddress() {
        mBodyandAddressItem = new HashMap<Long, BodyandAddress>();
    }

    /**
     * setBodyandAddress.
     * @param cursor Cursor
     * @param columnsMap IColumnsMapCallback
     */
    public void setBodyandAddress(Cursor cursor, IColumnsMapCallback columnsMap) {
        try {
            if (!cursor.getString(columnsMap.getColumnMsgType()).equals("mms")) {
                long msgId = cursor.getLong(columnsMap.getColumnMsgId());
                String address = cursor.getString(columnsMap.getColumnSmsAddress());
                String body    = cursor.getString(columnsMap.getColumnSmsBody());
                int boxType = cursor.getInt(columnsMap.getColumnSmsType());
                int ipMsgId = cursor.getInt(columnsMap.getColumnSmsIpMessageId());
                Log.d(TAG, "initListMap mAddress = " + address + "mBody" + body +
                    ", boxid = " + boxType + ", ipMsgId = " + ipMsgId);
                if (ipMsgId >= 0) {
                    BodyandAddress  ba = new BodyandAddress(address, body, boxType, ipMsgId);
                    mBodyandAddressItem.put(msgId, ba);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "initListMap error", e);
        }
    }

    /**
     * getBody.
     * @param id long
     * @return String
     */
    public  String getBody(long id) {
        if (mBodyandAddressItem != null && mBodyandAddressItem.size() > 0) {
            if (mBodyandAddressItem.get(id) != null) {
                return mBodyandAddressItem.get(id).mBody;
            }
        }
        return null;
    }

    /**
     * getAddress.
     * @param id long
     * @return String
     */
    public  String getAddress(long id) {
        if (mBodyandAddressItem != null && mBodyandAddressItem.size() > 0) {
            return mBodyandAddressItem.get(id).mAddress;
        } else {
            return null;
        }
    }

    /**
     * getBoxType.
     * @param id long
     * @return box type
     */
    public int getBoxType(long id) {
        if (mBodyandAddressItem != null && mBodyandAddressItem.size() > 0) {
            return mBodyandAddressItem.get(id).mBoxType;
        } else {
            return -1;
        }
    }

    /**
     * getIpMsgId.
     * @param id long
     * @return message id
     */
    public int getIpMsgId(long id) {
        if (mBodyandAddressItem.size() > 0 && mBodyandAddressItem.get(id) != null) {
            return mBodyandAddressItem.get(id).mIpMsgId;
        } else {
            return 0;
        }
    }

    /**
     * clearBodyandAddressList.
     */
    public void clearBodyandAddressList() {
        if (mBodyandAddressItem != null) {
            mBodyandAddressItem.clear();
        }
    }
}

