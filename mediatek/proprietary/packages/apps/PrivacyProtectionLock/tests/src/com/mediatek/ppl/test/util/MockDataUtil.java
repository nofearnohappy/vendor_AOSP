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

package com.mediatek.ppl.test.util;

import android.content.Context;
import android.os.RemoteException;

import com.mediatek.ppl.ControlData;
import com.mediatek.internal.telephony.ppl.IPplAgent;
import com.mediatek.ppl.PplApplication;

import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;

public class MockDataUtil {
    private static final String TAG = "PPL/MockDataUtil";

    /**
     * Build a sample sim finger print list
     * */
    public static byte[] buildSimFinger() {
        byte[] finger = new byte[ControlData.SIM_FINGERPRINT_LENGTH];
        for (int i = 0; i < finger.length; ++i) {
            finger[i] = '1';
        }
        return finger;
    }

    /**
     * Generate new secrets, including new password and new salt.
     *
     * @param password
     * @param salt
     * @return
     */
    private static byte[] buildSecrets(final byte[] password, byte[] salt) {

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[salt.length + password.length];
            System.arraycopy(password, 0, buffer, 0, password.length);
            System.arraycopy(salt, 0, buffer, password.length, salt.length);
            return md.digest(buffer);
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e);
        }
    }

    /**
     * Build a sample control data for testing
     * */
    public static ControlData buildSampleControlData() {
        ControlData controlData = new ControlData();
        controlData.setEnable(true);
        controlData.setProvision(true);

        // Not generate a random salt each time
        controlData.salt = new byte[ControlData.SALT_SIZE];
        for (int i = 0; i < controlData.salt.length; ++i) {
            controlData.salt[i] = '3';
        }
        controlData.secret = buildSecrets(MockPplUtil.PASSWORD_ORIGINAL.getBytes(), controlData.salt);

        controlData.SimFingerPrintList = new LinkedList<byte[]>();
        controlData.SimFingerPrintList.add(buildSimFinger());

        controlData.TrustedNumberList = new LinkedList<String>();
        controlData.TrustedNumberList.add(MockPplUtil.SERVICE_NUMBER_1st);
        return controlData;
    }

    /**
     * Check whether the password is correct.
     *
     * SHA1(password:salt) == secret
     */
    public static boolean checkPassword(final byte[] password, final byte[] salt, final byte[] secret) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[salt.length + password.length];
            System.arraycopy(password, 0, buffer, 0, password.length);
            System.arraycopy(salt, 0, buffer, password.length, salt.length);
            byte[] digest = md.digest(buffer);
            if (secret.length != digest.length) {
                return false;
            }
            for (int i = 0; i < secret.length; ++i) {
                if (secret[i] != digest[i]) {
                    return false;
                }
            }
            return true;
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e);
        }
    }

    /**
     * Load control data from file
     * */
    public static void cleanControlData() {
        writeControlData(new ControlData());
    }

    /**
     * Load control data from file
     * */
    public static ControlData loadControlData() {
        IPplAgent agent = PplApplication.getPlatformManager().getPPLAgent();
        ControlData controlData;
        try {
            controlData = ControlData.buildControlData(agent.readControlData());
        } catch (RemoteException e) {
            throw new Error(e);
        }
        return controlData;
    }

    /**
     * Write a sample control data for testing
     * */
    public static void writeSampleControlData() {
        writeControlData(buildSampleControlData());
    }

    /**
     * Write control data from file
     * */
    public static void writeControlData(ControlData config) {
        IPplAgent agent = PplApplication.getPlatformManager().getPPLAgent();
        try {
            agent.writeControlData(config.encode());
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    /**
     * Prepare a mock platformManager
     * */
    public static void preparePlatformManager(PplApplication application) {
        preparePlatformManager(application, application);
    }

    /**
     * Prepare a mock platformManager, init by certain context
     * */
    public static void preparePlatformManager(PplApplication application, Context context) {
        try {

            Field field = PplApplication.class.getDeclaredField("sPlatformManager");
            field.setAccessible(true);
            field.set(application,
                    new MockPlatformManager(context));

            MockPplUtil.formatLog(TAG, "preparePlatformManager");
        } catch (NoSuchFieldException e) {
            throw new Error(e);
        } catch (IllegalArgumentException e) {
            throw new Error(e);
        } catch (IllegalAccessException e) {
            throw new Error(e);
        }
    }
}
