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

package com.mediatek.dm.data;

import android.content.Context;
import android.util.Log;

import com.mediatek.dm.DmConst.TAG;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class StateValues {
    public static final String DD_FIELD0 = "DD0_SIZE";
    public static final String DD_FIELD1 = "DD1_OBJECT_URI";
    public static final String DD_FIELD2 = "DD2_TYPE";
    public static final String DD_FIELD3 = "DD3_NAME";
    public static final String DD_FIELD4 = "DD4_VERSION";
    public static final String DD_FIELD5 = "DD5_VENDOR";
    public static final String DD_FIELD6 = "DD6_DESCRIPTION";
    public static final String DD_FIELD7 = "DD7_INSTALL_NOTIFY_URI";
    public static final String DD_FIELD8 = "DD8_NEXT_URL";
    public static final String DD_FIELD9 = "DD9_INFO_URL";
    public static final String DD_FIELD10 = "DD10_ICON_URI";
    public static final String DD_FIELD11 = "DD11_INSTALL_PARAM";
    public static final String DD_SIZE = "DD_PACK_SIZE";

    public static final String DM_STATE = "DM_STATE";
    public static final String ST_STATE = "ST_STATE";
    public static final String ST_DOWNLOADED_SIZE = "ST_DOWNLOADED_SIZE";
    public static final String ERROR_CODE = "ERROR_CODE";
    public static final String POSTPONE_TIMES = "POSTPONE_TIMES";
    public static final String UPDATE_RECOVERY = "UPDATE_RECOVERY";

    private final Context mContext;
    private final String mFileName;
    private Properties mProps;

    public StateValues(Context context, String fileName) {
        mContext = context;
        mFileName = fileName;
        mProps = new Properties();
    }

    public void put(String key, String value) {
        if (value != null) {
            mProps.setProperty(key, value);
        }
    }

    public String get(String key) {
        String value = mProps.getProperty(key);
        return value != null ? value : "";
    }

    public synchronized void load() {
        FileInputStream fis = null;
        try {
            fis = mContext.openFileInput(mFileName);
            mProps.load(fis);
        } catch (FileNotFoundException e) {
            Log.w(TAG.COMMON, "++dm_values not exist yet, file name is " + mFileName);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public synchronized void commit() {
        FileOutputStream fos = null;
        try {
            fos = mContext.openFileOutput(mFileName, Context.MODE_PRIVATE);
            mProps.store(fos, null);

            // force sync to disk
            fos.flush();
            fos.getFD().sync();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
