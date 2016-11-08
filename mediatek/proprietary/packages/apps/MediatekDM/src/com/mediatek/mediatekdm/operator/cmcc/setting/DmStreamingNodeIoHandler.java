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

package com.mediatek.mediatekdm.operator.cmcc.setting;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.iohandler.DmDBNodeIoHandler;
import com.mediatek.mediatekdm.mdm.MdmException;

public class DmStreamingNodeIoHandler extends DmDBNodeIoHandler {
    private final String[] mItem = { "Name", "To-Proxy", "To-NapID", "NetInfo", "MIN-UDP-PORT",
            "MAX-UDP-PORT" };

    private String[] mContentValue = { null };

    String[] mProjection = { "mtk_rtsp_name", "mtk_rtsp_to_proxy", "mtk_rtsp_to_napid",
            "mtk_rtsp_netinfo", "mtk_rtsp_min_udp_port", "mtk_rtsp_max_udp_port" };

    Uri mTable = Uri.parse("content://media/internal/streaming/omartspsetting");

    public DmStreamingNodeIoHandler(Context context, Uri streamingUri) {
        mContext = context;
        mUri = streamingUri;
        for (int i = 0; i < mItem.length; i++) {
            mMap.put(mItem[i], mProjection[i]);
        }
    }

    public int read(int arg0, byte[] arg1) throws MdmException {

        String uriKey = mUri.getPath();
        Log.i(TAG.NODEIOHANDLER, "mUri: " + uriKey);
        Log.i(TAG.NODEIOHANDLER, "arg0: " + arg0);
        mRecordToRead = new String();
        for (int i = 0; i < getItem().length; i++) {
            if (mUri.getPath().contains(getItem()[i])) {
                if ((String) mMap.get(getItem()[i]) != null) {
                    if (mRecordToRead.length() == 0) {
                        mRecordToRead = Settings.System.getString(mContext.getContentResolver(),
                                (String) mMap.get(getItem()[i]));
                    }
                } else {
                    mRecordToRead += getContentValue()[i];
                }
                break;
            }
        }

        if (TextUtils.isEmpty(mRecordToRead)) {
            return 0;
        } else {
            byte[] temp = mRecordToRead.getBytes();
            if (arg1 == null) {
                return temp.length;
            }
            int numberRead = 0;
            for (; numberRead < arg1.length - arg0; numberRead++) {
                if (numberRead < temp.length) {
                    arg1[numberRead] = temp[arg0 + numberRead];
                } else {
                    break;
                }
            }
            if (numberRead < arg1.length - arg0) {
                mRecordToRead = null;
            } else if (numberRead < temp.length) {
                mRecordToRead = mRecordToRead.substring(arg1.length - arg0);
            }
            return numberRead;
        }
    }

    public void write(int arg0, byte[] arg1, int arg2) throws MdmException {

        Log.i(TAG.NODEIOHANDLER, "mUri: " + mUri.getPath());
        Log.i(TAG.NODEIOHANDLER, "arg1: " + new String(arg1));
        Log.i(TAG.NODEIOHANDLER, "arg0: " + arg0);
        Log.i(TAG.NODEIOHANDLER, "arg2: " + arg2);

        mRecordToWrite = new String(arg1);
        if (mRecordToWrite.length() == arg2) {
            for (int i = 0; i < getItem().length; i++) {
                if (mUri.getPath().contains(getItem()[i])) {
                    ContentValues values = new ContentValues();
                    if ((String) mMap.get(getItem()[i]) != null) {
                        if (!specificHandlingForWrite(mRecordToWrite, values, getItem()[i])) {
                            Settings.System.putString(mContext.getContentResolver(),
                                    (String) mMap.get(getItem()[i]),
                                    mRecordToWrite);
                        }
                    }
                    mRecordToWrite = null;
                    break;
                }
            }
        }
    }

    @Override
    protected String[] getContentValue() {
        return mContentValue;
    }

    @Override
    protected String[] getItem() {
        return mItem;
    }

    @Override
    protected String[] getProjection() {
        return mProjection;
    }

    @Override
    protected Uri getTableToBeQueryed() {
        return mTable;
    }

}
