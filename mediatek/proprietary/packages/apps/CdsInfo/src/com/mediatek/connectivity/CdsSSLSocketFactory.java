package com.mediatek.connectivity;

import org.apache.http.conn.ssl.SSLSocketFactory;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * SSLSocketFactory for trust all.
 *
 * @hide
 */

public class CdsSSLSocketFactory extends SSLSocketFactory {
    SSLContext mSslContext = SSLContext.getInstance("TLS");

    CdsSSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException,
            KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        super(truststore);
        TrustManager[] trustManagers = new TrustManager[] {
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(
                X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(
                X509Certificate[] certs, String authType) {
                }
            }
        };
        mSslContext.init(null, trustManagers, null);
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
        throws IOException, UnknownHostException {
        return mSslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
    }

    @Override
    public Socket createSocket() throws IOException {
        return mSslContext.getSocketFactory().createSocket();
    }
}