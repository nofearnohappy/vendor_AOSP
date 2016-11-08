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

package com.mediatek.configurecheck2;


import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;

/**
 * Manager to enable/disable ENWINFO URC.
 */
public class NetworkInfoManager {

    private Context mContext;
    private final String mTag;

    private int mEmInfoEnum = -1;
    private Handler mUrcHandler;
    private int mUrcMsg;
    private int mSimType;
    private int mFlag = 0;
    private int mFlagLteDc = 0;
    private Phone mPhone;

    private static final int FLAG_DATA_BIT = 8;
    private static final int FLAG_OFFSET_BIT = 0x08;
    private static final int FLAG_OR_DATA = 0xFFFFFFF7;

    private static final int MSG_NW_INFO = 1;
//    private static final int MSG_NW_INFO_LTEDC = 2;
    private static final int MSG_NW_INFO_OPEN = 3;
    private static final int MSG_NW_INFO_CLOSE = 4;

    public NetworkInfoManager(Context c, String tag) {
        mContext = c;
        mTag = tag;
        mSimType = getSimType();
    }

    private Handler mATCmdHander = new Handler() {
        public void handleMessage(Message msg) {
            CTSCLog.d(mTag, "handleMessage what = " + msg);
            AsyncResult ar;
            ar = (AsyncResult) msg.obj;
            if (ar.exception != null) {
                CTSCLog.d(mTag, "send fail");
                return;
            }
            String[] data = null;
            String[] atCommand = null;
            switch (msg.what) {
            case MSG_NW_INFO:
                data = (String[]) ar.result;
                CTSCLog.d(mTag, "data[0] is : " + data[0]);
                CTSCLog.d(mTag, "flag is : " + data[0].substring(FLAG_DATA_BIT));
                mFlag = Integer.valueOf(data[0].substring(FLAG_DATA_BIT));
                mFlag = mFlag | FLAG_OFFSET_BIT;
                CTSCLog.d(mTag, "flag change is : " + mFlag);
                if (mEmInfoEnum < 0) {
                    throw new IllegalArgumentException("EM info enum invalid: " + mEmInfoEnum);
                }
                atCommand = new String[2];
                atCommand[0] = "AT+EINFO=" + mFlag + "," + mEmInfoEnum + ",0";
                atCommand[1] = "+EINFO";
                sendATCommand(atCommand, MSG_NW_INFO_OPEN); //enable mEmInfoEnum type network report
                break;
            case MSG_NW_INFO_OPEN:
            case MSG_NW_INFO_CLOSE:
            default:
                break;
            }
        }
    };

    /**
     * Enable the required type of URC to be send out from modem,
     * and you will get this URC info in urcHandler.
     * @param infoEnum type of URC that you required,
     * which is defined in em_info_enum of em_public_struct.h
     * @param urcHandler handler by which you can get the URC info.
     * @param urcMsg message of urcHandler.
     */
    public void registerNetwork(int infoEnum, Handler urcHandler, int urcMsg) {
        mEmInfoEnum = infoEnum;
        mUrcHandler = urcHandler;
        mUrcMsg = urcMsg;

        if (TelephonyManager.getDefault().getPhoneCount() > 1) {
            mPhone = PhoneFactory.getPhone(mSimType);
        } else {
            mPhone = PhoneFactory.getDefaultPhone();
        }
        if (mPhone != null) {
            mPhone.registerForNetworkInfo(mUrcHandler, mUrcMsg, null);
        } else {
            return ;
        }

        //query current flag of network report
        String[] atCommand = {"AT+EINFO?", "+EINFO"};
        sendATCommand(atCommand, MSG_NW_INFO);
    }

    /**
     * Disable the required type of URC to be send out from modem.
     */
    public void unregisterNetwork() {
        mPhone.unregisterForNetworkInfo(mUrcHandler);

        mFlag = mFlag & FLAG_OR_DATA; //restore the flag
        CTSCLog.d(mTag, "The close flag is :" + mFlag);
        String[] atCloseCmd = new String[2];
        atCloseCmd[0] = "AT+EINFO=" + mFlag + "," + mEmInfoEnum + ",1";
        atCloseCmd[1] = "";
        sendATCommand(atCloseCmd, MSG_NW_INFO_CLOSE);
    }

    private void sendATCommand(String[] atCommand, int msg) {
        CTSCLog.d(mTag, "sendATCommand :" + atCommand[0]);
        mPhone.invokeOemRilRequestStrings(atCommand, mATCmdHander.obtainMessage(msg));
    }

