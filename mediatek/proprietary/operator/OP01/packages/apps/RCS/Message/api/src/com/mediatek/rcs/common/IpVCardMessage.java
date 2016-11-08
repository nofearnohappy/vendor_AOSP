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

package com.mediatek.rcs.common;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import com.android.vcard.VCardEntryHandler;
import com.android.vcard.VCardEntry;
import com.mediatek.rcs.common.service.FileStruct;
import com.mediatek.rcs.common.utils.ContextCacher;
import com.mediatek.rcs.common.utils.RcsVcardData;
import com.mediatek.rcs.common.utils.RcsVcardUtils;
import com.mediatek.rcs.common.utils.RcsVcardParserResult;

import java.io.File;

public class IpVCardMessage extends IpAttachMessage {

    private String mName = "";
    private String mMobilePhone = "";
    private String mHomePhone;
    private String mAddress;
    private String mEmail = "";
    private Bitmap mPortrait;
    private int mEntryCount = 0;
    private static final int SIZE_ONE = 1;
    private Uri mUri = null;
    //private static final int SIZE_K = 1024;

    private static final String TAG = "IpVCardMessage";

    myVcardEntryHandler mVcardEntryHandler = null;
    public IpVCardMessage(FileStruct fileStruct, String remote) {
        super();
        Log.d(TAG, "PluginIpVcardMessage(), fileStruct = " + fileStruct + " remote = " + remote);
        setSimId(-1);
        int size = (int) fileStruct.mSize;
        if (size == 0) {
            size = SIZE_ONE;
        }
        setSize(size);
        setPath(fileStruct.mFilePath);
        String name = fileStruct.mName.substring(0, (fileStruct.mName).lastIndexOf("."));
        setName(name);
        setType(IpMessageConsts.IpMessageType.VCARD);
        setFrom(remote);
        setTo(remote);
        setTag(fileStruct.mFileTransferTag);
        //setUri(fileStruct.mFilePath);
        //set entry count
        int entryCount = RcsVcardUtils.getVcardEntryCount(fileStruct.mFilePath);
        setEntryCount(entryCount);
        //set number and photo
        mVcardEntryHandler = new myVcardEntryHandler();
        RcsVcardUtils.parseVcard(fileStruct.mFilePath, mVcardEntryHandler);
    }

    private class myVcardEntryHandler implements VCardEntryHandler {
        public void onEntryCreated(final VCardEntry entry) {
            Log.d(TAG, "onEntryCreated,  enter");
            RcsVcardParserResult result = RcsVcardUtils.ParseRcsVcardEntry(
                    entry, ContextCacher.getPluginContext());
            String name = null;
            String number = null;
            String email = null;

            if (result != null) {
                name = result.getName();
                if (result.getNumber() != null && result.getNumber().size() != 0) {
                    number = result.getNumber().get(0).toString();
                } else if (result.getEmail() != null && result.getEmail().size() != 0) {
                    email = result.getEmail().get(0).toString();
                }
            }

            //set email
            if (email != null) {
                Log.d(TAG, "set email, email = " + email);
                setEmail(email);
            }
            //set phone number
            if (number != null) {
                Log.d(TAG, "set number, number = " + number);
                setMobilePhone(number);
            }
            //set name
            if (name != null && getEntryCount() == 1) {
                Log.d(TAG, "set name, name = " + name);
                setName(name);
            }
            //set photo
            Bitmap bitmap = null;
            byte[] pic = result.getPhoto();
            if (pic != null) {
                bitmap = BitmapFactory.decodeByteArray(pic, 0, pic.length);
            }
            setPortrait(bitmap);
        }

        public void onEnd() {

        }

        public void onStart() {

        }
    }
    public void setUri(String filePath) {
        Log.d(TAG, "setUri(), filePath = " + filePath);
        //mUri = Uri.parse("file:///" + filePath);
        mUri = Uri.fromFile(new File(filePath));
    }

    public Uri getUri() {
        return mUri;
    }

    public IpVCardMessage() {
        super();
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getMobilePhone() {
        return mMobilePhone;
    }

    public void setMobilePhone(String mobilePhone) {
        mMobilePhone = mobilePhone;
    }

    public String getHomePhone() {
        return mHomePhone;
    }

    public void setHomePhone(String homePhone) {
        mHomePhone = homePhone;
    }

    public String getAddress() {
        return mAddress;
    }

    public void setAddress(String address) {
        mAddress = address;
    }

    public String getEmail() {
        return mEmail;
    }

    public void setEmail(String email) {
        mEmail = email;
    }
    public void setPortrait(Bitmap bitmap) {
        mPortrait = bitmap;
}

    public Bitmap getPortrait() {
        return mPortrait;
    }

    public void setEntryCount(int entryCount) {
        mEntryCount = entryCount;
    }

    public int getEntryCount() {
        return mEntryCount;
    }
}
