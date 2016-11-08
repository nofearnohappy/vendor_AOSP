/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.rcs.message.cloudbackup.transport;

import android.content.Context;
import android.util.Log;
import com.mediatek.rcs.message.cloudbackup.Config;
import com.mediatek.rcs.message.cloudbackup.utils.CertificateValidationException;
import com.mediatek.rcs.message.cloudbackup.utils.MessagingException;
import com.mediatek.rcs.message.cloudbackup.utils.SSLUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

/**
 * Transport connection.
 *
 */
public class MailTransport {
    private static final String LOG_TAG = "RcsBR/MailTransport";
    private static final boolean DEBUG = true;
    // TODO protected eventually
    /*protected*/ public static final int SOCKET_CONNECT_TIMEOUT = 10000;
    /*protected*/ public static final int SOCKET_READ_TIMEOUT = 60000;

    private static final HostnameVerifier HOSTNAME_VERIFIER =
            HttpsURLConnection.getDefaultHostnameVerifier();

    private final String mDebugLabel;
    private final Context mContext;

    private Socket mSocket;
    private InputStream mIn;
    private OutputStream mOut;

    /**
     * Constructor.
     * @param context .
     * @param debugLabel .
     */
    public MailTransport(Context context, String debugLabel) {
        super();
        mContext = context;
        mDebugLabel = debugLabel;
    }

   /**
     * Returns a new transport, using the current transport as a model. The new transport is
     * configured identically (as if {@link #setSecurity(int, boolean)}, {@link #setPort(int)}
     * and {@link #setHost(String)} were invoked), but not opened or connected in any way.
     * @return .
     */
    @Override
    public MailTransport clone() {
        return new MailTransport(mContext, mDebugLabel);
    }

    /**
     * @return Server address.
     * TODO: how to config it.
     */
    public String getHost() {
        //return mHostAuth.mAddress;
        return Config.getHost();
    }

    /**
     * @return Server port.
     */
    public int getPort() {
        //return mHostAuth.mPort;
        return Config.getPort();
    }

    /**
     * @return if can try SSL.
     */
    public boolean canTrySslSecurity() {
        //return (mHostAuth.mFlags & HostAuth.FLAG_SSL) != 0;
        boolean res = false;
        return res;
    }

    /**
     * @return If can trust all certificates.
     */
    public boolean canTrustAllCertificates() {
        return Config.canTrustAllCertificates();
    }

    /**
     * Attempts to open a connection using the Uri supplied for connection parameters.  Will attempt
     * an SSL connection if indicated.
     * @throws MessagingException .
     * @throws CertificateValidationException .
     */
    public void open() throws MessagingException,
            CertificateValidationException {
        if (DEBUG) {
            Log.d(LOG_TAG, "*** " + mDebugLabel + " open " + getHost() + ":"
                    + String.valueOf(getPort()));
        }

        try {
            SocketAddress socketAddress = new InetSocketAddress(getHost(), getPort());
            if (canTrySslSecurity()) {
                /* TODO:
                mSocket = SSLUtils.getSSLSocketFactory(
                        mContext, mHostAuth, null, canTrustAllCertificates()).createSocket();
                        */
            } else {
                mSocket = new Socket();
            }
            mSocket.connect(socketAddress, SOCKET_CONNECT_TIMEOUT);
            // After the socket connects to an SSL server, confirm that the hostname is as expected
            if (canTrySslSecurity() && !canTrustAllCertificates()) {
                verifyHostname(mSocket, getHost());
            }
            // RCS:IMAP porting
            // Analytics.getInstance().sendEvent("socket_certificates",
            //         "open", Boolean.toString(canTrustAllCertificates()), 0);
            if (mSocket instanceof SSLSocket) {
                final SSLSocket sslSocket = (SSLSocket) mSocket;
                if (sslSocket.getSession() != null) {
                    // RCS:IMAP porting
                    // Analytics.getInstance().sendEvent("cipher_suite",
                    //         sslSocket.getSession().getProtocol(),
                    //         sslSocket.getSession().getCipherSuite(), 0);
                }
            }
            mIn = new BufferedInputStream(mSocket.getInputStream(), 1024);
            mOut = new BufferedOutputStream(mSocket.getOutputStream(), 512);
            mSocket.setSoTimeout(SOCKET_READ_TIMEOUT);
        } catch (SSLException e) {
            if (DEBUG) {
                Log.d(LOG_TAG, e.toString());
            }
            throw new CertificateValidationException(e.getMessage(), e);
        } catch (IOException ioe) {
            if (DEBUG) {
                Log.d(LOG_TAG, ioe.toString());
            }
            throw new MessagingException(MessagingException.IOERROR, ioe.toString());
        } catch (IllegalArgumentException iae) {
            if (DEBUG) {
                Log.d(LOG_TAG, iae.toString());
            }
            throw new MessagingException(MessagingException.UNSPECIFIED_EXCEPTION, iae.toString());
        }
    }

