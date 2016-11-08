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

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.mediatek.dm.DmConst;
import com.mediatek.dm.DmConst.TAG;
import com.redbend.vdm.VdmException;

public class DmConnNodeIoHandler extends DmNodeIoHandlerForDB {

    private String[] mItem = { "Cmnet/Name", "Cmnet/apn", "Cmwap/Name", "Cmwap/apn", "gprs/Addr",
            "PortNbr", "csd0/Addr", "apn", "ProxyAddr", "ProxyPort" };
    private String[] mContentValue = { null, null, null, null, null, null, null };
    String[] mProjection = { "name", "apn", "name", "apn", "proxy", "port", "csdnum", "apn",
            "proxy", "port" };

    Uri mTable = Uri.parse("content://telephony/carriers");

    public DmConnNodeIoHandler(Context ctx, Uri treeUri, String mccMnc) {
        Log.i(TAG.NODE_IO_HANDLER, "DmConnNodeIoHandler constructed");
        mContext = ctx;
        mUri = treeUri;
        mMccmnc = mccMnc;
        for (int i = 0; i < mItem.length; i++) {
            mMap.put(mItem[i], mProjection[i]);
        }
    }

    public void write(int offset, byte[] data, int totalSize) throws VdmException {
        Log.i(TAG.NODE_IO_HANDLER, "ConnNodeIoHandler write");
        mRecordToWrite = new String(data);
        if (mRecordToWrite.length() == totalSize) {

            String uriPath = mUri.getPath();
            String uriPathName = "";
            if (uriPath != null) {
                if (uriPath.indexOf("Conn") != -1) {
                    int indexOfConn = uriPath.lastIndexOf("Conn");
                    if (indexOfConn == -1) {
                        Log.e(TAG.NODE_IO_HANDLER, "index of Conn of uri is null");
                        return;
                    }
                    uriPathName = uriPath.substring(indexOfConn + 5);
                } else {
                    int indexOfSlash = uriPath.lastIndexOf("/");
                    if (indexOfSlash == -1) {
                        Log.e(TAG.NODE_IO_HANDLER, "index of / of uri is null");
                        return;
                    }
                    uriPathName = uriPath.substring(indexOfSlash + 1);
                }

            } else {
                Log.e(TAG.NODE_IO_HANDLER, "uri.getPath is null!");
                return;
            }

            for (int i = 0; i < mItem.length; i++) {
                String item = mItem[i];
                if (uriPathName.equals(item)) {
                    ContentValues values = new ContentValues();
                    if ((String) mMap.get(item) != null) {
                        values.put((String) mMap.get(item), mRecordToWrite);
                        mContext.getContentResolver().update(mTable, values,
                                buildSqlString(mMccmnc), null);
                    }
                    mRecordToWrite = null;
                    break;
                }
            }
        }
    }

    protected String buildSqlString(String mccMnc) {
        if (mccMnc == null || mccMnc.length() < 5) {
            Log.e(TAG.NODE_IO_HANDLER, "buildSqlString(), mccMnc invalid!");
            return null;
        }

        String mcc = mccMnc.substring(0, 3);
        String mnc = mccMnc.substring(3);
        // for cmcc
        if (mcc.equals(DmConst.MCC_460) && (mnc.equals(DmConst.MNC_00)
                || mnc.equals(DmConst.MNC_02) || mnc.equals(DmConst.MNC_07))) {
            if (mUri.getPath().contains(mItem[0]) || mUri.getPath().contains(mItem[1])) {
                return "mcc='" + mcc + "' AND type='default,supl,net' AND (mnc='" + mnc + "')";
            } else {
                return "mcc='" + mcc + "' AND type='default,supl,wap' AND (mnc='" + mnc + "')";
            }

        } else if (mcc.equalsIgnoreCase(DmConst.MCC_460) && mnc.equals(DmConst.MNC_01)) {
            return "mcc='" + mcc + "' AND (type is null) AND mnc='" + mnc
                    + "' AND (sourcetype='0')";
        }
        // need to add some code for other operator
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mcc='").append(mcc).append("' AND mnc='").append(mnc).append("'");

        return stringBuilder.toString();
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
