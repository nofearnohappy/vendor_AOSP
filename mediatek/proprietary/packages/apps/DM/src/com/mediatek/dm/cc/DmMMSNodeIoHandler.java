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
import android.util.Log;

import com.mediatek.dm.DmConst;
import com.mediatek.dm.DmConst.TAG;

public class DmMMSNodeIoHandler extends DmNodeIoHandlerForDB {

    private String[] mItem = { "Default", "MSCCenter" };
    private String[] mContentValue = { null, null };

    private String[] mProjection = { "name", "mmsc" };

    private Uri mTable = Uri.parse("content://telephony/carriers");

    public DmMMSNodeIoHandler(Context ctx, Uri treeUri, String mccMnc) {
        mContext = ctx;
        mUri = treeUri;
        mMccmnc = mccMnc;
        for (int i = 0; i < mItem.length; i++) {
            mMap.put(mItem[i], mProjection[i]);
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
            return "mcc='" + mcc + "' AND type='mms' AND (mnc='" + mnc + "') AND (sourcetype='0')";
        } else if (mcc.equals(DmConst.MCC_460) && mnc.equals(DmConst.MNC_01)) {
            return "mcc='" + mcc + "' AND type='mms' AND mnc='" + mnc + "' AND (sourcetype='0')";
        }
        // need to add some code for other operator
        return null;
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
