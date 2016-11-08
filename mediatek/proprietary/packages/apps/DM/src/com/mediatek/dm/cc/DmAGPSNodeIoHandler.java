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
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.dm.DmConst.TAG;
import com.mediatek.dm.DmService;
import com.mediatek.lbs.em2.utils.AgpsInterface;
import com.mediatek.lbs.em2.utils.SuplProfile;

import com.redbend.vdm.NodeIoHandler;
import com.redbend.vdm.VdmException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A handler for transfer AGPS related parameter values to session processor.
 */
public class DmAGPSNodeIoHandler implements NodeIoHandler {

    protected Context mContext;
    protected Uri mUri;
    protected String mValueToRead;
    protected Map<String, String> mMap = new HashMap<String, String>();

    private static final String APP_ID = "appId";
    private static final String PROVIDER_ID = "providerId";
    private static final String NAME = "name";
    private static final String DEFAULT_APN = "defaultApn";
    private static final String OPTION_APN = "optionApn";
    private static final String OPTION_APN2 = "optionApn2";
    private static final String ADDR = "addr";
    private static final String ADDR_TYPE = "addrType";

    private String[] mItem = { "IAPID", "ProviderID", "Name", "PrefConRef",
            "ToConRef", "ConRef", "SLP", "port" };


    private String[] mProjection = { APP_ID, PROVIDER_ID, NAME,
            DEFAULT_APN, OPTION_APN, OPTION_APN2, ADDR, ADDR_TYPE };

    /**
     * Constructor.
     * @param ctx context
     * @param treeUri AGPS node path of tree.xml
     */
    public DmAGPSNodeIoHandler(Context ctx, Uri treeUri) {
        Log.i(TAG.NODE_IO_HANDLER, "DmAGPSNodeIoHandler constructed");
        mContext = ctx;
        mUri = treeUri;

        for (int i = 0; i < mItem.length; i++) {
            mMap.put(mItem[i], mProjection[i]);
        }
    }