    private void showToast(int resId) {
        Toast.makeText(mContext, mContext.getString(resId), Toast.LENGTH_SHORT)
             .show();
    }

    private int getSimType() {
        return PhoneConstants.SIM_ID_1;
    }
}

class UrcParser {

    public static final int TYPE_UINT8 = 0;
    public static final int TYPE_UINT16 = 1;
    public static final int TYPE_UINT32 = 2;
    public static final int TYPE_INT8 = 3;
    public static final int TYPE_INT16 = 4;
    public static final int TYPE_INT32 = 5;
    //used to do alignment when field is a structure too
    public static final int TYPE_ALIGNMENT = 8;

    //1 byte is 8 bits which is 2 chars in HEX format: 0x01 -> "01"
    private static final int DATA_OFFSET_2 = 2;
    private static final int DATA_OFFSET_4 = 4;
    private static final int DATA_OFFSET_6 = 6;
    private static final int DATA_OFFSET_8 = 8;
    private static final int DATA_FORMAT = 16;

    private static final Boolean ALIGN_MENT_ENABLE = true;

    /**
     * get the start position of the field that you want in a URC structure.
     * @param frontFields fields array that in font of the field you want.
     * @return the start position of the field that you want.
     */
    public static int calculateOffset(UrcField[] frontFields) {
        int offset = 0;
        for (UrcField field : frontFields) {
            switch (field.type) {
            case TYPE_UINT8:
            case TYPE_INT8:
                offset += DATA_OFFSET_2 * field.count;
                break;
            case TYPE_UINT16:
            case TYPE_INT16:
                if (ALIGN_MENT_ENABLE) {
                    offset = (offset + 3) & ~3;
                }
                offset += DATA_OFFSET_4 * field.count;
                break;
            case TYPE_UINT32:
            case TYPE_INT32:
                if (ALIGN_MENT_ENABLE) {
                    offset = (offset + 7) & ~7;
                }
                offset += DATA_OFFSET_8 * field.count;
                break;
            case TYPE_ALIGNMENT:
                if (ALIGN_MENT_ENABLE) {
                    offset = (offset + 7) & ~7;
                }
            default:
                break;
            }
        }
        return offset;
    }

    public static int getValueFromByte(String data, int start, boolean signed) {
        if (data.length() < start + DATA_OFFSET_2) {
            return 0;
        }
        String sub = data.substring(start, start + DATA_OFFSET_2);
        if (signed) {
            short s = Short.valueOf(sub, DATA_FORMAT);
            byte b = (byte) s;
            return b;
        } else {
            return Short.valueOf(sub, DATA_FORMAT).shortValue();
        }
    }

    public static int getValueFrom2Byte(String data, int start, boolean signed) {
        if (data.length() < start + DATA_OFFSET_4) {
            return 0;
        }
        String low = data.substring(start, start + DATA_OFFSET_2);
        String high = data.substring(start + DATA_OFFSET_2, start
                + DATA_OFFSET_4);
        String reverse = high + low;
        if (signed) {
            int i = Integer.valueOf(reverse, DATA_FORMAT);
            Short s = (short) i;
            return s;
        } else {
            return Integer.valueOf(reverse, DATA_FORMAT).intValue();
        }
    }

    public static long getValueFrom4Byte(String data, int start, boolean signed) {
        if (data.length() < start + DATA_OFFSET_8) {
            return 0;
        }
        String byte1 = data.substring(start, start + DATA_OFFSET_2);
        String byte2 = data.substring(start + DATA_OFFSET_2, start + DATA_OFFSET_4);
        String byte3 = data.substring(start + DATA_OFFSET_4, start + DATA_OFFSET_6);
        String byte4 = data.substring(start + DATA_OFFSET_6, start + DATA_OFFSET_8);
        String reverse = byte4 + byte3 + byte2 + byte1;
        if (signed) {
            long lg = Long.valueOf(reverse, DATA_FORMAT);
            Integer i = (int) lg;
            return i;
        } else {
            return Long.valueOf(reverse, DATA_FORMAT).longValue();
        }
    }
}

class UrcField {
    int type;    //field type
    String name; //field name. Just for easy reading.
    int count;   //count of this type

    public UrcField(int type, String name, int count) {
        this.type = type;
        this.name = name;
        this.count = count;
    }
}

