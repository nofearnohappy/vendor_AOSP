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

package com.mediatek.dm.bootstrap;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.EditText;

import com.mediatek.dm.DmConst;
import com.mediatek.dm.DmService;
import com.mediatek.dm.R;
import com.redbend.vdm.BootMsgHandler;
import com.redbend.vdm.SessionInitiator;
import com.redbend.vdm.VdmException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Class support for Bootstrap session. Have not implement the function yet.
 *
 */
public class DmBootstrapHandler implements BootMsgHandler, SessionInitiator {

    private static final String TAG = "DM/Bootstrap";
    private static final String MEID_TO_PESN_HASH_NAME = "SHA-1";
    private static final String PESN_PREFIX = "80";
    private static final String HTTP_PREFIX = "http://";
    private static final String HTTPS_PREFIX = "https://";
    public static final String INITIATOR_DM_BOOTSTRAP = DmConst.SessionInitiatorId.INITIATOR_DM_BOOTSTRAP;
    public static final String INITIATOR_CP_BOOTSTRAP = DmConst.SessionInitiatorId.INITIATOR_CP_BOOTSTRAP;
    private EditText mEditText;
    private TelephonyManager mTelephonyManager;
    private String mInitiator;

    // private List<SimInfoRecord> mSiminfoList = new ArrayList<SimInfoRecord>();

    public DmBootstrapHandler(String initiator) {
        mInitiator = initiator;
    }

    /**
     * Returns "DmBootstrap".
     */
    public String getId() {
        return mInitiator;
    }

    /**
     * @see BootMsgHandler#getAddrType getAddrType
     *
     *      If the address looks like an HTTP URL, assume address type HTTP. Otherwise give up.
     */
    public int getAddrType(String addr) throws VdmException {

        int result;

        if (addr.startsWith(HTTPS_PREFIX) || addr.startsWith(HTTP_PREFIX)) {
            result = BootMsgHandler.ADDR_TYPE_HTTP;
        } else {
            throw new VdmException("invalid Address Type: " + addr);
        }

        return result;
    }

    /**
     * @see BootMsgHandler#getPin getPin
     */
    public void getPin() throws VdmException {
        Log.d(TAG, "get PIN begin");

        LayoutInflater factory = LayoutInflater.from(DmService.getInstance());
        mEditText = (EditText) factory.inflate(R.layout.alert_dialog_text_entry, null);

        AlertDialog dialog = new AlertDialog.Builder(DmService.getInstance())
                .setTitle(R.string.alert_dialog_bootstrap_title)
                .setMessage(R.string.alert_dialog_bootstrap_message).setView(mEditText)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Log.d(TAG, "get PIN : NegativeButton");
                        DmService.getInstance().notifyUserPinSet(null, false);
                    }
                }).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Log.d(TAG, "get PIN : PositiveButton");

                        Editable editable = mEditText.getText();
                        int etLength = editable.length();
                        char input[] = new char[etLength];
                        editable.getChars(0, etLength, input, 0);

                        String pinCode = new String(input);
                        Log.i(TAG, " Entered PIN Code: " + pinCode);

                        DmService.getInstance().notifyUserPinSet(pinCode, true);

                    }
                }).create();

        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();
    }

    /**
     * @see BootMsgHandler#getNss getNss
     *
     *      Use hex decoding.
     */
    public int getNss(byte[] buffer) throws VdmException {

        Log.i(TAG, "getNss begin");

        String networkId = getNetworkId();

        if (TextUtils.isEmpty(networkId)) {
            return -1;
        } else {
            byte[] nss = networkId.getBytes();
            return hexDecode(nss, buffer);
        }
    }

    private String getNetworkId() {
        String networkId = null;
        mTelephonyManager = TelephonyManager.getDefault();
        if (mTelephonyManager == null) {
            Log.e(TAG, "mTelephonyManager is null!");
            return null;
        }

        int subId = SubscriptionManager.getDefaultSubId();
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            Log.e(TAG, "subId is invalid!");
            return null;
        }

        if (mTelephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) {
            networkId = mTelephonyManager.getSubscriberId(subId);
            Log.i(TAG, "getNetworkId, type is GSM, id is imsi = " + networkId);
        } else if (mTelephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA) {
            //TODO CDMA networkID = SSD witch is calculated by CAVE, with parameters RAND ESN(UIM ID), IMSI and SSD-A
            String meid = getDeviceId(SubscriptionManager.getSlotId(subId));
            networkId = meidToESN(meid);
            Log.i(TAG, "getNetworkId, type is CDMA, id is esn = " + networkId);
        } else {
            //TODO TDMA networkId = SSD-S
        }
        return networkId;
    }

    private String meidToESN(String meid) {
        if (meid == null || meid.length() == 0) {
            return null;
        }
        byte[] meidByte = hexStringToBytes(meid);
        MessageDigest md;
        String pESN = null;
        try {
            md = MessageDigest.getInstance(MEID_TO_PESN_HASH_NAME);
            md.update(meidByte);
            String result = bytesToHexString(md.digest());
            int length = result.length();
            if (length > 6) {
                pESN = PESN_PREFIX + result.substring(length - 6, length);
            } else {
                Log.e(TAG, "digest result length < 6, it is not valid:" + result);
            }
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "No such algorithm:" + MEID_TO_PESN_HASH_NAME);
            e.printStackTrace();
        }
        if (pESN != null) {
            pESN = pESN.toUpperCase();
        }
        return pESN;
    }

    private byte[] hexStringToBytes(String hexString) {
        if (TextUtils.isEmpty(hexString)) {
            return null;
        }

        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    private byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    private String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    private String getDeviceId(int slot) {
        String meid = null;
        if (mTelephonyManager != null) {
            meid = mTelephonyManager.getDeviceId(slot);
        }

        if (meid != null) {
            meid = meid.toUpperCase();
        }

        return meid;
    }

    private int hexDecode(byte[] hex, byte[] bin) {
        int len = hex.length;
        int lenKey = len / 2 + 1;
        if (bin.length < lenKey) {
            return -1;
        }
        boolean even = len % 2 != 0;

        for (int i = 0; i < lenKey; i++) {
            if (i == 0) {
                bin[0] = (byte) (0x00 + (hex[0] - '0') * 16 + 9);
            } else if (i == (lenKey - 1) && !even) {
                bin[i] = (byte) (0x00 + 0xF0 + (hex[len - 1] - '0'));
            } else {
                bin[i] = (byte) (0x00 + (hex[i * 2] - '0') * 16 + (hex[i * 2 - 1] - '0'));
            }
        }

        return lenKey;
    }

}