    public int read(int offset, byte[] data) throws VdmException {

        if (mUri == null) {
            throw new VdmException("AGPS read URI is null!");
        }

        String valueToRead = null;
        String uriPath = mUri.getPath();
        Log.i(TAG.NODE_IO_HANDLER, "uri: " + uriPath);
        Log.i(TAG.NODE_IO_HANDLER, "arg0: " + offset);
        if (DmService.sCCStoredParams.containsKey(uriPath)) {
            valueToRead = DmService.sCCStoredParams.get(uriPath);
            Log.d(TAG.NODE_IO_HANDLER,
                    "get valueToRead from mCCStoredParams, the value is "
                            + valueToRead);
        } else {
            int leafIndex = uriPath.lastIndexOf(File.separator);
            if (leafIndex == -1) {
                throw new VdmException(
                        "AGPS read URI is not valid, has no '/'!");
            }
            String leafValue = uriPath.substring(leafIndex + 1);

            String itemString = null;
            for (int i = 0; i < mItem.length; i++) {
                if (leafValue.equals(mItem[i])) {
                    itemString = mItem[i];
                    break;
                }
            }

            if (itemString == null) {
                return 0;
            }

            String profileString = mMap.get(itemString);
            if (profileString == null) {
                return 0;
            }
            try {
                AgpsInterface agpsInterface = new AgpsInterface();
                SuplProfile agpsProfile = agpsInterface.getAgpsConfig().curSuplProfile;

                if (APP_ID.equals(profileString)) {
                    valueToRead = agpsProfile.appId;
                } else if (PROVIDER_ID.equals(profileString)) {
                    valueToRead = agpsProfile.providerId;
                } else if (NAME.equals(profileString)) {
                    valueToRead = agpsProfile.name;
                } else if (DEFAULT_APN.equals(profileString)) {
                    valueToRead = agpsProfile.defaultApn;
                } else if (OPTION_APN.equals(profileString)) {
                    valueToRead = agpsProfile.optionalApn;
                } else if (OPTION_APN2.equals(profileString)) {
                    valueToRead = agpsProfile.optionalApn2;
                } else if (ADDR.equals(profileString)) {
                    valueToRead = agpsProfile.addr + ":" + agpsProfile.port;
                } else if (ADDR_TYPE.equals(profileString)) {
                    valueToRead = agpsProfile.addressType;
                }
                DmService.sCCStoredParams.put(uriPath, valueToRead);
                Log.d(TAG.NODE_IO_HANDLER,
                        "put valueToRead to mCCStoredParams, the value is "
                                + valueToRead);
            } catch (IOException e) {
                Log.e(TAG.NODE_IO_HANDLER, "get AgpsInterface error: IOException!");
            }
        }

        if (TextUtils.isEmpty(valueToRead)) {
            return 0;
        } else {
            byte[] temp = valueToRead.getBytes();
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
                valueToRead = null;
            } else if (numberRead < temp.length) {
                valueToRead = valueToRead.substring(data.length - offset);
            }
            return numberRead;
        }
    }

    public void write(int offset, byte[] data, int totalSize) throws VdmException {

        Log.i(TAG.NODE_IO_HANDLER, "uri: " + mUri.getPath());
        Log.i(TAG.NODE_IO_HANDLER, "data: " + new String(data));
        Log.i(TAG.NODE_IO_HANDLER, "offset: " + offset);
        Log.i(TAG.NODE_IO_HANDLER, "total size: " + totalSize);

        String valueToWrite = new String(data);

        if (valueToWrite.length() != totalSize) {
            Log.e(TAG.NODE_IO_HANDLER,
                    "AGPS: arg1's length is not equals with arg2, do nothing.");
            return;
        }

        String uriPath = mUri.toString();
        int leafIndex = uriPath.lastIndexOf("/");
        if (leafIndex == -1) {
            throw new VdmException("AGPS read URI is not valid, has no '/'!");
        }
        String leafValue = uriPath.substring(leafIndex + 1);

        String itemString = null;
        for (int i = 0; i < mItem.length; i++) {
            if (leafValue.equals(mItem[i])) {
                itemString = mItem[i];
                break;
            }
        }

        if (itemString == null) {
            Log.e(TAG.NODE_IO_HANDLER, "AGPS: itemString is null, do nothing.");
            return;
        }

        String profileString = mMap.get(itemString);
        if (profileString == null) {
            Log.e(TAG.NODE_IO_HANDLER, "AGPS: profileString is null, do nothing.");
            return;
        }

        try {
            AgpsInterface agpsInterface = new AgpsInterface();
            SuplProfile agpsProfile = agpsInterface.getAgpsConfig().curSuplProfile;

            if (APP_ID.equals(profileString)) {
                agpsProfile.appId = valueToWrite;
            } else if (PROVIDER_ID.equals(profileString)) {
                agpsProfile.providerId = valueToWrite;
            } else if (NAME.equals(profileString)) {
                agpsProfile.name = valueToWrite;
            } else if (DEFAULT_APN.equals(profileString)) {
                agpsProfile.defaultApn = valueToWrite;
            } else if (OPTION_APN.equals(profileString)) {
                agpsProfile.optionalApn = valueToWrite;
            } else if (OPTION_APN2.equals(profileString)) {
                agpsProfile.optionalApn2 = valueToWrite;
            } else if (ADDR.equals(profileString)) {
                int indexOfColon = valueToWrite.indexOf(":");
                if (indexOfColon == -1) {
                    Log.i(TAG.NODE_IO_HANDLER, "AGPS:the record to write for addr"
                            + " have not a : or have not port");
                    agpsProfile.addr = valueToWrite;
                } else {
                    agpsProfile.addr = valueToWrite.substring(0, indexOfColon);
                    try {
                        agpsProfile.port = Integer.valueOf(valueToWrite
                                .substring(indexOfColon + 1));
                    } catch (NumberFormatException nfe) {
                        Log.e(TAG.NODE_IO_HANDLER,
                                "AGPS:NumberFormatException,"
                                        + "the record to write for addr have an invalid port:"
                                        + valueToWrite.substring(indexOfColon + 1));
                    }
                }
            } else if (ADDR_TYPE.equals(profileString)) {
                agpsProfile.addressType = valueToWrite;
            }
            agpsInterface.setSuplProfile(agpsProfile);
        } catch (IOException e) {
            Log.e(TAG.NODE_IO_HANDLER, "get AgpsInterface error: IOException!");
        }
    }
}
