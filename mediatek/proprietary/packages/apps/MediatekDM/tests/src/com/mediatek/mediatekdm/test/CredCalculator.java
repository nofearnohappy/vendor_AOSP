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

package com.mediatek.mediatekdm.test;

import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CredCalculator {
    protected byte mNonce;
    protected final String mUserName;
    protected final String mPassword;

    public CredCalculator(byte firstNonce, String username, String password) {
        if (firstNonce < 1 || firstNonce > 8) {

        }
        mNonce = firstNonce;
        mUserName = username;
        mPassword = password;
    }

    protected byte nextNonce() {
        return (byte) ((mNonce & 7) + 1);
    }

    public byte updateNonce() {
        mNonce = nextNonce();
        return mNonce;
    }

    public String getNonce() {
        byte[] data = new byte[] { mNonce };
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }

    public String calculateCred() {
        MessageDigest digest = null;
        String result = null;
        try {
            digest = MessageDigest.getInstance("MD5");
            byte[] identity = (mUserName + ":" + mPassword).getBytes("UTF-8");
            byte[] md5 = digest.digest(identity);
            byte[] enc = Base64.encode(md5, Base64.NO_WRAP);
            byte[] buf = new byte[enc.length + 1 + 1];
            System.arraycopy(enc, 0, buf, 0, enc.length);
            buf[enc.length] = (byte) ':';
            buf[enc.length + 1] = mNonce;
            md5 = digest.digest(buf);
            result = Base64.encodeToString(md5, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e);
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
        return result;
    }
}
