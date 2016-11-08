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
import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.PlatformManager;
import com.mediatek.mediatekdm.iohandler.DmDBNodeIoHandler;
import com.mediatek.mediatekdm.mdm.MdmException;

public class DmConnNodeIoHandler extends DmDBNodeIoHandler {
    private String[] mItem = { "Cmnet/Name", "Cmnet/apn", "Cmwap/Name", "Cmwap/apn", "gprs/Addr",
            "PortNbr", "csd0/Addr", "apn", "ProxyAddr", "ProxyPort" };
    private String[] mContentValue = { null, null, null, null, null, null, null };
    String[] mProjection = { "apn", "name", "name", "apn", "proxy", "port", "csdnum", "apn",
            "proxy", "port" };

    public DmConnNodeIoHandler(Context ctx, Uri treeUri, String mccMnc) {
        Log.i(TAG.NODEIOHANDLER, "DmConnNodeIoHandler constructed");

        mContext = ctx;
        mUri = treeUri;
        mMccMnc = mccMnc;
        for (int i = 0; i < mItem.length; i++) {
            mMap.put(mItem[i], mProjection[i]);
        }
    }

    public void write(int arg0, byte[] arg1, int arg2) throws MdmException {

        if (mRecordToWrite == null) {
            mRecordToWrite = new String();
        }
        mRecordToWrite = new String(arg1);
        if (mRecordToWrite.length() == arg2) {
            String uriPath = mUri.getPath();
            String uriPathName = "";
            if (uriPath != null) {
                if (uriPath.indexOf("Conn") != -1) {
                    int indexOfConn = uriPath.lastIndexOf("Conn");
                    if (indexOfConn == -1) {
                        Log.e(TAG.NODEIOHANDLER, "index of Conn of mUri is null");
                        return;
                    }
                    uriPathName = uriPath.substring(indexOfConn + 5);
                } else {
                    int indexOfSlash = uriPath.lastIndexOf("/");
                    if (indexOfSlash == -1) {
                        Log.e(TAG.NODEIOHANDLER, "index of / of mUri is null");
                        return;
                    }
                    uriPathName = uriPath.substring(indexOfSlash + 1);
                }

            } else {
                Log.e(TAG.NODEIOHANDLER, "mUri.getPath is null!");
                return;
            }

            Log.i(TAG.NODEIOHANDLER, "uriPathName = " + uriPathName + " value = " + mRecordToWrite);

            for (int i = 0; i < mItem.length; i++) {
                if (uriPathName.equals(mItem[i])) {
                    ContentValues values = new ContentValues();
                    if ((String) mMap.get(mItem[i]) != null) {
                        values.put((String) mMap.get(mItem[i]), mRecordToWrite);
                        mContext.getContentResolver().update(getTableToBeQueryed(), values,
                                buildSqlString(getMccMnc()), null);
                    }
                    mRecordToWrite = null;
                    break;
                }
            }
        }
    }

    protected String buildSqlString(String mccMnc) {
        String mcc = mccMnc.substring(0, 3);
        String mnc = mccMnc.substring(3);
        // for cmcc
        if (mcc.equals("460") && (mnc.equals("00") || mnc.equals("02") || mnc.equals("07"))) {
            if (mUri.getPath().contains(mItem[0]) || mUri.getPath().contains(mItem[1])) {
                return "mcc='460' AND type='default,supl,net' AND (mnc='" + mnc + "')";
            } else {
                return "mcc='460' AND (type = '' OR type is null) AND (mnc='" + mnc + "')";
            }
        } else if (mcc.equalsIgnoreCase("460") && mnc.equals("01")) {
            return "mcc='460' AND (type is null) AND mnc='01'";
        }
        // need to add some code for other operator
        return "mcc='" + mcc + "' AND mnc='" + mnc + "'";
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
        return PlatformManager.getInstance().getContentUri();
    }

}
