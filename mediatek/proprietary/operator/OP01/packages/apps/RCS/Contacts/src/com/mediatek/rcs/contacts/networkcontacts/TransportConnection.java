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

package com.mediatek.rcs.contacts.networkcontacts;

import android.util.Log;
import com.mediatek.gba.GbaHttpUrlCredential;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


/**
 * Transport layer connection.
 *
 */
public class TransportConnection {
    private static final String TAG = "NETWORKCONTACTS:TransportConnection";

    /**
     * Certification Trust manager.
     */
    class CertTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] arg0, String arg1) {
            Log.d(TAG, "[checkClientTrusted] " + arg0 + arg1);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] arg0, String arg1)
                throws CertificateException {
            Log.d(TAG, "[checkServerTrusted] X509Certificate amount:"
                    + arg0.length + ", cryptography: " + arg1);
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            Log.d(TAG, "[getAcceptedIssuers] ");
            return null;
        }
    }

    private HttpURLConnection mConnection;
    private Proxy.Type mProxyType = null;
    private URL mUrl = null;
    private TrustManager[] mTrustManagerArray =
            new TrustManager[] {new CertTrustManager()};

    /**
     * constructor.
     */
    public TransportConnection() {
    }

    /**
     * @param field property field.
     * @param value propert value.
     * @return result
     */
    public boolean addRequestProperty(String field, String value) {
        Log.d(TAG, "addRequestProperty: " + field + " = " + value);

        if (mConnection == null) {
            Log.e(TAG, "AddRequestProperty: mConnection=" + mConnection);
            return false;
        }

        try {
            mConnection.setRequestProperty(field, value);
            return true;
        } catch (IllegalStateException e) {
            Log.e(TAG, "AddRequestProperty: IllegalStateException");
        } catch (NullPointerException e) {
            Log.e(TAG, "AddRequestProperty: NullPointerException");
        }

        return false;
    }

    /**
     * @return result
     */
    public boolean closeComm() {
        Log.d(TAG, "closeComm()");
        if (mConnection == null) {
            Log.e(TAG, "closeComm: mConnection=" + mConnection);
            return false;
        }

        mConnection.disconnect();
        return true;
    }

    /**
     * destroy connection.
     */
    public void destroy() {
        // nothing to do
    }

    /**
     * @return content length.
     */
    public int getContentLength() {
        Log.d(TAG, "getContentLength()");

        if (mConnection == null) {
            Log.e(TAG, "getContentLength: mConnection=" + mConnection);
            return -1;
        }

        if (!waitResponse()) {
            Log.e(TAG, "getHeadField: timeout");
            return -1;
        }

        int length = mConnection.getContentLength();
        if (length < 0) {
            try {
                /* for chunked ?? */
                length = mConnection.getInputStream().available();
            } catch (IOException e) {
                Log.e(TAG, "in.available: IOException " + e.getMessage());
                e.printStackTrace();
            }
        }

        Log.d(TAG, "getContentLength() return " + length);

        return length;
    }

    /**
     * @param field field name.
     * @return value of field.
     */
    public String getHeadField(String field) {
        Log.d(TAG, "getHeadField: field=" + field);

        if (mConnection == null) {
            Log.e(TAG, "getHeadField: mConnection=" + mConnection);
            return null;
        }

        if (!waitResponse()) {
            Log.e(TAG, "getHeadField: timeout");
            return null;
        }

        return mConnection.getHeaderField(field);

    }

    /**
     * @param field field name.
     * @param defValue default value.
     * @return field value.
     */
    public int getHeadFieldInt(String field, int defValue) {
        Log.d(TAG, "getHeadFieldInt: field=" + field + " ,defValue=" + defValue);
        if (mConnection == null) {
            Log.e(TAG, "getHeadFieldInt: mConnection=" + mConnection);
            return defValue;
        }

        if (!waitResponse()) {
            Log.e(TAG, "getHeadField: timeout");
            return defValue;
        }

        return mConnection.getHeaderFieldInt(field, defValue);

    }

    /**
     * @return URL of connection.
     */
    public String getURL() {
        if (mConnection == null) {
            Log.e(TAG, "getURL: mConnection=" + mConnection);
            return null;
        }

        return mConnection.getURL().toString();
    }

    /**
     * @param uri url
     * @param proxyType
     *            : 0 -- DIRECT, 1 -- PROXY(HTTP??), 2 --SOCKS
     * @param proxyAddr proxy address
     * @param proxyPort proxy port.
     * @return result.
     */
    public boolean initialize(String uri, int proxyType, String proxyAddr,
            int proxyPort) {
        Log.d(TAG, "initialize: uri=" + uri + ", proxyType=" + proxyType
                + ", proxyAddr=" + proxyAddr + ", proxyPort=" + proxyPort);
        try {
            mUrl = new URL(uri);
            Log.d(TAG, "Host is " + mUrl.getHost());
            Log.d(TAG, "Port is " + mUrl.getPort());
        } catch (MalformedURLException e) {
            Log.e(TAG, "SimpleHttpConnection: invalid URL: " + uri);
            return false;
        }

        switch (proxyType) {
        case 0:
            mProxyType = Proxy.Type.DIRECT;
            break;
        case 1:
            mProxyType = Proxy.Type.HTTP;
            break;
        case 2:
            mProxyType = Proxy.Type.SOCKS;
            break;
        default:
            return false;
        }

        ContactsSyncEngine engine = ContactsSyncEngine.getInstance(null);
        if (engine == null) {
            Log.e(TAG, "openComm: no engine instance");
            return false;
        }

        GbaHttpUrlCredential credential = new GbaHttpUrlCredential(engine.getContext(), uri);
        Authenticator.setDefault(credential.getAuthenticator());

        try {
            if (mProxyType == Proxy.Type.DIRECT) {
                mConnection = (HttpURLConnection) mUrl.openConnection();
            } else {
                InetSocketAddress addr = new InetSocketAddress(proxyAddr,
                        proxyPort);
                mConnection = (HttpURLConnection) mUrl
                        .openConnection(new Proxy(mProxyType, addr));
            }
            return true;
        } catch (IOException e) {
            Log.d(TAG, "SimpleHttpConnection: IOException");
            e.printStackTrace();
            return false;
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "SimpleHttpConnection: IllegalArgumentException");
            return false;
        } catch (UnsupportedOperationException e) {
            Log.d(TAG, "SimpleHttpConnection: UnsupportedOperationException");
            return false;
        }
    }

    /**
     * @return open result.
     */
    public boolean openComm() {
        Log.d(TAG, "openComm()");
        if (mConnection == null) {
            Log.e(TAG, "openComm: mConnection=" + mConnection);
            return false;
        }

        if (mConnection instanceof HttpsURLConnection) {
            Log.d(TAG, "openComm(): https connection");
            HttpsURLConnection connection = (HttpsURLConnection) mConnection;
            try {
                HostnameVerifier hv = new HostnameVerifier() {
                    @Override
                    public boolean verify(String urlHostName, SSLSession session) {
                        Log.d(TAG, "verify:" + urlHostName);
                        return true;
                    }
                };

                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, mTrustManagerArray, null);
                SSLSocketFactory sslf = sc.getSocketFactory();
                connection.setHostnameVerifier(hv);
                connection.setSSLSocketFactory(sslf);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "openComm(): https exception!!!");
                e.printStackTrace();
            } catch (KeyManagementException e) {
                Log.e(TAG, "openComm(): https exception!!!");
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                Log.e(TAG, "openComm(): https exception!!!");
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "openComm(): http connection");
        }

        mConnection.setConnectTimeout(120 * 1000);
        mConnection.setReadTimeout(120 * 1000);
        mConnection.setDoOutput(true);
        mConnection.setDoInput(true);
        mConnection.setRequestProperty("Accept-Encoding", "identity");

        /* general header */
        addRequestProperty("Cache-Control", "private");
        addRequestProperty("Connection", "close");
        addRequestProperty("Accept",
                "application/vnd.syncml+xml, application/vnd.syncml+wbxml, */*");
        addRequestProperty("Accept-Language", "en");
        addRequestProperty("Accept-Charset", "utf-8");

        return true;
    }

    /**
     * @param buffer buffer to save received data.
     * @return bytes actually received.
     */
    public int recvData(byte[] buffer) {
        Log.d(TAG, "recvData: buflen=" + buffer.length);
        if (mConnection == null) {
            Log.e(TAG, "recvData: mConnection=" + mConnection);
            return -1;
        }

        try {
            InputStream in = mConnection.getInputStream();
            int ret = in.read(buffer);
            return ret;
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "recvData: SocketTimeoutException!!");
        } catch (IOException e) {
            Log.e(TAG, "recvData: IOException!!");
        }

        return -1;
    }

    /**
     * @param data data to be sent.
     * @return bytes actually sent.
     */
    public int sendData(byte[] data) {
        Log.d(TAG, "sendData: len=" + data.length);
        if (mConnection == null) {
            Log.e(TAG, "sendData: mConnection=" + mConnection);
            return -1;
        }

        addRequestProperty("Content-Length", String.valueOf(data.length));

        try {
            OutputStream out = mConnection.getOutputStream();
            out.write(data, 0, data.length);
            out.flush();
        } catch (IOException e) {
            Log.e(TAG, "sendData IOException: " + e);
            Log.e(TAG, "Message: " + e.getMessage());
            Log.e(TAG, "Cause: " + e.getCause());
            e.printStackTrace();
            return -1;
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "sendData: IndexOutOfBoundsException!!");
            return -1;
        }

        Log.d(TAG, "sendData: return " + data.length);
        return data.length;
    }

    private boolean waitResponse() {
        InputStream is = null;
        byte[] buf = new byte[8192];
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        boolean debug = true;

        try {
            Log.d(TAG, "waitResponse: enterring getInputStream...");
            Thread.currentThread().sleep(15*1000);
            mConnection.getInputStream();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "waitResponse: IOException");
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (debug) {
            is = mConnection.getErrorStream();
            try {
                while (true) {
                    int rd = is.read(buf, 0, 8192);
                    if (rd == -1) {
                        break;
                    }
                    bos.write(buf, 0, rd);
                }
            } catch (IOException e) {
                Log.e(TAG, "is.read: IOException");
                e.printStackTrace();
            }
            String responseDump = new String(buf);
            Log.d(TAG, "waitResponse: " + responseDump);
        }

        return false;
    }
}