    /**
     * Attempts to reopen a TLS connection using the Uri supplied for connection parameters.
     *
     * NOTE: No explicit hostname verification is required here, because it's handled automatically
     * by the call to createSocket().
     *
     * TODO should we explicitly close the old socket?  This seems funky to abandon it.
     *
     * @throws MessagingException .
     */
    public void reopenTls() throws MessagingException {
        try {
            mSocket = SSLUtils.getSSLSocketFactory()
                    .createSocket(mSocket, getHost(), getPort(), true);
            mSocket.setSoTimeout(SOCKET_READ_TIMEOUT);
            mIn = new BufferedInputStream(mSocket.getInputStream(), 1024);
            mOut = new BufferedOutputStream(mSocket.getOutputStream(), 512);
        } catch (SSLException e) {
            if (DEBUG) {
                Log.d(LOG_TAG, e.toString());
            }
            throw new CertificateValidationException(e.getMessage(), e);
        } catch (IOException ioe) {
            if (DEBUG) {
                Log.d(LOG_TAG, ioe.toString());
            }
            throw new MessagingException(MessagingException.IOERROR, ioe.toString());
        }
    }

    /**
     * Lightweight version of SSLCertificateSocketFactory.verifyHostname, which provides this
     * service but is not in the public API.
     *
     * Verify the hostname of the certificate used by the other end of a
     * connected socket.  You MUST call this if you did not supply a hostname
     * to SSLCertificateSocketFactory.createSocket().  It is harmless to call this method
     * redundantly if the hostname has already been verified.
     *
     * <p>Wildcard certificates are allowed to verify any matching hostname,
     * so "foo.bar.example.com" is verified if the peer has a certificate
     * for "*.example.com".
     *
     * @param socket An SSL socket which has been connected to a server
     * @param hostname The expected hostname of the remote server
     * @throws IOException if something goes wrong handshaking with the server
     * @throws SSLPeerUnverifiedException if the server cannot prove its identity
      */
    private static void verifyHostname(Socket socket, String hostname) throws IOException {
        // The code at the start of OpenSSLSocketImpl.startHandshake()
        // ensures that the call is idempotent, so we can safely call it.
        SSLSocket ssl = (SSLSocket) socket;
        ssl.startHandshake();

        SSLSession session = ssl.getSession();
        if (session == null) {
            throw new SSLException("Cannot verify SSL socket without session");
        }
        // TODO: Instead of reporting the name of the server we think we're connecting to,
        // we should be reporting the bad name in the certificate.  Unfortunately this is buried
        // in the verifier code and is not available in the verifier API, and extracting the
        // CN & alts is beyond the scope of this patch.
        if (!HOSTNAME_VERIFIER.verify(hostname, session)) {
            throw new SSLPeerUnverifiedException(
                    "Certificate hostname not useable for server: " + hostname);
        }
    }

    /**
     * Get the socket timeout.
     * @return the read timeout value in milliseconds
     * @throws SocketException .
     */
    public int getSoTimeout() throws SocketException {
        return mSocket.getSoTimeout();
    }

