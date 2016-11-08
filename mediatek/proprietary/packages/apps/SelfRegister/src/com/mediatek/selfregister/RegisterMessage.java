/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2014. All rights reserved.
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

package com.mediatek.selfregister;

import android.os.Build;
import android.util.Log;

import com.mediatek.selfregister.utils.AgentProxy;
import com.mediatek.selfregister.utils.PlatformManager;
import com.mediatek.selfregister.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Responsible for compose the register message.
 */
public class RegisterMessage {
    private static final String TAG = Const.TAG_PREFIX + "RegisterMessage";

    /// M: Fields of register message.
    private static final String FIELD_REG_VERSION = "REGVER";
    private static final String FIELD_MEID = "MEID";
    private static final String FIELD_MODEL = "MODELSMS";
    private static final String FIELD_SW_VERSION = "SWVER";
    private static final String FIELD_UE_TYPE = "UETYPE";
    private static final String FIELD_MACID = "MACID";

    private static final String FIELD_OS_VERSION = "OSVER";
    private static final String FIELD_HW_VERSION = "HWVER";
    private static final String FIELD_IMEI_1 = "IMEI1";
    private static final String FIELD_IMEI_2 = "IMEI2";

    private static final String FIELD_SIM1_CDMA_IMSI = "SIM1CDMAIMSI";
    private static final String FIELD_SIM1_ICCID = "SIM1ICCID";
    private static final String FIELD_SIM1_LTE_IMSI = "SIM1LTEIMSI";
    private static final String FIELD_SIM1_TYPE = "SIM1TYPE";

    private static final String FIELD_SID = "SID";
    private static final String FIELD_NID = "NID";
    private static final String FIELD_BASEID = "BASEID";

    private static final String FIELD_SIM2_IMSI = "SIM2IMSI";

    // specified values
    private static final String OPERATING_SYSTEM = "android";

    private static final int LENGTH_MAX_MANUFACTURE = 3;
    private static final int LENGTH_MAX_MODEL = 20;
    private static final int LENGTH_MAX_VERSION = 60;
    private static final int VALUE_UE_TYPE = 1;
    private static final String VALUE_REG_VERSION = "1.0";

    private RegisterService mRegisterService;
    private PlatformManager mPlatformManager;

    /**
     * Constructor method.
     * @param service The service which invokes this class.
     */
    public RegisterMessage(RegisterService service) {
        mRegisterService = service;
        mPlatformManager = mRegisterService.getPlatformManager();
    }

    /**
     * Collect the data of register message.
     * @return String The register message.
     */
    public String getRegisterMessage() {
        String message = generateMessageData();
        Log.d(TAG, "Generate data: " + message + "");
        return Utils.encodeBase64(message);
    }

