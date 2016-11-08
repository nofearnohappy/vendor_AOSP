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

/*
 *
 */

package com.mediatek.xcap.client;

import android.content.Context;
import android.net.Network;
import android.os.IBinder;
import android.os.ServiceManager;
import android.util.Log;

import com.android.okhttp.Headers;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
/**
 * XcapClient class.
 */
public class XcapClient {
    private static final String TAG = "XcapClient";

    private HttpURLConnection mConnection = null;

    private String mUserAgent;

    // Default connection and socket timeout of 60 seconds. Tweak to taste.
    private static final int SOCKET_OPERATION_TIMEOUT = 30 * 1000;
    private static final int MAX_SOCKET_CONNECTION = 30;

    private XcapDebugParam mDebugParam = XcapDebugParam.getInstance();
    private Network mNetwork;
    private Context mContext;

    private TrustManager[] mTrustAllCerts = new TrustManager[] {new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
        }

        public void checkClientTrusted(java.security.cert.X509Certificate[] certs,
                String authType) {
        }

        public void checkServerTrusted(java.security.cert.X509Certificate[] certs,
                String authType) {
        }
    } };

    public static final String METHOD_PUT = "PUT";
    public static final String METHOD_GET = "GET";
    public static final String METHOD_DELETE = "DELETE";

    /**
     * Constructor.
     *
     */
    public XcapClient() {
        composeUserAgent();
        initialize();
    }

    /**
     * Constructor.
     *
     * @param userAgent XCAP client User Agent
     */
    public XcapClient(String userAgent) {
        mUserAgent = userAgent;
        initialize();
    }

    /**
     * Constructor.
     *
     * @param network dedicated network
     */
    public XcapClient(Network network) {
        composeUserAgent();

        if (network != null) {
            mNetwork = network;
        }

        initialize();
    }

    /**
     * Constructor.
     *
     * @param userAgent XCAP client User Agent
     * @param network dedicated network
     */
    public XcapClient(String userAgent, Network network) {
        mUserAgent = userAgent;

        if (network != null) {
            mNetwork = network;
        }

        initialize();
    }

    /**
     * Set context.
     *
     * @param ctxt context to set
     */
    public void setContext(Context ctxt) {
        mContext = ctxt;
    }

    private void composeUserAgent() {
        boolean isGbaEnabled = false;
        IBinder b = ServiceManager.getService("GbaService");
        if (b != null) {
            Log.i(TAG , "GbaService Enabled");
            isGbaEnabled = true;
        }

        if (mDebugParam.getXcapUserAgent() != null && !mDebugParam.getXcapUserAgent().isEmpty()) {
            mUserAgent = mDebugParam.getXcapUserAgent();
        } else {
            mUserAgent = "XCAP Client" + (isGbaEnabled ? " 3gpp-gba" : "");
        }
    }

    private void initialize() {

    }

    /**
     * Shutdown connection.
     */
    public void shutdown() {
        if (mConnection != null) {
            mConnection.disconnect();
        }
    }

    private void addExtraHeaders(HttpURLConnection connection, Headers rawHeaders) {
        Set<String> names = rawHeaders.names();
        for (String name : names) {
            List<String> values = rawHeaders.values(name);
            for (String value : values) {
                if (!name.isEmpty() && !value.isEmpty()) {
                    // Add the header if the param is valid
                    connection.setRequestProperty(name, value);
                    break;
                }
            }
        }
    }

    private void logRequestHeaders(HttpURLConnection connection) {
        Map<String, List<String>> headerFields = connection.getRequestProperties();

        Log.d(TAG, "Request Headers:");

        for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
            final String key = entry.getKey();
            final List<String> values = entry.getValue();
            if (values != null) {
                for (String value : values) {
                    Log.d(TAG, key + ": " + value);
                }
            }
        }
    }

    private void logResponseHeaders(HttpURLConnection connection) {
        Map<String, List<String>> headerFields = connection.getHeaderFields();

        Log.d(TAG, "Response Headers:");

        for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
            final String key = entry.getKey();
            final List<String> values = entry.getValue();
            if (values != null) {
                for (String value : values) {
                    Log.d(TAG, key + ": " + value);
                }
            }
        }
    }

    private HttpURLConnection execute(URL url, String method, byte[] xml,
            Headers additionalRequestHeaders) throws IOException {
        int tryCount = 3;
        boolean success = false;
        //OkHttp usage
        mConnection = null;

        boolean isTrustAll = mDebugParam.getEnableXcapTrustAll();

        if (isTrustAll) {
            // Install the all-trusting trust manager
            try {
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, mTrustAllCerts, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            } catch (GeneralSecurityException se) {
                se.printStackTrace();
            }

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        }

        while (tryCount > 0 && !success) {
            try {
                Log.d(TAG, method + " :" + url.toString());

                if (mNetwork != null) {
                    mConnection = (HttpURLConnection) mNetwork.openConnection(url);
                } else {
                    mConnection = (HttpURLConnection) url.openConnection();
                }
                mConnection.setDoInput(true);
                mConnection.setConnectTimeout(SOCKET_OPERATION_TIMEOUT);
                mConnection.setReadTimeout(SOCKET_OPERATION_TIMEOUT);
                mConnection.setWriteTimeout(SOCKET_OPERATION_TIMEOUT);
                // Header: User-Agent
                mConnection.setRequestProperty("User-Agent", mUserAgent);
                addExtraHeaders(mConnection, additionalRequestHeaders);
                // Different stuff for GET and POST
                if (METHOD_PUT.equals(method)) {
                    mConnection.setDoOutput(true);
                    mConnection.setRequestMethod(METHOD_PUT);
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        logRequestHeaders(mConnection);
                    }
                    // Sending request body
                    final OutputStream out =
                            new BufferedOutputStream(mConnection.getOutputStream());
                    out.write(xml);
                    out.flush();
                    out.close();
                } else if (METHOD_GET.equals(method)) {
                    mConnection.setRequestMethod(METHOD_GET);
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        logRequestHeaders(mConnection);
                    }
                }


                // Get response
                final int responseCode = mConnection.getResponseCode();
                final String responseMessage = mConnection.getResponseMessage();
                Log.d(TAG, "HTTP: " + responseCode + " " + responseMessage);
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    logResponseHeaders(mConnection);
                }

                if (responseCode == 200 || responseCode == 403  || responseCode == 304 ||
                        responseCode == 412) {
                    success = true;
                    break;
                } else if (responseCode == 409) {
                    if ("true".equals(System.getProperty("xcap.handl409"))) {
                        success = true;
                        break;
                    } else {
                        Log.d(TAG, "HTTP status code is not 200 or 403");
                    }
                } else {
                    Log.d(TAG, "HTTP status code is not 200 or 403 or 409");
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
                throw e;
            } catch (ProtocolException e) {
                e.printStackTrace();
                throw e;
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "gba.auth:" + System.getProperty("gba.auth"));
                if ("403".equals(System.getProperty("gba.auth"))) {
                    success = true;
                    System.setProperty("gba.auth", "");
                    throw new IOException("GBA Authentication hit HTTP 403 Forbidden");
                } else {
                    throw e;
                }
            } finally {
                if (!success) {
                    try {
                        tryCount--;
                        if (tryCount > 0) {
                            Thread.sleep(5 * 1000);
                            Log.d(TAG, "retry once");
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return mConnection;
        //OkHttp usage end
    }

    /**
     * HTTP GET.
     *
     * @param  uri document URI
     * @param  additionalRequestHeaders HTTP headers
     * @return HTTP response
     * @throws IOException if I/O error
     */
    public HttpURLConnection get(URI uri, Headers additionalRequestHeaders) throws IOException {
        return execute(uri.toURL(), METHOD_GET, null, additionalRequestHeaders);
    }

    /**
     * HTTP PUT.
     *
     * @param  uri document URI
     * @param  mimetype MIME TYPE
     * @param  content content to upload
     * @return HTTP response
     * @throws IOException if I/O error
     */
    public HttpURLConnection put(URI uri, String mimetype, String content) throws IOException {
        Log.d(TAG, "PUT: " + content);
        return put(uri, mimetype, content.getBytes("UTF-8"), null, null, null);

    }

    /**
     * HTTP PUT.
     *
     * @param  uri document URI
     * @param  mimetype MIME TYPE
     * @param  content content to upload
     * @param  additionalRequestHeaders HTTP headers
     * @return HTTP response
     * @throws IOException if I/O error
     */
    public HttpURLConnection put(URI uri, String mimetype, String content,
            Headers additionalRequestHeaders) throws IOException {
        Log.d(TAG, "PUT: " + content);
        return put(uri, mimetype, content.getBytes("UTF-8"), additionalRequestHeaders, null, null);

    }

    /**
     * HTTP PUT.
     *
     * @param  uri document URI
     * @param  mimetype MIME TYPE
     * @param  content content to upload in string format
     * @param  additionalRequestHeaders HTTP headers
     * @param  eTag E-TAG
     * @param  condition use with E-TAG
     * @return HTTP response
     * @throws IOException if I/O error
     */
    public HttpURLConnection put(URI uri, String mimetype, String content,
            Headers additionalRequestHeaders, String eTag, String condition) throws IOException {
        Log.d(TAG, "PUT: " + content);
        return put(uri, mimetype, content.getBytes("UTF-8"), additionalRequestHeaders, eTag,
                condition);

    }

    /**
     * HTTP PUT.
     *
     * @param  uri document URI
     * @param  mimetype MIME TYPE
     * @param  content content to upload in byte array format
     * @param  additionalRequestHeaders HTTP headers
     * @param  eTag E-TAG
     * @param  condition use with E-TAG
     * @return HTTP response
     * @throws IOException if I/O error
     */
    public HttpURLConnection put(URI uri, String mimetype, byte[] content,
            Headers additionalRequestHeaders, String eTag, String condition) throws IOException {
        Headers.Builder headers = additionalRequestHeaders.newBuilder();
        headers.add(XcapConstants.HDR_KEY_CONTENT_TYPE, mimetype);
        return execute(uri.toURL(), METHOD_PUT, content, headers.build());
    }

    /**
     * HTTP DELETE.
     *
     * @param  uri document URI
     * @return HTTP response
     * @throws IOException if I/O error
     */
    public HttpURLConnection delete(URI uri) throws IOException {
        return delete(uri, null, null, null);
    }

    /**
     * HTTP DELETE.
     *
     * @param  uri document URI
     * @param  additionalRequestHeaders HTTP headers
     * @return HTTP response
     * @throws IOException if I/O error
     */
    public HttpURLConnection delete(URI uri, Headers additionalRequestHeaders) throws IOException {
        return delete(uri, additionalRequestHeaders, null, null);
    }

    /**
     * HTTP DELETE.
     *
     * @param  uri document URI
     * @param  additionalRequestHeaders HTTP headers
     * @param  eTag E-TAG
     * @param  condition use with E-TAG
     * @return HTTP response
     * @throws IOException if I/O error
     */
    public HttpURLConnection delete(URI uri, Headers additionalRequestHeaders, String eTag,
            String condition) throws IOException {
        return execute(uri.toURL(), METHOD_DELETE, null, additionalRequestHeaders);
    }
}
