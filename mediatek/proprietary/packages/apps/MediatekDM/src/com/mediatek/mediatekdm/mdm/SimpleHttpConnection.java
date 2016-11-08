package com.mediatek.mediatekdm.mdm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

public class SimpleHttpConnection implements PLHttpConnection {
    class CertTrustManager implements X509TrustManager {
        public void checkClientTrusted(X509Certificate[] arg0, String arg1) {
            mLogger.logMsg(MdmLogLevel.DEBUG, "[checkClientTrusted] " + arg0 + arg1);
        }

        public void checkServerTrusted(X509Certificate[] arg0, String arg1)
                throws CertificateException {
            mLogger.logMsg(MdmLogLevel.DEBUG, "[checkServerTrusted] X509Certificate amount:"
                    + arg0.length + ", cryptography: " + arg1);
        }

        public X509Certificate[] getAcceptedIssuers() {
            mLogger.logMsg(MdmLogLevel.DEBUG, "[getAcceptedIssuers] ");
            return null;
        }
    }

    private HttpURLConnection mConnection;
    private MdmEngine mEngine;
    private PLLogger mLogger = null;
    private Proxy.Type mProxyType = null;
    private URL mUrl = null;
    private TrustManager[] mTrustManagerArray = new TrustManager[] { new CertTrustManager() };

    public SimpleHttpConnection(MdmEngine engine) {
        mEngine = engine;
        mLogger = MdmEngine.getLogger();
    }

    @Override
    public boolean addRequestProperty(String field, String value) {
        mLogger.logMsg(MdmLogLevel.DEBUG, "addRequestProperty: " + field + " = " + value);

        if (mConnection == null) {
            mLogger.logMsg(MdmLogLevel.ERROR, "AddRequestProperty: mConnection=" + mConnection);
            return false;
        }

        try {
            mConnection.setRequestProperty(field, value);
            return true;
        } catch (IllegalStateException e) {
            mLogger.logMsg(MdmLogLevel.ERROR, "AddRequestProperty: IllegalStateException");
        } catch (NullPointerException e) {
            mLogger.logMsg(MdmLogLevel.ERROR, "AddRequestProperty: NullPointerException");
        }

        return false;
    }

    @Override
    public boolean closeComm() {
        mLogger.logMsg(MdmLogLevel.DEBUG, "closeComm()");
        if (mConnection == null) {
            mLogger.logMsg(MdmLogLevel.ERROR, "closeComm: mConnection=" + mConnection);
            return false;
        }

        mConnection.disconnect();
        return true;
    }

    @Override
    public void destroy() {
        // nothing to do
    }

    @Override
    public int getContentLength() {
        mLogger.logMsg(MdmLogLevel.DEBUG, "getContentLength()");

        if (mConnection == null) {
            mLogger.logMsg(MdmLogLevel.ERROR, "getContentLength: mConnection=" + mConnection);
            return -1;
        }

        if (!waitResponse()) {
            mLogger.logMsg(MdmLogLevel.ERROR, "getHeadField: timeout");
            return -1;
        }

        int length = mConnection.getContentLength();
        if (length < 0) {
            try {
                /* for chunked ?? */
                length = mConnection.getInputStream().available();
            } catch (IOException e) {
                mLogger.logMsg(MdmLogLevel.ERROR, "in.available: IOException " + e.getMessage());
                e.printStackTrace();
            }
        }

        mLogger.logMsg(MdmLogLevel.DEBUG, "getContentLength() return " + length);

        return length;
    }

    @Override
    public String getHeadField(String field) {
        mLogger.logMsg(MdmLogLevel.DEBUG, "getHeadField: field=" + field);

        if (mConnection == null) {
            mLogger.logMsg(MdmLogLevel.ERROR, "getHeadField: mConnection=" + mConnection);
            return null;
        }

        if (!waitResponse()) {
            mLogger.logMsg(MdmLogLevel.ERROR, "getHeadField: timeout");
            return null;
        }

        return mConnection.getHeaderField(field);

    }

    @Override
    public int getHeadFieldInt(String field, int defValue) {
        mLogger.logMsg(MdmLogLevel.DEBUG, "getHeadFieldInt: field=" + field + " ,defValue="
                + defValue);
        if (mConnection == null) {
            mLogger.logMsg(MdmLogLevel.ERROR, "getHeadFieldInt: mConnection=" + mConnection);
            return defValue;
        }

        if (!waitResponse()) {
            mLogger.logMsg(MdmLogLevel.ERROR, "getHeadField: timeout");
            return defValue;
        }

        return mConnection.getHeaderFieldInt(field, defValue);

    }

    @Override
    public String getURL() {
        if (mConnection == null) {
            mLogger.logMsg(MdmLogLevel.ERROR, "getURL: mConnection=" + mConnection);
            return null;
        }

        return mConnection.getURL().toString();
    }