    private String generateMessageData() {
        JSONObject data = new JSONObject();
        try {
            data.put(FIELD_REG_VERSION, VALUE_REG_VERSION);
            data.put(FIELD_MEID, getMeid());
            data.put(FIELD_MODEL, getModel());
            data.put(FIELD_SW_VERSION, getSoftwareVersion());
            data.put(FIELD_UE_TYPE, VALUE_UE_TYPE);
            data.put(FIELD_MACID, getMacID());

            data.put(FIELD_OS_VERSION, getOSVersion());
            data.put(FIELD_HW_VERSION, getHardwareVersion());
            data.put(FIELD_IMEI_1, getImei(Const.SLOT_ID_0));

            if (mPlatformManager.isSingleLoad()) {
                data.put(FIELD_IMEI_2, Const.VALUE_EMPTY);
            } else {
                data.put(FIELD_IMEI_2, getImei(Const.SLOT_ID_1));
            }

            int slotId = Const.SLOT_ID_0;
            if (mPlatformManager.hasIccCard(slotId)) {
                data.put(FIELD_SIM1_CDMA_IMSI, getCdmaImsi(slotId));
                data.put(FIELD_SIM1_ICCID, getIccId(slotId));
                data.put(FIELD_SIM1_LTE_IMSI, getLteImsi(slotId));
                data.put(FIELD_SIM1_TYPE, getSimType(slotId));
            } else {
                data.put(FIELD_SIM1_CDMA_IMSI, Const.VALUE_EMPTY);
                data.put(FIELD_SIM1_ICCID, Const.VALUE_EMPTY);
                data.put(FIELD_SIM1_LTE_IMSI, Const.VALUE_EMPTY);
                data.put(FIELD_SIM1_TYPE, Const.VALUE_EMPTY);
            }

            appendLocationInfo(data);

            slotId = Const.SLOT_ID_1;
            if (mPlatformManager.hasIccCard(slotId)) {
                data.put(FIELD_SIM2_IMSI, getCdmaImsi(slotId));
            } else {
                data.put(FIELD_SIM2_IMSI, Const.VALUE_EMPTY);
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSONException " + e);
            e.printStackTrace();
        }

        return data.toString();
    }

    private void appendLocationInfo(JSONObject data) throws JSONException {
        int value = getCdmaSi();
        if (value != Const.VALUE_INVALID_INT) {
            data.put(FIELD_SID, value);
        } else {
            data.put(FIELD_SID, Const.VALUE_EMPTY);
        }

        value = getCdmaNi();
        if (value != Const.VALUE_INVALID_INT) {
            data.put(FIELD_NID, value);
        } else {
            data.put(FIELD_NID, Const.VALUE_EMPTY);
        }

        value = getCdmaBaseId();
        if (value != Const.VALUE_INVALID_INT) {
            data.put(FIELD_BASEID, value);
        } else {
            data.put(FIELD_BASEID, Const.VALUE_EMPTY);
        }
    }

    private String getIccId(int slotId) {
        return mRegisterService.getIccIdFromCard(slotId);
    }

    private String getCdmaImsi(int slotId) {
        return mRegisterService.getComplexImsi(slotId)[0];
    }

    private String getLteImsi(int slotId) {
        return mRegisterService.getComplexImsi(slotId)[1];
    }

    private int getSimType(int slotId) {
        return mRegisterService.getSimType(slotId);
    }

    private int getCdmaSi() {
        return mRegisterService.getCdmaSi();
    }

    private int getCdmaNi() {
        return mRegisterService.getCdmaNi();
    }

    private int getCdmaBaseId() {
        return mRegisterService.getCdmaBaseId();
    }

    private String getMeid() {
        return mRegisterService.getMeid();
    }

    private String getHardwareVersion() {
        return PlatformManager.getHardwareVersion();
    }

    private String getImei(int slotId) {
        return mPlatformManager.getImei(slotId);
    }

    private String getOSVersion() {
        return OPERATING_SYSTEM + Build.VERSION.RELEASE;
    }

    private String getMacID() {
        StringBuilder macAddress = new StringBuilder();

        byte[] macByte = AgentProxy.getInstance().getMacAddress();
        char[] macChar = Utils.bytesToHexString(macByte).toCharArray();

        for (int i = 0; i < macByte.length; i++) {
            macAddress.append(macChar[i * 2]);
            macAddress.append(macChar[i * 2 + 1]);
            if (i != macByte.length - 1) {
                String str = new String(":");
                macAddress.append(str);
            }
        }

        Log.v(TAG, "Mac address is " + macAddress.toString());
        return macAddress.toString();
    }

    private String getModel() {
        String manufacturer = PlatformManager.getManufactor();
        if (manufacturer.length() > LENGTH_MAX_MANUFACTURE) {
            Log.w(TAG, "Manufacturer length > " + LENGTH_MAX_MANUFACTURE + ", cut it!");
            manufacturer = manufacturer.substring(0, LENGTH_MAX_MANUFACTURE);
        }

        String model = Build.MODEL;
        Log.d(TAG, "model:" + model);
        model = model.replaceAll("-", " ");
        if (model.indexOf(manufacturer) != -1) {
            model = model.replaceFirst(manufacturer, "");
        }

        String result = manufacturer + "-" + model;
        if (result.length() > LENGTH_MAX_MODEL) {
            Log.w(TAG, "Model length > " + LENGTH_MAX_MODEL + ", cut it!");
            result = result.substring(0, LENGTH_MAX_MODEL);
        }

        return result;
    }

    private String getSoftwareVersion() {
        String result = PlatformManager.getSoftwareVersion();
        if (result.length() > LENGTH_MAX_VERSION) {
            Log.w(TAG, "Software version length > " + LENGTH_MAX_VERSION + ", cut it!");
            result = result.substring(0, LENGTH_MAX_VERSION);
        }

        return result;
    }
}