    /**
     * Set the socket timeout.
     * @param timeoutMilliseconds the read timeout value if greater than {@code 0}, or
     *            {@code 0} for an infinite timeout.
     * @throws SocketException .
     */
    public void setSoTimeout(int timeoutMilliseconds) throws SocketException {
        mSocket.setSoTimeout(timeoutMilliseconds);
    }

    /**
     * @return if it is open.
     */
    public boolean isOpen() {
        Log.d(LOG_TAG, "isOpen: mIn=" + mIn + ", mOut=" + mOut + ", mSocket="
                + mSocket);
        if (mSocket != null) {
            Log.d(LOG_TAG, "connected: " + mSocket.isConnected()
                    + ", isClosed: " + mSocket.isClosed());
        }
        return (mIn != null && mOut != null && mSocket != null
                && mSocket.isConnected() && !mSocket.isClosed());
    }

    /**
     * Close the connection.  MUST NOT return any exceptions - must be "best effort" and safe.
     */
    public void close() {
        try {
            if (mIn != null) {
                mIn.close();
            }
        } catch (IOException e) {
            // May fail if the connection is already closed.
            e.printStackTrace();
        }
        try {
            if (mOut != null) {
                mOut.close();
            }
        } catch (IOException e) {
            // May fail if the connection is already closed.
            e.printStackTrace();
        }
        try {
            mSocket.close();
        } catch (IOException e) {
            // May fail if the connection is already closed.
            e.printStackTrace();
        }
        mIn = null;
        mOut = null;
        mSocket = null;
    }

    /**
     * @return InputStream.
     */
    public InputStream getInputStream() {
        return mIn;
    }

    /**
     * @return OutputStream.
     */
    public OutputStream getOutputStream() {
        return mOut;
    }

    /**
     * Writes a single line to the server using \r\n termination.
     * @param s .
     * @param sensitiveReplacement .
     * @throws IOException .
     */
    public void writeLine(String s, String sensitiveReplacement) throws IOException {
        if (DEBUG) {
            // RCS:IMAP porting
            //if (sensitiveReplacement != null && !Logging.DEBUG_SENSITIVE) {
            if (sensitiveReplacement != null) {
                Log.d(LOG_TAG, ">>> " + sensitiveReplacement);
            } else {
                Log.d(LOG_TAG, ">>> " + s);
            }
        }

        OutputStream out = getOutputStream();
        out.write(s.getBytes());
        out.write('\r');
        out.write('\n');
        out.flush();
    }

    /**
     * Reads a single line from the server, using either \r\n or \n as the delimiter.  The
     * delimiter char(s) are not included in the result.
     * @param loggable .
     * @return line readed.
     * @throws IOException .
     */
    public String readLine(boolean loggable) throws IOException {
        StringBuffer sb = new StringBuffer();
        InputStream in = getInputStream();
        int d;
        while ((d = in.read()) != -1) {
            if (((char) d) == '\r') {
                continue;
            } else if (((char) d) == '\n') {
                break;
            } else {
                sb.append((char) d);
            }
        }
        if (d == -1 && DEBUG) {
            Log.d(LOG_TAG, "End of stream reached while trying to read line.");
        }
        String ret = sb.toString();
        if (loggable && DEBUG) {
            Log.d(LOG_TAG, "<<< " + ret);
        }

        /**
         * M: If the end of the stream has been reached and there is no any
         * content in string buffer, it means that some error occurred, so it
         * need throw a IOException to force caller to jump out of loop.
         */
        if ((d == -1) && (ret.isEmpty())) {
            Log.e(LOG_TAG,
                    "End of stream reached, but no any content in string buffer.");
            throw new IOException(
                    "End of stream reached, but no any content in string buffer.");
        }
        /** @} */
        return ret;
    }

    /**
     * @return local address.
     */
    public InetAddress getLocalAddress() {
        if (isOpen()) {
            return mSocket.getLocalAddress();
        } else {
            return null;
        }
    }
}
