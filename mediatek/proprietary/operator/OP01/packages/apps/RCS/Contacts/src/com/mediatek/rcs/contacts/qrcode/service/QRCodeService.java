/*
* This software/firmware and related documentation ("MediaTek Software") are
* protected under relevant copyright laws. The information contained herein
* is confidential and proprietary to MediaTek Inc. and/or its licensors.
* Without the prior written permission of MediaTek inc. and/or its licensors,
* any reproduction, modification, use or disclosure of MediaTek Software,
* and information contained herein, in whole or in part, shall be strictly prohibited.
*/
/* MediaTek Inc. (C) 2014. All rights reserved.
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

package com.mediatek.rcs.contacts.qrcode.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.android.okhttp.Headers;

import com.mediatek.xcap.client.XcapClient;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.message.BasicHeader;

import org.gsma.joyn.JoynServiceConfiguration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Get QRCode from server to Capture activity.
 */
public class QRCodeService {

    private static final String TAG = "QRCodeService";

    private static final String AUTH_XCAP_3GPP_INTENDED = "X-3GPP-Intended-Identity";
    private static final String HOST_NAME = "Host";
    private static final String USER_AGENT = "User-Agent";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_LENGTH = "Content-Length";
    private static final String SIM_CHANGE = "com.mediatek.rcs.contacts.INTENT_RCS_LOGIN";

    private static QRCodeService sInstance;
    private static String sHostName;
    private Context mContext;
    private String mIntendedId;
    private String mPhoneNumber;

    private String mUserName = "sip:user@anritsu-cscf.com";
    private String mPassword = "password";
    private Credentials mCredential = null;

    private QRCodeService(Context ctx) {
        mContext = ctx;
        String sip = getSip();
        if (sip == null || sip.length() <= 0) {
            Log.d(TAG, "no sip, RCS is not registered");
        } else {
            //mXIntendedId = getXIntentIdFromSip(sip);
            mPhoneNumber = getPhoneNumberFromSip(sip);
            if (mPhoneNumber != null) {
                mIntendedId = "tel:" + mPhoneNumber;
            }
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SIM_CHANGE);
        mContext.registerReceiver(mSIMChangedBroadcastReceiver, intentFilter);
    }

    /**
     * Get single instance.
     * @param ctx Context
     * @return QRCodeService
     */
    public static QRCodeService getInstance(Context ctx) {

        if (sInstance == null) {
            Log.d(TAG, "new Instance");
            sInstance = new QRCodeService(ctx);
        }
        return sInstance;
    }

    /**
     * Get QRCode from server to Capture activity.
     * @param url String
     * @return VCard raw string
     * @throws QRCodeException Exception
     */
    public String getContactQRCode(String url) throws QRCodeException {
        Log.d(TAG, "getContactQRCode");

        if (mIntendedId == null || mIntendedId.length() <= 0) {
            String sip = getSip();
            if (sip == null || sip.length() <= 0) {
                Log.d(TAG, "no sip, RCS is not registered");
                throw new QRCodeException(400); //no IntentedId means must be "400 ArgsError"
            } else {
                //mXIntendedId = getXIntentIdFromSip(sip);
                mPhoneNumber = getPhoneNumberFromSip(sip);
                if (mPhoneNumber != null) {
                    mIntendedId = "tel:" + mPhoneNumber;
                } else {
                    throw new QRCodeException(400); //no IntentedId means must be "400 ArgsError"
                }
            }
        }
        if (sHostName == null || sHostName.length() == 0) {
            sHostName = getHostNameFromUrl(url);
        }

        XcapClient xcapClient = new XcapClient();
        String ret = null;
        HttpURLConnection conn = null;
        Headers.Builder headers = new Headers.Builder();

        headers.add(AUTH_XCAP_3GPP_INTENDED, "\"" + mIntendedId + "\"");
        headers.add(HOST_NAME, sHostName);
        headers.add(USER_AGENT, "XDM-client/OMA1.0");
        headers.add(CONTENT_TYPE, "application/vnd.oma.cab-pcc+xml");

        if (mCredential == null) {
            Log.d(TAG, "mCredentials is null");
        }
        try {
            mCredential = new UsernamePasswordCredentials(mUserName, mPassword);
            //xcapClient.setAuthenticationCredentials(mCredential);
            conn = xcapClient.get(new URI(url), headers.build());
            if (conn != null) {
                if (conn.getResponseCode() == 200) {
                    InputStream is = null;
                    try {
                        int contentLen = conn.getContentLength();
                        if (contentLen == -1) {
                            Log.d(TAG, "contentLen is -1");
                            ret = null;
                        } else {
                            is = conn.getInputStream();
                            // convert stream to string
                            ret = convertStreamToString(is);
                        }
                    } finally {
                        if (is != null) {
                            is.close();
                        }
                    }
                } else {
                    ret = null;
                    throw new QRCodeException(conn.getResponseCode());
                }
            } else {
                Log.d(TAG, "conn is null");
                throw new QRCodeException(new ConnectException());
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "catch IOException on http request");
            e.printStackTrace();
            throw new QRCodeException(e);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } finally {
            xcapClient.shutdown();
        }
        return ret;
    }

    private String convertStreamToString(InputStream inputStream) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder total = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            total.append(line);
            total.append('\n');
        }
        return total.toString();
    }

    private String getSip() {
        JoynServiceConfiguration config = new JoynServiceConfiguration();
        String sip = config.getPublicUri(mContext);
        Log.d(TAG, "getSip, sip is: " + sip);
        return sip;
    }

    private String getPhoneNumberFromSip(String sip) {
        Log.d(TAG, "getPhoneNumberFromSip sip: " + sip);
        int startIdx;
        String telNum;
        int endIdx;
        if (sip.startsWith("sip:")) {
            startIdx = sip.indexOf(":") + 1;
            endIdx = sip.indexOf("@");
            telNum = sip.substring(startIdx, endIdx);
            Log.d(TAG, "subStr is: " + telNum);
            return telNum;
        } else if (sip.startsWith("tel:")) {
            startIdx = sip.indexOf(":") + 1;
            telNum = sip.substring(startIdx);
            Log.d(TAG, "subStr is: " + telNum);
            return telNum;
        } else {
            Log.d(TAG, "error, sip format is unexpected");
        }
        return null;
    }

    /*get Host name, actually, it is the server ip address*/
    private String getHostNameFromUrl(String url) {
        Log.d(TAG, "getHostNameFromUrl url: " + url);
        String prefixHttp = "http://";
        String tmpStr = null;
        String ipAddress = null;
        int endIdx = 0;
        if (url.startsWith(prefixHttp)) {
            //remove "http://" first
            tmpStr = url.substring(prefixHttp.length());
            endIdx = tmpStr.indexOf(":");
            ipAddress = tmpStr.substring(0, endIdx);
            Log.d(TAG, "ipAddress is: " + ipAddress);
        } else {
            Log.d(TAG, "error format");
        }
        return ipAddress;
    }

    private final BroadcastReceiver mSIMChangedBroadcastReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(SIM_CHANGE)) {
                Log.d(TAG, "onReceive");
                synchronized (mIntendedId) {
                    mIntendedId = null; //sim changed, clear mIntendedId
                }
            }
        }
    };
}

