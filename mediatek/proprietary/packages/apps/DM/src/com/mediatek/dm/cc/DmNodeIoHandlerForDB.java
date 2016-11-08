/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.dm.cc;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.dm.DmConst.TAG;
import com.mediatek.dm.DmService;
import com.redbend.vdm.NodeIoHandler;
import com.redbend.vdm.VdmException;

import java.util.HashMap;
import java.util.Map;

public abstract class DmNodeIoHandlerForDB implements NodeIoHandler {

    protected Context mContext;
    protected Uri mUri;
    protected String mMccmnc;
    protected String mRecordToWrite;
    protected String mRecordToRead;

    protected Map<String, String> mMap = new HashMap<String, String>();

    public int read(int offset, byte[] data) throws VdmException {
        String recordToRead = null;
        String uriPath = mUri.getPath();
        Log.i(TAG.NODE_IO_HANDLER, "uri: " + uriPath);
        Log.i(TAG.NODE_IO_HANDLER, "offset: " + offset);

        if (DmService.sCCStoredParams.containsKey(uriPath)) {
            recordToRead = DmService.sCCStoredParams.get(uriPath);
            Log.d(TAG.NODE_IO_HANDLER,
                    "get valueToRead from mCCStoredParams, the value is "
                            + recordToRead);
        } else {
            recordToRead = new String();
            for (int i = 0; i < getItem().length; i++) {
                if (mUri.getPath().contains(getItem()[i])) {
                    if ((String) mMap.get(getItem()[i]) != null) {
                        if (specificHandlingForRead(getItem()[i]) != null) {
                            recordToRead = specificHandlingForRead(getItem()[i]);
                            break;
                        }
                        if (recordToRead.length() == 0) {
                            Cursor cur = null;
//                            try {
                                cur = mContext.getContentResolver().query(
                                        getTableToBeQueryed(), getProjection(),
                                        buildSqlString(mMccmnc), null, null);
                                if (cur != null && cur.moveToFirst()) {
                                    int col = cur.getColumnIndex((String) mMap
                                            .get(getItem()[i]));
                                    recordToRead = cur.getString(col);
                                }
//                            } catch (Exception e) {
//                                throw new VdmException(0xFFFF);
//                            } finally {
                                if (cur != null) {
                                    cur.close();
                                }
//                            }
                        }
                    } else {
                        recordToRead = getContentValue()[i];
                    }
                    break;
                }
            }
            DmService.sCCStoredParams.put(uriPath, recordToRead);
            Log.d(TAG.NODE_IO_HANDLER,
                    "put valueToRead to mCCStoredParams, the value is "
                            + recordToRead);
        }
        Log.v(TAG.NODE_IO_HANDLER, "recordToRead" + recordToRead);
        if (TextUtils.isEmpty(recordToRead)) {
            return 0;
        } else {
            byte[] temp = recordToRead.getBytes();
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
                recordToRead = null;
            } else if (numberRead < temp.length) {
                recordToRead = recordToRead.substring(data.length - offset);
            }
            return numberRead;
        }
    }

    public void write(int offset, byte[] data, int totalSize) throws VdmException {

        Log.i(TAG.NODE_IO_HANDLER, "uri: " + mUri.getPath());
        Log.i(TAG.NODE_IO_HANDLER, "data: " + new String(data));
        Log.i(TAG.NODE_IO_HANDLER, "offset: " + offset);
        Log.i(TAG.NODE_IO_HANDLER, "total size: " + totalSize);

        // if (recordToWrite == null) {
        // recordToWrite = new String();
        // }
        mRecordToWrite = new String(data);
        if (mRecordToWrite.length() == totalSize) {
            // Modify: added************start
            String uriPath = mUri.getPath();
            String uriPathName = "";
            if (uriPath != null) {
                int indexOfSlash = uriPath.lastIndexOf("/");
                if (indexOfSlash == -1) {
                    Log.e("DmService", "index of / of uri is null");
                    return;
                }
                uriPathName = uriPath.substring(indexOfSlash + 1);
            } else {
                Log.e("DmService", "uri.getPath is null!");
                return;
            }

            int length = getItem().length;
            for (int i = 0; i < length; i++) {
                String item = getItem()[i];
                if (item.equals(uriPathName)) {
                    ContentValues values = new ContentValues();
                    if ((String) mMap.get(item) != null) {
                        if (!specificHandlingForWrite(mRecordToWrite, values,
                                item)) {
                            values.put((String) mMap.get(item),
                                    mRecordToWrite);
                        }
                    } else {
                        mRecordToWrite = null;
                        break;
                    }
                    if (mContext.getContentResolver().update(
                            getTableToBeQueryed(), values,
                            buildSqlString(mMccmnc), null) == 0) {
                        if (getInsertUri() == null) {
                            setInsertUri(mContext.getContentResolver().insert(
                                    getTableToBeQueryed(), values));
                        } else {
                            if (mContext != null) {
                                mContext.getContentResolver().update(
                                        getInsertUri(), values, null, null);
                            }
                        }
                    }
                    mRecordToWrite = null;
                    break;
                }
            }
        }
    }

    protected boolean specificHandlingForWrite(String str, ContentValues cv,
            String item) {
        return false;
    }

    protected String specificHandlingForRead(String item) {
        return null;
    }

    protected void setInsertUri(Uri uri) {
    }

    protected Uri getInsertUri() {
        return null;
    }

    protected String buildSqlString(String mccMnc) {
        return null;
    }

    protected abstract String[] getItem();

    protected abstract String[] getProjection();

    protected abstract Uri getTableToBeQueryed();

    protected abstract String[] getContentValue();
}
