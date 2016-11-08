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

package com.mediatek.dm.cc;

import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore.Streaming;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.dm.DmConst.TAG;
import com.mediatek.dm.DmService;
import com.redbend.vdm.NodeIoHandler;
import com.redbend.vdm.VdmException;

import java.util.HashMap;
import java.util.Map;

public class DmStreamingNodeIoHandler implements NodeIoHandler {

    private final String[] mItem = { "Name", "To-Proxy", "To-NapID", "NetInfo", "MIN-UDP-PORT",
            "MAX-UDP-PORT" };

    private String[] mProjection = { Streaming.Setting.NAME, Streaming.Setting.TO_PROXY, Streaming.Setting.TO_NAPID,
            Streaming.Setting.NETINFO, Streaming.Setting.MIN_UDP_PORT, Streaming.Setting.MAX_UDP_PORT };

    private Context mContext;
    private Uri mUri;
    private String mRecordToRead;
    private String mRecordToWrite;
    private Map<String, String> mMap = new HashMap<String, String>();

    public DmStreamingNodeIoHandler(Context context, Uri streamingUri) {
        mContext = context;
        mUri = streamingUri;
        int length = mItem.length;
        for (int i = 0; i < length; i++) {
            mMap.put(mItem[i], mProjection[i]);
        }
    }

    public int read(int offset, byte[] data) throws VdmException {

        String uriKey = mUri.getPath();
        Log.i(TAG.NODE_IO_HANDLER, "uri: " + uriKey);
        Log.i(TAG.NODE_IO_HANDLER, "offset: " + offset);
        if (DmService.sCCStoredParams.containsKey(uriKey)) {
            mRecordToRead = DmService.sCCStoredParams.get(uriKey);
            Log.d(TAG.NODE_IO_HANDLER, "get valueToRead from mCCStoredParams, the value is "
                    + mRecordToRead);
        } else {
            int length = mItem.length;
            for (int i = 0; i < length; i++) {
                String item = mItem[i];
                if (mUri.getPath().contains(item)) {
                    if ((String) mMap.get(item) != null) {
                        mRecordToRead = Settings.System.getString(mContext.getContentResolver(),
                                (String) mMap.get(item));
                    }
                    break;
                }
            }
            DmService.sCCStoredParams.put(uriKey, mRecordToRead);
            Log.d(TAG.NODE_IO_HANDLER, "put valueToRead to mCCStoredParams, the value is "
                    + mRecordToRead);
        }

        if (TextUtils.isEmpty(mRecordToRead)) {
            return 0;
        } else {
            byte[] temp = mRecordToRead.getBytes();
            if (data == null) {
                return temp.length;
            }
            int numberRead = 0;
            for (; numberRead < data.length - offset; numberRead++) {
                if (numberRead < temp.length) {
                    data[numberRead] = temp[offset + numberRead];
                } else {
                    break;
                }
            }
            if (numberRead < data.length - offset) {
                mRecordToRead = null;
            } else if (numberRead < temp.length) {
                mRecordToRead = mRecordToRead.substring(data.length - offset);
            }
            return numberRead;
        }
    }

    public void write(int offset, byte[] data, int totalSize) throws VdmException {

        Log.i(TAG.NODE_IO_HANDLER, "uri: " + mUri.getPath());
        Log.i(TAG.NODE_IO_HANDLER, "data: " + new String(data));
        Log.i(TAG.NODE_IO_HANDLER, "offset: " + offset);
        Log.i(TAG.NODE_IO_HANDLER, "total size: " + totalSize);

        mRecordToWrite = new String(data);
        if (mRecordToWrite.length() == totalSize) {
            int length = mItem.length;
            for (int i = 0; i < length; i++) {
                if (mUri.getPath().contains(mItem[i])) {
                    if ((String) mMap.get(mItem[i]) != null) {
                        Settings.System.putString(mContext.getContentResolver(),
                                (String) mMap.get(mItem[i]), mRecordToWrite);
                    }
                    mRecordToWrite = null;
                    break;
                }
            }
        }
    }
}
