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

package com.mediatek.dm.test.ims;

import android.net.Uri;
import android.telephony.SubscriptionManager;
import android.test.AndroidTestCase;
import android.util.Log;

import com.android.ims.ImsConfig;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.ims.mo.ImsPhoneCtx;
import com.mediatek.dm.ims.DmPhoneCtxNodeIoHandler;

import com.redbend.vdm.VdmException;

import java.io.File;

import junit.framework.Assert;

public class PhoneCtxListTests extends AndroidTestCase {
    private static final String TAG = "[DmPhoneCtxListTests]";

    private final static String[] SUB_NODE = { "1", "2", "3", "4" };
    private final static String[] LEAF_NODE = { "PhoneContext", "Public_user_ID1",
            "Public_user_ID2", "Public_user_ID3", "Public_user_ID4" };

    private final static String PARENT_URI = "./3GPP_IMS/PhoneContext_List/";

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testRead() {
        ImsManager imsManager = ImsManager.getInstance(mContext, SubscriptionManager.getDefaultSubId());

        try {
            ImsConfig imsConfig = imsManager.getConfigInterface();
            ImsPhoneCtx[] phoneCtxList = imsConfig.getMasterImsPhoneCtxValue();
            int length = phoneCtxList.length;

            for (int i = 0; i < length; ++i) {
                String uriPath = PARENT_URI + SUB_NODE[i] + File.separator + LEAF_NODE[0];
                String result = readHandler(uriPath);
                Log.d(TAG, "[testRead]result: " + result + "expect: " + phoneCtxList[i].getPhoneCtx());
                assertTrue(result == null || result.equals(phoneCtxList[i].getPhoneCtx()));

                int leafLength = LEAF_NODE.length;
                for (int j = 0; j < leafLength - 1; ++j) {
                    uriPath = PARENT_URI + SUB_NODE[i] + File.separator + LEAF_NODE[j + 1];
                    result = readHandler(uriPath);
                    Log.d(TAG, "[testRead]result: " + result + "expect: " + phoneCtxList[i].getPhoneCtxIpuis()[j]);
                    assertTrue(result == null || result.equals(phoneCtxList[i].getPhoneCtxIpuis()[j]));
                }
            }
        } catch (ImsException e) {
            e.printStackTrace();
        }
    }

    private String readHandler(String uriPath) {
        DmPhoneCtxNodeIoHandler handler = new DmPhoneCtxNodeIoHandler(mContext, Uri.parse(uriPath));
        Log.d(TAG, "[readHandler]" + uriPath);

        byte[] buffer = new byte[64];
        int len = 0;
        try {
            len = handler.read(0, buffer);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }

        String readResult = null;
        if (len > 0) {
            readResult = new String(buffer, 0, len);
        }
        return readResult;
    }

    public void testWrite() {
        ImsManager imsManager = ImsManager.getInstance(mContext, SubscriptionManager.getDefaultSubId());

        try {
            ImsConfig imsConfig = imsManager.getConfigInterface();
            ImsPhoneCtx[] phoneCtxList = imsConfig.getMasterImsPhoneCtxValue();
            int length = phoneCtxList.length;

            for (int i = 0; i < length; ++i) {
                String uriPath = PARENT_URI + SUB_NODE[i] + File.separator + LEAF_NODE[0];
                String recordToWrite = "getPhoneCtx" + i;
                writeHandler(uriPath, recordToWrite);
                phoneCtxList = imsConfig.getMasterImsPhoneCtxValue();
                Assert.assertEquals(recordToWrite, phoneCtxList[i].getPhoneCtx());

                String [] phoneCtxIpuisList = phoneCtxList[i].getPhoneCtxIpuis();
                int leafLength = phoneCtxIpuisList.length;
                for (int j = 0; j < leafLength; ++j) {
                    uriPath = PARENT_URI + SUB_NODE[i] + File.separator + LEAF_NODE[j + 1];
                    recordToWrite = "PhoneCtx" + i + "id" + j;
                    writeHandler(uriPath, recordToWrite);
                    phoneCtxList = imsConfig.getMasterImsPhoneCtxValue();
                    Assert.assertEquals(recordToWrite, phoneCtxIpuisList[j]);
                }
            }
        } catch (ImsException e) {
            e.printStackTrace();
        }
    }

    private void writeHandler(String uriPath, String recordToWrite) {
        DmPhoneCtxNodeIoHandler handler = new DmPhoneCtxNodeIoHandler(mContext, Uri.parse(uriPath));
        Log.d(TAG, "[writeHandler]" + uriPath);

        byte[] testbytes = recordToWrite.getBytes();
        int testLen = testbytes.length;
        try {
            handler.write(0, testbytes, testLen);
        } catch (VdmException ve) {
            fail("Vdm exception.");
        }
    }

}
