/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
 ******************************************************************************/

package com.orangelabs.rcs.platform.network;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;

import android.content.Context;
import android.net.ConnectivityManager;

import com.orangelabs.rcs.core.ims.network.ImsNetworkInterface.DnsResolvedFields;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.utils.IpAddressUtils;
import com.orangelabs.rcs.utils.logger.Logger;

import org.apache.http.conn.util.InetAddressUtils;

/**
 * Android network factory
 *
 * @author jexa7410
 */
public class AndroidNetworkFactory extends NetworkFactory {
    // Changed by Deutsche Telekom
    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Returns the local IP address of a given network interface
     *
     * @param dnsEntry remote address to find an according local socket address
     * @param type the type of the network interface, should be either
     *        {@link android.net.ConnectivityManager#TYPE_WIFI} or
     *        {@link android.net.ConnectivityManager#TYPE_MOBILE}
     * @return Address
     */
    // Changed by Deutsche Telekom
    public String getLocalIpAddress(DnsResolvedFields dnsEntry, int type) {
        try {
            // What kind of remote address (P-CSCF) are we trying to reach?
            boolean isIpv4 = InetAddressUtils.isIPv4Address(dnsEntry.ipAddress);

            ConnectivityManager connMgr =  (ConnectivityManager)AndroidFactory
                    .getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

            List<InetAddress> addresses = connMgr.getLinkProperties(type).getAddresses();

            for (int i = 0; i < addresses.size(); i++) {
                InetAddress addr = addresses.get(i);
                String ipAddress = IpAddressUtils.extractHostAddress(addr.getHostAddress());
                if (!addr.isLoopbackAddress() &&
                    !addr.isLinkLocalAddress() &&
                    (InetAddressUtils.isIPv4Address(ipAddress) == isIpv4)) {
                    return ipAddress;
                }
            }
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("getLocalIpAddress failed with ", e);
            }
        }

        return null;
    }

    /**
     * Create a datagram connection
     *
     * @return Datagram connection
     */
    public DatagramConnection createDatagramConnection() {
        return new AndroidDatagramConnection();
    }

    /**
     * Create a datagram connection with a specific SO timeout
     *
     * @param timeout SO timeout
     * @return Datagram connection
     */
    public DatagramConnection createDatagramConnection(int timeout) {
        return new AndroidDatagramConnection(timeout);
    }

    /**
     * Create a socket client connection
     *
     * @return Socket connection
     */
    public SocketConnection createSocketClientConnection() {
        return new AndroidSocketConnection();
    }

    /**
     * Create a secure socket client connection
     *
     * @return Socket connection
     */
    public SocketConnection createSecureSocketClientConnection() {
        return new AndroidSecureSocketConnection();
    }



    /**
     * Create a socket server connection
     *
     * @return Socket server connection
     */
    public SocketServerConnection createSocketServerConnection() {
        return new AndroidSocketServerConnection();
    }

    /**
     * Create an HTTP connection
     *
     * @return HTTP connection
     */
    public HttpConnection createHttpConnection() {
        return new AndroidHttpConnection();
    }


    /** M: Create SSL socket @{ */
    /**
     * Create a ssl socket client connection
     *
     * @return Socket connection
     */
    public SocketConnection createSSLSocketClientConnection() {
        return new AndroidSSLSocketConnection();
    }

    /**
     * Create a ssl socket server connection
     *
     * @return Socket server connection
     */
    public SocketServerConnection createSSLSocketServerConnection() {
        return new AndroidSSLSocketServerConnection();
    }
    /** @} */


}