    /**
     * @param uri
     * @param proxyType
     *        : 0 -- DIRECT, 1 -- PROXY(HTTP??), 2 --SOCKS
     * @param proxyAddr
     * @param proxyPort
     */
    public boolean initialize(String uri, int proxyType, String proxyAddr, int proxyPort) {
        mLogger.logMsg(MdmLogLevel.DEBUG, "initialize: uri=" + uri + ", proxyType=" + proxyType
                + ", proxyAddr=" + proxyAddr + ", proxyPort=" + proxyPort);
        try {
            mUrl = new URL(uri);
            mLogger.logMsg(MdmLogLevel.DEBUG, "Host is " + mUrl.getHost());
            mLogger.logMsg(MdmLogLevel.DEBUG, "Port is " + mUrl.getPort());
        } catch (MalformedURLException e) {
            mLogger.logMsg(MdmLogLevel.ERROR, "SimpleHttpConnection: invalid URL: " + uri);
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

        try {
            if (mProxyType == Proxy.Type.DIRECT) {
                mConnection = (HttpURLConnection) mUrl.openConnection();
            } else {
                InetSocketAddress addr = new InetSocketAddress(proxyAddr, proxyPort);
                mConnection = (HttpURLConnection) mUrl.openConnection(new Proxy(mProxyType, addr));
            }
            return true;
        } catch (IOException e) {
            mLogger.logMsg(MdmLogLevel.DEBUG, "SimpleHttpConnection: IOException");
            e.printStackTrace();
            return false;
        } catch (IllegalArgumentException e) {
            mLogger.logMsg(MdmLogLevel.DEBUG, "SimpleHttpConnection: IllegalArgumentException");
            return false;
        } catch (UnsupportedOperationException e) {
            mLogger.logMsg(MdmLogLevel.DEBUG, "SimpleHttpConnection: UnsupportedOperationException");
            return false;
        }
    }

    @Override
    public boolean openComm() {
        mLogger.logMsg(MdmLogLevel.DEBUG, "openComm()");
        if (mConnection == null) {
            mLogger.logMsg(MdmLogLevel.ERROR, "openComm: mConnection=" + mConnection);
            return false;
        }

        if (mConnection instanceof HttpsURLConnection) {
            mLogger.logMsg(MdmLogLevel.DEBUG, "openComm(): https connection");
            HttpsURLConnection connection = (HttpsURLConnection) mConnection;
            try {
                // TODO: Implement HostnameVerifier
                HostnameVerifier hv = new HostnameVerifier() {
                    public boolean verify(String urlHostName, SSLSession session) {
                        mLogger.logMsg(MdmLogLevel.DEBUG, "verify:" + urlHostName);
                        return true;
                    }
                };

                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, mTrustManagerArray, null);
                SSLSocketFactory sslf = sc.getSocketFactory();
                connection.setHostnameVerifier(hv);
                connection.setSSLSocketFactory(sslf);
            } catch (IllegalArgumentException e) {
                mLogger.logMsg(MdmLogLevel.ERROR, "openComm(): https exception!!!");
                e.printStackTrace();
            } catch (KeyManagementException e) {
                mLogger.logMsg(MdmLogLevel.ERROR, "openComm(): https exception!!!");
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                mLogger.logMsg(MdmLogLevel.ERROR, "openComm(): https exception!!!");
                e.printStackTrace();
            }
        } else {
            mLogger.logMsg(MdmLogLevel.DEBUG, "openComm(): http connection");
        }

        mConnection.setConnectTimeout(mEngine.getConnectionTimeout() * 1000);
        mConnection.setReadTimeout(mEngine.getReadTimeout() * 1000);
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

    @Override
    public int recvData(byte[] buffer) {
        mLogger.logMsg(MdmLogLevel.DEBUG, "recvData: buflen=" + buffer.length);
        if (mConnection == null) {
            mLogger.logMsg(MdmLogLevel.ERROR, "recvData: mConnection=" + mConnection);
            return -1;
        }

        try {
            InputStream in = mConnection.getInputStream();
            int ret = in.read(buffer);
            return ret;
        } catch (SocketTimeoutException e) {
            mLogger.logMsg(MdmLogLevel.ERROR, "recvData: SocketTimeoutException!!");
        } catch (IOException e) {
            mLogger.logMsg(MdmLogLevel.ERROR, "recvData: IOException!!");
        }

        return -1;
    }

    @Override
    public int sendData(byte[] data) {
        mLogger.logMsg(MdmLogLevel.DEBUG, "sendData: len=" + data.length);
        if (mConnection == null) {
            mLogger.logMsg(MdmLogLevel.ERROR, "sendData: mConnection=" + mConnection);
            return -1;
        }

        addRequestProperty("Content-Length", String.valueOf(data.length));

        try {
            OutputStream out = mConnection.getOutputStream();
            out.write(data, 0, data.length);
            out.flush();
        } catch (IOException e) {
            mLogger.logMsg(MdmLogLevel.ERROR, "sendData IOException: " + e);
            mLogger.logMsg(MdmLogLevel.ERROR, "Message: " + e.getMessage());
            mLogger.logMsg(MdmLogLevel.ERROR, "Cause: " + e.getCause());
            e.printStackTrace();
            return -1;
        } catch (IndexOutOfBoundsException e) {
            mLogger.logMsg(MdmLogLevel.ERROR, "sendData: IndexOutOfBoundsException!!");
            return -1;
        }

        mLogger.logMsg(MdmLogLevel.DEBUG, "sendData: return " + data.length);
        return data.length;
    }

    private boolean waitResponse() {
        InputStream is = null;
        byte[] buf = new byte[8192];
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        boolean debug = true;

        try {
            mLogger.logMsg(MdmLogLevel.DEBUG, "waitResponse: enterring getInputStream...");
            mConnection.getInputStream();
            return true;
        } catch (IOException e) {
            mLogger.logMsg(MdmLogLevel.ERROR, "waitResponse: IOException");
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
                mLogger.logMsg(MdmLogLevel.ERROR, "is.read: IOException");
                e.printStackTrace();
            }
            String responseDump = new String(buf);
            mLogger.logMsg(MdmLogLevel.DEBUG, "waitResponse: " + responseDump);
        }

        return false;
    }
}
