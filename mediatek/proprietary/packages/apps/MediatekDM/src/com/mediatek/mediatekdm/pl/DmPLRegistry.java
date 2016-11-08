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

package com.mediatek.mediatekdm.pl;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.PLRegistry;

import java.util.Map;

public class DmPLRegistry implements PLRegistry {

    public DmPLRegistry(Context context) {
        mContext = context;
        mPreferences = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public Integer getIntValue(String key) {
        Integer value = 0;
        value = mPreferences.getInt(key, value);
        Log.d(TAG.PL, "getIntValue(" + key + ") returns " + value);
        return value;
    }

    public String getStringValue(String key) {
        String value = null;
        value = mPreferences.getString(key, value);
        Log.d(TAG.PL, "getStringValue(" + key + ") returns " + value);
        return value;
    }

    public void setIntValue(String key, int value) throws MdmException {
        Log.d(TAG.PL, "setIntValue(" + key + ", " + value + ")");
        mPreferences.edit().putInt(key, value).commit();
    }

    public void setStringValue(String key, String value) throws MdmException {
        Log.d(TAG.PL, "setStringValue(" + key + ", " + value + ")");
        SharedPreferences.Editor plEditor = mPreferences.edit();
        plEditor.putString(key, value).commit();
    }

    public void deleteKeysByPrefix(String prefix) throws MdmException {
        SharedPreferences.Editor editor = mPreferences.edit();
        Log.i(TAG.PL, "Removing all keys with prefix: " + prefix);
        Map<String, ?> map = mPreferences.getAll();
        for (String key : map.keySet()) {
            Log.i(TAG.PL, "found key: " + key);
            if (key.startsWith(prefix)) {
                editor.remove(key);
            }
        }
        editor.commit();
    }

    private Context mContext;
    private SharedPreferences mPreferences;
    private static final String PREF_NAME = "dm.reg";
}
